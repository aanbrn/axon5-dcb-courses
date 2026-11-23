// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.capture

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.axonframework.eventsourcing.eventstore.AppendCondition
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException
import org.axonframework.eventsourcing.eventstore.ConsistencyMarker
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStorageEngine.AppendTransaction
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.eventsourcing.eventstore.TaggedEventMessage
import org.axonframework.eventsourcing.eventstore.TerminalEventMessage
import org.axonframework.messaging.core.Context.ResourceKey
import org.axonframework.messaging.core.Message
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.EventMessage

/**
 * Records a [DecisionRecord] for every command attempt, from one place around the conditional append, so that no slice
 * contains capture code (D-13).
 *
 * The engine wraps the [EventStorageEngine], the layer that performs the DCB check, and sees what no public seam shows:
 * the exact [AppendCondition] sent to the store, the tags of the appended events, the consistency marker after the
 * commit and the store's verdict. For each unit of work it collects the sourcing criteria, the sourced events and the
 * append, and hands one finished [DecisionRecord] to the [DecisionRecorder] when the unit of work completes or fails.
 * Retries of a command re-dispatch the same command message, so their attempts are numbered by the command identifier.
 *
 * The decorator never changes the outcome of an append. When the store rejects one, it asks the store for the event
 * that caused the conflict, records it and then re-raises the original failure, so the unit of work fails exactly as it
 * would without capture.
 *
 * ### Why this seam, and what keeps it honest
 *
 * [EventStorageEngine] is annotated `@Internal`: Axon may change it in any release. Decorating the public event store
 * instead does not capture the tags of appended events or the conflicting event, and needs four workarounds (spike 001,
 * variant B). Axon's own `ConflictDiagnosingEventStorageEngine` is not used either: on Axon Server the rejection
 * reaches the engine wrapped in a `CompletionException`, which that class does not unwrap, so it never names the
 * conflicting event. This class unwraps the cause chain on both paths: an in-memory engine rejects in [appendEvents],
 * Axon Server rejects in `commit()`. If an Axon update breaks the contract this class relies on,
 * `DecisionCaptureIntegrationTest` fails in the pull request that brings the update; the fallback, stamping records per
 * slice, is recorded in ADR-0006.
 *
 * A source or append without a [ProcessingContext] belongs to no unit of work and no attempt, so it is passed through
 * uncaptured. Every decision runs in a unit of work.
 *
 * @param delegate the engine that stores the events
 * @param recorder receives the record of each finished attempt
 */
// UPSTREAM: UP-0001 Axon offers no public extension point for decision capture; replace the `@Internal` decoration
// when it does.
class DecisionCaptureEngine(
    private val delegate: EventStorageEngine,
    private val recorder: DecisionRecorder,
) : EventStorageEngine by delegate {

    private val attempts = AttemptCounter()

    /**
     * Sources the events for [condition] and notes the criteria and every event the unit of work receives.
     *
     * @param condition what to source
     * @param context the unit of work that sources, or `null` outside one
     * @return the stream of the delegate, which additionally reports its entries to the draft of [context]
     */
    override fun source(condition: SourcingCondition, context: ProcessingContext?): MessageStream<EventMessage> {
        val stream = delegate.source(condition, context)
        if (context == null) return stream
        val draft = draftOf(context)
        draft.sourcingCriteria += condition.criteria().toString()
        return stream.onNext { entry ->
            // The entry that carries the consistency marker is not an event, so it is not part of what was seen.
            if (entry.getResource(ConsistencyMarker.RESOURCE_KEY) == null) {
                draft.sourcedEvents += describe(entry.message())
            }
        }
    }

    /**
     * Notes the append condition and the events to append, then appends them through the delegate.
     *
     * A rejection is diagnosed and re-raised unchanged, both when the delegate rejects here and when the returned
     * transaction rejects in `commit()`.
     *
     * @param condition the condition the store checks the append against
     * @param context the unit of work that appends, or `null` outside one
     * @param events the events to append, with their tags
     * @return the transaction of the delegate, wrapped to diagnose a rejection at commit and to note the marker after
     *   it
     */
    override fun appendEvents(
        condition: AppendCondition,
        context: ProcessingContext?,
        events: List<TaggedEventMessage<*>>,
    ): CompletableFuture<AppendTransaction<*>> {
        val draft = context?.let(::draftOf)
        draft?.noteAppend(condition, events)
        val appended = delegate.appendEvents(condition, context, events)
        if (draft == null) return appended
        return appended
            .exceptionallyCompose { failure -> diagnose<AppendTransaction<*>>(failure, draft, condition, context) }
            .thenApply { transaction -> CapturingTransaction(transaction, draft, condition, context) }
    }

    private fun draftOf(context: ProcessingContext): DecisionDraft =
        checkNotNull(
            context.computeResourceIfAbsent(DRAFT_KEY) { DecisionDraft(context, recorder, attempts) },
        )

    /** Diagnoses a rejection at commit (the Axon Server path) and notes the marker after commit. */
    private inner class CapturingTransaction<R : Any>(
        private val transaction: AppendTransaction<R>,
        private val draft: DecisionDraft,
        private val condition: AppendCondition,
        private val context: ProcessingContext?,
    ) : AppendTransaction<R> by transaction {

        override fun commit(): CompletableFuture<R> =
            transaction.commit().exceptionallyCompose { failure -> diagnose(failure, draft, condition, context) }

        override fun afterCommit(commitResult: R): CompletableFuture<ConsistencyMarker> =
            transaction.afterCommit(commitResult).whenComplete { marker, _ ->
                draft.markerAfterCommit.set(marker?.toString())
            }
    }

    /**
     * When [failure] is a DCB conflict, sources the first event that matches the append criteria beyond the consistency
     * marker and notes it as the conflicting event. Always completes with the original [failure], never with a
     * different outcome.
     */
    // UPSTREAM: UP-0002 Axon's ConflictDiagnosingEventStorageEngine does not unwrap CompletionException on the Axon
    // Server path, so this function searches the whole cause chain itself.
    private fun <T> diagnose(
        failure: Throwable,
        draft: DecisionDraft,
        condition: AppendCondition,
        context: ProcessingContext?,
    ): CompletableFuture<T> {
        if (failure.causeChain().none { it is AppendEventsTransactionRejectedException }) {
            return CompletableFuture.failedFuture(failure)
        }
        val beyondMarker =
            SourcingCondition.conditionFor(condition.consistencyMarker().position(), condition.criteria())
        return delegate
            .source(beyondMarker, context)
            .filter { it.message() !is TerminalEventMessage }
            .first()
            .asCompletableFuture()
            .handle { entry, _ -> draft.conflictingEvent.set(entry?.message()?.let(::describe)) }
            .thenCompose { CompletableFuture.failedFuture(failure) }
    }

    private companion object {
        val DRAFT_KEY: ResourceKey<DecisionDraft> = ResourceKey.withLabel("decisionDraft")
    }
}

/**
 * The facts of one command attempt, collected while its unit of work runs and turned into a [DecisionRecord] when the
 * unit of work ends. The unit of work completes or fails exactly once, so exactly one record is made.
 */
internal class DecisionDraft(
    context: ProcessingContext,
    private val recorder: DecisionRecorder,
    attempts: AttemptCounter,
) {
    private val command: Message? = Message.fromContext(context)
    private val finished = AtomicBoolean()

    val sourcingCriteria = CopyOnWriteArrayList<String>()
    val sourcedEvents = CopyOnWriteArrayList<RecordedEvent>()
    val appendedEvents = CopyOnWriteArrayList<RecordedEvent>()
    val appendCriteria = AtomicReference<String?>()
    val consistencyMarker = AtomicReference<String?>()
    val markerAfterCommit = AtomicReference<String?>()
    val conflictingEvent = AtomicReference<RecordedEvent?>()

    init {
        context.whenComplete {
            finish(
                attempts,
                if (appendedEvents.isEmpty()) {
                    DecisionRecord.Outcome.NothingAppended
                } else {
                    DecisionRecord.Outcome.Committed(markerAfterCommit.get())
                },
            )
        }
        context.onError { _, _, error -> finish(attempts, outcomeOf(error)) }
    }

    /** Notes what the append asked the store to check and what it carries. */
    fun noteAppend(condition: AppendCondition, events: List<TaggedEventMessage<*>>) {
        appendCriteria.set(condition.criteria().toString())
        consistencyMarker.set(condition.consistencyMarker().toString())
        appendedEvents += events.map { describe(it) }
    }

    private fun outcomeOf(error: Throwable): DecisionRecord.Outcome {
        val conflict = error.causeChain().filterIsInstance<AppendEventsTransactionRejectedException>().firstOrNull()
        return if (conflict == null) {
            DecisionRecord.Outcome.Failed("${error.javaClass.simpleName}: ${error.message}")
        } else {
            val reported = conflict.conflictingEvent().map(::describe).orElse(null)
            DecisionRecord.Outcome.Conflict(reported ?: conflictingEvent.get())
        }
    }

    private fun finish(attempts: AttemptCounter, outcome: DecisionRecord.Outcome) {
        if (!finished.compareAndSet(false, true)) return
        val commandId = command?.identifier()
        val record =
            DecisionRecord(
                commandId = commandId,
                commandType = command?.type()?.qualifiedName()?.fullName(),
                attempt = attempts.next(commandId, retryExpected = outcome is DecisionRecord.Outcome.Conflict),
                sourcingCriteria = sourcingCriteria.toList(),
                sourcedEvents = sourcedEvents.toList(),
                appendCriteria = appendCriteria.get(),
                consistencyMarker = consistencyMarker.get(),
                appendedEvents = appendedEvents.toList(),
                outcome = outcome,
            )
        @Suppress(
            "TooGenericExceptionCaught",
        ) // Capture must not change the outcome of a decision, whatever a recorder throws.
        try {
            recorder.record(record)
        } catch (failure: Exception) {
            LOGGER.log(System.Logger.Level.ERROR, "The decision recorder failed for command $commandId", failure)
        }
    }

    private companion object {
        val LOGGER: System.Logger = System.getLogger(DecisionDraft::class.java.name)
    }
}

/**
 * Numbers the attempts of a command by its identifier.
 *
 * A command that ends without a conflict is forgotten, because it is not retried. A command that conflicts is kept for
 * its retry, and the number of commands kept is bounded: when a retry never comes, for example because the retries ran
 * out, the oldest kept command is dropped first instead of the map growing for the life of the service.
 */
internal class AttemptCounter(private val capacity: Int = DEFAULT_CAPACITY) {
    private val attempts =
        object : LinkedHashMap<String, Int>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>): Boolean = size > capacity
        }

    /**
     * Returns the 1-based number of the attempt that just ended.
     *
     * @param commandId the identifier of the command, or `null` outside a command, which is always attempt 1
     * @param retryExpected whether the command is retried after this attempt
     */
    @Synchronized
    fun next(commandId: String?, retryExpected: Boolean): Int {
        if (commandId == null) return 1
        val attempt = (attempts[commandId] ?: 0) + 1
        if (retryExpected) attempts[commandId] = attempt else attempts.remove(commandId)
        return attempt
    }

    private companion object {
        const val DEFAULT_CAPACITY = 10_000
    }
}

private fun Throwable.causeChain(): Sequence<Throwable> = generateSequence(this) { it.cause }

private fun describe(event: EventMessage): RecordedEvent =
    RecordedEvent(event.type().toString(), payloadText(event.payload()))

private fun describe(tagged: TaggedEventMessage<*>): RecordedEvent =
    RecordedEvent(
        type = tagged.event().type().toString(),
        payload = payloadText(tagged.event().payload()),
        tags = tagged.tags().map { RecordedTag(it.key(), it.value()) }.toSet(),
    )

private fun payloadText(payload: Any?): String? =
    if (payload is ByteArray) String(payload, Charsets.UTF_8) else payload?.toString()
