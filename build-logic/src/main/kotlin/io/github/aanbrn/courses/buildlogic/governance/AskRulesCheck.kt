// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import groovy.json.JsonException
import groovy.json.JsonSlurper
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when the Claude Code ask rules or the governance hooks drift from the governed paths.
 *
 * `.claude/settings.json` must carry, in `permissions.ask`, exactly one `Edit(/<path>)` rule for every entry of
 * `governance/governed-paths.txt`, parsed by `GovernedPaths`, and one `Bash(*config-set*)` rule, so the agent has to
 * ask the author before it edits a governance file or changes the GSD configuration. The order of the rules does not
 * matter and further rules are allowed.
 *
 * The ask rules anchor at the directory a session starts in and miss the worktrees of subagents, so the settings must
 * also register the governance hooks that `GovernanceHooks` defines, and their scripts must exist and be governed paths
 * themselves (ADR-0015). Ask rules and hooks are a convenience for the author, not a security boundary: CI
 * (`checkGsdConfigPolicy` and the governance workflow) stays the enforcement.
 */
@CacheableTask
abstract class AskRulesCheck : DefaultTask() {

    /** The Claude Code project settings, `.claude/settings.json`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val claudeSettings: RegularFileProperty

    /** The single list of governed path globs, `governance/governed-paths.txt`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val governedPaths: RegularFileProperty

    /**
     * The scripts of the governance hooks, resolved against the directory that holds `.claude/`, which is the project
     * directory Claude Code substitutes for `${CLAUDE_PROJECT_DIR}`. They are derived from [claudeSettings] and are
     * inputs only so that a deleted or changed script runs the check again.
     */
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) abstract val hookScripts: ConfigurableFileCollection

    /** The file the sorted list of problems is written to; empty when the rules and hooks are in sync. */
    @get:OutputFile abstract val report: RegularFileProperty

    init {
        hookScripts.from(
            claudeSettings.map { settings ->
                GovernanceHooks.HOOKS.map { projectDirectory(settings.asFile).resolve(it.script) }
            },
        )
    }

    /** Compares the ask rules and hooks of [claudeSettings] with the paths listed in [governedPaths]. */
    @TaskAction
    fun check() {
        val governed = GovernedPaths.parse(governedPaths.get().asFile.readText())
        val problems =
            when {
                governed.problems.isNotEmpty() -> governed.problems.map { "$GOVERNED_PATH $it" }
                governed.entries.isEmpty() -> listOf(NO_GOVERNED_PATHS)
                else -> problems(governed)
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
        appendLine("$SETTINGS_PATH does not match $GOVERNED_PATH:")
        problems.forEach { appendLine("  $it") }
        appendLine()
        append(
            "Every governed path needs exactly one Edit(/<path>) rule in permissions.ask, plus " +
                "$CONFIG_SET_RULE, and hooks.PreToolUse must register each governance hook once (ADR-0015). " +
                "$SETTINGS_PATH is itself a governed path.",
        )
    }

    private fun problems(governed: GovernedPaths): List<String> {
        val file = claudeSettings.get().asFile
        val settings = parse(file)
        return (askProblems(governed.entries, settings) +
                GovernanceHooks.problems(settings, governed, projectDirectory(file)))
            .sorted()
    }

    private fun askProblems(governed: List<String>, settings: Map<*, *>): List<String> {
        val ask = (settings["permissions"] as? Map<*, *>)?.get("ask")
        return when {
            ask == null || (ask is List<*> && ask.isEmpty()) -> listOf(NO_ASK_RULES)
            ask !is List<*> -> listOf(NOT_A_LIST)
            else -> ruleProblems(governed, ask)
        }
    }

    private fun ruleProblems(governed: List<String>, rules: List<*>): List<String> {
        val expected = governed.map { "Edit(/$it)" }.distinct() + CONFIG_SET_RULE
        val counts = rules.filterIsInstance<String>().groupingBy { it }.eachCount()
        return buildList {
            rules.filterNot { it is String }.mapTo(this) { "$NOT_A_STRING $it" }
            expected.filter { it !in counts }.mapTo(this) { "missing: $it" }
            expected.filter { (counts[it] ?: 0) > 1 }.mapTo(this) { "duplicated: $it" }
        }
    }

    private fun parse(file: File): Map<*, *> {
        val parsed =
            try {
                JsonSlurper().parse(file)
            } catch (e: JsonException) {
                throw GradleException("${file.name} is not valid JSON: ${e.message}", e)
            }
        return parsed as? Map<*, *> ?: throw GradleException("${file.name} is not a JSON object")
    }

    private companion object {
        const val SETTINGS_PATH = ".claude/settings.json"
        const val GOVERNED_PATH = "governance/governed-paths.txt"
        const val CONFIG_SET_RULE = "Bash(*config-set*)"
        const val NO_GOVERNED_PATHS = "$GOVERNED_PATH lists no governed path"
        const val NO_ASK_RULES = "permissions.ask is missing or empty"
        const val NOT_A_LIST = "permissions.ask is not a list"
        const val NOT_A_STRING = "permissions.ask contains a value that is not a string:"

        /** The directory that holds `.claude/` with the [settings] file in it. */
        fun projectDirectory(settings: File): File = settings.absoluteFile.parentFile.parentFile
    }
}
