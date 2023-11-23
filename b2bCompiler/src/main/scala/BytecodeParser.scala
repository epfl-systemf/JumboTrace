package b2bCompiler

import org.objectweb.asm.ClassReader

import scala.collection.mutable.ListBuffer

final class BytecodeParser extends PipelineStage[Array[Byte], BytecodeParser.Output] {

  override def run(in: Array[Byte]): Seq[(MethodUid, Seq[RegularBytecodeInstr])] = {
    val methodsSeq = ListBuffer.empty[(MethodUid, ListBuffer[RegularBytecodeInstr])]
    val classParsingVisitor = new ClassParsingVisitor(methodsSeq)
    val classReader = new ClassReader(in)
    classReader.accept(classParsingVisitor, ClassReader.EXPAND_FRAMES)
    methodsSeq.map((descr, lsBuf) => (descr, lsBuf.toSeq)).toSeq
  }

}

object BytecodeParser {
  
  type Output = Seq[(MethodUid, Seq[RegularBytecodeInstr])]
  
}
