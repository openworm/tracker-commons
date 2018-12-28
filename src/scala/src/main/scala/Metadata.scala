package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

import WconImplicits._

trait MightBeEmpty[A] { self: A =>
  def isEmpty: Boolean
  def nonEmptyOption: Option[A] = if (isEmpty) None else Some(this)
}

case class Laboratory(pi: String, name: String, location: String, contact: Vector[String], custom: Json.Obj)
extends AsJson with MightBeEmpty[Laboratory] with Customizable[Laboratory] {
  def isEmpty = pi.isEmpty && name.isEmpty && location.isEmpty && contact.isEmpty && custom.size == 0
  def customFn(f: Json.Obj => Json.Obj) = new Laboratory(pi, name, location, contact, f(custom))
  def json = Json ~? ("PI", pi) ~? ("name", name) ~? ("location", location) ~? ("contact", contact) ~~ custom ~ Json
}
object Laboratory extends FromJson[Laboratory] {
  private val listKeyNames = List("PI", "name", "location")
  private val someKeyNames = Option(("contact" :: listKeyNames).toSet)
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid laboratory: " + msg))
  def empty = new Laboratory("", "", "", Vector.empty, Json.Obj.empty)
  def parse(j: Json): Either[JastError, Laboratory] = {
    // If we didn't need detailed error messages this would be a one-liner!
    // j("PI").stringOr("") etc. would do the trick.
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    val count = o.countKeys(someKeyNames)
    count.foreach{ case (key, n) => if (n > 1) return BAD("multiple entries for "+key) }
    if (count.isEmpty) return BAD("None of "+listKeyNames.mkString(", ") + " found")
    val List(pi, name, location) = listKeyNames.map(key => o get_or_java_null key match {
      case null => ""
      case Json.Str(text) => text
      case _ => return BAD(key + " is not text") 
    })
    val contact = o get_or_java_null "contact" match {
      case null => Vector.empty[String]
      case Json.Str(text) => Vector(text)
      case jj   => jj.to[Vector[String]] match {
        case Right(vs) => vs
        case Left(je) => return BAD("Invalid contact: " + je.toString)
      }
    }
    Right(new Laboratory(pi, name, location, contact, Custom(o)))
  }
}

case class Arena(style: String, size: Option[Either[(Double, Double), Double]], orientation: String, custom: Json.Obj)
extends AsJson with MightBeEmpty[Arena] with Customizable[Arena] {
  def isEmpty =
    style.isEmpty &&
    (size match { 
      case None => true
      case Some(Right(x)) => !x.finite
      case Some(Left((x,y))) => !x.finite && !y.finite
    }) &&
    orientation.isEmpty &&
    custom.size == 0

  def customFn(f: Json.Obj => Json.Obj) = new Arena(style, size, orientation, f(custom))

  import Arena.jsonizeDoublePair
  def json = (Json
    ~? ("style", style)
    ~? ("size", size.map(Json either _))
    ~? ("orientation", orientation)
    ~~ custom ~ Json)
}
object Arena extends FromJson[Arena] {
  private[trackercommons] implicit val jsonizeDoublePair: Jsonize[(Double, Double)] = new Jsonize[(Double, Double)] {
    def jsonize(dd: (Double, Double)): Json = Json.Arr.Dbl(Array(dd._1, dd._2))
  }
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid arena: " + msg))
  def empty = new Arena("", None, "", Json.Obj.empty)
  def parse(j: Json): Either[JastError, Arena] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    var style, orientation: String = null
    var size: Either[(Double, Double), Double] = null
    o.foreach{ case (k,v) =>
      if (k == "style") {
        if (style ne null) return BAD("multiple type entries")
        style = v stringOr { return BAD("type is not expressed as text") }
      }
      else if (k == "size") {
        if (size ne null) return BAD("multiple size entries (two entries should be in an array)")
        size = v.to[Either[Array[Double], Double]] match {
          case Right(Right(x)) => Right(x)
          case Right(Left(xs)) => if (xs.length == 1) Right(xs(0))
            else if (xs.length == 2) Left((xs(0), xs(1)))
            else return BAD("wrong number of size entries: " + xs.length)
          case _ => return BAD("size is neither a number nor an array of two numbers")
        }
      }
      else if (k == "orientation") {
        if (orientation ne null) return BAD("multiple orientation entries")
        orientation = v stringOr { return BAD("orientation is not expressed as text") }
      }
    }
    if ((style eq null) && (orientation eq null) && (size eq null)) return BAD("No aspects of Arena are specified (style, size, orientation)")
    Right(new Arena(if (style eq null) "" else style, Option(size), if (orientation eq null) "" else orientation, Custom(o)))
  }
}

case class Interpolate(method: String, values: Vector[String], custom: Json.Obj)
extends AsJson with MightBeEmpty[Interpolate] with Customizable[Interpolate] {
  def isEmpty = method.isEmpty && values.isEmpty
  def customFn(f: Json.Obj => Json.Obj) = new Interpolate(method, values, f(custom))
  def json = Json ~? ("method", method) ~? ("values", values) ~~ custom ~ Json
}
object Interpolate extends FromJson[Interpolate] {
  val empty = new Interpolate("", Vector.empty, Json.Obj.empty)
  def parse(j: Json): Either[JastError, Interpolate] = j match {
    case o: Json.Obj =>
      val method = o get_or_java_null "method" match {
        case s: Json.Str => s.text
        case null => ""
        case _ => return Left(JastError("Interpolate 'method' was not a string"))
      }
      val values =
        if (o.contains("values")) o("values").to[Vector[String]] match {
          case Right(v) => v
          case Left(je) => return Left(JastError("Interpolate 'values' were not all strings: ", because = je))
        }
        else Vector.empty[String]
      Right(new Interpolate(method, values, o.filter((k, _) => k.startsWith("@"))))
    case _ => Left(JastError("Invalid interpolate record: not a JSON object"))
  }
}

case class Software(name: String, version: String, featureID: Set[String], settings: Option[Json], custom: Json.Obj)
extends AsJson with MightBeEmpty[Software] with Customizable[Software] {
  def isEmpty = name.isEmpty && version.isEmpty && featureID.size == 0 && custom.size == 0 && settings.isEmpty
  def customFn(f: Json.Obj => Json.Obj) = new Software(name, version, featureID, settings, f(custom))
  def json = Json ~? ("name", name) ~? ("version", version) ~? ("featureID", featureID.toArray) ~? ("settings", settings) ~~ custom ~ Json
}
object Software extends FromJson[Software] {
  private val listKeyNames = List("name", "version", "featureID", "settings")
  private val shortListKeyNames = List("name", "version")
  private val someKeyNames = Option(listKeyNames.toSet)
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid software metadata: " + msg))

  val default = new Software("Tracker Commons", "1.0-scala", Set.empty, None, Json.Obj.empty)
  val empty = new Software("", "", Set.empty, None, Json.Obj.empty)

  def parse(j: Json): Either[JastError, Software] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    val count = o.countKeys(someKeyNames)
    count.foreach{ case (key, n) => if (n > 1) return BAD("multiple entries for "+key) }
    if (count.size == 0) BAD("no name, version, or features")
    val List(name, version) = shortListKeyNames.map(key => o get_or_java_null key match {
      case null => ""
      case s: Json.Str => s.text
      case _ => return BAD(key + " is not text")
    })
    val features = o get_or_java_null "featureID" match {
      case null => Set.empty[String]
      case ja: Json.Arr.All => 
        val xs = ja.values.map{ case s: Json.Str => s.text; case x => return BAD("featureID contains non-string value "+x) }
        val ss = xs.toSet
        if (xs.length != ss.size)
          return BAD("duplicate keys in featureID list: " + xs.groupBy(identity).filter(_._2.length > 1).map(_._1).mkString(", "))
        ss
      case s: Json.Str => Set(s.text)
      case _ => return BAD("featureID is not an array of strings")
    }
    val settings = o get_or_java_null "settings" match {
      case j: Json => Some(j)
      case _       => None
    }
    Right(new Software(name, version, features, settings, Custom(o)))
  }
}

case class Metadata(
  id: String,
  lab: Vector[Laboratory],
  who: Vector[String],
  timestamp: Option[Either[java.time.OffsetDateTime, java.time.LocalDateTime]],
  temperature: Option[Double],
  humidity: Option[Double],
  arena: Option[Arena],
  food: Option[String],
  media: Option[String],
  sex: Option[String],
  stage: Option[String],
  age: Option[Double],
  strain: Option[String],
  protocol: Vector[String],
  interpolate: Vector[Interpolate],
  software: Vector[Software],
  custom: Json.Obj
) extends AsJson with MightBeEmpty[Metadata] with Customizable[Metadata] {
  def isEmpty =
    (lab.isEmpty || lab.forall(_.isEmpty)) &&
    (who.isEmpty || who.forall(_.isEmpty)) &&
    timestamp.isEmpty &&
    (temperature.isEmpty || temperature.forall(! _.finite)) &&
    (humidity.isEmpty || humidity.forall(! _.finite)) &&
    (arena.isEmpty || arena.forall(_.isEmpty)) &&
    (food.isEmpty || food.forall(_.isEmpty)) &&
    (media.isEmpty || media.forall(_.isEmpty)) &&
    (sex.isEmpty || sex.forall(_.isEmpty)) &&
    (stage.isEmpty || stage.forall(_.isEmpty)) &&
    (age.isEmpty || age.forall(! _.finite)) &&
    (strain.isEmpty || strain.forall(_.isEmpty)) &&
    (protocol.isEmpty || protocol.forall(_.isEmpty)) &&
    (interpolate.isEmpty || interpolate.forall(_.isEmpty)) &&
    (software.isEmpty || software.forall(_.isEmpty)) &&
    custom.size == 0

  def customFn(f: Json.Obj => Json.Obj) =
    new Metadata(
      id, lab, who,
      timestamp, temperature, humidity, arena, food, media, sex, stage, age, strain,
      protocol, interpolate, software, f(custom)
    )
    
  def json = (Json
    ~? ("id", id)
    ~? ("lab", if (lab.length == 1) lab.head.json else Json(lab))
    ~? ("who", if (who.length == 1) Json(who.head) else Json(who))
    ~? ("timestamp", timestamp.map(e => Json either e))
    ~? ("temperature", temperature)
    ~? ("humidity", humidity)
    ~? ("arena", arena)
    ~? ("food", food getOrElse "")
    ~? ("media", media getOrElse "")
    ~? ("sex", sex getOrElse "")
    ~? ("stage", stage getOrElse "")
    ~? ("age", age)
    ~? ("strain", strain getOrElse "")
    ~? ("protocol", if (protocol.length == 1) Json(protocol.head) else Json(protocol))
    ~? ("interpolate", if (interpolate.length == 1) Json(interpolate.head) else Json(interpolate))
    ~? ("software", if (software.length == 1) Json(software.head) else Json(software))
    ~~ custom ~ Json)
}
object Metadata extends FromJson[Metadata] {
  private implicit val parseLab: FromJson[Laboratory] = Laboratory
  private implicit val parseArena: FromJson[Arena] = Arena
  private implicit val parseInterpolate: FromJson[Interpolate] = Interpolate
  private implicit val parseSoftware: FromJson[Software] = Software

  val listKeyStrings = List("id", "food", "media", "sex", "stage", "strain")
  val listKeyNumbers = List("temperature", "humidity", "age")
  val listKeyOtherSingles = List("timestamp", "arena", "settings")
  val listKeyStringVectors = List("who", "protocol")
  val listKeyOtherVectors = List("lab", "software", "interpolate")
  val someSingles = Option((listKeyStrings ++ listKeyNumbers ++ listKeyOtherSingles).toSet)
  val someVectors = Option((listKeyStringVectors ++ listKeyOtherVectors).toSet)
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid metadata: " + msg))
  private def BAD(msg: String, because: JastError): Either[JastError, Nothing] =
    Left(JastError("Invalid metadata: " + msg, because = because))

  val semanticOrder = new math.Ordering[String] {
    import java.lang.{Character => C, Integer => I}
    private[this] def verifyNumericalDifference(a: String, b: String, i: Int): Boolean = {
      var j = i+1
      while (j < a.length && j < b.length && C.isDigit(a.charAt(j)) && C.isDigit(b.charAt(j))) j += 1
      !(j < b.length && C.isDigit(b.charAt(j)))
    }
    private[this] def compareNonzeroFrom(a: String, j: Int, b: String, k: Int, tiebreak: Int): Int = {
      var da = C.digit(a.charAt(j), 10)
      var db = C.digit(b.charAt(k), 10)
      var bigger = I.compare(da, db)
      var n = 1
      while (da >= 0 && db >= 0 && j+n < a.length && k+n < b.length) {
        da = C.digit(a.charAt(j+n), 10)
        db = C.digit(b.charAt(k+n), 10)
        if (bigger == 0) bigger = I.compare(da, db)
        n += 1
      }
      if (da < 0 && db < 0) { if (bigger == 0) tiebreak else bigger }  // Ran out of numbers at the same time
      else if (da < 0 && db >= 0) -1           // Second number is longer, so clearly bigger
      else if (db < 0 && da >= 0) 1            // First number is longer, so clearly bigger
      else {
        // Stopped due to length criteria
        if      (j+n+1 < a.length && C.isDigit(a.charAt(j+n+1))) 1   // First number is longer, so clearly bigger
        else if (k+n+1 < b.length && C.isDigit(b.charAt(k+n+1))) -1  // Second number longer, so clearly bigger
        else { if (bigger == 0) tiebreak else bigger }               // Same size, so use numeric/lexicographic order already found
      }
    }
    private[this] def compareNumericalFrom(a: String, b: String, i: Int, tiebreak: Int): Int = {
      var j = i-1
      while (j >= 0 && C.digit(a.charAt(j), 10) == 0) j -= 1
      if (j < 0 || C.digit(a.charAt(j), 10) < 0) {
        // Leading part is zeros
        j = i
        var k = i
        while (j < a.length && C.digit(a.charAt(j), 10) == 0) j += 1
        while (k < b.length && C.digit(b.charAt(k), 10) == 0) k += 1
        val aAllZero = (j >= a.length || !C.isDigit(a.charAt(j)))
        val bAllZero = (k >= b.length || !C.isDigit(b.charAt(k)))
        if (aAllZero && bAllZero) tiebreak
        else if (aAllZero) -1
        else if (bAllZero) 1
        else {
          // There's some nonzero stuff, so we can compare it numerically or lexicographically
          compareNonzeroFrom(a, j, b, k, tiebreak)
        }
      }
      else {
        // One number is either numerically or lexicographically bigger than the other
        compareNonzeroFrom(a, i, b, i, tiebreak)
      }
    }
    private[this] def compareFrom(a: String, b: String, i: Int): Int = {
      if (i >= a.length && i >= b.length) 0  // Ran out of both at the same time.  Identical
      else if (i >= a.length) -1  // Everything was identical until we ran out of a.  b wins
      else if (i >= b.length) 1   // Everything was identical until we ran out of b.  a wins
      else if (a.charAt(i) == b.charAt(i)) compareFrom(a, b, i+1)  // Same.  Keep going
      else {
        // There is a difference!  The two strings cannot possibly be the same.
        var ca = a.charAt(i)
        var cb = b.charAt(i)
        var da = C.digit(ca, 10)
        var db = C.digit(cb, 10)
        if (da < 0 || db < 0) {
          // Not both digits
          if (da < 0 && db < 0) C.compare(ca, cb)   // Neither is a digit--lexicographical order
          else if (i == 0 || C.digit(a.charAt(i-1), 10) < 0)  C.compare(ca , cb)  // Not part of a bigger number, lexical again
          else if (db < 0) 1  // b was the shorter number with same prefix, a wins
          else -1             // a was the shorter number with same prefix, b wins
        }
        else if (da > 0 && db > 0 && da != db) {
          // Numbers are different and no worries about leading zeros
          if (da > db) { if (verifyNumericalDifference(a, b, i)) 1 else -1 }
          else {         if (verifyNumericalDifference(b, a, i)) -1 else 1 }
        }
        else  {
          // Compare numerically with possible fallback to lexicographical differences
          compareNumericalFrom(a, b, i, C.compare(ca, cb))
        }
      }
    }
    def compare(a: String, b: String): Int = compareFrom(a, b, 0)
  }

  val empty = new Metadata(
    "", Vector.empty, Vector.empty,
    None, None, None, None, None, None, None, None, None, None,
    Vector.empty, Vector.empty, Vector.empty, Json.Obj.empty
  )

  private def genericJoin[A, B](as: Array[A])(f: A => B)(z: B, msg: String): Either[String, B] =
    Right(as.foldLeft(z){ (c, a) =>
      val b = f(a)
      if (c == z) b else if (b == z) c else if (b != c) return Left("Inconsistent " + msg) else c
    })

  private def joinTimestamps(
    ts: Array[Either[java.time.OffsetDateTime, java.time.LocalDateTime]]
  ): Either[String, Option[Either[java.time.OffsetDateTime, java.time.LocalDateTime]]] =
    if (ts.isEmpty) Right(None)
    else if (ts.length == 1) Right(Some(ts.head))
    else {
      val odts = ts.collect{ case Left(odt) => odt }
      val ldts = ts.collect{ case Right(ldt) => ldt }
      if (odts.length > 0 && ldts.length > 0) Left("Incompatible timestamps: mixture of local and with-offset")
      else if (odts.length > 0) {
        if (odts.exists(oi => oi isBefore odts.head)) Left("Time stamps out of order")
        Right(Some(Left(odts.head)))
      }
      else if (ldts.length > 0) {
        if (ldts.exists(li => li isBefore ldts.head)) Left("Time stamps out of order")
        else Right(Some(Right(ldts.head)))
      }
      else Left("Malformed timestamp found (neither local nor with-offset)")
    }

  private def numericJoin(numbers: Array[Option[Double]]): Option[Double] = {
    val vs = numbers.flatten
    if (vs.length == 0) None
    else if (vs.length == 1) Some(vs(0))
    else if (vs.forall(_ == vs(0))) Some(vs(0))
    else Some((vs.sum / vs.length))
  }

  def join(mds: Array[Metadata]): Either[String, Metadata] = {
    def VNil[A] = Vector.empty[A]
    val id = genericJoin(mds)(_.id)("", "IDs")                       match { case Right(x) => x; case Left(e) => return Left(e) }
    val lab = genericJoin(mds)(_.lab)(VNil, "labs")                  match { case Right(x) => x; case Left(e) => return Left(e) }
    val who = genericJoin(mds)(_.who)(VNil, "who")                   match { case Right(x) => x; case Left(e) => return Left(e) }
    val time = joinTimestamps(mds.flatMap(_.timestamp))              match { case Right(x) => x; case Left(e) => return Left(e) }
    val temp = numericJoin(mds.map(_.temperature))
    val humid = numericJoin(mds.map(_.humidity))
    val arena = genericJoin(mds)(_.arena)(None, "arenas")            match { case Right(x) => x; case Left(e) => return Left(e) }
    val food = genericJoin(mds)(_.food)(None, "food")                match { case Right(x) => x; case Left(e) => return Left(e) }
    val media = genericJoin(mds)(_.media)(None, "media")             match { case Right(x) => x; case Left(e) => return Left(e) }
    val sex = genericJoin(mds)(_.sex)(None, "sex")                   match { case Right(x) => x; case Left(e) => return Left(e) }
    val stage = genericJoin(mds)(_.stage)(None, "stage")             match { case Right(x) => x; case Left(e) => return Left(e) }
    val age = genericJoin(mds)(_.age)(None, "age")                   match { case Right(x) => x; case Left(e) => return Left(e) }
    val strain = genericJoin(mds)(_.strain)(None, "strain")          match { case Right(x) => x; case Left(e) => return Left(e) }
    val prot = genericJoin(mds)(_.protocol)(VNil, "protocol")        match { case Right(x) => x; case Left(e) => return Left(e) }
    val inp = genericJoin(mds)(_.interpolate)(VNil, "interpolation") match { case Right(x) => x; case Left(e) => return Left(e) }
    val soft = genericJoin(mds)(_.software)(VNil, "software")        match { case Right(x) => x; case Left(e) => return Left(e) }
    val custom = Custom.accumulate(mds.map(_.custom)) match { case Some(x) => x; case None => return Left("Inconsistent custom metadata") }
    Right(new Metadata(id, lab, who, time, temp, humid, arena, food, media, sex, stage, age, strain, prot, inp, soft, custom))
  }

  def parse(j: Json): Either[JastError, Metadata] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    val count = o.countKeys(someSingles)
    count.foreach{ case (key, n) => if (n > 1) return BAD("duplicate entries in metadata for " + key) }
    val vcount = o.countKeys(someVectors)
    val List(id, food, media, sex, stage, strain) = listKeyStrings.map{ key => o get_or_java_null key match {
      case null => None
      case s: Json.Str => Some(s.text)
      case _ => return BAD("non-string entry in metadata for " + key)
    }}
    val List(temperature, humidity, age) = listKeyNumbers.map{ key => o.get(key).map{
      case _: Json.Null => Double.NaN
      case n: Json.Num => n.double
      case _ => return BAD("non-numeric entry in metadata for " + key)
    }}
    val timestamp = o.get("timestamp").map{_.to[Either[java.time.OffsetDateTime, java.time.LocalDateTime]] match {
      case Right(e) => e
      case _ => return BAD("non-date entry in metadata timestamp")
    }}
    val arena = o.get("arena").map{_.to[Arena] match {
      case Right(a) => a
      case Left(je: JastError) => return BAD("Could not read metadata for arena.", because = je)
    }}
    val settings = o.get("settings")
    val List(who, protocol) = listKeyStringVectors.map{ key => 
      val vb = Vector.newBuilder[String]
      o.foreach((k, v) => if (k == key) {
        v match {
          case s: Json.Str => vb += s.text
          case ss: Json.Arr.All => ss.foreach{
            case s: Json.Str => vb += s.text
            case _ => return BAD("non-string entry in metadata for "+ key)
          }
          case _ => return BAD("non-string entry in metadata for "+key)
        }
      })
      vb.result
    }
    val lab = o.fold(Vector.newBuilder[Laboratory]){ (ls, k, v) =>
      if (k == "lab") v.to[Either[Array[Laboratory], Laboratory]] match {
        case Right(Right(l)) => ls += l
        case Right(Left(ll)) => ls ++= ll
        case Left(je) => return BAD("error in lab data in metadata", because = je)
      }
      ls
    }.result
    val interpolate = o.fold(Vector.newBuilder[Interpolate]){ (is, k, v) =>
      if (k == "interpolate") v.to[Either[Array[Interpolate], Interpolate]] match {
        case Right(Right(i)) => is += i
        case Right(Left(ii)) => is ++= ii
        case Left(je) => return BAD("error in interpolate data in metadata", because = je)
      }
      is
    }.result
    val software = o.fold(Vector.newBuilder[Software]){ (ss, k, v) =>
      if (k == "software") v.to[Either[Array[Software], Software]] match {
        case Right(Right(s)) => ss += s
        case Right(Left(s2)) => ss ++= s2
        case Left(je) => return BAD("error in software data in metadata", because = je)
      }
      ss
    }.result
    Right(new Metadata(
      id.getOrElse(""), lab, who,
      timestamp, temperature, humidity, arena, food, media, sex, stage, age, strain,
      protocol, interpolate, software, Custom(o)
    ))
  }
}
