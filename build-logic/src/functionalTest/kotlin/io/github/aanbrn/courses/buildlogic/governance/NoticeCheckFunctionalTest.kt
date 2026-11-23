// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves `checkNotice` keeps `NOTICE` mirroring every exception of the licence policy (CICD-09, D-31). */
class NoticeCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(notice: String, vararg artifacts: String): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            val exceptions =
                artifacts.joinToString(",") {
                    """{ "artifact": "$it", "license": "Vendor Terms", "reason": "Justified in the test." }"""
                }
            file("governance/dependency-licences.json", """{ "allowed": [], "exceptions": [$exceptions] }""")
            file("NOTICE", notice)
        }

    @Test
    fun `a NOTICE that names every exception artifact and the Axoniq terms passes`() {
        val notice = notice("`io.axoniq:connector`", "`io.axoniq.framework:server`")

        val result = fixture(notice, "io.axoniq:connector", "io.axoniq.framework:server").run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `an exception artifact missing from NOTICE fails naming it`() {
        val output =
            fixture(notice("`io.axoniq:connector`"), "io.axoniq:connector", "io.axoniq:other").runAndFail(TASK).output

        output shouldContain "missing from NOTICE: io.axoniq:other"
        output shouldNotContain "missing from NOTICE: io.axoniq:connector"
    }

    @Test
    fun `an artifact that only extends the name of an exception artifact does not count as a mention`() {
        val output = fixture(notice("`io.axoniq:connector-extra`"), "io.axoniq:connector").runAndFail(TASK).output

        output shouldContain "missing from NOTICE: io.axoniq:connector"
    }

    @Test
    fun `an artifact followed by sentence punctuation counts as a mention`() {
        val result = fixture(notice("The connector is `io.axoniq:connector`."), "io.axoniq:connector").run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a NOTICE without the Axoniq terms URL fails even when no exception exists`() {
        val output = fixture("Third-party components\n").runAndFail(TASK).output

        output shouldContain "missing from NOTICE: $TERMS_URL"
    }

    private fun notice(vararg mentions: String): String =
        "Third-party components\n\nAxoniq Framework terms: $TERMS_URL\n\n" + mentions.joinToString("\n") + "\n"

    private companion object {
        const val TASK = "checkNotice"
        const val TERMS_URL = "https://www.axoniq.io/legal/terms-of-service"
    }
}
