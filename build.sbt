ThisBuild / organization := "com.epfl.systemf.jumbotrace"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.2"
ThisBuild / scalacOptions += "-deprecation"

val asmVersion = "9.5"
val javaParserVersion = "3.25.3"
val playVersion = "2.9.4"
val j2htmlVersion = "1.6.0"
val junitInterfaceVersion = "0.13.3"

lazy val instrumenter = project
  .settings(
    name := "instrumenter",
    idePackagePrefix := Some("instrumenter"),
    libraryDependencies ++= Seq(
      "org.ow2.asm" % "asm" % asmVersion,
      "com.github.sbt" % "junit-interface" % junitInterfaceVersion % "test"
    )
  )

lazy val s2sCompiler = project
  .settings(
    name := "s2sCompiler",
    idePackagePrefix := Some("s2sCompiler"),
    libraryDependencies ++= Seq(
      "com.github.javaparser" % "javaparser-core" % "3.25.5",
      "com.github.javaparser" % "javaparser-symbol-solver-core" % "3.25.5",
      "org.ow2.asm" % "asm" % "9.6"
    )
  )
