// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * A test class carries the suffix of the suite it runs in (D-17, TEST-11), so a reader and the build tell the levels
 * apart by name, and no test function hides its return type behind an expression body (RESEARCH Pitfall 3).
 *
 * A class with `@Test` or `@ParameterizedTest` functions under `src/<suite>/kotlin` ends with the suffix of that suite
 * and with the suffix of no other suite. Helper classes without test functions are not subject to the rule, and nested
 * test classes follow their enclosing class. An expression-bodied test whose expression returns a value is not run by
 * JUnit and is skipped without a signal, which is why the body of a test is always a block.
 */
class TestNamingArchitectureTest {
    private val testClasses: List<KoClassDeclaration> =
        ArchitectureScopes.testSources().classes().filter { declaration ->
            !declaration.hasAnnotationWithName("Nested") &&
                declaration.functions().any { it.isTest() } &&
                sourceSetOf(declaration) in ArchitectureScopes.suiteSuffixes
        }

    @Test
    fun `a test class ends with the suffix of its source set and with no other suite suffix`() {
        testClasses.assertTrue(strict = true) { declaration ->
            val sourceSet = checkNotNull(sourceSetOf(declaration))
            val expected = ArchitectureScopes.suiteSuffixes.getValue(sourceSet)
            val foreign =
                ArchitectureScopes.suiteSuffixes.values.filter { other ->
                    other != expected && !expected.endsWith(other) && declaration.name.endsWith(other)
                }
            if (!declaration.name.endsWith(expected)) {
                addFailureReason("${declaration.name} under src/$sourceSet must end with '$expected'")
            }
            foreign.forEach { addFailureReason("${declaration.name} under src/$sourceSet must not end with '$it'") }
            declaration.name.endsWith(expected) && foreign.isEmpty()
        }
    }

    @Test
    fun `a test function has a block body`() {
        testClasses
            .flatMap { it.functions() }
            .filter { it.isTest() }
            .assertFalse(strict = true) { function -> function.hasExpressionBody }
    }

    private fun KoFunctionDeclaration.isTest(): Boolean = hasAnnotationWithName("Test", "ParameterizedTest")

    private fun sourceSetOf(declaration: KoClassDeclaration): String? =
        ArchitectureScopes.sourceSetOf(declaration.containingFile.path)
}
