// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

/**
 * Finds the headings of a Markdown body, the one scanner that the ADR and the upstream record checks share.
 *
 * Only level-1 (`# `) and level-2 (`## `) headings count, and only outside code fences, so an example block may show a
 * heading without it being mistaken for a section. A fence opens on a line that starts, after its indentation, with at
 * least three backticks or three tildes; its fence string is the whole run of that character. It closes only on a line
 * that starts with the same fence string, so a tilde line inside a backtick fence, or the other way round, is content.
 */
internal object MarkdownOutline {
    private const val FENCE_LENGTH = 3

    /**
     * A level-1 or level-2 heading with its [text], its 1-based file [line] and its count of non-blank [content] lines.
     */
    class Heading(val level: Int, val text: String, val line: Int, val content: Int)

    /**
     * The level-1 and level-2 headings of the body of [front] outside code fences, in file order.
     *
     * The text of a heading is its line without trailing blanks. Its content is the number of non-blank lines below it
     * up to the next heading, counting fence lines and fenced lines.
     *
     * @param front the parsed file, whose body lines are numbered from its `bodyStartLine`
     * @return the headings found
     */
    fun headings(front: FrontMatter.Result): List<Heading> {
        val found = mutableListOf<OpenHeading>()
        var fence: String? = null
        front.body.forEachIndexed { index, raw ->
            val line = front.bodyStartLine + index
            val text = raw.trimEnd()
            when (val open = fence) {
                null -> {
                    fence = fenceOpenedBy(text)
                    if (fence == null) headingOf(text, line)?.let(found::add)
                }
                else -> if (text.trimStart().startsWith(open)) fence = null
            }
            if (text.isNotBlank() && found.isNotEmpty() && found.last().line != line) found.last().content++
        }
        return found.map { Heading(it.level, it.text, it.line, it.content) }
    }

    private fun fenceOpenedBy(text: String): String? {
        val trimmed = text.trimStart()
        val marker = trimmed.firstOrNull()?.takeIf { it == '`' || it == '~' } ?: return null
        return trimmed.takeWhile { it == marker }.takeIf { it.length >= FENCE_LENGTH }
    }

    private fun headingOf(text: String, line: Int): OpenHeading? =
        when {
            text.startsWith("# ") -> OpenHeading(1, text, line)
            text.startsWith("## ") -> OpenHeading(2, text, line)
            else -> null
        }

    /** A heading whose content is still being counted. */
    private class OpenHeading(val level: Int, val text: String, val line: Int) {
        var content = 0
    }
}
