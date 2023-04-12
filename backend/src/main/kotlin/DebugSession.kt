import com.github.javaparser.ast.CompilationUnit
import com.sun.jdi.*
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import java.nio.file.Path
import kotlin.io.path.name

class DebugSession(classPath: Path, mainClassName: String, inspectedFiles: Map<Path, CompilationUnit>) {

    private val inspectedFilesNames: Set<String> = inspectedFiles.map { it.key.name }.toSet()
    private val vm: VirtualMachine

    private val trace: MutableTrace = mutableListOf()
    private val eventsUidStack: MutableList<CFEventUid> = mutableListOf()

    init {

        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val args = launchingConnector.defaultArguments()
        args["main"]!!.setValue(mainClassName)
        args["options"]!!.setValue("-cp $classPath -Xint")     // TODO is Xint really needed?
        vm = launchingConnector.launch(args)

        vm.eventRequestManager()
            .createClassPrepareRequest()
            .enable()
    }

    tailrec fun run(): Trace {
        val eventSet = try {
            vm.eventQueue().remove()
        } catch (vmde: VMDisconnectedException) {
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
        /*
         * TODO investigate incompatibilities between breakpoints and call/return events
         *  Also step seems even more incompatible
         */
//        for (location in loadedClass.allLineLocations()) {
//            eventRequestManager
//                .createBreakpointRequest(location)
//                .enable()
//        }
        eventRequestManager
            .createMethodEntryRequest()
            .apply { addClassFilter(loadedClass) }
            .enable()
        eventRequestManager
            .createMethodExitRequest()
            .apply { addClassFilter(loadedClass) }
            .enable()
    }

    private fun disableRequests(){
        with(vm.eventRequestManager()){
            breakpointRequests().forEach { it.disable() }
            methodEntryRequests().forEach { it.disable() }
            methodExitRequests().forEach { it.disable() }
            classPrepareRequests().forEach { it.disable() }
        }
    }

    private fun enableRequests(){
        with(vm.eventRequestManager()){
            breakpointRequests().forEach { it.enable() }
            methodEntryRequests().forEach { it.enable() }
            methodExitRequests().forEach { it.enable() }
            classPrepareRequests().forEach { it.enable() }
        }
    }

    private fun handleFunctionEntered(event: MethodEntryEvent) {
        val methodArgs = event.method().arguments()
        // this weird trick with empty args list is an attempt to prevent nondeterministic crashes (not sure if it works)
        val args: List<Pair<String, String>> =
            if (methodArgs.isEmpty()) emptyList()
            else methodArgs.map {
                val thread = event.thread()
                it.name() to evaluate(thread.frame(0).getValue(it), thread)
            }
        val methodName = event.method().name()
        val callEvent = FunCallEvent(methodName, args, eventsUidStack.lastOrNull())
        trace.add(callEvent)
        eventsUidStack.add(callEvent.uid)
    }

    private fun handleFunctionExited(event: MethodExitEvent) {
        eventsUidStack.removeLast()
        trace.add(
            FunExitEvent(
                event.method().name(),
                evaluate(event.returnValue(), event.thread()),
                eventsUidStack.lastOrNull()
            )
        )
    }

    private fun handleBreakpoint(event: BreakpointEvent) {
        val location = event.location()
        if (debugInfoIsPresent(location)) {
            val lineRef = LineRef(location.sourceName(), location.lineNumber())
            val visibleVars: Map<String, String>? = try {
                val thread = event.thread()
                val frame = thread.frame(0)
                frame.visibleVariables().associate { it.name() to evaluate(frame.getValue(it), thread) }
            } catch (_: IncompatibleThreadStateException){  // FIXME if possible
                null
            }
            trace.add(LineVisitedEvent(lineRef, visibleVars, eventsUidStack.lastOrNull()))
        }
    }

    private fun debugInfoIsPresent(location: Location): Boolean = (location.lineNumber() != -1)

    private fun evaluate(value: Value, thread: ThreadReference): String {
        return when (value) {
            is ArrayReference -> value.values.joinToString(prefix = "[", separator = ", ", postfix = "]") {
                evaluate(it, thread)
            }

            is VoidValue -> "<void>"

            is StringReference -> value.toString()

            is ObjectReference -> {
                val classType = value.referenceType() as ClassType
                val method = classType.concreteMethodByName("toString", "()Ljava/lang/String;")
                disableRequests()
                val str = value.invokeMethod(thread, method, emptyList(), 0x0)
                    .toString().drop(1).dropLast(1)
                enableRequests()
                val valUid = value.uniqueID()
                "[${classType.name()}-$valUid: $str]"
            }

            else -> value.toString()
        }
    }

}
