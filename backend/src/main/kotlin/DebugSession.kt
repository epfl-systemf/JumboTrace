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
    private val eventsUidStack: MutableList<CFEventUid> = mutableListOf()

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
                        handleBreakpoint(event)
                    }

                    is MethodEntryEvent -> {
                        handleFunctionEntered(event)
                    }

                    is MethodExitEvent -> {
                        handleFunctionExited(event)
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

    private fun handleFunctionEntered(event: MethodEntryEvent) {
        val methodArgs = event.method().arguments()
        // this weird trick with empty args list is an attempt to prevent nondeterministic crashes (not sure if it works)
        val args: List<Pair<String, String>> =
            if (methodArgs.isEmpty()) emptyList()
            else methodArgs.map {
                it.name() to stringOf(event.thread().frame(0).getValue(it))
            }
        val methodName = event.method().name()
        val callEvent = FunCallEvent(methodName, args)
        trace.add(callEvent)
        eventsUidStack.add(callEvent.uid)
    }

    private fun handleFunctionExited(event: MethodExitEvent) {
        trace.add(
            FunExitEvent(
                event.method().name(),
                stringOf(event.returnValue())
            )
        )
        eventsUidStack.removeLast()
    }

    private fun handleBreakpoint(event: BreakpointEvent) {
        val location = event.location()
        if (debugInfoIsPresent(location)) {
            val lineRef = LineRef(location.sourceName(), location.lineNumber())
            val parentCallUid = eventsUidStack.last()
            trace.add(LineVisitedEvent(lineRef, parentCallUid))
        }
    }

    private fun debugInfoIsPresent(currStopLocation: Location): Boolean = currStopLocation.lineNumber() != -1

    private fun stringOf(value: Value): String =
        when (value) {

            is ArrayReference ->
                value.values.joinToString(prefix = "[", separator = ",", postfix = "]", transform = ::stringOf)

            is VoidValue -> "<void>"

            else -> value.toString()
        }

}
