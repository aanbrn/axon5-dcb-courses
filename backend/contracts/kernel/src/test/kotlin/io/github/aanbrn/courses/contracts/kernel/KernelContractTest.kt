// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Covers the tag registry, the message namespace and the rejection-code contract of the kernel. */
class KernelContractTest {

    @Test
    fun `the course tag key is the course TypeID prefix`() {
        Tags.COURSE shouldBe "course"
        Tags.COURSE shouldBe CourseId.PREFIX
    }

    @Test
    fun `the student tag key is the student TypeID prefix`() {
        Tags.STUDENT shouldBe "student"
        Tags.STUDENT shouldBe StudentId.PREFIX
    }

    @Test
    fun `the message namespace is courses`() {
        MessageNamespace.COURSES shouldBe "courses"
    }

    @Test
    fun `a kebab-case rejection code keeps its value and prints as that value`() {
        val code = RejectionCode("course-full")

        code.value shouldBe "course-full"
        code.toString() shouldBe "course-full"
    }

    @Test
    fun `a rejection code with a single word and digits is accepted`() {
        RejectionCode("limit2").value shouldBe "limit2"
    }

    @Test
    fun `an empty rejection code is rejected`() {
        shouldThrow<IllegalArgumentException> { RejectionCode("") }
    }

    @Test
    fun `a rejection code with upper-case letters or spaces is rejected`() {
        shouldThrow<IllegalArgumentException> { RejectionCode("Course Full") }
        shouldThrow<IllegalArgumentException> { RejectionCode("Course-full") }
    }

    @Test
    fun `a rejection code with a leading hyphen is rejected`() {
        shouldThrow<IllegalArgumentException> { RejectionCode("-full") }
    }

    @Test
    fun `a rejection code with a doubled or trailing hyphen is rejected`() {
        shouldThrow<IllegalArgumentException> { RejectionCode("course--full") }
        shouldThrow<IllegalArgumentException> { RejectionCode("course-") }
    }

    @Test
    fun `a rejection exposes its code through the shared contract`() {
        val rejection =
            object : Rejection {
                override val code = RejectionCode("course-full")
            }

        rejection.code shouldBe RejectionCode("course-full")
    }
}
