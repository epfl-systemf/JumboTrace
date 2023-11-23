package b2bCompiler

import org.objectweb.asm.Label

import LocalsTable.*

final class LocalsTable(methodUid: MethodUid, vars: Map[String, Seq[VarInfo]]) {
  
  
  
}

object LocalsTable {
  
  import scala.collection.mutable
  
  final class Builder(methodUid: MethodUid) {
    private val vars: mutable.Map[String, mutable.ListBuffer[VarInfo]] = mutable.Map.empty
    
    def recordVar(name: String, descriptor: String, startLabel: Label, endLabel: Label, idx: Int): Builder = {
      val tpeSig = TypeSignature.parse(descriptor)
      if (!vars.contains(name)){
        vars.addOne(name -> mutable.ListBuffer.empty)
      }
      vars.apply(name).addOne(VarInfo(name, tpeSig, startLabel, endLabel, idx))
      this
    }
    
    def built: LocalsTable = new LocalsTable(methodUid, vars.toMap.map((name, ls) => (name, ls.toSeq)))
  }
  
  final case class VarInfo(name: String, tpeSig: TypeSignature, startLabel: Label, endLabel: Label, idx: Int)
  
}
