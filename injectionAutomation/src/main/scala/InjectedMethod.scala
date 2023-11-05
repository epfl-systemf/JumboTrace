package injectionAutomation

import com.github.javaparser.ast.expr.{Expression, MethodCallExpr, NameExpr}

object InjectedMethod {
  
  type VarId = String
  
  def logArrayAccess(array: VarId, idx: VarId): Expression = {
    MethodCallExpr("___JumboTrace___.arrayAccess", new NameExpr(array), new NameExpr(idx))
  }
  
}
