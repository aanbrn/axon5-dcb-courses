---
phase: quick-261124-nlk
plan: 01
subsystem: tooling
tags: [doctor, git, governance, image-references, kdoc]
requires: []
provides:
  - "doctor `git` check (local only) with shell tests"
  - "TestKit cases proving indented catalog image entries are checked"
  - "git minimum recorded in ADR-0015 and docs/tooling.md; CoursesJsonModule KDoc states value-only registration"
affects: [scripts/doctor.sh, docs/tooling.md, docs/adr/0015]
key-files:
  created: []
  modified:
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
    - docs/tooling.md
    - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
    - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
decisions:
  - "IN-06 is a false positive at the regex level (catalogProblems trims each line); fixed with guard tests and KDoc, regex untouched"
  - "doctor git check is local only and fails (not warns) for missing, old (< 2.36.0) or unparsable git"
  - "docs/tooling.md names no version number; ADR-0015 holds the 2.36 requirement and reason"
requirements-completed: [TOOL-03, AIDEV-08, INFRA-09, INFRA-06]
status: complete
duration: 20min
completed: 2026-11-24
commits: 3
plan_head_before: e06607c35d58c6df3892798899949ccb74e76b73
plan_head_after: 3f9da9c3c40c8742d24b2f1adb71be4afc6224b8
actuals:
  tokens: 14000
  tasks: 3
  commits: 3
---

# Phase quick-261124-nlk Plan 01: Phase 2 review findings IN-06, IN-07, IN-08 Summary

Local-only `git` doctor check (minimum 2.36.0) with fake-git shell tests, TestKit cases that guard indented catalog image entries, and KDoc/ADR/tooling-doc updates.

`commits: 3` counts the task commits; this SUMMARY is committed after them as the last commit.

## Tasks

| Task | Finding | Commit | Result |
|------|---------|--------|--------|
| 1 | IN-08 | 04a892d | `check_git` in `scripts/doctor.sh`; fake git in `scripts/test-doctor.sh`; 10 new assertions, 155 passed, 0 failed |
| 2 | IN-06 | 474c1fc | 2 TestKit cases (10 tests in the class), KDoc-only change in `ImageReferenceCheck.kt` |
| 3 | IN-08 docs, IN-07 | 3f9da9c | `docs/tooling.md`, ADR-0015, `CoursesJsonModule` KDoc |

## Findings

- **IN-06 (false positive at the regex level):** `catalogProblems` trims every catalog line before `CATALOG_ENTRY` is matched, and the same trim lets an indented `[versions]` header through. The unpinned-indented case passed the "must fail" expectation at once, so the planned fallback regex was not applied. The two new cases (spaces, tab, indented header; indented pinned entry passes) guard the trim against later refactoring. The KDoc of the class and of `CATALOG_ENTRY` now say the line is trimmed first. `ImageReferenceCheck.kt` changed in comment lines only (verified by diff against 6ffd531).
- **IN-07:** KDoc only. `CoursesJsonModule` states the types are registered as values, not map keys, and that the first contract with an ID-keyed map adds `addKeyDeserializer` through the same `TypedIdCompanion.parse`. No code line changed; Dokka (`:serialization-jackson:check`) passes.
- **IN-08:** `GIT_MIN_VERSION=2.36.0` with a comment naming ADR-0015 and the two git calls of `ask-governed-edit.sh`. `check_git` is local only (`--ci` prints `skip  git:`), fails with a fix line for a missing, older or unparsable git, and reports `ok` otherwise. The real doctor on this machine prints `ok  git: 2.54.0 meets the minimum 2.36.0 that the governance hooks need` and no new `fail` line (the only `fail` is the pre-existing `gsd` line, because a worktree lacks the untracked GSD installation; the Step 0 baseline had the same line).
- **Unchanged on purpose:** `ComposeVersions` reads the parsed catalog (`catalog.versionAliases`), not text, and `ComposeVersionsFunctionalTest.IMAGE_ENTRY` already allows `^[ \t]*` indentation. ADR-0011 and `.github/workflows/ci.yml` list the doctor checks non-exhaustively and stay as they are (`.github/**` is governed).

## Deviations from Plan

None - plan executed exactly as written. Task 2 needed two hand-applied ktfmt diffs reported by `spotlessCheck` (list-item indentation and `.joinToString` placement in the new test, KDoc re-wrap in `ImageReferenceCheck.kt`), which the plan anticipated. The baseline file for the doctor comparison was kept in the session scratchpad instead of `$TMPDIR`.

## Verification

- `bash scripts/test-doctor.sh`: 155 passed, 0 failed (baseline 145).
- `mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh`: clean.
- `mise x -- ./gradlew :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :build-logic:detekt :serialization-jackson:check checkImageReferences checkToolingInventory checkAdrs spotlessCheck`: BUILD SUCCESSFUL; the class has 10 passing tests.
- Plan greps: `docs/tooling.md` holds no `2.36` and the exact phrase twice; ADR-0015 holds `2.36` and `test-doctor`; the guard diffs for `ImageReferenceCheck.kt` and `CoursesJsonModule.kt` show comment lines only.

## For the orchestrator

- Record IN-06, IN-07 and IN-08 in `02-REVIEW-DISPOSITION.md` with the commit hashes above (IN-06: 474c1fc, a false positive guarded by tests; IN-07: 3f9da9c; IN-08: 04a892d and 3f9da9c).
- The merge prompt at cleanup will list `scripts/doctor.sh` and `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt` as governed files. Both were edited with the Edit tool only.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

All seven modified files exist and commits 04a892d, 474c1fc and 3f9da9c are on the branch.
