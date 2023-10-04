package instrumenter

import org.objectweb.asm.{Label, MethodVisitor}

/**
 * Traverses a method to build a [[MethodTable]]
 */
final class MethodExplorer(tableB: MethodTable.Builder) extends MethodVisitor(Config.config.asmVersion) {
  private var lastVisitedLabelIdx = -1

  override def visitLabel(label: Label): Unit = {
    lastVisitedLabelIdx += 1
    tableB.visitLabelInstr(label, lastVisitedLabelIdx)
    super.visitLabel(label)
  }

  override def visitLocalVariable(name: String, descriptor: String, signature: String, start: Label, end: Label, index: Int): Unit = {
    tableB.visitVarInfoInstr(name, descriptor, start, end, index)
    super.visitLocalVariable(name, descriptor, signature, start, end, index)
  }

  override def visitTryCatchBlock(start: Label, end: Label, handler: Label, exceptionType: String): Unit = {
    tableB.visitTryCatch(start, end, handler, exceptionType)
    super.visitTryCatchBlock(start, end, handler, exceptionType)
  }
  
}
