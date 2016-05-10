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
import org.openworm.trackercommons.WconImplicits._

trait WrapsScalaWcon[A] { def underlying: A }

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
extends WrapsScalaWcon[original.Arena]
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
      if (d.finite) new Arena(underlying.copy(diameter = Left((d1, d))))
      else this
    case Left((d1, d2)) =>
      new Arena(underlying.copy(
        diameter = if (d.finite) Left((d1, d)) else Right(d1)
      ))
  }
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

class Software(val underlying: original.Software)
extends WrapsScalaWcon[original.Software] {
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

  def from(name: String, featureIDs: Array[String]) = new Software(original.Software(name, "", featureIDs.toSet, Json.Obj.empty))

  def from(name: String, version: String, featureIDs: Array[String]) =
    new Software(original.Software(name, version, featureIDs.toSet, Json.Obj.empty))
}

class Metadata(val underlying: original.Metadata)
extends WrapsScalaWcon[original.Metadata] {
  def labs: Array[Laboratory] = underlying.lab.map(l => new Laboratory(l)).toArray
  def labs(ls: Array[Laboratory]): Metadata = new Metadata(underlying.copy(lab = ls.view.map(_.underlying).toVector))
  def addLab(l: Laboratory) = new Metadata(underlying.copy(lab = underlying.lab :+ l.underlying))

  def whos: Array[String] = if (underlying.who.length > 0) underlying.who.toArray else Metadata.emptyStringArray
  def whos(ws: Array[String]) = new Metadata(underlying.copy(who = ws.toVector))
  def addWho(w: String) = new Metadata(underlying.copy(who = underlying.who :+ w))

  def timestamp = ???

  def temperature = underlying.temperature.getOrElse(Double.NaN)
  def temperature(t: Double) = new Metadata(underlying.copy(temperature = if (t.finite) Some(t) else None))

  def humidity = underlying.humidity.getOrElse(Double.NaN)
  def humidity(h: Double) = new Metadata(underlying.copy(humidity = if (h.finite) Some(h) else None))

  def arena = underlying.arena.map(a => new Arena(a)).getOrElse(Arena.empty)
  def arena(a: Arena) = new Metadata(underlying.copy(arena = if (a.underlying.isEmpty) None else Some(a.underlying)))

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
  def protocols(ps: Array[String]) = new Metadata(underlying.copy(protocol = ps.toVector))
  def addProtocol(p: String) = new Metadata(underlying.copy(protocol = underlying.protocol :+ p))

  def softwares = if (underlying.software.length > 0) underlying.software.toArray.map(s => new Software(s)) else Metadata.emptySoftwareArray
  def softwares(ss: Array[Software]) = new Metadata(underlying.copy(software = ss.view.map(_.underlying).toVector))
  def addSoftware(s: Software) = new Metadata(underlying.copy(software = underlying.software :+ s.underlying))

  def settings = underlying.settings.getOrElse(Json.Null)
  def settings(s: Json) = new Metadata(underlying.copy(settings = Option(s)))

  def custom = underlying.custom
  def custom(c: Json.Obj) = new Metadata(underlying.copy(custom = c))
}
object Metadata {
  val empty = new Metadata(org.openworm.trackercommons.Metadata.empty)
  val emptyStringArray = new Array[String](0)
  val emptySoftwareArray = new Array[Software](0)

  def from(underlying: original.Metadata) = new Metadata(underlying)
}

class Data(
  val id: String,
  tsGen: () => Array[Double],
  cxsGen: () => Array[Double],
  cysGen: () => Array[Double],
  xssGen: () => Array[Array[Double]],
  yssGen: () => Array[Array[Double]],
  myCustom: Json.Obj
) {
  val ts: Array[Double] = tsGen()
  def t(i: Int): Double = ts(i)

  val cxs: Array[Double] = cxsGen()
  def cx(i: Int) = if (cxs.length > 0) cxs(i) else Double.NaN

  val cys: Array[Double] = cysGen()
  def cy(i: Int) = if (cys.length > 0) cys(i) else Double.NaN

  lazy val xss: Array[Array[Double]] = xssGen()
  def xs(i: Int) = xss(i)
  def x(i: Int, j: Int) = xss(i)(j)

  lazy val yss: Array[Array[Double]] = yssGen()
  def ys(i: Int) = yss(i)
  def y(i: Int, j: Int) = yss(i)(j)

  def custom: Json.Obj = myCustom
  def custom(c: Json.Obj): Data = new Data(id, tsGen, cxsGen, cysGen, xssGen, yssGen, c)

  def toUnderlying = {
    val nid = Jast.parse(id).double
    val sid = if (nid.isNaN) id else ""
    val oxs = if (cxs.length == 0) org.openworm.trackercommons.Data.findFloatOffsets(xss) else Data.emptyDoubleArray
    val oys = if (cys.length == 0) org.openworm.trackercommons.Data.findFloatOffsets(yss) else Data.emptyDoubleArray
    new org.openworm.trackercommons.Data(
      nid, sid,
      java.util.Arrays.copyOf(ts, ts.length),
      xss.indices.toArray.map(i =>
        original.Data.singly(
          xss(i),
          if (cxs.length > 0) cxs(i)
          else if (oxs.length > 1) oxs(i)
          else if (oxs.length == 1) oxs(0)
          else 0.0
        )
      ),
      yss.indices.toArray.map(i => 
        original.Data.singly(
          yss(i),
          if (cys.length > 0) cys(i)
          else if (oys.length > 1) oys(i)
          else if (oys.length == 1) oys(0)
          else 0.0
        )
      ),
      java.util.Arrays.copyOf(cxs, cxs.length),
      java.util.Arrays.copyOf(cys, cys.length),
      oxs,
      oys,
      custom
    )
  }
}
object Data {
  class MemoizedGenerator[A](a: => A) extends Function0[A] {
    private[this] lazy val result = a
    def apply(): A = result
  }

  val emptyDoubleArray = new Array[Double](0)
  val emptyDoublesGen = () => emptyDoubleArray
  val emptyDoubleArrayArray = new Array[Array[Double]](0)
  val emptyDoublessGen = () => emptyDoubleArrayArray
  def empty = new Data("", emptyDoublesGen, emptyDoublesGen, emptyDoublesGen, emptyDoublessGen, emptyDoublessGen, Json.Obj.empty)

  def from(id: String, ts: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, () => ts, emptyDoublesGen, emptyDoublesGen, () => xss, () => yss, Json.Obj.empty)

  def from(id: String, ts: Array[Double], cxs: Array[Double], cys: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, () => ts, () => cxs, () => cys, () => xss, () => yss, Json.Obj.empty)

  def from(underlying: org.openworm.trackercommons.Data): Data =
    new Data(
      if (underlying.nid.isNaN) underlying.sid else underlying.nid.toString,
      new MemoizedGenerator(java.util.Arrays.copyOf(underlying.ts, underlying.ts.length)),
      new MemoizedGenerator(java.util.Arrays.copyOf(underlying.cxs, underlying.cxs.length)),
      new MemoizedGenerator(java.util.Arrays.copyOf(underlying.cys, underlying.cys.length)),
      new MemoizedGenerator(underlying.xs.indices.toArray.map(i =>
        org.openworm.trackercommons.Data.doubly(
          underlying.xs(i),
          if (underlying.cxs.length > 0 && underlying.cxs(i).finite) underlying.cxs(i) else 0.0
        )
      )),
      new MemoizedGenerator(underlying.ys.indices.toArray.map(i =>
        org.openworm.trackercommons.Data.doubly(
          underlying.ys(i),
          if (underlying.cys.length > 0 && underlying.cys(i).finite) underlying.cys(i) else 0.0
        )
      )),
      underlying.custom
    )
}

class Wcon(
  myMeta: Metadata,
  myDatas: Array[Data],
  val units: original.UnitMap,
  myPreviousFiles: Array[String],
  myNextFiles: Array[String],
  myOwnFile: String,
  myCustom: Json.Obj
) {
  def meta: Metadata = myMeta
  def meta(m: Metadata): Wcon = new Wcon(m, myDatas, units, myPreviousFiles, myNextFiles, myOwnFile, myCustom)

  def datas: Array[Data] = myDatas
  def datas(ds: Array[Data]): Wcon = new Wcon(myMeta, ds, units, myPreviousFiles, myNextFiles, myOwnFile, myCustom)
  def addData(d: Data): Wcon = new Wcon(myMeta, myDatas :+ d, units, myPreviousFiles, myNextFiles, myOwnFile, myCustom)

  def previousFiles = myPreviousFiles
  def previousFiles(pf: Array[String]): Wcon = new Wcon(myMeta, myDatas, units, pf, myNextFiles, myOwnFile, myCustom)
  def addPreviousFile(f: String): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles :+ f, myNextFiles, myOwnFile, myCustom)

  def nextFiles = myNextFiles
  def nextFiles(nf: Array[String]): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles, nf, myOwnFile, myCustom)
  def addNextFile(f: String): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles, myNextFiles :+ f, myOwnFile, myCustom)

  def myFile: String = myOwnFile
  def myFile(f: String): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles, myNextFiles, f, myCustom)

  def custom = myCustom
  def custom(c: Json.Obj) = new Wcon(myMeta, myDatas, units, myPreviousFiles, myNextFiles, myOwnFile, c)
}
object Wcon {
  val emptyDataArray = new Array[Data](0)
  val emptyStringArray = new Array[String](0)

  def empty = new Wcon(Metadata.empty, emptyDataArray, original.UnitMap.default, emptyStringArray, emptyStringArray, "", Json.Obj.empty)

  def from(underlying: original.DataSet) = {
    val u = underlying // Just too long of a name!
    new Wcon(
      Metadata from u.meta,
      u.data.map{ case Left(dm) => Data from dm.toData; case Right(da) => Data from da },
      u.unitmap,
      if (u.files.names.length > 0) u.files.names.take(u.files.index-1).reverse.toArray else emptyStringArray,
      if (u.files.names.length > 0) u.files.names.drop(u.files.index).toArray else emptyStringArray,
      u.files.me,
      u.custom
    )
  }
}