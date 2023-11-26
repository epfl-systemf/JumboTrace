package b2bCompiler

import scala.collection.mutable

import org.objectweb.asm.Label

import MethodTable.*

final case class MethodTable(
                              methodUid: MethodUid,
                              varsDef: Map[Label, Seq[VarInfo]],
                              varsDelete: Map[Label, Seq[VarInfo]],
                              targetLabels: Set[Label]
                            )

object MethodTable {

  import scala.collection.mutable

  final class Builder(methodUid: MethodUid) {
    private val varsDef: mutable.Map[Label, mutable.ListBuffer[VarInfo]] = mutable.Map.empty
    private val varsDelete: mutable.Map[Label, mutable.ListBuffer[VarInfo]] = mutable.Map.empty
    private val targetLabels: mutable.Set[Label] = mutable.Set.empty

    def recordVar(name: String, descriptor: String, startLabel: Label, endLabel: Label, idx: Int): Builder = {
      val tpeSig = TypeSignature.parse(descriptor)
      val varInfo = VarInfo(name, tpeSig, startLabel, endLabel, idx)
      if (!varsDef.contains(startLabel)) {
        varsDef(startLabel) = mutable.ListBuffer.empty
      }
      varsDef(startLabel).addOne(varInfo)
      if (!varsDelete.contains(endLabel)) {
        varsDelete(endLabel) = mutable.ListBuffer.empty
      }
      varsDelete(endLabel).addOne(varInfo)
      this
    }
    
    def recordTargetLabel(label: Label): Builder = {
      targetLabels.addOne(label)
      this
    }

    def built: MethodTable = new MethodTable(
      methodUid,
      makeImmutable(varsDef),
      makeImmutable(varsDelete),
      targetLabels.toSet
    )

    private def makeImmutable(mutMap: mutable.Map[Label, mutable.ListBuffer[VarInfo]]): Map[Label, Seq[VarInfo]] = {
      mutMap.map { (label, varInfos) =>
        (label, varInfos.toSeq)
      }.toMap
    }
  }

}
