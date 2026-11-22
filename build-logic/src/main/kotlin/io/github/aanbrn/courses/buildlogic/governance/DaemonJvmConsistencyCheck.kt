// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import java.io.File
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when the JDK major version in `.java-version` differs from the `toolchainVersion` in
 * `gradle/gradle-daemon-jvm.properties`.
 *
 * `.java-version` is the single source of the JDK version (INFRA-09) and the daemon JVM criteria are generated from it
 * by `updateDaemonJvm`, so the two must always be equal. Both inputs are declared as file collections, which tolerate a
 * missing file, so that a missing or empty file produces a message that names it rather than a generic Gradle
 * input-validation error.
 */
@CacheableTask
abstract class DaemonJvmConsistencyCheck : DefaultTask() {

    /** The `.java-version` file, which holds the JDK major version. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val javaVersionFile: ConfigurableFileCollection

    /** The generated `gradle/gradle-daemon-jvm.properties` file. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val daemonJvmProperties: ConfigurableFileCollection

    /** The file the verified JDK major version is written to. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Compares the two versions and fails with the regeneration command when they differ. */
    @TaskAction
    fun check() {
        val expected = javaMajor(existing(javaVersionFile, JAVA_VERSION_PATH))
        val actual = toolchainVersion(existing(daemonJvmProperties, PROPERTIES_PATH))
        if (expected != actual) {
            fail(
                "$JAVA_VERSION_PATH requires JDK $expected, but $PROPERTIES_PATH has " +
                    "toolchainVersion=$actual. Run `./gradlew updateDaemonJvm` to regenerate it " +
                    "from $JAVA_VERSION_PATH.",
            )
        }
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText("JDK $expected\n")
        }
    }

    private fun existing(files: ConfigurableFileCollection, path: String): File =
        files.files.singleOrNull()?.takeIf { it.isFile } ?: fail("$path is missing. $REGENERATE")

    private fun javaMajor(file: File): Int {
        val text = file.readText().trim()
        if (text.isEmpty()) {
            fail("$JAVA_VERSION_PATH is empty. $REGENERATE")
        }
        return text.substringBefore('.').toIntOrNull()
            ?: fail(
                "$JAVA_VERSION_PATH must start with a numeric JDK major version such as 25, but " + "it holds '$text'.",
            )
    }

    private fun toolchainVersion(file: File): Int {
        val value = Properties().apply { file.reader().use(::load) }.getProperty(KEY)?.trim()
        return value?.toIntOrNull() ?: fail("$PROPERTIES_PATH has no toolchainVersion with a number. $REGENERATE")
    }

    private fun fail(message: String): Nothing = throw GradleException(message)

    private companion object {
        const val JAVA_VERSION_PATH = ".java-version"
        const val PROPERTIES_PATH = "gradle/gradle-daemon-jvm.properties"
        const val KEY = "toolchainVersion"
        const val REGENERATE =
            "Run `./gradlew updateDaemonJvm` after writing the JDK major version to " + "$JAVA_VERSION_PATH."
    }
}
