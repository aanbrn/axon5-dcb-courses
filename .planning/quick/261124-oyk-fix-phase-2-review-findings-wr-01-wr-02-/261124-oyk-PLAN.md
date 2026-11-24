---
phase: quick-261124-oyk
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - scripts/doctor.sh
  - scripts/test-doctor.sh
  - scripts/governance/ask-governed-merge.sh
  - scripts/governance/test-ask-governed-merge.sh
  - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/build.gradle.kts
  - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
  - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
autonomous: true
requirements: [TOOL-03, AIDEV-08, INFRA-09, CICD-04, INFRA-06]

estimate:
  tokens: 110000
  raw_tokens: 110000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "WR-01: when every `git` call exits 1 with the xcode-select note on stderr (the macOS `/usr/bin/git` shim without the Command Line Tools), `scripts/doctor.sh` prints `fail  git: cannot read the version from 'git --version'` with a `fix: brew install git ...` line and a `fix: xcode-select --install ...` line, runs every later check, prints the `summary:` line and exits 1"
    - "WR-01 siblings (same defect, observed at planning): a `mise --version` that exits non-zero gives `fail  mise: cannot read the installed version from 'mise --version'` and the summary; a `claude --version` that exits non-zero gives `warn  claude-code: cannot read the version from 'claude --version'` and the summary; neither aborts the run"
    - "IN-01: `bash $SCRIPT; git merge docs-only` and `bash \"$HOME/run.sh\" && git merge docs-only` still ask (fail closed), with a reason that says the script or the options of bash come from a shell expansion and names the word, and no longer says the options of bash cannot be told apart; `bash $OPTS -c 'git merge docs-only'` asks with the same reason; `bash run.sh && git merge docs-only` stays silent; ADR-0015 states that a script file named by a shell expansion asks and why"
    - "WR-03: `checkImageReferences` reports, naming the key, every `[versions]` image entry the Renovate image manager of `renovate.json5` cannot read correctly: a double- or single-quoted key, a key outside `image-[a-z0-9-]+` (upper case, `_`, `.`), a separator other than exactly one space on each side of `=` (none, two, a tab), and a reference whose registry has a port; it still accepts bare lower-case keys at any indentation, with a trailing comment; `renovate.json5` is unchanged"
    - "WR-03 parity: a functional test reads the image regex from the repository's `renovate.json5` and proves that it reads every sample entry the check accepts with exactly its name, tag and digest, matches none of the rejected key and separator samples, and misreads the port sample (name `localhost`); the same sample lists drive the TestKit runs, and `renovate.json5` is a declared input of the functionalTest task"
    - "WR-02: the `CoursesJsonModule` KDoc states that an ID used as a map key is written as its raw UUID by jackson-module-kotlin's value-class handling and read back from it, that a TypeID key is rejected, that the key form is an open decision taken with an ADR in the phase whose contract first holds an ID-keyed map, and that the TypeID form needs both a key serializer and a key deserializer; `JsonRoundTripTest` pins the observed behaviour; no code line of the module changes"
    - "All gates are green: `bash scripts/test-doctor.sh` (>=167 passed, 0 failed), `bash scripts/governance/test-ask-governed-merge.sh` (>=169 passed, 0 failed), shellcheck on the four scripts, and `mise x -- ./gradlew :build-logic:check :serialization-jackson:check checkImageReferences checkAdrs spotlessCheck`"
  artifacts:
    - path: "scripts/doctor.sh"
      provides: "version reads of mise, claude and git that fall back to an empty value instead of aborting under set -euo pipefail; xcode-select fix line in the git check"
      contains: "|| installed=''"
    - path: "scripts/test-doctor.sh"
      provides: "fakes that fail on a marker file (`git-broken`, `mise-version-fails`, `claude-fails`) and one case for each"
      contains: "git-broken"
    - path: "scripts/governance/ask-governed-merge.sh"
      provides: "analyze_shell reason for a dynamic word before the script: the script or the options of the shell come from a shell expansion"
      contains: "the script or the options of"
    - path: "scripts/governance/test-ask-governed-merge.sh"
      provides: "cases for `bash $SCRIPT`, `bash \"$HOME/run.sh\"`, the updated `$OPTS` case and a silent literal script file"
      contains: "the script or the options of bash"
    - path: "docs/adr/0015-governance-hooks-for-worktrees-and-merges.md"
      provides: "consequence sentence: a script file named by a shell expansion asks"
      contains: "bash \"$HOME/run.sh\""
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt"
      provides: "catalog entry rules equal to the Renovate image manager (bare key image-[a-z0-9-]+, ' = ', no registry port) and a KDoc that states the shared form"
      contains: "image-[a-z0-9-]+"
    - path: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt"
      provides: "TestKit cases per rejected shape and a parity test against the regex of renovate.json5"
      contains: "renovate.json5"
    - path: "build-logic/build.gradle.kts"
      provides: "renovate.json5 among the functionalTest repoFiles inputs"
      contains: "../renovate.json5"
    - path: "backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt"
      provides: "KDoc on the observed map-key behaviour and the open key-form decision"
      contains: "raw UUID"
    - path: "backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt"
      provides: "two tests that pin the ID map-key behaviour"
      contains: "Map<CourseId"
  key_links:
    - from: "scripts/test-doctor.sh"
      to: "scripts/doctor.sh"
      via: "run_doctor runs the copy of doctor.sh with PATH=$dir/bin:$SYSBIN, so the fakes in $dir/bin shadow the real tools; the marker files under $FAKE_DIR switch a fake to failing"
      pattern: "run_doctor"
    - from: "scripts/governance/test-ask-governed-merge.sh"
      to: "scripts/governance/ask-governed-merge.sh"
      via: "run_case feeds a PreToolUse Bash input to the hook and matches permissionDecisionReason with a jq regex"
      pattern: "run_case"
    - from: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt"
      to: "renovate.json5"
      via: "fixture.copyFromRepo(\"renovate.json5\") then the single-quoted matchString that holds image-[a-z0-9-]+ is unescaped and compiled"
      pattern: "copyFromRepo"
    - from: "build-logic/build.gradle.kts"
      to: "renovate.json5"
      via: "functionalTest repoFiles input, so a change to the regex reruns the parity test instead of reusing a cached result"
      pattern: "repoFiles"
    - from: "backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt"
      to: "backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt"
      via: "coursesJsonMapper() with the Kotlin module and CoursesJsonModule"
      pattern: "coursesJsonMapper"
---

<objective>
Fix the four findings of the incremental Phase 2 review
(`.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md`: WR-01, WR-02, WR-03 and IN-01), as the author
decided.

Author decisions (locked):
- WR-01: when `git --version` exits non-zero, doctor reports a `fail  git:` line with a `fix:` hint and goes on to the
  summary. A `scripts/test-doctor.sh` case uses a fake `git` that exits 1.
- WR-02, option (a): documentation only. The KDoc states the current behaviour (an ID map key is written as the raw
  UUID and read back as such) and that the TypeID key form is an open decision, taken with an ADR in the phase that
  first puts an ID-keyed map in a contract. No key serializer or deserializer and no behaviour change. The claim is
  verified by running code.
- WR-03: `ImageReferenceCheck` accepts exactly the catalog entry shapes the Renovate regex in `renovate.json5` (line
  83) matches. `renovate.json5` stays unchanged. The KDoc parity claim is fixed, and `ImageReferenceCheckFunctionalTest`
  gets a case for each rejected shape.
- IN-01: the prompt for `bash $SCRIPT; git merge x` names the real reason. The hook stays fail-closed, and a test
  asserts the message.

Planner findings and decisions (Claude's discretion inside the approved scope; verified at 6d81294):
- WR-01 has siblings. `check_mise` (`scripts/doctor.sh:105`) and `check_claude_code` (line 556) read
  `<tool> --version | awk ...` the same way, so a failing `mise --version` or `claude --version` also ends doctor
  silently. They get the same one-line fallback and one test each. The macOS shim fails every git call, so the fake
  `git-broken` mode fails every call, not only `--version`. That proves the later checks (`claude-tracking`,
  `upstream-removal`) still run. The "cannot read the version" report gets a second fix line,
  `xcode-select --install`, because the shim case is the common real one. The existing first fix line stays, and the
  existing assertion on it still holds.
- WR-03: the review says a registry port is never matched. That is wrong. The manager regex does match
  `localhost:5000/app:1.0@sha256:...`, but its `depName` group stops at the first colon, so it reads the image as
  `localhost` and the tag as `5000/app:1.0`. Renovate would look up the wrong image and never update the right one. The
  check therefore rejects a port reference with its own message. The parity guarantee is one-directional by design:
  every entry the check accepts, the manager reads with exactly its name, tag and digest. The check stays stricter
  where the manager would also read a reference that is not a valid pinned image (a tag with `/` or `:`, an empty path
  segment, text after the closing quote). A parity test compiles the regex straight from `renovate.json5`, so a later
  change to either side fails the build. `renovate.json5` therefore becomes a declared functionalTest input, a one-line
  change to the governed `build-logic/build.gradle.kts`. Taplo runs with `align_entries = false`, so formatted catalogs
  always use one space on each side of `=`.
- IN-01: the dynamic word sits where an option or the script operand is expected, and the hook cannot tell which.
  `bash $X 'git merge governed'` with `X=-c` runs a merge, so staying silent would open a bypass. One reason that is
  true in every case replaces the option-end message: `the script or the options of <shell> come from a shell
  expansion: <word>`. No look-ahead heuristic is used, and the decision (ask) does not change. ADR-0015 already says
  that a script from a shell expansion asks. Its consequence bullet on script files (line 216) gets one sentence so the
  contrast with a literal script file is explicit, as the review asks when the prompt is kept.
- WR-02: the review's probe is confirmed by a test, not only a probe. Two tests in `JsonRoundTripTest` pin the observed
  behaviour, so the open decision is visible and a change to the key handling fails a test. If the observation differs
  from the review (for example, keys come back as `UUID` objects instead of `CourseId`), the KDoc and the tests state
  what was observed and the SUMMARY records it. Production behaviour still does not change.

The executor runs in an agent worktree. Governed paths (`governance/governed-paths.txt`) touched here:
`scripts/doctor.sh`, `scripts/governance/**`, `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**`
and `build-logic/build.gradle.kts`. Edit them with the Edit and Write tools only. Never use shell writes (`sed -i`,
redirects, heredocs), and never use `spotlessApply` on them. When `spotlessCheck` reports a diff in a governed file,
apply that diff by hand with Edit. Use the file tools for the other files too. Out of scope: `02-REVIEW-DISPOSITION.md`
and STATE.md (the orchestrator records the fixes with the commit hashes), `renovate.json5`,
`governance/governed-paths.txt`, `.claude/settings.json`, `.github/**`. Commit each finding on its own (WR-01, IN-01,
WR-03, WR-02), so that the disposition can cite one hash per finding.

Purpose: TOOL-03 (doctor explains every problem instead of dying silently), AIDEV-08 (the merge hook's prompt states
the real reason), INFRA-09 and CICD-04 (every catalog image is in a form Renovate updates), INFRA-06 (the serialization
convention is documented truthfully).
Output: hardened version reads in doctor with tests; a truthful merge-hook reason with tests and an ADR sentence; an
image check aligned with the Renovate manager and guarded by a parity test; a corrected `CoursesJsonModule` KDoc pinned
by two tests.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md

Read only the parts named (line numbers verified at 6d81294):
- scripts/doctor.sh: `set -euo pipefail` 26, `report` 50-69, `has` 71-73, `check_mise` 97-122 (version read 105),
  `check_claude_code` 549-568 (version read 556), `GIT_MIN_VERSION` and `check_git` 570-599 (version read 586,
  cannot-read report 587-591), `check_claude_tracking` 603-621 (its `rev-parse` guard 604), `main` 741-760.
- scripts/test-doctor.sh: header 1-24, `make_env` 39-106 (fake mise 46-58, fake claude 83-88, fake git 89-101, all
  reading their answers from files under `$FAKE_DIR`), `run_doctor` and the asserts 150-193, `fresh` 200-205, the
  `mise` section 270-285, the Claude Code section 473-485, the git section 487-507, the summary line 640-641.
- scripts/governance/ask-governed-merge.sh: `analyze_shell` 890-951 (the dynamic-word branch 899-911, the option-end
  message at 906, the unknown-long-option message at 924 which stays), reason assembly 1361-1391 (a problem appears as
  `the hook could not check every merge in this command (<problem>), so it asks.`).
- scripts/governance/test-ask-governed-merge.sh: `bash_input`, `check`, `run_case` 97-146 (the reason argument is a jq
  regex), the existing `$OPTS` case at 346, the literal script-file case at 317-318, the last line prints
  `<n> passed, <m> failed`.
- docs/adr/0015-governance-hooks-for-worktrees-and-merges.md: 110-125 (what the merge hook asks about) and the
  consequence bullet at 216-217 on script files.
- renovate.json5: 74-88 (the image custom manager; the matchString at 83 is a single-quoted JSON5 string with doubled
  backslashes). Read only, never edit.
- build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt: whole file (160
  lines; class KDoc 19-35, `catalogProblems` 84-96, `catalogProblem` 98-106, `failureMessage` 117-126, companion 128-159).
- build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt:
  whole file (179 lines, 10 tests; the quoted-key assertion at 92 and 100 changes; helpers 166-170, constants 172-178).
- build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt: `file`, `read`,
  `copyFromRepo` 37-62, `PASSED` and `governance` 236-252.
- build-logic/build.gradle.kts: functionalTest suite 47-93 (the `repoFiles` input list 62-73).
- backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt: KDoc 17-31.
- backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt: whole file
  (14 tests; imports 1-22; `invalidFormat` helper and companion at the end).
- backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/Ids.kt: `CourseId` is a
  `@JvmInline value class` over `UUID` whose `toString()` is the TypeID.

Baselines at 6d81294: `bash scripts/test-doctor.sh` ends `155 passed, 0 failed` (about 40 s);
`bash scripts/governance/test-ask-governed-merge.sh` ends `166 passed, 0 failed`; ImageReferenceCheckFunctionalTest
has 10 tests; JsonRoundTripTest has 14; the real catalog has three image entries (lines 31-33), all bare, lower-case
and written with ` = `.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: doctor survives a failing --version (WR-01) and the merge hook names the real reason (IN-01), end to end through the shell tests</name>
  <files>scripts/test-doctor.sh, scripts/doctor.sh, scripts/governance/test-ask-governed-merge.sh, scripts/governance/ask-governed-merge.sh, docs/adr/0015-governance-hooks-for-worktrees-and-merges.md</files>
  <behavior>
    - git shim (marker `$ENV/git-broken`; every fake git call prints `xcode-select: note: No developer tools were found, requesting install.` to stderr and exits 1): exit 1; `^fail  git: cannot read the version from 'git --version'`; `^      fix: brew install git`; `^      fix: xcode-select --install`; `^skip  claude-tracking: ` (a later check ran); `^summary: `.
    - mise `--version` fails (marker `$ENV/mise-version-fails`): exit 1; `^fail  mise: cannot read the installed version from 'mise --version'`; `^summary: `.
    - claude `--version` fails (marker `$ENV/claude-fails`): exit 0; `^warn  claude-code: cannot read the version from 'claude --version'`; `^summary: `.
    - Merge hook: `bash $SCRIPT; git merge docs-only` asks, reason matches `the script or the options of bash come from a shell expansion: \$SCRIPT`.
    - Merge hook: `bash "$HOME/run.sh" && git merge docs-only` asks with `the script or the options of bash come from a shell expansion`.
    - Merge hook: the existing `bash $OPTS -c 'git merge docs-only'` case asserts `the script or the options of bash come from a shell expansion: \$OPTS` instead of the bare `shell expansion`.
    - Merge hook: `bash run.sh && git merge docs-only` passes silently (a literal script file stays outside the hook's view).
  </behavior>
  <action>
    Step 0, before any edit: record the real doctor's fail lines in the worktree as the baseline, because a worktree
    lacks the untracked GSD installation and some local checks fail there already:
    `DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh 2>&1 | grep -E '^fail' | sort > "${TMPDIR:-/tmp}/261124-oyk-doctor-before.txt"`
    (doctor may exit 1; that is expected).

    RED, WR-01 (scripts/test-doctor.sh is not governed; use Edit). In `make_env`, give each of the three fakes a failing
    mode switched by a marker file under `$FAKE_DIR`, following the `docker-down` pattern. The fake mise's `--version`
    branch exits 1 when `$FAKE_DIR/mise-version-fails` exists. The fake claude exits 1 when `$FAKE_DIR/claude-fails`
    exists. The fake git checks `$FAKE_DIR/git-broken` first, for every call: when it exists, it prints the xcode-select
    note to stderr and exits 1, as the macOS shim does. Keep the fakes quoted heredocs, and update the comment that
    precedes each one to name its marker. Add the three cases of `<behavior>`: the mise case at the end of the `mise`
    section, the claude case at the end of the Claude Code section, and the git case at the end of the git section. Each
    case starts with `fresh`, then touches its marker. Run `bash scripts/test-doctor.sh` and confirm the new assertions
    fail: doctor ends without a summary.

    GREEN, WR-01 (scripts/doctor.sh is governed; Edit tool only), per the author's WR-01 decision. At the three version
    reads (lines 105, 556 and 586), append `|| installed=''` to the assignment, exactly as the review suggests. Under
    `set -euo pipefail`, a failing command in the pipeline then yields an empty value, which the existing guards
    already report. In `check_git`, give the cannot-read report a second fix line after the existing one:
    `xcode-select --install (on macOS, /usr/bin/git is a stub until the Xcode Command Line Tools are installed)`. Extend
    the `check_git` comment by one clause: a `git` that fails outright, like that stub, also reads as a version that
    cannot be read. Keep lines within 120 columns. Run `bash scripts/test-doctor.sh` until it ends with at least
    `167 passed, 0 failed` (155 plus 12 new assertions). Commit WR-01 on its own.

    RED, IN-01 (scripts/governance/test-ask-governed-merge.sh is governed; Edit tool only). In the "options of a shell
    before its script" section, change the `$OPTS` case's reason regex from the bare `shell expansion` to the new full
    reason. Add the `bash $SCRIPT`, `bash "$HOME/run.sh"` and literal `bash run.sh` cases of `<behavior>`, each with a
    name that says what it proves. Escape `$` in the reason regex the way the neighbouring cases do. Run the suite and
    confirm that the new and changed ask cases fail on the reason.

    GREEN, IN-01 (scripts/governance/ask-governed-merge.sh is governed; Edit tool only), per the author's IN-01
    decision. In `analyze_shell`, the branch for a dynamic word reached while `has_script` is 0 (the option-end message
    at line 906) now appends `the script or the options of ${WORDS[$1]} come from a shell expansion: $word`. Keep the
    `COMMAND_MENTIONS_MERGE` guard and the `return 0`, so the decision stays fail-closed and only the text changes.
    Leave the `has_script` branch and the unknown-long-option message unchanged. Rewrite the comment above the branch:
    the word may be an option such as `-c` (which turns the next word into a script) or the script file itself, and the
    hook cannot tell which, so it asks. Run `bash scripts/governance/test-ask-governed-merge.sh` until it ends with at
    least `169 passed, 0 failed`.

    ADR-0015 (docs/adr is not governed; use Edit). Extend the consequence bullet at line 216 ("A script file that a
    shell or `source` runs counts with the programs that run git themselves ...") with one sentence. It says that a
    script file named by a shell expansion (`bash "$HOME/run.sh"`) asks when the command mentions a merge, because the
    expanded word may also be an option such as `-c` that turns the next word into a script. Keep Prettier's wrapping.
    Commit IN-01 (hook, its test and the ADR) on its own.

    End to end: compare the real doctor in the worktree with the Step 0 baseline (see `<verify>`). It must print no new
    fail line and must still print its `summary:` line. Run shellcheck on all four scripts (the lefthook pre-commit hook
    also runs it on staged `*.sh` files).
  </action>
  <verify>
    <automated>bash scripts/test-doctor.sh | tail -n 1 | awk '{exit !($1 >= 167 && $3 == 0)}'  # [HEAD=155 EXPECT>=167]</automated>
    <automated>bash scripts/governance/test-ask-governed-merge.sh | tail -n 1 | awk '{exit !($1 >= 169 && $3 == 0)}'  # [HEAD=166 EXPECT>=169]</automated>
    <automated>grep -v '^ *#' scripts/doctor.sh | grep -c "|| installed=''"  # [HEAD=0 EXPECT==3]</automated>
    <automated>grep -v '^ *#' scripts/doctor.sh | grep -c 'xcode-select --install'  # [HEAD=0 EXPECT==1]</automated>
    <automated>grep -v '^ *#' scripts/governance/ask-governed-merge.sh | grep -c 'end: they come from a shell expansion'  # [HEAD=1 EXPECT==0]</automated>
    <automated>grep -v '^ *#' scripts/governance/ask-governed-merge.sh | grep -c 'the script or the options of'  # [HEAD=0 EXPECT==1]</automated>
    <automated>grep -c 'the script or the options of bash' scripts/governance/test-ask-governed-merge.sh  # [HEAD=0 EXPECT>=1] (the reason may live in one shared variable)</automated>
    <automated>grep -cF 'bash "$HOME/run.sh"' docs/adr/0015-governance-hooks-for-worktrees-and-merges.md  # [HEAD=0 EXPECT>=1]</automated>
    <automated>test -z "$(DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh 2>&1 | grep -E '^fail' | sort | comm -13 "${TMPDIR:-/tmp}/261124-oyk-doctor-before.txt" -)"  # [no new fail line versus the Step 0 baseline]</automated>
    <automated>mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh scripts/governance/ask-governed-merge.sh scripts/governance/test-ask-governed-merge.sh</automated>
  </verify>
  <done>
    A `git`, `mise` or `claude` whose `--version` fails no longer ends doctor silently. Doctor reports it (git with the
    `brew install git` and `xcode-select --install` fixes), runs the remaining checks and prints the summary.
    `scripts/test-doctor.sh` proves all three and ends with at least `167 passed, 0 failed`. The merge hook asks for
    `bash $SCRIPT`, `bash "$HOME/run.sh"` and `bash $OPTS -c ...` with the reason "the script or the options of bash
    come from a shell expansion: <word>", and it stays silent for a literal script file. Its suite ends with at least
    `169 passed, 0 failed`. ADR-0015 records the contrast. The real doctor shows no new fail line, shellcheck is clean,
    and WR-01 and IN-01 are separate commits.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: align ImageReferenceCheck with the Renovate image manager and guard the parity with a test (WR-03)</name>
  <files>build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt, build-logic/build.gradle.kts</files>
  <behavior>
    - Accepted samples (one TestKit run passes with all of them in `[versions]`), each with the name and tag the manager must read: `image-axonserver = "docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot@sha256:<DIGEST>"`; `image-postgres = "postgres:18.7@sha256:<DIGEST>"`; `  image-indented = "testcontainers/ryuk:0.14.0@sha256:<DIGEST>"`; tab-indented `image-tabbed = "postgres:18.7@sha256:<DIGEST>"`; `image-commented = "postgres:18.7@sha256:<DIGEST>" # a trailing comment`; `image-a1-b2 = "postgres:18.7@sha256:<DIGEST>"`.
    - Rejected key samples, each with a valid pinned value (one failing run names every key with the bare-key message): `"image-double-quoted" = ...`, `'image-single-quoted' = ...`, `image-Upper = ...`, `image-under_score = ...`, `image-dot.ted = ...`.
    - Rejected separator samples (one failing run names every key with the separator message): `image-tight="..."`, `image-wide  = "..."`, `image-wide-after =  "..."`, `image-tab-sep<TAB>= "..."`.
    - Registry port: `image-port = "localhost:5000/app:1.0@sha256:<DIGEST>"` fails with the port message naming `image-port`.
    - Parity (no Gradle run): the image matchString read from the repository's `renovate.json5` reads every accepted sample with `depName`, `currentValue` and `currentDigest` equal to its name, tag and `sha256:<DIGEST>`; it matches no rejected key or separator sample; it reads the port sample with `depName` `localhost`.
    - Existing tests keep passing, except the quoted-key assertion in the "not a double-quoted string" test. `"image-quoted" = "postgres:18.7"` is now reported for its key, so it asserts the bare-key message.
  </behavior>
  <action>
    RED (the functional test is not governed; use Edit). Add to the test's companion the sample lists of `<behavior>`:
    a private list of accepted entries, each holding the catalog line, the expected name and the expected tag; a list
    of rejected key lines and one of rejected separator lines, each paired with the key a finding must name; and the
    port line. Build every value from the existing `DIGEST` constant. Add tests with prose names:
    (1) every accepted shape passes (one `fixture.run`); (2) every rejected key shape fails naming each key with the
    bare-key message; (3) every rejected separator shape fails naming each key with the separator message; (4) a
    registry port fails with the port message; (5) a pure parity test with no Gradle run. Test 5 calls
    `fixture.copyFromRepo("renovate.json5")` and `fixture.read(...)`, finds the single single-quoted matchString that
    contains `image-[a-z0-9-]+`, and fails with a clear message if there is not exactly one. It unescapes the doubled
    backslashes, compiles the result as a Kotlin `Regex`, and asserts the parity facts of `<behavior>` through the named
    groups. Update the quoted-key assertion of the existing test at lines 92/100. Run
    `mise x -- ./gradlew :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*'`: tests 2, 3, 4 and
    the changed assertion fail, and tests 1 and 5 pass, which shows that the manager really behaves as the samples
    claim.

    GREEN (ImageReferenceCheck.kt is governed; Edit tool only), per the author's WR-03 decision. `renovate.json5` stays
    unchanged.
    - Widen `CATALOG_ENTRY` into the detector of image entries. It captures the quote, a key of `image-` followed by any
      characters up to the closing quote, a blank or `=`, the blanks before `=`, the blanks after it, and the rest of
      the trimmed line. Bare and quoted keys of any spelling are then detected and checked.
    - Add `MANAGED_KEY` = `image-[a-z0-9-]+`, the key class of the manager regex. Add `REGISTRY_PORT`, which matches a
      reference whose first segment before a slash holds a colon and digits.
    - `catalogProblem` reports the first problem of each entry in this order:
      1. a quoted key or a key that does not match `MANAGED_KEY`: `<path>: <key> must be a bare key of the form
         image-[a-z0-9-]+, the only key the Renovate image manager reads`;
      2. blanks around `=` other than exactly one space on each side: `<path>: <key> must have exactly one space on
         each side of '=', the only separator the Renovate image manager reads`;
      3. a value that is not double-quoted: the existing message;
      4. a port reference: `<path>: <key> must not name a registry port, which the Renovate image manager reads as
         part of the tag, but is '<reference>'`;
      5. any other reference that is not pinned: the existing message.
    - Drop the optional registry-port group from `PINNED_REFERENCE` and rewrite its KDoc: the name has no colon, as in
      the manager's `depName`.
    - Rewrite the second bullet of the class KDoc to state the shared form. Every `image-*` key of `[versions]` is
      detected, bare or quoted and at any indentation (`catalogProblems` trims each line). Each must be written exactly
      as the Renovate custom manager for images in `renovate.json5` reads it: a bare key `image-[a-z0-9-]+`, one space
      on each side of `=`, and a double-quoted `<name>:<tag>@sha256:<64 hex digits>` whose name holds no registry
      port; a trailing comment is allowed. Any other form is reported, because the manager would skip or misread it
      and never update it. Every accepted entry is one the manager reads with its name, tag and digest. The check is
      stricter only where the manager would also read a reference that is not a valid pinned image. The functional
      test matches its samples against the regex read from `renovate.json5`.
    - Update the `CATALOG_ENTRY` KDoc to match, and extend the closing sentence of `failureMessage` to say that each
      entry is written in the form the Renovate image manager reads.

    build-logic/build.gradle.kts (governed; Edit tool only): add `"../renovate.json5"` to the functionalTest
    `repoFiles` input list. The parity test then reruns when the regex changes, instead of being answered from the
    build cache.

    Run the focused functional test, `:build-logic:detekt`, `checkImageReferences` (the real catalog must pass the
    tightened check unchanged) and `spotlessCheck`. Apply any Spotless diff in a governed file by hand. Avoid IDE-level
    findings that the Qodana gate would report (unused declarations, redundant constructs), with no suppressions.
    Commit WR-03 on its own.
  </action>
  <verify>
    <automated>mise x -- ./gradlew :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :build-logic:detekt checkImageReferences spotlessCheck</automated>
    <automated>grep -c '@Test' build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt  # [HEAD=10 EXPECT>=15]</automated>
    <automated>grep -cF 'renovate.json5' build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt  # [HEAD=0 EXPECT>=1]</automated>
    <automated>grep -v '^ *\*' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt | grep -cF '(?::[0-9]+)?'  # [HEAD=1 EXPECT==0]</automated>
    <automated>grep -cF 'image-[a-z0-9-]+' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt  # [HEAD=0 EXPECT>=2]</automated>
    <automated>grep -cF 'renovate.json5' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt  # [HEAD=0 EXPECT>=1]</automated>
    <automated>grep -cF '"../renovate.json5"' build-logic/build.gradle.kts  # [HEAD=0 EXPECT==1]</automated>
    <automated>git diff --quiet 6d81294 -- renovate.json5 gradle/libs.versions.toml  # [unchanged]</automated>
  </verify>
  <done>
    `checkImageReferences` rejects quoted keys, keys outside `image-[a-z0-9-]+`, any separator other than ` = `, and
    registry ports, each with a message that names the key. It accepts the manager's form at any indentation and with a
    trailing comment. The real catalog passes unchanged. A parity test proves against the live `renovate.json5` regex
    that every accepted sample is read with its exact name, tag and digest, that no rejected key or separator sample
    matches, and that the port sample is misread. `renovate.json5` is a functionalTest input, the KDoc states the shared
    form, `renovate.json5` itself is unchanged, and detekt and Spotless are clean.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: state the observed ID map-key behaviour in the CoursesJsonModule KDoc and pin it with tests (WR-02)</name>
  <files>backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt, backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt</files>
  <behavior>
    - `an ID map key is written as its raw UUID and read back from it`: for `id = TestIds.courseId(1)`, `mapper.writeValueAsString(mapOf(id to 1))` equals `{"<id.uuid>":1}` and does not contain `course_`; reading that JSON as `Map<CourseId, Int>` (the reified `readValue` of jackson-module-kotlin) equals `mapOf(id to 1)`.
    - `an ID map key in the TypeID form fails deserialisation`: reading `{"<id>":1}` (the TypeID text) as `Map<CourseId, Int>` throws `JacksonException`.
  </behavior>
  <action>
    Probe first (JsonRoundTripTest.kt is a test file; use Edit). Add the two tests of `<behavior>` with block bodies,
    following the class's conventions (prose names, Kotest matchers, the existing `mapper`). Run
    `mise x -- ./gradlew :serialization-jackson:test`. The review predicts that both pass at once: they pin behaviour
    that already exists, so no red phase is expected. If an assertion fails, the observed behaviour differs from the
    review. Then change the test to assert what was actually observed (for example, key objects that are `UUID`s rather
    than `CourseId`s, or a key text that differs), make the KDoc below state exactly that, and record the deviation in
    the SUMMARY. Do not add a key serializer, a key deserializer or any other production behaviour.

    KDoc (CoursesJsonModule.kt; not governed; use Edit), per the author's WR-02 option (a). Replace the paragraph at
    lines 27-30 that starts "These types are registered as values only". The new paragraph says:
    - the types are registered as values, not as map keys;
    - as a map key, jackson-module-kotlin unboxes a value class, so an entity ID key is written as its raw UUID, not as
      its TypeID, and is read back from that UUID; a key in the TypeID form fails; `JsonRoundTripTest` pins this;
    - the wire form of an ID key is an open decision, taken with an ADR in the phase whose contract first holds an
      ID-keyed map;
    - the TypeID form needs both `addKeySerializer` (through `toString()`) and `addKeyDeserializer` (through
      [TypedIdCompanion.parse], reporting a rejected key with the context's `weirdKeyException`), because a key
      deserializer alone would read TypeID keys while the Kotlin module still writes UUID keys.
    Change only KDoc lines; no code line of the file changes. Run `:serialization-jackson:check` (tests, detekt, Dokka
    with failOnWarning) and `spotlessCheck`. Commit WR-02 on its own.
  </action>
  <verify>
    <automated>mise x -- ./gradlew :serialization-jackson:check spotlessCheck</automated>
    <automated>grep -c 'Map<CourseId' backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt  # [HEAD=0 EXPECT>=2]</automated>
    <automated>grep -c 'raw UUID' backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt  # [HEAD=0 EXPECT>=1]</automated>
    <automated>grep -c 'no key deserializer' backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt  # [HEAD=1 EXPECT==0]</automated>
    <automated>grep -c 'addKeySerializer' backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt  # [HEAD=0 EXPECT>=1]</automated>
    <automated>grep -c 'ADR' backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt  # [HEAD=0 EXPECT>=1]</automated>
    <automated>d=$(git diff -U0 6d81294 -- backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt) && test -n "$d" && test -z "$(printf '%s\n' "$d" | grep -E '^[+-]' | grep -vE '^(\+\+\+|---) ' | grep -vE '^[+-] \*')"  # [HEAD: no diff, fails on purpose; EXPECT: a diff whose changed lines are all KDoc lines]</automated>
  </verify>
  <done>
    `JsonRoundTripTest` pins the ID map-key behaviour: a raw UUID key on write, a round trip back to `CourseId`, and a
    TypeID key rejected. If the observation differed, the tests pin what was observed and the SUMMARY records it. The
    `CoursesJsonModule` KDoc states that behaviour truthfully, names the open key-form decision with its ADR and phase
    trigger, and explains why the TypeID form needs both key directions. No code line of the module changes, and
    `:serialization-jackson:check` and `spotlessCheck` pass.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| host tools to doctor output | doctor runs `git`, `mise` and `claude` from PATH and must print a verdict and a fix whatever they return |
| agent Bash command to merge hook | the hook decides from the command text whether a merge may bring governed paths into a checkout; a silent outcome skips the author's confirmation |
| catalog text to Renovate | an image entry the Renovate manager skips or misreads is never updated, so a pinned image can go stale with known vulnerabilities |
| contract JSON to Jackson | the KDoc guides the first ID-keyed map contract; a wrong remedy breaks the wire round trip |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-oyk-01 | Denial of service | `check_git`, `check_mise`, `check_claude_code` version reads under `set -euo pipefail` | medium | mitigate | Task 1: `\|\| installed=''` on the three reads, tested with fakes that fail (`git-broken` fails every call like the macOS shim), each test asserting the summary line |
| T-oyk-02 | Elevation of privilege | `analyze_shell` dynamic-word branch in `scripts/governance/ask-governed-merge.sh` | medium | mitigate | Task 1: only the reason text changes; the `COMMAND_MENTIONS_MERGE` guard and `return 0` stay; tests assert `ask` for `$SCRIPT`, `"$HOME/run.sh"` and `$OPTS -c`, so a regression to silent fails the suite |
| T-oyk-03 | Tampering | `ImageReferenceCheck.catalogProblem` against the Renovate image regex | medium | mitigate | Task 2: check rejects every shape the manager skips or misreads (quoted key, key class, separator, registry port); a parity test compiles the live `renovate.json5` regex, which is a declared functionalTest input |
| T-oyk-04 | Repudiation | `CoursesJsonModule` KDoc on ID map keys | low | mitigate | Task 3: KDoc states observed behaviour, pinned by two tests; no production change |
| T-oyk-05 | Elevation of privilege | governed files (`scripts/doctor.sh`, `scripts/governance/**`, build-logic governance sources, `build-logic/build.gradle.kts`) | low | mitigate | Edit and Write tools only, never shell writes or `spotlessApply`; the merge hook lists them at cleanup for the author |
| T-oyk-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package-manager install and no dependency change in this plan, so the package legitimacy gate has nothing to audit |
</threat_model>

<verification>
Run from the checkout root after all tasks:
- `bash scripts/test-doctor.sh` ends with at least `167 passed, 0 failed`
- `bash scripts/governance/test-ask-governed-merge.sh` ends with at least `169 passed, 0 failed`
- `mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh scripts/governance/ask-governed-merge.sh scripts/governance/test-ask-governed-merge.sh` is clean
- `mise x -- ./gradlew :build-logic:check :serialization-jackson:check checkImageReferences checkAdrs spotlessCheck`
  succeeds. The whole functionalTest suite reruns, because its inputs changed. If it outlasts the 10-minute foreground
  limit, run it in the background.
- `DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh` prints no `fail` line that the Step 0 baseline lacks, and prints its
  `summary:` line
- `git diff --quiet 6d81294 -- renovate.json5` (unchanged)
</verification>

<success_criteria>
- WR-01, WR-02, WR-03 and IN-01 each have their fix, their tests and their own commit
- The SUMMARY records the planner findings: the WR-01 siblings (mise, claude-code) that were fixed with them; that the
  registry-port reference is misread by the manager rather than unmatched, so the parity is "accepted implies read
  correctly"; the single always-true IN-01 reason; and the WR-02 observation, with any deviation from the review
- The SUMMARY lists, for the orchestrator: record WR-01, WR-02, WR-03 and IN-01 as fixed in `02-REVIEW-DISPOSITION.md`
  with the commit hashes; the open WR-02 key-form decision (ADR in the phase that first puts an ID-keyed map in a
  contract) as a candidate for a STATE.md pending todo; and the governed files the cleanup merge prompt will list
  (`scripts/doctor.sh`, `scripts/governance/ask-governed-merge.sh`, `scripts/governance/test-ask-governed-merge.sh`,
  `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt`,
  `build-logic/build.gradle.kts`)
</success_criteria>

<output>
Create `.planning/quick/261124-oyk-fix-phase-2-review-findings-wr-01-wr-02-/261124-oyk-SUMMARY.md` when done
</output>
