---
gsd_state_version: "1.0"
current_phase: 02
current_phase_name: Runtime Infrastructure & DCB Spikes
status: executing
stopped_at: Phase 2 context gathered
last_updated: "2026-11-25T01:44:29.686Z"
last_activity: 2026-11-25
last_activity_desc: Phase 02 execution started
state_head: e4db42e690154d5163ae936004fd15fcdfe1a1d5
progress:
  total_phases: 13
  completed_phases: 1
  total_plans: 44
  completed_plans: 21
  percent: 8
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-11-22)

**Core value:** Show convincingly, in working, tested and documented code, how DCB enforces business invariants that span multiple entities without aggregates, and why that beats the aggregate-based approach.
**Current focus:** Phase 02 — Runtime Infrastructure & DCB Spikes

## Current Position

Phase: 02 (Runtime Infrastructure & DCB Spikes) — VERIFYING
Plan: 23 of 23 executed (all SUMMARYs present)
Status: Phase 02 verification human_needed (5/5 success criteria verified, re-verified 2026-11-24 at 6d81294); 02-UAT.md has 6 pending human items; phase PR #21 is open as a draft
Last activity: 2026-11-25 - Completed quick task 261125-bha: build-logic TestKit suite moved into a parallel functional-test CI job; Phase 2 verification stale until re-run

Progress: [█░░░░░░░░░] 8%

## Performance Metrics

**Velocity:**
- Total plans completed: 21
- Average duration: -
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01 | 21 | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 01 P01 | 7 min | 2 tasks | 17 files |
| Phase 01 P02 | 14 min | 2 tasks | 11 files |
| Phase 01 P03 | 25 min | 3 tasks | 8 files |
| Phase 01 P04 | 12min | 2 tasks | 9 files |
| Phase 01 P05 | 12 min | 2 tasks | 6 files |
| Phase 01 P06 | 14min | 2 tasks | 9 files |
| Phase 01 P07 | 20 min | 2 tasks | 10 files |
| Phase 01 P09 | 7 min | 2 tasks | 4 files |
| Phase 01 P08 | 22 min | 2 tasks | 6 files |
| Phase 01 P10 | 45 min | 3 tasks | 8 files |
| Phase 01 P11 | 8 min | 2 tasks | 2 files |
| Phase 01 P12 | 24 min | 3 tasks | 5 files |
| Phase 01 P13 | 22 min | 3 tasks | 10 files |
| Phase 01 P14 | 10 min | 3 tasks | 0 files |
| Phase 01 P15 | 5 min | 3 tasks | 6 files |
| Phase 01 P16 | 12 min | 2 tasks | 6 files |
| Phase 01 P17 | 15 min | 2 tasks | 4 files |
| Phase 01 P18 | 24 min | 2 tasks | 3 files |
| Phase 01 P19 | 6 min | 2 tasks | 4 files |
| Phase 01 P20 | 3 min | 2 tasks | 3 files |
| Phase 01 P21 | 75min | 2 tasks | 5 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Foundation split into Phase 1 (repository, build, CI, governance; no runtime) and Phase 2 (runtime infrastructure, test infrastructure, DCB spikes)
- [Roadmap]: Per-student limit (SUBS-03) is in the command walking skeleton, so the first subscribe decision already spans a course and a student
- [Roadmap]: Schedules (Phase 8) come before transfer (Phase 9) and the waiting list (Phase 10); decision records are stamped from Phase 3
- [Roadmap]: Idempotency and resilience land where first needed (P3, P4, P5, P6, P7); resilience tests, load test and signed releases come in Phase 12
- [Roadmap]: Cross-cutting conventions (ROADMAP.md) carry standing obligations such as ADRs, Event Modeling per slice, test naming and UI-SPEC per UI phase
- [Phase 01]: Kotlin override is the only api constraint in :platform in Phase 1 (api(libs.kotlin.stdlib), catalog Kotlin 2.4.21 over Boot-managed 2.3.21); later non-BOM libraries and pins get their own catalog-referenced api constraints
- [Phase 01]: Gradle dependency locking and verification-metadata.xml deferred to Phase 6 (CICD-05); Jackson BOM pinned to 3.1.8 as declared by spring-boot-dependencies 4.1.3
- [Phase 01]: detekt analyses the functionalTest source set; test classes (*FunctionalTest.kt) get detekt's default test exclusions for FunctionNaming and the undocumented-function/property rules, GradleFixture stays fully checked — detekt's built-in test exclusions match **/test/** only; prose test names are the project convention; no baseline or disabled rules
- [Phase 01]: TestKit fixtures run :lib tasks, never root build/check, and use the repository's real :platform — Root check reaches build-logic check and would recurse into functionalTest; an empty platform leaves test dependencies unversioned
- [Phase 01]: npm:renovate installed with one exact-version trust-policy exception (trust_policy_excludes = [@yarnpkg/libzip@3.2.2]), approved by the author; trust policy not disabled globally — libzip 3.2.2 was published manually by the Yarn lead maintainer without provenance (3.2.0 had it); remove the exception when renovate no longer pulls it
- [Phase 01]: mise.lock enforced with locked = true; .mise/locks sidecar lockfiles are committed with it — mise.lock references the npm and pipx sidecar lockfiles; without them a fresh clone cannot verify those tools
- [Phase 01]: Markdown leaves indent_size unset in .editorconfig because Prettier aligns ordered-list continuation lines to the three-column marker
- [Phase 01]: editorconfig-checker excludes mise.lock and .mise/locks as generated lockfiles
- [Phase 01]: Missing taplo is replaced by an existing placeholder file at configuration time so the Spotless native step can be stored in the configuration cache; VerifyFormatterBinary then fails the TOML tasks with a mise install hint
- [Phase 01]: Formatter binary checks are a separate upstream task (VerifyFormatterBinary), not a doFirst, because Spotless fingerprints its step state before doFirst runs; the binary path and version output are inputs of the Spotless tasks of that type
- [Phase 01]: REUSE: .planning/** gets an override annotation because planning prose quotes SPDX header text; SPDX text in code string literals is fenced with REUSE-IgnoreStart/End
- [Phase 01]: Governance config-policy report lists key paths only, never values (config files can hold credentials) — Gradle output reaches CI logs; checkGsdConfigPolicy still names every added, removed or changed key
- [Phase 01]: Governance TestKit tests assert task outcome in {SUCCESS, FROM_CACHE} via GradleFixture.PASSED — TestKit shares the user's Gradle home, so the build cache can answer an identical fixture build
- [Phase 01]: Plan 01-09: renovate[bot] exemption classifies patch lines by position after the first hunk header (no three-dash exclusion) and collapses release asset ids in mise.lock; same-repository PR head refs must be gsd/phase-*, renovate/* or release-please--*
- [Phase 01]: Plan 01-08: gradle-user-home and profile-out* are excluded from the Spotless source filter (gradle-profiler writes them into the project); lefthook is exercised with run --no-auto-install only, because a plain run installs the hook into the shared git directory
- [Phase 01]: Label descriptions for phase:1 to phase:13 carry the real roadmap phase names; community labels stay deferred to Phase 13
- [Phase 01]: checkToolingInventory reads mise.toml and docs/tooling.md with zero-dependency line readers (no TOML library); keys are compared verbatim including backend prefix
- [Phase 01]: Renovate java rule uses minimumReleaseAgeBehaviour timestamp-optional — The java-version datasource reports no release timestamps, so the default timestamp-required behaviour leaves every JDK bump pending; only LTS majors pass allowedVersions and the PR is a manual merge.
- [Phase 01]: Renovate security PRs bypass the cooldown but never automerge — Renovate forces a zero cooldown for vulnerability alerts; vulnerabilityAlerts sets automerge false so a human always merges them, keeping the three-rule D-12 automerge set narrow.
- [Phase 01]: ktfmt version in the catalog carries a Renovate annotation; .mise/locks sidecar lockfiles are ignored — ktfmt has no library alias so no manager extracted it; the generated sidecar package.json and pyproject.toml would otherwise get PRs.
- [Phase 01]: Plan 01-12: MISE_DISABLE_TOOLS=java,node is set at job level and the build job uses plain ./gradlew build with mise-action shims on PATH — Job-level env reaches the shims Gradle and lint steps invoke later; mise x without tool arguments would install every tool in mise.toml
- [Phase 01]: Plan 01-12: CodeQL ships the actions language only; java-kotlin deferred after a negative local spike — Two extraction attempts saw no Kotlin source on macOS arm64 (and --no-configuration-cache is rejected by Isolated Projects); ADR-0002 records evidence and re-enable criteria
- [Phase 01]: Plan 01-13: ruleset equality projects the live document onto the keys declared in main.json, and the main ruleset bypass actor is RepositoryRole 5 with mode pull_request (confirm via the apply read-back in plan 01-14)
- [Phase 01]: Plan 01-14: author approved apply-without-ruleset; repository and security settings, governance-approved label and GRADLE_ENCRYPTION_KEY are live, SETTINGS_DRIFT_SECTIONS=repository security, ruleset main deferred until ci-success and governance-success are green on the phase PR
- [Phase 01]: 01-15: a file list at the 3000-entry files API limit needs the governance-approved label instead of failing unconditionally, and never takes the renovate exemption
- [Phase 01]: 01-16: governed-paths grammar shared by check-pr.sh load_globs and GovernedPaths.parse; NUL bytes converted with tr so both parsers fail closed — bash read drops NUL silently while Kotlin keeps it; any input one parser keeps and the other drops must fail closed in both (WR-04)
- [Phase 01]: Governed-path list covers the enforcement surface (29 entries); version catalog, gradlew.bat, module conventions and tests stay outside, documented in the file
- [Phase 01]: 01-18: NUL strings are not rejected up front; the check-pr.sh reading fails closed through a checked jq status and an entry-count guard (WR-05)
- [Phase 01]: Plan 01-19: governed every Renovate config name and every mise config, rc and sidecar-lock location CI reads (54 entries); the renovate[bot] version-only exemption stays unchanged and its sidecar cost is a recorded follow-up
- [Phase 01]: WR-06 accepted as a residual for a one-maintainer repository and recorded in ADR-0003: governance.yml keeps pull_request and the merge-ref checkout; revisit on a second maintainer or external contributors
- [Phase 01]: 01-21: Spotless gets nodeExecutable only; node, taplo and shfmt resolved by ExecutableOnPath over the build's PATH, relative and empty entries skipped; production wiring stays lazy and the value source's cache precision is proven in fixture scripts (F-1)
- [Quick 261125-bha]: D-20 extended at the author's request: CI runs the build-logic TestKit suite in a third parallel job `functional-test` (needed by `ci-success`); the `build` job excludes it with `-x :build-logic:functionalTest` and still names `checkDependencyLicences`; local `check` is unchanged (D-19)

### Pending Todos

- [First ID-keyed map in a contract]: Decide the JSON key form of an ID-keyed map (today jackson-module-kotlin writes the raw UUID and rejects the TypeID form; the TypeID form needs a key serializer and a key deserializer) and record it in an ADR. `CoursesJsonModule` KDoc and `JsonRoundTripTest` pin the current behaviour (review WR-02, quick task 261124-oyk).
- [Renovate, from 2026-11-28]: Merge the open Renovate PRs by hand (automerge is off for them). #19 now updates `npm:renovate` to 44.257.9 (published 2026-11-21, so the 7-day npm cooldown, CICD-04, ends 2026-11-28) and `uv` to 0.13.9; it needs the `governance-approved` label, because the content of the renovate sidecar `aube-lock.yaml` changes, not only versions. #20 lock file maintenance has every check green except `renovate/stability-days`. Plan: merge both after PR #21 merges (Renovate rebases them); if Phase 2 runs past 2026-11-28, merge them earlier and then merge main into the phase branch.
- [Phase 6, CICD-02 acceptance]: Run `./gradlew bootBuildImage` and the web image task on the containerd image store, on an engine of Docker 29.9.0 or newer, as an acceptance check of CICD-02, and require the image job to run on such an engine or on the classic store (review IN-02 of 2026-11-25). The UP-0012 removal rests on an export observed on Docker 29.10.0; the 29.5.2 control did not reproduce the defect, and the project's own image build has never run on the containerd store. Locally, `doctor` warns for the containerd store below 29.9.0 (quick task 261125-a09). When CICD-02 is planned, add the check to its acceptance criteria and cite it from the decision paragraph of docs/upstream/0012-docker-containerd-platform-export.md.
- [Before Phase 3, CI and local speed]: Parallelise the build-logic TestKit suite itself: switch the fixtures from `includeBuild` of the real build-logic (`GradleFixture.writeSettings`) to `withPluginClasspath()` or a pre-built plugin, then set `maxParallelForks > 1` on `functionalTest` in `build-logic/build.gradle.kts` (governed). Expected about 9 -> 3-4 minutes, locally and in the CI `functional-test` job (quick task 261125-bha moved the suite into that parallel job).
- [Phase 4 discuss]: Choose the documentation-site tool. Docusaurus 3.10.2 with Mermaid is only a research recommendation (`.planning/research/STACK.md`, SUMMARY.md); PROJECT.md Key Decisions fixes only "docs site as a Gradle subproject" (Pending), and no ADR exists. Needed by INFRA-08 (docs-site skeleton as a Gradle subproject, Phase 4/5), GH-04 (GitHub Pages publication, Phase 8, with Storybook per UI-08), UPS-02's index on the site (Phase 5) and DOC-02 (final docs phase). Record the choice in an ADR and PROJECT.md.


- [Quick, after the Phase 2 PR merges]: Pull forward the GitHub presentation items that do not depend on the UI (author decision 2026-11-24), as quick tasks: (1) GH-01 repository metadata as code without the social preview image and homepage (description, topics, unused features disabled; applied via `gh` after author approval); (2) GH-03 community health files (CONTRIBUTING, CODE_OF_CONDUCT, SUPPORT, CODEOWNERS, SECURITY.md with the stated policy); (3) GH-13 `CITATION.cff`; (4) an honest work-in-progress README with CI, OpenSSF Scorecard and licence badges (the full showcase README, GH-02/DOC-01, stays in Phase 13). Update REQUIREMENTS.md traceability for each item delivered (GH-01 becomes partial until the social preview and homepage land in Phase 13). While there, reconcile the GH-04 phase: REQUIREMENTS.md says Phase 7, the Phase 4 discuss todo above says Phase 8.

### Blockers/Concerns

- [Phase 2]: Unlicensed Axon Server behaviour (12-hour shutdown vs 14-day limit, restart effect) may affect the `docker compose up` quick start and CI
- [Phase 2/3]: Decision-record capture relies on an `@Internal` Axon SPI; fallback is explicit per-slice stamping plus an architecture rule. The AxonFramework maintainer (#5326, UP-0001) states `@Internal` means "may change", not experimental, and `EventStorageEngine` stays in Axon Framework 5
- [Phase 8/10]: `EventCriteria` expressiveness for schedule overlap and the waiting-list head is unverified; fallback is passing candidates in the command and re-validating in the decision
- [Phases 2, 13]: Third-party skills/MCP servers and upstream issue publication need explicit author approval at execution time (Phase 1 repository settings and the `main` ruleset are applied)
- [Phase 3]: Review WR-01 deferred: conflict retry is fixed at 10 attempts with uncapped, unjittered back-off, so >~11 contenders on one open course end in raw `AppendEventsTransactionRejectedException`. Phase 3 SC2 must make the bound configurable, add capped full-jitter back-off and a 40-way concurrent subscription test. Upstream input (UP-0003, axoniq-framework#811): a dispatch-side `RetryPolicy` can match `AppendEventsTransactionRejectedException` in the `CommandExecutionException` message; the rejection is non-transient on purpose and only re-dispatching the command is retryable; weigh this against the handling-side decorator when the retry becomes configurable
- [Phase 3]: Qodana `UnusedSymbol` (profile Default, threshold 0) will likely flag production `@CommandHandler`/`@EventSourcingHandler` methods reached only through annotation scanning; choose the approach (explicit registration, entry-point configuration, or other) before the first slice, without suppressions
- [Optional hardening]: make `scripts/qodana.sh` refuse a `qodana.yml` (the Qodana CLI reads it before `qodana.yaml`; the path is governed since 261124-gju); once a `package.json` lands, decide on `minimumReleaseAgePackageManager: "7 days"` for the npm manager
- [Upstream]: Author decided (2026-11-24) to contribute pull requests to AxonIQ/AxonFramework after the Phase 2 PR: first #5326 (UP-0001) as an opt-in decorator definition, the public way to observe the conditional append that lets the command service drop its decorator of the `@Internal` `EventStorageEngine` (5.5.0 milestone); then #5328 (UP-0004, Kotlin-aware tag resolver, upstream milestone 5.7.0), starting with where the code belongs (core or the Kotlin extension). Work in a fork cloned outside this repository; show the author the diff and PR text before `gh pr create`; update the UP records on merge and remove workarounds only after a release
- [Tooling]: GSD hooks fail in Claude Code sessions started inside a git worktree (hooks resolve under `$CLAUDE_PROJECT_DIR`, the worktree has no `.claude/hooks/`); deferred follow-up from UAT test 3

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 261121-wjt | ADR-0004 on GSD configuration and COVERAGE.md | 2026-11-21 | c06f777 | [261121-wjt-adr-0004-on-gsd-configuration-and-covera](./quick/261121-wjt-adr-0004-on-gsd-configuration-and-covera/) |
| 2 | Fix CI Lint: pin python 3.14.10 and uv 0.13.8 in mise so pipx:reuse installs from its lock (e1a038d) | 2026-11-22 | e1a038d | — |
| 3 | Settings drift: report repository fields hidden from the read-only token as warnings, not drift (945db3e) | 2026-11-22 | 945db3e | — |
| 4 | Record phase 01 post-merge UAT tests 9-11 as passed (CodeQL/Scorecard/labels, Renovate, REUSE) | 2026-11-22 | 8d3f828 | — |
| 5 | Guard .mise in .gitignore: track only the sidecar lockfiles under .mise/locks | 2026-11-22 | 1527882 | — |
| 261122-gdy | Raise the maximum line width from 100 to 120 for every formatter (detekt backstop 140) | 2026-11-22 | d1a0fb8 | [261122-gdy-raise-the-maximum-line-width-from-100-to](./quick/261122-gdy-raise-the-maximum-line-width-from-100-to/) |
| 7 | Renovate governance exemption covers version-only moves of mise sidecar lockfiles | 2026-11-22 | 7ab7f48 | — |
| 8 | Record the agent git workflow (branches, no force-push, worktree finishing) in .claude/CLAUDE.md | 2026-11-22 | 6caa5b7 | — |
| 9 | Record the test class suffix convention (suffix mirrors the Gradle test suite) | 2026-11-22 | 18dc009 | — |
| 10 | Suppress incubating-API warnings with reasons, describe environment check tasks, plan gates for suppressions and IDE-only inspections | 2026-11-22 | 90339a9 | — |
| 261122-kqn | Adopt Qodana JVM Community as a required CI gate for IDE-only inspections | 2026-11-22 | fc88954 | [261122-kqn-adopt-qodana-as-a-required-ci-gate-for-i](./quick/261122-kqn-adopt-qodana-as-a-required-ci-gate-for-i/) |
| 261123-mub | Close pre-ship debt from interim audit: govern Qodana files, derive compose image check from catalog, WR-05 assertion, requirements bookkeeping | 2026-11-23 | 6d87e5a | [261123-mub-close-pre-ship-debt-from-interim-audit-g](./quick/261123-mub-close-pre-ship-debt-from-interim-audit-g/) |
| 261123-p1u | Fix all 45 Qodana findings on the Phase 2 branch without suppressions or baseline (Qodana confirmed 0 after 261123-rzt) | 2026-11-23 | 32bf6fa | [261123-p1u-fix-all-45-qodana-findings-on-the-phase-](./quick/261123-p1u-fix-all-45-qodana-findings-on-the-phase-/) |
| 261123-rzt | Fix Phase 2 review findings WR-02/03/04/06, IN-01..04 and the 4 remaining Qodana findings (Qodana: 0 problems) | 2026-11-23 | d2fdd91 | [261123-rzt-fix-phase-2-review-findings-wr-02-wr-03-](./quick/261123-rzt-fix-phase-2-review-findings-wr-02-wr-03-/) |
| 15 | Record the open decision on governing the other gate configs in STATE.md Blockers/Concerns | 2026-11-24 | 0ee1e67 | — |
| 261124-cm1 | Record the AxonIQ maintainer responses (2026-11-23) in upstream records UP-0001, -0002, -0003, -0004 and -0007 | 2026-11-24 | 614baa7 | [261124-cm1-record-the-axoniq-maintainer-responses-2](./quick/261124-cm1-record-the-axoniq-maintainer-responses-2/) |
| 261124-c1g | Fix review findings WR-07..WR-10: merge-hook bypasses and the ComposeVersions functional test | 2026-11-24 | ecfab86 | [261124-c1g-fix-review-findings-wr-07-wr-10-merge-ho](./quick/261124-c1g-fix-review-findings-wr-07-wr-10-merge-ho/) |
| 261124-dvp | Governance edit hook stops asking inside verified agent worktrees; the merge hook asks once (ADR-0015) | 2026-11-24 | 9fb92b0 | [261124-dvp-stop-the-governance-edit-hook-from-askin](./quick/261124-dvp-stop-the-governance-edit-hook-from-askin/) |
| 261124-eum | Renovate tracks the GSD version pin; the pin is never exempt from the governance label; GSD update flow in docs/tooling.md | 2026-11-24 | 4ebefac | [261124-eum-add-a-renovate-custom-manager-that-track](./quick/261124-eum-add-a-renovate-custom-manager-that-track/) |
| 261124-gju | Govern the configs and launchers of every required CI gate (58 -> 80 entries) and restore the 7-day Renovate cooldown for npm | 2026-11-24 | 2ac6c3c | [261124-gju-govern-the-configs-of-the-other-required](./quick/261124-gju-govern-the-configs-of-the-other-required/) |
| 261124-hbh | Govern the courses.kotlin-library convention plugin (detekt, allWarningsAsErrors, licence apply line, Dokka failOnWarning); narrow the residual to per-module build scripts | 2026-11-24 | f818100 | [261124-hbh-govern-build-logic-src-main-kotlin-cours](./quick/261124-hbh-govern-build-logic-src-main-kotlin-cours/) |
| 261124-kh6 | Fix Phase 2 review findings CR-01 and WR-01..WR-06 (merge-hook bypass forms, agent-worktree ** exemption, GSD pin validation, upstream records) and deduplicate merge-hook reasons | 2026-11-24 | e34377b | [261124-kh6-fix-phase-02-review-findings-cr-01-and-w](./quick/261124-kh6-fix-phase-02-review-findings-cr-01-and-w/) |
| 261124-nlk | Close Phase 2 review findings IN-06 (false positive, guarded by tests), IN-07 (value-only KDoc) and IN-08 (doctor checks git >= 2.36) | 2026-11-24 | 54f0e53 | [261124-nlk-fix-phase-02-review-findings-in-06-and-i](./quick/261124-nlk-fix-phase-02-review-findings-in-06-and-i/) |
| 24 | Record the 2026-11-24 maintainer reply on AxonFramework#5326 in UP-0001 (package, callback with logging default, assignment, target main) | 2026-11-24 | 2ad525f | — |
| 261124-oyk | Fix Phase 2 review findings WR-01 (doctor survives a failing --version), WR-02 (KDoc states the raw-UUID map key), WR-03 (image check matches the Renovate form) and IN-01 | 2026-11-24 | b62ce4d | [261124-oyk-fix-phase-2-review-findings-wr-01-wr-02-](./quick/261124-oyk-fix-phase-2-review-findings-wr-01-wr-02-/) |
| 261124-s7w | Fix Phase 2 review findings WR-01 (doctor survives a corrupt GSD manifest and six sibling reads), IN-01 and IN-02 | 2026-11-24 | 8788e2b | [261124-s7w-fix-phase-2-review-findings-wr-01-doctor](./quick/261124-s7w-fix-phase-2-review-findings-wr-01-doctor/) |
| 261124-uwn | Fix PR #21 CI failures (edit-hook reason on Linux, 5 Qodana findings), close review findings WR-01, IN-01, IN-02 and remove the UP-0012 workaround | 2026-11-24 | 1aa0bee | [261124-uwn-fix-the-phase-2-pr-21-ci-failures-linux-](./quick/261124-uwn-fix-the-phase-2-pr-21-ci-failures-linux-/) |
| 261124-wp3 | Enable native access (JEP 472) for every test JVM of the courses.test-suites convention, pinned by a TestSuitesFunctionalTest probe | 2026-11-25 | 5379af2 | [261124-wp3-enable-native-access-for-test-jvms-in-th](./quick/261124-wp3-enable-native-access-for-test-jvms-in-th/) |
| 261125-a09 | Fix Phase 2 review findings WR-01 (doctor warns for containerd below Docker 29.9.0), IN-01 (MCP server with neither command nor url fails) and IN-03 (compose line-order test); IN-02 noted for Phase 6 | 2026-11-25 | 6028435 | [261125-a09-fix-phase-2-review-findings-wr-01-in-01-](./quick/261125-a09-fix-phase-2-review-findings-wr-01-in-01-/) |
| 261125-bha | Move the build-logic TestKit suite into its own parallel CI job (functional-test) to cut the Build job from about 13.5 to 4-5 minutes | 2026-11-25 | 7ae6619 | [261125-bha-move-build-logic-functionaltest-into-its](./quick/261125-bha-move-build-logic-functionaltest-into-its/) |
| 31 | Translate quoted author answers in Phase 1 artifacts to English and drop the conversation-language note | 2026-11-25 | e4db42e | — |

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-11-22T11:11:47.170Z
Stopped at: Phase 2 context gathered
Resume file: .planning/phases/02-runtime-infrastructure-dcb-spikes/02-CONTEXT.md
