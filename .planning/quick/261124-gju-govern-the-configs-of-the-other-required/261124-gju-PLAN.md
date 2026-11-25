---
phase: quick-261124-gju
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - governance/governed-paths.txt
  - .claude/settings.json
  - scripts/governance/test-check-pr.sh
  - scripts/governance/test-ask-governed-edit.sh
  - docs/adr/0003-governance-gate-trust-boundary.md
  - renovate.json5
  - docs/tooling.md
autonomous: true
requirements: [AIDEV-06, AIDEV-08, CICD-04, TOOL-03]

estimate:
  tokens: 130000
  raw_tokens: 130000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "Author decision 1: a pull request that touches the configuration or launcher of a required CI gate without `governance-approved` fails governance-success. Newly governed (22 entries, list grows from 58 to 80): `.editorconfig`, `**/.editorconfig`, `.editorconfig-checker.json`, `.gitattributes`, `**/.gitattributes`, `REUSE.toml`, `**/REUSE.toml`, `.reuse/**`, `.prettierrc.yaml`, `.taplo.toml`, `config/spotless/**`, `.shellcheckrc`, `shellcheckrc`, `**/.shellcheckrc`, `**/shellcheckrc`, `zizmor.yml`, `zizmor.yaml`, `.gitleaks.toml`, `.gitleaksignore`, `qodana.yml`, `scripts/doctor.sh`, `tooling/doctor/**`; boundary cases in test-check-pr.sh run against the real list and prove each one, including nested files and files a pull request adds"
    - "Claude Code asks before an edit of each newly governed path: `.claude/settings.json` has exactly one `Edit(/<entry>)` ask rule per entry (59 to 81 ask rules including the config-set rule), `checkClaudeAskRules` is green, and the edit hook asks for a file matched by a `**/` entry in a subdirectory while staying silent for a near-miss name and for the root file that such an entry does not cover"
    - "Every deliberate exclusion carries its reason in the header of governance/governed-paths.txt and is pinned by an `ungoverned` boundary case: `lefthook.yml`, `scripts/format-staged.sh`, `.gitignore`, `.ecrc`, `taplo.toml`, `scripts/test-doctor.sh`, the architecture tests under `tooling/architecture`, `scripts/upstream/**`; the SUMMARY lists them, plus the flagged module convention plugin `courses.kotlin-library` (it wires the detekt config and `allWarningsAsErrors` into every module and stays outside under the recorded 01-17/01-19 exclusion)"
    - "The renovate[bot] version-only exemption is unchanged and cannot reach a newly governed path: no Renovate manager extracts a versioned dependency from any of them (extract-mode invariant; `tooling/doctor/build.gradle.kts` is a gradle package file with zero dependencies), `NEVER_EXEMPT_PATHS` stays `(governance/gsd-version)`, and every pre-existing test-check-pr.sh case still passes"
    - "Author decision 2: every npm-datasource update of type major, minor or patch waits seven days. A package rule placed in renovate.json5 overrides the three days of `security:minimumReleaseAgeNpm`; a local `renovate --platform=local --dry-run=lookup` shows no npm update younger than seven days without `pendingChecks`, covering the mise `npm:` tools and the GSD pin; pin, replacement and lock file maintenance updates keep the preset's opt-outs; `renovate-config-validator --strict renovate.json5` passes"
    - "The GSD package rule no longer sets its own `minimumReleaseAge` (the npm cooldown rule is the single source), and the renovate.json5 comments and the GSD updates section of docs/tooling.md say where the seven days for npm come from"
    - "All listed checks are green: test-check-pr.sh (>= 171 passed), test-ask-governed-edit.sh (>= 58 passed), test-ask-governed-merge.sh (120 passed), test-doctor.sh (139 passed), `checkClaudeAskRules checkToolingInventory checkAdrs spotlessCheck` and the GovernedPaths/AskRules functional tests, shellcheck on the changed scripts, editorconfig-checker and reuse lint"
  artifacts:
    - path: "governance/governed-paths.txt"
      provides: "Governed entries for the configuration and launchers of every required CI gate, the extended boundary rule and the recorded exclusions"
      contains: "tooling/doctor/**"
    - path: ".claude/settings.json"
      provides: "One Edit(/<entry>) ask rule per new governed entry"
      contains: "Edit(/**/.editorconfig)"
    - path: "scripts/governance/test-check-pr.sh"
      provides: "Boundary cases for every new entry and every recorded exclusion, and added-file cases for zizmor.yml and qodana.yml"
      contains: "boundary_case scripts/doctor.sh governed"
    - path: "scripts/governance/test-ask-governed-edit.sh"
      provides: "Edit hook cases for a leading `**/` entry"
      contains: "**/.editorconfig"
    - path: "docs/adr/0003-governance-gate-trust-boundary.md"
      provides: "Consequence recording that the configuration and launchers of the required gates are governed and outside the Renovate exemption's reach"
      contains: "scripts/doctor.sh"
    - path: "renovate.json5"
      provides: "npm cooldown package rule (major, minor, patch, seven days) and the GSD rule without its own cooldown"
      contains: "matchUpdateTypes: [\"major\", \"minor\", \"patch\"]"
    - path: "docs/tooling.md"
      provides: "GSD updates section naming the npm cooldown rule as the source of the seven days"
      contains: "## GSD updates"
  key_links:
    - from: "governance/governed-paths.txt"
      to: ".claude/settings.json"
      via: "checkClaudeAskRules (AskRulesCheck, GovernedPaths.parse): exactly one Edit(/<entry>) ask rule per entry"
      pattern: "Edit\\(/"
    - from: "governance/governed-paths.txt"
      to: "scripts/governance/check-pr.sh"
      via: "governed_globs and glob_match in scripts/governance/governed-paths.sh, shared with the Claude Code hooks"
      pattern: "governed_globs"
    - from: "renovate.json5"
      to: "mise.toml"
      via: "packageRules entry with matchDatasources npm and matchUpdateTypes major/minor/patch overriding the security:minimumReleaseAgeNpm rule for the npm: tools"
      pattern: "matchDatasources: \\[\"npm\"\\]"
---

<objective>
Close the open Phase 2 ship decision on the configs of the other required gates, and make the Renovate npm cooldown
the seven days that CICD-04 states.

Author decision 1 (2026-11-24, locked): govern the configuration and launchers of the required CI gates now, before the
Phase 2 PR. An ungoverned gate config lets an agent silently relax a required gate (an exclusion, an override, an
ignore entry) while CI stays green; detekt and Qodana are already governed, and since quick task 261124-dvp governed
edits in agent worktrees cost one merge prompt only. The author's list was verified against the repository, the CI jobs
of `ci-success` and the installed tools (see Planner findings); the plan governs the list plus the equivalents found
and records every deliberate exclusion.

Author decision 2 (2026-11-24, locked): `config:best-practices` extends `security:minimumReleaseAgeNpm`, whose package
rule lowers npm-datasource packages to three days and overrides the top-level `minimumReleaseAge: "7 days"`. A package
rule restores seven days for the npm datasource, limited to update types major, minor and patch, so that pin and
replacement updates (which carry no release timestamp) never wait forever. The GSD rule's own restatement from quick
task 261124-eum becomes redundant and is removed, so one rule owns the npm cooldown.

Out of scope, on purpose: STATE.md (the orchestrator removes the open-decision line), any change to the logic of
`scripts/governance/check-pr.sh` or to `NEVER_EXEMPT_PATHS`, the module convention plugins (recorded exclusion, flagged
in the SUMMARY), hardening `scripts/qodana.sh` against a `qodana.yml` (flagged only; governance covers it), and the
npm manager option `minimumReleaseAgePackageManager` (no npm package manager files exist yet; flagged for the frontend
phase).

Purpose: AIDEV-06 (governed changes need the label) and AIDEV-08 (ask rules for governed files) extended to the gate
configs, TOOL-03 (the doctor gate cannot be switched off silently), CICD-04 (seven-day minimum release age).
Output: 22 new governed entries with ask rules, boundary and hook tests, the documented boundary and exclusions, the
ADR-0003 consequence, the npm cooldown rule and the aligned docs.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@governance/governed-paths.txt
@renovate.json5
@docs/adr/0003-governance-gate-trust-boundary.md
@.planning/quick/261123-mub-close-pre-ship-debt-from-interim-audit-g/261123-mub-SUMMARY.md

Read only the parts named:
- scripts/governance/test-check-pr.sh: the helpers `run_case`, `entry`, `files_of` (lines 1-175) and the section
  `# --- governed boundary (WR-01) ---` (about lines 833-938) with `boundary_case`.
- scripts/governance/test-ask-governed-edit.sh: the test list heredoc (about lines 28-36), `run_case` (about line 106)
  and the section `# --- the session's own tree ---` (about lines 115-125).
- .claude/settings.json: `permissions.ask` only.
- docs/tooling.md: the `## GSD updates` section (about lines 181-208).
- scripts/governance/governed-paths.sh: `glob_match` (a leading `**/` entry matches the name in every subdirectory but
  not at the root, because the remaining literal starts with `/`).

## Planner findings (verified 2026-11-24 at 5eb35bf, installed tool versions from mise.lock)

Which required job reads what (ci.yml, `ci-success` needs build, lint, secrets, qodana, integration-test,
compose-smoke, doctor, upstream-records):
- lint, editorconfig-checker 4.0.2: reads only `.editorconfig-checker.json` at the root. Probe in a scratch repository:
  a config named `.editorconfig-checker.json` excluded the failing file (exit 0); `editorconfig-checker.json`, `.ecrc`,
  `ecrc` and `.editorconfig-checker.jsonc` were ignored (exit 1). A nested `sub/.editorconfig` with
  `trim_trailing_whitespace = false` made a violation under `sub/` pass (exit 0), so `.editorconfig` must be governed
  at every depth.
- build, Spotless (courses.root.gradle.kts): shfmt runs without flags and reads the `.editorconfig` files; Prettier gets
  `configFile(".prettierrc.yaml")` explicitly; taplo runs with `--config .taplo.toml` explicitly; the licence header
  templates are `config/spotless/license-header.txt` and `config/spotless/license-header-shell.txt`; the target set and
  its exclusions are fixed in courses.root.gradle.kts (governed). No required job runs the Prettier CLI, so other
  Prettier config names and `.prettierignore` are not read; `taplo.toml` is not read because of `--config`.
- lint, line-ending renormalisation: reads every `.gitattributes` (git applies them per directory). Attributes also
  steer the pull request view (`linguist-generated`, `-diff`) and `export-ignore` drops files from the `git archive`
  that a local `scripts/qodana.sh` run analyses.
- lint, reuse 6.3.0: a nested `sub/REUSE.toml` licensed a header-less file below it (exit 0), and the deprecated
  `.reuse/dep5` also works (exit 0 with a deprecation warning). So `REUSE.toml` at every depth and `.reuse/**`.
- lint, shellcheck 0.11.0 (run directly and by actionlint for `run:` scripts): `disable=all` in `.shellcheckrc` or
  `shellcheckrc`, in the script's directory or any parent, silenced a finding (exit 0 in all five probes).
- lint, zizmor 1.32.0 (input `.github/workflows`): root `zizmor.yml` and `zizmor.yaml` disabled the findings (exit 0),
  as do `.github/zizmor.yml|yaml` (already under `.github/**`); `.github/workflows/zizmor.yml` and `.zizmor.yml` were
  ignored.
- secrets, gitleaks 8.30.1: config precedence is `--config`, `GITLEAKS_CONFIG`, `GITLEAKS_CONFIG_TOML`, then
  `(target path)/.gitleaks.toml`; `--gitleaks-ignore-path` defaults to `.`, so a root `.gitleaksignore` drops findings.
  Neither file exists yet.
- qodana: the Qodana CLI looks up `qodana.yml` before `qodana.yaml` (JetBrains/qodana-cli,
  `internal/platform/qdyaml/yaml.go`, filenames `["qodana.yml", "qodana.yaml"]`), while `scripts/qodana.sh` validates
  only `qodana.yaml`. An added `qodana.yml` would replace the governed configuration. `.idea/` is git-ignored and
  `scripts/qodana.sh` refuses a project directory that contains `.idea/`, so it fails closed.
- doctor: `scripts/doctor.sh --ci` runs its shell checks and then `./gradlew doctor`, a task registered in
  `tooling/doctor/build.gradle.kts` that depends on `testcontainersProbe` (`tooling/doctor/src/main/kotlin/.../Doctor.kt`)
  and the root tasks `:verifyNodeVersion`, `:verifyShfmt`, `:verifyTaplo`, `:checkDaemonJvm` (root tasks are governed
  through courses.root.gradle.kts). An edit to the module's build script or probe alone can make the doctor job green
  without evaluating, so `tooling/doctor/**` is governed with `scripts/doctor.sh`.
- upstream-records: advisory, never fails on a match (UPS-04), so `scripts/upstream/**` stays outside.
- build and lint shell tests, architecture tests: test code, outside like the functional tests.
- Renovate: the extracted package files at HEAD include none of the new paths except `tooling/doctor/build.gradle.kts`,
  which has zero dependencies. No new path can take the version-only exemption, so `NEVER_EXEMPT_PATHS` stays as is.
- Renovate npm cooldown, `security:minimumReleaseAgeNpm` in renovate 44.252.8
  (`dist/config/presets/internal/security.preset.js`): one rule `matchDatasources: ["npm"]`, `minimumReleaseAge:
  "3 days"`, `internalChecksFilter: "strict"`, plus rules that set `minimumReleaseAge: null` for lockFileMaintenance,
  replacement, pin, bump, lockfileUpdate and rollback. Extended presets come before the repository's own package rules
  and later rules win. Lookup at HEAD (2026-11-24T05:02Z): `npm:renovate` proposed 44.257.6 (2026-11-21, no
  pendingChecks), `npm:@upstash/context7-mcp` 4.7.3 (2026-11-17, no pendingChecks), `npm:@playwright/mcp` 0.0.86
  (2026-11-18, no pendingChecks); the property gate of Task 3 fails at HEAD. In a scratch clone with the candidate rule
  as the first package rule, the validator passed and the gate passed: renovate 44.259.0 pendingChecks, context7-mcp fell
  back to 4.7.1 (8 days old), playwright 0.0.86 pendingChecks, GSD 1.23.0 pendingChecks.
- Module convention plugin `courses.kotlin-library.gradle.kts` (ungoverned by the recorded 01-17/01-19 exclusion)
  applies detekt with `config.setFrom(rootDirectory.file("config/detekt/detekt.yml"))` and sets `allWarningsAsErrors`,
  so an edit to it can relax detekt and the compiler gate. Not changed here; flag it for the author in the SUMMARY.

Execution notes:
- The executor runs in an agent worktree: governed edits there cost no per-edit prompt and the merge hook asks once.
  Change governed files (`governance/governed-paths.txt`, `.claude/settings.json`, `scripts/governance/**`,
  `renovate.json5`) with the Edit or Write tool, never with shell writes (ADR-0015, .claude/CLAUDE.md).
- Commits are conventional, in English, scoped `quick-261124-gju`, one per task.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Govern `.editorconfig` at every depth, end to end through list, ask rules, gate tests and hook tests</name>
  <files>governance/governed-paths.txt, .claude/settings.json, scripts/governance/test-check-pr.sh, scripts/governance/test-ask-governed-edit.sh</files>
  <precondition>`bash scripts/governance/test-check-pr.sh | tail -n 1` prints `136 passed, 0 failed` and `bash scripts/governance/test-ask-governed-edit.sh | tail -n 1` prints `55 passed, 0 failed` at the plan base.</precondition>
  <reversibility rating="reversible">Adding governed entries and ask rules is undone by deleting the lines.</reversibility>
  <action>
Prove the one new shape this plan introduces, a leading `**/` entry, through every layer before the bulk of the list
(author decision 1).

1. governance/governed-paths.txt (Edit tool):
   - In the Format paragraph of the header, add one sentence: an entry that starts with `**/` matches the name in every
     subdirectory but not at the repository root, so a root file needs an entry of its own.
   - Directly after the `config/detekt/**` entry, open a new section with a comment heading for the lint job's linters
     (one or two lines for now: editorconfig-checker reads `.editorconfig-checker.json` at the root and every
     `.editorconfig` from a file's directory up to the one with `root = true`, and shfmt in the build job reads the same
     files, so a nested `.editorconfig` can relax a subtree). Task 2 completes this comment. Add the entries
     `.editorconfig` and `**/.editorconfig`, in that order.
2. .claude/settings.json (Edit tool): in `permissions.ask`, insert `Edit(/.editorconfig)` and `Edit(/**/.editorconfig)`
   right after `Edit(/config/detekt/**)`, mirroring the list order. Keep the file valid JSON with Prettier's layout (one
   string per line).
3. scripts/governance/test-check-pr.sh (Edit tool): at the end of the `# --- governed boundary (WR-01) ---` section,
   just before the "A root renovate.json beats the governed renovate.json5" `run_case`, add a comment block "Required CI
   gate configuration (author decision of 2026-11-24)" and the cases: `boundary_case .editorconfig governed`,
   `boundary_case docs/.editorconfig governed`, `boundary_case backend/command-service/.editorconfig governed`, and
   `boundary_case docs/x.editorconfig ungoverned` with a one-line comment that a `**/` entry needs the slash.
4. scripts/governance/test-ask-governed-edit.sh (Edit tool): add the line `**/.editorconfig` to the test list heredoc
   (no other entry changes). In the `# --- the session's own tree ---` section, after the case "a governed name in a
   subdirectory is not governed", add three `run_case` lines with `edit_input Write`:
   - `$REPO/docs/.editorconfig` expects `ask`, with a reason regex in the style of the existing "a new file under a
     governed directory asks" case that matches the path `docs/.editorconfig` and the entry `**/.editorconfig` in
     backticks;
   - `$REPO/docs/x.editorconfig` expects `none` (a near-miss name);
   - `$REPO/.editorconfig` expects `none` (the test list has no root entry, which shows that a `**/` entry does not
     cover the root file).
5. Run the verify commands below, then commit: `feat(quick-261124-gju): govern .editorconfig at every depth`.
  </action>
  <verify>
    <automated>grep -cxF '**/.editorconfig' governance/governed-paths.txt  # [HEAD=0 EXPECT==1 @5eb35bf]</automated>
    <automated>grep -cxF '.editorconfig' governance/governed-paths.txt  # [HEAD=0 EXPECT==1 @5eb35bf]</automated>
    <automated>jq -r '.permissions.ask[]' .claude/settings.json | grep -cxF 'Edit(/**/.editorconfig)'  # [HEAD=0 EXPECT==1 @5eb35bf]</automated>
    <automated>bash scripts/governance/test-check-pr.sh | tail -n 1 | awk '{exit !($1 >= 140 && $3 == 0)}'  # [HEAD=136 EXPECT>=140 @5eb35bf]</automated>
    <automated>bash scripts/governance/test-ask-governed-edit.sh | tail -n 1 | awk '{exit !($1 >= 58 && $3 == 0)}'  # [HEAD=55 EXPECT>=58 @5eb35bf]</automated>
    <automated>mise x -- ./gradlew checkClaudeAskRules :build-logic:functionalTest --tests '*GovernedPaths*' --tests '*AskRules*'</automated>
  </verify>
  <done>`.editorconfig` and `**/.editorconfig` are governed entries with one ask rule each; the gate blocks a root and a
  nested `.editorconfig` without the label and passes `docs/x.editorconfig`; the edit hook asks for `docs/.editorconfig`
  and stays silent for the near-miss and the uncovered root file; checkClaudeAskRules and the GovernedPaths/AskRules
  functional tests are green; the change is committed.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Govern the remaining gate configs and launchers, document the boundary and the exclusions</name>
  <files>governance/governed-paths.txt, .claude/settings.json, scripts/governance/test-check-pr.sh, docs/adr/0003-governance-gate-trust-boundary.md</files>
  <behavior>
    - Each of these paths, touched alone by a human without the label, fails the gate: `.editorconfig-checker.json`, `.gitattributes`, `frontend/.gitattributes`, `REUSE.toml`, `backend/REUSE.toml`, `.reuse/dep5`, `.prettierrc.yaml`, `.taplo.toml`, `config/spotless/license-header.txt`, `.shellcheckrc`, `shellcheckrc`, `scripts/.shellcheckrc`, `scripts/upstream/shellcheckrc`, `zizmor.yml`, `zizmor.yaml`, `.gitleaks.toml`, `.gitleaksignore`, `qodana.yml`, `scripts/doctor.sh`, `tooling/doctor/build.gradle.kts`, `tooling/doctor/src/main/kotlin/io/github/aanbrn/courses/doctor/Doctor.kt`
    - Each recorded exclusion passes without the label: `lefthook.yml`, `scripts/format-staged.sh`, `.gitignore`, `.ecrc`, `taplo.toml`, `scripts/test-doctor.sh`, `tooling/architecture/src/main/kotlin/io/github/aanbrn/courses/architecture/ArchitectureScopes.kt`, `scripts/upstream/changed-dependencies.sh`
    - A pull request that adds `zizmor.yml` without the label fails and names it; one that adds `qodana.yml` fails and names it
    - Every pre-existing case, the Renovate exemption cases included, still passes
  </behavior>
  <action>
Complete author decision 1. Write the tests first (RED: the new governed cases fail against the list of Task 1), then
the list and the rules (GREEN).

1. scripts/governance/test-check-pr.sh (Edit tool): extend the block added in Task 1 with one `boundary_case` line per
   path in the behavior list (governed paths first, grouped by job in the same order as the list below, then a comment
   "Recorded exclusions of the gate configs, each explained in governance/governed-paths.txt" and the `ungoverned`
   lines). Add two `run_case` cases after them, in the style of the existing "a pull request adding renovate.json
   without the label fails" case, using `files_of` and `entry <path> added`: one for `zizmor.yml` (expected exit 1,
   output pattern naming `zizmor\.yml`) with a comment that the root name applies alongside `.github/zizmor.yml`, and
   one for `qodana.yml` (expected exit 1, pattern `qodana\.yml`) with a comment that the Qodana CLI reads `qodana.yml`
   before the governed `qodana.yaml`. Run the script and confirm the new governed cases fail (RED).
2. governance/governed-paths.txt (Edit tool). Entries, in this order (blank-line separated groups, each with a
   comment heading; one glob per line):
   - Lint job group (opened in Task 1, now complete): `.editorconfig`, `**/.editorconfig`, `.editorconfig-checker.json`,
     `.gitattributes`, `**/.gitattributes`, `REUSE.toml`, `**/REUSE.toml`, `.reuse/**`, `.shellcheckrc`, `shellcheckrc`,
     `**/.shellcheckrc`, `**/shellcheckrc`, `zizmor.yml`, `zizmor.yaml`. The comment states, per tool, what it reads and
     how an edit relaxes it, from the Planner findings: editorconfig-checker (root config only; every `.editorconfig` up
     to `root = true`), the renormalisation check (every `.gitattributes`; attributes also hide diffs in the pull
     request view and `export-ignore` drops files from the archive a local Qodana run analyses), reuse lint (REUSE.toml
     in any directory annotates its subtree; deprecated `.reuse/dep5`), shellcheck run directly and by actionlint
     (`.shellcheckrc` or `shellcheckrc` in a script's directory or any parent), zizmor (root `zizmor.yml` or
     `zizmor.yaml`; the `.github/` names are under `.github/**`).
   - Build job formatters group: `.prettierrc.yaml`, `.taplo.toml`, `config/spotless/**`. Comment: Spotless hands these
     to Prettier and taplo explicitly from courses.root.gradle.kts, with the licence header templates; shfmt reads the
     `.editorconfig` files listed above.
   - Secrets job group: `.gitleaks.toml`, `.gitleaksignore`. Comment: gitleaks reads both from the repository root; an
     allowlist or a fingerprint drops findings.
   - In the existing Qodana section: add `qodana.yml` between `qodana.yaml` and `scripts/qodana.sh`, and extend the
     section comment: the Qodana CLI reads `qodana.yml` before `qodana.yaml`, so an added `qodana.yml` would replace the
     governed configuration while `scripts/qodana.sh` still validates `qodana.yaml`.
   - New doctor section after the Qodana section: `scripts/doctor.sh`, `tooling/doctor/**`. Comment: the required
     `doctor` job runs `scripts/doctor.sh --ci`, which runs `./gradlew doctor`, registered with its Testcontainers probe
     in tooling/doctor; an edit to either alone can make the job green without evaluating.
   - A short comment before the three new groups (or at the end of the boundary paragraph): no Renovate manager reads a
     version from these files (`tooling/doctor/build.gradle.kts` is a gradle package file without versioned
     dependencies), so the renovate[bot] version-only exemption never applies to them and `NEVER_EXEMPT_PATHS` is
     unchanged.
   Header text:
   - Boundary rule paragraph: add that the boundary also covers the configuration of every job that `ci-success`
     requires, under every file name and directory level its tool reads, and the launchers those jobs run (author
     decision of 2026-11-24): an edit to one alone can add an exclusion, an override or an ignore entry, switch a rule
     off, or make the job pass without analysing while CI stays green.
   - "Outside the boundary" list: amend the module build scripts bullet with the exception of tooling/doctor (its build
     script registers the `doctor` task the required doctor job runs, governed below with its probe). Add bullets, each
     with its reason: the local pre-commit hook (`lefthook.yml`, `scripts/format-staged.sh`: CI runs every check
     itself); `.gitignore` (CI checks out tracked files only and every gate selects tracked files, through git for
     editorconfig-checker and shellcheck, reuse lint skipping only untracked ignored files, or a file set fixed in
     courses.root.gradle.kts for Spotless); configuration names the gates do not read here (`.ecrc`: editorconfig-checker
     4 reads only `.editorconfig-checker.json`; `taplo.toml`: Spotless passes `--config .taplo.toml`; other Prettier
     config names and `.prettierignore`: Spotless passes `.prettierrc.yaml` and never runs the Prettier CLI); `.idea/`
     (git-ignored, and `scripts/qodana.sh` refuses a directory that contains it, so a committed inspection profile
     fails the gate); the shell tests outside scripts/governance and the architecture tests under tooling/architecture
     (test code, like the functional tests); `scripts/upstream/**` (the upstream-records job is advisory and never
     fails on a match, UPS-04).
   - "Accepted residual" paragraph: add that suppression markers inside the files they suppress (shellcheck directives,
     editorconfig-checker disable comments, `gitleaks:allow`, REUSE ignore blocks, zizmor ignore comments) are not
     governed; they appear in the reviewed diff of the file they suppress.
   Keep every comment line within 120 columns and every entry printable ASCII.
3. .claude/settings.json (Edit tool): add exactly one `Edit(/<entry>)` ask rule for each entry this task adds in step 2
   (20 rules; the two entries of Task 1 already have theirs), at the positions that mirror the list order; the total
   becomes 81 including `Bash(*config-set*)`.
4. docs/adr/0003-governance-gate-trust-boundary.md: add one bullet at the end of `### Consequences`: the governed list
   also covers the configuration of every job that `ci-success` requires and the launchers those jobs run, under every
   name and directory level the tool reads (EditorConfig and editorconfig-checker, git attributes, REUSE, Prettier,
   taplo, the Spotless licence headers, shellcheck, zizmor, gitleaks, Qodana, and the doctor job's `scripts/doctor.sh`
   and `tooling/doctor/**`), by author decision of 2026-11-24, with governance/governed-paths.txt as the place that
   names them and records the exclusions; no Renovate manager reads a version from them, so the version-only exemption
   needs no new exception. Leave the front matter unchanged.
5. Run the test script (GREEN), the Renovate extract invariant, shellcheck, the Gradle checks, then commit:
   `feat(quick-261124-gju): govern the configs and launchers of every required CI gate`.
  </action>
  <verify>
    <automated>grep -v '^#' governance/governed-paths.txt | grep -c .  # [HEAD=58 EXPECT==80 @5eb35bf]</automated>
    <automated>jq '.permissions.ask | length' .claude/settings.json  # [HEAD=59 EXPECT==81 @5eb35bf]</automated>
    <automated>for p in .editorconfig-checker.json .gitattributes '**/.gitattributes' REUSE.toml '**/REUSE.toml' '.reuse/**' .prettierrc.yaml .taplo.toml 'config/spotless/**' .shellcheckrc shellcheckrc '**/.shellcheckrc' '**/shellcheckrc' zizmor.yml zizmor.yaml .gitleaks.toml .gitleaksignore qodana.yml scripts/doctor.sh 'tooling/doctor/**'; do grep -qxF -- "$p" governance/governed-paths.txt || echo "missing $p"; done | grep -c missing  # [HEAD=20 EXPECT==0 @5eb35bf]</automated>
    <automated>grep -c '^boundary_case ' scripts/governance/test-check-pr.sh  # [HEAD=54 EXPECT>=87 @5eb35bf]</automated>
    <automated>bash scripts/governance/test-check-pr.sh | tail -n 1 | awk '{exit !($1 >= 171 && $3 == 0)}'  # [HEAD=136 EXPECT>=171 @5eb35bf]</automated>
    <automated>LOG_LEVEL=debug LOG_FORMAT=json mise x -- renovate --platform=local --dry-run=extract --onboarding=false 2>&1 | jq -e -s '[.[] | select(.msg == "Extracted dependencies") | .packageFiles[][]] as $p | ($p | length) > 0 and ([$p[] | select(.packageFile | test("^(tooling/doctor/|scripts/doctor\\.sh$|config/spotless/|\\.reuse/|qodana\\.yml$|zizmor\\.ya?ml$|\\.gitleaks(\\.toml|ignore)$|\\.editorconfig-checker\\.json$|\\.prettierrc\\.yaml$|\\.taplo\\.toml$)|(^|/)(\\.editorconfig|\\.gitattributes|REUSE\\.toml|\\.?shellcheckrc)$")) | .deps | length] | add // 0) == 0'  # [HEAD=true EXPECT==true @5eb35bf] invariant the no-new-exception decision rests on, so it already holds at HEAD</automated>
    <automated>grep -c 'scripts/doctor.sh' docs/adr/0003-governance-gate-trust-boundary.md  # [HEAD=0 EXPECT>=1 @5eb35bf]</automated>
    <automated>mise x -- shellcheck scripts/governance/test-check-pr.sh scripts/governance/test-ask-governed-edit.sh</automated>
    <automated>mise x -- ./gradlew checkClaudeAskRules checkAdrs spotlessCheck :build-logic:functionalTest --tests '*GovernedPaths*' --tests '*AskRules*'</automated>
  </verify>
  <done>The list has 80 entries, with the 20 new ones of this task in the documented groups and comments; settings.json
  has 81 ask rules and checkClaudeAskRules is green; the header states the extended boundary, the doctor exception to
  the module build scripts bullet, every recorded exclusion with its reason and the suppression-marker residual;
  test-check-pr.sh passes with at least 171 cases, the RED run having failed on the new governed cases first; the
  Renovate extract invariant holds; ADR-0003 carries the consequence and checkAdrs is green; the change is committed.</done>
</task>

<task type="auto">
  <name>Task 3: Restore the seven-day Renovate cooldown for npm packages and drop the GSD restatement</name>
  <files>renovate.json5, docs/tooling.md</files>
  <precondition>`mise x -- renovate --version` prints a version, and the npm registry is reachable for the lookup (a lookup at the plan base fails the cooldown gate, see Planner findings).</precondition>
  <reversibility rating="reversible">A package rule and two comment edits; reverting the commit restores the previous cooldown.</reversibility>
  <action>
Implement author decision 2.

1. Baseline: run the lookup gate of the verify block once before editing and record in the SUMMARY its result and, for
   every npm-datasource update, depName, newVersion, releaseTimestamp and pendingChecks (date-dependent record, not a
   gate).
2. renovate.json5 (Edit tool):
   - Add a new first entry of `packageRules`, under a comment heading "Cooldown (CICD-04)": description "npm packages:
     restore the seven-day cooldown that security:minimumReleaseAgeNpm lowers to three days";
     `matchDatasources: ["npm"]`; `matchUpdateTypes: ["major", "minor", "patch"]`; `minimumReleaseAge: "7 days"`. The
     comment says: the package rules of extended presets come before these and later rules win, so this rule overrides
     the three days that `security:minimumReleaseAgeNpm` (part of `config:best-practices`) sets for the npm datasource;
     it is limited to major, minor and patch so the preset's opt-outs for pin, replacement, lock file maintenance, bump,
     lockfileUpdate and rollback stay intact, because those update types carry no release timestamp and would wait
     forever; it covers the mise `npm:` tools, the GSD pin and any future package.json dependency; the npm datasource
     reports release timestamps, so the default timestamp-required behaviour applies.
   - Extend the comment above the top-level `minimumReleaseAge: "7 days"`: package rules override it, and
     `config:best-practices` lowers npm packages to three days through one, so the first package rule restores seven
     days for them.
   - Update the comment "Rules apply in order and later rules win, ..." so it names the cooldown rule as coming first.
   - In the GSD package rule (`matchDepNames: ["@opengsd/gsd-core"]`): delete its `minimumReleaseAge: "7 days"` line
     and the two comment lines directly above that line, and shorten the description to "GSD (agent commands, agents and
     hooks): never automerge, with the review steps". Keep `automerge: false`, `prBodyNotes`, the rule's position after
     every automerge rule and its leading comment. Decision: the npm cooldown rule is the single source of the seven
     days; the restatement was a workaround for the gap this rule closes, and the gate below covers the GSD pin because
     its datasource is npm.
3. docs/tooling.md, `## GSD updates`, the "Renovate is the signal" bullet: replace the clause that attributes the
   seven-day age to the GSD rule with: like every npm update, the pin waits the repository's seven-day minimum release
   age, which a package rule restores because `config:best-practices` lowers npm packages to three days. Keep the rest
   of the bullet, and write no version numbers.
4. Run the verify commands. After the change, record in the SUMMARY the same per-update fields as in step 1. Commit:
   `fix(quick-261124-gju): restore the seven-day Renovate cooldown for npm packages`.
  </action>
  <verify>
    <automated>mise x -- renovate-config-validator --strict renovate.json5</automated>
    <automated>grep -c 'matchUpdateTypes: \["major", "minor", "patch"\]' renovate.json5  # [HEAD=0 EXPECT==1 @5eb35bf]</automated>
    <automated>grep -c "Restates the repository" renovate.json5  # [HEAD=1 EXPECT==0 @5eb35bf]</automated>
    <automated>grep -c 'restated on the GSD rule' docs/tooling.md  # [HEAD=1 EXPECT==0 @5eb35bf]</automated>
    <automated>LOG_LEVEL=debug LOG_FORMAT=json mise x -- renovate --platform=local --dry-run=lookup --onboarding=false 2>&1 | jq -e -s '[.[] | select(.msg == "packageFiles with updates") | .config[][] | .deps[] | select(.datasource == "npm") | .updates[]? | select(.updateType | IN("major", "minor", "patch"))] as $u | ($u | length) > 0 and ([$u[] | select(.pendingChecks != true and ((.releaseTimestamp | sub("\\.[0-9]+Z$"; "Z") | fromdateiso8601) > (now - 7 * 86400)))] | length == 0)'  # [HEAD=false EXPECT==true @5eb35bf] 3 violators at 2026-11-24T05:02Z: npm:renovate, npm:@upstash/context7-mcp, npm:@playwright/mcp</automated>
    <automated>mise x -- ./gradlew checkToolingInventory spotlessCheck</automated>
  </verify>
  <done>The validator passes; the npm cooldown rule is the first package rule with the three fields and the comment; no
  npm-datasource major, minor or patch update younger than seven days lacks pendingChecks in a local lookup (the GSD pin
  and the mise `npm:` tools included); the GSD rule keeps automerge false and its review notes without its own
  cooldown; the renovate.json5 comments and the GSD updates section name the npm rule as the source; the before and
  after lookup records are in the SUMMARY; the change is committed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| agent edit to repository | An agent session can change gate configuration in the pull request it prepares; the governed list, the ask rules and the hooks make such a change visible and label-gated |
| pull request to CI gates | Every required job runs from the pull request's own tree (ADR-0003), so a config file in the pull request decides what the gate checks |
| npm registry to Renovate | New npm releases reach the repository through Renovate pull requests; the cooldown gives scanners and the community time to catch a malicious or withdrawn release |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-gju-01 | Tampering | Root configs of required gates (`.editorconfig-checker.json`, `.editorconfig`, `.gitattributes`, `REUSE.toml`, `.prettierrc.yaml`, `.taplo.toml`, `config/spotless/**`) | high | mitigate | Governed entries with one ask rule each (Tasks 1, 2); boundary cases against the real list prove the label is required; checkClaudeAskRules keeps list and rules in sync |
| T-gju-02 | Tampering | Nested and alternate config names the tools also read (nested `.editorconfig`, `.gitattributes`, `REUSE.toml`, `.shellcheckrc`, `shellcheckrc`; root `zizmor.yml`/`zizmor.yaml`; `qodana.yml`; `.gitleaks.toml`, `.gitleaksignore`; `.reuse/dep5`) | high | mitigate | `**/` entries and alternate names, each verified against the installed tool (Planner findings); boundary cases for nested paths and added-file cases for `zizmor.yml` and `qodana.yml` (Task 2); hook cases for a leading `**/` entry (Task 1) |
| T-gju-03 | Tampering | Required doctor job (`scripts/doctor.sh`, `tooling/doctor/**`) | medium | mitigate | Both governed (Task 2), with boundary cases for the script, the module build script and the probe |
| T-gju-04 | Elevation of privilege | renovate[bot] version-only exemption in check-pr.sh | medium | mitigate | Extract invariant proves no Renovate manager reads a version from a newly governed path; `NEVER_EXEMPT_PATHS` and the exemption logic unchanged; every pre-existing case still passes (Task 2) |
| T-gju-05 | Tampering | npm-datasource updates (mise `npm:` tools, GSD pin) under a three-day cooldown | high | mitigate | First package rule restores seven days for major, minor and patch; lookup property gate fails at HEAD and passes after (Task 3) |
| T-gju-06 | Denial of service | Cooldown applied to update types without release timestamps | low | mitigate | `matchUpdateTypes` limited to major, minor, patch, so the preset's null cooldown for pin, replacement, lock file maintenance, bump, lockfileUpdate and rollback stays in force (Task 3) |
| T-gju-07 | Repudiation | Ask rules drift from the governed list | low | mitigate | checkClaudeAskRules runs in Tasks 1 and 2 and in `./gradlew build` on CI |
| T-gju-08 | Tampering | Module convention plugin `courses.kotlin-library` (detekt config path, `allWarningsAsErrors`) and in-file suppression markers | medium | accept | Recorded exclusion of module convention plugins (01-17, 01-19) stays; flagged to the author in the SUMMARY for a decision; suppression markers appear in the reviewed diff of the file they suppress (documented residual, Task 2) |
| T-gju-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package-manager install in this plan; no dependency is added or changed, so the package legitimacy gate has nothing to audit |
</threat_model>

<verification>
Run from the checkout root after all three tasks:
- `bash scripts/governance/test-check-pr.sh` ends with at least `171 passed, 0 failed`
- `bash scripts/governance/test-ask-governed-edit.sh` ends with at least `58 passed, 0 failed`
- `bash scripts/governance/test-ask-governed-merge.sh` ends with `120 passed, 0 failed`
- `bash scripts/test-doctor.sh` ends with `139 passed, 0 failed`
- `mise x -- ./gradlew checkClaudeAskRules checkToolingInventory checkAdrs spotlessCheck :build-logic:functionalTest --tests '*GovernedPaths*' --tests '*AskRules*'` succeeds
- `mise x -- renovate-config-validator --strict renovate.json5` succeeds
- the Task 3 lookup gate prints `true`
- `mise x -- editorconfig-checker` and `mise x -- reuse lint` succeed
- `mise x -- shellcheck scripts/governance/test-check-pr.sh scripts/governance/test-ask-governed-edit.sh` succeeds
</verification>

<success_criteria>
- 22 new governed entries (58 to 80) and 22 new ask rules (59 to 81), all checks green
- Every gate config and launcher that a required CI job reads is governed under every name and depth its tool reads,
  or listed as a recorded exclusion with its reason in governance/governed-paths.txt and an `ungoverned` boundary case
- The renovate[bot] exemption is unchanged and provably cannot reach a newly governed path
- npm-datasource major, minor and patch updates wait seven days; the GSD rule relies on the npm cooldown rule
- SUMMARY contains: the included paths with the evidence for each, the deliberately left-out paths with reasons, the
  flagged items for the author (`courses.kotlin-library` wiring detekt and `allWarningsAsErrors`; optional hardening of
  `scripts/qodana.sh` to refuse a `qodana.yml`; `minimumReleaseAgePackageManager` for the npm manager once a
  package.json lands), and the before and after lookup records
</success_criteria>

<output>
Create `.planning/quick/261124-gju-govern-the-configs-of-the-other-required/261124-gju-SUMMARY.md` when done
</output>
