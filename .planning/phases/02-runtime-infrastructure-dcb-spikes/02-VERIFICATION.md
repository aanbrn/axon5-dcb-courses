---
phase: 02-runtime-infrastructure-dcb-spikes
verified: 2026-11-25T00:30:00Z
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
  - "docs/adr/0015-governance-hooks-for-worktrees-and-merges.md"
  - "docs/tooling.md"
  - "docs/upstream/0012-docker-containerd-platform-export.md"
  - "scripts/doctor.sh"
  - "scripts/governance/ask-governed-edit.sh"
  - "scripts/governance/ask-governed-merge.sh"
  - "scripts/governance/test-ask-governed-edit.sh"
  - "scripts/governance/test-ask-governed-merge.sh"
  - "scripts/test-doctor.sh"
covered_digest: "v4:sha256:2e04acf0a167f1f33746aba8658b87e60fec7d5b131feca6acfa0071b149aaf2"
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
**Verified:** 2026-11-25T00:30:00Z
**Status:** human_needed
**Re-verification:** Yes. The previous report (human_needed, 5/5, verified at 6f2c1cc) went stale because quick task 261124-uwn (PR #21 CI fixes, review findings WR-01, IN-01 and IN-02 of the earlier review, the UP-0012 workaround removed from `scripts/doctor.sh`) and quick task 261124-wp3 (`--enable-native-access=ALL-UNNAMED` on every Test task of `courses.test-suites`) changed covered sources. The previous report listed no `gaps:`, so there was no gap closure to check. I re-ran the behavioural evidence on HEAD (d5dcb0d, clean apart from the untracked `.build/`) instead of reusing earlier results, and read the diff `6f2c1cc..HEAD` outside `.planning/` (14 files, 297 insertions, 104 deletions) for regressions.

All five roadmap success criteria hold on HEAD. The status stays `human_needed` because five items cannot be proven from the repository (`02-UAT.md`). The sixth item of the previous report, the first run of the four new CI jobs on a GitHub runner, is now evidenced: PR #21 at head `5d66ae4` has every check green (see the spot-check table), so it is removed from the human list. The fresh review (`02-REVIEW.md`: 0 critical, 1 warning, 3 info) is advisory and is recorded open in `02-REVIEW-DISPOSITION.md`.

Note on mode: ROADMAP.md marks Phase 2 `Mode: mvp`, but the goal is not written as "As a ..., I want to ..., so that ...", so the MVP guard would refuse. As in earlier reports I did not refuse: the goal is unambiguous and the five success criteria state observable outcomes. Rewriting the goal is a planning-document fix, not a code gap.

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | One compose command starts pinned Axon Server and PostgreSQL; DCB context auto-created; correct hostname; images only from the catalog; `doctor` checks tools, Docker engine, Testcontainers, GSD manifest and explains fixes | VERIFIED | `bash scripts/doctor.sh` on HEAD: exit 0, `summary: 14 ok, 2 warn, 0 fail, 0 skip`. It includes `ok testcontainers: unix:///var/run/docker.sock, server 29.10.0, ryuk enabled`, `ok claude-tracking (54 files)`, `ok upstream-removal`. The two warnings are the optional notes (gsd-defaults, platform-token) and each carries a `fix:` line. `docker compose config --quiet` exit 0; `compose.yaml` and `compose/infra.yaml` are unchanged since the stack was started and probed in the first verification, and the `compose-smoke` job passed on the GitHub runner (PR #21). `checkComposeVersions` and `checkImageReferences` pass with `--rerun-tasks`. |
| 2 | An integration test on Axon Server in Testcontainers shows a conflicting conditional append is rejected and a non-overlapping one succeeds; unit, component and integration run as separate Gradle suites sharing test fixtures with the standard toolchain | VERIFIED | Behaviour-dependent truth, so I ran the single named test after deleting the old result XML: `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun -q` exit 0. The XML written by this run has `tests="2" skipped="0" failures="0" errors="0"` with the cases `an append with an outdated marker for overlapping criteria is rejected()` and `appends whose criteria do not overlap both commit()`. Suites come from `courses.test-suites.gradle.kts`; `testFixtures` in kernel and command-service (TEST-12). The new `TestSuitesFunctionalTest` case (`every suite grants native access ...`) passes and pins the 261124-wp3 change. The orchestrator's full `./gradlew build` on `5d66ae4` (48 test classes, 358 tests, 0 failures) is taken from the orchestrator, not re-run here. |
| 3 | Contracts modules contain no Spring or framework logic; Kotlin events with TypeID value-class IDs and tags round-trip unchanged; malformed or wrong-prefix TypeIDs are rejected | VERIFIED | `:serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun-tasks` exit 0. `ContractsArchitectureTest`, `TypeIdVectorsTest`, `TypedIdTest` and the on-server serialization integration test are in the green orchestrator build. No production source of the contracts or serialization modules changed since the last report. |
| 4 | MADR ADRs from in-repo templates record the spike outcomes (licence behaviour also in the quick start, decision-record capture, Kotlin serialization and TypeID conventions, buildpack constraints); CI checks licences against an allowlist; NOTICE justifies the Axoniq modules | VERIFIED | `docs/adr` holds 15 ADRs, `docs/templates` the five templates. `checkAdrs checkNotice checkUpstreamRecords checkComposeVersions checkImageReferences checkClaudeAskRules checkDependencyLicences --rerun-tasks -q` exit 0. ADR-0008 (buildpack constraints) was updated by 261124-uwn to the UP-0012 outcome and still passes `checkAdrs`. `ImageReferenceCheckFunctionalTest` passes, including the numeric line-order test. |
| 5 | Every upstream defect has a `docs/upstream/` record; a CI check keeps markers and records consistent; dependency PRs prompt a removal-criteria check; skills and MCP servers installed from pinned sources after approval and listed in the inventory; only `.claude/settings.json` and `.claude/CLAUDE.md` tracked | VERIFIED (repository side; see human items) | 13 records `docs/upstream/0001..0013`; `checkUpstreamRecords` passes after the UP-0012 workaround markers were removed (doctor: `ok upstream-removal: no fixed-upstream record has workaround markers left`); `scripts/upstream/test-changed-dependencies.sh` 62 passed, 0 failed; the `upstream-records` job passed on PR #21. Tracked content under `.claude/` is `settings.json`, `CLAUDE.md` and the `skills/spike-findings-*` tree, the documented narrow exception of ADR-0014 and D-39, which `check_claude_tracking` encodes; this is the one place where the literal wording of the criterion ("only" two files) is narrower than the shipped rule, and it was decided and recorded in the phase, not an oversight. |

**Score:** 5/5 truths verified (0 present, behaviour-unverified)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|--------------|----------|
| 1 | GH-12 clause "used by the project skills" | Phase 8 | AIDEV-02 (Phase 8) defines the project skills ("write an ADR, draft an upstream issue"); 02-19-SUMMARY states that use by skills stays with Phase 8. All five templates exist in `docs/templates/` and are used by `checkAdrs` and `checkUpstreamRecords`. |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `compose.yaml`, `compose/infra.yaml`, `compose/versions.env`, `compose/postgres/init/` | Runtime stack | VERIFIED | Compose config clean; `compose-smoke` green on a GitHub runner |
| `tooling/test-support/` | Container singletons | VERIFIED | Used by the integration test re-run here |
| `build-logic/.../courses.test-suites.gradle.kts` | Suite convention, Kover, native access | VERIFIED | Suites ran in this run; `tasks.withType<Test>().configureEach { jvmArgs("--enable-native-access=ALL-UNNAMED") }` present, pinned by a functional test |
| `backend/contracts/{kernel,events,commands,queries}`, `backend/serialization-jackson` | Contracts and serialization | VERIFIED | Framework-free; round-trip tests green; `queries` source-less by design (D-02) |
| `backend/command-service` | Spike code for Phase 3 | VERIFIED | Integration test green |
| `tooling/doctor`, `scripts/doctor.sh`, `scripts/test-doctor.sh` | `doctor` | VERIFIED | Ran it, exit 0; `test-doctor.sh` 269 passed, 0 failed |
| Governance checks in `build-logic` (`ImageReferenceCheck` etc.) | Gates | VERIFIED | Registered, in `check`, passing |
| `docs/adr/0001..0015`, `docs/templates/*`, `docs/upstream/0001..0013`, `docs/quick-start.md`, `docs/tooling.md`, `NOTICE` | Documentation | VERIFIED | Present, checks green |
| `scripts/governance/*` and their tests | Governance scripts | VERIFIED | `test-ask-governed-edit` 65, `test-ask-governed-merge` 169, `test-check-pr` 173, `test-list-pr-files` 25, all 0 failed |
| `.github/workflows/ci.yml` | CI jobs | VERIFIED | `integration-test`, `compose-smoke`, `doctor`, `upstream-records` are in `ci-success` `needs:` and all ran green on PR #21 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `gradle/libs.versions.toml` `image-*` | `compose/versions.env` | `generateComposeVersions`, `checkComposeVersions`, `checkImageReferences` | WIRED | Checks pass |
| `renovate.json5` image manager | `ImageReferenceCheck` | regex shared through the functional test | WIRED | Functional test passes |
| Version catalog images | test JVMs | container image system properties | WIRED | Integration test ran against the pinned image |
| `courses.test-suites` | every `Test` task including `architectureTest` | `tasks.withType<Test>().configureEach` | WIRED | Functional test asserts no restricted-method warning in the three suites |
| `CommandServiceConfigurationEnhancer` | capture engine, retry bus, tag resolver, Jackson converter | Axon configuration enhancer | WIRED | Capture and serialization integration tests in the green build |
| `docs/upstream` records | code markers | `checkUpstreamRecords` | WIRED | Passes |
| `.claude/settings.json` hooks | `scripts/governance/ask-governed-{edit,merge}.sh` | `checkClaudeAskRules` | WIRED | Check and both hook suites pass |
| New CI jobs | `ci-success` | `needs:` list | WIRED | `ci.yml` line 390; `ci-success` SUCCESS on PR #21 |

### Data-Flow Trace (Level 4)

Not applicable: no UI or rendered dynamic data.

### Behavioural Spot-Checks

| Behaviour | Command | Result | Status |
|-----------|---------|--------|--------|
| Conflict rejected, disjoint append commits | `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun -q` | exit 0; fresh XML tests=2, failures=0, errors=0 | PASS |
| `doctor` | `bash scripts/doctor.sh` | exit 0, 14 ok, 2 warn, 0 fail | PASS |
| Governance gates | `checkAdrs checkNotice checkUpstreamRecords checkComposeVersions checkImageReferences checkClaudeAskRules checkDependencyLicences --rerun-tasks -q` | exit 0 | PASS |
| Changed-file tests | `:build-logic:functionalTest --tests '*TestSuitesFunctionalTest*' --tests '*ImageReferenceCheckFunctionalTest*' :serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun-tasks -q` | exit 0 | PASS |
| Shell suites | `test-doctor` 269, `test-ask-governed-edit` 65, `test-ask-governed-merge` 169, `test-check-pr` 173, `test-list-pr-files` 25, `test-changed-dependencies` 62, `test-apply-github-settings` 107 | 870 passed, 0 failed | PASS |
| Compose file validity | `docker compose config --quiet` | exit 0 | PASS |
| CI on a GitHub runner | `gh pr view 21 --json statusCheckRollup,headRefOid` | head `5d66ae4`; Build, Integration tests, Compose smoke test, Doctor, Lint, Qodana, Secret scan, Upstream records advisory, CodeQL, Analyze (actions), Governance policy, governance-success, ci-success all SUCCESS | PASS |
| Debt markers | `git grep -E '\b(TBD\|FIXME\|XXX)\b'` in backend, build-logic, scripts, compose, .github, docs, tooling | none (only `mktemp` templates, excluded) | PASS |
| Whole build | `./gradlew build` (run by the orchestrator, not by me) | green, 48 test classes, 358 tests, 0 failures | PASS (reported) |

### Probe Execution

No `probe-*.sh` convention exists in this repository; the phase's probe is the compose `probe` profile service, exercised by the `compose-smoke` job (green on PR #21).

### Requirements Coverage

The union of the `requirements:` frontmatter of the 23 PLAN files is exactly the 22 phase IDs (TEST-11, TEST-12, TEST-14, IDEM-04, INFRA-02..06, AIDEV-01/03/04/07, CICD-09, TOOL-03/04, UPS-01..04, GH-12, DOC-03), and each appears in REQUIREMENTS.md. The traceability table maps exactly these 22 IDs to Phase 2, so there is no ORPHANED requirement and no unclaimed ID.

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
| TOOL-03 | SATISFIED | `doctor` exit 0; failing checks carry `fix:` lines |
| TOOL-04 | SATISFIED on colima; Docker Desktop not exercised (human item 5) | |
| AIDEV-01 | SATISFIED (repository side) | Survey, SHA-pinned plugins, inventory |
| AIDEV-03 | SATISFIED | doctor `claude-tracking` ok, 54 files |
| AIDEV-04 | SATISFIED | `.mcp.json` pinned through mise; doctor `mcp` reports no warning |
| AIDEV-07 | SATISFIED | doctor `gsd` reports no fail line against the manifest |

Bookkeeping: REQUIREMENTS.md marks 18 of 22 Complete. CICD-09, UPS-02, UPS-03 and TOOL-04 are Pending with a stated reason that matches this report. The ROADMAP Phase 2 checkbox is unchecked; the orchestrator updates it once verification is accepted.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `scripts/doctor.sh` | 242-243 | WR-01 (open): `check_image_store` reports `ok` for the containerd image store on any engine; the "needs 29.9.0 or newer" limit is only in the message text. An engine older than 29.9.0 with the containerd store, which UP-0012 and ADR-0008 still document as failing to export the Paketo run image, no longer gets a `warn` | Warning (advisory) | A developer on such an engine gets a green `doctor` and fails at the first image build, which is Phase 6 work (CICD-02 has no image job yet). No Phase 2 criterion depends on it: SC1 requires `doctor` to check the Docker engine and Testcontainers connectivity, which it does. The fix is small (keep the server version, compare with 29.9.0) |
| `scripts/doctor.sh` | 503-509 | IN-01 (open): a server without a usable `command` counts as resolved in `check_mcp` | Info | Broken stdio server reported `ok`; the committed `.mcp.json` is fine |
| `docs/upstream/0012-docker-containerd-platform-export.md` | 74-99 | IN-02 (open): removal rests on "no defect observed on a fixed engine"; the record's image-build verification was not run, and nothing re-checks it when CICD-02 lands | Info | A decision of the author, written down; a CICD-02 acceptance item would keep it visible |
| `ImageReferenceCheckFunctionalTest.kt` | 278-300 | IN-03 (open): the numeric-order test covers catalog findings only, not compose findings | Info | Test precision |
| (none) | | No TBD/FIXME/XXX debt markers or stub patterns in phase sources | | Clean |

Findings CR-01 and WR-02..WR-10 of earlier reviews are recorded `fixed` in `02-REVIEW-DISPOSITION.md`. The findings of the review before this one (doctor GSD manifest read, TypeID-key test precision, Renovate catalog parity, url-only MCP server, unreadable `mise.toml` wording, line ordering) were re-checked in the `6f2c1cc..HEAD` diff and are fixed.

### Human Verification Required

See the `human_verification` frontmatter and `02-UAT.md` (items 2 to 6 of the file, still pending; item 1, the PR CI run, is evidenced above and can be marked passed):

1. **`/plugin` installation of the two skill plugins.** A Claude Code runtime action.
2. **NOTICE and quick-start wording about the Axoniq terms.** Legal and terms judgment.
3. **ADR-0001 and ADR-0002 re-written Considered Options.** Semantic fidelity.
4. **Unreported upstream drafts (UP-0006, UP-0010, UP-0011, UP-0013).** Author approval gate.
5. **Docker Desktop run.** A different engine from the one used here.

### Gaps Summary

No gaps. All five roadmap success criteria hold on the current HEAD. The conflict test, `doctor`, the governance gates, the changed-file functional tests, the seven shell suites (870 checks) pass in this run, PR #21 is fully green on a GitHub runner at `5d66ae4`, and the orchestrator's full build is green. Quick tasks 261124-uwn and 261124-wp3 fixed the earlier advisory findings and the native-access warning without regressing any criterion. One advisory warning (doctor stays silent on a containerd image store with an engine older than 29.9.0) and three info items remain open and touch no roadmap criterion. What remains for the phase is confirmation that only Claude Code, Docker Desktop or the author can give.

---

_Verified: 2026-11-25T00:30:00Z_
_Verifier: Claude (gsd-verifier)_
