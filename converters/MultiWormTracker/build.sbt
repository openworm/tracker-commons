resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val wcon = RootProject(file("../../src/scala"))

// The main project
lazy val main = (project in file(".")).
  settings(
    scalaVersion := "2.12.1",
    scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation"),
    libraryDependencies += "com.novocode" % "junit-interface" % "0.9" % "test",
    libraryDependencies += "com.github.ichoran" %% "kse" % "0.6-SNAPSHOT",
    name := "old-mwt-to-wcon",
    version := "0.1.0",
    mainClass in Compile := Some(
      "org.openworm.trackercommons.converters.mwt.OldMwtToWcon"
    )
  ).
  dependsOn(wcon)
