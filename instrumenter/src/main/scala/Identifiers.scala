package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.Type

final case class ClassName(name: String) extends AnyVal

final case class MethodName(name: String) extends AnyVal

final case class MethodDescriptor(args: Seq[TypeDescriptor], ret: TypeDescriptor) {
  override def toString: String = args.mkString("(", "", ")") ++ ret.toString
}

object MethodDescriptor {

  def parse(str: String): Option[MethodDescriptor] = {
    if (str.startsWith("(") && str.count(_ == '(') == 1 && str.count(_ == ')') == 1) {
      val Array(argsStr, retStr) = str.tail.split(')')
      val descriptorStartIndices = argsStr.indices.filter(idx => argsStr(idx) == '[' || argsStr(idx).isUpper)
      val argsDescr =
        if descriptorStartIndices.isEmpty then Seq.empty[Option[TypeDescriptor]]
        else (
          (descriptorStartIndices :+ argsStr.length)
            .sliding(2)
            .map(sl => argsStr.slice(sl(0), sl(1)))
            .map(TypeDescriptor.parse)
            .toSeq
          )
      val retDescr = TypeDescriptor.parse(retStr)
      if (argsDescr.forall(_.isDefined) && retDescr.isDefined) {
        Some(MethodDescriptor(argsDescr.map(_.get), retDescr.get))
      } else None
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

}
