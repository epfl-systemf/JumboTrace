package s2sCompiler

import com.github.javaparser.ast.Node
import injectionAutomation.InjectedMethod

final class ProgramString(indentGranularity: Int) {
  private val sb = new StringBuilder()
  private var indentLevel: Int = 0
  private var currentLineIdx: Int = 1

  def incIndent(): ProgramString = {
    indentLevel += 1
    this
  }

  def decIndent(): ProgramString = {
    indentLevel -= 1
    this
  }

  def add(str: String): ProgramString = {
    for (char <- str){
      if (char == '\n'){
        newLine()
      } else {
        sb.append(char)
      }
    }
    this
  }

  def addPositioned(astNode: Node): ProgramString = {
    astNode.getRange.ifPresent {
      range => {
        val nodeLineIdx = range.begin.line
        if (nodeLineIdx < currentLineIdx) {
          throw new IllegalStateException("going backwards in line indices")
        }
        while (currentLineIdx < nodeLineIdx) {
          newLine()
          currentLineIdx += 0   // just to avoid Intellij faulty warning
        }
      }
    }
    add(astNode.toString)
  }
  
  def addSpace(): ProgramString = add(" ")

  def newLine(): ProgramString = {
    sb.append("\n")
    sb.append(" " * (indentGranularity * indentLevel))
    currentLineIdx += 1
    this
  }
  
  def openParenthesis(): ProgramString = add("(")
  def closeParenthesis(): ProgramString = add(")")
  def openBracket(): ProgramString = add("[")
  def closeBracket(): ProgramString = add("]")
  def openBrace(): ProgramString = add("{")
  def closeBrace(): ProgramString = add("}")

  def build(): String = {
    sb.toString()
  }

}
