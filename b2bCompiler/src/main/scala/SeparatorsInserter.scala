package b2bCompiler

import OpcodesHelpers.*
import org.objectweb.asm.Opcodes

final class SeparatorsInserter extends PipelineStage[Seq[(MethodUid, Seq[RegularBytecodeInstr])], Seq[(MethodUid, Seq[BytecodeInstr])]] {

  override def run(input: Seq[(MethodUid, Seq[RegularBytecodeInstr])]): Seq[(MethodUid, Seq[BytecodeInstr])] = {
    input.map((uid, code) => (uid, insertSeparators(code)))
  }

  private def insertSeparators(code: Seq[RegularBytecodeInstr]): Seq[BytecodeInstr] = {
    val reversedInstr = List.newBuilder[BytecodeInstr]
    var demand = 0
    var alreadyInserted = true  // do not mark end of last statement
    for (instr <- code.reverse){
      alreadyInserted &= (demand == 0)
      if (demand == 0 && !alreadyInserted){
        reversedInstr.addOne(StatementSeparator())
        alreadyInserted = true
      }
      reversedInstr.addOne(instr)
      demand += deltaDemand(instr)
    }
    reversedInstr.addOne(StatementSeparator())  // mark beginning of first statement
    reversedInstr.result().reverse
  }

  private def deltaDemand(instr: RegularBytecodeInstr): Int = {
    deltaDemand(
      instr match {
        case LdcInsn(value) =>
          stackUpdateLdc(value)
        case MultiANewArrayInsn(descriptor, numDimensions) =>
          stackUpdateMultianewarray(numDimensions)
        case FieldInsn(opcode, owner, name, descriptor) =>
          stackUpdateField(opcode, TypeSignature.parse(descriptor))
        case MethodInsn(opcode, owner, name, descriptor, isInterface) =>
          stackUpdateInvocation(MethodSignature.parse(descriptor))
        case _ =>
          instr.opcodeOpt.map(stackUpdate).getOrElse(Seq.empty -> Seq.empty)
      }
    )
  }

  private def deltaDemand(stackUpdate: StackUpdate): Int = {
    val consumedSum = stackUpdate._1.map(_.nSlots).sum
    val producedSum = stackUpdate._2.map(_.nSlots).sum
    consumedSum - producedSum
  }

}
