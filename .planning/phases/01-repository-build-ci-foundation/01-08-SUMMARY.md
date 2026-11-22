---
phase: 01-repository-build-ci-foundation
plan: 08
subsystem: infra
tags: [lefthook, gitleaks, reuse, actionlint, zizmor, shellcheck, spotless, gradle-profiler, build-scan]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-05: Spotless formats for every file type and the mise-pinned formatter binaries; plan 01-06: REUSE.toml and the README quick start that already tells contributors to run lefthook install"
provides:
  - "lefthook.yml: sequential pre-commit hook with jobs format (re-stages), gitleaks, reuse, actionlint, zizmor (offline) and shellcheck; no pre-push hook, no analysis, no tests"
  - "scripts/format-staged.sh: staged-path adapter for ./gradlew spotlessApply -PspotlessIdeHook=<absolute paths>, skipping deletions and the GSD-owned .planning and .claude files (except .claude/settings.json)"
  - "performance.scenarios: gradle-profiler scenarios clean_build, noop_build, abi_change and config_cache_hit"
  - "docs/performance.md: how to run the scenarios, the INFRA-10 speed-ups in place, the opt-in --scan Build Scan policy and a measured baseline"
  - "Spotless source filter excludes gradle-user-home and profile-out* (gradle-profiler output inside the project)"
affects: [01-10, 01-11, 01-12, 01-14]

plan_head_before: 7504900001138cc311c60cf1b565d4ae9e82cb02
plan_head_after: 98524a81b568644b3d52289e1b4882f247de0f4e

actuals:
  tokens: 9000
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Hook jobs run through mise x -- so the pinned tools are used without activating mise in the shell"
    - "The format job passes only staged paths to the Spotless IDE hook; the same Spotless configuration answers the hook and spotlessCheck in CI"
    - "Hooks are proven with lefthook run pre-commit --no-auto-install, never lefthook install"

key-files:
  created:
    - lefthook.yml
    - scripts/format-staged.sh
    - performance.scenarios
    - docs/performance.md
  modified:
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt

key-decisions:
  - "gradle-user-home and profile-out* are excluded from the shared Spotless filter: gradle-profiler writes them into the project, and their gc.properties files failed spotlessMiscCheck, so build failed inside the profiler scenarios"
  - "clean_build bypasses the build cache (--no-build-cache) and cleans only the root build, so it measures real compile and analysis work but not a cold repository; the cold figure is the first warm-up"
  - "The format job's glob is a list (file extensions plus the dotfile names) rather than dropping the glob, so a commit without any formattable file never starts Gradle"

patterns-established:
  - "Run lefthook with --no-auto-install in any session that shares .git with other checkouts: a plain lefthook run installs the hook and a checksum file into the shared git directory"
  - "Profiler runs: delete profile-out first (otherwise results go to profile-out-2) and remove gradle-user-home afterwards"

requirements-completed: [TEST-09, CICD-06, CICD-08, INFRA-10]

coverage:
  - id: D1
    description: "The pre-commit hook reformats staged Kotlin with the SPDX header and re-stages it, while an unstaged change in README.md stays untouched"
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "mise x -- lefthook run pre-commit with a staged badly formatted HookProbe.kt and an unstaged README.md edit: file formatted with header and re-staged, README edit intact, exit 0"
        status: pass
    human_judgment: false
  - id: D2
    description: "A staged synthetic GitHub token is blocked by the hook's gitleaks job"
    requirement: CICD-06
    verification:
      - kind: integration
        ref: "mise x -- lefthook run pre-commit --no-auto-install with a staged run-time generated ghp_ token: gitleaks job failed, hook exit 1"
        status: pass
    human_judgment: false
  - id: D3
    description: "Licence, workflow and script checks run in the hook; the shellcheck, actionlint and zizmor jobs are skipped by their globs when no matching file is staged; the whole tree passes the hook"
    requirement: CICD-08
    verification:
      - kind: integration
        ref: "mise x -- lefthook run pre-commit --all-files --no-auto-install: all six jobs pass, git diff --exit-code clean"
        status: pass
      - kind: other
        ref: "mise x -- shellcheck scripts/format-staged.sh; mise x -- reuse lint --quiet; mise x -- gitleaks git --pre-commit --staged --no-banner --redact"
        status: pass
    human_judgment: false
  - id: D4
    description: "performance.scenarios parses and runs all four scenarios; docs/performance.md records a median per scenario"
    requirement: INFRA-10
    verification:
      - kind: other
        ref: "mise x -- gradle-profiler --benchmark --dry-run --project-dir . --scenario-file performance.scenarios clean_build noop_build abi_change config_cache_hit (exit 0); the real benchmark then produced numbers for all four"
        status: pass
      - kind: other
        ref: "grep -cE '^\\| *(clean_build|noop_build|abi_change|config_cache_hit) *\\|' docs/performance.md prints 4"
        status: pass
    human_judgment: false
  - id: D5
    description: "Build Scans are opt-in through --scan only and nothing in the build applies the Develocity plugin"
    requirement: INFRA-10
    verification:
      - kind: other
        ref: "grep -rn 'com.gradle.develocity' build-logic/src/main/kotlin settings.gradle.kts | grep -v '//' | wc -l prints 0; grep -c -- '--scan' docs/performance.md prints 4"
        status: pass
    human_judgment: false
  - id: D6
    description: "gradle-profiler output inside the project is never formatted"
    requirement: INFRA-10
    verification:
      - kind: integration
        ref: "FormattingFunctionalTest#profiler output directories inside the project are never formatted"
        status: pass
    human_judgment: false
  - id: D7
    description: "The hook works when installed into a real clone (lefthook install) and fires on git commit"
    verification: []
    human_judgment: true
    rationale: "lefthook install was deliberately not run: the hooks directory is shared with the author's main checkout and every worktree. The hook was proven by lefthook run only; the author installs it once per clone as the README quick start says."

duration: 22min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 08: Pre-commit Hook and Build Performance Summary

**A fast lefthook pre-commit hook that formats exactly the staged files through the Spotless IDE hook and re-stages them, blocks staged secrets and runs REUSE, actionlint, zizmor and shellcheck; plus committed gradle-profiler scenarios with a measured baseline and an opt-in-only Build Scan policy**

## Performance

- **Duration:** about 22 min (the longest waits were gradle-profiler populating a fresh Gradle user home)
- **Started:** 2026-11-21T10:25Z
- **Completed:** 2026-11-21T10:48Z
- **Tasks:** 2
- **Files modified:** 6 (4 created, 2 modified)

## Accomplishments

- Tracer slice proven end to end without installing the hook: a staged, badly indented `HookProbe.kt` came out formatted with the header on line 1 and re-staged, while an unstaged edit to `README.md` was left exactly as it was; a staged run-time generated token made the gitleaks job, and so the hook, exit 1.
- `lefthook run pre-commit --all-files` passes over the whole tracked tree (format 5.4 s, the other five jobs about 2 s together) and leaves `git diff` empty. With a few staged files the whole hook takes about 3.5 s.
- The shellcheck, actionlint and zizmor jobs are skipped by their globs when no matching file is staged ("no files for inspection"); in the all-files run they ran on the real workflow and scripts.
- `performance.scenarios` runs all four scenarios; the baseline is committed in `docs/performance.md` with the Build Scan policy and the list of speed-ups in place.
- A real defect found by running the profiler (see Deviations): its output directories inside the project failed Spotless.

## Task Commits

1. **Task 1: End-to-end commit screening (tracer)** - `2433e8a` (feat)
2. **Bug found by Task 2: profiler output failed Spotless** - `fe9a0b8` (fix)
3. **Task 2: Build performance** - `98524a8` (feat)

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

Tracer feedback gate: the tracer `<verify>` is automated-only, so after the commit the checks were re-run (shellcheck, `spotlessCheck`, `reuse lint --quiet`, the staged gitleaks command, the line 2 header grep) and `lefthook run pre-commit --all-files` was added; all passed, so Task 2 proceeded.

## Files Created/Modified

- `lefthook.yml` - the pre-commit hook: `format`, `gitleaks`, `reuse`, `actionlint`, `zizmor`, `shellcheck`, sequential, `min_version: 2.2.2`, everything through `mise x --`
- `scripts/format-staged.sh` - turns staged relative paths into the absolute comma-separated list for `-PspotlessIdeHook`; skips deletions, `.planning/`, and `.claude/` except `.claude/settings.json`; rejects a comma in a path
- `performance.scenarios` - gradle-profiler scenarios with warm-up and iteration counts
- `docs/performance.md` - running the scenarios, speed-ups in place, Build Scan policy, baseline
- `build-logic/.../courses.root.gradle.kts` - `**/gradle-user-home/**` and `**/profile-out*/**` added to the excluded list
- `build-logic/.../FormattingFunctionalTest.kt` - one new test for the exclusion

## Decisions Made

See `key-decisions` above. The README quick start and `docs/tooling.md` already say to run `lefthook install` once per clone, so no further documentation change was needed for that.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] gradle-profiler output directories were Spotless targets**
- **Found during:** Task 2 (first benchmark run: `clean_build` and `noop_build` both failed their warm-up)
- **Issue:** gradle-profiler creates `gradle-user-home/` and `profile-out/` (or `profile-out-2/`) inside the project. The `misc` format's `**/*.properties` glob matched the `gc.properties` files in the Gradle caches, so `spotlessMiscCheck` failed and `build` with it. The files are ignored by git but not by Spotless.
- **Fix:** both patterns added to the shared `excluded` list. Test first (RED: the new TestKit test failed with the `gc.properties`-style violation), then the exclusion (GREEN).
- **Files modified:** `build-logic/src/main/kotlin/courses.root.gradle.kts`, `FormattingFunctionalTest.kt`
- **Verification:** `FormattingFunctionalTest` 9 of 9 pass; the second benchmark run completed for all four scenarios; `mise x -- ./gradlew build` BUILD SUCCESSFUL.
- **Committed in:** `fe9a0b8`

**2. [Rule 3 - Blocking, process] `lefthook run` installed the hook into the shared git directory**
- **Found during:** Task 1, step 3a (first `lefthook run pre-commit`)
- **Issue:** the first run printed "sync hooks" and, because lefthook auto-installs hooks on `run`, wrote a `pre-commit` hook into the shared hooks directory and a `lefthook.checksum` file into the shared `.git/info`, which is exactly what the orchestrator asked not to do. The hook would have run in the author's main checkout and every other worktree.
- **Fix:** I removed those two files, which I had just created (nothing else in either directory was touched; the hooks directory again holds only the git samples, and `.git/info` holds `exclude` and `refs` as before), and ran every later invocation with `--no-auto-install`, which verified that nothing was written again.
- **Follow-up for the author:** run `lefthook install` once in the main clone when ready; the hook then runs `lefthook run pre-commit`, which needs `lefthook` or `mise` on `PATH`.
- **Files modified:** none in the repository.

**3. [Process] The temporary-index form of the wave check could not be used**
- **Found during:** plan-level verification
- **Issue:** the session harness refuses `GIT_INDEX_FILE`. The plan's whole-tree hook check copies the index for exactly that reason.
- **Fix:** equivalent check on the real index, which held nothing staged: `lefthook run pre-commit --all-files --no-auto-install` passed and `git diff --exit-code` was empty, both after Task 1 and again after Task 2.

---

**Total deviations:** 3 (1 bug fixed, 1 blocking process slip repaired, 1 process difference)
**Impact on plan:** one extra build-logic commit; no change of scope. The hook and scenarios are exactly as specified.

## Issues Encountered

- The harness refused compound shell commands containing git (including `&&` chains and a `$(...)` that read the commit ledger), so git steps ran as plain single commands. The commit ledger was written as a plain file in the worktree git directory and read back with a literal hash.
- `lefthook run --no-colors` is not a flag in 2.2.2; the option is `--colors off`.
- The first gradle-profiler run (the `--dry-run` verify command) took about 8 minutes because it populated an empty `gradle-user-home/`; see timing below.
- `gradle-profiler` numbers its output directory `profile-out-2` when `profile-out` exists, and only `profile-out/` is in `.gitignore`. Docs tell the reader to delete the directory first, and the exclusion pattern covers both.

## Verification Results and Timing

| Command | Result | Wall clock |
| --- | --- | --- |
| `mise x -- shellcheck scripts/format-staged.sh` then `mise x -- ./gradlew spotlessCheck` (warm) | pass | 1.6 s |
| `mise x -- reuse lint --quiet` | pass | 1.1 s |
| `mise x -- gitleaks git --pre-commit --staged --no-banner --redact` (current tree) | pass | 0.4 s |
| `sed -n 2p scripts/format-staged.sh \| grep -c ...` | 1 | under 1 s |
| `mise x -- lefthook run pre-commit` with staged probe (step 3a) | pass | 3.6 s |
| `mise x -- lefthook run pre-commit` with staged synthetic token (step 3b) | exit 1 from gitleaks | 2.3 s |
| `mise x -- lefthook run pre-commit --all-files --no-auto-install` | pass, `git diff` empty | 7.4 s |
| `mise x -- gradle-profiler --benchmark --dry-run ... <four scenarios>` | pass | about 8 min with an empty `gradle-user-home/`; not re-measured warm |
| `mise x -- gradle-profiler --benchmark ... <four scenarios>` (warm user home) | pass, all four have numbers | 3 min 4 s |
| `grep -cE '^\| *(clean_build\|...) *\|' docs/performance.md` | 4 | under 1 s |
| `FormattingFunctionalTest` (9 tests) | pass | 32 s |
| `mise x -- ./gradlew build` (warm) | BUILD SUCCESSFUL | 2 s |

Validation gap: the `--dry-run` verify command exceeds the 180 s per-task feedback budget of `01-VALIDATION.md` on a machine with no `gradle-user-home/` (one-time download and warm-up of a second Gradle home; the dry run also builds). On a warm home it is far below the budget, but this was not measured. Every other command is under 30 s except the TestKit run (32 s) and the real benchmark.

Baseline recorded in `docs/performance.md` (macOS, arm64, 10 cores, 2026-11-21), median of measured builds: `clean_build` 4812 ms (1 warm-up, 3 builds; the cold first build took 115 s), `noop_build` 328 ms (2 warm-ups, 5 builds), `abi_change` 278 ms (2, 5), `config_cache_hit` 70 ms (2, 8).

## Known Stubs

None.

## Threat Flags

None. T-01-13 mitigated (gitleaks `--staged --redact` in the hook, proven blocking). T-01-14 mitigated: the synthetic token was generated at run time into a scratch file, never printed, and deleted; neither it nor any token-shaped literal appears in a committed file or this summary. T-01-16 mitigated: `--scan` only, documented, no Develocity plugin applied. T-01-SC: no packages installed.

## User Setup Required

None - no external service configuration required. The author runs `lefthook install` once in the main clone (deliberately not done during execution), as the README quick start and `docs/tooling.md` say.

## Next Phase Readiness

- Plan 01-12 (CI) lints all workflow files and scripts; the hook only handles changed ones. `editorconfig-checker` is not in the hook (the plan lists six jobs); CI should run it.
- Any later plan that adds a profiler-like tool writing into the project directory must add its output directories to the `excluded` list in `courses.root.gradle.kts`.
- Plan 01-10 runs next on this tree; the hook is not installed here, so its commits are not screened by it.

## Self-Check: PASSED

Created files exist on disk (`lefthook.yml`, `scripts/format-staged.sh`, `performance.scenarios`, `docs/performance.md`); commits `2433e8a`, `fe9a0b8` and `98524a8` are ancestors of HEAD; acceptance criteria hold (`stage_fixed: true` 1, non-comment `pre-push` 0, non-comment `detekt|dokka|functionalTest` 0, `--staged` 1, line 2 of the script is the SPDX line, `config_cache_hit` in the scenarios 2, `--scan` in the docs 4, no `profile-out` or `gradle-user-home` directory, no `com.gradle.develocity` reference, `git status --porcelain` shows no probe or token file); plan-level verification passes (the all-files hook run is clean, four baseline rows).

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
