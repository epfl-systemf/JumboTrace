package s2sCompiler

import scala.collection.mutable

final class FreshNamesGenerator {
  private val prefix = "jbt"

  private val indices = mutable.Map.empty[String, Int]

  def nextName(middleFix: String): String = {
    val idx = indices.getOrElse(middleFix, 1)
    indices(middleFix) = idx + 1
    prefix + "$" + middleFix + "$" + idx
  }

}
