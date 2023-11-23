package b2bCompiler

final class TablesCreator extends PipelineStage[BytecodeParser.Output, TablesCreator.Output] {

  override def run(in: BytecodeParser.Output): TablesCreator.Output = {
    for ((methodUid, code) <- in) yield {
      val tablesBuilder = LocalsTable.Builder(methodUid)
      code.foreach {
        case LocalVarB(name, descriptor, signature, start, end, idx) =>
          tablesBuilder.recordVar(name, descriptor, start, end, idx)
        case _ => ()
      }
      (tablesBuilder.built, code)
    }
  }

}

object TablesCreator {
  
  type Output = Seq[(LocalsTable, Seq[RegularBytecodeInstr])]

}
