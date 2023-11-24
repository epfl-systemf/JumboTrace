package b2bCompiler

import AbsIntValue.Stamp

import java.util.concurrent.atomic.AtomicLong

sealed abstract class AbsIntValue(using creator: AbsIntValue.Creator) {
  val stamp: Stamp = creator.stampGen.incrementAndGet()

  def tpe: TypeSignature
}

object AbsIntValue {

  type Stamp = Long

  final class Creator {
    private[AbsIntValue] val stampGen = new AtomicLong(0)

    private given Creator = this

    final case class Constant(cst: Any) extends AbsIntValue {
      override def tpe: TypeSignature = TypeSignature.of(cst)
    }

    final case class VariableAccess(name: String, tpe: TypeSignature) extends AbsIntValue

    final case class StaticFieldAccess(className: String, fieldName: String, tpe: TypeSignature) extends AbsIntValue

    final case class InstanceFieldAccess(instance: AbsIntValue, fieldName: String, tpe: TypeSignature) extends AbsIntValue

    final case class StaticInvocation(className: String, methodName: String, sig: MethodSignature,
                                      args: Seq[AbsIntValue]) extends AbsIntValue {
      override def tpe: TypeSignature = sig.retType.getOrElse(throw UnsupportedOperationException("not supported for void methods"))
    }

    final case class DynamicInvocation(className: String, methodName: String, sig: MethodSignature,
                                       receiver: AbsIntValue, args: Seq[AbsIntValue]) extends AbsIntValue {
      override def tpe: TypeSignature = sig.retType.getOrElse(throw UnsupportedOperationException("not supported for void methods"))
    }
    
    final case class UnaryOperation(operator: UnaryOperator, operand: AbsIntValue, tpe: TypeSignature) extends AbsIntValue
    
    final case class BinaryOperation(operation: BinaryOperator, operand1: AbsIntValue,
                                     operand2: AbsIntValue, tpe: TypeSignature) extends AbsIntValue
    
    final case class Converted(value: AbsIntValue, tpe: TypeSignature) extends AbsIntValue
    
  }

}
