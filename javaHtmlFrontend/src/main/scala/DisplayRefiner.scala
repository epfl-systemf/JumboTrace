package javaHtmlFrontend

import traceElements.{PrimitiveValue, ReferenceValue, Value}

object DisplayRefiner {

  def refineMethodName(methodName: String): String = {
    Map(
      "<clinit>" -> "<class initialization method>",
      "<init>" -> "<constructor>"
    ).getOrElse(methodName, methodName)
  }

  def refinedValue(value: Value): String = {
    value match
      case PrimitiveValue(_, value) => value
      case ReferenceValue(tpe, hashcode, value) if tpe.contains("$$Lambda$") => "lambda#" + hashcode
      case ReferenceValue(_, _, value) => value
  }

}
