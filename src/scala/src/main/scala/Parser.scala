package org.openworm.trackercommons

import fastparse.all._
import json._

object Parser {

  import Parts.{W, Digit => D}

  val Commons = "\"tracker-commons\"" ~ W(":") ~ "true"

  val Date = P(
    (D ~ D ~ D ~ D).! ~ "-" ~ (D ~ D).! ~ "-" ~ (D ~ D).! ~ "T" ~                   // Date
    (D ~ D).! ~         ":" ~ (D ~ D).! ~ ":" ~ (D ~ D ~ ("." ~ D.rep(1)).?).! ~    // Time
    ("Z" | (CharIn("+-") ~ D ~ D ~ ":" ~ D ~ D)).!.?                                // Locale
  ).map{ case (y,mo,d,h,mi,s,loc) => 
    val ss = s.toDouble
    val ssi = math.floor(ss).toInt
    val ssns = math.rint((ss - ssi)*1e9).toInt
    (java.time.LocalDateTime.of(y.toInt, mo.toInt, d.toInt, h.toInt, mi.toInt, ssi, ssns), loc.getOrElse(""))
  }

  def durationFormat(jtd: java.time.Duration) = "%d:%02d:%07.4f".format(jtd.getSeconds/3600, (jtd.getSeconds % 3600)/60, (jtd.getSeconds % 60) + 1e-9*jtd.getNano)

  val Age = P((D rep 1).! ~ ":" ~ (D ~ D).! ~ ":" ~ (D ~ D ~ ("." ~ D.rep(1)).?).!).map{ case (h,m,s) => 
    val ss = s.toDouble
    val ssi = math.floor(ss).toInt
    val ssns = math.rint((ss - ssi)*1e9).toInt
    java.time.Duration.ZERO.withSeconds(h.toLong*3600L + m.toInt*60 + ssi).withNanos(ssns)
  }

  val UnitKV = Struct.Str ~ W(":" ~! Pass) ~ Struct.Str

  def Units = {
    val u = new collection.mutable.AnyRefMap[String, units.Units]
    P(
      "\"units\"" ~ W(":") ~ "{" ~ 
      W(UnitKV.filter{ case (k,v) => units.Units(v.value) match { case None => false; case Some(vi) => u += ((k.value, vi)); true }}).rep(sep = W(",")) ~
      "}"
    ).
    map(_ => u: collection.Map[String, units.Units]).
    filter(u => Seq("t", "x", "y").forall(u contains _))
  }

  val Meta = P("\"metadata\"" ~ W(":") ~! Struct.Obj)

  val Files = P("\"files\"" ~ W(":") ~! Struct.Obj)

  val Dat = P(
    "\"data\"" ~ W(":") ~! (
      Struct.Obj.map(x => Array(x)) |
      Struct.Arr.filter{ case ArrJ(xs) => xs.forall{ case ob: ObjJ => true; case _ => false } }.map{ case ArrJ(xs) => xs.collect{ case ob: ObjJ => ob } }
    )
  )

  val Single = {
    // Kludgy var-based method to parse something at most once
    var u: Option[Either[String, collection.Map[String, units.Units]]] = None
    var m: Option[Either[String, ObjJ]] = None
    var d: Option[Either[String, Array[ObjJ]]] = None
    var f: Option[Either[String, ObjJ]] = None
    var commons = false
    val custom = collection.mutable.AnyRefMap.empty[String, List[JSON]]
    (
      W("{") ~! Pass.map{_ => u = None; m = None; d = None; f = None; commons = false; custom.clear(); () } ~
      P(
        Commons.map{_ => commons = true; () } |
        Files.map{ x => f = if (f.isEmpty) Some(Right(x)) else Some(Left("Mulitple file specification blocks not supported")); () } |
        Units.map{ x => u = if (u.isEmpty) Some(Right(x)) else Some(Left("Multiple units blocks not supported")); () } |
        Meta.map{ x => m = if (m.isEmpty) Some(Right(x)) else Some(Left("Multiple metadata blocks not supported")); () } |
        Dat.map{ x => d = if (d.isEmpty) Some(Right(x)) else Some(Left("Multiple data blocks not supported (all should be in one array!)")); () } |
        Struct.KeyVal.map{ case (k,v) => if (k startsWith "@") { custom += (k, v :: custom.get(k).getOrElse(Nil)) }; () }
      ).rep(sep = W("," ~! Pass)) ~
      W("}")
    ).map(_ => (u, m, d, f, commons, custom))
  }

  def apply(s: String): Either[String, DataSet] = Single.parse(s) match {
    case Result.Success((ou, om, od, ofs, com, cust), _) => (ou, od) match {
      case (None, _) => Left("Not valid WCON--no units.")
      case (_, None) => Left("Not valid WCON--no data.")
      case (Some(Right(u)), Some(Right(d))) =>
        val meta = om match {
          case None => Metadata.empty
          case Some(Left(l)) => return Left(l)
          case Some(Right(m)) => (Metadata from m) match {
            case Left(l) => return Left(l)
            case Right(r) => r
          }
        }
        val datas = d.map{ di => (DatX from di) match {
          case Left(l) => return Left(l)
          case Right(d)=> d
        }}
        val fs = ofs match {
          case None => FileSet.empty
          case Some(Left(l)) => return Left(l)
          case Some(Right(f)) => (FileSet from f) match {
            case Left(l) => return Left(l)
            case Right(r) => r
          }
        }
        Right(DataSet(meta, u, datas, fs, json.ObjJ(cust.toMap)))
      case (Some(Left(l)), _) => Left(l)
      case (_, Some(Left(l))) => Left(l)
    }
    case _ => Left("Parsing failed--probably not a valid JSON object?")
  }
}
