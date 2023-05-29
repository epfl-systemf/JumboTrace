package javaHtmlFrontend

import traceElements.{PrimitiveValue, ReferenceValue, Value}

object DisplayRefiner {

  def refineMethodName(methodName: String): String = {
    Map(
      "<clinit>" -> "<class initialization method>",
      "<init>" -> "<constructor>"
    ).getOrElse(methodName, methodName)
  }

  def refinedValueShort(value: Value): String = {
    value match
      case PrimitiveValue(_, value) => value
      case ReferenceValue(tpe, hashcode, value) if tpe.contains("$$Lambda$") => "#lambda_" + hashcode
      case ReferenceValue(_, _, value) => value
  }

  def refinedValueComplete(value: Value): String = {
    value match
      case PrimitiveValue(tpe, value) => s"$value ($tpe)"
      case ReferenceValue(tpe, hashcode, value) if tpe.contains("$$Lambda$") => "#lambda_" + hashcode
      case ReferenceValue(tpe, hashcode, value) => s"$value (#$hashcode, $tpe)"
  }

}
