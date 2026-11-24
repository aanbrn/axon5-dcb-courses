---
phase: quick-261124-dvp
plan: 01
subsystem: governance
tags: [claude-code-hooks, worktrees, governance, bash, adr]
requires:
  - phase: quick-261123-mub
    provides: ADR-0015 edit and merge hooks
provides:
  - "ask-governed-edit.sh stays silent for governed edits inside verified agent worktrees"
  - "ADR-0015 amended with the 2026-11-24 author decision"
affects: [governance, agent-worktrees, merge-hook]
tech-stack:
  added: []
  patterns:
    - "Exemption derived only from git worktree list and the file system, byte-exact root prefix, fail closed"
key-files:
  created: []
  modified:
    - scripts/governance/ask-governed-edit.sh
    - scripts/governance/test-ask-governed-edit.sh
    - scripts/governance/ask-governed-merge.sh
    - governance/governed-paths.txt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt
    - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
    - docs/adr/0003-governance-gate-trust-boundary.md
    - docs/adr/0004-gsd-configuration.md
    - .claude/CLAUDE.md
key-decisions:
  - "Agent worktree = registered, on a branch, path as written and resolved exactly <session or main root>/.claude/worktrees/agent-<id>, branch agent-<id> or worktree-agent-<id>, not the session or main root"
  - "Exemption only for matches relative to that root with a byte-exact prefix; every other root and both path spellings still ask"
  - "The merge hook stays the single confirmation point for agent worktree work; it is unchanged"
requirements-completed: [AIDEV-08]
duration: about 1h
completed: 2026-11-24
status: complete
actuals:
  tokens: 10500
  tasks: 3
  commits: 4
plan_head_before: b2fc8fbba2543ff681b90d794c289e77cf21180b
plan_head_after: "tip of branch worktree-agent-a1e70d2e51b9d10b7 (the commit that adds this file)"
---

# Phase quick-261124-dvp Plan 01: Stop the governance edit hook asking inside agent worktrees Summary

Edit hook that skips governed matches relative to a verified agent worktree (registered, on `agent-<id>` or
`worktree-agent-<id>`, path exactly `<root>/.claude/worktrees/agent-<id>` as written and resolved) and keeps the per-edit
ask for the main checkout, the session root, every non-agent worktree and every spoof attempt.

## Precondition

The executor's own worktree: `/Users/afanasyev/Development/axon5-dcb-courses/.claude/worktrees/agent-a1e70d2e51b9d10b7`
on branch `worktree-agent-a1e70d2e51b9d10b7`, listed by `git worktree list --porcelain` under the main checkout at
`.claude/worktrees/agent-<id>` paired with `branch refs/heads/worktree-agent-<id>`. The pairing the rule relies on holds.
A live run of the new hook with `CLAUDE_PROJECT_DIR` set to the main checkout gave no output for a governed path of this
worktree and an ask for the same path in the main checkout.

## Tasks

| Task | Name                                                | Commit  | Files                                                                                               |
| ---- | --------------------------------------------------- | ------- | --------------------------------------------------------------------------------------------------- |
| 1a   | RED: tests for agent worktrees, main checkout, spoofs | 91c5e73 | scripts/governance/test-ask-governed-edit.sh                                                        |
| 1b   | GREEN: hook exemption                               | 7b332d0 | scripts/governance/ask-governed-edit.sh                                                             |
| 2    | ADR-0015 amendment, ADR-0003, ADR-0004              | 2ee079f | docs/adr/0015-..., docs/adr/0003-..., docs/adr/0004-...                                             |
| 3    | CLAUDE.md, governed list header, KDoc, merge header | final commit of this branch | .claude/CLAUDE.md, governance/governed-paths.txt, GovernanceHooks.kt, ask-governed-merge.sh, this SUMMARY |

## Test counts

- RED (new tests, old hook): 50 passed, 5 failed. The five failures are exactly the cases that expect an agent worktree
  to stay silent: governed-paths.txt, settings.json, MultiEdit and NotebookEdit twins under `agent-a`, and a Write under
  `agent-n` (branch `agent-n`).
- GREEN (new hook): 55 passed, 0 failed under the default bash and under `/bin/bash` 3.2.57 (plan minimum 54; 37 at HEAD).
- Unchanged suites: `test-ask-governed-merge.sh` 120 passed, 0 failed; `test-check-pr.sh` 132 passed, 0 failed.
- Gates: `shellcheck` clean on the three scripts; `checkClaudeAskRules`, `checkAdrs`, `spotlessCheck` and
  `:build-logic:functionalTest --tests '*AskRules*'` BUILD SUCCESSFUL.

## Governed files written

Five governed files in the worktree, all through the Write or Edit tool, none through shell writes: Write on
`scripts/governance/test-ask-governed-edit.sh` and `scripts/governance/ask-governed-edit.sh` (one call each, drafted in a
scratch mirror outside the repository), Edit on `scripts/governance/ask-governed-merge.sh`,
`governance/governed-paths.txt` and `GovernanceHooks.kt` (one call each). No call was declined. The executor cannot see
how many prompts the author answered; the run raised no error or denial on any of the five.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Scratch mirror is a git root, so the worktree path guard refused a Write into it**
- **Found during:** Task 1 (RED draft)
- **Issue:** `gsd-worktree-path-guard` denied a Write to a file inside the scratch `git init` mirror because it resolves
  to another git root than the active worktree.
- **Fix:** Drafted the two scripts with the Write tool in a scratch directory that is not a git repository, then copied
  them into the mirror with `cp` (allowed by the plan for the mirror). Nothing was written into the worktree by shell.
- **Files modified:** none in the repository

**2. [Rule 1 - Bug] ADR-0015 front matter must be a single-line flow list**
- **Found during:** Task 2 (`checkAdrs`)
- **Issue:** The multi-line `decision-makers` list that Prettier produces for long lists is rejected by `checkAdrs`
  (`decision-makers has no value`).
- **Fix:** One line of at most 120 columns: `["repository author (debug session ask-rules-skip-worktree-agents)", "repository author (2026-11-24)"]`.
  The existing entry is kept as it was.
- **Files modified:** docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
- **Commit:** 2ee079f

**3. [Judgement] No invented rejection rationale for the TTL option**
- ADR-0015 records the "remember an approval per task with a time to live" option as "Rejected by the author." without a
  reason, because the plan states the rejection but no reason; the "keep the per-edit ask" option carries the prompt-count
  reason from the Context section.

The plan's test minimum was 54; the behaviour list produced 55 cases (37 existing plus 18 new).

## Auth gates

None.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or file-access pattern beyond the hook's own worktree-list parsing, which
threat T-dvp-01 to T-dvp-07 already cover.

## Self-Check: PASSED

- Files exist: ask-governed-edit.sh, test-ask-governed-edit.sh, ADR-0015, ADR-0003, ADR-0004, CLAUDE.md,
  governed-paths.txt, GovernanceHooks.kt, ask-governed-merge.sh.
- Commits 91c5e73, 7b332d0 and 2ee079f are ancestors of HEAD.
