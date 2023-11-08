package s2sCompiler

import scala.annotation.tailrec
import scala.collection.mutable

final class FreshNamesGenerator(excludedIds: Set[String]) {
  private val prefix = "jbt"

  private val indices = mutable.Map.empty[String, Int]

  def nextName(middleFix: String): String = {
    val idx = indices.getOrElse(middleFix, 1)
    indices(middleFix) = idx + 1
    val id = prefix + "$" + middleFix + "$" + idx
    if excludedIds.contains(id) then nextName(middleFix) else id
  }
  
  def emptyCopy: FreshNamesGenerator = new FreshNamesGenerator(excludedIds)

}
