---
phase: 01-repository-build-ci-foundation
reviewed: 2026-11-22T00:00:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPath.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt
  - docs/adr/0004-gsd-configuration.md
findings:
  critical: 0
  warning: 3
  info: 10
  total: 13
  necessity: 1
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-11-22
**Depth:** standard
**Files Reviewed:** 6 (incremental re-review after plan 01-21 (gap G-01-2) and the quick task that added ADR-0004; scope is the diff since commit 2582186, the rest is not re-read)
**Status:** issues_found

## Summary

This round covers plan 01-21 (the `ExecutableOnPath` value source, its use by `VerifyNodeVersion`, `courses.root` and the formatter-binary tasks, and the new TestKit class) and ADR-0004. It rewrites the phase's single review record. Findings from earlier rounds that are outside this scope are carried forward unchanged with their ids (WR-02, WR-06, WR-08, IN-01 to IN-07).

No defect was found in the new production code. What was checked and held up:

- `ExecutableOnPath.firstExecutable` returns an absolute path of an existing regular, executable file or null. Relative, empty, non-executable and directory entries are skipped, and broken symlinks fail `isFile`. A missing `PATH` gives null, not an exception.
- The three call sites do the right thing with a null. `VerifyNodeVersion` treats a null as "not available" and fails with the existing `mise install` hint, and a launch failure is still caught as `GradleException`. `VerifyFormatterBinary` keeps its empty-string contract. `nodePath.orNull?.let` leaves the Prettier step unconfigured when node is missing, and `verifyNodeVersion` then fails first.
- No other process in `build-logic/src/main` is launched by a bare name: the only `commandLine(` calls take the resolved absolute path, and the only environment read is in `ExecutableOnPath.kt`.
- The production wiring is lazy, as the summary says. I ran `./gradlew help` with the configuration cache, then again with a fake `taplo` put first on `PATH`: the cache entry was reused, so a non-formatting build is not invalidated by tool changes.
- I ran `ExecutableOnPathFunctionalTest` (`:build-logic:functionalTest --tests '*ExecutableOnPathFunctionalTest*'`, `--offline`, `--rerun`): all five tests pass. The one deprecation in the run (`Configuration.setVisible`) does not come from these files.
- The ADR-0004 claims checked against the repository hold. `governance/gsd-config-policy.json` equals `.planning/config.json` today. `checkGsdConfigPolicy` is wired into `check`, and `ConfigPolicyCheck` ignores key order and reports key paths only. `.planning/config.json` and `governance/**` are governed paths and have ask rules. `Bash(*config-set*)` is an ask rule. The `agent_tools` and search-provider keys match the text. `plan_review.source_grounding` and `workflow.plan_check` exist, and the WR-01 and WR-07 provenance matches plans 01-17 and 01-19. The links to ADR-0001 and ADR-0003 resolve.

New in this round: one Warning (WR-09, an overclaim in ADR-0004 that repeats WR-08) and three Info items (IN-08 to IN-10).

Decisions recorded in SUMMARYs (production wiring stays lazy, author option B of F-1; relative PATH entries skipped, P-3; major-only `node-major.txt`, P-4; `nodeExecutable` without `npmExecutable`, P-1) were not re-litigated. Structural findings (fallow) were not provided. The necessity rubric was read at the first pass.

## Resolution of findings from earlier reviews

Unchanged from the previous review: CR-01, WR-01, WR-03, WR-04, WR-05 and WR-07 stay RESOLVED, and WR-06 stays ACCEPTED-RESIDUAL (ADR-0003). Nothing in this round's files touches them.

## Warnings

### WR-02: The approval label is sticky and is not bound to a commit

_Carried from the previous review, unchanged; deferred by the author. The code is at `scripts/governance/check-pr.sh:297-300`._

**File:** `scripts/governance/check-pr.sh:297-300`
**Issue:** Once `governance-approved` is on the PR, every later push passes. The check never learns which commit the maintainer approved, or who applied the label. The ruleset also has `dismiss_stale_reviews_on_push: false` and zero required reviews (`.github/rulesets/main.json`), so nothing else invalidates the approval. A PR approved at commit A can gain further governed changes at commit B and still pass. The wider governed boundary from plans 01-17 and 01-19 makes more files subject to this.
**Fix:** Bind approval to the head SHA, for example by removing the label in a workflow step on `synchronize`. This needs `issues: write` or `pull-requests: write` and is only safe on same-repository PRs. Alternatively, make the label step record `github.event.pull_request.head.sha` and require the label event to be newer than the last push. At minimum, document the accepted residual risk in ADR-0001 (ADR-0003 now lists it as a separate deferred residual).

### WR-08: ADR-0003 relies on Claude Code ask rules as a control over the agent, which the project itself says is not a security boundary

_Carried from the previous review, unchanged; ADR-0003 was not touched in this round._

**File:** `docs/adr/0003-governance-gate-trust-boundary.md:51-53`
**Issue:** Reason 2 says the ask rules "cover every governed path ... so the project's agent cannot change the gate without the author confirming the edit (AIDEV-08)". That overstates what the control does. `.claude/settings.json` holds only `Edit(/...)` ask rules (plus `Bash(*config-set*)`); there is no ask or deny rule on shell commands, so a `sed -i`, redirect, `git apply` or `cp` through the Bash tool changes a governed file without matching any rule. Edit rules also do not apply in a permission mode that skips prompts. The project's own code says the same: the KDoc of `AskRulesCheck` states "Ask rules are a convenience for the author, not a security boundary". The ADR's Decision paragraph hedges correctly ("an agent that does not tamper with the gate"), but Reason 2 and the Consequences section read as if the agent route were closed. The decision itself stands: the maintainer's diff review is the real control either way.
**Fix:** Reword Reason 2 to match the Decision paragraph, for example: "Claude Code's ask rules cover every governed path for the editing tools, so an agent that edits files with the Edit and Write tools has to ask the author first. The rules do not cover shell commands and are a convenience, not a security boundary (`AskRulesCheck`), so the diff review in Consequences stays the control."

### WR-09: ADR-0004 repeats the ask-rule overclaim of WR-08 for the one file it exists to protect

**File:** `docs/adr/0004-gsd-configuration.md:32-33`
**Issue:** Step 1 of "Changing the configuration deliberately" says the ask rules "make the author confirm both routes: the rule for edits to the file and the rule for `config-set` calls". The rules do not close those routes. `Edit(/.planning/config.json)` matches only the Edit tool. `Bash(*config-set*)` matches only a command line that contains the text `config-set`. A `sed -i`, `jq ... > file`, `cp` or a script run through the Bash tool rewrites `.planning/config.json` without matching either rule. This matters more here than in ADR-0003, because the sentence sits in the procedure that readers treat as the control for GSD settings. Two other ADR-0004 statements depend on it: the Consequences line "Every configuration change is a reviewed two-file pull request that carries the `governance-approved` label" is true of what reaches `main` (the CI check and the gate), but not of what an agent can change on a working branch. The ADR also does not say that the real controls are `checkGsdConfigPolicy` (fails `check`), the label gate and the maintainer's diff review. The ADR links ADR-0003 in step 3 but still presents step 1 as an agent-side control.
**Fix:** Reword step 1, for example: "Change `.planning/config.json`, through GSD or by hand. The ask rules in `.claude/settings.json` (AIDEV-08) make the author confirm the Edit tool and `config-set` routes. They do not cover other shell commands and are a convenience, not a security boundary, so `checkGsdConfigPolicy` and the pull request review remain the controls." Apply the WR-08 rewording to ADR-0003 in the same change.

## Info

### IN-01: Branch-name rule rejects the GSD milestone branches that the pinned policy declares

_Carried from the previous review, unchanged. The pattern is at `scripts/governance/check-pr.sh:29`._

**File:** `scripts/governance/check-pr.sh:29`
**Issue:** `governance/gsd-config-policy.json` pins `milestone_branch_template: "gsd/{milestone}-{slug}"`. `PHASE_BRANCH_PATTERN` only allows `gsd/phase-...`, so a PR from a milestone branch fails rule 2. This is unreachable while `branching_strategy` stays `phase`, but the two files disagree.
**Fix:** Either allow the milestone shape in the pattern, or note in the script header that milestone branching is unsupported. A single shared source for the branch templates is better.

### IN-02: `ToolingTableCheck` cannot see `[tools.<name>]` sub-tables or multi-line inline tables

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt:79-90`
**Issue:** Any line starting with `[` sets `inTools` to false, and `TOOLS_HEADER` matches only the exact `[tools]`. A tool pinned as `[tools."npm:foo"]` or `[tools.foo]` is invisible to the inventory check. With TOML 1.1 multi-line inline tables, continuation lines such as `version = "..."` would be reported as phantom tools. Today every entry is a single-line key, so nothing is wrong yet.
**Fix:** Match `^\[tools(\.|])` headers and derive the key from the header, or parse with a TOML library. Alternatively add a test that pins the "single-line keys only" assumption and fails loudly on a `[tools.` header.

### IN-03: Duplicated logic

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `.github/workflows/ci.yml:44-49,98-103,161-166` and `build-logic/src/main/kotlin/courses.root.gradle.kts:198-206` vs `courses.kotlin-library.gradle.kts:26-37`
**Issue:** The "Read the mise version" step is copy-pasted into three jobs. The `.java-version` to `JavaLanguageVersion` parsing, including the error message, is duplicated in two convention plugins. (In this round the parsing block moved to `courses.root.gradle.kts:202-216`; the duplication is unchanged.)
**Fix:** Move the mise-version read into a local composite action, or read it once in a job and pass it on. Put the JDK major parse into a shared build-logic helper.

### IN-04: Redundant or not-yet-instantiated attribute entries

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `.gitattributes:5-6,31,34-35` and `.editorconfig:46-48`
**Category:** necessity
**Issue:** `gradlew text eol=lf` and `*.sh text eol=lf` repeat what `* text=auto eol=lf` on line 2 already sets, and the `.editorconfig` `[gradlew] end_of_line = lf` repeats the `[*]` default. `frontend/**/generated/**`, `pnpm-lock.yaml`, `docs-site/**` and `gradle/verification-metadata.xml` match no tracked file in this repository. This is provable from the repository, but the entries are cheap and anticipatory, so it rests on judgment.
**Fix:** Keep them if they are deliberate forward declarations. Otherwise drop the redundant `gradlew` and `*.sh` rules, and add the future-path rules in the phase that creates those paths.

### IN-05: The governance gate and shell tests depend on unpinned runner tools

_Carried from the previous review, unchanged. It also applies to `list-pr-files.sh`, and to the `--raw-output0` records stream that WR-05's fix still depends on (it needs jq 1.7)._

**File:** `.github/workflows/governance.yml:25-46`, `docs/tooling.md:80`
**Issue:** The governance job runs `scripts/governance/check-pr.sh` with the runner image's `jq` and `gh`, not the versions pinned in `mise.toml` (jq 1.8.2, gh 2.104.0). The script needs `jq --raw-output0`, which exists only in jq 1.7 or later. A runner image change could break or alter the gate. It fails closed in the missing-feature case, but behaviour can differ between the pinned and the runner jq. The shell tests in the `lint` job run with the same unpinned jq.
**Fix:** Install `jq` and `gh` through the pinned `mise` setup in the governance job. Alternatively, have the script assert a minimum `jq` version at the start.

### IN-06: Invisible characters are written literally in the Kotlin test source

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPathsFormatFunctionalTest.kt:55,84`
**Issue:** K4 relies on a literal non-breaking space at the start of the second line of `"mise.toml\n .mcp.json\n"`, and K7 on a literal U+FEFF at the start of `"<U+FEFF>mise.toml\n"`. Neither is visible in an editor or a diff. If a tool or a contributor normalises the NBSP to an ordinary space, K4 changes meaning: the padded entry is stripped and valid, the build passes, and the test fails with a confusing message that does not mention whitespace. The bash twins already spell these as `\xc2\xa0` and `\xef\xbb\xbf` escapes.
**Fix:** Spell them as Kotlin escapes, `"mise.toml\n<U+00A0>.mcp.json\n"` and `"<U+FEFF>mise.toml\n"`, so the intent is readable and survives reformatting.

### IN-07: `package.json` is governed as a whole file for the sake of a deprecated `renovate` key

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `governance/governed-paths.txt:92-104`
**Issue:** The header's boundary rule governs a path when a change to it alone can rewrite the policy, and the comment itself says `package.json` counts "only through a deprecated `renovate` section". The entry governs the whole root file. No root `package.json` exists today, but the project's stack decision takes the pnpm version from the root `packageManager` field, so one will exist once the frontend lands, and it will then carry scripts, dev dependencies and the `packageManager` pin. Every human edit to it will need the label. A Renovate bump of the `packageManager` pin (`pnpm@x.y.z+sha512.<128 hex>`) will also not count as version-only: `normalize` collapses 64-hex and 40-hex runs, not a 128-hex sha512, so the removed and added lines differ after normalisation. This is a judgment call about future friction, not a bug today.
**Fix:** When the root `package.json` is created, decide deliberately: keep it governed and extend `normalize` with a `[0-9a-f]{128}` digest rule, or drop the entry and add a check that rejects a `renovate` key in `package.json`. Until then, note the expected friction next to the entry.

### IN-08: The `ExecutableOnPath` KDoc names the wrong working directory

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPath.kt:19-22`
**Issue:** The KDoc says "Gradle moves the daemon's working directory to the directory of each build". The test written to prove the relative-entry rule says the opposite: `ExecutableOnPathFunctionalTest.kt:117-119` states that TestKit starts the daemon's build from the test JVM's working directory, not the fixture root, and the control assertion at line 173 only passes because that is true. In Gradle, the daemon takes the directory the client was started in, which is not necessarily the project directory (it differs with `-p`, with `--project-dir`, and under TestKit). The rule itself (skip relative entries) is correct and is what the tests prove, but a maintainer who reads the KDoc will assume the project directory and could write a wrong test or a wrong "fix". The test comment is the accurate one.
**Fix:** Reword to: "Gradle gives the daemon the working directory of the client that started each build, so file checks of a relative entry follow the client, while the absolute path that the resolver returns would not."

### IN-09: Test (b) does not force a warm daemon, unlike test (a)

**File:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPathFunctionalTest.kt:79-98`
**Issue:** Test (a) isolates its daemon with a per-test `java.io.tmpdir` and asserts the daemon PID, so it controls the one condition under which the JDK's cached `PATH` matters. Test (b), which proves the other half of G-01-2 (Spotless's Prettier server runs the guarded node), runs in the shared default TestKit daemon, and puts `loggingDir` first on `PATH`. Whether it can fail on the unfixed code therefore depends on what that shared daemon launched earlier and on which process resolves `node` by name (the daemon's cached `PATH`, or `npm`'s `#!/usr/bin/env node` against the environment Spotless passes). A cold daemon or a different test order could let it pass without `nodeExecutable`. This rests on the launch mechanics of Spotless and the JDK, which I did not prove. The test passes today and the plan recorded its red state, so the finding is about repeatability, not a present failure.
**Fix:** Give test (b) the same isolated daemon as test (a), by extracting the `daemon-tmp` setup into a fixture helper, and make the first build warm the daemon with a different `PATH` before the logging build.

### IN-10: The two "run `--version` and fail with a `mise install` hint" tasks duplicate their mechanism

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt:64-89`
**Issue:** `VerifyNodeVersion.activeNodeVersion` and `VerifyFormatterBinary.runsVersion` (`VerifyFormatterBinary.kt:62-71`) each run `<absolute path> --version` with `isIgnoreExitValue`, capture stdout and turn an `ExecException` into the same "not available on PATH ... `mise install`" failure. Plan 01-21 touched both and made the two shapes the same (absolute path in, nullable result out), but the hint text and the exec block are still copied. The two copies have already drifted: one names `setup-node` for CI, the other names `mise x -- ./gradlew`. The duplication is small and cheap to keep.
**Fix:** If a third tool is added, extract one helper that takes the resolved path and returns the stdout or null, and keep the messages in the task classes.

---

_Reviewed: 2026-11-22_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
