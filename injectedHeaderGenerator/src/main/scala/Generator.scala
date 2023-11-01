import com.github.javaparser.JavaParser
import com.github.javaparser.StaticJavaParser.{parseClassOrInterfaceType, parseSimpleName, parseStatement}
import com.github.javaparser.ast.`type`.PrimitiveType.*
import com.github.javaparser.ast.`type`.{ClassOrInterfaceType, PrimitiveType, Type, VoidType}
import com.github.javaparser.ast.body.{ClassOrInterfaceDeclaration, FieldDeclaration, MethodDeclaration, Parameter, VariableDeclarator}
import com.github.javaparser.ast.expr.{MethodCallExpr, Name, NameExpr, ObjectCreationExpr}
import com.github.javaparser.ast.stmt.{BlockStmt, ExplicitConstructorInvocationStmt, ThrowStmt}
import com.github.javaparser.ast.{Modifier, NodeList}
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration

import java.io.FileWriter
import scala.util.Using

// TODO check performance of generated code, maybe invokeinterface slows things down

object Generator {

  private val packageName = "___jbt$instrumentation___"
  private val interfaceName = "___jbt$trace_container___"
  private val interfaceFullQualifiedName = packageName ++ "." ++ interfaceName

  private def modifPub = new NodeList(Modifier.publicModifier())
  private def modifPubStatic = new NodeList(Modifier.publicModifier(), Modifier.staticModifier())
  private def modifPrivateStatic = new NodeList(Modifier.privateModifier(), Modifier.staticModifier())

  private def objectType = parseClassOrInterfaceType("Object")
  private def stringType = parseClassOrInterfaceType("String")

  private val topmostTypes = Seq(
    intType(),
    byteType(),
    charType(),
    longType(),
    booleanType(),
    doubleType(),
    floatType(),
    shortType(),
    objectType
  )

  private val getInstanceMethodName = "getInstance"

  private def getInstanceMethod: MethodDeclaration = {
    new MethodDeclaration(modifPrivateStatic, parseClassOrInterfaceType(interfaceName), getInstanceMethodName)
      .setBody(
        new BlockStmt().addStatement(parseStatement("throw new AssertionError();"))
      )
  }

  private def instanceConstant: FieldDeclaration = {
    new FieldDeclaration(modifPubStatic, new VariableDeclarator(parseClassOrInterfaceType(interfaceName), "INSTANCE",
      MethodCallExpr(getInstanceMethodName)))
  }

  private val allTypesMethods = Seq[Type => MethodDeclaration](
    t => new MethodDeclaration(modifPub, "varDeclEvent_" + t, VoidType(), new NodeList(
      new Parameter(stringType, "identifier")
    )),
    t => new MethodDeclaration(modifPub, "varDefEvent_" + t, VoidType(), new NodeList(
      new Parameter(stringType, "identifier"), new Parameter(t, "value")
    ))
  )

  def generate(): Unit = {
    Using(new FileWriter(s"./injected/$interfaceName.java")){ writer =>
      val interface = new ClassOrInterfaceDeclaration(modifPub, true, interfaceName)
      interface.addMember(instanceConstant)
      interface.addMember(getInstanceMethod)
      for {
        pluggableDecl <- allTypesMethods
        tp <- topmostTypes
      }{
        interface.addMember(pluggableDecl(tp).setBody(null))
      }
      writer.write(interface.toString())
    }
  }

  def main(args: Array[String]): Unit = generate()

}
