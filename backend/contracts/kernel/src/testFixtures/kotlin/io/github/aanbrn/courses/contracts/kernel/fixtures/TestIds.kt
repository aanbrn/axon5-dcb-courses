// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel.fixtures

import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.StudentId
import java.util.UUID

/**
 * Readable, fixed entity IDs for tests.
 *
 * `TestIds.courseId(1)` is always the same value, so a failure message shows `course_01hw000000e008000000000001`
 * instead of a random ID that cannot be compared between runs. Every ID is a valid UUIDv7 (version and variant bits
 * set), as the IDs the production code generates are.
 */
object TestIds {
    private const val VERSION_7_MOST_SIGNIFICANT_BITS = 0x018F000000007000L
    private const val VARIANT_LEAST_SIGNIFICANT_BITS = Long.MIN_VALUE
    private const val MAX_NUMBER = 9_999

    /**
     * Returns the course ID with the given [number].
     *
     * @param number a number from 1 to 9,999
     * @return the same [CourseId] for the same [number]
     * @throws IllegalArgumentException when [number] is outside the range
     */
    fun courseId(number: Int): CourseId = CourseId(uuid(number))

    /**
     * Returns the student ID with the given [number].
     *
     * @param number a number from 1 to 9,999
     * @return the same [StudentId] for the same [number]
     * @throws IllegalArgumentException when [number] is outside the range
     */
    fun studentId(number: Int): StudentId = StudentId(uuid(number))

    private fun uuid(number: Int): UUID {
        require(number in 1..MAX_NUMBER) { "Test ID number must be in 1..$MAX_NUMBER, got $number" }
        return UUID(VERSION_7_MOST_SIGNIFICANT_BITS, VARIANT_LEAST_SIGNIFICANT_BITS or number.toLong())
    }
}
