---
phase: quick-261124-oyk
plan: 01
subsystem: tooling-governance-serialization
tags: [doctor, governance-hook, renovate, image-check, jackson, review-fix]
requires: []
provides:
  - doctor reports a failing git, mise or claude --version instead of dying silently
  - merge hook prompt names the real reason for a dynamic shell script word
  - checkImageReferences equals the Renovate image manager, guarded by a parity test
  - CoursesJsonModule KDoc states the observed ID map-key behaviour
affects: [scripts/doctor.sh, scripts/governance, build-logic, backend/serialization-jackson]
tech-stack:
  added: []
  patterns:
    - fakes that fail on a marker file in test-doctor.sh
    - regex read from renovate.json5 in a functional test (declared input)
key-files:
  created: []
  modified:
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - scripts/governance/ask-governed-merge.sh
    - scripts/governance/test-ask-governed-merge.sh
    - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
    - build-logic/build.gradle.kts
    - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
    - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
key-decisions:
  - "WR-02 key form (raw UUID vs TypeID for ID map keys) stays an open decision, taken with an ADR in the phase whose contract first holds an ID-keyed map"
  - "IN-01 uses one always-true reason; the hook stays fail-closed"
  - "WR-03 parity is one-directional: every accepted entry is read by the manager with its exact name, tag and digest"
requirements-completed: [TOOL-03, AIDEV-08, INFRA-09, CICD-04, INFRA-06]
status: complete
completed: 2026-11-24
commits: 4
plan_head_before: 0e0e763a09113c52a111ade2327f21b3be782ea5
plan_head_after: 9392b29c64fc8c659aa0d3ae7a6ec8fded8ea484
actuals:
  tokens: 40000
  tasks: 3
  commits: 4
---

# Quick 261124-oyk: Fix Phase 2 review findings WR-01..WR-03 and IN-01 Summary

Four review findings fixed, each in its own commit with its tests: doctor survives a failing `--version`, the merge hook
names the real reason for a dynamic shell word, `checkImageReferences` accepts exactly what the Renovate image manager
reads (proved by a parity test against the live `renovate.json5`), and the `CoursesJsonModule` KDoc states the observed
ID map-key behaviour, pinned by two tests.

## Commits (one per finding)

| Finding | Commit | Message |
| ------- | ------ | ------- |
| WR-01 | 5781180 | fix: doctor survives a failing --version of git, mise and claude |
| IN-01 | 58ccbb2 | fix: merge hook names the real reason for a dynamic shell script word |
| WR-03 | b61e980 | fix: checkImageReferences accepts exactly what the Renovate image manager reads |
| WR-02 | 9392b29 | docs: state the observed ID map-key behaviour of CoursesJsonModule and pin it |

## What changed

- **WR-01** (`scripts/doctor.sh`, `scripts/test-doctor.sh`): the three version reads (`mise`, `claude`, `git`) end with
  `|| installed=''`, so a failing tool gives an empty value that the existing guards report, instead of `set -e` ending
  the run. The git report gets a second fix line, `xcode-select --install`. Fakes gained marker files (`git-broken`
  fails every call like the macOS shim, `mise-version-fails`, `claude-fails`) and one case each. Suite: 155 -> 167 passed.
- **IN-01** (`ask-governed-merge.sh`, its test, ADR-0015): the dynamic-word branch before the script now reports
  `the script or the options of <shell> come from a shell expansion: <word>`. Decision unchanged (ask). Cases for
  `bash $SCRIPT`, `bash "$HOME/run.sh"`, the updated `$OPTS -c` case and a silent literal `bash run.sh`. ADR-0015 states
  that a script file named by a shell expansion asks. Suite: 166 -> 169 passed.
- **WR-03** (`ImageReferenceCheck.kt`, functional test, `build-logic/build.gradle.kts`): the check now reports quoted
  keys, keys outside `image-[a-z0-9-]+`, separators other than exactly one space on each side of `=`, and registry
  ports, each naming the key. `PINNED_REFERENCE` lost its registry-port group. The KDoc states the shared form.
  `renovate.json5` is a declared `functionalTest` input. Five new tests (10 -> 15): accepted shapes, rejected keys,
  rejected separators, registry port, and the parity test that compiles the regex from `renovate.json5`.
  `renovate.json5` and `gradle/libs.versions.toml` are unchanged.
- **WR-02** (`CoursesJsonModule.kt` KDoc, `JsonRoundTripTest.kt`): only KDoc lines changed in the module. Two tests
  pin the behaviour (14 -> 16 tests).

## Planner findings, confirmed during execution

- WR-01 siblings: `check_mise` and `check_claude_code` had the same unguarded read and were fixed with it.
- Registry port: the manager regex does match `localhost:5000/app:1.0@sha256:...` but reads image `localhost` and tag
  `5000/app:1.0`. The parity test proves this, so the check rejects a port with its own message. Parity is
  one-directional: accepted implies read correctly.
- IN-01: a single reason that is true in every case (the word may be an option such as `-c` or the script file).
- WR-02 observation: matches the review. An ID map key is written as the raw UUID (`{"<uuid>":1}`, no `course_`),
  read back as `Map<CourseId, Int>`, and a TypeID key fails with a `JacksonException`. No difference to report.

## Deviations from Plan

**1. [Rule 1 - Bug in the plan's sample] `image-dot.ted` cannot go through TestKit**
- **Found during:** Task 2 GREEN run.
- **Issue:** Gradle rejects the catalog before the check runs (`entry 'image-dot.ted' is not a valid alias`, dots
  create nested TOML entries), so a TestKit run with that key fails on the wrong thing.
- **Fix:** the dotted key stays a rejected sample for the parity test (the regex does not match it) and is dropped from
  the TestKit key list; commented in the test as `DOTTED_KEY`. The check's `CATALOG_ENTRY` still detects dotted keys and
  reports them with the bare-key message.
- **Files modified:** `ImageReferenceCheckFunctionalTest.kt`
- **Commit:** b61e980

**2. [Rule 3 - Blocking] detekt `DestructuringDeclarationWithTooManyEntries`**
- **Found during:** Task 2 detekt run.
- **Issue:** the five-entry destructuring of the widened `CATALOG_ENTRY` is not allowed (max 3).
- **Fix:** named groups (`quote`, `key`, `before`, `after`, `value`) read through a small `MatchResult.group(name)`
  helper.
- **Files modified:** `ImageReferenceCheck.kt`
- **Commit:** b61e980

**3. Spotless on governed files.** `spotlessCheck` reported diffs in the governed `ImageReferenceCheck.kt`. To honour
the rule of no `spotlessApply` on governed files, the diff was computed on a temporary non-governed copy
(Spotless IDE hook, copy deleted afterwards) and applied to the governed file by hand with Edit. The non-governed test
file was formatted through the same IDE hook.

## Verification

- `bash scripts/test-doctor.sh`: 167 passed, 0 failed
- `bash scripts/governance/test-ask-governed-merge.sh`: 169 passed, 0 failed
- `shellcheck` on the four scripts: clean
- `mise x -- ./gradlew :build-logic:check :serialization-jackson:check checkImageReferences checkAdrs spotlessCheck`:
  green (see the orchestrator note in the final message for the whole run)
- Real doctor in the worktree: no fail line beyond the baseline (`gsd: .claude/gsd-file-manifest.json is missing`,
  which the worktree lacks) and it still prints its `summary:` line

## For the orchestrator

- Record WR-01, WR-02, WR-03 and IN-01 as fixed in `02-REVIEW-DISPOSITION.md` with the commit hashes above.
- Candidate STATE.md pending todo: the WR-02 key-form decision (ADR in the phase that first puts an ID-keyed map in a
  contract).
- Governed files the cleanup merge prompt will list: `scripts/doctor.sh`, `scripts/governance/ask-governed-merge.sh`,
  `scripts/governance/test-ask-governed-merge.sh`,
  `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt`,
  `build-logic/build.gradle.kts`.

## Known Stubs

None.

## Threat Flags

None. No new endpoint, auth path or trust-boundary surface.

## Self-Check: PASSED

All four commits exist (5781180, 58ccbb2, b61e980, 9392b29); all ten files in `key-files.modified` changed.
