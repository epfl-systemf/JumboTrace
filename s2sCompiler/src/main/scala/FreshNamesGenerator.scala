package s2sCompiler

final class FreshNamesGenerator {
  private val prefix = "jbt$"
  private var idx = 0
  
  def nextName(): String = {
    idx += 1
    prefix + idx
  }

}
