package org.openworm.trackercommons

import kse.jsonal._
import kse.jsonal.JsonConverters._

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

  final class MakeWcon[U <: HasUnits, D <: HasData, F <: HasFile] private[trackercommons] (building: DataSet, nData: Int) {
    private[this] var stale = false

    def result(implicit evU: U =:= YesUnits, evD: D =:= YesData) =
      building.copy(data = java.util.Arrays.copyOf(building.data, nData))

    def write(implicit evU: U =:= YesUnits, evD: D =:= YesData, evF: F =:= YesFile): scala.util.Try[Unit] = scala.util.Try{
      ReadWrite.write(result, building.files.lookup(0).get)
    }

    def setUnits(u: UnitMap): MakeWcon[YesUnits, D, F] = {
      stale = true
      new MakeWcon[YesUnits, D, F](building.copy(unitmap = u), nData)
    }
    def setUnits(extra: Map[String, String]): Either[String, MakeWcon[YesUnits, D, F]] = {
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
      if (building.meta.arena.exists(_.diameter match { case Left(_) => true; case Right(x) => !x.isNaN })) adopt("diameter", "x")
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
    def setUnits(): MakeWcon[YesUnits, D, F] = setUnits(Map.empty[String, String]).right.get   // Always succeeds when no extras

    def setFile(files: FileSet): MakeWcon[U, D, YesFile] = {
      stale = true
      new MakeWcon[U, D, YesFile](building.copy(files = files), nData)
    }
    def setFile(file: java.io.File): MakeWcon[U, D, YesFile] = {
      val fs = FileSet(Vector(file.getName), 0, Json.Obj.empty)
      fs.setRootFile(file)
      setFile(fs)
    }
    def setFile(file: String): MakeWcon[U, D, YesFile] = setFile(new java.io.File(file))

    def setMeta(meta: Metadata): MakeWcon[U, D, F] = { stale = true; new MakeWcon[U, D, F](building.copy(meta = meta), nData) }
    def setMeta(meta: MakeMeta): MakeWcon[U, D, F] = setMeta(meta.result)

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

  final class MakeMeta private[trackercommons] (val result: Metadata) {
    def addLab(lab: Laboratory): MakeMeta =
      if (lab.isEmpty) this
      else new MakeMeta(result.copy(lab = result.lab :+ lab))
    def addLab(lab: MakeLab): MakeMeta = addLab(lab.result)
    def setLab(labs: Seq[Laboratory]) = new MakeMeta(result.copy(lab = labs.toVector))
    def dropLabs = if (result.lab.isEmpty) this else new MakeMeta(result.copy(lab = Vector.empty))

    def addWho(who: String) = if (who.isEmpty) this else new MakeMeta(result.copy(who = result.who :+ who))
    def setWho(whos: Seq[String]) = new MakeMeta(result.copy(who = whos.toVector))
    def dropWhos = if (result.who.isEmpty) this else new MakeMeta(result.copy(who = Vector.empty))

    def setTime(time: java.time.OffsetDateTime) = new MakeMeta(result.copy(timestamp = Some(Left(time))))
    def setTime(time: java.time.LocalDateTime) = new MakeMeta(result.copy(timestamp = Some(Right(time))))
    def dropTime = if (result.timestamp.isEmpty) this else new MakeMeta(result.copy(timestamp = None))

    def setTemp(temperature: Double) = new MakeMeta(result.copy(temperature = Some(temperature).filter(x => !x.isNaN && !x.isInfinite)))
    def dropTemp = if (result.temperature.isEmpty) this else new MakeMeta(result.copy(temperature = None))

    def setHumidity(humidity: Double) = new MakeMeta(result.copy(humidity = Some(humidity).filter(x => !x.isNaN && !x.isInfinite)))
    def dropHumidity = if (result.humidity.isEmpty) this else new MakeMeta(result.copy(humidity = None))

    def setArena(arena: Arena): MakeMeta = if (arena.isEmpty) dropArena else new MakeMeta(result.copy(arena = Some(arena)))
    def setArena(arena: MakeArena): MakeMeta = setArena(arena.result)
    def dropArena = if (result.arena.isEmpty) this else new MakeMeta(result.copy(arena = None))

    def setFood(food: String): MakeMeta = if (food.isEmpty) dropFood else new MakeMeta(result.copy(food = Some(food)))
    def setFood(): MakeMeta = setFood("OP50")
    def dropFood: MakeMeta = if (result.food.isEmpty) this else new MakeMeta(result.copy(food = None))

    def setMedia(media: String): MakeMeta = if (media.isEmpty) dropMedia else new MakeMeta(result.copy(media = Some(media)))
    def setMedia(): MakeMeta = setMedia("NGM")
    def dropMedia: MakeMeta = if (result.media.isEmpty) this else new MakeMeta(result.copy(media = None))

    def setSex(sex: String): MakeMeta = if (sex.isEmpty) dropSex else new MakeMeta(result.copy(sex = Some(sex)))
    def setSex(): MakeMeta = setSex("hermaphrodite")
    def dropSex: MakeMeta = if (result.sex.isEmpty) this else new MakeMeta(result.copy(sex = None))

    def setStage(stage: String) = if (stage.isEmpty) dropStage else new MakeMeta(result.copy(stage = Some(stage)))
    def dropStage = if (result.stage.isEmpty) this else new MakeMeta(result.copy(stage = None))

    def setAge(age: Double) = new MakeMeta(result.copy(age = Some(age).filter(x => !x.isNaN && !x.isInfinite)))
    def dropAge = if (result.age.isEmpty) this else new MakeMeta(result.copy(age = None))

    def setStrain(strain: String) = if (strain.isEmpty) dropStrain else new MakeMeta(result.copy(strain = Some(strain)))
    def dropStrain = if (result.strain.isEmpty) this else new MakeMeta(result.copy(strain = None))

    def addProtocol(protocol: String): MakeMeta =
      if (protocol.isEmpty) this
      else new MakeMeta(result.copy(protocol = result.protocol :+ protocol))
    def setProtocol(protocols: Seq[String]): MakeMeta = new MakeMeta(result.copy(protocol = protocols.toVector))
    def dropProtocols = if (result.protocol.isEmpty) this else new MakeMeta(result.copy(protocol = Vector.empty))

    def addSoftware(software: Software): MakeMeta =
      if (software.isEmpty) this
      else new MakeMeta(result.copy(software = result.software :+ software))
    def addSoftware(software: MakeSoft): MakeMeta = addSoftware(software.result)
    def setSoftware(softwares: Seq[Software]): MakeMeta = new MakeMeta(result.copy(software = softwares.toVector))
    def dropSoftware = if (result.software.isEmpty) this else new MakeMeta(result.copy(software = Vector.empty))

    def setSettings(settings: Json) = new MakeMeta(result.copy(settings = Some(settings)))
    def dropSettings = if (result.settings.isEmpty) this else new MakeMeta(result.copy(settings = None))

    def putCustom(key: String, value: Json) = new MakeMeta(result.copy(custom = Json.Obj(result.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeMeta(result.copy(custom = custom))
    def dropCustom = if (result.custom.size == 0) this else new MakeMeta(result.copy(custom = Json.Obj.empty))
  }

  def meta() = new MakeMeta(Metadata.empty)

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

    def kind(s: String) = result.copy(kind = s)
    def orient(s: String) = result.copy(orient = s)
    def diameter(d: Double) = result.copy(diameter = Right(d))
    def diameter(d1: Double, d2: Double) = result.copy(diameter = Left((d1, d2)))

    def putCustom(key: String, value: Json) = new MakeArena(result.copy(custom = Json.Obj(result.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeArena(result.copy(custom = custom))
    def dropCustom = if (result.custom.size == 0) this else new MakeArena(result.copy(custom = Json.Obj.empty))
  }

  def arena() = new MakeArena(Arena.empty)

  final class MakeSoft private[trackercommons] (val result: Software) {
    def isEmpty = result.isEmpty

    def name(s: String) = new MakeSoft(result.copy(name = s))
    def version(s: String) = new MakeSoft(result.copy(version = s))

    def addFeature(s: String) = new MakeSoft(result.copy(featureID = result.featureID + (if (s.startsWith("@")) s else "@" + s)))
    def setFeatures(ss: Iterable[String]) =
      new MakeSoft(result.copy(featureID = ss.map(s => if (s.startsWith("@")) s else "@" + s).toSet))
    def dropFeatures = new MakeSoft(result.copy(featureID = Set.empty))

    def putCustom(key: String, value: Json) = new MakeSoft(result.copy(custom = Json.Obj(result.custom.asMap + ((key, value)))))
    def setCustom(custom: Json.Obj) = new MakeSoft(result.copy(custom = custom))
    def dropCustom = if (result.custom.size == 0) this else new MakeSoft(result.copy(custom = Json.Obj.empty))
  }

  def software() = new MakeSoft(Software.empty)

  final class DataBuilder[D <: HasData](id: String) {
    private[this] var i = 0
    private[this] var ts = new Array[Double](1)
    private[this] var xs, ys = new Array[Array[Float]](1)
    private[this] var rxs, rys = new Array[Double](1)
    private[this] def txyFree(n: Int) {
      if (n+i > ts.length) {
        val m = math.min(Int.MaxValue - 1, math.max(2*ts.length, n+i))
        ts = java.util.Arrays.copyOf(ts, m)
        xs = java.util.Arrays.copyOf(xs, m)
        ys = java.util.Arrays.copyOf(ys, m)
        rxs = java.util.Arrays.copyOf(rxs, m)
        rys = java.util.Arrays.copyOf(rys, m)
      }
    }
    private[this] def txyAdd(t: Double, x: Array[Double], y: Array[Double]) {
      txyFree(1)
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
    private[this] def tsGet: Array[Double] = java.util.Arrays.copyOf(ts, i)
    private[this] def xsGet: Array[Array[Float]] = java.util.Arrays.copyOf(xs, i)
    private[this] def ysGet: Array[Array[Float]] = java.util.Arrays.copyOf(ys, i)
    private[this] def rxsGet: Array[Double] = java.util.Arrays.copyOf(rxs, i)
    private[this] def rysGet: Array[Double] = java.util.Arrays.copyOf(rys, i)

    private[this] var oi = 0
    private[this] var oxs, oys = DataBuilder.emptyD
    private[this] def oFree(n: Int) {
      if (n+oi > oxs.length) {
        val m = math.min(Int.MaxValue - 1, math.max(2*oxs.length, n+oi))
        oxs = java.util.Arrays.copyOf(oxs, m)
        oys = java.util.Arrays.copyOf(oys, m)
      }
    }
    private[this] def oFill(k: Int) {
      if (oi < k) {
        oFree(k - oi)
        while (oi < k) {
          oxs(oi) = Double.NaN
          oys(oi) = Double.NaN
          oi += 1
        }
      }
    }
    private[this] def oAdd(ox: Double, oy: Double) {
      if (oi > 0 || !(ox.isNaN || oy.isNaN || ox.isInfinite || oy.isInfinite)) {
        oFill(i-1)
        oFree(1)
        oxs(oi) = ox
        oys(oi) = oy
        oi += 1
      }
    }
    private[this] def oxsGet: Array[Double] = if (oi > 0) java.util.Arrays.copyOf(oxs, oi) else DataBuilder.emptyD
    private[this] def oysGet: Array[Double] = if (oi > 0) java.util.Arrays.copyOf(oys, oi) else DataBuilder.emptyD

    private[this] var ci = 0
    private[this] var cxs, cys = DataBuilder.emptyD
    private[this] def cFree(n: Int) {
      if (n+ci > cxs.length) {
        val m = math.min(Int.MaxValue - 1, math.max(2*cxs.length, n+ci))
        cxs = java.util.Arrays.copyOf(cxs, m)
        cys = java.util.Arrays.copyOf(cys, m)
      }
    }
    private[this] def cFill(k: Int) {
      if (ci < k) {
        cFree(k - ci)
        while (ci < k) {
          cxs(ci) = Double.NaN
          cys(ci) = Double.NaN
          ci += 1
        }
      }
    }
    private[this] def cAdd(cx: Double, cy: Double) {
      if (ci > 0 || !(cx.isNaN || cy.isNaN || cx.isInfinite || cy.isInfinite)) {
        cFill(i-1)
        cFree(1)
        cxs(ci) = cx
        cys(ci) = cy
        ci += 1
      }
    }
    private[this] def cxsGet: Array[Double] = if (ci > 0) java.util.Arrays.copyOf(cxs, ci) else DataBuilder.emptyD
    private[this] def cysGet: Array[Double] = if (ci > 0) java.util.Arrays.copyOf(cys, ci) else DataBuilder.emptyD

    private[this] var pi = 0
    private[this] var ps = DataBuilder.emptyP
    private[this] def pFree(n: Int) {
      if (n+pi > ps.length) {
        val m = math.min(Int.MaxValue - 1, math.max(2*ps.length, n+pi))
        ps = java.util.Arrays.copyOf(ps, m)
      }
    }
    private[this] def pFill(k: Int) {
      if (pi < k) {
        pFree(k - pi)
        while (pi < k) {
          ps(pi) = PerimeterPoints.empty
          pi += 1
        }
      }
    }
    private[this] def pAdd(p: PerimeterPoints) {
      if (p.size > 0 || pi > 0) {
        pFill(i-1)
        pFree(1)
        ps(pi) = p
        pi += 1
      }
    }
    private[this] def pGet: Option[Array[PerimeterPoints]] = if (pi > 0) Some(java.util.Arrays.copyOf(ps, pi)) else None

    private[this] var wi = 0
    private[this] var ws = DataBuilder.emptyW
    private[this] def wFree(n: Int) {
      if (n+wi > ws.length) {
        val m = math.min(Int.MaxValue - 1, math.max(2*ws.length, n+wi))
        ws = java.util.Arrays.copyOf(ws, m)
      }
    }
    private[this] def wFill(k: Int) {
      if (wi < k) {
        wFree(k - wi)
        while (wi < k) {
          ws(wi) = PixelWalk.empty
          wi += 1
        }
      }
    }
    private[this] def wAdd(w: PixelWalk) {
      if (w.size > 0 || wi > 0) {
        wFill(i-1)
        wFree(1)
        ws(wi) = w
        wi += 1
      }
    }
    private[this] def wGet: Option[Array[PixelWalk]] = if (wi > 0) Some(java.util.Arrays.copyOf(ws, wi)) else None

    private[this] var jm: collection.mutable.AnyRefMap[String, JArB] = null
    private[this] def jAdd(key: String, value: Json) {
      if (jm eq null) jm = new collection.mutable.AnyRefMap[String, JArB]
      jm.getOrElseUpdate(key, new JArB).add(value, i)
    }
    private[this] def jGet = 
      if (jm eq null) Json.Obj.empty
      else Json.Obj(jm.mapValues(b => Json(b.result)).toMap)

    def validate: Either[DataBuilder[NoData], DataBuilder[YesData]] =
      if (i == 0) Left(this.asInstanceOf[DataBuilder[NoData]])
      else Right(this.asInstanceOf[DataBuilder[YesData]])

    def result(implicit ev: D =:= YesData): Data = {
      if (oi > 0) oFill(i)
      if (ci > 0) cFill(i)
      if (pi > 0) pFill(i)
      if (wi > 0) wFill(i)
      if (jm ne null) jm.foreach{ case (_, j) => j.publish(i) }
      val t = tsGet
      val ox = oxsGet
      val oy = oysGet
      val oneT = t.length == 1
      Data(id, t, xsGet, ysGet, cxsGet, cysGet, ox, oy, pGet, wGet, jGet)(rxsGet, rysGet, oneT)
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
      txyAdd(t, xs, ys)
      var rxi = rxs(i-1)
      var ryi = rys(i-1)
      cAdd(cx, cy)
      if ((pxs ne null) && (pys ne null) && pxs.length > 0 && pys.length > 0) {
        val pxi, pyi = new Array[Float](math.min(pxs.length, pys.length))
        var j = 0; while (j < pxi.length) { pxi(j) = (pxs(j) - rxi).toFloat; pyi(j) = (pys(j) - ryi).toFloat; j += 1 }
        pAdd(PerimeterPoints(pxi, pyi, if (ptail >= 0) Some(ptail) else None)(rxi, ryi))
      }
      if (w ne null) wAdd(w)
      oAdd(ox, oy)
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
  }
  object DataBuilder {
    private[trackercommons] val emptyD = new Array[Double](0)
    private[trackercommons] val emptyF = new Array[Float](0)
    private[trackercommons] val emptyP = new Array[PerimeterPoints](0)
    private[trackercommons] val emptyW = new Array[PixelWalk](0)
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
