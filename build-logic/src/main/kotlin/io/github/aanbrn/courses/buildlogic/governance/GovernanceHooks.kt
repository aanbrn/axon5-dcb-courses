// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import java.io.File

/**
 * The Claude Code hooks that guard the governed paths in every worktree of the repository (ADR-0015), and the check
 * that `.claude/settings.json` registers them.
 *
 * The project ask rules `Edit(/<path>)` anchor at the directory a session starts in, so they never match a file in the
 * worktree of a subagent. `ask-governed-edit.sh` asks the author before a file tool changes a governed path in the main
 * checkout or in a worktree that is not an agent worktree, while inside an agent worktree the merge hook asks once when
 * the work is merged. `ask-governed-merge.sh` asks before a Bash command merges changes to governed paths. Each hook
 * must be registered exactly once under `hooks.PreToolUse`, with its matcher, run through `bash` with the script under
 * `${CLAUDE_PROJECT_DIR}`, with `onFailure` set to `"block"` so that a hook that cannot start or times out blocks the
 * call, and without an `if` filter or `async`, which would narrow it or discard its decision. `disableAllHooks` must
 * not be true. Further hooks are allowed: when hooks disagree, Claude Code lets `ask` win over `allow`.
 */
internal object GovernanceHooks {
    /**
     * A governance hook.
     *
     * @property matcher the `matcher` of its `PreToolUse` group, the tools it sees.
     * @property script the script, relative to the repository root.
     */
    data class Hook(val matcher: String, val script: String) {
        /** The `args` of its registration: the script under the project directory Claude Code substitutes. */
        val args: List<String>
            get() = listOf("$PROJECT_DIR/$script")
    }

    /** The hooks `.claude/settings.json` must register. */
    val HOOKS: List<Hook> =
        listOf(
            Hook("Edit|Write|MultiEdit|NotebookEdit", "scripts/governance/ask-governed-edit.sh"),
            Hook("Bash", "scripts/governance/ask-governed-merge.sh"),
        )

    private const val PROJECT_DIR = $$"${CLAUDE_PROJECT_DIR}"
    private const val COMMAND = "bash"
    private const val ON_FAILURE = "block"

    /**
     * Lists the problems with the hooks: their registration in [settings], the parsed `.claude/settings.json`, and
     * their scripts under the repository [root], each of which must be one of the [governed] paths. The list is empty
     * when every hook is in place.
     */
    fun problems(settings: Map<*, *>, governed: GovernedPaths, root: File): List<String> = buildList {
        if (settings["disableAllHooks"] == true) {
            add("disableAllHooks is true, which switches the governance hooks off")
        }
        val groups = (settings["hooks"] as? Map<*, *>)?.get("PreToolUse")
        if (groups !is List<*>) {
            add("hooks.PreToolUse is missing or not a list")
        }
        HOOKS.forEach { hook ->
            if (groups is List<*>) {
                addAll(registrationProblems(hook, groups))
            }
            if (!root.resolve(hook.script).isFile) {
                add("hook script ${hook.script} does not exist")
            }
            if (!governed.governs(hook.script)) {
                add("hook script ${hook.script} is not a governed path")
            }
        }
    }

    private fun registrationProblems(hook: Hook, groups: List<*>): List<String> {
        val registrations =
            groups.filterIsInstance<Map<*, *>>().flatMap { group ->
                handlers(group).filter { refersTo(it, hook) }.map { group to it }
            }
        return when (registrations.size) {
            0 -> listOf("missing hook: ${hook.script}")
            1 -> registrations.single().let { (group, handler) -> handlerProblems(hook, group, handler) }
            else -> listOf("duplicated hook: ${hook.script}")
        }
    }

    private fun handlers(group: Map<*, *>): List<Map<*, *>> =
        (group["hooks"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()

    private fun refersTo(handler: Map<*, *>, hook: Hook): Boolean =
        (listOf(handler["command"]) + (handler["args"] as? List<*>).orEmpty()).any {
            (it as? String)?.contains(hook.script) == true
        }

    private fun handlerProblems(hook: Hook, group: Map<*, *>, handler: Map<*, *>): List<String> {
        val issues = buildList {
            if (group["matcher"] != hook.matcher) {
                add("matcher is ${group["matcher"]}, expected ${hook.matcher}")
            }
            if (handler["type"] != "command") {
                add("type is ${handler["type"]}, expected command")
            }
            if (handler["command"] != COMMAND || handler["args"] != hook.args) {
                add("must run as command $COMMAND with args ${hook.args}")
            }
            if (handler["onFailure"] != ON_FAILURE) {
                add("onFailure is ${handler["onFailure"]}, expected $ON_FAILURE")
            }
            if ("if" in handler) {
                add("sets if, which narrows the calls it sees")
            }
            if (handler["async"] == true || handler["asyncRewake"] == true) {
                add("runs in the background, so its decision is discarded")
            }
        }
        return issues.map { "hook ${hook.script}: $it" }
    }
}
