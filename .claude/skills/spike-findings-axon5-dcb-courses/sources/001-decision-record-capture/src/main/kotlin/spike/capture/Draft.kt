package spike.capture

import java.util.concurrent.CopyOnWriteArrayList
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException
import org.axonframework.messaging.core.Context.ResourceKey
import org.axonframework.messaging.core.Message
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.EventMessage

/** Mutable per-ProcessingContext accumulator shared by both capture variants. */
internal class Draft(
    val variant: String,
    private val recorder: DecisionRecorder,
    ctx: ProcessingContext,
    private val markerAfterCommitSupplier: (() -> String?)? = null,
) {
    private val command: Message? = Message.fromContext(ctx)
    val sourcingCriteria = CopyOnWriteArrayList<String>()
    val sourcedEvents = CopyOnWriteArrayList<String>()
    val appendedEvents = CopyOnWriteArrayList<String>()
    @Volatile var appendCriteria: String? = null
    @Volatile var consistencyMarker: String? = null
    @Volatile var markerAfterCommit: String? = null
    @Volatile var conflictingEvent: String? = null
    @Volatile private var done = false

    init {
        ctx.whenComplete {
            markerAfterCommitSupplier?.let { markerAfterCommit = it() }
            finish(if (appendedEvents.isEmpty()) DecisionRecord.Outcome.NothingAppended else DecisionRecord.Outcome.Committed(markerAfterCommit))
        }
        ctx.onError { _, _, error ->
            finish(classify(error).let { if (it is DecisionRecord.Outcome.Conflict && it.conflictingEvent == null) it.copy(conflictingEvent = conflictingEvent) else it })
        }
    }

    fun finish(outcome: DecisionRecord.Outcome) {
        if (done) return
        done = true
        val id = command?.identifier()
        recorder.add(
            DecisionRecord(
                variant = variant,
                commandId = id,
                commandType = command?.type()?.toString(),
                attempt = recorder.nextAttempt(id),
                sourcingCriteria = sourcingCriteria.toList(),
                sourcedEvents = sourcedEvents.toList(),
                appendCriteria = appendCriteria,
                consistencyMarker = consistencyMarker,
                appendedEvents = appendedEvents.toList(),
                outcome = outcome,
            )
        )
    }

    companion object {
        fun key(variant: String): ResourceKey<Draft> = keys.getOrPut(variant) { ResourceKey.withLabel("decisionDraft-$variant") }
        private val keys = java.util.concurrent.ConcurrentHashMap<String, ResourceKey<Draft>>()

        fun describe(e: EventMessage): String = "${e.type()}:${e.payload()?.let { if (it is ByteArray) String(it) else it }}"

        fun classify(error: Throwable): DecisionRecord.Outcome {
            var t: Throwable? = error
            while (t != null) {
                if (t is AppendEventsTransactionRejectedException) {
                    return DecisionRecord.Outcome.Conflict(t.message ?: "", t.conflictingEvent().map(::describe).orElse(null))
                }
                t = t.cause
            }
            return DecisionRecord.Outcome.Failed("${error.javaClass.simpleName}: ${error.message}")
        }
    }
}

internal fun ProcessingContext.draft(variant: String, recorder: DecisionRecorder): Draft =
    computeResourceIfAbsent(Draft.key(variant)) { Draft(variant, recorder, this) }!!
