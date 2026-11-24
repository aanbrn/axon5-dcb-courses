---
phase: quick-261124-uwn
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - scripts/governance/ask-governed-edit.sh
  - scripts/governance/test-ask-governed-edit.sh
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckHooksFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ClaudeSettingsFixtures.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - scripts/doctor.sh
  - scripts/test-doctor.sh
  - docs/upstream/0012-docker-containerd-platform-export.md
  - docs/tooling.md
  - docs/adr/0008-container-images-with-buildpacks.md
autonomous: true
requirements: [AIDEV-08, CICD-08, CICD-01, AIDEV-04, TOOL-03, TOOL-04, INFRA-09, UPS-02]

estimate:
  tokens: 130000
  raw_tokens: 130000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "CI Lint: `bash scripts/governance/test-ask-governed-edit.sh` passes every case, with the test's temp root resolved physically (the path shape of the Linux runner), and the case `a different case spelling of an agent worktree directory asks` gets a reason that names `.../.claude/worktrees/AGENT-A`, the root as the path spells it. Before the hook change the same run reproduces the CI failure of run 45419305916 (reason naming `.../agent-a`)"
    - "The edit hook still asks for every path it asked for before (fail-closed unchanged); only the root named in the reason changes. New cases that reach the repository through a symbolic link as CLAUDE_PROJECT_DIR pass on macOS and Linux alike: the `AGENT-A` spelling asks, a governed file in the main checkout asks, a governed file inside agent worktree agent-a stays silent"
    - "Qodana: `scripts/qodana.sh` on the final committed HEAD reports 0 problems, with no suppression, no baseline and no change to qodana.yaml; the four strings use multi-dollar interpolation and the rejected-sample list is built with a collection builder"
    - "WR-01: a `.mcp.json` server that has no `command` (url-only HTTP or SSE server) gives `ok  mcp:` and no `warn  mcp:` line, and doctor never prints a server URL; a `mise x --` server without a binary argument is resolved as the `mise` command; an unresolved stdio binary still warns with its real name"
    - "IN-01: a missing `mise.toml` gives the `git restore mise.toml` fix and an unreadable one gives the `chmod u+r mise.toml` fix, each with its own test"
    - "IN-02: `checkImageReferences` lists its findings by path and then numeric line number, so a finding on line 3 comes before one on line 12 of the same file; every message text is unchanged"
    - "UP-0012: after a verification on a throwaway Docker engine of 29.9.0 or newer with the containerd image store, the two UP-0012 workaround markers are gone from the tracked tree, the `image-store` and `colima` checks accept both image stores, the record has status `workaround-removed` with the observed verification result and the residual, `checkUpstreamRecords` is green, and doctor prints `ok  upstream-removal:` with no UP-0012 warning"
    - "All gates are green: `mise x -- ./gradlew build`, all seven shell suites under `scripts/`, `scripts/qodana.sh` with 0 problems, and doctor without a new `fail` line"
  artifacts:
    - path: "scripts/governance/ask-governed-edit.sh"
      provides: "check_candidate passes the root as spelled in the candidate path to match_relative, so the reason names the path as written on every file system"
      contains: "${candidate:0:${#root}}"
    - path: "scripts/governance/test-ask-governed-edit.sh"
      provides: "physically resolved temp root (Linux path shape on macOS) and cases through a symbolic link to the repository as CLAUDE_PROJECT_DIR"
      contains: "pwd -P"
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt"
      provides: "findings held with path and line number and sorted by both before rendering; KDoc states the order"
      contains: "sortedWith"
    - path: "scripts/doctor.sh"
      provides: "check_mcp that never splits on empty tab fields, check_mise that separates a missing from an unreadable mise.toml, image-store and colima checks that accept both image stores without UP-0012 markers"
      contains: "chmod u+r mise.toml"
    - path: "scripts/test-doctor.sh"
      provides: "cases for a url-only MCP server, a `mise x --` server without binary, an unreadable mise.toml, and both image stores accepted"
      contains: "url"
    - path: "docs/upstream/0012-docker-containerd-platform-export.md"
      provides: "record in status workaround-removed with the 2026-11-24 verification evidence, decision and residual"
      contains: "workaround-removed"
  key_links:
    - from: "scripts/governance/test-ask-governed-edit.sh"
      to: "scripts/governance/ask-governed-edit.sh"
      via: "run_case pipes a PreToolUse JSON into the real hook under env -i with CLAUDE_PROJECT_DIR and matches permissionDecisionReason with jq test()"
      pattern: "run_case"
    - from: ".github/workflows/ci.yml"
      to: "scripts/governance/test-ask-governed-edit.sh"
      via: "the Lint job step `Run shell tests` runs every tracked scripts/test-*.sh and scripts/*/test-*.sh with xargs -n1 bash"
      pattern: "Run shell tests"
    - from: "scripts/doctor.sh"
      to: "docs/upstream/0012-docker-containerd-platform-export.md"
      via: "check_upstream_removal warns for every fixed-upstream record whose markers remain; a workaround-removed record with no marker reports ok"
      pattern: "check_upstream_removal"
    - from: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt"
      to: "docs/upstream/0012-docker-containerd-platform-export.md"
      via: "checkUpstreamRecords: a workaround-removed record must have no marker and keep all six sections"
      pattern: "workaround-removed"
---

<objective>
Make the Phase 2 pull request #21 green and close the open review findings, in one agent worktree, with one commit per
fix so that the review disposition can cite one hash per finding:

1. CI Lint failure (run 45419305916, job Lint, step "Run shell tests"): the edit-hook test case "a different case
   spelling of an agent worktree directory asks" fails on Linux only.
2. The 5 Qodana findings of the same run (profile Default, failThreshold 0), fixed without suppression or baseline.
3. Review findings of `.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md`: WR-01 (`check_mcp`
   misparses a url-only server), IN-01 (`mise.toml` fix text), IN-02 (catalog findings sorted as strings).
4. The UP-0012 workaround removal (author decision), with the record updated per ADR-0013.

Root cause of item 1, established at planning time (6e267b3) and verified on macOS: it is not the case sensitivity of
the file system. On macOS the test's temp directory sits under `/var/folders/...`, a symbolic link to `/private/var`,
while `git worktree list` reports the resolved `/private/var/...` paths. The lexical candidate `.../AGENT-A/mise.toml`
therefore matches no worktree root and reaches the hook's `.claude/worktrees/<name>` loop, whose reason names the root
as the path spells it (`AGENT-A`). On Linux `/tmp` has no symbolic link, so the same candidate first matches the
git-reported root `.../agent-a` through the case-folded prefix test, and `match_relative` names that root (`agent-a`).
The hook asks on both systems (fail-closed holds); only the reason text differs. Resolving the test's temp root with
`cd -P`/`pwd -P` reproduces the exact CI failure line on macOS, and passing `${candidate:0:${#root}}` (the root as the
candidate spells it) to `match_relative` makes all cases pass on both temp-root shapes (both measured in a scratch
copy). This follows the hook's own stated rule that only the comparison folds case and a reason shows the path as
written.

Governed paths touched (governance/governed-paths.txt, ADR-0015): `scripts/governance/**`,
`build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**` and `scripts/doctor.sh`. Edit them only
with the Edit and Write tools, never with sed, shell redirection or `spotlessApply`; inside the agent worktree the
edit hook stays silent and the merge hook asks the author once at cleanup. Use the file tools for the other files too.
Out of scope for the executor: `.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md` and
`.planning/STATE.md` (the orchestrator writes and commits both, see `<orchestrator_notes>`), `qodana.yaml`,
`governance/governed-paths.txt`, `.claude/**`, `.github/**`, `renovate.json5`, `gradle/libs.versions.toml`,
`docs/adr/0011-local-runtime-and-doctor.md` (its UP-0012 line only lists related records and stays true).

Purpose: AIDEV-08 (the governance edit hook behaves the same on every platform and stays fail-closed), CICD-08 (shell
tests green in CI Lint), CICD-01 (static analysis gate green), AIDEV-04 and TOOL-03 (doctor reports MCP servers and
mise.toml correctly), INFRA-09 (deterministic, file-ordered image findings), UPS-02 and TOOL-04 (the UP-0012 record and
the documented engine setup match the code).
Output: platform-independent hook reasons with tests; Qodana-clean build-logic; doctor fixes with tests; the UP-0012
workaround removed with recorded evidence.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md
@docs/adr/0013-upstream-records-and-workaround-markers.md
@docs/upstream/0012-docker-containerd-platform-export.md

Read only the parts named (line numbers verified at 6e267b3):
- scripts/governance/ask-governed-edit.sh: header 1-37 (case-folding rule 35-36), `match_relative` 98-105 (reason text
  102), `check_candidate` 250-274 (the two `match_relative` calls 261 and 270).
- scripts/governance/test-ask-governed-edit.sh: temp dir 11-12, fixture setup 22-63, `check` and `run_case` 80-116,
  agent-worktree attempts 176-207 (the AGENT-A cases 201-205), path spellings from 209.
- build-logic/src/functionalTest/.../governance/AskRulesCheckHooksFunctionalTest.kt 124-133;
  ClaudeSettingsFixtures.kt 26-34; build-logic/src/main/.../governance/GovernanceHooks.kt 25-42;
  ImageReferenceCheckFunctionalTest.kt 228-272 (Qodana site at 240, the read-elsewhere test 249-271), `catalog` 295,
  companion samples 330-383. Multi-dollar strings are already idiomatic here (ImageReferenceCheckFunctionalTest.kt 34,
  ImageReferenceCheck.kt `composeProblems`).
- build-logic/src/main/.../governance/ImageReferenceCheck.kt: class KDoc 19-43 (order sentence 42), `check()` 60-80
  (`problems.sorted()` 72), `composeProblems` 82-90, `catalogProblems` 96-111, `readOutsideEntries` 118-126,
  `catalogProblem` 129-150, `failureMessage` 159-169.
- scripts/doctor.sh: `check_mise` 97-125 (the read 104-110), `check_image_store` 224-243 (first marker 224),
  `COLIMA_AWK` 246-271, `check_colima` 276-318 (second marker 301), `check_gsd` pin branch (the missing versus
  unreadable split to mirror; find it with grep for `chmod u+r governance/gsd-version`), `check_mcp` 496-533,
  `check_upstream_removal` 709-733.
- scripts/test-doctor.sh: `fresh`, `make_unreadable`, `skip_unreadable` 210-230, the mise section 295-341 (the
  `no mise.toml` case 332-340), MCP 468-489, colima and image store 536-578, upstream-removal 673-745.
- docs/tooling.md 119-136 (the colima bullet on the image store 132-133); docs/adr/0008-container-images-with-buildpacks.md
  89-93, 115-116, 149-153.

Baselines at 6e267b3: test-ask-governed-edit.sh `62 passed, 0 failed` (macOS; Linux CI 61/1); test-doctor.sh
`246 passed, 0 failed`; real doctor (DOCTOR_SKIP_GRADLE=1, main checkout) `summary: 12 ok, 4 warn, 0 fail`, with
`warn  upstream-removal: UP-0012 ... scripts/doctor.sh:224, scripts/doctor.sh:301`; the local engine is colima with
Docker server 29.5.2 on the classic store (`overlay2`), Docker client 29.10.0 (UPDATE before dispatch: the author
upgraded colima's engine to Docker server 29.10.0, still the classic store `overlay2`); `docker:29.10.0-dind` exists on Docker
Hub (manifest resolved at planning time).
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: the edit hook names the root as written, so its test passes on Linux CI and macOS alike (CI Lint)</name>
  <files>scripts/governance/test-ask-governed-edit.sh, scripts/governance/ask-governed-edit.sh</files>
  <behavior>
    - With the temp root resolved physically, the run before the hook change prints exactly the CI failure:
      `FAIL - a different case spelling of an agent worktree directory asks`, reason naming `.../.claude/worktrees/agent-a`; every other case passes.
    - After the hook change that case passes with a reason naming `.../.claude/worktrees/AGENT-A`; the sibling
      "a leading `**/` entry under a different case spelling of an agent worktree asks" still passes.
    - New, with `ln -s "$REPO" "$TMP/repo-link"` and `CLAUDE_PROJECT_DIR="$TMP/repo-link"`:
      an Edit of `$TMP/repo-link/.claude/worktrees/AGENT-A/mise.toml` asks with reason pattern
      `mise\.toml in .*/repo-link/\.claude/worktrees/AGENT-A is a governed path`;
      an Edit of `$TMP/repo-link/mise.toml` asks with `mise\.toml in .*/repo-link is a governed path`;
      an Edit of `$TMP/repo-link/.claude/worktrees/agent-a/mise.toml` with that worktree as cwd is silent (`none`).
      These keep, on every platform, the symbolic-link-root coverage that macOS's `/var` link used to give by accident.
    - Every case that asked before still asks (fail-closed unchanged); the suite ends with at least `65 passed, 0 failed`.
  </behavior>
  <action>
    RED. In scripts/governance/test-ask-governed-edit.sh (governed; Edit tool) replace line 11 so the temp directory is
    created with mktemp, the EXIT trap removes that created path, and TMP is then that path resolved with `cd -P -- ...
    && pwd -P`. Add a comment that says why: the runner's `/tmp` has no symbolic link while macOS's `/var/folders` does,
    and `git worktree list` reports resolved paths, so an unresolved root would test macOS and Linux on different path
    shapes. Run `bash scripts/governance/test-ask-governed-edit.sh` and confirm the single failure of `<behavior>`
    (exit status non-zero, reason names `agent-a`). Do not commit the red state on its own.

    GREEN. In scripts/governance/ask-governed-edit.sh (governed; Edit tool), in `check_candidate`, pass the root as the
    candidate spells it to `match_relative` at both call sites (261 and 270): the second argument becomes
    `${candidate:0:${#root}}` instead of `$root`. The lengths are equal because the prefix test already matched in folded
    ASCII case under `LC_ALL=C`, so this changes only the reason text, never whether the hook asks. Update the comments:
    `match_relative` takes "<root as written in the path>"; the `check_candidate` comment says that the relative path and
    the root the reason names both keep the spelling of the candidate, so the reason is the same whichever root matches
    first and on every file system; the header sentence on case folding (35-36) adds that a reason shows the path and its
    root as written. Then add the three symbolic-link cases of `<behavior>` (create the link next to the other fixture
    links, put the cases at the end of the "attempts to pass as an agent worktree" section, name each after what it
    proves). Run the suite again: all pass.

    Optional cross-check on Linux, not a gate: `alpine:3.22` is in the local image store, but `apk add bash git jq` had
    no network inside the sandbox at planning time. If an unsandboxed run is approved, run the suite in that container
    from a copy of `scripts/governance`; otherwise the CI Lint job is the Linux confirmation after the push.

    Commit both files together (plain git commands, one per call): `fix(governance): name the root as written in the
    edit hook's ask reason`, body: the Linux-only failure of run 45419305916, the `/var` versus `/private/var` root
    cause, that the decision (ask) never differed, and that the test now runs on the resolved temp root with explicit
    symbolic-link-root cases.
  </action>
  <verify>
    <automated>bash scripts/governance/test-ask-governed-edit.sh && bash scripts/governance/test-ask-governed-merge.sh && mise x -- shellcheck scripts/governance/ask-governed-edit.sh scripts/governance/test-ask-governed-edit.sh</automated>
  </verify>
  <done>The edit-hook suite passes with at least 65 cases on the resolved temp root, the red run reproduced the CI line before the fix, the merge-hook suite still passes, shellcheck is clean, and one commit holds the change.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: build-logic is Qodana-clean and image findings are listed in file order (Qodana x5, IN-02)</name>
  <files>build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckHooksFunctionalTest.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ClaudeSettingsFixtures.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt</files>
  <behavior>
    - Qodana sites, each producing the identical string value: AskRulesCheckHooksFunctionalTest.kt 126 and 130,
      ClaudeSettingsFixtures.kt 32 and GovernanceHooks.kt 39 become multi-dollar literals; ImageReferenceCheckFunctionalTest.kt
      240 iterates a list built with `buildList` (all of REJECTED_KEYS, then DOTTED_KEY, then all of REJECTED_SEPARATORS).
      Every existing AskRulesCheck and ImageReferenceCheck functional test still passes unchanged.
    - IN-02 new functional test: a catalog whose Renovate-read-elsewhere lines (reuse READ_ELSEWHERE samples, for example a
      commented-out image entry) sit on a single-digit line and on a line of 10 or more in the same file (pad with plain
      `[versions]` entries); the failure output lists the single-digit `gradle/libs.versions.toml:<n>:` finding before
      the double-digit one (compare `indexOf`). It fails before the fix (lexical order puts `:12:` before `:3:`).
  </behavior>
  <action>
    Qodana first (all inspections of profile Default must stay enabled; no `@Suppress`, no baseline, no qodana.yaml edit).
    Convert each of the four string literals to the multi-dollar form the IDE quick fix produces: prefix `$$`, write the
    literal `${CLAUDE_PROJECT_DIR}` placeholder with a bare dollar instead of the backslash-escaped one, and write each real
    interpolation with two dollars (for example the `MERGE_HOOK` and `script` references). GovernanceHooks.kt is
    governed (Edit tool). At ImageReferenceCheckFunctionalTest.kt 240 replace the `+` concatenation with a `buildList`
    using `addAll` and `add` in the same order. Keep ktfmt layout by hand; `spotlessCheck` must pass. Commit:
    `style(build-logic): fix the five Qodana findings of the PR 21 run` (body lists the five sites and the inspections:
    "Multi-dollar interpolation can be used in string literals" x4, "Collection concatenation can be converted to
    collection builder" x1).

    IN-02 (ImageReferenceCheck.kt is governed; Edit tool). RED: add the functional test of `<behavior>` next to the
    read-elsewhere test and run it to see it fail. GREEN: hold each finding as a small private data class with the path,
    the line number and the rendered message; `composeProblems`, `catalogProblems`, `readOutsideEntries` and
    `catalogProblem` fill it (a `catalogProblem` finding, whose message has no line, still carries its line number for
    ordering only). In `check()` sort with `sortedWith(compareBy(path, line, message))` and render the messages, so the
    report file and `failureMessage` get file order while every message text stays byte-identical. Update the class KDoc
    sentence at 42 to say the findings are sorted by path and then line number, so the report is identical on every run
    and reads top to bottom. Commit: `fix(build-logic): list image reference findings by path and line number (IN-02)`.
  </action>
  <verify>
    <automated>! git grep -q -F '\${CLAUDE_PROJECT_DIR}' -- build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckHooksFunctionalTest.kt build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ClaudeSettingsFixtures.kt build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt && mise x -- ./gradlew :build-logic:check checkImageReferences checkClaudeAskRules spotlessCheck</automated>
    Grep gate baseline: escaped placeholder count in the three files [HEAD=4 EXPECT==0]. `:build-logic:check` reruns the
    whole functionalTest suite; if it outlasts the 10-minute foreground limit, run it in the background.
  </verify>
  <done>Two commits (Qodana sites, IN-02); the new ordering test failed before and passes after; `:build-logic:check`, `checkImageReferences`, `checkClaudeAskRules` and `spotlessCheck` succeed.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: doctor reads url-only MCP servers and mise.toml correctly, and the UP-0012 workaround is removed (WR-01, IN-01, UP-0012)</name>
  <files>scripts/doctor.sh, scripts/test-doctor.sh, docs/upstream/0012-docker-containerd-platform-export.md, docs/tooling.md, docs/adr/0008-container-images-with-buildpacks.md</files>
  <precondition>`docker info` answers through the colima context (needed only for the UP-0012 verification step).</precondition>
  <behavior>
    - WR-01, MCP section of test-doctor.sh: `.mcp.json` with the fixture's stdio servers plus
      `"remote": {"type": "http", "url": "https://example.invalid/mcp?token=secret-value"}`: exit 0, `^ok  mcp: `, no
      `^warn  mcp:`, no `secret-value` anywhere in the output. A file with only that url-only server: `^ok  mcp: `, no
      warn. A server `{"command": "mise", "args": ["x", "--"]}`: resolved as the `mise` command, `^ok  mcp: `. The existing
      "unresolved MCP binary" case still warns naming `context7-mcp`.
    - IN-01, mise section: the existing `no mise.toml` case keeps the fix line matching `git restore mise\.toml`; a new
      case with `make_unreadable "$ENV/repo/mise.toml"` (skip line when root can read it) gives exit 1, a `^fail  mise: `
      line, a fix line matching `chmod u\+r mise\.toml`, no `git restore` fix line, `^ok  tools: ` and `^summary: `.
    - UP-0012, colima and image-store section: the "colima with the containerd snapshotter" case now gives `^ok  colima: `
      and no `^warn  colima:`; the "containerd image store" case gives `^ok  image-store: the containerd image store is
      active` and no `^warn  image-store:`; the healthy tree keeps `^ok  image-store: ` for the classic store; the
      "colima without dns" case still warns.
  </behavior>
  <action>
    WR-01 (doctor.sh governed; Edit tool). RED: add the `<behavior>` MCP cases and see them fail (the url-only server is
    reported as an unresolved binary named `path`). GREEN: in `check_mcp`, stop relying on empty tab-separated fields,
    which `IFS=$'\t' read` collapses because tab is IFS whitespace. Keep `@tsv` (it escapes tabs and newlines inside a
    key or command) and make every emitted field non-empty, or switch to a non-whitespace delimiter while keeping the
    escaping; either way: a server without `command` is counted but not resolved (the ok line may say how many have
    nothing to resolve), its `url` is never emitted or printed; the `mise x|exec -- <binary>` branch applies only when
    the third argument is a string, otherwise the server resolves `mise` on PATH (the review's fix). Keep the shape check
    and its warn unchanged. Commit: `fix(doctor): resolve url-only MCP servers without misreading empty fields (WR-01)`.

    IN-01 (doctor.sh). RED: add the unreadable mise.toml case. GREEN: in `check_mise`, when the read fails, report a
    missing file with the existing `restore it: git restore mise.toml` fix and an existing but unreadable file with
    `chmod u+r mise.toml`, mirroring the missing versus unreadable split of `check_gsd` for the pin (same wording style).
    Update the comment above the read. Commit: `fix(doctor): tell a missing mise.toml from an unreadable one (IN-01)`.

    UP-0012, step 1, verification before any edit (it gates the removal). Per the record's "How to verify removal", on a
    throwaway engine of 29.9.0 or newer with the containerd image store: run `docker:29.10.0-dind` detached, privileged,
    with `--rm`, a fixed name such as `up0012-fixed`, and no volume or host mount; poll `docker exec <name> docker info`
    for up to 60 s; record `docker exec <name> docker version --format '{{.Server.Version}} {{.Server.Arch}}'` and
    `docker exec <name> docker info --format '{{.Driver}}'`, which must print `overlayfs` (Docker 29 uses the containerd
    store for a fresh data root). Inside it run the two commands of the record's minimal reproduction for the engine's
    platform (linux/arm64 on this machine): `docker pull --platform linux/arm64 paketobuildpacks/ubuntu-noble-run:latest`,
    then `docker save --platform linux/arm64 ... -o /tmp/run.tar` followed by `tar tf /tmp/run.tar | wc -l`. Pass means
    exit 0 and a non-zero entry count. As an informative control, repeat the same steps on `docker:29.5.2-dind` (the
    local engine's version) and record whether it fails with `no suitable export target`; a control that does not fail
    is recorded as such (a fresh engine may not trigger the shared-layer condition) and does not block. Stop both
    containers and remove the two dind images afterwards. If the sandbox refuses the docker socket or the pulls, request
    an unsandboxed run for these docker commands. Never change the colima configuration or the local engine.
    If the fixed engine cannot be run, is older than 29.9.0, does not report `overlayfs`, or the save fails: do not
    make the UP-0012 edits below; record the evidence in the SUMMARY under "UP-0012 blocked" and finish the remaining
    verification, so the orchestrator can take it to the author.

    UP-0012, step 2, removal (per the author's decision of 2026-11-24 and the record's removal procedure). RED: change the
    colima and image-store test expectations of `<behavior>`. GREEN in doctor.sh: delete both UP-0012 marker comment lines
    (224 and 301); `check_image_store` reports `ok` for both stores and names the one that is active (classic, or
    containerd with its driver), with a function comment that states both stores work and the containerd store needs
    Docker 29.9.0 or newer (docs/tooling.md); `check_colima` and `COLIMA_AWK` no longer read or judge
    `docker.features.containerd-snapshotter`, and the colima ok line names only the DNS servers; the UP-0011 DNS marker
    and logic stay as they are. Grep doctor.sh and test-doctor.sh for `snapshotter` and the header comments for the image
    store so no stale requirement remains. docs/tooling.md 132-133: replace the bullet with: either image store works;
    the classic store is the one this repository was verified on; the containerd store needs Docker Engine 29.9.0 or
    newer, because older engines cannot export the Paketo noble run image (link `upstream/0012-docker-containerd-platform-export.md`);
    `doctor` reports which store is active. ADR-0008: at 89-93 replace "the workaround stays until ..." and the CI runner
    sentence with the 2026-11-24 removal, the 29.9.0 requirement for the containerd store and that the CI image job
    (CICD-02) needs such an engine when its runner uses the containerd store; at 115-116 the `colima` check covers DNS
    (UP-0011) and the `image-store` check reports the active store; at 151-153 UP-0012 is "workaround removed" and only
    UP-0011 is left to let a doctor check go. The quick start (docs/quick-start.md) never mentions the image store, so
    it needs no change; say so in the SUMMARY.
    The record (docs/upstream/0012-docker-containerd-platform-export.md), keeping all six sections and the front matter
    keys: `status: "workaround-removed"`, `updated: "2026-11-24"`, `workaround: true` and `product-version` unchanged.
    Symptom: replace "so the workaround stays until ..." with a pointer to the removal. Workaround: past tense, what the
    two doctor checks did and that quick task 261124-uwn removed them on 2026-11-24, with what the checks do now.
    Removal criteria: keep the original criterion and add the decision: the author removed the workaround on 2026-11-24
    on the strength of the released fix and the verification below; the author's colima engine was upgraded to Docker
    29.10.0 on 2026-11-24 (classic store, which the defect never affected), so the developer engine meets the criterion; no CI job builds an
    image yet (CICD-02); residual: an engine older than 29.9.0 with the containerd store still fails, and
    docs/tooling.md states the requirement. How to verify removal: keep the procedure and add a dated "Result" paragraph
    with the observed versions, driver, entry count and the control outcome, exactly as measured.
    Commit: `chore(upstream): remove the UP-0012 image-store workaround`, body with the verification evidence.
  </action>
  <verify>
    <automated>bash scripts/test-doctor.sh && mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh && ! git grep -q 'UPSTREAM: UP-0012' -- . ':!docs/upstream' ':!.planning' ':!.claude' && grep -q '^status: "workaround-removed"' docs/upstream/0012-docker-containerd-platform-export.md && mise x -- ./gradlew checkUpstreamRecords spotlessCheck</automated>
    Grep gate baselines: marker lines outside docs/upstream, .planning and .claude [HEAD=2 EXPECT==0]; record status
    `workaround-removed` [HEAD=0 EXPECT==1]. When UP-0012 is blocked by step 1, only the first two commands apply and
    the SUMMARY says why.
  </verify>
  <done>Three commits (WR-01, IN-01, UP-0012) or two plus a recorded "UP-0012 blocked"; test-doctor.sh ends with more than 246 passed and 0 failed; shellcheck clean; no UP-0012 marker remains; checkUpstreamRecords and spotlessCheck pass.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| agent tool call to the governance edit hook | the hook decides whether an Edit/Write of a governed path needs the author; a path spelling (case, `..`, symbolic link) must never turn an ask into silence |
| `.mcp.json` content to doctor output | server entries may carry URLs with credentials; doctor output reaches terminals and CI logs |
| upstream record to readers and to doctor | a record that claims a verification that did not happen misleads every later removal or re-test |
| throwaway privileged container on the author's machine | the UP-0012 verification runs a privileged dind container inside the colima VM |
| governed files to the main checkout | edits made in the agent worktree reach the main checkout only through the merge the merge hook asks about |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-uwn-01 | Elevation of privilege | `check_candidate` in scripts/governance/ask-governed-edit.sh | high | mitigate | Task 1 changes only the second argument of `match_relative` (reason text); the prefix tests and exemptions are untouched; every pre-existing ask case still asks on the resolved temp root, and three new symbolic-link-root cases pin ask and exemption on both platforms |
| T-uwn-02 | Spoofing | agent-worktree exemption against a case variant (`AGENT-A`) | medium | mitigate | Task 1: the case variant asks on Linux and macOS, directly and through a symbolic link to the repository; exemption stays byte-for-byte |
| T-uwn-03 | Information disclosure | `check_mcp` in scripts/doctor.sh | medium | mitigate | Task 3 never emits or prints a server `url`; a test with `?token=secret-value` asserts the value is absent from the output |
| T-uwn-04 | Denial of service | `check_mcp`, `check_mise` under `set -euo pipefail` | low | mitigate | Task 3: url-only and argument-less servers parse without field shifts; missing and unreadable mise.toml each report a fix and reach the summary (tests assert `^summary: `) |
| T-uwn-05 | Repudiation | docs/upstream/0012-docker-containerd-platform-export.md | medium | mitigate | Task 3 step 1 gates the removal on an observed run on Docker 29.9.0 or newer with `overlayfs`; the record holds only measured versions, driver, entry count and the control result, plus the residual; a failed or impossible run blocks the removal and is reported |
| T-uwn-06 | Elevation of privilege | privileged `docker:29.10.0-dind` and `docker:29.5.2-dind` containers | low | accept | Version-pinned official images, `--rm`, no host mount or volume, run inside the colima VM, stopped and their images removed after the check; the colima configuration and the local engine are never changed |
| T-uwn-07 | Tampering | governed files (`scripts/governance/**`, build-logic governance sources, `scripts/doctor.sh`) | low | mitigate | Edit and Write tools only, never shell writes or `spotlessApply`; the merge hook lists them for the author at cleanup |
| T-uwn-08 | Tampering | Qodana gate | low | mitigate | Task 2 fixes the code; no suppression, baseline, profile or `failThreshold` change; `scripts/qodana.sh` runs on the final committed HEAD |
| T-uwn-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package-manager install and no dependency change in this plan, so the package legitimacy gate has nothing to audit; the dind images are pulled by version tag for a one-off check and never committed |
</threat_model>

<verification>
Run from the checkout root after all tasks and commits:
- `git ls-files -z -- 'scripts/test-*.sh' 'scripts/*/test-*.sh' | xargs -0 -r -n1 bash` exits 0 (the CI Lint
  command; seven suites, each ending `0 failed`)
- `mise x -- ./gradlew build` succeeds (run it in the background; it outlasts the foreground limit)
- `scripts/qodana.sh` (no argument: it analyses the committed HEAD of this worktree) exits 0 with 0 problems; run it in
  the background, about ten minutes with a warm cache. If it reports anything, fix the code, commit and rerun.
- `DOCTOR_SKIP_GRADLE=1 bash scripts/doctor.sh` in the worktree prints `ok  upstream-removal:`, no
  `upstream-removal: UP-0012` warning and no `fail` line except `fail  gsd:` for the GSD installation that a worktree
  lacks (untracked files). The exit-0 run of the real doctor is the orchestrator's check in the main checkout after
  the merge.
- `git diff --quiet 6e267b3 -- qodana.yaml governance/governed-paths.txt renovate.json5 gradle/libs.versions.toml .github`
</verification>

<success_criteria>
- Six commits (hook; Qodana; IN-02; WR-01; IN-01; UP-0012), each one fix, or five plus a "UP-0012 blocked" report
- The SUMMARY records: the reproduced CI failure line and the root cause (macOS `/var` symbolic link, not case
  sensitivity); the edit-hook and doctor test counts; the Qodana result with the analysed commit; the UP-0012
  verification evidence (both engines, driver, entry count) or the blocker; that docs/quick-start.md needed no change;
  the governed files the cleanup merge prompt will list
- The SUMMARY lists for the orchestrator the hashes for the disposition rows of WR-01, IN-01 and IN-02
</success_criteria>

<orchestrator_notes>
Resolved before dispatch: the author upgraded the colima engine to Docker server 29.10.0 (classic store), so the
removal criterion holds for the developer engine; the throwaway dind verification still runs because it is the only
way to exercise the containerd image store without changing the colima configuration. Record the local engine
version (verify with `docker version`) in the UP-0012 result paragraph.

After the merge, the orchestrator (not the executor) commits these planning edits:
1. `.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md`: set WR-01, IN-01 and IN-02 to
   `fixed` in the front matter list and in the table, Source cells in the existing form, e.g.
   `02-REVIEW.md — fixed by quick task 261124-uwn in <hash> (<one-line fix>)`, with the hashes from the SUMMARY.
2. `.planning/STATE.md` Pending Todos: replace the "[Renovate, from 2026-11-27]" entry with: "[Renovate, from
   2026-11-28]: Merge the open Renovate PRs by hand (automerge is off for them). #19 now updates `npm:renovate` to
   44.257.9 (published 2026-11-21, so the 7-day npm cooldown, CICD-04, ends 2026-11-28) and `uv` to 0.13.9; it needs
   the `governance-approved` label, because the content of the renovate sidecar `aube-lock.yaml` changes, not only
   versions. #20 lock file maintenance has every check green except `renovate/stability-days`. Plan: merge both after
   PR #21 merges (Renovate rebases them); if Phase 2 runs past 2026-11-28, merge them earlier and then merge main into
   the phase branch." Add the quick task row to "Quick Tasks Completed".
3. In the main checkout after the merge: `scripts/doctor.sh` exits 0 with no `fail` line and `ok  upstream-removal:`.
</orchestrator_notes>

<output>
Create `.planning/quick/261124-uwn-fix-the-phase-2-pr-21-ci-failures-linux-/261124-uwn-SUMMARY.md` when done
</output>
