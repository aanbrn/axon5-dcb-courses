---
phase: 01-repository-build-ci-foundation
plan: 16
subsystem: infra
tags: [governance, bash, kotlin, gradle, testkit, claude-code]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "check-pr.sh label gate (01-13, 01-15) and the AskRulesCheck Gradle task (01-13)"
provides:
  - "One governed-paths grammar implemented in check-pr.sh load_globs and in the internal GovernedPaths.parse"
  - "Comment lines in governance/governed-paths.txt, ready for plan 01-17"
  - "The same nine-case table tested in test-check-pr.sh and GovernedPathsFormatFunctionalTest"
  - "WR-04 recorded fixed"
affects: [01-17, governance, ci]

actuals:
  tokens: 14000
  tasks: 2
  commits: 4

tech-stack:
  added: []
  patterns:
    - "A grammar shared by a shell script and a build-logic parser is cross-referenced in both and tested with one case table on both sides"

key-files:
  created:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPaths.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPathsFormatFunctionalTest.kt
  modified:
    - scripts/governance/check-pr.sh
    - scripts/governance/test-check-pr.sh
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
    - .planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md

key-decisions:
  - "A NUL byte in governed-paths.txt is converted to a non-printable byte before the shell reader sees it (tr), because bash read silently drops NUL while Kotlin keeps it; both now fail closed with a line number"
  - "The shell strips padding with extglob parameter expansion (shopt -s extglob) bounded to space, tab and carriage return; a bracket-set loop was the alternative"

patterns-established:
  - "Shared-grammar parity: any change to load_globs must change GovernedPaths.parse and both case tables together (cross-reference comments in both files)"

requirements-completed: [AIDEV-06, AIDEV-08]

coverage:
  - id: D1
    description: "A padded, tab-indented or CRLF governed entry governs its path in the CI gate exactly as the Gradle check expects its ask rule"
    requirement: AIDEV-06
    verification:
      - kind: unit
        ref: "scripts/governance/test-check-pr.sh#G1,G2"
        status: pass
      - kind: integration
        ref: "GovernedPathsFormatFunctionalTest#K1"
        status: pass
    human_judgment: false
  - id: D2
    description: "Comments, inner spaces, non-ASCII, BOM, NUL, inner CR, empty file and line ordering behave identically in both parsers"
    requirement: AIDEV-08
    verification:
      - kind: unit
        ref: "scripts/governance/test-check-pr.sh#G3-G9,G8b"
        status: pass
      - kind: integration
        ref: "GovernedPathsFormatFunctionalTest#K2-K9"
        status: pass
    human_judgment: false
  - id: D3
    description: "WR-04 recorded fixed in 01-REVIEW-DISPOSITION.md with WR-02 deferred and open 6"
    verification:
      - kind: other
        ref: "grep -c '^| WR-04 | warning | fixed |' 01-REVIEW-DISPOSITION.md"
        status: pass
    human_judgment: false
  - id: D4
    description: "Concurrency edge: checkClaudeAskRules declares only its two inputs and one report and keeps no other state"
    verification: []
    human_judgment: true
    rationale: "Backstop truth in the plan; not testable locally"

plan_head_before: 7b4a658
plan_head_after: 031ab23095c699734ad9a65c7ce4728666c8b585

duration: 12min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 16: One governed-paths grammar for the shell gate and the Gradle check Summary

**`check-pr.sh` `load_globs` and the new internal `GovernedPaths.parse` read `governance/governed-paths.txt` with one grammar (LF split, space/tab/CR strip, `#` comments, printable-ASCII entries, line-numbered fail-closed errors), tested with the same case table on both sides; WR-04 is fixed.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-11-21T14:13:00Z
- **Completed:** 2026-11-21T14:26:00Z
- **Tasks:** 2 (tracer plus TDD)
- **Files modified:** 6

## Accomplishments

- A governed entry padded with spaces or tabs, or ending in a carriage return, now governs its path in CI. Before, `check-pr.sh` matched the padded text literally while `checkClaudeAskRules` trimmed it, so the Gradle check passed and CI left the path ungoverned.
- Both parsers ignore `#` comment lines, keep inner spaces, and fail closed with `line <n>: entry contains a character outside printable ASCII` for non-breaking space, byte order mark, NUL, inner tab and inner carriage return. Every offending line is reported in ascending line order.
- A file with only comments and blank lines fails both with `lists no governed path`. Entries equal after stripping merge into one expected ask rule, and two matching rules still report `duplicated:`.
- Shell cases G1-G9 plus G8b (NUL) and Kotlin cases K1-K9 share one table. The shell suite grew from 45 to 55 cases, the new Kotlin class has nine tests.

## Task Commits

1. **Task 1: one grammar end to end (tracer)** - `cd3adbe` (feat)
2. **Task 2 RED: failing cases G3-G9 and K2-K9** - `308d66c` (test)
3. **Task 2 GREEN: comments, printable-ASCII rule, line numbers** - `4730949` (feat)
4. **Task 2: WR-04 recorded fixed** - `031ab23` (docs)

**Plan metadata:** committed after this file (docs: complete plan)

## Files Created/Modified

- `scripts/governance/check-pr.sh` - `load_globs` implements the grammar; `shopt -s extglob`; cross-reference comment
- `scripts/governance/test-check-pr.sh` - helpers `governed_file`, `touch_json`, `run_order_case`; ten new grammar cases
- `build-logic/.../governance/GovernedPaths.kt` - internal parser: entries and line problems
- `build-logic/.../governance/AskRulesCheck.kt` - reads through `GovernedPaths.parse`; parse problems take precedence, then the empty list, then rule problems
- `build-logic/.../governance/GovernedPathsFormatFunctionalTest.kt` - nine TestKit tests
- `.planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md` - WR-04 `fixed`, `open: 6`

## TDD Evidence (Task 2)

- **RED:** `test-check-pr.sh` reported 48 passed, 6 failed (G3, G5, G6, G7, G8, G9) and `GovernedPathsFormatFunctionalTest` reported 6 of 9 failed (K2, K4, K5, K6, K7, K8). Each target failed on the planned behaviour: the comment line was matched as a glob or required an ask rule, the non-ASCII, BOM and inner-CR files were accepted, the comment-only file was not reported as empty, and no line-numbered message existed. Semantic assessment: tests ran, compiled and failed on the planned assertion, not on setup. The `gsd check tdd-red-evidence` classifier was not run because it has no adapter for the shell harness or the Gradle test report format in use; the RED output above is the real command output.
- **GREEN:** shell 55 passed, 0 failed; functional tests green.
- **REFACTOR:** none needed.

## Decisions Made

- NUL bytes: bash `read` drops NUL silently and Kotlin would keep the character, so the two could disagree. `load_globs` reads through `tr '\0' '\001'`, so a NUL line fails closed with its line number. Covered by G8b and by K7.
- `[[ $line == *[![:print:]]* ]]` under `LC_ALL=C` is the shell printable-ASCII test, as the plan prescribed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical] NUL byte disagreement between the parsers**
- **Found during:** Task 2 (GREEN, shell)
- **Issue:** The plan's goal is that no input is kept by one parser and dropped by the other. A NUL inside an entry is dropped by bash `read` but rejected by Kotlin.
- **Fix:** Feed the shell loop through `tr '\0' '\001'` so the byte becomes a non-printable, line-numbered failure. Added shell case G8b and a second assertion inside K7 so the Kotlin test count stays at nine.
- **Files modified:** `scripts/governance/check-pr.sh`, `scripts/governance/test-check-pr.sh`, `GovernedPathsFormatFunctionalTest.kt`
- **Verification:** 55 shell cases and the functional tests pass, also under macOS bash 3.2.57.
- **Committed in:** `4730949`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Closes a disagreement the plan's own prohibition covers; no scope growth.

## Issues Encountered

None. The harness refused one compound command (python edit plus test run); edits were redone with the Edit tool.

## Verification

- `mise x -- bash scripts/governance/test-check-pr.sh`: 55 passed, 0 failed (also with `/bin/bash` 3.2.57)
- `mise x -- bash scripts/governance/test-list-pr-files.sh`: 25 passed
- `mise x -- ./gradlew :build-logic:functionalTest --tests '*AskRulesCheck*' --tests '*GovernedPathsFormat*'`: pass
- `mise x -- ./gradlew check`: BUILD SUCCESSFUL (detekt, Dokka, Spotless, all functional tests, checkClaudeAskRules on the real files)
- `shellcheck`, `editorconfig-checker`, `reuse lint`: clean
- Acceptance greps: `@Test` count 9, `disposition: fixed` 3, `open: 6` 1, WR-04 row 1, `lists no governed path` 1

## Known Stubs

None.

## Threat Flags

None. T-01-45 (parser divergence) is mitigated as planned; no new endpoints or trust boundaries.

## Next Phase Readiness

Plan 01-17 can extend `governance/governed-paths.txt` and document the governed-path boundary in `#` comments. `governance/governed-paths.txt` itself was not edited here. Keep `load_globs` and `GovernedPaths.parse` in step when changing either.

## Self-Check: PASSED

- Files exist: GovernedPaths.kt, GovernedPathsFormatFunctionalTest.kt, check-pr.sh, test-check-pr.sh, AskRulesCheck.kt, 01-REVIEW-DISPOSITION.md
- Commits `cd3adbe`, `308d66c`, `4730949`, `031ab23` are on the branch

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
