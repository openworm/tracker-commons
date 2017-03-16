package org.openworm.trackercommons.minimal

// A fully self-contained WCON format reader
object Reader {
  import fastparse.all._

  // Begin full JSON parser
  def W[A](p: P[A]) = CharsWhile(_.isWhitespace).? ~ p ~ CharsWhile(_.isWhitespace).?

  val Null = P("null").map(_ => null)
  val Bool = P("true").map(_ => true) | P("false").map(_ => false)

  val Hex = CharIn("0123456789ABCDEFabcdef")
  val Esc = "\\" ~ (
    P("b").map(_ => "\b") |
    P("f").map(_ => "\f") |
    P("n").map(_ => "\n") |
    P("r").map(_ => "\r") |
    P("t").map(_ => "\t") |
    CharIn("\\/\"").! |
    "u" ~ (Hex ~ Hex ~ Hex ~ Hex).!.map(h4 => java.lang.Integer.parseInt(h4, 16).toChar.toString)
  )
  val Str = "\"" ~ (CharsWhile(c => c != '"' && c != '\\').! | Esc).rep.map(_.mkString) ~ "\""

  val Digits = CharsWhile(c => c >= '0' & c <= '9')
  val Num = ("-".? ~ ("0" | Digits) ~ ("." ~ Digits).? ~ (CharIn("eE") ~ CharIn("+-").? ~ Digits).?).!.map(_.toFloat)

  val Arr = P("[" ~ All.rep(sep = ",").map(_.toArray) ~ "]")

  val KeyVal = P(W(Str ~ W(":") ~ All))
  val Obj = "{" ~ KeyVal.rep(sep = ",").map(_.toMap) ~ "}"

  val All: P[Any] = W(Obj | Arr | Num | Str | Bool | Null)
  // End of full JSON parser

  // Begin of extraction of data from Json
  def wormify(entry: Map[String, Any], index: Int): Either[String, Worm] = {
    val hasNot = Seq("id", "t", "x", "y").filterNot(entry contains _)
    if (hasNot.nonEmpty) return Left(hasNot.map(x => s"no $x").mkString("\n"))
    val id = entry("id") match {
      case s: String => s
      case f: Float => if (f.toInt == f) f.toInt.toString else f.toString
      case _ => return Left(s"id is neither a string nor a number")
    }
    val ts = entry("t") match {
      case null => Array(Float.NaN)
      case f: Float => Array(f)
      case a: Array[Any] =>
        val b = a.collect{ case null => Float.NaN; case f: Float => f }
        if (b.length != a.length) return Left(s"found non-numeric value in t")
        b 
      case _ => return Left(s"t is neither a number or array of numbers") 
    }
    val List(oxs, oys): List[Array[Float]] = List( List("ox", "cx"), List("oy", "cy") ).map{
      case q :: qq :: Nil => entry.getOrElse(q, entry.getOrElse(qq, null)) match {
        case null => Array.fill(ts.length)(0f)
        case f: Float => Array.fill(ts.length)(f)
        case a: Array[Any] =>
          val b = a.collect{ case null => Float.NaN; case f: Float => f }
          if (a.length != ts.length || b.length != ts.length) return Left("Origin/centroid size doesn't match timepoint length")
          b
      }
      case _ => throw new Exception("Implementation error.  This pattern match should never fail.")
    }
    val List(xss, yss) = List(("x", oxs), ("y",oys)).map{ case (q,o) => entry(q) match {
      case null => Array(Array(Float.NaN))
      case f: Float => Array(Array(f + o(0)))
      case a: Array[Any] =>
        val b = a.collect{ case null => Float.NaN; case f: Float => f }
        if (b.length == a.length) {
          if (ts.length == 1) {
            for (i <- b.indices) b(i) += o(0)
            Array(b)
          }
          else {
            if (b.length != ts.length) return Left(s"mismatched sizes for t and $q")
            b.zipWithIndex.map{ case (f,i) => Array(f + o(i)) }
          }
        }
        else {
          val c = a.collect{
            case null => Array(Float.NaN)
            case f: Float => Array(f)
            case xs: Array[Any] =>
              val ys = xs.collect{ case null => Float.NaN; case f: Float=> f }
              if (xs.length != ys.length) return Left("found non-numeric value in $q")
              ys
          }
          if (c.length != a.length) return Left("found non-numeric value in $q")
          if (c.length != ts.length) return Left("mismatched sizes for t and $q")
          for {i <- c.indices; j <- c(i).indices} c(i)(j) += o(i)
          c
        }
    }}
    for (i <- xss.indices) if (xss(i).length != yss(i).length) return Left("x and y lengths fail to match at timepoint ${ts(i)}")
    Right(new Worm(id, ts, xss, yss, index))
  }
  // End of extraction of data from JSON

  // Interpretation of JSON as WCON (not counting extraction of data)
  def apply(wcon: String): Either[String, Array[Worm]] = All.parse{wcon} match {
    case rf: Parsed.Failure =>
      Left("Error parsing WCON: not valid JSON.\nParse error:\n" + rf.toString)
    case Parsed.Success(j, _) => j match {
      case m: Map[String @unchecked, Any @unchecked] =>
        if (!m.contains("units")) Left("Error parsing WCON: no units")
        else if (!m("units").isInstanceOf[Map[_,_]]) Left("Error parsing WCON: units is not an object")
        else if (!Seq("t", "x", "y").forall(v => m("units").asInstanceOf[Map[String, Any]] contains v))
          Left("Error parsing WCON: units object must specify units for t, x, and y")
        else if (!m.contains("data")) Left("Error parsing WCON: no data")
        else m("data") match {
          case e: Map[String @unchecked, Any @unchecked] => wormify(e, 0).right.map(worm => Array(worm))
          case js: Array[Any] =>
            val es = js.collect{ case e: Map[String @unchecked, Any @unchecked] => e }
            if (js.length != es.length) Left("Error parsing WCON: data array contains things that are not objects")
            else {
              val perhapsWorms = es.zipWithIndex.map{ case (e,i) => wormify(e, i) -> i }
              val badWorms = perhapsWorms.collect{ case (Left(s), i) => s"  Bad entry $i: $s" }.mkString("\n")
              if (badWorms.nonEmpty) Left("Error parsing WCON: bad data entries\n"+badWorms)
              else Right(
                perhapsWorms.collect{ case (Right(worm), _) => worm }.
                  groupBy(_.id).
                  map{ case (id, worms) => Worm merge worms }.
                  toArray
              )
            }
          case _ => Left("Error parsing WCON: data is neither an object nor an array of objects")
        }
      case _ => Left("Error parsing WCON: not a JSON object")
    }
  }
  // End of interpretation of JSON as WCON
}
