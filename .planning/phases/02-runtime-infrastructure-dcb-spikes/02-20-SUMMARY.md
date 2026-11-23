---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 20
subsystem: governance
tags: [governed-paths, ask-rules, ci, licences, detekt, tooling-inventory]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "courses.licences convention plugin and checkDependencyLicences (plan 02-13), the tooling inventory and checkToolingInventory (plans 02-02, 02-14), the build job of ci.yml (plans 02-08, 02-12, 02-18)"
provides:
  - "courses.licences.gradle.kts as a governed path with exactly one Edit ask rule"
  - "config/detekt/** as a governed path with exactly one Edit ask rule"
  - "CI build job names checkDependencyLicences, so a build in which no module registers the task fails"
  - "CycloneDX row and the licence-check scope note in docs/tooling.md"
affects: [02-23 ADR-0012 records this outcome, phase 5 npm half of CICD-09, phase 6 image SBOMs]

actuals:
  tokens: 7500
  tasks: 2
  commits: 2
plan_head_before: 984d9949533bc505ad7312463ecefe6cb981d04c
plan_head_after: 9039a61f16bd059349ed38cbf222833ce9dc8813

tech-stack:
  added: []
  patterns:
    - "A module convention plugin that registers a governance check is governed individually, while the line that applies it stays outside the boundary and CI names the task to catch its removal"
    - "Gate-relaxing configuration (the detekt rule set) is governed under a one-line rationale tied to the boundary rule"

key-files:
  created: []
  modified:
    - governance/governed-paths.txt
    - .claude/settings.json
    - .github/workflows/ci.yml
    - docs/tooling.md

key-decisions:
  - "The header's module-convention-plugin bullet states the one exception (courses.licences) and why the applying line in courses.kotlin-library stays outside; the Phase 1 boundary case for courses.kotlin-library.gradle.kts still holds"
  - "CI names checkDependencyLicences on the existing build step instead of adding a job or a second run; D-20 keeps its two parallel jobs"
  - "config/detekt/** is governed as a glob, so a baseline file added later is covered without another entry"

requirements-completed: [CICD-09]

coverage:
  - id: D1
    description: "courses.licences.gradle.kts is a governed path with exactly one Edit ask rule, and checkClaudeAskRules, spotlessCheck and the governance shell tests pass"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew checkClaudeAskRules spotlessCheck --console=plain (BUILD SUCCESSFUL)"
        status: pass
      - kind: integration
        ref: "bash scripts/governance/test-check-pr.sh (132 passed, 0 failed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "config/detekt/** is a governed path with exactly one Edit ask rule; the ask list holds 57 rules"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "jq '.permissions.ask | length' .claude/settings.json prints 57; checkClaudeAskRules passes"
        status: pass
    human_judgment: false
  - id: D3
    description: "CI's build job runs ./gradlew build checkDependencyLicences -x integrationTest; the invocation resolves the task in every module, and actionlint and zizmor are clean"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew build checkDependencyLicences -x integrationTest --dry-run | grep -c ':checkDependencyLicences ' (9)"
        status: pass
      - kind: integration
        ref: "mise x -- actionlint; mise x -- zizmor --offline --no-progress .github/workflows (no findings)"
        status: pass
      - kind: integration
        ref: "mise x -- ./gradlew build checkDependencyLicences -x integrationTest --console=plain (BUILD SUCCESSFUL)"
        status: pass
    human_judgment: false
  - id: D4
    description: "docs/tooling.md carries the CycloneDX row and the licence-check scope note, with no version number"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew checkToolingInventory spotlessCheck (BUILD SUCCESSFUL); version-number grep prints 0"
        status: pass
    human_judgment: false
  - id: D5
    description: "The wording of the scope note (npm developer tools outside the licence check, npm project dependencies joining in Phase 5) reads as an accurate statement of scope"
    verification: []
    human_judgment: true
    rationale: "Whether the scope statement matches the author's intent for the mise-pinned developer tools is a policy call, not something a test asserts."

duration: 12min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 20: Governance of the licence gate and tooling inventory Summary

**The per-module licence gate is a governed path with an ask rule, CI names `checkDependencyLicences` so a build that lost the gate fails instead of skipping it, the detekt rule set is governed, and the tooling inventory records the policy and the npm packages it does not cover**

## Performance

- **Duration:** 12 min (plus an 8.5 min full build run)
- **Started:** 2026-11-23T13:00:00Z
- **Completed:** 2026-11-23T13:20:00Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- `build-logic/src/main/kotlin/courses.licences.gradle.kts` is listed in `governance/governed-paths.txt` with exactly one `Edit(/...)` ask rule in `.claude/settings.json`. The header's module-convention-plugin bullet now states the one exception and why the line in `courses.kotlin-library` that applies the plugin stays outside; the Phase 1 boundary case for that file still passes (132 passed, 0 failed).
- `config/detekt/**` is governed as its own block under a one-line rationale, with exactly one `Edit(/config/detekt/**)` ask rule between the `courses.settings.settings.gradle.kts` rule and the `gradlew` rule. The ask list holds 57 rules (55 plus the two new ones; plan 02-05's marketplace settings are untouched).
- The `build` job's Gradle step runs `./gradlew build checkDependencyLicences -x integrationTest`, with a comment explaining that Gradle fails with "Task 'checkDependencyLicences' not found" when no module registers it. No job, permission, action pin or second run of the check was added (D-20 intact). actionlint and zizmor report no findings.
- `docs/tooling.md` has a CycloneDX row and a note in the Notes section: the policy in `governance/dependency-licences.json`, the per-module `checkDependencyLicences` from `check`, CI naming the task, and the `NOTICE` mirror checked by `checkNotice`. The note states that the mise-pinned npm developer tools (Context7 and Playwright MCP servers) are outside the licence check and are reviewed by the AIDEV-04 approval gate instead, and that npm project dependencies join the same policy in Phase 5.

## Task Commits

1. **Task 1: Governance of the licence gate and the detekt rule set (tracer)** - `ea7c632` (feat)
2. **Task 2: CycloneDX row and licence-check scope note** - `9039a61` (docs)

**Plan metadata:** committed with this SUMMARY (docs).

## Tracer verification

Task 1 is a tracer. `workflow.human_verify_mode` is the default `end-of-phase` and the tracer's `<verify>` holds only `<automated>` commands, so no checkpoint was raised. All three automated commands passed before expansion: `checkClaudeAskRules spotlessCheck` plus the governance shell tests, actionlint plus zizmor, and the dry-run grep (HEAD value 0 per the plan, now 9, one per Kotlin module).

## Files Created/Modified

- `governance/governed-paths.txt` - governed entries for `courses.licences.gradle.kts` and `config/detekt/**`, amended boundary note
- `.claude/settings.json` - two new `Edit` ask rules in the order of `governed-paths.txt`
- `.github/workflows/ci.yml` - `checkDependencyLicences` named in the `build` job's Gradle step, with its comment
- `docs/tooling.md` - CycloneDX row and the licence-check scope note; Prettier re-aligned the column widths of the main table, which is why the diff touches every row of it

## Decisions Made

- The CycloneDX row is wider than the former Purpose column, so Spotless widened the whole table. The re-alignment is formatter output, not content change.
- The scope note sits in the existing Notes section of the inventory rather than a new heading, because it is one bullet and the inventory already keeps cross-cutting notes there.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Plan commit ledger could not be written under the shared `.git` path**
- **Found during:** Task 1 (commit protocol step 0c)
- **Issue:** The sandbox refuses compound git commands and writes to the shared `.git/worktrees/...` directory from this worktree, so `gsd-plan-head-before-02-20` could not be persisted.
- **Fix:** `plan_head_before` is the HEAD recorded at spawn (`984d994`, equal to the orchestrator's expected base); `commits` is measured with `git rev-list --count 984d994..HEAD` (2).
- **Files modified:** none
- **Verification:** `git rev-list --count` prints 2, matching the two task commits.
- **Committed in:** n/a

---

**Total deviations:** 1 auto-fixed (1 blocking, tooling only)
**Impact on plan:** None on the deliverables.

## Issues Encountered

- A first combined `actionlint; zizmor` command line was refused by the sandbox (compound command), so the two ran separately. Both are clean.

## User Setup Required

None - no external service configuration required.

## Threat Flags

None. The plan adds ask rules and governed entries, and one argument to an existing CI step; no new endpoint, auth path, file access pattern or secret.

## Next Phase Readiness

- Plan 02-23 can record the outcome in ADR-0012: the licence gate is governed, CI names the task, and the detekt rule set is governed.
- Plans 02-21 and 02-22 are unaffected: neither lists a file under `config/detekt/` or the convention plugin.
- Phase 5 owns the npm half of CICD-09; Phase 6 can reuse `build/licences/bom.json` for image SBOMs.

## Self-Check: PASSED

- Modified files exist: `governance/governed-paths.txt`, `.claude/settings.json`, `.github/workflows/ci.yml`, `docs/tooling.md`.
- Commits exist on this branch: `ea7c632`, `9039a61`.
- Acceptance criteria, Task 1: licences entry count 1; licences ask rule count 1; CI step count 1; detekt entry count 1; detekt ask rule count 1; ask list length 57. Task 2: `outside the licence check` count 1; `dependency-licences.json` count 1; `CycloneDX` count 1.
- Plan verification: `checkClaudeAskRules`, `spotlessCheck`, `checkToolingInventory`, the governance shell tests (132 passed), actionlint, zizmor and `./gradlew build checkDependencyLicences -x integrationTest` all pass. The integration tests were excluded as in CI's `build` job and run in their own job.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
