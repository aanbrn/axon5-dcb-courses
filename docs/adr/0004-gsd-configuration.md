# ADR-0004: GSD configuration as a decision

- Status: accepted
- Date: 2026-11-21
- Deciders: repository author (decision D-14 and the planner web-search setting)

This is a minimal record. Phase 2 introduces the ADR templates and normalises the format of this note.

## Context

GSD reads `.planning/config.json` in every workflow, so a changed key changes how later phases are planned,
executed and verified. GSD also writes keys at runtime through `config-set`, for example
`workflow._auto_chain_active` while it chains phases automatically (Phase 1 research, finding 8), and a GSD
update can add keys.

Requirement AIDEV-05 asks for the project settings to be enforced as policy: a check must fail on any deviation
until the policy itself is changed deliberately.

## Decision: the policy pins the whole file

This implements decision D-14.

- `governance/gsd-config-policy.json` is an exact copy of the accepted `.planning/config.json`.
- `checkGsdConfigPolicy` (`ConfigPolicyCheck`, plan 01-07, part of `check`) compares the two parsed documents.
  It ignores key order and whitespace. It fails on any added, removed or changed key, and it reports key paths
  only, never values, because a configuration file can hold credentials.
- The whole file is pinned rather than a list of keys. Keys added by a GSD update and keys written at runtime
  then become visible, reviewable diffs instead of silent drift.

## Changing the configuration deliberately

1. Change `.planning/config.json`, through GSD or by hand. The ask rules in `.claude/settings.json` (AIDEV-08)
   make the author confirm both routes: the rule for edits to the file and the rule for `config-set` calls.
2. Make the same change to `governance/gsd-config-policy.json` in the same pull request.
   `checkGsdConfigPolicy` passes only when the two agree.
3. Both files are governed paths, so the pull request needs the `governance-approved` label for
   `governance-success` to pass ([ADR-0001](0001-workflow-permissions-and-required-checks.md)). Keep in mind the
   point of [ADR-0003](0003-governance-gate-trust-boundary.md): the gate runs from the pull request's own tree,
   so the label guards against accidental change and the maintainer's reading of the diff remains the control.
4. Record the reason for a non-obvious key in this ADR.

Transient runtime keys such as `workflow._auto_chain_active` are never committed. Reset them before committing,
and never copy them into the policy to make the check pass. The failure message of the check names the
offending key path.

## Decision: the planner may search the web

`agent_tools` gives the `gsd-planner` agent the `WebSearch` tool, and no other agent gets it. The external
search-provider keys in the same file (`brave_search`, `firecrawl`, `exa_search`, `tavily_search`, `ref_search`,
`perplexity` and `jina`) stay `false`.

Why: the toolchain moves fast and a model's training data lags behind it, while the planner writes the facts
that executors act on. Phase 1 supplied the evidence:

- For review finding WR-01 (fixed by plan 01-17), the planner established that Gradle reads a Groovy build or
  settings script before the Kotlin one. That claim rested on a Gradle forum source at MEDIUM confidence, and the
  plan chose the action that is safe either way: governing a file that does not exist costs nothing.
- For review finding WR-07 (fixed by plan 01-19), the planner established Renovate's configuration search order
  and mise's configuration file names. Both sets of names became governed paths.

Risks:

- Web content, whether wrong, outdated or written to steer agents, can shape a plan.
- Query text leaves the machine. Queries therefore carry technical questions only, never secrets, tokens,
  credentials, personal data or private repository content.
- Each planning run costs extra tokens and time.

Mitigations:

- The plan checker (`workflow.plan_check`) reviews every plan before execution.
- Executors prove every result by running the build, the linters and the tests, so a wrong web fact fails a
  gate instead of shipping.
- Source grounding (`plan_review.source_grounding`, authority `grep`) checks the claims of a plan against the
  repository.
- Only the planner has the tool.
- The maintainer reviews the diff of every pull request ([ADR-0003](0003-governance-gate-trust-boundary.md)).

## Rejected alternatives

1. Pinning only selected keys, as the examples in AIDEV-05 suggest. Unlisted keys, and keys added by a GSD
   update, could then change silently.
2. Ignoring keys that start with an underscore automatically. That would make the check lenient by convention.
   It is not adopted, and adopting it would itself be a deliberate policy change.
3. No web access for the planner. Plans would rest on training data alone, and the WR-01 and WR-07 facts were
   not in the repository.
4. Web access for every agent. It would widen the channel through which outside content can steer code changes
   without a gain, because executors prove facts by running tools.

## Consequences

- Every configuration change is a reviewed two-file pull request that carries the `governance-approved` label.
- A GSD update that adds keys fails `check` until the policy is updated deliberately.
- Planning runs cost more.
- Plans may cite web sources, and the executors' runs remain the proof.

## Recording later keys

Further non-obvious keys get a short section in this ADR, in the same pull request that changes the key. The
section states the decision, the reason and a revisit trigger. `model_profile`, `git.branching_strategy` and
`workflow.security_enforcement` are examples of keys that may need one. No decision is recorded for them now.

## When to revisit

- Web search: revisit when a plan proves to have been steered by wrong web content that no gate caught, when the
  plan checker or source grounding is switched off, when planning cost becomes a problem, or when a second
  maintainer or external contributors arrive (as in [ADR-0003](0003-governance-gate-trust-boundary.md)).
- The whole-file pin: revisit when transient keys keep failing the check. Decide then, as a policy change,
  whether to ignore them.
