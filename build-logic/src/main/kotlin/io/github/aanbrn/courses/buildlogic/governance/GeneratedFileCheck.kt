// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when a committed, generated file differs from the text its generator would write now.
 *
 * The check is generic on purpose: it knows the [expected] text, the committed [actual] file and the [fixCommand] that
 * regenerates it, and nothing about what the file is. `checkComposeVersions` uses it for `compose/versions.env`
 * (INFRA-09), and the `.env.example` check (INFRA-17) is a second instance.
 *
 * The comparison is byte for byte, so a committed file that a formatter reflowed is stale as well. A missing [actual]
 * file fails Gradle's own input validation, which names the file.
 */
@CacheableTask
abstract class GeneratedFileCheck @Inject constructor(private val layout: ProjectLayout) : DefaultTask() {

    /** The text the generator writes now. */
    @get:Input abstract val expected: Property<String>

    /** The committed generated file. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val actual: RegularFileProperty

    /** The command that regenerates [actual], quoted in the failure message. */
    @get:Input abstract val fixCommand: Property<String>

    /** The file the failure message is written to; empty when the file is fresh. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Compares [actual] with [expected] and fails with the fix command when they differ. */
    @TaskAction
    fun check() {
        val file = actual.get().asFile
        val message =
            if (file.readText() == expected.get()) {
                ""
            } else {
                val path = layout.projectDirectory.asFile.toPath().relativize(file.toPath())
                "${path.joinToString("/")} is stale; run ${fixCommand.get()}"
            }
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(if (message.isEmpty()) "" else "$message\n")
        }
        if (message.isNotEmpty()) {
            throw GradleException(message)
        }
    }
}
