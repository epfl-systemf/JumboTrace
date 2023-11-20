package b2bCompiler

trait PipelineStage[In, Out] { thisStage =>
  
  def run(in: In): Out
  
  final def andThen[NextIn >: Out, NextOut](nextStage: PipelineStage[NextIn, NextOut]): PipelineStage[In, NextOut] = {
    (in: In) => {
      val interm = thisStage.run(in)
      nextStage.run(interm)
    }
  }
  
}
