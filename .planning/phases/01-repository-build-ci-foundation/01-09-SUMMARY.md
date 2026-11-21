---
phase: 01-repository-build-ci-foundation
plan: 09
subsystem: infra
tags: [governance, github-actions, shell, jq, renovate, zizmor, actionlint, shellcheck]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-03: jq, gh, shellcheck, actionlint and zizmor pinned in mise.toml; plan 01-05: shfmt and the shell SPDX header applied by spotlessApply"
provides:
  - "governance/governed-paths.txt: the single list of 10 governed path globs"
  - "scripts/governance/check-pr.sh: label gate, narrow renovate[bot] version-only exemption and head-branch naming rule, driven only by environment variables"
  - "scripts/governance/test-check-pr.sh: 39 self-contained cases, including a real mise lock diff"
  - ".github/workflows/governance.yml: read-only pull_request workflow ending in the stable governance-success context"
affects: [01-10, 01-13, 01-14]

plan_head_before: ac434899baf08d3c0be0f8f01c8857f27d59909e
plan_head_after: 8f1ce18d3c1b4feb790806321d4d3e906c3fd829

actuals:
  tokens: 7000
  tasks: 2
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Policy logic lives in a tested shell script fed only through env: variables; the workflow contains no expression inside any run body"
    - "Patch fixtures write the one-space context marker as ~ and patch_file converts it, so fixture heredocs pass editorconfig-checker"
    - "Version-only means: removed and added lines are equal as sorted multisets after one normalize function collapses SHAs, digests and versions"

key-files:
  created:
    - governance/governed-paths.txt
    - scripts/governance/check-pr.sh
    - scripts/governance/test-check-pr.sh
    - .github/workflows/governance.yml
  modified: []

key-decisions:
  - "A removed or added line is recognised by its sign only after the first hunk header, not by excluding lines that start with three dashes: the files API patch has no file header, and the plan's three-dash exclusion would let a removed line beginning with two dashes hide next to a version bump (a test covers it)"
  - "An exemption needs status modified, a non-null non-empty patch, no previous filename and at least one changed line; an empty or missing patch is never exempt"
  - "mise.lock normalisation also collapses GitHub release asset ids (releases/assets/<digits>), because a real mise lock diff changes them with every version and the plan's version, checksum and size rules alone leave that line different"
  - "Fork pull requests are detected by head repo differing from PR_BASE_REPO; an empty PR_BASE_REPO fails closed, an unset PR_HEAD_REPO is treated as a fork (naming rule skipped, label gate still applies)"
  - "Branch names are matched in the C locale, so look-alike Unicode characters never satisfy the [a-z0-9-] class"

patterns-established:
  - "Governance checks that need PR context live in governance.yml and end in one governance-success job; the ruleset requires that context (plans 01-13 and 01-14)"

requirements-completed: [AIDEV-06, GH-07]

coverage:
  - id: D1
    description: "A PR that touches a governed file (or renames out of one) fails unless it carries the governance-approved label; empty, missing, malformed, spaced and non-ASCII inputs behave as specified"
    requirement: AIDEV-06
    verification:
      - kind: unit
        ref: "bash scripts/governance/test-check-pr.sh (39 passed, 0 failed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Only renovate[bot] with a version-only diff of every touched governed file is exempt; new lines, non-version tokens, human authors, added or patch-less files and hidden dash lines stay gated"
    requirement: AIDEV-06
    verification:
      - kind: unit
        ref: "bash scripts/governance/test-check-pr.sh (renovate cases, including the real mise lock diff)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Same-repository PRs must come from gsd/phase-<N>[.<M>]-<slug>, renovate/* or release-please--* branches; fork PRs skip the rule; an empty head ref fails closed"
    requirement: GH-07
    verification:
      - kind: unit
        ref: "bash scripts/governance/test-check-pr.sh (head branch naming cases)"
        status: pass
    human_judgment: false
  - id: D4
    description: "governance.yml triggers only on pull_request with a read-only token, passes every PR value through env, pins actions by SHA and ends in governance-success"
    requirement: AIDEV-06
    verification:
      - kind: other
        ref: "mise x -- actionlint; mise x -- zizmor --offline --no-progress . (no findings, also none under --persona=pedantic); acceptance greps"
        status: pass
    human_judgment: false
  - id: D5
    description: "Edge AIDEV-06 concurrency (adding the label re-runs the check and supersedes the earlier run) and edge GH-07 encoding (byte-wise branch comparison)"
    requirement: AIDEV-06
    verification: []
    human_judgment: true
    rationale: "Planned as backstop. The labeled trigger and the per-PR concurrency group only prove themselves on GitHub after the first PR (plan 01-14); the byte-wise comparison is covered by the look-alike Unicode test, but cannot be asserted for every Unicode class."

duration: 7min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 09: Pull Request Governance Gate Summary

**A tested shell gate and a read-only `governance.yml` workflow: a PR touching a governed file fails `governance-success` without `governance-approved`, except `renovate[bot]` bumps whose every governed diff is versions, SHAs or digests only, and same-repository PRs must come from `gsd/phase-*`, `renovate/*` or `release-please--*` branches**

## Performance

- **Duration:** 7 min
- **Started:** 2026-11-21T10:17:34Z
- **Completed:** 2026-11-21T10:24:26Z
- **Tasks:** 2 (Task 1 tracer, Task 2 TDD)
- **Files modified:** 4 created, none modified

## Accomplishments

- Label gate end to end: `check-pr.sh` reads the files-API JSON with `jq` only (NUL-separated, so spaces and non-ASCII names are exact), matches paths and rename sources against `governance/governed-paths.txt` with a literal-safe `**` matcher, and fails closed on a missing, non-array or malformed list.
- `governance.yml` runs only on `pull_request` (opened, synchronize, reopened, labeled, unlabeled, edited), `permissions: {}` at workflow level, `contents: read` plus `pull-requests: read` on the one job, SHA-pinned checkout with `persist-credentials: false`, every PR value through `env:`, per-PR concurrency, and a `governance-success` aggregator with `if: always()`.
- Renovate exemption (D-16): one `normalize` function collapses commit SHAs, `sha256:` and bare 64-hex digests and version tokens (and, for `mise.lock`, sizes and release asset ids); a file is version-only when sorted removed and added lines are equal. Verified against a real `mise lock jq` diff produced in a scratch copy (jq 1.8.2 to 1.8.1, four platforms).
- Head-branch naming (GH-07) with the three allowed families, fork pull requests exempt, empty ref failing closed.
- 39 test cases, all green; the suite runs in about 1 s.

## Task Commits

1. **Task 1: End-to-end label gate (tracer)** - `17d9a26` (feat). The tracer `<verify>` is automated-only and was re-run after the commit (tests, shellcheck, actionlint, zizmor, trigger grep); it passed, so expansion to Task 2 proceeded.
2. **Task 2 (TDD):**
   - RED: `b1057ce` (test) - 26 new cases, 15 failing against the Task 1 script
   - GREEN: `cea2a7b` (feat) - exemption and naming, all 39 pass
   - Follow-up: `8f1ce18` (refactor) - fixture context-line marker so `editorconfig-checker` passes

**Plan metadata:** the closing docs commit (SUMMARY, STATE, ROADMAP, REQUIREMENTS).

## Files Created/Modified

- `governance/governed-paths.txt` - ten governed globs, one per line
- `scripts/governance/check-pr.sh` - label gate, exemption and naming; env-only inputs, no `eval`
- `scripts/governance/test-check-pr.sh` - 39 cases, clean environment per case via `env -i`
- `.github/workflows/governance.yml` - PR workflow ending in `governance-success`

## Decisions Made

See `key-decisions` above. The two that matter for later plans: the plan 01-13 ask rules must read the same `governance/governed-paths.txt`, and the ruleset must require the `governance-success` context (the job's `name:` equals its key).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Patch lines are classified by position, not by a `---` exclusion**
- **Found during:** Task 2 (designing the exemption)
- **Issue:** the plan says removed lines are those starting with `-` but not `---`. A files-API `patch` has no file header, so a removed line whose content begins with two dashes appears as `---...` and would be skipped, letting it hide next to a version bump.
- **Fix:** lines count after the first `@@` hunk header; every `-` or `+` line counts. A test (`renovate hiding a removed line that starts with dashes fails`) covers it.
- **Files modified:** `scripts/governance/check-pr.sh`, `scripts/governance/test-check-pr.sh`
- **Committed in:** `cea2a7b`

**2. [Rule 1 - Bug] Release asset ids in `mise.lock` broke the real-lock case**
- **Found during:** Task 2 (capturing the real `mise lock` diff)
- **Issue:** `url_api` lines carry `releases/assets/<digits>`, which changes with every release and is neither a version, a SHA nor a size, so a genuine lock bump was not version-only under the plan's rules.
- **Fix:** for `mise.lock` only, `releases/assets/<digits>` collapses to `<ID>` (alongside `size = <N>`). A mutation run without that rule makes the real-lock case fail, so the test exercises it.
- **Files modified:** `scripts/governance/check-pr.sh`
- **Committed in:** `cea2a7b`

**3. [Rule 2 - Missing Critical] Empty patch and previous filename are never exempt**
- **Found during:** Task 2
- **Issue:** equal empty removed and added sets would count as version-only for a mode change, a pure rename or an empty patch.
- **Fix:** the exemption requires at least one changed line and an empty `previous_filename`.
- **Committed in:** `cea2a7b`

**4. [Rule 3 - Blocking] Fixture heredocs failed `editorconfig-checker`**
- **Found during:** plan-level verification (`./gradlew check` does not run it; a separate run did)
- **Issue:** patch context lines start with one space (odd left padding) inside test heredocs, 20 findings.
- **Fix:** fixtures write the context marker as `~`; `patch_file` converts it to a space.
- **Committed in:** `8f1ce18`

---

**Total deviations:** 4 auto-fixed (1 bug, 2 missing critical, 1 blocking)
**Impact on plan:** all four tighten or enable the specified behaviour; no scope creep.

## Issues Encountered

- The session harness refuses compound shell commands that contain the substring `git` (including the `.github` path and `gitleaks`); verification commands were split into plain commands, and `actionlint` and `zizmor` were run on the repository root instead of the single workflow path (they discover `.github/workflows` and report on the same file). The plan's third `<automated>` trigger check was verified by `grep -n pull_request_target` (no match) instead of the compound form.
- The plan's `<automated>` `mise x -- zizmor ... .github/workflows/governance.yml` form was therefore replaced by `mise x -- zizmor --offline --no-progress .`; the default persona reports nothing and the pedantic persona initially flagged one undocumented permission, fixed with an inline comment.
- macOS ships bash 3.2; the scripts avoid associative arrays and empty-array expansion so `bash scripts/governance/test-check-pr.sh` works there and on the Ubuntu runner.
- Known limitation: a Temurin-style URL change such as `25.0.5.1%2B2` to a new build number is not collapsed by the version rule (the `%2B<build>` suffix differs), so such a Renovate bump would still need the label. This errs on the safe side; extend `normalize` if Renovate starts bumping `.java-version` entries in `mise.lock`.

## Verification Results and Timing

| Command | Result | Wall clock |
| --- | --- | --- |
| `bash scripts/governance/test-check-pr.sh` | 39 passed, 0 failed | about 1.1 s |
| `mise x -- shellcheck scripts/governance/*.sh` | no findings | under 1 s |
| `mise x -- actionlint` | no findings | under 1 s |
| `mise x -- zizmor --offline --no-progress .` (also `--persona=pedantic`) | no findings | under 2 s |
| `mise x -- ./gradlew spotlessApply` / `check` (warm) | BUILD SUCCESSFUL | under 1 s each (cache hits) |
| `mise x -- editorconfig-checker`, `mise x -- reuse lint` | pass | under 1 s |
| Real lock fixture: `mise lock jq --platform linux-x64,linux-arm64,macos-arm64,macos-x64` in a scratch copy | pass | 8.8 s (one-off) |

Nothing exceeds 30 s; no validation gap against the 180 s budget.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-01-17 (only `pull_request`, read-only token, grep gate on the trigger), T-01-18 (every PR value via `env:`, no expression in `run:`, `jq` only, no `eval`; zizmor clean), T-01-19 (exact `renovate[bot]` plus version-only for every governed file) and T-01-20 (`.github/**` and `scripts/governance/**` are governed) are mitigated as planned. T-01-SC: no packages installed.

## User Setup Required

None in this plan. The `governance-approved` label must exist before the first PR (created in plan 01-14, defined in `.github/labels.yml` in plan 01-10); nothing was created on GitHub here.

## Next Phase Readiness

- Plan 01-13 can derive the Claude Code ask rules from `governance/governed-paths.txt` and require `governance-success` in the ruleset (together with `ci-success`).
- Plan 01-10 defines the `governance-approved` label in `.github/labels.yml`; plan 01-14 creates it before the first PR (Pitfall 11).
- The first real PR will prove the `labeled`/`unlabeled` re-run and concurrency behaviour (decision-coverage item D5, backstop).

## Self-Check: PASSED

Created files exist (`governance/governed-paths.txt`, `scripts/governance/check-pr.sh`, `scripts/governance/test-check-pr.sh`, `.github/workflows/governance.yml`); commits `17d9a26`, `b1057ce`, `cea2a7b`, `8f1ce18` are ancestors of HEAD; acceptance criteria hold (10 governed lines, one `governance-success:`, every `uses:` pinned by SHA, `persist-credentials: false` present, `renovate[bot]` and `gsd/phase-` present in the script, zero `eval `); plan-level verification passes (39 cases, shellcheck, actionlint, zizmor, `spotlessCheck`, `check`).

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
