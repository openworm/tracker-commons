package org.openworm.trackercommons

case class Laboratory(pi: String, name: String, location: String, custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map.empty[String, List[json.JSON]]
    if (pi.length > 0) m = m + ("pi" -> (json.StrJ(pi) :: Nil))
    if (name.length > 0) m = m + ("name" -> (json.StrJ(name) :: Nil))
    if (location.length > 0) m = m + ("location" -> (json.StrJ(location) :: Nil))
    json.ObjJ(m ++ custom.keyvals)
  }
}
object Laboratory extends json.Jsonic[Laboratory] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid laboratory: " + msg)
  def empty = new Laboratory("", "", "", json.ObjJ.empty)
  def from(ob: json.ObjJ): Either[String, Laboratory] = {
    val pi = ob.keyvals.get("pi").
      flatMap(x => if (x.length > 1) return BAD("multiple entries for head investigator (PI)") else x.headOption).
      map{
        case json.StrJ(s) => s
        case _ => return BAD("head investigator entry is not text")
      }
    val name = ob.keyvals.get("name").
      flatMap(x => if (x.length > 1) return BAD("multiple entries for lab name") else x.headOption).
      map{
        case json.StrJ(s) => s
        case _ => return BAD("lab name is not text")
      }
    val loc = ob.keyvals.get("location").
      flatMap(x => if (x.length > 1) return BAD("multiple entries for location") else x.headOption).
      map{
        case json.StrJ(s) => s
        case _ => return BAD("lab location is not text")
      }
    val custom = Metadata.getCustom(ob)
    if (pi.isEmpty && name.isEmpty && loc.isEmpty && custom.isEmpty) BAD("no PI, name, location, or custom fields")
    else Right(new Laboratory(pi getOrElse "", name getOrElse "", loc getOrElse "", custom))
  }
}

case class Temperature(experimental: Double, cultivation: Double, custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map.empty[String, List[json.JSON]]
    if (!experimental.isNaN) m = m + ("experimental" -> (json.NumJ(experimental) :: Nil))
    if (!cultivation.isNaN) m = m + ("cultivation" -> (json.NumJ(cultivation) :: Nil))
    json.ObjJ(m ++ custom.keyvals)
  }
}
object Temperature extends json.Jsonic[Temperature] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid temperature: " + msg)
  def from(ob: json.ObjJ): Either[String, Temperature] = {
    val expt = ob.keyvals.get("experimental").
      flatMap(x => if (x.length > 1) return BAD("multiple experimental temperatures") else x.headOption).
      map{
        case json.Dbl(d) => d
        case _ => return BAD("experimental temperature is not numeric")
      }
    val cult = ob.keyvals.get("cultivation").
      flatMap(x => if (x.length > 1) return BAD("multiple cultivation temperatures are not supported") else x.headOption).
      map{
        case json.Dbl(d) => d
        case _ => return BAD("cultivation temperature is not numeric")
      }
    val custom = Metadata.getCustom(ob)
    if (expt.isEmpty && cult.isEmpty && custom.isEmpty) BAD("no experimental or cultivation temperatures, or custom fields")
    else Right(new Temperature(expt getOrElse Double.NaN, cult getOrElse Double.NaN, custom))
  }
}

case class Arena(kind: String, diameter: Either[(Double, Double), Double], custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map.empty[String, List[json.JSON]]
    if (kind.length > 0) m = m + ("type" -> (json.StrJ(kind) :: Nil))
    diameter match {
      case Left((d1, d2)) => m = m + ("diameter" -> (json.ANumJ(Array(d1, d2)) :: Nil))
      case Right(d) if !d.isNaN => m = m + ("diameter" -> (json.NumJ(d) :: Nil))
      case _ =>
    }
    json.ObjJ(m ++ custom.keyvals)
  }
}
object Arena extends json.Jsonic[Arena] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid arena: " + msg)
  def from(ob: json.ObjJ): Either[String, Arena] = {
    val kind = ob.keyvals.get("type").
      flatMap(x => if (x.length > 1) return BAD("multiple type entries") else x.headOption).
      map{
        case json.StrJ(s) => s
        case _ => return BAD("type is not expressed as text")
      }
    val diam = ob.keyvals.get("diameter").
      flatMap(x => if (x.length > 1) return BAD("multiple diameter entries (two entries should be in an array)") else x.headOption).
      map {
        case json.Dbl(d) => Right(d)
        case json.ANumJ(ds) if ds.length == 2 => Left((ds(0), ds(1)))
        case _ => return BAD("diameter is not numeric")
      }
    val custom = Metadata.getCustom(ob)
    if (kind.isEmpty && diam.isEmpty && custom.isEmpty) BAD("no type or diameter or custom fields")
    else Right(new Arena(kind getOrElse "", diam getOrElse Right(Double.NaN), custom))
  }
}

case class Software(name: String, version: String, featureID: Set[String], custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map[String, List[json.JSON]]("name" -> (json.StrJ(name) :: Nil))
    if (version.length > 0) m = m + ("version" -> (json.StrJ(version) :: Nil))
    if (featureID.size > 0) m = m + ("featureID" -> (
      if (featureID.size == 1) json.StrJ(featureID.head) :: Nil
      else json.ArrJ(featureID.toArray.sorted.map(x => json.StrJ(x): json.JSON)) :: Nil
    ))
    json.ObjJ(m ++ custom.keyvals)
  }
}
object Software extends json.Jsonic[Software] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid software metadata: " + msg)
  def default = new Software("Tracker Commons", "1.0-scala", Set.empty, json.ObjJ.empty)
  def from(ob: json.ObjJ): Either[String, Software] = {
    val name = ob.keyvals.get("name").
      flatMap(x => if (x.length > 1) return BAD("multiple name entries") else x.headOption).
      map{
        case json.StrJ(s) => s
        case _ => return BAD("name is not text")
      }
    val ver = ob.keyvals.get("version").
      flatMap(x => if (x.length > 1) return BAD("multiple version entries") else x.headOption).
      flatMap{
        case json.NullJ => None
        case json.StrJ(s) => Some(s)
        case json.Dbl(d) => if (d.isInfinite || d.isNaN) None else Some("%.4f".format(d))
        case _ => return BAD("name is not text")
      }
    val feat = ob.keyvals.get("featureID").
      flatMap{ vs =>
        val labels = vs.flatMap{
          case json.StrJ(s) => s :: Nil
          case json.ArrJ(ss) => ss.map{ case json.StrJ(s) => s; case _ => return BAD("non-text featureID") }
          case _ => return BAD("non-text featureID")
        }
        labels.foreach{ l => if (!(l startsWith "@")) return BAD("featureID '" + l + "' does not start with @") }
        if (labels.isEmpty) None
        else Some(labels.toSet)
      }
    val custom = Metadata.getCustom(ob)
    if (name.isEmpty && ver.isEmpty && feat.isEmpty && custom.isEmpty) BAD("no name, version, features, or custom fields")
    else Right(new Software(name getOrElse "", ver getOrElse "", feat getOrElse Set.empty, custom))
  }
}

case class Metadata(
  lab: Vector[Laboratory],
  who: Vector[String],
  timestamp: Option[(java.time.LocalDateTime, String)],
  temperature: Option[Temperature],
  humidity: Option[Double],
  arena: Option[Arena],
  food: Option[String],
  media: Option[String],
  sex: Option[String],
  stage: Option[String],
  age: Option[java.time.Duration],
  strain: Option[String],
  protocol: Vector[String],
  software: Option[Software],
  settings: Option[json.JSON],
  custom: json.ObjJ
) extends json.Jsonable {
  def toObjJ = {
    val m = collection.mutable.AnyRefMap.empty[String, List[json.JSON]]
    if (lab.nonEmpty) m += ("lab", (if (lab.length == 1) lab.head.toObjJ else json.ArrJ(lab.toArray.map(l => l.toObjJ: json.JSON))) :: Nil)
    if (who.nonEmpty) m += ("who", (if (who.length == 1) json.StrJ(who.head) else json.ArrJ(who.toArray.map(w => json.StrJ(w): json.JSON))) :: Nil)
    timestamp match { case Some((ldt,s)) => m += ("timestamp", json.StrJ(ldt.toString + s) :: Nil); case _ => }
    temperature match { case Some(t) => m += ("temperature", t.toObjJ :: Nil); case _ => }
    humidity match { case Some(h) if !h.isNaN => m += ("humidity", json.NumJ(h) :: Nil); case _ => }
    arena match { case Some(a) => m += ("arena", a.toObjJ :: Nil); case _ => }
    food match { case Some(s) if s.length > 0 => m += ("food", json.StrJ(s) :: Nil); case _ => }
    media match { case Some(s) if s.length > 0 => m += ("media", json.StrJ(s) :: Nil); case _ => }
    sex match { case Some(s) if s.length > 0 => m += ("sex", json.StrJ(s) :: Nil); case _ => }
    stage match { case Some(s) if s.length > 0 => m += ("stage", json.StrJ(s) :: Nil); case _ => }
    age match { case Some(t) => m += ("age", json.StrJ(Parser.durationFormat(t)) :: Nil ); case _ => }
    strain match { case Some(s) if s.length > 0 => m += ("strain", json.StrJ(s) :: Nil); case _ => }
    if (protocol.nonEmpty) m += ("protocol", (if (protocol.length == 1) json.StrJ(protocol.head) else json.ArrJ(protocol.toArray.map(p => json.StrJ(p): json.JSON))) :: Nil)
    software match { case Some(s) => m += ("software", s.toObjJ :: Nil); case _ => }
    settings match { case Some(s) => m += ("settings", s :: Nil); case _ => }
    custom.keyvals.foreach{ case (k,vs) => m += (k, vs) }
    json.ObjJ(m.toMap)
  }
}
object Metadata extends json.Jsonic[Metadata] {
  private def BAD(msg: String): Either[String, Nothing] = Left("Invalid metadata: " + msg) 
  def getCustom(ob: json.ObjJ) =
    if (!ob.keyvals.exists{ case (k,vs) => vs.nonEmpty && (k startsWith "@") }) json.ObjJ.empty
    else ob.keyvals.filter{ case (k,vs) => vs.nonEmpty && (k startsWith "@") } match { case x => json.ObjJ(x) }
  val empty = new Metadata(Vector.empty, Vector.empty, None, None, None, None, None, None, None, None, None, None, Vector.empty, None, None, json.ObjJ.empty)
  def from(ob: json.ObjJ): Either[String, Metadata] = {
    def allOrBad[A](key: String, extract: json.JSON => Either[String, A]): Either[String, Vector[A]] = ob.keyvals.get(key) match {
      case None => Right(Vector.empty[A])
      case Some(vs) => Right(
        vs.flatMap{
          case json.ArrJ(js) => js
          case j => j :: Nil
        }.map(j => extract(j) match {
          case Left(l) => return Left(l)
          case Right(r) => r
        }).toVector
      )
    }
    def allObjOrBad[A](key: String, extract: json.ObjJ => Either[String, A]) =
      allOrBad(key, { case ob: json.ObjJ => extract(ob); case _ => Left(key + " should be an object")})
    def allString(key: String) = allOrBad(key, { case json.StrJ(s) => Right(s); case _ => Left("non-text " + key) })

    def optionOrBad[A](key: String, extract: json.JSON => Either[String, A]): Either[String, Option[A]] = ob.keyvals.get(key) match {
      case None => Right(None)
      case Some(Nil) => Right(None)
      case Some(j :: Nil) => extract(j).right.map(r => Option(r))
      case _ => Left("more than one entry for " + key)
    }
    def optObjOrBad[A](key: String, extract: json.ObjJ => Either[String, A]) =
      optionOrBad(key, { case ob: json.ObjJ => extract(ob); case _ => Left(key + " should be an object")})
    def optString(key: String) = optionOrBad(key, { case json.StrJ(s) => Right(s); case _ => Left("non-text " + key) })

    val lab = allObjOrBad("lab", Laboratory from _) match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val who = allString("who") match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val time = optString("timestamp") match {
      case Left(msg) => return BAD(msg)
      case Right(None) => None
      case Right(Some(str)) => Parser.Date.parse(str) match {
        case fastparse.core.Result.Success(ltd, _) => Some(ltd)
        case _ => return BAD("improper format in timestamp")
      }
    }
    val temp = optObjOrBad("temperature", Temperature from _) match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val humidity =
      optionOrBad("humidity", { case json.Dbl(d) => Right(d); case _ => Left("non-numeric humidity") }) match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val arena = optObjOrBad("arena", Arena from _) match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val food = optString("food") match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val media = optString("media") match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val sex = optString("sex") match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val stage = optString("stage") match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val age = optString("age") match {
      case Left(msg) => return BAD(msg)
      case Right(None) => None
      case Right(Some(str)) => Parser.Age.parse(str) match {
        case fastparse.core.Result.Success(dur, _) => Some(dur)
        case _ => return BAD("improper format in age")
      }      
    }
    val strain = optString("strain") match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val protocol = allString("protocol") match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val software = optObjOrBad("software", Software from _) match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val settings = optionOrBad("settings", {j => Right(j)}) match { case Left(msg) => return BAD(msg); case Right(x) => x }
    val custom = getCustom(ob)
    Right(new Metadata(
      lab, who, time, temp, humidity,
      arena, food, media, sex, stage,
      age, strain, protocol, software, settings,
      custom
    ))
  }
}
