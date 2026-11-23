// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

import java.util.UUID

// The prefixes live at file level because a companion cannot read its own members in its super constructor call.
private const val COURSE_PREFIX = "course"
private const val STUDENT_PREFIX = "student"

/**
 * The identity of a course, written as `course_<26 characters>`.
 *
 * @property uuid the UUID the TypeID suffix encodes
 */
@JvmInline
value class CourseId(override val uuid: UUID) : TypedId {

    /** @return the canonical TypeID */
    override fun toString(): String = TypeId.encode(PREFIX, uuid)

    /** Factory and parse API of [CourseId]. */
    companion object : TypedIdCompanion<CourseId>(COURSE_PREFIX, ::CourseId) {
        /** The TypeID prefix of a course. */
        const val PREFIX: String = COURSE_PREFIX
    }
}

/**
 * The identity of a student, written as `student_<26 characters>`.
 *
 * @property uuid the UUID the TypeID suffix encodes
 */
@JvmInline
value class StudentId(override val uuid: UUID) : TypedId {

    /** @return the canonical TypeID */
    override fun toString(): String = TypeId.encode(PREFIX, uuid)

    /** Factory and parse API of [StudentId]. */
    companion object : TypedIdCompanion<StudentId>(STUDENT_PREFIX, ::StudentId) {
        /** The TypeID prefix of a student. */
        const val PREFIX: String = STUDENT_PREFIX
    }
}
