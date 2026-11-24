---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-25T00:00:00Z
depth: standard
files_reviewed: 14
files_reviewed_list:
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/TestSuitesFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckHooksFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ClaudeSettingsFixtures.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/courses.test-suites.gradle.kts
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - docs/adr/0008-container-images-with-buildpacks.md
  - docs/tooling.md
  - docs/upstream/0012-docker-containerd-platform-export.md
  - scripts/doctor.sh
  - scripts/governance/ask-governed-edit.sh
  - scripts/governance/test-ask-governed-edit.sh
  - scripts/test-doctor.sh
findings:
  critical: 0
  warning: 1
  info: 3
  total: 4
  necessity: 0
status: issues_found
---

# Phase 2: Code Review Report

**Reviewed:** 2026-11-25
**Depth:** standard (incremental: `a4444d7..HEAD`, quick tasks 261124-uwn and 261124-wp3)
**Files Reviewed:** 14
**Status:** issues_found

## Summary

Reviewed the diff of both quick tasks and its interaction with the surrounding code. I also ran
`scripts/governance/test-ask-governed-edit.sh` (65 passed, 0 failed), `scripts/test-doctor.sh` (269 passed, 0 failed)
and `shellcheck` on the touched scripts (clean).

The code changes are sound:

- `ImageReferenceCheck` now orders findings by path, then numeric line, then message. The new functional test
  discriminates the old lexicographic order, because line 12 sorts before line 3 as text.
- `ask-governed-edit.sh` names the root in the candidate's own spelling in both loops. `${#root}` is a byte length
  under `LC_ALL=C`, so `${candidate:0:${#root}}` cannot cut a character.
- `tasks.withType<Test>().configureEach { jvmArgs(...) }` is lazy, covers `architectureTest` (its plugin applies
  `courses.test-suites`), and is pinned by a probe. The wp3 summary records the RED run, with the warning printed three
  times before the change.
- The `$$"..."` multi-dollar strings are behaviour-preserving.
- The `check_mcp` jq rewrite keeps every tab-separated field non-empty, so `read` cannot shift fields. A URL never leaves
  jq.

One warning remains: after removing the UP-0012 workaround, `doctor` can no longer see the one combination that the
record still documents as failing. No critical issues. Necessity: nothing found (`.claude/gsd-core/references/necessity-review.md` was read).

## Warnings

### WR-01: `doctor` reports `ok` for the containerd image store on an engine older than 29.9.0, the combination UP-0012 still documents as failing

**File:** `scripts/doctor.sh:242-243` (with `scripts/doctor.sh:209` and `docs/tooling.md:132-135`)
**Issue:** `check_image_store` now reports `ok` for any engine with the containerd store, and only mentions "needs Docker
29.9.0 or newer" in the message text. The docs and ADR-0008 state the residual: "an engine older than 29.9.0 with the
containerd store can still fail" to export the Paketo noble run image. That is exactly the setup where `doctor` can help,
and it stays silent. Before the change the same engine produced a `warn` with a fix. Now it produces `ok`, so a developer
on 29.5.x with Docker Desktop's default containerd store gets a green `doctor` and an opaque `no suitable export target
found` at the first image build. The server version is already read in `check_docker` (`version` is local to that
function, never stored), so the check is cheap.
**Fix:** Keep the server version in a global (for example `DOCKER_SERVER_VERSION=$version` next to `DOCKER_OK=true`),
then in the containerd branch compare it with 29.9.0 and warn below it:
```bash
if [[ $driver == overlayfs || $status == *io.containerd.snapshotter* ]]; then
  if [[ $(printf '%s\n29.9.0\n' "${DOCKER_SERVER_VERSION%%[-+]*}" | sort -V | sed -n 1p) != 29.9.0 ]]; then
    report warn image-store "the containerd image store is active on Docker $DOCKER_SERVER_VERSION, which cannot export the Paketo run image" \
      "upgrade the engine to 29.9.0 or newer, or switch to the classic image store (docs/tooling.md, UP-0012)"
  else
    report ok image-store "the containerd image store is active (driver ${driver:-unknown}, Docker $DOCKER_SERVER_VERSION)"
  fi
fi
```
Add a `test-doctor.sh` case with a fake engine of 29.5.2 and the containerd driver, and one with 29.10.0.

## Info

### IN-01: A server without a usable `command` is counted as resolved, so a broken stdio server is reported `ok`

**File:** `scripts/doctor.sh:503-509` (jq program of `check_mcp`)
**Issue:** `kind none` covers every server whose `command` is not a non-empty string. That includes the intended
HTTP/SSE servers, but also `{"x": {}}`, `{"command": ""}` and `{"command": 5}`. I confirmed that `{"mcpServers":{"x":{}}}`
yields `none`, so `doctor` prints "all 1 servers ... resolve ... or have no command to resolve (1)". The new text
mentions the case, but the check can no longer warn about a stdio server that has lost its command. A server with a
string `url` is the only legitimate case with no binary.
**Fix:** In the jq program, return `none` only when the server also has a `url` (`(.value.url | text) != null`), and
fall through to `["path", $name, "(missing command)"]` otherwise, so it lands in the unresolved list. Add a test for
`{"x": {}}`.

### IN-02: The UP-0012 removal evidence cannot tell a fixed engine from a broken one, and part of the record's own verification list was not run

**File:** `docs/upstream/0012-docker-containerd-platform-export.md:74-99`
**Issue:** The record's "How to verify removal" asks for `bootBuildImage` and the web image task on the containerd
store. The result section says they "were not run", and the removal criteria name the GitHub-hosted runner images, which
the decision defers because no CI job builds an image yet (CICD-02). The author took this decision and it is written
down. The control run also weakens the evidence: `docker:29.5.2-dind` did not fail either, so the experiment cannot
distinguish the fixed engine from the defective one. The status `workaround-removed` therefore rests on "no defect
observed on a fixed engine", not on a demonstrated fix. Nothing in the repository re-checks the combination when CICD-02
lands, and WR-01 removes the local signal as well.
**Fix:** Keep the residual visible: add a CICD-02 acceptance item ("image job runs on an engine of 29.9.0 or newer, or
on the classic store") to `.planning/ROADMAP.md` or the CICD-02 plan, and cite it from the record's decision paragraph.
If WR-01 is fixed, mention the `doctor` version check there as the local guard.

### IN-03: The numeric-order test covers only catalog findings, not compose findings

**File:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt:278-300`
**Issue:** `composeProblems` also switched to `Finding(path, index + 1, ...)`, but the new test
(`findings of one file are listed by numeric line number, not as text`) exercises only `gradle/libs.versions.toml`. A
regression in the compose ordering (for example, a compose file with an offending image on line 3 and another on line 12)
would pass.
**Fix:** Add a second assertion in the same test with a compose file that has bad `image:` entries on lines 3 and 12,
asserting the same index ordering.

---

_Reviewed: 2026-11-25_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
