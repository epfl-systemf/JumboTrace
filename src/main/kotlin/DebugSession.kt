import com.sun.jdi.*
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.request.StepRequest
import java.nio.file.Path
import kotlin.io.path.name

class DebugSession(mainClassName: String, private val mainFileName: String, filePaths: List<Path>) {
    /**
     * Maps file name to list of lines
     */
    private val filesContent: Map<String, List<String>>
    private val vm: VirtualMachine
    private val maxSrcFileNameLength: Int

    private var lastDisplayedLine: LineInfo? = null

    private val ansiYellow = "\u001B[33m"
    private val ansiReset = "\u001B[0m"

    init {

        require(filePaths.any { it.name == mainFileName })

        filesContent = filePaths.associate { Pair(it.name, it.toFile().readLines()) }
        maxSrcFileNameLength = filePaths.maxOf { it.name.length }

        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val args = launchingConnector.defaultArguments()
        args["main"]!!.setValue(mainClassName)
        vm = launchingConnector.launch(args)

        val classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest()
        classPrepareRequest.addClassFilter(mainClassName)
        classPrepareRequest.enable()
    }

    tailrec fun run() {
        val eventSet = try {
            vm.eventQueue().remove()
        } catch (vmde: VMDisconnectedException) {
            println("VM is disconnected")
            null
        }
        if (eventSet != null) {
            for (event in eventSet) {
                when (event) {
                    // at initialization, ClassPrepareEvent => set breakpoints to stop at first executed statement
                    is ClassPrepareEvent -> setBreakpointsInMainClass(event)
                    // when reaching the first statement, stop using breakpoints and move to stepping
                    is BreakpointEvent -> {
                        val thread = event.thread()
                        displayStatementIfLineChanged(thread)
                        deleteBreakpoints()
                        requestStep(thread)
                    }
                    // stepping: schedule a step on the next statement
                    is StepEvent -> {
                        val thread = event.thread()
                        displayStatementIfLineChanged(thread)
                        deleteStepRequests()
                        requestStep(thread)
                    }
                }
                vm.resume()
            }
            run()
        }
    }

    private fun setBreakpointsInMainClass(classPrepareEvent: ClassPrepareEvent) {
        val refType = classPrepareEvent.referenceType()
        for (lineNum in 1..(filesContent[mainFileName]!!.size)) {
            val locationsOfLine = refType.locationsOfLine(lineNum)
            for (location in locationsOfLine) {
                val breakReq = vm.eventRequestManager().createBreakpointRequest(location)
                breakReq.enable()
            }
        }
    }

    private fun deleteBreakpoints() {
        vm.eventRequestManager().deleteAllBreakpoints()
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

    private fun displayStatementIfLineChanged(thread: ThreadReference) {
        val stackFrame = thread.frame(0)
        val location = stackFrame.location()
        if (hasInfo(location) && location.sourceName() in filesContent && lastDisplayedLine?.matches(location) != true) {
            lastDisplayedLine = lineInfoFromLocation(location, thread.frameCount())
            actuallyDisplayStatement(location, stackFrame)
        } else if (
            (!hasInfo(location) || location.sourceName() !in filesContent)
            && lastDisplayedLine != null
            // as long as we are deeper in the stack than the last time a line was displayed, is considered as a nested call
            && thread.frameCount() <= lastDisplayedLine!!.frameCnt
        ) {
            lastDisplayedLine = null
        }
    }

    private fun actuallyDisplayStatement(location: Location, stackFrame: StackFrame) {
        require(location.sourceName() in filesContent)
        val lineNumber = location.lineNumber()
        val visibleVars = stackFrame.getValues(stackFrame.visibleVariables())
        val lineDescr = (
                "${location.sourceName().padStart(maxSrcFileNameLength)} : $lineNumber:\t" +
                        filesContent[location.sourceName()]!![lineNumber - 1] +
                        "   \t" + ansiYellow +
                        visibleVars
                            .map { (localVar, value) -> "${localVar.name()} = $value" }
                            .joinToString(prefix = "[", separator = ", ", postfix = "]") +
                        ansiReset
                )
        println(lineDescr)
    }

    private fun hasInfo(location: Location): Boolean = location.lineNumber() != -1

    private data class LineInfo(val sourceName: String, val line: Int, val frameCnt: Int) {
        fun matches(location: Location): Boolean {
            return location.sourceName() == this.sourceName && location.lineNumber() == this.line
        }
    }

    private fun lineInfoFromLocation(location: Location, frameCnt: Int): LineInfo =
        LineInfo(location.sourceName(), location.lineNumber(), frameCnt)

}
