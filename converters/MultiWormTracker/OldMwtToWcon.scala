package org.openworm.trackercommons.converters
package mwt

import org.openworm.trackercommons._

import java.nio.file.{ FileVisitResult => Fvr, _ }
import java.nio.file.attribute.{ BasicFileAttributes => Bfa }
import java.io._

import kse.flow._
import kse.eio._
import kse.jsonal._
import kse.jsonal.JsonConverters._

case class OldSummaryLine(frame: Int, time: Double, count: Int, events: Long)
object OldSummaryLine {
  def parse(g: Grok)(implicit fail: GrokHop[g.type]): OldSummaryLine = {
    val frame = g.I
    val time = g.D
    val count = g.I
    g trySkip 12
    var lastTok: String = ""
    val events =
      if (!g.hasContent || g.peekTok != "%") 0x0L
      else {
        var flags = 0x0L
        g.skip
        while (g.hasContent && { lastTok = g.tok; !lastTok.startsWith("%") }) {
          val h = Grok(lastTok)
          h{ implicit hail => 
            if (lastTok.startsWith("0x") || lastTok.startsWith("0X")) { h.exact("0x"); flags |= h.xI }
            else flags |= h.I
          }.yesOr(e => fail(g.customError("Not a valid stimulus: "+lastTok)))
        }
        flags
      }
    new OldSummaryLine(frame, time, count, events)
  }
}

case class OldBlobLine(
  frame: Int, time: Double, cx: Double, cy: Double,
  a: Int, boxL: Double, boxW: Double,
  spine: Array[Short],
  outx: Int, outy: Int, outN: Int, outline: Array[Byte]
) {
  private def sig3(d: Double) = (d*1e3).toLong/1e3
  def xs(mm_per_pixel: Double) = {
    val a = new Array[Double](spine.length/2)
    var i = 0; while (i < a.length) { a(i) = sig3((cx + spine(i << 1)) * mm_per_pixel); i += 1 }
    a
  }
  def ys(mm_per_pixel: Double) = {
    val a = new Array[Double](spine.length/2)
    var i = 0; while (i < a.length) { a(i) = sig3((cy + spine(1 + (i << 1))) * mm_per_pixel); i += 1 }
    a
  }
}
object OldBlobLine {
  val noSpine = new Array[Short](0)
  val noOutline = new Array[Byte](0)
  def parse(g: Grok)(implicit fail: GrokHop[g.type]): OldBlobLine = {
    val frame = g.I
    val time = g.D
    val x = g.D
    val y = g.D
    val a = g.I
    g.skip(3)
    val bl = g.D
    val bw = g.D
    val spine =
      if (!g.hasContent || g.peekTok != "%") noSpine
      else {
        g exact "%"
        var buffer = new Array[Short](22)
        var si = 0
        while (g.hasContent && g.peek != '%') {
          if (si >= buffer.length) buffer = java.util.Arrays.copyOf(buffer, buffer.length*2)
          buffer(si) = g.S
          si += 1
        }
        if (si != buffer.length) java.util.Arrays.copyOf(buffer, si & 0xFFFFFFFE) else buffer
      }
    val (oux, ouy, oun, oul) =
      if (!g.hasContent || g.peekTok != "%%") (0, 0, 0, noOutline)
      else {
        g exact "%%"
        val wx = g.I
        val wy = g.I
        val n = g.I
        val b = new Array[Byte]((n+3)/4)
        val chars = g.tok
        val m = (n+2)/3
        if (chars.length < m) fail(g.customError(f"Expected $n elements of pixel walk but only found string $chars"))
        var h, i = 0
        var j, k = 0
        var z = 0
        while (i < m) {
          val ci = chars.charAt(i) - '0'
          val bA = (ci >> 4) & 0x3; h += 1
          z |= (bA << k); k += 2; if (k > 6) { b(j) = z.toByte; z = 0; k = 0; j += 1 }
          if (h < n) {
            val bB = (ci >> 2) & 0x3; h += 1
            z |= (bB << k); k += 2; if (k > 6) { b(j) = z.toByte; z = 0; k = 0; j += 1 }
            if (h < n) {
              val bC = ci & 0x3; h += 1
              z |= (bC << k); k += 2; if (k > 6) { b(j) = z.toByte; z = 0; k = 0; j += 1 }
            }
          }
          i += 1
        }
        if (k != 0) b(j) = z.toByte
        (wx, wy, n, b)
      }
    new OldBlobLine(frame, time, x, y, a, bl, bw, spine, oux, ouy, oun, oul)
  }
}

case class OldBlob(id: Int, lines: Array[OldBlobLine]) {
  private def sig3(d: Double): Double = (d*1e3).toLong/1e3
  def toData(mm_per_pixel: Double): Data = {
    val areas, boxLengths, boxWidths = new Array[Double](lines.length)
    var i = 0
    while (i < lines.length) {
      areas(i) = sig3(lines(i).a * mm_per_pixel * mm_per_pixel)
      boxLengths(i) = sig3(lines(i).boxL * mm_per_pixel)
      boxWidths(i) = sig3(lines(i).boxW * mm_per_pixel)
      i += 1
    }
    val wb = Create.worm(
      id.toString,
      Json.Obj ~ (
        "@XJ",
        Json ~ 
          ("area", areas) ~
          ("box-length", boxLengths) ~
          ("box-width", boxWidths) ~ Json
      ) ~ Json.Obj
    )
    def addLine[D <: Create.HasData](wb: Create.DataBuilder[D], line: OldBlobLine) = {
      val cx = sig3(line.cx * mm_per_pixel)
      val cy = sig3(line.cy * mm_per_pixel)
      val t = line.time
      if (line.spine.length == 0 && line.outN <= 0) {
        wb.add(t, Array(cx), Array(cy))
      }
      else if (line.outN <= 0) {
        wb.add(t, cx, cy, line.xs(mm_per_pixel), line.ys(mm_per_pixel))
      }
      else {
        val pw = PixelWalk(line.outline, line.outN, line.outx * mm_per_pixel, line.outy * mm_per_pixel, mm_per_pixel)(0, 0)
        if (line.spine.length == 0) {
          wb.add(t, Array(cx), Array(cy), pw)
        }
        else {
          wb.add(t, cx, cy, line.xs(mm_per_pixel), line.ys(mm_per_pixel), pw)
        }
      }
    }
    var wbr = addLine(wb, lines(0))
    i = 1; while (i < lines.length) { wbr = addLine(wbr, lines(i)); i += 1 }
    wbr.result
  }
}
object OldBlob {
  def parseAll(g: Grok)(implicit fail: GrokHop[g.type]) = {
    val h = Grok("").delimit(0)
    var id = -1
    var n = 0
    var lines = new Array[OldBlobLine](100)
    val blobs = Array.newBuilder[OldBlob]
    h{ implicit hail =>
      while (g.hasContent) {
        h.input(g.tok.trim)
        if (h.peekTok == "%") {
          if (n > 0) {
            blobs += new OldBlob(id, java.util.Arrays.copyOf(lines, n))
            n = 0
          }
          id = h.skip.I
        }
        else if (h.hasContent) {
          val obl = OldBlobLine.parse(h)
          if (lines.length <= n) lines = java.util.Arrays.copyOf(lines, lines.length*2)
          lines(n) = obl
          n += 1
        }
      }
      if (id >= 0 && n > 0) blobs += new OldBlob(id, java.util.Arrays.copyOf(lines, n))
    }.foreachNo(n => fail(g.customError(n.toString + "\n")))
    blobs.result
  }
}

object OldMwtToWcon {
  val StrainMatcher = """([A-Z]+\d+\.?\p{Alnum})_.*""".r
  val TimeStampMatcher = """(\d\d\d\d)(\d\d)(\d\d)_(\d\d)(\d\d)(\d\d)""".r
  val AgeTempMatcher = """[^_]+_\p{Alpha}*(\d+)h(\d+)C_.*""".r

  def convertOldToWcon(summary: Option[Array[Byte]], blobs: Array[Array[Byte]], file: File, base: String, prefix: String) {
    summary match {
      case Some(bs) =>
        println(f"Summary file is ${bs.length} bytes long")
        val g = Grok("").delimit(0)
        val slines = g{ implicit fail =>
          (new String(bs)).lines.map{ l => OldSummaryLine.parse(g input l) }.toArray
        }.yesOr(e => throw new Exception("Couldn't parse summary line:\n" + e.toString))
        println(f"Times from ${slines.head.time}%.2f to ${slines.last.time}%.2f in ${slines.last.frame} frames")
      case None     => println("No summary file!")
    }

    println(f"${blobs.length} blobs files with a total of ${blobs.map(_.length).sum} bytes")
    val parseds = blobs.map{ blob =>
      val g = Grok.text(blob, Delimiter.newline).delimit(0)
      val parsed = g{ implicit fail => 
        OldBlob.parseAll(g)
      }.yesOr(e => throw new Exception("Couldn't parse blobs file:\n" + e.toString))
      println(f"Total number of pixels recorded in blobs: ${parsed.map(_.lines.map(_.a).sum).sum}")
      parsed
    }
    val datas = parseds.flatten.map(_.toData(0.026))
    println(f"Contours: ${datas.map(_.walks.map(_.length).getOrElse(0)).sum} vs Data Points: ${datas.map(_.ts.length).sum}")

    val id = base + "_" + prefix;
    var m = Create.meta.setID(id)
    m = m.addSoftware(Create.software.name("Multi-Worm Tracker").version("1.x LabView").addFeature("@XJ"))
    prefix match {
      case StrainMatcher(strain) => m = m.setStrain(strain)
      case _ =>
    }
    base match {
      case TimeStampMatcher(y, month, d, hr, min, s) => 
        m = m.setTime(java.time.LocalDateTime.of(y.toInt, month.toInt, d.toInt, hr.toInt, min.toInt, s.toInt))
      case _ =>
    }
    prefix match {
      case AgeTempMatcher(age, temp) =>
        m = m.setAge(age.toDouble).setTemp(temp.toDouble)
      case _ => 
    }
    var wcon = Create.wcon().setMeta(m).addData(datas.head)
    var i = 1; while (i < datas.length) { wcon = wcon.addData(datas(i)); i += 1 }
    val ans = wcon.setUnits().setOnlyFile(new File(file.getParent, id + ".wcon"))
    val inMemory = ans.result
    println(f"Data has ${inMemory.data.length} entries from ${parseds.map(_.length).sum} records")
    val onDisk = ReadWrite.write(inMemory, (new File(file.getParent, id + ".wcon")).getPath)
    println(f"$file is the source file")
    println(f"$base is the base file name")
    println(f"$prefix is the prefix")
    println(f"Errors in writing: $onDisk")
  }

  def convertDirectoryToWcon(dir: File, opts: Set[String]) {
    val targets = dir.listFiles
    val sfile = targets.find(_.getName.endsWith(".summary"))
    val summary = sfile.map(f => Files.readAllBytes(f.toPath))
    val bfiles = targets.filter(_.getName.endsWith(".blobs")).sortBy(_.getName)
    val blobs = bfiles.map(f => Files.readAllBytes(f.toPath))
    if (blobs.isEmpty) throw new Exception(f"No blobs files found in ${dir.getCanonicalFile.getPath}")
    val prefix = sfile.map(_.getName.dropRight(8)).getOrElse(bfiles.head.getName.split('_').dropRight(1).mkString("_"))
    convertOldToWcon(summary, blobs, dir, dir.getName, prefix)
  }

  def convertZippedOldToWcon(zip: File, opts: Set[String]) {
    val sys = FileSystems.newFileSystem(zip.toPath, null)
    val root = sys.getPath("/")
    var summaries = List.empty[Path]
    var blobs = List.empty[Path]
    var depth = 0
    Files.walkFileTree(root, new FileVisitor[Path] {
      def preVisitDirectory(dir: Path, attrs: Bfa) = {
        if (depth < 2) { depth += 1; Fvr.CONTINUE }
        else Fvr.SKIP_SUBTREE
      }
      def postVisitDirectory(dir: Path, ioe: java.io.IOException) = {
        if (depth > 0) depth -= 1
        Fvr.CONTINUE
      }
      def visitFile(file: Path, attrs: Bfa) = {
        val name = file.getFileName.toString
        if (name.endsWith("summary")) summaries = file :: summaries
        else if (name.endsWith("blobs")) blobs = file :: blobs
        Fvr.CONTINUE
      }
      def visitFileFailed(file: Path, ioe: java.io.IOException) = Fvr.CONTINUE
    })
    if (summaries.drop(1).nonEmpty) {
      throw new Exception(f"Too many summary files in ${zip.getCanonicalFile.getName}: ${summaries.reverse.mkString(", ")}")
    }
    if (blobs.isEmpty) {
      throw new Exception(f"No blobs files found in ${zip.getCanonicalFile.getName}")
    }
    if (blobs.drop(1).nonEmpty && !blobs.tail.forall(_.getParent == blobs.head.getParent)) {
      throw new Exception(f"blobs files found in different places in ${zip.getCanonicalFile.getName}: ${blobs.reverse.mkString(", ")}")
    }
    val sdata = summaries.headOption.map(p => Files.readAllBytes(p))
    val bdata = blobs.toArray.reverse.sortBy(_.getFileName.toString).map(p => Files.readAllBytes(p))
    val base = blobs.head.getParent match {
      case null => zip.getName.dropRight(4)
      case p =>
        val q = p.getFileName
        if (q eq null) zip.getName.dropRight(4)
        else q.toString
    }
    val prefix = summaries.headOption.
      map(_.getFileName.toString.dropRight(8)).
      getOrElse(blobs.head.getFileName.toString.split('_').dropRight(1).mkString("_"))
    convertOldToWcon(sdata, bdata, zip, base, prefix)
  }

  def getOptions(args: Array[String]): (Set[String], Array[String]) = {
    val preDashDash = args.takeWhile(_ != "--")
    val postDashDash = args.drop(preDashDash.length + 1)
    val (opts, notOpts) = preDashDash.partition(_.startsWith("--"))
    (opts.map(_.drop(2)).toSet, notOpts ++ postDashDash)
  }

  def main(argsAndOpts: Array[String]) {
    val (opts, args) = getOptions(argsAndOpts)
    if (args.length == 0) throw new Exception("Need a target of something to convert")
    val files = args.map{ a =>
      val fa = new File(a)
      if (!fa.exists) throw new Exception(f"File to convert does not exist: ${fa.getCanonicalFile.getPath}")
      if (fa.isDirectory && fa.listFiles.exists(_.getName.endsWith("blobs"))) fa
      else if (!fa.isDirectory) {
        if (fa.getName.endsWith(".zip")) fa
        else throw new Exception(f"Not a recognized file type: ${fa.getCanonicalFile.getPath}")
      }
      else throw new Exception(f"No blobs files in directory to convert: ${fa.getCanonicalFile.getPath}")
    }
    files.foreach{ fa =>
      if (fa.isDirectory) convertDirectoryToWcon(fa, opts)
      else convertZippedOldToWcon(fa, opts)
    }    
  }
}
