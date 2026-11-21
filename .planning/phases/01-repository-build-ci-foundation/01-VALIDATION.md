---
phase: "1"
slug: "repository-build-ci-foundation"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-11-21"
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Derived from `01-RESEARCH.md` § Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 6.0.3 (Jupiter) + Kotest assertions 6.2.7; Gradle TestKit for `build-logic` (`functionalTest` suite) |
| **Config file** | `build-logic/build.gradle.kts` (suite); sample module `build.gradle.kts` via the convention plugin — none exists yet, Wave 0 installs |
| **Quick run command** | `./gradlew :build-logic:functionalTest :tooling:convention-sample:check` |
| **Full suite command** | `./gradlew build` plus the CI lint commands (`reuse lint`, `editorconfig-checker`, `actionlint`, `zizmor --offline --no-progress .github/workflows`, `gitleaks git --redact --no-banner`) |
| **Estimated runtime** | ~120 seconds (full build with warm caches) |

---

## Sampling Rate

- **After every task commit:** lefthook pre-commit hook plus the quick run command (once the build skeleton exists)
- **After every plan wave:** Run `./gradlew build` and the CI lint commands
- **Before `/gsd-verify-work`:** Full suite must be green, and CI green on the phase PR
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

Task IDs are assigned by the planner; rows are keyed by requirement until plans exist.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| TBD | TBD | TBD | INFRA-01, INFRA-11 | — | N/A | TestKit | `./gradlew :build-logic:functionalTest --tests '*PlatformConvention*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | INFRA-10 | — | N/A | TestKit | `./gradlew :build-logic:functionalTest --tests '*ConfigurationCache*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | INFRA-12 | — | N/A | check | `./gradlew :build-logic:check` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | INFRA-09 | — | Version literals and JDK drift rejected | TestKit | `./gradlew :build-logic:functionalTest --tests '*VersionLiteral*' --tests '*DaemonJvm*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | AIDEV-05 | — | Config-policy deviation fails with key paths | TestKit | `./gradlew :build-logic:functionalTest --tests '*ConfigPolicy*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | TOOL-01, TOOL-02 | — | N/A | TestKit | `./gradlew :build-logic:functionalTest --tests '*ToolingTable*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | TEST-09 | — | N/A | TestKit + CI | `./gradlew spotlessCheck` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | TEST-10 | — | N/A | TestKit (negative fixtures) | `./gradlew :build-logic:functionalTest --tests '*KDoc*'` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | TEST-13 | — | N/A | sample module | `./gradlew :tooling:convention-sample:detekt` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | INFRA-16 | — | N/A | CLI | `reuse lint` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | INFRA-13 | — | N/A | CLI | `git add --renormalize . && git diff --cached --exit-code` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | INFRA-14 | — | N/A | CLI | `editorconfig-checker` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | CICD-01, CICD-03, CICD-08 | — | SHA-pinned actions, least-privilege tokens | CLI | `actionlint && zizmor --offline --no-progress .github/workflows` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | CICD-06 | — | No secrets in full history | CLI | `gitleaks git --redact --no-banner` | ✅ (tool) | ⬜ pending |
| TBD | TBD | TBD | CICD-04 | — | Cooldown on every version source | CLI | `npx --yes -p renovate@<ver> renovate-config-validator --strict` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | GH-06, GH-07 | — | Ruleset blocks force-push, requires checks | shell test | `scripts/apply-github-settings.sh --dry-run` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | GH-08, GH-11 | — | N/A | Gradle check | `./gradlew :build-logic:check` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | AIDEV-06 | — | Governance files need label | script tests | `scripts/governance/test-*.sh` | ❌ W0 | ⬜ pending |
| TBD | TBD | TBD | AIDEV-08 | — | Agent asks before governance edits | JSON check | `jq` assertion over `.claude/settings.json` (or Gradle check) | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Gradle wrapper, `settings.gradle.kts`, version catalog, `build-logic` skeleton with `functionalTest` suite
- [ ] TestKit fixtures per convention and governance task, including negative fixtures for detekt, Dokka and Spotless
- [ ] `config/detekt/detekt.yml`, `config/spotless/license-header.txt`, `.prettierrc.yaml`, `.editorconfig`, `.editorconfig-checker.json`
- [ ] `mise.toml`, `mise.lock`, `lefthook.yml`, `scripts/format-staged.sh`
- [ ] Framework install via the Gradle wrapper and mise; nothing else to install

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Ruleset, merge settings, security features applied on GitHub | GH-06, GH-07 | Requires admin `gh` calls behind explicit author approval | Run `scripts/apply-github-settings.sh` after approval, then compare `gh api repos/{owner}/{repo}/rulesets` with `.github/rulesets/main.json` |
| Renovate app installed and opening grouped PRs | CICD-04 | Requires installing the Mend Renovate GitHub App | Install the app, confirm the onboarding/dependency dashboard issue appears |
| Fresh-clone bootstrap with `mise install` | TOOL-02, INFRA-01 | Needs a clean machine or clone | Clone into a temp dir, run `mise install && ./gradlew build` |
| CI green on the phase PR, including CodeQL and Scorecard | CICD-01, GH-07 | Runs only on GitHub | Open the phase PR and confirm all required contexts pass |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
