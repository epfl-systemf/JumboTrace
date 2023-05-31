package instrumenter

import instrumenter.TypeDescriptor.Boolean
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

  def parse(str: String): TypeDescriptor = {
    str match
      case "Z" => Boolean
      case "C" => Char
      case "B" => Byte
      case "S" => Short
      case "I" => Int
      case "F" => Float
      case "J" => Long
      case "D" => Double
      case "V" => Void
      case arrayDescr if arrayDescr.nonEmpty && arrayDescr.startsWith("[") =>
        Array(parse(arrayDescr.tail))
      case classDescr if classDescr.length >= 3 && classDescr.startsWith("L") && classDescr.endsWith(";") => {
        val parts = classDescr.slice(1, classDescr.length - 1).split('/').toSeq
        Class(parts.init, parts.last)
      }
      case _ => assert(false, s"could not parse type descriptor: $str")
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

  import instrumenter.TypeDescriptor as TD

  extension(args: Seq[TypeDescriptor]) def ==>(ret: TypeDescriptor): MethodDescriptor = {
    MethodDescriptor(args, ret)
  }

  def parse(str: String): MethodDescriptor = {
    require((str.startsWith("(") && str.count(_ == '(') == 1 && str.count(_ == ')') == 1), s"could not parse method descriptor: $str")

    // To be called after detecting a L
    def consumeClass(charsIter: Iterator[Char]): (TypeDescriptor, Iterator[Char]) = {
      val (objTDIter, rem) =  charsIter.span(_ != ';')
      assert(rem.hasNext, s"expected ';', could not parse $str")
      rem.next() // drop ';'
      (TypeDescriptor.parse("L" ++ objTDIter.mkString ++ ";"), rem)
    }

    def consumeArg(charsIter: Iterator[Char]): (TypeDescriptor, Iterator[Char]) = {
      require(charsIter.nonEmpty)
      charsIter.next() match {
        case 'Z' => (TD.Boolean, charsIter)
        case 'C' => (TD.Char, charsIter)
        case 'B' => (TD.Byte, charsIter)
        case 'S' => (TD.Short, charsIter)
        case 'I' => (TD.Int, charsIter)
        case 'F' => (TD.Float, charsIter)
        case 'J' => (TD.Long, charsIter)
        case 'D' => (TD.Double, charsIter)
        case 'V' => assert(false, s"unexpected V (void?) in arguments type list ; parse argument was $str")
        case 'L' => consumeClass(charsIter)
        case '[' => {
          val (sub, rem) = consumeArg(charsIter)
          (TD.Array(sub), rem)
        }
        case _ => assert(false, s"could not parse method descriptor: $str")
      }
    }

    val Array(args, ret) = str.tail.split(')')
    val argsBuffer = ListBuffer.empty[TypeDescriptor]
    var argsCharsIter = args.iterator
    while (argsCharsIter.hasNext) {
      val (parsedArg, rem) = consumeArg(argsCharsIter)
      argsBuffer.addOne(parsedArg)
      argsCharsIter = rem
    }
    argsBuffer.toSeq ==> TD.parse(ret)
  }

}

