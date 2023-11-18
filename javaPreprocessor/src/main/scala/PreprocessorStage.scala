package javaPreprocessor

trait PreprocessorStage[-In, +Out] {
  thisStage =>

  protected def runImpl(input: In, errorReporter: ErrorReporter): Option[Out]

  final def run(input: In, errorReporter: ErrorReporter): Out = {
    val resultOpt = runImpl(input, errorReporter)
    errorReporter.displayAndTerminateIfErrors()
    assert(errorReporter.hasErrors == resultOpt.isEmpty)
    resultOpt match
      case Some(result) => result
      case None => assert(false)
  }

  final def andThen[NextOut](nextStage: PreprocessorStage[Out, NextOut]): PreprocessorStage[In, NextOut] = {
    (input: In, errorReporter: ErrorReporter) => {
      val thisStageRes = thisStage.run(input, errorReporter)
      nextStage.runImpl(thisStageRes, errorReporter)
    }
  }

  final def toMultiStage: PreprocessorStage[Seq[In], Seq[Out]] = {
    (inputs: Seq[In], errorReporter: ErrorReporter) => {
      inputs
        .map(thisStage.runImpl(_, errorReporter))
        .foldRight(Option(Seq.empty[Out])) { (curr, acc) =>
          for (a <- acc; c <- curr) yield c +: a
        }
    }
  }

}
