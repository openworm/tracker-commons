package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

case class UnitMap(lookup: Map[String, units.Convert], custom: Json.Obj)
extends AsJson with Customizable[UnitMap] {
  import units._
  val json = Json ~~ lookup ~~ custom ~~ Json
  def has(s: String) = lookup contains s
  def missing(s: String) = !(lookup contains s)

  def customFn(f: Json.Obj => Json.Obj) = copy(custom = f(custom))

  private def renum(n: Json.Num, u: units.Convert, from: Boolean): Json = {
    val x = n.double
    val y = if (from) x from u else x into u
    if (x == y || x.isNaN) n else Json.Num(y)
  }

  private def convert(j: Json, into: UnitMap.Into, u: units.Convert, from: Boolean): Json =
    j match {
      case o: Json.Obj =>
        if (o.isArrayBacked) {
          val kva = o.asFlatArray
          var a: Array[AnyRef] = null
          var j = 0
          var i = 0
          var same = true
          while (same && i+1 < kva.length) {
            val ki = kva(i).asInstanceOf[String]
            val vi = kva(i+1).asInstanceOf[Json]
            val wi = into(ki) match {
              case Some(in) =>
                val ui = lookup.getOrElse(ki, u)
                convert(vi, in, ui, from)
              case _ =>
                lookup.get(ki) match {
                  case Some(ui) => convert(vi, UnitMap.Nowhere, ui, from)
                  case _        => vi
                }
            }
            if (wi ne vi) {
              same = false
              a = new Array[AnyRef](kva.length)
              while (j < i) { a(j) = kva(j); j += 1 }
              a(j) = ki
              a(j+1) = wi
              j += 2
            }
            i += 2
          }
          if (j != 0) {
            while (j+1 < kva.length) {
              val kj = kva(j).asInstanceOf[String]
              a(j) = kj
              val vj = kva(j+1).asInstanceOf[Json]
              a(j+1) = into(kj) match {
                case Some(in) =>
                  val uj = lookup.getOrElse(kj, u)
                  convert(vj, in, uj, from)
                case _ =>
                  lookup.get(kj) match {
                    case Some(uj) => convert(vj, UnitMap.Nowhere, uj, from)
                    case _        => vj
                  }
              }
              j += 2
            }
            Json.Obj fromFlatArray a
          }
          else o
        }
        else {
          var transformed = false
          val oo = o.transformValues{ (ki, vi) =>
            val wi = into(ki) match {
              case Some(in) =>
                val ui = lookup.getOrElse(ki, u)
                convert(vi, in, ui, from)
              case _ =>
                lookup.get(ki) match {
                  case Some(ui) => convert(vi, UnitMap.Nowhere, ui, from)
                  case _        => vi
                }
            }              
            if (wi ne vi) transformed = true
            wi
          }
          if (transformed) oo else o
        }
      case jaa: Json.Arr.All =>
        var a: Array[Json] = null
        var j = 0
        var i = 0
        var same = true
        while (same && i < jaa.values.length) {
          val ji = jaa.values(i)
          val ki = convert(ji, into, u, from)
          if (ki ne ji) {
            same = false
            a = new Array[Json](jaa.values.length)
            while (j < i) { a(j) = jaa.values(j); j += 1 }
            a(j) = ki
            j += 1
          }
          i += 1
        }
        if (j != 0) {
          while (j < jaa.values.length) {
            a(j) = convert(jaa.values(j), into, u, from)
            j += 1
          }
          Json.Arr.All(a)
        }
        else jaa
      case x if u eq null =>
        x
      case n: Json.Num =>
        renum(n, u, from)
      case jad: Json.Arr.Dbl =>
        val a = java.util.Arrays.copyOf(jad.doubles, jad.doubles.length)
        if (from) a changeFrom u else a changeInto u
        var i = 0; while (i < a.length && jad.doubles(i) == a(i)) i += 1
        if (i == a.length) jad else Json.Arr.Dbl(a)
      case y =>
        y
    }

  // Note -- we need to go in both directions to take advantage of floating point division
  // being more accurate than multiplication by the reciprocal
  def fix(j: Json, into: UnitMap.Into = UnitMap.OnlyCustom): Json = convert(j, into, null, true)
  def unfix(j: Json, into: UnitMap.Into = UnitMap.OnlyCustom): Json = convert(j, into, null, false)
}
object UnitMap extends FromJson[UnitMap] {
  import units._

  sealed trait Into { def apply(s: String): Option[Into] }
  case object Nowhere extends Into { def apply(s: String) = None }
  case object OnlyCustom extends Into {
    lazy val asOption = Option(OnlyCustom)
    def apply(s: String) = if (s.length > 0 && s.charAt(0) == '@') asOption else None
  }
  final case class Nested(these: Map[String, Into]) extends Into { 
    def apply(s: String) = these.get(s) orElse OnlyCustom(s)
  }
  final case class Leaves(those: Set[String]) extends Into {
    def apply(s: String) = if (those contains s) OnlyCustom.asOption else OnlyCustom(s)
  }
  final class Arbitrary(pf: PartialFunction[String, Into]) extends Into {
    private val lifted = pf.lift
    def apply(s: String) = lifted(s)
  }

  val default = {
    import Standard._
    new UnitMap(
      Map(
        "t" -> second,
        "x" -> millimeter, "y" -> millimeter,
        "cx" -> millimeter, "cy" -> millimeter,
        "ox" -> millimeter, "oy" -> millimeter,
        "px" -> millimeter, "py" -> millimeter,
        "age" -> hour,
        "temperature" -> celsius,
        "humidity" -> percent,
        "diameter" -> millimeter
      ),
      Json.Obj.empty
    )
  }

  def parse(j: Json): Either[JastError, UnitMap] = j match {
    case o: Json.Obj =>
      o.filter((k,_) => !k.startsWith("@")).to[Map[String, Convert]] match {
        case Right(m) => Right(new UnitMap(m, o.filter((k, _) => k.startsWith("@"))))
        case e: Left[_, _] => e.asInstanceOf[Left[JastError, UnitMap]]
      }
    case _ => Left(JastError("Units are not a JSON object"))
  }
}
