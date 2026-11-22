---
phase: quick-261122-kqn
plan: 01
subsystem: ci
tags: [qodana, ci, inspections, renovate, governance]
requires: []
provides:
  - Required `qodana` CI job aggregated by `ci-success`
  - qodana.yaml with the single digest-pinned image, profile Default, failThreshold 0
  - scripts/qodana.sh, the one way to run the gate locally and in CI
  - Renovate coverage and the deps:docker label for the Qodana image
affects: [ci, renovate, docs/tooling.md, governance]
tech-stack:
  added: [Qodana JVM Community 2026.3 (container image, digest-pinned), actions/cache v6.1.0]
  patterns: [image pinned once as tag@digest in its tool's native config, plain docker run through a script]
key-files:
  created: [qodana.yaml, scripts/qodana.sh]
  modified:
    - .github/workflows/ci.yml
    - .github/labels.yml
    - renovate.json5
    - docs/tooling.md
    - .planning/PROJECT.md
    - .planning/ROADMAP.md
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt
decisions:
  - Qodana image pinned once as tag@digest in qodana.yaml (`linter:`), updated by a Renovate regex manager
  - Plain docker run through scripts/qodana.sh instead of JetBrains/qodana-action
  - 2026.3 adopted under the seven-day cooldown, an author-approved IDE-parity exception
requirements: [CICD-01, CICD-04, INFRA-09, INFRA-14, INFRA-16, GH-11]
status: complete
actuals:
  tokens: 60000
  tasks: 3
  commits: 3
plan_head_before: 41f8bef1d7446169f16189b8f5997be8c88cb9c5
plan_head_after: 3198d84586e8d983fae58d8b94b72c896a7a5ab4
completed: 2026-11-22
---

# Phase quick-261122-kqn Plan 01: Qodana as a required CI gate Summary

Qodana JVM Community 2026.3 (digest-pinned in `qodana.yaml`, profile `Default`, `failThreshold: 0`, no baseline) runs through `scripts/qodana.sh` in a new required `qodana` job; the tree starts at zero findings after resolving the six Kotlin findings.

## Tasks and commits

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Tracer: qodana.yaml, scripts/qodana.sh, the required CI job | 03ff0fb |
| 2 | Resolve the six Kotlin findings | f02301c |
| 3 | Renovate, deps:docker label, docs, planning records | 3198d84 |

## Real Qodana runs

| Run | Tree | Exit code | SARIF results | Wall time |
| --- | ---- | --------- | ------------- | --------- |
| Task 1 (tracer) | HEAD 03ff0fb, before the Kotlin fixes | 255 | 6 | 59 s |
| Task 2 | HEAD f02301c, after the Kotlin fixes | 0 | 0 | 48 s |

The Task 1 run reported exactly the six known findings and nothing else: SameParameterValue at ExecutableOnPathFunctionalTest.kt:231, CollectionConcatenationToBuildCollection at AskRulesCheck.kt:86, PrTemplateCheck.kt:46 and ToolingTableCheck.kt:45, IntroduceWhenSubject at ConfigPolicyCheck.kt:95, and ConvertCallChainIntoSequence at ToolingTableCheck.kt:88. No EditorConfigOptionRedundancy result and no result under a build directory appeared, so the exclusions and `failThreshold: 0` work as planned. Both runs used the warm local cache and the already-pulled image, so these times are far below the planned 10 minutes; a cold CI run still needs the image pull and the Gradle and IDE downloads (the plan estimated about 35 minutes cold).

The local Qodana cache (`QODANA_WORK_DIR/cache`) is 3.3 GB, a rough upper bound for the CI cache entry.

## Verification

- `./gradlew build` passes (full build after Task 2 in 5m53s, again after Task 3), including every governance TestKit test.
- shellcheck, actionlint, `zizmor --offline`, `renovate-config-validator --strict`, `reuse lint` and `editorconfig-checker` pass. The validator prints a pre-existing local RE2 warning (the native module is missing), which does not affect the result.
- `renovate --platform=local --dry-run=extract` shows `jetbrains/qodana-jvm-community` from qodana.yaml with currentValue `2026.3` and the `sha256:5420d961...` digest.
- scripts/qodana.sh rejects an unpinned linter (exit 1, fixed message) and a directory containing `.idea/` (exit 1) without calling Docker.

## Deviations from Plan

None - plan executed exactly as written.

The branch is the orchestrator's worktree branch `worktree-agent-ad52fe613da7daa50`, not the `gsd/phase-01-qodana-gate` named in the plan, because the task ran in an isolated worktree.

## Authentication gates

None.

## Known Stubs

None.

## Threat Flags

None. The new surface (the `qodana` job, the cache, the image pull) is covered by the plan's threat model T-kqn-01 to T-kqn-09.

## Notes for the maintainer

- The pull request touches `.github/**`, `renovate.json5` and `build-logic/.../governance/**`, so it needs the `governance-approved` label, which the author applies.
- The first CI run on main writes the Qodana cache; pull request runs only restore it.
- `qodana.yaml` and `scripts/qodana.sh` stay outside `governance/governed-paths.txt` by design, as recorded in the plan.

## Self-Check: PASSED

- qodana.yaml, scripts/qodana.sh (mode 100755) and this SUMMARY exist.
- Commits 03ff0fb, f02301c and 3198d84 are on the branch; `git rev-list --count 41f8bef..HEAD` is 3.
