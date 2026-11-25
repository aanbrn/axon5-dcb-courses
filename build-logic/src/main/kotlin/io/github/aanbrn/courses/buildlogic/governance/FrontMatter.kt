// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

/**
 * Reads the front matter of a Markdown file as a deliberately small subset of YAML, and fails closed on everything
 * else.
 *
 * The block sits between a `---` line on the first line and the next `---` line. Inside it only these lines are read:
 * - `key: scalar`, where the key is letters, digits and hyphens starting with a letter, and the scalar is a bare
 *   string, a double-quoted string (escapes `\"` and `\\` only), `true`, `false` or `null`;
 * - `key: [a, b]`, a list of bare or double-quoted strings, also `[]`, on one line or continued on indented lines as
 *   Prettier writes it (a bare `key:` then an indented `[`, or an open `[` with the items on the next lines), with an
 *   optional trailing comma before `]`;
 * - blank lines and whole-line comments, and a trailing ` # comment` after a value.
 *
 * Nesting, multi-line scalars, block scalars, anchors, tags, single quotes, duplicate keys, comments inside a list and
 * a missing closing `---` fail with `<file>:<line>: unsupported front matter: <reason>`. A quoted `"true"` or `"null"`
 * stays a string, so a value never changes meaning by being quoted. No YAML library is used: the governance checks need
 * five value shapes, and an extra parser in `build-logic` would accept far more than the records and decisions are
 * allowed to contain.
 */
internal object FrontMatter {

    /** A parsed value. */
    sealed interface Value {
        /** A bare or double-quoted string, [value] without its quotes. */
        data class Text(val value: String) : Value

        /** The literals `true` and `false`. */
        data class Flag(val value: Boolean) : Value

        /** The literal `null`. */
        data object Null : Value

        /** A list of strings, on one line or continued on indented lines. */
        data class Items(val values: List<String>) : Value
    }

    /** A [value] and the 1-based [line] it was read from. */
    data class Entry(val value: Value, val line: Int)

    /**
     * The outcome of [parse]: the [entries] by key in file order, the [body] lines after the closing `---`, and the
     * 1-based file line the body starts on.
     */
    class Result(val entries: Map<String, Entry>, val body: List<String>, val bodyStartLine: Int)

    /**
     * The reason a file is not accepted: [reason] on 1-based [line] of [file], worded `<file>:<line>: unsupported front
     * matter: <reason>`.
     */
    class Failure(val file: String, val line: Int, val reason: String) :
        Exception("$file:$line: unsupported front matter: $reason")

    /**
     * Parses the front matter at the start of [lines], the content of [file] (used in messages only).
     *
     * @throws Failure on any construct outside the supported subset.
     */
    fun parse(file: String, lines: List<String>): Result {
        if (lines.firstOrNull()?.trimEnd() != DELIMITER) fail(file, 1, "the file must start with a $DELIMITER line")
        val closing = lines.drop(1).indexOfFirst { it.trimEnd() == DELIMITER } + 1
        if (closing == 0) fail(file, 1, "there is no closing $DELIMITER line")
        // The block without its closing delimiter, so that a list can never read past it.
        val block = lines.subList(0, closing)
        val entries = linkedMapOf<String, Entry>()
        var index = 1
        while (index < block.size) {
            if (block[index].isBlank() || block[index].trimStart().startsWith("#")) {
                index++
                continue
            }
            val line = index + 1
            val parsed = parseEntry(file, block, index)
            if (parsed.key in entries) {
                fail(file, line, "duplicate key `${parsed.key}` (first on line ${entries.getValue(parsed.key).line})")
            }
            entries[parsed.key] = Entry(parsed.value, line)
            index = parsed.next
        }
        return Result(entries, lines.drop(closing + 1), closing + 2)
    }

    /** The [key] and [value] of one entry, and the index of the [next] line to read. */
    private class Parsed(val key: String, val value: Value, val next: Int)

    private fun parseEntry(file: String, block: List<String>, index: Int): Parsed {
        val line = index + 1
        val raw = block[index]
        if (raw.first().isWhitespace()) {
            fail(file, line, "indented line; only the continuation of an open list may be indented")
        }
        val match = ENTRY.matchEntire(raw.trimEnd()) ?: fail(file, line, "expected `key: value`")
        val key = match.groupValues[1]
        val text = match.groupValues[VALUE_GROUP].trim()
        if (text.startsWith("#") || (text.isEmpty() && !listContinues(block, index + 1))) {
            fail(file, line, "`$key` has no value; write null")
        }
        if (text.isNotEmpty() && (!text.startsWith("[") || closesList(text))) {
            return Parsed(key, parseValue(file, line, text), index + 1)
        }
        val (joined, next) = joinList(file, block, index, text)
        return Parsed(key, parseValue(file, line, joined), next)
    }

    /** Whether the line at [index] is an indented line that starts a list, as after a bare `key:`. */
    private fun listContinues(block: List<String>, index: Int): Boolean {
        val candidate = block.getOrNull(index) ?: return false
        return candidate.isNotEmpty() && candidate.first().isWhitespace() && candidate.trim().startsWith("[")
    }

    /**
     * Joins the [first] text of the list on line [index] with the indented lines that follow it, up to the line that
     * closes it. Returns the text and the index of the line after the list.
     */
    private fun joinList(file: String, block: List<String>, index: Int, first: String): Pair<String, Int> {
        val parts = mutableListOf<String>().apply { if (first.isNotEmpty()) add(first) }
        var next = index + 1
        while (parts.isEmpty() || !closesList(parts.joinToString(" "))) {
            val continuation = block.getOrNull(next)
            if (continuation.isNullOrBlank() || !continuation.first().isWhitespace()) {
                fail(file, index + 1, "unterminated list")
            }
            parts += continuation.trim()
            next++
        }
        return parts.joinToString(" ") to next
    }

    /** Whether a `]` outside double-quoted strings occurs in [text]. */
    private fun closesList(text: String): Boolean {
        var quoted = false
        var escaped = false
        for (char in text) {
            when {
                escaped -> escaped = false
                quoted && char == '\\' -> escaped = true
                char == '"' -> quoted = !quoted
                !quoted && char == ']' -> return true
            }
        }
        return false
    }

    private fun parseValue(file: String, line: Int, text: String): Value =
        when {
            text.startsWith("\"") ->
                Cursor(file, line, text).let { Value.Text(it.quoted()).also { _ -> it.requireEnd() } }
            text.startsWith("[") -> Value.Items(Cursor(file, line, text).list())
            text.first() in UNSUPPORTED_STARTS -> fail(file, line, "`${text.first()}` starts an unsupported construct")
            else -> bare(file, line, text)
        }

    private fun bare(file: String, line: Int, text: String): Value {
        val value = text.replace(TRAILING_COMMENT, "")
        if (value.isEmpty()) fail(file, line, "the value is only a comment; write null")
        if (": " in value || value.endsWith(":")) fail(file, line, "`$value` contains a mapping indicator")
        return when (value) {
            "true" -> Value.Flag(true)
            "false" -> Value.Flag(false)
            "null" -> Value.Null
            else -> Value.Text(value)
        }
    }

    private fun fail(file: String, line: Int, reason: String): Nothing = throw Failure(file, line, reason)

    /** Reads quoted strings and lists out of one line of [text]. */
    private class Cursor(private val file: String, private val line: Int, private val text: String) {
        private var position = 0

        /** The text of the double-quoted string at the cursor, leaving the cursor after its closing quote. */
        fun quoted(): String {
            position++
            val value = StringBuilder()
            while (position < text.length) {
                when (val char = text[position++]) {
                    '"' -> return value.toString()
                    '\\' -> value.append(escaped())
                    else -> value.append(char)
                }
            }
            fail(file, line, "unterminated quoted string")
        }

        /** The items of the list at the cursor, which must be the whole value. */
        fun list(): List<String> {
            position++
            val items = mutableListOf<String>()
            skipSpaces()
            if (peek() == ']') {
                position++
            } else {
                do {
                    skipSpaces()
                    items += item()
                } while (nextItemFollows())
            }
            requireEnd()
            return items
        }

        /**
         * Reads the separator after an item: true after a `,` that another item follows, false after the `]` that ends
         * the list, also when a trailing comma precedes it.
         */
        private fun nextItemFollows(): Boolean {
            skipSpaces()
            val separator = peek() ?: fail(file, line, "unterminated list")
            if (separator == '#') fail(file, line, COMMENT_IN_LIST)
            position++
            if (separator != ',' && separator != ']') {
                fail(file, line, "expected `,` or `]` in the list but found `$separator`")
            }
            return separator == ',' && !closesAfterTrailingComma()
        }

        /** Consumes the `]` that follows a trailing comma, if it is next. */
        private fun closesAfterTrailingComma(): Boolean {
            skipSpaces()
            if (peek() != ']') return false
            position++
            return true
        }

        /** Fails unless only blanks or a trailing comment remain. */
        fun requireEnd() {
            val rest = text.substring(position)
            if (rest.isNotBlank() && !rest.trimStart().startsWith("#")) {
                fail(file, line, "unexpected text after the value: `${rest.trim()}`")
            }
        }

        private fun item(): String =
            when (peek()) {
                '"' -> quoted().also { skipSpaces() }
                null -> fail(file, line, "unterminated list")
                '#' -> fail(file, line, COMMENT_IN_LIST)
                else -> bareItem()
            }

        private fun bareItem(): String {
            val start = position
            while (position < text.length && text[position] !in LIST_STOPS) position++
            val value = text.substring(start, position).trim()
            if (value.isEmpty()) fail(file, line, "empty list item")
            if (INLINE_COMMENT.containsMatchIn(value)) fail(file, line, COMMENT_IN_LIST)
            if (text.getOrNull(position) == '[') fail(file, line, "nested lists are not supported")
            if (value.first() in UNSUPPORTED_STARTS || value.first() == '"') {
                fail(file, line, "`${value.first()}` starts an unsupported list item")
            }
            return value
        }

        private fun escaped(): Char {
            val char = text.getOrNull(position++) ?: fail(file, line, "unterminated quoted string")
            if (char != '"' && char != '\\') fail(file, line, "unsupported escape `\\$char`; only `\\\"` and `\\\\`")
            return char
        }

        private fun peek(): Char? = text.getOrNull(position)

        private fun skipSpaces() {
            while (peek() == ' ') position++
        }
    }

    private const val DELIMITER = "---"
    private const val VALUE_GROUP = 2
    private val ENTRY = Regex("""([A-Za-z][A-Za-z0-9-]*):(?:\s+(.*))?""")
    private val TRAILING_COMMENT = Regex("""\s+#.*$""")
    private const val UNSUPPORTED_STARTS = "&*!|>{'@%`"
    private const val LIST_STOPS = ",]["
    private const val COMMENT_IN_LIST = "comments inside a list are not supported"
    private val INLINE_COMMENT = Regex("""\s#""")
}
