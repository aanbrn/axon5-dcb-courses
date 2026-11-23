// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.command.capture.DecisionRecord
import io.github.aanbrn.courses.command.capture.DecisionRecorder
import io.github.aanbrn.courses.command.capture.RecordedTag
import io.github.aanbrn.courses.command.fixtures.ArrivalBarrier
import io.github.aanbrn.courses.command.fixtures.CollectingDecisionRecorder
import io.github.aanbrn.courses.command.fixtures.CommandServiceTestApplication
import io.github.aanbrn.courses.contracts.commands.SubscribeResult
import io.github.aanbrn.courses.contracts.commands.SubscribeStudent
import io.github.aanbrn.courses.contracts.events.StudentSubscribed
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.StudentId
import io.github.aanbrn.courses.contracts.kernel.Tags
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.axonframework.common.configuration.ComponentNotFoundException
import org.axonframework.common.lifecycle.LifecycleHandlerInvocationException
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The decision-record capture over an in-memory event store, with no container: what a single attempt records, what a
 * conflict records, and that capture never changes the outcome of a decision.
 *
 * The same capture is proven on Axon Server by `DecisionCaptureIntegrationTest`. The in-memory engine rejects a
 * conflicting append in `appendEvents`, where Axon Server rejects it in `commit()`, so this test covers the other of
 * the two diagnosis paths.
 */
class DecisionCaptureTest {
    private val recorder = CollectingDecisionRecorder()
    private val course = CourseId.random()
    private lateinit var application: CommandServiceTestApplication
    private lateinit var pool: ExecutorService

    @BeforeEach
    fun startApplication() {
        application = CommandServiceTestApplication.inMemory(recorder, withCapacityHarness = true)
        pool = Executors.newFixedThreadPool(2)
    }

    @AfterEach
    fun stopApplication() {
        pool.shutdownNow()
        application.close()
    }

    @Test
    fun `a subscription that gets a seat is recorded as committed with what it saw and appended`() {
        val student = StudentId.random()
        seedCourse(capacity = 1)

        subscribe(student) shouldBe SubscribeResult.Subscribed

        val record = recorder.records.single()
        record.commandType shouldBe "courses.SubscribeStudent"
        record.attempt shouldBe 1
        record.sourcingCriteria.single() shouldContain course.toString()
        record.sourcedEvents.map { it.type } shouldBe listOf(COURSE_CREATED_TYPE)
        record.appendCriteria.orEmpty() shouldContain course.toString()
        record.consistencyMarker.orEmpty().shouldNotBeBlank()
        record.appendedEvents.single().type shouldBe STUDENT_SUBSCRIBED_TYPE
        record.appendedEvents.single().tags shouldBe
            setOf(RecordedTag(Tags.COURSE, course.toString()), RecordedTag(Tags.STUDENT, student.toString()))
        record.outcome
            .shouldBeInstanceOf<DecisionRecord.Outcome.Committed>()
            .markerAfterCommit
            .orEmpty()
            .shouldNotBeBlank()
    }

    @Test
    fun `a subscription to a full course is recorded as nothing appended, with the two events it saw`() {
        seedCourse(capacity = 1)
        subscribe(StudentId.random()) shouldBe SubscribeResult.Subscribed
        recorder.clear()

        subscribe(StudentId.random()) shouldBe SubscribeResult.Rejected(RejectionCode("course-full"))

        val record = recorder.records.single()
        record.outcome shouldBe DecisionRecord.Outcome.NothingAppended
        record.sourcedEvents.map { it.type } shouldBe listOf(COURSE_CREATED_TYPE, STUDENT_SUBSCRIBED_TYPE)
        record.appendedEvents shouldHaveSize 0
        record.consistencyMarker shouldBe null
    }

    @Test
    fun `two interleaved decisions on one course record one commit and one conflict that names the other's event`() {
        seedCourse(capacity = 2)
        val barrier = ArrivalBarrier(parties = 2)

        val failures =
            List(2) { pool.submitDecision(StudentId.random(), barrier) }
                .map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }

        val winner = recorder.committed().single()
        val loser = recorder.conflicts().single().outcome.shouldBeInstanceOf<DecisionRecord.Outcome.Conflict>()
        val conflictingEvent = checkNotNull(loser.conflictingEvent)
        conflictingEvent.type shouldBe STUDENT_SUBSCRIBED_TYPE
        conflictingEvent.payload.orEmpty() shouldContain
            winner.appendedEvents.single().tags.single { it.key == Tags.STUDENT }.value
        failures.count { it == null } shouldBe 1
    }

    @Test
    fun `a conflict is re-raised unchanged, so the losing unit of work still fails with the rejection`() {
        seedCourse(capacity = 2)
        val barrier = ArrivalBarrier(parties = 2)

        val failure =
            List(2) { pool.submitDecision(StudentId.random(), barrier) }
                .map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
                .filterNotNull()
                .single()

        generateSequence(failure) { it.cause }.any { it is AppendEventsTransactionRejectedException } shouldBe true
    }

    @Test
    fun `a recorder that throws does not change the outcome of the decision`() {
        application.close()
        application =
            CommandServiceTestApplication.inMemory(
                DecisionRecorder { error("recorder is broken") },
                withCapacityHarness = true,
            )
        seedCourse(capacity = 1)

        subscribe(StudentId.random()) shouldBe SubscribeResult.Subscribed
    }

    @Test
    fun `an application without a decision recorder does not start, so capture is never silently off`() {
        val configurer =
            EventSourcingConfigurer.create().componentRegistry { registry ->
                registry.registerEnhancer(CommandServiceConfigurationEnhancer())
                registry.registerComponent(EventStorageEngine::class.java) { InMemoryEventStorageEngine() }
            }

        val failure = shouldThrow<LifecycleHandlerInvocationException> { configurer.start() }

        generateSequence<Throwable>(failure) { it.cause }
            .filterIsInstance<ComponentNotFoundException>()
            .single()
            .message
            .orEmpty() shouldContain DecisionRecorder::class.java.name
    }

    private fun seedCourse(capacity: Int) {
        application.capacityHarness.seedCourse(course, capacity)
        recorder.clear()
    }

    private fun subscribe(student: StudentId): SubscribeResult =
        application.commandGateway
            .send(SubscribeStudent(course, student))
            .resultAs(SubscribeResult::class.java)
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    /**
     * Runs one decision in a unit of work on a pool thread: source the course, wait at the [barrier] until both
     * decisions have sourced, then append that [student] subscribed.
     *
     * @return the future of the failure of the unit of work, or `null` when it committed
     */
    private fun ExecutorService.submitDecision(
        student: StudentId,
        barrier: ArrivalBarrier,
    ): CompletableFuture<Throwable?> =
        CompletableFuture.supplyAsync(
            {
                runCatching {
                    application.unitOfWorkFactory
                        .create()
                        .executeWithResult { context ->
                            application.eventStore
                                .transaction(context)
                                .source(
                                    SourcingCondition.conditionFor(
                                        EventCriteria.havingTags(Tag.of(Tags.COURSE, course.toString())),
                                    ),
                                )
                                .reduce(0) { count, _ -> (count ?: 0) + 1 }
                                .thenCompose { barrier.arrive() }
                                .thenApply {
                                    EventAppender.forContext(context).append(StudentSubscribed(course, student))
                                }
                        }
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }
                    .exceptionOrNull()
            },
            this,
        )

    private companion object {
        const val TIMEOUT_SECONDS = 30L
        const val COURSE_CREATED_TYPE = "courses.CourseCreated#1"
        const val STUDENT_SUBSCRIBED_TYPE = "courses.StudentSubscribed#1"
    }
}
