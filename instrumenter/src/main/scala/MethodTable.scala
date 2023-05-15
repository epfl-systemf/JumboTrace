package com.epfl.systemf.jumbotrace.instrumenter

import MethodTable.LocalVariable

import org.objectweb.asm.{Label, Opcodes}

import scala.collection.mutable.ListBuffer
import scala.collection.mutable

final class MethodTable(
                         val ownerClass: ClassName,
                         val methodName: MethodName,
                         val methodDescr: MethodDescriptor,
                         val localVars: Seq[LocalVariable]
                       ){
  override def toString: String = {
    s"$ownerClass::$methodName$methodDescr " + localVars.mkString("[", ",", "]")
  }
}

object MethodTable {

  final class Builder(val ownerClass: ClassName, val methodName: MethodName, val methodDescr: MethodDescriptor) {

    private val localVars = ListBuffer.empty[LocalVariable]
    private val labels = mutable.Map.empty[Label, Int]  // label to line index

    private var currLineIdx: Int = -1

    def visitLabelInstr(label: Label): Unit = {
      labels(label) = currLineIdx  // by default, assign the label to the last encountered line idx (may be updated)
    }

    def visitLineNumberInstr(lineIdx: Int, label: Label): Unit = {
      labels(label) = lineIdx  // overwrite the default assignment if the program explicitely tells which line the label belongs to
      currLineIdx = lineIdx
    }

    def visitVarInfoInstr(varName: String, descriptorStr: String, scopeStart: Label, scopeEnd: Label, idx: Int): Unit = {
      val descriptor = TypeDescriptor.parse(descriptorStr).get
      val scope = Scope(labels.apply(scopeStart), labels.apply(scopeEnd))
      localVars.addOne(LocalVariable(varName, descriptor, scope, idx))
    }
    
    def built: MethodTable = new MethodTable(ownerClass, methodName, methodDescr, localVars.toSeq)

  }

  final case class Scope(startLine: Int, lastLine: Int){
    override def toString: String = s"[$startLine ; $lastLine]"
  }

  final case class LocalVariable(name: String, descriptor: TypeDescriptor, scope: Scope, idx: Int){
    override def toString: String = s"$name ($descriptor) scope=$scope idx=$idx"
  }

}
