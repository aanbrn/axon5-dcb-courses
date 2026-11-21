// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves `checkClaudeAskRules` keeps the Claude Code ask rules in `.claude/settings.json` in sync
 * with the governed paths: one `Edit(/<path>)` rule per path, plus `Bash(*config-set*)`.
 */
class AskRulesCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(
        settings: String = settings(*EXPECTED.toTypedArray()),
        paths: String = GOVERNED,
    ): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(".claude/settings.json", settings)
            file("governance/governed-paths.txt", paths)
        }

    @Test
    fun `settings with an edit rule for every governed path and the config-set rule pass`() {
        val result = fixture().run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a governed path without a rule fails naming the missing rule`() {
        val without = settings(*(EXPECTED - "Edit(/mise.lock)").toTypedArray())

        val output = fixture(without).runAndFail(TASK).output

        output shouldContain "missing: Edit(/mise.lock)"
        output shouldNotContain "duplicated:"
    }

    @Test
    fun `a missing config-set rule fails naming it`() {
        val without = settings(*(EXPECTED - CONFIG_SET).toTypedArray())

        val output = fixture(without).runAndFail(TASK).output

        output shouldContain "missing: $CONFIG_SET"
    }

    @Test
    fun `a duplicated rule fails naming the duplicate`() {
        val duplicated = settings(*(EXPECTED + "Edit(/.mcp.json)").toTypedArray())

        val output = fixture(duplicated).runAndFail(TASK).output

        output shouldContain "duplicated: Edit(/.mcp.json)"
        output shouldNotContain "missing:"
    }

    @Test
    fun `settings without permissions ask fail`() {
        val noPermissions = fixture("{}\n").runAndFail(TASK).output
        noPermissions shouldContain "permissions.ask is missing or empty"

        val noAsk = fixture("""{"permissions": {"allow": ["Bash(ls)"]}}""").runAndFail(TASK).output
        noAsk shouldContain "permissions.ask is missing or empty"
    }

    @Test
    fun `an empty ask array fails`() {
        val output = fixture(settings()).runAndFail(TASK).output

        output shouldContain "permissions.ask is missing or empty"
    }

    @Test
    fun `an ask value that is not a list fails`() {
        val output =
            fixture("""{"permissions": {"ask": "Edit(/mise.toml)"}}""").runAndFail(TASK).output

        output shouldContain "permissions.ask is not a list"
    }

    @Test
    fun `a rule that is not a string fails`() {
        val withNumber = settings(*EXPECTED.toTypedArray()).replace("]", ", 7]")

        val output = fixture(withNumber).runAndFail(TASK).output

        output shouldContain "permissions.ask contains a value that is not a string: 7"
    }

    @Test
    fun `the order of the rules does not matter`() {
        val reversed = settings(*EXPECTED.reversed().toTypedArray())

        val result = fixture(reversed).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `additional ask rules and other permission lists are allowed`() {
        val extended =
            """
            {
              "${'$'}schema": "https://json.schemastore.org/claude-code-settings.json",
              "permissions": {
                "ask": [${(EXPECTED + "Bash(git push*)").joinToString { "\"$it\"" }}],
                "allow": ["Bash(ls)"],
                "deny": ["Read(/.env)"]
              }
            }
            """
                .trimIndent()

        val result = fixture(extended).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a governed path added to the list without a rule fails`() {
        val extended = GOVERNED + "docs/adr/**\n"

        val output = fixture(paths = extended).runAndFail(TASK).output

        output shouldContain "missing: Edit(/docs/adr/**)"
    }

    @Test
    fun `blank lines and surrounding whitespace in the governed paths are ignored`() {
        val padded = "\n  .mcp.json  \n\n" + GOVERNED.replace(".mcp.json\n", "") + "\n\n"

        val result = fixture(paths = padded).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `an empty governed path list fails instead of passing vacuously`() {
        val output = fixture(paths = "\n\n").runAndFail(TASK).output

        output shouldContain "governance/governed-paths.txt lists no governed path"
    }

    @Test
    fun `settings that are not valid JSON fail`() {
        val output = fixture("{ not json").runAndFail(TASK).output

        output shouldContain "settings.json is not valid JSON"
    }

    @Test
    fun `a second run with unchanged inputs is up to date and a drift is still reported`() {
        val fixture = fixture()
        fixture.run(TASK)

        val second = fixture.run(TASK).task(":$TASK")?.outcome

        withClue("second run outcome") {
            listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second
        }
        fixture.file(".claude/settings.json", settings(*(EXPECTED - CONFIG_SET).toTypedArray()))
        fixture.runAndFail(TASK).output shouldContain "missing: $CONFIG_SET"
    }

    private companion object {
        const val TASK = "checkClaudeAskRules"
        const val CONFIG_SET = "Bash(*config-set*)"

        val GOVERNED =
            """
            .planning/config.json
            .claude/settings.json
            .mcp.json
            .github/**
            mise.toml
            mise.lock
            """
                .trimIndent() + "\n"

        val EXPECTED =
            listOf(
                "Edit(/.planning/config.json)",
                "Edit(/.claude/settings.json)",
                "Edit(/.mcp.json)",
                "Edit(/.github/**)",
                "Edit(/mise.toml)",
                "Edit(/mise.lock)",
                CONFIG_SET,
            )

        fun settings(vararg rules: String): String =
            """{"permissions": {"ask": [${rules.joinToString { "\"$it\"" }}]}}""" + "\n"
    }
}
