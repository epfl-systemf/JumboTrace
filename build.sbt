ThisBuild / organization := "com.epfl.systemf.jumbotrace"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.2"
ThisBuild / scalacOptions += "-deprecation"

val asmVersion = "9.5"
val junitInterfaceVersion = "0.13.3"

lazy val b2bCompiler = project
  .settings(
    name := "b2bCompiler",
    idePackagePrefix := Some("b2bCompiler"),
    libraryDependencies ++= Seq(
      "org.ow2.asm" % "asm" % asmVersion,
      "com.github.sbt" % "junit-interface" % junitInterfaceVersion % "test"
    )
  )

lazy val script = project
  .settings(
    name := "script",
    idePackagePrefix := Some("script")
  )
