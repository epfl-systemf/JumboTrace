package javaHtmlFrontend

import traceElements.Value

final case class PluggableCodeLine private(code: Code, colDecreasingVarsEnds: Seq[(Identifier, ColIdx)]) {

  def plugged(values: Map[Identifier, Value], lengthLimit: Int): String = {
    val line = new StringBuffer(code)
    for (id, colIdx) <- colDecreasingVarsEnds do {
      values.get(id)
        .filter(_.value.length <= lengthLimit)
        .foreach { value =>
          line.insert(colIdx, s":${value.value}")
        }
    }
    line.toString
  }

}

object PluggableCodeLine {

  def apply(code: Code, varsEnds: Seq[(Identifier, ColIdx)]): PluggableCodeLine = {
    new PluggableCodeLine(code, varsEnds.sortBy(-_._2))
  }

}
