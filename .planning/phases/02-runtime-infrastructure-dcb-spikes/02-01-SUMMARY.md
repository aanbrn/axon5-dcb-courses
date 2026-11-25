---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 01
subsystem: testing
tags: [gradle, jvm-test-suites, kotlin, typeid, uuidv7, kotest, junit5, detekt]

requires:
  - phase: 01-foundation-governance-build-conventions
    provides: "courses.kotlin-library convention, :platform alignment, version catalog, detekt/Dokka/Spotless gates"
provides:
  - "courses.test-suites convention plugin: test, componentTest, integrationTest and testFixtures per module, all wired into check"
  - ":contracts-kernel Gradle project at backend/contracts/kernel (flat name mapped by projectDir)"
  - "TypeId codec (spec v0.3.0), DecodedTypeId, TypedId, TypedIdCompanion, CourseId, StudentId"
  - "Catalog and :platform entries for java-uuid-generator, kotest-property, jackson-dataformat-yaml"
affects: [02-03, 02-04, 02-06, contracts, serialization, command-service, test-support, architecture]

plan_head_before: 9d5403637ab471533f7767dabf9b0ab5695be0a6
plan_head_after: c4d927b426f473a20c9ea985024753dfe45b6e20

actuals:
  tokens: 10000
  tasks: 2
  commits: 3

tech-stack:
  added: [java-uuid-generator 5.2.0, kotest-property (BOM-managed), jackson-dataformat-yaml (BOM-managed, test only)]
  patterns:
    - "One precompiled script plugin registers the three JVM test levels and wires every suite into check"
    - "Value-class entity IDs written as TypeIDs, with a shared TypedIdCompanion for of/random/parse/parseOrNull"
    - "Official spec vectors vendored byte for byte, with the pinned upstream commit recorded in REUSE.toml"

key-files:
  created:
    - build-logic/src/main/kotlin/courses.test-suites.gradle.kts
    - backend/contracts/kernel/build.gradle.kts
    - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeId.kt
    - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/TypedId.kt
    - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/Ids.kt
    - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdVectorsTest.kt
    - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypedIdTest.kt
    - backend/contracts/kernel/src/test/resources/typeid/valid.yml
    - backend/contracts/kernel/src/test/resources/typeid/invalid.yml
  modified:
    - gradle/libs.versions.toml
    - platform/build.gradle.kts
    - settings.gradle.kts
    - REUSE.toml

key-decisions:
  - "Entity ID prefixes are file-level private constants in Ids.kt because a companion object cannot read its own members in its super constructor call (INSTANCE_ACCESS_BEFORE_SUPER_CALL)"
  - "TypeId.decode reports a prefix-less suffix as a valid TypeID, so CourseId.parse of a bare suffix fails with the wrong-prefix message (expected 'course', got ''), not the malformed message"
  - "The 128-bit codec shifts a two-long accumulator five bits per character instead of the spike's per-bit loop; the first-character limit of 7 guarantees nothing is shifted out"
  - "parseOrNull catches IllegalArgumentException only, never Throwable, so programming errors are not swallowed"

patterns-established:
  - "Test classes carry a one-line KDoc, use prose test names and block bodies; property tests use runBlocking<Unit> so JUnit does not skip them"
  - "A convention file's explanatory comment goes below the plugins block: Spotless's licence-header step replaces any comment above the first statement"

requirements-completed: [TEST-11, TEST-14, IDEM-04, INFRA-02]

coverage:
  - id: D1
    description: "courses.test-suites registers componentTest and integrationTest next to test, and check depends on all three (D-15, D-19)"
    requirement: TEST-11
    verification:
      - kind: integration
        ref: "./gradlew :contracts-kernel:check --dry-run lists :contracts-kernel:test, componentTest and integrationTest (count 3)"
        status: pass
    human_judgment: false
  - id: D2
    description: "componentTest and integrationTest run on one fork and after the preceding suite; test keeps the multi-fork default (D-22)"
    requirement: TEST-11
    verification: []
    human_judgment: true
    rationale: "No test asserts maxParallelForks or shouldRunAfter yet; the TestKit proof of the convention is plan 02-04"
  - id: D3
    description: "Every suite gets JUnit Jupiter at the catalog version, the module, its testFixtures, Kotest assertions and kotest-property (D-21, TEST-14)"
    requirement: TEST-14
    verification:
      - kind: unit
        ref: "backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypedIdTest.kt#parsing the string form returns the same ID for any UUID (kotest-property, runs in the test suite)"
        status: pass
    human_judgment: false
  - id: D4
    description: "detekt analyses the componentTest, integrationTest and testFixtures source directories (RESEARCH Pitfall 2)"
    requirement: TEST-11
    verification:
      - kind: other
        ref: "Temporary undocumented function in src/componentTest made :contracts-kernel:detekt fail with UndocumentedPublicFunction; the probe was removed"
        status: pass
    human_judgment: false
  - id: D5
    description: "All 9 official valid TypeID vectors round-trip and all 21 invalid vectors are rejected with IllegalArgumentException (D-07)"
    requirement: IDEM-04
    verification:
      - kind: unit
        ref: "backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdVectorsTest.kt (2 tests, counts asserted 9 and 21)"
        status: pass
    human_judgment: false
  - id: D6
    description: "CourseId and StudentId: parse and parseOrNull API, wrong-prefix message, canonical toString, equality, UUIDv7 generation, edge cases (empty, suffix-only, upper case, overflow) (D-06, D-09)"
    requirement: IDEM-04
    verification:
      - kind: unit
        ref: "backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypedIdTest.kt (13 tests)"
        status: pass
    human_judgment: false
  - id: D7
    description: "Flat :contracts-kernel project at backend/contracts/kernel; settings.gradle.kts documents the backend layout (D-01)"
    requirement: INFRA-02
    verification:
      - kind: other
        ref: "grep -c 'project(\":contracts-kernel\").projectDir = file(\"backend/contracts/kernel\")' settings.gradle.kts prints 1; :contracts-kernel:check green"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 01: Contracts Kernel and Test-Suites Convention Summary

**First real Kotlin module `:contracts-kernel`, built on a new `courses.test-suites` convention (unit, component and integration suites all in `check`), carrying a TypeID v0.3.0 codec proven by the 30 official vectors and the `CourseId`/`StudentId` value classes with `parse`/`parseOrNull` and UUIDv7 generation.**

## Performance

- **Duration:** 25 min (including a 5.5 min `build-logic` regression run)
- **Started:** 2026-11-23T00:55:00Z
- **Completed:** 2026-11-23T01:21:00Z
- **Tasks:** 2 (one tracer, one TDD task)
- **Files modified:** 13 (9 created in the kernel and convention, 4 modified)

## Accomplishments

- `courses.test-suites` registers `componentTest` and `integrationTest` next to `test`, gives each suite JUnit Jupiter at the catalog version, the module, its `testFixtures`, Kotest assertions and `kotest-property`, pins the two infrastructure suites to one fork and `shouldRunAfter` order, wires all three into `check` and widens detekt to the suite and `testFixtures` sources.
- `:contracts-kernel` builds, passes detekt, Dokka `failOnWarning`, Spotless, `checkBuildScripts` and `reuse lint`, and runs 15 unit tests under `check`.
- The TypeID codec is a strict spec v0.3.0 implementation, checked against all 9 valid and 21 invalid official vectors (counts asserted, so an empty resource cannot pass).
- `CourseId` and `StudentId` expose `of`, `random` (UUIDv7, one shared generator), `parse` (wrong prefix names the expected and actual prefix) and `parseOrNull`, with tests for the empty, suffix-only, upper-case, overflow and equality edges and one kotest-property round-trip over arbitrary UUIDs.

## Task Commits

1. **Task 1: End-to-end `:contracts-kernel` through `courses.test-suites`** - `995601f` (feat, tracer)
2. **Task 2: ID value classes (TDD)**
   - RED: `d7cd058` (test)
   - GREEN: `c4d927b` (feat)
   - REFACTOR: none needed

**Plan metadata:** committed with this SUMMARY (docs).

## TDD Gate Compliance

- **RED (`d7cd058`):** `TypedIdTest` (12 tests at that commit) was committed before any implementation. Honest assessment: the run failed at compilation (`Unresolved reference 'CourseId'`, `'StudentId'`), not on a planned assertion, so under the strict `tdd.md` classifier it would be `build_failure` rather than `RED_EVIDENCE_OK`. The plan is `type: execute` and `workflow.tdd_mode` is off, so no classifier ran; the ID types did not exist at all, which makes a compile-level RED the only RED available without writing throwaway stubs.
- **GREEN (`c4d927b`):** all tests pass after implementation. One test expectation was corrected in the same commit (see Deviations).
- **REFACTOR:** not needed.

## Files Created/Modified

- `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` - the convention: three suites, `check` wiring, detekt reach
- `settings.gradle.kts` - includes `:contracts-kernel`, maps it to `backend/contracts/kernel`, documents the backend layout
- `gradle/libs.versions.toml` - `kotest-property`, `jackson-dataformat-yaml`, `java-uuid-generator` (5.2.0)
- `platform/build.gradle.kts` - `api(libs.java.uuid.generator)` constraint
- `REUSE.toml` - Apache-2.0 override annotation for the vendored vectors, naming the upstream commit
- `backend/contracts/kernel/build.gradle.kts` - applies the convention
- `.../kernel/TypeId.kt` - codec and `DecodedTypeId`
- `.../kernel/TypedId.kt` - `TypedId`, `TypedIdCompanion`
- `.../kernel/Ids.kt` - `CourseId`, `StudentId`
- `.../kernel/TypeIdVectorsTest.kt`, `TypedIdTest.kt` - 2 + 13 tests
- `backend/contracts/kernel/src/test/resources/typeid/{valid,invalid}.yml` - official vectors, byte-identical to spike 002

## Decisions Made

See `key-decisions` above. The notable one for later plans: the ID prefixes are private file-level constants because Kotlin forbids a companion reading its own members in its super constructor call.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Test expectation for the suffix-only input was wrong**
- **Found during:** Task 2 (GREEN run)
- **Issue:** The first version of `malformed text is rejected as a malformed TypeID` included `01h455vb4pex5vsknk084sn02q`, but a bare suffix is a valid prefix-less TypeID, so `CourseId.parse` rejects it with the wrong-prefix message, not `Malformed TypeID '`. The plan's behavior list only requires `IllegalArgumentException` for it.
- **Fix:** Moved the suffix-only case into its own test asserting `expected 'course', got ''`.
- **Files modified:** `TypedIdTest.kt`
- **Verification:** 13 tests, 0 failures
- **Committed in:** `c4d927b`

**2. [Rule 3 - Blocking] `ktfmt`/Spotless layout forced two cosmetic changes**
- **Found during:** Task 1 (`spotlessCheck`)
- **Issue:** Spotless's licence-header step treats everything above the first statement as the old header, so the file-level comment I wrote at the top of `courses.test-suites.gradle.kts` was deleted by `spotlessApply`. ktfmt also wrapped `project(...).projectDir = file(...)` when the layout comment followed it, which would have failed the plan's one-line acceptance grep.
- **Fix:** Moved the convention's explanatory comment below the `plugins` block, and moved the layout comment above `include(...)` in `settings.gradle.kts`.
- **Files modified:** `courses.test-suites.gradle.kts`, `settings.gradle.kts`
- **Verification:** `spotlessCheck` green; acceptance grep prints 1
- **Committed in:** `995601f`

---

**Total deviations:** 2 auto-fixed (1 bug in a test expectation, 1 blocking formatter interaction)
**Impact on plan:** No scope change; the plan's one-line settings acceptance criterion and the header comment content are both met.

## Issues Encountered

- The sandbox refuses any Bash command whose text contains the substring `git` inside a longer word (for example the `io/github/...` package path) combined with other tooling; commands were split or rewritten with globs.
- Prettier left the vendored vectors unchanged (they were already in its format), so no Spotless exclusion for `typeid/*.yml` was needed. Plan 02-04 should still keep that in mind when the vectors are re-synced.

## Timing (Nyquist)

| Command | Duration |
|---------|----------|
| `./gradlew :contracts-kernel:check` cold (build-logic compile, first configuration) | 32 s |
| `./gradlew :contracts-kernel:check` warm | 2 s |
| `./gradlew :contracts-kernel:test --tests '*TypedIdTest*' --rerun` warm | 2 s |
| `./gradlew :contracts-kernel:check --dry-run` | 1 s |
| `./gradlew -p build-logic check` (regression run, includes TestKit functional tests) | 5 min 33 s |

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or file access; the new libraries resolve through `:platform` and the catalog (T-02-01), the vectors are pinned in REUSE.toml with counts asserted (T-02-02), and the fail-closed codec has all 21 invalid vectors plus the edge cases as tests (T-02-03).

## User Setup Required

None - no external service configuration required.

## Flagged assumption carried forward

INFRA-02 (unclassified edge in the plan): "contracts live in dedicated modules" is met for the kernel here; `:contracts-events`, `:contracts-commands` and `:contracts-queries` land in plans 02-03 and 02-06. Not resolved by this plan.

## Next Phase Readiness

- Plan 02-03 can add the tag registry and message namespace to the kernel and retire the sample module.
- Plan 02-04 must still add the detekt excludes (`UndocumentedPublicFunction`, `UndocumentedPublicProperty`, `FunctionNaming`) for `**/componentTest/**` and `**/integrationTest/**`, and the TestKit proof of the convention (three tasks, `check` depends on all, a failing `integrationTest` fails `check`). Until then a suite with prose test names fails detekt.
- Any later `@Test` that returns a non-`Unit` value is silently skipped (RESEARCH Pitfall 3); use `runBlocking<Unit>` or a block body.

## Self-Check: PASSED

- Created files verified present on disk: the convention, the kernel build file, `TypeId.kt`, `TypedId.kt`, `Ids.kt`, both tests and both vector files.
- Commits `995601f`, `d7cd058` and `c4d927b` are ancestors of HEAD.
- `:contracts-kernel:check`, `spotlessCheck`, `checkBuildScripts`, `reuse lint` and `-p build-logic check` all green.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
