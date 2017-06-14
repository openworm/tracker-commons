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
            zos.write(PrettyJson.bytes(
              ds.copy(files = new FileSet(
                fname,
                if (i+1 < sets.length) Array(sets(i+1)._2) else Array.empty,
                if (i > 0) Array(sets(i-1)._2) else Array.empty,
                Json.Obj.empty
              )).json
            ))
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
