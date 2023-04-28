import com.github.javaparser.ast.CompilationUnit
import com.sun.jdi.*
import com.sun.jdi.event.*
import com.sun.jdi.request.StepRequest
import java.nio.file.Path
import kotlin.io.path.name

// TODO is access to parsed files really necessary here?
class DebugSession(classPath: Path, mainClassName: String, inspectedFiles: Map<Path, CompilationUnit>) {

    private val inspectedFilesNames: Set<String> = inspectedFiles.map { it.key.name }.toSet()
    private val vm: VirtualMachine

    private val trace: MutableTrace = mutableListOf()
    private val eventsUidStack: MutableList<CFEventUid> = mutableListOf()
    private val classesStack: MutableList<ReferenceType> = mutableListOf()

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
                        println("VM start")
                        vm.eventRequestManager()
                            .createClassPrepareRequest()
                            .enable()
                    }

                    is VMDisconnectEvent -> {
                        println("VM disconnect")
                        return trace
                    }

                    is ClassPrepareEvent -> {
                        val loadedClass = event.referenceType()
                        println("Loading class: $loadedClass")
                        try {
                            if (loadedClass.sourceName() in inspectedFilesNames) {
                                addMonitoringToClass(loadedClass, event.thread())
                            }
                        } catch (_: AbsentInformationException) {
                            // ignore (intended behavior when loading a class that has not been compiled with the -g option)
                        }
                    }

//                    is BreakpointEvent -> {
//                        handleStop(event)
//                    }

                    is StepEvent -> {
                        println("Step: ${event.location()}")
                        handleStop(event)
                    }

                    is MethodEntryEvent -> {
                        println("Method call: ${event.method().name()}")
                        handleFunctionEntered(event)
                    }

                    is MethodExitEvent -> {
                        println("Method return: ${event.method().name()}")
                        handleFunctionExited(event)
                    }
                }
                vm.resume()
            }
            run()
        }
    }

    private fun newStepRequest(thread: ThreadReference?, targetClass: ReferenceType): StepRequest {
        val eventRequestManager = vm.eventRequestManager()
        eventRequestManager.deleteEventRequests(eventRequestManager.stepRequests())
        return eventRequestManager
            .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER)
            .apply {
                addClassFilter(targetClass)
                enable()
            }
    }

    @Throws(AbsentInformationException::class)
    private fun addMonitoringToClass(loadedClass: ReferenceType, thread: ThreadReference) {
        val eventRequestManager = vm.eventRequestManager()
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
        val frame = thread.frame(0)
        val values = methodArgs.map(frame::getValue)
        val args =
            methodArgs.zip(values)
                .map { (localVar, value) ->
                    localVar.name() to value?.let { evaluate(it, thread) }
                }
        val methodName = method.name()
        val callEvent = FunCallEvent(methodName, args, eventsUidStack.lastOrNull())
        trace.add(callEvent)
        eventsUidStack.add(callEvent.uid)
        val currClass = method.declaringType()
        classesStack.add(currClass)
        newStepRequest(thread, currClass)
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

    private fun handleStop(event: LocatableEvent) {
        val location = event.location()
        if (debugInfoIsPresent(location)) {
            val lineRef = LineRef(location.sourceName(), location.lineNumber())
            val thread = event.thread()
            val frame = thread.frame(0)
            val visibleVars =
                frame.visibleVariables()
                    .map { localVar ->
                        localVar.name() to frame.getValue(localVar)
                    }.associate { (name, value) ->
                        name to (value?.let { evaluate(it, thread) })
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

}
