ThisBuild / organization := "com.epfl.systemf.jumbotrace"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.2"
ThisBuild / scalacOptions += "-deprecation"

val asmVersion = "9.5"
val javaParserVersion = "3.25.3"
val circeVersion = "0.14.3"

lazy val instrumenter = project
  .settings(
    name := "instrumenter",
    idePackagePrefix := Some("instrumenter"),
    libraryDependencies += "org.ow2.asm" % "asm" % asmVersion
  )

lazy val traceElements = project
  .settings(
    name := "traceElements",
    idePackagePrefix := Some("traceElements"),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion)
  )

lazy val debugCmdlineFrontend = project
  .settings(
    name := "debugCmdlineFrontend",
    idePackagePrefix := Some("debugCmdlineFrontend")
  ).dependsOn(traceElements)

lazy val javaCmdlineFrontend = project
  .settings(
    name := "javaCmdlineFrontend",
    idePackagePrefix := Some("javacmdfrontend"),
    libraryDependencies += "com.github.javaparser" % "javaparser-symbol-solver-core" % javaParserVersion,
  ).dependsOn(traceElements)
