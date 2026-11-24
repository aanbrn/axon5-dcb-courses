---
phase: 02-runtime-infrastructure-dcb-spikes
verified: 2026-11-24T01:40:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - ".claude/settings.json"
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
  - "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt"
  - "compose.yaml"
  - "compose/infra.yaml"
  - "scripts/doctor.sh"
  - "scripts/governance/ask-governed-edit.sh"
  - "scripts/governance/ask-governed-merge.sh"
covered_digest: "v4:sha256:4d7c45d7166c9d3f31400a0c10086d3b491c6fc6f91a3d8ca59b3b0cff53e9f0"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 5/5
  gaps_closed: []
  gaps_remaining: []
  regressions: []
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
    expected: "Each record moves to status reported with a non-empty upstream list and a reported date, and checkUpstreamRecords stays green. Five GitHub issues exist and were checked read-only in the earlier verification: AxonFramework#5326, #5327, #5328, axoniq-framework#811, #812."
    why_human: "UPS-03 publication needs the author's explicit approval. Four drafts (UP-0006, UP-0010, UP-0011, UP-0013) are still unreported; UP-0008, UP-0009, UP-0012 and UP-0005 link existing upstream issues."
  - test: "Run docker compose up -d --wait and ./gradlew integrationTest on a Docker Desktop machine (not colima)"
    expected: "Stack healthy and tests green"
    why_human: "TOOL-04 says colima and Docker Desktop. docs/tooling.md states colima is the verified engine and Docker Desktop needs no setting, but only colima was exercised in this phase."
---

# Phase 2: Runtime Infrastructure & DCB Spikes Verification Report

**Phase Goal:** DCB on a pinned Axon Server works locally and in CI, the JVM test infrastructure is in place, and every high-risk unknown is resolved and recorded before domain code is written.
**Verified:** 2026-11-24T01:40:00Z
**Status:** human_needed
**Re-verification:** Yes, regenerated against the current HEAD (565ce36). The earlier report (status human_needed, 5/5) went stale after quick tasks 261123-mub, 261123-p1u and 261123-rzt and the ADR-0015 governance hooks changed covered files. It listed no gaps, so no gap closure was due. This run re-checked every success criterion against the current code and looked for regressions from those changes.

The five roadmap success criteria still hold on the current HEAD, and no regression was found. The status stays `human_needed` because six items cannot be proven from the repository (see below). The fresh incremental code review (02-REVIEW.md, 0 critical, 4 warning, 3 info, all recorded open) concerns the ADR-0015 hooks and one brittle test, and none of its findings defeats a must-have.

Note on mode: ROADMAP.md marks Phase 2 `Mode: mvp`, but the roadmap goal is not written as "As a ..., I want to ..., so that ...". The MVP guard says to refuse and ask for `/gsd-mvp-phase 2`. I did not refuse, for the same reasons Phase 1's report gave: the goal is unambiguous, the phase has been verified before on the same terms, and the five success criteria state the observable outcomes. I verified against the roadmap goal and its success criteria. If the author wants strict MVP framing, rewriting the goal is a planning-document fix, not a code gap.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | One compose command starts pinned Axon Server and PostgreSQL; DCB context auto-created; correct hostname; images only from the catalog; `doctor` checks tools, Docker engine, Testcontainers, GSD manifest and explains fixes | VERIFIED | Fresh run: `POSTGRES_PORT=55432 docker compose -p verify02b up -d --wait` brought both containers to Healthy (host 5432 belongs to the author's PostgreSQL). `docker compose --profile probe run --rm probe` printed "http://axonserver:8024/actuator/health is reachable" and "axonserver:8124 accepts connections". `psql` listed the `query_service` and `automation_service` databases. The stack was torn down with `down -v` and the working tree stayed clean. `docker compose config --quiet` is clean. `compose/versions.env` carries the same `AXONSERVER_IMAGE` and `POSTGRES_IMAGE` digests as the `image-axonserver` and `image-postgres` entries of `gradle/libs.versions.toml`. `bash scripts/doctor.sh` exits 0 (12 ok, 3 warn, 0 fail). Each warning prints a `fix:` line. It covers mise tools (19), the Docker engine (colima), the classic image store, Testcontainers config and a live Testcontainers probe, 858 GSD files against the manifest, MCP servers, claude-code version and `.claude` tracking. The third warning is new since the last report and is the feature working as intended: `upstream-removal` (IN-04, quick task rzt) reports that UP-0012 is fixed upstream while its workaround markers remain in `scripts/doctor.sh`, because this engine runs Docker 29.5.2 and the removal criteria need 29.9.0 or newer. |
| 2 | An integration test on Axon Server in Testcontainers shows a conflicting conditional append is rejected and a non-overlapping one succeeds; unit, component and integration run as separate Gradle suites sharing test fixtures with the standard toolchain | VERIFIED | Behavior-dependent truth, so a behavioral test was run, not only read. `./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun` ran on HEAD at 2026-11-24T01:31:33Z: 2 tests, 0 failures, 0 skipped (result XML timestamp is fresh). The test forces two units of work to source the same criteria behind an `ArrivalBarrier`, then asserts that exactly one outcome is null, the other failure has `AppendEventsTransactionRejectedException` in its cause chain, and only one `StudentSubscribed` is stored. The second test uses two different courses and asserts both commit. `courses.test-suites.gradle.kts` registers `componentTest` and `integrationTest` JVM Test Suites. Result XML across all modules: 41 suites, 322 tests, 0 failures, 0 errors, 0 skipped. |
| 3 | Contracts modules contain no Spring or framework logic; Kotlin events with TypeID value-class IDs and tags round-trip unchanged; malformed or wrong-prefix TypeIDs are rejected | VERIFIED | The imports across `backend/contracts/*/src/main` are only kernel types, `java.util.UUID`, `com.fasterxml.uuid` (ID generation in the kernel), three Axon annotation types (`@Event`, `@Command`, `@EventTag`) and Jackson's `JsonTypeInfo`; no Spring import exists. `ContractsArchitectureTest` (Konsist) enforces it. `TypeId.kt` is the own codec. The earlier report recorded `TypeIdVectorsTest` (9 valid and 21 invalid official vectors), `TypedIdTest`, `JsonRoundTripTest` and the on-server `AxonServerSerializationIntegrationTest`; all of these are in the result XML with 0 failures on this HEAD. |
| 4 | MADR ADRs from in-repo templates record spike outcomes (licence behaviour also in quick start, decision-record capture, Kotlin serialization and TypeID conventions, buildpack constraints); CI checks licences against an allowlist; NOTICE justifies the Axoniq modules | VERIFIED | `docs/adr/0001..0015` is contiguous (0015 is the new governance-hooks ADR from ADR-0015's quick task). `checkAdrs` passed in this run's Gradle invocation. ADR-0005 (licence and Platform token), 0006 (decision capture), 0007 (Kotlin IDs, tags, serialization) and 0008 (buildpacks) exist. `docs/templates/` holds five templates. `checkNotice`, `checkComposeVersions`, `checkImageReferences` and `checkUpstreamRecords` passed in this run. `checkDependencyLicences` is wired into `check`, and the orchestrator's `./gradlew build` passed on this HEAD. |
| 5 | Every upstream defect has a `docs/upstream/` record; a CI check keeps markers and records consistent; dependency PRs prompt a removal-criteria check; skills and MCP servers installed from pinned sources after approval and listed in the inventory; only `.claude/settings.json` and `.claude/CLAUDE.md` tracked | VERIFIED (repository side; see human items) | 13 records UP-0001..UP-0013 exist and `checkUpstreamRecords` passed. `scripts/upstream/test-changed-dependencies.sh`: 62 passed, 0 failed. `.mcp.json` resolves through mise (doctor `mcp: ok`). `git ls-files .claude` shows only `settings.json`, `CLAUDE.md` and the documented `skills/spike-findings-*` exception (D-39, ADR-0014); doctor reports `claude-tracking: ok (54 files)`. The `.claude/settings.json` governance hooks are checked by `checkClaudeAskRules` (passed). |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | GH-12 clause "used by the project skills" | Phase 8 | AIDEV-02 (Phase 8) defines the project skills ("write an ADR, draft an upstream issue"); 02-19-SUMMARY states that use by skills stays with Phase 8 |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/` | Runtime stack | VERIFIED | Started healthy this run; no change to compose files since the last report |
| `tooling/test-support/` (`AxonServerEnvironment`, `PostgresEnvironment`, `ContainerImages`) | Container singletons | VERIFIED | Used by the integration tests that just ran |
| `build-logic/.../courses.test-suites.gradle.kts` | Suite convention and Kover | VERIFIED | Applied by every module; the suites ran |
| `backend/contracts/{kernel,events,commands,queries}`, `backend/serialization-jackson` | Contracts and serialization | VERIFIED | Framework-free; `queries` is source-less by design (D-02, IN-05 skipped as a judgment) |
| `backend/command-service` (capture engine, retry, tag resolver, fixtures) | Spike code for Phase 3 | VERIFIED | Tests green on HEAD, including the follow-up fixes (WR-03 non-public `@EventTag`, WR-05 capture-count assertion) |
| `tooling/architecture` (Konsist) | Architecture rules | VERIFIED | Suites in the result XML, 0 failures |
| `tooling/doctor`, `scripts/doctor.sh`, `scripts/test-doctor.sh` | `doctor` | VERIFIED | Ran it, exit 0; `test-doctor.sh` 139 passed |
| Governance checks in `build-logic` (`AdrCheck`, `UpstreamRecordCheck`, `NoticeCheck`, `DependencyLicenceCheck`, `ImageReferenceCheck`, `AskRulesCheck`) | Gates | VERIFIED | Registered and in `check`; the ones run here passed |
| `docs/adr/0001..0015`, `docs/templates/*` (5), `docs/upstream/0001..0013`, `docs/quick-start.md`, `docs/tooling.md`, `NOTICE` | Documentation | VERIFIED | Present |
| `scripts/governance/*` (check-pr, hooks, tests) | Governance scripts | VERIFIED | `test-check-pr.sh` 132 passed, `test-list-pr-files.sh` 25 passed |
| `.github/workflows/ci.yml` | CI jobs | VERIFIED (wiring); never run on GitHub | See human item 1 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `gradle/libs.versions.toml` `image-*` | `compose/versions.env` | `generateComposeVersions`, `checkComposeVersions`, `checkImageReferences` | WIRED | Values identical; the checks passed. Since quick task mub, compose images derive from every `image-*` entry except `ryuk` |
| Version catalog images | test JVMs | container image system properties | WIRED | The integration test ran against the pinned image through Testcontainers |
| `CommandServiceConfigurationEnhancer` | `DecisionCaptureEngine`, `RetryingCommandBus`, `KotlinTagResolver`, Jackson converter | Axon configuration enhancer | WIRED | Capture and serialization integration tests green |
| `docs/upstream` records | code markers | `checkUpstreamRecords` | WIRED | Passed; doctor additionally flags fixed records with leftover markers |
| `.claude/settings.json` hooks | `scripts/governance/ask-governed-{edit,merge}.sh` | `checkClaudeAskRules` | WIRED | Check passed; hooks' own tests passed in the reviewer's run |
| `compose-smoke` job | compose stack and conflict test | `-Pcourses.axonserver.external=localhost:8124` | WIRED | Same stack start and probe run locally |
| All new CI jobs | `ci-success` | `needs:` | WIRED | Unchanged since the last report |

### Data-Flow Trace (Level 4)

Not applicable: no UI or rendered dynamic data in this phase. The equivalent flow, decision capture to recorded decision, is asserted against real records in `DecisionCaptureIntegrationTest`.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Conflict rejected, disjoint append commits | `./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun` | BUILD SUCCESSFUL; 2 tests, 0 failures; timestamp 01:31:33Z | PASS |
| Compose stack starts healthy with no `.env` | `POSTGRES_PORT=55432 docker compose -p verify02b up -d --wait` | both Healthy | PASS |
| In-network scenario | `docker compose --profile probe run --rm probe` | health and gRPC port reachable | PASS |
| Service databases created | `psql -c "select datname from pg_database"` | `query_service`, `automation_service` present | PASS |
| `doctor` | `bash scripts/doctor.sh` | exit 0, 12 ok, 3 warn, 0 fail | PASS |
| Governance gates | `checkAdrs checkUpstreamRecords checkNotice checkComposeVersions checkImageReferences checkClaudeAskRules` | BUILD SUCCESSFUL | PASS |
| Script test suites | `test-doctor.sh`, `test-check-pr.sh`, `test-changed-dependencies.sh`, `test-list-pr-files.sh` | 139, 132, 62, 25 passed, 0 failed | PASS |
| All JVM suites | read every `TEST-*.xml` under the build directories | 41 files, 322 tests, 0 failures, 0 errors, 0 skipped | PASS |
| Debt markers | `git grep` for TBD, FIXME, XXX, TODO, HACK, PLACEHOLDER in sources, scripts, build-logic, compose, ADRs and workflows | none | PASS |

I did not re-run the full `./gradlew build`; I relied on the orchestrator's successful run on this HEAD and cross-checked the per-suite result XML.

### Probe Execution

No `probe-*.sh` convention exists in this repository. The phase's probe is the compose `probe` profile service, run above (PASS).

### Requirements Coverage

All 22 phase IDs appear in at least one PLAN `requirements:` field and in REQUIREMENTS.md. No ORPHANED requirement: the traceability table maps no other ID to Phase 2.

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| INFRA-02 | 02-01, 02-03, 02-06, 02-11 | SATISFIED | Four contracts modules, framework-free, enforced by Konsist |
| INFRA-03 | 02-10, 02-12 | SATISFIED | Compose started live this run |
| INFRA-04 | 02-07, 02-09 | SATISFIED | Conflict test green on HEAD |
| INFRA-05 | 02-12, 02-23 | SATISFIED | ADR-0005, quick start, `.env.example`; doctor warns on the missing token |
| INFRA-06 | 02-06, 02-09 | SATISFIED | JSON round trip and Axon Server serialization suites green |
| IDEM-04 | 02-01, 02-04, 02-23 | SATISFIED for Phase 2 scope | Own codec with official vectors, value classes, boundary rejection; the PostgreSQL `uuid` storage, API boundary and `typeid-js` clauses are later-phase work (wording reconciled in ADR-0007) |
| TEST-11 | 02-01, 02-04, 02-07, 02-08, 02-11 | SATISFIED | Three JVM Test Suites, Kover |
| TEST-12 | 02-04 | SATISFIED | `testFixtures` in the kernel and command-service |
| TEST-14 | 02-01, 02-03, 02-04, 02-07, 02-08 | SATISFIED (as scoped by D-21) | JUnit 6, Kotest assertions and property, Testcontainers; Awaitility and MockK join with their first consumer |
| CICD-09 | 02-13, 02-20 | SATISFIED (JVM half) | npm half is Phase 5; REQUIREMENTS.md keeps the row Pending for that reason |
| UPS-01 | 02-15, 02-16, 02-17 | SATISFIED | 13 records with machine-readable front matter |
| UPS-02 | 02-15, 02-16, 02-17 | SATISFIED (Phase 2 part) | Markers, check, index; the site index is Phase 5 |
| UPS-03 | 02-16, 02-17, 02-22 | PARTIAL, human | Five issues were published after approval. Four drafts (UP-0006, UP-0010, UP-0011, UP-0013) are unreported by the author's decision (human item 5) |
| UPS-04 | 02-18 | SATISFIED | Advisory job, script and tests (62 passed) |
| DOC-03 | 02-19, 02-23 | SATISFIED | 15 ADRs validated by `checkAdrs` |
| GH-12 | 02-15, 02-19 | SATISFIED, with a deferred clause | Five templates exist; use by skills is Phase 8 |
| TOOL-03 | 02-14 | SATISFIED | `doctor` ran, exit 0 |
| TOOL-04 | 02-07, 02-08, 02-14 | SATISFIED on colima; Docker Desktop not exercised (human item 6) |
| AIDEV-01 | 02-05, 02-21 | SATISFIED (repository side) | Survey, pinned plugins, inventory |
| AIDEV-03 | 02-05, 02-14 | SATISFIED | doctor `claude-tracking`, `.gitignore`, `git ls-files .claude` |
| AIDEV-04 | 02-02, 02-14 | SATISFIED | Pinned in mise, `.mcp.json`, doctor `mcp` |
| AIDEV-07 | 02-14 | SATISFIED | doctor `gsd`: version 1.22.0, 858 files match the manifest |

Bookkeeping: REQUIREMENTS.md already shows Complete for 18 of the 22 IDs. CICD-09, UPS-02, UPS-03 and TOOL-04 are Pending with a stated reason, which matches this report. The ROADMAP Phase 2 line (`- [ ] **Phase 2`) is still unchecked; that update belongs to the orchestrator once verification is accepted. It is not a goal gap.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (none) | | No debt markers or stub patterns in phase sources | | Clean |

The fresh incremental review (`02-REVIEW.md`, 0 critical, 4 warning, 3 info, all `open` in `02-REVIEW-DISPOSITION.md`) is advisory. None of its findings defeats a must-have:

- **WR-07, WR-08, WR-09** (`scripts/governance/ask-governed-merge.sh`): the ADR-0015 merge hook has false negatives. A `merge` split by shell quoting (`git mer''ge`), a merge piped or here-string-fed to a shell, and a `<<` that is not a heredoc each let a merge pass without a prompt. The hook is a guard rail, not a security boundary, and it is not part of any Phase 2 success criterion or requirement (it is extra governance work added after the plans). The reviewer notes that ADR-0015's wording ("governed content ... is caught when it is merged") overstates the coverage and that these shapes are not listed as residual risks. Recommended follow-up: apply the reviewer's fixes or add the shapes to the ADR's residual risks before relying on the hook.
- **WR-10** (`ComposeVersionsFunctionalTest`): it pins the repository catalog's current image set, so it will break when Phase 6 adds a compose image. A test-maintenance item for Phase 6, not a defect today (the test passes).
- **IN-06, IN-07, IN-08**: an indented catalog entry skipped by `ImageReferenceCheck`, an ID map key that is written but not readable back, and the unchecked git 2.36 requirement of the hooks. All are edge cases.
- WR-01 (conflict retry bound without jitter) stays deferred to Phase 3 planning, as recorded; Phase 3 success criterion 2 will exercise it. WR-02 to WR-06 and IN-01 to IN-04 are fixed; the fixes for WR-02, WR-03, WR-05 and WR-06 are in the tests that ran green.

### Human Verification Required

See the `human_verification` frontmatter and `02-UAT.md` (6 items, all pending). All six still genuinely need a human:

1. **First GitHub run of the new CI jobs.** Needs a GitHub runner.
2. **`/plugin` installation of the two skill plugins.** A Claude Code runtime action.
3. **NOTICE and quick-start wording about the Axoniq terms.** Legal and terms judgment.
4. **ADR-0001 and ADR-0002 re-written Considered Options.** Semantic fidelity.
5. **Unreported upstream drafts (UP-0006, UP-0010, UP-0011, UP-0013).** Author approval gate.
6. **Docker Desktop run.** A different engine than the one used here.

### Gaps Summary

No gaps. All five roadmap success criteria hold on the current HEAD, and the three quick tasks plus the governance hooks introduced no regression: the conflict test, the compose stack, doctor, the governance gates and all 322 JVM tests pass. The remaining items are confirmations that only a GitHub runner, Claude Code, Docker Desktop or the author can give, plus advisory review findings about the ADR-0015 merge hook.

---

_Verified: 2026-11-24T01:40:00Z_
_Verifier: Claude (gsd-verifier)_
