// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.name
import org.junit.jupiter.api.Test

/**
 * The contracts modules carry metadata only (D-04, D-44, INFRA-02): no Spring, no Jackson runtime, and from Axon and
 * Jackson only the annotation packages, declared as `compileOnly` dependencies.
 *
 * The import rules for Axon, Jackson and `tools.jackson` look at the production sources (`src/main`) of the contracts,
 * because that is what a consumer receives. Test and fixture sources of a module may use test-only libraries, for
 * example the kernel's vector test reads YAML with `jackson-dataformat-yaml`; Spring is rejected in every source set.
 * For the same reason a build script may declare Axon and Jackson libraries as `compileOnly`, or as a
 * `testImplementation` or `testRuntimeOnly` dependency, which no consumer receives; any other configuration would ship
 * the framework.
 */
class ContractsArchitectureTest {
    private val contractsProduction: KoScope =
        ArchitectureScopes.contracts().slice { file ->
            ArchitectureScopes.sourceSetOf(file.path) == ArchitectureScopes.PRODUCTION_SOURCE_SET
        }

    @Test
    fun `contracts import no Spring in any source set`() {
        ArchitectureScopes.contracts().files.assertTrue(strict = true) { file ->
            file.imports.none { it.name.startsWith("org.springframework") }
        }
    }

    @Test
    fun `contracts production sources import no Jackson 3 runtime`() {
        contractsProduction.files.assertTrue(strict = true) { file ->
            file.imports.none { it.name.startsWith("tools.jackson") }
        }
    }

    @Test
    fun `contracts production sources import only the Axon annotation packages`() {
        contractsProduction.files.assertTrue(strict = true) { file ->
            file.imports
                .filter { it.name.startsWith("org.axonframework") }
                .all { import -> ArchitectureScopes.allowedAxonPackages.any { import.name.startsWith("$it.") } }
        }
    }

    @Test
    fun `contracts production sources import only the Jackson annotation package`() {
        contractsProduction.files.assertTrue(strict = true) { file ->
            file.imports
                .filter { it.name.startsWith("com.fasterxml.jackson") }
                .all { it.name.startsWith("${ArchitectureScopes.ALLOWED_JACKSON_PACKAGE}.") }
        }
    }

    @Test
    fun `exactly the four contracts modules have a build script`() {
        val found =
            Files.newDirectoryStream(ArchitectureScopes.contractsDirectory).use { directories ->
                directories.filter { Files.isRegularFile(it.resolve("build.gradle.kts")) }.map { it.name }.toSet()
            }

        withClue("the modules with a build script under ${ArchitectureScopes.CONTRACTS_ROOT} differ from D-02") {
            found shouldBe ArchitectureScopes.contractsModules.toSet()
        }
    }

    @Test
    fun `contracts build scripts declare Axon and Jackson only as compileOnly or test dependencies`() {
        val violations =
            ArchitectureScopes.contractsModules.flatMap { module ->
                val script = ArchitectureScopes.contractsDirectory.resolve(module).resolve("build.gradle.kts")
                Files.readAllLines(script).mapIndexedNotNull { index, line ->
                    val code = line.trim()
                    val declaresFramework = code.contains("libs.axon") || code.contains("libs.jackson")
                    if (declaresFramework && !code.startsWith("//") && !ALLOWED_DECLARATION.containsMatchIn(code)) {
                        "${ArchitectureScopes.CONTRACTS_ROOT}/$module/build.gradle.kts:${index + 1}: $code"
                    } else {
                        null
                    }
                }
            }

        withClue("a contracts build script puts a framework library on a shipped classpath (D-04, D-44)") {
            violations.shouldBeEmpty()
        }
    }

    private companion object {
        /** A configuration that never reaches a consumer of the contracts module. */
        val ALLOWED_DECLARATION = Regex("^(compileOnly|testImplementation|testRuntimeOnly)\\(")
    }
}
