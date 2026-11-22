# Phase 2: Runtime Infrastructure & DCB Spikes - Pattern Map

**Mapped:** 2026-11-22
**Files analyzed:** 47 new or modified files (grouped below)
**Analogs found:** 43 / 47 (4 have no in-repo analog)

All analog paths were checked with `git ls-files` and are tracked (spike sources under `.planning/spikes/` are tracked;
`.claude/skills/spike-findings-*/sources/` is a tracked copy and is not cited). Line numbers refer to the files as of
this commit.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `settings.gradle.kts` (modify: `include` + `projectDir`, layout comment) | config | n/a | itself, lines 7-14 | exact |
| `platform/build.gradle.kts` (modify: `api(libs.konsist)`, `api(libs.java.uuid.generator)`) | config | n/a | itself, lines 17-21 (`kotlin-stdlib` constraint) | exact |
| `gradle/libs.versions.toml` (modify: konsist, uuid generator, cyclonedx, image refs) | config | n/a | itself, lines 1-40 (`# renovate:` comment style) | exact |
| `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` | config (convention plugin) | batch | `courses.kotlin-library.gradle.kts` + RESEARCH Pattern 1 prototype | exact |
| `build-logic/src/main/kotlin/courses.licences.gradle.kts` (or folded into kotlin-library) | config (convention plugin) | batch | `courses.kotlin-library.gradle.kts` | role-match |
| `build-logic/.../governance/UpstreamRecordCheck.kt` | utility (cacheable check) | file-I/O | `governance/ToolingTableCheck.kt` | exact |
| `build-logic/.../governance/AdrCheck.kt` | utility (cacheable check) | file-I/O | `governance/ToolingTableCheck.kt` | exact |
| `build-logic/.../governance/FrontMatter.kt` (shared parser) | utility | transform | `ToolingTableCheck.kt` line-reader helpers (lines 73-100) | role-match |
| `build-logic/.../governance/DependencyLicenceCheck.kt` | utility (cacheable check) | file-I/O + transform | `governance/ConfigPolicyCheck.kt` (`JsonSlurper`) | role-match |
| `build-logic/.../governance/ImageLiteralCheck.kt`, `GeneratedFileFreshnessCheck.kt` | utility (cacheable check) | file-I/O | `governance/ToolingTableCheck.kt` | role-match |
| `build-logic/src/main/kotlin/courses.governance.gradle.kts` (modify: register new checks) | config | batch | itself, lines 83-90 | exact |
| `build-logic/src/functionalTest/.../governance/*CheckFunctionalTest.kt` (one per new check) | test | request-response (TestKit) | `governance/ToolingTableCheckFunctionalTest.kt` | exact |
| `build-logic/src/functionalTest/.../GradleFixture.kt` (modify: variant applying `courses.test-suites`) | test helper | n/a | itself, lines 20-90 | exact |
| `backend/contracts/kernel/**` (`TypeId`, `TypedId`, ids, `Tags`, `Rejection`, testFixtures) | model / utility | transform | spike 002 `typeid/TypeId.kt`, `kernel/Ids.kt` | exact (port) |
| `backend/contracts/{commands,events,queries}/**` (annotated messages, sealed results) | model | request-response / event-driven | spike 002 `contracts/Messages.kt` | exact (port, D-44 change) |
| `backend/serialization/**` (`:serialization-jackson` module) | utility | transform | spike 002 `json/CoursesJsonModule.kt` | exact (port, D-44 change) |
| `backend/command-service/**` `KotlinTagResolver` | service | transform | spike 002 `KotlinTagResolver.kt` | exact (port) |
| `backend/command-service/**` decision-capture decorator | service | event-driven | spike 001 `capture/EngineCapture.kt` | exact (port) |
| `backend/command-service/**` Axon wiring `ConfigurationEnhancer` (capture + retry) | config | event-driven | spike 001 `Harness.kt` lines 37-47, 69-75 | role-match |
| `backend/command-service/src/integrationTest/**` conflict, round-trip, capture tests | test | request-response | spike 001 `DecisionCaptureTest.kt`, spike 002 `SerializationRoundTripTest.kt` | exact (port) |
| `backend/**/src/test/**` TypeID vector + property tests | test | transform | spike 002 `TypeIdVectorsTest.kt` | exact (port) |
| `tooling/test-support/**` (`AxonServerEnvironment`, `PostgresEnvironment`) | utility | request-response | spike 001 `DecisionCaptureTest.kt` lines 19-25 (container setup) | role-match |
| `tooling/architecture/**` Konsist rules | test | batch | no Konsist code in repo; RESEARCH Pattern 3 | no analog |
| `tooling/doctor/**` (JVM part) | utility | request-response | `VerifyNodeVersion.kt` (task shape) | partial |
| `compose.yaml`, `compose/infra.yaml`, `compose/postgres/init/01-service-databases.sh`, `compose/versions.env` | config | n/a | spike 003 `compose.yaml` | exact (port) |
| `.env.example` | config | n/a | none; D-47 rules | no analog |
| `scripts/doctor.sh`, `scripts/test-doctor.sh`, `scripts/upstream/*.sh` | utility / test | batch | `scripts/test-apply-github-settings.sh`, `scripts/apply-github-settings.sh` | role-match |
| `.github/workflows/ci.yml` (modify: `integration-test`, `doctor`, upstream-advisory jobs; `ci-success` needs) | config | batch | itself (`lint` job lines 80-146, `ci-success` lines 221-233) | exact |
| `renovate.json5` (modify) | config | n/a | itself, lines 175-205 | exact |
| `docs/adr/0001..0004` (normalise), `0005..` (new) | docs | n/a | `docs/adr/0002-codeql-kotlin-analysis.md` | exact |
| `docs/templates/*`, `docs/upstream/*`, `docs/quick-start.md`, `docs/tooling.md` (modify) | docs | n/a | `docs/tooling.md`; ADRs | role-match |
| `mise.toml`, `mise.lock`, `.mise/locks/**` (modify), `.mcp.json` | config | n/a | existing `"npm:renovate"` entry in `mise.toml` | role-match |
| `.claude/settings.json`, `governance/governed-paths.txt`, `governance/dependency-licences.json`, `NOTICE` | config | n/a | existing governed files | role-match |
| `qodana.yaml`, `performance.scenarios`, `docs/performance.md`, `scripts/governance/test-check-pr.sh` (repoint) | config | n/a | themselves | exact |

## Pattern Assignments

### `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` (convention plugin, batch)

**Analog:** `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` (project style), plus the verified
prototype in RESEARCH Pattern 1.

**Style to copy** (kotlin-library lines 9-23, the `:platform` alignment that suites and `testFixtures` inherit
automatically because it matches `*Implementation`):
```kotlin
val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()
val platformProject = dependencies.project(":platform")

configurations
    .matching { it.name == "implementation" || it.name.endsWith("Implementation") }
    .configureEach { this.dependencies.add(project.dependencies.platform(platformProject)) }
```

**Isolated-projects root access** (kotlin-library lines 25-28); reuse for the architecture module's declared inputs and
for catalog image inputs:
```kotlin
@Suppress("UnstableApiUsage") val rootDirectory: Directory = isolated.rootProject.projectDirectory
```

**Existing `Test` defaults to override per suite** (lines 49-52); the new convention sets `maxParallelForks = 1` for
`componentTest` and `integrationTest` only (D-22):
```kotlin
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
```

**Detekt widening** (kotlin-library lines 55-58 define `detekt { ... }`; extend with
`source.from("src/componentTest/kotlin", "src/integrationTest/kotlin", "src/testFixtures/kotlin")`). Precedent:
`source.from("src/functionalTest/kotlin")` in `build-logic/build.gradle.kts`. Also update
`config/detekt/detekt.yml` excludes (`**/componentTest/**`, `**/integrationTest/**`, `**/architectureTest/**`).

**Conventions to keep:** every registered task gets `group` and `description`, is cacheable, and uses lazy APIs
(`checkBuildScripts` rejects eager and cross-project APIs and version literals; image refs and versions come only from
the catalog). Core suite body and the `check` wiring line are in RESEARCH Pattern 1 (copy that block as is;
`useJUnitJupiter(libs.versions.junit)` is the catalog reference, no literal).

**What must change vs. the prototype:** remove the now redundant `testImplementation(libs.junit.jupiter)` and
`testRuntimeOnly(libs.junit.platform.launcher)` from `courses.kotlin-library` only after verifying the launcher is
supplied by `useJUnitJupiter` on Gradle 9.9.2. Add `kotest-property` to suites (D-21), not MockK.

---

### New governance checks: `UpstreamRecordCheck`, `AdrCheck`, `ImageLiteralCheck`, `GeneratedFileFreshnessCheck`, `DependencyLicenceCheck` (cacheable checks, file-I/O)

**Analog:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt`

**Imports + task skeleton** (lines 4-12, 26-36):
```kotlin
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ToolingTableCheck : DefaultTask() {
    /** The pinned tools, `mise.toml`. */
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val miseToml: RegularFileProperty
    /** The file the sorted list of differences is written to; empty when the files agree. */
    @get:OutputFile abstract val report: RegularFileProperty
```
For checks scanning many files (upstream markers, ADRs, image literals), use `ConfigurableFileCollection` with
`@get:InputFiles @get:PathSensitive(RELATIVE)` and feed it from `layout.projectDirectory.asFileTree.matching { ... }`
exactly as `checkBuildScripts` does (`courses.governance.gradle.kts` lines 35-61, including the exclude list for
`build`, `.gradle`, `.planning`, `.claude`, and `build-logic/src/functionalTest/**`).

**Core pattern: collect problems, write the report, throw once** (lines 40-61):
```kotlin
val problems = buildList { (pinned - documented).sorted().mapTo(this) { "missing in $INVENTORY_PATH: $it" } }
report.get().asFile.apply {
    parentFile.mkdirs()
    writeText(problems.joinToString(separator = "\n", postfix = if (problems.isEmpty()) "" else "\n"))
}
if (problems.isNotEmpty()) throw GradleException(failureMessage(problems))
```

**No parser dependency:** parse by lines (lines 23-24 explain). `FrontMatter` is the documented subset parser (flat
`key: scalar`, `key: [a, b]`, quoted or bare, `null`), failing with the line number on anything else (RESEARCH
Pattern 6).

**Registration:** add to `courses.governance.gradle.kts`, copying lines 83-90:
```kotlin
val checkToolingInventory =
    tasks.register<ToolingTableCheck>("checkToolingInventory") {
        group = "verification"
        description = "Fails when mise.toml and docs/tooling.md list different mise tools."
        miseToml.set(layout.projectDirectory.file("mise.toml"))
        report.set(reports.map { it.file("tooling-inventory.txt") })
    }
```
(then wire into `check` like the six existing checks, further down in that file). Per D-45, a `wont-fix` record keeps
its markers; the check must encode that rule.

**Licence check specifics:** use `groovy.json.JsonSlurper` (precedent: `ConfigPolicyCheck.kt`, `AskRulesCheck.kt`);
policy file `governance/dependency-licences.json` is a governed path (needs a new `Edit(/...)` ask rule in
`.claude/settings.json`, enforced by `checkClaudeAskRules`). CycloneDX is applied per module (not root, not
`allprojects`), with `includeConfigs = listOf(".*RuntimeClasspath")`; the verified block is in RESEARCH Pattern 5.

---

### TestKit tests for each new check (`*CheckFunctionalTest.kt`)

**Analog:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheckFunctionalTest.kt`

**Imports and structure** (lines 4-23):
```kotlin
import io.github.aanbrn.courses.buildlogic.GradleFixture
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ToolingTableCheckFunctionalTest {
    @TempDir lateinit var projectDir: Path
    private fun fixture(mise: String, inventory: String): GradleFixture =
        GradleFixture.governance(projectDir).apply {
            file("mise.toml", mise)
            file("docs/tooling.md", inventory)
        }
```
**Pass / fail assertions** (lines 25-40): `result.task(":$TASK")?.outcome shouldBeIn GradleFixture.PASSED` and
`fixture(...).runAndFail(TASK).output shouldContain "missing in docs/tooling.md: zizmor"`. Test class suffix is
`FunctionalTest`; run via `./gradlew :build-logic:functionalTest --tests '*XFunctionalTest*'`. `GradleFixture` already
runs every build with the configuration cache and isolated projects. For `courses.test-suites`, add a
`GradleFixture.testSuites(projectDir)` factory next to `standard`/`governance` that applies the new convention and
includes a module (`include(":lib")`, `file(...)`, `run`, `runAndFail` exist, lines 35-83).

---

### `backend/contracts/kernel/**` TypeID codec and IDs (model/utility, transform)

**Analog:** `.planning/spikes/002-kotlin-tags-jackson-typeid/src/main/kotlin/spike/typeid/TypeId.kt` and `spike/kernel/Ids.kt`

**Codec** (TypeId.kt lines 5-56; port verbatim, spec v0.3.0): `object TypeId` with `ALPHABET`, `DECODE`,
`PREFIX` regex `^([a-z]([a-z_]{0,61}[a-z])?)?$`, `encode(prefix, uuid)`, `decode(value): Pair<String, UUID>` throwing
`IllegalArgumentException` with precise reasons (`"Malformed TypeID '$value': suffix must be 26 characters"`, etc.).

**ID shape and registry** (Ids.kt lines 8-47):
```kotlin
interface TypedId { val uuid: UUID }

abstract class TypedIdCompanion<T : TypedId>(val prefix: String, private val wrap: (UUID) -> T) {
    fun of(uuid: UUID): T = wrap(uuid)
    fun random(): T = wrap(Generators.timeBasedEpochGenerator().generate())
    fun parse(value: String): T {
        val (p, uuid) = TypeId.decode(value)
        require(p == prefix) { "Wrong TypeID prefix in '$value': expected '$prefix', got '$p'" }
        return wrap(uuid)
    }
    fun parseOrNull(value: String): T? = runCatching { parse(value) }.getOrNull()
}

@JvmInline
value class CourseId(override val uuid: UUID) : TypedId {
    override fun toString() = TypeId.encode(PREFIX, uuid)
    companion object : TypedIdCompanion<CourseId>("course", ::CourseId) { const val PREFIX = "course" }
}

object Tags { const val COURSE = CourseId.PREFIX; const val STUDENT = StudentId.PREFIX }
```

**What must change for production:**
- The spike files have no KDoc; this repo has Dokka `failOnWarning` and detekt `UndocumentedPublic*`, so every public
  declaration needs KDoc.
- `random()` calls `Generators.timeBasedEpochGenerator()` per call and takes no clock. D-11 needs a `Clock`-taking
  UUIDv7 generator in `testFixtures`; `java-uuid-generator` is a catalog version plus `:platform` `api` constraint.
- Spike IDs are `CourseId`/`StudentId` demo types; Phase 2 contracts need only what the tests use (D-21 spirit);
  domain IDs are Phase 3.
- Kotlin files here use `package io.github.aanbrn.courses...` (Phase 1 D-01) and `// SPDX-License-Identifier: MIT`
  header (every existing `.kt`/`.kts` file has it, enforced by Spotless licence-header check), not `package spike`.
- Replace spike `println` calls and `@Suppress` hacks in tests with assertions only.

---

### `backend/contracts/{commands,events,queries}/**` (model)

**Analog:** `.planning/spikes/002-kotlin-tags-jackson-typeid/src/main/kotlin/spike/contracts/Messages.kt`

**Annotations and imports** (lines 3-22; proven import paths for 5.4.2):
```kotlin
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.messaging.commandhandling.annotation.Command
import org.axonframework.messaging.eventhandling.annotation.Event

@Event(namespace = "courses", name = "CourseCreated", version = "1")
data class CourseCreated(@EventTag(key = Tags.COURSE) val courseId: CourseId, val name: String)

@Event(namespace = "courses", name = "StudentSubscribed", version = "1")
data class StudentSubscribed(
    @field:EventTag(key = Tags.COURSE) val courseId: CourseId,
    @get:EventTag(key = Tags.STUDENT) val studentId: StudentId,
)

@Command(namespace = "courses", name = "SubscribeStudent", version = "1")
data class SubscribeStudent(val courseId: CourseId, val studentId: StudentId)
```
The three `@EventTag` use-site forms are all resolved by `KotlinTagResolver`; production should pick one (a plain
constructor property annotation, as in the official Kotlin example) and let Konsist require a registry reference.

**What must change (D-44):** the spike left the sealed result free of Jackson annotations and used a mix-in
(`Messages.kt` lines 24-29). Production puts the annotation on the type, with `jackson-annotations` as `compileOnly`
in the contracts build scripts:
```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "@type")
sealed interface SubscribeResult {
    data object Subscribed : SubscribeResult
    data class Rejected(val code: String, val courseId: CourseId) : SubscribeResult
}
```
(`com.fasterxml.jackson.annotation.JsonTypeInfo`; still the 2.x annotations package under Jackson 3.) Axon
dependencies in contracts are `compileOnly` only (D-04); the contracts build scripts must not use `implementation` for
Axon (text check in the architecture module).

---

### `backend/serialization/**` (`:serialization-jackson`, utility, transform)

**Analog:** `.planning/spikes/002-kotlin-tags-jackson-typeid/src/main/kotlin/spike/json/CoursesJsonModule.kt`

**Core pattern** (lines 20-43):
```kotlin
class CoursesJsonModule : SimpleModule("courses") {
    init { id(CourseId::class.java, CourseId); id(StudentId::class.java, StudentId) }
    private fun <T : TypedId> id(type: Class<T>, companion: TypedIdCompanion<T>) {
        addSerializer(type, object : ValueSerializer<T>() {
            override fun serialize(value: T, gen: JsonGenerator, ctxt: SerializationContext) { gen.writeString(value.toString()) }
        })
        addDeserializer(type, object : ValueDeserializer<T>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T = companion.parse(p.valueAsString)
        })
    }
}
fun coursesJsonMapper(): JsonMapper =
    JsonMapper.builder().addModule(KotlinModule.Builder().build()).addModule(CoursesJsonModule()).build()
```
Imports are all `tools.jackson.*` (databind, `module.kotlin.KotlinModule`, `databind.module.SimpleModule`).

**What must change:** drop the `setMixInAnnotation(SubscribeResult::class.java, SealedTypeInfo::class.java)` line and
the private mix-in interface (D-44; record the mix-in as the rejected alternative in the ADR). The module depends on the
kernel only, so it cannot name the per-command sealed results. Register the mapper as the Axon converter:
`DelegatingGeneralConverter(JacksonConverter(mapper))` (see command-service wiring below).

---

### `backend/command-service/**` Axon wiring (service/config, event-driven)

**Analogs:** `.planning/spikes/002-kotlin-tags-jackson-typeid/src/main/kotlin/spike/KotlinTagResolver.kt`,
`.planning/spikes/001-decision-record-capture/src/main/kotlin/spike/capture/EngineCapture.kt`,
`.planning/spikes/001-decision-record-capture/src/main/kotlin/spike/Harness.kt`

**KotlinTagResolver** (spike 002, lines 15-28, port as is, add KDoc and the UPSTREAM marker for UP-0004):
```kotlin
class KotlinTagResolver : TagResolver {
    override fun resolve(event: EventMessage): Set<Tag> {
        val payload = event.payload() ?: return emptySet()
        return payload::class.memberProperties.flatMap { prop ->
            val annotations = buildList {
                addAll(prop.annotations.filterIsInstance<EventTag>())
                prop.javaField?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
                prop.javaGetter?.getAnnotationsByType(EventTag::class.java)?.let { addAll(it) }
            }
            if (annotations.isEmpty()) return@flatMap emptyList()
            val value = prop.getter.call(payload) ?: return@flatMap emptyList()
            annotations.distinct().map { Tag.of(it.key.ifBlank { prop.name }, value.toString()) }
        }.toSet()
    }
}
```

**Capture decorator** (spike 001 `EngineCapture.kt`): class `EngineCapture(delegate: EventStorageEngine, recorder)`
implementing `EventStorageEngine by delegate`, overriding `source(...)` (records `condition.criteria()` and sourced
events via `stream.onNext`), `appendEvents(...)` (lines 32-48: records criteria, `consistencyMarker`, appended events
with tags, then `exceptionallyCompose { diagnose(...) }` and wraps in `Tx`), an inner `Tx` delegating
`AppendTransaction by tx` that diagnoses at `commit()` and records the marker in `afterCommit`, and `diagnose(...)`
that unwraps to `AppendEventsTransactionRejectedException` via `generateSequence(failure) { it.cause }` and re-sources
with `SourcingCondition.conditionFor(condition.consistencyMarker().position(), condition.criteria())`. Imports are the
`org.axonframework.eventsourcing.eventstore.*` block at lines 3-14 of that file.

**Wiring + retry** (spike 001 `Harness.kt` lines 37-47 and 69-75; production moves these into one `ConfigurationEnhancer`
per D-43, RESEARCH "Decision capture" bullets):
```kotlin
fun conflictRetryScheduler(maxRetries: Int = 10): RetryScheduler =
    AsyncRetryScheduler(
        MaxAttemptsPolicy(
            FilteringRetryPolicy(ExponentialBackOffRetryPolicy(5)) { t ->
                generateSequence(t) { it.cause }.any { it is AppendEventsTransactionRejectedException }
            }, maxRetries),
        Executors.newScheduledThreadPool(4))
// registration
cr.registerDecorator(EventStorageEngine::class.java, 0) { _, _, d -> EngineCapture(d, recorder) }
cr.registerDecorator(CommandBus::class.java, Int.MIN_VALUE + 10) { _, _, d -> RetryingCommandBus(d, conflictRetryScheduler()) }
cr.registerComponent(GeneralConverter::class.java) {
    DelegatingGeneralConverter(JacksonConverter(JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()))
}
```
Also register `KotlinTagResolver` as the `TagResolver` component and entities as
`EventSourcedEntityModule.autodetected(UUID::class.java, X::class.java)` (unboxed id type, spike 002 `EntityPath.kt`).

**What must change for production:**
- The spike configures through `EventSourcingConfigurer.create()` in a harness; the production unit is a
  `ConfigurationEnhancer` (`@Bean` later), still a plain Kotlin library in Phase 2 (D-43).
- Do not also register a `RetryScheduler` component.
- Replace `Executors.newScheduledThreadPool(4)` with a lifecycle-managed executor, replace `println` dump helpers, and
  mark each workaround `// UPSTREAM: UP-NNNN` (UP-0001 capture decorator on `@Internal` engine, UP-0002 `CompletionException`
  unwrapping in `diagnose`, UP-0004 tag resolver, UP-0005 UUID registration). The decorator uses an `@Internal` API,
  so keep the capture test failing loudly on Axon updates (D-13).
- The spike `DecisionRecorder`/`Draft`/`DecisionRecord` types (`capture/DecisionRecord.kt`, `Draft.kt`) are the
  recorder model; port only what the permanent test asserts, as production-quality documented code.

---

### Permanent Axon Server tests (INFRA-04 conflict, INFRA-06 round trip, decision capture)

**Analogs:** `.planning/spikes/001-decision-record-capture/src/test/kotlin/spike/DecisionCaptureTest.kt`,
`.planning/spikes/002-kotlin-tags-jackson-typeid/src/test/kotlin/spike/SerializationRoundTripTest.kt`

**Container + lifecycle** (DecisionCaptureTest lines 19-25), to be replaced by the test-support singleton (see below):
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecisionCaptureTest {
    private val server =
        AxonServerContainer("docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot").withDevMode(true).withDcbContext(true)
    @BeforeAll fun start() = server.start()
```
**Concurrency pattern, must keep** (lines 73-84): `Executors.newFixedThreadPool(12)`, `CompletableFuture.supplyAsync`
per subscription, 3-seat course, then assertions `results.count { it == Subscribed } shouldBe 3`,
`recorder.records.count { it.outcome is Outcome.Committed } shouldBe 3`, and every attempt greater than 1 preceded by a
conflict of the same command id. Assertions use kotest matchers (`io.kotest.matchers.shouldBe`) with JUnit `@Test`.

**What must change:** class names get the suite suffix (`*IntegrationTest`, e.g. `ConditionalAppendIntegrationTest`);
the image literal on line 22 and `println` calls go away (images come from the catalog via the task's system property,
INFRA-09, and a Konsist rule forbids literals in `DockerImageName.parse`/`*Container(`); `@ParameterizedTest` over
`Variant`/`in-memory` is dropped (production has only the engine variant; in-memory belongs in the unit `test` suite);
data are fresh TypeIDs per test (`CourseId.random()`, D-16), never a purge. Dispatch must come from a pool (a
single-thread dispatch yields 0 conflicts). Property tests under the JUnit runner must be
`fun x() { runBlocking<Unit> { checkAll(...) } }`; the expression-body form `= runBlocking { checkAll(...) }`
silently skips the test (RESEARCH, Pitfalls). Also support `-Pcourses.axonserver.external=localhost:8124` for the host
scenario (D-46).

**Vector test analog:** `TypeIdVectorsTest.kt` lines 13-52 (loads `valid.yml` (9) and `invalid.yml` (21) from
`src/test/resources`, round-trips 100,000 random UUIDs with `Random(42)`, asserts `id.uuid.version() shouldBe 7`).
It reads YAML with `tools.jackson.dataformat.yaml.YAMLMapper`, which needs `jackson-dataformat-yaml` as a test
dependency (add it to the catalog without a version, BOM-governed) or parse with a tiny line reader. Replace the
`println` lines 28 and 35 with assertions on the counts.

---

### `tooling/test-support/**` (`AxonServerEnvironment`, `PostgresEnvironment`; utility, request-response)

**Analog:** spike 001 test lines 19-25 (container configuration) and RESEARCH Pattern 2. There is no singleton in the
repo.

**Pattern:** Kotlin `object`s that lazily start one container per JVM from image references in system properties set by
`courses.test-suites` (constants for the property names in one place each in this module and the convention).
Use `io.axoniq.framework.testcontainer.AxonServerContainer(image).withDevMode(true).withDcbContext(true)` (never
`SharedAxonServerContainer`, which hard-codes `:latest`) and
`org.testcontainers.postgresql.PostgreSQLContainer(DockerImageName.parse(ref).asCompatibleSubstituteFor("postgres"))`
(package name `org.testcontainers.postgresql` is unconfirmed, verify by compiling in Wave 0). Add its own
`componentTest` / `integrationTest` smoke tests (`SELECT 1`, Axon Server health) so the singletons are exercised
(D-21). No `@ServiceConnection`/`spring-boot-testcontainers` in Phase 2.

---

### `tooling/architecture/**` (Konsist, test, batch) - no analog

Use RESEARCH Pattern 3 (verified against Konsist 1.0.0-beta.1 sources): `Konsist.scopeFromDirectory("backend")`,
`strict = true` on non-empty scopes plus a scope sanity meta test, declared task inputs
(`fileTree(rootDirectory) { include("backend/**/src/**/*.kt", ...) }.withPathSensitivity(RELATIVE)`, using the same
`isolated.rootProject.projectDirectory` idiom as `courses.kotlin-library.gradle.kts` line 28). Test classes end in
`ArchitectureTest`. The suite is registered as `architectureTest` (a fourth suite in this module only).

---

### `tooling/doctor/**` and `scripts/doctor.sh` (utility, batch)

**Gradle part analog:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt`.
Copy the doc style and the failure-message form that always names the exact fix (lines 49-53):
```kotlin
throw GradleException(
    "Node.js $actual is active, but .nvmrc requires $expected. Run `mise install` " +
        "locally, or use setup-node with `node-version-file: .nvmrc` in CI.",
)
```
`./gradlew doctor` is not cacheable: use `@DisableCachingByDefault(because = "reads the environment")` (line 28) or
`outputs.upToDateWhen { false }`, plus a `group` and a `description`. The JVM probe is
`DockerClientFactory.instance().client()`; `ExecutableOnPath.kt` is the binary-lookup helper to reuse for the
formatter/binary checks (`VerifyFormatterBinary.kt` has the same shape).

**Shell part analog:** `scripts/apply-github-settings.sh` and `scripts/test-apply-github-settings.sh`
(header `#!/usr/bin/env bash`, `# SPDX-License-Identifier: MIT`, `set -euo pipefail`,
`SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)`, `TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT`, and a fake
binary placed first on `PATH` to test what the script reads and never writes). `doctor.sh` tests (`scripts/test-doctor.sh`)
follow that fake-command model; CI auto-runs every tracked `scripts/test-*.sh` and `scripts/*/test-*.sh`
(`ci.yml` `lint` job, "Run shell tests" step) and shellchecks all `*.sh`. Never echo `.env` content; test only the
presence and emptiness of `AXONIQ_PLATFORM_AUTHENTICATION` (D-47: empty is a failure, absent is a warning).

---

### `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/01-service-databases.sh` (config)

**Analog:** `.planning/spikes/003-axon-server-compose-dcb-hostname/compose.yaml` (lines 1-35)

**Axon Server service to copy** (lines 5-26; proven):
```yaml
axonserver:
  image: docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot
  hostname: axonserver
  environment:
    AXONIQ_AXONSERVER_STANDALONE_DCB: "true"
    AXONIQ_AXONSERVER_DEVMODE_ENABLED: "true"
  healthcheck:
    test: ["CMD", "curl", "-fsS", "-o", "/dev/null", "http://localhost:8024/actuator/health"]
    interval: 5s
    timeout: 3s
    retries: 30
    start_period: 10s
  ports: ["8024:8024", "8124:8124"]
  volumes:
    - as-data:/axonserver/data
    - as-events:/axonserver/events
    - as-log:/axonserver/log
```
**What must change:**
- `image:` becomes `${AXONSERVER_IMAGE:?run ./gradlew generateComposeVersions}` (no image literals, INFRA-09; a
  check enforces this); the root `compose.yaml` has `name: courses` and
  `include: [{path: compose/infra.yaml, env_file: compose/versions.env}]`.
- The spike's `env_file: ./platform.env (required: false)` (lines 14-17) is replaced per D-47 by a bare
  `- AXONIQ_PLATFORM_AUTHENTICATION` entry under `environment:` (omitted when unset). Switch `environment:` to list form
  for that entry or use `AXONIQ_PLATFORM_AUTHENTICATION:` null form; verify with `docker compose config`.
- Add `AXONIQ_UPDATE_CHECKER_DISABLED: "true"`.
- Postgres 18: mount the volume at `/var/lib/postgresql`, healthcheck `pg_isready`, init script mounted at
  `/docker-entrypoint-initdb.d`, shellcheck and shfmt clean.
- The spike `probe` service (profile `probe`, lines 27-35) is the model for an optional network-reachability check
  under a `profiles:` entry, using a curl image instead of a spike jar.
- No separate init container (double init corrupts the Raft group).

---

### `.github/workflows/ci.yml` (modify)

**Analog:** itself. Copy the `lint` job skeleton (lines 80-100) for new jobs: `runs-on: ubuntu-latest`,
`timeout-minutes`, job-level `permissions: contents: read` (workflow level stays `permissions: {}`),
`actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1` with `persist-credentials: false`, and
`MISE_DISABLE_TOOLS: java,node` with `setup-node` on `.nvmrc`. Add `integration-test` (D-20:
`./gradlew integrationTest --console=plain`), a `doctor` job (`scripts/doctor.sh --ci`), and the UPS-04 advisory job
(read-only, summary + `::warning` only), and extend `ci-success` (lines 221-233): `needs: [build, lint, secrets, qodana, ...]`
with the existing `jq -e 'all(.[]; .result == "success")'` check. Change `build` to `./gradlew build -x integrationTest`.
Add `docker compose config --quiet` to the `lint` job. Pin any new action by commit SHA with a version comment (zizmor
and actionlint run in `lint`). `.github/**` is governed, so the PR needs `governance-approved`.

---

### `renovate.json5` (modify)

**Analog:** itself. For catalog image refs, copy the existing Qodana-image custom manager (`linter: <name>:<tag>@sha256:...`)
for `image-axonserver` and `image-postgres` lines in `gradle/libs.versions.toml`; the catalog comment style is
`# renovate: datasource=maven depName=com.facebook:ktfmt` (libs.versions.toml lines 5-6). For MCP tools, the mise
manager rules are at lines 175-205: add a `packageRules` entry disabling `automerge` for `@playwright/mcp` and
`@upstash/context7-mcp` (the "Automerge patch updates of mise tools" rule at line ~190 would otherwise merge their
`0.0.N` releases). `renovate-config-validator --strict` runs in `lint`.

---

### `docs/adr/*.md` (docs) and `docs/templates/adr.md`

**Analog:** `docs/adr/0002-codeql-kotlin-analysis.md` (current pre-normalisation shape).

Today's layout (lines 1-7 and later): `# ADR-0002: ...` title, `- Status: accepted`, `- Date: 2026-11-21`,
`- Deciders: repository author (decision D-24)`, then "This is a minimal record..." note, then `## Context`, `## Spike`,
`## Decision`, `## Re-enable criteria`. Normalise to MADR 4 with YAML front matter (`status`, `date`, `decision-makers`
required; Prettier-stable double-quoted strings), keep the `# ADR-NNNN: <title>` heading, then `## Context and Problem
Statement`, `## Considered Options`, `## Decision Outcome` (+ `### Consequences`, `### Confirmation`),
`## More Information`. Mapping: Decision to Decision Outcome, Re-enable criteria to Confirmation, rejected alternatives
to Considered Options; drop the "minimal record" note; keep content, dates and decisions unchanged. New ADRs 0005..
follow the same shape; the `AdrCheck` enforces keys, statuses, numbering, sections and superseded links.

---

### `docs/tooling.md`, `mise.toml`, `mise.lock`, `.mcp.json` (config)

**Analog:** the `"npm:renovate"` entry in `mise.toml` (`{ version = "44.252.8", trust_policy_excludes = [...] }`, with an
`aube` sidecar lock under `.mise/locks/**`) and the inventory table in `docs/tooling.md` (lines 19-30: a row's Tool
cell carries the exact `mise.toml` key in backticks, `Version source` is `mise`). Each new `[tools]` key
(`"npm:@upstash/context7-mcp"`, `"npm:@playwright/mcp"`) needs one inventory row, otherwise `checkToolingInventory` fails
(see `ToolingTableCheck.kt` lines 41-48). Run `mise lock --platform linux-x64,linux-arm64,macos-arm64,macos-x64` after
adding. `.mcp.json` content: RESEARCH Pattern 9 (no `env` block, no `${VAR}` references); `.mcp.json` is already a
governed path (`governed-paths.txt` line 54) with an `ask` rule. Install only after the AIDEV-04 human approval
checkpoint.

---

### `settings.gradle.kts`, `platform/build.gradle.kts`, `qodana.yaml`, `performance.scenarios` (modify)

**Analog:** the files themselves.

`settings.gradle.kts` lines 7-14 today:
```kotlin
include(
    ":platform",
    ":tooling:convention-sample",
)
// Anticipated role-based layout (D-03); each phase adds the modules it needs:
//   services/{command,query,automation,bff}, contracts/*, frontend, docs-site
```
Replace with flat names mapped by `project(":contracts-kernel").projectDir = file("backend/contracts/kernel")` etc., keep
`:tooling:*` nested, rewrite the comment to `backend/...` (D-01), and drop `convention-sample` once the references in
`qodana.yaml` (lines 30-32), `performance.scenarios` (line 28, `apply-abi-change-to`), `docs/performance.md` (line 75),
and `scripts/governance/test-check-pr.sh` (lines 861, 894) are repointed. `platform/build.gradle.kts` lines 17-21
(`constraints { api(libs.kotlin.stdlib) }`) is the model for the new `api(libs.konsist)` and
`api(libs.java.uuid.generator)` constraints. `settings.gradle.kts` is a governed path.

---

## Shared Patterns

### SPDX header and KDoc on everything
**Source:** every file under `build-logic/` (e.g. `ToolingTableCheck.kt` line 1 `// SPDX-License-Identifier: MIT`, KDoc on
every class and public member). **Apply to:** all new `.kt`, `.kts`, `.sh` files (`.sh`: `# SPDX-License-Identifier: MIT`
on line 2 after the shebang). The Dokka `failOnWarning` and detekt `UndocumentedPublic*` rules make this mandatory.
Spike code has neither header nor KDoc; add both on port.

### Versions only from the catalog
**Source:** `courses.kotlin-library.gradle.kts` (`libs` accessor), `gradle/libs.versions.toml`. **Apply to:** all
build scripts. `checkBuildScripts` rejects version literals; spike `build.gradle.kts` files (BOM-governed, no versions,
e.g. spike 001 lines 3-12) are the right dependency style but use string coordinates, where production uses catalog
accessors (`libs.axon.eventsourcing`, ...).

### Cacheable check + report + single GradleException
**Source:** `ToolingTableCheck.kt` lines 26-71. **Apply to:** all new build-logic checks (see above).

### Test class suffix = suite name
**Source:** `ToolingTableCheckFunctionalTest` (`*FunctionalTest`); established convention `*Test`, `*ComponentTest`,
`*IntegrationTest`, `*ArchitectureTest`. **Apply to:** all new test classes, enforced by a Konsist rule.

### Workaround markers
**Source:** RESEARCH Pattern 6 (`UPSTREAM: UP-NNNN` comment next to the workaround). **Apply to:** `KotlinTagResolver`,
capture decorator, `diagnose`, `UUID` entity-id registration, `.env.example`/compose token line, `doctor` token check,
`build-logic/build.gradle.kts` line 14 typed-`libs` workaround, `mise.toml` `trust_policy_excludes`.

### Workflow hygiene
**Source:** `.github/workflows/ci.yml` (`permissions: {}` at workflow level, job-level grants, SHA-pinned actions,
`persist-credentials: false`). **Apply to:** all workflow edits.

### Governed paths
Edits to `settings.gradle.kts`, `.github/**`, `mise.toml`, `.mcp.json`, `governance/**`, `.claude/settings.json`,
`renovate.json5`, `gradle/libs.versions.toml`-adjacent files listed in `governance/governed-paths.txt` need the
`governance-approved` label; adding a governed entry also needs a matching `Edit(/...)` rule in `.claude/settings.json`
(`checkClaudeAskRules`).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `tooling/architecture/**` (Konsist rules) | test | batch | No Konsist code in the repository; use RESEARCH Pattern 3 |
| `.env.example` | config | n/a | No env example exists; follow D-47 (token line commented out, never empty) and `.gitignore` (`!.env.example` is already allowed) |
| `docs/templates/{doc-page,event-model-slice,upstream-record,upstream-issue}.md` | docs | n/a | No template exists; ADR template follows MADR 4, upstream record follows RESEARCH Pattern 6 front matter |
| `scripts/upstream/changed-dependencies.sh` | utility | batch | Only analog is the `gh`-faking shell test style of `scripts/test-apply-github-settings.sh`; logic from RESEARCH Pattern 6 (UPS-04) |

## Metadata

**Analog search scope:** `build-logic/` (main and functionalTest), `platform/`, `settings.gradle.kts`,
`gradle/libs.versions.toml`, `.github/workflows/ci.yml`, `renovate.json5`, `scripts/`, `docs/`, `.planning/spikes/001..005`
**Files scanned:** about 40 (tracked source files read in full or in the cited ranges)
**Pattern extraction date:** 2026-11-22
