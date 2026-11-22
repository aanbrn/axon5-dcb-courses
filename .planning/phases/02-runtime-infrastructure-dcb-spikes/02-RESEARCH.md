# Phase 2: Runtime Infrastructure & DCB Spikes - Research

**Researched:** 2026-11-22
**Domain:** Gradle multi-module JVM test infrastructure, Axon Server 2026.1.8 (DCB) in compose and Testcontainers, Kotlin contracts and TypeID, repository governance tooling (licences, ADRs, upstream records, `doctor`, AI tooling)
**Confidence:** HIGH for everything exercised in this session (prototype builds, SBOM run, compose runs, mise run, Konsist sources) and for the five spikes; MEDIUM for the skills survey and the Renovate behaviour of new managers (not run)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Repository Layout & Contracts**
- **D-01 (amends Phase 1 D-03):** Backend code is grouped under `backend/`:
  - `backend/contracts/{kernel,commands,events,queries}`;
  - `backend/command-service`, `backend/query-service`, `backend/automation-service` and `backend/bff`, as flat service directories whose names match compose service and image names.

  `platform/`, `build-logic/`, `frontend/`, `docs-site/` and `tooling/` stay at the top level. Gradle project paths may be shortened via `projectDir` (planner's choice). Update the `settings.gradle.kts` layout comment and any path references, such as CI filters and REUSE. — **Reversibility:** costly — paths are referenced from CI, Renovate, REUSE, IDE run configurations and docs. This is the cheapest point to change, since only `platform/` and `tooling/` exist.
- **D-02:** Contracts are split by message kind into four modules: kernel (IDs, tag registry, shared `Rejection` and result base types), commands, events and queries. The dependency graph shows the CQRS boundaries: the query service depends on events and queries, and the BFF on commands and queries but not on events.
- **D-03:** Every message type has an explicit, stable type name and version through the standard AF5 mechanism (`@Namespace` / `@Event` and the equivalents for commands and queries):
  - a short namespace such as `courses` replaces the package-derived default;
  - `name` is the class simple name;
  - the version is explicit (not the implicit `0.0.1` default).

  A Konsist rule requires an explicit name and version on every message. Research must confirm how AF5 5.4.2 stores the qualified name: the 5.1 docs mention the `payloadType` column being renamed to `type`. — **Reversibility:** one-way after Phase 3 — the qualified names and versions are persisted in Axon Server with the first event, and changing them later needs upcasters or a new store.
- **D-04:** Contracts may carry Axon annotations as metadata only, with Axon as a `compileOnly` dependency so the contracts have no runtime framework dependency. A Konsist rule forbids any other Axon or Spring import in the contracts modules.
- **D-05:** Command results are a sealed hierarchy per command, for example `SubscribeResult = Subscribed | Rejected(code)`. The kernel provides a shared `Rejection` base and the stable rejection-code contract. Whether Axon serializes sealed hierarchies across the wire is checked in the serialization spike.

**TypeID & Serialization**
- **D-06:** An ID value class wraps `java.util.UUID` (for example `value class CourseId(val uuid: UUID)`), and each type fixes its prefix. Serialization and tags produce the canonical TypeID string, and `toString()` is overridden to return it. This maps directly to the native `uuid` columns and keeps UUIDv7 ordering. — **Reversibility:** costly — every contract, serializer and read-model mapping uses the shape. The serialized form (the canonical string) is fixed by IDEM-04 either way.
- **D-07:** Research looks for a mature JVM TypeID library. If there is none, the project implements Crockford base32 encoding itself and gets UUIDv7 from a vetted library or the JDK. The implementation is verified against the official TypeID spec's valid and invalid test vectors and by property tests. Record this in an ADR and clarify the "vetted libraries" wording of IDEM-04 accordingly.
- **D-08:** Serialization uses Jackson 3 with the Kotlin module through the AF5 converter (`axon.converter.general=jackson`). One Jackson module holds the value-class (de)serializers and is shared with the BFF REST layer later. kotlinx.serialization was rejected.
- **D-09:** The parse API is `parse(s)`, which throws `IllegalArgumentException` with a clear wrong-prefix or malformed message, plus `parseOrNull(s)`, mirroring the stdlib `toInt` and `toIntOrNull`.
- **D-10:** The tag registry is `object Tags` in the kernel with `const val` keys, because annotations need compile-time constants. An entity tag key equals its TypeID prefix (`course=course_01…`). Service tags, such as a normalised course name for uniqueness, are in the registry too. A Konsist rule requires every `@EventTag` key to come from the registry.
- **D-11:** Test IDs come from three sources:
  - readable fixed IDs from `testFixtures` for given-when-then tests (stable failure messages);
  - kotest `Arb` generators for property tests;
  - a UUIDv7 generator that takes a `Clock`, so tests that depend on ordering are deterministic.

  Production IDs are client-generated (IDEM-04). On the JVM, generation serves tests and demo data.

**Spikes**
- **D-12:** Spikes run through `/gsd-spike` **between this discussion and `/gsd-plan-phase 2`**, with exploratory code in `.planning/spikes/`, so the planner plans against known answers. Outcomes are recorded in MADR ADRs during phase execution. Spike proofs that are requirements themselves move into real modules as permanent tests: the conditional-append conflict test (INFRA-04), the serialization round-trip (INFRA-06) and the decision-record capture test. Spike topics:
  - decision-record capture (D-13);
  - Kotlin tags and the Jackson round-trip, including sealed results over Axon;
  - Paketo static image and environment defaults (D-35);
  - Axon Server licence restart behaviour and token delivery (D-14);
  - DCB context creation and hostname in compose.
- **D-13:** Decision-record capture (DEMO-01: criteria, consistency marker, appended events, outcome, retries) uses a **central decorator** around the Axon layer that performs the conditional append, so slices contain no capture code. If the spike confirms that this needs an `@Internal` Axon API, accept it with two safeguards:
  - a capture test that fails loudly in the (manually merged) Renovate PR when an Axon update breaks it;
  - an upstream record asking AxonIQ for a public extension point.

  Explicit per-slice stamping through a shared helper plus a Konsist rule is documented in the ADR as the fallback.
- **D-14:** The author has verified the Axon Server licence behaviour: unlicensed Axon Server in compose stops working after about 12 hours, and with an AxonIQ Platform token it connects and works without the limit. The spike verifies only two things: whether a container restart (with and without the data volume) resets the 12-hour limit, and how the token is supplied. No multi-hour soak test. The ADR and the quick start reflect the result.

**Test Infrastructure**
- **D-15:** The JVM test levels are three suites:
  - `test` (unit): no Spring and no containers; DCB decisions run on `AxonTestFixture` in memory;
  - `componentTest`: one service with its Spring context, a real PostgreSQL in Testcontainers and Axon in memory (no Axon Server);
  - `integrationTest`: a real Axon Server container, conditional appends and cross-service flows.

  The boundary between component and integration is Axon Server. H2 in PostgreSQL mode was rejected: it lacks `COLLATE "C"`, `ON CONFLICT` upserts, `jsonb` and dialect-faithful Flyway migrations, and it gives false confidence.
- **D-16:** Tests on the shared Axon Server container are isolated by fresh unique TypeIDs and data per test, with no purge between tests. This is parallel-safe because DCB criteria select events by tag. Global checks, such as name uniqueness, use unique data.
- **D-17:** The Konsist architecture rules live in one dedicated module (for example `tooling/architecture`) with an `architectureTest` suite that scans the whole repository's sources. Phase 2 rules:
  - the contracts contain no Spring or Axon logic imports (D-04);
  - every message has an explicit name and version (D-03);
  - every `@EventTag` key comes from the registry (D-10);
  - test class names end with their suite's suffix (cross-cutting convention).
- **D-18:** Shared test helpers live where they belong:
  - helpers for a module's types live in that module's `testFixtures` (ID fixtures and Arbs in the kernel, event builders in events);
  - Testcontainers singletons (Axon Server, PostgreSQL) live in a small dedicated test-support module, because they are infrastructure and belong to no particular module.
- **D-19:** `check` and `build` run **every** suite, including `integrationTest`. That makes them the single done criterion for agents, humans and CI, and GSD executors cannot report green without integration tests. There is no `quickCheck` or `fullCheck` aggregate. A human who wants a fast loop runs individual suite tasks (`test`, `componentTest`); document this in one line. The Gradle build cache keeps unchanged integration tests from re-running.
- **D-20:** CI runs two parallel jobs, both aggregated by `ci-success`:
  - `build`: `./gradlew build -x integrationTest`;
  - `integration-test`: `./gradlew integrationTest`.
- **D-21:** Of the TEST-14 libraries, only those Phase 2 tests use are added (catalog plus the `courses.test-suites` convention): `kotest-property`, Testcontainers and Awaitility if needed. MockK and `AxonTestFixture` arrive in Phase 3 with the first decisions. No unused dependencies.
- **D-22:** Parallelism is set per level. Unit tests keep multiple forks (current convention). `componentTest` and `integrationTest` use one fork per module so that container singletons and the Spring context cache are reused. JUnit in-fork parallelism comes later, if measurements justify it.

**Local Runtime (Compose) & Licence**
- **D-23:** A root `compose.yaml` pulls in per-concern files under `compose/` through `include:` (infra now; services, observability and UI later). Optional parts use profiles. `docker compose up` stays one command. Images come from the catalog through the Gradle-generated versions env file (cross-cutting INFRA-09), and `docker compose config` is validated in CI (CICD-08).
- **D-24:** One PostgreSQL server serves all services. An init script creates a separate database and user per service (query service, automation service and their token stores).
- **D-25:** The AxonIQ Platform token is optional. It is an environment variable in a gitignored `.env`, with a committed `.env.example` that has an empty value and explains it. Without the token the system works with the approximately 12-hour limit, and the quick start says so. `doctor` warns when the token is missing. CI uses no token, because its runs are short.
- **D-26:** Two connection scenarios must work against the compose Axon Server without reconfiguration: services running inside compose, and applications run from the IDE on the host. Research picks the mechanism (server hostname/internal-hostname settings or client settings). A smoke check covers both.

**doctor**
- **D-27:** `scripts/doctor.sh` is the single entry point.
  1. Dependency-free bootstrap checks:
     - mise and the pinned tools, JDK and Node;
     - the Docker engine (colima or Docker Desktop, `DOCKER_HOST` and Ryuk configuration);
     - the GSD installation against the SHA-256 hashes in `gsd-file-manifest.json`, plus modified hooks and unexpected files;
     - leaking `~/.gsd/defaults.json` keys;
     - the MCP servers;
     - the Platform token.
  2. Delegation to `./gradlew doctor` for JVM-side checks, including Testcontainers connectivity through the Testcontainers library itself.

  `doctor` consolidates the existing `VerifyFormatterBinary` and `VerifyNodeVersion` checks.
- **D-28:** `doctor` only diagnoses. It runs every check and reports each as ok, warn or fail with the exact fix command, exits non-zero on any fail, and never changes anything.
- **D-29:** A separate `ci.yml` job runs `doctor --ci` on ubuntu with Docker. In that mode the local-only checks (GSD, MCP, Platform token, colima) are reported as skip.

**Upstream Records & Licences**
- **D-30:** The licence check uses the CycloneDX SBOM (needed in Phase 6 anyway), and a small Gradle task compares its licences with an allowlist. npm uses the same approach later. Research verifies that the CycloneDX plugin works with the configuration cache and isolated projects. The fallback is the jk1 Gradle License Report plugin.
- **D-31:** The allowlist has three parts:
  - allowed: permissive licences (Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC and similar) and weak copyleft (EPL-2.0, MPL-2.0);
  - forbidden: GPL and AGPL;
  - per-artifact exceptions with justification: the Axoniq commercial-terms modules, mirrored in NOTICE.
- **D-32:** UPS-04 is a targeted CI check on PRs. It matches the changed dependencies against the `product` of open upstream records and prints their removal criteria in the job summary and as a warning annotation. It needs read-only permissions and posts no PR comments.
- **D-33:** The UPSTREAM marker and record consistency check is a Gradle task in `build-logic` with TestKit (carried forward from Phase 1 D-13). The ADR front-matter check (D-38) shares its front-matter parser.

**Templates & ADRs**
- **D-34:** There is one ADR template: full MADR 4 with YAML front matter. Optional sections (Decision Drivers, Pros and Cons, More Information) may be omitted for small decisions. Considered Options is mandatory.
- **D-35:** The Paketo spike covers images:
  - the frontend image gets a **prebuilt `dist/` from Gradle** (Node plugin plus pnpm builds once);
  - the Paketo Web Servers buildpack only serves it through nginx with project configuration: SPA fallback and an `/api` proxy with `proxy_buffering off` for SSE;
  - pnpm support inside Paketo is therefore not needed, and the image contains no Node.

  The spike builds a minimal static site and a minimal Spring Boot image in `.planning/spikes/`, records the working `BP_*` and `BPE_*` environment defaults (including JVM memory) and checks nginx configuration and colima operation. An ADR records the outcome.
- **D-36:** ADR-0001..0004 are normalised to the template, as Phase 1 D-07 promised: front matter and MADR structure, with unchanged content, dates and decisions. Custom sections (Re-enable criteria, When to revisit, Rejected alternatives) map to Confirmation, More Information and Considered Options. The "minimal record" notes are removed.
- **D-37:** The GH-12 templates live in `docs/templates/`: ADR, documentation page, Event Modeling slice description, upstream record and upstream issue draft. `docs/adr/` and `docs/upstream/` hold only real records.
- **D-38:** A Gradle check validates ADR front matter and structure:
  - required keys and allowed statuses;
  - the number in the filename matches the title;
  - numbering has no gaps or duplicates;
  - superseded links point to existing ADRs;
  - required sections are present.

**AI Tooling**
- **D-39 (amends AIDEV-03):** `.claude/skills/spike-findings-*/` is tracked too, as a narrow exception, because it is a GSD-generated artifact at the path GSD's wrap-up expects and appends to. Everything else under `.claude/` stays as AIDEV-03 says: only `settings.json` and `CLAUDE.md` are tracked, and the GSD installation and `settings.local.json` are ignored. The project's own skills stay in the repository-local plugin outside `.claude/` (AIDEV-02, Phase 8). Claude Code does not document `.agents/skills`, and GSD's wrap-up path is hard-coded, so neither is an option.
- **D-40:** The survey covers all AIDEV-01 stack areas (Axon, Kotlin, Gradle, Spring Boot, Playwright, React/shadcn, FSD, GitHub Actions, Paketo), and the tooling inventory records a verdict for each candidate. Only skills needed in Phases 2–4 are installed: Axon, Kotlin, Gradle, Spring Boot and GitHub Actions. They are installed as pinned marketplace plugins, each after explicit author approval. The others follow in their phases.
- **D-41:** GSD `agent_skills` is filled in Phase 2 after the survey. A phase task proposes the per-agent mapping (executor, planner, code reviewer and others) and the author approves it. `.planning/config.json` and `governance/gsd-config-policy.json` change in one PR with the `governance-approved` label, and ADR-0004 is updated.
- **D-42:** Context7 and Playwright MCP servers are installed as `npm:` tools through mise: exact version, checksums in `mise.lock`, Renovate-managed, and one version source (INFRA-09). `.mcp.json` calls the installed binaries and references only environment variables, never secrets. Research verifies that this works. The fallback is `npx` with an exact version plus a Renovate regex manager. Personal MCP servers (IntelliJ, optionally GitHub) stay in local scope and are documented.

### Claude's Discretion
- How the DCB context is created in compose: native Axon Server configuration if it exists, otherwise a one-shot init container calling `/v2/cluster/init?dcb=true`.
- Gradle project path names (`projectDir` mapping), exact module directory names for architecture and test-support modules, and the `.env` variable name for the Platform token.
- Exposed host ports, compose service names beyond those fixed by D-01, and the content of the PostgreSQL init script.
- The internal structure of the `courses.test-suites` convention, and the order of the suites in CI logs.
- Which existing upstream defects already qualify for `docs/upstream/` records (research collects candidates from the Phase 1 ADRs, STATE.md and the summaries).
- The removal of `:tooling:convention-sample` once the first real Kotlin module lands (already decided in Phase 1 D-22).

### Deferred Ideas (OUT OF SCOPE)
- Static analysis (detekt, `spotlessCheck`) in a fast-loop aggregate: not adopted together with `quickCheck` (D-19).
- JUnit in-fork parallelism for component and integration tests: revisit with measurements (D-22).
- `--fix` mode for `doctor`: not adopted (D-28).
- Frontend, Playwright and Paketo skills: installed in their phases per the survey verdicts (D-40).
</user_constraints>

## Project Constraints (from CLAUDE.md)

Extracted from `/Users/afanasyev/Development/axon5-dcb-courses/.claude/CLAUDE.md` (same authority as locked decisions):

- Tech stack: Kotlin + Spring Boot + Axon Framework 5, idiomatic Kotlin; event store is Axon Server (DCB), run in Docker; services split by CQRS role plus BFF; frontend React + TypeScript (FSD, Steiger).
- Test build structure: Gradle JVM Test Suites for test levels, Java Test Fixtures for shared helpers.
- Every produced artifact is auto-formatted and style-checked (pre-commit hook locally, CI enforced). All code, docs and repository artifacts are in English. Quality bar "highest professional class".
- "Decision Precedence" overrides: images are built with Cloud Native Buildpacks (`bootBuildImage`, Paketo Web Servers), not Jib or Dockerfiles; Kotlin formatting uses ktfmt via Spotless, not ktlint; the JDK version comes only from `.java-version`, Node.js from `.nvmrc`, pnpm from `packageManager`, everything else Gradle consumes comes from the version catalog via `:platform`; entity IDs use TypeID; no MapStruct or other mapping generators; no kapt; tags and releases are owned by release-please; GSD works on one branch per phase with squash merges.
- Agent git workflow: PR branch name must be `gsd/phase-<N>-<slug>`; no force-push; plain git commands (no `&&` chains) inside a worktree; executors run with `isolation: "worktree"`.
- GSD workflow enforcement: file edits start through a GSD command. Edits to governed paths need the `governance-approved` label and trigger Claude Code `ask` rules.
- The "Technology Stack" block in CLAUDE.md is generated from `.planning/research/STACK.md`; where it conflicts with PROJECT.md key decisions or REQUIREMENTS.md, those win (for example ktfmt, buildpacks).

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| INFRA-02 | Contracts modules without Spring/framework logic | Module layout, `compileOnly` Axon annotations, Konsist import rule (see "Konsist architecture module"); spike 002 contract shapes |
| INFRA-03 | One compose command starts pinned Axon Server (DCB, hostname) and PostgreSQL | "Compose structure"; spike 003/004 values; verified `include:` + versions env file + token pass-through; Postgres 18 volume path |
| INFRA-04 | Automated test: conflicting conditional append rejected, non-overlapping succeeds | Own Axon Server Testcontainers singleton (the shared container hard-codes `:latest`), spike 001 harness, "Test-support module" |
| INFRA-05 | Licence behaviour spike recorded in ADR and quick start | Spike 004 facts, `.env.example` rule (token line must be commented, not empty), ADR list |
| INFRA-06 | Kotlin event/tag serialization round trip incl. value-class IDs | Spike 002 facts; permanent test list; Jackson module placement open question |
| IDEM-04 | TypeID value-class IDs, validation, vetted libraries | Own JVM codec (spec 0.3.0, 30 vectors), `typeid-js` verdict for TypeScript, IDEM-04 wording fix |
| TEST-11 | Unit/component/integration as separate Gradle JVM Test Suites | Prototype of `courses.test-suites` under config cache + isolated projects (verified), `check` wiring, detekt reach |
| TEST-12 | Shared helpers via `testFixtures` | Prototype verified: `implementation(testFixtures(project()))` in each suite |
| TEST-14 | Standard test toolchain | Catalog additions, `kotest-property` with JUnit-only runner (and its silent-skip pitfall), Testcontainers, Awaitility deferral |
| CICD-09 | Dependency licences checked against allowlist; NOTICE | CycloneDX 3.6.1 verified under CC + IP; observed licence strings; exceptions (Axoniq, JNA) |
| UPS-01 | Upstream record per case with front matter and body | Record format, status semantics, candidate records |
| UPS-02 | Markers `UPSTREAM: UP-NNNN` + two-way CI check + generated index | Gradle check with shared front-matter parser, TestKit |
| UPS-03 | Report defects upstream with minimal reproduction, approval-gated | Issue-draft template; publication stays an approval gate |
| UPS-04 | Dependency updates prompt removal-criteria check | PR-time job design (read-only, summary + warning annotation) |
| DOC-03 | ADRs record every significant decision | MADR 4.0.0 template verified; normalisation of ADR-0001..0004; ADR-0005.. list |
| GH-12 | In-repository templates | `docs/templates/` set, MADR front-matter keys |
| TOOL-03 | `doctor` verifies environment and explains fixes | Shell + Gradle design; verified manifest hashing; mise JSON shape; Testcontainers probe |
| TOOL-04 | Works with colima and Docker Desktop (Testcontainers/Ryuk) | Verified colima prototype run; Testcontainers docs; Ryuk image pinning |
| AIDEV-01 | Skills survey, pinned marketplace plugins, approval | Candidate repositories, pinning mechanism (SHA `ref`), review criteria; Claude Code version gate |
| AIDEV-03 | `.claude` tracked selectively | `.gitignore` already implements it; D-39 exception already present |
| AIDEV-04 | Shared MCP servers pinned, no secrets, approval | Verified mise `npm:` install of both servers, `.mcp.json` env rules, Renovate automerge pitfall |
| AIDEV-07 | GSD installation integrity checked by `doctor` | Manifest algorithm verified (858/858 hashes match); two unlisted GSD files; defaults.json semantics |
</phase_requirements>

## Summary

Phase 1 left a complete, strict build (isolated projects, configuration cache, ktfmt, detekt, Dokka, governance checks, `:platform`), so Phase 2 is mostly extension work: three new convention concerns (test suites, licence SBOM check, generated compose versions), three new governance checks (upstream records, ADRs, image references), a handful of new modules, compose files, a shell tool (`doctor`) and AI-tooling pins. Every high-risk runtime unknown is already closed by the five spikes, so this research concentrated on the thirteen remaining unknowns. Most of them were resolved by running small prototypes against the real pinned versions: a copy of the repository with a `courses.test-suites` convention and a kernel module with `testFixtures`, a CycloneDX run under configuration cache and isolated projects, a mise `npm:` install of both MCP servers, compose `include:` experiments, and a Testcontainers probe on the author's colima.

The findings that change the plan most: (1) `SharedAxonServerContainer` from `axoniq-testcontainer` hard-codes `docker.axoniq.io/axoniq/axonserver:latest`, so it cannot be used; the project needs its own digest-pinned singleton in the test-support module. (2) Custom JVM test suites are not wired into `check` by Gradle and detekt's default task does not analyse them, so the convention must wire both explicitly. (3) The CycloneDX plugin 3.6.1 works under configuration cache and isolated projects only when applied per module (not aggregated from the root), and it exposes licences by SPDX `id`, by free-text `name` (Axoniq: `Axoniq Terms of Service`) or as an `expression`, with dual-licensed components appearing as several entries; JNA (LGPL-2.1-or-later, test scope) breaks the D-31 allowlist as written. (4) A bare `- AXONIQ_PLATFORM_AUTHENTICATION` entry under compose `environment:` omits the variable when it is absent from `.env`, so D-25 can use the root `.env` without a dedicated env file, but `.env.example` must carry the line commented out, because an empty value zombies Axon Server. (5) Claude Code 2.1.323 on the author's machine is one patch below the 2.1.324 needed to pin a marketplace by commit SHA. (6) `kotest-property` under the JUnit-only runner silently skips a test method when it is written as `= runBlocking { checkAll(...) }`. (7) Phase 2 has no module that naturally owns the decision-capture decorator, the Kotlin tag resolver, the Jackson module and the INFRA-04 test; this is the top open question.

**Primary recommendation:** Extend Phase 1 conventions instead of replacing them: add `courses.test-suites` (three suites, `check` wiring, detekt source wiring, catalog-supplied image properties as task inputs), create the contracts, test-support, architecture and doctor modules with flat Gradle project names mapped by `projectDir`, keep the two compose scenarios on spike 003's configuration, generate `compose/versions.env` from the catalog and commit it with a freshness check, and treat licences, upstream records, ADRs and image literals as cacheable build-logic checks with TestKit tests.

## Architectural Responsibility Map

This phase is infrastructure; the tiers below are the build and runtime layers it touches.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Message names, IDs, tags, results (contracts) | JVM library modules (`backend/contracts/*`) | Konsist rules (`tooling/architecture`) | Pure types; framework metadata only as `compileOnly` annotations (D-04) |
| Decision capture, tag resolution, conflict retry | Command-side Axon wiring (`CommandBus`/`EventStorageEngine` decorators) | Test-support integration tests | Must wrap the local segment on the handling side (spike 001) |
| Jackson serialization of IDs and sealed results | Shared serialization module (placement open) | Axon converter, later BFF | One module reused by Axon and REST (D-08) |
| DCB event store and routing | Axon Server container (compose and Testcontainers) | Pinned image from catalog | Spike 003: `STANDALONE_DCB` creates the `default` DCB context |
| Read-model database (later) and per-service DBs | PostgreSQL 18 container | Init script | D-24; token stores and projections arrive in Phase 4 |
| Image and tool versions | Version catalog | Gradle-generated `compose/versions.env`, task system properties | INFRA-09 single source |
| Test levels and fixtures | Gradle JVM Test Suites + `java-test-fixtures` | `check` aggregate, CI jobs | D-15, D-19, D-20 |
| Dependency licence policy | Build (CycloneDX per module + policy file under `governance/`) | NOTICE | Cacheable checks; policy edits are governed |
| Upstream and ADR consistency | Build (`build-logic` checks) | PR-time CI job (read-only) | D-32, D-33, D-38 |
| Environment diagnosis | `scripts/doctor.sh` (bootstrap) | `./gradlew doctor` (JVM) | Shell works before Gradle works (D-27) |
| AI tooling pins | `.claude/settings.json`, `.mcp.json`, `mise.toml` | `docs/tooling.md` inventory, `doctor` | Governed files, approval gates |

## What Phase 1 Already Provides (extend, do not duplicate)

[VERIFIED: files read this session]

| Asset | Where | Notes for Phase 2 |
|-------|-------|-------------------|
| Module convention | `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` | Applies `java-library`, Kotlin JVM, detekt, Dokka. Aligns every `implementation`/`*Implementation` configuration with `:platform` (lines 15-17: `.matching { it.name == "implementation" \|\| it.name.endsWith("Implementation") }`), so new suites and `testFixtures` are aligned automatically. Sets `useJUnitPlatform()` and `maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)` for every `Test` task (lines 50-51). `allWarningsAsErrors` with `-Xjsr305=strict -Xjspecify-annotations=strict`. |
| Root conventions | `courses.root.gradle.kts` (Spotless, `verifyNodeVersion`, `VerifyFormatterBinary`), `courses.governance.gradle.kts` (six checks wired into `check`) | Root `check` reaches build-logic through `gradle.includedBuild("build-logic").task(":check")` (line 198). Subprojects join `build` by task name selection. |
| Platform | `platform/build.gradle.kts` | `api(platform(libs.spring.boot.bom))`, axon-framework, axoniq-framework, testcontainers, jackson, kotlin, junit, kotest BOMs; one `api(libs.kotlin.stdlib)` constraint. New non-BOM libraries add catalog-referenced `api` constraints here (INFRA-11). |
| Catalog | `gradle/libs.versions.toml` | `kotlin = "2.4.21"`, `junit = "6.1.3"`, `kotest = "6.2.7"`, `axon = "5.4.2"`, `testcontainers = "2.0.5"`, `jackson = "3.3.0"`. `kotest-assertions-core = { module = "io.kotest:kotest-assertions-core" }` (BOM-governed, no version). |
| Governance checks | `build-logic/.../buildlogic/governance/*.kt` | `JsonSlurper` is the JSON precedent (`import groovy.json.JsonSlurper` in `ConfigPolicyCheck.kt` and `AskRulesCheck.kt`); `ToolingTableCheck` reads TOML and Markdown with line readers and no parser dependency; all are `@CacheableTask` with `@PathSensitive(RELATIVE)` inputs and a `report` output. `BuildScriptLintCheck` rejects version literals and eager/cross-project APIs. |
| TestKit | `GradleFixture.standard(projectDir)` / `GradleFixture.governance(projectDir)` | Runs with `--configuration-cache --isolated-projects`; functional suite is `functionalTest`, `maxParallelForks = 1`; run with `./gradlew :build-logic:functionalTest --tests '*XFunctionalTest*'` (pattern from `01-VALIDATION.md`). |
| CI | `.github/workflows/ci.yml` | Jobs `build`, `lint`, `secrets`, `qodana`, aggregate `ci-success` (`needs: [build, lint, secrets, qodana]`, `if: always()`). `permissions: {}` at workflow level, job-level `contents: read`, actions pinned by SHA. `build` runs `./gradlew build --console=plain` with `MISE_DISABLE_TOOLS: java,node`. |
| Tool pins | `mise.toml`, `mise.lock`, `.mise/locks/**` | `locked = true`. Existing `"npm:renovate" = { version = "44.252.8", trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"] }` is the working precedent for npm tools with an `aube` sidecar lock. `docs/tooling.md` rows are enforced by `checkToolingInventory` (Version source `mise` rows need the exact key in backticks). |
| Renovate | `renovate.json5` | Custom managers: java-version, catalog `# renovate: datasource=... depName=...`, mise `min_version`, Qodana image `linter: <name>:<tag>@sha256:...` (the model for catalog image lines). Automerge rule at line 190: `description: "Automerge patch updates of mise tools"`, `matchManagers: ["mise"]`. |
| Governance | `governance/governed-paths.txt` (line 54 `.mcp.json`, 57 `governance/**`, 69 `mise.toml`, 96 `renovate.json5`, 104 `package.json`, 107 `.github/**`, 120 `build.gradle.kts`, 122 `settings.gradle.kts`, 124 `gradle.properties`) | The Phase 2 PR necessarily touches governed paths (settings, `.github/**`, `mise.toml`, `.mcp.json`, `.claude/settings.json`, `governance/**`), so it needs the `governance-approved` label and Claude Code asks before each such edit. `checkClaudeAskRules` requires exactly one `Edit(/<entry>)` ask rule per governed entry: adding a governed entry means editing `.claude/settings.json` in the same PR. |
| `.gitignore` | repository root | Already tracks `/.claude/settings.json`, `/.claude/CLAUDE.md` and `!/.claude/skills/spike-findings-*/` (D-39 done), ignores `.env` and `.env.*` but keeps `!.env.example`. AIDEV-03 is therefore mostly verification plus the `doctor` check. |
| Old sample module | `settings.gradle.kts` lines 8-11 (`":platform",` `":tooling:convention-sample",`) and comment at line 14 (`//   services/{command,query,automation,bff}, contracts/*, frontend, docs-site`) | Removed when the first real Kotlin module lands (Phase 1 D-22). References to update are listed under "Repository layout move". |

## Established Spike Facts (carried values, do not re-derive)

[CITED: `.planning/spikes/WRAP-UP-SUMMARY.md` and `.claude/skills/spike-findings-axon5-dcb-courses/references/*.md`, read this session]

**Axon Server runtime (spikes 003, 004)**
- Image `docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot`, `hostname: axonserver`, env `AXONIQ_AXONSERVER_STANDALONE_DCB: "true"` and `AXONIQ_AXONSERVER_DEVMODE_ENABLED: "true"`; ports `8024` and `8124`; volumes `/axonserver/data`, `/axonserver/events`, `/axonserver/log`.
- Healthcheck: `["CMD", "curl", "-fsS", "-o", "/dev/null", "http://localhost:8024/actuator/health"]`, `interval: 5s`, `timeout: 3s`, `retries: 30`, `start_period: 10s` (the image's `/bin/sh` is dash, so no `/dev/tcp`).
- Clients: `axon.axonserver.servers=axonserver:8124` inside compose, `localhost:8124` from the host; a single node answers `sameConnection`; reconnect after restart works. `AXONIQ_UPDATE_CHECKER_DISABLED=true` in compose and CI.
- Licence: unregistered server logs `Shutdown scheduled at <boot + 12 h>`; every restart resets the window; token env is `AXONIQ_PLATFORM_AUTHENTICATION=<id>:<token>`; an **empty** value makes the server fail with `IndexOutOfBoundsException` (`AxoniqConsoleCoordinationService.kt:69`) and leaves a running container with dead HTTP/gRPC. Do not init the context from a separate container (double init corrupts the Raft group). `GET /v1/public/context` reports `dcbContext: false` even for the DCB context. Stored event type format is `namespace.Name#version`, sourcing returns a trailing `TerminalEventMessage`. The application limit (`maxApplications: 1`) is not enforced in 2026.1.8.
- Tests: `AxonServerContainer("…:2026.1.8-nonroot").withDevMode(true).withDcbContext(true)`.

**Decision capture (spike 001)**
- Decorate the `@Internal` `EventStorageEngine` (variant A); only `EventStorageEngine` and its nested `AppendTransaction` are `@Internal`. Record `condition.criteria()`, `consistencyMarker()`, the `TaggedEventMessage`s, the after-commit marker; diagnose conflicts by unwrapping to `AppendEventsTransactionRejectedException` and re-sourcing from the rejected marker with `SourcingCondition.conditionFor(marker.position(), criteria)`.
- Retry on the handling side: `CommandBus` decorator at order `Int.MIN_VALUE + 10` wrapping `RetryingCommandBus(d, AsyncRetryScheduler(MaxAttemptsPolicy(FilteringRetryPolicy(ExponentialBackOffRetryPolicy(5)) { … AppendEventsTransactionRejectedException … }, 10), Executors.newScheduledThreadPool(4)))`. Do not also register a `RetryScheduler` component. One `ConfigurationEnhancer` `@Bean` per command service registers both decorators.
- Creation commands that source nothing append with `AppendCondition.none()`: two concurrent creates of the same id both succeed; creation slices must source their own criteria.
- Permanent test shape: capture decorator × Axon Server Testcontainer, 12 concurrent subscriptions on a 3-seat course → exactly 3 committed, every attempt > 1 preceded by a conflict of the same command id (observed 42 attempts, 30 conflicts, max attempt 4). Concurrency tests must dispatch from a pool (single-thread dispatch yields 0 conflicts in memory).
- Upstream candidates: public decision-capture extension point; `ConflictDiagnosingEventStorageEngine` misses `CompletionException`; dispatch-side retry cannot classify remote DCB conflicts (`CommandExecutionException(AxonServerNonTransientRemoteCommandHandlingException AXONIQ-4005)` carries only text).

**Kotlin IDs, tags, serialization (spike 002)**
- TypeID spec v0.3.0; vectors `spec/valid.yml` (9) and `spec/invalid.yml` (21) at jetify-com/typeid commit `26a84e85491203834ed0d1c3423ba5f679a67114`; prefix regex `^([a-z]([a-z_]{0,61}[a-z])?)?$`; suffix 26 lowercase Crockford chars `0123456789abcdefghjkmnpqrstvwxyz`, first char ≤ `7`; UUIDv7 from `com.fasterxml.uuid:java-uuid-generator` 5.2.0 (`Generators.timeBasedEpochGenerator()`).
- ID shape: `@JvmInline value class CourseId(override val uuid: UUID) : TypedId` with `toString() = TypeId.encode(PREFIX, uuid)` and `companion object : TypedIdCompanion<CourseId>("course", ::CourseId) { const val PREFIX = "course" }`; `object Tags { const val COURSE = CourseId.PREFIX … }`.
- Annotation imports used (verbatim from the spike): `org.axonframework.eventsourcing.annotation.EventTag`, `org.axonframework.messaging.commandhandling.annotation.Command`, `org.axonframework.messaging.eventhandling.annotation.Event`; `@Event(namespace = "courses", name = "CourseCreated", version = "1")`; `TagResolver` is `org.axonframework.eventsourcing.eventstore.TagResolver`, `Tag` is `org.axonframework.messaging.eventstreaming.Tag`.
- The default `AnnotationBasedTagResolver` writes the raw UUID for value classes (silently breaks criteria) → `KotlinTagResolver` (kotlin-reflect, reads `prop.annotations`, `javaField` and `javaGetter` annotations, uses `value.toString()`), registered as the `TagResolver` component.
- Entities register with the unboxed id type: `EventSourcedEntityModule.autodetected(UUID::class.java, CourseSeats::class.java)`; `@TargetEntityId` property getter returns `UUID`; `axon-kotlin` is required for nullable `@InjectEntity` (non-null → `EntityNotFoundException` for new entities).
- Jackson 3 (`tools.jackson`): `SimpleModule` with a `ValueSerializer`/`ValueDeserializer` per id (`gen.writeString(value.toString())`, `companion.parse(p.valueAsString)`); sealed results need polymorphic type info (`@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "@type")`, imported from `com.fasterxml.jackson.annotation`), supplied by a mix-in in the spike; `data object` results deserialise to the singleton.
- Rejected libraries: `de.fxlae:typeid-java` 0.3.1 (idle since 2024-05, 33 stars), `me.lessis:typeid` (spec v0.2), aleris/typeid-kotlin (not on Maven Central under that name).

**Container images (spike 005)** (consumed by the buildpack ADR; image builds themselves are Phase 6)
- Boot: `bootBuildImage` default builder `paketobuildpacks/builder-noble-java-tiny`; env `BP_JVM_VERSION=25`, `BPE_DEFAULT_BPL_JVM_THREAD_COUNT=50`, `BPE_DEFAULT_BPL_JAVA_NMT_ENABLED=false`, `BPE_DELIM_JAVA_TOOL_OPTIONS=" "`, `BPE_PREPEND_JAVA_TOOL_OPTIONS=-XX:ReservedCodeCacheSize=64M`, `BP_HEALTH_CHECKER_ENABLED=true`, labels through `BP_IMAGE_LABELS`; `BPE_DEFAULT_JAVA_TOOL_OPTIONS` is ignored; `BPE_*` are invisible in `Config.Env`; health checks need container env `THC_PORT`/`THC_PATH` and `/workspace/health-check`.
- Frontend: Gradle builds `dist/`, a `Zip` task packs `dist/` + `nginx.conf` + `mime.types`, a `BootBuildImage` task with builder `paketobuildpacks/ubuntu-noble-builder` and buildpacks `paketo-buildpacks/web-servers` + health-checker; custom templated `nginx.conf` (`listen {{port}};`, temp paths under `{{ tempDir }}`, vendored `mime.types`, `try_files $uri $uri/ /index.html;`, `/api/` proxy with `proxy_buffering {{env "PROXY_BUFFERING"}}`); generated nginx config rejected; `ubuntu-noble-run-tiny` fails on `libcrypt.so.1`.
- Local blockers (author-approved fixes): colima's DNS forwarder never answers AAAA (10 s Go lookups → `colima start --dns <resolvers>`, persisted as `network.dns`); Docker 29 containerd image store breaks `docker save --platform` for the noble run image (moby#52193, docker/cli#6457) → classic store (`docker.features.containerd-snapshotter: false`).

## Standard Stack

### Core (additions for Phase 2)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `org.axonframework:axon-messaging`, `axon-eventsourcing` | 5.4.2 (BOM) | `@Event`, `@Command`, `@EventTag` annotations (contracts, `compileOnly`) and runtime in command-side modules | Already in `axon-framework-bom` [VERIFIED: axon-framework-bom-5.4.2.pom lists `axon-eventsourcing`, `axon-messaging`, `axon-kotlin`, `axon-kotlin-test`] |
| `io.axoniq.framework:axon-server-connector` | 5.4.2 (BOM) | Axon Server client | In `axoniq-framework-bom` [VERIFIED: pom] |
| `org.axonframework.extensions.kotlin:axon-kotlin` | 5.4.2 (BOM) | Nullable `@InjectEntity` | Spike 002 requirement |
| `io.axoniq.framework:axoniq-testcontainer` | 5.4.2 (BOM) | `AxonServerContainer` only (not `SharedAxonServerContainer`) | Sources read: shared instance hard-codes `docker.axoniq.io/axoniq/axonserver:latest` |
| `org.testcontainers:testcontainers`, `testcontainers-postgresql` | 2.0.5 (BOM, also Boot-managed 2.0.5) | Containers, PostgreSQL singleton, `DockerClientFactory` for `doctor` | 2.x artifact names are prefixed [VERIFIED: Maven Central metadata 2.0.5; boot pom line 209 `<testcontainers.version>2.0.5</testcontainers.version>`] |
| `io.kotest:kotest-property` | 6.2.7 (kotest-bom) | `Arb` generators and property tests under the JUnit runner | In `kotest-bom` 6.2.7 [VERIFIED: pom, 10 references]; prototype ran it |
| `com.fasterxml.uuid:java-uuid-generator` | 5.2.0 | UUIDv7 (`timeBasedEpochGenerator`) for `random()` and the `Clock`-taking test generator | Not in any BOM, needs a catalog version and an `api` constraint in `:platform` [VERIFIED: absent from spring-boot-dependencies 4.1.3; spike 002 used 5.2.0] |
| `com.lemonappdev:konsist` | 1.0.0-beta.1 (published 2026-11-04) | Architecture tests | POM requires `kotlin-compiler-embeddable` 2.4.21, same as the project [VERIFIED: konsist-1.0.0-beta.1.pom, Last-Modified 2026-11-04]. Fallback 0.18.1 (2026-10-10, built with Kotlin 2.4.21). Not in a BOM: catalog version + `:platform` constraint |
| `org.cyclonedx:cyclonedx-gradle-plugin` (plugin id `org.cyclonedx.bom`) | 3.6.1 | Per-module SBOM feeding the licence check | Verified under configuration cache and isolated projects when applied per module (experiment). Needs Gradle 8.4+ [CITED: README] |
| `tools.jackson.module:jackson-module-kotlin`, `tools.jackson.core:jackson-databind` | jackson BOM 3.3.0 | Serialization module | In `jackson-bom` [VERIFIED: bom lists `jackson-module-kotlin`] |
| `org.jetbrains.kotlin:kotlin-reflect` | Kotlin BOM | `KotlinTagResolver` | Spike 002 |
| `ch.qos.logback:logback-classic` | Boot-managed | Test logging binding | Spikes used `logback-test.xml`; EPL-1.0 / LGPL-2.1 dual licence, see licence section |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `org.awaitility:awaitility` | 4.3.0 (Boot-managed) | Async assertions | Add only when a Phase 2 test needs it (D-21). `axoniq-testcontainer` already pulls it transitively at compile scope |
| `com.fasterxml.jackson.core:jackson-annotations` | 2.22 resolved under jackson-bom 3.3.0 [VERIFIED: appears in the SBOM run] | `@JsonTypeInfo` for sealed-result handling | Only for the annotation-in-contracts option, or in the serialization module's mix-ins |
| `typeid-js` (npm) | 1.2.0 (published 2025-02-04) | TypeScript TypeID, Phase 5+ | Record in the ADR now, install later |
| `@upstash/context7-mcp` / `@playwright/mcp` (npm via mise) | 4.7.0 / 0.0.85 (7-day-old releases as of 2026-11-22) | MCP servers | After author approval, see MCP section |
| `AxonIQ/agent-skills` (Claude Code marketplace `axoniq`, plugin `axoniq-app-development`) | marketplace commit `59e7beb6bb487894aeb1294918463af1432bdece` (2026-10-12), plugin version 0.5.1 | Axon 5 skill | After author approval |
| `gradle/gradle-skills` (marketplace and plugin `gradle-skills`) | tag `1.1.0`, commit `00b7790139dff32063976f98334b2a2301b50a9d` | Gradle best-practice skill | After author approval |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Own `AxonServerContainer` singleton | `SharedAxonServerContainer.INSTANCE` | Hard-codes the `:latest` image (violates INFRA-09), ignores catalog digest; only its cross-process `FileLock` start idea is worth copying |
| Konsist 1.0.0-beta.1 | Konsist 0.18.1 | beta.1 fixes "many APIs returned incorrect results" but is pre-1.0 and calls itself the last pre-1.0 release [CITED: github.com/LemonAppDev/konsist/releases]; 0.18.1 is stable. Prefer beta.1 with `strict = true` rules and a meta test; fall back to 0.18.1 if an API is missing |
| CycloneDX 3.6.1 | jk1 `com.github.jk1.dependency-license-report` 3.1.5 | Only if CycloneDX fails in CI; jk1 has no SBOM use in Phase 6 |
| mise `npm:` MCP tools | `npx` with exact version + regex manager | Fallback only; `npx` fetches at run time without lock |
| Compose `include:` with `env_file` | `docker compose --env-file` flag | `include:` keeps `docker compose up` a single, flag-free command (verified) |

**Installation (catalog additions, no literals in build scripts):**
```toml
# gradle/libs.versions.toml  [versions]
konsist = "1.0.0-beta.1"
java-uuid-generator = "5.2.0"
cyclonedx = "3.6.1"
# Images as full references with tag and digest (INFRA-09); digests as of 2026-11-22, re-resolve when pinning.
image-axonserver = "docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot@sha256:90a477bc9c51d56b18778f16fe1ccd6d0f614e9ec7bcbf0a9b99b3fbad768577"
image-postgres = "postgres:18.7@sha256:eb63372c1e05c755f55fa30cbd07aecc443676fc6c0fa90b9002d01537841e1a"
```
`libraries` get BOM-governed entries without versions (axon messaging/eventsourcing/kotlin, `axon-server-connector`, `axoniq-testcontainer`, `testcontainers`, `testcontainers-postgresql`, `kotest-property`, jackson modules, `kotlin-reflect`, `logback-classic`) and versioned entries for `konsist`, `java-uuid-generator` and the CycloneDX plugin dependency (`org.cyclonedx:cyclonedx-gradle-plugin`); `:platform` gets `api(libs.konsist)` and `api(libs.java.uuid.generator)` constraints (the `kotlin-stdlib` precedent).

**Version verification:** [VERIFIED: Maven Central `maven-metadata.xml` and plugin portal, 2026-11-22] konsist 1.0.0-beta.1, kotest-property 6.2.7, testcontainers-postgresql 2.0.5, awaitility 4.3.0, java-uuid-generator 5.2.0, axoniq-testcontainer 5.4.2, axonserver-connector-java 2026.1.1, cyclonedx-gradle-plugin and marker `org.cyclonedx.bom` 3.6.1, jk1 license report 3.1.5. Image digests from the Docker Hub API (`docker.axoniq.io` fronts Docker Hub's `axoniq/axonserver`: its manifest endpoint answers with `www-authenticate` realm `auth.docker.io`, scope `repository:axoniq/axonserver:pull`).

### Protobuf and Gradle alignment (focus item 1)

`io.axoniq:axonserver-connector-java` 2026.1.1 builds with `protobuf.version` 4.36.0 and `grpc.version` 1.79.0 [VERIFIED: connector pom]. `spring-boot-dependencies` 4.1.3 imports `com.google.protobuf:protobuf-bom` at `${protobuf-java.version}` (= 4.36.2) [VERIFIED: boot pom lines 3491-3497 and property `<protobuf-java.version>4.36.2</protobuf-java.version>`], and `:platform` imports that BOM with `api(platform(libs.spring.boot.bom))`; Gradle selects the higher of 4.36.0 and 4.36.2. Spike 002/003 observed 4.36.2 resolved. **Conclusion: no explicit constraint is needed in `:platform` today.** Fallback if a Spring Boot bump ever stops managing protobuf below 4.36: add a catalog `protobuf` version and `api(libs.protobuf.java)`. Guard: every Axon Server integration test fails immediately with the `RuntimeVersion` `NoClassDefFoundError` if the alignment breaks, so no extra guard task is needed. (protobuf-java 4.37.0-RC3 is the newest Central release; the stable 4.36.2 is correct.)

## Package Legitimacy Audit

> Maven coordinates are not covered by the `package-legitimacy` seam (npm, pypi, crates only). They were verified through Maven Central metadata/POMs and the project's own STACK.md selection; each is tagged below. npm packages were run through the seam.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| `@upstash/context7-mcp` 4.7.x | npm | latest published 2026-11-17 (versions since at least 2026-10) | 1,178,954/wk | github.com/upstash/context7 | SUS (`too-new`: the newest release is days old, package is mature) | Flagged: planner must add `checkpoint:human-verify` before install (also the AIDEV-04 approval gate); pin a release at least 7 days old (4.7.0, 2026-11-12) |
| `@playwright/mcp` 0.0.85 | npm | latest published 2026-11-18, provenance attestation present | 11,320,236/wk | github.com/microsoft/playwright-mcp | SUS (`too-new`, same reason) | Flagged: same checkpoint; pin 0.0.85 (2026-11-09). Every release depends on an **alpha** Playwright (`1.66.0-alpha-2026-11-09`) [VERIFIED: npm view] |
| `typeid-js` 1.2.0 | npm | published 2025-02-04, repo pushed 2026-11-10 | 3,359,926/wk | github.com/jetify-com/typeid-js | OK | Approved for the ADR; install in Phase 5+ |
| `com.lemonappdev:konsist` | Maven Central | 1.0.0-beta.1 on 2026-11-04 | n/a | github.com/LemonAppDev/konsist | not scannable [VERIFIED: Central POM, Apache-2.0] | Approved (named in STACK.md, project already chose it) |
| `org.cyclonedx:cyclonedx-gradle-plugin` | Gradle plugin portal | 3.6.1, portal marker present | n/a | github.com/CycloneDX/cyclonedx-gradle-plugin | not scannable [VERIFIED: portal metadata, ran in experiment] | Approved |
| `com.fasterxml.uuid:java-uuid-generator` | Maven Central | 5.2.0 | n/a | FasterXML | not scannable [VERIFIED: spike 002 resolved and ran it] | Approved |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** `@upstash/context7-mcp`, `@playwright/mcp` (both only `too-new`; `postinstall` is empty for all three npm packages). The planner inserts `checkpoint:human-verify` before each install. Postinstall scripts: `npm view` returned none for the three packages [VERIFIED: gsd `postinstall: null` signals].

*Marketplace plugins and skills (`AxonIQ/agent-skills`, `gradle/gradle-skills`) are not packages in this sense; they are governed by the approval gate and the review criteria below.*

## Architecture Patterns

### System Architecture Diagram

```
                      developer / agent                                CI (GitHub Actions)
                             |                                                |
            ./gradlew build  |  ./gradlew doctor / scripts/doctor.sh          |  build job: build -x integrationTest
                             v                                                |  integration-test job: integrationTest
 +------------------ version catalog (single source) ------------------+     |  doctor job: doctor.sh --ci
 | libs.versions.toml: axon, kotest, konsist, image-axonserver@digest,  |     |  lint job: docker compose config, reuse ...
 | image-postgres@digest, cyclonedx                                     |     |  pr job: upstream-record advisory (read-only)
 +--+----------------+--------------------+---------------------------+     v
    |                |                    |                          ci-success (single required check)
    v                v                    v
 :platform      courses.test-suites   generateComposeVersions
 (BOM align)    (3 suites + fixtures) -> compose/versions.env (committed, freshness-checked)
    |                |                    |
    |      +---------+----------+         v
    |      | test | componentTest | integrationTest        compose.yaml --include--> compose/infra.yaml
    |      |      (Postgres TC) (Axon Server TC, own          axonserver (STANDALONE_DCB, hostname, healthcheck)
    |      |                     singleton, digest image)      postgres:18 (+ init script per service DB)
    |      +---------+----------+                              ^
    v                v                                         | localhost:8124 (host) / axonserver:8124 (network)
 contracts-kernel -> contracts-{commands,events,queries}  <--- tests (INFRA-04/06, capture) --> Axon Server container
 (TypeID codec, Tags, Rejection)  (compileOnly Axon annotations)
    ^
    | Konsist scans backend/** sources (declared inputs) -> :tooling:architecture architectureTest
 build-logic checks (cacheable, TestKit): upstream records <-> markers, ADR front matter, dependency licences
 (per-module CycloneDX BOM + governance/dependency-licences.json), image literals, generated-file freshness
```

### Recommended Project Structure

```
settings.gradle.kts                       # include(...) + projectDir mapping, layout comment updated (D-01)
backend/
  contracts/kernel/                       # :contracts-kernel   TypeId, ids, Tags, Rejection, result base; testFixtures
  contracts/commands/                     # :contracts-commands
  contracts/events/                       # :contracts-events
  contracts/queries/                      # :contracts-queries
  command-service/                        # :command-service    (see Open Question 1: Axon wiring + permanent Axon Server tests)
  serialization/                          # :serialization-jackson (see Open Question 2)
tooling/
  architecture/                           # :tooling:architecture  architectureTest suite (Konsist)
  test-support/                           # :tooling:test-support  Testcontainers singletons (main source set)
  doctor/                                 # :tooling:doctor        JVM part of doctor (testcontainers only)
build-logic/src/main/kotlin/
  courses.test-suites.gradle.kts          # three suites, check wiring, detekt sources, image properties
  courses.licences.gradle.kts             # CycloneDX per module + checkDependencyLicences  (or inside courses.kotlin-library)
  io/github/aanbrn/courses/buildlogic/governance/   # UpstreamRecordCheck, AdrCheck, ImageLiteralCheck, FrontMatter, ...
compose.yaml  compose/infra.yaml  compose/versions.env  compose/postgres/init/01-service-databases.sh
.env.example  NOTICE  governance/dependency-licences.json
docs/templates/  docs/upstream/  docs/adr/  docs/quick-start.md
scripts/doctor.sh  scripts/upstream/  scripts/test-doctor.sh (CI runs every scripts/test-*.sh)
.mcp.json
```
Gradle project names are flat for backend modules (`:contracts-kernel`, `:command-service`) with `project(":x").projectDir = file("backend/...")`. Reasons: [VERIFIED: prototype] a flat name with a `projectDir` mapping configures and runs without any intermediate project; nested paths such as `:contracts:kernel` would create an intermediate `:contracts` project whose default directory does not exist (Gradle deprecates projects without an existing directory) [ASSUMED: Gradle 9 behaviour for a missing intermediate directory was not exercised]; Phase 6 image names derive from `<rootProject.name>-<project.name>` (CICD-10), and flat names equal the compose service names fixed by D-01. `:tooling:*` keeps the existing nested precedent (the `tooling/` directory exists).

### Pattern 1: `courses.test-suites` convention (D-15, D-19, D-22, TEST-11, TEST-12)

**What:** one precompiled script plugin that applies `courses.kotlin-library` and `java-test-fixtures`, registers `componentTest` and `integrationTest` next to the built-in `test`, gives every suite JUnit Jupiter at the catalog version, the module itself, its own `testFixtures` and Kotest assertions, sets one fork for the two infrastructure suites, wires all suites into `check`, widens detekt, and passes image references as task inputs.

**Verified in a prototype (copy of the repository, Gradle 9.9.2, `--configuration-cache` and isolated projects on):** the module `:contracts-kernel` (flat name, `projectDir = backend/contracts/kernel`) compiled and ran one test in each suite using both `main` classes and `testFixtures` classes through `implementation(project())` and `implementation(testFixtures(project()))`.

```kotlin
// build-logic/src/main/kotlin/courses.test-suites.gradle.kts  (prototype that ran)
plugins {
    id("courses.kotlin-library")
    `java-test-fixtures`
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

@Suppress("UnstableApiUsage") // JVM Test Suite API is still @Incubating; a Gradle upgrade that changes it fails in its Renovate PR
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                implementation(libs.kotest.assertions.core)
            }
        }
        register<JvmTestSuite>("componentTest") {
            targets.configureEach { testTask.configure { shouldRunAfter("test"); maxParallelForks = 1 } }
        }
        register<JvmTestSuite>("integrationTest") {
            targets.configureEach { testTask.configure { shouldRunAfter("componentTest"); maxParallelForks = 1 } }
        }
    }
}

// D-19: check runs every suite (Gradle wires only `test` into `check`).
tasks.named("check") { dependsOn(testing.suites.withType<JvmTestSuite>().map { it.targets.map { t -> t.testTask } }) }
```

Observed facts that the plan must act on:
- `./gradlew :contracts-kernel:check --dry-run` before the last line listed `test` only: **`componentTest` and `integrationTest` are not in `check` unless wired** [VERIFIED: prototype]. After the wiring line all three appear.
- `:contracts-kernel:detekt` analysed `src/main` and reported a main-source finding but **did not report an undocumented public function placed in `src/componentTest`** [VERIFIED: prototype]. Add the suite and `testFixtures` source directories to `detekt { source.from(...) }` (Phase 1 did the same for `functionalTest`: `source.from("src/functionalTest/kotlin")` in `build-logic/build.gradle.kts`), and extend the test excludes in `config/detekt/detekt.yml`: today the `UndocumentedPublicFunction`, `UndocumentedPublicProperty` and `FunctionNaming` rules exclude `"**/test/**"` and `"**/*FunctionalTest.kt"` but not `**/componentTest/**`, `**/integrationTest/**`, `**/architectureTest/**`. `testFixtures` is public API of its module and should stay documented.
- Custom suites do not see `internal` declarations of `main` (compile error `Cannot access 'fun internalThing(): String': it is internal in file` in `componentTest`/`integrationTest`, while `test` compiles) [VERIFIED: prototype]. That is acceptable for black-box suites; use `kotlin.target.compilations` `associateWith` only if a suite really needs it.
- `maxParallelForks` from `courses.kotlin-library` (half the CPUs) still applies to the unit `test` suite; the two infrastructure suites override it to 1 (D-22).
- Image references and the update-checker flag: pass them as inputs of the component and integration tasks, from catalog providers: `jvmArgumentProviders` with a small `CommandLineArgumentProvider` class annotated `@Input` (a plain `systemProperty(name, provider)` unwrapping is [ASSUMED] and should be proven by a TestKit test that a changed image invalidates the task); also `environment("AXONIQ_UPDATE_CHECKER_DISABLED", "true")`, and Testcontainers' own images (see Pitfall 9).
- Where a module has no sources in a suite, compile and test tasks report `NO-SOURCE`; registering all three suites in every module is cheap and uniform.
- The existing convention declares `testImplementation(libs.junit.jupiter)` and `testRuntimeOnly(libs.junit.platform.launcher)` itself; with `useJUnitJupiter(libs.versions.junit)` on every suite the explicit dependency becomes redundant (verify the launcher is added by the suite on Gradle 9.9.2 before removing it; the prototype passed with the suites declaring only `useJUnitJupiter`).
- Kover (Phase 3) later instruments `test` and `componentTest` JVMs; nothing to do now.

A TestKit test in `build-logic` must prove the convention: three tasks exist, `check` depends on all, a failing test in `integrationTest` fails `check`, a test in each suite can read `testFixtures`, and the configuration cache and isolated projects stay on (the fixture already enables them). `GradleFixture.standard` needs a variant module applying `courses.test-suites`.

### Pattern 2: Test-support module with Testcontainers singletons (D-18, INFRA-10)

**What:** `:tooling:test-support` (main source set, library) with Kotlin `object`s `AxonServerEnvironment` and `PostgresEnvironment` that lazily build one container per JVM from image references read from system properties set by the convention (property names are constants in this module and in the convention: one place each). Modules depend on it with `integrationTestImplementation(project(":tooling:test-support"))` / `componentTestImplementation(...)`.

**Why not `SharedAxonServerContainer`:** its `INSTANCE` is `new AxonServerContainer("docker.axoniq.io/axoniq/axonserver:latest")` [VERIFIED: axoniq-testcontainer-5.4.2-sources.jar]. Copy only its idea: a cross-process `FileLock` around `start()` so concurrent Gradle module JVMs do not start duplicates when Testcontainers reuse is on.

**Known API (read from the 5.4.2 sources jar):** `AxonServerContainer(String)`/`(DockerImageName)` asserts compatibility with `docker.axoniq.io/axoniq/axonserver` or `axoniq/axonserver`; `withDevMode(true)`, `withDcbContext(true)`, `withAxonServerHostname(...)`, `withAxonServerName(...)`, `getAxonServerAddress()`, `getHttpPort()`, `getGrpcPort()`; `doStart()` calls `AxonServerContainerUtils.initCluster(getHost(), getHttpPort(), isShouldBeReused(), dcbContext)` which takes a cross-process file lock and re-checks whether the instance is already initialised (idempotent, so `withReuse(true)` is safe). The container pulls `awaitility` and `junit-jupiter` at compile scope transitively.

PostgreSQL: `org.testcontainers.postgresql.PostgreSQLContainer` from `testcontainers-postgresql` 2.x (package changed in 2.x) [ASSUMED: the package name was not compiled here; confirm in Wave 0], created from `DockerImageName.parse(<catalog ref>).asCompatibleSubstituteFor("postgres")`. `@ServiceConnection` needs `spring-boot-testcontainers` and belongs to Phase 4; Phase 2 must not add it. To keep the PostgreSQL singleton from being unused code (D-21), test-support gets its own `componentTest`/`integrationTest` smoke tests that start each singleton and run `SELECT 1` / an Axon Server health call.

**Local reuse (INFRA-10):** `withReuse(true)` plus `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`, local only and opt-in, CI never. The reuse hash needs a byte-identical container configuration across modules, which is why one shared definition lives in test-support. D-16 (unique ids, no purge) already makes sharing safe.

**Colima / Ryuk (TOOL-04), verified on the author's machine:** with `~/.testcontainers.properties` containing `tc.host=unix:///var/run/docker.sock`, a probe test in the prototype printed `isDockerAvailable=true socket=/var/run/docker.sock`, server version `29.5.2`, and a running Ryuk container `testcontainers/ryuk:0.14.0` [VERIFIED: prototype run]. The Testcontainers documentation's Colima recipe is different (start colima with `--network-address`, `DOCKER_HOST=~/.colima/default/docker.sock`, `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`, `TESTCONTAINERS_HOST_OVERRIDE` from `colima ls -j`) [CITED: java.testcontainers.org/supported_docker_environment]; document both, mark the property file variant as the author's verified one (`tc.host` is not in the documented key list [CITED: java.testcontainers.org/features/configuration, which lists `docker.host`]). Docker Desktop is "automatically detected" [CITED: same page]; it cannot be verified on this machine, so TOOL-04's Docker Desktop half rests on documentation (state this in the docs and `doctor`).

### Pattern 3: Konsist architecture module (D-17)

**Version:** 1.0.0-beta.1 (fallback 0.18.1) as above. A suite `architectureTest` lives in `:tooling:architecture` with tests suffixed `*ArchitectureTest` (matches the suffix convention).

**Scanning the whole repository from one module [VERIFIED: Konsist 1.0.0-beta.1 sources]:**
- `PathProvider.rootProjectPath` starts at the working directory and walks up until `GradleProjectRootDirResolver` finds `gradlew` (or the wrapper files) **and** `settings.gradle(.kts)`; the Gradle `Test` task's working directory is the module directory, so the walk reaches the repository root. No configuration needed.
- `ProjectDirectoryFilter` skips every hidden directory (`.planning`, `.claude`, `.gradle`, `.git`), `node_modules`, and `build` output directories, so spike code under `.planning/spikes` and the worktrees under `.claude` are not scanned (documented in the class KDoc).
- `scopeFromProject()` would also scan `build-logic/src`; use `Konsist.scopeFromDirectory("backend")` (path relative to the project root, `scopeFromDirectory(path, vararg paths)` exists) or `scopeFromDirectories(listOf(...))` for rule targets.
- Test source sets are classified by name: a source set whose name contains `test` (case-insensitive) is test code; `scopeFromProduction`/`scopeFromTest` take a `sourceSet` argument and throw `IllegalArgumentException` for mismatches [CITED: docs.konsist.lemonappdev.com/writing-tests/koscope.md]. `componentTest`, `integrationTest`, `architectureTest`, `testFixtures` all qualify as test.
- `assertTrue`/`assertFalse` exist on `List`, `Sequence` and single declarations with parameters `strict: Boolean = false`, `additionalMessage`, `testName`, `rule: KoRule`; **`strict = false` passes on an empty list** [VERIFIED: KoDeclarationAndProviderAssert.kt]. A wrong directory or an empty `contracts-events` therefore passes silently: use `strict = true` where the scope must be non-empty, and add a meta test asserting the scanned scope contains the expected modules. `scopeFromModule` additionally returns an empty scope for unknown modules unless `strict = true` [CITED: koscope.md]. Annotations expose `arguments: List<KoArgumentDeclaration>` (named arguments), and `hasAnnotationWithName` exists [VERIFIED: sources].

**Cache correctness (critical):** the `architectureTest` task reads files outside its project; declare them as inputs (`inputs.files(fileTree(rootDirectory) { include("backend/**/src/**/*.kt", "backend/**/build.gradle.kts", "tooling/**/src/**/*.kt") }.withPropertyName("scannedSources").withPathSensitivity(RELATIVE)`, root directory via `isolated.rootProject.projectDirectory` as `courses.kotlin-library` already does), otherwise Gradle serves the result from cache after a rule-breaking edit.

The four Phase 2 rules (with scope):
1. `backend/contracts`: no import whose name starts with `org.springframework`, and no import from `org.axonframework` except the annotation packages used as metadata (`org.axonframework.messaging.eventhandling.annotation`, `org.axonframework.messaging.commandhandling.annotation`, the query equivalent, `org.axonframework.eventsourcing.annotation`); plus a plain-JUnit text check that contracts build scripts declare Axon only as `compileOnly` (do not rely on Konsist reading `*.kts`, unverified).
2. Every class annotated `@Event`, `@Command` or `@Query` (and their `data object` / object forms) names the arguments `namespace`, `name` and `version` explicitly (`annotation.arguments.map { it.name }`).
3. Every `@EventTag(key = ...)` argument is a reference into the registry object (`Tags.X`), not a string literal (`strict = true` once real events exist).
4. Test classes end with their suite's suffix: a class under `src/test` ends in `Test` and not in another suite's suffix; `src/componentTest` → `ComponentTest`; `src/integrationTest` → `IntegrationTest`; `src/architectureTest` → `ArchitectureTest`; `src/functionalTest` (build-logic) → `FunctionalTest`. Exclude non-test helper classes by requiring at least one `@Test`/`@ParameterizedTest` function.

An additional cheap rule for INFRA-09: no string literal argument to `DockerImageName.parse(...)` or a `*Container(` constructor in any test source set (images come from the catalog through system properties).

### Pattern 4: Compose structure (D-23, D-24, D-25, D-26)

Verified experiments (Docker Compose v2.32.1 on colima):
- Root file `include:` with a per-include `env_file` works and keeps `docker compose up` flag-free:
  ```yaml
  # compose.yaml
  name: courses
  include:
    - path: compose/infra.yaml
      env_file: compose/versions.env
  ```
  `compose/infra.yaml` uses `image: ${AXONSERVER_IMAGE:?run ./gradlew generateComposeVersions}`; `docker compose config` resolved the image from `compose/versions.env` with **no root `.env` present**, so CI can validate with no secrets. A **listed** `env_file` that does not exist fails (`stat …/.env: no such file or directory`), so never list `.env` in the include; an optional root `.env` is auto-loaded for the root project and still feeds pass-through variables of included services [VERIFIED: experiments].
- **Platform token (reconciles D-25 with spike 004):** under `environment:` write the bare name `- AXONIQ_PLATFORM_AUTHENTICATION`. Results [VERIFIED: `docker compose up -d` plus `logs`, curl image]: variable absent from the env file → **not present in the container**; `AXONIQ_PLATFORM_AUTHENTICATION=` (empty) → present and empty (this zombies Axon Server); `=abc:def` → present. Therefore the root `.env` can hold the token (no dedicated `axonserver.env`, no `env_file` that would leak every `.env` variable into the Axon Server container), **but `.env.example` must contain the line commented out** (`# AXONIQ_PLATFORM_AUTHENTICATION=<environment-id>:<token>`), not "an empty value" as D-25 words it, and `doctor` must report an empty definition as a failure with the fix.
- Profiles: a `profiles: [db]` service is omitted from `config --services` by default and listed with `COMPOSE_PROFILES=db`; optional parts (probe, observability later) use profiles.
- The generated versions file: `generateComposeVersions` writes `compose/versions.env` (`AXONSERVER_IMAGE=…`, `POSTGRES_IMAGE=…`) from `libs.versions.image.*` providers; the file is **committed** so a fresh clone runs `docker compose up` without Gradle, and a `check`-wired freshness task fails when the committed file differs from the generated one (the same pattern Phase 6 reuses for `.env.example`, INFRA-17). Make the task type generic ("generated file equals committed file") so Phase 6 adds a second instance.
- PostgreSQL 18: mount the volume at `/var/lib/postgresql` (not `/var/lib/postgresql/data`); the image's `PGDATA` is now `/var/lib/postgresql/18/docker` [CITED: docker-library/postgres PR 1259 and issues 1370/1377 via search]. Init scripts in `/docker-entrypoint-initdb.d` run only on an empty data directory [ASSUMED: standard image behaviour, not re-run]; mount `compose/postgres/init/01-service-databases.sh` (shellcheck and shfmt apply) that creates one database and one login role per service using `psql` with `POSTGRES_*`-style env, with development-only default passwords overridable from `.env` (not secrets). Healthcheck `pg_isready -U $POSTGRES_USER -d $POSTGRES_DB`.
- Axon Server service: exactly the snippet in "Established Spike Facts"; `hostname: axonserver` is mandatory, and no init container (double init corrupts the Raft group).
- `no image literals in compose files` (INFRA-09): add a check that every `image:` value in `compose/**/*.yaml` and `compose.yaml` is a `${…}` interpolation.
- CI: add `docker compose config --quiet` to the `lint` job (compose is preinstalled on ubuntu runners [ASSUMED]); no `.env` is needed.
- **Smoke covering both scenarios (D-26):** host scenario = run the INFRA-04 test class against the compose server by letting it read an external address (`courses.axonserver.address`, default: Testcontainers singleton) and executing `./gradlew integrationTest -Pcourses.axonserver.external=localhost:8124 --tests '*ConditionalAppendIntegrationTest'`; in-network scenario = `docker compose up -d --wait` health plus an HTTP reachability probe from a sibling container on the compose network; the full gRPC-in-network proof is spike 003's probe and is re-exercised for real by the Phase 3/6 services (document this residual honestly; see Open Question 5).

### Pattern 5: Licence check (D-30, D-31, CICD-09)

**Verified experiment** (scratch module `a`, Gradle 9.9.2, `org.gradle.configuration-cache=true`, `org.gradle.isolated-projects=true`, plugin `org.cyclonedx.bom` 3.6.1 applied in the module's own build script): `:a:cyclonedxDirectBom` succeeded, the configuration cache entry was stored and the second run was `UP-TO-DATE`. Output `build/reports/cyclonedx-direct/bom.json` (specVersion 1.6) [CITED: README]. Applying the plugin on the root and relying on its subproject registration, or its aggregate `cyclonedxBom`, is not what was tested and the README states `allprojects` is incompatible with Isolated Projects. **Apply the plugin per module inside a convention plugin and check per module**; a root task cannot read other projects' outputs under isolated projects.

Configuration that worked (Kotlin DSL, property assignment):
```kotlin
tasks.cyclonedxDirectBom {
    includeConfigs = listOf(".*RuntimeClasspath")      // 101 -> 76 components; drops compiler and tool classpaths
    jsonOutput = layout.buildDirectory.file("licences/bom.json").get().asFile
    xmlOutput.unsetConvention()
    includeBomSerialNumber = false
}
```
Observed licence data from that module (Axoniq starter modules, kotlin-reflect, Konsist, java-uuid-generator, testcontainers-postgresql, axoniq-testcontainer, axon-kotlin):
- Licence entry shapes: `{"license": {"id": "Apache-2.0"}}` (78×), `{"license": {"name": "Axoniq Terms of Service", "url": "https://www.axoniq.io/legal/terms-of-service"}}` (no SPDX id; 5 components), and by SPDX `expression` in other projects. A component can carry **several entries that are alternatives** (OR): `jakarta.annotation:jakarta.annotation-api` 3.0.0 lists both `EPL-2.0` and `GPL-2.0-with-classpath-exception`.
- Components under the D-31 "forbidden/unlisted" families in that sample: `GPL-2.0-with-classpath-exception` (as the alternative above), **`net.java.dev.jna:jna` 5.18.1 `LGPL-2.1-or-later` (test scope, via Testcontainers)**, plus `MIT-0` (`org.reactivestreams:reactive-streams`) and `EPL-2.0` (JUnit, `jakarta.annotation-api`), `BSD-3-Clause` (protobuf-java, hamcrest).
- Axoniq components found: `io.axoniq.framework:axon-server-connector`, `axoniq-distributed-messaging`, `axoniq-framework-bom`, `axoniq-testcontainer` (all 5.4.2) and `io.axoniq:axonserver-connector-java` 2026.1.1. NOTICE and the exception list must cover `io.axoniq.framework:*` and `io.axoniq:axonserver-connector-java`.

Policy design (file `governance/dependency-licences.json`, governed, so changes need the label): `allowed` SPDX ids (Apache-2.0, MIT, MIT-0, BSD-2-Clause, BSD-3-Clause, ISC, EPL-2.0, MPL-2.0 and **EPL-1.0**, which logback's dual licence needs), `forbidden` patterns (`GPL-*`, `AGPL-*` as bare ids; the `-with-classpath-exception` variants are acceptable only as an alternative), `aliases` (free-text names such as "The Apache Software License, Version 2.0" → `Apache-2.0`), and `exceptions` entries `{artifact: "group:name", license: "<verbatim name>", reason: "..."}`. Semantics: a component passes when **any** of its entries is allowed or covered by an exception; an entry that is neither allowed, aliased nor excepted fails with the coordinates and the licence string (fail closed on unknown names). The JNA case needs an explicit exception stating that JNA is dual-licensed Apache-2.0 OR LGPL-2.1-or-later and is used only through test-scope Testcontainers [the dual licence is [ASSUMED] from training knowledge; confirm against the JNA POM/README when writing the exception]. Add a root-level cacheable check that every exception's artifact appears in `NOTICE` (the "mirrored in NOTICE" requirement, mechanically) and `NOTICE` contains the Axoniq terms URL. Parsing uses `groovy.json.JsonSlurper` like the existing checks.

Fallback: jk1 `com.github.jk1.dependency-license-report` 3.1.5 (portal metadata); not tested here.

### Pattern 6: Upstream-record system (UPS-01..UPS-04, D-32, D-33)

**Record file** `docs/upstream/NNNN-<slug>.md`, front matter (flat scalars and flow lists only; the shared parser rejects anything else):
```yaml
---
id: UP-0001
product: axon            # the catalog/mise key the record watches (see UPS-04 below)
product-version: "5.4.2"
status: open             # open | reported | fixed-upstream | workaround-removed | wont-fix
workaround: true         # whether code or config in this repository works around it
upstream: []             # issue / PR URLs
reported: null
updated: 2026-11-22
---
```
Body sections (template): Symptom, Minimal reproduction, Root cause, Workaround (with code locations), Removal criteria, How to verify removal.

**Marker:** `UPSTREAM: UP-NNNN` in a comment next to the workaround. Consistency rules (two directions): every marker names an existing record; records with `workaround: true` and status in {`open`, `reported`, `fixed-upstream`} have at least one marker; records with status `workaround-removed` or `workaround: false` have none. **`wont-fix` with `workaround: true` keeps its markers** (the workaround becomes permanent); UPS-02 says "closed records have none", so confirm this reading with the author (Open Question 4). Scanner scope: tracked sources excluding `docs/upstream/**`, `docs/templates/**`, `.planning/**`, `.claude/**` and the check's own functional-test fixtures (they contain marker text); anchor the regex to a comment context to avoid prose false positives.

**Check implementation:** `@CacheableTask` in the governance package (governed path), wired into `courses.governance.gradle.kts`, with a shared `FrontMatter` object used by the ADR check (D-33/D-38). Build-logic carries no YAML library (the `ToolingTableCheck` precedent avoids a TOML parser), so the front-matter parser is a documented subset parser: `key: scalar` and `key: [a, b]`, quoted or bare strings, `null`; any other construct fails the check with the line. The index for the docs site is generated later (Phase 13); Phase 2 can emit `build/reports/governance/upstream-index.md` from the same parse.

**UPS-04 PR job (D-32):** a `pull_request` job in `ci.yml` (so under the required `ci-success`, `permissions: contents: read`) runs `scripts/upstream/changed-dependencies.sh <base-sha>`: `git diff` of `gradle/libs.versions.toml`, `mise.toml`, `gradle/wrapper/gradle-wrapper.properties`, `.nvmrc`, `.java-version`, `package.json`/`pnpm-lock.yaml` (later), extracts the changed **keys** (catalog `[versions]` aliases and mise tool keys), matches them against the `product` field of open records, and for each hit prints id, status and removal criteria to `$GITHUB_STEP_SUMMARY` and as `::warning title=Upstream record UP-NNNN::...`. It never fails and never comments (read-only token). Sanitise record text before emitting workflow commands (record content is repository-controlled, so strip `%`, CR and LF). Products without a pinned key (colima, GSD) simply never match: say so in the template. Tests: shell tests auto-discovered by the existing CI step (`scripts/test-*.sh`, `scripts/*/test-*.sh`).

Records to create now (collected from the spike wrap-up, the Phase 1 sources and `git grep`; all drafts only, publication of issues stays behind the UPS-03 approval gate):
1. UP-0001 decision-record capture: no public extension point (workaround: decorator on `@Internal` `EventStorageEngine`).
2. UP-0002 `ConflictDiagnosingEventStorageEngine` does not unwrap `CompletionException`.
3. UP-0003 dispatch-side retry cannot classify remote DCB conflicts (`AXONIQ-4005` text only).
4. UP-0004 `AnnotationBasedTagResolver` ignores Kotlin value classes (workaround `KotlinTagResolver`).
5. UP-0005 `@TargetEntityId` on a value-class property resolves the unboxed type (workaround: `UUID` id type registration).
6. UP-0006 empty `AXONIQ_PLATFORM_AUTHENTICATION` leaves a zombie Axon Server (`AxoniqConsoleCoordinationService.kt:69`); workaround: variable absent, `.env.example` line commented, `doctor` check.
7. Phase 1 candidates found by grep: the typed `libs` accessor workaround in `build-logic/build.gradle.kts` (`// Required for the typed `libs` accessor inside precompiled script plugins (still needed on` line 14, Gradle) and `trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"]` in `mise.toml` (published without provenance), and ADR-0002 (CodeQL Kotlin extractor range ends before Kotlin 2.4.21, re-enable criteria already documented, no code workaround). Host-level: colima AAAA DNS and Docker 29 containerd store (moby#52193, docker/cli#6457 already exist upstream; record with `workaround: false` or link only).

### Pattern 7: ADRs and templates (DOC-03, GH-12, D-34..D-38)

**MADR template, verified** [VERIFIED: raw `adr/madr` repo `template/adr-template.md`, release 4.0.0 of 2024-09-17]: YAML front matter keys `status`, `date`, `decision-makers`, `consulted`, `informed` (documented as optional metadata), then `# {short title}`, `## Context and Problem Statement`, optional `## Decision Drivers`, `## Considered Options`, `## Decision Outcome` with optional `### Consequences` and `### Confirmation`, optional `## Pros and Cons of the Options`, optional `## More Information`. `status` values in the template: `proposed | rejected | accepted | deprecated | … | superseded by ADR-0123`. The MADR `date` means "when the decision was last updated"; D-36 keeps existing dates.

Project profile for the check (D-38): required keys `status`, `date`, `decision-makers`; allowed statuses `proposed`, `accepted`, `rejected`, `deprecated`, `superseded by ADR-NNNN` (link must resolve); required sections `Context and Problem Statement`, `Considered Options`, `Decision Outcome`; filename `NNNN-slug.md` with `# ADR-NNNN: …` title carrying the same number (existing titles read `# ADR-0001: Workflow permissions and required status checks`); numbering from 0001 without gaps or duplicates; `docs/templates/` excluded. Prettier owns Markdown and YAML layout (`.prettierrc.yaml`: `printWidth: 120`, `proseWrap: preserve`), so front matter must be Prettier-stable (double-quoted strings).

Normalisation of ADR-0001..0004: their current layout is `- Status:`/`- Date:`/`- Deciders:` bullets, a "This is a minimal record…" note, then Context/Decision/Consequences and custom sections (ADR-0002 has `## Re-enable criteria`, ADR-0003/0004 other custom headings). Map Decision → Decision Outcome, Consequences → Consequences, Re-enable criteria/When to revisit → Confirmation or More Information, rejected alternatives → Considered Options. Content, dates and decisions stay unchanged.

New ADRs (numbers continue at 0005): 0005 Axoniq licence behaviour and Platform token delivery (author-verified 12 h observation alongside spike 004; the quick start `docs/quick-start.md` carries the same two lines), 0006 decision-record capture approach (variant A, `@Internal` safeguards, per-slice stamping fallback), 0007 Kotlin serialization and TypeID conventions (own JVM codec, `typeid-js` for TypeScript, IDEM-04 wording fix, value-class tag/entity-id wiring, message naming and versioning including the one-way D-03), 0008 buildpack constraints (frontend prebuilt `dist/`, pnpm support not needed, BPE_* defaults, colima blockers). Recommended extras because DOC-03 says "every significant decision": 0009 repository layout and Gradle project naming (D-01), 0010 test-suite taxonomy and `check` semantics (D-15, D-19, D-20), and one for the licence allowlist policy.

Templates (`docs/templates/`): `adr.md` (MADR, front matter placeholders valid YAML strings), `doc-page.md`, `event-model-slice.md`, `upstream-record.md`, `upstream-issue.md` (minimal reproduction skeleton). Exclude the directory from the ADR, marker and Prettier-sensitive checks as needed.

### Pattern 8: `doctor` (TOOL-03, TOOL-04, AIDEV-07, D-27..D-29)

`scripts/doctor.sh`: bash, shellcheck/shfmt clean, `--ci` flag, one function per check printing `ok|warn|fail|skip` plus the exact fix command, exit 1 on any `fail`, never mutates anything. Delegates to `./gradlew doctor` at the end (skipped with a warning when the JDK check already failed).

| Check | Method (verified where noted) | Severity |
|-------|------------------------------|----------|
| mise present, pinned tools installed | `mise ls --current --json`: each tool has `"installed": true` and `"active": true`, with `requested_version` [VERIFIED: ran locally, 17 tools]; `mise lock --check` exists for lock freshness [VERIFIED: `mise lock --help`] | fail (local) |
| JDK vs `.java-version`, Node vs `.nvmrc` | `java -version` major equals `.java-version`; `node --version` major equals `.nvmrc` | fail |
| formatter binaries | `shfmt`, `taplo` on PATH (what `VerifyFormatterBinary` guards) | fail |
| Docker engine reachable | `docker info --format '{{json .}}'`; colima or Docker Desktop by `docker context show` and `DockerEndpoint` | fail |
| Docker image store | classic store has driver `overlay2`; the containerd store shows `Driver: overlayfs` and a `driver-type` entry `io.containerd.snapshotter.v1` in `DriverStatus` [CITED: spike 005; locally `DriverStatus` printed no `driver-type`] | warn |
| colima DNS | `~/.colima/default/colima.yaml` has a non-empty `network.dns` list (AAAA forwarder issue) and `docker.features.containerd-snapshotter: false` [VERIFIED: file has `dns:` and `containerd-snapshotter: false` here]; no container pull needed | warn (local, colima only) |
| Testcontainers config | `DOCKER_HOST`/`~/.testcontainers.properties` (`tc.host`, `docker.host`), `TESTCONTAINERS_RYUK_DISABLED`; real connectivity in the Gradle part | warn/fail |
| GSD installation | verify `.claude/gsd-file-manifest.json`: `manifestVersion 2`, `files` is a path→SHA-256 map relative to `.claude/` [VERIFIED: 858 of 858 entries matched `sha256(file bytes)` this session]. Report modified (hash differs), missing, and unexpected files under `gsd-core`, `agents`, `commands`, `hooks`, `scripts`; two files are present but unlisted in the current install: `hooks/package.json` and `scripts/changeset/README.md`, so keep a short allowlist or limit "unexpected" to executable hook extensions [VERIFIED: listing]. `skills/spike-findings-*` is the tracked D-39 exception and is not GSD's. Needs `jq` (mise-pinned) or fall back to a fail with `mise install` hint | fail (local) |
| `~/.gsd/defaults.json` leaking keys | warn on any key; here the file holds `resolve_model_ids: omit` and `runtime: opencode`, flagged by `~/.gsd/defaults-provenance.json` as set for the `opencode` runtime. GSD merges this file into config in install-time resolvers and when no project config exists [CITED: `gsd-core/bin/lib/commands.cjs` lines 778-806, `config-loader.cjs` lines 424 and 683]; which exact keys leak into this project is [ASSUMED] | warn |
| MCP servers | `.mcp.json` entries resolve: `mise which context7-mcp` / `playwright-mcp` succeed | warn |
| Platform token | `.env` defines `AXONIQ_PLATFORM_AUTHENTICATION` non-empty → ok; absent → warn ("12 h window"); defined **empty** → fail with fix. Never print the value | warn/fail |
| Claude Code version | `claude --version` ≥ 2.1.324, required to pin marketplaces by commit SHA; this machine is **2.1.323** [VERIFIED: `claude --version`; CITED marketplace reference] | warn |

`doctor` must not read `.env` content into logs: a Claude Code secret-read guard in this environment blocked a command that merely referenced `.env`; the script should test only the *presence and emptiness* of the one variable with `grep -c`/`awk` on the key, never echo the file. `--ci` skips: GSD, MCP, Platform token, colima, Claude Code version, `~/.gsd`.

JVM part: `:tooling:doctor` (library depending on `org.testcontainers:testcontainers` only) with a `doctor` task that runs `DockerClientFactory.instance().client()`; the prototype showed that this reaches Docker through the author's configuration and starts Ryuk [VERIFIED: probe test]. The `VerifyFormatterBinary`/`VerifyNodeVersion` tasks stay (they gate Spotless early); `doctor.sh` re-implements the same facts for the pre-Gradle stage. `./gradlew doctor` task: register `outputs.upToDateWhen { false }`, a group and a description (every convention-registered task needs both), not part of `check`.

### Pattern 9: AI tooling (D-39..D-42, AIDEV-01/03/04)

**MCP via mise (verified end to end in an isolated mise data directory):** with `"npm:@upstash/context7-mcp" = "4.7.0"` and `"npm:@playwright/mcp" = "0.0.85"` plus `node = "26.13.1"`, `mise install` succeeded with **no trust-policy exclusions**; `mise which` resolved `…/node_modules/.bin/context7-mcp` and `…/playwright-mcp`; `mise lock` wrote `[[tools."npm:@playwright/mcp"]]` entries with `aube = { path = ".mise/locks/npm-playwright-mcp/0.0.85", digest = "sha256:…" }` sidecar lockfiles, the same shape as the repository's `npm:renovate` entry. The sidecar directories are governed (`.mise/locks/**`). In the real repository run `mise lock --platform linux-x64,linux-arm64,macos-arm64,macos-x64` after adding the tools (the Renovate PR note for Node already says this) and add `docs/tooling.md` rows whose Tool cell holds the exact keys in backticks (`checkToolingInventory`).

`.mcp.json` (project scope, governed):
```json
{
  "mcpServers": {
    "context7": { "command": "mise", "args": ["x", "--", "context7-mcp"] },
    "playwright": { "command": "mise", "args": ["x", "--", "playwright-mcp", "--isolated", "--output-dir", ".playwright-mcp"] }
  }
}
```
Facts behind it: Context7 reads `CONTEXT7_API_KEY` from the process environment (`--api-key` or the env var per `--help`), so the file needs no `env` block, and referencing an unset `${VAR}` without a default makes Claude Code pass the **literal** `${VAR}` text [CITED: code.claude.com/docs/en/mcp]; therefore do not write `${CONTEXT7_API_KEY}` in `.mcp.json`. Whether the free tier works without a key is [ASSUMED]. Playwright MCP flags verified from `--help`: `--isolated` (in-memory profile), `--headless`, `--output-dir`, `--allowed-origins`/`--blocked-origins` (not a security boundary), `--secrets`. The stdio working directory is not documented; if `mise x` runs outside the repository it will not see `mise.toml`: use `mise -C "${CLAUDE_PROJECT_DIR:-.}" x -- …` [`mise -C` and `CLAUDE_PROJECT_DIR` expansion are CITED/ASSUMED respectively; prove in Wave 0]. Project-scope servers prompt for approval in interactive sessions [CITED]; do not pre-approve via `enabledMcpjsonServers` in the committed settings (it also defeats AIDEV-04's approval intent). Playwright MCP needs a browser binary that `npm install` does not provide; document the install step (its `browser_install` tool or `--browser`/channel flags). Renovate supports the mise `npm` backend [CITED: docs.renovatebot.com/modules/manager/mise]. **Pitfall:** `renovate.json5` automerges patch updates of mise tools; every Playwright MCP release is `0.0.N`, i.e. a patch bump, so those MCP servers would automerge. Add a package rule disabling automerge for `@playwright/mcp` and `@upstash/context7-mcp` (the classification of 0.0.x as patch is [ASSUMED]).

**Skills survey and pinning.** Claude Code pins a marketplace by commit through `extraKnownMarketplaces` with `ref` set to the full 40-character SHA, which requires Claude Code **v2.1.324 or later** (older versions cannot load such a marketplace) [CITED: code.claude.com/docs/en/plugins/marketplace-reference.md]; this machine has 2.1.323 [VERIFIED]. `ref` plus a relative-path plugin pins the plugin because the marketplace repository is the plugin source (both candidate marketplaces use relative sources). Settings shape [CITED, same page]:
```json
{
  "extraKnownMarketplaces": {
    "axoniq": { "source": { "source": "github", "repo": "AxonIQ/agent-skills", "ref": "59e7beb6bb487894aeb1294918463af1432bdece" } }
  },
  "enabledPlugins": { "axoniq-app-development@axoniq": true }
}
```
(Keys for `enabledPlugins` and any auto-update setting are in the settings reference, which was not read to the end; confirm in Wave 0.) Edits to `.claude/settings.json` are an `ask` rule and a governed path.

Candidates reviewed (verdicts are proposals for the author; nothing was installed):

| Area | Candidate | Evidence | Proposed verdict |
|------|-----------|----------|------------------|
| Axon | `AxonIQ/agent-skills`, marketplace `axoniq`, plugin `axoniq-app-development` | Official AxonIQ org, Apache-2.0, 2 stars, last push 2026-11-05, HEAD `59e7beb6bb487894aeb1294918463af1432bdece` (2026-10-12); the plugin tree is markdown plus `plugin.json` only (no hooks, scripts or MCP config); covers entities, DCB decision models, event store, testing; README warns output can hallucinate [VERIFIED: gh api tree listing] | Install after approval, pinned to the SHA; treat guidance as a hint, our spike skill wins on conflicts |
| Axon | `axoniq-migration`, `axoniq-framework-contribution` | Same repo; aimed at 4→5 migration and AF contributors | Reject (not applicable) |
| Gradle | `gradle/gradle-skills` marketplace/plugin `gradle-skills`, tag 1.1.0 commit `00b7790139dff32063976f98334b2a2301b50a9d` | Official Gradle, Apache-2.0, 80 stars; SKILL.md + references + an `evals/` folder + `.github/scripts/check-consistency.py` (CI only); skill says it reads bundled files and fetches nothing at run time; covers isolated-projects-style practices (e.g. `avoid_after_evaluate`, `do_not_use_internal_apis`) | Install after approval, pinned; run its audit once against `build-logic` as a review input |
| Kotlin | `Kotlin/kotlin-agent-skills` | Official org, Apache-2.0, 1133 stars; skills are JPA entity mapping and AGP9 migration; plugin bundles `.mcp.json` pointing at remote `https://mcp.kotlinlang.org/mcp` (unpinned remote server) | Reject for now (not applicable: no JPA, no Android; remote MCP unpinned) |
| Kotlin idioms | none official found | search found only aggregator listings of community skills | No skill; rely on Context7 for Kotlin docs |
| Spring Boot | `spring-projects/spring-tools` Claude plugin (`claude-plugins/spring-tools`) | Official; EPL-1.0; installs from a CDN marketplace URL (`https://cdn.spring.io/spring-tools/release/claude-plugins/marketplace.json`, not a git ref), downloads a language-server JAR at run time, ships hooks and MCP tools, needs Java 21+ | Defer: cannot be pinned by commit, downloads and runs code, hooks; re-evaluate when it can be pinned |
| Spring Boot | community Spring skills (several listings) | unvetted | Reject |
| GitHub Actions | `trailofbits/skills` `agentic-actions-auditor` | 8344 stars, CC-BY-SA-4.0, targets workflows that invoke AI agents; this repository has none; zizmor and actionlint already gate CI | Reject (no benefit) |
| GitHub Actions | `Seldaek/zizmorify`, `alexop-skills` harden-github-actions | duplicate zizmor, which CI runs | Reject |
| Playwright | `microsoft/playwright-cli` skills | Official, Apache-2.0, 15k stars | Defer to Phase 6/7 per D-40 |
| React/shadcn, FSD | shadcn-ui/ui (MIT), `feature-sliced/skills` (121 stars, no licence field) | not reviewed beyond metadata | Defer to their phases; FSD skill licence unclear |
| Paketo | none found | | None |

Review criteria to record in `docs/tooling.md` and apply per candidate: source owner and licence; contents are markdown only (no hooks, no scripts, no MCP/LSP config, no network fetches at run time); pinned by full commit SHA (never a branch or `@latest`); update is a deliberate PR (no auto-update); minimum Claude Code version; what it can read or execute; the author's explicit approval recorded in the PR. Whether third-party marketplaces auto-update by default is [ASSUMED]; pinning `ref` to a SHA makes updates deliberate anyway.

**`agent_skills` mapping (D-41):** the research cannot choose it; the phase task proposes it after the survey and the author approves. Edits to `.planning/config.json` and `governance/gsd-config-policy.json` land together (the policy pins the whole file; `agent_skills: {}` is the current value in both) and ADR-0004 is updated.

### Repository layout move (focus item 12)

Tracked references that mention the old layout or the sample module (from `git grep -n -E "convention-sample|tooling/|platform/|services/|contracts/"`, excluding `.planning`, `.claude`, `docs/adr`):

| File | Line | Text | Action |
|------|------|------|--------|
| `settings.gradle.kts` | 8-11, 13-14 | `":platform",` `":tooling:convention-sample",`; `// Anticipated role-based layout (D-03); each phase adds the modules it needs:` / `//   services/{command,query,automation,bff}, contracts/*, frontend, docs-site` | Replace includes, add `projectDir` mappings, rewrite comment to `backend/…` (governed path) |
| `qodana.yaml` | 30-32 | `- build-logic/build`, `- platform/build`, `- tooling/convention-sample/build` | Replace the sample line with one `build` path per new module (`docs/tooling.md` says a new module adds its build directory here) |
| `performance.scenarios` | 28 | `apply-abi-change-to = "tooling/convention-sample/src/main/kotlin/io/github/aanbrn/courses/sample/Greeting.kt"` | Point to a Kotlin main file of a real module (for example the kernel TypeID file); `docs/performance.md` line 75 describes "Recompiles `convention-sample`" and must be reworded |
| `scripts/governance/test-check-pr.sh` | 861, 894 | `boundary_case tooling/convention-sample/build.gradle.kts ungoverned`, `boundary_case tooling/convention-sample/mise.toml ungoverned` | Test-only path strings; keep or repoint to a real module (governed path: `scripts/governance/**`) |
| `build-logic/build.gradle.kts` | 62 | `"../platform/build.gradle.kts",` (TestKit input) | unchanged (platform stays top level) |
| `.github/workflows/*`, `renovate.json5`, `REUSE.toml`, `.gitattributes` | n/a | no path filters or module paths | none (CI has no path filters on purpose; REUSE uses `**`; `.gitattributes` mentions `frontend/**/generated/**` and `docs-site/**`, unchanged) |
| `.idea/` | n/a | untracked (`/.idea/` in `.gitignore`) | IDE run configurations are Phase 6 (INFRA-15) |
| `tooling/convention-sample/**` | n/a | deleted (Phase 1 D-22) when the first real module lands, after the three references above are repointed | delete in the same PR |

**Runtime State Inventory:** none of the five categories applies (no databases, live services, OS registrations, secrets or installed artifacts carry module paths); local `build/` directories and `.gradle` state of the removed sample module are untracked and disappear on `clean`.

### Anti-Patterns to Avoid
- Applying CycloneDX (or any per-module plugin) from the root with `allprojects`: incompatible with isolated projects.
- Declaring `docker.axoniq.io/...:latest` anywhere, or any image literal in a test or compose file (INFRA-09).
- A Konsist rule on an empty or wrong scope without `strict = true` and a scope sanity test.
- Reading files outside the module in a test task without declaring them as inputs (stale cached architecture results).
- `env_file: .env` on the Axon Server service (leaks every variable into the container); an empty `AXONIQ_PLATFORM_AUTHENTICATION`.
- Concurrency tests dispatched from one thread.
- A helper to read the whole `.env` in `doctor` (secrets must not reach logs).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Axon Server container with DCB context | Hand-written `GenericContainer` plus init HTTP calls | `AxonServerContainer(...).withDevMode(true).withDcbContext(true)` with the catalog image | Handles log wait, cross-process init lock, reuse re-check |
| SBOM and licence extraction | A Gradle dependency walker that parses POMs | CycloneDX `cyclonedxDirectBom` per module, then a small policy comparison | Plugin resolves POM licences, parents and aliases; verified under CC and IP |
| UUIDv7 | Own time-ordered UUID | `java-uuid-generator` `timeBasedEpochGenerator()` (accepts a clock) | Monotonic edge cases |
| Property-based tests | Random loops with seeds | `kotest-property` `checkAll` inside `runBlocking<Unit>` | Shrinking, reproducible seeds |
| Property-based tests running in JUnit | Kotest runner | JUnit + `kotest-property` (only the property library) | One test style (project decision) |
| Conflict retry | A hand-written retry loop around command dispatch | `RetryingCommandBus` + `FilteringRetryPolicy` at `CommandBus` order `MIN+10` (spike 001) | Axon's own scheduler, correct placement |
| Tag resolution for value classes | Reflection in each event | One `KotlinTagResolver` component | Default resolver silently writes raw UUIDs |
| JSON for config files | Own JSON parser in build-logic | `groovy.json.JsonSlurper` (Phase 1 precedent) | Already on the Gradle classpath |
| Docker host discovery in `doctor` | Parsing `docker info` text for Ryuk reachability | `DockerClientFactory.instance().client()` | Same code path as the tests |
| Hash verification of GSD files | Custom archive format | `sha256` over files listed in `gsd-file-manifest.json` | Format verified: path→hex map |

**Key insight:** TypeID Crockford encoding is the one thing hand-rolled on the JVM, by decision (D-07): no mature JVM library exists (spike 002), the codec is ~60 lines and checked by all 30 official vectors plus 100,000 random round trips.

## Common Pitfalls

### Pitfall 1: Custom suites silently outside `check`
**What goes wrong:** `componentTest` and `integrationTest` exist but `./gradlew check`/`build` never run them; executors report green without integration tests (contradicts D-19).
**Why:** Gradle wires only `test` into `check` [VERIFIED: `check --dry-run` before and after the wiring line].
**How to avoid:** the `tasks.named("check") { dependsOn(...) }` line; a TestKit test that a failing `integrationTest` fails `check`.
**Warning signs:** `check --dry-run` lacks `:module:integrationTest`.

### Pitfall 2: detekt and docs do not see the new source sets
**What goes wrong:** undocumented or badly named code in `componentTest`, `integrationTest`, `architectureTest`, `testFixtures` is never analysed; or detekt starts failing on prose test names there.
**Why:** `detekt` analyses `main` and `test` only [VERIFIED: prototype]; the excludes in `config/detekt/detekt.yml` match `**/test/**` and `**/*FunctionalTest.kt`.
**How to avoid:** `detekt { source.from("src/componentTest/kotlin", "src/integrationTest/kotlin", "src/testFixtures/kotlin") }` in the convention and matching excludes; keep `testFixtures` documented.

### Pitfall 3: `kotest-property` tests silently skipped
**What goes wrong:** `@Test fun \`x\`() = runBlocking { checkAll(Arb.uuid()) { … } }` compiles and passes but never runs.
**Why:** the function returns the non-`Unit` result of `checkAll`, so JUnit does not treat it as a test method [VERIFIED: prototype: `TEST-k.PropTest.xml` absent until `runBlocking<Unit>`, then `tests="1"`].
**How to avoid:** `runBlocking<Unit> { … }` or a block body; add a Konsist/architecture rule that `@Test` functions return `Unit`; assert test counts in a TestKit or smoke check for the first property test.

### Pitfall 4: Wrong or empty Konsist scope passes
**What goes wrong:** a rule never inspects anything and stays green.
**Why:** `strict = false` is the default and passes on empty lists; unknown modules yield an empty scope.
**How to avoid:** `strict = true`, a scope sanity meta test, `scopeFromDirectory("backend/...")` paths relative to the repository root.

### Pitfall 5: Architecture test result cached over rule violations
**Why:** sources scanned outside the module are not task inputs. **How to avoid:** declare them (see Pattern 3).

### Pitfall 6: Zombie Axon Server from an empty token
**What goes wrong:** container `running`, HTTP and gRPC dead after `cp .env.example .env`.
**Why:** an empty `AXONIQ_PLATFORM_AUTHENTICATION` is "configured" (spike 004; compose passes `X=` as an empty string [VERIFIED]).
**How to avoid:** commented line in `.env.example`, bare name under `environment:`, `doctor` failure on an empty value, a compose healthcheck that turns the zombie into `unhealthy`.

### Pitfall 7: PostgreSQL 18 data volume
**What goes wrong:** container fails or starts with an empty database after mounting `/var/lib/postgresql/data`.
**Why:** the image's volume and `PGDATA` changed in 18 [CITED: docker-library/postgres PR 1259, issues 1370 and 1377].
**How to avoid:** mount `/var/lib/postgresql`.

### Pitfall 8: Marketplace SHA pin on an older Claude Code
**What goes wrong:** the pinned marketplace fails to load on Claude Code 2.1.323.
**How to avoid:** require ≥ 2.1.324 (`doctor` warning, inventory row) before merging the settings change [VERIFIED local version, CITED requirement].

### Pitfall 9: Testcontainers' own images float
**What goes wrong:** the Ryuk image (`testcontainers/ryuk:0.14.0` in the probe) and the internal tiny image are not catalog-pinned, breaking INFRA-09's "all container images" rule.
**How to avoid:** pass `TESTCONTAINERS_RYUK_CONTAINER_IMAGE` (property `ryuk.container.image`, documented [CITED: java.testcontainers.org/features/configuration]) from a catalog entry with digest; add an entry for the tiny image if the checks start it (property name [ASSUMED]).

### Pitfall 10: Dual licences read as violations
**What goes wrong:** the licence check flags `jakarta.annotation-api` (EPL-2.0 plus GPL-2.0-with-classpath-exception), logback (EPL-1.0 plus LGPL-2.1) or JNA (LGPL-2.1-or-later) although a permissive alternative exists.
**How to avoid:** OR semantics across a component's entries, `EPL-1.0` in the allowed list, an explicit JNA exception.

### Pitfall 11: Renovate automerges agent tooling
See Pattern 9: `0.0.N` Playwright MCP releases are patch bumps and the existing rule automerges patch updates of mise tools.

### Pitfall 12: `SharedAxonServerContainer` and `:latest`
Using it pulls an unpinned floating image in CI and locally.

### Pitfall 13: Governed-path churn
The PR touches governed files (`settings.gradle.kts`, `.github/**`, `mise.toml`, `.mcp.json`, `.claude/settings.json`, `governance/**`, `renovate.json5`); `governance-success` fails without the label, and the `checkClaudeAskRules` task fails if a new governed entry has no matching ask rule.

### Pitfall 14: Marker text in fixtures and templates
The upstream check's own tests and `docs/templates/` contain `UPSTREAM: UP-NNNN`; exclude them from the scanner or the build fails on its own examples.

## Code Examples

### Compose pass-through of the optional token and versions include
```yaml
# compose.yaml
name: courses
include:
  - path: compose/infra.yaml
    env_file: compose/versions.env
```
```yaml
# compose/infra.yaml  (verified shape; values from spike 003)
services:
  axonserver:
    image: ${AXONSERVER_IMAGE:?run ./gradlew generateComposeVersions}
    hostname: axonserver
    environment:
      - AXONIQ_PLATFORM_AUTHENTICATION        # absent from .env => not set in the container (verified)
      - AXONIQ_AXONSERVER_STANDALONE_DCB=true
      - AXONIQ_AXONSERVER_DEVMODE_ENABLED=true
    healthcheck:
      test: ["CMD", "curl", "-fsS", "-o", "/dev/null", "http://localhost:8024/actuator/health"]
      interval: 5s
      timeout: 3s
      retries: 30
      start_period: 10s
    ports: ["8024:8024", "8124:8124"]
    volumes: [as-data:/axonserver/data, as-events:/axonserver/events, as-log:/axonserver/log]
  postgres:
    image: ${POSTGRES_IMAGE:?run ./gradlew generateComposeVersions}
    volumes:
      - pg-data:/var/lib/postgresql              # PostgreSQL 18+: mount here, not .../data
      - ./postgres/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 5s
      timeout: 3s
      retries: 20
volumes: { as-data: {}, as-events: {}, as-log: {}, pg-data: {} }
```
(Mount paths for the init script are relative to the included file's directory; confirm in Wave 0.)

### Licence entry reader (shapes seen in the SBOM)
```kotlin
// each component: licenses = [ {license:{id}} | {license:{name,url}} | {expression} ]  -> alternatives (OR)
fun entries(component: Map<*, *>): List<String> =
    (component["licenses"] as? List<*>).orEmpty().mapNotNull { entry ->
        val e = entry as Map<*, *>
        (e["license"] as? Map<*, *>)?.let { it["id"] ?: it["name"] } ?: e["expression"]
    }.map(Any::toString)
```

### Kotest property under JUnit
```kotlin
@Test
fun `every generated course id survives a TypeID round trip`() = runBlocking<Unit> { // Unit is mandatory
    checkAll(Arb.uuid()) { uuid -> CourseId.parse(CourseId(uuid).toString()) shouldBe CourseId(uuid) }
}
```

### Konsist rule skeleton (API surface verified in the sources jar)
```kotlin
@Test
fun `contracts import no Spring and no Axon logic`() {
    Konsist.scopeFromDirectory("backend/contracts").files.assertTrue(strict = true) { file ->
        file.imports.none { it.name.startsWith("org.springframework") } // Axon: allow only annotation packages
    }
}
```

### MCP binary resolution
```bash
mise x -- context7-mcp --help      # resolves node_modules/.bin/context7-mcp from mise.lock'ed install (verified)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `SharedAxonServerContainer` for all suites | Own pinned singleton | 5.4.0 added the shared holder with `:latest` | Cannot satisfy INFRA-09 |
| JaCoCo/kotest runner mixes | JUnit + Kotest assertion/property libraries | project decision | One runner, silent-skip pitfall for property tests |
| Docker Hub-only Postgres volume path | `/var/lib/postgresql` for 18+ | PostgreSQL 18 image | Mount path change |
| Pin marketplaces by branch/tag | `ref` as full commit SHA | Claude Code 2.1.324 | Machine needs update |
| Konsist 0.17/0.18 API | 1.0.0-beta.1 (API freeze candidate, JDK 11+, hidden directories not scanned) | 2026-11-04 | Rules may need `strict` handling; Kotlin 2.4 no longer compiles context receivers |

**Deprecated/outdated:** `de.fxlae:typeid-java` and other JVM TypeID libraries (idle or old spec); `BPE_DEFAULT_JAVA_TOOL_OPTIONS` for Paketo (ignored); an init container for the DCB context (double init hazard).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Gradle 9 fails or warns for a nested project path whose intermediate directory does not exist | Project structure | Low: flat names chosen anyway, the verified prototype avoids the question |
| A2 | `Test.systemProperty(name, provider)` does not unwrap lazily/track as input; `CommandLineArgumentProvider` with `@Input` is the safe route | Pattern 1 | Medium: cache correctness of image changes; TestKit test required |
| A3 | `org.testcontainers.postgresql.PostgreSQLContainer` is the 2.x class and name | Pattern 2 | Low: compile error caught in Wave 0 |
| A4 | JNA is dual-licensed Apache-2.0 OR LGPL-2.1-or-later | Pattern 5 | Medium: the exception text must be accurate; confirm from the JNA POM/README |
| A5 | Postgres image init scripts run only on an empty data directory | Pattern 4 | Low: standard image behaviour |
| A6 | Docker Compose is preinstalled and recent enough for `include:` per-include `env_file` on ubuntu runners | Pattern 4, CI | Medium: lint step fails; pin via action or document |
| A7 | Konsist does not reliably read `*.kts` build scripts | Pattern 3 | Low: plain text check used instead |
| A8 | Free tier of Context7 works without an API key | Pattern 9 | Low: documentation note only |
| A9 | `mise -C <dir> x --` and `${CLAUDE_PROJECT_DIR:-.}` expansion work in `.mcp.json` args; stdio cwd is the project root otherwise | Pattern 9 | Medium: MCP servers fail to start; Wave 0 proof |
| A10 | Renovate treats `0.0.N` bumps as `patch` | Pitfall 11 | Medium: an unneeded or missing no-automerge rule |
| A11 | Third-party marketplaces do not auto-update by default | Pattern 9 | Low: SHA pin makes it moot |
| A12 | GSD merges which exact `~/.gsd/defaults.json` keys into this project | Pattern 8 | Low: doctor warns on any key |
| A13 | Testcontainers tiny-image property name `tinyimage.container.image` | Pitfall 9 | Low |
| A14 | `Gradle`/Renovate can resolve `docker.axoniq.io/...` references for image updates | Catalog images | Medium: fall back to the Docker Hub name `axoniq/axonserver` (same digest) |
| A15 | `kotest-property` with Kotest 6.2.7 needs no extra runtime beyond transitive coroutines under JUnit | Standard stack | Low: prototype ran |
| A16 | Reuse of the Axon Server container across Gradle module JVMs (reuse flag) behaves as the shared holder's comments describe | Pattern 2 | Low: reuse is local and optional |

## Open Questions

1. **Where do the Axon wiring and the permanent Axon Server tests live in Phase 2?**
   - Known: D-12 moves the INFRA-04 conflict test, the INFRA-06 round trip and the decision-capture test into real modules; DEMO-01 itself is Phase 3; no service module exists yet; spike code is `KotlinTagResolver`, capture decorator, retry enhancer, ID entity registration.
   - Unclear: a module for them. Contracts must stay Axon-free at runtime (D-04).
   - Recommendation: create `:command-service` (`backend/command-service`) as a plain Kotlin library in Phase 2 holding `KotlinTagResolver`, the capture decorator with an in-memory recorder interface, the retry `ConfigurationEnhancer`, and the integration tests; Phase 3 turns it into the Spring Boot application and persists records. INFRA-04 test body should use only the public append/source API. Confirm with the author because it is the first service-module code.
2. **Where does the shared Jackson module live and how do sealed results get type info?**
   - Known: spike 002 used a mix-in inside one module referencing all contract types; D-02's graph says the query service must not depend on commands and the BFF must not depend on events.
   - Recommendation: `:serialization-jackson` (`backend/serialization`) depending on the kernel only (id serializers, `jackson-databind`, `jackson-module-kotlin`), with `@JsonTypeInfo` placed directly on sealed result types as a `compileOnly` `jackson-annotations` metadata annotation in `:contracts-commands` (extends D-04's metadata rule to Jackson annotations). The spike's mix-in design (no Jackson in contracts) is the alternative but forces one module to depend on every contract kind. Needs the author's choice.
3. **Konsist 1.0.0-beta.1 or 0.18.1?** Recommendation: beta.1, with `strict = true` rules and the scope sanity test; revert to 0.18.1 if the API surface used here is missing.
4. **UPS-02 semantics for `wont-fix`.** A workaround that must stay forever keeps its markers; UPS-02 reads "closed records have none". Recommendation: only `workaround-removed` forbids markers; confirm.
5. **How deep must the D-26 smoke go?** Recommendation: host scenario functional (the INFRA-04 test against `localhost:8124`) plus network reachability from a sibling container; real in-network gRPC clients arrive with Phase 3/6 services. Escalate only if the author wants a probe container with the spike's JRE image.
6. **`.env.example` wording.** D-25 says "an empty value"; verified behaviour requires a commented line. Recommendation: commented line plus explanation; update the D-25 wording in ADR-0005.
7. **IDEM-04 text.** Replace "Kotlin and TypeScript generators come from vetted libraries" with "Kotlin uses an own codec verified against the official vectors (no mature JVM library); TypeScript uses the official `typeid-js`" and record it in ADR-0007.
8. **Claude Code upgrade** to ≥ 2.1.324 before the SHA-pinned marketplace settings merge (author action).
9. **Spring Tools plugin** deferred; revisit after it offers a pinnable source.
10. **Renovate for the SHA-pinned marketplace refs:** a regex manager on `.claude/settings.json` (git-refs datasource) is plausible but untested; recommendation: manual deliberate updates, listed in the inventory.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK (Temurin) | Gradle build, Testcontainers tests | ✓ | 25.0.5.1 (matches `.java-version` 25) | none needed |
| Gradle wrapper | build | ✓ | 9.9.2 (`gradle-wrapper.properties`) | none |
| Node.js | Prettier via Spotless, mise `npm:` tools | ✓ | v24.20.0 via nvm on PATH (the repo's `.nvmrc` is `26`, mise pins 26.13.1) | `doctor` must flag the nvm/`.nvmrc` mismatch (Spotless's `verifyNodeVersion` already enforces) |
| mise | tool pins, MCP servers | ✓ | 2026.11.12 | none |
| Docker engine (colima) | Testcontainers, compose | ✓ | client 29.10.0, server 29.5.2, overlay2 (classic store) | Docker Desktop (documented, unverified here) |
| Docker Compose | compose, `config` validation | ✓ | v2.32.1 | none |
| `gh` CLI | research and `/gsd-ship` | ✓ | authenticated | none |
| Claude Code | skills/MCP | ✓ but below the pin requirement | 2.1.323 (needs ≥ 2.1.324 for SHA-pinned marketplaces) | upgrade |
| `docker buildx` | resolving manifest digests | ✗ | — | Docker Hub API (used here) or `docker manifest inspect` |
| `pnpm` | frontend (Phase 5) | ✗ | — | not needed in Phase 2 |
| colima DNS `network.dns` | Paketo builds (Phase 6) | present in `colima.yaml` | — | `doctor` warns when empty |
| Network access to Docker Hub, Maven Central, npm | CI and tests | ✓ here | — | — |

**Missing dependencies with no fallback:** none for Phase 2 execution; Claude Code must be upgraded before the SHA-pinned marketplace change.
**Missing dependencies with fallback:** `docker buildx` (use the Docker Hub tag API for digests, as done here).

## Validation Architecture

Nyquist validation is enabled (`workflow.nyquist_validation: true` in `.planning/config.json`).

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 6.1.3 (Jupiter) + Kotest assertions 6.2.7 (+ `kotest-property`); Gradle TestKit for `build-logic`; Konsist 1.0.0-beta.1 for architecture; Testcontainers 2.0.5 |
| Config file | `build-logic/build.gradle.kts` (`functionalTest` suite, exists); `courses.test-suites` (Wave 0, new) |
| Quick run command | `./gradlew :contracts-kernel:test :tooling:architecture:architectureTest :build-logic:functionalTest --tests '*Upstream*' --tests '*Adr*' --tests '*Licence*' --tests '*TestSuites*'` |
| Full suite command | `./gradlew build` (runs every suite including `integrationTest`, D-19) plus the lint-job commands (`reuse lint`, `editorconfig-checker`, `docker compose config --quiet`, `actionlint`, `zizmor`) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INFRA-02 | Contracts have no Spring/Axon-logic imports; Axon only `compileOnly` | architecture | `./gradlew :tooling:architecture:architectureTest --tests '*ContractsArchitectureTest*'`; fails when a contracts file imports `org.springframework` | ❌ Wave 0 |
| INFRA-03 | One command starts pinned Axon Server (DCB, hostname) and PostgreSQL | compose lint + smoke | `docker compose config --quiet` (CI lint); local/CI smoke `docker compose up -d --wait` then `./gradlew integrationTest -Pcourses.axonserver.external=localhost:8124 --tests '*ConditionalAppendIntegrationTest*'`; fails on unhealthy services or a missing DCB context | ❌ Wave 0 |
| INFRA-03 (images from catalog) | No image literals; versions file fresh | check task | `./gradlew verifyComposeVersions checkImageLiterals`; fails when `compose/versions.env` differs from the catalog or an `image:` is not `${…}` | ❌ Wave 0 |
| INFRA-04 | Conflicting append rejected, non-overlapping append succeeds on Axon Server | integration | `./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*'`; red when a conflicting concurrent append is accepted or a disjoint one rejected | ❌ Wave 0 |
| INFRA-05 | Licence behaviour recorded; quick start reflects it | ADR check + doc grep | `./gradlew checkAdrs`; `grep -q "12" docs/quick-start.md`; ADR-0005 present | ❌ Wave 0 |
| INFRA-06 | Value-class IDs, tags and sealed results round-trip unchanged; tag value equals `id.toString()` | unit + integration | `./gradlew :serialization-jackson:test :command-service:integrationTest --tests '*SerializationIntegrationTest*'`; red when the default tag resolver is used (raw UUID tags) | ❌ Wave 0 |
| IDEM-04 | All 30 official TypeID vectors, wrong prefix and malformed IDs rejected, 100 000 round trips | unit (+ property) | `./gradlew :contracts-kernel:test --tests '*TypeIdVectorsTest*' --tests '*TypeIdPropertyTest*'` | ❌ Wave 0 |
| TEST-11 | Three suites, wired into `check` | TestKit | `./gradlew :build-logic:functionalTest --tests '*TestSuitesFunctionalTest*'`; red when `check` skips `integrationTest` | ❌ Wave 0 |
| TEST-12 | Fixtures shared via `testFixtures` | TestKit + compile | same TestKit test; a suite test reads a `testFixtures` class | ❌ Wave 0 |
| TEST-14 | Toolchain on suite classpaths (JUnit 6, Kotest assertions, kotest-property, Testcontainers) | TestKit | `*TestSuitesFunctionalTest*` asserts resolved dependencies; first property test counted | ❌ Wave 0 |
| CICD-09 | Licence allowlist enforced; NOTICE mirrors exceptions | TestKit + check | `./gradlew :build-logic:functionalTest --tests '*LicenceCheckFunctionalTest*'`; `./gradlew checkDependencyLicences checkNotice`; red on a forbidden licence, an unknown licence name, or an exception missing from NOTICE | ❌ Wave 0 |
| UPS-01 | Record format valid | TestKit | `*UpstreamRecordCheckFunctionalTest*` (front matter keys, statuses) | ❌ Wave 0 |
| UPS-02 | Markers ↔ records both directions | TestKit | same test plus `./gradlew checkUpstreamRecords`; red on unknown marker, missing marker for an open workaround, marker on a removed one | ❌ Wave 0 |
| UPS-03 | Drafts exist, nothing published unapproved | doc + human gate | records carry `status: open` until approval; `checkpoint:human-verify` before any `gh issue create` | ❌ Wave 0 (manual gate) |
| UPS-04 | Dependency PR prints open records' removal criteria | shell test | `scripts/upstream/test-changed-dependencies.sh` (auto-run by CI); red when a changed catalog key matching a record's `product` yields no summary/annotation | ❌ Wave 0 |
| DOC-03 / GH-12 | ADRs match template; templates exist | check task | `./gradlew checkAdrs`; ADR-0001..0008 normalised; `ls docs/templates` has the five files | ❌ Wave 0 |
| TOOL-03 | `doctor` reports and exits non-zero on fail | shell test + CI job | `scripts/test-doctor.sh` (stubbed PATH cases); `scripts/doctor.sh --ci` in the CI `doctor` job; `./gradlew doctor` | ❌ Wave 0 |
| TOOL-04 | Works on colima | local + doc | `./gradlew doctor` plus `./gradlew integrationTest` on colima (verified in research); Docker Desktop by documentation | ❌ Wave 0 |
| AIDEV-01 | Survey recorded, plugins pinned by SHA, nothing unapproved | inventory check + human gate | `checkToolingInventory`; grep settings for 40-hex `ref`; approval recorded | ❌ Wave 0 (manual gate) |
| AIDEV-03 | Only `settings.json` and `CLAUDE.md` (+ D-39 exception) tracked | shell | `test "$(git ls-files .claude \| grep -v -e '^.claude/settings.json$' -e '^.claude/CLAUDE.md$' -e '^.claude/skills/spike-findings-')" = ""` | ✅ (rule exists; add to doctor/CI) |
| AIDEV-04 | MCP servers pinned, no secrets | check | `mise ls --current --json` shows both versions; `grep -c '"env"' .mcp.json` is 0 or only `${VAR}`; `checkToolingInventory` | ❌ Wave 0 |
| AIDEV-07 | GSD manifest verification | shell test | `scripts/test-doctor.sh` modifies a copy of a hooked file and expects `fail` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** the lefthook pre-commit hook plus the single focused Gradle command of the task (≤ 30 s targets: `:contracts-kernel:test`, a `--tests` filter of `functionalTest`).
- **Per wave merge:** `./gradlew build` and `docker compose config --quiet`.
- **Phase gate:** `./gradlew build` green (all suites), CI green (`build`, `integration-test`, `doctor`, `lint`, `ci-success`), `governance-approved` label present on the PR.

### Wave 0 Gaps
- [ ] `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` and `TestSuitesFunctionalTest` (TEST-11/12/14)
- [ ] `:contracts-kernel` with `testFixtures`, `TypeIdVectorsTest` (vectors from jetify-com/typeid commit `26a84e85…`), property test (TEST-14, IDEM-04); copy spike 002 sources
- [ ] `:tooling:test-support` singletons plus smoke tests; `:tooling:architecture` with `architectureTest` and the scope sanity test
- [ ] `governance/dependency-licences.json`, `NOTICE`, per-module CycloneDX wiring, `LicenceCheckFunctionalTest`
- [ ] `UpstreamRecordCheck`, `AdrCheck`, shared `FrontMatter`, TestKit tests
- [ ] `scripts/doctor.sh`, `scripts/test-doctor.sh`, `scripts/upstream/*` with shell tests
- [ ] Catalog entries (images with digests, konsist, uuid generator, cyclonedx) and `:platform` constraints
- [ ] Framework install: none (Gradle resolves everything)

## Security Domain

`security_enforcement` is enabled (`workflow.security_enforcement: true`, `security_asvs_level: 1`, `security_block_on: "high"` in `.planning/config.json`).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (no authentication in v1) | n/a; the AxonIQ Platform token is a licence credential, handled under V6/config |
| V3 Session Management | no | n/a |
| V4 Access Control | partial | Governance gate for governed paths; read-only `contents: read` tokens for new CI jobs; Claude Code `ask` rules |
| V5 Input Validation | yes | TypeID `parse` rejects malformed and wrong-prefix IDs; front-matter parsers fail closed on unsupported syntax; sanitise record text before emitting workflow commands |
| V6 Cryptography / secrets | yes | Token only via untracked `.env` (bare-name pass-through), never logged by `doctor`, gitleaks already runs; SHA-256 manifest verification uses standard tools, nothing hand-rolled |
| V10/V14 Supply chain & config | yes | Image tag + digest pins, SHA-pinned marketplaces, exact-version mise tools with `mise.lock` checksums, licence allowlist, no auto-update of agent tooling |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Workflow-command injection through record or dependency text in the UPS-04 job | Tampering | Sanitise `%`, CR, LF; job has `contents: read` only; advisory output, never fails or comments |
| Secret leakage via `doctor` or compose (`env_file: .env`, empty token echo) | Information disclosure | Presence test only; bare-name pass-through; `.env` ignored, `.env.example` committed with the value commented |
| Malicious or drifting skill/MCP package | Tampering / elevation | Review criteria, full-SHA pin, exact mise version with lockfile digest, author approval, no pre-approval in `enabledMcpjsonServers`; Playwright MCP `--isolated`, restricted origins |
| Floating container images (Ryuk, tiny image, `:latest` shared container) | Tampering | Catalog digests passed as task inputs; Konsist/check rule against image literals |
| Mutable governance via the PR's own tree (ADR-0003 residual) | Tampering | Existing governance gate; new checks live in governed paths |
| Docker socket exposure through Testcontainers/Ryuk | Elevation | Dev/CI only, documented; no privileged Ryuk |
| Axoniq Platform outbound connection (`34.149.46.65:7000`) | Information disclosure | Opt-in token only; CI uses none |

## Sources

### Primary (HIGH confidence)
- This repository (files read this session): `.planning/phases/02-…/02-CONTEXT.md`, `REQUIREMENTS.md`, `STATE.md`, `.planning/spikes/WRAP-UP-SUMMARY.md`, the spike skill and its four references, `settings.gradle.kts`, `build-logic/**`, `platform/build.gradle.kts`, `gradle/libs.versions.toml`, `governance/*`, `.github/workflows/ci.yml`, `mise.toml`, `mise.lock`, `renovate.json5`, `docs/tooling.md`, `qodana.yaml`, `.gitignore`, `.claude/settings.json`, `.claude/gsd-file-manifest.json`.
- Prototype runs (scratch copies, nothing written to the repository): `courses.test-suites` in a copy of the repo (three suites, `check` wiring, detekt reach, internal visibility, kotest-property skip pitfall, Testcontainers/Ryuk on colima); CycloneDX 3.6.1 under configuration cache and isolated projects with licence extraction and `includeConfigs`; mise `npm:` install and `mise lock` of both MCP servers in an isolated data directory; four compose experiments (`include:` + `env_file`, token pass-through, profiles); GSD manifest SHA-256 verification (858/858).
- Maven Central and plugin portal metadata and POMs: `spring-boot-dependencies` 4.1.3, `axonserver-connector-java` 2026.1.1, `konsist` 1.0.0-beta.1 and 0.18.1, `axon-framework-bom`/`axoniq-framework-bom` 5.4.2, `kotest-bom` 6.2.7, `jackson-bom` 3.3.0, plugin portal for `org.cyclonedx.bom` 3.6.1 and jk1 3.1.5.
- Sources jars read: `axoniq-testcontainer-5.4.2-sources.jar` (`SharedAxonServerContainer`, `AxonServerContainer`, `AxonServerContainerUtils`), `konsist-1.0.0-beta.1-sources.jar` (`PathProvider`, `GradleProjectRootDirResolver`, `ProjectDirectoryFilter`, `KoDeclarationAndProviderAssert`, `KoScopeCreator`).
- Docker Hub tag API (`axoniq/axonserver` 2026.1.8-nonroot, `library/postgres` 18.7 digests); npm registry (`npm view`, version histories); `gh api` for AxonIQ/agent-skills, gradle/gradle-skills, Kotlin/kotlin-agent-skills, spring-projects/spring-tools, jetify-com/typeid and typeid-js, adr/madr.

### Secondary (MEDIUM confidence)
- code.claude.com docs: plugin marketplace reference (`ref`/`sha`, `extraKnownMarketplaces`, version gate 2.1.324), MCP (env expansion, approval prompt), skills overview.
- docs.renovatebot.com mise manager (npm backend supported); java.testcontainers.org supported Docker environments and configuration; docs.konsist.lemonappdev.com scopes; github.com/CycloneDX/cyclonedx-gradle-plugin README; github.com/LemonAppDev/konsist/releases; docker-library/postgres PR 1259 and issues 1370/1377 (via search summary).

### Tertiary (LOW confidence)
- Skill aggregator listings (skillsmp.com, claudemarketplaces.com) used only to find repositories; every candidate was re-checked directly through `gh api`.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, versions and BOM coverage read from Maven Central POMs; Konsist beta status is the only soft spot.
- Architecture: HIGH for test suites, SBOM, compose, MCP, Konsist root detection (all executed or read from source); MEDIUM for module placement (Open Questions 1 and 2) and the doctor design (not built).
- Pitfalls: HIGH, most reproduced here (check wiring, detekt reach, internal visibility, property-test skip, empty token, include `env_file`, Claude Code version).

**Research date:** 2026-11-22
**Valid until:** 2026-12-06 for image digests, MCP/skill pins and Konsist (fast-moving); 2026-12-22 for the rest
