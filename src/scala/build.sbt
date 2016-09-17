lazy val root = (project in file(".")).settings(
  name := "tracker-commons",
  version := "0.1.0",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation"),
  libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test",
  libraryDependencies += "com.lihaoyi" %% "fastparse" % "0.2.1",
  libraryDependencies += "com.github.ichoran" %% "kse" % "0.5.0"
)
