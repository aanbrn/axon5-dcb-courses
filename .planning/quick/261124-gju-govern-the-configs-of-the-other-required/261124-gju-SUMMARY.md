---
phase: quick-261124-gju
plan: 01
subsystem: governance, renovate
tags: [governance, governed-paths, ask-rules, renovate, cooldown, ci-gates]
requires: []
provides:
  - governed entries for the configuration and launchers of every required CI gate (58 to 80 entries)
  - one Edit ask rule per new entry (59 to 81 rules)
  - boundary cases for every governed path and every recorded exclusion
  - seven-day Renovate cooldown for npm major, minor and patch updates
affects: [governance/governed-paths.txt, .claude/settings.json, renovate.json5, docs/tooling.md, ADR-0003]
tech-stack:
  added: []
  patterns: [leading `**/` governed entry for a name read at every directory depth]
key-files:
  created: []
  modified:
    - governance/governed-paths.txt
    - .claude/settings.json
    - scripts/governance/test-check-pr.sh
    - scripts/governance/test-ask-governed-edit.sh
    - docs/adr/0003-governance-gate-trust-boundary.md
    - renovate.json5
    - docs/tooling.md
decisions:
  - "Author decision 1: the configuration and launchers of every job that ci-success requires are governed, under every name and depth the tool reads"
  - "Author decision 2: one package rule (npm datasource, major/minor/patch, 7 days) owns the npm cooldown; the GSD rule no longer restates it"
  - "NEVER_EXEMPT_PATHS and the renovate[bot] exemption logic are unchanged: no Renovate manager reads a version from a newly governed path"
requirements-completed: [AIDEV-06, AIDEV-08, CICD-04, TOOL-03]
status: complete
completed: 2026-11-24
commits: 3
plan_head_before: b63208ce2b42392b400319f3f7c46c7846c5a392
plan_head_after: 2a4927f (the commit before the one that carries this SUMMARY, which cannot name its own hash)
actuals:
  tokens: 30000
  tasks: 3
  commits: 3
---

# Phase quick-261124-gju Plan 01: Govern the configs of the other required gates Summary

The governed list now covers the configuration and launchers of every required CI gate (22 new entries, 58 to 80, with
81 ask rules), and Renovate waits seven days for every npm major, minor and patch update through a single package rule.

## What was done

### Task 1 (tracer): `.editorconfig` at every depth, commit dead089

- Entries `.editorconfig` and `**/.editorconfig`, with the format note that a leading `**/` entry does not match the
  root.
- Boundary cases for the root, two nested paths and the near-miss `docs/x.editorconfig`.
- Edit hook cases (`**/.editorconfig` in the test list): `docs/.editorconfig` asks, `docs/x.editorconfig` and the root
  `.editorconfig` stay silent. test-check-pr.sh 136 to 140 passed, test-ask-governed-edit.sh 55 to 58 passed.

### Task 2: remaining configs and launchers, commit 2a4927f

Test cases were written first: 23 cases failed against the Task 1 list (RED), then 171 passed, 0 failed (GREEN).

Newly governed, with the evidence from the planner's probes:

| Path(s) | Job | Evidence |
| --- | --- | --- |
| `.editorconfig`, `**/.editorconfig`, `.editorconfig-checker.json` | lint, build (shfmt) | editorconfig-checker 4.0.2 reads only `.editorconfig-checker.json` at the root; a nested `.editorconfig` with `trim_trailing_whitespace = false` made a violation pass |
| `.gitattributes`, `**/.gitattributes` | lint | git applies attributes per directory; they steer the PR view and `export-ignore` shapes the local Qodana archive |
| `REUSE.toml`, `**/REUSE.toml`, `.reuse/**` | lint | reuse 6.3.0: a nested `REUSE.toml` licensed a header-less file; deprecated `.reuse/dep5` also works |
| `.shellcheckrc`, `shellcheckrc`, `**/.shellcheckrc`, `**/shellcheckrc` | lint (shellcheck, actionlint) | `disable=all` in either name, in the script's directory or a parent, silenced a finding in five probes |
| `zizmor.yml`, `zizmor.yaml` | lint | root names disabled findings (exit 0); the `.github/` names are already under `.github/**` |
| `.prettierrc.yaml`, `.taplo.toml`, `config/spotless/**` | build (Spotless) | passed explicitly from courses.root.gradle.kts, with the licence header templates |
| `.gitleaks.toml`, `.gitleaksignore` | secrets | gitleaks 8.30.1 reads both from the root |
| `qodana.yml` | qodana | the Qodana CLI reads `qodana.yml` before the governed `qodana.yaml`, while `scripts/qodana.sh` validates only `qodana.yaml` |
| `scripts/doctor.sh`, `tooling/doctor/**` | doctor | the job runs `scripts/doctor.sh --ci`, which runs `./gradlew doctor`, registered with its probe in tooling/doctor |

Deliberately left out (each recorded with its reason in the header of governance/governed-paths.txt and pinned by an
`ungoverned` boundary case, except `.idea/` and the Prettier names that have no sample path):

- `lefthook.yml`, `scripts/format-staged.sh`: local pre-commit hook, CI runs every check itself.
- `.gitignore`: CI checks out tracked files only and every gate selects tracked files.
- `.ecrc`: editorconfig-checker 4 reads only `.editorconfig-checker.json`. `taplo.toml`: Spotless passes `--config .taplo.toml`.
  Other Prettier config names and `.prettierignore`: Spotless passes `.prettierrc.yaml` and never runs the Prettier CLI.
- `.idea/`: git-ignored, and `scripts/qodana.sh` refuses a directory that contains it.
- `scripts/test-doctor.sh`, `tooling/architecture/**`: test code, like the functional tests.
- `scripts/upstream/**`: the upstream-records job is advisory (UPS-04).

The header also records the suppression-marker residual (shellcheck directives, editorconfig-checker disable comments,
`gitleaks:allow`, REUSE ignore blocks, zizmor ignore comments stay ungoverned and show in the reviewed diff).

ADR-0003 carries the consequence bullet. The renovate[bot] exemption is unchanged: the Renovate extract-mode invariant
(no extracted dependency in any newly governed path) holds, `NEVER_EXEMPT_PATHS` is untouched and all pre-existing
cases still pass.

### Task 3: seven-day npm cooldown, commit with this SUMMARY

- First package rule of renovate.json5: `matchDatasources: ["npm"]`, `matchUpdateTypes: ["major", "minor", "patch"]`,
  `minimumReleaseAge: "7 days"`, with the comment explaining why it must come first and why only these update types.
- The GSD rule lost its own `minimumReleaseAge` and the restating comment; its description was shortened.
- The top-level cooldown comment, the rule-order comment and the GSD updates section of docs/tooling.md name the npm
  rule as the source of the seven days.

Lookup records (`renovate --platform=local --dry-run=lookup`, 2026-11-24 about 05:21Z; date-dependent, not a gate):

| Update | Before: newVersion / releaseTimestamp / pendingChecks | After: newVersion / releaseTimestamp / pendingChecks |
| --- | --- | --- |
| `npm:renovate` (minor) | 44.257.6 / 2026-11-21T03:54Z / none | 44.259.0 / 2026-11-24T00:53Z / pending |
| `npm:@upstash/context7-mcp` (patch) | 4.7.3 / 2026-11-17T13:56Z / none | 4.7.1 / 2026-11-16T14:56Z / none (8 days old) |
| `npm:@playwright/mcp` (patch) | 0.0.86 / 2026-11-18T18:54Z / none | 0.0.86 / 2026-11-18T18:54Z / pending |
| `@opengsd/gsd-core` (minor) | 1.23.0 / 2026-11-23T02:38Z / pending (own rule) | 1.23.0 / 2026-11-23T02:38Z / pending (npm rule) |

The property gate printed `false` before the change (three violators) and `true` after.

## Flagged for the author

1. `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` wires `config/detekt/detekt.yml` and
   `allWarningsAsErrors` into every module and stays outside the list under the recorded 01-17/01-19 exclusion. An edit
   to it can relax detekt and the compiler gate. Not changed here.
2. Optional hardening: make `scripts/qodana.sh` refuse a `qodana.yml` beside `qodana.yaml`. Governance already covers it.
3. `minimumReleaseAgePackageManager` for the npm package manager: no package manager files exist yet; revisit when a
   `package.json` lands in the frontend phase.
4. STATE.md still lists the open decision for this work; the orchestrator removes it.

## Deviations from Plan

None - the plan executed as written.

One environment note: the `functionalTest` tasks were served from the Gradle build cache in the Task 2 run, so the
GovernedPaths and AskRules functional tests ran for real in the Task 1 run (2m24s, green) and from cache afterwards;
`checkClaudeAskRules` ran in both.

## Verification

- test-check-pr.sh 171 passed, test-ask-governed-edit.sh 58 passed, test-ask-governed-merge.sh 120 passed, test-doctor.sh 139 passed.
- `checkClaudeAskRules checkToolingInventory checkAdrs spotlessCheck` and the GovernedPaths/AskRules functional tests green.
- `renovate-config-validator --strict renovate.json5`, shellcheck on the changed scripts, editorconfig-checker and reuse lint pass.
- Renovate extract invariant: true. Lookup gate: true.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

Files and commits dead089 and 2a4927f verified present.
