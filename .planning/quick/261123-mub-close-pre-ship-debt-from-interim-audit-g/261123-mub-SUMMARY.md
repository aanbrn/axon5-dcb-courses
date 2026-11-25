---
phase: quick-261123-mub
plan: 01
subsystem: build-logic, governance, planning
tags: [compose, version-catalog, governance, qodana, testkit, requirements]
requires: []
provides:
  - one shared derivation of compose images from every image-* catalog entry
  - Qodana gate configuration and launcher as governed paths
  - non-vacuous committed-attempt assertion in DecisionCaptureIntegrationTest
affects: [courses.governance, courses.root, governance/governed-paths.txt, .claude/settings.json, REQUIREMENTS.md]
tech-stack:
  added: []
  patterns: [default-include derivation with an explicit test-only exclusion set]
key-files:
  created:
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt
  modified:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt
    - build-logic/src/main/kotlin/courses.governance.gradle.kts
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - gradle/libs.versions.toml
    - governance/governed-paths.txt
    - .claude/settings.json
    - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheckFunctionalTest.kt
    - docs/upstream/0011-colima-aaaa-dns.md
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md
    - .planning/REQUIREMENTS.md
    - .planning/phases/01-repository-build-ci-foundation/01-VERIFICATION.md
    - .planning/STATE.md
decisions:
  - "Compose images are derived by filtering: every image-* catalog entry except ComposeVersions.TEST_ONLY_IMAGES (ryuk)"
  - "qodana.yaml and scripts/qodana.sh are governed; NOTICE, docs/adr, docs/upstream, compose.yaml and compose/** stay outside, documented"
status: complete
completed: 2026-11-23
duration: about 40 minutes of work plus a 7m47s full build
commits: 5
plan_head_before: 305b21b743685322e5d7e747c357ab9ac241b928
plan_head_after: 85fd964205a5c514aa33a43dd0fbc810150ffdf1
actuals:
  tokens: 14000
  tasks: 3
  commits: 5
---

# Phase quick-261123-mub Plan 01: Close pre-ship debt from the interim audit Summary

Shared catalog-driven compose image derivation (`ComposeVersions.images`) proven by a TestKit test with a third image,
the Qodana gate files governed, the second decision-capture case made non-vacuous, and the Phase 2 requirement
statuses refreshed. The full `./gradlew build` passes; the Qodana gate could not run (image pull blocked by the network).

## Commits

| Task | Commit | Message |
|------|--------|---------|
| 1 (INT-W2) | `3cc9d17` | fix(build-logic): derive compose images from every image-* catalog entry |
| 2 part A (INT-W1) | `cf86564` | chore(governance): govern the Qodana gate configuration and launcher |
| 2 part B (WR-05) | `ae12133` | test(command-service): fail decision capture when no attempt committed |
| 3 | `c041b1e` | docs(quick-261123-mub): record WR-05 fix and refresh Phase 2 requirement statuses |
| gate fix | `85fd964` | style: satisfy editorconfig-checker in a test policy literal and a Go snippet |

## Task results

**Task 1.** `ComposeVersions.images(catalog)` walks the `image-*` version aliases, drops `TEST_ONLY_IMAGES`, and returns
short name to reference. `courses.governance` (`checkComposeVersions`) and `courses.root` (`generateComposeVersions`)
both call it and name no image. `ComposeVersionsFunctionalTest` has 3 cases (repository catalog renders two images and no
ryuk; a third `image-otel-lgtm` entry is rendered and checked, stale on the old file, passing after regeneration; a
versions file that also lists `RYUK_IMAGE` is stale), all run under `--configuration-cache --isolated-projects`. Red
state was observed first (the third-image case failed on the hardcoded logic). `GeneratedFileCheckFunctionalTest` still
passes. Byte-identity proof: after `generateComposeVersions --rerun`, `git diff --exit-code compose/versions.env` exits 0.

**Task 2.** `governance/governed-paths.txt` has 58 entries; `.claude/settings.json` has 58 `Edit(/...)` ask rules;
`checkClaudeAskRules` passes; `test-check-pr.sh` (132 passed) and `test-list-pr-files.sh` (25 passed) pass. The "outside the
boundary" list now names NOTICE, `docs/adr/**`, `docs/upstream/**`, `compose.yaml` and `compose/**` with the reason, and none
of them is an entry. `DecisionCaptureIntegrationTest` (2 tests, 0 failures) passes against a real Axon Server with
`recorder.committed() shouldHaveSize SEATS` in the second case.

**Task 3.** WR-05 is `fixed` with SHA `ae12133` (which touches the test file), `open` counter 10. 18 Phase 2 IDs are Complete
and `[x]`; CICD-09, UPS-02, UPS-03 and TOOL-04 stay Pending with the specified notes and `[ ]`; scope notes and footer added.
01-VERIFICATION.md frontmatter is `human_needed`. STATE.md Current Position lines updated (frontmatter and Quick Tasks
table untouched). All grep gates of the plan pass.

## The ryuk filter decision

The catalog holds `image-ryuk`, which no compose file uses (Testcontainers' reaper, pinned for test JVMs through
`TESTCONTAINERS_RYUK_CONTAINER_IMAGE`). The derivation includes every `image-*` entry by default and removes only names in
the KDoc-documented `TEST_ONLY_IMAGES` set (`ryuk`). The default-include direction is fail-safe: a new compose image is
rendered and checked with no build-logic edit, and an opt-out is a visible, reviewable change. Including ryuk would have
changed `compose/versions.env` and put an image no service runs into compose's environment.

## Observation: required-gate configuration files outside the governed boundary

Confirmed against `.github/workflows/ci.yml` (jobs of `ci-success`: build, lint, secrets, qodana, integration-test,
compose-smoke, doctor, upstream-records). None of these is in `governance/governed-paths.txt`; for the author to decide:

- `.editorconfig-checker.json` and `.editorconfig`: read by the lint job (`editorconfig-checker`); `.editorconfig` also by
  Spotless shell formatting in the build job. An exclusion edit can make the lint step pass without checking.
- `REUSE.toml`: read by the lint job (`reuse lint`); an annotation edit can license files without headers.
- `.prettierrc.yaml`, `.taplo.toml`, `config/spotless/**` (license headers): read by `spotlessCheck` in the build job.
- `scripts/doctor.sh`: run by the doctor job (`scripts/doctor.sh --ci`).
- `.gitattributes`: the lint job's renormalisation check reads it.
- `lefthook.yml`: local pre-commit only, not a CI gate (listed for completeness).
- `scripts/test-*.sh` and `scripts/*/test-*.sh` (other than `scripts/governance/**`): run by the lint job's "Run shell tests"
  step; like functional tests, deleting one cannot make a check pass, so likely correctly outside.

## Final gate

| Gate | Result |
|------|--------|
| `mise x -- ./gradlew build` | exit 0, BUILD SUCCESSFUL in 7m 47s (170 tasks: 123 executed, 6 from cache, 41 up-to-date) |
| `mise x -- reuse lint` | passes, 450/450 files compliant |
| `mise x -- editorconfig-checker` | exit 0 after commit `85fd964` (see deviation 1) |
| `scripts/qodana.sh` | NOT RUN: exit 125 twice after about 158 s each, see deviation 2 |

## Deviations from Plan

**1. [Rule 3 - Blocking] editorconfig-checker findings in files this plan did not touch.** The first run failed with 3
errors on the base tree: `DependencyLicenceCheckFunctionalTest.kt` lines 210-213 (raw-string JSON members indented by 2 where
`.kt` wants a multiple of 4) and `docs/upstream/0011-colima-aaaa-dns.md` lines 115-123 (tabs in a Go snippet). The plan says to
fix the cause in the owning file and never weaken a check, so I re-indented both (JSON members by 4 inside the
`trimIndent()` literal, the Go snippet with spaces), re-ran `editorconfig-checker` (exit 0), `spotlessCheck`,
`checkUpstreamRecords` and `DependencyLicenceCheckFunctionalTest` (green), and committed separately as `85fd964`. These
findings were already present at the plan base, so the lint job of the phase PR would have been red without this fix.

**2. [Environment blocker] Qodana did not run.** `scripts/qodana.sh` exited 125 on both attempts:
`docker: error pulling image configuration: download failed after attempts=6: dial tcp 52.222.236.7:443: i/o timeout` while
pulling `jetbrains/qodana-jvm-community:2026.3@sha256:5420d961...`. A direct `docker pull` of the same digest fails the same
way; the Docker Hub registry itself answers (401 on `/v2/`), so the blocker is network access from the colima VM and host to
the registry blob CDN, not the repository. No colima or Docker setting was changed. The new Kotlin code is covered by
detekt, ktfmt and the Kotlin compiler under `./gradlew build`, but the required `qodana` job result is unverified locally:
rerun `scripts/qodana.sh` once the registry is reachable.

**3. [Observation] The plan's verify one-liner runs `generateComposeVersions` and `checkComposeVersions` in one Gradle
invocation.** Gradle fails that with an implicit-dependency validation error (the check reads the generator's output without a
declared dependency). The wiring is untouched by this plan (the task graph shape was unchanged), so I ran them as two
invocations. A `mustRunAfter` or `dependsOn` in `courses.governance` would make the one-liner work; not done, out of scope.

**4. Skills.** The plugin skills `axoniq-app-development:axoniq-app-dev` and `gradle-skills:gradle-best-practices` are not
installed in this session; I continued without them.

**5. Implementation detail.** `the<VersionCatalogsExtension>()` must be evaluated at script level: inside the
`tasks.register { }` lambda `the` resolves on the task and fails. `courses.root` therefore holds a script-level
`composeImages` value, as `courses.governance` does.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- Created file `ComposeVersionsFunctionalTest.kt` exists.
- Commits `3cc9d17`, `cf86564`, `ae12133`, `c041b1e`, `85fd964` are ancestors of HEAD.
- Note: the final gate item `scripts/qodana.sh` is unverified (deviation 2).
