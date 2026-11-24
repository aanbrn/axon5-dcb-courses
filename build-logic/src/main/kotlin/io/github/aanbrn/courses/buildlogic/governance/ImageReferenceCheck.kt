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
 * - every `image-*` key of the `[versions]` table of the [catalog] is a double-quoted string of the form
 *   `<name>:<tag>@sha256:<64 hex digits>`, so the tag names the version for readers and Renovate, and the digest fixes
 *   the content. Every such key is matched, bare or quoted and at any indentation, because `catalogProblems` trims each
 *   catalog line before it is matched, and a value of any other shape (a literal string, an inline table, a multi-line
 *   string) is reported, because the Renovate custom manager reads only the double-quoted form and such an entry would
 *   never be updated.
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

    /** The `image-*` entries of the `[versions]` table that are not a double-quoted tag-and-digest reference. */
    private fun catalogProblems(path: String, lines: List<String>): List<String> {
        val problems = mutableListOf<String>()
        var inVersions = false
        for (line in lines.map { it.trim() }) {
            if (line.startsWith("[")) {
                inVersions = VERSIONS_HEADER.matches(line)
            } else if (inVersions) {
                catalogProblem(path, line)?.let { problems += it }
            }
        }
        return problems
    }

    private fun catalogProblem(path: String, line: String): String? {
        val (_, key, value) = CATALOG_ENTRY.matchEntire(line)?.destructured ?: return null
        val reference = DOUBLE_QUOTED_VALUE.matchEntire(value)?.groupValues?.get(1)
        return when {
            reference == null -> "$path: $key must be a double-quoted $PINNED_FORM, but is '$value'"
            PINNED_REFERENCE.matches(reference) -> null
            else -> "$path: $key must be $PINNED_FORM, but is '$reference'"
        }
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

        /** The shape of a pinned image reference, as the problems name it. */
        const val PINNED_FORM = "<name>:<tag>@sha256:<64 hex digits>"

        /**
         * An `image-*` entry of a TOML table, whatever its value: the quote of the key (empty for a bare key, the same
         * quote on both sides otherwise), the key, and the rest of the line after the equals sign. It is matched
         * against a catalog line whose leading and trailing blanks `catalogProblems` has already removed, so the `^`
         * anchor does not skip an indented entry.
         */
        val CATALOG_ENTRY = Regex("""^(["']?)(image-[A-Za-z0-9_.-]+)\1\s*=\s*(.*)$""")

        /** A double-quoted string with an optional trailing comment: the capture is the text between the quotes. */
        val DOUBLE_QUOTED_VALUE = Regex("""^"([^"]*)"\s*(?:#.*)?$""")

        /**
         * `[registry[:port]/][path/]name:tag@sha256:digest`. A colon followed by digits before the first slash is a
         * registry port, not a tag, so `registry:5000/app@sha256:...` has no tag and fails.
         */
        val PINNED_REFERENCE =
            Regex("""^(?:[^@\s/:]+(?::[0-9]+)?/)?(?:[^@\s/:]+/)*[^@\s/:]+:[^@\s/:]+@sha256:[0-9a-f]{64}$""")
    }
}
