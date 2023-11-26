package b2bCompiler

import org.objectweb.asm.Label

import scala.collection.mutable

final class AbsIntPreparator extends PipelineStage[TablesCreator.Output, AbsIntPreparator.Output] {

  override def run(in: TablesCreator.Output): AbsIntPreparator.Output = {
    in.map { (table, code) =>
      val (newCode, basicBlocksMap) = prepare(code, table)
      (table.methodUid, newCode, basicBlocksMap)
    }
  }

  private def prepare(
                       code: Seq[RegularBytecodeInstr],
                       table: MethodTable
                     ): (List[RegularBytecodeInstr | BasicBlockStart], Map[Label, List[RegularBytecodeInstr | BasicBlockStart]]) = {
    var newCode = List.empty[RegularBytecodeInstr | BasicBlockStart]
    val varsMapping = mutable.Map.empty[Int, VarInfo]
    val basicBlocksMap = mutable.Map.empty[Label, List[RegularBytecodeInstr | BasicBlockStart]]
    for (instr <- code.reverse) {
      //      println(s"Preparing $instr") // TODO remove
      instr match {
        case JumpInsn(opcode, label) =>
          newCode = instr :: BasicBlockStart(varsMapping.toMap, mutable.Set.empty) :: newCode
        case LabelOccurenceB(label) =>
          table.varsDelete.get(label).foreach {
            _.foreach { varInfo =>
              varsMapping(varInfo.idx) = varInfo
            }
          }
          table.varsDef.get(label).foreach {
            _.foreach { varInfo =>
              val removed = varsMapping.remove(varInfo.idx)
              assert(removed.forall(_ == varInfo))
            }
          }
          if (table.targetLabels.contains(label)) {
            newCode = BasicBlockStart(varsMapping.toMap, mutable.Set.empty) :: instr :: newCode
          }
        case _ =>
          newCode = instr :: newCode
      }
    }
    (newCode, basicBlocksMap.toMap)
  }

}

object AbsIntPreparator {

  type Output = Seq[(
    MethodUid,
      List[RegularBytecodeInstr | BasicBlockStart],
      Map[Label, List[RegularBytecodeInstr | BasicBlockStart]]
    )]

}
