package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

case class DataSet(meta: Metadata, unitmap: UnitMap, data: Array[Data], files: FileSet = FileSet.empty, custom: Json.Obj = Json.Obj.empty)
extends AsJson with Customizable[DataSet] {
  def foreach[U](f: Data => U) { data.foreach(f) }
  def map(f: Data => Data) = new DataSet(meta, unitmap, data.map(f), files, custom)
  def flatMap(f: Data => Option[Data]) = new DataSet(meta, unitmap, data.flatMap(x => f(x)), files, custom)

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
