// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when a container image is referenced in a way that can drift (INFRA-09, D-23).
 *
 * Two rules keep the version catalog the one source of every image:
 * - every `image:` value in the [composeFiles] is a `${VARIABLE}` interpolation, optionally with a `:?message` default
 *   check, so the reference comes from the generated `compose/versions.env` and never from a literal;
 * - every `image-*` key of the `[versions]` table of the [catalog] is `<name>:<tag>@sha256:<64 hex digits>`, so the tag
 *   names the version for readers and Renovate, and the digest fixes the content.
 *
 * Compose files are read line by line instead of with a YAML library: only `image:` entries are needed, and an extra
 * parser dependency in `build-logic` is not worth that. Commented-out lines are skipped. Findings are written as
 * `path:line: message` (compose) or `path: message` (catalog), sorted, so the report is identical on every run.
 */
@CacheableTask
abstract class ImageReferenceCheck @Inject constructor(private val layout: ProjectLayout) : DefaultTask() {

    /** The compose files to scan, a file tree rooted at the project directory. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val composeFiles: ConfigurableFileCollection

    /** The version catalog, `gradle/libs.versions.toml`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val catalog: RegularFileProperty

    /** The file the sorted findings are written to; empty when there are none. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Checks every compose `image:` value and every catalog `image-*` entry, and fails with all findings. */
    @TaskAction
    fun check() {
        val problems = buildList {
            composeFiles.asFileTree.visit {
                if (!isDirectory) {
                    addAll(composeProblems(relativePath.pathString, file.readLines()))
                }
            }
            val catalogPath =
                layout.projectDirectory.asFile.toPath().relativize(catalog.get().asFile.toPath()).joinToString("/")
            addAll(catalogProblems(catalogPath, catalog.get().asFile.readLines()))
        }
        val sorted = problems.sorted()
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(sorted.joinToString(separator = "") { "$it\n" })
        }
        if (sorted.isNotEmpty()) {
            throw GradleException(failureMessage(sorted))
        }
    }

    private fun composeProblems(path: String, lines: List<String>): List<String> =
        lines.withIndex().mapNotNull { (index, raw) ->
            val value = IMAGE_LINE.matchEntire(raw)?.groupValues?.get(1)?.let(::unquote)
            if (value == null || INTERPOLATION.matches(value)) {
                null
            } else {
                $$"$$path:$${index + 1}: image '$$value' must be a ${VARIABLE} interpolation of a generated value"
            }
        }

    /** The `image-*` entries of the `[versions]` table that are not a tag-and-digest reference. */
    private fun catalogProblems(path: String, lines: List<String>): List<String> {
        val problems = mutableListOf<String>()
        var inVersions = false
        for (line in lines.map { it.trim() }) {
            if (line.startsWith("[")) {
                inVersions = VERSIONS_HEADER.matches(line)
            } else if (inVersions) {
                CATALOG_IMAGE.find(line)?.let { entry ->
                    val (key, reference) = entry.destructured
                    if (!PINNED_REFERENCE.matches(reference)) {
                        problems += "$path: $key must be <name>:<tag>@sha256:<64 hex digits>, but is '$reference'"
                    }
                }
            }
        }
        return problems
    }

    private fun unquote(value: String): String =
        value.substringBefore(" #").trim().let { text ->
            if (text.length >= 2 && text.first() == text.last() && text.first() in QUOTES) {
                text.substring(1, text.length - 1)
            } else {
                text
            }
        }

    private fun failureMessage(problems: List<String>): String = buildString {
        appendLine("${problems.size} container image reference problem(s):")
        problems.forEach { appendLine("  $it") }
        appendLine()
        append(
            $$"Compose files reference images only as ${VARIABLE}, which ./gradlew generateComposeVersions writes " +
                "from the image-* entries of gradle/libs.versions.toml, and each entry is pinned by tag and " +
                "digest (INFRA-09).",
        )
    }

    private companion object {
        const val QUOTES = "\"'"

        /** An `image:` key at the start of a line or of a list item; the capture is the rest of the line. */
        val IMAGE_LINE = Regex("""^\s*(?:-\s+)?image:\s*(.*)$""")

        /** `${NAME}` or `${NAME:?message}`. */
        val INTERPOLATION = Regex("""^\$\{[A-Z0-9_]+(:\?[^}]*)?}$""")

        val VERSIONS_HEADER = Regex("""\[versions]\s*(#.*)?""")

        /** A double-quoted `image-*` entry of a TOML table: the key and the value. */
        val CATALOG_IMAGE = Regex("""^(image-[A-Za-z0-9_.-]+)\s*=\s*"([^"]*)"""")

        /**
         * `[registry[:port]/][path/]name:tag@sha256:digest`. A colon followed by digits before the first slash is a
         * registry port, not a tag, so `registry:5000/app@sha256:...` has no tag and fails.
         */
        val PINNED_REFERENCE =
            Regex("""^(?:[^@\s/:]+(?::[0-9]+)?/)?(?:[^@\s/:]+/)*[^@\s/:]+:[^@\s/:]+@sha256:[0-9a-f]{64}$""")
    }
}
