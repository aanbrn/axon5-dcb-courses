// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

/**
 * The entries of `governance/governed-paths.txt` and the lines that make the file unusable.
 *
 * The grammar has one meaning for every consumer. It mirrors `governed_globs` in
 * `scripts/governance/governed-paths.sh`, which the pull request gate and the Claude Code hooks share, and both must
 * change together:
 * 1. The text is split on LF only, so a lone carriage return never separates two entries.
 * 2. Leading and trailing space, tab and carriage return are stripped, and nothing else.
 * 3. Empty lines and lines starting with `#` are ignored.
 * 4. Every other entry must be printable ASCII (0x20 to 0x7E). Otherwise the line is reported in [problems] and the
 *    file is unusable, because bash and Kotlin could only disagree about such an entry.
 * 5. A file without entries is the caller's problem to report: it governs nothing.
 *
 * @property entries the stripped entries in file order; an entry may repeat.
 * @property problems one message per offending line in ascending line order, empty for a valid file.
 */
internal data class GovernedPaths(val entries: List<String>, val problems: List<String>) {
    /** Whether an entry matches [path], a path relative to the repository root, as [matches] defines. */
    fun governs(path: String): Boolean = entries.any { matches(path, it) }

    companion object {
        private const val STRIPPED = " \t\r"
        private const val LINE_SEPARATOR = '\n'
        private const val COMMENT_MARKER = '#'
        private const val ANY_CHARACTERS = "**"
        private val PRINTABLE_ASCII = ' '..'~'

        /** Parses the [text] of a governed paths file. */
        fun parse(text: String): GovernedPaths {
            val entries = mutableListOf<String>()
            val problems = mutableListOf<String>()
            text.split(LINE_SEPARATOR).forEachIndexed { index, line ->
                val entry = line.trim { it in STRIPPED }
                when {
                    entry.isEmpty() || entry.startsWith(COMMENT_MARKER) -> Unit
                    entry.all { it in PRINTABLE_ASCII } -> entries += entry
                    else -> problems += "line ${index + 1}: entry contains a character outside printable ASCII"
                }
            }
            return GovernedPaths(entries, problems)
        }

        /**
         * Whether [path] matches [glob]: `**` matches any characters including `/`, and every other character is
         * literal, so a plain name matches exactly. It mirrors `glob_match` in `scripts/governance/governed-paths.sh`,
         * and both must change together.
         */
        fun matches(path: String, glob: String): Boolean {
            val parts = glob.split(ANY_CHARACTERS)
            if (parts.size == 1) {
                return path == glob
            }
            // The first part anchors at the start and the last part at the end. Each part in between must follow the
            // one before it and is matched at its first occurrence, as the shell's shortest-prefix removal does.
            val start = if (path.startsWith(parts.first())) path.removePrefix(parts.first()) else null
            val rest =
                parts.subList(1, parts.lastIndex).fold(start) { remaining, part ->
                    remaining?.indexOf(part)?.takeIf { it >= 0 }?.let { remaining.substring(it + part.length) }
                }
            return rest?.endsWith(parts.last()) == true
        }
    }
}
