// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.command.fixtures.CommandServiceTestApplication
import io.github.aanbrn.courses.contracts.events.CourseCreated
import io.github.aanbrn.courses.contracts.events.StudentSubscribed
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.StudentId
import io.github.aanbrn.courses.contracts.kernel.Tags
import io.github.aanbrn.courses.testsupport.AxonServerEnvironment
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Proves, on a real Axon Server with a DCB context, that a conditional append is checked against the criteria that the
 * decision sourced (INFRA-04): an append whose consistency marker is outdated for overlapping criteria is rejected, and
 * an append whose criteria do not overlap the concurrent one commits.
 *
 * Each test creates its courses from fresh IDs and the store is never purged, so the tests are repeatable against the
 * same server and do not see each other's events (D-16). Two units of work run concurrently from a two-thread pool, and
 * a barrier holds both back until each has sourced its criteria, so both append against the same, soon outdated marker.
 * Without that barrier, the first unit could commit before the second one sourced anything, and no conflict would exist
 * to detect.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionalAppendIntegrationTest {
    private lateinit var application: CommandServiceTestApplication
    private lateinit var pool: ExecutorService

    @BeforeAll
    fun startApplication() {
        application = CommandServiceTestApplication.onAxonServer(AxonServerEnvironment.address)
        pool = Executors.newFixedThreadPool(CONCURRENT_UNITS)
    }

    @AfterAll
    fun stopApplication() {
        pool.shutdownNow()
        application.close()
    }

    @Test
    fun `an append with an outdated marker for overlapping criteria is rejected`() {
        val course = CourseId.random()
        seed(CourseCreated(course, "Event Modeling", capacity = CAPACITY))
        val barrier = SourcingBarrier(CONCURRENT_UNITS)

        val outcomes =
            listOf(StudentId.random(), StudentId.random())
                .map { student -> pool.submitDecision(course, student, barrier) }
                .map { it.await() }

        outcomes.count { it == null } shouldBe 1
        val failure = outcomes.filterNotNull().single()
        failure.causeChain().any { it is AppendEventsTransactionRejectedException } shouldBe true
        sourceEventNames(course) shouldContainExactlyInAnyOrder listOf(COURSE_CREATED, STUDENT_SUBSCRIBED)
    }

    @Test
    fun `appends whose criteria do not overlap both commit`() {
        val courseA = CourseId.random()
        val courseB = CourseId.random()
        seed(
            CourseCreated(courseA, "Aggregates", capacity = CAPACITY),
            CourseCreated(courseB, "Dynamic Boundaries", CAPACITY),
        )
        val barrier = SourcingBarrier(CONCURRENT_UNITS)

        val outcomes =
            listOf(courseA, courseB)
                .map { course -> pool.submitDecision(course, StudentId.random(), barrier) }
                .map {
                    it.await()
                }

        outcomes shouldBe listOf(null, null)
        sourceEventNames(courseA) shouldContainExactlyInAnyOrder listOf(COURSE_CREATED, STUDENT_SUBSCRIBED)
        sourceEventNames(courseB) shouldContainExactlyInAnyOrder listOf(COURSE_CREATED, STUDENT_SUBSCRIBED)
    }

    /** Appends [events] in a unit of work that sourced nothing, so the append carries no condition. */
    private fun seed(vararg events: Any) {
        application.unitOfWorkFactory
            .create()
            .executeWithResult { context ->
                EventAppender.forContext(context).append(*events)
                CompletableFuture.completedFuture(Unit)
            }
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /**
     * Runs one decision on a pool thread: source the events tagged with [course], wait at the [barrier] until every
     * decision has sourced, then append that [student] subscribed.
     *
     * @return the future of the failure of the unit of work, or `null` when it committed
     */
    private fun ExecutorService.submitDecision(
        course: CourseId,
        student: StudentId,
        barrier: SourcingBarrier,
    ): CompletableFuture<Throwable?> =
        CompletableFuture.supplyAsync(
            {
                runCatching {
                    application.unitOfWorkFactory
                        .create()
                        .executeWithResult { context ->
                            application.eventStore
                                .transaction(context)
                                .source(SourcingCondition.conditionFor(criteria(course)))
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

    /** The names of the course events stored for [course], read in a new unit of work. */
    private fun sourceEventNames(course: CourseId): List<String> =
        application.unitOfWorkFactory
            .create()
            .executeWithResult<List<String>> { context ->
                application.eventStore
                    .transaction(context)
                    .source(SourcingCondition.conditionFor(criteria(course)))
                    .reduce<List<String>>(emptyList()) { names, entry ->
                        names.orEmpty() + entry.message().type().qualifiedName().localName()
                    }
                    .thenApply { it.orEmpty() }
            }
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .filter { it == COURSE_CREATED || it == STUDENT_SUBSCRIBED }

    private fun criteria(course: CourseId): EventCriteria =
        EventCriteria.havingTags(Tag.of(Tags.COURSE, course.toString()))

    private fun CompletableFuture<Throwable?>.await(): Throwable? = get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private fun Throwable.causeChain(): Sequence<Throwable> = generateSequence(this) { it.cause }

    /**
     * Completes once [parties] decisions have arrived, without blocking any of them: a decision that waits on a
     * blocking latch could occupy the very thread that must complete the sourcing of the other one.
     */
    private class SourcingBarrier(private val parties: Int) {
        private val arrived = AtomicInteger()
        private val released = CompletableFuture<Unit>()

        /** Registers one sourced decision; the returned future completes when all have arrived. */
        fun arrive(): CompletableFuture<Unit> {
            if (arrived.incrementAndGet() == parties) released.complete(Unit)
            return released
        }
    }

    private companion object {
        const val CONCURRENT_UNITS = 2
        const val CAPACITY = 10
        const val TIMEOUT_SECONDS = 60L
        const val COURSE_CREATED = "CourseCreated"
        const val STUDENT_SUBSCRIBED = "StudentSubscribed"
    }
}
