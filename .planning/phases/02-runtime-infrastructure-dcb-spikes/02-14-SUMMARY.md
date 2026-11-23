---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 14
subsystem: tooling
tags: [doctor, bash, testcontainers, colima, gsd-integrity, mise, ci, spotless]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "02-02 MCP servers in .mcp.json, 02-05 pinned Claude Code marketplaces and the tracked-.claude rule, 02-12 .env.example and docs/quick-start.md, 02-13 NOTICE"
provides:
  - "scripts/doctor.sh: one read-only diagnosis with ok/warn/fail/skip lines, a fix command per problem, --ci mode and a final delegation to ./gradlew doctor"
  - ":tooling:doctor with the doctor lifecycle task over verifyNodeVersion, verifyShfmt, verifyTaplo, checkDaemonJvm and the testcontainersProbe JavaExec task"
  - "scripts/test-doctor.sh: 125 shell assertions with fake commands, a fake HOME, a restricted PATH and an idempotency check"
  - "governance/gsd-version pinning the GSD installation that doctor verifies against the manifest hashes"
  - "The Doctor CI job (doctor.sh --ci), required through ci-success"
  - "docs/tooling.md rows and sections for docker compose, compose images, GSD, Claude Code, the Docker engine and Testcontainers setup and doctor; a doctor step in docs/quick-start.md"
  - "The Spotless misc target covers .env.example, NOTICE and governance/gsd-version"
affects: [03-domain-model, phase-06-ci, onboarding]

actuals:
  tokens: 20000
  tasks: 3
  commits: 4
plan_head_before: 344136d6c0427aa42ad5fe9c2c5f8481ccb24172
plan_head_after: d1e4e478825127b902c3490c27a2c6544bd0987e

tech-stack:
  added: []
  patterns:
    - "One implementation per environment check: the shell keeps only what must run before Gradle or what Gradle does not own, and `./gradlew doctor` delegates to the existing root tasks by path (D-48)"
    - "Shell tests run the script under `env -i` with a PATH that holds only fake commands and symlinks to the few real tools it needs, so a machine's own java or docker never leaks into a case"
    - "Secrets are classified by an awk program that prints one state word, never the value"

key-files:
  created:
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - tooling/doctor/build.gradle.kts
    - tooling/doctor/src/main/kotlin/io/github/aanbrn/courses/doctor/Doctor.kt
    - governance/gsd-version
  modified:
    - settings.gradle.kts
    - qodana.yaml
    - .github/workflows/ci.yml
    - docs/tooling.md
    - docs/quick-start.md
    - build-logic/src/main/kotlin/courses.root.gradle.kts

key-decisions:
  - "doctor.sh runs `mise x -- ./gradlew doctor --continue` when mise exists, so the pinned Node and formatter binaries are on the build's PATH even in a shell where mise is not activated"
  - "Check names follow the plan's artifact list: mise, tools, java, docker, image-store, colima, testcontainers-config, gsd, gsd-defaults, mcp, platform-token, claude-code, claude-tracking, gradle"
  - "A missing GSD manifest is a failure with the pinned reinstall command, as AIDEV-07 makes the installation part of the verified environment; --ci skips it"
  - "The unexpected-file allowlist is the two files named in the plan, hooks/package.json and scripts/changeset/README.md; against the real installation 858 of 858 manifest files match and nothing is unexpected"

patterns-established:
  - "doctor prints `<status>  <check>: <detail>` and an indented `fix:` line per fix; the Kotlin probe prints the same format"

requirements-completed: [TOOL-03, TOOL-04, AIDEV-03, AIDEV-07, AIDEV-04]

coverage:
  - id: D1
    description: "scripts/doctor.sh reports every shell check as ok, warn, fail or skip with a fix line, exits 1 on a failure, handles a missing docker or Java, and a second run prints identical output without changing the tree"
    requirement: TOOL-03
    verification:
      - kind: unit
        ref: "bash scripts/test-doctor.sh (125 passed, 0 failed)"
        status: pass
      - kind: other
        ref: "mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh && mise x -- shfmt -d scripts/doctor.sh scripts/test-doctor.sh (no output)"
        status: pass
    human_judgment: false
  - id: D2
    description: "./gradlew doctor runs the existing verifyNodeVersion, verifyShfmt, verifyTaplo and checkDaemonJvm tasks plus testcontainersProbe, and the probe reaches Docker through Testcontainers with the catalog-pinned Ryuk image"
    requirement: TOOL-04
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew doctor --console=plain prints `ok  testcontainers: unix:///var/run/docker.sock, server 29.5.2, ryuk enabled` (grep -c == 1)"
        status: pass
      - kind: other
        ref: "./gradlew doctor --dry-run lists 5 delegated tasks (>= 5)"
        status: pass
    human_judgment: false
  - id: D3
    description: "GSD integrity: version pin, every manifest hash, modified hooks, missing and unexpected files, and global defaults key names; against the real installation 858 of 858 files match"
    requirement: AIDEV-07
    verification:
      - kind: unit
        ref: "scripts/test-doctor.sh GSD integrity and defaults sections"
        status: pass
      - kind: other
        ref: "DOCTOR_ROOT with .claude of the main checkout and this branch's governance/gsd-version: `ok  gsd: version 1.22.0, 858 files match the manifest, no unexpected files`"
        status: pass
    human_judgment: false
  - id: D4
    description: "The Platform token is classified as set, empty or absent without printing its value; an empty definition fails, absence warns about the 12-hour window"
    requirement: AIDEV-04
    verification:
      - kind: unit
        ref: "scripts/test-doctor.sh token cases (secret-sentinel-value never in the output; four empty spellings fail)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Tracked .claude content is limited to settings.json, CLAUDE.md and spike-findings skills, also in --ci"
    requirement: AIDEV-03
    verification:
      - kind: unit
        ref: "scripts/test-doctor.sh claude-tracking cases"
        status: pass
    human_judgment: false
  - id: D6
    description: "The Doctor CI job runs doctor.sh --ci on ubuntu and gates ci-success; actionlint and zizmor are clean"
    requirement: TOOL-03
    verification:
      - kind: other
        ref: "mise x -- actionlint && mise x -- zizmor --offline --no-progress .github/workflows (no findings)"
        status: pass
    human_judgment: true
    rationale: "The job has not run on a GitHub runner yet; mise-action installs every locked tool there for the first time, which only the first CI run can confirm"
  - id: D7
    description: "docs/tooling.md and docs/quick-start.md explain the Docker engine, Testcontainers and doctor setup without version numbers; spotlessMiscCheck provably covers .env.example, NOTICE and governance/gsd-version"
    requirement: TOOL-04
    verification:
      - kind: other
        ref: "checkToolingInventory spotlessCheck; no x.y.z in docs/tooling.md; spotlessMiscCheck probe fails for each of the three files when given trailing whitespace; reuse lint"
        status: pass
    human_judgment: false
  - id: D8
    description: "Docker Desktop half of TOOL-04"
    requirement: TOOL-04
    verification: []
    human_judgment: true
    rationale: "Documented through Testcontainers' automatic detection but not verifiable: no Docker Desktop on the author's machine (flagged assumption in the plan)"

duration: 50min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 14: doctor Summary

**`scripts/doctor.sh` diagnoses mise, Java, Docker and its image store, Testcontainers settings, the pinned GSD installation (SHA-256 per manifest file), MCP servers, the Platform token, colima, Claude Code and tracked `.claude` files read-only with a fix line per problem, and delegates every JDK, Node and formatter check to `./gradlew doctor` with a Testcontainers probe; a `Doctor` CI job runs it in `--ci` mode.**

## Performance

- **Duration:** about 50 min
- **Completed:** 2026-11-23
- **Tasks:** 3 (the first a tracer, the second TDD with a RED and a GREEN commit)
- **Files modified:** 11 (5 created, 6 modified)

## Accomplishments

- `scripts/doctor.sh` has one function per check and a shared `report` that prints `<status>  <check>: <detail>` plus indented `fix:` lines. It exits 1 on a `fail`, 2 on a usage error, and never writes anything. The shell reads no Node, JDK or formatter version (the grep guard returns 0): those run as `./gradlew doctor --continue`, which each failure reports with its own task's hint.
- `:tooling:doctor` adds the `testcontainersProbe` `JavaExec` task (catalog-pinned Ryuk image through `TESTCONTAINERS_RYUK_CONTAINER_IMAGE`) and the `doctor` lifecycle task depending on `:verifyNodeVersion`, `:verifyShfmt`, `:verifyTaplo`, `:checkDaemonJvm`. On the author's colima machine the probe prints `ok  testcontainers: unix:///var/run/docker.sock, server 29.5.2, ryuk enabled`.
- The GSD check verifies the version against `governance/gsd-version` and every manifest hash with `sha256sum` or `shasum`, names modified hooks as hooks, and lists missing and unexpected files (two allowed unlisted files). Against the real installation (858 files) it passes.
- The local-only checks (`gsd`, `gsd-defaults`, `mcp`, `platform-token`, `colima`, `claude-code`) report `skip` under `--ci`; `claude-tracking`, `docker`, `image-store` and the Testcontainers settings still run.
- `ci.yml` has a `doctor` job and `ci-success` needs it. `docs/tooling.md` gains rows for docker compose, the compose images, GSD and Claude Code, the sections "Docker engine and Testcontainers" and "Environment check (doctor)", and `docs/quick-start.md` a "Check your environment" step.
- The Spotless `misc` target now lists `.env.example`, `NOTICE` and `governance/gsd-version`; a probe that appends a trailing-space line makes `spotlessMiscCheck` fail for each of them.

## Task Commits

1. **Task 1 (tracer): end-to-end doctor, Gradle probe, shell tests** - `bb4296a` (feat)
2. **Task 2 (TDD): GSD, defaults, MCP, token, colima, Claude Code and tracking checks**
   - RED - `7a1ca71` (test)
   - GREEN - `52ddcdf` (feat)
3. **Task 3: doctor in CI, documentation, Spotless misc target** - `d1e4e47` (feat)

**Plan metadata:** the commit holding this SUMMARY (docs: complete plan).

## TDD Gate Compliance

- **RED:** `7a1ca71` added 66 failing assertions to `scripts/test-doctor.sh` (59 passed, 66 failed). Each failure was `output lacks ^ok  gsd: ...`, `exit 0, expected 1` and the like: the target checks did not exist yet, and every Task 1 case still passed, so the script parsed and ran. Semantic assessment: the failures are assertion failures on the planned behaviour, not syntax errors, zero-test discovery or fixture crashes. `gsd_run check tdd-red-evidence` supports no shell report format, so this is the durable record rather than a classifier verdict.
- **GREEN:** `52ddcdf` made all 125 assertions pass on the first run; shellcheck and shfmt are clean.
- **REFACTOR:** none needed.

## Decisions Made

See `key-decisions` above. The tracer gate was re-run end to end after Task 1 (`bash scripts/test-doctor.sh`, shellcheck and shfmt, `./gradlew doctor` with the probe line, the dry-run task graph, the acceptance greps), then expansion continued.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Test PATH needed `bash` and `env`**
- **Found during:** Task 1
- **Issue:** the fake commands start with `#!/usr/bin/env bash`, and the restricted test PATH held neither `env` nor `bash`, so every fake exited 127.
- **Fix:** both are symlinked into the test's tool directory.
- **Files modified:** `scripts/test-doctor.sh`
- **Committed in:** `bb4296a`

**2. [Rule 1 - Bug] `gradle` skip message hid the Java failure under `DOCTOR_SKIP_GRADLE=1`**
- **Found during:** Task 1
- **Issue:** the skip variable was tested before the missing-Java condition, so the plan's "reported as skipped because java failed" case printed the test-only reason.
- **Fix:** the Java check comes first.
- **Files modified:** `scripts/doctor.sh`
- **Committed in:** `bb4296a`

**3. [Rule 3 - Blocking] shellcheck findings on the first drafts**
- **Found during:** Tasks 1 and 2
- **Issue:** SC2034 on a flag only the later task read, SC2016 on jq programs, SC2088 on a leading tilde in messages.
- **Fix:** `CI_MODE` selects mode-specific fix hints already in Task 1, the jq programs carry a justified `shellcheck disable=SC2016`, and the defaults messages use `$HOME` text.
- **Files modified:** `scripts/doctor.sh`
- **Committed in:** `bb4296a`, `52ddcdf`

**4. [Rule 3 - Blocking] The plan commit ledger could not be written**
- **Found during:** Task 1 commit
- **Issue:** the sandbox refused to write the ledger file under the worktree's git directory.
- **Fix:** the base commit is the orchestrator's known base `344136d6c0427aa42ad5fe9c2c5f8481ccb24172`, recorded in the frontmatter, and `commits:` was measured against it with `git rev-list --count`.

---

**Total deviations:** 4 auto-fixed (2 blocking test and lint fixes, 1 bug, 1 bookkeeping)
**Impact on plan:** none on behaviour or scope.

## Issues Encountered

- **The GSD check fails inside a worktree.** `.claude/gsd-*` is git-ignored, so an isolated executor worktree has no manifest, and `scripts/doctor.sh` there reports `fail  gsd:` with the pinned reinstall command (which is correct). Task 2's second automated check (`no fail  gsd:`) therefore only passes in the main checkout, where `governance/gsd-version` arrives with the merge. I proved the real check by pointing `DOCTOR_ROOT` at a scratch directory that links the main checkout's `.claude` to this branch's `governance/gsd-version`: 858 of 858 files match and no file is unexpected.
- **Task 3's acceptance command `bash scripts/doctor.sh` on the author's machine.** From this worktree: 10 ok, 3 warn (the global defaults `resolve_model_ids` and `runtime`, the absent Platform token, the skipped Gradle step in the shell-only run) and the worktree-only `gsd` fail described above. A full run without `DOCTOR_SKIP_GRADLE` printed `ok  gradle: every JVM-side check passed`. The `warn  gsd-defaults` result matches the research finding that the author's `~/.gsd/defaults.json` holds those two keys.
- **Not run:** the `Doctor` CI job itself (first run on a GitHub runner); `mise-action` with no `install_args` installs every locked tool there for the first time, including `github:gradle/gradle-profiler`.
- Full `./gradlew build -x integrationTest` was green (8 min 44 s, 164 tasks).

## Known Stubs

None.

## Threat Flags

None. The surfaces are the plan's own threat model: T-02-34 (the awk token state and key-name-only listing, with the `secret-sentinel-value` test and the `(cat|source|.) +[^|]*\.env` grep returning 0), T-02-35 (hash verification, unexpected files, pinned version), T-02-36 (read-only checks, the idempotency test) and T-02-37 (`contents: read`, `persist-credentials: false`, SHA-pinned actions, actionlint and zizmor clean). The only new network-facing behaviour is the transient Ryuk container the probe starts through Testcontainers, as the plan allows.

## User Setup Required

None. Optional: write `AXONIQ_PLATFORM_AUTHENTICATION=<environment-id>:<token>` into `.env` to clear the 12-hour-window warning, and remove the `resolve_model_ids` and `runtime` keys from `~/.gsd/defaults.json` to clear the defaults warning.

## Next Phase Readiness

Every later plan and CI run can call `scripts/doctor.sh` first. Merge notes for the orchestrator: `governance/gsd-version` and `settings.gradle.kts`, `courses.root.gradle.kts` and `ci.yml` are governed paths, so the pull request needs the `governance-approved` label. After the merge, run `scripts/doctor.sh` in the main checkout to confirm the `gsd` check passes there.

## Self-Check: PASSED

- Files exist: `scripts/doctor.sh`, `scripts/test-doctor.sh`, `tooling/doctor/build.gradle.kts`, `tooling/doctor/src/main/kotlin/io/github/aanbrn/courses/doctor/Doctor.kt`, `governance/gsd-version`.
- Commits `bb4296a`, `7a1ca71`, `52ddcdf` and `d1e4e47` are ancestors of HEAD.
- Acceptance criteria re-run and passing: all greps in the three tasks, `doctor.sh --help` names `--ci`, `bash scripts/test-doctor.sh` (125 passed), shellcheck and shfmt clean, `./gradlew doctor` with the probe line and 5 dry-run tasks, actionlint and zizmor, `checkToolingInventory spotlessCheck`, the `spotlessMiscCheck` probe, `reuse lint`, and the full build.

---

_Phase: 02-runtime-infrastructure-dcb-spikes_
_Completed: 2026-11-23_
