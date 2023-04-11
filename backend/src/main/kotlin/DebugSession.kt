import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.sun.jdi.*
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.MethodEntryEvent
import com.sun.jdi.event.MethodExitEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.request.StepRequest
import java.nio.file.Path
import kotlin.io.path.name

class DebugSession(programDir: Path, mainClassName: String, private val inspectedFiles: Map<Path, CompilationUnit>) {

    private val inspectedFilesNames: Set<String> = inspectedFiles.map { it.key.name }.toSet()
    private val maxSrcFileNameLength: Int = inspectedFilesNames.maxOf(String::length)
    private val vm: VirtualMachine

    private val trace: MutableTrace = mutableListOf()

    private var lastStopInfo: StopInfo = StopInfo(null, null, null)

    init {

        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val args = launchingConnector.defaultArguments()
        args["main"]!!.setValue(mainClassName)
        args["options"]!!.setValue("-cp $programDir")
        vm = launchingConnector.launch(args)

        val classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest()
        classPrepareRequest.addClassFilter(mainClassName)
        classPrepareRequest.enable()
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
//                        requestStep(event.thread())
                        createFunEntryAndExitEventsRequest()
                    }

                    is StepEvent -> {
                        val thread = event.thread()
                        inspectProgramState(thread)
                        deleteStepRequests()
                        requestStep(thread)
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

    private fun createFunEntryAndExitEventsRequest() {
        val classesToMonitor =
            inspectedFiles
                .flatMap { it.value.findAll(TypeDeclaration::class.java) }
                .map { it.name.id }
        for (classToMonitor in classesToMonitor) {
            with(vm.eventRequestManager()) {
                createMethodEntryRequest()
                    .apply { addClassFilter(classToMonitor) }
                    .enable()
                createMethodExitRequest()
                    .apply { addClassFilter(classToMonitor) }
                    .enable()
            }
        }
    }

    private fun requestStep(thread: ThreadReference) {
        vm.eventRequestManager()
            .createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO)
            .enable()
    }

    private fun deleteStepRequests() {
        val manager = vm.eventRequestManager()
        manager.deleteEventRequests(manager.stepRequests())
    }

    private fun inspectProgramState(thread: ThreadReference) {
        val stackFrame = thread.frame(0)
        val currStopLocation = stackFrame.location()
        val stackDepth = thread.frameCount()
        if (!(debugInfoArePresent(currStopLocation) && currStopLocation.sourceName() in inspectedFilesNames)) {
            lastStopInfo = StopInfo(null, lastStopInfo.lastKnownEnclosingCall, stackDepth)
            return
        }
        val lastStopInfoBefore = lastStopInfo
        if ((lastStopInfoBefore.stackDepth != null) && (stackDepth < lastStopInfoBefore.stackDepth)) {
            val argsValues =
                currStopLocation
                    .method()
                    .arguments()
                    .map { it.name() to stringOf(stackFrame.getValue(it)) }
            trace.add(FunCallEvent(currStopLocation.method().name(), argsValues))
        }
//        if (lastStopInfoBefore.stackDepth != null && stackDepth > lastStopInfoBefore.stackDepth){
//            trace.add(FunExitEvent())
//        }

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
