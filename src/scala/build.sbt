lazy val root = (project in file(".")).settings(
  name := "tracker-commons",
  version := "0.1.0",
  scalaVersion := "2.11.7",
  libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test",
  libraryDependencies += "com.lihaoyi" %% "fastparse" % "0.2.1",
  libraryDependencies += "kse" %% "kse" % "0.4-SNAPSHOT"
)
