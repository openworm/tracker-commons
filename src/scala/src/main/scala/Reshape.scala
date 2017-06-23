package org.openworm.trackercommons

import kse.jsonal._

/** Class to perform arbitrary reshaping of array-like data.
  */
final class Reshape protected (val elements: Array[Int], val sizes: Array[Int]) {
  def array(i: Int) = elements(2*i)
  def index(i: Int) = elements(2*i+1)
  def length = elements.length/2

  def forallA(p: Int => Boolean) = {
    var all = true
    var i = 0
    while (all && i+1 < elements.length) { all = p(elements(i)); i += 2 }
    all
  }
  def forallI(p: Int => Boolean) = {
    var all = true
    var i = 0
    while (all && i+1 < elements.length) { all = p(elements(i+1)); i += 2 }
    all
  }

  def apply(fss: Array[Array[Float]]): Array[Float] = {
    val a = new Array[Float](elements.length/2)
    var i = 0; while (i < a.length) { a(i) = fss(elements(2*i))(elements(2*i+1)); i += 1 }
    a
  }
  def apply(dss: Array[Array[Double]]): Array[Double] = {
    val a = new Array[Double](elements.length/2)
    var i = 0; while (i < a.length) { a(i) = dss(elements(2*i))(elements(2*i+1)); i += 1 }
    a
  }
  def apply[A: reflect.ClassTag](xss: Array[Array[A]]): Array[A] = {
    val a = new Array[A](elements.length/2)
    var i = 0; while (i < a.length) { a(i) = xss(elements(2*i))(elements(2*i+1)); i += 1 }
    a
  }
}
object Reshape {
  class SizeMismatchException(message: String) extends RuntimeException(message) {}

  def single(index: Int, max: Int) = new Reshape(Array(0, index), Array(max))
  
  def direct(indices: Array[Int], max: Int) = {
    val as = new Array[Int](indices.length * 2)
    var i = 0
    while (i < indices.length) {
      as(2*i) = 0
      as(2*i+1) = indices(i)
      i += 1
    }
    new Reshape(as, Array(max))
  }

  def unchanged(n: Int) = {
    val as = new Array[Int](2*n)
    var i = 0
    while (i < n) {
      as(2*i) = 0
      as(2*i+1) = i
    }
    new Reshape(as, Array(n))
  }

  def select(indicator: Array[Boolean]) = {
    val count = {
      var i, n = 0
      while (i < indicator.length) { if (indicator(i)) n += 1; i += 1 }
      n
    }
    val elements = {
      var a = new Array[Int](2*count)
      var i, j = 0
      while (i < indicator.length) { if (indicator(i)) { a(j) = 0; j += 1; a(j) = i; j += 1 }; i += 1 }
      a
    }
    new Reshape(elements, Array(indicator.length))
  }

  def sort(values: Array[Double], deduplicate: Boolean = false) = {
    val indices = Array.range(0, values.length).sortBy(i => values(i))
    var n = indices.length
    while (n > 0 && java.lang.Double.isNaN(values(indices(n-1)))) n -= 1
    if (deduplicate) {
      var i = 1
      var j = 0
      while (i < n) {
        if (values(indices(i)) == values(indices(j))) {
          if (indices(i) < indices(j)) indices(j) = indices(i)
        }
        else {
          j += 1
          if (j < i) indices(j) = indices(i)
        }
        i += 1
      }
      n = j + 1
    }
    new Reshape(if (n < indices.length) java.util.Arrays.copyOf(indices, n) else indices, Array(values.length))
  }

  def filtersort(values: Array[Double], p: Int => Boolean, deduplicate: Boolean = false) = {
    var filtered = new Array[Double](if (values.length <= 16) values.length else 8)
    var i, j = 0
    while (i < values.length) {
      val vi = values(i)
      if (!java.lang.Double.isNaN(vi) && p(i)) {
        if (j >= filtered.length) {
          val m = (if (values.length - filtered.length < filtered.length) values.length else filtered.length*2)
          filtered = java.util.Arrays.copyOf(filtered, m)
        }
        filtered(j) = vi
        j += 1
      }
      i += 1
    }
    val sorted = sort(if (j < values.length) java.util.Arrays.copyOf(filtered, j) else values, deduplicate)
    if (j < values.length) new Reshape(sorted.elements, Array(values.length)) else sorted
  }

  def directSet(participants: Array[Int], elements: Array[Int], sizes: Array[Int]): Reshape = {
    if (participants.length != elements.length)
      throw new IllegalArgumentException(f"Nonmatching lengths for join indices: ${participants.length} != ${elements.length}")
    val as = new Array[Int](2*participants.length)
    var i = 0
    while (i < participants.length) {
      as(2*i) = participants(i)
      as(2*i+1) = elements(i)
      i += 1
    }
    new Reshape(as, if (sizes.length > 0) java.util.Arrays.copyOf(sizes, sizes.length) else sizes)
  }

  def directSet(indices: Array[(Int, Int)], sizes: Array[Int]): Reshape = {
    val as = new Array[Int](indices.length*2)
    var i = 0
    while (i < indices.length) {
      val ii = indices(i)
      as(2*i) = ii._1
      as(2*i+1) = ii._2
      i += 1
    }
    new Reshape(as, if (sizes.length > 0) java.util.Arrays.copyOf(sizes, sizes.length) else sizes)
  }

  def concatSet(ns: Array[Int]) = {
    var sum = 0
    var i = 0; while (i < ns.length) { val ni = ns(i); if (ni > 0) sum += ni; i += 1 }
    val as = new Array[Int](2*sum)
    i = 0
    var k = 0
    while (i < ns.length) {
      val ni = ns(i)
      var j = 0
      while (j < ni) {
        as(k) = i; k += 1
        as(k) = j; k += 1
        j += 1
      }
      i += 1
    }
    new Reshape(as, java.util.Arrays.copyOf(ns, ns.length))
  }

  def selectSet(indicators: Array[Array[Boolean]]) = {
    val count = {
      var i, n = 0
      while (i < indicators.length) {
        val ii = indicators(i)
        var j = 0
        while (j < ii.length) {
          if (ii(j)) n += 1
          j += 1
        }
        i += 1
      }
      n
    }
    val as = new Array[Int](count*2)
    var i, k = 0
    while (i < indicators.length) {
      val ii = indicators(i)
      var j = 0
      while (j < ii.length) {
        if (ii(j)) {
          as(k) = i; k += 1
          as(k) = j; k += 1
        }
        j += 1
      }
      i += 1
    }
    new Reshape(as, indicators.map(_.length))
  }

  def sortSet(values: Array[Array[Double]], deduplicate: Boolean = false): Reshape = {
    val count = { 
      var i, n = 0
      while (i < values.length) { 
        val vi = values(i)
        var j = 0; while (j < vi.length) { if (!java.lang.Double.isNaN(vi(j))) n += 1; j += 1 }
        i += 1
      }
      n
    }
    val pes = new Array[Long](count)
    var i, k = 0
    while (i < values.length) {
      val vi = values(i)
      val li = (i & 0x7FFFFFFFL) << 31
      var j = 0
      while (j < vi.length) {
        if (!java.lang.Double.isNaN(vi(j))) {
          pes(k) = li | (j & 0x7FFFFFFFL)
          k += 1
        }
        j += 1
      }
      i += 1
    }
    val spes = pes.sortBy(l => values(((l >>> 31) & 0x7FFFFFFFL).toInt)((l & 0x7FFFFFFFL).toInt))
    val as = new Array[Int](count*2)
    k = 0
    while (k < spes.length) {
      val pek = spes(k)
      as(2*k) = ((pek >>> 31) & 0x7FFFFFFFL).toInt
      as(2*k+1) = (pek & 0x7FFFFFFFL).toInt
      k += 1
    }
    if (deduplicate) {
      i = 2
      k = 0
      while (i+1 < as.length) {
        if (values(as(i))(as(i+1)) == values(as(k))(as(k+1))) {
          if (as(i) < as(k) || (as(i) == as(k) && as(i+1) < as(k+1))) {
            as(k) = as(i)
            as(k+1) = as(i+1)
          }
        }
        else {
          k += 2
          if (k < i) {
            as(k) = as(i)
            as(k+1) = as(i+1)
          }
        }
        i += 2
      }
      if (k+2 < i) return new Reshape(java.util.Arrays.copyOf(as, k+2), values.map(_.length))
    }
    new Reshape(as, values.map(_.length))
  }

  def filtersortSet(values: Array[Array[Double]], p: (Int, Int) => Boolean, deduplicate: Boolean = false) = {
    val filtered = new Array[Array[Double]](values.length)
    var i = 0
    while (i < values.length) {
      val vi = values(i)
      val fi = new Array[Double](vi.length)
      var j = 0
      while (j < vi.length) {
        fi(j) = if (p(i,j)) vi(j) else Double.NaN
        j += 1
      }
      filtered(i) = fi
      i += 1
    }
    val sorted = sortSet(filtered, deduplicate)
    new Reshape(sorted.elements, values.map(_.length))
  }
}
