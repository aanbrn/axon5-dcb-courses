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
 * Proves `checkToolingInventory` keeps `mise.toml` and `docs/tooling.md` listing the same tools.
 */
class ToolingTableCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(mise: String, inventory: String): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file("mise.toml", mise)
            file("docs/tooling.md", inventory)
        }

    @Test
    fun `matching mise tools and inventory rows pass`() {
        val result = fixture(MISE, inventory(ZIZMOR, REUSE, RENOVATE)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a mise tool without an inventory row fails naming it`() {
        val output = fixture(MISE, inventory(REUSE, RENOVATE)).runAndFail(TASK).output

        output shouldContain "missing in docs/tooling.md: zizmor"
        output shouldNotContain "missing in mise.toml"
    }

    @Test
    fun `a mise-backed row without a mise entry fails naming it`() {
        val mise = MISE.replace("\"pipx:reuse\" = { version = \"6.3.0\" }\n", "")

        val output = fixture(mise, inventory(ZIZMOR, REUSE, RENOVATE)).runAndFail(TASK).output

        output shouldContain "missing in mise.toml: pipx:reuse"
        output shouldNotContain "missing in docs/tooling.md"
    }

    @Test
    fun `a tool spelled differently in the two files is reported in both directions`() {
        val unprefixed = row("reuse (`reuse`)", "`mise`")

        val output = fixture(MISE, inventory(ZIZMOR, unprefixed, RENOVATE)).runAndFail(TASK).output

        output shouldContain "missing in docs/tooling.md: pipx:reuse"
        output shouldContain "missing in mise.toml: reuse"
    }

    @Test
    fun `row order and key order do not matter`() {
        val reordered =
            """
            [tools]
            "npm:renovate" = { version = "44.0.0", trust_policy_excludes = ["x@1.0.0"] }
            "pipx:reuse" = { version = "6.3.0" }
            zizmor = "1.32.0"
            """
                .trimIndent()

        val result = fixture(reordered, inventory(REUSE, ZIZMOR, RENOVATE)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `an empty tools table and no mise-backed rows pass`() {
        val result = fixture("[tools]\n", inventory(NOT_MISE)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `one mise-backed row with an empty tools table fails`() {
        val output = fixture("[tools]\n", inventory(ZIZMOR)).runAndFail(TASK).output

        output shouldContain "missing in mise.toml: zizmor"
    }

    @Test
    fun `inline table and quoted values are read as their key only`() {
        val mise =
            """
            [tools]
            # A comment is not a key = "x"
            "npm:renovate" = { version = "44.0.0", trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"] }
            'aqua:mvdan/sh' = "3.14.1"
            """
                .trimIndent()
        val shfmt = row("shfmt (`aqua:mvdan/sh`)", "`mise`")

        val result = fixture(mise, inventory(RENOVATE, shfmt)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `keys of other sections and rows with another version source are ignored`() {
        val mise = "[settings]\nlocked = true\n\n$MISE\n\n[env]\nFOO = \"bar\"\n"

        val result = fixture(mise, inventory(ZIZMOR, REUSE, RENOVATE, NOT_MISE)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a mise-backed row without a backticked key fails`() {
        val keyless = row("zizmor", "`mise`")

        val output = fixture(MISE, inventory(keyless, REUSE, RENOVATE)).runAndFail(TASK).output

        output shouldContain "no backticked mise.toml key in row: zizmor"
    }

    @Test
    fun `table rows are read regardless of column padding`() {
        val padded =
            "|   zizmor (`zizmor`)     |  Audits workflows   |    `mise`     | `mise install` |\n"

        val result = fixture(MISE, inventory(padded, REUSE, RENOVATE)).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a second run with unchanged inputs is up to date and a drift is still reported`() {
        val fixture = fixture(MISE, inventory(ZIZMOR, REUSE, RENOVATE))
        fixture.run(TASK)

        val second = fixture.run(TASK).task(":$TASK")?.outcome

        withClue("second run outcome") {
            listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second
        }
        fixture.file("docs/tooling.md", inventory(REUSE, RENOVATE))
        fixture.runAndFail(TASK).output shouldContain "missing in docs/tooling.md: zizmor"
    }

    private companion object {
        const val TASK = "checkToolingInventory"

        val MISE =
            """
            [tools]
            zizmor = "1.32.0"
            "pipx:reuse" = { version = "6.3.0" }
            "npm:renovate" = { version = "44.0.0", trust_policy_excludes = ["x@1.0.0"] }
            """
                .trimIndent() + "\n"

        val ZIZMOR = row("zizmor (`zizmor`)", "`mise`")
        val REUSE = row("reuse (`pipx:reuse`)", "`mise`")
        val RENOVATE = row("renovate (`npm:renovate`)", "`mise`")
        val NOT_MISE = row("JDK", "`.java-version`")

        fun row(tool: String, source: String): String =
            "| $tool | Purpose | $source | `mise install` | local, CI |\n"

        fun inventory(vararg rows: String): String =
            "# Tooling\n\n## Tools\n\n" +
                "| Tool | Purpose | Version source | Installation | Used in |\n" +
                "| --- | --- | --- | --- | --- |\n" +
                rows.joinToString(separator = "") +
                "\n## Notes\n\nA | pipe | outside | a | table is prose.\n"
    }
}
