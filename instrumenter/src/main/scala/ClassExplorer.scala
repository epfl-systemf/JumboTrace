package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{ClassVisitor, MethodVisitor}

import com.epfl.systemf.jumbotrace.instrumenter.TypeDescriptor as TD

final class ClassExplorer(classTableB: ClassTable.Builder) extends ClassVisitor(Config.current.asmVersion) {

  private val mainMethodDescriptorStr = MethodDescriptor(Seq(TD.Array(TD.String)), TD.Void).toString

  override def visitMethod(access: Int, name: String, descriptor: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    val isMainMethod = classTableB.isMainClass && name == "main" && descriptor == mainMethodDescriptorStr
    val methodDescriptor = MethodDescriptor.parse(descriptor).get
    val methodTableB = new MethodTable.Builder(classTableB.className, MethodName(name), methodDescriptor, isMainMethod)
    classTableB.addMethodTable(methodTableB)
    new MethodExplorer(methodTableB)
  }

}
