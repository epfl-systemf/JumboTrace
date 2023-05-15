package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{ClassVisitor, MethodVisitor}

final class ClassExplorer(classTableB: ClassTable.Builder) extends ClassVisitor(Config.current.asmVersion) {
  
  override def visitMethod(access: Int, name: String, descriptor: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    val methodDescriptor = MethodDescriptor.parse(descriptor).get
    val methodTableB = new MethodTable.Builder(classTableB.className, MethodName(name), methodDescriptor)
    new MethodExplorer(methodTableB)
  }
  
}
