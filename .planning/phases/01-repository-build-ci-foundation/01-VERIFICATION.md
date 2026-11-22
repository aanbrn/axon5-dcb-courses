---
phase: 01-repository-build-ci-foundation
verified: 2026-11-22T12:00:00Z
status: passed
score: 3/5 must-haves verified
covered_files:
  - .claude/settings.json
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
  - .planning/phases/01-repository-build-ci-foundation/01-21-PLAN.md
  - .planning/phases/01-repository-build-ci-foundation/01-21-SUMMARY.md
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPath.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt
  - docs/adr/0004-gsd-configuration.md
  - governance/governed-paths.txt
  - scripts/governance/check-pr.sh
  - scripts/governance/list-pr-files.sh
covered_digest: "v4:sha256:d12847a6d69b97cdf2cc52f9efa7233b1cda67daeaf1f4364b14549edf7e7ce3"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 3/5
  gaps_closed:
    - "G-01-2 (UAT test 2): a warm Gradle daemon resolved `node` from its startup PATH, so verifyNodeVersion could check a different binary than the one that formats. Fixed by plan 01-21 (ExecutableOnPath value source, absolute-path exec, Spotless nodeExecutable). Re-proven here by running the five new TestKit tests and a live warm-daemon reproduction, not by reading the SUMMARY"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Open the phase PR and let ci.yml and governance.yml run on GitHub (UAT test 7)"
    expected: "ci-success (build, lint, secrets jobs) and governance-success are green; the PR carries `governance-approved` because it touches governed files; the head branch name passes the naming rule; the `List changed files` step prints `governance: listed N changed files`. Specifically watch that the `build` job's Spotless and verifyNodeVersion steps pass on the hosted runner, where setup-node provides `node` on PATH and mise provides taplo and shfmt"
    why_human: "Nothing has run on GitHub yet beyond the author's applied repository and security settings. Hosted behaviour (mise-action, setup-java/setup-node, setup-gradle with the encrypted configuration cache, runner `jq` and `gh`, real files-API pagination, and now the ExecutableOnPath resolution on the runner's PATH) is only provable by the first run"
  - test: "After ci-success and governance-success are green on the phase PR, apply the ruleset (`scripts/apply-github-settings.sh --apply --only ruleset`), confirm the printed bypass actor is the repository admin role, delete the SETTINGS_DRIFT_SECTIONS variable, then run `--check` for all sections (UAT test 8)"
    expected: "Ruleset `main` is live with required contexts ci-success and governance-success bound to the GitHub Actions app (integration_id 15368), squash-only, linear history, no force-push or deletion, admin-only bypass; `--check` reports no drift"
    why_human: "The ruleset is deliberately deferred by the author until both contexts are green; applying it is an author action (GH-06, Success Criterion 4)"
  - test: "After the phase PR merges, confirm CodeQL (actions language) and OpenSSF Scorecard run from main and upload results, and that the labels workflow first sync yields exactly the labels in .github/labels.yml (UAT test 9)"
    expected: "Code scanning shows both analyses; labels in the repository equal labels.yml"
    why_human: "These workflows trigger on push to main or schedule; they cannot be exercised before the merge"
  - test: "Close Renovate onboarding PR #1 without merging, then after the merge confirm Renovate opens grouped PRs honouring `minimumReleaseAge: 7 days` for catalog, .java-version, mise.toml and Actions (UAT test 10)"
    expected: "No onboarding PR; update PRs are grouped per renovate.json5 and none appears before the 7-day cooldown (except the documented vulnerability exception and the JDK timestamp-optional waiver). Expect Renovate bumps of `npm:renovate` and `pipx:reuse` that rewrite a `.mise/locks/**` sidecar to need the `governance-approved` label (accepted in plan 01-19, T-01-54)"
    why_human: "Renovate reads renovate.json5 from main only after the merge; CICD-04 stays pending in REQUIREMENTS.md for this reason"
  - test: "Register the repository at api.reuse.software (UAT test 11)"
    expected: "The README REUSE badge shows `compliant`"
    why_human: "Registration sends the project name to a third party and is an author action; local `reuse lint` is already compliant (156/156 files)"
---

# Phase 1: Repository, Build & CI Foundation Verification Report

**Phase Goal:** A contributor can clone the repository, install pinned tools, and build and check the whole multi-project build (no domain code yet) with one command. Every commit and pull request is automatically formatted, analysed, licence-checked and governed.
**Verified:** 2026-11-22T12:00:00Z
**Status:** human_needed
**Re-verification:** Yes, after UAT (tests 1, 3-6 passed, test 2 raised G-01-2) and gap-closure plan 01-21

## Goal Achievement

No roadmap truth is FAILED and no code gap remains. The one gap raised by the author's UAT (G-01-2) is closed in code, and I re-proved the fix by running it (below). SC1, SC2 and SC5 are fully proven in the repository. SC3 and SC4 are code-complete, but they can only be finished by the first hosted run and by author actions that are still pending (UAT tests 7-11, all `blocked` on the phase PR and its merge). Those are hosted-run items, not code gaps.

Note on mode: ROADMAP.md marks Phase 1 `Mode: mvp`, but the roadmap goal is not written as "As a ..., I want to ..., so that ..." (`user-story.validate` returns `valid: false`). The MVP guard says to refuse and ask for `/gsd-mvp-phase 1`. I did not refuse, because the goal is unambiguous, the phase was verified twice before on the same terms, and 01-SKELETON.md states the end-to-end capability the user story would carry. I verified against the roadmap goal and its five success criteria and mapped the skeleton capability in the table below. If the author wants strict MVP framing, `/gsd-mvp-phase 1` should rewrite the goal; that is a planning-document fix, not a code gap.

### Gap-closure check (this round): G-01-2

| Claim (plan 01-21 / SUMMARY) | What I checked | Result |
| --- | --- | --- |
| `ExecutableOnPath` value source resolves an executable from `System.getenv("PATH")`, absolute entries only, regular executable file, absolute path or null | Read `ExecutableOnPath.kt` in full: `filter { it.isNotEmpty() && File(it).isAbsolute }`, `.map { File(it, name) }`, `.firstOrNull { it.isFile && it.canExecute() }?.absolutePath`; a null `PATH` yields null. 46 lines, KDoc present, a real `ValueSource` with `Parameters` | VERIFIED |
| `VerifyNodeVersion` runs the resolved absolute node, never a bare name | `execOperations.exec { commandLine(node, "--version") ... }` where `node` is `executable.orNull`; absent gives the existing `missingNode(null)`; messages and `parseMajor` unchanged; only the major goes to `node-major.txt` | VERIFIED |
| Spotless Prettier runs the same node | `courses.root.gradle.kts`: `nodePath.orNull?.let { prettier.nodeExecutable(it) }`, same `nodePath` that feeds `verifyNodeVersion` (`executable.set(nodePath)`); no `npmExecutable` (P-1) | VERIFIED |
| taplo and shfmt resolved by the same source; shell-based lookup gone | `executableOnPath("taplo"/"shfmt").orElse("")`; `grep 'executableOnPath("'` = 3; the only `commandLine(` calls in `build-logic/src/main` take the resolved path (`VerifyNodeVersion`, `VerifyFormatterBinary`); no `sh` or `node` bare launches; no `.exec {` in `courses.root` | VERIFIED |
| Only `ExecutableOnPath.kt` reads the environment | `grep -rl getenv build-logic/src/main` prints that one file | VERIFIED |
| The five TestKit tests, (a) to (e), pass; nine `FormattingFunctionalTest` tests unchanged and green | Ran `:build-logic:functionalTest --tests '*ExecutableOnPathFunctionalTest*' --tests '*FormattingFunctionalTest*' --rerun`: BUILD SUCCESSFUL; result XML: ExecutableOnPath 5 tests / 0 failures / 0 errors, Formatting 9 tests / 0 failures / 0 errors | VERIFIED |
| Warm-daemon behaviour (the UAT symptom) | Reproduced myself: warmed the daemon with `./gradlew help` (452 ms, daemon reused), put a `v22.0.0` shim first on `PATH`, ran `verifyNodeVersion`: failed with `Node.js 22 is active, but .nvmrc requires 24`, no daemon start notice, `Configuration cache entry reused`. Before the fix that same scenario reported Node 24 (UAT test 2). Shim directory removed afterwards | VERIFIED |
| `.nvmrc`, `.planning/config.json`, `node-major.txt` handling unchanged | `git diff 9581404 HEAD -- .nvmrc .planning/config.json` empty; `node-major.txt` still the one output, written with the major only | VERIFIED |
| `GradleFixture` stays within detekt's function limit | 11 member functions (limit 11); detekt green in `./gradlew build` | VERIFIED |

Behavioural proof for the cancel/ordering style claim ("a node switched on PATH between builds of one daemon is the node `verifyNodeVersion` checks") is a named test that ran and passed here (test (a), which also asserts all its builds ran in one daemon by PID), plus the manual reproduction above. So it is not left at presence-only.

Carried from earlier rounds and re-checked at HEAD: the governance chain (CR-01, WR-05, WR-07, WR-08) did not change in this round (no file under `scripts/governance/`, `governance/` or `.github/` is in the diff since the previous review base). `test-check-pr.sh` 122 passed, 0 failed; `test-list-pr-files.sh` 25 passed, 0 failed; 54 governed entries, 54 `Edit(/` ask rules, `Bash(*config-set*)` present.

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | `mise install` then `./gradlew build` succeeds with configuration cache, `build-logic` conventions (TestKit-tested), `:platform`, version catalog; every tool listed in `docs/tooling.md`; version literal or `.java-version`/daemon-JVM drift fails the build | VERIFIED | `mise x -- ./gradlew build spotlessCheck` at HEAD: BUILD SUCCESSFUL, configuration cache entry stored; the orchestrator's `--rerun-tasks --no-build-cache` run: 35/35 tasks executed, BUILD SUCCESSFUL. The 14 functional tests above ran green inside it. `checkToolingInventory`, `checkBuildScripts`, `checkDaemonJvm` are part of `check`. A literal fresh-clone run on a clean machine with network is the one thing not reproduced; it is covered by the first hosted run (human item 1) |
| 2 | One command formats every artifact type; pre-commit hook formats staged files; CI fails on formatting/`.editorconfig` violations, detekt, broken KDoc refs, missing SPDX header, non-normalised tree | VERIFIED | Spotless checks, detekt and Dokka `failOnWarning` run inside `build`. `editorconfig-checker` exit 0; `reuse lint`: compliant, 156 of 156 files; `actionlint` exit 0; `shellcheck scripts/*.sh scripts/*/*.sh` exit 0; `zizmor --offline` no findings (2 suppressed). Commit-time hook behaviour is now author-confirmed: UAT test 1 passed (format re-staged, gitleaks blocked a synthetic token, linters ran on matching staged files only). After 01-21, Prettier runs the verified node, so the formatter used in CI and hook is the pinned runtime |
| 3 | Every push and PR runs a workflow with SHA-pinned actions that builds, tests, analyses, lints workflows/scripts and scans full history; Renovate proposes grouped updates with minimum release age for every version source | UNCERTAIN (code verified, hosted run pending) | Workflows are unchanged since the last verification, where every `uses:` was confirmed pinned to a 40-character SHA; zizmor (which audits unpinned uses) and actionlint are clean now. UAT test 5 (author) passed on the declarations. Nothing has run on GitHub, so hosted behaviour and Renovate's real PRs are unproven (human items 1, 4) |
| 4 | `main` changes only through squash-merged PRs from `gsd/phase-*` branches that pass required checks; ruleset enforces linear history, blocks force-push, head branches deleted; PR template mirrors `/gsd-ship`; labels synced; secret scanning + push protection, Dependabot alerts, PVR, CodeQL, Scorecard active | UNCERTAIN (declared and partly live; ruleset deferred) | `.github/rulesets/main.json` and the repository settings JSON match the plan values (UAT test 5 passed); the ruleset is deliberately not applied until both contexts are green (author action, human item 2). PR template guarded by `checkPrTemplate`; `labels.yml`, CodeQL and Scorecard run from main/schedule only |
| 5 | A PR fails CI if it changes `.planning/config.json` against the declared policy or touches governance files without the `governance-approved` label; the agent must ask before editing governance files or running GSD `config-set` | VERIFIED | `checkGsdConfigPolicy`, `checkClaudeAskRules` green in `build`; 54 governed entries = 54 ask rules plus `config-set`; label gate 122/122 and feeder 25/25 (fail-closed cases included). UAT tests 3 and 4 (author) passed, including a live ask prompt on a governed `Edit`. Accepted residuals WR-06 (ADR-0003) and deferred WR-02 are the author's recorded decisions and are not in the requirement text |

**Score:** 3/5 roadmap truths verified (SC1, SC2, SC5); SC3 and SC4 are code-verified and wait on the hosted run or author action. No truth is FAILED. 0 behavior-dependent truths are left unexercised.

Plan-level must-haves (21 PLAN frontmatter blocks) were read. Plan 01-21 adds eight truths; all are covered in the gap-closure table or by the tests that ran. One scoping note: the 01-21 truth on test (d) and (e) states that these tests prove the value source in isolation and not the production wiring (finding F-1, author option B). I accept that framing, since it is stated openly, and I confirmed the production wiring separately: lazy `nodePath` for the guard, tests (a) to (c) exercise it end to end.

### User Flow Coverage (skeleton capability, 01-SKELETON.md)

| Step | Expected | Evidence | Status |
| --- | --- | --- | --- |
| Clone, `mise install` | pinned tools from `mise.toml` + `mise.lock`, JDK from `.java-version`, Node from `.nvmrc` | `mise x -- ...` resolves every tool used above; UAT tests 22-25 passed | VERIFIED |
| `./gradlew build` | one command builds and checks | BUILD SUCCESSFUL, 35 tasks | VERIFIED |
| Commit through the hook | format, gitleaks, reuse, linters | UAT test 1 passed | VERIFIED |
| PR passes `ci-success` and `governance-success`; squash-merge under the ruleset | hosted run, ruleset applied | not yet run | HUMAN (items 1, 2) |

### Prohibitions (ADR-550 D3/D4, judgment tier)

All prohibitions are judgment-tier with `status: resolved`. Verdicts are NON-AUTHORITATIVE.

| Prohibition | Verdict | Note |
| --- | --- | --- |
| 01-21: MUST NOT rely on the JDK's own `PATH` lookup for any process build-logic launches | judgment: holds | Both `commandLine(` calls in `build-logic/src/main` take an absolute path from `ExecutableOnPath`; no bare `node` or `sh` launch remains. Spotless's own launches are covered by `nodeExecutable` |
| 01-21: MUST NOT read `PATH` at configuration time outside the value source | judgment: holds | Only `ExecutableOnPath.kt` mentions `getenv` in main sources |
| 01-21: MUST NOT edit `.nvmrc`, change failure messages, or add the node path to `node-major.txt` | judgment: holds | `.nvmrc` and config diff empty; both messages read as before; `node-major.txt` still holds the major only |
| 01-21: MUST NOT weaken or skip a test, add `@Suppress`, a detekt baseline or config change, or disable the configuration cache or isolated projects | judgment: holds | No `@Suppress`, baseline or detekt config in the diff (six files changed, none of them config); `build` ran with the configuration cache and isolated projects on |
| 01-15 to 01-20 prohibitions (governance gate fail-closed, one grammar, no narrowing, no GitHub-side writes) | judgment: holds | Unchanged from the previous report; re-run gate tests 122/122 and 25/25, counts 54/54 |

### Deferred Items

None are deferred to a later phase. WR-02 (sticky approval label) and WR-06 (ADR-0003) are author-deferred review dispositions, not roadmap deferrals.

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `build-logic/.../ExecutableOnPath.kt` | Value source over the build's PATH | VERIFIED | Substantive, wired from `courses.root` (3 call sites), tested |
| `build-logic/.../VerifyNodeVersion.kt` | `executable` input, absolute exec | VERIFIED | Read in full |
| `build-logic/src/main/kotlin/courses.root.gradle.kts` | One resolution fed to guard and Spotless | VERIFIED | `nodePath`, `executableOnPath` x3, `nodeExecutable` |
| `ExecutableOnPathFunctionalTest.kt` | Tests (a) to (e) | VERIFIED | 5 tests ran, 0 failures |
| `GradleFixture.kt` | `path` parameter, at most 11 functions | VERIFIED | 11 functions, detekt green |
| Governance, workflows, ruleset, renovate, mise, catalog, platform, conventions | Phase 1 foundation | VERIFIED | Exercised by the passing build and lint commands; unchanged since the previous report |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `courses.root.gradle.kts` | `ExecutableOnPath.kt` | `executableOnPath(name)` wrapping `providers.of(ExecutableOnPath::class.java)` | WIRED | 3 uses |
| `courses.root.gradle.kts` | `VerifyNodeVersion.kt` | `executable.set(nodePath)` | WIRED | Live reproduction shows the guard acts on the first `node` on the current build's PATH |
| `courses.root.gradle.kts` | Spotless Prettier | `prettier.nodeExecutable(nodePath)` | WIRED | Test (b) asserts Prettier's `serve.js` ran the shim the guard checked |
| `verifyNodeVersion` output | Prettier tasks | `inputs.files(verifyNodeVersion.flatMap { it.output })` | WIRED | Unchanged |
| Governance chain (`governance.yml` to `list-pr-files.sh` to `check-pr.sh`, `governed-paths.txt` to ask rules) | | | WIRED | Unchanged; gate tests green |

### Data-Flow Trace (Level 4)

Not applicable to rendered data. The trace that matters in this round: `System.getenv("PATH")` of the current build, to `ExecutableOnPath`, to `nodePath`, to both `verifyNodeVersion` and Spotless `nodeExecutable`. It ends in a real environment read (not a static value), confirmed by the live v22 shim.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| New and existing functional tests | `mise x -- ./gradlew :build-logic:functionalTest --tests '*ExecutableOnPathFunctionalTest*' --tests '*FormattingFunctionalTest*' --rerun` | 5 + 9 tests, 0 failures | PASS |
| Warm-daemon node switch (UAT test 2) | `./gradlew help`, then `PATH=<v22 shim>:$PATH ./gradlew verifyNodeVersion` | `Node.js 22 is active, but .nvmrc requires 24`, daemon reused | PASS |
| Whole build | `mise x -- ./gradlew build spotlessCheck` | BUILD SUCCESSFUL | PASS |
| Gate shell tests | `bash scripts/governance/test-check-pr.sh`, `test-list-pr-files.sh` | 122 / 25 passed, 0 failed | PASS |
| Linters | `editorconfig-checker`, `reuse lint`, `actionlint`, `shellcheck`, `zizmor --offline` | all clean (REUSE 156/156) | PASS |

### Probe Execution

SKIPPED: the phase declares no `probe-*.sh` scripts and none exist under `scripts/*/tests/`.

### Requirements Coverage

All 25 IDs in the ROADMAP phase line appear in PLAN frontmatter and in REQUIREMENTS.md as Phase 1. No orphaned Phase 1 requirement exists.

| Requirement | Source Plan(s) | Status | Evidence |
| --- | --- | --- | --- |
| INFRA-01 | 01 | SATISFIED | Full build green |
| INFRA-09 | 01, 02, 03, 07, 11, 21 | SATISFIED | Catalog, `.java-version`, `.nvmrc`, `mise.lock`; after 01-21 the node checked against `.nvmrc` is the node used |
| INFRA-10 | 01, 02, 07, 08, 12, 21 | SATISFIED | Configuration cache reused, isolated projects on; ExecutableOnPath invalidates precisely (tests (d), (e)); hosted cache behaviour pending |
| INFRA-11 | 01, 02 | SATISFIED | `:platform` with the stack BOMs and Kotlin override |
| INFRA-12 | 02 | SATISFIED | Spotless, detekt, Dokka and TestKit apply to `build-logic` |
| INFRA-13 | 06, 12 | SATISFIED | `.gitattributes`; normalisation in CI lint |
| INFRA-14 | 01, 04, 05 | SATISFIED | `editorconfig-checker` exit 0 |
| INFRA-16 | 05, 06 | SATISFIED | `reuse lint` compliant 156/156; hosted registration pending (human item 5) |
| TEST-09 | 02, 04, 05, 08, 21 | SATISFIED | Spotless tasks, lefthook (UAT test 1 passed), CI; formatter runs the verified node |
| TEST-10 | 02 | SATISFIED | detekt `comments` rules and Dokka `failOnWarning` in `check` |
| TEST-13 | 02 | SATISFIED | detekt fails the build; the Biome half belongs to the frontend phase |
| CICD-01 | 12 | SATISFIED (code); hosted run pending | `ci.yml` |
| CICD-03 | 11, 12 | SATISFIED | Actions pinned; no container base images yet |
| CICD-04 | 11, 14 | NEEDS HUMAN | Config valid under `--strict`; Renovate reads it from `main` after the merge. Correctly Pending |
| CICD-06 | 08, 12 | SATISFIED | `secrets` job with full history; gitleaks blocked the synthetic token in UAT test 1 |
| CICD-08 | 08, 12 | SATISFIED | actionlint, zizmor, shellcheck clean; the compose-config part is moot (no compose file yet) |
| TOOL-01 | 03, 10 | SATISFIED | `docs/tooling.md`, `checkToolingInventory` |
| TOOL-02 | 03, 10, 11 | SATISFIED | `mise.toml` + `mise.lock`; Renovate extraction configured |
| GH-06 | 12, 13, 14 | NEEDS HUMAN | Ruleset deferred by the author; CodeQL/Scorecard first run after merge. Correctly Pending |
| GH-07 | 09, 13, 14 | SATISFIED | Squash-only, PR title/body, delete branches (verified live earlier); branch naming in `check-pr.sh` |
| GH-08 | 10 | SATISFIED | PR template and `checkPrTemplate` |
| GH-11 | 10 | SATISFIED | `labels.yml` and labels workflow |
| AIDEV-05 | 07 | SATISFIED | `checkGsdConfigPolicy` green |
| AIDEV-06 | 09, 14 to 20 | SATISFIED | Label gate and fail-closed feeder/gate; hosted binding waits on the ruleset (GH-06) |
| AIDEV-08 | 13, 16, 17, 19 | SATISFIED | 54 ask rules plus `config-set`; live ask prompt seen in UAT test 3 |

Bookkeeping note for the orchestrator: the REQUIREMENTS.md traceability table still shows `Gaps Found` for 19 of the 25 IDs and unchecked boxes for 21 (only INFRA-09, INFRA-10, TEST-09, AIDEV-06, AIDEV-08 are `Complete`). That reflects the first verification and should be refreshed by whoever owns the state update; this report did not touch it. CICD-04 and GH-06 should stay `Pending` until the human items are done.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `docs/adr/0004-gsd-configuration.md` | 32-33 | WR-09 (open): says the ask rules "make the author confirm both routes", repeating the WR-08 overclaim; shell commands such as `sed -i` are not matched | WARNING (documentation, not a requirement) | The ADR is outside the roadmap criteria, but it presents an agent-side control as stronger than it is. Reword per `01-REVIEW.md` WR-09 (and it should say `checkGsdConfigPolicy` and the PR review remain the controls). Does not block the phase goal |
| `.github/workflows/governance.yml` | 8-10, 32-34 | WR-06: gate runs from the PR's own merge ref | ACCEPTED RESIDUAL (ADR-0003) | Documented. Not a gap |
| `scripts/governance/check-pr.sh` | label check | WR-02: approval label sticky | DEFERRED by the author | Later pushes to an approved PR still pass |
| `ExecutableOnPath.kt` | 19-22 | IN-08: KDoc says Gradle moves the daemon's directory to "the directory of each build"; the test shows it is the client's working directory | INFO | KDoc wording only; the relative-entry rule itself is correct and tested |
| `ExecutableOnPathFunctionalTest.kt` | 79-98 | IN-09: test (b) does not force a warm daemon, so its red state depends on the shared TestKit daemon's history | INFO | Repeatability of one regression test; it passes now and test (a) covers the warm-daemon half deterministically |
| `VerifyNodeVersion.kt`, `VerifyFormatterBinary.kt` | | IN-10: duplicated "run --version, fail with `mise install`" mechanism | INFO | Cosmetic |
| `governed-paths.txt`, `check-pr.sh`, others | various | IN-01 to IN-07 | INFO | Latent or cosmetic, unchanged |

No `TBD`, `FIXME`, `XXX`, `TODO` or `HACK` markers in the build-logic, docs, scripts and governance sources I scanned (the only `XXXXXX` text would be `mktemp` templates, which I excluded). Review dispositions now: 11 of 20 open (WR-09 and IN-01 to IN-10 minus the carried ones), none critical; WR-02 and WR-06 deferred; all earlier criticals and warnings fixed except WR-09. `01-VALIDATION.md` still has `status: draft` (informational).

The `verify:post` capability steps (validate-phase, secure-phase, ui-review) were not dispatched in this run; the author runs them as separate commands. That is a workflow note, not a code gap.

### Human Verification Required

See the `human_verification` frontmatter list: the first hosted run of `ci.yml` and `governance.yml` (UAT test 7), applying the deferred ruleset (test 8), post-merge CodeQL/Scorecard/label sync (test 9), Renovate onboarding PR #1 and first grouped PRs (test 10), and REUSE registration (test 11). The earlier "commit-time hook behaviour" item is closed by UAT test 1. These are hosted-run or author actions that cannot be exercised before the merge, not code defects.

### Gaps Summary

No code gap blocks the phase goal. G-01-2 is closed and independently re-proven: the five new TestKit tests and the nine existing formatting tests pass, a live warm-daemon reproduction now detects a switched Node, every launched program in build-logic is an absolute path found from the build's own PATH, and the full build and every linter are clean. What is left is the first PR run on GitHub, the deferred ruleset, the post-merge security workflows, Renovate's first PRs, and REUSE registration, plus the open documentation warning WR-09 that the author can fix with one rewording.

---

_Verified: 2026-11-22T12:00:00Z_
_Verifier: Claude (gsd-verifier)_
