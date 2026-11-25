---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 10
subsystem: infra
tags: [docker-compose, axon-server, dcb, postgresql, gradle, governance, testkit, container-images]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "Catalog image entries image-axonserver and image-postgres with tag and digest, and ConditionalAppendIntegrationTest with the external Axon Server address (plan 02-07)"
provides:
  - "compose.yaml (project courses) including compose/infra.yaml with the generated compose/versions.env"
  - "Digest-pinned Axon Server with the DCB default context created at boot, fixed hostname axonserver, bare optional Platform token, and PostgreSQL 18 with per-service databases and login roles"
  - "generateComposeVersions (root) and the committed compose/versions.env"
  - "GeneratedFileCheck (generic freshness check, reused for .env.example in Phase 6) and checkComposeVersions"
  - "ImageReferenceCheck and checkImageReferences: compose images must be variables, catalog images must carry tag and digest"
affects: [02-11, 02-14, phase-03-command-service, phase-04-query-service, phase-06-docker-compose, renovate]

plan_head_before: 77639dfd7a10f499605b5112eeb5af0bca8c7942
plan_head_after: a7177c84332f2deba64114c0c5d67248e0719c5e

actuals:
  tokens: 7200
  tasks: 2
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Image references reach compose only as ${VAR} interpolations of values generated from the catalog into a committed env file that is checked for freshness in check"
    - "A generic generated-file check (expected text, committed file, fix command) instead of one task type per generated file"
    - "The optional Platform token is passed as a bare environment entry, so an absent variable never reaches the container"
    - "Published ports bind to 127.0.0.1"

key-files:
  created:
    - compose.yaml
    - compose/infra.yaml
    - compose/versions.env
    - compose/postgres/init/01-service-databases.sh
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GeneratedFileCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GeneratedFileCheckFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  modified:
    - build-logic/src/main/kotlin/courses.root.gradle.kts
    - build-logic/src/main/kotlin/courses.governance.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt

key-decisions:
  - "The PostgreSQL healthcheck probes over TCP (pg_isready -h 127.0.0.1): during init the image runs a temporary server on the Unix socket only, so the plan's socket probe could report healthy before the service databases exist"
  - "The catalog digest rule is stricter than the plan's regex: a colon followed by digits before the first slash is a registry port, not a tag, so registry:5000/app@sha256:... fails as untagged"
  - "A missing compose/versions.env is reported by Gradle's own input validation (it names the file), not by a custom message, to keep the plan's @InputFile contract for GeneratedFileCheck"
  - "The two catalog image lines of checkComposeVersions duplicate the two of generateComposeVersions (separate precompiled plugins that cannot share a value); a drift between them fails the freshness check on the next run"

patterns-established:
  - "TestKit fixtures that need their own gradle/libs.versions.toml rely on defaultLibrariesExtensionName, set in GradleFixture, to avoid the double catalog import"
  - "Compose files are scanned line by line for image: entries, like docs/tooling.md and mise.toml are scanned by ToolingTableCheck, with no YAML parser in build-logic"

requirements-completed: [INFRA-03]

coverage:
  - id: D1
    description: "docker compose up -d --wait in a fresh clone (no .env, no Gradle run) starts the pinned Axon Server with its DCB default context and PostgreSQL 18, both healthy (INFRA-03)"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "docker compose config --quiet && docker compose up -d --wait (twice, no .env present; both services healthy; docker compose down -v after)"
        status: pass
    human_judgment: false
  - id: D2
    description: "A host application reaches the compose Axon Server at localhost:8124 without reconfiguration: ConditionalAppendIntegrationTest passes against it (D-26 host scenario)"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "./gradlew :command-service:integrationTest -Pcourses.axonserver.external=localhost:8124 --tests '*ConditionalAppendIntegrationTest*' --rerun (tests=2 skipped=0 failures=0, twice)"
        status: pass
    human_judgment: false
  - id: D3
    description: "One PostgreSQL server serves both services: the init script creates the databases and login roles query_service and automation_service; the role query_service logs in over TCP with the development password and creates and drops a table"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "docker compose exec postgres psql -U postgres -Atc 'select datname from pg_database where datname in (...)' prints 'automation_service query_service'; psql -h 127.0.0.1 -U query_service"
        status: pass
    human_judgment: false
  - id: D4
    description: "The Axon Server service has no AXONIQ_PLATFORM_AUTHENTICATION in its environment when the variable is absent from .env, and every published port is bound to 127.0.0.1"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "docker compose exec axonserver env | grep -c AXONIQ_PLATFORM prints 0; docker compose ps shows 127.0.0.1:8024->8024, 127.0.0.1:8124->8124, 127.0.0.1:55432->5432"
        status: pass
    human_judgment: false
  - id: D5
    description: "checkComposeVersions fails with 'compose/versions.env is stale; run ./gradlew generateComposeVersions' for a stale or incomplete versions file, passes for a fresh one, and is cached on an unchanged run"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GeneratedFileCheckFunctionalTest.kt (5 tests, failures=0)"
        status: pass
    human_judgment: false
  - id: D6
    description: "checkImageReferences fails naming file and line for a literal compose image, naming the key for a catalog image without tag or without digest, ignores other keys and tables, and is cached on an unchanged run"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt (7 tests, failures=0)"
        status: pass
    human_judgment: false
  - id: D7
    description: "Both checks run in the root check and the whole build is green"
    requirement: INFRA-03
    verification:
      - kind: other
        ref: "mise x -- ./gradlew checkComposeVersions checkImageReferences build --console=plain (BUILD SUCCESSFUL in 6m 7s); a manual literal image in compose/infra.yaml made checkImageReferences fail naming line 37"
        status: pass
    human_judgment: false
  - id: D8
    description: "From inside the compose network the Axon Server gRPC endpoint is reachable by the name axonserver (in-network scenario, D-26)"
    verification: []
    human_judgment: true
    rationale: "Only the health endpoint and the host scenario were exercised; the full gRPC in-network proof belongs to the Phase 3 and 6 services (RESEARCH Open Question 5), as the plan states"

duration: 32min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 10: Local Runtime Compose and Image Governance Summary

**`docker compose up -d --wait` starts the digest-pinned DCB Axon Server (context created at boot, fixed hostname, loopback-only ports) and PostgreSQL 18 with per-service databases from images generated out of the version catalog, and `check` now fails on a stale `compose/versions.env` or any image reference that is not a catalog variable.**

## Performance

- **Duration:** about 32 min (start time estimated; the full `./gradlew build` took 6 min 7 s)
- **Started:** about 2026-11-23T02:50:00Z (estimated; not captured at start)
- **Completed:** 2026-11-23T03:25:00Z
- **Tasks:** 2 (a tracer and a TDD task)
- **Files modified:** 12 (9 created, 3 modified), 638 insertions

## Accomplishments

- The local runtime is proven end to end. `generateComposeVersions` renders `libs.versions.image.axonserver` and `libs.versions.image.postgres` into the committed `compose/versions.env`; `compose.yaml` (project `courses`) includes `compose/infra.yaml` with that file; `docker compose config --quiet` resolves without any `.env`; both services reach `healthy`; `ConditionalAppendIntegrationTest` (2 tests) passes against the compose Axon Server at `localhost:8124`, the host scenario of D-26; `docker compose down -v` leaves nothing behind.
- Axon Server carries the spike 003 and 004 findings: `AXONIQ_AXONSERVER_STANDALONE_DCB=true` creates the DCB `default` context at boot, `hostname: axonserver` is fixed, the Platform token is the bare entry `- AXONIQ_PLATFORM_AUTHENTICATION` (the container had no such variable when `.env` was absent), the update checker is off, and there is no init container.
- PostgreSQL 18.7 mounts its volume at `/var/lib/postgresql` and runs `01-service-databases.sh`, which creates the roles and databases `query_service` and `automation_service` with passwords passed as `psql -v` variables and interpolated as quoted literals. `query_service` logged in over TCP and could create and drop a table, so database ownership works for Flyway later.
- Every published port binds to `127.0.0.1`, and colima forwards loopback-bound ports to the host (curl to `localhost:8024` and the Gradle test on `localhost:8124` both worked).
- `GeneratedFileCheck` is generic (expected text, committed file, fix command) and is the Phase 6 building block for `.env.example`; `ImageReferenceCheck` enforces the two INFRA-09 image rules. Both are `@CacheableTask`, wired into `check`, and covered by 12 TestKit tests (5 and 7).

## Task Commits

1. **Task 1: End-to-end local runtime (tracer)** - `5b95ea3` (feat)
2. **Task 2: Freshness and image-reference checks (TDD)** - RED `91dd2be` (test), GREEN `a7177c8` (feat); no refactor commit was needed

**Plan metadata:** committed with this SUMMARY (docs).

The tracer feedback gate ran in its automated branch (`human_verify_mode` is `end-of-phase`, the tracer's `<verify>` carries only `<automated>`): the verify was re-run end to end after the commit (compose up, host test, database list, compose down) and passed, so Task 2 started.

## TDD Gate Compliance

- **RED:** `91dd2be` adds both TestKit classes. `gsd_run check tdd-red-evidence` returned `RED_EVIDENCE_OK` (`target_test_failed`) for `GeneratedFileCheckFunctionalTest#a stale digest fails naming the file and the command that regenerates it()` from the JUnit XML of the real run (12 of 12 tests failed). Semantic assessment: the test ran and failed on its `shouldContain "compose/versions.env is stale; ..."` assertion because `Task 'checkComposeVersions' not found in root project 'fixture'`, the intended reason for the RED of a new check; there was no compile, fixture or setup fault. The tests that expect success failed with `UnexpectedBuildFailure` for the same reason.
- **GREEN:** `a7177c8` adds the two task types and wires them; 12 of 12 tests pass (`failures="0"`, 5 and 7).
- **REFACTOR:** none.

## Files Created/Modified

- `compose.yaml` - project `courses`, `include:` of `compose/infra.yaml` with `env_file: compose/versions.env`, header explaining the rules
- `compose/infra.yaml` - `axonserver` and `postgres` services, volumes
- `compose/versions.env` - generated `AXONSERVER_IMAGE` and `POSTGRES_IMAGE` (tag and digest)
- `compose/postgres/init/01-service-databases.sh` - per-service roles and databases (executable, shellcheck and shfmt clean)
- `build-logic/.../ComposeVersions.kt` - `ComposeVersions.render` and the `GenerateComposeVersions` task
- `build-logic/.../governance/GeneratedFileCheck.kt`, `ImageReferenceCheck.kt` - the two cacheable checks
- `build-logic/.../courses.root.gradle.kts` - registers `generateComposeVersions`
- `build-logic/.../courses.governance.gradle.kts` - registers `checkComposeVersions` and `checkImageReferences` and adds both to `check`
- `build-logic/.../GradleFixture.kt` - renames the default catalog extension in fixture settings
- the two functional test classes

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] PostgreSQL healthcheck probed the Unix socket**
- **Found during:** Task 1 (writing `compose/infra.yaml`, before the first run)
- **Issue:** the plan's `pg_isready -U postgres -d postgres` connects over the Unix socket. The PostgreSQL image starts a temporary server for the init scripts that listens on the socket only, so the probe can pass while `01-service-databases.sh` is still running, and `--wait` would then return before the service databases exist.
- **Fix:** `pg_isready -h 127.0.0.1 -U postgres -d postgres`, with a comment, and `start_period: 5s`.
- **Files modified:** `compose/infra.yaml`
- **Verification:** both runs reported `postgres` healthy only after the database list was complete.
- **Committed in:** `5b95ea3`

**2. [Rule 3 - Blocking] TestKit fixture could not carry its own catalog**
- **Found during:** Task 2 (GREEN, first run of `ImageReferenceCheckFunctionalTest`)
- **Issue:** `checkImageReferences` reads `gradle/libs.versions.toml` of the project it runs in. A fixture that writes that file hits Gradle's `Multiple 'from' invocations in version catalog`, because Gradle auto-imports it as `libs` and `GradleFixture` also imports the repository catalog as `libs`.
- **Fix:** `defaultLibrariesExtensionName.set("fixtureLibs")` in the fixture's `dependencyResolutionManagement`, so the fixture's own catalog is imported under another name. `GradleFixture.kt` is not in the plan's file list; the change is one line and affects no existing fixture, none of which writes that file.
- **Files modified:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt`
- **Verification:** 12 of 12 functional tests pass, the earlier fixtures of the full build still pass.
- **Committed in:** `a7177c8`

**3. [Rule 1 - Bug] Detekt `MagicNumber` in the new test**
- **Found during:** Task 2, first full `./gradlew build`
- **Issue:** `"0".repeat(64)` inside a test body failed `:build-logic:detekt`.
- **Fix:** a `STALE_DIGEST` constant in the companion object.
- **Files modified:** `GeneratedFileCheckFunctionalTest.kt`
- **Verification:** the next full build was green.
- **Committed in:** `a7177c8`

**4. [Rule 2 - Missing Critical] Stricter catalog image pattern**
- **Found during:** Task 2 (implementing `ImageReferenceCheck`)
- **Issue:** the plan's `^[^@\s]+:[^@\s]+@sha256:[0-9a-f]{64}$` accepts `registry:5000/app@sha256:...` as "tagged", because the registry port looks like a tag.
- **Fix:** a pattern that treats `:digits` before the first slash as a port and requires the tag after the last path segment. Every case in the plan's behaviour list still holds.
- **Files modified:** `ImageReferenceCheck.kt`
- **Verification:** the "without tag" and "without digest" tests pass; the three catalog entries pass.
- **Committed in:** `a7177c8`

---

**Total deviations:** 4 auto-fixed (2 bugs, 1 missing critical, 1 blocking)
**Impact on plan:** none changes the plan's scope; the first and fourth make two plan statements true in cases the plan's wording missed, the second is test infrastructure.

## Issues Encountered

- Port 5432 on the host was held by a developer PostgreSQL, so the verification ran with `POSTGRES_PORT=55432` and `COMPOSE_PROJECT_NAME=courses-wt10` (both environment variables of the compose file, which the plan intends: `127.0.0.1:${POSTGRES_PORT:-5432}`). Nothing holding the port was touched; on a machine with a free 5432 the plan's verbatim command works. Ports 8024 and 8124 were free.
- `docker compose down -v` ran after each verification; `docker ps -a` and `docker volume ls` show nothing left for the `courses-wt10` project.
- The plan's automated command is verified with the two environment variables above, not verbatim, for that reason.
- `checkComposeVersions` has no custom message for a missing `compose/versions.env`; Gradle's input validation names the file instead (see key-decisions).

## Known Stubs

None.

## Threat Flags

None. The surface in this plan (ports on loopback, development passwords, the optional token) is the plan's own threat model (T-02-24 to T-02-27): ports are on `127.0.0.1`, no `env_file: .env`, the token is a bare entry, images are variables checked by `checkImageReferences` and `checkComposeVersions`.

## User Setup Required

None - no external service configuration required. Docker is the only requirement.

## Governed Paths

This plan touches governed paths, so its pull request needs the `governance-approved` label: `build-logic/src/main/kotlin/courses.root.gradle.kts`, `build-logic/src/main/kotlin/courses.governance.gradle.kts` and `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**`.

## Next Phase Readiness

- Phase 3 and 6 services can add their compose files under `compose/`, list them in the root `include:`, and reference images only through variables; `checkImageReferences` enforces that. A new image needs an `image-*` catalog entry and one line in each of `generateComposeVersions` and `checkComposeVersions`.
- Phase 6 can add `.env.example` as a second `GeneratedFileCheck` instance.
- The in-network gRPC scenario (D8) is still open and belongs to the first service that runs inside compose.
- Renovate still does not track the catalog image lines (open since plan 02-07).
- No blockers.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*

## Self-Check: PASSED

- All 9 created files exist on disk; `5b95ea3`, `91dd2be` and `a7177c8` are reachable from HEAD.
- Acceptance greps: `AXONIQ_AXONSERVER_STANDALONE_DCB=true` 1, bare token entry 1, `hostname: axonserver` 1, image lines with digest 2, `/var/lib/postgresql$` 1, `env_file: .env` 0, `checkComposeVersions` 3, `checkImageReferences` 3.
- Functional test XMLs: `GeneratedFileCheckFunctionalTest` tests=5 failures=0, `ImageReferenceCheckFunctionalTest` tests=7 failures=0.
- `mise x -- ./gradlew checkComposeVersions checkImageReferences build --console=plain` is green; shellcheck and shfmt report nothing for the init script.
