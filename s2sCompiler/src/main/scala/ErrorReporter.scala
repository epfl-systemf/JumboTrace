import com.github.javaparser.{Range, TokenRange}

import java.io.PrintStream
import java.util.Optional
import scala.collection.mutable.ListBuffer

final class ErrorReporter(out: PrintStream) {

  import ErrorReporter.*
  import ErrorLevel.*

  private val errors: ListBuffer[CompileError] = ListBuffer.empty

  export errors.nonEmpty as hasErrors

  def reportErrorPos(msg: String, errorLevel: ErrorLevel, fileName: Optional[FileName] | FileName, range: Optional[Range]): Unit = {
    val rngOpt = range.toScalaOption.map(_.begin)
    errors.addOne(CompileError(errorLevel, msg, Pos(
      fileName match {
        case name: FileName => Some(name)
        case _ => fileName.asInstanceOf[Optional[FileName]].toScalaOption
      },
      rngOpt.map(_.line),
      rngOpt.map(_.column)
    )))
    if (errorLevel == FatalError) {
      displayErrors()
      exitCompiler(-1)
    }
  }

  def reportErrorPos(msg: String, errorLevel: ErrorLevel.FatalError.type, fileName: Optional[FileName] | FileName, range: Optional[Range]): Nothing = {
    val level: ErrorLevel = errorLevel
    reportErrorPos(msg, level, fileName, range)
    assert(false)
  }

  transparent inline def reportError(inline msg: String, inline errorLevel: ErrorLevel): Unit = {
    reportErrorPos(msg, errorLevel, Optional.empty(), Optional.empty())
  }

  def displayAndTerminateIfErrors(): Unit = {
    displayErrors()
    if (errors.exists(err => err.errorLevel == NonFatalError || err.errorLevel == FatalError)) {
      exitCompiler(-1)
    } else {
      errors.clear()
    }
  }

  private def displayErrors(): Unit = {
    for (error <- errors) {
      out.println(error)
    }
  }

  private def exitCompiler(exitCode: Int): Nothing = {
    System.exit(exitCode)
    assert(false)
  }

}

object ErrorReporter {

  type FileName = String

  final case class Pos(fileName: Option[FileName], lineIdx: Option[Int], colIdx: Option[Int]) {
    // TODO is that a problem? Seems to not work properly when there are tabs
    def isEmpty: Boolean = fileName.isEmpty && lineIdx.isEmpty && colIdx.isEmpty
    override def toString: FileName = {
      s"${fileName.getOrElse("??")}:${lineIdx.getOrElse("??")}:${colIdx.getOrElse("??")}"
    }
  }

  final case class CompileError(errorLevel: ErrorLevel, msg: String, pos: Pos) {
    override def toString: String = s"[$errorLevel] $msg" ++ (if pos.isEmpty then "" else s" ($pos)")
  }

  enum ErrorLevel(descr: String) {
    case Warning extends ErrorLevel("WARNING")
    case CriticalWarning extends ErrorLevel("CRITICAL WARNING")
    case NonFatalError extends ErrorLevel("ERROR")
    case FatalError extends ErrorLevel("FATAL ERROR")

    override def toString: String = descr
  }

  extension[T] (opt: Optional[T]) private def toScalaOption: Option[T] = {
    if opt.isPresent then Some(opt.get()) else None
  }

}
