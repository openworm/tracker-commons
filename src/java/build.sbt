// Annoyingly, we need to know the resolvers of other projects
// We depend on the Scala project:

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
lazy val scalawcon = RootProject(file("../scala"))


// This part handles the Java project

lazy val root = (project in file(".")).dependsOn(scalawcon).settings(
  name := "JavaWcon",
  version := "0.1.0",
  scalaVersion := "2.11.8",
  assemblyJarName in assembly := "JavaWcon-0.1.0.jar",
  target in assembly := file("target")
)
