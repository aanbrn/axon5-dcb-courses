// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.command.fixtures.CapacityHarness
import io.github.aanbrn.courses.command.fixtures.CommandServiceTestApplication
import io.github.aanbrn.courses.contracts.commands.SubscribeResult
import io.github.aanbrn.courses.contracts.commands.SubscribeStudent
import io.github.aanbrn.courses.contracts.events.CourseCreated
import io.github.aanbrn.courses.contracts.events.StudentSubscribed
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.StudentId
import io.github.aanbrn.courses.contracts.kernel.Tags
import io.github.aanbrn.courses.testsupport.AxonServerEnvironment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.util.concurrent.TimeUnit
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.eventsourcing.eventstore.TerminalEventMessage
import org.axonframework.messaging.eventhandling.EventMessage
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Proves, through a real Axon Server, that the contracts survive storage (INFRA-06): events are stored under their
 * versioned names, are found by their TypeID tags and by nothing else, are read back equal to what was written, and a
 * sealed command result crosses the command bus as the right case.
 *
 * Everything here is invisible to a test over memory, where nothing is serialized. The stored name decides how an event
 * is replayed for as long as the store exists, the tag decides which decisions see the event at all, and the result
 * type decides whether a rejection reaches the caller as a value. Each test creates its course from fresh IDs and the
 * store is never purged, so the tests are repeatable against the same server (D-16).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AxonServerSerializationIntegrationTest {
    private lateinit var application: CommandServiceTestApplication

    @BeforeAll
    fun startApplication() {
        application =
            CommandServiceTestApplication.onAxonServer(AxonServerEnvironment.address, withCapacityHarness = true)
    }

    @AfterAll
    fun stopApplication() {
        application.close()
    }

    @Test
    fun `events are stored under their versioned names and the terminal entry is not an event`() {
        val course = CourseId.random()
        seedAndSubscribe(course, StudentId.random())

        val stored = source(Tag.of(Tags.COURSE, course.toString()))

        stored.map { it.type().toString() } shouldBe listOf("courses.CourseCreated#1", "courses.StudentSubscribed#1")
    }

    @Test
    fun `the TypeID tag finds the events of a course and the raw UUID finds none`() {
        val course = CourseId.random()
        seedAndSubscribe(course, StudentId.random())

        source(Tag.of(Tags.COURSE, course.toString())).size shouldBe 2
        source(Tag.of(Tags.COURSE, course.uuid.toString())).shouldBeEmpty()
    }

    @Test
    fun `stored payloads read back equal to the events that were written`() {
        val course = CourseId.random()
        val student = StudentId.random()
        seedAndSubscribe(course, student)

        val (created, subscribed) = source(Tag.of(Tags.COURSE, course.toString()))

        created.payloadAs(CourseCreated::class.java, application.messageConverter) shouldBe
            CourseCreated(course, CapacityHarness.DEFAULT_COURSE_NAME, capacity = 1)
        subscribed.payloadAs(StudentSubscribed::class.java, application.messageConverter) shouldBe
            StudentSubscribed(course, student)
    }

    @Test
    fun `a sealed command result returns as the case the handler produced`() {
        val course = CourseId.random()
        application.capacityHarness.seedCourse(course, capacity = 1)

        send(course, StudentId.random()) shouldBe SubscribeResult.Subscribed
        send(course, StudentId.random()) shouldBe SubscribeResult.Rejected(RejectionCode("course-full"))
    }

    private fun seedAndSubscribe(course: CourseId, student: StudentId) {
        application.capacityHarness.seedCourse(course, capacity = 1)
        send(course, student) shouldBe SubscribeResult.Subscribed
    }

    private fun send(course: CourseId, student: StudentId): SubscribeResult =
        application.commandGateway
            .send(SubscribeStudent(course, student))
            .resultAs(SubscribeResult::class.java)
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    /** The events stored under [tag], read in a new unit of work, without the terminal entry that ends the stream. */
    private fun source(tag: Tag): List<EventMessage> =
        application.unitOfWorkFactory
            .create()
            .executeWithResult<List<EventMessage>> { context ->
                application.eventStore
                    .transaction(context)
                    .source(SourcingCondition.conditionFor(EventCriteria.havingTags(tag)))
                    .reduce<List<EventMessage>>(emptyList()) { events, entry -> events.orEmpty() + entry.message() }
                    .thenApply { it.orEmpty() }
            }
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .filter { it !is TerminalEventMessage }

    private companion object {
        const val TIMEOUT_SECONDS = 60L
    }
}
