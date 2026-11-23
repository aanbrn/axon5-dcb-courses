// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.capture

import io.github.aanbrn.courses.command.fixtures.CollectingDecisionRecorder
import io.github.aanbrn.courses.command.fixtures.CommandServiceTestApplication
import io.github.aanbrn.courses.contracts.events.CourseCreated
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.MessageNamespace
import io.github.aanbrn.courses.contracts.kernel.Tags
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.axonframework.eventsourcing.eventstore.AppendCondition
import org.axonframework.eventsourcing.eventstore.GenericTaggedEventMessage
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.eventsourcing.eventstore.TaggedEventMessage
import org.axonframework.eventsourcing.eventstore.TerminalEventMessage
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.core.MessageType
import org.axonframework.messaging.eventhandling.GenericEventMessage
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The overloads of [DecisionCaptureEngine] that Axon declares as Java default methods: the `vararg` append must be
 * captured, because delegating it straight to the wrapped engine would skip capture, and the context-free source must
 * pass through without a record.
 */
class DecisionCaptureEngineTest {
    private val recorder = CollectingDecisionRecorder()
    private val course = CourseId.random()
    private val delegate = InMemoryEventStorageEngine()
    private val engine = DecisionCaptureEngine(delegate, recorder)
    private lateinit var application: CommandServiceTestApplication

    @BeforeEach
    fun startApplication() {
        application = CommandServiceTestApplication.inMemory()
    }

    @AfterEach
    fun stopApplication() {
        application.close()
    }

    @Test
    fun `an append through the vararg overload is captured with the type and tags of the event`() {
        val event = courseCreated()

        application.unitOfWorkFactory
            .create()
            .executeWithResult { context ->
                engine.appendEvents(AppendCondition.none(), context, event).thenApply {}
            }
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        val appended = recorder.records.single().appendedEvents.single()
        appended.type shouldBe COURSE_CREATED_TYPE
        appended.tags shouldBe setOf(RecordedTag(Tags.COURSE, course.toString()))
    }

    @Test
    fun `a source without a processing context returns the stored events and records nothing`() {
        delegate
            .appendEvents(AppendCondition.none(), null, listOf<TaggedEventMessage<*>>(courseCreated()))
            .thenCompose { it.commit() }
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val condition = SourcingCondition.conditionFor(EventCriteria.havingTags(Tag.of(Tags.COURSE, course.toString())))

        val sourced: CompletableFuture<Int?> =
            engine.source(condition).reduce(0) { count, entry ->
                if (entry.message() is TerminalEventMessage) count else (count ?: 0) + 1
            }

        sourced.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe 1
        recorder.records shouldBe emptyList()
    }

    private fun courseCreated(): TaggedEventMessage<*> =
        GenericTaggedEventMessage(
            GenericEventMessage(
                MessageType(MessageNamespace.COURSES, "CourseCreated", "1"),
                CourseCreated(course, "Event Modeling", 10),
            ),
            setOf(Tag.of(Tags.COURSE, course.toString())),
        )

    private companion object {
        const val TIMEOUT_SECONDS = 30L
        const val COURSE_CREATED_TYPE = "courses.CourseCreated#1"
    }
}
