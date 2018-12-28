package org.openworm.trackercommons.converters
package mwt

import org.openworm.trackercommons._

import java.nio.file.{ FileVisitResult => Fvr, StandardOpenOption => Soo, _ }
import java.nio.file.attribute.{ BasicFileAttributes => Bfa }
import java.io._

import kse.flow._
import kse.eio._
import kse.jsonal._
import kse.jsonal.JsonConverters._

case class ConversionOptions(
  mm_per_pixel: Double = 0.026,
  max_data: Int = 100000,
  elide_png: Boolean = false,
  tracked_for: Double = 0.0,
  must_move: Double = 0.0,
  to: Option[String] = None
) {}
object ConversionOptions {
  val known = Vector("no-image".r, "pixel-size=.+".r, "max-data=\\d+".r, "tracked-for=.+".r, "must-move=.+".r, "to=.*".r)
  def parse(opts: Set[String]): ConversionOptions = {
    var answer = ConversionOptions()
    opts.filter(x => known.forall(_.unapplySeq(x).isEmpty)).toList match {
      case Nil =>
      case xs =>
        println("Unknown options: " + xs.mkString(", "))
        println("Known options (in regular expression form) are:")
        known.foreach(ki => println("  "+ki))
        throw new Exception("Unknown command-line options")
    }
    if (opts contains "no-image") answer = answer.copy(elide_png = true)
    opts.filter(_ startsWith "pixel-size=").map(s => s.drop(11).toDouble).foreach(x => answer = answer.copy(mm_per_pixel = x))
    opts.filter(_ startsWith "max-data=").map(s => s.drop(9).toInt).foreach(x => answer = answer.copy(max_data = x))
    opts.filter(_ startsWith "tracked-for=").map(s => s.drop(12).toDouble).foreach(x => answer = answer.copy(tracked_for = x))
    opts.filter(_ startsWith "must-move=").map(s => s.drop(10).toDouble).foreach(x => answer = answer.copy(must_move = x))
    opts.filter(_ startsWith "to=").map(s => s.drop(3)).foreach(x => answer = answer.copy(to = Some(x)))
    answer
  }
}

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
  def allEvents(lines: Array[OldSummaryLine]): Json.Obj = {
    val evs = lines.filter(_.events != 0L)
    if (evs.isEmpty) Json.Obj.empty
    else {
      val bits = (0L /: evs)(_ | _.events)
      val labeled = (0 until 64).
        filter(bit => (bits & (1L << bit.toLong)) != 0).
        map{
          case 0 => ("tap", 1L)
          case 1 => ("puff", 2L)
          case n => ("custom "+(n-1), (1L <<  n.toLong))
        }
      val job = Json.Obj.builder
      labeled.foreach{ case (name, bit) => job ~ (name, Json(evs.filter(e => (e.events & bit) != 0).map(_.time))) }
      Json.Obj ~ ("@XJ", job.result) ~ Json.Obj
    }
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

  private def cxData(data: Data, i: Int): Double =
    if (data.cxs.length > 0) data.cxs(i)
    else {
      var j = 0
      val n = data.spineN(i)
      var x = 0.0
      while (j < n) { x += data.x(i, j); j += 1 }
      x/n
    }

  private def cyData(data: Data, i: Int): Double =
    if (data.cys.length > 0) data.cys(i)
    else {
      var j = 0
      val n = data.spineN(i)
      var y = 0.0
      while (j < n) { y += data.y(i, j); j += 1 }
      y/n
    }

  private def farthestIndex(data: Data, dsq: Double, x: Double, y: Double): Int = {
    var fi = 0
    var fsq = 0.0
    var i = 0
    while (i < data.ts.length) {
      val cxi = cxData(data, i)
      val cyi = cyData(data, i)
      val isq = (x - cxi)*(x - cxi) + (y - cyi)*(y - cyi)
      if (isq > fsq) {
        fi = i
        fsq = isq
        if (isq >= dsq) return -1
      }
      i += 1
    }
    return fi
  }

  def convertOldToWcon(
    summary: Option[Array[Byte]], blobs: Array[Array[Byte]], settings: Option[String], picture: Option[Array[Byte]],
    file: File, base: String, prefix: String, opts: ConversionOptions
  ) {
    val events = summary match {
      case Some(bs) =>
        val g = Grok("").delimit(0)
        val slines = g{ implicit fail =>
          (new String(bs)).lines.map{ l => OldSummaryLine.parse(g input l) }.toArray
        }.yesOr(e => throw new Exception("Couldn't parse summary line:\n" + e.toString))
        OldSummaryLine.allEvents(slines)
      case None     =>
        println("No summary file!")
        Json.Obj.empty
    }

    println(f"${blobs.length} blobs files with a total of ${blobs.map(_.length).sum} bytes")
    val parseds = blobs.map{ blob =>
      val g = Grok.text(blob, Delimiter.newline).delimit(0)
      val parsed = g{ implicit fail => 
        OldBlob.parseAll(g)
      }.yesOr(e => throw new Exception("Couldn't parse blobs file:\n" + e.toString))
      parsed
    }
    val datas = parseds.flatten.map(_.toData(opts.mm_per_pixel))

    val id = base + "_" + prefix;
    var m = Create.meta.setID(id)
    m = m.addSoftware({
      var soft = Create.software.name("Multi-Worm Tracker").version("1.x LabView").addFeature("@XJ")
      settings.foreach{ set => soft = soft.setSettings(Json(set)) }
      soft
    })
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
    val custom = events.asMap match {
      case m =>
        val xjOld = m.get("@XJ") match { case Some(o: Json.Obj) => o.asMap; case _ => Map.empty[String, Json] }
        Json.Obj(m + (("@XJ", Json.Obj(xjOld + (("mm_per_pixel", Json(opts.mm_per_pixel)))))))
    }

    var wcon = Create.wcon().setCustom(custom).setMeta(m).addData(datas.head)
    var i = 1; while (i < datas.length) { wcon = wcon.addData(datas(i)); i += 1 }
    val ans = wcon.setUnits().setOnlyFile(new File(""))
    var inMemory = ans.result
    if (
      (opts.tracked_for > 0 && opts.tracked_for < Double.PositiveInfinity) ||
      (opts.must_move > 0 && opts.must_move < Double.PositiveInfinity)
    ) {
      val ids = inMemory.data.map(_.id).toSet
      val consolidated = if (ids.size == inMemory.data.length) inMemory.data else inMemory.groupByIDs().data
      val rejected = collection.mutable.AnyRefMap.empty[String, Unit]
      if (opts.tracked_for > 0 && opts.tracked_for < Double.PositiveInfinity)
        consolidated.foreach{ data =>
          if (data.ts.length > 0) {
            val dt = data.ts.last - data.ts.head
            if (dt < opts.tracked_for) rejected(data.id) = ()
          }
          else rejected(data.id) = ()
        }
      if (opts.must_move > 0 && opts.must_move < Double.PositiveInfinity)
        consolidated.foreach{ data =>
          if (!rejected.contains(data.id)) {
            if (data.ts.length > 1) {
              val must = opts.must_move * opts.must_move
              val i = farthestIndex(data, must, cxData(data, 0), cyData(data, 0))
              if (i >= 0) {
                val ii = if (i == 0) 0 else farthestIndex(data, must, cxData(data, i), cyData(data, i))
                if (ii >= 0) rejected(data.id) = ()
              }
            }
            else rejected(data.id) = ()
          }
        }
      if (rejected.nonEmpty) {
        println("Skipping IDs " + rejected.map(_._1).toVector.sorted(Metadata.semanticOrder).mkString(", "))
        inMemory = inMemory.flatMap(d => if (rejected.contains(d.id)) None else Some(d))
      }
    }
    println(f"Data has ${inMemory.data.length} entries from ${parseds.map(_.length).sum} records")
    val target = (new File(opts.to.getOrElse(file.getParent), id + ".wcon.zip"))
    val onDisk = ReadWrite.writeChunkedZip(inMemory, target, prefix, opts.max_data, false)
    println(f"$file is the source file")
    println(f"$base is the base file name")
    println(f"$prefix is the prefix")
    onDisk match {
      case Left(e) =>
        println(f"Errors in writing: $e")
        throw new Exception(f"Could not write data for $id")
      case _ =>
    }
    picture.filterNot(_ => opts.elide_png) match {
      case Some(png) =>
        val sys = FileSystems.newFileSystem(target.toPath, null)
        try {
          Files.write(sys.getPath(f"$prefix.png"), png, Soo.CREATE, Soo.TRUNCATE_EXISTING)
        }
        finally { sys.close }
      case _ =>
    }
  }

  def convertDirectoryToWcon(dir: File, opts: ConversionOptions) {
    val targets = dir.listFiles
    val sfile = targets.find(_.getName.endsWith(".summary"))
    val summary = sfile.map(f => Files.readAllBytes(f.toPath))
    val settings = targets.find(_.getName.endsWith(".set")).map(f => new String(Files.readAllBytes(f.toPath)))
    val png = targets.find(_.getName.endsWith(".png")).map(f => Files.readAllBytes(f.toPath))
    val bfiles = targets.filter(_.getName.endsWith(".blobs")).sortBy(_.getName)
    val blobs = bfiles.map(f => Files.readAllBytes(f.toPath))
    if (blobs.isEmpty) throw new Exception(f"No blobs files found in ${dir.getCanonicalFile.getPath}")
    val prefix = sfile.map(_.getName.dropRight(8)).getOrElse(bfiles.head.getName.split('_').dropRight(1).mkString("_"))
    convertOldToWcon(summary, blobs, settings, png, dir, dir.getName, prefix, opts)
  }

  def convertZippedOldToWcon(zip: File, opts: ConversionOptions) {
    val sys = FileSystems.newFileSystem(zip.toPath, null)
    val root = sys.getPath("/")
    var summaries = List.empty[Path]
    var settings = List.empty[Path]
    var pngs = List.empty[Path]
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
        else if (name.endsWith(".set")) settings = file :: settings
        else if (name.endsWith(".png")) pngs = file :: pngs
        Fvr.CONTINUE
      }
      def visitFileFailed(file: Path, ioe: java.io.IOException) = Fvr.CONTINUE
    })
    if (summaries.drop(1).nonEmpty) {
      throw new Exception(f"Too many summary files in ${zip.getCanonicalFile.getName}: ${summaries.reverse.mkString(", ")}")
    }
    if (settings.drop(1).nonEmpty) {
      throw new Exception(f"Too many settings files in ${zip.getCanonicalFile.getName}: ${settings.reverse.mkString(", ")}")
    }
    if (blobs.isEmpty) {
      throw new Exception(f"No blobs files found in ${zip.getCanonicalFile.getName}")
    }
    if (blobs.drop(1).nonEmpty && !blobs.tail.forall(_.getParent == blobs.head.getParent)) {
      throw new Exception(f"blobs files found in different places in ${zip.getCanonicalFile.getName}: ${blobs.reverse.mkString(", ")}")
    }
    val sdata = summaries.headOption.map(p => Files.readAllBytes(p))
    val setdata = settings.headOption.map(p => new String(Files.readAllBytes(p)))
    val png = pngs.headOption.map(p => Files.readAllBytes(p))
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
    convertOldToWcon(sdata, bdata, setdata, png, zip, base, prefix, opts)
  }

  def getOptions(args: Array[String]): (ConversionOptions, Array[String]) = {
    val preDashDash = args.takeWhile(_ != "--")
    val postDashDash = args.drop(preDashDash.length + 1)
    val (opts, notOpts) = preDashDash.partition(_.startsWith("--"))
    (ConversionOptions.parse(opts.map(_.drop(2)).toSet), notOpts ++ postDashDash)
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
      if (files.length > 1 && (fa ne files.head)) println("##########")
      if (fa.isDirectory) convertDirectoryToWcon(fa, opts)
      else convertZippedOldToWcon(fa, opts)
    }    
  }
}
