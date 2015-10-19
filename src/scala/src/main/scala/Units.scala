package org.openworm.trackercommons

class Units {
  private[this] val myKnown = collection.mutable.AnyRefMap.empty[String,String]
  def known: collection.Map[String, String] = myKnown
  def +=(name: String, unit: String): Boolean = {
    if (myKnown contains name) false
    else {
      myKnown += (name, unit)
      true
    }
  }
  def convert(name: String, unit: String): Units.Converter = myKnown.get(name) match {
    case None => throw new Exception("Numeric quantity " + name + " not known.")
    case Some(u) => Units.canonical(unit) match {
      case None => throw new Exception("Unit " + unit + " not known.")
      case Some(v) => if (u == v) Units.IdentityConverter else Units.converter(u, v)
    }
  }
}

object Units {
  abstract class Converter extends (Double => Double) { self =>
    def o(c: Converter) = new Converter { def apply(x: Double) = c(self(x)) }
    def fix(xs: Array[Double]) { var i = 0; while (i < xs.length) { xs(i) = apply(xs(i)); i += 1 } }
    def fix(xs: Array[Float]) { var i = 0; while (i < xs.length) { xs(i) = apply(xs(i)).toFloat; i += 1 } }
    def fix(xss: Array[Array[Double]]) {
      var i = 0
      while (i < xss.length) {
        val xs = xss(i)
        var j = 0
        while (j < xs.length) {
          xs(j) = apply(xs(j))
          j += 1
        }
        i += 1
      }
    }
    def fix(xss: Array[Array[Float]]) {
      var i = 0
      while (i < xss.length) {
        val xs = xss(i)
        var j = 0
        while (j < xs.length) {
          xs(j) = apply(xs(j)).toFloat
          j += 1
        }
        i += 1
      }
    }
  }

  object IdentityConverter extends Converter {
    def apply(d: Double) = d
    override def fix(xs: Array[Double]) {}
    override def fix(xs: Array[Float]) {}
    override def fix(xss: Array[Array[Double]]) {}
    override def fix(xss: Array[Array[Float]]) {}
  }

  case class RatL(num: Long, den: Long) {
    def ten = if (den >= 10 && (den % 10)==0) new RatL(num, den/10) else RatL(num*10, den)
    def tenth = if (num >= 10 && (num % 10)==0) new RatL(num/10, den) else RatL(den, num*10)
  }
  object RatL { val One = RatL(1,1) }

  val knownUnits = Map(
    "meter" -> 0,
    "meters" -> 0,
    "m" -> 0,
    "foot" -> 1,
    "feet" -> 1,
    "ft" -> 1,
    "inch" -> 2,
    "inches" -> 2,
    "micron" -> 3,
    "microns" -> 3,
    "s" -> 10,
    "sec" -> 10,
    "second" -> 10,
    "seconds" -> 10,
    "min" -> 11,
    "minute" -> 11,
    "minutes" -> 11,
    "h" -> 12,
    "hour" -> 12,
    "hours" -> 12,
    "d" -> 13,
    "day" -> 13,
    "days" -> 13,
    "C" -> 20,
    "Celsius" -> 20,
    "celsius" -> 20,
    "F" -> 21,
    "fahrenheit" -> 21,
    "Fahrenheit" -> 21,
    "" -> 30,
    "%" -> 31,
    "percent" -> 31
  )

  val lengthConversions = Array(
    Array(RatL.One, RatL(10000, 3048), RatL(10000, 254), RatL(1000000, 1)),
    Array(RatL(3048, 10000), RatL.One, RatL(12, 1), RatL(304800, 1)),
    Array(RatL(254, 10000), RatL(1, 12), RatL.One, RatL(25400, 1)),
    Array(RatL(1, 1000000), RatL(1, 304800), RatL(1, 25400), RatL.One)
  )

  val timeConversions = Array(
    Array(RatL.One, RatL(60, 1), RatL(3600, 1), RatL(86400, 1)),
    Array(RatL(1, 60), RatL.One, RatL(60, 1), RatL(1440, 1)),
    Array(RatL(1, 3600), RatL(1, 60), RatL.One, RatL(24, 1)),
    Array(RatL(1, 86400), RatL(1, 3600), RatL(1, 60), RatL.One)
  )

  object FtoC extends Converter {
    def apply(x: Double) = ((x-32)*5)/9
  }

  object CtoF extends Converter {
    def apply(x: Double) = (x*9)/5 + 32
  }

  def canonical(unit: String): Option[String] = ???

  def converter(u: String, v: String): Converter = ???
}
