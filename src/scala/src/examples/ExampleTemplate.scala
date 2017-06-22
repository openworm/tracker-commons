package org.openworm.trackercommons.examples


case class Selected(count: Int, where: String) {}

trait ExampleTemplate[A] {
  def run(args: Array[String]): Either[String, A]
  def succeed(a: A): Unit
  def defaultLocation: String
  def defaultOutput: java.io.File = (new java.io.File("result.wcon")).getCanonicalFile

  // Expect `run` to call this to get a single file
  def existingFile(args: Array[String]): Either[String, java.io.File] = {
    val theFileName = args.headOption.getOrElse(defaultLocation)
    val theFile = new java.io.File(theFileName)
    if (!theFile.exists) {
      Left(Seq(
        f"Could not find ${theFile.getPath}",
        f"Are you running this from the correct directory?"
      ).mkString("\n"))
    }
    else Right(theFile)
  }

  def main(args: Array[String]) {
    run(args) match {
      case Left(msg) => println(msg); sys.exit(1)
      case Right(a)  => succeed(a)
    }
  }
}
