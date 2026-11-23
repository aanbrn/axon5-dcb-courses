// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import org.gradle.api.GradleException

/**
 * Reads the JDK major version out of the text of the `.java-version` file, the only place the JDK version lives
 * (INFRA-09).
 *
 * The text is trimmed and cut at its first dot, so `25`, `25` followed by a line break, and `25.0.1` all mean 25.
 *
 * @param text the content of the `.java-version` file
 * @return the JDK major version
 * @throws GradleException when the part before the first dot is not a number
 */
fun readJavaMajorVersion(text: String): Int =
    text.trim().substringBefore('.').toIntOrNull()
        ?: throw GradleException(".java-version must hold a numeric JDK major version, but it holds '${text.trim()}'")
