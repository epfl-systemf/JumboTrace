import java.nio.file.Path
import kotlin.system.exitProcess


fun main(args: Array<String>) {

    if (args.size != 2){
        reportError()
    }

    val className = args[0]
    val filePath = args[1]

    if (className.contains(".") || !filePath.contains(".")){
        reportError()
    }

    val debugger = Debugger(className, Path.of(filePath))
    debugger.run()

}

fun reportError(){
    println("Usage: DemoTracer <class name> <source file>  |  e.g. DemoTracer Foo ./Foo.java")
    exitProcess(-1)
}
