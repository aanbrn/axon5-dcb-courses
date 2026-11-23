// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

private val REJECTION_CODE_FORMAT = Regex("^[a-z][a-z0-9]*(-[a-z0-9]+)*$")

/**
 * The base of every per-command `Rejected` result.
 *
 * A business rejection is a value, not an exception, so every command's sealed result type has one `Rejected` case that
 * implements this interface and names the broken rule by a stable [code].
 */
interface Rejection {
    /** The stable code of the rule that rejected the command. */
    val code: RejectionCode
}

/**
 * The stable, machine-readable code of a rejection, in lower-case kebab-case such as `course-full`.
 *
 * A code never changes meaning once it has been released, because API clients and decision records match on it.
 *
 * @property value the code text
 * @throws IllegalArgumentException if [value] is not lower-case kebab-case
 */
@JvmInline
value class RejectionCode(val value: String) {

    init {
        require(REJECTION_CODE_FORMAT.matches(value)) { "Rejection code '$value' is not lower-case kebab-case" }
    }

    /** @return the code text */
    override fun toString(): String = value
}
