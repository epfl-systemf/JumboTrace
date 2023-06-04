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
   * Duplicate the element at the top of the stack (handles both single- and double-word elements)
   *
   * Stack evolution: [a,... -> [a,a,...
   */
  def DUP(td: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    if (isDoubleWordType(td)) {
      mv.visitInsn(Opcodes.DUP2)
    } else {
      mv.visitInsn(Opcodes.DUP)
    }
  }

  /**
   * Duplicate the 2 topmost elements on the stack (handles both single- and double-word elements)
   *
   * Stack evolution: [a,b,...  ->  [a,b,a,b,...
   */
  def DUP2(firstStackElemType: TypeDescriptor, secondStackElemType: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    if (isDoubleWordType(firstStackElemType) && isDoubleWordType(secondStackElemType)) {
      // Stack: [a1 a2 b1 b2 ...
      SWAP(firstStackElemType, secondStackElemType)
      // Stack: [b1 b2 a1 a2 ...
      mv.visitInsn(Opcodes.DUP2_X2)
      // Stack: [b1 b2 a1 a2 b1 b2 ...
      SWAP(secondStackElemType, firstStackElemType)
      // Stack: [a1 a2 b1 b2 b1 b2 ...
      mv.visitInsn(Opcodes.DUP2_X2)
      // Stack: [a1 a2 b1 b2 a1 a2 b1 b2 ...
    } else if (isDoubleWordType(firstStackElemType)) {
      // Stack: [d1 d2 x ...
      SWAP(firstStackElemType, secondStackElemType)
      // Stack: [x d1 d2 ...
      mv.visitInsn(Opcodes.DUP_X2)
      // Stack: [x d1 d2 x ...
      SWAP(secondStackElemType, firstStackElemType)
      // Stack: [d1 d2 x x ...
      mv.visitInsn(Opcodes.DUP2_X1)
      // Stack: [d1 d2 x d1 d2 x ...
    } else if (isDoubleWordType(secondStackElemType)) {
      // Stack: [x d1 d2 ...
      mv.visitInsn(Opcodes.DUP_X2)
      // Stack: [x d1 d2 x ...
      SWAP(firstStackElemType, secondStackElemType)
      // Stack: [d1 d2 x x ...
      mv.visitInsn(Opcodes.DUP2_X1)
      // Stack: [d1 d2 x d1 d2 x ...
    } else {
      mv.visitInsn(Opcodes.DUP2)
    }
  }

  /**
   * Swap the 2 topmost elements of the stack (handles both single- and double-word elements)
   *
   * Stack evolution: [a,b,... -> [b,a,...
   */
  def SWAP(firstStackElemType: TypeDescriptor, secondStackElemType: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    if (isDoubleWordType(firstStackElemType) && isDoubleWordType(secondStackElemType)){
      // Stack: [a1 a2 b1 b2 ...
      mv.visitInsn(Opcodes.DUP2_X2)
      // Stack: [a1 a2 b1 b2 a1 a2 ...
      mv.visitInsn(Opcodes.POP2)
      // Stack: [b1 b2 a1 a2 ...
    } else if (isDoubleWordType(firstStackElemType)){
      // Stack: [d1 d2 x ...
      mv.visitInsn(Opcodes.DUP2_X1)
      // Stack: [d1 d2 x d1 d2 ...
      mv.visitInsn(Opcodes.POP2)
      // Stack: [x d1 d2 ...
    } else if (isDoubleWordType(secondStackElemType)){
      // Stack: [x d1 d2 ...
      mv.visitInsn(Opcodes.DUP_X2)
      // Stack: [x d1 d2 x ...
      mv.visitInsn(Opcodes.POP)
      // Stack: [d1 d2 x ...
    } else {
      mv.visitInsn(Opcodes.SWAP)
    }
  }

  /**
   * @param start beginning of the code throwing the exception
   * @param end end of the code throwing the exception
   * @param handler beginning of the exception handler
   * @param typeInternalName internal name of the type of the exception
   */
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
  
  def GETFIELD(owner: String, fieldName: String, td: TypeDescriptor)(using mv: MethodVisitor): Unit = {
    mv.visitFieldInsn(Opcodes.GETFIELD, owner, fieldName, td.toString)
  }

}
