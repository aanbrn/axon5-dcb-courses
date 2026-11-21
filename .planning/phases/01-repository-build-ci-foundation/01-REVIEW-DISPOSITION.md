---
phase: 01
review: 01-REVIEW.md
titles: json
findings:
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "Governance file-list step fails open when `gh api` produces no output"
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "Governed paths do not cover the code that enforces governance"
  - id: WR-02
    severity: warning
    disposition: deferred
    title: "The approval label is sticky and is not bound to a commit"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "A PR with 3000 or more files hides governed files from the gate"
  - id: WR-04
    severity: warning
    disposition: fixed
    title: "The governed-paths file is parsed differently by the shell gate and the Gradle check"
  - id: IN-01
    severity: info
    disposition: open
    title: "Branch-name rule rejects the GSD milestone branches that the pinned policy declares"
  - id: IN-02
    severity: info
    disposition: open
    title: "`ToolingTableCheck` cannot see `[tools.<name>]` sub-tables or multi-line inline tables"
  - id: IN-03
    severity: info
    disposition: open
    title: "Duplicated logic"
  - id: IN-04
    severity: info
    disposition: open
    title: "Redundant or not-yet-instantiated attribute entries"
  - id: IN-05
    severity: info
    disposition: open
    title: "The governance gate and shell tests depend on unpinned runner tools"
  - id: WR-05
    severity: warning
    disposition: fixed
    title: "`check-pr.sh` ignores the exit status of the `jq` process substitution, so a mid-stream `jq` failure drops files and the gate passes"
  - id: WR-06
    severity: warning
    disposition: deferred
    title: "The gate runs from the pull request's own merge commit, so a PR can edit the gate that is meant to hold it"
  - id: WR-07
    severity: warning
    disposition: fixed
    title: "The governed boundary omits files that carry the same authority as governed ones"
  - id: IN-06
    severity: info
    disposition: open
    title: "Invisible characters are written literally in the Kotlin test source"
  - id: WR-08
    severity: warning
    disposition: fixed
    title: "ADR-0003 overstates the ask-rule control: shell writes are not matched"
  - id: IN-07
    severity: info
    disposition: open
    title: "A future root package.json is governed as a whole file because of the deprecated renovate key"
open: 7
total: 16
recorded: 2026-11-21T18:40:00.000Z
---

# Phase 01: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| CR-01 | critical | fixed | 01-REVIEW.md — fixed by plan 01-15 |
| WR-01 | warning | fixed | 01-REVIEW.md — fixed by plan 01-17 |
| WR-02 | warning | deferred | 01-REVIEW.md — deferred by the author at gap planning (2026-11-21): binding the approval label to a commit or dismissing it on push needs a separate design decision (workflow permissions or a new verification mechanism); not part of the gap-closure scope |
| WR-03 | warning | fixed | 01-REVIEW.md — fixed by plan 01-15 |
| WR-04 | warning | fixed | 01-REVIEW.md — fixed by plan 01-16 |
| IN-01 | info | open | 01-REVIEW.md |
| IN-02 | info | open | 01-REVIEW.md |
| IN-03 | info | open | 01-REVIEW.md |
| IN-04 | info | open | 01-REVIEW.md |
| IN-05 | info | open | 01-REVIEW.md |
| WR-05 | warning | fixed | 01-REVIEW.md — re-review after gap closure; fixed by plan 01-18 |
| WR-06 | warning | deferred | 01-REVIEW.md — re-review after gap closure; deferred by the author (2026-11-21): accepted residual for a one-maintainer repository, recorded in docs/adr/0003-governance-gate-trust-boundary.md |
| WR-07 | warning | fixed | 01-REVIEW.md — re-review after gap closure; fixed by plan 01-19 |
| IN-06 | info | open | 01-REVIEW.md — re-review after gap closure |
| WR-08 | warning | fixed | 01-REVIEW.md — re-review after gap round 2; fixed by the orchestrator in 5980b3d (ADR-0003 wording) |
| IN-07 | info | open | 01-REVIEW.md — re-review after gap round 2 |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
