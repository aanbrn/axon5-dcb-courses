// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * The scopes, module lists and naming tables that the architecture rules share.
 *
 * Every path is relative to the repository root, which Konsist finds by walking up from the working directory of the
 * test task until it meets `gradlew` and `settings.gradle.kts`. A scope that points at a wrong directory is empty, so
 * every rule asserts with `strict = true` and the scope sanity test names the modules each scope must contain.
 */
object ArchitectureScopes {
    /** The directory of the four contracts modules, relative to the repository root (D-02). */
    const val CONTRACTS_ROOT = "backend/contracts"

    /** The directory of every backend module, relative to the repository root. */
    const val BACKEND_ROOT = "backend"

    /** The directory of the tooling modules, relative to the repository root. */
    const val TOOLING_ROOT = "tooling"

    /** The directory of the TestKit tests of the build logic, relative to the repository root. */
    const val BUILD_LOGIC_FUNCTIONAL_TEST_ROOT = "build-logic/src/functionalTest"

    /** The directory name of the production source set of a module. */
    const val PRODUCTION_SOURCE_SET = "main"

    /** The four contracts modules of D-02, which are the directories under [CONTRACTS_ROOT]. */
    val contractsModules: List<String> = listOf("kernel", "commands", "events", "queries")

    /** The Axon packages whose annotations the contracts may import as metadata (D-04). */
    val allowedAxonPackages: List<String> =
        listOf(
            "org.axonframework.messaging.eventhandling.annotation",
            "org.axonframework.messaging.commandhandling.annotation",
            "org.axonframework.messaging.queryhandling.annotation",
            "org.axonframework.eventsourcing.annotation",
            "org.axonframework.modelling.annotation",
        )

    /** The only Jackson package the contracts may import (D-44): the annotations, never the databind runtime. */
    const val ALLOWED_JACKSON_PACKAGE = "com.fasterxml.jackson.annotation"

    /** The suffix every test class of a source set carries, by the name of the source set (D-17, TEST-11). */
    val suiteSuffixes: Map<String, String> =
        mapOf(
            "test" to "Test",
            "componentTest" to "ComponentTest",
            "integrationTest" to "IntegrationTest",
            "architectureTest" to "ArchitectureTest",
            "functionalTest" to "FunctionalTest",
        )

    /** The repository root as Konsist resolved it. */
    val repositoryRoot: Path
        get() = Path(Konsist.projectRootPath)

    /** The directory of the contracts modules on disk. */
    val contractsDirectory: Path
        get() = repositoryRoot.resolve(CONTRACTS_ROOT)

    /** Every source set of the four contracts modules. */
    fun contracts(): KoScope = Konsist.scopeFromDirectory(CONTRACTS_ROOT)

    /** Every source set of every backend module. */
    fun backend(): KoScope = Konsist.scopeFromDirectory(BACKEND_ROOT)

    /** Every source set of every tooling module. */
    fun tooling(): KoScope = Konsist.scopeFromDirectory(TOOLING_ROOT)

    /** The scope that holds test classes: the backend and tooling modules and the build-logic TestKit tests. */
    fun testSources(): KoScope =
        Konsist.scopeFromDirectories(listOf(BACKEND_ROOT, TOOLING_ROOT, BUILD_LOGIC_FUNCTIONAL_TEST_ROOT))

    /**
     * The name of the source set that a source file belongs to, read from its `src/<name>/kotlin` directory.
     *
     * @param path the path of a Kotlin source file
     * @return the source set name, or `null` when the path is not below a `src/<name>/kotlin` directory
     */
    fun sourceSetOf(path: String): String? = SOURCE_SET_PATTERN.find(path.replace('\\', '/'))?.groupValues?.get(1)

    private val SOURCE_SET_PATTERN = Regex("/src/([A-Za-z]+)/kotlin/")
}
