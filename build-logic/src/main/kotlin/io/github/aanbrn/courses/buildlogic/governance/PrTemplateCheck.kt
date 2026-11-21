// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import groovy.json.JsonException
import groovy.json.JsonSlurper
import java.io.File
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
 * Fails the build when the pull request template drifts from the `/gsd-ship` pull request body.
 *
 * `/gsd-ship` writes the pull request body with five core sections followed by the sections that
 * are enabled in `ship.pr_body_sections` of `.planning/config.json`. The template must start with
 * exactly those `##` headings, in that order, so that manual and bot pull requests have the same
 * structure as generated ones. Headings after them, such as the checklist, are allowed. Headings
 * inside HTML comments and fenced code blocks are not headings.
 */
@CacheableTask
abstract class PrTemplateCheck : DefaultTask() {

    /** The GSD configuration, `.planning/config.json`, that lists the configured body sections. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val config: RegularFileProperty

    /** The pull request template, `.github/PULL_REQUEST_TEMPLATE.md`. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val template: RegularFileProperty

    /** The file the list of problems is written to; empty when the template matches. */
    @get:OutputFile abstract val report: RegularFileProperty

    /** Compares the headings of [template] with the headings `/gsd-ship` produces. */
    @TaskAction
    fun check() {
        val expected = CORE_HEADINGS + configuredHeadings(config.get().asFile)
        val actual = templateHeadings(template.get().asFile.readLines())
        val problems =
            if (actual.take(expected.size) == expected) {
                emptyList()
            } else {
                expected.filterNot { it in actual }.map { "missing heading: ## $it" } +
                    "expected: ${expected.joinToString()}" +
                    "actual:   ${actual.joinToString()}"
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

    private fun failureMessage(problems: List<String>): String = buildString {
        appendLine("$TEMPLATE_PATH does not match the /gsd-ship pull request body:")
        problems.forEach { appendLine("  $it") }
        appendLine()
        append(
            "The template starts with the core headings, then every ship.pr_body_sections " +
                "heading of $CONFIG_PATH whose enabled flag is not false, in config order.",
        )
    }

    private fun configuredHeadings(file: File): List<String> {
        val root =
            try {
                JsonSlurper().parse(file)
            } catch (e: JsonException) {
                throw GradleException("${file.name} is not valid JSON: ${e.message}", e)
            }
        val sections = ((root as? Map<*, *>)?.get("ship") as? Map<*, *>)?.get("pr_body_sections")
        return (sections as? List<*>).orEmpty().mapNotNull { section ->
            val entry = section as? Map<*, *> ?: return@mapNotNull null
            val heading = entry["heading"] as? String
            when {
                entry["enabled"] == false -> null
                heading == null ->
                    throw GradleException(
                        "A ship.pr_body_sections entry in ${file.name} has no heading.",
                    )
                else -> heading.trim()
            }
        }
    }

    private fun templateHeadings(lines: List<String>): List<String> {
        val headings = mutableListOf<String>()
        var inComment = false
        var fence: String? = null
        for (line in lines) {
            val trimmed = line.trimStart()
            when {
                fence != null -> if (trimmed.startsWith(fence)) fence = null
                inComment -> inComment = COMMENT_END !in line
                trimmed.startsWith(COMMENT_START) ->
                    inComment = COMMENT_END !in trimmed.removePrefix(COMMENT_START)
                trimmed.startsWith(BACKTICK_FENCE) -> fence = BACKTICK_FENCE
                trimmed.startsWith(TILDE_FENCE) -> fence = TILDE_FENCE
                line.startsWith(HEADING_PREFIX) ->
                    headings += line.removePrefix(HEADING_PREFIX).trim()
            }
        }
        return headings
    }

    private companion object {
        const val CONFIG_PATH = ".planning/config.json"
        const val TEMPLATE_PATH = ".github/PULL_REQUEST_TEMPLATE.md"
        const val HEADING_PREFIX = "## "
        const val COMMENT_START = "<!--"
        const val COMMENT_END = "-->"
        const val BACKTICK_FENCE = "```"
        const val TILDE_FENCE = "~~~"

        /** The sections `/gsd-ship` always writes, in order. */
        val CORE_HEADINGS =
            listOf("Summary", "Changes", "Requirements Addressed", "Verification", "Key Decisions")
    }
}
