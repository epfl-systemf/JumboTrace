package javaHtmlFrontend

import ClassIndex.*

import com.github.javaparser.ParserConfiguration.LanguageLevel
import com.github.javaparser.ast.body.{ClassOrInterfaceDeclaration, EnumDeclaration, VariableDeclarator}
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.{JavaParser, ParserConfiguration, Position, Problem}

import java.io.File
import java.util.Optional
import scala.jdk.CollectionConverters.*
import scala.util.Try

final case class ClassIndex(className: String, srcFileName: String, identifierOccurrences: Seq[(VarId, Position)]){

  val varsByLine: Map[Int, Seq[(VarId, ColIdx)]] = {
    identifierOccurrences
      .groupBy(_._2.line)
      .map { (lineNumber, varIdsAndPos) =>
        lineNumber -> varIdsAndPos.map((vid, pos) => (vid, pos.column))
      }
  }

}

object ClassIndex {

  type VarId = String
  type ColIdx = Int

  private type TopLevelDecl = ClassOrInterfaceDeclaration | EnumDeclaration
  private type NamedExpr = NameExpr | VariableDeclarator

  private val languageLevel = LanguageLevel.JAVA_17

  def buildIndices(filename: String, code: String): Either[Seq[Problem], Seq[ClassIndex]] = {

    def optionalEntry(varName: String, optPos: Optional[Position]): Option[(String, Position)] = {
      if (optPos.isPresent){
        Some(varName -> optPos.get())
      } else None
    }

    def buildIndex(decl: TopLevelDecl): ClassIndex = {
      val indexedIdentifiers = (
        (decl.findAll(classOf[NameExpr]).asScala.toSeq ++ decl.findAll(classOf[VariableDeclarator]).asScala.toSeq: Seq[NamedExpr])
          .flatMap {
            case varDecl: VariableDeclarator =>
              optionalEntry(varDecl.getName.asString(), varDecl.getBegin)
            case nameExpr: NameExpr if nameExpr.hasRange =>
              optionalEntry(nameExpr.getName.asString(), nameExpr.getBegin)
            case _: NamedExpr => None
          }
        )
      val className = decl.getName.asString()
      ClassIndex(className, filename, indexedIdentifiers.sortBy(_._2))
    }

    val config = new ParserConfiguration().setLanguageLevel(languageLevel)
    val parseResult = new JavaParser(config).parse(code)
    Either.cond(parseResult.isSuccessful,
      {
        val cu = parseResult.getResult.get()
        val declarations: Seq[TopLevelDecl] = (
          cu.findAll(classOf[ClassOrInterfaceDeclaration]).asScala ++
            cu.findAll(classOf[EnumDeclaration]).asScala
          ).toSeq
        declarations.map(buildIndex)
      },
      parseResult.getProblems.asScala.toSeq
    )
  }

}
