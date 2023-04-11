import com.github.javaparser.ParserConfiguration
import com.github.javaparser.Problem
import java.nio.file.Path
import kotlin.jvm.optionals.getOrDefault


fun main(args: Array<String>) {

    if (args.size < 3) {
        reportArgsError()
    }

    val programDir = Path.of(args[0])
    val mainClassName = args[1]
    val srcFiles = args.drop(2).map(Path::of)

    val parser = SourceFilesParser(ParserConfiguration.LanguageLevel.JAVA_17)
    val debugSession = DebugSession(programDir, mainClassName, parser.parse(srcFiles, ::reportParseProblems))
    val trace = debugSession.run()
    displayTrace(trace)
    JsonWriter.write(Path.of("./trace/${mainClassName}-trace.json"), trace)
}

private fun displayTrace(trace: Trace) {
    val indentGranularity = 2
    var indent = 0
    for (cfe in trace) {
        if (cfe is FunExitEvent){
            indent -= indentGranularity
        }
        println(" ".repeat(indent) + cfe)
        if (cfe is FunCallEvent){
            indent += indentGranularity
        }
    }
}

fun reportArgsError(): Nothing {
    System.err.println("Usage: DemoTracer <program dir> <main class> <main file> <other files>*  |  e.g. DemoTracer Main ./Main.java ./Aux.java")
    ExitCode.CmdArgsError.exit()
}

fun reportParseProblems(problems: List<Pair<Path, Problem>>): Nothing {
    for (e in problems) {
        val (filepath, problem) = e
        val locationStr =
            problem.location
                .flatMap { it.begin.range }
                .map { it.begin.toString() }
                .getOrDefault("[unknown location]")
        System.err.println("$filepath:$locationStr : ${problem.message}")
    }
    System.err.println("Could not parse source files: errors found")
    ExitCode.SrcParseError.exit()
}
