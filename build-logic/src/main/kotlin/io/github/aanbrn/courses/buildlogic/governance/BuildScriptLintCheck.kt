// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when a build script or build-logic source holds a version literal or uses an
 * eager or cross-project Gradle API.
 *
 * Versions live in the version catalog and `.java-version` only (INFRA-09), and the build must stay
 * compatible with lazy task configuration, the configuration cache and isolated projects
 * (INFRA-10). Each finding is reported as `path:line: [rule] source line`, sorted by path and then
 * line number, so the report is identical on every run.
 */
@CacheableTask
abstract class BuildScriptLintCheck : DefaultTask() {

    /** The build scripts, `gradle.properties` and build-logic sources to scan. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val sources: ConfigurableFileCollection

    /** The file the sorted findings are written to; empty when there are none. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Scans every file in [sources] and fails with all findings. */
    @TaskAction
    fun check() {
        val findings = mutableListOf<Finding>()
        sources.asFileTree.visit {
            if (!isDirectory) {
                val path = relativePath.pathString
                val rules = if (file.name == PROPERTIES_FILE) PROPERTY_RULES else SOURCE_RULES
                findings += scan(path, file.readLines(), rules)
            }
        }
        val sorted = findings.sortedWith(compareBy({ it.path }, { it.line }, { it.rule }))
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(sorted.joinToString(separator = "") { it.render() + "\n" })
        }
        if (sorted.isNotEmpty()) {
            throw GradleException(failureMessage(sorted))
        }
    }

    private fun scan(path: String, lines: List<String>, rules: List<Rule>): List<Finding> =
        lines.withIndex().flatMap { (index, raw) ->
            val text = raw.trim()
            if (isComment(text)) {
                emptyList()
            } else {
                rules
                    .filter { it.pattern.containsMatchIn(text) }
                    .map { Finding(path, index + 1, it.name, text) }
            }
        }

    private fun isComment(trimmed: String): Boolean =
        trimmed.isEmpty() || COMMENT_PREFIXES.any { trimmed.startsWith(it) }

    private fun failureMessage(findings: List<Finding>): String = buildString {
        appendLine("${findings.size} build script governance finding(s):")
        findings.forEach { appendLine("  ${it.render()}") }
        appendLine()
        appendLine(
            "Versions belong in gradle/libs.versions.toml, and the JDK major version in " +
                ".java-version (INFRA-09).",
        )
        append(
            "Use lazy task APIs (register, named, configureEach) and configure each project " +
                "from its own build script or a convention plugin (INFRA-10).",
        )
    }

    /** A rule that flags every line matching [pattern] under the name [name]. */
    private class Rule(val name: String, val pattern: Regex)

    /** One violation: [rule] matched [text] on [line] of the file at [path]. */
    private class Finding(val path: String, val line: Int, val rule: String, val text: String) {
        fun render(): String = "$path:$line: [$rule] $text"
    }

    private companion object {
        const val PROPERTIES_FILE = "gradle.properties"
        val COMMENT_PREFIXES = listOf("//", "/*", "*", "#")

        val SOURCE_RULES =
            listOf(
                Rule("version-literal", Regex("\"[A-Za-z][\\w.-]*:[A-Za-z][\\w.-]*:\\d")),
                Rule("version-literal", Regex("\\bversion\\s*=\\s*\"\\d")),
                Rule("version-literal", Regex("\\bversion\\(\\s*\"\\d")),
                Rule("version-literal", Regex("\\)\\s+version\\s+\"")),
                Rule("version-literal", Regex("\\bjvmToolchain\\(\\s*\\d")),
                Rule("version-literal", Regex("\\bJavaLanguageVersion\\.of\\(\\s*\\d")),
                Rule("eager-api", Regex("\\btasks\\.(create|getByName|getByPath|findByName)\\(")),
                Rule("eager-api", Regex("\\btasks\\.withType<[^>]+>\\s*\\{")),
                Rule("eager-api", Regex("\\btasks\\.withType\\([^)]*\\)\\s*\\{")),
                Rule("cross-project", Regex("\\ballprojects\\s*\\{")),
                Rule("cross-project", Regex("\\bsubprojects\\s*\\{")),
                Rule("cross-project", Regex("\\bafterEvaluate\\b")),
            )

        val PROPERTY_RULES =
            listOf(Rule("version-literal", Regex("^[\\w.-]*[vV]ersion\\s*[=:]\\s*\\d")))
    }
}
