package org.openworm.trackercommons.json

import fastparse.all._

object Parts {
  val White = P( CharsWhile(_.isWhitespace) )
  def W[A](p: P[A]): P[A] = White.? ~ p ~ White.?
  val Hex = CharIn("0123456789ABCDEFabcdef")
  val Digit = CharIn("0123456789")
  val Digits = CharsWhile(c => c >= '0' && c <= '9')
}

object Text {
  import Parts._

  val Null = P("null")

  val Bool = "true" | "false"

  val Str = P(
    "\"" ~
      ( CharsWhile(c => c != '"' && c != '\\') |
        ("\\" ~ (("u" ~ Hex ~ Hex ~ Hex ~ Hex) | CharIn("\"\\/bfnrt"))
        )
      ).rep ~
    "\""
  )

  val Num = ("-".? ~! ("0" ~! Pass | Digits) ~! ("." ~ Digits).? ~ (CharIn("eE") ~ CharIn("+-").? ~ Digits).?)

  val Arr = P( "[" ~ W(Val.rep(sep = W(","))) ~ "]" )

  val KeyVal = P( Str ~ W(":") ~! Val )
  val Obj = P( "{" ~ W(KeyVal.rep(sep = W(","))) ~ "}" )

  val Val: P[Unit] = P( Obj | Arr | Str | Num | Bool | Null )
  val All = W(Val.!)
}

object Data {
  import Parts._

  val Null = Text.Null.map(_ => null)
  val Bool = P("true").map(_ => true) | P("false").map(_ => false)

  val Esc = "\\" ~ (
    "u" ~ (Hex ~ Hex ~ Hex ~ Hex).!.map(h4 => java.lang.Integer.parseInt(h4, 16).toChar.toString) |
    CharIn("bfnrt/\\\"").!.map{ case "b" => "\b"; case "f" => "\f"; case "n" => "\n"; case "r" => "\r"; case "t" => "\t"; case x => x }
  )
  val Str = "\"" ~ (CharsWhile(c => c != '"' && c != '\\').! | Esc).rep.map(_.mkString) ~ "\""

  val Num = Text.Num.!.map(_.toFloat)

  val Arr = P("[" ~ All.rep(sep = ",").map(_.toArray) ~ "]")

  val KeyVal = P(W(Str ~ W(":") ~! Val))
  val Obj = "{" ~ KeyVal.rep(sep = ",").map(_.toMap) ~ "}"

  val Val: P[Any] = P( Obj | Arr | Str | Num | Bool | Null )
  val All = W(Val)
}
