# Pitfalls Research

**Domain:** Axon Framework 5 + DCB reference implementation (Kotlin, Spring Boot, multi-service CQRS split, Axon Server, React UI, full test pyramid, supply-chain CI/CD, OpenTelemetry, AxonIQ Platform)
**Researched:** 2026-11-20
**Confidence:** MEDIUM-HIGH. The licensing, module-split, migration and tracing findings come from official AxonIQ docs (HIGH). DCB modeling and Kotlin findings mix official docs, the dcb.events spec and forum threads (MEDIUM). Items marked **[VERIFY]** are inferences that need a spike before the roadmap relies on them.

**Phase vocabulary used below** (the roadmapper may rename these; the mapping is what matters):
P1 Foundation and licensing spike, P2 Write side (DCB decisions), P3 Read side (projections), P4 Automation (waiting list), P5 Gateway/BFF and real-time, P6 UI, P7 Quality (E2E, load, architecture tests), P8 CI/CD and supply chain, P9 Observability and AxonIQ Platform, P10 Documentation and reference polish.

---

## Critical Pitfalls

### Pitfall 1: Assuming "Axon Framework 5 + Axon Server" is entirely open source and free (the Axoniq Framework split)

**What goes wrong:**
Since 5.1 the Axon Server connector, the Axon Server Spring integration, the Axon Server Testcontainer (`AxonServerContainer`), the distributed command/query buses, the Axon Server snapshot implementation, sequenced dead-letter queues, Workflows, tracing (`axoniq-tracing-micrometer`) and multi-tenancy live in **Axoniq Framework** (`io.axoniq.framework:*`), not in Apache-2.0 **Axon Framework** (`org.axonframework:*`). Axon Framework alone has the DCB Event Store API and in-process messaging, but no Axon Server connectivity. Almost every tutorial, blog post and AI-generated snippet written for 5.0 uses the old coordinates (`org.axonframework:axon-server-connector`, `org.axonframework.extensions.spring:axon-spring-boot-starter`, `org.axonframework.test.server.AxonServerContainer`) and fails or silently mixes the two families.

**Why it happens:**
The split happened between 5.0 and 5.1, and 5.x docs still contain 5.0-era text. The artifact naming is inconsistent (`axoniq-*` prefix, but the connector is `io.axoniq.framework:axon-server-connector`).

**Consequences:**
Wrong BOM or starter on the classpath, duplicate connectors, package-import churn (`org.axonframework.axonserver.connector` became `io.axoniq.framework.axonserver.connector`). The Axoniq modules are commercial: free for development/evaluation, **production requires a license**, and they check a license at startup. A public "production-grade reference" must state clearly that it is a non-production demo, or readers will copy it into production unlicensed.

**How to avoid:**
- P1 spike: build a walking skeleton with `io.axoniq.framework:axoniq-framework-bom`, `axoniq-spring-boot-starter` (includes the connector, do not add it again), `axoniq-testcontainer` (test scope) and `org.axonframework:axon-test` (still needed for `AxonTestFixture`).
- Record the Axon/Axoniq split and the licence stance as an ADR in P1. State in the README that the repo is for evaluation and learning, not production.
- Keep the **domain and decision code free of Axoniq imports** (only config/infrastructure modules touch `io.axoniq.*`). That makes the "Axon Framework only" boundary visible and keeps the DCB lesson portable.
- Do not import both BOMs.

**Warning signs:**
Compile errors on `AxonServerContainer` or `axonserver.connector` packages; a startup warning every 15 minutes about "no license, must not be used in production"; two connector jars in `dependencies`.

**Phase to address:** P1 (spike and ADR); re-check in P8 (Renovate must group these artifacts).

---

### Pitfall 2: Axon Server without a licence silently shuts down after 12 hours (docker compose demo dies)

**What goes wrong:**
The docs say Axon Server 2026.0+ that is not connected to AxonIQ Platform and has no licence file "runs for 12 hours, then shuts down." Docker Hub additionally says the image is for non-production use with deployment not exceeding 14 days. An older forum thread shows a 2025.2.1 upgrade that suddenly showed a 14-day expiry banner. The Axoniq Framework (5.3.2+) requests a temporary per-machine development permission from Axoniq Platform (renewed hourly), which implies internet access. Without it, commercial modules get a 24-hour window and then the app stops. Throughput limits are enforced as part of the licence in 2026.0+.

**Why it happens:**
Licensing changed across 2025.1, 2025.2 and 2026.0, and local demos are "always on" in a way tests are not.

**Consequences:**
A reader runs `docker compose up`, leaves it overnight, and the demo is dead. The Gatling/k6 load test, which is the most licence-sensitive thing in the project (throughput limits), may be throttled or stopped mid-run and produce misleading results. CI runners are fresh machines every time and rely on the dev permission mechanism.

**How to avoid:**
- P1: document the exact licence behaviour in the README ("dev mode", 12-hour limit, how to restart, how to attach an AxonIQ Platform token to lift it). **[VERIFY]** whether a restart resets the 12-hour window (the docs do not say) and whether the free "Individual" plan tier (non-production, 6 app instances per the pricing page, MEDIUM confidence because the table was garbled) covers a 4-service system plus a test run.
- Make the Platform connection *optional but recommended* in compose (`.env`, gitignored) and add a startup banner or README note when it is absent.
- Run the load test only after verifying licence/throughput behaviour on the exact Axon Server version; record the version in the results.
- Pin Axon Server image tags (never `latest`); the 2025.1 to 2025.2 jump broke DCB contexts and the licence model changed again in 2026.0.

**Warning signs:**
Axon Server container exits with code 0 after about 12 hours; "License is not valid!" errors on append/dispatch; throughput plateau in load tests that does not correlate with CPU.

**Phase to address:** P1 (spike and docs), P7 (load test), P8 (CI must tolerate dev-permission network call).

---

### Pitfall 3: Axon Server context is not a DCB context (or DCB created with an incompatible server version)

**What goes wrong:**
DCB needs a DCB-format context, created with `axoniq.axonserver.standalone-dcb=true` (standalone), `axoniq.axonserver.autocluster.dcb=true`, or `--dcb` on the CLI. The Docker/K8s install docs only show the plain `axoniq.axonserver.standalone` property and do not mention DCB. A default context is a classic (aggregate-based) context. DCB contexts created by 2025.1 (beta) are incompatible with 2025.2+ (a `rainbow.lsi` removal is required). 2025.1 limitations (no snapshots, no re-tagging) no longer apply from 2025.2; 2026.0 added conversion of existing contexts to DCB and the legacy EventStore API on DCB contexts; 2026.1 added multi-tier DCB storage.

**Why it happens:**
The "turn on DCB" flag is easy to miss and the first symptoms look like unrelated framework errors.

**Consequences:**
Confusing failures at first append, or code that "works" on a non-DCB context in tests (aggregate-based engine) but fails in compose. Test containers started without the DCB flag differ from compose.

**How to avoid:**
- One shared definition of "a DCB Axon Server" used by compose and by `AxonServerContainer` (the Axoniq container has a DCB initializer and a `DCB_CONTEXT` constant in `AxonServerContainerUtils`). **[VERIFY]** exact API in the spike.
- A smoke test (P1) that appends a tagged event with an `AppendCondition` and asserts a conflicting second append fails. That proves DCB semantics, not just connectivity.
- Pin both Axon Framework (5.4.x) and Axon Server (2026.1.x) versions and upgrade them together.

**Warning signs:**
Concurrency conflicts never raised in a race test; errors at context start; different behaviour in Testcontainers and compose.

**Phase to address:** P1.

---

### Pitfall 4: Missing, wrong or mismatched `@EventTag` produces silently empty state

**What goes wrong:**
The decision model is built by sourcing events by tag/type criteria. If an event lacks the tag used for loading (or the key differs in case or name), the entity is created from `@EntityCreator` with default values and **no error is raised**. Capacity reads as 0, counts as 0, and an invariant check passes wrongly. Confirmed in three independent sources: the Petr Macek migration write-up ("test fixtures silently fail to reconstruct state"), the forum "silent empty state" thread on the official tutorial's `AllCoursesFullyBookedNotifier` test, and the docs (tag key must match exactly; `TagResolver` fallback returns an empty tag set for unhandled event types). Passing tests can mislead because the fixture never built the intended state.

**Why it happens:**
Tags are semantic (they carry the identity of every subject an event affects), not just indexing. Developers add an event type and forget the tag, or rename a field and the default tag key (taken from the field name) changes. The tag value is taken from `toString()` of the annotated field.

**Consequences:**
Invariant violations that only show up under production-like data. Worse for this project: the core value ("capacity never exceeded") could be broken by a missing tag while the demo looks fine.

**How to avoid:**
- Choose tag keys that name the entity type (`Course`, `Student`) and set them explicitly (`@EventTag(key = "Course")`); never rely on field-name defaults.
- Create a tiny **tag registry** (constants object) in the shared API module, used by every `@EventTag`, `tagKey` and criteria builder.
- A reflection-based test over all event classes: every event class has the expected tags, and every given-when-then test asserts the *appended event's tags* as well as payload.
- Negative tests: "given events for a different course, state must not leak."
- Kotlin: **[VERIFY]** annotation use-site target (`@get:`/`@field:`/`@param:`) actually results in tags on the stored event, and that a `@JvmInline value class` ID does not yield `CourseId(value=abc)` from `toString()` (Axon uses `toString()` for the tag value). Override `toString()` or tag a raw `String` property.

**Warning signs:**
State is always at default values; "course not found" errors in valid flows; any test that passes with an empty `given()`.

**Phase to address:** P2 (and enforced as an architecture/reflection test in P7).

---

### Pitfall 5: Criteria drift: adding an event type without updating every decision that must see it

**What goes wrong:**
In DCB each decision declares its own query (tags + event types). A new fact (for example `StudentTransferred`, `StudentPromotedFromWaitingList`, `CapacityChanged`) changes the meaning of "seats taken" or "student's subscriptions", but every decision that counts them has its own criteria. Forgetting to include the new event type in one query yields an undercounted boundary: capacity leaks or per-student limit is exceeded after a transfer.

**Why it happens:**
There is no aggregate class enforcing that all invariants for an entity are co-located. Criteria are scattered across command handlers.

**Consequences:**
The classic DCB bug: invariants that pass every single-feature test but break across features (transfer plus subscribe plus waiting list).

**How to avoid:**
- Model **reusable state projections** per fact ("CourseSeatCount", "StudentSubscriptionSet") with their own event types and criteria and compose them in decisions. This is the dcb.events "decision model" pattern. Each projection owns its event-type list in one place.
- Cross-feature scenario tests (the full invariant suite from PROJECT.md executed against every combination of subscribe/unsubscribe/transfer/promote/capacity change).
- Property-based/concurrent test: random sequences of commands, assert invariants over the final event stream.

**Warning signs:**
Same event-type list copy-pasted in three handlers; a new event type touched only one file.

**Phase to address:** P2 (design), P4 (when the waiting list adds new event types), P7 (property/invariant tests).

---

### Pitfall 6: Hitting `EventCriteria` expressiveness limits (no AND, awkward variable tag sets, no collection injection)

**What goes wrong:**
`EventCriteria` supports OR (`either`/`or`); AND across criteria is an open ticket (#3343), a fluent builder for variable tag sets is open (#3381), and `@InjectEntity` with a collection of IDs is explicitly not supported (AxonIQ is hesitant because users could load every entity of a type). If a decision needs "the student's course subscriptions, then each of those courses' schedule events" (variable, data-dependent tag set, like the dcb.events "library loans: follow references" pattern), you will write ad-hoc `either` chains with special cases (a single tag yields nothing when chained pairwise, as one community user found).

**Why it happens:**
The DCB spec supports multi-step reads; the AF5 annotated entity model assumes IDs known from the command.

**Consequences:**
Schedule-overlap and transfer code becomes the ugliest part of the repo and obscures the lesson.

**How to avoid:**
- P2 spike on the schedule-overlap decision **before** committing to a modeling approach. Prefer **denormalising the schedule slot into the subscription event** (`StudentSubscribedToCourse` carries the slot) so the student's own events contain what the overlap check needs, then decide explicitly what happens when a course's schedule changes (see Pitfall 11).
- Wrap criteria construction in one small, tested helper (`criteriaForCourses(ids)`) instead of inlining.
- Track the upstream tickets; revisit at each Axon minor release.

**Warning signs:**
`fold`/`reduce` over `EventCriteria` in handlers; special-case for list size 1; handler tests that need 5+ given events just to set up tags.

**Phase to address:** P2 (spike, decision), P10 (document the trade-off honestly).

---

### Pitfall 7: Unbounded or over-broad consistency boundary (tags only, no types; growing streams)

**What goes wrong:**
Criteria with only a tag load **every** event of that tag, including irrelevant types, and the boundary conflicts with any writer touching that tag. A course's event stream grows (subscribe/unsubscribe churn, waiting list events); every `Subscribe` re-sources the whole stream. Over-broad boundaries also create false conflicts: a rename conflicting with a subscribe because both touch `Course:{id}` events that the subscribe decision does not actually depend on.

**Why it happens:**
"Tag everything and query by tag" is the easiest model. DCB's point is the *narrowest* boundary that protects the invariant, which requires listing event types.

**Consequences:**
Latency grows linearly with history; contention rises in the concurrency demo (the project's centerpiece); the race test shows retries instead of clean rejections.

**How to avoid:**
- Every projection lists **tags and event types** (dcb.events course-subscriptions example does this for each query item).
- Boundary-size budget per command written in the ADR (what events are loaded, why) and visible in the event explorer.
- Keep single-student limit events narrower than "all student events."
- Snapshots exist on DCB contexts since Axon Server 2025.2 (and AF 5.1 revised snapshotting; 5.2 documents a "snapshot reuse limitation"). Use only if measured; first try bounded streams. For unbounded cases the AxonIQ forum pattern is a "closing the books" boundary event plus a period tag; note its race (late event after the closing event) and time-zone traps.
- Count the loaded events in a metric (P9) so the load test can show growth.

**Warning signs:**
Command latency rising with number of past subscriptions; conflicts between unrelated commands on the same course.

**Phase to address:** P2 (model), P7 (load test proves it).

---

### Pitfall 8: Race handling: surfacing `ConcurrencyException` as an error instead of retrying into a business rejection

**What goes wrong:**
When two students race for the last seat, the DCB append of the loser fails (consistency marker violated). If the loser's command simply fails with a technical exception, the concurrency demo shows 500s rather than "course full." If retried without bounds or backoff, a thundering herd of 1000 racing students produces retry storms.

**Why it happens:**
The append condition detects the conflict; *re-running the decision* against the new state yields the business outcome ("course is full"). Retry behaviour is not obvious in AF5 (5.2 lists `RetryingCommandBus` support; **[VERIFY]** how it is enabled in the Spring starter and which exceptions it retries). The docs do not name how conflicts surface (the event-store internals page only says conflicting appends are "detected and handled appropriately").

**Consequences:**
Wrong demo narrative; misleading load-test numbers (error rate that is really expected contention); possible starvation.

**How to avoid:**
- P2: define "retry on append conflict, bounded (e.g. 3-5), exponential backoff with jitter, then surface a distinct `Conflict` outcome" and test it with a concurrent test (N threads, capacity K, assert exactly K successes and N-K business rejections, zero infrastructure errors).
- Classify command outcomes into SUCCESS, DOMAIN_REJECTION, CONFLICT_EXHAUSTED, TECHNICAL_ERROR in metrics and in the UI; the load-test pass criterion is about the invariant and the classification, not raw latency.

**Warning signs:**
HTTP 500/409 under load where "course full" is expected; retry counts equal to attempt counts.

**Phase to address:** P2 (behaviour), P5 (HTTP mapping), P7 (load test).

---

### Pitfall 9: Waiting-list automation built on eventually consistent read models (double enrolment, unfairness, barging)

**What goes wrong:**
Typical design: automation service reads the read-side waiting list, on `StudentUnsubscribed` picks the first student, sends `Subscribe`. Failures:
1. **Stale read model**: it promotes someone already promoted or removed (double enrolment).
2. **At-least-once delivery / processor reset**: AF5 streaming processors re-deliver after crashes; and a fresh processor catch-up is marked `REGULAR` not `REPLAY` in AF5, so handlers that relied on replay detection to skip side effects will re-issue commands (migration doc).
3. **Barging**: between the freed seat and the promotion, a new `Subscribe` from a non-waiting student takes the seat, which violates fairness.
4. **Promoted student no longer eligible**: since joining the list the student hit their course limit or a schedule conflict.
5. **Order**: fairness must follow event order, not read-model timestamp.
6. **Parallel segments** process events for the same course concurrently.

**Why it happens:**
Developers carry the saga mindset (react to an event with a command, rely on the projection for data).

**Consequences:**
The most visible demo feature is also the most likely to show a wrong result.

**How to avoid:**
- Make promotion a **DCB decision** (`PromoteNextFromWaitingList(courseId)`): the decision model sources `StudentWaitlisted`/`Removed`/`Promoted`/subscription events from the write side, picks the head *itself* by event position, re-checks capacity, student limit and schedule for that candidate, skips ineligible candidates (emitting an explicit event), and is **idempotent** (no free seat or empty list means no-op).
- `Subscribe` must include waiting-list events in its boundary and enqueue (or reject) when the list is non-empty, so seats cannot be barged.
- The automation processor only triggers the decision; it never chooses the student. Use a sequencing policy by course id (or rely on conflict + idempotency) and test with duplicate event delivery and processor reset.
- Capacity increase can free several seats: loop or issue one promotion per free seat, each idempotent.
- Avoid event cycles: promotion emits events that trigger more promotions; assert termination in tests.
- Do not use Workflows (Axoniq, preview, licensed for production) for this unless deliberately showcased; the plain event processor plus idempotent decision is the lesson. No saga/deadline exists in AF5 core; do not simulate timeouts (waiting-list expiry) unless required; if needed, use Spring `@Scheduled` plus a query for due work (the approach migrators reported).

**Warning signs:**
Automation service injects a query gateway to ask "who is next"; promotions produce duplicate `StudentSubscribed` for one student; test that reruns the processor from the start changes state.

**Phase to address:** P4 (with P2 supplying the decision model).

---

### Pitfall 10: Unique-name constraint modeled incorrectly (rename, normalisation, reuse)

**What goes wrong:**
(a) `CourseRenamed` tagged only with `Course:{id}`, not with the old and new name tags, so the old name is never seen as released and the new name's boundary is not checked. (b) No normalisation: `"Kotlin 101"` vs `"kotlin 101 "` bypass uniqueness (dcb.events warns a real system would normalise/hash the value). (c) Name checked in `CreateCourse` but not in `RenameCourse`. (d) Name tag appended on create but release not recorded, so the name is blocked forever. (e) Tag value limits or special characters in the name (**[VERIFY]** Axon Server tag key/value constraints) breaking appends.

**How to avoid:**
Follow the dcb.events "unique username" pattern: tag every name-related event `CourseName:{normalised}`; `CourseRenamed` carries **both** old and new name tags in one event so release and claim are one decision; decisions load name events of the *new* (and, for rename, the *old*) name, with types `CourseCreated`, `CourseRenamed`; decide retention explicitly (the reference example holds a released name for N days; here probably "released immediately"). Normalise in one function used by the decision and by the tag resolver. Test: create/rename races for the same name; rename A to B then create A.

**Warning signs:**
Two courses with names differing only by case; renamed course's old name still rejected.

**Phase to address:** P2.

---

### Pitfall 11: Transfer and schedule decisions that quietly drop atomicity or ignore schedule changes

**What goes wrong:**
- Transfer implemented as two commands (unsubscribe then subscribe) or as unsubscribe plus waiting-list promotion plus subscribe across services: loses atomicity, and the source seat can be promoted to a waiting student before the transfer completes.
- Transfer as one decision but appended as two events with different tag sets: a downstream projection sees an intermediate state, and a criteria list that omits the new event type (Pitfall 5) corrupts the counts.
- Overlap check ignores half-open interval semantics (back-to-back slots), time zones, recurrence, or compares against stale slot data.
- **Schedule change after subscriptions exist** invalidates the invariant, and checking it requires loading every enrolled student (a huge boundary).

**How to avoid:**
Decide and write down in an ADR (P2): single `StudentTransferred` event vs atomic multi-event append (recommend the single event tagged with Student, FromCourse, ToCourse so one fact is visible in the explorer); use half-open `[start, end)` intervals in one value object; a single fixed time zone or UTC for the demo; **schedule is immutable once the course has subscribers** (or changing it requires zero subscribers), which avoids the huge-boundary decision and is a legitimate DCB design statement. Transfer must trigger waiting-list promotion for the source course only after commit (via event, not inline). Test transfer when the target is full, when the student is already in the target, and when the transfer would create an overlap.

**Warning signs:**
Transfer implemented through `CommandGateway.send` from inside another command handler.

**Phase to address:** P2, P4.

---

### Pitfall 12: Axon 4 muscle memory: APIs and concepts that changed or vanished

**What goes wrong:**
Developers (and LLMs) write AF4 code against AF5. The traps that matter here:

| AF4 habit | AF5 reality | Source confidence |
|-----------|-------------|-------------------|
| `@Aggregate`, `@AggregateIdentifier`, constructor command handlers, `AggregateLifecycle.apply` | `@EventSourced` (Spring) / `@EventSourcedEntity(tagKey=...)`, `@EntityCreator` (**mandatory**), static creational handlers, `EventAppender` parameter | HIGH |
| `CurrentUnitOfWork`, ThreadLocal | `ProcessingContext` passed as explicit parameter; dispatch interceptors may run with a **null** context | HIGH |
| Synchronous interceptors | `MessageStream`-returning interceptors; `proceed(message, context)` | HIGH |
| `CommandGateway.send()` returns `CompletableFuture<R>` | returns `CommandResult` | HIGH (dev.to) |
| Multiple event handlers for one event in an entity hierarchy: first match | **all** run, parent first; merge or call `super` | HIGH |
| `@Revision`, `MetaData` (`Map<String,Object>`) | `@Event(version=...)` , `Metadata` (`Map<String,String>`) | HIGH |
| `ResponseTypes.multipleInstancesOf` | `queryMany`; **scatter-gather queries not supported** | HIGH |
| `@ProcessingGroup`, `TrackingEventProcessor`, `EventProcessingConfigurer` | `@Namespace`, `PooledStreamingEventProcessor`, `EventProcessorDefinition`/`MessagingConfigurer#eventProcessing` | HIGH |
| `QueryUpdateEmitter` as a field | must be a **handler parameter** | HIGH |
| `Serializer` (`JacksonSerializer`/XStream) | `Converter` family (General/Message/Event); `axon.serializer.*` becomes `axon.converter.*`; XStream gone | HIGH |
| `@Saga`, `DeadlineManager`, `EventScheduler` | removed from core; **Workflows** (Axoniq, preview) or legacy module | HIGH |
| `createHeadToken`/`createTailToken` | `latestToken(...)`/`firstToken(...)` return futures; names are inverted vs intuition, the compiler will not catch a swap | HIGH |
| Initial catch-up is a `REPLAY` for new processors | AF5 marks it `REGULAR`; side-effecting handlers run during catch-up | HIGH |
| Command metadata flows to events | **does not**; `EventAppender.append` starts with empty metadata unless a `CorrelationDataProvider` bean is registered | MEDIUM (forum, maintainer-confirmed) |
| `@InjectEntity` creational handler tolerant of absence | 5.3: may throw `EntityNotFoundException` unless the parameter is nullable (`State?` in Kotlin) | HIGH |

**Why it matters here:**
The command-metadata gap breaks the planned "event explorer shows which command caused an event" and any user attribution (role switcher). The REGULAR-not-REPLAY change breaks automation idempotency assumptions (Pitfall 9).

**How to avoid:**
Treat the official AF5 docs and the AxonIQ university-demo(-kotlin) repo as the only style references; ban AF4 blog posts in AI prompts; keep a `docs/af5-notes.md` cheat sheet fed by this table; register a `CorrelationDataProvider` (and a test asserting stored events carry causation/command id and role metadata) in P2; use explicit names and versions (`@Event(name=..., version=...)` **[VERIFY]**) so a package rename does not break stored events or cross-service deserialisation.

**Warning signs:**
Imports from `org.axonframework.commandhandling`, `...config.Configurer`, `AggregateLifecycle`; stored events without metadata.

**Phase to address:** P1 (cheat sheet, style rules), P2.

---

### Pitfall 13: Treating AF5 DCB/Kotlin as fully mature; upstream churn between 5.x minors

**What goes wrong:**
AF5 shipped (5.0 late 2025) and is at 5.4 as of this research. Release notes still show active API work: 5.1 revised snapshotting and moved modules; 5.2 removed the OTel extension, changed the default of the Jackson 3 converter, added upcasting (not available in 5.0); 5.3 had a minor breaking change (`@InjectEntity`). Docs still contain milestone-era text ("may be subject to change"). The DCB blog still says "experimental" while Axon Server docs say "full support" since 2025.2. Known open gaps: `EventCriteria` AND/any (Pitfall 6), Axon Server UI shows only one value for duplicate tag keys (confirmed UI bug, data is correct, no fix version), no `givenState`/time-based fixtures, no deadlines.

**How to avoid:**
Pin exact versions in a Gradle version catalog; read release notes at every Renovate bump; keep a "Known upstream gaps" section in the docs; do not build the event explorer on top of the Axon Server UI; write the first spike tests so a regression on upgrade is detected immediately (Pitfall 3 smoke test).

**Phase to address:** P1 (pin and spike), P8 (upgrade policy).

---

### Pitfall 14: Distribution of services breaking DCB assumptions (shared contracts, event class resolution, per-service stores)

**What goes wrong:**
Command, query, automation and gateway services share event and command classes. If each service redefines the classes, qualified names or tags drift. If one shared `api` module leaks decision code, the services re-couple. A query service that depends on the whole write-side module breaks the CQRS split and the architecture tests. Token stores and read-model databases shared across services with identical processor names steal each other's tokens.

**How to avoid:**
Shared `contracts` module containing only commands, events, tag constants and query/response types (no logic); ArchUnit rules (P7): query/automation/gateway may depend on contracts only; one database (schema) per service; unique, explicit `@Namespace` processor names, prefixed by service; automation issues commands through `CommandGateway` only; unique `axoniq.platform.application-name` per service (Pitfall 24).

**Phase to address:** P1 (module layout), P7 (enforcement).

---

## Kotlin and Spring Pitfalls

### Pitfall 15: Kotlin plugin, nullability and serialisation traps

**What goes wrong:**
- **all-open / no-arg**: `kotlin-spring` (all-open) is needed for Spring beans using proxies; `kotlin-jpa` (no-arg) for JPA read-model and token entities. Axon entities and event data classes do not need no-arg classes, but `@EntityCreator` must resolve a constructor: prefer a no-arg or ID-only creator, with state set by the first `@EventSourcingHandler`.
- **Immutable entities**: AF5 supports state classes whose `evolve` returns a new instance; Kotlin data classes with `copy` fit (the Kotlin extension docs show `evolveIf`). Mutating `var` fields in entities is the AF4 habit.
- **Nullability**: Kotlin `T?` is invisible to Java reflection; the `axon-kotlin` module (`KotlinReflectNullabilityResolver`, `KotlinReflectPropertyAccessStrategy`, loaded via ServiceLoader) fixes it. Without it on the classpath, a `State?` parameter may be treated as non-null and creational handlers throw (5.3 change).
- **JSpecify**: AF5 ships JSpecify annotations; Kotlin 2.x may treat them strictly, producing new compile errors on platform types. **[VERIFY]** against the Kotlin version in use.
- **Jackson**: Spring Boot 4 brings Jackson 3 (`axon.converter` value `jackson`), Boot 3 uses Jackson 2 (`jackson2`). Jackson 3 changed `final` field deserialisation. The Kotlin module must be registered on the `ObjectMapper` that the Axon `JacksonConverter` uses (**[VERIFY]**: Axon may build its own mapper; check by round-tripping a Kotlin data class event with nullable and default parameters). Value classes: forum reports limited Jackson support (use data class or string-backed IDs, or verify with a round-trip test). Defaults for missing properties: adding a field with a default to an event data class works only if the Kotlin module is active.
- **Event evolution**: stored events are immutable; renaming a property breaks old events. Add fields as nullable/default; use `@Event(version=...)` plus upcasters (available from 5.2) only if you must.

**How to avoid:**
A Kotlin conventions ADR in P1: IDs are `String`-backed value types with explicit `toString()` (or raw strings) to make tags predictable; event round-trip test over **all** event classes (serialise, deserialise, equals, tags present) in P2; add `axon-kotlin`; explicit `-Xjsr305`/JSpecify settings.

**Warning signs:**
`MissingKotlinParameterException`; tags containing `CourseId(value=...)`; `NullPointerException` in creational handlers; stored event JSON containing wrapper objects.

**Phase to address:** P1 (conventions), P2 (round-trip tests).

---

### Pitfall 16: Spring Boot autoconfiguration and dependency conflicts

**What goes wrong:**
- Starter coordinates changed (Pitfall 1); AF5 releases 5.1 fixed several Spring Boot 4 regressions (cyclic dependencies, autoconfig ordering), so pick the latest patch. Axon Framework 5.0.3+ lists Spring Boot 3 and 4 (table not fully legible, **[VERIFY]**).
- `axon.axonserver.enabled=false` makes tests fall back to in-memory/JPA event storage; a test that passes there does not prove DCB semantics (Pitfall 3).
- The Axon Server connector needs `protobuf-java` >= 4.29.3. Known issue: a transitive 3.x wins and the app fails at startup with `NoClassDefFoundError: com/google/protobuf/RuntimeVersion$RuntimeDomain`. The official workaround is an explicit `protobuf-java` 4.x declaration. In Gradle with Spring's dependency-management plugin or the Boot BOM, a managed older version can win; add a constraint and check `dependencyInsight`.
- Client TLS certificates are read once at connect; rotation needs a restart.
- PostgreSQL + Hibernate maps Axon's `@Lob` token columns to Large Objects (oid): use the BYTEA mapping for the token store (official known issue).
- JPA token store needs a new `mask` column; the framework cannot start without it.

**How to avoid:**
Explicit dependency alignment block in the build; Flyway for read-model schemas but let the token store DDL be an explicit migration verified by a test; actuator health of each service includes Axon Server connection state.

**Phase to address:** P1.

---

## CQRS and Read-Side Pitfalls

### Pitfall 17: Eventual consistency leaking into UI and tests

**What goes wrong:**
`POST /subscribe` returns 202/200 after the append, the UI immediately refetches and shows stale data (the projection has not caught up), or flaky tests assert on the read model right after the command, "fixed" with `Thread.sleep`.

**How to avoid:**
- API returns the **event position/ID** or a correlation ID; the UI shows an optimistic "pending" state until the matching subscription-query update arrives; never refetch-after-POST as the consistency strategy.
- Tests use Awaitility (backend) and Playwright web-first assertions (UI); never fixed sleeps. Provide a shared test helper "await until projection processed position >= X" exposed by the query service (read-side watermark) for integration tests.
- Read-side queries that need read-your-writes (own subscription list after subscribing) go through the watermark helper.
- Document which operations are strongly consistent (decisions) and which are eventually consistent (read models) in the README: this is a teaching point.

**Warning signs:**
`Thread.sleep` in tests; UI "flicker" after actions; E2E tests with retries enabled to pass.

**Phase to address:** P3 (watermark), P5, P6, P7.

---

### Pitfall 18: Projection replay, token store and processor configuration

**What goes wrong:**
In-memory token store (default when no store is configured) re-processes the whole stream on every restart and re-emits subscription query updates; processors from different services with the same name share a token row; `TrackingToken` class names and `mask` column differences; Axon Server token positions: AF4 stores last handled index, AF5 stores next index (migration only; irrelevant for greenfield, but an AF4-era tutorial may cause confusion). Replays are not distinguished from regular catch-up (Pitfall 9) so a rebuild of a projection that also sends notifications or commands repeats them.

**How to avoid:**
Persistent JPA token store in each service's DB from day one; explicit `@Namespace` names; a documented, scripted "rebuild projection" procedure (reset token plus truncate tables, tested in P3); keep side effects out of projectors; automation decisions idempotent (Pitfall 9); read-model tables use upserts keyed by event identity to tolerate redelivery; a replay test (delete read model, reset token, assert identical content).

**Phase to address:** P3.

---

### Pitfall 19: Subscription-query and SSE lifecycle leaks, proxies and reconnection

**What goes wrong:**
- AF5 `subscriptionQuery` returns a Reactor `Publisher` (initial result plus updates); reactor-core is mandatory. The subscription is closed server-side only when the publisher is **cancelled/disposed**; an SSE endpoint that does not cancel on client disconnect leaks one subscription (and emitter registration) per page load.
- React StrictMode double-mounts effects in development, opening two EventSources; missing `close()` in the cleanup leaks connections; browsers limit about 6 HTTP/1.1 connections per origin: several SSE streams per page block normal fetches.
- Proxies (nginx, the Vite dev proxy, API gateways, corporate proxies) buffer or time out SSE: need `proxy_buffering off`, `X-Accel-Buffering: no`, long read timeouts, no gzip on the stream, and a heartbeat comment every 15-30 seconds.
- `EventSource` cannot set headers, but the demo role switcher uses a role header. Use a cookie or query parameter for SSE only, and treat it as a demo mechanism, not authentication.
- Reconnection: `EventSource` auto-reconnects but a subscription query restarts with a fresh initial result; without `Last-Event-ID`/versioning the UI can show gaps or duplicates after reconnect. Simplest correct design: **every (re)connect starts with a full initial snapshot**, so the client replaces state rather than merging.
- Spring MVC `SseEmitter` thread usage vs WebFlux: mixing MVC and Reactor in the gateway creates blocking in event loops (**[VERIFY]** chosen stack; WebFlux gateway is the more natural fit).
- Backpressure: update bursts during the concurrency demo or replay overwhelm slow clients; use `onBackpressureLatest`/conflation per view.
- Scatter-gather subscription is unsupported in AF5; subscription updates go from the query service emitter to the subscriber via Axon Server, and replaying a projection emits updates for old events (Pitfall 18) unless updates are suppressed during catch-up.

**How to avoid:**
A tested `SseGateway` utility in P5: cancel on disconnect (assert subscription count returns to zero via a metric), heartbeats, snapshot-on-connect, bounded buffers; Playwright test that closes pages and asserts server-side subscription gauge returns to baseline; run the E2E suite through the real reverse proxy used in compose, not only the dev server.

**Warning signs:**
Gauge of active subscriptions only grows; UI stops updating after 60 seconds idle; works on localhost, breaks behind nginx.

**Phase to address:** P5 (backend), P6 (client), P7 (leak test).

---

## Testing Pitfalls

### Pitfall 20: AxonTestFixture misuse and integration tests that prove nothing

**What goes wrong:**
- `AxonTestFixture` is unified (replaces `AggregateTestFixture`/`SagaTestFixture`). Direct state injection (`givenState`), time manipulation (`whenTimeElapses`), deadline checks and `expectState` are not available: build state with events.
- Forgetting `fixture.stop()` leaks resources across tests; Spring requires `MessagesRecordingConfigurationEnhancer` registered as a bean (use `@AxonSpringBootTest`).
- Heavy infrastructure (Axon Server) is off by default; `asIntegrationTest()` opts in. A suite that never opts in never exercises DCB conflict detection.
- Given events without correct tags produce a green test with empty state (Pitfall 4).
- Tests tightly coupled to implementation (asserting exact event sequences including incidental events, or internal criteria), making every refactor break them.

**How to avoid:**
Pyramid with explicit roles: (1) pure decision tests via fixture (given events, when command, then events and tags, fast); (2) component tests per service with in-memory/disabled Axon Server; (3) a **small** integration set against real Axon Server DCB containing the concurrency and conflict tests that only a real store can prove; (4) E2E via browser for critical user journeys only. Assert on business outcomes (events/outcome classification), not on infrastructure.

**Phase to address:** P2, P7.

---

### Pitfall 21: Slow, flaky Testcontainers and E2E

**What goes wrong:**
Per-class container startup (Axon Server about tens of seconds, plus Postgres, plus services) multiplies across test classes; parallel Gradle forks start multiple servers; DCB context creation not complete when tests begin; append-only event store cannot be truncated between tests (Axon Server's purge is disabled; the sanctioned reset is delete-and-recreate context), so tests interfere through shared data and unique-name constraints.

**How to avoid:**
Singleton container per test source set (static holder, Testcontainers reuse in dev, not CI), readiness check on the actuator endpoint **and** a successful DCB probe append; each test uses randomly generated IDs and names (name uniqueness makes this mandatory); no cross-test data assumptions; cap Gradle `maxParallelForks`; Playwright: one worker per compose environment or per-test namespaces, `trace: on-first-retry`, no retries masking bugs, web-first assertions, data-testids; E2E count capped (about 10-15 journeys) and tagged critical; nightly full suite vs PR smoke subset. Cache images in CI and pre-pull.

**Phase to address:** P7, P8.

---

### Pitfall 22: Load-test results misinterpreted

**What goes wrong:**
Reading latency percentiles from a closed-loop test (coordinated omission), laptop docker numbers published as "throughput of Axon", ignoring that contention on a single course is *by design* serialised in DCB (the throughput for one hot course is bounded by conflicts and retries), ignoring licence throughput caps, Axon Server default heap (512 MB image default), cold JIT, projection lag mistaken for command latency, and counting expected business rejections as errors.

**How to avoid:**
Define success as **invariants**: after the run, successes per course == capacity, no student above the limit, no overlap, waiting list order preserved. Use an open-model arrival rate (k6 `constant-arrival-rate`, Gatling open injection), warm-up, fixed resource limits in compose, record versions and hardware, report conflict/rejection/error classes separately, keep the load test in a separate non-gating workflow, and present numbers as "illustrative on this hardware." Measure hot-course (everyone races one course) and spread (many courses) scenarios separately.

**Phase to address:** P7.

---

### Pitfall 23: Architecture tests and coverage tooling fighting Kotlin

**What goes wrong:**
ArchUnit works on bytecode, so Kotlin file-level classes (`FooKt`), companion objects and lambdas appear as classes that violate slice rules; JaCoCo reports penalise Kotlin data class generated methods, inline functions and `when` branches (low, misleading coverage), pushing people to write meaningless tests or lower thresholds.

**How to avoid:**
Slice by package with explicit rules that ignore synthetic classes; use Kover (or JaCoCo with Kotlin-aware excludes) with generated-code filters; set the gate on decision/domain packages (high threshold) rather than a global number; report coverage but gate on mutation or invariant tests if added.

**Phase to address:** P7.

---

## Observability and Platform Pitfalls

### Pitfall 24: OpenTelemetry in AF5: wrong module, double instrumentation, dashboards from AF4

**What goes wrong:**
`axon-tracing-opentelemetry` is gone (removed in 5.2). Tracing needs the Axoniq `axoniq-tracing-micrometer` binding plus `micrometer-tracing-bridge-otel` and an OTLP exporter (and `spring-boot-starter-actuator`); without it only a `LoggingSpanFactory` exists and nothing is exported. Axon does **not** configure the OTel SDK any more (sampling, exporters, propagation are the app's). Span names and attributes changed (`CommandBus.dispatch <name>`, `axon_*` became `axoniq.*`, tags as `axoniq.event_tag.*`), so AF4 dashboards break. Using the OTel Java agent **and** the Micrometer bridge duplicates spans. Pending batches are lost unless `tracerProvider.close()` runs at shutdown.

**Context propagation specifics:** distributed commands and queries continue the dispatcher's trace; event processing is asynchronous: processor spans are parented to the batch span with a link back to the publisher, and same-trace mode is limited by `distributed-in-same-trace-time-limit` (stale events start a linked trace). Subscription query updates produce linked producer/consumer spans. Expect a replay or catch-up to look like hours-long or orphaned traces. How trace context actually travels inside Axon Server messages is not documented beyond "continues the trace" (**[VERIFY]** by running a command from the gateway through the command service and checking one trace id in Tempo, plus an event processed in the query service via a span link).

**How to avoid:**
Choose one instrumentation path (Micrometer tracing bridge + OTLP, recommended; note this pulls an Axoniq module, see Pitfall 1); a P9 acceptance test: a single trace spanning browser (optional) to gateway to command service to Axon Server to automation/query processor; MDC `trace_id` in logs shipped to Loki; set the sampling rate and resource attributes (`service.name`) per service; dashboards built against the AF5 span names; flush on shutdown; keep the Grafana stack memory-limited.

**Phase to address:** P9.

---

### Pitfall 25: AxonIQ Platform connection: wrong property, shared identity, committed token

**What goes wrong:**
- Two different configuration surfaces: the **framework client** uses `axoniq.platform.credentials=<environment-id>:<access-token>` (without it the autoconfiguration does nothing) and Axon Server uses `axoniq.platform.authentication` (container env `axoniq_platform_authentication`) with the same `ENV_ID:TOKEN` form. Mixing them silently disables the connection.
- The token must have the right connector scope (Axon Server connector or all connectors); a revoked or expired token disconnects immediately, and every node must be updated.
- All four services report under the same name unless `axoniq.platform.application-name` is set (it defaults to the Spring application name, so make sure those are distinct).
- Outbound connectivity: framework client defaults to `framework.console.axoniq.io:7000`; Axon Server uses port 7001 to a fixed IP over TLS/RSocket. Corporate firewalls/VPNs block it. Retry/offline behaviour of the client is undocumented (**[VERIFY]**: start with no network and confirm the application and the compose stack stay healthy).
- Data exposure options default to the safe side (`dlq-mode: NONE`, `domain-event-access-mode: NONE`); do not turn them to `FULL` casually, as that lets the Platform read payloads.
- **The token must never be committed**: it ends up in `docker-compose.yml`, `application.yml`, shell history, CI logs (`docker compose config`, `env`), image layers, and `/actuator/env` (verify sanitisation of `*credentials*` keys on the Boot version in use).

**How to avoid:**
- Compose reads `AXONIQ_PLATFORM_CREDENTIALS` / `AXONIQ_PLATFORM_AUTHENTICATION` from an untracked `.env`; commit only `.env.example` with obvious placeholders (`ENV_ID:TOKEN_PLACEHOLDER`); `.env` in `.gitignore` **and** `.dockerignore`; the app must start cleanly when the variables are empty (feature flag by profile `platform`).
- Secret scanning in three layers: pre-commit hook (gitleaks), CI scan of full history (`fetch-depth: 0`), GitHub push protection; a custom gitleaks rule for the `ENV_ID:TOKEN` shape if the token format is distinctive.
- Never echo the variables in CI; Platform-connected steps run only on the main repository (fork PRs do not receive secrets) and are optional.
- Document the revocation/rotation procedure, and revoke the token used during the project's development once the demo is recorded, if it was ever pasted into an AI chat or terminal log (this project is AI-assisted: do not paste the token into prompts or `.planning/` files).

**Phase to address:** P9 (config), P8 (scanning), P1 (`.gitignore`/`.env.example` established on day one).

---

### Pitfall 26: Compose startup ordering and Axon Server hostname

**What goes wrong:**
The Axon Server image has `AXONIQ_AXONSERVER_HOSTNAME` defaulting to `localhost`; clients are redirected to the advertised hostname, so in compose the services connect to the server, are told "go to localhost:8124" and fail. The official compose sample defines no health check (only K8s probes use `/actuator/info` on 8024). "Healthy" does not mean the DCB context exists. Services starting before Axon Server retry noisily or crash. Memory: default 512 MB heap; five Grafana-stack containers plus Postgres plus four JVMs exceed laptop budgets.

**How to avoid:**
Set `AXONIQ_AXONSERVER_HOSTNAME` to the compose service name (and publish ports for host-based tests with a separate hostname strategy); healthcheck on 8024 `/actuator/info` plus an init container/script verifying the DCB context; `depends_on: condition: service_healthy`; services tolerate a late server (connection retry); explicit memory limits and `-XX:MaxRAMPercentage`; separate compose profiles (`core`, `observability`, `load`) so the base demo runs on 8 GB.

**Phase to address:** P1 (core), P9 (profiles).

---

## CI/CD and Supply Chain Pitfalls

### Pitfall 27: Security tooling that is itself the supply-chain risk, plus noisy scanners

**What goes wrong:**
In March 2026 the official `aquasecurity/trivy-action` and `setup-trivy` tags were force-pushed to malicious commits (infostealer exfiltrating CI secrets, about 12 hours of exposure; a malicious Trivy binary was also published). Projects that referenced tags (`@v0.x`) were exposed; pinned commit SHAs were not. A vulnerability scanner with `contents`/token access is exactly the thing an attacker wants to compromise. Separately, scanners fail builds on unfixable base-image CVEs, un-exploitable transitive findings and test-only dependencies, causing teams to disable or ignore them.

**How to avoid:**
- **Pin every third-party Action by full commit SHA** (first-party `actions/*` also, for a reference repo whose theme includes supply chain), with Renovate maintaining the digests; least-privilege `permissions:` per job; no `pull_request_target` with checkout of fork code; scanners run in jobs without secrets.
- Prefer scanner binaries installed from a verified release (checksum/signature) or run as pinned container digests rather than third-party wrapper actions; consider two independent scanners (for example Grype/Trivy for images, OSV-Scanner or OWASP dependency-check for Gradle) but keep one gate.
- Separate **report** (SARIF to Code Scanning, always) from **gate** (fail on fixable HIGH/CRITICAL only: `--ignore-unfixed`); every suppression in `.trivyignore`/VEX has a justification and an expiry date; scan runtime images only, not test fixtures.
- Provide SBOM (CycloneDX) and provenance/attestation (`actions/attest-build-provenance`) plus image signing (cosign keyless) as showcase items; keep them in a dedicated release workflow.

**Phase to address:** P8.

---

### Pitfall 28: Renovate configuration pitfalls

**What goes wrong:**
- Running both Renovate and Dependabot; onboarding PR floods; no grouping, so Axon BOM, Spring Boot, Kotlin and Testcontainers bump separately and break compatibility (Kotlin and Spring Boot versions are coupled; Axon Framework, Axoniq starter and Axon Server image must move together; Axon Server 2025.1 to 2025.2 invalidated DCB contexts).
- `minimumReleaseAge` reportedly does not apply to `digest`/`pinDigest` updates unless a separate package rule is added (user report on Codeberg, **[VERIFY]** against current Renovate docs); it also applies per version, and frequently released images can starve updates.
- Auto-merge on major bumps or on Axon/Axoniq packages.
- Docker image tags pinned without digests; GitHub Actions pinned by tag.
- Renovate needs a hosting model (GitHub App, or self-hosted workflow with a PAT or GitHub App token): a long-lived PAT is itself a secret to protect.

**How to avoid:**
Extend `config:best-practices` (pins digests for Docker and GitHub Actions); package rules grouping `org.axonframework`/`io.axoniq.framework`/Axon Server image into one "axon" group with no automerge and a PR label "needs-spike-rerun"; group Kotlin plus KSP/Kotlin plugin plus Spring Boot per compatibility; `prConcurrentLimit`, weekly schedule, dependency dashboard; a `minimumReleaseAge` (3-7 days) and an explicit digest rule; Gradle version catalog and wrapper supported; one tool only.

**Phase to address:** P8.

---

### Pitfall 29: Secret scanning false positives and blind spots

**What goes wrong:**
Gitleaks flags test fixtures, example tokens in docs/ADRs and `.planning/` content (this repo publishes AI planning history), base64 test data and compose dev passwords. Teams respond with broad regex allowlists that hide real secrets. Conversely, a scan on the shallow clone sees no history, so a token committed and removed earlier is missed; the gitleaks-action may require a licence key for org-owned repos (unverified, check).

**How to avoid:**
Path-scoped allowlists (never generic regexes), inline `gitleaks:allow` only with a comment, baseline file for any historical findings, full-history scan in CI (`fetch-depth: 0`), pre-commit hook, GitHub native secret scanning plus push protection, placeholders that are obviously fake and do not match real token patterns; a scheduled TruffleHog verified-only scan as a second opinion. If a real secret is ever committed: rotate first, rewrite history second.

**Phase to address:** P8 (and the `.gitignore`/`.env.example` baseline in P1).

---

### Pitfall 30: Container image size and build reproducibility

**What goes wrong:**
Fat Kotlin/Spring Boot jars in `eclipse-temurin:21-jdk` images produce 400+ MB images and many base-image CVEs; layer cache busted on every code change; Apple Silicon development vs amd64 CI produce different images; JVM defaults ignore container memory limits; unpinned base images change under you.

**How to avoid:**
Layered jar (`bootJar` layers) or Jib/buildpacks, JRE-only or distroless/hardened base pinned by digest, non-root user, `MaxRAMPercentage`, multi-arch builds with buildx, one parametrised Dockerfile for the four services, image size recorded in CI as a metric with a budget, `.dockerignore` including `.env`, `.planning`, `build`.

**Phase to address:** P8.

---

## Reference-Project Pitfalls

### Pitfall 31: Documentation drifting from code

**What goes wrong:**
README quick start stops working; snippets in docs and the AF4-vs-DCB comparison are hand-copied and go stale or never compiled; ADRs not updated when decisions change (for example Axoniq split, licence behaviour); C4/Event Modeling diagrams exported as images with no source; hard-coded counts, versions and ports in prose; the AF4 comparison containing incorrect AF4 code or a straw man (for instance claiming Axon 4 cannot do cross-entity consistency without noting saga/reservation or the limited DCB support that existed late in the 4.x line; **[VERIFY]** before asserting anything about AF4 DCB).

**How to avoid:**
Docs are built in CI: code snippets pulled from real source files (include tags/regions), diagrams as code (Structurizr/Mermaid/PlantUML) rendered in CI, link checker, README quick start executed in a smoke job (`docker compose up`, one command, assert one subscription), version numbers injected from the version catalog, ADR index with status and a "supersedes" field, a CI check that fails if an ADR-referenced file moves. The AF4 comparison uses compile-checked snippets in a separate, non-shipped sample source set or is clearly labelled illustrative and reviewed against AF4 docs. `.planning/` history is part of the product: keep STATE and ROADMAP consistent at each phase transition.

**Phase to address:** P10 (but snippet-extraction infrastructure in P1 so every phase writes docs against it).

---

### Pitfall 32: Over-engineering that buries the DCB lesson (and duplicating AxonIQ's own demo)

**What goes wrong:**
Four services, gateway, OTel stack, Platform, supply-chain tooling and a React app make the repo large; a reader looking for "how does DCB protect capacity?" cannot find it in under five minutes. AxonIQ already ships a university domain tutorial/demo (`university-demo`, `university-demo-kotlin`, Getting Started with subscribe-student), so a reference that merely re-implements it adds little.

**How to avoid:**
Make the DCB core unmissable: one `write-side` module, vertical slices (command, decision model, test side by side, one file per use case as in the official tutorial), a `docs/dcb-in-5-minutes.md` pointing to exactly 3 files; infrastructure (security, observability, CI) kept out of domain code; each phase ends with the repo still runnable and the domain slice readable. Differentiators to emphasise: the multi-entity decisions beyond the tutorial (transfer, waiting list, schedule overlap), the concurrency demo with provable invariants, and the event explorer showing the actual boundary. Defer anything not on that path (no extra auth, no extra services).

**Phase to address:** All; checkpoint at the end of P2 ("could a newcomer find the lesson?").

---

### Pitfall 33: Event explorer "which events formed the boundary" has no obvious framework hook

**What goes wrong:**
The UI feature requires showing the criteria and consistency marker used by a command. AF5 builds the append condition internally (the marker is returned as the final entry when sourcing; "users rarely need to handle it directly"); there is no documented read-after-the-fact API. Reconstructing it later from the event store is ambiguous (criteria may differ from what was used).

**How to avoid:**
Design in P2: record, per command, a small "decision record" (command id, criteria in readable form, marker/position, number of events sourced) as event metadata or a separate audit event/log, written through the same interceptor/`CorrelationDataProvider` path as Pitfall 12; do not wait until the UI phase. Alternatively compute the boundary from the declared projections in code and expose it via the query service. Flag for deeper research at the start of P2/P6 (hook points in `EventSourcedEntity` loading, `AppendCondition`/`ConsistencyMarker` access).

**Phase to address:** P2 (capture), P6 (display).

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Tag-only criteria (no event types) | Fast to write | Huge boundaries, false conflicts, slow commands | Never for hot entities (course); fine for throwaway spikes |
| Query the read model inside a command handler for "who's next" | Avoids modeling the waiting list as events | Stale decisions, double enrolment | Never |
| `Thread.sleep` to wait for projections | Green tests today | Flaky CI forever | Never; use watermark/Awaitility |
| `axon.axonserver.enabled=false` for all tests | Fast, no containers | DCB semantics never exercised | Only for pure decision tests; keep a real-server set |
| Share one DB/schema across services | Less compose | Token/processor-name collisions, hidden coupling | Never |
| Value-class IDs without a round-trip/tag test | Type safety | Wrong tags / wrong JSON | Only after a test proves tags and JSON |
| Global gitleaks regex allowlist | Quiet CI | Real secrets hidden | Never; path-scoped only |
| Pin Actions by tag | Readable workflows | Exposure to tag-rewrite attacks (Trivy, March 2026) | Never for this repo |
| `latest` Axon Server image | Always current | Licence/DCB breaking changes between releases | Never |
| Hand-copied code in docs | Quick docs | Drift | Never; extract from source |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Axon Server (compose) | Default hostname `localhost` advertised to clients; no health check; non-DCB context | Set `AXONIQ_AXONSERVER_HOSTNAME`, health check on `/actuator/info`, DCB standalone flag, verify with a probe append |
| Axon Server (tests) | Container per class; no DCB; license assumed | Singleton `AxonServerContainer` with DCB initializer; random IDs; readiness probe |
| Axoniq Platform | Wrong property name for framework vs server; token in compose file; same app name for all services | `axoniq.platform.credentials` vs `axoniq.platform.authentication`; env from untracked `.env`; unique `application-name` |
| AF5 and Jackson | Kotlin module not on the mapper Axon uses; Jackson 2 vs 3 confusion | Explicit `axon.converter.*` choice, round-trip test for all events |
| OTel | Agent plus Micrometer bridge; AF4 attribute names | One path, AF5 span names, linked traces for async processing |
| Postgres token store | Hibernate maps `@Lob` to oid; missing `mask` column | BYTEA mapping, explicit DDL migration |
| React SSE | EventSource through buffering proxy; no heartbeat; no cleanup | Proxy settings, heartbeats, cancel on disconnect, snapshot on connect |
| Renovate | Ungrouped Axon bumps; no digest age rule | Axon group, no automerge, explicit digest rule |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Sourcing the whole course stream per `Subscribe` | Latency grows with history | Types plus tags; bounded streams; snapshots if measured | Hundreds to low thousands of events per course |
| Retry storm in a last-seat race | Error spikes, long tails | Bounded retries with jitter; classify outcomes | 100+ concurrent writers on one course |
| Hot-course serialisation | Throughput flat regardless of replicas | Accept by design; show in demo; spread scenario | Any single hot course |
| SSE fan-out with per-client subscription queries | Memory/subscription growth | One shared stream per view, conflation, cancel on disconnect | Dozens of open tabs |
| Projection replay emitting updates | UI flood, CPU spike | Suppress emission during catch-up | Rebuilding with large history |
| 4 JVMs + Axon Server + Postgres + Grafana stack | Laptop swaps, compose timeouts | Compose profiles, memory limits | 8 GB machines |
| Docker-on-laptop benchmark | Non-reproducible numbers | Fixed limits, publish as illustrative | Always |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Committing the Platform token or `.env` | Account/environment takeover, data read access via Platform | `.env` in `.gitignore` and `.dockerignore`, `.env.example`, pre-commit and CI secret scan, rotate on any exposure |
| Passing secrets via image build args or `ENV` | Token in image layers/history | Runtime env only; scan images |
| Demo role switcher mistaken for auth | Reader copies it to production | State loudly in README/ADR; header/cookie is a demo mechanism |
| Enabling Platform `FULL` event/DLQ access | Payload exposure | Keep defaults (`NONE`) |
| Third-party Actions by mutable tag | CI secret theft (Trivy, March 2026) | SHA pinning, least privilege, no secrets in scanner jobs |
| Actuator endpoints exposed in compose | Env/config leakage | Expose only health/info/prometheus; sanitise |
| Fork PRs with secrets | Secret exfiltration | No secrets on fork PRs; Platform steps only on main repo |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| UI refetches after POST | Stale or flickering lists | Pending state plus subscription update; read-side watermark |
| Showing "conflict" as an error in the race demo | Demo seems broken | Distinct outcomes: seat won, waitlisted, course full, retried |
| Event explorer relies on Axon Server UI | Duplicate tag values hidden (UI bug) | Own explorer fed by own API |
| SSE silently dies | UI looks live but is frozen | Heartbeat, reconnect indicator, snapshot on reconnect |
| Role switcher not persisted | Confusing state on reload | Persist in local storage and cookie, show current role prominently |
| No explanation of the boundary | DCB lesson invisible | Show tags, types, markers per command with plain language |

## "Looks Done But Isn't" Checklist

- [ ] **Capacity invariant:** often only tested sequentially. Verify with a concurrent test against real Axon Server DCB (N threads, K seats, exactly K successes).
- [ ] **Tags:** every event class has expected tags and a stored-event assertion. Verify with the reflection test plus one fixture assertion per command.
- [ ] **Unique name:** verify case/whitespace variants, rename-and-reuse, concurrent create vs rename.
- [ ] **Transfer:** verify target full, already subscribed, overlap, and that the source seat is promoted only after commit.
- [ ] **Waiting list:** verify duplicate event delivery, processor reset, barging, ineligible head, capacity increase freeing multiple seats.
- [ ] **Schedule overlap:** back-to-back slots, same-slot different days, schedule change with subscribers.
- [ ] **Projection rebuild:** reset token and truncate read model reproduces identical state; no duplicate side effects.
- [ ] **Metadata:** stored events carry command id/causation, role. Verify via event explorer data, not payload only.
- [ ] **SSE:** subscription gauge returns to baseline after tab close; works through the compose proxy.
- [ ] **Compose cold start:** `docker compose up` on a clean machine works with no Platform token, and states what happens at 12 hours.
- [ ] **License banner:** README documents Axoniq module licence stance and Axon Server dev limits.
- [ ] **Platform token:** repo history, image layers and CI logs contain no token (scan them).
- [ ] **Traces:** one trace id from gateway to command service to processors, logs correlated with trace id.
- [ ] **Docs:** every code snippet comes from compiled source; quick start executed in CI.
- [ ] **Load test:** pass criteria are invariants, not latency; versions recorded.
- [ ] **Upgrade:** a Renovate bump of Axon runs the DCB smoke test and the full invariant suite.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Missing/wrong tags discovered late | HIGH | Events are immutable; use Axon Server re-tagging (supported since 2025.2) or a migration/new context; add reflection test; recreate demo data (acceptable for a demo; document) |
| Wrong boundary (tags only) | MEDIUM | Add event types to criteria; no data change needed; add regression test |
| Non-DCB or 2025.1 context | MEDIUM | 2026.0+ converts contexts to DCB; or recreate context and reseed demo data |
| Committed Platform token | LOW-MEDIUM | Revoke token in Platform first, issue new, remove from history (filter-repo), re-scan, update CI secrets |
| Compromised/unpinned GitHub Action | HIGH | Rotate every secret the workflow could read, pin SHAs, audit workflow runs in the exposure window |
| Projection corrupted by duplicate delivery | LOW | Reset token, truncate read model, replay (needs idempotent projectors) |
| Docs drift | MEDIUM | Introduce snippet extraction and CI checks, then fix backlog |
| Axon minor upgrade breaks API | MEDIUM | Pin back via Renovate rollback; read release notes; fix in dedicated PR |
| Over-engineered structure | HIGH | Collapse modules, move infrastructure to its own directory, add the "DCB in 5 minutes" guide |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1 Axoniq split and licence | P1 | ADR plus README banner; BOM contains only intended families; app starts in compose with only dev permission |
| 2 12-hour Axon Server limit | P1, P7 | Documented procedure; load test records version/limits |
| 3 DCB context setup | P1 | Smoke test: conflicting append rejected on compose and Testcontainers |
| 4 Missing tags | P2, P7 | Reflection test plus fixture assertions on tags |
| 5 Criteria drift | P2, P4, P7 | Cross-feature invariant and property tests |
| 6 EventCriteria limits | P2 | Spike result recorded in ADR; helper tested |
| 7 Broad boundaries | P2, P7 | Boundary budget per command; events-loaded metric; load test |
| 8 Conflict retry | P2, P5, P7 | N-thread test: exactly K successes, no technical errors |
| 9 Waiting-list races | P4 | Duplicate delivery, reset, barging, ineligible-head tests |
| 10 Unique name | P2 | Race and normalisation tests |
| 11 Transfer/schedule | P2, P4 | ADR; transfer/overlap scenario tests |
| 12 AF4 habits | P1, P2 | Cheat sheet; metadata assertion test; lint on forbidden imports |
| 13 AF5 churn | P1, P8 | Pinned catalog; upgrade runs smoke suite |
| 14 Shared contracts/coupling | P1, P7 | ArchUnit slice rules |
| 15 Kotlin/serialisation | P1, P2 | Round-trip test for all events; tag toString test |
| 16 Spring/autoconfig | P1 | `dependencyInsight` check; startup test; token DDL migration test |
| 17 Eventual consistency | P3, P5, P6, P7 | No sleeps (grep gate); watermark helper used |
| 18 Replay/token store | P3 | Rebuild test |
| 19 SSE/subscription leaks | P5, P6, P7 | Gauge-returns-to-zero E2E; proxy E2E |
| 20 Fixture misuse | P2, P7 | Fixture stop/teardown conventions; real-server test set exists |
| 21 Slow/flaky tests | P7, P8 | Suite time budget; flake rate tracked; random IDs |
| 22 Load-test interpretation | P7 | Invariant-based pass criteria; open model |
| 23 ArchUnit/coverage vs Kotlin | P7 | Coverage gate on domain packages; rules ignoring synthetic classes |
| 24 OTel | P9 | End-to-end trace acceptance test |
| 25 Platform token/config | P1, P8, P9 | Secret scans clean; app starts without token; unique app names |
| 26 Compose/hostname | P1, P9 | Cold-start CI job on clean runner |
| 27 Scanner risk/noise | P8 | SHA-pinned Actions; gate only on fixable HIGH+; suppressions with expiry |
| 28 Renovate | P8 | Dashboard shows groups; digest age rule; no automerge on Axon |
| 29 Secret false positives | P8 | Scoped allowlist; full-history scan passes |
| 30 Image size | P8 | Size budget in CI |
| 31 Docs drift | P1 (infra), P10 | Docs build and snippet extraction in CI; quick start smoke job |
| 32 Over-engineering | All; checkpoint end of P2 | Newcomer finds DCB lesson in 5 minutes |
| 33 Boundary explorer hook | P2, P6 | Decision record visible in explorer |

## Research Flags for Roadmap

- **P1 (needs spike, not just research):** licensing behaviour on the pinned versions (12 h limit, restart, Individual plan limits), DCB container setup API, Gradle dependency alignment incl. protobuf, Kotlin tag/serialisation behaviour (value classes, use-site targets, Jackson module).
- **P2 (needs deeper research):** schedule-overlap modeling given `EventCriteria` limits; how to capture the consistency boundary for the explorer; retry behaviour for append conflicts (`RetryingCommandBus`, exception types).
- **P4:** idempotent promotion decision and sequencing policy semantics in AF5 pooled processors.
- **P5/P6:** SSE/WebFlux choice and Axon 5 subscription-query cancellation semantics across services.
- **P9:** trace context across Axon Server messages and Axoniq tracing binding licensing.
- **P8, P10:** standard patterns, unlikely to need research beyond the checklists above.

## Sources

- AxonIQ docs, Axon Framework vs Axoniq Framework comparison: https://docs.axoniq.io/axon-framework-reference/development/framework-comparison/ (HIGH)
- AxonIQ docs, 5.0 to Axoniq Framework migration: https://docs.axoniq.io/axon-framework-reference/5.2/advanced-migration/paths/5.0-to-5.1/ (HIGH)
- AxonIQ docs, release notes 5.x: https://docs.axoniq.io/axon-framework-reference/5.3/release-notes/major-releases/ (HIGH)
- AxonIQ Platform licensing, sources, rotation and expiry, offline licenses: https://docs.axoniq.io/axoniq-platform-reference/licensing/ and /license-sources/ and /rotation-and-expiry/ and /offline-licenses/ (HIGH)
- AxonIQ Platform, Axon Server connector (ports, `axoniq.platform.authentication`): https://docs.axoniq.io/axoniq-platform-reference/connectors/axon-server/ (HIGH)
- Platform framework client (`axoniq.platform.credentials` and options): https://github.com/AxonIQ/platform-framework-client (HIGH)
- Axon Server release notes (DCB timeline, creating DCB contexts, Java 21): https://docs.axoniq.io/axon-server-reference/v2026.1/rn-as-major-releases/ (HIGH)
- Axon Server Docker Hub (non-production, 14 days, hostname, heap, volumes): https://hub.docker.com/r/axoniq/axonserver (HIGH)
- Axon Server Docker/K8s install (compose licence mount, probes): https://docs.axoniq.io/axon-server-reference/v2026.1/axon-server/installation/docker-k8s/ (HIGH)
- Forum, sudden licence change of Axon Server: https://discuss.axoniq.io/t/sudden-license-change-of-axon-server/6539 (MEDIUM)
- AxonIQ pricing (Individual: non-production, instance limit, DCB/Workflows tiers; table garbled): https://www.axoniq.io/pricing (MEDIUM)
- Migration paths: aggregates, event tagging, multi-entity, projectors/processors, token store, test fixtures, serializers, distributed tracing, interceptors: https://docs.axoniq.io/axon-framework-reference/development/migration/ (HIGH)
- Event Store internals (tags, criteria, append condition, consistency marker): https://docs.axoniq.io/axon-framework-reference/5.3/events/event-store-internals/ (HIGH)
- Testing / Advanced testing (AxonTestFixture, `asIntegrationTest`, container): https://docs.axoniq.io/axon-framework-reference/5.3/testing/advanced-testing/ (HIGH)
- Kotlin extension (reflection, nullability, event sourcing): https://docs.axoniq.io/axon-framework-reference/development/extensions/kotlin/reflection/ and /eventsourcing/ (HIGH)
- Spring Boot integration and known issues (protobuf, `@Lob`, sequences): https://docs.axoniq.io/axon-framework-reference/development/spring-boot-integration/ and /known-issues-and-workarounds/ (HIGH)
- Query dispatchers (subscription query, Reactor, scatter-gather): https://docs.axoniq.io/axon-framework-reference/5.3/queries/query-dispatchers/ (HIGH)
- dcb.events (spec FAQ, course subscriptions, unique username examples): https://dcb.events/ , /faq/ , /examples/course-subscriptions/ , /examples/unique-username/ (HIGH for patterns)
- AxonIQ DCB blog: https://www.axoniq.io/blog/dcb-in-af-5 (MEDIUM, says "experimental", predates Server 2025.2)
- AxonIQ university demos: https://github.com/AxonIQ/university-demo-kotlin and Getting Started tutorial: https://docs.axoniq.io/axon-framework-5-getting-started/5.4/ (MEDIUM)
- Forum: first findings on AF5 (`EventCriteria` and collection injection, tickets #3381, #3343): https://discuss.axoniq.io/t/experimenting-with-axon-framework-5-my-first-findings/6507 (MEDIUM)
- Forum: silent metadata gap: https://discuss.axoniq.io/t/the-silent-metadata-gap-command-metadata-doesnt-propagate-to-events-in-axon-5/6640 (MEDIUM)
- Forum: duplicate tag UI bug: https://discuss.axoniq.io/t/not-all-tags-show-up-in-the-axon-server-ui/6617 (MEDIUM)
- Forum: silent empty state in tutorial automation test: https://discuss.axoniq.io/t/getting-started-with-axon-framework-5-send-notification-to-the-administrator-when-all-courses-are-fully-booked/6458 (MEDIUM)
- Forum: closing the books pattern: https://discuss.axoniq.io/t/closing-the-books-pattern-in-axon-5/6598 (MEDIUM)
- Forum: Axon 4.12 to 5.0 experience: https://discuss.axoniq.io/t/our-experience-migrating-from-axon-framework-4-12-to-5-0/6629 (MEDIUM)
- Migration write-up: https://dev.to/petrmacek/migrating-from-axon-framework-4-to-5-what-we-learned-50db (MEDIUM)
- Workflows (preview, licensed for production): https://www.axoniq.io/blog/workflows-now-in-axoniq-framework (MEDIUM)
- Trivy GitHub Action compromise (March 2026): https://www.aquasec.com/blog/trivy-supply-chain-attack-what-you-need-to-know/ and https://github.com/aquasecurity/trivy/security/advisories/GHSA-69fq-xp46-6x23 (HIGH, via multiple outlets)
- Renovate docs (minimum release age, presets): https://docs.renovatebot.com/key-concepts/minimum-release-age/ , https://docs.renovatebot.com/presets-helpers/ ; digest-age report https://codeberg.org/endurain-project/endurain/issues/689 (MEDIUM, user report)
- Secret scanning practitioner guides (gitleaks/TruffleHog): various, LOW-MEDIUM; verify against official repos before relying on them.
- Items NOT researched in depth (general engineering knowledge, LOW-MEDIUM): SSE proxy buffering/heartbeat details, React StrictMode double-mount, ArchUnit/JaCoCo Kotlin quirks, k6/Gatling coordinated omission, image layering. Verify during the relevant phase.

---
*Pitfalls research for: Axon Framework 5 DCB reference implementation (course subscriptions)*
*Researched: 2026-11-20*
