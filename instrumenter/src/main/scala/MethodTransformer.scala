package instrumenter

import org.objectweb.asm.{Handle, Label, MethodVisitor, Opcodes}
import AsmDsl.*
import Injection.*
import Injection.EventMethod.*

import instrumenter.MethodDescriptor.==>
import instrumenter.MethodTable.LocalVariable
import instrumenter.TypeDescriptor as TD
import scala.collection.mutable

/**
 * Traverses a method and performs the injection of events-saving instructions
 */
final class MethodTransformer(
                               underlying: MethodVisitor,
                               methodTable: MethodTable,
                               instrumentedClasses: Set[ClassName]
                             ) extends MethodVisitor(Config.config.asmVersion, underlying) {

  private val ansiYellow = "\u001B[33m"
  private val ansiRed = "\u001B[31m"
  private val ansiReset = "\u001B[0m"

  private val initMethodName = MethodName("<init>")

  import methodTable.{ownerClass, methodName, isMainMethod, methodDescr, tryCatches}

  private given MethodVisitor = underlying

  private var lastVisitedLabelIdx = -1
  private var alreadySeenObjectInit = false

  private def canAccessClassField: Boolean = methodName != initMethodName || alreadySeenObjectInit

  override def visitCode(): Unit = {
    for (argVar <- methodTable.parameters) do {
      val td = topmostTypeFor(argVar.descriptor)
      LOAD(td, argVar.idx)
      INVOKE_STATIC(jumboTracer, SaveArgument.methodName, Seq(td) ==> TD.Void)
    }
    LDC(ownerClass.name)
    LDC(methodName.name)
    LDC(methodTable.isStatic)
    INVOKE_STATIC(jumboTracer, TerminateMethodCall.methodName, Seq(TD.String, TD.String, TD.Boolean) ==> TD.Void)
    INVOKE_STATIC(jumboTracer, IncrementNestingLevel.methodName, Seq.empty ==> TD.Void)
    super.visitCode()
  }

  override def visitMethodInsn(opcode: Int, ownerClass: String, methodName: String, descriptor: String, isInterface: Boolean): Unit = {
    // save call from caller iff the callee is not instrumented (o.w. it will save the call itself)
    if (!instrumentedClasses.contains(ClassName(ownerClass))) {
      val isStatic = (opcode == Opcodes.INVOKESTATIC)
      generateSaveAndPushbackArgs(ownerClass, methodName, descriptor, isStatic)
    }
    super.visitMethodInsn(opcode, ownerClass, methodName, descriptor, isInterface)
    alreadySeenObjectInit ||= (methodName == initMethodName.name)
  }

  override def visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String): Unit = {
    // TODO check if this is safe
    throw new UnsupportedOperationException("unexpected call to deprecated version of visitMethodInsn")
  }

  // TODO see if it is safe to ignore invokedynamic

  private def generateSaveAndPushbackArgs(
                                           ownerClass: String,
                                           methodName: String,
                                           descriptor: String,
                                           isStatic: Boolean
                                         ): Unit = {
    val methodDescr = MethodDescriptor.parse(descriptor)
    for (td <- methodDescr.args.reverse) {
      INVOKE_STATIC(jumboTracer, SaveArgument.methodName, Seq(topmostTypeFor(td)) ==> TD.Void)
    }
    INVOKE_STATIC(jumboTracer, ReverseArgsList.methodName, Seq.empty ==> TD.Void)
    for ((td, i) <- methodDescr.args.zipWithIndex) do {
      val topmostTd = topmostTypeFor(td)
      LDC(i)
      INVOKE_STATIC(jumboTracer, PushbackArg(topmostTd).methodName, Seq(TD.Int) ==> topmostTd)
      td match {
        case td: (TD.Array | TD.Class) =>
          CHECKCAST(td)
        case _ => ()
      }
    }
    LDC(ownerClass)
    LDC(methodName)
    LDC(isStatic)
    INVOKE_STATIC(jumboTracer, TerminateMethodCall.methodName, Seq(TD.String, TD.String, TD.Boolean) ==> TD.Void)
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
      // BASTORE can be applied to both bytes and booleans
      // so we test the type of the array, cast it and call the appropriate method
      val elseBrLabel = new Label()
      val endifLabel = new Label()
      // bring array to the top of the stack
      mv.visitInsn(Opcodes.DUP_X2)
      mv.visitInsn(Opcodes.POP)
      mv.visitInsn(Opcodes.DUP_X2)
      mv.visitInsn(Opcodes.POP)
      DUP(TD.Object)
      INSTANCEOF(TD.Array(TD.Byte))
      IFEQ(elseBrLabel)
      CHECKCAST(TD.Array(TD.Byte))
      // bring back array to its initial position
      mv.visitInsn(Opcodes.DUP_X2)
      mv.visitInsn(Opcodes.POP)
      callToInstrumentedArrayStore(TD.Byte)
      GOTO(endifLabel)
      LABEL(elseBrLabel)
      CHECKCAST(TD.Array(TD.Boolean))
      // bring back array to its initial position
      mv.visitInsn(Opcodes.DUP_X2)
      mv.visitInsn(Opcodes.POP)
      callToInstrumentedArrayStore(TD.Boolean)
      LABEL(endifLabel)
    } else if (opcode == Opcodes.CASTORE) {
      callToInstrumentedArrayStore(TD.Char)
    } else if (opcode == Opcodes.SASTORE) {
      callToInstrumentedArrayStore(TD.Short)
    } else if (opcode == Opcodes.BALOAD) {
      // BALOAD can be applied to both bytes and booleans
      // so we test the type of the array, cast it and call the appropriate method
      val elseLabel = new Label()
      val endLabel = new Label()
      DUP2(TD.Int, TD.Object)
      // bring array to the top of the stack
      mv.visitInsn(Opcodes.SWAP)
      DUP(TD.Object)
      INSTANCEOF(TD.Array(TD.Byte))
      IFEQ(elseLabel)
      CHECKCAST(TD.Array(TD.Byte))
      // bring back array to its initial position
      mv.visitInsn(Opcodes.SWAP)
      INVOKE_STATIC(jumboTracer, ArrayLoad.methodName, Seq(TD.Array(TD.Byte), TD.Int) ==> TD.Void)
      GOTO(endLabel)
      LABEL(elseLabel)
      CHECKCAST(TD.Array(TD.Boolean))
      // bring back array to its initial position
      mv.visitInsn(Opcodes.SWAP)
      INVOKE_STATIC(jumboTracer, ArrayLoad.methodName, Seq(TD.Array(TD.Boolean), TD.Int) ==> TD.Void)
      LABEL(endLabel)
    } else if (isArrayLoadInstr(opcode)) { // except the special case of BALOAD, which is handled above
      val unpreciseTypeDescr = arrayLoadInstrTypeDescr(opcode)
      DUP2(TD.Int, TD.Array(unpreciseTypeDescr))
      INVOKE_STATIC(jumboTracer, ArrayLoad.methodName, Seq(TD.Array(unpreciseTypeDescr), TD.Int) ==> TD.Void)
    }
    if (!isArrayStoreInstr(opcode)) {
      super.visitInsn(opcode)
    }
  }

  override def visitVarInsn(opcode: Int, varIndex: Int): Unit = {
    if (isVarStoreInstr(opcode)) {
      methodTable.findLocalVar(varIndex, lastVisitedLabelIdx)
        .foreach { localVar =>
          val typeDescr = topmostTypeFor(localVar.descriptor)
          DUP(typeDescr)
          LDC(localVar.name)
          SWAP(TD.String, typeDescr)
          INVOKE_STATIC(jumboTracer, VariableSet.methodName, Seq(TD.String, typeDescr) ==> TD.Void)
        }
    }
    super.visitVarInsn(opcode, varIndex)
    if (isVarLoadInstr(opcode) && !methodTable.isInitMethod) { // do not log variable loads in <init>, it is useless and crashes the program
      // FIXME check whether the exclusion of <init> is necessary
      methodTable.findLocalVar(varIndex, lastVisitedLabelIdx)
        .foreach { localVar =>
          val typeDescr = topmostTypeFor(localVar.descriptor)
          DUP(typeDescr)
          LDC(localVar.name)
          SWAP(TD.String, typeDescr)
          INVOKE_STATIC(jumboTracer, VariableGet.methodName, Seq(TD.String, typeDescr) ==> TD.Void)
        }
    }
  }

  override def visitIincInsn(varIndex: Int, increment: Int): Unit = { // IINC is i++
    methodTable.findLocalVar(varIndex, lastVisitedLabelIdx)
      .foreach { localVar =>
        LDC(localVar.name)
        LOAD(TD.Int, varIndex)
        INVOKE_STATIC(jumboTracer, VariableGet.methodName, Seq(TD.String, TD.Int) ==> TD.Void)
      }
    super.visitIincInsn(varIndex, increment)
    methodTable.findLocalVar(varIndex, lastVisitedLabelIdx).foreach { localVar =>
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
        if (canAccessClassField) {
          DUP2(unpreciseTypeDescr, TD.Object)
          LDC(fieldName)
          SWAP(TD.String, unpreciseTypeDescr)
          INVOKE_STATIC(jumboTracer, InstanceFieldSet.methodName, Seq(TD.Object, TD.String, unpreciseTypeDescr) ==> TD.Void)
        }
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
        if (canAccessClassField) {
          DUP(TD.Object) // [oo
          DUP(TD.Object) // [ooo
          GETFIELD(ownerClass, fieldName, preciseTypeDescr) // [voo
          SWAP(unpreciseTypeDescr, TD.Object) // [ovo
          LDC(fieldName) // [sovo
          INVOKE_STATIC(jumboTracer, InstanceFieldGet.methodName, Seq(unpreciseTypeDescr, TD.Object, TD.String) ==> TD.Void) // [o
        }
        callToSuper() // [v
      }
      case _ => assert(false, s"unexpected opcode: $opcode")
    }
  }

  override def visitLineNumber(line: Int, start: Label): Unit = {
    LDC(ownerClass.name)
    LDC(line)
    INVOKE_STATIC(jumboTracer, LineVisited.methodName, Seq(TD.String, TD.Int) ==> TD.Void)
    super.visitLineNumber(line, start)
  }

  override def visitLabel(label: Label): Unit = {
    super.visitLabel(label)
    lastVisitedLabelIdx += 1
  }

  private def callToInstrumentedArrayStore(elemType: TypeDescriptor): Unit = {
    INVOKE_STATIC(jumboTracer, InstrumentedArrayStore.methodName, Seq(TD.Array(elemType), TD.Int, elemType) ==> TD.Void)
  }

  /**
   * Topmost type in the hierarchy, without considering boxing
   */
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
