---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-24T00:00:00Z
depth: standard
files_reviewed: 70
files_reviewed_list:
  - .claude/CLAUDE.md
  - .claude/settings.json
  - backend/command-service/build.gradle.kts
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/ConditionalAppendIntegrationTest.kt
  - backend/command-service/src/integrationTest/kotlin/io/github/aanbrn/courses/command/DecisionCaptureIntegrationTest.kt
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/KotlinTagResolver.kt
  - backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngine.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/DecisionCaptureTest.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/KotlinTagResolverTest.kt
  - backend/command-service/src/test/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngineTest.kt
  - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CapacityHarness.kt
  - backend/command-service/src/testFixtures/kotlin/io/github/aanbrn/courses/command/fixtures/CommandServiceTestApplication.kt
  - backend/contracts/kernel/build.gradle.kts
  - backend/contracts/kernel/src/main/kotlin/io/github/aanbrn/courses/contracts/kernel/TypedId.kt
  - backend/contracts/kernel/src/test/kotlin/io/github/aanbrn/courses/contracts/kernel/TypeIdPropertyTest.kt
  - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/ClockedUuidV7Generator.kt
  - backend/contracts/kernel/src/testFixtures/kotlin/io/github/aanbrn/courses/contracts/kernel/fixtures/IdArbs.kt
  - backend/serialization-jackson/build.gradle.kts
  - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
  - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ConventionsFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/TestSuitesFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckHooksFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckInputShapeFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ClaudeSettingsFixtures.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPathsFormatFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/courses.governance.gradle.kts
  - build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/main/kotlin/courses.test-suites.gradle.kts
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersions.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/JavaVersionFile.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AdrCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/FrontMatter.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernanceHooks.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPaths.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/MarkdownOutline.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/UpstreamRecordCheck.kt
  - docs/adr/0003-governance-gate-trust-boundary.md
  - docs/adr/0004-gsd-configuration.md
  - docs/adr/0012-dependency-licence-policy.md
  - docs/adr/0013-upstream-records-and-workaround-markers.md
  - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
  - docs/tooling.md
  - docs/upstream/0011-colima-aaaa-dns.md
  - governance/governed-paths.txt
  - gradle/libs.versions.toml
  - scripts/doctor.sh
  - scripts/governance/ask-governed-edit.sh
  - scripts/governance/ask-governed-merge.sh
  - scripts/governance/check-pr.sh
  - scripts/governance/governed-paths.sh
  - scripts/governance/hook-common.sh
  - scripts/governance/test-ask-governed-edit.sh
  - scripts/governance/test-ask-governed-merge.sh
  - scripts/test-doctor.sh
  - tooling/architecture/src/architectureTest/kotlin/io/github/aanbrn/courses/architecture/ContainerImageArchitectureTest.kt
  - tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeView.kt
  - tooling/architecture/src/test/kotlin/io/github/aanbrn/courses/architecture/KotlinCodeViewTest.kt
  - tooling/doctor/build.gradle.kts
findings:
  critical: 0
  warning: 4
  info: 3
  total: 7
  necessity: 0
status: issues_found
---

# Phase 2: Code Review Report (incremental re-review)

**Reviewed:** 2026-11-24
**Depth:** standard
**Files Reviewed:** 70
**Status:** issues_found

## Summary

This is the incremental review of the changes since b5a0e6e: the follow-up fixes from quick tasks 261123-mub, 261123-p1u
and 261123-rzt, and the new governance hooks of ADR-0015. No structural findings block was provided, and the necessity
reference was not applied for any finding (none qualified).

The follow-up fixes hold up. The `DecisionCaptureEngine` overrides of the two Java default methods route through the
capturing overloads and have a test that fails without them. `KotlinTagResolver`, `CoursesJsonModule`,
`GovernedPaths.matches` (checked against the shell `glob_match`), `ComposeVersions`, `MarkdownOutline`, `KotlinCodeView`
and `GovernanceHooks` have no defect that I could demonstrate. The Gradle changes (opt-in `java-test-fixtures`,
`compileOnly` plus `runtimeOnly` for the mapper, the catalog read through `VersionCatalogsExtension`) are consistent with
the Gradle best practices. I ran the three shell test suites, and all pass (37, 63 and 139 checks).

The weaknesses sit in the new merge hook. I ran the real `ask-governed-merge.sh` against crafted tool inputs and found
several ways for a merge to pass without a prompt. All of them are inside the area the hook states it covers (shell
quoting, `bash -c`, heredocs fed to a shell), and none is listed among ADR-0015's residual risks. The hook is described
as a guard rail, not a security boundary, so none is a BLOCKER, but the ADR's "Good, because governed content that shell
commands ... wrote in a worktree is caught when it is merged" overstates what the hook does today. There is also one
brittle test that will break when Phase 6 adds the compose image the build-logic change was written for.

## Warnings

### WR-07: The merge hook's early exit lets a quote-split `merge` skip every check

**File:** `scripts/governance/ask-governed-merge.sh:36-40`
**Issue:** The hook leaves before it loads anything unless the raw JSON input contains the text `merge`, `cleanup-wave` or
`\u`. The comment says that "a `\u` escape could hide" the word, but shell quoting hides it just as well, and the lexer
below is built to understand quoting. I ran the hook with `git mer''ge nonexistent-xyz` in a repository: it printed
nothing, while `git merge nonexistent-xyz` asks. The same holds for `git m\erge ...` and `git "mer"ge ...`. Git runs
all of these as `git merge`. The branch of the merge is irrelevant, so a worktree branch that changes a governed path
enters the main checkout with no prompt. (`worktree.cleanup-wave` split by quotes bypasses in the same way.)
**Fix:** Normalise the input before the test, for example strip the characters the shell removes or that JSON escapes,
and test that text:
```bash
probe=${INPUT//[\'\"\\]/}
case $probe in
*merge* | *cleanup-wave* | *'\u'*) ;;
*) exit 0 ;;
esac
```
Add the `mer''ge`, `mer\ge` and `"mer"ge` forms to `test-ask-governed-merge.sh`.

### WR-08: A merge fed to a shell through a pipe or a here-string is not seen

**File:** `scripts/governance/ask-governed-merge.sh:205-235, 626-652`
**Issue:** The hook analyses `bash -c <script>`, `eval` and a heredoc whose command word is the shell. It does not analyse
the other ways of handing a script to a shell, and ADR-0015 lists none of them as residual. I ran each of these with an
unresolvable ref, which makes a recognised merge ask, and each printed nothing:
- `echo "git merge nonexistent-xyz" | bash`
- `bash <<< "git merge nonexistent-xyz"` (the here-string target is marked as a redirect word and dropped, so
  `analyze_shell` never sees it)
- `cat <<EOF | bash` with the merge in the heredoc body (the heredoc is registered as fed to `cat`)
- `sudo git merge ...` and `env -S "git merge ..."` (`sudo` is not a known wrapper, and `env -S` skips its value)
`ask-governed-edit.sh` is not affected. An executor that writes its own merge command in one of these shapes is not
asked, and ADR-0015's consequences section says governed content "is caught when it is merged".
**Fix:** Treat a shell that reads its script from standard input as unanalysable when the command mentions a merge:
in `analyze_words`, when the program is `bash|sh|zsh|dash|ksh` and `analyze_shell` found no `-c` script, add a problem if
`mentions_merge "$COMMAND"`; do the same for a here-string whose text mentions a merge. Add `sudo` and `doas` to the
wrappers, and handle `env -S` by analysing its value. Add the missing shapes to the ADR's residual risks if they stay
outside the hook.

### WR-09: A `<<` that is not a heredoc swallows the rest of the command without a trace

**File:** `scripts/governance/ask-governed-merge.sh:205-227, 239-266`
**Issue:** `lex_redirect` treats every unquoted `<<` as a heredoc. Arithmetic expansion uses the same characters. I ran
```
x=$((1<<2))
git merge nonexistent-xyz
```
and the hook printed nothing. The lexer registered the delimiter `2`, and `lex_heredoc_bodies` consumed every following
line looking for a line that equals `2`; reaching the end of the text is not recorded anywhere (`LEX_INCOMPLETE` stays 0),
so the merge on the next line is never analysed. Bash itself would run it. The same happens for any heredoc whose
delimiter never appears. This is a silent false negative where the hook's contract is to ask when it cannot analyse.
**Fix:** Mark the lexing incomplete when a heredoc body has no terminator, so that a command that mentions a merge asks:
```bash
found=0
while ((i < length)); do
  ...
  if [[ $candidate == "${heredoc_delimiters[index]}" ]]; then found=1; break; fi
  body+=$line$'\n'
done
[[ $found -eq 1 ]] || LEX_INCOMPLETE=1
```
Optionally skip a `<<` that follows `$((` or `((`.

### WR-10: `ComposeVersionsFunctionalTest` pins the repository catalog's current image set

**File:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ComposeVersionsFunctionalTest.kt:36-41, 43-57`
**Issue:** The fixture reads the repository's real `gradle/libs.versions.toml` until a test replaces it. The first test
expects `versionsFor("axonserver", "postgres")`, and the second inserts `image-otel-lgtm` after `image-ryuk` and expects
exactly `axonserver`, `otel_lgtm`, `postgres`. When the real catalog gains a compose image (the Phase 6 `otel-lgtm` image
is the example the test itself uses, and the stated aim of `ComposeVersions` is that "a new compose image is rendered and
checked without a build-logic change"), the first test fails on the extra line, and the second fails on a duplicate TOML
key. The test that proves "no build-logic change needed" would then require a test change.
**Fix:** Give the fixture its own minimal catalog through `changeCatalog` (two compose entries plus `image-ryuk`) in every
test, or derive the expected names in the first test from the catalog text (`image-*` keys minus `ryuk`) and make the
second test's new entry name one that cannot already exist, such as `image-zz-fixture`.

## Info

### IN-06: `ImageReferenceCheck` skips an indented catalog entry

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt:138-146`
**Issue:** `CATALOG_ENTRY` is anchored with `^(["']?)(image-...)`, so an entry written with leading whitespace
(`  image-x = 'postgres:18'`, valid TOML) is never matched and never reported, although the class documentation says every
such key is matched, bare or quoted. `ComposeVersions.images` reads the entry from the parsed catalog regardless, so the
unpinned reference would reach `compose/versions.env`.
**Fix:** Allow leading blanks in the regex: `^\s*(["']?)(image-[A-Za-z0-9_.-]+)\1\s*=\s*(.*)$`, and add a case to
`ImageReferenceCheckFunctionalTest`.

### IN-07: `CoursesJsonModule` writes an ID as a map key but cannot read it back

**File:** `backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt:28-42`
**Issue:** The module registers a value serializer and deserializer for each ID type. A `Map<CourseId, ...>` in a payload
is written with the ID's `toString()` as the key, but on read Jackson has no key deserializer for the ID type and fails.
No contract has a map keyed by an ID yet, so this is latent; the KDoc calls the module "the one Jackson 3 module for the
value types of the contracts".
**Fix:** When the first ID-keyed map appears, register `addKeyDeserializer(type, ...)` through the same `parse`, with the
same `weirdKey` style reporting, or state in the KDoc that IDs are value-only.

### IN-08: The hooks need git 2.36 or newer, and `doctor` does not check it

**File:** `scripts/governance/ask-governed-edit.sh:104-112`
**Issue:** The edit hook calls `git rev-parse --path-format=absolute` (git 2.31) and `git worktree list --porcelain -z`
(git 2.36). On an older git both fail, the hook fails closed, and every Edit and Write prompts the author, with a reason
that names git instead of the version. `scripts/doctor.sh` checks many tools but not this one, so the cause is hard to see.
**Fix:** Add a `doctor.sh` check that `git --version` is at least 2.36, or mention the minimum in `docs/tooling.md` and in
ADR-0015.

---

_Reviewed: 2026-11-24_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
