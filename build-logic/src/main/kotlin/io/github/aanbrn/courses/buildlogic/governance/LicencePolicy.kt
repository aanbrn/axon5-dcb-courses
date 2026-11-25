// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import groovy.json.JsonException
import groovy.json.JsonSlurper
import java.io.File
import org.gradle.api.GradleException

/**
 * One licence entry of an SBOM component: the text found in the SBOM and whether it came from an SPDX `expression`
 * (which may combine identifiers) or from an `id` or a free-text `name` (which is always one licence).
 */
internal data class LicenceEntry(val text: String, val isExpression: Boolean)

/**
 * A component of an SBOM with every licence entry it lists.
 *
 * [isProject] is true for a project dependency of the same build: it has no POM and so no licence entry, and its
 * licence is the repository's own `LICENSE`.
 */
internal data class LicencedComponent(
    val group: String,
    val name: String,
    val version: String,
    val entries: List<LicenceEntry>,
    val isProject: Boolean,
) {
    /** `group:name`, the form per-artifact exceptions use. */
    val artifact: String = if (group.isEmpty()) name else "$group:$name"

    /** `group:name:version`, the form problems are reported and sorted by. */
    val coordinates: String = "$artifact:$version"
}

/**
 * A justified exception for one artifact: [artifact] (`group:name`, never a pattern) may use the licence whose text is
 * exactly [license] because of [reason].
 */
internal data class LicenceException(val artifact: String, val license: String, val reason: String)

/**
 * The licence policy of `governance/dependency-licences.json` and the rules that apply it to SBOM components.
 *
 * A component passes when any of its entries is acceptable: an entry is acceptable when an exception covers its exact
 * text for this artifact, or when its licence expression is satisfied by identifiers the policy allows. Names are
 * resolved through the aliases first. Everything else fails closed: a component without entries, an unknown name, and
 * forbidden licences without an acceptable alternative.
 *
 * Project dependencies of the same build are not judged (see [LicencedComponent.isProject]).
 */
internal class LicencePolicy(
    private val allowed: Set<String>,
    private val forbidden: List<Regex>,
    private val aliases: Map<String, String>,
    private val exceptions: List<LicenceException>,
) {
    /** The problem with [component], or null when it passes. */
    fun problemWith(component: LicencedComponent): String? =
        when {
            component.isProject -> null
            component.entries.isEmpty() -> "${component.coordinates}: no licence entry in the SBOM"
            component.entries.any { isAcceptable(component, it) } -> null
            else ->
                "${component.coordinates}: no acceptable licence (${component.entries.joinToString("; ") { describe(it) }})"
        }

    private fun isAcceptable(component: LicencedComponent, entry: LicenceEntry): Boolean =
        exceptions.any { it.artifact == component.artifact && it.license == entry.text } ||
            expressionOf(entry).isSatisfiedBy(::isAllowed)

    private fun expressionOf(entry: LicenceEntry): LicenceExpression =
        if (entry.isExpression) LicenceExpression.parse(entry.text) else LicenceExpression.Atom(entry.text)

    private fun isAllowed(text: String): Boolean = resolve(text) in allowed

    private fun resolve(text: String): String = aliases[text] ?: text

    private fun describe(entry: LicenceEntry): String {
        val unacceptable =
            expressionOf(entry).atoms().filterNot(::isAllowed).distinct().map { "'$it' is ${classify(it)}" }
        return unacceptable.joinToString(", ")
    }

    private fun classify(text: String): String =
        if (forbidden.any { it.matches(resolve(text)) }) "forbidden" else "not in the policy"

    /** Reads and validates policy files. */
    companion object {
        /**
         * Reads the policy at [file].
         *
         * @throws GradleException when the file is not valid JSON or breaks a rule of the policy format: an exception
         *   without a reason, an exception for a pattern instead of one artifact, an alias that resolves to a forbidden
         *   licence, or an allowed identifier that matches a forbidden family. The gate is membership of `allowed`, so
         *   a policy must not list an identifier that its own `forbidden` patterns refuse.
         */
        fun read(file: File): LicencePolicy {
            val root = parse(file) as? Map<*, *> ?: throw GradleException("${file.name} must hold a JSON object")
            val allowed = strings(root, "allowed")
            val forbidden = strings(root, "forbidden").map(::globToRegex)
            val aliases = (root["aliases"] as? Map<*, *>).orEmpty().entries.associate { "${it.key}" to "${it.value}" }
            val exceptions = (root["exceptions"] as? List<*>).orEmpty().map(::exceptionOf)
            val problems = formatProblems(allowed, forbidden, aliases, exceptions)
            if (problems.isNotEmpty()) {
                throw GradleException("${file.name} is invalid:\n" + problems.joinToString("\n") { "  $it" })
            }
            return LicencePolicy(allowed.toSet(), forbidden, aliases, exceptions)
        }

        private fun parse(file: File): Any? =
            try {
                JsonSlurper().parse(file)
            } catch (e: JsonException) {
                throw GradleException("${file.name} is not valid JSON: ${e.message}", e)
            }

        private fun strings(root: Map<*, *>, key: String): List<String> =
            (root[key] as? List<*>).orEmpty().map { "$it" }

        private fun exceptionOf(entry: Any?): LicenceException {
            val map = entry as? Map<*, *> ?: emptyMap<Any?, Any?>()
            return LicenceException(
                artifact = "${map["artifact"] ?: ""}",
                license = "${map["license"] ?: ""}",
                reason = "${map["reason"] ?: ""}".trim(),
            )
        }

        private fun formatProblems(
            allowed: List<String>,
            forbidden: List<Regex>,
            aliases: Map<String, String>,
            exceptions: List<LicenceException>,
        ): List<String> = buildList {
            aliases
                .filterValues { target -> forbidden.any { it.matches(target) } }
                .forEach { (name, target) -> add("alias '$name' resolves to the forbidden licence '$target'") }
            allowed
                .filter { id -> forbidden.any { it.matches(id) } }
                .forEach { id -> add("'$id' is allowed, but matches a forbidden family") }
            exceptions.forEach { addAll(exceptionProblems(it)) }
        }

        private fun exceptionProblems(exception: LicenceException): List<String> = buildList {
            val label = exception.artifact.ifEmpty { "<no artifact>" }
            if (exception.artifact.count { it == ':' } != 1 || exception.artifact.any { it == '*' || it == '?' }) {
                add("exception '$label' must name exactly one artifact as group:name, without a wildcard")
            }
            if (exception.license.isBlank()) add("exception '$label' needs the verbatim licence text")
            if (exception.reason.isEmpty()) add("exception '$label' needs a written reason")
        }

        private fun globToRegex(glob: String): Regex = Regex(glob.split('*').joinToString(".*") { Regex.escape(it) })
    }
}
