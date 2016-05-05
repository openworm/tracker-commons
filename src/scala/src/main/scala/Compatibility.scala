/** A Java/JVM compatibility layer for the Scala implementation of the Tracker Commons WCON format.
  *
  * As a multi-paradigm language, Scala makes heavy use of closures and monadic data structures in
  * addition to inheritance hierarchies and primitive data types.  This facilitates rapid and safe
  * development in Scala, but can pose a barrier for other JVM-compatible languages that wish to
  * consume a Scala API.
  * 
  * This compatibility layer presents a view into the Scala WCON data structures that adhere to
  * standard practices in Java, especially those that aid interoperability with other JVM languages.
  *
  * In particular, it presents the six core data structures, `Arena`, `Laboratory`, `Software`, 
  * `Metadata`, `Data`, and `Wcon` as simple classes with fluent setters/getters (that is, to
  * to get the name field, it's `x.name`, and to set it, it's `x.name(newName)`).  Arrayed data
  * is all stored in `Array`s.  Factory methods exist to create the core structures using the
  * most common options.
  *
  * (Note that all data structures are immutable, so fluent setters actually return a new copy of
  * the data structure with the field set as desired.)
  *
  * Extended features are not fully supported.  Units are converted to defaults (mm, etc.) but cannot
  * be changed.  If there are multiple Wcon files, they must be each written out manually.
  * This provides a simple and effective interface to the key features of the WCON data format.
  */

package org.openworm.trackercommons.compatibility

import kse.jsonal._

import org.openworm.{trackercommons => original}

trait WrapsScalaWcon[A] { underlying: A }

class Laboratory(val underlying: original.Laboratory)
extends WrapsScalaWcon[original.Laboratory] {
  def pi = underlying.pi
  def pi(p: String) = new Laboratory(underlying.copy(pi = p))
  def name = underlying.name
  def name(n: String) = new Laboratory(underlying.copy(name = n))
  def location = underlying.location
  def location(l: String) = new Laboratory(underlying.copy(location = l))
  def custom = underlying.custom
  def custom(c: Json.Obj) = new Laboratory(underlying.copy(custom = c))
}
object Laboratory {
  val empty = new Laboratory(original.Laboratory.empty)

  def from(underlying: original.Laboratory): Laboratory = new Laboratory(underlying)

  def from(pi: String, name: String, location: String) =
    new Laboratory(original.Laboratory(pi, name, location, Json.Obj.empty))
}

class Arena(val underlying: original.Arena)
extends WrapsScalaWcon[Arena]
{
  def kind = underlying.kind
  def kind(k: String) = new Arena(underlying.copy(kind = k))
  def majorDiameter = underlying.diameter match { case Right(d) => d;          case Left((d1, d2)) => d1 }
  def majorDiameter(d: Double) = new Arena(underlying.diameter match {
    case Right(_) => underlying.copy(diameter = Right(d))
    case Left((_, d2)) => underlying.copy(diameter = Left((d, d2)))
  })
  def minorDiameter = underlying.diameter match { case Right(_) => Double.NaN; case Left((_, d2))  => d2 }
  def minorDiameter(d: Double) = underlying.diameter match {
    case Right(d1) => 
      if (d.finite) new Arena(underlying.copy(diameter = Left((d1, d)))
      else this
    case Left((d1, d2)) =>
      new Arena(underlying.copy(
        diameter = if (d.finite) Left((d1, d)) else Right(d1)
      ))
  })
  def orient = underlying.orient
  def orient(o: String) = new Arena(underlying.copy(orient = o))
  def custom = underlying.custom
  def custom(c: Json.Obj) = new Arena(underlying.copy(custom = c))
}
object Arena {
  val empty = new Arena(original.Arena.empty)

  def from(underlying: original.Arena) = new Arena(underlying)

  def from(kind: String, diameter: Double): Arena =
    new Arena(original.Arena(kind, Right(diameter), "", Json.Obj.empty))

  def from(kind: String, dmajor: Double, dminor: Double) =
    new Arena(original.Arena(kind, Left((dmajor, dminor)), "", Json.Obj.empty))

  def from(kind: String, diameter: Double, orient: String) =
    new Arena(original.Arena(kind, Right(diameter), orient, Json.Obj.empty))

  def from(kind: String, dmajor: Double, dminor: Double, orient: String) =
    new Arena(original.Arena(kind, Left((dmajor, dminor)), orient, Json.Obj.empty))
}

class Software(val underlying: original.Software) extends WrapsScalaWcon[original.Software] {
  def name = underlying.name
  def name(n: String) = new Software(underlying.copy(name = n))
  def version = underlying.version
  def version(v: String) = new Software(underlying.copy(version = v))
  def featureID = underlying.featureID.toArray.sorted
  def featureID(fs: Array[String]) = new Software(underlying.copy(featureID = fs.toSet))
  def addFeatureID(s: String) = new Software(underlying.copy(featureID = underlying.featureID + s))
  def removeFeatureID(s: String) =
    if (underlying.featureID contains s) new Software(underlying.copy(featureID = underlying.featureID - s))
    else this
  def custom = underlying.custom
  def custom(c: Json.Obj) = new Software(underlying.copy(custom = c))
}
object Software {
  val empty = new Software(original.Software.empty)

  def from(underlying: original.Software) = new Software(underlying)

  def from(name: String): Software = new Software(original.Software(name, "", Set(), Json.Obj.empty))

  def from(name: String, version: String): Software = new Software(original.Software(name, version, Set(), Json.Obj.empty))

  def from(name: String, featureIDs: Array[String]) = new Software(originalSoftware(name, "", featureIDs.toset, Json.Obj.empty))

  def from(name: String, version: String, featureIDs: Array[String]) =
    new Software(originalSoftware(name, version, featureIDs.toSet, Json.Obj.empty))
}

class Metadata(val underlying: original.Metadata) extends WrapsScalaWcon[Metadata] {
  def labs: Array[Laboratory] = underlying.lab.map(l => new Laboratory(l))
  def labs(ls: Array[Laboratory]): Metadata = new Metadata(underlying.copy(lab = ls.view.map(_.underlying).toVector))
  def addLab(l: Laboratory) = new Metadata(underlying.copy(lab = lab :+ l.underlying))

  def whos: Array[String] = if (underlying.who.length > 0) underlying.who.toArray else Metadata.emptyStringArray
  def whos(ws: Array[String]) = new Metadata(underlying.copy(who = ws.toVector))
  def addWho(w: String) = new Metadata(underlying.copy(who = who :+ w))

  def timestamp = ???

  def temperature = underlying.temperature.getOrElse(Double.NaN)
  def temperature(t: Double) = new Metadata(underlying.copy(temperature = if (t.finite) Some(t) else None))

  def humidity = underlying.humidity.getOrElse(Double.NaN)
  def humidity(h: Double) = new Metadata(underlying.copy(humidity = if (h.finite) Some(h) else None))

  def arena = underlying.arena.map(a => new Arena(a)).getOrElse(Arena.empty)
  def arena(a: Arena) = new Metadata(underlying.copy(arena = if (a.isEmpty) None else Some(a.underlying)))

  def food = underlying.food.getOrElse("")
  def food(f: String) = new Metadata(underlying.copy(food = if (f.isEmpty) None else Some(f)))

  def media = underlying.media.getOrElse("")
  def media(m: String) = new Metadata(underlying.copy(media = if (m.isEmpty) None else Some(m)))

  def sex = underlying.sex.getOrElse("")
  def sex(s: String) = new Metadata(underlying.copy(sex = if (s.isEmpty) None else Some(s)))

  def stage = underlying.stage.getOrElse("")
  def stage(s: String) = new Metadata(underlying.copy(stage = if (s.isEmpty) None else Some(s)))

  def age = underlying.age.getOrElse(Double.NaN)
  def age(a: Double) = new Metadata(underlying.copy(age = if (a.finite) Some(a) else None))

  def strain = underlying.strain.getOrElse("")
  def strain(s: String) = new Metadata(underlying.copy(strain = if (s.isEmpty) None else Some(s)))

  def protocols = if (underlying.protocol.length > 0) underlying.protocol.toArray else Metadata.emptyStringArray
  def protocols(ps: Array[Protocol]) = new Metadata(underlying.copy(protocol = ps.view.map(_.underlying).toVector))
  def addProtocol(p: Protocol) = new Metadata(underlying.copy(protocol = underlying.protocol :+ p.underlying))

  def softwares = if (underlying.software.length > 0) underlying.software.toArray.map(s => new Software(s)) else Metadata.emptySoftwareArray
  def softwares(ss: Array[Software]) = new Metadata(underlying.copy(software = ss.view.map(_.underlying).toVector))
  def addSoftware(s: Software) = new Metadata(underlying.copy(software = underlying.software :+ s.underlying))

  def settings = underlying.settings.getOrElse(Json.Null)
  def settings(s: Json) = new Metadata(underlying.copy(settings = Option(s)))

  def custom = underlying.custom
  def custom(c: Json.Obj) = new Metadata(custom = c)
}
object Metadata {
  val empty = new Metadata(org.openworm.trackercommons.Metadata.empty)
  val emptyStringArray = new Array[String]()
  val emptySoftwareArray = new Array[Software]()
}

class Data(
  val id: String,
  tsGen: () => Array[Double],
  cxsGen: () => Array[Double],
  cysGen: () => Array[Double],
  xssGen: () => Array[Array[Double]],
  yssGen: () => Array[Array[Double]],
  val custom: Json.Obj
) {
  val ts: Array[Double] = tsGen()
  def t(i: Int): Double = underlying.ts(i)
  val cxs: Array[Double] = cxsGen()
  def cx(i: Int) = if (cxs.length > 0) cxs(i) else Double.NaN
  val cys: Array[Double] = cysGen()
  def cy(i: Int) = if (cys.length > 0) cys(i) else Double.NaN
  lazy val xss: Array[Array[Double]] = xssGen()
  def xs(i: Int) = xss(i)
  def x(i: Int, j: Int) = xss(i)(j)
  lazy val yss: Array[Array[Double]] = yssGen()
  def ys(i: Int): yss(i)
  def y(i: Int, j: Int): yss(i)(j)

  def custom = underlying.custom
  def custom(c: Json.Obj) = new Data(id, tsGen, cxsGen, cysGen, xssGen, yssGen, c)

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
  class MemoizedGenerator[A](a: => A) extends Function0[A] {
    private[this] lazy val result = a
    def apply(): A = result
  }

  val emptyDoubleArray = new Array[Double](0)
  val emptyDoublesGen = () => emptyDoubleArray
  def empty = new Data(org.openworm.trackercommons.Data.empty)

  def from(id: String, ts: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, () => ts, emptyDoublesGen, emptyDoublesGen, () => xss, () => yss, Json.Obj.empty)

  def from(id: String, ts: Array[Double], cxs: Array[Double], cys: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, () => ts, () => cxs, () => cys, () => xss, () => yss, Json.Obj.empty)

  def from(underlying: org.openworm.trackercommons.Data): Data =
    new Data(
      if (underlying.nid.isNaN) underlying.sid else underlying.nid.toString,
      new MemoizedGenerator(java.lang.Arrays.copyOf(underlying.ts, underlying.ts.length)),
      new MemoizedGenerator(java.lang.Arrays.copyOf(underlying.cxs, underlying.cxs.length)),
      new MemoizedGenerator(java.lang.Arrays.copyOf(underlying.cys, underlying.cys.length)),
      new MemoizedGenerator(underlying.xss.map(i =>
        org.openworm.trackercommons.Data.doubly(
          xss(i),
          if (underlying.cxs.length > 0 && underlying.cxs(i).finite) underlying.cxs(i) else 0.0
        )
      )),
      new MemoizedGenerator(underlying.yss.map(i =>
        org.openworm.trackercommons.Data.doubly(
          yss(i),
          if (underlying.cys.length > 0 && underlying.cys(i).finite) underlying.cys(i) else 0.0
        )
      ))
    )
}
