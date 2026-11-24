---
phase: quick-261124-s7w
plan: 01
subsystem: tooling
tags: [doctor, bash, jackson, renovate, governance, build-logic]
requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: the incremental review 02-REVIEW.md (WR-01, IN-01, IN-02) after quick task 261124-oyk
provides:
  - doctor.sh reports every unreadable or malformed input file instead of dying under set -e
  - a TypeID map-key test that pins value, target type and message
  - checkImageReferences covers every catalog line the Renovate image manager reads
affects: [scripts/doctor.sh, serialization-jackson tests, build-logic governance]
tech-stack:
  added: []
  patterns:
    - "guarded read: `if ! x=$(reader 2>/dev/null); then report ... fix; return; fi`, with a jq shape check where later reads depend on the shape"
    - "superset regex (MANAGER_READ) plus a parity test against the live renovate.json5 regex"
key-files:
  created: []
  modified:
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
key-decisions:
  - "One guarded manifest read that also checks the shape (object with an object of files) covers the four later `.files` reads"
  - "A .mcp.json problem is a warn (the severity of every other mcp outcome); an unreadable pin, tool list or mise.toml is a fail"
  - "IN-02 uses one rule: every MANAGER_READ match on any catalog line except the image-* entry that starts a [versions] line is reported"
requirements-completed: [TOOL-03, AIDEV-07, AIDEV-04, INFRA-06, INFRA-09, CICD-04]
duration: 55min
completed: 2026-11-24
status: complete
actuals:
  tokens: 21000
  tasks: 3
  commits: 3
plan_head_before: d8fea3225447f3b3e69d75d2e8e04bd0acb18e36
plan_head_after: b6ea808be95786468688b5ddc4c05a42bc5491a6
---

# Phase quick-261124-s7w Plan 01: Fix Phase 2 review findings WR-01, IN-01, IN-02 Summary

**doctor.sh guards every file and tool-output read with a report and a fix, the TypeID map-key test pins the real Jackson cause, and checkImageReferences reports every catalog line the Renovate image manager reads outside the verified entries.**

## Commits

| Finding | Commit | Message |
| ------- | ------ | ------- |
| WR-01 (with siblings) | 0bafd34560c46b66aa583eb4482f9f50b805217d | fix(quick-261124-s7w): doctor reports a broken GSD manifest and every sibling read instead of dying (WR-01) |
| IN-01 | 7ba06d2f27842356e6dac7083b6511f14271382f | test(quick-261124-s7w): pin the cause of the TypeID map-key failure (IN-01) |
| IN-02 | b6ea808be95786468688b5ddc4c05a42bc5491a6 | fix(quick-261124-s7w): checkImageReferences reports every catalog line Renovate reads outside the verified entries (IN-02) |

## WR-01: reproduction and fix

Reproduced before the fix against the `scripts/test-doctor.sh` fixtures (all RED in the first run):

- Manifest `{`: exit 5 with only a raw `jq: parse error`. `[]`: exit 5 (`Cannot index array`). `{"version":"1.22.0"}`:
  exit 5 at the `.files | to_entries` read (`null (null) has no keys`). `{"version":"1.22.0","files":[]}`: no
  crash, but every GSD file reported as `unexpected`.
- Siblings: a `.mcp.json` of `{` or `{"mcpServers":{"x":1}}` ended doctor with exit 5, and `{"mcpServers":[]}` reported
  `ok` for zero servers; a tool list that is not JSON or `{"jq":{"version":"1"}}` ended with exit 5; a missing
  `mise.toml` ended with exit 1 and no output at all; an unreadable pin, `colima.yaml` or `.env` died on the reader.

Fix, in `scripts/doctor.sh` (the new report lines):

| Site | Report | Fix line |
| ---- | ------ | -------- |
| `check_gsd` manifest read (shape: top level object, `.files` object) | `fail gsd: .claude/gsd-file-manifest.json cannot be read as a GSD manifest ...` | the existing `npx -y @opengsd/gsd-core@<pin> --claude --local` |
| `check_gsd` pin read | `fail gsd: governance/gsd-version cannot be read` | `chmod u+r governance/gsd-version` |
| `check_mcp` read (shape: object, `mcpServers` an object of objects) | `warn mcp: .mcp.json cannot be read as MCP server configuration ...` | `git restore .mcp.json` |
| `check_tools` reads (shape: object of arrays of objects) | `fail tools: cannot read the tool list that 'mise ls --current --json' printed ...` | `mise install`, then `brew upgrade mise, or: mise self-update` |
| `check_mise` min_version read | `fail mise: mise.toml cannot be read from the repository root ...` | `git restore mise.toml` |
| `check_colima` read (folded into the `-f` branch) | `warn colima: cannot read the colima configuration ...` | `colima start --edit` |
| `check_platform_token` read | `warn platform-token: .env exists but cannot be read ...` | `chmod u+r .env` |

Readers run with `2>/dev/null`, so no raw parser output reaches the report, and the `.env` branch never prints a value.

### Classification of every other command substitution in doctor.sh

Not changed, with the reason:

- Script bootstrap (lines 31, 33): cannot fail on a readable script path.
- `printf | sort -V | sed` (check_mise, min_version comparison; the two version comparisons of claude and git): a
  pipeline over variables that cannot fail.
- Already in `if !`: `find_jq` callers, the `mise ls` read, the docker reads, `check_gsd_defaults` jq read, the sha tool
  lookup and similar.
- Already guarded with `|| installed=''` by 261124-oyk: `mise --version`, `claude --version`, `git --version`.
- Guarded with `|| true` or `docker_field`: the grep and sed pipelines of the testcontainers keys, the `actual` and
  `unexpected` pipelines of `check_gsd`, the docker field reads.
- Text pipelines over variables (the problem count, the problem lines): cannot fail.
- `.files | keys[]`, `.files | to_entries[]`, `.files | length` of `check_gsd`: safe once the manifest shape guard passed.
- `git ls-files` after the `rev-parse` guard; the substitution inside `[[ ]]` in an `||` list (errexit does not
  apply); the substitution in the arguments of `report` (its status is ignored); the substitution in an `if`.

## IN-01: the observed exception

Confirmed by running the test, no deviation from the prediction. The TypeID-form key fails in the key
deserializer of the underlying `UUID`: `InvalidFormatException` with value `course_...` (the TypeID text of the key),
target type `java.util.UUID`, message `Cannot deserialize Map key of type java.util.UUID from String ...`. The test
asserts `value`, `targetType shouldBe UUID::class.java` and `message shouldContain "Map key"` through `invalidFormat()`.
`JsonRoundTripTest` still has 16 tests, 0 failures.

## IN-02: the rule and the extra shapes

`MANAGER_READ` (`\bimage-[a-z0-9-]+ = "[^"\s]*@sha256:`) is a superset of the manager's match on one line. `catalogProblems`
now walks every line with its number and reports every `MANAGER_READ` match except the `image-*` entry that starts a
line of `[versions]` (which `catalogProblem` verifies). The finding reads
`path:line: the Renovate image manager reads <key> here, outside the [versions] entries this check verifies; ...`.
Beyond the two shapes the review names (a commented-out line, another table) it covers a `[versions]` alias such as
`old-image-x` (valid for Gradle, read by Renovate as `image-x`) and an entry in a trailing comment, also after an
accepted entry. Requiring the digest keeps `image-lib = "org.example:lib:1.0"` in `[libraries]` silent, and the real
catalog stays silent (three verified entries). The parity test compiles the live `renovate.json5` regex: it reads every
`READ_ELSEWHERE` sample under the key the finding names and reads none of the silent controls. `renovate.json5` and
`gradle/libs.versions.toml` are unchanged. `ImageReferenceCheckFunctionalTest` has 17 tests (15 before), 0 failures.

## Deviations from Plan

None - plan executed exactly as written. Small adjustments inside the plan's discretion:

- Line wrapping of two KDoc paragraphs was adjusted by hand to match the ktfmt output of `spotlessCheck` (never
  `spotlessApply` on the governed file); one KDoc sentence was reworded so that ktfmt does not split a code span.
- The `doctor.sh` `min_version` read was wrapped after the pipe to keep the 120-column style of the neighbouring code.

## Verification

- `bash scripts/test-doctor.sh`: 246 passed, 0 failed (167 before); no skips on this machine (not root).
- `shellcheck` and `shfmt -d` clean on `scripts/doctor.sh` and `scripts/test-doctor.sh`.
- Real doctor in the worktree: no fail line beyond the Step 0 baseline (the missing GSD manifest of a worktree), and
  the `summary:` line is still printed.
- `:serialization-jackson:check`, `ImageReferenceCheckFunctionalTest` (17 tests), `:build-logic:detekt`,
  `checkImageReferences` and `spotlessCheck` green.
- `mise x -- ./gradlew :build-logic:check :serialization-jackson:check checkImageReferences spotlessCheck`:
  BUILD SUCCESSFUL in 7m 14s (the whole functionalTest suite included).

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or schema; the new reads only parse files already read by doctor, and the
`.env` branch prints no value (T-s7w-02).

## For the orchestrator

- Record WR-01, IN-01 and IN-02 as fixed in `02-REVIEW-DISPOSITION.md` with the commit hashes above.
- Governed files the cleanup merge prompt will list: `scripts/doctor.sh` and
  `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt`.

## Self-Check: PASSED

Created files and commits verified present on the branch (see the commits table; `git rev-list --count` over the
plan base gave 3 before this summary).
