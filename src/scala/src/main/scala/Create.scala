package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

import WconImplicits._

object Create {
  sealed trait HasUnits {}
  final class NoUnits private () extends HasUnits {}
  final class YesUnits private () extends HasUnits {}

  sealed trait HasData {}
  final class NoData private () extends HasData {}
  final class YesData private () extends HasData {}

  sealed trait HasFile {}
  final class NoFile private () extends HasFile{}
  final class YesFile private () extends HasFile{}

  sealed trait HasID {}
  final class YesID private() extends HasID {}
  final class NoID private() extends HasID {}

  final class MakeWcon[U <: HasUnits, D <: HasData, F <: HasFile] private[trackercommons] (building: DataSet, nData: Int) {
    private[this] var stale = false

    def result()(implicit evU: U =:= YesUnits, evD: D =:= YesData) =
      building.copy(data = java.util.Arrays.copyOf(building.data, nData))

    def write()(implicit evU: U =:= YesUnits, evD: D =:= YesData, evF: F =:= YesFile): scala.util.Try[Unit] = scala.util.Try{
      ReadWrite.write(result, building.files.current)
    }

    def setUnits(u: UnitMap)(implicit evD: D =:= YesData): MakeWcon[YesUnits, D, F] = {
      stale = true
      new MakeWcon[YesUnits, D, F](building.copy(unitmap = u), nData)
    }
    def setUnits(extra: Map[String, String])(implicit evD: D =:= YesData): Either[String, MakeWcon[YesUnits, D, F]] = {
      val all = new collection.mutable.AnyRefMap[String, String]
      def sym(a: String, b: String) {
        val hasA = all contains a
        val hasB = all contains b
        if (hasA != hasB) {
          if (hasA) all += (b, all(a))
          else      all += (a, all(b))
        }
      }
      def adopt(a: String, b: String, k: String = null) {
        if (!all.contains(a)) {
          all += (a, if (k eq null) all(b) else all.getOrElse(b, k))
        }
      }
      def default(a: String, k: String) {
        if (!all.contains(a)) all += (a, k)
      }
      extra.foreach{ case (k, v) => all += (k, v) }
      sym("x", "y")
      sym("ox", "oy")
      sym("cx", "cy")
      sym("px", "py")
      var hasoxy = false
      var hascxy = false
      var haspxy = false
      var i = 0
      while (i < nData) { 
        val di = building.data(i)
        if (di.cxs.length > 0) hascxy = true
        if (di.oxs.length > 0) hasoxy = true
        if (di.perims.nonEmpty || di.walks.nonEmpty) haspxy = true
        i += 1
      }
      if (!all.contains("t")) all += ("t", "s")
      adopt("x", "cx", "mm")
      adopt("y", "cy", "mm")
      if (hasoxy) { adopt("ox", "x"); adopt("oy", "y") }
      if (hascxy) { adopt("cx", "x"); adopt("cy", "y") }
      if (haspxy) { adopt("px", "x"); adopt("py", "y") }
      if (building.meta.age.isDefined) default("age", "h")
      if (building.meta.temperature.exists(x => !x.isNaN)) default("temperature", "C")
      if (building.meta.humidity.exists(x => !x.isNaN)) default("humidity", "%")
      if (building.meta.arena.exists(_.size.isDefined)) adopt("size", "x")
      Right(setUnits(UnitMap(
        all.toMap.map{ case (k, v) => k -> ({
          units.parseUnit(v) match {
            case None => return Left(f"Not a unit: $v (for $k)")
            case Some(u) => u
          }
        })},
        Json.Obj.empty
      )))
    }
    def setUnits()(implicit evD: D =:= YesData): MakeWcon[YesUnits, D, F] = setUnits(Map.empty[String, String]).right.get   // Always succeeds when no extras

    def setFiles(files: FileSet): MakeWcon[U, D, YesFile] = {
      stale = true
      new MakeWcon[U, D, YesFile](building.copy(files = files), nData)
    }
    def setOnlyFile(file: String): MakeWcon[U, D, YesFile] = setFiles(FileSet(file))
    def setOnlyFile(file: java.io.File): MakeWcon[U, D, YesFile] = setFiles(FileSet(file.getPath))

    def setMeta(meta: Metadata): MakeWcon[U, D, F] = { stale = true; new MakeWcon[U, D, F](building.copy(meta = meta), nData) }
    def setMeta(meta: MakeMeta[YesID]): MakeWcon[U, D, F] = setMeta(meta.result)
    def setMetaWithID(meta: Metadata, id: String): MakeWcon[U, D, F] = setMeta(meta.copy(id = id))
    def setMetaWithID[I <: HasID](meta: MakeMeta[I], id: String): MakeWcon[U, D, F] = setMeta(meta.resultWithID(id))

    def addData(data: Data): MakeWcon[U, YesData, F] = {
      if (nData + 1 < building.data.length && !stale){
        stale = true
        building.data(nData) = data
        new MakeWcon[U, YesData, F](building, nData + 1)
      }
      else {
        stale = false
        val m = math.max(2*nData, nData + 1)
        val d2 = if (building.data.length > 0) java.util.Arrays.copyOf(building.data, m) else new Array[Data](m)
        d2(nData) = data
        new MakeWcon[U, YesData, F](building.copy(data = d2), nData + 1)
      }
    }
    def addData(data1: Data, data2: Data, more: Data*): MakeWcon[U, YesData, F] = {
      var w = addData(data1).addData(data2)
      for (d <- more) w = w.addData(d)
      w
    }
    def dropData: MakeWcon[U, NoData, F] = { stale = false; new MakeWcon[U, NoData, F](building.copy(data = Array.empty), 0) }

    def putCustom(key: String, value: Json) = {
      stale = true
      new MakeWcon(building.copy(custom = Json.Obj(building.custom.asMap + ((key, value)))), nData)
    }
    def setCustom(custom: Json.Obj) = {
      stale = true
      new MakeWcon(building.copy(custom = custom), nData)
    }
    def dropCustom = {
      stale = true
      if (building.custom.size == 0) this
      else new MakeWcon(building.copy(custom = Json.Obj.empty), nData)
    }   
  }

  def wcon(): MakeWcon[NoUnits, NoData, NoFile] = new MakeWcon(DataSet.empty, 0)

  final class MakeMeta[I <: HasID] private[trackercommons] (underlying: Metadata) {
    def setID(id: String): MakeMeta[YesID] = new MakeMeta[YesID](underlying.copy(id = id))
    def dropID: MakeMeta[NoID] =
      if (underlying.id.isEmpty) this.asInstanceOf[MakeMeta[NoID]]
      else new MakeMeta[NoID](underlying.copy(id = ""))

    def addLab(lab: Laboratory): MakeMeta[I] =
      if (lab.isEmpty) this
      else new MakeMeta[I](underlying.copy(lab = underlying.lab :+ lab))
    def addLab(lab: MakeLab): MakeMeta[I] = addLab(lab.result)
    def setLab(labs: Seq[Laboratory]) = new MakeMeta[I](underlying.copy(lab = labs.toVector))
    def dropLabs = if (underlying.lab.isEmpty) this else new MakeMeta[I](underlying.copy(lab = Vector.empty))

    def addWho(who: String) = if (who.isEmpty) this else new MakeMeta[I](underlying.copy(who = underlying.who :+ who))
    def setWho(whos: Seq[String]) = new MakeMeta[I](underlying.copy(who = whos.toVector))
    def dropWhos = if (underlying.who.isEmpty) this else new MakeMeta[I](underlying.copy(who = Vector.empty))

    def setTime(time: java.time.OffsetDateTime) = new MakeMeta[I](underlying.copy(timestamp = Some(Left(time))))
    def setTime(time: java.time.LocalDateTime) = new MakeMeta[I](underlying.copy(timestamp = Some(Right(time))))
    def dropTime = if (underlying.timestamp.isEmpty) this else new MakeMeta[I](underlying.copy(timestamp = None))

    def setTemp(temperature: Double) = new MakeMeta[I](underlying.copy(temperature = Some(temperature).filter(x => !x.isNaN && !x.isInfinite)))
    def dropTemp = if (underlying.temperature.isEmpty) this else new MakeMeta[I](underlying.copy(temperature = None))

    def setHumidity(humidity: Double) = new MakeMeta[I](underlying.copy(humidity = Some(humidity).filter(x => !x.isNaN && !x.isInfinite)))
    def dropHumidity = if (underlying.humidity.isEmpty) this else new MakeMeta[I](underlying.copy(humidity = None))

    def setArena(arena: Arena): MakeMeta[I] = if (arena.isEmpty) dropArena else new MakeMeta[I](underlying.copy(arena = Some(arena)))
    def setArena(arena: MakeArena): MakeMeta[I] = setArena(arena.result)
    def dropArena = if (underlying.arena.isEmpty) this else new MakeMeta[I](underlying.copy(arena = None))

    def setFood(food: String): MakeMeta[I] = if (food.isEmpty) dropFood else new MakeMeta[I](underlying.copy(food = Some(food)))
    def setFood(): MakeMeta[I] = setFood("OP50")
    def dropFood: MakeMeta[I] = if (underlying.food.isEmpty) this else new MakeMeta[I](underlying.copy(food = None))

    def setMedia(media: String): MakeMeta[I] = if (media.isEmpty) dropMedia else new MakeMeta[I](underlying.copy(media = Some(media)))
    def setMedia(): MakeMeta[I] = setMedia("NGM")
    def dropMedia: MakeMeta[I] = if (underlying.media.isEmpty) this else new MakeMeta[I](underlying.copy(media = None))

    def setSex(sex: String): MakeMeta[I] = if (sex.isEmpty) dropSex else new MakeMeta[I](underlying.copy(sex = Some(sex)))
    def setSex(): MakeMeta[I] = setSex("hermaphrodite")
    def dropSex: MakeMeta[I] = if (underlying.sex.isEmpty) this else new MakeMeta[I](underlying.copy(sex = None))

    def setStage(stage: String) = if (stage.isEmpty) dropStage else new MakeMeta[I](underlying.copy(stage = Some(stage)))
    def dropStage = if (underlying.stage.isEmpty) this else new MakeMeta[I](underlying.copy(stage = None))

    def setAge(age: Double) = new MakeMeta[I](underlying.copy(age = Some(age).filter(x => !x.isNaN && !x.isInfinite)))
    def dropAge = if (underlying.age.isEmpty) this else new MakeMeta[I](underlying.copy(age = None))

    def setStrain(strain: String) = if (strain.isEmpty) dropStrain else new MakeMeta[I](underlying.copy(strain = Some(strain)))
    def dropStrain = if (underlying.strain.isEmpty) this else new MakeMeta[I](underlying.copy(strain = None))

    def addProtocol(protocol: String): MakeMeta[I] = new MakeMeta[I](underlying.copy(protocol = underlying.protocol :+ protocol))
    def setProtocol(protocols: Seq[String]): MakeMeta[I] = new MakeMeta[I](underlying.copy(protocol = protocols.toVector))
    def dropProtocols = if (underlying.protocol.isEmpty) this else new MakeMeta[I](underlying.copy(protocol = Vector.empty))

    def addInterpolate(interpolate: Interpolate): MakeMeta[I] =
      if (interpolate.isEmpty) this
      else new MakeMeta[I](underlying.copy(interpolate = underlying.interpolate :+ interpolate))
    def setInterpolate(interpolations: Seq[Interpolate]): MakeMeta[I] = new MakeMeta[I](underlying.copy(interpolate = interpolations.toVector))
    def dropInterpolations = if (underlying.interpolate.isEmpty) this else new MakeMeta[I](underlying.copy(interpolate = Vector.empty))

    def addSoftware(software: Software): MakeMeta[I] =
      if (software.isEmpty) this
      else new MakeMeta[I](underlying.copy(software = underlying.software :+ software))
    def addSoftware(software: MakeSoft): MakeMeta[I] = addSoftware(software.result)
    def setSoftware(softwares: Seq[Software]): MakeMeta[I] = new MakeMeta[I](underlying.copy(software = softwares.toVector))
    def dropSoftware = if (underlying.software.isEmpty) this else new MakeMeta[I](underlying.copy(software = Vector.empty))

    def putCustom(key: String, value: Json) = new MakeMeta[I](underlying.copy(custom = Json.Obj(underlying.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeMeta[I](underlying.copy(custom = custom))
    def dropCustom = if (underlying.custom.size == 0) this else new MakeMeta[I](underlying.copy(custom = Json.Obj.empty))

    def resultWithID(id: String) = underlying.copy(id = id)
    def resultWithUUID() = underlying.copy(id = java.util.UUID.randomUUID.toString)

    def result(implicit ev: I =:= YesID) = underlying
  }

  def meta() = new MakeMeta[NoID](Metadata.empty)

  final class MakeLab private[trackercommons] (val result: Laboratory) {
    def isEmpty = result.isEmpty

    def pi(s: String) = result.copy(pi = s)
    def title(s: String) = result.copy(name = s)
    def location(s: String) = result.copy(location = s)

    def putCustom(key: String, value: Json) = new MakeLab(result.copy(custom = Json.Obj(result.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeLab(result.copy(custom = custom))
    def dropCustom = if (result.custom.size == 0) this else new MakeLab(result.copy(custom = Json.Obj.empty))
  }

  def lab() = new MakeLab(Laboratory.empty)

  final class MakeArena private[trackercommons] (val result: Arena) {
    def isEmpty = result.isEmpty

    def style(s: String) = new MakeArena(result.copy(style = s))
    def orientation(s: String) = new MakeArena(result.copy(orientation = s))
    def size(d: Double) = new MakeArena(result.copy(size = Some(Right(d))))
    def size(d1: Double, d2: Double) = new MakeArena(result.copy(size = Some(Left((d1, d2)))))
    def dropSize = new MakeArena(result.copy(size = None))

    def putCustom(key: String, value: Json) = new MakeArena(result.copy(custom = Json.Obj(result.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeArena(result.copy(custom = custom))
    def dropCustom = if (result.custom.size == 0) this else new MakeArena(result.copy(custom = Json.Obj.empty))
  }

  def arena() = new MakeArena(Arena.empty)

  final class MakeInterp private[trackercommons] (val result: Interpolate) {
    def isEmpty = result.isEmpty

    def method(s: String) = new MakeInterp(result.copy(method = s))

    def addValue(s: String): MakeInterp = new MakeInterp(result.copy(values = result.values :+ s))
    def setValues(ss: Iterable[String]): MakeInterp = new MakeInterp(result.copy(values = ss.toVector))
    def dropValues = if (result.values.isEmpty) this else new MakeInterp(result.copy(values = Vector.empty))

    def putCustom(key: String, value: Json) = new MakeInterp(result.copy(custom = Json.Obj(result.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeInterp(result.copy(custom = custom))
    def dropCustom = if (result.custom.size == 0) this else new MakeInterp(result.copy(custom = Json.Obj.empty))
  }

  def interpolate() = new MakeInterp(Interpolate.empty)

  final class MakeSoft private[trackercommons] (val result: Software) {
    def isEmpty = result.isEmpty

    def name(s: String) = new MakeSoft(result.copy(name = s))
    def version(s: String) = new MakeSoft(result.copy(version = s))

    def addFeature(s: String) = new MakeSoft(result.copy(featureID = result.featureID + (if (s.startsWith("@")) s else "@" + s)))
    def setFeatures(ss: Iterable[String]) =
      new MakeSoft(result.copy(featureID = ss.map(s => if (s.startsWith("@")) s else "@" + s).toSet))
    def dropFeatures = new MakeSoft(result.copy(featureID = Set.empty))

    def setSettings(j: Json) = new MakeSoft(result.copy(settings = Some(j)))
    def dropSettings = new MakeSoft(result.copy(settings = None))

    def putCustom(key: String, value: Json) = new MakeSoft(result.copy(custom = Json.Obj(result.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeSoft(result.copy(custom = custom))
    def dropCustom = if (result.custom.size == 0) this else new MakeSoft(result.copy(custom = Json.Obj.empty))
  }

  def software() = new MakeSoft(Software.empty)

  final class DataBuilder[D <: HasData](id: String) {
    private[this] var i = 0

    private[this] trait Acc {
      def size: Int
      def size_=(ii: Int): Unit
      def capacity: Int
      def copyTo(m: Int): Unit
      def zeroAt(j: Int): Unit
      final def free(n: Int) {
        val N = n.toLong + size
        if (N > capacity) {
          val m = math.min(Int.MaxValue - 1, math.max(2L*capacity, N)).toInt
          copyTo(m)
        }
      }
      final def fill(k: Int) { 
        if (size < k) {
          free(k - size)
          while (size < k) {
            zeroAt(size)
            size = size + 1
          }
        }
      }
      def keepup() {
        fill(i-1)
        free(1)
      }
      def complete() { fill(i); if (i != capacity) copyTo(i) }
    }

    // Helper class to accumulate the core times/positions
    private[this] final class AccTXY extends Acc {
      var ts = new Array[Double](1)
      var xs, ys = new Array[Array[Float]](1)
      var rxs, rys = new Array[Double](1)
      def size = i
      def size_=(ii: Int) { i = ii }
      def capacity = ts.length
      def copyTo(m: Int) {
        ts = java.util.Arrays.copyOf(ts, m)
        xs = java.util.Arrays.copyOf(xs, m)
        ys = java.util.Arrays.copyOf(ys, m)
        rxs = java.util.Arrays.copyOf(rxs, m)
        rys = java.util.Arrays.copyOf(rys, m)
      }
      def zeroAt(j: Int) {
        ts(j) = Double.NaN
        xs(j) = DataBuilder.emptyF
        ys(j) = DataBuilder.emptyF
        rxs(j) = 0.0
        rys(j) = 0.0
      }
      def add(t: Double, x: Array[Double], y: Array[Double]) {
        free(1)
        ts(i) = t
        val m = math.min(x.length, y.length)
        if (m > 0) {
          var x0 = x(0)
          var y0 = y(0)
          var j = 1; while (j < m) { if (x(j) < x0) x0 = x(j); if (y(j) < y0) y0 = y(j); j += 1 }
          val xi = new Array[Float](m)
          val yi = new Array[Float](m)
          j = 0; while (j < m) { xi(j) = (x(j) - x0).toFloat; yi(j) = (y(j) - y0).toFloat; j += 1 }
          xs(i) = xi
          ys(i) = yi
          rxs(i) = x0
          rys(i) = y0
        }
        else {
          xs(i) = DataBuilder.emptyF
          ys(i) = DataBuilder.emptyF
          rxs(i) = 0
          rys(i) = 0
        }
        i += 1
      }
    }
    private[this] val txy = new AccTXY;

    // Helper class to accumulate the additional XY data: origins and centroids
    private[this] final class AccXYQ extends Acc {
      var qi = 0
      var qxs, qys = DataBuilder.emptyD
      def size = qi
      def size_=(ii: Int) { qi = ii }
      def capacity = qxs.length
      def copyTo(m: Int) {
        qxs = java.util.Arrays.copyOf(qxs, m)
        qys = java.util.Arrays.copyOf(qys, m)
      }
      def zeroAt(j: Int) {
        qxs(j) = Double.NaN
        qys(j) = Double.NaN
      }
      def add(qx: Double, qy: Double) {
        if (qi > 0 || qx.finite || qy.finite) {
          keepup()
          qxs(qi) = qx
          qys(qi) = qy
          qi += 1
        }
      }
      override def complete() { if (qi > 0) super.complete() }
    }
    private[this] val oxy = new AccXYQ
    private[this] val cxy = new AccXYQ

    // Helper class to accumulate other single things (perimeters, ventral/head stuff)
    private[this] class AccA[A: reflect.ClassTag](zero: A, dedup: Boolean = false)(nonzero: A => Boolean) extends Acc {
      var ai = 0
      var dupi = 0
      var aa = new Array[A](1)
      def size = ai
      def size_=(ii: Int) { ai = ii }
      def capacity = aa.length
      def copyTo(m: Int) { val aaa = new Array[A](m); System.arraycopy(aa, 0, aaa, 0, math.min(aa.length, m)); aa = aaa }
      def zeroAt(j: Int) { aa(j) = zero }
      def add(a: A) {
        if (dedup && aa.length < 2 && ai == 1) {
          if (a == aa(0)) { dupi += 1; return }
          if (dupi > 0) {
            aa = Array.fill(ai + dupi)(aa(0))
            ai += dupi
            dupi = 0
          }
        }
        if (nonzero(a) || ai > 0) {
          keepup()
          aa(ai) = a
          ai += 1
        }
      }
      override def complete() {}
      def get() = {
        if (size > 0) {
          if (dedup && aa.length == 1 && ai == 1) Some(Array(aa(0)))
          else {
            fill(i)
            val temp = aa
            copyTo(i)
            val ans = Some(aa)
            aa = temp
            ans
          }
        }
        else None
      }
      def only(a: A) {
        ai = 1
        aa = Array(a)
      }
    }
    private[this] val pp = new AccA(PerimeterPoints.empty)(_.size > 0)
    private[this] val wk = new AccA(PixelWalk.empty)(_.size > 0)
    private[this] val hd = new AccA("?", true)(_ ne null)
    private[this] val vn = new AccA("?", true)(_ ne null)

    private[this] var jm: collection.mutable.AnyRefMap[String, JArB] = null
    private[this] def jAdd(key: String, value: Json) {
      if (jm eq null) jm = new collection.mutable.AnyRefMap[String, JArB]
      jm.getOrElseUpdate(key, new JArB).add(value, i)
    }
    private[this] def jGet = Json.Obj.empty/*
      if (jm eq null) Json.Obj.empty
      else Json.Obj(jm.mapValues(b => Json(b.result)).toMap)*/

    def validate: Either[DataBuilder[NoData], DataBuilder[YesData]] =
      if (i == 0) Left(this.asInstanceOf[DataBuilder[NoData]])
      else Right(this.asInstanceOf[DataBuilder[YesData]])

    def result(implicit ev: D =:= YesData): Data = {
      txy.complete
      oxy.complete
      cxy.complete
      if (jm ne null) jm.foreach{ case (_, j) => j.publish(i) }
      val myHd = hd.get.getOrElse(DataBuilder.emptyS)
      val myVn = vn.get.getOrElse(DataBuilder.emptyS)
      Data(id, txy.ts, txy.xs, txy.ys, cxy.qxs, cxy.qys, oxy.qxs, oxy.qys, pp.get, wk.get, myHd, myVn, jGet)(txy.rxs, txy.rys)
    }

    private[trackercommons] def personalCustom(j: Json.Obj): this.type = {
      j.iterator.foreach{ case (k, jv) => jAdd(k, jv) }
      this
    }

    private def myAdd(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int, w: PixelWalk,
      ox: Double, oy: Double, j: Json.Obj
    ): DataBuilder[YesData] = {
      txy.add(t, xs, ys)
      var rxi = txy.rxs(i-1)
      var ryi = txy.rys(i-1)
      cxy.add(cx, cy)
      if ((pxs ne null) && (pys ne null) && pxs.length > 0 && pys.length > 0) {
        val pxi, pyi = new Array[Float](math.min(pxs.length, pys.length))
        var j = 0; while (j < pxi.length) { pxi(j) = (pxs(j) - rxi).toFloat; pyi(j) = (pys(j) - ryi).toFloat; j += 1 }
        pp.add(PerimeterPoints(pxi, pyi, if (ptail >= 0) Some(ptail) else None)(rxi, ryi))
      }
      if (w ne null) wk.add(w)
      oxy.add(ox, oy)
      if (j.size > 0) j.iterator.foreach{ case (k, jv) => jAdd(k, jv) }
      this.asInstanceOf[DataBuilder[YesData]]
    }

    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int,
      ox: Double, oy: Double, j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, pxs, pys, ptail, null, ox, oy, j)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int,
      ox: Double, oy: Double
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, pxs, pys, ptail, null, ox, oy, Json.Obj.empty)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int,
      j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, pxs, pys, ptail, null, Double.NaN, Double.NaN, j)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, pxs, pys, ptail, null, Double.NaN, Double.NaN, Json.Obj.empty)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk,
      ox: Double, oy: Double, j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, w, ox, oy, j)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk,
      ox: Double, oy: Double
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, w, ox, oy, Json.Obj.empty)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk,
      j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, w, Double.NaN, Double.NaN, j)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, w, Double.NaN, Double.NaN, Json.Obj.empty)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double],
      ox: Double, oy: Double, j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, null, ox, oy, j)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double],
      ox: Double, oy: Double
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, null, ox, oy, Json.Obj.empty)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double],
      j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, null, Double.NaN, Double.NaN, j)
    def add(
      t: Double, cx: Double, cy: Double, xs: Array[Double], ys: Array[Double]
    ): DataBuilder[YesData] = myAdd(t, cx, cy, xs, ys, null, null, -1, null, Double.NaN, Double.NaN, Json.Obj.empty)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int,
      ox: Double, oy: Double, j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, pxs, pys, ptail, null, ox, oy, j)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int,
      ox: Double, oy: Double
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, pxs, pys, ptail, null, ox, oy, Json.Obj.empty)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int,
      j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, pxs, pys, ptail, null, Double.NaN, Double.NaN, j)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      pxs: Array[Double], pys: Array[Double], ptail: Int
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, pxs, pys, ptail, null, Double.NaN, Double.NaN, Json.Obj.empty)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk,
      ox: Double, oy: Double, j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, w, ox, oy, j)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk,
      ox: Double, oy: Double
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, w, ox, oy, Json.Obj.empty)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk,
      j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, w, Double.NaN, Double.NaN, j)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double], 
      w: PixelWalk
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, w, Double.NaN, Double.NaN, Json.Obj.empty)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double],
      ox: Double, oy: Double, j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, null, ox, oy, j)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double],
      ox: Double, oy: Double
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, null, ox, oy, Json.Obj.empty)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double],
      j: Json.Obj
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, null, Double.NaN, Double.NaN, j)
    def add(
      t: Double, xs: Array[Double], ys: Array[Double]
    ): DataBuilder[YesData] = myAdd(t, Double.NaN, Double.NaN, xs, ys, null, null, -1, null, Double.NaN, Double.NaN, Json.Obj.empty)

    def allHeads(s: String)(implicit ev: D =:= YesData): this.type = { hd.only(s); this }
    def withHead(s: String)(implicit ev: D =:= YesData): this.type = { hd.add(s); this }

    def allVentrals(s: String)(implicit ev: D =:= YesData): this.type = { vn.only(s); this }
    def withVentral(s: String)(implicit ev: D =:= YesData): this.type = { vn.add(s); this }
  }
  object DataBuilder {
    private[trackercommons] val emptyD = new Array[Double](0)
    private[trackercommons] val emptyF = new Array[Float](0)
    private[trackercommons] val emptyP = new Array[PerimeterPoints](0)
    private[trackercommons] val emptyW = new Array[PixelWalk](0)
    private[trackercommons] val emptyS = new Array[String](0)
  }

  def worm(id: String) = new DataBuilder[NoData](id)
  def worm(id: String, j: Json.Obj) = (new DataBuilder[NoData](id)).personalCustom(j)

  private[trackercommons] final class JArB() {
    private[this] var i = 0
    private[this] var unarr: Json = null
    private[this] var arr = new Array[Json](1)
    private[this] def free(n: Int) {
      if (n+i > arr.length) {
        val m = math.min(Int.MaxValue - 1, math.max(2*arr.length, n+i))
        arr = java.util.Arrays.copyOf(arr, m)
      }
    }
    private[this] def fill(k: Int) {
      if (i < k) {
        free(k - i)
        if (unarr ne null) while (i < k) { arr(i) = unarr;    i += 1 }
        else               while (i < k) { arr(i) = Json.Null; i += 1}
      }
    }
    def publish(index: Int) {
      if (i == 0 && (unarr ne null)) { arr(0) = unarr; i += 1 }
      else fill(index - i)
    }
    def add(j: Json, index: Int) {
      if (index == 0 && i == 0) unarr = j
      else {
        if (i == index) arr(i-1) = j
        else {
          if (i+1 < index) fill(index-1)
          free(1)
          arr(i) = j
          i = index
        }      
      }
    }
    def result: Array[Json] = java.util.Arrays.copyOf(arr, i)
  }
}
