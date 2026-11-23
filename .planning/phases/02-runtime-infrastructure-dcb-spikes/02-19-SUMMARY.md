---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 19
subsystem: governance
tags: [gradle, testkit, adr, madr, templates, front-matter, governance]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "FrontMatter.parse(file, lines), the courses.governance convention and the GradleFixture TestKit fixture (plans 02-02, 02-13 and 02-15)"
provides:
  - "docs/templates/adr.md: the one MADR 4 ADR template with YAML front matter"
  - "checkAdrs (cacheable, in check): front matter, status, numbering and structure rules for every docs/adr/NNNN-<slug>.md"
  - "ADR-0001..0004 normalised to the template with decisions, dates and statuses unchanged"
  - "docs/templates/doc-page.md and docs/templates/event-model-slice.md (all five GH-12 templates now exist)"
affects: [02-23 spike-outcome ADRs, every later phase's ADRs, phase 3 slice descriptions (DOC-05), phase 8 project skills (GH-12 use by skills)]

actuals:
  tokens: 11000
  tasks: 2
  commits: 3
plan_head_before: 34196d2c9701b8d97559ef056ffdbef9c7c605d7
plan_head_after: b58424033a78082b6b4775f62cc36906daf2fd4f

tech-stack:
  added: []
  patterns:
    - "ADR check reuses the shared fail-closed FrontMatter subset parser and the collect-sort-report-throw shape of the other governance checks"
    - "Heading outline that skips code fences, with a count of content lines per section, to fail on a missing or empty required section"
    - "Template directory docs/templates is outside every input of the ADR and marker checks"

key-files:
  created:
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheckFunctionalTest.kt
    - docs/templates/adr.md
    - docs/templates/doc-page.md
    - docs/templates/event-model-slice.md
  modified:
    - build-logic/src/main/kotlin/courses.governance.gradle.kts
    - docs/adr/0001-workflow-permissions-and-required-checks.md
    - docs/adr/0002-codeql-kotlin-analysis.md
    - docs/adr/0003-governance-gate-trust-boundary.md
    - docs/adr/0004-gsd-configuration.md

key-decisions:
  - "ADR front matter keys are exactly status, date, decision-makers (required) and consulted, informed (optional); any other key fails, like the unknown-key rule of the upstream record check"
  - "A required section that exists but has no content fails, and a superseded status may not name the ADR itself"
  - "Every Markdown file directly in docs/adr/ must be named NNNN-<slug>.md; there is no allowance for a README or an index in that directory"
  - "Revisit and re-enable criteria live under ## More Information, the Confirmation subsection of the template is reserved for how compliance is checked"

patterns-established:
  - "Normalising a record is structural only: text is moved, never reworded, except relative position words (above, below) that the move made untrue"
  - "A governance task that scans a directory takes a file tree rooted at the repository and reports repository-relative paths"

requirements-completed: [DOC-03, GH-12]

coverage:
  - id: D1
    description: "checkAdrs validates every ADR: file name and numbering from 0001 without gaps or duplicates, required keys, allowed statuses with an existing superseding ADR, a valid date, the title number, and the three required sections"
    requirement: DOC-03
    verification:
      - kind: integration
        ref: "AdrCheckFunctionalTest (24 tests, 0 failures)"
        status: pass
      - kind: command
        ref: "mise x -- ./gradlew checkAdrs on the real tree (BUILD SUCCESSFUL, empty report)"
        status: pass
    human_judgment: false
  - id: D2
    description: "ADR-0001..0004 follow the template with identical statuses, dates and decision-makers, the minimal-record notes removed, and only structure moved"
    requirement: DOC-03
    verification:
      - kind: command
        ref: "git diff --word-diff over docs/adr, status/date/Deciders lines compared with the plan base commit; checkAdrs; no 'This is a minimal record' left"
        status: pass
    human_judgment: true
    rationale: "Whether every sentence kept its meaning while moving into the template sections, and whether the derived Considered Options bullets are faithful to what each ADR already describes, is a reading judgment that no test asserts."
  - id: D3
    description: "All five GH-12 templates exist: adr, doc-page, event-model-slice (new) and upstream-record, upstream-issue (plan 02-15), Prettier-stable and excluded from the ADR and marker scans"
    requirement: GH-12
    verification:
      - kind: command
        ref: "ls docs/templates/*.md; mise x -- ./gradlew build (BUILD SUCCESSFUL)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The template guidance reads as clear, accurate instructions for authors and agents, and the eventmodeling skeleton renders"
    verification: []
    human_judgment: true
    rationale: "Editorial clarity and the rendered Mermaid diagram are judgments; the eventmodeling syntax was copied from the Mermaid 11.15+ documentation but was not rendered in this run."

duration: 55min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 19: ADR template, `checkAdrs` and normalised ADRs Summary

**A cacheable `checkAdrs` that enforces MADR 4 front matter, gap-free numbering and the required sections of every ADR through the shared front-matter parser, the matching ADR template, ADR-0001..0004 moved onto it without changing a decision, and the documentation-page and Event Modeling slice templates**

## Performance

- **Duration:** about 55 min, of which the whole-repository build took 10 min 20 s
- **Started:** 2026-11-23T05:20:00Z
- **Completed:** 2026-11-23T06:15:00Z
- **Tasks:** 2
- **Files modified:** 10 (5 created, 5 modified)

## Accomplishments

- `AdrCheck` reads every `docs/adr/*.md` through `FrontMatter.parse` and reports `path:line: message`, sorted by path, line and message:
  - file name `NNNN-<slug>.md`, duplicates naming both files, `0000` rejected, every number below the highest without a file named as missing;
  - required keys `status`, `date`, `decision-makers`, optional `consulted`, `informed`, unknown keys rejected, a real `YYYY-MM-DD` date, no empty value or blank list item;
  - status `proposed`, `accepted`, `rejected`, `deprecated` or `superseded by ADR-NNNN` where that ADR exists and is not the ADR itself;
  - title `# ADR-NNNN: <title>` with the file's number, and non-empty `## Context and Problem Statement`, `## Considered Options`, `## Decision Outcome` outside code fences.
- `checkAdrs` is registered in `courses.governance.gradle.kts` (group `verification`, the planned description) and wired into `check`; `docs/templates/` is outside its input.
- `docs/templates/adr.md` is MADR 4.0.0 with Prettier-stable front matter, guidance on which sections a small decision may omit, and the rule that the date is when the decision was last updated.
- ADR-0001..0004 were rewritten by a script that moves text verbatim, so sentences are byte-identical apart from the points under Deviations.
- Two further templates: `doc-page.md` (title, description, purpose and audience, content, related decisions, related tests) and `event-model-slice.md` (command, Mermaid `eventmodeling` skeleton, Given/When/Then, consistency boundary table, rejection codes, links).

## Task Commits

1. **Task 1: ADR template, `checkAdrs` in `check`, ADR-0001..0004 normalised (tracer)** - `60aa4ab` (feat)
2. **Task 2, tests: 24 TestKit cases for `AdrCheck`** - `a297ee5` (test)
3. **Task 2, templates: documentation-page and slice templates** - `b584240` (docs)

**Plan metadata:** committed with this SUMMARY (docs: complete plan).

## TDD and tracer notes

- **Tracer gate:** Task 1 is `type="tracer"` with an automated-only `<verify>`; `mise x -- ./gradlew checkAdrs spotlessCheck` passed before expansion, so `Tracer verified end-to-end` and Task 2 proceeded.
- **RED:** none exists. The plan orders the work so that `AdrCheck` lands in the tracer slice and Task 2's `tdd="true"` tests are written afterwards, so the first run of the 24 tests was already green (`tests="24" failures="0"`). The `test(...)` commit precedes no `feat(...)` GREEN commit of its own, which the TDD review will read as a plan-ordering artifact, not a skipped step. The assertions pin exact messages with file and line, so a regression of any rule fails a named test.
- **GREEN:** `AdrCheck` needed no fix after the tests ran.

## Normalisation is structural only (T-02-44)

`git diff --stat 34196d2c9701b8d97559ef056ffdbef9c7c605d7..HEAD -- docs/adr`:

```text
 ...001-workflow-permissions-and-required-checks.md | 27 ++++++----
 docs/adr/0002-codeql-kotlin-analysis.md            | 60 ++++++++++++----------
 docs/adr/0003-governance-gate-trust-boundary.md    | 59 +++++++++++----------
 docs/adr/0004-gsd-configuration.md                 | 54 ++++++++++---------
 4 files changed, 114 insertions(+), 86 deletions(-)
```

Status, date and deciders at the plan base commit versus the new front matter (the values are identical for all four):

| ADR  | Base commit bullets                                                                | New front matter                                                                                                     |
| ---- | ---------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| 0001 | `Status: accepted`, `Date: 2026-11-21`, `Deciders: repository author (decision D-21)` | `status: "accepted"`, `date: "2026-11-21"`, `decision-makers: ["repository author (decision D-21)"]`                 |
| 0002 | `Status: accepted`, `Date: 2026-11-21`, `Deciders: repository author (decision D-24)` | `status: "accepted"`, `date: "2026-11-21"`, `decision-makers: ["repository author (decision D-24)"]`                 |
| 0003 | `Status: accepted`, `Date: 2026-11-21`, `Deciders: repository author (gap-closure decision on review finding WR-06)` | `status: "accepted"`, `date: "2026-11-21"`, `decision-makers: ["repository author (gap-closure decision on review finding WR-06)"]` |
| 0004 | `Status: accepted`, `Date: 2026-11-21`, `Deciders: repository author (decision D-14 and the planner web-search setting)` | `status: "accepted"`, `date: "2026-11-21"`, `decision-makers: ["repository author (decision D-14 and the planner web-search setting)"]` |

The word-diff over each ADR shows only: the metadata bullets and minimal-record note replaced by front matter, `## Context` to `## Context and Problem Statement`, the new `## Considered Options` block, headings demoted or moved into `## Decision Outcome` and `## More Information`, and the two wording changes listed below. No sentence of a context, decision, reason, consequence or revisit criterion was reworded.

Section mapping: Decision to Decision Outcome; Consequences to `### Consequences` under it; ADR-0002 Spike and Re-enable criteria, ADR-0003 When to revisit and ADR-0004 Recording later keys and When to revisit to `## More Information`; ADR-0003 and ADR-0004 Rejected alternatives to `## Considered Options`; ADR-0004's two decisions and "Changing the configuration deliberately" as `###` subsections of Decision Outcome.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Relative position words made untrue by moving sections**
- **Found during:** Task 1 (reading the word-diff)
- **Issue:** ADR-0002 Consequences said "until the criteria above hold" and ADR-0003 said "The alternatives below either run ..."; after the move the criteria are below the sentence and the alternatives are above it.
- **Fix:** "criteria above" became "criteria below" (ADR-0002), "alternatives below" became "alternatives under Considered Options" (ADR-0003). No meaning changed.
- **Files modified:** `docs/adr/0002-codeql-kotlin-analysis.md`, `docs/adr/0003-governance-gate-trust-boundary.md`
- **Committed in:** `60aa4ab`

**2. [Rule 2 - Missing critical] Stricter validation than the minimum**
- **Found during:** Task 1
- **Issue:** The plan lists required keys, statuses, title, numbering and sections but not unknown keys, empty required sections, an ADR superseding itself, `0000`, or a non-conforming file name. Accepting them lets a typo or an empty section pass as a valid record.
- **Fix:** Each fails with a file, and a line where there is one, and is pinned by a TestKit case. This mirrors the strictness added to `checkUpstreamRecords` in plan 02-15.
- **Files modified:** `AdrCheck.kt`, `AdrCheckFunctionalTest.kt`
- **Committed in:** `60aa4ab`, `a297ee5`

**3. [Rule 3 - Blocking] Gradle daemon out of memory on the first run**
- **Found during:** Task 1 verify
- **Issue:** The first `checkAdrs spotlessCheck` ended with `OutOfMemoryError: GC overhead limit exceeded` and a stopped daemon, while sibling executors ran their own Gradle daemons.
- **Fix:** Re-ran the same command; it started a fresh daemon and passed. No code change.

**4. [Process] Considered Options bullets are derived text**
- **Found during:** Task 1
- **Issue:** ADR-0001 and ADR-0002 had no alternatives section, so their `## Considered Options` had to be written. Per the plan, only options the ADR already describes were listed: ADR-0001 the chosen design, the D-09 wording, folding governance into `ci.yml` and a skipped job standing in for the check; ADR-0002 the `actions`-only analysis and the `java-kotlin` language that the spike tested. ADR-0003 and ADR-0004 gained one connective sentence before their moved alternatives.
- **Impact:** This is new wording, derived from the Context of each ADR, and is covered by the human-judgment deliverable D2.

**Total deviations:** 4 (1 bug, 1 missing critical, 1 blocking, 1 process note). **Impact:** None changes a recorded decision; the stricter rules are the only functional addition, and each is reversible by deleting one rule and its test.

## Issues Encountered

None beyond the deviations above. One duplication to know about: `AdrCheck.kt` carries a private heading-outline routine (`outline`) that mirrors the one in `UpstreamRecordCheck.kt`. Plan 02-16 and 02-17 run in parallel and the upstream check is outside this plan's files, so the two were not merged; a later refactor can extract a shared internal outline helper.

## Verification

- `mise x -- ./gradlew checkAdrs spotlessCheck :build-logic:detekt checkBuildScripts checkUpstreamRecords`: green.
- `mise x -- ./gradlew :build-logic:functionalTest --tests '*AdrCheckFunctionalTest*'`: `tests="24" skipped="0" failures="0" errors="0"`, result XML `build-logic/build/test-results/functionalTest/TEST-io.github.aanbrn.courses.buildlogic.governance.AdrCheckFunctionalTest.xml` present.
- `mise x -- ./gradlew build` (whole repository): BUILD SUCCESSFUL in 10m 20s, including `checkAdrs` through `check`.
- Acceptance: `grep -c '^status: "accepted"$'` prints 1 for each of the four ADRs; `grep -c '^## Considered Options$'` on ADR-0002 prints 1; `grep -c checkAdrs courses.governance.gradle.kts` prints 3; `grep -l 'This is a minimal record' docs/adr/*.md` lists nothing; `grep -c eventmodeling docs/templates/event-model-slice.md` prints 2; the five templates are listed by `ls`.
- Merge note for 02-16: this plan's hunks in `courses.governance.gradle.kts` are one import, the `checkAdrs` block placed before the upstream-records comment, and one line in the `check` list; none touches the `unrecordedMarkers` lines.

## Known Stubs

None. Template files contain angle-bracket placeholders by design; they are excluded from every check and are the deliverable.

## Threat Flags

None. The check reads repository Markdown and writes one report under `build/`; it adds no network, authentication or trust-boundary surface. T-02-44 (tampering with recorded decisions) is mitigated by the structure-only rewrite and the comparison above, and T-02-45 (malformed metadata) by the fail-closed parser and `checkAdrs` in `check`.

## Next Phase Readiness

Plan 02-23 can write the spike-outcome ADRs (numbers from 0005) on `docs/templates/adr.md`; `checkAdrs` rejects a gap, a wrong title number or a dangling `superseded by`. The GH-12 use of the templates by project skills stays with Phase 8 (flagged in the plan).

## Self-Check: PASSED
