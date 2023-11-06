package injectionAutomation

import com.github.javaparser.ast.expr.{Expression, MethodCallExpr, NameExpr}

object InjectedMethods {
  
  type VarId = String
  
  private val injectedClassName: String = "___JumboTrace___"
  
  private def makeMethodName(rawName: String): String = injectedClassName ++ "." ++ rawName
  
  def iArrayAccess(access: Expression, array: VarId, idx: VarId): Expression = {
    MethodCallExpr(makeMethodName("arrayAccess"), access, new NameExpr(array), new NameExpr(idx))
  }
  
}
