package instrumenter

import instrumenter.TypeDescriptor as TD

object Injection {

  val jumboTracer: ClassName = ClassName("___JumboTracer___")
  
  enum EventMethod(methodNameStr: String) {
    case LineVisited extends EventMethod("lineVisited")
    case VariableSet extends EventMethod("variableSet")
    case StaticFieldSet extends EventMethod("staticFieldSet")
    case InstanceFieldSet extends EventMethod("instanceFieldSet")
    case Returned extends EventMethod("returned")
    case ReturnedVoid extends EventMethod("returnedVoid")
    case SaveArgument extends EventMethod("saveArgument")
    case TerminateMethodCall extends EventMethod("terminateMethodCall")
    case InstrumentedArrayStore extends EventMethod("instrumentedArrayStore")
    case SaveTermination extends EventMethod("saveTermination")
    
    val methodName: MethodName = MethodName(methodNameStr)
  }
  
  val display: MethodName = MethodName("display")
  val writeJsonTrace: MethodName = MethodName("writeJsonTrace")

}
