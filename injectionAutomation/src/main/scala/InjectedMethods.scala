package injectionAutomation

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.{AssignExpr, Expression, IntegerLiteralExpr, MethodCallExpr, NameExpr, StringLiteralExpr}
import com.github.javaparser.Range as Rng
import com.github.javaparser.ast.stmt.{ExpressionStmt, Statement}

object InjectedMethods {

  type VarId = String

  private val injectedClassName: String = "___JumboTrace___"

  private def makeMethodName(rawName: String): String = injectedClassName ++ "." ++ rawName

  def iVarWrite(subject: Expression, assignedVarId: VarId): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("varWrite"),
      subject,
      new StringLiteralExpr(assignedVarId),  // passing the name of the variable, not its value
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }
  
  def iVarRead(subject: Expression, varId: VarId): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("varRead"),
      subject,
      new StringLiteralExpr(varId), // passing the name of the variable, not its value
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }
  
  def iArrayWrite(subject: Expression, array: VarId, idx: VarId): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("arrayWrite"),
      subject,
      new NameExpr(array),
      new NameExpr(idx),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }

  def iArrayRead(subject: Expression, array: VarId, idx: VarId): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("arrayAccess"),
      subject,
      new NameExpr(array),
      new NameExpr(idx),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }

  def iStaticFieldWrite(subject: Expression, className: String, fieldName: String): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("staticFieldWrite"),
      subject,
      new StringLiteralExpr(className),
      new StringLiteralExpr(fieldName),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }

  def iStaticFieldRead(subject: Expression, className: String, fieldName: String): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("staticFieldRead"),
      subject,
      new StringLiteralExpr(className),
      new StringLiteralExpr(fieldName),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }
  
  def iInstanceFieldWrite(subject: Expression, receiver: VarId, fieldName: String): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("instanceFieldWrite"),
      subject,
      new NameExpr(receiver),
      new StringLiteralExpr(fieldName),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }
  
  def iInstanceFieldRead(subject: Expression, receiver: VarId, fieldName: String): Expression = {
    val range = subject.getRange.get()
    new MethodCallExpr(
      makeMethodName("instanceFieldRead"),
      subject,
      new NameExpr(receiver),
      new StringLiteralExpr(fieldName),
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    ).withRangeSet(range)
  }
  
  extension(expr: Expression) private def withRangeSet(range: Rng): Expression = {
    expr.setRange(range)
    expr
  }

}
