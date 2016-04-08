package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

case class UnitMap(lookup: Map[String, units.Convert], custom: Json.Obj) extends AsJson {
  import units._
  val json = Json ~~ lookup ~~ custom ~~ Json
  def has(s: String) = lookup contains s
  def missing(s: String) = !(lookup contains s)
  // These are duplicated for speed.
  private def renumF(n: Json.Num, u: units.Convert): Json = {
    val x = n.double
    val y = x from u
    if (x == y || x.isNaN) n else Json.Num(y)
  }
  private def renumU(n: Json.Num, u: units.Convert): Json = {
    val x = n.double
    val y = x into u
    if (x == y || x.isNaN) n else Json.Num(y)
  }
  // Note -- we need to go in both directions to take advantage of floating point division
  // being more accurate than multiplication by the reciprocal
  def fix(j: Json, known: Option[units.Convert] = None): Json = {
    j match {
      case n: Json.Num => known match { case Some(u) => renumF(n, u); case None => n }
      case jad: Json.Arr.Dbl => known match { case Some(u) => jad.doubles changeFrom u; case None => }; jad
      case jaa: Json.Arr.All => 
        val a = jaa.values   // Will mutate underlying array directly
        var i = 0
        while (i < a.length) { val j = fix(a(i), known); if (a(i) ne j) a(i) = j; i += 1 }
        jaa
      case o: Json.Obj => o.transformValues{ (k,v) => fix(v, lookup.get(k) orElse known) }
      case _ =>
    }
    j
  }
  def unfix(j: Json, known: Option[units.Convert] = None): Json = {
    j match {
      case n: Json.Num => known match { case Some(u) => renumU(n, u); case None => n }
      case jad: Json.Arr.Dbl => known match { case Some(u) => jad.doubles changeInto u; case None => }; jad
      case jaa: Json.Arr.All => 
        val a = jaa.values   // Will mutate underlying array directly
        var i = 0
        while (i < a.length) { val j = fix(a(i), known); if (a(i) ne j) a(i) = j; i += 1 }
        jaa
      case o: Json.Obj => o.transformValues{ (k,v) => fix(v, lookup.get(k) orElse known) }
      case _ =>
    }
    j
  }
}
object UnitMap extends FromJson[UnitMap] {
  import units._
  def parse(j: Json): Either[JastError, UnitMap] = j match {
    case o: Json.Obj =>
      o.filter((k,_) => !k.startsWith("@")).to[Map[String, Convert]] match {
        case Right(m) => Right(new UnitMap(m, o.filter((k, _) => k.startsWith("@"))))
        case e: Left[_, _] => e.asInstanceOf[Left[JastError, UnitMap]]
      }
    case _ => Left(JastError("Units are not a JSON object"))
  }
}
