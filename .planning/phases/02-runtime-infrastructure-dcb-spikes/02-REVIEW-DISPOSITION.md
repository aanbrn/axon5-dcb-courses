---
phase: 02
review: 02-REVIEW.md
titles: json
findings:
  - id: IN-01
    severity: info
    disposition: open
    title: "Review finding IDs are cited from durable code and docs, and the IDs are already ambiguous"
  - id: IN-02
    severity: info
    disposition: open
    title: "A pre-release of the minimum version passes the engine gate"
  - id: IN-03
    severity: info
    disposition: open
    title: "The `Read the mise version` and tool-setup steps are now copied into a third job"
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "`doctor` reports `ok` for the containerd image store on an engine older than 29.9.0, the combination UP-0012 still documents as failing"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "The new KDoc on `CoursesJsonModule` states a wrong map-key behaviour and a remedy that would not round-trip"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "`ImageReferenceCheck` accepts catalog entries that the Renovate manager never matches, though its KDoc says the check exists to catch them"
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
  - id: IN-04
    severity: info
    disposition: fixed
    title: "The upstream advisory can never fire for the record that most needs it"
  - id: IN-05
    severity: info
    disposition: skipped
    title: "Necessity (judgment) - the empty `contracts-queries` module and its rule entries have no instantiation yet"
open: 3
total: 19
recorded: 2026-11-25T02:15:37.235Z
---

# Phase 02: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| WR-01 | warning | fixed | 02-REVIEW.md — fixed by quick task 261125-a09 in 1e9c0fc (server version kept from check_docker; warn below 29.9.0 and on an uncomparable version; tests on both sides of the boundary) (not in the current review) |
| WR-02 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-oyk in 9392b29 (KDoc states the observed raw-UUID map key; JsonRoundTripTest pins it; key form left to an ADR when a contract first holds an ID-keyed map) (not in the current review) |
| WR-03 | warning | fixed | 02-REVIEW.md — fixed by quick task 261124-oyk in b61e980 (check accepts exactly the form the Renovate image manager reads, rejects registry ports; parity test reads the regex from renovate.json5) (not in the current review) |
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
| IN-04 | info | fixed | 02-REVIEW.md — fixed by quick task 261123-rzt in 123680f (doctor lists fixed-upstream records whose markers remain, with their removal criteria) (not in the current review) |
| IN-05 | info | skipped | deliberate scaffolding per D-02; architecture test requires exactly four contracts modules; first query slice is Phase 4 (not in the current review) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.

## Review of 2026-11-25 (gap-closure changes, diff base 7790881)

The current `02-REVIEW.md` reuses the IDs WR-01 and IN-01 to IN-04 for new findings; the rows above describe the
earlier review's findings of the same IDs.

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 02-REVIEW.md (2026-11-25) — fixed by quick task 261125-lqw in ec2b3f4 (`LC_ALL=C` prefixes the 429 grep only; test case 13 proves the wrapped command keeps the caller's locale) |
| IN-01 | info | open | 02-REVIEW.md (2026-11-25) — UP-0011 title, ADR-0008, docs/tooling.md and the doctor marker still state the colima AAAA stall as fact; follow-up with the author's UP-0011 decision |
| IN-02 | info | fixed | 02-REVIEW.md (2026-11-25) — fixed by quick task 261125-lqw in 39822f5 (host-neutral announcement and annotation title `Dependency download rate limit`) |
| IN-03 | info | open | 02-REVIEW.md (2026-11-25) — stale "does not cover Kotlin 2.4.21" marker text in `.github/workflows/codeql.yml:32`; follow-up |
| IN-04 | info | open | 02-REVIEW.md (2026-11-25) — five identical mirror steps in ci.yml (composite action candidate) and a hand-copied preset `ignorePaths` list in renovate.json5; follow-up |
