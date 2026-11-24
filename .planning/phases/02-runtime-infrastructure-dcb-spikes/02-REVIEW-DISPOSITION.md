---
phase: 02
review: 02-REVIEW.md
titles: json
findings:
  - id: WR-07
    severity: warning
    disposition: open
    title: "The merge hook's early exit lets a quote-split `merge` skip every check"
  - id: WR-08
    severity: warning
    disposition: open
    title: "A merge fed to a shell through a pipe or a here-string is not seen"
  - id: WR-09
    severity: warning
    disposition: open
    title: "A `<<` that is not a heredoc swallows the rest of the command without a trace"
  - id: WR-10
    severity: warning
    disposition: open
    title: "`ComposeVersionsFunctionalTest` pins the repository catalog's current image set"
  - id: IN-06
    severity: info
    disposition: open
    title: "`ImageReferenceCheck` skips an indented catalog entry"
  - id: IN-07
    severity: info
    disposition: open
    title: "`CoursesJsonModule` writes an ID as a map key but cannot read it back"
  - id: IN-08
    severity: info
    disposition: open
    title: "The hooks need git 2.36 or newer, and `doctor` does not check it"
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
open: 7
total: 18
recorded: 2026-11-24T01:30:29.860Z
---

# Phase 02: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-07 | warning | open | - |
| WR-08 | warning | open | - |
| WR-09 | warning | open | - |
| WR-10 | warning | open | - |
| IN-06 | info | open | - |
| IN-07 | info | open | - |
| IN-08 | info | open | - |
| WR-01 | warning | deferred | Phase 3 success criterion 2 (last-seat race, zero infrastructure errors): make the bound configurable, add capped full-jitter back-off and a 40-way concurrent subscription test in the first subscription slice (not in the current review) |
| WR-02 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in cc73961 (an allowed identifier that matches a forbidden family makes the policy invalid) (not in the current review) |
| WR-03 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 40c4b79 (non-public annotated properties are made accessible before they are read) (not in the current review) |
| WR-04 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 1b2394f (a string-aware scanner masks comments and literals before the image rule runs) (not in the current review) |
| WR-05 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-mub in ae12133 (committed-count assertion before the loop) (not in the current review) |
| WR-06 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in c2222c3 (every image-* catalog key is matched, a non-double-quoted value is reported) (not in the current review) |
| IN-01 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in f4fa65f (the pool is sized by STUDENTS and the suppression is gone) (not in the current review) |
| IN-02 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in f4fa65f (shared fixture barrier and submitDecision in the tests) and 71a11d1 (one .java-version parser in build-logic) (not in the current review) |
| IN-03 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 57fc03f (one shared outline scanner that closes a fence only on its own fence string) (not in the current review) |
| IN-04 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 123680f (doctor lists fixed-upstream records whose markers remain, with their removal criteria) (not in the current review) |
| IN-05 | info | skipped | deliberate scaffolding per D-02; architecture test requires exactly four contracts modules; first query slice is Phase 4 (not in the current review) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
