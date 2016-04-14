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
    ~ ("data", data.map(e => Json either e))
    ~? ("metadata", if (meta == Metadata.empty) None else Some(meta))
    ~? ("files", if (files == FileSet.empty) None else Some(files))
    ~~ custom ~ Json
  )

  def combined(
    combiner: collection.Map[String, (Array[List[Json]], Array[Array[Double]]) => Option[List[Json]]] = Map.empty
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
          for { data <- datas } data.custom.foreach{ (k,_) => labels += k }
          labels.toArray.foreach{ key => if (!(combiner contains key)) labels -= key }
          val custom =
            if (labels.isEmpty) Json.Obj.empty
            else throw new UnsupportedOperationException("Cannot merge custom labels.")
          val ic = datas.forall(_.independentC)
          val so = datas.exists(_.specifiedO)
          Data(datas(0).nid, datas(0).sid, ts, xs, ys, cxs, cys, custom).setCO(ic, so)
        }
      }
    )
  }
}
object DataSet extends FromJson[DataSet] {
  def apply(meta: Metadata, unitmap: UnitMap, data: Array[Data], files: FileSet = FileSet.empty, custom: Json.Obj = Json.Obj.empty): DataSet =
    new DataSet(meta, unitmap, data.map(x => Right(x)), files, custom)

  def parse(j: Json): Either[JastError, DataSet] = {
    implicit val parseDatum: FromJson[Datum] = Datum
    implicit val parseData: FromJson[Data] = Data

    val o = j match { 
      case jo: Json.Obj => jo
      case _ => return Left(JastError("Not a JSON object so not a WCON data set"))
    }
    val u = o("units").to(UnitMap) match {
      case Right(x) => x
      case Left(e) => return Left(JastError("Error parsing units in WCON data set", because = e))
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
