// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import java.io.File
import java.time.DateTimeException
import java.time.LocalDate
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when the upstream records under `docs/upstream/` or the workaround markers in the sources disagree
 * (UPS-01, UPS-02).
 *
 * **Records.** Every `docs/upstream/NNNN-<slug>.md` has the front matter keys `id` (`UP-NNNN`, the file's number),
 * `product`, `product-version`, `status`, `workaround`, `upstream`, `reported` and `updated`, a `# UP-NNNN: <title>`
 * heading and the sections Symptom, Minimal reproduction, Root cause, Workaround, Removal criteria and How to verify
 * removal. The status is one of `open`, `reported`, `fixed-upstream`, `workaround-removed` and `wont-fix`; a `reported`
 * record needs a non-empty `upstream` list and a `reported` date; two records never share an `id`.
 *
 * **Markers.** A comment `UPSTREAM: UP-NNNN` next to a workaround must name an existing record, and a record whose
 * `workaround` is `true` has at least one marker unless its status is `workaround-removed`; a record with that status,
 * and a record with `workaround: false`, has none. A `wont-fix` record keeps its markers, because the workaround
 * becomes permanent (D-45). `UP-00010` and `UP-0001x` are malformed markers, not markers of another record.
 *
 * Problems are reported as `path:line: message`, sorted by path and line, and the task writes [index] from the valid
 * records, sorted by `id`, as a Markdown table a documentation site can include unchanged.
 */
@CacheableTask
abstract class UpstreamRecordCheck : DefaultTask() {

    /** The record files, the Markdown files directly in `docs/upstream/`, as a file tree rooted at the repository. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val records: ConfigurableFileCollection

    /** The text files scanned for markers, as a file tree rooted at the repository root. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val sources: ConfigurableFileCollection

    /**
     * Temporary bridge: marker IDs tolerated although no record exists yet, because the markers landed in the code
     * before the records did. An entry that has a record, or has no marker any more, is itself a problem, so the list
     * empties itself and never hides a marker for good. Empty by default.
     */
    @get:Input abstract val unrecordedMarkers: SetProperty<String>

    /** The file the sorted problems are written to; empty when records and markers agree. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** The generated index of the valid records, a Markdown page with one table row per record. */
    @get:OutputFile abstract val index: RegularFileProperty

    /** Validates every record, scans the sources for markers, writes [report] and [index], and fails on problems. */
    @TaskAction
    fun check() {
        val validated = recordFiles().map { UpstreamRecords.validate(it) }
        val markers = scanMarkers()
        val unrecorded = unrecordedMarkers.get()
        val problems = buildList {
            validated.flatMapTo(this) { it.problems }
            addAll(UpstreamRecords.duplicateIds(validated))
            addAll(markers.problems)
            addAll(UpstreamRecords.consistency(validated, markers.found, unrecorded))
        }
            .sortedWith(compareBy({ it.path }, { it.line }, { it.message }))
        writeText(index.get().asFile, UpstreamRecords.renderIndex(validated.mapNotNull { it.row }))
        writeText(report.get().asFile, problems.joinToString(separator = "") { it.render() + "\n" })
        warnAboutTolerated(markers.found, unrecorded)
        if (problems.isNotEmpty()) {
            throw GradleException(failureMessage(problems))
        }
    }

    private fun recordFiles(): List<RecordFile> {
        val files = mutableListOf<RecordFile>()
        records.asFileTree.visit {
            if (!isDirectory) files += RecordFile(relativePath.pathString, file.readLines())
        }
        return files.sortedBy { it.path }
    }

    private fun scanMarkers(): Markers {
        val found = mutableListOf<Marker>()
        val problems = mutableListOf<Problem>()
        sources.asFileTree.visit {
            if (!isDirectory) UpstreamRecords.scan(relativePath.pathString, file.readLines(), found, problems)
        }
        return Markers(found, problems)
    }

    private fun warnAboutTolerated(markers: List<Marker>, unrecorded: Set<String>) {
        val tolerated = markers.filter { it.id in unrecorded }.map { it.id }.toSortedSet()
        if (tolerated.isNotEmpty()) {
            logger.warn("Tolerating markers without a record (unrecordedMarkers): {}", tolerated.joinToString())
        }
    }

    private fun writeText(file: File, text: String) {
        file.parentFile.mkdirs()
        file.writeText(text)
    }

    private fun failureMessage(problems: List<Problem>): String = buildString {
        appendLine("${problems.size} upstream record problem(s), also written to ${report.get().asFile.path}:")
        problems.forEach { appendLine("  ${it.render()}") }
        appendLine()
        append(
            "Every workaround carries an UPSTREAM marker naming a record in docs/upstream/, and a record " +
                "whose workaround is gone has no marker left (UPS-01, UPS-02). Start from " +
                "docs/templates/upstream-record.md.",
        )
    }
}

/** One record file: its repository-relative [path] and [lines]. */
private class RecordFile(val path: String, val lines: List<String>)

/** One marker comment naming [id] on [line] of the file at [path]. */
private class Marker(val path: String, val line: Int, val id: String)

/** The markers found in the sources and the malformed ones reported as [problems]. */
private class Markers(val found: List<Marker>, val problems: List<Problem>)

/** One violation: [message] about [path], on [line] when it is positive. */
private class Problem(val path: String, val line: Int, val message: String) {
    fun render(): String = if (line > 0) "$path:$line: $message" else "$path: $message"
}

/** The index row of a record without problems. */
private class IndexRow(
    val id: String,
    val title: String,
    val product: String,
    val status: String,
    val workaround: Boolean,
    val upstream: List<String>,
    val updated: String?,
)

/** What validating the front matter and the body of one record found. */
private class Checked(
    val problems: List<Problem>,
    val id: String?,
    val status: String?,
    val workaround: Boolean?,
    val row: IndexRow?,
)

/**
 * The result of validating one record file.
 *
 * @property path the repository-relative path of the record file.
 * @property problems what is wrong with the record, empty when it is valid.
 * @property fileId the `UP-NNNN` derived from the file name, or null when the name is malformed.
 * @property id the `id` front matter value when it is a valid identifier.
 * @property status the `status` when it is one of the allowed values.
 * @property workaround the `workaround` flag when it is a boolean.
 * @property row the index row, present only when the record has no problems.
 */
private class ValidatedRecord(
    val path: String,
    val problems: List<Problem>,
    val fileId: String?,
    val id: String?,
    val status: String?,
    val workaround: Boolean?,
    val row: IndexRow?,
)

/** The record and marker rules, kept apart from the task so they read as one specification. */
private object UpstreamRecords {
    private const val MARKER = "UPSTREAM:"
    private const val STATUS_REPORTED = "reported"
    private const val STATUS_WORKAROUND_REMOVED = "workaround-removed"
    private const val UNRECORDED_PROPERTY = "unrecordedMarkers"
    private const val NO_VALUE = "-"
    private val STATUSES = listOf("open", STATUS_REPORTED, "fixed-upstream", STATUS_WORKAROUND_REMOVED, "wont-fix")
    private val KEYS =
        listOf("id", "product", "product-version", "status", "workaround", "upstream", "reported", "updated")
    private val FILE_NAME = Regex("^([0-9]{4})-[a-z0-9-]+\\.md$")
    private val ID = Regex("^UP-[0-9]{4}$")
    private val DATE = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    /** A marker is the marker word directly after a comment opener; anything else is prose or data, never a marker. */
    private val CONTEXT = Regex("(?:^|\\s)(?://|/\\*|\\*|#|<!--|--)\\s*" + Regex.escape(MARKER) + "(.*)")
    private val VALID_MARKER = Regex("^ (UP-[0-9]{4})\\b")

    fun validate(file: RecordFile): ValidatedRecord {
        val number = FILE_NAME.matchEntire(file.path.substringAfterLast('/'))?.groupValues?.get(1)
        val fileId = number?.let { "UP-$it" }
        val problems = mutableListOf<Problem>()
        if (number == null) {
            problems += Problem(file.path, 0, "the file name must match NNNN-<slug>.md: four digits, a lowercase slug")
        }
        val front =
            try {
                FrontMatter.parse(file.path, file.lines)
            } catch (failure: FrontMatter.Failure) {
                problems += Problem(file.path, failure.line, "unsupported front matter: ${failure.reason}")
                return ValidatedRecord(file.path, problems, fileId, null, null, null, null)
            }
        val checked = RecordValidator(file.path, front, number).validate()
        problems += checked.problems
        return ValidatedRecord(
            file.path,
            problems,
            fileId,
            checked.id,
            checked.status,
            checked.workaround,
            checked.row.takeIf { problems.isEmpty() },
        )
    }

    /** Reports every file of a group of records that share one `id`, naming all of them. */
    fun duplicateIds(records: List<ValidatedRecord>): List<Problem> =
        records
            .filter { it.id != null }
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .flatMap { (id, group) ->
                group.map { record ->
                    val others = group.filter { it !== record }.joinToString { it.path }
                    Problem(record.path, 0, "duplicate id $id, also in $others")
                }
            }

    /** The two-way consistency of markers and records, and the hygiene of the temporary [unrecorded] list. */
    fun consistency(records: List<ValidatedRecord>, markers: List<Marker>, unrecorded: Set<String>): List<Problem> {
        val recordIds = records.mapNotNull { it.fileId }.toSet()
        val markersById = markers.groupBy { it.id }
        return buildList {
            markers
                .filter { it.id !in recordIds && it.id !in unrecorded }
                .mapTo(this) { Problem(it.path, it.line, "$MARKER ${it.id} names no record in docs/upstream/") }
            records.forEach { addAll(markerRule(it, markersById[it.fileId].orEmpty())) }
            unrecorded.sorted().forEach { addAll(unrecordedRule(it, records, markersById)) }
        }
    }

    private fun markerRule(record: ValidatedRecord, own: List<Marker>): List<Problem> {
        val status = record.status
        val workaround = record.workaround
        return when {
            record.fileId == null || status == null || workaround == null -> emptyList()
            workaround && status != STATUS_WORKAROUND_REMOVED -> missingMarker(record, status, own)
            else -> {
                val reason = if (workaround) "status is $STATUS_WORKAROUND_REMOVED" else "workaround is false"
                own.map {
                    Problem(it.path, it.line, "$MARKER ${it.id} remains, but ${record.path} says $reason; remove it")
                }
            }
        }
    }

    private fun missingMarker(record: ValidatedRecord, status: String, own: List<Marker>): List<Problem> =
        if (own.isEmpty()) {
            listOf(
                Problem(
                    record.path,
                    0,
                    "workaround is true and status is $status, but no $MARKER ${record.fileId} marker exists; " +
                        "add it next to the workaround",
                ),
            )
        } else {
            emptyList()
        }

    private fun unrecordedRule(
        id: String,
        records: List<ValidatedRecord>,
        markersById: Map<String, List<Marker>>,
    ): List<Problem> {
        val record = records.firstOrNull { it.fileId == id }
        return when {
            !ID.matches(id) -> listOf(Problem(UNRECORDED_PROPERTY, 0, "`$id` is not an UP-NNNN identifier"))
            record != null ->
                listOf(Problem(UNRECORDED_PROPERTY, 0, "$id has a record (${record.path}); remove it from the list"))
            markersById[id].isNullOrEmpty() ->
                listOf(Problem(UNRECORDED_PROPERTY, 0, "no marker names $id any more; remove it from the list"))
            else -> emptyList()
        }
    }

    /** Adds the valid and reports the malformed markers of one source file. */
    fun scan(path: String, lines: List<String>, found: MutableList<Marker>, problems: MutableList<Problem>) {
        lines.forEachIndexed { index, text ->
            val rest = if (MARKER in text) CONTEXT.find(text)?.groupValues?.get(1) else null
            if (rest != null) {
                val id = VALID_MARKER.find(rest)?.groupValues?.get(1)
                if (id != null) {
                    found += Marker(path, index + 1, id)
                } else {
                    val shown = "$MARKER${rest.trimEnd()}"
                    problems +=
                        Problem(path, index + 1, "malformed marker `$shown`; write $MARKER UP-NNNN, four digits")
                }
            }
        }
    }

    /** The site-ready page: a heading and one table row per record, sorted by `id`. */
    fun renderIndex(rows: List<IndexRow>): String = buildString {
        appendLine("# Upstream records")
        appendLine()
        appendLine("Generated by `checkUpstreamRecords` from `docs/upstream/`. Do not edit.")
        appendLine()
        appendLine("| ID | Title | Product | Status | Workaround | Upstream | Updated |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- |")
        rows
            .sortedBy { it.id }
            .forEach { row ->
                val links = row.upstream.joinToString(", ") { cell(it) }.ifEmpty { NO_VALUE }
                val workaround = if (row.workaround) "yes" else "no"
                val title = cell(row.title)
                val product = cell(row.product)
                appendLine(
                    "| ${row.id} | $title | $product | ${row.status} | $workaround | $links | ${row.updated ?: NO_VALUE} |",
                )
            }
    }

    private fun cell(text: String): String = text.replace("|", "\\|")

    /** Reads typed front matter values of one parsed record and collects the [problems] it finds. */
    private open class EntryReader(protected val path: String, protected val front: FrontMatter.Result) {
        val problems = mutableListOf<Problem>()

        protected fun problem(line: Int, message: String) {
            problems += Problem(path, line, message)
        }

        protected fun line(key: String): Int = front.entries[key]?.line ?: 0

        protected fun entry(key: String): FrontMatter.Entry? =
            front.entries[key].also { if (it == null) problem(0, "missing front matter key `$key`") }

        protected fun text(key: String): String? {
            val entry = entry(key) ?: return null
            val value = (entry.value as? FrontMatter.Value.Text)?.value
            return when {
                value == null -> null.also { problem(entry.line, "`$key` must be a string") }
                value.isBlank() -> null.also { problem(entry.line, "`$key` is empty") }
                else -> value
            }
        }

        /** The date of [key], or null for `null`, a missing key or an invalid value (the last two are reported). */
        protected fun date(key: String): String? {
            val entry = entry(key) ?: return null
            return when (val value = entry.value) {
                FrontMatter.Value.Null -> null
                is FrontMatter.Value.Text if isDate(value.value) -> value.value
                else -> null.also { problem(entry.line, "`$key` must be a YYYY-MM-DD date or null") }
            }
        }

        private fun isDate(value: String): Boolean =
            DATE.matches(value) &&
                try {
                    LocalDate.parse(value)
                    true
                } catch (_: DateTimeException) {
                    false
                }
    }

    /** Validates the front matter and the body of one parsed record. */
    private class RecordValidator(path: String, front: FrontMatter.Result, private val fileNumber: String?) :
        EntryReader(path, front) {
        private var id: String? = null
        private var product: String? = null
        private var status: String? = null
        private var workaround: Boolean? = null
        private var upstream: List<String>? = null
        private var updated: String? = null
        private var title: String? = null

        fun validate(): Checked {
            front.entries
                .filterKeys { it !in KEYS }
                .forEach { (key, entry) -> problem(entry.line, "unknown front matter key `$key`") }
            id = idValue()
            product = text("product")
            text("product-version")
            status = statusValue()
            workaround = workaroundValue()
            upstream = upstreamValue()
            date("reported")
            updated = date("updated")
            lifecycle()
            title = RecordBody.check(front, id) { line, message -> problem(line, message) }
            return Checked(problems, id, status, workaround, row())
        }

        private fun row(): IndexRow? =
            if (problems.isEmpty()) {
                IndexRow(
                    id = checkNotNull(id),
                    title = checkNotNull(title),
                    product = checkNotNull(product),
                    status = checkNotNull(status),
                    workaround = checkNotNull(workaround),
                    upstream = checkNotNull(upstream),
                    updated = updated,
                )
            } else {
                null
            }

        private fun idValue(): String? {
            val value = text("id")
            val valid = value?.takeIf { ID.matches(it) }
            when {
                value == null -> Unit
                valid == null -> problem(line("id"), "`id` must be UP-NNNN with four digits, found `$value`")
                fileNumber != null && value != "UP-$fileNumber" ->
                    problem(line("id"), "`id` $value does not match the file number $fileNumber")
            }
            return valid
        }

        private fun workaroundValue(): Boolean? {
            val entry = entry("workaround") ?: return null
            val value = (entry.value as? FrontMatter.Value.Flag)?.value
            if (value == null) problem(entry.line, "`workaround` must be true or false")
            return value
        }

        private fun upstreamValue(): List<String>? {
            val entry = entry("upstream") ?: return null
            val values = (entry.value as? FrontMatter.Value.Items)?.values
            when {
                values == null ->
                    problem(entry.line, "`upstream` must be a list such as [] or [\"https://example.org\"]")
                values.any { it.isBlank() } -> problem(entry.line, "`upstream` has an empty item")
            }
            return values
        }

        private fun statusValue(): String? {
            val value = text("status")
            val valid = value?.takeIf { it in STATUSES }
            if (value != null && valid == null) {
                problem(line("status"), "`status` must be one of ${STATUSES.joinToString()}, found `$value`")
            }
            return valid
        }

        /** A `reported` record needs the issue link and the date it was reported on. */
        private fun lifecycle() {
            if (status != STATUS_REPORTED) return
            if (upstream?.isEmpty() == true) {
                problem(line("upstream"), "status is $STATUS_REPORTED, so `upstream` needs at least one link")
            }
            if (front.entries["reported"]?.value == FrontMatter.Value.Null) {
                problem(line("reported"), "status is $STATUS_REPORTED, so `reported` needs the date it was reported")
            }
        }
    }
}

/** The title heading and the required sections of a record body. */
private object RecordBody {
    private val SECTIONS =
        listOf(
            "Symptom",
            "Minimal reproduction",
            "Root cause",
            "Workaround",
            "Removal criteria",
            "How to verify removal",
        )
    private val TITLE = Regex("^# (UP-[0-9]{4}): (\\S.*)$")

    /** Reports problems through [report] as (line, message) and returns the title, or null when there is none. */
    fun check(front: FrontMatter.Result, id: String?, report: (Int, String) -> Unit): String? {
        val headings = MarkdownOutline.headings(front)
        val title = headings.firstOrNull { it.level == 1 }?.let { TITLE.matchEntire(it.text) }
        if (title == null) {
            report(front.bodyStartLine, "missing the title heading `# UP-NNNN: <title>`")
        } else if (title.groupValues[1] != id) {
            report(front.bodyStartLine, "the title heading names ${title.groupValues[1]}, but `id` is $id")
        }
        SECTIONS.forEach { name ->
            val section = headings.firstOrNull { it.level == 2 && it.text == "## $name" }
            when {
                section == null -> report(front.bodyStartLine, "missing the section `## $name`")
                section.content == 0 -> report(section.line, "the section `## $name` is empty")
            }
        }
        return title?.groupValues?.get(2)
    }
}
