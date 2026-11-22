---
phase: "2"
slug: "runtime-infrastructure-dcb-spikes"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-11-22"
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 6.1.3 (Jupiter) + Kotest assertions 6.2.7 (+ `kotest-property`); Gradle TestKit for `build-logic`; Konsist for architecture; Testcontainers 2.0.5 |
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

Filled by the planner per task; the requirement-level map is in `02-RESEARCH.md` § Validation Architecture → Phase Requirements → Test Map.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 2-XX-XX | XX | X | (per plan) | — | — | — | — | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` and `TestSuitesFunctionalTest` (TEST-11/12/14)
- [ ] `:contracts-kernel` with `testFixtures`, `TypeIdVectorsTest` (vectors from jetify-com/typeid commit `26a84e85491203834ed0d1c3423ba5f679a67114`), property test (TEST-14, IDEM-04)
- [ ] test-support module with Testcontainers singletons plus smoke tests; architecture module with `architectureTest` and the scope sanity test
- [ ] dependency licence allowlist, `NOTICE`, per-module CycloneDX wiring, `LicenceCheckFunctionalTest`
- [ ] upstream-record check, ADR check, shared front-matter parser, TestKit tests
- [ ] `scripts/doctor.sh`, `scripts/test-doctor.sh`, `scripts/upstream/*` with shell tests
- [ ] Catalog entries (images with digests, Konsist, UUID generator, CycloneDX) and `:platform` constraints

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

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
