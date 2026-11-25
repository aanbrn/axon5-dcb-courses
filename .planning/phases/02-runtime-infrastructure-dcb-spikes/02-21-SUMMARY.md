---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 21
subsystem: infra
tags: [gsd, agent-skills, governance, adr]
requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: plan 02-05 approved plugins and skills survey; plan 02-19 normalised ADR-0004 and checkAdrs
provides:
  - approved per-agent agent_skills mapping in .planning/config.json and governance/gsd-config-policy.json
  - ADR-0004 section recording the mapping, reasons, approval and caveats
affects: [every GSD agent spawn from Phase 3 onward]
status: complete
actuals:
  tokens: 3500
  tasks: 2
  commits: 1
plan_head_before: 984d9949533bc505ad7312463ecefe6cb981d04c
plan_head_after: a0de5f119414306e1d9c34b126b40a02b15d83b9
tech-stack:
  added: []
  patterns: ["agent_skills: project skill by path, plugin skill as global:<plugin>:<skill>"]
key-files:
  created: []
  modified:
    - .planning/config.json
    - governance/gsd-config-policy.json
    - docs/adr/0004-gsd-configuration.md
key-decisions:
  - "Author chose option project-and-plugins on 2026-11-23: spike findings for six agents, axoniq-app-dev for planner, executor and researcher, gradle-best-practices for planner, executor and code reviewer; gradle-wrapper-upgrade not mapped"
requirements-completed: [AIDEV-01]
---

# Phase 2 Plan 21: agent_skills mapping Summary

Per-agent GSD `agent_skills` approved by the author (option `project-and-plugins`) and pinned identically in config and policy, with the mapping and its reasons recorded in ADR-0004.

## What was done

- Task 1 (checkpoint:decision, blocking-human): the author approved `project-and-plugins` on 2026-11-23, no `gradle-wrapper-upgrade`. No commit.
- Task 2: wrote the identical `agent_skills` object into `.planning/config.json` and `governance/gsd-config-policy.json` (no other key touched, no `config-set`), and added a "GSD agents load skills through `agent_skills`" decision section to ADR-0004 (mapping table, approval, caveats), with `date` set to 2026-11-23. Commit `a0de5f1`.

Mapping:

| Agent | Skills |
| --- | --- |
| gsd-planner | spike findings, axoniq-app-dev, gradle-best-practices |
| gsd-executor | spike findings, axoniq-app-dev, gradle-best-practices |
| gsd-phase-researcher | spike findings, axoniq-app-dev |
| gsd-pattern-mapper | spike findings |
| gsd-code-reviewer | spike findings, gradle-best-practices |
| gsd-verifier | spike findings |

## Verification

- `mise x -- ./gradlew checkGsdConfigPolicy checkAdrs spotlessCheck --console=plain`: BUILD SUCCESSFUL (spotlessApply was needed once to collapse single-element arrays in the policy file and re-wrap the ADR table).
- jq equality of `.agent_skills` between the two files: equal; `length` is 6.
- `gsd-tools query agent-skills <agent> --skills-only --cwd <worktree>` output (the "defaults.json" warning line omitted):
  - gsd-planner: `@.claude/skills/spike-findings-axon5-dcb-courses/SKILL.md`; load `axoniq-app-development:axoniq-app-dev`; load `gradle-skills:gradle-best-practices`.
  - gsd-executor: same three as the planner.
  - gsd-phase-researcher: spike findings; `axoniq-app-development:axoniq-app-dev`.
  - gsd-pattern-mapper: spike findings only.
  - gsd-code-reviewer: spike findings; `gradle-skills:gradle-best-practices`.
  - gsd-verifier: spike findings only.

## Deviations from Plan

### Adjusted wording

**1. ADR caveat on uninstalled plugins**
- The dispatch asked to record that `global:` entries "are skipped" until the plugins are installed. The `agent-skills` query shows GSD still emits a "Load the `...` skill via the Skill tool (plugin-provided)" instruction for them regardless of installation. The ADR therefore says the entries have no effect until the plugins are installed: GSD still tells the agent to load them and the load finds no such skill. Behaviour on a missing plugin skill was not tested.
- Files: `docs/adr/0004-gsd-configuration.md`. Commit: a0de5f1.

Otherwise: none, the plan executed as written.

## Notes

- The ledger file `gsd-plan-head-before-02-21` could not be written (sandbox refuses compound git commands); `plan_head_before` is the dispatch base commit 984d994. `commits` is measured at SUMMARY write time (1 task commit, before this SUMMARY commit).
- The pull request carrying these changes needs the `governance-approved` label (both config files are governed paths).
- Pending human check: the plan 02-05 plugins are not installed on the author's machine yet (`/plugin` step), so the `global:` skills do nothing until then.

## Known Stubs

None.

## Threat Flags

None. T-02-48 mitigations hold: only the project skill and the plugins approved in plan 02-05 are mapped, the author approved, and the policy pins the value.

## Self-Check: PASSED

- `.planning/config.json`, `governance/gsd-config-policy.json`, `docs/adr/0004-gsd-configuration.md` modified and committed in a0de5f1 (ancestor of HEAD).
