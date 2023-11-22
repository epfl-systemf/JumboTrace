package b2bCompiler

import org.objectweb.asm.Opcodes.*
import TypeSignature.*

object OpcodesHelpers {

  type StackUpdate = (Seq[TypeSignature], Seq[TypeSignature])
  
  def opcodeName(opcode: Int): String = {
    opcode match {
      case NOP => "nop"
      case ACONST_NULL => "aconst_null"
      case ICONST_M1 => "iconst_m1"
      case ICONST_0 => "iconst_0"
      case ICONST_1 => "iconst_1"
      case ICONST_2 => "iconst_2"
      case ICONST_3 => "iconst_3"
      case ICONST_4 => "iconst_4"
      case ICONST_5 => "iconst_5"
      case LCONST_0 => "lconst_0"
      case LCONST_1 => "lconst_1"
      case FCONST_0 => "fconst_0"
      case FCONST_1 => "fconst_1"
      case FCONST_2 => "fconst_2"
      case DCONST_0 => "dconst_0"
      case DCONST_1 => "dconst_1"
      case BIPUSH => "bipush"
      case SIPUSH => "sipush"
      case LDC => "ldc"
      case ILOAD => "iload"
      case LLOAD => "lload"
      case FLOAD => "fload"
      case DLOAD => "dload"
      case ALOAD => "aload"
      case IALOAD => "iaload"
      case LALOAD => "laload"
      case FALOAD => "faload"
      case DALOAD => "daload"
      case AALOAD => "aaload"
      case BALOAD => "baload"
      case CALOAD => "caload"
      case SALOAD => "saload"
      case ISTORE => "istore"
      case LSTORE => "lstore"
      case FSTORE => "fstore"
      case DSTORE => "dstore"
      case ASTORE => "astore"
      case IASTORE => "iastore"
      case LASTORE => "lastore"
      case FASTORE => "fastore"
      case DASTORE => "dastore"
      case AASTORE => "aastore"
      case BASTORE => "bastore"
      case CASTORE => "castore"
      case SASTORE => "sastore"
      case POP => "pop"
      case POP2 => "pop2"
      case DUP => "dup"
      case DUP_X1 => "dup_x1"
      case DUP_X2 => "dup_x2"
      case DUP2 => "dup2"
      case DUP2_X1 => "dup2_x1"
      case DUP2_X2 => "dup2_x2"
      case SWAP => "swap"
      case IADD => "iadd"
      case LADD => "ladd"
      case FADD => "fadd"
      case DADD => "dadd"
      case ISUB => "isub"
      case LSUB => "lsub"
      case FSUB => "fsub"
      case DSUB => "dsub"
      case IMUL => "imul"
      case LMUL => "lmul"
      case FMUL => "fmul"
      case DMUL => "dmul"
      case IDIV => "idiv"
      case LDIV => "ldiv"
      case FDIV => "fdiv"
      case DDIV => "ddiv"
      case IREM => "irem"
      case LREM => "lrem"
      case FREM => "frem"
      case DREM => "drem"
      case INEG => "ineg"
      case LNEG => "lneg"
      case FNEG => "fneg"
      case DNEG => "dneg"
      case ISHL => "ishl"
      case LSHL => "lshl"
      case ISHR => "ishr"
      case LSHR => "lshr"
      case IUSHR => "iushr"
      case LUSHR => "lushr"
      case IAND => "iand"
      case LAND => "land"
      case IOR => "ior"
      case LOR => "lor"
      case IXOR => "ixor"
      case LXOR => "lxor"
      case IINC => "iinc"
      case I2L => "i2l"
      case I2F => "i2f"
      case I2D => "i2d"
      case L2I => "l2i"
      case L2F => "l2f"
      case L2D => "l2d"
      case F2I => "f2i"
      case F2L => "f2l"
      case F2D => "f2d"
      case D2I => "d2i"
      case D2L => "d2l"
      case D2F => "d2f"
      case I2B => "i2b"
      case I2C => "i2c"
      case I2S => "i2s"
      case LCMP => "lcmp"
      case FCMPL => "fcmpl"
      case FCMPG => "fcmpg"
      case DCMPL => "dcmpl"
      case DCMPG => "dcmpg"
      case IFEQ => "ifeq"
      case IFNE => "ifne"
      case IFLT => "iflt"
      case IFGE => "ifge"
      case IFGT => "ifgt"
      case IFLE => "ifle"
      case IF_ICMPEQ => "if_icmpeq"
      case IF_ICMPNE => "if_icmpne"
      case IF_ICMPLT => "if_icmplt"
      case IF_ICMPGE => "if_icmpge"
      case IF_ICMPGT => "if_icmpgt"
      case IF_ICMPLE => "if_icmple"
      case IF_ACMPEQ => "if_acmpeq"
      case IF_ACMPNE => "if_acmpne"
      case GOTO => "goto"
      case JSR => "jsr"
      case RET => "ret"
      case TABLESWITCH => "tableswitch"
      case LOOKUPSWITCH => "lookupswitch"
      case IRETURN => "ireturn"
      case LRETURN => "lreturn"
      case FRETURN => "freturn"
      case DRETURN => "dreturn"
      case ARETURN => "areturn"
      case RETURN => "return"
      case GETSTATIC => "getstatic"
      case PUTSTATIC => "putstatic"
      case GETFIELD => "getfield"
      case PUTFIELD => "putfield"
      case INVOKEVIRTUAL => "invokevirtual"
      case INVOKESPECIAL => "invokespecial"
      case INVOKESTATIC => "invokestatic"
      case INVOKEINTERFACE => "invokeinterface"
      case INVOKEDYNAMIC => "invokedynamic"
      case NEW => "new"
      case NEWARRAY => "newarray"
      case ANEWARRAY => "anewarray"
      case ARRAYLENGTH => "arraylength"
      case ATHROW => "athrow"
      case CHECKCAST => "checkcast"
      case INSTANCEOF => "instanceof"
      case MONITORENTER => "monitorenter"
      case MONITOREXIT => "monitorexit"
      case MULTIANEWARRAY => "multianewarray"
      case IFNULL => "ifnull"
      case IFNONNULL => "ifnonnull"
    }
  }

  /**
   * @return (popped, pushed)
   */
  def stackUpdate(opcode: Int): StackUpdate = {
    opcode match {
      case NOP => noChange
      case ACONST_NULL => produce(ObjectRefT)
      case ICONST_M1 => produce(IntT)
      case ICONST_0 => produce(IntT)
      case ICONST_1 => produce(IntT)
      case ICONST_2 => produce(IntT)
      case ICONST_3 => produce(IntT)
      case ICONST_4 => produce(IntT)
      case ICONST_5 => produce(IntT)
      case LCONST_0 => produce(LongT)
      case LCONST_1 => produce(LongT)
      case FCONST_0 => produce(FloatT)
      case FCONST_1 => produce(FloatT)
      case FCONST_2 => produce(FloatT)
      case DCONST_0 => produce(DoubleT)
      case DCONST_1 => produce(DoubleT)
      case BIPUSH => produce(ByteT)
      case SIPUSH => produce(ShortT)
      // LDC excluded
      case ILOAD => produce(IntT)
      case LLOAD => produce(LongT)
      case FLOAD => produce(FloatT)
      case DLOAD => produce(DoubleT)
      case ALOAD => produce(ObjectRefT)
      case IALOAD => (Seq(ObjectRefT, IntT), Seq(IntT))
      case LALOAD => (Seq(ObjectRefT, IntT), Seq(LongT))
      case FALOAD => (Seq(ObjectRefT, IntT), Seq(FloatT))
      case DALOAD => (Seq(ObjectRefT, IntT), Seq(DoubleT))
      case AALOAD => (Seq(ObjectRefT, IntT), Seq(ObjectRefT))
      case BALOAD => (Seq(ObjectRefT, IntT), Seq(ByteT))  // might actually be a boolean
      case CALOAD => (Seq(ObjectRefT, IntT), Seq(CharT))
      case SALOAD => (Seq(ObjectRefT, IntT), Seq(ShortT))
      case ISTORE => consume(IntT)
      case LSTORE => consume(LongT)
      case FSTORE => consume(FloatT)
      case DSTORE => consume(DoubleT)
      case ASTORE => consume(ObjectRefT)
      case IASTORE => consume(ObjectRefT, IntT, IntT)
      case LASTORE => consume(ObjectRefT, IntT, LongT)
      case FASTORE => consume(ObjectRefT, IntT, FloatT)
      case DASTORE => consume(ObjectRefT, IntT, DoubleT)
      case AASTORE => consume(ObjectRefT, IntT, ObjectRefT)
      case BASTORE => consume(ObjectRefT, IntT, ByteT) // byte or boolean
      case CASTORE => consume(ObjectRefT, IntT, CharT)
      case SASTORE => consume(ObjectRefT, IntT, ShortT)
      case POP => consume(UnknownT)
      case POP2 => consume(UnknownT, UnknownT)
      case DUP =>
        val v = UnknownT
        Seq(v) -> Seq(v, v)
      case DUP_X1 =>
        val (v1, v2) = (UnknownT, UnknownT)
        Seq(v2, v1) -> Seq(v1, v2, v1)
      case DUP_X2 =>
        val (v1, v2, v3) = (UnknownT, UnknownT, UnknownT)
        Seq(v3, v2, v1) -> Seq(v1, v3, v2, v1)
      case DUP2 =>
        val (v1, v2) = (UnknownT, UnknownT)
        Seq(v2, v1) -> Seq(v2, v1, v2, v1)
      case DUP2_X1 =>
        val (v1, v2, v3) = (UnknownT, UnknownT, UnknownT)
        Seq(v3, v2, v1) -> Seq(v2, v1, v3, v2, v1)
      case DUP2_X2 =>
        val (v1, v2, v3, v4) = (UnknownT, UnknownT, UnknownT, UnknownT)
        Seq(v4, v3, v2, v1) -> Seq(v2, v1, v4, v3, v2, v1)
      case SWAP =>
        val (v1, v2) = (UnknownT, UnknownT)
        Seq(v2, v1) -> Seq(v1, v2)
      case IADD => update(2, 1, IntT)
      case LADD => update(2, 1, LongT)
      case FADD => update(2, 1, FloatT)
      case DADD => update(2, 1, DoubleT)
      case ISUB => update(2, 1, IntT)
      case LSUB => update(2, 1, LongT)
      case FSUB => update(2, 1, FloatT)
      case DSUB => update(2, 1, DoubleT)
      case IMUL => update(2, 1, IntT)
      case LMUL => update(2, 1, LongT)
      case FMUL => update(2, 1, FloatT)
      case DMUL => update(2, 1, DoubleT)
      case IDIV => update(2, 1, IntT)
      case LDIV => update(2, 1, LongT)
      case FDIV => update(2, 1, FloatT)
      case DDIV => update(2, 1, DoubleT)
      case IREM => update(2, 1, IntT)
      case LREM => update(2, 1, LongT)
      case FREM => update(2, 1, FloatT)
      case DREM => update(2, 1, DoubleT)
      case INEG => update(1, 1, IntT)
      case LNEG => update(1, 1, LongT)
      case FNEG => update(1, 1, FloatT)
      case DNEG => update(1, 1, DoubleT)
      case ISHL => Seq(IntT, IntT) -> Seq(IntT)
      case LSHL => Seq(LongT, IntT) -> Seq(LongT)
      case ISHR => Seq(IntT, IntT) -> Seq(IntT)
      case LSHR => Seq(LongT, IntT) -> Seq(LongT)
      case IUSHR => Seq(IntT, IntT) -> Seq(IntT)
      case LUSHR => Seq(LongT, IntT) -> Seq(LongT)
      case IAND => update(2, 1, IntT)
      case LAND => update(2, 1, LongT)
      case IOR => update(2, 1, IntT)
      case LOR => update(2, 1, LongT)
      case IXOR => update(2, 1, IntT)
      case LXOR => update(2, 1, LongT)
      case IINC => noChange
      case I2L => IntT converted LongT
      case I2F => IntT converted FloatT
      case I2D => IntT converted DoubleT
      case L2I => LongT converted IntT
      case L2F => LongT converted FloatT
      case L2D => LongT converted DoubleT
      case F2I => FloatT converted IntT
      case F2L => FloatT converted LongT
      case F2D => FloatT converted DoubleT
      case D2I => DoubleT converted IntT
      case D2L => DoubleT converted LongT
      case D2F => DoubleT converted FloatT
      case I2B => IntT converted ByteT
      case I2C => IntT converted CharT
      case I2S => IntT converted ShortT
      case LCMP => Seq(LongT, LongT) -> Seq(IntT)
      case FCMPL => Seq(FloatT, FloatT) -> Seq(IntT)
      case FCMPG => Seq(FloatT, FloatT) -> Seq(IntT)
      case DCMPL => Seq(DoubleT, DoubleT) -> Seq(IntT)
      case DCMPG => Seq(DoubleT, DoubleT) -> Seq(IntT)
      case IFEQ => consume(IntT)
      case IFNE => consume(IntT)
      case IFLT => consume(IntT)
      case IFGE => consume(IntT)
      case IFGT => consume(IntT)
      case IFLE => consume(IntT)
      case IF_ICMPEQ => Seq(IntT, IntT) -> Seq(BooleanT)
      case IF_ICMPNE => Seq(IntT, IntT) -> Seq(BooleanT)
      case IF_ICMPLT => Seq(IntT, IntT) -> Seq(BooleanT)
      case IF_ICMPGE => Seq(IntT, IntT) -> Seq(BooleanT)
      case IF_ICMPGT => Seq(IntT, IntT) -> Seq(BooleanT)
      case IF_ICMPLE => Seq(IntT, IntT) -> Seq(BooleanT)
      case IF_ACMPEQ => Seq(ObjectRefT, ObjectRefT) -> Seq(BooleanT)
      case IF_ACMPNE => Seq(ObjectRefT, ObjectRefT) -> Seq(BooleanT)
      case GOTO => noChange
      case JSR => produce(UnknownT)
      case RET => noChange
      case TABLESWITCH => consume(IntT)
      case LOOKUPSWITCH => consume(IntT)
      case IRETURN => consume(IntT)
      case LRETURN => consume(LongT)
      case FRETURN => consume(FloatT)
      case DRETURN => consume(DoubleT)
      case ARETURN => consume(ObjectRefT)
      case RETURN => noChange
      // opcodes for fields and methods are excluded
      case NEW => produce(ObjectRefT)
      case NEWARRAY => Seq(IntT) -> Seq(ObjectRefT)
      case ANEWARRAY => Seq(IntT) -> Seq(ObjectRefT)
      case ARRAYLENGTH => Seq(ObjectRefT) -> Seq(IntT)
      case ATHROW => consume(ObjectRefT)   // TODO check this one (clears the stack)
      case CHECKCAST => noChange  // no change as long as we do not consider types more precise than Object
      case INSTANCEOF => Seq(ObjectRefT) -> Seq(BooleanT)
      case MONITORENTER => consume(ObjectRefT)
      case MONITOREXIT => consume(ObjectRefT)
//    MULTIANEWARRAY excluded
      case IFNULL => consume(ObjectRefT)
      case IFNONNULL => consume(ObjectRefT)
    }
  }

  def stackUpdateLdc(constant: Any): StackUpdate = {
    produce(TypeSignature.of(constant))
  }

  def stackUpdateMultianewarray(nDim: Int): StackUpdate = {
    produce(Seq.fill(nDim)(IntT):_*)
  }

  def stackUpdateInvocation(sig: MethodSignature): StackUpdate = {
    (sig.params, sig.retType.toSeq)
  }

  def stackUpdateField(opcode: Int, fieldTypeSig: TypeSignature): StackUpdate = {
    opcode match {
      case GETSTATIC | GETFIELD =>
        produce(fieldTypeSig)
      case PUTSTATIC | PUTFIELD =>
        consume(fieldTypeSig)
    }
  }

  def isFieldInstruction(opcode: Int): Boolean = {
    GETSTATIC <= opcode && opcode <= PUTFIELD
  }

  def isInvocationInstruction(opcode: Int): Boolean = {
    INVOKEVIRTUAL <= opcode && opcode <= INVOKEDYNAMIC
  }

  private val noChange: StackUpdate = Seq.empty[TypeSignature] -> Seq.empty[TypeSignature]
  private def produce(ts: TypeSignature*): StackUpdate = (Seq.empty, ts.toSeq)
  private def consume(ts: TypeSignature*): StackUpdate = (ts.toSeq, Seq.empty)
  private def update(consumed: Int, produced: Int, tpe: => TypeSignature): StackUpdate = {
    (Seq.fill(consumed)(tpe), Seq.fill(produced)(tpe))
  }
  extension(src: TypeSignature) private def converted(dst: TypeSignature): StackUpdate = (Seq(src), Seq(dst))
  
}
