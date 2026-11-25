// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

import java.util.UUID

/**
 * A decoded TypeID: the type prefix and the UUID the suffix encodes.
 *
 * @property prefix the type prefix, empty for a TypeID without one
 * @property uuid the UUID held by the 26-character suffix
 */
data class DecodedTypeId(val prefix: String, val uuid: UUID)

/**
 * Codec for TypeID specification v0.3.0 (jetify-com/typeid): a lowercase type prefix, an underscore and a 26-character
 * suffix that is the UUID in lowercase Crockford base32.
 *
 * No mature JVM library exists for the format, so the codec is part of the kernel and is checked against all official
 * valid and invalid vectors. It is strict by design: every input that the specification rejects throws
 * [IllegalArgumentException], and every message starts with `Malformed TypeID '` and names the rule that was broken.
 */
object TypeId {
    private const val ALPHABET = "0123456789abcdefghjkmnpqrstvwxyz"
    private const val SEPARATOR = '_'
    private const val SUFFIX_LENGTH = 26
    private const val BITS_PER_CHAR = 5
    private const val CHAR_MASK = (1L shl BITS_PER_CHAR) - 1
    private const val HALF_BITS = Long.SIZE_BITS

    // 26 characters carry 130 bits, two more than a UUID; the first character may only use its low three bits.
    private const val MAX_FIRST_CHAR_VALUE = 7
    private const val ASCII_LIMIT = 128
    private const val NOT_IN_ALPHABET = -1

    private val prefixPattern = Regex("([a-z]([a-z_]{0,61}[a-z])?)?")

    private val charValues =
        IntArray(ASCII_LIMIT) { NOT_IN_ALPHABET }
            .also { table -> ALPHABET.forEachIndexed { i, c -> table[c.code] = i } }

    /**
     * Tells whether [prefix] is a valid TypeID prefix.
     *
     * A valid prefix is empty, or at most 63 characters of `a-z` and `_` that start and end with a letter.
     *
     * @param prefix the candidate prefix
     * @return `true` when the specification accepts [prefix]
     */
    fun isValidPrefix(prefix: String): Boolean = prefixPattern.matches(prefix)

    /**
     * Encodes [uuid] as a TypeID with the given [prefix].
     *
     * @param prefix the type prefix, empty for none
     * @param uuid the UUID to encode; all 128 bits are preserved
     * @return the canonical TypeID, `prefix_suffix`, or the bare suffix for an empty prefix
     * @throws IllegalArgumentException when [prefix] is not a valid TypeID prefix
     */
    fun encode(prefix: String, uuid: UUID): String {
        require(isValidPrefix(prefix)) {
            "Malformed TypeID '$prefix': a prefix is at most 63 characters of a-z and '_', starting and ending with a letter"
        }
        val suffix = CharArray(SUFFIX_LENGTH)
        var hi = uuid.mostSignificantBits
        var lo = uuid.leastSignificantBits
        // Peel five bits at a time off the 128-bit value, least significant first, which is the last character first.
        for (i in SUFFIX_LENGTH - 1 downTo 0) {
            suffix[i] = ALPHABET[(lo and CHAR_MASK).toInt()]
            lo = (lo ushr BITS_PER_CHAR) or (hi shl (HALF_BITS - BITS_PER_CHAR))
            hi = hi ushr BITS_PER_CHAR
        }
        val encoded = String(suffix)
        return if (prefix.isEmpty()) encoded else "$prefix$SEPARATOR$encoded"
    }

    /**
     * Decodes a TypeID into its prefix and UUID.
     *
     * @param value the TypeID text; it is case-sensitive and ASCII only
     * @return the prefix and the UUID the suffix encodes
     * @throws IllegalArgumentException when [value] is not a canonical TypeID
     */
    fun decode(value: String): DecodedTypeId {
        val separator = value.lastIndexOf(SEPARATOR)
        require(separator != 0) { "Malformed TypeID '$value': empty prefix with separator" }
        val prefix = if (separator < 0) "" else value.substring(0, separator)
        val suffix = if (separator < 0) value else value.substring(separator + 1)
        require(isValidPrefix(prefix)) { "Malformed TypeID '$value': invalid prefix '$prefix'" }
        require(suffix.length == SUFFIX_LENGTH) {
            "Malformed TypeID '$value': suffix must be $SUFFIX_LENGTH characters"
        }
        var hi = 0L
        var lo = 0L
        suffix.forEachIndexed { index, char ->
            val digit = if (char.code < ASCII_LIMIT) charValues[char.code] else NOT_IN_ALPHABET
            require(digit != NOT_IN_ALPHABET) { "Malformed TypeID '$value': invalid character '$char'" }
            require(index > 0 || digit <= MAX_FIRST_CHAR_VALUE) {
                "Malformed TypeID '$value': suffix overflows 128 bits"
            }
            // Shift the 128-bit accumulator left by one character and append the digit; the overflow check above
            // guarantees that nothing is shifted out of `hi`.
            hi = (hi shl BITS_PER_CHAR) or (lo ushr (HALF_BITS - BITS_PER_CHAR))
            lo = (lo shl BITS_PER_CHAR) or digit.toLong()
        }
        return DecodedTypeId(prefix, UUID(hi, lo))
    }
}
