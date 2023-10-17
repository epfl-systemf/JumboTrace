package traceElements

/**
 * An element of a trace. Represents an event that happened in the program during its execution
 */
sealed trait TraceElement {
  def nestingLevel: Int
}

final case class SystemOutPrinted(
                                   text: String,
                                   nestingLevel: Int
                                 ) extends TraceElement

final case class SystemErrPrinted(
                                   text: String,
                                   nestingLevel: Int
                                 ) extends TraceElement

final case class LineVisited(
                              className: String,
                              lineNumber: Int,
                              nestingLevel: Int
                            ) extends TraceElement

final case class VarSet(
                         varId: String,
                         value: Value,
                         nestingLevel: Int
                       ) extends TraceElement

final case class VarGet(
                         varId: String,
                         value: Value,
                         nestingLevel: Int
                       ) extends TraceElement

final case class ArrayElemSet(
                               array: ReferenceValue,
                               idx: Int,
                               value: Value,
                               nestingLevel: Int
                             ) extends TraceElement

final case class ArrayElemGet(
                               array: ReferenceValue,
                               idx: Int,
                               value: Value,
                               nestingLevel: Int
                             ) extends TraceElement

final case class StaticFieldSet(
                                 owner: String,
                                 fieldName: String,
                                 value: Value,
                                 nestingLevel: Int
                               ) extends TraceElement

final case class StaticFieldGet(
                                 owner: String,
                                 fieldName: String,
                                 value: Value,
                                 nestingLevel: Int
                               ) extends TraceElement

final case class InstanceFieldSet(
                                   owner: ReferenceValue,
                                   fieldName: String,
                                   value: Value,
                                   nestingLevel: Int
                                 ) extends TraceElement

final case class InstanceFieldGet(
                                   owner: ReferenceValue,
                                   fieldName: String,
                                   value: Value,
                                   nestingLevel: Int
                                 ) extends TraceElement

final case class Return(
                         methodName: String,
                         value: Value,
                         nestingLevel: Int
                       ) extends TraceElement

final case class ReturnVoid(
                             methodName: String,
                             nestingLevel: Int
                           ) extends TraceElement

final case class MethodCalled(
                               ownerClass: String,
                               methodName: String,
                               args: Seq[Value],
                               isStatic: Boolean,
                               nestingLevel: Int
                             ) extends TraceElement

final case class Initialization(
                                 dateTime: String,
                                 nestingLevel: Int
                               ) extends TraceElement

final case class Termination(
                              msg: String,
                              nestingLevel: Int
                            ) extends TraceElement
