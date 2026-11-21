---
phase: 01-repository-build-ci-foundation
verified: 2026-11-21T21:45:00Z
status: human_needed
score: 3/5 must-haves verified
covered_files:
  - .claude/settings.json
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
  - .planning/phases/01-repository-build-ci-foundation/01-15-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-15-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-16-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-16-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-17-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-17-SUMMARY.md
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPaths.kt
  - governance/governed-paths.txt
  - mise.toml
  - renovate.json5
  - scripts/governance/check-pr.sh
  - scripts/governance/list-pr-files.sh
  - scripts/governance/test-check-pr.sh
  - scripts/governance/test-list-pr-files.sh
covered_digest: "v4:sha256:837e1fd713897a98189c2d15b8fdcfdd6c12beedbf37e1fc83192aa4e2f4f154"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 2/5
  gaps_closed:
    - "CR-01: the governance file-list step failed open on empty `gh api` output (closed by plan 01-15)"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Decide on WR-05 (reproduced): `check-pr.sh` reads the file list through a `jq` process substitution whose exit status is never read, so a mid-stream `jq` failure drops the remaining entries and the gate exits 0. Either fix it (write the records to a temp file, check `jq`'s status, assert the loop saw every entry) or record the residual in ADR-0001"
    expected: "A decision by the author. Reproduced here: a two-entry list whose second entry is `.github/workflows/ci.yml` with a NUL in its patch prints a `jq` error, then `governance: no governed file is touched`, exit 0, no label. Judged non-blocking in this report (see Anti-Patterns), flagged as `unverified-prohibition` against the plan 01-15 prohibition 'MUST NOT let governance-success pass a pull request whose complete changed-file list the gate did not evaluate'"
    why_human: "Judgment-tier prohibition with a reproduced counter-example on a narrow path. Whether the real files API can ever emit a NUL inside a patch string (git treats NUL-bearing files as binary and sends no patch, but only the first 8000 bytes are sniffed) can only be settled by the author or a hosted experiment"
  - test: "Decide on WR-06 and WR-07, then apply the ruleset. WR-06: `governance.yml` runs on `pull_request` and executes `check-pr.sh`, `list-pr-files.sh` and `governed-paths.txt` from the PR's own merge ref, so a PR can edit the gate that is meant to hold it. WR-07: alternate Renovate config names (`renovate.json`, `.renovaterc*`) and the tracked `.mise/locks/**` are outside the governed list"
    expected: "Either the author fixes them (base-ref checkout or `pull_request_target`, a real CODEOWNERS with code-owner review, extra governed entries plus matching `Edit(/...)` ask rules) or records the accepted residual for a one-maintainer repository in ADR-0001"
    why_human: "Design-level trade-offs; the requirement text (AIDEV-06) lists governance files and the label, not tamper-resistance against a PR that rewrites the gate. WR-02 (sticky label) was deferred by the author on the same grounds"
  - test: "Open the phase PR and let ci.yml and governance.yml run on GitHub"
    expected: "ci-success (build, lint, secrets jobs) and governance-success are green; the PR carries `governance-approved` because it touches .github/**, mise.toml, renovate.json5, governance/** and the governed build files; the head branch name passes the naming rule; the `List changed files` step prints `governance: listed N changed files`"
    why_human: "Nothing has run on GitHub yet beyond the author's applied repository and security settings. Hosted behaviour (mise-action, setup-java/setup-node, setup-gradle with the encrypted configuration cache, runner `jq` and `gh`, real files-API pagination) is only provable by the first run"
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
    why_human: "The hook wiring (lefthook.yml to scripts/format-staged.sh) is present and shellcheck-clean, but its commit-time behaviour was not exercised in this verification"
  - test: "Register the repository at api.reuse.software"
    expected: "The README REUSE badge shows `compliant`"
    why_human: "Registration sends the project name to a third party and is an author action; local `reuse lint` is already compliant (139/139 files)"
---

# Phase 1: Repository, Build & CI Foundation Verification Report

**Phase Goal:** A contributor can clone the repository, install pinned tools, and build and check the whole multi-project build (no domain code yet) with one command. Every commit and pull request is automatically formatted, analysed, licence-checked and governed.
**Verified:** 2026-11-21T21:45:00Z
**Status:** human_needed
**Re-verification:** Yes, after gap closure (plans 01-15, 01-16, 01-17)

## Goal Achievement

The one blocking gap from the first verification (CR-01) is closed and was re-proven by running the new code, not by reading the SUMMARY. No roadmap truth is FAILED. Three roadmap truths are fully proven in the repository (SC1, SC2, SC5). Two (SC3, SC4) are code-complete but can only be finished by the first hosted run and by author actions that are still pending, and those are classified as human verification, not code gaps.

The re-review found three new warnings (WR-05, WR-06, WR-07). I judged that none of them breaks a must-have, but WR-05 is a real, reproduced fail-open path in the same gate as CR-01, so it is carried as a flagged judgment-tier prohibition for the author to decide on, together with WR-06 and WR-07. The reasoning is under "Anti-Patterns Found".

### Gap-closure check (previous gap: CR-01)

| Check | Evidence | Result |
| --- | --- | --- |
| The workflow no longer pipes `gh api` into `jq` | `.github/workflows/governance.yml` step `List changed files` runs `scripts/governance/list-pr-files.sh` with `GH_TOKEN`, `PR_BASE_REPO`, `PR_NUMBER`, `PR_FILES_JSON`. The workflow sets `defaults.run.shell: bash` (pipefail), so `governance-success`'s `jq -e` is also pipefail-protected | VERIFIED |
| The listing script fails closed | `list-pr-files.sh` reads the status of `gh api --paginate` directly, rejects an empty output file, checks `jq -s 'add'`, requires `type == "array"`, writes to a same-directory temp file and renames atomically; a stale destination is removed first; an EXIT trap removes temp files | VERIFIED |
| The original reproduction now fails | Fake `gh` that prints nothing and exits 0, run against the real script: `governance: gh printed nothing for the pull request files; an empty output is never read as an empty list`, exit 1, no destination file left (only the fake `bin` directory remained) | VERIFIED (run here) |
| Regression tests exist and pass | `bash scripts/governance/test-list-pr-files.sh`: 25 passed, 0 failed. `bash scripts/governance/test-check-pr.sh`: 80 passed, 0 failed (was 39). CI's `lint` job discovers both through `git ls-files -z -- 'scripts/*/test-*.sh'` (ci.yml line 145) | VERIFIED |
| WR-03 (3000-file cap), WR-04 (two parsers), WR-01 (enforcement code ungoverned) closed | `FILES_API_LIMIT` rule in `check-pr.sh`; one grammar in `load_globs` and `GovernedPaths.parse` with twin case tables (80 shell cases, `GovernedPathsFormatFunctionalTest`); `governed-paths.txt` now has 29 entries and `.claude/settings.json` has 29 `Edit(/...)` rules plus `Bash(*config-set*)`, and `checkClaudeAskRules` passes in the build | VERIFIED |

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | `mise install` then `./gradlew build` succeeds with configuration cache, `build-logic` conventions (TestKit-tested), `:platform`, version catalog; every tool listed in `docs/tooling.md`; version literal or `.java-version`/daemon-JVM drift fails the build | VERIFIED | Re-run at HEAD: `mise x -- ./gradlew build --no-build-cache --rerun-tasks` exited 0, "BUILD SUCCESSFUL in 2m 54s", 35 of 35 tasks executed, "Configuration cache entry reused", working tree clean afterwards. Result XML: 86 tests, 0 failures, 0 errors, 0 skipped. `checkBuildScripts`, `checkDaemonJvm`, `checkToolingInventory`, `checkGsdConfigPolicy`, `checkPrTemplate`, `checkClaudeAskRules` all executed in `check`. The literal "fresh clone" run needs a clean machine and network; the equivalent full-rerun build was done |
| 2 | One command formats every artifact type; pre-commit hook formats staged files; CI fails on formatting/`.editorconfig` violations, detekt, broken KDoc refs, missing SPDX header, non-normalised tree | VERIFIED (hook commit-time behaviour is a human item) | Build ran `spotlessMisc/KotlinGradle/Shell/Toml/Kotlin/Prettier` checks, `detekt` (sample module and `build-logic`) and `dokkaGeneratePublicationHtml` (both). `editorconfig-checker` exit 0, `reuse lint` compliant (139/139 files, REUSE 3.3), `actionlint` exit 0, `shellcheck` of `scripts/**/*.sh` exit 0, `zizmor --offline` no findings (2 suppressed), `gitleaks git` 101 commits no leaks, `git ls-files --eol` shows no non-LF index entries |
| 3 | Every push and PR runs a workflow with SHA-pinned actions that builds, tests, analyses, lints workflows/scripts and scans full history; Renovate proposes grouped updates with minimum release age for every version source | UNCERTAIN (code verified, hosted run pending) | All 23 `uses:` lines across the workflows are pinned to 40-character SHAs with a version comment (grep for an unpinned line returned none). `ci.yml` has `build`, `lint`, `secrets` aggregated by `ci-success`. `renovate-config-validator --strict renovate.json5` passes; exactly 3 `automerge: true` rules. Nothing has run on GitHub, so hosted behaviour and Renovate's real PRs are unproven (Human Verification items 3 and 6) |
| 4 | `main` changes only through squash-merged PRs from `gsd/phase-*` branches that pass required checks; ruleset enforces linear history, blocks force-push, head branches deleted; PR template mirrors `/gsd-ship`; labels synced; secret scanning + push protection, Dependabot alerts, PVR, CodeQL, Scorecard active | UNCERTAIN (declared and partly live; ruleset deferred) | `.github/rulesets/main.json` declares the full rule set but is deliberately not applied (author action, item 4). Repository and security settings were verified live with no drift in the first verification; this pass did not repeat that read and no settings file changed since. PR template guarded by `checkPrTemplate`, labels as code, CodeQL (actions language) and Scorecard workflows exist and run from main/schedule only |
| 5 | A PR fails CI if it changes `.planning/config.json` against the declared policy or touches governance files without the `governance-approved` label; the agent must ask before editing governance files or running GSD `config-set` | VERIFIED (with warnings WR-05, WR-06, WR-07) | Config policy: `checkGsdConfigPolicy` in `check` passed. Ask rules: 29 `Edit(/...)` rules match the 29 governed entries one to one (counted here) plus `Bash(*config-set*)`, `checkClaudeAskRules` passed. Label gate: `check-pr.sh` 80 cases pass; the file-list feeder now fails closed (CR-01 closed, re-run here). Three residual hardening paths remain, none of which a routine (non-crafted) PR can trigger; see Anti-Patterns |

**Score:** 3/5 roadmap truths verified (SC1, SC2, SC5); SC3 and SC4 are code-verified and wait on the hosted run or author action. No truth is FAILED. 0 behavior-dependent truths are left unexercised: the fail-closed behaviour of the listing and the gate is exercised by named shell cases (25 and 80 cases, run here).

Plan-level must-haves (the 17 PLAN frontmatter blocks) were read. Plans 01-15 to 01-17 add truths about the listing script, the 3000-file rule, the shared grammar and the widened boundary; every locally checkable one is covered by the table above and the gap-closure table. None reduces the roadmap criteria.

### Prohibitions (ADR-550 D3/D4, judgment tier)

All prohibitions in plans 01-15 to 01-17 are judgment-tier with `status: resolved`. Verdicts are NON-AUTHORITATIVE.

| Prohibition | Verdict | Note |
| --- | --- | --- |
| 01-15: MUST NOT let `governance-success` pass a PR whose complete changed-file list the gate did not evaluate; any failure to obtain or interpret the list fails the check | `unverified-prohibition` (human review recommended) | Holds for every failure of obtaining the list (CR-01 path, re-run here). Does not hold for one failure of interpreting it: WR-05, reproduced here. Flagged, not silently passed |
| 01-15: MUST NOT add any way to pass without the label other than the Renovate version-only exemption; a list at the API limit is never exempt | judgment: holds | `check_files_api_limit` never consults the exemption; boundary values 2999/3000/3001 are tested |
| 01-16: MUST NOT leave any input on which one parser keeps a governed entry and the other drops, rewrites or rejects it | judgment: holds | Re-review compared the grammar line by line; twin tables G1-G9 and K1-K9 pass; I found no divergence |
| 01-17: MUST NOT remove or narrow any of the ten original entries, their ask rules or `config-set` rule | judgment: holds | All ten original entries are still present in `governed-paths.txt`; `Bash(*config-set*)` still in `.claude/settings.json` |
| 01-17: MUST NOT add an entry that makes a D-12 automerge flow need the label | judgment: holds | The catalog, module conventions and Actions workflows (other than `.github/**` already governed) remain outside; the exclusions are argued in the file header |
| 01-15/16/17: MUST NOT perform any GitHub-side write | judgment: holds | Commit history of the gap plans touches repository files only; no GitHub write is evidenced |

### Deferred Items

None. WR-05, WR-06 and WR-07 are not addressed by any later roadmap phase (later phases cover domain, frontend and observability).

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `scripts/governance/list-pr-files.sh` | Fail-closed listing, `per_page=100` | VERIFIED | Exists, substantive, wired from `governance.yml`, 25 cases |
| `scripts/governance/test-list-pr-files.sh` | Fake-`gh` tests (`FAKE_GH_LOG`) | VERIFIED | 25 passed; picked up by the CI glob |
| `.github/workflows/governance.yml` | Runs the listing script under bash with pipefail | VERIFIED | Contains `scripts/governance/list-pr-files.sh` and `defaults.run.shell: bash` |
| `scripts/governance/check-pr.sh` | Label gate with `FILES_API_LIMIT` and shared grammar | VERIFIED (WR-05 open) | 80 cases pass |
| `GovernedPaths.kt`, `AskRulesCheck.kt`, `GovernedPathsFormatFunctionalTest.kt` | Kotlin side of the grammar and its TestKit cases | VERIFIED | Compiled and tested in the build (86 tests, 0 failures); `AskRulesCheck` calls `GovernedPaths.parse` |
| `governance/governed-paths.txt`, `.claude/settings.json` | 29 entries and 29 ask rules plus `config-set`, documented boundary | VERIFIED | Counted here: 29 non-comment entries, 29 `Edit(/` rules; `checkClaudeAskRules` green |
| `gradle/libs.versions.toml`, `platform/**`, `build-logic/**`, `settings.gradle.kts`, `gradle.properties`, `mise.toml`, `mise.lock`, `.editorconfig`, `.gitattributes`, `REUSE.toml`, `lefthook.yml`, the six workflows, `renovate.json5`, `.github/rulesets/main.json`, PR template, `labels.yml`, `docs/tooling.md` | Phase 1 foundation | VERIFIED | Exercised by the passing build and lint commands above; unchanged since the first verification except the governance files |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `governance.yml` `List changed files` | `list-pr-files.sh` | step `run:` with env contract | WIRED | Script is executable and takes only environment inputs |
| `list-pr-files.sh` | `check-pr.sh` | validated array renamed onto `PR_FILES_JSON` (`runner.temp/pr-files.json`) | WIRED | Both steps use the same path; chain cases in `test-list-pr-files.sh` |
| `governed-paths.txt` | `check-pr.sh` (`load_globs`), `AskRulesCheck` (`GovernedPaths.parse`), `.claude/settings.json` | single list, one grammar | WIRED | 29 / 29 / 29 |
| `ci.yml` jobs | `ci-success` | `needs` + `if: always()` | WIRED | Unchanged |
| `.github/rulesets/main.json` | `ci-success`, `governance-success` | required contexts | WIRED (declared) | Not applied yet (author action) |
| `check-pr.sh` | `jq` producer of the per-file records | process substitution `< <(jq --raw-output0 ...)` | PARTIAL | Producer status is never read (WR-05, reproduced) |
| `lefthook.yml` | `scripts/format-staged.sh` / Spotless | `mise x --` | WIRED (static) | Not installed in a checkout yet |

### Data-Flow Trace (Level 4)

Not applicable: nothing renders dynamic data. The one pipeline that matters, PR file list to governance verdict, was traced: `gh api` (status read) to `list-pr-files.sh` (validated, atomic) to `check-pr.sh` (validated shape, then per-file loop). The weak point on that chain is the per-file loop's producer (WR-05).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Whole build, every task re-executed, no build cache | `mise x -- ./gradlew build --no-build-cache --rerun-tasks` | exit 0, 35/35 executed, 2m 54s, tree clean | PASS |
| TestKit and unit tests | result XML under `build-logic/build/test-results` | 86 tests, 0 failures, 0 errors, 0 skipped | PASS |
| Gate shell tests | `mise x -- bash scripts/governance/test-check-pr.sh` | 80 passed, 0 failed | PASS |
| Listing script tests | `mise x -- bash scripts/governance/test-list-pr-files.sh` | 25 passed, 0 failed | PASS |
| CR-01 reproduction (fake `gh`, empty output, exit 0) | `list-pr-files.sh` with a stub `gh` | exit 1, error message, no destination file | PASS |
| WR-05 reproduction (NUL in a governed entry's patch) | `bash /tmp/p01-verify/run-wr05.sh` (real `check-pr.sh`, no label, list with `.github/workflows/ci.yml`) | `jq: error ... Cannot dump a string containing NUL`, then "no governed file is touched", exit 0 | FAIL (confirms WR-05) |
| Linters | `actionlint`, `shellcheck`, `zizmor --offline`, `editorconfig-checker`, `reuse lint`, `renovate-config-validator --strict`, `gitleaks git` | all clean | PASS |

### Probe Execution

SKIPPED: the phase declares no `probe-*.sh` scripts and none exist under `scripts/*/tests/`.

### Requirements Coverage

All 25 IDs in the ROADMAP phase line appear in PLAN frontmatter and in REQUIREMENTS.md as Phase 1. No orphaned Phase 1 requirement exists (INFRA-15 maps to Phase 6, not here).

| Requirement | Source Plan(s) | Status | Evidence |
| --- | --- | --- | --- |
| INFRA-01 | 01-01 | SATISFIED | Full build green |
| INFRA-09 | 01-01, 01-02, 01-03, 01-07, 01-11 | SATISFIED | Catalog, `.java-version`, `.nvmrc`, `mise.lock`, SHA pins; `checkBuildScripts`, `checkDaemonJvm` in the green build |
| INFRA-10 | 01-01, 01-02, 01-07, 01-08, 01-12 | SATISFIED | Configuration cache reused, build-logic included build, `gradle.properties`; hosted cache behaviour pending |
| INFRA-11 | 01-01, 01-02 | SATISFIED | `:platform` with the stack BOMs and Kotlin override |
| INFRA-12 | 01-02 | SATISFIED | Spotless, detekt, Dokka and TestKit apply to `build-logic` (ran in build) |
| INFRA-13 | 01-06, 01-12 | SATISFIED | `.gitattributes`; `git ls-files --eol` clean |
| INFRA-14 | 01-01, 01-04, 01-05, 01-06 | SATISFIED | `editorconfig-checker` exit 0 |
| INFRA-16 | 01-05, 01-06 | SATISFIED | `reuse lint` compliant 139/139; hosted registration pending (human) |
| TEST-09 | 01-02, 01-04, 01-05, 01-08 | SATISFIED | Spotless tasks, lefthook, CI; commit-time hook behaviour is a human item |
| TEST-10 | 01-02 | SATISFIED | detekt `comments` rules and Dokka `failOnWarning` in `check` |
| TEST-13 | 01-02 | SATISFIED | detekt fails the build; the Biome half belongs to the frontend phase (no TypeScript yet) |
| CICD-01 | 01-12 | SATISFIED (code); hosted run pending | `ci.yml` |
| CICD-03 | 01-11, 01-12 | SATISFIED | All 23 `uses:` pinned; no container base images exist yet |
| CICD-04 | 01-11, 01-14 | NEEDS HUMAN | Config valid under `--strict`; Renovate reads it from `main` after the merge. Correctly still pending |
| CICD-06 | 01-08, 01-12 | SATISFIED | `secrets` job with full history; gitleaks clean locally |
| CICD-08 | 01-08, 01-12 | SATISFIED | actionlint, zizmor, shellcheck clean; the `docker compose config` part is moot (no compose file yet) |
| TOOL-01 | 01-03, 01-10 | SATISFIED | `docs/tooling.md`, `checkToolingInventory` |
| TOOL-02 | 01-03, 01-10, 01-11 | SATISFIED | `mise.toml` + `mise.lock`; Renovate extraction configured |
| GH-06 | 01-12, 01-13, 01-14 | NEEDS HUMAN | Ruleset deferred by the author; CodeQL/Scorecard first run after merge. Correctly still pending |
| GH-07 | 01-09, 01-13, 01-14 | SATISFIED | Squash-only, PR title/body, delete branches (verified live, no drift, in the first pass); branch naming in `check-pr.sh` |
| GH-08 | 01-10 | SATISFIED | PR template and `checkPrTemplate` |
| GH-11 | 01-10 | SATISFIED | `labels.yml` and labels workflow |
| AIDEV-05 | 01-07 | SATISFIED | `checkGsdConfigPolicy` green; `.planning/config.json` equals the policy |
| AIDEV-06 | 01-09, 01-14, 01-15, 01-16, 01-17 | SATISFIED (warnings WR-05, WR-06, WR-07) | Label gate, fail-closed feeder, widened boundary all verified; hosted binding waits on the ruleset (GH-06). The earlier "Complete" mark in REQUIREMENTS.md is now supportable |
| AIDEV-08 | 01-13, 01-16, 01-17 | SATISFIED | 29 ask rules plus `config-set`; `checkClaudeAskRules` green |

Bookkeeping note for the orchestrator: REQUIREMENTS.md still shows `Gaps Found` in the traceability table for 20 of these IDs and unchecked boxes for all but AIDEV-06 and AIDEV-08. That reflects the first verification and should be refreshed by whoever owns the state update; this report did not touch it. CICD-04 and GH-06 should stay `Pending` until the human items above are done.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `scripts/governance/check-pr.sh` | 226-260 | WR-05: exit status of `jq` process substitution never read; a mid-stream `jq` failure ends the loop quietly and `touched` stays empty | WARNING (reproduced, judged non-blocking) | Gate exits 0 for a list whose governed entry could not be evaluated. Trigger needs a string with a NUL in a patch or filename; git treats files with NUL as binary and the API sends no patch for them, so a routine PR or a transient failure cannot trigger it. The crafted case is strictly harder to exploit than WR-06, which already allows editing the gate itself. Small fix (temp file plus status check plus `seen == count`) recommended before the ruleset makes `governance-success` binding |
| `.github/workflows/governance.yml` | 8-10, 32-34 | WR-06: gate runs from the PR's own merge ref, so a PR can change the gate that holds it | WARNING (design) | Maintainer-approval contract is guarded against accidents and against an agent that does not tamper with the gate, not against a PR that rewrites it. No CODEOWNERS file exists and the ruleset requires 0 reviews. Record in ADR-0001 or fix with a base-ref checkout |
| `governance/governed-paths.txt` | 41-44 | WR-07: `renovate.json`, `.renovaterc*` and tracked `.mise/locks/**` not governed | WARNING | A new root `renovate.json` would take precedence over the governed `renovate.json5` without the label. Add five entries plus matching ask rules, or accept |
| `scripts/governance/check-pr.sh` | 266-269 | WR-02: approval label sticky, not bound to a commit | WARNING (deferred by the author) | Later pushes to an approved PR still pass |
| `GovernedPathsFormatFunctionalTest.kt` | 55, 84 | IN-06: literal NBSP and BOM in Kotlin source | INFO | Test fragility only; spell them as Kotlin unicode escapes (NBSP U+00A0, BOM U+FEFF) |
| `check-pr.sh`, `ToolingTableCheck.kt`, `ci.yml`, `.gitattributes`, `docs/tooling.md` | various | IN-01 to IN-05 | INFO | Latent or cosmetic, unchanged from the first review |

No `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` markers in non-planning files (grep found none). Review dispositions: CR-01, WR-01, WR-03, WR-04 `fixed`; WR-02 `deferred`; WR-05, WR-06, WR-07, IN-01 to IN-06 `open`; none critical. `01-VALIDATION.md` still has `status: draft` (informational, not a goal criterion).

Why WR-05 is not a gap: CR-01 was a blocker because the trigger was routine, a transient `gh` failure, and the result was a green gate with no evaluation. WR-05's trigger is a crafted NUL in API data. It stays flagged because it is the same fail-open shape in the same gate and the plan 01-15 prohibition says any failure to interpret the list must fail the check. If the author wants that prohibition to hold without exception, the fix is about ten lines and should land with WR-06 and WR-07 as one "gate integrity" change before the ruleset is applied.

### Human Verification Required

See the `human_verification` frontmatter list: the WR-05 decision (flagged prohibition), the WR-06/WR-07 decision, the first hosted run of `ci.yml` and `governance.yml`, applying the deferred ruleset, post-merge CodeQL/Scorecard/label sync, Renovate onboarding PR #1 and first grouped PRs, commit-time behaviour of the lefthook hook, and REUSE registration.

### Gaps Summary

No code gap blocks the phase goal. CR-01 is closed and proven by running the new listing script against the original failure and by 105 passing shell cases (25 plus 80). The full build is green with every task re-executed, 86 TestKit and unit tests pass, and every linter is clean. What remains is author or hosted work: the first PR run, the deferred ruleset, post-merge security workflows, Renovate's first PRs, the hook's commit-time behaviour, REUSE registration, and a decision on WR-05, WR-06 and WR-07 (fix or record in ADR-0001).

---

_Verified: 2026-11-21T21:45:00Z_
_Verifier: Claude (gsd-verifier)_
