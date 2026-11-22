# Phase 2: Runtime Infrastructure & DCB Spikes - Context

**Gathered:** 2026-11-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 2 makes DCB on a pinned Axon Server work locally and in CI and puts the JVM test infrastructure in place. It also
resolves and records every high-risk unknown before any domain code is written. It delivers:

- the local runtime: compose with Axon Server (DCB context, hostname) and PostgreSQL;
- the contracts modules, with TypeID value-class IDs, the tag registry and sealed results, plus the serialization
  round-trip;
- the Gradle JVM test suites with test fixtures and Testcontainers;
- the integration test for conditional-append conflicts;
- spikes recorded in MADR ADRs (licence, decision-record capture, serialization and TypeID, buildpacks);
- the in-repository templates;
- the upstream-record system with its CI check;
- the dependency licence check and NOTICE;
- `doctor`;
- vetted Claude Code skills and MCP servers.

Requirements: INFRA-02..06, IDEM-04, TEST-11, TEST-12, TEST-14, CICD-09, UPS-01..04, DOC-03, GH-12, TOOL-03, TOOL-04,
AIDEV-01, AIDEV-03, AIDEV-04, AIDEV-07. Domain decisions (commands, invariants) are Phase 3 and later.

</domain>

<decisions>
## Implementation Decisions

### Repository Layout & Contracts
- **D-01 (amends Phase 1 D-03):** Backend code is grouped under `backend/`:
  - `backend/contracts/{kernel,commands,events,queries}`;
  - `backend/command-service`, `backend/query-service`, `backend/automation-service` and `backend/bff`, as flat service
    directories whose names match compose service and image names.

  `platform/`, `build-logic/`, `frontend/`, `docs-site/` and `tooling/` stay at the top level. Gradle project paths
  may be shortened via `projectDir` (planner's choice). Update the `settings.gradle.kts` layout comment and any path
  references, such as CI filters and REUSE. — **Reversibility:** costly — paths are referenced from CI, Renovate, REUSE,
  IDE run configurations and docs. This is the cheapest point to change, since only `platform/` and `tooling/` exist.
- **D-02:** Contracts are split by message kind into four modules: kernel (IDs, tag registry, shared `Rejection` and
  result base types), commands, events and queries. The dependency graph shows the CQRS boundaries: the query service
  depends on events and queries, and the BFF on commands and queries but not on events.
- **D-03:** Every message type has an explicit, stable type name and version through the standard AF5 mechanism
  (`@Namespace` / `@Event` and the equivalents for commands and queries):
  - a short namespace such as `courses` replaces the package-derived default;
  - `name` is the class simple name;
  - the version is explicit (not the implicit `0.0.1` default).

  A Konsist rule requires an explicit name and version on every message. Research must confirm how AF5 5.4.2 stores the
  qualified name: the 5.1 docs mention the `payloadType` column being renamed to `type`. — **Reversibility:** one-way
  after Phase 3 — the qualified names and versions are persisted in Axon Server with the first event, and changing them
  later needs upcasters or a new store.
- **D-04:** Contracts may carry Axon annotations as metadata only, with Axon as a `compileOnly` dependency so the
  contracts have no runtime framework dependency. A Konsist rule forbids any other Axon or Spring import in the
  contracts modules.
- **D-05:** Command results are a sealed hierarchy per command, for example `SubscribeResult = Subscribed |
  Rejected(code)`. The kernel provides a shared `Rejection` base and the stable rejection-code contract. Whether Axon
  serializes sealed hierarchies across the wire is checked in the serialization spike.

### TypeID & Serialization
- **D-06:** An ID value class wraps `java.util.UUID` (for example `value class CourseId(val uuid: UUID)`), and each type
  fixes its prefix. Serialization and tags produce the canonical TypeID string, and `toString()` is overridden to return
  it. This maps directly to the native `uuid` columns and keeps UUIDv7 ordering. — **Reversibility:** costly — every
  contract, serializer and read-model mapping uses the shape. The serialized form (the canonical string) is fixed by
  IDEM-04 either way.
- **D-07:** Research looks for a mature JVM TypeID library. If there is none, the project implements Crockford base32
  encoding itself and gets UUIDv7 from a vetted library or the JDK. The implementation is verified against the official
  TypeID spec's valid and invalid test vectors and by property tests. Record this in an ADR and clarify the
  "vetted libraries" wording of IDEM-04 accordingly.
- **D-08:** Serialization uses Jackson 3 with the Kotlin module through the AF5 converter
  (`axon.converter.general=jackson`). One Jackson module holds the value-class (de)serializers and is shared with the BFF
  REST layer later. kotlinx.serialization was rejected.
- **D-09:** The parse API is `parse(s)`, which throws `IllegalArgumentException` with a clear wrong-prefix or malformed
  message, plus `parseOrNull(s)`, mirroring the stdlib `toInt` and `toIntOrNull`.
- **D-10:** The tag registry is `object Tags` in the kernel with `const val` keys, because annotations need compile-time
  constants. An entity tag key equals its TypeID prefix (`course=course_01…`). Service tags, such as a normalised course
  name for uniqueness, are in the registry too. A Konsist rule requires every `@EventTag` key to come from the registry.
- **D-11:** Test IDs come from three sources:
  - readable fixed IDs from `testFixtures` for given-when-then tests (stable failure messages);
  - kotest `Arb` generators for property tests;
  - a UUIDv7 generator that takes a `Clock`, so tests that depend on ordering are deterministic.

  Production IDs are client-generated (IDEM-04). On the JVM, generation serves tests and demo data.

### Spikes
- **D-12:** Spikes run through `/gsd-spike` **between this discussion and `/gsd-plan-phase 2`**, with exploratory code in
  `.planning/spikes/`, so the planner plans against known answers. Outcomes are recorded in MADR ADRs during phase
  execution. Spike proofs that are requirements themselves move into real modules as permanent tests: the
  conditional-append conflict test (INFRA-04), the serialization round-trip (INFRA-06) and the decision-record capture
  test. Spike topics:
  - decision-record capture (D-13);
  - Kotlin tags and the Jackson round-trip, including sealed results over Axon;
  - Paketo static image and environment defaults (D-35);
  - Axon Server licence restart behaviour and token delivery (D-14);
  - DCB context creation and hostname in compose.
- **D-13:** Decision-record capture (DEMO-01: criteria, consistency marker, appended events, outcome, retries) uses a
  **central decorator** around the Axon layer that performs the conditional append, so slices contain no capture code.
  If the spike confirms that this needs an `@Internal` Axon API, accept it with two safeguards:
  - a capture test that fails loudly in the (manually merged) Renovate PR when an Axon update breaks it;
  - an upstream record asking AxonIQ for a public extension point.

  Explicit per-slice stamping through a shared helper plus a Konsist rule is documented in the ADR as the fallback.
- **D-14:** The author has verified the Axon Server licence behaviour: unlicensed Axon Server in compose stops working
  after about 12 hours, and with an AxonIQ Platform token it connects and works without the limit. The spike verifies
  only two things: whether a container restart (with and without the data volume) resets the 12-hour limit, and how the
  token is supplied. No multi-hour soak test. The ADR and the quick start reflect the result.

### Test Infrastructure
- **D-15:** The JVM test levels are three suites:
  - `test` (unit): no Spring and no containers; DCB decisions run on `AxonTestFixture` in memory;
  - `componentTest`: one service with its Spring context, a real PostgreSQL in Testcontainers and Axon in memory (no
    Axon Server);
  - `integrationTest`: a real Axon Server container, conditional appends and cross-service flows.

  The boundary between component and integration is Axon Server. H2 in PostgreSQL mode was rejected: it lacks
  `COLLATE "C"`, `ON CONFLICT` upserts, `jsonb` and dialect-faithful Flyway migrations, and it gives false confidence.
- **D-16:** Tests on the shared Axon Server container are isolated by fresh unique TypeIDs and data per test, with no
  purge between tests. This is parallel-safe because DCB criteria select events by tag. Global checks, such as name
  uniqueness, use unique data.
- **D-17:** The Konsist architecture rules live in one dedicated module (for example `tooling/architecture`) with an
  `architectureTest` suite that scans the whole repository's sources. Phase 2 rules:
  - the contracts contain no Spring or Axon logic imports (D-04);
  - every message has an explicit name and version (D-03);
  - every `@EventTag` key comes from the registry (D-10);
  - test class names end with their suite's suffix (cross-cutting convention).
- **D-18:** Shared test helpers live where they belong:
  - helpers for a module's types live in that module's `testFixtures` (ID fixtures and Arbs in the kernel, event
    builders in events);
  - Testcontainers singletons (Axon Server, PostgreSQL) live in a small dedicated test-support module, because they are
    infrastructure and belong to no particular module.
- **D-19:** `check` and `build` run **every** suite, including `integrationTest`. That makes them the single done
  criterion for agents, humans and CI, and GSD executors cannot report green without integration tests. There is no
  `quickCheck` or `fullCheck` aggregate. A human who wants a fast loop runs individual suite tasks (`test`,
  `componentTest`); document this in one line. The Gradle build cache keeps unchanged integration tests from re-running.
- **D-20:** CI runs two parallel jobs, both aggregated by `ci-success`:
  - `build`: `./gradlew build -x integrationTest`;
  - `integration-test`: `./gradlew integrationTest`.
- **D-21:** Of the TEST-14 libraries, only those Phase 2 tests use are added (catalog plus the `courses.test-suites`
  convention): `kotest-property`, Testcontainers and Awaitility if needed. MockK and `AxonTestFixture` arrive in Phase 3
  with the first decisions. No unused dependencies.
- **D-22:** Parallelism is set per level. Unit tests keep multiple forks (current convention). `componentTest` and
  `integrationTest` use one fork per module so that container singletons and the Spring context cache are reused. JUnit
  in-fork parallelism comes later, if measurements justify it.

### Local Runtime (Compose) & Licence
- **D-23:** A root `compose.yaml` pulls in per-concern files under `compose/` through `include:` (infra now; services,
  observability and UI later). Optional parts use profiles. `docker compose up` stays one command. Images come from the
  catalog through the Gradle-generated versions env file (cross-cutting INFRA-09), and `docker compose config` is
  validated in CI (CICD-08).
- **D-24:** One PostgreSQL server serves all services. An init script creates a separate database and user per service
  (query service, automation service and their token stores).
- **D-25:** The AxonIQ Platform token is optional. It is an environment variable in a gitignored `.env`, with a
  committed `.env.example` that has an empty value and explains it. Without the token the system works with the
  approximately 12-hour limit, and the quick start says so. `doctor` warns when the token is missing. CI uses no token,
  because its runs are short.
- **D-26:** Two connection scenarios must work against the compose Axon Server without reconfiguration: services running
  inside compose, and applications run from the IDE on the host. Research picks the mechanism (server
  hostname/internal-hostname settings or client settings). A smoke check covers both.

### doctor
- **D-27:** `scripts/doctor.sh` is the single entry point.
  1. Dependency-free bootstrap checks:
     - mise and the pinned tools, JDK and Node;
     - the Docker engine (colima or Docker Desktop, `DOCKER_HOST` and Ryuk configuration);
     - the GSD installation against the SHA-256 hashes in `gsd-file-manifest.json`, plus modified hooks and unexpected
       files;
     - leaking `~/.gsd/defaults.json` keys;
     - the MCP servers;
     - the Platform token.
  2. Delegation to `./gradlew doctor` for JVM-side checks, including Testcontainers connectivity through the
     Testcontainers library itself.

  `doctor` consolidates the existing `VerifyFormatterBinary` and `VerifyNodeVersion` checks.
- **D-28:** `doctor` only diagnoses. It runs every check and reports each as ok, warn or fail with the exact fix
  command, exits non-zero on any fail, and never changes anything.
- **D-29:** A separate `ci.yml` job runs `doctor --ci` on ubuntu with Docker. In that mode the local-only checks (GSD,
  MCP, Platform token, colima) are reported as skip.

### Upstream Records & Licences
- **D-30:** The licence check uses the CycloneDX SBOM (needed in Phase 6 anyway), and a small Gradle task compares its
  licences with an allowlist. npm uses the same approach later. Research verifies that the CycloneDX plugin works with
  the configuration cache and isolated projects. The fallback is the jk1 Gradle License Report plugin.
- **D-31:** The allowlist has three parts:
  - allowed: permissive licences (Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC and similar) and weak copyleft
    (EPL-2.0, MPL-2.0);
  - forbidden: GPL and AGPL;
  - per-artifact exceptions with justification: the Axoniq commercial-terms modules, mirrored in NOTICE.
- **D-32:** UPS-04 is a targeted CI check on PRs. It matches the changed dependencies against the `product` of open
  upstream records and prints their removal criteria in the job summary and as a warning annotation. It needs read-only
  permissions and posts no PR comments.
- **D-33:** The UPSTREAM marker and record consistency check is a Gradle task in `build-logic` with TestKit (carried
  forward from Phase 1 D-13). The ADR front-matter check (D-38) shares its front-matter parser.

### Templates & ADRs
- **D-34:** There is one ADR template: full MADR 4 with YAML front matter. Optional sections (Decision Drivers, Pros
  and Cons, More Information) may be omitted for small decisions. Considered Options is mandatory.
- **D-35:** The Paketo spike covers images:
  - the frontend image gets a **prebuilt `dist/` from Gradle** (Node plugin plus pnpm builds once);
  - the Paketo Web Servers buildpack only serves it through nginx with project configuration: SPA fallback and an
    `/api` proxy with `proxy_buffering off` for SSE;
  - pnpm support inside Paketo is therefore not needed, and the image contains no Node.

  The spike builds a minimal static site and a minimal Spring Boot image in `.planning/spikes/`, records the working
  `BP_*` and `BPE_*` environment defaults (including JVM memory) and checks nginx configuration and colima operation. An
  ADR records the outcome.
- **D-36:** ADR-0001..0004 are normalised to the template, as Phase 1 D-07 promised: front matter and MADR structure,
  with unchanged content, dates and decisions. Custom sections (Re-enable criteria, When to revisit, Rejected
  alternatives) map to Confirmation, More Information and Considered Options. The "minimal record" notes are removed.
- **D-37:** The GH-12 templates live in `docs/templates/`: ADR, documentation page, Event Modeling slice description,
  upstream record and upstream issue draft. `docs/adr/` and `docs/upstream/` hold only real records.
- **D-38:** A Gradle check validates ADR front matter and structure:
  - required keys and allowed statuses;
  - the number in the filename matches the title;
  - numbering has no gaps or duplicates;
  - superseded links point to existing ADRs;
  - required sections are present.

### AI Tooling
- **D-39 (amends AIDEV-03):** `.claude/skills/spike-findings-*/` is tracked too, as a narrow exception, because it is a
  GSD-generated artifact at the path GSD's wrap-up expects and appends to. Everything else under `.claude/` stays as
  AIDEV-03 says: only `settings.json` and `CLAUDE.md` are tracked, and the GSD installation and `settings.local.json`
  are ignored. The project's own skills stay in the repository-local plugin outside `.claude/` (AIDEV-02, Phase 8).
  Claude Code does not document `.agents/skills`, and GSD's wrap-up path is hard-coded, so neither is an option.
- **D-40:** The survey covers all AIDEV-01 stack areas (Axon, Kotlin, Gradle, Spring Boot, Playwright, React/shadcn,
  FSD, GitHub Actions, Paketo), and the tooling inventory records a verdict for each candidate. Only skills needed in
  Phases 2–4 are installed: Axon, Kotlin, Gradle, Spring Boot and GitHub Actions. They are installed as pinned
  marketplace plugins, each after explicit author approval. The others follow in their phases.
- **D-41:** GSD `agent_skills` is filled in Phase 2 after the survey. A phase task proposes the per-agent mapping
  (executor, planner, code reviewer and others) and the author approves it. `.planning/config.json` and
  `governance/gsd-config-policy.json` change in one PR with the `governance-approved` label, and ADR-0004 is updated.
- **D-42:** Context7 and Playwright MCP servers are installed as `npm:` tools through mise: exact version, checksums in
  `mise.lock`, Renovate-managed, and one version source (INFRA-09). `.mcp.json` calls the installed binaries and
  references only environment variables, never secrets. Research verifies that this works. The fallback is `npx` with an
  exact version plus a Renovate regex manager. Personal MCP servers (IntelliJ, optionally GitHub) stay in local scope
  and are documented.

### Post-Research Decisions (2026-11-22, after spikes 001–005 and 02-RESEARCH.md)
- **D-43:** The Axon wiring proven by the spikes (`KotlinTagResolver`, the decision-capture `EventStorageEngine`
  decorator, the handling-side conflict-retry `ConfigurationEnhancer`) and the permanent Axon Server tests (INFRA-04
  conflict test, INFRA-06 round trip, decision-capture test) live in `backend/command-service`, created in Phase 2 as a
  plain Kotlin library. Phase 3 turns it into the Spring Boot command service.
- **D-44 (extends D-04):** Sealed command results carry `@JsonTypeInfo` directly, with `jackson-annotations` as a
  `compileOnly` metadata dependency of the contracts, the same rule D-04 sets for Axon annotations. The shared Jackson
  module (`serialization-jackson`) depends on the kernel only, so the D-02 graph stays intact. The spike's mix-in
  design is recorded as the rejected alternative.
- **D-45 (clarifies UPS-02):** Only records whose workaround has been removed forbid workaround markers. A `wont-fix`
  record keeps its markers, because its workaround is permanent.
- **D-46 (scopes D-26):** Phase 2 verifies the host scenario functionally (the INFRA-04 integration test against
  `localhost:8124`) and the compose scenario at network level (reachability from a sibling container). Functional
  in-network gRPC clients arrive with the Phase 3/6 services.
- **D-47 (corrects D-25 wording):** `.env.example` carries the Platform token line commented out, with an explanation;
  an empty value is not allowed, because an empty `AXONIQ_PLATFORM_AUTHENTICATION` crashes Axon Server into a zombie
  state (spike 004). Compose passes it as a bare `- AXONIQ_PLATFORM_AUTHENTICATION` entry, which omits it when unset
  (verified in research); `doctor` fails on an empty definition.
- Spike findings in `.planning/spikes/` and the `spike-findings-axon5-dcb-courses` skill are established facts for
  planning (see `.planning/spikes/WRAP-UP-SUMMARY.md`).

### Claude's Discretion
- How the DCB context is created in compose: native Axon Server configuration if it exists, otherwise a one-shot init
  container calling `/v2/cluster/init?dcb=true`.
- Gradle project path names (`projectDir` mapping), exact module directory names for architecture and test-support
  modules, and the `.env` variable name for the Platform token.
- Exposed host ports, compose service names beyond those fixed by D-01, and the content of the PostgreSQL init script.
- The internal structure of the `courses.test-suites` convention, and the order of the suites in CI logs.
- Which existing upstream defects already qualify for `docs/upstream/` records (research collects candidates from the
  Phase 1 ADRs, STATE.md and the summaries).
- The removal of `:tooling:convention-sample` once the first real Kotlin module lands (already decided in Phase 1 D-22).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope & requirements
- `.planning/ROADMAP.md` §"Phase 2: Runtime Infrastructure & DCB Spikes" — goal, success criteria, research topics,
  approval gates
- `.planning/ROADMAP.md` §"Cross-Cutting Conventions" — Phase 2 sub-scopes of INFRA-09 and INFRA-10 (catalog images
  with digest, the Gradle-generated compose env file, Testcontainers singletons), CICD-08 (`docker compose config`), and
  the test suffix rule
- `.planning/REQUIREMENTS.md` — INFRA-02..06, IDEM-04, TEST-11/12/14, CICD-09, UPS-01..04, DOC-03, GH-12, TOOL-03/04,
  AIDEV-01/03/04/07; AIDEV-02 (Phase 8) constrains D-39; DEMO-01 (Phase 3) defines the decision record for D-13
- `.planning/PROJECT.md` §"Key Decisions" — TypeID, buildpacks, test toolchain, single version source, upstream-first

### Prior decisions
- `.planning/phases/01-repository-build-ci-foundation/01-CONTEXT.md` — D-01 (package), D-03 (layout, amended by D-01
  here), D-04 (convention plugin naming), D-07 (ADR normalisation promise), D-13 (governance checks as Gradle tasks),
  D-22 (sample module removal)
- `docs/adr/0001-workflow-permissions-and-required-checks.md`, `docs/adr/0002-codeql-kotlin-analysis.md`,
  `docs/adr/0003-governance-gate-trust-boundary.md`, `docs/adr/0004-gsd-configuration.md` — to normalise (D-36);
  ADR-0004 is updated by D-41

### Stack research
- `.planning/research/STACK.md` — versions and compatibility (Axon 5.4.2, Axon Server 2026.1.8, Testcontainers 2.0.5,
  the `axoniq-testcontainer` `withDcbContext`, protobuf pin, Jackson 3)
- `.planning/research/PITFALLS.md`, `.planning/research/ARCHITECTURE.md` — known risks and architecture notes

### Build and governance code to extend
- `settings.gradle.kts` — module includes and layout comment (D-01)
- `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` — current test setup and `:platform` alignment
  (basis for `courses.test-suites`)
- `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyFormatterBinary.kt`,
  `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt` — existing environment checks
  folded into `doctor` (D-27)
- `gradle/libs.versions.toml` — catalog (Axon BOMs, Testcontainers BOM, Kotest already present)
- `governance/gsd-config-policy.json`, `.planning/config.json` — whole-file policy pinning `agent_skills` (D-41)
- `governance/governed-paths.txt` — must cover new governance surfaces (`.mcp.json`, compose and doctor if governed)
- `.gitignore` — `.claude` tracking rules (D-39)
- `.claude/settings.json` — plugin and marketplace registration for AIDEV-01; `ask` rules already cover `.mcp.json`
- `docs/tooling.md` — tooling inventory (skills, MCP servers, GSD version)
- `.claude/gsd-core/workflows/spike.md`, `.claude/gsd-core/workflows/spike-wrap-up.md` — the spike mechanism (D-12,
  D-39)

### External documentation (verify against the pinned versions)
- AF5 message naming: https://docs.axoniq.io/axon-framework-reference/5.0/messaging-concepts/anatomy-message/ and
  https://apidocs.axoniq.io/latest/org/axonframework/messaging/eventhandling/annotation/Event.html (D-03)
- AF5 event versioning: https://docs.axoniq.io/axon-framework-reference/5.0/events/event-versioning/
- AF5 message migration (`payloadType` → `type`):
  https://docs.axoniq.io/axon-framework-reference/5.1/migration/paths/messages/
- AF5 event tagging: https://docs.axoniq.io/axon-framework-reference/5.3/migration/paths/aggregates/event-tagging/ (D-10)
- Claude Code skills locations: https://code.claude.com/docs/en/skills (D-39)
- TypeID specification and test vectors (jetify-com/typeid) — research to pin the exact spec version (D-07)
- MADR 4 templates (adr.github.io/madr) — D-34

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `courses.kotlin-library` convention: `:platform` alignment for every `*Implementation` configuration, so new suites
  and `testFixtures` are aligned automatically. It also provides the JDK from `.java-version`, `allWarningsAsErrors`,
  detekt and Dokka with `failOnWarning`.
- `VerifyFormatterBinary`, `VerifyNodeVersion` and `ExecutableOnPath`: patterns for binary and version checks that
  `doctor`'s Gradle part can reuse.
- The governance checks in `build-logic` (Kotlin, TestKit, cacheable) are the pattern for the UPSTREAM marker check,
  the ADR front-matter check and the licence allowlist check.
- `GradleFixture` and the TestKit setup (`functionalTest` suite) are already in `build-logic`.
- The catalog already has the `axon-framework-bom`, `axoniq-framework-bom`, `testcontainers-bom`, `kotest-bom` and
  `kotest-assertions-core` entries.

### Established Patterns
- Every version lives in a single source (catalog, `.java-version`, `.nvmrc`, mise), and `checkBuildScripts` rejects
  version literals.
- Every task a convention plugin registers has a group and a description and is cacheable. The build is
  isolated-projects compatible.
- Test class suffix = suite name (`*Test`, `*ComponentTest`, `*IntegrationTest`, `*ArchitectureTest`,
  `*FunctionalTest`).
- Workflows: `permissions: {}` at workflow level with job-level grants. The `ci-success` aggregate is the single
  required check.
- Governance-file changes need the `governance-approved` label. Edits to `.planning/config.json`,
  `.claude/settings.json` and `.mcp.json` are `ask` rules.

### Integration Points
- `settings.gradle.kts` gains the backend and tooling modules. `:tooling:convention-sample` goes when the first real
  Kotlin module lands.
- `ci.yml` gains the `integration-test` and `doctor` jobs, plus the licence, upstream and ADR checks (through `check`).
  Renovate gains the compose/Testcontainers image and MCP tool updates.
- `mise.toml` gains the MCP server tools, and `docs/tooling.md` lists them (`checkToolingInventory` enforces it).

</code_context>

<specifics>
## Specific Ideas

- The decision record is the foundation of the event explorer and the concurrency demo. It answers questions such as
  "the 31st student was rejected because the decision saw these 30 subscriptions up to position 1042". Completeness
  (marker, retries) matters more than avoiding an `@Internal` API.
- Event names use the namespace `courses` (or similar short value), not `io.github.aanbrn.courses...`.
- Spikes should answer questions before planning, not inside plans.
- Facts the author verified (D-14) go into the ADR as author-verified observations, alongside the documentation
  sources.

</specifics>

<deferred>
## Deferred Ideas

- Static analysis (detekt, `spotlessCheck`) in a fast-loop aggregate: not adopted together with `quickCheck` (D-19).
- JUnit in-fork parallelism for component and integration tests: revisit with measurements (D-22).
- `--fix` mode for `doctor`: not adopted (D-28).
- Frontend, Playwright and Paketo skills: installed in their phases per the survey verdicts (D-40).

</deferred>

---

*Phase: 02-runtime-infrastructure-dcb-spikes*
*Context gathered: 2026-11-22*
