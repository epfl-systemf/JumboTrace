package traceElements

/**
 * An element of a trace. Represents an event that happened in the program during its execution
 */
sealed trait TraceElement {
  def id: Long

  def parentId: Long
}

final case class SystemOutPrinted(
                                   text: String,
                                   id: Long,
                                   parentId: Long
                                 ) extends TraceElement

final case class SystemErrPrinted(
                                   text: String,
                                   id: Long,
                                   parentId: Long
                                 ) extends TraceElement

final case class LineVisited(
                              className: String,
                              lineNumber: Int,
                              id: Long,
                              parentId: Long
                            ) extends TraceElement

final case class VarSet(
                         varId: String,
                         value: Value,
                         id: Long,
                         parentId: Long
                       ) extends TraceElement

final case class VarGet(
                         varId: String,
                         value: Value,
                         id: Long,
                         parentId: Long
                       ) extends TraceElement

final case class ArrayElemSet(
                               array: ReferenceValue,
                               idx: Int,
                               value: Value,
                               id: Long,
                               parentId: Long
                             ) extends TraceElement

final case class ArrayElemGet(
                               array: ReferenceValue,
                               idx: Int,
                               value: Value,
                               id: Long,
                               parentId: Long
                             ) extends TraceElement

final case class StaticFieldSet(
                                 owner: String,
                                 fieldName: String,
                                 value: Value,
                                 id: Long,
                                 parentId: Long
                               ) extends TraceElement

final case class StaticFieldGet(
                                 owner: String,
                                 fieldName: String,
                                 value: Value,
                                 id: Long,
                                 parentId: Long
                               ) extends TraceElement

final case class InstanceFieldSet(
                                   owner: ReferenceValue,
                                   fieldName: String,
                                   value: Value,
                                   id: Long,
                                   parentId: Long
                                 ) extends TraceElement

final case class InstanceFieldGet(
                                   owner: ReferenceValue,
                                   fieldName: String,
                                   value: Value,
                                   id: Long,
                                   parentId: Long
                                 ) extends TraceElement

final case class Return(
                         methodName: String,
                         value: Value,
                         id: Long,
                         parentId: Long
                       ) extends TraceElement

final case class ReturnVoid(
                             methodName: String,
                             id: Long,
                             parentId: Long
                           ) extends TraceElement

// TODO save receiver?
final case class MethodCalled(
                               ownerClass: String,
                               methodName: String,
                               args: Seq[Value],
                               isStatic: Boolean,
                               id: Long,
                               parentId: Long
                             ) extends TraceElement

final case class Initialization(
                                 dateTime: String,
                                 id: Long,
                                 parentId: Long
                               ) extends TraceElement
