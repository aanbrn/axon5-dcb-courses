---
phase: quick-261125-pep
plan: 01
subsystem: agent-tooling
tags: [claude-code, skills, dcb, reuse, gsd, governance]
requires: []
provides:
  - vendored dynamic-consistency-boundaries skill, byte-identical to AxonIQ/AxonFramework f521585912e2915d086158f62e1e6e2c351ae39b
  - claude-tracking allow-list and .gitignore exception for exactly that directory
  - agent_skills mapping of the skill to gsd-planner, gsd-executor, gsd-phase-researcher and gsd-verifier
affects: [phase-3-planning, docs/tooling.md, ADR-0004, ADR-0014]
tech-stack:
  added: []
  patterns: [vendored byte-identical third-party skill with a project-owned provenance README]
key-files:
  created:
    - .claude/skills/dynamic-consistency-boundaries/SKILL.md
    - .claude/skills/dynamic-consistency-boundaries/references/axon-framework-api.md
    - .claude/skills/dynamic-consistency-boundaries/references/axon-server-api.md
    - .claude/skills/dynamic-consistency-boundaries/references/concepts.md
    - .claude/skills/dynamic-consistency-boundaries/references/examples.md
    - .claude/skills/dynamic-consistency-boundaries/README.md
  modified:
    - .gitignore
    - REUSE.toml
    - NOTICE
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - .planning/config.json
    - governance/gsd-config-policy.json
    - docs/adr/0004-gsd-configuration.md
    - docs/adr/0014-claude-code-skills-and-mcp-servers.md
    - docs/tooling.md
    - .claude/CLAUDE.md
key-decisions:
  - "Vendor the DCB skill byte for byte at a pinned commit; the ADR-0014 amendment records the author decision amending AIDEV-01 and AIDEV-03 for this one skill"
  - "No Renovate manager tracks the vendored skill; the README procedure (check on Axon Framework updates and before DCB phases) applies instead"
  - "Map the skill to the planner, executor, phase researcher and verifier only, not the pattern mapper or the code reviewer"
requirements-completed: [AIDEV-01, AIDEV-03, AIDEV-05, CICD-09, DOC-03]
duration: 12min
completed: 2026-11-25
status: complete
commits: 3
plan_head_before: ae03d84a825ffc2ed793c596bf1e06f4e3b9177f
plan_head_after: 52883f1aaac68d4213edc00159e358496b675ecc
actuals:
  tokens: 60000
  tasks: 3
  commits: 3
---

# Phase quick-261125-pep Plan 01: Vendor the AxonFramework DCB skill Summary

The `dynamic-consistency-boundaries` skill of AxonIQ/AxonFramework is vendored byte for byte at commit
`f521585912e2915d086158f62e1e6e2c351ae39b`, tracked, licensed (Apache-2.0 through REUSE.toml and NOTICE), mapped to four
GSD agents and documented in docs/tooling.md, ADR-0004 and ADR-0014.

## Tasks

| Task | Name                                                         | Commit    |
| ---- | ------------------------------------------------------------ | --------- |
| 1    | Vendor the five files and make the repository accept them    | `2dcee99` |
| 2    | Map the skill to the DCB-facing GSD agents (config, policy, ADR-0004, CLAUDE.md) | `e25e396` |
| 3    | Document the vendored-skill mode and amend ADR-0014          | `52883f1` |

## Verification

- Byte identity: the five SHA-256 digests match the planning-time values, the git blob ids match the upstream blob ids,
  and a `git show <pin>:<path> | cmp` per file against the pinned commit passes for all five.
- Tracking: doctor reports `ok  claude-tracking: ... (60 files)`; `scripts/test-doctor.sh` went from 365 to 370
  assertions (RED seen first: 2 failures before the doctor change, then 370 passed, 0 failed), including the rejected
  lookalike directory `dynamic-consistency-boundaries-x`.
- Licensing: `reuse lint` passes (537 of 537 files); `reuse spdx` resolves SKILL.md and references to Apache-2.0 with
  `2026 AxonIQ B.V. and the Axon Framework contributors` and the README to MIT with the author's copyright only.
- Directory holds exactly six tracked files, all Markdown.
- Whole CI lint job replayed locally: actionlint, zizmor (offline, no findings), shellcheck on all tracked `*.sh`,
  editorconfig-checker, reuse lint, renovate-config-validator, `docker compose config`, every `scripts/**/test-*.sh`
  (all pass), and the Gradle checks spotlessCheck, checkGsdConfigPolicy, checkToolingInventory, checkAdrs,
  checkClaudeAskRules and checkNotice.
- docs/tooling.md: no commit SHA written, no existing table row re-padded (0 removed `| ` lines against the base).
- No Kotlin or Gradle script changed, so no Qodana run.

### `gsd_run query agent-skills <agent> --skills-only`

Run from the worktree (project root resolved from the working directory); none printed a "Skill not found" warning.

- `gsd-planner`: `@.claude/skills/spike-findings-axon5-dcb-courses/SKILL.md`,
  `@.claude/skills/dynamic-consistency-boundaries/SKILL.md`, then the two plugin skills (axoniq-app-dev,
  gradle-best-practices).
- `gsd-executor`: the same four entries.
- `gsd-phase-researcher`: spike findings, `@.claude/skills/dynamic-consistency-boundaries/SKILL.md`, axoniq-app-dev.
- `gsd-verifier`: spike findings and `@.claude/skills/dynamic-consistency-boundaries/SKILL.md`.
- `gsd-code-reviewer` (spike findings, gradle-best-practices) and `gsd-pattern-mapper` (spike findings) do not list it.

## Renovate decision

No Renovate manager tracks the pin. A custom manager could change only the commit in the README, because copying the new
files needs post-upgrade commands, which Renovate runs only where its operator allows them, so its pull request would
leave the pin and the content disagreeing. A tracker of the framework's main branch would also propose nearly every
framework commit, although the skill has changed in one commit so far. The marketplace plugin pins are updated by hand
for the same reason of review. Recorded in docs/tooling.md (Vendored skills) and ADR-0014.

## Governed files changed

For the merge prompt and the `governance-approved` label (governance/governed-paths.txt):

- `.planning/config.json`
- `governance/gsd-config-policy.json`
- `REUSE.toml`
- `scripts/doctor.sh`

Other tracked files touched: `.gitignore`, `NOTICE`, `scripts/test-doctor.sh`, `.claude/CLAUDE.md`,
`docs/adr/0004-gsd-configuration.md`, `docs/adr/0014-claude-code-skills-and-mcp-servers.md`, `docs/tooling.md`, and the
six files under `.claude/skills/dynamic-consistency-boundaries/`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Renormalisation gate replaced by an equivalent check**
- **Found during:** Task 3
- **Issue:** The `GIT_INDEX_FILE=... git add --renormalize` gate is a compound git command that the worktree sandbox
  refuses to run.
- **Fix:** Checked `git ls-files --eol` instead: every new and changed Markdown file is `i/lf w/lf` under
  `text=auto eol=lf`, so renormalisation cannot change anything. The upstream bytes were also proven by `cmp`.
- **Files modified:** none

**2. [Consistency edit] ADR-0014 wording**
- The existing bullet "nothing is copied into `.claude/`" in the Skills section of ADR-0014 became "no plugin is copied
  into `.claude/`", so it no longer contradicts the new vendored-skill section.
- **Commit:** `52883f1`

Otherwise the plan was executed as written. The `git check-ignore` gate of Task 1 was likewise refused as a compound
command; the tracked six-file listing and the successful `git add` without `-f` show the exception works.

## Known Stubs

None.

## Threat Flags

None. The vendored content is Markdown only; no new endpoint, auth path or schema.

## Self-Check: PASSED

- Files: the six vendored files, `.planning/config.json`, `governance/gsd-config-policy.json`, `REUSE.toml`, `NOTICE`,
  `scripts/doctor.sh`, `docs/tooling.md`, ADR-0004 and ADR-0014 exist and carry the changes above.
- Commits `2dcee99`, `e25e396` and `52883f1` are ancestors of HEAD (`git rev-list --count` from the plan base: 3).
- Nothing pushed. STATE.md and ROADMAP.md untouched.
