package org.openworm.trackercommons

import fastparse.all._
import json._

object Parser {

  import Parts.{W, Digit => D}

  val Commons = "\"tracker-commons\"" ~ W(":") ~ "true"

  val Date = P(
    (D ~ D ~ D ~ D).! ~ "-" ~ (D ~ D).! ~ "-" ~ (D ~ D).! ~ "T" ~                   // Date
    (D ~ D).! ~         ":" ~ (D ~ D).! ~ ":" ~ (D ~ D ~ ("." ~ D.rep(1)).?).! ~    // Time
    ("Z" | (CharIn("+-") ~ D ~ D ~ ":" ~ D ~ D)).!.?                                // Locale
  ).map{ case (y,mo,d,h,mi,s,loc) => 
    val ss = s.toDouble
    val ssi = math.floor(ss).toInt
    val ssns = math.rint((ss - ssi)*1e9).toInt
    (java.time.LocalDateTime.of(y.toInt, mo.toInt, d.toInt, h.toInt, mi.toInt, ssi, ssns), loc.getOrElse(""))
  }

  val Age = P((D rep 1).! ~ ":" ~ (D ~ D).! ~ ":" ~ (D ~ D ~ ("." ~ D.rep(1)).?).!).map{ case (h,m,s) => 
    val ss = s.toDouble
    val ssi = math.floor(ss).toInt
    val ssns = math.rint((ss - ssi)*1e9).toInt
    java.time.Duration.ZERO.withSeconds(h.toLong*3600L + m.toInt*60 + ssi).withNanos(ssns)
  }

  val UnitKV = Struct.Str ~ W(":" ~! Pass) ~ Struct.Str

  val Units = {
    val u = new collection.mutable.AnyRefMap[String, units.Units]
    P(
      "\"units\"" ~ W(":") ~ "{" ~ 
      W(UnitKV.filter{ case (k,v) => units.Units(v.value) match { case None => false; case Some(vi) => u += ((k.value, vi)); true }}).rep(sep = W(",")) ~
      "}" 
    ).
    map(_ => u: collection.Map[String, units.Units]).
    filter(u => Seq("t", "x", "y").forall(u contains _))
  }

  def durationFormat(jtd: java.time.Duration) = "%d:%02d:%07.4f".format(jtd.getSeconds/3600, (jtd.getSeconds % 3600)/60, (jtd.getSeconds % 60) + 1e-9*jtd.getNano)
}
