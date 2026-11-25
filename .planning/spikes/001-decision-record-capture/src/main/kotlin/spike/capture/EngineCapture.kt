package spike.capture

import java.util.concurrent.CompletableFuture
import org.axonframework.eventsourcing.eventstore.AppendCondition
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException
import org.axonframework.eventsourcing.eventstore.TerminalEventMessage
import org.axonframework.eventsourcing.eventstore.ConsistencyMarker
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStorageEngine.AppendTransaction
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.eventsourcing.eventstore.TaggedEventMessage
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.EventMessage

/**
 * Variant A: decorates the @Internal EventStorageEngine — the layer that performs the conditional append.
 * Sees the exact AppendCondition sent to the store and the store's verdict.
 */
class EngineCapture(private val delegate: EventStorageEngine, private val recorder: DecisionRecorder) :
    EventStorageEngine by delegate {

    override fun source(condition: SourcingCondition, context: ProcessingContext?): MessageStream<EventMessage> {
        val stream = delegate.source(condition, context)
        if (context == null) return stream
        val draft = context.draft(VARIANT, recorder)
        draft.sourcingCriteria += condition.criteria().toString()
        return stream.onNext { entry ->
            if (entry.getResource(ConsistencyMarker.RESOURCE_KEY) == null) draft.sourcedEvents += Draft.describe(entry.message())
        }
    }

    override fun appendEvents(
        condition: AppendCondition,
        context: ProcessingContext?,
        events: List<TaggedEventMessage<*>>,
    ): CompletableFuture<AppendTransaction<*>> {
        val draft = context?.draft(VARIANT, recorder)
        draft?.apply {
            appendCriteria = condition.criteria().toString()
            consistencyMarker = condition.consistencyMarker().toString()
            appendedEvents += events.map { "${Draft.describe(it.event())} tags=${it.tags()}" }
        }
        return delegate.appendEvents(condition, context, events)
            .exceptionallyCompose { failure -> if (draft == null) CompletableFuture.failedFuture(failure) else diagnose(failure, draft, condition, context) }
            .thenApply { tx -> if (draft == null) tx else Tx(tx, draft, condition, context) }
    }

    /** Diagnoses commit-time conflicts (Axon Server path) and records the marker after commit. */
    private inner class Tx<R : Any>(
        private val tx: AppendTransaction<R>,
        private val draft: Draft,
        private val condition: AppendCondition,
        private val context: ProcessingContext?,
    ) : AppendTransaction<R> by tx {
        override fun commit(): CompletableFuture<R> =
            tx.commit().exceptionallyCompose { failure -> diagnose(failure, draft, condition, context) }

        override fun afterCommit(commitResult: R): CompletableFuture<ConsistencyMarker> =
            tx.afterCommit(commitResult).whenComplete { marker, _ -> draft.markerAfterCommit = marker?.toString() }
    }

    /**
     * On a DCB conflict, sources the first event matching the append criteria beyond the consistency marker - what
     * ConflictDiagnosingEventStorageEngine does, but unwrapping CompletionException, which that class misses on the
     * Axon Server path (the rejection surfaces from commit() wrapped). The in-memory engine rejects in appendEvents().
     */
    private fun <T> diagnose(failure: Throwable, draft: Draft, condition: AppendCondition, context: ProcessingContext?): CompletableFuture<T> {
        generateSequence(failure) { it.cause }.filterIsInstance<AppendEventsTransactionRejectedException>().firstOrNull()
            ?: return CompletableFuture.failedFuture(failure)
        return delegate.source(SourcingCondition.conditionFor(condition.consistencyMarker().position(), condition.criteria()), context)
            .filter { it.message() !is TerminalEventMessage }
            .first()
            .asCompletableFuture()
            .handle { entry, _ -> draft.conflictingEvent = entry?.message()?.let(Draft::describe) }
            .thenCompose { CompletableFuture.failedFuture(failure) }
    }

    companion object {
        const val VARIANT = "A-engine"
    }
}
