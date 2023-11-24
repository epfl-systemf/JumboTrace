package b2bCompiler

import scala.collection.mutable

import org.objectweb.asm.Label

import LocalsTable.*

final class LocalsTable(methodUid: MethodUid, vars: Map[Int, Seq[VarInfo]]) {

  def findLocal(varIdx: Int, alreadySeenLabels: mutable.Set[Label]): VarInfo = {
    val matchingVars =
      vars.apply(varIdx).filter { varInfo =>
        alreadySeenLabels.contains(varInfo.startLabel) && !alreadySeenLabels.contains(varInfo.endLabel)
      }
    end matchingVars
    if (matchingVars.isEmpty){
      throw AssertionError("no variable found")
    } else if (matchingVars.size > 1){
      throw AssertionError("ambiguous: several variables found")
    }
    matchingVars.head
  }

}

object LocalsTable {

  import scala.collection.mutable

  final class Builder(methodUid: MethodUid) {
    private val vars: mutable.Map[Int, mutable.ListBuffer[VarInfo]] = mutable.Map.empty

    def recordVar(name: String, descriptor: String, startLabel: Label, endLabel: Label, idx: Int): Builder = {
      val tpeSig = TypeSignature.parse(descriptor)
      if (!vars.contains(idx)) {
        vars.addOne(idx -> mutable.ListBuffer.empty)
      }
      vars.apply(idx).addOne(VarInfo(name, tpeSig, startLabel, endLabel, idx))
      this
    }

    def built: LocalsTable = new LocalsTable(methodUid, vars.toMap.map((idx, ls) => (idx, ls.toSeq)))
  }

  final case class VarInfo(name: String, tpeSig: TypeSignature, startLabel: Label, endLabel: Label, idx: Int)

}
