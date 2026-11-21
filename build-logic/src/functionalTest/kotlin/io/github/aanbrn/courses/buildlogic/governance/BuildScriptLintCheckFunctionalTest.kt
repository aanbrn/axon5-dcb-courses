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

/** Proves `checkBuildScripts` rejects version literals and eager or cross-project build APIs. */
class BuildScriptLintCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private val fixture by lazy { GradleFixture.governance(projectDir) }

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

        result.task(":checkBuildScripts")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `a dependency with a literal version is reported with its line`() {
        fixture.file(
            "lib/build.gradle.kts",
            "plugins {}\n\ndependencies {\n    implementation(\"com.example:lib:1.2.3\")\n}\n",
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

        val output = fixture.runAndFail("checkBuildScripts").output

        listOf(1, 2, 3, 4).forEach { line ->
            output shouldContain "lib/build.gradle.kts:$line: [version-literal]"
        }
        output shouldNotContain "lib/build.gradle.kts:5:"
    }

    @Test
    fun `a numeric version in gradle properties and in build-logic sources is reported`() {
        fixture.file("gradle.properties", "group=io.example\nkotlinVersion=2.4.21\n")
        fixture.file(
            "build-logic/src/main/kotlin/Plugin.kt",
            "package demo\n\nval dependency = \"org.example:thing:9.9\"\n",
        )

        val output = fixture.runAndFail("checkBuildScripts").output

        output shouldContain "gradle.properties:2: [version-literal]"
        output shouldContain "build-logic/src/main/kotlin/Plugin.kt:3: [version-literal]"
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

        val output = fixture.runAndFail("checkBuildScripts").output

        listOf(1, 2, 3).forEach { line ->
            output shouldContain "lib/build.gradle.kts:$line: [eager-api]"
        }
        listOf(4, 5, 6).forEach { line ->
            output shouldContain "lib/build.gradle.kts:$line: [cross-project]"
        }
    }

    @Test
    fun `findings are written sorted by path and then numeric line`() {
        val literal = "implementation(\"com.example:lib:1.2.3\")"
        fixture.file("b/build.gradle.kts", "$literal\n")
        fixture.file("a/build.gradle.kts", "\n".repeat(9) + "$literal\n\n$literal\n")
        fixture.file("a/other.gradle.kts", "$literal\n")

        fixture.runAndFail("checkBuildScripts")
        val findings =
            fixture.read("build/reports/governance/build-scripts.txt").lines().filter {
                it.isNotEmpty()
            }

        findings.map { it.substringBefore(" ") } shouldBe
            listOf(
                "a/build.gradle.kts:10:",
                "a/build.gradle.kts:12:",
                "a/other.gradle.kts:1:",
                "b/build.gradle.kts:1:",
            )
    }

    @Test
    fun `generated and tool-owned directories are not scanned`() {
        val literal = "implementation(\"com.example:lib:1.2.3\")\n"
        fixture.file("lib/build/generated.gradle.kts", literal)
        fixture.file(".planning/example.gradle.kts", literal)
        fixture.file("lib/node_modules/pkg/example.gradle.kts", literal)

        val result = fixture.run("checkBuildScripts")

        result.task(":checkBuildScripts")?.outcome shouldBe TaskOutcome.SUCCESS
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
