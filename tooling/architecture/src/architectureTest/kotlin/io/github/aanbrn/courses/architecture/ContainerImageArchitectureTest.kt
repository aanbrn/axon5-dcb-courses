// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * No source names a container image as a string literal (INFRA-09, T-02-23).
 *
 * The images of the test suites come from the version catalog through system properties, with their tag and digest, so
 * that one place pins them and a change re-runs the tests. A `DockerImageName.parse("...")` call or a constructor of a
 * class whose name ends with `Container` that receives a string literal as its first argument bypasses that pin. The
 * rule scans every source set under `backend` and `tooling`; comments are removed first, so documentation may show the
 * forbidden form.
 */
class ContainerImageArchitectureTest {
    @Test
    fun `no image name or container constructor takes a string literal image`() {
        val scope = ArchitectureScopes.backend() + ArchitectureScopes.tooling()

        scope.files.assertTrue(strict = true) { file ->
            val code = file.text.withoutComments()
            val violations = LITERAL_IMAGE_CALLS.flatMap { pattern -> pattern.findAll(code).map { it.value.trim() } }
            violations.forEach { addFailureReason("${file.nameWithExtension}: image literal in '$it'") }
            violations.isEmpty()
        }
    }

    private fun String.withoutComments(): String = replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    private companion object {
        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("//[^\\n]*")

        /** The two call shapes of the rule: `DockerImageName.parse` and a capitalised `*Container` constructor. */
        val LITERAL_IMAGE_CALLS =
            listOf(
                Regex("\\bDockerImageName\\s*\\.\\s*parse\\s*\\(\\s*(?:\\w+\\s*=\\s*)?\""),
                Regex("\\b[A-Z][A-Za-z0-9_]*Container\\s*(?:<[^>]*>)?\\s*\\(\\s*(?:\\w+\\s*=\\s*)?\""),
            )
    }
}
