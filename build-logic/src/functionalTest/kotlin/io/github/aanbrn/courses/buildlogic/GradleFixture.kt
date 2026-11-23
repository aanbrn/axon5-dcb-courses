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
 * The fixture includes the actual `build-logic` directory through `pluginManagement { includeBuild }` instead of
 * injecting a plugin classpath, because the typed `libs` accessor used by the precompiled script plugins fails to
 * resolve with an injected classpath.
 *
 * @property projectDir the empty directory the fixture build is written to, normally a JUnit temporary directory.
 */
class GradleFixture(val projectDir: Path) {
    private val buildLogic = requiredProperty("buildLogic")
    private val repoRoot = Path.of(requiredProperty("repoRoot"))
    private val includedProjects = mutableListOf<String>()

    /**
     * The absolute path of the version catalog the fixture build reads as `libs`: the repository's own until a test
     * replaces it, for example through [changeCatalog]. Assigning a path rewrites the fixture settings.
     */
    var catalog: String = requiredProperty("catalog")
        set(value) {
            field = value
            writeSettings()
        }

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
     * Every run enables the configuration cache and isolated projects, so a convention that breaks either fails the
     * test that exercised it. When [path] is not null, the build runs in the test JVM's environment with `PATH`
     * replaced by it, which must be passed by name.
     */
    fun run(vararg args: String, path: String? = null): BuildResult = runner(args, path).build()

    /**
     * Runs Gradle with [args] under the same flags as [run] and expects the build to fail. The optional [path] replaces
     * `PATH` as it does for [run].
     */
    fun runAndFail(vararg args: String, path: String? = null): BuildResult = runner(args, path).buildAndFail()

    /**
     * Runs Gradle with [args] like [run], in an environment whose `PATH` has no directory that contains an executable
     * named [tool].
     */
    fun runWithout(tool: String, vararg args: String): BuildResult = run(*args, path = pathWithout(tool))

    /** Like [runWithout], but expects the build to fail. */
    fun runAndFailWithout(tool: String, vararg args: String): BuildResult = runAndFail(*args, path = pathWithout(tool))

    private fun pathWithout(tool: String): String =
        System.getenv("PATH")
            .orEmpty()
            .split(File.pathSeparator)
            .filterNot { directory -> File(directory, tool).exists() }
            .joinToString(File.pathSeparator)

    private fun runner(args: Array<out String>, path: String?): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(
                *args,
                "--configuration-cache",
                "--isolated-projects",
                "--stacktrace",
                "--console=plain",
            )
            .apply { path?.let { withEnvironment(System.getenv() + ("PATH" to it)) } }

    private fun writeSettings() {
        val includes = includedProjects.joinToString(separator = "\n") { "include(\"$it\")" }
        file(
            "settings.gradle.kts",
            """
            $HEADER
            pluginManagement { includeBuild("$buildLogic") }

            plugins { id("courses.settings") }

            dependencyResolutionManagement {
                // A fixture may carry its own gradle/libs.versions.toml, which Gradle imports under the default
                // extension name. Renaming that default keeps it from colliding with the repository catalog below.
                defaultLibrariesExtensionName.set("fixtureLibs")
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
         * Creates a fixture in [projectDir] with the `courses.root` plugin on the root project, the repository's own
         * `:platform` project, and a `:lib` project that applies `courses.kotlin-library` and declares no dependencies
         * of its own.
         *
         * The shared Spotless headers, the shared detekt, Prettier, taplo and editor configuration, `.nvmrc`, and a JDK
         * 25 `.java-version` are copied or written as the real repository has them.
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
         * Creates a fixture in [projectDir] like [standard], except that `:lib` applies `courses.test-suites`, which
         * adds the `componentTest` and `integrationTest` suites and the `testFixtures` source set to the three it
         * starts with.
         *
         * `:lib` has a documented class in `src/main` (`fixture.Library`) and a documented object in `src/testFixtures`
         * (`fixture.fixtures.Samples`); no test exists, so each test adds the ones it needs with [addTest]. A `doFirst`
         * action on every `Test` task prints `FORKS <task>=<maxParallelForks>` when the task really runs.
         */
        fun testSuites(projectDir: Path): GradleFixture =
            standard(projectDir).apply {
                file(
                    "lib/build.gradle.kts",
                    """
                    $HEADER
                    plugins { id("courses.test-suites") }

                    tasks.withType<Test>().configureEach {
                        doFirst { println("FORKS ${'$'}name=${'$'}maxParallelForks") }
                    }
                    """
                        .trimIndent() + "\n",
                )
                file(
                    "lib/src/main/kotlin/fixture/Library.kt",
                    """
                    $HEADER
                    package fixture

                    /** A documented public class. */
                    class Library {
                        /** Adds [left] and [right]. */
                        fun add(left: Int, right: Int): Int = left + right
                    }
                    """
                        .trimIndent() + "\n",
                )
                file(
                    "lib/src/testFixtures/kotlin/fixture/fixtures/Samples.kt",
                    """
                    $HEADER
                    package fixture.fixtures

                    /** Sample values shared by the tests of every suite. */
                    object Samples {
                        /** Returns the sample value. */
                        fun sample(): String = listOf("sam", "ple").joinToString("")
                    }
                    """
                        .trimIndent() + "\n",
                )
            }

        /**
         * The outcomes of a task that passed, whether it ran or its result came from the build cache, which TestKit
         * shares with the user's Gradle home.
         */
        val PASSED: Set<TaskOutcome> = setOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE)

        /**
         * Creates a fixture in [projectDir] whose root project applies only `courses.governance`.
         *
         * Governance checks read plain repository files, so the fixture carries none of them; each test writes the
         * files its check reads.
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

/**
 * Writes a JUnit test class with one test to the source set of the suite [suite] of the `:lib` project, and returns the
 * absolute path of the file.
 *
 * The class lives in the package `fixture` and carries a one-line KDoc, as every test class in this repository does.
 * The test is named in backticked prose and has a block body, and it has no KDoc, as the tests of the real suites do.
 *
 * @param suite the name of the suite, which is also the name of its source set, for example `componentTest`
 * @param className the simple name of the test class
 * @param testName the prose name of the single test function
 * @param body the single line of Kotlin that makes up the test function
 * @param imports the import lines the [body] needs, without the `import` keyword
 */
fun GradleFixture.addTest(
    suite: String,
    className: String,
    testName: String,
    body: String,
    imports: List<String> = emptyList(),
): Path {
    val importLines = (listOf("org.junit.jupiter.api.Test") + imports).map { "import $it" }
    val lines =
        listOf(GradleFixture.HEADER, "package fixture", "") +
            importLines +
            listOf(
                "",
                "/** A test in the $suite suite. */",
                "class $className {",
                "    @Test",
                "    fun `$testName`() {",
                "        $body",
                "    }",
                "}",
            )
    return file("lib/src/$suite/kotlin/fixture/$className.kt", lines.joinToString("\n", postfix = "\n"))
}

/** Where [changeCatalog] writes the fixture's own copy of the version catalog, relative to the fixture root. */
private const val CATALOG_COPY = "fixture-libs.versions.toml"

/**
 * Makes the fixture use its own copy of the version catalog, rewritten by [transform], from the next run on.
 *
 * The first call copies the repository's catalog into the fixture; every call applies [transform] to the current text
 * of the copy and points the fixture settings at it. The copy is how a test changes a catalog entry, such as a
 * container image, between two runs of the same fixture without touching the real catalog.
 *
 * @param transform maps the current catalog text to the new one; it must change the text, which this function checks so
 *   that a transformation that matches nothing cannot pass a test for the wrong reason.
 */
fun GradleFixture.changeCatalog(transform: (String) -> String) {
    val current = Files.readString(Path.of(catalog))
    val changed = transform(current)
    check(changed != current) { "The catalog transformation changed nothing" }
    val copy = projectDir.resolve(CATALOG_COPY)
    Files.writeString(copy, changed)
    catalog = copy.toAbsolutePath().toString()
}
