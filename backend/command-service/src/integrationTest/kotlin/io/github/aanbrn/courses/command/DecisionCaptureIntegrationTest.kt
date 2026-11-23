// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.command.capture.DecisionRecord
import io.github.aanbrn.courses.command.fixtures.ArrivalBarrier
import io.github.aanbrn.courses.command.fixtures.CapacityHarness
import io.github.aanbrn.courses.command.fixtures.CollectingDecisionRecorder
import io.github.aanbrn.courses.command.fixtures.CommandServiceTestApplication
import io.github.aanbrn.courses.contracts.commands.SubscribeResult
import io.github.aanbrn.courses.contracts.commands.SubscribeStudent
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.StudentId
import io.github.aanbrn.courses.testsupport.AxonServerEnvironment
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The permanent safeguard of the decision-record capture (D-13): under real concurrency on Axon Server, every command
 * attempt is recorded with its criteria, marker, events and outcome, conflicting appends are retried transparently, and
 * the capacity of the course still holds exactly.
 *
 * The capture decorates Axon's `@Internal` `EventStorageEngine`, which Axon may change in any release. This test runs
 * in the integration job on every Renovate pull request, so an Axon update that breaks what the decorator relies on,
 * such as where the conflict surfaces or what it wraps, fails here and not in production.
 *
 * Twelve students subscribe to a course of three seats from twelve threads, because a single dispatching thread
 * serialises the commands in memory and produces no conflict at all. An [ArrivalBarrier] holds the first twelve
 * attempts back until each has sourced the course, so all of them decide on the same events and the first round
 * conflicts for certain. The retries that follow run unhindered.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecisionCaptureIntegrationTest {
    private val recorder = CollectingDecisionRecorder()
    private val barrier = AtomicReference(ArrivalBarrier(STUDENTS))
    private lateinit var application: CommandServiceTestApplication
    private lateinit var pool: ExecutorService

    @BeforeAll
    fun startApplication() {
        application =
            CommandServiceTestApplication.onAxonServer(
                AxonServerEnvironment.address,
                recorder,
                withCapacityHarness = true,
                beforeAppend = { barrier.get().arrive() },
            )
        pool = Executors.newFixedThreadPool(STUDENTS)
    }

    @AfterAll
    fun stopApplication() {
        pool.shutdownNow()
        application.close()
    }

    @Test
    fun `concurrent subscriptions conflict, are retried and never exceed the capacity`() {
        val course = CourseId.random()
        application.capacityHarness.seedCourse(course, SEATS)
        recorder.clear()

        val results = subscribeConcurrently(course)

        results.count { it == SubscribeResult.Subscribed } shouldBe SEATS
        results.count { it == COURSE_FULL } shouldBe STUDENTS - SEATS
        recorder.committed() shouldHaveSize SEATS
        recorder.conflicts().forEach { it.conflictingEventType() shouldBe STUDENT_SUBSCRIBED_TYPE }
        recorder.conflicts().isNotEmpty() shouldBe true
        recorder.records
            .filter { it.attempt > 1 }
            .forEach { retry ->
                recorder.ofCommand(retry.commandId.orEmpty()).any { earlier ->
                    earlier.attempt == retry.attempt - 1 && earlier.outcome is DecisionRecord.Outcome.Conflict
                } shouldBe true
            }
    }

    @Test
    fun `a committed attempt records what it saw, what it asked the store and what it appended`() {
        val course = CourseId.random()
        application.capacityHarness.seedCourse(course, SEATS)
        recorder.clear()

        subscribeConcurrently(course)

        recorder.committed() shouldHaveSize SEATS
        recorder.committed().forEach { record ->
            record.commandType shouldBe SUBSCRIBE_COMMAND
            record.sourcingCriteria.single() shouldContain course.toString()
            record.appendCriteria.orEmpty() shouldContain course.toString()
            record.consistencyMarker.orEmpty().shouldNotBeBlank()
            record.appendedEvents.single().type shouldBe STUDENT_SUBSCRIBED_TYPE
            record.outcome
                .shouldBeInstanceOf<DecisionRecord.Outcome.Committed>()
                .markerAfterCommit
                .orEmpty()
                .shouldNotBeBlank()
        }
    }

    /**
     * Subscribes [STUDENTS] new students to [course] from [STUDENTS] threads and returns their results. Every call
     * holds back its own first round at a new barrier, so the tests do not depend on their order.
     */
    private fun subscribeConcurrently(course: CourseId): List<SubscribeResult> {
        barrier.set(ArrivalBarrier(STUDENTS))
        return List(STUDENTS) { subscribe(course, StudentId.random()) }
            .map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
    }

    private fun subscribe(course: CourseId, student: StudentId): CompletableFuture<SubscribeResult> =
        CompletableFuture.supplyAsync(
            {
                application.commandGateway
                    .send(SubscribeStudent(course, student))
                    .resultAs(SubscribeResult::class.java)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            },
            pool,
        )

    private fun DecisionRecord.conflictingEventType(): String? =
        outcome.shouldBeInstanceOf<DecisionRecord.Outcome.Conflict>().conflictingEvent?.type

    private companion object {
        const val STUDENTS = 12
        const val SEATS = 3
        const val TIMEOUT_SECONDS = 60L
        const val STUDENT_SUBSCRIBED_TYPE = "courses.StudentSubscribed#1"
        const val SUBSCRIBE_COMMAND = "courses.SubscribeStudent"
        val COURSE_FULL = SubscribeResult.Rejected(RejectionCode(CapacityHarness.COURSE_FULL))
    }
}
