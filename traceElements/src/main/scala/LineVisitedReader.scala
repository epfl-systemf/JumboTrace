package traceElements

import play.api.libs.json.{Json, Reads}

import MethodCalledReader._
import NonRecursiveReaders._
import TraceElementReader._
import ValueReader._

private[traceElements] object LineVisitedReader {

  implicit val lineVisitedReader: Reads[LineVisited] = Json.reads[LineVisited]

}
