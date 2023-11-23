package b2bCompiler

import OpcodesHelpers.*
import org.objectweb.asm.Opcodes

final class SeparatorsInserter extends PipelineStage[BytecodeParser.Output, SeparatorsInserter.Output] {

  override def run(input: Seq[(MethodUid, Seq[RegularBytecodeInstr])]): Seq[(MethodUid, Seq[BytecodeInstr])] = {
    input.map((uid, code) => (uid, insertSeparators(code)))
  }

  private def insertSeparators(rawCode: Seq[RegularBytecodeInstr]): Seq[BytecodeInstr] = {
    val codeB = Seq.newBuilder[BytecodeInstr]
    var stackCnt = 0
    var currLine = -1
    for (instr <- rawCode){
      instr match {
        case LineNumberB(line, start) =>
          currLine = line
        case _ => ()
      }
      if (stackCnt == 0 && !instr.isMetadata){
        codeB.addOne(StatementSeparator(currLine))
      }
      codeB.addOne(instr)
      stackCnt += stackDelta(instr)
    }
    codeB.result()
  }

  private def stackDelta(instr: RegularBytecodeInstr): Int = {
    stackDelta(
      instr match {
        case LdcInsn(value) =>
          stackUpdateLdc(value)
        case MultiANewArrayInsn(descriptor, numDimensions) =>
          stackUpdateMultianewarray(numDimensions)
        case FieldInsn(opcode, owner, name, descriptor) =>
          stackUpdateField(opcode, TypeSignature.parse(descriptor))
        case MethodInsn(opcode, owner, name, descriptor, isInterface) =>
          stackUpdateInvocation(opcode, MethodSignature.parse(descriptor))
        case _ =>
          instr.opcodeOpt.map(stackUpdate).getOrElse(Seq.empty -> Seq.empty)
      }
    )
  }

  private def stackDelta(stackUpdate: StackUpdate): Int = {
    val consumedSum = stackUpdate._1.map(_.nSlots).sum
    val producedSum = stackUpdate._2.map(_.nSlots).sum
    producedSum - consumedSum
  }

}

object SeparatorsInserter {
  
  type Output = Seq[(MethodUid, Seq[BytecodeInstr])]
  
}
