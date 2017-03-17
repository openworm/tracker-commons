package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

import WconImplicits._

trait MightBeEmpty[A] { self: A =>
  def isEmpty: Boolean
  def nonEmptyOption: Option[A] = if (isEmpty) None else Some(this)
}

trait Customizable[A] { self: A =>
  def custom: Json.Obj
  def customFn(f: Json.Obj => Json.Obj): A
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
    Right(new Laboratory(pi, name, location, contact, o.filter((k,_) => k.startsWith("@"))))
  }
}

case class Arena(kind: String, diameter: Either[(Double, Double), Double], orient: String, custom: Json.Obj)
extends AsJson with MightBeEmpty[Arena] with Customizable[Arena] {
  def isEmpty =
    kind.isEmpty &&
    (diameter match { case Right(x) => !x.finite; case Left((x,y)) => !x.finite && !y.finite }) &&
    orient.isEmpty &&
    custom.size == 0

  def customFn(f: Json.Obj => Json.Obj) = new Arena(kind, diameter, orient, f(custom))

  import Arena.jsonizeDoublePair
  def json = (Json
    ~? ("type", kind)
    ~ ("diameter", Json either diameter)
    ~? ("orient", orient)
    ~~ custom ~ Json)
}
object Arena extends FromJson[Arena] {
  private[trackercommons] implicit val jsonizeDoublePair: Jsonize[(Double, Double)] = new Jsonize[(Double, Double)] {
    def jsonize(dd: (Double, Double)): Json = Json.Arr.Dbl(Array(dd._1, dd._2))
  }
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid arena: " + msg))
  def empty = new Arena("", Right(Double.NaN), "", Json.Obj.empty)
  def parse(j: Json): Either[JastError, Arena] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    var kind, orient: String = null
    var diam: Either[(Double, Double), Double] = null
    o.foreach{ case (k,v) =>
      if (k == "type") {
        if (kind ne null) return BAD("multiple type entries")
        kind = v stringOr { return BAD("type is not expressed as text") }
      }
      else if (k == "diameter") {
        if (diam ne null) return BAD("multiple diameter entries (two entries should be in an array)")
        diam = v.to[Either[Array[Double], Double]] match {
          case Right(Right(x)) => Right(x)
          case Right(Left(xs)) => if (xs.length == 1) Right(xs(0))
            else if (xs.length == 2) Left((xs(0), xs(1)))
            else return BAD("wrong number of diameter entries: " + xs.length)
          case _ => return BAD("diameter is neither a number nor an array of two numbers")
        }
      }
      else if (k == "orient") {
        if (orient ne null) return BAD("multiple orientation entries")
        orient = v stringOr { return BAD("orientation is not expressed as text") }
      }
    }
    if ((kind eq null) && (orient eq null) && (diam eq null)) return BAD("No aspects of Arena are specified (type, diameter, orient)")
    Right(new Arena(if (kind eq null) "" else kind, diam, if (orient eq null) "" else orient, o.filter((k, _) => k.startsWith("@"))))
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
    Right(new Software(name, version, features, settings, o.filter{ (k,_) => k.startsWith("@") }))
  }
}

case class Metadata(
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
      lab, who, timestamp, temperature, humidity, arena, food, media, sex, stage, age, strain, protocol, interpolate, software, f(custom)
    )
    
  def json = (Json
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

  val listKeyStrings = List("food", "media", "sex", "stage", "strain")
  val listKeyNumbers = List("temperature", "humidity", "age")
  val listKeyOtherSingles = List("timestamp", "arena", "settings")
  val listKeyStringVectors = List("who", "protocol")
  val listKeyOtherVectors = List("lab", "software", "interpolate")
  val someSingles = Option((listKeyStrings ++ listKeyNumbers ++ listKeyOtherSingles).toSet)
  val someVectors = Option((listKeyStringVectors ++ listKeyOtherVectors).toSet)
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid metadata: " + msg))
  private def BAD(msg: String, because: JastError): Either[JastError, Nothing] =
    Left(JastError("Invalid metadata: " + msg, because = because))

  val empty = new Metadata(
    Vector.empty, Vector.empty, None, None, None, None, None, None, None, None, None, None, Vector.empty, Vector.empty, Vector.empty, Json.Obj.empty
  )

  def parse(j: Json): Either[JastError, Metadata] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    val count = o.countKeys(someSingles)
    count.foreach{ case (key, n) => if (n > 1) return BAD("duplicate entries in metadata for " + key) }
    val vcount = o.countKeys(someVectors)
    val List(food, media, sex, stage, strain) = listKeyStrings.map{ key => o get_or_java_null key match {
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
      lab, who, timestamp, temperature, humidity, arena, food, media, sex, stage, age, strain, protocol, interpolate, software,
      o.filter((k, _) => k.startsWith("@"))
    ))
  }
}
