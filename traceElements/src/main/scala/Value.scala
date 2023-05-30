package traceElements

sealed trait Value {
  val tpe: String
  val value: String
  
  def shortDescr: String
  
}

final case class PrimitiveValue(override val tpe: String, override val value: String) extends Value {
  override def shortDescr: String = value
}

final case class ReferenceValue(override val tpe: String, hashcode: Int, override val value: String) extends Value {
  override def shortDescr: String = s"${tpe}_$hashcode"
}
