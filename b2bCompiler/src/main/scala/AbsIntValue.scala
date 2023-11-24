package b2bCompiler

import AbsIntValue.Stamp

import java.util.concurrent.atomic.AtomicLong

sealed abstract class AbsIntValue(using creator: AbsIntValue.Creator) {
  val stamp: Stamp = creator.stampGen.incrementAndGet()

  def tpe: TypeSignature

  override def toString: String = toStringImpl

  protected def toStringImpl: String
}

object AbsIntValue {

  type Stamp = Long

  final class Creator {
    private[AbsIntValue] val stampGen = new AtomicLong(0)

    private given Creator = this

    final case class Constant(cst: Any) extends AbsIntValue {
      override def tpe: TypeSignature = TypeSignature.of(cst)

      override protected def toStringImpl: String = {
        cst match
          case null => "null"
          case str: String => s"\"$str\""
          case _ => cst.toString
      }
    }

    final case class VariableAccess(name: String, tpe: TypeSignature) extends AbsIntValue {
      override protected def toStringImpl: String = name
    }

    final case class StaticFieldAccess(className: String, fieldName: String, tpe: TypeSignature) extends AbsIntValue {
      override protected def toStringImpl: String = s"$className.$fieldName"
    }

    final case class InstanceFieldAccess(instance: AbsIntValue, fieldName: String, tpe: TypeSignature) extends AbsIntValue {
      override protected def toStringImpl: String = s"${p(instance)}.$fieldName"
    }

    final case class StaticInvocation(className: String, methodName: String, sig: MethodSignature,
                                      args: Seq[AbsIntValue]) extends AbsIntValue {
      override def tpe: TypeSignature = sig.retType.getOrElse(throw UnsupportedOperationException("not supported for void methods"))

      override protected def toStringImpl: String = s"$className.$methodName" ++ args.mkString("(", ",", ")")
    }

    final case class DynamicInvocation(className: String, methodName: String, sig: MethodSignature,
                                       receiver: AbsIntValue, args: Seq[AbsIntValue]) extends AbsIntValue {
      override def tpe: TypeSignature = sig.retType.getOrElse(throw UnsupportedOperationException("not supported for void methods"))

      override protected def toStringImpl: String = s"${p(receiver)}.$methodName" ++ args.mkString("(", ",", ")")
    }
    
    final case class UnaryOperation(operator: UnaryOperator, operand: AbsIntValue, tpe: TypeSignature) extends AbsIntValue {
      override protected def toStringImpl: String = s"$operator${p(operand)}"
    }
    
    final case class BinaryOperation(operation: BinaryOperator, operand1: AbsIntValue,
                                     operand2: AbsIntValue, tpe: TypeSignature) extends AbsIntValue {
      override protected def toStringImpl: String = s"${p(operand1)} $operation ${p(operand2)}"
    }
    
    final case class ArrayAccess(array: AbsIntValue, idx: AbsIntValue, tpe: TypeSignature) extends AbsIntValue {
      override protected def toStringImpl: String = s"${p(array)}[$idx]"
    }
    
    final case class Converted(value: AbsIntValue, tpe: TypeSignature) extends AbsIntValue {
      override protected def toStringImpl: String = value.toString
    }

    private def p(absIntValue: AbsIntValue): String = {
      absIntValue match {
        case absIntValue: (VariableAccess | Constant) => absIntValue.toString
        case _ => s"($absIntValue)"
      }
    }
    
  }

}
