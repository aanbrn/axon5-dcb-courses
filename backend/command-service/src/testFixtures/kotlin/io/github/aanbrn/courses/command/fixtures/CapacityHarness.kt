// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.fixtures

import io.github.aanbrn.courses.contracts.commands.SubscribeResult
import io.github.aanbrn.courses.contracts.commands.SubscribeStudent
import io.github.aanbrn.courses.contracts.events.CourseCreated
import io.github.aanbrn.courses.contracts.events.StudentSubscribed
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.MessageNamespace
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.Tags
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.core.QualifiedName
import org.axonframework.messaging.core.conversion.MessageConverter
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag

/**
 * A hand-written capacity decision for tests of the capture and the retry: subscribe a student to a course while seats
 * remain.
 *
 * This is test infrastructure, not a production decision: the command service has no command handler before Phase 3,
 * and the real decision is a vertical slice. The harness exists so that the capture decorator and the conflict retry
 * can be proven under real concurrency without waiting for the slices. It sources the events tagged with the course
 * through the public [EventStore], folds the capacity and the subscriptions, and then rejects with `course-not-found`
 * or `course-full`, or appends [StudentSubscribed] and returns [SubscribeResult.Subscribed].
 *
 * The [beforeAppend] hook runs after the decision has sourced and before it appends. A test uses [ArrivalBarrier] there
 * to hold the first round of concurrent attempts back until all of them sourced, so every one of them decides on the
 * same events and the conflicts are certain instead of depending on timing.
 *
 * @param eventStore the store the decision sources from
 * @param unitOfWorkFactory creates the unit of work in which [seedCourse] appends
 * @param converter reads the payloads of sourced events, which are bytes when they come from Axon Server
 * @param beforeAppend completes when the decision may append; it must not block a thread, because the thread that waits
 *   could be the one that completes another attempt
 */
class CapacityHarness(
    private val eventStore: EventStore,
    private val unitOfWorkFactory: UnitOfWorkFactory,
    private val converter: MessageConverter,
    private val beforeAppend: () -> CompletableFuture<*>,
) {

    /**
     * Decides whether the student of [command] gets a seat.
     *
     * @param command the subscription to decide
     * @param context the unit of work of the command, in which the decision sources and appends
     * @return the result of the decision, after the append has been handed to the store
     */
    @CommandHandler
    fun handle(command: SubscribeStudent, context: ProcessingContext): CompletableFuture<SubscribeResult> {
        val criteria = EventCriteria.havingTags(Tag.of(Tags.COURSE, command.courseId.toString()))
        return eventStore
            .transaction(context)
            .source(SourcingCondition.conditionFor(criteria))
            .reduce(CourseFacts()) { facts, entry -> (facts ?: CourseFacts()).plus(entry.message(), converter) }
            .thenCompose { facts -> beforeAppend().thenApply { facts } }
            .thenApply { facts -> decide(command, context, checkNotNull(facts)) }
    }

    /**
     * Creates [courseId] with [capacity] seats, in a unit of work of its own that sources nothing.
     *
     * @param courseId the course to create
     * @param capacity the number of seats
     * @param name the display name of the course
     */
    fun seedCourse(courseId: CourseId, capacity: Int, name: String = DEFAULT_COURSE_NAME) {
        unitOfWorkFactory
            .create()
            .executeWithResult { context ->
                EventAppender.forContext(context).append(CourseCreated(courseId, name, capacity))
                CompletableFuture.completedFuture(Unit)
            }
            .get(SEED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun decide(command: SubscribeStudent, context: ProcessingContext, facts: CourseFacts): SubscribeResult =
        when {
            facts.capacity == null -> SubscribeResult.Rejected(RejectionCode(COURSE_NOT_FOUND))
            facts.subscribed >= facts.capacity -> SubscribeResult.Rejected(RejectionCode(COURSE_FULL))
            else -> {
                EventAppender.forContext(context).append(StudentSubscribed(command.courseId, command.studentId))
                SubscribeResult.Subscribed
            }
        }

    /** What the sourced events say about one course: its capacity once created, and how many seats are taken. */
    private data class CourseFacts(val capacity: Int? = null, val subscribed: Int = 0) {

        fun plus(event: EventMessage, converter: MessageConverter): CourseFacts =
            when (event.type().qualifiedName()) {
                COURSE_CREATED_NAME ->
                    copy(capacity = checkNotNull(event.payloadAs(CourseCreated::class.java, converter)).capacity)
                STUDENT_SUBSCRIBED_NAME -> copy(subscribed = subscribed + 1)
                else -> this
            }
    }

    /** The rejection codes of the harness. */
    companion object {
        /** The course has not been created. */
        const val COURSE_NOT_FOUND = "course-not-found"

        /** Every seat of the course is taken. */
        const val COURSE_FULL = "course-full"

        /** The display name [seedCourse] gives a course unless the test names it. */
        const val DEFAULT_COURSE_NAME = "Event Modeling"

        private const val SEED_TIMEOUT_SECONDS = 60L
        private val COURSE_CREATED_NAME = QualifiedName(MessageNamespace.COURSES, "CourseCreated")
        private val STUDENT_SUBSCRIBED_NAME = QualifiedName(MessageNamespace.COURSES, "StudentSubscribed")
    }
}

/**
 * Completes once [parties] callers have arrived, without blocking any of them, and stays complete.
 *
 * It is the [CapacityHarness] `beforeAppend` hook of a concurrency test. A blocking latch would hold a handler thread,
 * and with more attempts than handler threads the threads that wait could be the ones that must complete the sourcing
 * of the attempts that have not arrived yet. Callers after the first [parties] pass at once, so the retries that follow
 * the first round run unhindered.
 *
 * @param parties the number of attempts to hold back, normally the number of concurrent commands
 */
class ArrivalBarrier(private val parties: Int) {
    private val arrived = AtomicInteger()
    private val released = CompletableFuture<Unit>()

    /** Registers one arrival and returns the future that completes when [parties] have arrived. */
    fun arrive(): CompletableFuture<Unit> {
        if (arrived.incrementAndGet() == parties) released.complete(Unit)
        return released
    }
}
