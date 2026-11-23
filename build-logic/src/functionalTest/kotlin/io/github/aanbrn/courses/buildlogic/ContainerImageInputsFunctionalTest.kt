// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves that the container images of the version catalog and the external Axon Server address are inputs of the
 * integration test task, so that a cached green result can never survive a change that should have re-run the tests.
 *
 * Every run passes `--no-build-cache`: the verdict is whether Gradle considers the task's inputs unchanged (up to date)
 * or changed (executed), and a result restored from the shared local build cache would hide an executed run behind
 * `FROM_CACHE`.
 */
class ContainerImageInputsFunctionalTest {
    @TempDir lateinit var projectDir: Path

    @Test
    fun `a second identical run of the integration suite is up to date`() {
        val fixture = fixtureWithIntegrationTest()
        fixture.integrationTest() shouldBe TaskOutcome.SUCCESS

        val second = fixture.integrationTest()

        UNCHANGED shouldContain second
    }

    @Test
    fun `changing a catalog image runs the integration suite again`() {
        val fixture = fixtureWithIntegrationTest()
        fixture.integrationTest() shouldBe TaskOutcome.SUCCESS

        fixture.changeCatalog(::retagAxonServer)
        val afterChange = fixture.integrationTest()

        withClue("the integration suite must run again after image-axonserver changed") {
            afterChange shouldBe TaskOutcome.SUCCESS
        }
    }

    @Test
    fun `setting the external Axon Server address runs the integration suite again`() {
        val fixture = fixtureWithIntegrationTest()
        fixture.integrationTest() shouldBe TaskOutcome.SUCCESS

        val afterChange = fixture.integrationTest("-Pcourses.axonserver.external=localhost:9999")

        withClue("the integration suite must run again after -Pcourses.axonserver.external was added") {
            afterChange shouldBe TaskOutcome.SUCCESS
        }
    }

    private fun fixtureWithIntegrationTest(): GradleFixture =
        GradleFixture.testSuites(projectDir).apply {
            addTest("integrationTest", "InputsProbeTest", "the integration suite runs", "check(true)")
        }

    private fun GradleFixture.integrationTest(vararg args: String): TaskOutcome {
        val result = run(":lib:integrationTest", "--no-build-cache", *args)
        return checkNotNull(result.task(":lib:integrationTest")) { "the build did not schedule :lib:integrationTest" }
            .outcome
    }

    /** Gives `image-axonserver` another tag and keeps its digest, whatever the current tag is. */
    private fun retagAxonServer(catalog: String): String =
        catalog.replace(AXON_SERVER_IMAGE) { match -> "${match.groupValues[1]}retagged${match.groupValues[2]}" }

    private companion object {
        val UNCHANGED = setOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE)
        val AXON_SERVER_IMAGE = Regex("""(?m)^(image-axonserver = "[^:"]+:)[^@"]+(@sha256:[0-9a-f]+")$""")
    }
}
