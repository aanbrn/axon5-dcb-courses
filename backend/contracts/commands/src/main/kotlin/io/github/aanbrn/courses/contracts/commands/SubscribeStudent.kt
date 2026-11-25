// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.commands

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.github.aanbrn.courses.contracts.kernel.MessageNamespace
import io.github.aanbrn.courses.contracts.kernel.Rejection
import io.github.aanbrn.courses.contracts.kernel.RejectionCode
import io.github.aanbrn.courses.contracts.kernel.StudentId
import org.axonframework.messaging.commandhandling.annotation.Command

/**
 * Subscribes a student to a course.
 *
 * @property courseId the course to subscribe to
 * @property studentId the student who subscribes
 */
@Command(namespace = MessageNamespace.COURSES, name = "SubscribeStudent", version = "1")
data class SubscribeStudent(val courseId: CourseId, val studentId: StudentId)

/**
 * The outcome of [SubscribeStudent]: the student was subscribed, or a business rule rejected the command.
 *
 * The type carries its own `@type` discriminator, so only these declared cases resolve when a result is read back.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "@type")
sealed interface SubscribeResult {
    /** The student was subscribed. */
    data object Subscribed : SubscribeResult

    /**
     * A business rule rejected the subscription.
     *
     * @property code the stable code of the rule that rejected the command
     */
    data class Rejected(override val code: RejectionCode) : SubscribeResult, Rejection
}
