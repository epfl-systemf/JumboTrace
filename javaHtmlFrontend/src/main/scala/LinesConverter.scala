package javaHtmlFrontend

import javaHtmlFrontend.LinesConverter.*
import traceElements.*

import scala.collection.mutable

final class LinesConverter(classIndices: Map[ClassName, ClassIndex], files: Map[Filename, Seq[CodeLine]]) {

  def convert(lineVisited: LineVisited): String = {
    val classIndex = classIndices.apply(lineVisited.className)
    val rawLine = files.apply(classIndex.srcFileName)(lineVisited.lineNumber - 1)
    rawLine ++ readValues(lineVisited).mkString("   :   ", ", ", "")   // FIXME
  }

  private def readValues(lineVisited: LineVisited): Map[VarId, Value] = {
    lineVisited.subEvents
      .flatMap {
        case VarGet(varId, value) => Some(varId -> value)
        case ArrayElemGet(arrayId, idx, value) => Some(s"$arrayId[$idx]" -> value)
        case StaticFieldGet(owner, fieldName, value) => Some(s"$owner.$fieldName" -> value)
        case InstanceFieldGet(owner, fieldName, value) => Some(s"$owner.$fieldName" -> value)
        case _ => None
      }.groupBy(_._1)
      .map { (varId: VarId, pairs: Seq[(VarId, Value)]) =>
        varId -> pairs.map(_._2).toSet
      }.filter(_._2.size == 1)
      .map { (varId: VarId, singletonVal: Set[Value]) =>
        varId -> singletonVal.head
      }
      .toMap
  }

}

object LinesConverter {

  type Filename = String
  type ClassName = String
  type CodeLine = String
  type VarId = String

}
