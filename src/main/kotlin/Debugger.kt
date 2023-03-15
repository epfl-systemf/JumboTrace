import com.sun.jdi.Bootstrap
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.ClassPrepareEvent
import java.nio.file.Path

class Debugger(private val debugClassName: String, filePath: Path) {
    private val srcLines: List<String>
    private val vm: VirtualMachine

    init {
        srcLines = filePath.toFile().readLines()

        val launchingConnector = Bootstrap.virtualMachineManager().defaultConnector()
        val args = launchingConnector.defaultArguments()
        args["main"]!!.setValue(debugClassName)
        vm = launchingConnector.launch(args)

        val classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest()
        classPrepareRequest.addClassFilter(debugClassName)
        classPrepareRequest.enable()
    }

    fun run() {
        val eventSet = try {
            vm.eventQueue().remove()
        } catch (vmde: VMDisconnectedException) {
            println("VM is disconnected")
            null
        }
        if (eventSet != null) {
            for (event in eventSet) {
                when (event) {
                    is ClassPrepareEvent -> setBreakpoints(event)
                    is BreakpointEvent -> displayStatement(event)
                }
                vm.resume()
            }
            run()
        }
    }

    private fun setBreakpoints(classPrepareEvent: ClassPrepareEvent) {
        val refType = classPrepareEvent.referenceType()
        for (lineNum in 1..(srcLines.size)) {
            val locationsOfLine = refType.locationsOfLine(lineNum)
            if (locationsOfLine.isNotEmpty()) {
                val breakReq = vm.eventRequestManager().createBreakpointRequest(locationsOfLine[0])
                breakReq.enable()
            }
        }
    }

    private fun displayStatement(breakpointEvent: BreakpointEvent) {
        val stackFrame = breakpointEvent.thread().frame(0)
        val location = stackFrame.location()
        if (location.toString().contains(debugClassName)) {
            val lineNumber = location.lineNumber()
            val visibleVars = stackFrame.getValues(stackFrame.visibleVariables())
            val lineDescr = (
                    "$lineNumber:\t" +
                            srcLines[lineNumber - 1] +
                            "   \t" +
                            visibleVars
                                .map { (localVar, value) -> "${localVar.name()} = $value" }
                                .joinToString(prefix = "[", separator = ",", postfix = "]")
                    )
            println(lineDescr)
        }
    }

}
