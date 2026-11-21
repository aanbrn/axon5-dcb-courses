---
phase: 01-repository-build-ci-foundation
verified: 2026-11-21T14:00:00Z
status: gaps_found
score: 2/5 must-haves verified
covered_files:
  - .github/rulesets/main.json
  - .github/workflows/ci.yml
  - .github/workflows/governance.yml
  - .planning/phases/01-repository-build-ci-foundation/01-01-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-01-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-02-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-02-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-03-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-03-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-04-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-04-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-05-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-05-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-06-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-06-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-07-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-07-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-08-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-08-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-09-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-09-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-10-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-10-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-11-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-11-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-12-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-12-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-13-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-13-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-14-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-14-SUMMARY.md
  - build.gradle.kts
  - governance/governed-paths.txt
  - gradle/libs.versions.toml
  - mise.toml
  - renovate.json5
  - scripts/governance/check-pr.sh
  - settings.gradle.kts
covered_digest: "v4:sha256:5feabadb94e08ac893b494713da0abbb197cf30043e90273f55296b123645824"
behavior_unverified: 0
overrides_applied: 0
gaps:
  - truth: "A pull request fails CI if it touches governance files without the `governance-approved` label (Success Criterion 5, AIDEV-06; the gate is designed to fail closed)"
    status: partial
    reason: >-
      CR-01 (confirmed): the `List changed files` step in governance.yml pipes `gh api --paginate` into
      `jq -s 'add // []'` with no `shell:` key, so GitHub runs `bash -e {0}` without pipefail. If `gh api`
      emits nothing (network blip, TLS or DNS failure, timeout), jq slurps empty input to `[]`, the step
      exits 0, and check-pr.sh prints "no governed file is touched" and exits 0, so `governance-success`
      goes green for a PR that may touch `.github/**`, `mise.toml` etc. without the label. Reproduced
      locally: `cat /dev/null | jq -s 'add // []'` under `bash -e` gives step exit 0 and `[]`, and
      check-pr.sh then exits 0 on that file. test-check-pr.sh (39 cases) never covers this step because it
      feeds the script a hand-made file.
    artifacts:
      - path: ".github/workflows/governance.yml"
        issue: "Lines 34-36: pipeline without pipefail and with a `// []` default makes the file-list step fail open"
    missing:
      - "Make the step fail on any gh failure: `shell: bash` (adds -o pipefail) plus `set -euo pipefail`, drop the `// []` default, and assert `jq -e 'type == \"array\"'` on the result (or write gh output to a temp file first and check its exit status)"
human_verification:
  - test: "Open the phase PR and let ci.yml and governance.yml run on GitHub"
    expected: "ci-success (build, lint, secrets jobs) and governance-success are green; the PR carries `governance-approved` because it touches .github/**, mise.toml, renovate.json5 and governance/**; the head branch name passes the naming rule"
    why_human: "Nothing has run on GitHub yet (no push, no hosted CI run). Hosted-run behaviour (setup-java/setup-node/mise-action/setup-gradle wiring, encrypted config cache with GRADLE_ENCRYPTION_KEY, runner jq/gh) can only be proven by the first run"
  - test: "After ci-success and governance-success are green on the phase PR, apply the ruleset (`scripts/apply-github-settings.sh --apply --only ruleset`), confirm the printed bypass actor is the repository admin role, delete the SETTINGS_DRIFT_SECTIONS variable, then run `--check` for all sections"
    expected: "Ruleset `main` is live with required contexts ci-success and governance-success bound to the GitHub Actions app (integration_id 15368), squash-only, linear history, no force-push or deletion, admin-only bypass; `--check` reports no drift"
    why_human: "The ruleset is deliberately deferred by the author until both contexts are green; applying it is an author action (GH-06, Success Criterion 4)"
  - test: "After the phase PR merges, confirm CodeQL (actions language) and OpenSSF Scorecard run from main and upload results, and that the labels workflow first sync yields exactly the labels in .github/labels.yml"
    expected: "Code scanning shows both analyses; labels in the repository equal labels.yml"
    why_human: "These workflows trigger on push to main or schedule; they cannot be exercised before the merge"
  - test: "Close Renovate onboarding PR #1 without merging, then after the merge confirm Renovate opens grouped PRs honouring `minimumReleaseAge: 7 days` for catalog, .java-version, mise.toml and Actions"
    expected: "No onboarding PR; update PRs are grouped per renovate.json5 and none appears before the 7-day cooldown (except the documented vulnerability exception and the JDK timestamp-optional waiver)"
    why_human: "Renovate reads renovate.json5 from main only after the merge; CICD-04 stays pending in REQUIREMENTS.md for this reason"
  - test: "In a clone, run `mise install`, `lefthook install`, stage a Kotlin file with bad formatting and a synthetic GitHub token and run `git commit`"
    expected: "The format job re-stages the formatted Kotlin file; the gitleaks job blocks the commit on the token; the reuse, actionlint, zizmor and shellcheck jobs run on matching staged files only"
    why_human: "The author has not run `lefthook install` in a checkout (no pre-commit hook in the shared hooks directory); the hook wiring (lefthook.yml to scripts/format-staged.sh) is present and shellchecked but its commit-time behaviour was not exercised in this verification"
  - test: "Register the repository at api.reuse.software"
    expected: "The README REUSE badge shows `compliant`"
    why_human: "Registration sends the project name to a third party and is an author action; local `reuse lint` is already compliant (128/128 files)"
---

# Phase 1: Repository, Build & CI Foundation Verification Report

**Phase Goal:** A contributor can clone the repository, install pinned tools, and build and check the whole multi-project build (no domain code yet) with one command. Every commit and pull request is automatically formatted, analysed, licence-checked and governed.
**Verified:** 2026-11-21T14:00:00Z
**Status:** gaps_found
**Re-verification:** No, initial verification

## Goal Achievement

The local half of the goal (clone, pinned tools, one-command build and check, formatting, analysis, licence check) is achieved and was proven by running the build and the CI lint commands in this worktree. The "every pull request is governed" half has one real defect (CR-01: the governance gate can fail open), and several roadmap criteria can only be proven by the first hosted run or by author actions that are still pending. No hosted-only item was treated as a code gap.

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | `mise install` then `./gradlew build` succeeds with configuration cache, `build-logic` conventions (TestKit-tested), `:platform`, version catalog; every tool listed in `docs/tooling.md`; version literal or `.java-version`/daemon-JVM drift fails the build | VERIFIED | `mise x -- ./gradlew build --no-build-cache --rerun-tasks` exited 0: 35 of 35 tasks executed, BUILD SUCCESSFUL in 2m 37s, "Configuration cache entry reused", working tree clean afterwards. 78 TestKit and unit test cases, 0 failures, 0 errors (read from result XML). `gradle.properties` sets configuration-cache + `problems=fail`, build cache, parallel, isolated-projects, vfs.watch. `:platform` imports 8 BOMs from catalog aliases with the Kotlin override. `courses.governance` wires `checkBuildScripts` (version literals, eager APIs), `checkDaemonJvm`, `checkToolingInventory` into `check`; each has a functional test that proves the failure (BuildScriptLint 8, DaemonJvm 9, ToolingTable 10 cases). docs/tooling.md lists 17 mise tools (matches mise.toml via `checkToolingInventory`) plus Gradle, JDK, Node, git, Docker engine and a "Planned tools" table for pnpm, pack, trivy, grype, syft, cosign, Structurizr, act, docker compose. The literal "fresh clone" run was not repeated (it needs network and a clean machine); the equivalent full-rerun build was. |
| 2 | One command formats every artifact type; pre-commit hook formats staged files; CI fails on formatting/`.editorconfig` violations, detekt, broken KDoc refs, missing SPDX header, non-normalised tree | VERIFIED (hook behaviour is a human item) | `spotlessCheck` runs 6 Spotless tasks (Kotlin, KotlinGradle, Misc, Shell, Toml, Prettier) in the full build and `spotlessApply` leaves the tree clean. detekt (`dev.detekt` 2.0.0-alpha.6, `comments` rules active, no baseline) and Dokka `failOnWarning` run in `check` for the sample module and for `build-logic`. Local runs of the CI lint commands: `editorconfig-checker` exit 0, `reuse lint` compliant (128/128 files, REUSE 3.3), `actionlint` exit 0, `zizmor --offline` no findings, `gitleaks git` 79 commits no leaks, `git ls-files --eol` shows no non-LF index entries. ci.yml `lint` job runs the same commands plus index renormalisation. `lefthook.yml` and `scripts/format-staged.sh` exist and are wired to Spotless; see Human Verification item 5 for the commit-time behaviour. |
| 3 | Every push and PR runs a workflow with SHA-pinned actions that builds, tests, analyses, lints workflows/scripts and scans full history; Renovate proposes grouped updates with minimum release age for every version source | UNCERTAIN (code verified, hosted run pending) | ci.yml: `build`, `lint`, `secrets` (`fetch-depth: 0`, `gitleaks git`) aggregated by `ci-success` with `if: always()` and a jq check that rejects `skipped`. All `uses:` lines across 6 workflows are pinned to 40-character SHAs with version comments (grep found no unpinned line); workflow-level `permissions: {}` everywhere with minimal job grants and `persist-credentials: false`. `renovate-config-validator --strict renovate.json5` passes ("Config validated successfully"). renovate.json5 sets `minimumReleaseAge: "7 days"` at top level, groups (kotlin, junit, kotest, spring-boot, axon, testcontainers, formatting, github-actions, dev tools), custom managers for `.java-version`, annotated catalog versions and mise `min_version`; the catalog, wrapper, mise tools, `.nvmrc` and Actions come from built-in managers. Exactly 3 `automerge: true` rules. Nothing has run on GitHub, so the workflows' hosted behaviour and Renovate's actual PRs are unproven (Human Verification items 1 and 4). |
| 4 | `main` changes only through squash-merged PRs from `gsd/phase-*` branches that pass required checks; ruleset enforces linear history, blocks force-push, head branches deleted; PR template mirrors `/gsd-ship`; labels synced; secret scanning + push protection, Dependabot alerts, PVR, CodeQL, Scorecard active | UNCERTAIN (declared and partly live; ruleset deferred) | `.github/rulesets/main.json` declares deletion, non_fast_forward, required_linear_history, PR with squash-only, required contexts `ci-success` and `governance-success` bound to integration 15368, admin-only bypass (`RepositoryRole` 5, mode `pull_request`). It is NOT live (deliberately deferred by the author). Live repository and security settings: `scripts/apply-github-settings.sh --check --only repository --only security` reported "no drift" (run by this verifier, read-only): squash only, PR_TITLE/PR_BODY, delete head branches, auto-merge, secret scanning, push protection, Dependabot alerts, private vulnerability reporting on. PR template carries the `/gsd-ship` sections in order and `checkPrTemplate` (10 test cases) guards drift. `labels.yml` + `labels.yml` workflow (dry-run on PR, sync on main, `issues: write` only in sync). CodeQL (actions language; Kotlin deferred by ADR-0002) and Scorecard workflows exist and run only from main/schedule. `check-pr.sh` branch rule accepts `gsd/phase-<n>-<slug>` (current branch `gsd/phase-01-execution` passes). Ruleset apply and first CodeQL/Scorecard runs are author or post-merge actions (Human Verification items 2 and 3). |
| 5 | A PR fails CI if it changes `.planning/config.json` against the declared policy or touches governance files without the `governance-approved` label; the agent must ask before editing governance files or running GSD `config-set` | FAILED (partial) | Config policy: `checkGsdConfigPolicy` in `check`; `.planning/config.json` is semantically equal to `governance/gsd-config-policy.json` (verified by diff); 8 functional test cases. Ask rules: `.claude/settings.json` has `ask` entries for all 10 governed paths plus `Bash(*config-set*)`; `checkClaudeAskRules` is in `check` (15 test cases). Label gate: check-pr.sh is careful and well tested (39 cases pass, including fork, Unicode, Renovate version-only exemption). BUT the workflow step that feeds it can fail open (CR-01, reproduced): an empty `gh api` output yields `[]` and a green `governance-success` without evaluating any file. See Gaps. |

**Score:** 2/5 roadmap truths verified (SC1, SC2); SC3 and SC4 are code-verified but need the hosted run or author action; SC5 failed on CR-01.

Plan-level must-haves (the 14 PLAN frontmatter blocks) were read; none reduces or contradicts the roadmap criteria above. Those that are locally checkable are covered by the evidence in the rows above (platform BOMs, `.java-version` as the only JDK source, SPDX header via Spotless, daemon JVM generated from `.java-version`, mise pins with `mise.lock`, Prettier/taplo/shfmt as cache inputs, REUSE.toml aggregate precedence, `.gitattributes` rules, label groups, Renovate extraction, ruleset and repository-settings files).

### Deferred Items

None. The CR-01 gap and the warnings below are not addressed by any later roadmap phase.

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `gradle/libs.versions.toml`, `platform/build.gradle.kts`, `build-logic/**`, `settings.gradle.kts`, `gradle.properties` | Version catalog, `:platform`, convention plugins, build speed settings | VERIFIED | Present, substantive, used by the passing build; the sample module applies `courses.kotlin-library`. |
| `build-logic/src/functionalTest/**` | TestKit tests incl. configuration cache | VERIFIED | 11 classes, 77 `@Test` methods, all passing; `a second run reuses the configuration cache` present. |
| `mise.toml`, `mise.lock`, `.mise/locks/**`, `.nvmrc`, `.java-version`, `docs/tooling.md` | Pinned tools and inventory | VERIFIED | `locked = true`; inventory enforced by `checkToolingInventory`. |
| `.editorconfig`, `.prettierrc.yaml`, `.taplo.toml`, `.gitattributes`, `.gitignore`, `REUSE.toml`, `LICENSES/*` | Formatting, normalisation, REUSE | VERIFIED | `editorconfig-checker`, `reuse lint` and the Spotless tasks pass. |
| `lefthook.yml`, `scripts/format-staged.sh` | Pre-commit hook | VERIFIED (static) | Present, shellcheck-clean, wired; commit-time behaviour is a human item. |
| `.github/workflows/{ci,governance,codeql,scorecard,labels,settings-drift}.yml` | CI, governance, security analysis | PARTIAL | All pass actionlint and zizmor and are SHA-pinned; `governance.yml` has the CR-01 defect. |
| `governance/**`, `scripts/governance/**`, build-logic governance checks | Governance gate | PARTIAL | Substantive and heavily tested; fail-open feeder step (CR-01), plus WR-01..WR-04 hardening gaps. |
| `.github/rulesets/main.json`, `.github/repository-settings.json`, `scripts/apply-github-settings.sh` | Settings as code | VERIFIED (declared); ruleset not applied | Repository and security sections verified live with no drift; ruleset deferred by the author. |
| `renovate.json5` | Renovate config | VERIFIED (static) | Validates under `--strict`. |
| `.github/PULL_REQUEST_TEMPLATE.md`, `.github/labels.yml` | PR template, labels | VERIFIED | Guarded by `checkPrTemplate`; `governance-approved` description in labels.yml matches the live label. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `build.gradle.kts` | `courses.governance` / `courses.root` | plugin ids | WIRED | Six governance checks are dependencies of `check`. |
| `courses.kotlin-library` | `:platform` | `platform(project(":platform"))` on every `implementation`/`*Implementation` configuration | WIRED | Applied by `configurations.matching{...}.configureEach`; covers main and test. Only the `implementation` family is covered, which is all that exists today. |
| `courses.kotlin-library` | `.java-version` | `providers.fileContents` to `JavaLanguageVersion` | WIRED | Functional test proves editing the file invalidates the configuration cache. |
| `governed-paths.txt` | `check-pr.sh`, `AskRulesCheck`, `.claude/settings.json` | single source list | WIRED | All 10 entries covered by ask rules; see WR-04 for the parser mismatch. |
| `governance.yml` file-list step | `check-pr.sh` | `pr-files.json` via `PR_FILES_JSON` | PARTIAL | Wired, but the step can hand over an empty list on `gh` failure (CR-01). |
| `ci.yml` jobs | `ci-success` | `needs` + `if: always()` | WIRED | Aggregator rejects any non-`success` result. |
| `.github/rulesets/main.json` | `ci-success`, `governance-success` job names | required contexts | WIRED (declared) | Job names match; not applied yet. |
| `lefthook.yml` | `scripts/format-staged.sh` / Gradle `spotlessApply` | `mise x --` | WIRED (static) | Not installed in a checkout yet. |

### Data-Flow Trace (Level 4)

Not applicable: no component renders dynamic data; the phase delivers build, CI and governance configuration. The one data pipeline that matters, PR file list to governance verdict, is traced under CR-01.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Whole build with every task re-executed and no build cache | `mise x -- ./gradlew build --no-build-cache --rerun-tasks` | exit 0, 35/35 tasks executed, 2m 37s, tree clean afterwards | PASS |
| Governance gate shell tests | `mise x -- bash scripts/governance/test-check-pr.sh` | 39 passed, 0 failed | PASS |
| Settings script tests | `mise x -- bash scripts/test-apply-github-settings.sh` | 88 passed, 0 failed | PASS |
| Live repository and security settings | `mise x -- scripts/apply-github-settings.sh --check --only repository --only security` | no drift | PASS |
| Governance step with empty `gh` output | `cat /dev/null \| jq -s 'add // []'` under `bash -e`, then `check-pr.sh` | step exit 0, `[]`, gate exit 0 ("no governed file is touched") | FAIL (confirms CR-01) |

### Probe Execution

SKIPPED: the phase declares no `probe-*.sh` scripts and none exist under `scripts/*/tests/`.

### Requirements Coverage

All 25 IDs in the ROADMAP phase line appear in PLAN frontmatter and in REQUIREMENTS.md traceability as Phase 1. No orphaned Phase 1 requirement exists.

| Requirement | Source Plan(s) | Description (short) | Status | Evidence |
| --- | --- | --- | --- | --- |
| INFRA-01 | 01-01 | One-command build, convention plugins, catalog | SATISFIED | Full build green |
| INFRA-09 | 01-01, 01-02, 01-03, 01-07, 01-11 | Versions pinned in one place per ecosystem | SATISFIED | Catalog, `.java-version`, `.nvmrc`, mise.lock, SHA pins; `checkBuildScripts`, `checkDaemonJvm` |
| INFRA-10 | 01-01, 01-02, 01-07, 01-08, 01-12 | Build speed mechanisms | SATISFIED | gradle.properties, build-logic included build, gradle-profiler scenarios in `performance.scenarios` and `docs/performance.md`; hosted cache behaviour pending |
| INFRA-11 | 01-01, 01-02 | `:platform` alignment point | SATISFIED | 8 BOMs plus Kotlin constraint |
| INFRA-12 | 01-02 | build-logic first-class | SATISFIED | Spotless, detekt, Dokka and TestKit apply to build-logic |
| INFRA-13 | 01-06, 01-12 | `.gitattributes` and normalisation check | SATISFIED | File content plus CI renormalise step; `git ls-files --eol` clean |
| INFRA-14 | 01-01, 01-04, 01-05, 01-06 | `.gitignore` and `.editorconfig` | SATISFIED | `editorconfig-checker` passes |
| INFRA-16 | 01-05, 01-06 | REUSE compliance | SATISFIED | `reuse lint` compliant; badge in README; hosted registration pending (human) |
| TEST-09 | 01-02, 01-04, 01-05, 01-08 | One-command formatting, hook, CI check | SATISFIED | Spotless tasks, lefthook, CI; hook behaviour is a human item |
| TEST-10 | 01-02 | KDoc via ktfmt, detekt comments, Dokka failOnWarning | SATISFIED | detekt config and Dokka in `check` |
| TEST-13 | 01-02 | Static analysis fails build | SATISFIED | detekt in `check`; Biome part of the requirement belongs to the frontend phase (no TS yet) |
| CICD-01 | 01-12 | Actions build and test on push and PR | SATISFIED (code); hosted run pending | ci.yml |
| CICD-03 | 01-11, 01-12 | Actions pinned by SHA | SATISFIED | All `uses:` pinned; no container base images exist yet |
| CICD-04 | 01-11, 01-14 | Renovate with grouping and min age | NEEDS HUMAN | Config valid; Renovate reads it from `main` only after merge. Correctly still pending in REQUIREMENTS.md |
| CICD-06 | 01-08, 01-12 | Secret leak detection (history and PRs) | SATISFIED | ci.yml `secrets` job full history; gitleaks pre-commit job; local scan clean |
| CICD-08 | 01-08, 01-12 | actionlint, zizmor, shellcheck locally and in CI | SATISFIED | Run locally, zero findings; `docker compose config` part is moot (no compose file yet) |
| TOOL-01 | 01-03, 01-10 | Tool inventory in docs/tooling.md | SATISFIED | Inventory plus planned tools; `checkToolingInventory` |
| TOOL-02 | 01-03, 01-10, 01-11 | CLI tools pinned in mise.toml, Renovate | SATISFIED | mise.toml + mise.lock; Renovate extraction configured |
| GH-06 | 01-12, 01-13, 01-14 | Repository security settings and ruleset | NEEDS HUMAN | Security settings live, no drift; ruleset deferred; CodeQL/Scorecard first run after merge. Correctly still pending in REQUIREMENTS.md |
| GH-07 | 01-09, 01-13, 01-14 | GSD workflow repository config | SATISFIED | Squash only, PR title/body, delete branches live, no drift; branch naming enforced by check-pr.sh |
| GH-08 | 01-10 | PR template mirrors `/gsd-ship` | SATISFIED | Template and `checkPrTemplate` |
| GH-11 | 01-10 | Labels as code | SATISFIED | labels.yml and labels workflow |
| AIDEV-05 | 01-07 | `.planning/config.json` policy | SATISFIED | `checkGsdConfigPolicy`; config equals policy |
| AIDEV-06 | 01-09, 01-14 | Governance files need the label | PARTIAL | Gate logic is correct and tested; CR-01 lets it fail open on a transient `gh` failure; REQUIREMENTS.md marks it complete, which overstates this until CR-01 is fixed |
| AIDEV-08 | 01-13 | Claude ask rules for governance files and `config-set` | SATISFIED | `.claude/settings.json` and `checkClaudeAskRules` |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `.github/workflows/governance.yml` | 34-36 | Pipeline without pipefail, `// []` default (CR-01) | BLOCKER | Governance gate fails open on empty `gh` output |
| `governance/governed-paths.txt` | 1-10 | Enforcement code not governed (WR-01) | WARNING | A PR can edit `courses.governance.gradle.kts`, the `buildlogic/governance/**` checks or `scripts/apply-github-settings.sh` without the label; the listed governance files of AIDEV-06 are all covered, so this is hardening beyond the requirement text |
| `scripts/governance/check-pr.sh` | 206-209 | Sticky approval label, not bound to a commit (WR-02) | WARNING | Later pushes to an approved PR still pass; ruleset has `dismiss_stale_reviews_on_push: false` and 0 reviews. Accept and document in ADR-0001, or bind to head SHA |
| `.github/workflows/governance.yml`, `check-pr.sh` | 35-36, 81-84 | 3000-file API cap not detected (WR-03) | WARNING | A PR with 3000 or more files can push a governed file out of the list |
| `check-pr.sh` / `AskRulesCheck.kt` | 74-76 / 46 | Governed-paths parsed differently (WR-04) | WARNING | A padded entry would be silently ungoverned in CI while Gradle passes; latent, no current entry is padded |
| `check-pr.sh` | 24-26 | Branch pattern vs policy milestone template (IN-01) | INFO | Unreachable while `branching_strategy` is `phase` |
| `ToolingTableCheck.kt` | 79-90 | Cannot see `[tools.<name>]` sub-tables (IN-02) | INFO | Latent |
| `ci.yml`, build-logic plugins | various | Duplicated mise-version step and JDK parse (IN-03) | INFO | Maintainability |
| `.gitattributes`, `.editorconfig` | various | Redundant or anticipatory entries (IN-04) | INFO | Cosmetic |
| `governance.yml`, `docs/tooling.md` | 25-46 / 80 | Governance job uses runner `jq` and `gh`, not mise-pinned (IN-05) | INFO | Behaviour could drift with a runner image change; fails closed on missing features |

No `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` markers in non-planning files. Review dispositions for all 10 findings are still `open` in 01-REVIEW-DISPOSITION.md. 01-VALIDATION.md still has `status: draft` and `nyquist_compliant: false` (informational; not a goal criterion).

### Human Verification Required

See the `human_verification` frontmatter list: first hosted run of ci.yml and governance.yml with the `governance-approved` label; applying the deferred ruleset after both required contexts are green; CodeQL, Scorecard and label sync after merge; Renovate onboarding PR #1 and first grouped PRs; commit-time behaviour of the lefthook pre-commit hook; REUSE registration.

### Gaps Summary

One code gap blocks "every pull request is governed": CR-01. The governance workflow's file-list step can produce an empty list when `gh api` fails to print anything, and the gate then reports "no governed file is touched" and goes green. The fix is small (set `shell: bash`, drop the `// []` default, validate that the result is an array) and the fix should come with a test for the empty-output case. Because the phase PR itself touches `.github/**` and `mise.toml`, and the ruleset that makes `governance-success` binding is still deferred, the exposure today is limited, but the gate is the control that Success Criterion 5 and AIDEV-06 rely on once the ruleset is applied. WR-01 to WR-04 are hardening of the same gate; the author may fix them in the same round or record them as accepted (WR-02 in particular is a documentation decision for ADR-0001).

Everything else that can be checked in the repository holds: the full build is green with every task re-executed, 78 TestKit and unit tests pass, formatters, detekt, Dokka, REUSE, editorconfig, actionlint, zizmor and gitleaks are clean, all actions are SHA-pinned, Renovate's configuration validates, the 25 requirement IDs are all accounted for, and the live repository and security settings match the declared files. CICD-04 and GH-06 are correctly left pending in REQUIREMENTS.md until the hosted run and the author actions listed above.

---

_Verified: 2026-11-21T14:00:00Z_
_Verifier: Claude (gsd-verifier)_
