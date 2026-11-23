---
phase: 02
review: 02-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "Conflict retry has a fixed, unconfigurable bound and no jitter, so a busy course exhausts it"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "`forbidden` in the licence policy is advisory, so an allowed entry can match a forbidden family"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "`KotlinTagResolver` cannot read a non-public `@EventTag` property, which Axon's resolver can"
  - id: WR-04
    severity: warning
    disposition: fixed
    title: "`ContainerImageArchitectureTest` strips comments in a way that can hide a violation"
  - id: WR-05
    severity: warning
    disposition: fixed
    title: "Second `DecisionCaptureIntegrationTest` case can pass without asserting anything"
  - id: WR-06
    severity: warning
    disposition: fixed
    title: "`ImageReferenceCheck` skips catalog image entries that are not a plain double-quoted string"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "Magic number 12 is suppressed with a justification that argues against it"
  - id: IN-02
    severity: info
    disposition: fixed
    title: "Duplicated helpers in build logic and in tests"
  - id: IN-03
    severity: info
    disposition: fixed
    title: "Duplicated outline scanner in two governance checks, with a fence toggle that mixes fence types"
  - id: IN-04
    severity: info
    disposition: fixed
    title: "The upstream advisory can never fire for the record that most needs it"
  - id: IN-05
    severity: info
    disposition: skipped
    title: "Necessity (judgment) - the empty `contracts-queries` module and its rule entries have no instantiation yet"
open: 0
total: 11
recorded: 2026-11-23T06:52:19.989Z
---

# Phase 02: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | Phase 3 success criterion 2 (last-seat race, zero infrastructure errors): make the bound configurable, add capped full-jitter back-off and a 40-way concurrent subscription test in the first subscription slice |
| WR-02 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in cc73961 (an allowed identifier that matches a forbidden family makes the policy invalid) |
| WR-03 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 40c4b79 (non-public annotated properties are made accessible before they are read) |
| WR-04 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 1b2394f (a string-aware scanner masks comments and literals before the image rule runs) |
| WR-05 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-mub in ae12133 (committed-count assertion before the loop) |
| WR-06 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in c2222c3 (every image-* catalog key is matched, a non-double-quoted value is reported) |
| IN-01 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in f4fa65f (the pool is sized by STUDENTS and the suppression is gone) |
| IN-02 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in f4fa65f (shared fixture barrier and submitDecision in the tests) and 71a11d1 (one .java-version parser in build-logic) |
| IN-03 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 57fc03f (one shared outline scanner that closes a fence only on its own fence string) |
| IN-04 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 123680f (doctor lists fixed-upstream records whose markers remain, with their removal criteria) |
| IN-05 | info | skipped | deliberate scaffolding per D-02; architecture test requires exactly four contracts modules; first query slice is Phase 4 |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved.
