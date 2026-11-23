---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 03
subsystem: contracts
tags: [kotlin, dcb, tags, rejection, kernel, gradle, qodana, gradle-profiler]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: ":contracts-kernel module, TypeId codec, CourseId and StudentId with PREFIX constants, courses.test-suites convention (plan 02-01)"
provides:
  - "Tags registry: Tags.COURSE and Tags.STUDENT as const vals equal to the TypeID prefixes"
  - "MessageNamespace.COURSES = \"courses\""
  - "Rejection interface and RejectionCode value class (lower-case kebab-case only)"
  - "Repository without the Phase 1 sample module; the kernel is the only Kotlin module"
affects: [02-06, 02-11, contracts-events, contracts-commands, command-service, architecture]

plan_head_before: 95238f6c611991d406d7a956d95a89a5a71eece3
plan_head_after: f205ba6b4e4fbe247b2977958cdfcbcc1f82e457

actuals:
  tokens: 6500
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Tag keys are const vals derived from the entity TypeID prefixes, so a key cannot drift from its ID"
    - "Business rejections are values: a per-command Rejected case implements Rejection and names the rule by a stable RejectionCode"

key-files:
  created:
    - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/Tags.kt
    - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/MessageNamespace.kt
    - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/Rejection.kt
    - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/KernelContractTest.kt
  modified:
    - settings.gradle.kts
    - qodana.yaml
    - performance.scenarios
    - docs/performance.md
    - scripts/governance/test-check-pr.sh
    - build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
  deleted:
    - tooling/convention-sample/build.gradle.kts
    - tooling/convention-sample/src/main/kotlin/io/github/aanbrn/courses/sample/Greeting.kt
    - tooling/convention-sample/src/test/kotlin/io/github/aanbrn/courses/sample/GreetingTest.kt

key-decisions:
  - "courses.kotlin-library no longer declares junit-jupiter, junit-platform-launcher or kotest-assertions-core: courses.test-suites supplies all three to every suite, and no project or TestKit fixture that applies courses.kotlin-library alone declares tests"
  - "The abi_change benchmark targets the kernel's TypeId.kt, the ABI-visible file with the most fan-out in the only Kotlin module"

patterns-established:
  - "A new module adds its build directory to the Qodana All exclusion (docs/tooling.md); done here for backend/contracts/kernel/build"

requirements-completed: [INFRA-02, TEST-14]

coverage:
  - id: D1
    description: "Tags.COURSE and Tags.STUDENT are const vals equal to the TypeID prefixes; MessageNamespace.COURSES is \"courses\" (D-03, D-10)"
    requirement: INFRA-02
    verification:
      - kind: unit
        ref: "backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/KernelContractTest.kt#the course tag key is the course TypeID prefix"
        status: pass
    human_judgment: false
  - id: D2
    description: "RejectionCode accepts only lower-case kebab-case; empty, upper-case, spaced, leading-hyphen, doubled-hyphen and trailing-hyphen codes throw IllegalArgumentException; Rejection exposes the code (D-05)"
    requirement: INFRA-02
    verification:
      - kind: unit
        ref: "backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/KernelContractTest.kt (10 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The sample module is gone and no build file, Qodana config, gradle-profiler scenario or governance test names it; the governance boundary cases are repointed, not deleted (Phase 1 D-22)"
    requirement: INFRA-02
    verification:
      - kind: integration
        ref: "bash scripts/governance/test-check-pr.sh (132 passed, 0 failed); ./gradlew build green; grep for convention-sample in the five files prints 0"
        status: pass
    human_judgment: false
  - id: D4
    description: "JUnit 6 stays the only runner after courses.kotlin-library drops its explicit test dependencies (TEST-14)"
    requirement: TEST-14
    verification:
      - kind: integration
        ref: "./gradlew :contracts-kernel:dependencies --configuration testRuntimeClasspath still lists junit-platform-launcher 6.1.3 and junit-jupiter 6.1.3; ./gradlew build (including the build-logic TestKit suite) green"
        status: pass
    human_judgment: false
  - id: D5
    description: "docs/performance.md abi_change row re-measured on contracts-kernel"
    verification:
      - kind: other
        ref: "gradle-profiler --benchmark abi_change: 5 measured builds, median 448 ms"
        status: pass
    human_judgment: false

duration: 21min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 03: Kernel Contract Surface and Sample Retirement Summary

**The kernel gains the DCB tag registry (`Tags`), the `courses` message namespace and the `Rejection`/`RejectionCode` contract (lower-case kebab-case, enforced at construction), and the Phase 1 `convention-sample` module is retired with every reference repointed to the kernel.**

## Performance

- **Duration:** 21 min (of which about 5.5 min is the full `./gradlew build` including the `build-logic` TestKit suite, and about 3 min is the gradle-profiler run)
- **Started:** 2026-11-23T01:45:00Z
- **Completed:** 2026-11-23T02:07:00Z
- **Tasks:** 2 (one TDD, one chore)
- **Files modified:** 13 (4 created, 6 modified, 3 deleted)

## Accomplishments

- `Tags`, `MessageNamespace`, `Rejection` and `RejectionCode` are in the kernel with KDoc on every public declaration. `Tags.COURSE` and `Tags.STUDENT` are `const val`s initialised from the ID prefixes, so a tag key cannot drift from its TypeID prefix.
- `RejectionCode` validates `^[a-z][a-z0-9]*(-[a-z0-9]+)*$` in `init`; 10 tests cover the constants, the valid forms, the empty string and the four malformed shapes, and a `Rejection` implementation.
- `tooling/convention-sample` is removed and `settings.gradle.kts`, `qodana.yaml`, `performance.scenarios`, `docs/performance.md` and the governance shell tests no longer name it. The kernel's build directory is in the Qodana `All` exclusion.
- The `abi_change` benchmark row is re-measured on `contracts-kernel`: median 448 ms over 5 measured builds (401 to 558 ms), against 278 ms for the old, smaller sample.
- `courses.kotlin-library` no longer carries its own JUnit and Kotest test dependencies (see Decisions Made).

## Task Commits

1. **Task 1: Kernel contracts (TDD)**
   - RED: `89bfdcf` (test)
   - GREEN: `3d47c9f` (feat)
   - REFACTOR: none needed
2. **Task 2: Retire the sample module, repoint references, settle JUnit dependencies** - `f205ba6` (chore). The `abi_change` row was re-measured in this commit.

**Plan metadata:** committed with this SUMMARY (docs).

## TDD Gate Compliance

- **RED (`89bfdcf`):** `KernelContractTest` was committed before any implementation. Honest assessment, as in plan 02-01: the run failed at compilation (`Unresolved reference 'Tags'`), not on a planned assertion, so under the strict `tdd.md` classifier it would be `build_failure`, not `RED_EVIDENCE_OK`. The plan is `type: execute` and `workflow.tdd_mode` is off, so no classifier ran. The types did not exist, and a compile-level RED is the only RED available without throwaway stubs.
- **GREEN (`3d47c9f`):** all 10 tests pass, `:contracts-kernel:check` (detekt, Dokka `failOnWarning`) and `spotlessCheck` green.
- **REFACTOR:** not needed.

## Files Created/Modified

- `.../kernel/Tags.kt` - tag registry, `const val`s equal to the TypeID prefixes
- `.../kernel/MessageNamespace.kt` - `MessageNamespace.COURSES`
- `.../kernel/Rejection.kt` - `Rejection` interface and `RejectionCode` value class
- `.../kernel/KernelContractTest.kt` - 10 unit tests
- `settings.gradle.kts` - sample include removed
- `qodana.yaml` - `tooling/convention-sample/build` replaced by `backend/contracts/kernel/build`
- `performance.scenarios` - `abi_change` targets `TypeId.kt`
- `docs/performance.md` - scenario description and the `abi_change` baseline row
- `scripts/governance/test-check-pr.sh` - two boundary cases repointed to `backend/contracts/kernel/{build.gradle.kts,mise.toml}`
- `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` - redundant test dependencies and the unused `libs` accessor removed
- `tooling/convention-sample/**` - deleted (3 files)

## Decisions Made

- **JUnit dependencies left `courses.kotlin-library`.** Evidence for the plan's two conditions:
  1. `./gradlew :contracts-kernel:dependencies --configuration testRuntimeClasspath` still lists `org.junit.platform:junit-platform-launcher -> 6.1.3` as a top-level dependency and `junit-jupiter:6.1.3`, now supplied by `useJUnitJupiter(libs.versions.junit)` in `courses.test-suites`. `kotest-assertions-core 6.2.7` is still on the test classpaths through the suites' `implementation`.
  2. No TestKit fixture that applies `courses.kotlin-library` without `courses.test-suites` declares tests: `GradleFixture.standard` (`:lib`) has only a main source, `GradleFixture.governance` applies `courses.governance` only, and `BuildScriptLintCheckFunctionalTest` writes a build script but no test sources. `./gradlew build`, which runs the `build-logic` TestKit suite, is green.
  JUnit 6 remains the only runner (TEST-14). The consequence for later modules: a module that writes tests must apply `courses.test-suites`, not `courses.kotlin-library` alone.
- **The `libs` accessor was removed from `courses.kotlin-library`** with the dependencies block, because nothing else in that file used it.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `docs/performance.md` named the sample in a second place**
- **Found during:** Task 2 (grep for remaining references)
- **Issue:** The scenario table at line 29 described the `abi_change` scenario as "An API change to `Greeting.kt`", which the plan's step 4 (only the baseline row) did not list, and which would have left a stale reference to a deleted file.
- **Fix:** Reworded the description to name the kernel's `TypeId.kt`.
- **Files modified:** `docs/performance.md`
- **Verification:** the plan's grep for `convention-sample` in the five files prints 0; no `Greeting` reference remains outside `docs/adr` and `.planning`
- **Committed in:** `f205ba6`

**2. [Rule 1 - Bug] Formatter changed the line wrapping of two KDoc blocks**
- **Found during:** Task 1 (`spotlessCheck` after GREEN)
- **Issue:** ktfmt re-wrapped the KDoc of `MessageNamespace` and `Rejection` to its line width, which failed `spotlessCheck`.
- **Fix:** `spotlessApply`; no content change.
- **Files modified:** `MessageNamespace.kt`, `Rejection.kt`
- **Verification:** `:contracts-kernel:check spotlessCheck` green
- **Committed in:** `3d47c9f`

**3. [Plan literal] Constants carry no explicit type**
- **Found during:** Task 1 (acceptance criteria)
- **Issue:** the acceptance greps need `const val COURSE = CourseId.PREFIX` and `const val COURSES = "courses"` verbatim, so the first draft's `: String` annotations were dropped. detekt and Dokka accept both forms.
- **Files modified:** `Tags.kt`, `MessageNamespace.kt`
- **Committed in:** `3d47c9f`

---

**Total deviations:** 3 auto-fixed (1 blocking, 1 formatter, 1 plan-literal alignment)
**Impact on plan:** None. All three keep the plan's acceptance criteria satisfiable and add no scope.

## Issues Encountered

- The sandbox refuses compound Bash commands that mix git with other tooling, so the commit ledger file was written with a plain `echo` after reading the git directory with a separate command.
- The `abi_change` figure is not comparable with the old row: the old sample was a single tiny class, the kernel has the TypeID codec and the ID value classes, so the median rose from 278 ms to 448 ms for the same task (`assemble`). The note in `docs/performance.md` says so.

## Known Stubs

None.

## Threat Flags

None. The only new input surface is `RejectionCode`, which fails closed on anything but lower-case kebab-case (T-02-51); the governance boundary cases were repointed, not deleted (T-02-52, `test-check-pr.sh`: 132 passed, 0 failed). No package installs (T-02-SC).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-06 can build the events and commands modules on `Tags`, `MessageNamespace` and `Rejection`.
- Plan 02-11's architecture rules can require every `@EventTag` key to come from `Tags`.
- Plan 02-04 still owns the detekt excludes for suite sources and the TestKit proof of `courses.test-suites`.

## Self-Check: PASSED

- Created files verified present on disk: `Tags.kt`, `MessageNamespace.kt`, `Rejection.kt`, `KernelContractTest.kt`.
- `tooling/convention-sample` is absent; `git ls-files tooling/` prints nothing.
- Commits `89bfdcf`, `3d47c9f` and `f205ba6` are ancestors of HEAD.
- `KernelContractTest`: 10 test cases, 0 failures. `:contracts-kernel:check`, `spotlessCheck`, `bash scripts/governance/test-check-pr.sh` and `./gradlew build` all green.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
