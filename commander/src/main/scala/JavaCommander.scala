package commander

import debugCmdlineFrontend.DebugCmdlineFrontend
import instrumenter.{ClassName, Config, Instrumenter}
import javaHtmlFrontend.JavaHtmlFrontend

import java.io.{ByteArrayInputStream, FileOutputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Using

/**
 * Very simple command line program for running all the features of the debugger
 *
 * Quick and dirty, designed mainly for the presentation of the project
 *
 * Command-line parsing is very rudimentary (based on pattern match)
 *
 * Meant to be run in from a directory containing the Java source files
 */
object JavaCommander {

  private val jsonTraceFilePath = "./trace/trace.json"
  private val htmlTraceFilePath = "./trace/trace.html"
  private val additionalCodeDir = "injected"

  private val INSTRUMENT_CMD = "instrument"
  private val DISPLAY_CMD = "display"
  private val GEN_HTML_CMD = "html"
  private val RUN_CMD = "run"
  private val RUN_INSTRUMENTED = "run-i"
  private val COMPILE_CMD = "compile"
  private val ADD_ADDITIONAL_CODE_CMD = "add-code"
  private val HELP_CMD = "help"
  private val ALL_CMD = "all"

  private val LOG_OPTION = "-log"
  private val CLASSPATH_OPTION = "-cp"

  def main(args: Array[String]): Unit = {

    args.toList match {
      case Nil => {
        System.err.println("No argument: exiting")
        System.exit(-1)
      }
      case INSTRUMENT_CMD :: mainClass :: Nil =>
        Instrumenter.performInstrumentation(_ => (), ClassName(mainClass))
      case INSTRUMENT_CMD :: mainClass :: LOG_OPTION :: Nil =>
        Instrumenter.performInstrumentation(println, ClassName(mainClass))
      case DISPLAY_CMD :: Nil =>
        Using(Source.fromFile(jsonTraceFilePath)) { src =>
          DebugCmdlineFrontend.performDisplay(src)(using System.out)
        }
      case GEN_HTML_CMD :: Nil =>
        JavaHtmlFrontend.generateHtml(
          jsonTraceFilePath = jsonTraceFilePath,
          srcFilesNames = allFileNamesInDirWithExtension("java"),
          outputFilePath = htmlTraceFilePath
        )
      case RUN_CMD :: mainClassName :: Nil =>
        runJava(mainClassName)
      case RUN_INSTRUMENTED :: mainClassName :: Nil =>
        runJava(mainClassName, classPathOpt = Some(Config.config.transformedClassesDirName))
      case COMPILE_CMD :: Nil => compile()
      case ADD_ADDITIONAL_CODE_CMD :: Nil => addAdditionalCode()
      case ALL_CMD :: mainClassName :: Nil => {
        compile()
        Instrumenter.performInstrumentation(_ => (), ClassName(mainClassName))
        addAdditionalCode()
        runJava(mainClassName, classPathOpt = Some(Config.config.transformedClassesDirName))
        JavaHtmlFrontend.generateHtml(
          jsonTraceFilePath = jsonTraceFilePath,
          srcFilesNames = allFileNamesInDirWithExtension("java"),
          outputFilePath = htmlTraceFilePath
        )
      }
      case HELP_CMD :: mainClassName :: Nil => displayHelp()
      case _ =>
        System.err.println("unrecognized command or arguments")
    }

  }

  private def displayHelp(): Unit = {
    println("Commands:")
    println(s"$INSTRUMENT_CMD <main class> [-log]")
    println(DISPLAY_CMD)
    println(GEN_HTML_CMD)
    println(s"$RUN_CMD <main class>")
    println(s"$RUN_INSTRUMENTED <main class>")
    println(COMPILE_CMD)
    println(ADD_ADDITIONAL_CODE_CMD)
    println(s"$ALL_CMD <main class>")
    println(HELP_CMD)
  }

  private def runJava(mainClassName: String, classPathOpt: Option[String] = None): Unit = {
    import sys.process.*
    try {
      val command = classPathOpt.map(classPath => s"java -cp $classPath $mainClassName").getOrElse(s"java $mainClassName")
      val output = command.!!
      println("--------- Program output ------------------------")
      println(output)
      println("-------------------------------------------------")
    } catch {
      case e: RuntimeException =>
        System.err.println("An error occured:")
        System.err.println(e.getMessage)
        System.exit(-1)
    }
  }

  private def compile(): Unit = {
    import sys.process.*
    try {
      val command = "javac -g *.java"
      val output = command.!!
      println("Compilation result:")
      println(output)
      println()
    } catch {
      case e: RuntimeException => {
        System.err.println("An error occured:")
        System.err.println(e.getMessage)
        System.exit(-1)
      }
    }
  }

  private def addAdditionalCode(): Unit = {
    val additCodeDir = Paths.get(additionalCodeDir).toFile
    assert(additCodeDir.exists())
    for file <- additCodeDir.listFiles().toSeq if file.getName.endsWith(".class") do {
      val src = file.toPath
      val dst = Paths.get(Config.config.transformedClassesDirName ++ "/" ++ file.getName)
      Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def allFileNamesInDirWithExtension(extension: String): Seq[String] = {
    require(!extension.contains('.'))
    val currDir = Paths.get(".").toFile
    for sub <- currDir.listFiles().toSeq if sub.isFile && sub.getName.endsWith("." ++ extension) yield {
      sub.getName
    }
  }

}
