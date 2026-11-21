// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves `checkBuildScripts` rejects version literals and eager or cross-project build APIs. */
class BuildScriptLintCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private val fixture by lazy { GradleFixture.governance(projectDir) }

    /** The `path:line: [rule]` prefix of every finding in the report, in report order. */
    private fun findings(): List<String> =
        fixture
            .read("build/reports/governance/build-scripts.txt")
            .lines()
            .filter { it.isNotEmpty() }
            .map { it.split(" ").take(2).joinToString(" ") }

    @Test
    fun `a build without version literals passes`() {
        fixture.file(
            "lib/build.gradle.kts",
            """
            // implementation("com.example:lib:1.2.3") is only mentioned in a comment
            plugins { id("courses.kotlin-library") }

            dependencies { implementation(libs.example) }

            tasks.register("hello")
            tasks.withType<Test>().configureEach { useJUnitPlatform() }
            """
                .trimIndent(),
        )
        fixture.file("gradle.properties", "group=io.example\norg.gradle.caching=true\n")

        val result = fixture.run("checkBuildScripts")

        result.task(":checkBuildScripts")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a dependency with a literal version is reported with its line`() {
        fixture.file(
            "lib/build.gradle.kts",
            """
            plugins {}

            dependencies {
                implementation("com.example:lib:1.2.3")
            }
            """
                .trimIndent(),
        )

        val output = fixture.runAndFail("checkBuildScripts").output

        output shouldContain "lib/build.gradle.kts:4: [version-literal]"
    }

    @Test
    fun `plugin versions, version properties and JDK numbers are version literals`() {
        fixture.file(
            "lib/build.gradle.kts",
            """
            plugins { id("x.y") version "1.0" }
            version = "1.0"
            kotlin { jvmToolchain(25) }
            val language = JavaLanguageVersion.of(25)
            val fine = JavaLanguageVersion.of(javaMajor)
            """
                .trimIndent(),
        )

        fixture.runAndFail("checkBuildScripts")

        findings() shouldBe
            listOf(
                "lib/build.gradle.kts:1: [version-literal]",
                "lib/build.gradle.kts:2: [version-literal]",
                "lib/build.gradle.kts:3: [version-literal]",
                "lib/build.gradle.kts:4: [version-literal]",
            )
    }

    @Test
    fun `a numeric version in gradle properties and in build-logic sources is reported`() {
        fixture.file("gradle.properties", "group=io.example\nkotlinVersion=2.4.21\n")
        fixture.file(
            "build-logic/src/main/kotlin/Plugin.kt",
            "package demo\n\nval dependency = \"org.example:thing:9.9\"\n",
        )

        fixture.runAndFail("checkBuildScripts")

        findings() shouldBe
            listOf(
                "build-logic/src/main/kotlin/Plugin.kt:3: [version-literal]",
                "gradle.properties:2: [version-literal]",
            )
    }

    @Test
    fun `eager and cross-project APIs are reported with their rule`() {
        fixture.file(
            "lib/build.gradle.kts",
            """
            tasks.create("a")
            tasks.getByName("b")
            tasks.withType<Test> { useJUnitPlatform() }
            allprojects { }
            subprojects { }
            afterEvaluate { }
            """
                .trimIndent(),
        )

        fixture.runAndFail("checkBuildScripts")

        findings() shouldBe
            listOf(
                "lib/build.gradle.kts:1: [eager-api]",
                "lib/build.gradle.kts:2: [eager-api]",
                "lib/build.gradle.kts:3: [eager-api]",
                "lib/build.gradle.kts:4: [cross-project]",
                "lib/build.gradle.kts:5: [cross-project]",
                "lib/build.gradle.kts:6: [cross-project]",
            )
    }

    @Test
    fun `findings are written sorted by path and then numeric line`() {
        val literal = "implementation(\"com.example:lib:1.2.3\")"
        fixture.file("b/build.gradle.kts", "$literal\n")
        // The first literal sits on line 10, so a textual sort would place it after line 12.
        fixture.file(
            "a/build.gradle.kts",
            """
            // padding
            // padding
            // padding
            // padding
            // padding
            // padding
            // padding
            // padding
            // padding
            $literal

            $literal
            """
                .trimIndent(),
        )
        fixture.file("a/other.gradle.kts", "$literal\n")

        fixture.runAndFail("checkBuildScripts")

        findings() shouldBe
            listOf(
                "a/build.gradle.kts:10: [version-literal]",
                "a/build.gradle.kts:12: [version-literal]",
                "a/other.gradle.kts:1: [version-literal]",
                "b/build.gradle.kts:1: [version-literal]",
            )
    }

    @Test
    fun `generated and tool-owned directories are not scanned`() {
        val literal = "implementation(\"com.example:lib:1.2.3\")\n"
        fixture.file("lib/build/generated.gradle.kts", literal)
        fixture.file(".planning/example.gradle.kts", literal)
        fixture.file("lib/node_modules/pkg/example.gradle.kts", literal)

        val result = fixture.run("checkBuildScripts")

        result.task(":checkBuildScripts")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a second run with unchanged inputs is up to date with the same verdict`() {
        fixture.file("lib/build.gradle.kts", "plugins {}\n")
        fixture.run("checkBuildScripts")

        val second = fixture.run("checkBuildScripts").task(":checkBuildScripts")?.outcome

        withClue("second run outcome") {
            listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second
        }
    }
}
