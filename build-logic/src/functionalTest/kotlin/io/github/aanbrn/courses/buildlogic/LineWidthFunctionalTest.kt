// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves that the formatters wrap at the project's 120-column limit: a line of exactly 120 columns is kept, and a line
 * of 121 columns is wrapped. It covers ktfmt for Kotlin and KTS.
 */
class LineWidthFunctionalTest {
    @TempDir lateinit var projectDir: Path

    @Test
    fun `Kotlin and KTS lines of 120 columns are kept and lines of 121 columns are wrapped`() {
        val fixture = GradleFixture.standard(projectDir)
        val header = "${GradleFixture.HEADER}\npackage fixture\n\n/** A documented public class. */\n"

        fixture.assertWidthBoundary(
            formats = listOf("Kotlin", "KotlinGradle"),
            kept =
                mapOf(
                    "lib/src/main/kotlin/fixture/Kept.kt" to
                        header + lineOfWidth("class Kept(val first: String, val ", ": String)", MAX_WIDTH) + "\n",
                    "data/KeptScript.kts" to
                        "${GradleFixture.HEADER}\n" +
                            lineOfWidth("val kept = listOf(\"a\", \"", "\")", MAX_WIDTH) +
                            "\n",
                ),
            wrapped =
                mapOf(
                    "lib/src/main/kotlin/fixture/Wrapped.kt" to
                        header +
                            lineOfWidth(
                                "class Wrapped(val first: String, val ",
                                ": String)",
                                MAX_WIDTH + 1,
                            ) +
                            "\n",
                    "data/WrappedScript.kts" to
                        "${GradleFixture.HEADER}\n" +
                            lineOfWidth("val wrapped = listOf(\"a\", \"", "\")", MAX_WIDTH + 1) +
                            "\n",
                ),
        )
    }
}

private const val MAX_WIDTH = 120

/** A line of exactly [width] characters: [prefix], then the letter `b` as padding, then [suffix]. */
private fun lineOfWidth(prefix: String, suffix: String, width: Int): String {
    val padding = width - prefix.length - suffix.length
    require(padding > 0) { "A width of $width leaves no room between '$prefix' and '$suffix'." }
    return (prefix + "b".repeat(padding) + suffix).also { it.length shouldBe width }
}

/**
 * Formats the fixture with the Spotless [formats] (for example `Kotlin`) and checks the width boundary: every [kept]
 * file (path to content) is untouched, and every [wrapped] file is reported, rewritten onto more lines of at most
 * [MAX_WIDTH] columns, and then passes the check.
 */
private fun GradleFixture.assertWidthBoundary(
    formats: List<String>,
    kept: Map<String, String>,
    wrapped: Map<String, String>,
) {
    val applyTasks = formats.map { "spotless${it}Apply" }.toTypedArray()
    val checkTasks = formats.map { "spotless${it}Check" }.toTypedArray()
    // Normalises the scaffolding, so that the failure below is caused by the width files only.
    run(*applyTasks)
    (kept + wrapped).forEach { (path, content) -> file(path, content) }

    val failed = runAndFail("--continue", *checkTasks)
    wrapped.keys.forEach { failed.output shouldContain it.substringAfterLast('/') }
    kept.keys.forEach { failed.output shouldNotContain it.substringAfterLast('/') }

    run(*applyTasks)

    kept.forEach { (path, content) -> read(path) shouldBe content }
    wrapped.forEach { (path, content) ->
        val formatted = read(path).lines()
        formatted.size shouldBeGreaterThan content.lines().size
        formatted.forEach { it.length shouldBeLessThanOrEqual MAX_WIDTH }
    }
    val passed = run(*checkTasks)
    checkTasks.forEach { passed.task(":$it")?.outcome shouldBeIn GradleFixture.PASSED }
}
