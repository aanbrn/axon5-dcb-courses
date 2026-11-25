---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 04
subsystem: testing
tags: [gradle, test-fixtures, kotest-property, testkit, kover, detekt, uuidv7]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "courses.test-suites convention and the :contracts-kernel module with CourseId, StudentId and the TypeID codec (plan 02-01)"
provides:
  - "Kernel testFixtures: TestIds, Arb.courseId()/studentId() and ClockedUuidV7Generator"
  - "TypeIdPropertyTest: property and concurrency tests of the ID codec that demonstrably run"
  - "TestSuitesFunctionalTest and GradleFixture.testSuites/addTest: TestKit proof of the courses.test-suites contract"
  - "Kover in courses.test-suites: every module reports coverage over test, componentTest and integrationTest"
  - "detekt excludes for suite test names (componentTest, integrationTest, architectureTest) and TooManyFunctions"
affects: [02-06, 02-07, 02-11, phase-03-coverage-aggregation, contracts, test-support]

plan_head_before: 95238f6c611991d406d7a956d95a89a5a71eece3
plan_head_after: 08ea913e62adcd8d2599f114b6ae1c528052b134

actuals:
  tokens: 6600
  tasks: 3
  commits: 5

tech-stack:
  added: [kover-gradle-plugin 0.9.11]
  patterns:
    - "Test helpers per module through java-test-fixtures, consumed by every suite via the convention"
    - "Convention behavior pinned by TestKit tests that run under the configuration cache and isolated projects"
    - "Coverage XML parsed by the test instead of grepped"

key-files:
  created:
    - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/TestIds.kt
    - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/IdArbs.kt
    - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/ClockedUuidV7Generator.kt
    - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdPropertyTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/TestSuitesFunctionalTest.kt
  modified:
    - backend/contracts/kernel/build.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
    - build-logic/src/main/kotlin/courses.test-suites.gradle.kts
    - build-logic/build.gradle.kts
    - config/detekt/detekt.yml
    - gradle/libs.versions.toml

key-decisions:
  - "Suite order uses mustRunAfter, not shouldRunAfter: TestKit showed Gradle running integrationTest before test and componentTest in a check build with the soft form"
  - "Kover is applied inside courses.test-suites with no test-task exclusion and no class filter; the aggregated report, thresholds and CI reporting stay with TEST-08 in Phase 3"
  - "TooManyFunctions is excluded for the same test source sets as the KDoc rules, because a test class grows with the behaviors it proves"
  - "GradleFixture.addTest is a top-level extension function so the class stays under the detekt function limit"

patterns-established:
  - "A fixture test that must prove a source set is analysed adds the source set to detekt in the fixture build file"
  - "TestKit tests that need a task to really execute pass --no-build-cache (the test JVM's Gradle home build cache would answer FROM_CACHE)"

requirements-completed: [TEST-11, TEST-12, TEST-14, IDEM-04]

coverage:
  - id: D1
    description: "Kernel testFixtures provide readable fixed IDs, Arb generators and a Clock-based UUIDv7 generator (TEST-12, D-11, D-18)"
    requirement: TEST-12
    verification:
      - kind: unit
        ref: "backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdPropertyTest.kt (7 tests, result XML tests=7 failures=0)"
        status: pass
    human_judgment: false
  - id: D2
    description: "The property test is executed, not skipped: block bodies with runBlocking<Unit>, non-zero test count in the result XML (Pitfall 3, T-02-08)"
    requirement: TEST-12
    verification:
      - kind: unit
        ref: "build/test-results/test/TEST-io.github.aanbrn.courses.contracts.kernel.TypeIdPropertyTest.xml reports tests=7 skipped=0"
        status: pass
    human_judgment: false
  - id: D3
    description: "IDEM-04 edges: 100,000 CourseId.random() from eight threads are pairwise distinct; round trip holds for arbitrary UUIDs and for 100,000 seeded UUIDs"
    requirement: IDEM-04
    verification:
      - kind: unit
        ref: "TypeIdPropertyTest#one hundred thousand random course ids from eight threads are pairwise distinct, #every generated course id survives a TypeID round trip, #one hundred thousand seeded random UUIDs survive a round trip"
        status: pass
    human_judgment: false
  - id: D4
    description: "courses.test-suites contract under configuration cache and isolated projects: three suite tasks in check, failing integrationTest fails check, fixtures reachable from every suite, classpaths, NO-SOURCE, order, single fork (TEST-11, TEST-14, T-02-09)"
    requirement: TEST-11
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/TestSuitesFunctionalTest.kt (9 wiring and detekt tests)"
        status: pass
    human_judgment: false
  - id: D5
    description: "detekt reaches componentTest, integrationTest and testFixtures; prose test names without KDoc pass in the suites (and in architectureTest), an undocumented public function in testFixtures fails (Pitfall 2)"
    requirement: TEST-11
    verification:
      - kind: integration
        ref: "TestSuitesFunctionalTest#prose test names without KDoc pass detekt in the component, integration and architecture suites; #an undocumented public function in testFixtures fails detekt"
        status: pass
    human_judgment: false
  - id: D6
    description: "All three suites count toward coverage: code reached by one suite only is covered in the Kover XML, code reached by none is uncovered, a second run keeps integrationTest up to date (TEST-11, T-02-53)"
    requirement: TEST-11
    verification:
      - kind: integration
        ref: "TestSuitesFunctionalTest#code reached by each suite is reported covered and code reached by none is reported uncovered (parses report.xml); result XML tests=12 failures=0"
        status: pass
      - kind: other
        ref: "./gradlew :contracts-kernel:koverXmlReport writes backend/contracts/kernel/build/reports/kover/report.xml and it lists contracts/kernel/TypeId"
        status: pass
    human_judgment: false
---

# Phase 2 Plan 04: Kernel Test Fixtures, Suite Convention Proof and Per-Module Coverage Summary

**The kernel now publishes `testFixtures` (fixed `TestIds`, kotest `Arb` ID generators, a `Clock`-driven UUIDv7 generator) used by a property test that provably runs, a 12-test TestKit suite pins the `courses.test-suites` contract under the configuration cache and isolated projects, and Kover in that convention reports coverage over all three suites for every module.**

## Performance

- **Duration:** about 75 min (dominated by TestKit runs of 50 to 90 s each and three 3 to 4 min full builds)
- **Tasks:** 3 (one tracer, two TDD)
- **Files modified:** 11 (5 created, 6 modified)
- **Commits:** 5 (1 tracer, RED and GREEN for task 2, RED and GREEN for task 3)

## Accomplishments

- `TestIds.courseId(n)`/`studentId(n)` return stable UUIDv7-shaped IDs (`course_01hw000000e008000000000001` for `n = 1`), `Arb.courseId()`/`studentId()` give property tests arbitrary IDs, and `ClockedUuidV7Generator(clock, random)` produces strictly increasing UUIDv7 values from a fixed clock.
- `TypeIdPropertyTest` (7 tests) covers the round trip over arbitrary IDs, `parseOrNull` on arbitrary text, 100,000 seeded UUIDs, the UUIDv7 version bit, monotonic clocked IDs, 100,000 IDs from eight threads being pairwise distinct, and the readable test ID contract. Its result XML reports `tests="7" skipped="0" failures="0"`.
- `TestSuitesFunctionalTest` (12 tests) proves: `check` depends on `test`, `componentTest` and `integrationTest`; a test in each suite reads a `testFixtures` class; a failing `integrationTest` fails `check`; every suite runtime classpath carries JUnit Jupiter, Kotest assertions and `kotest-property`; an unused suite is `NO-SOURCE`; the suites run in order; `componentTest` and `integrationTest` use one fork; detekt exempts suite test names (including an `architectureTest` source set added to detekt in the fixture) but still fails an undocumented `testFixtures` function; Kover writes a parseable XML report with per-suite coverage and keeps `integrationTest` up to date.
- `courses.test-suites` applies Kover (`0.9.11`, the newest on Maven Central, checked on 2026-11-23). No suite is excluded from instrumentation and no filter is set.

## Task Commits

1. **Task 1: Kernel fixtures and property test (tracer)** - `adf54e3` (feat)
2. **Task 2: TestKit proof of `courses.test-suites`**
   - RED: `ff607a7` (test)
   - GREEN: `8a32181` (feat)
3. **Task 3: Kover over all three suites**
   - RED: `9df2889` (test)
   - GREEN: `08ea913` (feat)

**Plan metadata:** committed with this SUMMARY (docs).

## TDD Gate Compliance

- **Task 2 RED (`ff607a7`):** the tests were first run before the detekt excludes existed. Five tests failed on `UndocumentedPublicFunction` and `FunctionNaming` for the suite test names, as intended. The same run also showed two defects in my own fixture sources (`FunctionOnlyReturningConstant`, then `MagicNumber`), which I fixed before the RED commit and the first GREEN run. The committed RED state therefore fails on the expected rules but was not re-run in isolation after the fixture fix.
- **Task 2 GREEN (`8a32181`):** 9 of 9 tests pass after the detekt excludes and the `mustRunAfter` fix (see Deviations).
- **Task 3 RED (`9df2889`):** three Kover tests failed with `Cannot locate tasks that match ':lib:koverXmlReport'`, the exact message of the missing capability.
- **Task 3 GREEN (`08ea913`):** 12 of 12 tests pass.
- **REFACTOR:** none.

## Decisions Made

See `key-decisions` in the frontmatter. The one later plans must know: suite order is `mustRunAfter`, and Kover is already applied by the convention, so a module needs no coverage configuration to appear in `koverXmlReport`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Suite ordering in `courses.test-suites` was not honoured**
- **Found during:** Task 2 (ordering test)
- **Issue:** with `shouldRunAfter`, a `:lib:check` build executed `integrationTest` before `test` and `componentTest`. The suites iterate in name order, so the soft hint lost.
- **Fix:** `mustRunAfter` for both suites; it adds no dependency, so running one suite alone still runs only that suite. The convention comment explains why.
- **Files modified:** `build-logic/src/main/kotlin/courses.test-suites.gradle.kts`
- **Verification:** `within a module the suites run as test, componentTest and integrationTest` passes
- **Commit:** `8a32181`

**2. [Rule 3 - Blocking] detekt rules tripped on the new sources**
- **Found during:** Task 2 and the final `./gradlew build` of Task 3
- **Issue:** the fixture sources hit `FunctionOnlyReturningConstant` and `MagicNumber`; after Task 3 `:build-logic:detekt` failed with `TooManyFunctions` on `GradleFixture` (12 functions) and `TestSuitesFunctionalTest` (17) and with `MaxLineLength` on a string literal.
- **Fix:** non-constant fixture functions; `addTest` became a top-level extension function; the long literal was split; `TooManyFunctions` is excluded for the same test source sets as the KDoc rules (a test class grows with its behaviors). The plan's `files_modified` lists `config/detekt/detekt.yml` for the other excludes, so this stays inside the plan's files.
- **Files modified:** `GradleFixture.kt`, `TestSuitesFunctionalTest.kt`, `config/detekt/detekt.yml`
- **Verification:** `./gradlew build` green
- **Commits:** `8a32181`, `08ea913`

---

**Total deviations:** 2 auto-fixed (1 bug in the convention, 1 blocking detekt interaction)
**Impact on plan:** No scope change. The ordering fix strengthens the D-22 behavior the plan asks the TestKit test to assert.

## Issues Encountered

- The sandbox refuses Bash commands that contain the substring `github` (the package path `io/github/aanbrn`) together with heredocs or shell chains, so the sources were written with the Write and Edit tools and the Gradle runs were kept to plain commands.
- The `GradleFixture` KDoc previously claimed test names pass detekt in every suite; it now says what the fixture really writes.
- BSD `sed` refused two combined substitutions; edits were made with the Edit tool instead.

## Kover Notes

- Kover is a Gradle plugin and its JVM agent, not a TEST-14 test library: it adds nothing to any suite's `implementation` or `runtimeOnly` configuration, so D-21 is untouched.
- The plugin worked with the configuration cache and isolated projects on, with no workaround. No upstream-record candidate arose.
- Per-module tasks are `koverXmlReport` and `koverHtmlReport`; the report is `build/reports/kover/report.xml`. The aggregated cross-module report, thresholds, exclusions and CI reporting remain TEST-08 and the CICD-01 coverage clause in Phase 3.

## Timing (Nyquist)

| Command | Duration |
|---------|----------|
| `./gradlew :contracts-kernel:check` | 1 s warm |
| `./gradlew :build-logic:functionalTest --tests '*TestSuitesFunctionalTest*'` (9 tests) | 49 to 64 s |
| `./gradlew :build-logic:functionalTest --tests '*TestSuitesFunctionalTest*'` (12 tests) | 53 to 93 s |
| `./gradlew :contracts-kernel:koverXmlReport` | 7 s |
| `./gradlew build` (full, includes the whole TestKit suite) | 3 min 20 s |

## Known Stubs

None.

## Threat Flags

None. The Kover plugin is pinned in the catalog (T-02-54), the report and the property tests have counted assertions (T-02-08, T-02-53), and the convention has a TestKit test under the configuration cache and isolated projects (T-02-09).

## Flagged assumptions carried forward

- TEST-12: helpers per module through `testFixtures` (D-18); infrastructure singletons live in `:tooling:test-support` (plan 02-07). Not resolved here.
- TEST-14: the standard toolchain for Phase 2 is JUnit 6, Kotest assertions, `kotest-property` and Testcontainers; MockK, `AxonTestFixture` and Awaitility arrive with the first tests that need them (D-21). Not resolved here.
- TEST-11 coverage: delivered per module; aggregation, thresholds and CI reporting are Phase 3.

## Next Phase Readiness

- Later modules get unit, component and integration coverage by applying `courses.test-suites`; Phase 3 only aggregates and sets thresholds.
- Plan 02-11 can rely on `architectureTest` being excluded from the KDoc and naming rules, but it must add `src/architectureTest/kotlin` to detekt's sources when it registers that suite (the convention does not register it).
- The architecture rule of plan 02-11 that forbids expression-bodied tests (T-02-08) is still to come.

## Self-Check: PASSED

- Created files verified present on disk: the three `testFixtures` sources, `TypeIdPropertyTest.kt`, `TestSuitesFunctionalTest.kt`.
- Commits `adf54e3`, `ff607a7`, `8a32181`, `9df2889` and `08ea913` are ancestors of HEAD.
- `:contracts-kernel:check`, `:contracts-kernel:koverXmlReport` and the full `./gradlew build` are green; the result XMLs report `tests="7" failures="0"` and `tests="12" failures="0"`.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
