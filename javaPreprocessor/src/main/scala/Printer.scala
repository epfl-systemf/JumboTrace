package javaPreprocessor

import com.github.javaparser.ast.CompilationUnit

final class Printer extends PreprocessorStage[CompilationUnit, String] {

  override protected def runImpl(cu: CompilationUnit, errorReporter: ErrorReporter): Option[String] = {
    Some(cu.toString)
  }

}
