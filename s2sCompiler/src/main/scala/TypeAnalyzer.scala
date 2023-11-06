package s2sCompiler

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.`type`.Type
import com.github.javaparser.ast.expr.Expression

/**
 * Builds a map from expressions to their types
 * Relies on JavaSymbolSolver, which sometimes fails. It should therefore never be assumed that all types will be 
 * present in the map
 */
final class TypeAnalyzer extends CompilerStage[CompilationUnit, (CompilationUnit, Map[Expression, Type])] {

  override protected def runImpl(cu: CompilationUnit, errorReporter: ErrorReporter): Option[(CompilationUnit, Map[Expression, Type])] = {
    val mapB = Map.newBuilder[Expression, Type]
    cu.findAll(classOf[Expression]).forEach { expr =>
      try {
        val resolvedType = expr.calculateResolvedType()
        val tpe = StaticJavaParser.parseType(resolvedType.describe())
        mapB.addOne(expr -> tpe)
      } catch {
        case _: Exception => ()
      }
    }
    Some(cu, mapB.result())
  }

}
