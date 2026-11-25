---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 15
subsystem: governance
tags: [gradle, testkit, upstream-records, front-matter, markers, governance]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "courses.governance convention, the GradleFixture TestKit fixture and the ToolingTableCheck line-reader precedent (plans 02-02 and 02-13)"
provides:
  - "FrontMatter: fail-closed parser for a documented YAML subset, shared with the ADR check of plan 02-19"
  - "checkUpstreamRecords (cacheable, in check): record validation, status lifecycle rule, two-way UPSTREAM marker consistency"
  - "Generated site-ready index build/reports/governance/upstream-index.md, one table row per record sorted by id"
  - "docs/templates/upstream-record.md and docs/templates/upstream-issue.md"
affects: [02-16 and 02-17 upstream records, 02-19 ADR check, 02-22 issue publication, phase 5 documentation site (index is a build input)]

actuals:
  tokens: 15700
  tasks: 2
  commits: 2
plan_head_before: 344136d6c0427aa42ad5fe9c2c5f8481ccb24172
plan_head_after: 943f6c8e5a275cd9ca5635bcb24b35681aaa787f

tech-stack:
  added: []
  patterns:
    - "Hand-written subset parser instead of a YAML library, failing with file:line and a reason on any other construct"
    - "Check collects every problem, sorts by path then line, writes a report, and throws one GradleException naming it"
    - "Marker is recognised only directly after a comment opener, so strings, prose and data never count"
    - "Temporary bridge property that empties itself: an entry with a record or without a marker is itself a failure"

key-files:
  created:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt
    - docs/templates/upstream-record.md
    - docs/templates/upstream-issue.md
  modified:
    - build-logic/src/main/kotlin/courses.governance.gradle.kts

key-decisions:
  - "Front matter subset: flat key: scalar and one-line key: [a, b] with bare or double-quoted strings, true, false, null, whole-line and trailing comments; a quoted \"true\" or \"null\" stays a string; everything else fails with file:line"
  - "Record rules are strict beyond the minimum: unknown keys, an empty section and a missing title heading fail, so the template is the one canonical shape"
  - "A marker counts only as UPSTREAM: UP-NNNN directly after a comment opener (//, /*, *, #, <!--, --); UP-00010, UP-0001x, UP-0001 without the space and lower-case ids are malformed and fail"
  - "wont-fix with workaround true keeps its markers (D-45); workaround-removed or workaround false has none"
  - "The scan excludes list is shared with checkBuildScripts through one scanExcludes value, so the two checks cannot drift"

patterns-established:
  - "Governance checks that scan the repository take a file tree rooted at the repository and report repository-relative paths"
  - "A convention-plugin default that a TestKit fixture must override is overridden in the fixture root build script, not by loosening the check"

requirements-completed: [UPS-01, UPS-02, GH-12]

coverage:
  - id: D1
    description: "checkUpstreamRecords validates every record: required keys, id and file name encoding, allowed statuses, dates, required sections, title, duplicate ids"
    requirement: UPS-01
    verification:
      - kind: integration
        ref: "UpstreamRecordCheckFunctionalTest (24 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Status lifecycle: a reported record needs a non-empty upstream list and a reported date"
    requirement: UPS-01
    verification:
      - kind: integration
        ref: "UpstreamRecordCheckFunctionalTest#a reported record needs an upstream link and a reported date"
        status: pass
    human_judgment: false
  - id: D3
    description: "Markers and records are consistent in both directions, wont-fix keeps its markers, malformed markers fail, examples in records, templates, .planning, .claude and build output are not scanned"
    requirement: UPS-02
    verification:
      - kind: integration
        ref: "UpstreamRecordCheckFunctionalTest (marker, wont-fix, malformed-marker and exclusion cases)"
        status: pass
      - kind: command
        ref: "mise x -- ./gradlew checkUpstreamRecords on the real tree (BUILD SUCCESSFUL)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The index page is generated as a site-ready Markdown table sorted by id"
    requirement: UPS-02
    verification:
      - kind: integration
        ref: "UpstreamRecordCheckFunctionalTest#a valid record with its marker passes and the index lists the records sorted by id"
        status: pass
    human_judgment: false
  - id: D5
    description: "Record and issue templates exist, are Prettier-stable and excluded from the marker scan"
    requirement: GH-12
    verification:
      - kind: command
        ref: "mise x -- ./gradlew spotlessCheck checkUpstreamRecords; section grep over docs/templates/upstream-record.md"
        status: pass
    human_judgment: false
  - id: D6
    description: "The template guidance reads as clear, accurate instructions for authors and agents"
    verification: []
    human_judgment: true
    rationale: "Whether the lifecycle, product-key and publication guidance is clear and complete is an editorial judgment, not something a test asserts."

duration: 40min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 15: Upstream record check and templates Summary

**A cacheable `checkUpstreamRecords` that validates `docs/upstream/` records through a fail-closed front-matter subset parser, keeps `UPSTREAM: UP-NNNN` markers and records consistent in both directions, and writes a site-ready index; plus the record and issue templates**

## Performance

- **Duration:** about 40 min
- **Started:** 2026-11-23T04:50:00Z
- **Completed:** 2026-11-23T05:30:00Z
- **Tasks:** 2
- **Files modified:** 6 (5 created, 1 modified)

## Accomplishments

- `FrontMatter` parses only flat scalars and one-line lists and fails with `<file>:<line>: unsupported front matter: <reason>` on nesting, block scalars, anchors, single quotes, duplicate keys, indented lines, unterminated lists and a missing closing `---`.
- `UpstreamRecordCheck` enforces every rule of the plan: eight required keys, `id` `^UP-[0-9]{4}$` matching the file number, file name `NNNN-<slug>.md`, five statuses, `YYYY-MM-DD` or `null` dates, the six sections, the title heading, duplicate ids naming both files, and the `reported` lifecycle (non-empty `upstream` and a `reported` date, each failing naming the file and key).
- Marker consistency runs in both directions with the D-45 rule for `wont-fix`; malformed markers (`UP-00010`, `UP-0001x`) fail instead of counting for another record; problems are sorted by file and line.
- The index `build/reports/governance/upstream-index.md` is written on every run as a heading plus one table row per valid record, sorted by id, ready for the Phase 5 documentation site to include unchanged.
- 24 TestKit cases cover each must-have truth and edge; the check is cacheable and runs clean under the configuration cache and isolated projects flags the fixture enforces.

## Task Commits

1. **Task 1: End-to-end upstream check** - `5e1b735` (feat)
2. **Task 2: Upstream record and issue templates** - `943f6c8` (docs)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-existing markers without records broke the real-tree gate**
- **Found during:** Task 1 (real-tree verify)
- **Issue:** `KotlinTagResolver.kt` and `DecisionCaptureEngine.kt` already carry `UPSTREAM: UP-0001`, `UP-0002` and `UP-0004`, but their records are plan 02-16's work and the plan forbids committing records here (`git ls-files docs/upstream` prints nothing). With the check in `check` the build would fail until 02-16, and the plan prohibits widening exclusions to hide markers.
- **Fix:** Added a narrow, self-removing bridge instead of an exclusion: a task input `unrecordedMarkers` (empty by default) that `courses.governance.gradle.kts` sets to exactly `UP-0001`, `UP-0002`, `UP-0004`. The check stays strict for every other id. An entry fails the build as soon as it has a record or loses its last marker, so the list cannot outlive its purpose or hide a marker for good. Tolerated markers are logged as a warning. TestKit cases prove the bridge tolerates only the listed ids, demands its own removal, and rejects a malformed entry.
- **Files modified:** `UpstreamRecordCheck.kt`, `courses.governance.gradle.kts`
- **Commit:** 5e1b735
- **Consequence for plan 02-16:** the moment 02-16 adds `docs/upstream/0001-*.md`, `0002-*.md` or `0004-*.md`, `checkUpstreamRecords` reports `unrecordedMarkers: UP-000N has a record (...); remove it from the list`. 02-16 must delete the `unrecordedMarkers.set(setOf("UP-0001", "UP-0002", "UP-0004"))` line (and its comment) in `build-logic/src/main/kotlin/courses.governance.gradle.kts`, which is not in 02-16's `files_modified`.

**2. [Rule 2 - Missing critical] Stricter record validation than the minimum**
- **Found during:** Task 1
- **Issue:** The plan lists required keys and sections but not unknown keys, empty sections or a missing title heading; accepting them would let a record into the index with no title or silently carry typos.
- **Fix:** Unknown front matter keys, empty required sections and a missing or mismatched `# UP-NNNN: <title>` heading fail. The index title comes from that heading.
- **Files modified:** `UpstreamRecordCheck.kt`
- **Commit:** 5e1b735

**3. [Rule 1 - Bug] Kotlin nested comments in KDoc**
- **Found during:** Task 1 (first compile)
- **Issue:** A KDoc line containing `docs/upstream/*.md` opened a nested comment and broke compilation.
- **Fix:** Reworded the KDoc.
- **Commit:** 5e1b735

**4. [Rule 2 - Missing critical] Marker recognition anchored to a comment opener**
- **Found during:** Task 1
- **Issue:** The plan asks for the marker regex "anchored to a comment or prose context" without defining it; an unanchored regex would turn the check's own message strings and ordinary prose into markers.
- **Fix:** A marker counts only as `UPSTREAM:` directly after `//`, `/*`, `*`, `#`, `<!--` or `--`. This is stricter in the safe direction: a marker outside a comment is ignored, so a record that depends on it fails for a missing marker.
- **Commit:** 5e1b735

**Total deviations:** 4 auto-fixed (1 blocking, 2 missing critical, 1 bug). **Impact:** The bridge is the only functional addition outside the plan; it is reversible by deleting one line in `courses.governance.gradle.kts`, which 02-16 must do.

## Issues Encountered

None beyond the deviations above.

## Verification

- `mise x -- ./gradlew :build-logic:functionalTest --tests '*UpstreamRecordCheckFunctionalTest*'`: 24 tests, 0 failures, result XML present.
- `mise x -- ./gradlew checkUpstreamRecords` on the real tree: passes, index written (header only, no records yet), three tolerated markers logged.
- `mise x -- ./gradlew :build-logic:detekt spotlessCheck checkBuildScripts checkClaudeAskRules checkUpstreamRecords`: green.
- `mise x -- ./gradlew build` (whole repository, including every check in `check`): BUILD SUCCESSFUL in 8m 28s.
- Acceptance greps: `checkUpstreamRecords` count in the script 4, `wont-fix` in the check 3, the lifecycle test name 1, `^id: "UP-NNNN"$` 1, `Minimal reproduction` in the issue template 1, `git ls-files docs/upstream` empty.

## Known Stubs

None.

## Threat Flags

None. The check reads repository text and writes two files under `build/`; it adds no network, auth or trust-boundary surface beyond the plan's threat model (T-02-38 and T-02-39 are mitigated by the fail-closed parser and the two-way rules).

## Next Phase Readiness

Ready for plan 02-16 and 02-17 (records). Both must follow the template; 02-16 must also remove the `unrecordedMarkers` bridge as described under deviation 1. Plan 02-19 reuses `FrontMatter.parse(file, lines)`.

## Self-Check: PASSED
