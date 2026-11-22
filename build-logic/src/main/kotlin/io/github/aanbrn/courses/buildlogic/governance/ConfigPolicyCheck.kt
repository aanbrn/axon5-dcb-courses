// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import groovy.json.JsonException
import groovy.json.JsonSlurper
import java.io.File
import java.math.BigDecimal
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
 * Fails the build when `.planning/config.json` is not semantically equal to the committed policy.
 *
 * GSD workflows and agents can rewrite the project settings (model overrides, branching, tagging). The policy file is
 * an exact copy of the accepted configuration, so any added, removed or changed key is a visible, reviewable change and
 * never a silent one. Key order and whitespace do not matter. Only key paths are reported, never values, because a
 * configuration file can hold credentials.
 */
@CacheableTask
abstract class ConfigPolicyCheck : DefaultTask() {

    /** The live GSD configuration, `.planning/config.json`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val config: RegularFileProperty

    /** The committed policy, an exact copy of the accepted configuration. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val policy: RegularFileProperty

    /** The file the sorted list of deviations is written to; empty when there are none. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Compares [config] with [policy] and fails with every deviating key path. */
    @TaskAction
    fun check() {
        val deviations = mutableListOf<String>()
        compare("", parse(policy.get().asFile), parse(config.get().asFile), deviations)
        deviations.sort()
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                deviations.joinToString(
                    separator = "\n",
                    postfix = if (deviations.isEmpty()) "" else "\n",
                ),
            )
        }
        if (deviations.isNotEmpty()) {
            throw GradleException(failureMessage(deviations))
        }
    }

    private fun failureMessage(deviations: List<String>): String = buildString {
        appendLine("$CONFIG_PATH deviates from the committed policy $POLICY_PATH:")
        deviations.forEach { appendLine("  $it") }
        appendLine()
        appendLine(
            "Transient GSD runtime keys (for example workflow._auto_chain_active) must be " +
                "reset before committing.",
        )
        append(
            "To change the policy deliberately, update $POLICY_PATH in the same pull request " +
                "and record the decision (D-14); the policy file is a governed path.",
        )
    }

    private fun parse(file: File): Any? =
        try {
            JsonSlurper().parse(file)
        } catch (e: JsonException) {
            throw GradleException("${file.name} is not valid JSON: ${e.message}", e)
        }

    private fun compare(path: String, expected: Any?, actual: Any?, out: MutableList<String>) {
        when {
            expected is Map<*, *> && actual is Map<*, *> -> compareMaps(path, expected, actual, out)
            expected is List<*> && actual is List<*> -> compareLists(path, expected, actual, out)
            !sameScalar(expected, actual) -> out += "changed: ${label(path)}"
        }
    }

    private fun compareMaps(
        path: String,
        expected: Map<*, *>,
        actual: Map<*, *>,
        out: MutableList<String>,
    ) {
        for (key in expected.keys + actual.keys) {
            val child = if (path.isEmpty()) "$key" else "$path.$key"
            when (key) {
                !in actual -> out += "removed: $child"
                !in expected -> out += "added: $child"
                else -> compare(child, expected[key], actual[key], out)
            }
        }
    }

    private fun compareLists(
        path: String,
        expected: List<*>,
        actual: List<*>,
        out: MutableList<String>,
    ) {
        for (index in 0 until maxOf(expected.size, actual.size)) {
            val child = "$path[$index]"
            when {
                index >= actual.size -> out += "removed: $child"
                index >= expected.size -> out += "added: $child"
                else -> compare(child, expected[index], actual[index], out)
            }
        }
    }

    private fun sameScalar(expected: Any?, actual: Any?): Boolean =
        if (expected is Number && actual is Number) {
            BigDecimal(expected.toString()).compareTo(BigDecimal(actual.toString())) == 0
        } else {
            expected == actual
        }

    private fun label(path: String): String = path.ifEmpty { "<root>" }

    private companion object {
        const val CONFIG_PATH = ".planning/config.json"
        const val POLICY_PATH = "governance/gsd-config-policy.json"
    }
}
