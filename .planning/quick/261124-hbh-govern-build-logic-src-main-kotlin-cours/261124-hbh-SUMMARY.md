---
phase: quick-261124-hbh
plan: 01
subsystem: governance
tags: [governance, governed-paths, detekt, convention-plugins, ask-rules]
requires:
  - phase: quick-261124-gju
    provides: the governed list of required gate configs, whose SUMMARY flagged courses.kotlin-library
provides:
  - courses.kotlin-library.gradle.kts governed (81 entries, 82 ask rules)
  - the convention-plugin detekt residual retired in every living record
affects: [governance/governed-paths.txt, .claude/settings.json, ADR-0003, 02-SECURITY.md]
tech-stack:
  added: []
  patterns: [governed entry + ask rule + boundary case kept in step]
key-files:
  created: []
  modified:
    - governance/governed-paths.txt
    - .claude/settings.json
    - scripts/governance/test-check-pr.sh
    - docs/adr/0003-governance-gate-trust-boundary.md
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md
key-decisions:
  - "courses.kotlin-library is governed for its gate wiring (detekt, allWarningsAsErrors, courses.licences, Dokka failOnWarning), not for governance-task reach; sibling module convention plugins stay outside"
  - "The remaining residual is a module's own build script switching off its own detekt task, licence task or compiler options"
duration: 25min
completed: 2026-11-24
status: complete
commits: 2
plan_head_before: 1cfd67bdb694803544e7f72c05ddcff021ae3766
plan_head_after: 3099b8d473045df95b86210e30d8a06103f7139a
actuals:
  tokens: 5000
  tasks: 2
  commits: 2
---

# Phase quick-261124-hbh Plan 01: Govern courses.kotlin-library Summary

A pull request touching `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` now needs `governance-approved`,
Claude Code asks before editing it, and the accepted residual for dropping detekt through a module convention plugin is
retired from the living records.

`plan_head_after` is the Task 1 commit: this SUMMARY is committed together with the Task 2 commit, so that commit's own
hash cannot be written into the file. `commits: 2` counts both task commits.

## What changed

- Task 1 (`3099b8d`): test-first. The existing boundary case for the file flipped to `governed` (RED: 172 passed, 1
  failed), two `ungoverned` cases pin `courses.test-suites` and `courses.architecture-tests`. Then the list entry in its
  own commented block after `config/detekt/**` (80 to 81), the single ask rule after `Edit(/config/detekt/**)` (81 to
  82), and the header: boundary rule paragraph, rewritten module-convention-plugin bullet (the sentence saying the
  `courses.licences` apply line stays outside is gone), and the per-module residual in the accepted-residual paragraph.
  GREEN: 173 passed, 0 failed.
- Task 2: ADR-0003 consequence bullet; the paragraph after the Accepted Risks Log in 02-SECURITY.md now names only the
  per-module route and points at this quick task. No register or risk-log row changed.

## Verification

- test-check-pr.sh 173 passed; test-ask-governed-edit.sh 58 passed; test-ask-governed-merge.sh 120 passed
- `checkClaudeAskRules`, `:build-logic:functionalTest --tests '*GovernedPaths*' --tests '*AskRules*'`, `checkAdrs`,
  `spotlessCheck`, shellcheck, editorconfig-checker: all green
- Renovate invariant (`renovate --platform=local --dry-run=extract`, deps extracted from the file): prints `0`, so the
  renovate[bot] version-only exemption cannot reach the file; `NEVER_EXEMPT_PATHS` and the exemption logic are unchanged

## Historical records left unchanged (they record the boundary as it was when they ran)

- 02-20-PLAN.md rows T-02-58 and T-02-60
- the 261124-gju PLAN and SUMMARY
- STATE.md lines 119 and 138 (orchestrator-owned; line 138, the open decision, is for the orchestrator to remove)

ADR-0012 stays unchanged: its "Governed gate" bullet remains accurate and never recorded the residual.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- governance/governed-paths.txt, .claude/settings.json, scripts/governance/test-check-pr.sh, ADR-0003, 02-SECURITY.md
  modified as listed; commit 3099b8d present on the branch.
