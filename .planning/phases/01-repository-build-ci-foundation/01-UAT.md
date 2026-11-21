---
status: testing
phase: 01-repository-build-ci-foundation
source: [01-VERIFICATION.md]
started: 2026-11-21T16:45:00Z
updated: 2026-11-21T16:45:00Z
---

## Current Test

number: 1
name: Decide on WR-05 (fix the jq process-substitution fail-open path or record the residual)
expected: |
  A decision by the author: either fix it (write the jq records to a temp file, check jq's status, assert the
  loop saw every entry) or record the residual in ADR-0001. Reproduced: a two-entry list whose second entry is
  .github/workflows/ci.yml with a NUL in its patch prints a jq error, then "governance: no governed file is
  touched", exit 0, no label.
awaiting: user response

## Tests

### 1. Decide on WR-05 (fix the jq process-substitution fail-open path or record the residual)
expected: The author either fixes it (records to a temp file, jq status checked, loop asserted to see every entry) or records the residual in ADR-0001; the reproduced case (NUL in a governed entry's patch) must then fail the gate or be documented as accepted
result: [pending]

### 2. Decide on WR-06 and WR-07, then apply the ruleset
expected: The author either fixes them (base-ref checkout or `pull_request_target`, a real CODEOWNERS with code-owner review, extra governed entries for `renovate.json`, `.renovaterc*` and `.mise/locks/**` plus matching `Edit(/...)` ask rules) or records the accepted residual for a one-maintainer repository in ADR-0001
result: [pending]

### 3. Open the phase PR and let ci.yml and governance.yml run on GitHub
expected: ci-success (build, lint, secrets jobs) and governance-success are green; the PR carries `governance-approved`; the head branch name passes the naming rule; the `List changed files` step prints `governance: listed N changed files`
result: [pending]

### 4. Apply the deferred ruleset after both contexts are green
expected: `scripts/apply-github-settings.sh --apply --only ruleset` makes ruleset `main` live (required contexts ci-success and governance-success bound to integration_id 15368, squash-only, linear history, no force-push or deletion, admin-only bypass); the printed bypass actor is the admin role; after `gh variable delete SETTINGS_DRIFT_SECTIONS --repo aanbrn/axon5-dcb-courses`, `--check` reports no drift for all sections
result: [pending]

### 5. Post-merge CodeQL, Scorecard and first label sync
expected: Code scanning shows the CodeQL (actions) and Scorecard analyses from main; repository labels equal .github/labels.yml after the first sync
result: [pending]

### 6. Close Renovate onboarding PR #1 and check the first grouped PRs
expected: No onboarding PR; update PRs are grouped per renovate.json5 and none appears before the 7-day cooldown (except the documented vulnerability exception and the JDK timestamp-optional waiver)
result: [pending]

### 7. Commit-time hook behaviour in a clone
expected: After `mise install` and `lefthook install`, committing a badly formatted Kotlin file re-stages it formatted; a synthetic GitHub token blocks the commit via gitleaks; reuse, actionlint, zizmor and shellcheck run only on matching staged files
result: [pending]

### 8. Register the repository at api.reuse.software
expected: The README REUSE badge shows `compliant`
result: [pending]

## Summary

total: 8
passed: 0
issues: 0
pending: 8
skipped: 0
blocked: 0

## Gaps
