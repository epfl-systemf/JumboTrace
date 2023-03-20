import java.nio.file.Path
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    if (args.size < 2){
        reportError()
    }

    val mainClassName = args[0]
    val mainFileName = args[1]
    val filenames = args.drop(1)

    if (mainClassName.contains(".") || !filenames.all { it.contains(".") }){
        reportError()
    }

    val debugSession = DebugSession(mainClassName, mainFileName, filenames.map(Path::of))
    debugSession.run()

}

fun reportError(){
    System.err.println("Usage: DemoTracer <main class> <main file> <other files>*  |  e.g. DemoTracer Main ./Main.java ./Aux.java")
    exitProcess(-1)
}
