// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves `checkClaudeAskRules` rejects `.claude/settings.json` and governed path lists that are
 * malformed or empty instead of passing vacuously.
 */
class AskRulesCheckInputShapeFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(settings: String, paths: String = GOVERNED): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(".claude/settings.json", settings)
            file("governance/governed-paths.txt", paths)
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
        val withNumber = settings(*EXPECTED).replace("]", ", 7]")

        val output = fixture(withNumber).runAndFail(TASK).output

        output shouldContain "permissions.ask contains a value that is not a string: 7"
    }

    @Test
    fun `an empty governed path list fails instead of passing vacuously`() {
        val output = fixture(settings(*EXPECTED), paths = "\n\n").runAndFail(TASK).output

        output shouldContain "governance/governed-paths.txt lists no governed path"
    }

    @Test
    fun `settings that are not valid JSON fail`() {
        val output = fixture("{ not json").runAndFail(TASK).output

        output shouldContain "settings.json is not valid JSON"
    }

    private companion object {
        const val TASK = "checkClaudeAskRules"
        const val GOVERNED = ".mcp.json\nmise.toml\n"
        val EXPECTED = arrayOf("Edit(/.mcp.json)", "Edit(/mise.toml)", "Bash(*config-set*)")

        fun settings(vararg rules: String): String =
            """{"permissions": {"ask": [${rules.joinToString { "\"$it\"" }}]}}""" + "\n"
    }
}
