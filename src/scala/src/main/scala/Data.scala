package org.openworm.trackercommons

trait HasId extends math.Ordered[HasId] {
  def nid: Double
  def sid: String
  def idJSON: json.JSON = if (nid.isNaN) { if (sid == null) json.NullJ else json.StrJ(sid) } else json.NumJ(nid)
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

case class Datum(nid: Double, sid: String, t: Double, x: Array[Float], y: Array[Float], cx: Double, cy: Double, custom: json.ObjJ)
extends HasId with json.Jsonable {
  def toObjJ = json.ObjJ(
    Map[String, List[json.JSON]](
      "id" -> (idJSON :: Nil),
      "t" -> (json.NumJ(t) :: Nil),
      "x" -> (json.ANumJ(Data.doubly(x)) :: Nil),
      "y" -> (json.ANumJ(Data.doubly(y)) :: Nil),
      "cx" -> (json.NumJ(cx) :: Nil),
      "cy" -> (json.NumJ(cy) :: Nil)
    ) ++ custom.keyvals
  )
}
object Datum extends json.Jsonic[Datum] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid data entry: " + msg)
  private def MYBAD(nid: Double, sid: String, t: Double, msg: String): Either[String, Nothing] =
    BAD("Data point for " + IdOnly(nid,sid).idJSON.toJson + " at time " + t + " has " + msg)
  def from(ob: json.ObjJ): Either[String, Datum] = {
    val (nid, sid) = ob.keyvals.get("id") match {
      case None => return BAD("no ID!")
      case Some(j :: Nil) => j match {
        case json.NullJ => (Double.NaN, null: String)
        case json.NumJ(x) => (x, null: String)
        case json.StrJ(s) => (Double.NaN, s)
        case _ => return BAD("ID neither numeric nor text!")
      }
      case _ => return BAD("more than one ID!")
    }
    val t = ob.keyvals.get("t") match {
      case Some(json.NumJ(x) :: Nil) => x
      case _ => return BAD("no (unique) time")
    }
    val ox = ob.keyvals.get("ox") match {
      case Some(json.NumJ(x) :: Nil) => x
      case None => Double.NaN
      case _ => return MYBAD(nid, sid, t, "more than one entry for ox")
    }
    val oy = ob.keyvals.get("oy") match {
      case Some(json.NumJ(y) :: Nil) => y
      case None => Double.NaN
      case _ => return MYBAD(nid, sid, t, "more than one entry for oy")
    }
    var cx = ob.keyvals.get("cx") match {
      case Some(json.NumJ(x) :: Nil) => x
      case None => Double.NaN
      case _ => return MYBAD(nid, sid, t, "more than one entry for cx")
    }
    var cy = ob.keyvals.get("cy") match {
      case Some(json.NumJ(y) :: Nil) => y
      case None => Double.NaN
      case _ => return MYBAD(nid, sid, t, "more than one entry for cy")
    }
    val x = ob.keyvals.get("x") match {
      case None => return MYBAD(nid, sid, t, "no x!")
      case Some(j :: Nil) => j match {
        case json.NullJ => if (cx.isNaN) return MYBAD(nid, sid, t, "no x!") else Array(cx)
        case json.NumJ(xi) => Array(xi)
        case json.ANumJ(xi) => xi
        case _ => return MYBAD(nid, sid, t, "non-numeric or improperly shaped x")
      }
      case _ => return MYBAD(nid, sid, t, "more than one entry for x!")
    }
    val y = ob.keyvals.get("y") match {
      case None => return MYBAD(nid, sid, t, "no y!")
      case Some(j :: Nil) => j match {
        case json.NullJ => if (cy.isNaN) return MYBAD(nid, sid, t, "no y!") else Array(cy)
        case json.NumJ(yi) => Array(yi)
        case json.ANumJ(yi) => yi
        case _ => return MYBAD(nid, sid, t, "non-numeric or improperly shaped y")
      }
      case _ => return MYBAD(nid, sid, t, "more than one entry for y!")
    }
    if (y.length != x.length) return MYBAD(nid, sid, t, "mismatch in x and y sizes!")
    if (!ox.isNaN) { cx += ox }
    if (!oy.isNaN) { cy += oy }
    var rx = if (!ox.isNaN) ox else if (!cx.isNaN) cx else 0.0
    var ry = if (!oy.isNaN) oy else if (!cy.isNaN) cy else 0.0
    if (cx.isNaN) { 
      var xS = 0.0
      var xN, i = 0
      while (i < x.length) { val xi = x(i); if (!xi.isNaN) { xS += xi; xN +=1 }; i += 1 }
      if (xN > 0) cx = xS/xN
    }
    if (cy.isNaN) { 
      var yS = 0.0
      var yN, i = 0
      while (i < y.length) { val yi = y(i); if (!yi.isNaN) { yS += yi; yN +=1 }; i += 1 }
      if (yN > 0) cy = yS/yN
    }
    rx -= cx
    ry -= cy
    if (rx.isNaN || math.abs(rx) < 1e-9) rx = 0
    if (ry.isNaN || math.abs(ry) < 1e-9) ry = 0
    Right(new Datum(nid, sid, t, Data.singly(x, rx), Data.singly(y, ry), cx, cy, Metadata.getCustom(ob)))
  }
}

case class Data(nid: Double, sid: String, ts: Array[Double], xs: Array[Array[Float]], ys: Array[Array[Float]], cxs: Array[Double], cys: Array[Double], custom: json.ObjJ)
extends HasId with json.Jsonable {
  def toObjJ = json.ObjJ(
    Map[String, List[json.JSON]](
      "id" -> (idJSON :: Nil),
      "t" -> (json.ANumJ(ts) :: Nil),
      "x" -> (json.AANumJ(Data.doubly(xs)) :: Nil),
      "y" -> (json.AANumJ(Data.doubly(ys)) :: Nil),
      "cx" -> (json.ANumJ(cxs) :: Nil),
      "cy" -> (json.ANumJ(cys) :: Nil)
    ) ++ custom.keyvals
  )
}
object Data extends json.Jsonic[Data] {
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

  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid data entries: " + msg)
  private def IBAD(nid: Double, sid: String, msg: String): Either[String, Nothing] =
    BAD("Data points for " + IdOnly(nid,sid).idJSON.toJson + " have " + msg)
  private def MYBAD(nid: Double, sid: String, t: Double, msg: String): Either[String, Nothing] =
    BAD("Data point for " + IdOnly(nid,sid).idJSON.toJson + " at time " + t + " has " + msg)

  private val emptyD = new Array[Double](0)
  private val zeroD = Array(0.0)
  private val emptyDD = new Array[Array[Double]](0)

  def from(ob: json.ObjJ): Either[String, Data] = {
    val (nid, sid) = ob.keyvals.get("id") match {
      case None => return BAD("no ID!")
      case Some(j :: Nil) => j match {
        case json.NullJ => (Double.NaN, null: String)
        case json.NumJ(x) => (x, null: String)
        case json.StrJ(s) => (Double.NaN, s)
        case _ => return BAD("ID neither numeric nor text!")
      }
      case _ => return BAD("more than one ID!")
    }
    val t: Array[Double] = ob.keyvals.get("t") match {
      case Some(json.ANumJ(x) :: Nil) => x
      case _ => return BAD("no (unique) time array!")
    }
    val ox: Array[Double] = ob.keyvals.get("ox") match {
      case None => emptyD
      case Some(j :: Nil) => j match {
        case json.NumJ(x) => Array(x)
        case json.ANumJ(x) =>
          if (x.length != t.length) return IBAD(nid, sid, "origin x array size does not match time series size!")
          x
        case _ => return IBAD(nid, sid, "non-numeric x origin")
      }
      case _ => return IBAD(nid, sid, "more than one entry for ox!")
    }
    val oy: Array[Double] = ob.keyvals.get("oy") match {
      case None => emptyD
      case Some(j :: Nil) => j match {
        case json.NumJ(x) => Array(x)
        case json.ANumJ(x) =>
          if (x.length != t.length) return IBAD(nid, sid, "origin y array size does not match time series size!")
          x
        case _ => return IBAD(nid, sid, "non-numeric y origin")
      }
      case _ => return IBAD(nid, sid, "more than one entry for oy!")
    }
    var cx: Array[Double] = ob.keyvals.get("cx") match {
      case Some(j :: Nil) => j match {
        case json.ANumJ(x) =>
          if (x.length != t.length) return IBAD(nid, sid, "centroid x array size does not match time series size!")
          else x
        case _ => return IBAD(nid, sid, "non-numeric or improperly shaped cx")
      }
      case None => emptyD
      case _ => return IBAD(nid, sid, "more than one entry for cx")
    }
    var cy: Array[Double] = ob.keyvals.get("cy") match {
      case Some(j :: Nil) => j match {
        case json.ANumJ(x) =>
          if (x.length != t.length) return IBAD(nid, sid, "centroid x array size does not match time series size!")
          else x
        case _ => return IBAD(nid, sid, "non-numeric or improperly shaped cx")
      }
      case None => emptyD
      case _ => return IBAD(nid, sid, "more than one entry for cx")
    }
    val x: Array[Array[Double]] = ob.keyvals.get("x") match {
      case None => return IBAD(nid, sid, "no x!")
      case Some(j :: Nil) => j match {
        case json.NullJ =>
          if (cx.length == 0) return IBAD(nid, sid, "no x!")
          else if (ox.length == 0) Array.fill(cx.length)(zeroD)
          else cx.map(x => Array(x))
        case json.NumJ(xi) => if (t.length == 1) Array(Array(xi)) else return IBAD(nid, sid, "x size does not match time series size!")
        case json.ANumJ(xi) => 
          if (t.length == 1) Array(xi)
          else if (t.length == xi.length) xi.map(x => Array(x))
          else return IBAD(nid, sid, "x size does not match time series size!")
        case json.AANumJ(xi) =>
          if (xi.length == t.length) xi
          else return IBAD(nid, sid, "x size does not match time series size!")
        case _ => return IBAD(nid, sid, "non-numeric or improperly shaped x")
      }
      case _ => return IBAD(nid, sid, "more than one entry for x!")
    }
    val y: Array[Array[Double]] = ob.keyvals.get("y") match {
      case None => return IBAD(nid, sid, "no y!")
      case Some(j :: Nil) => j match {
        case json.NullJ => 
          if (cy.length == 0) return IBAD(nid, sid, "no y!")
          else if (oy.length == 0) Array.fill(cy.length)(zeroD)
          else cy.map(y => Array(y))
        case json.NumJ(yi) => if (t.length == 1) Array(Array(yi)) else return IBAD(nid, sid, "y size does not match time series size!")
        case json.ANumJ(yi) => 
          if (t.length == 1) Array(yi)
          else if (t.length == yi.length) yi.map(y => Array(y))
          else return IBAD(nid, sid, "y size does not match time series size!")
        case json.AANumJ(yi) =>
          if (yi.length == t.length) yi
          else return IBAD(nid, sid, "y size does not match time series size!")
        case _ => return IBAD(nid, sid, "non-numeric or improperly shaped y")
      }
      case _ => return IBAD(nid, sid, "more than one entry for y!")
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
    while (i < x.length) {
      val xi = x(i)
      val yi = y(i)
      var rx = if (ox.length > 0) ox(if (ox.length==1) 0 else i) else if (cx.length > 0) cx(i) else 0.0
      var ry = if (oy.length > 0) oy(if (oy.length==1) 0 else i) else if (cy.length > 0) cy(i) else 0.0
      val cxi = if (cx.length > 0) cx(i) else {
        var xS = 0.0
        var xN, j = 0
        while (j < xi.length) { val xij = xi(j); if (!xij.isNaN) { xS += xij; xN +=1 }; j += 1 }
        if (xN > 0) xS/xN else Double.NaN
      }
      val cyi = if (cy.length > 0) cy(i) else {
        var yS = 0.0
        var yN, j = 0
        while (j < yi.length) { val yij = yi(j); if (!yij.isNaN) { yS += yij; yN +=1 }; j += 1 }
        if (yN > 0) yS/yN else Double.NaN
      }
      rx -= cxi
      ry -= cyi
      if (!rx.isNaN && math.abs(rx) >= 1e-9) { var j = 0; while (j < xi.length) { xi(j) += rx; j += 1 } }
      if (!ry.isNaN && math.abs(ry) >= 1e-9) { var j = 0; while (j < yi.length) { yi(j) += ry; j += 1 } }
    }
    Right(new Data(nid, sid, t, Data.singly(x), Data.singly(y), cx, cy, Metadata.getCustom(ob)))
  }
}

object DatX extends json.Jsonic[Either[Datum, Data]] {
  def from(ob: json.ObjJ): Either[String, Either[Datum, Data]] = {
    Datum.from(ob) match {
      case Right(x) => Right(Left(x))
      case Left(e) => if (e.endsWith("!")) Left(e) else Data.from(ob) match {
        case Right(x) => Right(Right(x))
        case Left(e) => Left(e)
      }
    }
  }
}
