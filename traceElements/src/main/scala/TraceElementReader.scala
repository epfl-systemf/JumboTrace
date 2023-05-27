package traceElements

import play.api.libs.json.{Json, JsonConfiguration, JsonNaming, Reads}
import NonRecursiveReaders._
import MethodCalledReader._
import LineVisitedReader._

import play.api.libs.json.JsonConfiguration.Aux

private[traceElements] object TraceElementReader {

  implicit val cfg: Aux[Json.MacroOptions] =
    JsonConfiguration(
      discriminator = "type",
      typeNaming = _.replace("traceElements.", "")
    )

  implicit val traceElementReader: Reads[TraceElement] = Json.reads[TraceElement]
  implicit val traceReader: Reads[Seq[TraceElement]] = Reads.seq(traceElementReader)

}
