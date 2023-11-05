package s2sCompiler

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter

final class Printer extends CompilerStage[(CompilationUnit, CompilationUnit), String] {

  override protected def runImpl(input: (CompilationUnit, CompilationUnit), errorReporter: ErrorReporter): Option[String] = {
    val (oldCu, newCu) = input
    LexicalPreservingPrinter.setup(newCu)
    Some(LexicalPreservingPrinter.print(newCu))
  }

}
