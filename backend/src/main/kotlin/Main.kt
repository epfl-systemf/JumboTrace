import java.nio.file.Path
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    if (args.size < 3) {
        reportError()
    }

    val programDir = Path.of(args[0])
    val mainClassName = args[1]
    val srcFiles = args.drop(2).map(Path::of)

    val debugSession = DebugSession(programDir, mainClassName, srcFiles)
    val trace = debugSession.run()
    JsonWriter.write(Path.of("./trace/${mainClassName}-trace.json"), trace)
}

fun reportError() {
    System.err.println("Usage: DemoTracer <program dir> <main class> <main file> <other files>*  |  e.g. DemoTracer Main ./Main.java ./Aux.java")
    exitProcess(-1)
}
