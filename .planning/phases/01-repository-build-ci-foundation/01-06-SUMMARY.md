---
phase: 01-repository-build-ci-foundation
plan: 06
subsystem: infra
tags: [reuse, spdx, gitattributes, line-endings, readme, mise]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-05: formatters for TOML, shell and text; mise-pinned reuse (plan 01-03); docs/tooling.md (plan 01-01 to 01-03)"
provides:
  - "REUSE.toml: MIT aggregate for every file, Apache-2.0 override for the Gradle wrapper files, MIT override for .planning/**; LICENSES/MIT.txt and LICENSES/Apache-2.0.txt; `reuse lint` reports REUSE 3.3 compliance"
  - "README.md with the REUSE badge, quick start and pointer to docs/tooling.md"
  - ".gitattributes: text=auto eol=lf, LF for gradlew and *.sh, CRLF for *.bat and *.cmd, binary files, linguist-generated and linguist-documentation"
  - "Normalised committed tree (gradlew.bat now LF in the index, CRLF in checkouts) and a renormalisation check that exits 0"
affects: [01-07, 01-08, 01-09, 01-11, 01-12, 01-14]

plan_head_before: 82a994154f6a0b3eec8eae00cbf54efe9933aa43
plan_head_after: 3661a57c332442e690b18832a96d44e1ac9f008f

actuals:
  tokens: 6500
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "REUSE: one aggregate annotation for the whole tree; an override annotation only where file contents must be ignored or replaced (Gradle wrapper, planning documents)"
    - "SPDX text that has to live in a string literal is wrapped in REUSE-IgnoreStart and REUSE-IgnoreEnd comments"
    - "Generated lockfiles (mise.lock, .mise/locks/**) are licensed by the aggregate annotation, never edited"

key-files:
  created:
    - REUSE.toml
    - LICENSES/MIT.txt
    - LICENSES/Apache-2.0.txt
    - README.md
    - .gitattributes
  modified:
    - gradlew.bat
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt

key-decisions:
  - "The planning documents are covered by a second override annotation (.planning/**): their prose quotes SPDX header text, and REUSE parses any `SPDX-License-Identifier:` text as a tag, so 29 invalid expressions broke `reuse lint`. `override` makes REUSE ignore the file contents."
  - "SPDX text inside build-logic string literals is fenced with REUSE-IgnoreStart and REUSE-IgnoreEnd comments rather than rewritten or overridden by path, so the sources stay readable and their own headers stay authoritative"
  - "The normalisation check was run against the real index (clean at that point, nothing left staged) because the session harness refuses `GIT_INDEX_FILE`; the temporary-index form stays the documented reusable command for plans 01-08 and 01-12"

patterns-established:
  - "New documents that quote SPDX text are safe under .planning/** and docs; elsewhere use REUSE-IgnoreStart and REUSE-IgnoreEnd"

requirements-completed: [INFRA-13, INFRA-16]

coverage:
  - id: D1
    description: "`reuse lint` reports the repository compliant with REUSE 3.3; every file (78 of 78) has copyright and licence information, the wrapper files resolve to Apache-2.0, and the generated mise lockfiles are covered"
    requirement: INFRA-16
    verification:
      - kind: other
        ref: "mise x -- reuse lint (Congratulations! Your project is compliant with version 3.3 of the REUSE Specification)"
        status: pass
    human_judgment: false
  - id: D2
    description: "README.md shows the REUSE badge, a quick start (mise, mise install, lefthook install, ./gradlew build) and a pointer to docs/tooling.md"
    requirement: INFRA-16
    verification:
      - kind: other
        ref: "grep -c 'api.reuse.software/badge/github.com/aanbrn/axon5-dcb-courses' README.md prints 1; grep of 'mise install' and 'docs/tooling.md' succeeds"
        status: pass
    human_judgment: false
  - id: D3
    description: ".gitattributes sets LF, CRLF for batch files, binary types, linguist-generated and linguist-documentation; git check-attr reports eol crlf for gradlew.bat, eol lf for gradlew and binary set for the wrapper jar"
    requirement: INFRA-13
    verification:
      - kind: other
        ref: "git check-attr eol -- gradlew.bat gradlew; git check-attr binary -- gradle/wrapper/gradle-wrapper.jar"
        status: pass
    human_judgment: false
  - id: D4
    description: "The committed tree is normalised: renormalising and diffing against the index exits 0, empty and binary files are untouched, and the working tree is clean afterwards"
    requirement: INFRA-13
    verification:
      - kind: other
        ref: "git add --renormalize . then git diff --cached --exit-code --stat (rc 0); git status --porcelain empty"
        status: pass
    human_judgment: false
  - id: D5
    description: "The REUSE badge resolves and turns green on api.reuse.software"
    requirement: INFRA-16
    verification: []
    human_judgment: true
    rationale: "The badge is green only after the author registers the repository at api.reuse.software (author-only step in plan 01-14); the URL cannot be verified before the repository is public."

duration: 14min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 06: REUSE Compliance, README and Line Endings Summary

**REUSE.toml (MIT aggregate, Apache-2.0 override for the Gradle wrapper) makes `reuse lint` compliant with REUSE 3.3 for all 78 files, the minimal README carries the REUSE badge and quick start, and `.gitattributes` fixes LF, CRLF and binary handling with the tree renormalised**

## Performance

- **Duration:** about 14 min
- **Completed:** 2026-11-21T09:58Z
- **Tasks:** 2
- **Files modified:** 9 (5 created, 4 modified)

## Accomplishments

- Tracer slice proven end to end: `REUSE.toml`, the two downloaded licence texts and the README, with `reuse lint` compliant and `spotlessCheck` green.
- The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) resolves to Apache-2.0 only through the `override` annotation; every other file, including the JSON, the jar-adjacent files and the generated `mise.lock` and `.mise/locks/**` lockfiles, is MIT through the aggregate annotation.
- `.gitattributes` matches INFRA-13 exactly; `gradlew.bat` was the only committed file that changed under renormalisation (112 lines, CRLF to LF in the index; checkouts keep CRLF). Empty and binary files were not touched.
- `./gradlew check` is green and `editorconfig-checker` exits 0 on the new files.

## Task Commits

1. **Task 1: End-to-end licensing (tracer)** - `4d05157` (feat)
2. **Task 2: Line endings and attributes**
   - `.gitattributes`: `1064710` (chore)
   - normalised `gradlew.bat`: `3661a57` (chore)

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

Tracer feedback gate: the tracer `<verify>` is automated-only (interactive run, `end-of-phase`); `reuse lint` and the badge grep were re-run after the commit and passed, so expansion to Task 2 proceeded.

## Files Created/Modified

- `REUSE.toml` - aggregate MIT annotation for `**`, Apache-2.0 override for the wrapper files, MIT override for `.planning/**`
- `LICENSES/MIT.txt`, `LICENSES/Apache-2.0.txt` - licence texts from `reuse download`; the root `LICENSE` stays for GitHub's licence detection
- `README.md` - title, project description, REUSE badge, quick start, pointer to `docs/tooling.md`, licence section
- `.gitattributes` - line endings, binary types, generated and documentation markers
- `gradlew.bat` - renormalised only (LF in the index, CRLF in checkouts)
- `courses.root.gradle.kts`, `FormattingFunctionalTest.kt`, `GradleFixture.kt` - SPDX text in string literals fenced with `REUSE-IgnoreStart`/`REUSE-IgnoreEnd`; `GradleFixture` builds its fixture settings file from `$HEADER` instead of a second literal

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `reuse lint` rejected SPDX text quoted in planning documents**
- **Found during:** Task 1 (`reuse lint`)
- **Issue:** 29 "invalid SPDX License Expressions": `.planning/**` prose (plans, summaries, research) quotes `SPDX-License-Identifier: MIT` followed by backticks or quotes, which REUSE parses as a tag. The same text will recur in every later planning document.
- **Fix:** a second annotation `path = ".planning/**"`, `precedence = "override"` (MIT, same holder). This is the only way to make REUSE ignore file contents without editing generated documents.
- **Files modified:** `REUSE.toml`
- **Verification:** `reuse lint` reports no planning file as invalid.
- **Committed in:** `4d05157`
- **Consequence for the plan's criteria:** the acceptance criterion `grep -c 'precedence = "override"' REUSE.toml` prints 1 now prints 2. Its intent (the wrapper override exists) holds; the count is intentionally 2.

**2. [Rule 3 - Blocking] `reuse lint` rejected SPDX text in three build-logic sources**
- **Found during:** Task 1 (`reuse lint`)
- **Issue:** a regex literal in `courses.root.gradle.kts`, the header expectation in `FormattingFunctionalTest.kt` and two literals in `GradleFixture.kt` contain `SPDX-License-Identifier: MIT` followed by quote characters (the trap described in RESEARCH Finding 5a, here in code owned by plans 01-02, 01-04 and 01-05).
- **Fix:** `REUSE-IgnoreStart`/`REUSE-IgnoreEnd` comments around the literals; the fixture settings file and the expectation in the shell test now reference one constant or local value. No behaviour changed.
- **Files modified:** `courses.root.gradle.kts`, `FormattingFunctionalTest.kt`, `GradleFixture.kt`
- **Verification:** `reuse lint` compliant; `:build-logic:functionalTest` BUILD SUCCESSFUL (56 s); `./gradlew check` BUILD SUCCESSFUL.
- **Committed in:** `4d05157`

**3. [Process] The temporary-index normalisation check could not be run literally**
- **Found during:** Task 2 verification
- **Issue:** the session harness refuses any command that sets `GIT_INDEX_FILE`, so the plan's `<automated>` command (copy the index, renormalise into the copy) was blocked.
- **Fix:** the equivalent check ran on the real index at a moment when it equalled HEAD (`git add --renormalize .`, `git diff --cached --exit-code --stat` rc 0, `git status --porcelain` empty). Nothing was left staged. The temporary-index command remains the documented non-mutating form for plans 01-08 and 01-12, where it runs outside this harness; it was not exercised here.

---

**Total deviations:** 3 (2 blocking issues auto-fixed, 1 process difference)
**Impact on plan:** none on scope; the licensing and normalisation outcomes are exactly those the plan describes.

## Issues Encountered

- The harness refused compound git commands and `GIT_INDEX_FILE`; commands were split into plain ones.
- ktfmt keeps a blank line between the `REUSE-IgnoreStart`-fenced constant and the next member, which is harmless: the fence still encloses the literal.

## Verification Results and Timing

| Command | Result | Wall clock |
| --- | --- | --- |
| `mise x -- reuse lint` | compliant with REUSE 3.3 (78 of 78 files) | 1 s to 3 s |
| `grep -c 'api.reuse.software/badge/...' README.md` | 1 | under 1 s |
| `mise x -- ./gradlew spotlessApply` then `spotlessCheck` (warm) | pass, no file changed | about 1 s each |
| `git add --renormalize .` then `git diff --cached --exit-code --stat` (real index, clean) | rc 0 | under 1 s |
| `git check-attr eol` and `git check-attr binary` | `gradlew.bat: crlf`, `gradlew: lf`, `gradle-wrapper.jar: binary: set` | under 1 s |
| `mise x -- ./gradlew :build-logic:functionalTest` (after the literal changes) | BUILD SUCCESSFUL | 56 s |
| `mise x -- ./gradlew check` (warm) | BUILD SUCCESSFUL | 8 s |
| `mise x -- editorconfig-checker` | exit 0 | under 1 s |

Only the TestKit run exceeds 30 s; it is far below the 180 s budget of `01-VALIDATION.md`, so there is no validation gap.

## Known Stubs

None.

## Threat Flags

None. T-01-09 mitigated: `REUSE.toml`, `LICENSES/` and `reuse lint` (hook in plan 01-08, CI in plan 01-12). T-01-SC: no packages installed; `reuse download` fetched only the two SPDX licence texts.

## User Setup Required

None - no external service configuration required. The badge turns green after the author registers the repository at api.reuse.software (plan 01-14).

## Next Phase Readiness

- Plan 01-08 (hook) and plan 01-12 (CI) can run `reuse lint` and the renormalisation check (temporary `GIT_INDEX_FILE` copy, then `git diff --cached --exit-code`).
- New shell scripts, YAML, JSON and other headerless files are covered by the aggregate annotation; a new file whose prose or string literal contains `SPDX-License-Identifier:` text outside `.planning/**` needs `REUSE-IgnoreStart`/`REUSE-IgnoreEnd` around it.
- `docs/tooling.md` repeats the README quick start; keep the two in step.

## Self-Check: PASSED

Created files exist on disk (`REUSE.toml`, `LICENSES/MIT.txt`, `LICENSES/Apache-2.0.txt`, `README.md`, `.gitattributes`); commits `4d05157`, `1064710` and `3661a57` are ancestors of HEAD; acceptance criteria hold (`precedence = "aggregate"` 1, `precedence = "override"` 2 by the documented deviation, `docs/tooling.md` and `mise install` in the README, `linguist-documentation` 3, `*.bat text eol=crlf` 1, `git status --porcelain` empty); plan-level verification passes (`reuse lint`, renormalisation check, `spotlessCheck`, `check`).

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
