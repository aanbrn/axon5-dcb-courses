---
phase: 01-repository-build-ci-foundation
plan: 21
subsystem: build-logic
tags: [gradle, value-source, node, prettier, spotless, configuration-cache, testkit, gap-closure]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "01-04 verifyNodeVersion and the Spotless wiring; 01-05 taplo and shfmt verify tasks; GradleFixture and FormattingFunctionalTest"
provides:
  - "ExecutableOnPath value source: first executable regular file of a name on the build's own PATH, absolute entries only, as an absolute path"
  - "verifyNodeVersion runs the resolved absolute node and Spotless Prettier runs the same node through nodeExecutable (G-01-2 closed)"
  - "taplo and shfmt resolved by the same value source; the shell-based lookup launched by bare name is gone"
  - "ExecutableOnPathFunctionalTest with five TestKit tests, (a) to (e)"
affects: [phase-01-verification, 01-UAT test 2 re-verification]

actuals:
  tokens: 6400
  tasks: 2
  commits: 6
plan_head_before: 0ac7ec320832f4fd3f68a785ce4f088e5b350823
plan_head_after: 1b7cd6c5b15f18bc07273c725a7e05faa77f5219

tech-stack:
  added: []
  patterns:
    - "Programs launched by build-logic are resolved by one value source over System.getenv(\"PATH\") and exec'd by absolute path, never by bare name"
    - "A configuration-cache input that must track a resolved binary is a ValueSource, not a direct PATH read"

key-files:
  created:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPath.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt
  modified:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt

key-decisions:
  - "Spotless gets nodeExecutable only, not npmExecutable (P-1): Spotless takes npm from the node directory when it is there and starts it with that directory first on PATH"
  - "Relative and empty PATH entries are skipped by the resolver (P-3)"
  - "Production wiring stays lazy; the configuration-cache precision of the value source is proven in the fixture's own build script (F-1, author option B)"
  - "node-major.txt keeps the major only (P-4)"

patterns-established:
  - "TestKit daemon isolation by a per-test java.io.tmpdir in org.gradle.jvmargs, with the daemon PID asserted"

requirements-completed: [TEST-09, INFRA-09, INFRA-10]

coverage:
  - id: D1
    description: "In a warm daemon, a node switched on PATH between builds is the node verifyNodeVersion checks (G-01-2 cause 1)"
    requirement: "INFRA-09"
    verification:
      - kind: integration
        ref: "ExecutableOnPathFunctionalTest#a node switched on PATH between builds of one daemon is the node verifyNodeVersion checks"
        status: pass
    human_judgment: false
  - id: D2
    description: "Spotless Prettier's server runs the node the guard checked, also with no npm beside it (G-01-2 cause 2)"
    requirement: "TEST-09"
    verification:
      - kind: integration
        ref: "ExecutableOnPathFunctionalTest#Prettier runs the node verifyNodeVersion checked even with no npm beside it"
        status: pass
    human_judgment: false
  - id: D3
    description: "Without node on PATH, configuration succeeds and verifyNodeVersion fails first with the existing message"
    requirement: "TEST-09"
    verification:
      - kind: integration
        ref: "ExecutableOnPathFunctionalTest#without node on PATH configuration succeeds and verifyNodeVersion fails before Prettier"
        status: pass
    human_judgment: false
  - id: D4
    description: "The ExecutableOnPath value source is a precise configuration-cache input: reused when no resolved binary changes (relative, non-executable, directory and empty entries skipped), invalidated naming ExecutableOnPath when node, taplo or shfmt changes"
    requirement: "INFRA-10"
    verification:
      - kind: integration
        ref: "ExecutableOnPathFunctionalTest#the configuration cache survives a PATH change that resolves the same node and not one that does not"
        status: pass
      - kind: integration
        ref: "ExecutableOnPathFunctionalTest#a taplo or shfmt switched on PATH invalidates a configuration cache that realises the resolver"
        status: pass
    human_judgment: false
  - id: D5
    description: "No process in the build-logic main sources is launched by a bare program name; only ExecutableOnPath.kt reads the environment"
    requirement: "INFRA-10"
    verification:
      - kind: other
        ref: "grep -rnE 'commandLine\\(\"(node|sh)\"' build-logic/src/main prints nothing; grep -rl getenv build-logic/src/main prints ExecutableOnPath.kt only"
        status: pass
    human_judgment: false

duration: 75min
completed: 2026-11-22
status: complete
---

# Phase 01 Plan 21: Resolve node from the build's PATH Summary

**`ExecutableOnPath` value source resolves `node`, `taplo` and `shfmt` from each build's own `PATH`, so `verifyNodeVersion` and Spotless Prettier run the same absolute node even in a warm Gradle daemon (G-01-2).**

## Performance

- **Duration:** about 75 min over four sessions (a fresh agent each time after three STOPs)
- **Tasks:** 2 of 2
- **Files:** 2 created, 3 modified

## Accomplishments

- G-01-2 cause 1 closed: `verifyNodeVersion` execs the absolute path found on `System.getenv("PATH")` instead of a bare `node`, which the JDK resolved against a `PATH` it cached when the daemon JVM launched its first process.
- G-01-2 cause 2 closed: Spotless Prettier gets the same path through `nodeExecutable(...)`; test (b) shows its `serve.js` start runs the logging shim, with no npm beside it.
- `taplo` and `shfmt` come from the same value source (empty string when missing, keeping `VerifyFormatterBinary`'s contract). The `sh -c 'command -v'` helper is deleted.
- Five TestKit tests in the new `ExecutableOnPathFunctionalTest`.

## Task Commits

Task commits and plan commits inside the window `0ac7ec3..1b7cd6c`, measured with `git rev-list --count` as 6:

1. **Task 1 RED:** `3552a3c` test(01-21): reproduce the stale node lookup in a warm daemon (G-01-2)
2. **Task 1 GREEN:** `0156948` fix(01-21): run the node on the build's PATH in verifyNodeVersion and Prettier (G-01-2)
3. **Task 2 tests:** `9459c48` test(01-21): prove ExecutableOnPath's configuration-cache precision in isolation (G-01-2)
4. **Task 2 GREEN:** `1b7cd6c` fix(01-21): resolve taplo and shfmt through ExecutableOnPath (G-01-2)

Orchestrator commits inside the window (plan corrections, not task commits): `88af1c0` (working-directory mechanism behind test (d)) and `039551a` (finding F-1 and option B).

## Task 1 evidence (RED, GREEN)

RED, on unchanged main sources:

- (a) failed at `ExecutableOnPathFunctionalTest.kt:63` on assertion ii (`collection is []`), after assertion i (daemon-PID equality, line 61) passed and before build 3 ran. A scratch run printed `daemon-pid=35321` for all three builds.
- (b) failed at line 96: the shim log was empty.
- (c) failed at line 108: `:verifyNodeVersion` was SUCCESS without node on `PATH`.

GREEN: the `ExecutableOnPath` value source; `VerifyNodeVersion` takes an `@Input @Optional executable` and execs it by absolute path; `courses.root.gradle.kts` gains `executableOnPath(name)`, `nodePath` and `nodeExecutable(...)`; detekt `ThrowsCount` in `activeNodeVersion` fixed in the same task (`result == null || exitValue != 0`) with messages and cause unchanged. 12 functional tests and `./gradlew check` were green.

## Task 2 evidence

History: (1) the test (d) control first printed `relative-node-visible=false`, because the TestKit daemon's working directory is the test JVM's (`build-logic/`), not the fixture root; the plan was corrected in `88af1c0` (the relative entry is written relative to the test JVM's working directory, asserted not absolute). (2) Tests (d) and (e) then could not invalidate through the production wiring: the lazy providers are realised at configuration time only when Spotless tasks are in the graph, and Spotless itself reads `PATH` at configuration time (F-1). The author chose option B (`039551a`): production stays lazy and (d) and (e) realise `ExecutableOnPath` in the fixture's own script, never with a Spotless task in the graph.

Result of the adapted RED step: (d) and (e) passed already after Task 1, as the rewritten plan expects. Because Task 2's production change has no failing-test RED, the RED-side evidence is:

- `grep -c 'commandLine("sh"' build-logic/src/main/kotlin/courses.root.gradle.kts` printed 1 before GREEN, and 0 `commandLine("(node|sh)"` matches remain in `build-logic/src/main` afterwards.
- The nine `FormattingFunctionalTest` tests, including the missing-taplo and missing-shfmt hints, passed before and after GREEN.

Mutation check for the relative-entry rule (P-3): without the absolute-entry condition (`.filter { it.isNotEmpty() }`), (d) failed at build B on `expected:<Configuration cache entry reused>`, with the reason `Calculating task graph as configuration cache cannot be reused because a build logic input of type 'ExecutableOnPath' has changed`. Its control printed `relative-node-visible=true`, and `resolved-node=` was the absolute path of `relative-bin/node` resolved against the daemon's start directory. The other four tests passed. With the condition restored, `git status` showed no change to `ExecutableOnPath.kt`, and all five tests passed.

Final verification: 14 tests (5 new, 9 existing) green; `./gradlew check` green (detekt, Dokka, spotlessCheck, governance checks, whole functionalTest suite); `editorconfig-checker` exit 0; `reuse lint` compliant (155/155).

## Real-worktree warm-daemon reproduction (UAT test 2)

Shim directories lived under `/Users/afanasyev/.claude/jobs/cc9bd202/tmp/gsd-01-21-*`, not `/tmp`, and were removed afterwards. `mise x -- which node` printed `/Users/afanasyev/.local/share/mise/installs/node/24.22.3/bin/node`. The daemon was warmed with `help`.

- Logging shim, `verifyNodeVersion spotlessPrettierCheck --rerun-tasks`: BUILD SUCCESSFUL, `:verifyNodeVersion` and `:spotlessPrettier` executed. `calls.log` held `--version` and `serve.js --node-server-instance-id=9515b792-a203-48ef-a8ca-6216c7ae5274`. Configuration cache: `Calculating task graph as no cached configuration is available for tasks: verifyNodeVersion spotlessPrettierCheck` and `Configuration cache entry stored.`
- `v22.0.0` shim, `verifyNodeVersion`: FAILED with `Node.js 22 is active, but .nvmrc requires 24. Run \`mise install\` locally, or use setup-node with \`node-version-file: .nvmrc\` in CI.` Configuration cache line: `Calculating task graph as configuration cache cannot be reused because an input to plugin class 'org.gradle.kotlin.dsl.plugins.precompiled.PrecompiledScriptPlugins' has changed.`, then `Configuration cache entry stored.`
- Neither run printed `Starting a Gradle Daemon` or a single-use daemon notice, so the warm daemon was reused.
- Normal `PATH`, `verifyNodeVersion spotlessCheck`: BUILD SUCCESSFUL.

P-4 observation: a node path change alone does not re-run Prettier. With the same Node major, `spotlessPrettier` was `FROM-CACHE` after switching from the real node to the logging shim, and `UP-TO-DATE` on a repeat with the same `PATH`. The configuration cache was invalidated, naming `a build logic input of type 'ExecutableOnPath' has changed`. The first run above re-executed `spotlessPrettier` only because of `--rerun-tasks`.

## Deviations from Plan

### Plan corrections handled through the orchestrator

- `88af1c0` and `039551a` corrected the working-directory mechanism of test (d) and recorded finding F-1 (see Task 2 evidence). Both followed STOP rules in the plan, not executor deviations.

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Reflowed the comment ktfmt had wrapped after "same absolute"**
- **Found during:** Task 2 GREEN
- **Issue:** the `nodePath` comment in `courses.root.gradle.kts` read badly after ktfmt reflowed it to 100 columns.
- **Fix:** shortened the wording ("the same path") so it wraps naturally.
- **Files modified:** `build-logic/src/main/kotlin/courses.root.gradle.kts`
- **Commit:** `1b7cd6c`

None of the plan's tasks changed otherwise: executed as rewritten by the plan corrections.

## Threat Flags

None. The resolver narrows what the build runs (absolute entries only, executable regular files only); T-01-58 to T-01-60 are mitigated by tests (a), (b), (d) and the mutation run.

## Known Stubs

None.

## Governance

Only governed path edited: `build-logic/src/main/kotlin/courses.root.gradle.kts` (author-approved for this plan). `.nvmrc` and `.planning/config.json` are unchanged.

## Self-Check

- Files: `ExecutableOnPath.kt`, `ExecutableOnPathFunctionalTest.kt`, `VerifyNodeVersion.kt`, `courses.root.gradle.kts`, `GradleFixture.kt` all exist.
- Commits `3552a3c`, `0156948`, `9459c48`, `1b7cd6c` are ancestors of HEAD.

## Self-Check: PASSED
