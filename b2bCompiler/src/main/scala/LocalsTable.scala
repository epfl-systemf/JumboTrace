package b2bCompiler

import scala.collection.mutable

import org.objectweb.asm.Label

import LocalsTable.*

final class LocalsTable(val methodUid: MethodUid, vars: Map[Int, Seq[VarInfo]]) {

  def findLocal(varIdx: Int, alreadySeenLabels: mutable.Set[Label]): Option[VarInfo] = {
    vars.get(varIdx).flatMap { varInfos =>
      val matchingVars =
        varInfos.filter { varInfo =>
            alreadySeenLabels.contains(varInfo.startLabel) && !alreadySeenLabels.contains(varInfo.endLabel)
          }
      end matchingVars
      if (matchingVars.size > 1) {
        throw AssertionError("ambiguous: several variables found")
      }
      matchingVars.headOption
    }
  }

  override def toString: String = {
    val sb = new StringBuilder()
    sb.append("TABLE of ")
      .append(methodUid)
      .append(":\n")
    for ((_, varsGroup) <- vars; varInfo <- varsGroup){
      sb.append("  ")
        .append(varInfo.toString)
        .append("\n")
    }
    sb.toString()
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

  final case class VarInfo(name: String, tpeSig: TypeSignature, startLabel: Label, endLabel: Label, idx: Int){
    override def toString: String = s"$name $tpeSig slot=$idx [$startLabel;$endLabel]"
  }

}
