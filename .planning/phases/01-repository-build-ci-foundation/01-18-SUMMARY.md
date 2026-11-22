---
phase: 01-repository-build-ci-foundation
plan: 18
subsystem: infra
tags: [governance, bash, jq, fail-closed, review-gap]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "PR governance gate (01-13), fail-closed file list reading and the files API limit rule (01-15)"
provides:
  - "check-pr.sh reads per-file records through a mktemp file, checks jq's exit status before any entry is evaluated and asserts it evaluated exactly `jq length` entries"
  - "Cases R1 to R11 in test-check-pr.sh, including a fake jq that proves each guard on its own"
  - "WR-05 recorded fixed (open count 8)"
affects: [governance, ci]

actuals:
  tokens: 11000
  tasks: 2
  commits: 4
plan_head_before: 8b986ff6cd3b03524f9bce20c70a5d3b4a7bd7c9
plan_head_after: 40f3bb45e40a5791e53e0d43536f623b11681805

tech-stack:
  added: []
  patterns:
    - "A producer whose output feeds a loop is run as a plain command into a script-level temp file so its status is observable; a second count-based guard covers a producer that exits 0 but writes short"
    - "A fake tool wrapper first on PATH, selected by a mode variable, isolates one guard per test case"

key-files:
  created: []
  modified:
    - scripts/governance/check-pr.sh
    - scripts/governance/test-check-pr.sh
    - .planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md

key-decisions:
  - "NUL strings are not rejected up front in validate_inputs: the reproduction must reach the status and count guards, so each guard is exercised by a real failure"
  - "RECORDS_FILE is a script-level global with an EXIT trap registered first in main, the same pattern as list-pr-files.sh, so the trap sees it after check_governed_files returned"
  - "The completeness check counts one loop iteration per five-field record against `jq length`, both counting JSON array entries; it returns before the label or the renovate exemption is consulted"

patterns-established:
  - "Unreadable list fails closed with or without the label (carries the 01-15 prohibition forward without the WR-05 exception)"

requirements-completed: [AIDEV-06]

coverage:
  - id: D1
    description: "The reproduced WR-05 list (harmless first entry, governed second entry with a NUL in its patch, no label) fails the gate instead of printing 'no governed file is touched'"
    requirement: AIDEV-06
    verification:
      - kind: integration
        ref: "scripts/governance/test-check-pr.sh#R1"
        status: pass
    human_judgment: false
  - id: D2
    description: "NUL in a filename, a previous_filename or the patch of an ungoverned only entry fails the gate; the label and the renovate exemption never turn an unreadable list into a pass"
    requirement: AIDEV-06
    verification:
      - kind: integration
        ref: "scripts/governance/test-check-pr.sh#R2-R6"
        status: pass
    human_judgment: false
  - id: D3
    description: "The status guard and the completeness guard each fail on their own: a fake jq that exits 3 after writing every record (R7), exits 0 one record short (R8) or writes a torn record (R9)"
    requirement: AIDEV-06
    verification:
      - kind: integration
        ref: "scripts/governance/test-check-pr.sh#R7-R9"
        status: pass
    human_judgment: false
  - id: D4
    description: "No records file is left in TMPDIR after a failing or a passing run, and it existed during the run"
    requirement: AIDEV-06
    verification:
      - kind: integration
        ref: "scripts/governance/test-check-pr.sh#R10-R11"
        status: pass
    human_judgment: false
  - id: D5
    description: "Existing behaviour is unchanged: 80 earlier cases (empty list, 2999-entry list, label rule, files API limit, branch rule, normaliser) and 25 listing cases still pass"
    requirement: AIDEV-06
    verification:
      - kind: integration
        ref: "mise x -- bash scripts/governance/test-check-pr.sh (91 passed, 0 failed); test-list-pr-files.sh (25 passed, 0 failed)"
        status: pass
    human_judgment: false
  - id: D6
    description: "WR-05 recorded as fixed in 01-REVIEW-DISPOSITION.md, WR-02 kept deferred, open count 8"
    verification: []
    human_judgment: true
    rationale: "A disposition record is a bookkeeping artifact; the author confirms it when closing the gap round"

duration: 24min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 18: Fail-closed reading of the changed files list (WR-05) Summary

**`check-pr.sh` now writes jq's per-file records to a mktemp file, reads jq's exit status before evaluating anything and asserts it evaluated exactly `jq length` entries, so a mid-stream jq failure fails the gate instead of dropping the remaining files.**

## Performance

- **Duration:** 24 min
- **Tasks:** 2 (tracer + tdd), 4 production commits
- **Files modified:** 3

## Accomplishments

- The WR-05 reproduction (harmless first entry, then `.github/workflows/ci.yml` with a NUL in its patch, no label) now exits 1 with `the changed files list could not be read: jq exited with status <n>`; before the fix it printed `no governed file is touched` and exited 0.
- A second guard, independent of jq's status, compares the number of five-field records the loop read with `jq length` and fails with `<seen> of <count> entries were evaluated`. It is proven alone by a fake jq that exits 0 one record short (R8) or writes a torn record (R9); the status guard is proven alone by a fake jq that writes every record and then exits 3 (R7).
- Neither the `governance-approved` label (R5) nor the renovate[bot] exemption (R6) turns an unreadable list into a pass. An empty list and the 2999-entry list still pass.
- The records file is created with `mktemp` under `TMPDIR`, removed by an EXIT trap on the failing and the passing path (R10, R11); the cases check that the file existed during the run, so they cannot pass vacuously.

## Task Commits

1. **Task 1 (tracer): fail-closed reading, one path** - `489dda1` (feat). R1 added first, run against the unchanged script, then the script change.
2. **Task 2 (tdd) RED** - `605501f` (test): cases R2 to R11 and the helpers `fake_jq` and `run_tmpdir_case`.
3. **Task 2 (tdd) GREEN** - `697e1ba` (feat): the completeness check.
4. **Disposition** - `40f3bb4` (docs): WR-05 recorded `fixed`, `open: 8`.

**Plan metadata:** the commit that adds this SUMMARY together with STATE.md and ROADMAP.md.

## TDD Evidence

- **RED, Task 1:** R1 ran against the unchanged script and failed on its planned assertion: exit 0, expected 1, output `jq: error ... Cannot dump a string containing NUL with --raw-output0 option` followed by `governance: no governed file is touched` (80 passed, 1 failed). Semantic assessment: the target case executed, jq aborted part-way, the gate passed the list; this is exactly the fail-open reproduced in 01-VERIFICATION.md, for the planned reason.
- **RED, Task 2:** R8 and R9 failed against the status-only reading, both with exit 0 and `no governed file is touched` (89 passed, 2 failed); R2 to R7, R10 and R11 passed, as the plan predicted, because Task 1 already delivered what they describe. Semantic assessment: both failing cases executed through the fake jq and failed on the planned assertion (missing completeness guard).
- **GREEN:** 91 passed, 0 failed.
- The plan has `type: execute` and the suite is a bash harness that emits no TAP, JUnit, `go test` or similar report, so `check tdd-red-evidence` has no adapter for it; the RED outputs above were inspected by hand and no classifier verdict is claimed.

## Verification

- `mise x -- bash scripts/governance/test-check-pr.sh`: 91 passed, 0 failed
- `mise x -- bash scripts/governance/test-list-pr-files.sh`: 25 passed, 0 failed
- `mise x -- shellcheck` on both scripts: clean
- `mise x -- editorconfig-checker`: clean; `mise x -- reuse lint`: compliant
- `mise x -- ./gradlew check`: BUILD SUCCESSFUL
- Acceptance greps: no `< <(jq` left in check-pr.sh; `could not be read` x2; `trap cleanup EXIT` x1; 11 R cases; `disposition: fixed` x5; `open: 8`; WR-05 row `fixed`; `disposition: deferred` x1 (WR-02 kept)
- The pre-commit hook (format, gitleaks, reuse, shellcheck) passed on every commit.

## Decisions Made

See `key-decisions` in the frontmatter. No decision departs from the plan.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The harness refused two compound shell commands that mixed `git` with other commands; they were re-run as separate plain commands.

## Known Stubs

None.

## Threat Flags

None. The change tightens an existing trust boundary (T-01-50) and adds no network endpoint, auth path or schema.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- WR-05 is closed. WR-07 (plan 01-19) and the WR-06 ADR (plan 01-20) remain; WR-02 stays deferred.
- T-01-51 (a NUL in a pull request file name or patch can never pass the gate) is accepted as designed: failing closed is intended.
- Accepted backstop: a run killed before its EXIT trap fires can leave a `check-pr-records.*` file in the runner's temporary directory; no other run reads it, because each run creates a fresh mktemp name.
- No GitHub-side write was performed.

## Self-Check: PASSED

- `scripts/governance/check-pr.sh`, `scripts/governance/test-check-pr.sh` and `01-REVIEW-DISPOSITION.md` exist and are modified as described.
- Commits `489dda1`, `605501f`, `697e1ba`, `40f3bb4` are ancestors of HEAD.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
