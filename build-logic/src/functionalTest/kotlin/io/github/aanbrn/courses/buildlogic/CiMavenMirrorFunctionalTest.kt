// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves the repository order that `scripts/ci/maven-central-mirror.init.gradle.kts` creates: with the script, the
 * Maven Central mirror is the first plugin repository and the first dependency repository of every build, the fixture
 * root build and the real `build-logic` build included, and the repositories the settings files declare follow in their
 * original order. Without the script the lists are exactly the declared ones, which is what local builds and Renovate
 * see.
 *
 * A probe init script prints the lists at `settingsEvaluated`, after the settings scripts and their plugins have added
 * their own repositories. Every run uses the configuration cache and isolated projects, as every fixture run does, so
 * the init script is also proven to be compatible with both.
 */
class CiMavenMirrorFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private val fixture by lazy {
        GradleFixture(projectDir).apply { file("build.gradle.kts", "${GradleFixture.HEADER}\n") }
    }

    private val mirrorScript: Path by lazy {
        val repoRoot = checkNotNull(System.getProperty("repoRoot")) { "System property 'repoRoot' is not set." }
        Path.of(repoRoot).resolve("scripts/ci/maven-central-mirror.init.gradle.kts")
    }

    @Test
    fun `with the init script the mirror is the first plugin and dependency repository of every build`() {
        val builds = repositoriesOf(runHelp(withMirror = true))

        for (build in listOf(FIXTURE_BUILD, BUILD_LOGIC)) {
            builds shouldContainKey build
            builds.getValue(build).plugins.first() shouldBe MIRROR
            builds.getValue(build).dependencies.first() shouldBe MIRROR
        }
    }

    @Test
    fun `with the init script the declared repositories follow the mirror in their original order`() {
        val builds = repositoriesOf(runHelp(withMirror = true))

        builds.getValue(FIXTURE_BUILD).plugins shouldContainExactly listOf(MIRROR, PLUGIN_PORTAL)
        builds.getValue(FIXTURE_BUILD).dependencies shouldContainExactly listOf(MIRROR, MAVEN_CENTRAL)
        builds.getValue(BUILD_LOGIC).plugins shouldContainExactly listOf(MIRROR, PLUGIN_PORTAL)
        builds.getValue(BUILD_LOGIC).dependencies shouldContainExactly listOf(MIRROR, PLUGIN_PORTAL, MAVEN_CENTRAL)
    }

    @Test
    fun `without the init script both builds keep exactly the declared repositories`() {
        val builds = repositoriesOf(runHelp(withMirror = false))

        builds.getValue(FIXTURE_BUILD).plugins shouldContainExactly emptyList()
        builds.getValue(FIXTURE_BUILD).dependencies shouldContainExactly listOf(MAVEN_CENTRAL)
        builds.getValue(BUILD_LOGIC).plugins shouldContainExactly emptyList()
        builds.getValue(BUILD_LOGIC).dependencies shouldContainExactly listOf(PLUGIN_PORTAL, MAVEN_CENTRAL)
    }

    @Test
    fun `a second run with the init script reuses the configuration cache`() {
        runHelp(withMirror = true)

        runHelp(withMirror = true) shouldContain REUSED
    }

    private fun runHelp(withMirror: Boolean): String {
        val probe = fixture.file(PROBE_FILE, PROBE_SCRIPT).toString()
        val arguments = buildList {
            add("help")
            if (withMirror) addAll(listOf("--init-script", mirrorScript.toString()))
            addAll(listOf("--init-script", probe))
        }
        return fixture.run(*arguments.toTypedArray()).output
    }

    /** The plugin and dependency repository URLs of one build, in the order Gradle resolves them. */
    private data class Repositories(val plugins: List<String>, val dependencies: List<String>)

    /** Parses every probe line of [output] into the repositories of the build it names. */
    private fun repositoriesOf(output: String): Map<String, Repositories> =
        output
            .lineSequence()
            .mapNotNull { PROBE_LINE.matchEntire(it.trim()) }
            .associate { match ->
                val (build, plugins, dependencies) = match.destructured
                build to Repositories(split(plugins), split(dependencies))
            }

    private fun split(urls: String): List<String> = urls.split(' ').filter { it.isNotEmpty() }

    private companion object {
        const val FIXTURE_BUILD = "fixture"
        const val BUILD_LOGIC = "build-logic"
        const val PROBE_FILE = "probe.init.gradle.kts"
        const val REUSED = "Reusing configuration cache."
        const val MIRROR = "https://maven-central.storage-download.googleapis.com/maven2/"
        const val MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/"
        const val PLUGIN_PORTAL = "https://plugins.gradle.org/m2"

        val PROBE_LINE = Regex("""MIRROR-PROBE build=(\S+) plugins=\[(.*)] dependencies=\[(.*)]""")

        /**
         * Prints, per build and in order, the URL of every `pluginManagement` and `dependencyResolutionManagement`
         * repository. It reads only `Settings`, so isolated projects accept it.
         */
        val PROBE_SCRIPT =
            $$"""
            $${GradleFixture.HEADER}
            @Suppress("UnstableApiUsage")
            gradle.settingsEvaluated {
                fun urls(handler: RepositoryHandler) =
                    handler.joinToString(" ") { (it as MavenArtifactRepository).url.toString() }
                println(
                    "MIRROR-PROBE build=${rootProject.name} plugins=[${urls(pluginManagement.repositories)}] " +
                        "dependencies=[${urls(dependencyResolutionManagement.repositories)}]"
                )
            }
            """
                .trimIndent() + "\n"
    }
}
