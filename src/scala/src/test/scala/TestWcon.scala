package org.openworm.trackercommons.test

import org.openworm.trackercommons._
import json._

import org.junit.Test
import org.junit.Assert._

class TestWcon {
  private val Accuracy = 1.001e-3

  def same(a: BoolJ, b: BoolJ, where: String): String =
    if (a.value == b.value) ""
    else " " + where + ":unequal-bools(" + a.value + "," + b.value + ") "

  def same(a: NumJ, b: NumJ, where: String): String = same(a.value, b.value, where)

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
        val ans = same(ai, bi, where + "[" + i + "]")
        if (ans.nonEmpty) return ans
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
          val ans = same(aij, bij, where + "[" + i + ", " + j + "]")
          if (ans.nonEmpty) return ans
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

  def same(a: ObjJ, b: ObjJ, where: String): String = {
    val ks = a.keyvals.keySet | b.keyvals.keySet
    ks.foreach{ k =>
      val va = (a.keyvals get k).map(_.filterNot(j => EmptyJson(j))) getOrElse Nil
      val vb = (b.keyvals get k).map(_.filterNot(j => EmptyJson(j))) getOrElse Nil
      val ans = (va, vb) match {
        case (Nil, Nil) => ""
        case (Nil, _) => s" $where{$k}:absent(_, $vb) "
        case (_, Nil) => s" $where{$k}:absent($va, _) "
        case (ai :: Nil, bi :: Nil) => same(ai, bi, where + "{" + k + "}")
        case _ => 
          val aa = if (va.length > 1) ArrJ(va.toArray) else va.head
          val bb = if (vb.length > 1) ArrJ(vb.toArray) else vb.head
          same(aa, bb, where + "{" + k + "}" )
      }
      if (ans.nonEmpty) return ans
    }
    ""
  }

  object EmptyJson {
    def apply(j: JSON): Boolean = j match {
      case NullJ => true
      case StrJ("") => true
      case NumJ(x) => x.isNaN || x.isInfinite
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
        case _ => " " + where + ":type-mismatch(arr=<<<" + aas.toJson + ">>>, "+b.kind+"=<<<" +b.toJson+">>>) "
      }
      case aob: ObjJ => b match {
        case bob: ObjJ =>
          val akb = aob.keyvals.keySet diff bob.keyvals.keySet
          val bka = bob.keyvals.keySet diff aob.keyvals.keySet
          if (akb.nonEmpty || bka.nonEmpty) " " + where + ":key-mismatch(" + akb.toList.sorted.mkString(", ") + "; " + bka.toList.sorted.mkString(", ") + ") "
          else aob.keyvals.iterator.
            map{ case (k,avs) => 
              same(ArrJ(avs.toArray.filter(_ ne NullJ)), ArrJ(bob.keyvals(k).toArray.filter(_ ne NullJ)), where+"."+k) 
            }.
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
    else if (a.isInfinite && b.isNaN) ""
    else if (a.isNaN && b.isInfinite) ""
    else if (math.abs(a-b) < Accuracy) ""
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

  def sameTS(a: Option[(java.time.LocalDateTime, String)], b: Option[(java.time.LocalDateTime, String)]): String =
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

  def sameD(a: Option[java.time.Duration], b: Option[java.time.Duration]): String = 
    if (a == b) "" else " age-mismatch(" + a + ", " + b + ") "

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
    sameD(a.age, b.age) +
    same(a.strain, b.strain, "strain") +
    same(a.protocol, b.protocol, "protocol") +
    sameS(a.software, b.software) +
    same(a.settings getOrElse NullJ, b.settings getOrElse NullJ, "settings") +
    same(a.custom, b.custom, "custom")

  def same(a: UnitMap, b: UnitMap): String = same(a.toObjJ, b.toObjJ, "unitmap")

  def same(a: Datum, b: Datum, where: String): String = same(
    Data(a.nid, a.sid, Array(a.t), Array(a.x), Array(a.y), Array(a.cx), Array(a.cy), a.custom),
    Data(b.nid, b.sid, Array(b.t), Array(b.x), Array(b.y), Array(b.cx), Array(b.cy), b.custom),
    where
  )

  def same(a: Data, b: Data, where: String): String =
    same(a.idJSON, b.idJSON, where + ".id") +
    same(ANumJ(a.ts), ANumJ(b.ts), where + ".ts") +
    same(AANumJ(Data.doubly(a.xs)), AANumJ(Data.doubly(b.xs)), where + ".xs") +
    same(AANumJ(Data.doubly(a.ys)), AANumJ(Data.doubly(b.ys)), where + ".ys") +
    same(ANumJ(a.cxs), ANumJ(b.cxs), where + ".cxs") +
    same(ANumJ(a.cys), ANumJ(b.cys), where + ".cys") +
    same(a.custom, b.custom, where + ".custom")

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
    case _ => 10*(r.nextDouble - 0.3)
  }

  def genANumJ(r: R): ANumJ = ANumJ((0 to r.nextInt(30)).map(_ => genDbl(r)).toArray)

  def genAANumJ(r: R): AANumJ = AANumJ((0 to r.nextInt(6)).map(_ => (0 to r.nextInt(7)).map(_ => genDbl(r)).toArray).toArray)

  def genArrJ(r: R, depth: Int): ArrJ =
    if (depth > 5) ArrJ.empty
    else {
      var arrj = ArrJ.empty
      var regen = true
      while (regen) {
        // Don't generate things that are just a numeric array--those should be ANumJ or AANumJ
        val arrj = ArrJ((1 to r.nextInt(6)).map(_ => genJSON(r,depth)).toArray)
        if (arrj.values.forall{ case NullJ => true; case _: NumJ => true; case _ => false }) regen = true
        else if (arrj.values.forall{ case _: ANumJ => true; case _ => false }) regen = true
        else regen = false
      }
      arrj
    }

  def genObjJ(r: R, depth: Int): ObjJ =
    if (depth > 5) ObjJ.empty
    else ObjJ(
      (1 to r.nextInt(6)).map{ _ => 
        r.nextInt(3) match {
          case 0 => genFish(r) -> (genJSON(r, depth) :: Nil)
          case 1 => ("@" + genFish(r)) -> (1 to math.floor(1.2 + r.nextDouble).toInt).map(_ => genJSON(r, depth)).toList
          case _ => ("q" * (1 + r.nextInt(12)) -> ((r.nextInt(2) match { case 0 => genANumJ(r); case _ => genAANumJ(r) }) :: Nil))
        }
      }.toMap
    )

  def genJSON(r: R, depth: Int): JSON = if (depth > 5) NullJ else r.nextInt(10) match {
    case 0 => NullJ
    case 1 => if (r.nextBoolean) TrueJ else FalseJ
    case 2 => StrJ(genFish(r))
    case 3 => NumJ(r.nextInt(5) match { case 0 => Double.PositiveInfinity; case 1 => Double.NegativeInfinity; case _ => Double.NaN })
    case 4 => NumJ(r.nextDouble - 0.5)
    case 5 => genANumJ(r)
    case 6 => genAANumJ(r)
    case 7 => genArrJ(r, depth + 1)
    case _ => genObjJ(r, depth + 1)
  }

  def genCustom(r: R): ObjJ = genObjJ(r, 0) match { case ObjJ(kvs) => ObjJ(kvs.map{ case (k, vs) => (if (k startsWith "@") k else "@"+k) -> vs }) }

  val fish = Array("", "", "", "salmon", "cod", "herring", "halibut", "perch", "minnow", "bass", "trout", "pike")
  def genFish(r: R) = fish(r.nextInt(fish.length))

  def genLab(r: R): Laboratory = Iterator.continually(Laboratory(genFish(r), genFish(r), genFish(r), genCustom(r))).dropWhile(_ == Laboratory.empty).next

  def genLDT(r: R): (java.time.LocalDateTime, String) = (
    { 
      val now = java.time.LocalDateTime.now
      r.nextInt(3) match { case 0 => now.minusNanos(r.nextInt(100000)*1000000L); case 1 => now.minusSeconds(r.nextInt(10000000)); case _ => now }
    },
    r.nextInt(4) match { case 0 => "Z"; case 1 => "+08:00"; case _ => "" }
  )

  def genTemp(r: R): Double = 273.15 + 33*r.nextDouble

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
  }.dropWhile(x => x.kind.isEmpty && x.diameter.fold(y => y._1.isNaN && y._2.isNaN, _.isNaN) && x.custom.keyvals.isEmpty).next

  def genDur(r: R): java.time.Duration = r.nextBoolean match {
    case false => java.time.Duration.ofMillis(r.nextInt(1000))
    case true => java.time.Duration.ofSeconds(r.nextInt(100))
  }

  def genSoft(r: R): Software = Iterator.continually{
    Software(genFish(r), genFish(r), Vector.fill(r.nextInt(4))("@" + genFish(r)).toSet, genCustom(r))
    }.dropWhile(x => x.name.isEmpty && x.version.isEmpty && x.featureID.isEmpty && x.custom.keyvals.isEmpty).next

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
    Vector.fill(r.nextInt(3))(genSoft(r)),
    opt(r)(genJson(r)),
    genCustom(r)
  )

  def genUnitMap(r: R): UnitMap = {
    val dur = r.nextInt(6) match { case 0 => "ms"; case 1 => "h"; case 2 => "min"; case _ => "s" }
    val dist = r.nextInt(6) match { case 0 => "cm"; case 1 => "um"; case 2 => "inch"; case _ => "mm" }
    val namedUnits = Array("mm", "cm", "meter", "metre", "K", "C", "F", "mm^2/s", "1/hr", "1 / ms cm").map(units.parseUnit).map(_.get)
    var m = Map(
      "t" -> units.parseUnit(dur).get,
      "x" -> units.parseUnit(dist).get,
      "y" -> units.parseUnit(dist).get,
      "ox" -> units.parseUnit(dist).get,
      "oy" -> units.parseUnit(dist).get,
      "cx" -> units.parseUnit(dist).get,
      "cy" -> units.parseUnit(dist).get
    )
    (0 until (r.nextInt(10) - 5)).foreach{ _ =>
      val qs = "q" * (r.nextInt(10) + 1)
      m += (qs -> namedUnits(r.nextInt(namedUnits.length)))
    }
    UnitMap(m, ObjJ.empty)
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
    val before = ((n-1) to 0 by -1).map("_" + _)
    val me = "_"+n
    val after = ((n+1) to m).map("_" + _)
    FileSet((before.reverse.toVector :+ me) ++ after, before.length, genCustom(r))
  }

  def genDataSet(r: R) = DataSet(genMetadata(r), genUnitMap(r), genDataA(r), genFiles(r), genCustom(r))

  @Test
  def test_OutIn() {
    val r = new scala.util.Random(1522)
    for (i <- 1 to 1000) {
      val ds = genDataSet(r)
      val ser = ds.toObjJ.toJsons.mkString("\n")
      val des = Parser(ser) match {
        case Right(x) => x
        case Left(x) =>
          println(ser)
          println(x)
          throw new IllegalArgumentException(x)
      }
      assertEquals("", same(ds, des) match {
        case x if x.length > 0 =>
         println(ser)
         println("###############################")
         println(des.toObjJ.toJsons.mkString("\n"))
         println(x)
         // println(ds.data(1).fold(_.custom, _.custom).keyvals.get("@halibut").get.map(_.toJson).mkString(" :: "))
         // println(des.data(1).fold(_.custom, _.custom).keyvals.get("@halibut").get.map(_.toJson).mkString(" :: "))  
          x
        case x => x
      })      
    }
  }

  case class Ex(text: String, line: Int)

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
        Parser(c.text) match {
          case Left(x) => println(c); println(x); throw new IllegalArgumentException(x)
          case _ =>
        }        
      }
      else json.Struct.All.parse(c.text.trim) match {
        case fastparse.core.Result.Failure(x) => println(c); println(x); throw new IllegalArgumentException(x.toString)
        case _ =>
      }
    }
  }
}
