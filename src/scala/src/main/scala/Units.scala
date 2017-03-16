package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

package units {
  trait Convert extends AsJson {
    def name: String
    def toInternal(x: Double): Double
    def toExternal(x: Double): Double
    def isIdentity: Boolean = false

    def json = Json.Str(name)

    def changeToInternal(ds: Array[Double]) { var i = 0; while (i < ds.length) { ds(i) = toInternal(ds(i)); i +=1 } }
    def changeToInternal(fs: Array[Float]) { var i = 0; while (i < fs.length) { fs(i) = toInternal(fs(i)).toFloat; i += 1 } }
    def changeToInternal(dss: Array[Array[Double]]) { var i = 0; while (i < dss.length) { changeToInternal(dss(i)); i += 1 } }
    def changeToInternal(fss: Array[Array[Float]]) { var i = 0; while (i < fss.length) { changeToInternal(fss(i)); i += 1 } }

    def changeToExternal(ds: Array[Double]) { var i = 0; while (i < ds.length) { ds(i) = toExternal(ds(i)); i +=1 } }
    def changeToExternal(fs: Array[Float]) { var i = 0; while (i < fs.length) { fs(i) = toExternal(fs(i)).toFloat; i += 1 } }
    def changeToExternal(dss: Array[Array[Double]]) { var i = 0; while (i < dss.length) { changeToExternal(dss(i)); i += 1 } }
    def changeToExternal(fss: Array[Array[Float]]) { var i = 0; while (i < fss.length) { changeToExternal(fss(i)); i += 1 } }
  }

  case class IdentityConvert(name: String) extends Convert {
    def toInternal(x: Double) = x
    def toExternal(x: Double) = x
    override def isIdentity = true
  }

  case class LinearConvert(name: String, one: Double, dDim: Int, tDim: Int, aDim: Int) extends Convert {
    import LinearConvert._
    def toInternal(x: Double) = x * one
    def toExternal(x: Double) = x / one
    override def isIdentity = math.abs(one - 1) < 1e-9

    override def changeToInternal(ds: Array[Double]) { 
      if (math.abs(one-1) > 1e-9) { var i = 0; while (i < ds.length) { ds(i) = ds(i) * one; i +=1 } }
    }
    override def changeToInternal(fs: Array[Float]) { 
      if (math.abs(one-1) > 1e-9) { var i = 0; while (i < fs.length) { fs(i) = (fs(i) * one).toFloat; i += 1 } }
    }

    override def changeToExternal(ds: Array[Double]) { 
      if (math.abs(one-1) > 1e-9) { var i = 0; while (i < ds.length) { ds(i) = ds(i) / one; i +=1 } }
    }
    override def changeToExternal(fs: Array[Float]) { 
      if (math.abs(one-1) > 1e-9) { var i = 0; while (i < fs.length) { fs(i) = (fs(i) / one).toFloat; i += 1 } }
    }

    def *(lc: LinearConvert) =
      new LinearConvert(s"($name) * (${lc.name})", one * lc.one, dDim + lc.dDim, tDim + lc.tDim, aDim + lc.aDim)
    def /(lc: LinearConvert) = 
      new LinearConvert(s"($name) / (${lc.name})", one / lc.one, dDim - lc.dDim, tDim - lc.tDim, aDim - lc.aDim)
    def ^(n: Int) =
      new LinearConvert(s"($name)^$n", math.pow(one, n), dDim*n, tDim*n, aDim*n)

    def e(n: Int) = copy(one = if (n >= 0) one * ("1e"+n.toString).toDouble else one / ("1e"+(-n).toString).toDouble)
  }

  object Standard {
    val millimeter = LinearConvert("mm", 1, 1, 0, 0)
    val meter = LinearConvert("m", 1e3, 1, 0, 0)
    val micron = LinearConvert("um", 1e-3, 1, 0, 0)
    val inch = LinearConvert("in", 25.4, 1, 0, 0)
    val foot = LinearConvert("ft", 304.8, 1, 0, 0)
    val second = LinearConvert("s", 1, 0, 1, 0)
    val minute = LinearConvert("min", 60, 0, 1, 0)
    val hour = LinearConvert("hr", 3600, 0, 1, 0)
    val day = LinearConvert("day", 86400, 0, 1, 0)
    val radian = LinearConvert("radian", 1, 0, 0, 1)
    val degree = LinearConvert("degree", 180/math.Pi, 0, 0, 1)
    val one = LinearConvert("", 1, 0, 0, 0)
    def scalar(x: Double) = LinearConvert(x.toString, x, 0, 0, 0)
    val percent = LinearConvert("%", 0.01, 0, 0, 0)
    val celsius = IdentityConvert("C")
    val kelvin = new Convert { def name = "K"; def toInternal(x: Double) = x - 273.15; def toExternal(x: Double) = x + 273.15 }
    val fahrenheit = new Convert { def name = "F"; def toInternal(x: Double) = ((x-32)*5)/9; def toExternal(x: Double) = (x*9)/5 + 32 }    
  }

  object ConvertParser {
    import Standard._
    import fastparse.all._
    val W = P(CharsWhile(_.isWhitespace).?)
    val D = CharIn("0123456789")
    val Dp = CharIn("123456789")
    val PositiveDouble = P(
      ( P("0." ~ "0".rep ~ Dp ~ D.rep) | 
        P(Dp ~ D.rep ~ ("." ~ D.rep(1)).?)
      ) ~ 
      (CharIn("eE") ~ CharIn("+-").? ~ D.rep(1)).?
    ).!.map(_.toDouble)
    // Be SURE to put the longer version first or it will not be matched!
    val LongLinBase = 
      P("meters" | "meter" | "metres" | "metre").map(_ => meter) |
      P("inches" | "inch").map(_ => inch) |
      P("foot" | "feet").map(_ => foot) |
      P("seconds" | "second").map(_ => second) |
      P("minutes" | "minute").map(_ => minute) |
      P("hours" | "hour").map(_ => hour) |
      P("days" | "day").map(_ => day) |
      P("radians" | "radian").map(_ => radian) |
      P("degrees" | "degree").map(_ => degree)
    val LongPrefix =
      P("centi").map(_ => -2) |
      P("milli").map(_ => -3) |
      P("micro").map(_ => -6) |
      P("nano").map(_ => -9) |
      P("kilo").map(_ => 3) |
      P("mega").map(_ => 6) |
      P("giga").map(_ => 9)
    val LongLin =
      P("micron" | "microns").map(_ => micron) |
      (LongPrefix ~ LongLinBase).map{ case (a,b) => b e a } |
      LongLinBase
    val ShortLinBase =
      P("rad").map(_ => radian) |
      P("deg").map(_ => degree) |  // Must go above "d"!
      P("s").map(_ => second) |
      P("min").map(_ => minute) |  // Must go above "m"!
      P("hr" | "h").map(_ => hour) |
      P("d").map(_ => day) |
      P("m").map(_ => meter) |
      P("in").map(_ => inch) |
      P("ft").map(_ => foot)
    val ShortPrefix =
      P("c").map(_ => -2) |
      P("m").map(_ => -3) |
      P("u" | "\u00B5" | "\u03BC").map(_ => -6) |
      P("n").map(_ => -9) |
      P("k").map(_ => 3) |
      P("M").map(_ => 6) |
      P("G").map(_ => 9)
    val ShortLin =
      P("mm").map(_ => millimeter) |
      P("%").map(_ => percent) |
      (ShortPrefix ~ ShortLinBase).map{ case (a,b) => b e a } |
      ShortLinBase
    type PLC = Parser[LinearConvert]
    val OneLin: PLC = P(PositiveDouble.map(x => if (x==1) one else scalar(x)) | LongLin | ShortLin)
    val ExpLin: PLC = P((ParLin ~ W ~ "^" ~ W ~ P("-".? ~ CharsWhile(_.isDigit)).!.map(_.toDouble.toInt)).map{ case (a,b) => a ^ b })
    val PrecA: PLC  = P(ExpLin | ParLin)
    val MulLin: PLC = P(((PrecA ~ W ~ PrecB) | (PrecA ~ W ~ "*" ~ W ~ PrecB)).map{ case (a,b) => a * b })
    val PrecB: PLC  = P(MulLin | PrecA)
    val DivLin: PLC = P((PrecB ~ W ~ "/" ~ W ~ PrecB).map{ case (a,b) => a / b })
    val PrecC: PLC  = P(DivLin | PrecB)
    val ParLin: PLC = P("(" ~ W ~ PrecC ~ W ~ ")") | OneLin
    val NonLin =
      P("celsius" | "Celsius" | "centigrade" | "Centigrade" | "C").map(_ => celsius) |
      P("fahrenheit" | "Fahrenheit" | "F").map(_ => fahrenheit) |
      P("kelvin" | "Kelvin" | "K").map(_ => kelvin)
    val Cvt = (PrecC ~ End) | (NonLin ~ End)

    def apply(s: String): Option[Convert] = Cvt.parse(s) match {
      case Parsed.Success(x, _) => Some(x)
      case _ => None
    }
  }
}

package object units {
  def parseUnit(s: String): Option[Convert] = ConvertParser(s) match {
    case Some(lc: LinearConvert) => Some(lc.copy(name = s))
    case x                       => x
  }

  implicit val parseConvertFromJson = new FromJson[Convert] {
    def parse(j: Json): Either[JastError, Convert] = j match {
      case Json.Str(text) => parseUnit(text) match {
        case Some(c) => Right(c)
        case None => Left(JastError("Could not parse unit "+text))
      }
      case _ => Left(JastError("No unit conversion: value is not a string"))
    }
  }

  implicit class FloatIntoUnits(private val underlying: Float) extends AnyVal {
    def into(u: Convert): Float = (u toExternal underlying).toFloat
    def from(u: Convert): Float = (u toInternal underlying).toFloat
  }
  implicit class ArrayFloatIntoConvert(val underlying: Array[Float]) extends AnyVal {
    def changeInto(u: Convert): underlying.type = { u changeToExternal underlying; underlying }
    def changeFrom(u: Convert): underlying.type = { u changeToInternal underlying; underlying }
  }
  implicit class ArrayArrayFloatIntoConvert(val underlying: Array[Array[Float]]) extends AnyVal {
    def changeInto(u: Convert): underlying.type = { u changeToExternal underlying; underlying }
    def changeFrom(u: Convert): underlying.type = { u changeToInternal underlying; underlying }
  }
  implicit class DoubleIntoConvert(private val underlying: Double) extends AnyVal {
    def into(u: Convert): Double = u toExternal underlying
    def from(u: Convert): Double = u toInternal underlying
  }
  implicit class ArrayDoubleIntoConvert(val underlying: Array[Double]) extends AnyVal {
    def changeInto(u: Convert): underlying.type = { u changeToExternal underlying; underlying }
    def changeFrom(u: Convert): underlying.type = { u changeToInternal underlying; underlying }
  }
  implicit class ArrayArrayDoubleIntoConvert(val underlying: Array[Array[Double]]) extends AnyVal {
    def changeInto(u: Convert): underlying.type = { u changeToExternal underlying; underlying }
    def changeFrom(u: Convert): underlying.type = { u changeToInternal underlying; underlying }
  }
}
