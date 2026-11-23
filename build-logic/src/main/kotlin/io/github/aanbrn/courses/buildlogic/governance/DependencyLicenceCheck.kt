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
 * Fails the build when a component of a module's CycloneDX SBOM has a licence the policy does not accept.
 *
 * The policy `governance/dependency-licences.json` lists the allowed SPDX identifiers, the forbidden families, the
 * aliases that map the free-text names found in POMs to identifiers, and per-artifact exceptions that each carry a
 * written reason. A component passes when any of its licence alternatives is allowed or an exception covers it. A
 * component without a licence entry, with a name the policy does not know, or with only forbidden alternatives fails,
 * and the failure names the coordinates and the licence text. Problems are sorted by `group:name:version`, so two runs
 * report the same text.
 *
 * The SBOM format is the only source-specific part. A second source, such as the npm dependencies of the frontend,
 * joins by producing a CycloneDX document and registering another instance of this task against the same policy.
 */
@CacheableTask
abstract class DependencyLicenceCheck : DefaultTask() {

    /** The CycloneDX SBOM of one module in JSON form. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val bom: RegularFileProperty

    /** The licence policy, `governance/dependency-licences.json`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val policy: RegularFileProperty

    /** The file the sorted list of problems is written to; empty when every component passes. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Checks every component of [bom] against [policy] and fails with all problems. */
    @TaskAction
    fun check() {
        val licencePolicy = LicencePolicy.read(policy.get().asFile)
        val problems = components().sortedBy { it.coordinates }.mapNotNull { licencePolicy.problemWith(it) }
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

    private fun components(): List<LicencedComponent> {
        val file = bom.get().asFile
        val document =
            try {
                JsonSlurper().parse(file)
            } catch (e: JsonException) {
                throw GradleException("${file.name} is not valid JSON: ${e.message}", e)
            }
        val components = (document as? Map<*, *>)?.get("components") as? List<*>
        return components.orEmpty().filterIsInstance<Map<*, *>>().map(::componentOf)
    }

    private fun componentOf(component: Map<*, *>): LicencedComponent =
        LicencedComponent(
            group = "${component["group"] ?: ""}",
            name = "${component["name"] ?: ""}",
            version = "${component["version"] ?: ""}",
            entries = (component["licenses"] as? List<*>).orEmpty().mapNotNull(::entryOf),
            isProject = "${component["purl"] ?: ""}".contains(PROJECT_QUALIFIER),
        )

    // Each entry is `{license: {id}}`, `{license: {name, url}}` or `{expression}`; several entries are alternatives.
    private fun entryOf(entry: Any?): LicenceEntry? {
        val map = entry as? Map<*, *> ?: return null
        val licence = map["license"] as? Map<*, *>
        return when {
            licence != null -> (licence["id"] ?: licence["name"])?.let { LicenceEntry("$it", isExpression = false) }
            else -> map["expression"]?.let { LicenceEntry("$it", isExpression = true) }
        }
    }

    private fun failureMessage(problems: List<String>): String = buildString {
        appendLine("${problems.size} dependency licence problem(s) in ${bom.get().asFile.path}:")
        problems.forEach { appendLine("  $it") }
        appendLine()
        append(
            "An unknown licence name is resolved by an alias, a justified exception for exactly one artifact, or " +
                "by replacing the dependency. Aliases and exceptions live in $POLICY_PATH, a governed path (D-31).",
        )
    }

    private companion object {
        const val POLICY_PATH = "governance/dependency-licences.json"

        // The purl qualifier the SBOM plugin adds to project dependencies of the same build and to nothing else.
        const val PROJECT_QUALIFIER = "?project_path="
    }
}
