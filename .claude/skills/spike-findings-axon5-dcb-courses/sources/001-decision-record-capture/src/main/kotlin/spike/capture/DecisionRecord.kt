package spike.capture

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** What DEMO-01 needs per decision attempt. */
data class DecisionRecord(
    val variant: String,
    val commandId: String?,
    val commandType: String?,
    val attempt: Int,
    val sourcingCriteria: List<String>,
    val sourcedEvents: List<String>,
    val appendCriteria: String?,
    val consistencyMarker: String?,
    val appendedEvents: List<String>,
    val outcome: Outcome,
) {
    sealed interface Outcome {
        data class Committed(val markerAfterCommit: String?) : Outcome
        data object NothingAppended : Outcome
        data class Conflict(val message: String, val conflictingEvent: String?) : Outcome
        data class Failed(val error: String) : Outcome
    }
}

/** Thread-safe sink; attempt numbers are derived from the command identifier, which retries keep. */
class DecisionRecorder {
    val records = CopyOnWriteArrayList<DecisionRecord>()
    private val attempts = ConcurrentHashMap<String, Int>()

    fun nextAttempt(commandId: String?): Int = if (commandId == null) 1 else attempts.merge(commandId, 1, Int::plus)!!

    fun add(record: DecisionRecord) {
        records += record
    }
}
