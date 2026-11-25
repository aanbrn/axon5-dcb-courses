// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.contracts.events.CourseCreated
import io.github.aanbrn.courses.contracts.events.StudentSubscribed
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.Tags
import io.github.aanbrn.courses.contracts.kernel.fixtures.TestIds
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.eventsourcing.eventstore.AnnotationBasedTagResolver
import org.axonframework.messaging.core.MessageType
import org.axonframework.messaging.eventhandling.GenericEventMessage
import org.axonframework.messaging.eventstreaming.Tag
import org.junit.jupiter.api.Test

class KotlinTagResolverTest {
    private val resolver = KotlinTagResolver()
    private val course = TestIds.courseId(1)
    private val student = TestIds.studentId(2)

    @Test
    fun `a subscription is tagged with the TypeIDs of its course and its student`() {
        val tags = resolver.resolve(event(StudentSubscribed(course, student)))

        tags shouldBe setOf(Tag.of(Tags.COURSE, course.toString()), Tag.of(Tags.STUDENT, student.toString()))
    }

    @Test
    fun `no tag carries the raw UUID of a value class`() {
        val tags = resolver.resolve(event(StudentSubscribed(course, student)))

        tags.forEach { it.value() shouldNotContain course.uuid.toString() }
        tags.forEach { it.value() shouldNotContain student.uuid.toString() }
    }

    @Test
    fun `Axon's own resolver tags the raw UUID, which is why the custom resolver exists`() {
        val axonTags = AnnotationBasedTagResolver().resolve(event(StudentSubscribed(course, student)))
        val ourTags = resolver.resolve(event(StudentSubscribed(course, student)))

        axonTags shouldBe
            setOf(Tag.of(Tags.COURSE, course.uuid.toString()), Tag.of(Tags.STUDENT, student.uuid.toString()))
        ourTags shouldNotBe axonTags
    }

    @Test
    fun `a course creation is tagged with its course only`() {
        val tags = resolver.resolve(event(CourseCreated(course, "Event Modeling", capacity = 10)))

        tags shouldBe setOf(Tag.of(Tags.COURSE, course.toString()))
    }

    @Test
    fun `a payload without tag annotations has no tags`() {
        resolver.resolve(event("a plain string payload")).shouldBeEmpty()
    }

    @Test
    fun `a private tagged property is read, as Axon's own resolver reads it`() {
        val tags = resolver.resolve(event(PrivatelyTagged(course)))

        tags shouldBe setOf(Tag.of(Tags.COURSE, course.toString()))
    }

    private fun event(payload: Any) = GenericEventMessage(MessageType(payload::class.java), payload)
}

private class PrivatelyTagged(@EventTag(key = Tags.COURSE) private val course: CourseId) {
    override fun toString(): String = "PrivatelyTagged($course)"
}
