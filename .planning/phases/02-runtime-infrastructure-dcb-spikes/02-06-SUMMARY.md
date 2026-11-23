---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 06
subsystem: contracts
tags: [kotlin, axon, jackson, typeid, serialization, contracts, gradle, dokka]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "Kernel Tags, MessageNamespace, Rejection and RejectionCode (plan 02-03); kernel testFixtures TestIds and the courses.test-suites convention (plan 02-04); TypedId codec and ID value classes (plan 02-01)"
provides:
  - ":contracts-events with CourseCreated and StudentSubscribed (courses.CourseCreated#1, courses.StudentSubscribed#1)"
  - ":contracts-commands with SubscribeStudent (courses.SubscribeStudent#1) and the sealed SubscribeResult = Subscribed | Rejected(code)"
  - ":contracts-queries, the fourth D-02 contracts module, source-less until the first query slice"
  - ":serialization-jackson with CoursesJsonModule and coursesJsonMapper() (Jackson 3, kernel-only dependency)"
  - "JsonRoundTripTest, 12 tests proving the INFRA-06 round trip and its failure modes"
  - "Catalog aliases axon-messaging, axon-eventsourcing, jackson-annotations, jackson-databind, jackson-module-kotlin"
affects: [02-07, 02-11, command-service, bff, architecture, phase-03-first-slice]

plan_head_before: e9b799dc937ae347620d047b15c5e5a3d0d07eb1
plan_head_after: 25a9d1bae042d663b06a4da499227b2d6c462883

actuals:
  tokens: 4600
  tasks: 3
  commits: 4

tech-stack:
  added: []
  patterns:
    - "Contracts take Axon and Jackson annotations as compileOnly metadata and the kernel through api"
    - "Polymorphism lives on the sealed result type itself (@JsonTypeInfo SIMPLE_NAME, @type), never through default typing or a mix-in"
    - "The shared JSON module knows the kernel only; contract types are test subjects of the serialization module"
    - "Value-class IDs and RejectionCode are written as plain strings and read back through the validating factories"

key-files:
  created:
    - backend/contracts/events/build.gradle.kts
    - backend/contracts/events/src/main/kotlin/io/github/aanbrn/courses/contracts/events/CourseEvents.kt
    - backend/contracts/commands/build.gradle.kts
    - backend/contracts/commands/src/main/kotlin/io/github/aanbrn/courses/contracts/commands/SubscribeStudent.kt
    - backend/contracts/queries/build.gradle.kts
    - backend/serialization-jackson/build.gradle.kts
    - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
    - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
  modified:
    - gradle/libs.versions.toml
    - settings.gradle.kts
    - qodana.yaml

key-decisions:
  - "RejectionCode is (de)serialised by an explicit serializer pair in CoursesJsonModule, so the wire form is the bare code text and a malformed code fails with the value class's own message"
  - "The three module-wide serializer helpers are file-private classes and objects rather than anonymous objects, so each has KDoc and the module class stays small"
  - "courses.kotlin-library was left untouched: Dokka 2.3.0 handles the source-less queries module without a workaround"

patterns-established:
  - "A new module adds its build directory to the Qodana All exclusion, in the same commit that includes it in settings.gradle.kts"
  - "A module with no source still applies courses.test-suites and passes check: compile, detekt and the three test tasks are NO-SOURCE, Dokka exits gracefully"

requirements-completed: [INFRA-02, INFRA-06]

coverage:
  - id: D1
    description: "CourseCreated and StudentSubscribed exist in :contracts-events with @Event(namespace = COURSES, version = 1) and registry-keyed @EventTag; CourseCreated round-trips through coursesJsonMapper() and its JSON holds the canonical TypeID, not UUID text (INFRA-06, D-03, D-10)"
    requirement: INFRA-06
    verification:
      - kind: unit
        ref: "backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt#a course created event round trips unchanged, #the JSON of an event holds the canonical TypeID and no UUID text"
        status: pass
    human_judgment: false
  - id: D2
    description: "A wrong-prefix or malformed ID in JSON fails deserialisation with 'Wrong TypeID prefix' / 'Malformed TypeID' (T-02-10)"
    requirement: INFRA-06
    verification:
      - kind: unit
        ref: "JsonRoundTripTest#an ID of another entity type in a course field fails deserialisation, #a malformed ID in JSON fails deserialisation"
        status: pass
    human_judgment: false
  - id: D3
    description: "Edge INFRA-06: nil-UUID and all-ones-UUID IDs round-trip unchanged; 10,000 generated UUIDs keep full precision through write and read"
    requirement: INFRA-06
    verification:
      - kind: unit
        ref: "JsonRoundTripTest#IDs built from the nil UUID and from the all-ones UUID round trip, #ten thousand generated UUIDs survive the round trip with full precision"
        status: pass
    human_judgment: false
  - id: D4
    description: "SubscribeStudent round-trips; SubscribeResult.Subscribed is {\"@type\":\"Subscribed\"} and reads back as the same singleton; Rejected carries @type and code and round-trips; an unknown @type and a malformed code fail (D-44, T-02-11)"
    requirement: INFRA-06
    verification:
      - kind: unit
        ref: "JsonRoundTripTest (6 command and result tests; result XML tests=12 skipped=0 failures=0)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The contracts carry Axon and Jackson only as compileOnly annotation artifacts, depend on the kernel through api, commands do not depend on events, and :serialization-jackson depends on the kernel only in main (D-02, D-04, D-44)"
    requirement: INFRA-02
    verification:
      - kind: other
        ref: "grep acceptance criteria on backend/contracts/*/build.gradle.kts and backend/serialization-jackson/build.gradle.kts (compileOnly counts, zero implementation/api libs.axon|jackson, zero contracts-events in commands)"
        status: pass
    human_judgment: false
  - id: D6
    description: ":contracts-queries exists as the fourth D-02 module and its check passes while source-less, with no stand-in type and no change to Dokka's failOnWarning (ROADMAP Phase 2 success criterion 3)"
    requirement: INFRA-02
    verification:
      - kind: integration
        ref: "./gradlew :contracts-queries:check (compileKotlin, detekt and all three test tasks NO-SOURCE; Dokka 'Exiting Generation: Nothing to document'); ./gradlew build green"
        status: pass
    human_judgment: false

duration: 20min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 06: Events and Commands Contracts, Queries Module and the Shared Jackson Module Summary

**`:contracts-events`, `:contracts-commands` and a source-less `:contracts-queries` carry the first walking-skeleton messages with annotation-only framework coupling, and `:serialization-jackson` round-trips them through one kernel-only Jackson 3 module: TypeID strings for IDs, bare text for rejection codes, and `@JsonTypeInfo` on the sealed `SubscribeResult` instead of a mix-in.**

## Performance

- **Duration:** about 20 min (the three `./gradlew build` runs dominate: one cold run of 6 min 28 s, the rest cached)
- **Started:** about 2026-11-23T02:15:00Z (not captured at start; estimated from the first commit time)
- **Completed:** 2026-11-23T02:35:00Z
- **Tasks:** 3 (one tracer, one TDD, one auto)
- **Files modified:** 11 (8 created, 3 modified)

## Accomplishments

- `CourseCreated(courseId, name, capacity)` and `StudentSubscribed(courseId, studentId)` use `@Event(namespace = MessageNamespace.COURSES, name, version = "1")` and `@EventTag(key = Tags.X)` in the plain constructor-property form. They produce the stored types `courses.CourseCreated#1` and `courses.StudentSubscribed#1`.
- `SubscribeStudent` and the sealed `SubscribeResult` live in `:contracts-commands`. `Rejected(override val code: RejectionCode)` implements the kernel `Rejection`; the type carries `@JsonTypeInfo(use = SIMPLE_NAME, property = "@type")` itself (D-44).
- `CoursesJsonModule` registers TypeID string (de)serializers for `CourseId` and `StudentId` through one generic pair of private classes over `TypedIdCompanion`, plus a text pair for `RejectionCode`. `coursesJsonMapper()` adds the Kotlin module and no default typing.
- `JsonRoundTripTest` has 12 tests (`tests="12" skipped="0" failures="0"`): event round trip, canonical-TypeID JSON, wrong-prefix and malformed failures, nil and all-ones UUIDs, 10,000 generated UUIDs, command round trip, `Subscribed` singleton identity, `Rejected` round trip, unknown `@type` failure and malformed rejection-code failure.
- `:contracts-queries` is wired like its siblings and holds only a build script; `check` passes without source.
- The catalog gains the five version-less, BOM-governed libraries. `axon-messaging` and `axon-eventsourcing` resolve to 5.4.2 through `:platform`; `jackson-annotations` resolves to 2.23 through the Jackson BOM (visible in the Dokka classpath of `:contracts-queries`); `jackson-databind` and `jackson-module-kotlin` resolve at 3.3.0.

## Task Commits

1. **Task 1: End-to-end event serialization (tracer)** - `2f19b0d` (feat). The tracer feedback gate ran in its automated branch: `<verify>` was re-run end to end after `spotlessApply`, passed, and expansion continued.
2. **Task 2: Commands module with the sealed result (TDD)**
   - RED: `99c3427` (test)
   - GREEN: `5d5c395` (feat)
   - REFACTOR: none needed
3. **Task 3: Queries module** - `25a9d1b` (feat)

**Plan metadata:** committed with this SUMMARY (docs).

## TDD Gate Compliance

- **RED (`99c3427`):** the six command and result tests were committed before any implementation. Honest assessment, as in plans 02-01, 02-03 and 02-04: the run failed at compilation (`Unresolved reference 'commands'`, `Unresolved reference 'SubscribeStudent'`), not on a planned assertion, so under the strict `tdd.md` classifier it would be `build_failure`, not `RED_EVIDENCE_OK`. The plan is `type: execute` and `workflow.tdd_mode` is off, so no classifier ran. The types did not exist, and a compile-level RED is the only RED available without throwaway stubs.
- **GREEN (`5d5c395`):** all 12 tests passed on the first run, including the value-class `RejectionCode` inside `Rejected` and the `data object` singleton identity; `./gradlew build` was green.
- **REFACTOR:** not needed.

## Files Created/Modified

- `backend/contracts/events/build.gradle.kts` - kernel through `api`, `compileOnly` Axon annotations
- `.../contracts/events/CourseEvents.kt` - `CourseCreated`, `StudentSubscribed`
- `backend/contracts/commands/build.gradle.kts` - kernel through `api`, `compileOnly` `axon-messaging` and `jackson-annotations`
- `.../contracts/commands/SubscribeStudent.kt` - `SubscribeStudent`, sealed `SubscribeResult`
- `backend/contracts/queries/build.gradle.kts` - the source-less fourth contracts module
- `backend/serialization-jackson/build.gradle.kts` - kernel `api`, `jackson-databind` `api`, Kotlin module `implementation`, contracts as test dependencies only
- `.../serialization/CoursesJsonModule.kt` - the shared module and `coursesJsonMapper()`
- `.../serialization/JsonRoundTripTest.kt` - 12 unit tests
- `gradle/libs.versions.toml`, `settings.gradle.kts`, `qodana.yaml` - catalog aliases, four new includes with `projectDir` mapping, four build directories in the Qodana `All` exclusion

## Dokka output for `:contracts-queries`

`./gradlew :contracts-queries:dokkaGeneratePublicationHtml --rerun --no-build-cache --info` ends with `Exiting Generation: Nothing to document` and the task succeeds with `failOnWarning = true`. `compileKotlin` and `detekt` report `NO-SOURCE`, as do the three test tasks. `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` was therefore **not changed**: no `onlyIf` was needed, and neither `failOnWarning` nor the sources were touched.

## Decisions Made

See `key-decisions` in the frontmatter. The one later plans must know: a module that only declares a contracts build script (no source) passes `check` under `courses.test-suites`, so the queries module needs nothing from the first query slice except its source.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Module includes were added per task, not all in Task 1**
- **Found during:** Task 1 (planning the settings change)
- **Issue:** step 2 of Task 1 includes `:contracts-commands` while its directory is created only in Task 2; the plan itself allows deferring the include "if Gradle rejects a missing directory". Including the module early would also have left Task 1's commit unbuildable if it were checked out alone.
- **Fix:** Task 1 includes `:contracts-events` and `:serialization-jackson`, Task 2 adds `:contracts-commands`, Task 3 adds `:contracts-queries`; the Qodana exclusions follow the same order, which also satisfies the per-task acceptance greps (`qodana.yaml` commands count 1 after Task 2, queries count 1 after Task 3).
- **Files modified:** `settings.gradle.kts`, `qodana.yaml`
- **Committed in:** `2f19b0d`, `5d5c395`, `25a9d1b`

**2. [Rule 1 - Bug] Formatter re-wrapped two sources**
- **Found during:** Task 1 (`spotlessCheck`) and Task 3 (`./gradlew build`)
- **Issue:** ktfmt wrapped the `RejectionCodeDeserializer` expression body and a comment in `queries/build.gradle.kts`; the Task 3 build run failed `spotlessKotlinGradleCheck` before the fix.
- **Fix:** `spotlessApply`; the queries build-script comment was rewritten to fit the 120-column wrapping, with no content change.
- **Files modified:** `CoursesJsonModule.kt`, `backend/contracts/queries/build.gradle.kts`
- **Verification:** `spotlessCheck` and `./gradlew build` green
- **Committed in:** `2f19b0d`, `25a9d1b`

---

**Total deviations:** 2 auto-fixed (1 blocking ordering choice the plan allowed, 1 formatter)
**Impact on plan:** None. Both keep every acceptance criterion satisfiable and add no scope.

## Issues Encountered

- The sandbox refuses compound Bash commands that mix git, `python3` heredocs or chains, so the test additions were made with the Edit tool and the commit ledger and sentinel with plain `echo` commands.
- The tracer commit's first verification came up green on the first Gradle run with no compile fixes: value-class IDs inside Kotlin data classes work with the Kotlin module plus the custom serializers, as spike 002 showed.

## Known Stubs

None. `:contracts-queries` is intentionally source-less (D-02, plan truth), not a stub: it holds no placeholder type and the first query slice adds its first message.

## Threat Flags

None. The deserialisers fail closed on malformed and wrong-prefix IDs (T-02-10); `@JsonTypeInfo` is only on the sealed `SubscribeResult` and a test proves an unknown `@type` fails, with no default typing (T-02-11). No package installs (T-02-SC).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-07 onward can register `coursesJsonMapper()` with Axon and use these messages in the permanent Axon Server tests.
- Plan 02-11's architecture rules now cover all four contracts modules. They can check that every `@EventTag` key comes from `Tags`, that no contracts build script uses `implementation` or `api` for Axon or Jackson, and that commands do not depend on events.
- The message names and versions become one-way when Phase 3 persists the first event (D-03).

## Self-Check: PASSED

- Created files verified present on disk: the eight files listed under `key-files.created`.
- Commits `2f19b0d`, `99c3427`, `5d5c395` and `25a9d1b` are ancestors of HEAD; `git rev-list --count e9b799d..HEAD` printed 4 before this SUMMARY commit.
- `JsonRoundTripTest` result XML: `tests="12" skipped="0" failures="0"`. `:serialization-jackson:check`, `:contracts-events:check`, `:contracts-queries:check`, `spotlessCheck` and the full `./gradlew build` are green.
- `git ls-files -- backend/contracts/queries` prints exactly `backend/contracts/queries/build.gradle.kts`.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
