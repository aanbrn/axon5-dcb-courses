// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves `checkClaudeAskRules` reads `governance/governed-paths.txt` with the same grammar as
 * `load_globs` in `scripts/governance/check-pr.sh`. Every case has a twin in
 * `scripts/governance/test-check-pr.sh`.
 */
class GovernedPathsFormatFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(paths: String, rules: List<String> = MISE_RULES): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(".claude/settings.json", settings(rules))
            file("governance/governed-paths.txt", paths)
        }

    @Test
    fun `K1 padded and CRLF terminated entries expect a rule for the stripped entry`() {
        val rules = listOf("Edit(/.mcp.json)", "Edit(/mise.toml)", CONFIG_SET)

        val result = fixture(".mcp.json\n\tmise.toml\r\n", rules).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `K2 comment lines govern nothing`() {
        val result = fixture("# a comment\n   # indented comment\nmise.toml\n").run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `K3 an entry keeps its inner space`() {
        val rules = listOf("Edit(/docs/my notes.md)", CONFIG_SET)

        val result = fixture("docs/my notes.md\n", rules).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `K4 a non-breaking space fails closed naming the line`() {
        val output = fixture("mise.toml\n .mcp.json\n").runAndFail(TASK).output

        output shouldContain "line 2: entry contains a character outside printable ASCII"
        output shouldNotContain "missing:"
    }

    @Test
    fun `K5 every offending line is reported in ascending line order`() {
        val lines = List(LINE_COUNT) { "docs/f$it.md" }.toMutableList()
        lines[1] = " .mcp.json"
        lines[LINE_COUNT - 1] = "mise\t.toml"

        val output = fixture(lines.joinToString("\n", postfix = "\n")).runAndFail(TASK).output

        output.indexOf("line 2:") shouldBeGreaterThanOrEqual 0
        output.indexOf("line 2:") shouldBeLessThan output.indexOf("line $LINE_COUNT:")
    }

    @Test
    fun `K6 a file with only comments and blank lines lists no governed path`() {
        val output = fixture("# nothing governed\n\n   \n").runAndFail(TASK).output

        output shouldContain "governance/governed-paths.txt lists no governed path"
    }

    @Test
    fun `K7 a byte order mark fails closed naming the first line`() {
        val output = fixture("﻿mise.toml\n").runAndFail(TASK).output

        output shouldContain "line 1: entry contains a character outside printable ASCII"
    }

    @Test
    fun `K8 an inner carriage return fails closed instead of splitting the entry`() {
        val output = fixture("mise.toml\r.mcp.json\n").runAndFail(TASK).output

        output shouldContain "line 1: entry contains a character outside printable ASCII"
    }

    @Test
    fun `K9 entries equal after stripping expect one rule`() {
        val paths = ".mcp.json\n  .mcp.json\n"
        val once = listOf("Edit(/.mcp.json)", CONFIG_SET)

        fixture(paths, once).run(TASK).task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED

        val twice = once + "Edit(/.mcp.json)"
        fixture(paths, twice).runAndFail(TASK).output shouldContain "duplicated: Edit(/.mcp.json)"
    }

    private companion object {
        const val TASK = "checkClaudeAskRules"
        const val CONFIG_SET = "Bash(*config-set*)"
        const val LINE_COUNT = 10
        val MISE_RULES = listOf("Edit(/mise.toml)", CONFIG_SET)

        fun settings(rules: List<String>): String =
            """{"permissions": {"ask": [${rules.joinToString { "\"$it\"" }}]}}""" + "\n"
    }
}
