package org.openworm.trackercommons.examples

import org.openworm.trackercommons._

object CountAnimals extends ExampleTemplate[Int] {
  val defaultLocation = "../../tests/examples/count_animals.wcon"

  def run(args: Array[String]): Either[String, Int] = existingFile(args) match {
    case Left(msg) => Left(msg)
    case Right(file) =>
      ReadWrite.readOne(file) match {
        case Left(err) => Left(err.toString)
        case Right(ds) => Right(ds.data.map(_.id).toSet.size)
      }
  }

  def succeed(n: Int) {
    println(f"Found $n animals")
  }
}
