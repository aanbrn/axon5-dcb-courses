---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-23T00:00:00Z
depth: standard
files_reviewed: 142
files_reviewed_list:
  - .claude/settings.json
  - .env.example
  - .github/workflows/ci.yml
  - .github/workflows/codeql.yml
  - .gitignore
  - .mcp.json
  - REUSE.toml
  - backend/command-service/build.gradle.kts
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/AxonServerSerializationIntegrationTest.kt
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt
  - backend/command-service/src/integrationTest/resources/logback-test.xml
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngine.kt
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionRecord.kt
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionRecorder.kt
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/retry/ConflictRetry.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt
  - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CapacityHarness.kt
  - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CollectingDecisionRecorder.kt
  - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt
  - backend/contracts/commands/build.gradle.kts
  - backend/contracts/commands/src/main/kotlin/io/github/aanbrn/courses/contracts/commands/SubscribeStudent.kt
  - backend/contracts/events/build.gradle.kts
  - backend/contracts/events/src/main/kotlin/io/github/aanbrn/courses/contracts/events/CourseEvents.kt
  - backend/contracts/kernel/build.gradle.kts
  - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/Ids.kt
  - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/MessageNamespace.kt
  - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/Rejection.kt
  - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/Tags.kt
  - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeId.kt
  - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/TypedId.kt
  - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/KernelContractTest.kt
  - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdPropertyTest.kt
  - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdVectorsTest.kt
  - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypedIdTest.kt
  - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/ClockedUuidV7Generator.kt
  - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/IdArbs.kt
  - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/TestIds.kt
  - backend/contracts/queries/build.gradle.kts
  - backend/serialization-jackson/build.gradle.kts
  - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
  - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
  - build-logic/build.gradle.kts
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ContainerImageInputsFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/TestSuitesFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GeneratedFileCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/NoticeCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/courses.architecture-tests.gradle.kts
  - build-logic/src/main/kotlin/courses.governance.gradle.kts
  - build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
  - build-logic/src/main/kotlin/courses.licences.gradle.kts
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/main/kotlin/courses.test-suites.gradle.kts
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ContainerImageArguments.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GeneratedFileCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicenceExpression.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/NoticeCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt
  - compose.yaml
  - compose/infra.yaml
  - compose/postgres/init/01-service-databases.sh
  - compose/versions.env
  - config/detekt/detekt.yml
  - docs/adr/0001-workflow-permissions-and-required-checks.md
  - docs/adr/0002-codeql-kotlin-analysis.md
  - docs/adr/0003-governance-gate-trust-boundary.md
  - docs/adr/0004-gsd-configuration.md
  - docs/adr/0005-axon-server-licence-and-platform-token.md
  - docs/adr/0006-decision-record-capture.md
  - docs/adr/0007-kotlin-ids-tags-and-serialization.md
  - docs/adr/0008-container-images-with-buildpacks.md
  - docs/adr/0009-repository-layout-and-project-naming.md
  - docs/adr/0010-test-levels-as-jvm-test-suites.md
  - docs/adr/0011-local-runtime-and-doctor.md
  - docs/adr/0012-dependency-licence-policy.md
  - docs/adr/0013-upstream-records-and-workaround-markers.md
  - docs/adr/0014-claude-code-skills-and-mcp-servers.md
  - docs/performance.md
  - docs/quick-start.md
  - docs/templates/adr.md
  - docs/templates/doc-page.md
  - docs/templates/event-model-slice.md
  - docs/templates/upstream-issue.md
  - docs/templates/upstream-record.md
  - docs/tooling.md
  - docs/upstream/0001-decision-capture-extension-point.md
  - docs/upstream/0002-conflict-diagnosis-completion-exception.md
  - docs/upstream/0003-remote-conflict-classification.md
  - docs/upstream/0004-tag-resolver-value-classes.md
  - docs/upstream/0005-target-entity-id-value-classes.md
  - docs/upstream/0006-empty-platform-token.md
  - docs/upstream/0007-shared-testcontainer-latest-image.md
  - docs/upstream/0008-precompiled-plugin-typed-accessors.md
  - docs/upstream/0009-libzip-missing-provenance.md
  - docs/upstream/0010-codeql-kotlin-extractor.md
  - docs/upstream/0011-colima-aaaa-dns.md
  - docs/upstream/0012-docker-containerd-platform-export.md
  - docs/upstream/0013-axon-server-dcb-context-flag.md
  - governance/dependency-licences.json
  - governance/governed-paths.txt
  - governance/gsd-config-policy.json
  - gradle/libs.versions.toml
  - mise.toml
  - performance.scenarios
  - platform/build.gradle.kts
  - qodana.yaml
  - renovate.json5
  - scripts/doctor.sh
  - scripts/governance/test-check-pr.sh
  - scripts/test-doctor.sh
  - scripts/upstream/changed-dependencies.sh
  - scripts/upstream/test-changed-dependencies.sh
  - settings.gradle.kts
  - tooling/architecture/build.gradle.kts
  - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt
  - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContractsArchitectureTest.kt
  - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/MessageArchitectureTest.kt
  - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ScopeSanityArchitectureTest.kt
  - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/TestNamingArchitectureTest.kt
  - tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/ArchitectureScopes.kt
  - tooling/doctor/build.gradle.kts
  - tooling/doctor/src/main/kotlin/io/github/aanbrn/courses/doctor/Doctor.kt
  - tooling/test-support/build.gradle.kts
  - tooling/test-support/src/componentTest/kotlin/io/github/aanbrn/courses/testsupport/PostgresEnvironmentComponentTest.kt
  - tooling/test-support/src/integrationTest/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironmentIntegrationTest.kt
  - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironment.kt
  - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/ContainerImages.kt
  - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/PostgresEnvironment.kt
findings:
  critical: 0
  warning: 6
  info: 5
  total: 11
  necessity: 1
status: issues_found
---

# Phase 2: Code Review Report

**Reviewed:** 2026-11-23
**Depth:** standard
**Files Reviewed:** 142
**Status:** issues_found

## Summary

I reviewed the Kotlin command-service library (capture engine, conflict retry, tag resolver, test fixtures and the three
integration tests), the contracts and serialization modules, the build-logic conventions and governance checks, the
tooling modules, compose, both workflows, the bash tooling and the configuration files. For the ADRs and upstream records
I checked cross-references, links and the facts that can be verified against the code.

Not read line by line, because they are tests of already-read code or prose with enforced structure: most of the
governance TestKit tests, the three shell test scripts (`test-doctor.sh`, `test-changed-dependencies.sh`,
`test-check-pr.sh`), and the ADR and upstream record prose beyond ADR-0006, 0010, 0011, 0012 and UP-0001, UP-0003. The
`gradle-skills:gradle-best-practices` skill is not installed in this environment, so I applied general Gradle knowledge
instead. The necessity rubric (`necessity-review.md`) was read.

No security vulnerability and no data-loss defect was found. Secrets handling is sound: the Platform token is never
printed, compose ports bind to loopback, workflow permissions are minimal and actions are pinned by SHA,
`changed-dependencies.sh` escapes workflow commands, and the postgres init script passes passwords as psql variables.

I tried to break these suspected defects and could not, so they are not findings: the `compose.yaml` `include:` with
`env_file:` still honours a project `.env` (verified with `docker compose config`); `.env` lines of the form
`KEY= # note` are read by Compose as a non-empty value, which matches what `doctor.sh` reports; the TypeID codec
arithmetic and the `TestIds` KDoc example are correct; `MaxAttemptsPolicy` counts retries as `ConflictRetry.kt` documents
(checked against the Axon 5.4.2 sources).

The six warnings are one design limit of the conflict retry that the flagship concurrency scenario will meet, two
fail-open spots in governance checks, one robustness gap against Axon's own tag resolver, and two weaknesses in tests.

## Warnings

### WR-01: Conflict retry has a fixed, unconfigurable bound and no jitter, so a busy course exhausts it

**File:** `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/retry/ConflictRetry.kt:46-58`
(wired at `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt:87-90`)
**Issue:** `conflictRetryScheduler` is built with `DEFAULT_MAX_ATTEMPTS = 10` and the enhancer never passes another value.
In Axon 5.4.2 `ExponentialBackOffRetryPolicy` computes `initialWaitTime << previousFailures.size()` with no jitter and no
cap (read from `ExponentialBackOffRetryPolicy.java`), and `MaxAttemptsPolicy` stops after `maxAttempts` retries. Every
round of contenders on one set of criteria has exactly one winner, which is the property `DecisionCaptureIntegrationTest`
relies on. So when more than about 11 decisions contend for the same still-open course (for example 30 students
subscribing to a 30-seat course at once), the last ones lose 11 rounds in a row. They then fail with the raw
`AppendEventsTransactionRejectedException` (as a `CommandExecutionException`) instead of `Subscribed` or
`Rejected(course-full)`, although each of them would succeed on another retry. The delays also grow to 2.5 s by the tenth
retry. The tests only run 12 contenders against 3 seats, where contention disappears after four rounds because the
course fills, so they cannot show this. The project's headline demo is concurrent subscriptions to one course.
**Fix:** Make the bound a constructor parameter of `CommandServiceConfigurationEnhancer` and pass it to
`conflictRetryScheduler`. Wrap the back-off in a small `RetryPolicy` that randomises the delay (full jitter) and caps it,
for example:
```kotlin
class JitteredBackOff(private val delegate: RetryPolicy, private val random: Random = Random.Default) : RetryPolicy {
    override fun defineFor(message: Message, cause: Throwable, previous: List<Array<Class<out Throwable>>>) =
        delegate.defineFor(message, cause, previous)
        // then replace the Outcome delay with random.nextLong(1, delay + 1), capped at a few hundred milliseconds
}
```
Add an integration test with at least 40 concurrent subscriptions to a 40-seat course that asserts that every command
ends as `Subscribed`.

### WR-02: `forbidden` in the licence policy is advisory, so an allowed entry can match a forbidden family

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt:96-135`
**Issue:** The gate is membership of `allowed` (`isAllowed`, line 74). `forbidden` is used only to word the failure
message (`classify`). `read` rejects an alias that resolves to a forbidden licence (`formatProblems`) but never checks
`allowed` against `forbidden`. A policy edit that adds `"GPL-3.0-only"` to `allowed` is therefore accepted although
`forbidden` lists `GPL-*`. ADR-0012 and the KDoc present the policy as one that "refuses to be weakened", and the file
carries two lists that are meant to agree. The file is a governed path, but this check is the machine-side guard that a
human review should not have to replace.
**Fix:** In `formatProblems`, take `allowed` as a parameter and add
`allowed.filter { id -> forbidden.any { it.matches(id) } }.forEach { add("'$it' is allowed, but matches a forbidden family") }`.
Add a case to `DependencyLicenceCheckFunctionalTest` for it.

### WR-03: `KotlinTagResolver` cannot read a non-public `@EventTag` property, which Axon's resolver can

**File:** `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt:44`
**Issue:** `property.getter.call(payload)` is invoked without `isAccessible = true`. For a `private` (or otherwise
non-public) Kotlin property, kotlin-reflect throws `IllegalCallableAccessException`. Axon's `AnnotationBasedTagResolver`,
which this class replaces (UP-0004), makes fields accessible, so an event payload that tags a private property worked
before the swap and would fail at publish time after it. The failure surfaces in the append path of a command, not at
startup. The current events only use public properties, which is why no test shows it. This is a parity gap in a class
whose documented contract is to be a drop-in replacement.
**Fix:**
```kotlin
import kotlin.reflect.jvm.isAccessible
...
val value = if (annotations.isEmpty()) null else property.apply { isAccessible = true }.getter.call(payload)
```
and add a `KotlinTagResolverTest` case with a payload that annotates a private property. If private properties are
meant to be unsupported, fail with an explicit message in `eventTags` instead.

### WR-04: `ContainerImageArchitectureTest` strips comments in a way that can hide a violation

**File:** `tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt:29-33`
**Issue:** `withoutComments` removes `//[^\n]*` and `/\*.*?\*/` with regexes that ignore string literals. Two
consequences. First, a `//` inside a string, such as `"http://..."`, removes the rest of that line, so a literal image
argument on the same line is not seen (`AxonServerEnvironmentIntegrationTest.kt:21` has such a line today). Second, a
string that contains `/*`, such as a glob like `"**/*.kt"`, starts a block-comment match that runs to the next `*/`
anywhere later in the file and deletes everything between. Both are false negatives of the rule that guards INFRA-09
(no image literal in any test). No current file is affected, so the rule is green, but it is a guard that can fail open
as soon as such a string appears.
**Fix:** Drop the regex pre-pass. Konsist already parses the file: inspect `file.functions()`/`file.expressions` call
sites, or strip comments with a small scanner that tracks string state. As a minimal change, only treat `//` as a
comment start when it is at the start of the line or follows whitespace, and skip `/*` inside a quoted string.

### WR-05: Second `DecisionCaptureIntegrationTest` case can pass without asserting anything

**File:** `backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt:93-110`
**Issue:** `a committed attempt records what it saw...` ignores the results of `subscribeConcurrently` and then loops over
`recorder.committed().forEach { ... }`. If no attempt were recorded as committed (for example because a future Axon
update changes where the outcome is set), the loop body never runs and the test is green. The permanent safeguard of
D-13 (ADR-0006 "Confirmation") should be unable to pass vacuously.
**Fix:** Assert the count before the loop: `recorder.committed() shouldHaveSize SEATS`.

### WR-06: `ImageReferenceCheck` skips catalog image entries that are not a plain double-quoted string

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt:131`
**Issue:** `CATALOG_IMAGE` matches only `image-* = "value"`. An `image-*` key written as a TOML literal string
(`'value'`), an inline table, or a multi-line string does not match, so it is neither checked for the tag and digest nor
reported. The check fails open on exactly the shape it is meant to forbid (an unpinned image). The Renovate custom
manager also only reads the double-quoted form, so such an entry would also never be updated.
**Fix:** Match every `image-*` key and report any value that is not a double-quoted string:
```kotlin
val CATALOG_KEY = Regex("""^(image-[A-Za-z0-9_.-]+)\s*=\s*(.*)$""")
// then: if the value does not match Regex("^\"([^\"]*)\"\\s*(#.*)?$") report "$key must be a double-quoted <name>:<tag>@sha256:<digest>"
```

## Info

### IN-01: Magic number 12 is suppressed with a justification that argues against it

**File:** `backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt:50-63`
**Issue:** The `@Suppress("MagicNumber")` comment says the literal 12 "equals STUDENTS", yet `STUDENTS` is in scope in the
same class. Two constants that must stay equal are stated twice, and the suppression hides a detekt rule that would
otherwise point at it.
**Fix:** `pool = Executors.newFixedThreadPool(STUDENTS)` and delete the suppression and its comment.

### IN-02: Duplicated helpers in build logic and in tests

**File:** `build-logic/src/main/kotlin/courses.root.gradle.kts:222` and
`build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts:27`; `backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt:112,166`
and `backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt:185`
**Issue:** The `.java-version` parsing, including its error message, is written out twice in two convention plugins.
`ConditionalAppendIntegrationTest.SourcingBarrier` is a copy of the fixture `ArrivalBarrier`, and the
`submitDecision` helper (source, wait at the barrier, append) is repeated in two test classes. A later change to one copy
will not reach the other.
**Fix:** Extract a `readJavaMajorVersion(text: String)` function into `io.github.aanbrn.courses.buildlogic`. Use
`ArrivalBarrier` from the command-service test fixtures in `ConditionalAppendIntegrationTest`, and move `submitDecision`
into the fixtures.

### IN-03: Duplicated outline scanner in two governance checks, with a fence toggle that mixes fence types

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt:189-204` and
`build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt:515-530`
**Issue:** `outline` is the same function twice (`AdrHeading`/`Heading` differ only by name). Both toggle `fenced` on
any line that starts with three backticks or three tildes, so a `~~~` line inside a backtick fence closes the fence early
and a heading shown in the rest of the example block is then counted. Rare, but it makes the section-presence rules
depend on content they are meant to ignore.
**Fix:** Move the scanner next to `FrontMatter` and track the opening fence string, closing only on a line that starts
with the same fence.

### IN-04: The upstream advisory can never fire for the record that most needs it

**File:** `scripts/upstream/changed-dependencies.sh:17-24` (design) and `docs/upstream/0012-docker-containerd-platform-export.md:5`
**Issue:** The advisory reports a record only when its `product` equals a changed key from the catalog, `mise.toml`,
the wrapper, `.java-version`, `.nvmrc` or a workflow action. UP-0012 has `status: "fixed-upstream"` and `product: "docker"`,
and UP-0011 has `product: "colima"`. Neither product has a pinned key, so the record whose status says that the
workaround should now be removed is invisible to the mechanism meant to surface it. The script header states that such
products never match, so this is a known limitation, but it has no compensating check.
**Fix:** Either let `doctor.sh` print the removal criteria of any `fixed-upstream` record whose workaround markers still
exist, or record in UP-0012 and UP-0011 that removal has to be noticed by hand.

### IN-05: Necessity (judgment) - the empty `contracts-queries` module and its rule entries have no instantiation yet

**File:** `backend/contracts/queries/build.gradle.kts:3`
**Category:** necessity
**Issue:** The module has no source ("holds no source until the first query slice", line 3 of the file). No `@Query`
message exists anywhere in the repository, so the `Query` entry of `MODULE_OF_MESSAGE_ANNOTATION` and the
`queryhandling` and `modelling` entries of `ArchitectureScopes.allowedAxonPackages` run against nothing, and
`MessageArchitectureTest` itself says no assertion can target the module. This rests on judgment: D-02 mandates the four
modules and an architecture test enforces that exactly four exist, so the cost is small and it is deliberate scaffolding.
**Fix:** No change needed now. If the first query slice slips beyond Phase 4, drop the module and the unused rule entries
until it arrives, or keep them and mention the decision in ADR-0009.

---

_Reviewed: 2026-11-23_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
