---
phase: quick-261122-gdy
plan: 01
subsystem: build-logic, formatting
tags: [spotless, ktfmt, prettier, taplo, detekt, line-width]
requires: []
provides:
  - "120-column limit across ktfmt, Prettier and taplo, proven by TestKit"
  - "detekt MaxLineLength 140 backstop"
affects: [all formatted artifacts]
tech-stack:
  added: []
  patterns: ["one shared maxLineWidth val on the ktfmt step", "TestKit 120/121 boundary helper"]
key-files:
  created:
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt
  modified:
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - .editorconfig
    - config/detekt/detekt.yml
    - .prettierrc.yaml
    - .taplo.toml
    - .planning/PROJECT.md
decisions:
  - "Maximum line width 120 for every formatted artifact (author decision 2026-11-22), recorded in PROJECT.md"
metrics:
  duration: ~25 min
  completed: 2026-11-22
status: complete
requirements: [TEST-09, INFRA-14, TEST-13, INFRA-12]
actuals:
  tasks: 3
  commits: 4
plan_head_before: 4fae937355b95ca73d9f78d0e2b13da675a112fb
plan_head_after: 1e59070
---

# Phase quick-261122-gdy Plan 01: Raise the maximum line width to 120 Summary

Every formatter (ktfmt for Kotlin and KTS, Prettier for JSON, YAML and Markdown, taplo for TOML) now wraps at 120
columns, proven by TestKit tests at the 120/121 boundary; the tree is reformatted by `spotlessApply` alone.

## Tasks

| Task | Commit | Result |
| ---- | ------ | ------ |
| 1. Kotlin/KTS at 120 (tracer) | d37258b | `LineWidthFunctionalTest` Kotlin test; `maxLineWidth` val and `setMaxWidth` on both ktfmt steps; `.editorconfig` Kotlin section; detekt `MaxLineLength` 140 |
| 2. JSON, YAML, Markdown, TOML at 120 | 157359a | JSON and TOML boundary tests; Prettier `printWidth` 120; taplo `column_width` 120; `.editorconfig` TOML section and corrected comment |
| 3. Reformat and record decision | 65c3f8b, 1e59070 | 26 files reformatted; PROJECT.md Key Decisions row |

RED evidence: at HEAD the Kotlin test failed because the 120-column kept files were reported by `spotlessKotlinCheck`
(output named `Kept.kt` and `KeptScript.kts`); the JSON and TOML tests failed the same way (`kept.json`,
`kept.toml`) before the Prettier and taplo edits. No compile errors.

## Verification

- Token gate (word diff outside `.planning`, before staging): 0 segments other than whitespace, commas and `*`;
  `git diff` was non-empty, so `spotlessApply` rewrote files (26 `.kt`/`.kts` files; no JSON, YAML or TOML file
  needed a rewrite).
- `./gradlew build` passed (spotlessCheck, detekt with 140, Dokka, all TestKit tests including the three
  `LineWidthFunctionalTest` tests, governance checks).
- `editorconfig-checker`, `reuse lint`, `actionlint`, `zizmor --offline --no-progress .github/workflows` and
  `renovate-config-validator --strict renovate.json5` passed. The validator prints a pre-existing RE2 native-module
  warning (falls back to RegExp) and still validates successfully.
- Configuration grep gates of Tasks 1 and 2 all hold.

## Deviations from Plan

None in behaviour. Two environment notes:

- The worktree branch is `worktree-agent-a67e47aadd0bbdd78`, not `gsd/phase-01-line-width-120` (the executor ran in
  an isolated worktree; no branch was created or switched by hand). The orchestrator merges.
- The sandbox refused git commands containing `;`, `&&` chains or redirects, so the two git gates were run as plain
  commands (`git diff --stat`, and the word diff piped straight into `grep`); the results are equivalent.
- The lefthook format hook reflowed the new test file at 120 columns on the first commit, which is a self-check of
  the new limit.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- LineWidthFunctionalTest.kt exists; commits d37258b, 157359a, 65c3f8b, 1e59070 are on the branch; working tree clean.
