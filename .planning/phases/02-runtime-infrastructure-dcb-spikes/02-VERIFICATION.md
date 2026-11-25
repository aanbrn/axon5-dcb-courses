---
phase: 02-runtime-infrastructure-dcb-spikes
verified: 2026-11-25T03:00:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - ".claude/settings.json"
  - ".github/workflows/ci.yml"
  - ".gitignore"
  - ".mcp.json"
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
  - "backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt"
  - "build-logic/build.gradle.kts"
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt"
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/TestSuitesFunctionalTest.kt"
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckHooksFunctionalTest.kt"
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ClaudeSettingsFixtures.kt"
  - "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt"
  - "build-logic/src/main/kotlin/courses.test-suites.gradle.kts"
  - "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt"
  - "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt"
  - "compose.yaml"
  - "compose/infra.yaml"
  - "docs/adr/0008-container-images-with-buildpacks.md"
  - "docs/adr/0010-test-levels-as-jvm-test-suites.md"
  - "docs/adr/0015-governance-hooks-for-worktrees-and-merges.md"
  - "docs/tooling.md"
  - "docs/upstream/0012-docker-containerd-platform-export.md"
  - "scripts/doctor.sh"
  - "scripts/governance/ask-governed-edit.sh"
  - "scripts/governance/ask-governed-merge.sh"
  - "scripts/governance/test-ask-governed-edit.sh"
  - "scripts/governance/test-ask-governed-merge.sh"
  - "scripts/test-doctor.sh"
covered_digest: "v4:sha256:c2a823f1aceaaa7042610f102b26a1f95ebb8e58e60a70ee761f1174c3be4807"
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
**Verified:** 2026-11-25T03:00:00Z
**Status:** human_needed
**Re-verification:** Yes. The previous report (human_needed, 5/5, written at d5dcb0d) went stale because covered sources changed afterwards: quick task 261125-a09 (`scripts/doctor.sh` warns again for the containerd image store below Docker 29.9.0; a `.mcp.json` server with neither `command` nor `url` fails; compose line-order functional test), quick task 261125-bha (the build-logic TestKit suite moved from the `build` CI job into a parallel `functional-test` job that `ci-success` needs; ADR-0010 and `docs/tooling.md` updated) and a `/.build/` entry in `.gitignore`. The previous report listed no `gaps:`, so there was no gap closure to check. I re-ran the behavioural evidence on HEAD (7d72267, working tree clean) rather than reusing the old results, and read the diff `d5dcb0d..HEAD` outside `.planning/` (9 files, 250 insertions, 36 deletions) for regressions.

All five roadmap success criteria hold on HEAD. The status stays `human_needed` because five items cannot be proven from the repository (`02-UAT.md`, items 2 to 6). Item 1 of the UAT file (a pull request CI run on a GitHub runner) is evidenced: PR #21 at head 07fb5d1 has every check green, including the new `Functional tests (build-logic)` job, so it is not carried as a human item. The fresh review (`02-REVIEW.md`: 0 critical, 0 warning, 3 info) is advisory and is recorded open in `02-REVIEW-DISPOSITION.md`.

Note on mode: ROADMAP.md marks Phase 2 `Mode: mvp`, but the goal is not written as "As a ..., I want to ..., so that ...", so the MVP guard would refuse. As in earlier reports I did not refuse: the goal is unambiguous and the five success criteria state observable outcomes. Rewriting the goal is a planning-document fix, not a code gap.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | One compose command starts pinned Axon Server and PostgreSQL; DCB context auto-created; correct hostname; images only from the catalog; `doctor` checks tools, Docker engine, Testcontainers, GSD manifest and explains fixes | VERIFIED | `bash scripts/doctor.sh` on HEAD: exit 0, `summary: 14 ok, 2 warn, 0 fail, 0 skip`. It reports `ok docker: colima reachable ... server 29.10.0`, `ok image-store: the classic image store is active on Docker 29.10.0`, `ok testcontainers: unix:///var/run/docker.sock, server 29.10.0, ryuk enabled`, `ok gsd: version 1.22.0, 858 files match the manifest, no unexpected files`, `ok mcp: all 2 servers in .mcp.json resolve`, `ok claude-tracking (54 files)`. The two warnings (gsd-defaults, platform-token) are the optional notes and carry `fix:` lines. `docker compose config --quiet` exit 0; `compose.yaml` and `compose/infra.yaml` did not change in this round. `checkComposeVersions` and `checkImageReferences` pass with `--rerun-tasks`. The `compose-smoke` job is green on PR #21. The WR-01 and IN-01 fixes are in `scripts/doctor.sh` (`CONTAINERD_STORE_MIN_DOCKER=29.9.0` with a `sort -V` comparison, warn on an older or uncomparable engine; `none` and `missing` server kinds in `check_mcp`, `report fail mcp` for a server with neither command nor url) and covered by `scripts/test-doctor.sh` (318 passed, 0 failed). |
| 2 | An integration test on Axon Server in Testcontainers shows a conflicting conditional append is rejected and a non-overlapping one succeeds; unit, component and integration run as separate Gradle suites sharing test fixtures with the standard toolchain | VERIFIED | Behaviour-dependent truth, so I ran the single named test after deleting the old result XML: `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun -q` exit 0. The fresh XML has `tests="2" skipped="0" failures="0" errors="0"` with the cases `an append with an outdated marker for overlapping criteria is rejected()` and `appends whose criteria do not overlap both commit()`. Suites come from `courses.test-suites.gradle.kts` (unchanged in this round); `testFixtures` exist in kernel and command-service (TEST-12). The orchestrator's full `./gradlew build` on HEAD (48 test classes, 359 tests, 0 failures) is taken from the orchestrator, not re-run here. |
| 3 | Contracts modules contain no Spring or framework logic; Kotlin events with TypeID value-class IDs and tags round-trip unchanged; malformed or wrong-prefix TypeIDs are rejected | VERIFIED | `:serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun-tasks` exit 0 (in the gate run below). `ContractsArchitectureTest`, `TypeIdVectorsTest`, `TypedIdTest` and the on-server serialization integration test are in the orchestrator's green build. No production source of the contracts or serialization modules changed since the previous report (the diff `d5dcb0d..HEAD` touches none of them). |
| 4 | MADR ADRs from in-repo templates record the spike outcomes (licence behaviour also in the quick start, decision-record capture, Kotlin serialization and TypeID conventions, buildpack constraints); CI checks licences against an allowlist; NOTICE justifies the Axoniq modules | VERIFIED | `docs/adr` holds 15 ADRs (ADR-0010 was updated in this round to describe the `functional-test` job), `docs/templates` the five templates. `checkAdrs checkNotice checkUpstreamRecords checkComposeVersions checkImageReferences checkClaudeAskRules checkDependencyLicences :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun-tasks -q` exit 0. The new compose line-order test in `ImageReferenceCheckFunctionalTest` passes. |
| 5 | Every upstream defect has a `docs/upstream/` record; a CI check keeps markers and records consistent; dependency PRs prompt a removal-criteria check; skills and MCP servers installed from pinned sources after approval and listed in the inventory; only `.claude/settings.json` and `.claude/CLAUDE.md` tracked | VERIFIED (repository side; see human items) | 13 records `docs/upstream/0001..0013`; `checkUpstreamRecords` passes; doctor `ok upstream-removal`; `scripts/upstream/test-changed-dependencies.sh` 62 passed, 0 failed; the `Upstream records advisory` job is green on PR #21. UP-0012 was edited in this round (8 lines) and still passes the check. Tracked content under `.claude/` is `settings.json`, `CLAUDE.md` and the `skills/spike-findings-*` tree, the documented narrow exception of ADR-0014 and D-39, which `check_claude_tracking` encodes; the literal wording of the criterion ("only" two files) is narrower than the shipped rule, which was decided and recorded in the phase, not an oversight. |

**Score:** 5/5 truths verified (0 present, behaviour-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | GH-12 clause "used by the project skills" | Phase 8 | AIDEV-02 (Phase 8) defines the project skills ("write an ADR, draft an upstream issue"); 02-19-SUMMARY states that use by skills stays with Phase 8. All five templates exist in `docs/templates/` and are used by `checkAdrs` and `checkUpstreamRecords`. |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/` | Runtime stack | VERIFIED | Compose config clean; unchanged this round; `compose-smoke` green on a GitHub runner |
| `tooling/test-support/` | Container singletons | VERIFIED | Used by the integration test re-run here |
| `build-logic/.../courses.test-suites.gradle.kts` | Suite convention, Kover, native access | VERIFIED | Unchanged this round; suites ran in this run |
| `backend/contracts/{kernel,events,commands,queries}`, `backend/serialization-jackson` | Contracts and serialization | VERIFIED | Framework-free; round-trip test green; `queries` source-less by design (D-02) |
| `backend/command-service` | Spike code for Phase 3 | VERIFIED | Integration test green |
| `tooling/doctor`, `scripts/doctor.sh`, `scripts/test-doctor.sh` | `doctor` | VERIFIED | Ran it, exit 0; `test-doctor.sh` 318 passed, 0 failed (was 269 before the two fixes added cases) |
| Governance checks in `build-logic` (`ImageReferenceCheck` etc.) | Gates | VERIFIED | Registered, in `check`, passing |
| `docs/adr/0001..0015`, `docs/templates/*`, `docs/upstream/0001..0013`, `docs/quick-start.md`, `docs/tooling.md`, `NOTICE` | Documentation | VERIFIED | Present, checks green; ADR-0010 and `docs/tooling.md` describe the job split |
| `scripts/governance/*` and their tests | Governance scripts | VERIFIED | `test-ask-governed-edit` 65, `test-ask-governed-merge` 169, `test-check-pr` 173, `test-list-pr-files` 25, all 0 failed |
| `.github/workflows/ci.yml` | CI jobs | VERIFIED | `functional-test` job added (SHA-pinned actions, `persist-credentials: false`, job-level `contents: read`, 30-minute timeout, failure artifact upload); `build` now runs `-x :build-logic:functionalTest`; `ci-success` `needs:` lists `build, functional-test, lint, secrets, qodana, integration-test, compose-smoke, doctor, upstream-records` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `gradle/libs.versions.toml` `image-*` | `compose/versions.env` | `generateComposeVersions`, `checkComposeVersions`, `checkImageReferences` | WIRED | Checks pass |
| `renovate.json5` image manager | `ImageReferenceCheck` | regex shared through the functional test | WIRED | Functional test passes |
| Version catalog images | test JVMs | container image system properties | WIRED | Integration test ran against the pinned image |
| `courses.test-suites` | every `Test` task | `tasks.withType<Test>().configureEach` | WIRED | Unchanged; pinned by `TestSuitesFunctionalTest` |
| `functional-test` job | `./gradlew :build-logic:functionalTest` | CI step; `build` job excludes it with `-x :build-logic:functionalTest` | WIRED | Partition without gap or overlap (the review ran `--dry-run` to show the task is planned in one job only); job is green on PR #21 and is in `ci-success` `needs:` |
| `functional-test` job | `ci-success` | `needs:` list | WIRED | `ci.yml` line 450; `ci-success` SUCCESS on PR #21 |
| `CommandServiceConfigurationEnhancer` | capture engine, retry bus, tag resolver, Jackson converter | Axon configuration enhancer | WIRED | Capture and serialization integration tests in the green build |
| `docs/upstream` records | code markers | `checkUpstreamRecords` | WIRED | Passes |
| `.claude/settings.json` hooks | `scripts/governance/ask-governed-{edit,merge}.sh` | `checkClaudeAskRules` | WIRED | Check and both hook suites pass |

### Data-Flow Trace (Level 4)

Not applicable: no UI or rendered dynamic data.

### Behavioural Spot-Checks

| Behaviour | Command | Result | Status |
|-----------|---------|--------|--------|
| Conflict rejected, disjoint append commits | `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun -q` | exit 0; fresh XML tests=2, failures=0, errors=0 | PASS |
| `doctor` | `bash scripts/doctor.sh` | exit 0, 14 ok, 2 warn, 0 fail, 0 skip | PASS |
| Governance gates and changed-file tests | `checkAdrs checkNotice checkUpstreamRecords checkComposeVersions checkImageReferences checkClaudeAskRules checkDependencyLicences :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun-tasks -q` | exit 0 | PASS |
| Shell suites | `test-doctor` 318, `test-ask-governed-edit` 65, `test-ask-governed-merge` 169, `test-check-pr` 173, `test-list-pr-files` 25, `test-changed-dependencies` 62, `test-apply-github-settings` 107 | 919 passed, 0 failed | PASS |
| Compose file validity | `docker compose config --quiet` | exit 0 | PASS |
| CI on a GitHub runner | `gh pr view 21 --json statusCheckRollup,headRefOid` | head 07fb5d1; Build, Functional tests (build-logic), Integration tests, Compose smoke test, Doctor, Lint, Qodana, Secret scan, Upstream records advisory, CodeQL, Analyze (actions), Governance policy, governance-success, ci-success all SUCCESS | PASS |
| Debt markers | `git grep -E '\b(TBD\|FIXME\|XXX)\b'` in backend, build-logic, scripts, compose, .github, docs, tooling | none (only `mktemp` templates, excluded) | PASS |
| Whole build | `./gradlew build` (run by the orchestrator, not by me) | green, 48 test classes, 359 tests, 0 failures | PASS (reported) |

Note: PR #21 CI ran on 07fb5d1. Commits after it on the branch (`a8e8c2e` `.gitignore` entry, and `.planning/` documents) cannot change a job outcome: the `.gitignore` line ignores a tool cache directory and the rest is under `.planning/`.

### Probe Execution

No `probe-*.sh` convention exists in this repository; the phase's probe is the compose `probe` profile service, exercised by the `compose-smoke` job (green on PR #21).

### Requirements Coverage

The union of the `requirements:` frontmatter of the 23 PLAN files is exactly the 22 phase IDs (AIDEV-01, AIDEV-03, AIDEV-04, AIDEV-07, CICD-09, DOC-03, GH-12, IDEM-04, INFRA-02..06, TEST-11, TEST-12, TEST-14, TOOL-03, TOOL-04, UPS-01..04); I extracted it from the plans and compared it with the ROADMAP Phase 2 `Requirements:` line and the REQUIREMENTS.md traceability table. Every ID appears in REQUIREMENTS.md, no plan claims an ID outside the 22, and no ID mapped to Phase 2 is unclaimed, so there is no ORPHANED requirement.

| Requirement | Status | Evidence |
|-------------|--------|----------|
| INFRA-02 | SATISFIED | Contracts modules framework-free, Konsist-enforced |
| INFRA-03 | SATISFIED | Compose config clean, images from catalog, `compose-smoke` green on a runner |
| INFRA-04 | SATISFIED | Conflict test re-run here, 2/2 |
| INFRA-05 | SATISFIED | ADR-0005, quick start, `.env.example`; doctor warns on the missing token |
| INFRA-06 | SATISFIED | Round-trip and on-server serialization suites green |
| IDEM-04 | SATISFIED for Phase 2 scope | Own codec with official vectors, value classes, boundary rejection; PostgreSQL `uuid`, API boundary and `typeid-js` are later-phase (ADR-0007) |
| TEST-11 | SATISFIED | Three JVM Test Suites, Kover |
| TEST-12 | SATISFIED | `testFixtures` in kernel and command-service |
| TEST-14 | SATISFIED (as scoped by D-21) | JUnit 6, Kotest, Testcontainers; Awaitility and MockK join with their first consumer |
| CICD-09 | SATISFIED (JVM half) | `checkDependencyLicences` and `checkNotice` pass; npm half is Phase 5, REQUIREMENTS.md keeps it Pending |
| UPS-01 | SATISFIED | 13 records with machine-readable front matter |
| UPS-02 | SATISFIED (Phase 2 part) | Markers, check, index; site index is Phase 5 |
| UPS-03 | PARTIAL, human | Five issues published after approval; four drafts (UP-0006, UP-0010, UP-0011, UP-0013) unreported pending the author (human item 4) |
| UPS-04 | SATISFIED | Advisory job (green on PR #21) and script, 62 checks |
| DOC-03 | SATISFIED | 15 ADRs validated by `checkAdrs` |
| GH-12 | SATISFIED, with a deferred clause | Five templates exist; use by skills is Phase 8 |
| TOOL-03 | SATISFIED | `doctor` exit 0; failing checks carry `fix:` lines; the image-store and mcp checks were tightened this round |
| TOOL-04 | SATISFIED on colima; Docker Desktop not exercised (human item 5) | |
| AIDEV-01 | SATISFIED (repository side) | Survey, SHA-pinned plugins, inventory |
| AIDEV-03 | SATISFIED | doctor `claude-tracking` ok, 54 files |
| AIDEV-04 | SATISFIED | `.mcp.json` pinned through mise; doctor `mcp` ok, and now fails on a server with neither command nor url |
| AIDEV-07 | SATISFIED | doctor `gsd` ok: 858 files match the manifest |

Bookkeeping: REQUIREMENTS.md marks 18 of 22 Complete. CICD-09, UPS-02, UPS-03 and TOOL-04 are Pending with a stated reason that matches this report. The ROADMAP Phase 2 checkbox is unchecked; the orchestrator updates it once verification is accepted.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `scripts/test-doctor.sh`, `docs/upstream/0012-docker-containerd-platform-export.md` | various | IN-01 (open, review of this round): review finding IDs (WR-01, IN-01) are cited from durable code and docs, and the IDs collide across incremental reviews | Info | Comments explain nothing a reader can resolve; cite the quick task or commit instead |
| `scripts/doctor.sh` | 302-312 | IN-02 (open): a `-rc` pre-release of 29.9.0 is stripped to `29.9.0` and passes the engine gate | Info | A missed warning on a pre-release engine only; doctor never fails because of it |
| `.github/workflows/ci.yml` | 129-170 | IN-03 (open): the setup block is now copied into a third job | Info | Maintainability; optional composite action |
| (none) | | No TBD/FIXME/XXX debt markers or stub patterns in phase sources | | Clean |

The previous report's warning WR-01 and its info items IN-01 and IN-03 are fixed by quick task 261125-a09 (confirmed in `scripts/doctor.sh` and by the test counts above). IN-02 of that review (UP-0012 removal rests on "no defect observed on a fixed engine", to be re-checked when CICD-02 lands) was recorded for Phase 6 by the same task. None of the three open items touches a roadmap criterion.

### Human Verification Required

See the `human_verification` frontmatter and `02-UAT.md` (items 2 to 6, still pending; item 1, the PR CI run, is evidenced above and can be marked passed):

1. **`/plugin` installation of the two skill plugins.** A Claude Code runtime action.
2. **NOTICE and quick-start wording about the Axoniq terms.** Legal and terms judgment.
3. **ADR-0001 and ADR-0002 re-written Considered Options.** Semantic fidelity.
4. **Unreported upstream drafts (UP-0006, UP-0010, UP-0011, UP-0013).** Author approval gate.
5. **Docker Desktop run.** A different engine from the one used here.

### Gaps Summary

No gaps. All five roadmap success criteria hold on the current HEAD. The conflict test, `doctor`, the governance gates, the changed-file functional tests and the seven shell suites (919 checks) pass in this run, PR #21 is fully green on a GitHub runner at 07fb5d1 including the new `functional-test` job, and the orchestrator's full build is green. Quick tasks 261125-a09 and 261125-bha fixed the earlier advisory findings and moved the TestKit suite into a parallel job that `ci-success` requires, without regressing any criterion. Three info items remain open and touch no roadmap criterion. What remains for the phase is confirmation that only Claude Code, Docker Desktop or the author can give.

---

_Verified: 2026-11-25T03:00:00Z_
_Verifier: Claude (gsd-verifier)_
