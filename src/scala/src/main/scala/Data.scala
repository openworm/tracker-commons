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

/** Class to specify a single timepoint on a single animal.
  * Note that `x` and `y` are relative to `cx` and `cy`, if finite;
  * otherwise they are relative to `ox` and `oy`.
  * 
  * To set the policy for encoding the positions, set `originPolicy` to
  * `Data.OriginPolicy.Never` if the origin should never be output (instead propagated to x, y, cx, cy);
  * `Data.OriginPolicy.Needed` if the origin should be output only when the centroid is missing;
  * or `Data.OriginPolicy.Always` if the origin should be output whenever it is present.
  */
case class Datum(
  nid: Double, sid: String,
  t: Double, x: Array[Float], y: Array[Float],
  cx: Double, cy: Double,
  ox: Double, oy: Double,
  custom: Json.Obj
) extends HasId with AsJson {
  var originPolicy: Data.OriginPolicy = Data.OriginPolicy.Needed
  private[this] val hasO = !(ox.isNaN || oy.isNaN || ox.isInfinite || oy.isInfinite || (ox == 0 && oy == 0))
  private[this] val hasC = !(cx.isNaN || cy.isNaN || cx.isInfinite || cy.isInfinite)
  private val gxFix = (if (hasC) cx else if (hasO) ox else 0)
  private val gyFix = (if (hasC) cx else if (hasO) ox else 0)

  /** x coordinate, global coordinates */
  def gx(i: Int): Double = x(i) + gxFix

  /** y coordinate, global coordinates */
  def gy(i: Int): Double = y(i) + gyFix

  def json = {
    val b = Json ~ ("id", idJSON) ~ ("t", t)
    val dbx = Data.doubly(x)
    val dby = Data.doubly(y)
    originPolicy match {
      case Data.OriginPolicy.Needed =>
        if (hasC) b ~ ("cx", cx) ~ ("cy", cy)
        else if (hasO) b ~ ("ox", ox) ~ ("oy", oy)
      case Data.OriginPolicy.Never =>
        if (hasC) b ~ ("cx", cx) ~ ("cy", cy)
        else if (hasO) {
          var i = 0; while (i < dbx.length) { dbx(i) += ox; dby(i) += oy; i += 1 }
        }
      case Data.OriginPolicy.Always =>
        if (hasO) {
          if (hasC) b ~ ("cx", cx - ox) ~ ("cy", cy - oy)
          b ~ ("ox", ox) ~ ("oy", oy)
        }
        else if (hasC) {
          b ~ ("cx", 0) ~ ("cy", 0) ~ ("ox", ox) ~ ("oy", oy)
        }
        else {
          var lx, ly = 0.0
          var nx, ny = 0
          var i = 0
          while (i < dbx.length) {
            if (dbx(i).finite) { lx += dbx(i); nx += 1 }
            if (dby(i).finite) { ly += dby(i); ny += 1 }
            i += 1
          }
          lx = lx / math.max(1, nx)
          ly = ly / math.max(1, ny)
          b ~ ("ox", lx) ~ ("oy", ly)
          i = 0
          while (i < dbx.length) { dbx(i) -= lx; dby(i) -= ly; i += 1 }
        }
    }
    b ~ ("x", dbx) ~ ("y", dby) ~~ custom ~ Json
  }
  def toData: Data = {
    val d = new Data(
      nid, sid,
      Array(t), Array(x), Array(y),
      if (hasC) Array(cx) else Data.emptyD, if (hasC) Array(cy) else Data.emptyD,
      if (hasO) Array(ox) else Data.emptyD, if (hasO) Array(oy) else Data.emptyD,
      custom
    )
    d.originPolicy = originPolicy
    d
  }
  def similarTo(d: Datum, tol: Double, checkCentroids: Boolean): Boolean =
    math.abs(t - d.t) <= tol &&
    (!checkCentroids || (cx.finite == d.cx.finite && cy.finite == d.cy.finite)) &&
    (if (checkCentroids && (cx.finite || cy.finite)) math.abs(cx - d.cx) + math.abs(cy - d.cy) <= 2*tol else true) &&
    (x.length == d.x.length) &&
    { var i = 0;
      var close = true;
      while (i < x.length && close) {
        close = (x(i) + gxFix - d.x(i) - d.gxFix) <= tol
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
  def parse(j: Json): Either[JastError, Datum] = {
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
    val cx = if (finC > 0 && finO > 0) cx0 + ox else cx0
    val cy = if (finC > 0 && finO > 0) cy0 + oy else cy0
    val List((x, rx), (y, ry)) = List(("x", cx, ox), ("y", cy, oy)).map{ case (key, c, ori) => 
      val values = o(key) match {
        case Json.Null => if (finC == 0) return MYBAD(nid, sid, t, f"no $key!") else Array(c)
        case n: Json.Num => Array(n.double)
        case ns: Json.Arr.Dbl => ns.doubles
        case _ => return MYBAD(nid, sid, t, f"no valid $key!")
      }
      val ri =
        if (finC > 0 || finO > 0) ori
        else {
          var s = 0.0
          var n, i = 0
          while (i < values.length) { val vi = values(i); if (!vi.isNaN && !vi.isInfinite) { s += vi; n += 1 }; i += 1 }
          if (n > 0) {
            val r = s/n
            if (!r.isInfinite && math.abs(r) > 1e-9) {
              i = 0
              while (i < values.length) { values(i) -= r; i += 1 }
              r
            }
            else 0.0
          }
          else 0.0
        }
      (values, ri)
    }
    Right((new Datum(nid, sid, t, Data.singly(x), Data.singly(y), cx, cy, rx, ry, o.filter((k,_) => k.startsWith("@")))))
  }
}

/** Class to specify multiple timepoints on a single animal.
  * `x` and `y` values are relative to `cx` and `cy` values, if present, `ox` and `oy` otherwise.
  * If all values of `ox` and `oy` are the same, they may be specified by an array of length 1 to save space.
  */
case class Data(
  nid: Double, sid: String,
  ts: Array[Double],
  xs: Array[Array[Float]], ys: Array[Array[Float]],
  cxs: Array[Double], cys: Array[Double],
  oxs: Array[Double], oys: Array[Double],
  custom: Json.Obj
)
extends HasId with AsJson {
  assert(
    (ts ne null) && (xs ne null) && (ys ne null) && (cxs ne null) && (cys ne null) && (oxs ne null) && (oys ne null) &&
    ts.length == xs.length &&
    ts.length == ys.length &&
    { cxs.length == ts.length || cxs.length == 0 } &&
    { oxs.length == ts.length || oxs.length == 1 || oxs.length == 0 } &&
    cxs.length == cys.length &&
    oxs.length == oys.length &&
    { var i = 0
      var good = true
      while (good && i < xs.length) {
        good = (xs(i) ne null) && (ys(i) ne null) && xs(i).length == ys(i).length
        i += 1
      }
      good
    }
  )

  private val isGlobalOffset = cxs.length == 0 && oxs.length == 1 && oxs(0).finite && oys(0).finite
  private[this] val isAnyOffset = cxs.length > 0 || oxs.length > 0 || isGlobalOffset
  private val globalOffsetX = if (oxs.length == 1) oxs(0) else 0.0
  private val globalOffsetY = if (oys.length == 1) oys(0) else 0.0

  var originPolicy: Data.OriginPolicy = Data.OriginPolicy.Needed

  private def computeDeltaCxy(): (Array[Double], Array[Double]) =
    if (cxs.length == 0 || oxs.length == 0 || (oxs.length == 1 && !(oxs(0).finite && oys(1).finite))) (cxs, cys)
    else {
      var dcx, dcy = new Array[Double](ts.length)
      var i = 0
      if (oxs.length == 1) {
        while (i < dcx.length) {
          dcx(i) = cxs(i) - globalOffsetX
          dcy(i) = cys(i) - globalOffsetY
          i += 1
        }
      }
      else {
        while (i < dcx.length) {
          if (oxs(i).finite && oys(i).finite) {
            dcx(i) = cxs(i) - oxs(i)
            dcy(i) = cys(i) - oys(i)
          }
          i += 1
        }        
      }
      (dcx, dcy)
    }

  /** spine x in global coordinates, point by point */
  def gx(i: Int, j: Int) =
    if (isAnyOffset)
      xs(i)(j) + (
        if (cxs.length > 0) cxs(i)
        else if (isGlobalOffset) globalOffsetX
        else oxs(i)
      )
    else xs(i)(j)

  /** spine y in global coordinates, point by point */
  def gy(i: Int, j: Int) =
    if (isAnyOffset)
      ys(i)(j) + (
        if (cys.length > 0) cys(i)
        else if (isGlobalOffset) globalOffsetY
        else oys(i)
      )
    else ys(i)(j)

  /** Entire spine x, in global coordinates */
  def gxs(i: Int): Array[Double] =
    if (!isAnyOffset) Data.doubly(xs(i))
    else (Data.doubly(xs(i), if (cxs.length > 0) cxs(i) else if (isGlobalOffset) globalOffsetY else oxs(i)))

  /** Entire spine y, in global coordinates */
  def gys(i: Int): Array[Double] =
    if (!isAnyOffset) Data.doubly(ys(i))
    else (Data.doubly(ys(i), if (cys.length > 0) cys(i) else if (isGlobalOffset) globalOffsetY else oys(i)))

  def json = {
    val b = Json ~ ("id", idJSON) ~ ("t", ts)
    val dxs = Data.doubly(xs)
    val dys = Data.doubly(ys)
    originPolicy match {
      case Data.OriginPolicy.Needed =>
        if (cxs.length > 0) b ~ ("cx", cxs) ~ ("cy", cys)
        else if (oxs.length > 0) b ~ ("ox", oxs) ~ ("oy", oys)
      case Data.OriginPolicy.Always =>
        if (oxs.length > 0) {
          if (cxs.length > 0) {
            val (fixedCxs, fixedCys) = computeDeltaCxy()
            b ~ ("cx", fixedCxs) ~ ("cy", fixedCys)
          }
          b ~ ("ox", oxs) ~ ("oy", oys)
        }
        else if (cxs.length > 0) {
          val zeros = new Array[Double](oxs.length)
          b ~ ("cx", zeros) ~ ("cy", zeros) ~ ("ox", cxs) ~ ("oy", cys)
        }
        else {
          val ax, ay = new Array[Double](ts.length)
          var i = 0
          while (i < ax.length) {
            val dxsi = dxs(i)
            val dysi = dys(i)
            var qx, qy = 0.0
            var n, j = 0
            while (j < dxsi.length) {
              if (dxsi(j).finite && dysi(j).finite) {
                n += 1
                qx += dxsi(j)
                qy += dysi(j)
              }
              j += 1
            }
            if (n > 0) {
              qx /= n
              qy /= n
              j = 0
              while (j < dxsi.length) {
                if (dxsi(j).finite) dxsi(j) -= qx
                if (dysi(j).finite) dysi(j) -= qy
                j += 1
              }
            }
            ax(i) = qx
            ay(i) = qy
            i += 1
          }
          b ~ ("ox", ax) ~ ("oy", ay)
        }
      case Data.OriginPolicy.Never =>
        var i = 0
        if (oxs.length > 0) {
          if (cxs.length > 0) {
            val (fixedCxs, fixedCys) = computeDeltaCxy()
            b ~ ("cx", fixedCxs) ~ ("cy", fixedCys)
            while (i < dxs.length) {
              if (!(cxs(i).finite && cys(i).finite) && oxs(i).finite && oys(i).finite) {
                val x0 = oxs(i)
                val y0 = oys(i)
                val dxsi = dxs(i)
                val dysi = dys(i)
                var j = 0
                while (j < dxsi.length) { dxsi(j) += x0; dysi(j) += y0; j += 1 }
              }
            }
          }
          else {
            while (i < dxs.length) {
              if (isGlobalOffset || (oxs(i).finite && oys(i).finite)) {
                val x0 = if (isGlobalOffset) globalOffsetX else oxs(i)
                val y0 = if (isGlobalOffset) globalOffsetY else oys(i)
                val dxsi = dxs(i)
                val dysi = dys(i)
                var j = 0
                while (j < dxsi.length) { dxsi(j) += x0; dysi(j) += y0; j += 1 }
              }
            }
          }
        }
        else if (cxs.length > 0) b ~ ("cx", cxs) ~ ("cy", cys)
    }
    b ~ ("x", dxs) ~ ("y", dys) ~~ custom ~ Json
  }

  override def toString = json.toString

  def similarTo(d: Datum, tol: Double, checkCentroids: Boolean): Boolean = similarTo(d.toData, tol, checkCentroids)

  def similarTo(d: Datum, tol: Double): Boolean = similarTo(d.toData, tol, true)

  def similarTo(d: Data, tol: Double): Boolean = similarTo(d, tol, true)

  def similarTo(d: Data, tol: Double, checkCentroids: Boolean): Boolean =
    (ts.length == d.ts.length) &&
    { var i = 0
      var close = true
      while (i < ts.length && close) {
        close = math.abs(ts(i) - d.ts(i)) <= tol
        i += 1
      }
      close
    } &&
    (cxs.length == d.cxs.length || !checkCentroids) &&
    { var i = 0
      var close = true
      if (checkCentroids) while (i < cxs.length && close) {
        close = math.abs(cxs(i) - d.cxs(i)) + math.abs(cys(i) - d.cys(i)) <= 2*tol
        i += 1
      }
      close
    } &&
    (xs.length == d.xs.length) &&
    { var i = 0
      var close = true
      while (i < xs.length && close) {
        close = xs(i).length == d.xs(i).length
        val dX =
          (if (cxs.length > 0) cxs(i) else if (oxs.length > 1) oxs(i) else globalOffsetX) -
          (if (d.cxs.length > 0) d.cxs(i) else if (d.oxs.length > 1) d.oxs(i) else d.globalOffsetX)
        val dY =
          (if (cys.length > 0) cys(i) else if (oys.length > 1) oys(i) else globalOffsetY) -
          (if (d.cys.length > 0) d.cys(i) else if (d.oys.length > 1) d.oys(i) else d.globalOffsetY)
        var j = 0
        while (j < xs(i).length && close) {
          close = math.abs(xs(i)(j) - d.xs(i)(j) + dX) + math.abs(ys(i)(j) - d.ys(i)(j) + dY) <= 2*tol
          j += 1
        }
        i += 1
      }
      close
    }
}
object Data extends FromJson[Data] {
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

  def findFloatOffsets(zss: Array[Array[Double]]): Array[Double] = ???

  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid data entries: " + msg))
  private def IBAD(nid: Double, sid: String, msg: String): Either[JastError, Nothing] =
    BAD("Data points for " + IdOnly(nid,sid).idJSON.json + " have " + msg)
  private def MYBAD(nid: Double, sid: String, t: Double, msg: String): Either[JastError, Nothing] =
    BAD("Data point for " + IdOnly(nid,sid).idJSON.json + " at time " + t + " has " + msg)

  private[trackercommons] val emptyD = new Array[Double](0)
  private[trackercommons] val zeroD = Array(0.0)
  private[trackercommons] val emptyDD = new Array[Array[Double]](0)

  sealed trait OriginPolicy {}
  object OriginPolicy {
    case object Never extends OriginPolicy {}
    case object Needed extends OriginPolicy {}
    case object Always extends OriginPolicy {}
  }

  private val someSingles = Option(Set("id", "t", "x", "y", "cx", "cy", "ox", "oy"))

  def parse(j: Json): Either[JastError, Data] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    o.countKeys(someSingles).foreach{ case (key, n) => if (n > 1) return BAD("duplicate entries for " + key) }

    var numO = 0
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
    val List(ox0, oy0) = List("ox", "oy").map(key => o.get(key) match {
      case None => emptyD
      case Some(j) => j match {
        case n: Json.Num => Array(n.double)
        case ja: Json.Arr.Dbl => 
          if (ja.size != 1 && ja.size != t.length)
             return IBAD(nid, sid, f"$key array size does not match time series size!") 
          numO += 1
          ja.doubles
        case _ => return IBAD(nid, sid, f"non-numeric $key origin")
      }
    })
    var ox = ox0
    var oy = oy0
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
    if (numO == 1) return IBAD(nid, sid, "only one of ox, oy: include both or neither!")
    if (numC == 1) return IBAD(nid, sid, "only one of cx, cy: include both or neither!")
    val List(x, y) = List(("x", cx, ox), ("y", cy, oy)).map{ case (key, c, ori) => o(key) match {
        case Json.Null =>
          if (c.length == 0) return IBAD(nid, sid, f"no $key!")
          else if (ori.length == 0) Array.fill(c.length)(zeroD)
          else c.map(x => Array(x))
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
    }
    var i = 0
    while (i < x.length) {
      if (x(i).length != y(i).length) return MYBAD(nid, sid, t(i), "mismatch in x and y sizes!")
      i += 1
    }
    if (ox.length > 0 && cx.length > 0) {
      if (ox.length == 1) { var i = 0; while (i < cx.length) { cx(i) += ox(0); i += 1 } }
      else { var i = 0; while (i < cx.length) { cx(i) += ox(i); i +=1 } }
    }
    if (oy.length > 0 && cy.length > 0) {
      if (oy.length == 1) { var i = 0; while (i < cy.length) { cy(i) += oy(0); i += 1 } }
      else { var i = 0; while (i < cy.length) { cy(i) += oy(i); i +=1 } }
    }
    i = 0
    /*
    if (ox.length == 0 && cx.length == 0) {
      var tentox, tentoy = new Array[Double](x.length)
      var fracerrsq = 0.0
      var i = 0
      while (i < x.length) {
        var j = 0
        var myminx, mymaxx, myminy, mymaxy = Double.NaN
        var e = 0.0
        while (j < x(i).length) {
          val xij = x(i)(j)
          val yij = y(i)(j)
          if (xij.finite) {
            if (!(myminx.finite && myminx <= xij)) myminx = xij
            if (!(mymaxx.finite && mymaxx >= xij)) mymaxx = xij
          }
          if (yij.finite) {
            if (!(myminy.finite && myminy <= yij)) myminy = yij
            if (!(mymaxy.finite && mymaxy >= yij)) mymaxy = yij
          }
          if (xij.finite && yij.finite) e = math.max(e, math.max(math.abs(xij - xij.toFloat), math.abs(yij - yij.toFloat)))
          j += 1
        }
        var sepsq = 0.0
        if (myminx.finite && mymaxx.finite) {
          val aminx = math.abs(myminx)
          val amaxx = math.abs(mymaxx)
          val dx = mymaxx - myminx
          tentox(i) = if (dx < 2*math.min(aminx, amaxx)) (myminx + mymaxx)*0.5 else 0.0
          sepsq += dx*dx
        }
        else tentox(i) = 0.0
        if (myminy.finite && mymaxy.finite) {
          val aminy = math.abs(myminy)
          val amaxy = math.abs(mymaxy)
          val dy = mymaxy - myminy
          tentoy(i) = if (dy < 2*math.min(aminy, amaxy)) (myminy + mymaxy)*0.5 else 0.0
          sepsq += dy*dy
        }
        if (sepsq > 0) fracerrsq = math.max(fracerrsq, (e*e) / sepsq)
        i += 1
      }
      if (fracerrsq > 1e-6) {
        ox = tentox
        oy = tentoy
      }
    }
    */
    Right(new Data(nid, sid, t, Data.singly(x), Data.singly(y), cx, cy, ox, oy, o.filter((k,_) => k.startsWith("@"))))
  }
}
