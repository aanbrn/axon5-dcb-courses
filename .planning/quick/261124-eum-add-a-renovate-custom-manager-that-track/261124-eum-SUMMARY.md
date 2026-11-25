---
phase: quick-261124-eum
plan: 01
subsystem: tooling
tags: [renovate, governance, gsd, doctor, docs]
requires: []
provides:
  - Renovate regex custom manager reading governance/gsd-version as the npm package @opengsd/gsd-core
  - NEVER_EXEMPT_PATHS in the governance gate (GSD pin never takes the renovate[bot] exemption)
  - doctor reinstall fix for a GSD version mismatch
  - "GSD updates" section in docs/tooling.md and an ADR-0003 consequence
affects: [renovate.json5, scripts/governance, scripts/doctor.sh, docs]
tech-stack:
  added: []
  patterns: [never-exempt path list in the governance gate]
key-files:
  created: []
  modified:
    - renovate.json5
    - scripts/governance/check-pr.sh
    - scripts/governance/test-check-pr.sh
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - docs/tooling.md
    - docs/adr/0003-governance-gate-trust-boundary.md
key-decisions:
  - "The renovate[bot] version-only exemption deliberately does not cover governance/gsd-version (author decision 2026-11-24)"
  - "minimumReleaseAge 7 days restated on the GSD package rule, because config:best-practices lowers npm packages to three days"
requirements-completed: [CICD-04, AIDEV-07, AIDEV-06]
duration: about 25 min
completed: 2026-11-24
status: complete
commits: 3
plan_head_before: f2732ca912edcc437a9c8b900a8772797f4fd5f8
plan_head_after: the commit that contains this file (task 3 commit; see git log)
actuals:
  tokens: 14000
  tasks: 3
  commits: 3
---

# Phase quick-261124-eum Plan 01: Renovate tracks the GSD pin, adoption stays human-gated

Renovate now reads `governance/gsd-version` as `@opengsd/gsd-core` (npm, seven-day cooldown, never automerged), the governance gate requires `governance-approved` for a Renovate bump of that pin, doctor tells a stale checkout to reinstall the pin, and the flow is documented.

## What was built

- **Task 1 (tracer)** `renovate.json5`: regex custom manager after the `.java-version` entry (`managerFilePatterns: ["/^governance/gsd-version$/"]`, `depNameTemplate: "@opengsd/gsd-core"`, `datasourceTemplate: "npm"`), and a package rule after the MCP never-automerge rule with `matchDepNames`, `automerge: false`, `minimumReleaseAge: "7 days"` (with the comment on the three-day npm preset) and a templated `prBodyNotes` listing the review steps. No group, no new label. Commit d18d7b6.
- **Task 2** `check-pr.sh`: `NEVER_EXEMPT_PATHS=(governance/gsd-version)`, `is_never_exempt` (exact whole-path match, false for empty), consulted for `filename` and `previous_filename` before the exemption; the failure block prints `governance: never exempt: <path> (...)`. Four new cases in `test-check-pr.sh`. Commit f0f6e54.
- **Task 3** `doctor.sh`: the version-mismatch fix is now `$install_fix`; `test-doctor.sh` expects the install command for the pinned version. `docs/tooling.md`: GSD row rewritten, new `## GSD updates` section (no version numbers). ADR-0003: consequence bullet. Commit: the one that contains this file.

## Evidence

**Task 2 RED** (new cases against the unchanged gate): `133 passed, 3 failed`. Failing:
- "renovate bumping the GSD pin fails without the label and names the file" (exit 0, expected 1)
- "renovate bumping the GSD pin is told the pin is never exempt" (exit 0, expected 1)
- "an otherwise exempt mise.toml bump does not carry the GSD pin through without the label" (exit 0, expected 1)

The fourth new case (bump with the label passes) passes before and after, as expected. **GREEN:** `136 passed, 0 failed`; shellcheck clean.

**Task 1 verification:** `renovate-config-validator --strict` exit 0; local extraction logs `"packageFile":"governance/gsd-version"` with `"depName":"@opengsd/gsd-core"`, `"currentValue":"1.22.0"`, `"datasource":"npm"`; `minimumReleaseAge: "7 days"` appears twice; the pin still holds `1.22.0` (`git diff` against the base shows no change).

**Task 1 lookup record (date-dependent, not a gate):** `newVersion` `1.23.0` (release timestamp 2026-11-23T02:38:10Z), `pendingChecks: true` (release younger than seven days), `branchName` `renovate/opengsd-gsd-core-1.x`.

**Final gates:** `checkToolingInventory checkAdrs spotlessCheck` green; `test-doctor.sh` 139 passed, 0 failed; `editorconfig-checker` exit 0; shellcheck clean on all four scripts.

## Deviations from Plan

None - plan executed exactly as written.

## Follow-ups for the author

- The same three-day npm cooldown gap (`security:minimumReleaseAgeNpm`) affects the existing mise `npm:` tools; not changed here, as the plan instructs.
- The pin itself is not bumped; adopt 1.23.0 through the flow in `docs/tooling.md` after the Phase 2 PR.

## Known Stubs

None.

## Threat Flags

None. The change adds no new network endpoint or trust boundary beyond those in the plan's threat model.

## Edit-hook prompts

No edit-hook approval prompt appeared for governed edits in this worktree (renovate.json5, scripts/governance/**).

## Self-Check: PASSED

- Files modified exist (renovate.json5, check-pr.sh, test-check-pr.sh, doctor.sh, test-doctor.sh, docs/tooling.md, ADR-0003).
- Commits d18d7b6 and f0f6e54 are on the branch.
