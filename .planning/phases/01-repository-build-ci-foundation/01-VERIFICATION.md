---
phase: 01-repository-build-ci-foundation
verified: 2026-11-21T23:30:00Z
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
  - .planning/phases/01-repository-build-ci-foundation/01-18-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-18-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-19-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-19-SUMMARY.md
  - .planning/phases/01-repository-build-ci-foundation/01-20-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-20-SUMMARY.md
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPaths.kt
  - docs/adr/0001-workflow-permissions-and-required-checks.md
  - docs/adr/0003-governance-gate-trust-boundary.md
  - governance/governed-paths.txt
  - mise.toml
  - renovate.json5
  - scripts/governance/check-pr.sh
  - scripts/governance/list-pr-files.sh
  - scripts/governance/test-check-pr.sh
  - scripts/governance/test-list-pr-files.sh
covered_digest: "v4:sha256:d35484742153cc615d839569343ac89fb3d9ad27d6556bc42c32e0842e9a8fcc"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 3/5
  gaps_closed:
    - "WR-05: check-pr.sh jq process-substitution fail-open path (fixed by plan 01-18, re-proven here by running it)"
    - "WR-07: governed boundary omitted Renovate and mise names that carry the same authority (fixed by plan 01-19: 54 entries, 54 ask rules)"
    - "WR-06: gate runs from the PR's own tree (accepted residual, recorded in ADR-0003; the ADR states the risk accurately, so it is not a gap)"
    - "WR-08: ADR-0003 overclaimed the ask-rule control (fixed in 5980b3d, wording now states shell writes are not matched)"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Open the phase PR and let ci.yml and governance.yml run on GitHub"
    expected: "ci-success (build, lint, secrets jobs) and governance-success are green; the PR carries `governance-approved` because it touches governed files; the head branch name passes the naming rule; the `List changed files` step prints `governance: listed N changed files`"
    why_human: "Nothing has run on GitHub yet beyond the author's applied repository and security settings. Hosted behaviour (mise-action, setup-java/setup-node, setup-gradle with the encrypted configuration cache, runner `jq` and `gh`, real files-API pagination) is only provable by the first run"
  - test: "After ci-success and governance-success are green on the phase PR, apply the ruleset (`scripts/apply-github-settings.sh --apply --only ruleset`), confirm the printed bypass actor is the repository admin role, delete the SETTINGS_DRIFT_SECTIONS variable, then run `--check` for all sections"
    expected: "Ruleset `main` is live with required contexts ci-success and governance-success bound to the GitHub Actions app (integration_id 15368), squash-only, linear history, no force-push or deletion, admin-only bypass; `--check` reports no drift"
    why_human: "The ruleset is deliberately deferred by the author until both contexts are green; applying it is an author action (GH-06, Success Criterion 4)"
  - test: "After the phase PR merges, confirm CodeQL (actions language) and OpenSSF Scorecard run from main and upload results, and that the labels workflow first sync yields exactly the labels in .github/labels.yml"
    expected: "Code scanning shows both analyses; labels in the repository equal labels.yml"
    why_human: "These workflows trigger on push to main or schedule; they cannot be exercised before the merge"
  - test: "Close Renovate onboarding PR #1 without merging, then after the merge confirm Renovate opens grouped PRs honouring `minimumReleaseAge: 7 days` for catalog, .java-version, mise.toml and Actions"
    expected: "No onboarding PR; update PRs are grouped per renovate.json5 and none appears before the 7-day cooldown (except the documented vulnerability exception and the JDK timestamp-optional waiver). Expect Renovate bumps of `npm:renovate` and `pipx:reuse` that rewrite a `.mise/locks/**` sidecar to need the `governance-approved` label (accepted in plan 01-19, T-01-54)"
    why_human: "Renovate reads renovate.json5 from main only after the merge; CICD-04 stays pending in REQUIREMENTS.md for this reason"
  - test: "In a clone, run `mise install`, `lefthook install`, stage a Kotlin file with bad formatting and a synthetic GitHub token and run `git commit`"
    expected: "The format job re-stages the formatted Kotlin file; the gitleaks job blocks the commit on the token; the reuse, actionlint, zizmor and shellcheck jobs run on matching staged files only"
    why_human: "The hook wiring (lefthook.yml to scripts/format-staged.sh) is present and shellcheck-clean, but its commit-time behaviour was not exercised in this verification"
  - test: "Register the repository at api.reuse.software"
    expected: "The README REUSE badge shows `compliant`"
    why_human: "Registration sends the project name to a third party and is an author action; local `reuse lint` is already compliant (147/147 files)"
---

# Phase 1: Repository, Build & CI Foundation Verification Report

**Phase Goal:** A contributor can clone the repository, install pinned tools, and build and check the whole multi-project build (no domain code yet) with one command. Every commit and pull request is automatically formatted, analysed, licence-checked and governed.
**Verified:** 2026-11-21T23:30:00Z
**Status:** human_needed
**Re-verification:** Yes, after the second gap round (plans 01-18, 01-19, 01-20) and the author's decisions on WR-05, WR-06 and WR-07

## Goal Achievement

No roadmap truth is FAILED and no code gap remains. The two decisions that were open in the previous report are resolved: WR-05 and WR-07 are fixed in code and I re-proved both by running the code, not by reading the SUMMARY; WR-06 is accepted as a residual and ADR-0003 describes it accurately. UAT tests 1 and 2 (the author's decisions) are therefore resolved by the code and the ADR. Three roadmap truths are fully proven in the repository (SC1, SC2, SC5). SC3 and SC4 are code-complete but can only be finished by the first hosted run and by author actions that are still pending; those are hosted-run items, not code gaps.

### Gap-closure check (this round)

| Finding | What was claimed | What I checked | Result |
| --- | --- | --- | --- |
| WR-05 (jq process substitution status lost) | Records written to a `mktemp` file, jq status read, `seen` compared with `jq length` | Read `check-pr.sh` lines 224-312: `jq ... >"$RECORDS_FILE" \|\| jq_status=$?`, explicit `return 1` on non-zero, loop counts `seen`, `seen -ne count` returns 1 before the label or exemption is consulted, EXIT trap removes the file. No `< <(jq` remains. Reproduced the original failing input (harmless first entry, `.github/workflows/ci.yml` second with a NUL in its patch, no label): output `jq: error ... Cannot dump a string containing NUL`, `the changed files list could not be read: jq exited with status 5`, exit 1. No `check-pr-records.*` file left in `/tmp`. Ran `test-check-pr.sh`: 122 passed, 0 failed (includes R1-R11) | VERIFIED |
| WR-07 (boundary omitted equal-authority files) | 54 governed entries, 54 ask rules, `Bash(*config-set*)` retained | Counted: 54 non-comment entries in `governed-paths.txt`, 54 `Edit(/` rules in `.claude/settings.json`, `Bash(*config-set*)` present (line 59). `checkClaudeAskRules` is green in the build (it compares the two through `GovernedPaths.parse`). All ten original entries are still present. `renovate.json`, `renovate.jsonc`, `.renovaterc*`, the `.gitlab/` names, `package.json`, every mise config/rc name and `.mise/locks/**` are governed; the exclusions are argued in the file header | VERIFIED |
| WR-06 (gate runs from the PR's own tree) | Accepted residual, recorded in `docs/adr/0003-governance-gate-trust-boundary.md` | Read the ADR against the repository. Trigger is `pull_request` with the merge-ref checkout; no CODEOWNERS exists; `.github/rulesets/main.json` has `required_approving_review_count: 0`, `require_code_owner_review: false`, squash only, admin bypass `pull_request` mode. The three listed capabilities of a tampering PR (gate exits 0, narrowed list, workflow edited) are true. The rejected alternatives are stated correctly (`pull_request_target` is a zizmor `dangerous-triggers` audit; a sparse base checkout leaves the workflow file under PR control; an author cannot approve their own PR). The ask-rule claim now says shell writes are not matched, which is true: the rules are `Edit(...)` patterns. The ADR does not understate the risk | VERIFIED (accepted residual, not a gap) |
| WR-08 (ADR overclaim) | Fixed in `5980b3d` | ADR-0003 lines 51-55 and 76-79 now say the rules are a guard rail, not a security boundary, and that a shell command is not matched | VERIFIED |

Carried from the first and second verification and re-checked at HEAD: CR-01 (empty `gh api` output fails open) stays closed. `test-list-pr-files.sh` passes 25 of 25, and `governance.yml` still runs `list-pr-files.sh` under `defaults.run.shell: bash`.

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | `mise install` then `./gradlew build` succeeds with configuration cache, `build-logic` conventions (TestKit-tested), `:platform`, version catalog; every tool listed in `docs/tooling.md`; version literal or `.java-version`/daemon-JVM drift fails the build | VERIFIED | Previous verification ran `mise x -- ./gradlew build --no-build-cache --rerun-tasks`: exit 0, 35/35 tasks executed, 86 tests, 0 failures. At this HEAD: `./gradlew build` BUILD SUCCESSFUL (35 tasks, "Configuration cache entry reused"), plus `checkClaudeAskRules`, `checkBuildScripts`, `checkGsdConfigPolicy`, `checkPrTemplate`, `checkToolingInventory` green. The orchestrator also ran the build, `spotlessCheck` and `editorconfig-checker` green at HEAD. A literal fresh-clone run needs a clean machine and network |
| 2 | One command formats every artifact type; pre-commit hook formats staged files; CI fails on formatting/`.editorconfig` violations, detekt, broken KDoc refs, missing SPDX header, non-normalised tree | VERIFIED (commit-time hook behaviour is a human item) | Build runs the Spotless checks, `detekt`, Dokka with `failOnWarning`. At HEAD: `editorconfig-checker` exit 0; `reuse lint` compliant, 147 of 147 files with licence information (REUSE 3.3); `actionlint` exit 0; `shellcheck scripts/*.sh scripts/*/*.sh` exit 0; `zizmor --offline` no findings (2 suppressed) |
| 3 | Every push and PR runs a workflow with SHA-pinned actions that builds, tests, analyses, lints workflows/scripts and scans full history; Renovate proposes grouped updates with minimum release age for every version source | UNCERTAIN (code verified, hosted run pending) | Every `uses:` line in `.github/workflows/*.yml` is pinned to a 40-character SHA (grep for an unpinned line returned none). `ci.yml` has `build`, `lint`, `secrets` aggregated by `ci-success`. `renovate.json5` has exactly three `automerge: true` rules (D-12). Nothing has run on GitHub, so hosted behaviour and Renovate's real PRs are unproven (Human Verification items 1 and 4) |
| 4 | `main` changes only through squash-merged PRs from `gsd/phase-*` branches that pass required checks; ruleset enforces linear history, blocks force-push, head branches deleted; PR template mirrors `/gsd-ship`; labels synced; secret scanning + push protection, Dependabot alerts, PVR, CodeQL, Scorecard active | UNCERTAIN (declared and partly live; ruleset deferred) | `.github/rulesets/main.json` declares deletion, non_fast_forward, linear history, squash-only pull request and the two required contexts bound to integration 15368; it is deliberately not applied until both contexts are green (author action, item 2). Repository and security settings were verified live in the first pass; no settings file changed since. PR template guarded by `checkPrTemplate`, `labels.yml`, CodeQL and Scorecard workflows exist and run from main/schedule only |
| 5 | A PR fails CI if it changes `.planning/config.json` against the declared policy or touches governance files without the `governance-approved` label; the agent must ask before editing governance files or running GSD `config-set` | VERIFIED | Config policy: `checkGsdConfigPolicy` green. Ask rules: 54 `Edit(/...)` rules match the 54 governed entries one to one plus `Bash(*config-set*)`, `checkClaudeAskRules` green. Label gate: `test-check-pr.sh` 122 of 122; the file-list feeder fails closed (25 of 25) and the gate itself fails closed on an unreadable or incompletely read list (WR-05 reproduction exits 1). The residual that a PR can edit the gate that holds it (WR-06) is accepted in ADR-0003, and the sticky label (WR-02) is deferred by the author; neither is in the requirement text |

**Score:** 3/5 roadmap truths verified (SC1, SC2, SC5); SC3 and SC4 are code-verified and wait on the hosted run or author action. No truth is FAILED. 0 behavior-dependent truths are left unexercised: the fail-closed behaviour of the listing and of the gate is exercised by named shell cases (25 and 122 cases, run here).

Plan-level must-haves (the 20 PLAN frontmatter blocks) were read. Plans 01-18 to 01-20 add truths about reading the records through a status-checked temp file, the completeness guard, the widened Renovate and mise boundary, and the ADR; each locally checkable one is covered above. None reduces the roadmap criteria.

### Prohibitions (ADR-550 D3/D4, judgment tier)

All prohibitions are judgment-tier with `status: resolved`. Verdicts are NON-AUTHORITATIVE.

| Prohibition | Verdict | Note |
| --- | --- | --- |
| 01-15: MUST NOT let `governance-success` pass a PR whose complete changed-file list the gate did not evaluate; any failure to obtain or interpret the list fails the check | judgment: holds (the previous `unverified-prohibition` flag is cleared) | The WR-05 counter-example now fails closed, with or without the label and for the Renovate author (cases R5, R6). Two independent guards: jq status and `seen == count` |
| 01-15: MUST NOT add any way to pass without the label other than the Renovate version-only exemption; a list at the API limit is never exempt | judgment: holds | `check_files_api_limit` never consults the exemption; the unreadable-list failures return before the label or exemption is consulted |
| 01-16: MUST NOT leave any input on which one parser keeps a governed entry and the other drops, rewrites or rejects it | judgment: holds | One grammar, twin case tables in shell and `GovernedPathsFormatFunctionalTest`; the 24 entries added in plan 01-19 are plain printable ASCII and pass both parsers (the shell tests and `checkClaudeAskRules` are green) |
| 01-17/01-19: MUST NOT remove or narrow any original entry, ask rule or the `config-set` rule | judgment: holds | All original entries, `Bash(*config-set*)` still present; counts grew from 29 to 54 |
| 01-17/01-19: MUST NOT add an entry that makes a D-12 automerge flow need the label | judgment: holds with one documented cost | The three automerge rules (Actions digests, mise patch, formatter patch) still qualify for the version-only exemption. `.mise/locks/**` is disabled in Renovate's own rules, but a `npm:renovate` or `pipx:reuse` bump that rewrites a sidecar adds new files, which the exemption never covers, so that PR needs the label. The author accepted this (T-01-54) and a test pins it; recorded as human item 4 |
| 01-18/01-19/01-20: MUST NOT perform any GitHub-side write | judgment: holds | The commits of the gap plans touch repository files only |

### Deferred Items

None are deferred to a later phase. WR-02 (sticky approval label) and WR-06 (ADR-0003) are author-deferred review dispositions, not roadmap deferrals; later phases cover domain, frontend and observability and do not address them. Neither is a must-have of this phase.

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `scripts/governance/check-pr.sh` | Label gate, files API limit, shared grammar, status-checked records file, completeness guard | VERIFIED | Substantive (326 lines), wired from `governance.yml`, 122 cases pass |
| `scripts/governance/list-pr-files.sh`, `test-list-pr-files.sh` | Fail-closed listing and its fake-`gh` tests | VERIFIED | 25 passed, 0 failed |
| `scripts/governance/test-check-pr.sh` | Gate tests including R1-R11 and the boundary cases | VERIFIED | 122 passed, 0 failed |
| `governance/governed-paths.txt`, `.claude/settings.json` | 54 entries and 54 ask rules plus `config-set`, documented boundary and exclusions | VERIFIED | Counted here; `checkClaudeAskRules` green |
| `GovernedPaths.kt`, `AskRulesCheck.kt` | Kotlin side of the grammar | VERIFIED | Compiled and tested in the green build |
| `docs/adr/0003-governance-gate-trust-boundary.md` | Accurate record of the WR-06 residual; linked from ADR-0001 | VERIFIED | Read against the workflow, ruleset and settings; no misstatement of the risk found |
| `.github/workflows/governance.yml`, `ci.yml`, `codeql.yml`, `scorecard.yml`, `labels.yml`, `settings-drift.yml` | Pinned workflows | VERIFIED | actionlint and zizmor clean, all actions SHA-pinned |
| `.github/rulesets/main.json` | Declared ruleset | VERIFIED (declared, not applied) | Applying it is an author action (human item 2) |
| `renovate.json5`, `mise.toml`, `mise.lock`, `gradle/libs.versions.toml`, `platform/**`, `build-logic/**`, `.editorconfig`, `.gitattributes`, `REUSE.toml`, `lefthook.yml`, PR template, `labels.yml`, `docs/tooling.md` | Phase 1 foundation | VERIFIED | Exercised by the passing build and lint commands |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `governance.yml` `List changed files` | `list-pr-files.sh` | step `run:` with env contract | WIRED | Unchanged since the previous verification |
| `list-pr-files.sh` | `check-pr.sh` | validated array on `PR_FILES_JSON` | WIRED | Chain cases in `test-list-pr-files.sh` |
| `check-pr.sh` | `jq` producer of per-file records | `jq ... >"$RECORDS_FILE" \|\| jq_status=$?` then `seen`/`count` | WIRED | Was PARTIAL (WR-05); now status read and count asserted; reproduced failure exits 1 |
| `governed-paths.txt` | `check-pr.sh` (`load_globs`), `AskRulesCheck` (`GovernedPaths.parse`), `.claude/settings.json` | single list, one grammar | WIRED | 54 / 54 / 54 |
| `ci.yml` jobs | `ci-success` | `needs` + `if: always()` | WIRED | Unchanged |
| `.github/rulesets/main.json` | `ci-success`, `governance-success` | required contexts | WIRED (declared) | Not applied yet (author action) |
| ADR-0001 | ADR-0003 | cross-reference in Consequences | WIRED | Added by plan 01-20 |
| `lefthook.yml` | `scripts/format-staged.sh` / Spotless | `mise x --` | WIRED (static) | Not installed in a checkout yet (human item 5) |

### Data-Flow Trace (Level 4)

Not applicable: nothing renders dynamic data. The one pipeline that matters, PR file list to governance verdict, was traced end to end: `gh api` (status read) to `list-pr-files.sh` (validated, atomic) to `check-pr.sh` (validated shape, records file written with status read, count asserted, then the per-file loop). The previous weak point on that chain (WR-05) is closed.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Gate shell tests | `mise x -- bash scripts/governance/test-check-pr.sh` | 122 passed, 0 failed | PASS |
| Listing script tests | `mise x -- bash scripts/governance/test-list-pr-files.sh` | 25 passed, 0 failed | PASS |
| WR-05 reproduction (NUL in a governed entry's patch) | real `check-pr.sh` with a two-entry list, no label | `could not be read: jq exited with status 5`, exit 1, no temp file left | PASS (was FAIL in the previous report) |
| Governed list and ask rules in step | `grep -vE '^\s*(#\|$)' governed-paths.txt \| wc -l` / `grep -c 'Edit(/' .claude/settings.json` | 54 / 54, `checkClaudeAskRules` green | PASS |
| Whole build | `mise x -- ./gradlew build` | BUILD SUCCESSFUL, 35 tasks, configuration cache reused | PASS |
| Linters | `actionlint`, `shellcheck`, `zizmor --offline`, `editorconfig-checker`, `reuse lint` | all clean (147/147 REUSE) | PASS |

### Probe Execution

SKIPPED: the phase declares no `probe-*.sh` scripts and none exist under `scripts/*/tests/`.

### Requirements Coverage

All 25 IDs in the ROADMAP phase line appear in PLAN frontmatter and in REQUIREMENTS.md as Phase 1. No orphaned Phase 1 requirement exists (INFRA-15 maps to Phase 6).

| Requirement | Source Plan(s) | Status | Evidence |
| --- | --- | --- | --- |
| INFRA-01 | 01-01 | SATISFIED | Full build green |
| INFRA-09 | 01-01, 01-02, 01-03, 01-07, 01-11 | SATISFIED | Catalog, `.java-version`, `.nvmrc`, `mise.lock`, SHA pins; `checkBuildScripts`, `checkDaemonJvm` green |
| INFRA-10 | 01-01, 01-02, 01-07, 01-08, 01-12 | SATISFIED | Configuration cache reused, build-logic included build; hosted cache behaviour pending |
| INFRA-11 | 01-01, 01-02 | SATISFIED | `:platform` with the stack BOMs and Kotlin override |
| INFRA-12 | 01-02 | SATISFIED | Spotless, detekt, Dokka and TestKit apply to `build-logic` |
| INFRA-13 | 01-06, 01-12 | SATISFIED | `.gitattributes`; no non-LF index entries (previous pass) |
| INFRA-14 | 01-01, 01-04, 01-05, 01-06 | SATISFIED | `editorconfig-checker` exit 0 |
| INFRA-16 | 01-05, 01-06 | SATISFIED | `reuse lint` compliant 147/147; hosted registration pending (human item 6) |
| TEST-09 | 01-02, 01-04, 01-05, 01-08 | SATISFIED | Spotless tasks, lefthook, CI; commit-time behaviour is a human item |
| TEST-10 | 01-02 | SATISFIED | detekt `comments` rules and Dokka `failOnWarning` in `check` |
| TEST-13 | 01-02 | SATISFIED | detekt fails the build; the Biome half belongs to the frontend phase |
| CICD-01 | 01-12 | SATISFIED (code); hosted run pending | `ci.yml` |
| CICD-03 | 01-11, 01-12 | SATISFIED | Every `uses:` pinned; no container base images exist yet |
| CICD-04 | 01-11, 01-14 | NEEDS HUMAN | Config valid under `--strict`; Renovate reads it from `main` after the merge. Correctly Pending |
| CICD-06 | 01-08, 01-12 | SATISFIED | `secrets` job with full history; gitleaks clean (previous pass) |
| CICD-08 | 01-08, 01-12 | SATISFIED | actionlint, zizmor, shellcheck clean; the `docker compose config` part is moot (no compose file yet) |
| TOOL-01 | 01-03, 01-10 | SATISFIED | `docs/tooling.md`, `checkToolingInventory` |
| TOOL-02 | 01-03, 01-10, 01-11 | SATISFIED | `mise.toml` + `mise.lock`; Renovate extraction configured |
| GH-06 | 01-12, 01-13, 01-14 | NEEDS HUMAN | Ruleset deferred by the author; CodeQL/Scorecard first run after merge. Correctly Pending |
| GH-07 | 01-09, 01-13, 01-14 | SATISFIED | Squash-only, PR title/body, delete branches (verified live in the first pass); branch naming in `check-pr.sh` |
| GH-08 | 01-10 | SATISFIED | PR template and `checkPrTemplate` |
| GH-11 | 01-10 | SATISFIED | `labels.yml` and labels workflow |
| AIDEV-05 | 01-07 | SATISFIED | `checkGsdConfigPolicy` green; `.planning/config.json` equals the policy |
| AIDEV-06 | 01-09, 01-14 to 01-19 | SATISFIED | Label gate, fail-closed feeder and gate, widened boundary; hosted binding waits on the ruleset (GH-06). Accepted residuals: WR-02 (deferred), WR-06 (ADR-0003) |
| AIDEV-08 | 01-13, 01-16, 01-17, 01-19 | SATISFIED | 54 ask rules plus `config-set`; `checkClaudeAskRules` green |

Bookkeeping note for the orchestrator: REQUIREMENTS.md still shows `Gaps Found` in the traceability table for 22 of these IDs and unchecked boxes for most. That reflects the first verification and should be refreshed by whoever owns the state update; this report did not touch it. CICD-04 and GH-06 should stay `Pending` until the human items above are done.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `.github/workflows/governance.yml` | 8-10, 32-34 | WR-06: gate runs from the PR's own merge ref | ACCEPTED RESIDUAL (ADR-0003) | Documented, with reasons, rejected alternatives and revisit triggers. Not a gap |
| `scripts/governance/check-pr.sh` | 266-269 area (label check) | WR-02: approval label sticky, not bound to a commit | DEFERRED by the author | Later pushes to an approved PR still pass |
| `governance/governed-paths.txt` | `package.json` entry | IN-07: a future root `package.json` is governed as a whole file | INFO | Phase 7+ frontend work will need the label on every root `package.json` change. Revisit when the frontend lands |
| `GovernedPathsFormatFunctionalTest.kt` | 55, 84 | IN-06: literal NBSP and BOM in Kotlin source | INFO | Test fragility only |
| `check-pr.sh`, `ToolingTableCheck.kt`, `ci.yml`, `.gitattributes`, `docs/tooling.md` | various | IN-01 to IN-05 | INFO | Latent or cosmetic, unchanged from the first review |

No `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` markers in non-planning files (the only grep hits are `mktemp ...XXXXXX` templates, which are not markers). Review dispositions: CR-01, WR-01, WR-03, WR-04, WR-05, WR-07, WR-08 `fixed`; WR-02 and WR-06 `deferred`; IN-01 to IN-07 `open` (info only). `01-VALIDATION.md` still has `status: draft` (informational, not a goal criterion).

### UAT mapping

`01-UAT.md` tests 1 and 2 (the decisions on WR-05, WR-06, WR-07) are resolved by the code and ADR-0003 as described in the gap-closure table and should be marked passed. UAT tests 3 to 8 correspond to Human Verification items 1 to 6 below and stay pending.

### Human Verification Required

See the `human_verification` frontmatter list: the first hosted run of `ci.yml` and `governance.yml`, applying the deferred ruleset, post-merge CodeQL/Scorecard/label sync, Renovate onboarding PR #1 and first grouped PRs (including the expected label need on sidecar bumps), commit-time behaviour of the lefthook hook, and REUSE registration. These are hosted-run or author actions that cannot be exercised before the merge, not code defects.

### Gaps Summary

No code gap blocks the phase goal. Everything checkable in the repository is proven by running it: the full build is green with the configuration cache reused, 122 gate cases and 25 listing cases pass, the WR-05 counter-example now fails the gate, the governed list and the ask rules agree at 54 entries each, and every linter is clean. The remaining accepted residuals (WR-02, WR-06) are the author's recorded decisions. What is left is the first PR run on GitHub, the deferred ruleset, the post-merge security workflows, Renovate's first PRs, the hook's commit-time behaviour in a clone, and REUSE registration.

---

_Verified: 2026-11-21T23:30:00Z_
_Verifier: Claude (gsd-verifier)_
