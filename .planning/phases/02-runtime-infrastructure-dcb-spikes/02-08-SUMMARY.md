---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 08
subsystem: testing
tags: [testcontainers, postgresql, axon-server, gradle-testkit, github-actions, kover, qodana]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "Catalog images as tracked test-task inputs, ContainerImages, AxonServerEnvironment and KotlinTagResolver (plan 02-07); the Kover wiring and the testSuites TestKit fixture (plan 02-04); the tooling inventory (plan 02-05)"
provides:
  - "PostgresEnvironment: one lazily started PostgreSQL container per JVM from the catalog image, with its own component smoke test"
  - "An Axon Server integration smoke test (health of the container, or the configured external address) and AxonServerEnvironment.httpAddress"
  - "GradleFixture.catalog and the changeCatalog extension, and ContainerImageInputsFunctionalTest: the TestKit proof that a catalog image change or the external address re-runs integrationTest"
  - "A required, parallel integration-test CI job; the build job no longer runs integration tests"
  - "Inventory rows for Testcontainers, the test container images and Kover, and the Qodana excludes of the two new modules"
affects: [02-09, 02-10, 02-11, 02-14, phase-03-command-service, phase-04, qodana, ci]

plan_head_before: 77639dfd7a10f499605b5112eeb5af0bca8c7942
plan_head_after: 354f04eb99fe2bfdd890268497374307b7ac4cb1

actuals:
  tokens: 5800
  tasks: 2
  commits: 3

tech-stack:
  added: [testcontainers-postgresql (api of test-support), postgresql JDBC driver (componentTest of test-support only)]
  patterns:
    - "Every container used by the tests is a lazily started per-JVM object in :tooling:test-support that reads its image from ContainerImages and has a smoke test of its own"
    - "A TestKit fixture that needs a different catalog edits a copy of the real one through changeCatalog, which fails when the edit changes nothing"
    - "Integration tests have their own CI job with the same read-only permissions as build; the build job excludes them with -x integrationTest"

key-files:
  created:
    - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/PostgresEnvironment.kt
    - tooling/test-support/src/componentTest/kotlin/io/github/aanbrn/courses/testsupport/PostgresEnvironmentComponentTest.kt
    - tooling/test-support/src/integrationTest/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironmentIntegrationTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ContainerImageInputsFunctionalTest.kt
  modified:
    - tooling/test-support/build.gradle.kts
    - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironment.kt
    - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
    - .github/workflows/ci.yml
    - docs/tooling.md
    - qodana.yaml

key-decisions:
  - "KotlinTagResolverTest from plan 02-07 was extended, not duplicated: it gained the test that Axon's AnnotationBasedTagResolver tags the raw UUID (the part the plan requires and 02-07 did not have)"
  - "ContainerImageInputsFunctionalTest runs every Gradle call with --no-build-cache, so the verdict is the up-to-date check of the task's inputs; a result restored from the shared local build cache could otherwise hide a task that should have executed"
  - "changeCatalog is an extension function over a public catalog property of GradleFixture, because a 12th member function tripped detekt TooManyFunctions (limit 11); the fixture's public API stays the same shape"
  - "AxonServerEnvironment.httpAddress is null for an external server, since only its gRPC address is configured; the smoke test checks the configured address in that mode instead of guessing an HTTP port"
  - "The integration-test job needs no mise tools and no Node.js, so it has only checkout, setup-java and setup-gradle; the plan's other prohibitions (no token, no .env, no write permissions) hold"

patterns-established:
  - "Every container singleton has a smoke test, and every smoke test exercises a singleton (D-21: no unused code)"
  - "TestKit proofs of task-input tracking assert SUCCESS versus UP_TO_DATE under --no-build-cache"

requirements-completed: [TEST-11, TEST-14, TOOL-04]

coverage:
  - id: D1
    description: "PostgresEnvironment starts one PostgreSQL container from the catalog image and answers SELECT 1 over JDBC; the container's image equals the courses.image.postgres property (TEST-14)"
    requirement: TEST-14
    verification:
      - kind: integration
        ref: "tooling/test-support/src/componentTest/kotlin/io/github/aanbrn/courses/testsupport/PostgresEnvironmentComponentTest.kt (2 tests, failures=0)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The Axon Server singleton answers /actuator/health with 200, or in external mode its address is the configured one"
    requirement: TEST-14
    verification:
      - kind: integration
        ref: "tooling/test-support/src/integrationTest/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironmentIntegrationTest.kt (1 test, container mode, failures=0)"
        status: pass
    human_judgment: false
  - id: D3
    description: "A catalog image change or -Pcourses.axonserver.external re-runs integrationTest, while an unchanged second run is up to date (TOOL-04, INFRA-09 sub-scope, assumption A2)"
    requirement: TOOL-04
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ContainerImageInputsFunctionalTest.kt (3 tests, failures=0)"
        status: pass
    human_judgment: false
  - id: D4
    description: "KotlinTagResolverTest pins that Axon's AnnotationBasedTagResolver yields the raw UUID while the custom resolver yields TypeID strings"
    requirement: TEST-14
    verification:
      - kind: unit
        ref: "backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt (5 tests, failures=0)"
        status: pass
    human_judgment: false
  - id: D5
    description: "ci.yml runs 'build -x integrationTest' in build and 'integrationTest' in a parallel integration-test job with contents: read, SHA-pinned actions and persist-credentials: false; ci-success requires it; actionlint and zizmor report nothing"
    requirement: TEST-11
    verification:
      - kind: other
        ref: "mise x -- actionlint (clean); mise x -- zizmor --offline --no-progress .github/workflows (No findings); grep acceptance counts 1/1/1"
        status: pass
    human_judgment: false
  - id: D6
    description: "The integration-test job succeeds on a GitHub-hosted runner and is listed as required by ci-success"
    requirement: TEST-11
    verification: []
    human_judgment: true
    rationale: "A GitHub-hosted run (Docker on the runner, anonymous pull of the pinned Axon Server image, setup-gradle without Node or mise) cannot be exercised from this worktree; it is observed on the first pull request run"
  - id: D7
    description: "docs/tooling.md lists Testcontainers, the test container images and Kover and states the fast loop; qodana.yaml excludes tooling/test-support/build and backend/command-service/build; checkToolingInventory and spotlessCheck pass"
    requirement: TOOL-04
    verification:
      - kind: other
        ref: "./gradlew checkToolingInventory spotlessCheck (BUILD SUCCESSFUL); grep counts Testcontainers=2, Kover=1, qodana paths=2"
        status: pass
    human_judgment: false

duration: 70min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 08: Test infrastructure expansion and the integration-test CI job Summary

**A catalog-pinned PostgreSQL singleton and an Axon Server health smoke test join the tracer's singleton, a TestKit test proves that a changed image or external address re-runs `integrationTest` while an unchanged run is up to date, and CI runs the integration tests in their own required `integration-test` job in parallel with `build`.**

## Performance

- **Duration:** about 70 min (a full `./gradlew build` takes about 4.5 min and the build-logic functional suite dominates it)
- **Started:** about 2026-11-23T10:00:00Z (estimated; not captured at start)
- **Completed:** 2026-11-23T11:20:00Z
- **Tasks:** 2 (Task 1 as RED, GREEN)
- **Files modified:** 11 (4 created, 7 modified), 292 insertions, 7 deletions

## Accomplishments

- `PostgresEnvironment` starts one `org.testcontainers.postgresql.PostgreSQLContainer` per JVM, lazily, from `ContainerImages.postgres` (`asCompatibleSubstituteFor("postgres")`, `withReuse(true)`). It exposes `jdbcUrl`, `username`, `password` and `image`. The 2.x class package was confirmed by compiling (assumption A3 holds: `org.testcontainers.postgresql`, from the `testcontainers-postgresql` artifact, now an `api` dependency of `:tooling:test-support`). There is no Spring and no `@ServiceConnection`.
- `PostgresEnvironmentComponentTest` runs `SELECT 1` through a JDBC connection and checks that the container's image equals the catalog image. `AxonServerEnvironmentIntegrationTest` GETs `/actuator/health` on the container's HTTP port and expects 200, or in external mode checks that the singleton's address is the configured one. Both ran against real containers and passed (the PostgreSQL pin is therefore checked at the reference level, tag plus digest).
- `ContainerImageInputsFunctionalTest` (3 tests) proves assumption A2: in a `testSuites` fixture a second identical `:lib:integrationTest` run is up to date, and the task executes again after `image-axonserver` is retagged in the fixture's own catalog copy and after `-Pcourses.axonserver.external=localhost:9999` is added.
- `KotlinTagResolverTest` (plan 02-07's four tests) gained the fifth: Axon's `AnnotationBasedTagResolver` tags `StudentSubscribed` with the raw UUIDs and the custom resolver's tags differ from them.
- CI: `build` runs `./gradlew build -x integrationTest --console=plain`; the new `integration-test` job ("Integration tests", `ubuntu-latest`, 30 minutes, `contents: read`, `persist-credentials: false`, SHA-pinned actions, setup-java on `.java-version`, setup-gradle with the read-only cache outside `main`) runs `./gradlew integrationTest --console=plain` and uploads `integration-test-reports` on failure; `ci-success` needs it. No token, no `.env`, no registry login.
- `docs/tooling.md` has rows for Testcontainers, the test container images (`image-*`) and Kover, and one paragraph on the fast loop; `qodana.yaml` excludes the two new modules' build directories (the Qodana excludes that plan 02-07 deferred).

## TDD record (Task 1)

- **RED** (`59f285a`): the tests were written first; the RED commit also holds placeholder singletons and a placeholder `changeCatalog` so that the tests compile and fail on their assertions rather than on a build error. The target tests failed for the planned reasons: `PostgresEnvironmentComponentTest` (2 of 2 failed: `SQLException` on the empty URL and `AssertionFailedError` on the empty image), `AxonServerEnvironmentIntegrationTest` (`ConnectException`/`UnresolvedAddressException` on `http://null/...`), and `ContainerImageInputsFunctionalTest` `changing a catalog image runs the integration suite again` (`expected:<SUCCESS> but was:<UP_TO_DATE>`, the placeholder left the catalog unchanged). `semanticAssessment`: the targets executed and failed on the planned behavior; no zero-test, setup or syntax fault. Two of the three functional tests and the new tag-resolver test were green in RED by design: they characterize behavior that plan 02-07 already delivered (unchanged run up to date, external address as input, Axon's raw-UUID resolver). The classifier `gsd_run check tdd-red-evidence` was not run, because `workflow.tdd_mode` is off and the plan is `type: execute`.
- **GREEN** (`f90f978`): real singletons, `httpAddress`, and the fixture's catalog support; all target tests pass.
- **REFACTOR:** none.

## Task Commits

1. **Task 1 RED: failing tests for the singletons and catalog-driven re-runs** - `59f285a` (test)
2. **Task 1 GREEN: PostgreSQL singleton, health smoke, fixture catalog support** - `f90f978` (feat)
3. **Task 2: integration-test CI job, inventory rows, Qodana excludes** - `354f04e` (chore)

**Plan metadata:** the commit holding this SUMMARY (docs: complete plan).

## Files Created/Modified

- `tooling/test-support/src/main/kotlin/.../PostgresEnvironment.kt` - the per-JVM PostgreSQL singleton
- `tooling/test-support/src/main/kotlin/.../AxonServerEnvironment.kt` - `httpAddress` for the health smoke test
- `tooling/test-support/build.gradle.kts` - `api(libs.testcontainers.postgresql)`, `componentTestImplementation(libs.postgresql)`
- `tooling/test-support/src/componentTest/.../PostgresEnvironmentComponentTest.kt`, `src/integrationTest/.../AxonServerEnvironmentIntegrationTest.kt` - the smoke tests
- `backend/command-service/src/test/kotlin/.../KotlinTagResolverTest.kt` - the AnnotationBasedTagResolver contrast test
- `build-logic/src/functionalTest/kotlin/.../GradleFixture.kt` - public `projectDir` and `catalog`, the `changeCatalog` extension
- `build-logic/src/functionalTest/kotlin/.../ContainerImageInputsFunctionalTest.kt` - the TestKit input-tracking proof
- `.github/workflows/ci.yml` - `build -x integrationTest`, the `integration-test` job, `ci-success` needs
- `docs/tooling.md`, `qodana.yaml` - inventory rows and fast-loop line, Qodana excludes

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Detekt `TooManyFunctions` on `GradleFixture`**
- **Found during:** Task 1, `:build-logic:check`
- **Issue:** a public `changeCatalog` member made the class's 12th function (limit 11), which failed `:build-logic:detekt`.
- **Fix:** `catalog` became a public property whose setter rewrites the fixture settings, `projectDir` became public, and `changeCatalog` is an extension function in the same file. The KDoc parameter became `@property`.
- **Files modified:** `GradleFixture.kt`
- **Verification:** `:build-logic:detekt` and the functional test pass; full `./gradlew build` green.
- **Committed in:** `f90f978`

**2. [Rule 1 - Bug] Detekt `UndocumentedPublicClass` on the two new smoke tests**
- **Found during:** Task 1, `:tooling:test-support:check`
- **Issue:** the componentTest and integrationTest sources need a one-line KDoc per test class.
- **Fix:** added the KDoc lines.
- **Committed in:** `f90f978`

### Plan adjustments, not deviations of intent

- The plan's `<files>` lists `KotlinTagResolverTest.kt` as a file to create; plan 02-07 had already created it (orchestrator note), so it was extended with the one missing test and no duplicate exists.
- The plan's `catalogOverride` parameter of `testSuites` ("for example") became the `changeCatalog` function, because the proof needs to change the catalog after a first run, which a creation-time parameter cannot do. Existing callers are unchanged.
- The plan's second `<automated>` check for Task 2 (`zizmor --offline`) was run as written; CI's `lint` job runs zizmor online, which this change does not affect.

---

**Total deviations:** 2 auto-fixed (1 blocking, 1 bug), plus two disclosed plan adjustments
**Impact on plan:** none of them changes the plan's scope or any must-have truth.

## Issues Encountered

- `./gradlew :build-logic:check` takes about 5.5 minutes because the whole functional suite runs; a first run reported only the detekt finding, so the fixes were verified with `:build-logic:detekt` plus the single functional class, then with one full `./gradlew build` (4.3 min, green) at the end.
- Other worktrees' integration suites were running on the same Docker engine; no container start failed from contention.

## Known Stubs

None. The placeholders of the RED commit are replaced in `f90f978`.

## Threat Flags

None. The new `integration-test` job is covered by T-02-18 (job-level `contents: read`, `persist-credentials: false`, SHA-pinned actions, actionlint and zizmor clean) and the stale-green risk by T-02-55 (`ContainerImageInputsFunctionalTest`); no install commands ran (T-02-SC).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plans 02-09 to 02-11 and Phase 3 can use `PostgresEnvironment` and `AxonServerEnvironment`, and a module that adds integration tests is picked up by the `integration-test` job without editing `ci.yml`.
- Deferred, still open from plan 02-07: Renovate does not track the catalog `image-*` lines (needs a regex manager in `renovate.json5`, a governed path outside this plan), and `CommandServiceTestApplication.inMemory()` has no caller yet. Neither belongs to this plan's files.
- The first pull request run should confirm the `integration-test` job on a hosted runner (coverage D6) and then be added to the repository ruleset's required checks only through `ci-success`, which already aggregates it.

## Self-Check: PASSED

- All four created files exist on disk; `59f285a`, `f90f978` and `354f04e` are ancestors of HEAD (`git rev-list --count 77639df..HEAD` = 3 before this commit).
- Acceptance greps: `object PostgresEnvironment` 1; `AnnotationBasedTagResolver` at least 1; `needs: [build, lint, secrets, qodana, integration-test]` 1; `./gradlew integrationTest --console=plain` 1; `build -x integrationTest` 1; `Testcontainers` 2; `Kover` 1; qodana paths 2. `ContainerImageInputsFunctionalTest` result XML: `tests="3" skipped="0" failures="0"`.
- Result XML present for componentTest, integrationTest and `KotlinTagResolverTest` (`tests="2"`, `"1"`, `"5"`, all `failures="0"`).
- `mise x -- ./gradlew build --console=plain` green (4m 19s) after `spotlessApply`; actionlint and zizmor clean.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
