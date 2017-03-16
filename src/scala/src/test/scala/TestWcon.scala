package org.openworm.trackercommons.test

import org.openworm.trackercommons._
import WconImplicits._

import kse.jsonal._

import org.junit.Test
import org.junit.Assert._

class TestWcon {
  private val Accuracy = 1.001e-3
  private def sigfig(x: Double) = org.openworm.trackercommons.Data.sig(x)

  def same(a: Json.Bool, b: Json.Bool, where: String): String =
    if (a.value == b.value) ""
    else " " + where + ":unequal-bools(" + a.value + "," + b.value + ") "

  def same(a: Json.Num, b: Json.Num, where: String): String = same(a.double, b.double, where)

  def same(a: Json.Str, b: Json.Str, where: String): String = 
    if (a.text == b.text) ""
    else " " + where + ":unequal-strings(" + a.text + "," + b.text + ") "

  def same(a: Json.Arr.Dbl, b: Json.Arr.Dbl, where: String): String =
    if (a.doubles.length != b.doubles.length) " " + where + ":numarray-lengths(" + a.doubles.length + "," + b.doubles.length + ") "
    else {
      var i = 0
      while (i < a.doubles.length) {
        val ai = a.doubles(i)
        val bi = b.doubles(i)
        val ans = same(ai, bi, where + "[" + i + "]")
        if (ans.nonEmpty) return ans
        i += 1
      }
      ""
    }

  def same(a: Json.Arr.All, b: Json.Arr.All, where: String): String = 
    if (a.values.length != b.values.length) " " + where + ":array-lengths(" + a.values.length + "," + b.values.length + ") "
    else {
      var i = 0
      while (i < a.values.length) {
        val s = same(a.values(i), b.values(i), where+"["+i+"]")
        if (s.nonEmpty) return s
        i += 1
      }
      ""
    }

  def same(a: Json.Obj, b: Json.Obj, where: String): String =
    if (a.size != b.size) f" $where:object-size(${a.size}, ${b.size})<<<${a.iterator.map(_._1).mkString("|||")}>>><<<${b.iterator.map(_._1).mkString("|||")}>>>"
    else if (a.hasDuplicateKeys != b.hasDuplicateKeys) " " + where + ":duplicates(" + a.hasDuplicateKeys + "," + b.hasDuplicateKeys + ") "
    else if (!a.hasDuplicateKeys) {
      a.foreach{ (k, v) => b(k) match {
        case _: JastError => return f" $where:key-missing($k)($v)"
        case v2: Json if same(v,v2,"").nonEmpty => return same(v, v2, f"$where:key-values($k)($v, $v2)")
        case _: Json =>
      }}
      ""
    }
    else {
      def BAD(k: String, s: String) = f" $where:$s($k)<<<${a.iterator.collect{ case (ki, v) if k == ki => v }.mkString("|||")}>>><<<${b.iterator.collect{ case (ki, v) if k == ki => v }.mkString("|||")}>>> "
      val arm = new collection.mutable.AnyRefMap[String, List[Json]]
      b.foreach{ (k,v) => arm(k) = v :: arm.getOrElse(k, Nil) }
      val keys = arm.map(_._1)
      keys.foreach{ k => val vs = arm(k); if (vs.lengthCompare(1) > 0) arm(k) = vs.reverse }
      a.foreach{ (k,v) => arm.getOrElse(k, Nil) match {
        case Nil => if (!EmptyJson(v)) return BAD(k, "key-has-less")
        case v2 :: rest =>
          if (EmptyJson(v)) {
            if (EmptyJson(v2)) arm(k) = rest
          }
          else if (EmptyJson(v2)) {
            var vs = rest
            while (vs.nonEmpty && EmptyJson(vs.head)) vs = vs.tail
            if (vs.isEmpty) return BAD(k, "key-has-less")
            if (same(v, vs.head, "").nonEmpty) return same(v, vs.head, f"$where{$k}")
            arm(k) = vs.tail
          }
          else if (same(v, v2, "").isEmpty) arm(k) = rest
          else return same(v, v2, f"$where{$k}")
      }}
      arm.filter{ case (k,vs) => vs.nonEmpty }.toList match {
        case Nil =>
        case (k, _) :: rest => return BAD(k, "key-has-more")
      }
      ""
    }

  object EmptyJson {
    def apply(j: Json): Boolean = j match {
      case Json.Null => true
      case Json.Str("") => true
      case n: Json.Num => n.double.isNaN || n.double.isInfinite
      case ja: Json.Arr if ja.size == 0 => true
      case jo: Json.Obj if jo.size == 0 => true
      case _ => false
    }
  }

  def same(a: Json, b: Json, where: String): String = {
    val ea = EmptyJson(a)
    val eb = EmptyJson(b)
    if (ea && eb) ""
    else if (ea != eb) " " + where + ":empty-nonempty(" + a + "," + b + ") "
    else a match {
      case ab: Json.Bool => b match {
        case bb: Json.Bool => same(ab, bb, where)
        case jaa: Json.Arr.All if jaa.size == 1 => same(Json ~ ab ~ Json, jaa, where)
        case _ => " " + where + ":type-mismatch(bool, " + b + ") "
      }
      case as: Json.Str => b match {
        case bs: Json.Str => same(as, bs, where)
        case jaa: Json.Arr.All if jaa.size == 1 => same(Json ~ as ~ Json, jaa, where)
        case _ => " " + where + ":type-mismatch(str, " + b + ") "
      }
      case an: Json.Num => b match {
        case bn: Json.Num => same(an.double, bn.double, where)
        case jad: Json.Arr.Dbl if jad.doubles.length == 1 => same(an.double, jad.doubles(0), where)
        case _ => " " + where + ":type-mismatch(num, " + b + ") "
      }
      case jad: Json.Arr.Dbl => b match {
        case bn: Json.Num if jad.size == 1 => same(jad.doubles(0), bn.double, where)
        case jbd: Json.Arr.Dbl => same(jad, jbd, where)
        case _ => " " + where + ":type-mismatch(nums, " + b + ") "
      }
      case jaa: Json.Arr.All => b match {
        case jbb: Json.Arr.All => same(jaa, jbb, where)
        case x if jaa.size == 1 => same(b, jaa, where)
        case _ => " " + where + ":type-mismatch(arr=<<<" + jaa + ">>>, <<<" + b + ">>>) "
      }
      case aob: Json.Obj => b match {
        case bob: Json.Obj => same(aob, bob, where)
        case jaa: Json.Arr.All if jaa.size == 1 => same(Json ~ aob ~ Json, jaa, where)
        case _ => " " + where + ":type-mismatch(obj, "+b+") "
      }
      case _ => " " + where + ":unknown-type("+a+") "
    }
  }

  def same(a: String, b: String, where: String): String =
    if (a == b) "" else " " + where +":unequal-strings(" + a + ", " + b + ") "

  def same(a: Option[String], b: Option[String], where: String): String = same(a getOrElse "", b getOrElse "", where)

  def same(a: Seq[String], b: Seq[String], where: String): String =
    if (a.length != b.length) {
      if (a.length + b.length == 1 && (a ++ b).filter(_.nonEmpty).length == 0) ""
      else f" $where:unequal-number-of-strings(${a.length}, ${b.length}) "
    }
    else {
      (a zip b).zipWithIndex.foreach{ case ((ai, bi), i) => val s = same(ai, bi, where+"["+i+"]"); if (s.nonEmpty) return s }
      ""
    }

  def same(a: Set[String], b: Set[String], where: String): String = same(a.toList.sorted, b.toList.sorted, where)

  def same(a: Double, b: Double, where: String): String = {
    if (a.isNaN && b.isNaN) ""
    else if (a.isInfinite && b.isNaN) ""
    else if (a.isNaN && b.isInfinite) ""
    else if (a == b || math.abs(a-b) < Accuracy) ""
    else " "+where+":unequal-numbers(" + a + ", " + b + ") "
  }

  def same(a: Laboratory, b: Laboratory, where: String): String = 
    same(a.pi, b.pi, where + ".pi") +
    same(a.name, b.name, where + ".name") +
    same(a.location, b.location, where + ".location") +
    same(a.custom, b.custom, where + ".custom")

  def same(a: Seq[Laboratory], b: Seq[Laboratory]): String = {
    if (a.length != b.length) " unequal-lab-lengths(" + a.length + ", " + b.length + ") "
    else (a zip b).zipWithIndex.foreach{ case ((ai, bi), i) => val s = same(ai, bi, "lab["+i+"]"); if (s.nonEmpty) return s }
    ""
  }

  def sameTS(
    a: Option[Either[java.time.OffsetDateTime, java.time.LocalDateTime]],
    b: Option[Either[java.time.OffsetDateTime, java.time.LocalDateTime]]
  ): String =
    if (a == b) "" else " timestamp:(" + a + ", " + b + ")"

  def same(a: Either[(Double, Double), Double], b: Either[(Double, Double), Double], where: String): String = (a, b) match {
    case (Right(ar), Right(br)) => same(ar, br, where)
    case (Left(al), Left(bl)) => same(al._1, bl._1, where+".axis_1") + same(al._2, bl._2, where+".axis_2")
    case (Right(ar), lb @ Left(_)) => same(Left((ar, ar)): Either[(Double, Double), Double], lb, where)
    case (la @ Left(_), Right(br)) => same(la, Left((br, br)): Either[(Double, Double), Double], where)
  }

  def sameA(a: Option[Arena], b: Option[Arena]): String = (a, b) match {
    case (None, None) => ""
    case (Some(aa), None) => " arena-mismatch(" + aa + ", _) "
    case (None, Some(ba)) => " arena-mismatch(_, " + ba + ") "
    case (Some(aa), Some(ba)) =>
      same(aa.kind, ba.kind, "arena.type") +
      same(aa.diameter, ba.diameter, "arena.diam") +
      same(aa.custom, ba.custom, "arena.custom")
  }

  def sameS(a: Vector[Software], b: Vector[Software]): String = 
    if (a.length != b.length) " software-length-mismatch(" + a.length + ", " + b.length + ") "
    else (a zip b).map{ case (sa, sb) =>
      same(sa.name, sb.name, "software.name") +
      same(sa.version, sb.version, "software.version") +
      same(sa.featureID, sb.featureID, "software.featureID") +
      same(sa.custom, sb.custom, "software.custom")
    }.filter(_.length > 0).mkString("; ")

  def same(a: Metadata, b: Metadata): String = 
    same(a.lab, b.lab) +
    same(a.who, b.who, "who") +
    sameTS(a.timestamp, b.timestamp) +
    same(a.temperature.getOrElse(Double.NaN), b.temperature.getOrElse(Double.NaN), "temperature") +
    same(a.humidity.getOrElse(Double.NaN), b.humidity.getOrElse(Double.NaN), "humidity") +
    sameA(a.arena, b.arena) +
    same(a.food, b.food, "food") +
    same(a.media, b.media, "media") +
    same(a.sex, b.sex, "sex") +
    same(a.stage, b.stage, "stage") +
    same(a.age.getOrElse(Double.NaN), b.age.getOrElse(Double.NaN), "age") +
    same(a.strain, b.strain, "strain") +
    same(a.protocol, b.protocol, "protocol") +
    sameS(a.software, b.software) +
    same(a.settings getOrElse Json.Null, b.settings getOrElse Json.Null, "settings") +
    same(a.custom, b.custom, "custom")

  def same(a: UnitMap, b: UnitMap): String = same(a.json, b.json, "unitmap")

  def same(pa: Option[Array[Perimeter]], pb: Option[Array[Perimeter]], where: String, ev: Perimeter.type): String = (pa, pb) match {
    case (None, None) => ""
    case (Some(_), None) => where + " perimeter-mismatch(exists, missing)"
    case (None, Some(_)) => where + " perimeter-mismatch(missing, exists)"
    case (Some(ps), Some(qs)) =>
      if (ps.length != qs.length) f"$where perimeter-number-mismatch(${ps.length}, ${qs.length})"
      else (ps zip qs).zipWithIndex.map{ case ((p,q), i) =>
        if (p.size != q.size) f"$where perimeter($i).size-mismatch(${p.size}, ${q.size})"
        else {
          val (px, py) = p.getPoints
          val (qx, qy) = q.getPoints
          val ans = same(Json.Arr.Dbl(px), Json.Arr.Dbl(qx), where + f" perimeter($i).x") + same(Json.Arr.Dbl(py), Json.Arr.Dbl(qy), where + f" perimeter($i).y")
          if (ans.length > 0 && p.isInstanceOf[PixelWalk])
              ans + f"\n${p.asInstanceOf[PixelWalk].toString}\n${q.asInstanceOf[PixelWalk].toString}"
          else ans
        }
      }.filter(_.length > 0).mkString("\n")
  }

  def same(a: Data, b: Data, where: String): String =
    same(a.idJSON, b.idJSON, where + ".id") +
    same(Json.Arr.Dbl(a.ts), Json.Arr.Dbl(b.ts), where + ".ts") +
    same(
      Json.Arr.All((0 until a.xDatas.length).map(i => Json.Arr.Dbl(a.spinePoints(i)._1)).toArray), 
      Json.Arr.All((0 until b.xDatas.length).map(i => Json.Arr.Dbl(b.spinePoints(i)._1)).toArray), 
      where + ".xs"
    ) +
    same(
      Json.Arr.All((0 until a.yDatas.length).map(i => Json.Arr.Dbl(a.spinePoints(i)._2)).toArray), 
      Json.Arr.All((0 until b.yDatas.length).map(i => Json.Arr.Dbl(b.spinePoints(i)._2)).toArray), 
      where + ".ys"
    ) +
    same(Json.Arr.Dbl(a.cxs), Json.Arr.Dbl(b.cxs), where + ".cxs") +
    same(Json.Arr.Dbl(a.cys), Json.Arr.Dbl(b.cys), where + ".cys") +
    same(a.perims.map(_.map(x => x: Perimeter)), b.perims.map(_.map(x => x: Perimeter)), where + ".perim", Perimeter) +
    same(a.walks.map(_.map(x => x: Perimeter)), b.walks.map(_.map(x => x: Perimeter)), where +".walk", Perimeter) +
    same(a.custom, b.custom, where + ".custom")

  def same(a: Array[Data], b: Array[Data]): String =
    if (a.length != b.length) f"unequal-data-lengths(${a.length}, ${b.length})"
    else (a zip b).zipWithIndex.map{ case ((ai, bi), i) => same(ai, bi, "data[" + i + "]") }.filter(_.length > 0).mkString("\n")

  def same(a: FileSet, b: FileSet): String =
    same(a.me, b.me, "fileset.this") +
    same(a.names, b.names, "fileset.names") +
    same(a.custom, b.custom, "fileset.custom")

  def same(a: DataSet, b: DataSet): String =
    same(a.meta, b.meta) + same(a.unitmap, b.unitmap) + same(a.data, b.data) + same(a.files, b.files) + same(a.custom, b.custom, "custom")

  type R = scala.util.Random

  def opt[A](r: R)(f: => A) = if (r.nextDouble < 0.5) None else Some(f)

  def genDbl(r: R): Double = r.nextInt(20) match {
    case 0 => Double.PositiveInfinity
    case 1 => Double.NegativeInfinity
    case 2 | 3 => Double.NaN
    case _ => 10*sigfig(r.nextDouble - 0.3)
  }

  def genANumJ(r: R): Json.Arr.Dbl = Json.Arr.Dbl((0 to r.nextInt(30)).map(_ => genDbl(r)).toArray)

  def genAANumJ(r: R): Json.Arr.All = Json.Arr.All(
    (0 to r.nextInt(6)).map(_ => Json.Arr.Dbl((0 to r.nextInt(7)).map(_ => genDbl(r)).toArray)).toArray
  )

  def genArrJ(r: R, depth: Int, allowDuplicates: Boolean = false): Json.Arr.All =
    if (depth > 5) Json.Arr.All.empty
    else {
      var arrj = Json.Arr.All.empty
      var regen = true
      while (regen) {
        // Don't generate things that are just a numeric array--those should be Json.Arr.Dbl
        val arrj = Json.Arr.All((1 to r.nextInt(6)).map(_ => genJSON(r,depth,allowDuplicates)).toArray)
        if (arrj.values.forall{ case Json.Null => true; case _: Json.Num => true; case _ => false }) regen = true
        else regen = false
      }
      arrj
    }

  def genObjJ(r: R, depth: Int, allowDuplicates: Boolean = false): Json.Obj =
    if (depth > 5) Json.Obj.empty
    else {
      val b = Json.Obj.builder
      (1 to r.nextInt(6)).foreach{ _ => 
        r.nextInt(3) match {
          case 0 => b ~ (genFish(r), genJSON(r, depth, allowDuplicates))
          case 1 =>
            val key = "@" + genFish(r)
            (1 to math.floor(1.2 + r.nextDouble).toInt).foreach(_ => b ~ (key, genJSON(r, depth, allowDuplicates)))
          case _ => b ~ ("q" * (1 + r.nextInt(12)), ((r.nextInt(2) match { case 0 => genANumJ(r); case _ => genAANumJ(r) })))
        }
      }
      b.result
    }

  def genJSON(r: R, depth: Int, allowDuplicates: Boolean = false): Json = if (depth > 5) Json.Null else r.nextInt(10) match {
    case 0 => Json.Null
    case 1 => Json.Bool(r.nextBoolean)
    case 2 => Json.Str(genFish(r))
    case 3 => Json.Num(r.nextInt(5) match { case 0 => Double.PositiveInfinity; case 1 => Double.NegativeInfinity; case _ => Double.NaN })
    case 4 => Json.Num(sigfig(r.nextDouble - 0.5))
    case 5 => genANumJ(r)
    case 6 => genAANumJ(r)
    case 7 => genArrJ(r, depth + 1, allowDuplicates)
    case _ => genObjJ(r, depth + 1, allowDuplicates)
  }

  def genCustom(r: R): Json.Obj = genObjJ(r, 0) match { case g => 
    val b = Json.Obj.builder
    g.foreach{ case (k,v) => b ~ (if (k startsWith "@") k else "@"+k, v) }
    b.result
  }

  val fish = Array("", "", "", "salmon", "cod", "herring", "halibut", "perch", "minnow", "bass", "trout", "pike")
  def genFish(r: R) = fish(r.nextInt(fish.length))

  def genLab(r: R): Laboratory = Iterator.
    continually(Laboratory(genFish(r), genFish(r), genFish(r), genCustom(r))).
    dropWhile(x => x.pi.isEmpty && x.name.isEmpty && x.location.isEmpty).next

  def genLDT(r: R): Either[java.time.OffsetDateTime, java.time.LocalDateTime] =
    if (r.nextBoolean) Right({
      val now = java.time.LocalDateTime.now
      r.nextInt(3) match { case 0 => now.minusNanos(r.nextInt(100000)*1000000L); case 1 => now.minusSeconds(r.nextInt(10000000)); case _ => now }
    })
    else Left({
      val now = java.time.OffsetDateTime.now
      val t = r.nextInt(3) match { case 0 => now.minusNanos(r.nextInt(100000)*1000000L); case 1 => now.minusSeconds(r.nextInt(10000000)); case _ => now }
      r.nextInt(4) match {
        case 0 => t.withOffsetSameLocal(java.time.ZoneOffset.UTC)
        case 1 => t.withOffsetSameLocal(java.time.ZoneOffset.ofHours(r.nextInt(24)-11))
        case _ => t
      }
    })

  def genTemp(r: R): Double = 273.15 + 33*sigfig(r.nextDouble)

  def genArena(r: R): Arena = Iterator.continually{
    Arena(
      genFish(r),
      {
        val a, b = (r.nextInt(100000)+100)/1000.0
        r.nextInt(4) match { case 0 => Right(a); case 1 => Left((a, a)); case 2 => Left((a, b)); case _ => Right(Double.NaN) }
      },
      genFish(r),
      genCustom(r)
    )
  }.dropWhile(x => x.kind.isEmpty && x.diameter.fold(y => y._1.isNaN && y._2.isNaN, _.isNaN)).next

  def genAge(r: R): Double = r.nextBoolean match {
    case false => Double.NaN
    case true => r.nextInt(1000000)/3600.0   // In hours
  }

  def genSoft(r: R): Software = Iterator.continually{
    Software(genFish(r), genFish(r), Vector.fill(r.nextInt(4))("@" + genFish(r)).toSet, genCustom(r))
    }.dropWhile(x => x.name.isEmpty && x.version.isEmpty && x.featureID.isEmpty).next

  def genJSON(r: R): Json = genJSON(r, 0)

  def genMetadata(r: R): Metadata = if (r.nextDouble < 0.33) Metadata.empty else Metadata(
    Vector.fill(r.nextInt(3))(genLab(r)),
    Vector.fill(r.nextInt(3))(genFish(r)),
    opt(r)(genLDT(r)),
    opt(r)(genTemp(r)),
    opt(r)(sigfig(r.nextDouble)),
    opt(r)(genArena(r)),
    opt(r)(genFish(r)),
    opt(r)(genFish(r)),
    opt(r)(genFish(r)),
    opt(r)(genFish(r)),
    opt(r)(genAge(r)),
    opt(r)(genFish(r)),
    Vector.fill(r.nextInt(3))(genFish(r)),
    Vector.fill(r.nextInt(3))(genSoft(r)),
    opt(r)(genJSON(r)),
    genCustom(r)
  )

  def genUnitMap(r: R, md: Metadata): UnitMap = {
    val dur = r.nextInt(6) match { case 0 => "ms"; case 1 => "h"; case 2 => "min"; case _ => "s" }
    val dist = (3+r.nextInt(6)) match { case 0 => "cm"; case 1 => "um"; case 2 => "inch"; case _ => "mm" }
    val namedUnits = Array("mm", "cm", "meter", "metre", "K", "C", "F", "mm^2/s", "1/hr", "1 / ms cm").map(units.parseUnit).map(_.get)
    var m = Map(
      "t" -> units.parseUnit(dur).get,
      "x" -> units.parseUnit(dist).get,
      "y" -> units.parseUnit(dist).get,
      "ox" -> units.parseUnit(dist).get,
      "oy" -> units.parseUnit(dist).get,
      "cx" -> units.parseUnit(dist).get,
      "cy" -> units.parseUnit(dist).get,
      "px" -> units.parseUnit(dist).get,
      "py" -> units.parseUnit(dist).get
    )
    (0 until (r.nextInt(10) - 5)).foreach{ _ =>
      val qs = "q" * (r.nextInt(10) + 1)
      m += (qs -> namedUnits(r.nextInt(namedUnits.length)))
    }
    if (md.temperature.nonEmpty) m += ("temperature" -> units.parseUnit("C").get)
    if (md.humidity.nonEmpty) m += ("humidity" -> units.parseUnit("1").get)
    if (md.age.nonEmpty) m += ("age" -> units.parseUnit("h").get)
    if (md.arena.nonEmpty) m += ("size" -> units.parseUnit(dist).get)
    UnitMap(m, Json.Obj.empty)
  }

  def genData(r: R): Data = {
    val (nid, sid) = r.nextInt(4) match {
      case 0 => (sigfig(r.nextDouble), null)
      case 1 => (r.nextInt(100).toDouble, null)
      case _ => (Double.NaN, "worm-"+r.nextInt(1000))
    }
    val ts = Array.fill(r.nextInt(10)+1)(0.1 + 0.9*r.nextDouble) match { case x =>
      x(0) = sigfig(x(0)); var i = 1; while (i < x.length) { x(i) = sigfig(x(i) + x(i-1)); i += 1 }; x
    }
    val (cxs, cys) = 
      if (r.nextInt(5) == 0) (Data.emptyD, Data.emptyD)
      else {
        val a, b = Array.fill(ts.length)(r.nextDouble - 0.5) match { case x => 
          x(0) = sigfig(x(0)); var i = 1; while (i < x.length) { x(i) = sigfig(x(i) + x(i-1)); i += 1 }; x 
        }
        (a, b)
      }
    val (oxs, oys) = r.nextInt(5) match {
      case 0 => (Data.emptyD, Data.emptyD)
      case 1 => val a, b = Array(sigfig(100*(r.nextDouble - 0.5))); (a, b)
      case _ => val a, b = Array.fill(ts.length)(sigfig(100*(r.nextDouble - 0.5))); (a, b)
    }
    val xsb, ysb = Array.newBuilder[Array[Float]]
    (0 until ts.length).foreach{ _ =>
      val n = r.nextInt(11)+1
      val x, y = Array.fill(n)(sigfig(r.nextDouble - 0.5).toFloat).sorted
      xsb += x
      ysb += y
    }
    val xs = xsb.result
    val ys = ysb.result
    val prms = r.nextInt(4) match {
      case x if x < 2 => None
      case y => Some(Array.fill(ts.length){
        val xs, ys = new Array[Float](20)
        var i = 0; while (i < xs.length) { xs(i) = 2*(r.nextDouble - 0.5).toFloat; ys(i) = 2*(r.nextDouble - 0.5).toFloat; i += 1 }
        PerimeterPoints(xs, ys, if (y == 3) Some(r.nextInt(10)+5) else None)(0, 0)
      })
    }
    val wlks = r.nextInt(4) match {
      case x if x < 2 => None
      case y => Some(Array.tabulate(ts.length){ i =>
        val n = r.nextInt(20)+10
        val w = new Array[Byte]((n+3)/4)
        var j = 0; while (j < w.length) { w(j) = r.nextInt(256).toByte; j += 1 }
        val t = if (y == 3) n/2 else -1
        val x0 = xs(i)(0)
        val y0 = ys(i)(0)
        val s = 0.02 + 0.18*r.nextDouble
        val rx = if (oxs.length == 0) 0 else if (oxs.length == 1) oxs(0) else oxs(i)
        val ry = if (oys.length == 0) 0 else if (oys.length == 1) oys(0) else oys(i)
        PixelWalk(w, n, x0, y0, s, t)(0, 0).globalizeFrom(rx, ry)
      })
    }
    val unarr = ts.length == 1 && r.nextInt(3) == 0
    Data(
      nid, sid, ts, xsb.result, ysb.result, cxs, cys, oxs, oys, prms, wlks, genCustom(r)
    )(
      new Array[Double](ts.length), new Array[Double](ts.length), unarr, oxs.length == 1 && !unarr && r.nextInt(3) == 0
    )
  }

  def genDataA(r: R): Array[Data] = {
    val n = math.max(1, r.nextInt(10)-3)
    Array.fill(n)(genData(r))
  }

  def genFiles(r: R): FileSet = if (r.nextDouble < 0.5) FileSet.empty else {
    val m = r.nextInt(6)+2
    val n = r.nextInt(m)
    val before = ((n-1) to 0 by -1).map("_" + _)
    val me = "_"+n
    val after = ((n+1) to m).map("_" + _)
    FileSet((before.reverse.toVector :+ me) ++ after, before.length, genCustom(r))
  }

  def genDataSet(r: R) = {
    val m = genMetadata(r)
    DataSet(m, genUnitMap(r, m), genDataA(r), genFiles(r), genCustom(r))
  }

  @Test
  def test_OutIn() {
    val r = new scala.util.Random(1522)
    for (i <- 1 to 1000) {
      val ds = genDataSet(r)
      val ser = ds.json.toString
      val des = Jast.parse(ser).to(DataSet) match {
        case Right(x) => x
        case Left(x) =>
          println(ser)
          println(x)
          throw new IllegalArgumentException(x.toString)
      }
      assertEquals("", same(ds, des) match {
        case x if x.length > 0 =>
          println(ser)
          println("###############################")
          println(des.json)
          println(x)
          println
          x
        case x => x
      })      
    }
  }

  @Test
  def test_Compatibility() {
    val r = new scala.util.Random(88512)
    for (i <- 1 to 1000) {
      val ds = genDataSet(r)
      val wc = compatibility.Wcon from ds
      val dss = wc.toUnderlying
      val Seq(dsnd, dssnd) =
        Seq(ds, dss).map(dx => dx.copy(data = dx.data.map{ d => 
          Data.empty.copy(custom = d.custom)(Data.empty.rxs, Data.empty.rys, false, false)
        }))
      assertEquals(
        "", 
        same(dsnd, dssnd).tap{ x => if (x.length > 0) {
          println(dsnd)
          println("###############################")
          println(dssnd)
          println(x)
        }} +
        same(
          ds.copy(data  = ds.data.map(x =>  x.copy(perims = None, walks = None)(x.rxs, x.rys, false, x.originUnarrayed))),
          dss.copy(data = dss.data.map(x => x.copy(perims = None, walks = None)(x.rxs, x.rys, false, x.originUnarrayed)))
        )
      )      
    }
  }

  case class Ex(text: String, line: Int) {}

  def pull_Examples_From_MD(lines: Vector[String], knownExamples: Vector[Ex] = Vector.empty, n: Int = 0): Vector[Ex] = {
    val next = lines.dropWhile(x => !(x.trim.toUpperCase == "```JSON")).drop(1)
    if (next.isEmpty) knownExamples
    else {
      val code = next.takeWhile(x => x.trim != "```")
      pull_Examples_From_MD(
        next.drop(code.length+1),
        knownExamples :+ Ex(code.mkString("\n"), n + (lines.length - next.length)),
        n + (lines.length - next.length) + code.length
      )
    }
  }

  @Test
  def test_FormatSpecExamples() {
    val path = "../../WCON_format.md"
    val lines = { val s = scala.io.Source.fromFile(path); try { s.getLines.toVector } finally { s.close } }
    val codes = pull_Examples_From_MD(lines)
    codes.foreach{ c =>
      if (c.text.trim.startsWith("{")) {
        Jast.parse(c.text).to(DataSet) match {
          case Left(x) => println(c); println(x); throw new IllegalArgumentException(x.toString)
          case _ =>
        }
      }
      else Jast.parse(c.text.trim) match {
        case je: JastError => println(c); println(je); throw new IllegalArgumentException(je.toString)
        case _ =>
      }
    }
  }

  @Test
  def test_all_offsets() {
    val paths = (new java.io.File("../../tests").getCanonicalFile).
      listFiles.
      filter(f => (f.getName startsWith "offset_") && (f.getName endsWith ".wcon"))

    val jsons: Array[Json] = (paths.map(Jast parse _) zip paths).map{
      case (je: JastError, p) => println(je); println(p); fail("Error reading JSON"); throw new Exception("Never here")
      case (j: Json, _) => j
    }

    val wcons: Array[DataSet] = (jsons.map(_.to(DataSet)) zip paths).map{
      case (Left(je), p) => println(je); println(p); fail("Error reading WCON data from JSON"); throw new Exception("Never here")
      case (Right(ds), p) =>
       val dsi = if (ds.files.names.length == 0) ds.copy(files = FileSet(Vector(p.getName), 0, Json.Obj.empty)) else ds
       dsi.files.setRootFile(p)
       dsi
    }

    assertTrue("All coordinates not the same", {
      wcons.forall(w => wcons.forall(v => {
        assertEquals("Number of data entries", w.data.length, v.data.length)
        w.data.indices.forall{ i =>
          val wi = w.data(i)
          val vi = v.data(i)
          val ans = wi.similarTo(vi, 1e-4, wi.cxs.length > 0 && vi.cxs.length > 0)
          if (!ans) {
            println( "Failed coordinate match!")
            println(wi)
            println("ISN'T")
            println(vi)
            println(f"Mismatch at data index $i; note rxs/rys")
            println(f"In ${w.files.me}:")            
            println(f"  ${wi.rxs.mkString(", ")}")
            println(f"  ${wi.rys.mkString(", ")}")
            println(f"In ${v.files.me}:")
            println(f"  ${vi.rxs.mkString(", ")}")
            println(f"  ${vi.rys.mkString(", ")}")
          }
          ans
        }
      }))
    })
  }
}
