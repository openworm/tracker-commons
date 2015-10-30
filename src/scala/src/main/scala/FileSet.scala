package org.openworm.trackercommons

case class FileSet(me: String, before: List[String], after: List[String], custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map("this" -> ((json.StrJ(me): json.JSON) :: Nil))
    if (before.nonEmpty) m = m + ("prev" -> (json.ArrJ(before.map(x => json.StrJ(x)).toArray) :: Nil))
    if (after.nonEmpty) m = m + ("next" -> (json.ArrJ(after.map(x => json.StrJ(x)).toArray) :: Nil))
    json.ObjJ(m ++ custom.keyvals)
  }
}
object FileSet extends json.Jsonic[FileSet] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid files specification: " + msg)
  def empty = new FileSet("", Nil, Nil, Metadata.emptyObjJ)
  def from(ob: json.ObjJ): Either[String, FileSet] = {
    val me = ob.keyvals.get("this") match {
      case None => return BAD("extension for this file not specifed")
      case Some(json.StrJ(x) :: Nil) => x
      case _ => return BAD("file extension must be exactly one string")
    }
    val List(pv, nx) = List("prev", "next").map(ident => ob.keyvals.get(ident) match {
      case None => Nil
      case Some(js) => js.flatMap{
        case json.NullJ => Nil
        case json.StrJ(x) => x :: Nil
        case json.ArrJ(xs) => xs.map{
          case json.StrJ(x) => x
          case _ => return BAD("non-text file name in " + ident)
        }
        case _ => return BAD("non-text file name in " + ident)
      }.toList
    })
    Right(new FileSet(me, pv, nx, Metadata.getCustom(ob)))
  }
}
