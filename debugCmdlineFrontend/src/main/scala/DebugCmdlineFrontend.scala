package debugCmdlineFrontend

import play.api.libs.json.{JsError, JsSuccess}

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Using}
import traceElements.*

import java.io.PrintStream

object DebugCmdlineFrontend {

  def main(args: Array[String]): Unit = {
    Using(Source.fromFile(args.head)){ bufSrc =>
      performDisplay(bufSrc)(using System.out)
    }.get
  }

  def performDisplay(bufSrc: BufferedSource)(using ps: PrintStream): Unit = {
    val trace = JsonParser.parse(bufSrc.getLines().mkString("\n"))
    displayAll(trace, 0)
  }

  private def displayAll(traceElements: Seq[TraceElement], indent: Int)(using ps: PrintStream): Unit = {
    for (traceElem <- traceElements) do {
      display(traceElem, indent)
    }
  }

  private def display(traceElement: TraceElement, indent: Int)(using ps: PrintStream): Unit = {

    def println(str: String): Unit = ps.println(" " * (indent * 2) ++ str)

    traceElement match
      case LineVisited(className, lineNum) =>
        println(s"LINE $className:$lineNum")
      case VarSet(varId, value) =>
        println(s"SET $varId = $value")
      case ArrayElemSet(arrayId, idx, value) =>
        println(s"SET $arrayId[$idx] = $value")
      case StaticFieldSet(owner, fieldName, value) =>
        println(s"SET $owner.$fieldName = $value")
      case InstanceFieldSet(owner, fieldName, value) =>
        println(s"SET $owner.$fieldName = $value")
      case Return(methodName, value) =>
        println(s"RETURN $value FROM $methodName")
      case ReturnVoid(methodName) =>
        println(s"RETURN void FROM $methodName")
      case MethodCalled(ownerClass, methodName, args, isStatic, subEvents) =>
        println(s"CALL $ownerClass :: $methodName (${args.mkString(",")})")
        displayAll(subEvents, indent + 1)
      case Initialization(dateTime) =>
        println(s"INITIALIZATION: ${formatTime(dateTime)}")
      case Termination(msg) =>
        println(s"TERMINATION: $msg")
  }

  private def formatTime(dateTime: String): String = {
    dateTime.reverse.dropWhile(_ != '.').reverse.init   // remove nanoseconds
      .replace("T", " at ")
  }

}
