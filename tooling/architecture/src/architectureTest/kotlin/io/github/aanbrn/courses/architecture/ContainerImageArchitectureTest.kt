// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

/**
 * No source names a container image as a string literal (INFRA-09, T-02-23).
 *
 * The images of the test suites come from the version catalog through system properties, with their tag and digest, so
 * that one place pins them and a change re-runs the tests. A `DockerImageName.parse("...")` call or a constructor of a
 * class whose name ends with `Container` that receives a string literal as its first argument bypasses that pin. The
 * rule scans every source set under `backend` and `tooling`. Comments and the contents of string and character literals
 * are masked first by [KotlinCodeView], which tracks string state, so documentation and test data may show the
 * forbidden form while a URL string or a glob string cannot hide a literal after it.
 */
class ContainerImageArchitectureTest {
    @Test
    fun `no image name or container constructor takes a string literal image`() {
        val scope = ArchitectureScopes.backend() + ArchitectureScopes.tooling()

        scope.files.assertTrue(strict = true) { file ->
            val violations = imageLiteralsIn(file.text)
            violations.forEach { addFailureReason("${file.nameWithExtension}: image literal in '$it'") }
            violations.isEmpty()
        }
    }

    @Test
    fun `a URL string on the line of an image literal does not hide it`() {
        val source = """val health = "http://localhost:8024"; val image = DockerImageName.parse("axoniq/axonserver")"""

        imageLiteralsIn(source) shouldHaveSize 1
    }

    @Test
    fun `a glob string before an image literal does not hide it from the next block comment`() {
        val source =
            """
            val glob = "**/*.kt"
            val postgres = GenericContainer("postgres:18")
            /* a comment */
            """
                .trimIndent()

        imageLiteralsIn(source) shouldHaveSize 1
    }

    @Test
    fun `a forbidden call shape in a comment or a string literal is not a violation`() {
        val source =
            """
            // DockerImageName.parse("a/b")
            /* GenericContainer("c:1") */
            val text = "DockerImageName.parse(\"d/e\")"
            val raw = ""${'"'}PostgresContainer("f:2")""${'"'}
            """
                .trimIndent()

        imageLiteralsIn(source).shouldBeEmpty()
    }

    private fun imageLiteralsIn(source: String): List<String> {
        val code = KotlinCodeView.of(source)
        return LITERAL_IMAGE_CALLS.flatMap { pattern -> pattern.findAll(code).map { it.value.trim() } }
    }

    private companion object {
        /** The two call shapes of the rule: `DockerImageName.parse` and a capitalised `*Container` constructor. */
        val LITERAL_IMAGE_CALLS =
            listOf(
                Regex("\\bDockerImageName\\s*\\.\\s*parse\\s*\\(\\s*(?:\\w+\\s*=\\s*)?\""),
                Regex("\\b[A-Z][A-Za-z0-9_]*Container\\s*(?:<[^>]*>)?\\s*\\(\\s*(?:\\w+\\s*=\\s*)?\""),
            )
    }
}
