package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

case class DataSet(meta: Metadata, unitmap: UnitMap, data: Array[Data], files: FileSet = FileSet.empty, custom: Json.Obj = Json.Obj.empty)
extends AsJson with Customizable[DataSet] {
  def foreach[U](f: Data => U) { data.foreach(f) }
  def map(f: Data => Data) = new DataSet(meta, unitmap, data.map(f), files, custom)
  def flatMap(f: Data => Option[Data]) = new DataSet(meta, unitmap, data.flatMap(x => f(x)), files, custom)

  def groupByIDs(unshaped: Option[collection.mutable.ArrayBuffer[Custom.Unshaped]] = None): DataSet = {
    val groups = data.zipWithIndex.groupBy(_._1.id).toMap
    if (groups.size == data.length) this
    else {
      val groupedData = groups.map{ case (_, vs) => 
        val ix = vs.map(_._2).min
        val ds = vs.map(_._1)
        val re = Reshape.sortSet(ds.map(_.ts), deduplicate = true)
        if (unshaped.nonEmpty) {
          val u = new Custom.Unshaped
          val join = Data.join(re, ds, unshaped = Some(u))
          if (u.mistakes.nonEmpty) unshaped.get += u
          ix -> join
        }
        else ix -> Data.join(re, ds)
      }.toArray.sortBy(_._1).flatMap(_._2)
      this.copy(data = groupedData)
    }
  }

  def customFn(f: Json.Obj => Json.Obj) = copy(custom = f(custom))

  def json = unitmap.unfix(
    Json
    ~ ("units", unitmap)
    ~ ("data", data)
    ~? ("metadata", if (meta == Metadata.empty) None else Some(meta))
    ~? ("files", if (files == FileSet.empty) None else Some(files))
    ~~ custom
    ~ Json,
    DataSet.convertedParts
  )
}
object DataSet extends FromJson[DataSet] {
  val empty = new DataSet(Metadata.empty, UnitMap(Map.empty, Json.Obj.empty), new Array[Data](0))

  val convertedParts = UnitMap.Nested(Map(
    ("files", UnitMap.OnlyCustom),
    ("data", UnitMap.Leaves(Set("walk"))),
    ("metadata", UnitMap.Leaves(Set("lab", "arena", "software", "interpolate")))
  ))

  def join(dses: Array[DataSet]): Either[String, DataSet] = {
    val m = Metadata.join(dses.map(_.meta)) match {
      case Right(x) => x
      case Left(e) => return Left(e)
    }
    val u = UnitMap.join(dses.map(_.unitmap)) match {
      case Right(x) => x
      case Left(e) => return Left(e)
    }
    val d = dses.flatMap(_.data)

    // Files we only calculate for validity and custom information; cannot actually retain all the separate files!
    val f =
      if (dses.forall(ds => ds.files.next.isEmpty && ds.files.prev.isEmpty)) FileSet.linked(dses.map(_.files)) match {
        case Some(x) => x
        case None    => return Left("Could not create sensible file information")
      }
      else FileSet.join(dses.map(_.files)) match {
        case Right(x) => x
        case Left(e) => return Left(e)
      }

    val c = Custom.accumulate(dses.map(_.custom)) match {
      case Some(x) => x
      case None => return Left("Incompatible custom information in WCON files (top level)")
    }
    Right(new DataSet(m, u, d, if (f.custom.size > 0) FileSet.empty.copy(custom = f.custom) else FileSet.empty, c))
  }

  def parse(j: Json): Either[JastError, DataSet] = {
    implicit val parseData: FromJson[Data] = Data

    val (o, u) = j match { 
      case jo: Json.Obj =>
        val ux = jo("units").to(UnitMap) match {
          case Right(x) => x
          case Left(e) => return Left(JastError("Error parsing units in WCON data set", because = e))
        }
        val jx = ux.fix(jo, convertedParts) match {
          case x: Json.Obj => x
          case _           => return Left(JastError("Did not get a JSON object after unit converting a JSON object???"))
        }
        (jx, ux)
      case _ => return Left(JastError("Not a JSON object so not a WCON data set"))
    }
    
    val d = o("data") match {
      case jo: Json.Obj => jo.to[Data] match {
        case Right(x) => Array(x)
        case Left(e) => return Left(JastError("Error parsing single data entry in WCON", because = e))
      }
      case jj => jj.to[Array[Data]] match {
        case Right(x) => x
        case Left(e) => return Left(JastError("Error parsing data array in WCON", because = e))
      }
    }
    val f = o.get("files").map(_.to(FileSet)) match {
      case None => FileSet.empty
      case Some(Right(x)) => x
      case Some(Left(e)) => return Left(JastError("Error parsing file information in WCON data set", because = e))
    }
    val m = o.get("metadata").map(_.to(Metadata)) match {
      case None => Metadata.empty
      case Some(Right(x)) => x
      case Some(Left(e)) => return Left(JastError("Error parsing metadata in WCON data set", because = e))
    }
    Right(new DataSet(m, u, d, f, o.filter((k,_) => k.startsWith("@"))))
  }
}
