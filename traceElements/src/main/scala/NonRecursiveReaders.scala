package traceElements

import play.api.libs.json.{Json, Reads}

import PositionReader._

private[traceElements] object NonRecursiveReaders {
  implicit val varSetReader: Reads[VarSet] = Json.reads[VarSet]
  implicit val arrayElemSetReader: Reads[ArrayElemSet] = Json.reads[ArrayElemSet]
  implicit val staticFieldSetReader: Reads[StaticFieldSet] = Json.reads[StaticFieldSet]
  implicit val instanceFieldSetReader: Reads[InstanceFieldSet] = Json.reads[InstanceFieldSet]
  implicit val returnReader: Reads[Return] = Json.reads[Return]
  implicit val returnVoidReader: Reads[ReturnVoid] = Json.reads[ReturnVoid]
  implicit val initializationReader: Reads[Initialization] = Json.reads[Initialization]
  implicit val terminationReader: Reads[Termination] = Json.reads[Termination]
}
