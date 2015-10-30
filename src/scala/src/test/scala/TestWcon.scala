package org.openworm.trackercommons.test

import org.openworm.trackercommons._
import json._

import org.junit.Test
import org.junit.Assert._

class TestWcon {
  def same(a: BoolJ, b: BoolJ, where: String): String =
    if (a.value == b.value) ""
    else " " + where + ":unequal-bools(" + a.value + "," + b.value + ") "

  def same(a: NumJ, b: NumJ, where: String): String =
    if (a.value.isNaN && b.value.isNaN) ""
    else if (math.abs(a.value-b.value) < 1e-9) ""
    else " " + where + ":unequal-nums(" + a.value + "," + b.value + ") "

  def same(a: StrJ, b: StrJ, where: String): String = 
    if (a.value == b.value) ""
    else " " + where + ":unequal-strings(" + a.value + "," + b.value + ") "

  def same(a: ANumJ, b: ANumJ, where: String): String =
    if (a.values.length != b.values.length) " " + where + ":numarray-lengths(" + a.values.length + "," + b.values.length + ") "
    else {
      var i = 0
      while (i < a.values.length) {
        val ai = a.values(i)
        val bi = b.values(i)
        if (!((ai.isNaN && bi.isNaN) || (math.abs(ai - bi) < 1e-9))) return " " + where + "["+i+"]:unequal-nums(" + ai + "," + bi + ") "
        i += 1
      }
      ""
    }

  def same(a: AANumJ, b: AANumJ, where: String): String =
    if (a.valuess.length != b.valuess.length) " " + where + ":numarrayarray-lengths(" + a.valuess.length + "," + b.valuess.length + ") "
    else {
      var i = 0
      while (i < a.valuess.length) {
        val ai = a.valuess(i)
        val bi = b.valuess(i)
        if (!(ai.length == bi.length)) return " " + where + ":numarrayarray[" + i + "]-sublength(" + ai.length + "," + bi.length + ") "
        var j = 0
        while (j < ai.length) {
          val aij = ai(j)
          val bij = bi(j)
          if (!((aij.isNaN && bij.isNaN) || (math.abs(aij - bij) < 1e-9))) return " " + where + "["+i+","+j+"]:unequal-nums(" + aij + "," + bij + ") "
          j += 1
        }
        i += 1
      }
      ""
    }

  def same(a: ArrJ, b: ArrJ, where: String): String = 
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

  def same(a: ObjJ, b: ObjJ, where: String): String = ???

  object EmptyJson {
    def apply(j: JSON): Boolean = j match {
      case NullJ => true
      case StrJ("") => true
      case ANumJ(v) if v.length == 0 => true
      case ArrJ(v) if v.length == 0 => true
      case ObjJ(kv) if kv.size == 0 => true
      case _ => false
    }
  }

  def same(a: JSON, b: JSON, where: String): String = {
    val ea = EmptyJson(a)
    val eb = EmptyJson(b)
    if (ea && eb) ""
    else if (ea != eb) " " + where + ":empty-nonempty("+a.toJson + "," + b.toJson + ")"
    else a match {
      case ab: BoolJ => b match {
        case bb: BoolJ => same(ab, bb, where)
        case ArrJ(Array(bb: BoolJ)) => same(ab, bb, where)
        case _ => " " + where + ":type-mismatch(bool, "+b.toJson+") "
      }
      case as: StrJ => b match {
        case bs: StrJ => same(as, bs, where)
        case ArrJ(Array(bs: StrJ)) => same(as, bs, where)
        case _ => " " + where + ":type-mismatch(str, "+b.toJson+") "
      }
      case an: NumJ => b match {
        case bn: NumJ => same(an, bn, where)
        case bns: ANumJ if bns.values.length == 1 => same(an, NumJ(bns.values(0)), where)
        case _ => " " + where + ":type-mismatch(num, "+b.toJson+") "
      }
      case ans: ANumJ => b match {
        case bn: NumJ if ans.values.length == 1 => same(NumJ(ans.values(0)), b, where)
        case bns: ANumJ => same(ans, bns, where)
        case _ => " " + where + ":type-mismatch(nums, "+b.toJson+") "
      }
      case anss: AANumJ => b match {
        case bnss: AANumJ => same(anss, bnss, where)
        case _ => " " + where + ":type-mismatch(numss, "+b.toJson+") "
      }
      case aas: ArrJ => b match {
        case bas: ArrJ => same(aas, bas, where)
        case x if aas.length == 1 => same(b, aas, where)
        case _ => " " + where + ":type-mismatch(arr, "+b.toJson+") "
      }
      case aob: ObjJ => b match {
        case bob: ObjJ =>
          val akb = aob.keyvals.keySet diff bob.keyvals.keySet
          val bka = bob.keyvals.keySet diff aob.keyvals.keySet
          if (akb.nonEmpty || bka.nonEmpty) " " + where + ":key-mismatch(" + akb.toList.sorted.mkString(", ") + "; " + bka.toList.sorted.mkString(", ") + ") "
          else aob.keyvals.iterator.
            map{ case (k,avs) => same(ArrJ(avs.toArray), ArrJ(bob.keyvals(k).toArray), where+"."+k) }.
            dropWhile(_.nonEmpty).
            take(1).toList.headOption.getOrElse("")
        case _ => " " + where + ":type-mismatch(obj, "+b.toJson+") "
      }
      case _ => " " + where + ":unknown-type("+a+") "
    }
  }

  def same(a: String, b: String, where: String): String =
    if (a == b) "" else " " + where +":unequal-strings(" + a + ", " + b + ") "

  def same(a: Option[String], b: Option[String], where: String): String = same(a getOrElse "", b getOrElse "", where)

  def same(a: Seq[String], b: Seq[String], where: String): String =
    if (a.length != b.length) " " + where +":unequal-number-of-strings(" + a.length + ", " + b.length + ") "
    else {
      (a zip b).zipWithIndex.foreach{ case ((ai, bi), i) => val s = same(ai, bi, where+"["+i+"]"); if (s.nonEmpty) return s }
      ""
    }

  def same(a: Set[String], b: Set[String], where: String): String = same(a.toList.sorted, b.toList.sorted, where)

  def same(a: Double, b: Double, where: String): String = {
    if (a.isNaN && b.isNaN) ""
    else if (math.abs(a-b) < 1e-9) ""
    else " "+where+":unequal-numbers(" + a + ", " + b + ") "
  }

  def same(a: Laboratory, b: Laboratory, where: String): String = ???

  def same(a: Seq[Laboratory], b: Seq[Laboratory]): String = {
    if (a.length != b.length) " unequal-lab-lengths(" + a.length + ", " + b.length + ") "
    else (a zip b).zipWithIndex.foreach{ case ((ai, bi), i) => val s = same(ai, bi, "lab["+i+"]"); if (s.nonEmpty) return s }
    ""
  }

  def sameTS(a: Option[(java.time.LocalDateTime, String)], b: Option[(java.time.LocalDateTime, String)]): String =
    if (a == b) "" else " timestamp:(" + a + ", " + b + ")"

  def same(a: Temperature, b: Temperature): String =
    same(a.experimental, b.experimental, "temperature.experimental") +
    same(a.cultivation, b.cultivation, "temperature.cultivation") +
    same(a.custom, b.custom, "temperature.custom")

  def sameT(a: Option[Temperature], b: Option[Temperature]): String = (a, b) match {
    case (None, None) => ""
    case (Some(_), None) => " temperature-mismatch(" + a + ", _) "
    case (None, Some(_)) => " temperature-mismatch(_, " + b + ") "
    case (Some(ta), Some(tb)) => same(ta, tb)
  }

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

  def sameD(a: Option[java.time.Duration], b: Option[java.time.Duration]): String = 
    if (a == b) "" else " age-mismatch(" + a + ", " + b + ") "

  def sameS(a: Option[Software], b: Option[Software]): String = (a, b) match {
    case (None, None) => ""
    case (Some(sa), None) => " software-mismatch(" + sa + ", _) "
    case (None, Some(sb)) => " software-mismatch(_, " + sb + ") "
    case (Some(sa), Some(sb)) =>
      same(sa.name, sb.name, "software.name") +
      same(sa.version, sb.version, "software.version") +
      same(sa.featureID, sb.featureID, "software.featureID") +
      same(sa.custom, sb.custom, "software.custom")
  }

  def same(a: Metadata, b: Metadata): String = 
    same(a.lab, b.lab) +
    same(a.who, b.who, "who") +
    sameTS(a.timestamp, b.timestamp) +
    sameT(a.temperature, b.temperature) +
    same(a.humidity.getOrElse(Double.NaN), b.humidity.getOrElse(Double.NaN), "humidity") +
    sameA(a.arena, b.arena) +
    same(a.food, b.food, "food") +
    same(a.media, b.media, "media") +
    same(a.sex, b.sex, "sex") +
    same(a.stage, b.stage, "stage") +
    sameD(a.age, b.age) +
    same(a.strain, b.strain, "strain") +
    same(a.protocol, b.protocol, "protocol") +
    sameS(a.software, b.software) +
    same(a.settings getOrElse NullJ, b.settings getOrElse NullJ, "settings") +
    same(a.custom, b.custom, "custom")

  def same(a: UnitMap, b: UnitMap): String = same(a.toObjJ, b.toObjJ, "unitmap")

  def same(a: Datum, b: Datum, where: String): String = ???

  def same(a: Data, b: Data, where: String): String = ???

  def same(a: Array[Either[Datum, Data]], b: Array[Either[Datum, Data]]): String = {
    if (a.length != b.length) " unequal-data-length(" + a.length + ", " + b.length + ") "
    else {
      for (i <- a.indices) {
        val w = " data["+i+"]"
        val s = (a(i), b(i)) match {
          case (Left(am), Left(bm)) => same(am, bm, w)
          case (Left(am), Right(ba)) if ba.ts.length == 1 => same(am, Datum(ba.nid, ba.sid, ba.ts(0), ba.xs(0), ba.ys(0), ba.cxs(0), ba.cys(0), ba.custom), w)
          case (Right(aa), Left(bm)) if aa.ts.length == 1 => same(Datum(aa.nid, aa.sid, aa.ts(0), aa.xs(0), aa.ys(0), aa.cxs(0), aa.cys(0), aa.custom), bm, w)
          case (Right(aa), Right(ba)) => same(aa, ba, w)
        }
        if (s.nonEmpty) return s
      }
      ""
    }
  }

  def same(a: FileSet, b: FileSet): String =
    same(a.me, b.me, "fileset.this") +
    same(a.before, b.before, "fileset.before") +
    same(a.after, b.after, "fileset.after") +
    same(a.custom, b.custom, "fileset.custom")

  def same(a: DataSet, b: DataSet): String =
    same(a.meta, b.meta) + same(a.unitmap, b.unitmap) + same(a.data, b.data) + same(a.files, b.files) + same(a.custom, b.custom, "custom")

  type R = scala.util.Random

  def opt[A](r: R)(f: => A) = if (r.nextDouble < 0.5) None else Some(f)

  def genObjJ(r: R, depth: Int): ObjJ = if (depth > 5) Metadata.emptyObjJ else ???

  def genJSON(r: R, depth: Int): JSON = if (depth > 5) NullJ else ???

  def genCustom(r: R): ObjJ = genObjJ(r, 0)

  val fish = Array("", "salmon", "cod", "herring", "halibut", "perch", "minnow", "bass", "trout", "pike")
  def genFish(r: R) = fish(r.nextInt(fish.length))

  def genLab(r: R): Laboratory = ???

  def genLDT(r: R): (java.time.LocalDateTime, String) = ???

  def genTemp(r: R): Temperature = ???

  def genArena(r: R): Arena = ???

  def genDur(r: R): java.time.Duration = r.nextBoolean match {
    case false => java.time.Duration.ofMillis(r.nextInt(1000))
    case true => java.time.Duration.ofSeconds(r.nextInt(100))
  }

  def genSoft(r: R): Software = Software(genFish(r), genFish(r), Vector.fill(r.nextInt(4))(genFish(r)).toSet, genCustom(r))

  def genJson(r: R): JSON = genJSON(r, 0)

  def genMetadata(r: R): Metadata = if (r.nextDouble < 0.33) Metadata.empty else Metadata(
    Vector.fill(r.nextInt(3))(genLab(r)),
    Vector.fill(r.nextInt(3))(genFish(r)),
    opt(r)(genLDT(r)),
    opt(r)(genTemp(r)),
    opt(r)(r.nextDouble),
    opt(r)(genArena(r)),
    opt(r)(genFish(r)),
    opt(r)(genFish(r)),
    opt(r)(genFish(r)),
    opt(r)(genFish(r)),
    opt(r)(genDur(r)),
    opt(r)(genFish(r)),
    Vector.fill(r.nextInt(3))(genFish(r)),
    opt(r)(genSoft(r)),
    opt(r)(genJson(r)),
    genCustom(r)
  )

  def genUnitMap(r: R): UnitMap = {
    val dur = r.nextInt(6) match { case 0 => "min"; case 1 => "h"; case 2 => "ms"; case _ => "s" }
    val dist = r.nextInt(6) match { case 0 => "cm"; case 1 => "um"; case 2 => "inch"; case _ => "mm" }
    var m = Map(
      "t" -> units.Units(dur).get,
      "x" -> units.Units(dist).get,
      "y" -> units.Units(dist).get,
      "ox" -> units.Units(dist).get,
      "oy" -> units.Units(dist).get,
      "cx" -> units.Units(dist).get,
      "cy" -> units.Units(dist).get
    )
    (0 until (r.nextInt(10) - 5)).foreach{ _ =>
      val qs = "q" * (r.nextInt(10) + 1)
      m += (qs -> (units.NamedUnits.map(_._2).toArray match { case x => x(r.nextInt(x.length)) }))
    }
    UnitMap(m, Metadata.emptyObjJ)
  }

  def genDatum(r: R): Datum = genData(r) match {
    case Data(nid, sid, ts, xs, ys, cxs, cys, custom) => Datum(nid, sid, ts(0), xs(0), ys(0), cxs(0), cys(0), custom)
  }

  def genData(r: R): Data = {
    val (nid, sid) = r.nextInt(4) match {
      case 0 => (r.nextDouble, null)
      case 1 => (r.nextInt(100).toDouble, null)
      case _ => (Double.NaN, "worm-"+r.nextInt(1000))
    }
    val ts = Array.fill(r.nextInt(10)+1)(0.1 + 0.9*r.nextDouble) match { case x => var i = 1; while (i < x.length) { x(i) = x(i) + x(i-1); i += 1 }; x }
    val cxs, cys = Array.fill(ts.length)(r.nextDouble - 0.5) match { case x => var i = 1; while (i < x.length) { x(i) = x(i) + x(i-1); i += 1 }; x }
    val xsb, ysb = Array.newBuilder[Array[Float]]
    (0 until ts.length).foreach{ _ =>
      val n = r.nextInt(11)+1
      val x, y = Array.fill(n)((r.nextDouble - 0.5).toFloat).sorted
      xsb += x
      ysb += y
    }
    Data(nid, sid, ts, xsb.result, ysb.result, cxs, cys, genCustom(r))
  }

  def genDataA(r: R): Array[Either[Datum, Data]] = {
    val n = math.max(1, r.nextInt(10)-3)
    Array.fill(n)(if (r.nextBoolean) Right(genData(r)) else Left(genDatum(r)))
  }

  def genFiles(r: R): FileSet = if (r.nextDouble < 0.5) FileSet.empty else {
    val m = r.nextInt(6)+2
    val n = r.nextInt(m)
    FileSet("_"+n, ((n-1) to 0 by -1).map("_" + _).toList, ((n+1) to m).map("_" + _).toList, genCustom(r))
  }

  def genDataSet(r: R) = DataSet(genMetadata(r), genUnitMap(r), genDataA(r), genFiles(r), genCustom(r))
}
