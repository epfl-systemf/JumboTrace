package instrumenter

import org.objectweb.asm.{ClassReader, ClassWriter}

import java.io.{FileReader, FileWriter}
import java.nio.file.{Files, Paths}
import scala.util.Using

object Instrumenter {

  private val logArg = "-log"

  def main(args: Array[String]): Unit = {

    if (!(1 <= args.length && args.length <= 2) || (args.length == 2 && args(1) != logArg)){
      System.err.println("Usage:")
      System.err.println("first argument is the name of the main class (required)")
      System.err.println(s"second argument should either be \"$logArg\" or be absent; indicates whether exploration and transformation should be logged")
      System.exit(-1)
    }

    val logger: String => Unit = if args.length >= 2 && args(1) == logArg then println else _ => ()

    val mainClass = ClassName(args(0))

    performInstrumentation(logger, mainClass)
  }

  def performInstrumentation(logger: String => Unit, mainClass: ClassName): Unit = {
    
    val classes = allClassesInCurrDir()
    if (!classes.contains(mainClass)){
      logger(s"Main class $mainClass not found")
    }
    
    val classesSet = classes.toSet
    for (className <- classes) do {

      val inputPath = Paths.get(".").resolve(s"$className.class")
      val outputPath = Paths.get(".").resolve(Config.config.transformedClassesDirName).resolve(s"$className.class")

      val inputBytes = Files.readAllBytes(inputPath)

      val classTableB = new ClassTable.Builder(className, isMainClass = (className == mainClass))
      val classExplorer = new ClassExplorer(classTableB, logger)

      val explorationReader = new ClassReader(inputBytes)
      explorationReader.accept(classExplorer, ClassReader.EXPAND_FRAMES)
      val classTable = classTableB.built

      val transformationReader = new ClassReader(inputBytes)
      val transformationWriter = new ClassWriter(transformationReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)

      transformationReader.accept(new ClassTransformer(transformationWriter, classTable, logger, classesSet), ClassReader.EXPAND_FRAMES)

      val outFile = outputPath.toFile
      outFile.getParentFile.mkdirs()
      outFile.createNewFile()
      Files.write(outputPath, transformationWriter.toByteArray)
    }
  }

  private def allClassesInCurrDir(): Seq[ClassName] = {
    val currDir = Paths.get(".").toFile
    for sub <- currDir.listFiles().toSeq if sub.isFile && sub.getName.endsWith(".class") yield {
      ClassName(sub.getName.takeWhile(_ != '.'))
    }
  }

}
