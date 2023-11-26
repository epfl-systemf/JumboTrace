package b2bCompiler

final case class MethodState(
                              definedVars: Map[Int, VarInfo],
                              stackState: Map[AbsIntValue, List[AbsIntValue]]
                            )
