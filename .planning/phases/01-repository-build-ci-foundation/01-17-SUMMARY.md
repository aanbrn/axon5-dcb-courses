---
phase: 01-repository-build-ci-foundation
plan: 17
subsystem: infra
tags: [governance, bash, gradle, claude-code, codeowners]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Shared governed-paths grammar with comments (01-16), PR gate (01-13, 01-15), AskRulesCheck"
provides:
  - "governance/governed-paths.txt covering the enforcement surface (29 entries) with format, boundary rule, exclusions and accepted residual as comments"
  - "29 matching Edit ask rules in .claude/settings.json plus Bash(*config-set*)"
  - "boundary_case helper and 25 boundary cases in test-check-pr.sh (20 governed, 5 ungoverned)"
  - "WR-01 recorded fixed"
affects: [governance, ci, phase-2-module-wiring]

actuals:
  tokens: 9000
  tasks: 2
  commits: 2
plan_head_before: 66be3fe00426903a71fdb340b422985e37babfeb
plan_head_after: e4f3c9ff55dc8b007a774659a888331249e59cab

tech-stack:
  added: []
  patterns:
    - "The governed list states its boundary rule and exclusions next to the entries, and boundary cases run against the real file"

key-files:
  created: []
  modified:
    - governance/governed-paths.txt
    - .claude/settings.json
    - scripts/governance/test-check-pr.sh
    - .planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md

key-decisions:
  - "The ask rules in .claude/settings.json follow the order of the entries in governed-paths.txt, so the two files read side by side"
  - "The version catalog, gradlew.bat, module conventions, functional tests and the settings-script tests stay ungoverned, with the reasons written into the file and tested"

patterns-established:
  - "A path is governed when a change to it alone can rewrite the policy, stop a check from running, or make one pass without evaluating"

requirements-completed: [AIDEV-06, AIDEV-08]

coverage:
  - id: D1
    description: "A pull request changing a governance check, its wiring, the settings writer, the root build configuration, the plugin build, the root and settings conventions or the launcher needs the governance-approved label"
    requirement: AIDEV-06
    verification:
      - kind: unit
        ref: "scripts/governance/test-check-pr.sh#boundary cases (20 governed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Claude Code asks before editing any governed path: one Edit rule per entry (29) plus config-set"
    requirement: AIDEV-08
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew checkClaudeAskRules"
        status: pass
      - kind: integration
        ref: "mise x -- ./gradlew check"
        status: pass
    human_judgment: false
  - id: D3
    description: "Deliberate exclusions (catalog, gradlew.bat, module conventions, functional tests, settings-script tests, convention sample) pass without the label"
    requirement: AIDEV-06
    verification:
      - kind: unit
        ref: "scripts/governance/test-check-pr.sh#boundary cases (5 ungoverned)"
        status: pass
    human_judgment: false
  - id: D4
    description: "WR-01 recorded fixed; CR-01, WR-03, WR-04 fixed, WR-02 deferred, open 5"
    verification:
      - kind: other
        ref: "grep -c '^| WR-01 | warning | fixed |' 01-REVIEW-DISPOSITION.md"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 17: Governed enforcement boundary Summary

**The governed-path list now covers the governance checks' code and wiring, the settings writer, the root and plugin builds and the Gradle launcher, with the boundary rule written into the file and 25 boundary cases against the real list.**

## Performance

- **Duration:** 15 min
- **Completed:** 2026-11-21T14:30:00Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- WR-01 closed: dropping `checkGsdConfigPolicy` from `check`, gutting a check class, editing `apply-github-settings.sh`, removing the governance plugin from the root build, or swapping the wrapper now fails `governance-success` without the label, and the agent asks first.
- `governance/governed-paths.txt` went from 10 to 29 entries in eight commented sections. It is the first file to use `#` comments, which proves plan 01-16's grammar on the real list in both the shell gate and `checkClaudeAskRules`.
- Exclusions are explained in the file and tested: the version catalog (D-12 automerge), `gradlew.bat` (D-10), module conventions, functional tests, `scripts/test-apply-github-settings.sh`, the convention sample.

## Task Commits

1. **Task 1: governance checks' own code and the settings writer governed** - `ee45ad6` (feat)
2. **Task 2: full enforcement boundary, rule in the file, WR-01 fixed** - `e4f3c9f` (feat)

**Plan metadata:** recorded in the final docs commit.

## Files Created/Modified

- `governance/governed-paths.txt` - 29 entries plus header (format, boundary rule, exclusions, residual) and section comments
- `.claude/settings.json` - 29 `Edit(/<entry>)` rules in list order plus `Bash(*config-set*)` last
- `scripts/governance/test-check-pr.sh` - `boundary_case` helper and 25 cases (59 then 80 passing in total)
- `01-REVIEW-DISPOSITION.md` - WR-01 fixed, open 5

## Decisions Made

- Ask rules follow the entry order of the list rather than appending, so the original ten rules were reordered (none removed or narrowed).
- No other deviation from the plan's boundary.

## Deviations from Plan

None - plan executed exactly as written. (The original ten ask rules changed position within the array as the plan's "order as in the file" instruction implies; the set is unchanged and `Bash(*config-set*)` stays last.)

## Issues Encountered

None. Verification: `test-check-pr.sh` 80 passed, 0 failed; `test-list-pr-files.sh` 25 passed; `mise x -- ./gradlew check` green; `editorconfig-checker`, `reuse lint`, `shellcheck` clean; acceptance greps all at their expected counts (29, 29, 37 comment lines, catalog 0, original ten 10).

## Known Stubs

None.

## Threat Flags

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Every later edit of the newly governed files (for example adding a module to `settings.gradle.kts` in Phase 2) needs the label and prompts the agent; Renovate Gradle wrapper pull requests are manual merges.
- No GitHub-side write was made; the phase pull request carries `governance-approved`.
- Open review findings: IN-01..IN-05; WR-02 deferred by the author.

## Self-Check: PASSED

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
