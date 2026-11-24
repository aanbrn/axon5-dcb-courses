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
 * - every `image-*` key of the `[versions]` table of the [catalog] is detected, bare or quoted and at any indentation
 *   (`catalogProblems` trims each line), and must be written exactly as the Renovate custom manager for images in
 *   `renovate.json5` reads it: a bare key `image-[a-z0-9-]+`, one space on each side of `=`, and a double-quoted
 *   `<name>:<tag>@sha256:<64 hex digits>` whose name holds no registry port; a trailing comment is allowed. The tag
 *   names the version for readers and Renovate, and the digest fixes the content. Any other form (a quoted or
 *   upper-case key, another separator, a literal string, an inline table, a multi-line string, a registry port) is
 *   reported, because the manager would skip or misread it and never update the entry. Every entry the check accepts is
 *   read by the manager with its name, tag and digest. The check is stricter only where the manager would also read a
 *   reference that is not a valid pinned image. The converse holds too: every other catalog line on which the manager
 *   reads an image (a commented-out entry, an entry of another table, a key such as `old-image-x` whose `image-x` part
 *   the manager's word boundary finds, or an entry in a trailing comment, also after an accepted entry) is reported
 *   with its line, so the manager reads only entries that the check verifies. The functional test matches its samples
 *   against the regex it reads from `renovate.json5`, so a change on either side fails the build.
 *
 * Compose files are read line by line instead of with a YAML library: only `image:` entries are needed, and an extra
 * parser dependency in `build-logic` is not worth that. Commented-out lines are skipped. Findings are written as
 * `path:line: message` for compose files and for the catalog lines that the manager reads outside the verified entries,
 * and as `path: message` for the other catalog findings, sorted, so the report is identical on every run.
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

    /**
     * The `image-*` entries of the `[versions]` table that are not a double-quoted tag-and-digest reference, and every
     * other line (any table, comments included) on which the Renovate image manager reads an image.
     */
    private fun catalogProblems(path: String, lines: List<String>): List<String> {
        val problems = mutableListOf<String>()
        var inVersions = false
        for ((index, raw) in lines.withIndex()) {
            val line = raw.trim()
            val header = line.startsWith("[")
            if (header) {
                inVersions = VERSIONS_HEADER.matches(line)
            } else if (inVersions) {
                catalogProblem(path, line)?.let { problems += it }
            }
            problems += readOutsideEntries(path, index + 1, line, verified = inVersions && !header)
        }
        return problems
    }

    /**
     * The findings for the places of one trimmed catalog line that the Renovate image manager reads although
     * [catalogProblem] never verifies them: every [MANAGER_READ] match, except the entry that starts a `[versions]`
     * line (`verified`), which [catalogProblem] has checked.
     */
    private fun readOutsideEntries(path: String, number: Int, line: String, verified: Boolean): List<String> =
        MANAGER_READ.findAll(line)
            .filterNot { verified && it.range.first == 0 }
            .map {
                "$path:$number: the Renovate image manager reads ${it.value.substringBefore(" = ")} here, " +
                    "outside the [versions] entries this check verifies; " +
                    "delete it or make it an image-* entry of [versions]"
            }
            .toList()

    /** The first problem of one catalog line, in the order of the shared form; `null` for a line that is fine. */
    private fun catalogProblem(path: String, line: String): String? {
        val entry = CATALOG_ENTRY.matchEntire(line) ?: return null
        val key = entry.group("key")
        val value = entry.group("value")
        val reference = DOUBLE_QUOTED_VALUE.matchEntire(value)?.groupValues?.get(1)
        return when {
            entry.group("quote").isNotEmpty() || !MANAGED_KEY.matches(key) ->
                "$path: $key must be a bare key of the form $KEY_FORM, the only key the Renovate image manager reads"
            entry.group("before") != " " || entry.group("after") != " " ->
                "$path: $key must have exactly one space on each side of '=', " +
                    "the only separator the Renovate image manager reads"
            reference == null -> "$path: $key must be a double-quoted $PINNED_FORM, but is '$value'"
            REGISTRY_PORT.containsMatchIn(reference) ->
                "$path: $key must not name a registry port, which the Renovate image manager reads as part of the " +
                    "tag, but is '$reference'"
            PINNED_REFERENCE.matches(reference) -> null
            else -> "$path: $key must be $PINNED_FORM, but is '$reference'"
        }
    }

    private fun MatchResult.group(name: String): String = groups[name]?.value.orEmpty()

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
                "digest and written in the form the Renovate image manager reads (INFRA-09).",
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

        /** The key class of the Renovate image manager in `renovate.json5`, as the problems name it. */
        const val KEY_FORM = "image-[a-z0-9-]+"

        /** A key the Renovate image manager reads; the functional test matches the same class against its regex. */
        val MANAGED_KEY = Regex(KEY_FORM)

        /**
         * A superset of what the Renovate image manager matches on one line: its key class and separator (`image-` key,
         * one space, `=`, one space), then a double-quoted value that reaches `@sha256:`. The leading word boundary is
         * the manager's own, which also holds after `-`, `.` and `#`, so a comment (`# image-x = ...`) and a key such
         * as `old-image-x` match too. Requiring the digest keeps aliases that the manager never reads silent, such as
         * `image-lib = "org.example:lib:1.0"` in `[libraries]`. The functional test compares samples with the manager's
         * regex from `renovate.json5`.
         */
        val MANAGER_READ = Regex("""\bimage-[a-z0-9-]+ = "[^"\s]*@sha256:""")

        /**
         * An `image-*` entry of a TOML table, whatever its spelling: the quote of the key (empty for a bare key, the
         * same quote on both sides otherwise), the key (every character up to the closing quote, a blank or `=`), the
         * blanks before the equals sign, the blanks after it, and the rest of the line. It is matched against a catalog
         * line whose leading and trailing blanks `catalogProblems` has already removed, so the `^` anchor does not skip
         * an indented entry.
         */
        val CATALOG_ENTRY =
            Regex("""^(?<quote>["']?)(?<key>image-[^"'\s=]*)\k<quote>(?<before>\s*)=(?<after>\s*)(?<value>.*)$""")

        /** A double-quoted string with an optional trailing comment: the capture is the text between the quotes. */
        val DOUBLE_QUOTED_VALUE = Regex("""^"([^"]*)"\s*(?:#.*)?$""")

        /**
         * A colon followed by digits in the first segment before a slash: a registry port. The Renovate image manager
         * ends the image name at the first colon, so it would read `localhost:5000/app:1.0` as the image `localhost`
         * with the tag `5000/app:1.0`, and never update the right image.
         */
        val REGISTRY_PORT = Regex("""^[^@\s/:]+:[0-9]+/""")

        /**
         * `[registry/][path/]name:tag@sha256:digest`. The name holds no colon, as in the `depName` group of the
         * Renovate image manager, which ends the name at the first colon.
         */
        val PINNED_REFERENCE = Regex("""^(?:[^@\s/:]+/)*[^@\s/:]+:[^@\s/:]+@sha256:[0-9a-f]{64}$""")
    }
}
