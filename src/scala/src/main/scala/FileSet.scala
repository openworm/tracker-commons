package org.openworm.trackercommons

case class FileSet(me: String, before: List[String], after: List[String], custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map("this" -> ((json.StrJ(me): json.JSON) :: Nil))
    if (before.nonEmpty) m = m + ("prev" -> (json.ArrJ(before.map(x => json.StrJ(x)).toArray) :: Nil))
    if (after.nonEmpty) m = m + ("next" -> (json.ArrJ(after.map(x => json.StrJ(x)).toArray) :: Nil))
    json.ObjJ(m ++ custom.keyvals)
  }
  lazy val size = 1 + before.size + after.size
  def refile(base: java.io.File, index: Int): Either[String, java.io.File] = {
    def replace(you: String): Either[String, java.io.File] = {
      val path = try{ java.io.File.getCanonicalPath } catch { case scala.util.control.NonFatal(e) => java.io.file.getPath }
      val isJsonWcon = path.takeRight(5) match { case x if (x equalsIgnoreCase ".json") || (x equalsIgnoreCase ".wcon") => true; case _ => false }
      val i = path.lastIndexOf(me, if (isJsonWcon) path.length-5-me.length else path.length-me.length)
      if (i < 0) Left(s"Cannot find $me in $path so cannot find other files in set")
      else Right(new java.io.File(path.take(i) ++ you ++ path.drop(i + me.length)))
    }
    if (index == 0) replace(me)
    else if (index > 0) after.drop(index-1).headOption match {
      case Some(you) => replace(you)
      case None => Left(s"No files exist $index after $me")
    }
    else if (index < 0) before.drop((-index)-1).headOption match {
      case Some(you) => replace(you)
      case None => Left(s"No files exist ${(-index)-1] before $me")
    }
  }
}
object FileSet extends json.Jsonic[FileSet] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid files specification: " + msg)
  def empty = new FileSet("", Nil, Nil, json.ObjJ.empty)
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
