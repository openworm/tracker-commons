package org.openworm.trackercommons.json

import fastparse.all._

class Indented(val text: String) {
  private var myIndent: Int = 0
  def indent = myIndent
  def indent_=(i: Int) { myIndent = math.max(i, 0) }
  def textFn(f: String => String) = { val ans = new Indented(f(text)); ans.indent = myIndent; ans }
  def in(i: Int): this.type = { indent = indent + i; this }
  override def toString = 
    if (myIndent < 1) text
    else if (myIndent < 100) Indented.spaces(myIndent) + text
    else " "*myIndent + text
}
object Indented {
  def apply(text: String) = new Indented(text)
  val spaces = (0 until 100).map(i => " "*i).toVector
}

trait Jsonable {
  def toObjJ: ObjJ
}
trait Jsonic[A] {
  def from(ob: ObjJ): Either[String, A]
}

trait JsonOut {
  def toJson: String
  def toJsons: Vector[Indented] = Vector(Indented(toJson))
}

sealed trait JSON extends JsonOut { def kind: String }

case object NullJ extends JSON { def kind = "null"; def toJson = "null" }

sealed trait BoolJ extends JSON { final def kind = "bool"; def value: Boolean }
case object TrueJ extends BoolJ { 
  def value = true
  def toJson = "true" 
}
case object FalseJ extends BoolJ {
  def value = false
  def toJson = "false" 
}

case class StrJ(value: String) extends JSON {
  final def kind = "str"
  def toJson: String = {
    var n = 0
    var i = 0
    while (i < value.length) {
      val c = value.charAt(i)
      if (c >= 32 && c < 127)  { 
        if (c != '"' && c != '\\') n += 1
        else n += 2
      }
      else if (c == '\b' || c == '\f' || c == '\n' || c == '\r' || c == '\t') n += 2
      else n += 6
      i += 1
    }
    if (n == i) "\"" + value + "\""
    else {
      var ac = new Array[Char](n+2)
      i = 0
      n = 1
      ac(0) = '"'
      while (i < value.length) {
        val c = value.charAt(i)
        if (c >= 32 && c < 127) {
          if (c != '"' && c != '\\') { ac(n) = c; n += 1 }
          else { ac(n) = '\\'; ac(n+1) = c; n += 2 }
        }
        else {
          ac(n) = '\\'
          c match {
            case '\b' => ac(n+1) = 'b'; n += 2
            case '\f' => ac(n+1) = 'f'; n += 2
            case '\n' => ac(n+1) = 'n'; n += 2
            case '\r' => ac(n+1) = 'r'; n += 2
            case '\t' => ac(n+1) = 't'; n += 2
            case _ =>
              ac(n+1) = 'u'
              ac(n+2) = (c >> 12) match { case x if x < 10 => ('0' + x).toChar; case x => ('A' + (x-10)).toChar }
              ac(n+3) = ((c >> 8) & 0xF) match { case x if x < 10 => ('0' + x).toChar; case x => ('A' + (x-10)).toChar }
              ac(n+4) = ((c >> 4) & 0xF) match { case x if x < 10 => ('0' + x).toChar; case x => ('A' + (x-10)).toChar }
              ac(n+5) = (c & 0xF) match { case x if x < 10 => ('0' + x).toChar; case x => ('A' + (x-10)).toChar }
              n += 6
          }
        }
        i += 1
      }
      ac(n) = '"'
      new String(ac)
    }
  }
}

case class NumJ(var value: Double) extends JSON {
  final def kind = "num"
  // Note--value is var to support unit conversion!
  def toJson = Dbl.toJson(value)
}

case class ANumJ(values: Array[Double]) extends JSON {
  final def kind = "num[]"
  def length = values.length
  def toJson = values.map(value => Dbl.toJson(value)).mkString("[", ", ", "]")
  override def toJsons =
    if (values.length < 20) Vector(Indented(toJson))
    else {
      val vb = Vector.newBuilder[Indented]
      vb += Indented("[")
      var i = 0
      while (i < values.length) {
        var k = i+20
        var sb = new StringBuilder
        while (i < k && i < values.length) {
          val vi = values(i)
          sb ++= Dbl.toJson(vi)
          if (i+1 < values.length) sb ++= ", "
          i += 1
        }
        if (k-i < 20) vb += Indented(sb.result()).in(2)
      }
      vb += Indented("]")
      vb.result()
    }
}
object ANumJ {
  def empty = new ANumJ(emptyAD)
  val emptyAD = new Array[Double](0)
  val emptyAF = new Array[Float](0)
}

case class AANumJ(valuess: Array[Array[Double]]) extends JSON {
  final def kind = "num[][]"
  def length = valuess.length
  def toJson = valuess.map(values => ANumJ(values).toJson).mkString("[ ", ", ", " ]")
  override def toJsons = {
    val vb = Vector.newBuilder[Indented]
    vb += Indented("[")
    var it = valuess.iterator
    while (it.hasNext) {
      val s = ANumJ(it.next).toJson
      vb += Indented(if (it.hasNext) s + "," else s).in(2)
    }
    vb += Indented("]")
    vb.result()
  }
}
object AANumJ {
  def empty = new AANumJ(emptyAAD)
  val emptyAAD = new Array[Array[Double]](0)
  val emptyAAF = new Array[Array[Float]](0)
}

case class ArrJ(values: Array[JSON]) extends JSON {
  final def kind = "arr"
  def length = values.length
  def toJson = values.map(_.toJson).mkString("[ ", ", ", "]")
  override def toJsons: Vector[Indented] = {
    if (values.length < 2) {
      val j = toJson
      if (j.length < 120) return Vector(Indented(toJson))
    }
    val vb = Vector.newBuilder[Indented]
    vb += Indented("[")
    val it = values.iterator
    while (it.hasNext) {
      val jt = it.next.toJsons.iterator
      while (jt.hasNext) {
        val x = jt.next.in(2)
        if (!jt.hasNext && it.hasNext) vb += x.textFn(_ + ",") else vb += x
      }
    }
    vb += Indented("]")
    vb.result()
  }
}
object ArrJ {
  def empty = new ArrJ(emptyAJ)
  val emptyAJ = new Array[JSON](0)
}

case class ObjJ(keyvals: Map[String, List[JSON]]) extends JSON {
  final def kind = "obj"
  def isEmpty = keyvals.isEmpty
  def toJson = keyvals.toVector.sortBy(_._1).flatMap{ case (k,vs) => vs.map(v => "\"" + k + "\": " + v.toJson) }.mkString("{ ", ", ", " }")
  override def toJsons = {
    if (
      keyvals.iterator.
      flatMap{ case (k,vs) =>
        val kn = k.length
        vs.iterator.map(_ match { 
          case NullJ | _: BoolJ | _: NumJ => 5L + kn
          case s: StrJ => s.value.length.toLong + kn
          case x: ANumJ => 5L*x.values.length + kn
          case _ => 1000L
        })
      }.
      dropWhile(_ < 100).
      isEmpty
    ) {
      Vector(Indented(toJson))
    }
    else {
      val vb = Vector.newBuilder[Indented]
      vb += Indented("{")
      val it = keyvals.toVector.sortBy(_._1).iterator.flatMap{ case (k,vs) => vs.iterator.map{ k -> _ } }
      while (it.hasNext) {
        val (k,v) = it.next
        val jt = v.toJsons.iterator
        val h = (if (jt.hasNext) jt.next.text else "null")
        vb += Indented(StrJ(k).toJson + ": " + (if (jt.hasNext || !it.hasNext) h else h + ",")).in(2)
        while (jt.hasNext) {
          val jx = jt.next
          jx.in(2)
          vb += (if (jt.hasNext || !it.hasNext) jx else jx.textFn(_ + ","))
        }
      }
      vb += Indented("}")
      vb.result()
    }
  }
}
object ObjJ {
  def empty = new ObjJ(Map.empty[String, List[JSON]])
}

case class TextJ(unparsed: String) extends JSON {
  final def kind = "txt"
  def toJson = unparsed
}

object Parts {
  val White = P( CharsWhile(_.isWhitespace) )
  def W[A](p: P[A]): P[A] = White.? ~ p ~ White.?
  val Hex = CharIn("0123456789ABCDEFabcdef")
  val Digit = CharIn("0123456789")
  val Digits = CharsWhile(c => c >= '0' && c <= '9')
}

object Text {
  import Parts._

  val Null = P("null")

  val Bool = "true" | "false"

  val Str = P(
    "\"" ~
      ( CharsWhile(c => c != '"' && c != '\\') |
        ("\\" ~ (("u" ~ Hex ~ Hex ~ Hex ~ Hex) | CharIn("\"\\/bfnrt"))
        )
      ).rep ~
    "\""
  )

  val Num = ("-".? ~ ("0" ~! Pass | Digits) ~! ("." ~ Digits).? ~ (CharIn("eE") ~ CharIn("+-").? ~ Digits).?)

  val Arr = P( "[" ~ W(Val.rep(sep = W("," ~! Pass))) ~! W("]") )

  val KeyVal = P( Str ~ W(":") ~! Val )
  val Obj = P( "{" ~ W(KeyVal.rep(sep = W("," ~! Pass))) ~! W("}") )

  val Val: P[Unit] = P( Obj | Arr | Str | Num | Bool | Null )
  val All = W(Val.!)
}

object Struct {
  import Parts._

  val Null = Text.Null.map(_ => NullJ)
  val Bool = P("true").map(_ => TrueJ) | P("false").map(_ => FalseJ)

  val Esc = "\\" ~ (
    "u" ~ (Hex ~ Hex ~ Hex ~ Hex).!.map(h4 => java.lang.Integer.parseInt(h4, 16).toChar.toString) |
    CharIn("bfnrt/\\\"").!.map{ case "b" => "\b"; case "f" => "\f"; case "n" => "\n"; case "r" => "\r"; case "t" => "\t"; case x => x }
  )
  val Str = "\"" ~ (CharsWhile(c => c != '"' && c != '\\').! | Esc).rep.map(xs => StrJ(xs.mkString)) ~ "\""

  val Dbl = 
    Text.Num.!.map(_.toDouble) |
    Null.map(_ => Double.NaN) |                   // Recommended--use null!
    IgnoreCase("\"nan\"").map(_ => Double.NaN) |  // Not recommended, but valid JSON
    IgnoreCase("nan").!.map(_ => Double.NaN) |    // Not even valid JSON, don't do this
    (IgnoreCase("\"inf") ~ IgnoreCase("inity").? ~ "\"").map(_ => Double.PositiveInfinity) |   // Not recommended, but valid JSON
    (IgnoreCase("inf") ~ IgnoreCase("inity").?).map(_ => Double.PositiveInfinity) |            // Not even valid JSON, don't do this
    (IgnoreCase("\"-inf") ~ IgnoreCase("inity").? ~ "\"").map(_ => Double.NegativeInfinity) |  // Not recommended, but valid JSON
    (IgnoreCase("-inf") ~ IgnoreCase("inity").?).map(_ => Double.NegativeInfinity)             // Not even valid JSON, don't do this

  val Num = Dbl.map(x => NumJ(x))

  val ADbl = "[" ~ W(Dbl).rep(sep = "," ~! Pass).map(_.toArray) ~! W("]")

  val ANum = ADbl.map(x => ANumJ(x))

  val AADbl = "[" ~ W(ADbl).rep(1, sep = "," ~! Pass).map(_.toArray) ~! W("]")

  val AANum = AADbl.map(x => AANumJ(x))

  val Arr = P("[" ~ All.rep(sep = "," ~! Pass).map(x => ArrJ(x.toArray)) ~! W("]"))

  val KeyVal = P(W(Str.map(_.value) ~! W(":") ~! Val))
  val Obj = "{" ~ KeyVal.rep(sep = "," ~! Pass).map(x => ObjJ(x.groupBy(_._1).map{ case (k,xs) => k -> xs.map(_._2).toList })) ~! W("}")

  val Val: P[JSON] = P( Obj | NoCut(ANum) | NoCut(AANum) | Arr | Str | Num | Bool | Null )

  val All = W(Val)
}

object Dbl {
  private val someNaN = Some(Double.NaN)
  private val somePosInf = Some(Double.PositiveInfinity)
  private val someNegInf = Some(Double.NegativeInfinity)

  def toJson(d: Double): String =
    if (d.isNaN || d.isInfinite) "null"
    else if (d == 0) "0"
    else math.abs(d) match {
      case x if x > Long.MaxValue => "%e".format(d)
      case x if x > 1e7 => d.toLong.toString
      case x if x > 10 => "%.3f".format(d)
      case x if x > 1 => "%.4f".format(d)
      case x if x > 0.1 => "%.5f".format(d)
      case x if x > 1e-3  => "%.7f".format(d)
      case x if x > 1e-5 => "%.9f".format(d)
      case x => "%7.4e".format(d)
    }

  def unapply(j: JSON): Option[Double] = j match {
    case NumJ(d) => Some(d)
    case NullJ => someNaN
    case ANumJ(ds) if ds.length == 1 => Some(ds(0))
    case StrJ(s) => s.toLowerCase match {
      case "nan" => someNaN
      case "inf" => somePosInf
      case "-inf" => someNegInf
      case "infinity" => somePosInf
      case "-infinity" => someNegInf
      case _ => None
    }
    case _ => None
  }
}
