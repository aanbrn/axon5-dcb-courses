// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.contracts.events.CourseCreated
import io.github.aanbrn.courses.contracts.events.StudentSubscribed
import io.github.aanbrn.courses.contracts.kernel.Tags
import io.github.aanbrn.courses.contracts.kernel.fixtures.TestIds
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
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
    fun `a course creation is tagged with its course only`() {
        val tags = resolver.resolve(event(CourseCreated(course, "Event Modeling", capacity = 10)))

        tags shouldBe setOf(Tag.of(Tags.COURSE, course.toString()))
    }

    @Test
    fun `a payload without tag annotations has no tags`() {
        resolver.resolve(event("a plain string payload")).shouldBeEmpty()
    }

    private fun event(payload: Any) = GenericEventMessage(MessageType(payload::class.java), payload)
}
