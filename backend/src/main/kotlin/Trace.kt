import kotlinx.serialization.Serializable

typealias Trace = List<TraceElement>
typealias MutableTrace = MutableList<TraceElement>

@Serializable data class TraceElement(val filename: String, val line: Int, val varsState: Map<String, String>)
