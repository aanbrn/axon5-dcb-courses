// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Proves `checkImageReferences` accepts only interpolated compose images and fully pinned catalog images. */
class ImageReferenceCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private val fixture by lazy {
        GradleFixture.governance(projectDir).apply {
            file(CATALOG, catalog("image-axonserver = \"$AXONSERVER\""))
            file("compose.yaml", "name: courses\ninclude:\n  - path: compose/infra.yaml\n")
        }
    }

    @Test
    fun `interpolated images pass, quoted, commented and with a trailing comment`() {
        fixture.file(
            INFRA,
            services(
                $$"    image: ${AXONSERVER_IMAGE:?run ./gradlew generateComposeVersions}",
                $$"    image: \"${POSTGRES_IMAGE}\" # a trailing comment",
                "    # image: postgres:18",
            ),
        )

        val result = fixture.run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a literal image fails naming the file and the line`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}", "    image: postgres:18"))

        val output = fixture.runAndFail(TASK).output

        output shouldContain $$"compose/infra.yaml:4: image 'postgres:18' must be a ${VARIABLE} interpolation"
        output shouldNotContain "compose/infra.yaml:3"
    }

    @Test
    fun `a literal image in a nested compose directory fails`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file("compose/observability/lgtm.yaml", services("    image: grafana/otel-lgtm:latest"))

        val output = fixture.runAndFail(TASK).output

        output shouldContain "compose/observability/lgtm.yaml:3: image 'grafana/otel-lgtm:latest'"
    }

    @Test
    fun `a catalog image without a digest fails naming the key`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(CATALOG, catalog("image-axonserver = \"$AXONSERVER\"", "image-x = \"postgres:18.7\""))

        val output = fixture.runAndFail(TASK).output

        output shouldContain "gradle/libs.versions.toml: image-x must be <name>:<tag>@sha256:<64 hex digits>"
        output shouldNotContain "image-axonserver must"
    }

    @Test
    fun `a catalog image without a tag fails naming the key`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(CATALOG, catalog("image-axonserver = \"$AXONSERVER\"", "image-x = \"postgres@sha256:$DIGEST\""))

        val output = fixture.runAndFail(TASK).output

        output shouldContain "gradle/libs.versions.toml: image-x must be <name>:<tag>@sha256:<64 hex digits>"
    }

    @Test
    fun `a catalog image that is not a double-quoted string fails naming the key, and a quoted key is checked`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(
            CATALOG,
            catalog(
                "image-axonserver = \"$AXONSERVER\"",
                "image-literal = 'postgres:18.7@sha256:$DIGEST'",
                "image-rich = { require = \"postgres:18.7@sha256:$DIGEST\" }",
                "\"image-quoted\" = \"postgres:18.7\"",
            ),
        )

        val output = fixture.runAndFail(TASK).output

        output shouldContain "image-literal must be a double-quoted <name>:<tag>@sha256:<64 hex digits>"
        output shouldContain "image-rich must be a double-quoted <name>:<tag>@sha256:<64 hex digits>"
        output shouldContain "image-quoted must be <name>:<tag>@sha256:<64 hex digits>, but is 'postgres:18.7'"
        output shouldNotContain "image-axonserver must"
    }

    @Test
    fun `keys that do not start with image- and entries of other tables are not image references`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(
            CATALOG,
            catalog("image-axonserver = \"$AXONSERVER\"", "imagemagick = \"7.1\"") +
                "\n[libraries]\nimage-lib = \"org.example:lib:1.0\"\n",
        )

        val result = fixture.run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a second run with unchanged inputs is up to date and a later literal is still reported`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.run(TASK)

        val second = fixture.run(TASK).task(":$TASK")?.outcome

        withClue("second run outcome") { listOf(TaskOutcome.UP_TO_DATE, TaskOutcome.FROM_CACHE) shouldContain second }
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}", "    image: redis:8"))
        fixture.runAndFail(TASK).output shouldContain "compose/infra.yaml:4: image 'redis:8'"
    }

    private fun catalog(vararg images: String): String =
        "[versions]\njunit = \"6.0.3\"\n" + images.joinToString("\n") + "\n"

    private fun services(vararg imageLines: String): String =
        "services:\n  app:\n" + imageLines.joinToString(separator = "\n") + "\n"

    private companion object {
        const val TASK = "checkImageReferences"
        const val CATALOG = "gradle/libs.versions.toml"
        const val INFRA = "compose/infra.yaml"
        val DIGEST = "0123456789abcdef".repeat(4)
        val AXONSERVER = "docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot@sha256:$DIGEST"
    }
}
