---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-24T19:35:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
  - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
  - build-logic/build.gradle.kts
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
  - scripts/doctor.sh
  - scripts/governance/ask-governed-merge.sh
  - scripts/governance/test-ask-governed-merge.sh
  - scripts/test-doctor.sh
findings:
  critical: 0
  warning: 1
  info: 2
  total: 3
  necessity: 0
status: issues_found
---

# Phase 2: Code Review Report (incremental, after quick task 261124-oyk)

**Reviewed:** 2026-11-24T19:35:00Z
**Depth:** standard
**Files Reviewed:** 10
**Status:** issues_found

## Summary

This is an incremental review of `git diff 2e36184..HEAD` for the ten files that quick task 261124-oyk changed. I read each diff and its immediate surroundings, and ran the affected checks.

All four previous findings are fixed correctly and completely:

- **WR-01 (doctor survives a failing `--version`): fixed.** `scripts/doctor.sh` is `set -euo pipefail`, so a failing `mise`, `claude` or `git` made the pipeline fail inside the command substitution and killed doctor silently. The `|| installed=''` guards at lines 105, 556 and 588 turn that into the existing "cannot read the version" report. The new git-stub case adds the `xcode-select --install` fix line (`report` accepts any number of fix lines). `scripts/test-doctor.sh` has regression cases for each of the three tools, and the git-stub case also asserts that the later checks and the summary still run. `bash scripts/test-doctor.sh` gives 167 passed, 0 failed, and shellcheck is clean on all four scripts.
- **WR-02 (`CoursesJsonModule` KDoc on ID map keys): fixed.** The KDoc now states the observed behaviour: the key is written as the raw UUID, read back from it, and a TypeID-form key is rejected. It also names both directions the TypeID form needs, `addKeySerializer` and `addKeyDeserializer`. Two new `JsonRoundTripTest` cases pin the behaviour. `:serialization-jackson:test` is green with 16 tests, including both new ones.
- **WR-03 (`checkImageReferences` accepts what Renovate reads): fixed.** The check now enforces the manager regex of `renovate.json5`: a bare key `image-[a-z0-9-]+`, exactly one space on each side of `=`, a double-quoted `name:tag@sha256:digest` with no colon in the name, and no registry port. I traced the regexes by hand and ran `ImageReferenceCheckFunctionalTest` (15 tests, 0 failures). The functional test extracts the live regex from `renovate.json5` and compares accepted and rejected samples against it, including the `localhost:5000` misread. Adding `../renovate.json5` to the `repoFiles` inputs in `build-logic/build.gradle.kts` keeps that test's task cacheable and correctly invalidated. The three real catalog entries (`image-axonserver`, `image-postgres`, `image-ryuk`) are in the accepted form.
- **IN-01 (merge hook names the real reason for a dynamic shell word): fixed.** The new message names the word and both possibilities, and the ADR-0015 paragraph matches what the code does. The three new hook cases and the literal-script control (`bash run.sh && git merge docs-only` passes silently) pass; `bash scripts/governance/test-ask-governed-merge.sh` gives 169 passed, 0 failed.

I found no regressions, no security issues and no correctness bugs in the changed lines. Three residual items remain, none blocking.

## Warnings

### WR-01: `check_gsd` still dies silently under `set -e` when the GSD manifest is not valid JSON

**File:** `scripts/doctor.sh:385`
**Issue:** This is the same failure class as the fixed WR-01, in a sibling check the quick task did not touch. `installed=$("$jq" -r '.version // empty' "$manifest")` is unguarded. A truncated or corrupted `.claude/gsd-file-manifest.json` makes `jq` exit non-zero (2 for a parse error), `set -e` ends the script, and the user gets a raw jq error with no `fail  gsd:` line, no fix hint and no summary. That is exactly the case where doctor should say "reinstall GSD". The `-f` test only proves the file exists, not that it parses. I traced this from the code and did not run it against a corrupt manifest.
**Fix:**
```bash
if ! installed=$("$jq" -r '.version // empty' "$manifest" 2>/dev/null); then
  report fail gsd ".claude/gsd-file-manifest.json cannot be parsed" "$install_fix"
  return
fi
```
Add a `test-doctor.sh` case that writes `{` into the manifest and asserts `^fail  gsd: ` and `^summary: `.

## Info

### IN-01: The TypeID-key test asserts only `JacksonException`, which any failure satisfies

**File:** `backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt:173-178`
**Issue:** `an ID map key in the TypeID form fails deserialisation` uses `shouldThrow<JacksonException>`, the root of the Jackson exception hierarchy. The test exists to pin "a TypeID-form key is rejected as a malformed UUID". It would also stay green if the failure came from something unrelated, such as a mapper misconfiguration or a changed target type. The sibling tests in this class already use `invalidFormat()` to pin the cause.
**Fix:** Assert the specific cause, for example `shouldThrow<JacksonException> { ... }.invalidFormat()` (the existing helper walks the cause chain to an `InvalidFormatException`) and check its `targetType` or `value`.

### IN-02: Renovate reads catalog lines that `checkImageReferences` never inspects

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt:93-100`
**Issue:** The KDoc claim "every entry the check accepts is read by the manager" holds, and the tests pin it. The converse is not covered: the manager regex in `renovate.json5` is unanchored (`\bimage-[a-z0-9-]+ = "..."`) and the file pattern is the whole catalog. It therefore also reads a commented-out `# image-old = "x:1@sha256:..."` line and an `image-*` entry in any table other than `[versions]`. The check anchors at the line start and only looks inside `[versions]`, so it ignores both. The practical effect is limited to Renovate opening a PR for a dead comment or a stray entry, and no current line is affected. The "single source of every image" guarantee is slightly weaker than the KDoc suggests.
**Fix:** Either note the gap in the KDoc ("Renovate also reads commented-out lines and other tables; keep neither"), or have `catalogProblems` also flag a commented-out line or a line outside `[versions]` that matches `image-[a-z0-9-]+ = "`.

---

_Reviewed: 2026-11-24T19:35:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
