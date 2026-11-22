// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves each quality gate fails the build on a violation instead of passing silently. */
class QualityGatesFunctionalTest {
    @TempDir lateinit var projectDir: Path

    @Test
    fun `an undocumented public class fails detekt`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.file(
            "lib/src/main/kotlin/fixture/Undocumented.kt",
            "${GradleFixture.HEADER}\npackage fixture\n\nclass Undocumented\n",
        )

        val result = fixture.runAndFail(":lib:detekt")

        result.task(":lib:detekt")?.outcome shouldBe TaskOutcome.FAILED
        result.output shouldContain "UndocumentedPublicClass"
    }

    @Test
    fun `a KDoc link to a missing symbol fails Dokka`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.file(
            "lib/src/main/kotlin/fixture/Linked.kt",
            "${GradleFixture.HEADER}\npackage fixture\n\n" +
                "/** Points at [MissingSymbol], which does not exist. */\nclass Linked\n",
        )

        val result = fixture.runAndFail(":lib:dokkaGeneratePublicationHtml")

        result.task(":lib:dokkaGeneratePublicationHtml")?.outcome shouldBe TaskOutcome.FAILED
        result.output shouldContain "MissingSymbol"
    }

    @Test
    fun `an unformatted Kotlin file fails spotlessCheck and spotlessApply fixes it`() {
        val fixture = GradleFixture.standard(projectDir)
        // Normalises the scaffolding, so that the failure below is caused by the messy file only.
        fixture.run("spotlessApply")
        fixture.run("spotlessCheck")
        fixture.file(
            "lib/src/main/kotlin/fixture/Messy.kt",
            "${GradleFixture.HEADER}\npackage fixture\n\n" +
                "/** A documented public class. */\nclass   Messy( val   name : String )\n",
        )

        val failed = fixture.runAndFail("spotlessCheck")
        failed.output shouldContain "Messy.kt"

        fixture.run("spotlessApply")
        fixture.run("spotlessCheck")
        fixture.read("lib/src/main/kotlin/fixture/Messy.kt") shouldContain "class Messy(val name: String)"
    }

    @Test
    fun `a Kotlin file without the SPDX header fails spotlessCheck and spotlessApply adds it`() {
        val fixture = GradleFixture.standard(projectDir)
        fixture.run("spotlessApply")
        fixture.run("spotlessCheck")
        fixture.file(
            "lib/src/main/kotlin/fixture/Headerless.kt",
            "package fixture\n\n/** A documented public class. */\nclass Headerless\n",
        )

        val failed = fixture.runAndFail("spotlessCheck")
        failed.output shouldContain "Headerless.kt"

        fixture.run("spotlessApply")
        fixture.read("lib/src/main/kotlin/fixture/Headerless.kt") shouldStartWith GradleFixture.HEADER
    }
}
