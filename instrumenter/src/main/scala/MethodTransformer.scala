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
                             ) extends MethodVisitor(Config.config.asmVersion, underlying) {

  private val ansiYellow = "\u001B[33m"
  private val ansiRed = "\u001B[31m"
  private val ansiReset = "\u001B[0m"

  import methodTable.{ownerClass, methodName, isMainMethod, methodDescr, tryCatches}

  private given MethodVisitor = underlying

  private lazy val tryCatchLabels = (new Label(), new Label())

  override def visitCode(): Unit = {
    if (isMainMethod) {
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
    if (isRetInstr) {
      if (methodDescr.ret == TD.Void) {
        LDC(methodName.name)
        INVOKE_STATIC(jumboTracer, ReturnedVoid.methodName, Seq(TD.String) ==> TD.Void)
      } else {
        val returnedTypeDescr = topmostTypeFor(methodDescr.ret)
        DUP(returnedTypeDescr)
        LDC(methodName.name)
        SWAP(TD.String, returnedTypeDescr)
        INVOKE_STATIC(jumboTracer, Returned.methodName, Seq(TD.String, returnedTypeDescr) ==> TD.Void)
      }
    } else if (opcode == Opcodes.IASTORE) {
      callToInstrumentedArrayStore(TD.Int)
    } else if (opcode == Opcodes.LASTORE) {
      callToInstrumentedArrayStore(TD.Long)
    } else if (opcode == Opcodes.FASTORE) {
      callToInstrumentedArrayStore(TD.Float)
    } else if (opcode == Opcodes.DASTORE) {
      callToInstrumentedArrayStore(TD.Double)
    } else if (opcode == Opcodes.AASTORE) {
      callToInstrumentedArrayStore(TD.Object)
    } else if (opcode == Opcodes.BASTORE) {
      callToInstrumentedArrayStore(TD.Byte)
    } else if (opcode == Opcodes.CASTORE) {
      callToInstrumentedArrayStore(TD.Char)
    } else if (opcode == Opcodes.SASTORE) {
      callToInstrumentedArrayStore(TD.Short)
    } else if (isArrayLoadInstr(opcode)) {
      val unpreciseTypeDescr = arrayLoadInstrTypeDescr(opcode)
      DUP2(TD.Int, TD.Array(unpreciseTypeDescr))
      INVOKE_STATIC(jumboTracer, ArrayLoad.methodName, Seq(TD.Array(unpreciseTypeDescr), TD.Int) ==> TD.Void)
    }
    if (isMainMethod && isRetInstr) {
      PRINTLN(ansiYellow + "JumboTracer: program terminating normally" + ansiReset)
      LDC("Program terminating normally")
      INVOKE_STATIC(jumboTracer, SaveTermination.methodName, Seq(TD.String) ==> TD.Void)
      INVOKE_STATIC(jumboTracer, display, Seq.empty ==> TD.Void) // TODO remove (just for debugging)
      INVOKE_STATIC(jumboTracer, writeJsonTrace, Seq.empty ==> TD.Void)
    }
    if (!isArrayStoreInstr(opcode)) {
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
    if (isVarLoadInstr(opcode) && !methodTable.isInitMethod) { // do not log variable loads in <init>, it is useless and crashes the program
      methodTable.localVars.get(varIndex).foreach { localVar =>
        val typeDescr = topmostTypeFor(localVar.descriptor)
        DUP(typeDescr)
        LDC(localVar.name)
        SWAP(TD.String, typeDescr)
        INVOKE_STATIC(jumboTracer, VariableGet.methodName, Seq(TD.String, typeDescr) ==> TD.Void)
      }
    }
  }

  override def visitIincInsn(varIndex: Int, increment: Int): Unit = {
    methodTable.localVars.get(varIndex).foreach { localVar =>
      LDC(localVar.name)
      LOAD(TD.Int, varIndex)
      INVOKE_STATIC(jumboTracer, VariableGet.methodName, Seq(TD.String, TD.Int) ==> TD.Void)
    }
    super.visitIincInsn(varIndex, increment)
    methodTable.localVars.get(varIndex).foreach { localVar =>
      LDC(localVar.name)
      LOAD(TD.Int, varIndex)
      INVOKE_STATIC(jumboTracer, VariableSet.methodName, Seq(TD.String, TD.Int) ==> TD.Void)
    }
  }

  override def visitFieldInsn(opcode: Int, ownerClass: String, fieldName: String, descriptor: String): Unit = {

    val preciseTypeDescr = TypeDescriptor.parse(descriptor)
    val unpreciseTypeDescr: TypeDescriptor = topmostTypeFor(preciseTypeDescr)

    def callToSuper(): Unit = {
      super.visitFieldInsn(opcode, ownerClass, fieldName, descriptor)
    }

    opcode match {
      case Opcodes.PUTSTATIC => {
        DUP(unpreciseTypeDescr)
        LDC(ownerClass)
        SWAP(TD.String, unpreciseTypeDescr)
        LDC(fieldName)
        SWAP(TD.String, unpreciseTypeDescr)
        INVOKE_STATIC(jumboTracer, StaticFieldSet.methodName, Seq(TD.String, TD.String, unpreciseTypeDescr) ==> TD.Void)
        callToSuper()
      }
      case Opcodes.PUTFIELD => {
        DUP2(unpreciseTypeDescr, TD.Object)
        LDC(fieldName)
        SWAP(TD.String, unpreciseTypeDescr)
        INVOKE_STATIC(jumboTracer, InstanceFieldSet.methodName, Seq(TD.Object, TD.String, unpreciseTypeDescr) ==> TD.Void)
        callToSuper()
      }
      case Opcodes.GETSTATIC => {
        callToSuper()
        DUP(unpreciseTypeDescr)
        LDC(ownerClass)
        SWAP(TD.String, unpreciseTypeDescr)
        LDC(fieldName)
        SWAP(TD.String, unpreciseTypeDescr)
        INVOKE_STATIC(jumboTracer, StaticFieldGet.methodName, Seq(TD.String, TD.String, unpreciseTypeDescr) ==> TD.Void)
      }
      case Opcodes.GETFIELD => {
        // Stack: o = owner | s = field name (string) | v = field value
        // Initially: [o
        DUP(TD.Object) // [oo
        DUP(TD.Object) // [ooo
        GETFIELD(ownerClass, fieldName, preciseTypeDescr) // [voo
        SWAP(unpreciseTypeDescr, TD.Object) // [ovo
        LDC(fieldName) // [sovo
        INVOKE_STATIC(jumboTracer, InstanceFieldGet.methodName, Seq(unpreciseTypeDescr, TD.Object, TD.String) ==> TD.Void) // [o
        callToSuper() // [v
      }
      case _ => assert(false, s"unexpected opcode: $opcode")
    }
  }

  override def visitMaxs(maxStack: Int, maxLocals: Int): Unit = {
    if (isMainMethod) {
      LABEL(tryCatchLabels._2)
      LDC("Program terminating with an exception")
      INVOKE_STATIC(jumboTracer, SaveTermination.methodName, Seq(TD.String) ==> TD.Void)
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

  private def isVarLoadInstr(opcode: Int): Boolean = {
    Opcodes.ILOAD <= opcode && opcode <= Opcodes.ALOAD
  }

  private def isArrayLoadInstr(opcode: Int): Boolean = {
    Opcodes.IALOAD <= opcode && opcode <= Opcodes.SALOAD
  }

  private def arrayLoadInstrTypeDescr(opcode: Int): TypeDescriptor = {
    require(isArrayLoadInstr(opcode))
    opcode match
      case Opcodes.IALOAD => TD.Int
      case Opcodes.LALOAD => TD.Long
      case Opcodes.FALOAD => TD.Float
      case Opcodes.DALOAD => TD.Double
      case Opcodes.AALOAD => TD.Object
      case Opcodes.BALOAD => TD.Byte
      case Opcodes.CALOAD => TD.Char
      case Opcodes.SALOAD => TD.Short
  }

}
