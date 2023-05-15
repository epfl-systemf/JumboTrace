package com.epfl.systemf.jumbotrace.instrumenter

import org.objectweb.asm.ClassReader

import java.io.FileReader
import java.nio.file.{Files, Paths}
import scala.util.Using

object Main {

  def main(args: Array[String]): Unit = {
    for (className <- args) do {

      val path = Paths.get(s"./$className.class")
      val bytes = Files.readAllBytes(path)

      val classTableB = new ClassTable.Builder(ClassName(className))
      val classExplorer = new ClassExplorer(classTableB)

      val explorationReader = new ClassReader(bytes)
      explorationReader.accept(classExplorer, ClassReader.EXPAND_FRAMES)
      val classTable = classTableB.built

      println(classTable)
    }
  }

}
