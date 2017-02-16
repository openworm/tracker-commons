package org.openworm.trackercommons

import scala.util._
import scala.util.control.NonFatal

import kse.jsonal._
import kse.jsonal.JsonConverters._

object ReadWrite {
  private implicit val parseDataSet: FromJson[DataSet] = DataSet

  def read(s: String): Either[String, DataSet] = read(new java.io.File(s))

  def read(f: java.io.File): Either[String, DataSet] = {
    if (f.getName.toLowerCase.endsWith(".zip")) return readZip(f)
    try {
      Jast.parse(f).to[DataSet].left.map(err => s"Could not read ${f.getPath} because\n$err")
    }
    catch { case NonFatal(e) => Left("Could not read " + f.getPath + " because of a " + e.getClass.getName + "\nDetails:\n" + e.getStackTrace.mkString("\n")) }
  }

  def readZip(f: java.io.File): Either[String, DataSet] = {
    if (!f.getName.toLowerCase.endsWith(".zip")) return Left("Zipped WCON files must have extension `.zip`")
    try {
      val zf = new java.util.zip.ZipFile(f)
      try {
        val i = zf.entries
        var theEntry: Option[java.util.zip.ZipEntry] = None
        while (i.hasMoreElements) {
          val ze = i.nextElement
          // Favor WCON over JSON over whatever file
          if (ze.getName.toLowerCase.endsWith(".wcon")) {
            if (theEntry.exists(_.getName.toLowerCase.endsWith(".wcon")))
              return Left(f"Found two possible WCON files\n  ${theEntry.get.getName}\n  ${ze.getName}\nand don't know which to read.")
            else theEntry = Some(ze)
          }
          else if (ze.getName.toLowerCase.endsWith(".json") && !theEntry.exists(_.getName.toLowerCase.endsWith(".wcon"))) {
            if (theEntry.exists(_.getName.toLowerCase.endsWith(".json")))
              return Left(f"Found two possible WCON files\n  ${theEntry.get.getName}\n  ${ze.getName}\nand don't know which to read.")
              else theEntry = Some(ze)
          }
          else if (theEntry.isEmpty) theEntry = Some(ze)
        }
        if (theEntry.isEmpty) return Left("Could not find any files in zip file " + f.getPath)
        val s = scala.io.Source.fromInputStream(zf.getInputStream(theEntry.get))
        try { Jast.parse(s.mkString).to[DataSet].left.map(err => f"Could not read ${f.getPath} because \n$err") }
        finally s.close
      }
      finally zf.close
    }
    catch { case NonFatal(_) => Left("Could not read zip file " + f.getPath) }
  }

  def readAll(s: String): Either[String, Vector[DataSet]] = readAll(new java.io.File(s))

  def readAll(f: java.io.File): Either[String, Vector[DataSet]] =
    if (f.getName.toLowerCase.endsWith(".zip")) readAllZip(f)
    else read(f) match {
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

  private def orderAndCheckConsistency(ds: Vector[DataSet], names: Vector[String]): Either[String, Vector[DataSet]] = {
    if (ds.length <= 1) return Right(ds)

    val nameset = names.toSet
    if (nameset.size != names.length)
      return Left("Huh?!  Duplicate names in zip file???  " + names.groupBy(identity).filter(_._2.length > 1).map(_._1).mkString(", "))
    val lookup = names.zipWithIndex.toMap
    val withNames = (ds zip names).map{ case (di, ni) =>
      val fi = di.files.names
      if (fi.isEmpty) return Left(s"No files listed in $ni")
      val i = FileSet.indexInPath(ni, fi(di.files.index))
      if (i < 0) return Left(s"Mismatch between stated and actual name: $fi not part of $ni")
      else {
        val pre = ni.take(i)
        val post = ni.drop(i + fi(di.files.index).length)
        val ans = fi.map(x => pre + x + post)
        val missing = ans.toSet diff nameset
        if (missing.nonEmpty) return Left(f"Files referenced in $ni that do not appear in zip: ${missing.toList.sorted.mkString(", ")}")
        (di, ans, di.files.index, ni)
      }
    }
    val threading = new Array[Int](ds.length)
    threading(0) = ds.indexWhere(_.files.index == 0)
    if (threading(0) < 0) return Left("Cannot find any file that claims to be first in the list out of "+names.mkString(", "))
    var i = 1
    while (i < threading.length) {
      val (di, ns, ii, ni) = withNames(threading(i-1))
      if (ii >= ni.length - 1) return Left("Nothing reported to come after $ni but still ${threading.length-i} files left?")
      threading(i) = lookup(ns(ii+1))
      i += 1
    }
    if (threading.toSet.size != threading.length) return Left("Cycles found in file order of "+names.mkString(", "))
    if (withNames(threading(threading.length - 1)) match { case (_, ns, ii, _) => (ii < ns.length-1) })
      return Left(f"Last file in list should be ${withNames(threading(threading.length-1))._4} but it says there are more?")
    val threaded = (withNames zip threading).sortBy(_._2)
    val misorderings = threaded.flatMap{ case ((di, ns, ii, ni), i) =>
      if (ii > i)
        Some("Files specified before first file: " + ns.take(ii-i).mkString(", "))
      else if (ns.length - ii > threaded.length - i)
        Some("Files specified after last file: " + ns.takeRight((ns.length - ii) - (threaded.length - i)))
      else {
        ns.zipWithIndex.
          collect{ case (nj, j) if threaded(i + j - ii)._1._4 != nj => nj + " != " + threaded(i + j - ii)._1._4 }.
          reduceLeftOption(_ + _).
          map(nx => "Order of files does not agree: " + nx.mkString(", "))
      }
    }
    misorderings match {
      case mo if mo.nonEmpty => Left(mo.mkString("\n"))
      case _ => Right(ds)
    }
  }

  def readAllZip(zip: java.io.File): Either[String, Vector[DataSet]] = {    
    if (!zip.getName.toLowerCase.endsWith(".zip")) return Left("Zipped WCON files must have extension `.zip`")
    try {
      val zf = new java.util.zip.ZipFile(zip)
      try {
        val i = zf.entries
        var theEntries: Vector[java.util.zip.ZipEntry] = Vector.empty
        while (i.hasMoreElements) {
          val ze = i.nextElement
          // Favor WCON over JSON over whatever file
          if (ze.getName.toLowerCase.endsWith(".wcon")) {
            if (theEntries.exists(_.getName.toLowerCase.endsWith(".wcon"))) theEntries = theEntries :+ ze
            else theEntries = Vector(ze)
          }
          else if (ze.getName.toLowerCase.endsWith(".json") && !theEntries.exists(_.getName.toLowerCase.endsWith(".wcon"))) {
            if (theEntries.exists(_.getName.toLowerCase.endsWith(".json"))) theEntries = theEntries :+ ze
            else theEntries = Vector(ze)
          }
        }
        if (theEntries.isEmpty) return Left("Could not find any WCON files in zip file " + zip.getPath)
        val results = theEntries.map{ theEntry =>
          val s = scala.io.Source.fromInputStream(zf.getInputStream(theEntry))
          try { Jast.parse(s.mkString).to[DataSet] match {
            case Right(x) => x
            case Left(err) => return Left(f"Could not read ${theEntry.getName} in ${zip.getPath} because \n$err")
          }}
          finally s.close
        }
        orderAndCheckConsistency(results, theEntries.map(_.getName))
      }
      finally zf.close
    }
    catch { case NonFatal(e) => Left(f"Could not read WCON from zip file ${zip.getPath}\n  ${e.getClass.getName} ${e.printStackTrace; e.getMessage}") }
  }

  def write(ds: DataSet, file: String): Either[String, Unit] = write(ds, new java.io.File(file))

  def write(ds: DataSet, root: String, file: String): Either[String, Unit] = write(ds, new java.io.File(root), file)

  def write(ds: DataSet, f: java.io.File): Either[String, Unit] = write(ds, f.getParentFile, f.getName)

  def write(ds: DataSet, f: java.io.File, fname: String): Either[String, Unit] =
    writeUnfixed(ds.copy(files = FileSet(Vector(fname), 0, Json.Obj.empty)), f, fname)

  def writeUnfixed(ds: DataSet, f: java.io.File, fname: String): Either[String, Unit] = {
    val fout = new java.io.File(f, fname)
    val pw = Try { new java.io.PrintWriter(fout) } match {
      case Success(x) => x
      case Failure(e) => return Left("Could not open output file " + fout)
    }
    try { pw.print( PrettyJson(ds.json) ); Right(()) }
    catch { case NonFatal(e) => Left("Error while writing file " + fout + "\n" + e.toString + Option(e.getMessage).getOrElse("")) }
    finally Try { pw.close }
  }

  def writeZip(ds: DataSet, zip: java.io.File, fname: String): Either[String, Unit] = {
    try {
      val fos = new java.io.FileOutputStream(
        if (zip.getName.toLowerCase.endsWith(".zip")) zip
        else new java.io.File(zip.getPath + ".zip")
      )
      try {
        val zos = new java.util.zip.ZipOutputStream(fos)
        try {
          val ze = new java.util.zip.ZipEntry(fname)
          zos.putNextEntry(ze)
          zos.write( PrettyJson.bytes(ds.copy(files = FileSet(Vector(fname), 0, Json.Obj.empty)).json) )
          zos.closeEntry
        }
        finally zos.close
      }
      finally fos.close
      Right(())
    }
    catch {
      case NonFatal(e) => Left("Error while writing zip file " + zip + "\n" + e.toString + Option(e.getMessage).getOrElse(""))
    }
  }

  def writeAll(root: java.io.File, sets: Vector[(DataSet, String)]): Either[String, Unit] = {
    val fileset = sets.map(_._2)
    sets.zipWithIndex.foreach{ case ((ds, f), i) =>
      writeUnfixed(ds.copy(files = FileSet(fileset, i, Json.Obj.empty)), root, f) match {
        case x: Left[String, Unit] => return x
        case _ =>
      }
    }
    Right(())
  }

  def writeAllZip(zip: java.io.File, sets: Vector[(DataSet, String)]): Either[String, Unit] = {
    val fileset = sets.map(_._2)
    try {
      val fos = new java.io.FileOutputStream(
        if (zip.getName.toLowerCase.endsWith(".zip")) zip
        else new java.io.File(zip.getPath + ".zip")
      )
      try {
        val zos = new java.util.zip.ZipOutputStream(fos)
        try {
          sets.zipWithIndex.foreach{ case ((ds, fname), i) =>
            val ze = new java.util.zip.ZipEntry(fname)
            zos.putNextEntry(ze)
            zos.write( PrettyJson.bytes(ds.copy(files = new FileSet(fileset, i, Json.Obj.empty)).json) )
            zos.closeEntry
          }
        }
        finally zos.close
      }
      finally fos.close
      Right(())
    }
    catch {
      case NonFatal(e) => Left("Error while writing zip file " + zip + "\n" + e.toString + Option(e.getMessage).getOrElse(""))
    }    
  }
}
