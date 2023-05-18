package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.Type

import scala.collection.mutable.ListBuffer

enum TypeDescriptor(str: String, asmType: Option[Type]) {
  case Boolean extends TypeDescriptor("Z", Some(Type.BOOLEAN_TYPE))
  case Char extends TypeDescriptor("C", Some(Type.CHAR_TYPE))
  case Byte extends TypeDescriptor("B", Some(Type.BYTE_TYPE))
  case Short extends TypeDescriptor("S", Some(Type.SHORT_TYPE))
  case Int extends TypeDescriptor("I", Some(Type.INT_TYPE))
  case Float extends TypeDescriptor("F", Some(Type.FLOAT_TYPE))
  case Long extends TypeDescriptor("J", Some(Type.LONG_TYPE))
  case Double extends TypeDescriptor("D", Some(Type.DOUBLE_TYPE))
  case Void extends TypeDescriptor("V", Some(Type.VOID_TYPE))
  case Array(elemDescr: TypeDescriptor) extends TypeDescriptor("[" + elemDescr, None)
  case Class(prefixes: Seq[String], className: String) extends TypeDescriptor((prefixes :+ className).mkString("L", "/", ";"), None)

  /**
   * @param intOpcode the opcode to use for an integer
   * @param refOpcode the opcode that performs the same task as `intOpcode` but on a reference (class instance, array)
   * @return the opcode performing the same task as both arguments but for the type of `this`
   */
  def getOpcode(intOpcode: Int, refOpcode: => Int): Int = {
    import org.objectweb.asm.Opcodes.*
    val acceptedIntCodes = Set(ILOAD, ISTORE, IALOAD, IASTORE, IADD, ISUB, IMUL, IDIV, IREM, INEG, ISHL, ISHR, IUSHR, IAND, IOR, IXOR, IRETURN)
    require(acceptedIntCodes.contains(intOpcode), s"cannot convert opcode $intOpcode")
    asmType.map(_.getOpcode(intOpcode)).getOrElse(refOpcode)
  }

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
        val parts = classDescr.slice(1, classDescr.length - 1).split('/').toSeq
        Some(Class(parts.init, parts.last))
      }
      case _ => None
  }

  def isDoubleWordType(td: TypeDescriptor): Boolean = {
    td match
      case TypeDescriptor.Double | TypeDescriptor.Long => true
      case _ => false
  }

  val String: TypeDescriptor = Class(Seq("java", "lang"), "String")
  val Object: TypeDescriptor = Class(Seq("java", "lang"), "Object")
  val Throwable: TypeDescriptor = Class(Seq("java", "lang"), "Throwable")

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

