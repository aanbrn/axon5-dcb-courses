---
phase: 02
review: 02-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "Conflict retry has a fixed, unconfigurable bound and no jitter, so a busy course exhausts it"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`forbidden` in the licence policy is advisory, so an allowed entry can match a forbidden family"
  - id: WR-03
    severity: warning
    disposition: open
    title: "`KotlinTagResolver` cannot read a non-public `@EventTag` property, which Axon's resolver can"
  - id: WR-04
    severity: warning
    disposition: open
    title: "`ContainerImageArchitectureTest` strips comments in a way that can hide a violation"
  - id: WR-05
    severity: warning
    disposition: fixed
    title: "Second `DecisionCaptureIntegrationTest` case can pass without asserting anything"
  - id: WR-06
    severity: warning
    disposition: open
    title: "`ImageReferenceCheck` skips catalog image entries that are not a plain double-quoted string"
  - id: IN-01
    severity: info
    disposition: open
    title: "Magic number 12 is suppressed with a justification that argues against it"
  - id: IN-02
    severity: info
    disposition: open
    title: "Duplicated helpers in build logic and in tests"
  - id: IN-03
    severity: info
    disposition: open
    title: "Duplicated outline scanner in two governance checks, with a fence toggle that mixes fence types"
  - id: IN-04
    severity: info
    disposition: open
    title: "The upstream advisory can never fire for the record that most needs it"
  - id: IN-05
    severity: info
    disposition: open
    title: "Necessity (judgment) - the empty `contracts-queries` module and its rule entries have no instantiation yet"
open: 10
total: 11
recorded: 2026-11-23T06:52:19.989Z
---

# Phase 02: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| WR-04 | warning | open | - |
| WR-05 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-mub in ae12133 (committed-count assertion before the loop) |
| WR-06 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved.
