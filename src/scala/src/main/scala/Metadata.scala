package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

case class Laboratory(pi: String, name: String, location: String, custom: Json.Obj) extends AsJson {
  def json = Json ~? ("PI", pi) ~? ("name", name) ~? ("location", location) ~~ custom ~ Json
}
object Laboratory extends FromJson[Laboratory] {
  private val listKeyNames = List("PI", "name", "location")
  private val someKeyNames = Option(listKeyNames.toSet)
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid laboratory: " + msg))
  def empty = new Laboratory("", "", "", Json.Obj.empty)
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
    val List(pi, name, location) = listKeyNames.map(key => o getOrNull key match {
      case null => ""
      case Json.Str(text) => text
      case _ => return BAD(key + " is not text") 
    })
    Right(new Laboratory(pi, name, location, o.filter((k,_) => k.startsWith("@"))))
  }
}

case class Arena(kind: String, diameter: Either[(Double, Double), Double], orient: String, custom: Json.Obj) extends AsJson {
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
      case jo: Json. Obj => jo
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

case class Software(name: String, version: String, featureID: Set[String], custom: Json.Obj) extends AsJson {
  def json = Json ~? ("name", name) ~? ("version", version) ~? ("featureID", featureID.toArray) ~~ custom ~ Json
}
object Software extends FromJson[Software] {
  private val listKeyNames = List("name", "version", "featureID")
  private val shortListKeyNames = List("name", "version")
  private val someKeyNames = Option(listKeyNames.toSet)
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid software metadata: " + msg))
  def default = new Software("Tracker Commons", "1.0-scala", Set.empty, Json.Obj.empty)
  def parse(j: Json): Either[JastError, Software] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    val count = o.countKeys(someKeyNames)
    count.foreach{ case (key, n) => if (n > 1) return BAD("multiple entries for "+key) }
    if (count.size == 0) BAD("no name, version, or features")
    val List(name, version) = shortListKeyNames.map(key => o getOrNull key match {
      case null => ""
      case s: Json.Str => s.text
      case _ => return BAD(key + " is not text")
    })
    val features = o getOrNull "featureID" match {
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
    Right(new Software(name, version, features, o.filter{ (k,_) => k.startsWith("@") }))
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
  software: Vector[Software],
  settings: Option[Json],
  custom: Json.Obj
) extends AsJson {
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
    ~? ("software", if (software.length == 1) Json(software.head) else Json(software))
    ~? ("settings", settings)
    ~~ custom ~ Json)
}
object Metadata extends FromJson[Metadata] {
  private implicit val parseLab: FromJson[Laboratory] = Laboratory
  private implicit val parseArena: FromJson[Arena] = Arena
  private implicit val parseSoftware: FromJson[Software] = Software

  val listKeyStrings = List("food", "media", "sex", "stage", "strain")
  val listKeyNumbers = List("temperature", "humidity", "age")
  val listKeyOtherSingles = List("timestamp", "arena", "settings")
  val listKeyStringVectors = List("who", "protocol")
  val listKeyOtherVectors = List("lab", "software")
  val someSingles = Option((listKeyStrings ++ listKeyNumbers ++ listKeyOtherSingles).toSet)
  val someVectors = Option((listKeyStringVectors ++ listKeyOtherVectors).toSet)
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid metadata: " + msg))
  private def BAD(msg: String, because: JastError): Either[JastError, Nothing] =
    Left(JastError("Invalid metadata: " + msg, because = because))
  val empty = new Metadata(
    Vector.empty, Vector.empty, None, None, None, None, None, None, None, None, None, None, Vector.empty, Vector.empty, None, Json.Obj.empty
  )
  def parse(j: Json): Either[JastError, Metadata] = {
    val o = j match {
      case jo: Json.Obj => jo
      case _ => return BAD("not a JSON object")
    }
    val count = o.countKeys(someSingles)
    count.foreach{ case (key, n) => if (n > 1) return BAD("duplicate entries in metadata for " + key) }
    val vcount = o.countKeys(someVectors)
    val List(food, media, sex, stage, strain) = listKeyStrings.map{ key => o getOrNull key match {
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
    val software = o.fold(Vector.newBuilder[Software]){ (ss, k, v) =>
      if (k == "software") v.to[Either[Array[Software], Software]] match {
        case Right(Right(s)) => ss += s
        case Right(Left(s2)) => ss ++= s2
        case Left(je) => return BAD("error in software data in metadata", because = je)
      }
      ss
    }.result
    Right(new Metadata(lab, who, timestamp, temperature, humidity, arena, food, media, sex, stage, age, strain, protocol, software, settings, o.filter((k, _) => k.startsWith("@"))))
  }
}
