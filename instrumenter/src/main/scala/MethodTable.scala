package com.epfl.systemf.jumbotrace.instrumenter

import MethodTable.LocalVariable
import TypeDescriptor.isDoubleWordType

import org.objectweb.asm.{Label, Opcodes}

import scala.collection.mutable.ListBuffer
import scala.collection.mutable

final class MethodTable(
                         val ownerClass: ClassName,
                         val methodName: MethodName,
                         val methodDescr: MethodDescriptor,
                         val localVars: Map[Int, LocalVariable],
                         val isStatic: Boolean,
                         val isMainMethod: Boolean
                       ){

  def localVarsIndices: Seq[Int] = {
    var skip = (methodName.name == "<init>")  // aload on receiver as the first instruction in <init> crashes the program
    val indicesCnt = methodDescr.args.size + (if isStatic then 0 else 1)
    val indices = ListBuffer.empty[Int]
    var currIdx = 0
    for ((_, localVar) <- localVars.take(indicesCnt)) do {
      if (!skip){
        indices.addOne(currIdx)
      }
      currIdx += (if isDoubleWordType(localVar.descriptor) then 2 else 1)
      skip = false
    }
    indices.toSeq
  }

  override def toString: String = {
    val mainMethodSymbol = if isMainMethod then "*" else ""
    val staticSymbol = if isStatic then "static " else ""
    s"$staticSymbol$ownerClass::$methodName$mainMethodSymbol$methodDescr " + localVars.mkString("[", ",", "]")
  }
}

object MethodTable {

  final class Builder(
                       val ownerClass: ClassName,
                       val methodName: MethodName,
                       val methodDescr: MethodDescriptor,
                       val isStatic: Boolean,
                       val isMainMethod: Boolean
                     ) {

    private val localVars = mutable.Map.empty[Int, LocalVariable]   // var index to variable info
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
      localVars(idx) = LocalVariable(varName, descriptor, scope, idx)
    }

    def built: MethodTable = {
      new MethodTable(ownerClass, methodName, methodDescr, localVars.toMap, isStatic = isStatic, isMainMethod = isMainMethod)
    }

  }

  final case class Scope(startLine: Int, lastLine: Int){
    override def toString: String = s"[$startLine;$lastLine]"
  }

  final case class LocalVariable(name: String, descriptor: TypeDescriptor, scope: Scope, idx: Int){
    override def toString: String = s"{$name $scope idx=$idx ($descriptor)}"
  }

}
