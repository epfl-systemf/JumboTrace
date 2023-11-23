package b2bCompiler

import b2bCompiler.OpcodesTyping.{InsnOpcode, IntInsnOpcode}
import org.objectweb.asm.{AnnotationVisitor, Attribute, Handle, Label, MethodVisitor, Opcodes, TypePath}

import scala.collection.mutable.ListBuffer

final class MethodParsingVisitor(bytecodeList: ListBuffer[RegularBytecodeInstr]) extends MethodVisitor(Config.asmVersion) {
  
  override def visitFrame(tpe: Int, numLocal: Int, local: Array[AnyRef], numStack: Int, stack: Array[AnyRef]): Unit = {
    bytecodeList.addOne(FrameB(tpe, numLocal, local, numStack, stack))
    super.visitFrame(tpe, numLocal, local, numStack, stack)
  }

  override def visitInsn(opcode: Int): Unit = {
    bytecodeList.addOne(Insn(opcode.CAST))
    super.visitInsn(opcode)
  }

  override def visitIntInsn(opcode: Int, operand: Int): Unit = {
    bytecodeList.addOne(IntInsn(opcode.CAST, operand))
    super.visitIntInsn(opcode, operand)
  }

  override def visitVarInsn(opcode: Int, varIndex: Int): Unit = {
    bytecodeList.addOne(VarInsn(opcode.CAST, varIndex))
    super.visitVarInsn(opcode, varIndex)
  }

  override def visitTypeInsn(opcode: Int, tpe: String): Unit = {
    bytecodeList.addOne(TypeInsn(opcode.CAST, tpe))
    super.visitTypeInsn(opcode, tpe)
  }

  override def visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String): Unit = {
    bytecodeList.addOne(FieldInsn(opcode.CAST, owner, name, descriptor))
    super.visitFieldInsn(opcode, owner, name, descriptor)
  }

  override def visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean): Unit = {
    bytecodeList.addOne(MethodInsn(opcode.CAST, owner, name, descriptor, isInterface))
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
  }

  // here just for safety, can very probably be removed without problems
  override def visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String): Unit = {
    val isInterface = opcode == Opcodes.INVOKEINTERFACE
    visitMethodInsn(opcode, owner, name, descriptor, isInterface)
  }

  override def visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, bootstrapMethodArguments: Any*): Unit = {
    bytecodeList.addOne(InvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments))
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments: _*)
  }

  override def visitJumpInsn(opcode: Int, label: Label): Unit = {
    bytecodeList.addOne(JumpInsn(opcode.CAST, label))
    super.visitJumpInsn(opcode, label)
  }

  override def visitLabel(label: Label): Unit = {
    bytecodeList.addOne(LabelOccurenceB(label))
    super.visitLabel(label)
  }

  override def visitLdcInsn(value: Any): Unit = {
    bytecodeList.addOne(LdcInsn(value))
    super.visitLdcInsn(value)
  }

  override def visitIincInsn(varIndex: Int, increment: Int): Unit = {
    bytecodeList.addOne(IincInsn(varIndex, increment))
    super.visitIincInsn(varIndex, increment)
  }

  override def visitTableSwitchInsn(min: Int, max: Int, dflt: Label, labels: Label*): Unit = {
    bytecodeList.addOne(TableSwitchInsn(min, max, dflt, labels))
    super.visitTableSwitchInsn(min, max, dflt, labels: _*)
  }

  override def visitLookupSwitchInsn(dflt: Label, keys: Array[Int], labels: Array[Label]): Unit = {
    bytecodeList.addOne(LookupSwitchInsn(dflt, keys, labels))
    super.visitLookupSwitchInsn(dflt, keys, labels)
  }

  override def visitMultiANewArrayInsn(descriptor: String, numDimensions: Int): Unit = {
    bytecodeList.addOne(MultiANewArrayInsn(descriptor, numDimensions))
    super.visitMultiANewArrayInsn(descriptor, numDimensions)
  }

  override def visitInsnAnnotation(typeRef: Int, typePath: TypePath, descriptor: String, visible: Boolean): AnnotationVisitor = {
    bytecodeList.addOne(InsnAnnotationB(typeRef, typePath, descriptor, visible))
    super.visitInsnAnnotation(typeRef, typePath, descriptor, visible)
  }

  override def visitTryCatchBlock(start: Label, end: Label, handler: Label, tpe: String): Unit = {
    bytecodeList.addOne(TryCatchBlockB(start, end, handler, tpe))
    super.visitTryCatchBlock(start, end, handler, tpe)
  }

  override def visitTryCatchAnnotation(typeRef: Int, typePath: TypePath, descriptor: String, visible: Boolean): AnnotationVisitor = {
    bytecodeList.addOne(TryCatchAnnotationB(typeRef, typePath, descriptor, visible))
    super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible)
  }

  override def visitLocalVariable(name: String, descriptor: String, signature: String, start: Label, end: Label, index: Int): Unit = {
    bytecodeList.addOne(LocalVarB(name, descriptor, signature, start, end, index))
    super.visitLocalVariable(name, descriptor, signature, start, end, index)
  }

  override def visitLocalVariableAnnotation(
                                             typeRef: Int,
                                             typePath: TypePath,
                                             start: Array[Label],
                                             end: Array[Label],
                                             index: Array[Int],
                                             descriptor: String,
                                             visible: Boolean
                                           ): AnnotationVisitor = {
    bytecodeList.addOne(LocalVarAnnotB(typeRef, typePath, start, end, index, descriptor, visible))
    super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible)
  }

  override def visitLineNumber(line: Int, start: Label): Unit = {
    bytecodeList.addOne(LineNumberB(line, start))
    super.visitLineNumber(line, start)
  }

  override def visitMaxs(maxStack: Int, maxLocals: Int): Unit = {
    bytecodeList.addOne(MaxsB(maxStack, maxLocals))
    super.visitMaxs(maxStack, maxLocals)
  }

  override def visitParameter(name: String, access: Int): Unit = {
    bytecodeList.addOne(ParamB(name, access))
    super.visitParameter(name, access)
  }

  override def visitAnnotationDefault(): AnnotationVisitor = {
    bytecodeList.addOne(AnnotationDefaultB())
    super.visitAnnotationDefault()
  }

  override def visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor = {
    bytecodeList.addOne(AnnotationB(descriptor, visible))
    super.visitAnnotation(descriptor, visible)
  }

  override def visitTypeAnnotation(typeRef: Int, typePath: TypePath, descriptor: String, visible: Boolean): AnnotationVisitor = {
    bytecodeList.addOne(TypeAnnotB(typeRef, typePath, descriptor, visible))
    super.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
  }

  override def visitAnnotableParameterCount(parameterCount: Int, visible: Boolean): Unit = {
    bytecodeList.addOne(AnnotableParameterCountB(parameterCount, visible))
    super.visitAnnotableParameterCount(parameterCount, visible)
  }

  override def visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor = {
    bytecodeList.addOne(ParameterAnnotationB(parameter, descriptor, visible))
    super.visitParameterAnnotation(parameter, descriptor, visible)
  }

  override def visitAttribute(attribute: Attribute): Unit = {
    bytecodeList.addOne(AttributeB(attribute))
    super.visitAttribute(attribute)
  }

  override def visitCode(): Unit = {
    bytecodeList.addOne(CodeB())
    super.visitCode()
  }

  override def visitEnd(): Unit = {
    bytecodeList.addOne(EndB())
    super.visitEnd()
  }
  
  extension(opcode: Int) private def CAST[T <: Int]: T = opcode.asInstanceOf[T]
}
