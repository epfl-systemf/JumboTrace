package injectionAutomation

import com.github.javaparser.ast.expr.{Expression, MethodCallExpr, NameExpr}

object InjectedMethods {
  
  type VarId = String
  
  def iArrayAccess(access: Expression, array: VarId, idx: VarId): Expression = {
    MethodCallExpr("___JumboTrace___.arrayAccess", access, new NameExpr(array), new NameExpr(idx))
  }
  
}
