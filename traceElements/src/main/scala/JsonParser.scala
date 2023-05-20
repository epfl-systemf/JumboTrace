package traceElements

import io.circe.generic.auto.*
import io.circe.{Decoder, parser}

import scala.util.{Failure, Success, Try}

object JsonParser {

  def parse(str: String): Try[Seq[TraceElement]] = {
    parser.decode[Seq[Map[String, String]]](str) match
      case Left(error) => Failure(error)
      case Right(rawTrace) => Success {
        for (traceElemMap <- rawTrace) yield {
          createTraceElement(traceElemMap)
        }
      }
  }

  private def createTraceElement(map: Map[String, String]): TraceElement = {

    def strFld(key: String): String = map.apply(key)
    def intFld(key: String): Int = map.apply(key).toInt
    def boolFld(key: String): Boolean = map.apply(key).toBoolean
    def strSeqFld(key: String): Seq[String] = {
      val raw = map.apply(key)
      assert(raw.startsWith("["))
      assert(raw.endsWith("]"))
      raw.split(',').toSeq
    }

    def className = strFld("className")
    def lineNum = intFld("lineNum")
    def varId = strFld("varId")
    def value = strFld("value")
    def arrayId = strFld("arrayId")
    def idx = intFld("idx")
    def owner = strFld("owner")
    def fieldName = strFld("fieldName")
    def methodName = strFld("methodName")
    def ownerClass = strFld("ownerClass")
    def args = strSeqFld("args")
    def isStatic = boolFld("isStatic")

    strFld("type") match {
      case "LineVisited" =>
        LineVisited(className, lineNum)
      case "VarSet" =>
        VarSet(varId, value)
      case "ArrayElemSet" =>
        ArrayElemSet(arrayId, idx, value)
      case "StaticFieldSet" =>
        StaticFieldSet(owner, fieldName, value)
      case "InstanceFieldSet" =>
        InstanceFieldSet(owner, fieldName, value)
      case "Return" =>
        Return(methodName, value)
      case "ReturnVoid" =>
        ReturnVoid(methodName)
      case "MethodCalled" =>
        MethodCalled(ownerClass, methodName, args, isStatic)
    }
  }

}
