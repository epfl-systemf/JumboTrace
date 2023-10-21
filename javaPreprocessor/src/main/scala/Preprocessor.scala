package javaPreprocessor

import scala.io.Source
import scala.util.Using

final class Preprocessor {

  type FileName = String

  private type Code = String
  private type LineIdx = Int

  def preprocess(originalFileName: FileName, newFileName: FileName, mappingFileName: FileName): Unit = {
    Using(Source.fromFile(originalFileName)){ bufSrc =>
      val formattedCode = preprocess(bufSrc.getLines().toSeq)
      // TODO
    }
  }

  def preprocess(codeLines: Seq[String]): (Code, Map[LineIdx, LineIdx]) = {
    val code = codeLines.map(_.trim).mkString("\n")
    makeOneStatementPerLine(code)
  }

  // assumption: code has been parsed in such a way that there is no Windows or old MacOS end-of-line anymore (i.e. no \r)
  // assumption: lines have been trimmed
  private def makeOneStatementPerLine(code: Code): (Code, Map[LineIdx, LineIdx]) = {

    val codeB = new StringBuilder()
    val linesMappingB = Map.newBuilder[LineIdx, LineIdx]
    val codeIter = new OnTheFlyTokenizeIterator(code)

    // TODO recognize if, while, etc.

    def deleteSpaces(): Unit = {
      while (codeIter.peekNextChar.isSpaceChar && codeIter.hasNext) {
        codeIter.consume()
      }
    }

    var origFileLineIdx = 1
    var formattedFileLineIdx = 1

    linesMappingB.addOne(origFileLineIdx -> formattedFileLineIdx)
    while (codeIter.hasNext){
      val curr = codeIter.consume()
      codeB.append(curr)
      curr match {
        case ";" | "{" | "}" if codeIter.hasNext && codeIter.peekNextChar != '\n' =>
          deleteSpaces()
          codeB.append('\n')
          formattedFileLineIdx += 1
          linesMappingB.addOne(formattedFileLineIdx -> origFileLineIdx)
        case "\n" =>
          deleteSpaces()
          require(!codeIter.peekNextChar.isSpaceChar, s"invalid code input: line $origFileLineIdx has not been trimmed")
          origFileLineIdx += 1
          formattedFileLineIdx += 1
          linesMappingB.addOne(formattedFileLineIdx -> origFileLineIdx)
        case _ => ()
      }
    }
    (codeB.toString(), linesMappingB.result())
  }

}
