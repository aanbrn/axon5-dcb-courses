// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

/**
 * Fails the build when the Node.js on `PATH` does not have the major version recorded in `.nvmrc`.
 *
 * Spotless runs Prettier with this same binary, because `courses.root` gives the one resolved path to both this task
 * and Spotless's `nodeExecutable`. A silent mismatch with `.nvmrc` would format with a runtime the repository does not
 * pin. The task writes the verified major version to [output]; the Prettier Spotless tasks declare that file as an
 * input, so a cached Spotless result can never be reused after the active Node major changes.
 */
@DisableCachingByDefault(because = "reads the environment")
abstract class VerifyNodeVersion @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {

    /** The `.nvmrc` file that holds the Node.js version the repository pins. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val nvmrc: RegularFileProperty

    /**
     * The absolute path of the Node.js binary that [ExecutableOnPath] found on the build's `PATH`, absent when there is
     * none.
     */
    @get:Input @get:Optional abstract val executable: Property<String>

    /** The file the verified Node.js major version is written to. */
    @get:OutputFile abstract val output: RegularFileProperty

    /** Compares the active Node.js major version with the one in [nvmrc] and records it. */
    @TaskAction
    fun verify() {
        val expected = parseMajor(nvmrc.get().asFile.readText())
        val actual = parseMajor(activeNodeVersion())
        if (actual != expected) {
            throw GradleException(
                "Node.js $actual is active, but .nvmrc requires $expected. Run `mise install` " +
                    "locally, or use setup-node with `node-version-file: .nvmrc` in CI.",
            )
        }
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText("$actual\n")
        }
    }

    private fun activeNodeVersion(): String {
        val stdout = ByteArrayOutputStream()
        val result =
            try {
                executable.orNull?.let { node ->
                    execOperations.exec {
                        commandLine(node, "--version")
                        standardOutput = stdout
                        isIgnoreExitValue = true
                    }
                }
            } catch (e: GradleException) {
                throw missingNode(e)
            }
        if (result == null || result.exitValue != 0) {
            throw missingNode(null)
        }
        return stdout.toString(Charsets.UTF_8)
    }

    private fun missingNode(cause: Throwable?): GradleException =
        GradleException(
            "Node.js is not available on PATH, but .nvmrc requires it. Run `mise install` " +
                "locally, or use setup-node with `node-version-file: .nvmrc` in CI.",
            cause,
        )

    private fun parseMajor(version: String): Int {
        val text = version.trim().removePrefix("v")
        return text.substringBefore('.').toIntOrNull()
            ?: throw GradleException(
                "Cannot read a Node.js major version from '$text'. .nvmrc must start with a " +
                    "number such as 24 or 24.22.3.",
            )
    }
}
