package b2bCompiler

import AbstractInterpreter.{AbsIntException, AbsIntStack}
import org.objectweb.asm.Label

import UnaryOperator.*
import BinaryOperator.*

import scala.collection.mutable

final class AbstractInterpreter extends PipelineStage[TablesCreator.Output, AbstractInterpreter.Output] {

  override def run(in: TablesCreator.Output): AbstractInterpreter.Output = {
    in.map { (table, code) =>
      (table, augmentWithInterpretationData(code, table))
    }
  }

  private def augmentWithInterpretationData(code: Seq[RegularBytecodeInstr], localsTable: LocalsTable): Seq[BytecodeInstr] = {

    val absIntValueCreator = new AbsIntValue.Creator()
    import absIntValueCreator.*

    val stack = new AbsIntStack()
    val codeB = Seq.newBuilder[BytecodeInstr]
    val alreadySeenLabels = mutable.Set.empty[Label]

    for (instr <- code) {
      codeB.addOne(instr)
      instr match {

        case LabelOccurenceB(label) =>
          alreadySeenLabels.addOne(label)

        // second layer so that the compiler checks that all cases are covered
        case effectingBytecodeInstr: EffectingBytecodeInstr => {

          assert(!effectingBytecodeInstr.isInstanceOf[JumpInsn] || stack.isEmpty)

          import org.objectweb.asm.Opcodes.*
          effectingBytecodeInstr match {

            case Insn(NOP) => ()
            case Insn(ACONST_NULL) =>
              stack.push(Constant(null))
            case Insn(ICONST_M1) =>
              stack.push(Constant(-1))
            case Insn(ICONST_0) =>
              stack.push(Constant(0))
            case Insn(ICONST_1) =>
              stack.push(Constant(1))
            case Insn(ICONST_2) =>
              stack.push(Constant(2))
            case Insn(ICONST_3) =>
              stack.push(Constant(3))
            case Insn(ICONST_4) =>
              stack.push(Constant(4))
            case Insn(ICONST_5) =>
              stack.push(Constant(5))
            case Insn(LCONST_0) =>
              stack.push(Constant(0.toLong))
            case Insn(LCONST_1) =>
              stack.push(Constant(1.toLong))
            case Insn(FCONST_0) =>
              stack.push(Constant(0f))
            case Insn(FCONST_1) =>
              stack.push(Constant(1f))
            case Insn(FCONST_2) =>
              stack.push(Constant(2f))
            case Insn(DCONST_0) =>
              stack.push(Constant(0.0))
            case Insn(DCONST_1) =>
              stack.push(Constant(1.0))
            case Insn(IALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, IntT))
            case Insn(LALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, LongT))
            case Insn(FALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, FloatT))
            case Insn(DALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, DoubleT))
            case Insn(AALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, ObjectRefT))
            case Insn(BALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, ByteT))
            case Insn(CALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, CharT))
            case Insn(SALOAD) =>
              val idx = stack.pop(IntT)
              val arr = stack.pop(ObjectRefT)
              stack.push(ArrayAccess(arr, idx, ShortT))
            case Insn(IASTORE) =>
              stack.pop(IntT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(LASTORE) =>
              stack.pop(LongT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(FASTORE) =>
              stack.pop(FloatT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(DASTORE) =>
              stack.pop(DoubleT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(AASTORE) =>
              stack.pop(ObjectRefT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(BASTORE) =>
              stack.pop(ByteT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(CASTORE) =>
              stack.pop(CharT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(SASTORE) =>
              stack.pop(ShortT)
              stack.pop(IntT)
              stack.pop(ObjectRefT)
            case Insn(POP) =>
              stack.pop1()
            case Insn(POP2) =>
              stack.pop2()
            case Insn(DUP) =>
              val e = stack.pop1()
              stack.push(e)
              stack.push(e)
            case Insn(DUP_X1) => ???
            case Insn(DUP_X2) => ???
            case Insn(DUP2) =>
              val e = stack.pop2()
              stack.push(e)
              stack.push(e)
            case Insn(DUP2_X1) => ???
            case Insn(DUP2_X2) => ???
            case Insn(SWAP) =>
              val v2 = stack.pop1()
              val v1 = stack.pop1()
              stack.push(v2)
              stack.push(v1)
            case Insn(IADD) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(Add, v1, v2, IntT))
            case Insn(LADD) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(Add, v1, v2, LongT))
            case Insn(FADD) =>
              val v2 = stack.pop(FloatT)
              val v1 = stack.pop(FloatT)
              stack.push(BinaryOperation(Add, v1, v2, FloatT))
            case Insn(DADD) =>
              val v2 = stack.pop(DoubleT)
              val v1 = stack.pop(DoubleT)
              stack.push(BinaryOperation(Add, v1, v2, DoubleT))
            case Insn(ISUB) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(Sub, v1, v2, IntT))
            case Insn(LSUB) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(Sub, v1, v2, LongT))
            case Insn(FSUB) =>
              val v2 = stack.pop(FloatT)
              val v1 = stack.pop(FloatT)
              stack.push(BinaryOperation(Sub, v1, v2, FloatT))
            case Insn(DSUB) =>
              val v2 = stack.pop(DoubleT)
              val v1 = stack.pop(DoubleT)
              stack.push(BinaryOperation(Sub, v1, v2, DoubleT))
            case Insn(IMUL) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(Mul, v1, v2, IntT))
            case Insn(LMUL) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(Mul, v1, v2, LongT))
            case Insn(FMUL) =>
              val v2 = stack.pop(FloatT)
              val v1 = stack.pop(FloatT)
              stack.push(BinaryOperation(Mul, v1, v2, FloatT))
            case Insn(DMUL) =>
              val v2 = stack.pop(DoubleT)
              val v1 = stack.pop(DoubleT)
              stack.push(BinaryOperation(Mul, v1, v2, DoubleT))
            case Insn(IDIV) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(Div, v1, v2, IntT))
            case Insn(LDIV) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(Div, v1, v2, LongT))
            case Insn(FDIV) =>
              val v2 = stack.pop(FloatT)
              val v1 = stack.pop(FloatT)
              stack.push(BinaryOperation(Div, v1, v2, FloatT))
            case Insn(DDIV) =>
              val v2 = stack.pop(DoubleT)
              val v1 = stack.pop(DoubleT)
              stack.push(BinaryOperation(Div, v1, v2, DoubleT))
            case Insn(IREM) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(Mod, v1, v2, IntT))
            case Insn(LREM) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(Mod, v1, v2, LongT))
            case Insn(FREM) =>
              val v2 = stack.pop(FloatT)
              val v1 = stack.pop(FloatT)
              stack.push(BinaryOperation(Mod, v1, v2, FloatT))
            case Insn(DREM) =>
              val v2 = stack.pop(DoubleT)
              val v1 = stack.pop(DoubleT)
              stack.push(BinaryOperation(Mod, v1, v2, DoubleT))
            case Insn(INEG) =>
              val v = stack.pop(IntT)
              stack.push(UnaryOperation(Neg, v, IntT))
            case Insn(LNEG) =>
              val v = stack.pop(LongT)
              stack.push(UnaryOperation(Neg, v, LongT))
            case Insn(FNEG) =>
              val v = stack.pop(FloatT)
              stack.push(UnaryOperation(Neg, v, FloatT))
            case Insn(DNEG) =>
              val v = stack.pop(DoubleT)
              stack.push(UnaryOperation(Neg, v, DoubleT))
            case Insn(ISHL) =>
              val shift = stack.pop(IntT)
              val v = stack.pop(IntT)
              stack.push(BinaryOperation(ShiftLeft, v, shift, IntT))
            case Insn(LSHL) =>
              val shift = stack.pop(IntT)
              val v = stack.pop(LongT)
              stack.push(BinaryOperation(ShiftLeft, v, shift, LongT))
            case Insn(ISHR) =>
              val shift = stack.pop(IntT)
              val v = stack.pop(IntT)
              stack.push(BinaryOperation(ShiftRight, v, shift, IntT))
            case Insn(LSHR) =>
              val shift = stack.pop(IntT)
              val v = stack.pop(LongT)
              stack.push(BinaryOperation(ShiftRight, v, shift, LongT))
            case Insn(IUSHR) =>
              val shift = stack.pop(IntT)
              val v = stack.pop(IntT)
              stack.push(BinaryOperation(UnsignedShiftRight, v, shift, IntT))
            case Insn(LUSHR) =>
              val shift = stack.pop(IntT)
              val v = stack.pop(LongT)
              stack.push(BinaryOperation(UnsignedShiftRight, v, shift, LongT))
            case Insn(IAND) => ???
            case Insn(LAND) => ???
            case Insn(IOR) => ???
            case Insn(LOR) => ???
            case Insn(IXOR) => ???
            case Insn(LXOR) => ???
            case Insn(I2L) =>
              val v = stack.pop(IntT)
              stack.push(Converted(v, LongT))
            case Insn(I2F) =>
              val v = stack.pop(IntT)
              stack.push(Converted(v, FloatT))
            case Insn(I2D) =>
              val v = stack.pop(IntT)
              stack.push(Converted(v, DoubleT))
            case Insn(L2I) =>
              val v = stack.pop(LongT)
              stack.push(Converted(v, IntT))
            case Insn(L2F) =>
              val v = stack.pop(LongT)
              stack.push(Converted(v, FloatT))
            case Insn(L2D) =>
              val v = stack.pop(LongT)
              stack.push(Converted(v, DoubleT))
            case Insn(F2I) =>
              val v = stack.pop(FloatT)
              stack.push(Converted(v, IntT))
            case Insn(F2L) =>
              val v = stack.pop(FloatT)
              stack.push(Converted(v, LongT))
            case Insn(F2D) =>
              val v = stack.pop(FloatT)
              stack.push(Converted(v, DoubleT))
            case Insn(D2I) =>
              val v = stack.pop(DoubleT)
              stack.push(Converted(v, IntT))
            case Insn(D2L) =>
              val v = stack.pop(DoubleT)
              stack.push(Converted(v, LongT))
            case Insn(D2F) =>
              val v = stack.pop(DoubleT)
              stack.push(Converted(v, FloatT))
            case Insn(I2B) =>
              val v = stack.pop(IntT)
              stack.push(Converted(v, ByteT))
            case Insn(I2C) =>
              val v = stack.pop(IntT)
              stack.push(Converted(v, CharT))
            case Insn(I2S) =>
              val v = stack.pop(IntT)
              stack.push(Converted(v, ShortT))
            case Insn(LCMP) => ???
            case Insn(FCMPL) => ???
            case Insn(FCMPG) => ???
            case Insn(DCMPL) => ???
            case Insn(DCMPG) => ???
            case Insn(IRETURN) =>
              stack.pop(IntT)
            case Insn(LRETURN) =>
              stack.pop(LongT)
            case Insn(FRETURN) =>
              stack.pop(LongT)
            case Insn(DRETURN) =>
              stack.pop(DoubleT)
            case Insn(ARETURN) =>
              stack.pop(ObjectRefT)
            case Insn(RETURN) => ()
            case Insn(ARRAYLENGTH) => ???
            case Insn(ATHROW) => {
              /* Leave it on the stack for the handler. Frame may have to be cleared, but it seems to not be an issue
              *  if we don't do it (will just keep unused values on the stack) */
              ()
            }
            case Insn(MONITORENTER) => ???
            case Insn(MONITOREXIT) => ???

            case IntInsn(BIPUSH, operand) => ???
            case IntInsn(SIPUSH, operand) => ???
            case IntInsn(NEWARRAY, operand) => ???

            case VarInsn(ILOAD, varIndex) =>
              val varName = localsTable.findLocal(varIndex, alreadySeenLabels).name
              stack.push(VariableAccess(varName, IntT))
            case VarInsn(LLOAD, varIndex) =>
              val varName = localsTable.findLocal(varIndex, alreadySeenLabels).name
              stack.push(VariableAccess(varName, LongT))
            case VarInsn(FLOAD, varIndex) =>
              val varName = localsTable.findLocal(varIndex, alreadySeenLabels).name
              stack.push(VariableAccess(varName, FloatT))
            case VarInsn(DLOAD, varIndex) =>
              val varName = localsTable.findLocal(varIndex, alreadySeenLabels).name
              stack.push(VariableAccess(varName, DoubleT))
            case VarInsn(ALOAD, varIndex) =>
              val varName = localsTable.findLocal(varIndex, alreadySeenLabels).name
              stack.push(VariableAccess(varName, ObjectRefT))
            case VarInsn(ISTORE, varIndex) =>
              stack.pop(IntT)
            case VarInsn(LSTORE, varIndex) =>
              stack.pop(LongT)
            case VarInsn(FSTORE, varIndex) =>
              stack.pop(FloatT)
            case VarInsn(DSTORE, varIndex) =>
              stack.pop(DoubleT)
            case VarInsn(ASTORE, varIndex) =>
              stack.pop(ObjectRefT)
            case VarInsn(RET, varIndex) => ()

            case TypeInsn(NEW, tpe) => ???
            case TypeInsn(ANEWARRAY, tpe) => ???
            case TypeInsn(CHECKCAST, tpe) => ???
            case TypeInsn(INSTANCEOF, tpe) => ???

            case FieldInsn(GETSTATIC, owner, name, descriptor) => ???
            case FieldInsn(PUTSTATIC, owner, name, descriptor) => ???
            case FieldInsn(GETFIELD, owner, name, descriptor) => ???
            case FieldInsn(PUTFIELD, owner, name, descriptor) => ???

            case MethodInsn(INVOKEVIRTUAL, owner, name, descriptor, isInterface) => ???
            case MethodInsn(INVOKESPECIAL, owner, name, descriptor, isInterface) => ???
            case MethodInsn(INVOKESTATIC, owner, name, descriptor, isInterface) => ???
            case MethodInsn(INVOKEINTERFACE, owner, name, descriptor, isInterface) => ???

            case InvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments) => ???

            case JumpInsn(IFEQ, label) => ???
            case JumpInsn(IFNE, label) => ???
            case JumpInsn(IFLT, label) => ???
            case JumpInsn(IFGE, label) => ???
            case JumpInsn(IFGT, label) => ???
            case JumpInsn(IFLE, label) => ???
            case JumpInsn(IF_ICMPEQ, label) => ???
            case JumpInsn(IF_ICMPNE, label) => ???
            case JumpInsn(IF_ICMPLT, label) => ???
            case JumpInsn(IF_ICMPGE, label) => ???
            case JumpInsn(IF_ICMPGT, label) => ???
            case JumpInsn(IF_ICMPLE, label) => ???
            case JumpInsn(IF_ACMPEQ, label) => ???
            case JumpInsn(IF_ACMPNE, label) => ???
            case JumpInsn(GOTO, label) => ???
            case JumpInsn(JSR, label) => ???
            case JumpInsn(IFNULL, label) => ???
            case JumpInsn(IFNONNULL, label) => ???

            case LdcInsn(value) => ???

            case IincInsn(varIndex, increment) => ???

            case TableSwitchInsn(min, max, dflt, labels) => ???

            case LookupSwitchInsn(dflt, keys, labels) => ???

            case MultiANewArrayInsn(descriptor, numDimensions) => ???

            case unexpected => throw new AssertionError(s"unexpected: $unexpected")
          }
          codeB.addOne(StackState(stack.currState))
        }
        case _ => ()
      }
    }
    codeB.result()
  }

}

object AbstractInterpreter {

  type Output = Seq[(LocalsTable, Seq[BytecodeInstr])]

  final case class AbsIntException(msg: String) extends RuntimeException(msg)

  final class AbsIntStack {
    private var elems: List[AbsIntValue] = List.empty

    def isEmpty: Boolean = elems.isEmpty

    def currState: List[AbsIntValue] = elems

    def push(absIntValue: AbsIntValue): Unit = {
      elems = absIntValue :: elems
    }

    def pop1(): AbsIntValue = {
      elems match {
        case Nil =>
          throw AbsIntException("empty stack")
        case topElem :: others =>
          elems = others
          if (topElem.tpe.nSlots != 1) {
            throw AbsIntException(s"expected value on 1 slot, found ${topElem.tpe}")
          }
          topElem
      }
    }

    def pop2(): AbsIntValue = {
      elems match {
        case Nil =>
          throw AbsIntException("empty stack")
        case topElem :: others =>
          elems = others
          if (topElem.tpe.nSlots != 2) {
            throw AbsIntException(s"expected value on 2 slots, found ${topElem.tpe}")
          }
          topElem
      }
    }

    def pop(sig: TypeSignature): AbsIntValue = {
      elems match {
        case Nil =>
          throw AbsIntException("empty stack")
        case topElem :: others =>
          elems = others
          if (typeCheck(sig, topElem.tpe)) {
            throw AbsIntException(s"expected $sig, found ${topElem.tpe} ($topElem)")
          }
          topElem
      }
    }
  }

  private def typeCheck(expected: TypeSignature, actual: TypeSignature): Boolean = {
    (expected, actual) match
      case _ if expected == actual => true
      case (BooleanT, ByteT) => true // may result from array access, as baload works on both boolean[] and byte[]
      case (BooleanT, IntT) => true // for constants
      case (ByteT, IntT) => true // for constants
      case (CharT, IntT) => true // for constants
      case (ShortT, IntT) => true // for constants
      case _ => false
  }

}
