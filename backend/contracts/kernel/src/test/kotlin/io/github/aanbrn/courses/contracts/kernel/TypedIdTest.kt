// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/** Covers the parse API, equality and generation of the entity IDs. */
class TypedIdTest {

    private val uuid = UUID.fromString("00000000-0000-0000-0000-000000000010")
    private val canonicalCourseId = "course_0000000000000000000000000g"

    @Test
    fun `a wrong prefix is rejected with the expected and the actual prefix`() {
        val failure = shouldThrow<IllegalArgumentException> { CourseId.parse(StudentId.random().toString()) }

        failure.message.orEmpty() shouldContain "expected 'course', got 'student'"
    }

    @Test
    fun `malformed text is rejected as a malformed TypeID`() {
        listOf("garbage", "", "course_", "COURSE_01h455vb4pex5vsknk084sn02q").forEach {
            val failure = shouldThrow<IllegalArgumentException> { CourseId.parse(it) }

            failure.message.orEmpty() shouldStartWith "Malformed TypeID '"
        }
    }

    @Test
    fun `a suffix without a prefix is rejected as a wrong prefix`() {
        val failure = shouldThrow<IllegalArgumentException> { CourseId.parse("01h455vb4pex5vsknk084sn02q") }

        failure.message.orEmpty() shouldContain "expected 'course', got ''"
    }

    @Test
    fun `an upper-case suffix is rejected because TypeIDs are case-sensitive`() {
        shouldThrow<IllegalArgumentException> { CourseId.parse("course_01H455VB4PEX5VSKNK084SN02Q") }
    }

    @Test
    fun `a suffix that would overflow 128 bits is rejected`() {
        shouldThrow<IllegalArgumentException> { CourseId.parse("course_8zzzzzzzzzzzzzzzzzzzzzzzzz") }
    }

    @Test
    fun `parseOrNull returns null for a wrong prefix and for every malformed text`() {
        listOf(StudentId.random().toString(), "garbage", "", "course_", "01h455vb4pex5vsknk084sn02q").forEach {
            CourseId.parseOrNull(it) shouldBe null
        }
    }

    @Test
    fun `parseOrNull returns the ID for a canonical TypeID`() {
        CourseId.parseOrNull(canonicalCourseId) shouldBe CourseId.of(uuid)
    }

    @Test
    fun `toString is the canonical TypeID`() {
        val id = CourseId.of(uuid)

        id.toString() shouldStartWith "course_"
        id.toString().length shouldBe "course_".length + 26
        id.toString() shouldBe canonicalCourseId
    }

    @Test
    fun `a student ID carries the student prefix`() {
        StudentId.of(uuid).toString() shouldStartWith "student_"
        StudentId.parse(StudentId.of(uuid).toString()) shouldBe StudentId.of(uuid)
    }

    @Test
    fun `IDs over the same UUID are equal and IDs over different UUIDs are not`() {
        CourseId.of(uuid) shouldBe CourseId.of(uuid)
        CourseId.of(uuid) shouldNotBe CourseId.of(UUID(uuid.mostSignificantBits, uuid.leastSignificantBits + 1))
    }

    @Test
    fun `a random ID is a version 7 UUID`() {
        CourseId.random().uuid.version() shouldBe 7
    }

    @Test
    fun `random IDs differ from one another`() {
        CourseId.random() shouldNotBe CourseId.random()
    }

    @Test
    fun `parsing the string form returns the same ID for any UUID`() {
        runBlocking<Unit> {
            checkAll(Arb.uuid()) { value ->
                val id = CourseId.of(value)

                CourseId.parse(id.toString()) shouldBe id
                CourseId.parse(id.toString()).toString() shouldBe id.toString()
            }
        }
    }
}
