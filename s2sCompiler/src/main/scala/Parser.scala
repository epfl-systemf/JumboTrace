package s2sCompiler

import com.github.javaparser.ParserConfiguration.LanguageLevel
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.{JavaParser, ParserConfiguration}
import ErrorReporter.ErrorLevel.*

import java.io.File

final class Parser(srcDirectory: File) extends CompilerStage[File, CompilationUnit] {

  private val javaparser = new JavaParser(
    new ParserConfiguration()
      .setLanguageLevel(LanguageLevel.JAVA_17)
  )

  override protected def runImpl(srcFile: File, errorReporter: ErrorReporter): Option[CompilationUnit] = {
    val parseRes = javaparser.parse(srcFile)
    if (parseRes.isSuccessful) {
      Some(parseRes.getResult.get())
    } else {
      parseRes.getProblems.forEach { problem =>
        errorReporter.reportErrorPos(problem.toString, NonFatalError, srcFile.getName, problem.getLocation.flatMap(_.toRange))
      }
      None
    }
  }

}
