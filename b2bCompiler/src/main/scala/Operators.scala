package b2bCompiler

enum UnaryOperator(symbol: String) {
  case Not extends UnaryOperator("!")
  case Neg extends UnaryOperator("-")

  override def toString: String = symbol
}

enum BinaryOperator(symbol: String) {

  case Add extends BinaryOperator("+")
  case Sub extends BinaryOperator("-")
  case Mul extends BinaryOperator("*")
  case Div extends BinaryOperator("/")
  case Mod extends BinaryOperator("%")
  
  case Equals extends BinaryOperator("==")
  case NotEquals extends BinaryOperator("!=")
  case LessThan extends BinaryOperator("<")
  case GreaterThan extends BinaryOperator(">")
  case LessEq extends BinaryOperator("<=")
  case GreaterEq extends BinaryOperator(">=")

  case ShiftLeft extends BinaryOperator("<<")
  case ShiftRight extends BinaryOperator(">>")
  case UnsignedShiftRight extends BinaryOperator(">>>")

  case BitwiseAnd extends BinaryOperator("&")
  case BitwiseOr extends BinaryOperator("||")
  case BitwiseXor extends BinaryOperator("^")

  override def toString: String = symbol
}
