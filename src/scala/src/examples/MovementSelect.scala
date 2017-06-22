package org.openworm.trackercommons.examples

import org.openworm.trackercommons._

object MovementSelect extends ExampleTemplate[Selected] {
  val defaultLocation = "../../tests/examples/all_movements.wcon"

  def sq(x: Double) = x*x

  def movesFar(d: Data): Boolean = {
    if (d.ts.length < 2) return false

    val cx, cy, l = new Array[Double](d.ts.length)
    for (i <- l.indices) {
      val ni = d.spineN(i)
      if (ni < 1) {
        cx(i) = Double.NaN
        cy(i) = Double.NaN
        l(i) = 0
      }
      else {
        cx(i) = if (d.cxs.length == 0) (d.x(i, 0) + d.x(i, ni-1))/2 else d.cxs(i)
        cy(i) = if (d.cys.length == 0) (d.y(i, 0) + d.y(i, ni-1))/2 else d.cys(i)
        l(i) = math.sqrt(sq(d.x(i, 0) - d.x(i, ni-1)) + sq(d.y(i, 0) - d.y(i, ni-1)))
      }
    }

    val longest = l.max
    if (longest == 0) return false     // Reject worms without a size

    var i0 = 0
    while (i0 < cx.length && (cx(i0).isNaN || cy(i0).isNaN)) i0 += 1  // i0 is at first valid position
    var i1 = cx.length -1
    while (i1 > i0 && (cx(i1).isNaN || cy(i1).isNaN)) i1 -= 1  // i1 at last valid position
    if (i1 <= i0) return false   // Didn't go anywhere valid

    var distmax = 0.0
    var i = i0 + 1
    while (i <= i1) {
      while (cx(i).isNaN || cy(i).isNaN) i += 1
      val dist = sq(cx(i) - cx(i0)) + sq(cy(i) - cy(i0))
      if (dist > distmax) distmax = dist
      i += 1
    }
    distmax = math.sqrt(distmax)

    distmax > longest  // We have to have traveled at least one nose-to-tail distance
  }

  def run(args: Array[String]): Either[String, Selected] = existingFile(args) match {
    case Left(msg) => Left(msg)
    case Right(file) =>
      ReadWrite.readOne(file) match {
        case Left(err) => Left(err.toString)
        case Right(ds) =>
          val selected = ds.groupByIDs().flatMap(d => if (movesFar(d)) Some(d) else None)
          val output = defaultOutput
          ReadWrite.write(selected, output) match {
            case Left(err) => Left(err)
            case _         => Right(
              Selected(selected.data.length, output.getPath)
            )
          }
      }
  }

  def succeed(s: Selected) {
    println(f"Found ${s.count} records; wrote to ${s.where}")
  }
}
