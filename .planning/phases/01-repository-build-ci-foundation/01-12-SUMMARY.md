---
phase: 01-repository-build-ci-foundation
plan: 12
subsystem: infra
tags: [github-actions, ci, gradle, mise, gitleaks, actionlint, zizmor, codeql, scorecard, adr]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Gradle build with Spotless, detekt, Dokka and governance checks (01-01 to 01-07, 01-10), REUSE and normalisation (01-06), hook and shell scripts (01-08), governance workflow and shell test (01-09), Renovate config (01-11), mise tool pins (01-03)"
provides:
  - "ci.yml with build, lint and secrets jobs and the stable ci-success aggregator"
  - "codeql.yml analysing the actions language on push, pull request and weekly"
  - "scorecard.yml publishing OpenSSF Scorecard results and SARIF"
  - "ADR-0001 (workflow permissions and two required contexts) and ADR-0002 (CodeQL Kotlin spike outcome)"
affects: [01-13 ruleset and required contexts, 01-14 approval items, Phase 2 ADR templates, Phase 2 first Kotlin module, Phase 3 coverage, Phase 6 supply chain]

plan_head_before: 90ef3b130c133a546b56f388d382f40b8b3308ab
plan_head_after: 5a6caea38bc9121496674e275d27fa44bcff3650

actuals:
  tokens: 4160
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Workflow-level permissions {} with minimal job grants, persist-credentials false, every action pinned by commit SHA with a version comment"
    - "Aggregator job with if: always() and a strict success comparison as the single required context per workflow"
    - "CI tool installs go through mise (version read from mise.toml min_version, MISE_DISABLE_TOOLS=java,node) while JDK and Node come from .java-version and .nvmrc"
    - "Lint job discovers shell tests with git ls-files so new scripts/**/test-*.sh join CI without editing the workflow"

key-files:
  created:
    - .github/workflows/ci.yml
    - .github/workflows/codeql.yml
    - .github/workflows/scorecard.yml
    - docs/adr/0001-workflow-permissions-and-required-checks.md
    - docs/adr/0002-codeql-kotlin-analysis.md
  modified: []

key-decisions:
  - "Plan 01-12: MISE_DISABLE_TOOLS=java,node is set at job level (not only on the mise-action step) so the shims Spotless and the lint commands use never try to resolve a second JDK or Node"
  - "Plan 01-12: build job relies on the mise-action shims on PATH and plain ./gradlew build (not mise x --), because mise x would try to install every tool in mise.toml in the build job"
  - "Plan 01-12: CodeQL ships the actions language only; the java-kotlin spike extracted no source, recorded with re-enable criteria in ADR-0002"
  - "Plan 01-12: concurrency groups added to codeql.yml and scorecard.yml to keep the zizmor pedantic persona clean"

patterns-established:
  - "Normalisation check renormalises a copy of the index through GIT_INDEX_FILE and a trap-cleaned temporary file; it only passes on a clean tree because git add --renormalize also stages tracked modifications"

requirements-completed: [CICD-01, CICD-03, CICD-06, CICD-08, INFRA-10, INFRA-13, GH-06]

coverage:
  - id: D1
    description: "ci.yml build job runs ./gradlew build with JDK and Node from version files, mise-provided shfmt and taplo, a main-only Gradle cache and no Build Scans"
    requirement: "CICD-01"
    verification:
      - kind: other
        ref: "mise x -- actionlint .github/workflows/ci.yml; mise x -- zizmor --offline --no-progress --persona=pedantic .; mise x -- ./gradlew build --dry-run; mise x -- ./gradlew check --rerun-tasks"
        status: pass
    human_judgment: true
    rationale: "The workflow has not run on GitHub-hosted runners yet; the first hosted run on the phase PR proves the mise shims, setup-gradle inputs and cache behaviour"
  - id: D2
    description: "lint job covers actionlint, zizmor, shellcheck, editorconfig-checker, reuse lint, index-copy normalisation, strict Renovate validation and every tracked scripts shell test"
    requirement: "CICD-08"
    verification:
      - kind: other
        ref: "each lint command run locally through mise x --; scripts/governance/test-check-pr.sh 39 passed, 0 failed; normalisation script rc 0 on a clean tree"
        status: pass
    human_judgment: true
    rationale: "Tool installation by mise-action in the hosted lint job (pipx and npm backends) is only provable on the first hosted run"
  - id: D3
    description: "secrets job scans the full history with gitleaks git --redact"
    requirement: "CICD-06"
    verification:
      - kind: other
        ref: "mise x -- gitleaks git --redact --no-banner (66 commits, no leaks found)"
        status: pass
    human_judgment: false
  - id: D4
    description: "ci-success aggregates build, lint and secrets with if: always() and treats skipped, cancelled and failed results as failure; no path filters"
    requirement: "CICD-01"
    verification:
      - kind: other
        ref: "jq -e 'all(.[]; .result == \"success\")' against skipped, cancelled and success inputs (rc 1, 1, 0); grep paths: count 0"
        status: pass
    human_judgment: false
  - id: D5
    description: "Hardening: permissions {} at workflow level, minimal job grants, persist-credentials false, every uses pinned by a 40-hex SHA with a tag comment"
    requirement: "CICD-03"
    verification:
      - kind: other
        ref: "grep of every uses line (0 unpinned); zizmor default and pedantic persona clean"
        status: pass
    human_judgment: false
  - id: D6
    description: "Gradle cache writes only from main, optional GRADLE_ENCRYPTION_KEY, no Build Scan flag in any workflow"
    requirement: "INFRA-10"
    verification:
      - kind: other
        ref: "cat .github/workflows/*.yml | grep -vE '^\\s*#' | grep -c -- '--scan' prints 0"
        status: pass
    human_judgment: true
    rationale: "Cache write and read-only behaviour is observable only on hosted runs"
  - id: D7
    description: "Normalisation check never touches the real index"
    requirement: "INFRA-13"
    verification:
      - kind: other
        ref: "bash normalise.sh (kept outside the repository) rc 0 on a clean tree; git status unchanged afterwards"
        status: pass
    human_judgment: false
  - id: D8
    description: "CodeQL (actions) and OpenSSF Scorecard workflows exist; java-kotlin deferred with spike evidence"
    requirement: "GH-06"
    verification:
      - kind: other
        ref: "mise x -- actionlint; mise x -- zizmor --persona=pedantic .; docs/adr/0002-codeql-kotlin-analysis.md"
        status: pass
    human_judgment: true
    rationale: "Neither workflow has run on GitHub; Scorecard publication and code-scanning upload need the first hosted run on main"
  - id: D9
    description: "ADR-0001 records the D-21 deviation from D-09 (permissions {} and two required contexts)"
    verification: []
    human_judgment: true
    rationale: "Prose decision record; the author reviews the wording"

duration: 24 min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 12: CI, CodeQL and Scorecard Summary

**Hardened GitHub Actions CI (build, lint, full-history secret scan) behind one `ci-success` context, plus CodeQL for workflows and OpenSSF Scorecard; CodeQL Kotlin analysis is deferred after a negative spike.**

## Performance

- **Duration:** 24 min (the CodeQL bundle download dominated: about 9 minutes with 16 parallel ranged requests)
- **Started:** 2026-11-21T11:16:45Z
- **Completed:** 2026-11-21T11:41:02Z
- **Tasks:** 3 (1 tracer, 2 auto)
- **Files modified:** 5 created, 0 modified

## Accomplishments

- `ci.yml`: `build` (`./gradlew build --console=plain`, JDK and Node from `.java-version` and `.nvmrc`, shfmt and taplo from mise, `gradle/actions/setup-gradle` with `cache-provider: basic`, cache writes from `main` only, optional `GRADLE_ENCRYPTION_KEY`, build reports uploaded on failure), `lint` (actionlint, zizmor with `GH_TOKEN`, shellcheck, editorconfig-checker, `reuse lint`, index-copy normalisation, `renovate-config-validator --strict`, every tracked `scripts/**/test-*.sh`), `secrets` (`gitleaks git --redact --no-banner` over full history) and `ci-success` (`needs: [build, lint, secrets]`, `if: always()`, strict `success`).
- `codeql.yml` (`actions` language, push to `main`, pull requests, weekly) and `scorecard.yml` (push to `main`, branch-protection-rule, weekly, `publish_results: true`, SARIF upload).
- ADR-0001 (D-21: `permissions: {}` plus job grants, two required contexts) and ADR-0002 (spike result and re-enable criteria).
- Tracer gate: the tracer's `<verify>` is automated-only and human-verify mode is `end-of-phase`, so the verify was re-run end to end (actionlint, zizmor, `./gradlew build --dry-run`, aggregator expression) and passed: "Tracer verified end-to-end, expanding".

## Task Commits

1. **Task 1: end-to-end CI build with `ci-success`** - `25d4a88` (feat)
2. **Task 2: lint and secrets jobs, ADR-0001** - `9a4761c` (feat)
3. **Task 3: CodeQL, Scorecard, ADR-0002** - `5a6caea` (feat)

**Plan metadata:** recorded by the docs commit that adds this summary.

## Files Created/Modified

- `.github/workflows/ci.yml` - build, lint, secrets and ci-success jobs
- `.github/workflows/codeql.yml` - CodeQL analysis of the workflows
- `.github/workflows/scorecard.yml` - OpenSSF Scorecard with SARIF upload
- `docs/adr/0001-workflow-permissions-and-required-checks.md` - D-21 decision record
- `docs/adr/0002-codeql-kotlin-analysis.md` - CodeQL Kotlin spike outcome

## CodeQL spike result (D-24)

`java-kotlin` did **not** ship. Two attempts with the `codeql-bundle-v2.27.5` macOS bundle (checksum verified against the release, scratch directory deleted afterwards):

1. With `--no-configuration-cache`: Gradle aborted, `Configuration Cache cannot be disabled when Isolated Projects is enabled.`
2. With `--rerun-tasks` and `-Pkotlin.compiler.execution.strategy=in-process`: the compile ran (`BUILD SUCCESSFUL`, `:tooling:convention-sample:compileKotlin` executed) but the extraction summary line was `ERROR: CodeQL detected code written in Java/Kotlin but could not process any of it.`

The result is bounded: the spike ran on macOS Apple Silicon, not on the Linux x86-64 CI runner. ADR-0002 records the evidence and the re-enable criteria (first real Kotlin module in Phase 2, or a documented recipe; retry on Linux with the configuration cache left on).

## Decisions Made

- `MISE_DISABLE_TOOLS=java,node` is set at job level so every step, including tool shims invoked later by Gradle and the lint commands, sees it, not only the mise-action step.
- The build job uses plain `./gradlew build` with the mise-action shims on `PATH` rather than `mise x -- ./gradlew build`: `mise x` without tool arguments would try to install every tool in `mise.toml` in that job.
- `codeql.yml` and `scorecard.yml` carry concurrency groups and commented permissions so the pedantic zizmor persona stays clean (not only the default one).
- Plan 01-13 note: ADR-0001 lists the two required contexts (`ci-success`, `governance-success`); the job names equal the context names.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Spike command incompatible with Isolated Projects**
- **Found during:** Task 3 (CodeQL spike, attempt 1)
- **Issue:** the plan's command passes `--no-configuration-cache`, which Gradle rejects while `org.gradle.isolated-projects=true`.
- **Fix:** attempt 2 dropped the flag and kept `--rerun-tasks` so the compile still executes; recorded in ADR-0002.
- **Files modified:** docs/adr/0002-codeql-kotlin-analysis.md
- **Committed in:** 5a6caea

**2. [Rule 3 - Blocking] Slow single-connection bundle download**
- **Found during:** Task 3
- **Issue:** `gh release download` ran at about 0.4 MB/s for the 879 MB bundle (about 35 minutes).
- **Fix:** a scratch script outside the repository fetched 16 ranged parts in parallel from the release URL (about 9 minutes) and the SHA-256 was verified against the release's published digest.
- **Files modified:** none in the repository
- **Committed in:** n/a

**3. [Rule 2 - Missing critical hygiene] Workflow-hardening additions beyond the plan text**
- **Found during:** Tasks 2 and 3
- **Issue:** the pedantic zizmor persona (a standing requirement from earlier plans) flagged missing concurrency limits and undocumented permissions in the new workflows.
- **Fix:** added concurrency groups (`codeql.yml`, `scorecard.yml`) and permission comments; `MISE_DISABLE_TOOLS` moved to job level.
- **Files modified:** .github/workflows/codeql.yml, .github/workflows/scorecard.yml, .github/workflows/ci.yml
- **Committed in:** 9a4761c, 5a6caea

---

**Total deviations:** 3 auto-fixed (2 blocking, 1 hygiene). **Impact on plan:** none on scope; the CodeQL Kotlin leg is deferred, which the plan allows.

## Issues Encountered

- The session harness refuses compound commands that contain `git` tokens (including `.github` paths); commands were split into plain ones, and the CodeQL scratch scripts live outside the repository.
- The plan-commit ledger file under the shared git directory was not written; `plan_head_before` is the HEAD recorded before the first commit and `commits` is `git rev-list --count` from it (3 task commits at write time, before the docs commit).
- `git add --renormalize .` also stages modifications of tracked files, so the normalisation check reports uncommitted edits as differences. In CI the tree is clean, so it is exact; locally run it on a clean tree.
- `renovate-config-validator` prints a RE2 stack trace line on this machine and still exits 0 (same as plan 01-11).

## Open Verifications and Follow-ups

- **First hosted run (phase PR via `/gsd-ship`):** confirms the mise-action shims provide shfmt and taplo to Spotless in the `build` job, `install_args` for `pipx:reuse` and `npm:renovate` in the `lint` job, the setup-gradle `cache-provider: basic` input at the pinned SHA, and CodeQL, Scorecard publication and the SARIF upload. None of that could be run locally.
- **Secret in `with:`:** the auditor persona of zizmor reports `secrets-outside-env` for the optional `GRADLE_ENCRYPTION_KEY` in `build` (only the auditor persona, not default or pedantic); the secret is optional and created in plan 01-14.
- **Action ages:** `jdx/mise-action` v5.2.10, `ossf/scorecard-action` v2.6.0 and `github/codeql-action` v4.39.4 were released less than 7 days before pinning; the plan fixes the research-table versions, and Renovate (7-day cooldown) governs later bumps.
- **Standing obligations (ROADMAP Cross-Cutting Conventions):** coverage-report clause of CICD-01 is owned by Phase 3 (Kover, TEST-08) and CICD-08's `docker compose config` validation by Phase 2; CICD-03 base images are Phase 6.
- **CICD-06 flagged assumption:** a leak already in history is reported by the full-history scan, not purged (history rewriting is out of scope); the staged hook (01-08) and push protection (01-14) are the other layers.

## Verification Results and Timing

Warm caches, run through `mise x --` where the tool comes from mise.

| Command | Result | Wall clock |
| --- | --- | --- |
| `actionlint` (all workflows) | pass | under 1 s |
| `zizmor --offline --no-progress` (default and `--persona=pedantic`) | no findings | under 2 s |
| `./gradlew build --dry-run --console=plain` | BUILD SUCCESSFUL | under 1 s |
| aggregator `jq` expression (skipped, cancelled, success inputs) | rc 1, 1, 0 | under 1 s |
| `shellcheck` on the three tracked scripts | pass | under 1 s |
| `editorconfig-checker` | pass | under 1 s |
| `reuse lint` | pass | 1 s to 3 s |
| `renovate-config-validator --strict renovate.json5` | pass (rc 0) | about 1 s |
| `bash scripts/governance/test-check-pr.sh` | 39 passed, 0 failed | 1.1 s |
| normalisation script (index copy, clean tree) | clean, rc 0 | under 1 s |
| `gitleaks git --redact --no-banner` (66 commits) | no leaks found | 0.5 s |
| `./gradlew build` (warm, cache hits) | BUILD SUCCESSFUL | 1 s |
| `./gradlew check --rerun-tasks` (everything re-executed) | BUILD SUCCESSFUL | 2 min 22 s |
| CodeQL bundle download (879 MB, 16 parallel ranges) | verified | about 9 min (one-off) |
| CodeQL extraction attempts | 1: Gradle abort, 2: no source seen | 10 s and 19 s |

Only the full `check --rerun-tasks` (142 s) and the one-off bundle download exceed 30 s; the check is inside the 180 s per-task feedback budget of `01-VALIDATION.md`. No validation gap. Acceptance criteria: `^permissions: {}` 1, `java-version-file: .java-version` 1, `paths:` 0, `--scan` 0, `needs: [build, lint, secrets]` 1, `fetch-depth: 0` 1, `MISE_DISABLE_TOOLS` 3, `GIT_INDEX_FILE` 1, `governance-success` in ADR-0001 1, unpinned `uses` 0, `language:` 2 (matrix contains `actions`), `publish_results: true` 1, `java-kotlin` in ADR-0002 4.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-01-26 (permissions `{}`, job grants, no persisted credentials), T-01-27 (full-SHA pins, online zizmor with the job token, Renovate cooldown), T-01-28 (values via `env:`, actionlint and zizmor clean), T-01-29 (cache read-only except `main`), T-01-30 (gitleaks `--redact`, no echoed secrets) are mitigated as planned. T-01-SC: no packages installed; the CodeQL bundle is GitHub's official release, checksum verified, kept outside the repository and deleted.

## User Setup Required

None - no external service configuration required in this plan. The optional `GRADLE_ENCRYPTION_KEY` secret is created in plan 01-14.

## Next Phase Readiness

- Plan 01-13 can require `ci-success` and `governance-success` (job names equal the context names, see ADR-0001) and add its shell tests under `scripts/**/test-*.sh`; the lint job picks them up without editing the workflow.
- Plan 01-14's approval summary should list the optional `GRADLE_ENCRYPTION_KEY` secret and note that the first hosted run is the proof of the open verifications above.

## Self-Check: PASSED

Created files exist on disk (`ci.yml`, `codeql.yml`, `scorecard.yml`, ADR-0001, ADR-0002); commits `25d4a88`, `9a4761c` and `5a6caea` are ancestors of HEAD; every acceptance criterion holds; the plan-level verification passes (actionlint, zizmor, every lint and secrets command, `./gradlew check`).

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
