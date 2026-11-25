---
phase: quick-261124-nlk
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - scripts/doctor.sh
  - scripts/test-doctor.sh
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - docs/tooling.md
  - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
  - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
autonomous: true
requirements: [TOOL-03, AIDEV-08, INFRA-09, INFRA-06]

estimate:
  tokens: 90000
  raw_tokens: 90000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "IN-08: on a developer machine `scripts/doctor.sh` prints `ok  git: <version> meets the minimum 2.36.0 ...` for git 2.36.0 or newer, `fail  git: <version> is older than 2.36.0 ...` with a `fix: brew install git ...` line and exit 1 for an older git, and `fail  git: cannot read the version from 'git --version'` with a fix line and exit 1 for an unparsable version; `--ci` reports `skip  git: local-only check, skipped by --ci`"
    - "IN-08: the real `scripts/doctor.sh` on the author's machine (git 2.54.0) gains an `ok  git:` line and no new `fail` line"
    - "IN-08: docs/tooling.md says the minimum git release is enforced by `scripts/doctor.sh` without writing a version number (its own rule: versions are never written in that document), and lists the git check among the script's checks and among the `--ci` skips; ADR-0015 states that the hooks need git 2.36 or newer and why, and that doctor checks it"
    - "IN-06: an indented `image-*` catalog entry (spaces or a tab, also under an indented `[versions]` header) that is not pinned fails `checkImageReferences` naming its key, and an indented pinned entry passes; the class KDoc and the `CATALOG_ENTRY` KDoc say that a catalog line is trimmed before it is matched, so any indentation is checked"
    - "IN-07: the `CoursesJsonModule` KDoc states that its types are registered as values, not as map keys, and that the first contract with an ID-keyed map adds a key deserializer (`addKeyDeserializer`) through the same parse; no code line changes"
    - "All gates are green: `bash scripts/test-doctor.sh` (>=155 passed, 0 failed), shellcheck on both scripts, and `mise x -- ./gradlew :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :build-logic:detekt :serialization-jackson:check checkImageReferences checkToolingInventory checkAdrs spotlessCheck`"
  artifacts:
    - path: "scripts/doctor.sh"
      provides: "check_git (local only): git present, version parsable, at least GIT_MIN_VERSION"
      contains: "GIT_MIN_VERSION"
    - path: "scripts/test-doctor.sh"
      provides: "A fake git that answers --version from FAKE_DIR/git-version and forwards everything else to the real git; cases for healthy, old, minimum, unparsable and --ci"
      contains: "git-version"
    - path: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt"
      provides: "Two TestKit cases for indented catalog entries (unpinned fails, pinned passes)"
      contains: "indented"
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt"
      provides: "KDoc that records the trimming of catalog lines; no logic change"
      contains: "indent"
    - path: "docs/tooling.md"
      provides: "git row and doctor section mention the minimum git release enforced by doctor"
      contains: "the git release the governance hooks need"
    - path: "docs/adr/0015-governance-hooks-for-worktrees-and-merges.md"
      provides: "Consequence and confirmation entries for the git minimum"
      contains: "2.36"
    - path: "backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt"
      provides: "KDoc: value-only registration, key deserializer added with the first ID-keyed map"
      contains: "addKeyDeserializer"
  key_links:
    - from: "scripts/test-doctor.sh"
      to: "scripts/doctor.sh"
      via: "run_doctor runs the copy of doctor.sh with PATH=$dir/bin:$SYSBIN, so the fake $dir/bin/git shadows the real git symlink in SYSBIN"
      pattern: "run_doctor"
    - from: "scripts/doctor.sh"
      to: "docs/adr/0015-governance-hooks-for-worktrees-and-merges.md"
      via: "GIT_MIN_VERSION comment names ADR-0015 and the two git calls of scripts/governance/ask-governed-edit.sh (lines 184 and 191) that need it"
      pattern: "GIT_MIN_VERSION"
    - from: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt"
      to: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt"
      via: "fixture.run/runAndFail(checkImageReferences) on a real catalog; catalogProblems trims each line (line 87) before CATALOG_ENTRY matches"
      pattern: "checkImageReferences"
---

<objective>
Close three info findings of the Phase 2 review (`git show 03358fd:.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md`,
sections IN-06, IN-07, IN-08), within the author-approved scope.

Planner findings and decisions (Claude's discretion inside the approved scope):
- IN-06 is a false positive at the regex level, verified at 6ffd531: `catalogProblems` in `ImageReferenceCheck.kt`
  trims every catalog line (`for (line in lines.map { it.trim() })`, line 87) before `CATALOG_ENTRY` is matched, and the
  same trim lets an indented `[versions]` header through `line.startsWith("[")`. Leading blanks (spaces and tabs) are
  therefore already allowed. The author's intent (an indented unpinned entry fails, an indented pinned one passes) is
  delivered by two TestKit cases that prove it and guard the trim against a later refactoring, plus KDoc that states the
  trim, because the anchored regex read on its own is what misled the reviewer. Adding `^\s*` to the regex would
  re-handle what the caller already stripped and is not done. Fallback, recorded as a deviation in the SUMMARY: only if
  the new unpinned-indented case does NOT fail the task, apply the review's regex `^\s*(["']?)(image-[A-Za-z0-9_.-]+)\1\s*=\s*(.*)$`.
- IN-06 sibling check, nothing to change: `ComposeVersions.images` reads the parsed catalog (`catalog.versionAliases`),
  not text, and `ComposeVersionsFunctionalTest.IMAGE_ENTRY` already allows `^[ \t]*` indentation.
- IN-08: the doctor check is named `git`, is local only (skipped by `--ci`) and fails (not warns) per the author. Local
  only, because the one consumer of the minimum is the Claude Code governance hook pair (ADR-0015), which runs only on
  developer machines; CI never runs the hooks and its Ubuntu runners ship a far newer git. This mirrors `claude-code`
  (AIDEV-04). Missing git, an old git and an unparsable version all fail with a fix line. The test cases are the three
  the author named (old, new, unparsable) plus the exact minimum as the "new" boundary and the `--ci` skip; a missing-git
  case is not added because the test harness links the real git into SYSBIN for its own repository setup.
- IN-08 docs: docs/tooling.md line 18 says "Versions are never written in this document", so the git row follows the
  Claude Code row's wording ("the minimum release ... is enforced by `scripts/doctor.sh`") instead of writing 2.36;
  ADR-0015 states the number and the reason. ADR-0011 and the comment in `.github/workflows/ci.yml` (line 245) already
  list the doctor checks non-exhaustively (both omit upstream-removal) and stay unchanged; `.github/**` is governed.
- IN-07: KDoc only, no behaviour change; the statement covers every type the module registers (IDs and `RejectionCode`),
  with the ID-keyed map as the named case.

The executor runs in an agent worktree. `scripts/doctor.sh` and
`build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**` are governed paths
(governance/governed-paths.txt): edit them with the Edit and Write tools only, never with shell writes (`sed -i`,
redirects, heredocs) and never through `spotlessApply`; when `spotlessCheck` reports a diff in a governed file, apply
that diff by hand with Edit. docs/adr/**, docs/tooling.md, the test scripts and the functional tests are not governed,
but use the file tools there too. Out of scope: `02-REVIEW-DISPOSITION.md` and STATE.md (the orchestrator records the
fixes with the commit hashes), `.claude/settings.json`, `governance/governed-paths.txt`, `.github/**`, the hooks under
`scripts/governance/`.

Purpose: TOOL-03 (doctor explains how to fix the environment), AIDEV-08 (the governance hooks work as ADR-0015
describes), INFRA-09 (every catalog image is pinned), INFRA-06 (the serialization convention is documented truthfully).
Output: a `git` doctor check with tests, two TestKit cases and clarified KDoc for the image check, updated tooling doc
and ADR-0015, and a clarified `CoursesJsonModule` KDoc.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md

Read only the parts named (line numbers verified at 6ffd531):
- scripts/doctor.sh: header and usage text 1-25 (the "Which checks live where" list at 7-17 and the `--ci` list at
  20-21), `report` 50-69, `has` 71-73, `local_only` 75-84, `check_mise` 96-122 (fail on an unparsable version),
  `CLAUDE_CODE_MIN_VERSION` and `check_claude_code` 544-567 (the model for the new check: readonly constant, awk on line
  1, `sort -V` comparison), `main` 710-731.
- scripts/test-doctor.sh: header 1-23 (SYSBIN links the real git), `make_env` 36-91 (the fakes read their answers from
  files under `$FAKE_DIR`; `claude-version` at 83-88 is the model), `run_doctor` and the `assert_*` helpers 133-180,
  the healthy local-state block 274-288, the Claude Code cases 459-471, the `--ci` block 566-589.
- scripts/governance/ask-governed-edit.sh lines 184 and 191: the `rev-parse --path-format=absolute` (git 2.31) and
  `worktree list --porcelain -z` (git 2.36) calls that set the minimum.
- build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt: class KDoc 19-34,
  `catalogProblems` 83-95, `CATALOG_ENTRY` and its KDoc 141-145.
- build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt:
  whole file (143 lines; 8 tests; `catalog()` and `services()` helpers 130-134, constants 136-142).
- docs/tooling.md: line 18 (no versions rule), the git row 46 and the Claude Code row 51 (wording model), the
  Environment check section 142-163.
- docs/adr/0015-governance-hooks-for-worktrees-and-merges.md: `### Consequences` 156-182 and `### Confirmation` 223-242.
- backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt: KDoc 17-26
  and `typedId` 38-41.

Baselines at 6ffd531: `bash scripts/test-doctor.sh` ends with `145 passed, 0 failed` (about 40 s);
ImageReferenceCheckFunctionalTest has 8 tests; the machine's git is `git version 2.54.0 (Apple Git-157)`.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: doctor `git` check (IN-08), end to end from the shell tests to the real doctor run</name>
  <files>scripts/doctor.sh, scripts/test-doctor.sh</files>
  <behavior>
    - Healthy tree (fake git prints `git version 2.47.1 (Apple Git-154)`): output has `^ok  git: 2\.47\.1 meets the minimum 2\.36\.0`, exit 0, and the healthy `assert_no_out` regex also covers `git`.
    - Old git (`git version 2.35.8`): exit 1, `^fail  git: 2\.35\.8 is older than 2\.36\.0`, and `^      fix: brew install git`.
    - The minimum itself (`git version 2.36.0`): exit 0, `^ok  git: 2\.36\.0 meets the minimum 2\.36\.0`.
    - Unparsable (`git version unknown`): exit 1, `^fail  git: cannot read the version from 'git --version'`, and `^      fix: brew install git`.
    - `--ci`: `^skip  git: ` (add `git` to the existing skip loop at line 577).
  </behavior>
  <action>
    Step 0, before any edit: record the real doctor's fail lines in the worktree as the baseline, because a worktree lacks
    the untracked GSD installation and some local checks fail there already: run
    `DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh 2>&1 | grep -E '^fail' | sort > "${TMPDIR:-/tmp}/261124-nlk-doctor-before.txt"`
    (doctor may exit 1; that is expected). The Gradle half (`./gradlew doctor`) is untouched by this plan and is skipped
    for a deterministic comparison.

    RED, scripts/test-doctor.sh (not governed; use Edit): add the real git path once near the top
    (`REAL_GIT=$(command -v git)`). In `make_env`, following the `claude-version` fake: write
    `git version 2.47.1 (Apple Git-154)` to `$dir/git-version` and the real git path to `$dir/real-git`, and create
    `$dir/bin/git` from a quoted heredoc: when the first argument is `--version` it prints `$FAKE_DIR/git-version`
    verbatim, otherwise it execs the path read from `$FAKE_DIR/real-git` with all arguments (doctor's claude-tracking and
    upstream-removal checks call `git -C ...` and must keep working). Add it to the `chmod +x` line. Reading the path
    from a file keeps the fake a quoted heredoc like the others (no SC2016). Then add the cases of `<behavior>`: the
    `ok  git:` assertion and `git` in the `assert_no_out` alternation of the healthy block (274-288); a new section
    `# --- the git version the governance hooks need ---` after the Claude Code cases with the old, minimum and
    unparsable cases (each `fresh`, then write the version line to `$ENV/git-version`); `git` in the `--ci` skip loop.
    Run `bash scripts/test-doctor.sh` and confirm the new assertions fail.

    GREEN, scripts/doctor.sh (governed; Edit tool only), modelled on `check_claude_code`:
    a readonly `GIT_MIN_VERSION=2.36.0` with a comment saying it is the git release the governance hooks need
    (ADR-0015): `git worktree list --porcelain -z` arrived in 2.36 and `git rev-parse --path-format=absolute` in 2.31,
    both called by scripts/governance/ask-governed-edit.sh, and on an older git the edit hook fails closed and asks for
    every edit; docs/tooling.md points here instead of repeating the number.
    A function `check_git` with the comment style of the other checks ("git (local only, AIDEV-08): ..."), starting with
    `local_only git && return`. When `has git` is false: `report fail git "the git CLI is not on PATH"` with a fix that
    names `brew install git` and the package manager. Read the version as the third field of line 1 of
    `git --version` when fields 1 and 2 are `git` and `version` (awk), and treat it as unparsable unless it matches
    `^[0-9]+\.[0-9]+` (bash `=~`); unparsable reports fail `cannot read the version from 'git --version'` with a fix that
    starts with `brew install git`. Compare with `sort -V` exactly like `check_claude_code`; ok prints
    `<installed> meets the minimum $GIT_MIN_VERSION that the governance hooks need`, fail prints
    `<installed> is older than $GIT_MIN_VERSION, which the governance hooks need (ADR-0015)` with the fix
    `brew install git (Homebrew's git must come before /usr/bin on PATH), or upgrade git with your package manager`.
    Call `check_git` in `main` right after `check_claude_code`. Update the header: in the "Which checks live where"
    list add "the git version the governance hooks need" after "the Claude Code version", and the `--ci` usage line
    becomes "(gsd, gsd-defaults, mcp, platform-token, colima, claude-code and git)". Keep lines within 120 columns.
    Run `bash scripts/test-doctor.sh` until it ends `>= 155 passed, 0 failed` (145 + 10 new assertions).

    End to end: run the real doctor again
    (`DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh 2>&1 | grep -E '^fail' | sort | comm -13 "${TMPDIR:-/tmp}/261124-nlk-doctor-before.txt" -`
    must print nothing) and confirm it prints `ok  git: 2.54.0 meets the minimum 2.36.0 ...`; with `--ci` it prints
    `skip  git:`. Run shellcheck on both scripts (the lefthook pre-commit hook runs it on staged `*.sh` too).
  </action>
  <verify>
    <automated>bash scripts/test-doctor.sh | tail -n 1 | awk '{exit !($1 >= 155 && $3 == 0)}'  # [HEAD=145 EXPECT>=155]</automated>
    <automated>DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh 2>&1 | grep -cE '^ok  git: [0-9.]+ meets the minimum 2\.36\.0'  # [HEAD=0 EXPECT==1]</automated>
    <automated>DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh --ci 2>&1 | grep -cE '^skip  git: '  # [HEAD=0 EXPECT==1]</automated>
    <automated>test -z "$(DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh 2>&1 | grep -E '^fail' | sort | comm -13 "${TMPDIR:-/tmp}/261124-nlk-doctor-before.txt" -)"  # [no new fail line versus the Step 0 baseline]</automated>
    <automated>grep -v '^ *#' scripts/doctor.sh | grep -c 'GIT_MIN_VERSION'  # [HEAD=0 EXPECT>=2]</automated>
    <automated>grep -v '^ *#' scripts/test-doctor.sh | grep -c 'git-version'  # [HEAD=0 EXPECT>=4]</automated>
    <automated>mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh</automated>
  </verify>
  <done>
    `scripts/doctor.sh` has a local-only `git` check that fails with a fix for a missing, old or unparsable git and is ok
    from 2.36.0; `scripts/test-doctor.sh` proves healthy, old, minimum, unparsable and `--ci` with a fake git that forwards
    every other call to the real one, ending `>= 155 passed, 0 failed`; the real doctor prints `ok  git: 2.54.0 ...` and
    no new fail line; shellcheck is clean.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: prove that indented catalog image entries are checked, and say so in the KDoc (IN-06)</name>
  <files>build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt</files>
  <behavior>
    - Case A, `an indented catalog image that is not pinned fails naming the key, also under an indented header`: compose file `services($$"    image: ${AXONSERVER_IMAGE}")`; catalog written literally (not through `catalog()`) as `  [versions]`, `  junit = "6.0.3"`, `  image-axonserver = "$AXONSERVER"`, `  image-spaces = "postgres:18.7"`, and a tab-indented `image-tab = 'postgres:18.7@sha256:$DIGEST'`. `runAndFail(TASK)` output contains `gradle/libs.versions.toml: image-spaces must be <name>:<tag>@sha256:<64 hex digits>, but is 'postgres:18.7'` and `image-tab must be a double-quoted <name>:<tag>@sha256:<64 hex digits>`, and does not contain `image-axonserver must`.
    - Case B, `an indented pinned catalog image passes`: `catalog("  image-axonserver = \"$AXONSERVER\"", "\timage-postgres = \"postgres:18.7@sha256:$DIGEST\"")`; `run(TASK)` outcome `shouldBeIn GradleFixture.PASSED`.
  </behavior>
  <action>
    Add the two cases of `<behavior>` to ImageReferenceCheckFunctionalTest (not governed; Edit tool), after the case
    `a catalog image that is not a double-quoted string fails naming the key, and a quoted key is checked`, in the style
    of the existing cases (Kotest `shouldContain` / `shouldNotContain` / `shouldBeIn`, the existing `TASK`, `CATALOG`,
    `INFRA`, `DIGEST`, `AXONSERVER` constants). Use only string literals and the existing constants, no numeric literals
    (detekt's magic-number rule runs over `src/functionalTest/kotlin` too, build-logic/build.gradle.kts line 28).
    Run the class. Expected per the objective: both cases pass at once, because `catalogProblems` trims every line
    before `CATALOG_ENTRY` matches. If Case A does not fail the task, apply the review's regex from the objective's
    fallback and record it as a deviation; if Gradle itself rejects the indented `[versions]` header while loading the
    catalog (it is valid TOML, so this is not expected), keep the header unindented in Case A and record why.

    Then clarify the KDoc of ImageReferenceCheck.kt (governed; Edit tool only), changing comment lines only: in the class
    KDoc, "Every such key is matched, bare or quoted," becomes a sentence that says every such key is matched bare or
    quoted and at any indentation, because each catalog line is trimmed before it is matched; the `CATALOG_ENTRY` KDoc
    says it is matched against a catalog line whose leading and trailing blanks `catalogProblems` has already removed,
    so the anchor does not skip an indented entry. Use backticks, not KDoc links, for the private `catalogProblems`.
    No regex, no logic and no import changes (unless the fallback triggered). Re-wrap the edited KDoc within 120 columns
    as ktfmt would; if `spotlessCheck` reports a diff, apply it by hand with Edit.
  </action>
  <verify>
    <automated>mise x -- ./gradlew :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :build-logic:detekt checkImageReferences spotlessCheck</automated>
    <automated>sed -nE 's/.* tests="([0-9]+)" skipped="0" failures="0" errors="0".*/\1/p' build-logic/build/test-results/functionalTest/TEST-io.github.aanbrn.courses.buildlogic.governance.ImageReferenceCheckFunctionalTest.xml  # [HEAD=8 EXPECT==10]</automated>
    <automated>grep -c '@Test' build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt  # [HEAD=8 EXPECT==10]</automated>
    <automated>grep -ci 'indent' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt  # [HEAD=0 EXPECT>=1]</automated>
    <automated>d=$(git diff -U0 6ffd531 -- build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt) && test "$(printf '%s\n' "$d" | grep -E '^[+-]' | grep -vE '^(\+\+\+|---) ' | grep -cvE '^[+-][[:space:]]*\*')" -eq 0  # [HEAD=0 EXPECT==0 @6ffd531 GUARD MUTATION: a regex, logic or import change in ImageReferenceCheck.kt (only KDoc lines may change unless the recorded fallback triggered)]</automated>
  </verify>
  <done>
    ImageReferenceCheckFunctionalTest has 10 passing tests, two of them proving that an indented unpinned entry fails
    naming its key (spaces, tab, indented header) and an indented pinned entry passes; ImageReferenceCheck.kt changes
    only in KDoc, which now states the trim; `:build-logic:detekt`, `checkImageReferences` and `spotlessCheck` pass.
  </done>
</task>

<task type="auto">
  <name>Task 3: document the git minimum (IN-08) and the value-only ID registration (IN-07)</name>
  <files>docs/tooling.md, docs/adr/0015-governance-hooks-for-worktrees-and-merges.md, backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt</files>
  <action>
    <!-- planner-discipline-allow: 2.36 -->
    docs/tooling.md (IN-08): do not write any version number here (line 18). In the git row (line 46) the Installation
    cell becomes "Host package manager (host prerequisite); the minimum release the governance hooks need is enforced by
    `scripts/doctor.sh`", the same wording pattern as the Claude Code row; keep the table aligned (pad the cell so the
    column width and the row's `|` positions stay as they are; the cell stays shorter than the widest Installation cell,
    the GSD row). In the Environment check section, insert the exact phrase "the git release the governance hooks need"
    into the script's list after "the Claude Code release" (line 152) and into the `--ci` skip list (line 162, which then
    ends "colima, the Claude Code release and the git release the governance hooks need."). Re-wrap those bullets by
    hand within 120 columns (Prettier runs with `proseWrap: preserve`, so it will not re-wrap them).
    `checkToolingInventory` compares only the mise tools, so it stays green; run it anyway.

    docs/adr/0015-governance-hooks-for-worktrees-and-merges.md (IN-08, not governed): under `### Consequences` add a
    bullet "Bad, because the hooks need git 2.36 or newer: the edit hook calls `git worktree list --porcelain -z` (2.36)
    and `git rev-parse --path-format=absolute` (2.31). On an older git both calls fail, the hook fails closed and asks
    for every edit with a reason that names git rather than the version, so `scripts/doctor.sh` checks the git version
    on developer machines (its `git` check, skipped by `--ci`), and docs/tooling.md points to that check." (wording may
    be tightened, facts must stay). Under `### Confirmation` add a bullet that `scripts/test-doctor.sh` covers the `git`
    check of `scripts/doctor.sh` with an old version, the minimum and an unparsable version, plus the `--ci` skip.
    Keep lines within 120 columns; front matter unchanged; `checkAdrs` must stay green.

    CoursesJsonModule.kt (IN-07, KDoc only): extend the class KDoc with a paragraph stating that these types are
    registered as values only, not as map keys: a map keyed by an ID would be written with the ID's `toString()` as the
    key, but the module has no key deserializer to read it back, so the first contract with an ID-keyed map adds one
    with `addKeyDeserializer` for the ID type, parsing through the same [TypedIdCompanion.parse] and reporting a rejected
    key through the context's `weirdKeyException`. No code line, import or signature changes; Dokka's failOnWarning
    applies to this module, so keep the only KDoc link to the already-linked public [TypedIdCompanion.parse] and write
    other names in backticks.
  </action>
  <verify>
    <automated>mise x -- ./gradlew :serialization-jackson:check checkToolingInventory checkAdrs spotlessCheck</automated>
    <automated>grep -cE '^\| git .*scripts/doctor\.sh' docs/tooling.md  # [HEAD=0 EXPECT==1]</automated>
    <automated>tr '\n' ' ' < docs/tooling.md | tr -s ' ' | awk -v p='the git release the governance hooks need' '{ print gsub(p, "") }'  # [HEAD=0 EXPECT==2]</automated>
    <automated>grep -c '2\.36' docs/tooling.md  # [HEAD=0 EXPECT==0 @6ffd531 GUARD MUTATION: writing the git minimum version number into docs/tooling.md, which line 18 forbids]</automated>
    <automated>grep -v '^#' docs/adr/0015-governance-hooks-for-worktrees-and-merges.md | grep -c '2\.36'  # [HEAD=0 EXPECT>=1]</automated>
    <automated>grep -v '^#' docs/adr/0015-governance-hooks-for-worktrees-and-merges.md | grep -c 'test-doctor'  # [HEAD=0 EXPECT>=1]</automated>
    <automated>grep -c 'addKeyDeserializer' backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt  # [HEAD=0 EXPECT>=1]</automated>
    <automated>d=$(git diff -U0 6ffd531 -- backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt) && test "$(printf '%s\n' "$d" | grep -E '^[+-]' | grep -vE '^(\+\+\+|---) ' | grep -cvE '^[+-][[:space:]]*\*')" -eq 0  # [HEAD=0 EXPECT==0 @6ffd531 GUARD MUTATION: a code, import or signature change in CoursesJsonModule.kt (IN-07 is KDoc only)]</automated>
  </verify>
  <done>
    docs/tooling.md names doctor as the enforcer of the minimum git release in the git row and lists the git check in
    both doctor lists, with no version number; ADR-0015 records the 2.36 requirement, its reason, the doctor check and
    its tests; the CoursesJsonModule KDoc states the value-only registration and the `addKeyDeserializer` follow-up, with
    no code change; `:serialization-jackson:check`, `checkToolingInventory`, `checkAdrs` and `spotlessCheck` pass.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| host git to doctor output | doctor parses `git --version` from whatever git is first on PATH and prints a verdict and a fix command the developer copies |
| governance hooks to developer | an old git makes the edit hook fail closed; without a diagnosis the developer may be tempted to disable the hooks |
| catalog text to image check | `checkImageReferences` decides from raw TOML lines whether an image is pinned; a skipped line lets an unpinned image reach compose/versions.env |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-nlk-01 | Denial of service | edit hook on git older than 2.36 (prompts on every edit, reason names git not the version) | low | mitigate | Task 1: local-only `git` doctor check with a fix line; Task 3: minimum recorded in ADR-0015 and pointed to from docs/tooling.md |
| T-nlk-02 | Tampering | `check_git` parsing of `git --version` | low | mitigate | Task 1: version taken only from field 3 of line 1 after `git version`, validated with `^[0-9]+\.[0-9]+` before `sort -V`; unparsable fails; the raw text is never built into a fix command (fix lines are constants) |
| T-nlk-03 | Tampering | `catalogProblems` / `CATALOG_ENTRY` on an indented `image-*` entry | low | mitigate | Task 2: TestKit cases prove indented unpinned entries fail and pinned ones pass, guarding the trim at line 87 against a refactoring |
| T-nlk-04 | Repudiation | `CoursesJsonModule` KDoc claims more than the module does (ID-keyed maps) | low | mitigate | Task 3: KDoc states value-only registration and the key-deserializer follow-up |
| T-nlk-05 | Elevation of privilege | governed files (`scripts/doctor.sh`, build-logic governance sources) changed outside the file tools | low | mitigate | Executor edits them with Edit/Write only, never `spotlessApply` or shell writes; the merge hook lists them at cleanup |
| T-nlk-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package-manager install and no dependency change in this plan, so the package legitimacy gate has nothing to audit |
</threat_model>

<verification>
Run from the checkout root after all tasks:
- `mise x -- ./gradlew :build-logic:functionalTest --tests '*ImageReferenceCheckFunctionalTest*' :build-logic:detekt :serialization-jackson:check checkImageReferences checkToolingInventory checkAdrs spotlessCheck` succeeds
- `bash scripts/test-doctor.sh` ends with at least `155 passed, 0 failed`
- `DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh` prints `ok  git: 2.54.0 meets the minimum 2.36.0 ...` and no `fail` line that the Step 0 baseline lacks; `--ci` prints `skip  git:`
- `mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh` is clean
</verification>

<success_criteria>
- IN-06, IN-07 and IN-08 each have their fix and evidence; the SUMMARY states that IN-06 was a false positive at the
  regex level (line 87 trims) and that the TestKit cases now guard it, or records the fallback if it triggered
- ComposeVersions and ComposeVersionsFunctionalTest unchanged, with the reason in the SUMMARY (parsed catalog;
  `IMAGE_ENTRY` already allows indentation)
- docs/tooling.md holds no version number; ADR-0011 and `.github/workflows/ci.yml` unchanged, with the reason in the SUMMARY
- SUMMARY lists, for the orchestrator: record IN-06, IN-07 and IN-08 in `02-REVIEW-DISPOSITION.md` with the commit
  hashes; the merge prompt at cleanup will list `scripts/doctor.sh` and
  `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt` as governed files
</success_criteria>

<output>
Create `.planning/quick/261124-nlk-fix-phase-02-review-findings-in-06-and-i/261124-nlk-SUMMARY.md` when done
</output>
