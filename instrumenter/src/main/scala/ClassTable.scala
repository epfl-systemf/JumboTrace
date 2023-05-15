package com.epfl.systemf.jumbotrace.instrumenter

import scala.collection.mutable

final class ClassTable(
                        val className: ClassName,
                        val methodTables: Map[(MethodName, MethodDescriptor), MethodTable],
                        val isMainClass: Boolean
                      ){
  def getMethodTable(methodName: MethodName, methodDescriptor: MethodDescriptor): Option[MethodTable] = {
    methodTables.get((methodName, methodDescriptor))
  }

  override def toString: String = {
    val width = 50
    val mainClassSymbol = if isMainClass then "*" else ""
    val header = s"-- $className$mainClassSymbol ".padTo(width, '-')
    val footer = "-" * width
    methodTables.values.mkString(header + "\n", "\n", "\n" + footer)
  }
}

object ClassTable {

  final class Builder(val className: ClassName, val isMainClass: Boolean) {
    private val methodTables = mutable.Map.empty[(MethodName, MethodDescriptor), MethodTable.Builder]

    def addMethodTable(methodTableB: MethodTable.Builder): Unit = {
      methodTables((methodTableB.methodName, methodTableB.methodDescr)) = methodTableB
    }

    def built: ClassTable = {
      val builtMethodTables = methodTables.toMap.map((id, tb) => (id, tb.built))
      new ClassTable(className, builtMethodTables, isMainClass)
    }

  }

}
