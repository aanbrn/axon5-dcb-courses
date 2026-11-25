---
phase: quick-261125-mwg
plan: 01
subsystem: governance-ci-build-logic
tags: [front-matter, upstream-records, renovate, composite-action, spotless, git-ignore, zizmor]
requires: []
provides:
  - upstream records and markers worded as observed (IN-01, IN-03)
  - front matter parser that reads Prettier's multi-line lists
  - one composite action for the CI mirror step, and a manager-scoped Renovate exclusion (IN-04)
  - Spotless that skips untracked git-ignored files
affects: [docs/upstream, docs/adr, .github, renovate.json5, build-logic, governance]
tech-stack:
  added: []
  patterns:
    - "ValueSource that runs git at configuration time and returns a filtered, cache-stable list"
    - "local composite action for a step repeated across jobs"
key-files:
  created:
    - .github/actions/maven-central-mirror/action.yml
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt
  modified:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt
    - .github/workflows/ci.yml
    - .github/workflows/codeql.yml
    - renovate.json5
    - lefthook.yml
    - governance/governed-paths.txt
    - .claude/settings.json
    - docs/upstream/0006-empty-platform-token.md
    - docs/upstream/0011-colima-aaaa-dns.md
    - docs/upstream/0013-axon-server-dcb-context-flag.md
    - docs/adr/0008-container-images-with-buildpacks.md
    - docs/adr/0016-ci-dependency-downloads.md
    - docs/templates/adr.md
    - docs/templates/upstream-record.md
    - docs/tooling.md
    - scripts/doctor.sh
key-decisions:
  - "The composite action is referenced as ./.github/actions/maven-central-mirror with an inline zizmor ignore of self-repository, because actionlint cannot parse the $/ form that audit asks for"
  - "GitIgnoredPaths drops a directory entry that holds only covered directories, so a leftover module with only a build directory never invalidates the configuration cache"
  - "Front matter bare list items containing whitespace followed by # are comments and fail closed, so a comment after an item without a comma cannot hide inside an item"
requirements-completed: [UPS-01, UPS-02, TOOL-03, CICD-01, CICD-04, CICD-08, TEST-09, INFRA-12]
duration: about 2h
completed: 2026-11-25
status: complete
actuals:
  tokens: 120000
  tasks: 3
  commits: 6
plan_head_before: 2dd5752b1267a3d6e81087f73c20f24d7a4e7bfc
plan_head_after: e05f36187b8b29fdf2562a3972d8b581abff2568
---

# Phase quick-261125-mwg Plan 01: Close the open Phase 02 follow-ups Summary

Multi-line Prettier lists in the front matter parser, the colima and CodeQL claims worded as observed, one composite action
for the CI mirror step with a manager-scoped Renovate exclusion, and a Spotless gate that skips untracked git-ignored files.

## Commits per finding

| Finding | Commit | What |
| ------- | ------ | ---- |
| IN-01 | `bfa1c20` | UP-0011 title and issue-draft title, Root cause assessment, ADR-0008, docs/tooling.md and the doctor marker say the stall was observed once and not reproduced on a clean profile |
| IN-03 | `4fc8521` | The UP-0010 marker at codeql.yml line 32 says the extractor found no sources, with an Isolated Projects interaction suspected (comment lines only) |
| parser | `56dc4c0` | `FrontMatter` reads both multi-line list shapes and a trailing comma; the `# prettier-ignore` lines of UP-0006 and UP-0013 are gone |
| IN-04a | `b8061f1` | `.github/actions/maven-central-mirror/action.yml` replaces the five inline install steps; zizmor audits `.github/actions` in the lint job and in lefthook |
| IN-04b | `1ae2c76` | `renovate.json5` drops the hand-copied preset list for `gradle: { ignorePaths: ["scripts/ci/**"] }` |
| Spotless | `e05f361` | `GitIgnoredPaths` plus `courses.root` exclude untracked git-ignored paths; governed entry and ask rule added |

## RED evidence

Parser (`:build-logic:functionalTest --tests '*UpstreamRecordCheckFunctionalTest*'`, unchanged parser): 27 tests
completed, 2 failed.

```
UpstreamRecordCheckFunctionalTest > front matter outside the supported subset fails with the line() FAILED
    org.opentest4j.AssertionFailedError at UpstreamRecordCheckFunctionalTest.kt:354
UpstreamRecordCheckFunctionalTest > front matter accepts the multi-line lists that Prettier writes and a trailing comma() FAILED
    org.gradle.testkit.runner.UnexpectedBuildFailure at UpstreamRecordCheckFunctionalTest.kt:42
```

The accepting test failed because the parser rejected the multi-line shapes, and the failure-case test failed on the new
cases the old parser accepted or worded differently (the `#` item). After the implementation the failure cases were split
into two tests to satisfy detekt `LongMethod`; both and the accepting test pass (27 tests, `BUILD SUCCESSFUL`).

Spotless (`--tests '*FormattingFunctionalTest*'`, unchanged build): 12 tests completed, 2 failed.

```
FormattingFunctionalTest > a tracked file is formatted even where gitignore matches it() FAILED
    org.opentest4j.AssertionFailedError at FormattingFunctionalTest.kt:187
FormattingFunctionalTest > untracked files that git ignores are never formatted() FAILED
    org.gradle.testkit.runner.UnexpectedBuildFailure at FormattingFunctionalTest.kt:170
```

The third test (a project directory below the work tree root keeps only the static exclusions) passes on HEAD by design:
it is a guard that the fallback stays closed. After the implementation all 12 pass, with the ignored-files test asserting
`UP_TO_DATE` as the plan asked (the ignored files are not inputs).

## Renovate dry run (IN-04b)

`RENOVATE_BASE_DIR=<scratch> LOG_LEVEL=debug mise x -- renovate --platform=local --dry-run=extract` (Renovate 44.252.8),
before and after the change, on the tree that already holds the composite action. The `Matched` sequences are identical:

```
buildpacks 2, bun 3, docker-compose 5, github-actions 7, gradle 38, gradle-wrapper 1, html 4, mise 1, npm 3, nvm 1,
pep621 1, pixi 1, poetry 1, renovate-config 1, regex 1/1/1/1/1/2, jsonata 7
```

github-actions is 7, which is the 6 of BASE plus the new action.yml. After the change: `"packageFile": "scripts/ci/` 0,
`storage-download.googleapis` 0, `"packageFile": "settings.gradle.kts"` 1. `renovate-config-validator --strict` exits 0.

## Live proofs (Spotless)

- An unformatted `.playwright-mcp/probe.json` (git-ignored in this repository): `spotlessCheck` ended `BUILD SUCCESSFUL`;
  the probe directory was deleted afterwards.
- Configuration cache: `spotlessCheck` twice (second run `Reusing configuration cache.`), then
  `backend/zz-probe/build/x.txt` created and `spotlessCheck` again: `Reusing configuration cache.`; the probe was deleted.
  The first attempt, before the directory filter existed, printed `cannot be reused because a build logic input of type
  'GitIgnoredPaths' has changed`, see the deviations.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] zizmor 1.32.0 flags `uses: ./.github/actions/...` as `self-repository`**
- **Found during:** Task 2, Part A
- **Issue:** the plan's `./` reference produced five low findings (exit 12), contradicting "zizmor is clean". The `$/` form
  that the audit asks for is rejected by actionlint ("ref is missing").
- **Fix:** kept `./.github/actions/maven-central-mirror` (the form the plan's grep gates expect and actionlint parses),
  added `# zizmor: ignore[self-repository]` on the five `uses:` lines and explained it in the ci.yml header. actionlint,
  and zizmor over workflows plus actions, are clean (`5 ignored`).
- **Files modified:** .github/workflows/ci.yml
- **Commit:** b8061f1

**2. [Rule 1 - Bug] `GitIgnoredPaths` value changed when a directory held only build outputs**
- **Found during:** Task 3 live proof
- **Issue:** git lists a wholly ignored directory such as `backend/zz-probe/` as an entry of its own, so a leftover module
  that holds only a `build` directory changed the value and invalidated the configuration cache, contradicting the plan's
  expectation.
- **Fix:** a directory entry is dropped when it holds no file outside the covered directories (a `Files.walkFileTree` walk
  that skips covered names and does not follow links). The live proof then printed `Reusing configuration cache.`
- **Files modified:** GitIgnoredPaths.kt
- **Commit:** e05f361

**3. [Rule 2 - Missing critical functionality] Comments inside a multi-line list could hide in an item**
- **Found during:** Task 1, parser
- **Issue:** a trailing comment after an item without a comma (`a # note` then `]`) joins into the bare item `a # note`.
- **Fix:** a bare item that holds whitespace followed by `#`, or a `#` where a separator is expected, fails with
  `comments inside a list are not supported`. Test cases cover both.
- **Files modified:** FrontMatter.kt, UpstreamRecordCheckFunctionalTest.kt
- **Commit:** 56dc4c0

**4. [Rule 3 - Blocking] detekt limits**
- **Found during:** Task 1 and Task 3
- **Issue:** `ReturnCount` (FrontMatter `nextItemFollows`, `GitIgnoredPaths.obtain`) and `LongMethod` (the failure-case test).
- **Fix:** split helpers and split the test into two with a shared `assertFrontMatterFails`; no rule was suppressed.
- **Commits:** 56dc4c0, e05f361

The `kotlin-dsl` plugin makes the receiver of a Gradle `Action` lambda `this`, so `exec.exec { ... }` in
`GitIgnoredPaths` uses `this`, not `it` (a compile error found and fixed while writing it, no deviation in behaviour).

## Known Stubs

None.

## Threat Flags

None. The composite action has no inputs and no expressions, `.github/**` stays governed, and `GitIgnoredPaths.kt` is a
governed path with its ask rule (T-mwg-01, T-mwg-02, T-mwg-07).

## Gates

On the final HEAD `e05f361`, from the worktree root: actionlint, zizmor over `.github/workflows .github/actions` (offline),
shellcheck over every tracked `.sh`, editorconfig-checker, `reuse lint`, the CI index normalisation step,
`renovate-config-validator --strict`, `docker compose config --quiet` and every tracked shell test suite (0 failed
everywhere) passed. The Gradle gates `spotlessCheck checkUpstreamRecords checkAdrs checkClaudeAskRules checkBuildScripts
checkToolingInventory :build-logic:check` (detekt, Dokka, full TestKit suite): see the final line below. The scope gate
`git diff --name-only 9ae2d94 HEAD -- ':!.planning'` lists exactly the plan's `files_modified`.

The one local difference from CI: `zizmor` ran offline, because the online audits need the job token.

Final Gradle run on `e05f361`: `BUILD SUCCESSFUL in 9m 4s` for `spotlessCheck checkUpstreamRecords checkAdrs
checkClaudeAskRules checkBuildScripts checkToolingInventory :build-logic:check`, including the full `:build-logic:functionalTest`.

## Self-Check: PASSED

- Created files exist: `.github/actions/maven-central-mirror/action.yml`, `GitIgnoredPaths.kt`.
- Commits exist on the branch: `bfa1c20`, `4fc8521`, `56dc4c0`, `b8061f1`, `1ae2c76`, `e05f361`.
