---
phase: 02-runtime-infrastructure-dcb-spikes
verified: 2026-11-23T07:05:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - ".github/workflows/ci.yml"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-01-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-01-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-02-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-02-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-03-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-03-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-04-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-04-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-05-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-05-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-06-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-06-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-07-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-07-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-08-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-08-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-09-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-09-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-10-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-10-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-11-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-11-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-12-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-12-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-13-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-13-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-14-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-14-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-15-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-15-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-16-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-16-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-17-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-17-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-18-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-18-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-19-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-19-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-20-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-20-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-21-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-21-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-22-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-22-SUMMARY.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-23-PLAN.md"
  - ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-23-SUMMARY.md"
  - "backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt"
  - "backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeId.kt"
  - "build-logic/src/main/kotlin/courses.test-suites.gradle.kts"
  - "compose.yaml"
  - "compose/infra.yaml"
  - "scripts/doctor.sh"
covered_digest: "v4:sha256:8c386a11ad3c158b7bb30f17952fee924766742b99dce64ca918202f9724045f"
behavior_unverified: 0
overrides_applied: 0
deferred:
  - truth: "GH-12 clause: the in-repository templates 'are used by the project skills'"
    addressed_in: "Phase 8"
    evidence: "REQUIREMENTS AIDEV-02 (Phase 8): project-specific skills 'write an ADR, draft an upstream issue' in a repository-local plugin; 02-19-SUMMARY records that the skills' use of the templates stays with Phase 8. All five templates exist in docs/templates/ and are used by checkAdrs and checkUpstreamRecords."
human_verification:
  - test: "Open a pull request from this branch and let GitHub Actions run the full workflow"
    expected: "Jobs integration-test, compose-smoke, doctor and upstream-records pass on a GitHub runner, and ci-success (which needs all of them) is green"
    why_human: "The four jobs were added in this phase and have never run on a GitHub runner. Locally verified: the same Gradle task, the compose start plus probe, scripts/doctor.sh and the Axon Server conflict test on a compose stack. The runner environment (anonymous image pull, Docker on ubuntu-latest, SHA-pinned actions) is only provable there."
  - test: "In Claude Code run /plugin and confirm axoniq-app-development@axoniq and gradle-skills@gradle-skills install from the SHA-pinned marketplaces in .claude/settings.json"
    expected: "Both plugins install and list their skills; the 'global:' agent_skills entries in .planning/config.json resolve through gsd-tools query agent-skills"
    why_human: "Plugin installation is a Claude Code runtime action. The repository side (40-hex refs, enabledPlugins, doctor claude-code check) is verified; the install itself is not observable from the codebase."
  - test: "Read the Axoniq wording in NOTICE, docs/quick-start.md and ADR-0005 against the current Axoniq Terms of Service page"
    expected: "The statements 'free for development, demonstrations and CI; a licence is required in production' and the 12-hour unregistered window are accurate and acceptable to the author"
    why_human: "Legal wording and the correctness of third-party terms are a judgment call. The consistency check (checkNotice: every policy exception appears in NOTICE) passes."
  - test: "Read the re-written Considered Options of ADR-0001 and ADR-0002"
    expected: "The structure now follows MADR with decisions, dates and statuses unchanged (02-19 prohibition)"
    why_human: "Whether a normalisation preserved the meaning of an earlier decision is a semantic judgment. checkAdrs validates structure only."
  - test: "Post the forum or support drafts for UP-0006 (empty Platform token) and UP-0013 (dcbContext flag), and decide on UP-0010 and UP-0011, then record the URLs and set status to reported"
    expected: "Each record moves to status reported with a non-empty upstream list and a reported date, and checkUpstreamRecords stays green. Five GitHub issues exist and were checked read-only: AxonFramework#5326, #5327, #5328, axoniq-framework#811, #812."
    why_human: "UPS-03 publication needs the author's explicit approval. Four drafts (UP-0006, UP-0010, UP-0011, UP-0013) are still unreported; UP-0008, UP-0009, UP-0012 and UP-0005 link existing upstream issues."
  - test: "Run docker compose up -d --wait and ./gradlew integrationTest on a Docker Desktop machine (not colima)"
    expected: "Stack healthy and tests green"
    why_human: "TOOL-04 says colima and Docker Desktop. docs/tooling.md states colima is the verified engine and Docker Desktop needs no setting, but only colima was exercised in this phase."
---

# Phase 2: Runtime Infrastructure & DCB Spikes Verification Report

**Phase Goal:** DCB on a pinned Axon Server works locally and in CI, the JVM test infrastructure is in place, and every high-risk unknown is resolved and recorded before domain code is written.
**Verified:** 2026-11-23T07:05:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

Every roadmap success criterion is achieved in the codebase and was exercised by this verifier, not taken from the SUMMARYs. No blocker was found. The status is `human_needed` only because some items cannot be proven from the repository: the new CI jobs have not run on GitHub, plugin installation and legal wording need a human, and four upstream drafts await the author.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | One compose command starts pinned Axon Server and PostgreSQL; DCB context auto-created; correct hostname; images only from the catalog; `doctor` checks tools, Docker engine, Testcontainers, GSD manifest and explains fixes | VERIFIED | Ran `POSTGRES_PORT=55432 docker compose -p verify02 up -d --wait` (host 5432 was taken by the author's PostgreSQL): both services healthy in 15 s. `docker compose --profile probe run --rm probe` printed both "reachable" lines. `compose/versions.env` equals the `image-axonserver` and `image-postgres` entries of `gradle/libs.versions.toml`. `compose/infra.yaml` sets `hostname: axonserver` and `AXONIQ_AXONSERVER_STANDALONE_DCB=true`, with no image literal. The `default` context served the DCB conflict test, and PostgreSQL held the `query_service` and `automation_service` databases. Stack torn down with `down -v`; working tree stayed clean. `bash scripts/doctor.sh`: 12 ok, 2 warn (author's `~/.gsd/defaults.json` keys, absent Platform token), 0 fail, exit 0. Each result carries a `fix:` line. It covers mise tools, Docker engine, colima, Testcontainers config, a live Testcontainers probe, and 858 GSD files against the manifest. |
| 2 | An integration test on Axon Server in Testcontainers shows a conflicting conditional append is rejected and a non-overlapping one succeeds; unit, component and integration run as separate Gradle suites sharing test fixtures with the standard toolchain | VERIFIED | `ConditionalAppendIntegrationTest` (2 tests, 0 failures, 0 skipped). One test forces two units of work to source the same criteria behind a non-blocking barrier, then asserts exactly one commits and the other fails with `AppendEventsTransactionRejectedException`. The other uses disjoint courses and asserts both commit. Because the conflict rejection is a state-transition invariant, a behavioral test supports it rather than symbol presence. I also re-ran this test against the live compose Axon Server (`-Pcourses.axonserver.external=localhost:8124`, fresh run at 06:55:27Z): green. `courses.test-suites.gradle.kts` registers `componentTest` and `integrationTest` JVM Test Suites. Both run with `maxParallelForks = 1` and `mustRunAfter`, and `check` depends on all three. The suites share `testFixtures` (kernel and command-service), JUnit 6, Kotest assertions and property, and Kover. Suite results: 18 non-build-logic suites, 0 failures. |
| 3 | Contracts modules contain no Spring or framework logic; Kotlin events with TypeID value-class IDs and tags round-trip unchanged; malformed or wrong-prefix TypeIDs are rejected | VERIFIED | Imports across `backend/contracts/*/src/main`: only kernel types, `java.util.UUID`, `com.fasterxml.uuid.Generators` (ID generation in the kernel), three Axon annotation types and `JsonTypeInfo`. Axon and Jackson are `compileOnly` annotations (build files). `ContractsArchitectureTest` enforces this (6 tests green). `TypeId` is an own codec; `TypeIdVectorsTest` checks all 9 valid and 21 invalid official vectors from `valid.yml` and `invalid.yml`. `TypedIdTest` covers wrong prefix, malformed, case and overflow. `JsonRoundTripTest` (12 tests) covers events, command and sealed results, nil and all-ones UUIDs, 10,000 UUIDs and rejection of malformed or wrong-type IDs. `AxonServerSerializationIntegrationTest` shows on a real server that the TypeID tag finds a course's events and the raw UUID finds none. |
| 4 | MADR ADRs from in-repo templates record spike outcomes (licence behaviour also in quick start, decision-record capture, Kotlin serialization and TypeID conventions, buildpack constraints); CI checks licences against an allowlist; NOTICE justifies the Axoniq modules | VERIFIED | `docs/adr/0001..0014` with no gaps, validated by `checkAdrs` (wired into `check`). ADR-0005 (licence and Platform token), 0006 (decision capture), 0007 (Kotlin IDs, tags, serialization), 0008 (buildpacks: no Node or pnpm in the image per D-35, `BPE_DEFAULT_*` defaults and their labels, `BPE_DEFAULT_JAVA_TOOL_OPTIONS` ignored). `docs/quick-start.md` has the 12-hour window, restart behaviour, token path and the empty-value warning. `governance/dependency-licences.json` holds the allowlist, forbidden families and five Axoniq exceptions with reasons. `checkDependencyLicences` is registered per module and wired into `check` and the CI `build` job. `NOTICE` lists all five exceptions and `checkNotice` guards the mirror. |
| 5 | Every upstream defect has a `docs/upstream/` record; a CI check keeps markers and records consistent; dependency PRs prompt a removal-criteria check; skills and MCP servers installed from pinned sources after approval and listed in the inventory; only `.claude/settings.json` and `.claude/CLAUDE.md` tracked | VERIFIED (repository side; see human items) | 13 records UP-0001..UP-0013 with the required front matter. `checkUpstreamRecords` is in `check`. `UPSTREAM: UP-` markers appear in command-service, compose, doctor, mise, CodeQL, Gradle and `.env.example`. UP-0005 and UP-0013 carry no marker because their `workaround` is false. The `upstream-records` CI job runs `scripts/upstream/changed-dependencies.sh` with `contents: read`. Context7 and Playwright MCP are pinned as `npm:` tools in `mise.toml`; `.mcp.json` has no env block or secret; `settings.json` pre-approves no MCP server. Two plugins are pinned by 40-hex SHA in `extraKnownMarketplaces`. `docs/tooling.md` has the inventory rows and a survey of all nine AIDEV-01 areas. `.gitignore` tracks only `settings.json`, `CLAUDE.md` and the documented `skills/spike-findings-*` exception (D-39, ADR-0014); `doctor` reports `claude-tracking: ok (54 files)`. Five issues confirmed to exist on GitHub, read-only (AxonFramework#5326, #5327, #5328, axoniq-framework#811, #812, all open and authored by the project owner). |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | GH-12 clause "used by the project skills" | Phase 8 | AIDEV-02 (Phase 8) defines the project skills ("write an ADR, draft an upstream issue"); 02-19-SUMMARY states that use by skills stays with Phase 8 |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/01-service-databases.sh` | Runtime stack | VERIFIED | Started and healthy; loopback-bound ports; no `env_file`, no token, no image literal |
| `tooling/test-support/` (`AxonServerEnvironment`, `PostgresEnvironment`, `ContainerImages`) | Container singletons | VERIFIED | Used by `AxonServerEnvironmentIntegrationTest` (1), `PostgresEnvironmentComponentTest` (2) and the command-service tests |
| `build-logic/.../courses.test-suites.gradle.kts` | Suite convention and Kover | VERIFIED | Substantive; applied by every module; TestKit-tested (`TestSuitesFunctionalTest`, 12 tests) |
| `backend/contracts/{kernel,events,commands,queries}` | Contracts | VERIFIED | `queries` is source-less by design (D-02); see Info items |
| `backend/serialization-jackson` | Serialization | VERIFIED | Depends on the kernel only; used by `command-service` and tested |
| `backend/command-service` (capture engine, retry, tag resolver, fixtures) | Spike code for Phase 3 | VERIFIED | `DecisionCaptureEngine` and `RetryingCommandBus` wired in `CommandServiceConfigurationEnhancer`; `DecisionCaptureIntegrationTest` exercises them on Axon Server |
| `tooling/architecture` (Konsist) | Architecture rules | VERIFIED | 16 tests green, 5 classes |
| `tooling/doctor`, `scripts/doctor.sh`, `scripts/test-doctor.sh` | `doctor` | VERIFIED | Ran it, exit 0 |
| Governance checks in `build-logic` (`AdrCheck`, `UpstreamRecordCheck`, `NoticeCheck`, `DependencyLicenceCheck`, `ImageReferenceCheck`, `GeneratedFileCheck`) | Gates | VERIFIED | All registered and in `check`; functional tests green (e.g. 24 + 24 + 17 tests) |
| `docs/adr/0001..0014`, `docs/templates/*` (5), `docs/upstream/0001..0013`, `docs/quick-start.md`, `docs/tooling.md`, `NOTICE` | Documentation | VERIFIED | Present and substantive |
| `.mcp.json`, `.claude/settings.json`, `mise.toml`, `.planning/config.json` `agent_skills` | AI tooling | VERIFIED | Pins confirmed |
| `.github/workflows/ci.yml` (`integration-test`, `compose-smoke`, `doctor`, `upstream-records`, `ci-success`) | CI | VERIFIED (wiring); never run on GitHub | See human item 1 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `gradle/libs.versions.toml` `image-*` | `compose/versions.env` | `generateComposeVersions`, `checkComposeVersions`, `checkImageReferences` | WIRED | Values identical; the check is in `check` |
| Version catalog images | test JVMs | `ContainerImageArguments` system properties, task inputs | WIRED | `useContainerImages()` in `courses.test-suites` |
| `CommandServiceConfigurationEnhancer` | `DecisionCaptureEngine`, `RetryingCommandBus`, `KotlinTagResolver`, Jackson converter | Axon configuration enhancer | WIRED | Retry order `Int.MIN_VALUE + 10`; the capture and serialization integration tests use it |
| `courses.licences` | `check` and CI `build` | `checkDependencyLicences` | WIRED | `ci.yml` line 73 |
| `docs/upstream` records | code markers | `checkUpstreamRecords` | WIRED | Consistent both ways (marker grep matches the open and reported records that declare `workaround: true`) |
| `compose-smoke` job | compose stack and conflict test | `-Pcourses.axonserver.external=localhost:8124` | WIRED | Same command passed locally against a compose stack |
| All new CI jobs | `ci-success` | `needs:` | WIRED | The aggregate lists all eight jobs |
| `.planning/config.json` `agent_skills` | `governance/gsd-config-policy.json` | `checkGsdConfigPolicy` | WIRED | Task is in `check`; the orchestrator's full build passed |

### Data-Flow Trace (Level 4)

Not applicable: no UI or rendered dynamic data in this phase. The equivalent flow was traced for decision capture (`DecisionCaptureEngine` to `DecisionRecorder`) and asserted against real records (criteria, marker, outcome, retries) in `DecisionCaptureIntegrationTest`.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Compose stack starts healthy with no `.env` | `docker compose -p verify02 up -d --wait` | axonserver and postgres healthy | PASS |
| In-network scenario | `docker compose --profile probe run --rm probe` | health and gRPC port reachable | PASS |
| Host scenario: conditional append conflict | `./gradlew :command-service:integrationTest -Pcourses.axonserver.external=localhost:8124 --tests '*ConditionalAppendIntegrationTest*'` | BUILD SUCCESSFUL, 2 tests, 0 failures, fresh timestamp | PASS |
| Service databases created | `psql -c "select datname from pg_database"` in the container | `query_service`, `automation_service` present | PASS |
| `doctor` | `bash scripts/doctor.sh` | exit 0, 12 ok, 2 warn, 0 fail | PASS |
| Test results from the orchestrator's full build | read all 18 `TEST-*.xml` files in backend, tooling and architecture, plus the build-logic functional suites | 0 failures, 0 errors, 0 skipped | PASS |
| Debt markers | grep for TBD, FIXME, XXX, TODO, HACK and PLACEHOLDER in tracked non-planning sources | none | PASS |

I did not re-run the full build; I relied on the orchestrator's 160-task `build --rerun-tasks` and cross-checked its per-suite XML results.

### Probe Execution

No `probe-*.sh` conventions exist in this repository. The phase's probe is the compose `probe` profile service, run above (PASS).

### Requirements Coverage

All 22 phase IDs appear in at least one PLAN `requirements:` field and in REQUIREMENTS.md; no ORPHANED requirement (the traceability table maps no other ID to Phase 2).

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| INFRA-02 | 02-01, 02-03, 02-06, 02-11 | SATISFIED | Four contracts modules, framework-free, enforced by Konsist |
| INFRA-03 | 02-10, 02-12 | SATISFIED | Compose started live |
| INFRA-04 | 02-07, 02-09 | SATISFIED | Conflict test green, also against compose |
| INFRA-05 | 02-12, 02-23 | SATISFIED | ADR-0005, quick-start, `.env.example`; the empty-token trap is guarded (UP-0006) |
| INFRA-06 | 02-06, 02-09 | SATISFIED | JSON round trip and Axon Server serialization tests |
| IDEM-04 | 02-01, 02-04, 02-23 | SATISFIED for Phase 2 scope | Own codec with official vectors, value classes, boundary rejection (`parse`); the PostgreSQL `uuid` storage, API boundary and `typeid-js` clauses are later-phase work (the REQUIREMENTS wording was reconciled in 02-23, ADR-0007) |
| TEST-11 | 02-01, 02-04, 02-07, 02-08, 02-11 | SATISFIED | Three JVM Test Suites, Kover on all |
| TEST-12 | 02-04 | SATISFIED | `testFixtures` in the kernel and command-service, consumed by every suite |
| TEST-14 | 02-01, 02-03, 02-04, 02-07, 02-08 | SATISFIED (as scoped by D-21) | JUnit 6, Kotest assertions and property, Testcontainers, `AxonTestFixture` deferred with the first decision; Awaitility and MockK join with their first consumer per the ROADMAP wording |
| CICD-09 | 02-13, 02-20 | SATISFIED (JVM half) | npm half is Phase 5 per the traceability row |
| UPS-01 | 02-15, 02-16, 02-17 | SATISFIED | 13 records with machine-readable front matter |
| UPS-02 | 02-15, 02-16, 02-17 | SATISFIED (Phase 2 part) | Markers, check and index; the site index is Phase 5 |
| UPS-03 | 02-16, 02-17, 02-22 | PARTIAL, human | Five published after approval. Four drafts (UP-0006, UP-0010, UP-0011, UP-0013) are unreported, held back by the author's decision; see human item 5 |
| UPS-04 | 02-18 | SATISFIED | Advisory job and script plus tests (`test-changed-dependencies.sh`) |
| DOC-03 | 02-19, 02-23 | SATISFIED | 14 ADRs, validated by `checkAdrs` |
| GH-12 | 02-15, 02-19 | SATISFIED, with a deferred clause | Five templates exist; use by skills is Phase 8 |
| TOOL-03 | 02-14 | SATISFIED | `doctor` ran, exit 0 |
| TOOL-04 | 02-07, 02-08, 02-14 | SATISFIED on colima; Docker Desktop is documented but not exercised (human item 6) |
| AIDEV-01 | 02-05, 02-21 | SATISFIED (repository side) | Survey, pinned plugins, inventory |
| AIDEV-03 | 02-05, 02-14 | SATISFIED | `doctor claude-tracking`, `.gitignore` |
| AIDEV-04 | 02-02, 02-14 | SATISFIED | Pinned in mise, `.mcp.json`, `doctor mcp` |
| AIDEV-07 | 02-14 | SATISFIED | `doctor gsd`: version 1.22.0, manifest SHA-256 match, defaults warning present |

Bookkeeping: the REQUIREMENTS.md checkboxes and traceability rows show Pending for 19 of the 22 IDs (only AIDEV-01, AIDEV-03 and AIDEV-04 are marked Complete), and the ROADMAP Phase 2 line is unchecked. These updates belong to the orchestrator once verification is accepted. They are not a goal gap.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | | TBD, FIXME, XXX, TODO, HACK or stub patterns in phase sources | | Clean |

The 11 findings of `02-REVIEW.md` (0 critical, 6 warning, 5 info; all `open` in `02-REVIEW-DISPOSITION.md`) do not break a must-have. Three matter for later phases and are recorded here as warnings, not gaps:

- **WR-01** (`ConflictRetry.kt`): the bound is fixed at 10 attempts with a no-jitter exponential back-off. When many students contend for one open course, the last commands can fail with a raw `AppendEventsTransactionRejectedException`. Phase 2 tests use 12 contenders on 3 seats, where the course fills and contention ends. Phase 3 success criterion 2 (concurrent last-seat race, zero infrastructure errors, bounded transparent retry) will expose this, so triage it at Phase 3 planning.
- **WR-05** (`DecisionCaptureIntegrationTest`, second case): its loop can pass with no committed record. The first test asserts the committed count (equal to `SEATS`), so the permanent safeguard is not vacuous today. Add the count assertion to the second test.
- **WR-02** (`LicencePolicy`): `forbidden` is advisory, so a policy edit that adds a forbidden licence to `allowed` would pass. The file is a governed path, which limits the exposure.

WR-03, WR-04 and WR-06 are edge-case hardening of the tag resolver, a Konsist comment scanner and the image-reference check. The info items are style and duplication notes.

### Human Verification Required

See the `human_verification` frontmatter. Summary:

1. **First GitHub run of the new CI jobs.** Why human: it needs a GitHub runner.
2. **`/plugin` installation of the two skill plugins.** Why human: it is a Claude Code runtime action.
3. **NOTICE and quick-start wording about the Axoniq terms.** Why human: legal and terms judgment.
4. **ADR-0001 and ADR-0002 re-written Considered Options.** Why human: semantic fidelity.
5. **Unreported upstream drafts (UP-0006, UP-0010, UP-0011, UP-0013).** Why human: author approval gate.
6. **Docker Desktop run.** Why human: a different engine than the one used here.

### Gaps Summary

No gaps. The five roadmap success criteria hold in the actual code. Compose runs and is healthy, the conflict test passes against both Testcontainers and the compose server, the codec honours the official vectors, the governance checks are wired and green, and the upstream issues exist. Remaining items are confirmations that only a GitHub runner, Claude Code or the author can give. Deviations that were accepted or documented, not defects: the spike-findings skill is tracked under `.claude/` (D-39, ADR-0014); the contracts rules scan `src/main` only (accepted in ADR-0009); `contracts-queries` is source-less (D-02, IN-05); and GH-12's skill-use clause is deferred to Phase 8.

---

_Verified: 2026-11-23T07:05:00Z_
_Verifier: Claude (gsd-verifier)_
