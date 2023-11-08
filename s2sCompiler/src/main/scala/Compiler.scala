package s2sCompiler

import java.io.File
import ErrorReporter.CompileError

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.visitor.VoidVisitor
import ErrorReporter.ErrorLevel.*

import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.{CombinedTypeSolver, JavaParserTypeSolver, ReflectionTypeSolver}

import java.nio.file.Files
import scala.io.Source
import scala.util.Using

object Compiler {

  def main(args: Array[String]): Unit = {

    val errorReporter = new ErrorReporter(System.err)

    if (args.length != 1) {
      errorReporter.reportError("Usage: compiler takes the source directory as its single argument", FatalError)
    }

    val srcDirName = args.head
    val srcDir = new File(srcDirName)
    if (!srcDir.exists()) {
      errorReporter.reportError(s"directory $srcDirName does not exits", FatalError)
    }
    if (!srcDir.isDirectory) {
      errorReporter.reportError(s"$srcDirName is not a directory", FatalError)
    }

    val compiler = {
      new Parser(srcDir)
        .andThen(new Analyzer())
        .andThen(new Transformer())
        .andThen(new Printer())
    }

    Files.walk(srcDir.toPath)
      .map(_.toFile)
      .filter(file => !file.isDirectory && file.getName.endsWith(".java"))
      .forEach { file =>
        val origCodeLines = Using(Source.fromFile(file))(
          _.getLines().toSeq
        ).get
        val finalCodeLines = compiler.run(file, errorReporter).lines().toArray()
        val origCodeLinesWidth = origCodeLines.maxBy(_.length).length
        println(s"\n ---- [${file.getName}] ${"-".repeat(origCodeLinesWidth*2)}\n")
        for (lineIdx <- (0 until (origCodeLines.length.max(finalCodeLines.length)))){
          print(s"${lineIdx+1}. ".padTo(5, ' '))
          print((if lineIdx < origCodeLines.length then origCodeLines(lineIdx) else "").padTo(origCodeLinesWidth, ' '))
          print(s" | ${lineIdx+1}. ".padTo(8, ' '))
          if (lineIdx < finalCodeLines.length){
            print(finalCodeLines(lineIdx))
          }
          println()
        }
      }

  }

  private def yellow(str: String): String = {
    val yellow = "\u001B[33m"
    val reset = "\u001B[0m"
    yellow ++ str ++ reset
  }

}
