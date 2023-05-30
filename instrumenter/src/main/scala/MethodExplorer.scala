package instrumenter

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

  override def visitLineNumber(line: Int, start: Label): Unit = {
    tableB.visitLineNumberInstr(line, start)
    super.visitLineNumber(line, start)
  }

  override def visitTryCatchBlock(start: Label, end: Label, handler: Label, exceptionType: String): Unit = {
    tableB.visitTryCatch(start, end, handler, exceptionType)
    super.visitTryCatchBlock(start, end, handler, exceptionType)
  }
  
}
