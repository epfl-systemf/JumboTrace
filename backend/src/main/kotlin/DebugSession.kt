import com.github.javaparser.ast.CompilationUnit
import com.sun.jdi.*
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.jvm.Throws

class DebugSession(programDir: Path, mainClassName: String, inspectedFiles: Map<Path, CompilationUnit>) {

    private val inspectedFilesNames: Set<String> = inspectedFiles.map { it.key.name }.toSet()
    private val vm: VirtualMachine

    private val trace: MutableTrace = mutableListOf()

    init {

        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val args = launchingConnector.defaultArguments()
        args["main"]!!.setValue(mainClassName)
        args["options"]!!.setValue("-cp $programDir")
        vm = launchingConnector.launch(args)

        vm.eventRequestManager()
            .createClassPrepareRequest()
            .enable()
    }

    tailrec fun run(): Trace {
        val eventSet = try {
            vm.eventQueue().remove()
        } catch (vmde: VMDisconnectedException) {
            println("VM is disconnected")
            null
        }
        return if (eventSet == null) {
            trace
        } else {
            for (event in eventSet) {
                when (event) {

                    is ClassPrepareEvent -> {
                        val loadedClass = event.referenceType()
                        try {
                            if (loadedClass.sourceName() in inspectedFilesNames) {
                                addMonitoringToClass(loadedClass)
                            }
                        } catch (_: AbsentInformationException) {
                            // ignore (intended behavior when loading a class that has not been compiled with the -g option)
                        }
                    }

                    is BreakpointEvent -> {
                        if (debugInfoArePresent(event.location())) {
                            println("visited line ${event.location()}")
                        }
                    }

                    is MethodEntryEvent -> {
                        println("entered ${event.method().name()}")
                    }

                    is MethodExitEvent -> {
                        println("exited ${event.method().name()}")
                    }
                }
                vm.resume()
            }
            run()
        }
    }

    @Throws(AbsentInformationException::class)
    private fun addMonitoringToClass(loadedClass: ReferenceType) {
        val eventRequestManager = vm.eventRequestManager()
        for (location in loadedClass.allLineLocations()) {
            eventRequestManager
                .createBreakpointRequest(location)
                .enable()
        }
        eventRequestManager
            .createMethodEntryRequest()
            .apply { addClassFilter(loadedClass) }
            .enable()
        eventRequestManager
            .createMethodExitRequest()
            .apply { addClassFilter(loadedClass) }
            .enable()
    }

    private fun debugInfoArePresent(currStopLocation: Location): Boolean = currStopLocation.lineNumber() != -1

    private fun stringOf(value: Value): String =
        when (value) {
            is ArrayReference ->
                value.values.map(::stringOf).joinToString(prefix = "[", separator = ",", postfix = "]")

            else ->
                value.toString()
        }

}
