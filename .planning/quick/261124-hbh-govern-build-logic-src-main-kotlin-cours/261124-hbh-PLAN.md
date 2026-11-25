---
phase: quick-261124-hbh
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - governance/governed-paths.txt
  - .claude/settings.json
  - scripts/governance/test-check-pr.sh
  - docs/adr/0003-governance-gate-trust-boundary.md
  - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md
autonomous: true
requirements: [AIDEV-06, AIDEV-08, TEST-10, TEST-13]

estimate:
  tokens: 60000
  raw_tokens: 60000
  tasks: 2
  confidence: low

must_haves:
  truths:
    - "Author decision (2026-11-24, locked): a pull request that touches `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` without `governance-approved` fails governance-success; the list grows from 80 to 81 entries, and the existing boundary case for that file in test-check-pr.sh now expects `governed`"
    - "Claude Code asks before an edit of the file: `.claude/settings.json` gains exactly one ask rule `Edit(/build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts)` (81 to 82 rules, nothing else in the file changes) and `checkClaudeAskRules` is green"
    - "The sibling module convention plugins stay ungoverned, per the author decision: `courses.test-suites.gradle.kts` and `courses.architecture-tests.gradle.kts` are pinned by `ungoverned` boundary cases"
    - "The header of governance/governed-paths.txt states the new exception and its reason (gate wiring: detekt with the governed rule set, `allWarningsAsErrors`, the `courses.licences` apply line and Dokka's `failOnWarning`, not governance-task reach), says the `courses.licences` apply line now lives in a governed file, names the other module convention plugins as staying outside, and names the remaining per-module residual (a module's own build script switching off its own detekt task, licence task or compiler options)"
    - "The accepted residual for removing detekt through a module convention plugin is retired wherever a living document records it: the governed-paths.txt header, the paragraph after the Accepted Risks Log in 02-SECURITY.md (rest of the register untouched) and a new ADR-0003 consequence bullet; executed plans and summaries stay as historical records"
    - "The renovate[bot] version-only exemption cannot reach the file: Renovate extracts it as a gradle package file with zero dependencies (the invariant prints 0 at HEAD and after), and `NEVER_EXEMPT_PATHS` and the exemption logic stay unchanged"
    - "All listed checks are green: test-check-pr.sh (173 passed), test-ask-governed-edit.sh (58 passed), test-ask-governed-merge.sh (120 passed), `checkClaudeAskRules checkAdrs spotlessCheck` and the GovernedPaths/AskRules functional tests"
  artifacts:
    - path: "governance/governed-paths.txt"
      provides: "The governed entry for courses.kotlin-library, the rewritten module-convention-plugin bullet and the narrowed accepted residual"
      contains: "build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts"
    - path: ".claude/settings.json"
      provides: "One Edit ask rule for the new entry"
      contains: "Edit(/build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts)"
    - path: "scripts/governance/test-check-pr.sh"
      provides: "Boundary case flipped to governed, and ungoverned cases for the sibling module convention plugins"
      contains: "boundary_case build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts governed"
    - path: "docs/adr/0003-governance-gate-trust-boundary.md"
      provides: "Consequence recording the governed module convention plugin and the remaining per-module residual"
      contains: "courses.kotlin-library"
    - path: ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md"
      provides: "Residual paragraph without the convention-plugin route, pointing at quick task 261124-hbh"
      contains: "261124-hbh"
  key_links:
    - from: "governance/governed-paths.txt"
      to: ".claude/settings.json"
      via: "checkClaudeAskRules (AskRulesCheck, GovernedPaths.parse): exactly one Edit(/<entry>) ask rule per entry"
      pattern: "Edit\\(/build-logic/src/main/kotlin/courses\\.kotlin-library"
    - from: "governance/governed-paths.txt"
      to: "scripts/governance/check-pr.sh"
      via: "governed_globs and glob_match in scripts/governance/governed-paths.sh, exercised by boundary_case in test-check-pr.sh against the real list"
      pattern: "boundary_case build-logic/src/main/kotlin/courses\\.kotlin-library"
    - from: ".planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md"
      to: "governance/governed-paths.txt"
      via: "the residual paragraph says which residuals governed-paths.txt names; the Accepted residual paragraph of the header must name the per-module route"
      pattern: "Residuals named in"
---

<objective>
Govern `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` and retire the accepted residual for removing
detekt from a module convention plugin.

Author decision (2026-11-24, locked; referenced below as "the author decision"): govern only
`build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts`. It applies `dev.detekt`, points detekt at the governed
`config/detekt/detekt.yml` and sets `allWarningsAsErrors`, so one edit can silently switch off two required gates while
CI stays green. Other module convention plugins stay outside with the existing rationale (under isolated projects they
cannot reach root-project governance tasks). The earlier exclusion (decisions 01-17/01-19) was made when every governed
edit cost a prompt; since quick task 261124-dvp an agent-worktree edit costs one merge prompt only. The 261124-gju
SUMMARY flagged this file (its "Flagged for the author" item 1), and this task closes that flag.

Out of scope, on purpose: STATE.md (the orchestrator removes the open decision at its line 138), the other module
convention plugins, any change to `scripts/governance/check-pr.sh`, `NEVER_EXEMPT_PATHS` or the hooks, any rewrite of
the 02-SECURITY.md threat register beyond the residual paragraph, and historical execution records (02-20-PLAN.md rows
T-02-58/T-02-60, the 261124-gju PLAN and SUMMARY), which record the boundary as it was when they ran.

Purpose: AIDEV-06 (governed changes need the label) and AIDEV-08 (ask rules for governed files) extended to the file
that wires TEST-13 (detekt, warnings as errors) and TEST-10 (Dokka `failOnWarning`) into every Kotlin module.
Output: one governed entry with its ask rule, boundary cases, the rewritten header, the ADR-0003 consequence and the
updated 02-SECURITY.md residual paragraph.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@governance/governed-paths.txt
@build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
@.planning/quick/261124-gju-govern-the-configs-of-the-other-required/261124-gju-SUMMARY.md

Read only the parts named:
- scripts/governance/test-check-pr.sh: `boundary_case` and the first groups of the section
  `# --- governed boundary (WR-01) ---` (about lines 833-890).
- .claude/settings.json: `permissions.ask` only (the anchor `"Edit(/config/detekt/**)",` is at about line 57).
- docs/adr/0003-governance-gate-trust-boundary.md: `### Consequences` (about lines 81-105).
- .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md: the Accepted Risks Log and the paragraph after
  it (about lines 112-127).

## Planner findings (verified 2026-11-24 at 09a763c)

- What the file wires into every Kotlin module (the module conventions `courses.test-suites` and
  `courses.architecture-tests` apply it too): `id("dev.detekt")` with `buildUponDefaultConfig` and
  `config.setFrom(rootDirectory.file("config/detekt/detekt.yml"))`; `allWarningsAsErrors.set(true)` with
  `-Xjsr305=strict` and `-Xjspecify-annotations=strict`; `id("courses.licences")`; Dokka `failOnWarning.set(true)` with
  `check` depending on `dokkaGeneratePublicationHtml`. The build job runs all of these through `./gradlew build`.
- Where the residual is recorded (grep over the repository, `.planning/` included):
  - governance/governed-paths.txt header, the "module convention plugins" bullet (lines 29-34) and the "Accepted
    residual" paragraph (lines 79-84); the bullet still says the `courses.licences` apply line stays outside;
  - .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md lines 125-126, the paragraph after the Accepted
    Risks Log, which names two residuals "named in governance/governed-paths.txt";
  - historical only: 02-20-PLAN.md (T-02-58, T-02-60 rows), the 261124-gju PLAN and SUMMARY, STATE.md lines 119 and
    138 (orchestrator-owned). No ADR records it: ADR-0003 and ADR-0015 do not mention module convention plugins, and
    ADR-0012's "Governed gate" bullet (courses.licences governed, CI names `checkDependencyLicences`, detekt rule set
    governed) stays accurate, so ADR-0012 is left unchanged.
- The per-module route stays open: `BuildScriptLintCheck` rejects version literals, eager task APIs, `allprojects`,
  `subprojects` and `afterEvaluate`, but not a module build script that disables its own `detekt` or
  `checkDependencyLicences` task or resets its own compiler options. Module build scripts stay outside (governed-paths
  header, "module build scripts" bullet), so that route becomes the named residual.
- Renovate (`renovate --platform=local --dry-run=extract`, 2026-11-24): every `build-logic/src/main/kotlin/*.gradle.kts`
  file is extracted by the gradle manager with zero dependencies, `courses.kotlin-library.gradle.kts` included. The
  renovate[bot] version-only exemption therefore never applies to the file and needs no change.
- Baselines at 09a763c: 80 governed entries, 81 ask rules, test-check-pr.sh 171 passed, test-ask-governed-edit.sh 58
  passed, test-ask-governed-merge.sh 120 passed. The functional tests use their own fixture lists, and the edit-hook
  test uses its own entry list: a literal path entry is a shape both already cover, so neither needs a new case.

Execution notes:
- The executor runs in an agent worktree: governed edits cost no per-edit prompt, and the merge hook asks once. Change
  the governed files (`governance/governed-paths.txt`, `.claude/settings.json`, `scripts/governance/test-check-pr.sh`)
  with the Edit tool, never with shell writes (ADR-0015, .claude/CLAUDE.md).
- Conventional commits in English, scoped `quick-261124-hbh`, one per task. Wrap edited comment and prose lines at the
  width of the surrounding lines, never beyond 120 columns; keep every governed-paths.txt line printable ASCII.
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Govern courses.kotlin-library end to end: entry, ask rule, boundary cases and header</name>
  <files>governance/governed-paths.txt, .claude/settings.json, scripts/governance/test-check-pr.sh</files>
  <precondition>`bash scripts/governance/test-check-pr.sh | tail -n 1` prints `171 passed, 0 failed` and `jq '.permissions.ask | length' .claude/settings.json` prints `81` at the plan base.</precondition>
  <reversibility rating="reversible">One list entry, one ask rule and header prose; deleting them restores the previous boundary.</reversibility>
  <behavior>
    - A one-file pull request by a human touching `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` without the label fails the gate
    - The same for `build-logic/src/main/kotlin/courses.test-suites.gradle.kts` and `build-logic/src/main/kotlin/courses.architecture-tests.gradle.kts` passes
    - Every pre-existing case, the `courses.licences`, `courses.root` and `courses.settings` cases and the Renovate exemption cases included, still passes
  </behavior>
  <action>
Implement the author decision through every layer: the gate tests first (RED), then the list, the ask rule and the
header (GREEN).

1. scripts/governance/test-check-pr.sh (Edit tool), section `# --- governed boundary (WR-01) ---`:
   - Change the expectation of the existing `boundary_case` line for
     `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` (about line 863) to `governed`, and put a one-line
     comment directly above it: the module convention plugin that wires detekt, `allWarningsAsErrors` and the licence
     check into every module is governed (author decision of 2026-11-24).
   - In the group "Deliberately outside the boundary, for the reasons recorded in governance/governed-paths.txt" (about
     line 885), append `boundary_case build-logic/src/main/kotlin/courses.test-suites.gradle.kts ungoverned` and
     `boundary_case build-logic/src/main/kotlin/courses.architecture-tests.gradle.kts ungoverned`, preceded by a
     one-line comment that the other module convention plugins stay outside.
   - Run the script and confirm RED: exactly the flipped case fails (172 passed, 1 failed).
2. governance/governed-paths.txt (Edit tool):
   - Entry: open a new block directly after the `config/detekt/**` block and before the block that starts "The
     configuration of the other jobs that ci-success requires". Comment heading, two to four lines: the module
     convention plugin that wires required build-job gates into every Kotlin module; it applies detekt with the rule set
     above, sets `allWarningsAsErrors`, applies `courses.licences` and Dokka's `failOnWarning`, so an edit to it alone
     can switch detekt or the compiler gate off while CI stays green (author decision of 2026-11-24); Renovate extracts
     it as a gradle package file without dependencies, so the renovate[bot] version-only exemption never applies to it.
     Then the single entry `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts`.
   - Boundary rule paragraph: extend the clause "the convention plugins applied to the root project and to settings"
     with the module convention plugin that wires the detekt, compiler and licence gates into every Kotlin module.
     Re-wrap only the lines this changes.
   - "Outside the boundary" list, replace the whole "module convention plugins" bullet (lines 29-34) with one that
     says: module convention plugins stay outside because under isolated projects they cannot reach the root project's
     tasks, and checkBuildScripts rejects allprojects, subprojects and afterEvaluate. Two are governed below, each for
     its own reason: courses.licences registers a per-module governance check (checkDependencyLicences);
     courses.kotlin-library is governed for its gate wiring rather than for reach (it applies detekt with the governed
     rule set, sets allWarningsAsErrors, applies courses.licences and Dokka's failOnWarning in every Kotlin module, so
     one edit there could switch two required gates off while CI stays green; author decision of 2026-11-24). The line
     that applies courses.licences therefore lives in a governed file, and CI still names checkDependencyLicences
     explicitly, so a build in which no module registers it fails. The other module convention plugins
     (courses.test-suites, courses.architecture-tests) stay outside. End the bullet with a semicolon like its
     neighbours. Decision note: the old sentence saying the apply line sits outside the boundary goes away entirely,
     because it is no longer true.
   - "Accepted residual" paragraph: add one sentence after the build-code sentence: a module's own build script can
     still switch off that module's detekt task, licence task or compiler options, because module build scripts stay
     outside the boundary; such a line shows in the reviewed diff. This is the residual that 02-SECURITY.md points at
     (Task 2).
3. .claude/settings.json (Edit tool): in `permissions.ask`, insert the single string
   `Edit(/build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts)` directly after `Edit(/config/detekt/**)`,
   mirroring the list order, one string per line in the existing layout. Change nothing else in the file.
4. Run the script again (GREEN: 173 passed, 0 failed), the Renovate invariant, shellcheck and the Gradle checks below.
   Record the Renovate invariant result in the SUMMARY. Commit:
   `feat(quick-261124-hbh): govern the courses.kotlin-library convention plugin`.
  </action>
  <verify>
    <automated>grep -cxF 'build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts' governance/governed-paths.txt  # [HEAD=0 EXPECT==1 @09a763c]</automated>
    <automated>grep -v '^#' governance/governed-paths.txt | grep -c .  # [HEAD=80 EXPECT==81 @09a763c]</automated>
    <automated>jq -r '.permissions.ask[]' .claude/settings.json | grep -cxF 'Edit(/build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts)'  # [HEAD=0 EXPECT==1 @09a763c]</automated>
    <automated>jq '.permissions.ask | length' .claude/settings.json  # [HEAD=81 EXPECT==82 @09a763c]</automated>
    <automated>grep -cxF 'boundary_case build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts governed' scripts/governance/test-check-pr.sh  # [HEAD=0 EXPECT==1 @09a763c]</automated>
    <automated>grep -v '^[[:space:]]*#' scripts/governance/test-check-pr.sh | grep -c 'courses.kotlin-library.gradle.kts ungoverned'  # [HEAD=1 EXPECT==0 @09a763c]</automated>
    <automated>grep -cE '^boundary_case build-logic/src/main/kotlin/courses\.(test-suites|architecture-tests)\.gradle\.kts ungoverned$' scripts/governance/test-check-pr.sh  # [HEAD=0 EXPECT==2 @09a763c]</automated>
    <automated>grep -cE 'courses\.test-suites|courses\.architecture-tests' governance/governed-paths.txt  # [HEAD=0 EXPECT>=1 @09a763c]</automated>
    <automated>grep -c 'applies it stays outside' governance/governed-paths.txt  # [HEAD=1 EXPECT==0 @09a763c] target is header prose, so no comment filter applies</automated>
    <automated>bash scripts/governance/test-check-pr.sh | tail -n 1 | awk '{exit !($1 >= 173 && $3 == 0)}'  # [HEAD=171 EXPECT>=173 @09a763c]</automated>
    <automated>LOG_LEVEL=debug LOG_FORMAT=json mise x -- renovate --platform=local --dry-run=extract --onboarding=false 2>&1 | jq -s '[.[] | select(.msg == "Extracted dependencies") | .packageFiles[][] | select(.packageFile == "build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts") | .deps | length] | add'  # [HEAD=0 EXPECT==0 @09a763c GUARD MUTATION: a plugins-block entry with a version literal in the file makes the gradle manager extract one dependency; `null` means the file was not extracted at all and also fails] invariant the unchanged exemption rests on</automated>
    <automated>mise x -- shellcheck scripts/governance/test-check-pr.sh</automated>
    <automated>mise x -- ./gradlew checkClaudeAskRules :build-logic:functionalTest --tests '*GovernedPaths*' --tests '*AskRules*'</automated>
  </verify>
  <done>The list has 81 entries with courses.kotlin-library in its own commented block after the detekt rule set;
  settings.json has 82 ask rules and checkClaudeAskRules is green; the gate blocks an unlabelled change to the file and
  passes the two sibling module convention plugins (RED run failed on the flipped case first, GREEN run 173 passed);
  the header states the exception and its reason, the governed licence apply line, the sibling plugins staying outside
  and the per-module residual; the Renovate invariant prints 0; the change is committed.</done>
</task>

<task type="auto">
  <name>Task 2: Retire the convention-plugin detekt residual in ADR-0003 and 02-SECURITY.md</name>
  <files>docs/adr/0003-governance-gate-trust-boundary.md, .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md</files>
  <reversibility rating="reversible">Documentation prose; reverting the commit restores the previous records.</reversibility>
  <action>
Bring the living records in line with Task 1 (the author decision).

1. docs/adr/0003-governance-gate-trust-boundary.md: add one bullet at the end of `### Consequences`, after the bullet
   about the configuration of the jobs that `ci-success` requires. Content: the module convention plugin
   `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` is governed too, by author decision of 2026-11-24,
   although module convention plugins otherwise stay outside because they cannot reach the root project's governance
   tasks under isolated projects; it applies detekt with the governed rule set, sets `allWarningsAsErrors` and applies
   `courses.licences` in every Kotlin module, so an unlabelled edit to it could switch off two required gates while CI
   stays green. A module's own build script stays outside, and `governance/governed-paths.txt` records that residual.
   No Renovate manager reads a version from the file, so the version-only exemption needs no new exception. Leave the
   front matter and every other section unchanged.
2. .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md: replace only the two-line paragraph after the
   Accepted Risks Log (the one that starts "Residuals named in `governance/governed-paths.txt`", about lines 125-126).
   New content: the residuals named in `governance/governed-paths.txt` (a single module's own build script switching
   off its licence task, its detekt task or its compiler options) remain accepted there and are not repeated here; the
   convention-plugin route is closed, because `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts`, which
   applies detekt and the licence plugin and sets `allWarningsAsErrors` for every module, is governed since quick task
   261124-hbh (author decision of 2026-11-24). Do not touch the threat register, the Accepted Risks Log table, the
   audit trail or any other line.
3. Leave the historical records unchanged and list them in the SUMMARY with the reason (they record the boundary when
   they ran): 02-20-PLAN.md rows T-02-58 and T-02-60, the 261124-gju PLAN and SUMMARY, and STATE.md lines 119 and 138
   (the orchestrator owns STATE.md). Also note in the SUMMARY that ADR-0012 stays unchanged because its "Governed gate"
   bullet remains accurate and never recorded the residual.
4. Run the verify commands, then commit:
   `docs(quick-261124-hbh): retire the convention-plugin detekt residual`.
  </action>
  <verify>
    <automated>grep -c 'courses.kotlin-library' docs/adr/0003-governance-gate-trust-boundary.md  # [HEAD=0 EXPECT>=1 @09a763c]</automated>
    <automated>grep -ci 'dropping detekt' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md  # [HEAD=1 EXPECT==0 @09a763c] markdown prose, no comment syntax to filter</automated>
    <automated>grep -c '261124-hbh' .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md  # [HEAD=0 EXPECT>=1 @09a763c]</automated>
    <automated>d=$(git diff -U0 HEAD -- .planning/phases/02-runtime-infrastructure-dcb-spikes/02-SECURITY.md) && printf '%s\n' "$d" | grep -cE '^[-+]\| (T|AR)-02-'  # [HEAD=0 EXPECT==0 @09a763c GUARD MUTATION: editing any `| T-02-` register row or `| AR-02-` risk-log row makes the count non-zero] no register or risk-log row changes (run before the commit)</automated>
    <automated>mise x -- ./gradlew checkAdrs spotlessCheck</automated>
  </verify>
  <done>ADR-0003 carries the consequence bullet and checkAdrs is green; the 02-SECURITY.md residual paragraph names only
  the per-module route and points at quick task 261124-hbh, with no register or risk-log row changed; the SUMMARY lists
  the historical records left unchanged and why ADR-0012 stays as is; the change is committed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| agent edit to repository | An agent session can change build configuration in the pull request it prepares; the governed list, the ask rules and the hooks make a change to a governed file visible and label-gated |
| pull request to CI gates | The required build job runs `./gradlew build` from the pull request's own tree (ADR-0003), so the convention plugin in the pull request decides whether detekt, the compiler gate and Dokka run strictly |
| Renovate to governed files | renovate[bot] pull requests that change only versions in governed files are exempt from the label; the exemption must not reach a file with no versioned dependency |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-hbh-01 | Tampering | `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` (detekt application and rule-set path, `allWarningsAsErrors`, `courses.licences` apply line, Dokka `failOnWarning`) | medium | mitigate | Governed entry with one ask rule (Task 1); the flipped boundary case proves an unlabelled change fails the gate; checkClaudeAskRules keeps list and rules in sync |
| T-hbh-02 | Elevation of privilege | renovate[bot] version-only exemption in check-pr.sh | low | mitigate | Extract invariant proves the file is a gradle package file with zero dependencies (Task 1); `NEVER_EXEMPT_PATHS` and the exemption logic unchanged; every pre-existing case still passes |
| T-hbh-03 | Tampering | A module's own build script disabling its detekt task, licence task or compiler options | medium | accept | Module build scripts stay outside (isolated projects rationale); the residual is named in the governed-paths.txt header and 02-SECURITY.md (Tasks 1, 2), and the line shows in the reviewed diff; CI names `checkDependencyLicences`, so losing it in every module fails |
| T-hbh-04 | Tampering | Sibling module convention plugins `courses.test-suites`, `courses.architecture-tests` | low | accept | Author decision keeps them outside with the recorded rationale; pinned by `ungoverned` boundary cases (Task 1) |
| T-hbh-05 | Repudiation | Stale records of the retired residual | low | mitigate | Task 2 grep gates prove 02-SECURITY.md no longer names the convention-plugin route and points at 261124-hbh; ADR-0003 records the consequence |
| T-hbh-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package-manager install in this plan; no dependency is added or changed, so the package legitimacy gate has nothing to audit |
</threat_model>

<verification>
Run from the checkout root after both tasks:
- `bash scripts/governance/test-check-pr.sh` ends with `173 passed, 0 failed`
- `bash scripts/governance/test-ask-governed-edit.sh` ends with `58 passed, 0 failed`
- `bash scripts/governance/test-ask-governed-merge.sh` ends with `120 passed, 0 failed`
- `mise x -- ./gradlew checkClaudeAskRules checkAdrs spotlessCheck :build-logic:functionalTest --tests '*GovernedPaths*' --tests '*AskRules*'` succeeds
- the Task 1 Renovate extract invariant prints `0`
- `mise x -- shellcheck scripts/governance/test-check-pr.sh` and `mise x -- editorconfig-checker` succeed
</verification>

<success_criteria>
- One new governed entry (80 to 81) and one new ask rule (81 to 82); `courses.kotlin-library` is governed, the sibling
  module convention plugins are pinned as ungoverned, all checks green
- The governed-paths.txt header states the exception with its gate-wiring reason, the governed licence apply line and
  the remaining per-module residual
- The convention-plugin detekt residual no longer appears as accepted in any living record (governed-paths.txt header,
  02-SECURITY.md paragraph); ADR-0003 records the consequence
- The renovate[bot] exemption is unchanged and provably cannot reach the file
- SUMMARY contains: the Renovate invariant result, the historical records left unchanged with the reason, the note on
  ADR-0012, and that STATE.md line 138 (the open decision) is for the orchestrator to remove
</success_criteria>

<output>
Create `.planning/quick/261124-hbh-govern-build-logic-src-main-kotlin-cours/261124-hbh-SUMMARY.md` when done
</output>
