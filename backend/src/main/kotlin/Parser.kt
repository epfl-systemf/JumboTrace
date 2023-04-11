import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ParserConfiguration.LanguageLevel
import com.github.javaparser.Problem
import com.github.javaparser.ast.CompilationUnit
import java.nio.file.Path

class SourceFilesParser(languageLevel: LanguageLevel) {
    private val parser: JavaParser

    init {
        val config = ParserConfiguration()
        config.languageLevel = languageLevel
        parser = JavaParser(config)
    }

    fun parse(filenames: List<Path>, errorCallback: (List<Pair<Path, Problem>>) -> Nothing): Map<Path, CompilationUnit> {
        val parsingResults = filenames.map { it to parser.parse(it) }
        val problems = parsingResults.flatMap { (path, result) ->
            result.problems.map { problem -> path to problem }
        }
        return if (problems.isEmpty()){
            parsingResults.associate { it.first to it.second.result.get() }
        } else {
            errorCallback(problems)
        }
    }

}