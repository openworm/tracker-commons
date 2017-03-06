package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

import WconImplicits._

trait HasId extends math.Ordered[HasId] {
  def nid: Double
  def sid: String
  def idEither: Either[Double, String] = if (nid.isNaN) Right(sid) else Left(nid)
  def idJSON: Json = if (nid.isNaN) { if (sid == null) Json.Null else Json.Str(sid) } else Json.Num(nid)
  def compare(them: HasId) =
    if (nid.isNaN)
      if (them.nid.isNaN)
        if (sid == null)
          if (them.sid == null) 0
          else -1
        else if (them.sid == null) 1
        else sid compareTo them.sid
      else 1
    else
      if (them.nid.isNaN) -1
      else if (them.nid.isNaN) 1
      else if (nid < them.nid) -1
      else if (nid > them.nid) 1
      else 0
}

case class IdOnly(nid: Double, sid: String) extends HasId {}

/** Trait to specify the perimeter of a single animal. */
trait Perimeter {
  def tailIndex: Option[Int]
  def size: Int
  def x(i: Int): Double
  def y(i: Int): Double
  def getPoints(): (Array[Double], Array[Double])
}
object Perimeter {
  def empty: Perimeter = NoPerimeter
}

object NoPerimeter extends Perimeter {
  private[this] val noPoints = new Array[Double](0)
  private[this] val noPointsPair = (noPoints, noPoints)
  def tailIndex = None
  def size = 0
  def x(i: Int) = throw new IndexOutOfBoundsException("No perimeter")
  def y(i: Int) = throw new IndexOutOfBoundsException("No perimeter")
  def getPoints(): (Array[Double], Array[Double]) = noPointsPair
}

case class PixelWalk(path: Array[Byte], n: Int, x0: Double, y0: Double, side: Double, tail: Int = -1)(val ocx: Double, val ocy: Double)
extends Perimeter with AsJson {
  private[this] var myGxn = 0
  private[this] var myGyn = 0
  private[this] var myI = 0
  def tailIndex = if (tail >= 0) Some(tail) else None
  def size = n
  def step(i: Int) = {
    if (i>=4*path.length || i < 0) {
      println(f"This isn't going to work; asking for $i when length is ${path.length}")
    }
    ((path(i >>> 2) & 0xFF) >>> (i & 0x3)) & 0x3
  }
  private[this] def findI(i: Int) {
    if (myI < i) {
      while (myI < i) { 
        step(myI) match {
          case 0 => myGxn += 1
          case 1 => myGyn += 1
          case 2 => myGxn -= 1
          case _ => myGyn -= 1
        }
        myI += 1
      }
    }
    else if (myI > i) {
      while (myI > i) {
        myI -= 1
        step(myI) match {
          case 0 => myGxn -= 1
          case 1 => myGyn -= 1
          case 2 => myGxn += 1
          case _ => myGyn += 1
        }
      }
    }
  }
  def x(i: Int) = {
    if (i != myI) findI(i)
    x0 + side*myGxn.toDouble
  }
  def y(i: Int) = {
    if (i != myI) findI(i)
    y0 + side*myGyn.toDouble
  }
  def getPoints(): (Array[Double], Array[Double]) = {
    val xs, ys = new Array[Double](n)
    var i = 0
    while (i < n) { xs(i) = x(i); ys(i) = y(i); i += 1 }
    (xs, ys)
  }
  def translate(rx: Double, ry: Double) = new PixelWalk(path, n, x0+rx, y0+ry, side, tail)(rx, ry)
  def json = {
    val b = Json ~ ("px", Json.Arr.Dbl(Array(Data.sig(x0-ocx), Data.sig(y0-ocy), side)))
    if (tail >= 0) b ~ ("n", Json(Array[Double](n, tail)))
    else b ~ ("n", n)
    b ~ ("path", path) ~ Json
  }
}
object PixelWalk extends FromJson[PixelWalk] {
  val emptyBytes = new Array[Byte](0)
  val empty = new PixelWalk(emptyBytes, 0, 0, 0, 0, -1)(0, 0)

  def parse(j: Json): Either[JastError, PixelWalk] = j match {
    case o: Json.Obj =>
      if (o.size != 3) return Left(JastError("PixelWalk must contain exactly three elements"))
      val ori = o("px") match {
        case jad: Json.Arr.Dbl if jad.size == 3 => jad.doubles
        case _ => return Left(JastError("PixelWalk must contain a 'px' field that is an array of three numbers"))
      }
      val n = o("n") match {
        case jn: Json.Num if jn.double.toInt == jn.double =>
          Array(jn.double.toInt, -1)
        case jad: Json.Arr.Dbl if jad.size == 2 && jad.doubles(0).toInt == jad.doubles(0) && jad.doubles(1).toInt == jad.doubles(1) =>
          Array(jad.doubles(0).toInt, jad.doubles(1).toInt)
        case _ =>
          return Left(JastError("PixelWalk must contain an 'n' field that is a single integer, or an array of two integers"))
      }
      val path =
        if (n(0) > 0) o("path").to[Array[Byte]] match { case Left(je) => return Left(je); case Right(p) => p }
        else emptyBytes
      if (n(0) > 4*path.length) return Left(JastError(f"path contains at most ${4*path.length} steps but ${n(0)} declared"))
      if (n(1) >= n(0)) return Left(JastError(f"tail index ${n(1)} is outside of path length ${n(0)}"))
      Right(new PixelWalk(path, n(0), ori(0), ori(1), ori(2).toFloat, n(1))(0, 0))
    case _ => Left(JastError("JSON value is not an array so cannot be a PixelWalk"))
  }
}

case class PerimeterPoints(xData: Array[Float], yData: Array[Float], tailIndex: Option[Int] = None)(val rx: Double, val ry: Double)
extends Perimeter {
  val size = math.min(xData.length, yData.length)
  def x(i: Int) = xData(i) + rx
  def y(i: Int) = yData(i) + ry
  def getPoints(): (Array[Double], Array[Double]) = {
    val px, py = new Array[Double](size)
    var i = 0
    while (i < size) {
      px(i) = xData(i) + rx
      py(i) = yData(i) + ry
      i += 1
    }
    (px, py)
  }
}
object PerimeterPoints {
  val empty = new PerimeterPoints(new Array[Float](0), new Array[Float](0), None)(0, 0)
}

/** Class to specify a single timepoint on a single animal.
  * Note that `x` and `y` are global, while xData and yData
  * are relative to `rx` and `ry`.
  *
  * The centroid, `cx` and `cy`, is global here.  It appears
  * relative to `ox` and `oy` in the JSON file.
  */
case class Datum(
  nid: Double, sid: String,
  t: Double, xData: Array[Float], yData: Array[Float],
  cx: Double, cy: Double,
  ox: Double, oy: Double,
  perim: Option[PerimeterPoints],
  walk: Option[PixelWalk],
  custom: Json.Obj
)(
  val rx: Double, val ry: Double
) extends HasId with AsJson {
  val hasO = !ox.isNaN && !oy.isNaN && !ox.isInfinite && !oy.isInfinite
  val hasC = !cx.isNaN && !cy.isNaN && !cx.isInfinite && !cy.isInfinite

  /** The length of the spine */
  val spineN = math.min(xData.length, yData.length)

  /** x coordinate, global coordinates */
  def x(i: Int): Double = xData(i) + rx

  /** y coordinate, global coordinates */
  def y(i: Int): Double = yData(i) + ry

  /** Global coordinates of spine */
  def spinePoints(): (Array[Double], Array[Double]) = {
    val xs, ys = new Array[Double](spineN)
    var i = 0
    while (i < spineN) {
      xs(i) = x(i)
      ys(i) = y(i)
      i += 1
    }
    (xs, ys)
  }

  private val myPerim = (perim orElse walk).getOrElse(NoPerimeter)

  val perimN = myPerim.size

  def px(i: Int) = myPerim.x(i)

  def py(i: Int) = myPerim.y(i)

  def perimPoints(): (Array[Double], Array[Double]) = myPerim.getPoints()

  private def externalize(coords: Array[Float], r: Double, o: Double): Array[Double] = {
    val ext = new Array[Double](coords.length)
    val delta = r - (if (hasO) o else 0.0)
    var i = 0
    while (i < ext.length) {
      ext(i) = Data.sig(coords(i) + delta)
      i += 1
    }
    ext
  }

  def json = {
    val b = Json ~ ("id", idJSON) ~ ("t", t)
    if (hasO) b ~ ("ox", ox) ~ ("oy", oy)
    if (hasC) {
      b ~ ("cx", if (hasO) Data.sig(cx - ox) else cx) ~ ("cy", if (hasO) Data.sig(cy - oy) else cy)
    }
    val dbx = externalize(xData, rx, ox)
    val dby = externalize(yData, ry, oy)
    b ~ ("x", Json(dbx)) ~ ("y", Json(dby))
    if (perim.isDefined) {
      val ppts = perim.get
      val px = externalize(ppts.xData, ppts.rx, ox)
      val py = externalize(ppts.yData, ppts.ry, oy)
      b ~ ("px", Json(px)) ~ ("py", Json(py)) ~? ("ptail", ppts.tailIndex)
    }
    b ~? ("walk", walk) ~~ custom ~ Json
  }

  def toData: Data = {
    val d = new Data(
      nid, sid,
      Array(t), Array(xData), Array(yData),
      if (hasC) Array(cx) else Data.emptyD, if (hasC) Array(cy) else Data.emptyD,
      if (hasO) Array(ox) else Data.emptyD, if (hasO) Array(oy) else Data.emptyD, false,
      perim.map(p => Array(p)),
      walk.map(w => Array(w)),
      custom
    )(Array(rx), Array(ry))
    d
  }

  def similarTo(d: Datum, tol: Double, checkCentroids: Boolean): Boolean =
    math.abs(t - d.t) <= tol &&
    (!checkCentroids || (cx.finite == d.cx.finite && cy.finite == d.cy.finite)) &&
    (if (checkCentroids && (cx.finite || cy.finite)) math.abs(cx - d.cx) + math.abs(cy - d.cy) <= 2*tol else true) &&
    (spineN == d.spineN) &&
    { var i = 0;
      var close = true;
      while (i < spineN && close) {
        close = math.abs(x(i) - d.x(i)) <= tol && math.abs(y(i) - d.y(i)) <= tol
        i += 1
      }
      close
    }
  def similarTo(d: Datum, tol: Double): Boolean = similarTo(d, tol, true)
  def similarTo(d: Data, tol: Double, checkCentroids: Boolean): Boolean = toData.similarTo(d, tol, checkCentroids)
  def similarTo(d: Data, tol: Double): Boolean = toData.similarTo(d, tol, true)
}
object Datum extends FromJson[Datum] {
  private val someSingles = Option(Set("id", "t", "x", "y", "cx", "cy", "ox", "oy"))
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid data entry: " + msg))
  private def MYBAD(nid: Double, sid: String, t: Double, msg: String): Either[JastError, Nothing] =
    BAD("Data point for " + IdOnly(nid,sid).idJSON.toString + " at time " + t + " has " + msg)
  private def sensiblyOffset(hasO: Boolean, o: Double, hasC: Boolean, c: Double, zs: Array[Double], ps: Array[Double]): Double = {
    var zmin, zmax = zs(0)
    var i = 0; while (i < zs.length) { if (zs(i) < zmin) zmin = zs(i) else if (zs(i) > zmax) zmax = zs(i); i += 1 }
    if (math.abs(zmin) < 10 && math.abs(zmax) < 10) {
      if (hasO) o else 0
    }
    else {
      if (hasC) {
        val fix = if (hasO) o else 0
        i = 0; while (i < zs.length) { zs(i) += fix - c; i += 1 }
        if (ps ne null) { i = 0; while (i < ps.length) { ps(i) += fix - c; i += 1 } }
        c
      }
      else {
        i = 0; while (i < zs.length) { zs(i) -= zmin; i += 1 }
        if (ps ne null) { i = 0; while (i < ps.length) { ps(i) -= zmin; i += 1 } }
        if (hasO) o + zmin else zmin
      }
    }
  }
  def datumFromValues(
    nid: Double, sid: String, t: Double,
    hasO: Boolean, ox: Double, oy: Double,
    hasC: Boolean, cx0: Double, cy0: Double,
    x0: Array[Double], y0: Array[Double],
    px0: Array[Double], py0: Array[Double],
    walk0: Option[PixelWalk], ptail: Option[Int],
    o: Json.Obj
  ): Datum = {
    val cx = if (hasC && hasO) cx0 + ox else cx0
    val cy = if (hasC && hasO) cy0 + oy else cy0
    val rx = sensiblyOffset(hasO, ox, hasC, cx, x0, px0)
    val ry = sensiblyOffset(hasO, oy, hasC, cy, y0, py0)
    val walk = walk0.map(_.translate(if (hasO) ox else 0, if (hasO) oy else 0))
    val peri = 
      if ((px0 ne null) && (py0 ne null)) Some(PerimeterPoints(Data.singly(px0), Data.singly(py0), ptail)(rx, ry))
      else None
    new Datum(nid, sid, t, Data.singly(x0), Data.singly(y0), cx, cy, ox, oy, peri, walk, o)(rx, ry)
  }
  def parse(j: Json): Either[JastError, Datum] = {
    implicit val jsonToPixelWalk: FromJson[PixelWalk] = PixelWalk
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    o.countKeys(someSingles).foreach{ case (key, n) => if (n > 1) return BAD("duplicate entries for " + key) }
    val (nid, sid) = o.get("id") match {
      case None => return BAD("no ID!")
      case Some(j) => j match {
        case n: Json.Num if n.isDouble || n.isLong => (n.double, null: String)
        case Json.Str(s) => (Double.NaN, s)
        case _ => return BAD("ID neither numeric nor text!")
      }
    }
    val t = o("t") match {
      case n: Json.Num if n.isDouble || n.isLong => n.double
      case _ => return BAD("no valid timepoint")
    }
    var finO = 0
    val List(ox, oy) = List("ox", "oy").map(key => o.get(key) match {
      case Some(n: Json.Num) => val ans = n.double; if (!ans.isNaN && !ans.isInfinite) finO += 1; ans
      case None => Double.NaN
      case _ => return BAD(f"$key is not numeric")
    })
    if ((finO & 1) != 0) return BAD(f"ox $ox and oy $oy do not agree on whether origin is finite")
    var finC = 0
    val List(cx0, cy0) = List("cx", "cy") map(key => o.get(key) match {
      case Some(n: Json.Num) => val ans = n.double; if (!ans.isNaN && !ans.isInfinite) finC += 1; ans
      case None => Double.NaN
      case _ => return BAD(f"$key is not numeric")
    })
    if ((finC & 1) != 0) return BAD(f"cx $cx0 and cy $cy0 do not agree on whether centroid is finite")
    val List((x0, px0), (y0, py0)) = List(("x", "px"), ("y", "py")).map{ case (key, peri) => 
      val values = o(key) match {
        case Json.Null => return MYBAD(nid, sid, t, f"no $key!")
        case n: Json.Num => Array(n.double)
        case ns: Json.Arr.Dbl => ns.doubles
        case xs: Json.Arr.All if xs.size == 0 => Data.emptyD
        case je => return MYBAD(nid, sid, t, f"no valid $key was $je (${je.getClass.getName})!")
      }
      val perivalues = o.get(peri) match {
        case None => null
        case Some(j) => j match {
          case Json.Null => null
          case n: Json.Num => Array(n.double)
          case ns: Json.Arr.Dbl => ns.doubles
          case xs: Json.Arr.All if xs.size == 0 => Data.emptyD
          case _ => return MYBAD(nid, sid, t, f"$peri is present but not numeric / numeric array")
        }
      }
      (values, perivalues)
    }
    if (x0.length != y0.length) return MYBAD(nid, sid, t, "x and y not the same lengths (${x0.length} vs ${y0.length})")
    if ((px0 eq null) != (py0 eq null)) return MYBAD(nid, sid, t, "Only one of px or py present")
    if ((px0 ne null) && px0.length != py0.length) return MYBAD(nid, sid, t, "px and py not the same lengths (${px0.length} vs ${py0.length})")

    val walk0 = o.get("walk") match {
      case None => None
      case Some(j) => j.to[PixelWalk] match {
        case Left(je) => return MYBAD(nid, sid, t, je.toString)
        case Right(pw) => Some(pw)
      }
    }

    val ptail = 
      if ((px0 ne null) && (py0 ne null)) {
        o.get("ptail").flatMap{_ match {
          case jn: Json.Num if (jn.double.toInt == jn.double) => Some(jn.double.toInt)
          case _ => return MYBAD(nid, sid, t, "ptail isn't an integer")
        }}
      }
      else None

    Right(datumFromValues(
      nid, sid, t, finO > 0, ox, oy, finC > 0, cx0, cy0, x0, y0, px0, py0, walk0, ptail, o.filter((k,_) => k.startsWith("@"))
    ))
  }
}

/** Class to specify multiple timepoints on a single animal.
  * `xDatas` and `yDatas` are relative to `rxs` and `rys`.  `cxs` and `cys` are global whether or not
  * `oxs` and `oys` were present.
  * If all values of `ox` and `oy` are the same, they may be specified by an array of length 1 to save space.
  */
case class Data(
  nid: Double, sid: String,
  ts: Array[Double],
  xDatas: Array[Array[Float]], yDatas: Array[Array[Float]],
  cxs: Array[Double], cys: Array[Double],
  oxs: Array[Double], oys: Array[Double], unarrayedOrigin: Boolean,
  perims: Option[Array[PerimeterPoints]],
  walks: Option[Array[PixelWalk]],
  custom: Json.Obj
)(
  val rxs: Array[Double], val rys: Array[Double]
)
extends HasId with AsJson {
  assert(
    (ts ne null) && (xDatas ne null) && (yDatas ne null) &&
    (cxs ne null) && (cys ne null) && (oxs ne null) && (oys ne null) && (rxs ne null) && (rys ne null) &&
    ts.length == xDatas.length &&
    ts.length == yDatas.length &&
    { cxs.length == ts.length || cxs.length == 0 } &&
    { oxs.length == ts.length || oxs.length == 1 || oxs.length == 0 } &&
    cxs.length == cys.length &&
    oxs.length == oys.length &&
    ts.length == rxs.length &&
    ts.length == rys.length &&
    { var i = 0
      var good = true
      while (good && i < xDatas.length) {
        good = (xDatas(i) ne null) && (yDatas(i) ne null) && xDatas(i).length == yDatas(i).length
        i += 1
      }
      good
    } &&
    perims.forall(_.length == ts.length) &&
    walks.forall(_.length == ts.length)
  )

  def n: Int = ts.length

  def spineN(i: Int) = xDatas(i).length

  def x(i: Int, j: Int) = xDatas(i)(j) + rxs(i)

  def y(i: Int, j: Int) = yDatas(i)(j) + rys(i)

  def spinePoints(i: Int): (Array[Double], Array[Double]) = {
    val m = spineN(i)
    val xs, ys = new Array[Double](m)
    val xi = xDatas(i)
    val yi = yDatas(i)
    val rxi = rxs(i)
    val ryi = rys(i)
    var j = 0
    while (j < m) {
      xs(j) = xi(j) + rxi
      ys(j) = yi(j) + ryi
      j += 1
    }
    (xs, ys)
  }

  val myPerims: Array[Perimeter] = {
    val mps = new Array[Perimeter](ts.length)
    (perims, walks) match {
      case (None, None)         => var i = 0; while (i < mps.length) { mps(i) = NoPerimeter; i += 1 }
      case (Some(ps), None)     => var i = 0; while (i < mps.length) { mps(i) = ps(i); i += 1 }
      case (None, Some(pw))     => var i = 0; while (i < mps.length) { mps(i) = pw(i); i += 1 }
      case (Some(ps), Some(pw)) => var i = 0; while (i < mps.length) { mps(i) = if (ps(i).size == 0) pw(i) else ps(i); i += 1 }
    }
    mps
  }

  def perimN(i: Int) = myPerims(i).size

  def px(i: Int, j: Int) = ???

  def py(i: Int, j: Int) = ???

  def perimPoints(i: Int): (Array[Double], Array[Double]) = myPerims(i).getPoints()

  def datum(i: Int): Datum = new Datum(
    nid, sid, ts(i), xDatas(i), yDatas(i),
    if (cxs.length == 0) Double.NaN else cxs(i),
    if (cys.length == 0) Double.NaN else cys(i),
    if (oxs.length == 0) Double.NaN else if (oxs.length == 1) oxs(0) else oxs(i),
    if (oys.length == 0) Double.NaN else if (oys.length == 1) oys(0) else oys(i),
    perims.flatMap(pms => if (pms(i).size == 0) None else Some(pms(i))),
    walks.flatMap(pws => if (pws(i).size == 0) None else Some(pws(i))),
    custom
  )(rxs(i), rys(i))

  private def externalize(coords: Array[Float], r: Double, has: Boolean, oc: Double): Array[Double] = {
    val ext = new Array[Double](coords.length)
    val delta = r - (if (has) oc else 0.0)
    var i = 0
    while (i < ext.length) {
      ext(i) = Data.sig(coords(i) + delta)
      i += 1
    }
    ext
  }

  def json = {
    val b = Json ~ ("id", idJSON) ~ ("t", Json(ts))
    if (oxs.length > 0) {
      if (oxs.length == 1 && unarrayedOrigin) b ~ ("ox", oxs(0)) ~ ("oy", oys(0))
      else b ~ ("ox", Json(oxs)) ~ ("oy", Json(oys))
    }
    if (cxs.length > 0) {
      val kxs =
        if (oxs.length == 0 || (oxs.length == 1 && oxs(0) == 0)) cxs
        else {
          var qs = new Array[Double](cxs.length)
          if (oxs.length == 1) { var i = 0; while (i < cxs.length) { qs(i) = Data.sig(cxs(i) - oxs(0)); i += 1 } }
          else                 { var i = 0; while (i < cxs.length) { qs(i) = Data.sig(cxs(i) - oxs(i)); i += 1 } }
          qs
        }
      val kys =
        if (oys.length == 0 || (oys.length == 1 && oys(0) == 0)) cys
        else {
          var qs = new Array[Double](cys.length)
          if (oys.length == 1) { var i = 0; while (i < cys.length) { qs(i) = Data.sig(cys(i) - oys(0)); i += 1 } }
          else                 { var i = 0; while (i < cys.length) { qs(i) = Data.sig(cys(i) - oys(i)); i += 1 } }
          qs
        }
      b ~ ("cx", Json(kxs)) ~ ("cy", Json(kys))
    }
    val dxs = ts.indices.map{ i =>
      val oi = if (oxs.length > 0) { if (oxs.length == 1) oxs(0) else oxs(i) } else 0.0
      val has = (oxs.length > 0) && !oi.isNaN
      externalize(xDatas(i), rxs(i), has, oi)
    }
    val dys = ts.indices.map{ i =>
      val oi = if (oys.length > 0) { if (oys.length == 1) oys(0) else oys(i) } else 0.0
      val has = (oys.length > 0) && !oi.isNaN
      externalize(yDatas(i), rys(i), has, oi)
    }
    b ~ ("x", Json(dxs)) ~ ("y", Json(dys))
    if (perims.isDefined) {
      val pms = perims.get
      val pxs = ts.indices.map{ i =>
        val oi = if (oxs.length > 0) { if (oxs.length == 1) oxs(0) else oxs(i) } else 0.0
        val has = (oxs.length > 0 ) && !oi.isNaN
        externalize(pms(i).xData, pms(i).rx, has, oi)
      }
      val pys = ts.indices.map{ i =>
        val oi = if (oys.length > 0) { if (oys.length == 1) oys(0) else oys(i) } else 0.0
        val has = (oys.length > 0) && !oi.isNaN
        externalize(pms(i).yData, pms(i).ry, has, oi)
      }
      b ~ ("px", Json(pxs)) ~ ("py", Json(pys))
      if (pms.exists(_.tailIndex.isDefined)) {
        val tis = new Array[Double](n)
        var i = 0; while (i < tis.length) { tis(i) = pms(i).tailIndex match { case Some(i) => i.toDouble; case _ => Double.NaN }; i += 1 }
        i = 1; while (i < tis.length && tis(i) == tis(i-1)) i += 1
        if (i == tis.length) b ~ ("ptail", tis(0))
        else b ~ ("ptail", Json(tis))
      }
    }
    b ~? ("walk", walks) ~~ custom ~ Json
  }

  override def toString = json.toString

  def similarTo(d: Datum, tol: Double, checkCentroids: Boolean): Boolean = similarTo(d.toData, tol, checkCentroids)

  def similarTo(d: Datum, tol: Double): Boolean = similarTo(d.toData, tol, true)

  def similarTo(d: Data, tol: Double): Boolean = similarTo(d, tol, true)

  def similarTo(d: Data, tol: Double, checkCentroids: Boolean): Boolean =
    (n == d.n) &&
    { var i = 0
      var close = true
      while (i < ts.length && close) {
        close = math.abs(ts(i) - d.ts(i)) <= tol
        i += 1
      }
      close
    } &&
    { var i = 0
      var close = true
      if (checkCentroids) while (i < cxs.length && close) {
        close = math.abs(cxs(i) - d.cxs(i)) < tol && math.abs(cys(i) - d.cys(i)) <= tol
        i += 1
      }
      close
    } &&
    { var i = 0
      var close = true
      while (i < n && close) {
        close = spineN(i) == d.spineN(i)
        val jN = spineN(i)
        var j = 0
        while (j < jN && close) {
          close = math.abs(x(i, j) - d.x(i, j)) < tol && math.abs(y(i, j) - d.y(i, j)) < tol
          j += 1
        }
        i += 1
      }
      close
    }
}
object Data extends FromJson[Data] {
  def sig(x: Double): Double =
    if (x < -1e9 || x > 1e9) x
    else math.rint(x*1e5).toLong/1e5

  def doubly(xs: Array[Float]): Array[Double] = {
    var qs = new Array[Double](xs.length)
    var i = 0
    while (i < xs.length) { qs(i) = xs(i); i += 1 }
    qs
  }
  def doubly(xs: Array[Float], rx: Double): Array[Double] = {
    var qs = new Array[Double](xs.length)
    var i = 0
    while (i < xs.length) { qs(i) = xs(i) + rx; i += 1 }
    qs
  }
  def doubly(xss: Array[Array[Float]]): Array[Array[Double]] = {
    var qss = new Array[Array[Double]](xss.length)
    var i = 0
    while (i < xss.length) { qss(i) = doubly(xss(i)); i += 1 }
    qss
  }
  def singly(xs: Array[Double]): Array[Float] = {
    var qs = new Array[Float](xs.length)
    var i = 0
    while (i < xs.length) { qs(i) = xs(i).toFloat; i += 1 }
    qs
  }
  def singly(xs: Array[Double], rx: Double): Array[Float] = {
    var qs = new Array[Float](xs.length)
    var i = 0
    while (i < xs.length) { qs(i) = (xs(i) + rx).toFloat; i += 1 }
    qs
  }
  def singly(xss: Array[Array[Double]]): Array[Array[Float]] = {
    var qss = new Array[Array[Float]](xss.length)
    var i = 0
    while (i < xss.length) { qss(i) = singly(xss(i)); i += 1 }
    qss
  }

  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid data entries: " + msg))
  private def IBAD(nid: Double, sid: String, msg: String): Either[JastError, Nothing] =
    BAD("Data points for " + IdOnly(nid,sid).idJSON.json + " have " + msg)
  private def MYBAD(nid: Double, sid: String, t: Double, msg: String): Either[JastError, Nothing] =
    BAD("Data point for " + IdOnly(nid,sid).idJSON.json + " at time " + t + " has " + msg)

  private[trackercommons] val emptyD = new Array[Double](0)
  private[trackercommons] val zeroD = Array(0.0)
  private[trackercommons] val emptyFF = new Array[Array[Float]](0)

  val empty = new Data(
    Double.NaN, "", emptyD, emptyFF, emptyFF, emptyD, emptyD, emptyD, emptyD, false, None, None, Json.Obj.empty
  )(emptyD, emptyD)

  private val someSingles = Option(Set("id", "t", "x", "y", "cx", "cy", "ox", "oy", "px", "py", "ptail", "walk"))

  def parse(j: Json): Either[JastError, Data] = {
    implicit val pixelwalkFromJson: FromJson[PixelWalk] = PixelWalk

    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    o.countKeys(someSingles).foreach{ case (key, n) => if (n > 1) return BAD("duplicate entries for " + key) }

    var numC = 0
    val (nid, sid) = o("id") match {
      case Json.Null => (Double.NaN, null: String)
      case n: Json.Num => (n.double, null: String)
      case Json.Str(s) => (Double.NaN, s)
      case _ => return BAD("no valid ID!")
    }
    val t: Array[Double] = o("t") match {
      case ja: Json.Arr.Dbl => ja.doubles
      case _ => return BAD("no time array!")
    }
    var numO = 0
    var unarr = false
    val List(ox, oy) = List("ox", "oy").map(key => o.get(key) match {
      case None => emptyD
      case Some(j) => j match {
        case n: Json.Num => unarr = true; Array(n.double)
        case ja: Json.Arr.Dbl => 
          if (ja.size != 1 && ja.size != t.length)
             return IBAD(nid, sid, f"$key array size does not match time series size!") 
          numO += 1
          ja.doubles
        case _ => return IBAD(nid, sid, f"non-numeric $key origin")
      }
    })
    if (ox.length != oy.length) IBAD(nid, sid, "ox and oy sizes do not match")
    if (numO == 1) return IBAD(nid, sid, "only one of ox, oy: include both or neither!")

    val List(cx, cy) = List("cx", "cy").map(key => o.get(key) match {
      case None => emptyD
      case Some(j) => j match {
        case ja: Json.Arr.Dbl =>
          if (ja.size != t.length) return IBAD(nid, sid, f"$key array size does not match time series size!")
          numC += 1
          ja.doubles
        case _=> return IBAD(nid, sid, f"non-numeric or improperly shaped $key")
      }
    })
    if (numC == 1) return IBAD(nid, sid, "only one of cx, cy: include both or neither!")

    val List((x0, px0), (y0, py0)) =
      List(("x", "px"), ("y", "py")).map{ case (key, peri) =>
        val ans =
          List(key, peri).map{ kk => o.get(kk) match {
            case None => if (kk.startsWith("p")) null else return IBAD(nid, sid, f"no $key!")
            case Some(j) => j match {
              case Json.Null => 
                if (kk.startsWith("p")) null
                else return IBAD(nid, sid, f"no $key!")
              case n: Json.Num =>
                if (t.length != 1) return IBAD(nid, sid, f"$key size does not match time series size!")
                Array(Array(n.double))
              case ja: Json.Arr.Dbl => 
                if (t.length == 1) Array(ja.doubles)
                else if (t.length == ja.size) ja.doubles.map(x => Array(x))
                else return IBAD(nid, sid, f"$key size does not match time series size!")
              case jall: Json.Arr.All =>
                if (jall.size != t.length) return IBAD(nid, sid, f"$key size does not match time series size!")
                jall.values.map(_ match {
                  case Json.Null => emptyD
                  case n: Json.Num => Array(n.double)
                  case ja: Json.Arr.Dbl => ja.doubles
                  case _ => return IBAD(nid, sid, f"$key has non-numeric data elements!")
                })
              case _ => return IBAD(nid, sid, f"non-numeric or improperly shaped $key")          
            }
          }}
        (ans.head, ans.tail.head)
      }
    if ((px0 eq null) != (py0 eq null)) return IBAD(nid, sid, "Only one of px or py present")
    var i = 0
    while (i < x0.length) {
      if (x0(i).length != y0(i).length) return MYBAD(nid, sid, t(i), "mismatch in x and y sizes!")
      if ((px0 ne null) && px0(i).length != py0(i).length) return MYBAD(nid, sid, t(i), "mismatch in px and py sizes!")
      i += 1
    }

    val walk0 = o.get("walk") match {
      case None => None
      case Some(j) => j match {
        case jaa: Json.Arr.All if jaa.size == t.length =>
          val pws = new Array[PixelWalk](t.length)
          var i = 0
          while (i < pws.length) {
            pws(i) = jaa(i) match {
              case Json.Null => PixelWalk.empty
              case j         => j.to[PixelWalk] match {
                case Right(pw) => pw
                case Left(je) => return MYBAD(nid, sid, t(i), "Can't read walk: " + je.toString)
              }
            }
            i += 1
          }
          Some(pws)
        }
    }

    val ptail: Option[Array[Int]] = 
      if ((px0 ne null) && (py0 ne null)) {
        o.get("ptail").flatMap{_ match {
          case Json.Null => None
          case jn: Json.Num if (jn.double.toInt == jn.double) => Some(Array.fill(t.length)(jn.double.toInt))
          case jad: Json.Arr.Dbl if jad.size == t.length => Some(jad.doubles.map(d =>
              if (d.isNaN) -1
              else if (d.toInt == d && d >= 0) d.toInt
              else return IBAD(nid, sid, "ptail isn't an appropriate number of integers") 
            ))
          case _ => return IBAD(nid, sid, "ptail isn't an appropriate number of integers")
        }}
      }
      else None

    val x, y = new Array[Array[Float]](t.length)
    val rx, ry = new Array[Double](t.length)
    val pms = if (px0 ne null) new Array[PerimeterPoints](t.length) else null
    i = 0
    while (i < x.length) {
      val oxi = if (ox.length < 1) Double.NaN else if (ox.length == 1) ox(0) else ox(i)
      val oyi = if (oy.length < 1) Double.NaN else if (oy.length == 1) oy(0) else oy(i)
      val datumi = Datum.datumFromValues(
        nid, sid, t(i),
        ox.length > 0, oxi, oyi,
        cx.length > 0, if (cx.length > 0) cx(i) else Double.NaN, if (cy.length > 0) cy(i) else Double.NaN,
        x0(i), y0(i),
        if (px0 eq null) null else px0(i), if (py0 eq null) null else py0(i),
        walk0.map(_(i)), ptail.flatMap(p => if (p(i) < 0) None else Some(p(i))),
        Json.Obj.empty
      )
      if (cx.length > 0) cx(i) = datumi.cx
      if (cy.length > 0) cy(i) = datumi.cy
      x(i) = datumi.xData
      y(i) = datumi.yData
      rx(i) = datumi.rx
      ry(i) = datumi.ry
      datumi.walk.foreach(w => walk0.get(i) = w)
      if (pms ne null) pms(i) = datumi.perim.get
      i += 1
    }
    Right(new Data(nid, sid, t, x, y, cx, cy, ox, oy, unarr, Option(pms), walk0, o.filter((k,_) => k.startsWith("@")))(rx, ry))
  }
}
