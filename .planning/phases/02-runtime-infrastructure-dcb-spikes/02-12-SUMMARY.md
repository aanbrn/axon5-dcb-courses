---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 12
subsystem: infra
tags: [docker-compose, axon-server, licence, github-actions, renovate, quick-start, env-template]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "compose.yaml, compose/infra.yaml with the bare Platform token entry, compose/versions.env and the image checks (plan 02-10); the integration-test CI job and the external Axon Server address of ConditionalAppendIntegrationTest (plans 02-07 and 02-08)"
provides:
  - ".env.example: the committed template of the optional local environment, with no active assignment"
  - "compose service probe (profile probe): network-level proof that axonserver:8024 and axonserver:8124 are reachable from the compose network"
  - "docs/quick-start.md: start, endpoints, the 12-hour licence behaviour, the probe, stop and the test commands"
  - "CI: the step Validate compose files in lint and the required job compose-smoke (host scenario plus in-network probe)"
  - "Renovate: one regex manager over the catalog image-* lines and the compose/versions.env *_IMAGE lines, tag and digest together"
affects: [02-14, phase-03-command-service, phase-06-docker-compose, renovate]

plan_head_before: 2667725da7dc3e2e22fb7718eb65b9d393567cbe
plan_head_after: b50f097828ead006467107154bd525075d0bce49

actuals:
  tokens: 4300
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "The optional credential is documented only as a commented line in the committed template, and passed to the container only as a bare compose entry, so it can never be empty"
    - "The in-network scenario is proved by a profile-gated service that reuses the Axon Server image (it ships curl) instead of a new image"
    - "One Renovate regex manager over both pin locations: the two files carry the same depName, so a single pull request moves tag and digest in both"
    - "A packageRule overridePackageName sends lookups for a mirror host to the canonical registry while the files keep the official host"

key-files:
  created:
    - .env.example
    - docs/quick-start.md
  modified:
    - compose/infra.yaml
    - .github/workflows/ci.yml
    - renovate.json5

key-decisions:
  - "The TCP check of the probe is an HTTP/2 prior-knowledge request to the gRPC port: it exits 0 on any answer and non-zero on a refused or timed-out connection, so every non-zero exit fails the probe (simpler and stricter than parsing curl exit codes of a telnet:// probe, which always times out on an open port)"
  - "Renovate looks Axon Server up on Docker Hub through overridePackageName instead of switching the catalog reference: docker.axoniq.io lists tags but answers every manifest request with 429, and the override keeps the official host in the pinned references"
  - "The Renovate regexes are not anchored with ^: with an anchor the manager found no dependencies in either file, so \\b marks the start of the key"
  - "PostgreSQL updates are limited to the 18.x line with allowedVersions; a major upgrade needs a dump and restore of the data volume and stays a deliberate decision"

patterns-established:
  - "Docker Compose commands in verification run with POSTGRES_PORT and a worktree-unique COMPOSE_PROJECT_NAME, then docker compose down -v"

requirements-completed: [INFRA-03, INFRA-05]

coverage:
  - id: D1
    description: ".env.example documents the optional AxonIQ Platform token as a commented line with the 12-hour explanation and the empty-value warning, and has no active assignment (INFRA-03, D-25 as corrected by D-47)"
    requirement: INFRA-03
    verification:
      - kind: other
        ref: "grep -cE '^[A-Z_]+=' .env.example prints 0; grep -c '^# AXONIQ_PLATFORM_AUTHENTICATION=<environment-id>:<token>$' .env.example prints 1"
        status: pass
    human_judgment: false
  - id: D2
    description: "With the token defined in the env file the Axon Server container receives it; with the line commented or absent the container has no such variable"
    requirement: INFRA-03
    verification:
      - kind: other
        ref: "docker compose --env-file <scratch with dummy token> config --format json | jq reports example-id:example-token; --env-file .env.example reports absent"
        status: pass
    human_judgment: false
  - id: D3
    description: "A probe-profile service on the compose network reaches http://axonserver:8024/actuator/health and axonserver:8124 (in-network scenario, D-26 and D-46)"
    requirement: INFRA-03
    verification:
      - kind: integration
        ref: "docker compose up -d --wait && docker compose --profile probe run --rm probe (exit 0, three runs)"
        status: pass
    human_judgment: false
  - id: D4
    description: "docs/quick-start.md explains start and stop, endpoints and the licence behaviour (12 hours, a restart opens a new window, a token lifts the limit; INFRA-05, D-14)"
    requirement: INFRA-05
    verification:
      - kind: other
        ref: "grep -c '12 hours' docs/quick-start.md prints 2; spotlessCheck (Prettier) green in the full build"
        status: pass
    human_judgment: true
    rationale: "Whether the prose reads clearly to a newcomer is a judgment no test asserts"
  - id: D5
    description: "CI validates compose with docker compose config --quiet and runs compose-smoke (host scenario against localhost:8124 plus the probe), required through ci-success; the job holds no secret beyond the Gradle cache key and no write permission"
    requirement: INFRA-03
    verification:
      - kind: other
        ref: "mise x -- actionlint; mise x -- zizmor --offline --no-progress .github/workflows (No findings); grep counts: config --quiet 1, compose-smoke 2, secrets other than GRADLE_ENCRYPTION_KEY 0"
        status: pass
      - kind: integration
        ref: "the compose-smoke steps run locally: ConditionalAppendIntegrationTest tests=2 skipped=0 failures=0 against localhost:8124, then the probe"
        status: pass
    human_judgment: true
    rationale: "The job itself only runs on GitHub Actions; the first pull request run of compose-smoke is the final proof"
  - id: D6
    description: "Renovate updates each image's tag and digest together in the catalog and compose/versions.env (INFRA-09)"
    verification:
      - kind: other
        ref: "renovate-config-validator --strict renovate.json5 passes; renovate --platform=local --dry-run=extract lists image-axonserver, image-postgres, image-ryuk, AXONSERVER_IMAGE and POSTGRES_IMAGE; --dry-run=lookup resolves axoniq/axonserver on index.docker.io without a 429"
        status: pass
    human_judgment: true
    rationale: "A real update pull request only appears when a newer tag exists; the configuration is proved by extraction and lookup, not by an actual update"

duration: 11min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 12: Local Runtime User Slice, Compose Smoke Test and Image Renovation Summary

**A newcomer starts the runtime from `docs/quick-start.md` knowing about the 12-hour Axon Server limit and the optional Platform token (`.env.example`, never active, never empty), CI lints compose and proves the host and in-network connection scenarios in a required `compose-smoke` job, and Renovate moves each image's tag and digest in the catalog and `compose/versions.env` together.**

## Performance

- **Duration:** about 11 min of work, plus a full `./gradlew build` of about 8 min
- **Started:** 2026-11-23T03:31:12Z
- **Completed:** 2026-11-23T03:45:00Z
- **Tasks:** 2 (a tracer and an auto task)
- **Files modified:** 5 (2 created, 3 modified)

## Accomplishments

- The optional token path is safe by construction. `.env.example` (header `SPDX-License-Identifier: MIT`) explains the copy to the untracked `.env`, carries the single commented line `# AXONIQ_PLATFORM_AUTHENTICATION=<environment-id>:<token>` with the 12-hour explanation and the empty-value hazard, and lists the commented development overrides `POSTGRES_PASSWORD`, `QUERY_SERVICE_DB_PASSWORD`, `AUTOMATION_SERVICE_DB_PASSWORD` and `POSTGRES_PORT` with their defaults in prose. `docker compose --env-file <scratch> config --format json` showed the dummy token on `axonserver`; with `--env-file .env.example` the variable was absent.
- The in-network scenario is probed. `docker compose --profile probe run --rm probe` runs inside the compose network on the Axon Server image (it ships `curl`, no new image is pinned): a health request to `http://axonserver:8024/actuator/health`, then an HTTP/2 prior-knowledge request to `axonserver:8124` with retries. It exited 0 three times and does not start with `docker compose up`.
- `docs/quick-start.md` covers prerequisites, start, the endpoints table, the licence section (about 12 hours after start, every restart or recreate opens a new window, `docker compose restart axonserver` for a fresh one, token through `.env`, never empty), the probe, stop (`down` and `down -v`) and the test commands including the external-address integration test. It follows `docs/tooling.md` style and carries no version numbers.
- CI: the `lint` job validates the compose files with `docker compose config --quiet` (no `.env`, no secret). The new `compose-smoke` job (`permissions: contents: read`, `persist-credentials: false`, SHA-pinned actions as elsewhere, `timeout-minutes: 20`) starts the stack, runs `ConditionalAppendIntegrationTest` against `localhost:8124`, runs the probe, prints the container logs on failure and always runs `docker compose --profile probe down -v`. It is in the `needs` of `ci-success`.
- Renovate: a regex manager "Container images: tag and digest in the version catalog and compose/versions.env" reads the `image-*` catalog keys and the `*_IMAGE` variables; a package rule keeps those updates un-automerged with a PR note about `checkComposeVersions`; PostgreSQL is limited to 18.x. This also closes the item that plans 02-07, 02-08 and 02-10 deferred: the catalog image lines were not tracked by any manager before.

### Renovate extraction (local, `LOG_LEVEL=debug renovate --platform=local --dry-run=extract`)

```text
"replaceString": "image-axonserver = \"docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot@sha256:90a477bc...768577\""   (packageFile gradle/libs.versions.toml)
"replaceString": "image-postgres = \"postgres:18.7@sha256:eb63372c...841e1a\""                                          (packageFile gradle/libs.versions.toml)
"replaceString": "image-ryuk = \"testcontainers/ryuk:0.14.0@sha256:7c1a8a9a...b8cd28f0\""                                  (packageFile gradle/libs.versions.toml)
"replaceString": "AXONSERVER_IMAGE=docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot@sha256:90a477bc...768577"           (packageFile compose/versions.env)
"replaceString": "POSTGRES_IMAGE=postgres:18.7@sha256:eb63372c...841e1a"                                                  (packageFile compose/versions.env)
```

Each entry also carries `currentValue` and `currentDigest` and `datasource: docker`. The lookup run (`--dry-run=lookup`) queried `index.docker.io/axoniq/axonserver` and `postgres` without a 429 and reported no newer tag or digest.

## Task Commits

1. **Task 1: Optional token, in-network probe and quick start (tracer)** - `e3fe39b` (feat)
2. **Task 2: CI compose validation, compose-smoke and Renovate manager** - `b50f097` (ci)

**Plan metadata:** committed with this SUMMARY (docs).

The tracer feedback gate ran in its automated branch (`human_verify_mode` `end-of-phase`, the tracer's `<verify>` carries only `<automated>`): both verify commands passed end to end (token proof, then compose up, probe, compose down), so Task 2 started.

## Files Created/Modified

- `.env.example` - commented template of the optional environment, no active assignment
- `docs/quick-start.md` - the local runtime quick start with the licence behaviour
- `compose/infra.yaml` - the `probe` service (profile `probe`)
- `.github/workflows/ci.yml` - step "Validate compose files" in `lint`, job `compose-smoke`, `ci-success` needs
- `renovate.json5` - image manager, no-automerge rule, PostgreSQL major limit, Axon Server lookup override

## Decisions Made

See `key-decisions` above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Renovate could not look up `docker.axoniq.io` (RESEARCH A14), handled by an override instead of switching the reference**
- **Found during:** Task 2 (`renovate --platform=local --dry-run=lookup`)
- **Issue:** `docker.axoniq.io` answers every manifest request with `429 Too Many Requests` (10 failures in the run; `curl` with a valid Docker Hub bearer token reproduces it persistently, while tag listing returns 200), so Renovate could never resolve a digest. `registry-1.docker.io` serves the identical digest `sha256:90a477bc...` for `axoniq/axonserver:2026.1.8-nonroot`.
- **Fix:** the plan's contingency was to switch the catalog reference to `axoniq/axonserver` and regenerate `compose/versions.env`. A package rule `overridePackageName: "axoniq/axonserver"` for the dependency `docker.axoniq.io/axoniq/axonserver` reaches the same goal with a smaller blast radius: the pinned references, the Testcontainers image and `compose/versions.env` stay unchanged, and `checkComposeVersions` needs no regeneration. The second lookup run resolved `index.docker.io/axoniq/axonserver` with 0 failures.
- **Files modified:** `renovate.json5`
- **Verification:** `renovate-config-validator --strict` passes; lookup log shows `getDigest(https://index.docker.io, axoniq/axonserver, 2026.1.8-nonroot)` and no 429.
- **Committed in:** `b50f097`

**2. [Rule 1 - Bug] Anchored regexes found nothing**
- **Found during:** Task 2 (extraction run)
- **Issue:** with `^` at the start of both `matchStrings`, the log reported "No dependencies found in file for custom regex manager" for `compose/versions.env` and `gradle/libs.versions.toml`.
- **Fix:** replaced `^` with `\b`; all five images were then extracted.
- **Files modified:** `renovate.json5`
- **Verification:** the extraction lines quoted above.
- **Committed in:** `b50f097`

**3. [Rule 1 - Bug] The planned TCP probe cannot tell an open port from a closed one by exit code alone**
- **Found during:** Task 1 (testing inside the Axon Server container)
- **Issue:** `curl telnet://host:8124` against the open gRPC port prints binary data and exits 28 (timeout), against a closed port exits 7; treating only 7 as failure works but reads as a hack.
- **Fix:** `curl --http2-prior-knowledge` against the gRPC port exits 0 on an open port and 7 on a refused one, so the probe fails on any non-zero exit.
- **Files modified:** `compose/infra.yaml`
- **Verification:** both exit codes observed inside the container; the probe exits 0 against the running stack.
- **Committed in:** `e3fe39b`

---

**Total deviations:** 3 auto-fixed (2 bugs, 1 blocking)
**Impact on plan:** none changes the scope; the first replaces the plan's A14 contingency by an equivalent that touches fewer files.

## Issues Encountered

- Port 5432 on the host is held by the developer's PostgreSQL, so every compose verification ran with `POSTGRES_PORT=55432` and `COMPOSE_PROJECT_NAME=courses-wt12`, and ended with `docker compose down -v`; `docker ps -a` and `docker volume ls` show nothing left for that project.
- The Renovate validator prints "RE2 not usable, falling back to RegExp" on this machine (the native module of the mise-installed Renovate is not built); validation still ends with "Config validated successfully". It is a local environment quirk, unrelated to the change.
- The local extraction also lists the spike compose files under `.planning/spikes` and `.claude/skills/spike-findings-*/sources` (built-in docker-compose manager: `eclipse-temurin:25-jre`, `spike005-*:latest`). They are not part of this plan; they can produce noisy Renovate pull requests and belong to a decision about excluding `.planning/spikes` and the spike skill sources from Renovate (deferred: out of this plan's scope, and no `deferred-items.md` was written to avoid a merge clash with the parallel plan 02-11 worktree).
- The `compose-smoke` job cannot run before a pull request exists; its first run on GitHub Actions is the final proof. The same commands were run locally against the compose stack.

## Known Stubs

None.

## Threat Flags

None. The surface of this plan is the plan's own threat model: T-02-28 (no active token line, dummy value only in a scratch file, gitleaks passed on both commits), T-02-29 (commented line, documented hazard, bare pass-through), T-02-30 (`permissions: contents: read`, `persist-credentials: false`, SHA-pinned actions, no secret other than the Gradle cache key, guarded count 0), T-02-31 (one regex manager over both files, no automerge, `checkImageReferences` and `checkComposeVersions` unchanged and green).

## User Setup Required

None - no external service configuration required. The AxonIQ Platform token is optional and documented in `docs/quick-start.md`.

## Next Phase Readiness

- Plan 02-14 (`doctor`) can read `.env` and fail on an empty `AXONIQ_PLATFORM_AUTHENTICATION` as the threat register expects.
- The first service that runs inside compose (Phases 3 and 6) replaces the probe by a functional gRPC client; the probe stays as the network-level smoke test.
- The `compose-smoke` check is a new required context through `ci-success`; its name does not change the ruleset because the ruleset requires only `ci-success`.
- No blockers.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*

## Self-Check: PASSED

- Created files exist: `.env.example`, `docs/quick-start.md`; `e3fe39b` and `b50f097` are reachable from HEAD (2 commits since `2667725`).
- Acceptance greps: active assignments in `.env.example` 0; commented token line 1; `12 hours` in the quick start 2; `profiles: [probe]` 1; `docker compose config --quiet` in `ci.yml` 1; `compose-smoke` 2; `compose/versions.env` in `renovate.json5` 5; secrets other than `GRADLE_ENCRYPTION_KEY` 0; `checkImageReferences` and `checkComposeVersions` green.
- Verification: token pass-through proof PASS; compose up, probe, `down -v` PASS; `actionlint` clean; `zizmor --offline` no findings; `renovate-config-validator --strict` passes; Renovate extraction lists all five images; `mise x -- ./gradlew build` finished green (exit 0).
