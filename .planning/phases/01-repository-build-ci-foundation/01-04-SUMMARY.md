---
phase: 01-repository-build-ci-foundation
plan: 04
subsystem: infra
tags: [spotless, prettier, editorconfig, editorconfig-checker, node, nvmrc, testkit]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Plan 01-02: courses.root Spotless setup, GradleFixture and the TestKit suite; plan 01-03: mise toolchain, .nvmrc, editorconfig-checker binary"
provides:
  - "Prettier Spotless format over JSON, JSON5, YAML and Markdown (plus .claude/settings.json) in courses.root"
  - "VerifyNodeVersion task that checks the active Node major against .nvmrc and feeds the Prettier Spotless tasks"
  - ".prettierrc.yaml aligned with .editorconfig"
  - ".editorconfig for every file type and .editorconfig-checker.json"
  - "FormattingFunctionalTest (3 TestKit tests) and a GradleFixture that carries .prettierrc.yaml and .nvmrc"
affects: [01-05, 01-06, 01-07, 01-08, 01-12]

plan_head_before: b7fff69ce4a4b00badb6ae68ca9cb76581056b4a
plan_head_after: d018f93e45726f7e42ff7c12c018342678d0f33f

actuals:
  tokens: 9000
  tasks: 2
  commits: 2

tech-stack:
  added: [Prettier 3.10.1 through Spotless, editorconfig-checker (already pinned in 01-03)]
  patterns:
    - "Environment-dependent tasks run on every build and publish their result as a file that downstream cacheable tasks take as an input"
    - "Spotless targets stay FileTrees; an explicit single-file FileTree adds the one tracked file under an otherwise excluded directory"
    - "Generated lockfiles (mise.lock, .mise/locks) are excluded from editorconfig-checker instead of being formatted"

key-files:
  created:
    - .prettierrc.yaml
    - .editorconfig
    - .editorconfig-checker.json
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt
  modified:
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
    - docs/tooling.md
    - config/detekt/detekt.yml

key-decisions:
  - "Markdown leaves indent_size unset in .editorconfig: Prettier aligns ordered-list continuation lines to the three-column marker, which no fixed indent size can describe"
  - "gradle/wrapper/** joined the shared excluded list instead of a Prettier-only list; no Kotlin source lives there, so Kotlin targets are unaffected"
  - "editorconfig-checker excludes mise.lock and .mise/locks/ (generated lockfiles) in addition to the plan's LICENSES, wrapper jar, .planning and .claude"

patterns-established:
  - "FormattingFunctionalTest is the home for formatting behaviours; plan 01-05 extends it with TOML, shell and text formats"
  - "Any Spotless task whose name starts with spotlessPrettier depends on verifyNodeVersion's output file"

requirements-completed: [TEST-09, INFRA-14]

coverage:
  - id: D1
    description: "One ./gradlew spotlessApply formats Kotlin, KTS, JSON, JSON5, YAML and Markdown; a second run changes nothing"
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../FormattingFunctionalTest.kt#unformatted JSON, YAML and Markdown fail spotlessCheck and spotlessApply fixes them"
        status: pass
      - kind: other
        ref: "mise x -- ./gradlew spotlessApply (twice), then verifyNodeVersion spotlessCheck: BUILD SUCCESSFUL, no file changed by the second run"
        status: pass
    human_judgment: false
  - id: D2
    description: "A Node major different from .nvmrc fails fast with a message naming .nvmrc and the fix; a matching major lets the Prettier tasks run"
    requirement: TEST-09
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/.../FormattingFunctionalTest.kt#a Node major different from nvmrc fails verifyNodeVersion naming nvmrc"
        status: pass
      - kind: integration
        ref: "build-logic/src/functionalTest/.../FormattingFunctionalTest.kt#a matching Node major passes verifyNodeVersion and guards the Prettier tasks"
        status: pass
    human_judgment: false
  - id: D3
    description: ".editorconfig defines settings for every file type, declares [*] first, agrees with ktfmt and Prettier, and editorconfig-checker passes on the formatted tree"
    requirement: INFRA-14
    verification:
      - kind: other
        ref: "mise x -- editorconfig-checker (exit 0); negative probe with a 3-space JSON file exits 1; an empty file passes"
        status: pass
      - kind: other
        ref: "head -1 .editorconfig | grep -qx 'root = true' and first section is [*]"
        status: pass
    human_judgment: false
  - id: D4
    description: "Node is verified before Prettier runs and its result invalidates cached Spotless output on a Node switch"
    requirement: TEST-09
    verification: []
    human_judgment: true
    rationale: "Build-cache invalidation across a real Node version switch was not exercised (only one Node major is installed); the wiring (output file as input of every spotlessPrettier task) is covered by the passing guard test, the cross-version behaviour is by construction."

duration: 12min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 04: Prettier Formatting and Editor Settings Summary

**Spotless now formats JSON, JSON5, YAML and Markdown with Prettier (Node major verified against `.nvmrc` by a fail-fast task that is an input of every Prettier task), and a complete `.editorconfig` that editorconfig-checker proves agrees with ktfmt and Prettier**

## Performance

- **Duration:** about 12 min
- **Completed:** 2026-11-21T09:40Z
- **Tasks:** 2
- **Files modified:** 9 (5 created, 4 modified; 2 of the modified are reformat-only)

## Accomplishments

- Tracer slice proven end to end: a `prettier` Spotless format in `courses.root` over FileTree targets, `verifyNodeVersion` wired as an input of every `spotlessPrettier*` task, and a three-test TestKit suite showing unformatted JSON, YAML and Markdown fail `spotlessCheck`, are fixed by `spotlessApply`, and then pass; a wrong `.nvmrc` major fails with a message naming `.nvmrc` and `mise install`.
- `./gradlew spotlessApply` is idempotent on the repository: a second run changed no file.
- `.editorconfig` covers Kotlin, data formats, TOML, Markdown, shell, Windows scripts, text, scenarios, `gradlew` and XML, with `[*]` first; `editorconfig-checker` exits 0 on the formatted tree, exits 1 on a deliberately mis-indented file, and passes on an empty file.
- `./gradlew check` (all earlier gates plus the new Prettier check and 3 new TestKit tests) is green.

## Task Commits

1. **Task 1: End-to-end Prettier (tracer)** - `4df9763` (feat)
2. **Task 2: Editor settings** - `d018f93` (feat)

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

Tracer feedback gate: the tracer `<verify>` is automated-only; both automated commands passed after the commit (filtered `FormattingFunctionalTest` run, then `spotlessApply` followed by `verifyNodeVersion spotlessCheck`), so expansion to Task 2 proceeded.

## Files Created/Modified

- `.prettierrc.yaml` - Prettier settings (100 columns, 2 spaces, LF, `proseWrap: preserve`)
- `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt` - fail-fast Node major check that writes the verified major to a build output file
- `build-logic/src/main/kotlin/courses.root.gradle.kts` - `verifyNodeVersion` registration, `prettier` format, input wiring, `gradle/wrapper/**` exclusion
- `build-logic/src/functionalTest/.../FormattingFunctionalTest.kt` - Prettier and Node-guard TestKit tests
- `build-logic/src/functionalTest/.../GradleFixture.kt` - standard fixture copies `.prettierrc.yaml` and `.nvmrc`
- `.editorconfig`, `.editorconfig-checker.json` - editor settings and checker configuration
- `docs/tooling.md`, `config/detekt/detekt.yml` - reformat-only (Prettier aligned the tables and rewrote the long `excludes` flow lists)

## Decisions Made

- Markdown `indent_size = unset`: see deviation 1.
- `gradle/wrapper/**` was added to the shared `excluded` list, which is simpler than a second Prettier-only list and harmless for Kotlin targets.
- `mise.lock` and `.mise/locks/` are excluded from editorconfig-checker (follow-up from plan 01-03); plan 01-06 still has to handle them for REUSE.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Markdown cannot carry a fixed indent size in `.editorconfig`**
- **Found during:** Task 2 (first `editorconfig-checker` run)
- **Issue:** the plan sets `[*.md] indent_size = 2`, but Prettier output in `docs/tooling.md` aligns continuation lines of ordered-list items to the three-column `1. ` marker (3 spaces), which the checker reports as "want multiple of 2". The formatter and `.editorconfig` would disagree on every numbered list.
- **Fix:** `[*.md]` sets `indent_size = unset` (with a comment saying why); Prettier owns Markdown layout. All other sections keep the values Prettier and ktfmt use.
- **Files modified:** `.editorconfig`
- **Verification:** `mise x -- editorconfig-checker` exits 0 on the Prettier-formatted tree; the JSON negative probe still fails.
- **Committed in:** `d018f93`

**2. [Rule 1 - Bug] Test fixture content and check scope**
- **Found during:** Task 1 (first test run)
- **Issue:** Prettier keeps Setext headings, so the planned `Title` / `=====` Markdown sample never changed; and `spotlessCheck` on the raw fixture failed on the unnormalised Kotlin scaffolding, not on the Prettier format.
- **Fix:** the Markdown sample uses `#   Title` with extra blank lines and loose list markers, and the Node-guard test runs `spotlessPrettierCheck` instead of the whole `spotlessCheck`.
- **Files modified:** `FormattingFunctionalTest.kt`
- **Committed in:** `4df9763`

**3. [Rule 3 - Blocking] Plan 01-03 follow-up: lockfiles in editorconfig-checker**
- **Found during:** Task 2
- **Issue:** `mise.lock` and `.mise/locks/**` are generated and must not be reformatted.
- **Fix:** two extra `Exclude` patterns in `.editorconfig-checker.json`.
- **Files modified:** `.editorconfig-checker.json`
- **Committed in:** `d018f93`

---

**Total deviations:** 3 auto-fixed (2 bugs, 1 blocking)
**Impact on plan:** All three needed for a consistent formatter-versus-editorconfig contract; no gate weakened. Deviation 1 is the one the author may want to review.

## Issues Encountered

- The harness refused compound shell commands with git-like tokens; they were split into plain commands.
- ktfmt reflowed a code comment in `courses.root.gradle.kts` during the first `spotlessApply`; the comment was reworded to fit the limit.
- Edge cases marked `verification: backstop` in the plan (empty file, processing order independence) have no dedicated test; the empty-file case was probed manually and passed.
- `.claude/settings.json` is allowed by `.gitignore` but does not exist in this checkout, so the explicit FileTree for it currently matches nothing; it formats the file as soon as it is tracked.

## Verification Results and Timing

| Command | Result | Wall clock |
| --- | --- | --- |
| `mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*'` (warm, 3 tests) | pass | 13 s (first run with failures 17 s) |
| `mise x -- ./gradlew spotlessApply` then `verifyNodeVersion spotlessCheck` | pass, second apply changed nothing | about 5 s first apply, under 1 s afterwards |
| `mise x -- editorconfig-checker` | pass | under 1 s |
| `mise x -- ./gradlew check` (all gates, 11 TestKit tests) | BUILD SUCCESSFUL | 41 s |

The filtered `FormattingFunctionalTest` run stays well under the 30 s expectation and the 180 s budget of `01-VALIDATION.md`; no validation gap.

## Known Stubs

None.

## Threat Flags

None. T-01-07 mitigated as planned: Prettier version pinned in the catalog, no Prettier plugins, Node major verified by `verifyNodeVersion`. T-01-SC: no new packages installed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 01-05 can add TOML, shell and text formats to `courses.root` and `FormattingFunctionalTest`; `.editorconfig` already has the `[*.sh]` and `[*.toml]` sections shfmt and taplo read, and `.taplo.toml` is already a `functionalTest` input.
- Plan 01-06 (REUSE) must still cover `mise.lock` and `.mise/locks/**`.
- Plan 01-10's inventory cross-check should be aware that Prettier realigned the `docs/tooling.md` tables (column padding only, content unchanged).

## Self-Check: PASSED

All created files exist on disk; commits `4df9763` and `d018f93` are ancestors of HEAD; Task 1 and Task 2 acceptance criteria (`format("prettier")` count 1, `verifyNodeVersion` count 4, `target("**` count 0, `[*.{kt,kts}]` 1, `[*.sh]` 1, `MaxLineLength` 1) and plan-level verification pass.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
