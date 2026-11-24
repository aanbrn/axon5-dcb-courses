---
phase: 02-runtime-infrastructure-dcb-spikes
verified: 2026-11-24T20:15:00Z
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
  - "backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt"
  - "build-logic/build.gradle.kts"
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
covered_digest: "v4:sha256:20ff6ecd936cef56a56e8df252c0c66b261c9d5725553f039f0f09bf4368aca3"
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
**Verified:** 2026-11-24T20:15:00Z
**Status:** human_needed
**Re-verification:** Yes. The previous report (human_needed, 5/5, no gaps, verified at 6d81294) went stale because quick task 261124-oyk changed `scripts/doctor.sh`, `scripts/test-doctor.sh`, the merge hook and its test, `ImageReferenceCheck` and its functional test, `build-logic/build.gradle.kts`, the `CoursesJsonModule` KDoc, `JsonRoundTripTest` and ADR-0015. The previous report listed no `gaps:`, so there was no gap closure to check. I re-ran the behavioural evidence myself on HEAD (8099a35, clean tree) instead of reusing earlier results, and read the diff `6d81294..HEAD` for regressions.

All five roadmap success criteria hold on HEAD. The status stays `human_needed` because six items cannot be proven from the repository (`02-UAT.md`, all pending). The three warnings and one info of the previous code review (WR-01 doctor `git --version`, WR-02 KDoc, WR-03 Renovate parity, IN-01 hook prompt text) are fixed by 261124-oyk; I confirmed each in the diff. The incremental review `97724dc` (0 critical, 1 warning, 2 info) is advisory.

Note on mode: ROADMAP.md marks Phase 2 `Mode: mvp`, but the goal is not written as "As a ..., I want to ..., so that ...", so the MVP guard would refuse. As in the earlier Phase 1 and Phase 2 reports I did not refuse: the goal is unambiguous and the five success criteria state observable outcomes. Rewriting the goal is a planning-document fix, not a code gap.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | One compose command starts pinned Axon Server and PostgreSQL; DCB context auto-created; correct hostname; images only from the catalog; `doctor` checks tools, Docker engine, Testcontainers, GSD manifest and explains fixes | VERIFIED | `bash scripts/doctor.sh` on HEAD: exit 0, `summary: 13 ok, 3 warn, 0 fail, 0 skip`. It covers mise, tools (19 pinned), java, docker (colima), image-store, colima DNS, testcontainers-config, gsd (858 files match), mcp, claude-code, git, claude-tracking, the Gradle-side `testcontainers` probe (`ryuk enabled`). The three warnings are the optional gsd-defaults, platform-token and upstream-removal notes, each with a `fix:` line. `docker compose config --quiet` is clean. `compose.yaml` and `compose/infra.yaml` are unchanged since the last report, in which I started the stack, ran the probe service and listed both service databases; `checkComposeVersions` and `checkImageReferences` pass on HEAD (this run). |
| 2 | An integration test on Axon Server in Testcontainers shows a conflicting conditional append is rejected and a non-overlapping one succeeds; unit, component and integration run as separate Gradle suites sharing test fixtures with the standard toolchain | VERIFIED | Behaviour-dependent truth, so I ran the single named test: `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun -q` exit 0, result XML `tests="2" skipped="0" failures="0" errors="0"`. Suites come from `courses.test-suites.gradle.kts`, `testFixtures` in the kernel and command-service (TEST-12). The orchestrator's full uncached `./gradlew build` on this HEAD: BUILD SUCCESSFUL, 48 test classes, 354 tests, 0 failures (taken from the orchestrator, not re-run here). |
| 3 | Contracts modules contain no Spring or framework logic; Kotlin events with TypeID value-class IDs and tags round-trip unchanged; malformed or wrong-prefix TypeIDs are rejected | VERIFIED | `:serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun-tasks` passes, including the new test that pins the ID-key behaviour the corrected KDoc now describes. `ContractsArchitectureTest`, `TypeIdVectorsTest`, `TypedIdTest` and the on-server serialization integration test are in the green full build. The only production-source change in this area since the last report is the KDoc paragraph in `CoursesJsonModule.kt` (no behaviour change). |
| 4 | MADR ADRs from in-repo templates record the spike outcomes (licence behaviour also in the quick start, decision-record capture, Kotlin serialization and TypeID conventions, buildpack constraints); CI checks licences against an allowlist; NOTICE justifies the Axoniq modules | VERIFIED | `docs/adr` holds 15 ADRs; `checkAdrs checkNotice checkUpstreamRecords checkComposeVersions checkImageReferences checkClaudeAskRules checkDependencyLicences` all pass on HEAD with `--rerun-tasks` (exit 0). ADR-0015, one of the changed files, passes `checkAdrs`. `:build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*'` passes, and it now matches its samples against the regex read from `renovate.json5` (the input added in `build-logic/build.gradle.kts`). |
| 5 | Every upstream defect has a `docs/upstream/` record; a CI check keeps markers and records consistent; dependency PRs prompt a removal-criteria check; skills and MCP servers installed from pinned sources after approval and listed in the inventory; only `.claude/settings.json` and `.claude/CLAUDE.md` tracked | VERIFIED (repository side; see human items) | 13 records `docs/upstream/0001..0013`; `checkUpstreamRecords` passes; `test-changed-dependencies.sh` 62 passed, 0 failed; doctor: `ok mcp: all 2 servers resolve`, `ok claude-code: 2.1.331 meets the minimum 2.1.324 for marketplaces pinned by commit SHA`, `ok claude-tracking: only project-owned files are tracked under .claude (54 files)`. |

**Score:** 5/5 truths verified (0 present, behaviour-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | GH-12 clause "used by the project skills" | Phase 8 | AIDEV-02 (Phase 8) defines the project skills ("write an ADR, draft an upstream issue"); 02-19-SUMMARY states that use by skills stays with Phase 8. All five templates exist in `docs/templates/` and are used by `checkAdrs` and `checkUpstreamRecords`. |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/` | Runtime stack | VERIFIED | Unchanged since the stack was started and probed; `docker compose config` clean |
| `tooling/test-support/` (Axon Server and PostgreSQL environments, container images) | Container singletons | VERIFIED | Used by the integration test re-run here |
| `build-logic/.../courses.test-suites.gradle.kts` | Suite convention and Kover | VERIFIED | Suites ran in the build |
| `backend/contracts/{kernel,events,commands,queries}`, `backend/serialization-jackson` | Contracts and serialization | VERIFIED | Framework-free, round-trip tests green; `queries` source-less by design (D-02) |
| `backend/command-service` (capture engine, retry, tag resolver, fixtures) | Spike code for Phase 3 | VERIFIED | Integration test green |
| `tooling/doctor`, `scripts/doctor.sh`, `scripts/test-doctor.sh` | `doctor` | VERIFIED | Ran it, exit 0; `test-doctor.sh` 167 passed, 0 failed; the three `--version` pipelines now end in `\|\| installed=''`, so a failing `mise`, `claude` or `git` reaches a `fail`/`warn` line with a `fix:` |
| Governance checks in `build-logic` (`ImageReferenceCheck` etc.) | Gates | VERIFIED | Registered, in `check`, passing; Renovate key, separator and registry-port rules now enforced and tested |
| `docs/adr/0001..0015`, `docs/templates/*`, `docs/upstream/0001..0013`, `docs/quick-start.md`, `docs/tooling.md`, `NOTICE` | Documentation | VERIFIED | Present, checks green |
| `scripts/governance/*` and their tests | Governance scripts | VERIFIED | `test-check-pr` 173, `test-list-pr-files` 25, `test-ask-governed-edit` 62, `test-ask-governed-merge` 169, all 0 failed |
| `.github/workflows/ci.yml` | CI jobs | VERIFIED (wiring); never run on GitHub | See human item 1 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `gradle/libs.versions.toml` `image-*` | `compose/versions.env` | `generateComposeVersions`, `checkComposeVersions`, `checkImageReferences` | WIRED | Checks pass |
| `renovate.json5` image manager | `ImageReferenceCheck` | regex shared through the functional test (new `repoFiles` input) | WIRED | Functional test passes |
| Version catalog images | test JVMs | container image system properties | WIRED | Integration test ran against the pinned image |
| `CommandServiceConfigurationEnhancer` | capture engine, retry bus, tag resolver, Jackson converter | Axon configuration enhancer | WIRED | Capture and serialization integration tests in the green build |
| `docs/upstream` records | code markers | `checkUpstreamRecords` | WIRED | Passes (UP-0012 removal warning in doctor is informational) |
| `.claude/settings.json` hooks | `scripts/governance/ask-governed-{edit,merge}.sh` | `checkClaudeAskRules` | WIRED | Check and both hook suites pass |
| New CI jobs | `ci-success` | `needs:` list | WIRED | In `ci.yml` |

### Data-Flow Trace (Level 4)

Not applicable: no UI or rendered dynamic data.

### Behavioural Spot-Checks

| Behaviour | Command | Result | Status |
|-----------|---------|--------|--------|
| Conflict rejected, disjoint append commits | `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun -q` | exit 0; tests=2, failures=0, errors=0 | PASS |
| `doctor` | `bash scripts/doctor.sh` | exit 0, 13 ok, 3 warn, 0 fail | PASS |
| Governance gates and changed-file tests | `checkAdrs checkUpstreamRecords checkNotice checkComposeVersions checkImageReferences checkClaudeAskRules checkDependencyLicences :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun-tasks -q` | exit 0 | PASS |
| Shell suites | `test-doctor` 167, `test-check-pr` 173, `test-list-pr-files` 25, `test-ask-governed-edit` 62, `test-ask-governed-merge` 169, `test-changed-dependencies` 62, `test-apply-github-settings` 107 | 765 passed, 0 failed | PASS |
| Compose file validity | `docker compose config --quiet` | exit 0 | PASS |
| Debt markers | `git grep` for TBD, FIXME, XXX in backend, build-logic, scripts, compose, .github, docs, tooling | none | PASS |
| Whole build | `./gradlew build` (run by the orchestrator, not by me) | BUILD SUCCESSFUL, 354 tests | PASS (reported) |

### Probe Execution

No `probe-*.sh` convention exists in this repository; the phase's probe is the compose `probe` profile service (exercised in the previous report; compose files unchanged since).

### Requirements Coverage

All 22 phase IDs appear in the `requirements:` frontmatter of at least one PLAN (extracted from the PLAN files) and in REQUIREMENTS.md. The traceability table maps exactly these 22 IDs to Phase 2, so there is no ORPHANED requirement.

| Requirement | Status | Evidence |
|-------------|--------|----------|
| INFRA-02 | SATISFIED | Contracts modules framework-free, Konsist-enforced |
| INFRA-03 | SATISFIED | Compose config clean, images from catalog, stack verified previously on unchanged files |
| INFRA-04 | SATISFIED | Conflict test green, re-run here |
| INFRA-05 | SATISFIED | ADR-0005, quick start, `.env.example`; doctor warns on the missing token |
| INFRA-06 | SATISFIED | Round-trip and on-server serialization suites green |
| IDEM-04 | SATISFIED for Phase 2 scope | Own codec with official vectors, value classes, boundary rejection; PostgreSQL `uuid`, API boundary and `typeid-js` are later-phase (ADR-0007) |
| TEST-11 | SATISFIED | Three JVM Test Suites, Kover |
| TEST-12 | SATISFIED | `testFixtures` in kernel and command-service |
| TEST-14 | SATISFIED (as scoped by D-21) | JUnit 6, Kotest, Testcontainers; Awaitility and MockK join with their first consumer |
| CICD-09 | SATISFIED (JVM half) | npm half is Phase 5; REQUIREMENTS.md keeps it Pending |
| UPS-01 | SATISFIED | 13 records with machine-readable front matter |
| UPS-02 | SATISFIED (Phase 2 part) | Markers, check, index; site index is Phase 5 |
| UPS-03 | PARTIAL, human | Five issues published after approval; four drafts (UP-0006, UP-0010, UP-0011, UP-0013) unreported pending the author (human item 5) |
| UPS-04 | SATISFIED | Advisory job and script, 62 checks |
| DOC-03 | SATISFIED | 15 ADRs validated by `checkAdrs` |
| GH-12 | SATISFIED, with a deferred clause | Five templates exist; use by skills is Phase 8 |
| TOOL-03 | SATISFIED | `doctor` exit 0; failing checks carry `fix:` lines, including the failing-`git` path fixed by 261124-oyk |
| TOOL-04 | SATISFIED on colima; Docker Desktop not exercised (human item 6) | |
| AIDEV-01 | SATISFIED (repository side) | Survey, SHA-pinned plugins, inventory |
| AIDEV-03 | SATISFIED | doctor `claude-tracking` ok, 54 files |
| AIDEV-04 | SATISFIED | `.mcp.json` pinned through mise; doctor `mcp` ok |
| AIDEV-07 | SATISFIED | doctor `gsd` ok against the manifest (858 files) |

Bookkeeping: REQUIREMENTS.md marks 18 of 22 Complete. CICD-09, UPS-02, UPS-03 and TOOL-04 are Pending with a stated reason that matches this report. The ROADMAP Phase 2 checkbox is still unchecked; the orchestrator updates it once verification is accepted.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `scripts/doctor.sh` | 385 | `installed=$("$jq" -r '.version // empty' "$manifest")` under `set -euo pipefail` with no `\|\| true`: a GSD manifest that is not valid JSON ends doctor silently before any `fail gsd:` line (review WR-01, still open in `02-REVIEW-DISPOSITION.md`) | Warning (advisory) | Edge case on a corrupt `gsd-file-manifest.json`; not on the normal path (doctor exits 0 here) and defeats no Phase 2 criterion. Same class as the three sites 261124-oyk fixed |
| `backend/.../JsonRoundTripTest.kt` | | IN-01 (open): the TypeID-key test asserts only `JacksonException` | Info | Test precision |
| `build-logic/.../ImageReferenceCheck.kt` | | IN-02 (open): Renovate reads catalog lines the check never inspects | Info | Residual parity gap, current catalog clean |
| (none) | | No TBD/FIXME/XXX markers or stub patterns in phase sources | | Clean |

Previous findings CR-01, WR-02..WR-10 and the 261124-kh6/nlk review items are recorded `fixed` in `02-REVIEW-DISPOSITION.md`; the three warnings and one info of the first incremental review were re-checked in the `6d81294..HEAD` diff and are fixed.

### Human Verification Required

See the `human_verification` frontmatter and `02-UAT.md` (6 items, all pending, none resolved by the code):

1. **First GitHub run of the new CI jobs.** Needs a GitHub runner and a pull request.
2. **`/plugin` installation of the two skill plugins.** A Claude Code runtime action.
3. **NOTICE and quick-start wording about the Axoniq terms.** Legal and terms judgment.
4. **ADR-0001 and ADR-0002 re-written Considered Options.** Semantic fidelity.
5. **Unreported upstream drafts (UP-0006, UP-0010, UP-0011, UP-0013).** Author approval gate.
6. **Docker Desktop run.** A different engine from the one used here.

### Gaps Summary

No gaps. All five roadmap success criteria hold on the current HEAD. The conflict test, `doctor`, the governance gates, the seven shell suites (765 checks) and the targeted Gradle checks pass in this run, and the orchestrator's full build is green. Quick task 261124-oyk fixed the earlier advisory findings without regressing any criterion. One advisory edge case (doctor.sh:385) remains open. What remains for the phase is confirmation that only a GitHub runner, Claude Code, Docker Desktop or the author can give.

---

_Verified: 2026-11-24T20:15:00Z_
_Verifier: Claude (gsd-verifier)_
