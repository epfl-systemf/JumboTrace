package traceElements

sealed trait TraceElement

final case class LineVisited(className: String, lineNum: Int) extends TraceElement {
  override def toString: String = s"VISIT line $lineNum in $className"
}

final case class VarSet(varId: String, value: String) extends TraceElement {
  override def toString: String = s"SET $varId = $value"
}

final case class ArrayElemSet(arrayId: String, idx: Int, value: String) extends TraceElement {
  override def toString: String = s"$arrayId[$idx] = $value"
}

final case class StaticFieldSet(owner: String, fieldName: String, value: String) extends TraceElement {
  override def toString: String = s"$owner.$fieldName = $value"
}

final case class InstanceFieldSet(owner: String, fieldName: String, value: String) extends TraceElement {
  override def toString: String = s"$owner.$fieldName = $value"
}

final case class Return(methodName: String, value: String) extends TraceElement {
  override def toString: String = s"$methodName RETURNS $value"
}

final case class ReturnVoid(methodName: String) extends TraceElement {
  override def toString: String = s"$methodName RETURNS void"
}

final case class MethodCalled(ownerClass: String, methodName: String, args: Seq[String], isStatic: Boolean) extends TraceElement {
  override def toString: String = s"CALL $ownerClass :: $methodName(${args.mkString(",")})"
}

final case class Initialization(dateTime: String) extends TraceElement {
  // dropWhile thing removes the nanoseconds
  override def toString: String = s"TRACE INITIALIZED AT ${dateTime.reverse.dropWhile(_ != '.').reverse.init}"
}

final case class Termination(msg: String) extends TraceElement {
  override def toString: String = msg
}
