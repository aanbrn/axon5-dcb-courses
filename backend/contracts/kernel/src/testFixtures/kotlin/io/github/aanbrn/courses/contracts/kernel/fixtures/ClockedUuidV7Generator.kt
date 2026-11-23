// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel.fixtures

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.UUIDClock
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.StudentId
import java.time.Clock
import java.util.Random
import java.util.UUID

/**
 * Generates UUIDv7 values from an injected [Clock], for tests whose outcome depends on ID order or creation time.
 *
 * With a fixed [Clock] and a seeded [Random] the sequence is deterministic. UUIDs generated in the same millisecond
 * stay strictly increasing, because the UUIDv7 generator counts upwards within a millisecond.
 *
 * @param clock the source of the timestamp embedded in every UUID
 * @param random the source of the random bits, seeded for a reproducible sequence
 */
class ClockedUuidV7Generator(clock: Clock, random: Random) {
    private val generator: TimeBasedEpochGenerator =
        Generators.timeBasedEpochGenerator(
            random,
            object : UUIDClock() {
                override fun currentTimeMillis(): Long = clock.millis()
            },
        )

    /**
     * Generates the next UUIDv7.
     *
     * @return a UUID whose timestamp is the current time of the clock
     */
    fun nextUuid(): UUID = generator.generate()

    /**
     * Generates the next course ID.
     *
     * @return a [CourseId] over [nextUuid]
     */
    fun nextCourseId(): CourseId = CourseId(nextUuid())

    /**
     * Generates the next student ID.
     *
     * @return a [StudentId] over [nextUuid]
     */
    fun nextStudentId(): StudentId = StudentId(nextUuid())
}
