package org.openworm.trackercommons.compatibility

import kse.jsonal._

import org.openworm.{trackercommons => original}

trait WrapsScalaWcon[A] { underlying: A }

class Laboratory(val underlying: original.Laboratory)
extends WrapsScalaWcon[original.Laboratory] {
  def pi = underlying.pi
  def name = underlying.name
  def location = underlying.location
  def custom = underlying.custom
}
object Laboratory {
  val empty = new Laboratory(original.Laboratory.empty)

  def from(underlying: original.Laboratory): Laboratory = new Laboratory(underlying)

  def from(pi: String, name: String, location: String) =
    new Laboratory(original.Laboratory(pi, name, location, Json.Obj.empty))

  def from(pi: String, name: String, location: String, custom: Json.Obj) =
    new Laboratory(original.Laboratory(pi, name, location, custom))
}

class Arena(val underlying: original.Arena)
extends WrapsScalaWcon[Arena]
{
  def kind = underlying.kind
  def majorDiameter = underlying.diameter match { case Right(d) => d;          case Left((d1, d2)) => d1 }
  def minorDiameter = underlying.diameter match { case Right(_) => Double.NaN; case Left((_, d2))  => d2 }
  def orient = underlying.orient
  def custom = underlying.custom
}
object Arena {
  val empty = new Arena(original.Arena.empty)

  def from(kind: String, diameter: Double): Arena =
    new Arena(original.Arena(kind, Right(diameter), "", Json.Obj.empty))

  def from(kind: String, dmajor: Double, dminor: Double) =
    new Arena(original.Arena(kind, Left((dmajor, dminor)), "", Json.Obj.empty))

  def from(kind: String, diameter: Double, orient: String) =
    new Arena(original.Arena(kind, Right(diameter), orient, Json.Obj.empty))

  def from(kind: String, dmajor: Double, dminor: Double, orient: String) =
    new Arena(original.Arena(kind, Left((dmajor, dminor)), orient, Json.Obj.empty))

  def from(kind: String, diameter: Double, orient: String, custom: Json.Obj) =
    new Arena(original.Arena(kind, Right(diameter), orient, custom))

  def from(kind: String, dmajor: Double, dminor: Double, orient: String, custom: Json.Obj) =
    new Arena(original.Arena(kind, Left((dmajor, dminor)), orient, custom))
}

class Software(val underlying: original.Software) extends WrapsScalaWcon[original.Software]{
  def name = underlying.name
  def version = underlying.version
  def featureID = underlying.featureID.toArray.sorted
  def custom = underlying.custom
}
object Software {
  val empty = new Software(original.Software.empty)

  def from(name: String): Software = new Software(original.Software(name, "", Set(), Json.Obj.empty))

  def from(name: String, version: String): Software = new Software(original.Software(name, version, Set(), Json.Obj.empty))

  def from(name: String, featureIDs: Array[String]) = new Software(originalSoftware(name, "", featureIDs.toset, Json.Obj.empty))

  def from(name: String, version: String, featureIDs: Array[String]) =
    new Software(originalSoftware(name, version, featureIDs.toSet, Json.Obj.empty))

  def from(name: String, custom: Json.Obj): Software = new Software(original.Software(name, "", Set(), custom))

  def from(name: String, version: String custom: Json.Obj): Software =
    new Software(original.Software(name, version, Set(), custom))

  def from(name: String, featureIDs: Array[String] custom: Json.Obj): Software =
    new Software(originalSoftware(name, "", featureIDs.toset, custom))

  def from(name: String, version: String, featureIDs: Array[String] custom: Json.Obj) =
    new Software(originalSoftware(name, version, featureIDs.toSet, custom))
}

class Metadata(val underlying: original.Metadata) extends WrapsScalaWcon[Metadata] {
  def lab: Laboratory = underlying.lab.map(l => new Laboratory(l)).getOrElse(Laboratory.empty))
  def lab(l: Laboratory): Metadata = new Metadata(underlying.copy(lab = Option(l.underlying)))

  def who: Array[String] = if (underlying.who.length > 0) underlying.who.toArray else Metadata.emptyStringArray
  def who(w: Array[String]): metadata = new Metadata(underlying.copy(who = w.toVector))

  def timestamp = ???
  def temperature = underlying.temperature.getOrElse(Double.NaN)
  def humidity = underlying.humidity.getOrElse(Double.NaN)
  def arena = underlying.arena.map(a => new Arena(a)).getOrElse(Arena.empty)
  def food = underlying.food.getOrElse("")
  def media = underlying.media.getOrElse("")
  def sex = underlying.sex.getOrElse("")
  def stage = underlying.stage.getOrElse("")
  def age = underlying.age.getOrElse(Double.NaN)
  def strain = underlying.strain.getOrElse("")
  def protocol = if (underlying.protocol.length > 0) underlying.protocol.toArray else Metadata.emptyStringArray
  def software = if (underlying.software.length > 0) underlying.software.toArray.map(s => new Software(s)) else Metadata.emptySoftwareArray
  def settings = underlying.settings.getOrElse(Json.Null)
  def custom = underlying.custom  
}
object Metadata {
  val empty = new Metadata(org.openworm.trackercommons.Metadata.empty)
  val emptyStringArray = new Array[String]()
  val emptySoftwareArray = new Array[Software]()
}

class Data(
  val id: String,
  tsGen: => Array[Double],
  cxsGen: => Array[Double],
  cysGen: => Array[Double],
  xssGen: => Array[Array[Double]],
  yssGen: => Array[Array[Double]],
  custom: Json.Obj
) {
  val ts: Array[Double] = tsGen
  def t(i: Int): Double = underlying.ts(i)
  val cxs: Array[Double] = cxsGen
  def cx(i: Int) = if (cxs.length > 0) cxs(i) else Double.NaN
  val cys: Array[Double] = cysGen
  def cy(i: Int) = if (cys.length > 0) cys(i) else Double.NaN
  lazy val xss: Array[Array[Double]] = xssGen
  def xs(i: Int) = xss(i)
  def x(i: Int, j: Int) = xss(i)(j)
  lazy val yss: Array[Array[Double]] = yssGen
  def ys(i: Int): yss(i)
  def y(i: Int, j: Int): yss(i)(j)
  def custom = underlying.custom
  def toUnderlying = {
    val nid = Jast.parse(id).double
    val sid = if (nid.isNaN) id else ""
    val oxs = if (cxs.length == 0) org.openworm.trackercommons.Data.findFloatOffsets(xss) else Data.emptyDoubleArray
    val oys = if (cys.length == 0) org.openworm.trackercommons.Data.findFloatOffsets(yss) else Data.emptyDoubleArray
    new org.openworm.trackercommons.Data(
      nid, sid,
      java.util.Arrays.copyOf(ts, ts.length),
      xss.map(i => Data.singly(xss(i), if (cxs.length > 0) cxs(i) else if (oxs.length > 1) oxs(i) else if (oxs.length == 1) oxs(0) else 0.0)),
      yss.map(i => Data.singly(xss(i), if (cxs.length > 0) cxs(i) else if (oxs.length > 1) oxs(i) else if (oxs.length == 1) oxs(0) else 0.0)),
      java.util.Arrays.copyOf(cxs, cxs.length),
      java.util.Arrays.copyOf(cys, cys.length),
      oxs,
      oys
    )
  )
}
object Data {
  val emptyDoubleArray = new Array[Double](0)
  def empty = new Data(org.openworm.trackercommons.Data.empty)

  def from(id: String, ts: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, ts, emptyDoubleArray, emptyDoubleArray, xss, yss, Json.Obj.empty)

  def from(id: String, ts: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]], custom: Json.Obj): Data =
    new Data(id, ts, emptyDoubleArray, emptyDoubleArray, xss, yss, custom)

  def from(id: String, ts: Array[Double], cxs: Array[Double], cys: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, ts, cxs, cys, xss, yss, Json.Obj.empty)

  def from(id: String, ts: Array[Double], cxs: Array[Double], cys: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]], custom: Json.Obj): Data =
    new Data(id, ts, cxs, cys, xss, yss, custom)

  def from(underlying: org.openworm.trackercommons.Data): Data =
    new Data(
      if (underlying.nid.isNaN) underlying.sid else underlying.nid.toString,
      java.lang.Arrays.copyOf(underlying.ts, underlying.ts.length),
      java.lang.Arrays.copyOf(underlying.cxs, underlying.cxs.length),
      java.lang.Arrays.copyOf(underlying.cys, underlying.cys.length),
      underlying.xss.map(i =>
        org.openworm.trackercommons.Data.doubly(
          xss(i),
          if (underlying.cxs.length > 0 && underlying.cxs(i).finite) underlying.cxs(i) else 0.0
        )
      ),
      underlying.yss.map(i =>
        org.openworm.trackercommons.Data.doubly(
          yss(i),
          if (underlying.cys.length > 0 && underlying.cys(i).finite) underlying.cys(i) else 0.0
        )
      )
    )
}

class Wcon(val underlying: org.openworm.trackercommons.DataSet) {
  lazy val metadata = new Metadata(underlying.metadata)
  lazy val units = underlying.unitmap.json
  def thisFilename = underlying.files.me
  lazy val nextFilenames = underlying.files.names.drop(1 + underlying.files.index).toArray
  lazy val prevFilenames = underlying.files.names.take(underlying.files.index).toArray
  lazy val dataset: Array[Data] = underlying.data.map{x => new Data(x match{case Right(d) => d; case Left(dm) => dm.toData})}
  def custom = underlying.custom
}
