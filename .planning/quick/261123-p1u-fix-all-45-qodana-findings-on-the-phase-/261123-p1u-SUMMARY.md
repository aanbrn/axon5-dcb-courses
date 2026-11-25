---
phase: quick-261123-p1u
plan: 01
subsystem: build-and-backend-quality
tags: [qodana, kotlin, gradle, axon, jackson, testkit]
requires: []
provides:
  - "45 Qodana findings fixed in source and build configuration (verification of the 0-problem result blocked, see below)"
  - "Opt-in test fixtures convention (java-test-fixtures per module)"
  - "DecisionCaptureEngine overriding the Java default methods of EventStorageEngine, with a unit test"
affects: [backend/command-service, backend/contracts/kernel, backend/serialization-jackson, build-logic, tooling/doctor]
tech-stack:
  added: []
  patterns:
    - "explicit Axon CommandHandler registered by QualifiedName instead of annotation scanning (test harness)"
    - "Jackson ToStringSerializer plus weirdStringException(...).withCause(...) for from-string value types"
    - "pluginManager.withPlugin(java-test-fixtures) in a convention plugin for opt-in fixtures"
key-files:
  created:
    - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngineTest.kt
  modified:
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngine.kt
    - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CapacityHarness.kt
    - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
    - build-logic/src/main/kotlin/courses.test-suites.gradle.kts
decisions:
  - "Test fixtures are opt-in per module (reversible)"
  - "command-service main takes serialization-jackson as compileOnly plus runtimeOnly (reversible)"
  - "CapacityHarness is an explicit CommandHandler registered for courses.SubscribeStudent (reversible)"
metrics:
  duration: "about 1h 15m"
  completed: 2026-11-23
status: complete
actuals:
  tokens: 16000
  tasks: 3
  commits: 4
plan_head_before: ff7ce8bf551bf8c8981137f92a00d2243aa82440
plan_head_after: 57ec1089d00f65c436029e84ef293e74803aa855
---

# Phase quick-261123-p1u Plan 01: Fix all 45 Qodana findings Summary

All 45 Qodana findings are fixed on their merits in source and build configuration, and `./gradlew build`, `reuse lint`
and `editorconfig-checker` pass, but the final Qodana run could not be completed: the Qodana CLI in the pinned image
cannot reach `qodana.cloud` from this machine (see "Qodana run: BLOCKED"), so "0 problems" is NOT yet confirmed.

`status: complete` in the frontmatter marks the plan's code work as finished; the one open verification is the Qodana
run below and must be repeated by the author (or the orchestrator) on a network that reaches `qodana.cloud`.

## Qodana run: BLOCKED (external connectivity)

`mise x -- bash scripts/qodana.sh`, run twice from the worktree root on the committed HEAD (04b0af1, then 57ec108),
exited 1 after about 150 seconds both times, before any analysis started:

```
level=fatal msg="Failed to obtain proper API endpoints: request of available API versions failed: failed to obtain proper cloud response"
Attempt #1..3 for request to '/api/versions' failed. Error: Get "https://qodana.cloud/api/versions": ... i/o timeout
```

Diagnosis: `https://qodana.cloud` (CloudFront, 18.66.102.x) and `https://www.jetbrains.com` time out from the host and from
inside the pinned Qodana container (colima), while `https://example.com` returns 200 and `https://api.qodana.cloud`
answers. DNS resolves. Six probes over about two minutes all timed out. The planning-time run that produced the 45
findings reached the API, so this is a network condition of the current session, not a change in the repository. I did not
change `scripts/qodana.sh`, `qodana.yaml` or the Docker setup, and an attempt to repeat a diagnostic `curl` outside the
sandbox was denied by the permission system, so I did not work around it.

What remains: run `bash scripts/qodana.sh` from this worktree (HEAD 57ec108) with `qodana.cloud` reachable, then
`jq '.runs[0].results | length' "$HOME/.cache/axon5-dcb-courses/qodana/results/qodana.sarif.json"` must print 0. The
previous SARIF in that cache was deleted by the script's `rm -rf results` at the start of each run. If Qodana still
reports anything, fix it on its merits as the plan says.

## Disposition table

Commits: T1 = 856d8ba, T2 = e9933ba, T3 = 04b0af1, T1b = 57ec108 (test tweak for finding 19).

| # | Rule | Fix | Commit |
|---|------|-----|--------|
| 1 | UnnecessaryModuleDependency command-service.main -> contracts-commands | removed `implementation(:contracts-commands)` from main; fixtures keep `testFixturesApi` (backend/command-service/build.gradle.kts) | T3 |
| 2 | same -> contracts-events | removed `implementation(:contracts-events)` from main, same file | T3 |
| 3 | same -> contracts-kernel | serialization-jackson no longer exports the kernel (`api` to `implementation`); command-service main has no contracts dependency | T3 |
| 4 | UnnecessaryModuleDependency command-service.test -> serialization-jackson.main | `compileOnly` + `runtimeOnly` instead of `implementation`, so it leaves the unit-test compile classpath | T3 |
| 5 | serialization-jackson.test -> serialization-jackson.testFixtures | fixtures opt-in: no `java-test-fixtures` there, so no empty fixtures module | T3 |
| 6 | architectureTest -> architecture.testFixtures | same convention change | T3 |
| 7 | doctor.main -> test-support.main | `implementation(libs.testcontainers)`; probe still prints `ok testcontainers: ... ryuk enabled` | T3 |
| 8 | test-support.componentTest -> test-support.testFixtures | same convention change | T3 |
| 9 | test-support.integrationTest -> test-support.testFixtures | same convention change | T3 |
| 10 | JavaDefaultMethodsNotOverriddenByDelegation (DecisionCaptureEngine) | explicit overrides of vararg `appendEvents` (routes to the capturing List overload), `source(condition)` (routes to `source(condition, null)`) and `describeTo` (`describeWrapperOf(delegate)`); `DecisionCaptureEngineTest` proves the vararg capture and the context-free pass-through | T1 |
| 11 | SimplifiableCallChain (DecisionCaptureTest) | `mapNotNull { }` before `.single()` | T1 |
| 12 | RedundantSamConstructor (DecisionCaptureTest) | plain lambda in the named `recorder` argument | T1 |
| 13 | UnusedSymbol CapacityHarness.handle | not deleted: explicit `CommandHandler`, registered for `SUBSCRIBE_STUDENT_NAME` (see judgement calls) | T1 |
| 14 | UsePropertyAccessSyntax (CommandServiceTestApplication) | `servers = address` | T1 |
| 15 | HasPlatformType (TypedId) | `val uuidV7: TimeBasedEpochGenerator` | T1 |
| 16 | UnusedSymbol ClockedUuidV7Generator.nextCourseId | deleted (no caller in any source set) | T1 |
| 17 | UnusedSymbol ClockedUuidV7Generator.nextStudentId | deleted (no caller in any source set) | T1 |
| 18 | UnusedSymbol IdArbs.studentId | deleted (no caller in any source set) | T1 |
| 19 | SameParameterValue TestIds.courseId(number) | range contract of TestIds now tested (distinct v7/IETF values at 1, 2, 500, 9999; `IllegalArgumentException` at 0, 10000, -1); calls use explicit numbers | T1, T1b |
| 20 | unused `ctxt` (serializer 1) | hand-written serializers deleted; Jackson's `ToStringSerializer.instance` registered for both IDs | T1 |
| 21 | unused `ctxt` (serializer 2) | same, for `RejectionCode` | T1 |
| 22, 23 | CanConvertToMultiDollarString GradleFixture.kt:198 (two) | `testSuites` build script is a `$$"""` raw string (`$$HEADER`, plain `$name=$maxParallelForks`); produced script is identical | T2 |
| 24 | CollectionConcatenationToBuildCollection GradleFixture | `addTest` builds its lines with `buildList` | T2 |
| 25 | SameParameterValue DependencyLicenceCheckFunctionalTest | `VENDOR_TERMS` constant; `exception` helper lost the licence parameter | T2 |
| 26-35 | CanConvertToMultiDollarString ImageReferenceCheckFunctionalTest (ten) | every escaped-dollar literal is a `$$"..."` literal with identical content | T2 |
| 36 | CollectionConcatenationToBuildCollection AdrCheck | `buildList { flatMapTo; addAll; addAll }`, same order | T2 |
| 37 | IntroduceWhenSubject AdrCheck | `when (target) { null -> ...; file.number -> ...; !in known -> ... }` | T2 |
| 38 | IntroduceWhenSubject FrontMatter | `when (char)` | T2 |
| 39 | KotlinUnreachableCode FrontMatter | dropped the `return` keyword before the `Nothing`-typed `fail` call (see judgement calls) | T2 |
| 40, 41 | CanConvertToMultiDollarString ImageReferenceCheck (two) | `$$"..."` literals with plain `${VARIABLE}`, `$$path`, `$${index + 1}`, `$$value` | T2 |
| 42 | CollectionConcatenationToBuildCollection UpstreamRecordCheck | `buildList` for the four problem sources before `sortedWith` | T2 |
| 43, 44 | SameParameterValue UpstreamRecordCheck (`workaround`, `upstream`) | `workaroundValue()` and `upstreamValue()` replace the generic `flag` and `list`; `entry` became `protected` | T2 |
| 45 | IntroduceWhenSubject UpstreamRecordCheck.date | `when (value)` with a `when` guard (`is Text if isDate(value.value)`) | T2 |

## Verification results

| Check | Result |
|-------|--------|
| `mise x -- ./gradlew build --console=plain` (final tree before the test-only commit 57ec108) | BUILD SUCCESSFUL, exit 0, 460 s (all suites, integrationTest against Axon Server, build-logic functionalTest, detekt, Dokka, Spotless) |
| `:contracts-kernel:check` after 57ec108 | BUILD SUCCESSFUL |
| `mise x -- reuse lint` | compliant (453/453 files) |
| `mise x -- editorconfig-checker` | exit 0 |
| `scripts/qodana.sh` | BLOCKED: exit 1 after about 150 s, `qodana.cloud` unreachable (both attempts); result count not obtained |
| No-weakening audit | `git diff 21cdf25..HEAD -- qodana.yaml config/detekt` is empty; added `@Suppress`/`noinspection` lines in `*.kt`/`*.kts`: 0; tracked qodana/detekt baseline files: 0 |
| Plan grep gates | all 46 hold except the first run of `TestIds.courseId(` (count 2, expected >= 3), fixed in 57ec108 |

Task-level checks: Task 1 `spotlessCheck :contracts-kernel:check :serialization-jackson:check :command-service:check
:tooling:architecture:check` green (40 s); Task 2 `spotlessCheck :build-logic:check` green (9 min 21 s, TestKit); Task 3 focused
checks (TestSuitesFunctionalTest, command-service, serialization-jackson, contracts-kernel, doctor probe) green (2 min 43 s).

## Judgement calls

**CapacityHarness.handle was used, not unused.** Axon called it through annotation scanning whenever a test dispatched
`SubscribeStudent`, so deleting it would have broken `DecisionCaptureTest`, `DecisionCaptureIntegrationTest` and
`AxonServerSerializationIntegrationTest`. The honest fix is an explicit `org.axonframework.messaging.commandhandling.CommandHandler`
registered with `commandHandler(SUBSCRIBE_STUDENT_NAME) { ... }`, so the call is visible in code. The harness resolves the
result type through the configured `MessageTypeResolver` (a registered component, confirmed by the passing tests).

**compileOnly + runtimeOnly for serialization-jackson in command-service.** Gradle inheritance on this build:
`testCompileOnly` extends only `compileOnlyApi`, `testRuntimeOnly` extends `runtimeOnly`, so the module stays on main's compile
classpath and on every runtime classpath, but leaves the unit-test compile classpath. IntelliJ imports it as a RUNTIME-scope
dependency of the test module, and `UnnecessaryModuleDependencyInspection` skips RUNTIME-scope entries (candidates are
filtered with `e.getScope() != DependencyScope.RUNTIME && !e.isExported()`). The `:platform` constraints sit on
`implementation` configurations that every resolvable classpath extends, so versions stay aligned.

**Fixtures opt-in against D-21.** D-21 says no unused dependencies; the convention previously gave every module an empty
fixtures module that its suites depended on. Now `java-test-fixtures` is applied by the module that has fixtures (kernel,
command-service) and `courses.test-suites` wires `testFixtures(project())` into every suite of such a module through
`pluginManager.withPlugin`, including `architectureTest`. TestKit proves both sides: all suites call a fixtures class, and a
module without the plugin has no `compileTestFixturesKotlin` task.

**KotlinUnreachableCode analysis (finding 39).** `fail` returns `Nothing`, so the `return` in front of the final call in
`Cursor.quoted()` never executed. It hid no logic bug: the loop ends only when the text runs out before a closing quote, and a
trailing backslash is reported by `escaped()`. The characterisation row (unterminated quoted string, line 2) passes before and
after the change.

**Rule 1 deviation found during Task 1: the explicit handler must add the command to the context.** The first run with the
explicit handler failed `DecisionCaptureTest` (`commandType` was null). Axon's annotated adapter hands its handlers a context
produced by `Message.addToContext(context, message)`, which is where the decision capture reads the command from. The explicit
`CapacityHarness.handle` now does the same (`Message.addToContext(context, command)` and uses that context for sourcing and
appending). Fixed in the same commit (856d8ba).

**Jackson failure type.** Parse failures now surface as `InvalidFormatException` (value, target type, original
`IllegalArgumentException` as cause), asserted by `JsonRoundTripTest`; the serialised JSON is unchanged. A non-string token
reaches the parser as an empty string and is reported the same way instead of throwing a null-pointer error.

## Contract note for the deleted fixture functions

`ClockedUuidV7Generator.nextCourseId`, `nextStudentId` and `IdArbs.studentId` are still listed as kernel fixture API in
`.planning/phases/02-runtime-infrastructure-dcb-spikes/02-04-PLAN.md` (lines 31, 124, 247) and `02-04-SUMMARY.md` (line 127). They
had no caller in any source set and can return when Phase 3 needs them.

## Kotlin language evidence for build-logic

Gradle 9.9.2 embeds Kotlin 2.4.21 (`kotlin-compiler-embeddable-2.4.21-min.jar` in the wrapper distribution's `lib`). Multi-dollar
interpolation and `when` guard conditions are stable since Kotlin 2.2. `:build-logic:compileKotlin` and
`:build-logic:compileFunctionalTestKotlin` accepted `$$"..."`, `$$"""..."""` and `is Text if ...` without warnings under
`allWarningsAsErrors`, and the whole TestKit suite passed.

## Forward concern for Phase 3

Production `@CommandHandler` and `@EventSourcingHandler` methods of Axon 5 are reached only through annotation scanning, so
Qodana's UnusedSymbol is likely to report them as well. The author should decide the approach before the first slice (not
solved here).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Explicit command handler lost the command message in the context**
- **Found during:** Task 1
- **Issue:** `DecisionCaptureTest` failed with `commandType` null after switching the harness to an explicit `CommandHandler`
- **Fix:** `Message.addToContext(context, command)` before sourcing and appending
- **Files modified:** backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CapacityHarness.kt
- **Commit:** 856d8ba

**2. [Plan gate] `TestIds.courseId(` gate needed explicit call lines**
- **Found during:** final gate run
- **Issue:** the distinct-values test used a method reference, so the plan gate counted 2 lines instead of >= 3
- **Fix:** explicit calls with 1, 2, 500 and 9999 for both ID kinds
- **Files modified:** backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdPropertyTest.kt
- **Commit:** 57ec108

### Blocked

- Qodana run could not start its analysis (see "Qodana run: BLOCKED"); not a code deviation.

## Tooling notes

The plugins `axoniq-app-development:axoniq-app-dev` and `gradle-skills:gradle-best-practices` are not installed; execution
continued without them. No package was installed or upgraded.

## Known Stubs

None.

## Threat Flags

None. The mitigations of the plan's threat register hold: no gate configuration changed (T-p1u-01), `runtimeOnly` keeps the
module on every runtime classpath and the integration tests pass (T-p1u-02), the vararg append is captured and tested
(T-p1u-03), TestKit covers the convention change (T-p1u-05).

## Self-Check: PASSED

- Created file present: DecisionCaptureEngineTest.kt.
- Commits present on the branch: 856d8ba, e9933ba, 04b0af1, 57ec108.
- Open item, not a self-check failure: the Qodana result count is unconfirmed because of the connectivity block.
