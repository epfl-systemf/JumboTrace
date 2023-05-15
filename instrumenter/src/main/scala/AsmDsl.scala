package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{MethodVisitor, Opcodes}

object AsmDsl {

  /**
   * Push a constant onto the stack
   */
  def LDC(constant: Any)(using mv: MethodVisitor): Unit = {
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

}
