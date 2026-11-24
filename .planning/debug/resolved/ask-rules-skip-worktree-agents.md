---
status: resolved
trigger: "Governance ask rules (Edit(/<governed path>) in .claude/settings.json) did not prompt the author when gsd-executor subagents dispatched with isolation: worktree edited governed files"
created: 2026-11-23
updated: 2026-11-24
---

# Debug: ask rules do not fire for worktree-isolated agents

## Symptoms

- **Expected:** every Edit/Write of a governed path (governance/governed-paths.txt, mirrored one-to-one as `Edit(/<entry>)` ask
  rules in `.claude/settings.json`, checked by `checkClaudeAskRules`; requirement AIDEV-08) prompts the author.
- **Actual:** gsd-executor subagents dispatched with `isolation: "worktree"` (worktrees under `.claude/worktrees/agent-*/`)
  edited governed files with no prompt: build-logic governance package, `build-logic/src/main/kotlin/courses.root.gradle.kts`,
  `scripts/doctor.sh`, `governance/governed-paths.txt`, `.claude/settings.json`.
- **Errors:** none; edits succeed silently.
- **Timeline:** observed 2026-11-23 in quick tasks 261123-mub and 261123-rzt. Unknown whether ask rules ever fired inside a
  worktree. In the main checkout, Phase 1 UAT test 3 confirmed a live ask prompt on a governed Edit.
- **Reproduction:** dispatch a gsd-executor with `isolation: "worktree"` and have it Edit a governed file.

## Context and hypotheses to test

- Author's hypothesis: GSD (and untracked `.claude/` project config such as hooks, `gsd-core`, agents) is not installed in the
  worktree, so it should be. Only `.claude/settings.json`, `.claude/CLAUDE.md` and `.claude/skills/spike-findings-*` are
  tracked (`.gitignore`, D-39).
- Alternative: `Edit(/...)` rules are anchored at the project root of the session's settings source; a file under
  `.claude/worktrees/agent-x/build-logic/...` does not match `/build-logic/**`, so no ask rule applies regardless of what is
  installed in the worktree.
- Others: which settings file a subagent in a worktree loads (main checkout vs worktree copy of `.claude/settings.json`);
  whether the session's permission mode (auto mode) or allow rules auto-approved; interaction with GSD hooks
  (`gsd-worktree-path-guard`, agent isolation guard).
- Related known concern in STATE.md: GSD hooks fail in Claude Code sessions started inside a git worktree (hooks resolve under
  `$CLAUDE_PROJECT_DIR`, the worktree has no `.claude/hooks/`).

## Constraints

- Any fix must keep `checkClaudeAskRules` parity (one ask rule per governed entry) or extend that check consistently.
- Fix is applied only after author approval. Branch: gsd/phase-2-runtime-infrastructure-dcb-spikes.

## Current Focus

hypothesis: |
  Root cause has two independent parts (AND-gate: both must be closed for AIDEV-08 to hold for delegated work).
  (1) Anchoring: Claude Code resolves a `/`-prefixed permission pattern from projectSettings/localSettings against the
      settings root of the session, which is the directory the session was launched in (originalCwd = main checkout), not
      against the subagent's worktree cwd. A worktree subagent therefore evaluates `Edit(/governance/**)` as
      `<main>/governance/**`, and its edits of `<main>/.claude/worktrees/agent-x/governance/**` match no ask rule; auto mode
      then accepts them (reasonType "mode" for in-cwd edits, "classifier" for protected paths such as .claude/settings.json).
  (2) Coverage: governed content also reaches the main checkout through channels that Edit rules do not cover: Bash writes
      inside the worktree (python3/sed/perl/cat heredocs, encouraged by auto mode's bashFirst steer) and the
      `git merge --no-ff` that `worktree.cleanup-wave` runs in the main checkout from a Bash call.
bug_class: Bohrbug (deterministic: every worktree-agent edit of a governed path is accepted without a prompt)
doc_confirmation_pending:
  - "`/path` permission patterns resolve relative to the settings file / project root of the session (not the subagent cwd)"
  - "a subagent spawned with isolation: worktree evaluates permissions with the parent session's settings sources"
  - "Edit(...) rules do not apply to Bash subprocess writes or git operations"
  - "how an ask decision inside a background (non-interactive) subagent is surfaced: prompt in parent UI vs auto-deny"
  - "auto mode honours ask rules (empirically yes for main-session Edit and Bash; see evidence)"
reasoning_checkpoint:
  hypothesis: "Worktree agents' Edit/Write calls on governed files are accepted without a prompt because the project ask
    rules are anchored at the main checkout root (the session's launch directory), so paths under
    .claude/worktrees/<agent>/ never match them; in addition, Bash writes and the merge-back git merge are outside the scope of
    Edit ask rules altogether."
  confirming_evidence:
    - "Transcripts: 125 Edit/Write permission decisions on governed paths across sessions 22b04aad, 7015f70a, efc56988 - all in
      worktrees, all accepted (86 mode, 18 classifier, 21 command-rule), zero ask/prompt"
    - "Same subagents applied the main checkout's localSettings allow rule Edit(.planning/*) (ruleSource localSettings) although
      .claude/settings.local.json is untracked and absent from the worktree -> subagents use the launch directory's settings"
    - "Every agent worktree had the tracked .claude/settings.json with 56-58 Edit(/...) ask rules (base commits 305b21b, ff7ce8b,
      7a52ded) and still no ask fired -> the worktree's copy is not what is evaluated, or is anchored elsewhere"
    - "Positive control A (Phase 1 UAT, session c95a6123 launched INSIDE .claude/worktrees/phase-01-execution, auto mode): Edit of
      <that worktree>/governance/governed-paths.txt prompted and was declined -> ask rules fire in auto mode when the session root
      is the worktree"
    - "Positive control B (session 7015f70a launched in the main checkout on the phase-2 branch, auto mode): Bash(*config-set*)
      ask fired (decision user_temporary, ruleSource projectSettings) -> project ask rules load and fire in auto mode"
    - "Claude Code 2.1.330 bundle: pattern parser _c() maps a single leading '/' to root = tBn(source); for projectSettings and
      localSettings tBn returns EFe() -> settings context cwd = ns()/Te() = originalCwd; patterns without '/' get root null
      (relative to current cwd), which is why Edit(.planning/*) matched inside the worktree"
  falsification_test: "Probe P4 (worktree subagent runs `echo config-set-probe`) shows NO ask while P1 (same subagent, Write to
    <worktree>/mise/ask-rule-probe.txt) also shows no ask -> project ask rules are not applied in worktree subagents at all
    (H3), and anchoring is not the operative cause. Also falsified if the official docs state '/path' anchors at the
    subagent's working directory."
  fix_rationale: "(1) Adding worktree-anchored twins Edit(/.claude/worktrees/*/<entry>) makes the existing ask semantics reach
    agent worktrees for the Edit/Write/MultiEdit channel; (2) a merge-time governed-diff gate catches every write channel
    (Edit, Bash, scripts) at the point where worktree work enters the main checkout."
  blind_spots: "Resolved by probes on 2.1.330: P1/P4 confirm anchoring in a worktree subagent; P3/P4 show background
    non-interactive subagents surface asks. Remaining: git merge/checkout writes and arbitrary subprocess writes stay outside
    any Edit rule (closed at integration by fix B)."
  candidate_causes:
    - "config: project ask rules are '/'-anchored and the anchor is the session launch directory (main checkout)"
    - "environment: subagent spawned in a worktree evaluates permissions with the parent session's settings sources and root"
    - "code/process: governed writes via Bash and via cleanup-wave git merge are outside Edit-rule scope"
    - "environment (eliminated): auto mode ignores ask rules; GSD hooks auto-allow; worktree lacks GSD install"
  and_gate: "yes - closing only the anchoring gap still lets governed changes land via Bash writes and the merge-back; closing
    only the merge gate leaves Edit-level prompting (AIDEV-08 wording) broken for agents"
probe_plan: |
  Harmless target: mise/ask-rule-probe.txt (matches governed `mise/**`, does not exist, mise ignores non-config files).
  P1 worktree subagent (general-purpose, isolation: worktree, FOREGROUND): Write <its worktree>/mise/ask-rule-probe.txt.
     H1 predicts: no ask (accepted by mode).
  P4 same kind of subagent: Bash `echo config-set-probe` (matches Bash(*config-set*), path-independent).
     H1 predicts: ask fires (prompt). If no ask -> project ask rules are not applied in worktree subagents at all (H3).
  P4b as P4 but dispatched with run_in_background: true -> shows whether an ask in a background agent is surfaced to the
     author or auto-denied (decides whether option A blocks background executors).
  P3 non-isolated subagent (cwd = main checkout): Write /Users/afanasyev/Development/axon5-dcb-courses/mise/ask-rule-probe.txt.
     Predicts ask; decline it.
  P7 main session or non-isolated subagent: Bash `printf probe > mise/ask-rule-probe.txt` -> predicts no Edit-rule ask
     (classifier decides), i.e. Edit rules do not cover Bash writes.
  No temporary settings rule needed. Revert: rm -f mise/ask-rule-probe.txt in the main checkout; rmdir mise if empty; agent
  worktrees without commits are removed automatically, otherwise `git worktree remove --force` + `git branch -D`.
  Evidence to read afterwards: permissionDecision records in the probe subagents' JSONL (reasonType/ruleSource).
fix_options: |
  A. Worktree twins of every ask rule: Edit(/.claude/worktrees/*/<entry>) next to Edit(/<entry>) (58 more rules) and extend
     AskRulesCheck.ruleProblems so expected = both forms per entry + Bash(*config-set*) (parity per entry stays exact).
     Covers Edit/Write/MultiEdit by agent worktrees and EnterWorktree sessions under .claude/worktrees/. Does not cover Bash
     writes or the merge. Background executors that hit an ask may be auto-denied (P4b decides).
  B. Merge-time governed-diff gate: a project-owned script (scripts/governance/, governed) lists files changed on the
     executor branch that match governed-paths.txt; a tracked PreToolUse Bash hook returns permissionDecision "ask" for
     `worktree.cleanup-wave` / `git merge` when that list is non-empty (or, lighter, a CLAUDE.md rule that the orchestrator
     runs the script and asks via AskUserQuestion before merging). Catches every write channel at integration.
  C. Bash-write coverage in place: PreToolUse Bash hook heuristics or sandbox denyWrite on governed paths. Heuristic is
     bypassable; sandbox would affect Gradle/mise/docker. Not recommended now.
  D. Process: plans that touch governed paths run inline in the main session (no worktree executor). Simple but leaves Bash
     writes uncovered and depends on discipline.
  Recommended: A + B (A restores AIDEV-08 for the Edit channel; B closes Bash and merge channels for delegated work).
author_decision: |
  (2026-11-23, decision checkpoint answered) Q1: "A (hook) + B (strict)".
  A = a PreToolUse hook that reads governance/governed-paths.txt and returns permissionDecision "ask" for Edit/Write/
      NotebookEdit (and MultiEdit if present) whose absolute file_path, taken relative to the repository root OR to any
      .claude/worktrees/<name>/ root, matches a governed entry.
  B = strict: a PreToolUse Bash hook that returns "ask" before `worktree.cleanup-wave` / `git merge` when the branch being
      merged changes any governed path, listing the governed files in the reason.
  Extend the parity check (AskRulesCheck / checkClaudeAskRules) so the hook(s) and their registration in
  .claude/settings.json are themselves governed and verified.
  Q2: run probes P1, P4, P4b, P3, P7 (done, see Evidence). Q3: decided by the probes - background asks are surfaced, so no
  further checkpoint.
checkpoint: |
  (2026-11-24) DECISION CHECKPOINT - the author DECLINED the Write prompt for the new governed file
  scripts/governance/ask-governed-edit.sh (hook A). Per the fix brief the continuation agent stopped without retrying.
  Already written with the author's approval (uncommitted): scripts/governance/governed-paths.sh (new shared grammar
  and matcher), scripts/governance/check-pr.sh (sources it; test-check-pr.sh 132/132 green, shellcheck clean),
  scripts/governance/hook-common.sh (new hook protocol library). Ungoverned, uncommitted: docs/adr/0015 (new),
  docs/adr/0003 and 0004, docs/tooling.md, .claude/CLAUDE.md (all describe hooks that are not in the tree yet).
  Not yet written: ask-governed-edit.sh, ask-governed-merge.sh, test-ask-governed-edit.sh, test-ask-governed-merge.sh,
  GovernanceHooks.kt, GovernedPaths.kt, AskRulesCheck.kt, courses.governance.gradle.kts (description),
  governance/governed-paths.txt (header comment), the functional tests, and .claude/settings.json (registration).
  Verified drafts (all green) are in the session scratchpad: dev/scripts/governance/ (shell: 37 + 61 hook tests and
  132 gate tests under bash 3.2 and 5, shellcheck and shfmt clean, 17 mutants killed) and copy/build-logic/ (Kotlin:
  40 functional tests incl. 16 new hook tests, detekt, Dokka, ktfmt green; checkClaudeAskRules green on a copy with the
  registration).
checkpoint_answer: |
  (2026-11-24) Q1 "Continue from the drafts" - the decline was accidental; retry the Write, write the remaining drafts
  (settings.json last), chmod +x of the 4 new scripts approved. Q2 (.claude/CLAUDE.md bullet "Governed paths ask in
  every worktree"): "Keep".
next_action: none - resolved 2026-11-24. Live hook probes P1', P2' and the merge probe passed (see Evidence); the
  session manager judged the fix confirmed from the probe transcripts; the author answered both probe prompts (allowed
  once) and has not given a separate verdict. Session archived to .planning/debug/resolved/; the session manager
  commits this file. Knowledge-base entry not written by this agent (finalize brief limits changes to this file).
verification_outcome: "falsification test of reasoning_checkpoint_fix NOT triggered: after the hooks went live, the
  worktree subagent Write to <its worktree>/mise/ask-rule-probe.txt raised a hook ask (P1')."
fix_progress: |
  (2026-11-24, continuation) The fix is written, verified and committed (commits 7d738ef, 34dca1c, 6e668f6, 8456e67;
  see Resolution). Every governed file was written through Write/Edit and approved by the author at the ask-rule prompt
  (user_temporary, rule:projectSettings), byte-identical to the scratchpad drafts. The governed-paths.txt header had no
  draft and was written new. Hooks registered in .claude/settings.json at 2026-11-24T00:36:49Z; afterwards every
  Edit, Write and Bash call of the continuation agent ran without an unexpected prompt or block. The transcripts carry
  no PreToolUse hook records at all (also none for the GSD PreToolUse hooks), so whether the hooks are already live in
  the running session is not observable without a prompting probe; the live probes decide it.
deviations_from_brief:
  - "Hook registration uses exec form (command bash + args [${CLAUDE_PROJECT_DIR}/scripts/governance/<hook>.sh]) with
    onFailure block instead of the shell-form command string: the hooks reference prefers exec form for placeholders,
    and running through bash does not depend on the file mode bit."
  - "Shared code split into two sourced libraries: governed-paths.sh (grammar + matcher, also used by check-pr.sh) and
    hook-common.sh (hook protocol); ADR-0003 does not require check-pr.sh to be self-contained."
  - "The governed-path check of the hook scripts in AskRulesCheck derives the repository root from the location of
    .claude/settings.json (the directory ${CLAUDE_PROJECT_DIR} stands for) instead of a new task property."
fix_plan: |
  (continuation agent, 2026-11-23) Hook semantics confirmed in the official docs (raw copies in the session scratchpad
  docs/ directory):
  - hooks.md "Hook locations": settings-file hooks also run inside subagents; PreToolUse input carries agent_id/agent_type.
  - hooks.md "Common input fields" + "PreToolUse input": tool_name, tool_input, tool_use_id, cwd; Write/Edit/Read
    tool_input.file_path is always absolute; permissions.md "Tool-specific permission rules" names notebook_path as
    NotebookEdit's path field; MultiEdit is legacy and still matched by Edit rules.
  - hooks.md "PreToolUse decision control": hookSpecificOutput.permissionDecision "ask" + permissionDecisionReason (shown
    in the prompt), precedence deny > defer > ask > allow, so no other hook's allow can override the ask; "PreToolUse ask
    decisions in auto mode": a hook ask goes to the permission prompt, not to the classifier.
  - hooks.md "Matcher patterns": letters/digits/_/-/space/,/| only = exact list, so `Edit|Write|MultiEdit|NotebookEdit`
    and `Bash` are exact matchers (a Bash matcher also sees Monitor commands).
  - hooks.md "Exit code output": exit 0 + JSON applies the fields; any other exit or invalid JSON is a non-blocking error
    (the call proceeds) unless the handler sets onFailure "block" ("Block the action when a hook fails", v2.1.295+);
    timeouts likewise do not block without onFailure "block". Hence: scripts always exit 0 with JSON (fail closed via an
    EXIT trap) and the registration sets onFailure "block" for a hook that cannot start or times out.
  - hooks.md "Reference scripts by path": ${CLAUDE_PROJECT_DIR} is the project root where the session started and stays
    there when Claude enters a worktree; the input cwd follows the worktree. Exec form (command + args) is preferred for
    placeholders. settings.md "When edits take effect": settings files, hooks included, reload mid-session.
  - settings.md "Where Claude Code looks for each file": .claude/settings.json is read from the session's primary
    working directory, so a session whose hooks are registered has CLAUDE_PROJECT_DIR = repository (or worktree) root.
  Implementation:
  - scripts/governance/governed-paths.sh: the grammar (governed_globs) and matcher (glob_match, first_governed_glob)
    extracted from check-pr.sh, sourced by check-pr.sh and both hooks (ADR-0003 does not require check-pr.sh to be
    self-contained: the library sits under the same governed scripts/governance/** and comes from the same tree).
  - scripts/governance/hook-common.sh: the hook protocol (ask JSON, fail-closed EXIT trap, input field reading, lexical
    path normalisation, ASCII case folding).
  - scripts/governance/ask-governed-edit.sh (hook A) and ask-governed-merge.sh (hook B, strict), tests
    test-ask-governed-edit.sh and test-ask-governed-merge.sh. bash 3.2 compatible: the first bash on PATH is /bin/bash
    3.2 on this machine, and Claude Code runs hooks with that PATH.
  - Registration in .claude/settings.json (edited last): exec form `"command": "bash", "args":
    ["${CLAUDE_PROJECT_DIR}/scripts/governance/<hook>.sh"]`, onFailure "block". Deviation from the brief's shell-form
    command string: the docs prefer exec form for placeholders, and running through `bash` does not depend on the file
    mode bit.
  - AskRulesCheck: verifies the two registrations (matcher, command, args, onFailure block, no `if`, not async), that
    disableAllHooks is not true, and that each hook script exists and is a governed path (GovernedPaths gains the glob
    matcher that mirrors glob_match).
  - Docs: new ADR-0015, ADR-0003 reason bullet, governed-paths.txt header, GovernedPaths KDoc, .claude/CLAUDE.md note.
reasoning_checkpoint_fix:
  hypothesis: "Worktree agents' governed edits are not prompted because '/'-anchored project ask rules resolve against
    the session launch directory (main checkout); governed content also enters the main checkout through Bash writes
    and the cleanup-wave git merge, which no Edit rule covers."
  confirming_evidence:
    - "P1: worktree subagent Write to <worktree>/mise/ask-rule-probe.txt accepted by mode, no ask"
    - "P3: same rule asks for <main>/mise/ask-rule-probe.txt (user_reject, rule:projectSettings)"
    - "P4: path-independent Bash(*config-set*) ask fired in the same worktree subagent -> rules are loaded, only anchoring differs"
    - "P7: Bash redirect write to a governed path in the main checkout accepted by mode, no ask"
  falsification_test: "After the hooks are live, a worktree subagent Write to <its worktree>/mise/ask-rule-probe.txt
    still shows no ask (hook not applied in subagents, or path matching wrong) -> hypothesis or fix wrong."
  fix_rationale: "Hook A matches the edited path against every worktree root of the repository, so the anchoring of
    permission rules no longer matters for Edit/Write/MultiEdit/NotebookEdit; hook B inspects what a merge brings into a
    worktree, which catches governed content regardless of the channel (Bash, scripts, git) that wrote it."
  blind_spots: "Bash writes inside a worktree are still not prompted at write time (caught at merge by B); other
    integration channels (git pull, cherry-pick, rebase, reset, checkout of paths, patch apply, git aliases, scripts
    that run git) are outside B; disableAllHooks in the untracked settings.local.json switches the hooks off."
  candidate_causes:
    - "config: '/'-anchored project ask rules anchor at the session launch directory"
    - "process/code: governed writes via Bash and via the cleanup-wave git merge are outside Edit-rule scope"
  and_gate: "yes - both channels must be closed (see Current Focus and_gate)"

## Evidence

- timestamp: 2026-11-23
  checked: .claude/settings.json, .claude/settings.local.json, ~/.claude/settings.json
  found: project settings hold 59 ask rules (58 Edit(/<entry>) + Bash(*config-set*)); no allow rules and no defaultMode in
    project settings. settings.local.json (untracked) holds allow rules Read/Edit(.planning/*), Read/Edit(STATE.md), all GSD hooks
    (PreToolUse guards, PostToolUse context monitor, SessionStart, etc.) and worktree.baseRef=head. User settings hold only
    plugins/marketplaces/theme. No bypassPermissions, no acceptEdits default.
  implication: nothing in settings explicitly allows governed edits; acceptance must come from mode/classifier/command rules.
- timestamp: 2026-11-23
  checked: governance/governed-paths.txt
  found: 58 entries, mirrored one-to-one by the Edit(/...) ask rules. `scripts/doctor.sh` is NOT governed (only apply-github-settings.sh,
    scripts/governance/**, scripts/qodana.sh are).
  implication: the silent edit of scripts/doctor.sh listed in Symptoms is expected behaviour, not part of the bug.
- timestamp: 2026-11-23
  checked: .claude/agents/gsd-executor.md frontmatter
  found: tools Read, Write, Edit, Bash, Grep, Glob, Skill, context7; no permissionMode field in any GSD agent.
  implication: executors inherit the session permission mode.
- timestamp: 2026-11-23
  checked: main session transcripts (permissionMode field)
  found: every session since 2026-11-20 ran with permissionMode "auto" (0c0e26e3, 22b04aad, 288538cb, 675b64c5, 7015f70a,
    b94c2726, efc56988, ...); subagent meta: requestShape "background", requestNonInteractive true, spawnedWithWorktree true.
  implication: all executors run as background, non-interactive subagents in auto mode.
- timestamp: 2026-11-23
  checked: permissionDecision records in all retained transcripts (scratchpad all-decisions.tsv, governed-decisions.tsv)
  found: 125 Edit/Write decisions target governed paths; ALL are inside worktrees (agent-* and named worktrees); all accepted:
    86 reasonType "mode" (auto mode in-cwd edit), 18 "classifier" (.claude/settings.json, .github/workflows/ci.yml,
    .github/labels.yml, .mcp.json - protected paths sent to the auto-mode classifier), 21 "rule/command" (skill allowed-tools,
    named EnterWorktree worktrees in Phase 1). Zero ask prompts. Zero governed Edit/Write in the main checkout in retained
    transcripts. The quick tasks: mub (a84163ca) edited courses.governance.gradle.kts, courses.root.gradle.kts,
    governance/governed-paths.txt (mode) and .claude/settings.json (classifier); p1u (ac87cc3d) edited governance package
    files (mode); rzt (ace0c9c0) wrote governance/MarkdownOutline.kt (mode).
  implication: the gap is systemic across Phase 1 and Phase 2 worktree work, not specific to the two quick tasks.
- timestamp: 2026-11-23
  checked: rzt executor decisions for .planning writes
  found: Write/Edit of <worktree>/.planning/... accepted with reasonType "rule", ruleSource "localSettings".
  implication: the subagent evaluates the MAIN checkout's .claude/settings.local.json (the worktree has no such file - it is
    untracked) and resolves the slash-less pattern `.planning/*` against its worktree cwd.
- timestamp: 2026-11-23
  checked: git show <base>:.claude/settings.json for the pre-dispatch commits 305b21b (mub), ff7ce8b (p1u), 7a52ded (rzt)
  found: 56, 58 and 58 Edit(/...) ask rules respectively; the file is tracked, so each agent worktree had it on disk.
  implication: the ask rules WERE "installed" in every worktree; installing more of .claude/ (hooks, gsd-core, agents) into the
    worktree would not change which settings the subagent evaluates.
- timestamp: 2026-11-23
  checked: Phase 1 UAT test 3 note and session c95a6123 (project dir ...--claude-worktrees-phase-01-execution)
  found: session launched inside .claude/worktrees/phase-01-execution, permissionMode auto; Edit of
    <worktree>/governance/governed-paths.txt was prompted and declined (toolDenialKind recorded, tool result "The user doesn't
    want to proceed"). UAT note also: a session started from the main checkout without .claude/settings.json was NOT asked.
  implication: positive control - ask rules fire in auto mode when the session's launch directory is the tree being edited.
- timestamp: 2026-11-23
  checked: session 7015f70a (launched in the main checkout on gsd/phase-2-..., auto mode)
  found: one Bash decision "accept / user_temporary / ruleSource projectSettings" - the Bash(*config-set*) ask rule prompted.
  implication: positive control - project ask rules are loaded and honoured in auto mode for the main session in Phase 2.
- timestamp: 2026-11-23
  checked: Claude Code bundle /opt/homebrew/Caskroom/claude-code@latest/2.1.330/claude (minified JS, mmap search)
  found: `_c(pattern, root)`: '//x' -> absolute; '~/x' -> home; '/x' -> root = tBn(source); otherwise root null (cwd-relative).
    `tBn`: cliArg/command/session/... -> originalCwd; userSettings/policySettings/projectSettings/localSettings/flagSettings ->
    EFe(source) = hQi(source, L()); for project/local settings hQi returns the settings context cwd = ns() = Te() =
    `h()?.originalCwd ?? <session>.originalCwd`. projectSettings file path = <that cwd>/.claude/settings.json.
  implication: source-level support (to be confirmed by docs) that '/'-anchored project rules and the project settings file
    itself are resolved from the session launch directory, not from the worktree subagent's cwd.
- timestamp: 2026-11-23
  checked: executor Bash commands (exec-bash.tsv) and Phase 2 executor Bash commands in 7015f70a
  found: governed files were also written via Bash, accepted by the classifier: rzt python3 heredocs rewrote
    buildlogic/governance/LicencePolicy.kt and ImageReferenceCheck.kt; Phase 2 executors used python3/sed -i/perl -pi/cat > on
    courses.governance.gradle.kts, settings.gradle.kts, renovate.json5, governance/dependency-licences.json,
    courses.licences.gradle.kts, .github/workflows/*.yml, governance package files. Auto mode's system steer ("bashFirst":
    true) tells agents to prefer sed/heredocs/short scripts for small edits.
  implication: even a correctly anchored Edit ask rule would not see a large share of governed writes.
- timestamp: 2026-11-23
  checked: .claude/gsd-core/bin/lib/worktree-safety.cjs (cleanup-wave) and the merge-back Bash calls
  found: cleanup-wave runs `git merge <branch> --no-ff --no-edit` with cwd = repo root (line 1259) from a Bash tool call;
    those Bash calls were accepted by the classifier.
  implication: governed changes enter the main checkout through git, which no Edit rule can intercept.
- timestamp: 2026-11-23
  checked: .claude/hooks/*.js for permissionDecision output; gsd-worktree-path-guard.js; hooks/lib/hook-exit.js
  found: no hook emits permissionDecision allow/ask; allow() is a plain exit 0; worktree-path-guard only blocks absolute paths
    outside the worktree root.
  implication: GSD hooks neither grant nor suppress permission prompts.
- timestamp: 2026-11-23
  checked: official Claude Code docs (claude-code-guide research by the session manager; pages permissions, permission-modes,
    sub-agents, worktrees, hooks, hooks-guide, settings, tools-reference on code.claude.com/docs/en/)
  found: |
    - permissions#how-path-patterns-match: `/path` = "Path relative to the settings source"; for project .claude/settings.json the
      anchor is `<primary working directory>/path`; "Local settings rules anchor at the session's primary working directory".
      Worktree anchoring is documented only for worktree SESSIONS ("in a worktree session, a shared rule such as Edit(/src/**)
      matches that worktree's own src/ directory"); NOT documented for subagents with isolation: worktree.
    - sub-agents: subagents inherit "the parent conversation's permission rules"; isolation: worktree runs the subagent's Bash in
      its worktree; worktree is branched from the default branch unless worktree.baseRef = "head".
    - sub-agents#permission-modes: when the parent is in bypassPermissions, acceptEdits or auto mode the subagent runs in that
      mode and frontmatter permissionMode is ignored.
    - sub-agents#run-subagents-in-foreground-or-background: "Background subagents surface every permission prompt in your main
      session" (no documented auto-deny for background subagents outside dontAsk / -p).
    - permissions#manage-permissions: rules evaluated deny, then ask, then allow; permission-modes (auto mode): "Explicit ask
      rules still force a prompt"; permission-modes#actions-no-mode-auto-approves lists "Tools matched by an explicit ask rule"
      (no mode, including bypassPermissions, auto-approves them).
    - permissions#what-read-and-edit-rules-cover: Edit/Read DENY rules also apply to Bash redirection targets and recognised
      file commands (cat, head, tail, sed, tee) but not to arbitrary subprocesses (Python/Node scripts); whether ASK rules cover
      Bash writes, and whether git merge/checkout writes are covered, is NOT documented.
    - hooks#pretooluse-decision-control: a PreToolUse hook may return permissionDecision "ask"; tool_input.file_path is always
      absolute for Write/Edit/Read; the hook `if` field accepts permission-rule syntax such as "Edit(**/src/**)"; settings hooks
      also run inside subagents; a hook "ask" in auto mode goes to the permission prompt, not to the classifier.
    - permissions#how-deep-a-pattern-matches: `Edit(**/build-logic/**)` matches at any depth; bare deny/ask patterns match at any
      depth under the current directory; `//**/x` matches anywhere on the filesystem.
    Raw doc copies (session scratchpad, may not survive the session):
    /private/tmp/claude-501/-Users-afanasyev-Development-axon5-dcb-courses/efc56988-d163-4dce-8fa2-57e2ab5917f7/scratchpad/docs/
  implication: docs confirm the anchor is the session's primary working directory (main checkout) and that rules are inherited
    by subagents; nothing documents a subagent's worktree becoming the anchor, so H1 (anchoring) is doc-consistent but the
    subagent case itself is undocumented (probe P1/P4 remains the empirical confirmation). Docs say background subagent asks
    surface in the main session (relevant to P4b / option A). Auto mode and permissionMode are confirmed not to bypass ask
    rules. Bash/git coverage by ask rules is undocumented. `**/`-prefixed patterns and PreToolUse hooks (with absolute
    file_path) are documented alternatives to worktree twins for fix option A.

- timestamp: 2026-11-23
  checked: live probes P1, P4/P4b, P3, P7 (approved by the author), run by the session manager (subagent a92c7bb3, spawn
    depth 1, requestShape background, requestNonInteractive true, auto mode) and its probe child a4a222be (general-purpose,
    isolation worktree, spawn depth 2, background, non-interactive). Decisions read from the subagent JSONL
    (permissionDecision records), not from the probe agent's self-report.
  found: |
    - P1 (worktree subagent, Write .claude/worktrees/agent-a4a222be4cacd8a12/mise/ask-rule-probe.txt): accept, source config,
      reasonType "mode". No ask. The governed `Edit(/mise/**)` rule did not match the worktree path.
    - P4 = P4b (same worktree subagent, background and non-interactive, Bash `echo config-set-probe`): accept, source
      "user_temporary", ruleSource projectSettings, reasonCode "rule:projectSettings", grantScope "once"; tool_use at
      15:14:29.5Z, decision at 15:15:07.4Z (38 s, a human answer). The path-independent `Bash(*config-set*)` ask rule fired in
      the worktree subagent and the prompt was surfaced to the author in the main session (the author allowed it once).
      The probe agent's own report said "no prompt occurred"; the transcript is authoritative.
    - P3 (non-isolated background subagent, cwd = main checkout, Write <main>/mise/ask-rule-probe.txt): reject, source
      "user_reject", reasonCode "rule:projectSettings", toolDenialKind "user-rejected"; tool_use 15:14:28.8Z, decision
      15:14:42.1Z (13 s, the author declined). The same `Edit(/mise/**)` rule fires when the path is under the main checkout.
    - P7 (same non-isolated subagent, Bash `mkdir -p mise && printf 'probe\n' > mise/ask-rule-probe.txt` in the main checkout):
      accept, source config, reasonType "mode". No ask: Edit ask rules do not cover Bash redirect writes.
    - Cleanup: P7 file and directory removed in the main checkout; the probe worktree removed its file, ended clean and was
      removed automatically together with its branch (git worktree list shows only the main checkout; no
      worktree-agent-a4a222be branch). git status shows only this debug file.
  implication: H1 (anchoring) confirmed and H3 (project ask rules not applied in worktree subagents) falsified - the same
    subagent honoured a path-independent project ask rule while its governed-path Write matched none. Background,
    non-interactive subagents (also at spawn depth 2) surface asks in the main session; they are not auto-denied, so an "ask"
    decision from a hook will reach the author for background executors too (Q3 checkpoint not needed). P7 confirms the
    Bash channel gap (part 2 of the AND-gate).

- timestamp: 2026-11-24
  checked: live hook probes after the fix, run by the session manager (subagent af52828aba4b9f0b5) in the same Claude
    Code session efc56988, no restart; hooks registered in .claude/settings.json at 2026-11-24T00:36:49Z (commits
    7d738ef, 34dca1c, 6e668f6, 8456e67). Probe child: general-purpose subagent aadb976668d23572f, isolation worktree,
    background, auto mode, worktree .claude/worktrees/agent-aadb976668d23572f. Decisions read from permissionDecision
    records and PreToolUse hook attachments in the transcripts, not from agent self-reports. Re-checked by this agent:
    subagents/agent-aadb976668d23572f.jsonl holds exactly one hook ask attachment (reason text below) and one decision
    with reasonCode "hook:PreToolUse"; subagents/agent-af52828aba4b9f0b5.jsonl holds one merge ask attachment and one
    decision with reasonCode "hook:PreToolUse".
  found: |
    - P1' (worktree subagent, Write <worktree>/mise/ask-rule-probe.txt, the same call that P1 showed accepted by "mode"
      before the fix): hook attachment PreToolUse:Write at 00:55:06.376Z returned permissionDecision "ask", reason
      "Governance hook: mise/ask-rule-probe.txt in /Users/afanasyev/Development/axon5-dcb-courses/.claude/worktrees/
      agent-aadb976668d23572f is a governed path (entry `mise/**` of governance/governed-paths.txt). Edits to governed
      paths need the author's confirmation (AIDEV-08)." Decision at 00:56:05.090Z: accept, source "user_temporary",
      grantScope "once", reasonCode "hook:PreToolUse" (59 s human latency). The author allowed the probe once; the probe
      agent created and then removed the file.
    - P2' (same subagent, Write <worktree>/docs/hook-probe-nongoverned.txt, a non-governed path): accept, source config,
      reasonType "mode"; no hook ask.
    - Merge probe (session manager Bash, cwd main checkout): scratch clone <scratchpad>/hookprobe-clone with branch
      probe/governed-change adding mise/ask-rule-probe.txt; command `git -C <clone> merge --no-ff --no-edit
      probe/governed-change`. Hook attachment PreToolUse:Bash at 00:56:41.577Z: permissionDecision "ask", reason
      "Governance hook: this command merges changes to governed paths: mise/ask-rule-probe.txt (from
      probe/governed-change). Merges of governed paths need the author's confirmation (AIDEV-08)." Decision at
      00:57:03.560Z: accept, user_temporary, grantScope once, reasonCode "hook:PreToolUse" (22 s). The merge ran only in
      the scratch clone.
    - The hooks became live mid-session without a restart (settings reload confirmed empirically).
    - Session-manager Bash calls that do not merge ran without any hook prompt.
    - Cleanup: scratch clone deleted; probe worktree and its branch removed automatically. Re-checked by this agent:
      `git worktree list` shows only the main checkout, no worktree-agent-* or probe/* branch, no mise/ directory in the
      main checkout, `git status --short` shows only this debug file; HEAD 8456e67.
  implication: the falsification test of reasoning_checkpoint_fix was NOT triggered. Hook A closes the anchoring gap for
    Edit/Write in worktree subagents (the exact P1 call that was silently accepted before the fix now asks, with the
    worktree and matching entry named), does not over-ask on non-governed paths (P2'), and hook B asks before a merge that
    brings governed content regardless of how it was written. Both channels of the AND-gate are closed; the asks surface
    to the author from a background worktree subagent and from the main session alike.
    Verdict (session manager, from the transcripts): fixed.

## Eliminated

- hypothesis: project ask rules are not applied in worktree subagents at all (H3)
  evidence: probe P4 - Bash(*config-set*) ask fired in a worktree subagent (user_temporary, rule:projectSettings).
  timestamp: 2026-11-23
- hypothesis: an ask raised inside a background, non-interactive subagent is silently auto-denied
  evidence: probes P3 and P4 - both prompts reached the author (13 s and 38 s human latency, user_reject and user_temporary).
  timestamp: 2026-11-23

- hypothesis: auto mode ignores ask rules
  evidence: positive controls - Phase 1 UAT session (auto mode) prompted on a governed Edit; session 7015f70a (auto mode)
    prompted on Bash(*config-set*).
  timestamp: 2026-11-23
- hypothesis: allow rules or a default permission mode in settings auto-approve governed edits
  evidence: no allow rule covers governed paths (only .planning/* and STATE.md); no defaultMode anywhere; decisions on governed
    paths are mode/classifier, never an allow rule from settings.
  timestamp: 2026-11-23
- hypothesis: a GSD PreToolUse hook returns permissionDecision allow and bypasses the ask rules
  evidence: no hook emits permissionDecision; recorded reasonType for governed edits is mode/classifier, not hook.
  timestamp: 2026-11-23
- hypothesis (author's): the ask rules do not fire because GSD/.claude config is not installed in the worktree
  evidence: the tracked .claude/settings.json with all ask rules was present in every agent worktree; the subagent evaluated
    the main checkout's untracked settings.local.json (ruleSource localSettings), i.e. it reads settings from the launch
    directory, so installing hooks/gsd-core/agents into the worktree does not change permission evaluation.
  timestamp: 2026-11-23
- hypothesis: an agent permissionMode frontmatter (e.g. acceptEdits/bypassPermissions) on gsd-executor
  evidence: no permissionMode in any .claude/agents/*.md.
  timestamp: 2026-11-23

## Resolution

root_cause: (confirmed by the docs and probes P1/P3/P4/P7) Project ask rules are written as `Edit(/<entry>)`;
  Claude Code anchors a single-slash pattern at the settings root of the session, which is the launch directory (main
  checkout). Worktree-isolated subagents run in the same session and evaluate the main checkout's settings with that anchor, so
  their edits under .claude/worktrees/<agent>/ never match a governed rule and auto mode accepts them. Independently, governed
  content also reaches the main checkout via Bash writes in the worktree and via the cleanup-wave `git merge`, which Edit rules
  never cover.
fix: A + B strict per author_decision (ADR-0015). Hook A scripts/governance/ask-governed-edit.sh (matcher
  Edit|Write|MultiEdit|NotebookEdit) asks when the edited file, taken relative to the session root, the main worktree,
  every registered worktree or any .claude/worktrees/<name>/ root, is a governed path. Hook B
  scripts/governance/ask-governed-merge.sh (matcher Bash) asks when a `git merge` or GSD `worktree.cleanup-wave` brings
  changes to governed paths, listing them, and asks when it cannot resolve a merge. Both share governed-paths.sh
  (grammar and matcher, also sourced by check-pr.sh) and hook-common.sh (fail-closed hook protocol, never "allow").
  Registered in .claude/settings.json in exec form (`bash` + `${CLAUDE_PROJECT_DIR}/scripts/governance/<hook>.sh`),
  onFailure "block". checkClaudeAskRules extended by GovernanceHooks (registration once per hook with matcher, command,
  args, onFailure block, no if/async; disableAllHooks not true; both scripts exist and are governed); GovernedPaths
  gained `governs`/`matches` mirroring glob_match. Docs: ADR-0015 (new), ADR-0003, ADR-0004, docs/tooling.md,
  .claude/CLAUDE.md, governed-paths.txt header.
commits:
  - "7d738ef refactor(governance): share the governed-paths grammar and matcher in one shell library"
  - "34dca1c feat(governance): add hooks that ask before governed edits in any worktree and governed merges"
  - "6e668f6 feat(governance): register the governance hooks and verify them in checkClaudeAskRules"
  - "8456e67 docs(governance): describe the governance hooks in the ADRs, tooling guide and agent rules"
verification: |
  (2026-11-24, in the repository; all pre-commit hooks ran and passed on each commit: format, gitleaks, reuse,
  shellcheck)
  - Shell tests under /bin/bash 3.2.57 and /opt/homebrew/bin/bash 5.3.20 (hook run with the same bash):
    test-ask-governed-edit.sh 37/37, test-ask-governed-merge.sh 63/63, test-check-pr.sh 132/132,
    test-list-pr-files.sh 25/25, zero failures on both.
  - shellcheck 0.11.0 clean on every tracked *.sh plus the new scripts; shfmt 3.14.1 -d clean on scripts/governance;
    editorconfig-checker clean; reuse lint clean.
  - Red before green: with everything except the registration in place, `./gradlew checkClaudeAskRules` FAILED with
    "hooks.PreToolUse is missing or not a list"; after the registration it passed with an empty report.
  - Governance functional tests 40/40 (AskRulesCheck 9, AskRulesCheckHooks 16, AskRulesCheckInputShape 6,
    GovernedPathsFormat 9).
  - `mise x -- ./gradlew build checkDependencyLicences -x integrationTest` (the CI command): BUILD SUCCESSFUL in 8m 57s,
    build-logic functionalTest 217 tests, 0 failures, 0 errors; spotlessCheck, detekt, Dokka, checkAdrs,
    checkGsdConfigPolicy, checkClaudeAskRules green. (A first run without `mise x` failed only in verifyNodeVersion and
    Node/Prettier-dependent functional tests because the shell PATH has nvm Node 24.20.0 while .nvmrc pins 26; not
    related to the fix.)
  - `mise x -- scripts/doctor.sh --ci`: 8 ok, 1 warn (pre-existing UP-0012 upstream-removal note), 0 fail.
  - Dry runs of the committed hooks on this session's inputs: Edit of the debug file -> no output; Edit of
    <main>/.claude/settings.json -> ask naming entry `.claude/settings.json`; Write of
    <main>/.claude/worktrees/agent-x/mise/ask-rule-probe.txt -> ask naming entry `mise/**` and the worktree;
    `git commit -m "... merge ..."` -> no output; one Edit hook run about 0.1 s.
  - Committed scripts and settings.json are byte-identical to the verified scratchpad drafts.
  - Live probes (2026-11-24, session manager, same session, no restart; decisions from transcript records, see the last
    Evidence entry):
    - P1' worktree subagent Write <worktree>/mise/ask-rule-probe.txt -> hook ask naming entry `mise/**` and the
      worktree (user_temporary, once, reasonCode hook:PreToolUse). Before the fix the same call was accepted by "mode"
      (P1). Bug present before, gone after.
    - P2' worktree subagent Write <worktree>/docs/hook-probe-nongoverned.txt -> accepted by "mode", no ask (no
      over-asking on non-governed paths).
    - Merge probe `git -C <scratch clone> merge --no-ff --no-edit probe/governed-change` (branch adds
      mise/ask-rule-probe.txt) -> hook ask listing the governed file and the source branch (user_temporary, once,
      reasonCode hook:PreToolUse).
    - Non-merge Bash calls of the session manager -> no hook prompt. Hooks went live mid-session without a restart.
    - Cleanup verified: only the main checkout in `git worktree list`, no probe branches, no mise/ directory, clean
      status apart from this file.
  - Human verification: the author answered the P1' and merge-probe prompts (allowed once each, hook:PreToolUse); the
    "confirmed fixed" verdict is the session manager's, from the transcripts, not a separate author statement.
  - Fix-acceptance guardrail (per-signal):
    target_test: pass (shell hook tests 37/37 and 63/63, AskRulesCheckHooks functional tests 16/16; live probe P1')
    mutation_check: pass at the shell level (17 seeded mutants killed by the hook and gate test scripts on the drafts);
      Stryker not applicable (bash and Kotlin build-logic, no Stryker configured)
    no_op_deletion: pass (the fix adds behaviour: two hooks, two libraries, registration and checks; the only removal is
      the grammar and matcher moved from check-pr.sh into governed-paths.sh, covered by test-check-pr.sh 132/132)
    adjacent_tests: pass (test-check-pr.sh, test-list-pr-files.sh, build-logic functionalTest 217/217, full CI build)
    revert_and_reconfirm: pass in before/after form (bug observed before the fix by P1 and by checkClaudeAskRules red
      without the registration; gone after by P1' and green checkClaudeAskRules); no explicit revert of the committed
      fix was run, because P1 and P1' are the same call on the same Claude Code version in the same session
    guardrail_verdict: accepted
files_changed: [scripts/governance/governed-paths.sh (new), scripts/governance/check-pr.sh,
  scripts/governance/hook-common.sh (new), scripts/governance/ask-governed-edit.sh (new),
  scripts/governance/ask-governed-merge.sh (new), scripts/governance/test-ask-governed-edit.sh (new),
  scripts/governance/test-ask-governed-merge.sh (new), .claude/settings.json, governance/governed-paths.txt,
  build-logic/src/main/kotlin/courses.governance.gradle.kts,
  build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/{AskRulesCheck.kt, GovernedPaths.kt,
  GovernanceHooks.kt (new)},
  build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/{AskRulesCheckFunctionalTest.kt,
  AskRulesCheckInputShapeFunctionalTest.kt, GovernedPathsFormatFunctionalTest.kt, AskRulesCheckHooksFunctionalTest.kt
  (new), ClaudeSettingsFixtures.kt (new)}, docs/adr/0015-governance-hooks-for-worktrees-and-merges.md (new),
  docs/adr/0003-governance-gate-trust-boundary.md, docs/adr/0004-gsd-configuration.md, docs/tooling.md,
  .claude/CLAUDE.md] (all committed in 7d738ef, 34dca1c, 6e668f6 and 8456e67 on
  gsd/phase-2-runtime-infrastructure-dcb-spikes; this session file is archived to
  .planning/debug/resolved/ask-rules-skip-worktree-agents.md and committed by the session manager, not by the debugger)
cycles: |
  Not counted explicitly during the session; reconstructed from the checkpoints in this file:
  investigation 1 cycle (static evidence from settings, transcripts and the Claude Code bundle, official docs, then
  live probes P1/P3/P4/P7; 7 hypotheses eliminated), followed by the author's fix decision (A hook + B strict);
  fix 2 cycles (the first stopped at the accidentally declined Write of ask-governed-edit.sh, the second wrote,
  verified and committed the fix); verification 1 cycle (live probes P1', P2', merge probe; verdict by the session manager
  from the transcripts).
prevention: |
  Blameless postmortem (branches reuse reasoning_checkpoint.candidate_causes; AND-gate = yes).
  Branch 1 (config): governed edits in agent worktrees were not prompted.
    Why? The ask rules were `Edit(/<entry>)`, anchored at the session's primary working directory (the main checkout).
    Why was that anchor wrong for agents? Subagents with isolation: worktree run in the parent session and inherit its
      settings and anchor; the docs describe worktree anchoring only for worktree sessions, not for worktree subagents.
    Why was this assumption never tested? The only live ask test (Phase 1 UAT test 3) ran in a session launched inside
      the worktree, which is the case where the anchor matches; no test exercised a worktree subagent.
  Branch 2 (process/code): governed content reached the main checkout through channels no Edit rule covers.
    Why? Executors write with Bash (auto mode steers to sed/heredocs/scripts) and cleanup-wave integrates the work with
      `git merge` from a Bash call.
    Why was that not covered? The control (ADR on governance, AIDEV-08) was specified as "one Edit ask rule per governed
      entry" and checkClaudeAskRules verified exactly that textual parity, so the check passed while the rule did not
      reach the paths and channels agents actually used.
  Branch 3 (environment): the gap was invisible in the record of work.
    Why? Auto mode accepts silently (reasonType mode/classifier), and nothing reviewed permissionDecision records for
      governed paths; it surfaced only when the author noticed missing prompts in quick tasks 261123-mub and -rzt.
  why_not_caught: "No gate existed for this class: checkClaudeAskRules verified that the ask rules exist (textual
    parity with governed-paths.txt), not that they fire for the paths and channels in use; the one live check (Phase 1
    UAT test 3) ran in a session launched inside the worktree, so the anchor happened to match; no test covered a
    worktree subagent, Bash writes or the merge-back."
  recurrence_guard: |
    - Hook A scripts/governance/ask-governed-edit.sh matches the edited path against the session root, the main
      worktree, every registered worktree and any .claude/worktrees/<name>/ root, so rule anchoring no longer decides
      whether a governed edit asks; hook B scripts/governance/ask-governed-merge.sh asks before any `git merge` or
      `worktree.cleanup-wave` that brings governed paths, whatever channel wrote them. Both fail closed (EXIT trap,
      onFailure "block") and never emit "allow".
    - Regression tests: scripts/governance/test-ask-governed-edit.sh (37 cases, including worktree paths under
      .claude/worktrees/<agent>/) and scripts/governance/test-ask-governed-merge.sh (63 cases), run under bash 3.2 and 5.
    - Build gate: checkClaudeAskRules (AskRulesCheck + GovernanceHooks.kt) fails the build when either hook registration
      is missing or altered (matcher, command, args, onFailure block, no if/async), when disableAllHooks is true, or when
      a hook script is missing or not itself a governed path; covered by
      build-logic/src/functionalTest/.../governance/AskRulesCheckHooksFunctionalTest.kt (16 tests). The hooks, the
      libraries and settings.json are governed, so changing the guard itself asks the author.
    - Decision record: docs/adr/0015-governance-hooks-for-worktrees-and-merges.md; agent rule "Governed paths ask in
      every worktree" in .claude/CLAUDE.md.
    - All artifacts verified present in the tree at HEAD 8456e67 by this agent (2026-11-24).
  residual_risk: "Bash writes inside a worktree are prompted only at merge time (hook B), not at write time; other
    integration channels (git pull, cherry-pick, rebase, reset, checkout of paths, patch apply, git aliases, scripts
    that run git) and disableAllHooks in the untracked settings.local.json remain outside the guard (documented in
    ADR-0015 and reasoning_checkpoint_fix.blind_spots)."
