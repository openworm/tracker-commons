package org.openworm.trackercommons.examples

import org.openworm.trackercommons._

object CircleTravel extends ExampleTemplate[String] {
  val defaultLocation = ""

  def run(args: Array[String]): Either[String, String] = {
    val circle = Create.wcon.
      setMeta(
        Create.meta("circle example").addSoftware( Create.software.name("CircleTravel.scala") )
      ).
      addData({
        val w = Create.worm("1").add(0.0, Array(1.0), Array(0.0))
        for (i <- 1 until 12)
          w.add(i.toDouble, Array(math.cos(i*math.Pi/6)), Array(math.sin(i*math.Pi/6)))
        w
      }).
      setUnits().
      result
    val output = defaultOutput
    ReadWrite.write(circle, output) match {
      case Left(err) => Left(err)
      case _         => Right(output.getPath)
    }
  }

  def succeed(s: String) {
    println(f"Wrote a circularly traveling point animal to $s")
  }
}
