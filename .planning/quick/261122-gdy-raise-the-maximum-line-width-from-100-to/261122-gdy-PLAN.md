---
phase: quick-261122-gdy
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - .editorconfig
  - config/detekt/detekt.yml
  - .prettierrc.yaml
  - .taplo.toml
  - .planning/PROJECT.md
  # Task 3 also commits whatever `spotlessApply` rewrites at 120 columns. That set is formatter output,
  # known only after the run (expected: build-logic, tooling, platform and root *.kt/*.kts, *.toml,
  # *.json/*.json5 and *.yml/*.yaml files, including governed paths under .github/ and governance/).
autonomous: true
requirements: [TEST-09, INFRA-14, TEST-13, INFRA-12]

estimate:
  tokens: 90000
  raw_tokens: 90000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "ktfmt (Kotlin and KTS) keeps a line of exactly 120 columns unchanged and wraps a line of 121 columns, proven by a TestKit test that runs the real courses.root plugin"
    - "Prettier (JSON) and taplo (TOML) keep a line of exactly 120 columns unchanged and wrap a line of 121 columns, proven by TestKit tests that use the real .prettierrc.yaml and .taplo.toml"
    - ".editorconfig declares max_line_length 120 for [*.{kt,kts}] and [*.toml], .prettierrc.yaml has printWidth 120, .taplo.toml has column_width 120, and their comments name the one shared value accurately"
    - "detekt MaxLineLength is 140, a backstop above the 120-column formatter limit for lines no formatter can break"
    - "The whole tree is formatted at 120 columns: ./gradlew build (spotlessCheck, detekt, Dokka, TestKit) and the CI Lint commands pass, and the reformat changed only whitespace, comma and KDoc-star tokens outside .planning"
    - "PROJECT.md Key Decisions records the 120-column limit as an author decision dated 2026-11-22, with rationale and trade-off"
  artifacts:
    - path: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt"
      provides: "TestKit proof that ktfmt, Prettier and taplo keep 120 columns and wrap at 121"
      contains: "MAX_WIDTH = 120"
    - path: "build-logic/src/main/kotlin/courses.root.gradle.kts"
      provides: "Shared maxLineWidth applied to the ktfmt step of the kotlin and kotlinGradle formats"
      contains: "setMaxWidth(maxLineWidth)"
    - path: ".editorconfig"
      provides: "max_line_length 120 for Kotlin, KTS and TOML"
      contains: "max_line_length = 120"
    - path: ".prettierrc.yaml"
      provides: "Prettier print width 120"
      contains: "printWidth: 120"
    - path: ".taplo.toml"
      provides: "taplo column width 120"
      contains: "column_width = 120"
    - path: "config/detekt/detekt.yml"
      provides: "detekt MaxLineLength backstop 140"
      contains: "maxLineLength: 140"
    - path: ".planning/PROJECT.md"
      provides: "Key Decisions row for the 120-column author decision"
      contains: "| Maximum line width 120 for every formatted artifact"
  key_links:
    - from: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt"
      to: "build-logic/src/main/kotlin/courses.root.gradle.kts"
      via: "GradleFixture.standard applies the real courses.root plugin, so spotlessKotlinCheck and spotlessKotlinGradleCheck run the ktfmt step with maxLineWidth"
    - from: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt"
      to: ".prettierrc.yaml"
      via: "copyFromRepo(\".prettierrc.yaml\") and copyFromRepo(\".taplo.toml\") make the width tests format with the committed configuration"
    - from: "build-logic/src/main/kotlin/courses.root.gradle.kts"
      to: ".editorconfig"
      via: "the maxLineWidth comment and the .editorconfig Kotlin comment cross-reference the same 120 value"
    - from: ".taplo.toml"
      to: ".editorconfig"
      via: "column_width equals max_line_length of the [*.toml] section (comment in both files)"
---

<objective>
Raise the maximum line width of every formatted artifact from 100 to 120 columns, prove it with TestKit tests,
reformat the tree, and record the author decision.

1. Kotlin and KTS: the ktfmt step (kotlinlang style) gets `maxWidth` 120 through Spotless's
   `configure { it.setMaxWidth(...) }`. `.editorconfig` `[*.{kt,kts}]` follows. detekt `MaxLineLength` moves from
   120 to 140 and becomes a backstop for lines no formatter can break.
2. TOML, JSON, YAML and Markdown: taplo `column_width`, the `.editorconfig` `[*.toml]` section and Prettier
   `printWidth` move to 120. Prettier keeps `proseWrap: preserve`, so Markdown prose is not rewrapped.
3. Reformat the whole tree with `spotlessApply`. Prove the reformat changed only whitespace, comma and KDoc-star
   tokens. Add the author decision to the Key Decisions table in `.planning/PROJECT.md`.

Purpose: long DCB and Axon names and annotations wrap badly at 100 columns, and 120 is the IntelliJ, Spring and
Axon convention. This is an author decision (2026-11-22). It is cheapest to make before any domain code exists.
Output: one new TestKit test class, six configuration edits, a formatter-only reformat commit, and one Key
Decisions row. Work stays on the existing branch `gsd/phase-01-line-width-120`. Do not create or switch branches,
and do not push.
</objective>

<execution_context>
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/workflows/execute-plan.md
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@build-logic/src/main/kotlin/courses.root.gradle.kts
@build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt

Facts observed at planning time (HEAD `2ee5111`, branch `gsd/phase-01-line-width-120`, clean tree):

- Spotless 8.11.0 (`gradle/libs.versions.toml` `spotless`) depends on spotless-lib 4.11.0. Its
  `BaseKotlinExtension.KtfmtConfig.kotlinlangStyle()` returns `ConfigurableStyle`. That type has
  `configure(java.util.function.Consumer<KtfmtStep.KtfmtFormattingOptions>)`. `KtfmtFormattingOptions` has setters
  only (`setMaxWidth(int)`, `setBlockIndent`, `setContinuationIndent`, `setRemoveUnusedImports`,
  `setTrailingCommaManagementStrategy`) and no getters. Kotlin property syntax (`it.maxWidth = ...`) therefore does
  not compile, so call `it.setMaxWidth(...)`. The Gradle Kotlin DSL applies SAM-with-receiver only to
  `HasImplicitReceiver` types, so this `Consumer` lambda uses `it`. For ktfmt 0.65 (>= 0.63), spotless-lib applies
  the options through `FormattingOptions.toBuilder()`. Only `maxWidth` is overridden. Indents and the trailing-comma
  strategy stay as kotlinlang style defines them.
- ktfmt does not read `.editorconfig`, so the width must be set on the Spotless step. ktfmt reflows KDoc to
  `maxWidth`. It does not reflow `//` line comments.
- taplo 0.10.0 (`mise.toml`) has no EditorConfig support. `taplo fmt --help` lists no such option, and the binary
  contains no `editorconfig` string. Spotless also pipes TOML through stdin (`fmt --config .taplo.toml -`), so no
  path-based configuration could apply. The comment above `[*.toml]` in `.editorconfig` claims otherwise and is
  inaccurate. Probe with `column_width = 120`: `items = ["a", "bbb…"]` at exactly 120 columns stays on one line. At
  121 columns it expands to four lines (`items = [`, `"a",`, `"bbb…"`, `]`). The limit is inclusive.
- `.editorconfig-checker.json` disables `MaxLineLength`, so detekt `MaxLineLength` is the only line-length gate.
  It stays that way. `.editorconfig-checker.json` is not touched.
- The only repository text outside `.planning` that names the 100-column limit is the configuration itself
  (`.editorconfig` lines 12, 15 and 23, `.prettierrc.yaml` line 3, `.taplo.toml` line 4). Docs and README do not
  mention a width. Nothing else needs a wording update.
- `GradleFixture.standard(projectDir)` applies the real `courses.root` plugin. It copies the real
  `.prettierrc.yaml`, `.taplo.toml`, `.editorconfig`, `config/detekt/detekt.yml` and `platform/build.gradle.kts`.
  The width tests therefore exercise the committed configuration. `run`, `runAndFail`, `file`, `read`,
  `GradleFixture.HEADER` and `GradleFixture.PASSED` are the fixture API. Arguments such as `--continue` pass
  straight to Gradle. Spotless task names are `spotlessKotlin*`, `spotlessKotlinGradle*`, `spotlessPrettier*` and
  `spotlessToml*`, each with `Apply` and `Check` variants. Until Task 3 runs, the copied
  `platform/build.gradle.kts` is still formatted at the old width. Each test therefore normalises scaffolding with
  the format's `Apply` tasks first, as the existing tests do.
- detekt analyses the `functionalTest` source set. `*FunctionalTest.kt` files are excluded from the undocumented
  function and property rules and from `FunctionNaming` (prose test names). `MagicNumber` is not excluded, so use
  a `private const val` for 120 (constant declarations are ignored by `MagicNumber`).
- Prior verify command for TestKit tests:
  `mise x -- ./gradlew :build-logic:functionalTest --tests '*FormattingFunctionalTest*' --console=plain`.
- The CI Lint job (`.github/workflows/ci.yml`) runs `actionlint`, `zizmor --no-progress .github/workflows`,
  `shellcheck`, `editorconfig-checker`, `reuse lint` and `renovate-config-validator --strict renovate.json5`. The
  lefthook pre-commit hook formats staged files through `scripts/format-staged.sh`. That script uses the working
  tree's build-logic, so files committed in Task 1 and later are formatted at 120.
- Governed paths (`governance/governed-paths.txt`) touched by this plan include
  `build-logic/src/main/kotlin/courses.root.gradle.kts` and, through the reformat, `.github/**`, `governance/**`,
  root and build-logic build scripts, `mise.toml`, `renovate.json5` and `.claude/settings.json`. `.editorconfig`,
  `config/**`, `.prettierrc.yaml` and `.taplo.toml` are not governed. Claude Code ask rules may prompt on governed
  edits, which is expected. The PR carries `governance-approved`. The orchestrator handles the PR, and this plan
  does not open it.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Kotlin and KTS at 120 columns end to end (TestKit proof, ktfmt maxWidth, .editorconfig, detekt backstop)</name>
  <files>build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt, build-logic/src/main/kotlin/courses.root.gradle.kts, .editorconfig, config/detekt/detekt.yml</files>
  <read_first>
    - build-logic/src/main/kotlin/courses.root.gradle.kts (shared vals lines 14-93, spotless kotlin/kotlinGradle blocks lines 95-107)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt (standard(), run, runAndFail, file, read, HEADER, PASSED)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/QualityGatesFunctionalTest.kt (test style: class KDoc, @TempDir, prose names, kotest matchers)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/BuildScriptLintCheckFunctionalTest.kt (shouldBeIn GradleFixture.PASSED import)
    - .editorconfig (lines 12-15)
    - config/detekt/detekt.yml (lines 1-6 and 60-62)
  </read_first>
  <behavior>
    - A `.kt` file whose class declaration line is exactly 120 columns and a `.kts` file whose `val` line is exactly 120 columns are not reported by spotlessKotlinCheck / spotlessKotlinGradleCheck, and spotlessApply leaves both byte-identical.
    - The 121-column twins are reported by name. After the Apply tasks, every line of them is at most 120 columns and they span more lines than before. The Check tasks then pass.
    - RED at HEAD (kotlinlang default width 100): the test fails because the 120-column files are reported or rewritten. It must not fail because of a compile error.
  </behavior>
  <action>
RED. Create `LineWidthFunctionalTest.kt` in package `io.github.aanbrn.courses.buildlogic`, starting with the
SPDX header like its siblings. Give the class a KDoc saying that it proves the formatters wrap at the project's
120-column limit: a line of exactly 120 columns is kept, and a line of 121 columns is wrapped. Add a file-level
`private const val MAX_WIDTH = 120`. Add a private helper `lineOfWidth(prefix, suffix, width)`. It pads between
prefix and suffix with the letter `b` and checks that the result is exactly `width` long. Add one generic private
helper that all width tests share (Task 2 adds two more tests to it). It takes the fixture, the Spotless format
names (for example `Kotlin` and `KotlinGradle`), and two maps from repository-relative path to content (kept files
and wrapped files). It then:
(1) runs `spotless<Format>Apply` for each format, to normalise the scaffolding;
(2) writes all files;
(3) runs `runAndFail` with `--continue` and every `spotless<Format>Check` task, and asserts that the output names
each wrapped file by file name and no kept file;
(4) runs the Apply tasks again;
(5) asserts that each kept file is byte-identical to its content, and that each wrapped file has more lines than
before with no line longer than `MAX_WIDTH`;
(6) runs the Check tasks and asserts that each outcome `shouldBeIn GradleFixture.PASSED`.
Write the test `Kotlin and KTS lines of 120 columns are kept and lines of 121 columns are wrapped` on
`GradleFixture.standard(projectDir)` with formats `Kotlin` and `KotlinGradle`. It has four files:
- Kept file `lib/src/main/kotlin/fixture/Kept.kt`: HEADER, `package fixture`, a blank line,
  `/** A documented public class. */`, then `lineOfWidth("class Kept(val first: String, val ", ": String)", MAX_WIDTH)`.
- Wrapped file `lib/src/main/kotlin/fixture/Wrapped.kt`: the same with `class Wrapped(` and `MAX_WIDTH + 1`.
- Kept file `data/KeptScript.kts`: HEADER, then `lineOfWidth("val kept = listOf(\"a\", \"", "\")", MAX_WIDTH)`.
- Wrapped file `data/WrappedScript.kts`: the same with `val wrapped` and `MAX_WIDTH + 1`.
These names keep every kept name from being a substring of a wrapped name. Run the test and confirm it fails at
HEAD, as described in the behavior block.

GREEN. In `courses.root.gradle.kts`, add `val maxLineWidth = 120` next to the other shared vals before the
`spotless` block, with a `//` comment. The comment says this is the one line-width limit for every formatter, and
that the same value is `max_line_length` in `.editorconfig`, `printWidth` in `.prettierrc.yaml` and
`column_width` in `.taplo.toml`. It also says that detekt `MaxLineLength` (140) is only a backstop for lines no
formatter can break. In both the `kotlin` and the `kotlinGradle` block, chain
`.configure { it.setMaxWidth(maxLineWidth) }` onto `ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()`. Keep
kotlinlang style and change no other option, per the author decision (maxWidth 120 on kotlinlangStyle).

In `.editorconfig`, raise `max_line_length` in `[*.{kt,kts}]` from 100 to 120. Rewrite the comment above the
section: Kotlin uses ktfmt kotlinlang style with 4 spaces, and the 120-column limit is set on the ktfmt step in
`build-logic/src/main/kotlin/courses.root.gradle.kts` (ktfmt does not read this file).

In `config/detekt/detekt.yml`, set `style.MaxLineLength.maxLineLength` to 140. Add a comment line above
`MaxLineLength:`: it is a backstop above the 120-column formatter limit and flags only lines no formatter can
break, such as long string literals and URLs.

Re-run the test until it passes. Do not touch `.editorconfig-checker.json`.
  </action>
  <verify>
    <automated>mise x -- ./gradlew :build-logic:functionalTest --tests '*LineWidthFunctionalTest*' --console=plain</automated>
    <automated>grep -v '^ *//' build-logic/src/main/kotlin/courses.root.gradle.kts | grep -c 'setMaxWidth(maxLineWidth)' # [HEAD=0 EXPECT>=2 @2ee5111]</automated>
    <automated>grep -c '^val maxLineWidth[[:space:]]=[[:space:]]120$' build-logic/src/main/kotlin/courses.root.gradle.kts # [HEAD=0 EXPECT>=1 @2ee5111]</automated>
    <automated>grep -A2 '^\[\*\.{kt,kts}\]' .editorconfig | grep -c 'max_line_length[[:space:]]=[[:space:]]120' # [HEAD=0 EXPECT>=1 @2ee5111]</automated>
    <automated>grep -A2 '^\[\*\.{kt,kts}\]' .editorconfig | grep -c 'max_line_length[[:space:]]=[[:space:]]100' # [HEAD=1 EXPECT==0 @2ee5111]</automated>
    <automated>grep -c '^    maxLineLength: 140$' config/detekt/detekt.yml # [HEAD=0 EXPECT>=1 @2ee5111]</automated>
    <automated>grep -c '@Test' build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt # [HEAD=error:2 EXPECT>=1 @2ee5111]</automated>
  </verify>
  <done>The Kotlin/KTS width test failed at HEAD on the kept files and passes now. Both ktfmt steps use maxWidth 120 through the shared `maxLineWidth`. `.editorconfig` declares 120 for Kotlin and KTS with an accurate comment. detekt MaxLineLength is 140 with its backstop comment. The task is committed on `gsd/phase-01-line-width-120`.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: JSON, YAML, Markdown and TOML at 120 columns (Prettier printWidth, taplo column_width, .editorconfig TOML section)</name>
  <files>build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt, .prettierrc.yaml, .taplo.toml, .editorconfig</files>
  <read_first>
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt (helper written in Task 1)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt (lines 18-38 and 63-77: Prettier and taplo fixture flow)
    - .prettierrc.yaml, .taplo.toml, .editorconfig (lines 17-23)
  </read_first>
  <precondition>`mise x -- taplo --version` reports 0.10.0 and `mise x -- node --version` matches `.nvmrc`. The Prettier and taplo Spotless steps need both on PATH.</precondition>
  <behavior>
    - JSON: `data/kept.json`, a top-level array `["a", "bbb…"]` of exactly 120 columns plus a newline, is not reported by spotlessPrettierCheck and stays byte-identical. Its 121-column twin `data/wrapped.json` is reported and then expanded to one element per line.
    - TOML: `data/kept.toml`, `items = ["a", "bbb…"]` at exactly 120 columns, is not reported by spotlessTomlCheck and stays byte-identical. Its 121-column twin `data/wrapped.toml` is reported and then expanded (matches the planning-time taplo probe).
    - RED before the config edits: both new tests fail because the 120-column kept files are reported or rewritten at width 100.
  </behavior>
  <action>
RED. In `LineWidthFunctionalTest.kt`, add two tests that reuse the Task 1 helper, `MAX_WIDTH` and `lineOfWidth`:
- `JSON lines of 120 columns are kept and lines of 121 columns are wrapped`: format `Prettier`, kept file
  `data/kept.json` with content `lineOfWidth("[\"a\", \"", "\"]", MAX_WIDTH)` plus a newline, and wrapped file
  `data/wrapped.json` with `MAX_WIDTH + 1`.
- `TOML lines of 120 columns are kept and lines of 121 columns are wrapped`: format `Toml`, kept file
  `data/kept.toml` with content `lineOfWidth("items = [\"a\", \"", "\"]", MAX_WIDTH)` plus a newline, and wrapped
  file `data/wrapped.toml` with `MAX_WIDTH + 1`.
Update the class KDoc so that it names ktfmt, Prettier and taplo. Run both tests and confirm they fail, as
described in the behavior block.

GREEN. Raise `printWidth` in `.prettierrc.yaml` from 100 to 120. Keep `proseWrap: preserve` and every other key
unchanged. Raise `column_width` in `.taplo.toml` from 100 to 120, and keep its comment, which points at the
`[*.toml]` section of `.editorconfig`. In `.editorconfig`, raise `max_line_length` in `[*.toml]` from 100 to 120.
Replace the comment above that section. The current comment claims that taplo takes its limit from this
section, but taplo 0.10.0 has no EditorConfig support (see the planning-time facts). The new comment says that
taplo takes its column limit from `column_width` in `.taplo.toml`, which must equal this value. All of this is
per the author decision (120 for TOML and for Prettier-formatted JSON, YAML and Markdown). Do not add
`max_line_length` to other `.editorconfig` sections. The author scoped the editorconfig change to `*.{kt,kts}` and
`*.toml`.

Re-run the width tests and the existing formatting tests until all pass.
  </action>
  <verify>
    <automated>mise x -- ./gradlew :build-logic:functionalTest --tests '*LineWidthFunctionalTest*' --tests '*FormattingFunctionalTest*' --console=plain</automated>
    <automated>grep -c '^printWidth: 120$' .prettierrc.yaml # [HEAD=0 EXPECT>=1 @2ee5111]</automated>
    <automated>grep -c '^column_width[[:space:]]=[[:space:]]120$' .taplo.toml # [HEAD=0 EXPECT>=1 @2ee5111]</automated>
    <automated>grep -A1 '^\[\*\.toml\]' .editorconfig | grep -c 'max_line_length[[:space:]]=[[:space:]]120' # [HEAD=0 EXPECT>=1 @2ee5111]</automated>
    <automated>grep -c 'taplo reads the column limit' .editorconfig # [HEAD=1 EXPECT==0 @2ee5111]</automated>
    <automated>grep -c '@Test' build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/LineWidthFunctionalTest.kt # [HEAD=error:2 EXPECT>=3 @2ee5111]</automated>
  </verify>
  <done>The JSON and TOML width tests failed before the edits and pass now, and FormattingFunctionalTest still passes. Prettier, taplo and `.editorconfig` all say 120, and the TOML comment is accurate. The task is committed on `gsd/phase-01-line-width-120`.</done>
</task>

<task type="auto">
  <name>Task 3: Reformat the tree at 120 columns (formatter output only) and record the author decision</name>
  <files>Formatter output of `spotlessApply` (set known only after the run; expected build-logic, tooling, platform and root *.kt/*.kts, *.toml, *.json/*.json5, *.yml/*.yaml, including governed paths under .github/ and governance/), .planning/PROJECT.md</files>
  <read_first>
    - .planning/PROJECT.md (Key Decisions table, lines 108-140; last row is "Node.js 26 LTS instead of 24")
    - .github/workflows/ci.yml (Lint job commands, lines 105-141)
  </read_first>
  <action>
Reformat. From the repository root, run `mise x -- ./gradlew spotlessApply --console=plain`. This task is
formatter output only. Make no hand edits to any file Spotless formats. Do not rewrap `//` line comments or
Markdown prose by hand: ktfmt reflows KDoc itself, and Prettier keeps prose under `proseWrap: preserve`. The
reformat is the full scope of the author's "reformat the tree with spotlessApply" item.

Before staging, prove that the diff is formatter-made, using the two git gates in `<verify>` in order. First,
`git diff --quiet -- . ':!.planning'; echo $?` must print 1, which shows that spotlessApply rewrote files. Then
the token gate writes the word diff to `${TMPDIR:-/tmp}/gdy-word.diff` and counts the changed segments made of
anything other than whitespace, commas and `*`. It must print 0. Capturing the diff in a file first means a
failing `git diff` fails the gate and cannot read as clean. At planning time a synthetic repository confirmed that line joins, removed trailing commas and
reflowed KDoc stars count as 0, and that any changed word counts as more than 0. If it prints more than 0, print
the segments (the same pipeline without `-c`). Trace each one to a formatter rule and record it in the SUMMARY.
Revert any segment no formatter explains instead of committing it.

Run the full gates:
- `mise x -- ./gradlew build --console=plain` (spotlessCheck, detekt with the 140 backstop, Dokka, every TestKit
  test including LineWidthFunctionalTest, and the governance checks).
- `mise x -- editorconfig-checker`.
- `mise x -- reuse lint`.
- `mise x -- actionlint`.
- `mise x -- zizmor --offline --no-progress .github/workflows`.
- `mise x -- renovate-config-validator --strict renovate.json5`.
If detekt reports a line over 140 columns (one no formatter can break), shorten that line by hand in a separate,
named change. Never raise the limit or add a suppression. Commit the reformat as its own commit.

Record the decision. Append one row to the end of the Key Decisions table in `.planning/PROJECT.md`, directly
after the "Node.js 26 LTS instead of 24" row, with these exact three cells:
- Decision: `Maximum line width 120 for every formatted artifact: Kotlin and KTS through ktfmt (kotlinlang style, maxWidth 120), TOML through taplo (column_width), JSON, YAML and Markdown through Prettier (printWidth, with prose kept by proseWrap: preserve); detekt MaxLineLength 140 as a backstop for lines no formatter can break`
- Rationale: `Long DCB and Axon type names and annotations wrap badly at 100 columns; 120 is the IntelliJ, Spring and Axon convention; switching before any domain code exists is the cheapest point. Trade-off: less room for side-by-side diffs and wider code blocks on the docs site`
- Outcome: `✓ 2026-11-22 (author decision)`
Use backticks around identifiers in the row the way neighbouring rows do (`maxWidth`, `column_width`,
`printWidth`, `proseWrap: preserve`, `MaxLineLength`). Keep the text otherwise as given. Change nothing else in
`.planning/`. STATE.md and the SUMMARY belong to the orchestrator.
  </action>
  <verify>
    <automated>git diff --quiet -- . ':!.planning'; echo $? # run before staging: 1 means spotlessApply rewrote files [HEAD=0 EXPECT==1 @2ee5111]</automated>
    <automated>git diff --word-diff=porcelain --word-diff-regex='[[:alnum:]_]+|[^[:space:]]' -- . ':!.planning' > "${TMPDIR:-/tmp}/gdy-word.diff" && grep -E '^[-+]' "${TMPDIR:-/tmp}/gdy-word.diff" | grep -vE '^(---|\+\+\+) ' | grep -cvE '^[-+][[:space:],*]*$' # run before staging, after the previous gate printed 1 [HEAD=0 EXPECT==0 @2ee5111 GUARD MUTATION: any hand edit that changes a word, such as a changed string literal in .github/workflows/ci.yml]</automated>
    <automated>mise x -- ./gradlew build --console=plain</automated>
    <automated>mise x -- editorconfig-checker</automated>
    <automated>mise x -- reuse lint</automated>
    <automated>mise x -- actionlint</automated>
    <automated>mise x -- zizmor --offline --no-progress .github/workflows</automated>
    <automated>mise x -- renovate-config-validator --strict renovate.json5</automated>
    <automated>grep -c '^| Maximum line width 120 for every formatted artifact' .planning/PROJECT.md # [HEAD=0 EXPECT>=1 @2ee5111]</automated>
  </verify>
  <done>The tree is formatted at 120 columns, and the reformat commit contains formatter output only (token gate 0, or every non-zero segment traced to a formatter rule in the SUMMARY). `./gradlew build` and every CI Lint command pass. PROJECT.md has the dated author-decision row. Everything is committed on `gsd/phase-01-line-width-120`, and nothing is pushed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| contributor or agent → governed repository configuration | The reformat rewrites governed paths (`.github/**`, `governance/**`, root and build-logic build scripts, `mise.toml`, `renovate.json5`, `.claude/settings.json`). The governance gate requires the `governance-approved` label and author review. |
| formatter configuration → CI quality gates | The width settings decide what `spotlessCheck` and detekt accept on every future pull request. |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-QGDY-01 | Tampering | Task 3 reformat diff over governed files (workflows, governance scripts and data, Renovate and mise config) | medium | mitigate | Task 3 forbids hand edits. The token-level `git diff --word-diff` gate proves that only whitespace, comma and KDoc-star tokens changed outside `.planning`. Any other segment must be traced to a formatter rule or reverted. actionlint, zizmor, `renovate-config-validator --strict`, `reuse lint`, editorconfig-checker and `./gradlew build` (including the governance checks) pass. The PR carries `governance-approved` after author review. |
| T-QGDY-02 | Tampering | Quality gates weakened under cover of the width change (detekt MaxLineLength, editorconfig-checker) | low | mitigate | A grep gate pins detekt `maxLineLength` to exactly 140. `.editorconfig-checker.json` is not in `files_modified`. Task 3 forbids raising the limit or adding suppressions for over-long lines. The decision is recorded as an author decision in PROJECT.md. |
| T-QGDY-03 | Denial of service | Width drift between ktfmt, Prettier, taplo and `.editorconfig` (hook, CI and IDE disagree) | low | mitigate | `LineWidthFunctionalTest` pins the real formatter behaviour at 120 and 121 columns with the copied committed configs and the real `courses.root` plugin. The comments in `courses.root.gradle.kts`, `.editorconfig` and `.taplo.toml` cross-reference the one value. |
| T-QGDY-SC | Tampering | npm/pip/cargo installs | high | accept | This plan installs no packages. ktfmt, Prettier and taplo versions are unchanged, and only tools already locked in `mise.lock` run. |
</threat_model>

<verification>
- `mise x -- ./gradlew build --console=plain` passes. It covers spotlessCheck at 120 columns, detekt with
  MaxLineLength 140, Dokka, all TestKit tests including the three `LineWidthFunctionalTest` tests, and the
  governance checks.
- `mise x -- editorconfig-checker`, `mise x -- reuse lint`, `mise x -- actionlint`,
  `mise x -- zizmor --offline --no-progress .github/workflows` and
  `mise x -- renovate-config-validator --strict renovate.json5` pass.
- The configuration grep gates of Tasks 1 and 2 hold: `setMaxWidth(maxLineWidth)` ==2, `max_line_length = 120` in
  both `.editorconfig` sections, `printWidth: 120`, `column_width = 120` and `maxLineLength: 140`.
- `git branch --show-current` prints `gsd/phase-01-line-width-120`. `git log --oneline 2ee5111..HEAD` shows the
  task commits. `git status --porcelain` is empty apart from the orchestrator-owned SUMMARY.
</verification>

<success_criteria>
- Every formatter wraps at 120 columns, and a TestKit test proves it at the 120/121 boundary for ktfmt (Kotlin and
  KTS), Prettier (JSON) and taplo (TOML).
- `.editorconfig`, `.prettierrc.yaml`, `.taplo.toml` and the ktfmt step agree on 120. detekt MaxLineLength 140 is
  the backstop.
- The tree is reformatted by `spotlessApply` alone, and the full build and the CI Lint commands are green.
- The author decision is recorded in the PROJECT.md Key Decisions table.
- No branch was created or switched. Nothing is pushed.
</success_criteria>

<output>
Create `.planning/quick/261122-gdy-raise-the-maximum-line-width-from-100-to/261122-gdy-SUMMARY.md` with `status: complete` in its frontmatter when done (the orchestrator commits it).
</output>
