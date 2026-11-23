// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.events

import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.MessageNamespace
import io.github.aanbrn.courses.contracts.kernel.StudentId
import io.github.aanbrn.courses.contracts.kernel.Tags
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.eventhandling.annotation.Event

/**
 * A course was created and can accept subscriptions.
 *
 * @property courseId the identity of the new course, which tags the event
 * @property name the display name of the course
 * @property capacity the number of students the course can hold
 */
@Event(namespace = MessageNamespace.COURSES, name = "CourseCreated", version = "1")
data class CourseCreated(@EventTag(key = Tags.COURSE) val courseId: CourseId, val name: String, val capacity: Int)

/**
 * A student took a seat in a course.
 *
 * @property courseId the course the student subscribed to, which tags the event
 * @property studentId the student who subscribed, which tags the event
 */
@Event(namespace = MessageNamespace.COURSES, name = "StudentSubscribed", version = "1")
data class StudentSubscribed(
    @EventTag(key = Tags.COURSE) val courseId: CourseId,
    @EventTag(key = Tags.STUDENT) val studentId: StudentId,
)
