# Architecture Research

**Domain:** Axon Framework 5 / DCB event-sourced CQRS system (courses and students), Kotlin + Spring Boot, split by CQRS role, Axon Server as DCB event store and message router, React + TypeScript UI with real-time updates
**Researched:** 2026-11-20
**Confidence:** MEDIUM-HIGH. The AF5 modelling, DCB, event processor and Spring Boot facts were read from Axon source, official docs and the official `university-demo*` examples, so they are HIGH. The multi-service recommendations (consistency-boundary recording, wait-for-projection, subscription-query scale-out, licensing behaviour of the local stack) are MEDIUM or LOW and are flagged inline as spikes.

> Versions verified on 2026-11-20: Axon Framework / Axoniq Framework **5.4.2** (Maven Central, released 2026-11-16), Axon Server **2026.1.8** (Docker Hub `axoniq/axonserver`), Spring Boot **4.1.2** (the official Spring Boot 4 example uses it), Kotlin 2.4.21, Gradle 9.9.2, Testcontainers 2.0.5, Konsist 1.0.0-beta.1, Gatling 3.16.1, Structurizr `structurizr/structurizr` v2026.10.26, LikeC4 1.61.0, Grafana `docker-otel-lgtm` v0.41.1.

---

## 0. Findings That Shape the Whole Architecture

These are the facts most likely to change roadmap decisions. They are verified unless marked otherwise.

1. **The official reference is almost exactly this domain.** AxonIQ's `university-demo` (faculty, courses, students, subscribe, capacity, notifications) now lives in the Axon Framework repo under `examples/`. It includes `university-demo-kotlin` (plain Kotlin, no Spring) and `university-java-springboot-4` (Spring Boot 4.1.2 with Axon Server and Postgres or H2). It uses `write` / `read` / `automation` vertical-slice packages. This project should follow those conventions and go beyond them (multi-service, UI, explorer, waiting list, transfer, schedules). Do not invent a different slice vocabulary.
2. **Axon Server connectivity is in the commercial `io.axoniq.framework` namespace, not in Apache-2.0 `org.axonframework`.** Since 5.1 the `axon-server-connector`, the distributed command/query buses (which carry subscription queries), `AxonServerContainer` (Testcontainers) and the Spring integration all moved. Axoniq's published terms say the commercial modules are free for evaluation and development and need a licence for production. Axoniq Framework 5.3.2+ requests a temporary, machine-bound development permission from Axoniq Platform when it finds no licence and logs a warning every 15 minutes. The project author's Platform account and token cover this. **Consequence:** the multi-service split cannot be built from pure Apache-2.0 artifacts. State this in the README and ADR-001.
3. **Axon Server 2026.0+ without a licence runs 12 hours and then shuts down** (Axoniq Platform licensing reference). A plain `docker compose up` for community users will therefore stop after 12 hours unless a Platform-delivered licence is configured. Throughput limits are also enforced as part of the licence (2026.0.0 release post). How a licence reaches a containerised Axon Server (Platform connection property) was not found in the docs I could read. **This is a Phase-1 spike (LOW confidence).** It affects the compose file, CI and the public quick start.
4. **A DCB context must exist in Axon Server.** The official example compose creates it automatically with the env var `axoniq.axonserver.standalone-dcb: true` (image `axonserver:2026.0.0`, plus `axoniq_axonserver_devmode_enabled: true`). The older manual route is "Create context, tick *DCB context (beta)*" in the UI. Without a DCB context the app fails with `UNAVAILABLE`. Testcontainers: `AxonServerContainer().withDevMode(true)` plus `AxonServerContainerUtils.purgeEventsFromAxonServer(..., DCB_CONTEXT)` to create or reset it.
5. **Axon Server does not record why an append was allowed.** The DCB gRPC API (`dcb.proto`) stores events and their tags, but the `ConsistencyCondition` (criteria and marker) of an append is validated and then discarded. `Source` and `Stream` responses return `SequencedEvent` with **no tags** (tags need a separate `GetTags(sequence)` call). **The event explorer's "consistency boundary of a command" feature is therefore not available from Axon Server. The application must record it at append time** (see section 5). This must be designed in the first command slice, because events appended earlier cannot be back-filled.
6. **There is no `Saga` in AF5.** `Workflows` (5.4) are commercial and described as a preview. The official long-running-process guide recommends, for teams doing Vertical Slice Architecture without Workflows, **automation slices**: an event handler that dispatches a command, with the decision (and therefore idempotency) living in the command handler. Use that for the waiting list.
7. **Business-rule failures should be result objects, not exceptions.** AF5 wraps every exception from a command handler in a generic `CommandExecutionException` (with optional `details`) when it crosses an Axon Server boundary. The official guidance is "use result objects for expected failure". The official demos still throw `IllegalStateException`. Do the better thing.
8. **`AppendEventsTransactionRejectedException` (the DCB optimistic-concurrency failure) extends `AxonNonTransientException`.** It is not retried by default. The concurrency demo needs an explicit retry policy (`AsyncRetryScheduler` with `FilteringRetryPolicy`, `ExponentialBackOffRetryPolicy`, `MaxAttemptsPolicy`). Once it crosses the distributed command bus it may arrive as a generic `CommandExecutionException`, so the filter must be verified end to end (spike, MEDIUM).
9. **Replay is a first-class AF5 feature again (since 5.1).** `resetTokens(...)` works on a stopped processor, and there is a Replay API (`ResetHandler`, `ReplayStatus`). A pooled streaming processor starts, by default, at the first position of the stream. Spring Boot defaults to `JpaTokenStore` if JPA is on the classpath, otherwise `InMemoryTokenStore`. In-memory tokens silently re-process everything on every restart.

---

## Standard Architecture

### System Overview

```
 USERS (demo role switcher: admin / student X; no authentication in v1)
        |
        v
┌────────────────────────────────────────────────────────────────────────┐
│ PRESENTATION   frontend (React + TS SPA, served by nginx)              │
│   REST (JSON, OpenAPI-typed client)          SSE (EventSource)         │
└───────────────┬───────────────────────────────────▲────────────────────┘
                │ /api/**                           │ /api/stream/**
                v                                   │
┌────────────────────────────────────────────────────────────────────────┐
│ EDGE   bff-service (Spring WebFlux, Kotlin)                            │
│   REST controllers + OpenAPI  |  CommandGateway  |  QueryGateway       │
│   subscriptionQuery -> Flux -> SSE  |  error mapping (problem+json)    │
│   NO domain logic, NO database, NO event classes                       │
└──────┬──────────────────────────────────────────▲──────────────────────┘
       │ commands (routed)                        │ queries + subscription queries (routed)
       v                                          │
┌────────────────────────────────────────────────────────────────────────┐
│ MESSAGING + EVENT STORE      Axon Server 2026.1 (DCB context "courses")│
│   command routing | query routing (incl. subscription) | DCB event     │
│   store (tags, criteria, consistency markers) | no token store         │
└──────▲───────────────┬────────────────────────┬─────────────────────────┘
       │               │ events (stream)        │ events (stream)
       │ append w/     v                        v
       │ condition   ┌──────────────────┐   ┌────────────────────────┐
┌──────┴─────────┐   │ query-service    │   │ automation-service     │
│ command-service│   │ projections ->   │   │ event handlers ->      │
│ DCB decisions  │   │ Postgres (+token │   │ commands (via Axon     │
│ (write slices) │   │ store), query    │   │ Server). Own state only│
│ No database    │   │ handlers, event  │   │ from event store       │
│                │   │ log (explorer)   │   │ (+ token store schema) │
└────────────────┘   └───────┬──────────┘   └───────────┬────────────┘
       ▲                     │ SQL                      │ commands back
       └─────────────────────┼──────────────────────────┘ (command-service handles)
                             v
                    ┌─────────────────┐       ┌──────────────────────────┐
                    │ PostgreSQL      │       │ Grafana LGTM (otel-lgtm) │
                    │ query + auto    │       │ OTLP from all services   │
                    └─────────────────┘       └──────────────────────────┘
```

### Component Responsibilities

| Component | Owns | Does NOT own | Typical implementation here |
|-----------|------|--------------|-----------------------------|
| **contracts** (Gradle modules) | Commands, events (with `@EventTag`), queries and results, ids, tag keys. The published language. | Behaviour, Spring, persistence | Pure Kotlin data classes with `@Command` / `@Event` annotations; depends on `axon-messaging` annotations only |
| **command-service** | All DCB decisions. One decision model (`State`) per command, sourced by tag criteria. Appends events with an `AppendCondition`. | Any database, read models, REST | Spring Boot (non-web, actuator only). `@CommandHandler` + `@EventSourced` state per slice |
| **query-service** | Read models in Postgres, tracking tokens, query handlers, subscription-query update emission, **event log for the explorer** | Decisions, commands | Pooled streaming processors, `@EventHandler` projectors, `@QueryHandler`, `QueryUpdateEmitter` |
| **automation-service** | Event-triggered policies (waiting-list promotion). Reads event-sourced state to decide *who*; the command-service decides *whether*. | Invariants, REST, read models | `@EventHandler` + `CommandDispatcher`, pooled streaming processor, token store |
| **bff-service** | The only REST/SSE surface. DTO shaping, OpenAPI, HTTP error mapping, SSE fan-out, demo "race" endpoint, role-switch header handling | Business rules, projection storage | Spring WebFlux + Kotlin coroutines/Reactor, `CommandGateway`, `QueryGateway` |
| **Axon Server** | Event storage (DCB: tags, criteria, markers), command/query routing, subscription query routing | Tokens, read models | Docker image, context in DCB mode |
| **PostgreSQL** | Projection tables + token store for query-service; token store for automation-service | Events | One instance, one schema/database per service |
| **frontend** | Views, optimistic/pending UI, SSE consumption | Rules | Vite + React + TS, generated OpenAPI client |
| **otel-lgtm** | Metrics, logs, traces backend + Grafana | | `grafana/otel-lgtm` single container |

### Why exactly these four backend services

- The command-service has **no database at all**: state is rebuilt from events per command. This is a strong, simple demonstration of "decision state is ephemeral".
- Splitting the write side by entity (course-service, student-service) is out of scope and wrong for DCB. Invariants span entities (capacity + per-student limit + schedule overlap + transfer touch both). One command-service owns every decision.
- Commands, queries and events are all routed by Axon Server, so **no service calls another service over HTTP**. Only the BFF speaks HTTP, and only to browsers.

---

## 1. How AF5 Models DCB Command Handling (verified)

### 1.1 The mechanism

| Concept | AF5 type / annotation | Notes |
|---------|----------------------|-------|
| Event tag | `@EventTag(key = "course")` on event properties (key defaults to the property name); or a custom `TagResolver` | An event can have 0..N tags. Every property that should be a tag needs the annotation. |
| Event type name | `@Event(name = "...")` on the event class | Give every event an explicit stable name. It is the identity used in criteria and the event store. |
| Criteria | `EventCriteria.havingTags(Tag.of(k, v)).andBeingOneOfTypes(...)`; combine alternatives with `EventCriteria.either(...)` | Within one criterion, **all tags must match** and **types are OR-ed**. Across criteria in `either`, any match counts. |
| Decision model | An `@EventSourced` (Spring) / `@EventSourcedEntity` (plain) class with `@EntityCreator` constructor and `@EventSourcingHandler` methods | State holds only what the rules need. Handlers may mutate or return a new instance (records are fine). |
| Boundary declaration | A static `@EventCriteriaBuilder` method taking the entity id and returning `EventCriteria` (or a `CriteriaResolver`) | **This method is the consistency boundary.** Without it AF falls back to the `tagKey` (single-tag, aggregate-like) or the class simple name. |
| Entity id | `@TargetEntityId` on a command property or method (can be private); composite ids such as `SubscriptionId(studentId, courseId)` are normal | Used to call the criteria builder. |
| Handler | `@CommandHandler fun handle(cmd, @InjectEntity state: State, appender: EventAppender)` | `EventAppender.append(events)` stages events; they are published only if the handler succeeds. |
| Consistency marker | `ConsistencyMarker`, returned as the final entry when sourcing | Handled automatically. |
| Append condition | `AppendCondition` = criteria + marker | If any event matching the criteria exists **after** the marker, the append is rejected (`AppendEventsTransactionRejectedException`). |

What happens in one command (read from `DefaultEventStoreTransaction`):

1. `@InjectEntity` triggers `source(SourcingCondition)`. The framework records the criteria in the `ProcessingContext` as the future append condition. If several entities are injected, their criteria are OR-ed and the **lowest** marker wins (the safe choice).
2. The handler decides and calls `eventAppender.append(...)`. Events are tagged by the `TagResolver`.
3. In the `APPEND_EVENTS` phase (before commit) the engine appends with the resolved `AppendCondition`. Axon Server validates it atomically against the DCB store.
4. Conflict means `AppendEventsTransactionRejectedException` (non-transient). `ConflictDiagnosingEventStorageEngine` (`@Internal`) can enrich it with the first conflicting event.
5. If the entity was created without sourcing (creational command) but the event carries tags, an `ORIGIN`-marker condition is created automatically. Two concurrent "create course X" commands cannot both succeed.

Release 5.3 note: `@InjectEntity` on a **creational** handler can throw `EntityNotFoundException`, so such parameters must be nullable (`State?` in Kotlin).

### 1.2 Kotlin slice (pattern from the official `university-demo-kotlin`, Spring flavour from `university-java-springboot-4`)

```kotlin
// contracts/commands
@Command
data class SubscribeStudentToCourse(val studentId: StudentId, val courseId: CourseId) {
    @TargetEntityId
    val subscriptionId: SubscriptionId = SubscriptionId(studentId, courseId)
}

// command-service/.../faculty/write/subscribestudent/SubscribeStudentToCourse.kt
@Component
internal class SubscribeStudentToCourseHandler {
    @CommandHandler
    fun handle(cmd: SubscribeStudentToCourse, @InjectEntity state: State, appender: EventAppender) =
        appender.append(state.decide(cmd))
}

@EventSourced(idType = SubscriptionId::class)       // Spring Boot; plain AF uses @EventSourcedEntity
internal class State @EntityCreator constructor() {
    companion object {
        @JvmStatic @EventCriteriaBuilder
        fun criteria(id: SubscriptionId): EventCriteria = EventCriteria.either(
            EventCriteria.havingTags(Tag.of(Tags.COURSE, id.courseId.value))
                .andBeingOneOfTypes(/* CourseCreated, CourseCapacityChanged, StudentSubscribedToCourse, ... */),
            EventCriteria.havingTags(Tag.of(Tags.STUDENT, id.studentId.value))
                .andBeingOneOfTypes(/* StudentEnrolled, StudentSubscribedToCourse, ... */),
        )
    }
    // minimal counters + @EventSourcingHandler evolve(...) methods
    fun decide(cmd: SubscribeStudentToCourse): SubscribeResult { /* return sealed result, do not throw */ }
}
```

Design rules to adopt:

- **One `State` class per command slice, never shared across slices.** The same events are folded differently by different slices. Excluding irrelevant types (for example `CourseRenamed` from the subscribe criteria) is what lets unrelated commands run concurrently without conflict.
- **`decide()` is a pure function of `State` and the command.** Unit-testable with no Axon at all, and again through `AxonTestFixture`.
- **Return a sealed result for expected outcomes** (`Subscribed`, `Waitlisted`, `Rejected(reason)`). Reserve exceptions for bugs and infrastructure (finding 7).
- **Transfer (student moves A to B)**: one slice, id `TransferId(studentId, fromCourse, toCourse)`, criteria is an `either` over the two course tags plus the student tag. One decision, one append of two events, one atomic condition.
- **Unique course name**: tag the name (`courseName`) on `CourseCreated` and `CourseRenamed`. The slice criteria is `havingTags(courseName = X)` over those two types. Rename releases the old name by tagging `CourseRenamed` with both the new and the old name, so the fold toggles taken/free (modelling detail for the features phase).
- **Alternative (not recommended as default)**: multiple `@InjectEntity` parameters per handler. Supported, with criteria OR-ed, but it hides the boundary across several types. Prefer one decision model per slice so the boundary is one readable method.

### 1.3 Concurrency handling at the edge

- Retry on `AppendEventsTransactionRejectedException` at the **dispatching side** (BFF `CommandGateway` and automation `CommandDispatcher`), because a retry must re-run the whole handler with fresh sourcing. Register an `AsyncRetryScheduler` bean with a policy that filters this exception, uses exponential back-off and a small max attempts. Verify the filter matches after the Axon Server hop (spike).
- Optional: `@Command(routingKey = "courseId")` so all commands for one course land on the same command-service instance and are sequenced by the command bus, which lowers conflict rate when scaling out. DCB still guarantees correctness without it.
- The demo should expose a conflict counter (interceptor or the diagnosing engine) so the UI and Grafana can show how many appends were rejected and retried while the invariant held.

---

## Recommended Project Structure

### Gradle multi-project layout

```
axon5-dcb-courses/
├── settings.gradle.kts                  # includeBuild("build-logic"); typesafe project accessors
├── gradle/libs.versions.toml            # version catalog; BOMs: axoniq-framework-bom, spring-boot-dependencies
├── build-logic/                         # included build = convention plugins
│   └── src/main/kotlin/
│       ├── dcb.kotlin-conventions.gradle.kts        # JDK 21 toolchain, Kotlin options, detekt/ktlint, kover
│       ├── dcb.contracts-conventions.gradle.kts     # no Spring; only axon-messaging annotations allowed
│       ├── dcb.axon-service-conventions.gradle.kts  # Spring Boot, axoniq-spring-boot-starter, actuator, OTel, bootBuildImage
│       ├── dcb.testing-conventions.gradle.kts       # jvm-test-suite: test (unit), componentTest, integrationTest
│       └── dcb.docker-conventions.gradle.kts        # image build + naming
├── contracts/
│   ├── shared-kernel/                   # StudentId, CourseId, ..., Tags (tag key constants)
│   ├── commands/                        # depends: shared-kernel
│   ├── events/                          # depends: shared-kernel
│   └── queries/                         # query + result types for the BFF<->query-service boundary; depends: shared-kernel
├── services/
│   ├── command-service/                 # depends: contracts:commands, contracts:events
│   ├── query-service/                   # depends: contracts:events, contracts:queries
│   ├── automation-service/              # depends: contracts:events, contracts:commands
│   └── bff-service/                     # depends: contracts:commands, contracts:queries   (NOT events)
├── frontend/                            # Vite + React + TS; wired in with com.github.node-gradle.node (lint/test/build tasks)
├── integration-tests/                   # multi-context, real Axon Server + Postgres (Testcontainers)
├── e2e-tests/                           # Playwright (TypeScript), runs against docker compose
├── load-tests/                          # Gatling (Kotlin) or k6; not part of `check`
├── architecture-tests/                  # Konsist rules over all module sources
├── docs/                                # adr/, architecture/workspace.dsl, event-modeling/, site
├── observability/                       # otel-lgtm config, Grafana dashboards-as-code
├── compose.yaml                         # + compose.dev.yaml, compose.e2e.yaml (profiles)
└── .github/workflows/
```

**Rationale for the contracts split.** The Gradle dependency graph itself enforces the CQRS story:

| Consumer | commands | events | queries |
|----------|:--------:|:------:|:-------:|
| bff-service | yes | **no** | yes |
| command-service | yes | yes | no |
| query-service | no | yes | yes |
| automation-service | yes | yes | no |

No service depends on another service (enforced in the convention plugin plus a Konsist test). If four tiny modules feel heavy, the fallback is one `contracts` module with `commands`/`events`/`queries` packages and the same rule enforced by Konsist. The dependency-matrix version is preferred because it fails at compile time.

### Package layout inside each service (vertical slices, as in the official demo)

```
command-service/src/main/kotlin/dev/.../faculty/
├── write/
│   ├── createcourse/       CreateCourseHandler.kt, State.kt           # one folder per command
│   ├── renamecourse/
│   ├── changecoursecapacity/
│   ├── enrollstudent/
│   ├── subscribestudent/   # also owns waiting-list branch of the decision
│   ├── unsubscribestudent/
│   ├── transferstudent/
│   ├── promotefromwaitinglist/   # command that automation dispatches
│   └── setcourseschedule/
└── infrastructure/         # retry policy, decision-record (consistency boundary) stamping, converter config

query-service/.../faculty/read/
├── coursecatalog/          # projector, repository, query handlers, update emission
├── courseroster/
├── studentschedule/
├── waitinglist/
├── eventlog/               # explorer read model (see section 5)
└── infrastructure/         # token store config, reset endpoints

automation-service/.../faculty/automation/
└── promotefromwaitinglist/ # WhenSeatFreedThenPromoteNextWaitingStudent.kt + its own event-sourced State
```

Slice rules (Konsist-enforceable): a slice package must not import another slice package; `write` must not import `read`; `State` classes are `internal`; contracts have no Spring imports; handlers and `State` live in the same slice folder.

---

## Architectural Patterns

### Pattern 1: Decision model per command slice (DCB)
**What:** Each command has its own event-sourced `State` and criteria builder.
**When to use:** Every write operation.
**Trade-offs:** Some duplication between slices (deliberate; AF's own guide says "slices do not share lookups"). In return there is no shared aggregate and no accidental coupling. Boundaries can be as narrow as the rule allows.

### Pattern 2: Contracts-as-published-language with enforced dependency matrix
**What:** Commands, events and queries live in separate tiny modules. **When to use:** Always in this project. **Trade-offs:** More Gradle modules; fails at compile time instead of review time.

### Pattern 3: Automation slice = event handler + command, idempotency in the decision
**What:** A pooled streaming processor handler reacts to an event and dispatches a command. The handler **returns the dispatch future** so the processor waits and treats failure as event failure. The decision in the command handler is what makes a duplicate harmless.
**When to use:** Waiting list and any "when X then do Y".
**Example (shape; Java example from the official demo uses `CommandDispatcher` and `StateManager`):**
```kotlin
@Component
class WhenSeatFreedThenPromoteNext {
    @EventHandler
    fun on(e: StudentUnsubscribedFromCourse, dispatcher: CommandDispatcher, ctx: ProcessingContext): CompletableFuture<*> {
        val queue = ctx.component(StateManager::class.java)
            .loadEntity(WaitingListState::class.java, e.courseId, ctx).join()   // event-sourced, not a read model
        val head = queue.head() ?: return CompletableFuture.completedFuture(null)
        return dispatcher.send(PromoteStudentFromWaitingList(e.courseId, head), Any::class.java)
    }
}
```
The command-service re-checks in one DCB decision (course tag + student tag): seat free, student still waiting, student below the course limit, no schedule overlap. A duplicate delivery or stale head yields a `Rejected(AlreadyPromoted)` result, not an exception, so the processor does not loop.

### Pattern 4: Read slice = projector + query handler + update emitter in one JVM
**What:** The handler that updates the table also calls `emitter.emit(QueryType::class.java, predicate, update)` (the `QueryUpdateEmitter` is a handler parameter in AF5, not a field). The `@QueryHandler` returns the initial snapshot. **When to use:** Every live view. **Trade-offs:** Emitter and subscription handler must be in the same JVM (see Scaling).

### Pattern 5: Subscribe-before-send for read-your-writes
**What:** For flows that need confirmation (REST `?await=true`, load test assertions, the race demo): the BFF opens a subscription query on the affected view, then dispatches the command, then awaits the first update matching a predicate (with timeout). **When to use:** Only where confirmation is required. The UI normally does not wait: it sets a pending state on command acceptance and lets the SSE stream confirm. **Trade-offs:** Predicate must be cheap and specific. (Pattern assembled from documented AF5 APIs; MEDIUM confidence, validate in the BFF phase.)

### Pattern 6: Snapshot-then-delta SSE
**What:** One SSE connection per view. First message is the full current state (the subscription query's initial result), followed by updates. The client replaces its cache. **Why:** Reconnection is trivially correct. There is no `Last-Event-ID` replay logic, because every connect starts with a snapshot. Use `onBackpressureLatest` for state-shaped updates and send a comment heartbeat every ~15 s. Dispose the publisher on client disconnect to release the subscription on the server.

### Pattern 7: Decision record stamped on events (for the explorer)
See section 5.

---

## 2. Service Split and Communication

| Interaction | Mechanism | Path |
|-------------|-----------|------|
| UI to BFF | HTTPS REST + SSE | nginx proxy `/api` to bff |
| BFF to command-service | **Command** via `CommandGateway` | BFF → Axon Server → handler (location transparent; no URL known) |
| BFF to query-service | **Query** via `QueryGateway.query/queryMany`; **subscription query** `subscriptionQuery` returns a `Publisher` (initial + updates, needs `reactor-core`) | BFF → Axon Server → query handler |
| command-service to event store | Append with `AppendCondition` | gRPC `DcbEventStore.Append` |
| query-service, automation-service from event store | Pooled streaming processors (`StreamEventsRequest` with criteria from handler types) | gRPC `DcbEventStore.Stream` |
| automation-service to command-service | **Command** via `CommandDispatcher` (inside a processing context) or `CommandGateway` | Axon Server routes |
| BFF/Explorer to history | Query (`FindEvents`, `GetDecisionBoundary`) against query-service event log | via QueryBus |

Rules:

- **Only the BFF exposes HTTP to the outside.** The three other services expose actuator (health, metrics) on a management port only.
- **Location transparency is real:** services know message types (contracts) and the Axon Server address, never each other's hostnames. Scaling or moving a handler requires no client change.
- **Distributed buses are Axoniq Framework** (finding 2). Kotlin gets the nicer API from `axon-kotlin` (extension functions); verify which extensions cover `subscriptionQuery`.
- **Converter:** configure `axon.converter.general` (the demos use `jackson` / `jackson2`). Spring Boot 4 ships Jackson 3, so verify the Axon converter option matches the Jackson major version and registers the Kotlin module. Event JSON must stay stable because the explorer displays it.
- **Commands carry their result type.** Handlers return sealed results declared in `contracts:commands`. The BFF maps `Rejected(reason)` to HTTP 409/422 with problem+json, and infrastructure failures to 5xx.

### Where the REST API lives
BFF only: springdoc-openapi (webflux UI) generates the spec. A Gradle task writes `openapi.json` to `docs/api/` and the frontend generates its typed client from it. A CI step fails on drift. SSE endpoints are documented by hand (OpenAPI cannot model event streams well).

### How the BFF pushes real-time updates
```
Browser EventSource  <--SSE--  BFF controller returns Flux<ServerSentEvent<ViewDto>>
                                   ^
                                   | queryGateway.subscriptionQuery(GetCourseCatalog, CourseCatalogView::class.java)
                                   v
                              Axon Server (subscription query routing)
                                   ^
                                   | initial: @QueryHandler (reads Postgres)
                                   | updates: projector -> emitter.emit(GetCourseCatalog::class.java, filter, view)
                              query-service
```

---

## 3. Automation (Waiting List)

**Recommended shape: automation slices with state from context events** (the official `WhenCourseFullyBookedThenSendNotification` example is the template). No saga class, no process-state table.

Flow:
```
StudentUnsubscribedFromCourse | StudentTransferred(out) | CourseCapacityChanged(up)   [events]
   -> automation-service pooled streaming processor "automation-waitinglist"
   -> load WaitingListState (event-sourced, criteria: course tag; ordered queue)
   -> if queue non-empty: dispatch PromoteStudentFromWaitingList(courseId, head)   [command via Axon Server]
   -> command-service: one DCB decision (course tag + student tag) -> StudentPromotedFromWaitingList
        (+ StudentSubscribedToCourse in the same append) | Rejected(reason) as a result
   -> query-service projects both; subscription updates reach the UI
```

Decisions:

- **Automation decides who, command decides whether.** The automation never trusts a read model for decisions (stale projections break the invariant story). It reads event-sourced state (works because all services share one Axon Server context; "state from context events" only works inside one context) and the command re-validates under a DCB condition.
- **Join/queue is part of the subscribe decision:** when full, `SubscribeStudentToCourse` appends `StudentAddedToWaitingList` instead of failing. Capacity check and enqueue are atomic, so there is no window between "full" and "enqueued".
- **Idempotency:** the processor tracks a token, so at-least-once delivery is the contract. Handlers must tolerate re-delivery. The command-side decision is the idempotency guard ("student no longer waiting" means no-op result). No dedupe table.
- **Ordering:** sequence per course (`@SequencingPolicy(PropertySequencingPolicy, "courseId")`), so one course's events are handled in order while courses run in parallel.
- **Errors:** return the dispatch future so failures become event failures and are retried by the processor. Expected business rejections are *results*, so they must not poison the processor. Configure a bounded retry/back-off. The Sequenced Dead Letter Queue is commercial; for v1 log + metric + Platform visibility.
- **Workflows (5.4)** exist but are a commercial preview. Mention in ADR as considered and deferred; the project's point is DCB, not workflow engines.
- **Schedules and timeouts:** none needed. Axon Server's `DcbEventScheduler` and AF deadlines exist if a hold-timeout feature is ever added.

---

## 4. Read Models

**Storage:** PostgreSQL for query-service, Flyway migrations, one table set per read slice. `JdbcTokenStore` or `JpaTokenStore` **in the same database** as the projections, so handler writes and token advance commit in one transaction (docs: "Handler work and the token update share one transaction"; recommended practice). Provide an explicit `TokenStore` bean so it can never silently fall back to `InMemoryTokenStore`.

| Read model | Serves | Notes |
|------------|--------|-------|
| `course_catalog` | list/detail, capacity, name, seats left | emits updates for `GetCourseCatalog`, `GetCourse(id)` |
| `course_roster` | students per course, waiting list per course | |
| `student_schedule` | student's courses and slots, conflicts | drives the UI overlap hint (server remains authoritative) |
| `event_log` | explorer | see section 5 |

**Processor config:** `EventProcessorDefinition.pooledStreaming(name).assigningHandlers { ... }` beans in Spring (as in the official example) or `axon.eventhandling.processors.<name>.mode / initial-segment-count / batch-size / thread-count`. Use `@SequencingPolicy` keyed by entity id for ordered, parallel projection. Use a single segment for projections that must be strictly globally ordered (event log).

**Replay / reset:**
- `resetTokens()` needs the processor stopped (in every node). Triggerable programmatically (admin endpoint on query-service, not exposed via the BFF) or from the Axoniq Platform "Reset Processor" button (author has an account).
- Use a `@ResetHandler` to truncate the slice's tables before replay and the `ReplayStatus` parameter to **suppress subscription-query emission during replay**.
- Zero-downtime rebuild pattern: new processor name + new table version (`course_catalog_v2`), switch query handler after caught-up. Keep as a documented option, not a v1 feature.
- A first-time processor starts at the first event and is **not** flagged as replay. Initial catch-up emits updates unless suppressed (no subscribers exist then, so harmless).

**Eventual consistency in the UI:**
1. Commands return a result immediately (accepted, waitlisted, rejected).
2. The UI shows a pending state for the affected row and relies on its open SSE view to confirm (snapshot-then-delta).
3. For REST/test callers: `?await=true` implements subscribe-before-send (Pattern 5).
4. Read model lag is observable: Axoniq Platform and a Micrometer gauge for processor latency, surfaced as a small "projection lag" indicator in the UI.
5. Never read a projection to make a command decision.

---

## 5. Event Explorer and the Consistency Boundary

**What Axon Server gives you** (verified from `dcb.proto` and the connector's `DcbEventChannel`): `Source(fromSequence, criteria)` (finite), `Stream(fromSequence, criteria)` (live), `GetHead`, `GetTail`, `GetSequenceAt(timestamp)`, `GetTags(sequence)`. Responses carry `sequence`, event identifier, name, version, timestamp, payload bytes, metadata, but **not tags**. The appended `ConsistencyCondition` is not retrievable.

**Recommendation: build the explorer as a read slice of the query-service (`eventlog`) fed by a single-segment streaming processor, with the boundary recorded at write time.**

1. **Event log projection.** For every event, store `position` (global sequence from the `TrackingToken` handler parameter), `event_id`, `name`, `timestamp`, `payload` (JSON), `metadata` (JSON), `tags` (resolved with the same annotation-based `TagResolver` the command side uses, so no N+1 `GetTags` calls), plus the decision record (below). Postgres makes paging and filtering by tag, name or time trivial. A subscription query (`TailEvents`) gives the live tail.
2. **Decision record, stamped at append time.** In the command-service, decorate the `EventStorageEngine` (`registry.registerDecorator(EventStorageEngine::class.java, order) { config, name, delegate -> ... }`; the API is verified for `EventStore` and is the same mechanism AF uses for `ConflictDiagnosingEventStorageEngine`). In `appendEvents(condition, context, events)` you can see the `AppendCondition` (criteria and consistency marker), the events and the context (command message id). Add metadata to each event: `dcb.commandType`, `dcb.commandId`, `dcb.criteria` (list of `{tags, names}`), `dcb.marker`, `dcb.decisionId` (shared by events in one append). Axon propagates correlation data such as the causing command id via correlation metadata. Use it rather than reinventing it.
3. **Showing the boundary:** for an event X, take its decision record. The boundary is "events with `position < marker` that match any recorded criterion". Compute this with SQL over `event_log` (tags and names are stored). Highlight events from the same decision and, for a rejected append, the conflicting event.
4. **Fallback if decorating the engine proves impractical** (the SPI is `@Internal` for some pieces; LOW-MEDIUM risk): record criteria per slice explicitly by having every `State` expose its `EventCriteria` and stamping metadata from a handler interceptor, with a Konsist rule that every slice does so. This is less generic but uses only public API. The marker can be approximated by "head position at sourcing time" via `EventStoreTransaction.source(condition, resumePositionCallback)` (callback receives the `Position`).
5. **Raw mode (optional differentiator):** a debug endpoint that opens `Source(criteria)` straight against Axon Server for a recorded criterion to prove that the stored boundary matches what the store returns. Needs `axonserver-connector-java` (`DcbEventChannel`) in query-service only.

**Spike required in the command-service phase:** prove the decorator receives criteria and marker, that metadata added there reaches the stored event, and that it survives conflict/retry (each retry produces a new decision record).

---

## 6. Test Architecture

| Level | Where it lives | Tooling | What it proves |
|-------|---------------|---------|----------------|
| **Decision unit** | each service `src/test`, next to the slice | `State.decide()` as plain function tests; `AxonTestFixture` (`given().events/commands`, `whenever().command`, `then().success().events / exception / noEvents`) with `axon-kotlin-test` (`whenever`, `exception(KClass)`) | DCB rules with production slice configuration, in-memory store (`ServerConnectorConfigurationEnhancer` disabled or `axon.axonserver.enabled=false`) |
| **Read-slice unit** | `query-service/src/test` | `AxonTestFixture` `given().events(...)`, then `await(...)` for the async projection; query via `QueryGateway` from `Configuration` (fixture has no first-class query assertions) | Projection and query handler behaviour |
| **Automation unit** | `automation-service/src/test` | event in, command out (given event [+ state], then command dispatched) | Policy correctness and idempotency |
| **Component (per service)** | `src/componentTest` (jvm-test-suite) | `@AxonSpringBootTest` (`axon-spring-boot-starter-test`) + Testcontainers Postgres, Axon Server disabled | Spring wiring, Flyway, token store, REST contract (BFF with mocked gateways, OpenAPI snapshot) |
| **Integration** | `integration-tests` module | Testcontainers: `AxonServerContainer` (`axoniq-testcontainer`, dev mode, DCB context) + Postgres; boots command, query, automation (and BFF) Spring contexts **in one JVM** against the real Axon Server | Real DCB append conditions and conflicts, routing, subscription queries across "services", replay, waiting list end to end, concurrent-subscribe invariant (N threads, capacity C, assert subscribed == C) |
| **Contract** | bff-service componentTest | OpenAPI JSON snapshot test | Frontend/BFF contract stability |
| **E2E** | `e2e-tests/` | Playwright (TypeScript) against the **built images** via `docker compose --profile app up --wait` | Full user journeys, SSE live updates, race demo, explorer |
| **Architecture** | `architecture-tests/` | Konsist (source-based Kotlin rules), plus Gradle dependency-matrix check | Slice isolation, contracts purity, service independence, `State` visibility, naming |
| **Load** | `load-tests/` | Gatling (Kotlin DSL; HTML reports) or k6 (native Prometheus output into LGTM); run in CI on schedule or manually, not in `check` | Latency and throughput under the concurrent-subscription scenario, **with a post-run invariant assertion** (subscribed count never exceeds capacity) |
| **Coverage** | all JVM modules | Kover merged report | Reporting |

Notes:
- Konsist is at `1.0.0-beta.1` (2026-11-04); the prior line is 0.18.x. Pin and note in an ADR. ArchUnit (1.5.1) is bytecode-based and less natural for Kotlin; not needed.
- The fixture "configured the same as production" approach means unit tests exercise the real criteria builder. A wrong criteria shows up as a missing-event failure, not a green test.

---

## 7. Docker Compose Topology

```
compose.yaml  (profiles: infra | app | obs ; e2e/load via overlay files)

axon-server      image axoniq/axonserver:2026.1.8 (pin; docker.axoniq.io mirror also used by official docs)
                 ports 8024 (UI/actuator), 8124 (gRPC)
                 env  axoniq.axonserver.standalone-dcb=true   # creates DCB context (verified in official example)
                      axoniq_axonserver_devmode_enabled=true  # dev only
                 volumes data, events;  healthcheck on :8024
                 + licence/Platform wiring  <-- SPIKE (12-hour limit without licence)
postgres         postgres:17|18, init script creates databases: query_db, automation_db; healthcheck
command-service  depends_on axon-server(healthy); NO postgres; env axon.axonserver.servers, context
query-service    depends_on axon-server, postgres(healthy); Flyway runs at start
automation-service depends_on axon-server, postgres(healthy)
bff-service      depends_on axon-server; port 8080 internal
frontend         nginx serving SPA, proxies /api to bff-service (HTTP/2 or limit SSE streams, see pitfalls); port 3000 -> host 5173/8081
otel-lgtm        image grafana/otel-lgtm; ports 3000 (Grafana), 4317/4318 (OTLP)   # all services export OTLP here
```

- **Profiles / overlays:** `infra` (axon-server, postgres, otel-lgtm) is what developers run while the services run from the IDE with Spring Boot Docker Compose support. `app` adds the five application containers. `e2e` adds a Playwright runner container (or the host runs Playwright against `localhost`). `load` adds the load generator.
- **Secrets/config:** Axoniq Platform credentials come from an optional `.env` (`axoniq.platform.credentials`, optional; client `io.axoniq.platform:axoniq-platform-framework-client` 5.4.4). Absent credentials must not break startup.
- **Observability:** Axon 5 traces natively (a `SpanFactory` bean activates tracing; OpenTelemetry export via Micrometer Tracing binding, `axon-tracing-opentelemetry` is commercial; `LoggingSpanFactory` is the open fallback). Metrics via `axon-metrics-micrometer`. Spring Boot OTLP export to `otel-lgtm`. Trace propagation across the Axon Server hop is stamped into message metadata, so command → event → projection → subscription update appear as linked traces (the processor batch is linked, not parented, to the publisher).
- **Images:** one Gradle convention (`bootBuildImage` or Jib) per service, pinned base images, scanned in CI.

---

## 8. C4 Diagram-as-Code

**Recommendation: Structurizr DSL as the single source of truth** (`docs/architecture/workspace.dsl`), with the unified `structurizr/structurizr` tool (the old `structurizr/cli` repo is archived; latest release v2026.10.26).

- Views to model: **System Context**, **Containers** (the diagram in this document), **Components** for command-service (slices) and query-service (read slices), a **Deployment** view (docker compose), and **Dynamic** views for three flows: subscribe with conflict and retry, waiting-list promotion, SSE update path.
- CI: `structurizr validate` plus export to **Mermaid** (renders natively on GitHub in README and in markdown docs sites) and optionally PlantUML/SVG. Commit the exports or generate them in the docs build, but never hand-edit.
- C4 does not cover **Event Modeling / Event Storming** artifacts or sequence diagrams for DCB internals. Keep those separate (images, Mermaid sequence diagrams) and link from ADRs.
- Alternative: **LikeC4** (v1.61.0, very active) gives interactive, embeddable, zoomable diagrams for a docs site, but is not C4-native (you define the C4 element kinds yourself). Choose it only if an interactive docs site matters more than C4 orthodoxy. (MEDIUM confidence; tool landscape judgement.)

---

## Data Flow

### Request flow (command)

```
[User clicks Subscribe]
   ↓
frontend --POST /api/subscriptions--> bff (validate DTO, retry-policy gateway)
   ↓ CommandGateway.send(SubscribeStudentToCourse)
Axon Server (route by routing key)
   ↓
command-service: @InjectEntity State  <-- DcbEventStore.Source(criteria)  (events + marker)
   ↓ decide() -> sealed result ; EventAppender.append(...)
   ↓ DcbEventStore.Append(events, ConsistencyCondition(criteria, marker))   [decision record in metadata]
   ├── conflict -> AppendEventsTransactionRejectedException -> retry at BFF (re-source, re-decide)
   └── ok -> result returned to BFF -> HTTP 202/200/409
```

### Event flow (read side and automation)

```
Axon Server event stream
   ├─> query-service pooled streaming processors: update Postgres + token (one tx) -> emitter.emit(...) -> subscribers
   ├─> query-service event-log processor (single segment) -> event_log rows
   └─> automation-service processor -> (state from events) -> CommandDispatcher.send(...) -> Axon Server -> command-service
```

### State management (UI)

```
SSE snapshot/delta stream --> TanStack Query cache (setQueryData: replace with snapshot, merge deltas)
Mutations (REST) --> pending flag on affected row --> cleared when SSE confirms or command result is Rejected
Role switcher (admin / student X) --> request header consumed by BFF (no auth), drives which commands the UI offers
```

### Key data flows

1. **Subscribe, happy path:** command → source (course+student criteria) → decide → append with condition → events → projections → SSE.
2. **Last-seat race:** N concurrent commands, one wins the append, the rest get `AppendEventsTransactionRejectedException`, retry, re-source, and get `Waitlisted` or `Rejected(CourseFull)`. Capacity is never exceeded. Explorer shows the boundary and the conflict.
3. **Waiting list promotion:** unsubscribe → automation → promote command → decision → events → UI.
4. **Explorer:** event → decision record (criteria, marker) → SQL over event_log → highlighted boundary.

---

## Suggested Build Order (dependencies drive phases)

1. **Foundation and spikes.** Gradle skeleton, `build-logic`, contracts (shared-kernel, events, commands), pinned versions and BOMs, compose `infra` profile with Axon Server DCB context, CI skeleton. **Spikes:** (a) Axon Server licence in compose and CI (finding 3), (b) Jackson converter with Kotlin and Spring Boot 4, (c) decision-record decorator (section 5). Nothing else depends on more risk than these.
2. **Command walking skeleton.** command-service with create course, enroll student, subscribe (with `AxonTestFixture` tests, decision-record stamping, retry policy, sealed results). Integration test against real Axon Server proving concurrent subscribe keeps capacity. *Everything downstream consumes these events.*
3. **Query walking skeleton.** query-service with `course_catalog`, token store, query handler, update emission; `event_log` projection can start here since it is generic. Component + integration tests.
4. **BFF.** REST + OpenAPI, command/query gateways, error mapping, first SSE view. Depends on 2 and 3.
5. **Frontend skeleton.** Vite app, generated client, course list with live updates, role switcher. Playwright smoke test and `app` compose profile. Depends on 4.
6. **Remaining write slices, each with its read slice and UI.** Rename + unique name, capacity change, unsubscribe, per-student limit, schedule slots + overlap, transfer. Independent slices, parallelisable, ordered by demo value.
7. **Automation.** Waiting list (adds `StudentAddedToWaitingList` to the subscribe decision, the promote command, the automation slice, and the `waiting_list` read model). Depends on 6 (unsubscribe, capacity change, transfer events).
8. **Explorer UI and concurrency demo.** Needs the decision records (step 2) and event_log (step 3); builds the boundary view, conflict display, race endpoint and UI. Load tests and invariant assertion.
9. **Observability polish and Platform.** Dashboards-as-code in Grafana, trace propagation checks, Axoniq Platform optional integration, processor lag in UI. Basic OTLP wiring should already exist from step 1.
10. **Hardening and documentation.** Konsist rules (can start in step 2 and grow with slices), coverage, vulnerability scanning, C4/ADRs/Event Modeling, README, "how this was built". Docs (ADRs, C4) are written continuously, not at the end.

---

## Scaling Considerations

| Scale | Architecture adjustments |
|-------|--------------------------|
| Demo (1 instance each, hundreds of students) | Exactly the compose topology. Single-segment event log, 4 segments elsewhere. |
| Tens of thousands of users | Scale **command-service** horizontally (Axon Server load-balances by routing key; use `routingKey=courseId` to cut conflicts). Scale BFF horizontally (stateless). Query-service: see below. |
| Beyond | Axon Server cluster (licence), partition read models, snapshotting for long-lived entities (AF 5.1 reintroduced snapshots; `AxonServerSnapshotStore` via enhancer), consider split event store contexts per bounded context (then "state from context events" no longer spans them). |

### Scaling priorities

1. **First bottleneck:** append conflicts on hot courses (the whole point of the demo). Retry with back-off, narrower criteria, routing key by course.
2. **Second bottleneck:** sourcing cost for hot tags (a course with thousands of subscriptions re-sources all events per command). Use snapshots or restructure the decision state (counter events) if it shows up in the load test. Worth showing as a measured finding in the load-test writeup.
3. **Query-service scale-out hazard:** with several instances, pooled streaming segments spread events across instances. An update emitted on instance B does not reach a subscription held on instance A (same class of problem as the AF4 "emitter and handler in different JVM" forum threads). For v1 run exactly one query-service instance. If scale-out is needed, run the *emitting* processor per instance in broadcast mode (each instance sees all events) while the persisting processor stays pooled. (MEDIUM, spike if scale-out is promised.)

---

## Anti-Patterns

### Anti-Pattern 1: Entity-per-service write split
**What people do:** course-service and student-service because "that is what the aggregates were".
**Why it's wrong:** DCB invariants span both; you recreate cross-service coordination (saga) that DCB removes.
**Do this instead:** one command-service, one decision model per command.

### Anti-Pattern 2: Deciding on read models
**What people do:** the automation or a command handler queries a projection for "is the course full?".
**Why it's wrong:** projections lag; the invariant is no longer protected by the append condition.
**Do this instead:** decide only from event-sourced state loaded with criteria. Projections are for queries.

### Anti-Pattern 3: One shared `State` for many commands
**What people do:** rebuild the aggregate under another name and widen criteria to "everything about a course".
**Why it's wrong:** over-wide criteria cause false conflicts (rename blocks subscribe); coupling returns.
**Do this instead:** per-slice state with the narrowest event types that matter.

### Anti-Pattern 4: Exceptions for business rejections
**What people do:** `throw IllegalStateException("Course is fully booked")` as the official demos do.
**Why it's wrong:** over a distributed bus it becomes a generic `CommandExecutionException`; processors treat it as failure and may retry or stall; HTTP mapping becomes string matching.
**Do this instead:** sealed result types in contracts; map to 409/422 in the BFF.

### Anti-Pattern 5: Treating tags as a query index for the UI
**What people do:** the BFF reads the event store directly for lists.
**Why it's wrong:** couples UI to event shape and bypasses read models.
**Do this instead:** read slices. The explorer is a deliberate exception with its own projection.

### Anti-Pattern 6: In-memory token store / unnamed processors
**What people do:** accept the default token store in Spring.
**Why it's wrong:** if no JPA is present the default is `InMemoryTokenStore` and every restart re-processes the full stream.
**Do this instead:** explicit `TokenStore` bean in the projection database; name processors explicitly and stably (renaming one creates a new token set).

### Anti-Pattern 7: Calling services over HTTP "just this once"
**What people do:** BFF calls query-service REST for a convenience endpoint.
**Why it's wrong:** removes location transparency, the project's showcase.
**Do this instead:** add a query message to `contracts:queries`.

### Anti-Pattern 8: Replaying with emission on
**What people do:** reset a processor and let every historic event fire subscription updates and side effects.
**Do this instead:** inspect `ReplayStatus` and suppress emission and side effects during replay.

---

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Axon Server 2026.1.x | gRPC 8124 via `axon-server-connector` (Axoniq Framework) | DCB context required; licence behaviour (12 h) needs spike; HTTP 8024 for UI/health |
| Axoniq Platform | `io.axoniq.platform:axoniq-platform-framework-client` 5.4.4, `axoniq.platform.credentials` | Optional; processor reset, handler metrics, licence delivery. Data sent: see client README; keep `domain-event-access-mode` at `NONE` default |
| Grafana LGTM | OTLP 4317/4318 | Single container, dev only |
| PostgreSQL | JDBC (token store + projections) | Flyway; same DB for token and projection |
| GitHub Actions / registry | Build, Testcontainers, image scan | Docker-in-Docker for Testcontainers; compose for E2E |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| frontend ↔ bff | REST + SSE over HTTP | Generated TS client; SSE multiplexing or HTTP/2 |
| bff ↔ command-service | Commands via Axon Server | Result types in `contracts:commands`; retry on conflict |
| bff ↔ query-service | Queries and subscription queries via Axon Server | Types in `contracts:queries` |
| automation ↔ command-service | Commands via Axon Server | Idempotent decisions |
| command-service → event store | Append with `AppendCondition` | Decision record stamped |
| query/automation ← event store | Pooled streaming, token store | Tokens in Postgres |
| slice ↔ slice | none (no imports) | Konsist-enforced; share only via events |
| service ↔ service | none directly | Gradle-enforced |

---

## Pitfalls Specific to Architecture (for PITFALLS.md hand-off)

1. **Commercial licensing of the Axon Server connector and the 12-hour Axon Server limit** (findings 2, 3). Could break "runs locally with docker compose" for public users. Needs an early decision, ADR and README disclosure.
2. **Consistency boundary is not queryable from Axon Server** (finding 5). Retrofitting is impossible for historical events.
3. **DCB conflict exception is non-transient** (finding 8). Without a retry policy the concurrency demo shows errors instead of correct arbitration.
4. **Subscription query scale-out** (section Scaling). Silent missing updates with more than one query-service instance.
5. **SSE connection limits**: browsers allow ~6 HTTP/1.1 connections per origin; several SSE views per page can starve REST calls. Use one multiplexed stream per page or HTTP/2 at nginx.
6. **Event schema stability**: explicit `@Event(name=...)`, additive changes, version field; upcasters (`message transformation`) are commercial in `io.axoniq.framework`. Decide the event model carefully up front.
7. **Jackson major version mismatch** with Spring Boot 4 and Kotlin data classes (converter spike).
8. **`@Internal` API reliance** for decision-record stamping; isolate behind one class and one integration test so a framework upgrade fails loudly.
9. **`@InjectEntity` nullability** in creational handlers (AF 5.3 breaking change): declare `State?` in Kotlin.
10. **Spring Boot 4 + Testcontainers 2.0 artifact naming** (modules renamed in 2.0; `axoniq-testcontainer` 5.4.2 aligns to Testcontainers 2.0.5). Align the BOM, do not mix 1.x modules.

---

## Sources

HIGH confidence (official docs, source code, official examples; read 2026-11-20):
- Axon Framework repository, `examples/university-demo-kotlin`, `examples/university-java-springboot-4`, `examples/docker-compose.yaml`, `examples/pom.xml` (github.com/AxonFramework/AxonFramework)
- Axon Framework source: `DefaultEventStoreTransaction`, `EventStoreTransaction`, `AppendCondition`, `AppendEventsTransactionRejectedException`, `ConflictDiagnosingEventStorageEngine`, `EventSourcingConfigurationDefaults` (same repo)
- Axon Framework docs in repo: `docs/saga-guide/.../vertical-slices.adoc`, `choosing-an-approach.adoc`, `index.adoc`; `docs/reference-guide/.../events/event-processors/streaming.adoc`, `messaging-concepts/exception-handling.adoc`, `monitoring/tracing.adoc`, `migration/paths/token-store.adoc`, `extensions/kotlin/testing.adoc`
- https://docs.axoniq.io/axon-framework-reference/5.4/events/event-store-internals/ (tags, `EventCriteria`, `ConsistencyMarker`, `AppendCondition`, `@EventCriteriaBuilder`)
- https://docs.axoniq.io/axon-framework-5-getting-started/5.4/ (tutorial pages: project structure, subscribe student, stateful handler, read model, configure Axon Server)
- https://docs.axoniq.io/axon-framework-reference/5.4/queries/query-dispatchers/ (subscription queries, `QueryUpdateEmitter`)
- https://docs.axoniq.io/axon-framework-reference/5.4/release-notes/major-releases/ (5.1 connector move, snapshots, Kotlin extension, 5.3 `@InjectEntity` change, 5.4 workflows and legacy sagas)
- https://docs.axoniq.io/axon-framework-reference/5.4/framework-comparison/ and `/modules/` (Apache-2.0 vs commercial module split, artifact coordinates)
- https://docs.axoniq.io/axoniq-platform-reference/licensing/ (12-hour Axon Server limit, development permission)
- https://discuss.axoniq.io/t/axon-server-2026-0-0/6681 (Platform License V2, throughput limits)
- Axon Server API `dcb.proto` (github.com/AxonIQ/axon-server-api) and `DcbEventChannel` (github.com/AxonIQ/axonserver-connector-java)
- Maven Central metadata for versions (axoniq-framework-bom, axoniq-spring-boot-starter, axoniq-testcontainer, axon-kotlin, axon-spring-boot-starter-test = 5.4.2; axoniq-platform-framework-client = 5.4.4); Docker Hub tags for `axoniq/axonserver`; GitHub releases for tool versions

MEDIUM confidence:
- https://www.axoniq.io/open-source and https://axoniq.io/pricing (licence marketing copy; legal terms not read)
- https://docs.axoniq.io/axon-framework-reference/5.4/commands/infrastructure/ (retry scheduler, routing keys)
- Structurizr / LikeC4 comparison (blog posts; tool release data from GitHub)

LOW confidence (design judgement or unverified, flagged as spikes): decision-record stamping through an `EventStorageEngine` decorator; retry-filter behaviour of the wrapped conflict exception after the Axon Server hop; subscribe-before-send read-your-writes; subscription-query scale-out workaround; how a licence reaches a containerised Axon Server 2026.1.

---
*Architecture research for: Axon Framework 5 DCB courses/students reference system (Kotlin, Spring Boot, Axon Server, React)*
*Researched: 2026-11-20*
