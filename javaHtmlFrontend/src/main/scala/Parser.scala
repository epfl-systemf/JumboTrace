package javaHtmlFrontend

import com.github.javaparser.ParserConfiguration.LanguageLevel
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.{ClassOrInterfaceDeclaration, EnumDeclaration, VariableDeclarator}
import com.github.javaparser.ast.expr.{AssignExpr, FieldAccessExpr, NameExpr}
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.{JavaParser, ParserConfiguration, Position, Problem}

import scala.io.Source
import scala.jdk.CollectionConverters.*

object Parser {

  sealed trait ParsingResult

  final case class ParsingSuccess(
                                   class2File: Map[ClassName, FileName],
                                   pluggableLines: Map[FileName, Seq[PluggableCodeLine]]
                                 ) extends ParsingResult

  final case class ParsingFailure(problems: Seq[Problem]) extends ParsingResult

  private val languageLevel = LanguageLevel.JAVA_17

  def parse(files: Seq[(FileName, Code)]): ParsingResult = {

    type TopLevelDecl = ClassOrInterfaceDeclaration | EnumDeclaration

    val config = new ParserConfiguration().setLanguageLevel(languageLevel)
    val parser = new JavaParser(config)

    val parsedFiles = files.map((fileName, code) => (fileName, code, parser.parse(code)))
    if (parsedFiles.forall(_._3.isSuccessful)) {
      val class2FileB = Map.newBuilder[ClassName, FileName]
      val pluggableLinesB = Map.newBuilder[FileName, Seq[PluggableCodeLine]]
      for (fileName, code, parseRes) <- parsedFiles do {
        val cu = parseRes.getResult.get()
        class2FileB.addAll(
          (
            cu.findAll(classOf[ClassOrInterfaceDeclaration]).asScala ++
              cu.findAll(classOf[EnumDeclaration]).asScala
            ).map(_.getName.asString() -> fileName)
        )
        val codeLines = Source.fromString(code).getLines().toSeq
        pluggableLinesB.addOne(fileName, sourceToPluggableLines(codeLines, cu))
      }
      ParsingSuccess(class2FileB.result(), pluggableLinesB.result())
    } else {
      val problems = parsedFiles.flatMap(_._3.getProblems.asScala)
      ParsingFailure(problems)
    }

  }

  private def sourceToPluggableLines(sourceLines: Seq[Code], cu: CompilationUnit): Seq[PluggableCodeLine] = {
    val endsByNames: Map[Int, Seq[(Identifier, ColIdx)]] =
      (cu.findAll(classOf[NameExpr]).asScala ++
        cu.findAll(classOf[VariableDeclarator]).asScala ++
        cu.findAll(classOf[FieldAccessExpr]).asScala
        ).toSeq
        .filter(_.getEnd.isPresent)
        .groupBy(_.getEnd.get().line)
        .map { (lineIdx, exprs) =>
          lineIdx -> exprs.map {
            case accessExpr: (NameExpr | FieldAccessExpr) =>
              accessExpr.getName.asString() -> accessExpr.getEnd.get().column
            case varDecl: VariableDeclarator => {
              val varName = varDecl.getName.asString()
              val varNameEnd = varDecl.getBegin.get().column + varName.length - 1
              varName -> varNameEnd
            }
          }
        }
    sourceLines.zipWithIndex.map { (srcLine, lineIdx) =>
      PluggableCodeLine(srcLine, endsByNames.getOrElse(lineIdx + 1, Seq.empty))
    }
  }

}
