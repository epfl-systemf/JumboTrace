package javaPreprocessor

import scala.io.Source
import scala.util.Using

object Preprocessor {

  type FileName = String
  type Code = String
  type LineIdx = Int
  type ColIdx = Int

  def preprocess(originalFileName: FileName, newFileName: FileName, mappingFileName: FileName): Unit = {
    Using(Source.fromFile(originalFileName)){ bufSrc =>
      val formattedCode = preprocess(bufSrc.getLines().toSeq)
      // TODO
    }
  }

  def preprocess(codeLines: Seq[String]): (Code, Map[LineIdx, (LineIdx, ColIdx)]) = {
    val code = codeLines.mkString("\n")
    makeOneStatementPerLine(code)
  }

  // assumption: code has been parsed in such a way that there is no Windows or old MacOS end-of-line anymore (i.e. no \r)
  private def makeOneStatementPerLine(code: Code): (Code, Map[LineIdx, (LineIdx, ColIdx)]) = {

    val codeB = new StringBuilder()
    val linesMappingB = Map.newBuilder[LineIdx, (LineIdx, ColIdx)]
    val codeIter = new OnTheFlyTokenizeIterator(code)

    var origFileLineIdx = 1
    var formattedFileLineIdx = 1
    var origFileColIdx = 1

    // TODO recognize if, while, etc.
    // Plan: transform e.g.  if (x == 0){ ... }  into  boolean ___if_cond_line_78___; if (___if_cond_line_78___ = x == 0){ ... }

    def deleteSpaces(): Unit = {
      while (codeIter.peekNextChar.isSpaceChar && codeIter.hasNext) {
        codeIter.consume()
        origFileColIdx += 1
      }
    }

    deleteSpaces()
    linesMappingB.addOne(formattedFileLineIdx -> (origFileLineIdx, origFileColIdx))
    while (codeIter.hasNext){
      val curr = codeIter.consume()
      origFileColIdx += curr.length
      codeB.append(curr)
      curr match {
        case ";" | "{" | "}" if codeIter.hasNext && codeIter.peekNextChar != '\n' =>
          deleteSpaces()
          codeB.append('\n')
          formattedFileLineIdx += 1
          linesMappingB.addOne(formattedFileLineIdx -> (origFileLineIdx, origFileColIdx))
        case "\n" =>
          origFileLineIdx += 1
          formattedFileLineIdx += 1
          origFileColIdx = 1
          deleteSpaces()
          linesMappingB.addOne(formattedFileLineIdx -> (origFileLineIdx, origFileColIdx))
        case _ => ()
      }
    }
    (codeB.toString(), linesMappingB.result())
  }

}
