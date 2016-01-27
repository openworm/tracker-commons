package org.openworm.trackercommons

case class DataSet(meta: Metadata, unitmap: UnitMap, data: Array[Either[Datum, Data]], files: FileSet, custom: json.ObjJ)
extends json.Jsonable {
  var sourceFile: Option[java.io.File] = None
  def withSourceFile(f: java.io.File) = { sourceFile = Some(f); this }
  def withNoSource: this.type = { sourceFile = None; this }
  def toObjJ = unitmap.unfix(json.ObjJ({
    var m = Map(
      "units" -> (unitmap.toObjJ :: Nil),
      "data" -> (json.ArrJ(data.map{ case Left(dm) => dm.toObjJ; case Right(da) => da.toObjJ }) :: Nil)
    )
    if (meta != Metadata.empty) m = m + ("metadata" -> (meta.toObjJ :: Nil))
    if (files != FileSet.empty) m = m + ("files" -> (files.toObjJ :: Nil))
    m ++ custom.keyvals
  }))

  def combined(
    combiner: collection.Map[String, (Array[List[json.JSON]], Array[Array[Double]]) => Option[List[json.JSON]]] = Map.empty
  ): Either[String, Array[Data]] = {
    Right(data.
      map{ case Right(x) => x; case Left(x) => x.toData }.
      groupBy(_.idEither).
      iterator.map(_._2.filter(_.ts.length > 0).sortBy(_.ts(0))).filter(_.length > 0).toArray.
      map{ datas =>
        if (datas.length == 1) datas(0)
        else {
          for (i <- datas.indices.drop(1)) {
            val l = datas(i-1)
            val r = datas(i)
            if (!(l.ts(l.ts.length-1) < r.ts(0))) return Left(s"Data timepoints overlap on animal ${l.idJSON} near t=${r.ts(0)}")
          }
          val ts, cxs, cys = new Array[Double](datas.map(_.ts.length).sum)
          val xs, ys = new Array[Array[Float]](ts.length)
          var i = 0
          for (data <- datas) {
            System.arraycopy(data.ts, 0, ts, i, data.ts.length)
            System.arraycopy(data.xs, 0, xs, i, data.xs.length)
            System.arraycopy(data.ys, 0, ys, i, data.ys.length)
            System.arraycopy(data.cxs, 0, cxs, i, data.cxs.length)
            System.arraycopy(data.cys, 0, cys, i, data.cys.length)
            i += data.ts.length
          }
          val labels = collection.mutable.Set.empty[String]
          for { data <- datas; (k,_) <- data.custom.keyvals } labels += k
          labels.toArray.foreach{ key => if (!(combiner contains key)) labels -= key }
          val custom =
            if (labels.isEmpty) json.ObjJ.empty
            else {
              val tss = datas.map(_.ts)
              json.ObjJ(
                labels.map{ label =>
                  val combo = combiner(label)
                  val values = datas.map(_.custom.keyvals.getOrElse(label, Nil))
                  val merged = combo(values, tss) match {
                    case None => return Left(s"Failed to merge ${tss.length} custom keys of type $label for animal ${datas.head.idJSON}")
                    case Some(x) => x
                  }
                  label -> merged
                }.toMap
              )
            }
          val cX = datas.forall(_.derivedCx)
          val cY = datas.forall(_.derivedCy)
          Data(datas(0).nid, datas(0).sid, ts, xs, ys, cxs, cys, custom).setDerived(cX, cY)
        }
      }
    )
  }
}
object DataSet {
  def apply(meta: Metadata, unitmap: UnitMap, data: Array[Data], files: FileSet = FileSet.empty, custom: json.ObjJ = json.ObjJ.empty): DataSet =
    new DataSet(meta, unitmap, data.map(x => Right(x)), files, custom)

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
