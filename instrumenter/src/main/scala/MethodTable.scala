package instrumenter

import MethodTable.{LocalVariable, TryCatch}
import TypeDescriptor.isDoubleWordType

import org.objectweb.asm.{Label, Opcodes}

import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.collection.immutable.SortedMap

/**
 * @param ownerClass   name of the class the method belongs to
 * @param methodName   name of the method
 * @param methodDescr  JVM descriptor of the method arguments types and return type
 * @param localVars    all the local variables appearing in the method body, incl. its parameters, grouped by their index
 * @param tryCatches   the try-catch blocks appearing in the method
 * @param isStatic     flag for static methods
 * @param isMainMethod flag for main methods
 */
final class MethodTable(
                         val ownerClass: ClassName,
                         val methodName: MethodName,
                         val methodDescr: MethodDescriptor,
                         val localVars: SortedMap[Int, List[LocalVariable]],
                         val tryCatches: Seq[TryCatch],
                         val isStatic: Boolean,
                         val isMainMethod: Boolean
                       ) {

  private val initMethodName = MethodName("<init>")

  val parameters: Seq[LocalVariable] = {
    val argsCnt = methodDescr.args.size + (if isStatic then 0 else 1)
    /* assumption: as the parameters are visible in the whole function body, it is safe to assume that the first indices,
     *  which are the parameters, are mapped to a singleton list, hence the use of flatten */
    val rawArgs = (
      localVars.toList
        .sortBy(_._1)
        .flatMap(_._2)
        .take(argsCnt)
      )
    // if init method, referring to the head of rawArgs seems to cause issues because it is the this ptr, which is not yet initialized
    if methodName == initMethodName then rawArgs.tail else rawArgs
  }

  def findLocalVar(variableIdx: Int, lastVisitedLabelIdx: Int): Option[LocalVariable] = {
    // the variable we want is the first (in program order) that has not yet gone out of scope
    localVars
      .get(variableIdx)
      .flatMap(_.find(varInfo => lastVisitedLabelIdx < varInfo.scopeEndLabelIdx))
  }

  def isInitMethod: Boolean = (methodName == initMethodName)

  override def toString: String = {
    val mainMethodSymbol = if isMainMethod then "*" else ""
    val staticSymbol = if isStatic then "static " else ""
    s"$staticSymbol$ownerClass::$methodName$mainMethodSymbol$methodDescr " + localVars.mkString("[", ",", "]") + tryCatches.mkString("[", ", ", "]")
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

    private val localVars = mutable.SortedMap.empty[Int, List[LocalVariable]] // var index to variable info
    private val tryCatches = mutable.ListBuffer.empty[TryCatch]
    private val labels = mutable.Map.empty[Label, Int] // label to label index

    def visitLabelInstr(label: Label, labelIdx: Int): Unit = {
      labels(label) = labelIdx
    }

    def visitVarInfoInstr(varName: String, descriptorStr: String, scopeStart: Label, scopeEnd: Label, idx: Int): Unit = {
      val descriptor = TypeDescriptor.parse(descriptorStr)
      localVars(idx) = localVars.getOrElse(idx, Nil) :+ LocalVariable(varName, descriptor, labels(scopeStart), labels(scopeEnd), idx)
    }

    def visitTryCatch(start: Label, end: Label, handler: Label, exceptionType: String): Unit = {
      tryCatches.addOne(TryCatch(start, end, handler, exceptionType))
    }

    def built: MethodTable = {
      new MethodTable(ownerClass, methodName, methodDescr, SortedMap.from(localVars), tryCatches.toSeq, isStatic = isStatic, isMainMethod = isMainMethod)
    }

  }

  final case class LocalVariable(
                                  name: String,
                                  descriptor: TypeDescriptor,
                                  scopeStartLabelIdx: Int,
                                  scopeEndLabelIdx: Int,
                                  idx: Int
                                ) {
    override def toString: String = s"{$name [label$scopeStartLabelIdx;label$scopeEndLabelIdx] idx=$idx ($descriptor)}"
  }

  final case class TryCatch(start: Label, end: Label, handler: Label, excType: String) {
    override def toString: String = s"$start - $end H:$handler [$excType]"
  }

}
