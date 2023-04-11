import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

typealias CFEventUid = Long

typealias Trace = List<ControlFlowEvent>
typealias MutableTrace = MutableList<ControlFlowEvent>

@Serializable sealed class ControlFlowEvent {
    val uid: CFEventUid = uidGenerator.incrementAndGet()

    companion object {
        private val uidGenerator = AtomicLong()
    }
}

@Serializable data class LineVisitedEvent(val newLine: LineRef, val stackParentUid: CFEventUid): ControlFlowEvent()

@Serializable data class FunCallEvent(val funId: String, val args: List<Pair<String, String>>): ControlFlowEvent()
@Serializable data class FunExitEvent(val funId: String, val retVal: String?): ControlFlowEvent()

@Serializable data class LoopEnterEvent(val loopLine: LineRef): ControlFlowEvent()
@Serializable data class LoopNewIterEvent(val loopLine: LineRef): ControlFlowEvent()
@Serializable data class LoopExitEvent(val loopLine: LineRef): ControlFlowEvent()

@Serializable data class NewVarDefinedEvent(val varName: String, val value: String?): ControlFlowEvent()
@Serializable data class VarSetEvent(val varName: String, val value: String): ControlFlowEvent()
@Serializable data class FieldSetEvent(val owner: String, val field: String, val value: String): ControlFlowEvent()

