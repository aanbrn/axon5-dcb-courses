# Axon 5 DCB Courses

## What This Is

A reference implementation of the classic "students subscribe to courses" domain built on Axon Framework 5 using Dynamic Consistency Boundaries (DCB) instead of aggregates, inspired by the article "Killing the Aggregate". It is a learning example engineered to production-grade standards — architecture, code, tests, documentation, CI/CD and observability — aimed at the community (developers moving from Axon 4 aggregates to DCB), at the author's own deep learning, and as a portfolio/showcase piece.

## Core Value

Show convincingly — in working, tested, documented code — how DCB enforces business invariants that span multiple entities (course capacity, per-student limits, unique names, transfers, schedule conflicts) without aggregates, and why that is better than the aggregate-based approach.

## Requirements

### Validated

(None yet — ship to validate)

### Active

**Domain (write side, DCB):**
- [ ] Course lifecycle: create course, rename course, change course capacity
- [ ] Student registration
- [ ] Subscribe student to course / unsubscribe student from course
- [ ] Invariant: course capacity is never exceeded, including under concurrent subscriptions
- [ ] Invariant: per-student limit on the number of subscribed courses
- [ ] Invariant: course name is unique; a renamed course releases its old name
- [ ] Atomic transfer of a student from one course to another (one decision spanning three entities)
- [ ] Waiting list: subscribing to a full course enqueues the student; a freed seat automatically subscribes the next student
- [ ] Course schedule slots; a student cannot subscribe to courses with overlapping schedules

**CQRS:**
- [ ] Clear separation of command side (DCB decisions over the event store) and query side (projections into dedicated read models)
- [ ] Queries served via QueryBus; live updates via subscription queries

**Services:**
- [ ] Command service (DCB decision logic)
- [ ] Query/projection service (read models)
- [ ] Automation service (event-driven processes such as the waiting list)
- [ ] API gateway/BFF exposing a REST API with OpenAPI specification
- [ ] Axon Server as DCB-capable event store and message routing between services

**UI (React + TypeScript):**
- [ ] Modern, polished, functional UI covering all business operations
- [ ] Real-time updates of views (SSE/WebSocket backed by subscription queries)
- [ ] Event explorer: browse events with tags and see which events formed the consistency boundary of a given command
- [ ] Concurrency demo: many students racing for the last seat, visibly preserving the invariant
- [ ] Demo role switcher (admin / student X) instead of real authentication

**Quality & testing:**
- [ ] Unit tests (given-when-then for DCB decisions)
- [ ] Component tests per service
- [ ] Integration tests (Testcontainers with Axon Server and databases)
- [ ] End-to-end tests through the browser (Playwright)
- [ ] Architecture tests (module/slice boundaries)
- [ ] Load test of concurrent subscriptions (Gatling or k6)
- [ ] Code coverage measurement and reporting
- [ ] Documented, idiomatic Kotlin code

**CI/CD & supply chain:**
- [ ] GitHub Actions: build, all test levels, coverage, static analysis, container image build
- [ ] Automated dependency and tool version update tracking (e.g. Renovate/Dependabot)
- [ ] Vulnerability scanning of dependencies and built container images
- [ ] Secret leak detection in the repository

**Observability:**
- [ ] Metrics, tracing and logs via OpenTelemetry with Grafana stack in docker compose
- [ ] Optional connection to AxonIQ Platform (author has an account and token for verification)

**Documentation:**
- [ ] Presentable README (overview, quick start, screenshots/diagrams)
- [ ] Architecture documentation: ADRs, C4 diagrams, Event Modeling / Event Storming artifacts, documentation site
- [ ] Explanation of DCB concepts as applied in this project
- [ ] Comparison with the Axon 4 aggregate approach (documentation only: diagrams and snippets showing how each scenario would look with aggregates and why it is worse)
- [ ] "How this was built" section: AI-assisted development process, with planning history kept in `.planning/` and lessons learned

**Runtime:**
- [ ] Whole system runs locally with docker compose

### Out of Scope

- Authentication/authorization (OAuth2/OIDC, Keycloak) — deferred to v2; demo role switcher keeps focus on DCB
- Working aggregate-based implementation in code — comparison lives in documentation only; a parallel implementation would roughly double the effort
- Splitting the write side by entity (separate course/student command services) — contradicts the core DCB idea, since invariants span courses and students
- Kubernetes/cloud deployment — docker compose is enough for a reference example

## Context

- The author has hands-on experience with Axon Framework 4 and its aggregate-based model, and recognized the problems described in "Killing the Aggregate" from practice.
- Axon Framework 5 introduces DCB support; the project is a way to learn it deeply and produce a reference others can follow.
- A second goal is to explore AI-assisted development; the process itself (GSD planning artifacts, decisions, lessons) is part of the deliverable.
- Audience is broad: community (public GitHub repo, "before/after" emphasis), self-study, and portfolio/article material.
- Axon 5 and its DCB support are new — APIs, Axon Server DCB support and Spring Boot integration must be verified against current official docs rather than assumed.
- Author has an AxonIQ Platform account and token, so the integration can be verified for real.

## Constraints

- **Tech stack (backend)**: Kotlin + Spring Boot + Axon Framework 5 — author's choice; idiomatic Kotlin over Java-style code
- **Event store**: Axon Server (DCB-capable) — native AxonIQ store with DCB support, run in Docker
- **Architecture**: multiple services split by CQRS role (command / query / automation) plus API gateway/BFF — makes CQRS and Axon location transparency visible
- **Frontend**: React + TypeScript, organised by Feature-Sliced Design (FSD) and enforced by Steiger
- **Test build structure**: Gradle JVM Test Suites for test levels, Java Test Fixtures for shared test helpers
- **Formatting**: every produced artifact is auto-formatted and style-checked (locally via pre-commit hook, enforced in CI)
- **Runtime**: docker compose for local run of the entire system
- **Quality bar**: "highest professional class" — every artifact (code, tests, docs, CI) must be reference quality
- **Language**: all code, docs and repository artifacts in English

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| DCB instead of aggregates | Core subject of the project; invariants span multiple entities | — Pending |
| Kotlin + Spring Boot | Author preference; concise, modern | — Pending |
| Axon Server as event store | Native DCB support from AxonIQ | — Pending |
| Services split by CQRS role (command / query / automation + BFF), not by entity | DCB invariants span courses and students; role split showcases CQRS | — Pending |
| Aggregate comparison only in docs | Keeps scope manageable while preserving the "before/after" story | — Pending |
| No auth in v1, demo role switcher | Keeps focus on DCB; auth deferred to v2 | — Pending |
| AI-assisted process documented as part of the reference | Second learning goal of the author | — Pending |
| Single source of truth for versions per ecosystem: Gradle version catalog for everything Gradle consumes (incl. tools, container images); `.java-version` for JDK; `.nvmrc` for Node.js; `packageManager` + lockfile for pnpm/npm; SHA in workflows for Actions | No duplicated versions; native mechanisms per ecosystem keep Renovate working | — Pending |
| TypeID (type prefix + UUIDv7, base32) for entity IDs, stored as native `uuid` | Client-generatable, time-ordered, readable and type-safe in event explorer/logs; immutable in events, so decided up front | — Pending (verify Kotlin/TS library maturity in Phase 1) |
| UI phasing: dedicated UI foundation phase, a UI slice with its own UI-SPEC in every feature phase, a dedicated DCB teaching-UI phase, final UI review | Every feature is demonstrable end to end; showcase UI gets its own design contract | — Pending |
| Storybook + WCAG 2.2 AA (axe in component and E2E tests) | Component catalogue and accessibility are part of reference quality | — Pending |
| Git workflow: branch per phase, PR via /gsd-ship, squash merge only, `main` protected by ruleset (PR required, checks, linear history, 0 approvals) | Fits GSD ship (built around squash), clean `main`, PR as review point for a solo author | — Pending |
| release-please owns tags and releases; GSD `create_tag: false` | Avoid conflicting `vX.Y` (GSD) and `vX.Y.Z` (release-please) tags | ✓ Applied in config |
| Upstream-first: report defects/gaps upstream, one record per case in `docs/upstream/`, workarounds marked by record ID with CI consistency check | Be useful to the products we depend on; keep local hacks visible and removable | — Pending |
| CLI tools pinned via mise; documented tooling inventory and `doctor` check | Reproducible environment beyond Gradle-managed tools | — Pending |
| Vetted, project-local Claude Code skills plus own project skills | Better AI-assisted development; skills are code and must be reviewed | — Pending |
| Frontend and docs site as Gradle subprojects (Gradle Node plugin) | Explicit build graph: BFF OpenAPI → generated client → frontend build/test; one `./gradlew build` for the whole system | — Pending |
| Cloud Native Buildpacks (Paketo) for all images: `bootBuildImage` for JVM, Web Servers buildpack for frontend | One consistent image approach; built-in SBOM, base-image rebase, JVM memory tuning; no Dockerfiles | — Pending (verify pnpm support in Paketo Node.js buildpack in Phase 1) |
| JUnit 6 runner + Kotest Assertions/Property (no Kotest runner) | Native Spring Boot 4, Testcontainers and Gradle Test Suites support; suspend tests in JUnit 6; idiomatic Kotlin assertions and property testing | — Pending (verify Kotest/Kotlin 2.4 compatibility in Phase 1) |
| Gradle JVM Test Suites + Java Test Fixtures | Clean separation of test levels; shared fixtures without duplication | — Pending |
| FSD for frontend, enforced by Steiger | Explicit frontend architecture, symmetric to backend architecture tests | — Pending |
| Auto-format and style-check all produced artifacts | Consistent reference-quality code regardless of author (human or AI) | — Pending |
| Kotlin formatter with KDoc support (ktfmt) instead of ktlint; detekt `comments` rules; Dokka `failOnWarning` | ktlint does not format KDoc; KDoc quality is part of reference quality | — Pending (verify ktfmt KDoc behaviour in Phase 1) |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-11-20 after initialization*
