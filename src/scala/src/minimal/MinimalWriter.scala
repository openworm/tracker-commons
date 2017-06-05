package org.openworm.trackercommons.minimal

object Writer {
  def apply(x: Float): String = 
    if (x.isNaN || x.isInfinite) "null"
    else "%.4f".format(x).reverse.dropWhile(c => c == '0' || c == '.').reverse

  def apply(xs: Array[Float]): String =
    if (xs.length == 1) apply(xs.head)
    else "[" + xs.map(x => apply(x)).mkString(",") + "]"

  def apply(xss: Array[Array[Float]], indent: Int = -1): String = 
    if (indent < 0) "[ " + xss.map(xs => apply(xs)).mkString(", ") + " ]"
    else {
      val sp = " "*indent
      xss.map(xs => sp + "  " + apply(xs)).mkString("[\n", ",\n", "\n"+sp+"]")
    }

  def apply(worm: Worm, indent: Int): String = {
    val sp = " "*indent
    if (worm.ts.length <= 1) sp + s"""{ "id":${worm.id}, "t":${apply(worm.ts)}, "x":${apply(worm.xss)}, "y":${apply(worm.yss)} }"""
    else
s"""$sp{
$sp  "id": ${worm.id},
$sp  "t": ${apply(worm.ts)},
$sp  "x": ${apply(worm.xss, indent+2)},
$sp  "y": ${apply(worm.yss, indent+2)}
$sp}"""
  }

  def apply(worms: Array[Worm]): String =
s"""{
  "units": {"t": "s", "x": "mm", "y": "mm"},
  "data": [
${worms.map(w => apply(w, 4)).mkString(",\n")}
  ]
}"""
}
