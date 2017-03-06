package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

case class DataSet(meta: Metadata, unitmap: UnitMap, data: Array[Either[Datum, Data]], files: FileSet, custom: Json.Obj)
extends AsJson {
  var sourceFile: Option[java.io.File] = None
  def withSourceFile(f: java.io.File) = { sourceFile = Some(f); this }
  def withNoSource: this.type = { sourceFile = None; this }
  def json = unitmap.unfix(
    Json
    ~ ("units", unitmap)
    ~ ("data", Json(data.map(e => Json either e)))
    ~? ("metadata", if (meta == Metadata.empty) None else Some(meta))
    ~? ("files", if (files == FileSet.empty) None else Some(files))
    ~~ custom
    ~ Json,
    DataSet.convertedParts
  )
}
object DataSet extends FromJson[DataSet] {
  def apply(meta: Metadata, unitmap: UnitMap, data: Array[Data], files: FileSet = FileSet.empty, custom: Json.Obj = Json.Obj.empty): DataSet =
    new DataSet(meta, unitmap, data.map(x => Right(x)), files, custom)

  val convertedParts = UnitMap.Nested(Map(
    ("files", UnitMap.OnlyCustom),
    ("data", UnitMap.Leaves(Set("walk"))),
    ("metadata", UnitMap.Leaves(Set("lab", "arena", "software")))
  ))

  def parse(j: Json): Either[JastError, DataSet] = {
    implicit val parseDatum: FromJson[Datum] = Datum
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
    
    val d = o("data").to[Array[Either[Datum, Data]]] match {
      case Right(x) => x
      case Left(e) => return Left(JastError("Error parsing data in WCON data set", because = e))
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
