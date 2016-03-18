package org.openworm.trackercommons

import scala.util._
import scala.util.control.NonFatal

object ReadWrite {
  def read(s: String): Either[String, DataSet] = read(new java.io.File(s))

  def read(f: java.io.File): Either[String, DataSet] = {
    try {
      val s = scala.io.Source.fromFile(f)
      try { Parser(s.mkString).left.map(err => s"Could not read ${f.getPath} because\n$err") }
      finally{ s.close }
    }
    catch { case NonFatal(_) => Left("Could not read " + f.getPath) }
  }

  def readAll(s: String): Either[String, Vector[DataSet]] = readAll(new java.io.File(s))

  def readAll(f: java.io.File): Either[String, Vector[DataSet]] = read(f) match {
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
          val di = read(fi) match {
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

  def write(ds: DataSet, file: String): Either[String, Unit] = write(ds, new java.io.File(file))

  def write(ds: DataSet, root: String, file: String): Either[String, Unit] = write(ds, new java.io.File(root), file)

  def write(ds: DataSet, f: java.io.File): Either[String, Unit] = write(ds, f.getParentFile, f.getName)

  def write(ds: DataSet, f: java.io.File, fname: String): Either[String, Unit] = {
    val lines = ds.copy(files = FileSet(Vector(fname), 0, json.ObjJ.empty)).toObjJ.toJsons
    val fout = new java.io.File(f, fname)
    writeLinesTo(lines, fout)
  }

  private def writeLinesTo(lines: Vector[json.Indented], fout: java.io.File): Either[String, Unit] = {
    val pw = Try{ new java.io.PrintWriter(fout) } match {
      case Success(x) => x
      case Failure(e) => return Left("Could not open output file " + fout)
    }
    try {
      lines.foreach(pw.println)
      Right(())
    }
    catch {
      case e: Exception => Left("Error while writing file " + fout + "\n" + e.toString + Option(e.getMessage).getOrElse(""))
    }
    finally Try { pw.close }
  }

  def writeAll(root: java.io.File, sets: Vector[(DataSet, String)]): Either[String, Unit] = {
    val fileset = sets.map(_._2)
    sets.zipWithIndex.foreach{ case ((ds, f), i) =>
      val lines = ds.copy(files = FileSet(fileset, i, json.ObjJ.empty)).toObjJ.toJsons
      val fout = new java.io.File(root, f)
      writeLinesTo(lines, fout) match {
        case x: Left[String, Unit] => return x
        case _ =>
      }
    }
    Right(())
  }
}
