package org.openworm.trackercommons

object WconImplicits {
  // Could just use kse.maths._ for this one!
  implicit class PostfixMathematics(private val x: Double) extends AnyVal {
    def finite = !(java.lang.Double.isNaN(x) || java.lang.Double.isInfinite(x))
  }

  // Could just use kse.flow_ for this one!
  implicit class TapAnything[A](private val a: A) extends AnyVal {
    def tap[Z](f: A => Z): A = { f(a); a }
  }
}
