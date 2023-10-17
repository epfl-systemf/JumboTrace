package debugCmdlineFrontend

import play.api.libs.json.{JsError, JsSuccess}

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Using}
import traceElements.*

import java.io.PrintStream

/**
 * Simple frontend to display a trace in a readable way in the console or a file
 */
object DebugCmdlineFrontend {

  def performDisplay(bufSrc: BufferedSource, fileIdx: Int)(using ps: PrintStream): Unit = {
    val trace = JsonParser.parse(bufSrc.getLines().mkString("\n"))
    ps.println(ansiBlueCode + s"New trace file, index $fileIdx" + ansiResetCode)
    for (traceElem <- trace) do {
      display(traceElem, fileIdx)
    }
  }

  private def display(traceElement: TraceElement, fileIdx: Int)(using ps: PrintStream): Unit = {

    val indent = traceElement.nestingLevel

    def printlnTracer(str: String): Unit = {
      ps.println(ansiYellowCode ++ " " * indent ++ str ++ ansiResetCode)
    }

    def printlnStdout(str: String): Unit = {
      ps.println(" " * indent ++ str)
    }

    def printlnStderr(str: String): Unit = {
      ps.println(ansiRedCode ++ " " * indent ++ str ++ ansiResetCode)
    }

    traceElement match {
      case SystemOutPrinted(text, _) =>
        printlnStdout(text)
      case SystemErrPrinted(text, _) =>
        printlnStderr(text)
      case LineVisited(className, lineNumber, _) =>
        printlnTracer(s"VISIT LINE: $className:$lineNumber")
      case VarSet(varId, value, _) =>
        printlnTracer(s"SET $varId := $value")
      case VarGet(varId, value, _) =>
        printlnTracer(s"GET $varId : $value")
      case ArrayElemSet(array, idx, value, _) =>
        printlnTracer(s"SET $array[$idx] := $value")
      case ArrayElemGet(array, idx, value, _) =>
        printlnTracer(s"GET $array[$idx] : $value")
      case StaticFieldSet(owner, fieldName, value, _) =>
        printlnTracer(s"SET $owner.$fieldName := $value")
      case StaticFieldGet(owner, fieldName, value, _) =>
        printlnTracer(s"GET $owner.$fieldName : $value")
      case InstanceFieldSet(owner, fieldName, value, _) =>
        printlnTracer(s"SET $owner.$fieldName := $value")
      case InstanceFieldGet(owner, fieldName, value, _) =>
        printlnTracer(s"GET $owner.$fieldName : $value")
      case Return(methodName, value, _) =>
        printlnTracer(s"$methodName RETURNS $value")
      case ReturnVoid(methodName, _) =>
        printlnTracer(s"$methodName RETURNS void")
      case MethodCalled(ownerClass, methodName, args, isStatic, _) =>
        printlnTracer(s"CALL $ownerClass.$methodName${args.mkString("(", ",", ")")}")
      case Initialization(dateTime, _) =>
        printlnTracer(s"INITIALIZATION AT $dateTime")
      case Termination(msg, _) =>
        printlnTracer(s"TERMINATION: $msg")
    }
  }

  private def formatTime(dateTime: String): String = {
    dateTime.reverse.dropWhile(_ != '.').reverse.init   // remove nanoseconds
      .replace("T", " at ")
  }

  private val ansiYellowCode = "\u001B[33m"
  private val ansiRedCode = "\u001B[31m"
  private val ansiBlueCode = "\u001B[34m"
  private val ansiResetCode = "\u001B[0m"

}
