package instrumenter

import instrumenter.TypeDescriptor as TD

/**
 * Names of the methods of the _\_\_Jumbotracer_\_\_ class
 */
object Injection {

  val jumboTracer: ClassName = ClassName("___JumboTracer___")
  
  enum EventMethod(methodNameStr: String) {
    case LineVisited extends EventMethod("lineVisited")
    case VariableSet extends EventMethod("variableSet")
    case VariableGet extends EventMethod("variableGet")
    case StaticFieldSet extends EventMethod("staticFieldSet")
    case StaticFieldGet extends EventMethod("staticFieldGet")
    case InstanceFieldSet extends EventMethod("instanceFieldSet")
    case InstanceFieldGet extends EventMethod("instanceFieldGet")
    case Returned extends EventMethod("returned")
    case ReturnedVoid extends EventMethod("returnedVoid")
    case SaveArgument extends EventMethod("saveArgument")
    case TerminateMethodCall extends EventMethod("terminateMethodCall")
    case InstrumentedArrayStore extends EventMethod("instrumentedArrayStore")
    case ArrayLoad extends EventMethod("arrayLoad")
    case SaveTermination extends EventMethod("saveTermination")
    
    val methodName: MethodName = MethodName(methodNameStr)
  }

}
