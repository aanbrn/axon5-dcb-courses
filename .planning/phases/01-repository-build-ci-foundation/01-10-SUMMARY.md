---
phase: 01-repository-build-ci-foundation
plan: 10
subsystem: infra
tags: [github-labels, ghaction-github-labeler, pr-template, gradle, build-logic, testkit, mise, governance]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "courses.governance plugin and ConfigPolicyCheck task shape (01-07), governance.yml hardening conventions and the governance-approved label name (01-09), mise.toml and the docs/tooling.md table contract (01-03, 01-05)"
provides:
  - ".github/labels.yml: the D-18 label set (42 labels, community labels deferred)"
  - ".github/workflows/labels.yml: dry-run validation on PRs, exact sync from main and manual dispatch, issues write only in the sync job"
  - ".github/PULL_REQUEST_TEMPLATE.md mirroring the /gsd-ship body plus a checklist"
  - "checkPrTemplate (PrTemplateCheck): template headings must equal the five core sections plus the enabled ship.pr_body_sections"
  - "checkToolingInventory (ToolingTableCheck): mise.toml [tools] keys and mise-backed docs/tooling.md rows must agree in both directions"
affects: [01-11, 01-13, 01-14, phase-12-conventional-commit-title-rule, phase-13-community-labels]

actuals:
  tokens: 8400
  tasks: 3
  commits: 5
plan_head_before: 3a7f2f95b65914314f2c11dcf2844ca028b658eb
plan_head_after: f271e50665e97f99f7dffb0ac4e80217ac19e683

tech-stack:
  added: [crazy-max/ghaction-github-labeler v6.0.0 (SHA-pinned)]
  patterns:
    - "Zero-dependency line readers for the single TOML table and the Markdown table a check needs (no tomlj)"
    - "Governance check = @CacheableTask in build-logic, registered in courses.governance, wired into check, covered by a TestKit class"

key-files:
  created:
    - .github/labels.yml
    - .github/workflows/labels.yml
    - .github/PULL_REQUEST_TEMPLATE.md
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheckFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheckFunctionalTest.kt
  modified:
    - build-logic/src/main/kotlin/courses.governance.gradle.kts

key-decisions:
  - "Label descriptions for phase:1..13 carry the real roadmap phase names, not placeholders"
  - "checkPrTemplate ignores headings inside HTML comments and fenced code blocks, so the per-section hints cannot create or hide a heading"
  - "checkToolingInventory treats a mise-backed row without a backticked key as its own problem line instead of silently skipping it"
  - "The Version source cell is compared after stripping backticks, because docs/tooling.md writes it as `mise`"

patterns-established:
  - "Pedantic zizmor persona is clean too: every non-default permission carries an explanatory comment"

requirements-completed: [GH-08, GH-11, TOOL-01, TOOL-02]

coverage:
  - id: D1
    description: "labels.yml defines all D-18 groups (9 type, 8 area, phase 1-13, deps plus 6 ecosystems, autorelease pending and tagged, upstream, governance-approved, security) with per-group colours, quoted names, and no community labels"
    requirement: GH-11
    verification:
      - kind: other
        ref: "grep acceptance counts: 17 type/area, 13 phase, 7 deps, 1 governance-approved, 1 autorelease: pending, 0 community labels; spotlessCheck (Prettier parses the file)"
        status: pass
    human_judgment: false
  - id: D2
    description: "labels workflow: dry-run validate on pull requests, exact sync on main pushes and dispatch, issues write only in sync, serialised without cancelling, SHA-pinned actions, persist-credentials false"
    requirement: GH-11
    verification:
      - kind: other
        ref: "mise x -- actionlint .github/workflows/labels.yml; mise x -- zizmor --offline --no-progress (also --persona=pedantic) report no findings"
        status: pass
    human_judgment: false
  - id: D3
    description: "The sync actually creates, updates and deletes labels on GitHub, and is idempotent for an unchanged file"
    verification: []
    human_judgment: true
    rationale: "Backstop: needs the workflow on main and a live repository; plan 01-14 applies it and the first run is the proof. Not run here by design (local-only plan)."
  - id: D4
    description: "PULL_REQUEST_TEMPLATE.md has the five core headings, the four enabled configured headings and a checklist, in order"
    requirement: GH-08
    verification:
      - kind: other
        ref: "grep -c '^## ' prints 10; first heading is ## Summary; mise x -- ./gradlew checkPrTemplate green"
        status: pass
    human_judgment: false
  - id: D5
    description: "checkPrTemplate fails on a missing heading, wrong order, or a missing enabled configured section; disabled sections are not required; wired into check; cacheable"
    requirement: GH-08
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../governance/PrTemplateCheckFunctionalTest.kt (10 tests)"
        status: pass
    human_judgment: false
  - id: D6
    description: "checkToolingInventory fails in both directions when mise.toml and docs/tooling.md disagree (including spelling drift pipx:reuse vs reuse), ignores order and non-mise rows, tolerates the inline-table npm:renovate entry and Prettier padding; the real repository passes"
    requirement: TOOL-01
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../governance/ToolingTableCheckFunctionalTest.kt (10 tests)"
        status: pass
      - kind: other
        ref: "mise x -- ./gradlew checkToolingInventory green on the real mise.toml and docs/tooling.md"
        status: pass
    human_judgment: false
  - id: D7
    description: "Both checks run in check; no TOML library was added to build-logic"
    requirement: TOOL-02
    verification:
      - kind: other
        ref: "mise x -- ./gradlew check --dry-run lists all five governance checks; mise x -- ./gradlew build green; grep -c tomlj build-logic/build.gradle.kts prints 0"
        status: pass
    human_judgment: false

duration: 45min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 10: Labels, PR Template and Metadata Drift Checks Summary

**D-18 labels as code with a least-privilege SHA-pinned validate and sync workflow, a /gsd-ship-shaped PR template, and two cacheable build checks that stop that template and `docs/tooling.md` from drifting from `.planning/config.json` and `mise.toml`**

## Performance

- **Duration:** about 45 min
- **Started:** 2026-11-21T10:49Z (approx.)
- **Completed:** 2026-11-21T11:35Z (approx.)
- **Tasks:** 3 (one tracer, two TDD)
- **Files modified:** 8 (7 created, 1 modified)

## Accomplishments

- Tracer proven end to end: `labels.yml` (42 labels) and the `Labels` workflow passed actionlint, zizmor (default and pedantic persona), `spotlessCheck` and every grep criterion; logged "Tracer verified end-to-end - expanding".
- The action's input names were confirmed against `action.yml` at the pinned commit `548a7c36...` (Assumption A10): `yaml-file`, `skip-delete`, `dry-run`; the tag `v6.0.0` still resolves to that commit.
- `checkPrTemplate` requires the template's leading `##` headings to start with Summary, Changes, Requirements Addressed, Verification, Key Decisions followed by every enabled `ship.pr_body_sections` heading in config order; failures list missing headings and expected versus actual.
- `checkToolingInventory` reports `missing in docs/tooling.md: <key>` and `missing in mise.toml: <key>` sorted, and passes on the real repository including the inline-table `"npm:renovate"` entry (the follow-up from 01-03).
- Both TDD tasks went RED (10 of 10 and 12 of 12 failing: tasks did not exist) before GREEN.

## Task Commits

1. **Task 1: Labels as code (tracer)** - `e51aa5b` (feat)
2. **Task 2 (TDD):**
   - RED: `26ab2e2` (test) - 10 failing `PrTemplateCheckFunctionalTest` scenarios
   - GREEN: `5b66221` (feat) - template, `PrTemplateCheck`, `checkPrTemplate` wiring
3. **Task 3 (TDD):**
   - RED: `60ce02a` (test) - 12 failing `ToolingTableCheckFunctionalTest` scenarios
   - GREEN: `f271e50` (feat) - `ToolingTableCheck`, `checkToolingInventory` wiring, tests merged to 10

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

## Files Created/Modified

- `.github/labels.yml` - 42 labels in the D-18 groups; every name quoted, per-group colours
- `.github/workflows/labels.yml` - jobs `validate` (dry run, `issues: read`) and `sync` (`issues: write`); `permissions: {}` at top; concurrency group without cancellation
- `.github/PULL_REQUEST_TEMPLATE.md` - nine body sections with HTML-comment hints, then `## Checklist`
- `.../governance/PrTemplateCheck.kt`, `ToolingTableCheck.kt` - the two `@CacheableTask` classes
- `.../governance/PrTemplateCheckFunctionalTest.kt`, `ToolingTableCheckFunctionalTest.kt` - 10 + 10 TestKit tests
- `build-logic/src/main/kotlin/courses.governance.gradle.kts` - registers `checkPrTemplate`, `checkToolingInventory`; `check` now depends on five governance checks

## Decisions Made

See `key-decisions`. The label descriptions use the real roadmap phase names. Both checks report names only (headings, tool keys), never file contents beyond that.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] detekt TooManyFunctions in the new tooling test class**
- **Found during:** Task 3 (first `check` after GREEN)
- **Issue:** the 12-test class plus fixture helper exceeded detekt's 11-function limit.
- **Fix:** merged the column-padding test into the matching-pass test and the empty-table pass and fail cases into one test (12 to 10 tests); no rule was disabled. All plan behaviours are still asserted.
- **Files modified:** `ToolingTableCheckFunctionalTest.kt`
- **Verification:** `mise x -- ./gradlew check` green.
- **Committed in:** `f271e50`

**2. [Rule 2 - Missing Critical] Explanatory comment on `issues: read`**
- **Found during:** Task 1 (zizmor pedantic persona, run as an extra check)
- **Issue:** the pedantic persona flags permissions without an explanatory comment; the default persona suppressed it.
- **Fix:** added a one-line comment next to the permission, as `governance.yml` does for `pull-requests: read`.
- **Files modified:** `.github/workflows/labels.yml`
- **Verification:** zizmor with `--persona=pedantic` reports no findings.
- **Committed in:** `e51aa5b`

---

**Total deviations:** 2 auto-fixed (1 bug, 1 missing critical)
**Impact on plan:** none on scope or behaviour. Test count changed from the 12 first committed in the RED commit to 10.

## Issues Encountered

- The session harness refuses compound commands that name git or contain a heredoc interpreter call, and refuses to write into the shared `.git` directory. Equivalent substitutions: `git status`, `git add`, `git commit` and `git rev-list` ran as separate plain commands; the root-pin script ran before every Edit/Write batch and commit.
- The plan-commit ledger file (`gsd-plan-head-before-01-10` under the git dir) could not be written (writes into the shared `.git/worktrees` path are blocked). `plan_head_before` is the HEAD recorded before the first commit, `3a7f2f9`; `commits: 5` is `git rev-list --count 3a7f2f9..HEAD`.
- One-off phase-description rewrite ran through a small script kept outside the repository (`/Users/afanasyev/.claude/jobs/1a564a96/tmp/fix_phases.py`), because a heredoc interpreter call is refused.

## Verification Results and Timing

Every `<automated>` command, run as `mise x -- ...` with warm Gradle and TestKit caches:

| Command | Result | Wall clock |
| --- | --- | --- |
| `actionlint .github/workflows/labels.yml` | pass | under 1 s |
| `zizmor --offline --no-progress .github/workflows/labels.yml` | pass, no findings | under 2 s |
| `./gradlew spotlessCheck` | pass | about 1 s |
| `./gradlew :build-logic:functionalTest --tests '*PrTemplateCheckFunctionalTest*'` (10 tests) | pass | 15-18 s |
| `./gradlew checkPrTemplate` | pass | under 1 s |
| `./gradlew :build-logic:functionalTest --tests '*ToolingTableCheckFunctionalTest*'` (10 tests) | pass | 16 s |
| `./gradlew checkToolingInventory` | pass | under 1 s |
| `./gradlew check` (full) | pass | 2 min 11 s |

The filtered `functionalTest` runs take under 20 s and the full `check` takes 131 s, so every command stays within the 180 s per-task feedback budget. No validation gap.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-01-21 is mitigated as planned (`issues: write` only in `sync`, which cannot run on pull requests; the validate job is a dry run with `issues: read`; the action is pinned by SHA). T-01-22 is accepted as planned: the first sync deletes GitHub's default labels and must be listed in the plan 01-14 approval summary. T-01-SC: no packages installed.

## User Setup Required

None in this plan. Nothing was created or synced on GitHub and nothing was pushed. Plan 01-14 creates the `governance-approved` label before the first PR (Pitfall 11) and applies the sync.

## Next Phase Readiness

- Plan 01-11 (Renovate) can rely on the labels `deps`, `deps:gradle`, `deps:github-actions`, `deps:mise`, `deps:jdk`, `deps:node`, `deps:npm` and `security`.
- Plan 01-14's approval summary must disclose that the first sync deletes labels not in `labels.yml`, including GitHub's defaults (T-01-22), and that the workflow's idempotency and the labeler's handling of the description-less case are backstop items proven only by the first live run.
- Plans that add or remove a mise tool must update `docs/tooling.md` in the same change, or `check` fails.

## Self-Check: PASSED

Created files exist (labels.yml, workflows/labels.yml, PULL_REQUEST_TEMPLATE.md, PrTemplateCheck.kt, ToolingTableCheck.kt, both TestKit classes); commits `e51aa5b`, `26ab2e2`, `5b66221`, `60ce02a`, `f271e50` are ancestors of HEAD; every acceptance criterion holds (17 type/area, 13 phase, 7 deps, 1 governance-approved, 1 autorelease: pending, 0 community labels, 1 `issues: write`, 10 template headings with `## Summary` first, `checkPrTemplate` and `checkToolingInventory` registered, 0 `tomlj`); `mise x -- ./gradlew build` green.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
