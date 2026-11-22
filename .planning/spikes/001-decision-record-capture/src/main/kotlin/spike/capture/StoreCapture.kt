package spike.capture

import java.util.function.Consumer
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.EventStoreTransaction
import org.axonframework.eventsourcing.eventstore.Position
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.EventMessage

/**
 * Variant B: decorates the public EventStore and EventStoreTransaction only.
 * The resolved AppendCondition is observed through an identity overrideAppendCondition.
 */
class StoreCapture(private val delegate: EventStore, private val recorder: DecisionRecorder) : EventStore by delegate {

    // StorageEngineBackedEventStore.publish uses its own transaction(), bypassing this decorator; route through ours
    // so that append-only decisions (no sourcing) are captured too.
    override fun publish(context: ProcessingContext?, events: List<EventMessage>): java.util.concurrent.CompletableFuture<Void> {
        if (context != null) transaction(context)
        return delegate.publish(context, events)
    }

    override fun transaction(processingContext: ProcessingContext): EventStoreTransaction {
        val tx = delegate.transaction(processingContext)
        var created = false
        val draft = processingContext.computeResourceIfAbsent(Draft.key(VARIANT)) {
            created = true
            Draft(VARIANT, recorder, processingContext) { tx.appendPosition().toString() }
        }!!
        if (created) {
            tx.onAppend { draft.appendedEvents += Draft.describe(it) }
            tx.overrideAppendCondition { resolved ->
                draft.appendCriteria = resolved.criteria().toString()
                draft.consistencyMarker = resolved.consistencyMarker().toString()
                resolved
            }
        }
        return Tx(tx, draft)
    }

    private class Tx(private val tx: EventStoreTransaction, private val draft: Draft) : EventStoreTransaction by tx {
        override fun source(condition: SourcingCondition, resumePositionCallback: Consumer<Position>?): MessageStream<out EventMessage> {
            draft.sourcingCriteria += condition.criteria().toString()
            return tx.source(condition, resumePositionCallback).onNext { draft.sourcedEvents += Draft.describe(it.message()) }
        }

        override fun source(condition: SourcingCondition): MessageStream<out EventMessage> = source(condition, null)
    }

    companion object {
        const val VARIANT = "B-store"
    }
}
