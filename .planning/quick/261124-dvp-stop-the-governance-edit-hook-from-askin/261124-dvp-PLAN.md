---
phase: quick-261124-dvp
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - scripts/governance/ask-governed-edit.sh
  - scripts/governance/test-ask-governed-edit.sh
  - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
  - docs/adr/0003-governance-gate-trust-boundary.md
  - docs/adr/0004-gsd-configuration.md
  - .claude/CLAUDE.md
  - governance/governed-paths.txt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt
  - scripts/governance/ask-governed-merge.sh
autonomous: true
requirements: [AIDEV-08]

estimate:
  tokens: 120000
  raw_tokens: 120000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "Author decision 2026-11-24 (locked): an Edit, Write, MultiEdit or NotebookEdit of a governed path inside an agent worktree gets no output from scripts/governance/ask-governed-edit.sh. An agent worktree is a worktree that `git worktree list --porcelain -z` of the session's repository reports, that is on a branch (not detached, not bare, not prunable), whose path both as written and with symbolic links resolved is exactly `<session root or main worktree root>/.claude/worktrees/agent-<id>` (`<id>` from [A-Za-z0-9._-]), whose branch is `agent-<id>` or `worktree-agent-<id>`, and which is not itself the session root or the main worktree root"
    - "The main checkout keeps the per-edit ask: a governed edit in the session root or the main worktree root asks with the unchanged reason (entry and root named), also while agent worktrees exist and also when the session root itself is an agent worktree"
    - "Non-agent worktrees keep the per-edit ask: an EnterWorktree-style worktree `.claude/worktrees/<name>` on `worktree-<name>` or on a renamed `gsd/phase-*` branch, any worktree outside `.claude/worktrees/`, a Workflow-tool worktree (`worktree-wf_*`), and an unregistered directory under `.claude/worktrees/` (agent-shaped or not)"
    - "Every spoof attempt asks: a `..` path or a relative path from an agent-worktree cwd into the main checkout, a symbolic link file or directory inside an agent worktree that points into the main checkout, a registered agent worktree whose directory was replaced by a symbolic link to the main checkout, a different case spelling of the agent worktree directory, a detached agent worktree, an agent-named worktree on a non-agent branch, a branch that names a different agent, and an agent-shaped worktree outside `.claude/worktrees/`"
    - "The exemption is decided only from git's worktree list of the session's repository and the file system, through the hook's existing porcelain loop and its lexical_path/physical_path/add_root resolution; no environment variable and no tool-input field beyond the file path and cwd the hook already reads takes part, and every candidate spelling (as written and resolved) must clear every non-exempt root"
    - "The merge hook still asks once when `git merge` or `worktree.cleanup-wave` brings an agent branch's governed changes into a worktree: test-ask-governed-merge.sh stays 120 passed, 0 failed, and test-check-pr.sh stays 132 passed, 0 failed"
    - "ADR-0015 records the 2026-11-24 decision with the alternatives the author rejected (keep the per-edit ask; remember an approval per task with a TTL), the consequences and the residual risk that unapproved governed edits can accumulate in an agent worktree and are gated only at the merge prompt and at the pull request (`governance-approved` label through check-pr.sh); ADR-0003, ADR-0004, .claude/CLAUDE.md, the governed-paths.txt header, the GovernanceHooks KDoc and the merge hook header describe the new behaviour; docs/tooling.md needs no change because it does not describe the per-edit behaviour"
    - "`bash scripts/governance/test-ask-governed-edit.sh` passes with at least 54 cases (37 at HEAD) under the default bash and under /bin/bash 3.2, and `mise x -- ./gradlew checkClaudeAskRules checkAdrs spotlessCheck :build-logic:functionalTest --tests '*AskRules*'` passes"
  artifacts:
    - path: "scripts/governance/ask-governed-edit.sh"
      provides: "Edit hook that skips governed matches relative to a verified agent worktree root and keeps asking for every other root"
      contains: "AGENT_ROOTS"
    - path: "scripts/governance/test-ask-governed-edit.sh"
      provides: "Cases for agent worktrees (silent), the main checkout and non-agent worktrees (ask) and every spoof attempt (ask)"
      contains: "agent worktree"
    - path: "docs/adr/0015-governance-hooks-for-worktrees-and-merges.md"
      provides: "The amended decision, options, consequences, residual risks and confirmation"
      contains: "governance-approved"
    - path: ".claude/CLAUDE.md"
      provides: "Agent Git Workflow bullet describing per-edit asks outside agent worktrees and the single merge prompt"
      contains: "agent worktree"
  key_links:
    - from: "scripts/governance/ask-governed-edit.sh"
      to: "scripts/governance/ask-governed-merge.sh"
      via: "governed edits left unprompted in an agent worktree reach the main checkout only through the `git merge` / `worktree.cleanup-wave` the merge hook asks about"
      pattern: "cleanup-wave"
    - from: ".claude/settings.json"
      to: "scripts/governance/ask-governed-edit.sh"
      via: "unchanged exec-form registration under `${CLAUDE_PROJECT_DIR}` with onFailure block, verified by checkClaudeAskRules"
      pattern: "ask-governed-edit.sh"
    - from: "scripts/governance/ask-governed-edit.sh"
      to: "git worktree list --porcelain -z"
      via: "the existing WORKTREE_LIST loop, extended to read each record's branch, detached, bare and prunable fields"
      pattern: "WORKTREE_LIST"
---

<objective>
Stop the governance edit hook from asking on each Edit/Write of a governed path inside an agent worktree, as the author
decided on 2026-11-24 (locked; the author rejected "keep as is" and "remember an approval per task with a TTL"). The
merge hook keeps asking once, naming the governed files, when the agent worktree's branch is merged into the main
checkout. The main checkout and every non-agent worktree keep the per-edit ask. The exemption fails closed: it rests on
git's worktree list and the file system, never on an environment variable or the tool-input path alone, and any doubt
keeps asking.

Purpose: a quick task whose executor edited governed files raised dozens of prompts, one per edit, about changes that
cannot reach the main checkout before the merge prompt anyway. One prompt per merge keeps AIDEV-08's intent (the agent
cannot silently change the settings, hooks and governed list the session runs on, which come from
`${CLAUDE_PROJECT_DIR}`) with far fewer prompts.

Output: the amended hook and its tests, ADR-0015 amended in place (it is not on `main` yet), and the descriptions in
ADR-0003, ADR-0004, `.claude/CLAUDE.md`, the governed-paths.txt header, the GovernanceHooks KDoc and the merge hook
header brought in line.

Ground rules for this run (the old hook is still live, because hooks run from the main checkout): every governed edit
in the executor's worktree asks the author. Governed files in this plan are `scripts/governance/**`,
`governance/governed-paths.txt` and `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**`.
Change them only with the Write or Edit tool, never with shell redirection, `sed -i`, `cp` or scripts. Keep the number
of prompts low: one Write per rewritten governed file, one Edit per comment-only change. If the author declines a
prompt, stop and report instead of working around it. The Axon and Gradle plugin skills are not needed: nothing here
touches Axon code or the Gradle build logic beyond one KDoc paragraph.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
@scripts/governance/ask-governed-edit.sh
@scripts/governance/hook-common.sh
@scripts/governance/test-ask-governed-edit.sh

Background, read only the parts named:
- `.planning/debug/resolved/ask-rules-skip-worktree-agents.md`, sections `author_decision` and `## Resolution`: why the
  hooks exist (project ask rules anchor at the session's launch directory and miss worktree subagents). The change
  must not reopen that hole for the main checkout or for non-agent worktrees.
- `scripts/governance/test-ask-governed-merge.sh` case "cleanup-wave of a branch with a governed change asks" (about
  line 316): proof that the merge of a `worktree-agent-*` branch with a governed change already asks, listing the file.
- `.claude/gsd-core/references/worktree-branch-check.md` lines 18-21: agent worktrees use the `agent-<id>` branch
  namespace, previously `worktree-agent-<id>`; both are accepted. This repository's history shows directories
  `.claude/worktrees/agent-<hex>` paired with branches `worktree-agent-<hex>`.

Facts observed at planning time (HEAD 2e5cf4f):
- test-ask-governed-edit.sh: 37 passed, 0 failed. test-ask-governed-merge.sh: 120 passed, 0 failed.
  test-check-pr.sh: 132 passed, 0 failed.
- `governance/governed-paths.txt` has no entry that matches `.claude/worktrees/**`, so a path inside an agent worktree,
  taken relative to the main root, is not governed. Only `.claude/settings.json` starts with `.claude`.
- `checkClaudeAskRules` (`AskRulesCheck`, `GovernanceHooks`) checks registration, script existence and governance of
  the hook scripts, not their behaviour, so it stays green without code changes.
- docs/tooling.md line 245 names the hooks without describing per-edit behaviour, so it stays unchanged.
- Prettier uses `proseWrap: preserve`, so phrases in Markdown stay on the line they are written on.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Agent worktree edits pass the edit hook silently while every other tree keeps asking (hook and tests)</name>
  <files>scripts/governance/test-ask-governed-edit.sh, scripts/governance/ask-governed-edit.sh</files>
  <precondition>The executor runs in an agent worktree: `git worktree list --porcelain` lists its own worktree at `<main checkout>/.claude/worktrees/agent-<id>` with `branch refs/heads/worktree-agent-<id>` or `branch refs/heads/agent-<id>`, the pairing the exemption relies on. Record the observed pair in the SUMMARY; if the directory and branch are paired differently, halt and report, because the rule below would not exempt real agent worktrees.</precondition>
  <behavior>
    - Agent worktree `.claude/worktrees/agent-a` on `worktree-agent-a`: Edit of `governance/governed-paths.txt`, Write of `.claude/settings.json`, MultiEdit of `mise.toml` and NotebookEdit of `governance/a.ipynb` give no output.
    - Agent worktree `.claude/worktrees/agent-n` on branch `agent-n` (new namespace): Write of `mise.toml` gives no output.
    - Main checkout: Edit of `$REPO/mise.toml`, MultiEdit of `$REPO/mise.toml`, NotebookEdit of `$REPO/governance/a.ipynb` ask, as do all existing main-checkout cases.
    - Session started inside the agent worktree (CLAUDE_PROJECT_DIR set to it): Edit of its own `mise.toml` asks.
    - Non-agent worktrees ask: `.claude/worktrees/feature-x` on `worktree-feature-x`, `.claude/worktrees/feature-y` on `gsd/phase-9-probe`, the existing `elsewhere` worktree outside the repository, the existing unregistered `.claude/worktrees/ghost`.
    - Spoofs ask: unregistered `.claude/worktrees/agent-ghost`; `.claude/worktrees/agent-p` on `gsd/phase-9-agent-dir`; detached `.claude/worktrees/agent-d`; `.claude/worktrees/agent-m` on `worktree-agent-other`; `$TMP/agent-o` on `worktree-agent-o`; `$AGENT/../../../mise.toml`; relative `../../../mise.toml` with cwd `$AGENT`; `$AGENT/docs/escape.toml` linked to `$REPO/mise.toml`; `$AGENT/linked-main/new.json` with `linked-main` linked to `$REPO/governance`; registered `.claude/worktrees/agent-s` whose directory is replaced by a symbolic link to `$REPO`; `.claude/worktrees/AGENT-A/mise.toml`.
  </behavior>
  <action>
Implements the locked author decision of 2026-11-24 for the edit hook. Draft both files in a scratch directory outside
the repository (the session scratchpad) so drafting raises no governed-edit prompts: a `git init` mirror that holds
copies of `governance/governed-paths.txt` and `scripts/governance/` lets the test run unchanged there, including the
case that runs the hook without CLAUDE_PROJECT_DIR. Copying into the scratch mirror with shell commands is fine; moving
the final text into the worktree is not: Read each finished draft and Write it to its repository path with the Write
tool, one Write per file.

RED. Extend scripts/governance/test-ask-governed-edit.sh (keep its structure, helpers and the fail-closed and
malformed-input sections as they are):
- Setup, after the existing `agent-a` and `elsewhere` worktrees: add worktrees `.claude/worktrees/agent-n` on branch
  `agent-n`, `.claude/worktrees/feature-x` on `worktree-feature-x`, `.claude/worktrees/feature-y` on
  `gsd/phase-9-probe`, `.claude/worktrees/agent-p` on `gsd/phase-9-agent-dir`, `.claude/worktrees/agent-d` with
  `--detach`, `.claude/worktrees/agent-m` on `worktree-agent-other`, `$TMP/agent-o` on `worktree-agent-o`, and
  `.claude/worktrees/agent-s` on `worktree-agent-s` whose directory is then removed and replaced by a symbolic link to
  `$REPO`. Create the unregistered directory `.claude/worktrees/agent-ghost`, a symbolic link `$AGENT/docs/escape.toml`
  to `$REPO/mise.toml` and a symbolic link `$AGENT/linked-main` to `$REPO/governance`. Update the setup comment.
- Flip the two existing agent-worktree cases (governed-paths.txt and settings.json under `$AGENT`) to `none` and
  rename them so the name says that the merge hook, not the edit hook, asks. Retarget the existing MultiEdit and
  NotebookEdit cases to the main checkout (they keep asking and keep covering notebook_path) and add agent-worktree
  twins that expect `none`.
- Add the cases of the behavior list, grouped under section comments for agent worktrees (silent), trees that keep
  asking (main checkout, session inside an agent worktree, non-agent worktrees) and attempts to pass as an agent
  worktree. Ask cases use reason patterns that name the governed file; where the main checkout is the expected root,
  match `in .*/repo is a governed path`.
Write the test file into the worktree, run `bash scripts/governance/test-ask-governed-edit.sh` and confirm it fails and
that the failures are exactly the cases that expect an agent worktree to stay silent (five with the old hook). Commit
as `test(quick-261124-dvp): ...`.

GREEN. Change scripts/governance/ask-governed-edit.sh, reusing its parser and path functions, no second parser:
- Extend the existing loop over WORKTREE_LIST so it tracks one porcelain record at a time: the `worktree <path>` field
  starts a record, `branch refs/heads/<name>` sets its branch, `detached`, `bare` and `prunable` (with or without a
  reason) make it unusable, `locked` does not matter, and an empty field (the record separator of `-z` output, a
  doubled \001 after the tr) or the next `worktree` field or the end of the list finishes it. Keep calling add_root for
  every `worktree` path as today.
- A finished usable record with a branch goes to a new function (for example agent_worktree_record) that records the
  worktree's name in an AGENT_NAMES array only when all of these hold: lexical_path and physical_path of the path both
  succeed; both forms end in the same last component `<name>`, which starts with `agent-`, has at least one character
  after it and contains only A-Z, a-z, 0-9, dot, underscore and hyphen; each form equals exactly
  `<base>/.claude/worktrees/<name>` for some base in BASES (reuse the CLAUDE_WORKTREES constant; byte-exact, not
  case-folded); neither form equals any entry of BASES (so a session started inside an agent worktree keeps asking for
  its own tree); and the branch equals `<name>` or `worktree-<name>`. BASES is complete before the loop runs.
- After the loop, build AGENT_ROOTS as every `<base>/.claude/worktrees/<name>` for each base in BASES and each name in
  AGENT_NAMES, skipping any value that equals an entry of BASES. Adding every base spelling (as written and resolved,
  which differ on macOS where /var is /private/var) is safe because the resolved candidate is checked on its own.
- Add a predicate (for example is_agent_root) that succeeds only when its root is an element of AGENT_ROOTS, compared
  byte-exact, and the candidate starts with that root followed by a slash, also byte-exact. A case-variant spelling
  therefore never matches the exemption and keeps asking.
- In check_candidate, before each match_relative call (the ROOTS loop and the BASES loop, where the root is
  `$base$CLAUDE_WORKTREES` plus the candidate's next component), skip the call when the predicate holds for that root
  and candidate. Every other governed match still asks with the unchanged reason, and both candidates (as written and
  resolved) still run through every root, so a lexical exemption can never hide what the resolved path points to.
- Constraints: bash 3.2 and `set -u` (expand possibly empty arrays as `${ARR[@]+"${ARR[@]}"}`, as lexical_path
  already does); bash builtins only, because the no-jq case gives the hook a PATH with just cat, git, tr, readlink and
  dirname; read no environment variable for the decision; lines at most 120 columns; shellcheck-clean.
- Rewrite the header comment: the hook asks before an edit of a governed path in the main checkout and in any worktree
  that is not an agent worktree; define an agent worktree exactly as above; state that inside one the hook stays
  silent because the change reaches the main checkout only through the merge that ask-governed-merge.sh asks about,
  and that the session's settings, hooks and governed list come from CLAUDE_PROJECT_DIR (ADR-0015, author decision
  2026-11-24); list the non-agent worktrees that keep asking.
Write the hook into the worktree, run the tests under the default bash and under /bin/bash 3.2, run shellcheck, and
commit as `feat(quick-261124-dvp): ...`. If a run fails, fix the draft in the scratch mirror and Write again rather
than many small Edits.
  </action>
  <verify>
    <automated>bash scripts/governance/test-ask-governed-edit.sh | tail -n 1 | awk '{exit !($1 >= 54 && $3 == 0)}'  # [HEAD=37 EXPECT>=54 @2e5cf4f]</automated>
    <automated>env PATH="/bin:$PATH" /bin/bash scripts/governance/test-ask-governed-edit.sh | tail -n 1 | awk '{exit !($1 >= 54 && $3 == 0)}'  # [HEAD=37 EXPECT>=54 @2e5cf4f]</automated>
    <automated>grep -v '^ *#' scripts/governance/ask-governed-edit.sh | grep -c 'AGENT_ROOTS'  # [HEAD=0 EXPECT>=2 @2e5cf4f]</automated>
    <automated>bash scripts/governance/test-ask-governed-merge.sh | tail -n 1 | grep -qx '120 passed, 0 failed'  # [HEAD=120 EXPECT==120 @2e5cf4f GUARD MUTATION: any behaviour change in ask-governed-merge.sh, such as cleanup-wave no longer asking]</automated>
    <automated>mise x -- shellcheck scripts/governance/ask-governed-edit.sh scripts/governance/test-ask-governed-edit.sh</automated>
  </verify>
  <done>The RED run failed on exactly the agent-worktree silence cases and was committed; after the hook change both bash versions report at least 54 passed and 0 failed; every main-checkout, non-agent and spoof case asks; agent-worktree governed edits produce no output; shellcheck is clean; the SUMMARY records the executor's own worktree path and branch pair and the RED and GREEN counts.</done>
</task>

<task type="auto">
  <name>Task 2: Amend ADR-0015 and align ADR-0003 and ADR-0004 with the agent-worktree rule</name>
  <files>docs/adr/0015-governance-hooks-for-worktrees-and-merges.md, docs/adr/0003-governance-gate-trust-boundary.md, docs/adr/0004-gsd-configuration.md</files>
  <action>
These files are not governed, so edits raise no prompts. Amend ADR-0015 in place (it is on the Phase 2 branch, not on
`main`), keeping its H1, file name, the required MADR sections and front matter keys that checkAdrs validates. Record
the author decision of 2026-11-24 (locked):
- Front matter: date becomes "2026-11-24"; decision-makers becomes a list that keeps the existing entry and adds the
  repository author's 2026-11-24 decision on agent worktree edits.
- Context and Problem Statement: add a paragraph: once the hooks were live, quick task 261124-c1g, whose executor
  edited governed files in its agent worktree, raised dozens of prompts, one per Edit or Write, for changes that cannot
  reach the main checkout before the merge prompt, which lists them anyway; many prompts about the same change set wear
  down the attention each prompt deserves.
- Decision Drivers: reword the first driver so the author is asked before a governed change reaches the main checkout,
  per edit in the main checkout and in non-agent worktrees and once per merge for an agent worktree's work; add a
  driver that prompts stay few enough to be read.
- Considered Options: add the three options for edits inside an agent worktree: keep the per-edit ask; remember an
  approval per task with a TTL; no per-edit ask inside an agent worktree, with the merge hook as the single
  confirmation point (chosen, 2026-11-24).
- Decision Outcome: add the amendment. Explain why AIDEV-08's intent holds: the settings, hooks and governed list the
  session runs on come from `${CLAUDE_PROJECT_DIR}`, so a governed change in an agent worktree has no effect on the
  session until it is merged, and the merge prompt names every governed file. The main checkout and non-agent worktrees
  keep the per-edit ask because their changes reach a pull request branch without a merge the hook sees. State that
  the exemption belongs to the edited file's worktree, not to the caller.
- The hooks: in the ask-governed-edit.sh bullet, define an agent worktree exactly as Task 1 implements it (registered
  in `git worktree list`; on a branch, not detached, bare or prunable; path as written and resolved exactly
  `<session or main root>/.claude/worktrees/agent-<id>`; branch `agent-<id>` or `worktree-agent-<id>`; not the session
  or main root; exemption only for matches relative to that root with a byte-exact prefix; every other match and
  every resolved spelling still checked). List the categories that keep asking and why: EnterWorktree worktrees
  (`worktree-<name>`, later renamed to `gsd/phase-*`, pushed as the pull request branch without a merge into the main
  checkout), worktrees outside `.claude/worktrees/`, Workflow-tool worktrees (`worktree-wf_*`, not covered by the
  decision), agent worktrees nested under a worktree that is neither the session nor the main root, and unregistered
  directories.
- Consequences: reword the first Good bullet so the per-edit prompt applies to the main checkout and non-agent
  worktrees; add Good: one prompt per merge of an agent's work instead of one per edit; add Bad: governed edits in an
  agent worktree are not prompted when they happen; the merge prompt names the files but does not show their content,
  so the author reads the diff (for example `git diff HEAD...<branch>`) before confirming; an executor that rewrites a
  governed file it should not touch is noticed only at the merge. Update the existing Bad bullet about shell writes so
  it covers file-tool edits in an agent worktree too.
- Residual risks: add that unapproved governed edits can accumulate in an agent worktree and are gated only at the
  merge prompt and at the pull request, where `governance-success` (check-pr.sh) requires the `governance-approved`
  label (ADR-0001, ADR-0003); that the integration channels the merge hook does not see (cherry-pick, rebase,
  `checkout <ref> -- <path>`, a shell copy from the agent worktree into the main checkout) now carry file-tool edits as
  well as shell writes; that a human worktree given an agent-shaped name and branch is treated as an agent worktree;
  and that an agent's edit of the governed `.claude/settings.json` or hook scripts in its own worktree does not change
  the running session but would apply to a session started inside that worktree.
- Confirmation: describe the new test cases (agent worktrees silent; main checkout, session inside an agent worktree,
  non-agent worktrees and spoof attempts ask) and replace the live check: a worktree subagent's Write to a governed path
  of its own worktree passes without a prompt, the `worktree.cleanup-wave` merge that brings it asks once listing the
  file, and a Write to the same path in the main checkout asks.
- More Information: name quick task 261124-dvp; add revisit triggers: GSD or Claude Code bringing agent worktree work
  into the main checkout by means other than `git merge`, or agent worktrees running sessions that load their own
  project settings.

ADR-0003, Reasons bullet that starts "Claude Code's ask rules and the governance hooks": say the ask rules and hooks
cover every governed path for the agent's file-edit tools in the main checkout and in non-agent worktrees, governed
changes made in an agent worktree when a merge brings them into the main checkout, and the merges that bring changes to
one into a worktree; keep the rest of the bullet (guard rail, not a security boundary; review stays the control).

ADR-0004, step 1 of "Changing the configuration deliberately": replace the sentence on agent worktrees so it says that
in an agent worktree the edit rule does not match and the ADR-0015 edit hook does not ask at edit time, the `config-set`
rule (path-independent) still asks, and the ADR-0015 merge hook asks when the change is merged into the main checkout.

Use the phrase "agent worktree" for the concept throughout. Commit as `docs(quick-261124-dvp): ...`.
  </action>
  <verify>
    <automated>grep -v '^#' docs/adr/0015-governance-hooks-for-worktrees-and-merges.md | grep -c 'governance-approved'  # [HEAD=0 EXPECT>=1 @2e5cf4f]</automated>
    <automated>grep -v '^#' docs/adr/0015-governance-hooks-for-worktrees-and-merges.md | grep -c 'agent worktree'  # [HEAD=0 EXPECT>=3 @2e5cf4f]</automated>
    <automated>grep -c '^date: "2026-11-24"' docs/adr/0015-governance-hooks-for-worktrees-and-merges.md  # [HEAD=0 EXPECT==1 @2e5cf4f]</automated>
    <automated>grep -v '^#' docs/adr/0003-governance-gate-trust-boundary.md | grep -c 'file-edit tools in every worktree'  # [HEAD=1 EXPECT==0 @2e5cf4f]</automated>
    <automated>grep -v '^#' docs/adr/0004-gsd-configuration.md | grep -c 'asks instead'  # [HEAD=1 EXPECT==0 @2e5cf4f]</automated>
    <automated>mise x -- ./gradlew checkAdrs</automated>
  </verify>
  <done>ADR-0015 carries the 2026-11-24 decision, the three considered options, the agent worktree definition, the categories that keep asking, the new consequences and residual risks (including the `governance-approved` gate at the pull request) and the new confirmation; ADR-0003 and ADR-0004 no longer claim per-edit asks in agent worktrees; checkAdrs passes.</done>
</task>

<task type="auto">
  <name>Task 3: Align the agent rules, governed-list header, GovernanceHooks KDoc and merge hook header, then run every gate</name>
  <files>.claude/CLAUDE.md, governance/governed-paths.txt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt, scripts/governance/ask-governed-merge.sh</files>
  <action>
Describe the 2026-11-24 author decision wherever the repository states the edit hook's behaviour. Three of these files
are governed (governed-paths.txt, GovernanceHooks.kt, ask-governed-merge.sh): one Edit each, no shell writes; stop and
report if the author declines.
- .claude/CLAUDE.md, section "Agent Git Workflow": replace the bold-titled bullet that describes the two ADR-0015 hooks
  (its title begins "Governed paths ask"). New title: "Governed paths ask in the main checkout and at merge." Body: the
  edit hook asks before an Edit, Write, MultiEdit or NotebookEdit of a governed path in the main checkout and in any
  worktree that is not an agent worktree (`.claude/worktrees/agent-<id>` on branch `agent-<id>` or
  `worktree-agent-<id>`); inside an agent worktree it does not ask, and the merge hook asks once, listing the governed
  files, before a Bash command whose `git merge` or `worktree.cleanup-wave` brings them into the main checkout or
  another worktree. Keep the instructions to change governed files with those tools rather than shell writes, to pass
  the manifest and refs as literal values, and to stop and report when the author declines a prompt; add that the
  orchestrator reads the governed diff of the branch before the author confirms the merge prompt.
- governance/governed-paths.txt, header lines 4-7: the hooks of ADR-0015 ask before an edit in the main checkout or in a
  worktree that is not an agent worktree, and before a merge that brings a change to one into a worktree. Comment lines
  only; keep the existing width of about 96 columns; no entry changes.
- GovernanceHooks.kt, object KDoc: ask-governed-edit.sh asks before a file tool changes a governed path in the main
  checkout or in a worktree that is not an agent worktree, while inside an agent worktree the merge hook asks once when
  the work is merged; keep the rest of the KDoc. KDoc lines at most 120 columns (ktfmt, detekt).
- ask-governed-merge.sh, header comment lines 6-9: say that ask rules and the edit hook see only the file tools and
  that the edit hook does not ask in an agent worktree, so work that reaches a governed file in an agent worktree,
  through the file tools or a shell command (sed, a heredoc, a script), enters the main checkout through a merge, where
  this hook stands. Keep the exact phrase "the edit hook does not ask in an agent worktree" on one line. Comment change
  only; the script's behaviour stays byte-for-byte the same outside the comment.
Then run the full gate set below and fix anything it reports (formatting through the Edit tool for governed files).
Commit as `docs(quick-261124-dvp): ...`.
  </action>
  <verify>
    <automated>grep -v '^#' .claude/CLAUDE.md | grep -c 'in every worktree'  # [HEAD=1 EXPECT==0 @2e5cf4f]</automated>
    <automated>grep -v '^#' .claude/CLAUDE.md | grep -c 'governed path in any worktree'  # [HEAD=1 EXPECT==0 @2e5cf4f]</automated>
    <automated>grep -v '^#' .claude/CLAUDE.md | grep -c 'agent worktree'  # [HEAD=0 EXPECT>=1 @2e5cf4f]</automated>
    <automated>grep '^#' governance/governed-paths.txt | grep -c 'agent worktree'  # [HEAD=0 EXPECT>=1 @2e5cf4f]</automated>
    <automated>grep -c 'agent worktree' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt  # [HEAD=0 EXPECT>=1 @2e5cf4f]</automated>
    <automated>grep '^#' scripts/governance/ask-governed-merge.sh | grep -c 'the edit hook does not ask in an agent worktree'  # [HEAD=0 EXPECT==1 @2e5cf4f]</automated>
    <automated>bash scripts/governance/test-ask-governed-edit.sh | tail -n 1 | awk '{exit !($1 >= 54 && $3 == 0)}'  # [HEAD=37 EXPECT>=54 @2e5cf4f]</automated>
    <automated>bash scripts/governance/test-ask-governed-merge.sh | tail -n 1 | grep -qx '120 passed, 0 failed'  # [HEAD=120 EXPECT==120 @2e5cf4f GUARD MUTATION: any behaviour change in ask-governed-merge.sh, such as cleanup-wave no longer asking]</automated>
    <automated>bash scripts/governance/test-check-pr.sh | tail -n 1 | grep -qx '132 passed, 0 failed'  # [HEAD=132 EXPECT==132 @2e5cf4f GUARD MUTATION: a change to governed-paths.sh or the governed list that breaks check-pr.sh]</automated>
    <automated>mise x -- ./gradlew checkClaudeAskRules checkAdrs spotlessCheck :build-logic:functionalTest --tests '*AskRules*'</automated>
  </verify>
  <done>No repository text claims a per-edit ask inside agent worktrees; the four files describe the main-checkout and non-agent per-edit ask and the single merge prompt; the merge hook's behaviour is unchanged (120/120); all shell suites, checkClaudeAskRules, checkAdrs, spotlessCheck (ktfmt, shfmt, Prettier) and the AskRules functional tests pass.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| agent tool call → edit hook | The tool input's file path and the hook input's cwd are agent-controlled and untrusted |
| agent-writable file system → edit hook | An agent can create directories, symbolic links and `.git` files in its worktree and in the main checkout through shell commands |
| git worktree metadata → edit hook | `git worktree list` reflects metadata an agent can change through git commands (add, switch, detach), but not a value it can set through the tool input |
| agent worktree → main checkout | Governed content crosses here through `git merge` / `worktree.cleanup-wave` (merge hook) or through channels the merge hook does not see |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-dvp-01 | Elevation of privilege | ask-governed-edit.sh exemption | high | mitigate | Exemption only for roots in AGENT_ROOTS built from verified `git worktree list` records; candidate must start with the root byte-exact; both the as-written and the resolved candidate run through every root, so `..` paths, relative paths from an agent cwd, symbolic links into the main checkout and case-variant spellings still ask (Task 1 spoof cases) |
| T-dvp-02 | Tampering | registered agent worktree directory replaced by a symbolic link | high | mitigate | A record qualifies only when its resolved path is also exactly `<base>/.claude/worktrees/<name>` and neither form equals a base, so a link to the main checkout disqualifies it; regression case `agent-s` |
| T-dvp-03 | Spoofing | agent claims agent-worktree status | medium | mitigate | No environment variable or extra tool-input field takes part; branch must be `agent-<id>` or `worktree-agent-<id>` paired with the directory name; detached, bare and prunable records, unregistered directories and agent-shaped worktrees outside `.claude/worktrees/` keep asking (Task 1 cases) |
| T-dvp-04 | Elevation of privilege | session started inside an agent worktree | medium | mitigate | Records whose path equals the session root or main root never qualify, so the session's own tree keeps the per-edit ask (Task 1 case) |
| T-dvp-05 | Tampering | unapproved governed edits accumulate in an agent worktree and leave through channels the merge hook does not see (cherry-pick, rebase, `checkout <ref> -- <path>`, shell copy) | medium | accept | Recorded as a residual risk in ADR-0015; the pull request gate `governance-success` (check-pr.sh, `governance-approved` label) and the maintainer's review remain the control (ADR-0003); the merge hook covers `git merge` and `worktree.cleanup-wave` |
| T-dvp-06 | Elevation of privilege | agent edits its worktree's `.claude/settings.json` or hook scripts | low | mitigate | Hooks, settings and governed list load from `${CLAUDE_PROJECT_DIR}` (main checkout), so worktree copies do not affect the running session; the merge prompt lists them; a session started inside that worktree is recorded as residual risk in ADR-0015 |
| T-dvp-07 | Denial of service | hook failure while parsing records | low | mitigate | Existing EXIT trap turns any undecided exit into an ask; `onFailure: block` stays in the registration; empty arrays expanded safely for bash 3.2 under `set -u` |
| T-dvp-SC | Tampering | npm/pip/cargo installs | low | accept | No package installs in this plan; no package-legitimacy checkpoint needed |
</threat_model>

<verification>
- `bash scripts/governance/test-ask-governed-edit.sh` (default bash and `/bin/bash` 3.2 with `/bin` first on PATH):
  at least 54 passed, 0 failed.
- `bash scripts/governance/test-ask-governed-merge.sh`: 120 passed, 0 failed.
- `bash scripts/governance/test-check-pr.sh`: 132 passed, 0 failed.
- `mise x -- ./gradlew checkClaudeAskRules checkAdrs spotlessCheck :build-logic:functionalTest --tests '*AskRules*'`:
  BUILD SUCCESSFUL.
- `mise x -- shellcheck scripts/governance/ask-governed-edit.sh scripts/governance/test-ask-governed-edit.sh
  scripts/governance/ask-governed-merge.sh`: clean.
- Post-merge live check (orchestrator with the author, recommended, not part of the executor's run): once the change
  is merged, a worktree subagent's Write to a governed path of its own worktree passes without a prompt and the
  `worktree.cleanup-wave` merge of that branch asks once listing the file; discard the probe branch afterwards. The
  orchestrator's own merge of this task's branch is expected to raise exactly one merge prompt that lists the governed
  files of this plan.
</verification>

<success_criteria>
- Inside an agent worktree, governed Edit/Write/MultiEdit/NotebookEdit calls raise no edit-hook prompt; the merge of
  the agent branch raises one prompt that names the governed files.
- The main checkout, a session started inside an agent worktree, EnterWorktree and other non-agent worktrees, and every
  spoof attempt keep the per-edit ask.
- ADR-0015 records the decision, the rejected alternatives, consequences and the residual risk with the pull request
  `governance-approved` gate; the other descriptions match the new behaviour.
- All listed gates pass.
</success_criteria>

<output>
Create `.planning/quick/261124-dvp-stop-the-governance-edit-hook-from-askin/261124-dvp-SUMMARY.md` when done. Record:
the executor's own worktree path and branch pair from the precondition; RED and GREEN test counts; the governed files
written and how many prompts the author answered; any deviation.

Orchestrator note (not for the executor): after the merge, remove the "[Phase 2 ship, after 261124-c1g]" pending todo
about the edit hook from `.planning/STATE.md` when recording the quick task.
</output>
