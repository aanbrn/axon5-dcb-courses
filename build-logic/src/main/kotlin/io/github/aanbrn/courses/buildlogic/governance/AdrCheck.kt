// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import java.time.DateTimeException
import java.time.LocalDate
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when an architecture decision record under `docs/adr/` breaks the MADR front matter, numbering or
 * structure rules (DOC-03, D-38). The template is `docs/templates/adr.md`, which is never checked.
 *
 * **File.** Every Markdown file directly in `docs/adr/` is named `NNNN-<slug>.md`: four digits from `0001`, a lowercase
 * slug. The numbers run from `0001` without gaps, and no two files share a number.
 *
 * **Front matter.** The block is read by the shared [FrontMatter] subset parser. The keys are `status`, `date` and
 * `decision-makers`, which are required, and `consulted` and `informed`, which are optional; any other key fails. The
 * `status` is `proposed`, `accepted`, `rejected`, `deprecated` or `superseded by ADR-NNNN`, naming another existing
 * ADR. The `date` is a valid `YYYY-MM-DD` day. `decision-makers`, `consulted` and `informed` are a string or a list of
 * strings without blank items; `decision-makers` is not empty.
 *
 * **Structure.** The first level-1 heading is `# ADR-NNNN: <title>` with the number of the file name, and the level-2
 * sections Context and Problem Statement, Considered Options and Decision Outcome exist and are not empty. Headings
 * inside code fences do not count.
 *
 * Problems are reported as `path:line: message`, sorted by path, line and message, so the report is deterministic. The
 * task collects every problem of every file before it fails with one exception.
 */
@CacheableTask
abstract class AdrCheck : DefaultTask() {

    /** The ADR files, the Markdown files directly in `docs/adr/`, as a file tree rooted at the repository. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val adrs: ConfigurableFileCollection

    /** The file the sorted problems are written to; empty when every ADR follows the rules. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Validates every ADR, writes [report] and fails with every problem found. */
    @TaskAction
    fun check() {
        val problems = AdrRules.validate(adrFiles()).sortedWith(compareBy({ it.path }, { it.line }, { it.message }))
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(problems.joinToString(separator = "") { it.render() + "\n" })
        }
        if (problems.isNotEmpty()) {
            throw GradleException(failureMessage(problems))
        }
    }

    private fun adrFiles(): List<AdrFile> {
        val files = mutableListOf<AdrFile>()
        adrs.asFileTree.visit {
            if (!isDirectory) files += AdrFile(relativePath.pathString, file.readLines())
        }
        return files.sortedBy { it.path }
    }

    private fun failureMessage(problems: List<AdrProblem>): String = buildString {
        appendLine("${problems.size} ADR problem(s), also written to ${report.get().asFile.path}:")
        problems.forEach { appendLine("  ${it.render()}") }
        appendLine()
        append(
            "Every ADR in docs/adr/ is named NNNN-<slug>.md, numbered from 0001 without gaps, and follows " +
                "docs/templates/adr.md (DOC-03).",
        )
    }
}

/** One ADR file: its repository-relative [path] and [lines]. */
private class AdrFile(val path: String, val lines: List<String>) {
    /** The four-digit number of the file name, or null when the name is malformed. */
    val number: String? = AdrRules.FILE_NAME.matchEntire(path.substringAfterLast('/'))?.groupValues?.get(1)
}

/** One violation: [message] about [path], on [line] when it is positive. */
private class AdrProblem(val path: String, val line: Int, val message: String) {
    fun render(): String = if (line > 0) "$path:$line: $message" else "$path: $message"
}

/** The ADR rules, kept apart from the task so they read as one specification. */
private object AdrRules {
    val FILE_NAME = Regex("^([0-9]{4})-[a-z0-9-]+\\.md$")
    private const val DIRECTORY = "docs/adr"
    private const val STATUS_SUPERSEDED_PREFIX = "superseded by ADR-"
    private val STATUSES = listOf("proposed", "accepted", "rejected", "deprecated")
    private val SUPERSEDED = Regex("^superseded by ADR-([0-9]{4})$")
    private val REQUIRED_KEYS = listOf("status", "date", "decision-makers")
    private val OPTIONAL_KEYS = listOf("consulted", "informed")
    private val DATE = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    private val TITLE = Regex("^# ADR-([0-9]{4}): (\\S.*)$")
    private val SECTIONS = listOf("Context and Problem Statement", "Considered Options", "Decision Outcome")

    fun validate(files: List<AdrFile>): List<AdrProblem> {
        val known = files.mapNotNull { it.number }.toSet()
        return buildList {
            files.flatMapTo(this) { validate(it, known) }
            addAll(duplicates(files))
            addAll(gaps(known))
        }
    }

    private fun validate(file: AdrFile, known: Set<String>): List<AdrProblem> {
        val problems = mutableListOf<AdrProblem>()
        if (file.number == null) {
            problems +=
                AdrProblem(file.path, 0, "the file name must match NNNN-<slug>.md: four digits, a lowercase slug")
        }
        val front =
            try {
                FrontMatter.parse(file.path, file.lines)
            } catch (failure: FrontMatter.Failure) {
                return problems + AdrProblem(file.path, failure.line, "unsupported front matter: ${failure.reason}")
            }
        problems += Metadata(file, front, known).validate()
        problems += structure(file, front)
        return problems
    }

    /** Reports every file of a group of ADRs that share one number, naming all of them. */
    private fun duplicates(files: List<AdrFile>): List<AdrProblem> =
        files
            .filter { it.number != null }
            .groupBy { it.number }
            .filterValues { it.size > 1 }
            .flatMap { (number, group) ->
                group.map { file ->
                    val others = group.filter { it !== file }.joinToString { it.path }
                    AdrProblem(file.path, 0, "duplicate number $number, also in $others")
                }
            }

    /** Reports `0000`, and every number below the highest that has no file. */
    private fun gaps(known: Set<String>): List<AdrProblem> {
        val numbers = known.map { it.toInt() }
        val zero =
            if (0 in numbers) listOf(AdrProblem(DIRECTORY, 0, "ADR numbers start at 0001, not 0000")) else emptyList()
        val highest = numbers.maxOrNull() ?: 0
        val missing =
            (1..highest)
                .filter { it !in numbers }
                .map { number ->
                    val name = "%04d".format(number)
                    AdrProblem(DIRECTORY, 0, "ADR-$name is missing; numbering runs from 0001 without gaps")
                }
        return zero + missing
    }

    private fun structure(file: AdrFile, front: FrontMatter.Result): List<AdrProblem> {
        val problems = mutableListOf<AdrProblem>()
        val headings = MarkdownOutline.headings(front)
        val title = headings.firstOrNull { it.level == 1 }?.let { TITLE.matchEntire(it.text) }
        if (title == null) {
            problems += AdrProblem(file.path, front.bodyStartLine, "missing the title heading `# ADR-NNNN: <title>`")
        } else if (file.number != null && title.groupValues[1] != file.number) {
            problems +=
                AdrProblem(
                    file.path,
                    front.bodyStartLine,
                    "the title heading names ADR-${title.groupValues[1]}, but the file name says ${file.number}",
                )
        }
        SECTIONS.forEach { name ->
            val section = headings.firstOrNull { it.level == 2 && it.text == "## $name" }
            when {
                section == null ->
                    problems += AdrProblem(file.path, front.bodyStartLine, "missing the section `## $name`")
                section.content == 0 ->
                    problems += AdrProblem(file.path, section.line, "the section `## $name` is empty")
            }
        }
        return problems
    }

    /** Validates the front matter keys of one ADR and collects the problems it finds. */
    private class Metadata(
        private val file: AdrFile,
        private val front: FrontMatter.Result,
        private val known: Set<String>,
    ) {
        private val problems = mutableListOf<AdrProblem>()

        fun validate(): List<AdrProblem> {
            front.entries
                .filterKeys { it !in REQUIRED_KEYS && it !in OPTIONAL_KEYS }
                .forEach { (key, entry) -> problem(entry.line, "unknown front matter key `$key`") }
            status()
            date()
            people("decision-makers", required = true)
            OPTIONAL_KEYS.forEach { people(it, required = false) }
            return problems
        }

        private fun problem(line: Int, message: String) {
            problems += AdrProblem(file.path, line, message)
        }

        private fun entry(key: String, required: Boolean): FrontMatter.Entry? =
            front.entries[key].also { if (it == null && required) problem(0, "missing front matter key `$key`") }

        /** The string value of [key], or null when it is missing, not a string or blank; the last two are reported. */
        private fun text(key: String): FrontMatter.Entry? {
            val entry = entry(key, required = true) ?: return null
            val value = (entry.value as? FrontMatter.Value.Text)?.value
            return when {
                value == null -> null.also { problem(entry.line, "`$key` must be a string") }
                value.isBlank() -> null.also { problem(entry.line, "`$key` is empty") }
                else -> entry
            }
        }

        private fun status() {
            val entry = text("status") ?: return
            val value = (entry.value as FrontMatter.Value.Text).value
            if (value in STATUSES) return
            when (val target = SUPERSEDED.matchEntire(value)?.groupValues?.get(1)) {
                null ->
                    problem(
                        entry.line,
                        "`status` must be one of ${STATUSES.joinToString()}, or `${STATUS_SUPERSEDED_PREFIX}NNNN`, " +
                            "found `$value`",
                    )
                file.number -> problem(entry.line, "`status` makes ADR-$target supersede itself")
                !in known -> problem(entry.line, "`status` names ADR-$target, but no docs/adr/$target-<slug>.md exists")
            }
        }

        private fun date() {
            val entry = text("date") ?: return
            val value = (entry.value as FrontMatter.Value.Text).value
            if (!isDate(value)) problem(entry.line, "`date` must be a valid YYYY-MM-DD date, found `$value`")
        }

        private fun isDate(value: String): Boolean =
            DATE.matches(value) &&
                try {
                    LocalDate.parse(value)
                    true
                } catch (_: DateTimeException) {
                    false
                }

        /** A string or a list of strings; [required] keys must also be present and hold at least one person. */
        private fun people(key: String, required: Boolean) {
            val entry = entry(key, required) ?: return
            val names =
                when (val value = entry.value) {
                    is FrontMatter.Value.Text -> listOf(value.value)
                    is FrontMatter.Value.Items -> value.values
                    else -> null.also { problem(entry.line, "`$key` must be a string or a list of strings") }
                } ?: return
            when {
                names.any { it.isBlank() } -> problem(entry.line, "`$key` has an empty item")
                required && names.isEmpty() -> problem(entry.line, "`$key` is empty")
            }
        }
    }
}
