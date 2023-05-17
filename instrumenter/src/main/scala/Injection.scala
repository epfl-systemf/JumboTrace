package com.epfl.systemf.jumbotrace.instrumenter

import com.epfl.systemf.jumbotrace.instrumenter.TypeDescriptor as TD

object Injection {

  val jumboTracer: ClassName = ClassName("___JumboTracer___")
  
  enum EventMethod(methodNameStr: String) {
    case LineVisited extends EventMethod("lineVisited")
    case VariableSet extends EventMethod("variableSet")
    case Returned extends EventMethod("returned")
    case ReturnedVoid extends EventMethod("returnedVoid")
    
    val methodName: MethodName = MethodName(methodNameStr)
  }
  
  val display: MethodName = MethodName("display")
  val writeJsonTrace: MethodName = MethodName("writeJsonTrace")

}
