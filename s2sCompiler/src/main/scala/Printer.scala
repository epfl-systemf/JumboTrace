package s2sCompiler

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.visitor.VoidVisitor
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter

final class Printer extends CompilerStage[CompilationUnit, String] {

  override protected def runImpl(cu: CompilationUnit, errorReporter: ErrorReporter): Option[String] = {
    // TODO printer that actually preserves line numbers
//    LexicalPreservingPrinter.setup(cu)
    val codeStr = cu.toString
    Some(codeStr)
  }

}
