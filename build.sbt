ThisBuild / organization := "com.epfl.systemf.jumbotrace"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.2"

lazy val instrumenter = project
  .settings(
    name := "instrumenter",
    idePackagePrefix := Some("instrumenter"),
    libraryDependencies += "org.ow2.asm" % "asm" % "9.5",
    scalacOptions += "-deprecation"
  )
