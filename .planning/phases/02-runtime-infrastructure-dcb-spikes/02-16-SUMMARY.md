---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 16
subsystem: governance
tags: [upstream-records, axon, dcb, markers, issue-drafts, kotlin]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "DecisionCaptureEngine, conflict retry, KotlinTagResolver and AxonServerEnvironment (plans 02-07 and 02-09); checkUpstreamRecords and the record and issue templates (plan 02-15)"
provides:
  - "Seven records under docs/upstream/: UP-0001 to UP-0005, UP-0007 and UP-0013, each with removal criteria and a concrete verification"
  - "Upstream issue drafts with minimal reproductions in UP-0001, UP-0002, UP-0003, UP-0004, UP-0007 and UP-0013; UP-0005 links the existing upstream issue"
  - "UPSTREAM markers at every Axon workaround site: UP-0001 (class and decorator registration), UP-0002 (diagnose function), UP-0003 (retry order constant and bus registration), UP-0004 (resolver registration, existing class marker kept), UP-0007 (AxonServerEnvironment KDoc)"
  - "checkUpstreamRecords green over real data, without the temporary unrecordedMarkers bridge"
affects: [02-17 upstream records UP-0006 and UP-0008 to UP-0012, 02-18 UPS-04 advisory job, 02-19 courses.governance.gradle.kts, 02-22 issue publication, phase-03 entities (UP-0005 gets a workaround and markers)]

actuals:
  tokens: 11700
  tasks: 2
  commits: 2
plan_head_before: 34196d2c9701b8d97559ef056ffdbef9c7c605d7
plan_head_after: c6fdaca129461465f2b348522d099bf73f0aed48

tech-stack:
  added: []
  patterns:
    - "A record states a mechanism it can prove: UP-0002 carries a plain-JDK reproduction of the CompletionException wrapping that was run, not only the Axon symptom"
    - "Records link source lines at the released tag (axon-5.4.2, axoniq-5.4.2), so a link stays true after upstream moves on"
    - "An existing upstream issue is linked and the record is set to reported with the issue's opening date; no duplicate draft is written"

key-files:
  created:
    - docs/upstream/0001-decision-capture-extension-point.md
    - docs/upstream/0002-conflict-diagnosis-completion-exception.md
    - docs/upstream/0003-remote-conflict-classification.md
    - docs/upstream/0004-tag-resolver-value-classes.md
    - docs/upstream/0005-target-entity-id-value-classes.md
    - docs/upstream/0007-shared-testcontainer-latest-image.md
    - docs/upstream/0013-axon-server-dcb-context-flag.md
  modified:
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngine.kt
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/retry/ConflictRetry.kt
    - tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironment.kt
    - build-logic/src/main/kotlin/courses.governance.gradle.kts

key-decisions:
  - "UP-0005 is status reported, not open: AxonIQ/AxonFramework#3761 (Support Kotlin value class for @TargetEntityId, open since 2025-10-08) already covers it, so the record links it, sets reported to 2025-10-08 and writes no draft; the extra findings are a possible comment, which is a publication step for plan 02-22"
  - "UP-0013 records that /v2/contexts reports dcbContext true for the same context, so the misleading signal is specifically the v1 endpoint; the draft targets the AxonIQ forum or support because Axon Server has no public tracker"
  - "UP-0003's draft targets AxonIQ/axoniq-framework, where the connector converts the remote error and drops the type; the retry placement itself is in AxonFramework and is cited"
  - "The UP-0002 marker moved from the class to the diagnose function, as the plan asks; UP-0001 stays on the class"

patterns-established:
  - "Marker comments use // lines next to the workaround, or a KDoc paragraph starting with UPSTREAM: where the plan asks for the KDoc, with the concept named instead of a forbidden class name"

requirements-completed: [UPS-01, UPS-02, UPS-03]

coverage:
  - id: D1
    description: "Records UP-0001 to UP-0003 (decision-capture extension point, CompletionException in conflict diagnosis, remote conflict classification) with removal criteria, a concrete verification and a draft each"
    requirement: UPS-01
    verification:
      - kind: command
        ref: "mise x -- ./gradlew checkUpstreamRecords (BUILD SUCCESSFUL); index lists UP-0001 to UP-0003"
        status: pass
    human_judgment: false
  - id: D2
    description: "Records UP-0004, UP-0005, UP-0007 and UP-0013 (value-class tags, value-class target entity id, latest test-container image, dcbContext flag); UP-0005 and UP-0013 have workaround false and no marker"
    requirement: UPS-01
    verification:
      - kind: command
        ref: "mise x -- ./gradlew checkUpstreamRecords; grep -cE '^\\| UP-(000[1-57]|0013) ' build/reports/governance/upstream-index.md prints 7"
        status: pass
    human_judgment: false
  - id: D3
    description: "Every Axon workaround carries its UPSTREAM marker and markers and records agree in both directions; the temporary unrecordedMarkers bridge is gone"
    requirement: UPS-02
    verification:
      - kind: command
        ref: "grep -rho 'UPSTREAM: UP-000[1-3]' backend | sort -u | wc -l prints 3; grep -rho 'UPSTREAM: UP-000[47]' backend tooling | sort -u | wc -l prints 2; grep -c unrecordedMarkers courses.governance.gradle.kts prints 0"
        status: pass
    human_judgment: false
  - id: D4
    description: "No private paths or e-mail addresses in any record or draft, and no marker for UP-0005 or UP-0013"
    requirement: UPS-03
    verification:
      - kind: command
        ref: "grep -rlE '/Users/|@[a-z0-9.-]+\\.(com|io|org)' docs/upstream | wc -l prints 0; grep -rc 'UPSTREAM: UP-0005|UP-0013' backend tooling | grep -vc ':0$' prints 0"
        status: pass
    human_judgment: false
  - id: D5
    description: "The issue drafts are accurate, self-contained and fit to publish upstream"
    requirement: UPS-03
    verification: []
    human_judgment: true
    rationale: "Whether a draft is accurate, fair to the maintainers and worth publishing is an editorial judgment of the author, which is exactly the approval gate of plan 02-22."

duration: 70min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 16: Axon upstream records Summary

**Seven upstream records (UP-0001 to UP-0005, UP-0007, UP-0013) with verified root causes, issue drafts that carry minimal reproductions, UPSTREAM markers at every Axon workaround site, and `checkUpstreamRecords` green over real data with the temporary bridge deleted**

## Performance

- **Duration:** about 70 min
- **Started:** 2026-11-23T05:20:00Z (estimated; not captured at start)
- **Completed:** 2026-11-23T05:50:00Z
- **Tasks:** 2 (a tracer and an auto task)
- **Files modified:** 12 (7 created, 5 modified), 905 insertions and 7 deletions

## Accomplishments

- **Records with evidence, not recollection.** Source lines were read at the released tags and are linked at those tags. UP-0002's root cause was reproduced in plain JDK 25: a stage after a composed failure sees `CompletionException caused by IllegalStateException`, while a direct failed future shows the bare exception. That explains why `ConflictDiagnosingEventStorageEngine` works in memory (rejection thrown directly) and not on Axon Server (rejection relayed through `exceptionallyCompose` in the connector's commit).
- **Existing reports were searched before drafting** (GitHub issues of `AxonIQ/AxonFramework` and `AxonIQ/axoniq-framework`, the AxonIQ forum). One hit is a true duplicate: `AxonFramework#3761` for the value-class `@TargetEntityId`, so UP-0005 links it and is `reported` with the opening date 2025-10-08. Related but not duplicate: `axoniq-framework#299` (same commit path, closed, different defect) in UP-0002 and `axoniq-framework#221` (licence resolution of the same test container) in UP-0007.
- **A new finding while confirming UP-0013 on a throwaway Axon Server 2026.1.8:** `GET /v2/contexts` reports `dcbContext: true` for the `default` context, so only the v1 endpoint is misleading. Recorded in the record and the draft.
- **Markers:** UP-0001 on the class KDoc site and on the decorator registration, UP-0002 on `diagnose`, UP-0003 on the order constant and the retrying bus registration, UP-0004 on the resolver registration (the class marker already existed), UP-0007 in the `AxonServerEnvironment` KDoc. UP-0005 and UP-0013 have none, as `workaround: false` requires.
- **Bridge removal.** The `unrecordedMarkers` property and its comment in `courses.governance.gradle.kts` are gone.

## Task Commits

1. **Task 1: UP-0001 to UP-0003 with markers (tracer)** - `279586d` (docs)
2. **Task 2: UP-0004, UP-0005, UP-0007, UP-0013 with markers and bridge removal** - `c6fdaca` (docs)

The tracer feedback gate ran in its automated branch: the `<verify>` commands (`checkUpstreamRecords`, the index count of 3 and the marker count of 3) were re-run after the last change to Task 1 and passed before Task 2 started.

**Plan metadata:** committed with this SUMMARY (docs).

## Files Created/Modified

- `docs/upstream/0001-...` to `0005-...`, `0007-...`, `0013-...` - the seven records and their drafts
- `capture/DecisionCaptureEngine.kt` - UP-0002 marker moved onto `diagnose`
- `CommandServiceConfigurationEnhancer.kt` - UP-0001, UP-0003 and UP-0004 markers at the registrations
- `retry/ConflictRetry.kt` - UP-0003 marker at the decoration order constant
- `AxonServerEnvironment.kt` - UP-0007 marker in the KDoc, concept-named
- `courses.governance.gradle.kts` - the `unrecordedMarkers` bridge deleted

## Decisions Made

See `key-decisions` above. The two worth remembering: UP-0005 is `reported` through an existing issue rather than drafted, and UP-0013 pins the misleading signal to the v1 endpoint.

## Deviations from Plan

### Authorized scope extension (orchestrator note)

**1. [Authorized] Deleted the `unrecordedMarkers` bridge in `courses.governance.gradle.kts`**
- **Found during:** Task 1 (the first record for a bridged id makes `checkUpstreamRecords` fail)
- **Issue:** plan 02-15 left a temporary bridge for UP-0001, UP-0002 and UP-0004; the file is not in this plan's `files_modified`.
- **Fix:** the orchestrator authorized deleting exactly that property and its comment. Because Task 1 adds the records for UP-0001 and UP-0002 and the Task 1 verify has to be green, Task 1 first shrank the list to `UP-0004` (`279586d`), and Task 2, which adds the last record, deleted the property and its comment (`c6fdaca`). Nothing else in the file was touched, so the merge with plan 02-19 stays a small hunk.
- **Files modified:** `build-logic/src/main/kotlin/courses.governance.gradle.kts`
- **Verification:** `grep -c unrecordedMarkers` prints 0, `checkUpstreamRecords` green with 7 records.
- **Committed in:** `279586d`, `c6fdaca`

### Auto-fixed Issues

**2. [Rule 3 - Blocking] Reference-style link layout required by Prettier**
- **Found during:** Task 1 (`spotlessCheck`)
- **Issue:** Prettier rewrites a reference link definition onto one line; my wrapped form failed `spotlessCheck`.
- **Fix:** `spotlessApply` in both tasks; no content change.
- **Committed in:** `279586d`, `c6fdaca`

---

**Total deviations:** 1 authorized scope extension, 1 auto-fixed (1 blocking). **Impact:** none on scope beyond the orchestrator-authorized hunk.

## Issues Encountered

- GitHub code search is limited to 30 requests per minute and was exhausted once; the remaining searches ran paced. No result was lost.
- The first read of `/v1/public/context` on the throwaway server returned an empty list because the context was still being created; the record's reproduction says to wait for the log line.
- Reading the Axon Server image through `docker.axoniq.io` was not needed: the pull-through alias image `axoniq/axonserver:2026.1.8-nonroot` was already local. The record uses the catalog name.
- Not verified here: UP-0001 to UP-0003 describe observations from the phase spikes (43 wrapped commit failures, the erased remote error type, the four gaps of the public seam). The mechanism of UP-0002 and the source lines of all records were checked now; the Axon Server runs were not repeated, because `DecisionCaptureIntegrationTest` of plan 02-09 already exercises them on every build.

## Verification

- `mise x -- ./gradlew checkUpstreamRecords spotlessCheck :command-service:detekt :tooling:test-support:detekt`: BUILD SUCCESSFUL; the index lists the 7 records of this plan.
- `mise x -- ./gradlew build --console=plain` (whole repository, run on the final commit): BUILD SUCCESSFUL in 10m 56s, 169 tasks (the machine was shared with other builds).
- Acceptance greps: 3 record files for UP-0001 to UP-0003 and 7 for the plan; `UPSTREAM: UP-0001` in the enhancer 1; markers for UP-0001 to UP-0003 in `backend` 3 distinct, for UP-0004 and UP-0007 in `backend` and `tooling` 2 distinct; `workaround: false` once in each of UP-0005 and UP-0013; markers for UP-0005 or UP-0013 0; private-data grep 0; `unrecordedMarkers` in the governance script 0.
- Pre-commit hooks (format, gitleaks, reuse) ran on both commits and passed.

## Known Stubs

None.

## Threat Flags

None. The plan adds text and comments only. T-02-40 is mitigated by the static grep (`grep -rlE '/Users/|@[a-z0-9.-]+\.(com|io|org)' docs/upstream` prints 0) and by drafts that contain only minimal reproductions; T-02-41 by the markers and the two-way `checkUpstreamRecords`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-17 writes UP-0006 and UP-0008 to UP-0012; the ids and marker sites are disjoint from this plan.
- Plan 02-22 publishes after the author approves: UP-0001, UP-0002, UP-0003, UP-0004, UP-0007 (issues for `AxonIQ/AxonFramework` and `AxonIQ/axoniq-framework`), UP-0013 (forum or support post) and a possible comment on `AxonFramework#3761` for UP-0005. On publication each record moves to `reported` with its link and date.
- Phase 3: when entities register with the underlying `UUID` id type, UP-0005 sets `workaround: true` and gets its markers.
- No blockers.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*

## Self-Check: PASSED

- All 7 created record files exist under `docs/upstream/`; `279586d` and `c6fdaca` are reachable from HEAD.
- `checkUpstreamRecords`, `spotlessCheck`, detekt and the full `./gradlew build` are green.
