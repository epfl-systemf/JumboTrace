package b2bCompiler

import scala.collection.immutable.Seq

final class BytecodePrinter extends PipelineStage[Seq[(MethodUid, Seq[BytecodeInstr])], String] {

  override def run(methods: Seq[(MethodUid, Seq[BytecodeInstr])]): String = {
    val sb = new StringBuilder()
    for ((methodName, bytecode) <- methods){
      sb
        .append(methodName)
        .append(":\n")
      for (instr <- bytecode){
        sb
          .append("   ")
          .append(instr.toString)
          .append("\n")
      }
      sb.append("\n")
    }
    sb.toString()
  }

}
