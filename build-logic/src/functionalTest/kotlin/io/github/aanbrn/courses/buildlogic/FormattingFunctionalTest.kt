// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves the repository-wide formatting beyond Kotlin: Prettier, the Node.js version guard, taplo, shfmt and the
 * generic text formats.
 */
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

    @Test
    fun `unformatted TOML fails spotlessCheck and spotlessApply fixes it`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.run("spotlessApply")
        fixture.file("data/messy.toml", "name   =   \"x\"\nitems = [\n      1,\n 2]\n")

        val failed = fixture.runAndFail("spotlessTomlCheck")
        failed.output shouldContain "messy.toml"

        fixture.run("spotlessApply")

        fixture.read("data/messy.toml") shouldBe "name = \"x\"\nitems = [1, 2]\n"
        fixture.run("spotlessTomlCheck").task(":spotlessTomlCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `a missing taplo fails the TOML tasks with a mise install hint but not configuration`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.file("data/messy.toml", "name   =   \"x\"\n")

        val help = fixture.runWithout("taplo", "help")
        help.task(":help")?.outcome shouldBe TaskOutcome.SUCCESS

        val result = fixture.runAndFailWithout("taplo", "spotlessTomlCheck")

        result.output shouldContain "mise install"
        result.task(":verifyTaplo")?.outcome shouldBe TaskOutcome.FAILED
    }

    @Test
    fun `a shell script gets the SPDX header on line 2 and shfmt indentation`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.run("spotlessApply")
        fixture.file(
            "scripts/run.sh",
            "#!/usr/bin/env bash\n# Runs the thing.\nif true; then\n\techo hi\nfi\n",
        )

        val failed = fixture.runAndFail("spotlessShellCheck")
        failed.output shouldContain "run.sh"

        fixture.run("spotlessApply")

        // REUSE-IgnoreStart
        val expected =
            "#!/usr/bin/env bash\n# SPDX-License-Identifier: MIT\n# Runs the thing.\n" +
                "if true; then\n  echo hi\nfi\n"
        // REUSE-IgnoreEnd
        fixture.read("scripts/run.sh") shouldBe expected
        fixture.run("spotlessShellCheck").task(":spotlessShellCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `a missing shfmt fails the shell tasks with a mise install hint but not configuration`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.file("scripts/run.sh", "#!/usr/bin/env bash\necho hi\n")

        fixture.runWithout("shfmt", "help").task(":help")?.outcome shouldBe TaskOutcome.SUCCESS

        val result = fixture.runAndFailWithout("shfmt", "spotlessShellCheck")

        result.output shouldContain "mise install"
        result.task(":verifyShfmt")?.outcome shouldBe TaskOutcome.FAILED
    }

    @Test
    fun `properties with trailing whitespace and no final newline fail spotlessCheck and are fixed`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.run("spotlessApply")
        fixture.file("data/messy.properties", "key=value   \nother=1")

        val failed = fixture.runAndFail("spotlessMiscCheck")
        failed.output shouldContain "messy.properties"

        fixture.run("spotlessApply")

        fixture.read("data/messy.properties") shouldBe "key=value\nother=1\n"
        fixture.run("spotlessMiscCheck").task(":spotlessMiscCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `profiler output directories inside the project are never formatted`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.run("spotlessApply")
        // gradle-profiler writes a Gradle user home and its results into the project directory.
        fixture.file("gradle-user-home/caches/gc.properties", "key=value   \nother=1")
        fixture.file("profile-out/results.properties", "key=value   \nother=1")
        fixture.file("profile-out-2/results.properties", "key=value   \nother=1")

        // The task is up to date: the excluded files are not even inputs of it.
        fixture.run("spotlessMiscCheck").task(":spotlessMiscCheck")?.outcome shouldBe TaskOutcome.UP_TO_DATE
    }

    @Test
    fun `untracked files that git ignores are never formatted`() {
        git(projectDir, "init")
        val fixture = GradleFixture.standard(projectDir)
        fixture.file(".gitignore", ".playwright-mcp/\nscratch/\n")
        fixture.run("spotlessApply")
        // A Playwright MCP session writes unformatted files into a git-ignored directory of the repository.
        fixture.file(".playwright-mcp/page.json", MESSY_JSON)
        fixture.file("scratch/notes.md", MESSY_MARKDOWN)

        // The task is up to date: the ignored files are not even inputs of it.
        fixture.run("spotlessPrettierCheck").task(":spotlessPrettierCheck")?.outcome shouldBe TaskOutcome.UP_TO_DATE
    }

    @Test
    fun `a tracked file is formatted even where gitignore matches it`() {
        git(projectDir, "init")
        val fixture = GradleFixture.standard(projectDir)
        fixture.file(".gitignore", ".playwright-mcp/\nscratch/\n")
        fixture.run("spotlessApply")
        fixture.file(".playwright-mcp/page.json", MESSY_JSON)
        fixture.file("scratch/notes.md", MESSY_MARKDOWN)
        fixture.file("scratch/tracked.json", MESSY_JSON)
        git(projectDir, "add", "-f", "scratch/tracked.json")

        val failed = fixture.runAndFail("spotlessPrettierCheck")

        failed.output shouldContain "tracked.json"
        failed.output shouldNotContain "notes.md"
        failed.output shouldNotContain "page.json"
    }

    @Test
    fun `a project directory below the git work tree root keeps only the static exclusions`() {
        git(projectDir, "init")
        projectDir.resolve(".gitignore").toFile().writeText("scratch/\n")
        val nested = projectDir.resolve("nested").also { Files.createDirectories(it) }
        val fixture = GradleFixture.standard(nested)
        fixture.run("spotlessApply")
        fixture.file("scratch/notes.md", MESSY_MARKDOWN)

        fixture.runAndFail("spotlessPrettierCheck").output shouldContain "notes.md"
    }

    private fun git(directory: Path, vararg args: String) {
        val process =
            ProcessBuilder("git", "-c", "init.defaultBranch=main", *args)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        withClue("git ${args.joinToString(" ")}: $output") { process.waitFor() shouldBe 0 }
    }

    private companion object {
        const val MESSY_JSON = "{\"name\":\"x\",   \"items\":[1,2,3]}\n"
        const val MESSY_MARKDOWN = "#   Title\n\n\n\n*   first\n*   second\n"
    }
}
