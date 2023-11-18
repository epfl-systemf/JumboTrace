package injectionAutomation

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.{AssignExpr, Expression, IntegerLiteralExpr, MethodCallExpr, NameExpr, StringLiteralExpr}
import com.github.javaparser.Range as Rng
import com.github.javaparser.ast.stmt.{ExpressionStmt, Statement}

object InjectedMethods {

  type VarId = String

  private val injectedClassName: String = "___JumboTrace___"

  private def makeMethodName(rawName: String): String = injectedClassName ++ "." ++ rawName

  def iVarAssign(assignExpr: Expression, assignedVarId: VarId): Expression = {
    val range = assignExpr.getRange.get()
    new MethodCallExpr(
      makeMethodName("varAssign"),
      assignExpr,
      new StringLiteralExpr(assignedVarId),  // passing the name of the variable, not its value
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    )
  }

  def iArrayRead(access: Expression, array: VarId, idx: VarId): Expression = {
    val range = access.getRange.get()
    new MethodCallExpr(
      makeMethodName("arrayAccess"),
      access,
      new NameExpr(array),
      new NameExpr(idx),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    )
  }
  
  def iArrayWrite(write: Expression, array: VarId, idx: VarId): Expression = {
    val range = write.getRange.get()
    new MethodCallExpr(
      makeMethodName("arrayWrite"),
      write,
      new NameExpr(array),
      new NameExpr(idx),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    )
  }
  
  def iFieldWrite(write: Expression, receiver: VarId, fieldName: VarId): Expression = {
    val range = write.getRange.get()
    new MethodCallExpr(
      makeMethodName("fieldWrite"),
      write,
      new NameExpr(receiver),
      new StringLiteralExpr(fieldName),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    )
  }

}
