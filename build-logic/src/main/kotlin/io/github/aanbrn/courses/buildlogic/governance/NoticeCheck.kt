// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import groovy.json.JsonException
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when `NOTICE` does not mirror the exceptions of the dependency licence policy.
 *
 * Every exception in `governance/dependency-licences.json` names one artifact as `group:name`; that exact artifact must
 * appear in `NOTICE`, so an exception can never exist without being disclosed. `NOTICE` must also contain the Axoniq
 * terms URL. An artifact counts as mentioned only as a whole name: `io.axoniq:connector-extra` does not mention
 * `io.axoniq:connector`, while sentence punctuation after the name is fine.
 */
@CacheableTask
abstract class NoticeCheck : DefaultTask() {

    /** The licence policy, `governance/dependency-licences.json`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val policy: RegularFileProperty

    /** The attribution file, `NOTICE`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val notice: RegularFileProperty

    /** The file the sorted list of missing mentions is written to; empty when `NOTICE` is complete. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Compares the exceptions of [policy] with the text of [notice] and fails with every missing mention. */
    @TaskAction
    fun check() {
        val text = notice.get().asFile.readText()
        val problems =
            (exceptionArtifacts() + TERMS_URL)
                .toSortedSet()
                .filterNot { isMentioned(it, text) }
                .map {
                    "missing from NOTICE: $it"
                }
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                problems.joinToString(
                    separator = "\n",
                    postfix = if (problems.isEmpty()) "" else "\n",
                ),
            )
        }
        if (problems.isNotEmpty()) {
            throw GradleException(failureMessage(problems))
        }
    }

    private fun exceptionArtifacts(): List<String> {
        val file = policy.get().asFile
        val root =
            try {
                JsonSlurper().parse(file)
            } catch (e: JsonException) {
                throw GradleException("${file.name} is not valid JSON: ${e.message}", e)
            }
        val exceptions = (root as? Map<*, *>)?.get("exceptions") as? List<*>
        return exceptions.orEmpty().mapNotNull { (it as? Map<*, *>)?.get("artifact")?.toString() }
    }

    private fun isMentioned(name: String, text: String): Boolean =
        Regex("(?<![A-Za-z0-9_.:/-])${Regex.escape(name)}(?![A-Za-z0-9_-])").containsMatchIn(text)

    private fun failureMessage(problems: List<String>): String = buildString {
        appendLine("NOTICE does not mirror $POLICY_PATH:")
        problems.forEach { appendLine("  $it") }
        appendLine()
        append(
            "List every exception artifact under the Axoniq Framework terms or the third-party components in " +
                "NOTICE, in the same pull request that adds the exception (D-31).",
        )
    }

    private companion object {
        const val POLICY_PATH = "governance/dependency-licences.json"
        const val TERMS_URL = "https://www.axoniq.io/legal/terms-of-service"
    }
}
