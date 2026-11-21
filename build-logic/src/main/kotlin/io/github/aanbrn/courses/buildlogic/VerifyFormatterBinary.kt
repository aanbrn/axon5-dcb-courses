// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Fails the build with a `mise install` hint when a formatter binary pinned in `mise.toml` is not
 * available on `PATH`.
 *
 * Spotless runs the binary it is pointed at, so a missing tool would otherwise surface as a raw
 * process error in the middle of a Spotless task. Checking in a separate task confines the failure
 * to the formatting tasks of that file type (configuration never fails, so Gradle invocations that
 * do not format keep working) and gives the message a single owner. The task records the binary's
 * path and `--version` output in [output]; the Spotless tasks of that type declare the file as an
 * input, so a cached result can never be reused after the tool changes.
 */
@DisableCachingByDefault(because = "reads the environment")
abstract class VerifyFormatterBinary
@Inject
constructor(private val execOperations: ExecOperations) : DefaultTask() {

    /** The name of the tool as pinned in `mise.toml`, for example `taplo`. */
    @get:Input abstract val toolName: Property<String>

    /**
     * The absolute path of the binary found on `PATH` when the build was configured, or an empty
     * string when it was not found.
     */
    @get:Input abstract val executable: Property<String>

    /** The file the verified path and version output are written to. */
    @get:OutputFile abstract val output: RegularFileProperty

    /** Runs the binary with `--version` and records the result. */
    @TaskAction
    fun verify() {
        val path = executable.get()
        val stdout = ByteArrayOutputStream()
        if (path.isEmpty() || !runsVersion(path, stdout)) {
            throw GradleException(
                "${toolName.get()} is not available on PATH, but mise.toml pins it. Run " +
                    "`mise install`, then activate mise in the shell or run the build as " +
                    "`mise x -- ./gradlew ...`.",
            )
        }
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText("$path\n${stdout.toString(Charsets.UTF_8).trim()}\n")
        }
    }

    private fun runsVersion(path: String, stdout: ByteArrayOutputStream): Boolean =
        try {
            execOperations
                .exec {
                    commandLine(path, "--version")
                    standardOutput = stdout
                    isIgnoreExitValue = true
                }
                .exitValue == 0
        } catch (_: GradleException) {
            false
        }
}
