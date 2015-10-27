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

case class Temperature(experimental: Double, cultivation: Double, custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map.empty[String, List[json.JSON]]
    if (!experimental.isNaN) m = m + ("experimental" -> (json.NumJ(experimental) :: Nil))
    if (!cultivation.isNaN) m = m + ("cultivation" -> (json.NumJ(cultivation) :: Nil))
    json.ObjJ(m ++ custom.keyvals)
  }
}

case class Arena(kind: String, diameter: Double, otherDiameter: Double, custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map.empty[String, List[json.JSON]]
    if (kind.length > 0) m = m + ("type" -> (json.StrJ(kind) :: Nil))
    if (!diameter.isNaN) m = m + ("diameter" -> (json.NumJ(diameter) :: Nil))
    if (!otherDiameter.isNaN) m = m + ("axis2_diameter" -> (json.NumJ(otherDiameter) :: Nil))
    json.ObjJ(m ++ custom.keyvals)
  }
}

case class Software(name: String, version: String, featureID: String, custom: json.ObjJ) extends json.Jsonable {
  def toObjJ = {
    var m = Map[String, List[json.JSON]]("name" -> (json.StrJ(name) :: Nil))
    if (version.length > 0) m = m + ("version" -> (json.StrJ(version) :: Nil))
    if (featureID.length > 0) m = m + ("featureID" -> (json.StrJ("@" + featureID) :: Nil))
    json.ObjJ(m ++ custom.keyvals)
  }
}
object Software {
  def default = new Software("Tracker Commons", "1.0-scala", "", Metadata.emptyObjJ)
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
    if (protocol.nonEmpty) m += ("protocol", (if (who.length == 1) json.StrJ(protocol.head) else json.ArrJ(protocol.toArray.map(p => json.StrJ(p): json.JSON))) :: Nil)
    software match { case Some(s) => m += ("software", s.toObjJ :: Nil); case _ => }
    settings match { case Some(s) => m += ("settings", s :: Nil); case _ => }
    custom.keyvals.foreach{ case (k,vs) => m += (k, vs) }
    json.ObjJ(m.toMap)
  }
}
object Metadata {
  val emptyObjJ = json.ObjJ(Map.empty)
  val empty = new Metadata(Vector.empty, Vector.empty, None, None, None, None, None, None, None, None, None, None, Vector.empty, None, None, emptyObjJ)
  def from(ob: json.ObjJ): Either[String, Metadata] = {
    ???
  }
}
