---
status: "accepted"
date: "2026-11-21"
decision-makers: ["repository author (decision D-21)"]
---

# ADR-0001: Workflow permissions and required status checks

## Context and Problem Statement

The original plan (D-09) asked for two things: every workflow declares `permissions: read-all` as its
default token permission, and a single required status check (`ci-success`) gates merges.

Two findings from the Phase 1 prototype contradict that wording:

- zizmor 1.32.0 reports `permissions: read-all` as `excessive-permissions` (medium severity, non-zero
  exit). A workflow-level `permissions: {}` together with explicit job-level grants is clean.
- A job can only depend (`needs`) on jobs of its own workflow, so `ci-success` cannot aggregate a separate
  governance workflow. The governance check has to run on the `labeled` and `unlabeled` pull request
  events, so folding it into `ci.yml` would re-run the whole build every time a label changes. Letting a
  skipped job stand in for the check would let a later green run mask a failing one.

## Considered Options

- Workflow-level `permissions: {}` with job-level grants, and an aggregator job in each of `ci.yml` and
  `governance.yml` behind the two required contexts `ci-success` and `governance-success` (chosen).
- The wording of D-09: every workflow declares `permissions: read-all`, and a single required status check
  `ci-success` gates merges.
- Folding the governance check into `ci.yml`, so that `ci-success` aggregates it.
- Letting a skipped job stand in for the governance check.

## Decision Outcome

- Every workflow declares `permissions: {}` at workflow level. Each job grants itself only what it needs,
  typically `contents: read`. Every checkout uses `persist-credentials: false`, and every third-party action
  is pinned by full commit SHA with a version comment.
- `ci.yml` and `governance.yml` each end in an aggregator job that runs with `if: always()` and passes only
  when every needed job finished with the result `success`. Skipped, cancelled and failed jobs all count as
  failure.
- The repository ruleset requires exactly two contexts: `ci-success` and `governance-success`.

### Consequences

- Adding, renaming or removing a job inside `ci.yml` or `governance.yml` never changes the ruleset, because
  only the aggregators are required.
- Both aggregators must keep `if: always()` and the strict `success` comparison. A change that weakens
  either turns the required context into a gate that cannot fail.
- The two contexts are visible deviations from the wording of D-09. This note is the record of that
  deviation; the author accepted it as D-21.
- A workflow added later (for example CodeQL or Scorecard) is not part of the required contexts unless an
  aggregator is added for it and the ruleset is updated deliberately.
- The required `governance-success` context is evaluated by code from the pull request's own tree.
  [ADR-0003](0003-governance-gate-trust-boundary.md) records why that residual is accepted and when to revisit
  it.
