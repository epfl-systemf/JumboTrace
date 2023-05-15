package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{Label, MethodVisitor, Opcodes}
import AsmDsl.*
import Injection.*

import com.epfl.systemf.jumbotrace.instrumenter.TypeDescriptor as TD

final class MethodTransformer(
                               underlying: MethodVisitor,
                               methodTable: MethodTable
                             ) extends MethodVisitor(Config.current.asmVersion, underlying) {

  import methodTable.{ownerClass, methodName, isMainMethod}
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
    if (isMainMethod && isReturnInstr(opcode)) {
      INVOKE_STATIC(jumboTracer, display, MethodDescriptor(Seq.empty, TD.Void))
    }
    super.visitInsn(opcode)
  }

  override def visitMaxs(maxStack: Int, maxLocals: Int): Unit = {
    if (isMainMethod){
      LABEL(tryCatchLabels._2)
      INVOKE_STATIC(jumboTracer, display, MethodDescriptor(Seq.empty, TD.Void))
      RETURN
    }
    super.visitMaxs(maxStack, maxLocals)
  }

  override def visitLineNumber(line: Int, start: Label): Unit = {
    LDC(ownerClass.name)
    LDC(line)
    INVOKE_STATIC(jumboTracer, lineVisited, stringIntToVoid)
    super.visitLineNumber(line, start)
  }

  private def isReturnInstr(opcode: Int): Boolean = {
    import Opcodes.*
    opcode match
      case IRETURN | LRETURN | FRETURN | DRETURN | ARETURN | RETURN => true
      case _ => false
  }

}
