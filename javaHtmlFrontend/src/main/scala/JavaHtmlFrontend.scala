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
import traceElements.*

import java.io.{File, FileWriter, PrintWriter}
import java.util
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try, Using}

object JavaHtmlFrontend {

  private type CodeLine = String

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
          Using(Source.fromFile(filename))(_.getLines().toSeq)
            .recover { err =>
              System.err.println(s"An error occured: could not read at least 1 source file: ${err.getMessage}")
              exit(-1)
            }.get
      }
    }

    val indices = srcFiles.flatMap { (filename, lines) =>
      ClassIndex.buildIndices(filename, lines.mkString("\n")) match
        case Left(problems) =>
          System.err.println("Error(s) occured:")
          problems.foreach {
            prob => System.err.println(prob.getMessage)
          }
          exit(-1)
        case Right(idcs) =>
          idcs.map { classIndex =>
            classIndex.className -> classIndex
          }
    }.toMap

    val linesConverter = new LinesConverter(indices, srcFiles.toMap)

    Using(new PrintWriter("./jumbotracer-transformed/trace/trace.html")) { writer =>
      writer.println(buildHtml(trace))
    }
  }

  private def buildHtml(traceElements: Seq[TraceElement]): String = {

    def buildRecursively(traceElement: TraceElement): DomContent = {
      traceElement match
        case LineVisited(className, lineNumber, subEvents) =>
          details(
            summary(s"VISIT line $lineNumber in class $className"),
            buildAllRecursively(subEvents)
              .withStyle("padding-left: 10px;")
          )
        case MethodCalled(ownerClass, methodName, args, _, subEvents) =>
          details(
            summary(s"CALL $ownerClass::$methodName(${args.mkString(",")})"),
            buildAllRecursively(subEvents)
              .withStyle("padding-left: 25px;")
          )
        case VarSet(varId, value) =>
          div(s"$varId := $value")
        case VarGet(varId, value) =>
          div(s"$varId == $value")
        case ArrayElemSet(arrayId, idx, value) =>
          div(s"$arrayId[$idx] := $value")
        case ArrayElemGet(arrayId, idx, value) =>
          div(s"$arrayId[$idx] == $value")
        case StaticFieldSet(owner, fieldName, value) =>
          div(s"$owner.$fieldName := $value")
        case StaticFieldGet(owner, fieldName, value) =>
          div(s"$owner.$fieldName == $value")
        case InstanceFieldSet(owner, fieldName, value) =>
          div(s"$owner.$fieldName := $value")
        case InstanceFieldGet(owner, fieldName, value) =>
          div(s"$owner.$fieldName == $value")
        case Return(methodName, value) =>
          div(s"RETURN $value FROM $methodName")
        case ReturnVoid(methodName) =>
          div(s"RETURN void FROM $methodName")
        case Initialization(dateTime) =>
          div(s"INITIALIZATION AT ${formatTime(dateTime)}")
        case Termination(msg) =>
          div(s"TERMINATION: $msg")
    }

    def buildAllRecursively(traceElements: Seq[TraceElement]): DivTag = {
      div(traceElements.map(buildRecursively): _*)
    }

    "<!DOCTYPE html>" ++ "\n" ++
      html(
        header(
          title("JumboTrace")
        ),
        body(
          buildAllRecursively(traceElements)
        )
      ).withStyle("font-family: Consolas;")
        .renderFormatted()
  }

  private def traverseTrace(trace: Seq[TraceElement], indentLevel: Int)(using linesConverter: LinesConverter): Unit = {

    def display(str: String): Unit = {
      println((" " * indentLevel) ++ str)
    }

    def subDisplay(str: String): Unit = display(" " ++ str)

    trace.foreach {
      case lineVisited@LineVisited(className, lineNumber, subEvents) =>
        display(linesConverter.convert(lineVisited) ++ s" ($className:$lineNumber)")
        traverseTrace(subEvents, indentLevel + 1)
      case Return(methodName, value) =>
        subDisplay(s"return $value from $methodName")
      case ReturnVoid(methodName) =>
        subDisplay(s"return void from $methodName")
      case MethodCalled(ownerClass, methodName, args, _, subEvents) =>
        subDisplay(s"call: $ownerClass::$methodName(${args.mkString(",")})")
        traverseTrace(subEvents, indentLevel + 2)
      case Initialization(dateTime) =>
        display(s"Initialization: ${formatTime(dateTime)}")
      case Termination(msg) =>
        display(s"Termination: $msg")
      case _ => ()
    }
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
