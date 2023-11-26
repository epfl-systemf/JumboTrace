package b2bCompiler

import java.nio.file.{Files, Path}

object Main {

  def main(args: Array[String]): Unit = {
    val filepath = args.head
    val pipeline =
      new BytecodeParser()
        .andThen(new TablesCreator())
        .andThen(new AbsIntPreparator())
        .andThen {
          _.map { methodInfo =>
            val (uid, code, _) = methodInfo
            (uid, code)
          }
        }
        .andThen(new BytecodePrinter())
    end pipeline
    val bytes = Files.readAllBytes(Path.of(filepath))
    val bytecodeStr = pipeline.run(bytes)
    println(bytecodeStr)
  }

}
