---
phase: 01-repository-build-ci-foundation
plan: 07
subsystem: infra
tags: [gradle, governance, build-logic, testkit, gsd-config, daemon-jvm, build-scripts-lint]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-02: courses.root, GradleFixture, TestKit functionalTest suite, gradle-daemon-jvm.properties generated from .java-version; plan 01-05: formatting wiring and the mise-based Gradle invocation"
provides:
  - "courses.governance convention plugin (applied to the root project) with three cacheable check tasks wired into check"
  - "checkGsdConfigPolicy: whole-file semantic comparison of .planning/config.json with governance/gsd-config-policy.json, reporting added, removed and changed key paths (never values)"
  - "checkBuildScripts: version-literal, eager-api and cross-project rules over *.gradle.kts, gradle.properties and build-logic Kotlin sources, reported as path:line: [rule] sorted by path then line"
  - "checkDaemonJvm: .java-version major must equal toolchainVersion in gradle/gradle-daemon-jvm.properties, with an updateDaemonJvm hint"
  - "governance/gsd-config-policy.json: exact copy of the committed .planning/config.json"
  - "GradleFixture.governance(projectDir) factory and GradleFixture.PASSED outcome set"
affects: [01-09, 01-10, 01-11, 01-12, phase-02]

plan_head_before: 20a3c84c6f44611c6c440707a8edb3e9e4f6b87d
plan_head_after: 9c5b1048c0a9fef52fe09f2936cbc56f5235f782

actuals:
  tokens: 8900
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Governance checks are @CacheableTask classes in build-logic with a plugin script that only wires them; no Project access in task actions"
    - "Optional-input checks declare ConfigurableFileCollection inputs so a missing file yields a message naming it, not a Gradle input-validation error"
    - "Lint rules are escaped regexes so the scanner source does not match its own rules; rule changes are made with a test, never a suppression list"

key-files:
  created:
    - build-logic/src/main/kotlin/courses.governance.gradle.kts
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/BuildScriptLintCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/DaemonJvmConsistencyCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheckFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/BuildScriptLintCheckFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DaemonJvmCheckFunctionalTest.kt
    - governance/gsd-config-policy.json
  modified:
    - build.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt

key-decisions:
  - "The config policy report lists key paths only, never values, because a GSD configuration file can hold credentials and Gradle output ends up in CI logs"
  - "Governance tests assert outcome in {SUCCESS, FROM_CACHE} (GradleFixture.PASSED), because TestKit shares the user's Gradle home and its build cache can answer an identical fixture build"
  - "checkBuildScripts excludes build output and tool-owned or vendored directories (build, .gradle, .kotlin, node_modules, .git, .idea, .mise, .planning, .claude) and the TestKit sources, whose test data legitimately contains literals"
  - "gradle.properties is scanned at the repository root only, as the plan lists it; a nested gradle.properties would need its own include"

patterns-established:
  - "Tracer-then-TDD split for a governance plugin: one end-to-end check first (Task 1), then the remaining checks test-first (Task 2)"
  - "A failing governance check names its remedy in the message (reset transient keys, update the policy in the same PR, run updateDaemonJvm, use the catalog and lazy APIs)"

requirements-completed: [AIDEV-05, INFRA-09, INFRA-10]

coverage:
  - id: D1
    description: "checkGsdConfigPolicy passes for a semantically equal config and fails on any added, removed or changed key (including inside lists), listing each key path and naming transient GSD keys and the same-PR policy rule"
    requirement: AIDEV-05
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../governance/ConfigPolicyCheckFunctionalTest.kt (8 tests)"
        status: pass
      - kind: other
        ref: "mise x -- ./gradlew checkGsdConfigPolicy (green on the repository)"
        status: pass
    human_judgment: false
  - id: D2
    description: "governance/gsd-config-policy.json is an exact copy of .planning/config.json"
    requirement: AIDEV-05
    verification:
      - kind: other
        ref: "jq -S . governance/gsd-config-policy.json | diff - <(jq -S . .planning/config.json) (no output)"
        status: pass
    human_judgment: false
  - id: D3
    description: "checkBuildScripts rejects version literals (GAV strings, plugin versions, version assignments, numeric jvmToolchain and JavaLanguageVersion.of) and eager or cross-project APIs, with sorted path:line: [rule] output, and passes on the repository"
    requirement: INFRA-09
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../governance/BuildScriptLintCheckFunctionalTest.kt (9 tests)"
        status: pass
      - kind: other
        ref: "mise x -- ./gradlew checkBuildScripts (green on the repository; a planted literal failed it and the second clean run was UP-TO-DATE)"
        status: pass
    human_judgment: false
  - id: D4
    description: "checkDaemonJvm fails when .java-version and toolchainVersion differ, when .java-version is missing, empty or non-numeric, or the properties file is missing or lacks the key, and names updateDaemonJvm"
    requirement: INFRA-09
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../governance/DaemonJvmCheckFunctionalTest.kt (8 tests)"
        status: pass
      - kind: other
        ref: "mise x -- ./gradlew checkDaemonJvm (green on the repository)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The three checks are cacheable tasks wired into root check through courses.governance; re-running with unchanged inputs is UP-TO-DATE or FROM-CACHE"
    requirement: INFRA-10
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew check --dry-run lists :checkGsdConfigPolicy, :checkBuildScripts, :checkDaemonJvm; second checkBuildScripts run reported UP-TO-DATE"
        status: pass
      - kind: integration
        ref: "mise x -- ./gradlew check (BUILD SUCCESSFUL, 1m 34s)"
        status: pass
    human_judgment: false
  - id: D6
    description: "The prohibition on silently relaxing governance (no ignoring of underscore-prefixed transient keys, no widened patterns) holds"
    verification: []
    human_judgment: true
    rationale: "Judgment-verified prohibition: no automated test can show that a future change did not weaken a rule; reviewers must look at any diff to the policy file, rules or exclude lists. This plan relaxed nothing."

duration: 20min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 07: Repository Governance Checks Summary

**Three cacheable `courses.governance` checks in `./gradlew check`: whole-file GSD config policy (key paths only), no version literals or eager and cross-project build APIs, and `.java-version` equal to the daemon JVM toolchain, with 25 TestKit tests behind them**

## Performance

- **Duration:** about 20 min
- **Started:** about 2026-11-21T09:55Z
- **Completed:** 2026-11-21T10:16Z
- **Tasks:** 2
- **Files modified:** 10 (8 created, 2 modified)

## Accomplishments

- Tracer slice proven end to end: policy file, task class, plugin, root wiring and 8 TestKit tests; the tracer `<verify>` is automated-only, both commands passed, logged "Tracer verified end-to-end - expanding".
- `checkGsdConfigPolicy` compares parsed JSON recursively (map by key, list by index, numbers by value), so key order and whitespace never matter; the failure message names `workflow._auto_chain_active` as an example of a transient key and says the policy changes only deliberately in the same PR.
- `checkBuildScripts` passes on the repository without any suppression: a planted `implementation("a.b:c:1.0")` in a new `*.gradle.kts` failed it with `tooling/zz-probe.gradle.kts:1: [version-literal]`, and the build was green again after removal.
- `checkDaemonJvm` takes both inputs as file collections so a missing or empty `.java-version` produces `.java-version is missing` or `.java-version is empty` rather than a generic input-validation failure.
- Task 2 went RED (17 of 17 new tests failed: tasks did not exist) then GREEN.

## Task Commits

1. **Task 1: End-to-end config policy (tracer)** - `8519e37` (feat)
2. **Task 2 (TDD):**
   - RED: `be50ca2` (test) - 17 failing tests for build-script lint and daemon-JVM consistency
   - GREEN: `9c5b104` (feat) - `BuildScriptLintCheck`, `DaemonJvmConsistencyCheck`, plugin wiring, test fixes

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

## Files Created/Modified

- `build-logic/src/main/kotlin/courses.governance.gradle.kts` - registers `checkGsdConfigPolicy`, `checkBuildScripts`, `checkDaemonJvm`, wires them into `check`
- `.../governance/ConfigPolicyCheck.kt`, `BuildScriptLintCheck.kt`, `DaemonJvmConsistencyCheck.kt` - the three `@CacheableTask` classes
- `.../governance/*FunctionalTest.kt` - 8 + 9 + 8 TestKit tests
- `governance/gsd-config-policy.json` - exact copy of `.planning/config.json` (Prettier-formatted by `spotlessApply`; the comparison is semantic)
- `build.gradle.kts` - applies `courses.governance` next to `courses.root`
- `GradleFixture.kt` - additive: `governance(projectDir)` factory and the `PASSED` outcome set

## Decisions Made

See `key-decisions`. The two that matter for later plans: reports carry key paths only (no values), and `checkBuildScripts` scans the root `gradle.properties` plus every `*.gradle.kts` and build-logic Kotlin source, so Phases 2, 5 and 6 get the literal check for the scripts they add with no further wiring.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] TestKit tests assumed task outcome SUCCESS**
- **Found during:** Task 2 (first full `./gradlew check`)
- **Issue:** `BuildScriptLintCheckFunctionalTest > a build without version literals passes` failed with `expected SUCCESS but was FROM_CACHE`: TestKit shares the user's Gradle home, and an identical earlier fixture build was answered from the build cache. The same latent flake existed in the Task 1 test and the daemon tests.
- **Fix:** added `GradleFixture.PASSED` (`SUCCESS` or `FROM_CACHE`) and asserted membership in all three governance test classes.
- **Files modified:** `GradleFixture.kt`, the three `*FunctionalTest.kt` files
- **Verification:** `./gradlew check` green; filtered runs with `--rerun` green.
- **Committed in:** `9c5b104`

**2. [Rule 1 - Bug] detekt MagicNumber and editorconfig-checker findings in the new tests**
- **Found during:** Task 2 (`./gradlew check`, `editorconfig-checker`)
- **Issue:** `listOf(1, 2, 3, 4).forEach` and `"\n".repeat(9)` tripped `MagicNumber`; the 2-space JSON in a raw string tripped editorconfig-checker (`want multiple of 4`).
- **Fix:** tests assert the full expected list of `path:line: [rule]` strings; the sort test uses nine padding comment lines; the JSON sample is indented by 4 inside the raw string. No rule was disabled.
- **Files modified:** `BuildScriptLintCheckFunctionalTest.kt`, `ConfigPolicyCheckFunctionalTest.kt`
- **Verification:** `./gradlew check` and `editorconfig-checker` green.
- **Committed in:** `9c5b104`

---

**Total deviations:** 2 auto-fixed (2 bugs in test code)
**Impact on plan:** none on scope or production code. The plan's `files_modified` did not list `GradleFixture.kt`; the additive `governance` factory and `PASSED` set were needed to reuse it as the plan instructs.

## Issues Encountered

- The harness refused compound shell commands that included git or tool tokens; they were split into plain commands.
- `checkBuildScripts` reports `FROM-CACHE` rather than a fresh run after a planted literal is removed, because that exact input set was cached earlier; this is the intended cache behaviour.

## Verification Results and Timing

| Command (warm Gradle and TestKit caches) | Result | Wall clock |
| --- | --- | --- |
| `mise x -- ./gradlew :build-logic:functionalTest --tests '*ConfigPolicyCheckFunctionalTest*'` (8 tests, `--rerun`) | pass | 14 s |
| `mise x -- ./gradlew checkGsdConfigPolicy` | pass | under 1 s |
| `mise x -- ./gradlew :build-logic:functionalTest --tests '*BuildScriptLintCheckFunctionalTest*' --tests '*DaemonJvmCheckFunctionalTest*'` (17 tests, `--rerun`) | pass | 34 s |
| `mise x -- ./gradlew checkBuildScripts checkDaemonJvm` | pass | under 1 s |
| `mise x -- ./gradlew check` (whole repository) | pass | 1 min 34 s |

Every `<automated>` command is well within the 180 s per-task feedback budget; the filtered functionalTest runs exceed 30 s as expected and none is a validation gap. Other gates: `mise x -- reuse lint` compliant, `mise x -- editorconfig-checker` exit 0, `jq -S` diff of policy against config empty.

## Known Stubs

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 01-09 can mark `governance/` a governed path (the `governance-approved` label); plan 01-10 reuses `courses.governance` for the tooling-inventory and PR-template checks; plan 01-12 runs these checks through CI `check`.
- Standing obligation: any GSD run that rewrites `.planning/config.json` with transient keys makes local `./gradlew check` fail until the keys are reset, which is the intended AIDEV-05 signal.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*

## Self-Check: PASSED
