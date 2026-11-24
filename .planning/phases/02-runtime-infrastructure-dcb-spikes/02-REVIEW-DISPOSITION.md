---
phase: 02
review: 02-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "`check_git` aborts the whole doctor run, with no report, when `git --version` exits non-zero"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "The new KDoc on `CoursesJsonModule` states a wrong map-key behaviour and a remedy that would not round-trip"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "`ImageReferenceCheck` accepts catalog entries that the Renovate manager never matches, though its KDoc says the check exists to catch them"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "A shell whose operand comes from an expansion now asks with a misleading reason"
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "`bash -euo pipefail -c '...'` hides the script from the merge hook"
  - id: WR-04
    severity: warning
    disposition: fixed
    title: "The agent worktree exemption does not hold for the `**/` governed entries"
  - id: WR-05
    severity: warning
    disposition: fixed
    title: "`doctor` builds a copy-paste install command from the unvalidated pin"
  - id: WR-06
    severity: warning
    disposition: fixed
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
    disposition: fixed
    title: "`ImageReferenceCheck` skips an indented catalog entry"
  - id: IN-07
    severity: info
    disposition: fixed
    title: "`CoursesJsonModule` writes an ID as a map key but cannot read it back"
  - id: IN-08
    severity: info
    disposition: fixed
    title: "The hooks need git 2.36 or newer, and `doctor` does not check it"
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
open: 4
total: 19
recorded: 2026-11-24T10:49:07.687Z
---

# Phase 02: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-oyk in 5781180 (empty-string fallback on the git, mise and claude version reads; doctor reports `fail  git:` and finishes) |
| WR-02 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-oyk in 9392b29 (KDoc states the observed raw-UUID map key; JsonRoundTripTest pins it; key form left to an ADR when a contract first holds an ID-keyed map) |
| WR-03 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-oyk in b61e980 (check accepts exactly the form the Renovate image manager reads, rejects registry ports; parity test reads the regex from renovate.json5) |
| IN-01 | info | fixed | 02-REVIEW.md — fixed by quick task 261124-oyk in 58ccbb2 (reason names the shell expansion; still asks) |
| CR-01 | critical | fixed | 02-REVIEW.md — fixed by quick task 261124-kh6 in 9f8be86 (shell option clusters and value-taking long options parsed as bash does; unknown forms ask) (not in the current review) |
| WR-04 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-kh6 in 996829c (a `**/` match inside an agent worktree is exempt; the five `Edit(/**/...)` ask rules recorded as an ADR-0015 residual risk) (not in the current review) |
| WR-05 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-kh6 in dd73bcf (the pin must match a plain version before the fix command is built) (not in the current review) |
| WR-06 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-kh6 in dd73bcf (UP-0001 and UP-0004 record the 2026-11-24 contribution decision) (not in the current review) |
| WR-07 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in 01bf991 (the early exit tests the command with quotes, backslashes, `$` and line continuations removed, by a linear filter) (not in the current review) |
| WR-08 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in 55247cc (shells reading standard input, here-strings, `env -S`, `sudo`/`doas` and programs whose arguments run git are analysed or ask; the rest is listed in ADR-0015 residual risks) (not in the current review) |
| WR-09 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in bd589d0 (`<<` in arithmetic is not a heredoc; a heredoc without its delimiter line leaves the lexing incomplete) (not in the current review) |
| WR-10 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-c1g in 6a06a26 (every test runs on its own fixture image set) (not in the current review) |
| IN-06 | info | fixed | 02-REVIEW.md — fixed by quick task 261124-nlk in 474c1fc (false positive: catalogProblems trims each line before matching; two TestKit cases now guard indented entries, KDoc states it) (not in the current review) |
| IN-07 | info | fixed | 02-REVIEW.md — fixed by quick task 261124-nlk in 3f9da9c (KDoc states the ID types are registered as values only; the first ID-keyed map adds addKeyDeserializer) (not in the current review) |
| IN-08 | info | fixed | 02-REVIEW.md — fixed by quick task 261124-nlk in 04a892d and 3f9da9c (doctor fails on a missing, unparsable or pre-2.36 git; ADR-0015 and docs/tooling.md record the minimum) (not in the current review) |
| IN-02 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in f4fa65f (shared fixture barrier and submitDecision in the tests) and 71a11d1 (one .java-version parser in build-logic) (not in the current review) |
| IN-03 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 57fc03f (one shared outline scanner that closes a fence only on its own fence string) (not in the current review) |
| IN-04 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 123680f (doctor lists fixed-upstream records whose markers remain, with their removal criteria) (not in the current review) |
| IN-05 | info | skipped | deliberate scaffolding per D-02; architecture test requires exactly four contracts modules; first query slice is Phase 4 (not in the current review) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
