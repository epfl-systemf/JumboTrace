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

lazy val traceElements = project
  .settings(
    name := "traceElements",
    scalaVersion := "2.13.10",
    idePackagePrefix := Some("traceElements"),
    libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion
  )

lazy val debugCmdlineFrontend = project
  .settings(
    name := "debugCmdlineFrontend",
    idePackagePrefix := Some("debugCmdlineFrontend")
  ).dependsOn(traceElements)

lazy val javaHtmlFrontend = project
  .settings(
    name := "javaHtmlFrontend",
    idePackagePrefix := Some("javaHtmlFrontend"),
    libraryDependencies ++= Seq(
      "com.github.javaparser" % "javaparser-symbol-solver-core" % javaParserVersion,
      "com.j2html" % "j2html" % j2htmlVersion
    )
  ).dependsOn(traceElements)

lazy val commander = project
  .settings(
    name := "commander",
    idePackagePrefix := Some("commander"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    }
  ).dependsOn(instrumenter, traceElements, debugCmdlineFrontend, javaHtmlFrontend)
