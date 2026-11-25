---
phase: quick-261125-bha
plan: 01
subsystem: ci
tags: [ci, github-actions, testkit, build-logic, adr]
requires: []
provides:
  - "CI job `functional-test` running `:build-logic:functionalTest` in parallel with `build`"
  - "Build job without the TestKit suite, `ci-success` needing the new job"
affects: [".github/workflows/ci.yml", "docs/adr/0010-test-levels-as-jvm-test-suites.md", "docs/tooling.md"]
tech-stack:
  added: []
  patterns: ["parallel CI test jobs aggregated by ci-success (D-20)"]
key-files:
  created: []
  modified:
    - .github/workflows/ci.yml
    - docs/adr/0010-test-levels-as-jvm-test-suites.md
    - docs/tooling.md
key-decisions:
  - "Job id `functional-test`, name `Functional tests (build-logic)`, placed after `integration-test`, listed right after `build` in `ci-success` needs"
  - "ADR-0010 keeps its date and is edited in place (precedent 261125-a09)"
requirements-completed: [CICD-01, CICD-03, INFRA-12]
duration: about 35 min (of which 9m 34s the full local build)
completed: 2026-11-25
status: complete
commits: 2
plan_head_before: 3d3cc345da0b2721abfbce1e0f87e9818ba3001c
plan_head_after: 76bf182d92cb376aaede0a81101c3a9caba3a71c
actuals:
  tokens: 9000
  tasks: 3
  commits: 2
---

# Quick Task 261125-bha: Move build-logic functionalTest into its own CI job Summary

The build logic's TestKit suite (about 9 of the 13.5 minutes of the CI `build` job) now runs in its own parallel
`functional-test` job, while local `check` and `build` still run it (D-19).

## What changed

- `.github/workflows/ci.yml` (governed, edited with the Edit tool only; commit `db8c12c`):
  - Build step: `./gradlew build checkDependencyLicences -x integrationTest -x :build-logic:functionalTest --console=plain`
    (`checkDependencyLicences` still named, T-02-58); step comment rewritten.
  - New job `functional-test` (`Functional tests (build-logic)`) after `integration-test`: `contents: read`,
    `persist-credentials: false`, same SHA-pinned checkout, setup-java, setup-node, mise-action (shfmt and taplo only),
    setup-gradle (read-only cache off main, same encryption key), `MISE_DISABLE_TOOLS: java,node`; runs
    `./gradlew :build-logic:functionalTest --console=plain`; on failure uploads `functional-test-reports`.
  - `ci-success` `needs` now `[build, functional-test, lint, secrets, qodana, integration-test, compose-smoke, doctor, upstream-records]`.
- `docs/adr/0010-test-levels-as-jvm-test-suites.md` and `docs/tooling.md` (commit `76bf182`): D-20 bullet,
  Confirmation and the job-layout pros and cons describe three parallel jobs and the current Build command; the
  fast-loop paragraph names the TestKit functional tests and their CI job.
- `build-logic/build.gradle.kts` untouched (`git rev-list --count a881aac..HEAD -- build-logic/build.gradle.kts` = 0).

## Task 1 gate results (on the final HEAD, 21 of 21 PASS)

- Build run line dry run: BUILD SUCCESSFUL, `:build-logic:(functionalTest|compileFunctionalTestKotlin)` count 0
  (was 2), `:checkDependencyLicences` count 9.
- functional-test run line dry run lists `:build-logic:functionalTest`.
- `needs` equals the set of the other 9 job ids.
- `check` dry-run graph (333 tasks) fully covered by the union of the CI `run: ./gradlew` graphs.
- Grep gates: functional-test job 1, Build run line 1, functional-test run line 1, needs prefix 1,
  `functional-test-reports` 1, setup-node 4, mise-action 5, install_args 2, persist-credentials 9, `contents: read` 9,
  `MISE_DISABLE_TOOLS` 5, cache-read-only 5, upload-artifact 4.
- D-19: `./gradlew check --dry-run` still lists `:build-logic:functionalTest` (1).

## Task 2 gate results

`checkAdrs spotlessPrettierCheck` BUILD SUCCESSFUL; ADR greps: `-x :build-logic:functionalTest` 1, `functional-test` 1,
`has the three jobs` 1, old "two jobs / both required / aggregates both" 0; tooling.md: new sentence 1, old sentence 0.

## Task 3: lint-job steps and full build (final HEAD `76bf182`, no files changed after it)

Untracked `.build/` deleted before editorconfig-checker; `git ls-files .build` is empty and `.build/` did not
reappear.

| Step | Exit |
| --- | --- |
| actionlint | 0 |
| zizmor `--no-progress .github/workflows` (ONLINE, `GH_TOKEN` from `gh auth token`; no findings, 6 suppressed) | 0 |
| shellcheck over `git ls-files -z '*.sh'` | 0 |
| editorconfig-checker | 0 |
| `reuse lint` | 0 |
| line-ending renormalisation on a temporary index copy | 0 |
| `renovate-config-validator --strict renovate.json5` (warns that RE2 is not usable locally and falls back to RegExp; config validated) | 0 |
| `docker compose config --quiet` | 0 |
| every `scripts/test-*.sh` and `scripts/*/test-*.sh` shell test | 0 |

Full `mise x -- ./gradlew build --console=plain`: BUILD SUCCESSFUL in 9m 34s (166 actionable tasks: 78 executed,
75 from cache, 13 up-to-date), including `:build-logic:functionalTest`. Qodana was not run locally (no Kotlin change;
CI runs it).

Expected CI effect: the `Build` job drops the roughly 9 minute TestKit suite (13m35s to about 4 to 5 minutes) and the
new `Functional tests (build-logic)` job takes about 10 minutes in parallel, so the `ci-success` critical path shrinks
by about 3 to 4 minutes. The new job's Gradle cache may be cold until it has run once on main.

## Deviations from Plan

None - plan executed exactly as written. The existing Build step comment had its first sentences in a slightly different
form than the plan assumed ("TestKit tests," was inside the list); it was rewritten as specified.

## Known Stubs

None.

## Threat Flags

None. The new job reuses the existing trust boundaries (read-only token, no credentials persisted, read-only cache off
main, SHA-pinned actions), and zizmor found nothing.

## Self-Check: PASSED

- `.github/workflows/ci.yml`, ADR-0010 and `docs/tooling.md` modified and committed; commits `db8c12c` and `76bf182`
  are on the branch; `git status` clean before this SUMMARY.
