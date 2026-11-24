---
phase: 02-runtime-infrastructure-dcb-spikes
verified: 2026-11-24T09:30:00Z
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
  - "backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt"
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt"
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt"
  - "build-logic/src/main/kotlin/courses.test-suites.gradle.kts"
  - "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt"
  - "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt"
  - "compose.yaml"
  - "compose/infra.yaml"
  - "docs/adr/0015-governance-hooks-for-worktrees-and-merges.md"
  - "docs/tooling.md"
  - "scripts/doctor.sh"
  - "scripts/governance/ask-governed-edit.sh"
  - "scripts/governance/ask-governed-merge.sh"
  - "scripts/governance/test-ask-governed-edit.sh"
  - "scripts/governance/test-ask-governed-merge.sh"
  - "scripts/test-doctor.sh"
covered_digest: "v4:sha256:e5e53051248e54d98dc46d543a9e9bf11ea7b6063045a3cddf92bb838c0b2284"
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
**Verified:** 2026-11-24T09:30:00Z
**Status:** human_needed
**Re-verification:** Yes. The previous report (human_needed, 5/5, no gaps) went stale because quick tasks 261124-kh6 and 261124-nlk changed `scripts/doctor.sh`, the two governance hook scripts and their tests, `ImageReferenceCheck`, ADR-0015, `docs/tooling.md`, `CoursesJsonModule` and two upstream records. The previous report listed no `gaps:`, so there was no gap closure to check. I re-checked every success criterion against the current HEAD (e79ef1b), re-ran the behavioral evidence myself instead of reusing earlier results, and looked for regressions from the two quick tasks.

All five roadmap success criteria hold on HEAD. The status stays `human_needed` because six items cannot be proven from the repository (`02-UAT.md`, all pending). The incremental review (`02-REVIEW.md`: 0 critical, 3 warnings, 1 info, all open in `02-REVIEW-DISPOSITION.md`) is advisory. I confirmed WR-01 independently: it is a real regression in `doctor` in an edge case (see Anti-Patterns), but it does not defeat any Phase 2 criterion.

Note on mode: ROADMAP.md marks Phase 2 `Mode: mvp`, but the goal is not written as "As a ..., I want to ..., so that ...", so the MVP guard would refuse. I did not refuse, for the reason the Phase 1 report and the earlier Phase 2 report gave: the goal is unambiguous and the five success criteria state the observable outcomes. Rewriting the goal is a planning-document fix, not a code gap.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | One compose command starts pinned Axon Server and PostgreSQL; DCB context auto-created; correct hostname; images only from the catalog; `doctor` checks tools, Docker engine, Testcontainers, GSD manifest and explains fixes | VERIFIED | I ran `POSTGRES_PORT=55433 docker compose -p verify02d up -d --wait` in this session: both containers Healthy. `docker compose --profile probe run --rm probe` printed "http://axonserver:8024/actuator/health is reachable" and "axonserver:8124 accepts connections". `pg_database` listed `query_service` and `automation_service`. I tore the stack down with `down -v`; the working tree stayed clean. `checkComposeVersions` and `checkImageReferences` pass (image refs come from the catalog through `compose/versions.env`). `bash scripts/doctor.sh` exits 0 with `summary: 13 ok, 3 warn, 0 fail, 0 skip` (the three warns are the optional gsd-defaults, platform-token and upstream-removal notes); it covers mise, tools, java, docker (colima), image store, colima DNS, testcontainers config, gsd manifest (858 files match), mcp, claude-code, git, claude-tracking, and prints `ok testcontainers: ... ryuk enabled`. `scripts/test-doctor.sh`: 155 passed, 0 failed. One edge case of the new `git` check is defective (WR-01, advisory). |
| 2 | An integration test on Axon Server in Testcontainers shows a conflicting conditional append is rejected and a non-overlapping one succeeds; unit, component and integration run as separate Gradle suites sharing test fixtures with the standard toolchain | VERIFIED | Behavior-dependent truth, so I ran the single named test: `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun` on HEAD, BUILD SUCCESSFUL in 21s, result XML `tests="2" skipped="0" failures="0" errors="0"`. The suites come from `courses.test-suites.gradle.kts` (`componentTest`, `integrationTest`), with `testFixtures` in the kernel and command-service (TEST-12). `mise x -- ./gradlew build -q` exits 0 on HEAD (the orchestrator's earlier uncached run: 48 test classes, 347 tests, 0 failures). |
| 3 | Contracts modules contain no Spring or framework logic; Kotlin events with TypeID value-class IDs and tags round-trip unchanged; malformed or wrong-prefix TypeIDs are rejected | VERIFIED | `ContractsArchitectureTest` (Konsist), `TypeIdVectorsTest`, `TypedIdTest`, `JsonRoundTripTest` and the on-server `AxonServerSerializationIntegrationTest` are part of the green full build. The only phase change in this area since the last report is a KDoc paragraph in `CoursesJsonModule.kt` (see WR-02, a documentation inaccuracy, no behavior change). |
| 4 | MADR ADRs from in-repo templates record the spike outcomes (licence behaviour also in the quick start, decision-record capture, Kotlin serialization and TypeID conventions, buildpack constraints); CI checks licences against an allowlist; NOTICE justifies the Axoniq modules | VERIFIED | `docs/adr/0001..0015` present; `checkAdrs`, `checkNotice`, `checkUpstreamRecords`, `checkComposeVersions`, `checkImageReferences`, `checkClaudeAskRules` and `checkDependencyLicences` all BUILD SUCCESSFUL on HEAD in this session (ADR-0015 and `ImageReferenceCheck` were among the changed files, and the checks are green over them). |
| 5 | Every upstream defect has a `docs/upstream/` record; a CI check keeps markers and records consistent; dependency PRs prompt a removal-criteria check; skills and MCP servers installed from pinned sources after approval and listed in the inventory; only `.claude/settings.json` and `.claude/CLAUDE.md` tracked | VERIFIED (repository side; see human items) | `checkUpstreamRecords` passes over the edited UP-0001 and UP-0004. `scripts/upstream/test-changed-dependencies.sh`: 62 passed, 0 failed. doctor reports `ok mcp: all 2 servers ... resolve`, `ok claude-code: 2.1.331 meets the minimum 2.1.324 for marketplaces pinned by commit SHA`, `ok claude-tracking: only project-owned files are tracked under .claude (54 files)` (`settings.json`, `CLAUDE.md` and the documented `skills/spike-findings-*` exception, ADR-0014). |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | GH-12 clause "used by the project skills" | Phase 8 | AIDEV-02 (Phase 8) defines the project skills ("write an ADR, draft an upstream issue"); 02-19-SUMMARY states that use by skills stays with Phase 8 |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/` | Runtime stack | VERIFIED | Started healthy and probed in this session |
| `tooling/test-support/` (`AxonServerEnvironment`, `PostgresEnvironment`, `ContainerImages`) | Container singletons | VERIFIED | Used by the integration test I re-ran |
| `build-logic/.../courses.test-suites.gradle.kts` | Suite convention and Kover | VERIFIED | Applied by every module; suites ran in the full build |
| `backend/contracts/{kernel,events,commands,queries}`, `backend/serialization-jackson` | Contracts and serialization | VERIFIED | Framework-free; `queries` is source-less by design (D-02) |
| `backend/command-service` (capture engine, retry, tag resolver, fixtures) | Spike code for Phase 3 | VERIFIED | Tests green |
| `tooling/architecture` (Konsist) | Architecture rules | VERIFIED | Suites green |
| `tooling/doctor`, `scripts/doctor.sh`, `scripts/test-doctor.sh` | `doctor` | VERIFIED with one known edge-case defect (WR-01) | Ran it, exit 0; 155 test checks passed |
| Governance checks in `build-logic` (`ImageReferenceCheck` etc.) | Gates | VERIFIED with a known gap (WR-03) | Registered, in `check`, passing |
| `docs/adr/0001..0015`, `docs/templates/*`, `docs/upstream/0001..0013`, `docs/quick-start.md`, `docs/tooling.md`, `NOTICE` | Documentation | VERIFIED | Present, checks green |
| `scripts/governance/*` and their tests | Governance scripts | VERIFIED | `test-check-pr.sh` 173, `test-list-pr-files.sh` 25, `test-ask-governed-edit.sh` 62, `test-ask-governed-merge.sh` 166 passed, 0 failed |
| `.github/workflows/ci.yml` | CI jobs | VERIFIED (wiring); never run on GitHub | See human item 1 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `gradle/libs.versions.toml` `image-*` | `compose/versions.env` | `generateComposeVersions`, `checkComposeVersions`, `checkImageReferences` | WIRED | Checks pass; the stack started from those values |
| Version catalog images | test JVMs | container image system properties | WIRED | The integration test ran against the pinned image |
| `CommandServiceConfigurationEnhancer` | `DecisionCaptureEngine`, `RetryingCommandBus`, `KotlinTagResolver`, Jackson converter | Axon configuration enhancer | WIRED | Capture and serialization integration tests green |
| `docs/upstream` records | code markers | `checkUpstreamRecords` | WIRED | Passes (UP-0012 removal-criteria warning shown by doctor is informational) |
| `.claude/settings.json` hooks | `scripts/governance/ask-governed-{edit,merge}.sh` | `checkClaudeAskRules` | WIRED | Check passes; the hooks' own suites pass |
| `compose-smoke` job | compose stack and conflict test | `-Pcourses.axonserver.external=localhost:8124` | WIRED | Same start, probe and test run locally |
| All new CI jobs | `ci-success` | `needs: [build, lint, secrets, qodana, integration-test, compose-smoke, doctor, upstream-records]` | WIRED | In `ci.yml` |

### Data-Flow Trace (Level 4)

Not applicable: no UI or rendered dynamic data. The equivalent flow, decision capture to recorded decision, is asserted against real records in `DecisionCaptureIntegrationTest`.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Conflict rejected, disjoint append commits | `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun` | BUILD SUCCESSFUL; tests=2, failures=0, errors=0 | PASS |
| Compose stack starts healthy, no `.env` | `POSTGRES_PORT=55433 docker compose -p verify02d up -d --wait` | both Healthy | PASS |
| In-network scenario | `docker compose --profile probe run --rm probe` | health URL and gRPC port reachable | PASS |
| Service databases created | `psql -Atc "select datname from pg_database"` | `query_service`, `automation_service` present | PASS |
| `doctor` | `bash scripts/doctor.sh` | exit 0, 13 ok, 3 warn, 0 fail | PASS |
| Governance gates | `checkAdrs checkUpstreamRecords checkNotice checkComposeVersions checkImageReferences checkClaudeAskRules checkDependencyLicences` | BUILD SUCCESSFUL | PASS |
| Shell suites | `test-doctor` 155, `test-check-pr` 173, `test-list-pr-files` 25, `test-ask-governed-edit` 62, `test-ask-governed-merge` 166, `test-changed-dependencies` 62, `test-apply-github-settings` 107 | 750 passed, 0 failed | PASS |
| Whole build | `mise x -- ./gradlew build -q` | exit 0 | PASS |
| WR-01 replay | `set -euo pipefail` with a failing producer inside `$(... \| awk)` | shell exits 1 before the next statement | CONFIRMED (defect) |
| Debt markers | `git grep` for TBD, FIXME, XXX in backend, build-logic, scripts, compose, .github, docs, tooling | none | PASS |

A plain `./gradlew build` outside `mise` fails in `verifyShfmt`, `verifyTaplo` and `verifyNodeVersion` because the shell has no mise activation. That is the documented behavior of those guards, not a defect.

### Probe Execution

No `probe-*.sh` convention exists in this repository. The phase's probe is the compose `probe` profile service, run above (PASS).

### Requirements Coverage

All 22 phase IDs appear in the `requirements:` frontmatter of at least one PLAN (extracted from the PLAN files, 22 distinct IDs) and in REQUIREMENTS.md. No ORPHANED requirement: the traceability table maps no other ID to Phase 2.

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| INFRA-02 | 02-01, 02-03, 02-06, 02-11 | SATISFIED | Contracts modules framework-free, enforced by Konsist |
| INFRA-03 | 02-10, 02-12 | SATISFIED | Compose started and probed this session |
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
| TOOL-03 | 02-14 | SATISFIED (edge-case defect WR-01 noted) | `doctor` ran, exit 0, every failing check carries a `fix:` line on the normal path |
| TOOL-04 | 02-07, 02-08, 02-14 | SATISFIED on colima; Docker Desktop not exercised (human item 6) | |
| AIDEV-01 | 02-05, 02-21 | SATISFIED (repository side) | Survey, SHA-pinned plugins, inventory |
| AIDEV-03 | 02-05, 02-14 | SATISFIED | doctor `claude-tracking` ok, 54 project-owned files |
| AIDEV-04 | 02-02, 02-14 | SATISFIED | `.mcp.json` pinned through mise; doctor `mcp` ok |
| AIDEV-07 | 02-14 | SATISFIED | doctor `gsd` check passes against the manifest (858 files) |

Bookkeeping: REQUIREMENTS.md shows Complete for 18 of the 22 IDs. CICD-09, UPS-02, UPS-03 and TOOL-04 are Pending with a stated reason, which matches this report. The ROADMAP Phase 2 checkbox is still unchecked; the orchestrator updates it once verification is accepted.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `scripts/doctor.sh` | 586-594 | WR-01 (regression from quick task nlk): `set -euo pipefail` is active (line 26) and `installed=$(git --version 2>/dev/null \| awk ...)` has no `\|\| installed=''`. If `git --version` exits non-zero (the macOS `/usr/bin/git` shim without the Command Line Tools), `pipefail` plus `set -e` ends the script with status 1 before any `fail git:` line, `fix:` line or summary is printed. I read the code and replayed the pattern in a shell: the script exits before the next statement. | Warning (advisory) | `doctor` is silent in one setup where it should help (fresh macOS without developer tools). Not on the normal path: doctor exits 0 here. The existing test only covers a `git` that exits 0 with unparsable output |
| `backend/serialization-jackson/.../CoursesJsonModule.kt` | 27-30 | WR-02: new KDoc says an ID-keyed map "would be written with the ID's `toString()` as the key" and has no key deserializer. The reviewer ran the module and found the Kotlin module unboxes the value class, so keys are written and read as raw UUIDs, which round-trip; the suggested remedy (key deserializer parsing TypeIDs only) would break the round trip | Warning (documentation) | Misleading guidance for the first contract that uses an ID-keyed map. No behavior change in this phase |
| `build-logic/.../governance/ImageReferenceCheck.kt` | 27-30, 148, 158 | WR-03: the check accepts catalog entries the Renovate manager (`renovate.json5:83`, `\bimage-[a-z0-9-]+ = "..."`, `depName` without `:`) never matches (quoted key, uppercase or `_` key, non-single-space `=`, registry with a port); the KDoc overstates parity | Warning (advisory) | A hand-written entry in such a shape would pass CI and never be updated by Renovate. Pre-existing gap. The current catalog is clean |
| `scripts/governance/ask-governed-merge.sh` | 898-910 | IN-01: `bash $SCRIPT; git merge ...` asks with the reason "cannot tell where the options of bash end" | Info | Misleading prompt text, fail-closed |
| (none) | | No debt markers or stub patterns in phase sources | | Clean |

The previous report's critical finding (CR-01, `bash -euo pipefail -c` hiding a merge from the hook) and WR-02..WR-09 are recorded as `fixed` in `02-REVIEW-DISPOSITION.md`. The incremental review tried about 50 commands against the new hook and found no bypass; the 166-check merge-hook suite passes. I did not re-derive that review, I confirmed only WR-01 and WR-03's regex quotation.

Suggested quick fix before the pull request merges: WR-01 (`|| installed=''` plus a test where `git --version` exits 1), WR-02 (correct the KDoc paragraph). WR-03 and IN-01 can wait for the follow-up review pass.

### Human Verification Required

See the `human_verification` frontmatter and `02-UAT.md` (6 items, all pending). All six still need a human:

1. **First GitHub run of the new CI jobs.** Needs a GitHub runner and a pull request.
2. **`/plugin` installation of the two skill plugins.** A Claude Code runtime action.
3. **NOTICE and quick-start wording about the Axoniq terms.** Legal and terms judgment.
4. **ADR-0001 and ADR-0002 re-written Considered Options.** Semantic fidelity.
5. **Unreported upstream drafts (UP-0006, UP-0010, UP-0011, UP-0013).** Author approval gate.
6. **Docker Desktop run.** A different engine from the one used here.

### Gaps Summary

No gaps. All five roadmap success criteria hold on the current HEAD. The conflict test, the compose stack, `doctor`, the governance gates, the seven shell suites (750 checks) and the full build pass, and quick tasks 261124-kh6 and 261124-nlk introduced no regression to any success criterion. One edge-case regression in `doctor` (WR-01, confirmed) and two other advisory review findings remain open. What remains for the phase is confirmation that only a GitHub runner, Claude Code, Docker Desktop or the author can give.

---

_Verified: 2026-11-24T09:30:00Z_
_Verifier: Claude (gsd-verifier)_
