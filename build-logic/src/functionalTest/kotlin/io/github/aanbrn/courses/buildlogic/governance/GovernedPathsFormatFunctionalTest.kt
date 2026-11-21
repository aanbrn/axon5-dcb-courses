// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.matchers.collections.shouldBeIn
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

    private fun fixture(paths: String, vararg rules: String): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(".claude/settings.json", settings(*rules))
            file("governance/governed-paths.txt", paths)
        }

    @Test
    fun `K1 padded and CRLF terminated entries expect a rule for the stripped entry`() {
        val result =
            fixture(
                    ".mcp.json\n\tmise.toml\r\n",
                    "Edit(/.mcp.json)",
                    "Edit(/mise.toml)",
                    CONFIG_SET,
                )
                .run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    private companion object {
        const val TASK = "checkClaudeAskRules"
        const val CONFIG_SET = "Bash(*config-set*)"

        fun settings(vararg rules: String): String =
            """{"permissions": {"ask": [${rules.joinToString { "\"$it\"" }}]}}""" + "\n"
    }
}
