package traceElements

sealed trait TraceElement

final case class VarSet(
                         varId: String,
                         value: String,
                         pos: Position
                       ) extends TraceElement

final case class ArrayElemSet(
                               arrayId: String,
                               idx: Int,
                               value: String,
                               pos: Position
                             ) extends TraceElement

final case class StaticFieldSet(
                                 owner: String,
                                 fieldName: String,
                                 value: String,
                                 pos: Position
                               ) extends TraceElement

final case class InstanceFieldSet(
                                   owner: String,
                                   fieldName: String,
                                   value: String,
                                   pos: Position
                                 ) extends TraceElement

final case class Return(
                         methodName: String,
                         value: String,
                         pos: Position
                       ) extends TraceElement

final case class ReturnVoid(
                             methodName: String,
                             pos: Position
                           ) extends TraceElement

final case class MethodCalled(
                               ownerClass: String,
                               methodName: String,
                               args: Seq[String],
                               isStatic: Boolean,
                               pos: Position,
                               subEvents: Seq[TraceElement]
                             ) extends TraceElement

final case class Initialization(dateTime: String) extends TraceElement

final case class Termination(msg: String) extends TraceElement

sealed trait Position
final case class ConcretePosition(className: String, lineNum: Int) extends Position {
  override def toString: String = s"$className:$lineNum"
}
object NoPosition extends Position {
  override def toString: String = s"<no position>"
}
