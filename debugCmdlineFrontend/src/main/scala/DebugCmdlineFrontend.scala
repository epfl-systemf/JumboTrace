import scala.io.Source
import scala.util.{Failure, Success, Using}
import traceElements.{JsonParser, MethodCalled, Return, ReturnVoid}

object DebugCmdlineFrontend {

  def main(args: Array[String]): Unit = {
    val lines = Using(Source.fromFile(args.head)){ bufSrc =>
      bufSrc.getLines().toSeq
    }.get
    JsonParser.parse(lines.mkString("\n")) match
      case Failure(exception) => throw exception
      case Success(trace) =>
        var indent = 0
        for traceElem <- trace do {
          if (traceElem.isInstanceOf[Return] || traceElem.isInstanceOf[ReturnVoid]){
            indent -= 1
          }
          println(("  " * indent) + traceElem)
          if (traceElem.isInstanceOf[MethodCalled]){
            indent += 1
          }
        }
  }

}
