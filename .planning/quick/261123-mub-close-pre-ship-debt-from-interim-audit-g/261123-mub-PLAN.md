---
phase: quick-261123-mub
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt
  - build-logic/src/main/kotlin/courses.governance.gradle.kts
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt
  - gradle/libs.versions.toml
  - governance/governed-paths.txt
  - .claude/settings.json
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt
  - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md
  - .planning/REQUIREMENTS.md
  - .planning/phases/01-repository-build-ci-foundation/01-VERIFICATION.md
  - .planning/STATE.md
autonomous: true
# Every ID listed here ends this plan as Complete. CICD-09, UPS-02, UPS-03 and TOOL-04 are deliberately absent:
# they stay Pending (Task 3), so an automatic requirements.mark-complete over this list cannot flip them.
requirements: [AIDEV-06, INFRA-09, INFRA-02, INFRA-03, INFRA-04, INFRA-05, INFRA-06, IDEM-04, TEST-11, TEST-12, TEST-14, DOC-03, GH-12, UPS-01, UPS-04, TOOL-03, AIDEV-01, AIDEV-03, AIDEV-04, AIDEV-07]

estimate:
  tokens: 120000
  raw_tokens: 120000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "Neither courses.governance nor courses.root names an individual container image any more: both call one shared ComposeVersions function that derives the compose images from every `image-*` version alias of the catalog, minus an explicit, KDoc-documented test-only set that holds `ryuk`"
    - "A catalog with a third compose image entry (`image-otel-lgtm`) makes generateComposeVersions write an `OTEL_LGTM_IMAGE=` line and makes checkComposeVersions fail on the old two-image file and pass on the regenerated one, proven by a TestKit test that runs with --configuration-cache --isolated-projects"
    - "compose/versions.env stays byte-identical: generateComposeVersions on the real repository leaves `git diff --exit-code compose/versions.env` clean and checkComposeVersions passes"
    - "governance/governed-paths.txt holds 58 entries, including qodana.yaml and scripts/qodana.sh under a rationale block; .claude/settings.json has exactly one Edit ask rule for each, checkClaudeAskRules passes and the governance shell tests pass; NOTICE, docs/adr, docs/upstream, compose.yaml and compose/** stay ungoverned and are named, with the reason, in the file's 'outside the boundary' list"
    - "The second DecisionCaptureIntegrationTest case asserts that exactly SEATS attempts committed before it inspects them, and passes against a real Axon Server; WR-05 is `fixed` in 02-REVIEW-DISPOSITION.md with the SHA of the commit that fixed it"
    - "REQUIREMENTS.md shows the 18 Phase 2 IDs verified SATISFIED as Complete and [x]; CICD-09, UPS-02, UPS-03 and TOOL-04 stay Pending with a parenthesised note and [ ]; 01-VERIFICATION.md frontmatter status is human_needed; STATE.md Current Position says 23 of 23 plans executed, verification human_needed, UAT pending"
    - "`mise x -- ./gradlew build` passes on the final tree"
  artifacts:
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt"
      provides: "The one derivation of compose images from the catalog (images) next to the one rendering (render), with the documented test-only exclusion"
      contains: "TEST_ONLY_IMAGES"
    - path: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt"
      provides: "TestKit proof that a third image-* entry is rendered and checked, and that ryuk is not"
      contains: "image-otel-lgtm"
    - path: "governance/governed-paths.txt"
      provides: "Governed entries for the Qodana gate configuration and launcher, and the documented data-file exclusions"
      contains: "scripts/qodana.sh"
    - path: ".claude/settings.json"
      provides: "Ask rules in parity with the governed list"
      contains: "Edit(/scripts/qodana.sh)"
    - path: "backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt"
      provides: "Non-vacuous committed-attempt assertion (WR-05)"
      contains: "recorder.committed() shouldHaveSize SEATS"
  key_links:
    - from: "build-logic/src/main/kotlin/courses.governance.gradle.kts"
      to: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt"
      via: "checkComposeVersions `expected` is ComposeVersions.render(ComposeVersions.images(<libs catalog>))"
    - from: "build-logic/src/main/kotlin/courses.root.gradle.kts"
      to: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt"
      via: "generateComposeVersions `images` is set from ComposeVersions.images(<libs catalog>)"
    - from: "gradle/libs.versions.toml"
      to: "compose/versions.env"
      via: "every [versions] alias whose first segment is `image`, except the test-only set, becomes one <NAME>_IMAGE line"
    - from: ".claude/settings.json"
      to: "governance/governed-paths.txt"
      via: "checkClaudeAskRules: exactly one Edit(/<entry>) ask rule per governed entry"
---

<objective>
Close the pre-ship debt of the interim milestone audit (`.planning/v1-phase-02-MILESTONE-AUDIT.md`) on the current
branch `gsd/phase-2-runtime-infrastructure-dcb-spikes`, so it ships with the Phase 2 pull request. No new branch.

1. INT-W2: the compose image set is derived from the version catalog in one shared function instead of being hardcoded
   twice, with a TestKit proof that a third `image-*` entry is rendered and checked.
2. INT-W1: `qodana.yaml` and `scripts/qodana.sh`, the configuration and launcher of the required `qodana` gate in
   `ci-success`, become governed paths with matching ask rules. The data files the checks read stay outside on purpose.
3. WR-05 (02-REVIEW.md): the second `DecisionCaptureIntegrationTest` case can no longer pass with no committed record.
4. Bookkeeping: REQUIREMENTS.md Phase 2 statuses, the 01-VERIFICATION.md frontmatter status, the STATE.md current
   position, and the WR-05 disposition.

Purpose: the phase PR should not carry known governance-boundary gaps, a fragile freshness check, a test that can pass
vacuously, or stale status documents into review.
Output: one shared compose-image derivation with a TestKit test, two new governed entries and ask rules, one added
assertion, refreshed planning documents, and a green `./gradlew build`.

Decision made here (Claude's discretion, explicitly allowed by the task): the catalog holds a `image-*` entry that no
compose file uses, `image-ryuk` (Testcontainers' Ryuk reaper, pinned for test JVMs through
TESTCONTAINERS_RYUK_CONTAINER_IMAGE in courses.test-suites). The derivation therefore **filters** through an explicit
test-only set (`ryuk`) and includes every other `image-*` entry by default. The default-include direction is fail-safe:
a new compose image is covered without editing the build logic, and only a deliberate, reviewable opt-out removes an
entry. Including ryuk instead would change compose/versions.env (breaking the byte-identity requirement) and put an
image no compose service runs into compose's environment. The Qodana image is pinned in qodana.yaml, not in the
catalog (verified), so it is not affected.

Not in scope (stays as it is): the image map in courses.test-suites (`useContainerImages`), which feeds test JVMs, not
compose; WR-01..WR-04, WR-06 and IN-01..IN-05; CICD-04 and GH-06 statuses; the milestone audit file itself.
</objective>

<execution_context>
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/workflows/execute-plan.md
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/v1-phase-02-MILESTONE-AUDIT.md
@.claude/CLAUDE.md

Interfaces the executor works against (extracted, do not re-derive):

- `ComposeVersions` (object, build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt):
  `const val HEADER`, `fun render(images: Map<String, String>): String` (sorts by key, emits
  `<KEY uppercased>_IMAGE=<reference>` lines after HEADER, trailing newline). Same file:
  `abstract class GenerateComposeVersions` with `@get:Input abstract val images: MapProperty<String, String>` and
  `@get:OutputFile abstract val target: RegularFileProperty`.
- `GeneratedFileCheck` task (governance package) used by `checkComposeVersions`: properties `expected: Property<String>`,
  `actual`, `fixCommand`, `report`. Stale message: `compose/versions.env is stale; run ./gradlew generateComposeVersions`.
- Test fixture API (build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt):
  `GradleFixture.standard(projectDir)` (root applies `courses.root`, includes `:platform` and `:lib`),
  `GradleFixture.governance(projectDir)`, `fixture.file(path, content)`, `fixture.read(path)`, `fixture.run(...)`,
  `fixture.runAndFail(...)`, `GradleFixture.HEADER`, `GradleFixture.PASSED`, and the extension
  `GradleFixture.changeCatalog { text -> newText }` (copies the repository catalog into the fixture, rewrites it, and
  requires the transformation to change the text). Every run passes `--configuration-cache --isolated-projects`.
- The repository catalog path reaches functional tests as the system property `catalog`; the existing
  `GeneratedFileCheckFunctionalTest.expectedVersions()` shows how to read image references from it with
  `Regex("""(?m)^image-$name = "([^"]+)"$""")`.
- Catalog image entries today (gradle/libs.versions.toml lines 26-32): `image-axonserver`, `image-postgres`,
  `image-ryuk`, preceded by a 4-line comment.
- Gradle facts (Context7, Gradle docs): a TOML version given as a single string is a *required* version; catalog
  aliases may use `-` or `_` separators and are normalised to dot notation.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: End-to-end shared compose-image derivation from every image-* catalog entry (INT-W2)</name>
  <files>build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt, build-logic/src/main/kotlin/courses.governance.gradle.kts, build-logic/src/main/kotlin/courses.root.gradle.kts, build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt, gradle/libs.versions.toml</files>
  <read_first>
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt (whole file, about 60 lines)
    - build-logic/src/main/kotlin/courses.governance.gradle.kts (the `libs` accessor near line 20 and checkComposeVersions near lines 104-118)
    - build-logic/src/main/kotlin/courses.root.gradle.kts (generateComposeVersions near lines 198-208; the `libs` accessor near line 13 stays, it has other uses)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt (standard, changeCatalog, HEADER, PASSED)
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GeneratedFileCheckFunctionalTest.kt (expectedVersions helper and test naming style)
    - gradle/libs.versions.toml lines 26-32
  </read_first>
  <behavior>
    - With the unchanged repository catalog, `generateComposeVersions` writes exactly HEADER, `AXONSERVER_IMAGE=<image-axonserver>` and `POSTGRES_IMAGE=<image-postgres>` (no RYUK line), equal to the text built from the catalog with the regex helper.
    - After `changeCatalog` adds `image-otel-lgtm = "grafana/otel-lgtm:0.41.1@sha256:<64 hex>"` right after the `image-ryuk` line, `checkComposeVersions` fails on the old two-image versions file with the stale message, `generateComposeVersions` writes an `OTEL_LGTM_IMAGE=grafana/otel-lgtm:0.41.1@sha256:...` line between the AXONSERVER and POSTGRES lines, and `checkComposeVersions` then passes (outcome in GradleFixture.PASSED).
    - A versions file holding the correct two-image text plus a `RYUK_IMAGE=<image-ryuk>` line is reported stale by `checkComposeVersions`, which documents the test-only exclusion.
  </behavior>
  <action>
Implements INT-W2 with the filter decision recorded in the objective. TDD order: write the functional test first, run
it and see the third-image case fail on the current hardcoded build logic, then implement, then run it green.

1. Functional test (new file build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt,
   package `io.github.aanbrn.courses.buildlogic`, SPDX header like every test, class KDoc in the style of
   GeneratedFileCheckFunctionalTest, prose backticked test names, Kotest assertions). Fixture:
   `GradleFixture.standard(projectDir)`, then overwrite `build.gradle.kts` with HEADER plus a `plugins` block applying
   `courses.root` and `courses.governance`, which is what the real root build.gradle.kts applies. Write the three cases
   from `<behavior>`. Build expected texts from HEADER plus `<NAME>_IMAGE=<reference>` lines, reading references from
   the fixture's current catalog file (`fixture.catalog`) with the same regex style as
   GeneratedFileCheckFunctionalTest. Build the fake digest in code (`"sha256:" + "a".repeat(64)`) and insert the
   new entry with `changeCatalog` by matching the `image-ryuk` line. Do not edit GradleFixture.kt or
   GeneratedFileCheckFunctionalTest.kt.

2. ComposeVersions.kt: add to the `ComposeVersions` object
   - `TEST_ONLY_IMAGES: Set<String>` holding `ryuk`, with KDoc: these `image-*` entries are pinned for test JVMs only
     (ryuk is Testcontainers' resource reaper, set through TESTCONTAINERS_RYUK_CONTAINER_IMAGE by courses.test-suites),
     no compose file runs them, so they are filtered out; every other entry is a compose image by default, so a new
     compose image is rendered and checked without a build-logic change, and excluding one is a visible, reviewed edit
     of this set.
   - `fun images(catalog: VersionCatalog): Map<String, String>` (import `org.gradle.api.artifacts.VersionCatalog`).
     Walk `catalog.versionAliases`; split each alias on `.`, `-` and `_` so the result does not depend on Gradle's
     dot normalisation; keep aliases whose first segment is exactly `image` and that have at least one more segment;
     the short name is the remaining segments joined with `_` (so `image-otel-lgtm` becomes `otel_lgtm`, rendered as
     `OTEL_LGTM_IMAGE`, which matches the `[A-Z0-9_]+_IMAGE` variable form the compose files, ImageReferenceCheck and
     the Renovate regex manager already use); drop names in TEST_ONLY_IMAGES; the value is
     `catalog.findVersion(alias).get().requiredVersion`. Throw a GradleException naming the alias when the required
     version is blank (a rich version such as `strictly`), since a compose image must be a plain tag-and-digest string.
     KDoc states: the derivation, the short-name rule, the exclusion and why, and that this is the single source for
     both generateComposeVersions and checkComposeVersions, so they cannot disagree on the image set (as `render`
     already guarantees for the text).
   - Update the object KDoc and the GenerateComposeVersions KDoc: replace the sentence naming
     `${AXONSERVER_IMAGE}` and `${POSTGRES_IMAGE}` with the generic `${<NAME>_IMAGE}` form fed by every compose image
     of the catalog.

3. courses.governance.gradle.kts: in checkComposeVersions replace the `zip` of the two type-safe image version
   accessors with `expected.set(ComposeVersions.render(ComposeVersions.images(catalog)))`, where `catalog` is the
   `libs` catalog obtained through `the<VersionCatalogsExtension>().named("libs")` (import
   `org.gradle.api.artifacts.VersionCatalogsExtension`). Compute it at configuration time as a plain value: catalog
   contents are settings-level constants and a changed catalog already invalidates the configuration cache, so no
   provider wrapper is needed and nothing non-serialisable is captured. Remove the `val libs = the<...LibrariesForLibs>()`
   line if nothing else in the file uses it (today the image accessors are its only use), so no unused declaration is
   left. Rewrite the comment above the task: the expected text is the rendering of every compose image of the catalog
   through ComposeVersions.images, the same function generateComposeVersions (courses.root) uses.

4. courses.root.gradle.kts: in generateComposeVersions replace the two individual `images.put(...)` lines with one
   `images.set(ComposeVersions.images(<libs catalog via VersionCatalogsExtension>))` (import ComposeVersions and
   VersionCatalogsExtension). Keep `group`, `description` and `target`. Adjust the comment above it to say the image
   set comes from ComposeVersions.images. Keep the existing `libs` accessor, which other code in this file uses.

5. gradle/libs.versions.toml: rewrite only the comment block above the `image-*` entries (lines 26-29) so it states the
   current rule: the entries reach the test tasks through ContainerImageArguments, and every `image-*` entry except the
   test-only ones listed in ComposeVersions.TEST_ONLY_IMAGES (today `ryuk`) also reaches Docker Compose through the
   generated compose/versions.env; keep the Ryuk/TESTCONTAINERS_RYUK_CONTAINER_IMAGE sentence. Drop the stale "from
   Phase 6" remark (compose already uses the axonserver image). Keep lines at most 120 characters. Do not change any
   entry value.

6. Run `mise x -- ./gradlew spotlessApply --console=plain` if formatting differs, then the verify commands. Commit as
   one commit, for example `fix(build-logic): derive compose images from every image-* catalog entry`.

courses.governance.gradle.kts and courses.root.gradle.kts are governed paths: Claude Code will ask before each edit,
which is expected (the author approves).
  </action>
  <verify>
    <automated>mise x -- ./gradlew :build-logic:functionalTest --tests '*ComposeVersionsFunctionalTest*' --tests '*GeneratedFileCheckFunctionalTest*' --console=plain && mise x -- ./gradlew generateComposeVersions checkComposeVersions checkImageReferences checkBuildScripts --console=plain && git diff --exit-code compose/versions.env</automated>
    Gates (run from the checkout root):
    - `grep -v '^[[:space:]]*//' build-logic/src/main/kotlin/courses.governance.gradle.kts | grep -c 'libs.versions.image'` [HEAD=1 EXPECT==0 @669c3f0]
    - `grep -v '^[[:space:]]*//' build-logic/src/main/kotlin/courses.root.gradle.kts | grep -c 'libs.versions.image'` [HEAD=2 EXPECT==0 @669c3f0]
    - `grep -c 'ComposeVersions.images(' build-logic/src/main/kotlin/courses.governance.gradle.kts` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c 'ComposeVersions.images(' build-logic/src/main/kotlin/courses.root.gradle.kts` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c 'TEST_ONLY_IMAGES' build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt` [HEAD=0 EXPECT>=2 @669c3f0]
    - `grep -c '<testcase' build-logic/build/test-results/functionalTest/TEST-io.github.aanbrn.courses.buildlogic.ComposeVersionsFunctionalTest.xml` [HEAD=error:2 EXPECT>=3 @669c3f0]
    - `grep -c '<failure' build-logic/build/test-results/functionalTest/TEST-io.github.aanbrn.courses.buildlogic.ComposeVersionsFunctionalTest.xml` [HEAD=error:2 EXPECT==0 @669c3f0]
    - `git diff --exit-code compose/versions.env` after generateComposeVersions [HEAD=0 EXPECT==0 @669c3f0 GUARD MUTATION: drop the ryuk filter from ComposeVersions.images, then generateComposeVersions adds a RYUK_IMAGE line and the diff exits 1] (exit status; invariance gate for the byte-identity requirement)
  </verify>
  <done>Both scripts call ComposeVersions.images and name no image; the new TestKit class passes with at least three cases under --configuration-cache --isolated-projects (third image rendered and checked, ryuk excluded, repository catalog unchanged in output); the existing GeneratedFileCheckFunctionalTest still passes; compose/versions.env is unchanged after regeneration; the change is committed.</done>
</task>

<task type="auto">
  <name>Task 2: Govern the Qodana gate files (INT-W1) and make the second decision-capture case non-vacuous (WR-05)</name>
  <files>governance/governed-paths.txt, .claude/settings.json, backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt</files>
  <precondition>The Docker engine (colima) is reachable: `docker info` exits 0; the integration test starts Axon Server through Testcontainers.</precondition>
  <read_first>
    - governance/governed-paths.txt (whole file: header with the boundary rule and the 'Outside the boundary, on purpose' list, then the entry blocks; the `config/detekt/**` block is the model for the new one)
    - .claude/settings.json (permissions.ask; `Edit(/config/detekt/**)` sits just before `Edit(/gradlew)`)
    - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt lines 70-115 (both test cases)
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW.md section WR-05 (lines 265-272)
  </read_first>
  <action>
Part A, INT-W1 (author decision in the task: govern exactly these two files).

1. governance/governed-paths.txt: add a new block directly after the `config/detekt/**` block and before the launcher
   block. Comment, in the file's voice: the Qodana gate, which the required `qodana` job of ci-success runs; its
   configuration (the digest-pinned image, the inspection profile, the exclusions and the fail threshold) and the
   launcher that CI and developers run it with; an edit to either alone can switch inspections off or make the job pass
   without analysing. Entries, one per line: `qodana.yaml` and `scripts/qodana.sh`.
2. Same file, the 'Outside the boundary, on purpose' list in the header: add one bullet (keep the list's `;` style)
   naming the files the checks read as data rather than as configuration: NOTICE (checkNotice), docs/adr/**
   (checkAdrs), docs/upstream/** (checkUpstreamRecords), compose.yaml and compose/** (checkComposeVersions,
   checkImageReferences and the compose-smoke job). Reason: an edit to them is what those checks evaluate, so it cannot
   switch a check off or make one pass without evaluating. Do NOT add any of them as entries (author decision).
   <!-- planner-discipline-allow: NOTICE -->
3. .claude/settings.json: add `"Edit(/qodana.yaml)"` and `"Edit(/scripts/qodana.sh)"` to `permissions.ask`,
   directly after `"Edit(/config/detekt/**)"`, mirroring the list order (AskRulesCheck ignores order; mirroring keeps
   review easy). Change nothing else in the file.
4. No test or document pins the entry count (checked at planning time: the only "54"/"56"/"57" counts live in
   historical .planning SUMMARY/STATE text, which stays as written; scripts/governance/test-*.sh read the real list but
   assert behaviour, not its size). Leave them untouched.
5. Commit, for example `chore(governance): govern the Qodana gate configuration and launcher`.

Both files are governed: Claude Code asks before each edit, which is expected.

Part B, WR-05.

6. In the second case (`a committed attempt records what it saw, what it asked the store and what it appended`),
   insert `recorder.committed() shouldHaveSize SEATS` on its own line directly after the `subscribeConcurrently(course)`
   call and before the `recorder.committed().forEach` loop, exactly as 02-REVIEW.md WR-05 prescribes. `shouldHaveSize`
   is already imported. Change nothing else.
7. Commit on its own, for example `test(command-service): fail decision capture when no attempt committed`, and note
   the short SHA of this commit (`git log -1 --format=%h`) for Task 3.

Observation only, no change: while editing the governed list, note in the SUMMARY which other configuration files of
required ci-success jobs sit outside the boundary (candidates seen at planning time: `.editorconfig-checker.json`,
`REUSE.toml`, `.editorconfig`, `.prettierrc.yaml`, `.taplo.toml`, `config/spotless/**`, `scripts/doctor.sh`; confirm
each against .github/workflows/ci.yml) so the author can decide on them later. Do not govern them in this task.
  </action>
  <verify>
    <automated>mise x -- ./gradlew checkClaudeAskRules --console=plain && mise x -- bash scripts/governance/test-check-pr.sh && mise x -- bash scripts/governance/test-list-pr-files.sh && mise x -- ./gradlew :command-service:integrationTest --tests '*DecisionCaptureIntegrationTest*' --console=plain</automated>
    Gates (run from the checkout root):
    - `grep -v '^#' governance/governed-paths.txt | grep -c -v '^[[:space:]]*$'` [HEAD=56 EXPECT==58 @669c3f0]
    - `grep -c '"Edit(/' .claude/settings.json` [HEAD=56 EXPECT==58 @669c3f0]
    - `grep -c -x 'qodana.yaml' governance/governed-paths.txt` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c -x 'scripts/qodana.sh' governance/governed-paths.txt` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c -F '"Edit(/qodana.yaml)"' .claude/settings.json` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c -F '"Edit(/scripts/qodana.sh)"' .claude/settings.json` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c -x -E 'NOTICE|docs/adr/.*|docs/upstream/.*|compose\.yaml|compose/.*' governance/governed-paths.txt` [HEAD=0 EXPECT==0 @669c3f0 GUARD MUTATION: add compose/** as a governed entry, which counts 1] (invariance gate for the author's exclusion decision; whole-line match, so the new comment bullet does not count)
    - `grep -c 'recorder.committed() shouldHaveSize SEATS' backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt` [HEAD=1 EXPECT==2 @669c3f0]
  </verify>
  <done>The governed list has 58 entries including the two Qodana files under a rationale block, the 'outside' list names the data files with the reason, the ask rules are in parity (checkClaudeAskRules green), both governance shell tests pass, DecisionCaptureIntegrationTest passes with the added count assertion, and the two changes are two separate commits; the WR-05 commit SHA is noted.</done>
</task>

<task type="auto">
  <name>Task 3: Record WR-05 as fixed, refresh the planning bookkeeping, run the full gate</name>
  <files>.planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md, .planning/REQUIREMENTS.md, .planning/phases/01-repository-build-ci-foundation/01-VERIFICATION.md, .planning/STATE.md</files>
  <read_first>
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md (whole file)
    - .planning/phases/01-repository-build-ci-foundation/01-REVIEW-DISPOSITION.md lines 92-115 (wording of `fixed` Source cells)
    - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-VERIFICATION.md lines 170-200 (Requirements Coverage table)
    - .planning/REQUIREMENTS.md: the requirement list lines 10-30, 79-90, 124-160, 164-218 for the checkboxes, and the Traceability table from line 267 to the end
    - .planning/phases/01-repository-build-ci-foundation/01-VERIFICATION.md lines 1-5 and line 91
    - .planning/STATE.md section `## Current Position` (lines 28-35)
  </read_first>
  <action>
1. 02-REVIEW-DISPOSITION.md (WR-05): in the frontmatter set the WR-05 entry's `disposition` to `fixed` and lower the
   `open` counter from 11 to 10 (`total` stays). In the table set the WR-05 row to
   `| WR-05 | warning | fixed | 02-REVIEW.md — fixed by quick task 261123-mub in <short SHA from Task 2> (committed-count assertion before the loop) |`.
   Leave every other finding `open`. Do not touch the `recorded` timestamp.

2. REQUIREMENTS.md, the 15 IDs 02-VERIFICATION.md lists as SATISFIED and that are still Pending: INFRA-02, INFRA-03,
   INFRA-04, INFRA-05, INFRA-06, IDEM-04, TEST-11, TEST-12, TEST-14, DOC-03, GH-12, UPS-01, UPS-04, TOOL-03, AIDEV-07.
   For each, set the traceability Status cell to `Complete` and flip its `- [ ] **ID**` checkbox to `- [x]`. AIDEV-01,
   AIDEV-03 and AIDEV-04 are already Complete and [x]; leave them. Edit by hand (the GSD helper
   `requirements.mark-complete` would do the same two edits, but hand edits keep the worktree self-contained).

3. REQUIREMENTS.md, the four that stay Pending with an unchecked box. GSD reads the leading status word, so a
   parenthesised note keeps the table tool-compatible:
   - CICD-09: `Pending (JVM half verified in Phase 2; npm half is Phase 5)`
   - UPS-02: `Pending (Phase 2 part verified; the documentation-site index is Phase 5)`
   - UPS-03: `Pending (human UAT 5 open: four drafts unreported)`
   - TOOL-04: `Pending (verified on colima; human UAT 6 open: Docker Desktop)`

4. REQUIREMENTS.md, below the `**Coverage:**` list and before the closing `---`: add a short `**Scope notes:**` line
   or two saying that IDEM-04, GH-12 and TEST-14 are Complete for their Phase 2 scope as recorded in
   02-VERIFICATION.md (IDEM-04's PostgreSQL `uuid` storage, API-boundary and `typeid-js` clauses, GH-12's skill-use
   clause, and TEST-14's AxonTestFixture, Awaitility and MockK arrive with the phases that first use them). Update the
   `*Last updated: ...*` footer to `*Last updated: 2026-11-23 after the interim milestone audit (Phase 2 statuses)*`.

5. 01-VERIFICATION.md: change the frontmatter `status` value from `passed` to `human_needed`, matching the body's
   `**Status:** human_needed` (line 91). Change nothing else (score, covered_files and covered_digest stay).

6. STATE.md: edit ONLY the four position lines under `## Current Position` (Phase, Plan, Status, Last activity); keep
   the `Progress:` line, the frontmatter and every other section as they are (the orchestrator owns the frontmatter
   and the Quick Tasks table). New text:
   - `Phase: 02 (Runtime Infrastructure & DCB Spikes) — VERIFYING`
   - `Plan: 23 of 23 executed (all SUMMARYs present)`
   - `Status: Phase 02 verification human_needed (5/5 success criteria verified); 02-UAT.md has 6 pending human items; the phase PR is not open yet`
   - `Last activity: 2026-11-23 — interim milestone audit (tech_debt); pre-ship debt closed by quick task 261123-mub`

7. Commit the four documents together, for example
   `docs(quick-261123-mub): record WR-05 fix and refresh Phase 2 requirement statuses`.

8. Full gate on the final tree (all commits of Tasks 1-3 in place): `mise x -- ./gradlew build --console=plain`
   (runs every suite including integrationTest, the functional tests and all governance checks; it can take longer
   than the 10-minute foreground limit, so run it in the background and wait for it), then `mise x -- reuse lint` and
   `mise x -- editorconfig-checker`. Then run the required Qodana gate on the committed HEAD with
   `scripts/qodana.sh` (several minutes, background); it must exit 0 so the new Kotlin code does not turn the phase
   PR's `qodana` job red. If any of these fails, fix the cause in the file that owns it and commit the fix
   separately; never weaken a check.
  </action>
  <verify>
    <automated>mise x -- ./gradlew build --console=plain && mise x -- reuse lint && mise x -- editorconfig-checker && scripts/qodana.sh</automated>
    Gates (run from the checkout root):
    - `grep -A2 'id: WR-05' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md | grep -c 'disposition: fixed'` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c '^open: 10$' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md` [HEAD=0 EXPECT==1 @669c3f0]
    - `sha=$(grep -oE 'quick task 261123-mub in [0-9a-f]{7,}' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-REVIEW-DISPOSITION.md | awk '{print $NF}') && git cat-file -e "${sha}^{commit}" && changed=$(git show --name-only --format= "$sha") && printf '%s\n' "$changed" | grep -c DecisionCaptureIntegrationTest.kt` [HEAD=error:128 EXPECT==1 @669c3f0]
    - `grep -c '| Phase 2 | Complete |' .planning/REQUIREMENTS.md` [HEAD=3 EXPECT==18 @669c3f0]
    - `grep -c -E '^- \[x\] \*\*(INFRA-0[2-6]|IDEM-04|TEST-1[124]|DOC-03|GH-12|UPS-0[14]|TOOL-03|AIDEV-0[1347])\*\*' .planning/REQUIREMENTS.md` [HEAD=3 EXPECT==18 @669c3f0]
    - `grep -c -E '^- \[ \] \*\*(CICD-09|UPS-02|UPS-03|TOOL-04)\*\*' .planning/REQUIREMENTS.md` [HEAD=4 EXPECT==4 @669c3f0 GUARD MUTATION: tick the UPS-03 checkbox, which counts 3] (invariance gate: these four must stay unchecked)
    - `grep -c -E '^\| (CICD-09|UPS-02|UPS-03|TOOL-04) \|.*\| Pending \(' .planning/REQUIREMENTS.md` [HEAD=0 EXPECT==4 @669c3f0]
    - `grep -c '^status: human_needed$' .planning/phases/01-repository-build-ci-foundation/01-VERIFICATION.md` [HEAD=0 EXPECT==1 @669c3f0]
    - `grep -c '^status: passed$' .planning/phases/01-repository-build-ci-foundation/01-VERIFICATION.md` [HEAD=1 EXPECT==0 @669c3f0]
    - `grep -c '^Plan: 23 of 23' .planning/STATE.md` [HEAD=0 EXPECT==1 @669c3f0]
    - `sed -n '/^## Current Position/,/^## Performance/p' .planning/STATE.md | grep -c -E 'human_needed|UAT'` [HEAD=0 EXPECT>=1 @669c3f0]
  </verify>
  <done>WR-05 is `fixed` with a real commit SHA that touched the test; 18 Phase 2 IDs are Complete and [x], the four exceptions are Pending with notes and unchecked; 01-VERIFICATION frontmatter says human_needed; the STATE.md position reflects 23 of 23 executed, verification human_needed and pending UAT; `./gradlew build`, `reuse lint`, `editorconfig-checker` and `scripts/qodana.sh` all exit 0.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| PR content -> required CI gates | A pull request can change the files a gate reads; governed paths need the `governance-approved` label and a maintainer review |
| Agent file edits -> governed files | Claude Code's ask rules make every edit of a governed path an explicit author approval |
| Version catalog -> compose runtime | Catalog `image-*` entries become the images Docker Compose pulls, through generated compose/versions.env |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-mub-01 | Tampering | qodana.yaml, scripts/qodana.sh (required `qodana` gate) | medium | mitigate | Task 2 governs both files: a PR touching them needs the `governance-approved` label (check-pr.sh reads the list) and the agent must ask before editing them (ask rules, parity enforced by checkClaudeAskRules) |
| T-mub-02 | Elevation of privilege | governance/governed-paths.txt and .claude/settings.json edited together | medium | mitigate | Both are already governed (`governance/**`, `.claude/settings.json`); the edit runs under ask prompts and the PR carries the label; checkClaudeAskRules fails on any mismatch between the two |
| T-mub-03 | Tampering | ComposeVersions.TEST_ONLY_IMAGES used to hide a compose image | low | mitigate | Default-include derivation; an exclusion is a visible code change with KDoc; a compose file that interpolates a variable missing from compose/versions.env fails loudly (`${X_IMAGE:?...}`) in the compose-smoke job; TestKit test pins that ryuk alone is excluded |
| T-mub-04 | Repudiation | REQUIREMENTS.md statuses flipped beyond the evidence | low | mitigate | Only IDs that 02-VERIFICATION.md lists as SATISFIED are marked Complete; UPS-03, TOOL-04, CICD-09 and UPS-02 stay Pending with notes; grep gates pin both sets |
| T-mub-05 | Repudiation | DecisionCaptureIntegrationTest passing vacuously (D-13 safeguard) | medium | mitigate | Committed-count assertion before the loop (WR-05); the test must run green against a real Axon Server |
| T-mub-SC | Tampering | npm/pip/cargo installs | high | accept | No package, plugin or tool is added or upgraded by this plan; no install task exists, so no legitimacy checkpoint is needed |
</threat_model>

<verification>
- Task 1: shared derivation, TestKit proof of a third image, byte-identical compose/versions.env.
- Task 2: 58 governed entries in parity with 58 ask rules; governance shell tests green; WR-05 assertion green against
  Axon Server.
- Task 3: bookkeeping gates; `mise x -- ./gradlew build`, `reuse lint`, `editorconfig-checker` and
  `scripts/qodana.sh` exit 0 on the final tree.
</verification>

<success_criteria>
- INT-W1 and INT-W2 from the audit are closed in code, with the data-file exclusions documented in the governed list.
- WR-05 is fixed and recorded with its commit SHA.
- REQUIREMENTS.md, 01-VERIFICATION.md and the STATE.md position match the verified state of Phases 1 and 2.
- The full Gradle build and the Qodana gate pass locally, so the phase PR's required jobs see no new failure from this
  work.
</success_criteria>

<output>
Create `.planning/quick/261123-mub-close-pre-ship-debt-from-interim-audit-g/261123-mub-SUMMARY.md` when done. Include:
- the commit SHAs of Task 1, Task 2 (both commits) and Task 3, and of any fix commit from the final gate;
- the byte-identity proof (`git diff --exit-code compose/versions.env` result) and the ComposeVersionsFunctionalTest case count;
- the ryuk filter decision in one paragraph (what, why, default-include direction);
- the observation list of other required-gate configuration files outside the governed boundary, each confirmed against .github/workflows/ci.yml, for the author to decide on later;
- durations and exit codes of `./gradlew build` and `scripts/qodana.sh`.
</output>
