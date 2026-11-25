---
phase: quick-261124-kh6
plan: 01
subsystem: governance
tags: [governance, hooks, bash, adr-0015, doctor, upstream-records]
requires: []
provides:
  - merge hook that reads shell option clusters, wrapper option clusters, GIT_DIR-style variables and brace expansion
  - merge hook ref-mover rule and deduplicated, capped reasons
  - edit hook that exempts a `**/` entry match inside an agent worktree
  - doctor that validates the GSD pin before it builds an install command
affects: [AIDEV-08, AIDEV-07, UPS-01, UPS-03]
tech-stack:
  added: []
  patterns:
    - "lexer shadow of the unquoted text of a word, for brace expansion"
    - "read_wrapper_option: one parser of short-option clusters and long options for env, sudo and doas"
    - "unique_messages: linear printf/tr/awk dedupe, fail closed to the messages as given"
key-files:
  created: []
  modified:
    - scripts/governance/ask-governed-merge.sh
    - scripts/governance/test-ask-governed-merge.sh
    - scripts/governance/ask-governed-edit.sh
    - scripts/governance/test-ask-governed-edit.sh
    - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - docs/upstream/0001-decision-capture-extension-point.md
    - docs/upstream/0004-tag-resolver-value-classes.md
key-decisions:
  - "Brace expansion that assembles the merge word inside one word, and a merge option value that splits into several words, stay outside the hook and are recorded in ADR-0015 Residual risks"
  - "The ref-mover rule is order-independent and fails closed"
  - "The Edit(/**/<name>) ask rules keep prompting inside agent worktrees; recorded as a residual risk, not changed"
  - "ADR-0004 and .claude/CLAUDE.md stay unchanged: ADR-0004 line 49 concerns the root-anchored .planning/config.json rule, and CLAUDE.md speaks only of the hooks"
requirements-completed: [AIDEV-08, AIDEV-07, UPS-01, UPS-03]
duration: ~45min
completed: 2026-11-24
status: complete
estimate:
  tokens: 140000
  raw_tokens: 140000
  tasks: 3
  confidence: low
actuals:
  tokens: 60000
  tasks: 3
  commits: 4
plan_head_before: 7474264933db21eac0485d7597565aa7e5db390c
plan_head_after: dd73bcf88134880f7a09b3a4b184371ca9f55032
---

# Phase quick-261124-kh6 Plan 01: Fix Phase 2 review findings Summary

Merge hook hardened against strict-mode shell options, wrapper option clusters, GIT_DIR-style variables, brace expansion and ref movers in the same command, with deduplicated reasons; edit hook exempts `**/` entry matches inside agent worktrees; doctor validates the GSD pin; UP-0001 and UP-0004 record the contribution decision.

## What was done

- **Task 1 (tracer), commit 9f8be86.** CR-01, WR-01, WR-02. `analyze_shell` parses options the way bash does (`o`/`O` letters take values, `--rcfile`/`--init-file` take a file, unknown long options and dynamic option words ask). `read_wrapper_option` replaces the exact-word matching of the `env`, `sudo` and `doas` loops: clusters, attached values, `--chdir=`, `env -iS'...'`. `git --attr-source` takes a value. `GIT_DIR`, `GIT_WORK_TREE`, `GIT_COMMON_DIR` and `GIT_CEILING_DIRECTORIES` in an assignment, `export`, `declare`, `typeset`, `local` or `readonly` ask when the command merges. The lexer keeps a per-word shadow of the unquoted text and marks a word with an unquoted `{a,b}` or `{a..b}` as dynamic; `analyze_embedded` asks about a dynamic argument that names git or a merge. 33 new cases.
- **Task 2, commits 0534a85 and 996829c.** WR-03 (`ref_mover_name`, `REF_MOVER`, `MERGE_SEEN`; fetch, pull, checkout, switch, reset, rebase, update-ref, symbolic-ref, replace, bisect, filter-branch, remote; forced branch/tag; `stash branch`; `worktree add`), the dedupe (`unique_messages`, `MAX_PROBLEMS=10`, `; and N more`), WR-04 (`below_agent_root` in the edit hook), and ADR-0015 (hook description, consequences, residual risks, confirmation). 13 new merge cases (one existing case split in two), 4 new edit cases.
- **Task 3, commit dd73bcf.** WR-05 (`check_gsd` validates the pin as `N.N.N` before `install_fix` exists, message does not echo the pin; 6 new assertions) and WR-06 (UP-0001 and UP-0004 state the 2026-11-24 decision and name the Phase 2 pull request).

## Verification

- test-ask-governed-merge.sh: 166 passed, 0 failed (default bash and `/bin/bash` 3.2)
- test-ask-governed-edit.sh: 62 passed, 0 failed (default bash and `/bin/bash` 3.2)
- test-doctor.sh: 145 passed, 0 failed
- test-check-pr.sh: 173 passed; test-list-pr-files.sh: 25 passed
- shellcheck on the six changed scripts: clean
- `./gradlew checkClaudeAskRules checkUpstreamRecords checkAdrs spotlessCheck`: BUILD SUCCESSFUL
- The lefthook pre-commit hook (format, gitleaks, reuse, shellcheck) ran on every commit.

## Deviations from Plan

None - plan executed exactly as written. Two small test-side adjustments: the new dedupe cases build their regexes in double quotes to keep shellcheck SC2016 quiet, and the merge hook header comment was updated for the ref-mover rule in the Task 2 commit (the plan asked for header updates in Task 1 only for the Task 1 forms).

## Auth gates

None.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or file access pattern; the changes tighten existing hooks.

## For the orchestrator

- Remove the STATE.md pending todo "Deduplicate the reasons of the governance merge hook" (done in 0534a85).
- Record CR-01, WR-01, WR-02 (9f8be86), WR-03 (0534a85), WR-04 (996829c), WR-05 and WR-06 (dd73bcf) as fixed in `02-REVIEW-DISPOSITION.md` with these hashes (the hashes are branch hashes; use the merged ones if the merge rewrites them).
- The merge prompt at cleanup will list `scripts/governance/*`, `scripts/doctor.sh` and `docs/adr/*` as governed files.
- `commits: 4` counts the code commits; the SUMMARY commit that follows is the fifth commit of the branch.

## Self-Check: PASSED

- 9f8be86, 0534a85, 996829c, dd73bcf are ancestors of HEAD; all nine files in `key-files.modified` exist and differ from the base.
