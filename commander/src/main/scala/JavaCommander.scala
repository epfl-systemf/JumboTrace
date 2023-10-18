package commander

import debugCmdlineFrontend.DebugCmdlineFrontend
import instrumenter.{ClassName, Config, Instrumenter}
import javaHtmlFrontend.JavaHtmlFrontend

import java.io.{ByteArrayInputStream, FileOutputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.io.Source
import scala.util.Using

/**
 * Very simple command line program for running most of the features of the tracer
 *
 * Quick and dirty, designed mainly for the presentation of the project
 *
 * Command-line parsing is very rudimentary (based on pattern match)
 */
object JavaCommander {

  private def jsonTraceFilePath(idx: Int): String = "./trace/trace_" + idx + ".json"
  private val htmlTraceFilePath = "./trace/trace.html"
  private val additionalCodeDir = "injected"

  // Commands
  private val INSTRUMENT_CMD = "instrument"
  private val DISPLAY_CMD = "display"
  private val GEN_HTML_CMD = "html"
  private val RUN_CMD = "run"
  private val RUN_INSTRUMENTED = "run-i"
  private val COMPILE_CMD = "compile"
  private val ADD_ADDITIONAL_CODE_CMD = "add-code"
  private val HELP_CMD = "help"
  private val ALL_CMD = "all"

  // Options
  private val LOG_OPTION = "-log"
  private val CLASSPATH_OPTION = "-cp"

  def main(args: Array[String]): Unit = {
    
    /* 
     * WARNING: some of the methods called from here were themselves main methods before refactoring, 
     * and may call System.exit instead of throwing exceptions
     */

    // FIXME some of the commands have not been adapted to the new multi-file traces format

    args.toList match {
      case Nil => {
        System.err.println("No argument: exiting")
        System.exit(-1)
      }
      case INSTRUMENT_CMD :: mainClass :: Nil =>
        Instrumenter.performInstrumentation(_ => (), ClassName(mainClass))
      case INSTRUMENT_CMD :: mainClass :: LOG_OPTION :: Nil =>
        Instrumenter.performInstrumentation(println, ClassName(mainClass))
      case DISPLAY_CMD :: Nil => {
        val ids2Indents = mutable.Map.empty[Long, Int]
        var fileIdx = 1
        while (Files.exists(Paths.get(jsonTraceFilePath(fileIdx)))){
          Using(Source.fromFile(jsonTraceFilePath(fileIdx))) { src =>
            DebugCmdlineFrontend.performDisplay(src, fileIdx, ids2Indents)(using System.out)
          }.get
          fileIdx += 1
        }
        println("No more file to read. Exiting")
      }
      case GEN_HTML_CMD :: Nil =>
        JavaHtmlFrontend.generateHtml(
          jsonTraceFilePath = ???,  // FIXME
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
        println(" > Compiling...")
        compile()
        println(" > Instrumenting...")
        Instrumenter.performInstrumentation(_ => (), ClassName(mainClassName))
        println(" > Copying additional code...")
        addAdditionalCode()
        println(" > Running the program...")
        runJava(mainClassName, classPathOpt = Some(Config.config.transformedClassesDirName))
        println(" > Generating HTML...")
        JavaHtmlFrontend.generateHtml(
          jsonTraceFilePath = ???,  // FIXME
          srcFilesNames = allFileNamesInDirWithExtension("java"),
          outputFilePath = htmlTraceFilePath
        )
      }
      case HELP_CMD :: Nil => displayHelp()
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
