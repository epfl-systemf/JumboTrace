package javaHtmlFrontend

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.{ClassOrInterfaceDeclaration, EnumDeclaration, VariableDeclarator}
import com.github.javaparser.ast.expr.{AssignExpr, NameExpr}
import j2html.{Config, TagCreator}
import j2html.TagCreator.*
import j2html.attributes.Attr
import j2html.tags.specialized.DivTag
import j2html.tags.{ContainerTag, DomContent, Tag}
import javaHtmlFrontend.Parser.ParsingSuccess
import traceElements.*

import DisplayRefiner.{refinedValue, refineMethodName}

import java.io.{File, FileWriter, PrintWriter}
import java.util
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try, Using}

object JavaHtmlFrontend {

  private val pluggedValueLengthLimit = 8
  private val codeLineMinWidth = 80
  private val preSpacing = "margin-top: 5px;"

  private type DisplayableTraceElement = LineVisited | MethodCalled | Initialization | Termination

  def main(args: Array[String]): Unit = {

    if (args.isEmpty || args.tail.exists(!_.endsWith(".java"))) {
      System.err.println("Usage:")
      System.err.println("first argument is the path to the trace")
      System.err.println("subsequent arguments are paths to .java source files")
      exit(-1)
    }

    val traceFileName = args.head
    val srcFilesNames = args.tail.toSeq

    val trace = {
      Using(Source.fromFile(traceFileName))(_.getLines().mkString("\n"))
        .map(JsonParser.parse)
        .recover { err =>
          System.err.println(s"An error occured: could not read trace ; ${err.getMessage}")
          exit(-1)
        }.get
    }

    val srcFiles = {
      srcFilesNames.map { filename =>
        filename ->
          Using(Source.fromFile(filename))(_.getLines().mkString("\n"))
            .recover { err =>
              System.err.println(s"An error occured: could not read at least 1 source file: ${err.getMessage}")
              exit(-1)
            }.get
      }
    }

    Parser.parse(srcFiles) match
      case Parser.ParsingFailure(problems) => {
        System.err.println("Error(s) occured:")
        for prob <- problems do {
          System.err.println(prob.getMessage)
        }
        exit(-1)
      }
      case ParsingSuccess(class2File, pluggableLines) => {
        Using(new PrintWriter("./jumbotracer-transformed/trace/trace.html")) { writer =>
          writer.println(buildHtml(trace, class2File, pluggableLines))
        }
      }
  }

  private def buildHtml(
                         traceElements: Seq[TraceElement],
                         class2Files: Map[ClassName, FileName],
                         pluggableLines: Map[FileName, Seq[PluggableCodeLine]]
                       ): String = {

    var lastVisitedLine: (ClassName, LineNumber) = ("", -1)

    def buildRecursively(traceElement: DisplayableTraceElement, indentLevel: Int): DomContent = {
      traceElement match
        case LineVisited(className, lineNumber, subEvents) => {
          val addSpace = (className, lineNumber - 1) != lastVisitedLine
          lastVisitedLine = (className, lineNumber)
          var hideFlag = false
          div(
            div(
              (for {
                filename <- class2Files.get(className)
                plugLinesSeq <- pluggableLines.get(filename)
                plugLine <- plugLinesSeq.lift.apply(lineNumber - 1)
              } yield {
                hideFlag = plugLine.mustHide
                (("\u00A0" * indentLevel) ++
                  plugLine.plugged(readValues(subEvents), pluggedValueLengthLimit).trim ++ " ").padTo(codeLineMinWidth, '.') ++
                  s" ($filename:$lineNumber)"
              }).getOrElse(s"Visit line $lineNumber in class $className (missing in provided source files)")
            ).withStyle(
              (if addSpace then preSpacing else "") ++
                (if hideFlag then "display:none;" else "")
            ),
            buildAllRecursively(subEvents, indentLevel + 1)
          )
        }
        case MethodCalled(ownerClass, methodName, args, _, subEvents) =>
          details(
            summary(
              b(("\u00A0" * indentLevel) ++
                s"CALL $ownerClass::${refineMethodName(methodName)}(${args.map(refinedValue).mkString(",")})")
            ).withStyle("display: block;"),
            buildAllRecursively(subEvents, indentLevel + 3)
          )
        case Initialization(dateTime) =>
          div(s"INITIALIZATION AT ${formatTime(dateTime)}")
        case Termination(msg) =>
          div(s"TERMINATION: $msg")
            .withStyle(preSpacing)
    }

    def buildAllRecursively(traceElements: Seq[TraceElement], indentLevel: Int): DivTag = {
      val displayableElems = traceElements.flatMap {
        case dte: DisplayableTraceElement => Some(dte)
        case _ => None
      }
      div(displayableElems.map(buildRecursively(_, indentLevel)): _*)
    }

    "<!DOCTYPE html>" ++ "\n" ++
      html(
        header(
          title("JumboTrace")
        ),
        body(
          buildAllRecursively(traceElements, 0)
        )
      ).withStyle("font-family: Consolas;")
        .renderFormatted()
  }

  private def readValues(subEvents: Seq[TraceElement]): Map[Identifier, Value] = {
    subEvents.flatMap {
      case VarGet(varId, value) => Some(varId -> value)
      case ArrayElemGet(arrayId, idx, value) => Some(s"$arrayId[$idx]" -> value)
      case StaticFieldGet(owner, fieldName, value) => Some(s"$owner.$fieldName" -> value)
      case InstanceFieldGet(owner, fieldName, value) => Some(s"$owner.$fieldName" -> value)
      case _ => None
    }.groupBy(_._1)
      .map { (varId: Identifier, pairs: Seq[(Identifier, Value)]) =>
        varId -> pairs.map(_._2).toSet
      }.filter(_._2.size == 1) // exclude the variables for which multiple accesses yielded distinct values
      .map { (varId: Identifier, singletonVal: Set[Value]) =>
        varId -> singletonVal.head
      }
      .toMap
  }

  private def formatTime(dateTime: String): String = {
    dateTime.reverse.dropWhile(_ != '.').reverse.init // remove nanoseconds
      .replace("T", " at ")
  }

  private def exit(exitCode: Int): Nothing = {
    System.exit(exitCode)
    throw AssertionError("cannot happen since program exited")
  }

}
