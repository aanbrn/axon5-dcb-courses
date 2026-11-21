# ADR-0001: Workflow permissions and required status checks

- Status: accepted
- Date: 2026-11-21
- Deciders: repository author (decision D-21)

This is a minimal record. Phase 2 introduces the ADR templates and normalises the format of this note.

## Context

The original plan (D-09) asked for two things: every workflow declares `permissions: read-all` as its
default token permission, and a single required status check (`ci-success`) gates merges.

Two findings from the Phase 1 prototype contradict that wording:

- zizmor 1.32.0 reports `permissions: read-all` as `excessive-permissions` (medium severity, non-zero
  exit). A workflow-level `permissions: {}` together with explicit job-level grants is clean.
- A job can only depend (`needs`) on jobs of its own workflow, so `ci-success` cannot aggregate a separate
  governance workflow. The governance check has to run on the `labeled` and `unlabeled` pull request
  events, so folding it into `ci.yml` would re-run the whole build every time a label changes. Letting a
  skipped job stand in for the check would let a later green run mask a failing one.

## Decision

- Every workflow declares `permissions: {}` at workflow level. Each job grants itself only what it needs,
  typically `contents: read`. Every checkout uses `persist-credentials: false`, and every third-party action
  is pinned by full commit SHA with a version comment.
- `ci.yml` and `governance.yml` each end in an aggregator job that runs with `if: always()` and passes only
  when every needed job finished with the result `success`. Skipped, cancelled and failed jobs all count as
  failure.
- The repository ruleset requires exactly two contexts: `ci-success` and `governance-success`.

## Consequences

- Adding, renaming or removing a job inside `ci.yml` or `governance.yml` never changes the ruleset, because
  only the aggregators are required.
- Both aggregators must keep `if: always()` and the strict `success` comparison. A change that weakens
  either turns the required context into a gate that cannot fail.
- The two contexts are visible deviations from the wording of D-09. This note is the record of that
  deviation; the author accepted it as D-21.
- A workflow added later (for example CodeQL or Scorecard) is not part of the required contexts unless an
  aggregator is added for it and the ruleset is updated deliberately.
