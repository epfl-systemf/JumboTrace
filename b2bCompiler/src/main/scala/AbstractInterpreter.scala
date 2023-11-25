package b2bCompiler

import AbstractInterpreter.{AbsIntException, AbsIntStack}
import org.objectweb.asm.Label

import UnaryOperator.*
import BinaryOperator.*

import scala.collection.mutable

final class AbstractInterpreter extends PipelineStage[TablesCreator.Output, AbstractInterpreter.Output] {

  override def run(in: TablesCreator.Output): AbstractInterpreter.Output = {
    in.map { (table, code) =>
      (table.methodUid, augmentWithInterpretationData(code, table))
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

          // TODO this assertion does not hold: is it an issue?
//          assert(!effectingBytecodeInstr.isInstanceOf[JumpInsn] || stack.isEmpty)

          import org.objectweb.asm.Opcodes.*
          effectingBytecodeInstr match {

            case Insn(NOP) => ()
            case Insn(ACONST_NULL) =>
              stack.push(Constant(null, ObjectRefT))
            case Insn(ICONST_M1) =>
              stack.push(Constant(-1, IntT))
            case Insn(ICONST_0) =>
              stack.push(Constant(0, IntT))
            case Insn(ICONST_1) =>
              stack.push(Constant(1, IntT))
            case Insn(ICONST_2) =>
              stack.push(Constant(2, IntT))
            case Insn(ICONST_3) =>
              stack.push(Constant(3, IntT))
            case Insn(ICONST_4) =>
              stack.push(Constant(4, IntT))
            case Insn(ICONST_5) =>
              stack.push(Constant(5, IntT))
            case Insn(LCONST_0) =>
              stack.push(Constant(0.toLong, LongT))
            case Insn(LCONST_1) =>
              stack.push(Constant(1.toLong, LongT))
            case Insn(FCONST_0) =>
              stack.push(Constant(0f, FloatT))
            case Insn(FCONST_1) =>
              stack.push(Constant(1f, FloatT))
            case Insn(FCONST_2) =>
              stack.push(Constant(2f, FloatT))
            case Insn(DCONST_0) =>
              stack.push(Constant(0.0, DoubleT))
            case Insn(DCONST_1) =>
              stack.push(Constant(1.0, DoubleT))
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
            case Insn(DUP_X1) =>
              val v1 = stack.pop1()
              val v2 = stack.pop1()
              stack.push(v1)
              stack.push(v2)
              stack.push(v1)
            case Insn(DUP_X2) =>
              val v1 = stack.pop1()
              val v2 = stack.pop1()
              val v3 = stack.pop1()
              stack.push(v1)
              stack.push(v3)
              stack.push(v2)
              stack.push(v1)
            case Insn(DUP2) =>
              val e = stack.pop2()
              stack.push(e)
              stack.push(e)
            case Insn(DUP2_X1) =>
              val v12 = stack.pop2()
              val v3 = stack.pop1()
              stack.push(v12)
              stack.push(v3)
              stack.push(v12)
            case Insn(DUP2_X2) =>
              val v12 = stack.pop2()
              val v34 = stack.pop2()
              stack.push(v12)
              stack.push(v34)
              stack.push(v12)
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
            case Insn(IAND) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(BitwiseAnd, v1, v2, IntT))
            case Insn(LAND) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(BitwiseAnd, v1, v2, LongT))
            case Insn(IOR) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(BitwiseOr, v1, v2, IntT))
            case Insn(LOR) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(BitwiseOr, v1, v2, LongT))
            case Insn(IXOR) =>
              val v2 = stack.pop(IntT)
              val v1 = stack.pop(IntT)
              stack.push(BinaryOperation(BitwiseXor, v1, v2, IntT))
            case Insn(LXOR) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(BinaryOperation(BitwiseXor, v1, v2, LongT))
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
            case Insn(LCMP) =>
              val v2 = stack.pop(LongT)
              val v1 = stack.pop(LongT)
              stack.push(Comparison(v1, v2))
            case Insn(FCMPL) =>
              val v2 = stack.pop(FloatT)
              val v1 = stack.pop(FloatT)
              stack.push(Comparison(v1, v2))
            case Insn(FCMPG) =>
              val v2 = stack.pop(FloatT)
              val v1 = stack.pop(FloatT)
              stack.push(Comparison(v1, v2))
            case Insn(DCMPL) =>
              val v2 = stack.pop(DoubleT)
              val v1 = stack.pop(DoubleT)
              stack.push(Comparison(v1, v2))
            case Insn(DCMPG) =>
              val v2 = stack.pop(DoubleT)
              val v1 = stack.pop(DoubleT)
              stack.push(Comparison(v1, v2))
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
            case Insn(ARRAYLENGTH) =>
              val arr = stack.pop(ObjectRefT)
              stack.push(InstanceFieldAccess(arr, "length", IntT))
            case Insn(ATHROW) => {
              /* Leave it on the stack for the handler. Frame may have to be cleared, but it seems to not be an issue
              *  if we don't do it (will just keep unused values on the stack) */
              ()
            }
            case Insn(MONITORENTER) =>
              stack.pop(ObjectRefT)
            case Insn(MONITOREXIT) =>
              stack.pop(ObjectRefT)

            case IntInsn(BIPUSH, operand) =>
              stack.push(Constant(operand, IntT))
            case IntInsn(SIPUSH, operand) =>
              stack.push(Constant(operand, IntT))

            case IntInsn(NEWARRAY, T_BOOLEAN) =>
              val length = stack.pop(IntT)
              stack.push(Array(BooleanT, length))
            case IntInsn(NEWARRAY, T_CHAR) =>
              val length = stack.pop(IntT)
              stack.push(Array(CharT, length))
            case IntInsn(NEWARRAY, T_FLOAT) =>
              val length = stack.pop(IntT)
              stack.push(Array(FloatT, length))
            case IntInsn(NEWARRAY, T_DOUBLE) =>
              val length = stack.pop(IntT)
              stack.push(Array(DoubleT, length))
            case IntInsn(NEWARRAY, T_BYTE) =>
              val length = stack.pop(IntT)
              stack.push(Array(ByteT, length))
            case IntInsn(NEWARRAY, T_SHORT) =>
              val length = stack.pop(IntT)
              stack.push(Array(ShortT, length))
            case IntInsn(NEWARRAY, T_INT) =>
              val length = stack.pop(IntT)
              stack.push(Array(IntT, length))
            case IntInsn(NEWARRAY, T_LONG) =>
              val length = stack.pop(IntT)
              stack.push(Array(LongT, length))

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
            case VarInsn(RET, varIndex) =>
              throw new UnsupportedOperationException("opcode RET is not supported")

            case TypeInsn(NEW, tpe) =>
              stack.push(ObjectInstance(tpe))
            case TypeInsn(ANEWARRAY, tpe) =>
              val length = stack.pop(IntT)
              stack.push(Array(ObjectRefT, length))
            case TypeInsn(CHECKCAST, tpe) => ()
            case TypeInsn(INSTANCEOF, tpe) =>
              val v = stack.pop(ObjectRefT)
              stack.push(InstanceOf(v, tpe))

            case FieldInsn(GETSTATIC, owner, name, descriptor) =>
              val tpe = TypeSignature.parse(descriptor)
              stack.push(StaticFieldAccess(owner, name, tpe))
            case FieldInsn(PUTSTATIC, owner, name, descriptor) =>
              val tpe = TypeSignature.parse(descriptor)
              stack.pop(tpe)
            case FieldInsn(GETFIELD, owner, name, descriptor) =>
              val tpe = TypeSignature.parse(descriptor)
              val receiver = stack.pop(ObjectRefT)
              stack.push(InstanceFieldAccess(receiver, name, tpe))
            case FieldInsn(PUTFIELD, owner, name, descriptor) =>
              val tpe = TypeSignature.parse(descriptor)
              stack.pop(tpe)
              stack.pop(ObjectRefT)

            case MethodInsn(INVOKEVIRTUAL, owner, name, descriptor, isInterface) =>
              val sig = MethodSignature.parse(descriptor)
              val args = stack.popMultiple(sig.params)
              val receiver = stack.pop(ObjectRefT)
              if (!sig.isVoidMethod){
                stack.push(InvokeWithReceiver(owner, name, sig, receiver, args))
              }
            case MethodInsn(INVOKESPECIAL, owner, name, descriptor, isInterface) =>
              val sig = MethodSignature.parse(descriptor)
              val args = stack.popMultiple(sig.params)
              val receiver = stack.pop(ObjectRefT)
              if (!sig.isVoidMethod) {
                stack.push(InvokeWithReceiver(owner, name, sig, receiver, args))
              }
            case MethodInsn(INVOKESTATIC, owner, name, descriptor, isInterface) =>
              val sig = MethodSignature.parse(descriptor)
              val args = stack.popMultiple(sig.params)
              if (!sig.isVoidMethod){
                stack.push(InvokeWithoutReceiver(owner, name, sig, args))
              }
            case MethodInsn(INVOKEINTERFACE, owner, name, descriptor, isInterface) =>
              val sig = MethodSignature.parse(descriptor)
              val args = stack.popMultiple(sig.params)
              val receiver = stack.pop(ObjectRefT)
              if (!sig.isVoidMethod) {
                stack.push(InvokeWithReceiver(owner, name, sig, receiver, args))
              }

            case InvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments) =>
              val sig = MethodSignature.parse(descriptor)
              val args = stack.popMultiple(sig.params)
              // FIXME "unknown", if possible to do better
              if (!sig.isVoidMethod){
                stack.push(InvokeWithoutReceiver("<unknown>", name, sig, args))
              }

            case JumpInsn(IFEQ, label) =>
              stack.pop(IntT)
            case JumpInsn(IFNE, label) =>
              stack.pop(IntT)
            case JumpInsn(IFLT, label) =>
              stack.pop(IntT)
            case JumpInsn(IFGE, label) =>
              stack.pop(IntT)
            case JumpInsn(IFGT, label) =>
              stack.pop(IntT)
            case JumpInsn(IFLE, label) =>
              stack.pop(IntT)
            case JumpInsn(IF_ICMPEQ, label) =>
              stack.popMultiple(Seq(IntT, IntT))
            case JumpInsn(IF_ICMPNE, label) =>
              stack.popMultiple(Seq(IntT, IntT))
            case JumpInsn(IF_ICMPLT, label) =>
              stack.popMultiple(Seq(IntT, IntT))
            case JumpInsn(IF_ICMPGE, label) =>
              stack.popMultiple(Seq(IntT, IntT))
            case JumpInsn(IF_ICMPGT, label) =>
              stack.popMultiple(Seq(IntT, IntT))
            case JumpInsn(IF_ICMPLE, label) =>
              stack.popMultiple(Seq(IntT, IntT))
            case JumpInsn(IF_ACMPEQ, label) =>
              stack.popMultiple(Seq(ObjectRefT, ObjectRefT))
            case JumpInsn(IF_ACMPNE, label) =>
              stack.popMultiple(Seq(ObjectRefT, ObjectRefT))
            case JumpInsn(GOTO, label) => ()
            case JumpInsn(JSR, label) =>
              throw new UnsupportedOperationException("opcode JSR is not supported")
            case JumpInsn(IFNULL, label) =>
              stack.pop(ObjectRefT)
            case JumpInsn(IFNONNULL, label) =>
              stack.pop(ObjectRefT)

            case LdcInsn(value) =>
              stack.push(Constant(value, TypeSignature.of(value)))

            case IincInsn(varIndex, increment) => ()

            case TableSwitchInsn(min, max, dflt, labels) =>
              stack.pop(IntT)

            case LookupSwitchInsn(dflt, keys, labels) =>
              stack.pop(IntT)

            case MultiANewArrayInsn(descriptor, numDimensions) =>
              // remove the leading '[' because we want the element type
              val elemType = TypeSignature.parse(descriptor.tail)
              val lengths = stack.popMultiple(Seq.fill(numDimensions)(IntT))
              stack.push(Array(elemType, lengths))

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

  type Output = Seq[(MethodUid, Seq[BytecodeInstr])]

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
          if (!typeCheck(sig, topElem.tpe)) {
            throw AbsIntException(s"expected $sig, found ${topElem.tpe} ($topElem)")
          }
          topElem
      }
    }

    /**
     * Both `types` and the result are oriented as follows: stack bottom ... stack top
     */
    def popMultiple(types: Seq[TypeSignature]): Seq[AbsIntValue] = {
      (for (tpe <- types.reverse) yield pop(tpe)).reverse
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
