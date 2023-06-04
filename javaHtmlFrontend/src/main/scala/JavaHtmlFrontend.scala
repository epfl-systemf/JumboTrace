package javaHtmlFrontend

import DisplayRefiner.{refineMethodName, refinedValueComplete, refinedValueShort}
import Parser.ParsingSuccess

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.{ClassOrInterfaceDeclaration, EnumDeclaration, VariableDeclarator}
import com.github.javaparser.ast.expr.{AssignExpr, NameExpr}
import j2html.TagCreator.*
import j2html.attributes.Attr
import j2html.tags.specialized.{DivTag, ScriptTag}
import j2html.tags.{ContainerTag, DomContent, Tag}
import j2html.{Config, TagCreator}
import traceElements.*

import java.io.{File, FileWriter, PrintWriter}
import java.util
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try, Using}

object JavaHtmlFrontend {

  private val pluggedValueLengthLimit = 8
  private val codeLineMinWidth = 80
  private val preSpacing = "margin-top: 5px;"

  private val methodCallIndent = 3
  private val lineVisitedIndent = 1

  private val lineVisitedDetailsPrefix = "line-visited-details"
  private val lineVisitedSummaryPrefix = "line-visited-summary"
  private val methodCallDetailsPrefix = "method-call-details"
  private val methodCallSummaryPrefix = "method-call-summary"

  private val setDetailsStateScript = {
    s"""
       |function expandAll(){
       |    document.body.querySelectorAll("details")
       |      .forEach((e) => {
       |         if (e.id.startsWith(\"$methodCallDetailsPrefix\")){
       |            e.setAttribute("open", "true");
       |         }
       |      });
       |}
       |
       |function collapseAll(){
       |    document.body.querySelectorAll("details")
       |      .forEach((e) => e.removeAttribute("open"));
       |}
       |
       |function highlight(nodeId, color){
       |    document.getElementById(nodeId).style.backgroundColor = color;
       |}
       |
       |function removeHighlighting(nodeId){
       |    document.getElementById(nodeId).style.backgroundColor = "";
       |}
       |""".stripMargin
  }

  private type DisplayableTraceElement = LineVisited | MethodCalled | Return | ReturnVoid | Initialization | Termination

  def main(args: Array[String]): Unit = {

    if (args.isEmpty || args.tail.exists(!_.endsWith(".java"))) {
      System.err.println("Usage:")
      System.err.println("first argument is the path to the trace")
      System.err.println("subsequent arguments are paths to .java source files")
      exit(-1)
    }

    val traceFileName = args.head
    val srcFilesNames = args.tail.toSeq

    generateHtml(traceFileName, srcFilesNames, "./jumbotracer-transformed/trace/trace.html")
  }

  def generateHtml(jsonTraceFilePath: String, srcFilesNames: Seq[String], outputFilePath: String): Unit = {

    // Load the trace from the trace file
    val trace = {
      Using(Source.fromFile(jsonTraceFilePath))(_.getLines().mkString("\n"))
        .map(JsonParser.parse)
        .recover { err =>
          System.err.println(s"An error occured: could not read trace ; ${err.getMessage}")
          exit(-1)
        }.get
    }

    // Load the source files
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

    // Try to parse the files and combine them with the trace to build the HTML page
    Parser.parse(srcFiles) match
      case Parser.ParsingFailure(problems) => {
        System.err.println("Error(s) occured:")
        for prob <- problems do {
          System.err.println(prob.getMessage)
        }
        exit(-1)
      }
      case ParsingSuccess(class2File, pluggableLines) => {
        Using(new PrintWriter(outputFilePath)) { writer =>
          writer.println(buildHtml(trace, class2File, pluggableLines))
        }.get
      }
  }

  private def buildHtml(
                         traceElements: Seq[TraceElement],
                         class2Files: Map[ClassName, FileName],
                         pluggableLines: Map[FileName, Seq[PluggableCodeLine]]
                       ): String = {

    var lastVisitedLine: (ClassName, LineNumber) = ("", -1)

    val idsGenerator = new AtomicInteger(0)

    def buildRecursively(traceElement: DisplayableTraceElement, currIndentLevel: Int, lastCallIndentLevel: Int): DomContent = {
      traceElement match
        case LineVisited(className, lineNumber, subEvents) => {
          val addSpace = (className, lineNumber - 1) != lastVisitedLine
          lastVisitedLine = (className, lineNumber)
          var hideFlag = false  // set to true when the node being built should be hidden (easier than not building it)
          val currIdIdx = idsGenerator.incrementAndGet()
          val summaryId = lineVisitedSummaryPrefix + currIdIdx
          val detailsId = lineVisitedDetailsPrefix + currIdIdx
          div(
            details(
              summary(
                (for {
                  filename <- class2Files.get(className)
                  plugLinesSeq <- pluggableLines.get(filename)
                  plugLine <- plugLinesSeq.lift.apply(lineNumber - 1)
                } yield {
                  hideFlag = plugLine.mustHide
                  (("\u00A0" * currIndentLevel) ++
                    plugLine.plugged(readValues(subEvents), pluggedValueLengthLimit).trim ++ " ").padTo(codeLineMinWidth, '.') ++
                    s" ($filename:$lineNumber)"
                }).getOrElse(s"Visit line $lineNumber in class $className (missing in provided source files)")
              ).withStyle("display: block;")
                .withId(summaryId),
              readWrites(traceElement, currIndentLevel + lineVisitedIndent).withStyle("color: blue;")
            ).withStyle(
              (if addSpace then preSpacing else "") ++
                (if hideFlag then "display:none;" else "")
            ).withId(detailsId)
              .attr("onmouseenter", s"highlight(\"$detailsId\", \"lightcyan\")")
              .attr("onmouseleave", s"removeHighlighting(\"$detailsId\")"),
            buildAllRecursively(subEvents, currIndentLevel + lineVisitedIndent, lastCallIndentLevel)
          )
        }
        case MethodCalled(ownerClass, methodName, args, _, subEvents) =>
          val idIdx = idsGenerator.incrementAndGet()
          val summaryId = methodCallSummaryPrefix + idIdx
          val detailsId = methodCallDetailsPrefix + idIdx
          details(
            summary(
              b(("\u00A0" * currIndentLevel) ++
                s"CALL $ownerClass::${refineMethodName(methodName)}(${args.map(refinedValueShort).mkString(",")})")
            ).withStyle("display: block;")
              .withId(summaryId),
            buildAllRecursively(subEvents, currIndentLevel + methodCallIndent, currIndentLevel)
          ).withId(detailsId)
            .attr("onmouseenter", s"highlight(\"$summaryId\", \"moccasin\")")
            .attr("onmouseleave", s"removeHighlighting(\"$summaryId\")")
        case Return(methodName, value) =>
          div(b(("\u00A0" * lastCallIndentLevel) ++ s"$methodName RETURNS ${refinedValueShort(value)}"))
        case ReturnVoid(methodName) =>
          div(b(("\u00A0" * lastCallIndentLevel) ++ s"$methodName RETURNS void"))
        case Initialization(dateTime) =>
          div(s"INITIALIZATION AT ${formatTime(dateTime)}")
        case Termination(msg) =>
          div(s"TERMINATION: $msg")
            .withStyle(preSpacing)
    }

    def buildAllRecursively(traceElements: Seq[TraceElement], indentLevel: Int, lastCallIndentLevel: Int): DivTag = {
      val displayableElems = traceElements.flatMap {
        case dte: DisplayableTraceElement => Some(dte)
        case _ => None
      }
      div(displayableElems.map(buildRecursively(_, indentLevel, lastCallIndentLevel)): _*)
    }

    "<!DOCTYPE html>" ++ "\n" ++
      html(
        header(
          title("JumboTrace")
        ),
        body(
          div("Click on a line to expand").withStyle("font-family: Arial;"),
          br(),
          div(
            button("Expand all").attr("onClick", "expandAll()"),
            button("Collapse all").attr("onClick", "collapseAll()"),
          ),
          br(),
          buildAllRecursively(traceElements, 0, 0)
        ),
        script(setDetailsStateScript)
      ).withStyle("font-family: Consolas;")
        .renderFormatted()
  }

  /**
   * The information pane below a visited line, containing info about variables read and updated
   */
  private def readWrites(traceElement: TraceElement, indentLevel: Int): DivTag = {
    val reads = firstReads(traceElement)
    val writes = lastWrites(traceElement)
    val descr = (
      (reads.keys ++ writes.keys).toSeq
        .flatMap { key =>
          def rd = refinedValueComplete(reads.apply(key))

          def wr = refinedValueComplete(writes.apply(key))

          if (reads.contains(key) && writes.contains(key)) {
            Some(s"> $key: $rd -> $wr")
          } else if (reads.contains(key)) {
            Some(s"> $key: $rd")
          } else if (writes.contains(key)) {
            Some(s"> $key := $wr")
          } else None
        }
      )
    div(descr.map(d => div(i(("\u00A0" * indentLevel) ++ d))): _*)
  }

  /**
   * Retrieve the values that can be safely displayed next to variables in a the code line
   * 
   * (safely meaning that there is no risk of confusion because all the reads of the value
   * gave the same result)
   */
  private def readValues(subEvents: Seq[TraceElement]): Map[Identifier, Value] = {
    subEvents.flatMap {
      case VarGet(varId, value) => Some(varId -> value)
      case ArrayElemGet(array, idx, value) => Some(s"${array.shortDescr}[$idx]" -> value)
      case StaticFieldGet(owner, fieldName, value) => Some(s"$owner.$fieldName" -> value)
      case InstanceFieldGet(owner, fieldName, value) => Some(s"${owner.shortDescr}.$fieldName" -> value)
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

  /**
   * For each value read in that `TraceElement`, its value the first time the line reads it
   */
  private def firstReads(traceElement: TraceElement): Map[String, Value] = {
    val reads = mutable.Map.empty[String, Value]
    for (queryDescr, value) <- orderedReads(traceElement) do {
      if (!reads.contains(queryDescr)) {
        reads(queryDescr) = value
      }
    }
    reads.toMap
  }

  /**
   * For each value written in that `TraceElement`, the last value the line wrote to it
   */
  private def lastWrites(traceElement: TraceElement): Map[String, Value] = {
    val writes = mutable.Map.empty[String, Value]
    for (wrDescr, value) <- orderedWrites(traceElement) do {
      writes(wrDescr) = value
    }
    writes.toMap
  }

  /**
   * All the reads performed during this `TraceElement`, in chronological order
   */
  private def orderedReads(traceElement: TraceElement): Seq[(String, Value)] = {
    traceElement match
      case LineVisited(_, _, subEvents) =>
        subEvents.flatMap(orderedReads)
      case VarGet(varId, value) => Seq(varId -> value)
      case ArrayElemGet(array, idx, value) => Seq(s"${array.shortDescr}[$idx]" -> value)
      case StaticFieldGet(owner, fieldName, value) => Seq(s"$owner.$fieldName" -> value)
      case InstanceFieldGet(owner, fieldName, value) => Seq(s"${owner.shortDescr}.$fieldName" -> value)
      case _ => Seq.empty
  }

  /**
   * All the writes performed during this `TraceElement`, in chronological order
   */
  private def orderedWrites(traceElement: TraceElement): Seq[(String, Value)] = {
    traceElement match
      case LineVisited(_, _, subEvents) =>
        subEvents.flatMap(orderedWrites)
      case VarSet(varId, value) => Seq(varId -> value)
      case ArrayElemSet(array, idx, value) => Seq(s"${array.shortDescr}[$idx]" -> value)
      case StaticFieldSet(owner, fieldName, value) => Seq(s"$owner.$fieldName" -> value)
      case InstanceFieldSet(owner, fieldName, value) => Seq(s"${owner.shortDescr}.$fieldName" -> value)
      case _ => Seq.empty
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
