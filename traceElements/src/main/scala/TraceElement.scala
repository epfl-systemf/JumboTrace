package traceElements

sealed trait TraceElement

final case class LineVisited(
                              className: String,
                              lineNum: Int
                            ) extends TraceElement

final case class VarSet(
                         varId: String,
                         value: String
                       ) extends TraceElement

final case class ArrayElemSet(
                               arrayId: String,
                               idx: Int,
                               value: String
                             ) extends TraceElement

final case class StaticFieldSet(
                                 owner: String,
                                 fieldName: String,
                                 value: String
                               ) extends TraceElement

final case class InstanceFieldSet(
                                   owner: String,
                                   fieldName: String,
                                   value: String
                                 ) extends TraceElement

final case class Return(
                         methodName: String,
                         value: String
                       ) extends TraceElement

final case class ReturnVoid(methodName: String) extends TraceElement

final case class MethodCalled(
                               ownerClass: String,
                               methodName: String,
                               args: Seq[String],
                               isStatic: Boolean,
                               subEvents: Seq[TraceElement]
                             ) extends TraceElement

final case class Initialization(dateTime: String) extends TraceElement

final case class Termination(msg: String) extends TraceElement
