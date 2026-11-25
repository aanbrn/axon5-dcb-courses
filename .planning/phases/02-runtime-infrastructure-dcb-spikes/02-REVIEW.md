---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-25T00:00:00Z
depth: standard
files_reviewed: 21
files_reviewed_list:
  - .claude/settings.json
  - .github/workflows/ci.yml
  - build-logic/build.gradle.kts
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/CiMavenMirrorFunctionalTest.kt
  - docs/adr/0004-gsd-configuration.md
  - docs/adr/0013-upstream-records-and-workaround-markers.md
  - docs/adr/0016-ci-dependency-downloads.md
  - docs/quick-start.md
  - docs/templates/upstream-issue.md
  - docs/tooling.md
  - docs/upstream/0006-empty-platform-token.md
  - docs/upstream/0010-codeql-kotlin-extractor.md
  - docs/upstream/0011-colima-aaaa-dns.md
  - docs/upstream/0013-axon-server-dcb-context-flag.md
  - governance/governed-paths.txt
  - renovate.json5
  - scripts/ci/maven-central-mirror.init.gradle.kts
  - scripts/ci/retry-on-rate-limit.sh
  - scripts/ci/test-retry-on-rate-limit.sh
  - scripts/doctor.sh
  - scripts/test-doctor.sh
findings:
  critical: 0
  warning: 1
  info: 4
  total: 5
  necessity: 0
status: issues_found
---

# Phase 02: Code Review Report (incremental, gap-closure plans 02-24..02-31 and quick task 261125-k0a)

**Reviewed:** 2026-11-25
**Depth:** standard
**Files Reviewed:** 21
**Status:** issues_found

## Summary

Reviewed the diff `7790881..HEAD` of the 21 scoped files, with whole files read for context. The substance is sound:

- `retry-on-rate-limit.sh` is correct on the paths I traced: the `PIPESTATUS[0]` capture under `pipefail`, the exactly-one-retry bound, argument pass-through, the fixed announcement text, and the second run's status being final. `scripts/ci/test-retry-on-rate-limit.sh` passes (48/48).
- The mirror init script is proven by `CiMavenMirrorFunctionalTest`. I ran it locally under `--configuration-cache --isolated-projects`: 4 tests, build successful.
- `check_claude_plugins` in `doctor.sh` is robust. Plugin ids are validated against a strict regex before they are echoed or put into a copyable command. `.version == 2` and the `scope`/`projectPath` field names match the real `~/.claude/plugins/installed_plugins.json` on this machine. `scripts/test-doctor.sh` passes (365/365), and `scripts/doctor.sh` reports `ok claude-plugins` here.
- `shellcheck` is clean on all four scripts, and `checkClaudeAskRules` passes with the new `scripts/ci/**` governed path and ask rule.
- The documented residuals (the TestKit user home has no `init.d`, qodana is untouched, the mirror is an unverified source until CICD-05) are stated honestly in ADR-0016. As agreed, they are not reported as defects.

No critical issues. One warning (the wrapper changes the locale of the command it wraps) and four info items.

Not run locally: `spotlessCheck`, `checkToolingInventory` and `checkBuildScripts` (the sandbox has Node 24 while `.nvmrc` requires 26, so `verifyNodeVersion` blocks the build), and `shfmt`/`editorconfig-checker` (not installed).

## Warnings

### WR-01: `retry-on-rate-limit.sh` exports `LC_ALL=C` and thereby changes the locale of the wrapped Gradle/doctor command

**File:** `scripts/ci/retry-on-rate-limit.sh:36`
**Issue:** `export LC_ALL=C` is meant to make the `grep` byte-wise ("regardless of the caller's locale"), but the export is inherited by the wrapped command in both runs. I confirmed this: `LC_ALL=en_US.UTF-8 scripts/ci/retry-on-rate-limit.sh bash -c 'echo LC_ALL=$LC_ALL'` prints `LC_ALL=C`. Every wrapped step (`./gradlew build`, `integrationTest`, `functionalTest`, the compose-smoke test run and `doctor.sh --ci`) therefore runs under the C/POSIX locale, whereas the same command run locally, or in the other CI jobs that are not wrapped (`lint`, `qodana`), runs under the runner's UTF-8 locale. On the JVM this changes the console and JNU encodings (`sun.jnu.encoding`, `stdout.encoding`) and the default `Locale`. Non-ASCII characters in task, test or compiler output turn into `?`, and tests that depend on the default locale or on non-ASCII path names behave differently from a local run. It is a silent difference between local and CI that the ADR never mentions, and it is unrelated to what the wrapper exists to do. The grep is the only thing that needs the C locale.
**Fix:** Scope the locale to the one command that needs it and drop the export:
```bash
# remove: export LC_ALL=C
if ! LC_ALL=C grep -aqF -- "$RATE_LIMIT_MESSAGE" "$log"; then
  exit "$status"
fi
```
Add a case to `test-retry-on-rate-limit.sh` that runs a fake command printing `$LC_ALL` and asserts it equals the caller's value (or is unset).

## Info

### IN-01: UP-0011 now says the AAAA stall is "not established", but the record title and four other places still state it as fact

**File:** `docs/upstream/0011-colima-aaaa-dns.md:12` (also `docs/adr/0008-container-images-with-buildpacks.md:87`, `docs/tooling.md:134`, `scripts/doctor.sh:299` and `:301`)
**Issue:** The 2026-11-25 clean-profile run did not reproduce the stall. The record's Root cause section was correctly rewritten to "Not established", and the removal criterion was changed accordingly. However, the H1 still reads "The default colima DNS forwarder never answers AAAA queries and stalls Go tools". The `# UPSTREAM: UP-0011` marker in `doctor.sh`, ADR-0008 ("never answers AAAA queries") and `docs/tooling.md` also still state the cause as an established fact. The workaround and the `colima` doctor check are deliberately kept (the record says so), but the prose now contradicts the record it cites.
**Fix:** Reword the H1 to what was observed once, for example "A default colima profile once stalled Go DNS lookups for about 10 s per lookup (not reproduced on a clean profile)". Soften the sentences in ADR-0008, `docs/tooling.md` and the doctor marker/comment to "can stall (observed once, UP-0011)".

### IN-02: Retry announcement and header name Maven Central, but the match is host-agnostic

**File:** `scripts/ci/retry-on-rate-limit.sh:40` and `:70`
**Issue:** The trigger is the text `Received status code 429 from server`, which Gradle prints for any host (Plugin Portal, services.gradle.org, the mirror). ADR-0016 says the retry covers "a 429 from whichever host answered", but the announcement and the `::warning title=Maven Central rate limit::` annotation always say "Maven Central answered HTTP 429". A rate limit from another host would be mis-attributed in the job summary.
**Fix:** Use a neutral fixed text, for example `a download answered HTTP 429 (Too Many Requests)`, and title the annotation `Dependency download rate limit`. Update the two assertions in `test-retry-on-rate-limit.sh` that match `$RETRY_MARKER` and `$WARNING_PREFIX`.

### IN-03: `codeql.yml:32` still says the extractor "does not cover Kotlin 2.4.21" (known stale marker, outside this scope)

**File:** `.github/workflows/codeql.yml:32`
**Issue:** The UP-0010 record (title, Analysis, and the draft) was corrected on 2026-11-25: the documented Kotlin range `1.8.0 to 2.4.2x` includes 2.4.21, and the Isolated Projects interaction is now the likelier cause. The `# UPSTREAM: UP-0010` marker text in the workflow still carries the old claim. This is the already-known item and is noted for completeness only.
**Fix:** Change it to "the CodeQL Kotlin extractor found no sources in this Kotlin 2.4.21 Gradle build (Isolated Projects interaction suspected)".

### IN-04: Duplicated CI step (5 copies) and a hand-copied preset list in `renovate.json5`

**File:** `.github/workflows/ci.yml:79-83`, `:133-137`, `:198-202`, `:244-248`, `:391-395`; `renovate.json5:17-27`
**Issue:** (a) The "Use the Maven Central mirror (CI only)" step is copied verbatim into five jobs. A future change (path, mode, a different `GRADLE_USER_HOME` handling) has to be made in five places, and one drifting copy would silently leave a job without the mirror. ADR-0016 relies on the step being "identical in every job". (b) The new `ignorePaths` list re-states the `:ignoreModulesAndTests` preset by hand, because an array replaces the preset's array. The reasoning in the comment is correct, but the list can drift from the preset when `config:best-practices` changes, and nothing detects it.
**Fix:** (a) Move the step into a local composite action (`.github/actions/use-maven-central-mirror`) and call it from each job. Keep it SHA-pinned or local, and keep `scripts/ci/**` governed. (b) Accept the copy but add a note to revisit it on a Renovate major bump, or express the exclusion as a `packageRules` entry with `matchFileNames: ["scripts/ci/**"]` plus `enabled: false` if that proves sufficient to keep the mirror URL out of the Gradle registry set (verify with a Renovate dry run before relying on it).

---

_Reviewed: 2026-11-25_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
