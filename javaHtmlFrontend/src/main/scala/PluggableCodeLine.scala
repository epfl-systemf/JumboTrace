package javaHtmlFrontend

import traceElements.Value

import scala.io.AnsiColor

/**
 * A code line, with the variables whose value can be inlined  (along with their end column)
 */
final case class PluggableCodeLine private(code: Code, colDecreasingVarsEnds: Seq[(Identifier, ColIdx)]) {

  /**
   * This line, with the provided values plugged
   */
  def plugged(values: Map[Identifier, Value], lengthLimit: Int): String = {
    val line = new StringBuffer(code)
    for (id, colIdx) <- colDecreasingVarsEnds if 0 <= colIdx && colIdx < line.length() do {
      values.get(id)
        .map(DisplayRefiner.refinedValueShort)
        .filter(_.length <= lengthLimit)
        .foreach { refinedVal =>
          line.insert(colIdx, s":$refinedVal")
        }
    }
    line.toString
  }

  def mustHide: Boolean = (code.trim == "}")

}

object PluggableCodeLine {

  def apply(code: Code, varsEnds: Seq[(Identifier, ColIdx)]): PluggableCodeLine = {
    new PluggableCodeLine(code, varsEnds.sortBy(-_._2))
  }

}
