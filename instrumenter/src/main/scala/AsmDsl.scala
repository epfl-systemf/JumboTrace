package instrumenter

import org.objectweb.asm.{Label, MethodVisitor, Opcodes}
import TypeDescriptor.isDoubleWordType

object AsmDsl {

  /**
   * Push a constant to the stack
   */
  def LDC(constant: (Integer | Byte | Character | Short | Boolean | Float | Long | Double | String))(using mv: MethodVisitor): Unit = {
    // the list of accepted types is a little more restrictive than needed (see SymbolTable::addConstant)
    mv.visitLdcInsn(constant)
  }

  /**
   * Invocation of a class method
   */
  def INVOKE_STATIC(ownerClass: ClassName, methodName: MethodName, methodDescriptor: MethodDescriptor)(using mv: MethodVisitor): Unit = {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ownerClass.name, methodName.name, methodDescriptor.toString, false)
  }

  /**
   * Invocation of an instance method
   */
  def INVOKE_VIRTUAL(ownerClass: ClassName, methodName: MethodName, methodDescriptor: MethodDescriptor)(using mv: MethodVisitor): Unit = {
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ownerClass.name, methodName.name, methodDescriptor.toString, false)
  }

  /**
   * Duplicate the element at the top of the stack (or the topmost 2 elements if `td` is a double word element)
   */
  def DUP(td: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    if (isDoubleWordType(td)) {
      mv.visitInsn(Opcodes.DUP2)
    } else {
      mv.visitInsn(Opcodes.DUP)
    }
  }

  /**
   * Swap the 2 topmost elements of the stack
   */
  def SWAP(firstStackElemType: TypeDescriptor, secondStackElemType: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    // stack representation in comments: bottom ... top
    if (isDoubleWordType(firstStackElemType) && isDoubleWordType(secondStackElemType)){
      // a1 a2 b1 b2 -> b1 b2 a1 a2 b1 b2 -> b1 b2 a1 a2
      mv.visitInsn(Opcodes.DUP2_X2)
      mv.visitInsn(Opcodes.POP2)
    } else if (isDoubleWordType(firstStackElemType)){
      // x d1 d2 -> d1 d2 x d1 d2 -> d1 d2 x
      mv.visitInsn(Opcodes.DUP2_X1)
      mv.visitInsn(Opcodes.POP2)
    } else if (isDoubleWordType(secondStackElemType)){
      // d2 d1 x -> x d2 d1 x -> x d2 d1
      mv.visitInsn(Opcodes.DUP_X2)
      mv.visitInsn(Opcodes.POP)
    } else {
      mv.visitInsn(Opcodes.SWAP)
    }
  }

  def DUP2(firstStackElemType: TypeDescriptor, secondStackElemType: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    // stack representation in comments: bottom ... top
    if (isDoubleWordType(firstStackElemType) && isDoubleWordType(secondStackElemType)) {
      // a1 a2 b1 b2 -> b1 b2 a1 a2 -> a1 a2 b1 b2 a1 a2 -> a1 a2 a1 a2 b1 b2 -> a1 a2 b1 b2 a1 a2 b1 b2
      SWAP(firstStackElemType, secondStackElemType)
      mv.visitInsn(Opcodes.DUP2_X2)
      SWAP(secondStackElemType, firstStackElemType)
      mv.visitInsn(Opcodes.DUP2_X2)
    } else if (isDoubleWordType(firstStackElemType)) {
      // x d1 d2 -> d1 d2 x -> x d1 d2 x -> x x d1 d2 -> x d1 d2 x d1 d2
      SWAP(firstStackElemType, secondStackElemType)
      mv.visitInsn(Opcodes.DUP_X2)
      SWAP(secondStackElemType, firstStackElemType)
      mv.visitInsn(Opcodes.DUP2_X1)
    } else if (isDoubleWordType(secondStackElemType)) {
      // d1 d2 x -> x d1 d2 x -> x x d1 d2 -> x d1 d2 x d1 d2
      mv.visitInsn(Opcodes.DUP_X2)
      SWAP(firstStackElemType, secondStackElemType)
      mv.visitInsn(Opcodes.DUP2_X1)
    } else {
      mv.visitInsn(Opcodes.DUP2)
    }
  }

  def TRY_CATCH(start: Label, end: Label, handler: Label, typeInternalName: String)(using mv: MethodVisitor): Unit = {
    mv.visitTryCatchBlock(start, end, handler, typeInternalName)
  }

  def LABEL(label: Label)(using mv: MethodVisitor): Unit = {
    mv.visitLabel(label)
  }

  def GOTO(label: Label)(using mv: MethodVisitor): Unit = {
    mv.visitJumpInsn(Opcodes.GOTO, label)
  }

  def RETURN(td: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    val opcode = td.getOpcode(Opcodes.IRETURN, Opcodes.ARETURN)
    mv.visitInsn(opcode)
  }

  def LOAD(td: TypeDescriptor, varIdx: Int)(using mv: MethodVisitor): Unit = {
    val opcode = td.getOpcode(Opcodes.ILOAD, Opcodes.ALOAD)
    mv.visitVarInsn(opcode, varIdx)
  }
  
  def ATHROW(using mv: MethodVisitor): Unit = {
    mv.visitInsn(Opcodes.ATHROW)
  }

  def PRINTLN(str: String)(using mv: MethodVisitor): Unit = {
    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
    LDC(str)
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
  }

}
