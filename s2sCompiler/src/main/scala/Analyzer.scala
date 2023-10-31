import ErrorReporter.ErrorLevel.CriticalWarning
import com.github.javaparser.ast.*
import com.github.javaparser.ast.body.{MethodDeclaration, VariableDeclarator}
import com.github.javaparser.ast.expr.{AssignExpr, FieldAccessExpr, NameExpr}
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.{TreeVisitor, VoidVisitorAdapter, VoidVisitorWithDefaults}

import scala.collection.mutable

final class Analyzer extends CompilerStage[CompilationUnit, CompilationUnit] {
  
  override protected def runImpl(cu: CompilationUnit, errorReporter: ErrorReporter): Option[CompilationUnit] = {
    val filename = if cu.getStorage.isPresent then cu.getStorage.get().getFileName else "<missing file name>"
    cu.accept(new AnalysisVisitor(), VisitorCtx(errorReporter, filename))
    Some(cu)
  }
  
  private final class AnalysisVisitor extends VoidVisitorAdapter[VisitorCtx] {

    override def visit(n: MethodDeclaration, ctx: VisitorCtx): Unit = {
      super.visit(n, ctx)
      if (n.getName.getIdentifier == "toString"){
        n.accept(new ToStringNoMutVisitor(), ctx)
      }
    }

  }

  private final class ToStringNoMutVisitor extends VoidVisitorAdapter[VisitorCtx] {
    private val stack: mutable.Stack[mutable.ListBuffer[String]] = mutable.Stack.empty

    private def varIsKnown(varId: String): Boolean = {
      stack.exists(_.contains(varId))
    }

    override def visit(n: BlockStmt, ctx: VisitorCtx): Unit = {
      stack.push(mutable.ListBuffer.empty)
      super.visit(n, ctx)
      stack.pop()
    }

    override def visit(n: VariableDeclarator, ctx: VisitorCtx): Unit = {
      super.visit(n, ctx)
      stack.head.addOne(n.getName.getIdentifier)
    }

    override def visit(n: AssignExpr, ctx: VisitorCtx): Unit = {
      // TODO transitive checks for mutations
      super.visit(n, ctx)
      n.getTarget match {
        case nameExpr: NameExpr if !varIsKnown(nameExpr.getName.getIdentifier) =>
          ctx.er.reportErrorPos(s"assigning to field ${nameExpr.getName.getIdentifier}: side effect in toString",
            CriticalWarning, ctx.filename, n.getRange)
        case fieldAccessExpr: FieldAccessExpr =>
          ctx.er.reportErrorPos(s"assigning to field ${fieldAccessExpr.getName.getIdentifier}: side effect in toString",
            CriticalWarning, ctx.filename, n.getRange)
        case _ => ()
      }
    }

  }
  
  private final case class VisitorCtx(er: ErrorReporter, filename: String)
  
}
