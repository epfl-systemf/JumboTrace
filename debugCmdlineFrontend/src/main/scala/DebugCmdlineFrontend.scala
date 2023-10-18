package debugCmdlineFrontend

import play.api.libs.json.{JsError, JsSuccess}

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Using}
import scala.collection.mutable
import traceElements.*

import java.io.PrintStream

/**
 * Simple frontend to display a trace in a readable way in the console or a file
 */
object DebugCmdlineFrontend {

  def performDisplay(bufSrc: BufferedSource, fileIdx: Int, ids2Indents: mutable.Map[Long, Int])(using ps: PrintStream): Unit = {

    def display(traceElement: TraceElement, fileIdx: Int)(using ps: PrintStream): Unit = {

      val indent = if traceElement.parentId == 0 then 0 else ids2Indents.apply(traceElement.parentId) + 1
      if (traceElement.isInstanceOf[LineVisited | MethodCalled]){
        ids2Indents(traceElement.id) = indent
      }

      def printlnTracer(str: String): Unit = {
        ps.println(
          ansiYellowCode ++
            " " * indent ++ str ++ s" (id ${traceElement.id}, parent ${traceElement.parentId})" ++
            ansiResetCode
        )
      }

      def printlnStdout(str: String): Unit = {
        ps.println(
          " " * indent ++ str ++
            ansiYellowCode ++
            s" (id ${traceElement.id}, parent ${traceElement.parentId})" ++
            ansiResetCode
        )
      }

      def printlnStderr(str: String): Unit = {
        ps.println(
          ansiRedCode ++
          " " * indent ++ str ++
          ansiYellowCode ++
          s" (id ${traceElement.id}, parent ${traceElement.parentId})" ++
          ansiResetCode
        )
      }

      traceElement match
        case SystemOutPrinted(text, _, _) =>
          printlnStdout(text)
        case SystemErrPrinted(text, _, _) =>
          printlnStderr(text)
        case LineVisited(className, lineNumber, _, _) =>
          printlnTracer(s"VISIT LINE: $className:$lineNumber")
        case VarSet(varId, value, _, _) =>
          printlnTracer(s"SET $varId := ${value.shortDescr}")
        case VarGet(varId, value, _, _) =>
          printlnTracer(s"GET $varId : ${value.shortDescr}")
        case ArrayElemSet(array, idx, value, _, _) =>
          printlnTracer(s"SET ${array.shortDescr}[$idx] := ${value.shortDescr}")
        case ArrayElemGet(array, idx, value, _, _) =>
          printlnTracer(s"SET ${array.shortDescr}[$idx] := ${value.shortDescr}")
        case StaticFieldSet(owner, fieldName, value, _, _) =>
          printlnTracer(s"SET $owner.$fieldName := ${value.shortDescr}")
        case StaticFieldGet(owner, fieldName, value, _, _) =>
          printlnTracer(s"GET $owner.$fieldName : ${value.shortDescr}")
        case InstanceFieldSet(owner, fieldName, value, _, _) =>
          printlnTracer(s"SET ${owner.shortDescr}.$fieldName := ${value.shortDescr}")
        case InstanceFieldGet(owner, fieldName, value, _, _) =>
          printlnTracer(s"GET ${owner.shortDescr}.$fieldName : ${value.shortDescr}")
        case Return(methodName, value, _, _) =>
          printlnTracer(s"$methodName RETURNS ${value.shortDescr}")
        case ReturnVoid(methodName, _, _) =>
          printlnTracer(s"$methodName RETURNS void")
        case MethodCalled(ownerClass, methodName, args, isStatic, _, _) =>
          printlnTracer(s"CALL $ownerClass.$methodName${args.map(_.shortDescr).mkString("(", ",", ")")}")
        case Initialization(dateTime, _, _) =>
          printlnTracer(s"INITIALIZATION AT $dateTime")
    }

    val trace = JsonParser.parse(bufSrc.getLines().mkString("\n"))
    ps.println(ansiBlueCode + s"New trace file, index $fileIdx" + ansiResetCode)
    for (traceElem <- trace) do {
      display(traceElem, fileIdx)
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
