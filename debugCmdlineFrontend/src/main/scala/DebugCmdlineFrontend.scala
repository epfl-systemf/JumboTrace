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
    def printlnPositioned(str: String, position: Position): Unit = println(s"$str ($position)")

    traceElement match
      case VarSet(varId, value, pos) =>
        printlnPositioned(s"SET $varId = $value", pos)
      case ArrayElemSet(arrayId, idx, value, pos) =>
        printlnPositioned(s"SET $arrayId[$idx] = $value", pos)
      case StaticFieldSet(owner, fieldName, value, pos) =>
        printlnPositioned(s"SET $owner.$fieldName = $value", pos)
      case InstanceFieldSet(owner, fieldName, value, pos) =>
        printlnPositioned(s"SET $owner.$fieldName = $value", pos)
      case Return(methodName, value, pos) =>
        printlnPositioned(s"RETURN $value FROM $methodName", pos)
      case ReturnVoid(methodName, pos) =>
        printlnPositioned(s"RETURN void FROM $methodName", pos)
      case MethodCalled(ownerClass, methodName, args, isStatic, pos, subEvents) =>
        printlnPositioned(s"CALL $ownerClass :: $methodName (${args.mkString(",")})", pos)
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
