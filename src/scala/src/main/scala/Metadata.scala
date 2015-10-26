package org.openworm.trackercommons

case class Laboratory(pi: String, name: String, location: String) {}

case class Temperature(experimental: Double, cultivation: Double) {}

case class Arena(kind: String, diameterA: Double, diameterB: Double = Double.NaN) {}

case class Software(name: String, version: String, featureID: String) {}

case class Metadata(
  lab: Vector[Laboratory],
  who: Vector[String],
  timestamp: Option[(java.time.LocalDateTime, String)],
  temperature: Option[Temperature],
  humidity: Option[Double],
  arena: Option[Arena],
  food: Option[String],
  media: Option[String],
  sex: Option[String],
  stage: Option[String],
  age: Option[java.time.Duration],
  strain: Option[String],
  protocol: Vector[String],
  software: Option[Software],
  settings: Option[Any],
  custom: Map[String, Any]
) {}
