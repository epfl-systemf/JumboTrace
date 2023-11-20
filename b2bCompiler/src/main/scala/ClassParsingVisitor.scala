package b2bCompiler

import org.objectweb.asm.{ClassVisitor, MethodVisitor}

import scala.collection.mutable.ListBuffer

/**
 * Name concatenated with descriptor
 */
type MethodUid = String

final class ClassParsingVisitor(methodsSeq: ListBuffer[(MethodUid, ListBuffer[RegularBytecodeInstr])]) extends ClassVisitor(Config.asmVersion) {

  override def visitMethod(access: Int, name: String, descriptor: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    val methodUid = name ++ descriptor
    val bytecodesSeq = ListBuffer.empty[RegularBytecodeInstr]
    methodsSeq.addOne(methodUid -> bytecodesSeq)
    new MethodParsingVisitor(bytecodesSeq)
  }

}
