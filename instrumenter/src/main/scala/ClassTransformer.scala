package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{ClassVisitor, MethodVisitor}
import com.epfl.systemf.jumbotrace.instrumenter.TypeDescriptor as TD
import com.epfl.systemf.jumbotrace.instrumenter.MethodDescriptor.*

final class ClassTransformer(
                              underlying: ClassVisitor,
                              classTable: ClassTable
                            ) extends ClassVisitor(Config.current.asmVersion, underlying) {

  override def visitMethod(access: Int, name: String, descriptor: String, signature: String, exceptions: Array[String]): MethodVisitor = {
    val defaultVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
    val isToString = (name == "toString" && descriptor == (Seq.empty ==> TD.String).toString)
    if (isToString){  // do not instrument toString since it is called by the instrumentation
      defaultVisitor
    } else {
      new MethodTransformer(
        defaultVisitor,
        classTable.getMethodTable(MethodName(name), MethodDescriptor.parse(descriptor).get).get
      )
    }
  }

}
