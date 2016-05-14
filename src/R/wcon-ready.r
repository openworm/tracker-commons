library(rscala)
si = scalaInterpreter("target/WconR-0.1.0.jar")
si %~% "import kse.flow._, kse.coll._, kse.maths._, kse.eio._"
si %~% "import kse.jsonal._, kse.jsonal.JsonConverters._"
si %~% "import org.openworm.trackercommons.{compatibility => ct, _}"

