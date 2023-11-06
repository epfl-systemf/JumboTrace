package s2sCompiler

import java.io.File
import ErrorReporter.CompileError

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.visitor.VoidVisitor
import ErrorReporter.ErrorLevel.*

import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.{CombinedTypeSolver, JavaParserTypeSolver, ReflectionTypeSolver}

import java.nio.file.Files

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
        .andThen(new TypeAnalyzer())
        .andThen(new Transformer())
        .andThen(new Printer())
    }

    Files.walk(srcDir.toPath)
      .map(_.toFile)
      .filter(file => !file.isDirectory && file.getName.endsWith(".java"))
      .forEach { file =>
        val code = compiler.run(file, errorReporter)
        val linesNumberedCode = code.lines().toArray.zipWithIndex.map((l, i) => s"${i+1}. $l").mkString("\n")
        println(s"\n [${file.getName}] -----------------------------\n")
        println(linesNumberedCode)
      }

  }

  private def yellow(str: String): String = {
    val yellow = "\u001B[33m"
    val reset = "\u001B[0m"
    yellow ++ str ++ reset
  }

}
