---
phase: 01-repository-build-ci-foundation
plan: 20
subsystem: governance
tags: [adr, governance, github-actions, pull_request, trust-boundary, review-finding]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "01-19 closed WR-07; 01-18 closed WR-05; ADR-0001 required-checks decision; governance.yml gate"
provides:
  - "ADR-0003 recording the WR-06 residual (gate runs from the pull request's own tree), the reasons it is accepted, three rejected alternatives and the revisit triggers"
  - "ADR-0001 Consequences cross-reference to ADR-0003"
  - "WR-06 recorded as deferred in 01-REVIEW-DISPOSITION.md with the ADR path as the reason"
affects: [phase-02-adr-templates, governance, ruleset-application]

actuals:
  tokens: 1700
  tasks: 2
  commits: 2
plan_head_before: 56077fc74ba6e754931b89d9e88f71cb79f3dffb
plan_head_after: 271fac211f0ca675ad343003cb6493fd02c9ea7b

tech-stack:
  added: []
  patterns:
    - "Accepted residuals are written down as minimal ADRs and linked from the ADR of the affected decision"

key-files:
  created:
    - docs/adr/0003-governance-gate-trust-boundary.md
  modified:
    - docs/adr/0001-workflow-permissions-and-required-checks.md
    - .planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md

key-decisions:
  - "WR-06 is accepted, not fixed: governance.yml keeps the pull_request trigger and the merge-ref checkout for a one-maintainer repository (author decision, ADR-0003)"
  - "The gate is a guard against accidental and agent-made governed changes, not a security boundary against a pull request crafted to rewrite it"
  - "Decision is revisited when a second person gets merge rights or external contributors become a regular contribution path"

patterns-established:
  - "ADR-0003 follows the minimal ADR format of ADR-0001 and ADR-0002 until Phase 2 normalises it"

requirements-completed: [AIDEV-06]

coverage:
  - id: D1
    description: "ADR-0003 states the WR-06 residual, the reasons for accepting it, three rejected alternatives with reasons and the revisit triggers, in the project's ADR format"
    requirement: "AIDEV-06"
    verification:
      - kind: other
        ref: "mise x -- ./gradlew spotlessCheck; mise x -- editorconfig-checker; mise x -- reuse lint; plan Task 1 grep acceptance criteria"
        status: pass
    human_judgment: true
    rationale: "Whether the stated trust boundary and the accepted-residual reasoning are right is the author's judgment; tooling checks only format and required content"
  - id: D2
    description: "ADR-0001 links ADR-0003 from its Consequences section with its Context, Decision and earlier Consequences unchanged"
    requirement: "AIDEV-06"
    verification:
      - kind: other
        ref: "grep -c -F '0003-governance-gate-trust-boundary.md' docs/adr/0001-workflow-permissions-and-required-checks.md prints 1; grep -c '^## ' prints 3"
        status: pass
    human_judgment: false
  - id: D3
    description: "01-REVIEW-DISPOSITION.md records WR-06 as deferred with the ADR path; WR-05 and WR-07 stay fixed, WR-02 stays deferred, open count is 6"
    requirement: "AIDEV-06"
    verification:
      - kind: other
        ref: "grep counts on 01-REVIEW-DISPOSITION.md: deferred 2, open 6, fixed 6, '^open: 6$' 1, WR-06 row deferred 1, ADR path 1"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 20: Governance gate trust boundary Summary

**ADR-0003 records review finding WR-06 as an accepted residual for a one-maintainer repository: the governance gate runs from the pull request's own merge ref, three alternatives are rejected with reasons, and the revisit triggers are named.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-11-21T15:36:53Z
- **Completed:** 2026-11-21T15:38:20Z
- **Tasks:** 2
- **Files modified:** 3 (1 created, 2 modified), plus this SUMMARY

## Accomplishments

- `docs/adr/0003-governance-gate-trust-boundary.md` written in the format of ADR-0001 and ADR-0002. It states what a pull request can do to the gate in the same change that it is meant to be held by, and that the Phase 1 research assumed the gate would not run pull request code. It names the boundary: the gate guards against accidental and agent-made governed changes, not against a pull request crafted to rewrite it.
- Reasons for acceptance recorded: the single maintainer merges every pull request through a squash-only ruleset, ask rules guard every governed path, and the residual was already accepted as T-01-20 at ASVS L1.
- Three alternatives rejected with a reason each: `pull_request_target` (zizmor `dangerous-triggers`, privileged context), a sparse base-ref checkout of the scripts (the workflow file stays in the pull request's hands), and CODEOWNERS with required review (meaningless with one maintainer).
- ADR-0001 gains one Consequences bullet linking ADR-0003. WR-06 is `deferred` in the disposition record with the ADR path in its Source cell; `open` is 6 (IN-01 to IN-06).

## Task Commits

1. **Task 1: ADR-0003 on record and passing every docs check** - `8c3e199` (docs)
2. **Task 2: ADR-0001 cross-reference and WR-06 disposition** - `271fac2` (docs)

**Plan metadata:** committed with this SUMMARY (docs: complete plan)

## Files Created/Modified

- `docs/adr/0003-governance-gate-trust-boundary.md` - the accepted-residual record
- `docs/adr/0001-workflow-permissions-and-required-checks.md` - one added Consequences bullet linking ADR-0003
- `.planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md` - WR-06 `deferred`, `open: 6`

## Decisions Made

- WR-06 is documented, not fixed, per the author's decision for this run. No workflow, ruleset or CODEOWNERS change was made, and `governance.yml` still has the `pull_request` trigger (verified by grep).
- The ADR quotes no SPDX tag text, so no REUSE-IgnoreStart/End fences were needed.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. The harness refused one compound command that held both a `git`-looking token and several greps; it was split into plain commands and rerun.

## User Setup Required

None - no external service configuration required.

## Threat Flags

None. T-01-56 and T-01-57 (both accept) are covered by ADR-0003 itself; no new network, auth or file-access surface was added.

## Next Phase Readiness

- All three re-review warnings (WR-05, WR-06, WR-07) are now closed or recorded; WR-02 stays deferred and IN-01 to IN-06 stay open.
- Phase 2 normalises the ADR note format; ADR-0003 carries the same minimal-record sentence as ADR-0001 and ADR-0002.
- Ruleset application and the human verification items remain with the author.

## Self-Check: PASSED

- FOUND: `docs/adr/0003-governance-gate-trust-boundary.md`
- FOUND: commits `8c3e199` and `271fac2` on `gsd/phase-01-execution`
- `mise x -- ./gradlew check`, `editorconfig-checker` and `reuse lint` all green after Task 2
- All Task 1 and Task 2 acceptance criteria passed (grep counts match the plan's expectations)

---

_Phase: 01-repository-build-ci-foundation_
_Completed: 2026-11-21_
