package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

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
  * Note that x and y are relative to cx and cy.
  * If the file did not specify a cx and cy, they are calculated, but `derivedCx` and `derivedCy` are set to true.
  */
case class Datum(nid: Double, sid: String, t: Double, x: Array[Float], y: Array[Float], cx: Double, cy: Double, custom: Json.Obj)
extends HasId with AsJson {
  var independentC = false
  var specifiedO = false
  def setCO(ic: Boolean, so: Boolean): this.type = { independentC = ic; specifiedO = so; this }
  def json = (Json 
      ~ ("id", idJSON) ~ ("t", t) ~ ("x", Data.doubly(x)) ~ ("y", Data.doubly(y))
      ~ (if (specifiedO && !independentC) "ox" else "cx", cx)
      ~ (if (specifiedO && !independentC) "oy" else "cy", cy)
      ~~ custom ~ Json)
  def toData: Data = (new Data(nid, sid, Array(t), Array(x), Array(y), Array(cx), Array(cy), custom)).setCO(independentC, specifiedO)
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
    var numO = 0
    var numC = 0
    val (nid, sid) = o.get("id") match {
      case None => return BAD("no ID!")
      case Some(j) => j match {
        case Json.Null => (Double.NaN, null: String)
        case n: Json.Num => (n.double, null: String)
        case Json.Str(s) => (Double.NaN, s)
        case _ => return BAD("ID neither numeric nor text!")
      }
    }
    val t = o("t") match {
      case n: Json.Num => n.double
      case _ => return BAD("no valid timepoint")
    }
    val List(ox, oy) = List("ox", "oy").map(key => o.get(key) match {
      case Some(n: Json.Num) => numO += 1; n.double
      case None => Double.NaN
      case _ => return BAD(f"$key is not numeric")
    })
    val List(cx0, cy0) = List("cx", "cy") map(key => o.get(key) match {
      case Some(n: Json.Num) => numC += 1; n.double
      case None => Double.NaN
      case _ => return BAD(f"$key is not numeric")
    })
    var cx = cx0
    var cy = cy0
    val List(x,y) = List(("x", cx), ("y", cy)).map{ case (key, c) => o(key) match {
      case Json.Null => if (c.isNaN) return MYBAD(nid, sid, t, f"no $key!") else Array(c)
      case n: Json.Num => Array(n.double)
      case ns: Json.Arr.Dbl => ns.doubles
      case _ => return MYBAD(nid, sid, t, f"no valid $key!")
    }}
    if (y.length != x.length) return MYBAD(nid, sid, t, "mismatch in x and y sizes!")
    if (!ox.isNaN) { cx += ox }
    if (!oy.isNaN) { cy += oy }
    var rx = if (!ox.isNaN) ox else if (!cx.isNaN) cx else 0.0
    var ry = if (!oy.isNaN) oy else if (!cy.isNaN) cy else 0.0
    if (numO == 1) return MYBAD(nid, sid, t, "only one of ox, oy: need both or neither!")
    if (numC == 1) return MYBAD(nid, sid, t, "only one of cx, cy: need both or neither!")
    if (cx.isNaN) {
      numC -= 1
      var xS = 0.0
      var xN, i = 0
      while (i < x.length) { val xi = x(i); if (!xi.isNaN) { xS += xi; xN +=1 }; i += 1 }
      if (xN > 0) cx = xS/xN
    }
    if (cy.isNaN) { 
      numC -= 1
      var yS = 0.0
      var yN, i = 0
      while (i < y.length) { val yi = y(i); if (!yi.isNaN) { yS += yi; yN +=1 }; i += 1 }
      if (yN > 0) cy = yS/yN
    }
    rx -= cx
    ry -= cy
    if (rx.isNaN || math.abs(rx) < 1e-9) rx = 0
    if (ry.isNaN || math.abs(ry) < 1e-9) ry = 0
    Right((new Datum(nid, sid, t, Data.singly(x, rx), Data.singly(y, ry), cx, cy, o.filter((k,_) => k.startsWith("@")))).setCO(numC > 0, numO > 0))
  }
}

/** Class to specify multiple timepoints on a single animal.
  * `x` and `y` values are relative to `cx` and `cy` values.
  * If the original data did not contain `cx` and `cy`, `derivedCx` and `derivedCy` is set.
  */
case class Data(
  nid: Double, sid: String,
  ts: Array[Double],
  xs: Array[Array[Float]], ys: Array[Array[Float]],
  cxs: Array[Double], cys: Array[Double],
  custom: Json.Obj
)
extends HasId with AsJson {
  var independentC = false
  var specifiedO = false
  def setCO(ic: Boolean, so: Boolean): this.type = { independentC = ic; specifiedO = so; this }
  def json = (Json
    ~ ("id", idJSON) ~ ("t", Json(ts)) ~ ("x", Json(xs.map(Data.doubly))) ~ ("y", Json(ys.map(Data.doubly)))
    ~ (if (specifiedO && !independentC) "ox" else "cx", cxs)
    ~ (if (specifiedO && !independentC) "oy" else "cy", cys)
    ~~ custom ~ Json)
  override def toString = json.toString
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

  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid data entries: " + msg))
  private def IBAD(nid: Double, sid: String, msg: String): Either[JastError, Nothing] =
    BAD("Data points for " + IdOnly(nid,sid).idJSON.json + " have " + msg)
  private def MYBAD(nid: Double, sid: String, t: Double, msg: String): Either[JastError, Nothing] =
    BAD("Data point for " + IdOnly(nid,sid).idJSON.json + " at time " + t + " has " + msg)

  private val emptyD = new Array[Double](0)
  private val zeroD = Array(0.0)
  private val emptyDD = new Array[Array[Double]](0)

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
    val List(ox, oy) = List("ox", "oy").map(key => o.get(key) match {
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
    val List(cx0, cy0) = List("cx", "cy").map(key => o.get(key) match {
      case None => emptyD
      case Some(j) => j match {
        case ja: Json.Arr.Dbl =>
          if (ja.size != t.length) return IBAD(nid, sid, f"$key array size does not match time series size!")
          numC += 1
          ja.doubles
        case _=> return IBAD(nid, sid, f"non-numeric or improperly shaped $key")
      }
    })
    var cx = cx0
    var cy = cy0
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
    val kx = if (cx.length == 0) new Array[Double](x.length) else emptyD
    val ky = if (cy.length == 0) new Array[Double](y.length) else emptyD
    while (i < x.length) {
      val xi = x(i)
      val yi = y(i)
      var rx = if (ox.length > 0) ox(if (ox.length==1) 0 else i) else if (cx.length > 0) cx(i) else 0.0
      var ry = if (oy.length > 0) oy(if (oy.length==1) 0 else i) else if (cy.length > 0) cy(i) else 0.0
      val cxi = if (cx.length > 0) cx(i) else {
        var xS = 0.0
        var xN, j = 0
        while (j < xi.length) { val xij = xi(j); if (!xij.isNaN) { xS += xij; xN +=1 }; j += 1 }
        kx(i) = if (xN > 0) xS/xN else Double.NaN
        kx(i)
      }
      val cyi = if (cy.length > 0) cy(i) else {
        var yS = 0.0
        var yN, j = 0
        while (j < yi.length) { val yij = yi(j); if (!yij.isNaN) { yS += yij; yN +=1 }; j += 1 }
        ky(i) = if (yN > 0) yS/yN else Double.NaN
        ky(i)
      }
      rx -= cxi
      ry -= cyi
      if (!rx.isNaN && math.abs(rx) >= 1e-9) { var j = 0; while (j < xi.length) { xi(j) += rx; j += 1 } }
      if (!ry.isNaN && math.abs(ry) >= 1e-9) { var j = 0; while (j < yi.length) { yi(j) += ry; j += 1 } }
      i += 1
    }
    Right(
      (
        new Data(nid, sid, t, Data.singly(x), Data.singly(y), if (kx.length > 0) kx else cx, if (ky.length > 0) ky else cy, o.filter((k,_) => k.startsWith("@")))
      ).setCO(numC > 0, numO > 0)
    )
  }
}
