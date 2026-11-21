// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

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

    /**
     * Runs Gradle with [args] like [run], in an environment whose `PATH` has no directory that
     * contains an executable named [tool].
     */
    fun runWithout(tool: String, vararg args: String): BuildResult =
        runner(*args).withEnvironment(environmentWithout(tool)).build()

    /** Like [runWithout], but expects the build to fail. */
    fun runAndFailWithout(tool: String, vararg args: String): BuildResult =
        runner(*args).withEnvironment(environmentWithout(tool)).buildAndFail()

    private fun environmentWithout(tool: String): Map<String, String> {
        val path =
            System.getenv("PATH")
                .orEmpty()
                .split(File.pathSeparator)
                .filterNot { directory -> File(directory, tool).exists() }
                .joinToString(File.pathSeparator)
        return System.getenv() + ("PATH" to path)
    }

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
            $HEADER
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
        // REUSE-IgnoreStart
        const val HEADER = "// SPDX-License-Identifier: MIT"

        // REUSE-IgnoreEnd

        /**
         * Creates a fixture in [projectDir] with the `courses.root` plugin on the root project, the
         * repository's own `:platform` project, and a `:lib` project that applies
         * `courses.kotlin-library` and declares no dependencies of its own.
         *
         * The shared Spotless headers, the shared detekt, Prettier, taplo and editor configuration,
         * `.nvmrc`, and a JDK 25 `.java-version` are copied or written as the real repository has
         * them.
         */
        fun standard(projectDir: Path): GradleFixture =
            GradleFixture(projectDir).apply {
                copyFromRepo("config/spotless/license-header.txt")
                copyFromRepo("config/detekt/detekt.yml")
                copyFromRepo(".prettierrc.yaml")
                copyFromRepo(".taplo.toml")
                copyFromRepo(".editorconfig")
                copyFromRepo("config/spotless/license-header-shell.txt")
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

        /**
         * The outcomes of a task that passed, whether it ran or its result came from the build
         * cache, which TestKit shares with the user's Gradle home.
         */
        val PASSED: Set<TaskOutcome> = setOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE)

        /**
         * Creates a fixture in [projectDir] whose root project applies only `courses.governance`.
         *
         * Governance checks read plain repository files, so the fixture carries none of them; each
         * test writes the files its check reads.
         */
        fun governance(projectDir: Path): GradleFixture =
            GradleFixture(projectDir).apply {
                file("build.gradle.kts", "$HEADER\nplugins { id(\"courses.governance\") }\n")
            }

        private fun requiredProperty(name: String): String =
            checkNotNull(System.getProperty(name)) {
                "System property '$name' is not set; run the fixture through the functionalTest task."
            }
    }
}
