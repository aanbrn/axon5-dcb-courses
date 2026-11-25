<!-- GSD:project-start source:PROJECT.md -->

## Project

**Axon 5 DCB Courses**

A reference implementation of the classic "students subscribe to courses" domain built on Axon Framework 5 using Dynamic Consistency Boundaries (DCB) instead of aggregates, inspired by the article "Killing the Aggregate". It is a learning example engineered to production-grade standards — architecture, code, tests, documentation, CI/CD and observability — aimed at the community (developers moving from Axon 4 aggregates to DCB), at the author's own deep learning, and as a portfolio/showcase piece.

**Core Value:** Show convincingly — in working, tested, documented code — how DCB enforces business invariants that span multiple entities (course capacity, per-student limits, unique names, transfers, schedule conflicts) without aggregates, and why that is better than the aggregate-based approach.

### Constraints

- **Tech stack (backend)**: Kotlin + Spring Boot + Axon Framework 5 — author's choice; idiomatic Kotlin over Java-style code
- **Event store**: Axon Server (DCB-capable) — native AxonIQ store with DCB support, run in Docker
- **Architecture**: multiple services split by CQRS role (command / query / automation) plus API gateway/BFF — makes CQRS and Axon location transparency visible
- **Frontend**: React + TypeScript, organised by Feature-Sliced Design (FSD) and enforced by Steiger
- **Test build structure**: Gradle JVM Test Suites for test levels, Java Test Fixtures for shared test helpers
- **Formatting**: every produced artifact is auto-formatted and style-checked (locally via pre-commit hook, enforced in CI)
- **Runtime**: docker compose for local run of the entire system
- **Quality bar**: "highest professional class" — every artifact (code, tests, docs, CI) must be reference quality
- **Language**: all code, docs and repository artifacts in English

<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->

## Technology Stack

## Read This First: Findings That Change Roadmap Decisions

## Recommended Stack

### Core Technologies: Backend

| Technology | Version | Purpose | Why Recommended | Confidence |
|------------|---------|---------|-----------------|------------|
| Kotlin | 2.4.21 | Language | Latest stable (2.5.0-RC is pre-release). The official Axon Kotlin example uses 2.4.21 with `languageVersion` 2.2, and `axon-kotlin` is built with 2.4.20. | HIGH |
| JDK | 25 LTS (Temurin) | Build toolchain and runtime | AF's own CI matrix is JDK 21 and 25. Boot 4.1 supports 25. Run a Gradle toolchain on 25 and set `jvmTarget` to 21 only if a tool breaks. Fallback is 21. | MEDIUM-HIGH |
| Gradle | 9.9.2 (Kotlin DSL) | Build | Current release. Use version catalog, `build-logic` included build with convention plugins, configuration cache, toolchains, dependency locking and verification. | HIGH |
| Spring Boot | 4.1.3 | Application framework | Newest 4.1 patch. See finding 5. Springdoc 3.2.0 targets this line. Bump to 4.2.x only after 4.2.1 and an Axon compat check. | MEDIUM |
| Axon Framework (OSS) | 5.4.2 | Messaging, entities, event sourcing, `axon-test`, Kotlin extension, Micrometer metrics, Reactor extension | Apache 2.0 core, released 2026-11-16. Import `org.axonframework:axon-framework-bom` via the Axoniq BOM. | HIGH |
| Axoniq Framework | 5.4.2 | `axon-server-connector`, `axoniq-spring-boot-starter`, `axoniq-testcontainer` | The only way to talk to Axon Server and DCB. Commercial terms, free for dev. Import `io.axoniq.framework:axoniq-framework-bom:5.4.2`. | HIGH |
| Axon Server | 2026.1.8 (`docker.axoniq.io/axoniq/axonserver`) | DCB event store and message routing | Latest 2026.1 patch. 2026.1.5 and 2026.1.6 are marked "Do not use". Java 21 minimum. 2026.1.7 and 2026.1.8 contain DCB fixes. One JAR now, no Standard/Enterprise split. Pin the tag and digest. | HIGH on version, LOW on licence behaviour |
| Axon Server client lib | `io.axoniq:axonserver-connector-java` 2026.1.1 | gRPC client | Comes transitively. Pin `protobuf-java` 4.36.2 or newer (the official Boot 4 example pins 4.36.2) to avoid the documented `RuntimeVersion` `NoClassDefFoundError`. | HIGH |
| Jackson | 3.x (`tools.jackson`), Boot-managed | Axon payload conversion and REST | Boot 4 defaults to Jackson 3, and AF5 supports it. Set `axon.converter.general=jackson` as the official example does. Add `tools.jackson.module:jackson-module-kotlin`. | MEDIUM-HIGH |

### Core Technologies: Service Shape

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| `spring-boot-starter-webflux` + Kotlin coroutines | Boot-managed | BFF REST and SSE | `QueryGateway.subscriptionQuery` returns a `Publisher`, so Reactor or `Flow` to `text/event-stream` is the natural SSE fit. The official Boot 4 example uses webflux plus springdoc-webflux. |
| `spring-boot-starter-actuator` | Boot-managed | Health, metrics and probes in all services | Command, query and automation services need no REST API. They are Axon handlers reached through Axon Server, which makes location transparency visible. |
| `axoniq-spring-boot-starter` | 5.4.2 | Auto-config for Axon on Boot, includes `axon-server-connector` | Single dependency used by the official Boot 4 example. Entities use `@EventSourced`. |
| `org.axonframework.extensions.spring:axon-spring-boot-starter-test` | 5.4.2 | `@AxonSpringBootTest` | Gives an injected `AxonTestFixture` with Spring wiring. |

### Database

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| PostgreSQL | 18 (`postgres:18`, currently 18.7) | Read models and token stores | PostgreSQL 19 has an RC but GA is unconfirmed, so use 18. |
| Spring Data JDBC | Boot-managed | Projection persistence | Immutable Kotlin data classes, no JPA proxies or `kotlin-jpa` plugin, and it shares the Spring transaction with the token store. Use `JdbcClient` for hand-written query projections. |
| Axon `JdbcTokenStore` | AF 5.4.2 (auto-configured) | Tracking tokens for pooled streaming processors | With a `DataSource` and no JPA, `JdbcAutoConfiguration` registers a Jackson-JSON `JdbcTokenStore` over `SpringDataSourceConnectionProvider`. This avoids the PostgreSQL `@Lob` and `OID` problem in the AF known-issues page, and keeps token and projection writes in one transaction. Still declare the `TokenStore` explicitly, because with no `TokenStore` bean and no `EntityManager` the Boot default is `InMemoryTokenStore`. |
| Flyway | Boot-managed (12.4.0 in Boot 4.1.3) | Schema migrations including the token table | AF5 requires a `mask` column on the token table. Create the table by migration, not `createSchema`. Use `spring-boot-starter-flyway`. |

### Core Technologies: Frontend

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Node.js | 24 LTS (24.22.3) | Tooling and CI | Active LTS, matches the dev machine. Node 26 just became LTS and is also supported by Vitest 5. | HIGH |
| pnpm | 12.x | Package manager | Strict `node_modules`, a `minimumReleaseAge` cooldown, and build-script allow-listing for npm supply-chain control. | MEDIUM |
| React | 19.3.1 | UI | Current stable. | HIGH |
| TypeScript | 6.0.3 | Language | See finding 6. | HIGH |
| Vite | 8.3.8 | Build and dev server | Current. Pair with `@vitejs/plugin-react` 6.3.0. | HIGH |
| TanStack Router | 1.170.x | Type-safe routing | Typed route params and search params, plus data loaders, in a pure SPA. Alternative is React Router 8.5.2. | MEDIUM |
| TanStack Query | 5.108.x | Server state | Fits the "snapshot then SSE deltas then `setQueryData`" pattern. | HIGH |
| Tailwind CSS | 4.3.5 (`@tailwindcss/vite`) | Styling | Standard with shadcn/ui. | HIGH |
| shadcn/ui (CLI 4.23) + `radix-ui` 1.8 | n/a | Component library | Copy-in accessible components that you own, which makes a polished UI cheap. Add `lucide-react`, `recharts` for the concurrency and capacity charts, and `@tanstack/react-table` 9 for the event explorer. | MEDIUM-HIGH |
| react-hook-form 7.92 + zod 4.6 | n/a | Forms and validation | Standard pairing. Use zod schemas generated or aligned from OpenAPI. | HIGH |
| Orval | 8.48 | OpenAPI to TS client, TanStack Query hooks and MSW mocks | Mature (8.x) and generates query hooks and mock handlers, so tests share the contract. Needs Node 22.18+. Alternative: `@hey-api/openapi-ts` 0.99, which is pre-1.0. | MEDIUM |
| Browser `EventSource` | native | SSE client | Auto-reconnect and `Last-Event-ID`, no library needed. Behind nginx set `proxy_buffering off`, and use HTTP/2 to avoid the 6-connection limit. | HIGH |

### Supporting Libraries: Backend

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `org.axonframework.extensions.kotlin:axon-kotlin` | 5.4.2 | Reified `queryGateway.query<T>()`, `queryMany`, `@InjectEntity` nullability, value-class IDs, `EventSourcedEntityModuleExt` | Always. Without it, `GiftCard?`-style nullable `@InjectEntity` parameters are treated as non-null. |
| `org.axonframework.extensions.kotlin:axon-kotlin-test` | 5.4.2 | `whenever(...)` and `exception(KClass, msg)` fixture helpers | Unit tests. `when` is a Kotlin keyword, hence `whenever`. |
| `org.axonframework.extensions.reactor:axon-reactor` | 5.4.2 | `ReactorQueryGateway` returning `Flux` | BFF. `reactor-core` is mandatory for subscription queries anyway. |
| `org.axonframework.extensions.metrics:axon-metrics-micrometer` | 5.4.2 | Command, event processor and query metrics | All services. Apache 2.0. |
| `spring-boot-starter-opentelemetry` | Boot-managed (4.1.3) | OTLP export of metrics, traces and logs via Micrometer | All services. See Observability. |
| `io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0` | 2.34.0-alpha | Logs to OTLP | Boot does not bridge Logback into the OTel `SdkLoggerProvider` out of the box. Keep log lines trace-correlated. |
| `org.springdoc:springdoc-openapi-starter-webflux-ui` | 3.2.0 | OpenAPI 3.1 from the BFF | BFF only. 3.2.0 is built on Boot 4.1.2. Write `openapi.json` in a Gradle task, commit it, and fail CI on drift. |
| `io.github.oshai:kotlin-logging-jvm` | latest 6.x | Idiomatic logging | Optional. Used by the official Kotlin example and by the Platform client. |
| `io.axoniq.platform:axoniq-platform-spring-boot-starter` | 5.4.4 | Optional AxonIQ Platform connection | Put behind a `platform` Spring profile or Gradle `-P` flag. Needs only OSS AF messaging. Set `axoniq.platform.credentials=ENV_ID:TOKEN` from an env var, never committed. Outbound `34.149.46.65:7000` must be allowed. |

### Testing

| Tool | Version | Purpose | Notes |
|------|---------|---------|-------|
| JUnit | 6.x (Boot-managed 6.0.3) | Test platform | AF 5.4.2 itself builds on JUnit 6.1.3, so there is no compat concern. Boot 4.1.3 manages 6.0.3. |
| `AxonTestFixture` (`org.axonframework:axon-test`) | 5.4.2 | Given/when/then for DCB decisions | `given().event(..)`, `when().command(..)`, `then().success().events(..)`, `.exception(..)`, `.noEvents()`, `await(..)`, `expect(config -> ..)`. Runs on the same `ApplicationConfigurer` as production and exercises real criteria resolution. Use `asIntegrationTest()` for real infrastructure. |
| MockK | 1.14.12 | Mocking | Use `springmockk` 5.0.1 only if `@MockkBean` is needed in Spring slices. Check Boot 4 compat in Phase 1. |
| Kotest assertions | 6.2.7 (`kotest-assertions-core` only) | Fluent matchers | Use the assertion library, not the Kotest runner, so JUnit and `AxonTestFixture` stay the one test style. |
| Testcontainers | 2.0.5 | Real infrastructure in tests | 2.x artifact names are prefixed: `testcontainers-postgresql`, `testcontainers-junit-jupiter`. Use `spring-boot-testcontainers` with `@ServiceConnection`. |
| `io.axoniq.framework:axoniq-testcontainer` | 5.4.2 | `AxonServerContainer` | `withDcbContext(true)` and `withDevMode(true)` exist. It posts to `/v2/cluster/init?dcb=true` to create the DCB `default` context. `@ServiceConnection` is supported. `SharedAxonServerContainer` allows reuse across modules. |
| Awaitility | 4.3.0 | Async assertions | Eventual consistency in projections. |
| Konsist | 1.0.0-beta.1 (fallback 0.18.1) | Kotlin architecture tests | Primary architecture tool. See Alternatives. |
| ArchUnit | 1.5.1 | Bytecode-level rules | Optional. AF itself uses it. Skip unless a rule needs bytecode. |
| Gatling | 3.16.1, Gradle plugin `io.gatling.gradle` | Load test of concurrent subscriptions | Kotlin DSL, JVM, Gradle integration, and assertions that can fail CI. |
| Kover | 0.9.11 | Coverage | Kotlin-aware and merges multi-module reports. XML output is JaCoCo-compatible for Codecov. Instrument unit and component tests, since integration JVMs share the process. |
| Vitest | 5.1.1 | Frontend unit and component tests | Needs Node 22.12+, 24 or 26+. Coverage via `@vitest/coverage-v8`. |
| Testing Library | `@testing-library/react` 16.3.3, `user-event` 14.6.13, `jest-dom` 8.0.0 | Component tests | Use `jsdom` (31.x). |
| MSW | 3.0.7 | API mocking | Orval generates handlers. |
| Playwright | `@playwright/test` 1.65.1 | E2E and concurrency demo | TypeScript runner in `e2e/`, against `docker compose up --wait`. Add `@axe-core/playwright` 4.14.0 for accessibility checks, and record traces on CI failure. |

### Static Analysis and Formatting

| Tool | Version | Purpose | Notes |
|------|---------|---------|-------|
| Spotless Gradle plugin | 8.11.0 with ktlint 1.8.0 | Kotlin and `*.kts` formatting | Use `ratchetFrom("origin/main")` and `spotlessCheck` in CI. |
| detekt | `dev.detekt` 2.0.0-alpha.6 | Code smells, complexity, KDoc rules | The 1.23.8 line is built on Kotlin 2.0.21 and does not match Kotlin 2.4. 2.0 alpha.6 is built on Kotlin 2.4.10 and tested on JDK 25. **Pre-release, so LOW-MEDIUM.** Fallback: `allWarningsAsErrors` plus ktlint plus Konsist. |
| Kotlin compiler | `allWarningsAsErrors=true`, `-Xjsr305=strict`, JSpecify strict | First-line static analysis | Boot 4 and Axon 5 use JSpecify nullability. |
| Biome | 2.6.8 | Frontend lint and format | One fast tool, with no TypeScript-version coupling. Skip ESLint. |
| Knip | 6.47 | Dead exports and dependencies in the UI | Optional. |

### Infrastructure and CI/CD

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| GitHub Actions | `actions/checkout` v7.0.1, `gradle/actions` v6.4.1 (`setup-gradle`), `docker/build-push-action` v7.4.0 | CI | **Pin every action by commit SHA**, with Renovate keeping the tag comment current. Default `permissions: read-all`, with per-job elevation and `persist-credentials: false`. |
| Renovate | 44.x (GitHub App or `renovatebot/github-action`) | Dependency, action, image, Gradle catalog and wrapper updates | Preferred over Dependabot: `helpers:pinGitHubActionDigests`, Docker digest pinning, grouped updates, `minimumReleaseAge` cooldown (about 7 days), and regex managers for the Axon Server tag in compose and Java sources. Keep Dependabot *alerts* on, and turn Dependabot *version updates* off. |
| Gradle dependency locking + `verification-metadata.xml` | built in | Reproducible, checksum-verified dependencies | Needed for scanners to see exact versions and to detect tampering. |
| CycloneDX Gradle plugin | 3.6.1 (`org.cyclonedx.bom`) | SBOM per service | Feeds Grype or Trivy and release attestations. |
| Syft | 1.57.x | Image SBOM | For the container SBOM, attached as an attestation. |
| Trivy | 0.76.x binary | Filesystem, IaC and image scanning | Install the pinned release binary and verify its checksum (or run the image by digest) instead of consuming a floating action tag. See finding 8. |
| Grype | 0.122.x | Second-opinion scan on SBOMs | Cheap with Syft output. Optional but good supply-chain signal. |
| `actions/dependency-review-action` | latest SHA-pinned | PR gate on newly introduced vulnerable or disallowed-licence dependencies | Native to GitHub. Free on public repos. |
| gitleaks | 8.30.1 (CLI, not `gitleaks-action`) | Secret detection in CI and a pre-commit hook | The Action needs a licence for organisations. The CLI is MIT. Also enable GitHub secret scanning and push protection. |
| CodeQL | `github/codeql-action` (SHA-pinned) | SAST for Kotlin and JavaScript/TypeScript | Native and free for public repos. |
| OpenSSF Scorecard | 5.7.0 | Supply-chain health badge and checks | Showcase value. |
| Cosign + GitHub artifact attestations | cosign 3.1.x | Keyless image signing and SLSA provenance | Sign images and attach SBOM attestations. |
| Jib | 3.5.4 (`com.google.cloud.tools.jib`) | Backend images | No Docker daemon, reproducible layers, non-root, base image pinned by digest. Base: `eclipse-temurin:25-jre-noble` (digest-pinned) or `gcr.io/distroless/java25-debian13`. The 3.5.4 Gradle plugin was released 2026-07-14, so confirm Gradle 9.9 compat in the first CI run. |
| `nginxinc/nginx-unprivileged:stable-alpine` | digest-pinned | UI image | Multi-stage Dockerfile: `node:24-alpine` build, then static files on non-root nginx with an SSE-safe proxy block. |
| OWASP Dependency-Check 13.0.0 | n/a | **Not recommended** | Slow, NVD-API-key dependent and noisy. Trivy, Grype and dependency-review cover the need. |

### Observability

| Technology | Version | Purpose | Notes |
|------------|---------|---------|-------|
| Spring Boot OTel starter path | `spring-boot-starter-opentelemetry` | Traces, metrics, logs over OTLP from Micrometer Observation | Chosen over the OTel Java agent because Axon spans and metrics already go through Micrometer, the one-instrumentation-system rule avoids double spans, and it keeps config in `application.yml`. Set `management.tracing.sampling.probability=1.0` in the demo. |
| OTel Java agent | 2.34.0 | **Optional** zero-code spans for gRPC (Axon Server leg), JDBC and HTTP | Enable only on a `-Pagent` or compose profile, never together with Micrometer HTTP instrumentation, or spans duplicate. |
| `axon-metrics-micrometer` | 5.4.2 | Axon capacity, latency and counters | Set `axon.metrics.micrometer.dimensional=true` (default). |
| Axon span export | custom `SpanFactory` over Micrometer `Tracer` and `Propagator` | Axon command, event, query and entity spans in Tempo | **Spike, LOW-MEDIUM.** AF tracing is built into the framework. Providing a `SpanFactory` bean turns it on, and the `axon.tracing.*` properties tune it. Fallback: `axoniq-tracing-micrometer` 5.4.2 in dev (licensed for production, temporary dev permission otherwise). |
| `grafana/otel-lgtm` | v0.41.1 (Grafana 13.3.1, Loki 3.7.10), digest-pinned | Local LGTM (Loki, Grafana, Tempo, Mimir/Prometheus) with OTLP on 4317/4318 | Single container, ideal for compose. Pre-provision dashboards for Axon metrics and the invariant demo. |
| Axoniq Platform | client 5.4.4 | Optional Axon-aware monitoring | Complements, not replaces, Grafana. See supporting libraries. |

### Documentation

| Tool | Version | Purpose | Why |
|------|---------|---------|-----|
| Docusaurus | 3.10.2 + `@docusaurus/theme-mermaid` | Docs site (README companion, ADRs, DCB explanation, how-it-was-built) | Same Node and TS toolchain as the UI and one Renovate ecosystem. MDX, versioning and built-in Mermaid (resolves Mermaid 12.3.0). Active, with Docusaurus 4 in canary. Antora would match AxonIQ's own docs, but needs AsciiDoc and a separate toolchain. |
| Structurizr (`structurizr/structurizr` image) | v2026.10.26 | C4 model as code (DSL), exported to Mermaid or PNG/SVG | Reference implementation of C4. The old `structurizr/cli` and `lite` repos are archived, so use the unified image and its `export` command in CI. Alternative: LikeC4 (mentioned by sibling research, 1.61.0). |
| Mermaid `eventmodeling` diagrams | Mermaid 12.3.0 (feature since 11.15.0) | Event Modeling in diagrams-as-code | Native swimlane, time-frame notation (`ui`, `cmd`, `evt`, `rmo`, `pcr`). Renders in Docusaurus and on GitHub. |
| Emlang | 1.3.0 | Optional YAML Event Model with Given/When/Then slices | Optional machine-checkable companion to the diagram. Maintenance activity unverified. |
| Mermaid / PlantUML | 12.3.0 / 1.2026.10 | Sequence, state and flow diagrams | Prefer Mermaid, because it renders natively in GitHub and Docusaurus. |
| adr-tools style MADR | n/a | ADRs | Plain markdown under `docs/adr/`. |

## Installation

# Frontend (Node 24 LTS, pnpm 12)

# Docs site

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Axon Server via `io.axoniq.framework` | Pure Apache-2.0 AF5 with `InMemoryEventStorageEngine` (tests) | Unit tests only. For anything multi-service it has no DCB-capable open-source store, since the JPA store is aggregate-based. |
| Axon Server (DCB) | `axoniq-postgresql` DCB event store | Only if the project must drop Axon Server. It is a licensed module and loses Axon Server routing, so it fails the project constraint. |
| Spring Boot 4.1.3 | Boot 3.5.16 | If the Boot 4 and Axon spike fails. Axon builds against 3.5 and it is the lowest-risk pairing, but its OSS support has ended. |
| Spring Boot 4.1.3 | Boot 4.2.0 | After 4.2.1 and an Axon matrix pass. It aligns Kotlin 2.4.21, JUnit 6.1.3 and Spring 7.1. |
| WebFlux BFF with coroutines | Spring MVC with virtual threads and `SseEmitter` | If the team dislikes reactive code. Subscription queries are `Publisher` based, so you would bridge them anyway. |
| Spring Data JDBC + `JdbcTokenStore` | Spring Data JPA/Hibernate 7 | Only if you want JPA entity features. It needs the `kotlin-jpa` and `allopen` plugins and a `bytea` mapping for tokens on Postgres. |
| Spring Data JDBC | jOOQ 3.21 | Heavy read-model SQL and compile-time-checked queries. Needs codegen wiring. Exposed 1.6 brings its own transaction manager, which conflicts with the token-store transaction. |
| Konsist | ArchUnit 1.5.1 | Java-heavy or bytecode-level rules. ArchUnit handles Kotlin synthetic classes poorly. Konsist reads Kotlin source and can check KDoc, naming and slice dependencies. |
| Kover | JaCoCo | Only if a tool needs JaCoCo-native exec files. Kover understands Kotlin inline and generated code. |
| Gatling (Kotlin DSL) | k6 2.4.0 | If you want JS tests, built-in OTLP output and Grafana integration. It is a second language, so Gatling was chosen. |
| Biome 2.6.8 | ESLint 10 + typescript-eslint 8.73 | Needs TS below 6.1, and you must pick the formatter separately. Choose only for a specific rule plugin. |
| TanStack Router | React Router 8.5.2 | Familiarity. React Router is fine for a plain SPA. |
| Orval | `openapi-typescript` 7.13 + `openapi-fetch` 0.17 | Minimal runtime. Peers TS 5.x, so it works but unsupported. |
| Docusaurus | MkDocs Material 9.8.2 / Zensical 0.1.x | MkDocs Material is maintenance mode. Zensical is too new to adopt for a showcase. |
| Docusaurus | Antora | If the docs should match AxonIQ's AsciiDoc. |
| Renovate | Dependabot | If you refuse a third-party app. Dependabot lacks digest pinning breadth and grouping. |
| Trivy + Grype + dependency-review | OWASP Dependency-Check | Compliance regimes that demand it. |
| OTel via Spring Boot starter | OTel Java agent 2.34.0 | When you want JDBC and gRPC spans without code. Don't combine with Micrometer HTTP spans. |
| Jib | Spring Boot `bootBuildImage` (Paketo) | If you want Paketo SBOMs and JVM tuning. It needs a Docker daemon and is slower. A multi-stage Dockerfile with layered jars is the other option. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `org.axonframework:axon-server-connector` 5.0.x or the 5.1.0-RC line | Stale pre-split artifacts. The release line stopped at 5.1.0-RC2 under the Apache group. | `io.axoniq.framework:axon-server-connector` 5.4.2 via `axoniq-spring-boot-starter`. |
| Axon Framework 4.13 | Aggregate-based. No DCB, no `AxonTestFixture` given/when/then on entities. | AF 5.4.2. |
| JPA/Hibernate event store (`AggregateBasedJpaEventStorageEngine`) | Not DCB. | Axon Server DCB context. |
| `axoniq-workflow-*`, `axoniq-legacy` sagas, `axoniq-dead-letter` | Commercial modules that stop the app without a licence. | Plain automation event handlers, idempotency in the decision, and Axon retry or error handlers. |
| Spring Boot 4.1.0 | Axon handler registration races with `applicationTaskExecutor`. | 4.1.3. |
| `InMemoryTokenStore` by accident | Silent full replays on restart. | Explicit `JdbcTokenStore` bean in the same database as projections. |
| Hibernate `@Lob` token tables on Postgres | PostgreSQL large-object `OID` storage makes tokens slow and awkward. | JSON `JdbcTokenStore`. |
| TypeScript 7.0.2 | Breaks typescript-eslint and `openapi-typescript` peer ranges. | 6.0.3. |
| `aquasecurity/trivy-action` by tag | March 2026 tag-hijack incident. | Pinned binary or SHA, plus checksum verification. |
| `gitleaks/gitleaks-action` | Licence required for organisations. | gitleaks CLI. |
| detekt 1.23.8 | Compiled against Kotlin 2.0.21, so parse or analysis mismatches on Kotlin 2.4. | `dev.detekt` 2.0.0-alpha.6, or drop it. |
| MkDocs Material for a new docs site | Maintenance mode. | Docusaurus. |
| Kotest runner | A second test style next to JUnit and `AxonTestFixture`. | JUnit plus Kotest assertions. |
| WebSocket for live updates | Bidirectional capability is unused. Needs a custom reconnect and auth story. | SSE. |
| Floating `latest` Docker tags | Non-reproducible and hostile to scanners. | Version tag plus digest, with Renovate. |

## Stack Patterns by Variant

- Document `AXONIQ_PLATFORM_AUTHENTICATION` in `.env.example` for a free Platform workspace, or an "up for under 12 hours" note.
- Keep CI on short-lived Testcontainers, which are inside the free dev and CI scope.
- Because the licence reaches Axon Server through the Platform connection, the author's token also covers the demo.
- Drop to Boot 3.5.16, the version Axon builds against. Keep Kotlin 2.4.21. Switch springdoc to its 2.x line (2.10.0 was released alongside 3.2.0) because springdoc 3.x targets Boot 4. Jackson reverts to 2.x (`com.fasterxml.jackson`), which AF5 also supports.
- Ship `LoggingSpanFactory` plus HTTP and gRPC spans from the Java agent profile, and document `axoniq-tracing-micrometer` as an opt-in dev profile.
- Do not enable the Platform starter by default. Make it opt-in through a compose profile and environment variable.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Axon 5.4.2 (OSS and Axoniq) | JDK 21 and 25 | Both in AF CI. Baseline is Java 21. |
| Axon 5.4.2 | Spring Boot 3.5.16, 4.0.8, 4.1.1+ | AF compat matrix. 4.1.0 known broken. Built on Spring 6.2, tested on Boot 4. |
| Axon 5.4.2 | Axon Server 2026.1.x | DCB context required. Create via `/v2/cluster/init?dcb=true` or UI. DCB needs Axon Server 2025.1 or newer. |
| Axon Server 2026.1.8 | Java 21+ | Image is Java 21. Use the `-nonroot` tag where possible. |
| Axoniq licence V2 | Framework 5, Axon Server 2026.0+ | V1 licences do not work with Framework 5. |
| Boot 4.1.3 | Kotlin 2.4.21 (override) | BOM manages 2.3.21. Use `extra["kotlin.version"]` or the catalog. Axon Kotlin extension is built on 2.4.20. |
| Boot 4.1.3 | springdoc 3.2.0 | springdoc 3.2.0 is built on Boot 4.1.2. |
| Boot 4.1.3 | Testcontainers 2.0.5, JUnit 6.0.3, Hibernate 7.4, Flyway 12.4 | All Boot-managed. |
| Vite 8.3.8 | `@vitejs/plugin-react` 6.x, Vitest 5.1.1 | `plugin-react` 6 requires Vite 8. Vitest 5 supports Vite 6, 7 and 8. |
| TypeScript 6.0.3 | typescript-eslint 8.73 (if ever used), `@hey-api/openapi-ts` | TS 7 is outside peer ranges. |
| Docusaurus 3.10.2 | React 18 or 19, Mermaid 12.3.0 | Event Modeling diagrams need Mermaid 11.15 or newer. |

## Sources

- Maven Central `maven-metadata.xml` and POMs (HIGH, authoritative for versions): `org.axonframework:axon-*` 5.4.2 (released 2026-11-16), `io.axoniq.framework:*` 5.4.2, `io.axoniq.platform:*` 5.4.4, `org.springframework.boot:*` 4.1.3 and 4.2.0, `spring-boot-dependencies` 4.1.3, springdoc 3.2.0, Testcontainers 2.0.5, Kotlin 2.4.21.
- Gradle plugin portal and `services.gradle.org` for Gradle 9.9.2, Kover, detekt, Spotless, Jib and CycloneDX (HIGH). npm registry for the frontend packages (HIGH). Docker Hub API for base image tags (HIGH).
- Axon Framework reference 5.4: framework comparison, release notes, Spring Boot integration, event store internals, stateful command handlers, monitoring, tracing, metrics, testing, Kotlin extension (HIGH). `docs.axoniq.io/axon-framework-reference/5.4/`.
- AF repo `AxonFramework/AxonFramework`: `examples/university-java-springboot-4`, `examples/university-demo-kotlin`, `.github/workflows/main.yml` (JDK 21 and 25 matrix), `.github/workflows/spring-boot-compatibility.yml` (Boot 3.5.16, 4.0.8, 4.1.0 allow-failure, 4.1.1), Spring Boot autoconfigure sources (`JdbcAutoConfiguration`) (HIGH).
- Axoniq Framework repo `AxonIQ/axoniq-framework`: README and licence statement, `AxoniqAddon` service files showing which modules are licence-gated, `AxonServerContainer` source (HIGH).
- Axoniq Platform reference: licensing, licence sources, offline licences, Framework connector (HIGH on documented behaviour, LOW where the Axon Server 12-hour claim conflicts with the Docker Hub 14-day text).
- Axon Server reference 2026.1 release notes and patch notes, Docker Hub `axoniq/axonserver` README (MEDIUM, with the licence conflict above).
- AxonIQ forum thread "Sudden license change of Axon Server?" (Allard Buijze, 2025-12-10): Developer plan is for development, CI, demos and short PoCs, and not for production (MEDIUM).
- axoniq.io pricing page (MEDIUM, the page text was partly garbled by the fetch).
- Spring Boot 4.2 reference pages on observability and tracing (MEDIUM, docs shown are for 4.2, and 4.1.3 behaviour assumed the same).
- Trivy supply-chain incident: GitHub advisory GHSA-69fq-xp46-6x23 and CVE-2026-33634, with Snyk, CrowdStrike and Aqua write-ups (HIGH on the incident, MEDIUM on exposure window).
- Material for MkDocs maintenance-mode announcement and Zensical PyPI release history (HIGH).
- Mermaid Event Modeling docs (`mermaid.js.org/syntax/eventmodeling.html`) and Emlang site (MEDIUM).
- GitHub releases for detekt, Konsist, Kover, Jib, Gatling, k6, Renovate, Trivy, Grype, gitleaks, Syft, cosign, Structurizr, grafana/docker-otel-lgtm and OpenTelemetry Java instrumentation (HIGH).

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

- **Spike findings for axon5-dcb-courses** (implementation patterns, constraints, gotchas) → `Skill("spike-findings-axon5-dcb-courses")`
- **Dynamic Consistency Boundaries** (DCB concepts and their mapping onto Axon Framework 5 and the Axon Server DCB API; vendored from AxonIQ/AxonFramework, provenance in its README.md) → `Skill("dynamic-consistency-boundaries")`
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-fast` for a trivial task inline, with no subagents and no PLAN.md
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->

## Decision Precedence

The "Technology Stack" section above is generated from early research (`.planning/research/STACK.md`). Where it conflicts with the Key Decisions in `.planning/PROJECT.md` or with `.planning/REQUIREMENTS.md`, the decisions and requirements win. Notable overrides:

- Container images are built with Cloud Native Buildpacks (`bootBuildImage`, Paketo Web Servers), not Jib or Dockerfiles.
- Kotlin formatting uses ktfmt (KDoc-aware) via Spotless, not ktlint.
- JDK version comes only from `.java-version`, Node.js from `.nvmrc`, pnpm from `packageManager`; everything else Gradle consumes comes from the version catalog via the `:platform` module.
- Entity IDs use TypeID; no MapStruct or other mapping generators; no kapt.
- Tags and releases are owned by release-please; GSD works on one branch per phase with squash merges.

## Agent Git Workflow

How a Claude Code session takes a change from branch to `main` in this repository. `main` is protected by the
`main` ruleset, so every change, however small, goes through a pull request.

- **Branch names.** A pull request from a branch of this repository must come from `gsd/phase-<N>-<slug>`
  (`renovate/*` and `release-please--*` are for bots); `governance-success` fails on any other name. A worktree
  created by `EnterWorktree` starts on `worktree-<name>`, so rename it before the first push.
- **No force-push.** Never rewrite a pushed branch. After a rebase, push the result under a new branch name and
  delete the old remote branch.
- **Finish the job yourself.** While the session sits in a worktree, the sandbox refuses git commands aimed at the
  main checkout (`git -C <main>`, `cd <main> && git ...`), and the human's `!` commands too. Do not hand the human
  terminal steps. After the merge, leave the worktree with `ExitWorktree` (`action: remove`; pass
  `discard_changes: true` only when its branch is already squash-merged), then, from the main checkout, check
  `git status --short`, run `git pull --ff-only`, delete the local branch, run `git fetch --prune` and, when
  `mise.toml` or `mise.lock` changed, `mise install`. If the human deleted the session's worktree, run
  `ExitWorktree` (`action: keep`) before `EnterWorktree` creates a new one.
- **Plain git commands in a worktree.** The sandbox refuses compound commands that mix git with loops, `&&` chains,
  redirects or `cd`; run each git command on its own.
- **GSD executors run isolated.** The GSD agent isolation guard requires `isolation: "worktree"` on `gsd-executor`
  dispatches. Commit the quick task's PLAN first, pass the plan commit and its parent as the allowed bases in the
  embedded worktree branch check, and merge the executor's branch back with
  `gsd-tools query worktree.cleanup-wave --manifest <file>`.
- **Governed paths ask in the main checkout and at merge.** Two hooks in `.claude/settings.json` (ADR-0015) ask the
  author before an Edit, Write, MultiEdit or NotebookEdit of a governed path in the main checkout and in any worktree
  that is not an agent worktree (`.claude/worktrees/agent-<id>` on branch `agent-<id>` or `worktree-agent-<id>`).
  Inside an agent worktree the edit hook does not ask; the merge hook asks once, listing the governed files, before a
  Bash command whose `git merge` or `worktree.cleanup-wave` brings them into the main checkout or another worktree.
  Change governed files with the file tools, not with shell writes, pass the manifest and refs as literal values so the
  merge hook can resolve them, and when the author declines a prompt, stop and report instead of working around it.
  The orchestrator reads the governed diff of the branch (`git diff HEAD...<branch>`) before the author confirms the
  merge prompt.
