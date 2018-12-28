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

  def firstIndex = -prev.length
  def lastIndex = next.length

  def iterator: Iterator[String] = new scala.collection.AbstractIterator[String] {
    private var i = self.firstIndex - 1
    def hasNext = i < self.lastIndex
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

  def linked(fses: Array[FileSet]): Option[FileSet] =
    if (fses.length == 0) None
    else if (fses.forall(_ == empty)) Some(empty)
    else {
      val names = fses.map(_.current)
      if (names.toSet.size != names.length) None
      else Custom.accumulate(fses.map(_.custom)).map(c => new FileSet(names.head, names.tail, noFiles, c))
    }

  def join(sets: Array[FileSet]): Either[String, FileSet] = {
    if (sets.isEmpty) Left("No files")
    else if (sets.length == 1) Right(sets.head)
    else {
      val canonical: Array[FileSet] =
        if (sets.zip(sets.tail).forall{case (l, r) => l(0) == r(-1) && l(1) == r(0) }) sets
        else {
          val m = sets.map(si => si.current -> si).toMap
          val mentioned = sets.flatMap(_.prev).toSet ++ sets.flatMap(_.next).toSet
          if (m.size < sets.size) return Left(f"List of FileSets contains duplicate 'current' filenames!")
          if (!m.forall{ case (k, _) => mentioned contains k})
            return Left(f"Read files that no other file knows about: " + m.map(_._1).filterNot(mentioned).mkString("\n  ", "\n  ", ""))
          if (!mentioned.forall(x => m contains x))
            return Left(f"Files mentioned that were not read: " + mentioned.filterNot(m contains _).mkString("\n  ", "\n  ", ""))
          val head: FileSet = sets.filter(_.prev.isEmpty).toList match {
            case x :: Nil       => x
            case Nil            => return Left(f"FileSets contain cyclical references.")
            case x :: y :: more => return Left(f"Both $x and $y think they should be the first data file")
          }
          Iterator.iterate(Option(head))(si => si.flatMap(x => x.next.headOption.flatMap(m get _))).
            takeWhile(_.isDefined).
            take(sets.size + 1).   // If there's some sort of loop, this will cut it short while still signaling it
            flatten.toArray
        }
      if (!(canonical.length == sets.size)) return Left(f"Found a chain of ${canonical.length} file sets but expected ${sets.size}")
      canonical.zipWithIndex.foreach{ case (fs, i) =>
        if (i > 0) {
          if (fs.prev.isEmpty) return Left(f"${fs.current} is missing link to previous file ${canonical(i-1).current}")
          fs.prev.zipWithIndex.foreach{ case (ps, j) =>
            if (i-j < 1)
              return Left(f"${fs.current} claims $ps should be before ${canonical.head.current}")
            else if (ps != canonical(i-j-1).current)
              return Left(f"Disagreement about whether file #${i-j} is $ps or ${canonical(i-j-1).current}")
          }
        }
        if (i+1 < canonical.length) {
          if (fs.next.isEmpty) return Left(f"${fs.current} is missing link to next file ${canonical(i+1).current}")
          fs.next.zipWithIndex.foreach{ case (ns, j) =>
            if (i+j+1 >= canonical.length)
              return Left(f"${fs.current} claims $ns should be before ${canonical.last.current}")
            else if (ns != canonical(i+j+1).current)
              return Left(f"Disagreement about whether file #${i+j+2} is $ns or ${canonical(i+j+1).current}")
          }
        }
      }
      val customMap = collection.mutable.AnyRefMap.empty[String, Json]
      canonical.foreach(o => o.custom.iterator.foreach{ case (k, v) => 
        if (customMap contains k) {
          if (customMap(k) != v) customMap(k) = Json.Null
        }
        else customMap(k) = v
      })
      Right(new FileSet(canonical.head.current, canonical.tail.map(_.current), Array.empty, Json.Obj(customMap)))
    }
  }
}
