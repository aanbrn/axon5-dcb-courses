// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves the repository-wide formatting beyond Kotlin: Prettier and the Node.js version guard. */
class FormattingFunctionalTest {
    @TempDir lateinit var projectDir: Path

    @Test
    fun `unformatted JSON, YAML and Markdown fail spotlessCheck and spotlessApply fixes them`() {
        val fixture = GradleFixture.standard(projectDir)
        // Normalises the scaffolding, so that the failure below is caused by the messy files only.
        fixture.run("spotlessApply")
        fixture.run("spotlessCheck")
        fixture.file("data/messy.json", "{\"name\":\"x\",   \"items\":[1,2,3]}\n")
        fixture.file("data/messy.yml", "root:\n      child: 1\n      other:   [a,   b]\n")
        fixture.file("data/messy.md", "#   Title\n\n\n\n*   first\n*   second\n")

        val failed = fixture.runAndFail("spotlessCheck")
        failed.output shouldContain "messy.json"
        failed.output shouldContain "messy.yml"
        failed.output shouldContain "messy.md"

        fixture.run("spotlessApply")

        fixture.read("data/messy.json") shouldBe "{ \"name\": \"x\", \"items\": [1, 2, 3] }\n"
        fixture.read("data/messy.yml") shouldBe "root:\n  child: 1\n  other: [a, b]\n"
        fixture.read("data/messy.md") shouldBe "# Title\n\n- first\n- second\n"
        fixture.run("spotlessCheck").task(":spotlessCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `a Node major different from nvmrc fails verifyNodeVersion naming nvmrc`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.file(".nvmrc", "1\n")

        val result = fixture.runAndFail("verifyNodeVersion")

        result.task(":verifyNodeVersion")?.outcome shouldBe TaskOutcome.FAILED
        result.output shouldContain ".nvmrc"
        result.output shouldContain "mise install"
    }

    @Test
    fun `a matching Node major passes verifyNodeVersion and guards the Prettier tasks`() {
        val fixture = GradleFixture.standard(projectDir)

        val result = fixture.run("spotlessPrettierCheck")

        result.task(":verifyNodeVersion")?.outcome shouldBe TaskOutcome.SUCCESS
        result.task(":spotlessPrettierCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
