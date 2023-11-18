package s2sCompiler

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.{CompilationUnit, Node}
import com.github.javaparser.ast.`type`.Type
import com.github.javaparser.ast.expr.{Expression, FieldAccessExpr, NameExpr, SimpleName}
import com.github.javaparser.ast.stmt.LabeledStmt
import com.github.javaparser.resolution.{Resolvable, UnsolvedSymbolException}
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration
import s2sCompiler.ErrorReporter.ErrorLevel

import scala.collection.mutable
import scala.util.Try

/**
 * Builds a map from expressions to their types
 * Relies on JavaSymbolSolver, which sometimes fails. It should therefore never be assumed that all types will be 
 * present in the map
 */
final class Analyzer extends CompilerStage[CompilationUnit, Analyzer.Result] {

  override protected def runImpl(cu: CompilationUnit, errorReporter: ErrorReporter): Option[Analyzer.Result] = {
    val filename = cu.getStorage.map(_.getFileName).orElseGet { () => "<missing file name>" }
    val typesMapB = Map.newBuilder[Expression, Type]
    val declarationsMapB = Map.newBuilder[Resolvable[_], ResolvedValueDeclaration]
    val varIdsSetB = Set.newBuilder[String]
    cu.findAll(classOf[Node]).forEach { node =>
      computeAndAddTypeIfExpr(typesMapB, node)
      node match {
        case resolvable: (NameExpr | FieldAccessExpr) =>
          try {
            val pair = resolvable -> resolvable.resolve()
            declarationsMapB.addOne(pair)
          } catch {
            case _: Exception => ()
          }
        case simpleName: SimpleName =>
          varIdsSetB.addOne(simpleName.getIdentifier)
        case _ => ()
      }
    }
    Some(Analyzer.Result(cu, typesMapB.result(), declarationsMapB.result(), varIdsSetB.result()))
  }

  private def computeAndAddTypeIfExpr(mapB: mutable.Builder[(Expression, Type), Map[Expression, Type]], node: Node): Unit = {
    node match {
      case expr: Expression => {
        try {
          val resolvedType = expr.calculateResolvedType()
          val tpe = StaticJavaParser.parseType(resolvedType.describe())
          mapB.addOne(expr -> tpe)
        } catch {
          case _: Exception => ()
        }
      }
      case _ => ()
    }
  }

}

object Analyzer {

  final case class Result(
                           cu: CompilationUnit,
                           types: Map[Expression, Type],
                           declarations: Map[Resolvable[_], ResolvedValueDeclaration],
                           usedVariableNames: Set[String]
                         )

}
