package traceElements

import play.api.libs.json.{Json, JsonConfiguration, Reads}
import play.api.libs.json.JsonConfiguration.Aux

object ValueReader {

  implicit val cfg: Aux[Json.MacroOptions] =
    JsonConfiguration(
      discriminator = "type",
      typeNaming = _.replace("traceElements.", "")
    )
  
  implicit val primitiveValueReader: Reads[PrimitiveValue] = Json.reads[PrimitiveValue]
  implicit val referenceValueReader: Reads[ReferenceValue] = Json.reads[ReferenceValue]
  implicit val valueReader: Reads[Value] = Json.reads[Value]
  
}
