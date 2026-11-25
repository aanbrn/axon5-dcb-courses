---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 09
subsystem: testing
tags: [axon, dcb, decision-record, event-storage-engine, retry, axon-server, kotlin, concurrency]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "command-service library with KotlinTagResolver, CommandServiceConfigurationEnhancer, CommandServiceTestApplication and the Axon Server singleton (plan 02-07); TypeID IDs, events, SubscribeStudent and the shared Jackson mapper (plan 02-06)"
provides:
  - "DecisionCaptureEngine: a central EventStorageEngine decorator that records one DecisionRecord per command attempt, with no capture code in slices (D-13)"
  - "DecisionRecord, RecordedEvent, RecordedTag and the DecisionRecorder interface (persistence is Phase 3)"
  - "conflictRetryScheduler and CONFLICT_RETRY_DECORATION_ORDER: handling-side retry of DCB conflicts through RetryingCommandBus at Int.MIN_VALUE + 10"
  - "CommandServiceConfigurationEnhancer registers the capture decorator, a start guard that requires a DecisionRecorder, a lifecycle-managed retry executor and the retry decorator"
  - "Test fixtures CapacityHarness, ArrivalBarrier and CollectingDecisionRecorder; CommandServiceTestApplication gained a recorder, the harness, a command gateway and a message converter"
  - "Three permanent tests: DecisionCaptureIntegrationTest (the D-13 safeguard), AxonServerSerializationIntegrationTest (INFRA-06 on Axon Server) and the in-memory DecisionCaptureTest"
affects: [02-16, phase-03-command-service, phase-03-decision-slices, renovate, demo-01]

plan_head_before: 77639dfd7a10f499605b5112eeb5af0bca8c7942
plan_head_after: a0179093b7247b418517a72d2b6aa613fbecb2f9

actuals:
  tokens: 18900
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Capture is one EventStorageEngine decorator at order 0 reporting to a required DecisionRecorder component; the enhancer registers it, slices never see it"
    - "Conflict retry is a CommandBus decorator at Int.MIN_VALUE + 10 with a filtering policy; no RetryScheduler component is ever registered"
    - "Concurrency tests dispatch from a 12-thread pool and hold the first round at a non-blocking ArrivalBarrier, so conflicts are certain"
    - "A component that must exist at start gets a guard component with an onStart handler, because decorators resolve their dependencies lazily"

key-files:
  created:
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionRecord.kt
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionRecorder.kt
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngine.kt
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/retry/ConflictRetry.kt
    - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CapacityHarness.kt
    - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CollectingDecisionRecorder.kt
    - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt
    - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/AxonServerSerializationIntegrationTest.kt
    - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt
  modified:
    - backend/command-service/build.gradle.kts
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt
    - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt

key-decisions:
  - "The decision record holds text and numbers only (RecordedEvent with type, payload text and tags), not Axon types, so Phase 3 can persist and render it without depending on Axon"
  - "Attempts are numbered by command id in a bounded counter: a command that ends without a conflict is forgotten, a conflicting one is kept for its retry, and the oldest is dropped past 10,000 entries (the spike's map grew without bound)"
  - "A recorder that throws is logged and swallowed, because capture must never change the outcome of a decision; the failure goes to System.Logger so main needs no logging dependency"
  - "Appends and sources without a ProcessingContext are passed through uncaptured, as they belong to no command attempt; documented in the engine KDoc"
  - "The retry executor is its own component type (ConflictRetryExecutor), not a ScheduledExecutorService, so no lookup of that type can find it by accident; it is shut down in the last lifecycle phase"
  - "The capacity harness is an annotated handler in testFixtures that sources through the public EventStore and folds the course by hand; the first-round conflicts come from a non-blocking barrier, never a blocking latch"

patterns-established:
  - "Decorators that resolve a component lazily need a start-phase guard when the component is mandatory"
  - "Every plan-level test of a decorator is paired with a mutation check that proves it fails when the decorator is broken (swallowing the conflict, dropping the conflicting event)"

requirements-completed: [INFRA-06, INFRA-04]

coverage:
  - id: D1
    description: "Under 12 concurrent SubscribeStudent commands on a 3-seat course on Axon Server there are exactly 3 Subscribed and 9 Rejected(course-full), exactly 3 committed records, at least one conflict naming courses.StudentSubscribed#1, and every attempt above 1 is preceded by a conflict of the same command id with attempt - 1"
    requirement: INFRA-04
    verification:
      - kind: integration
        ref: "backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt#concurrent subscriptions conflict, are retried and never exceed the capacity"
        status: pass
    human_judgment: false
  - id: D2
    description: "A committed attempt records its command type, the course tag in the sourcing and append criteria, a non-blank consistency marker, one appended StudentSubscribed with its tags, and a non-blank marker after commit"
    requirement: INFRA-04
    verification:
      - kind: integration
        ref: "DecisionCaptureIntegrationTest#a committed attempt records what it saw, what it asked the store and what it appended"
        status: pass
      - kind: unit
        ref: "backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt#a subscription that gets a seat is recorded as committed with what it saw and appended"
        status: pass
    human_judgment: false
  - id: D3
    description: "A decision that appends nothing is recorded as NothingAppended with the two events it saw, and two interleaved decisions record one commit and one conflict that names the other's event; the losing unit of work still fails with the original rejection (prohibition: the decorator never changes the outcome)"
    requirement: INFRA-04
    verification:
      - kind: unit
        ref: "DecisionCaptureTest (a subscription to a full course ..., two interleaved decisions ..., a conflict is re-raised unchanged ..., a recorder that throws ...)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Without a DecisionRecorder component the application does not start, and the failure names the missing component (T-02-21: capture is never silently off)"
    verification:
      - kind: unit
        ref: "DecisionCaptureTest#an application without a decision recorder does not start, so capture is never silently off"
        status: pass
    human_judgment: false
  - id: D5
    description: "INFRA-06 on Axon Server: stored types read back as courses.CourseCreated#1 and courses.StudentSubscribed#1 with the terminal entry filtered; sourcing by the course TypeID tag returns 2 events and by the raw UUID none; stored payloads equal the originals; Subscribed and Rejected(course-full) return through CommandGateway.send(...).resultAs(SubscribeResult::class.java)"
    requirement: INFRA-06
    verification:
      - kind: integration
        ref: "backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/AxonServerSerializationIntegrationTest.kt (4 tests)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Production code contains no command handler in Phase 2, no RetryScheduler component is registered, and the retry decoration order is Int.MIN_VALUE + 10"
    verification:
      - kind: other
        ref: "grep -rl '@CommandHandler' backend/command-service/src/main | wc -l = 0; grep -c 'RetryScheduler::class' CommandServiceConfigurationEnhancer.kt = 0; grep -c 'CONFLICT_RETRY_DECORATION_ORDER = Int.MIN_VALUE + 10' ConflictRetry.kt = 1"
        status: pass
    human_judgment: false
  - id: D7
    description: "The upstream records UP-0001 and UP-0002 named in the engine KDoc exist as filed upstream issues"
    verification: []
    human_judgment: true
    rationale: "The records are written in plan 02-16; this plan only marks the workarounds with // UPSTREAM comments, so nothing here proves the issues are filed"

duration: 95min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 09: Decision Capture and Conflict Retry Summary

**A central `EventStorageEngine` decorator records every command attempt (criteria, sourced events, append condition, tagged events, marker after commit, outcome) and a handling-side `RetryingCommandBus` retries DCB conflicts, proven on Axon Server by 12 concurrent subscriptions to a 3-seat course that end with exactly 3 seats taken, plus the INFRA-06 round trip through the store.**

## Performance

- **Duration:** about 95 min (the full `./gradlew build` takes 7.5 min)
- **Started:** about 2026-11-23T11:10:00Z (estimated; not captured at start)
- **Completed:** 2026-11-23T12:45:00Z
- **Tasks:** 2 (a tracer and a TDD task)
- **Files modified:** 12 (9 created, 3 modified), about 1,200 insertions

## Accomplishments

- `DecisionCaptureEngine` ports spike 001 variant A into the command service as production code. It diagnoses conflicts on both paths (rejected in `appendEvents` in memory, in `commit()` on Axon Server), unwraps the `CompletionException` that Axon's own diagnosing engine misses, records the conflicting event, and re-raises the original failure. A mutation that swallows the failure and one that drops the conflicting event are both caught by the unit tests.
- `CommandServiceConfigurationEnhancer` now wires everything with no per-slice code: the capture decorator at order 0, a guard that fails the start when no `DecisionRecorder` exists, a `ConflictRetryExecutor` closed with the configuration, and `RetryingCommandBus` at `Int.MIN_VALUE + 10`. No `RetryScheduler` component is registered.
- `DecisionCaptureIntegrationTest` (the D-13 safeguard) runs 12 `SubscribeStudent` commands from a 12-thread pool against a 3-seat course on Axon Server, with the first round held at an `ArrivalBarrier`. It asserts exactly 3 `Subscribed` and 9 `Rejected(course-full)`, exactly 3 committed records, conflicts that name `courses.StudentSubscribed#1`, and the attempt-chain rule. Both tests of the class passed in every run.
- `AxonServerSerializationIntegrationTest` makes the spike 002 round trip permanent: versioned names, TypeID tag versus raw UUID, payload equality through the service's own converter, and sealed results through `resultAs(SubscribeResult::class.java)`.
- The `CapacityHarness` lives in `testFixtures`; the production source set has no `@CommandHandler`, so Phase 3 owns the real decisions.

## Task Commits

1. **Task 1: End-to-end decision capture with conflict retry (tracer)** - `07f38e1` (feat)
2. **Task 2: In-memory capture unit test and the INFRA-06 round trip (TDD)**
   - RED - `61f9a19` (test)
   - GREEN - `a017909` (feat)

The tracer feedback gate ran in its automated branch (interactive mode, `human_verify_mode: end-of-phase`, the tracer's `<verify>` is automated-only, no `gate="blocking-human"`): the `<verify>` was re-run end to end after the last change (`DecisionCaptureIntegrationTest` with `--rerun`, 2 tests, 0 failures, 0 skipped, and the `RetryScheduler::class` grep at 0) and passed before Task 2 started.

**Plan metadata:** committed with this SUMMARY (docs).

## TDD Gate Compliance

Task 2 is `tdd="true"` inside an `execute` plan, so the plan-level gate sequence does not apply, but the cycle was followed.

- **RED:** `test(02-09)` `61f9a19` added `DecisionCaptureTest` (6 tests) and `AxonServerSerializationIntegrationTest` (4 tests). Target test `an application without a decision recorder does not start, so capture is never silently off` failed with `AssertionFailedError: Expected exception ComponentNotFoundException but no exception was thrown` (JUnit XML of the actual run, `backend/command-service/build/test-results/test/TEST-...DecisionCaptureTest.xml`). Semantic assessment: the target executed and failed on the planned assertion for the intended reason, a configuration without a recorder started instead of failing, which contradicts the plan's "a missing recorder fails configuration loudly". No `gsd_run check tdd-red-evidence` record was produced, because `workflow.tdd_mode` is off and this is not a `type: tdd` plan.
- **Unexpected GREEN for the other 9 tests, by design:** Task 1 is a tracer that already built the capture and the retry, and plans 02-06 and 02-07 already built the serialization. These tests are characterization tests of finished code. To show that they are not vacuous, two mutations were applied to `DecisionCaptureEngine` and reverted: dropping the conflicting event failed the interleaved-conflict test, and swallowing the original failure failed the interleaved-conflict test and the re-raise test.
- **GREEN:** `feat(02-09)` `a017909` added the start guard; all 6 unit tests pass.
- **REFACTOR:** none.

## Files Created/Modified

- `capture/DecisionRecord.kt`, `DecisionRecorder.kt` - the immutable record model and the recorder interface
- `capture/DecisionCaptureEngine.kt` - the decorator, the per-unit-of-work draft and the bounded attempt counter
- `retry/ConflictRetry.kt` - decoration order constant and the filtering scheduler
- `CommandServiceConfigurationEnhancer.kt` - registers the decorators, the start guard and the retry executor
- `fixtures/CapacityHarness.kt`, `CollectingDecisionRecorder.kt`, `CommandServiceTestApplication.kt` - test infrastructure
- `build.gradle.kts` - the fixtures expose the events and commands contracts (`testFixturesApi`)
- `DecisionCaptureIntegrationTest.kt`, `AxonServerSerializationIntegrationTest.kt`, `DecisionCaptureTest.kt` - the three permanent tests

## Decisions Made

See `key-decisions` above. The two that later plans must know: records are Axon-free text (persist and render without Axon), and `conflictRetryScheduler(executor, maxAttempts)` takes its bound as Axon's `MaxAttemptsPolicy` counts it, retries after the first attempt, so a command is dispatched at most `maxAttempts + 1` times.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Start guard so a missing recorder fails the configuration**
- **Found during:** Task 2 (RED run of the missing-recorder test)
- **Issue:** the plan requires that a missing recorder fails configuration loudly. The decorator resolves the recorder only when the storage engine is first built, so an application without one started normally and failed at its first command.
- **Fix:** a `RecorderGuard` component whose start handler resolves `DecisionRecorder`, so the start fails with `LifecycleHandlerInvocationException` caused by `ComponentNotFoundException` naming the recorder.
- **Files modified:** `CommandServiceConfigurationEnhancer.kt`, `DecisionCaptureTest.kt` (the expected exception type)
- **Verification:** the test passes; full `./gradlew build` is green.
- **Committed in:** `a017909`

**2. [Rule 1 - Bug] Unbounded attempt map of the spike**
- **Found during:** Task 1 (porting `DecisionRecorder.nextAttempt`)
- **Issue:** the spike counted attempts in a `ConcurrentHashMap` keyed by command id that never shrank, a memory leak in a long-running service.
- **Fix:** `AttemptCounter` forgets a command when it ends without a conflict and keeps at most 10,000 conflicting commands, oldest dropped first.
- **Files modified:** `capture/DecisionCaptureEngine.kt`
- **Committed in:** `07f38e1`

**3. [Rule 2 - Missing Critical] A throwing recorder must not change a decision**
- **Found during:** Task 1 (the plan's prohibition that capture never changes an outcome)
- **Issue:** a recorder exception raised inside the unit of work's completion callbacks could fail a decision that had committed.
- **Fix:** the draft catches and logs the failure through `System.Logger`; a unit test proves a throwing recorder leaves the result `Subscribed`.
- **Files modified:** `capture/DecisionCaptureEngine.kt`, `DecisionCaptureTest.kt`
- **Committed in:** `07f38e1`, `61f9a19`

**4. [Rule 3 - Blocking] Detekt `OutdatedDocumentation` and `MagicNumber`**
- **Found during:** Task 1, `:command-service:detekt`
- **Issue:** `@property` tags on private constructor properties were flagged, and the literal `12` in the pool is a magic number, while the plan's acceptance criterion greps the literal `Executors.newFixedThreadPool(12)`.
- **Fix:** `@param` tags, and a `@Suppress("MagicNumber")` with a reason comment on the one function that creates the pool.
- **Files modified:** `DecisionCaptureEngine.kt`, `CapacityHarness.kt`, `DecisionCaptureIntegrationTest.kt`
- **Committed in:** `07f38e1`

---

**Total deviations:** 4 auto-fixed (1 bug, 2 missing critical, 1 blocking)
**Impact on plan:** none changes the plan's scope. The start guard and the throwing-recorder test pin plan truths and the T-02-19 and T-02-21 mitigations that the plan states but no listed file implemented; the leak fix removes a spike defect before it ships.

## Issues Encountered

- The `MaxAttemptsPolicy` parameter is named `retries` but the plan's `maxAttempts` bound reads like a total. From the 5.4.2 sources the scheduler's history holds one entry per earlier failure, so the bound counts retries; the KDoc of `conflictRetryScheduler` says so.
- The worktree sandbox refuses writes under the main repository's `.git` directory, so the per-plan HEAD ledger file of the commit protocol could not be persisted. `commits: 3` was measured with `git rev-list --count 77639df..HEAD` against the base the orchestrator supplied, which is also the recorded `plan_head_before`.
- Observed, not asserted: the full `./gradlew build` takes 7 min 20 s in this worktree with other suites running on the machine.

## Known Stubs

None.

## Threat Flags

None. The decorator and the retry sit on the trust boundary the plan's threat model already covers (T-02-19, T-02-20, T-02-21); no new endpoint, auth path or schema was added.

## User Setup Required

None - no external service configuration required. Docker is the only requirement, as before.

## Next Phase Readiness

- Phase 3 decisions get decision records and conflict retries by registering a persisting `DecisionRecorder` component; no slice code is needed. The retry bound is 10 retries with 5 ms exponential back-off.
- Plan 02-16 owns the upstream records UP-0001 (a public extension point for decision capture) and UP-0002 (`ConflictDiagnosingEventStorageEngine` does not unwrap `CompletionException`); the engine KDoc carries the `// UPSTREAM` markers.
- Spike finding to carry into slices: a creation command that sources nothing appends with `AppendCondition.none()`, so two concurrent creations of the same ID both commit; the harness here deliberately has no creation command.
- No blockers.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*

## Self-Check: PASSED

- All 9 created files exist on disk (checked with `ls`); `07f38e1`, `61f9a19` and `a017909` are reachable from HEAD.
- Result XMLs: `DecisionCaptureTest` tests=6, `AxonServerSerializationIntegrationTest` tests=4, `DecisionCaptureIntegrationTest` tests=2, all with skipped=0 and failures=0.
- Acceptance greps: `CONFLICT_RETRY_DECORATION_ORDER = Int.MIN_VALUE + 10` 1; `AppendEventsTransactionRejectedException` in the engine 3; `@CommandHandler` files under `src/main` 0; `Executors.newFixedThreadPool(12)` 1; `RetryScheduler::class` in the enhancer 0; `courses.StudentSubscribed#1` in the serialization test at least 1; `resultAs(SubscribeResult::class.java)` at least 1.
- `mise x -- ./gradlew build --console=plain` is green (7 min 20 s) after `spotlessApply`, including detekt, Dokka and all three test levels.
