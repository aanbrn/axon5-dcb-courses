---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 07
subsystem: testing
tags: [axon-server, dcb, testcontainers, gradle, kotlin, tags, ryuk, container-images]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "courses.test-suites convention with the integrationTest suite and kernel testFixtures (plan 02-04); CourseCreated and StudentSubscribed events and the shared Jackson mapper (plan 02-06)"
provides:
  - "Catalog images image-axonserver, image-postgres and image-ryuk as full tag-plus-digest references"
  - "ContainerImageArguments: catalog images and the external Axon Server address as tracked inputs of the componentTest and integrationTest tasks"
  - ":tooling:test-support with ContainerImages and the per-JVM AxonServerEnvironment singleton (or an external address)"
  - ":command-service as a plain Kotlin library with KotlinTagResolver and CommandServiceConfigurationEnhancer"
  - "CommandServiceTestApplication test fixture over Axon Server or an in-memory engine"
  - "ConditionalAppendIntegrationTest: the INFRA-04 proof that DCB rejects an outdated overlapping append and accepts a non-overlapping one"
affects: [02-08, 02-09, 02-10, 02-11, 02-14, phase-03-command-service, qodana, renovate]

plan_head_before: 355f186fcca4978076e35eab295353b581489fe2
plan_head_after: 8cae4eee4be23b35cfb926b527b74e33a5b249d6

actuals:
  tokens: 9100
  tasks: 1
  commits: 1

tech-stack:
  added: [axoniq-testcontainer 5.4.2, axon-server-connector 5.4.2, testcontainers 2.0.5, kotlin-reflect, logback-classic, postgresql driver (catalog only)]
  patterns:
    - "Catalog image references (tag and digest) reach test JVMs only as @Input system properties of a CommandLineArgumentProvider, never as literals"
    - "One lazily started Axon Server container per JVM in test-support, with an external-address escape hatch for the compose host scenario"
    - "A command-service Axon configuration is a ConfigurationEnhancer registered explicitly, not discovered through META-INF/services"
    - "Concurrency tests dispatch from a pool and hold every decision at a non-blocking barrier until each has sourced its criteria"

key-files:
  created:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ContainerImageArguments.kt
    - tooling/test-support/build.gradle.kts
    - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/ContainerImages.kt
    - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironment.kt
    - backend/command-service/build.gradle.kts
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt
    - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt
    - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt
    - backend/command-service/src/integrationTest/resources/logback-test.xml
    - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt
  modified:
    - gradle/libs.versions.toml
    - settings.gradle.kts
    - build-logic/src/main/kotlin/courses.test-suites.gradle.kts

key-decisions:
  - "The Axon Server digest is the Docker Hub index digest of axoniq/axonserver:2026.1.8-nonroot, because docker.axoniq.io is a pull-through alias of the same repository (its auth realm is auth.docker.io) and answered 429 to the digest probe; the container started from the docker.axoniq.io name with that digest is the pinned image (assumption A14 holds)"
  - "The Ryuk tag is 0.14.0, the tag Testcontainers 2.0.5 starts, read from the earlier probe's running reaper and the local image; its digest is the manifest-list digest"
  - "The sourcing barrier is a CompletableFuture-based non-blocking barrier, not a CountDownLatch: a blocking latch wait could occupy the thread that has to complete the other decision's sourcing"
  - "Both concurrency tests assert the outcome of each unit of work (null for commit, the failure for rejection), never a swallowed exception, and read the store back to check what was committed"

patterns-established:
  - "Tests obtain container images only through ContainerImages in test-support; the Konsist literal rule planned for the architecture module has the same grep as its first check"
  - "Test fixtures that expose Axon types declare them testFixturesApi, so a consuming suite compiles against them"

requirements-completed: [INFRA-04, TEST-11, TEST-14, TOOL-04]

coverage:
  - id: D1
    description: "A conditional append whose consistency marker is outdated for overlapping criteria is rejected with AppendEventsTransactionRejectedException on a real DCB Axon Server, and afterwards sourcing returns the creation plus exactly one subscription (INFRA-04)"
    requirement: INFRA-04
    verification:
      - kind: integration
        ref: "backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt#an append with an outdated marker for overlapping criteria is rejected"
        status: pass
    human_judgment: false
  - id: D2
    description: "Two concurrent appends whose criteria do not overlap (course A and course B) both commit"
    requirement: INFRA-04
    verification:
      - kind: integration
        ref: "ConditionalAppendIntegrationTest#appends whose criteria do not overlap both commit"
        status: pass
    human_judgment: false
  - id: D3
    description: "Edge INFRA-04 idempotency: the integration test passes twice against the same running Axon Server through -Pcourses.axonserver.external, because every run uses fresh TypeIDs and nothing is purged (D-16)"
    requirement: INFRA-04
    verification:
      - kind: integration
        ref: "./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun -Pcourses.axonserver.external=localhost:18124 (twice, tests=2 skipped=0 failures=0 both times; no container started)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The Axon Server, PostgreSQL and Ryuk images are catalog entries with tag and digest, reach the integration and component tasks as @Input system properties plus TESTCONTAINERS_RYUK_CONTAINER_IMAGE, and no test source holds an image literal; the started container reports the digest-pinned image (INFRA-09 sub-scope, TOOL-04)"
    requirement: TOOL-04
    verification:
      - kind: other
        ref: "grep -cE '^image-(axonserver|postgres|ryuk) = \".+:.+@sha256:[0-9a-f]{64}\"$' gradle/libs.versions.toml = 3; literal-constructor grep over tooling/test-support/src and backend/command-service/src = 0; docker inspect of the test container shows Config.Image with @sha256"
        status: pass
    human_judgment: false
  - id: D5
    description: "The tags of StudentSubscribed are course=<CourseId.toString()> and student=<StudentId.toString()>, never raw UUIDs; the resolver is the TagResolver of the command service wiring (D-43, spike 002)"
    requirement: INFRA-04
    verification:
      - kind: unit
        ref: "backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt (4 tests)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Local Axon Server container reuse across JVMs (withReuse(true) with testcontainers.reuse.enable=true) behaves as documented"
    verification: []
    human_judgment: true
    rationale: "Reuse is local and opt-in (RESEARCH A16); it was not exercised, because the author's ~/.testcontainers.properties does not enable it and CI never does"

duration: 50min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 07: Axon Server Conditional Append Tracer Summary

**A digest-pinned Axon Server, started once per JVM by `:tooling:test-support` from catalog images that reach the test tasks as tracked inputs, proves on `:command-service` wiring that a DCB conditional append with an outdated marker for overlapping criteria is rejected with `AppendEventsTransactionRejectedException`, while a concurrent append with non-overlapping criteria commits.**

## Performance

- **Duration:** about 50 min (a cold `./gradlew build` takes 6.5 min; the rest ran cached)
- **Started:** about 2026-11-23T09:50:00Z (estimated; not captured at start)
- **Completed:** 2026-11-23T10:40:00Z
- **Tasks:** 1 (a tracer)
- **Files modified:** 14 (11 created, 3 modified), 599 insertions

## Accomplishments

- The path from catalog to test is proven end to end. `image-axonserver`, `image-postgres` and `image-ryuk` are full tag-and-digest references in the version catalog. `ContainerImageArguments` (a `CommandLineArgumentProvider` with `@Input` properties) hands them to `componentTest` and `integrationTest` as `-Dcourses.image.*`, adds `-Dcourses.axonserver.external` when `-Pcourses.axonserver.external` is set, and the convention sets `TESTCONTAINERS_RYUK_CONTAINER_IMAGE` and `AXONIQ_UPDATE_CHECKER_DISABLED`. `docker inspect` of the started container shows `Config.Image` with the `@sha256` digest, and the Ryuk container started in the same run was the pinned `0.14.0`.
- `:tooling:test-support` holds `ContainerImages` (property-name constants and accessors that fail with the plan's message when a property is missing) and `AxonServerEnvironment` (the external address, or one lazily started `AxonServerContainer` with `withDevMode(true)`, `withDcbContext(true)`, `withReuse(true)`; `SharedAxonServerContainer` is not used).
- `:command-service` is a plain Kotlin library. `KotlinTagResolver` is the spike 002 resolver with KDoc, and `CommandServiceConfigurationEnhancer` registers `DelegatingGeneralConverter(JacksonConverter(coursesJsonMapper()))` and the resolver. It registered cleanly over Axon's defaults with no override-policy error.
- `ConditionalAppendIntegrationTest` has two tests. The first seeds a course, runs two units of work from a two-thread pool, holds both at a barrier until each has sourced `course=<A>`, lets both append a `StudentSubscribed`, asserts exactly one commits and the other's cause chain contains `AppendEventsTransactionRejectedException`, then re-sources the course and finds `CourseCreated` plus exactly one `StudentSubscribed`. The second does the same for two different courses and asserts both commit.
- Run against one running Axon Server through `-Pcourses.axonserver.external=localhost:18124`, the test passed twice in a row with no container started, which closes the idempotency edge.

## Task Commits

1. **Task 1: End-to-end conditional append on Axon Server (tracer)** - `8cae4ee` (feat)

The tracer feedback gate ran in its automated branch (auto mode, no `gate="blocking-human"`): `<verify>` was re-run end to end after the last change (`spotlessApply build`, then the integration test twice on the external server), passed, and there was no expansion task to start.

**Plan metadata:** committed with this SUMMARY (docs).

## Files Created/Modified

- `gradle/libs.versions.toml` - three image references, seven version-less library aliases
- `settings.gradle.kts` - includes `:tooling:test-support` and `:command-service` (`backend/command-service`)
- `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` - `useContainerImages()` on the component and integration test tasks
- `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ContainerImageArguments.kt` - the tracked-input argument provider
- `tooling/test-support/**` - `ContainerImages`, `AxonServerEnvironment`, build script
- `backend/command-service/build.gradle.kts` - dependencies of main, fixtures, unit and integration suites
- `backend/command-service/src/main/kotlin/.../KotlinTagResolver.kt`, `CommandServiceConfigurationEnhancer.kt` - the Axon wiring
- `backend/command-service/src/testFixtures/kotlin/.../CommandServiceTestApplication.kt` - Axon over Axon Server or memory
- `backend/command-service/src/integrationTest/**` - the INFRA-04 test and its `logback-test.xml`
- `backend/command-service/src/test/kotlin/.../KotlinTagResolverTest.kt` - unit test of the TypeID tags

## Decisions Made

See `key-decisions` above. In short: Docker Hub index digest for the Axon Server alias, Ryuk `0.14.0`, a non-blocking sourcing barrier, and per-unit outcomes asserted instead of caught.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added `KotlinTagResolverTest`**
- **Found during:** Task 1 (command-service wiring)
- **Issue:** the plan's truths require that the tags of `StudentSubscribed` are `course=<CourseId.toString()>` and `student=<StudentId.toString()>`, never raw UUIDs, but none of the 13 listed files tests the resolver in isolation. The integration test only catches a wrong tag indirectly, as a missing conflict.
- **Fix:** a four-test unit test in the `test` suite (both IDs tagged as TypeIDs, no raw UUID in any tag value, a creation tagged by course only, a payload without annotations has no tags), plus `testImplementation(testFixtures(project(":contracts-kernel")))` for `TestIds`.
- **Files modified:** `backend/command-service/src/test/kotlin/.../KotlinTagResolverTest.kt`, `backend/command-service/build.gradle.kts`
- **Verification:** `./gradlew build` runs it green.
- **Committed in:** `8cae4ee`

**2. [Rule 1 - Bug] Detekt findings in the new sources**
- **Found during:** Task 1, first full `./gradlew build`
- **Issue:** `MagicNumber` (a literal capacity in the integration test) and `OutdatedDocumentation` (a `@property` tag for a private constructor property of the test application) failed `:command-service:detekt`.
- **Fix:** a `CAPACITY` constant in the test and the tag removed.
- **Files modified:** `ConditionalAppendIntegrationTest.kt`, `CommandServiceTestApplication.kt`
- **Verification:** the second `spotlessApply build` was green.
- **Committed in:** `8cae4ee`

**3. [Rule 3 - Blocking] Kotlin nullability of `MessageStream.reduce` and `executeWithResult`**
- **Found during:** Task 1, compiling the integration test
- **Issue:** under `-Xjspecify-annotations=strict`, `reduce` passes the accumulator as nullable and `executeWithResult` could not infer its result type.
- **Fix:** explicit type arguments and `orEmpty()` on the accumulators. No behavior change.
- **Files modified:** `ConditionalAppendIntegrationTest.kt`
- **Committed in:** `8cae4ee`

---

**Total deviations:** 3 auto-fixed (1 missing critical, 1 bug, 1 blocking)
**Impact on plan:** none of them changes the plan's scope; the extra test is the only added file, and it pins a plan truth.

## Issues Encountered

- `docker.axoniq.io` answered the digest request with `HTTP 429` (Docker Hub anonymous rate limit behind it) and `401` without a token. The digest was resolved from the Docker Hub index of the same repository, and `docker inspect` of the started container confirms that `docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot@sha256:90a477bc...` is pulled and runs.
- The plan expects `grep -c 'SharedAxonServerContainer'` to print 0, which holds only because the KDoc says "the shared holder that `axoniq-testcontainer` ships" instead of naming the class.
- Plan 02-08 still owns the Qodana exclusion for `tooling/test-support/build` and `backend/command-service/build`, as the plan's split note says, so the `qodana` gate will show findings in generated code for the two new modules until then.

## Known Stubs

None.

## Deferred Items

- **Renovate does not track the catalog image lines.** The existing catalog manager matches `# renovate: datasource=... depName=...` followed by `<key> = "<version>"` and cannot read a `repo:tag@sha256:...` string, and `renovate.json5` is a governed path outside this plan's file list. A regex manager like the Qodana one (`depName`, `currentValue`, `currentDigest`, `datasource=docker`) belongs with the catalog in plan 02-08 or 02-14. Until then the three digests only change by hand.
- **Assumption A2 is not proven by a TestKit test.** The convention uses `CommandLineArgumentProvider` with `@Input` properties as the plan asks, but no functional test shows that a changed image invalidates the test task's cache. The first full build did execute `integrationTest` with the new inputs; the TestKit proof belongs with the plan that extends `TestSuitesFunctionalTest`.
- **`CommandServiceTestApplication.inMemory()` has no caller yet.** It is in the plan's fixture contract and compiles, but nothing runs on it until a unit test of the decision slices needs it.

## User Setup Required

None - no external service configuration required. Docker is the only requirement, as before.

## Next Phase Readiness

- Plan 02-08 can add the PostgreSQL singleton next to `AxonServerEnvironment`, reading `ContainerImages.postgres`, and then add the smoke and TestKit proofs, the Qodana paths and the `integration-test` CI job.
- Decision capture and retry (later plans) can register their decorators in `CommandServiceConfigurationEnhancer` and reuse `CommandServiceTestApplication` and the same barrier shape. Spike 001 found that concurrent tests must dispatch from a pool, which this test already does.
- No blockers.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*

## Self-Check: PASSED

- All 11 created files exist on disk; `8cae4ee` is reachable from HEAD.
- `ConditionalAppendIntegrationTest` result XML: `tests="2" skipped="0" failures="0"`.
- Acceptance greps: 3 catalog image lines with digest, 0 `SharedAxonServerContainer`, 1 `projectDir` line, 1 `jvmArgumentProviders` (at least 1 required), 1 `TESTCONTAINERS_RYUK_CONTAINER_IMAGE`, 0 image literals in test code.
- `mise x -- ./gradlew build --console=plain` is green after `spotlessApply`.
