package org.openworm.trackercommons

import scala.util.control.NonFatal

object ReadWrite {
  def apply(s: String): Either[String, DataSet] = apply(new java.io.File(s))

  def apply(f: java.io.File): Either[String, DataSet] = {
    try {
      val s = scala.io.Source.fromFile(f)
      try { Parser(s.mkString).left.map(err => s"Could not read ${f.getPath} because\n$err") }
      finally{ s.close }
    }
    catch { case NonFatal(_) => Left("Could not read " + f.getPath) }
  }

  def all(s: String): Either[String, Vector[DataSet]] = all(new java.io.File(s))

  def all(f: java.io.File): Either[String, Vector[DataSet]] = apply(f) match {
    case Left(err) => Left(err)
    case Right(dset) =>
      if (dset.files.size <= 1) Right(Vector(dset))
      else {
        var ds = Vector(dset)
        var fs = dset.files
        fs.setRootFile(f)
        var i0 = 0
        var i1 = 0
        while (fs.indices.contains(i0) || fs.indices.contains(i1)) {
          val i = if (fs.indices.contains(i0-1)) i0-1 else i1+1
          val fi = fs.lookup(i) match {
            case None => return Left("Extended data set was supposed to contain ${fs.you(i)} but can't find corresponding file")
            case Some(x) => x
          }
          val di = apply(fi) match {
            case Left(err) => return Left(err)
            case Right(x) => x
          }
          val fsi = di.files
          fsi.setRootFile(fi)
          if (fs.indices.contains(i-1) && !fsi.indices.contains(-1))
            return Left("Expected to find ${fs.you(i-1)} data set but not according to\n  $fi")
          if (fs.indices.contains(i+1) && !fsi.indices.contains(1))
            return Left("Expected to find ${fs.you(i+1)} data set but not according to\n  $fi")
          fs = fs.join(i, fsi) match {
            case Left(err) => return Left(err)
            case Right(x) => x
          }
          if (i < i0) {
            i0 = i
            ds = di +: ds
          }
          else {
            i1 = i
            ds = ds :+ di
          }
        }
        Right(ds)
      }
  }
}
