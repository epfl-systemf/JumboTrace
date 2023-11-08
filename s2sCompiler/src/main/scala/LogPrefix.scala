package s2sCompiler

import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.visitor.{GenericVisitor, VoidVisitor}

trait LogPrefix(logStats: Statement*) extends Statement {

  abstract override def accept[R, A](v: GenericVisitor[R, A], arg: A): R = {
    logStats.foreach(_.accept(v, arg))
    super.accept(v, arg)
  }

  abstract override def accept[A](v: VoidVisitor[A], arg: A): Unit = {
    logStats.foreach(_.accept(v, arg))
    super.accept(v, arg)
  }

}
