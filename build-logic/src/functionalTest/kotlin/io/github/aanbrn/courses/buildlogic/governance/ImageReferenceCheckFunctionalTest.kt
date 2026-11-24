// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
        output shouldContain "image-quoted must be a bare key of the form image-[a-z0-9-]+"
        output shouldNotContain "image-axonserver must"
    }

    @Test
    fun `an indented catalog image that is not pinned fails naming the key, also under an indented header`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(
            CATALOG,
            listOf(
                    "  [versions]",
                    "  junit = \"6.0.3\"",
                    "  image-axonserver = \"$AXONSERVER\"",
                    "  image-spaces = \"postgres:18.7\"",
                    "\timage-tab = 'postgres:18.7@sha256:$DIGEST'",
                )
                .joinToString("\n", postfix = "\n"),
        )

        val output = fixture.runAndFail(TASK).output

        output shouldContain
            "gradle/libs.versions.toml: image-spaces must be <name>:<tag>@sha256:<64 hex digits>, but is 'postgres:18.7'"
        output shouldContain "image-tab must be a double-quoted <name>:<tag>@sha256:<64 hex digits>"
        output shouldNotContain "image-axonserver must"
    }

    @Test
    fun `an indented pinned catalog image passes`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(
            CATALOG,
            catalog("  image-axonserver = \"$AXONSERVER\"", "\timage-postgres = \"postgres:18.7@sha256:$DIGEST\""),
        )

        val result = fixture.run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
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

    @Test
    fun `every entry shape that the Renovate image manager reads passes`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(CATALOG, catalog(*ACCEPTED.map { it.line }.toTypedArray()))

        val result = fixture.run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `a key that Renovate does not read fails naming each key with the bare key message`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(
            CATALOG,
            catalog("image-axonserver = \"$AXONSERVER\"", *REJECTED_KEYS.map { it.line }.toTypedArray()),
        )

        val output = fixture.runAndFail(TASK).output

        REJECTED_KEYS.forEach {
            output shouldContain
                "gradle/libs.versions.toml: ${it.key} must be a bare key of the form image-[a-z0-9-]+, " +
                    "the only key the Renovate image manager reads"
        }
        output shouldNotContain "image-axonserver must"
    }

    @Test
    fun `a separator that Renovate does not read fails naming each key with the separator message`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(
            CATALOG,
            catalog("image-axonserver = \"$AXONSERVER\"", *REJECTED_SEPARATORS.map { it.line }.toTypedArray()),
        )

        val output = fixture.runAndFail(TASK).output

        REJECTED_SEPARATORS.forEach {
            output shouldContain
                "gradle/libs.versions.toml: ${it.key} must have exactly one space on each side of '=', " +
                    "the only separator the Renovate image manager reads"
        }
        output shouldNotContain "image-axonserver must"
    }

    @Test
    fun `a registry port fails naming the key, because Renovate would read the port as part of the tag`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        fixture.file(CATALOG, catalog("image-axonserver = \"$AXONSERVER\"", PORT_LINE))

        val output = fixture.runAndFail(TASK).output

        output shouldContain
            "gradle/libs.versions.toml: image-port must not name a registry port, which the Renovate image " +
                "manager reads as part of the tag, but is 'localhost:5000/app:1.0@sha256:$DIGEST'"
        output shouldNotContain "image-axonserver must"
    }

    @Test
    fun `the image regex of renovate json5 reads every accepted entry exactly and no rejected key or separator`() {
        val regex = renovateImageRegex()

        ACCEPTED.forEach { sample ->
            val match = withClue("accepted entry: ${sample.line}") { regex.find(sample.line).shouldNotBeNull() }
            withClue("depName of ${sample.line}") { match.groups["depName"]?.value shouldBe sample.name }
            withClue("currentValue of ${sample.line}") { match.groups["currentValue"]?.value shouldBe sample.tag }
            withClue("currentDigest of ${sample.line}") {
                match.groups["currentDigest"]?.value shouldBe "sha256:$DIGEST"
            }
        }
        (REJECTED_KEYS + DOTTED_KEY + REJECTED_SEPARATORS).forEach { sample ->
            withClue("rejected entry: ${sample.line}") { regex.find(sample.line) shouldBe null }
        }
        // The manager does match a registry port, but reads `localhost` as the image and `5000/app:1.0` as the tag.
        val port = withClue("port entry") { regex.find(PORT_LINE).shouldNotBeNull() }
        port.groups["depName"]?.value shouldBe "localhost"
        port.groups["currentValue"]?.value shouldBe "5000/app:1.0"
    }

    @Test
    fun `a catalog line that Renovate reads outside the verified entries fails naming its line and key`() {
        fixture.file(INFRA, services($$"    image: ${AXONSERVER_IMAGE}"))
        val text =
            catalog(
                "image-axonserver = \"$AXONSERVER\"",
                *READ_ELSEWHERE.dropLast(1).map { it.line }.toTypedArray(),
            ) + "\n[libraries]\nimage-lib = \"org.example:lib:1.0\"\n${READ_ELSEWHERE.last().line}\n"
        fixture.file(CATALOG, text)
        val lines = text.lines()

        val output = fixture.runAndFail(TASK).output

        READ_ELSEWHERE.forEach {
            output shouldContain
                "gradle/libs.versions.toml:${lines.indexOf(it.line) + 1}: the Renovate image manager reads " +
                    "${it.key} here, outside the [versions] entries this check verifies"
        }
        output shouldNotContain "image-postgres must"
        output shouldNotContain "reads image-postgres here"
        output shouldNotContain "image-axonserver must"
        output shouldNotContain "reads image-lib here"
    }

    @Test
    fun `the image regex of renovate json5 reads every line reported as read elsewhere and no silent control`() {
        val regex = renovateImageRegex()

        READ_ELSEWHERE.forEach { sample ->
            val keys = regex.findAll(sample.line).map { it.value.substringBefore(" = ") }.toList()
            withClue("keys that the manager reads from ${sample.line}") { keys shouldContain sample.key }
        }
        SILENT_CONTROLS.forEach { line -> withClue("silent control: $line") { regex.find(line) shouldBe null } }
    }

    /** The image `matchStrings` entry of `renovate.json5`, unescaped from its JSON5 single-quoted form. */
    private fun renovateImageRegex(): Regex {
        fixture.copyFromRepo("renovate.json5")
        val candidates =
            RENOVATE_IMAGE_STRING.findAll(fixture.read("renovate.json5")).map { it.groupValues[1] }.toList()
        withClue("single-quoted matchStrings of renovate.json5 that hold the catalog key class") {
            candidates shouldHaveSize 1
        }
        return Regex(candidates.single().replace("\\\\", "\\"))
    }

    private fun catalog(vararg images: String): String =
        "[versions]\njunit = \"6.0.3\"\n" + images.joinToString("\n") + "\n"

    private fun services(vararg imageLines: String): String =
        "services:\n  app:\n" + imageLines.joinToString(separator = "\n") + "\n"

    /** A catalog entry that the check accepts, with the name and tag the Renovate manager must read from it. */
    private data class Accepted(val line: String, val name: String, val tag: String)

    /** A catalog entry that the check rejects, with the key that the finding must name. */
    private data class Rejected(val line: String, val key: String)

    private companion object {
        const val TASK = "checkImageReferences"
        const val CATALOG = "gradle/libs.versions.toml"
        const val INFRA = "compose/infra.yaml"
        val DIGEST = "0123456789abcdef".repeat(4)
        val AXONSERVER = "docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot@sha256:$DIGEST"
        val POSTGRES = "postgres:18.7@sha256:$DIGEST"

        /**
         * The line of `renovate.json5` that holds the image regex: a single-quoted string with the catalog key class.
         */
        val RENOVATE_IMAGE_STRING = Regex("""(?m)^\s*'([^'\n]*image-\[a-z0-9-]\+[^'\n]*)',?$""")

        val ACCEPTED =
            listOf(
                Accepted(
                    "image-axonserver = \"$AXONSERVER\"",
                    "docker.axoniq.io/axoniq/axonserver",
                    "2026.1.8-nonroot",
                ),
                Accepted("image-postgres = \"$POSTGRES\"", "postgres", "18.7"),
                Accepted(
                    "  image-indented = \"testcontainers/ryuk:0.14.0@sha256:$DIGEST\"",
                    "testcontainers/ryuk",
                    "0.14.0",
                ),
                Accepted("\timage-tabbed = \"$POSTGRES\"", "postgres", "18.7"),
                Accepted("image-commented = \"$POSTGRES\" # a trailing comment", "postgres", "18.7"),
                Accepted("image-a1-b2 = \"$POSTGRES\"", "postgres", "18.7"),
            )

        val REJECTED_KEYS =
            listOf(
                Rejected("\"image-double-quoted\" = \"$POSTGRES\"", "image-double-quoted"),
                Rejected("'image-single-quoted' = \"$POSTGRES\"", "image-single-quoted"),
                Rejected("image-Upper = \"$POSTGRES\"", "image-Upper"),
                Rejected("image-under_score = \"$POSTGRES\"", "image-under_score"),
            )

        /** A dotted key is no catalog alias, so Gradle rejects it before the check runs; only the regex is compared. */
        val DOTTED_KEY = Rejected("image-dot.ted = \"$POSTGRES\"", "image-dot.ted")

        val REJECTED_SEPARATORS =
            listOf(
                Rejected("image-tight=\"$POSTGRES\"", "image-tight"),
                Rejected("image-wide  = \"$POSTGRES\"", "image-wide"),
                Rejected("image-wide-after =  \"$POSTGRES\"", "image-wide-after"),
                Rejected("image-tab-sep\t= \"$POSTGRES\"", "image-tab-sep"),
            )

        val PORT_LINE = "image-port = \"localhost:5000/app:1.0@sha256:$DIGEST\""

        /**
         * Lines on which the Renovate image manager reads an image although they are not an `image-*` entry at the
         * start of a `[versions]` line, with the key the finding must name: a commented-out older entry of a live key,
         * a `[versions]` alias that ends in an image key, an entry in the trailing comment of an accepted entry, and a
         * commented-out entry of another table (the last one is written under `[libraries]`).
         */
        val READ_ELSEWHERE =
            listOf(
                Rejected(
                    "# image-axonserver = \"docker.axoniq.io/axoniq/axonserver:2026.1.7-nonroot@sha256:$DIGEST\"",
                    "image-axonserver",
                ),
                Rejected("old-image-x = \"$POSTGRES\"", "image-x"),
                Rejected(
                    "image-postgres = \"$POSTGRES\" # was image-previous = \"postgres:18.6@sha256:$DIGEST\"",
                    "image-previous",
                ),
                Rejected("# image-library = \"$POSTGRES\"", "image-library"),
            )

        /** Lines that the manager does not read, so the check keeps silent about them. */
        val SILENT_CONTROLS =
            listOf("image-lib = \"org.example:lib:1.0\"", "# image-* entries carry a tag and a digest")
    }
}
