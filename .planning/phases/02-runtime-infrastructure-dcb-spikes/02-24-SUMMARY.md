---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 24
subsystem: infra
tags: [github-actions, gradle, maven-central, rate-limit, bash, governance]

# Dependency graph
requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: the required CI jobs (build, integration-test, functional-test, compose-smoke, doctor) and the governed-paths machinery
provides:
  - scripts/ci/retry-on-rate-limit.sh, a one-retry wrapper keyed to Gradle's HTTP 429 message
  - scripts/ci/test-retry-on-rate-limit.sh, 48 shell assertions picked up by the lint job's shell-test step
  - all five Gradle steps of the required jobs running through the wrapper
  - scripts/ci/** as a governed path with its Edit ask rule
affects: [02-30 push and CI proof, 02-31 CI-only Maven Central mirror]

# Actuals (#2632)
actuals:
  tokens: 5300
  tasks: 2
  commits: 3
plan_head_before: e45269c8271bb77e1b7163856789c17a602c26a8
plan_head_after: b8410c206b5c2d004144ce0ab2abbc858d556827

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "CI launcher under scripts/ci/ with a sibling test-*.sh that the lint job discovers by pathspec"
    - "Retry keyed to one fixed error signature, bounded to a single rerun, announced with a fixed-text annotation"

key-files:
  created:
    - scripts/ci/retry-on-rate-limit.sh
    - scripts/ci/test-retry-on-rate-limit.sh
  modified:
    - .github/workflows/ci.yml
    - governance/governed-paths.txt
    - .claude/settings.json

key-decisions:
  - "The wrapper searches the merged output for the fixed string `Received status code 429 from server`, so a real test, compile or check failure is never retried"
  - "Under GitHub Actions a retry prints both the plain line and the warning annotation, as the plan specifies"
  - "Qodana stays unwrapped: a retry would repeat an analysis of up to 60 minutes and its cache is restored from main"

patterns-established:
  - "scripts/ci/** is governed: pull requests that touch a CI launcher need the governance-approved label"

requirements-completed: [INFRA-03, INFRA-04, TOOL-03]

coverage:
  - id: D1
    description: "A Gradle step that fails with Gradle's 429 message is rerun exactly once after a delay and the second result is final; any other failure, and success, runs once with the command's own exit status"
    requirement: INFRA-03
    verification:
      - kind: unit
        ref: "scripts/ci/test-retry-on-rate-limit.sh#429 then success / 429 twice / 429 then other failure / other failure / success / 429 on stderr"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every retry is announced with a fixed-text warning annotation under GitHub Actions and a plain line elsewhere; arguments reach the command unchanged; usage and delay errors exit 2 before the command runs"
    requirement: INFRA-03
    verification:
      - kind: unit
        ref: "scripts/ci/test-retry-on-rate-limit.sh#arguments / no command / delay / outside Actions / default delay / fixed text"
        status: pass
    human_judgment: false
  - id: D3
    description: "All five Gradle invocations of the required jobs (build, integration-test, functional-test, compose-smoke host scenario, doctor) run through the wrapper; Qodana and the cache policy are unchanged"
    requirement: INFRA-04
    verification:
      - kind: other
        ref: "grep -cE '^\\s+(run: )?\\./gradlew ' .github/workflows/ci.yml prints 0; non-comment wrapper references print 5; cache-read-only count stays 5"
        status: pass
      - kind: other
        ref: "mise x -- actionlint && mise x -- zizmor --offline --no-progress .github/workflows"
        status: pass
    human_judgment: false
  - id: D4
    description: "scripts/ci/** is governed: the governed-paths entry and the Edit ask rule agree and the pull-request gate still parses the list"
    requirement: TOOL-03
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew checkClaudeAskRules spotlessCheck"
        status: pass
      - kind: unit
        ref: "scripts/governance/test-check-pr.sh (173 passed, 0 failed)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The retry survives a real transient Maven Central 429 on a GitHub runner"
    verification: []
    human_judgment: true
    rationale: "A genuine 429 cannot be provoked locally; the CI proof on a runner is plan 02-30 (push and CI run, author-gated)"

# Metrics
duration: 10min
completed: 2026-11-25
status: complete
---

# Phase 02 Plan 24: Rate-limit retry wrapper for the required CI jobs Summary

**A governed bash wrapper that reruns a Gradle step once, after a delay, only when Gradle reports HTTP 429 from Maven Central, wired around all five Gradle steps of the required jobs and covered by 48 shell assertions.**

## Performance

- **Duration:** about 10 min
- **Started:** 2026-11-25T11:15:00Z (approximate)
- **Completed:** 2026-11-25T11:25:00Z
- **Tasks:** 2
- **Files modified:** 5 (2 created, 3 modified)

## Accomplishments

- `scripts/ci/retry-on-rate-limit.sh` runs `"$@"` with both streams merged, streamed live and copied to a temp log. A non-zero status whose log contains `Received status code 429 from server` triggers one fixed-text announcement (plain line, plus a `::warning title=Maven Central rate limit::` annotation under GitHub Actions), a sleep of `RATE_LIMIT_RETRY_DELAY_SECONDS` (default 60) and a single rerun whose status is final. Everything else exits with the command's own status. No `eval`, no re-quoting.
- `scripts/ci/test-retry-on-rate-limit.sh` proves the nine behaviours of the plan with fake commands (48 assertions, about two seconds): success runs once, a non-429 failure runs once and keeps its status, 429 then success, 429 twice (no third run), 429 then another failure, 429 on standard error only, argument fidelity (spaces, glob characters, `$HOME`, a quote, an empty word), usage and invalid-delay errors exiting 2 before the command runs, the plain message outside Actions, plus the delay itself, the 60 s default (through a fake `sleep`) and the injection case.
- The compose-smoke host scenario (the job that failed at 729dcff), build, integration-test, functional-test and doctor (`scripts/doctor.sh --ci`, whose Gradle output reaches the wrapper) now run through the wrapper. The mechanism is documented once in the workflow header, with the Qodana exception recorded there.
- `scripts/ci/**` is governed (entry after `tooling/doctor/**`) with the `Edit(/scripts/ci/**)` ask rule, so `checkClaudeAskRules` stays green and pull requests that touch the launcher need `governance-approved`.

## Task Commits

1. **Task 1 (tracer, TDD): wrapper, shell tests, compose-smoke host scenario, governance**
   - RED: `60d331d` (test)
   - GREEN: `d8987ae` (feat)
2. **Task 2: every Gradle step of the required jobs through the wrapper, mechanism documented once** - `b8410c2` (ci)

**Plan metadata:** committed with this SUMMARY (docs: complete plan)

## TDD Gate Compliance

- **RED:** `test(02-24)` commit `60d331d`. Run before the wrapper existed: `8 passed, 40 failed`. Every wrapper-dependent assertion failed with `env: .../scripts/ci/retry-on-rate-limit.sh: No such file or directory` (exit 127), which is the planned `[HEAD=error:127]` state. The 8 assertions that passed are negative ones (a command that must not run, no warning annotation) that hold trivially without a wrapper. Semantic assessment: the target suite executed and failed on the planned behaviour assertions (exit status, run count, retry line) because the script under test was absent, not on a syntax, fixture or discovery fault. The plan's `type: execute` carries no TDD gate-mode, and the shell test format is not one of the formats `gsd_run check tdd-red-evidence` parses, so no classifier record was produced; the evidence is the captured run above.
- **GREEN:** `feat(02-24)` commit `d8987ae`: `48 passed, 0 failed`.
- **REFACTOR:** none needed.

## Tracer feedback gate

Auto mode off and `human_verify_mode` is `end-of-phase` with an automated-only `<verify>`, so the tracer verify was re-run before expansion (`48 passed, 0 failed`, exit 0): tracer verified end to end, expanded into Task 2.

## Files Created/Modified

- `scripts/ci/retry-on-rate-limit.sh` - the wrapper (executable, SPDX MIT, header explains Gradle 9.9.2's own retry, the repository disabling and what the script never does)
- `scripts/ci/test-retry-on-rate-limit.sh` - self-contained shell tests, discovered by the lint job's `scripts/*/test-*.sh` pathspec
- `.github/workflows/ci.yml` - five Gradle steps wrapped, header paragraph, per-step pointer comment
- `governance/governed-paths.txt` - `scripts/ci/**` entry with its rationale
- `.claude/settings.json` - `Edit(/scripts/ci/**)` ask rule; nothing else changed

## Decisions Made

- Search the merged output for the fixed 429 string only; the delay is validated as digits before anything runs, so the annotation text is a constant plus a number.
- Print the plain retry line under Actions as well as the annotation (the plan's wording), accepting one duplicated line in the log.
- The build step's `run:` is folded with `>-` because the wrapped line exceeds 120 columns.
- Qodana stays outside the wrapper (residual below).

## Deviations from Plan

None - plan executed exactly as written. The only liberty is cosmetic: the tests carry 48 assertions across 12 cases instead of exactly nine, because the plan's nine behaviours were split into one assertion per fact and three cases were added (delay honoured, default delay, injection).

## Residuals

- **Qodana is not wrapped.** It runs Gradle inside its own container through `scripts/qodana.sh`, a retry would repeat an analysis of up to 60 minutes, and its cache is restored from main, where the job already runs. A 429 inside Qodana still fails that job.
- **The CI proof is plan 02-30.** A real 429 on a runner cannot be provoked locally; the first run that hits one will show the warning annotation. The push and the re-run of the failed job are author-gated and not part of this plan.
- **Doctor reruns whole.** When the doctor job retries, `scripts/doctor.sh --ci` reruns every check, not only the Gradle one; harmless, and the second result is final.
- **Plan 02-31** adds the CI-only Maven Central mirror; the wrapper covers a 429 from either host.

## Issues Encountered

- `shellcheck` reported SC2016 (info) on the single-quoted `'$HOME'` argument of the argument-fidelity test; fixed by writing it as `"\$HOME"` rather than suppressing it.
- One compound command mixing `git ls-files` with `grep` and `PIPESTATUS` was refused by the worktree sandbox; the shell tests were re-run through a plain glob loop instead (all green).

## Known Stubs

None.

## Threat Flags

None. The wrapper adds no network endpoint, auth path or file access beyond a `mktemp` log removed by `trap`; T-02-61 to T-02-64 mitigations are in place (governed path, fixed 429 string, at most one retry, constant annotation text, no `eval`, no repository change).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-31 can add its init script under the now-governed `scripts/ci/**` and edit the same `ci.yml` steps.
- Plan 02-30 pushes the work and proves it on GitHub runners.
- The merge hook will ask the author once at merge for the governed files in this branch (`scripts/ci/**` entry, `.github/workflows/ci.yml`, `.claude/settings.json`, `governance/governed-paths.txt`); that is expected (ADR-0015).

## Self-Check: PASSED

- FOUND: `scripts/ci/retry-on-rate-limit.sh`, `scripts/ci/test-retry-on-rate-limit.sh` (both `100755` in the index)
- FOUND commits `60d331d`, `d8987ae`, `b8410c2` on `worktree-agent-acc521b46821a3e8c`
- Acceptance criteria re-run: Task 1 (wrapper references 1, governed entry 1, ask rule 1, both scripts `100755`, 429 string 2) and Task 2 (bare `./gradlew` lines 0, wrapper references 5, bare doctor step 0, wrapped doctor step 1, Qodana line 1, `cache-read-only` count 5) all pass
- Plan verification: shell tests, shellcheck, actionlint, zizmor (offline), editorconfig-checker, REUSE, every tracked shell test, `checkClaudeAskRules`, `spotlessCheck` and `test-check-pr.sh` (173 passed, 0 failed) green; `retry-on-rate-limit.sh mise x -- ./gradlew help` succeeds

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-25*
