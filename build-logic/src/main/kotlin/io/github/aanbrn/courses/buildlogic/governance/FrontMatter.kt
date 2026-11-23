// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

/**
 * Reads the front matter of a Markdown file as a deliberately small subset of YAML, and fails closed on everything
 * else.
 *
 * The block sits between a `---` line on the first line and the next `---` line. Inside it only these lines are read:
 * - `key: scalar`, where the key is letters, digits and hyphens starting with a letter, and the scalar is a bare
 *   string, a double-quoted string (escapes `\"` and `\\` only), `true`, `false` or `null`;
 * - `key: [a, b]`, a one-line list of bare or double-quoted strings, also `[]`;
 * - blank lines and whole-line comments, and a trailing ` # comment` after a value.
 *
 * Nesting, multi-line values, block scalars, anchors, tags, single quotes, duplicate keys and a missing closing `---`
 * fail with `<file>:<line>: unsupported front matter: <reason>`. A quoted `"true"` or `"null"` stays a string, so a
 * value never changes meaning by being quoted. No YAML library is used: the governance checks need five value shapes,
 * and an extra parser in `build-logic` would accept far more than the records and decisions are allowed to contain.
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

        /** A one-line list of strings. */
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
        val entries = linkedMapOf<String, Entry>()
        for (index in 1 until closing) {
            val raw = lines[index]
            if (raw.isBlank() || raw.trimStart().startsWith("#")) continue
            val line = index + 1
            val (key, value) = parseEntry(file, line, raw)
            if (key in entries) fail(file, line, "duplicate key `$key` (first on line ${entries.getValue(key).line})")
            entries[key] = Entry(value, line)
        }
        return Result(entries, lines.drop(closing + 1), closing + 2)
    }

    private fun parseEntry(file: String, line: Int, raw: String): Pair<String, Value> {
        if (raw.first().isWhitespace()) {
            fail(file, line, "indented line; nested structures and multi-line values are not supported")
        }
        val match = ENTRY.matchEntire(raw.trimEnd()) ?: fail(file, line, "expected `key: value`")
        val text = match.groupValues[VALUE_GROUP].trim()
        if (text.isEmpty() || text.startsWith("#"))
            fail(file, line, "`${match.groupValues[1]}` has no value; write null")
        return match.groupValues[1] to parseValue(file, line, text)
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
                val char = text[position++]
                when {
                    char == '"' -> return value.toString()
                    char == '\\' -> value.append(escaped())
                    else -> value.append(char)
                }
            }
            return fail(file, line, "unterminated quoted string")
        }

        /** The items of the list at the cursor, which must be the whole value. */
        fun list(): List<String> {
            position++
            val items = mutableListOf<String>()
            skipSpaces()
            if (peek() == ']') {
                position++
            } else {
                var separator: Char
                do {
                    skipSpaces()
                    items += item()
                    skipSpaces()
                    separator = peek() ?: fail(file, line, "unterminated list")
                    position++
                } while (separator == ',')
                if (separator != ']') fail(file, line, "expected `,` or `]` in the list but found `$separator`")
            }
            requireEnd()
            return items
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
                else -> bareItem()
            }

        private fun bareItem(): String {
            val start = position
            while (position < text.length && text[position] !in LIST_STOPS) position++
            val value = text.substring(start, position).trim()
            if (value.isEmpty()) fail(file, line, "empty list item")
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
}
