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

import java.time._

import kse.jsonal._

import org.openworm.{trackercommons => original}
import org.openworm.trackercommons.WconImplicits._

/** Trait for compatibility layer that wraps a Scala class with more advanced language features */
trait WrapsScalaWcon[A] { 
  /** The underlying Scala class */
  def underlying: A
}

/** Represents identifying information for a laboratory */
class Laboratory(val underlying: original.Laboratory)
extends WrapsScalaWcon[original.Laboratory] {
  /** The name of the Principal Investigator */
  def pi = underlying.pi
  /** Sets the name of the Principal Investigator (returns a new copy) */
  def pi(p: String) = new Laboratory(underlying.copy(pi = p))

  /** The name of the laboratory itself */
  def name = underlying.name
  /** Sets the name of the laboratory (returns a new copy) */
  def name(n: String) = new Laboratory(underlying.copy(name = n))

  /** The location of the laboratory */
  def location = underlying.location
  /** Sets the location of the laboratory */
  def location(l: String) = new Laboratory(underlying.copy(location = l))

  /** Custom laboratory information (as a `Json.Obj`) */
  def custom = underlying.custom
  /** Sets custom information for the laboratory (returns a new copy) */
  def custom(c: Json.Obj) = new Laboratory(underlying.copy(custom = c))
}
object Laboratory {
  /** A laboratory object with no identifying information */
  val empty = new Laboratory(original.Laboratory.empty)

  /** Wraps the default Scala `Laboratory` for compatibility */
  def from(underlying: original.Laboratory): Laboratory = new Laboratory(underlying)

  /** Creates a new `Laboratory` from PI, lab name, and location information.
    *
    * Leave strings empty, `""` if there is no information for that field.  `null` fields are not supported.
    */
  def from(pi: String, name: String, location: String) =
    new Laboratory(original.Laboratory(pi, name, location, Json.Obj.empty))
}

/** Represents information for the environment of the animal during tracking */
class Arena(val underlying: original.Arena)
extends WrapsScalaWcon[original.Arena]
{
  /** The type of media, e.g. NGM agar, agarose, etc..
    *
    * Non-circular/elliptical shapes should be noted here also.
    */
  def kind = underlying.kind
  /** Sets the type of the media (returns a new copy) */
  def kind(k: String) = new Arena(underlying.copy(kind = k))

  /** The long axis of the arena (diameter if circular).*/
  def majorDiameter = underlying.diameter match { case Right(d) => d;          case Left((d1, d2)) => d1 }
  /** Sets the long axis of the arena (returns a new copy) */
  def majorDiameter(d: Double) = new Arena(underlying.diameter match {
    case Right(_) => underlying.copy(diameter = Right(d))
    case Left((_, d2)) => underlying.copy(diameter = Left((d, d2)))
  })

  /** The short axis of the arena; may be `NaN` if the arena is circular or square. */
  def minorDiameter = underlying.diameter match { case Right(_) => Double.NaN; case Left((_, d2))  => d2 }
  /** Sets the short axis of the arena (returns a new copy) */
  def minorDiameter(d: Double) = underlying.diameter match {
    case Right(d1) => 
      if (d.finite) new Arena(underlying.copy(diameter = Left((d1, d))))
      else this
    case Left((d1, d2)) =>
      new Arena(underlying.copy(
        diameter = if (d.finite) Left((d1, d)) else Right(d1)
      ))
  }

  /** The orientation of the arena relative to the camera.
    *
    * `"toward"` means the animals are on the same side as the camera.  `"away"` means the
    * camera looks through the media to see the animal.  If neither is appropriate, describe
    * briefly.  For example, an animal between two cover slips might be `"symmetric"` (but
    * it's less likely that an automatic parser will understand what is going on).
    */
  def orient = underlying.orient
  /** Specifies the orientation of the arena relative to the camera (returns a new copy) */
  def orient(o: String) = new Arena(underlying.copy(orient = o))


  /** Custom arena information (as a `Json.Obj`) */
  def custom = underlying.custom
  /** Sets custom information for the arena (returns a new copy) */
  def custom(c: Json.Obj) = new Arena(underlying.copy(custom = c))
}
object Arena {
  /** An `Arena` object that does not specify anything about the arena */
  val empty = new Arena(original.Arena.empty)

  /** Wraps a standard Scala `Arena` in this compatibility object */
  def from(underlying: original.Arena) = new Arena(underlying)

  /** Creates a new arena given a known type and diameter */
  def from(kind: String, diameter: Double): Arena =
    new Arena(original.Arena(kind, Right(diameter), "", Json.Obj.empty))

  /** Creates a new arena given a known type and major and minor axes */
  def from(kind: String, dmajor: Double, dminor: Double) =
    new Arena(original.Arena(kind, Left((dmajor, dminor)), "", Json.Obj.empty))

  /** Creates a new arena given a known type, diameter, and orientation */
  def from(kind: String, diameter: Double, orient: String) =
    new Arena(original.Arena(kind, Right(diameter), orient, Json.Obj.empty))

  /** Creates a new arena given a known type, major and minor axes, and orientation */
  def from(kind: String, dmajor: Double, dminor: Double, orient: String) =
    new Arena(original.Arena(kind, Left((dmajor, dminor)), orient, Json.Obj.empty))
}

/** Represents information about the software that gathered and/or processed the data */
class Software(val underlying: original.Software)
extends WrapsScalaWcon[original.Software] {
  /** The name of the software */
  def name = underlying.name
  /** Sets the name of the software (returns a new copy) */
  def name(n: String) = new Software(underlying.copy(name = n))

  /** The version number of the software (as a `String`) */
  def version = underlying.version
  /** Sets the version number of the software (returns a new copy) */
  def version(v: String) = new Software(underlying.copy(version = v))

  /** An array of custom tag names that this software knows how to create/process */
  def featureID = underlying.featureID.toArray.sorted
  /** Sets the custom tag names that the named software knows how to create/process (returns a new copy) */
  def featureID(fs: Array[String]) = new Software(underlying.copy(featureID = fs.toSet))
  /** Specifies an additional custom tag name that the named software knows how to create/process (returns a new copy) */
  def addFeatureID(s: String) = new Software(underlying.copy(featureID = underlying.featureID + s))

  /** Custom software information (as a `Json.Obj`) */
  def custom = underlying.custom
  /** Sets custom information for the software (returns a new copy) */
  def custom(c: Json.Obj) = new Software(underlying.copy(custom = c))
}
object Software {
  /** A software object that contains no information */
  val empty = new Software(original.Software.empty)

  /** Wraps a standard Scala `Software` in this compatibility object */
  def from(underlying: original.Software) = new Software(underlying)

  /** Specifies software by name only */
  def from(name: String): Software = new Software(original.Software(name, "", Set(), Json.Obj.empty))

  /** Specifies software by name and version */
  def from(name: String, version: String): Software = new Software(original.Software(name, version, Set(), Json.Obj.empty))

  /** Specifies software by name and tags handled */
  def from(name: String, featureIDs: Array[String]) = new Software(original.Software(name, "", featureIDs.toSet, Json.Obj.empty))

  /** Specifies software by name, version, and tags handled */
  def from(name: String, version: String, featureIDs: Array[String]) =
    new Software(original.Software(name, version, featureIDs.toSet, Json.Obj.empty))
}

/** Represents information associated with an experiment. */
class Metadata(val underlying: original.Metadata)
extends WrapsScalaWcon[original.Metadata] {
  /** An array of the labs involved. */
  def labs: Array[Laboratory] = underlying.lab.map(l => new Laboratory(l)).toArray
  /** Sets the labs involved (returns a new copy) */
  def labs(ls: Array[Laboratory]): Metadata = new Metadata(underlying.copy(lab = ls.view.map(_.underlying).toVector))
  /** Adds an additional lab (returns a new copy) */
  def addLab(l: Laboratory) = new Metadata(underlying.copy(lab = underlying.lab :+ l.underlying))

  /** An array of the individuals involved (one `String` per individual, presumably their name) */
  def whos: Array[String] = if (underlying.who.length > 0) underlying.who.toArray else Metadata.emptyStringArray
  /** Sets the individuals involved (returns a new copy) */
  def whos(ws: Array[String]) = new Metadata(underlying.copy(who = ws.toVector))
  /** Adds an individual (returns a new copy) */
  def addWho(w: String) = new Metadata(underlying.copy(who = underlying.who :+ w))

  /** A timestamp associated with this experiment, typically but not necessarily without time zone information.
    *
    * WARNING - This method will return `null` if no timestamp is associated with the experiment!
    */
  def timestamp: OffsetDateTime = underlying.timestamp match {
    case None => null
    case Some(Left(odt)) => odt
    case Some(Right(ldt)) => OffsetDateTime.of(ldt, ZoneId.systemDefault.getRules.getOffset(ldt))
  }
  /** Sets a timestamp associated with this experiment with no time zone specified (returns a new copy) */
  def timestamp(stamp: LocalDateTime): Metadata = new Metadata(underlying.copy(timestamp = Some(Right(stamp))))
  /** Associates a timestamp including a time zone with this experiment (returns a new copy) */
  def timestamp(stamp: OffsetDateTime): Metadata = new Metadata(underlying.copy(timestamp = Some(Left(stamp))))
  
  /** The temperature in Celsius at which the experiment was performed, or `NaN` if not specified */
  def temperature = underlying.temperature.getOrElse(Double.NaN)
  /** Sets the information about the temperature; use `NaN` if it was not known (returns a new copy) */
  def temperature(t: Double) = new Metadata(underlying.copy(temperature = if (t.finite) Some(t) else None))

  /** The relative humidity (0-1) at which the experiment was performed, or `NaN` if not specified */
  def humidity = underlying.humidity.getOrElse(Double.NaN)
  /** Sets information about the relative humidity.  Divide by 100 if you have a percentage; use `NaN` if not known.  (Returns a new copy.) */
  def humidity(h: Double) = new Metadata(underlying.copy(humidity = if (h.finite) Some(h) else None))

  /** Information about the arena in which the experiment took place. */
  def arena = underlying.arena.map(a => new Arena(a)).getOrElse(Arena.empty)
  /** Sets information about the arena in which the experiment took place (returns a new copy) */
  def arena(a: Arena) = new Metadata(underlying.copy(arena = if (a.underlying.isEmpty) None else Some(a.underlying)))

  /** Information about the food present during the experiment */
  def food = underlying.food.getOrElse("")
  /** Sets a description of the food present during the experiment (returns a new copy) */
  def food(f: String) = new Metadata(underlying.copy(food = if (f.isEmpty) None else Some(f)))

  /** Information about the media on which the experiment took place */
  def media = underlying.media.getOrElse("")
  /** Sets a description of the media on which the experiment took place (returns a new copy) */
  def media(m: String) = new Metadata(underlying.copy(media = if (m.isEmpty) None else Some(m)))

  /** Information about the sex of the animals.  If left blank, hermaphrodite should be assumed. */
  def sex = underlying.sex.getOrElse("")
  /** Sets a description of the sex of the animals (returns a new copy) */
  def sex(s: String) = new Metadata(underlying.copy(sex = if (s.isEmpty) None else Some(s)))

  /** Information about the stage of life of the animals.  Use standard nomenclature, "L4", "dauer", "young adult".
    *
    * Note: for lifespan studies, it is customary to specify age here as "adult day 0", "adult day 25", etc..
    */
  def stage = underlying.stage.getOrElse("")
  /** Sets a description of the stage of the animals during the experiment (returns a new copy) */
  def stage(s: String) = new Metadata(underlying.copy(stage = if (s.isEmpty) None else Some(s)))

  /** Information about the age of the animals (in seconds!), or `NaN` if it is not known.
    *
    * Note: this is total age, not days of adulthood, by default, and the units are seconds.
    */
  def age = underlying.age.getOrElse(Double.NaN)
  /** Sets the (expected average) age of the animals during the experiment (returns a new copy) */
  def age(a: Double) = new Metadata(underlying.copy(age = if (a.finite) Some(a) else None))

  /** Information about the strain.  Please use standard nomenclature. */
  def strain = underlying.strain.getOrElse("")
  /** Sets information about the strain (returns a new copy) */
  def strain(s: String) = new Metadata(underlying.copy(strain = if (s.isEmpty) None else Some(s)))

  /** The protocol for the experiment as an Array of Strings. */
  def protocols = if (underlying.protocol.length > 0) underlying.protocol.toArray else Metadata.emptyStringArray
  /** Sets a description of the protocol for the experiment from an Array of Strings (returns a new copy) */
  def protocols(ps: Array[String]) = new Metadata(underlying.copy(protocol = ps.toVector))
  /** Adds a single line to the experiment's pprotocol (returns a new copy) */
  def addProtocol(p: String) = new Metadata(underlying.copy(protocol = underlying.protocol :+ p))

  /** An array containing the different software packages used to process this data.
    *
    * Note: it is customary to list them in the order applied (capturing software first, etc.)
    */
  def softwares = if (underlying.software.length > 0) underlying.software.toArray.map(s => new Software(s)) else Metadata.emptySoftwareArray
  /** Specifies the different software packages used (returns a new copy) */
  def softwares(ss: Array[Software]) = new Metadata(underlying.copy(software = ss.view.map(_.underlying).toVector))
  /** Specifies an additional software package used by adding it to the end of the list (returns a new copy) */
  def addSoftware(s: Software) = new Metadata(underlying.copy(software = underlying.software :+ s.underlying))

  /** Arbitrary JSON containing settings used by the software that produced or analyzed this data.
    *
    * Note: If multiple packages were used, it is customary to place one set of settings per package in a JSON array.
    */
  def settings = underlying.settings.getOrElse(Json.Null)
  /** Sets arbitrary JSON containing settings used by the software that produced or analyzed this data (returns a new copy). */
  def settings(s: Json) = new Metadata(underlying.copy(settings = Option(s)))

  /** Custom metadata information (as a `Json.Obj`) */
  def custom = underlying.custom
  /** Sets the custom metadata information (returns a new copy) */
  def custom(c: Json.Obj) = new Metadata(underlying.copy(custom = c))
}
object Metadata {
  /** A metadata object that specifies nothing about the experiment */
  val empty = new Metadata(org.openworm.trackercommons.Metadata.empty)

  /** An empty array of strings (used to set missing metadata) */
  val emptyStringArray = new Array[String](0)

  /** An empty array of `Software` (used to set missing metadata) */
  val emptySoftwareArray = new Array[Software](0)

  /** A `Metadata` object that wraps the standard Scala version. */
  def from(underlying: original.Metadata) = new Metadata(underlying)
}

/** A representation of the position and posture of a worm over time.
  *
  * The data in this class is typically a copy of an original, so may be modified freely.
  *
  * The class is set up to load the data lazily.  It's preferable if the generators also
  * cache the data so that you can alter the custom data without having to regenerate the
  * data multiple times.
  */
class Data(
  val id: String,
  tsGen: () => Array[Double],
  cxsGen: () => Array[Double],
  cysGen: () => Array[Double],
  xssGen: () => Array[Array[Double]],
  yssGen: () => Array[Array[Double]],
  myCustom: Json.Obj
) {
  /** The array of times at which this animal was measured. */
  lazy val ts: Array[Double] = tsGen()
  /** The `i`th timepoint recorded for this animal. */
  def t(i: Int): Double = ts(i)

  /** The array of x-positions for the centroid of the animal; if not known, the value will be NaN */
  lazy val cxs: Array[Double] = cxsGen()
  /** A single x-coordinate of a centroid, NaN if not known. */
  def cx(i: Int) = if (cxs.length > 0) cxs(i) else Double.NaN

  /** The array of y-positions for the centroid of the animal; if not known, the value will be NaN */
  lazy val cys: Array[Double] = cysGen()
  /** A single y-coordinate of a centroid, NaN if not known. */
  def cy(i: Int) = if (cys.length > 0) cys(i) else Double.NaN

  /** The x spine points for the animal at each time point.  If not known, the array should be length 0.
    *
    * Note: these points are in global coordinates.
    */
  lazy val xss: Array[Array[Double]] = xssGen()
  /** The x spine points for the animal at a particular time point (in global coordinates). */
  def xs(i: Int) = xss(i)
  /** The x coordinate of the `j`th spine point of the `i` time point. */
  def x(i: Int, j: Int) = xss(i)(j)

  /** The y spine points for the animal at each time point.  If not known, the array should be length 0.
    *
    * Note: these points are in global coordinates.
    */
  lazy val yss: Array[Array[Double]] = yssGen()
  /** The y spine points for the animal at a particular time point (in global coordinates). */
  def ys(i: Int) = yss(i)
  /** The y coordinate of the `j`th spine point of the `i` time point. */
  def y(i: Int, j: Int) = yss(i)(j)

  /** Custom data (as a `Json.Obj`) */
  def custom: Json.Obj = myCustom
  /** Sets custom data (returns a new copy) */
  def custom(c: Json.Obj): Data = new Data(id, tsGen, cxsGen, cysGen, xssGen, yssGen, c)

  /** Converts this data record to the standard Scala format. */
  def toUnderlying = {
    val rxs = 
      if (cxs.length == 0) xss.map(_.min.toDouble)
      else java.util.Arrays.copyOf(cxs, cxs.length)
    val rys = 
      if (cys.length == 0) yss.map(_.min.toDouble)
      else java.util.Arrays.copyOf(cys, cys.length)
    new org.openworm.trackercommons.Data(
      id,
      java.util.Arrays.copyOf(ts, ts.length),
      xss.indices.toArray.map(i => original.Data.singly(xss(i), -rxs(i))),
      yss.indices.toArray.map(i => original.Data.singly(yss(i), -rys(i))),
      java.util.Arrays.copyOf(cxs, cxs.length),
      java.util.Arrays.copyOf(cys, cys.length),
      Data.emptyDoubleArray,
      Data.emptyDoubleArray,
      None,
      None,
      custom
    )(rxs,rys, false)
  }
}
object Data {
  /** A data generator that will cache the result of the first generation operation */
  class MemoizedGenerator[A](a: => A) extends Function0[A] {
    private[this] lazy val result = a
    def apply(): A = result
  }

  /** An empty array of Doubles (used for missing data) */
  val emptyDoubleArray = new Array[Double](0)
  /** A generator that produces an empty array of Doubles */
  val emptyDoublesGen = () => emptyDoubleArray
  /** An empty array of arrays of Doubles (used for missing data sets) */
  val emptyDoubleArrayArray = new Array[Array[Double]](0)
  /** A generator that produces an empty array of arrays of Doubles */
  val emptyDoublessGen = () => emptyDoubleArrayArray

  /** A data set containing no data. */
  def empty = new Data("", emptyDoublesGen, emptyDoublesGen, emptyDoublesGen, emptyDoublessGen, emptyDoublessGen, Json.Obj.empty)

  /** Creates data given an ID, series of time points, and sets of spine points */
  def from(id: String, ts: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, () => ts, emptyDoublesGen, emptyDoublesGen, () => xss, () => yss, Json.Obj.empty)

  /** Creates data given an ID, series of time points, centroid points, and spine points */
  def from(id: String, ts: Array[Double], cxs: Array[Double], cys: Array[Double], xss: Array[Array[Double]], yss: Array[Array[Double]]): Data =
    new Data(id, () => ts, () => cxs, () => cys, () => xss, () => yss, Json.Obj.empty)

  /** Generates data by translating from the standard Scala data format */
  def from(underlying: org.openworm.trackercommons.Data): Data =
    new Data(
      underlying.id,
      new MemoizedGenerator(java.util.Arrays.copyOf(underlying.ts, underlying.ts.length)),
      new MemoizedGenerator(java.util.Arrays.copyOf(underlying.cxs, underlying.cxs.length)),
      new MemoizedGenerator(java.util.Arrays.copyOf(underlying.cys, underlying.cys.length)),
      new MemoizedGenerator(underlying.xDatas.indices.toArray.map(i =>
        org.openworm.trackercommons.Data.doubly(
          underlying.xDatas(i), underlying.rxs(i)
        )
      )),
      new MemoizedGenerator(underlying.yDatas.indices.toArray.map(i =>
        org.openworm.trackercommons.Data.doubly(
          underlying.yDatas(i),
          underlying.rys(i)
        )
      )),
      underlying.custom
    )
}

/** Represents the data from a tracking experiment */
class Wcon(
  myMeta: Metadata,
  myDatas: Array[Data],
  val units: original.UnitMap,
  myPreviousFiles: Array[String],
  myNextFiles: Array[String],
  myOwnFile: String,
  myFileCustom: Json.Obj,
  myCustom: Json.Obj
) {
  /** The metadata associated with the experiment */
  def meta: Metadata = myMeta
  /** Sets the metadata for this experiment (returns a new copy) */
  def meta(m: Metadata): Wcon = new Wcon(m, myDatas, units, myPreviousFiles, myNextFiles, myOwnFile, myFileCustom, myCustom)

  /** The array of data associated with the experiment */
  def datas: Array[Data] = myDatas
  /** Sets the array of data associated with the experiment (returns a new copy) */
  def datas(ds: Array[Data]): Wcon = new Wcon(myMeta, ds, units, myPreviousFiles, myNextFiles, myOwnFile, myFileCustom, myCustom)
  /** Adds data for a single tracked object to the experiment (returns a new copy) */
  def addData(d: Data): Wcon = new Wcon(myMeta, myDatas :+ d, units, myPreviousFiles, myNextFiles, myOwnFile, myFileCustom, myCustom)

  /** The names of files in the same experiment gathered before this one (most recent first) */
  def previousFiles = myPreviousFiles
  /** Set the names of files in the same experiment that were before this one (returns a new copy) */
  def previousFiles(pf: Array[String]): Wcon = new Wcon(myMeta, myDatas, units, pf, myNextFiles, myOwnFile, myFileCustom, myCustom)
  /** Adds a new oldest previous file to the list (returns a new copy) */
  def addPreviousFile(f: String): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles :+ f, myNextFiles, myOwnFile, myFileCustom, myCustom)

  /** The names of files in the same experiment gathered after this one (next one first) */
  def nextFiles = myNextFiles
  /** Set the names of files in the same experiment that were after this one (returns a new copy) */
  def nextFiles(nf: Array[String]): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles, nf, myOwnFile, myFileCustom, myCustom)
  /** Adds a new next file after all the others (returns a new copy) */
  def addNextFile(f: String): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles, myNextFiles :+ f, myOwnFile, myFileCustom, myCustom)

  /** The name of this file (or the portion used to discriminate this from other files in the experiment) */
  def myFile: String = myOwnFile
  /** Sets the name of this file */
  def myFile(f: String): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles, myNextFiles, f, myFileCustom, myCustom)

  /** Custom information about the files in this experiment (as a Json.Obj) */
  def fileCustom = myFileCustom
  /** Sets custom information about files in this experiment (returns a new copy) */
  def fileCustom(fc: Json.Obj): Wcon = new Wcon(myMeta, myDatas, units, myPreviousFiles, myNextFiles, myOwnFile, fc, myCustom)

  /** Custom information about this experiment (in a Json.Obj) */
  def custom = myCustom
  /** Sets the custom information about this experiment (returns a new copy) */
  def custom(c: Json.Obj) = new Wcon(myMeta, myDatas, units, myPreviousFiles, myNextFiles, myOwnFile, myFileCustom, c)

  /** Converts to standard Scala form for data */
  def toUnderlying: original.DataSet = new original.DataSet(
    meta.underlying,
    units,
    myDatas.map(_.toUnderlying),
    original.FileSet(
      previousFiles.reverse.toVector ++ (if (myFile.isEmpty) Vector() else Vector(myFile)) ++ nextFiles.toVector,
      previousFiles.length,
      myFileCustom
    ),
    myCustom
  )
}
object Wcon {
  /** An empty array of data */
  val emptyDataArray = new Array[Data](0)
  /** An empty array of strings */
  val emptyStringArray = new Array[String](0)

  /** An empty experiment (no data, no metadata, no files, just a map for units) */
  def empty = new Wcon(Metadata.empty, emptyDataArray, original.UnitMap.default, emptyStringArray, emptyStringArray, "", Json.Obj.empty, Json.Obj.empty)

  /** Creates a view of this data set from a standard Scala data set.
    *
    * Note: as the data is largely replicated, this can double the required memory.
    */
  def from(underlying: original.DataSet) = {
    val u = underlying // Just too long of a name!
    new Wcon(
      Metadata from u.meta,
      u.data.map{ da => Data from da },
      u.unitmap,
      if (u.files.names.length > 0) u.files.names.take(u.files.index).reverse.toArray else emptyStringArray,
      if (u.files.names.length > 0) u.files.names.drop(u.files.index+1).toArray else emptyStringArray,
      u.files.me,
      u.files.custom,
      u.custom
    )
  }
}
