data class StopInfo(
    val lineRef: LineRef?,
    val lastKnownEnclosingCall: FunCallEvent?,
    val stackDepth: Int?
){
    fun isNotYetStopped(): Boolean = (lineRef == null && lastKnownEnclosingCall == null && stackDepth == null)
}
