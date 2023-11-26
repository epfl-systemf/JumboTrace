package b2bCompiler

import org.objectweb.asm.Label

final case class VarInfo(name: String, tpeSig: TypeSignature, startLabel: Label, endLabel: Label, idx: Int) {
  override def toString: String = s"$name $tpeSig slot=$idx [$startLabel;$endLabel]"
}
