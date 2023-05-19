package instrumenter

import org.objectweb.asm.{ClassVisitor, MethodVisitor}
import instrumenter.TypeDescriptor as TD
import instrumenter.MethodDescriptor.*

final class ClassTransformer(
                              underlying: ClassVisitor,
                              classTable: ClassTable
                            ) extends ClassVisitor(Config.current.asmVersion, underlying) {

  override def visitMethod(access: Int, name: String, descriptor: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    new MethodTransformer(
      super.visitMethod(access, name, descriptor, signature, exceptions),
      classTable.getMethodTable(MethodName(name), MethodDescriptor.parse(descriptor).get).get
    )
  }

}
