---
phase: 02-runtime-infrastructure-dcb-spikes
verified: 2026-11-24T07:40:00Z
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
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt"
  - "build-logic/src/main/kotlin/courses.test-suites.gradle.kts"
  - "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt"
  - "compose.yaml"
  - "compose/infra.yaml"
  - "scripts/doctor.sh"
  - "scripts/governance/ask-governed-edit.sh"
  - "scripts/governance/ask-governed-merge.sh"
covered_digest: "v4:sha256:1d7d37c4f9309799858acf455f1d628520845c785d4ad89bf0d02a0692bc7f28"
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
    expected: "The structure follows MADR with decisions, dates and statuses unchanged (02-19 prohibition)"
    why_human: "Whether a normalisation preserved the meaning of an earlier decision is a semantic judgment. checkAdrs validates structure only."
  - test: "Post the forum or support drafts for UP-0006 (empty Platform token) and UP-0013 (dcbContext flag), decide on UP-0010 and UP-0011, then record the URLs and set status to reported"
    expected: "Each record moves to status reported with a non-empty upstream list and a reported date, and checkUpstreamRecords stays green. Five GitHub issues exist: AxonFramework#5326, #5327, #5328, axoniq-framework#811, #812."
    why_human: "UPS-03 publication needs the author's explicit approval. Four drafts (UP-0006, UP-0010, UP-0011, UP-0013) are still unreported."
  - test: "Run docker compose up -d --wait and ./gradlew integrationTest on a Docker Desktop machine (not colima)"
    expected: "Stack healthy and tests green"
    why_human: "TOOL-04 says colima and Docker Desktop. docs/tooling.md states colima is the verified engine, but only colima was exercised in this phase (the author declined installing Docker Desktop)."
---

# Phase 2: Runtime Infrastructure & DCB Spikes Verification Report

**Phase Goal:** DCB on a pinned Axon Server works locally and in CI, the JVM test infrastructure is in place, and every high-risk unknown is resolved and recorded before domain code is written.
**Verified:** 2026-11-24T07:40:00Z
**Status:** human_needed
**Re-verification:** Yes. The previous report (human_needed, 5/5, no gaps) went stale because quick tasks 261124-c1g, -dvp, -eum, -gju and -hbh changed covered files. It listed no `gaps:`, so there was no gap closure to check. I re-checked every success criterion against the current HEAD (5c14be1), ran the behavioral evidence again instead of reusing it, and looked for regressions from those quick tasks.

All five roadmap success criteria hold on the current HEAD. The status stays `human_needed` because six items cannot be proven from the repository (`02-UAT.md`, all pending). The incremental code review (`02-REVIEW.md`: 1 critical, 6 warnings, all open) concerns only the ADR-0015 governance hooks, a doctor edge case and two stale upstream records. I read the code behind the critical finding and weighed it against each success criterion (see Anti-Patterns). It does not defeat any Phase 2 criterion, but it is a real defect that should be fixed or recorded before the pull request merges.

Note on mode: ROADMAP.md marks Phase 2 `Mode: mvp`, but the goal is not written as "As a ..., I want to ..., so that ...", so the MVP guard would refuse. I did not refuse, for the reason Phase 1's report and the previous Phase 2 report gave: the goal is unambiguous and the five success criteria state the observable outcomes. Rewriting the goal is a planning-document fix, not a code gap.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | One compose command starts pinned Axon Server and PostgreSQL; DCB context auto-created; correct hostname; images only from the catalog; `doctor` checks tools, Docker engine, Testcontainers, GSD manifest and explains fixes | VERIFIED | I ran `POSTGRES_PORT=55432 docker compose -p verify02c up -d --wait` in this session: both containers reached Healthy. `docker compose --profile probe run --rm probe` printed "http://axonserver:8024/actuator/health is reachable" and "axonserver:8124 accepts connections". The `postgres` service listed the `query_service` and `automation_service` databases. I tore the stack down with `down -v`; the working tree stayed clean. `compose/infra.yaml` sets `hostname: axonserver` and `AXONIQ_AXONSERVER_STANDALONE_DCB=true` (the DCB `default` context is created at boot, no init container). Image references are `${AXONSERVER_IMAGE:?...}` and `${POSTGRES_IMAGE:?...}`, whose values in `compose/versions.env` equal the `image-axonserver` and `image-postgres` catalog entries (same tag and sha256 digest, compared by eye). `docker compose config --quiet` is clean. `bash scripts/doctor.sh` exit 0 (12 ok, 3 warn, 0 fail); its Testcontainers probe printed `ok testcontainers: unix:///var/run/docker.sock, server 29.5.2, ryuk enabled`. `scripts/test-doctor.sh`: 139 passed, 0 failed. |
| 2 | An integration test on Axon Server in Testcontainers shows a conflicting conditional append is rejected and a non-overlapping one succeeds; unit, component and integration run as separate Gradle suites sharing test fixtures with the standard toolchain | VERIFIED | Behavior-dependent truth, so I ran the test: `./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun` on HEAD, BUILD SUCCESSFUL, result XML `tests=2 skipped=0 failures=0 errors=0` timestamp 2026-11-24T07:25:45Z (fresh). The test holds two units of work behind an `ArrivalBarrier` so both source the same criteria, then asserts one outcome is rejected with `AppendEventsTransactionRejectedException` in the cause chain; the second test uses two different courses and asserts both commit. `courses.test-suites.gradle.kts` registers the `componentTest` and `integrationTest` JVM Test Suites; `testFixtures` exist in the kernel and command-service (TEST-12). The orchestrator's full `./gradlew build` at HEAD was green (43 test classes, 328 tests, 0 failures); my own `mise x -- ./gradlew build` also reported BUILD SUCCESSFUL (up to date, so it re-proved the cache, not the tests). |
| 3 | Contracts modules contain no Spring or framework logic; Kotlin events with TypeID value-class IDs and tags round-trip unchanged; malformed or wrong-prefix TypeIDs are rejected | VERIFIED | I filtered the imports of `backend/contracts/*/src/main`: besides kernel types, `java.util.UUID`, `com.fasterxml.uuid`, the three Axon annotation types and Jackson's `JsonTypeInfo`, nothing remains (no Spring). `ContractsArchitectureTest` (Konsist) enforces it. `TypeId.kt` is the own codec. `TypeIdVectorsTest`, `TypedIdTest`, `JsonRoundTripTest` and the on-server `AxonServerSerializationIntegrationTest` are in the green full build. |
| 4 | MADR ADRs from in-repo templates record the spike outcomes (licence behaviour also in the quick start, decision-record capture, Kotlin serialization and TypeID conventions, buildpack constraints); CI checks licences against an allowlist; NOTICE justifies the Axoniq modules | VERIFIED | `docs/adr/0001..0015` is contiguous; 0005 (licence and Platform token), 0006 (decision capture), 0007 (Kotlin IDs, tags, serialization) and 0008 (buildpacks) exist; `docs/templates/` holds the five templates. `checkAdrs`, `checkNotice`, `checkComposeVersions`, `checkImageReferences`, `checkUpstreamRecords` and `checkClaudeAskRules` all report BUILD SUCCESSFUL in this session (the tasks were UP-TO-DATE, which means their inputs did not change since a passing run). `checkDependencyLicences` is wired into `check`, which the green build ran. |
| 5 | Every upstream defect has a `docs/upstream/` record; a CI check keeps markers and records consistent; dependency PRs prompt a removal-criteria check; skills and MCP servers installed from pinned sources after approval and listed in the inventory; only `.claude/settings.json` and `.claude/CLAUDE.md` tracked | VERIFIED (repository side; see human items) | `docs/upstream/0001..0013` exist and `checkUpstreamRecords` passes. `scripts/upstream/test-changed-dependencies.sh`: 62 passed, 0 failed. `.claude/settings.json` registers both marketplaces by 40-hex `ref`. `.mcp.json` pins Context7 and Playwright through `mise x --`. `git ls-files .claude` lists 54 files: `settings.json`, `CLAUDE.md` and the documented `skills/spike-findings-*` exception (ADR-0014, D-39), nothing else. |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | GH-12 clause "used by the project skills" | Phase 8 | AIDEV-02 (Phase 8) defines the project skills ("write an ADR, draft an upstream issue"); 02-19-SUMMARY states that use by skills stays with Phase 8 |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/` | Runtime stack | VERIFIED | Started healthy in this session |
| `tooling/test-support/` (`AxonServerEnvironment`, `PostgresEnvironment`, `ContainerImages`) | Container singletons | VERIFIED | Used by the integration test I ran |
| `build-logic/.../courses.test-suites.gradle.kts` | Suite convention and Kover | VERIFIED | Applied by every module; the suites ran |
| `backend/contracts/{kernel,events,commands,queries}`, `backend/serialization-jackson` | Contracts and serialization | VERIFIED | Framework-free; `queries` is source-less by design (D-02) |
| `backend/command-service` (capture engine, retry, tag resolver, fixtures) | Spike code for Phase 3 | VERIFIED | Tests green in the full build |
| `tooling/architecture` (Konsist) | Architecture rules | VERIFIED | Suites green |
| `tooling/doctor`, `scripts/doctor.sh`, `scripts/test-doctor.sh` | `doctor` | VERIFIED | Ran it, exit 0; 139 test checks passed |
| Governance checks in `build-logic` | Gates | VERIFIED | Registered, in `check`, passing |
| `docs/adr/0001..0015`, `docs/templates/*`, `docs/upstream/0001..0013`, `docs/quick-start.md`, `docs/tooling.md`, `NOTICE` | Documentation | VERIFIED | Present |
| `scripts/governance/*` and their tests | Governance scripts | VERIFIED with a known defect (CR-01) | `test-check-pr.sh` 173, `test-list-pr-files.sh` 25, `test-ask-governed-edit.sh` 58, `test-ask-governed-merge.sh` 120 passed, 0 failed |
| `.github/workflows/ci.yml` | CI jobs | VERIFIED (wiring); never run on GitHub | See human item 1 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `gradle/libs.versions.toml` `image-*` | `compose/versions.env` | `generateComposeVersions`, `checkComposeVersions`, `checkImageReferences` | WIRED | Values identical; checks pass |
| Version catalog images | test JVMs | container image system properties | WIRED | The integration test ran against the pinned image |
| `CommandServiceConfigurationEnhancer` | `DecisionCaptureEngine`, `RetryingCommandBus`, `KotlinTagResolver`, Jackson converter | Axon configuration enhancer | WIRED | Capture and serialization integration tests green |
| `docs/upstream` records | code markers | `checkUpstreamRecords` | WIRED | Passes |
| `.claude/settings.json` hooks | `scripts/governance/ask-governed-{edit,merge}.sh` | `checkClaudeAskRules` | WIRED | Check passes; the hooks' own suites pass |
| `compose-smoke` job | compose stack and conflict test | `-Pcourses.axonserver.external=localhost:8124` | WIRED | Same start and probe run locally |
| All new CI jobs | `ci-success` | `needs: [build, lint, secrets, qodana, integration-test, compose-smoke, doctor, upstream-records]` | WIRED | Read in `ci.yml` line 390 |

### Data-Flow Trace (Level 4)

Not applicable: no UI or rendered dynamic data. The equivalent flow, decision capture to recorded decision, is asserted against real records in `DecisionCaptureIntegrationTest`.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Conflict rejected, disjoint append commits | `./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun` | BUILD SUCCESSFUL; 2 tests, 0 failures | PASS |
| Compose stack starts healthy, no `.env` | `POSTGRES_PORT=55432 docker compose -p verify02c up -d --wait` | both Healthy | PASS |
| In-network scenario | `docker compose --profile probe run --rm probe` | health URL and gRPC port reachable | PASS |
| Service databases created | `psql -c "select datname from pg_database"` | `query_service`, `automation_service` present | PASS |
| `docker compose config` | `docker compose config --quiet` | clean | PASS |
| `doctor` | `bash scripts/doctor.sh` | exit 0, 12 ok, 3 warn, 0 fail | PASS |
| Governance gates | `checkAdrs checkUpstreamRecords checkNotice checkComposeVersions checkImageReferences checkClaudeAskRules` | BUILD SUCCESSFUL | PASS |
| Shell suites | six `test-*.sh` scripts | 139, 173, 62, 25, 58, 120 passed, 0 failed | PASS |
| Whole build | `mise x -- ./gradlew build` | BUILD SUCCESSFUL (cached); orchestrator's non-cached run: 328 tests, 0 failures | PASS |
| Debt markers | `git grep` for TBD, FIXME, XXX in backend, build-logic, scripts, compose, workflows, ADRs, upstream records, tooling | none | PASS |

A plain `./gradlew build` outside `mise` fails in `verifyShfmt`, `verifyTaplo` and `verifyNodeVersion` because this shell has no mise activation (Node 24 active, `.nvmrc` requires 26). That is the documented behavior of those guards ("run the build as `mise x -- ./gradlew ...`"), not a defect.

### Probe Execution

No `probe-*.sh` convention exists in this repository. The phase's probe is the compose `probe` profile service, run above (PASS).

### Requirements Coverage

All 22 phase IDs appear in the `requirements:` field of at least one PLAN and in REQUIREMENTS.md. No ORPHANED requirement: the traceability table maps no other ID to Phase 2.

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| INFRA-02 | 02-01, 02-03, 02-06, 02-11 | SATISFIED | Contracts modules framework-free, enforced by Konsist |
| INFRA-03 | 02-10, 02-12 | SATISFIED | Compose started live this session |
| INFRA-04 | 02-07, 02-09 | SATISFIED | Conflict test green, re-run this session |
| INFRA-05 | 02-12, 02-23 | SATISFIED | ADR-0005, quick start, `.env.example`; doctor warns on the missing token |
| INFRA-06 | 02-06, 02-09 | SATISFIED | JSON round trip and on-server serialization suites green |
| IDEM-04 | 02-01, 02-04, 02-23 | SATISFIED for Phase 2 scope | Own codec with official vectors, value classes, boundary rejection; PostgreSQL `uuid` storage, API boundary and `typeid-js` clauses are later-phase work (ADR-0007) |
| TEST-11 | 02-01, 02-04, 02-07, 02-08, 02-11 | SATISFIED | Three JVM Test Suites, Kover |
| TEST-12 | 02-04 | SATISFIED | `testFixtures` in the kernel and command-service |
| TEST-14 | 02-01, 02-03, 02-04, 02-07, 02-08 | SATISFIED (as scoped by D-21) | JUnit 6, Kotest assertions and property, Testcontainers; Awaitility and MockK join with their first consumer |
| CICD-09 | 02-13, 02-20 | SATISFIED (JVM half) | npm half is Phase 5; REQUIREMENTS.md keeps the row Pending for that reason |
| UPS-01 | 02-15, 02-16, 02-17 | SATISFIED | 13 records with machine-readable front matter |
| UPS-02 | 02-15, 02-16, 02-17 | SATISFIED (Phase 2 part) | Markers, check, index; the site index is Phase 5 |
| UPS-03 | 02-16, 02-17, 02-22 | PARTIAL, human | Five issues published after approval. Four drafts (UP-0006, UP-0010, UP-0011, UP-0013) unreported pending the author (human item 5) |
| UPS-04 | 02-18 | SATISFIED | Advisory job, script, 62 checks passed |
| DOC-03 | 02-19, 02-23 | SATISFIED | 15 ADRs validated by `checkAdrs` |
| GH-12 | 02-15, 02-19 | SATISFIED, with a deferred clause | Five templates exist; use by skills is Phase 8 |
| TOOL-03 | 02-14 | SATISFIED | `doctor` ran, exit 0 |
| TOOL-04 | 02-07, 02-08, 02-14 | SATISFIED on colima; Docker Desktop not exercised (human item 6) | |
| AIDEV-01 | 02-05, 02-21 | SATISFIED (repository side) | Survey, SHA-pinned plugins, inventory |
| AIDEV-03 | 02-05, 02-14 | SATISFIED | `git ls-files .claude` shows only the allowed files; doctor `claude-tracking` |
| AIDEV-04 | 02-02, 02-14 | SATISFIED | `.mcp.json` pinned through mise; doctor `mcp` |
| AIDEV-07 | 02-14 | SATISFIED | doctor `gsd` check passes against the manifest |

Bookkeeping: REQUIREMENTS.md shows Complete for 18 of the 22 IDs. CICD-09, UPS-02, UPS-03 and TOOL-04 are Pending with a stated reason, which matches this report. The ROADMAP Phase 2 checkbox is still unchecked; the orchestrator updates it once verification is accepted.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `scripts/governance/ask-governed-merge.sh` | 820-829 | CR-01: `analyze_shell` skips an option value only for the exact words `-o`, `+o`, `-O`, `+O`. In `-euo pipefail` or `-eo pipefail` the `o` takes `pipefail`, but the hook reads `pipefail` as the script and never reaches `-c`; `--rcfile <file>` has the same fault. I read the code and it matches the reviewer's account. | Warning (advisory) | A merge of governed content spelled `bash -euo pipefail -c 'git merge ...'` is not prompted. Not tied to any Phase 2 success criterion or requirement |
| same file | 668-694, 948-1000, 1034-1041, 567-598 | WR-01 to WR-03: option forms of `git`, `sudo`, `env`, a `GIT_DIR` prefix, brace expansion, and refs moved earlier in the same command | Warning (advisory) | Same: further forms in which the merge hook does not ask |
| `scripts/governance/ask-governed-edit.sh` | 228-238 | WR-04: the agent-worktree exemption does not hold for `**/` governed entries, so those four names still prompt | Warning (advisory) | Extra prompts only, harmless for safety |
| `scripts/doctor.sh` | 358-361, 379-381, 429 | WR-05: install command built from an unvalidated GSD pin | Warning (advisory) | A blank pin prints `npx -y @opengsd/gsd-core@ ...`; the pin file is governed |
| `docs/upstream/0001-...md`, `0004-...md` | 114, 99 | WR-06: records still say the contribution is undecided; STATE.md records the author's decision of 2026-11-24 to contribute | Warning (documentation) | A public record states the opposite of the decision |
| (none) | | No debt markers or stub patterns in phase sources | | Clean |

How I weighed the critical finding against the success criteria:

- SC1 to SC4 do not mention governance hooks. SC5 asks that only `.claude/settings.json` and `.claude/CLAUDE.md` are tracked and that upstream records, checks and pinned tooling exist. The hooks are not part of it, and `checkClaudeAskRules` (which guards the hook registration and the ask rules in `settings.json`) passes.
- The nearest criterion is Phase 1 SC5 ("the agent must ask for confirmation before it edits governance files"), already verified in Phase 1. The main-checkout edit hook and the `Edit(...)` ask rules still prompt; the CI `check-pr.sh` gate (173 checks passed) still requires the `governance-approved` label. The gap is limited to one spelling of a shell invocation in the merge hook that ADR-0015 describes as the confirmation point for agent-worktree work, and it needs an agent to write that exact form.
- Conclusion: advisory, not a Phase 2 blocker. It is a genuine defect in a control that ADR-0015 says covers `bash -c`, and ADR-0015's Residual risks section does not list it. The author should either apply the reviewer's fix (with test cases for `bash -euo pipefail -c`, `bash -eo pipefail -c`, `sh -eu -c`, `bash --rcfile x -c`) or add the forms to Residual risks before relying on the hook. Quick fix candidates: CR-01, WR-01 to WR-03 (one commit in `ask-governed-merge.sh` and its test file), WR-06 (two record paragraphs), WR-05 (a version pattern check).

IN-06, IN-07 and IN-08 (open) are edge cases and none touches a success criterion. WR-01 of the earlier review (conflict retry bound without jitter) stays deferred to Phase 3, whose success criterion 2 exercises it.

### Human Verification Required

See the `human_verification` frontmatter and `02-UAT.md` (6 items, all pending). All six still need a human:

1. **First GitHub run of the new CI jobs.** Needs a GitHub runner and a pull request.
2. **`/plugin` installation of the two skill plugins.** A Claude Code runtime action.
3. **NOTICE and quick-start wording about the Axoniq terms.** Legal and terms judgment.
4. **ADR-0001 and ADR-0002 re-written Considered Options.** Semantic fidelity.
5. **Unreported upstream drafts (UP-0006, UP-0010, UP-0011, UP-0013).** Author approval gate.
6. **Docker Desktop run.** A different engine from the one used here.

### Gaps Summary

No gaps. All five roadmap success criteria hold on the current HEAD. The conflict test, the compose stack, `doctor`, the governance gates, the six shell suites and the full build pass, and the five quick tasks introduced no regression to any Phase 2 deliverable. What remains is confirmation that only a GitHub runner, Claude Code, Docker Desktop or the author can give, plus advisory review findings about the ADR-0015 hooks, `doctor` and two upstream records.

---

_Verified: 2026-11-24T07:40:00Z_
_Verifier: Claude (gsd-verifier)_
