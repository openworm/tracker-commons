package org.openworm.trackercommons


package units {
  trait Units { self =>
    def kind: String
    def name: String

    def of(value: Float): Float = of(value.toDouble).toFloat
    def of(value: Double): Double
    def fix(xs: Array[Float]) { var i = 0; while (i < xs.length) { xs(i) = of(xs(i)); i += 1 } }
    def fix(xs: Array[Double]) { var i = 0; while (i < xs.length) { xs(i) = of(xs(i)); i += 1 } }
    def fix(xss: Array[Array[Float]]) { var i = 0; while (i < xss.length) { fix(xss(i)); i += 1 } }
    def fix(xss: Array[Array[Double]]) { var i = 0; while (i < xss.length) { fix(xss(i)); i += 1 } }

    def from(value: Float): Float = from(value.toDouble).toFloat
    def from(value: Double): Double
    def unfix(xs: Array[Float]) { var i = 0; while (i < xs.length) { xs(i) = from(xs(i)); i += 1 } }
    def unfix(xs: Array[Double]) { var i = 0; while (i < xs.length) { xs(i) = from(xs(i)); i += 1 } }
    def unfix(xss: Array[Array[Float]]) { var i = 0; while (i < xss.length) { unfix(xss(i)); i += 1 } }
    def unfix(xss: Array[Array[Double]]) { var i = 0; while (i < xss.length) { unfix(xss(i)); i += 1 } }

    def roundedTo(digits: Int): Units =
      if (digits < 0 || digits > 9) throw new IllegalArgumentException("Can only round to 0-9 digits")
      else new Units {
        def kind = self.kind
        def name = self.name
        private def rounded(raw: Double): Double =
          if (raw < 1e9) math.rint(raw * Units.shiftLeftBy(digits)).toLong * Units.shiftRightBy(digits)
          else raw
        def of(value: Double) = rounded(self of value)
        def from(value: Double) = rounded(self from value)
    }
  }
  object Units {
    private[this] val myCachedSingletons = new java.util.concurrent.ConcurrentHashMap[String, Units]
    
    val shiftLeftBy = (0 to 15).map(i => ("1e"+i).toDouble).toArray
    val shiftRightBy = (0 to 15).map(i => ("1e-"+i).toDouble).toArray

    def apply(uname: String) = Option(myCachedSingletons.get(uname)).orElse {
      for { u <- NamedUnits get uname } yield {
        myCachedSingletons  put (uname, u)
        u
      }
    }

  }

  trait Distance extends Units { final def kind = "distance (mm)" }
  class Meters(val name: String, prefix: Int = 0) extends Distance {
    private[this] val to_mm = ("1e"+(prefix+3)).toDouble
    private[this] val from_mm = ("1e"+(-prefix-3)).toDouble
    def of(value: Double) = value * to_mm
    def from(value: Double) = value * from_mm
  }
  object Meters extends Meters("m", 0) {}
  object Millimeters extends Meters("mm", -3) {}
  object Microns extends Meters("um", -6) {}
  class Inches(val name: String, prefix: Int = 0) extends Distance {
    private[this] val to_mm = ("25.4e"+prefix).toDouble
    private[this] val from_mm = (("1e"+(-prefix+1)).toDouble/254)
    def of(value: Double) = value * to_mm
    def from(value: Double) = value * from_mm
  }
  object Inches extends Inches("in", 0)
  class Feet(val name: String, prefix: Int = 0) extends Distance {
    private[this] val to_mm = ("304.8e"+prefix).toDouble
    private[this] val from_mm = (("1e"+(-prefix+1)).toDouble/3048)
    def of(value: Double) = value * to_mm
    def from(value: Double) = value * from_mm
  }
  object Feet extends Feet("ft", 0)

  trait Time extends Units { final def kind = "time (s)" }
  class Seconds(val name: String, prefix: Int = 0) extends Time {
    private[this] val to_s = ("1e"+prefix).toDouble
    private[this] val from_s = ("1e"+(-prefix)).toDouble
    def of(value: Double) = value * to_s
    def from(value: Double) = value * from_s
  }
  object Seconds extends Seconds("s", 0) {}
  object Milliseconds extends Seconds("ms", -3) {}
  object Microseconds extends Seconds("us", -6) {}
  class Minutes(val name: String, prefix: Int = 0) extends Time {
    private[this] val to_s = ("6e"+(prefix+1)).toDouble
    private[this] val from_s = ("1e"+(-prefix-1)).toDouble / 6
    def of(value: Double) = value * to_s
    def from(value: Double) = value * from_s
  }
  object Minutes extends Minutes("min", 0) {}
  class Hours(val name: String, prefix: Int = 0) extends Time {
    private[this] val to_s = ("36e"+(prefix+2)).toDouble
    private[this] val from_s = ("1e"+(-prefix-2)).toDouble / 36
    def of(value: Double) = value * to_s
    def from(value: Double) = value * from_s
  }
  object Hours extends Hours("h", 0) {}
  class Days(val name: String, prefix: Int = 0) extends Time {
    private[this] val to_s = ("864e"+(prefix+2)).toDouble
    private[this] val from_s = ("1e"+(-prefix-2)).toDouble / 864
    def of(value: Double) = value * to_s
    def from(value: Double) = value * from_s
  }
  object Days extends Days("d", 0) {}

  trait Dimensionless extends Units { final def kind = "(dimensionless)" }
  object Whole extends Dimensionless {
    def name = ""
    def of(value: Double) = value
    def from(value: Double) = value
  }
  class Percent(val name: String) extends Dimensionless {
    def of(value: Double) = value/100
    def from(value: Double) = 100*value
  }
  object Percent extends Percent("%") {}

  trait Temperature extends Units { final def kind = "temperature (C)"}
  class Celsius(val name: String) extends Temperature {
    def of(value: Double) = value
    def from(value: Double) = value
  }
  object Celsius extends Celsius("C") {}
  class Fahrenheit(val name: String) extends Temperature {
    def of(value: Double) = (value - 32)*5/9
    def from(value: Double) = (value*9)/5 + 32
  }
  object Fahrenheit extends Fahrenheit("F") {}


  abstract class Converter(val fromName: String, val toName: String) {
    final def apply(x: Float): Float = change(x)
    def change(x: Float): Float
    def change(xs: Array[Float]) { var i = 0; while (i < xs.length) { xs(i) = change(xs(i)); i += 1 } }
    def change(xss: Array[Array[Float]]) { var i = 0; while (i < xss.length) { change(xss(i)); i += 1 } }
    final def apply(x: Double): Double = change(x)
    def change(x: Double): Double
    def change(xs: Array[Double]) { var i = 0; while (i < xs.length) { xs(i) = change(xs(i)); i += 1 } }
    def change(xss: Array[Array[Double]]) { var i = 0; while (i < xss.length) { change(xss(i)); i += 1 } }
  }
  final class IdentityConverter(_fromName: String, _toName: String) extends Converter(_fromName, _toName) {
    def change(x: Float) = x
    override def change(xs: Array[Float]) {}
    override def change(xss: Array[Array[Float]]) {}
    def change(x: Double) = x
    override def change(xs: Array[Double]) {}
    override def change(xss: Array[Array[Double]]) {}
  }
  final class InConverter(_fromName: String, _toName: String, in: Units) extends Converter(_fromName, _toName) {
    def change(x: Float) = in of x
    override def change(xs: Array[Float]) { in fix xs }
    override def change(xss: Array[Array[Float]]) { in fix xss }
    def change(x: Double) = in of x
    override def change(xs: Array[Double]) { in fix xs }
    override def change(xss: Array[Array[Double]]) { in fix xss }
  }
  final class OutConverter(_fromName: String, _toName: String, out: Units) extends Converter(_fromName, _toName) {
    def change(x: Float) = out from x
    override def change(xs: Array[Float]) { out unfix xs }
    override def change(xss: Array[Array[Float]]) { out unfix xss }
    def change(x: Double) = out from x
    override def change(xs: Array[Double]) { out unfix xs }
    override def change(xss: Array[Array[Double]]) { out unfix xss }
  }
  final class InOutConverter(_fromName: String, _toName: String, in: Units, out: Units) extends Converter(_fromName, _toName) {
    def change(x: Float) = out from (in of x)
    def change(x: Double) = out from (in of x)
  }
}

package object units {
  implicit class FloatIntoUnits(private val underlying: Float) extends AnyVal {
    def into(u: Units): Float = u from underlying
    def was(u: Units): Float = u of underlying
  }
  implicit class ArrayFloatIntoUnits(val underlying: Array[Float]) extends AnyVal {
    def into(u: Units): underlying.type = { u unfix underlying; underlying }
    def was(u: Units): underlying.type = { u fix underlying; underlying }
  }
  implicit class ArrayArrayFloatIntoUnits(val underlying: Array[Array[Float]]) extends AnyVal {
    def into(u: Units): underlying.type = { u unfix underlying; underlying }
    def was(u: Units): underlying.type = { u fix underlying; underlying }
  }
  implicit class DoubleIntoUnits(private val underlying: Double) extends AnyVal {
    def into(u: Units): Double = u from underlying
    def was(u: Units): Double = u of underlying
  }
  implicit class ArrayDoubleIntoUnits(val underlying: Array[Double]) extends AnyVal {
    def into(u: Units): underlying.type = { u unfix underlying; underlying }
    def was(u: Units): underlying.type = { u fix underlying; underlying }
  }
  implicit class ArrayArrayDoubleIntoUnits(val underlying: Array[Array[Double]]) extends AnyVal {
    def into(u: Units): underlying.type = { u unfix underlying; underlying }
    def was(u: Units): underlying.type = { u fix underlying; underlying }
  }

  val SiShortPrefixes = Map(
    "" -> 0,
    "c" -> -2,
    "m" -> -3,
    "u" -> -6,
    "\u00B5" -> -6,
    "\u03BC" -> -6,
    "n" -> -9,
    "k" -> 3,
    "M"-> 6,
    "G" -> 9
  )

  val SiShortPrefixByShift = Map(
    0 -> "",
    -2 -> "c",
    -3 -> "m",
    -6 -> "u",
    -9 -> "n",
    3 -> "k",
    6 -> "M",
    9 -> "G"
  )

  val SiLongPrefixes = Map(
    "" -> 0,
    "centi" -> -2,
    "milli" -> -3,
    "micro" -> -6,
    "nano" -> -9,
    "kilo" -> 3,
    "mega" -> 6,
    "giga" -> 9
  )

  val SiLongPrefixByShift = Map(
    0 -> "",
    -2 -> "centi",
    -3 -> "milli",
    -6 -> "micro",
    -9 -> "nano",
    3 -> "kilo",
    6 -> "mega",
    9 -> "giga"
  )

  val NamedDistances = (
    (for { base <- Seq("meter", "meters", "metre", "metres"); (prefix, shift) <- SiLongPrefixes } yield new Meters(prefix + base, shift)) ++
    (for { base <- Seq("m"); (prefix, shift) <- SiShortPrefixes } yield new Meters(prefix + base, shift)) ++
    (for { base <- Seq("inch", "inches"); (prefix, shift) <- SiLongPrefixes } yield new Inches(prefix + base, shift)) ++
    (Seq(new Inches("in", 0))) ++
    (for { base <- Seq("foot", "feet"); (prefix, shift) <- SiLongPrefixes } yield new Feet(prefix + base, shift)) ++
    (Seq(new Feet("ft", 0)))
  ).map(u => u.name -> u).toMap

  val NamedTimes = (
    (for { base <- Seq("second", "seconds") ; (prefix, shift) <- SiLongPrefixes } yield new Seconds(prefix + base, shift )) ++
    (for { base <- Seq("s", "sec") ; (prefix, shift) <- SiShortPrefixes } yield new Seconds(prefix + base, shift)) ++
    (for { base <- Seq("minute", "minutes") ; (prefix, shift) <- SiLongPrefixes } yield new Minutes(prefix + base, shift )) ++
    (for { base <- Seq("min"); (prefix, shift) <- SiShortPrefixes } yield new Minutes(prefix + base, shift)) ++
    (for { base <- Seq("hour", "hours") ; (prefix, shift) <- SiLongPrefixes } yield new Hours(prefix + base, shift )) ++
    (for { base <- Seq("h"); (prefix, shift) <- SiShortPrefixes } yield new Hours(prefix + base, shift)) ++
    (for { base <- Seq("day", "days") ; (prefix, shift) <- SiLongPrefixes } yield new Days(prefix + base, shift )) ++
    (for { base <- Seq("d"); (prefix, shift) <- SiShortPrefixes } yield new Days(prefix + base, shift))
  ).map(u => u.name -> u).toMap

  val NamedDimensionless = (
    Seq(Whole) ++
    Seq("%", "percent").map(name => new Percent(name))
  ).map(u => u.name -> u).toMap

  val NamedTemperatures = (
    Seq("Celsius", "Centigrade", "C").map(name => new Celsius(name)) ++
    Seq("Fahrenheit", "F").map(name => new Fahrenheit(name))
  ).map(u => u.name -> u).toMap

  val NamedUnits = NamedDistances ++ NamedTimes ++ NamedDimensionless ++ NamedTemperatures

  private[this] val myCachedPairs = new java.util.concurrent.ConcurrentHashMap[(String, String), Converter]

  def converter(uname: String, vname: String): Option[Converter] = Option(myCachedPairs.get((uname, vname))).orElse(
    for { u <- NamedUnits get uname; v <- NamedUnits get vname } yield {
      val cvt =
        if (u.name == v.name || ((u of 0) == (v of 0) && (u of 1) == (v of 1))) new IdentityConverter(u.name, v.name)
        else if ((u of 0) == 0 && (u of 1) == 1) new OutConverter(u.name, v.name, v)
        else if ((v of 0) == 0 && (v of 1) == 1) new InConverter(u.name, v.name, u)
        else new InOutConverter(u.name, v.name, u, v)
      myCachedPairs.put((uname, vname), cvt)
      cvt
    }
  )
}
