package org.openworm.trackercommons.examples

object CountAnimals extends ExampleTemplate[Int] {
  val defaultPath = "../../tests/count_animals.wcon"

  def run(args: Array[String]): Either[String, Int] = existingFile(args) match {
    case Left(msg) => Left(msg)
    case Right(file) => Right(-1)
  }

  def succeed(n: Int) {
    println(f"Found $n animals")
  }
}
