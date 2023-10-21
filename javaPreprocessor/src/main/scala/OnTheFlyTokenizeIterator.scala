package javaPreprocessor

/**
 * This iterator wraps an LL1Iterator. Its purpose is to return keywords like if, for, etc. in
 * one piece for further detection
 */
final class OnTheFlyTokenizeIterator(iterable: Iterable[Char]) {
  private val ll1Iterator: LL1Iterator = new LL1Iterator(iterable)

  def hasNext: Boolean = ll1Iterator.hasNext

  def peekNextChar: Char = ll1Iterator.peekNext

  def consume(): String = {
    val sb = new StringBuilder()
    val c = ll1Iterator.consume()
    sb.append(c)
    if (c.isLetter){
      while (ll1Iterator.hasNext && isLetterOrDigitOrUnderscore(ll1Iterator.peekNext)) {
        sb.append(ll1Iterator.consume())
      }
    }
    sb.toString()
  }

  private def isLetterOrDigitOrUnderscore(c: Char): Boolean = {
    c.isLetterOrDigit || c == '_'
  }

}
