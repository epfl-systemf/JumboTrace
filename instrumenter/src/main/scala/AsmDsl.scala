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
   *
   * @param secondStackElemTypeDescr type descriptor of the second element (on the stack, indexed from the beginning)
   *
   *                                 <b>ASSUMES that the topmost element of the stack is a single-word element (i.e. neither a double nor a long)</b>
   */
  def SWAP(secondStackElemTypeDescr: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    if (isDoubleWordType(secondStackElemTypeDescr)) {
      // bottom ... top
      // d2 d1 x  ->  x d2 d1 x  ->  x d2 d1
      mv.visitInsn(Opcodes.DUP_X2)
      mv.visitInsn(Opcodes.POP)
    } else {
      mv.visitInsn(Opcodes.SWAP)
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
