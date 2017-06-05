package org.openworm.trackercommons.examples

trait ExampleTemplate[A] {
  def run(args: Array[String]): Either[String, A]
  def succeed(a: A): Unit
  def defaultPath: String

  def existingFile(args: Array[String]): Either[String, java.io.File] = {
    val theFileName = args.headOption.getOrElse(defaultPath)
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
      case Right(n)  => 
    }
  }
}
