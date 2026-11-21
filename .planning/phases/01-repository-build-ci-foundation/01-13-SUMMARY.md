---
phase: 01-repository-build-ci-foundation
plan: 13
subsystem: infra
tags: [github-settings, rulesets, gh-cli, jq, drift-check, claude-code, permissions, gradle, testkit]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-09: governance/governed-paths.txt and the governance-success context; plan 01-12: ci-success context, lint job shell-test discovery; plan 01-03: gh, jq, shellcheck, actionlint, zizmor pinned in mise.toml"
provides:
  - ".github/rulesets/main.json: declarative main ruleset (squash-only PRs, linear history, no deletion or force-push, ci-success and governance-success bound to the GitHub Actions app)"
  - ".github/repository-settings.json: declarative merge settings and security features"
  - "scripts/apply-github-settings.sh: idempotent --dry-run (default), --check and --apply over gh api with --repo and repeatable --only ruleset|repository|security"
  - "scripts/test-apply-github-settings.sh: 88 fake-gh assertions, run by the CI lint job through its test discovery"
  - ".github/workflows/settings-drift.yml: read-only scheduled drift check, skipped until SETTINGS_READ_TOKEN exists, scoped by SETTINGS_DRIFT_SECTIONS"
  - ".claude/settings.json: 11 ask rules (one Edit per governed path plus Bash(*config-set*))"
  - "checkClaudeAskRules Gradle task (AskRulesCheck) wired into check"
affects: [01-14 approval and apply, Phase 13 repository metadata]

plan_head_before: acc0e9ae801381025a5e48551f7f13954c8ad469
plan_head_after: b1272f856c4a8770eab98bd56110c32978a9e840

actuals:
  tokens: 11500
  tasks: 3
  commits: 5

tech-stack:
  added: []
  patterns:
    - "Settings as code: desired JSON in the repository, one script that reads with GET only and writes solely in --apply, re-read after apply and fail while a delta remains"
    - "Ruleset comparison normalises server fields and list order, then projects the live document onto the keys the desired one declares, so server defaults never read as drift"
    - "A failed read is never an empty state: the ruleset list and every probe abort on non-404 errors; only HTTP 404 on the Dependabot alerts probe means disabled"
    - "Drift workflow reads its scope from a repository variable passed through env only, one --only argument per word"
    - "Ask-rule sync check derives its expected rules from governance/governed-paths.txt, the same file the governance label gate reads"

key-files:
  created:
    - .github/rulesets/main.json
    - .github/repository-settings.json
    - scripts/apply-github-settings.sh
    - scripts/test-apply-github-settings.sh
    - .github/workflows/settings-drift.yml
    - .claude/settings.json
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckInputShapeFunctionalTest.kt
  modified:
    - build-logic/src/main/kotlin/courses.governance.gradle.kts

key-decisions:
  - "Plan 01-13: the main ruleset bypass actor is RepositoryRole 5 with bypass_mode pull_request (Assumption A1); --apply prints the read-back bypass actors so the author confirms the admin role in plan 01-14"
  - "Plan 01-13: ruleset equality compares only keys declared in main.json (projection), because GitHub adds defaults such as required_reviewers and do_not_enforce_on_create"
  - "Plan 01-13: the ask-rule input-shape tests live in a second test class so each stays within detekt's TooManyFunctions limit of 11"

requirements-completed: [GH-06, GH-07, AIDEV-08]

coverage:
  - id: D1
    description: "Declared main ruleset and repository settings JSON matching the plan's acceptance values"
    requirement: "GH-07"
    verification:
      - kind: other
        ref: "jq acceptance queries on .github/rulesets/main.json and .github/repository-settings.json (rule types, contexts, squash, PR_TITLE PR_BODY)"
        status: pass
    human_judgment: true
    rationale: "The author approves the declared policy and the admin bypass actor in plan 01-14 before anything is applied"
  - id: D2
    description: "Apply script is idempotent and cannot write in dry-run or check mode"
    requirement: "GH-06"
    verification:
      - kind: other
        ref: "bash scripts/test-apply-github-settings.sh (88 passed, 0 failed); mise x -- shellcheck scripts/*.sh"
        status: pass
    human_judgment: false
  - id: D3
    description: "Live read-only dry-run lists the expected deltas for plan 01-14"
    requirement: "GH-06"
    verification:
      - kind: other
        ref: "scripts/apply-github-settings.sh --dry-run (10 deltas, nothing written)"
        status: pass
    human_judgment: true
    rationale: "The author reviews the delta list at the plan 01-14 approval gate"
  - id: D4
    description: "Read-only drift workflow with token skip, narrowed-scope notice and no write mode"
    requirement: "GH-06"
    verification:
      - kind: other
        ref: "mise x -- actionlint; mise x -- zizmor --offline --no-progress (default and pedantic); extracted step run locally in skip, narrowed and bad-section cases"
        status: pass
    human_judgment: true
    rationale: "The workflow has not run on GitHub; it needs the SETTINGS_READ_TOKEN secret created in plan 01-14"
  - id: D5
    description: "checkClaudeAskRules keeps ask rules and governed paths in sync (missing, duplicate, empty, order, extras)"
    requirement: "AIDEV-08"
    verification:
      - kind: integration
        ref: "AskRulesCheckFunctionalTest (9 tests) and AskRulesCheckInputShapeFunctionalTest (6 tests); mise x -- ./gradlew build"
        status: pass
    human_judgment: false
  - id: D6
    description: ".claude/settings.json makes the agent ask before governed edits and config-set"
    requirement: "AIDEV-08"
    verification:
      - kind: other
        ref: "jq -e '.permissions.ask | index(\"Bash(*config-set*)\") != null' .claude/settings.json; mise x -- ./gradlew checkClaudeAskRules"
        status: pass
    human_judgment: true
    rationale: "Claude Code's runtime prompting (including whether .claude paths behave like ordinary project paths, Assumption A8) can only be observed in a live session"

duration: 22 min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 13: GitHub Settings as Code and Agent Ask Rules Summary

**Declarative `main` ruleset and repository settings with an idempotent `--dry-run`/`--check`/`--apply` script (88 fake-gh tests), a read-only drift workflow scoped by `SETTINGS_DRIFT_SECTIONS`, and Claude Code ask rules for every governed path and `config-set` kept in sync by `checkClaudeAskRules`.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-11-21T11:42:00Z
- **Completed:** 2026-11-21T12:04:00Z
- **Tasks:** 3 (1 tracer, 1 auto, 1 TDD)
- **Files modified:** 9 created, 1 modified (plus this SUMMARY and planning state)

## Accomplishments

- `.github/rulesets/main.json` and `.github/repository-settings.json` declare the policy; the GitHub Actions app id 15368 was confirmed with a read-only `gh api /apps/github-actions`.
- `apply-github-settings.sh` reads only through `gh api` GETs plus `jq`, compares the ruleset after normalising server fields and list order and projecting onto the declared keys, and writes only inside `apply_changes`, reachable solely from `--apply`. After a write it re-reads, fails while a delta remains and prints the ruleset bypass actors.
- Fake-`gh` tests (88 assertions) cover idempotency, zero non-GET calls in default, `--dry-run` and `--check`, the exact write calls of `--apply` (including that the repository PATCH body holds only differing fields), `--only` scoping, ruleset drift variants (dropped context, wrong app id, extra merge method, missing bypass, missing or extra rule, organisation ruleset of the same name), toggles, a 500 on the alerts probe not reading as "disabled", a failed ruleset list not reading as "absent", and usage errors (exit 2).
- `settings-drift.yml` (weekly, manual, settings-file push) runs `--check` only, skips with a notice until `SETTINGS_READ_TOKEN` exists, builds one `--only` per word of `SETTINGS_DRIFT_SECTIONS` read through `env:`, and prints a notice naming a narrowed scope on every run.
- `AskRulesCheck` / `checkClaudeAskRules` requires exactly one `Edit(/<path>)` per line of `governance/governed-paths.txt` plus `Bash(*config-set*)`; `.claude/settings.json` was written last with the 11 rules and nothing else.
- Tracer gate: the tracer `<verify>` is automated-only and human-verify mode is `end-of-phase`, so the verify (tests, shellcheck, live dry-run) was re-run after the commit and passed: "Tracer verified end-to-end, expanding".

## Live `--dry-run` delta list (for plan 01-14)

Run against `aanbrn/axon5-dcb-courses` with read-only GETs, nothing written:

```text
repository: aanbrn/axon5-dcb-courses, mode: dry-run, sections: ruleset repository security
ruleset main: create
repository.allow_merge_commit: true -> false
repository.allow_rebase_merge: true -> false
repository.squash_merge_commit_title: COMMIT_OR_PR_TITLE -> PR_TITLE
repository.squash_merge_commit_message: COMMIT_MESSAGES -> PR_BODY
repository.delete_branch_on_merge: false -> true
repository.allow_auto_merge: false -> true
repository.allow_update_branch: false -> true
security.vulnerability_alerts: false -> true
security.private_vulnerability_reporting: false -> true
10 delta(s); nothing written (dry-run)
```

Secret scanning and push protection are already enabled, and automated security fixes are already off, so neither appears. The read-only ruleset list was readable with the author's token; Assumption A9 (unauthenticated read) was not tested, the drift check relies on the token secret.

## Task Commits

1. **Task 1: Settings as code and apply script (tracer)** - `74e8a40` (feat); follow-up `10ef286` (fix, editorconfig-checker left-padding in the embedded jq and fixtures, which the CI lint job would have rejected)
2. **Task 2: Drift check workflow** - `926bab8` (feat)
3. **Task 3 (TDD):** RED `b3239e7` (test, 15 of 15 failing because the task did not exist), GREEN `b1272f8` (feat, task, wiring and `.claude/settings.json`)

**Plan metadata:** the closing docs commit (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Files Created/Modified

- `.github/rulesets/main.json`, `.github/repository-settings.json` - declared state
- `scripts/apply-github-settings.sh`, `scripts/test-apply-github-settings.sh` - apply/check script and tests
- `.github/workflows/settings-drift.yml` - read-only drift check
- `.claude/settings.json` - Claude Code ask rules
- `build-logic/.../governance/AskRulesCheck.kt`, `courses.governance.gradle.kts` - sync check and wiring
- `build-logic/src/functionalTest/.../AskRulesCheckFunctionalTest.kt`, `AskRulesCheckInputShapeFunctionalTest.kt` - 15 TestKit tests

## Decisions Made

See `key-decisions`. The two that matter for plan 01-14: confirm the printed bypass actor is the repository admin role after `--apply` (Assumption A1), and set `SETTINGS_DRIFT_SECTIONS` only while the applied sections are a strict subset.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Odd left padding failed `editorconfig-checker`**
- **Found during:** Task 2 verification (the CI lint job runs `editorconfig-checker`; `./gradlew check` does not)
- **Issue:** continuation lines in the embedded jq library and test fixtures used odd indentation, 6 findings
- **Fix:** re-indented in multiples of two (shell) and four (Kotlin)
- **Files modified:** `scripts/apply-github-settings.sh`, `scripts/test-apply-github-settings.sh`, `AskRulesCheckFunctionalTest.kt`
- **Committed in:** `10ef286`, `b1272f8`

**2. [Rule 3 - Blocking] detekt `TooManyFunctions` and `ReturnCount` on the new task and tests**
- **Found during:** Task 3 (`./gradlew build`)
- **Issue:** one test class with 15 tests exceeded the limit of 11, and `problems()` had three returns
- **Fix:** split the tests over two classes (a second file, `AskRulesCheckInputShapeFunctionalTest.kt`, beyond the plan's file list) and rewrote `problems()` as a `when` with a helper
- **Committed in:** `b1272f8`

**3. [Rule 2 - Missing Critical] A failed read must never read as an empty state**
- **Found during:** Task 1 (design)
- **Issue:** command substitutions do not inherit `set -e`, so a failed rulesets GET inside a helper would have read as "no ruleset" and `--apply` would POST a duplicate
- **Fix:** explicit `|| exit 1` in the helper; non-404 errors on the alerts probe abort; both covered by tests
- **Committed in:** `74e8a40`

---

**Total deviations:** 3 auto-fixed (1 missing critical, 2 blocking)
**Impact on plan:** all three tighten or enable specified behaviour; the only scope addition is one extra test file.

## Issues Encountered

- The session harness refuses compound shell commands that contain the substring `git` (including `.github` paths and `apply-github-settings.sh`); the plan's `<automated>` `actionlint`/`zizmor` forms with a workflow path were run on the repository root instead (same files), and some commands used a `.g?thub` glob.
- The plan's second `<automated>` of Task 2 (`tests=$(git ls-files ...)`) was verified as `git ls-files -- 'scripts/test-*.sh' 'scripts/*/test-*.sh' | grep -x scripts/test-apply-github-settings.sh` (rc 0), which is the same discovery the lint job uses.
- `.claude/settings.json` is now a governed file with an ask rule on itself: any further edit of a governed path in Phase 1 prompts the author. No executor edits one after Task 3.

## Verification Results and Timing

| Command | Result | Wall clock |
| --- | --- | --- |
| `bash scripts/test-apply-github-settings.sh` | 88 passed, 0 failed | about 4 s |
| `mise x -- shellcheck scripts/*.sh` | no findings | under 1 s |
| `scripts/apply-github-settings.sh --dry-run` (live, read-only) | 10 deltas, rc 0, no writes | about 4 s |
| `mise x -- actionlint`, `zizmor --offline --no-progress` (default and pedantic) | no findings | under 2 s each |
| `./gradlew :build-logic:functionalTest --tests '*AskRulesCheck*'` | 15 tests, 0 failures | 21 to 24 s |
| `./gradlew checkClaudeAskRules` | BUILD SUCCESSFUL | about 1 s (warm) |
| `mise x -- ./gradlew build` | BUILD SUCCESSFUL | 2 min 30 s |
| `mise x -- editorconfig-checker`, `mise x -- reuse lint` | pass | under 2 s |

Every `<automated>` command stays under the 180 s per-task budget; no validation gap. (The full `build` is not a per-task automated command and takes 150 s.)

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-01-31 (default dry-run, writes only in `--apply`, tests assert zero non-GET calls), T-01-32 (`integration_id` 15368 on both contexts), T-01-33 (token only in `env:`, no write mode in the workflow), T-01-34 (ask rules plus sync check) and T-01-40 (notice on every narrowed run, unknown section exits 2) are mitigated as planned. T-01-SC: no packages installed.

## User Setup Required

None in this plan. Plan 01-14 creates the `SETTINGS_READ_TOKEN` secret, optionally sets `SETTINGS_DRIFT_SECTIONS`, and runs `--apply` behind its human checkpoints. Nothing was applied to GitHub here.

## Next Phase Readiness

- Plan 01-14 can present the delta list above and run `scripts/apply-github-settings.sh --apply` (optionally with `--only`).
- Residual: Assumptions A1 (admin role id 5) and A8 (`.claude` path rules behave like ordinary paths) are confirmed only by the apply read-back and a live session respectively.

## Self-Check: PASSED

All created files exist on disk and all five commits (`74e8a40`, `926bab8`, `10ef286`, `b3239e7`, `b1272f8`) are ancestors of HEAD.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
