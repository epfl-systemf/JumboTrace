package traceElements

import NonRecursiveReaders._
import TraceElementReader._
import LineVisitedReader._
import ValueReader._

import play.api.libs.json.{Json, Reads}

private[traceElements] object MethodCalledReader {
  implicit val methodCalledReader: Reads[MethodCalled] = Json.reads[MethodCalled]
}
