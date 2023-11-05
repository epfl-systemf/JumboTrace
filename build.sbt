ThisBuild / organization := "com.epfl.systemf.jumbotrace"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.2"
ThisBuild / scalacOptions += "-deprecation"

val asmVersion = "9.6"
val javaParserVersion = "3.25.5"
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
      "com.github.javaparser" % "javaparser-core" % javaParserVersion,
      "com.github.javaparser" % "javaparser-symbol-solver-core" % javaParserVersion,
      "org.ow2.asm" % "asm" % asmVersion
    )
  ).dependsOn(injectionAutomation)

lazy val injectionAutomation = project
  .settings(
    name := "injectionAutomation",
    idePackagePrefix := Some("injectionAutomation"),
    libraryDependencies += "com.github.javaparser" % "javaparser-core" % javaParserVersion
  )
