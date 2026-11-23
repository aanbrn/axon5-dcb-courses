// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

import io.github.aanbrn.courses.contracts.kernel.fixtures.ClockedUuidV7Generator
import io.github.aanbrn.courses.contracts.kernel.fixtures.TestIds
import io.github.aanbrn.courses.contracts.kernel.fixtures.courseId
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Property tests of the entity ID codec, driven by the kernel's `testFixtures`.
 *
 * Every test has a block body: a test that returns a value other than `Unit` is silently skipped by JUnit.
 */
class TypeIdPropertyTest {

    @Test
    fun `every generated course id survives a TypeID round trip`() {
        runBlocking<Unit> { checkAll(Arb.courseId()) { id -> CourseId.parse(id.toString()) shouldBe id } }
    }

    @Test
    fun `parseOrNull never throws for arbitrary text`() {
        runBlocking<Unit> {
            checkAll(Arb.string()) { text ->
                val parsed = CourseId.parseOrNull(text)

                (parsed == null || parsed.toString() == text) shouldBe true
            }
        }
    }

    @Test
    fun `one hundred thousand seeded random UUIDs survive a round trip`() {
        val random = Random(SEED)

        repeat(ROUNDS) {
            val uuid = UUID(random.nextLong(), random.nextLong())

            CourseId.parse(CourseId(uuid).toString()) shouldBe CourseId(uuid)
        }
    }

    @Test
    fun `a generated course id is a UUIDv7`() {
        CourseId.random().uuid.version() shouldBe UUID_VERSION_7
    }

    @Test
    fun `a clocked generator over a fixed clock yields strictly increasing UUIDs`() {
        val clock = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC)
        val generator = ClockedUuidV7Generator(clock, java.util.Random(SEED.toLong()))

        val ids = List(CLOCKED_IDS) { generator.nextUuid().toString() }

        ids.shouldBeStrictlyIncreasing()
    }

    @Test
    fun `one hundred thousand random course ids from eight threads are pairwise distinct`() {
        val pool = Executors.newFixedThreadPool(THREADS)
        try {
            val ids =
                List(THREADS) { Callable { List(ROUNDS / THREADS) { CourseId.random() } } }
                    .let(pool::invokeAll)
                    .flatMap { it.get() }

            ids.size shouldBe ROUNDS
            ids.toSet().size shouldBe ROUNDS
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `a fixed test id prints as a stable readable TypeID`() {
        TestIds.courseId(1).toString() shouldBe "course_01hw000000e008000000000001"
        TestIds.studentId(2).toString() shouldBe "student_01hw000000e008000000000002"
    }

    private companion object {
        const val SEED = 42
        const val ROUNDS = 100_000
        const val THREADS = 8
        const val CLOCKED_IDS = 1_000
        const val UUID_VERSION_7 = 7
    }
}
