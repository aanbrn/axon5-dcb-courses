// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.serialization

import io.github.aanbrn.courses.contracts.commands.SubscribeResult
import io.github.aanbrn.courses.contracts.commands.SubscribeStudent
import io.github.aanbrn.courses.contracts.events.CourseCreated
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.fixtures.TestIds
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tools.jackson.core.JacksonException

/**
 * Round trips of the contract messages through the shared Jackson mapper (INFRA-06).
 *
 * Every test has a block body: a test that returns a value other than `Unit` is silently skipped by JUnit.
 */
class JsonRoundTripTest {
    private val mapper = coursesJsonMapper()

    private fun roundTrip(event: CourseCreated): CourseCreated =
        mapper.readValue(mapper.writeValueAsString(event), CourseCreated::class.java)

    @Test
    fun `a course created event round trips unchanged`() {
        val event = CourseCreated(TestIds.courseId(1), "Domain-Driven Design", CAPACITY)

        roundTrip(event) shouldBe event
    }

    @Test
    fun `the JSON of an event holds the canonical TypeID and no UUID text`() {
        val id = TestIds.courseId(1)

        val json = mapper.writeValueAsString(CourseCreated(id, "Domain-Driven Design", CAPACITY))

        json shouldBe """{"courseId":"$id","name":"Domain-Driven Design","capacity":$CAPACITY}"""
        json shouldContain "\"courseId\":\"course_"
        json shouldNotContain id.uuid.toString()
    }

    @Test
    fun `an ID of another entity type in a course field fails deserialisation`() {
        val json = """{"courseId":"${TestIds.studentId(1)}","name":"Domain-Driven Design","capacity":$CAPACITY}"""

        val failure = shouldThrow<JacksonException> { mapper.readValue(json, CourseCreated::class.java) }

        failure.message shouldContain "Wrong TypeID prefix"
    }

    @Test
    fun `a malformed ID in JSON fails deserialisation`() {
        val json = """{"courseId":"course_not-a-typeid","name":"Domain-Driven Design","capacity":$CAPACITY}"""

        val failure = shouldThrow<JacksonException> { mapper.readValue(json, CourseCreated::class.java) }

        failure.message shouldContain "Malformed TypeID"
    }

    @Test
    fun `IDs built from the nil UUID and from the all-ones UUID round trip`() {
        for (uuid in listOf(UUID(0L, 0L), UUID(-1L, -1L))) {
            val event = CourseCreated(CourseId(uuid), "Boundary", CAPACITY)

            roundTrip(event).courseId.uuid shouldBe uuid
        }
    }

    @Test
    fun `ten thousand generated UUIDs survive the round trip with full precision`() {
        runBlocking<Unit> {
            checkAll(PRECISION_ROUNDS, Arb.uuid()) { uuid ->
                roundTrip(CourseCreated(CourseId(uuid), "Precision", CAPACITY)).courseId.uuid shouldBe uuid
            }
        }
    }

    @Test
    fun `a subscribe command round trips unchanged`() {
        val command = SubscribeStudent(TestIds.courseId(1), TestIds.studentId(1))

        val json = mapper.writeValueAsString(command)

        json shouldBe """{"courseId":"${command.courseId}","studentId":"${command.studentId}"}"""
        mapper.readValue(json, SubscribeStudent::class.java) shouldBe command
    }

    @Test
    fun `the subscribed result is written as its type name alone`() {
        mapper.writeValueAsString(SubscribeResult.Subscribed) shouldBe SUBSCRIBED_JSON
    }

    @Test
    fun `the subscribed result reads back as the same singleton`() {
        val result = mapper.readValue(SUBSCRIBED_JSON, SubscribeResult::class.java)

        (result === SubscribeResult.Subscribed) shouldBe true
    }

    @Test
    fun `a rejected result carries its type name and its code and round trips unchanged`() {
        val rejected = SubscribeResult.Rejected(RejectionCode("course-full"))

        val json = mapper.writeValueAsString(rejected)

        json shouldContain "\"@type\":\"Rejected\""
        json shouldContain "\"code\":\"course-full\""
        mapper.readValue(json, SubscribeResult::class.java) shouldBe rejected
    }

    @Test
    fun `a result with an unknown type name fails deserialisation`() {
        shouldThrow<JacksonException> { mapper.readValue("""{"@type":"Unknown"}""", SubscribeResult::class.java) }
    }

    @Test
    fun `a rejected result with a malformed code fails deserialisation`() {
        val json = """{"@type":"Rejected","code":"Course Full"}"""

        val failure = shouldThrow<JacksonException> { mapper.readValue(json, SubscribeResult::class.java) }

        failure.message shouldContain "not lower-case kebab-case"
    }

    private companion object {
        const val SUBSCRIBED_JSON = """{"@type":"Subscribed"}"""
        const val CAPACITY = 30
        const val PRECISION_ROUNDS = 10_000
    }
}
