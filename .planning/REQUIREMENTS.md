# Requirements: Axon 5 DCB Courses

**Defined:** 2026-11-20
**Core Value:** Show convincingly — in working, tested, documented code — how DCB enforces business invariants that span multiple entities (course capacity, per-student limits, unique names, transfers, schedule conflicts) without aggregates, and why that is better than the aggregate-based approach.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Foundation & Infrastructure

- [x] **INFRA-01**: Developer can build the whole backend with one Gradle command from a multi-project build with convention plugins (`build-logic`) and a version catalog
- [ ] **INFRA-09**: Every version is pinned in exactly one place per ecosystem: the Gradle version catalog holds all libraries, BOMs (consumed through the `:platform` module, no dependency-management plugin), Gradle plugins, tool versions and all container images used anywhere — infrastructure images for compose and integration tests (Axon Server, PostgreSQL, Grafana LGTM, Toxiproxy, etc.) as well as Paketo builder/run images and individual buildpacks — as full references with tag + digest; integration tests receive them as system properties declared as task inputs (no image literals in test code), docker compose consumes a Gradle-generated versions env file (no image literals in compose files, both checked in CI), and Renovate updates tag and digest together; the JDK version lives only in `.java-version` (read by the Gradle toolchain convention, CI `setup-java` via `java-version-file`, mise/jenv/IDE; `gradle/gradle-daemon-jvm.properties` is generated from it and checked for consistency); the Node.js version lives only in `.nvmrc` (read by nvm, mise, `setup-node` and the Gradle Node plugin convention) and the pnpm version in the `packageManager` field of `package.json`; npm packages use exact versions in `package.json` plus `pnpm-lock.yaml`; GitHub Actions are pinned by SHA in workflows; version literals in build scripts are rejected by an automated check; Renovate covers all of these sources
- [ ] **INFRA-11**: A dedicated `:platform` module (`java-platform`) is the single alignment point for JVM dependencies: it imports all BOMs (Spring Boot, Axoniq/Axon Framework, Testcontainers, Kotlin, Kotest, Jackson, etc.) and declares `api` constraints for libraries not covered by a BOM and for deliberate overrides (e.g. Kotlin over Boot's managed version), referencing only version-catalog aliases; convention plugins apply `platform(project(":platform"))` to every JVM module and every relevant configuration (main, all test suites, test fixtures, load tests)
- [ ] **INFRA-12**: `build-logic` (included build with convention plugins) is first-class code: it uses the same version catalog (plugin dependencies declared as catalog libraries, type-safe catalog access in precompiled script plugins, no version literals), is formatted, statically analysed and KDoc-documented under the same rules and pre-commit/CI checks as application code, its convention plugins have Gradle TestKit functional tests (including configuration-cache compatibility), and it meets the same configuration-cache/isolated-projects and lazy-configuration requirements
- [ ] **INFRA-13**: `.gitattributes` defines repository-wide file handling consistent with `.editorconfig`: `text=auto eol=lf` by default, strict LF for `gradlew` and shell scripts, CRLF for `*.bat`/`*.cmd`, `binary` for images, fonts and jars; `linguist-generated` for generated client code and lock/verification files and `linguist-documentation` for docs so GitHub language stats and PR diffs stay meaningful; `-diff` for machine-generated lock metadata; CI verifies that the working tree is normalised
- [ ] **INFRA-16**: The repository is REUSE-compliant: every source file (Kotlin, Gradle KTS, TS/TSX, CSS, shell, etc.) carries a single `SPDX-License-Identifier: MIT` line (added and checked by Spotless where supported); copyright and licensing for all files, including those that cannot carry headers (JSON, images, generated code), are declared once in `REUSE.toml` (`precedence = "aggregate"`); `reuse lint` runs in pre-commit and CI, and the README shows the REUSE badge
- [ ] **INFRA-17**: Environment variables are defined once per service in a descriptor (name, description, image default, secret flag), from which Gradle generates: image defaults passed to buildpack builds as `BPE_DEFAULT_*` (overridable at runtime), OCI image labels documenting each variable and its default so they are visible via `docker image inspect` (Paketo Image Labels buildpack), `.env.example`, and the configuration reference page; application config keeps local-development defaults (`${VAR:local-default}`); secrets never get image defaults or labels; CI verifies generated artifacts are up to date; whether buildpack env can appear in `Config.Env` directly is verified in Phase 1
- [ ] **INFRA-14**: `.gitignore` comprehensively covers Gradle, Kotlin, Node/pnpm, IDE files, `.env`, test/Playwright/coverage artifacts and local tool state; `.editorconfig` explicitly defines settings for every file type in the repository, and formatter configurations are verified not to conflict with it
- [ ] **INFRA-15**: Shared IDE run configurations are versioned (IntelliJ run configurations for services, compose and test suites; recommended VS Code extensions), while all other IDE state stays ignored
- [ ] **INFRA-10**: The Gradle build uses every applicable speed-up mechanism: configuration cache enabled with problems failing the build; local and CI build cache (`gradle/actions/setup-gradle`, encrypted configuration cache, cache writes from `main` only); parallel execution and file-system watching; isolated-projects-compatible build logic (no cross-project configuration; isolated projects evaluated in Phase 1); convention plugins in an included `build-logic` build (no `buildSrc`); lazy configuration API and cacheable tasks with declared inputs/outputs (including frontend, client generation and docs tasks); Gradle daemon JVM criteria and tuned daemon/Kotlin daemon JVM args; K2 incremental compilation without kapt; parallel test forks and separately parallelised test suites in CI; Testcontainers singletons (and local reuse); reused buildpack caches; build performance tracked with gradle-profiler scenarios; Build Scans optional and only with author approval
- [ ] **INFRA-02**: Shared contracts (commands, events, queries, shared kernel with tag constants and sealed result types) live in dedicated modules with no Spring or framework logic
- [ ] **INFRA-03**: Developer can start Axon Server (pinned version, DCB context created automatically, correct hostname) and PostgreSQL with one docker compose command
- [ ] **INFRA-04**: An automated smoke test proves that a conflicting conditional append is rejected by the DCB event store
- [ ] **INFRA-05**: Axoniq licensing behaviour (unlicensed limits, AxonIQ Platform licence delivery) is verified by a spike and recorded in an ADR, with the outcome reflected in the quick start
- [ ] **INFRA-06**: Kotlin event/tag serialization round-trip (including value-class IDs) is verified and fixed by convention
- [ ] **INFRA-07**: User can start the entire system (all services, UI, infrastructure) with one `docker compose up`, with secrets supplied only via an untracked `.env` (template `.env.example` committed)
- [ ] **INFRA-08**: Frontend (and the documentation site) are Gradle subprojects: pinned Node.js/pnpm managed by the Gradle Node plugin, pnpm install/build/test/lint wired into `check`/`build` with declared inputs/outputs, API client generation depending on the BFF OpenAPI spec task, and frontend image build exposed as a Gradle task; `./gradlew build` verifies the whole system while `pnpm dev` remains available for local UI work

### Courses

- [ ] **COURSE-01**: Admin can create a course with a name and capacity
- [ ] **COURSE-02**: Course creation is rejected if another course already has the same (normalised) name
- [ ] **COURSE-03**: Admin can rename a course; the old name is released and can be reused; renaming to the course's own name (including case-only changes) is handled by owner check
- [ ] **COURSE-04**: Renaming a course never conflicts with concurrent subscriptions (rename is outside the subscribe boundary)
- [ ] **COURSE-05**: Admin can change course capacity; a change to the same value or below the current number of subscribed students is rejected
- [ ] **COURSE-06**: Admin can assign a course schedule as slots on a fixed weekly grid; the schedule can be changed only while the course has no subscribed students

### Students

- [ ] **STUD-01**: Admin can register a student
- [ ] **STUD-02**: Commands referencing a non-existent student or course are rejected with a clear reason

### Subscriptions

- [ ] **SUBS-01**: Student can subscribe to a course with a free seat
- [ ] **SUBS-02**: Subscription is rejected if the student is already subscribed to that course
- [ ] **SUBS-03**: Subscription is rejected if the student is already subscribed to 3 courses
- [ ] **SUBS-04**: Subscription is rejected if the course's schedule overlaps a course the student is already subscribed to
- [ ] **SUBS-05**: Course capacity is never exceeded, even under many concurrent subscription attempts for the last seat
- [ ] **SUBS-06**: Student can unsubscribe from a course they are subscribed to
- [ ] **SUBS-07**: Append conflicts are retried transparently with a bounded policy; business rejections are returned as typed (sealed) results with stable error codes, never as technical errors

### Transfer

- [ ] **XFER-01**: Student can atomically transfer from one course to another in a single decision spanning the student and both courses
- [ ] **XFER-02**: Transfer is rejected if the target course is full (no automatic waitlisting), if the student is not subscribed to the source course, or is already subscribed to the target
- [ ] **XFER-03**: Transfer is rejected if the target course's schedule overlaps the student's other courses (excluding the source course)
- [ ] **XFER-04**: A student at the course limit can still transfer

### Waiting List

- [ ] **WAIT-01**: Subscribing to a full course places the student on the course's waiting list instead of rejecting
- [ ] **WAIT-02**: While a course's waiting list is non-empty, a direct subscription joins the queue instead of taking a freed seat (no barging); order is FIFO
- [ ] **WAIT-03**: When a seat is freed (unsubscribe, transfer-out, capacity increase), the next eligible student on the waiting list is automatically subscribed
- [ ] **WAIT-04**: A waiting-list head who is no longer eligible (limit reached, schedule overlap) is removed from the list with an explicit reason and promotion moves on to the next student
- [ ] **WAIT-05**: Student can withdraw from a course, whether subscribed or waitlisted, with one command that avoids races with concurrent promotion
- [ ] **WAIT-06**: Automated promotion is idempotent: duplicate event delivery or processor reset never causes double enrolment or exceeds capacity

### Read Side (CQRS)

- [ ] **READ-01**: Command side (DCB decisions over the event store) and query side (projections into dedicated read models in PostgreSQL) are separate services; read models are never used to enforce invariants
- [ ] **READ-02**: User can view the course catalogue with name, capacity, free seats, schedule and waiting-list length
- [ ] **READ-03**: User can view a course's detail with its subscribed students and waiting list
- [ ] **READ-04**: Student can view their subscriptions, timetable and waiting-list positions
- [ ] **READ-05**: Admin can view the student directory
- [ ] **READ-06**: Projections can be rebuilt by replay with a documented procedure, without emitting live updates during replay

### Idempotency

- [ ] **IDEM-01**: Every state-changing API call accepts an idempotency key (`Idempotency-Key`); retrying the same command returns the original outcome without appending duplicate events, enforced inside the DCB decision (key tag in the consistency boundary) rather than a separate dedup table
- [ ] **IDEM-02**: Entity IDs (courses, students) are client-generated so that create commands are safely repeatable
- [ ] **IDEM-04**: Entity IDs use the TypeID format (type prefix + UUIDv7 in Crockford base32, e.g. `course_01jdq8x6v2f3k9m4r7t5w0y1zb`), modelled as Kotlin value classes with validation of prefix and format; the canonical TypeID string is the only representation used in DCB tags, events, API and UI (never `toString()` of a wrapper); read models store IDs as native PostgreSQL `uuid` with the prefix implied by the column (any text storage uses `COLLATE "C"`); Kotlin and TypeScript generators come from vetted libraries (maturity verified in Phase 1), and validation rejects malformed or wrong-type IDs at the API boundary
- [ ] **IDEM-03**: Projections are idempotent under at-least-once delivery and replay (token and projection updated in one transaction, upsert semantics)

### Resilience

- [ ] **RES-01**: Services expose health/readiness, compose uses healthchecks and dependency ordering, and services reconnect automatically after an Axon Server restart
- [ ] **RES-07**: Health checks work on minimal images without a shell: Spring Boot Actuator liveness/readiness groups (readiness covers Axon Server connectivity, PostgreSQL where used, and event processor state) on a separate, unpublished management port; application images include the Paketo Health Checker buildpack and compose runs its `health-check` process; the frontend serves a static health endpoint checked the same way; infrastructure containers use native checks (e.g. `pg_isready`) or a documented alternative/upstream issue where the image lacks one; compose uses `depends_on: service_healthy`, `start_period`/`start_interval` and restart policies; the same readiness endpoints drive Testcontainers wait strategies and E2E startup
- [ ] **RES-02**: Event processor failures follow an explicit, documented error policy (retry with backoff, no silent skipping, failures visible in metrics) recorded in an ADR, without commercial dead-letter queues
- [ ] **RES-03**: Command dispatch uses timeouts and bounded retries for transient failures; the BFF maps unavailability to `503` with `Retry-After`
- [ ] **RES-04**: Services shut down gracefully: in-flight commands complete and event processors release their tokens
- [ ] **RES-05**: Resilience tests inject failures (Axon Server restart, PostgreSQL outage, network latency via Toxiproxy) and verify recovery with invariants intact
- [ ] **RES-06**: UI recovers from disconnects: SSE auto-reconnects and resynchronises, offline/error states are clear, and command retries reuse the same idempotency key

### API & Real-time

- [ ] **API-01**: All operations are exposed via a REST API in a BFF service, documented by an OpenAPI specification checked for drift in CI
- [ ] **API-02**: Errors are returned as problem+json with stable error codes and human-readable messages
- [ ] **API-03**: Client receives live updates of read models over SSE backed by subscription queries (snapshot on connect, then deltas; heartbeats; cleanup on disconnect)
- [ ] **API-04**: Services communicate only via Axon Server (commands, events, queries); no service-to-service HTTP calls

### UI

- [ ] **UI-01**: User can perform all business operations from a modern, polished React + TypeScript UI using a generated typed API client
- [ ] **UI-02**: UI views update in real time without manual refresh
- [ ] **UI-03**: User can switch demo role (admin / a specific student) without real authentication
- [ ] **UI-04**: Rejections are shown with a human-readable explanation: which rule fired and which events in the consistency boundary caused it
- [ ] **UI-05**: UI shows when a command hit a concurrency conflict and was retried
- [ ] **UI-06**: Frontend code is organised by Feature-Sliced Design (FSD) layers, with layer and import boundaries enforced by Steiger in CI
- [ ] **UI-07**: UI is built on a deliberate design system (shadcn/ui + Tailwind theme, typography, colour, spacing) with consistent loading, empty, error and offline states; each UI-bearing phase has a design contract (UI-SPEC) and a UI review
- [ ] **UI-08**: Storybook catalogues shared, entity and widget components with their states; interaction and accessibility checks run from stories, and the Storybook is published alongside the documentation site
- [ ] **UI-09**: UI meets WCAG 2.2 AA: automated axe checks in component tests and Playwright E2E, full keyboard navigation and visible focus

### DCB Teaching Tools

- [ ] **DEMO-01**: Each command's decision record (criteria, consistency marker, appended events, outcome, retries) is captured at append time
- [ ] **DEMO-02**: User can browse the event log with tags and filter by tag and event type
- [ ] **DEMO-03**: User can open any command's "consistency boundary" view showing which events formed its decision
- [ ] **DEMO-04**: User can run a concurrency race (many students for the last seat) and see the outcome per participant and that the invariant held
- [ ] **DEMO-05**: User can seed demo scenarios (through commands only) and reset the demo context (demo profile only)
- [ ] **DEMO-06**: UI offers guided "try this" scenario cards linked to the relevant docs and tests

### Testing & Quality

- [ ] **TEST-01**: Every DCB decision has given-when-then unit tests, including assertions on appended event tags and narrow-boundary no-conflict tests
- [ ] **TEST-02**: A reflection test ensures every event class carries the expected tags
- [ ] **TEST-03**: Each service has component tests
- [ ] **TEST-04**: Integration tests run services against real Axon Server and PostgreSQL via Testcontainers, including an N-thread capacity invariant test
- [ ] **TEST-05**: End-to-end tests drive the UI in a browser (Playwright) against the composed system built from release images
- [ ] **TEST-06**: Architecture tests enforce module and slice boundaries
- [ ] **TEST-07**: A load test of concurrent subscriptions asserts that the invariant holds after the run
- [ ] **TEST-08**: Backend code coverage is measured with Kover as one aggregated report across all modules and all test suites (unit, component, integration), with line and branch coverage, layered thresholds (high for DCB decision and automation logic, moderate overall) and explicit, justified exclusions (generated code, configuration, entry points)
- [ ] **TEST-16**: Frontend code coverage is measured with Vitest (v8 provider) with thresholds per FSD layer (higher for `shared`/`entities`/`features`, lower for `app`/`pages`) and the generated API client excluded
- [ ] **TEST-17**: Coverage is reported for backend and frontend as CI artifacts (HTML plus XML/LCOV), in the job summary and as a PR comment, with a self-generated README badge (no external coverage SaaS without the author's approval); thresholds only ratchet upwards unless deliberately changed
- [ ] **TEST-09**: All produced artifacts (Kotlin, Gradle KTS, TS/TSX, JSON, YAML, Markdown) are automatically formatted by one command and checked for style compliance, with a shared `.editorconfig`, a pre-commit hook formatting changed files, CI failing on violations, and generated code excluded
- [ ] **TEST-10**: Code is idiomatic Kotlin with KDoc on public contracts and decision logic; KDoc is auto-formatted (formatter with KDoc support, e.g. ktfmt, instead of ktlint), its presence and quality on public API are enforced by static analysis (detekt `comments` rules), and broken KDoc references fail the build (Dokka `failOnWarning`)
- [ ] **TEST-18**: Mapping between layers (API ↔ commands/queries, query results ↔ DTOs) is done by explicit Kotlin extension functions next to the layer boundary (`toCommand`, `toDto`, `toView`), without mapping generators; naming and placement are enforced by architecture tests
- [ ] **TEST-11**: JVM test levels (unit, component, integration) are separate Gradle JVM Test Suites with their own dependencies and tasks, all included in coverage
- [ ] **TEST-12**: Shared test data builders and test helpers are published via Gradle Java Test Fixtures (`testFixtures`) instead of being duplicated across modules
- [ ] **TEST-13**: Static analysis runs for Kotlin (detekt or equivalent) and TypeScript (Biome) with the build failing on violations
- [ ] **TEST-14**: Test toolchain is standardised: JUnit 6 (Jupiter) as the only JVM runner, Kotest Assertions for assertions, `kotest-property` for property-based invariant tests, AxonTestFixture for DCB decisions, MockK (minimal mocking), Awaitility for async, Testcontainers for infrastructure; frontend uses Vitest, Testing Library and MSW
- [ ] **TEST-15**: Tests read as a business specification: Kotlin test methods and `@Nested` classes use backticked prose names in business language (no JVM-forbidden characters; prose names only in test sources, enforced by static analysis/Konsist); tests are tagged with the business rule ID they prove (`@Tag`) to feed the rule → test catalogue

### CI/CD & Supply Chain

- [ ] **CICD-01**: GitHub Actions build, run all test levels, report coverage and static analysis on every push and PR
- [ ] **CICD-02**: Container images for all services and the UI are built with Cloud Native Buildpacks (Paketo): Spring Boot `bootBuildImage` for JVM services, Paketo Web Servers buildpack (nginx with custom config: SPA routing, `/api` proxy to the BFF with SSE-safe buffering) for the frontend; no hand-written Dockerfiles for application images
- [ ] **CICD-10**: Image build tasks always set fully qualified image names from one convention-plugin pattern `ghcr.io/<owner>/<rootProject.name>-<project.name>:<project.version>` (registry and owner configurable via `gradle.properties` for forks; version from the release-please-managed version file, SNAPSHOT between releases); CI adds a short git SHA tag for `main` builds and `latest` only for releases; signing and attestation target the digest; images carry standard OCI labels (`source`, `version`, `revision`, `created`, `licenses`, `title`, `description`); docker compose references the same names via the generated env file
- [ ] **CICD-03**: All Actions and base images are pinned by SHA/digest
- [ ] **CICD-04**: Dependency and tool version updates are tracked automatically (Renovate) with grouping and minimum release age
- [ ] **CICD-05**: Dependencies and built images are scanned for vulnerabilities, failing on fixable high-severity issues
- [ ] **CICD-06**: Secret leaks are detected in the repository (full history) and in PRs
- [ ] **CICD-07**: SBOMs are generated and images are signed with provenance attestations
- [ ] **CICD-08**: Workflows and scripts are linted locally (pre-commit) and in CI: actionlint for GitHub Actions workflows (with shellcheck for `run:` blocks), zizmor for Actions security issues (template injection, excessive permissions, unpinned actions, cache poisoning, dangerous triggers), shellcheck for all shell scripts, and `docker compose config` validation for compose files; tool versions pinned in `mise.toml`
- [ ] **CICD-09**: Dependency licences (JVM and npm) are reported and checked in CI against an allowlist, with Axoniq commercial-terms modules explicitly listed and justified; a NOTICE file lists third-party attributions and the Axoniq terms

### Observability

- [ ] **OBS-01**: Services export metrics, traces and logs via OpenTelemetry to a Grafana stack started by docker compose
- [ ] **OBS-02**: A single request can be traced end to end from the BFF through command handling to projections
- [ ] **OBS-03**: Services can optionally connect to AxonIQ Platform via a token supplied in `.env`; the system starts normally without it

### Documentation

- [ ] **DOC-01**: Presentable README with the "why DCB" pitch, architecture overview, screenshots/diagrams, quick start (verified in CI) and an honest licensing note
- [ ] **DOC-02**: Documentation site explaining DCB concepts as applied in this project
- [ ] **DOC-03**: ADRs record every significant decision
- [ ] **DOC-04**: C4 diagrams as code (context, container, component)
- [ ] **DOC-05**: Event Modeling artifacts describe every slice
- [ ] **DOC-06**: Consistency boundary table: for each command, which tags and event types form its boundary and why
- [ ] **DOC-07**: Comparison with the Axon 4 aggregate approach for each scenario (diagrams and snippets, documentation only)
- [ ] **DOC-08**: Axon 4 → Axon 5/DCB migration guide (concept mapping and traps)
- [ ] **DOC-09**: "When not to use DCB" page
- [ ] **DOC-10**: Business rule → test traceability catalogue
- [ ] **DOC-11**: "How this was built" section describing the AI-assisted process, curated from `.planning/`
- [ ] **DOC-12**: "Sagas → automation slices → Workflows" page: why DCB removes the need for sagas for cross-entity invariants (e.g. atomic transfer instead of a compensating saga), when an automation slice is enough (waiting list), and when a long-running process genuinely needs Axoniq Workflows (with their licensing/preview status)

### GitHub Repository Presentation

- [ ] **GH-01**: Repository metadata is complete and managed as code (applied via `gh` after author approval): description, topics, homepage (docs site), social preview image, unused features (e.g. wiki) disabled
- [ ] **GH-02**: README showcases the project with badges (CI, coverage, license, OpenSSF Scorecard, Axon/Kotlin/Spring Boot versions) and a UI screenshot or animation
- [ ] **GH-03**: Community health files exist: CONTRIBUTING, CODE_OF_CONDUCT, SUPPORT, CODEOWNERS, and SECURITY.md stating supported versions (latest release and `main`), reporting only via private vulnerability reporting, realistic response timelines (acknowledgement, assessment, fix, disclosure), scope (non-production teaching reference, no authentication in v1; dependency vulnerabilities routed upstream) and a safe-harbour statement
- [ ] **GH-08**: A single PR template matches the structure of the `/gsd-ship` PR body (summary, user stories and acceptance criteria, risks and dependencies, verification, checklist), so GSD-generated and manual PRs (Renovate, contributors) look the same
- [ ] **GH-09**: YAML issue forms exist for bug report (mandatory reproduction and versions), feature request and documentation feedback; blank issues are disabled and `config.yml` routes questions to Discussions and vulnerabilities to private vulnerability reporting
- [ ] **GH-10**: GitHub Discussions is enabled with categories (Q&A, Ideas, Show and tell) and discussion templates
- [ ] **GH-11**: Labels are managed as code (`labels.yml` synced by CI), aligned with Conventional Commit types, release-please, Renovate and the upstream register
- [ ] **GH-12**: In-repository templates exist for ADRs (MADR), documentation pages, Event Modeling slice descriptions, upstream records (`docs/upstream/`) and upstream issues (minimal reproduction), and are used by the project skills
- [ ] **GH-13**: `CITATION.cff` enables GitHub's "Cite this repository" for references from articles and talks
- [ ] **GH-04**: Documentation site is published to GitHub Pages automatically from CI
- [ ] **GH-05**: Commits and PR titles follow Conventional Commits (checked locally by the pre-commit tooling and in CI); release-please exclusively owns version tags (`vX.Y.Z`), changelogs and GitHub Releases (GSD milestone tagging disabled); release images are published to GHCR, signed and attested
- [ ] **GH-06**: Repository security settings are enabled: `main` protected by a ruleset (changes only via PR, required status checks, linear history, no force-push or deletion, 0 required approvals for the solo author, bypass for admins only), secret scanning with push protection, Dependabot alerts, private vulnerability reporting, CodeQL, OpenSSF Scorecard workflow
- [ ] **GH-07**: Repository is configured for the GSD workflow: one branch per phase (`gsd/phase-{N}-{slug}`), each phase shipped via `/gsd-ship` as a PR; only squash merge is enabled, using the PR title as the commit title and the PR body (GSD planning summary) as the commit message; head branches are deleted after merge

### Upstream-first

- [ ] **UPS-01**: Each upstream case (defect, gap or documentation issue in a used product) has its own record `docs/upstream/NNNN-<slug>.md` with machine-readable front matter (ID `UP-NNNN`, product and version, status `open` → `reported` → `fixed-upstream` → `workaround-removed` / `wont-fix`, upstream issue/PR links, dates) and a full body: symptom, minimal reproduction, root-cause analysis, workaround and all its code locations, removal criteria and how to verify them
- [ ] **UPS-02**: Workarounds are marked in code/config by record ID (`UPSTREAM: UP-NNNN`), and a CI check enforces consistency in both directions (every marker references an existing record; open records with a workaround have markers; closed records have none); the index with statuses is generated for the documentation site
- [ ] **UPS-03**: Defects and documentation gaps found in used products are reported upstream with a minimal reproduction (issue or PR), preferring upstream fixes over local workarounds; drafts are prepared by the AI and published only after the author's explicit approval
- [ ] **UPS-04**: Dependency updates include a check whether recorded workarounds can be removed according to their removal criteria

### Developer Tooling

- [ ] **TOOL-01**: All developer and CI tools are inventoried in `docs/tooling.md` (purpose, version source, installation): git, gh, Docker-compatible engine (colima/OrbStack/Docker Desktop), docker compose, pack, JDK, Node.js/pnpm, lefthook, gitleaks, trivy, grype, syft, cosign, Structurizr, act, etc.
- [ ] **TOOL-02**: CLI tools not managed by Gradle are pinned in `mise.toml` and kept up to date by Renovate
- [ ] **TOOL-03**: A `doctor` command verifies the local environment (required tools and versions, Docker engine, Testcontainers connectivity) and explains how to fix problems
- [ ] **TOOL-04**: The setup works with colima (Testcontainers `DOCKER_HOST`/Ryuk configuration documented and verified) as well as Docker Desktop

### AI Development Tooling

- [ ] **AIDEV-01**: Claude Code skills for the project's stack are surveyed (Axon, Kotlin, Gradle, Spring Boot, Playwright, React/shadcn, FSD, GitHub Actions, Paketo), and each candidate is reviewed before installation; third-party skills are installed as plugins from their marketplaces (nothing copied into `.claude/`), with pinned source/version, only with the author's approval, and documented in the tooling inventory
- [ ] **AIDEV-02**: Project-specific skills encode the project's conventions (e.g. add a DCB command slice, add a projection, write an ADR, draft an upstream issue) and live in a repository-local Claude Code plugin outside `.claude/` (e.g. `tooling/claude-plugin/` with its own marketplace file), so they never mix with GSD-managed files
- [ ] **AIDEV-03**: `.claude/` is tracked selectively: only `.claude/settings.json` (marketplace and plugin registration) and `.claude/CLAUDE.md` are committed; the GSD installation and `settings.local.json` are ignored, and the GSD version and install command are recorded in the tooling inventory and checked by `doctor`
- [ ] **AIDEV-04**: Shared MCP servers (Context7 for up-to-date library documentation, Playwright for browser-driven UI verification) are configured in the project-scope `.mcp.json` at the repository root with pinned versions and no secrets (environment variable references only); personal MCP servers (IntelliJ, optionally GitHub) are configured in local scope and documented with setup instructions; all MCP servers are listed in the tooling inventory, checked by `doctor`, and added only with the author's approval
- [ ] **AIDEV-05**: GSD project settings are enforced as policy: a CI test validates `.planning/config.json` against an explicit policy (e.g. branching strategy, tagging, commit_docs, runtime, no foreign model overrides) and fails on any deviation until the policy itself is deliberately changed
- [ ] **AIDEV-06**: Changes to governance files (`.planning/config.json`, `.claude/settings.json`, `.mcp.json`, `.github/**`, `mise.toml`, Renovate config, CODEOWNERS) fail a CI governance check unless the PR carries a `governance-approved` label applied by a maintainer
- [ ] **AIDEV-07**: GSD installation integrity is protected: GSD is installed at a pinned version (never `@latest`), `doctor` verifies installed files against the SHA-256 hashes in `gsd-file-manifest.json` and reports modified hooks or unexpected files, `doctor` warns about global `~/.gsd/defaults.json` keys that leak into projects, and GSD updates happen only deliberately via `/gsd-update` with changelog and hook diff review
- [ ] **AIDEV-08**: Claude Code permission rules in `.claude/settings.json` require explicit confirmation (`ask`) for edits to governance files and for GSD `config-set` calls, so the agent cannot silently change project settings

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Security

- **AUTH-01**: User authenticates via OAuth2/OIDC (e.g. Keycloak)
- **AUTH-02**: Admin and student roles are enforced on the API

### Domain

- **DOM-01**: Admin can archive a course
- **DOM-02**: Limits (per-student course limit) are configurable at runtime as events
- **DOM-03**: Waiting list has a maximum length
- **DOM-04**: Released course names have a retention window before reuse
- **DOM-05**: Seat offer with acceptance deadline: a freed seat is offered to the waiting-list head, who must accept within a time limit before it passes on — implemented with Axoniq Workflows once they are GA and licence-compatible with this reference

### Platform

- **PLAT-01**: Event upcasting/versioning machinery
- **PLAT-02**: Horizontal scale-out of the query service with correct subscription-query delivery
- **PLAT-03**: Kubernetes deployment manifests

### UI

- **UIX-01**: Dark mode
- **UIX-02**: Internationalisation
- **UIX-03**: Projection-lag dashboard

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Working aggregate-based implementation | Comparison lives in docs only; a parallel implementation would roughly double the effort |
| Splitting the write side by entity | Contradicts DCB: invariants span courses and students |
| Sagas with compensation | DCB makes multi-entity decisions atomic; automation slices replace sagas |
| Enforcing invariants from read models or in the UI | Read models are eventually consistent; invariants belong to DCB decisions |
| "Invariants off" toggle | Would teach the wrong lesson and complicate the code |
| Seeding by direct event insertion | Bypasses decisions; demo data must go through commands |
| Arbitrary-interval schedules | Fixed slot grid keeps DCB criteria static; alternative recorded in an ADR |
| Commercial Axoniq extras (Workflows, dead-letter, Postgres DCB store, legacy sagas) | Not needed; keep the reference as open as possible |
| Snapshots | Decision boundaries are small; snapshots add noise to the lesson |
| Generic framework/abstraction layer over Axon | Obscures the DCB lesson |
| Mapping generators (MapStruct, Konvert) | MapStruct needs kapt (excluded for build speed/K2); mappings are thin and better explicit; projections are logic, not mapping |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| INFRA-01 | Phase 1 | Complete |
| INFRA-09 | Phase 1 | Pending |
| INFRA-11 | Phase 1 | Pending |
| INFRA-12 | Phase 1 | Pending |
| INFRA-13 | Phase 1 | Pending |
| INFRA-16 | Phase 1 | Pending |
| INFRA-17 | Phase 6 | Pending |
| INFRA-14 | Phase 1 | Pending |
| INFRA-15 | Phase 6 | Pending |
| INFRA-10 | Phase 1 | Pending |
| INFRA-02 | Phase 2 | Pending |
| INFRA-03 | Phase 2 | Pending |
| INFRA-04 | Phase 2 | Pending |
| INFRA-05 | Phase 2 | Pending |
| INFRA-06 | Phase 2 | Pending |
| INFRA-07 | Phase 6 | Pending |
| INFRA-08 | Phase 5 | Pending |
| COURSE-01 | Phase 3 | Pending |
| COURSE-02 | Phase 3 | Pending |
| COURSE-03 | Phase 8 | Pending |
| COURSE-04 | Phase 8 | Pending |
| COURSE-05 | Phase 8 | Pending |
| COURSE-06 | Phase 8 | Pending |
| STUD-01 | Phase 3 | Pending |
| STUD-02 | Phase 3 | Pending |
| SUBS-01 | Phase 3 | Pending |
| SUBS-02 | Phase 3 | Pending |
| SUBS-03 | Phase 3 | Pending |
| SUBS-04 | Phase 8 | Pending |
| SUBS-05 | Phase 3 | Pending |
| SUBS-06 | Phase 3 | Pending |
| SUBS-07 | Phase 3 | Pending |
| XFER-01 | Phase 9 | Pending |
| XFER-02 | Phase 9 | Pending |
| XFER-03 | Phase 9 | Pending |
| XFER-04 | Phase 9 | Pending |
| WAIT-01 | Phase 10 | Pending |
| WAIT-02 | Phase 10 | Pending |
| WAIT-03 | Phase 10 | Pending |
| WAIT-04 | Phase 10 | Pending |
| WAIT-05 | Phase 10 | Pending |
| WAIT-06 | Phase 10 | Pending |
| READ-01 | Phase 4 | Pending |
| READ-02 | Phase 4 | Pending |
| READ-03 | Phase 4 | Pending |
| READ-04 | Phase 4 | Pending |
| READ-05 | Phase 4 | Pending |
| READ-06 | Phase 4 | Pending |
| IDEM-01 | Phase 3 | Pending |
| IDEM-02 | Phase 3 | Pending |
| IDEM-04 | Phase 2 | Pending |
| IDEM-03 | Phase 4 | Pending |
| RES-01 | Phase 6 | Pending |
| RES-07 | Phase 6 | Pending |
| RES-02 | Phase 4 | Pending |
| RES-03 | Phase 5 | Pending |
| RES-04 | Phase 6 | Pending |
| RES-05 | Phase 12 | Pending |
| RES-06 | Phase 7 | Pending |
| API-01 | Phase 5 | Pending |
| API-02 | Phase 5 | Pending |
| API-03 | Phase 5 | Pending |
| API-04 | Phase 5 | Pending |
| UI-01 | Phase 13 | Pending |
| UI-02 | Phase 7 | Pending |
| UI-03 | Phase 7 | Pending |
| UI-04 | Phase 11 | Pending |
| UI-05 | Phase 11 | Pending |
| UI-06 | Phase 7 | Pending |
| UI-07 | Phase 7 | Pending |
| UI-08 | Phase 7 | Pending |
| UI-09 | Phase 7 | Pending |
| DEMO-01 | Phase 3 | Pending |
| DEMO-02 | Phase 11 | Pending |
| DEMO-03 | Phase 11 | Pending |
| DEMO-04 | Phase 11 | Pending |
| DEMO-05 | Phase 11 | Pending |
| DEMO-06 | Phase 11 | Pending |
| TEST-01 | Phase 3 | Pending |
| TEST-02 | Phase 3 | Pending |
| TEST-03 | Phase 10 | Pending |
| TEST-04 | Phase 4 | Pending |
| TEST-05 | Phase 6 | Pending |
| TEST-06 | Phase 3 | Pending |
| TEST-07 | Phase 12 | Pending |
| TEST-08 | Phase 3 | Pending |
| TEST-16 | Phase 7 | Pending |
| TEST-17 | Phase 7 | Pending |
| TEST-09 | Phase 1 | Pending |
| TEST-10 | Phase 1 | Pending |
| TEST-18 | Phase 5 | Pending |
| TEST-11 | Phase 2 | Pending |
| TEST-12 | Phase 2 | Pending |
| TEST-13 | Phase 1 | Pending |
| TEST-14 | Phase 2 | Pending |
| TEST-15 | Phase 3 | Pending |
| CICD-01 | Phase 1 | Pending |
| CICD-02 | Phase 6 | Pending |
| CICD-10 | Phase 6 | Pending |
| CICD-03 | Phase 1 | Pending |
| CICD-04 | Phase 1 | Pending |
| CICD-05 | Phase 6 | Pending |
| CICD-06 | Phase 1 | Pending |
| CICD-07 | Phase 12 | Pending |
| CICD-08 | Phase 1 | Pending |
| CICD-09 | Phase 2 | Pending |
| OBS-01 | Phase 12 | Pending |
| OBS-02 | Phase 12 | Pending |
| OBS-03 | Phase 12 | Pending |
| DOC-01 | Phase 13 | Pending |
| DOC-02 | Phase 13 | Pending |
| DOC-03 | Phase 2 | Pending |
| DOC-04 | Phase 13 | Pending |
| DOC-05 | Phase 3 | Pending |
| DOC-06 | Phase 11 | Pending |
| DOC-07 | Phase 13 | Pending |
| DOC-08 | Phase 13 | Pending |
| DOC-09 | Phase 13 | Pending |
| DOC-10 | Phase 11 | Pending |
| DOC-11 | Phase 13 | Pending |
| DOC-12 | Phase 10 | Pending |
| GH-01 | Phase 13 | Pending |
| GH-02 | Phase 13 | Pending |
| GH-03 | Phase 13 | Pending |
| GH-08 | Phase 1 | Pending |
| GH-09 | Phase 13 | Pending |
| GH-10 | Phase 13 | Pending |
| GH-11 | Phase 1 | Pending |
| GH-12 | Phase 2 | Pending |
| GH-13 | Phase 13 | Pending |
| GH-04 | Phase 7 | Pending |
| GH-05 | Phase 12 | Pending |
| GH-06 | Phase 1 | Pending |
| GH-07 | Phase 1 | Pending |
| UPS-01 | Phase 2 | Pending |
| UPS-02 | Phase 2 | Pending |
| UPS-03 | Phase 2 | Pending |
| UPS-04 | Phase 2 | Pending |
| TOOL-01 | Phase 1 | Pending |
| TOOL-02 | Phase 1 | Pending |
| TOOL-03 | Phase 2 | Pending |
| TOOL-04 | Phase 2 | Pending |
| AIDEV-01 | Phase 2 | Pending |
| AIDEV-02 | Phase 8 | Pending |
| AIDEV-03 | Phase 2 | Pending |
| AIDEV-04 | Phase 2 | Pending |
| AIDEV-05 | Phase 1 | Pending |
| AIDEV-06 | Phase 1 | Pending |
| AIDEV-07 | Phase 2 | Pending |
| AIDEV-08 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 150 total
- Mapped to phases: 150
- Unmapped: 0 ✓

---
*Requirements defined: 2026-11-20*
*Last updated: 2026-11-21 after roadmap creation (traceability filled)*
