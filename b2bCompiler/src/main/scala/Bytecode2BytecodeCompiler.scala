package b2bCompiler

import java.nio.file.{Files, Path}

object Bytecode2BytecodeCompiler {

  def main(args: Array[String]): Unit = {
    val filepath = args.head
    val pipeline = new BytecodeParser().andThen(new BytecodePrinter())
    val bytes = Files.readAllBytes(Path.of(filepath))
    val bytecodeStr = pipeline.run(bytes)
    println(bytecodeStr)
  }

}
