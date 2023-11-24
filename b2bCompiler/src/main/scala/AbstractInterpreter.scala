package b2bCompiler

import b2bCompiler.AbstractInterpreter.{AbsIntException, AbsIntStack}
import org.objectweb.asm.Label

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

          import org.objectweb.asm.Opcodes.*
          effectingBytecodeInstr match {

            case Insn(NOP) =>
            // do nothing
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
            case Insn(IALOAD) => ???
            case Insn(LALOAD) => ???
            case Insn(FALOAD) => ???
            case Insn(DALOAD) => ???
            case Insn(AALOAD) => ???
            case Insn(BALOAD) => ???
            case Insn(CALOAD) => ???
            case Insn(SALOAD) => ???
            case Insn(IASTORE) => ???
            case Insn(LASTORE) => ???
            case Insn(FASTORE) => ???
            case Insn(DASTORE) => ???
            case Insn(AASTORE) => ???
            case Insn(BASTORE) => ???
            case Insn(CASTORE) => ???
            case Insn(SASTORE) => ???
            case Insn(POP) => ???
            case Insn(POP2) => ???
            case Insn(DUP) => ???
            case Insn(DUP_X1) => ???
            case Insn(DUP_X2) => ???
            case Insn(DUP2) => ???
            case Insn(DUP2_X1) => ???
            case Insn(DUP2_X2) => ???
            case Insn(SWAP) => ???
            case Insn(IADD) => ???
            case Insn(LADD) => ???
            case Insn(FADD) => ???
            case Insn(DADD) => ???
            case Insn(ISUB) => ???
            case Insn(LSUB) => ???
            case Insn(FSUB) => ???
            case Insn(DSUB) => ???
            case Insn(IMUL) => ???
            case Insn(LMUL) => ???
            case Insn(FMUL) => ???
            case Insn(DMUL) => ???
            case Insn(IDIV) => ???
            case Insn(LDIV) => ???
            case Insn(FDIV) => ???
            case Insn(DDIV) => ???
            case Insn(IREM) => ???
            case Insn(LREM) => ???
            case Insn(FREM) => ???
            case Insn(DREM) => ???
            case Insn(INEG) => ???
            case Insn(LNEG) => ???
            case Insn(FNEG) => ???
            case Insn(DNEG) => ???
            case Insn(ISHL) => ???
            case Insn(LSHL) => ???
            case Insn(ISHR) => ???
            case Insn(LSHR) => ???
            case Insn(IUSHR) => ???
            case Insn(LUSHR) => ???
            case Insn(IAND) => ???
            case Insn(LAND) => ???
            case Insn(IOR) => ???
            case Insn(LOR) => ???
            case Insn(IXOR) => ???
            case Insn(LXOR) => ???
            case Insn(I2L) => ???
            case Insn(I2F) => ???
            case Insn(I2D) => ???
            case Insn(L2I) => ???
            case Insn(L2F) => ???
            case Insn(L2D) => ???
            case Insn(F2I) => ???
            case Insn(F2L) => ???
            case Insn(F2D) => ???
            case Insn(D2I) => ???
            case Insn(D2L) => ???
            case Insn(D2F) => ???
            case Insn(I2B) => ???
            case Insn(I2C) => ???
            case Insn(I2S) => ???
            case Insn(LCMP) => ???
            case Insn(FCMPL) => ???
            case Insn(FCMPG) => ???
            case Insn(DCMPL) => ???
            case Insn(DCMPG) => ???
            case Insn(IRETURN) => ???
            case Insn(LRETURN) => ???
            case Insn(FRETURN) => ???
            case Insn(DRETURN) => ???
            case Insn(ARETURN) => ???
            case Insn(RETURN) => ???
            case Insn(ARRAYLENGTH) => ???
            case Insn(ATHROW) => ???
            case Insn(MONITORENTER) => ???
            case Insn(MONITOREXIT) => ???

            case IntInsn(BIPUSH, operand) => ???
            case IntInsn(SIPUSH, operand) => ???
            case IntInsn(NEWARRAY, operand) => ???

            case VarInsn(ILOAD, varIndex) => ???
            case VarInsn(LLOAD, varIndex) => ???
            case VarInsn(FLOAD, varIndex) => ???
            case VarInsn(DLOAD, varIndex) => ???
            case VarInsn(ALOAD, varIndex) => ???
            case VarInsn(ISTORE, varIndex) => ???
            case VarInsn(LSTORE, varIndex) => ???
            case VarInsn(FSTORE, varIndex) => ???
            case VarInsn(DSTORE, varIndex) => ???
            case VarInsn(ASTORE, varIndex) => ???
            case VarInsn(RET, varIndex) => ???

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
          if (topElem.tpe != sig) {
            throw AbsIntException(s"expected $sig, found ${topElem.tpe} ($topElem)")
          }
          topElem
      }
    }
  }

}
