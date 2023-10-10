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
    case ReverseArgsList extends EventMethod("reverseArgsList")
    case PushbackArg(tpe: TypeDescriptor) extends EventMethod("pushbackArgument_" + javaRootTypeName(tpe))
    case TerminateMethodCall extends EventMethod("terminateMethodCall")
    case IncrementNestingLevel extends EventMethod("incrementNestingLevel")
    case InstrumentedArrayStore extends EventMethod("instrumentedArrayStore")
    case ArrayLoad extends EventMethod("arrayLoad")
    
    val methodName: MethodName = MethodName(methodNameStr)
  }
  
  private def javaRootTypeName(td: TypeDescriptor): String = {
    td match
      case TD.Boolean => "boolean"
      case TD.Char => "char"
      case TD.Byte => "byte"
      case TD.Short => "short"
      case TD.Int => "int"
      case TD.Float => "float"
      case TD.Long => "long"
      case TD.Double => "double"
      case TD.Void => "void"
      case _ : (TD.Array | TD.Class) => "Object"
  }

}
