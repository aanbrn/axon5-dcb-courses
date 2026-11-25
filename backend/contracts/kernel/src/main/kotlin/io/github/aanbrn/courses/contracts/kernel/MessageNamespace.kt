// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

/**
 * The message namespace of the system.
 *
 * Every `@Event`, `@Command` and `@Query` declares this namespace, and Axon Server stores a message type as
 * `courses.<Name>#<version>`. A name and a version are one-way after the first persisted event: renaming or reusing
 * them breaks replay.
 */
object MessageNamespace {
    /** The namespace of every message in this system. */
    const val COURSES = "courses"
}
