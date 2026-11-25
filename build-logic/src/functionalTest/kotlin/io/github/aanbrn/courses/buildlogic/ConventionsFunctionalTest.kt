// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves the build conventions behave under the configuration cache and isolated projects. */
class ConventionsFunctionalTest {
    @TempDir lateinit var projectDir: Path

    @Test
    fun `a second run reuses the configuration cache`() {
        val fixture = GradleFixture.standard(projectDir)

        fixture.run(":lib:build").output shouldNotContain REUSING
        fixture.run(":lib:build").output shouldContain REUSING
    }

    @Test
    fun `editing the java-version file invalidates the configuration cache`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.run(":lib:build")

        fixture.file(".java-version", "25\n\n")
        val output = fixture.run(":lib:build").output

        output shouldNotContain REUSING
        output shouldContain ".java-version"
    }

    @Test
    fun `a java-version file that is not a numeric JDK major version fails the build with its text`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.file(".java-version", "temurin-25\n")

        val output = fixture.runAndFail(":lib:build").output

        output shouldContain ".java-version must hold a numeric JDK major version, but it holds 'temurin-25'"
    }

    @Test
    fun `a module without dependencies gets the platform on implementation exactly once`() {
        val fixture = GradleFixture.standard(projectDir)

        val output = fixture.run(":lib:dependencies", "--configuration", "implementation").output

        withClue(output) { PLATFORM.findAll(output).count() shouldBe 1 }
    }

    @Test
    fun `a module without dependencies gets the platform on testImplementation exactly once`() {
        val fixture = GradleFixture.standard(projectDir)

        val output = fixture.run(":lib:dependencies", "--configuration", "testImplementation").output

        withClue(output) { PLATFORM.findAll(output).count() shouldBe 1 }
    }

    private companion object {
        const val REUSING = "Reusing configuration cache."
        // Gradle prints the dependency as `project platform` or `project :platform`.
        val PLATFORM = Regex("project :?platform")
    }
}
