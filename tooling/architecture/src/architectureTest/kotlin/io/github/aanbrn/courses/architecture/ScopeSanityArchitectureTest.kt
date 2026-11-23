// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import org.junit.jupiter.api.Test

/**
 * Proves that the scopes of the other rules see the modules they are meant to see. A Konsist rule on an empty or
 * misplaced scope passes silently, so this test names the module that is missing from a scope (RESEARCH Pitfall 4).
 */
class ScopeSanityArchitectureTest {
    @Test
    fun `the contracts scope holds the kernel, events and commands sources`() {
        val paths = ArchitectureScopes.contracts().files.map { it.path.replace('\\', '/') }

        listOf("kernel", "events", "commands").forEach { module ->
            withClue("the contracts scope holds no source of the '$module' module") {
                paths.any { it.contains("/${ArchitectureScopes.CONTRACTS_ROOT}/$module/src/") } shouldBe true
            }
        }
    }

    @Test
    fun `the backend scope holds the serialization and command service sources`() {
        val paths = ArchitectureScopes.backend().files.map { it.path.replace('\\', '/') }

        listOf("serialization-jackson", "command-service").forEach { module ->
            withClue("the backend scope holds no source of the '$module' module") {
                paths.any { it.contains("/${ArchitectureScopes.BACKEND_ROOT}/$module/src/") } shouldBe true
            }
        }
    }

    @Test
    fun `each of the four contracts modules exists under the contracts scope root with a build script`() {
        val root = ArchitectureScopes.contractsDirectory

        ArchitectureScopes.contractsModules.forEach { module ->
            withClue(
                "the contracts module '$module' has no build.gradle.kts under ${ArchitectureScopes.CONTRACTS_ROOT}",
            ) {
                Files.isRegularFile(root.resolve(module).resolve("build.gradle.kts")) shouldBe true
            }
        }
    }

    @Test
    fun `the test scope holds sources of every suite that a rule checks`() {
        val sourceSets = ArchitectureScopes.testSources().files.mapNotNull { ArchitectureScopes.sourceSetOf(it.path) }

        sourceSets.toSet() shouldContainAll setOf("test", "componentTest", "integrationTest", "functionalTest")
    }
}
