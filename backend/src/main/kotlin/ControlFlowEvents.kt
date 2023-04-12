import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

typealias CFEventUid = Long

typealias Trace = List<ControlFlowEvent>
typealias MutableTrace = MutableList<ControlFlowEvent>

@Serializable
sealed class ControlFlowEvent {
    val uid: CFEventUid = uidGenerator.incrementAndGet()

    abstract val stackParentUid: CFEventUid?

    companion object {
        private val uidGenerator = AtomicLong()
    }
}

@Serializable
data class LineVisitedEvent(
    val newLine: LineRef,
    val visibleVars: Map<String, String?>?,
    override val stackParentUid: CFEventUid?
) : ControlFlowEvent() {
    override fun toString(): String =
        "[$uid ($stackParentUid)] VISIT $newLine => " +
                (visibleVars
                    ?.map { (n, v) -> "$n = ${v ?: "??"}" }
                    ?.joinToString(prefix = "{ ", separator = ", ", postfix = " }")
                    ?: "<?? missing vars>")
}

@Serializable
data class FunCallEvent(
    val funId: String,
    val args: List<Pair<String, String?>>?,
    override val stackParentUid: CFEventUid?
) : ControlFlowEvent() {
    override fun toString(): String =
        "[$uid ($stackParentUid)] CALL $funId" + (
                args?.joinToString(
                    prefix = "(",
                    postfix = ")",
                    separator = ",",
                    transform = { (name, value) ->
                        "$name = ${value ?: "<??>"}"
                    })
                    ?: "<?? missing args>"
                )
}

@Serializable
data class FunExitEvent(
    val funId: String,
    val retVal: String?,
    override val stackParentUid: CFEventUid?
) : ControlFlowEvent() {
    override fun toString(): String = "[$uid ($stackParentUid)] EXIT $funId --> return $retVal"
}

@Serializable
data class LoopEnterEvent(
    val loopLine: LineRef,
    override val stackParentUid: CFEventUid?
) : ControlFlowEvent() {
    // TODO toString
}

@Serializable
data class LoopNewIterEvent(
    val loopLine: LineRef,
    override val stackParentUid: CFEventUid?
) : ControlFlowEvent() {
    // TODO toString
}

@Serializable
data class LoopExitEvent(
    val loopLine: LineRef,
    override val stackParentUid: CFEventUid?
) : ControlFlowEvent() {
    // TODO toString
}

