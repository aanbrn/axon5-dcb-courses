// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

/**
 * A view of Kotlin source text in which comments and the contents of literals cannot be mistaken for code.
 *
 * A rule that searches source text for a call shape must not see a shape that only appears in documentation, in a
 * string, or in test data. Removing comments with a regular expression is not enough, because the text of a literal
 * decides what is a comment: a URL string holds two slashes, which start a line comment that hides the rest of its
 * line, and a glob string holds a slash followed by a star, which starts a block comment that hides everything up to
 * the next block comment terminator anywhere in the file. [of] therefore tracks the lexical state, the way the Kotlin
 * compiler does:
 * - a line comment runs to the end of the line, and a block comment to its matching terminator; block comments nest;
 * - a string ends at the next unescaped quote; a raw string (three quotes) has no escapes and ends at the last three
 *   quotes of a run of quotes;
 * - a string or raw string may carry a run of dollar signs before its opening quote, which sets how many dollar signs
 *   open a template (with two, a template needs two dollar signs before its brace); only a template body is code, and
 *   it may contain further strings and comments;
 * - a character literal ends at its closing apostrophe, with backslash escapes, and never spans a line;
 * - a backtick identifier is code and is copied unchanged to its closing backtick on the same line, because a test name
 *   such as `` `Axon's own resolver` `` holds an apostrophe that must not open a character literal.
 */
object KotlinCodeView {
    private const val RAW_DELIMITER_LENGTH = 3

    /**
     * Masks the comments and literal contents of [source].
     *
     * Every character of a comment and of the content of a string, raw string or character literal becomes a space,
     * except line breaks. The delimiters of a literal, the code of a template and all other code stay verbatim. The
     * result therefore has the length of [source], and every offset and line number of the result matches the source. A
     * comment or literal that is not terminated is masked to the end of the text.
     *
     * @param source the Kotlin source text
     * @return the masked copy of [source]
     */
    fun of(source: String): String = Scanner(Masked(source)).scan()

    private class Scanner(private val text: Masked) {
        private val source = text.source
        private var position by text::position

        fun scan(): String {
            scanCode(inTemplate = false)
            return text.toString()
        }

        /** Scans code; in a template body, returns after the brace that closes the template. */
        private fun scanCode(inTemplate: Boolean) {
            var depth = 0
            while (position < source.length && !(inTemplate && depth < 0)) {
                when {
                    source.startsWith("//", position) -> maskLineComment()
                    source.startsWith("/*", position) -> maskBlockComment()
                    source[position] == '`' -> skipBacktickIdentifier()
                    source[position] == '\'' -> maskCharLiteral()
                    source[position] == '"' || source[position] == '$' -> scanQuoteOrDollar()
                    source[position] == '{' -> {
                        depth++
                        position++
                    }
                    source[position] == '}' -> {
                        depth--
                        position++
                    }
                    else -> position++
                }
            }
        }

        private fun maskLineComment() {
            while (position < source.length && source[position] != '\n') text.mask(position++)
        }

        private fun maskBlockComment() {
            var depth = 0
            do {
                when {
                    source.startsWith("/*", position) -> {
                        depth++
                        text.maskRange(position, 2)
                    }
                    source.startsWith("*/", position) -> {
                        depth--
                        text.maskRange(position, 2)
                    }
                    else -> text.maskRange(position, 1)
                }
            } while (depth > 0 && position < source.length)
        }

        private fun skipBacktickIdentifier() {
            position++
            while (position < source.length && source[position] != '\n') {
                if (source[position++] == '`') return
            }
        }

        private fun maskCharLiteral() {
            position++
            while (position < source.length && source[position] != '\n' && source[position] != '\'') {
                if (source[position] == '\\') text.maskRange(position, 1)
                text.maskRange(position, 1)
            }
            if (position < source.length && source[position] == '\'') position++
        }

        /** At a quote or at a run of `$`: a run of `$` that is not followed by a quote is code and is skipped. */
        private fun scanQuoteOrDollar() {
            val dollars = text.runLength('$')
            val quote = position + dollars
            position = quote
            if (quote < source.length && source[quote] == '"') scanString(maxOf(dollars, 1))
        }

        private fun scanString(prefix: Int) {
            val raw = source.startsWith("\"\"\"", position)
            position += if (raw) RAW_DELIMITER_LENGTH else 1
            var closed = false
            while (position < source.length && !closed) {
                when {
                    source[position] == '\\' && !raw -> text.maskRange(position, 2)
                    source[position] == '"' -> closed = closeQuotes(raw)
                    source[position] == '$' -> scanDollars(prefix)
                    else -> text.maskRange(position, 1)
                }
            }
        }

        /** Returns whether the quotes at the position end the literal. */
        private fun closeQuotes(raw: Boolean): Boolean {
            if (!raw) {
                position++
                return true
            }
            val quotes = text.runLength('"')
            val closes = quotes >= RAW_DELIMITER_LENGTH
            val content = if (closes) quotes - RAW_DELIMITER_LENGTH else quotes
            text.maskRange(position, content)
            position += quotes - content
            return closes
        }

        /** At a run of `$` in a literal: a template when [prefix] or more of them precede a brace, else content. */
        private fun scanDollars(prefix: Int) {
            val dollars = text.runLength('$')
            val brace = position + dollars
            val opensTemplate = dollars >= prefix && brace < source.length && source[brace] == '{'
            if (opensTemplate) {
                text.maskRange(position, dollars - prefix)
                position = brace + 1
                scanCode(inTemplate = true)
            } else {
                text.maskRange(position, dollars)
            }
        }
    }

    /** The source text, a masked copy of it and the position of the scan. */
    private class Masked(val source: String) {
        private val view = source.toCharArray()
        var position = 0

        /** The number of consecutive [char] at the position. */
        fun runLength(char: Char): Int {
            var end = position
            while (end < source.length && source[end] == char) end++
            return end - position
        }

        /** Masks up to [count] characters from [start] and moves the position behind them. */
        fun maskRange(start: Int, count: Int) {
            val end = minOf(start + count, source.length)
            for (index in start until end) mask(index)
            position = maxOf(position, end)
        }

        fun mask(index: Int) {
            if (source[index] != '\n' && source[index] != '\r') view[index] = ' '
        }

        override fun toString(): String = String(view)
    }
}
