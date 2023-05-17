ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "instrumenter",
    idePackagePrefix := Some("com.epfl.systemf.jumbotrace.instrumenter"),
    libraryDependencies += "org.ow2.asm" % "asm" % "9.5",
    scalacOptions += "-deprecation"
  )
