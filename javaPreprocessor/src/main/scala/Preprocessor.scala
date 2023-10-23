package javaPreprocessor

import com.github.javaparser
import com.github.javaparser.ParserConfiguration.LanguageLevel
import com.github.javaparser.ast.`type`.PrimitiveType
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.GenericVisitor
import com.github.javaparser.ast.{CompilationUnit, ImportDeclaration, Node, NodeList}
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter
import com.github.javaparser.{JavaParser, ParserConfiguration, Position, Problem}

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import scala.io.Source
import scala.util.Using

final class Preprocessor {

  type FileName = String
  type Code = String
  type LineIdx = Int
  type ColIdx = Int

  private val languageLevel = LanguageLevel.JAVA_17

  private val varNameIdx = new AtomicLong(0)
  private def nextVarName(constructDescr: String): String = {
    s"___$constructDescr" + varNameIdx.incrementAndGet()
  }

  def preprocess(originalFileName: FileName, newFileName: FileName, mappingFileName: FileName): Seq[Problem] = {
    Using(Source.fromFile(originalFileName)) { bufSrc =>
      val config = new ParserConfiguration().setLanguageLevel(languageLevel)
      val parser = new JavaParser(config)
      val result = parser.parse(bufSrc.reader())
      if (result.isSuccessful) {
        val cu = result.getResult.get()
        preprocess(cu)

        ??? // TODO

      } else {
        val seqB = Seq.newBuilder[Problem]
        result.getProblems.forEach(seqB.addOne)
        seqB.result()
      }
    }.get
  }

  def preprocess(cu: CompilationUnit): Unit = {
    handleWhileLoops(cu)
    handleDoWhileLoops(cu)
    handleIf(cu)
    handleForLoops(cu)  // must be performed after handleWhileLoops
  }

  private def handleForLoops(cu: CompilationUnit): Unit = {
    cu.findAll(classOf[ForStmt]).forEach { forStmt =>
      val inStats = new NodeList[Statement]()
      inStats.add(forStmt.getBody)
      inStats.addAll(forStmt.getUpdate.stream().map(new ExpressionStmt(_)).toList)
      val outStats = new NodeList[Statement]()
      val condVarName = nextVarName("for_cond")
      outStats.add(new ExpressionStmt(new VariableDeclarationExpr(PrimitiveType.booleanType(), condVarName)))
      outStats.addAll(forStmt.getInitialization.stream().map(new ExpressionStmt(_)).toList)
      outStats.add(new WhileStmt(
        new AssignExpr(new NameExpr(condVarName), forStmt.getCompare.orElse(new BooleanLiteralExpr(true)), AssignExpr.Operator.ASSIGN),
        new BlockStmt(inStats)
      ))
      forStmt.replace(new BlockStmt(outStats))
    }
  }

  private def handleWhileLoops(cu: CompilationUnit): Unit = {
    cu.findAll(classOf[WhileStmt]).forEach { whileStmt =>
      val outStats = new NodeList[Statement]()
      val condVarName = nextVarName("while_cond")
      outStats.add(new ExpressionStmt(new VariableDeclarationExpr(PrimitiveType.booleanType(), condVarName)))
      outStats.add(new WhileStmt(
        new AssignExpr(new NameExpr(condVarName), whileStmt.getCondition, AssignExpr.Operator.ASSIGN),
        whileStmt.getBody
      ))
      whileStmt.replace(new BlockStmt(outStats))
    }
  }

  private def handleDoWhileLoops(cu: CompilationUnit): Unit = {
    cu.findAll(classOf[DoStmt]).forEach { doStmt =>
      val outStats = new NodeList[Statement]()
      val condVarName = nextVarName("do_while_cond")
      outStats.add(new ExpressionStmt(new VariableDeclarationExpr(PrimitiveType.booleanType(), condVarName)))
      outStats.add(new DoStmt(
        doStmt.getBody,
        new AssignExpr(new NameExpr(condVarName), doStmt.getCondition, AssignExpr.Operator.ASSIGN)
      ))
      doStmt.replace(new BlockStmt(outStats))
    }
  }

  private def handleIf(cu: CompilationUnit): Unit = {
    cu.findAll(classOf[IfStmt]).forEach { ifStmt =>
      val outStats = new NodeList[Statement]()
      val condVarName = nextVarName("if_cond")
      outStats.add(new ExpressionStmt(new VariableDeclarationExpr(PrimitiveType.booleanType(), condVarName)))
      outStats.add(new IfStmt(
        new AssignExpr(new NameExpr(condVarName), ifStmt.getCondition, AssignExpr.Operator.ASSIGN),
        ifStmt.getThenStmt,
        if ifStmt.getElseStmt.isPresent then ifStmt.getElseStmt.get() else null
      ))
      ifStmt.replace(new BlockStmt(outStats))
    }
  }

  // TODO handle switch

}

@main def tempTestMain(): Unit = {
  val input =
    """
      |class Foo {
      |
      |   void foo(int x, int y, int z){
      |      for (int i = 0; i < 10; i++){
      |         System.out.println(i);
      |      }
      |      if (x == y){
      |         x = z;
      |      }
      |      while (z < y){
      |         z += 2;
      |      }
      |      do {
      |         if (x % 2 == 0){
      |            y += 1;
      |         } else {
      |            z += 2;
      |         }
      |      } while (z+y*x % 3 != 0);
      |      if (x == 1){
      |         y = 2;
      |      } else if (y == 15){
      |         y = 11;
      |      } else if (z == 42){
      |         y = x;
      |      } else {
      |         y = z-x;
      |      }
      |   }
      |}
      |""".stripMargin
  end input
  val parseRes = (new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_17))).parse(input)
  parseRes.getProblems.forEach(System.err.println(_))
  assert(parseRes.isSuccessful)
  val cu = parseRes.getResult.get()
  println(cu)
  println("\n ---------------------------- \n")
  (new Preprocessor()).preprocess(cu)
  println(cu)
//  cu.findAll(classOf[Node]).forEach { node =>
//    println(s"${node.getClass.getSimpleName} ${node.getRange}")
//  }
}
