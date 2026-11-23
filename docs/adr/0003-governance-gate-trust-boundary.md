---
status: "accepted"
date: "2026-11-21"
decision-makers: ["repository author (gap-closure decision on review finding WR-06)"]
---

# ADR-0003: The governance gate runs from the pull request's own tree

## Context and Problem Statement

`governance.yml` triggers on `pull_request`. Its checkout takes the default ref, which for that event is the
merge of the pull request head into the base (`refs/pull/<number>/merge`). The workflow definition,
`scripts/governance/list-pr-files.sh`, `scripts/governance/check-pr.sh` and `governance/governed-paths.txt`
therefore all come from the pull request's own tree.

So a pull request that touches a governed file can, in the same change, do any of the following, and
`governance-success` turns green without the `governance-approved` label:

- make the gate script exit 0;
- narrow the governed list to an entry it does not touch (a list without entries fails closed, a list with one
  unused entry does not);
- edit the workflow so the aggregating job passes without running the check.

The Phase 1 research assumed the gate would not run pull request code. The implementation does run it.

What else holds such a pull request today:

- The declared ruleset (`.github/rulesets/main.json`, applied once both required contexts are green on the
  phase pull request) requires a pull request into `main` and squash merging, with zero approving reviews and
  no code-owner review.
- No CODEOWNERS file exists.
- Every file of the gate is itself governed (`.github/**`, `scripts/governance/**`, `governance/**`), so a
  pull request that touches them in good faith needs the label anyway. The protection, however, is evaluated
  by the code under change.

Plan 01-09 accepted this residual as threat T-01-20 at ASVS level 1. Review finding WR-06 asked for it to be
fixed or recorded.

## Considered Options

The chosen option, described under Decision Outcome, keeps the `pull_request` trigger and accepts the residual.
The rejected alternatives:

1. `pull_request_target` with a checkout of the base commit only. The workflow file would come from the base
   branch, but the trigger runs in the base repository's context, with access to its secrets and a token that
   can carry write permissions. zizmor reports the trigger in its `dangerous-triggers` audit. A single later
   step that checks out or executes pull request content would turn it into a privileged code-execution path.
   For one maintainer, that risk outweighs the gain.
2. Keeping `pull_request` and checking out the base ref for `scripts/governance/**` and
   `governance/governed-paths.txt` in a second, sparse checkout. The workflow file itself still comes from the
   pull request, which can change the step to run its own copy or drop the step. The hole moves; it does not
   close.
3. A CODEOWNERS file with required code-owner review on the governed paths. With one maintainer, the only code
   owner is also the author of the pull requests, and GitHub does not accept an author's approval of their own
   pull request. Every governed change would need the admin bypass, and the rule adds no second reviewer.

## Decision Outcome

Keep the `pull_request` trigger and the checkout of the pull request's merge ref, and accept the residual for
this one-maintainer repository.

The boundary is stated plainly: the gate guards against accidental governed changes and against an agent that
does not tamper with the gate. It is not a security boundary against a pull request crafted to rewrite it.

Reasons:

- The single maintainer merges every pull request. Through the ruleset, every change to `main` arrives as a
  squash-merged pull request whose diff the maintainer reads, including a rewrite of the gate.
- Claude Code's ask rules cover every governed path for the agent's file-edit tool (`.claude/settings.json`,
  kept in step with `governance/governed-paths.txt` by `checkClaudeAskRules`), so an edit to the gate through
  that tool needs the author's confirmation (AIDEV-08). The rules do not match shell commands that write files,
  so they make an unconfirmed change unlikely rather than impossible; they are a guard rail, not a security
  boundary, and the maintainer's review of the diff remains the control.
- T-01-20 already accepted the residual at ASVS L1. The alternatives under Considered Options either run pull
  request-adjacent logic in a privileged context or do not close the gap.

### Consequences

- `governance-success` stays one of the two required contexts (ADR-0001) and keeps catching unlabelled
  governed changes made in good faith, whether by the maintainer or by the agent. It does not stop an agent
  change that edits the gate itself, whether through a confirmed edit or through a shell command the ask rules
  do not match; the maintainer's diff review covers that case.
- Before merging, the maintainer reads the diff of every governed file, with particular care for
  `.github/workflows/governance.yml`, `scripts/governance/**` and `governance/**`, because a change there can
  change the verdict of the check that runs on the same pull request.
- Every `pull_request` workflow, `ci.yml` included, runs from the pull request's tree. The same reading duty
  applies to changes in `.github/**`.
- The approval label is not bound to a commit (review finding WR-02). That is a separate residual, deferred by
  the author.

## More Information

### When to revisit

Revisit this decision when either of these holds:

- a second person gets merge rights;
- pull requests from external contributors, including forks, become a regular way to contribute.

Then evaluate the three alternatives again. With two maintainers, CODEOWNERS with required code-owner review
becomes meaningful. With outside contributors, a base-only `pull_request_target` gate, reviewed under zizmor,
becomes worth evaluating.
