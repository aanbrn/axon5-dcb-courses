// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves `checkPrTemplate` keeps the pull request template aligned with the `/gsd-ship` body. */
class PrTemplateCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(template: String, config: String = config()): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(".planning/config.json", config)
            file(".github/PULL_REQUEST_TEMPLATE.md", template)
        }

    @Test
    fun `a template with the core and enabled configured headings in order passes`() {
        val result = fixture(template(*CORE, *CONFIGURED, "Checklist")).run("checkPrTemplate")

        result.task(":checkPrTemplate")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `headings after the expected ones are allowed`() {
        val result = fixture(template(*CORE, *CONFIGURED, "Checklist", "Notes")).run("checkPrTemplate")

        result.task(":checkPrTemplate")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a template missing a core heading fails naming it`() {
        val headings = CORE.filterNot { it == "Verification" } + CONFIGURED

        val output = fixture(template(*headings.toTypedArray())).runAndFail("checkPrTemplate").output

        output shouldContain "missing heading: ## Verification"
    }

    @Test
    fun `a template with headings out of order fails showing expected and actual`() {
        val swapped = arrayOf("Changes", "Summary") + CORE.drop(2) + CONFIGURED

        val output = fixture(template(*swapped)).runAndFail("checkPrTemplate").output

        output shouldContain "expected: Summary, Changes, Requirements Addressed"
        output shouldContain "actual:   Changes, Summary, Requirements Addressed"
    }

    @Test
    fun `a configured section that is disabled is not required`() {
        val config = config(listOf("Risks" to false, "Metrics" to true))

        val result = fixture(template(*CORE, "Metrics"), config).run("checkPrTemplate")

        result.task(":checkPrTemplate")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `an enabled configured section that the template lacks fails naming it`() {
        val config = config(listOf("Risks" to true))

        val output = fixture(template(*CORE), config).runAndFail("checkPrTemplate").output

        output shouldContain "missing heading: ## Risks"
    }

    @Test
    fun `a configuration without ship sections requires only the core headings`() {
        val result = fixture(template(*CORE), "{}").run("checkPrTemplate")

        result.task(":checkPrTemplate")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a heading inside an HTML comment or a code fence does not count`() {
        val hidden =
            template("Summary", "Changes") +
                "<!--\n## Requirements Addressed\n-->\n" +
                "```\n## Verification\n```\n" +
                template("Key Decisions")

        val output = fixture(hidden, "{}").runAndFail("checkPrTemplate").output

        output shouldContain "missing heading: ## Requirements Addressed"
        output shouldContain "missing heading: ## Verification"
        output shouldContain "actual:   Summary, Changes, Key Decisions"
    }

    @Test
    fun `a configuration that is not valid JSON fails naming the file`() {
        val output = fixture(template(*CORE), "{ not json").runAndFail("checkPrTemplate").output

        output shouldContain "config.json is not valid JSON"
    }

    @Test
    fun `a second run with unchanged inputs is up to date and a drift is still reported`() {
        val fixture = fixture(template(*CORE, *CONFIGURED))
        fixture.run("checkPrTemplate")

        val second = fixture.run("checkPrTemplate").task(":checkPrTemplate")?.outcome

        withClue("second run outcome") {
            listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second
        }
        fixture.file(".github/PULL_REQUEST_TEMPLATE.md", template(*CORE))
        fixture.runAndFail("checkPrTemplate").output shouldContain "missing heading: ## Risks"
    }

    private companion object {
        val CORE = arrayOf("Summary", "Changes", "Requirements Addressed", "Verification", "Key Decisions")
        val CONFIGURED = arrayOf("Stories", "Risks", "Metrics", "Review")

        fun template(vararg headings: String): String =
            headings.joinToString(separator = "\n") { "## $it\n\n<!-- hint -->\n" } + "\n"

        fun config(sections: List<Pair<String, Boolean>> = CONFIGURED.map { it to true }): String {
            val entries =
                sections.joinToString(separator = ",") { (heading, enabled) ->
                    """{"heading": "$heading", "enabled": $enabled}"""
                }
            return """{"ship": {"pr_body_sections": [$entries]}}"""
        }
    }
}
