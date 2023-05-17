package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{ClassReader, ClassWriter}

import java.io.{FileReader, FileWriter}
import java.nio.file.{Files, Paths}
import scala.util.Using

object Main {

  def main(args: Array[String]): Unit = {

    if (args.length != 1){
      System.err.println("Usage: single argument is the name of the main class")
      System.exit(-1)
    }

    val mainClass = ClassName(args(0))

    for (className <- allClassesInCurrDir()) do {

      val inputPath = Paths.get(".").resolve(s"$className.class")
      val outputPath = Paths.get(".").resolve(Config.current.transformedClassesDirName).resolve(s"$className.class")

      val inputBytes = Files.readAllBytes(inputPath)

      val classTableB = new ClassTable.Builder(className, isMainClass = (className == mainClass))
      val classExplorer = new ClassExplorer(classTableB)

      val explorationReader = new ClassReader(inputBytes)
      explorationReader.accept(classExplorer, ClassReader.EXPAND_FRAMES)
      val classTable = classTableB.built

      println(classTable)

      val transformationReader = new ClassReader(inputBytes)
      val transformationWriter = new ClassWriter(transformationReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)

      transformationReader.accept(new ClassTransformer(transformationWriter, classTable), ClassReader.EXPAND_FRAMES)

      val outFile = outputPath.toFile
      outFile.getParentFile.mkdirs()
      outFile.createNewFile()
      Files.write(outputPath, transformationWriter.toByteArray)
    }
  }

  private def allClassesInCurrDir(): Seq[ClassName] = {
    val currDir = Paths.get(".").toFile
    for sub <- currDir.listFiles() if sub.isFile && sub.getName.endsWith(".class") yield {
      ClassName(sub.getName.takeWhile(_ != '.'))
    }
  }

}
