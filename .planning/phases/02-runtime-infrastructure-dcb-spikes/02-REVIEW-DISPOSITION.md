---
phase: 02
review: 02-REVIEW.md
titles: json
findings:
  - id: CR-01
    severity: critical
    disposition: open
    title: "`bash -euo pipefail -c '...'` hides the script from the merge hook"
  - id: WR-01
    severity: warning
    disposition: open
    title: "Option forms of `git`, `sudo`, `env` and a `GIT_DIR` prefix make the hook read the wrong repository or none"
  - id: WR-02
    severity: warning
    disposition: open
    title: "Brace expansion assembles a merge command without a trace"
  - id: WR-03
    severity: warning
    disposition: open
    title: "Refs that an earlier part of the same command moves are read before the command runs"
  - id: WR-04
    severity: warning
    disposition: open
    title: "The agent worktree exemption does not hold for the `**/` governed entries"
  - id: WR-05
    severity: warning
    disposition: open
    title: "`doctor` builds a copy-paste install command from the unvalidated pin"
  - id: WR-06
    severity: warning
    disposition: open
    title: "UP-0001 and UP-0004 still say the contribution is undecided"
  - id: WR-07
    severity: warning
    disposition: fixed
    title: "The merge hook's early exit lets a quote-split `merge` skip every check"
  - id: WR-08
    severity: warning
    disposition: fixed
    title: "A merge fed to a shell through a pipe or a here-string is not seen"
  - id: WR-09
    severity: warning
    disposition: fixed
    title: "A `<<` that is not a heredoc swallows the rest of the command without a trace"
  - id: WR-10
    severity: warning
    disposition: fixed
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
open: 10
total: 19
recorded: 2026-11-24T07:24:48.477Z
---

# Phase 02: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| CR-01 | critical | open | - |
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| WR-04 | warning | open | - |
| WR-05 | warning | open | - |
| WR-06 | warning | open | - |
| WR-07 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in 01bf991 (the early exit tests the command with quotes, backslashes, `$` and line continuations removed, by a linear filter) (not in the current review) |
| WR-08 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in 55247cc (shells reading standard input, here-strings, `env -S`, `sudo`/`doas` and programs whose arguments run git are analysed or ask; the rest is listed in ADR-0015 residual risks) (not in the current review) |
| WR-09 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in bd589d0 (`<<` in arithmetic is not a heredoc; a heredoc without its delimiter line leaves the lexing incomplete) (not in the current review) |
| WR-10 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in 6a06a26 (every test runs on its own fixture image set) (not in the current review) |
| IN-06 | info | open | - (not in the current review) |
| IN-07 | info | open | - (not in the current review) |
| IN-08 | info | open | - (not in the current review) |
| IN-01 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in f4fa65f (the pool is sized by STUDENTS and the suppression is gone) (not in the current review) |
| IN-02 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in f4fa65f (shared fixture barrier and submitDecision in the tests) and 71a11d1 (one .java-version parser in build-logic) (not in the current review) |
| IN-03 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 57fc03f (one shared outline scanner that closes a fence only on its own fence string) (not in the current review) |
| IN-04 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 123680f (doctor lists fixed-upstream records whose markers remain, with their removal criteria) (not in the current review) |
| IN-05 | info | skipped | deliberate scaffolding per D-02; architecture test requires exactly four contracts modules; first query slice is Phase 4 (not in the current review) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
