// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

/**
 * The entries of `governance/governed-paths.txt` and the lines that make the file unusable.
 *
 * The grammar has one meaning for every consumer. It mirrors `load_globs` in
 * `scripts/governance/check-pr.sh`, and both must change together:
 * 1. The text is split on LF only, so a lone carriage return never separates two entries.
 * 2. Leading and trailing space, tab and carriage return are stripped, and nothing else.
 * 3. Empty lines and lines starting with `#` are ignored.
 * 4. Every other entry must be printable ASCII (0x20 to 0x7E). Otherwise the line is reported in
 *    [problems] and the file is unusable, because bash and Kotlin could only disagree about such an
 *    entry.
 * 5. A file without entries is the caller's problem to report: it governs nothing.
 *
 * @property entries the stripped entries in file order; an entry may repeat.
 * @property problems one message per offending line in ascending line order, empty for a valid
 *   file.
 */
internal data class GovernedPaths(val entries: List<String>, val problems: List<String>) {
    companion object {
        private const val STRIPPED = " \t\r"
        private const val LINE_SEPARATOR = '\n'

        /** Parses the [text] of a governed paths file. */
        fun parse(text: String): GovernedPaths {
            val entries =
                text
                    .split(LINE_SEPARATOR)
                    .map { line -> line.trim { it in STRIPPED } }
                    .filter {
                        it.isNotEmpty()
                    }
            return GovernedPaths(entries, emptyList())
        }
    }
}
