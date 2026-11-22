---
phase: 01
review: 01-REVIEW.md
titles: json
findings:
  - id: WR-02
    severity: warning
    disposition: deferred
    title: "The approval label is sticky and is not bound to a commit"
  - id: WR-08
    severity: warning
    disposition: open
    title: "ADR-0003 relies on Claude Code ask rules as a control over the agent, which the project itself says is not a security boundary"
  - id: WR-09
    severity: warning
    disposition: open
    title: "ADR-0004 repeats the ask-rule overclaim of WR-08 for the one file it exists to protect"
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
  - id: IN-06
    severity: info
    disposition: open
    title: "Invisible characters are written literally in the Kotlin test source"
  - id: IN-07
    severity: info
    disposition: open
    title: "`package.json` is governed as a whole file for the sake of a deprecated `renovate` key"
  - id: IN-08
    severity: info
    disposition: open
    title: "The `ExecutableOnPath` KDoc names the wrong working directory"
  - id: IN-09
    severity: info
    disposition: open
    title: "Test (b) does not force a warm daemon, unlike test (a)"
  - id: IN-10
    severity: info
    disposition: open
    title: "The two \"run `--version` and fail with a `mise install` hint\" tasks duplicate their mechanism"
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "Governance file-list step fails open when `gh api` produces no output"
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "Governed paths do not cover the code that enforces governance"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "A PR with 3000 or more files hides governed files from the gate"
  - id: WR-04
    severity: warning
    disposition: fixed
    title: "The governed-paths file is parsed differently by the shell gate and the Gradle check"
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
open: 12
total: 20
recorded: 2026-11-22T01:15:30.620Z
---

# Phase 01: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-02 | warning | deferred | 01-REVIEW.md — deferred by the author at gap planning (2026-11-21): binding the approval label to a commit or dismissing it on push needs a separate design decision (workflow permissions or a new verification mechanism); not part of the gap-closure scope |
| WR-08 | warning | open | - |
| WR-09 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |
| IN-05 | info | open | - |
| IN-06 | info | open | - |
| IN-07 | info | open | - |
| IN-08 | info | open | - |
| IN-09 | info | open | - |
| IN-10 | info | open | - |
| CR-01 | critical | fixed | 01-REVIEW.md — fixed by plan 01-15 (not in the current review) |
| WR-01 | warning | fixed | 01-REVIEW.md — fixed by plan 01-17 (not in the current review) |
| WR-03 | warning | fixed | 01-REVIEW.md — fixed by plan 01-15 (not in the current review) |
| WR-04 | warning | fixed | 01-REVIEW.md — fixed by plan 01-16 (not in the current review) |
| WR-05 | warning | fixed | 01-REVIEW.md — re-review after gap closure; fixed by plan 01-18 (not in the current review) |
| WR-06 | warning | deferred | 01-REVIEW.md — re-review after gap closure; deferred by the author (2026-11-21): accepted residual for a one-maintainer repository, recorded in docs/adr/0003-governance-gate-trust-boundary.md (not in the current review) |
| WR-07 | warning | fixed | 01-REVIEW.md — re-review after gap closure; fixed by plan 01-19 (not in the current review) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
