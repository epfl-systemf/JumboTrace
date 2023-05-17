package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.Type

import scala.collection.mutable.ListBuffer

final case class ClassName(name: String) extends AnyVal {
  override def toString: String = name
}

final case class MethodName(name: String) extends AnyVal {
  override def toString: String = name
}

final case class MethodDescriptor(args: Seq[TypeDescriptor], ret: TypeDescriptor) {
  override def toString: String = args.mkString("(", "", ")") ++ ret.toString
}

object MethodDescriptor {
  private val uniqueCharArgDesciptors = Set('Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D')
  
  extension(args: Seq[TypeDescriptor]) def ==>(ret: TypeDescriptor): MethodDescriptor = {
    MethodDescriptor(args, ret)
  }

  def parse(str: String): Option[MethodDescriptor] = {  // TODO tests
    if (str.startsWith("(") && str.count(_ == '(') == 1 && str.count(_ == ')') == 1) {
      val Array(argsStr, retStr) = str.tail.split(')')
      val argsDescriptors = ListBuffer.empty[TypeDescriptor]
      var argsStrIter = argsStr.iterator
      var isArray = false
      while (argsStrIter.hasNext){
        val curr = argsStrIter.next()
        if (uniqueCharArgDesciptors.contains(curr) && isArray){
          argsDescriptors.addOne(TypeDescriptor.Array(TypeDescriptor.parse(curr.toString).get))
          isArray = false
        } else if (uniqueCharArgDesciptors.contains(curr)){
          argsDescriptors.addOne(TypeDescriptor.parse(curr.toString).get)
        } else if (curr == '['){
          isArray = true
        } else if (curr == 'L'){
          val (className, rem) = argsStrIter.span(_ != ';')
          if (rem.isEmpty){
            return None
          }
          argsDescriptors.addOne(TypeDescriptor.parse(s"L$className;").get)
          argsStrIter = rem.iterator
          argsStrIter.next() // drop ';'
        } else {
          return None
        }
      }
      TypeDescriptor.parse(retStr).map(MethodDescriptor(argsDescriptors.toSeq, _))
    } else None
  }

}

enum TypeDescriptor(str: String) {
  case Boolean extends TypeDescriptor("Z")
  case Char extends TypeDescriptor("C")
  case Byte extends TypeDescriptor("B")
  case Short extends TypeDescriptor("S")
  case Int extends TypeDescriptor("I")
  case Float extends TypeDescriptor("F")
  case Long extends TypeDescriptor("J")
  case Double extends TypeDescriptor("D")
  case Void extends TypeDescriptor("V")
  case Array(elemDescr: TypeDescriptor) extends TypeDescriptor("[" + elemDescr)
  case Class(prefixes: Seq[String], className: String) extends TypeDescriptor((prefixes :+ className).mkString("L", "/", ";"))

  override def toString: String = str
}

object TypeDescriptor {

  def parse(str: String): Option[TypeDescriptor] = {
    str match
      case "Z" => Some(Boolean)
      case "C" => Some(Char)
      case "B" => Some(Byte)
      case "S" => Some(Short)
      case "I" => Some(Int)
      case "F" => Some(Float)
      case "J" => Some(Long)
      case "D" => Some(Double)
      case "V" => Some(Void)
      case arrayDescr if arrayDescr.nonEmpty && arrayDescr.startsWith("[") =>
        parse(arrayDescr.tail).map(Array.apply)
      case classDescr if classDescr.length >= 3 && classDescr.startsWith("L") && classDescr.endsWith(";") => {
        val parts = classDescr.slice(1, classDescr.length - 1).split('/')
        Some(Class(parts.init, parts.last))
      }
      case _ => None
  }
  
  val String: TypeDescriptor = Class(Seq("java", "lang"), "String")
  val Object: TypeDescriptor = Class(Seq("java", "lang"), "Object")

}
