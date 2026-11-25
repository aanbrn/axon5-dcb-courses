---
phase: quick-261124-c1g
plan: 01
subsystem: governance
tags: [governance, hooks, bash, adr-0015, testkit, review-followup]
requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: ADR-0015 merge hook, ComposeVersions generator, 02-REVIEW.md findings WR-07..WR-10
provides:
  - merge hook with a linear normalised early probe, arithmetic-aware lexer, stdin/here-string/env -S/sudo/doas handling and an embedded git/shell rule
  - 57 new regression cases in test-ask-governed-merge.sh (63 -> 120)
  - ADR-0015 that states what the hook recognises, the consequence it supports and the residual shapes
  - ComposeVersionsFunctionalTest independent of the image-* entries of the repository catalog
  - WR-07..WR-10 closed in 02-REVIEW-DISPOSITION.md (open: 3)
affects: [governance, build-logic functional tests]
tech-stack:
  added: []
  patterns:
    - "Hook early exit: fork-free case, then one sed and one tr; never bash pattern substitution on the whole input (quadratic in bash 3.2)"
    - "Whole-command mention flag decides whether text from a shell expansion or from standard input matters"
key-files:
  created: []
  modified:
    - scripts/governance/ask-governed-merge.sh
    - scripts/governance/test-ask-governed-merge.sh
    - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md
key-decisions:
  - "The probe deletes the three-character JSON line-continuation sequence (backslash backslash backslash n), not every backslash-n pair, so the n of a hidden cleanup-wave word survives"
  - "A dynamic assignment word (x=$((1<<2))) is an assignment, not a program named by an expansion"
  - "ADR-0015 is amended in place (unmerged phase branch); no superseding ADR"
patterns-established:
  - "Each fixed hook bypass has a regression case that failed first (RED observed for every new ask case)"
requirements-completed: [AIDEV-08, INFRA-09]
status: complete
duration: 55min
completed: 2026-11-24
estimate:
  tokens: 150000
  raw_tokens: 150000
  tasks: 3
  confidence: low
actuals:
  tokens: 75000
  tasks: 3
  commits: 5
plan_head_before: ae9067cc57d3234caaa2cf9da87812cd770a2b21
plan_head_after: c7ca5e903f0390ef71c60439f610676a2ed77c7c
---

# Phase quick-261124-c1g Plan 01: Fix review findings WR-07..WR-10 Summary

**ADR-0015 merge hook hardened against quote-split merge words, scripts handed to a shell, and arithmetic `<<`, with a linear early probe and a fixture-owned image set for the compose-versions TestKit proof.**

## Performance

- **Tasks:** 3 of 3
- **Commits:** 5 (WR-07, WR-09, WR-08, WR-10, ledger)
- **Files modified:** 5

## Accomplishments

- WR-07: the early probe now searches the command again with quotes, backslashes, `$` and line continuations removed, through one `sed` and one `tr`. A 150 KB command that never mentions a merge is judged in under 1 s under /bin/bash 3.2 (the case asserts under 10 s). `mentions_merge` applies the same normalisation.
- WR-09: `$((...))` and `((...))` in command position (also after `if`, `while`, `for` and similar) are arithmetic, so `<<` there is no heredoc. A heredoc without its delimiter line, an arithmetic that closes with a single `)` and one that never closes make the lexing incomplete.
- WR-08: shells that read standard input (pipe, `-s`, `/dev/stdin`, process substitution, heredoc behind a wrapper) and `source` ask when the command mentions a merge. A here-string is analysed exactly. `sudo`, `doas` and `env -S` are unwrapped, `command -v` runs nothing, and programs whose arguments run git or `bash -c` (`mise x --`, `stdbuf`, `caffeinate`, `script`, `coproc`, `watch`) are analysed. A script, eval text, program or git subcommand from a shell expansion asks when the whole command mentions a merge.
- ADR-0015 now describes exactly the recognised forms, limits the "caught when it is merged" consequence to analysed-or-asked forms, adds a Bad bullet for fail-closed prompts and lists the residual shapes (expansion-built or ANSI-C-built merge word, script files, stdin shells behind unknown wrappers).
- WR-10: the fixture catalog replaces every real `image-*` entry with two fixture compose images (declared in reverse name order) and a fixture reaper; no expectation names a repository image.

## Task Commits

1. **Task 1a: WR-07 probe and mentions_merge** - `01bf991` (fix)
2. **Task 1b: WR-09 arithmetic-aware lexer, open heredocs** - `bd589d0` (fix)
3. **Task 2: WR-08 stdin shells, here-strings, wrappers, embedded git, ADR-0015** - `55247cc` (fix)
4. **Task 3a: WR-10 fixture-owned image set** - `6a06a26` (test)
5. **Task 3b: ledger** - `c7ca5e9` (docs)

## Verification

- `bash scripts/governance/test-ask-governed-merge.sh`: 120 passed, 0 failed (also under bash 5.3). `test-ask-governed-edit.sh`: 37 passed. `test-check-pr.sh`: 132 passed, 0 failed (the plan said 139 from the review; the suite grew no failures).
- `shellcheck` clean on both scripts; `spotlessCheck`, `checkAdrs`, `checkClaudeAskRules` pass; `:build-logic:functionalTest --tests '*ComposeVersionsFunctionalTest*'` ran 3 tests, 0 failures; `editorconfig-checker` and `reuse lint` clean.
- The planning probe list (35 shapes) now asks in every case except the documented residual `git mer${E}ge`; the false-positive guards stay silent.
- RED was observed: every new `ask` case failed before the hook change (11 of the WR-07/WR-09 cases, then 30 of the WR-08 cases).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The planned probe filter had a bypass of its own**
- **Found during:** Task 1
- **Issue:** The plan said to delete every JSON backslash-n pair first. A shell reads `clea\nup-wave` as `cleanup-wave`, and in JSON that is `\\n`; deleting the `\n` pair removes the `n` and the probe misses the word.
- **Fix:** delete only the three-character sequence backslash backslash backslash n (a line continuation) before deleting quotes, backslashes and `$`. Added the regression case "a backslash before the n of cleanup-wave asks".
- **Files modified:** scripts/governance/ask-governed-merge.sh, scripts/governance/test-ask-governed-merge.sh
- **Commit:** 01bf991

**2. [Rule 3 - Blocking] A dynamic assignment word must not count as a program**
- **Found during:** Task 2
- **Issue:** With the whole-command mention flag, `x=$((1<<2))` followed by `git merge governed` would have raised "program named by a shell expansion" for the word `x=`, breaking the exact-reason expectation of the WR-09 case.
- **Fix:** analyze_words skips every word that matches the assignment pattern, dynamic or not.
- **Commit:** 55247cc

**3. [Rule 2 - Missing critical] Small additions in the same files**
- `analyze_shell` advances past `--` (it used to take `--` itself as the script); sudo long value options and `-D`/`--chdir` (cwd becomes unknown) are handled; the no-jq test PATH gained `sed`.
- A case for `x=$((1<<2))` next to `git merge-base` and one for a for-loop `((...))` guard the false-positive side.

**4. Commit split**
- The WR-09 test cases were held back from the WR-07 commit (removed, then re-added) so each commit passes its own suite.

### Notes

- Commit trailers use the plan's line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; the session attribution reminder names a different model. The plan's explicit instruction was followed.
- The optional plugin skills named in the agent skills list (axoniq, gradle) were not loaded: the work touched shell hooks, an ADR and one TestKit test, none of which they cover.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or trust-boundary file access.

## Residual (documented in ADR-0015)

- A merge word or git subcommand built by a parameter expansion, a command substitution or ANSI-C escapes leaves at the probe.
- Script files run by a shell or `source`.
- A shell that reads standard input behind a program the hook does not know as a wrapper.

## Self-Check: PASSED

All five files exist and the commits 01bf991, bd589d0, 55247cc, 6a06a26 and c7ca5e9 are on the branch (`git rev-list --count ae9067c..HEAD` = 5).
