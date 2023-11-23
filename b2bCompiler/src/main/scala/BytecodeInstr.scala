package b2bCompiler

import org.objectweb.asm.{Attribute, Handle, Label, TypePath}
import OpcodesTyping.*

import org.objectweb.asm.Opcodes

import OpcodesHelpers.opcodeName

sealed trait BytecodeInstr {
  def descr: String
}

sealed trait RegularBytecodeInstr extends BytecodeInstr {
  
  def isMetadata: Boolean

  /**
   * should return a negative number if no opcode corresponds to this instruction
   */
  protected def opcode: Int

  final def opcodeOpt: Option[Int] = {
    val op = opcode
    if op < 0 then None else Some(op)
  }
}

sealed trait MetadataBytecodeInstr extends RegularBytecodeInstr {
  final override def isMetadata: Boolean = true
}

sealed trait EffectingBytecodeInstr extends RegularBytecodeInstr {
  final override def isMetadata: Boolean = false
}


final case class FrameB(
                         opcode: Int,
                         numLocal: Int,
                         local: Array[AnyRef],
                         numStack: Int,
                         stack: Array[AnyRef]
                       ) extends MetadataBytecodeInstr {
  override def descr: String = "frame"
}

final case class Insn(opcode: InsnOpcode) extends EffectingBytecodeInstr {
  override def descr: String = opcodeName(opcode)
}

final case class IntInsn(opcode: IntInsnOpcode, operand: Int) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(opcode)} $operand"
}

final case class VarInsn(opcode: VarInsnOpcode, varIndex: Int) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(opcode)} $varIndex"
}

final case class TypeInsn(opcode: TypeInsnOpcode, tpe: String) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(opcode)} $tpe"
}

final case class FieldInsn(opcode: FieldInsnOpcode, owner: String, name: String, descriptor: String) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(opcode)} $owner $name $descriptor"
}

final case class MethodInsn(
                             opcode: MethodInsnOpcode,
                             owner: String,
                             name: String,
                             descriptor: String,
                             isInterface: Boolean
                           ) extends EffectingBytecodeInstr {
  override def descr: String = {
    val isInterfaceDescr = if isInterface then "(itf)" else ""
    s"${opcodeName(opcode)} $owner $name $descriptor $isInterfaceDescr"
  }
}

final case class InvokeDynamicInsn(
                                    name: String,
                                    descriptor: String,
                                    bootstrapMethodHandle: Handle,
                                    bootstrapMethodArguments: Seq[Any]
                                  ) extends EffectingBytecodeInstr {
  override def descr: String = {
    s"${opcodeName(Opcodes.INVOKEDYNAMIC)} $name $descriptor handle:$bootstrapMethodHandle" ++ bootstrapMethodArguments.mkString("(", ",", ")")
  }

  override def opcode: Int = Opcodes.INVOKEDYNAMIC
}

final case class JumpInsn(opcode: JumpInsnOpcode, label: Label) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(opcode)} $label"
}

final case class LabelOccurenceB(label: Label) extends MetadataBytecodeInstr {
  override def descr: String = s"label $label"

  override def opcode: Int = -1
}

final case class LdcInsn(value: Any) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(Opcodes.LDC)} $value"

  override protected def opcode: Int = Opcodes.LDC
}

final case class IincInsn(varIndex: Int, increment: Int) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(Opcodes.IINC)} $varIndex $increment"

  override protected def opcode: Int = Opcodes.IINC
}

final case class TableSwitchInsn(min: Int, max: Int, dflt: Label, labels: Seq[Label]) extends EffectingBytecodeInstr {
  override def descr: String = {
    s"${opcodeName(Opcodes.TABLESWITCH)} $min $max $dflt " ++ labels.mkString("[", ",", "]")
  }

  override protected def opcode: Int = Opcodes.TABLESWITCH
}

final case class LookupSwitchInsn(dflt: Label, keys: Array[Int], labels: Array[Label]) extends EffectingBytecodeInstr {
  override def descr: String = {
    s"${opcodeName(Opcodes.LOOKUPSWITCH)} $dflt " ++ keys.mkString("[", ",", "]") ++ " " ++ labels.mkString("[", ",", "]")
  }

  override protected def opcode: Int = Opcodes.LOOKUPSWITCH
}

final case class MultiANewArrayInsn(descriptor: String, numDimensions: Int) extends EffectingBytecodeInstr {
  override def descr: String = s"${opcodeName(Opcodes.MULTIANEWARRAY)} $descriptor $numDimensions"

  override protected def opcode: Int = Opcodes.MULTIANEWARRAY
}

final case class InsnAnnotationB(
                                  typeRef: Int,
                                  typePath: TypePath,
                                  descriptor: String,
                                  visible: Boolean
                                ) extends MetadataBytecodeInstr {
  override def descr: String = "insn-annotation"

  override protected def opcode: Int = -1
}

final case class TryCatchBlockB(start: Label, end: Label, handler: Label, tpe: String) extends MetadataBytecodeInstr {
  override def descr: String = s"try-catch start=$start end=$end handler=$handler $tpe"

  override protected def opcode: Int = -1
}

final case class TryCatchAnnotationB(
                                      typeRef: Int,
                                      typePath: TypePath,
                                      descriptor: String,
                                      visible: Boolean
                                    ) extends MetadataBytecodeInstr {
  override def descr: String = "try-catch-annot"

  override protected def opcode: Int = -1
}

final case class LocalVarB(
                            name: String,
                            descriptor: String,
                            signature: String,
                            start: Label,
                            end: Label,
                            index: Int
                          ) extends MetadataBytecodeInstr {
  override def descr: String = s"local $name $descriptor $signature [$start,$end] idx=$index"

  override protected def opcode: Int = -1
}

final case class LocalVarAnnotB(
                                 typeRef: Int,
                                 typePath: TypePath,
                                 start: Array[Label],
                                 end: Array[Label],
                                 index: Array[Int],
                                 descriptor: String,
                                 visible: Boolean
                               ) extends MetadataBytecodeInstr {
  override def descr: String = s"local-var-annot"

  override protected def opcode: Int = -1
}

final case class LineNumberB(line: Int, start: Label) extends MetadataBytecodeInstr {
  override def descr: String = s"line $line $start"

  override protected def opcode: Int = -1
}

final case class MaxsB(maxStack: Int, maxLocals: Int) extends MetadataBytecodeInstr {
  override def descr: String = s"maxs maxStack=$maxStack maxLocals=$maxLocals"

  override protected def opcode: Int = -1
}

final case class ParamB(name: String, access: Int) extends MetadataBytecodeInstr {
  override def descr: String = s"param $name access=$access"

  override protected def opcode: Int = -1
}

final case class AnnotationDefaultB() extends MetadataBytecodeInstr {
  override def descr: String = "annotation-default"

  override protected def opcode: Int = -1
}

final case class AnnotationB(descriptor: String, visible: Boolean) extends MetadataBytecodeInstr {
  override def descr: String = "annotation"

  override protected def opcode: Int = -1
}

final case class TypeAnnotB(
                             typeRef: Int,
                             typePath: TypePath,
                             descriptor: String,
                             visible: Boolean
                           ) extends MetadataBytecodeInstr {
  override def descr: String = "type-annotation"

  override protected def opcode: Int = -1
}

final case class AnnotableParameterCountB(parameterCount: Int, visible: Boolean) extends MetadataBytecodeInstr {

  override def descr: String = "annotable-parameter-count"

  override protected def opcode: Int = -1
}

final case class ParameterAnnotationB(parameter: Int, descriptor: String, visible: Boolean) extends MetadataBytecodeInstr {

  override def descr: String = "parameter-annotation"

  override protected def opcode: Int = -1
}

final case class AttributeB(attribute: Attribute) extends MetadataBytecodeInstr {
  override def descr: String = "attribute"

  override protected def opcode: Int = -1
}

final case class CodeB() extends MetadataBytecodeInstr {
  override def descr: String = "code-start"

  override protected def opcode: Int = -1
}

final case class EndB() extends MetadataBytecodeInstr {
  override def descr: String = "code-end"

  override protected def opcode: Int = -1
}

final case class StatementSeparator(startLine: Int) extends BytecodeInstr {
  override def descr: String = s"---- new-stat ($startLine) ----"
}

final case class StackState(stack: List[AbsIntValue]) extends BytecodeInstr {
  override def descr: String = "stack: [" ++ stack.mkString(",")
}
