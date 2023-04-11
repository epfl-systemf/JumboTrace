import kotlinx.serialization.Serializable

@Serializable data class LineRef(val filename: String, val lineIdx: Int)
