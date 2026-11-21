---
phase: 01-repository-build-ci-foundation
plan: 14
subsystem: infra
tags: [github-settings, gh-cli, settings-drift, governance-label, renovate, approval-gate]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-13: apply-github-settings.sh (--dry-run, --check, --apply, --only), declared ruleset and repository settings, settings-drift.yml scoped by SETTINGS_DRIFT_SECTIONS; plan 01-09: governance-success context and governed-paths"
provides:
  - "Live repository and security settings matching .github/repository-settings.json, applied by the author and verified read-only with --check"
  - "Label governance-approved (live), secret GRADLE_ENCRYPTION_KEY (live), secret SETTINGS_READ_TOKEN (live, name seen only)"
  - "Repository variable SETTINGS_DRIFT_SECTIONS = 'repository security', the single scope record read by this plan's check and by settings-drift.yml"
affects: [phase PR (/gsd-ship), Phase 13 repository metadata]

plan_head_before: 357389eecaa324f19a8e21df6e55beb9bbff55af
plan_head_after: 357389eecaa324f19a8e21df6e55beb9bbff55af

actuals:
  tokens: 3000
  tasks: 3
  commits: 0

tech-stack:
  added: []
  patterns:
    - "Partial approval is recorded in one repository variable (SETTINGS_DRIFT_SECTIONS) that every gate reads, so a deferred section turns nothing red"

key-files:
  created: []
  modified: []

key-decisions:
  - "Plan 01-14: author approved apply-without-ruleset (items 2, 3, 4 with a trailing period in the label description, 5); item 1 (ruleset main) is deferred until ci-success and governance-success are green on the phase PR (Pitfall 11's conservative order)"
  - "Plan 01-14: SETTINGS_DRIFT_SECTIONS is 'repository security' while the ruleset is deferred; the author deletes it after applying the ruleset"

requirements-completed: [GH-07, AIDEV-06]

coverage:
  - id: D1
    description: "Merge settings (squash only, PR_TITLE and PR_BODY, delete head branches, auto-merge, update branch) and security features (Dependabot alerts, private vulnerability reporting, secret scanning, push protection) live and matching the declared files"
    requirement: "GH-07"
    verification:
      - kind: other
        ref: "scripts/apply-github-settings.sh --check --only repository --only security (drift scope read from SETTINGS_DRIFT_SECTIONS): no drift, rc 0"
        status: pass
    human_judgment: false
  - id: D2
    description: "governance-approved label exists on the repository before the phase PR is opened"
    requirement: "AIDEV-06"
    verification:
      - kind: other
        ref: "gh label list --search governance-approved: 1 match, description 'Maintainer approval for changes to governance files.'"
        status: pass
    human_judgment: false
  - id: D3
    description: "GRADLE_ENCRYPTION_KEY secret present (value never shown)"
    verification:
      - kind: other
        ref: "gh secret list --json name: GRADLE_ENCRYPTION_KEY listed"
        status: pass
    human_judgment: false
  - id: D4
    description: "Ruleset main applied with required checks ci-success and governance-success and the admin bypass actor confirmed"
    requirement: "GH-06"
    verification: []
    human_judgment: true
    rationale: "Deliberately deferred by the author until both required contexts are observed green on the phase PR; scripts/apply-github-settings.sh --dry-run --only ruleset still reports 'ruleset main: create'"
  - id: D5
    description: "Author-only steps: Renovate app, drift-check token, REUSE registration, lefthook install"
    requirement: "CICD-04"
    verification: []
    human_judgment: true
    rationale: "These run under the author's identity; the executor observed only secret names and Renovate's onboarding pull request, see the table in this SUMMARY"

duration: 10 min (this continuation; Tasks 1 and 2 were author interaction handled by earlier continuations and the orchestrator)
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 14: Approval-Gated GitHub Settings Summary

**Author-approved merge and security settings, the `governance-approved` label and the configuration-cache key are live and verified read-only (`--check` scoped to `repository security` reports no drift); the `main` ruleset is deliberately deferred until both required contexts are green on the phase PR.**

## Performance

- **Duration:** 10 min for this continuation (Task 3 and close-out)
- **Started (this continuation):** 2026-11-21T12:42:21Z
- **Completed:** 2026-11-21T12:46:00Z
- **Tasks:** 3 (decision, human-action, auto)
- **Files modified:** 0 repository files (plus this SUMMARY and planning state)

## Accomplishments

- Task 1 approval recorded and honoured: only the approved items were written, by the author.
- Live settings verified read-only: `--check --only repository --only security` exits 0 with "no drift".
- The scope record `SETTINGS_DRIFT_SECTIONS` equals, as a set, the applied sections, so neither this plan's gate nor the scheduled `settings-drift.yml` can turn red because of the deferred ruleset.
- `governance-approved` exists before the phase PR is opened, so `governance-success` can pass on the first PR that touches governance files.
- The executor changed nothing on GitHub in this plan.

## Approval (Task 1, verbatim)

Author's decision: **"apply-without-ruleset, пункт 4 с точкой"**, that is:

| Item | Decision |
| --- | --- |
| 1. Ruleset `main` | Deferred (see "Deferred ruleset") |
| 2. Merge settings | Approved |
| 3. Security features | Approved |
| 4. Label `governance-approved` | Approved, with a trailing period in the description: `Maintainer approval for changes to governance files.` (the plan text had no period) |
| 5. `GRADLE_ENCRYPTION_KEY` | Approved |
| Scope record | `SETTINGS_DRIFT_SECTIONS="repository security"` |

Resulting section scope: applied = `repository`, `security`; deferred = `ruleset`. A strict non-empty subset, so the variable is required and was set.

## Commands the author ran (Task 2, observed by the orchestrator)

All run by the author in their own session (D-15); none by the executor.

1. `scripts/apply-github-settings.sh --apply --only repository`: 7 deltas applied; "applied; the live settings match the declared ones".
2. `scripts/apply-github-settings.sh --apply --only security`: `vulnerability_alerts` and `private_vulnerability_reporting` set to true; "applied; the live settings match the declared ones".
3. `gh variable set SETTINGS_DRIFT_SECTIONS --repo aanbrn/axon5-dcb-courses --body "repository security"`.
4. `gh label create governance-approved ... --description "Maintainer approval for changes to governance files." --force`.
5. `openssl rand -base64 16 | gh secret set GRADLE_ENCRYPTION_KEY --repo aanbrn/axon5-dcb-courses` (value never shown).

Reply: "done". Item 1's command (`--apply --only ruleset`) was not run, as approved.

## Verification (Task 3, executor, read-only)

| Check | Output | Wall clock |
| --- | --- | --- |
| Gate 1: `gh variable get SETTINGS_DRIFT_SECTIONS`, then `scripts/apply-github-settings.sh --check --only repository --only security` | `drift scope: repository security`, `no drift`, rc 0 | 4.4 s |
| `gh label list --search governance-approved` (count) | `1`; description `Maintainer approval for changes to governance files.` | 1.0 s |
| `gh secret list` (count of `GRADLE_ENCRYPTION_KEY`) | `1` | 0.7 s |
| `gh secret list` (names) | `GRADLE_ENCRYPTION_KEY`, `SETTINGS_READ_TOKEN` | under 1 s |
| `gh variable list` (names) | `SETTINGS_DRIFT_SECTIONS` | under 1 s |
| `scripts/apply-github-settings.sh --dry-run --only ruleset` (confirms the deferral) | `ruleset main: create`, 1 delta, nothing written | about 4 s |

The printed drift scope equals the applied sections as a set, and no `<automated>` command exceeded 30 s (budget 180 s), so there is no validation gap. No secret value appeared anywhere.

## Deferred ruleset

- **What:** ruleset `main` (PR required with 0 approvals, squash only, linear history, no force-push or deletion, required contexts `ci-success` and `governance-success` bound to the GitHub Actions app, admin bypass through pull requests only).
- **Trigger:** `ci-success` and `governance-success` have both been observed green on the phase PR.
- **Steps for the author, in order:**
  1. `scripts/apply-github-settings.sh --apply --only ruleset`
  2. Confirm the printed bypass actor is the repository admin role (Assumption A1: `RepositoryRole` 5, mode `pull_request`).
  3. `gh variable delete SETTINGS_DRIFT_SECTIONS --repo aanbrn/axon5-dcb-courses` (all three sections are then checked again; or `gh variable set ... --body "ruleset repository security"`).
  4. Optionally `gh workflow run settings-drift.yml` and `scripts/apply-github-settings.sh --check`.
- **Until then:** the phase PR is not protected by the ruleset, and `main` accepts direct pushes. GH-06 stays open for this reason.

## Author-only steps

| Step | State | Evidence or trigger |
| --- | --- | --- |
| Drift-check token `SETTINGS_READ_TOKEN` | **Verified done** (name only) | `gh secret list` lists it. Its permission scope (Administration read, Metadata read) cannot be read; the scheduled drift check is the proof. Created after sections were applied, as the plan required |
| Renovate app (D-11) | **Installed, onboarding open** | Pull request #1 "Configure Renovate" (`renovate/configure`, opened 2026-11-21T12:31:18Z) exists, so the app was installed before the phase PR merged, not after. **Open follow-up for the author: close PR #1 without merging** (the plan's instruction for an early install); Renovate then reads `renovate.json5` from `main` after the phase PR merges. No Dependency Dashboard issue yet. The executor did not close it (GitHub write) |
| REUSE registration at api.reuse.software | **Open, unverified** | Not observable without sending the project name to a third party, which the executor did not do. Trigger: the author registers, any time; the README badge shows its status |
| `lefthook install` | **Open, unverified** | The shared hooks directory contains only `prepare-commit-msg`, no `pre-commit`, so the lefthook hook does not appear to be installed in the user's checkout. Optional per the plan; the author runs it once in their own clone |

## Task Commits

No repository files changed in this plan (`files_modified: []`), so there are no task commits. Task 1 was a recorded decision, Task 2 was author-run commands, and Task 3 was read-only verification.

**Plan metadata:** the closing docs commit (this SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Files Created/Modified

None. Live GitHub state only.

## Decisions Made

See `key-decisions`. Requirement bookkeeping, a judgement call: GH-07 (merge settings, PR title and body as the squash message, head branches deleted) and AIDEV-06 (the label now exists on top of plan 01-09's gate) are marked complete. GH-06 stays pending because its ruleset is deferred and CodeQL and Scorecard first run from `main`. CICD-04 stays pending because Renovate cannot read `renovate.json5` until the phase PR merges.

## Deviations from Plan

None in execution. One wording difference was approved by the author: the label description carries a trailing period, unlike the plan text (Task 1 decision, not an executor deviation). The requirements the plan lists were not all marked complete, as explained under Decisions Made.

## Issues Encountered

- A Renovate onboarding pull request appeared during execution because the author installed the app early (see the table above).
- The session harness refuses compound commands containing the substring `git`; verification commands ran as single commands.

## Known Stubs

None.

## Threat Flags

None. T-01-35 (no write beyond the approved scope: the executor only read), T-01-36 (no secret value shown or transcribed; names only), T-01-37 (the ruleset is deferred, so required contexts cannot block merges), T-01-38 (REUSE and Renovate performed by the author; the executor sent no personal data) are mitigated as planned. T-01-SC: no packages installed.

## User Setup Required

Open items for the author (see "Deferred ruleset" and "Author-only steps"): close Renovate PR #1 without merging, register at the REUSE API, optionally `lefthook install`, and after the phase PR's checks are green apply the ruleset and delete `SETTINGS_DRIFT_SECTIONS`.

## Next Phase Readiness

- The phase PR can be opened with `/gsd-ship`: the label exists, merge settings are live (squash only, head branches deleted).
- Required follow-up after the first green run of the phase PR: the ruleset steps above.
- After the phase PR merges: CodeQL, Scorecard and Renovate run from `main`; the labels workflow's first sync deletes labels not in `.github/labels.yml`.

## Self-Check: PASSED

No files were created by this plan beyond this SUMMARY. The read-only checks above were re-run in this session: `--check` rc 0 with scope `repository security`, label count 1, secret count 1.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
