import java.nio.file.Path
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    if (args.size < 3){
        reportError()
    }

    val programDir = Path.of(args[0])
    val copiedProgramPath = programDir.resolve("java-tracer-tmp")
    val mainClassName = args[1]
    val mainFileName = args[2]
    val srcFiles = args.drop(2).map { copiedProgramPath.resolve(it) }

    val filesCopyManager = FilesCopyManager(copiedProgramPath)

    try {
        filesCopyManager.scanAndCopyToTmp(programDir)
        val debugSession = DebugSession(copiedProgramPath.toString(), mainClassName, mainFileName, srcFiles)
        val trace = debugSession.run()
        JsonWriter.write(Path.of("./trace/${mainClassName}-trace.json"), trace)
    } finally {
        filesCopyManager.deleteTmpDir()
    }

}

fun reportError(){
    System.err.println("Usage: DemoTracer <program dir> <main class> <main file> <other files>*  |  e.g. DemoTracer Main ./Main.java ./Aux.java")
    exitProcess(-1)
}
