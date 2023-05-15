package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{Label, MethodVisitor}

final class MethodExplorer(tableB: MethodTable.Builder) extends MethodVisitor(Config.current.asmVersion) {

  override def visitLabel(label: Label): Unit = {
    tableB.visitLabelInstr(label)
    super.visitLabel(label)
  }

  override def visitLocalVariable(name: String, descriptor: String, signature: String, start: Label, end: Label, index: Int): Unit = {
    tableB.visitVarInfoInstr(name, descriptor, start, end, index)
    super.visitLocalVariable(name, descriptor, signature, start, end, index)
  }
}
