// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves `checkGsdConfigPolicy` compares the GSD configuration with the policy as a whole. */
class ConfigPolicyCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(config: String, policy: String = POLICY): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(".planning/config.json", config)
            file("governance/gsd-config-policy.json", policy)
        }

    @Test
    fun `a configuration equal to the policy up to key order and whitespace passes`() {
        val reordered =
            """{"workflow":{"research":true,"auto_advance":false},"git":{"phase_branch_template":"gsd/phase-{phase}-{slug}","create_tag":false},"mode":"interactive"}"""

        val result = fixture(reordered).run("checkGsdConfigPolicy")

        result.task(":checkGsdConfigPolicy")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `a transient key added to the configuration fails and is named`() {
        val withTransient =
            POLICY.replace(
                "\"research\": true,",
                "\"research\": true, \"_auto_chain_active\": true,",
            )

        val output = fixture(withTransient).runAndFail("checkGsdConfigPolicy").output

        output shouldContain "added: workflow._auto_chain_active"
        output shouldContain "must be reset before committing"
        output shouldContain "update governance/gsd-config-policy.json in the same pull request"
    }

    @Test
    fun `a changed value fails and names the key path without printing values`() {
        val changed = POLICY.replace("\"create_tag\": false", "\"create_tag\": true")

        val output = fixture(changed).runAndFail("checkGsdConfigPolicy").output

        output shouldContain "changed: git.create_tag"
        output shouldNotContain "create_tag: true"
    }

    @Test
    fun `a removed key fails and is named`() {
        val removed =
            POLICY.replace(", \"phase_branch_template\": \"gsd/phase-{phase}-{slug}\"", "")

        val output = fixture(removed).runAndFail("checkGsdConfigPolicy").output

        output shouldContain "removed: git.phase_branch_template"
    }

    @Test
    fun `a deviation inside a list is reported with its index`() {
        val listPolicy = """{"ship":{"sections":[{"heading":"A"},{"heading":"B"}]}}"""
        val listConfig =
            """{"ship":{"sections":[{"heading":"A"},{"heading":"C"},{"heading":"D"}]}}"""

        val output = fixture(listConfig, listPolicy).runAndFail("checkGsdConfigPolicy").output

        output shouldContain "changed: ship.sections[1].heading"
        output shouldContain "added: ship.sections[2]"
    }

    @Test
    fun `deviations are written to the report sorted`() {
        val deviating =
            POLICY.replace("\"create_tag\": false", "\"create_tag\": true")
                .replace("\"mode\": \"interactive\"", "\"mode\": \"yolo\", \"aaa\": 1")

        val fixture = fixture(deviating)
        fixture.runAndFail("checkGsdConfigPolicy")

        fixture.read("build/reports/governance/gsd-config-policy.txt").lines().filter {
            it.isNotEmpty()
        } shouldBe listOf("added: aaa", "changed: git.create_tag", "changed: mode")
    }

    @Test
    fun `a configuration that is not valid JSON fails naming the file`() {
        val output = fixture("{ not json").runAndFail("checkGsdConfigPolicy").output

        output shouldContain "config.json is not valid JSON"
    }

    @Test
    fun `a second run with unchanged inputs is up to date and a deviation is still reported`() {
        val fixture = fixture(POLICY)
        fixture.run("checkGsdConfigPolicy")

        val second = fixture.run("checkGsdConfigPolicy").task(":checkGsdConfigPolicy")?.outcome

        withClue("second run outcome") {
            listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second
        }
        fixture.file(
            ".planning/config.json",
            POLICY.replace("\"mode\": \"interactive\"", "\"mode\": \"yolo\""),
        )
        fixture.runAndFail("checkGsdConfigPolicy").output shouldContain "changed: mode"
    }

    private companion object {
        val POLICY =
            """
            {
              "workflow": { "research": true, "auto_advance": false },
              "git": { "create_tag": false, "phase_branch_template": "gsd/phase-{phase}-{slug}" },
              "mode": "interactive"
            }
            """
                .trimIndent()
    }
}
