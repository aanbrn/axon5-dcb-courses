---
phase: 01-repository-build-ci-foundation
plan: 03
subsystem: infra
tags: [mise, tooling, supply-chain, lockfile, nvmrc, renovate, reuse, zizmor]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: ":platform and convention plugins (plan 01-01), quality gates and TestKit suite (plan 01-02); .java-version"
provides:
  - "mise.toml pinning 13 CLI tools with min_version as the only mise version source"
  - "mise.lock with per-platform checksums for linux-x64, linux-arm64, macos-arm64 and macos-x64, enforced through locked = true"
  - ".mise/locks sidecar lockfiles for the npm (renovate) and pipx (reuse) tools"
  - ".nvmrc as the single Node.js version source, read through idiomatic_version_file_enable_tools"
  - "docs/tooling.md inventory (main table plus Planned tools) following the D-17 five-column contract"
affects: [01-04, 01-05, 01-06, 01-08, 01-10, 01-11, 01-12]

actuals:
  tokens: 76700
  tasks: 3
  commits: 2
plan_head_before: 60797d707442fd231d82b02ff96d17330b780a97
plan_head_after: 4e435f8f6de82025b747bb6071d65c768ea0dfb9

tech-stack:
  added: [mise 2026.11.11, gitleaks, actionlint, shellcheck, lefthook, zizmor, editorconfig-checker, reuse, gradle-profiler, shfmt, taplo, renovate, jq, gh]
  patterns:
    - "Single version source per tool: JDK in .java-version, Node.js in .nvmrc, mise in min_version, CLI tools in mise.toml"
    - "Explicit backends (aqua:, github:, pipx:, npm:) for tools whose registry alias is ambiguous or deprecated"
    - "Lockfile enforced (locked = true) with checksums per platform"

key-files:
  created: [mise.toml, mise.lock, .nvmrc, docs/tooling.md, .mise/locks/npm-renovate/44.239.1~aa673d8d/aube-lock.yaml, .mise/locks/npm-renovate/44.239.1~aa673d8d/package.json, .mise/locks/pipx-reuse/6.3.0~068c1fb0/uv.lock, .mise/locks/pipx-reuse/6.3.0~068c1fb0/pyproject.toml]
  modified: []

key-decisions:
  - "npm:renovate is installed with one exact-version trust-policy exception, trust_policy_excludes = [\"@yarnpkg/libzip@3.2.2\"], approved by the author; the trust policy is not disabled globally"
  - "locked = true is enabled in [settings] once mise.lock exists, so mise refuses anything not pinned with a checksum"
  - "The .mise/locks sidecar lockfiles are committed together with mise.lock because mise.lock references them for the npm and pipx backends"

patterns-established:
  - "Tooling inventory rows carry the exact mise.toml key in backticks and Version source mise (checked by the shell one-liner now and by plan 01-10 later)"
  - "Pinned versions are the newest release at least seven days old at pinning time"

requirements-completed: [TOOL-01, TOOL-02, INFRA-09]

coverage:
  - id: D1
    description: "mise install provisions every pinned CLI tool from mise.toml and mise.lock; every tool answers its version command"
    requirement: TOOL-02
    verification:
      - kind: other
        ref: "mise x -- reuse --version && zizmor --version && lefthook version && editorconfig-checker --version && gradle-profiler --version && shfmt --version && taplo --version (all exit 0)"
        status: pass
    human_judgment: false
  - id: D2
    description: "JDK comes from .java-version and Node.js from .nvmrc; neither is pinned in mise.toml; mise version lives only in min_version"
    requirement: INFRA-09
    verification:
      - kind: other
        ref: "mise ls --current node shows .nvmrc as source; grep -cE '^\"?(java|node)\"? *=' mise.toml prints 0; grep -c '^min_version = ' mise.toml prints 1"
        status: pass
    human_judgment: false
  - id: D3
    description: "mise.lock holds per-platform checksums for four platforms and is enforced with locked = true"
    requirement: TOOL-02
    verification:
      - kind: other
        ref: "mise lock --platform linux-x64,linux-arm64,macos-arm64,macos-x64; mise install succeeds with locked = true"
        status: pass
    human_judgment: false
  - id: D4
    description: "docs/tooling.md names every mise.toml tool key in backticks, has the Planned tools table with the same five columns, and contains no version numbers"
    requirement: TOOL-01
    verification:
      - kind: other
        ref: "key-coverage shell one-liner from Task 3 (0 missing keys of 13), no-version grep, row-presence loop, acceptance greps"
        status: pass
    human_judgment: false
  - id: D5
    description: "Package legitimacy gate answered by the author before any install, including the trust-policy exception for renovate"
    requirement: TOOL-02
    verification: []
    human_judgment: true
    rationale: "A human trust decision about third-party packages cannot be asserted by a test; it is recorded here as the author's explicit approval and decision."

duration: 25min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 03: Tool Bootstrap Summary

**mise-pinned, checksum-locked CLI toolchain (13 tools, four platforms) with JDK and Node.js sourced from `.java-version` and `.nvmrc`, plus a version-free `docs/tooling.md` inventory**

## Performance

- **Duration:** about 25 min for this continuation run (Task 1 and the first part of Task 2 ran in earlier executor sessions and a human checkpoint)
- **Completed:** 2026-11-21T09:35Z
- **Tasks:** 3 (Task 1 checkpoint approved by the author, Task 2 tracer, Task 3 docs)
- **Files modified:** 8 created (mise.toml, mise.lock, .nvmrc, docs/tooling.md, four `.mise/locks` sidecar files)

## Accomplishments

- `mise install` provisions 13 CLI tools plus the JDK (25, from `.java-version`) and Node.js (24, from `.nvmrc`); all 13 tools answer a version command through `mise x --`.
- `mise.lock` locks every tool for `linux-x64`, `linux-arm64`, `macos-arm64` and `macos-x64` with checksums and provenance; `locked = true` makes mise refuse anything not recorded there (T-01-04).
- `actionlint` is pinned through the explicit `aqua:rhysd/actionlint` backend (T-01-05, Pitfall 15); `reuse` is installed with its `charset-normalizer` extra and runs without ImportError.
- `docs/tooling.md` inventories every tool and host prerequisite with its single version source, lists the later-phase tools in `Planned tools` (nine rows, `not yet pinned (Phase N)`), and contains no version numbers.

## Task Commits

1. **Task 1: Package legitimacy gate** - no commit (checkpoint:human-verify, `blocking-human`, approved by the author)
2. **Task 2: End-to-end tool bootstrap (tracer)** - `0b27db7` (feat)
3. **Task 3: Tool inventory** - `4e435f8` (docs)

**Plan metadata:** committed with this SUMMARY (docs: complete plan)

## Files Created/Modified

- `mise.toml` - `min_version`, `[settings]` (`idiomatic_version_file_enable_tools`, `locked`), 13 pinned `[tools]`
- `mise.lock` - per-platform checksums and provenance for 13 tools plus java and node
- `.nvmrc` - `24`, the single Node.js source
- `.mise/locks/npm-renovate/44.239.1~aa673d8d/{aube-lock.yaml,package.json}` - sidecar lock for the renovate dependency tree
- `.mise/locks/pipx-reuse/6.3.0~068c1fb0/{uv.lock,pyproject.toml}` - sidecar lock for reuse and its Python dependencies
- `docs/tooling.md` - tool inventory (main table, Planned tools, notes)

## Decisions Made

- **Trust-policy exception for renovate (author decision).** `mise install` failed for `npm:renovate@44.239.1` with `trust downgrade for @yarnpkg/libzip@3.2.2 (trustPolicy=no-downgrade): earlier published version 3.2.0 had provenance attestation but this version has no trust evidence`. Registry facts, verified before asking: `@yarnpkg/libzip@3.2.2` was published 2025-09-18 by `arcanis` (the Yarn lead maintainer) from github.com/yarnpkg/berry without provenance; `3.2.0` was published by `yarnbot` with SLSA provenance. After an explanation of the risk, the author typed "exclude" and confirmed. The exception is applied exactly and narrowly: `"npm:renovate" = { version = "44.239.1", trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"] }`, with an explanatory comment in `mise.toml` (transitive dependency of renovate, manual publish by the Yarn maintainer without provenance, exact version only, remove when renovate no longer pulls it). `npm.shell_out=true` and any setting that disables the trust policy globally were deliberately not used. The exception is recorded in `mise.lock` as an option of the renovate entry, and the resolved tree is integrity-pinned in the sidecar `aube-lock.yaml`. Revisit on every Renovate bump of `npm:renovate`: if the new release no longer depends on `@yarnpkg/libzip@3.2.2`, delete the exception.
- `locked = true` added under `[settings]` after `mise.lock` was generated (the documented setting name is `locked`, confirmed with `mise settings ls -a`).
- Platforms locked: `linux-x64` (CI), `linux-arm64`, `macos-arm64` (local) and `macos-x64`.
- Pins: gitleaks 8.30.1, actionlint 1.7.12, shellcheck 0.11.0, lefthook 2.2.2, zizmor 1.32.0, editorconfig-checker 4.0.2, reuse 6.3.0, gradle-profiler 0.26.0, shfmt 3.14.1, taplo 0.10.0, renovate 44.239.1, jq 1.8.2, gh 2.104.0 (the last four newest at least seven days old at pinning time).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Committed the `.mise/locks` sidecar lockfiles**
- **Found during:** Task 2 (`mise lock`)
- **Issue:** `mise.lock` references `.mise/locks/npm-renovate/...` and `.mise/locks/pipx-reuse/...` (aube and uv lockfiles) for the npm and pipx backends. Without them the lock is incomplete and `locked = true` cannot verify those tools on a fresh clone. They were not in the plan's `files_modified`.
- **Fix:** staged the four sidecar files with `mise.lock`.
- **Files modified:** `.mise/locks/**` (four files)
- **Verification:** `mise install` with `locked = true` succeeded; `git status` clean afterwards.
- **Committed in:** `0b27db7`

**2. [Rule 3 - Blocking] Trust-policy exception for `@yarnpkg/libzip@3.2.2`**
- **Found during:** Task 2 (`mise install`)
- **Issue:** see Decisions Made; installation of `npm:renovate` was blocked by mise's npm trust policy.
- **Fix:** exact-version `trust_policy_excludes` entry approved by the author (human decision, not an executor-initiated bypass).
- **Files modified:** `mise.toml`
- **Verification:** `mise install` completes, `renovate-config-validator --help` runs.
- **Committed in:** `0b27db7`

**3. [Rule 2 - Missing Critical] Added two notes to `docs/tooling.md`**
- **Found during:** Task 3
- **Issue:** the plan's notes did not mention that the lock is enforced or that renovate carries a trust exception; a reader of the inventory would not find either.
- **Fix:** added two version-free notes (lock enforcement with the sidecar lockfiles; the renovate exception pointing to `mise.toml`).
- **Files modified:** `docs/tooling.md`
- **Verification:** no-version-number grep still passes.
- **Committed in:** `4e435f8`

---

**Total deviations:** 3 (2 blocking, 1 missing critical)
**Impact on plan:** All needed for a verifiable, complete lock and an honest inventory; no scope creep.

## Issues Encountered

- The task 2 commit message says "14 CLI tools"; the correct count is 13 CLI tools in `[tools]` (14 was counting the earlier plan wording). `mise.toml` and this SUMMARY are authoritative.
- The harness refuses compound shell commands that contain git-like tokens (for example the substring in `gitleaks`), so some verification one-liners were split; results are identical.

## Verification Results and Timing

| Command | Result | Wall clock |
| --- | --- | --- |
| `mise install` (cold, renovate resolve and fetch) | pass | 54.9 s (over 30 s, within the 180 s budget) |
| `mise lock --platform linux-x64,linux-arm64,macos-arm64,macos-x64` | pass, 60 targets checked | 183 s (one-off; not a per-task feedback command, over the 180 s budget only because it downloads artifacts for four platforms) |
| `mise install` with `locked = true` (warm) | pass | 7.8 s |
| Task 2 `<automated>` tool chain (reuse, zizmor, lefthook, editorconfig-checker, gradle-profiler, shfmt, taplo) | pass | under 1 s |
| Task 2 `<automated>` `mise ls --current node` shows `.nvmrc` | pass | under 1 s |
| Task 3 key-coverage one-liner (13 keys, 0 missing) | pass | under 1 s |
| Task 3 no-version-number grep | pass | under 1 s |
| `./gradlew check` (plan 01-02 gates) | BUILD SUCCESSFUL | 34 s (warm) |

Tracer feedback gate: the tracer `<verify>` is automated-only; it was re-run end to end after the commit and passed, so expansion to Task 3 proceeded.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. Note for follow-up: the lockfiles `mise.lock` and `.mise/locks/**` are generated files that plan 01-04 (editorconfig) and plan 01-06 (REUSE) must cover with exclusions or annotations.

## User Setup Required

None - no external service configuration required. Contributors need mise (`brew install mise`), activated in the shell or used through `mise x --`.

## Next Phase Readiness

- Plans 01-04, 01-05 and 01-06 can rely on `mise x --` providing shfmt, taplo, editorconfig-checker, reuse and Node.js.
- Plan 01-04 and 01-06 must account for `mise.lock` and `.mise/locks/**` in editorconfig and REUSE configuration.
- Plan 01-11 (Renovate) must keep the `npm:renovate` exception in mind and expect `min_version` and `mise.lock` updates; plan 01-12 should run mise with the lock enforced.
- The plan 01-10 inventory cross-check must tolerate the table-form `npm:renovate` entry (an inline table value, key still `"npm:renovate"`).

## Self-Check: PASSED

All created files exist on disk; commits `0b27db7` and `4e435f8` are ancestors of HEAD; all Task 2 and Task 3 acceptance criteria and plan-level verification commands were re-run and pass.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
