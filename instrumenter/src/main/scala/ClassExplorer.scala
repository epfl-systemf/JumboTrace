package instrumenter

import org.objectweb.asm.{ClassVisitor, MethodVisitor, Opcodes}
import instrumenter.TypeDescriptor as TD

final class ClassExplorer(classTableB: ClassTable.Builder, logger: String => Unit) extends ClassVisitor(Config.current.asmVersion) {

  private val mainMethodDescriptorStr = MethodDescriptor(Seq(TD.Array(TD.String)), TD.Void).toString

  override def visitMethod(access: Int, name: String, descriptor: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    val isStatic = (access & Opcodes.ACC_STATIC) != 0
    val isMainMethod = isStatic && classTableB.isMainClass && name == "main" && descriptor == mainMethodDescriptorStr
    val methodDescriptor = MethodDescriptor.parse(descriptor).get
    logger(s"Exploring ${classTableB.className}::$name")
    val methodTableB = new MethodTable.Builder(classTableB.className, MethodName(name), methodDescriptor, isStatic = isStatic, isMainMethod = isMainMethod)
    classTableB.addMethodTable(methodTableB)
    new MethodExplorer(methodTableB)
  }

}
