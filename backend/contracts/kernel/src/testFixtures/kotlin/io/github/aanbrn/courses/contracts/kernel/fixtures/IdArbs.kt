// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel.fixtures

import io.github.aanbrn.courses.contracts.kernel.CourseId
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.uuid

/**
 * Generates arbitrary course IDs for property tests.
 *
 * The IDs are built over arbitrary UUIDs of any version, because the TypeID codec accepts every UUID.
 *
 * @return a generator of [CourseId] values
 */
fun Arb.Companion.courseId(): Arb<CourseId> = Arb.uuid().map(::CourseId)
