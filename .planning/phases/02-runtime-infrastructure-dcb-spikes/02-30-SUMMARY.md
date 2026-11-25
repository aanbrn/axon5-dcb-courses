---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 30
subsystem: infra
tags: [github-actions, ci, gap-closure, G-02-1, compose-smoke, rate-limit, maven-central-mirror]

# Dependency graph
requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: plans 02-24 (429 retry wrapper), 02-25, 02-26 and 02-31 (CI-only Maven Central mirror), pushed to PR #21
provides:
  - confirmation that the compose-smoke failure at 729dcff was a Gradle Plugin Portal 429 flake (same-SHA re-run passed)
  - proof that PR #21 at 07f309d is fully green on GitHub runners (every CI job, ci-success, governance-success, CodeQL)
  - proof that the CI-only Maven Central mirror step ran in each of the five Gradle jobs and that no 429 reached the retry wrapper
affects: [phase 02 verification, UAT test 1 re-check]

# Actuals (#2632)
actuals:
  tokens: 5000
  tasks: 3
  commits: 0
plan_head_before: 07f309de9428a7c657877aea785020e6a10bf458
plan_head_after: 07f309de9428a7c657877aea785020e6a10bf458

# Tech tracking
tech-stack:
  added: []
  patterns: []

key-files:
  created:
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-30-SUMMARY.md
  modified: []

key-decisions:
  - "Author answered push-now (2026-11-25); the orchestrator pushed 729dcff..07f309d as a plain fast-forward after running every lint-job step locally"

patterns-established: []

requirements-completed: [INFRA-03, INFRA-04, TOOL-03]

coverage:
  - id: D1
    description: "The same-SHA re-run of the failed compose-smoke job (run 45532702352, attempt 2 at 729dcff) succeeded, which confirms the Plugin Portal 429 flake diagnosis"
    requirement: INFRA-03
    verification:
      - kind: other
        ref: "gh api repos/aanbrn/axon5-dcb-courses/actions/runs/45532702352 (run_attempt 2, status completed, conclusion success); attempt-1 log of job 138931678592 shows 'Received status code 429 from server: Too Many Requests'"
        status: pass
    human_judgment: false
  - id: D2
    description: "At the pushed head 07f309d every job of the CI workflow succeeds, including Compose smoke test, and ci-success and governance-success are green"
    requirement: INFRA-04
    verification:
      - kind: other
        ref: "gh pr checks 21 --repo aanbrn/axon5-dcb-courses --required (exit 0: ci-success pass, governance-success pass)"
        status: pass
      - kind: other
        ref: "gh run view 45563826333 --json jobs: zero jobs with a conclusion other than success"
        status: pass
    human_judgment: false
  - id: D3
    description: "The CI-only Maven Central mirror step ran and succeeded in each of the five Gradle jobs, the five Gradle steps ran through scripts/ci/retry-on-rate-limit.sh, and no 429 or retry occurred"
    requirement: TOOL-03
    verification:
      - kind: other
        ref: "gh api repos/aanbrn/axon5-dcb-courses/actions/jobs/<id> step 'Use the Maven Central mirror (CI only)' = success in Build, Integration tests, Functional tests (build-logic), Compose smoke test, Doctor; grep -cF 'retry-on-rate-limit:' = 0 in all five job logs"
        status: pass
    human_judgment: false

# Metrics
duration: 25min
completed: 2026-11-25
status: complete
---

# Phase 02 Plan 30: Prove CI green on GitHub runners (G-02-1) Summary

**G-02-1 closed: PR #21 at head 07f309d is fully green on GitHub runners (all nine CI jobs, ci-success, governance-success, CodeQL), the earlier compose-smoke failure was a confirmed Plugin Portal 429 flake, and with the CI-only Maven Central mirror in place no 429 occurred, so the retry wrapper never had to fire.**

## Performance

- **Duration:** about 25 min for this continuation (Task 3 polling dominated); Tasks 1 and 2 ran in the earlier segment
- **Completed:** 2026-11-25
- **Tasks:** 3 (Task 1 auto, read-only; Task 2 blocking-human decision, answered `push-now`; Task 3 auto, read-only)
- **Files modified:** 0 repository files (the SUMMARY only)

## Accomplishments

- Classified the same-SHA re-run: flake confirmed (Task 1).
- The author consented to the push and the orchestrator pushed `729dcff..07f309d` (Task 2).
- Watched CI at the pushed head to completion and recorded every job's conclusion (Task 3).

## Task 1: same-SHA re-run of the failed compose-smoke job (read-only)

- Run https://github.com/aanbrn/axon5-dcb-courses/actions/runs/45532702352, head_sha `729dcff76c87da47fe091ec5456cc81e740d137d`, run_attempt 2, status completed, conclusion success (updated 2026-11-25T03:23:59Z).
- Attempt 2 jobs, all success: Compose smoke test (job 138953639483), Secret scan, Functional tests (build-logic), Doctor, Integration tests, Build, Qodana, Lint, Upstream records advisory, ci-success (job 138954867316).
- Attempt 1 failures: only Compose smoke test (job 138931678592) and ci-success (job 138935533158). The attempt-1 compose-smoke log contains `Error resolving plugin [id: 'courses.settings']` ... `Repository Gradle Central Plugin Repository is disabled due to earlier error below: > Received status code 429 from server: Too Many Requests`, BUILD FAILED in 27s (reading the log needs `gh api --allow-escape-sequences`).
- Classification: **flake confirmed** (same SHA, attempt 1 failed on a Plugin Portal 429, attempt 2 passed everything).

## Task 2: push consent (decision checkpoint)

- Author answered `push-now` (2026-11-25).
- Before pushing, the orchestrator ran every lint-job step locally in the main checkout (actionlint, zizmor, shellcheck, editorconfig-checker, reuse lint, line-ending renormalisation, renovate-config-validator --strict, docker compose config --quiet, all shell tests): all green.
- The orchestrator pushed `729dcff..07f309d` to origin `gsd/phase-2-runtime-infrastructure-dcb-spikes` as a plain fast-forward, never forced. Pushed SHA: `07f309de9428a7c657877aea785020e6a10bf458`. Attempt 2 of run 45532702352 had already concluded, so the push cancelled nothing (T-02-77).
- Precondition of Task 3 verified: `gh pr view 21 --json headRefOid --jq .headRefOid` prints `07f309de9428a7c657877aea785020e6a10bf458`.

## Task 3: CI and Governance at the pushed head 07f309d

Runs for the head SHA (`gh run list --commit 07f309d...`), all completed with success:

| Workflow | Run | Conclusion |
|----------|-----|------------|
| CI | https://github.com/aanbrn/axon5-dcb-courses/actions/runs/45563826333 | success |
| Governance | https://github.com/aanbrn/axon5-dcb-courses/actions/runs/45563826409 | success |
| CodeQL | https://github.com/aanbrn/axon5-dcb-courses/actions/runs/45563826264 | success |

CI run 45563826333 jobs (attempt 1, no re-run needed):

| Job | Job id | Conclusion |
|-----|--------|------------|
| Build | 139033662441 | success |
| Integration tests | 139033662589 | success |
| Functional tests (build-logic) | 139033662104 | success |
| Compose smoke test | 139033662593 | success |
| Doctor | 139033662514 | success |
| Lint | 139033662487 | success |
| Secret scan | 139033662479 | success |
| Qodana | 139033662427 | success |
| Upstream records advisory | 139033662557 | success |
| ci-success | 139038391675 | success |

Governance run: `governance-success` (job 139033732189) is pass. `gh pr checks 21 --required` exits 0 with `ci-success pass` and `governance-success pass`.

Wrapper and mirror evidence (five Gradle jobs: Build, Integration tests, Functional tests (build-logic), Compose smoke test, Doctor; logs fetched with `gh api --allow-escape-sequences repos/aanbrn/axon5-dcb-courses/actions/jobs/<id>/logs`):

- `grep -F 'retry-on-rate-limit:'` finds **0** lines in all five logs, so the wrapper never had to retry. `grep 'status code 429'` also finds 0 lines in all five.
- The step "Use the Maven Central mirror (CI only)" has conclusion `success` in all five jobs (read from the jobs API step list), and each log contains the step's command (`install -D -m 0644 scripts/ci/maven-central-mirror.init.gradle.kts ... init.d/maven-central-mirror.init.gradle.kts`). The raw log does not print the step's display name, only its command, which is why the API step list is the primary evidence.
- Each of the five logs shows the Gradle step invoked through `scripts/ci/retry-on-rate-limit.sh`.

Plan verification commands: `gh pr checks 21 --required` exit 0; the CI-run job check (`select(.conclusion != "success") | length` equals 0) exit 0.

## Task Commits

No repository files changed in this plan, so there are no task commits. The SUMMARY is committed on its own as the plan metadata commit (`docs(02-30)`); STATE.md and ROADMAP.md are left to the orchestrator.

## Files Created/Modified

- `.planning/phases/02-runtime-infrastructure-dcb-spikes/02-30-SUMMARY.md` - this record

## Decisions Made

None beyond the author's `push-now` consent recorded above.

## Deviations from Plan

None - plan executed exactly as written. Two procedural notes that are not deviations: the plan's Task 3 text greps for the step name in the logs, but GitHub prints only a step's command in the raw log, so the step's conclusion was read from the jobs API and its command from the log; and the Task 3 polling used `until` loops instead of fixed `sleep` calls because the harness blocks a leading `sleep`.

## Issues Encountered

None. No job failed at the pushed head; no rate limit reached any Gradle job, so the residual-429 branch of Task 3 did not apply.

## Observation for the author

The wrapper's retry path was not exercised at this head (0 retries, 0 429s). The earlier attempt-1 failure at 729dcff (Plugin Portal 429, before the mirror existed) is the only observed 429. One green run cannot prove the mirror eliminates the Plugin Portal rate limit; it shows the combination held on this run. If a 429 reappears, the job log names the answering host.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- UAT test 1 (full workflow green on a GitHub runner) can be re-checked against PR #21 at 07f309d.
- No blockers. PR #21 remains a draft; marking it ready and merging are the author's actions.

## Known Stubs

None.

## Self-Check: PASSED

- The pushed SHA equals the PR #21 head (`07f309de9428a7c657877aea785020e6a10bf458`).
- The three run URLs, ten CI job conclusions and `governance-success` above were read from the GitHub API after the runs completed.
- Plan verification commands both exit 0.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-25*
