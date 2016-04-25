package org.openworm.trackercommons

object WconImplicits {
  // Could just use kse.maths._ for this one!
  implicit class PostfixMathematics(private val x: Double) extends AnyVal {
    def finite = !(java.lang.Double.isNaN(x) || java.lang.Double.isInfinite(x))
  }
}
