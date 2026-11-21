---
phase: 01-repository-build-ci-foundation
plan: 05
subsystem: infra
tags: [spotless, taplo, shfmt, spdx, editorconfig, testkit, mise]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-04: Prettier format, .editorconfig with [*.sh] and [*.toml], FormattingFunctionalTest; plan 01-03: taplo and shfmt pinned in mise.toml"
provides:
  - "toml, shell and misc Spotless formats in courses.root: taplo, shfmt plus SPDX header on line 2, and generic whitespace steps for properties, scenarios, txt and dotfile configs"
  - "VerifyFormatterBinary task: a missing formatter binary fails only that file type's Spotless tasks with a mise install hint, and its path and version output are inputs of those tasks"
  - ".taplo.toml aligned with .editorconfig and config/spotless/license-header-shell.txt"
  - "GradleFixture.runWithout / runAndFailWithout (PATH without a named tool) and a standard fixture that carries .taplo.toml, .editorconfig and the shell header"
affects: [01-06, 01-08, 01-09, 01-11, 01-12]

plan_head_before: 10b8c3a9ebd2fe5b81a5805e9bc7935404ef3029
plan_head_after: 92b3430a131eaf6d48319516e6bedd3972deb98c

actuals:
  tokens: 12000
  tasks: 2
  commits: 3

tech-stack:
  added: [taplo 0.10.0 and shfmt 3.14.1 as formatter binaries (already pinned in plan 01-03)]
  patterns:
    - "Binary-dependent formatter: an environment-checking task writes the resolved path and version to a file that is an input of every Spotless task of that type"
    - "Optional formatter binaries are resolved at configuration time with providers.exec (command -v); a missing one never fails configuration"
    - "Shell script convention: line 1 shebang, line 2 '# SPDX-License-Identifier: MIT' (Spotless-managed), 2-space indentation (shfmt)"

key-files:
  created:
    - .taplo.toml
    - config/spotless/license-header-shell.txt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyFormatterBinary.kt
  modified:
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt

key-decisions:
  - "A missing taplo is replaced, at configuration time, by an existing file (.taplo.toml) that is never executed: Spotless hashes the executable when the configuration cache is stored, so a nonexistent placeholder path broke configuration instead of reaching the verifyTaplo hint"
  - "The missing-binary check is a task (VerifyFormatterBinary), not a doFirst: Spotless fingerprints its step state before any doFirst runs, so only a separate upstream task can own the error message"
  - "shfmt is wired with version '*' and an explicit pathToExe: the version is pinned once, in mise.toml; the binary version output is an input of the shell tasks"

patterns-established:
  - "Generated lockfiles stay outside every formatter: mise.lock is never matched (extension .lock) and .mise/** is in the shared excluded list"
  - "TestKit tests that need a binary missing from PATH build the environment from the real one minus the directories that contain it (GradleFixture.runWithout)"

requirements-completed: [TEST-09, INFRA-14, INFRA-16]

coverage:
  - id: D1
    description: "One ./gradlew spotlessApply formats TOML with taplo; unformatted TOML fails spotlessCheck, is fixed by spotlessApply, and a second run changes nothing"
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "FormattingFunctionalTest#unformatted TOML fails spotlessCheck and spotlessApply fixes it"
        status: pass
      - kind: other
        ref: "mise x -- ./gradlew spotlessApply (twice) then spotlessCheck: BUILD SUCCESSFUL, git status unchanged"
        status: pass
    human_judgment: false
  - id: D2
    description: "A missing taplo or shfmt fails only that type's Spotless tasks with a mise install hint; configuration (help) still succeeds"
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "FormattingFunctionalTest#a missing taplo fails the TOML tasks with a mise install hint but not configuration"
        status: pass
      - kind: integration
        ref: "FormattingFunctionalTest#a missing shfmt fails the shell tasks with a mise install hint but not configuration"
        status: pass
    human_judgment: false
  - id: D3
    description: "Shell scripts get the SPDX header on line 2 after the shebang, keep their descriptive comment, and are indented with 2 spaces by shfmt"
    requirement: INFRA-16
    verification:
      - kind: integration
        ref: "FormattingFunctionalTest#a shell script gets the SPDX header on line 2 and shfmt indentation"
        status: pass
    human_judgment: false
  - id: D4
    description: "Properties, scenarios, txt and dotfile configs lose trailing whitespace and gain a final newline through the misc format"
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "FormattingFunctionalTest#properties with trailing whitespace and no final newline fail spotlessCheck and are fixed"
        status: pass
    human_judgment: false
  - id: D5
    description: ".editorconfig agrees with taplo and shfmt: editorconfig-checker exits 0 on the tree formatted by spotlessApply; the Renovate annotation above prettier in libs.versions.toml survives taplo"
    requirement: INFRA-14
    verification:
      - kind: other
        ref: "mise x -- editorconfig-checker (exit 0)"
        status: pass
      - kind: other
        ref: "grep -B1 '^prettier = ' gradle/libs.versions.toml | head -1 | grep -q '^# renovate: datasource=npm depName=prettier$'"
        status: pass
    human_judgment: false
  - id: D6
    description: "Tool version and tool config as cache inputs: a changed taplo, shfmt or .taplo.toml/.editorconfig invalidates cached Spotless results"
    requirement: TEST-09
    verification: []
    human_judgment: true
    rationale: "The wiring (VerifyFormatterBinary output and the config file are declared inputs of every spotlessToml/spotlessShell task) is exercised only by the passing tasks; cross-version cache invalidation was not tested because only one taplo and one shfmt version are installed."

duration: 12min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 05: TOML, Shell and Text Formatting Summary

**The single `./gradlew spotlessApply` now also formats TOML with the mise-pinned taplo, shell scripts with shfmt plus an SPDX header on line 2, and properties, scenarios, text and dotfile configs with generic whitespace steps; a missing formatter binary fails only its own formatting tasks with a `mise install` hint**

## Performance

- **Duration:** about 12 min
- **Completed:** 2026-11-21T09:53Z
- **Tasks:** 2
- **Files modified:** 6 (3 created, 3 modified); the repository-wide `spotlessApply` changed no other file

## Accomplishments

- Tracer slice proven end to end: taplo behind `nativeCmd`, `VerifyFormatterBinary` as an upstream task, `.taplo.toml` and the binary output as inputs of every `spotlessToml*` task, and TestKit tests for the apply and missing-binary behaviours.
- Shell scripts: shfmt reads the `[*.sh]` section of `.editorconfig`; the SPDX line lands on line 2 under a shebang and only that line is managed (the delimiter `^(?!# SPDX-License-Identifier: )` leaves any other leading comment untouched).
- `misc` format covers properties, profiler scenarios, text files and the dotfile configs, excluding `gradle/gradle-daemon-jvm.properties`, the wrapper, `LICENSES/**` and build output.
- `mise.toml` and `gradle/libs.versions.toml` were already in taplo's format, so the repository-wide run changed no tracked file; the `# renovate:` comment still sits directly above `prettier = ...`. A second `spotlessApply` changed nothing and `editorconfig-checker` exits 0.
- `./gradlew check` is green (all earlier gates plus 8 `FormattingFunctionalTest` tests).

## Task Commits

1. **Task 1: End-to-end TOML (tracer)** - `65ae5fb` (feat)
2. **Task 2 (TDD):**
   - RED: `9d46ca9` (test) - three failing behaviours for shell, missing shfmt and properties
   - GREEN: `92b3430` (feat) - shell and misc formats, shfmt input wiring, detekt fix

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

Tracer feedback gate: the tracer `<verify>` is automated-only; the three automated commands passed after the commit (filtered `FormattingFunctionalTest`, then `spotlessApply` and `spotlessCheck`, then the Renovate-annotation grep), so expansion to Task 2 proceeded.

## Files Created/Modified

- `.taplo.toml` - taplo settings equal to the `[*.toml]` section of `.editorconfig` (2 spaces, 100 columns)
- `config/spotless/license-header-shell.txt` - the single line `# SPDX-License-Identifier: MIT`
- `build-logic/.../VerifyFormatterBinary.kt` - checks that a formatter binary runs, records its path and `--version` output, fails with a `mise install` hint
- `build-logic/src/main/kotlin/courses.root.gradle.kts` - `toml`, `shell` and `misc` formats, binary resolution and input wiring
- `FormattingFunctionalTest.kt`, `GradleFixture.kt` - five new tests; `runWithout` and `runAndFailWithout`; the standard fixture copies `.taplo.toml`, `.editorconfig` and the shell header

## Decisions Made

See `key-decisions` above. The two that shape later plans: the missing-binary check is a separate task (the same pattern as `verifyNodeVersion`), and the SPDX shell header uses a negative-lookahead delimiter so that only the SPDX line is managed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Placeholder path for a missing taplo broke the configuration cache**
- **Found during:** Task 1 (missing-taplo TestKit test)
- **Issue:** the plan allowed "a placeholder path whose step error is replaced by a clear message", but Spotless hashes the native executable when it stores the configuration cache entry, so a nonexistent placeholder failed configuration (`File signature can only be created for existing regular files, given: taplo`) before `verifyTaplo` could run.
- **Fix:** the placeholder is an existing file (`.taplo.toml`) that is never executed; `verifyTaplo` fails first with the hint.
- **Files modified:** `build-logic/src/main/kotlin/courses.root.gradle.kts`
- **Verification:** `a missing taplo fails the TOML tasks ...` passes: `help` succeeds, `spotlessTomlCheck` fails with `mise install` and `:verifyTaplo` FAILED.
- **Committed in:** `65ae5fb`

**2. [Rule 1 - Bug] detekt rejected the first VerifyFormatterBinary (ThrowsCount, missing KDoc)**
- **Found during:** Task 2 (`./gradlew check`)
- **Issue:** `verify()` had three throw statements and `registerBinaryCheck` had no KDoc.
- **Fix:** one throw guarded by a `runsVersion` helper; KDoc on the function.
- **Files modified:** `VerifyFormatterBinary.kt`, `courses.root.gradle.kts`
- **Verification:** `./gradlew check` BUILD SUCCESSFUL.
- **Committed in:** `92b3430`

**3. [Process] The first Task 1 commit was redone to keep TDD ordering**
- **Found during:** start of Task 2
- **Issue:** the first draft of the Task 1 commit already contained the shell and misc configuration, which would have made Task 2's RED step meaningless.
- **Fix:** the unpushed commit was soft-reset and recreated without the Task 2 parts; Task 2 then went RED (3 failures) then GREEN. Only my own unpushed commit was rewritten.
- **Files modified:** none beyond the plan's own files.

---

**Total deviations:** 3 (2 bugs auto-fixed, 1 process correction)
**Impact on plan:** none on scope; all behaviours in the plan are covered and tested.

## Issues Encountered

- The harness refused compound shell commands containing git-like tokens; they were split into plain commands.
- `taplo fmt -` logs an INFO line on stderr when it finds the config; Spotless reads stdout only, so it is harmless.
- No shell scripts exist yet (plans 01-08 and 01-09 add the first), so the shell format has been proven by TestKit only, not on repository files. The `misc` format likewise found nothing to change in the repository.

## Verification Results and Timing

| Command | Result | Wall clock |
| --- | --- | --- |
| `mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*'` (warm, 8 tests) | pass | 18 s to 28 s |
| `mise x -- ./gradlew spotlessApply` then `spotlessCheck` | pass, second apply changed nothing | under 1 s each (warm) |
| `mise x -- editorconfig-checker` | pass | under 1 s |
| `mise x -- ./gradlew check` (all gates, 14 TestKit tests) | BUILD SUCCESSFUL | 62 s |

The filtered `FormattingFunctionalTest` run stays under the 30 s expectation here and far below the 180 s budget of `01-VALIDATION.md`; no validation gap.

## Known Stubs

None.

## Threat Flags

None. T-01-39 mitigated as planned: the binaries are the mise-pinned ones, and their path and version output are inputs of the Spotless tasks (plus the executable hash in the taplo step). T-01-SC: no packages installed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 01-06 (REUSE) must still cover `mise.lock` and `.mise/locks/**`, which no formatter touches; shell scripts must use the line 1 shebang, line 2 SPDX convention.
- Plans 01-08 and 01-09 can add shell scripts: shfmt and the SPDX header are applied by `spotlessApply`.
- Plan 01-12 must install the mise tools before any Gradle step that formats or checks (`mise install`, then `mise x --` or an activated shell); a Gradle run without taplo and shfmt on `PATH` still configures and builds, but `spotlessCheck` fails with the `mise install` hint.

## Self-Check: PASSED

Created files exist on disk (`.taplo.toml`, `config/spotless/license-header-shell.txt`, `VerifyFormatterBinary.kt`); commits `65ae5fb`, `9d46ca9` and `92b3430` are ancestors of HEAD; acceptance criteria hold (`format("toml")` 1, `column_width = 100` 1, `mise install` in the root script 1, `shfmt` 3, `format("misc")` 1, shell header file exactly `# SPDX-License-Identifier: MIT`); plan-level verification passes (`spotlessApply` idempotent, `spotlessCheck`, `editorconfig-checker` and `check` green).

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
