package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{Label, MethodVisitor}
import AsmDsl.*
import Injection.*

final class MethodTransformer(
                               underlying: MethodVisitor,
                               methodTable: MethodTable
                             ) extends MethodVisitor(Config.current.asmVersion, underlying) {

  import methodTable.{ownerClass, methodName}
  given MethodVisitor = underlying

  override def visitCode(): Unit = {
    // TODO save arguments
    super.visitCode()
  }

  override def visitLineNumber(line: Int, start: Label): Unit = {
    LDC(ownerClass)
    LDC(line)
    INVOKE_STATIC(jumboTracer, lineVisited, stringIntToVoid)
    super.visitLineNumber(line, start)
  }

}
