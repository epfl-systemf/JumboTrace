package javaPreprocessor

final class LL1Iterator(iterable: Iterable[Char]) {

  private val iterator = iterable.iterator
  private var next: Option[Char] = iterator.nextOption()
  private var reachedEnd: Boolean = iterable.isEmpty

  def hasNext: Boolean = next.isDefined

  def peekNext: Char = next.getOrElse(throwEndOfIter())

  def consume(): Char = {
    val curr = next.getOrElse(throwEndOfIter())
    next = iterator.nextOption()
    curr
  }

  private def throwEndOfIter() = throw new IllegalStateException("reached end of iterator")

}
