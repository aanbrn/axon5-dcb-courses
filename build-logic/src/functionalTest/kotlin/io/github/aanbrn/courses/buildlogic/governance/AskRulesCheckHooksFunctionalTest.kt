// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic.governance

import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Proves `checkClaudeAskRules` requires `.claude/settings.json` to register the governance hooks of ADR-0015 exactly
 * once each, in the form that makes them decide every call, and their scripts to exist as governed paths.
 */
class AskRulesCheckHooksFunctionalTest {
    @TempDir lateinit var projectDir: Path

    private fun fixture(
        hooks: Any? = hooksSetting(),
        extra: Map<String, Any?> = emptyMap(),
        entries: List<String> = ENTRIES,
        scripts: Boolean = true,
    ): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file(".claude/settings.json", claudeSettings(askRules(entries), hooks, extra))
            file("governance/governed-paths.txt", entries.joinToString("\n", postfix = "\n"))
            if (scripts) {
                writeHookScripts()
            }
        }

    @Test
    fun `both hooks registered as expected pass`() {
        val result = fixture().run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `further hooks and further groups are allowed`() {
        val lint = mapOf("type" to "command", "command" to "./lint.sh")
        val hooks =
            hooksSetting(
                hookGroup(EDIT_MATCHER, hookHandler(EDIT_HOOK), lint),
                hookGroup("Bash", hookHandler(MERGE_HOOK)),
                hookGroup("Read", lint),
            ) + ("PostToolUse" to listOf(hookGroup("Edit", lint)))

        val result = fixture(hooks).run(TASK)

        result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
    }

    @Test
    fun `settings without hooks fail naming the missing setting`() {
        val output = fixture(hooks = null).runAndFail(TASK).output

        output shouldContain "hooks.PreToolUse is missing or not a list"
    }

    @Test
    fun `PreToolUse that is not a list fails`() {
        val output = fixture(mapOf("PreToolUse" to "Edit")).runAndFail(TASK).output

        output shouldContain "hooks.PreToolUse is missing or not a list"
    }

    @Test
    fun `a missing hook fails naming its script`() {
        val output = fixture(hooksSetting(hookGroup("Bash", hookHandler(MERGE_HOOK)))).runAndFail(TASK).output

        output shouldContain "missing hook: $EDIT_HOOK"
        output shouldNotContain "missing hook: $MERGE_HOOK"
    }

    @Test
    fun `a hook registered twice fails naming its script`() {
        val hooks =
            hooksSetting(
                hookGroup(EDIT_MATCHER, hookHandler(EDIT_HOOK)),
                hookGroup("Bash", hookHandler(MERGE_HOOK)),
                hookGroup("Write", hookHandler(EDIT_HOOK)),
            )

        val output = fixture(hooks).runAndFail(TASK).output

        output shouldContain "duplicated hook: $EDIT_HOOK"
    }

    @Test
    fun `a hook under another matcher fails naming both matchers`() {
        val hooks =
            hooksSetting(hookGroup("Edit|Write", hookHandler(EDIT_HOOK)), hookGroup("Bash", hookHandler(MERGE_HOOK)))

        val output = fixture(hooks).runAndFail(TASK).output

        output shouldContain "hook $EDIT_HOOK: matcher is Edit|Write, expected $EDIT_MATCHER"
    }

    @Test
    fun `a hook that may fail open fails`() {
        val open = hookHandler(MERGE_HOOK) - "onFailure"
        val continuing = hookHandler(MERGE_HOOK) + ("onFailure" to "continue")

        fixture(withMergeHandler(open)).runAndFail(TASK).output shouldContain
            "hook $MERGE_HOOK: onFailure is null, expected block"
        fixture(withMergeHandler(continuing)).runAndFail(TASK).output shouldContain
            "hook $MERGE_HOOK: onFailure is continue, expected block"
    }

    @Test
    fun `a hook narrowed by if or run in the background fails`() {
        val narrowed = hookHandler(MERGE_HOOK) + ("if" to "Bash(git merge *)")
        val background = hookHandler(MERGE_HOOK) + ("async" to true)

        fixture(withMergeHandler(narrowed)).runAndFail(TASK).output shouldContain
            "hook $MERGE_HOOK: sets if, which narrows the calls it sees"
        fixture(withMergeHandler(background)).runAndFail(TASK).output shouldContain
            "hook $MERGE_HOOK: runs in the background, so its decision is discarded"
    }

    @Test
    fun `a hook run another way than bash with the script under the project directory fails`() {
        val shellForm = mapOf("type" to "command", "command" to $$"\"${CLAUDE_PROJECT_DIR}\"/$$MERGE_HOOK")
        val relative = hookHandler(MERGE_HOOK) + ("args" to listOf(MERGE_HOOK))

        fixture(withMergeHandler(shellForm + ("onFailure" to "block"))).runAndFail(TASK).output shouldContain
            $$"hook $$MERGE_HOOK: must run as command bash with args [${CLAUDE_PROJECT_DIR}/$$MERGE_HOOK]"
        fixture(withMergeHandler(relative)).runAndFail(TASK).output shouldContain
            "hook $MERGE_HOOK: must run as command bash"
    }

    @Test
    fun `disableAllHooks fails`() {
        val output = fixture(extra = mapOf("disableAllHooks" to true)).runAndFail(TASK).output

        output shouldContain "disableAllHooks is true, which switches the governance hooks off"
    }

    @Test
    fun `a missing hook script fails`() {
        val output = fixture(scripts = false).runAndFail(TASK).output

        output shouldContain "hook script $EDIT_HOOK does not exist"
        output shouldContain "hook script $MERGE_HOOK does not exist"
    }

    @Test
    fun `hook scripts outside the governed paths fail`() {
        val output = fixture(entries = listOf("mise.toml")).runAndFail(TASK).output

        output shouldContain "hook script $EDIT_HOOK is not a governed path"
        output shouldContain "hook script $MERGE_HOOK is not a governed path"
    }

    @Test
    fun `hook scripts governed by exact entries or by a glob with stars in the middle pass`() {
        listOf(listOf(EDIT_HOOK, MERGE_HOOK), listOf("scripts/**/ask-**.sh")).forEach { entries ->
            val result = fixture(entries = entries).run(TASK)

            result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED
        }
    }

    @Test
    fun `a single star is literal and governs no hook script`() {
        val output = fixture(entries = listOf("scripts/governance/*")).runAndFail(TASK).output

        output shouldContain "hook script $EDIT_HOOK is not a governed path"
    }

    @Test
    fun `deleting a hook script after a passing run fails the next run`() {
        val fixture = fixture()
        fixture.run(TASK)

        Files.delete(projectDir.resolve(MERGE_HOOK))

        fixture.runAndFail(TASK).output shouldContain "hook script $MERGE_HOOK does not exist"
    }

    private fun withMergeHandler(handler: Map<String, Any?>): Map<String, Any?> =
        hooksSetting(hookGroup(EDIT_MATCHER, hookHandler(EDIT_HOOK)), hookGroup("Bash", handler))

    private companion object {
        const val TASK = "checkClaudeAskRules"
        val ENTRIES = listOf(".claude/settings.json", HOOK_SCRIPTS_ENTRY)
    }
}
