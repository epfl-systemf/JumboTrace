import com.github.javaparser.ast.CompilationUnit
import com.sun.jdi.*
import com.sun.jdi.event.*
import java.nio.file.Path
import kotlin.io.path.name

// TODO is access to parsed files really necessary here?
class DebugSession(classPath: Path, mainClassName: String, inspectedFiles: Map<Path, CompilationUnit>) {

    private val inspectedFilesNames: Set<String> = inspectedFiles.map { it.key.name }.toSet()
    private val vm: VirtualMachine

    private val trace: MutableTrace = mutableListOf()
    private val eventsUidStack: MutableList<CFEventUid> = mutableListOf()

    init {

        // TODO redirect VM stdin/stdout/stderr
        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val args = launchingConnector.defaultArguments()
        args["main"]!!.setValue(mainClassName)
        args["options"]!!.setValue("-cp $classPath -Xint")     // TODO is Xint really needed?
        vm = launchingConnector.launch(args)
    }

    tailrec fun run(): Trace {
        val eventSet = vm.eventQueue().remove()
        return if (eventSet == null) {
            trace
        } else {
            for (event in eventSet) {
                when (event) {

                    is VMStartEvent -> {
                        vm.eventRequestManager()
                            .createClassPrepareRequest()
                            .enable()
                    }

                    is VMDisconnectEvent -> {
                        return trace
                    }

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
         * TODO try to set watchpoints on all methods to be notified when variables are updated
         *  Maybe (seems unlikely) this allows avoiding calls to frame.getVariables and similar and breakpoints can be used only
         *  to know what path the control-flow followed
         */
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

    private fun <T> withRequestsDisabled(action: () -> T): T {
        val requests = with(vm.eventRequestManager()) {
            breakpointRequests() +
                    methodEntryRequests() +
                    methodExitRequests() +
                    classPrepareRequests() +
                    stepRequests()
        }
        requests.forEach { it.disable() }
        val ret = action()
        requests.forEach { it.enable() }
        return ret
    }

    private fun handleFunctionEntered(event: MethodEntryEvent) {
        val thread = event.thread()
        val method = event.method()
        val methodArgs = method.arguments()
        val args = nullOn(IncompatibleThreadStateException::class.java) {
            val frame = thread.frame(0)
            methodArgs.map { localVar ->
                val value = nullOn(IllegalArgumentException::class.java) {
                    frame.getValue(localVar)
                }
                localVar.name() to value?.let { evaluate(it, thread) }
            }
        }
        val methodName = method.name()
        val callEvent = FunCallEvent(methodName, args, eventsUidStack.lastOrNull())
        trace.add(callEvent)
        eventsUidStack.add(callEvent.uid)
    }

    private fun handleFunctionExited(event: MethodExitEvent) {
        trace.add(
            FunExitEvent(
                event.method().name(),
                evaluate(event.returnValue(), event.thread()),
                eventsUidStack.removeLast()
            )
        )
    }

    private fun handleBreakpoint(event: BreakpointEvent) {
        val location = event.location()
        if (debugInfoIsPresent(location)) {
            val lineRef = LineRef(location.sourceName(), location.lineNumber())
            val visibleVars = nullOn(IncompatibleThreadStateException::class.java) {
                val thread = event.thread()
                val frame = thread.frame(0)
                frame.visibleVariables().associate { localVar ->
                    val value = nullOn(InvalidStackFrameException::class.java) {
                        frame.getValue(localVar)
                    }
                    localVar.name() to value?.let { evaluate(it, thread) }
                }
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
                val str = withRequestsDisabled {
                    value.invokeMethod(thread, method, emptyList(), ClassType.INVOKE_SINGLE_THREADED)
                        .toString().drop(1).dropLast(1)
                }
                val valUid = value.uniqueID()
                "[${classType.name().takeLastWhile { it != '.' }}-$valUid: $str]"
            }

            else -> value.toString()
        }
    }

    private fun <E : Throwable, T> nullOn(excClass: Class<E>, action: () -> T): T? {
        return try {
            action()
        } catch (e: Throwable) {
            if (e::class.java == excClass) {
                null
            } else {
                throw e
            }
        }
    }

}
