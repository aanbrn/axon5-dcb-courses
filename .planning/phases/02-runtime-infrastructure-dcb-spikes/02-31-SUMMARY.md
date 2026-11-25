---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 31
subsystem: infra
tags: [github-actions, gradle, init-script, maven-central, mirror, testkit, renovate, adr]

# Dependency graph
requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: plan 02-24, the 429 retry wrapper, the five wrapped Gradle steps and the governed scripts/ci/** path
provides:
  - scripts/ci/maven-central-mirror.init.gradle.kts, a CI-only init script that puts Google's Maven Central mirror first in the plugin and dependency repositories of every build
  - CiMavenMirrorFunctionalTest, four TestKit tests that pin the repository order under isolated projects and the configuration cache
  - the five Gradle jobs (build, integration-test, functional-test, compose-smoke, doctor) installing the script into the runner's Gradle user home init.d
  - ADR-0016, the record of the CI dependency download decisions and their trust position
  - a renovate.json5 ignorePaths entry that keeps the mirror out of Renovate's registries
affects: [02-30 push and CI proof, 06 CICD-05 dependency verification]

# Actuals (#2632)
actuals:
  tokens: 21000
  tasks: 2
  commits: 3
plan_head_before: 618b691a6a3e393a7c768c58b2e00b209562e390
plan_head_after: 79eb7c7423492cfe46fac5dad73afb5a150fc8f0

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "CI-only Gradle init script installed by the workflow into init.d: identical command and content in every job, so configuration-cache entries restored from main stay reusable"
    - "A probe init script that prints repository lists at settingsEvaluated, to assert repository order from TestKit"

key-files:
  created:
    - scripts/ci/maven-central-mirror.init.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/CiMavenMirrorFunctionalTest.kt
    - docs/adr/0016-ci-dependency-downloads.md
  modified:
    - .github/workflows/ci.yml
    - build-logic/build.gradle.kts
    - renovate.json5

key-decisions:
  - "The mirror goes first, in beforeSettings, for pluginManagement and dependencyResolutionManagement of every build; the Gradle Plugin Portal is added back behind it because it is the implicit default that disappears once the list is not empty"
  - "The explanation of the init script sits below its @Suppress annotation, because Spotless takes every comment above the first statement for the licence header and rewrites it"
  - "renovate.json5 lists the :ignoreModulesAndTests paths again before scripts/ci/**, because an ignorePaths array replaces the preset's"

patterns-established:
  - "Gradle files under scripts/ci/ are ignored by Renovate; their repositories are not lookup registries"

requirements-completed: [INFRA-03, INFRA-04, TOOL-03]

coverage:
  - id: D1
    description: "With the init script the mirror is the first plugin repository and the first dependency repository of the fixture root build and of the real build-logic build, the declared repositories follow in their original order (plugins: mirror, Plugin Portal; fixture dependencies: mirror, Maven Central; build-logic dependencies: mirror, Plugin Portal, Maven Central)"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../CiMavenMirrorFunctionalTest.kt#with the init script the mirror is the first plugin and dependency repository of every build / the declared repositories follow the mirror in their original order"
        status: pass
    human_judgment: false
  - id: D2
    description: "Without the init script both builds keep exactly the declared repositories (plugin lists empty), and a second run with the init script reuses the configuration cache, all under --configuration-cache --isolated-projects"
    requirement: INFRA-04
    verification:
      - kind: integration
        ref: "CiMavenMirrorFunctionalTest.kt#without the init script both builds keep exactly the declared repositories / a second run with the init script reuses the configuration cache"
        status: pass
    human_judgment: false
  - id: D3
    description: "The real root and build-logic builds resolve their classpaths with the script applied and --refresh-dependencies, with no FAILED dependency"
    requirement: INFRA-03
    verification:
      - kind: other
        ref: "./gradlew --init-script scripts/ci/maven-central-mirror.init.gradle.kts --refresh-dependencies buildEnvironment :command-service:dependencies (status 0, FAILED count 0)"
        status: pass
    human_judgment: false
  - id: D4
    description: "All five Gradle jobs install the script after setup-gradle with an identical step; settings files, the version catalog, cache-provider basic and cache-read-only are unchanged"
    requirement: INFRA-04
    verification:
      - kind: other
        ref: "non-comment references to maven-central-mirror.init.gradle.kts in ci.yml print 5; cache-read-only 5; cache-provider: basic 5; actionlint and zizmor (offline) clean"
        status: pass
    human_judgment: false
  - id: D5
    description: "ADR-0016 is accepted, records the options, the 429 retry, the kept cache settings and the trust position (unverified source until CICD-05), and passes checkAdrs"
    requirement: TOOL-03
    verification:
      - kind: other
        ref: "./gradlew checkAdrs spotlessCheck; grep CICD-05 docs/adr/0016-ci-dependency-downloads.md prints 3"
        status: pass
    human_judgment: false
  - id: D6
    description: "Renovate reads only the declared repositories: its extraction no longer lists the mirror in any registry URL and the dependency count is unchanged"
    requirement: TOOL-03
    verification:
      - kind: other
        ref: "renovate --platform=local --dry-run=extract (mirror URL 37 occurrences before the ignorePaths entry, 0 after; 196 depName entries before and after); renovate-config-validator --strict renovate.json5"
        status: pass
    human_judgment: false
  - id: D7
    description: "The mirror absorbs a real Maven Central 429 on a GitHub runner"
    verification: []
    human_judgment: true
    rationale: "A genuine rate limit cannot be provoked locally; the CI proof on a runner is plan 02-30 (push and CI run, author-gated)"

# Metrics
duration: 15min
completed: 2026-11-25
status: complete
---

# Phase 02 Plan 31: CI-only Maven Central mirror first through an init script Summary

**A governed Gradle init script that puts Google's Maven Central mirror first in every CI build's plugin and dependency repositories, installed into all five Gradle jobs, pinned by TestKit tests under isolated projects and the configuration cache, recorded in ADR-0016, and kept out of Renovate.**

## Performance

- **Duration:** about 15 min
- **Started:** 2026-11-25T06:08:56Z
- **Completed:** 2026-11-25T06:24:00Z
- **Tasks:** 2
- **Files modified:** 6 (3 created, 3 modified)

## Accomplishments

- `scripts/ci/maven-central-mirror.init.gradle.kts` runs in `gradle.beforeSettings`: it adds a `MavenCentralMirror` Maven repository (release content only) followed by `gradlePluginPortal()` to `pluginManagement.repositories`, and the same mirror to `dependencyResolutionManagement.repositories`. Gradle appends the repositories the settings scripts declare after these, so the declared order stays behind the mirror. It reads no environment variable or system property, touches no project and never names the insecure continue option.
- `CiMavenMirrorFunctionalTest` (four tests, 1m 30s cold) runs a fixture build that includes the real build-logic, with the script and a probe init script that prints both repository lists per build at `settingsEvaluated`. It proves mirror-first in both builds, the exact original order behind it, the untouched lists without the script and the configuration-cache reuse of a second run, all under `--configuration-cache --isolated-projects`. The script is a `functionalTest` input, so a change to it reruns the cached test.
- The real root build and the included build-logic build resolve with the script applied: `./gradlew --init-script scripts/ci/maven-central-mirror.init.gradle.kts --refresh-dependencies buildEnvironment :command-service:dependencies` exits 0 with no `FAILED` line.
- All five Gradle jobs install the script in an identical step after setup-gradle, into `${GRADLE_USER_HOME:-$HOME/.gradle}/init.d/`. The workflow header now explains the mirror, the wrapper and the cache policy in one paragraph.
- ADR-0016 records the decision, six options and the trust position stated plainly: an additional, unverified download source until dependency verification arrives with CICD-05, integrity resting on HTTPS and the mirror's operator, and the new dependency on the mirror's availability.

## Task Commits

1. **Task 1 (tracer, TDD): init script, TestKit proof, compose-smoke installing it**
   - RED: `990ad56` (test)
   - GREEN: `3663882` (feat)
2. **Task 2: other four Gradle jobs, ADR-0016, Renovate ignore** - `79eb7c7` (ci)

**Plan metadata:** committed with this SUMMARY (docs: complete plan)

## TDD Gate Compliance

- **RED:** `test(02-31)` commit `990ad56`. Run before the script existed: `4 tests completed, 3 failed`. The three tests that apply the script failed with `UnexpectedBuildFailure ... The specified initialization script '.../scripts/ci/maven-central-mirror.init.gradle.kts' does not exist.` The fourth test (no init script) passed on purpose: it is the baseline and it also validated the probe and the expected declared lists (plugin lists empty, Maven Central, Plugin Portal URLs) before any implementation. Semantic assessment: the target tests executed and failed because the script under test was absent, not on a syntax, fixture or discovery fault. The plan is `type: execute`; the JUnit XML was produced by Gradle, and no `gsd_run check tdd-red-evidence` record was written, the same position as plan 02-24.
- **GREEN:** `feat(02-31)` commit `3663882`: `tests="4" failures="0"` on the first implementation.
- **REFACTOR:** none needed.

## Tracer feedback gate

Auto mode off, `human_verify_mode` default `end-of-phase`, automated-only `<verify>`: all three tracer verify commands were re-run before expansion (TestKit 4/4, real-build resolution with `--refresh-dependencies` status 0 and zero `FAILED`, `checkBuildScripts spotlessCheck :build-logic:detekt` plus actionlint and zizmor) and passed. Tracer verified end to end, expanded into Task 2.

## Files Created/Modified

- `scripts/ci/maven-central-mirror.init.gradle.kts` - the CI-only init script (SPDX MIT, governed path)
- `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/CiMavenMirrorFunctionalTest.kt` - the repository-order and configuration-cache proof
- `build-logic/build.gradle.kts` - the script added to the functionalTest `repoFiles` inputs (one line)
- `.github/workflows/ci.yml` - install step in the five Gradle jobs, header paragraph rewritten
- `docs/adr/0016-ci-dependency-downloads.md` - ADR-0016, accepted
- `renovate.json5` - `ignorePaths` with the preset paths repeated and `scripts/ci/**` added

## Decisions Made

- Mirror first, in `beforeSettings`, with the Plugin Portal added back behind it in the plugin list.
- Keep the author decisions of 2026-11-25: no settings file, catalog, cache provider or `cache-read-only` change, no insecure continue option, wrapper kept.
- The long explanation lives below the `@Suppress` annotation, not above it, because the repository's Spotless delimiter (`[^/\s]`) treats every leading comment as the licence header.
- Renovate: add `scripts/ci/**` to `ignorePaths` (see the extract result below).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Spotless deleted the init script's header comment**
- **Found during:** Task 1 (`spotlessCheck` after the GREEN run)
- **Issue:** the plan asked for a header comment, but the `kotlinGradle` Spotless target replaces every comment above the first non-comment line with the licence header, so the comment was removed by `spotlessApply`.
- **Fix:** moved the comment between the `@Suppress("UnstableApiUsage")` annotation and `gradle.beforeSettings`, with a one-line note on why. `spotlessCheck`, `checkBuildScripts` and detekt pass and the test is still green.
- **Files modified:** `scripts/ci/maven-central-mirror.init.gradle.kts`
- **Committed in:** `3663882`

**2. [Rule 3 - Blocking] Plan verify command is not zsh-safe**
- **Found during:** Task 1 verify 2
- **Issue:** `status=$?` fails in zsh (`status` is read-only), so the command never ran.
- **Fix:** ran the same command under `bash -c`. The plan text is unchanged.

**3. [Plan branch taken] Renovate showed the mirror, so renovate.json5 changed**
- **Found during:** Task 2 step 3
- **Issue:** the plan's conditional branch applied; the extract result is below.
- **Fix:** `scripts/ci/**` added to `ignorePaths`, validated with `renovate-config-validator --strict`.
- **Committed in:** `79eb7c7`

---

**Total deviations:** 2 auto-fixed (both Rule 3, blocking), 1 plan branch taken. **Impact on plan:** none on scope; the header text is intact and the Renovate entry is the one the plan allowed.

## Renovate extract result

`renovate --platform=local --dry-run=extract` (the file list comes from git, so the script had to be committed first). The init script itself yields `deps: []`, but the gradle manager treats it as a Gradle file and adds the mirror URL to the `registryUrls` of the other package files: 37 occurrences of the mirror URL across the extraction, 38 gradle files matched. With `scripts/ci/**` in `ignorePaths`: 0 occurrences, the init script is no longer a package file, and the dependency count is unchanged at 196 `depName` entries. `renovate.json5` therefore changed (15 lines), and `renovate-config-validator --strict` passes.

## Issues Encountered

- A one-time `spotlessApply` was needed to learn the header-comment behaviour above; nothing else.
- The sandbox refused one compound command that mixed `git` (inside `github.ref` text) with `grep`; the greps were run on their own.

## Residuals

- **TestKit fixtures inside functional-test** use TestKit's own Gradle user home, which has no `init.d` script, so the fixture builds there still resolve from the declared repositories; the wrapper covers a 429 only if Gradle's message reaches the job log.
- **Qodana** runs Gradle in its own container with its own Gradle home and is unchanged; it stays outside the wrapper too (plan 02-24).
- **First main run with the script** costs one configuration-cache miss for the jobs whose entries main restores, because the init script joins the fingerprint.
- **A mirror 5xx fails a build** without a retry (the wrapper retries only a 429); accepted in ADR-0016.
- **Not run locally:** the lint job's line-ending renormalisation step (it needs a compound git command the sandbox refuses). Every other lint step ran: actionlint, zizmor (offline), shellcheck over every tracked shell script, editorconfig-checker, `reuse lint`, `renovate-config-validator --strict`, `docker compose config --quiet`, and all eight tracked shell tests. The two changed file types, YAML and Markdown, are covered by Prettier through `spotlessCheck`.
- **The CI proof on GitHub runners is plan 02-30**; a real 429 absorbed by the mirror cannot be shown locally.
- **Merge hook:** the branch touches governed files (`scripts/ci/**`, `.github/workflows/ci.yml`, `build-logic/build.gradle.kts`, `renovate.json5`), so the merge prompt will list them (ADR-0015).

## Known Stubs

None.

## Threat Flags

None beyond the plan's register. T-02-78 (unverified mirror) is accepted and recorded in ADR-0016; T-02-79 is mitigated by the governed path and the repository-order test; T-02-80 and T-02-81 are accepted and recorded; no package install happened (T-02-SC).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-30 can push the branch and prove the mirror, the wrapper and the cache policy on GitHub runners.
- CICD-05 (Phase 6) adds dependency verification, which then covers the mirrored artifacts with no mirror-specific entry; ADR-0016 names that revisit point.

## Self-Check: PASSED

- FOUND: `scripts/ci/maven-central-mirror.init.gradle.kts`, `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/CiMavenMirrorFunctionalTest.kt`, `docs/adr/0016-ci-dependency-downloads.md`
- FOUND commits `990ad56`, `3663882`, `79eb7c7` on `worktree-agent-a4a7eb461a940b043` (3 commits since plan base `618b691`)
- Acceptance criteria re-run: mirror URL in the script 1 (>=1); mirror in settings files and catalog 0; `allowInsecureContinueWhenDisabled` in the script 0; non-comment script references in ci.yml 5 (>=5); `cache-read-only` 5, `cache-provider: basic` 5; `status: "accepted"` 1; `CICD-05` in the ADR 3
- Plan verification: `CiMavenMirrorFunctionalTest` 4/4 green; real build resolves with the script and `--refresh-dependencies`; `checkBuildScripts spotlessCheck :build-logic:detekt checkAdrs` green; actionlint, zizmor, editorconfig-checker, `reuse lint` clean; ToolingTableCheck and ImageReferenceCheck functional tests green

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-25*
