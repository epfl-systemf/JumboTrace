import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

typealias CFEventUid = Long

typealias Trace = List<ControlFlowEvent>
typealias MutableTrace = MutableList<ControlFlowEvent>

@Serializable
sealed class ControlFlowEvent {
    val uid: CFEventUid = uidGenerator.incrementAndGet()

    companion object {
        private val uidGenerator = AtomicLong()
    }
}

@Serializable
data class LineVisitedEvent(
    val newLine: LineRef,
    val stackParentUid: CFEventUid?,
    val visibleVars: Map<String, String>?
) : ControlFlowEvent() {
    override fun toString(): String =
        "[$uid] VISIT $newLine (stack parent: $stackParentUid) " +
                (visibleVars
                    ?.map { (n, v) -> "$n = $v" }
                    ?.joinToString(prefix = "[", separator = ", ", postfix = "]")
                    ?: "<missing variables info>")
}

@Serializable
data class FunCallEvent(val funId: String, val args: List<Pair<String, String>>) : ControlFlowEvent() {
    override fun toString(): String =
        "[$uid] CALL $funId" + args.joinToString(
            prefix = "(",
            postfix = ")",
            separator = ",",
            transform = { (name, value) ->
                "$name = $value"
            })
}

@Serializable
data class FunExitEvent(val funId: String, val retVal: String?) : ControlFlowEvent() {
    override fun toString(): String = "[$uid] EXIT $funId --> return $retVal"
}

@Serializable
data class LoopEnterEvent(val loopLine: LineRef) : ControlFlowEvent() {
    // TODO toString
}

@Serializable
data class LoopNewIterEvent(val loopLine: LineRef) : ControlFlowEvent() {
    // TODO toString
}

@Serializable
data class LoopExitEvent(val loopLine: LineRef) : ControlFlowEvent() {
    // TODO toString
}

