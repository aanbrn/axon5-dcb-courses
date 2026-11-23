---
phase: quick-261123-rzt
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngineTest.kt
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt
  - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt
  - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CapacityHarness.kt
  - tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeView.kt
  - tooling/architecture/src/test/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeViewTest.kt
  - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheckFunctionalTest.kt
  - docs/adr/0012-dependency-licence-policy.md
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/JavaVersionFile.kt
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ConventionsFunctionalTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/MarkdownOutline.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt
  - scripts/doctor.sh
  - scripts/test-doctor.sh
  - docs/tooling.md
  - docs/adr/0013-upstream-records-and-workaround-markers.md
  - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md
autonomous: true
requirements: [CICD-01, CICD-09, INFRA-09, INFRA-12, TEST-12, UPS-04, TOOL-03]

estimate:
  tokens: 220000
  raw_tokens: 220000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "`mise x -- bash scripts/qodana.sh`, run with no argument from the executor's worktree root on its committed HEAD, exits 0 and its SARIF (`$HOME/.cache/axon5-dcb-courses/qodana/results/qodana.sarif.json`) holds 0 results"
    - "No suppression annotation or inspection-suppression comment is added in any Kotlin file since ed1980c, qodana.yaml and config/detekt are byte-identical to ed1980c, and no Qodana or detekt baseline file is tracked; the one magic-number suppression in DecisionCaptureIntegrationTest is gone"
    - "`mise x -- ./gradlew build` passes on the final tree (every suite including integrationTest on Axon Server, the build-logic TestKit suite, the architecture rules, detekt, Dokka failOnWarning, ktfmt via Spotless, allWarningsAsErrors)"
    - "WR-02: a licence policy whose `allowed` list holds an identifier matching a `forbidden` family (for example GPL-3.0-only against GPL-*) is rejected as invalid with `'GPL-3.0-only' is allowed, but matches a forbidden family`, proven by a TestKit case"
    - "WR-03: KotlinTagResolver tags a private `@EventTag` property of a payload with the TypeID text of its value, proven by a KotlinTagResolverTest case"
    - "WR-04: the image-literal rule scans a Kotlin code view in which comments and literal contents are masked by a scanner that tracks string, raw-string, char-literal, template and backtick-identifier state; tests prove that a `\"http://...\"` string and a `\"**/*.kt\"` string no longer hide an image literal after them"
    - "WR-06: every `image-*` key of the catalog `[versions]` table is checked, and a value that is not a double-quoted string (TOML literal string, inline table, multi-line string) is reported as `<key> must be a double-quoted <name>:<tag>@sha256:<64 hex digits>`, proven by a TestKit case"
    - "IN-01..IN-03: DecisionCaptureIntegrationTest sizes its pool by STUDENTS; `.java-version` is parsed by one build-logic function used by courses.root and courses.kotlin-library; the command-service tests share the fixture ArrivalBarrier and one fixture submitDecision; AdrCheck and UpstreamRecordCheck share one Markdown outline scanner that closes a fence only on a line starting with the opening fence string, proven by a `~~~` line inside a backtick fence in both TestKit suites"
    - "IN-04: `scripts/doctor.sh` reports a warn line per docs/upstream record with status fixed-upstream whose workaround markers remain in tracked files, naming the marker locations and printing the record's Removal criteria; scripts/test-doctor.sh proves the warn, the ok and the non-marker cases"
    - "02-REVIEW-DISPOSITION.md: WR-02, WR-03, WR-04, WR-06, IN-01, IN-02, IN-03, IN-04 are `fixed` with commit SHAs, WR-01 is `deferred` and IN-05 `skipped` with the author's Source text, both in the front matter and in the table, and `open: 0`"
  artifacts:
    - path: "tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeView.kt"
      provides: "Comment- and literal-masking scanner for Kotlin source text, same length, newlines kept"
      contains: "object KotlinCodeView"
    - path: "tooling/architecture/src/test/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeViewTest.kt"
      provides: "Unit proof of every scanner state (strings, raw strings, chars, templates, backtick identifiers, nested block comments)"
      contains: "class KotlinCodeViewTest"
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/MarkdownOutline.kt"
      provides: "The one level-1/level-2 heading scanner with fence-string tracking, used by AdrCheck and UpstreamRecordCheck"
      contains: "object MarkdownOutline"
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/JavaVersionFile.kt"
      provides: "readJavaMajorVersion, the one parser of .java-version text for the convention plugins"
      contains: "fun readJavaMajorVersion("
    - path: "scripts/doctor.sh"
      provides: "upstream-removal check"
      contains: "check_upstream_removal"
  key_links:
    - from: "tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt"
      to: "tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeView.kt"
      via: "imageLiteralsIn(source) runs LITERAL_IMAGE_CALLS over KotlinCodeView.of(source)"
    - from: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt"
      to: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/MarkdownOutline.kt"
      via: "structure() reads MarkdownOutline.headings(front); UpstreamRecordCheck RecordBody.check does the same"
    - from: "build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts"
      to: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/JavaVersionFile.kt"
      via: "providers.fileContents(.java-version).asText.map { JavaLanguageVersion.of(readJavaMajorVersion(it)) }; courses.root updateDaemonJvm does the same"
    - from: "backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt"
      to: "backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt"
      via: "application.submitDecision(pool, course, student, ArrivalBarrier(CONCURRENT_UNITS)); DecisionCaptureTest calls the same member"
    - from: "scripts/doctor.sh"
      to: "docs/upstream/0012-docker-containerd-platform-export.md"
      via: "check_upstream_removal reads id/status front matter and the Removal criteria section, then git grep for markers in tracked files"
---

<objective>
Resolve the author-approved dispositions of the Phase 2 code review (`.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md`
lines 191-348, `02-REVIEW-DISPOSITION.md`) and the 4 Qodana findings left by quick task 261123-p1u, so the required
`qodana` gate reports 0 problems. Work on the current branch `gsd/phase-2-runtime-infrastructure-dcb-spikes` (the
executor's worktree branch merges back into it); no new branch.

Author decisions (locked):
- FIX now: WR-02, WR-03, WR-04, WR-06, IN-01, IN-02, IN-03, IN-04 (Tasks 1-3 below carry each finding's full text and fix).
- DEFER WR-01: disposition `deferred`, Source cell exactly: `Phase 3 success criterion 2 (last-seat race, zero infrastructure errors): make the bound configurable, add capped full-jitter back-off and a 40-way concurrent subscription test in the first subscription slice`.
  No code change for WR-01 in this task.
- SKIP IN-05: disposition `skipped`, Source cell exactly: `deliberate scaffolding per D-02; architecture test requires exactly four contracts modules; first query slice is Phase 4`.
  No code change for IN-05.
- Mark every fixed finding `fixed` with its commit SHA(s) in 02-REVIEW-DISPOSITION.md, in the front matter
  `disposition` and in the table row, and set `open:` to 0.
- Fix the four Qodana leftovers on their merits (verbatim, ruleId | location | message):
  - RemoveExplicitTypeArguments | backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngineTest.kt:68 | Explicit type arguments can be inferred
  - MoveVariableDeclarationIntoWhen | build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt:251 | Variable declaration could be moved into 'when'
  - MoveVariableDeclarationIntoWhen | build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt:116 | Variable declaration could be moved into 'when'
  - MoveVariableDeclarationIntoWhen | build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt:366 | Variable declaration could be moved into 'when'

Hard rules (apply to every task):
- NO `@Suppress`, NO `noinspection` comment, NO Qodana baseline, NO detekt baseline, NO change to qodana.yaml or
  config/detekt. Qodana (profile Default, failThreshold 0) analyses all new code too: write idiomatic forms from the start
  (`when (val x = ...)` instead of a declaration followed by `when (x)`, a `when` subject where every branch tests the
  same value, inferred type arguments, `buildList`/`buildString` instead of concatenating collections, `$$"..."` /
  `$$"""..."""` multi-dollar strings instead of escaped dollars, no unused symbols, parameters or imports, and no
  parameter that every call site passes the same constant (SameParameterValue): prefer a constant or an inline literal in
  tests over a new helper parameter with one value).
- ktfmt via Spotless, detekt (default rules plus config/detekt/detekt.yml, incl. complexity and nesting limits),
  Dokka with failOnWarning (KDoc on every public declaration) and allWarningsAsErrors stay green. Run
  `mise x -- ./gradlew spotlessApply --console=plain` before each task's verify.
- Every new source file starts with the same `SPDX-License-Identifier: MIT` header line as its neighbours (REUSE).
- Commits: conventional messages scoped `quick-261123-rzt`, one commit per finding group as listed in each task, each
  ending with the line `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. In the worktree run every git command
  on its own (no `&&` chains, loops, redirects or `cd` mixed with git). Record every commit SHA for the disposition.
- Governed paths (build-logic governance package, courses.root.gradle.kts, scripts/doctor.sh) trigger ask prompts for
  the author; that is expected, wait for them.
- Do not touch `.planning/STATE.md`; the orchestrator owns it.

Tracer-first is not applied (`--no-tracer` reasoning): this is remediation of existing, proven code with no new
architecture; a thin slice would add no information. The end-to-end check is the full build plus the Qodana gate at the
end of Task 3.

Purpose: the phase PR must not turn the required `qodana` job red, and every review finding the author accepted is closed
with a test that would have caught it.
Output: fixes and tests for 8 findings, 4 Qodana fixes, an updated disposition file, a green build and a 0-problem
Qodana run, with a per-finding disposition table in the SUMMARY.
</objective>

<execution_context>
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/workflows/execute-plan.md
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@.claude/skills/spike-findings-axon5-dcb-courses/SKILL.md
@.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md
@.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md
@.planning/quick/261123-p1u-fix-all-45-qodana-findings-on-the-phase-/261123-p1u-SUMMARY.md

## Facts established during planning (do not re-derive; verify only where a step says so)

- Base commit of this task: `ed1980c` (branch head when the plan was written; the plan commit itself only touches
  `.planning/`). The diff gates below compare against it.
- Gradle project paths: `:command-service`, `:contracts-kernel`, `:contracts-events`, `:tooling:architecture`,
  `:tooling:doctor`; build-logic is an included build reachable as `:build-logic:<task>` from the root (p1u ran
  `:build-logic:check` that way). Root `check` depends on the build-logic `:check`.
- Every JVM test suite of a module gets `implementation(project())`, kotest assertions and kotest-property, and the
  module's testFixtures when it applies `java-test-fixtures` (courses.test-suites). command-service applies it;
  tooling/architecture does not and has no `src/test` yet, but the `test` suite exists. Test class names end with the
  suffix of their suite: `Test` for `src/test`, `ArchitectureTest`, `IntegrationTest`, `FunctionalTest`
  (TestNamingArchitectureTest), and every `@Test` function has a block body.
- MessageArchitectureTest requires every `@EventTag` in backend sources (tests included) to name its key as a
  `Tags.*` reference, e.g. `@EventTag(key = Tags.COURSE)`.
- ContainerImageArchitectureTest scans every `.kt` file under `backend` and `tooling` (its own file included). Its two
  patterns (`LITERAL_IMAGE_CALLS`) end at the opening quote of the first argument.
- The build-logic TestKit fixture imports the fixture's own `gradle/libs.versions.toml` as a Gradle version catalog
  (`defaultLibrariesExtensionName = fixtureLibs`), so a catalog written by a test must still be a valid Gradle catalog:
  `[versions]` accepts a string or a rich-version table (`{ require = "..." }`), not arbitrary tables.
- Kotlin in build-logic is 2.4.21 (Gradle 9.9.2 embedded); multi-dollar strings and `when` guards compile there.
- UpstreamRecordCheck's marker definition: `(?:^|\s)(?://|/\*|\*|#|<!--|--)\s*UPSTREAM:` followed by ` UP-NNNN` and a word
  boundary; it scans `**/*.kt, *.kts, *.sh, *.yaml, *.yml, *.toml, *.json, *.json5, *.md, *.properties, *.xml`, root
  `.env.example` and `NOTICE`, excluding build/.gradle/.kotlin/node_modules/.git/.idea/.mise/.planning/.claude,
  `build-logic/src/functionalTest/**`, `docs/upstream/**` and `docs/templates/**`. Any text in a scanned file (doctor.sh,
  test-doctor.sh included) where the marker word plus colon follows a comment opener IS a marker and must name a real
  record, so new shell code must build that text from a variable instead of writing it literally.
- docs/upstream/0012-docker-containerd-platform-export.md has `status: "fixed-upstream"` and its markers are at
  scripts/doctor.sh:209 and :286; it is the only record whose front matter status is `fixed-upstream` (every other
  record is `open` or `reported`). So after Task 3 the real `doctor.sh` prints exactly one upstream-removal warn, for
  UP-0012.
- CI runs every `scripts/test-*.sh` and `scripts/*/test-*.sh` with bash, shellcheck on every tracked `*.sh`, and
  `scripts/doctor.sh --ci` in the Doctor job. Spotless format-checks shell scripts with shfmt during `./gradlew build`.
- Qodana analyses the committed HEAD exported by `git archive` (scripts/qodana.sh), so it must run after the last
  commit. The previous run on this machine timed out reaching `qodana.cloud` (network, not code); retry a couple of
  minutes later if that happens and report precisely.
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Kotlin fixes in command-service and the architecture rules (WR-03, WR-04, IN-01, IN-02 test part, Qodana RemoveExplicitTypeArguments)</name>
  <files>backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt, backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt, tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeView.kt, tooling/architecture/src/test/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeViewTest.kt, tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt, backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt, backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt, backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt, backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt, backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CapacityHarness.kt, backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngineTest.kt</files>
  <read_first>
    - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt (59 lines; line 44 is the call)
    - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt
    - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt (42 lines)
    - tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/ArchitectureScopes.kt (style of a public main declaration with KDoc)
    - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt (lines 106-175: the helper and the barrier copy)
    - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt (lines 106-212)
    - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt and CapacityHarness.kt (lines 157-177: ArrivalBarrier)
  </read_first>
  <behavior>
    - WR-03: KotlinTagResolverTest new case: a payload class declared in the test file with a private constructor property `@EventTag(key = Tags.COURSE) private val course: CourseId` resolves to exactly `setOf(Tag.of(Tags.COURSE, course.toString()))` (the TypeID text, not the raw UUID). Fails before the fix with kotlin-reflect's IllegalCallableAccessException.
    - WR-04 scanner (KotlinCodeViewTest, each input built so the marker text `KEEP` must survive and `GONE` must not): `//` inside `"http://host"` does not start a comment; `/*` inside `"**/*.kt"` does not start a block comment (code after it and before a later `*/` survives); nested block comments `/* a /* b */ GONE */` are removed entirely; an escaped quote `"a\"//b"` does not end the string; a raw string containing `//` and single quotes is one literal; a char literal `'"'` does not open a string; a backtick identifier with an apostrophe (`fun \`it's fine\`()`) does not open a char literal; a template `"${"/*"}"` keeps the template code visible and closes correctly; a multi-dollar string `$$"${NOT_A_TEMPLATE}"` treats `${` as content; string and comment contents are masked (a word inside a literal is absent from the view); the view has the input's length and its newline positions.
    - WR-04 rule (ContainerImageArchitectureTest, two new tests through a private `imageLiteralsIn(source)`): a source line `val health = "http://localhost:8024"; val image = DockerImageName.parse("axoniq/axonserver")` yields one violation (zero before the fix); a source with `val glob = "**/*.kt"` followed by a line with a `GenericContainer("postgres:18")` call and a later `/* comment */` yields one violation (zero before the fix). A third test: forbidden call shapes that sit only in a line comment, a block comment or inside a string literal yield none.
    - IN-01/IN-02/Qodana: refactor-only; the existing DecisionCaptureTest, DecisionCaptureEngineTest, ConditionalAppendIntegrationTest and DecisionCaptureIntegrationTest cases keep passing unchanged in what they assert.
  </behavior>
  <action>
**WR-03 (review text):** `property.getter.call(payload)` is invoked without `isAccessible = true`. For a private (or
otherwise non-public) Kotlin property, kotlin-reflect throws `IllegalCallableAccessException`. Axon's
`AnnotationBasedTagResolver`, which this class replaces (UP-0004), makes fields accessible, so an event payload that tags
a private property worked before the swap and would fail at publish time, in the append path of a command. The class's
documented contract is to be a drop-in replacement. Fix as the author decided: read non-public `@EventTag` properties.

1. RED: add the KotlinTagResolverTest case from the behavior block. Declare the payload as a private class in the test
   file with the private annotated constructor property; give the class an `override fun toString()` that reads the
   property, so the property is used in code (otherwise Qodana reports it unused and detekt reports an unused private
   property). Use `TestIds`/the existing `course` field for the value and `Tags.COURSE` for the key (architecture rule).
   Run it and see it fail.
2. GREEN in KotlinTagResolver.resolve: make the property accessible only when it carries at least one `@EventTag`
   (import `kotlin.reflect.jvm.isAccessible`; e.g. `property.apply { isAccessible = true }.getter.call(payload)` inside
   the existing `annotations.isEmpty()` branch). Never touch properties without the annotation: a JDK payload class such
   as `String` would throw `InaccessibleObjectException` under the module system, and the existing "plain string
   payload" test covers that path. If the new test then shows the raw UUID instead of the TypeID for the private
   value-class property (kotlin-reflect reading the backing field unboxed), box it through the property's value-class
   classifier and record this in the SUMMARY; the test must assert the TypeID text either way. Extend the class KDoc:
   non-public annotated properties are read too, as Axon's resolver does.
3. Commit: `fix(quick-261123-rzt): read non-public @EventTag properties in KotlinTagResolver (WR-03)`.

**WR-04 (review text):** `withoutComments` removes `//[^\n]*` and `/\*.*?\*/` with regexes that ignore string literals.
A `//` inside a string such as `"http://..."` removes the rest of that line, so a literal image argument on the same
line is not seen (`AxonServerEnvironmentIntegrationTest.kt:21` has such a line today). A string that contains `/*`,
such as a glob `"**/*.kt"`, starts a block-comment match that runs to the next `*/` anywhere later in the file and
deletes everything between. Both are false negatives of the rule that guards INFRA-09. Author decision: a small scanner
that tracks string state, with tests proving `"http://..."` and `"**/*.kt"` no longer hide a literal.

1. RED: create `tooling/architecture/src/test/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeViewTest.kt`
   (JUnit 5 + kotest assertions, block-bodied tests) with the scanner cases of the behavior block, and add the three
   rule tests to ContainerImageArchitectureTest. Write the rule tests' inputs as raw strings without any `$`; write
   scanner inputs that contain `"""` as regular escaped strings and inputs that contain `${` as `$$"..."` strings, so the
   test sources themselves are ordinary Kotlin. Because the new view masks literal contents, forbidden call shapes inside
   these test strings are invisible to the rule when it scans its own sources (dogfooding: a scanner bug shows up as a
   self-violation).
2. GREEN: create `tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeView.kt`, a
   public `object KotlinCodeView` with KDoc and one public function `of(source: String): String` that returns a copy of
   the text with the same length in which every character of a comment and of a literal's content is replaced by a
   space, except line breaks, which are kept; literal delimiters and all other code are kept verbatim. Scanner rules,
   in code context: `//` masks to the end of the line; `/*` masks to its matching `*/` with nesting (Kotlin block
   comments nest); a backtick starts a backtick identifier that is copied unchanged to the closing backtick on the same
   line (test names like `Axon's own resolver ...` contain apostrophes); `'` starts a char literal whose content is
   masked up to the closing `'`, honouring backslash escapes; `"""` starts a raw string and `"` a string, each preceded
   by an optional run of `$` that sets its interpolation prefix (default one `$`). Inside a string: a backslash masks
   itself and the next character, `"` ends it. Inside a raw string: no escapes, it ends at the last three quotes of a
   run of quotes. In both, exactly prefix-many `$` followed by `{` opens a template: the `${` stays, the template body is
   scanned in code context (nested strings, comments, chars) with brace depth tracking, and its matching `}` returns to
   the literal; any other `$` is content. An unterminated comment or literal masks to the end of the text. Structure it
   for detekt: a private scanner class with an explicit context stack and one small function per state, named
   constants instead of magic numbers (raw-string delimiter length), no deep nesting.
3. In ContainerImageArchitectureTest: delete the regex pre-pass and its two comment regexes; add a private
   `imageLiteralsIn(source: String): List<String>` in the class (or its companion) that applies `LITERAL_IMAGE_CALLS`
   to `KotlinCodeView.of(source)` and returns the trimmed matches; the main rule calls it per file (failure message
   unchanged). Update the class KDoc: comments and the contents of string and char literals are masked first, so
   documentation and test data may show the forbidden form.
4. Commit: `fix(quick-261123-rzt): mask comments and literals with a scanner in the image literal rule (WR-04)`.

**IN-01 (review text):** the magic-number suppression comment on `startApplication` in DecisionCaptureIntegrationTest
says the literal 12 "equals STUDENTS", yet `STUDENTS` is in scope; two constants that must stay equal are stated twice.
Fix: `pool = Executors.newFixedThreadPool(STUDENTS)` and delete the suppression annotation together with its two-line
comment.

**IN-02, test half (review text):** `ConditionalAppendIntegrationTest`'s private barrier class is a copy of the fixture
`ArrivalBarrier`, and the decision helper (source, wait at the barrier, append) is repeated in ConditionalAppendIntegrationTest
and DecisionCaptureTest. Fix:
1. Add to CommandServiceTestApplication a public member with KDoc, `fun submitDecision(executor: Executor, course: CourseId, student: StudentId, barrier: ArrivalBarrier): CompletableFuture<Throwable?>`,
   with the body both copies share today: on `executor`, in a new unit of work, source the events tagged
   `Tag.of(Tags.COURSE, course.toString())` through `eventStore.transaction(context)`, count them, `thenCompose` on
   `barrier.arrive()`, then append `StudentSubscribed(course, student)` with `EventAppender.forContext(context)`; wait
   for the unit of work inside the supplier with a private fixture timeout constant of 60 seconds, and return the
   failure (`runCatching { ... }.exceptionOrNull()`), `null` when it committed. No timeout parameter (each caller
   would pass one constant).
2. ConditionalAppendIntegrationTest: delete its private extension helper and its private barrier class, use
   `ArrivalBarrier(CONCURRENT_UNITS)` from `io.github.aanbrn.courses.command.fixtures` and
   `application.submitDecision(pool, course, student, barrier)`; update its class KDoc sentence about the barrier to name
   ArrivalBarrier. DecisionCaptureTest: delete its private extension helper and call
   `application.submitDecision(pool, course, StudentId.random(), barrier)`. Remove every import that becomes unused in
   both files (Qodana and detekt report unused imports).
3. Broaden the ArrivalBarrier KDoc in CapacityHarness.kt: it is the `beforeAppend` hook of a concurrency test and the
   barrier of `CommandServiceTestApplication.submitDecision`.

**Qodana RemoveExplicitTypeArguments** at DecisionCaptureEngineTest.kt:68: drop the explicit type argument of the
`listOf` call passed to `delegate.appendEvents(AppendCondition.none(), null, ...)`; `courseCreated()` already returns
`TaggedEventMessage<*>`, so the List overload is still chosen. The `TaggedEventMessage` import stays (used at line 82).

Commit IN-01, the IN-02 test half and the Qodana fix as:
`refactor(quick-261123-rzt): share the decision helpers of the command-service tests (IN-01, IN-02, Qodana)`.

Then run `mise x -- ./gradlew spotlessApply --console=plain` (commit any formatting with the change it belongs to)
and the verify command. The integration suite needs the Docker engine (colima) running.
  </action>
  <verify>
    <automated>mise x -- ./gradlew spotlessCheck :command-service:check :tooling:architecture:check --console=plain</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c 'isAccessible' backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt` [HEAD=0 EXPECT>=1]
    - `grep -cE 'LINE_COMMENT|BLOCK_COMMENT' tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt` [HEAD=3 EXPECT==0]
    - `grep -c 'KotlinCodeView' tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt` [HEAD=0 EXPECT>=1]
    - `grep -vE '^[[:space:]]*//' backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt | grep -c 'MagicNumber'` [HEAD=1 EXPECT==0]
    - `grep -c 'newFixedThreadPool(STUDENTS)' backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt` [HEAD=0 EXPECT==1]
    - `grep -c 'SourcingBarrier' backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt` [HEAD=4 EXPECT==0]
    - `grep -rh 'fun ExecutorService.submitDecision' backend/command-service/src | wc -l` [HEAD=2 EXPECT==0]
    - `grep -c 'fun submitDecision' backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt` [HEAD=0 EXPECT==1]
    - `grep -c 'listOf<TaggedEventMessage' backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngineTest.kt` [HEAD=1 EXPECT==0]
    - The new KotlinTagResolverTest case and the two "hidden literal" rule tests were seen failing before their fix (state it in the SUMMARY).
  </acceptance_criteria>
  <done>The private-property tag test, the scanner tests and the three rule tests pass; the image rule is green over the
  whole repository including its own test files; command-service unit and integration suites pass with the shared
  fixture helpers; three commits recorded with their SHAs.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: build-logic fixes (WR-02, WR-06, IN-02 build half, IN-03, Qodana MoveVariableDeclarationIntoWhen x3)</name>
  <files>build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheckFunctionalTest.kt, docs/adr/0012-dependency-licence-policy.md, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/JavaVersionFile.kt, build-logic/src/main/kotlin/courses.root.gradle.kts, build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ConventionsFunctionalTest.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/MarkdownOutline.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheckFunctionalTest.kt, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt</files>
  <read_first>
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt (lines 87-148: read and formatProblems)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheckFunctionalTest.kt (helpers at lines 190-239)
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt (lines 80-139)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
    - build-logic/src/main/kotlin/courses.root.gradle.kts (lines 220-233) and build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts (lines 23-32)
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt (lines 93-96, 160-208, 246-260) and UpstreamRecordCheck.kt (lines 362-371, 479-532), FrontMatter.kt (lines 108-124)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheckFunctionalTest.kt (line 262 test) and UpstreamRecordCheckFunctionalTest.kt (line 114 test)
  </read_first>
  <behavior>
    - WR-02 (DependencyLicenceCheckFunctionalTest, new case): with the policy literal `{ "allowed": ["Apache-2.0", "GPL-3.0-only"], "forbidden": ["GPL-*", "AGPL-*"] }` passed directly to `fixture(bom(component("lib", id("MIT"))), policy)`, the task fails and the output contains `policy.json is invalid` and `'GPL-3.0-only' is allowed, but matches a forbidden family`. Fails before the fix (task passes or fails for another reason).
    - WR-06 (ImageReferenceCheckFunctionalTest, new case): a catalog `[versions]` table holding the valid `image-axonserver` entry plus `image-literal = 'postgres:18.7@sha256:<DIGEST>'` (TOML literal string), `image-rich = { require = "postgres:18.7@sha256:<DIGEST>" }` (rich-version table) and `"image-quoted" = "postgres:18.7"` (quoted key, unpinned) fails; the output names `image-literal must be a double-quoted <name>:<tag>@sha256:<64 hex digits>`, the same for `image-rich`, and `image-quoted must be <name>:<tag>@sha256:<64 hex digits>, but is 'postgres:18.7'`; it does not name image-axonserver. Before the fix the three entries are silently skipped.
    - IN-02 build half (ConventionsFunctionalTest, characterization): with `.java-version` holding `temurin-25`, `runAndFail(":lib:build")` output contains `.java-version must hold a numeric JDK major version, but it holds 'temurin-25'` before and after the extraction.
    - IN-03 (AdrCheckFunctionalTest and UpstreamRecordCheckFunctionalTest, one new case each): a body that lacks a required section and appends a fenced block opened by a backtick fence, containing a `~~~` line and then the missing section heading, closed by a backtick fence, still reports `missing the section ...` (`Considered Options` for ADRs, `Root cause` for records). Fails before the fix because the `~~~` line closes the fence early and the heading is counted.
    - All existing TestKit cases of the five touched suites keep passing.
  </behavior>
  <action>
**WR-02 (review text):** the gate is membership of `allowed`; `forbidden` is used only to word the failure message.
`read` rejects an alias that resolves to a forbidden licence (`formatProblems`) but never checks `allowed` against
`forbidden`, so a policy edit that adds `"GPL-3.0-only"` to `allowed` is accepted although `forbidden` lists `GPL-*`.
ADR-0012 and the KDoc present the policy as one that refuses to be weakened. Fix:
1. RED: add the behavior case to DependencyLicenceCheckFunctionalTest with the policy as an inline string literal (do
   not add an `allowed` parameter to the `policy(...)` helper: one call site would make it a SameParameterValue
   finding).
2. GREEN: `formatProblems` takes the allowed identifiers as an additional parameter; `read` passes
   `strings(root, "allowed")` (compute it once and reuse it for the constructor). Add, after the alias problems, one
   problem per allowed identifier that any forbidden pattern fully matches, worded
   `'<id>' is allowed, but matches a forbidden family`. Extend the KDoc of `read` with this rule, and in
   docs/adr/0012-dependency-licence-policy.md (the "Exceptions per artifact" bullet, lines 63-65) extend the sentence
   that lists what makes the policy invalid with "an allowed identifier that matches a forbidden family". The real
   governance/dependency-licences.json stays valid (no allowed entry matches `GPL-*` or `AGPL-*`; patterns match the
   whole identifier, so `LGPL-*` would not match `GPL-*`).
3. Commit: `fix(quick-261123-rzt): reject an allowed licence that matches a forbidden family (WR-02)`.

**WR-06 (review text):** the catalog regex matches only `image-* = "value"`. An `image-*` key written as a TOML literal
string (`'value'`), an inline table, or a multi-line string does not match, so it is neither checked for the tag and
digest nor reported: the check fails open on exactly the shape it is meant to forbid, and the Renovate custom manager
also reads only the double-quoted form, so such an entry would never be updated. Fix (author: every `image-*` catalog
key is matched; a non-double-quoted value is reported):
1. RED: add the behavior case to ImageReferenceCheckFunctionalTest (multi-dollar or plain literals as the file already
   uses; `$DIGEST` interpolation stays a normal template). If Gradle itself refuses one of the shapes while importing
   the fixture catalog, drop that shape from the test and note in the SUMMARY that Gradle rejects it before the check
   runs.
2. GREEN in ImageReferenceCheck: replace the double-quoted-only entry regex with a key regex that matches every
   `image-*` key of the `[versions]` table, bare or quoted (`"image-x"` / `'image-x'`), and captures the rest of the line
   after `=`; a value regex accepts exactly a double-quoted string with an optional trailing `# comment`. A value that does
   not match is reported as `<path>: <key> must be a double-quoted <name>:<tag>@sha256:<64 hex digits>, but is '<raw value>'`;
   a double-quoted value is checked against `PINNED_REFERENCE` as today with the existing message. Keep the loop shape
   readable (a helper returning the problem or null via `when`). Update the class KDoc (catalog rule: double-quoted,
   tag and digest; any other value shape is reported) and the KDoc of the key regex.
3. Commit: `fix(quick-261123-rzt): report every image catalog key that is not a pinned double-quoted string (WR-06)`.

**IN-02, build half (review text):** the `.java-version` parsing, including its error message, is written out twice
in two convention plugins (courses.root.gradle.kts `updateDaemonJvm`, courses.kotlin-library.gradle.kts
`javaLanguageVersion`). Fix:
1. Add the ConventionsFunctionalTest characterization case of the behavior block (passes before and after).
2. Create `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/JavaVersionFile.kt` with a public top-level
   `fun readJavaMajorVersion(text: String): Int` and KDoc: trims the text, takes the part before the first `.`, returns it
   as an Int, and otherwise throws `GradleException` with the unchanged message
   `.java-version must hold a numeric JDK major version, but it holds '<trimmed text>'`.
3. In both convention plugins replace the inline parsing with `JavaLanguageVersion.of(readJavaMajorVersion(text))`
   inside the existing `providers.fileContents(...).asText.map { ... }`, importing
   `io.github.aanbrn.courses.buildlogic.readJavaMajorVersion`; keep the surrounding comments. Leave
   `DaemonJvmConsistencyCheck` and its own parser alone (different, tested messages with a regenerate hint; not in the
   author's scope).
4. Commit: `refactor(quick-261123-rzt): read .java-version through one build-logic function (IN-02)`.

**IN-03 (review text):** `outline` is the same function in AdrCheck (lines 192-207) and UpstreamRecordCheck
(lines 516-531), with heading classes that differ only by name. Both toggle the fenced state on any line that starts
with three backticks or three tildes, so a `~~~` line inside a backtick fence closes the fence early and a heading
shown in the rest of the example block is then counted. Fix (author: one shared scanner next to FrontMatter that closes
a fence only on a line starting with the same fence string):
1. RED: add the behavior case to both TestKit suites, modelled on the existing `a heading inside a code fence is not a
   section` tests.
2. GREEN: create `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/MarkdownOutline.kt`, an
   `internal object MarkdownOutline` with KDoc, a nested immutable `class Heading(val level: Int, val text: String, val line: Int, val content: Int)`
   and `fun headings(front: FrontMatter.Result): List<Heading>`. Semantics identical to today's scanner except the fence
   rule: body lines are numbered from `front.bodyStartLine`; text is compared after `trimEnd()`; outside a fence a line
   starting with `# ` is a level-1 and `## ` a level-2 heading; `content` counts the non-blank lines after a heading up
   to the next heading (fence lines and fenced lines count as content, as today). Fence rule: outside a fence, a line
   whose `trimStart()` begins with at least three backticks or three tildes opens a fence whose fence string is the full
   run of that character at the start; inside a fence, only a line whose `trimStart()` starts with that fence string
   closes it.
3. AdrCheck: `structure()` uses `MarkdownOutline.headings(front)`; delete its private outline function and its heading
   class. UpstreamRecordCheck: `RecordBody.check` does the same; delete its outline function and its heading class.
4. Commit: `fix(quick-261123-rzt): share one Markdown outline scanner that closes a fence only on its own fence (IN-03)`.

**Qodana MoveVariableDeclarationIntoWhen x3** (same files, separate commit): AdrCheck `Metadata.status()` becomes
`when (val target = SUPERSEDED.matchEntire(value)?.groupValues?.get(1)) { ... }` with the branch messages unchanged;
FrontMatter `Cursor.quoted()` becomes `when (val char = text[position++]) { ... }`; UpstreamRecordCheck
`date(key)` becomes `return when (val value = entry.value) { ... }` keeping the `is FrontMatter.Value.Text if isDate(value.value)`
guard. Commit: `refactor(quick-261123-rzt): move variable declarations into their when subjects (Qodana)`.

Then `mise x -- ./gradlew spotlessApply --console=plain` and the verify command. If `--tests` filters are not accepted
on the included-build task path, run `mise x -- ./gradlew :build-logic:functionalTest --console=plain` instead (about 9
minutes) and say so in the SUMMARY.
  </action>
  <verify>
    <automated>mise x -- ./gradlew spotlessCheck :build-logic:functionalTest --tests '*DependencyLicenceCheckFunctionalTest' --tests '*ImageReferenceCheckFunctionalTest' --tests '*ConventionsFunctionalTest' --tests '*AdrCheckFunctionalTest' --tests '*UpstreamRecordCheckFunctionalTest' --console=plain</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c 'matches a forbidden family' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt` [HEAD=0 EXPECT==1]
    - `grep -c 'must be a double-quoted' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt` [HEAD=0 EXPECT>=1]
    - `grep -c 'toIntOrNull' build-logic/src/main/kotlin/courses.root.gradle.kts` [HEAD=1 EXPECT==0] and the same for `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` [HEAD=1 EXPECT==0]
    - `grep -c 'readJavaMajorVersion' build-logic/src/main/kotlin/courses.root.gradle.kts` [HEAD=0 EXPECT>=1] and the same for courses.kotlin-library.gradle.kts [HEAD=0 EXPECT>=1]
    - `grep -c 'private fun outline' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt` [HEAD=1 EXPECT==0] and the same for UpstreamRecordCheck.kt [HEAD=1 EXPECT==0]
    - `grep -c 'MarkdownOutline' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt` [HEAD=0 EXPECT>=1] and the same for UpstreamRecordCheck.kt [HEAD=0 EXPECT>=1]
    - `grep -c 'when (val target' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt` [HEAD=0 EXPECT==1]
    - `grep -c 'when (val char' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt` [HEAD=0 EXPECT==1]
    - `grep -c 'when (val value = entry.value)' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt` [HEAD=0 EXPECT==1]
    - The WR-02, WR-06 and both IN-03 cases were seen failing before their fix (state it in the SUMMARY).
  </acceptance_criteria>
  <done>The five touched TestKit suites pass, including the new cases; the real repository's governance checks still pass
  (verified again by the full build in Task 3); five commits recorded with their SHAs.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: doctor removal advisory (IN-04), dispositions, final gates and the Qodana run</name>
  <files>scripts/doctor.sh, scripts/test-doctor.sh, docs/tooling.md, docs/adr/0013-upstream-records-and-workaround-markers.md, .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md</files>
  <read_first>
    - scripts/doctor.sh (lines 1-92 header, `report`, `local_only`; lines 561-645 check_claude_tracking, check_gradle, main)
    - scripts/test-doctor.sh (lines 1-200 helpers and the first cases; the `--ci` block and the tail)
    - docs/upstream/0012-docker-containerd-platform-export.md (front matter and the "## Removal criteria" section)
    - docs/tooling.md (section "Environment check (doctor)", lines 142-160)
    - docs/adr/0013-upstream-records-and-workaround-markers.md (Consequences, lines 86-96)
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md
  </read_first>
  <behavior>
    - Healthy test tree without docs/upstream: `^ok  upstream-removal: ` and exit 0.
    - A tracked record `docs/upstream/0042-sample.md` with `id: "UP-0042"`, `status: "fixed-upstream"` and a `## Removal criteria` section of two prose lines, plus a tracked `scripts/sample.sh` whose first line is a `#` comment carrying the marker for UP-0042: `^warn  upstream-removal: UP-0042 .*scripts/sample\.sh:1`, a `fix:` line with both criteria lines joined into one line, exit 0 (a warn never fails doctor), and the same in `--ci` mode.
    - The same record with `status: "open"`: no warn for UP-0042, `^ok  upstream-removal: `.
    - A fixed-upstream record whose ID appears only in prose (not after a comment opener), only under docs/templates/, only as a longer ID (UP-00421), or only in an untracked file: `^ok  upstream-removal: `.
    - The existing cases, including "the tree is unchanged after two runs" and "second run prints the same output", keep passing.
  </behavior>
  <action>
**IN-04 (review text):** the dependency-update advisory (`scripts/upstream/changed-dependencies.sh`) reports a record only
when its `product` equals a changed key of the catalog, `mise.toml`, the wrapper, `.java-version`, `.nvmrc` or a workflow
action. UP-0012 has `status: "fixed-upstream"` and `product: "docker"`, and UP-0011 has `product: "colima"`; neither
product has a pinned key, so the record whose status says that the workaround should now be removed is invisible to the
mechanism meant to surface it. Author decision: doctor prints the removal criteria of every docs/upstream record with
status fixed-upstream whose workaround markers still exist in the tree. Implementation choice (planner): a new check in
`scripts/doctor.sh`, because the Gradle side (`checkUpstreamRecords`) is a cacheable task whose warnings vanish on a
cache hit, and `./gradlew doctor` is skipped without a Java runtime.

1. RED: add a `# --- upstream-removal ---` section to scripts/test-doctor.sh with the behavior cases, using the existing
   `fresh`, `run_doctor`, `assert_out`, `assert_no_out`, `assert_code` helpers and `git -C "$ENV/repo" add` for tracked
   files. Write marker lines from a variable holding the marker word, e.g. `printf '# %s: UP-0042 sample workaround\n' "$MARK"`,
   never the marker word plus colon literally after a comment opener: checkUpstreamRecords scans this script and would
   report a marker for the non-existent UP-0042.
2. GREEN: add `check_upstream_removal` to scripts/doctor.sh, called in `main` after `check_claude_tracking` and before
   `check_gradle`, in both normal and `--ci` mode (it reads repository files only). Behaviour:
   - When `$ROOT` is not a git work tree: `report skip upstream-removal "not a git checkout, so the markers cannot be listed"`.
   - For each `$ROOT/docs/upstream/[0-9][0-9][0-9][0-9]-*.md` in sorted order, read `id` and `status` from the front
     matter between the first two `---` lines with awk (strip surrounding double quotes and `\r`). Only `fixed-upstream`
     records are considered.
   - Markers: `git -C "$ROOT" grep -n -E -e "<pattern>"` over the tracked files with the same scope as
     checkUpstreamRecords (pathspecs `*.kt *.kts *.sh *.yaml *.yml *.toml *.json *.json5 *.md *.properties *.xml .env.example NOTICE`
     and `:(exclude)` pathspecs for `docs/upstream`, `docs/templates`, `.planning`, `.claude`,
     `build-logic/src/functionalTest`), with the pattern mirroring its marker rule:
     `(^|[[:space:]])(//|/\*|\*|#|<!--|--)[[:space:]]*<WORD>: <id>([^0-9A-Za-z_]|$)`, where `<WORD>` comes from a readonly
     variable (same reason as in the tests). `git grep` exits 1 on no match; handle it explicitly under `set -euo pipefail`.
     Keep the `path:line` part of each hit.
   - Removal criteria: the non-blank lines between `## Removal criteria` and the next line starting with `#`, joined by
     single spaces into one line, `\r` removed.
   - Per record with markers: `report warn upstream-removal "<id> is fixed upstream, but its workaround markers remain: <path:line, path:line>" "removal criteria (<record path>): <joined text>" "when they hold, follow How to verify removal in that record, then remove the markers and set its status to workaround-removed"`.
   - When no record warned: `report ok upstream-removal "no fixed-upstream record has workaround markers left"`.
   Every printed line starts with the report prefix (`warn  ` or `      fix: `) and record text is collapsed to one line,
   so a record can never start a GitHub workflow command line in the CI log. Add a comment above the function stating
   that the marker pattern mirrors UpstreamRecordCheck's rule and that `checkUpstreamRecords` stays the authority on
   marker consistency. Update the header comment's "Which checks live where" list (the script's own checks) with the
   upstream-removal reminder.
3. Run `DOCTOR_SKIP_GRADLE=1 mise x -- bash scripts/doctor.sh --ci` in the worktree: it must print
   `warn  upstream-removal: UP-0012 is fixed upstream, but its workaround markers remain: scripts/doctor.sh:<n>, scripts/doctor.sh:<m>`
   followed by the UP-0012 removal criteria (other checks may warn or fail in this environment; only this line matters).
4. Docs: in docs/tooling.md "Environment check (doctor)" add the upstream-removal reminder to the list of the script's
   own checks and state that it also runs in `--ci`. In docs/adr/0013-upstream-records-and-workaround-markers.md add
   one Consequences bullet after the "advisory matches version keys" bullet: Good, because `scripts/doctor.sh` lists every
   fixed-upstream record whose markers remain, with its removal criteria, whatever its product, which covers the records
   the advisory cannot match. Wrap new prose like the surrounding lines (about 120 columns) and do not add Markdown
   links to repository paths beyond the existing style.
5. `mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh`, `mise x -- bash scripts/test-doctor.sh`, then
   commit: `feat(quick-261123-rzt): doctor lists fixed-upstream records whose workaround markers remain (IN-04)`.

**Final gates** (fix anything they report on its merits, inside the hard rules, and commit the fix with the finding it
belongs to):
6. `mise x -- ./gradlew build --console=plain` (about 8 minutes; Docker must be running), `mise x -- reuse lint`,
   `mise x -- editorconfig-checker`.
7. Disposition: edit .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md by hand. Front
   matter: WR-02, WR-03, WR-04, WR-06, IN-01, IN-02, IN-03, IN-04 get `disposition: fixed`; WR-01 gets
   `disposition: deferred`; IN-05 gets `disposition: skipped`; WR-05 stays `fixed`; `open: 0`; titles, `total`,
   `recorded` unchanged. Table rows: the Disposition column mirrors the front matter; each fixed row's Source cell
   follows the existing WR-05 row's pattern, `02-REVIEW.md — fixed by quick task 261123-rzt in <short SHA> (<what changed, one clause>)`,
   with both SHAs for IN-02 (test half from Task 1, build half from Task 2) and the IN-01 SHA from Task 1; the WR-01 and
   IN-05 Source cells hold the author's exact text from the objective. Commit:
   `docs(quick-261123-rzt): record the dispositions of the Phase 2 review`.
8. Qodana, after the last commit, from the worktree root: `mise x -- bash scripts/qodana.sh` (about 12 minutes; it
   archives the committed HEAD), then `jq '.runs[0].results | length' "$HOME/.cache/axon5-dcb-courses/qodana/results/qodana.sarif.json"`
   must print 0. If the CLI cannot reach `qodana.cloud` (timeout on `/api/versions`), wait a couple of minutes and retry
   at least twice; if it still fails, report the exact error lines, the attempts and the HEAD SHA in the SUMMARY and do
   not work around the network. If Qodana reports problems, list them (ruleId | location | message), fix each on its
   merits in a separate commit `fix(quick-261123-rzt): resolve Qodana findings in the review fixes`, re-run the
   affected Gradle checks and Qodana, and repeat until 0.
9. SUMMARY (`.planning/quick/261123-rzt-fix-phase-2-review-findings-wr-02-wr-03-/261123-rzt-SUMMARY.md`): a disposition
   table per finding (ID, fix, commit SHA), the RED evidence per test, the Qodana result count and HEAD, the gate
   results, and every judgement call taken during execution.
  </action>
  <verify>
    <automated>mise x -- bash scripts/test-doctor.sh && mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh && mise x -- ./gradlew build --console=plain && mise x -- reuse lint && mise x -- editorconfig-checker && mise x -- bash scripts/qodana.sh && test "$(jq '.runs[0].results | length' "$HOME/.cache/axon5-dcb-courses/qodana/results/qodana.sarif.json")" = 0</automated>
  </verify>
  <acceptance_criteria>
    - `grep -c 'upstream-removal' scripts/doctor.sh` [HEAD=0 EXPECT>=2]
    - `grep -c 'upstream-removal' scripts/test-doctor.sh` [HEAD=0 EXPECT>=3]
    - `grep -c 'disposition: open' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md` [HEAD=10 EXPECT==0]
    - `grep -c '^open: 0$' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md` [HEAD=0 EXPECT==1]
    - `grep -cE '^\| (WR|IN)-0[0-9] \| (warning|info) \| open \|' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md` [HEAD=10 EXPECT==0]
    - `git diff --quiet ed1980c..HEAD -- qodana.yaml config/detekt; echo $?` [HEAD=0 EXPECT==0 @ed1980c GUARD MUTATION: any edit to qodana.yaml or to a file under config/detekt makes it print 1]
    - `d=$(git diff ed1980c..HEAD -- '*.kt' '*.kts') && printf '%s\n' "$d" | grep -cE '^\+.*(@Suppress|noinspection)'` [HEAD=0 EXPECT==0 @ed1980c GUARD MUTATION: adding one suppression annotation line to any Kotlin file makes it print 1]
    - `f=$(git ls-files) && printf '%s\n' "$f" | grep -ciE '(qodana|detekt).*baseline'` [HEAD=0 EXPECT==0 @ed1980c GUARD MUTATION: committing a file named qodana-baseline.json makes it print 1]
    - `jq '.runs[0].results | length' "$HOME/.cache/axon5-dcb-courses/qodana/results/qodana.sarif.json"` [HEAD=4 EXPECT==0 @ed1980c] (the cached SARIF of the run that found the 4 leftovers; the gate reads the run on the final committed HEAD)
  </acceptance_criteria>
  <done>doctor warns about UP-0012 with its removal criteria and the shell tests prove all cases; build, REUSE,
  editorconfig, shellcheck and the shell tests pass; the disposition file has `open: 0` with SHAs; Qodana reports 0
  problems on the final HEAD (or the SUMMARY reports the exact network failure after retries).</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| repository policy files -> governance checks | `governance/dependency-licences.json` and `gradle/libs.versions.toml` are edited in pull requests; the checks are the machine-side guard of their rules |
| Kotlin sources -> architecture rule | any test or production file may contain text that looks like a comment or a call; the rule must not fail open on it |
| docs/upstream records -> doctor output -> CI log | record text is repository-controlled and printed by `doctor.sh --ci` into a GitHub Actions log |
| event payload classes -> KotlinTagResolver reflection | the resolver now opens non-public annotated members |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-rzt-01 | Tampering | LicencePolicy.read | high | mitigate | WR-02: an allowed identifier matching a forbidden pattern makes the policy invalid; TestKit case proves it |
| T-rzt-02 | Tampering | ImageReferenceCheck catalog rule | high | mitigate | WR-06: every `image-*` key is checked, any non-double-quoted value is reported, so an unpinned image cannot hide in another TOML shape |
| T-rzt-03 | Tampering | ContainerImageArchitectureTest | medium | mitigate | WR-04: literal-aware scanner; tests for the two known fail-open shapes and a self-scan of the rule's own test data |
| T-rzt-04 | Spoofing | doctor.sh upstream-removal output in CI | medium | mitigate | record text collapsed to one line with CR removed and always printed after the fixed `warn  ` / `      fix: ` prefix, so no line can begin a workflow command; nothing secret is read |
| T-rzt-05 | Elevation of Privilege | KotlinTagResolver isAccessible | low | mitigate | accessibility is set only on properties that carry `@EventTag`; JDK and unannotated members are never opened |
| T-rzt-06 | Repudiation | Qodana/detekt gate configuration | high | mitigate | diff gates prove qodana.yaml and config/detekt unchanged, no suppression added, no baseline tracked |
| T-rzt-07 | Tampering | doctor.sh / test-doctor.sh marker text | low | mitigate | marker word built from a variable, so the scripts never carry a false marker that checkUpstreamRecords would have to resolve |
| T-rzt-SC | Tampering | npm/pip/cargo installs | low | accept | no package is installed or upgraded by this plan; no package-manager install task exists |
</threat_model>

<verification>
- Task verifies: `mise x -- ./gradlew spotlessCheck :command-service:check :tooling:architecture:check`, the focused
  build-logic TestKit run, `mise x -- bash scripts/test-doctor.sh`, shellcheck.
- Final: `mise x -- ./gradlew build`, `mise x -- reuse lint`, `mise x -- editorconfig-checker`, and
  `mise x -- bash scripts/qodana.sh` on the final committed HEAD with 0 SARIF results.
- Audit gates of Task 3 (no suppressions, no baseline, qodana.yaml and config/detekt untouched since ed1980c).
</verification>

<success_criteria>
- WR-02, WR-03, WR-04, WR-06, IN-01, IN-02, IN-03, IN-04 fixed, each with a test that fails without the fix (IN-01 and
  IN-02 are refactors guarded by the existing suites plus one characterization case).
- WR-01 deferred and IN-05 skipped with the author's exact Source text; `open: 0`.
- The 4 Qodana leftovers fixed; Qodana reports 0 problems; `./gradlew build` green.
</success_criteria>

<output>
Create `.planning/quick/261123-rzt-fix-phase-2-review-findings-wr-02-wr-03-/261123-rzt-SUMMARY.md` when done
</output>
