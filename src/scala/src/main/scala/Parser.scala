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

  val QDate = "\"" ~ Date ~ "\""

  val Age = P((D rep 1).! ~ ":" ~ (D ~ D).! ~ ":" ~ (D ~ D ~ ("." ~ D.rep(1)).?).!).map{ case (h,m,s) => 
    val ss = s.toDouble
    val ssi = math.floor(ss).toInt
    val ssns = math.rint((ss - ssi)*1e9).toInt
    java.time.Duration.ZERO.withSeconds(h.toLong*3600L + m.toInt*60 + ssi).withNanos(ssns)
  }

  val QAge = "\"" ~ Age "\""

  val Real = (
    Data.Num |
    Text.Null.map(_ => Double.NaN) |
    IgnoreCase("\"nan\"").map(_ => Double.NaN) |  // Not recommended, but valid JSON
    IgnoreCase("nan").!.map(_ => Double.NaN) |    // Not even valid JSON, don't do this
    (IgnoreCase("\"inf") ~ IgnoreCase("inity").? ~ "\"").map(_ => Double.PositiveInfinity) |   // Not recommended, but valid JSON
    (IgnoreCase("inf") ~ IgnoreCase("inity").?).map(_ => Double.PositiveInfinity) |            // Not even valid JSON, don't do this
    (IgnoreCase("\"-inf") ~ IgnoreCase("inity").? ~ "\"").map(_ => Double.NegativeInfinity) |  // Not recommended, but valid JSON
    (IgnoreCase("-inf") ~ IgnoreCase("inity").?).map(_ => Double.NegativeInfinity)             // Not even valid JSON, don't do this
  )

  val Realz = "[" ~ W(Real).rep(sep = "," ~! Pass).map(_.toArray) ~ "]"

  val Reals = Real.map(x => Array(x)) | Realz

  val Realzz = "[" ~ W(Reals).rep(sep = "," ~! Pass).map(_.toArray) ~ "]"

  val Realss = NoCut(Realz.map(x => Array(x))) | Realzz

  val UnitKV = Data.Str ~ W(":" ~! Pass) ~ Data.Str

  val Units = {
    val u = new collection.mutable.AnyRefMap[String, units.Units]
    P(
      "\"units\"" ~ W(":") ~ "{" ~ 
      W(UnitKV.filter{ case (k,v) => units.Units(v) match { case None => false; case Some(vi) => u += ((k, vi)); true }}).rep(sep = W(",")) ~
      "}" 
    ).
    map(_ => u: collection.Map[String, units.Units]).
    filter(u => Seq("t", "x", "y").forall(u contains _))
  }

  def V[X](p: P[X]): P[Vector[X]] = "[" + p.rep(sep = "," ~! Pass).toVector + "]"

  def Kw(s: String) = P(s ~ ":" ~! Pass)

  val Meta = {
    var lab = Vector.empty[Laboratory]
    var who = Vector.empty[String]
    var timestamp: Option[(java.time.LocalDateTime, String)] = None
    var temperature: Option[Temperature] = None
    var humidity: Option[Double] = None
    var arena: Option[Arena] = None
    var food: Option[String] = None
    var media: Option[String] = None
    var sex: Option[String] = None
    var stage: Option[String] = None
    var age: Option[java.time.Duration] = None
    var strain: Option[String] = None
    var protocol = Vector.empty[String]
    var software: Option[Software] = None
    var settings: Option[Any] = None
    var custom: Map[String, Any] = None
    P(
      "\"metadata\"" ~ W(":" ~! Pass) ~ "{" ~ W(
        (Kw("lab") ~ (Lab.map{l => lab += l; () } | V(Lab).map{ls => lab ++= ls; () })) |
        (Kw("who") ~ (Str.map{s => who += s; () } | V(Str).map{ss => who ++= ss; () })) |
        (Kw("timestamp") ~ QDate.filter(_ => timestamp.isEmpty).map{ case (ldt, s) => timestamp = Some((ldt, s)); ()}) |
        (Kw("temperature") ~ Temp.filter(_ => temperature.isEmpty).map{t => temperature = Some(t); ()}) |
        (Kw("humidity") ~ Data.Num.filter(_ => humidity.isEmpty))
      ).rep("," ~! Pass) ~
      "}"
    ).map(_ => Metadata(lab, who, timestamp, temperature, humidity, arena, food, media, sex, stage, age, strain, protocol, software, settings, custom))
  }

  def apply(s: String) = {
    var commons = false
    var u: Option[collection.Map[String, units.Units]] = None
    var m: Option[Metadata] = None
    var d: Option[Data] = None
    val at = new collection.mutable.AnyRefMap[String, Any]
  }
}
