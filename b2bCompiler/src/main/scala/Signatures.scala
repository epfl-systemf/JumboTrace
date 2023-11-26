package b2bCompiler

sealed trait TypeSignature {
  val nSlots: Int

  override def toString: String = toStringImpl

  protected def toStringImpl: String
}

trait RegularTypeSignature(override val nSlots: Int, val descriptor: String) extends TypeSignature

object BooleanT extends RegularTypeSignature(1, "Z"){
  override protected def toStringImpl: String = "boolean"
}

object CharT extends RegularTypeSignature(1, "C"){
  override protected def toStringImpl: String = "char"
}

object ByteT extends RegularTypeSignature(1, "B"){
  override protected def toStringImpl: String = "byte"
}

object ShortT extends RegularTypeSignature(1, "S"){
  override protected def toStringImpl: String = "short"
}

object IntT extends RegularTypeSignature(1, "I"){
  override protected def toStringImpl: String = "int"
}

object FloatT extends RegularTypeSignature(1, "F"){
  override protected def toStringImpl: String = "float"
}

object LongT extends RegularTypeSignature(2, "J"){
  override protected def toStringImpl: String = "long"
}

object DoubleT extends RegularTypeSignature(2, "D"){
  override protected def toStringImpl: String = "double"
}

object ObjectRefT extends RegularTypeSignature(1, "Ljava/lang/Object;"){
  override protected def toStringImpl: String = "Object"
}

object UnknownT extends TypeSignature {
  override val nSlots: Int = 1

  override protected def toStringImpl: String = "<unknown type>"
}

object TypeSignature {

  def of(x: Any): TypeSignature = {
    x match
      case _: Boolean => BooleanT
      case _: Char => CharT
      case _: Byte => ByteT
      case _: Short => ShortT
      case _: Int => IntT
      case _: Float => FloatT
      case _: Long => LongT
      case _: Double => DoubleT
      case _ => ObjectRefT
  }

  def parse(str: String): RegularTypeSignature = {
    str match
      case "Z" => BooleanT
      case "C" => CharT
      case "B" => ByteT
      case "S" => ShortT
      case "I" => IntT
      case "F" => FloatT
      case "J" => LongT
      case "D" => DoubleT
      case _ =>
        assert(str.startsWith("L") || str.startsWith("["), s"first character is invalid: $str")
        ObjectRefT
  }

}

final case class MethodSignature(
                                  params: Seq[RegularTypeSignature],
                                  retType: Option[RegularTypeSignature]
                                ) {

  def descriptor: String = {
    params.map(_.descriptor).mkString("(", "", ")") ++ retType.map(_.descriptor).getOrElse("V")
  }
  
  def isVoidMethod: Boolean = retType.isEmpty

}

object MethodSignature {

  def parse(str: String): MethodSignature = {

    enum State {
      case SeenArray, InsideObject, Default
    }
    import State.*

    assert(str.startsWith("("))
    val Array(paramsStr, retTypeStr) = str.tail.split(Array(')'))
    var state = Default
    val paramsB = List.newBuilder[RegularTypeSignature]
    for (char <- paramsStr) {
      (char, state) match {
        case ('L', Default) =>
          paramsB.addOne(ObjectRefT)
          state = InsideObject
        case ('L', SeenArray | InsideObject) =>
          state = InsideObject
        case ('[', Default) =>
          paramsB.addOne(ObjectRefT)
          state = SeenArray
        case ('[', SeenArray) => ()
        case (';', InsideObject) =>
          state = Default
        case (_, InsideObject) => ()
        case ('Z', Default) => paramsB.addOne(BooleanT)
        case ('C', Default) => paramsB.addOne(CharT)
        case ('B', Default) => paramsB.addOne(ByteT)
        case ('S', Default) => paramsB.addOne(ShortT)
        case ('I', Default) => paramsB.addOne(IntT)
        case ('F', Default) => paramsB.addOne(FloatT)
        case ('J', Default) => paramsB.addOne(LongT)
        case ('D', Default) => paramsB.addOne(DoubleT)
        case _ => throw new AssertionError(s"unexpected '$char' in state $state")
      }
    }
    val params = paramsB.result()
    val optResType = if retTypeStr == "V" then None else Some(TypeSignature.parse(retTypeStr))
    MethodSignature(params, optResType)
  }

}