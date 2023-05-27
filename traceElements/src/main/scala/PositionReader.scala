package traceElements

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.{Json, JsonConfiguration, Reads}

private[traceElements] object PositionReader {

  implicit val cfg: Aux[Json.MacroOptions] =
    JsonConfiguration(
      discriminator = "type",
      typeNaming = _.replace("traceElements.", "")
    )

  implicit val concretePositionReader: Reads[ConcretePosition] = Json.reads[ConcretePosition]
  implicit val noPositionReader: Reads[NoPosition.type] = Json.reads[NoPosition.type]
  implicit val positionReader: Reads[Position] = Json.reads[Position]

}
