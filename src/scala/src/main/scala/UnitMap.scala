package org.openworm.trackercommons

case class UnitMap(lookup: Map[String, units.Units], custom: json.ObjJ) extends json.Jsonable {
  val toObjJ = json.ObjJ(lookup.map{ case (k,v) => k -> (json.StrJ(v.name) :: Nil) } ++ custom.keyvals)
  def has(s: String) = lookup contains s
  def missing(s: String) = !(lookup contains s)
  def fix(j: json.JSON, known: Option[units.Units] = None): j.type = {
    j match {
      case n @ json.NumJ(x) => known match { case Some(u) => n.value = u of x; case None => }
      case json.ANumJ(xs) => known match { case Some(u) => u fix xs; case None => }
      case json.AANumJ(xss) => known match { case Some(u) => u fix xss; case None => }
      case json.ArrJ(js) => var i = 0; while (i < js.length) { fix(js(i), known); i +=1 }
      case json.ObjJ(kv) => kv.foreach{ case (k,vs) => vs.foreach(v => fix(v, lookup.get(k) orElse known)) }
      case _ =>
    }
    j
  }
  def unfix(j: json.JSON, known: Option[units.Units] = None): j.type = {
    j match {
      case n @ json.NumJ(x) => known match { case Some(u) => n.value = u from x; case None => }
      case json.ANumJ(xs) => known match { case Some(u) => u unfix xs; case None => }
      case json.AANumJ(xss) => known match { case Some(u) => u unfix xss; case None => }
      case json.ArrJ(js) => var i = 0; while (i < js.length) { unfix(js(i), known); i += 1 }
      case json.ObjJ(kv) => kv.foreach{ case (k,vs) => vs.foreach(v => unfix(v, lookup.get(k) orElse known)) }
      case _ =>
    }
    j
  }
}
object UnitMap extends json.Jsonic[UnitMap] {
  def from(ob: json.ObjJ): Either[String, UnitMap] = Right(new UnitMap(
    ob.keyvals.map{ 
      case (k, json.StrJ(v) :: Nil) => units.Units(v) match {
        case Some(u) => k -> u
        case None => return Left("Invalid unit: " + v)
      }
      case _ => return Left("Invalid units: all units must be given once, and as text")
    },
    Metadata.getCustom(ob)
  ))
}
