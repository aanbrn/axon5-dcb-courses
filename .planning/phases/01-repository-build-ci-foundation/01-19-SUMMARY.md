---
phase: 01-repository-build-ci-foundation
plan: 19
subsystem: infra
tags: [governance, renovate, mise, claude-code, ask-rules, gap-closure]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "plan 01-16 governed-paths grammar, 01-17 boundary section and `boundary_case` helper, 01-18 fail-closed changed files list"
provides:
  - "governance/governed-paths.txt with 54 entries covering every Renovate config name and every mise config, rc and sidecar-lock location CI reads, with the reasoning and the exclusions written into the file"
  - "25 new `Edit(/<entry>)` ask rules (54 in total, `Bash(*config-set*)` still last)"
  - "31 new check-pr.sh test cases (122 in total) pinning each governed name, four recorded exclusions and the unchanged renovate[bot] exemption"
  - "WR-07 recorded fixed in 01-REVIEW-DISPOSITION.md (open count 7)"
affects: [phase-01-gap-closure, 01-20, ci, governance]

actuals:
  tokens: 9000
  tasks: 2
  commits: 2
plan_head_before: 470fa2ce0cefba5583653604e017f8a91b3c50bc
plan_head_after: 3529028b4f1d5fc54ec1ceb53cd6fa157a6dc05e

tech-stack:
  added: []
  patterns:
    - "Governed list entry groups carry their reasoning as comment lines directly above the entries"
    - "Recorded exclusions are pinned by ungoverned boundary cases against the real list"

key-files:
  created: []
  modified:
    - governance/governed-paths.txt
    - .claude/settings.json
    - scripts/governance/test-check-pr.sh
    - .planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md

key-decisions:
  - "Governed every name in Renovate's documented search order (renovate.json, renovate.jsonc, .gitlab/*, .renovaterc*, root package.json); .github/renovate.* stays covered by .github/**"
  - "Governed every mise config and rc name read from the repository root, plus .mise/locks/**; excluded environment files, subdirectory configs, alternate lockfiles, .java-version, .nvmrc and nested package.json with the reasons recorded in the file header"
  - "Did not extend the renovate[bot] version-only exemption and did not touch check-pr.sh (author decision); the sidecar cost is pinned by a test and recorded as a follow-up"

patterns-established:
  - "Adding a governed entry means one list line, one ask rule in list order and one boundary case"

requirements-completed: [AIDEV-06, AIDEV-08]

coverage:
  - id: D1
    description: "A pull request adding or changing renovate.json, renovate.jsonc or a sidecar lockfile fails governance-success without the label, and the agent's ask rules cover the same paths"
    requirement: "AIDEV-06"
    verification:
      - kind: unit
        ref: "scripts/governance/test-check-pr.sh#a pull request adding renovate.json without the label fails"
        status: pass
      - kind: integration
        ref: "mise x -- ./gradlew checkClaudeAskRules"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every Renovate config name and every mise config, rc and lock location CI honours is governed, with 22 further boundary cases and four ungoverned exclusion cases"
    requirement: "AIDEV-08"
    verification:
      - kind: unit
        ref: "scripts/governance/test-check-pr.sh (122 passed, 0 failed)"
        status: pass
      - kind: integration
        ref: "mise x -- ./gradlew check"
        status: pass
    human_judgment: false
  - id: D3
    description: "The recorded reasoning for each governed name and each exclusion in governed-paths.txt is accurate and adequate"
    verification: []
    human_judgment: true
    rationale: "Prose correctness about Renovate and mise lookup behaviour is not asserted by any test"

duration: 6min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 19: Close WR-07 Summary

**Governed list extended from 29 to 54 entries so every Renovate config name, every mise config and rc name CI honours and the sidecar lockfiles need the approval label and make Claude Code ask, with reasoning and exclusions recorded in the file.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-11-21T22:28:00Z
- **Completed:** 2026-11-21T22:35:00Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Task 1 (tracer): `renovate.json`, `renovate.jsonc` and `.mise/locks/**` governed end to end (list, ask rules, tests). A pull request adding `renovate.json` now fails without the label, the reviewer's scenario.
- Task 2: 22 further entries (Renovate `.gitlab/*`, `.renovaterc*`, root `package.json`, and 14 mise config and rc names), each with an ask rule and a boundary case.
- The file header and the two section comments record why each name is governed, and why mise environment files, subdirectory configs, alternate lockfiles, `.java-version`, `.nvmrc` and nested `package.json` files stay outside. Four ungoverned cases pin those exclusions.
- WR-07 recorded `fixed`, `open` count 7, WR-02 still `deferred`.

## Task Commits

1. **Task 1: precedence-winning Renovate names and sidecar lockfiles** - `90a8a4a` (feat)
2. **Task 2: every Renovate name and mise config, rc and lock location, plus the disposition** - `3529028` (feat)

**Plan metadata:** committed with this SUMMARY (docs: complete plan)

## Files Created/Modified

- `governance/governed-paths.txt` - 54 entries, new "Tool pins" and "Renovate configuration" sections, five exclusion bullets
- `.claude/settings.json` - 54 `Edit(/<entry>)` rules in list order, `Bash(*config-set*)` last
- `scripts/governance/test-check-pr.sh` - WR-07 subsection: 25 governed and 4 ungoverned boundary cases, adding `renovate.json`, bot-added sidecar lockfile
- `.planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md` - WR-07 fixed, open 7

## Verification

- `test-check-pr.sh`: 95 passed after Task 1, 122 passed, 0 failed after Task 2
- `test-list-pr-files.sh`: 25 passed, 0 failed
- `mise x -- ./gradlew check`, `checkClaudeAskRules`, `spotlessCheck`: green
- `editorconfig-checker`, `reuse lint`, `shellcheck test-check-pr.sh`: clean
- `jq -r '.permissions.ask[-1]'` prints `Bash(*config-set*)`; ask list length 55
- All acceptance criteria counts matched (54 entries, 54 Edit rules, 22 new entries, 10 original entries, 6 `disposition: fixed`, 1 `deferred`)

## Decisions Made

- Followed the plan as written: root `package.json` and `.gitlab/renovate.*` are governed (author did not veto).
- `check-pr.sh` was not modified and the renovate[bot] version-only exemption is unchanged.

## Deviations from Plan

None - plan executed exactly as written.

One execution note, not a deviation: the per-plan commit ledger file under the shared git directory could not be written because the session harness refuses writes outside the worktree. `commits: 2` was measured against the recorded base `470fa2c` (`git rev-list --count 470fa2c..HEAD`) instead.

## Issues Encountered

None. The harness refused compound commands that name git together with other tokens; they were split into plain commands.

## Known Stubs

None.

## Threat Flags

None. The plan's threat register (T-01-52 to T-01-55) is addressed by the list, the ask rules and the boundary cases.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- WR-06 (plan 01-20) is the remaining warning-level gap.
- **Possible follow-up accepted by the author (T-01-54):** governing `.mise/locks/**` takes Renovate's `npm:renovate` and `pipx:reuse` bumps that rewrite a sidecar out of the version-only exemption, because a new sidecar directory means added files, which the exemption never covers. Those pull requests now need the `governance-approved` label. This is pinned by the test `renovate adding a mise sidecar lockfile needs the label`. Extending the exemption, for example to accept a bot's sidecar directory swap that matches a version-only `mise.toml`/`mise.lock` change, is a separate decision and was not made here.
- A root `package.json` added in a later phase becomes governed (a version-only modification by renovate[bot] still takes the exemption).

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*

## Self-Check: PASSED

- All four modified files exist; commits `90a8a4a` and `3529028` are on the branch.
- `git rev-list --count 470fa2c..HEAD` returned 2 before this SUMMARY commit.
