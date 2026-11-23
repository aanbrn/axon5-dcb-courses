// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

/**
 * A parsed SPDX licence expression: licence identifiers combined with `AND`, `OR` and parentheses.
 *
 * `OR` lists alternatives, so a component under `EPL-2.0 OR GPL-2.0-with-classpath-exception` may be used under either
 * licence. `AND` lists obligations, so a component under `MIT AND GPL-3.0-only` must satisfy both. `AND` binds tighter
 * than `OR`, as in the SPDX specification. The operators are recognised in upper case only, as the specification
 * requires, so a word of a free-text licence name never reads as an operator. `X WITH Y` stays one identifier, `X WITH
 * Y`, which the policy has to name explicitly.
 */
internal sealed interface LicenceExpression {

    /** A single licence identifier or free-text name. */
    data class Atom(val text: String) : LicenceExpression

    /** All [operands] are required. */
    data class AllOf(val operands: List<LicenceExpression>) : LicenceExpression

    /** Any one of the [operands] is enough. */
    data class AnyOf(val operands: List<LicenceExpression>) : LicenceExpression

    /**
     * Whether the expression is satisfied when [accepts] decides which identifiers are acceptable: an `OR` needs one
     * satisfied operand, an `AND` all of them.
     */
    fun isSatisfiedBy(accepts: (String) -> Boolean): Boolean =
        when (this) {
            is Atom -> accepts(text)
            is AllOf -> operands.all { it.isSatisfiedBy(accepts) }
            is AnyOf -> operands.any { it.isSatisfiedBy(accepts) }
        }

    /** Every identifier of the expression in reading order, duplicates kept. */
    fun atoms(): List<String> =
        when (this) {
            is Atom -> listOf(text)
            is AllOf -> operands.flatMap { it.atoms() }
            is AnyOf -> operands.flatMap { it.atoms() }
        }

    /** Parses SPDX expressions. */
    companion object {
        /**
         * Parses [text], or returns the whole text as one [Atom] when it is not a well-formed expression, so that
         * malformed input is judged as an unknown licence and fails closed.
         */
        fun parse(text: String): LicenceExpression = runCatching {
            Parser(tokens(text)).parseAll()
        }
            .getOrElse { Atom(text.trim()) }

        private fun tokens(text: String): List<String> =
            text.replace("(", " ( ").replace(")", " ) ").split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    private class Parser(private val tokens: List<String>) {
        private var position = 0

        fun parseAll(): LicenceExpression {
            val expression = parseOr()
            require(position == tokens.size) { "unexpected token '${tokens[position]}'" }
            return expression
        }

        private fun parseOr(): LicenceExpression = parseList("OR", ::parseAnd, ::AnyOf)

        private fun parseAnd(): LicenceExpression = parseList("AND", ::parsePrimary, ::AllOf)

        private fun parseList(
            operator: String,
            operand: () -> LicenceExpression,
            combine: (List<LicenceExpression>) -> LicenceExpression,
        ): LicenceExpression {
            val operands = mutableListOf(operand())
            while (peek() == operator) {
                position++
                operands += operand()
            }
            return operands.singleOrNull() ?: combine(operands)
        }

        private fun parsePrimary(): LicenceExpression {
            val token = next()
            require(token !in RESERVED) { "unexpected '$token'" }
            return when {
                token == "(" -> parseOr().also { require(next() == ")") { "missing ')'" } }
                peek() == "WITH" -> Atom("$token ${next()} ${identifier()}")
                else -> Atom(token)
            }
        }

        private fun identifier(): String = next().also { require(it !in RESERVED) { "unexpected '$it'" } }

        private fun peek(): String? = tokens.getOrNull(position)

        private fun next(): String = tokens.getOrNull(position++) ?: error("unexpected end of expression")

        private companion object {
            val RESERVED = setOf("AND", "OR", "WITH", ")")
        }
    }
}
