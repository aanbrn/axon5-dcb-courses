---
phase: quick-261124-uwn
plan: 01
subsystem: governance-hooks, build-logic, doctor, upstream-records
tags: [ci-lint, qodana, doctor, upstream, governance]
requires: []
provides:
  - platform-independent reason text of the governance edit hook, with symbolic-link-root tests
  - Qodana-clean build-logic and file-ordered image reference findings (IN-02)
  - doctor that reads url-only MCP servers (WR-01) and tells a missing from an unreadable mise.toml (IN-01)
  - UP-0012 workaround removed, record in status workaround-removed
affects: [scripts/governance, scripts/doctor.sh, build-logic governance sources, docs/upstream, docs/tooling.md, ADR-0008]
tech-stack:
  added: []
  patterns:
    - "doctor parses tab-separated jq output only with non-empty fields (kind, name, binary)"
key-files:
  created: []
  modified:
    - scripts/governance/ask-governed-edit.sh
    - scripts/governance/test-ask-governed-edit.sh
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckHooksFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ClaudeSettingsFixtures.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - docs/upstream/0012-docker-containerd-platform-export.md
    - docs/tooling.md
    - docs/adr/0008-container-images-with-buildpacks.md
decisions:
  - "The edit hook names the root as the candidate path spells it; the ask decision is unchanged"
  - "UP-0012 workaround removed after an observed export on a Docker 29.10.0 containerd-store engine"
metrics:
  tasks: 3
  commits: 7
  completed: 2026-11-24
status: complete
plan_head_before: de9305bdb662ecc3342a75d9941bc6912bb34287
plan_head_after: e2ec662a2a390737046ce129b1aabf6ca1173410
actuals:
  tokens: 60000
  tasks: 3
  commits: 7
---

# Phase quick-261124-uwn Plan 01: PR 21 CI failures and the open Phase 2 review findings Summary

Edit-hook reasons that no longer depend on the file system's symbolic links, a Qodana-clean build-logic with
file-ordered image findings, doctor fixes for url-only MCP servers and an unreadable `mise.toml`, and the UP-0012
workaround removed on the strength of an observed export on a fixed engine.

## Commits

| # | Hash | Message | Finding |
|---|------|---------|---------|
| 1 | a520c9e | fix(governance): name the root as written in the edit hook's ask reason | CI Lint failure, run 45419305916 |
| 2 | 671648c | style(build-logic): fix the five Qodana findings of the PR 21 run | Qodana x5 |
| 3 | ab720cc | fix(build-logic): list image reference findings by path and line number (IN-02) | IN-02 |
| 4 | 53dac4e | fix(doctor): resolve url-only MCP servers without misreading empty fields (WR-01) | WR-01 |
| 5 | 0e67cf3 | fix(doctor): tell a missing mise.toml from an unreadable one (IN-01) | IN-01 |
| 6 | 5d0979f | chore(upstream): remove the UP-0012 image-store workaround | UP-0012 |
| 7 | e2ec662 | style(build-logic): drop the explicit type argument of compareBy in ImageReferenceCheck | Qodana finding of commit 3 |

For the disposition rows: WR-01 = 53dac4e, IN-01 = 0e67cf3, IN-02 = ab720cc (plus e2ec662 for the Qodana cleanup of
the same code).

## CI Lint failure: reproduction and root cause

With the test's temp root resolved physically (`cd -P`/`pwd -P`, the path shape of the Linux runner), the unchanged hook
reproduced the CI line on macOS:

```text
FAIL - a different case spelling of an agent worktree directory asks (exit 0, expected exit 0 and ask; reason pattern: mise\.toml in .*AGENT-A is a governed path)
  | ... mise.toml in /private/var/folders/.../repo/.claude/worktrees/agent-a is a governed path ...
61 passed, 1 failed
```

Root cause: not the case sensitivity of the file system. macOS's `/var/folders` is a symbolic link to `/private/var`,
while `git worktree list` reports resolved paths, so on macOS the lexical candidate `.../AGENT-A/mise.toml` matched no
worktree root and reached the `.claude/worktrees/<name>` loop, which names the root as written (`AGENT-A`). On Linux
`/tmp` has no link, so the candidate first matched the git-reported root `.../agent-a` through the case-folded prefix
test. The hook asked on both systems; only the reason text differed. `check_candidate` now passes
`${candidate:0:${#root}}` (the root as the candidate spells it) to `match_relative` at both call sites. The prefix
tests and exemptions are untouched, so fail-closed behaviour is unchanged.

Test counts: `test-ask-governed-edit.sh` 62 -> 65 passed, 0 failed (three new cases through a symbolic link to the
repository as `CLAUDE_PROJECT_DIR`: the `AGENT-A` spelling asks, a governed file in the main checkout asks, a governed
file inside agent worktree agent-a stays silent). The temp root is now resolved physically, so macOS and Linux test the
same path shape. A Linux container cross-check was not run (no network for `apk add` in the sandbox at planning time);
the CI Lint job after the push is the Linux confirmation.

## Qodana

Run 1 on commit 5d0979f reported one problem: `RemoveExplicitTypeArguments` on `compareBy<Finding>` introduced by the
IN-02 change (the five original sites were fixed). Fixed in e2ec662. Run 2 of `scripts/qodana.sh` on committed HEAD
e2ec662: `Analysis results: 0 problem detected`, no baseline, no suppression, `qodana.yaml` unchanged. Docs-only SUMMARY
commit follows, so the analysed code is the final code.

## Doctor

- WR-01: `check_mcp` emits `kind<TAB>name<TAB>binary` with every field non-empty (`-` when there is nothing to
  resolve); a server without `command` is counted but not resolved and its url never leaves jq; `mise x --` without a
  binary argument resolves `mise`. New tests assert the `ok  mcp:` line, no warn, no `secret-value` in the output, a
  url-only server before an unresolved one, and that an unresolved stdio binary keeps its real name.
- IN-01: missing `mise.toml` keeps `git restore mise.toml`; unreadable gives `chmod u+r mise.toml` (own tests; the
  unreadable case skips as root).
- `test-doctor.sh`: 246 -> 269 passed, 0 failed. Real doctor in the worktree (`DOCTOR_SKIP_GRADLE=1`):
  `12 ok, 3 warn, 1 fail`; the single fail is `gsd:` (the worktree has no GSD installation), `ok  upstream-removal:`
  is printed with no UP-0012 warning, `ok  image-store:` (classic) and `ok  colima: network.dns lists 2 servers`.

All seven shell suites pass: apply-github-settings 107, doctor 269, edit hook 65, merge hook 169, check-pr 173,
list-pr-files 25, changed-dependencies 62. `mise x -- ./gradlew build` succeeded (before the one-line compareBy
change of e2ec662; that change was verified with `spotlessCheck` and the ImageReferenceCheck functional tests, and
Qodana).

## UP-0012 verification evidence

Local engine (`docker version`): client 29.10.0, server 29.10.0, context colima, classic store (`overlay2`).

Throwaway privileged containers, no volume or host mount, run with `--rm`, stopped afterwards and both dind images
removed (the author's own k8s containers were not touched; the colima configuration was not changed):

| Engine | Driver / store | `docker pull --platform linux/arm64 paketobuildpacks/ubuntu-noble-run:latest` | `docker save --platform linux/arm64 ... -o /tmp/run.tar` | `tar tf` entries |
|--------|----------------|------|------|------|
| `docker:29.10.0-dind` (server 29.10.0, arm64) | `overlayfs`, driver status `io.containerd.snapshotter.v1` | ok | exit 0 | 11 |
| `docker:29.5.2-dind` (control, server 29.5.2, arm64) | `overlayfs` | ok | no error printed (exit code not captured; archive written) | 11 |

The control did not reproduce the defect (probably because a fresh data root has no layers another image already
unpacked; recorded as not checked). The project's image build (`bootBuildImage`, web image) was not run on the
containerd store, as the plan did not require it and no CI job builds an image yet. The two UP-0012 markers are gone
from `scripts/doctor.sh`, the record is `workaround-removed` with the result paragraph and the residual (an engine older
than 29.9.0 with the containerd store), `docs/tooling.md` and ADR-0008 state the 29.9.0 requirement, and
`checkUpstreamRecords` passes. `docs/quick-start.md` never mentions the image store, so it needed no change.
`docs/adr/0011-local-runtime-and-doctor.md` was left alone as instructed.

## Governed files for the cleanup merge prompt

- `scripts/governance/ask-governed-edit.sh`, `scripts/governance/test-ask-governed-edit.sh`
- `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt`
- `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt`
- `scripts/doctor.sh`

All were changed with the Edit tool only. Untouched as required: `qodana.yaml`, `governance/governed-paths.txt`,
`renovate.json5`, `gradle/libs.versions.toml`, `.github`, `.claude`, STATE.md, the plan and 02-REVIEW-DISPOSITION.md.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Qodana finding introduced by the IN-02 change**
- **Found during:** final `scripts/qodana.sh` run
- **Issue:** `compareBy<Finding>(...)` carried an explicit type argument that Qodana reports (`RemoveExplicitTypeArguments`)
- **Fix:** removed the type argument; the comparator type is inferred
- **Files modified:** `ImageReferenceCheck.kt`
- **Commit:** e2ec662

**2. Doctor messages kept the existing detail wording where tests match it.** Two additions beyond the plan text:
the `check_mise` split checks `[[ ! -e ]]` first, and the `ok  mcp:` line mentions how many servers have no command to
resolve when there are any. Neither changes an asserted prefix.

**3. ADR-0008 wording.** Besides the three places the plan named, the sentence introducing the two local defects and the
"two local engine defects" consequence were updated, so no stale claim remains.

### Plan items not done

- The optional Linux container cross-check of the edit-hook suite was not run (not a gate).
- The ledger file for `commits:` could not be written (the sandbox refuses compound git commands), so
  `plan_head_before` is the dispatch base de9305b; `commits: 7` is `git log de9305b..HEAD` before this SUMMARY commit.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or trust-boundary file access was added.

## Self-Check: PASSED

- All seven commit hashes above exist on the branch (`git log de9305b..HEAD` lists them).
- The twelve modified files exist; the UP-0012 record has `status: "workaround-removed"`; no `UPSTREAM: UP-0012` marker
  remains outside `docs/upstream`, `.planning` and `.claude`.
