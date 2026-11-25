// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

/**
 * The registry of DCB tag keys.
 *
 * The tag key of an entity equals its TypeID prefix, so a tagged event names its entity the same way its identifier
 * does. Every `@EventTag` key must come from this object; the architecture rules enforce that. Service tags, for
 * example a normalised course name used for uniqueness, are added here by the slice that needs them.
 */
object Tags {
    /** The tag key of a course, equal to [CourseId.PREFIX]. */
    const val COURSE = CourseId.PREFIX

    /** The tag key of a student, equal to [StudentId.PREFIX]. */
    const val STUDENT = StudentId.PREFIX
}
