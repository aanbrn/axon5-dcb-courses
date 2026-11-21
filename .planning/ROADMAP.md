# Roadmap: Axon 5 DCB Courses

## Overview

The journey starts with a repository that already meets reference quality before any domain code exists: build, formatting, CI and governance. Next, DCB on Axon Server is proven and the riskiest unknowns are resolved through spikes. A walking skeleton follows. The command service enforces capacity and the per-student limit under real concurrency, the query service keeps live read models, and the BFF serves a first browser view over SSE. After that, the whole system runs in containers with one `docker compose up`. A UI foundation then turns the skeleton into a polished, accessible application. Feature phases add course administration and schedules, atomic transfer and the automated waiting list, each as a complete vertical slice with its own UI. The DCB teaching UI makes the consistency boundary visible. Hardening adds observability, resilience, load testing and signed releases. A final pass completes the reference documentation, the public presentation of the repository and the UI polish.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Repository, Build & CI Foundation** - Reference-quality build, formatting, CI and governance before any runtime code
- [ ] **Phase 2: Runtime Infrastructure & DCB Spikes** - Axon Server DCB proven, test infrastructure ready, high-risk unknowns resolved in ADRs
- [ ] **Phase 3: Command Walking Skeleton** - Core invariants (unique name, capacity, per-student limit) enforced by DCB under real concurrency
- [ ] **Phase 4: Read Side & Live Queries** - Query service with idempotent PostgreSQL projections, QueryBus queries and subscription queries
- [ ] **Phase 5: BFF & First Live UI** - End-to-end path from the browser through REST/OpenAPI to SSE live updates
- [ ] **Phase 6: Containerised System** - Whole system from buildpack images with one `docker compose up`, healthy and restart-safe
- [ ] **Phase 7: UI Foundation** - Design system, app shell, role switcher, FSD, Storybook, accessibility and resilient real-time UI
- [ ] **Phase 8: Course Administration & Schedules** - Rename, capacity change, weekly schedules and overlap prevention, end to end
- [ ] **Phase 9: Atomic Transfer** - One decision spanning a student and two courses
- [ ] **Phase 10: Waiting List & Automation** - Fair FIFO waiting list with idempotent automated promotion
- [ ] **Phase 11: DCB Teaching UI** - Event explorer, consistency-boundary view, rejection explainer, race demo and guided scenarios
- [ ] **Phase 12: Observability, Resilience & Signed Releases** - OpenTelemetry and Grafana, failure injection, load test, signed and attested releases
- [ ] **Phase 13: Reference Documentation & Final Polish** - Docs site content, README, GitHub presentation and final UI review

## Phase Details

### Phase 1: Repository, Build & CI Foundation
**Goal**: A contributor can clone the repository, install pinned tools, and build and check the whole multi-project build (no domain code yet) with one command. Every commit and pull request is automatically formatted, analysed, licence-checked and governed.
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: INFRA-01, INFRA-09, INFRA-10, INFRA-11, INFRA-12, INFRA-13, INFRA-14, INFRA-16, TEST-09, TEST-10, TEST-13, CICD-01, CICD-03, CICD-04, CICD-06, CICD-08, TOOL-01, TOOL-02, GH-06, GH-07, GH-08, GH-11, AIDEV-05, AIDEV-06, AIDEV-08
**Success Criteria** (what must be TRUE):
  1. On a fresh clone, `mise install` followed by `./gradlew build` succeeds with the configuration cache enabled, using `build-logic` convention plugins (covered by TestKit tests), the `:platform` module and the version catalog, and every tool is listed in `docs/tooling.md`. Any version literal in a build script, or drift between `.java-version` and the Gradle daemon JVM criteria, fails the build.
  2. One command formats every artifact type (Kotlin including KDoc, Gradle KTS, JSON, YAML, Markdown), and the pre-commit hook formats staged files. CI fails on formatting or `.editorconfig` violations, detekt findings, broken KDoc references, a missing SPDX header (`reuse lint`) or a working tree that is not normalised.
  3. Every push and pull request runs a GitHub Actions workflow with SHA-pinned actions. It builds, tests and analyses the code, lints workflows and scripts (actionlint, zizmor, shellcheck) and scans the full history for leaked secrets. Renovate proposes grouped updates with a minimum release age for every version source (catalog, `.java-version`, `mise.toml`, Actions).
  4. `main` changes only through squash-merged pull requests from `gsd/phase-*` branches that pass the required checks. The ruleset enforces linear history and blocks force-push, and head branches are deleted after merge. The PR template mirrors the `/gsd-ship` body, and labels are synced from `labels.yml`. Secret scanning with push protection, Dependabot alerts, private vulnerability reporting, CodeQL and OpenSSF Scorecard are active.
  5. A pull request fails CI if it changes `.planning/config.json` against the declared policy or touches governance files without the `governance-approved` label. The agent must ask for confirmation before it edits governance files or runs GSD `config-set`.
**Plans:** 17/17 plans executed

Plans:
- [x] 01-15-PLAN.md
- [x] 01-16-PLAN.md
- [x] 01-17-PLAN.md

**Wave 1**
- [x] 01-01-PLAN.md — Build skeleton tracer: wrapper, catalog, `build-logic` settings and module conventions, `:platform` with every stack BOM and the explicit Kotlin override, sample module, `.gitignore` (wave 1)

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 01-02-PLAN.md — Quality conventions and TestKit: ktfmt + SPDX, detekt, Dokka, daemon JVM criteria, `functionalTest` suite (wave 2)
- [x] 01-03-PLAN.md — Pinned CLI tools: package-legitimacy gate, `mise.toml`/`mise.lock` (incl. shfmt, taplo), `.nvmrc`, `docs/tooling.md` (wave 2)

**Wave 3** *(blocked on Wave 2 completion)*
- [x] 01-04-PLAN.md — Prettier formatting (JSON, JSON5, YAML, Markdown) with Node verified against `.nvmrc`, and `.editorconfig` (wave 3)

**Wave 4** *(blocked on Wave 3 completion)*
- [x] 01-05-PLAN.md — Remaining formatters: TOML (taplo), shell (shfmt + SPDX header) and generic text formats (wave 4)

**Wave 5** *(blocked on Wave 4 completion)*
- [x] 01-06-PLAN.md — REUSE compliance, minimal README, `.gitattributes` and tree normalisation (wave 5)
- [x] 01-07-PLAN.md — Governance checks in build-logic: config policy, build-script lint, daemon JVM (wave 5)
- [x] 01-09-PLAN.md — PR governance gate: `governance-approved` label, Renovate exemption, head-branch naming (wave 5)

**Wave 6** *(blocked on Wave 5 completion)*
- [x] 01-08-PLAN.md — lefthook pre-commit hook and gradle-profiler scenarios with baseline (wave 6)
- [x] 01-10-PLAN.md — Labels as code, the `/gsd-ship` PR template with `checkPrTemplate`, and `checkToolingInventory` (wave 6)

**Wave 7** *(blocked on Wave 6 completion)*
- [x] 01-11-PLAN.md — Renovate configuration: every version source, cooldown, grouping, narrow automerge (wave 7)

**Wave 8** *(blocked on Wave 7 completion)*
- [x] 01-12-PLAN.md — CI workflows: `ci.yml` (`ci-success`), CodeQL (actions + Kotlin spike), Scorecard, ADR-lite notes (wave 8)

**Wave 9** *(blocked on Wave 8 completion)*
- [x] 01-13-PLAN.md — GitHub settings as code, apply/drift scripts, Claude Code ask rules (wave 9)

**Wave 10** *(blocked on Wave 9 completion)*
- [x] 01-14-PLAN.md — Approval-gated application of GitHub settings, label and secrets by the author; read-only verification (wave 10)

**Cross-cutting constraints:** <!-- gsd:generated -->
- Edge TEST-09 empty: running `./gradlew spotlessApply` on an already formatted tree modifies no file (`git status --porcelain` stays empty)

**Research**: Light. Topics: ktfmt KDoc formatting behaviour, isolated-projects compatibility, Spotless and REUSE header integration.
**Approval gates**: GitHub repository settings (GH-06, GH-07) are applied via `gh` only after explicit author approval at execution time.

### Phase 2: Runtime Infrastructure & DCB Spikes
**Goal**: DCB on a pinned Axon Server works locally and in CI, the JVM test infrastructure is in place, and every high-risk unknown is resolved and recorded before domain code is written.
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: INFRA-02, INFRA-03, INFRA-04, INFRA-05, INFRA-06, IDEM-04, TEST-11, TEST-12, TEST-14, CICD-09, UPS-01, UPS-02, UPS-03, UPS-04, DOC-03, GH-12, TOOL-03, TOOL-04, AIDEV-01, AIDEV-03, AIDEV-04, AIDEV-07
**Success Criteria** (what must be TRUE):
  1. One docker compose command starts the pinned Axon Server and PostgreSQL. Axon Server gets its DCB context created automatically and the correct hostname, and image references come only from the version catalog. `doctor` checks the required tools and versions, the Docker engine (colima or Docker Desktop), Testcontainers connectivity and the pinned GSD installation against its manifest, and explains how to fix each problem.
  2. An automated integration test against Axon Server in Testcontainers shows that a conditional append conflicting with a concurrent append is rejected, while a non-overlapping append succeeds. Unit, component and integration tests run as separate Gradle test suites that share helpers through test fixtures and use the standard toolchain (JUnit 6, Kotest assertions and property testing, Testcontainers, Awaitility).
  3. The contracts modules (commands, events, queries, a shared kernel with the tag registry and sealed result types) contain no Spring or framework logic. Kotlin events with TypeID value-class IDs and tags round-trip through serialization unchanged, and malformed or wrong-prefix TypeIDs are rejected.
  4. MADR ADRs, created from in-repository templates, record the spike outcomes: Axoniq licence behaviour (also reflected in the quick-start notes), the decision-record capture approach, Kotlin serialization and TypeID conventions, and buildpack constraints (pnpm support, image environment defaults). CI checks dependency licences against an allowlist, and NOTICE justifies the Axoniq modules.
  5. Every upstream defect found so far has a `docs/upstream/` record, a CI check keeps workaround markers and records consistent, and dependency update PRs prompt a check of the removal criteria. Vetted Claude Code skills and MCP servers (Context7, Playwright) are installed from pinned sources only after approval and are listed in the tooling inventory. Only `.claude/settings.json` and `.claude/CLAUDE.md` are tracked.
**Plans**: TBD
**Research**: Yes. Topics: licence behaviour of the pinned Axon Server (12-hour vs 14-day limit, restart, licence delivery via AxonIQ Platform), DCB container setup, protobuf and Gradle alignment, the Kotlin tag and Jackson round-trip, the decision-record decorator spike, TypeID library maturity (Kotlin and TypeScript), Paketo pnpm support.
**Approval gates**: Third-party skills and MCP servers (AIDEV-01, AIDEV-04) and upstream issue publication (UPS-03) require explicit author approval.

### Phase 3: Command Walking Skeleton
**Goal**: The command service enforces the core multi-entity invariants (unique course name, course capacity, per-student course limit) through DCB decisions, proven under real concurrency. Decision records and idempotency are built in from the very first event.
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: COURSE-01, COURSE-02, STUD-01, STUD-02, SUBS-01, SUBS-02, SUBS-03, SUBS-05, SUBS-06, SUBS-07, IDEM-01, IDEM-02, DEMO-01, TEST-01, TEST-02, TEST-06, TEST-08, TEST-15, DOC-05
**Success Criteria** (what must be TRUE):
  1. Commands sent to the command service register students, create courses with client-generated TypeIDs, and subscribe and unsubscribe students. Duplicate (normalised) course names, full courses, duplicate subscriptions, the 3-course limit and unknown students or courses are rejected as typed results with stable error codes, never as technical errors.
  2. In a concurrent integration test against real Axon Server, many students race for the last seats, and the run ends with exactly `capacity` subscriptions and zero infrastructure errors. Append conflicts are retried transparently within a bounded policy.
  3. Re-sending any command with the same idempotency key returns the original outcome and appends no new events.
  4. Every appended event carries the expected tags and its decision record (criteria, consistency marker, outcome, retries). Given-when-then tests assert tags and boundaries, including narrow-boundary no-conflict cases, and a reflection test checks the tags of every event class.
  5. Tests read as a business specification (prose names, business-rule tags), and architecture tests enforce module and slice boundaries. The aggregated Kover report meets the high threshold for decision logic, and every slice has an Event Modeling description.
**Plans**: TBD
**Research**: Yes. Topics: decision-record capture on `EventStorageEngine` (an `@Internal` SPI; fallback is explicit per-slice stamping), conflict retry after the Axon Server hop (`AsyncRetryScheduler`, `FilteringRetryPolicy`, `RetryingCommandBus` in the Spring starter), tag value length and charset.

### Phase 4: Read Side & Live Queries
**Goal**: A separate query service keeps PostgreSQL read models current from the event stream and serves them through the QueryBus with live updates. The read models stay correct under redelivery, failures and replay.
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: READ-01, READ-02, READ-03, READ-04, READ-05, READ-06, IDEM-03, RES-02, TEST-04
**Success Criteria** (what must be TRUE):
  1. After commands are handled, QueryBus queries answered by the query service return the course catalogue (name, capacity, free seats), course detail with subscribed students, a student's subscriptions and the student directory. The command side never reads these models.
  2. A subscription query delivers the current result immediately, then an update shortly after every relevant event.
  3. Duplicate event delivery or a processor reset leaves every read model unchanged, because the token and the projection are committed together with upsert semantics.
  4. The documented rebuild procedure replays a projection from scratch to an identical state without emitting live updates during the replay.
  5. A failing projection is retried with backoff according to the error policy recorded in an ADR. It is never silently skipped, and the failure is visible in metrics. Integration tests run the query service against real Axon Server and PostgreSQL via Testcontainers.
**Plans**: TBD

### Phase 5: BFF & First Live UI
**Goal**: A user in the browser triggers a command and watches the result arrive live. The path UI → BFF → command service → events → projection → SSE → UI is proven end to end before feature breadth grows.
**Mode:** mvp
**Depends on**: Phase 4
**Requirements**: API-01, API-02, API-03, API-04, INFRA-08, TEST-18, RES-03
**Success Criteria** (what must be TRUE):
  1. Every operation built so far is available through the BFF REST API, described by an OpenAPI specification whose drift fails CI. Each operation accepts an `Idempotency-Key` and returns rejections as problem+json with stable error codes. An unavailable or slow command service yields `503` with `Retry-After`.
  2. A user opens the course catalogue in the browser, creates a course or subscribes a student from another tab, and sees the change appear live without refresh (SSE snapshot on connect, deltas afterwards, heartbeats, cleanup on disconnect).
  3. `./gradlew build` also installs, lints, tests and builds the frontend and the documentation-site skeleton with pinned Node.js and pnpm. It regenerates the typed API client whenever the BFF OpenAPI specification changes, `pnpm dev` still works for local UI work, and a Playwright smoke test passes.
  4. Architecture tests prove that services communicate only through Axon Server (no service-to-service HTTP) and that layer mapping uses explicit `toCommand`/`toDto`/`toView` extension functions at the layer boundaries.
**Plans**: TBD
**UI hint**: yes

### Phase 6: Containerised System
**Goal**: The whole system (services, UI and infrastructure) runs from buildpack-built images with one `docker compose up`. It becomes healthy in the right order, recovers from restarts and is verified end to end against those same images.
**Mode:** mvp
**Depends on**: Phase 5
**Requirements**: INFRA-07, INFRA-15, INFRA-17, CICD-02, CICD-05, CICD-10, RES-01, RES-04, RES-07, TEST-05
**Success Criteria** (what must be TRUE):
  1. `docker compose up`, with only an untracked `.env` created from the committed `.env.example`, starts the infrastructure, all services and the UI. The UI works through nginx, including SSE and SPA routing.
  2. Gradle builds the images for every service and the UI with Paketo buildpacks (no Dockerfiles). The images carry fully qualified `ghcr.io/<owner>/<project>-<module>:<version>` names and standard OCI labels, and document each environment variable and its default, visible via `docker image inspect`. `.env.example` and the configuration reference are generated from per-service descriptors, and CI rejects stale copies.
  3. Each container reports health without a shell on an unpublished management port, and compose starts containers in dependency order. Services reconnect automatically after an Axon Server restart. Stopping a service lets in-flight commands finish and releases processor tokens.
  4. Playwright end-to-end tests run in CI against the composed system built from release images. Dependency and image scans fail the build on fixable high-severity vulnerabilities.
  5. Shared IntelliJ run configurations start each service, the compose stack and every test suite, and VS Code suggests the recommended extensions when the repository is opened.
**Plans**: TBD
**Research**: Light. Topics: Paketo Web Servers buildpack nginx config for SSE, Health Checker buildpack, whether buildpack environment defaults can appear in `Config.Env` (outcome of the Phase 2 spike).

### Phase 7: UI Foundation
**Goal**: The frontend becomes a polished, accessible, real-time application built on a deliberate design system. It has a demo role switcher and complete screens for every operation delivered so far.
**Mode:** mvp
**Depends on**: Phase 5 (frontend, SSE), Phase 6 (composed system for E2E and accessibility checks)
**Requirements**: UI-02, UI-03, UI-06, UI-07, UI-08, UI-09, RES-06, TEST-16, TEST-17, GH-04
**Success Criteria** (what must be TRUE):
  1. A user can switch between admin and any registered student in the app shell. As admin, they register students, create courses, browse the student directory and see course rosters. As a student, they subscribe, unsubscribe and see their own subscriptions. The shadcn/ui + Tailwind design system gives every screen consistent loading, empty, error and offline states.
  2. Every screen updates in real time without refresh. After a network drop, the UI shows an offline state, then reconnects and resynchronises automatically, and a retried command reuses its idempotency key.
  3. Every flow is keyboard-operable with visible focus, and automated axe checks pass in component tests and Playwright (WCAG 2.2 AA).
  4. Storybook catalogues shared, entity and widget components in all their states, with interaction and accessibility checks. CI publishes it to GitHub Pages together with the documentation-site skeleton.
  5. Steiger enforces FSD layer and import boundaries in CI, and frontend coverage meets per-layer thresholds. Backend and frontend coverage appear as CI artifacts, in the job summary, as a PR comment and as a README badge.
**Plans**: TBD
**UI hint**: yes

### Phase 8: Course Administration & Schedules
**Goal**: Admins fully manage courses (rename, capacity, weekly schedule), and students can no longer take courses with overlapping schedules. Each rule is delivered as a complete vertical slice, from decision to UI.
**Mode:** mvp
**Depends on**: Phase 7
**Requirements**: COURSE-03, COURSE-04, COURSE-05, COURSE-06, SUBS-04, AIDEV-02
**Success Criteria** (what must be TRUE):
  1. An admin can rename a course and immediately reuse its old name for another course, and renaming a course to its own name (including a case-only change) succeeds. A rename never conflicts with concurrent subscriptions.
  2. An admin can change a course's capacity. A change to the same value, or below the current number of subscribed students, is rejected with an explanation, and free seats update live.
  3. An admin can assign a schedule on the fixed weekly slot grid while the course has no subscribers. Changing it after the first subscription is rejected, including when the change races with that first subscription.
  4. A student cannot subscribe to a course whose slots overlap one of their courses and is told which course conflicts. The catalogue shows schedules, and the student sees a weekly timetable.
  5. Project skills in a repository-local Claude Code plugin encode the conventions for vertical slices, projections, ADRs and upstream issues, and this phase's slices are built with them.
**Plans**: TBD
**UI hint**: yes
**Research**: Yes. Topic: schedule-overlap modelling within `EventCriteria` limits (no AND, no variable tag sets). `slotIds` must be part of the subscription event before transfer and the waiting list.

### Phase 9: Atomic Transfer
**Goal**: A student moves from one course to another in a single DCB decision spanning the student and both courses. This is the strongest demonstration of a rule that aggregates can only approximate with a compensating saga.
**Mode:** mvp
**Depends on**: Phase 8
**Requirements**: XFER-01, XFER-02, XFER-03, XFER-04
**Success Criteria** (what must be TRUE):
  1. A student can transfer from a source course to a target course in one action. Afterwards they are subscribed to the target and not to the source, the change is recorded as one event tagged with the student and both courses, and both courses' seat counts update live.
  2. A transfer is rejected with a clear reason if the target is full (no automatic waitlisting), the student is not subscribed to the source, the student is already subscribed to the target, or the target's schedule overlaps the student's other courses (source excluded).
  3. A student already at the 3-course limit can still transfer.
  4. Under concurrent transfers and subscriptions, property-based and concurrency tests show that capacities and per-student limits are never violated. The UI explains why a transfer is unavailable before it is attempted.
**Plans**: TBD
**UI hint**: yes
**Research**: Yes. Topic: three-entity criteria in `@InjectEntity` (composite identifiers, `either` across tag sets).

### Phase 10: Waiting List & Automation
**Goal**: Full courses queue students fairly, and an idempotent automation service fills freed seats automatically, with no sagas, no double enrolment and no barging.
**Mode:** mvp
**Depends on**: Phase 9 (transfer-out and capacity-increase triggers, final subscribe shape)
**Requirements**: WAIT-01, WAIT-02, WAIT-03, WAIT-04, WAIT-05, WAIT-06, TEST-03, DOC-12
**Success Criteria** (what must be TRUE):
  1. Subscribing to a full course, or to any course whose waiting list is not empty, places the student on the waiting list in FIFO order instead of rejecting. Students see their waiting-list positions and the catalogue shows waiting-list length, both live.
  2. When a seat is freed (unsubscribe, transfer-out, capacity increase), the next eligible waiting student is subscribed automatically within seconds. An ineligible head (limit reached, schedule overlap) is removed with an explicit reason and promotion moves on. A capacity increase of several seats promotes several students.
  3. A student can withdraw from a course, whether subscribed or waitlisted, with a single command that never races with concurrent promotion.
  4. Duplicate event delivery or a processor reset of the automation service never causes double enrolment or exceeds capacity. Every service (command, query, automation and BFF) has its own component test suite.
  5. A documentation page explains why atomic transfer replaces a compensating saga, why an automation slice is enough for the waiting list, and when a long-running process would need Axoniq Workflows.
**Plans**: TBD
**UI hint**: yes
**Research**: Yes. Topics: criteria that depend on a first-pass read (queue head, eligibility), `@SequencingPolicy` semantics, REGULAR vs REPLAY on processor catch-up.

### Phase 11: DCB Teaching UI
**Goal**: Users see DCB at work. They browse tagged events, open the consistency boundary behind any decision, understand rejections and retries, run a live race for the last seat and follow guided scenarios.
**Mode:** mvp
**Depends on**: Phase 10 (full command set); decision records from Phase 3
**Requirements**: DEMO-02, DEMO-03, DEMO-04, DEMO-05, DEMO-06, UI-04, UI-05, DOC-06, DOC-10
**Success Criteria** (what must be TRUE):
  1. A user can browse the event log with tags and filter it by tag and event type.
  2. From any command outcome, a user can open its consistency boundary: the criteria, the consistency marker and the events that formed the decision. Rejections state which rule fired and which events caused it, and commands that hit a concurrency conflict show that they were retried.
  3. A user can start a race of many students for the last seat and see each participant's outcome (seat won, waitlisted, rejected, retried) and that the capacity invariant held.
  4. In the demo profile, a user can seed scenarios (through commands only) and reset the demo context. Guided "try this" cards link each scenario to its documentation and to the tests that prove the rule.
  5. The documentation site shows, for every command, which tags and event types form its consistency boundary and why. It also publishes a business rule → test catalogue generated from test tags.
**Plans**: TBD
**UI hint**: yes
**Research**: Yes. Topics: reading the event store with tags for the explorer read path, resetting an Axon Server DCB context.

### Phase 12: Observability, Resilience & Signed Releases
**Goal**: The system is observable end to end and proven resilient under injected failures and load, and every release ships versioned, signed and attested images.
**Mode:** mvp
**Depends on**: Phase 11 (complete feature set); composed system from Phase 6
**Requirements**: OBS-01, OBS-02, OBS-03, RES-05, TEST-07, CICD-07, GH-05
**Success Criteria** (what must be TRUE):
  1. Grafana, started by docker compose, shows metrics, traces and logs from all services, and a single request can be followed as one trace from the BFF through command handling to the projections.
  2. With an AxonIQ Platform token in `.env`, the services appear in AxonIQ Platform. Without the token, the system starts and works normally.
  3. Automated resilience tests restart Axon Server, take PostgreSQL down and add network latency via Toxiproxy, and the system recovers with all invariants intact.
  4. A load test of concurrent subscriptions finishes with the capacity and per-student invariants verified.
  5. Merging a release-please PR creates the version tag, changelog and GitHub Release, and publishes images to GHCR with SBOMs, signatures and provenance attestations that cosign can verify. Commit messages and PR titles that do not follow Conventional Commits fail the checks.
**Plans**: TBD
**Research**: Yes. Topics: trace context propagation across Axon Server messages, open vs commercial tracing binding (prefer the open `SpanFactory` path).

### Phase 13: Reference Documentation & Final Polish
**Goal**: A visitor to the repository or the documentation site understands why DCB matters and can run the system in minutes. They can learn the concepts, compare them with aggregates, plan a migration from Axon 4 and see how the project was built. Everything can be done in a reviewed, polished UI.
**Mode:** mvp
**Depends on**: Phase 12
**Requirements**: DOC-01, DOC-02, DOC-04, DOC-07, DOC-08, DOC-09, DOC-11, GH-01, GH-02, GH-03, GH-09, GH-10, GH-13, UI-01
**Success Criteria** (what must be TRUE):
  1. The README presents the "why DCB" pitch, an architecture overview, a UI screenshot or animation, badges (CI, coverage, licence, Scorecard, Axon/Kotlin/Spring Boot versions), an honest licensing note and a quick start that CI runs successfully.
  2. The documentation site explains DCB concepts as applied in this project. It includes C4 diagrams as code, a per-scenario comparison with Axon 4 aggregates, an Axon 4 → Axon 5/DCB migration guide, a "when not to use DCB" page and a "how this was built" section curated from `.planning/`.
  3. Repository metadata (description, topics, homepage, social preview) is managed as code. Community health files, issue forms that route questions to Discussions and vulnerabilities to private reporting, Discussions categories and `CITATION.cff` are in place.
  4. Every business operation can be performed from the UI through the generated typed client, and the final UI review passes for all screens.
**Plans**: TBD
**UI hint**: yes
**Approval gates**: Repository metadata and Discussions settings (GH-01, GH-10) are applied via `gh` only after explicit author approval.

## Cross-Cutting Conventions

Some requirements are delivered and verified in one phase but set a standing rule that every later phase must follow. Plan-phase should carry the relevant ones into each phase's must-haves.

| Requirement(s) | Established in | Standing obligation for later phases |
|----------------|----------------|--------------------------------------|
| INFRA-09, INFRA-10 | Phase 1 | New versions (compose images, buildpacks, `.nvmrc`, `packageManager`, npm packages) go only to their single source and are covered by Renovate. New tasks are cacheable, with declared inputs and outputs. Sub-scopes delivered by the phase that introduces the artefact: Phase 2 — compose and Testcontainers images (Axon Server, PostgreSQL, etc.) as catalog entries with tag and digest, passed to integration tests as task-input system properties and to compose through a Gradle-generated versions env file, with no image literals and both checked in CI (INFRA-09), plus Testcontainers singletons with local reuse (INFRA-10); Phase 5 — `packageManager` in `package.json`, exact npm versions with `pnpm-lock.yaml`, and the Gradle Node plugin convention reading `.nvmrc` (INFRA-09); Phase 6 — Paketo builder/run images and buildpacks with tag and digest (INFRA-09) and reused buildpack caches (INFRA-10). |
| INFRA-16, TEST-09, TEST-10, TEST-13 | Phase 1 | Every new file carries an SPDX header and is auto-formatted. Public contracts and decision logic have KDoc. Biome lints TypeScript from the moment the frontend exists (Phase 5). |
| CICD-01, CICD-08 | Phase 1 | Every new test level, analysis and report joins the CI workflow; coverage reporting joins in Phase 3 with Kover (TEST-08). Every new workflow, script and compose file is linted locally and in CI; `docker compose config` validation joins with the first compose file in Phase 2 (CICD-08). |
| DOC-03, UPS-01..04 | Phase 2 | Every significant decision gets an ADR in the phase that makes it. Every upstream defect gets a record, and every workaround gets a marker. |
| TEST-11, TEST-12, TEST-14 | Phase 2 | New tests go into the right suite, shared helpers live in `testFixtures`, and the standard toolchain applies (frontend: Vitest, Testing Library, MSW). |
| IDEM-04 | Phase 2 | TypeID everywhere. Read models store native `uuid`, and the API boundary rejects malformed or wrong-type IDs (Phase 5 onward). |
| IDEM-01, IDEM-02, DEMO-01 | Phase 3 | Every new state-changing command accepts an idempotency key, uses client-generated IDs and stamps its decision record. |
| TEST-01, TEST-02, TEST-06, TEST-08, TEST-15 | Phase 3 | Every new decision gets given-when-then tests with tag assertions, prose names and rule tags. Architecture rules grow with new modules and slices. Coverage thresholds hold, and automation logic gets the high threshold in Phase 10. |
| TEST-03 | Phase 3 onward (verified in Phase 10) | Every service gets component tests from the phase that creates it. |
| DOC-05 | Phase 3 | Every new slice gets an Event Modeling description in the phase that builds it. |
| READ-02, READ-04 | Phase 4 | Feature phases extend the catalogue and student views (schedule and timetable in Phase 8, waiting list in Phase 10). |
| IDEM-03, RES-02 | Phase 4 | Every new projection is idempotent and follows the processor error policy. |
| API-01, API-02, TEST-18 | Phase 5 | Every new operation is exposed through the BFF with OpenAPI, problem+json errors and explicit mapping functions. |
| INFRA-15, INFRA-17, RES-07 | Phase 6 | Every new service (e.g. the automation service in Phase 10) gets an environment descriptor, shell-less health checks and run configurations. |
| UI-07, UI-08, UI-09 | Phase 7 | Every UI-bearing phase has a UI-SPEC design contract and a UI review, adds stories for new components and keeps axe checks green. |

**Execution-time approval gates** (never applied without the author's explicit approval): GitHub repository settings (GH-01, GH-06, GH-07, GH-10), third-party skills and MCP servers (AIDEV-01, AIDEV-04), publication of upstream issues (UPS-03), Build Scans (INFRA-10), external coverage SaaS (TEST-17).

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13

**Coverage:** 150/150 v1 requirements mapped (see REQUIREMENTS.md Traceability)

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Repository, Build & CI Foundation | 17/17 | In Progress | - |
| 2. Runtime Infrastructure & DCB Spikes | 0/TBD | Not started | - |
| 3. Command Walking Skeleton | 0/TBD | Not started | - |
| 4. Read Side & Live Queries | 0/TBD | Not started | - |
| 5. BFF & First Live UI | 0/TBD | Not started | - |
| 6. Containerised System | 0/TBD | Not started | - |
| 7. UI Foundation | 0/TBD | Not started | - |
| 8. Course Administration & Schedules | 0/TBD | Not started | - |
| 9. Atomic Transfer | 0/TBD | Not started | - |
| 10. Waiting List & Automation | 0/TBD | Not started | - |
| 11. DCB Teaching UI | 0/TBD | Not started | - |
| 12. Observability, Resilience & Signed Releases | 0/TBD | Not started | - |
| 13. Reference Documentation & Final Polish | 0/TBD | Not started | - |
