---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 17
subsystem: governance
tags: [upstream-records, markers, axon-server, gradle, codeql, colima, docker, renovate]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "checkUpstreamRecords and the record and issue templates (02-15), compose, .env.example and doctor (02-12, 02-14)"
provides:
  - "Records UP-0006 and UP-0008..UP-0012 under docs/upstream/ with front matter, six sections and removal checks"
  - "UPSTREAM: UP-NNNN markers at every workaround site in compose, .env.example, doctor, build-logic, mise.toml and the CodeQL workflow"
  - "Issue drafts for the AxonIQ forum (UP-0006), abiosoft/colima (UP-0011) and github/codeql (UP-0010); existing upstream issues linked for UP-0008, UP-0009 and UP-0012"
affects: [02-16 upstream records, 02-18 dependency-update advisory, 02-22 issue publication, phase 5 documentation site]

actuals:
  tokens: 8400
  tasks: 2
  commits: 2
plan_head_before: 34196d2c9701b8d97559ef056ffdbef9c7c605d7
plan_head_after: 9a1f830d562f6871d39c09d7c9302aa2e9b14b05

tech-stack:
  added: []
  patterns:
    - "A record's issue draft sits in a four-backtick markdown fence, so it stays verbatim publishable and the check's heading outline ignores it"
    - "Every claim in a record was re-run or re-fetched on the execution date; what could not be re-run is stated as such with the follow-up"

key-files:
  created:
    - docs/upstream/0006-empty-platform-token.md
    - docs/upstream/0008-precompiled-plugin-typed-accessors.md
    - docs/upstream/0009-libzip-missing-provenance.md
    - docs/upstream/0010-codeql-kotlin-extractor.md
    - docs/upstream/0011-colima-aaaa-dns.md
    - docs/upstream/0012-docker-containerd-platform-export.md
  modified:
    - compose/infra.yaml
    - .env.example
    - scripts/doctor.sh
    - build-logic/build.gradle.kts
    - mise.toml
    - .github/workflows/codeql.yml

key-decisions:
  - "UP-0012 is fixed-upstream, not reported: moby/moby#52193 was closed as completed on 2026-11-16 after moby/moby#53615 (Docker 29.9.0) fixed the root cause; the workaround stays until every engine is 29.9.0 or newer"
  - "UP-0010 states two unseparated candidate causes (Kotlin 2.4.21 outside the extractor range, Isolated Projects forcing the configuration cache) instead of one"
  - "UP-0011 and UP-0010 drafts are marked as needing a re-run of the reproduction on a clean setup before publication (plan 02-22)"

patterns-established:
  - "Markers sit on their own comment line directly above the workaround; a record lists each marker location"

requirements-completed: [UPS-01, UPS-02, UPS-03]

coverage:
  - id: D1
    description: "Six records (UP-0006, UP-0008..UP-0012) exist, validate and appear in the generated index"
    requirement: UPS-01
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew checkUpstreamRecords; grep -cE '^\\| UP-(0006|000[89]|001[0-2]) ' build/reports/governance/upstream-index.md prints 6"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every workaround in compose, .env.example, doctor, build-logic, mise.toml and the CodeQL workflow carries an UPSTREAM marker and the two-way consistency check passes"
    requirement: UPS-02
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew build --console=plain (BUILD SUCCESSFUL); bash scripts/test-doctor.sh (125 passed); docker compose config --quiet; actionlint; zizmor (no findings)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Records and drafts contain no private data"
    requirement: UPS-03
    verification:
      - kind: other
        ref: "grep -rlE '/Users/|/home/[a-z]|@[a-z0-9.-]+\\.(com|io|org)' docs/upstream | wc -l prints 0"
        status: pass
    human_judgment: true
    rationale: "The grep covers paths and e-mail addresses only; whether a draft is free of anything else private, and whether its wording is right for the maintainers, is the author's judgment before plan 02-22 publishes it"
  - id: D4
    description: "The UP-0010 and UP-0011 minimal reproductions"
    requirement: UPS-03
    verification: []
    human_judgment: true
    rationale: "Neither was re-run on a clean setup: UP-0011 needs a colima profile without custom resolvers (a VM image download that was abandoned) and UP-0010 needs a CodeQL bundle run on Linux x86-64; plan 02-22 re-runs both before publication"

duration: 70min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 17: Runtime, host and build-tooling upstream records Summary

**Six upstream records (UP-0006, UP-0008..UP-0012) with markers at every workaround site and issue drafts or links for each, validated by `checkUpstreamRecords` and a green full build; UP-0006 was re-reproduced against the pinned Axon Server image and UP-0012 turned out to be already fixed upstream**

## Performance

- **Duration:** about 70 min
- **Started:** 2026-11-23T05:25:00Z
- **Completed:** 2026-11-23T06:15:00Z
- **Tasks:** 2 (the first a tracer)
- **Files modified:** 12 (6 created, 6 modified)

## Accomplishments

- UP-0006 (empty Platform token): reproduced on 2026-11-23 against `axoniq/axonserver:2026.1.8-nonroot` (digest equals the catalog pin). The `IndexOutOfBoundsException` at `AxoniqConsoleCoordinationService.kt:69` is certain; what the container does next is not. Without `AXONIQ_AXONSERVER_STANDALONE_DCB` it exits with code 1; with it, the container stayed `running` with dead endpoints in two of three runs. The forum draft says so. A forum search found no report of this (the one Platform topic, about a Raft pre-vote loop, is a different defect).
- UP-0008 (typed `libs` accessors): linked `gradle/gradle#15383`. Verified by deleting the workaround line: `:build-logic:compileKotlin` fails with `Unresolved reference 'accessors'` in four convention plugins.
- UP-0009 (libzip 3.2.2): linked `yarnpkg/berry#7182`. The npm registry confirms 3.2.0 and 3.2.1 carry attestations (published by the release bot) and 3.2.2, still the latest release, does not (published by a person).
- UP-0010 (CodeQL Kotlin extractor): copied ADR-0002's re-enable criteria and made them observable. No upstream issue for Kotlin 2.4.21 exists; precedent `github/codeql#22381` shows each Kotlin release lagging about a month.
- UP-0011 (colima AAAA): searched `abiosoft/colima` and `lima-vm/lima`; `#1275` (AAAA with `network.address=true`) and general DNS reports are related, not duplicates, so a draft was written.
- UP-0012 (Docker containerd export): see the deviation below.
- Markers: `compose/infra.yaml`, `.env.example` and `scripts/doctor.sh` (UP-0006), `scripts/doctor.sh` (UP-0011, UP-0012 twice), `build-logic/build.gradle.kts` (UP-0008), `mise.toml` (UP-0009), `.github/workflows/codeql.yml` (UP-0010 twice). The diffs of the six governed files add comment lines only.

## Task Commits

1. **Task 1 (tracer): runtime and host records with markers** - `00a306c` (docs)
2. **Task 2: build-tooling records with markers** - `9a1f830` (docs)

**Plan metadata:** the commit holding this SUMMARY (docs: complete plan).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] UP-0012 status is `fixed-upstream`, not `reported`**
- **Found during:** Task 1 (searching the trackers before drafting)
- **Issue:** the plan, written from spike 005, assumed `moby/moby#52193` and `docker/cli#6457` were open. `moby/moby#52193` was closed as completed on 2026-11-16: a commenter shows that `moby/moby#53615` ("Always fetch missing layer content", milestone 29.9.0) fixes the root cause `moby/moby#49784`, and Docker 29.10.0 is already released. `docker/cli#6457` was closed as not planned in 2025 (not reproducible for the maintainers).
- **Fix:** the record is `status: "fixed-upstream"` with both issues in `upstream` and `reported: "2026-03-19"` (the opening date of `moby/moby#52193`, as the plan specifies). `workaround: true` keeps the markers; the removal criteria are an engine of 29.9.0 or newer on every developer machine and CI runner, and the verification is the reproduction on such an engine. That the fix covers the Paketo noble run image is likely (same symptom and cause) but unverified; the record says so.
- **Files modified:** `docs/upstream/0012-docker-containerd-platform-export.md`
- **Commit:** `00a306c`

**2. [Rule 2 - Missing critical] A second marker for UP-0012 and UP-0010**
- **Found during:** Tasks 1 and 2
- **Issue:** the plan names one marker site per record, but the containerd workaround lives in two places (the `image-store` check and the snapshotter test inside the `colima` check) and the CodeQL language choice in two (the matrix entry and the `languages` setting).
- **Fix:** a marker at each site, all listed in the records' Workaround sections.
- **Commit:** `00a306c`, `9a1f830`

**3. [Rule 1 - Bug] Accuracy corrections against the plan's wording**
- **Found during:** Tasks 1 and 2
- **Issue:** the plan and spike 004 describe the empty-token defect as always leaving a running container; it is configuration dependent (see Accomplishments). The plan's UP-0010 wording ("extraction under isolated projects saw no sources") hides that the Kotlin version is also outside the extractor range, so the record names both candidate causes.
- **Fix:** the records state what was observed and what was not.
- **Commit:** `00a306c`, `9a1f830`

**Total deviations:** 3 (2 corrections of a premise, 1 added marker sites). **Impact:** none on scope; UP-0012 is the only record whose status differs from the plan.

## Issues Encountered

- **UP-0011 reproduction not re-run on a clean profile.** I started a throwaway colima profile to observe the 10 s lookup, but the VM image download ran at under 1 MB/s; I aborted it, deleted the partial profile and the partial cache file, and left the author's default profile untouched. The Go lookup program and the `docker create`/`docker cp`/`docker start` commands were verified on the existing profile (168 ms, explicit resolvers). The 10 s side rests on spike 005. The record says so, and plan 02-22 must re-run it on a clean profile before publication.
- **UP-0010 is likewise unverified standalone** (no CodeQL bundle was downloaded; the spike of ADR-0002 is the evidence, on macOS only).
- **The plan commit ledger was not written.** The sandbox refuses writes under the shared git directory (as in 02-14); the base commit is the orchestrator's known `34196d2c9701b8d97559ef056ffdbef9c7c605d7`, and `commits:` was measured against it.
- **Observation outside this plan's scope, for the author:** the AxonIQ forum topic 6639 (February 2026, Axon Server Enterprise 2025.2.4) reports that with `standalone-dcb=true` and a valid Platform token on fresh volumes the `_admin` Raft context loops in pre-vote and the `default` DCB context is never created; AxonIQ's workaround is `axoniq.axonserver.autocluster.dcb=true`. This repository's compose file uses `AXONIQ_AXONSERVER_STANDALONE_DCB=true` with an optional token on fresh volumes, so it may be worth checking whether the 2026.1.8 server is affected before the first token-bearing run. It is a different product line and version, so it is not recorded as a defect here.
- Docker images pulled for the checks (`axoniq/axonserver:2026.1.8-nonroot`, `golang:1.25-alpine`, `alpine:3.22`) remain in the local colima store; the temporary containers were removed.

## Known Stubs

None.

## Threat Flags

None. The edits are comments and Markdown records; T-02-56 is mitigated (placeholders and generic version strings only, the private-data grep prints 0, nothing was published or reacted to upstream) and T-02-57 (comment-only diffs in governed files, verified by actionlint, zizmor, `test-doctor.sh`, `docker compose config` and the full build). Read-only upstream lookups (GitHub issue reads, the npm registry, the AxonIQ forum search) carried no project data.

## User Setup Required

None.

## Next Phase Readiness

Ready for plan 02-18 (the advisory matches `image-axonserver`, `gradle`, `npm:renovate` and `action:github/codeql-action`; `colima` and `docker` never match, as the records state). The phase pull request touches governed paths (`compose/infra.yaml`, `.github/workflows/codeql.yml`, `build-logic/build.gradle.kts`, `mise.toml`) and needs the `governance-approved` label. Plan 02-16 must still delete the `unrecordedMarkers` bridge in `courses.governance.gradle.kts`; its records and these six merge in either order because the id sets are disjoint. Plan 02-22 must re-run the UP-0010 and UP-0011 reproductions and obtain the author's approval for each draft before publishing anything.

## Self-Check: PASSED

- Files exist: all six `docs/upstream/` records, and the six modified files carry their markers.
- Commits `00a306c` and `9a1f830` are ancestors of HEAD.
- Acceptance criteria re-run and passing: marker greps (1 per file for UP-0006, 3 for UP-0011 and UP-0012 in doctor, 1 each for UP-0008 and UP-0009, 2 for UP-0010), `ls` count 6, private-data grep 0, index rows 6; `checkUpstreamRecords`, the full `./gradlew build`, `bash scripts/test-doctor.sh` (125 passed), `docker compose config --quiet`, shellcheck, shfmt, actionlint and zizmor all clean.

---

_Phase: 02-runtime-infrastructure-dcb-spikes_
_Completed: 2026-11-23_
