package org.openworm.trackercommons

case class DataSet(meta: Metadata, unitmap: UnitMap, data: Array[Either[Datum, Data]], files: FileSet, custom: json.ObjJ)
extends json.Jsonable {
  def toObjJ = unitmap.unfix(json.ObjJ({
    var m = Map(
      "units" -> (unitmap.toObjJ :: Nil),
      "data" -> (json.ArrJ(data.map{ case Left(dm) => dm.toObjJ; case Right(da) => da.toObjJ }) :: Nil)
    )
    if (meta != Metadata.empty) m = m + ("metadata" -> (meta.toObjJ :: Nil))
    if (files != FileSet.empty) m = m + ("files" -> (files.toObjJ :: Nil))
    m ++ custom.keyvals
  }))
}
object DataSet {
  def dataEntry(ob: json.ObjJ): Either[String, Either[Datum, Data]] = {
    Datum.from(ob) match {
      case Right(x) => Right(Left(x))
      case Left(e) => if (e.endsWith("!")) Left(e) else Data.from(ob) match {
        case Right(x) => Right(Right(x))
        case Left(e) => Left(e)
      }
    }
  }

  def from(s: String): Either[String, DataSet] = Parser(s)

  def from(f: java.io.File): Either[String, DataSet] = {
    try {
      val src = scala.io.Source.fromFile(f)
      try { from(src.mkString) }
      finally { src.close }
    }
    catch { case scala.util.control.NonFatal(t) => Left("File IO error on "+f.getPath+"\n"+t.toString) }
  }
}
