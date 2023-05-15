package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{Label, MethodVisitor, Opcodes}

object AsmDsl {

  /**
   * Push a constant onto the stack
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
   * Duplicate the element at the top of the stack
   */
  def DUP(using mv: MethodVisitor): Unit = {
    mv.visitInsn(Opcodes.DUP)
  }

  /**
   * Swap the 2 topmost elements of the stack
   */
  def SWAP(using mv: MethodVisitor): Unit = {
    mv.visitInsn(Opcodes.SWAP)
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

  def RETURN(using mv: MethodVisitor): Unit = {
    mv.visitInsn(Opcodes.RETURN)
  }

}
