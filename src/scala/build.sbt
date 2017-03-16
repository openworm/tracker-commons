// Everything uses these settings
lazy val minimalSettings = Seq(
    scalaVersion := "2.12.1",
    scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation"),
    libraryDependencies += "com.lihaoyi" %% "fastparse" % "0.4.2"
  )

// Everything but the minimal implementation uses these
lazy val commonSettings = minimalSettings ++ Seq(
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies += "com.github.ichoran" %% "kse" % "0.6-SNAPSHOT"
  )

// How to build the minimal version of the WCON reader
lazy val minimal: Project = (project in file("src/minimal")).
  settings(minimalSettings: _*).
  settings(
    name := "tracker-commons-minimal",
    version := "0.1.0"
  )

// The main project
lazy val root: Project = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "tracker-commons",
    version := "0.2.0",
    libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test",
    unmanagedSourceDirectories in Test += baseDirectory.value / "src/examples"
  ).
  dependsOn(minimal % "test->compile")

// To build and run examples
lazy val examples: Project = (project in file("src/examples")).
  settings(commonSettings: _*).
  dependsOn(root)
