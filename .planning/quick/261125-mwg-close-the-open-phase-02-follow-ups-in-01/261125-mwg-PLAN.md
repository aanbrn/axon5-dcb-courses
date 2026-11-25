---
phase: quick-261125-mwg
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - docs/upstream/0011-colima-aaaa-dns.md
  - docs/adr/0008-container-images-with-buildpacks.md
  - docs/tooling.md
  - scripts/doctor.sh
  - .github/workflows/codeql.yml
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt
  - docs/upstream/0006-empty-platform-token.md
  - docs/upstream/0013-axon-server-dcb-context-flag.md
  - docs/templates/adr.md
  - docs/templates/upstream-record.md
  - .github/actions/maven-central-mirror/action.yml
  - .github/workflows/ci.yml
  - docs/adr/0016-ci-dependency-downloads.md
  - renovate.json5
  - lefthook.yml
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt
  - governance/governed-paths.txt
  - .claude/settings.json
autonomous: true
requirements: [UPS-01, UPS-02, TOOL-03, CICD-01, CICD-04, CICD-08, TEST-09, INFRA-12]

estimate:
  tokens: 160000
  raw_tokens: 160000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "IN-01: no tracked file outside .planning and .claude states the colima AAAA stall as an established fact. The UP-0011 title and its issue-draft title, ADR-0008, docs/tooling.md and the UP-0011 marker and comment in scripts/doctor.sh say it was observed once on a profile without explicit resolvers and was not reproduced on a clean profile. UP-0011 keeps `status: \"open\"` and `workaround: true`, its Root cause section records the author's assessment (a nuance of the local DNS settings, untested), and the `colima` doctor check, its warning text and the `UPSTREAM: UP-0011` marker stay"
    - "IN-03: the UP-0010 marker comment at .github/workflows/codeql.yml line 32 says the extractor found no sources in this Kotlin 2.4.21 Gradle build, with an Isolated Projects interaction suspected. Only comment lines of codeql.yml differ from BASE"
    - "The shared front matter parser accepts a list continued on indented lines in both shapes Prettier 3.10.1 writes (`key:` followed by an indented `[`, and `key: [` with the items on the next lines), with a trailing comma before `]`. Everything else still fails closed with `<file>:<line>: unsupported front matter: <reason>`: a comment inside a list, a blank or unindented line while a list is open, text after `]`, an empty item, an indented line after a closed value, and `key:` followed by anything that is not an indented `[`"
    - "docs/upstream/0006 and docs/upstream/0013 carry no prettier-ignore line and hold the multi-line `upstream` shape Prettier writes. `spotlessCheck` and `checkUpstreamRecords` pass on them, and the generated upstream index still lists their links"
    - "IN-04a: each of the five Gradle jobs of ci.yml (build, integration-test, functional-test, compose-smoke, doctor) calls the local composite action `./.github/actions/maven-central-mirror` at the position of the old step, after setup-gradle. The action installs the same file to the same `init.d` path. The lint job's zizmor step and the pre-commit zizmor job also audit `.github/actions`, and actionlint and zizmor are clean"
    - "IN-04b: renovate.json5 holds no hand-copied preset list. The exclusion is `gradle: { ignorePaths: [\"scripts/ci/**\"] }` at manager scope, so every other manager keeps `:ignoreModulesAndTests` as Renovate maintains it. A local `renovate --platform=local --dry-run=extract` matches the same file count per manager as at BASE, except github-actions, which gains the new action.yml. It extracts nothing from scripts/ci and lists no mirror URL"
    - "Spotless never formats an untracked file that git ignores (for example `.playwright-mcp/**`), but it always formats a tracked file, even one that .gitignore matches. Outside the root of a git work tree, only the static exclusions apply. A new ignored `build/` directory does not invalidate the configuration cache"
    - "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt is a governed path with exactly one `Edit(/...)` ask rule, and `checkClaudeAskRules` passes"
    - "Gates are green on the final HEAD: every CI lint-job step run locally, `spotlessCheck`, `checkUpstreamRecords`, `checkAdrs`, `checkClaudeAskRules`, `checkBuildScripts`, `checkToolingInventory` and `:build-logic:check` (detekt, Dokka and the full TestKit suite)"
  artifacts:
    - path: ".github/actions/maven-central-mirror/action.yml"
      provides: "the one definition of the CI-only mirror install step, a composite action with a single bash step"
      contains: "using: composite"
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt"
      provides: "the ValueSource that lists the untracked paths git ignores, as Ant exclude patterns, and falls back to an empty list outside a work tree root"
      contains: "ls-files"
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt"
      provides: "front matter lists continued on indented lines, with a trailing comma"
      contains: "Items"
    - path: "docs/upstream/0011-colima-aaaa-dns.md"
      provides: "a title and an issue draft worded as observed once, plus the author's assessment in Root cause"
      contains: "not reproduced on a clean profile"
    - path: "renovate.json5"
      provides: "a manager-scoped exclusion of scripts/ci for the gradle manager"
      contains: "gradle: { ignorePaths"
  key_links:
    - from: ".github/workflows/ci.yml"
      to: ".github/actions/maven-central-mirror/action.yml"
      via: "`uses: ./.github/actions/maven-central-mirror` in five jobs, each after setup-gradle"
      pattern: "uses: \\./\\.github/actions/maven-central-mirror"
    - from: ".github/actions/maven-central-mirror/action.yml"
      to: "scripts/ci/maven-central-mirror.init.gradle.kts"
      via: "install into `${GRADLE_USER_HOME:-$HOME/.gradle}/init.d/`"
      pattern: "maven-central-mirror\\.init\\.gradle\\.kts"
    - from: "build-logic/src/main/kotlin/courses.root.gradle.kts"
      to: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt"
      via: "providers.of(GitIgnoredPaths::class.java), whose patterns every Spotless target excludes"
      pattern: "GitIgnoredPaths"
    - from: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt"
      to: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt"
      via: "FrontMatter.parse (AdrCheck uses the same parser)"
      pattern: "FrontMatter\\.parse"
    - from: "governance/governed-paths.txt"
      to: ".claude/settings.json"
      via: "checkClaudeAskRules: exactly one Edit(/<entry>) ask rule per governed entry"
      pattern: "GitIgnoredPaths\\.kt"
---

<objective>
Close the open Phase 02 follow-ups. These are IN-01, IN-03 and IN-04 of the incremental review of 2026-11-25
(`.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md`), plus two build-logic follow-ups. The first
teaches the front matter parser the multi-line list that Prettier writes, so that the two `# prettier-ignore` lines of
plans 02-28 and 02-29 can go. The second makes Spotless skip git-ignored files, because the git-ignored
`.playwright-mcp/` of every Playwright MCP session fails `spotlessCheck`.

Tracer-first is not applied. The five items are independent maintenance fixes that share no layer, so a thin
end-to-end slice would add no information. The tasks are grouped by area: upstream records, CI and Renovate, Spotless.

Execution context: the executor runs on the branch `gsd/phase-2-review-followups`, which is created from `main` after
PR #21 (Phase 2, head 97ea3ad) is squash-merged. The work happens in one agent worktree. Every line number below was
verified at 97ea3ad, whose content `main` will hold. Before the first edit, record the start commit as `BASE` with
`BASE=$(git merge-base HEAD main)` and reuse it in every gate. Do not push.

Decisions taken in this plan:
- IN-01 wording. Reword the claim to "observed once on a profile without explicit resolvers, not reproduced on a clean
  profile, cause not established". The workaround, the `colima` doctor check, its warning text (`network.dns is empty,
  so the default DNS forwarder can stall image pulls`, asserted by scripts/test-doctor.sh) and the record status
  `open` stay. Add the author's assessment (a nuance of the local DNS settings) to the Root cause section as an
  untested assessment. Reword the issue-draft title inside the record too, because it repeats the claim. The tracked
  spike output under `.claude/skills/spike-findings-axon5-dcb-courses/` is a historical record of spike 005 and stays
  as written. ADR-0013 line 84 ("withheld until ... re-verified") stays true and is not touched.
- IN-04b, Renovate. Per docs.renovatebot.com configuration-options, `ignorePaths` is `mergeable: false` and has the
  scopes `repo` and `manager`. The docs (nuget page) show the manager-scoped form. So there is no additive top-level
  form, but a manager-level list replaces the inherited list for that one manager only. The hand-copied list already
  drifted: the `:ignoreModulesAndTests` preset that Renovate 44.262.1 documents adds `**/.mise/locks/**` and
  `**/.config/mise/locks/**`, and the copy, which matches the mise-pinned 44.252.8, lacks both. Decision: remove the
  top-level `ignorePaths` and add `gradle: { ignorePaths: ["scripts/ci/**"] }`.
  The planner proved this in a scratch clone with the pinned Renovate 44.252.8 (`--platform=local
  --dry-run=extract`):
  - At HEAD: gradle matches 38 files, nothing from scripts/ci, and no mirror URL.
  - With the manager-scoped form: identical match counts for every manager, nothing from scripts/ci, and no mirror URL.
  - Negative control without any exclusion: gradle matches 39 files, `scripts/ci/maven-central-mirror.init.gradle.kts`
    is extracted, and the mirror URL appears 37 times in registryUrls.

  One semantic difference: the gradle manager no longer applies the preset's test, example and vendor directories.
  Today no gradle-manager file lives under them (`git ls-files` check at 97ea3ad). The repository's own
  `.mise/locks/**` packageRule (renovate.json5 lines 124-128) stays.
- IN-04a, the composite action lives at `.github/actions/maven-central-mirror/action.yml`, named after the init script.
  `.github/**` is already governed (governance/governed-paths.txt, `.github/**`, and the `Edit(/.github/**)` ask rule
  in `.claude/settings.json` line 39), so no new entry is needed. zizmor in CI currently reads only
  `.github/workflows`. The lint step and the pre-commit job are extended to `.github/actions`, so that the new file
  stays audited (CICD-08). That is a lint-scope addition, not a build behaviour change.
- Spotless. Spotless skips the untracked paths that git ignores, derived from git rather than from a second
  hand-kept list, so that later ignored outputs (`test-results/`, `coverage/`, `.vscode/settings.json`) cannot break
  it either. Only untracked paths are skipped, so an edit to `.gitignore` can never drop a tracked file from a gate.
  That keeps `.gitignore` outside the governance boundary. The new `GitIgnoredPaths.kt` decides which files Spotless
  sees, and an edit to it alone could empty the Spotless file sets. By the boundary rule of
  governance/governed-paths.txt it therefore becomes a governed entry with its ask rule.

Governed paths (ADR-0015): `.github/**` (ci.yml, codeql.yml and the new action.yml), `renovate.json5`,
`scripts/doctor.sh`, `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**`,
`build-logic/src/main/kotlin/courses.root.gradle.kts`, `governance/**`, `.claude/settings.json` and, once its entry
exists, `GitIgnoredPaths.kt`. Create and change them with the Edit and Write tools only, never with sed, shell
redirection or `spotlessApply`. Never run `spotlessApply` at all in this plan: it rewrites every unformatted file it
finds, governed ones included. Fix formatting from the diff that `spotlessCheck` prints, with the Edit tool. Inside the
agent worktree the edit hook stays silent; at cleanup, the merge hook asks the author once, listing the governed files.
Out of scope for the executor: `.planning/STATE.md`, this PLAN, and
`.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md` (see `<orchestrator_notes>`).

Purpose: UPS-01/UPS-02 (accurate records and markers, a parser for the record format Prettier writes), TOOL-03
(doctor's marker text), CICD-01/CICD-08 (one definition of the CI mirror step, linted), CICD-04 (a Renovate
exclusion that cannot drift from its preset), TEST-09/INFRA-12 (formatting gates that see only what CI sees,
proven by TestKit).
Output: six commits (IN-01, IN-03, parser, IN-04a, IN-04b, Spotless), plus any gate-fix commits, and a SUMMARY.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md

Read only the parts named (line numbers at 97ea3ad):
- docs/upstream/0011-colima-aaaa-dns.md: front matter 1-10, title 12, Root cause 64-75, draft title 118.
- docs/adr/0008-container-images-with-buildpacks.md: 82-90 (Local prerequisites) and 110.
- docs/tooling.md: 133-134. scripts/doctor.sh: 299-301 and the warning at 320-321 (unchanged).
- .github/workflows/codeql.yml: 28-45. docs/upstream/0010-codeql-kotlin-extractor.md: Analysis section.
- build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt: the whole file, 193 lines.
- build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt:
  247-314 (the subset failure cases and the comments case) and 470-507 (DEFAULTS and the `record` helper).
- docs/upstream/0006-empty-platform-token.md and docs/upstream/0013-axon-server-dcb-context-flag.md: lines 1-11.
- docs/templates/adr.md line 14 and docs/templates/upstream-record.md line 16 (the front matter subset sentence).
- .github/workflows/ci.yml: header 9-19, the five mirror steps 78-82, 132-136, 197-201, 243-247 and 390-394, and the
  zizmor step 307-310. lefthook.yml: 33-35. docs/adr/0016-ci-dependency-downloads.md: 46-62.
- renovate.json5: 1-28 and 120-128.
- build-logic/src/main/kotlin/courses.root.gradle.kts: the whole file, 230 lines.
- build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ExecutableOnPath.kt: the whole file (the ValueSource
  pattern and why programs are launched by absolute path).
- build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt: 1-40 and
  143-155. GradleFixture.kt: 95-175 (runner flags and `standard`).
- governance/governed-paths.txt: 72-74 (the `.gitignore` bullet) and 222-226 (the build job's formatters).
  .claude/settings.json: 70-80.
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Upstream records and markers: reword UP-0011 (IN-01) and the UP-0010 marker (IN-03), and parse multi-line front matter lists</name>
  <precondition>The worktree base holds the Phase 2 content: docs/upstream/0011-colima-aaaa-dns.md and build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt exist, and `grep -c 'UPSTREAM: UP-0011' scripts/doctor.sh` prints 1.</precondition>
  <files>docs/upstream/0011-colima-aaaa-dns.md, docs/adr/0008-container-images-with-buildpacks.md, docs/tooling.md, scripts/doctor.sh, .github/workflows/codeql.yml, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt, docs/upstream/0006-empty-platform-token.md, docs/upstream/0013-axon-server-dcb-context-flag.md, docs/templates/adr.md, docs/templates/upstream-record.md</files>
  <behavior>
    - Accepted: `upstream:` then an indented `[`, items one per line each ending with a comma, then an indented `]`.
      This is exactly what Prettier 3.10.1 writes for a list longer than 120 columns. The record passes, and the
      upstream index row lists the links in order.
    - Accepted: `upstream: [` with items on the following indented lines and an indented `]`. This is Prettier's shape
      when a list holds a comment, which the parser itself rejects, so the test uses the shape without the comment.
    - Accepted: one trailing comma before `]` in a one-line list, as YAML 1.2 allows.
    - Rejected, each case showing the substring it expects: an open multi-line list ended by the closing `---` gives
      `unterminated list` on the key's line. A whole-line comment or a trailing comment inside an open list gives
      `comments inside a list are not supported`. A one-line list whose bare item starts with `#` is rejected the same
      way (today it is wrongly accepted as an item). `] trailing` gives `unexpected text after the value`. `[a,,]`
      gives `empty list item`. The existing cases keep their messages: `nested` still gives
      "`upstream` has no value", `indented` still gives "indented line", `unterminated list` still applies.
  </behavior>
  <action>
    Part A, IN-01 (one commit). Change every file with the Edit tool. Keep prose lines at 120 columns or less.
    - docs/upstream/0011-colima-aaaa-dns.md:
      - Line 12 becomes
        `# UP-0011: A colima profile without explicit resolvers once stalled Go DNS lookups for about 10 s, not reproduced on a clean profile`.
        Keep the `UP-0011: ` prefix that checkUpstreamRecords reads.
      - Retitle the issue draft at line 118 neutrally, for example
        `# Go DNS lookups in containers took about 10 seconds on a colima profile without explicit resolvers (AAAA suspected)`.
      - Root cause: keep "Not established." as its first words, and add one sentence. It records the author's
        assessment of 2026-11-25: the stall most likely came from a nuance of the local DNS settings of the spike's
        machine, not from a colima default. The assessment is untested, so the record stays open, and the explicit
        `network.dns` workaround and the `colima` check stay.
      - Set `updated` to the execution date (`date +%F`). Do not change `status: "open"`, `workaround: true` or any
        other key.
    - docs/adr/0008-container-images-with-buildpacks.md:
      - Line 84: "Two defects of the local engine" becomes "Two problems of the local engine".
      - The colima DNS bullet (87-89) states what happened in the spike. Go-based tools on a colima profile without
        explicit resolvers waited about 10 seconds per lookup: the built-in forwarder appeared not to answer AAAA
        queries in time, and Go waits for both A and AAAA. So the buildpacks timed out downloading the JRE. A clean
        profile did not reproduce it, and the cause is not established. The workaround (explicit resolvers,
        `colima start --dns <resolver> --dns <resolver>`) stays, and `doctor` checks it. Tracked as UP-0011.
      - Line 110 becomes "Bad, because the colima DNS workaround (UP-0011) is set by hand on each machine while the
        cause of the stall is unknown."
    - docs/tooling.md lines 133-134: `network.dns` lists at least one resolver, because a profile without explicit
      resolvers once stalled Go DNS lookups in containers for about 10 s each and broke the buildpack downloads; a
      clean profile did not reproduce it ([UP-0011](upstream/0011-colima-aaaa-dns.md)).
    - scripts/doctor.sh lines 299-301 (governed, Edit tool):
      - Line 1: `# UPSTREAM: UP-0011 a colima profile without explicit resolvers once stalled Go DNS lookups for about 10 s each`.
      - Line 2: `# (AAAA suspected); a clean profile did not reproduce it, so the cause is not established.`
      - Then the `colima (local only)` comment, with its reason reworded to "an explicit DNS list, the UP-0011
        workaround kept until the cause is known".
      - Do not change the warning text at 320-321, or any code.

    Commit: `docs(quick-261125-mwg): word the colima DNS stall as observed once (IN-01)`.

    Part B, IN-03 (one commit). In .github/workflows/codeql.yml line 32 (governed, Edit tool), replace only the comment
    text after `# UPSTREAM: UP-0010`. The new text: the CodeQL Kotlin extractor found no sources in this Kotlin 2.4.21
    Gradle build; an Isolated Projects interaction is suspected, since the extractor documents Kotlin up to 2.4.2x.
    Split it over two comment lines at the same indentation if it exceeds 120 columns. Leave the matrix, the second
    marker at line 43 and every other line unchanged.
    Commit: `ci(quick-261125-mwg): correct the UP-0010 marker text (IN-03)`.

    Part C, parser, RED first (one commit for tests and implementation, after RED is recorded):
    - Add TestKit cases to UpstreamRecordCheckFunctionalTest.kt for every `<behavior>` bullet:
      - extend the `front matter outside the supported subset` case list;
      - add one passing test for the two accepted multi-line shapes and the one-line trailing comma, asserting the
        upstream index row, like the existing comments test.
      The `record` helper writes `key: value`. A value that starts with a newline yields `upstream: ` plus the
      continuation lines, and the parser trims trailing blanks, so the helper needs no change.
    - Run `mise x -- ./gradlew :build-logic:functionalTest --tests '*UpstreamRecordCheckFunctionalTest*' --console=plain`.
      Record in the SUMMARY that the new accepting tests and the `#`-item case fail on the unchanged parser.
    - Then change FrontMatter.kt (governed, Edit tool; ktfmt layout by hand, checked by spotlessCheck):
      - In `parse`, walk the block with an explicit index. A list continues onto the following lines only while it is
        open, that is while its closing `]` has not been read outside a double-quoted string.
      - A list opens either when the inline value starts with `[` and does not close on that line, or when the inline
        value is empty and the next line starts with a space and its trimmed text starts with `[`. Any other empty
        value keeps today's "`<key>` has no value; write null" failure.
      - A continuation line must start with a space and must not be blank. While the list is open, a blank line, an
        unindented line or the closing delimiter ends the scan and gives `unterminated list`.
      - Join the inline text and the trimmed continuation lines with single spaces, and parse the result with the
        existing `Cursor.list()`. `Entry.line` and every failure inside the list report the key's line.
      - After the list closes, the next line is read as a new entry, so an indented line there still fails as an
        indented line.
      - In `Cursor`: accept one trailing comma before `]` (`[,]` and `[a,,]` still fail with `empty list item`). Reject
        a bare item that starts with `#` with `comments inside a list are not supported`; this also rejects a trailing
        comment inside an open multi-line list, and a whole-line comment inside it once joined. A trailing ` # comment`
        after the closing `]` stays accepted.
      - Update the KDoc: the list bullet of the subset becomes "a list of bare or double-quoted strings, on one line or
        continued on indented lines as Prettier writes it, with an optional trailing comma"; "multi-line values"
        becomes "multi-line scalars"; the `Items` KDoc follows; and the indented-line failure text names that only an
        open list may continue on indented lines. Keep detekt green: split helpers rather than suppressing rules.
    - Remove the whole-line `# prettier-ignore` from docs/upstream/0006-empty-platform-token.md and
      docs/upstream/0013-axon-server-dcb-context-flag.md with the Edit tool. Write the `upstream` value in the shape
      Prettier 3.10.1 printed for 0006 at planning time: `upstream:` alone on its line, then `  [` (two spaces), then the
      unchanged quoted URL indented by four spaces and followed by a comma, then `  ]`. Do not change `updated`, since
      this is a formatting change.
    - docs/templates/adr.md line 14 and docs/templates/upstream-record.md line 16: "one-line lists" becomes "lists, on
      one line or in the multi-line form Prettier writes for a long list".

    <!-- planner-discipline-allow: prettier-ignore -->
    Commit: `feat(quick-261125-mwg): accept multi-line front matter lists and drop the prettier-ignore lines`.
  </action>
  <verify>
    <automated>BASE=$(git merge-base HEAD main) && { git grep -q 'never answers AAAA' -- ':!.planning' ':!.claude'; test $? -eq 1; } && { git grep -q prettier-ignore -- docs/upstream; test $? -eq 1; } && ! grep -q 'does not cover Kotlin 2.4.21' .github/workflows/codeql.yml && codeql_diff=$(git diff -U0 "$BASE" -- .github/workflows/codeql.yml) && ! printf '%s\n' "$codeql_diff" | grep -E '^[+-] ' | grep -qvE '^[+-] *#' && bash scripts/test-doctor.sh && mise x -- ./gradlew checkUpstreamRecords checkAdrs spotlessCheck --console=plain && mise x -- ./gradlew :build-logic:functionalTest --tests '*UpstreamRecordCheckFunctionalTest*' --tests '*AdrCheckFunctionalTest*' --console=plain</automated>
    `git grep -q` exits 1 when nothing matches and above 1 on an error, so `test $? -eq 1` fails on both a match and a
    broken command.
    Grep gates:
    `cat docs/upstream/0011-colima-aaaa-dns.md docs/adr/0008-container-images-with-buildpacks.md docs/tooling.md scripts/doctor.sh | grep -c 'never answers AAAA'` [HEAD=4 EXPECT==0 @97ea3ad];
    `grep -c '^# UP-0011: .*not reproduced on a clean profile' docs/upstream/0011-colima-aaaa-dns.md` [HEAD=0 EXPECT>=1 @97ea3ad];
    `grep -c '^status: "open"$' docs/upstream/0011-colima-aaaa-dns.md` [HEAD=1 EXPECT>=1 @97ea3ad GUARD MUTATION: the status line rewritten to another status];
    `grep -c "author's assessment" docs/upstream/0011-colima-aaaa-dns.md` [HEAD=0 EXPECT>=1 @97ea3ad];
    `grep -c 'UPSTREAM: UP-0011' scripts/doctor.sh` [HEAD=1 EXPECT>=1 @97ea3ad GUARD MUTATION: the marker line dropped while rewording the comment];
    `grep -c 'not reproduce' scripts/doctor.sh` [HEAD=0 EXPECT>=1 @97ea3ad];
    `grep -c 'does not cover Kotlin 2.4.21' .github/workflows/codeql.yml` [HEAD=1 EXPECT==0 @97ea3ad];
    `grep -c 'UPSTREAM: UP-0010' .github/workflows/codeql.yml` [HEAD=2 EXPECT>=2 @97ea3ad GUARD MUTATION: the line-32 marker replaced instead of its text];
    `grep -c 'Isolated Projects' .github/workflows/codeql.yml` [HEAD=0 EXPECT>=1 @97ea3ad];
    `cat docs/upstream/0006-empty-platform-token.md docs/upstream/0013-axon-server-dcb-context-flag.md | grep -c prettier-ignore` [HEAD=2 EXPECT==0 @97ea3ad];
    `grep -c '^upstream:$' docs/upstream/0006-empty-platform-token.md` [HEAD=0 EXPECT>=1 @97ea3ad];
    `grep -c '^upstream:$' docs/upstream/0013-axon-server-dcb-context-flag.md` [HEAD=0 EXPECT>=1 @97ea3ad].
  </verify>
  <done>The SUMMARY quotes the RED failures of the new tests on the unchanged parser and the green run afterwards.
  `checkUpstreamRecords`, `checkAdrs` and `spotlessCheck` pass with 0006 and 0013 in Prettier's own shape.
  scripts/test-doctor.sh ends with 0 failed. Every grep gate holds, and there are three commits (IN-01, IN-03,
  parser).</done>
</task>

<task type="auto">
  <name>Task 2: One composite action for the CI mirror step (IN-04a) and a manager-scoped Renovate exclusion (IN-04b)</name>
  <precondition>`mise x -- renovate --version` prints a 44.x version, and `mise x -- zizmor --version` prints 1.32.0.</precondition>
  <files>.github/actions/maven-central-mirror/action.yml, .github/workflows/ci.yml, docs/adr/0016-ci-dependency-downloads.md, lefthook.yml, renovate.json5</files>
  <action>
    Part A, IN-04a (one commit).
    - Create .github/actions/maven-central-mirror/action.yml with the Write tool (governed through `.github/**`):
      - a leading comment: CI only, installs scripts/ci/maven-central-mirror.init.gradle.kts into the runner's Gradle
        user home, called after setup-gradle by every Gradle job, see docs/adr/0016-ci-dependency-downloads.md;
      - `name: Use the Maven Central mirror (CI only)` and a one-line `description`;
      - `runs: using: composite`, with one step named `Install the mirror init script`, `shell: bash`, and the run
        command <!-- planner-discipline-allow: install -D -m 0644 -->
        `install -D -m 0644 "${GITHUB_WORKSPACE}/scripts/ci/maven-central-mirror.init.gradle.kts" "${GRADLE_USER_HOME:-$HOME/.gradle}/init.d/maven-central-mirror.init.gradle.kts"`.
      The explicit `${GITHUB_WORKSPACE}` is what the relative path resolved against in the job, so the source, the
      target and the mode stay identical. No inputs, no outputs and no `${{ }}` expressions, which gives zizmor no
      template-injection surface. Every job checks out the repository first, so the local action resolves. The only
      difference is `shell: bash` (`-eo pipefail` instead of the default `-e`), which cannot change a single-command
      step.
    - .github/workflows/ci.yml (Edit tool): in each of the five jobs, replace the inline step under the
      `# See docs/adr/0016-ci-dependency-downloads.md.` comment with a step of the same name
      (`Use the Maven Central mirror (CI only)`) that has `uses: ./.github/actions/maven-central-mirror`, at the same
      position, after setup-gradle. Keep the ADR comment.
    - In the ci.yml header (lines 12-13), the first mechanism becomes: the local composite action
      .github/actions/maven-central-mirror, called after setup-gradle in each of these jobs, installs
      scripts/ci/maven-central-mirror.init.gradle.kts into the runner's Gradle user home.
    - In the lint job's zizmor step (line 310), the command becomes `zizmor --no-progress .github/workflows .github/actions`.
      The step is renamed to `Audit workflows and actions (zizmor)`.
    - lefthook.yml (not governed): the zizmor job's `glob` becomes a list of `.github/workflows/*.{yml,yaml}` and
      `.github/actions/**/action.{yml,yaml}`. The actionlint job stays on workflows only: actionlint lints workflow
      files, and it reads the local action's metadata when it lints ci.yml.
    - docs/adr/0016-ci-dependency-downloads.md lines 56-59: the install now happens in the local composite action,
      which each of the five Gradle jobs calls after setup-gradle. One definition replaces the five copies, so the step
      cannot drift between jobs, and the init script's contribution to the configuration-cache fingerprint stays the
      same everywhere. Search the ADR for any other sentence describing per-job copies, and update it the same way.
    - Leave scripts/ci/maven-central-mirror.init.gradle.kts unchanged. Its comment ("the workflow installs this script
      ... in every Gradle job") stays true, and it is a functionalTest input.

    Commit: `ci(quick-261125-mwg): install the Maven Central mirror through one composite action (IN-04)`.

    Part B, IN-04b (one commit).
    - First, record the baseline. Run
      `RENOVATE_BASE_DIR=$(mktemp -d) LOG_LEVEL=debug mise x -- renovate --platform=local --dry-run=extract`
      into a scratch log (about 30 s, offline-capable, read-only). Save the sequence of
      `Matched <n> file(s) for manager <m>` lines. At planning time: gradle 38, github-actions 6, npm 3. Save the
      counts of `"packageFile": "scripts/ci/` (0) and `storage-download.googleapis` (0).
    - renovate.json5 (Edit tool):
      - Delete the top-level `ignorePaths` array and its four-line comment (lines 13-27).
      - In its place add `gradle: { ignorePaths: ["scripts/ci/**"] },` with a comment that says:
        - the CI-only mirror init script (docs/adr/0016-ci-dependency-downloads.md) is a Gradle file, and the gradle
          manager would otherwise add its mirror URL to the registries it looks every Gradle dependency up in;
        - `ignorePaths` is not mergeable, so a top-level array would replace the `:ignoreModulesAndTests` list that
          Renovate maintains;
        - at manager scope it replaces that list for the gradle manager only. Every other manager keeps the preset as
          Renovate ships it, and the gradle manager skips only scripts/ci/**. The preset's directories hold no Gradle
          build file here.

      Leave the `.mise/locks/**` packageRule as it is.
    - Rerun the same dry run on the change. Pass when:
      - every `Matched` line equals the baseline, except github-actions, which gains one file (the new action.yml,
        which has no third-party `uses:`);
      - `"packageFile": "scripts/ci/` and `storage-download.googleapis` still count 0;
      - `"packageFile": "settings.gradle.kts"` is still present.

      Quote both `Matched` sequences in the SUMMARY.
    - Fallback, only if the dry run cannot be run at all (not if it fails the comparison, which is a defect to fix):
      keep a top-level array instead. Update it to the preset Renovate 44.262.1 documents (add `**/.mise/locks/**` and
      `**/.config/mise/locks/**` after `**/__fixtures__/**`), followed by `scripts/ci/**`. Add a comment that names the
      preset and that Renovate version, and that tells a reviewer of a Renovate major bump to compare the list with
      the preset. Record the reason in the SUMMARY.

    Commit: `build(quick-261125-mwg): scope the scripts/ci exclusion to the Renovate gradle manager (IN-04)`.
  </action>
  <verify>
    <automated>test "$(grep -c 'uses: ./.github/actions/maven-central-mirror' .github/workflows/ci.yml)" -ge 5 && ! grep -q 'install -D -m 0644' .github/workflows/ci.yml && grep -q 'install -D -m 0644' .github/actions/maven-central-mirror/action.yml && mise x -- actionlint && mise x -- zizmor --offline --no-progress .github/workflows .github/actions && mise x -- zizmor --offline --no-progress .github/actions && mise x -- renovate-config-validator --strict renovate.json5 && ! grep -q '"\*\*/bower_components/\*\*"' renovate.json5 && grep -q 'gradle: { ignorePaths: \["scripts/ci/\*\*"\] }' renovate.json5 && mise x -- ./gradlew spotlessCheck --console=plain</automated>
    Grep gates:
    `grep -c 'uses: ./.github/actions/maven-central-mirror' .github/workflows/ci.yml` [HEAD=0 EXPECT>=5 @97ea3ad] (one per Gradle job: build, integration-test, functional-test, compose-smoke, doctor);
    `grep -c 'install -D -m 0644' .github/workflows/ci.yml` [HEAD=5 EXPECT==0 @97ea3ad];
    `grep -c 'install -D -m 0644' .github/actions/maven-central-mirror/action.yml` [HEAD=error:2 EXPECT>=1 @97ea3ad];
    `grep -c 'zizmor --no-progress .github/workflows .github/actions' .github/workflows/ci.yml` [HEAD=0 EXPECT>=1 @97ea3ad];
    `grep -c '"\*\*/bower_components/\*\*"' renovate.json5` [HEAD=1 EXPECT==0 @97ea3ad];
    `grep -c 'gradle: { ignorePaths: \["scripts/ci/\*\*"\] }' renovate.json5` [HEAD=0 EXPECT>=1 @97ea3ad].
    The zizmor run on `.github/actions` alone must exit 0. zizmor exits non-zero when it collects no input, so this
    proves the action is audited.
  </verify>
  <done>Five jobs call the composite action, and ci.yml holds no inline install. actionlint, zizmor (workflows plus
  actions) and the strict Renovate validator pass. The SUMMARY quotes the baseline and the changed `Matched` sequences
  of the Renovate dry run: equal except github-actions +1, nothing from scripts/ci, no mirror URL. There are two
  commits.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Spotless skips untracked git-ignored files, proven by TestKit</name>
  <files>build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt, build-logic/src/main/kotlin/courses.root.gradle.kts, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt, governance/governed-paths.txt, .claude/settings.json</files>
  <behavior>
    - In a fixture that is a git work tree (`git init`), whose `.gitignore` holds `.playwright-mcp/` and `scratch/`,
      an unformatted `.playwright-mcp/page.json` and an unformatted `scratch/notes.md` leave `spotlessPrettierCheck`
      green. Assert UP_TO_DATE, as the profiler test does, since the ignored files are not even inputs. If Spotless
      reports SUCCESS instead, assert SUCCESS and an output naming neither file, and say why in the SUMMARY.
    - After `git add -f scratch/tracked.json` (unformatted), `spotlessPrettierCheck` fails. The output names
      `tracked.json` and names neither `notes.md` nor `page.json`, because a tracked file is formatted even where
      .gitignore matches it.
    - A fixture placed in a subdirectory of a git work tree whose `.gitignore` ignores the fixture's `scratch/` still
      fails on an unformatted `scratch/notes.md`. The project directory is not the work tree root, so only the static
      exclusions apply.
    - Every existing TestKit test, whose fixtures are not git repositories, passes unchanged (the fallback path).
  </behavior>
  <action>
    RED first: add the three tests to FormattingFunctionalTest.kt (not governed). Use a small private helper that runs
    `git -c init.defaultBranch=main <args>` in a directory through ProcessBuilder and asserts exit 0. Run them with
    `mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*' --console=plain`, and record
    in the SUMMARY that the first test fails on HEAD.

    GitIgnoredPaths.kt (new, Write tool; it becomes governed below). Follow the ExecutableOnPath pattern and the Gradle
    best practice of running external processes at configuration time only through a ValueSource:
    - `abstract class GitIgnoredPaths @Inject constructor(private val exec: ExecOperations) :
      ValueSource<List<String>, GitIgnoredPaths.Parameters>`.
    - Parameters:
      - `gitExecutable: Property<String>`, the absolute path from `ExecutableOnPath`, never a bare `git`, because of
        the JDK's cached PATH (JDK-6413313);
      - `workTree: DirectoryProperty`;
      - `coveredDirectoryNames: SetProperty<String>`.
    - `obtain()` returns an empty list when the executable is absent, or when
      `git -C <workTree> rev-parse --show-toplevel` fails or names another directory. Compare canonical files: macOS
      temp dirs sit behind the `/var` to `/private/var` symlink.
    - Otherwise run `git -C <workTree> ls-files -z --others --ignored --exclude-standard --directory`, with
      `isIgnoreExitValue` and stdout and stderr captured. Split on NUL, then:
      - drop every entry with a path segment in `coveredDirectoryNames` (a name ending in `*` matches as a prefix), so
        that build outputs appearing or disappearing never change the value and the configuration cache stays
        reused;
      - drop every entry containing `*` or `?`, because Ant patterns have no escape, and an unexcluded entry only
        means it stays formatted (fail closed);
      - map a directory entry `dir/` to `dir/**` and a file entry to its path;
      - return the result sorted.
    - KDoc for the class and the parameters (detekt and Dokka run on build-logic). It states that `--others` never
      lists a tracked file, so `.gitignore` can never remove a tracked file from a gate.

    courses.root.gradle.kts (governed, Edit tool):
    - Introduce `excludedDirectoryNames`: build, .gradle, .kotlin, node_modules, .git, .idea, .planning, .claude, .gsd,
      .mise, gradle-user-home, profile-out*.
    - Derive `excluded` from it as `**/<name>/**`, plus `gradle/wrapper/**` and `LICENSES/**`, so the pattern list is
      unchanged.
    - Obtain `gitIgnored` once through `providers.of(GitIgnoredPaths::class.java)`, with `gitExecutable` from the
      existing `executableOnPath("git")`, `workTree` set to `layout.projectDirectory` and `coveredDirectoryNames` set
      to `excludedDirectoryNames`. Read it with `.get()` at configuration, because Spotless needs a concrete file
      tree, as `taploPath.get()` already does. Comment why.
    - `sources()` adds `exclude(gitIgnored)`.
    - Update the comment above the list and the KDoc of `sources()`: untracked files that git ignores are skipped too;
      tracked files are always formatted.

    Governance (Edit tool, both governed):
    - governance/governed-paths.txt: add `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt`
      in the build job's formatters section (after `config/spotless/**`). Extend that section's comment: GitIgnoredPaths
      decides which untracked files Spotless skips, and an edit to it alone could empty the Spotless file sets.
      Reword the `.gitignore` bullet (lines 72-74) so that reuse lint and Spotless both skip only untracked ignored
      files (Spotless through GitIgnoredPaths, which lists untracked paths only).
    - .claude/settings.json: add `"Edit(/build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt)"`
      after `"Edit(/config/spotless/**)"`.

    Live proofs in the worktree:
    - create an unformatted `.playwright-mcp/probe.json`, run `spotlessCheck` (passes), then delete the directory;
    - check the configuration cache: run `spotlessCheck` twice, then create `backend/zz-probe/build/x.txt`, run it again
      and expect `Reusing configuration cache.`, then delete `backend/zz-probe`.

    Quote both results in the SUMMARY.
    Commit: `fix(quick-261125-mwg): skip untracked git-ignored files in Spotless`.
  </action>
  <verify>
    <automated>mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*' --console=plain && mise x -- ./gradlew checkClaudeAskRules checkBuildScripts spotlessCheck :build-logic:check -x :build-logic:functionalTest --console=plain && mkdir -p .playwright-mcp && printf '{"probe":1,   "items":[1,2]}\n' > .playwright-mcp/probe.json && mise x -- ./gradlew spotlessCheck --console=plain; status=$?; rm -rf .playwright-mcp; test "$status" = 0</automated>
    Grep gates:
    `grep -c 'GitIgnoredPaths' build-logic/src/main/kotlin/courses.root.gradle.kts` [HEAD=0 EXPECT>=1 @97ea3ad];
    `grep -c '^build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt$' governance/governed-paths.txt` [HEAD=0 EXPECT>=1 @97ea3ad] (checkClaudeAskRules enforces exactly one matching ask rule);
    `grep -c 'Edit(/build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt)' .claude/settings.json` [HEAD=0 EXPECT>=1 @97ea3ad];
    `grep -c 'ls-files' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/GitIgnoredPaths.kt` [HEAD=error:2 EXPECT>=1 @97ea3ad].
  </verify>
  <done>The three new TestKit tests pass after a recorded RED. `checkClaudeAskRules`, `checkBuildScripts`,
  `spotlessCheck` and build-logic detekt and Dokka pass. The `.playwright-mcp` probe leaves `spotlessCheck` green, and
  the configuration cache is reused after a new `build/` directory appears. There is one commit.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| agent worktree to main checkout | governed edits reach the main checkout only through the merge the merge hook asks about (ADR-0015) |
| repository files to Spotless gate | GitIgnoredPaths decides which files the build job's formatter evaluates |
| workflow to local composite action | code in `.github/actions/**` runs in five CI jobs with `contents: read` |
| renovate.json5 to hosted Renovate | which files the gradle manager reads decides the registries every Gradle lookup uses |
| docs/upstream and docs/adr text to governance checks | the front matter parser decides which inputs checkUpstreamRecords and checkAdrs accept |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-mwg-01 | Tampering | GitIgnoredPaths / Spotless file sets | high | mitigate | Only `ls-files --others` entries are excluded, so a tracked file is never skipped. The work-tree-root guard compares canonical files. Entries with Ant wildcards stay formatted. TestKit proves that a force-added tracked file under an ignored directory fails the check. The file becomes a governed path with its ask rule |
| T-mwg-02 | Elevation of privilege | .github/actions/maven-central-mirror/action.yml | medium | mitigate | A local action with no inputs and no `${{ }}` expressions, run only after checkout under the jobs' unchanged `contents: read`. zizmor now audits `.github/actions` in CI and pre-commit. `.github/**` is governed |
| T-mwg-03 | Tampering | Renovate gradle registry set | medium | mitigate | Manager-scoped `gradle.ignorePaths`. The local extract dry run proves nothing from scripts/ci is read and no mirror URL reaches registryUrls (negative control at planning time: 37 occurrences without the exclusion). `renovate-config-validator --strict` |
| T-mwg-04 | Tampering | FrontMatter parser | medium | mitigate | Continuation only while a list is open. Comments inside lists, blank or unindented lines in an open list, text after `]` and empty items fail closed. TestKit failure cases for each |
| T-mwg-05 | Repudiation | UP-0011 and UP-0010 prose and markers | low | mitigate | The wording gates prove no tracked file outside .planning and .claude repeats the refuted claims. The diff gate proves codeql.yml changed in comments only |
| T-mwg-06 | Denial of service | configuration cache and git at configuration time | low | mitigate | A filtered ValueSource value that ignores build outputs. A live proof that the cache is reused after a new `build/` directory appears |
| T-mwg-07 | Tampering | governed files edited by shell writes | medium | mitigate | Edit and Write tools only, no `spotlessApply`. The merge hook lists every governed file for the author at cleanup |
| T-mwg-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package-manager install and no dependency change. The Renovate run uses the mise-pinned, already installed 44.252.8, so the package legitimacy gate has nothing to audit |
</threat_model>

<verification>
Full gates on the final committed HEAD, from the worktree root, in the CI lint job's order (memory rule: run every lint
step locally, because lefthook does not run editorconfig-checker):
- `mise x -- actionlint`;
- `mise x -- zizmor --offline --no-progress .github/workflows .github/actions` (CI runs it online with the job token;
  run it online too with `GH_TOKEN=$(gh auth token)` when `gh` is authenticated);
- `git ls-files -z '*.sh' | xargs -0 -r mise x -- shellcheck`;
- `mise x -- editorconfig-checker`;
- `mise x -- reuse lint`;
- the normalisation step copied from ci.yml: copy the index to a temp file, run `git add --renormalize .` on that copy,
  then `git diff --cached --exit-code --stat`;
- `mise x -- renovate-config-validator --strict renovate.json5`;
- `docker compose config --quiet`;
- `git ls-files -z -- 'scripts/test-*.sh' 'scripts/*/test-*.sh' | xargs -0 -r -n1 bash`: every suite ends with
  `0 failed`.

Then, in the background, because the full TestKit suite takes about ten minutes:
`mise x -- ./gradlew spotlessCheck checkUpstreamRecords checkAdrs checkClaudeAskRules checkBuildScripts checkToolingInventory :build-logic:check --console=plain`.
Run `spotlessCheck` again after writing the SUMMARY, which is Markdown that Spotless reads.

Scope gate: `git diff --name-only "$BASE" HEAD -- ':!.planning'` lists exactly the files in `files_modified`. If a gate
fails, fix it in its own commit and name that commit in the SUMMARY.
</verification>

<success_criteria>
- IN-01, IN-03 and IN-04 of the 2026-11-25 review are fixed in their own commits, and the SUMMARY maps each finding
  to its commit hash.
- The `# prettier-ignore` lines are gone. The parser accepts Prettier's multi-line lists and still fails closed on
  every other construct.
- `spotlessCheck` ignores `.playwright-mcp/` and any other untracked ignored path, never skips a tracked file, and
  keeps the configuration cache stable.
- Every CI lint-job step and the affected Gradle checks, including the full `:build-logic:functionalTest`, are green
  on the final HEAD. Nothing is pushed.
</success_criteria>

<orchestrator_notes>
After the executor's branch is merged back (the merge hook asks the author once about the governed files), the
orchestrator sets the rows IN-01, IN-03 and IN-04 in the "Review of 2026-11-25" table of
`.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md` to `fixed`, with
"fixed by quick task 261125-mwg in <hash>", and records the quick task in `.planning/STATE.md`. The executor touches
neither file.
</orchestrator_notes>

<output>
Create `.planning/quick/261125-mwg-close-the-open-phase-02-follow-ups-in-01/261125-mwg-SUMMARY.md` when done
</output>
