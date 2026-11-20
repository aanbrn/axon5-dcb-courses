# Project Research Summary

**Project:** Axon 5 DCB Courses
**Domain:** Reference implementation of the "students subscribe to courses" domain on Axon Framework 5 using Dynamic Consistency Boundaries (DCB), split into command, query and automation services plus a BFF, with a React UI, Axon Server as the DCB event store, and production-grade quality, CI and docs expectations
**Researched:** 2026-11-20
**Confidence:** MEDIUM-HIGH

## Executive Summary

This is a teaching-grade reference system, not a generic CRUD app. Its core claim is that DCB enforces multi-entity invariants (course capacity, per-student limits, unique course names, atomic transfers, schedule overlap, waiting-list fairness) through one atomic conditional append, without aggregates or sagas. The official AxonIQ `university-demo` already covers the basic subscribe/capacity domain, so this project's value lies in what goes beyond it: the multi-service split, the UI, the event explorer showing the real consistency boundary, the concurrency demo, the waiting list, transfer, schedules, and the test and CI rigor around them.

The recommended approach follows the official AF5 conventions (`write` / `read` / `automation` vertical slices, `AxonTestFixture` given-when-then tests, `@EventSourced` decision models with explicit `@EventCriteriaBuilder` boundaries). Each command gets its own narrow decision model and returns a sealed result for business outcomes; exceptions are reserved for bugs and infrastructure. The waiting list is an automation slice (event handler plus command) whose idempotency lives in the command-side decision, not in a saga. Read models are Postgres projections served through the QueryBus, with live updates via subscription queries pushed to the browser over SSE. The stack is Kotlin 2.4.21, Spring Boot 4.1.3, Axon Framework and Axoniq Framework 5.4.2, Axon Server 2026.1.8, PostgreSQL 18, React 19 with TypeScript 6.0.3, and Docusaurus for docs.

The key risks are licensing and tooling, not the domain logic. The Axon Server connector and DCB support live in the commercial `io.axoniq.framework` namespace (free for development, production needs a licence), and an unlicensed Axon Server reportedly shuts down after 12 hours, which threatens the "docker compose up" promise; this must be spiked in Phase 1 and documented. The most dangerous correctness risks are silent empty state from missing `@EventTag`s, over-broad or missing criteria (including event types), DCB append conflicts that surface as technical errors instead of business rejections, and waiting-list races. Each has a prevention strategy (tag registry plus reflection tests, criteria listing tags and types, bounded retry with a conflict outcome, an idempotent promotion decision that re-validates eligibility). The consistency boundary of a command is not recoverable from Axon Server after the fact, so the decision record must be stamped at append time from the first command slice.

## Key Findings

### Recommended Stack

Backend: Kotlin on Spring Boot with Axon 5.4.2 and `axoniq-spring-boot-starter` (brings the Axon Server connector). Use `axon-kotlin` and `axon-kotlin-test`. BFF is WebFlux with Reactor (subscription queries are `Publisher`-based) and springdoc. Query and automation services use Spring Data JDBC, Flyway and an explicit `JdbcTokenStore`. Frontend: Vite, React 19, TanStack Router and Query, Tailwind 4, shadcn/ui, Orval-generated clients, native `EventSource`, Vitest and Playwright. Infrastructure: GitHub Actions with SHA-pinned actions, Renovate, Gradle lockfiles and verification metadata, Jib, Kover, Spotless/ktlint, Biome, gitleaks CLI, Trivy and Grype via pinned binaries, CycloneDX and cosign. Observability: OTLP to `grafana/otel-lgtm`, with `axon-metrics-micrometer`.

**Core technologies:**
- **Kotlin 2.4.21 (override of Boot's 2.3.21):** latest stable; Axon's Kotlin extension is built against 2.4.20.
- **Spring Boot 4.1.3:** newest 4.1 patch; 4.1.0 is broken for Axon; 4.2.0 is untested by Axon.
- **Axon Framework and Axoniq Framework 5.4.2:** Apache 2.0 core plus the commercial Axon Server connector, the only path to DCB.
- **Axon Server 2026.1.8 (pinned tag and digest, DCB context):** DCB event store and message routing; 2026.1.5 and 2026.1.6 must not be used.
- **PostgreSQL 18:** read models and token store; 19 is not GA.
- **Spring Data JDBC plus `JdbcTokenStore`:** immutable Kotlin data classes, one transaction for projection and token writes, no JPA proxy or `@Lob` issues.
- **React 19 with TypeScript 6.0.3:** TypeScript 7 breaks typescript-eslint and openapi-typescript peer ranges.
- **Docusaurus 3.10.2 with Mermaid:** MkDocs Material is in maintenance mode.
- **JUnit 6, AxonTestFixture, Testcontainers 2.0.5, Awaitility, Konsist:** the test pyramid; Kotest assertions only, not the Kotest runner.

### Expected Features

Table stakes: a complete write side (register student, create course with unique name, change capacity, subscribe and unsubscribe with the per-student limit and full-course rejection, rename), a read side (catalogue and detail, my subscriptions and timetable, student directory), live updates, a demo-only role switcher, human-readable domain errors, and generated typed API clients. Differentiators: event explorer with tags, "boundary of this command" view, concurrency race demo, waiting list auto-enrolment with a Withdraw command, atomic transfer, schedule-slot overlap prevention, scenario seeding and reset, and guided "try this" scenarios.

**Must have (table stakes):**
- RegisterStudent, CreateCourse, ChangeCourseCapacity (rejected if below current subscriptions), SubscribeStudentToCourse, UnsubscribeStudentFromCourse, RenameCourse
- Per-student limit (recommend 3; dcb.events uses 5)
- Unique course name with release on rename, using normalised tag values
- Catalogue and detail read models with seats and waitlist length, plus live updates via SSE
- Role switcher (demo only), human-readable rejections from stable error codes, Playwright smoke test, one-command `docker compose up`
- README with "why DCB" pitch, MADR ADRs, DCB-concepts page, and an Axon 4 aggregate comparison (docs only)

**Should have (competitive):**
- Atomic transfer as one decision over three entities, one `StudentTransferred` event tagged student, from-course and to-course; transfer to a full course rejected (no auto-waitlist); transfer allowed at the limit
- Waiting list: subscribe enqueues when full or when the queue is non-empty (no barging); promotion is a decision that re-validates the candidate; Withdraw is one command over subscription and waitlist state
- Schedule slots on a fixed grid; subscription events carry slot ids; schedule changes only while the course has zero subscribers
- Event explorer, "boundary of this command" view, rejection explainer, race demo with invariant assertion
- Narrow-boundary no-conflict tests (rename does not block subscribe), idempotent automation redelivery test, scenario reset gated to a demo profile

**Defer (v2+):**
- Authentication and authorisation (OIDC, Keycloak)
- Course archive or delete, configurable limits as events, waitlist cap, retention window for released names
- Snapshots, upcasters and event versioning machinery
- Arbitrary-interval schedules (recorded as an ADR alternative)
- Dark mode, i18n, projection-lag dashboard, benchmark page, boundary Venn diagram, multi-context or multi-tenant designs

### Architecture Approach

Four backend services split by CQRS role plus a BFF, communicating only through Axon Server (commands, queries, subscription queries, events). The command service has no database; each command rebuilds its own narrow decision state from events and appends with an `AppendCondition`. The query service owns Postgres projections and the token store, serves queries, and emits subscription-query updates. The automation service runs pooled streaming processors that decide "who" from event-sourced state and dispatch commands; the command service decides "whether". The BFF is the only HTTP surface (REST, OpenAPI, problem+json, SSE fan-out). Gradle modules enforce CQRS: `contracts` split into commands, events, queries and shared kernel, and each service depends only on the contracts it needs.

**Major components:**
1. **contracts (commands, events, queries, shared-kernel):** published language; tag constants and sealed result types; no logic, no Spring.
2. **command-service:** all DCB decisions; one `State` per command slice; explicit `@EventCriteriaBuilder`; sealed results; decision-record decorator stamping criteria and marker on events.
3. **query-service:** Postgres read models (catalogue, roster, student schedule, waiting list, event log for the explorer), query handlers, subscription-query emitters, single-segment event-log processor.
4. **automation-service:** event handler for waiting-list promotion that dispatches a command; per-course sequencing; own token store.
5. **bff-service:** REST plus OpenAPI, command and query gateways, subscription query to SSE, race endpoint, error mapping, role header handling.
6. **frontend:** Vite SPA with generated client; TanStack Query cache updated from SSE snapshot-then-delta; pending state for mutations; nginx with SSE-safe proxy.
7. **Axon Server plus PostgreSQL plus otel-lgtm:** event store and router (DCB context), projection and token store, observability backend.

**Key patterns:**
- One decision model per command slice, never shared; narrowest event types; `CourseRenamed` excluded from subscribe criteria.
- Sealed results for expected outcomes (Subscribed, Waitlisted, Rejected(reason)); exceptions only for bugs and infrastructure.
- Automation slice: event handler plus command, idempotency in the decision.
- Snapshot-then-delta SSE: every (re)connect starts with a full snapshot; cancel subscription on disconnect; heartbeats every 15 to 30 seconds.
- Explicit `TokenStore` bean in the projection database; explicitly named processors; retry on append conflict at the dispatching side with bounded exponential back-off.

### Critical Pitfalls

1. **Axoniq licensing and the 12-hour Axon Server limit:** make the commercial connector an ADR and README disclosure; spike whether an unlicensed compose stack survives beyond 12 hours and how the Platform connection delivers a licence; keep the Platform connection optional so the app starts without it; never use `latest` tags.
2. **Missing or mismatched `@EventTag` gives silently empty state:** use a tag registry with explicit keys (never field-name defaults); reflection test over all event classes; given-when-then tests asserting appended tags; negative test that other courses' events do not leak; verify value-class `toString()` tag values.
3. **Criteria drift and over-broad boundaries:** every criterion lists tags and event types; shared state folds per fact (seat count, subscription set); cross-feature and property-based invariant tests; per-command boundary budget in an ADR.
4. **DCB conflicts surfacing as technical errors:** retry at the dispatching side with a bounded policy and jitter (`AppendEventsTransactionRejectedException` is non-transient by default and may cross the bus as a generic `CommandExecutionException`); re-run the decision; the N-thread test must show exactly K successes and zero infrastructure errors.
5. **Waiting-list races (double enrolment, barging, stale head, duplicate delivery):** Subscribe includes waitlist events in its boundary; promotion picks the head from event-sourced state and re-validates in one decision; ineligible heads skipped with an explicit event; idempotent by construction; tests for processor reset and duplicate delivery; never choose the student from a read model.

Also high-impact: Axon Server default hostname `localhost` breaks compose connectivity (set `AXONIQ_AXONSERVER_HOSTNAME`); the DCB context must be created explicitly (`standalone-dcb`); Axon 4 habits leak into AF5 code; command metadata does not propagate to events by default (register a `CorrelationDataProvider`); initial catch-up of a new processor is `REGULAR`, not `REPLAY`; the March 2026 Trivy action tag compromise means every action and image is pinned by SHA or digest; the Platform token must never be committed.

## Implications for Roadmap

Suggested phase structure follows the dependency order in ARCHITECTURE.md and the MVP ordering in FEATURES.md. Ten phases; the first and write-side phases carry most risk.

### Phase 1: Foundation, Spikes and Licensing
**Rationale:** Build layout, pinned versions and a working DCB Axon Server underpin everything. Licensing, DCB context, Kotlin and Jackson round-trips and the decision-record hook carry the highest unknowns and must be resolved before domain code.
**Delivers:** Gradle multi-project skeleton with `build-logic` convention plugins and version catalog; `contracts` modules with tag registry; compose `infra` profile with Axon Server DCB context and hostname fix; smoke test proving a conflicting conditional append is rejected; spikes on licence behaviour (12-hour limit, restart, Platform delivery), Jackson converter with Kotlin, and the `EventStorageEngine` decision-record decorator; ADRs for the Axoniq split and licence stance, Kotlin conventions, and an AF5 cheat sheet; `.gitignore`, `.env.example`, secret-scan baseline; early CI skeleton with pinned actions.
**Avoids:** Pitfalls 1, 2, 3, 12, 13, 15, 16, 25, 26

### Phase 2: Command Walking Skeleton
**Rationale:** Events and tag taxonomy defined here are consumed downstream. Decision-record stamping must exist from the first append because it cannot be back-filled.
**Delivers:** command-service with RegisterStudent, CreateCourse (unique-name tag), SubscribeStudentToCourse, UnsubscribeStudentFromCourse; sealed result types; per-slice `State` with explicit `@EventCriteriaBuilder`; retry policy with a distinct conflict outcome; decision record in metadata; `CorrelationDataProvider`; given-when-then `AxonTestFixture` tests asserting appended tags; narrow-boundary no-conflict tests; N-thread integration test against real Axon Server proving capacity is never exceeded.
**Avoids:** Pitfalls 4, 5, 7, 8, 10, 20, 33

### Phase 3: Query Walking Skeleton
**Rationale:** Read models and a watermark helper are needed before the UI can be verified; the event-log projection is generic and can start early.
**Delivers:** query-service with `course_catalog` projection, Flyway migrations, explicit `JdbcTokenStore` in the same database, query handlers, subscription-query emission suppressed during replay, `event_log` projection with positions and tags, read-side watermark helper for tests, documented rebuild procedure with replay test.
**Avoids:** Pitfalls 17, 18; deciding on read models

### Phase 4: BFF, SSE and First UI Slice
**Rationale:** Proves the end-to-end path and the real-time story before feature breadth multiplies the surface.
**Delivers:** bff-service with REST, OpenAPI with drift check, command and query gateways, problem+json mapping, SSE endpoint with snapshot-on-connect, heartbeats and cancel-on-disconnect, subscription-gauge test; Vite SPA with generated client, catalogue with live updates, role switcher, Playwright smoke test, `app` compose profile behind nginx with SSE-safe proxy settings.
**Avoids:** Pitfalls 19, 17; SSE connection starvation

### Phase 5: Remaining Write Slices (Rename, Capacity, Limit, Schedule)
**Rationale:** Independent slices reusing the Phase 2 pattern. Schedule slots must precede transfer and waitlist because they share the subscription event shape and overlap rule; `slotIds` must be in the subscription event up front.
**Delivers:** RenameCourse with normalised unique name and release on rename (old and new name tags on one event; name-equals-self handled by owner check); ChangeCourseCapacity with reduce-below-subscriptions rejection; per-student limit in subscribe; SetCourseSchedule on fixed slot grid, only with zero subscribers; overlap prevention in subscribe; timetable view; each slice with read model and UI form; conflict test for schedule change racing first subscribe.
**Avoids:** Pitfalls 6, 10, 11

### Phase 6: Atomic Transfer
**Rationale:** Strongest demonstration of one decision over three entities; depends on stable subscribe and overlap rules.
**Delivers:** TransferStudent decision with criteria over target course, source course and student; single `StudentTransferred` event with three tags; rejection for full target and overlapping schedules; transfer allowed at the limit; shared subscription-facts fold; property-based test on subscription counts; UI transfer flow with disabled-state reasons.
**Avoids:** Pitfalls 5, 11

### Phase 7: Waiting List and Automation
**Rationale:** Highest design risk; depends on unsubscribe, capacity change and transfer events; must follow the final shape of subscribe.
**Delivers:** Subscribe enqueues when full or when waiters exist (no barging); FIFO by event position; WithdrawStudentFromCourse as one command over subscription and waitlist state; PromoteFromWaitlist decision that re-validates eligibility and emits explicit skip events for ineligible heads; automation slice with per-course sequencing and bounded retries; triggers on unsubscribe, transfer-out and capacity increase; periodic sweep safety net; waitlist read model and UI; live "promoted" toast; tests for duplicate delivery, processor reset, ineligible head, multi-seat capacity increase, and no-event cycles.
**Avoids:** Pitfalls 9, 5

### Phase 8: Event Explorer, Concurrency Demo and Scenario Tools
**Rationale:** Depends on decision records (Phase 2), the event-log projection (Phase 3) and the full command set.
**Delivers:** Event explorer UI with tag and type filtering; boundary-of-command view from stored criteria and marker; rejection explainer; race endpoint and UI with outcome classes (seat won, waitlisted, course full, retried); scenario seeding through commands only; dev-only context reset; guided "try this" cards linked to docs and tests.
**Avoids:** Pitfalls 22, 33; relying on the Axon Server UI for the explorer

### Phase 9: Quality Gates, CI/CD and Observability
**Rationale:** Tooling starts in Phase 1 but is hardened and gated once features are stable; Platform and tracing are optional and last.
**Delivers:** full test pyramid (component, integration, E2E against built images, Konsist architecture rules, Gatling load test with invariant assertion outside `check`); Kover gate on domain packages; Spotless, detekt 2.0 alpha or Konsist fallback, Biome; CodeQL, full-history gitleaks, dependency-review, Trivy and Grype with fixable-only gate; SBOM, provenance and cosign; Renovate with grouped Axon, Kotlin and Spring updates, digest rules and `minimumReleaseAge`; OTLP to LGTM with an open-source `SpanFactory` and one end-to-end trace check; optional Platform profile with unique application names and no-token startup.
**Avoids:** Pitfalls 21, 22, 23, 24, 25, 27, 28, 29, 30

### Phase 10: Documentation and Reference Polish
**Rationale:** Docs are written continuously, but the final reference pass, comparison page and "how this was built" narrative come last.
**Delivers:** Docusaurus site with DCB concepts, boundary-per-command table, Event Modeling board and per-slice descriptions, C4 Structurizr views exported to Mermaid, ADR index with status and supersedes, Axon 4 versus DCB comparison (labelled illustrative or compile-checked), "when not to use DCB" page, failure-mode page, README with quick start executed in CI and an honest licence banner, "how this was built" curated from `.planning/`.
**Avoids:** Pitfalls 31, 32

### Phase Ordering Rationale

- Phase 1 first: licensing, DCB context and the decision-record hook change what everything else looks like.
- Phases 2 and 3 establish event shapes and tags every slice reuses; the walking skeleton proves the core invariant early.
- Phase 4 proves the real-time path before breadth.
- Phase 5 (schedule) precedes Phases 6 and 7 because they share the subscription event shape; adding `slotIds` late forces fixture rework.
- Phase 7 depends on unsubscribe, capacity change and transfer, and changes the subscribe decision's final shape.
- Phase 8 needs decision records, the event log and the full command set.
- Phase 9 hardens everything; some tooling starts in Phase 1.
- Phase 10 is finalised last but written continuously.

### Research Flags

Needs deeper research during planning (`/gsd-plan-phase --research-phase <N>`):
- **Phase 1:** licence behaviour on pinned Axon Server (12-hour limit versus 14-day text, restart, Platform delivery); DCB container setup API; protobuf and Gradle alignment; Kotlin tag and Jackson round-trip.
- **Phase 2:** decision-record decorator on `EventStorageEngine` (`@Internal`); retry filter after the Axon Server hop (`AsyncRetryScheduler`, `FilteringRetryPolicy`); availability of `RetryingCommandBus` in the Spring starter; tag value length and charset.
- **Phase 5:** schedule-overlap modelling under `EventCriteria` limits (no AND, no variable tag sets).
- **Phase 6:** three-entity criteria in `@InjectEntity` (composite ids, `either` across tag sets).
- **Phase 7:** criteria depending on a first-pass read (queue head, eligibility); `@SequencingPolicy` semantics; REGULAR versus REPLAY on catch-up.
- **Phase 8:** reading the event store with tags from the query service; Axon Server context reset.
- **Phase 9:** trace context across Axon Server messages; open versus commercial tracing binding.

Standard patterns (skip research-phase):
- **Phase 4:** standard WebFlux, springdoc and React patterns; SSE details verifiable during execution.
- **Phase 8 UI:** standard tables and filters once the read model exists.
- **Phase 10:** Docusaurus, Structurizr export and ADR templates are established.
- **Phase 9 CI hardening:** SHA pinning, Renovate grouping and scanner gating are established practice; confirm versions at first run.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM-HIGH | Versions verified against Maven Central, npm and Gradle plugin portal on 2026-11-20. Three LOW items: Axon Server licence in plain compose, Axon span export, Boot 4.1 with Axon compiled against Spring 6.2. |
| Features | MEDIUM-HIGH | dcb.events and AxonIQ university demo facts are HIGH. Extension rules (waiting list, transfer, schedule, unique name) are design recommendations, MEDIUM. Quality norms LOW-MEDIUM. |
| Architecture | MEDIUM-HIGH | AF5 modelling and Spring integration from framework source and official examples (HIGH). Multi-service recommendations (decision-record stamping, subscription-query scale-out, licence behaviour) are MEDIUM or LOW, flagged as spikes. |
| Pitfalls | MEDIUM-HIGH | Licensing, module split, migration and tracing from official AxonIQ docs (HIGH). DCB modelling and Kotlin specifics MEDIUM. VERIFY items need spikes. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **Licence behaviour:** Platform reference says 12-hour shutdown; Docker Hub says 14-day non-production limit. Restart effect unknown. Spike in Phase 1 for more than 12 hours; decide quick-start and CI approach by ADR.
- **Conflict retry in AF5:** no AF5-specific retry docs found; exception is non-transient and may be wrapped after the hop. Spike in Phase 2 with N-thread test.
- **Decision-record capture:** Axon does not expose past criteria or marker. Decorator relies on an `@Internal` SPI; fallback is per-slice explicit criteria stamping plus a Konsist rule. Phase 1 spike; ADR.
- **First-pass-dependent criteria:** waitlist head and variable-tag schedule checks depend on `EventCriteria` expressiveness. Spike in Phases 5 and 7; fallback is passing the candidate in the command and re-validating in the decision.
- **Tag value constraints:** length and charset for normalised names unknown. Test in Phase 2 with long and Unicode names.
- **Token store:** STACK says `JdbcTokenStore`; ARCHITECTURE says JDBC or JPA; PITFALLS says "persistent JPA token store". Resolve as JDBC (no JPA in stack); ADR.
- **Tracing path:** STACK prefers a custom open `SpanFactory`; PITFALLS recommends the Axoniq Micrometer binding. Resolve in Phase 9; prefer the open path.
- **Spring Boot patch:** STACK says 4.1.3; ARCHITECTURE cites 4.1.2. Use 4.1.3; confirm in Phase 1.
- **Per-student limit:** FEATURES recommends 3 (official sample); dcb.events uses 5. Record in ADR; configurable in code, not an event.
- **Rejection style:** FEATURES suggests typed errors; ARCHITECTURE and PITFALLS recommend sealed results. Resolve as sealed results; keep FEATURES' error codes inside results.
- **Waitlist promotion shape:** ARCHITECTURE (automation reads event-sourced queue, dispatches head; command re-validates) and FEATURES (re-validation rules) agree once combined; use both.
- **Subscription-query scale-out:** more than one query-service instance can silently miss updates. Run one instance in v1; document.
- **Event naming and versioning:** explicit `@Event(name, version)` and additive-only changes need a convention in Phase 1.

## Sources

### Primary (HIGH confidence)
- Maven Central and POM metadata for `org.axonframework:*`, `io.axoniq.framework:*`, `io.axoniq.platform:*`, Spring Boot, Kotlin, Testcontainers, springdoc (checked 2026-11-20)
- Axon Framework reference 5.4 and release notes (DCB mechanics, `EventCriteria`, consistency marker, `@EventCriteriaBuilder`, subscription queries, testing, Kotlin extension, migration paths)
- AxonFramework examples: `university-demo-kotlin`, `university-java-springboot-4`, `docker-compose.yaml`; framework source for `DefaultEventStoreTransaction`, `AppendEventsTransactionRejectedException`, `JdbcAutoConfiguration`
- AxonIQ Getting Started (AF5) and university-demo
- Axoniq Platform licensing reference; Axoniq Framework repository (licence-gated modules, `AxonServerContainer`)
- Axon Server reference 2026.1 release notes, Docker Hub `axoniq/axonserver`, `dcb.proto` and `DcbEventChannel`
- dcb.events course subscriptions, unique username, bike rental and aggregates topic
- Trivy GitHub Action compromise (GHSA-69fq-xp46-6x23, CVE-2026-33634)

### Secondary (MEDIUM confidence)
- AxonIQ forum threads: Axon Server licence change; silent metadata gap; silent empty state in tutorial test; closing-the-books pattern; duplicate tag UI bug; `EventCriteria` limits (#3343, #3381); AF 4.12 to 5.0 migration
- Petr Macek migration write-up (dev.to)
- Sliceworkz "Aggregates and DCB"
- AxonIQ DCB blog and concept page (predates Axon Server 2025.2 support)
- AxonIQ pricing page (Individual plan limits; table partly garbled)
- Structurizr, LikeC4, Mermaid event modeling and Docusaurus release data
- Renovate minimum release age docs and digest-age user report

### Tertiary (LOW confidence)
- Axon Server unlicensed behaviour beyond 12 hours (conflicting sources; spike)
- Decision-record stamping via `EventStorageEngine` decorator (inferred from `@Internal` SPI; spike)
- Subscription-query scale-out behaviour (inferred; spike)
- Reference-project quality norms (general practice)
- SSE proxy buffering, React StrictMode double-mount, k6 coordinated omission (general knowledge; verify in relevant phase)

---
*Research completed: 2026-11-20*
*Ready for roadmap: yes*
