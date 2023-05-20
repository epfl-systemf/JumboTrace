package instrumenter

import org.objectweb.asm.{Handle, Label, MethodVisitor, Opcodes}
import AsmDsl.*
import Injection.*
import Injection.EventMethod.*

import instrumenter.MethodDescriptor.==>
import instrumenter.MethodTable.LocalVariable
import instrumenter.TypeDescriptor as TD

final class MethodTransformer(
                               underlying: MethodVisitor,
                               methodTable: MethodTable
                             ) extends MethodVisitor(Config.current.asmVersion, underlying) {

  private val ansiYellow = "\u001B[33m"
  private val ansiRed = "\u001B[31m"
  private val ansiReset = "\u001B[0m"

  import methodTable.{ownerClass, methodName, isMainMethod, methodDescr, tryCatches}
  private given MethodVisitor = underlying

  private lazy val tryCatchLabels = (new Label(), new Label())

  override def visitCode(): Unit = {
    if (isMainMethod){
      TRY_CATCH(tryCatchLabels._1, tryCatchLabels._2, tryCatchLabels._2, "java/lang/Throwable")
      LABEL(tryCatchLabels._1)
    }
    for (argVar <- methodTable.arguments) do {
      val typeDescr = topmostTypeFor(argVar.descriptor)
      LOAD(typeDescr, argVar.idx)
      INVOKE_STATIC(jumboTracer, SaveArgument.methodName, Seq(typeDescr) ==> TD.Void)
    }
    LDC(ownerClass.name)
    LDC(methodName.name)
    LDC(methodTable.isStatic)
    INVOKE_STATIC(jumboTracer, TerminateMethodCall.methodName, Seq(TD.String, TD.String, TD.Boolean) ==> TD.Void)
    super.visitCode()
  }


  override def visitInsn(opcode: Int): Unit = {
    val isRetInstr = isReturnInstr(opcode)
    if (isRetInstr){
      if (methodDescr.ret == TD.Void){
        LDC(methodName.name)
        INVOKE_STATIC(jumboTracer, ReturnedVoid.methodName, Seq(TD.String) ==> TD.Void)
      } else {
        val returnedTypeDescr = topmostTypeFor(methodDescr.ret)
        DUP(returnedTypeDescr)
        LDC(methodName.name)
        SWAP(TD.String, returnedTypeDescr)
        INVOKE_STATIC(jumboTracer, Returned.methodName, Seq(TD.String, returnedTypeDescr) ==> TD.Void)
      }
    } else if (opcode == Opcodes.IASTORE){
      callToInstrumentedArrayStore(TD.Int)
    } else if (opcode == Opcodes.LASTORE){
      callToInstrumentedArrayStore(TD.Long)
    } else if (opcode == Opcodes.FASTORE){
      callToInstrumentedArrayStore(TD.Float)
    } else if (opcode == Opcodes.DASTORE){
      callToInstrumentedArrayStore(TD.Double)
    } else if (opcode == Opcodes.AASTORE){
      callToInstrumentedArrayStore(TD.Object)
    } else if (opcode == Opcodes.BASTORE){
      callToInstrumentedArrayStore(TD.Byte)
    } else if (opcode == Opcodes.CASTORE){
      callToInstrumentedArrayStore(TD.Char)
    } else if (opcode == Opcodes.SASTORE){
      callToInstrumentedArrayStore(TD.Short)
    }
    if (isMainMethod && isRetInstr) {
      PRINTLN(ansiYellow + "JumboTracer: program terminating normally" + ansiReset)
      INVOKE_STATIC(jumboTracer, display, Seq.empty ==> TD.Void) // TODO remove (just for debugging)
      INVOKE_STATIC(jumboTracer, writeJsonTrace, Seq.empty ==> TD.Void)
    }
    if (!isArrayStoreInstr(opcode)){
      super.visitInsn(opcode)
    }
  }

  override def visitVarInsn(opcode: Int, varIndex: Int): Unit = {
    if (isVarStoreInstr(opcode)) {
      methodTable.localVars.get(varIndex).foreach { localVar =>
        val typeDescr = topmostTypeFor(localVar.descriptor)
        DUP(typeDescr)
        LDC(localVar.name)
        SWAP(TD.String, typeDescr)
        INVOKE_STATIC(jumboTracer, VariableSet.methodName, Seq(TD.String, typeDescr) ==> TD.Void)
      }
    }
    super.visitVarInsn(opcode, varIndex)
  }

  override def visitFieldInsn(opcode: Int, ownerClass: String, name: String, descriptor: String): Unit = {

    lazy val unpreciseTypeDescr: TypeDescriptor = topmostTypeFor(TypeDescriptor.parse(descriptor).get)

    if (opcode == Opcodes.PUTSTATIC){
      DUP(unpreciseTypeDescr)
      LDC(ownerClass)
      SWAP(TD.String, unpreciseTypeDescr)
      LDC(name)
      SWAP(TD.String, unpreciseTypeDescr)
      INVOKE_STATIC(jumboTracer, StaticFieldSet.methodName, Seq(TD.String, TD.String, unpreciseTypeDescr) ==> TD.Void)
    } else if (opcode == Opcodes.PUTFIELD){
      DUP2(TD.Object, unpreciseTypeDescr)
      LDC(name)
      SWAP(TD.String, unpreciseTypeDescr)
      INVOKE_STATIC(jumboTracer, InstanceFieldSet.methodName, Seq(TD.Object, TD.String, unpreciseTypeDescr) ==> TD.Void)
    }
    super.visitFieldInsn(opcode, ownerClass, name, descriptor)
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

  private def callToInstrumentedArrayStore(elemType: TypeDescriptor): Unit = {
    INVOKE_STATIC(jumboTracer, InstrumentedArrayStore.methodName, Seq(TD.Array(elemType), TD.Int, elemType) ==> TD.Void)
  }

  private def topmostTypeFor(td: TypeDescriptor): TypeDescriptor = {
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
