// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when `mise.toml` and the tool inventory `docs/tooling.md` list different tools.
 *
 * Every key of the `[tools]` table in `mise.toml` must have a row in the inventory whose `Version
 * source` cell is `mise` and whose `Tool` cell carries the exact key in backticks, and every such
 * row must have a key. Keys are compared verbatim, including their backend prefix, so `pipx:reuse`
 * and `reuse` are different tools. The order of keys and rows does not matter. Rows with another
 * version source, such as `.nvmrc` or a not yet pinned tool, are not checked.
 *
 * The files are read line by line instead of with a TOML library: only the keys of one table are
 * needed, and an extra parser dependency in `build-logic` is not worth that.
 */
@CacheableTask
abstract class ToolingTableCheck : DefaultTask() {

    /** The pinned tools, `mise.toml`. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val miseToml: RegularFileProperty

    /** The tool inventory, `docs/tooling.md`. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inventory: RegularFileProperty

    /** The file the sorted list of differences is written to; empty when the files agree. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Compares the tool keys of [miseToml] with the mise-backed rows of [inventory]. */
    @TaskAction
    fun check() {
        val pinned = miseKeys(miseToml.get().asFile.readLines())
        val rows = inventoryRows(inventory.get().asFile.readLines())
        val documented = rows.mapNotNull { it.key }.toSet()
        val problems =
            (pinned - documented).sorted().map { "missing in $INVENTORY_PATH: $it" } +
                (documented - pinned).sorted().map { "missing in $MISE_PATH: $it" } +
                rows
                    .filter { it.key == null }
                    .map { "no backticked mise.toml key in row: ${it.tool}" }
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                problems.joinToString(
                    separator = "\n",
                    postfix = if (problems.isEmpty()) "" else "\n",
                ),
            )
        }
        if (problems.isNotEmpty()) {
            throw GradleException(failureMessage(problems))
        }
    }

    private fun failureMessage(problems: List<String>): String = buildString {
        appendLine("$MISE_PATH and $INVENTORY_PATH list different mise tools:")
        problems.forEach { appendLine("  $it") }
        appendLine()
        append(
            "Every [tools] key needs a row with Version source `mise` and the exact key in " +
                "backticks in the Tool cell, and the other way round (D-17).",
        )
    }

    /** The keys of the `[tools]` table, unquoted; comments, values and other tables are skipped. */
    private fun miseKeys(lines: List<String>): Set<String> {
        val keys = mutableSetOf<String>()
        var inTools = false
        for (line in lines.map { it.trim() }) {
            when {
                line.startsWith("[") -> inTools = TOOLS_HEADER.matches(line)
                inTools ->
                    KEY.find(line)?.let { keys += it.groupValues.drop(1).first(String::isNotEmpty) }
            }
        }
        return keys
    }

    /** The rows of any Markdown table whose `Version source` cell is `mise`. */
    private fun inventoryRows(lines: List<String>): List<InventoryRow> =
        lines
            .map { it.trim() }
            .filter { it.startsWith("|") }
            .map { row -> row.trim('|').split('|').map { it.trim() } }
            .filter { cells ->
                cells.size > SOURCE_COLUMN && cells[SOURCE_COLUMN].trim('`') == MISE
            }
            .map { cells ->
                InventoryRow(cells.first(), BACKTICKED.find(cells.first())?.groupValues?.get(1))
            }

    private data class InventoryRow(val tool: String, val key: String?)

    private companion object {
        const val MISE_PATH = "mise.toml"
        const val INVENTORY_PATH = "docs/tooling.md"
        const val MISE = "mise"
        const val SOURCE_COLUMN = 2

        val TOOLS_HEADER = Regex("""\[tools]\s*(#.*)?""")

        /** A bare, double-quoted or single-quoted key at the start of a line, before the `=`. */
        val KEY = Regex("""^(?:"([^"]+)"|'([^']+)'|([A-Za-z0-9_-]+))\s*=""")
        val BACKTICKED = Regex("`([^`]+)`")
    }
}
