package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{Label, MethodVisitor, Opcodes}
import AsmDsl.*
import Injection.*
import Injection.EventMethod.*

import com.epfl.systemf.jumbotrace.instrumenter.MethodDescriptor.==>
import com.epfl.systemf.jumbotrace.instrumenter.MethodTable.LocalVariable
import com.epfl.systemf.jumbotrace.instrumenter.TypeDescriptor as TD

final class MethodTransformer(
                               underlying: MethodVisitor,
                               methodTable: MethodTable
                             ) extends MethodVisitor(Config.current.asmVersion, underlying) {

  private val ansiYellow = "\u001B[33m"
  private val ansiRed = "\u001B[31m"
  private val ansiReset = "\u001B[0m"

  import methodTable.{ownerClass, methodName, isMainMethod, methodDescr}
  private given MethodVisitor = underlying

  private lazy val tryCatchLabels = (new Label(), new Label())

  override def visitCode(): Unit = {
    // TODO save arguments
    if (isMainMethod){
      TRY_CATCH(tryCatchLabels._1, tryCatchLabels._2, tryCatchLabels._2, "java/lang/Throwable")
      LABEL(tryCatchLabels._1)
    }
    super.visitCode()
  }

  override def visitInsn(opcode: Int): Unit = {
    val isRetInstr = isReturnInstr(opcode)
    if (isRetInstr){
      if (methodDescr.ret == TD.Void){
        LDC(methodName.name)
        INVOKE_STATIC(jumboTracer, ReturnedVoid.methodName, Seq(TD.String) ==> TD.Void)
      } else {
        val returnedTypeDescr = unpreciseTypingReturnTypeDescriptorFor(methodDescr.ret)
        DUP(returnedTypeDescr)
        LDC(methodName.name)
        SWAP(returnedTypeDescr)
        INVOKE_STATIC(jumboTracer, Returned.methodName, Seq(TD.String, returnedTypeDescr) ==> TD.Void)
      }
    }
    if (isMainMethod && isRetInstr) {
      PRINTLN(ansiYellow + "JumboTracer: program terminating normally" + ansiReset)
      INVOKE_STATIC(jumboTracer, display, Seq.empty ==> TD.Void) // TODO remove (just for debugging)
      INVOKE_STATIC(jumboTracer, writeJsonTrace, Seq.empty ==> TD.Void)
    }
    super.visitInsn(opcode)
  }

  override def visitVarInsn(opcode: Int, varIndex: Int): Unit = {
    methodTable.localVars.get(varIndex).foreach { localVar =>
      val typeDescr = unpreciseTypingReturnTypeDescriptorFor(localVar.descriptor)
      if (isVarStoreInstr(opcode)) {
        DUP(typeDescr)
        LDC(localVar.name)
        SWAP(typeDescr)
        INVOKE_STATIC(jumboTracer, VariableSet.methodName, Seq(TD.String, typeDescr) ==> TD.Void)
      } else if (isArrayStoreInstr(opcode)) {
        // TODO
        ???
      }
    }
    super.visitVarInsn(opcode, varIndex)
  }

  override def visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String): Unit = {
    if (opcode == Opcodes.PUTSTATIC){
      val unpreciseTypeDescr = unpreciseTypingReturnTypeDescriptorFor(TypeDescriptor.parse(descriptor).get)
      DUP(unpreciseTypeDescr)
      LDC(owner)
      SWAP(unpreciseTypeDescr)
      LDC(name)
      SWAP(unpreciseTypeDescr)
      INVOKE_STATIC(jumboTracer, StaticFieldSet.methodName, Seq(TD.String, TD.String, unpreciseTypeDescr) ==> TD.Void)
    }
    super.visitFieldInsn(opcode, owner, name, descriptor)
  }

  override def visitMaxs(maxStack: Int, maxLocals: Int): Unit = {
    if (isMainMethod){
      LABEL(tryCatchLabels._2)
      PRINTLN(s"$ansiYellow JumboTracer: $ansiRed[ERROR]$ansiYellow: program terminating with an exception$ansiReset")
      INVOKE_STATIC(jumboTracer, display, Seq.empty ==> TD.Void) // TODO remove (just for debugging)
      INVOKE_STATIC(jumboTracer, writeJsonTrace, Seq.empty ==> TD.Void)
      ATHROW
    }
    super.visitMaxs(maxStack, maxLocals)
  }

  override def visitLineNumber(line: Int, start: Label): Unit = {
    LDC(ownerClass.name)
    LDC(line)
    INVOKE_STATIC(jumboTracer, LineVisited.methodName, Seq(TD.String, TD.Int) ==> TD.Void)
    super.visitLineNumber(line, start)
  }

  private def unpreciseTypingReturnTypeDescriptorFor(td: TypeDescriptor): TypeDescriptor = {
    td match
      case _: (TD.Array | TD.Class) => TD.Object
      case primitive => primitive
  }

  private def isReturnInstr(opcode: Int): Boolean = {
    Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN
  }

  private def isVarStoreInstr(opcode: Int): Boolean = {
    Opcodes.ISTORE <= opcode && opcode <= Opcodes.ASTORE
  }

  private def isArrayStoreInstr(opcode: Int): Boolean = {
    Opcodes.IASTORE <= opcode && opcode <= Opcodes.SASTORE
  }

}
