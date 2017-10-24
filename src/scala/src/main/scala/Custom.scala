package org.openworm.trackercommons

import scala.collection.mutable.{ AnyRefMap, ListBuffer }

import kse.jsonal._
import kse.jsonal.JsonConverters._

import WconImplicits._

trait Customizable[A] { self: A =>
  def custom: Json.Obj
  def customFn(f: Json.Obj => Json.Obj): A
}

object Custom {
  def apply(o: Json.Obj) = o.filter{ case (k, _) => k startsWith "@" }

  class Unshaped() {
    private[Custom] var myMistakes: Option[Array[Json.Obj]] = None
    def mistakes = myMistakes
  }

  trait Magic { self =>
    def apply(keys: List[String]): Option[(Reshape, Array[Json]) => Option[Json]]

    def orElse(that: Magic): Magic = new Magic {
      def apply(keys: List[String]): Option[(Reshape, Array[Json]) => Option[Json]] =
        self(keys).map(f => (r, vs) => f(r, vs) orElse that(keys).flatMap(g => g(r, vs)))
    }
  }
  object Magic {
    val reshapeArrays: (Reshape, Array[Json]) => Option[Json] = (reshaper, vs) => {
      if (vs.length == 0 || reshaper.length != vs.length) None
      else {        
        var allArrays = true
        var nDblArrs = {
          var i, n = 0
          while (allArrays && i < vs.length) {
            vs(i) match {
              case ja: Json.Arr if ja.size == reshaper.sizes(i) =>
                ja match {
                  case jad: Json.Arr.Dbl => n += 1
                  case jaa: Json.Arr.All =>
                    if (jaa.values.forall(_ match { case Json.Null => true; case _: Json.Num => true; case _ => false })) n += 1
                }
                i += 1
              case _ =>
                allArrays = false
            }
          }
          n
        }
        if (allArrays) {
          if (nDblArrs == vs.length) {
            val xss = new Array[Array[Double]](vs.length)
            var i = 0
            while (i < vs.length) {
              vs(i) match {
                case jad: Json.Arr.Dbl => xss(i) = jad.doubles
                case jaa: Json.Arr.All =>
                  val xs = new Array[Double](reshaper.sizes(i))
                  var j = 0
                  while (j < xs.length) {
                    xs(j) = jaa.values(j).double
                    j += 1
                  }
                  xss(i) = xs
                case _ => throw new IllegalArgumentException("Validated double arrays no longer are double arrays???")
              }
              i += 1
            }
            val shapely = reshaper(xss)
            if (shapely.isEmpty) None
            else Some(Json.Arr.Dbl(shapely))
          }
          else {
            val xss = new Array[Array[Json]](vs.length)
            var i = 0
            while (i < vs.length) {
              vs(i) match {
                case jaa: Json.Arr.All => xss(i) = jaa.values
                case jad: Json.Arr.Dbl =>
                  val xs = new Array[Json](reshaper.sizes(i))
                  var j = 0
                  while (j < xs.length) {
                    xs(j) = Json(jad.doubles(j))
                    j += 1
                  }
                case _ => throw new IllegalArgumentException("Validated sets of JSON arrays are no longer arrays???")
              }
              i += 1
            }
            val shapely = reshaper(xss)
            if (shapely.isEmpty) None
            else Some(Json.Arr.All(shapely))
          }
        }
        else None
      }
    }

    val reshapeConsts: (Reshape, Array[Json]) => Option[Json] = (reshaper, vs) => {
      if (vs.length == 0 || reshaper.length != vs.length) None
      else {
        var allTheSame = true
        var i = 1
        while (allTheSame && i < vs.length) {
          vs(i) match {
            case ja: Json.Arr          => allTheSame = false
            case j: Json if j == vs(0) => i += 1
            case _                     => allTheSame = false
          }
        }
        if (allTheSame) Some(vs(0))
        else None
      }
    }

    val reshapeByDuplicating: (Reshape, Array[Json]) => Option[Json] = (reshaper, vs) => {
      if (vs.length < 2 || reshaper.length != vs.length) None
      else {
        var allCanMatch = true
        var i = 0
        while (allCanMatch && i < vs.length) {
          vs(i) match {
            case ja: Json.Arr => allCanMatch = (ja.size == reshaper.sizes(i))
            case jo: Json.Obj => allCanMatch = false
            case _            =>
          }
          i += 1
        }
        if (allCanMatch) {
          val matching = new Array[Json](vs.length)
          var i = 0
          while (i < vs.length) {
            vs(i) match {
              case ja: Json.Arr => matching(i) = ja
              case jn: Json.Num => matching(i) = Json.Arr.Dbl(Array.fill(reshaper.sizes(i))(jn.double))
              case jx           => matching(i) = Json.Arr.All(Array.fill(reshaper.sizes(i))(jx))
            }
            i += 1
          }
          reshapeArrays(reshaper, matching)
        }
        else None
      }
    }

    private val myMagicArrayOrConst: Option[(Reshape, Array[Json]) => Option[Json]] =
      Some((r,v) => reshapeArrays(r, v) orElse reshapeConsts(r, v))

    private val myMagicArrayOrConstOrDup: Option[(Reshape, Array[Json]) => Option[Json]] =
      Some((r, v) => reshapeArrays(r, v) orElse reshapeConsts(r, v) orElse reshapeByDuplicating(r, v))

    val default: Magic = xs => if (xs.isEmpty || xs.head.startsWith("@")) myMagicArrayOrConst else None
    val expand: Magic = xs => if (xs.isEmpty || xs.head.startsWith("@")) myMagicArrayOrConstOrDup else None
  }

  def fragmentJsonByKeys(os: Array[Json.Obj], depth: Int = 1): Map[List[String], Array[Json]] = {
    val all = collection.mutable.AnyRefMap.empty[String, Array[Json]]
    var i = 0
    while (i < os.length) {
      os(i).iterator.foreach{ case (k, v) => 
        val a = all.getOrElseUpdate(k, new Array[Json](os.length))
        a(i) = v
      }
      i += 1
    }
    val ans = collection.mutable.AnyRefMap.empty[List[String], Array[Json]]
    all.foreach{ case (k, v) =>
      var allo = true
      var nnull = 0
      var i = 0
      while (i < v.length) {
        val vi = v(i)
        if (vi == null) {
          nnull += 1
          v(i) = Json.Null
        }
        else vi match {
          case Json.Null   => nnull += 1
          case _: Json.Obj => 
          case _           => allo = false
        }
        i += 1
      }
      if (!allo || depth <= 0) ans += (k :: Nil, v)
      else {
        val u = new Array[Json.Obj](v.length)
        var i = 0
        while (i < v.length) { 
          u(i) = v(i) match {
            case jo: Json.Obj => jo
            case _            => Json.Obj.empty
          }
          i += 1
        }
        fragmentJsonByKeys(u, depth-1).foreach{ case (kk, vv) => ans += (k :: kk, vv) }
      }
    }
    ans.toMap
  }

  def assembleJsonByKeys(ms: Map[List[String], Json]): Json.Obj = {
    val kvs = collection.mutable.AnyRefMap.empty[String, Map[List[String], Json]]
    ms.foreach{ case (k, v) =>
      k match {
        case key :: Nil  => kvs(key) = Map(Nil -> v)
        case Nil         => kvs("")  = Map(Nil -> v)
        case key :: more => kvs(key) = kvs.getOrElseUpdate(key, Map.empty[List[String], Json]) + ((more, v))
      }
    }
    Json.Obj(kvs.mapValuesNow{ case m =>
      if (m.size == 1 && m.head._1 == Nil) m.head._2
      else assembleJsonByKeys(m)
    })
  }

  /** reshape splits apart a bunch of JSON objects by key, recursively, then merges them into a single JSON object using magic. */
  private def myReshape(os: Array[Json.Obj], reshaper: Reshape, depth: Int, magic: Magic, unshaped: Option[Unshaped]): Json.Obj = {
    val pieces = fragmentJsonByKeys(os, depth)
    val handled = collection.mutable.AnyRefMap.empty[List[String], Json]
    val unhandled = collection.mutable.AnyRefMap.empty[List[String], Array[Json]]
    val trackErrors = unshaped.isDefined
    pieces.foreach{ case (ks, vs) =>
      magic(ks).flatMap(spell => spell(reshaper, vs)) match {
        case Some(j) => handled(ks) = j
        case None    => if (trackErrors) unhandled(ks) = vs
      }
    }
    val union = assembleJsonByKeys(handled.toMap)
    if (trackErrors) {
      unshaped.get.myMistakes =
        if (unhandled.isEmpty) None
        else Some({
          Array.range(0, os.length).map{ i => 
            val m = collection.mutable.AnyRefMap.empty[List[String], Json]
            unhandled.foreach{ case (ks, vs) => if (vs(i) ne null) m(ks) = vs(i) }
            if (m.isEmpty) Json.Obj.empty
            else assembleJsonByKeys(m.toMap)
          }
        })
    }
    union
  }

  def reshape(os: Array[Json.Obj], reshaper: Reshape, depth: Int, magic: Magic, unshaped: Option[Unshaped]) =
    myReshape(os, reshaper, depth, magic, unshaped)
  def reshape(os: Array[Json.Obj], reshaper: Reshape, magic: Magic, unshaped: Option[Unshaped]) =
    myReshape(os, reshaper, 1, magic, unshaped)
  def reshape(os: Array[Json.Obj], reshaper: Reshape, depth: Int, unshaped: Option[Unshaped]) =
    myReshape(os, reshaper, depth, Magic.default, unshaped)
  def reshape(os: Array[Json.Obj], reshaper: Reshape, unshaped: Option[Unshaped]) =
    myReshape(os, reshaper, 1, Magic.default, unshaped)
  def reshape(os: Array[Json.Obj], reshaper: Reshape, depth: Int, magic: Magic) =
    myReshape(os, reshaper, depth, magic, None)
  def reshape(os: Array[Json.Obj], reshaper: Reshape, magic: Magic) =
    myReshape(os, reshaper, 1, magic, None)
  def reshape(os: Array[Json.Obj], reshaper: Reshape, depth: Int) =
    myReshape(os, reshaper, depth, Magic.default, None)
  def reshape(os: Array[Json.Obj], reshaper: Reshape) =
    myReshape(os, reshaper, 1, Magic.default, None)

  def reshape(o: Json.Obj, reshaper: Reshape, depth: Int, magic: Magic, unshaped: Option[Unshaped]) =
    myReshape(Array(o), reshaper, depth, magic, unshaped)
  def reshape(o: Json.Obj, reshaper: Reshape, magic: Magic, unshaped: Option[Unshaped]) =
    myReshape(Array(o), reshaper, 1, magic, unshaped)
  def reshape(o: Json.Obj, reshaper: Reshape, depth: Int, unshaped: Option[Unshaped]) =
    myReshape(Array(o), reshaper, depth, Magic.default, unshaped)
  def reshape(o: Json.Obj, reshaper: Reshape, unshaped: Option[Unshaped]) =
    myReshape(Array(o), reshaper, 1, Magic.default, unshaped)
  def reshape(o: Json.Obj, reshaper: Reshape, depth: Int, magic: Magic) =
    myReshape(Array(o), reshaper, depth, magic, None)
  def reshape(o: Json.Obj, reshaper: Reshape, magic: Magic) =
    myReshape(Array(o), reshaper, 1, magic, None)
  def reshape(o: Json.Obj, reshaper: Reshape, depth: Int) =
    myReshape(Array(o), reshaper, depth, Magic.default, None)
  def reshape(o: Json.Obj, reshaper: Reshape) =
    myReshape(Array(o), reshaper, 1, Magic.default, None)

  object Expand {
    def reshape(os: Array[Json.Obj], reshaper: Reshape, depth: Int, unshaped: Option[Unshaped]) =
      myReshape(os, reshaper, depth, Magic.expand, unshaped)
    def reshape(os: Array[Json.Obj], reshaper: Reshape, unshaped: Option[Unshaped]) =
      myReshape(os, reshaper, 1, Magic.expand, unshaped)
    def reshape(os: Array[Json.Obj], reshaper: Reshape, depth: Int) =
      myReshape(os, reshaper, depth, Magic.expand, None)
    def reshape(os: Array[Json.Obj], reshaper: Reshape) =
      myReshape(os, reshaper, 1, Magic.expand, None)

    def reshape(o: Json.Obj, reshaper: Reshape, depth: Int, unshaped: Option[Unshaped]) =
      myReshape(Array(o), reshaper, depth, Magic.expand, unshaped)
    def reshape(o: Json.Obj, reshaper: Reshape, unshaped: Option[Unshaped]) =
      myReshape(Array(o), reshaper, 1, Magic.expand, unshaped)
    def reshape(o: Json.Obj, reshaper: Reshape, depth: Int) =
      myReshape(Array(o), reshaper, depth, Magic.expand, None)
    def reshape(o: Json.Obj, reshaper: Reshape) =
      myReshape(Array(o), reshaper, 1, Magic.expand, None)
  }
}
