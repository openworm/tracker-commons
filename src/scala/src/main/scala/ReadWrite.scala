package org.openworm.trackercommons

import scala.util._
import scala.util.control.NonFatal

import kse.jsonal._
import kse.jsonal.JsonConverters._

object ReadWrite {
  private implicit val parseDataSet: FromJson[DataSet] = DataSet

  private def errorMessaging(e: java.lang.Throwable, p: java.nio.file.Path, zip: Option[java.io.File]): String = {
    val in = zip match { case Some(z) => f" in ${z.getPath}"; case None => "" }
    val detail = "\n" + e.toString + "\n"
    val stack = e.getStackTrace.mkString("Stack trace:\n", "\n", "\n")
    f"Could not read ${p}$in because of a ${e.getClass.getName}.$detail$stack}"
  }

  private def readFromPath(p: java.nio.file.Path, zip: Option[java.io.File]): Either[String, DataSet] = {
    try {
      val fis = java.nio.file.Files.newInputStream(p, java.nio.file.StandardOpenOption.READ)
      try {
        Jast.parse(fis).to[DataSet].left.map{ e => 
          throw new IllegalArgumentException("JSON does not encode a DataSet:\n"+e.toString+"\n")
        }
      }
      finally { fis.close }
    }
    catch { case NonFatal(e) => Left(errorMessaging(e, p, zip)) }
  }

  def readOne(f: java.io.File): Either[String, DataSet] = {
    if (f.getName.toLowerCase.endsWith(".zip")) {
      import java.nio.file.FileVisitResult.{CONTINUE, TERMINATE}
      import java.nio.file.{Files, Path, SimpleFileVisitor, FileSystems}
      import java.nio.file.attribute.{BasicFileAttributes => BfAttr}
      val sys = FileSystems.newFileSystem(f.toPath, null)
      try {
        var uniqueWcon = List.empty[Path]
        Files.walkFileTree(sys.getPath("/"), new SimpleFileVisitor[Path] {
          override def visitFile(path: Path, attr: BfAttr) = {
            if (path.getFileName.endsWith(".wcon") && !attr.isDirectory) {
              val uhoh = uniqueWcon.isEmpty
              uniqueWcon = path :: uniqueWcon
              if (uhoh) TERMINATE else CONTINUE
            }
            else CONTINUE
          }
        })
        uniqueWcon match {
          case Nil         => Left(f"No .wcon file found in ${f.getPath}")
          case p :: Nil    => readFromPath(p, Some(f))
          case p :: q :: _ => Left(f"More than one .wcon file in ${f.getPath}: $p and $q.  Perhaps you meant to `readAll`?")
        }
      }
      catch {
        case NonFatal(e) => Left(errorMessaging(e, f.toPath, None))
      }
      finally {
        try { sys.close } catch { case NonFatal(_) => }
      }
    }
    else readFromPath(f.toPath, None)
  }

  def readOne(s: String): Either[String, DataSet] = readOne(new java.io.File(s))

  def readAll(f: java.io.File): Either[String, Array[DataSet]] = {
    import java.nio.file.FileVisitResult.{CONTINUE, TERMINATE, SKIP_SUBTREE}
    import java.nio.file.{Files, Path, SimpleFileVisitor, FileSystem, FileSystems}
    import java.nio.file.attribute.{BasicFileAttributes => BfAttr}
    var zippy: Option[FileSystem] = None
    try {
      val root =
        if (f.isDirectory) f.toPath
        else if (f.getName.endsWith(".zip")) {
          zippy = Some(FileSystems.newFileSystem(f.toPath, null))
          zippy.get.getPath("/")
        }
        else if (f.getName.endsWith(".wcon")) return readOne(f).right.map(ds => Array(ds))
        else return Left(f"Can only read all from a directory, zip file, or single wcon file, not ${f.getPath}")
      val wset = {
        val pb = Array.newBuilder[Path]
        var depth = 0
        Files.walkFileTree(root, new SimpleFileVisitor[Path] {
          override def preVisitDirectory(dir: Path, attr: BfAttr) = {
            if (depth >= 2) SKIP_SUBTREE
            else { depth += 1; CONTINUE }
          }
          override def postVisitDirectory(dir: Path, ioe: java.io.IOException) = { 
            if (depth > 0) depth -= 1
            CONTINUE
          }
          override def visitFile(file: Path, attr: BfAttr) = {
            if (file.getFileName.endsWith(".wcon")) pb += file
            else if (zippy.isEmpty && file.getFileName.endsWith(".wcon.zip")) pb += file
            CONTINUE
          }
        })
        pb.result
      }
      val prefixes = wset.map(_.getParent).toSet
      if (prefixes.size == 0) return Right(Array.empty)
      if (prefixes.size > 1)
        return Left(f".wcon files found in multiple directories: ${prefixes.toList.map(_.toString).sorted.mkString(", ")}")
      val dses = wset.map(w => readFromPath(w, zippy.map(_ => f)) match {
        case Right(ds) => ds
        case Left(e) => return Left(e)
      })
      val fses = dses.map(ds => ds.files.current -> ds).toMap
      if (fses.size != dses.length) return Left(f"Duplicate filenames found in files in ${f.getPath}")
      val fsone = FileSet.join(dses.map(_.files)) match {
        case Left(e) => return Left("Error while reading .wcon files in ${f.getPath}\n" + e)
        case Right(fs) => fs
      }
      Right(fsone.iterator.toArray.map(fs => fses(fs)))
    }
    finally {
      zippy.foreach(fs => try { fs.close } catch { case NonFatal(_) => })
    }
  }

  def write(ds: DataSet, file: String): Either[String, Unit] = write(ds, new java.io.File(file))

  def write(ds: DataSet, root: String, file: String): Either[String, Unit] = write(ds, new java.io.File(root), file)

  def write(ds: DataSet, f: java.io.File): Either[String, Unit] = write(ds, f.getParentFile, f.getName)

  def write(ds: DataSet, f: java.io.File, fname: String): Either[String, Unit] =
    writeUnfixed(ds.copy(files = FileSet(fname, Array.empty, Array.empty, Json.Obj.empty)), f, fname)

  def writeUnfixed(ds: DataSet, f: java.io.File, fname: String): Either[String, Unit] = {
    val fout = new java.io.File(f, fname)
    val pw = Try { new java.io.PrintWriter(fout) } match {
      case Success(x) => x
      case Failure(e) => return Left("Could not open output file " + fout)
    }
    try { pw.print( PrettyJson(ds.json) ); Right(()) }
    catch { case NonFatal(e) => e.printStackTrace; Left("Error while writing file " + fout + "\n" + e.toString + Option(e.getMessage).getOrElse("")) }
    finally Try { pw.close }
  }

  def writeChunkedZip(
    ds: DataSet, zip: java.io.File, name: String,
    maxEntries: Int = 500000, timeMajor: Boolean = true,
    slop: Double = 0.1
  ): Either[String, Unit] = {
    val tidyname = (if (name.toLowerCase.endsWith(".zip")) name.dropRight(4) else name) match {
      case nm => if (nm.toLowerCase.endsWith(".wcon")) nm.dropRight(5) else nm                                   
    }
    val spacey = tidyname.contains(" ")
    if (timeMajor) {
      case class N(var value: Int) { def ++(): this.type = { value += 1; this } }
      val tses = collection.mutable.TreeMap.empty[Double, N]
      for { d <- ds; t <- d.ts } tses.getOrElseUpdate(t, N(0)).++
      val counts = tses.toArray.map{ case (k, v) => k -> v.value }
      val bins = counts.
        scanLeft((0, 0, 0.0, 0.0)){ (s, x) =>
          val (bin, si, t0, t1) = s
          val (t, ni) = x
          if (si == 0) (bin, ni, t, t)
          else if (si + ni <= maxEntries && maxEntries > 0) (bin, si + ni, math.min(t0, t), math.max(t1, t))
          else (bin + 1, ni, t, t)
        }.
        drop(1).
        groupBy(_._1).map{ case (k, vs) => vs.last }.toArray.
        sortBy(_._1)
      val n = bins.lastOption.map(_._1).getOrElse(0)
      if (n == 0 || (n == 1 && bins.last._2 < maxEntries*slop)) writeZip(ds, zip, tidyname+".wcon")
      else {
        val formatter = "%0"+n+"d"
        writeAllZip(
          zip,
          bins.iterator.map{ case (i, _, t0, t1) =>
            val namei = if (spacey) f"$tidyname - ${i+1}.wcon" else f"${tidyname}_${formatter.format(i+1)}.wcon"
            (ds.flatMap(di => di.timeWindow(t0, t1)), namei)
          }
        )
      }
    }
    else {
      val idt = collection.mutable.AnyRefMap.empty[String, (Double, Int, List[Int])]
      ds.data.zipWithIndex.foreach{ case (di, i) =>
        if (di.ts.length > 0)
          idt.get(di.id) match {
            case None => if (di.ts.length > 0) idt(di.id) = (di.ts.head, di.ts.length, i :: Nil)
            case Some((t, n, ii)) => idt(di.id) = (math.min(t, di.ts.head), n + di.ts.length, i :: ii)
          }
      }
      val pieces = idt.toArray.
        sortWith((l, r) => l._2._1 < r._2._1 || l._2._1 == r._2._1 && l._1 < r._1)
      val bins = pieces.
        scanLeft((0, 0)){ (s, x) => 
          if (s._2 == 0) (s._1, x._2._2)
          else if (s._2 + x._2._2 <= maxEntries && maxEntries > 0) (s._1, s._2 + x._2._2)
          else (s._1 + 1, x._2._2)
        }.
        drop(1)
      val n = bins.lastOption.map(_._1).getOrElse(0)
      if (n == 0 || (n == 1 && bins.last._2 < maxEntries*slop)) writeZip(ds, zip, tidyname+".wcon")
      else {
        val indexed = (bins.map(_._1) zip pieces).groupBy(_._1)
        val formatter = "%0" + n + "d"
        writeAllZip(
          zip,
          Iterator.range(0, n).map{ i =>
            val parts = indexed(i).
              map(_._2).
              sortWith((a,b) => a._2._1 < b._2._1 || (a._2._1 == b._2._1 && Metadata.semanticOrder.lt(a._1, b._1))).
              flatMap(_._2._3.reverse)
            val namei = if (spacey) f"$tidyname - ${i+1}.wcon" else f"${tidyname}_${formatter.format(i+1)}.wcon"
            (ds.copy(data = parts.map(i => ds.data(i))), namei)
          }
        )
      }
    }
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
          zos.write( PrettyJson.bytes(ds.copy(files = FileSet(fname, Array.empty, Array.empty, Json.Obj.empty)).json) )
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
      writeUnfixed(
        ds.copy(files = new FileSet(
          f,
          if (i+1 < sets.length) Array(sets(i+1)._2) else Array.empty,
          if (i > 0) Array(sets(i-1)._2) else Array.empty,
          Json.Obj.empty
          )),
        root,
        f
      ) match {
        case x: Left[String, Unit] => return x
        case _ =>
      }
    }
    Right(())
  }

  def writeAllZip(zip: java.io.File, sets: Iterator[(DataSet, String)]): Either[String, Unit] = {
    val bsets = sets.buffered
    var previousFname = ""
    try {
      val fos = new java.io.FileOutputStream(
        if (zip.getName.toLowerCase.endsWith(".zip")) zip
        else new java.io.File(zip.getPath + ".zip")
      )
      try {
        val zos = new java.util.zip.ZipOutputStream(fos)
        try {
          var i = 0
          while (bsets.hasNext) {
            val (ds, fname) = bsets.next
            val ze = new java.util.zip.ZipEntry(fname)
            zos.putNextEntry(ze)
            zos.write(PrettyJson.bytes(
              ds.copy(files = new FileSet(
                fname,
                if (bsets.hasNext) Array(bsets.head._2) else Array.empty,
                if (i > 0) Array(previousFname) else Array.empty,
                Json.Obj.empty
              )).json
            ))
            previousFname = fname
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

  def writeAllZip(zip: java.io.File, sets: Iterable[(DataSet, String)]): Either[String, Unit] = writeAllZip(zip, sets.iterator)
}
