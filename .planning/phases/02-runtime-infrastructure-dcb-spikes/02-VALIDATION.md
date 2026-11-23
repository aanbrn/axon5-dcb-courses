---
phase: "2"
slug: "runtime-infrastructure-dcb-spikes"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: true) (#2117)
status: validated
nyquist_compliant: true
wave_0_complete: true
created: "2026-11-22"
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 6.1.3 (Jupiter) + Kotest assertions 6.2.7 (+ `kotest-property`); Gradle TestKit for `build-logic`; Konsist for architecture; Testcontainers 2.0.5; Kover 0.9.11 instruments every suite (TEST-11) |
| **Config file** | `build-logic/build.gradle.kts` (`functionalTest` suite, exists); `courses.test-suites` convention (Wave 0, new) |
| **Quick run command** | the task's single focused Gradle command (e.g. `./gradlew :contracts-kernel:test`, `./gradlew :build-logic:functionalTest --tests '<Name>FunctionalTest'`) |
| **Full suite command** | `./gradlew build` (runs every suite including `integrationTest`, D-19) plus `docker compose config --quiet` |
| **Estimated runtime** | quick ≤ 30 s; full ~5–10 min (integration tests start Axon Server and PostgreSQL containers) |

---

## Sampling Rate

- **After every task commit:** lefthook pre-commit hook plus the task's focused Gradle command
- **After every plan wave:** `./gradlew build` and `docker compose config --quiet`
- **Before `/gsd-verify-work`:** Full suite must be green; CI green (`build`, `integration-test`, `doctor`, `lint`, `ci-success`)
- **Max feedback latency:** 30 seconds for the per-task command

---

## Per-Task Verification Map

Filled by the planner per task; the requirement-level map is in `02-RESEARCH.md` § Validation Architecture → Phase Requirements → Test Map. Integration-suite and full-build commands exceed the 30 s per-task budget by design (D-19: `check` runs every suite); each plan also has a focused command within budget, and the SUMMARY records measured durations.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 2-01-01 | 01 | 1 | TEST-11, IDEM-04 | T-02-02, T-02-03 | Codec rejects all 21 invalid vectors | unit + dry-run | `mise x -- ./gradlew :contracts-kernel:check` and `check --dry-run` suite count | ✅ | ✅ green |
| 2-01-02 | 01 | 1 | IDEM-04, INFRA-02 | T-02-03 | Wrong-prefix and malformed IDs rejected | unit | `mise x -- ./gradlew :contracts-kernel:test --tests '*TypedIdTest*'` | ✅ | ✅ green |
| 2-02-01 | 02 | 1 | AIDEV-04 | T-02-04 | Legitimacy gate before install | checkpoint (blocking-human) | — (manual approval) | n/a | 🔒 manual (approved) |
| 2-02-02 | 02 | 1 | AIDEV-04 | T-02-05 | No secrets in `.mcp.json` | CLI | `mise x -- context7-mcp --help`; `jq` checks on `.mcp.json`; `mise lock --check` | ✅ | ✅ green |
| 2-02-03 | 02 | 1 | AIDEV-04 | T-02-07 | No automerge of MCP tools | check | `mise x -- ./gradlew checkToolingInventory`; `renovate-config-validator --strict` | ✅ | ✅ green |
| 2-03-01 | 03 | 2 | INFRA-02 | T-02-51 | Malformed rejection codes rejected | unit | `mise x -- ./gradlew :contracts-kernel:test --tests '*KernelContractTest*'` | ✅ | ✅ green |
| 2-03-02 | 03 | 2 | TEST-14 | T-02-52 | Governance boundary cases repointed, never deleted | build + shell | `bash scripts/governance/test-check-pr.sh`; `mise x -- ./gradlew build` | ✅ | ✅ green |
| 2-04-01 | 04 | 2 | TEST-12, TEST-14, IDEM-04 | T-02-08 | Property tests demonstrably run | unit (property) | `mise x -- ./gradlew :contracts-kernel:test --tests '*TypeIdPropertyTest*' --rerun` + XML test count | ✅ | ✅ green |
| 2-04-02 | 04 | 2 | TEST-11, TEST-12, TEST-14 | T-02-09 | Suites wired into check; `testFixtures` stay documented | TestKit | `mise x -- ./gradlew :build-logic:functionalTest --tests '*TestSuitesFunctionalTest*'` | ✅ | ✅ green |
| 2-04-03 | 04 | 2 | TEST-11 | T-02-53, T-02-54 | Every suite counted in coverage; no suite excluded | TestKit + report | `*TestSuitesFunctionalTest*`; `mise x -- ./gradlew :contracts-kernel:koverXmlReport` | ✅ | ✅ green |
| 2-05-01 | 05 | 2 | AIDEV-01, AIDEV-03 | — | Only allowed `.claude` files tracked | doc grep + git | area loop over `docs/tooling.md`; `git ls-files .claude` allow list | ✅ | ✅ green |
| 2-05-02 | 05 | 2 | AIDEV-01 | T-02-13 | Per-plugin approval | checkpoint (blocking-human) | — (manual approval) | n/a | 🔒 manual (approved) |
| 2-05-03 | 05 | 2 | AIDEV-01 | T-02-12 | Marketplaces pinned by 40-hex SHA | CLI + check | `jq` SHA checks on `.claude/settings.json`; `checkClaudeAskRules spotlessCheck` | ✅ | ✅ green |
| 2-06-01 | 06 | 3 | INFRA-02, INFRA-06 | T-02-10 | Malformed IDs fail deserialisation | unit | `mise x -- ./gradlew :serialization-jackson:check :contracts-events:check` | ✅ | ✅ green |
| 2-06-02 | 06 | 3 | INFRA-06 | T-02-11 | Unknown `@type` rejected | unit | `mise x -- ./gradlew :serialization-jackson:test --tests '*JsonRoundTripTest*' --rerun` | ✅ | ✅ green |
| 2-06-03 | 06 | 3 | INFRA-02 | — | Queries contracts carry Axon only as compile-only metadata | build | `mise x -- ./gradlew :contracts-queries:check`; `build` | ✅ | ✅ green |
| 2-07-01 | 07 | 4 | INFRA-04, TOOL-04 | T-02-15, T-02-17 | Digest-pinned images only; no image literals in tests | integration | `mise x -- ./gradlew :command-service:integrationTest --tests '*ConditionalAppendIntegrationTest*' --rerun` | ✅ | ✅ green |
| 2-08-01 | 08 | 5 | TEST-11, TEST-14 | T-02-55 | Image change re-runs tests | component + integration + TestKit | `mise x -- ./gradlew :tooling:test-support:check :command-service:test`; `*ContainerImageInputsFunctionalTest*` | ✅ | ✅ green |
| 2-08-02 | 08 | 5 | TEST-11, TOOL-04 | T-02-18 | Read-only CI job | lint + check | `mise x -- actionlint`; `zizmor --offline`; `checkToolingInventory spotlessCheck` | ✅ | ✅ green |
| 2-09-01 | 09 | 5 | INFRA-06, INFRA-04 | T-02-19 | Capture never alters outcomes | integration (concurrency) | `mise x -- ./gradlew :command-service:integrationTest --tests '*DecisionCaptureIntegrationTest*' --rerun` | ✅ | ✅ green |
| 2-09-02 | 09 | 5 | INFRA-06 | T-02-21 | — | unit + integration | `mise x -- ./gradlew :command-service:test --tests '*DecisionCaptureTest*' :command-service:integrationTest --tests '*AxonServerSerializationIntegrationTest*'` | ✅ | ✅ green |
| 2-10-01 | 10 | 5 | INFRA-03 | T-02-25, T-02-26 | Ports on 127.0.0.1; no `.env` passthrough | compose smoke + integration | `docker compose up -d --wait` + host-scenario `ConditionalAppendIntegrationTest` | ✅ | ✅ green |
| 2-10-02 | 10 | 5 | INFRA-03 | T-02-24 | Stale or literal images fail check | TestKit + check | `*GeneratedFileCheckFunctionalTest*`, `*ImageReferenceCheckFunctionalTest*`; `checkComposeVersions checkImageReferences` | ✅ | ✅ green |
| 2-11-01 | 11 | 6 | INFRA-02, TEST-11 | T-02-22 | Rules re-run on source change; all four contracts modules, including the source-less queries module, are in scope | architecture | `mise x -- ./gradlew :tooling:architecture:architectureTest --rerun` | ✅ | ✅ green |
| 2-11-02 | 11 | 6 | INFRA-02, TEST-11 | T-02-23 | No image literals in tests | architecture | `mise x -- ./gradlew :tooling:architecture:architectureTest --rerun` + XML count; `build` | ✅ | ✅ green |
| 2-12-01 | 12 | 6 | INFRA-03, INFRA-05 | T-02-28, T-02-29 | Token absent unless defined; never empty | compose config + probe | `docker compose --env-file … config` token proof; `docker compose --profile probe run --rm probe` | ✅ | ✅ green |
| 2-12-02 | 12 | 6 | INFRA-03 | T-02-30, T-02-31 | CI job read-only, no secret beyond the Gradle cache key; Renovate tag+digest | lint + grep | `actionlint`; `zizmor --offline`; `renovate-config-validator --strict`; secret-reference count guard (prints 0) | ✅ | ✅ green |
| 2-13-01 | 13 | 7 | CICD-09 | T-02-32 | Fail-closed licence policy | check | `mise x -- ./gradlew checkDependencyLicences`; `:build-logic:functionalTest` | ✅ | ✅ green |
| 2-13-02 | 13 | 7 | CICD-09 | T-02-33 | Exceptions mirrored in NOTICE | TestKit + check | `*DependencyLicenceCheckFunctionalTest*`; `checkNotice build` | ✅ | ✅ green |
| 2-14-01 | 14 | 8 | TOOL-03, TOOL-04 | T-02-36 | Diagnose-only; one implementation per check (D-48: delegates to the existing Gradle checks) | shell test + Gradle | `bash scripts/test-doctor.sh`; `mise x -- ./gradlew doctor` exit status and `ok  testcontainers:` line; `doctor --dry-run` delegated-task count tested `-ge 5` | ✅ | ✅ green |
| 2-14-02 | 14 | 8 | AIDEV-07, AIDEV-03, AIDEV-04 | T-02-34, T-02-35 | Never prints secrets; hashes verified | shell test | `bash scripts/test-doctor.sh`; real `scripts/doctor.sh` run, reported-check count tested `-ge 5`, no `fail  gsd:` line | ✅ | ✅ green |
| 2-14-03 | 14 | 8 | TOOL-03 | T-02-37 | Read-only CI job; `.env.example`, `NOTICE`, `governance/gsd-version` style-checked | lint + check | `actionlint`; `zizmor --offline`; `checkToolingInventory spotlessCheck`; `spotlessMiscCheck` trailing-whitespace probe per file, then `spotlessCheck` | ✅ | ✅ green |
| 2-15-01 | 15 | 8 | UPS-01, UPS-02 | T-02-38 | Fail-closed front matter; `reported` needs an upstream link and a date | TestKit + check | `*UpstreamRecordCheckFunctionalTest*`; `checkUpstreamRecords` | ✅ | ✅ green |
| 2-15-02 | 15 | 8 | UPS-01, GH-12 | — | — | check | `spotlessCheck checkUpstreamRecords`; section loop | ✅ | ✅ green |
| 2-16-01 | 16 | 9 | UPS-01, UPS-02, UPS-03 | T-02-40 | No private data in records | check | `checkUpstreamRecords` + index and marker counts | ✅ | ✅ green |
| 2-16-02 | 16 | 9 | UPS-01, UPS-02 | T-02-41 | Two-way consistency | check | `checkUpstreamRecords` index count; marker guard | ✅ | ✅ green |
| 2-17-01 | 17 | 9 | UPS-01, UPS-02, UPS-03 | T-02-56 | No private data in host records | check + shell | `checkUpstreamRecords`; `bash scripts/test-doctor.sh`; `docker compose config --quiet` | ✅ | ✅ green |
| 2-17-02 | 17 | 9 | UPS-01, UPS-02 | T-02-57 | Comment-only governed edits | check + lint + build | `checkUpstreamRecords` index count; `actionlint`; `zizmor --offline`; `build` | ✅ | ✅ green |
| 2-18-01 | 18 | 9 | UPS-04 | T-02-42 | Workflow-command escaping | shell test | `bash scripts/upstream/test-changed-dependencies.sh` | ✅ | ✅ green |
| 2-18-02 | 18 | 9 | UPS-04 | T-02-43 | Read-only advisory job | lint | `actionlint`; `zizmor --offline` | ✅ | ✅ green |
| 2-19-01 | 19 | 9 | DOC-03 | T-02-44 | Decisions unchanged by normalisation | check | `mise x -- ./gradlew checkAdrs spotlessCheck` | ✅ | ✅ green |
| 2-19-02 | 19 | 9 | DOC-03, GH-12 | T-02-45 | Fail-closed ADR metadata | TestKit | `*AdrCheckFunctionalTest*`; `build` | ✅ | ✅ green |
| 2-20-01 | 20 | 10 | CICD-09 | T-02-58, T-02-60 | Licence gate wiring and detekt rule set governed; CI fails when no module registers the gate | check + shell + lint | `checkClaudeAskRules spotlessCheck`; `bash scripts/governance/test-check-pr.sh`; `actionlint`; `zizmor --offline`; `build checkDependencyLicences --dry-run` task count; ask-rule counts (57) | ✅ | ✅ green |
| 2-20-02 | 20 | 10 | CICD-09 | — | — | check + grep | `checkToolingInventory spotlessCheck`; no version numbers in `docs/tooling.md`; `outside the licence check` grep | ✅ | ✅ green |
| 2-21-01 | 21 | 10 | AIDEV-01 | T-02-48 | Mapping approved first | checkpoint (blocking-human) | — (manual approval) | n/a | 🔒 manual (approved) |
| 2-21-02 | 21 | 10 | AIDEV-01 | T-02-48 | Config equals policy | check | `checkGsdConfigPolicy checkAdrs spotlessCheck`; `jq` equality | ✅ | ✅ green |
| 2-22-01 | 22 | 10 | UPS-03 | T-02-49 | Publication approved per draft | checkpoint (blocking-human) | — (manual approval) | n/a | 🔒 manual (approved) |
| 2-22-02 | 22 | 10 | UPS-03 | T-02-49, T-02-50 | Only approved drafts published | check | `checkUpstreamRecords spotlessCheck`; reported-without-link guard; 13 records present | ✅ | ✅ green |
| 2-23-01 | 23 | 11 | INFRA-05, DOC-03 | T-02-46 | No credentials in ADRs | check | `checkAdrs spotlessCheck`; quick-start link grep | ✅ | ✅ green |
| 2-23-02 | 23 | 11 | DOC-03 | T-02-47 | — | check | `checkAdrs spotlessCheck` | ✅ | ✅ green |
| 2-23-03 | 23 | 11 | DOC-03, IDEM-04 | T-02-47 | — | check | `checkAdrs spotlessCheck`; IDEM-04 wording grep | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky · 🔒 manual approval gate (see Manual-Only)*

---

## Wave 0 Requirements

- [x] `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` and `TestSuitesFunctionalTest` (TEST-11/12/14), including the per-suite Kover coverage proof (TEST-11)
- [x] `:contracts-kernel` with `testFixtures`, `TypeIdVectorsTest` (vectors from jetify-com/typeid commit `26a84e85491203834ed0d1c3423ba5f679a67114`), property test (TEST-14, IDEM-04)
- [x] test-support module with Testcontainers singletons plus smoke tests; architecture module with `architectureTest` and the scope sanity test
- [x] dependency licence allowlist, `NOTICE`, per-module CycloneDX wiring, `DependencyLicenceCheckFunctionalTest` (planned as `LicenceCheckFunctionalTest`)
- [x] upstream-record check, ADR check, shared front-matter parser, TestKit tests
- [x] `scripts/doctor.sh`, `scripts/test-doctor.sh`, `scripts/upstream/*` with shell tests
- [x] Catalog entries (images with digests, Konsist, UUID generator, CycloneDX, Kover) and `:platform` constraints

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Upstream issues published only after approval | UPS-03 | Approval gate | Records stay drafts until the author approves; `checkpoint:human-verify` before any `gh issue create` |
| Skills/plugins installed only after approval | AIDEV-01 | Approval gate | Author approves each candidate; settings carry SHA-pinned refs |
| MCP servers installed only after approval | AIDEV-04 | Approval gate | Author approves Context7 and Playwright MCP versions before `mise.toml`/`.mcp.json` change |
| Docker Desktop operation | TOOL-04 | No Docker Desktop on the dev machine | Documented; colima verified automatically |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s for each per-task focused command (integration and full-build commands exceed it by design, D-19)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** validated 2026-11-23 — full `./gradlew build` green (39 test classes, 274 tests, 0 failures), shell tests `test-doctor.sh`, `test-check-pr.sh` and `test-changed-dependencies.sh` green, `docker compose config --quiet` clean

## Validation Audit 2026-11-23

| Metric | Count |
|---|---|
| Gaps found | 0 |
| Resolved | 0 |
| Escalated | 0 |
