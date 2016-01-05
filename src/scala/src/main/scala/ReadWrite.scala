package org.openworm.trackercommons

object ReadWrite {
  def readOne(s: String): Either[String, DataSet] = readOne(new java.io.File(s))

  def readOne(f: java.io.File): Either[String, DataSet] = ???

  def readAll(s: String): Either[String, Array[DataSet]] = readAll(new java.io.File(s))

  def readAll(f: java.io.File): Either[String, Array[DataSet]] = readOne(f) match {
    case Left(err) => Left(err)
    case Right(dset) => ???
  }
}
