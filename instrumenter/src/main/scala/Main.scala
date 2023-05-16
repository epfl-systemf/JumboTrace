package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.{ClassReader, ClassWriter}

import java.io.{FileReader, FileWriter}
import java.nio.file.{Files, Paths}
import scala.util.Using

object Main {

  def main(args: Array[String]): Unit = {

    // TODO add inner classes to the array of classes

    var isMain = true // main class is given by the first argument

    for (className <- args.map(ClassName.apply)) do {

      val inputPath = Paths.get(".").resolve(s"$className.class")
      val outputPath = Paths.get(".").resolve(Config.current.transformedClassesDirName).resolve(s"$className.class")

      val inputBytes = Files.readAllBytes(inputPath)

      val classTableB = new ClassTable.Builder(className, isMain)
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

      isMain = false
    }
  }

}
