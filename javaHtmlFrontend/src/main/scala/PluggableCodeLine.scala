package javaHtmlFrontend

import traceElements.Value

import scala.io.AnsiColor

final case class PluggableCodeLine private(code: Code, colDecreasingVarsEnds: Seq[(Identifier, ColIdx)]) {

  def plugged(values: Map[Identifier, Value], lengthLimit: Int): String = {
    val line = new StringBuffer(code)
    for (id, colIdx) <- colDecreasingVarsEnds if 0 <= colIdx && colIdx < line.length() do {
      values.get(id)
        .map(DisplayRefiner.refinedValue)
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
