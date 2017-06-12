package org.openworm.trackercommons

import java.io.File
import java.nio.file.Path

import scala.util._
import scala.util.control.NonFatal

import kse.jsonal._
import kse.jsonal.JsonConverters._

case class FileSet(
  current: String,
  next: Array[String] = FileSet.noFiles,
  prev: Array[String] = FileSet.noFiles,
  custom: Json.Obj = Json.Obj.empty
)
extends AsJson with Customizable[FileSet] { self =>
  def customFn(f: Json.Obj => Json.Obj) = copy(custom = f(custom))

  def hasNext: Boolean = next.nonEmpty
  def hasPrev: Boolean = prev.nonEmpty

  def currentOne(parent: Path): Path = parent resolve current
  def nextOne(parent: Path): Path =
    if (hasNext) parent resolve next.head
    else throw new NoSuchElementException("No next file after "+current)
  def prevOne(parent: Path): Path = 
    if (hasPrev) parent resolve prev.head
    else throw new NoSuchElementException("No prev file before "+current)

  def apply(i: Int): Option[String] = 
    if (i == 0) Some(current)
    else if (i < 0 && -i <= prev.length) Some(prev(-i-1))
    else if (i > 0 && i <= next.length) Some(next(i-1))
    else None

  def pathed(parent: Path, i: Int): Option[Path] = apply(i).map(parent resolve _)

  def firstIndex = 0 - next.length
  def lastIndex = prev.length

  def iterator: Iterator[String] = new scala.collection.AbstractIterator[String] {
    private var i = firstIndex - 1
    def hasNext = i < lastIndex
    def next = { i += 1; self(i).get }
  }

  def json = Json ~ ("current", current) ~? ("next", next) ~? ("prev", prev) ~~ custom ~ Json

  override def toString = f"FileSet($current, ${next.mkString("[", ", ", "]")}, ${prev.mkString("[", ", ", "]")}, $custom)"

  val size = 1 + next.length + prev.length
}
object FileSet extends FromJson[FileSet] {
  private def BAD(msg: String): Either[JastError, Nothing] = Left(JastError("Invalid files specification: " + msg))

  val noFiles = new Array[String](0)
  val empty = new FileSet("", noFiles, noFiles, Json.Obj.empty)

  def parse(j: Json): Either[JastError, FileSet] = {
    val o = j match { 
      case jo: Json.Obj => jo
      case _ => return Left(JastError("Not a file specification because not a JSON object"))
    }
    val current = o("current") match {
      case Json.Str(text) => text
      case _ => return Left(JastError("'current' entry not found or not a string"))
    }
    val List(next, prev) = List("next", "prev").map{ key => o.get(key) match {
      case None => Array.empty[String]
      case Some(Json.Null) => Array.empty[String]
      case Some(s: Json.Str) => Array(s.text)
      case Some(jaa: Json.Arr.All) => jaa.to[Array[String]] match {
        case Right(x) => x
        case Left(je) => return Left(JastError(f"Did not find strings of file names for key $key in file information", because = je))
      }
      case _ => return Left(JastError(f"Did not find strings of file names for $key key in file information"))
    }}
    Right(new FileSet(current, next, prev, o.filter((k, _) => k.startsWith("@"))))
  }

  def join(sets: Array[FileSet]): Either[String, FileSet] = {
    if (sets.isEmpty) Left("No files")
    else if (sets.length == 1) Right(sets.head)
    else {
      val ordered =
        if (sets.zip(sets.tail).forall{case (l, r) => l(0) == r(-1) && l(1) == r(0) }) sets
        else {
          val m = sets.map(si => si(0) -> si).toMap
          if (m.size < sets.size) return Left(f"List of FileSets contains duplicate 'current' filenames!")
          val v0 = sets.head
          val vfwd = Iterator.
            iterate(Option(v0))(si => si.flatMap(x => m get x(1))).
            takeWhile(_.isDefined).
            flatten.drop(1).toArray
          val vbkw = Iterator.
            iterate(Option(v0))(si => si.flatMap(x => m get x(-1))).
            takeWhile(_.isDefined).
            flatten.drop(1).toArray
          vbkw.reverse ++ Array(v0) ++ vfwd
        }
      if (!(ordered.length == sets.size)) return Left(f"Only found ${ordered.length} of ${sets.size} self-consistent file sets")
      val ozi = ordered.zipWithIndex
      val earliest = ozi.map{ case (o, i) => o.firstIndex + i }.min
      val latest   = ozi.map{ case (o, i) => o.lastIndex  + i }.max
      val canonical = new Array[String](1 + latest - earliest)
      val oit = ozi.iterator
      while (oit.hasNext) {
        val (o, i) = oit.next
        var j = o.firstIndex
        while (j <= o.lastIndex) {
          val k = i + j - earliest
          if (canonical(k) eq null) canonical(k) = o(j).get
          else if (o(j).get != canonical(k)) return Left("Mismatch in file sets.  ${o.current} claims file should be ${o(j).get} but ${canonical(k)} already found")
          j += 1
        }
      }
      val customMap = collection.mutable.AnyRefMap.empty[String, Json]
      ordered.foreach(o => o.custom.iterator.foreach{ case (k, v) => 
        if (customMap contains k) {
          if (customMap(k) != v) customMap(k) = Json.Null
        }
        else customMap(k) = v
      })
      Right(new FileSet(canonical.head, canonical.tail, Array.empty, Json.Obj(customMap)))
    }
  }
}
