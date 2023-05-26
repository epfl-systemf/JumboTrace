package traceElements

import play.api.libs.json._

object JsonParser {

  def parse(str: String): Seq[TraceElement] = {
    val jsValue = Json.parse(str)
    jsValue.as[Seq[TraceElement]](TraceElementReader.traceReader)
  }

}
