package org.openworm.trackercommons.minimal

case class Worm(id: String, ts: Array[Float], xss: Array[Array[Float]], yss: Array[Array[Float]], index: Int) {
  def ++(w: Worm) = new Worm(id, ts ++ w.ts, xss ++ w.xss, yss ++ w.yss, math.min(index, w.index))
}
object Worm {
  def merge(ws: Array[Worm]) = if (ws.length == 1) ws.head else {
    val ws2 = ws.sortBy(_.ts(0))
    new Worm(ws.head.id, ws2.flatMap(_.ts), ws2.flatMap(_.xss), ws2.flatMap(_.yss), ws.map(_.index).min)
  }
}
