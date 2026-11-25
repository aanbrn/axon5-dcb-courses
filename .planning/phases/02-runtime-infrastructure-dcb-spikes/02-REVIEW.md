---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-25T00:00:00Z
depth: standard
files_reviewed: 9
files_reviewed_list:
  - .github/workflows/ci.yml
  - .gitignore
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - docs/adr/0008-container-images-with-buildpacks.md
  - docs/adr/0010-test-levels-as-jvm-test-suites.md
  - docs/tooling.md
  - docs/upstream/0012-docker-containerd-platform-export.md
  - scripts/doctor.sh
  - scripts/test-doctor.sh
findings:
  critical: 0
  warning: 0
  info: 3
  total: 3
  necessity: 0
status: issues_found
---

# Phase 2: Code Review Report (incremental, after quick tasks 261125-a09 and 261125-bha)

**Reviewed:** 2026-11-25T00:00:00Z
**Depth:** standard
**Files Reviewed:** 9
**Status:** issues_found

## Summary

Scope: the diff `6d8bc0a..HEAD` of nine files: the doctor `image-store` version gate (WR-01 fix of the previous review), the
`.mcp.json` server with neither a command nor a url (IN-01 fix), the compose line-order functional test (IN-03 fix), the new
parallel `functional-test` CI job with its `ci-success` wiring and documentation, and the `/.build/` ignore entry.

Verification performed, not only reading:

- `./gradlew build checkDependencyLicences -x integrationTest -x :build-logic:functionalTest --dry-run` plans no
  `:build-logic:functionalTest` (nor its compile or classes tasks), while root `check` still reaches the rest of
  `:build-logic:check`. So the exclusion of an included-build task works, and the `build` and `functional-test` jobs
  partition `check` without a gap or an overlap.
- `./gradlew :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*numeric*'` passes. The new compose test
  is discriminating (lines 3 and 12 sort the wrong way as text) and the compose finding format it asserts matches
  `ImageReferenceCheck` (sorted by path, then numeric line).
- `scripts/test-doctor.sh` passes, 318 of 318.
- The numeric version comparison (`sort -V` over `core` after stripping a `-`/`+` suffix, strict `N.N.N` regex guard) is
  correct for `29.9.0`, `29.10.0`, `29.8.2`, `28.5.2`, suffixed versions and an uncomparable `dev`. A warn never fails the
  doctor (exit 0), so the `doctor` CI job cannot turn red because of an engine on the containerd store.
- The jq change classifies `{}`, `command: ""`, `command: 5` and `url: ""` as `missing` and keeps a url out of the output
  (test asserts `secret-value` never appears). The `fail` plus `fix:` lines match the tests.
- The CI claims in ADR-0010 (about 9 of 13.5 minutes, run 45507745588) match the real run's data cited in the quick task
  plan. The three docs that describe the job split (ADR-0010, `docs/tooling.md`, `ci.yml` comments) agree with one another.
- The new `functional-test` job copies the `build` job's hardening: SHA-pinned actions, `persist-credentials: false`,
  job-level `contents: read`, `timeout-minutes: 30`, cache read-only off main, and an artifact upload on failure.

No bug, security issue or correctness defect was found. Three minor maintainability items follow.

## Info

### IN-01: Review finding IDs are cited from durable code and docs, and the IDs are already ambiguous

**File:** `scripts/test-doctor.sh:495,524,653` and `docs/upstream/0012-docker-containerd-platform-export.md:66`
**Issue:** Test comments and an upstream record say "WR-01 of the Phase 2 review", "IN-01 of the Phase 2 review" and "review
WR-01". `02-REVIEW.md` is overwritten by every incremental review, and every incremental review restarts at CR-/WR-/IN-01. The
file already shows the collision: `scripts/test-doctor.sh` uses "WR-01" for at least three different findings (lines 321/445/563
"the review's WR-01 class", line 495 and line 653 "WR-01 of the Phase 2 review"), and this review reuses IN-01 for a fourth
subject. A reader cannot resolve any of these IDs from the repository, so the comments explain nothing that the adjacent text
does not.
**Fix:** Drop the ID, or cite something stable: the quick task directory (`261125-a09`) or the commit hash. For example, in
`scripts/test-doctor.sh:653`: `# ... the check warns (quick task 261125-a09). A '-' or '+' suffix ...`.

### IN-02: A pre-release of the minimum version passes the engine gate

**File:** `scripts/doctor.sh:302-312`
**Issue:** `core=${version%%[-+]*}` strips the suffix before comparing, so `29.9.0-rc.1` yields `29.9.0` and reports `ok ...
meets its minimum`. A release candidate precedes the release that carries the UP-0012 fix and is not guaranteed to contain
it. The comment documents the stripping, but not that this direction is the unsafe one (for `+azure-1`-style build metadata it
is right, for a `-rc` pre-release it is not). Impact is low: the result is only a missed warning on a pre-release engine.
**Fix:** Treat a `-` suffix as a pre-release: when `$version` contains `-` after the core, compare as older than the minimum
when `core` equals the minimum, or simply report the "cannot be compared" warning for any `-` suffix and strip only `+`
build metadata:
```bash
core=${version%%+*}
# `29.9.0-rc.1` no longer matches ^N.N.N$, so it takes the "cannot be compared" warning branch.
```
Add a `29.9.0-rc.1` case to `scripts/test-doctor.sh` next to the suffixed-version case.

### IN-03: The `Read the mise version` and tool-setup steps are now copied into a third job

**File:** `.github/workflows/ci.yml:129-170`
**Issue:** The checkout, `setup-java`, `setup-node`, `Read the mise version`, `mise-action` and `setup-gradle` block now exists
in `build`, `functional-test` and (partly) `doctor`, `lint` and the other jobs; the `sed` that reads `min_version` is duplicated
verbatim, so a change to `mise.toml` parsing or to an action SHA must be made in several places (Renovate keeps the SHAs in step,
but the `sed` and the `install_args` are manual). The comment of the new job also repeats what the `build` job says about mise.
**Fix:** Optional: extract the shared setup into a local composite action (`.github/actions/setup-build`) taking the mise
`install_args` as an input. Not required for this change.

---

_Reviewed: 2026-11-25T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
