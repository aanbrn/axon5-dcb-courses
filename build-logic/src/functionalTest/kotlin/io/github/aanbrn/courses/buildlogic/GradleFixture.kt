// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

/**
 * A throwaway Gradle build in [projectDir] that applies the real conventions from this repository.
 *
 * The fixture includes the actual `build-logic` directory through `pluginManagement { includeBuild
 * }` instead of injecting a plugin classpath, because the typed `libs` accessor used by the
 * precompiled script plugins fails to resolve with an injected classpath.
 *
 * @param projectDir the empty directory the fixture build is written to, normally a JUnit temporary
 *   directory.
 */
class GradleFixture(private val projectDir: Path) {
    private val buildLogic = requiredProperty("buildLogic")
    private val repoRoot = Path.of(requiredProperty("repoRoot"))
    private val catalog = requiredProperty("catalog")
    private val includedProjects = mutableListOf<String>()

    init {
        writeSettings()
    }

    /**
     * Writes [content] to [path] relative to the fixture root, creating missing directories.
     *
     * @return the absolute path of the written file.
     */
    fun file(path: String, content: String): Path {
        val target = projectDir.resolve(path)
        Files.createDirectories(target.parent)
        return Files.writeString(target, content)
    }

    /** Reads the file at [path] relative to the fixture root. */
    fun read(path: String): String = Files.readString(projectDir.resolve(path))

    /**
     * Copies the repository file at [relativePath] to the same relative location in the fixture.
     *
     * @return the absolute path of the copy.
     */
    fun copyFromRepo(relativePath: String): Path {
        val target = projectDir.resolve(relativePath)
        Files.createDirectories(target.parent)
        return Files.copy(repoRoot.resolve(relativePath), target)
    }

    /** Adds the project with the given Gradle [projectPath], for example `:lib`, to the build. */
    fun include(projectPath: String) {
        includedProjects += projectPath
        writeSettings()
    }

    /**
     * Runs Gradle with [args] and expects success.
     *
     * Every run enables the configuration cache and isolated projects, so a convention that breaks
     * either fails the test that exercised it.
     */
    fun run(vararg args: String): BuildResult = runner(*args).build()

    /** Runs Gradle with [args] under the same flags as [run] and expects the build to fail. */
    fun runAndFail(vararg args: String): BuildResult = runner(*args).buildAndFail()

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(
                *args,
                "--configuration-cache",
                "--isolated-projects",
                "--stacktrace",
                "--console=plain",
            )

    private fun writeSettings() {
        val includes = includedProjects.joinToString(separator = "\n") { "include(\"$it\")" }
        file(
            "settings.gradle.kts",
            """
            // SPDX-License-Identifier: MIT
            pluginManagement { includeBuild("$buildLogic") }

            plugins { id("courses.settings") }

            dependencyResolutionManagement {
                versionCatalogs { create("libs") { from(files("$catalog")) } }
            }

            rootProject.name = "fixture"

            $includes
            """
                .trimIndent() + "\n",
        )
    }

    /** Creates fixtures with the project layout every convention test starts from. */
    companion object {
        /** The SPDX header every Kotlin source and script in a fixture starts with. */
        const val HEADER = "// SPDX-License-Identifier: MIT"

        /**
         * Creates a fixture in [projectDir] with the `courses.root` plugin on the root project, the
         * repository's own `:platform` project, and a `:lib` project that applies
         * `courses.kotlin-library` and declares no dependencies of its own.
         *
         * The shared Spotless header, the shared detekt and Prettier configuration, `.nvmrc`, and a
         * JDK 25 `.java-version` are copied or written as the real repository has them.
         */
        fun standard(projectDir: Path): GradleFixture =
            GradleFixture(projectDir).apply {
                copyFromRepo("config/spotless/license-header.txt")
                copyFromRepo("config/detekt/detekt.yml")
                copyFromRepo(".prettierrc.yaml")
                copyFromRepo(".nvmrc")
                file(".java-version", "25\n")
                file("build.gradle.kts", "$HEADER\nplugins { id(\"courses.root\") }\n")
                copyFromRepo("platform/build.gradle.kts")
                file(
                    "lib/build.gradle.kts",
                    "$HEADER\nplugins { id(\"courses.kotlin-library\") }\n",
                )
                file(
                    "lib/src/main/kotlin/fixture/Documented.kt",
                    "$HEADER\npackage fixture\n\n/** A documented public class. */\nclass Documented\n",
                )
                include(":platform")
                include(":lib")
            }

        private fun requiredProperty(name: String): String =
            checkNotNull(System.getProperty(name)) {
                "System property '$name' is not set; run the fixture through the functionalTest task."
            }
    }
}
