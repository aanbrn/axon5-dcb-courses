// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture

/** The ask rule for GSD `config-set` calls that `checkClaudeAskRules` always expects. */
internal const val CONFIG_SET_RULE = "Bash(*config-set*)"

/** The script of the hook for the file tools, relative to the repository root. */
internal const val EDIT_HOOK = "scripts/governance/ask-governed-edit.sh"

/** The script of the hook for Bash, relative to the repository root. */
internal const val MERGE_HOOK = "scripts/governance/ask-governed-merge.sh"

/** The matcher of the hook for the file tools. */
internal const val EDIT_MATCHER = "Edit|Write|MultiEdit|NotebookEdit"

/** The governed entry that covers the hook scripts. */
internal const val HOOK_SCRIPTS_ENTRY = "scripts/governance/**"

/** The ask rule for [HOOK_SCRIPTS_ENTRY]. */
internal const val HOOK_SCRIPTS_RULE = "Edit(/$HOOK_SCRIPTS_ENTRY)"

/** The `timeout` of a hook handler in the fixtures; the check accepts any. */
private const val HOOK_TIMEOUT_SECONDS = 30

/** A hook handler for [script] as `checkClaudeAskRules` expects it. A test changes one field to break it. */
internal fun hookHandler(script: String): Map<String, Any?> =
    linkedMapOf(
        "type" to "command",
        "command" to "bash",
        "args" to listOf("\${CLAUDE_PROJECT_DIR}/$script"),
        "timeout" to HOOK_TIMEOUT_SECONDS,
        "onFailure" to "block",
    )

/** A `PreToolUse` matcher group with the given [handlers]. */
internal fun hookGroup(matcher: String, vararg handlers: Map<String, Any?>): Map<String, Any?> =
    linkedMapOf("matcher" to matcher, "hooks" to handlers.toList())

/** The `hooks` setting with the given `PreToolUse` [groups]; by default both governance hooks as expected. */
internal fun hooksSetting(
    vararg groups: Map<String, Any?> =
        arrayOf(hookGroup(EDIT_MATCHER, hookHandler(EDIT_HOOK)), hookGroup("Bash", hookHandler(MERGE_HOOK))),
): Map<String, Any?> = linkedMapOf("PreToolUse" to groups.toList())

/**
 * The text of a `.claude/settings.json` with [rules] in `permissions.ask`, the [hooks] setting unless it is null, and
 * the [extra] top-level settings.
 */
internal fun claudeSettings(
    rules: List<String>,
    hooks: Any? = hooksSetting(),
    extra: Map<String, Any?> = emptyMap(),
): String {
    val settings = linkedMapOf<String, Any?>("permissions" to mapOf("ask" to rules))
    if (hooks != null) {
        settings["hooks"] = hooks
    }
    settings.putAll(extra)
    return json(settings) + "\n"
}

/** The ask rules for the governed [entries]: one `Edit(/<entry>)` rule each, plus [CONFIG_SET_RULE]. */
internal fun askRules(entries: List<String>): List<String> = entries.map { "Edit(/$it)" } + CONFIG_SET_RULE

/** Writes both governance hook scripts, which `checkClaudeAskRules` requires to exist. */
internal fun GradleFixture.writeHookScripts() {
    listOf(EDIT_HOOK, MERGE_HOOK).forEach { file(it, "#!/usr/bin/env bash\nexit 0\n") }
}

/** Renders maps, lists, strings, numbers, booleans and null as JSON. */
internal fun json(value: Any?): String =
    when (value) {
        null -> "null"
        is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { "${json(it.key)}: ${json(it.value)}" }
        is List<*> -> value.joinToString(prefix = "[", postfix = "]") { json(it) }
        else -> value.toString()
    }
