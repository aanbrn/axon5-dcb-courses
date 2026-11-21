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
| **Quick run command** | `mise x -- ./gradlew :build-logic:functionalTest :tooling:convention-sample:check` |
| **Full suite command** | `mise x -- ./gradlew build` plus the CI lint commands (`reuse lint`, `editorconfig-checker`, `actionlint`, `zizmor --offline --no-progress .github/workflows`, `gitleaks git --redact --no-banner`) |
| **Estimated runtime** | ~120 seconds (full build with warm caches); per-task commands over 30 s are listed under "Long-running verify commands" |

---

## Sampling Rate

- **After every task commit:** lefthook pre-commit hook plus the quick run command (once the build skeleton exists)
- **After every plan wave:** Run `./gradlew build` and the CI lint commands
- **Before `/gsd-verify-work`:** Full suite must be green, and CI green on the phase PR
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

Rows are generated from the `<verify>` blocks of plans 01-01 to 01-14 (task ID = `<plan>-T<task>`). Every
`--tests` filter names a test class that the listed task creates. Gradle commands that format or check run as
`mise x -- ./gradlew …` unless mise is activated in the shell (plans 01-04 and later). File Exists: `task` = the
task creates the test or checked file itself (tracer-first and TDD tasks, so there is no separate Wave 0);
`prior: <plan>` = an earlier plan created it; `tool` = a CLI pinned in `mise.toml`; `n/a` = checkpoint.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-T1 | 01-01 | 1 | INFRA-01, INFRA-09, INFRA-10, INFRA-14 | T-01-01, T-01-08 | Wrapper distribution checksum-pinned; `.env` ignored | build (sample JUnit test) | `./gradlew :tooling:convention-sample:build` twice, second run contains `Reusing configuration cache.`; `./gradlew :tooling:convention-sample:test --rerun` plus a test XML under `tooling/convention-sample/build/test-results/test` | task | ⬜ pending |
| 01-01-T2 | 01-01 | 1 | INFRA-11 | T-01-02 | Versions only through catalog aliases | Gradle report | `./gradlew :platform:dependencies --configuration api` lists 8 BOMs; `./gradlew :tooling:convention-sample:dependencyInsight --configuration runtimeClasspath --dependency org.jetbrains.kotlin:kotlin-stdlib` resolves the catalog `kotlin` version | task | ⬜ pending |
| 01-02-T1 | 01-02 | 2 | TEST-09, TEST-10, TEST-13, INFRA-09, INFRA-12 | T-01-03 | No detekt baseline | check | `./gradlew spotlessCheck :tooling:convention-sample:check :build-logic:detekt :build-logic:dokkaGeneratePublicationHtml`; `grep -qx 'toolchainVersion=25' gradle/gradle-daemon-jvm.properties` | task | ⬜ pending |
| 01-02-T2 | 01-02 | 2 | INFRA-10, INFRA-11, INFRA-12, TEST-10, TEST-13 | T-01-03 | Each gate proven to fail on a violation | TestKit | `./gradlew :build-logic:functionalTest --tests '*ConventionsFunctionalTest*' --tests '*QualityGatesFunctionalTest*'`; `./gradlew build --dry-run` reaches `:build-logic:functionalTest` | task | ⬜ pending |
| 01-03-T1 | 01-03 | 2 | TOOL-02 | T-01-06, T-01-SC | Package legitimacy confirmed before any install | manual (blocking checkpoint) | — (author replies `approved`) | n/a | ⬜ pending |
| 01-03-T2 | 01-03 | 2 | TOOL-02, INFRA-09 | T-01-04, T-01-05 | Lock-verified binaries; explicit actionlint backend | CLI | `mise x -- reuse --version && … && mise x -- taplo --version`; `mise ls --current node \| grep -q '\.nvmrc'` | tool | ⬜ pending |
| 01-03-T3 | 01-03 | 2 | TOOL-01 | — | N/A | shell check | every `mise.toml` `[tools]` key appears backticked in `docs/tooling.md`; no `X.Y` version string in it | task | ⬜ pending |
| 01-04-T1 | 01-04 | 3 | TEST-09 | T-01-07 | Node major verified against `.nvmrc` | TestKit | `mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*'`; `mise x -- ./gradlew spotlessApply` then `verifyNodeVersion spotlessCheck` | task | ⬜ pending |
| 01-04-T2 | 01-04 | 3 | INFRA-14 | — | N/A | CLI | `mise x -- editorconfig-checker`; `.editorconfig` starts with `root = true` and `[*]` | tool | ⬜ pending |
| 01-05-T1 | 01-05 | 4 | TEST-09, INFRA-14 | T-01-39 | taplo version and config are cache inputs | TestKit | `mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*'`; `spotlessApply` then `spotlessCheck`; Renovate annotation still directly above `prettier = ` | prior: 01-04 (extended) | ⬜ pending |
| 01-05-T2 | 01-05 | 4 | TEST-09, INFRA-14, INFRA-16 | T-01-39 | shfmt version and `.editorconfig` are cache inputs | TestKit + CLI | `mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*'`; `spotlessApply`, `spotlessCheck`, `mise x -- editorconfig-checker` | prior: 01-04 (extended) | ⬜ pending |
| 01-06-T1 | 01-06 | 5 | INFRA-16 | T-01-09 | Licence information on every file | CLI | `mise x -- reuse lint`; REUSE badge in `README.md` | tool | ⬜ pending |
| 01-06-T2 | 01-06 | 5 | INFRA-13 | — | Check never stages into the real index | CLI | renormalise into a temporary `GIT_INDEX_FILE` copy of the index, then `git diff --cached --exit-code --stat`; `git check-attr eol` / `binary` | tool | ⬜ pending |
| 01-07-T1 | 01-07 | 5 | AIDEV-05 | T-01-10 | Config-policy deviation fails with key paths | TestKit | `./gradlew :build-logic:functionalTest --tests '*ConfigPolicyCheckFunctionalTest*'`; `./gradlew checkGsdConfigPolicy` | task | ⬜ pending |
| 01-07-T2 | 01-07 | 5 | INFRA-09, INFRA-10 | T-01-11, T-01-12 | Version literals, eager APIs and JDK drift rejected | TestKit | `./gradlew :build-logic:functionalTest --tests '*BuildScriptLintCheckFunctionalTest*' --tests '*DaemonJvmCheckFunctionalTest*'`; `./gradlew checkBuildScripts checkDaemonJvm` | task | ⬜ pending |
| 01-08-T1 | 01-08 | 6 | TEST-09, CICD-06, CICD-08 | T-01-13, T-01-14 | Staged secret blocked by the hook | CLI + hook run | `mise x -- shellcheck scripts/format-staged.sh && mise x -- ./gradlew spotlessCheck`; `mise x -- reuse lint --quiet && mise x -- gitleaks git --pre-commit --staged --no-banner --redact`; SPDX header on line 2 of the script | task | ⬜ pending |
| 01-08-T2 | 01-08 | 6 | INFRA-10 | T-01-16 | Build Scans opt-in only | CLI | `mise x -- gradle-profiler --benchmark --dry-run --project-dir . --scenario-file performance.scenarios clean_build noop_build abi_change config_cache_hit`; four baseline rows in `docs/performance.md` | task | ⬜ pending |
| 01-09-T1 | 01-09 | 5 | AIDEV-06, GH-07 | T-01-17, T-01-18, T-01-20 | Governance files need the label; no privileged PR trigger | script tests | `bash scripts/governance/test-check-pr.sh`; shellcheck, actionlint and zizmor on the new files | task | ⬜ pending |
| 01-09-T2 | 01-09 | 5 | AIDEV-06, GH-07 | T-01-19 | Renovate exemption only for version-only diffs | script tests | `bash scripts/governance/test-check-pr.sh` (at least 20 cases); `mise x -- shellcheck` | task | ⬜ pending |
| 01-10-T1 | 01-10 | 6 | GH-11 | T-01-21, T-01-22 | `issues: write` only in the sync job | CLI | `mise x -- actionlint .github/workflows/labels.yml && mise x -- zizmor --offline --no-progress .github/workflows/labels.yml && mise x -- ./gradlew spotlessCheck`; 17 `type:`/`area:` labels | task | ⬜ pending |
| 01-10-T2 | 01-10 | 6 | GH-08 | — | N/A | TestKit | `./gradlew :build-logic:functionalTest --tests '*PrTemplateCheckFunctionalTest*'`; `./gradlew checkPrTemplate` | task | ⬜ pending |
| 01-10-T3 | 01-10 | 6 | TOOL-01, TOOL-02 | — | N/A | TestKit | `./gradlew :build-logic:functionalTest --tests '*ToolingTableCheckFunctionalTest*'`; `./gradlew checkToolingInventory` | task | ⬜ pending |
| 01-11-T1 | 01-11 | 7 | CICD-04, INFRA-09, TOOL-02 | T-01-23 | 7-day cooldown on every version source | CLI | `mise x -- renovate-config-validator --strict renovate.json5`; `renovate --platform=local --dry-run=extract` finds `java`, `prettier` and `jdx/mise` | tool | ⬜ pending |
| 01-11-T2 | 01-11 | 7 | CICD-04 | T-01-24, T-01-25 | Automerge limited to the three D-12 rules | CLI | `mise x -- renovate-config-validator --strict renovate.json5`; exactly 3 `automerge: true`; every applied label exists in `.github/labels.yml` | tool | ⬜ pending |
| 01-12-T1 | 01-12 | 8 | CICD-01, CICD-03, INFRA-10 | T-01-26, T-01-28, T-01-29 | SHA-pinned actions, least-privilege tokens | CLI | `mise x -- actionlint .github/workflows/ci.yml && mise x -- zizmor --offline --no-progress .github/workflows/ci.yml`; `./gradlew build --dry-run`; the `ci-success` jq expression rejects `skipped` | tool | ⬜ pending |
| 01-12-T2 | 01-12 | 8 | CICD-01, CICD-06, CICD-08, INFRA-13 | T-01-30 | No secrets in full history | CLI | shellcheck on tracked `*.sh`, `editorconfig-checker`, `reuse lint`, `renovate-config-validator --strict`, every tracked `scripts/test-*.sh` and `scripts/*/test-*.sh`; `mise x -- gitleaks git --redact --no-banner` | tool | ⬜ pending |
| 01-12-T3 | 01-12 | 8 | GH-06, CICD-03 | T-01-27 | Every `uses:` pinned by a 40-hex SHA | CLI | `mise x -- actionlint && mise x -- zizmor --offline --no-progress .github/workflows`; SHA-pin grep over all workflows | tool | ⬜ pending |
| 01-13-T1 | 01-13 | 9 | GH-06, GH-07 | T-01-31, T-01-32 | No write in `--dry-run` or `--check` | script tests | `bash scripts/test-apply-github-settings.sh`; shellcheck; live `scripts/apply-github-settings.sh --dry-run` (read-only) | task | ⬜ pending |
| 01-13-T2 | 01-13 | 9 | GH-06 | T-01-33 | Drift workflow cannot apply settings | CLI | `mise x -- actionlint .github/workflows/settings-drift.yml && mise x -- zizmor --offline --no-progress .github/workflows`; settings test matched by the CI `git ls-files` discovery | tool | ⬜ pending |
| 01-13-T3 | 01-13 | 9 | AIDEV-08 | T-01-34 | Agent asks before governance edits | TestKit + JSON check | `./gradlew :build-logic:functionalTest --tests '*AskRulesCheckFunctionalTest*'`; `./gradlew checkClaudeAskRules`; `jq` finds `Bash(*config-set*)` in `.claude/settings.json` | task | ⬜ pending |
| 01-14-T1 | 01-14 | 10 | GH-06, GH-07 | T-01-35 | Settings applied only after explicit approval | manual (decision checkpoint) | — | n/a | ⬜ pending |
| 01-14-T2 | 01-14 | 10 | GH-06, GH-07, CICD-04 | T-01-36, T-01-38 | The author runs every write | manual (human action) | — | n/a | ⬜ pending |
| 01-14-T3 | 01-14 | 10 | GH-06, GH-07, AIDEV-06 | T-01-35, T-01-37 | Read-only verification of approved items | CLI | `scripts/apply-github-settings.sh --check` (approved sections); `gh label list` and `gh secret list` (names only) | prior: 01-13 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Long-running Verify Commands (> 30 s)

Per-task feedback budget: 180 s with warm Gradle and TestKit caches. Each plan's SUMMARY records the measured
wall-clock duration of every `<automated>` command (the plans' `<verification>` sections require it). The first
run of a plan that downloads dependencies is recorded separately and does not count against the budget. A command
measured over 180 s is reported in the SUMMARY as a validation gap; the remedy is a narrower `--tests` filter per
behaviour, or moving the command to the wave-level check.

| Command | Task(s) | Why it exceeds 30 s | Expected (warm) | Measured |
|---------|---------|---------------------|-----------------|----------|
| `./gradlew :tooling:convention-sample:build` (first run) | 01-01-T1 | distribution and dependency download | excluded (cold); warm under 60 s | 01-01-SUMMARY |
| `./gradlew spotlessCheck :tooling:convention-sample:check :build-logic:detekt :build-logic:dokkaGeneratePublicationHtml` | 01-02-T1 | detekt and Dokka on two projects | 30-90 s | 01-02-SUMMARY |
| `--tests '*ConventionsFunctionalTest*' --tests '*QualityGatesFunctionalTest*'` | 01-02-T2 | at least 7 nested TestKit builds | 60-180 s | 01-02-SUMMARY |
| `--tests '*FormattingFunctionalTest*'` | 01-04-T1, 01-05-T1, 01-05-T2 | nested builds running Prettier, taplo and shfmt | 30-150 s | 01-04-SUMMARY, 01-05-SUMMARY |
| `--tests '*ConfigPolicyCheckFunctionalTest*'`; `--tests '*BuildScriptLintCheckFunctionalTest*' --tests '*DaemonJvmCheckFunctionalTest*'` | 01-07-T1, 01-07-T2 | nested TestKit builds | 30-120 s each | 01-07-SUMMARY |
| `--tests '*PrTemplateCheckFunctionalTest*'`; `--tests '*ToolingTableCheckFunctionalTest*'` | 01-10-T2, 01-10-T3 | nested TestKit builds | 30-90 s each | 01-10-SUMMARY |
| `--tests '*AskRulesCheckFunctionalTest*'` | 01-13-T3 | nested TestKit builds | 30-90 s | 01-13-SUMMARY |
| `gradle-profiler --benchmark --dry-run …` | 01-08-T2 | one Gradle daemon per scenario | 30-120 s | 01-08-SUMMARY |
| `renovate --platform=local --dry-run=extract` | 01-11-T1 | Renovate start-up and extraction | 30-90 s | 01-11-SUMMARY |

---

## Wave 0 Requirements

Tracer-first plans create each test or checked file in the task it verifies, so there is no separate Wave 0 plan.
The items below map to the plans that create them.

- [ ] Gradle wrapper, `settings.gradle.kts`, version catalog, `build-logic` skeleton (plan 01-01); `functionalTest` suite and `GradleFixture` (plan 01-02)
- [ ] TestKit test classes, including negative fixtures for detekt, Dokka and Spotless: `ConventionsFunctionalTest`, `QualityGatesFunctionalTest` (01-02); `FormattingFunctionalTest` (01-04, extended in 01-05); `ConfigPolicyCheckFunctionalTest`, `BuildScriptLintCheckFunctionalTest`, `DaemonJvmCheckFunctionalTest` (01-07); `PrTemplateCheckFunctionalTest`, `ToolingTableCheckFunctionalTest` (01-10); `AskRulesCheckFunctionalTest` (01-13)
- [ ] `config/detekt/detekt.yml`, `config/spotless/license-header.txt` (01-02); `.prettierrc.yaml`, `.editorconfig`, `.editorconfig-checker.json` (01-04); `.taplo.toml`, `config/spotless/license-header-shell.txt` (01-05)
- [ ] `mise.toml`, `mise.lock` (01-03); `lefthook.yml`, `scripts/format-staged.sh` (01-08); shell tests `scripts/governance/test-check-pr.sh` (01-09) and `scripts/test-apply-github-settings.sh` (01-13)
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
