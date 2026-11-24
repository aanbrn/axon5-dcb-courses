---
phase: quick-261124-eum
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - renovate.json5
  - scripts/governance/check-pr.sh
  - scripts/governance/test-check-pr.sh
  - scripts/doctor.sh
  - scripts/test-doctor.sh
  - docs/tooling.md
  - docs/adr/0003-governance-gate-trust-boundary.md
autonomous: true
requirements: [CICD-04, AIDEV-07, AIDEV-06]

estimate:
  tokens: 90000
  raw_tokens: 90000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "Renovate reads `governance/gsd-version` as the npm package `@opengsd/gsd-core`: a local `renovate --platform=local --dry-run=extract` run from the checkout root logs the package file `governance/gsd-version` with depName `@opengsd/gsd-core`, the current pin as currentValue and datasource `npm`, and `renovate-config-validator --strict renovate.json5` passes"
    - "The GSD update is never automerged and waits for the repository's standard seven-day cooldown: the package rule for `@opengsd/gsd-core` (matchDepNames) sets `automerge: false` and `minimumReleaseAge: \"7 days\"` (restating the top-level value, because the `security:minimumReleaseAgeNpm` rule inside `config:best-practices` otherwise lowers every npm-datasource dependency to three days), sits after every automerge rule, has no `groupName`, and its `prBodyNotes` list the review steps; labels come from the existing rules (`deps`, plus `deps:npm` from the npm datasource rule), so `.github/labels.yml` stays unchanged"
    - "Author decision 2026-11-24 (prefer NOT exempting): the renovate[bot] version-only exemption in scripts/governance/check-pr.sh deliberately does not cover `governance/gsd-version`. A renovate[bot] pull request whose only governed change is a version-only bump of the pin fails without `governance-approved`, names the file and explains why; with the label it passes; every other governed file keeps the exemption (all 132 pre-existing test-check-pr.sh cases still pass)"
    - "After the pin moves, a checkout whose installed GSD differs from the pin fails doctor's `gsd` step with the reinstall command for the pinned version (`npx -y @opengsd/gsd-core@<pin> --claude --local`) as its fix"
    - "docs/tooling.md has a `## GSD updates` section with the flow: the Renovate pull request changes only the pin; before merge read the release notes and the hook diff, install locally and get doctor green, add `governance-approved`, merge by hand; after merge other checkouts' doctor asks for a reinstall. It states that the exemption deliberately does not cover the pin, and the page still writes no version numbers. ADR-0003 records the exclusion"
    - "The pin is not bumped: `governance/gsd-version` still holds the version it holds at HEAD"
    - "`checkToolingInventory`, `checkAdrs` and `spotlessCheck` stay green, and both shell test suites (test-check-pr.sh, test-doctor.sh) pass"
  artifacts:
    - path: "renovate.json5"
      provides: "Regex custom manager for governance/gsd-version (npm @opengsd/gsd-core) and a never-automerge package rule with the GSD review steps"
      contains: "@opengsd/gsd-core"
    - path: "scripts/governance/check-pr.sh"
      provides: "NEVER_EXEMPT_PATHS: governed paths that never take the renovate[bot] version-only exemption"
      contains: "NEVER_EXEMPT_PATHS"
    - path: "scripts/governance/test-check-pr.sh"
      provides: "Cases proving a renovate[bot] bump of the GSD pin needs the label"
      contains: "governance/gsd-version"
    - path: "scripts/doctor.sh"
      provides: "Version-mismatch fix that reinstalls the pinned GSD"
      contains: "install_fix"
    - path: "docs/tooling.md"
      provides: "GSD updates section and the updated GSD inventory row"
      contains: "## GSD updates"
    - path: "docs/adr/0003-governance-gate-trust-boundary.md"
      provides: "Consequence recording that the GSD pin is never exempt"
      contains: "governance/gsd-version"
  key_links:
    - from: "renovate.json5"
      to: "governance/gsd-version"
      via: "customManagers regex entry: managerFilePatterns anchored on the root path, depNameTemplate @opengsd/gsd-core, datasourceTemplate npm"
      pattern: "gsd-version"
    - from: "scripts/governance/check-pr.sh"
      to: "governance/gsd-version"
      via: "NEVER_EXEMPT_PATHS consulted before the renovate[bot] version-only exemption in check_governed_files"
      pattern: "NEVER_EXEMPT_PATHS"
    - from: "docs/tooling.md"
      to: "scripts/governance/check-pr.sh"
      via: "GSD updates section names NEVER_EXEMPT_PATHS and the governance-approved label"
      pattern: "NEVER_EXEMPT_PATHS"
    - from: "scripts/doctor.sh"
      to: "governance/gsd-version"
      via: "check_gsd reads the pin and prints install_fix on a version mismatch"
      pattern: "install_fix"
---

<objective>
Make a new GSD release visible and keep its adoption a deliberate, human-gated step.

Today nothing signals a new GSD release: `governance/gsd-version` pins the installed version, but `renovate.json5` has no
manager for that file, so 1.23.0 (published 2026-11-23) went unnoticed. This plan adds a Renovate regex custom manager
that reads the pin as the npm package `@opengsd/gsd-core` (standard seven-day cooldown, never automerged), makes the
governance gate require `governance-approved` for that pin even when renovate[bot] changes only the version (author
decision 2026-11-24: a GSD release replaces hooks that execute in the author's Claude Code session, so a human gate is
wanted), aligns doctor's version-mismatch fix with the new flow, and documents the flow in `docs/tooling.md` and ADR-0003.

Out of scope, on purpose: bumping the pin to 1.23.0 (that happens after the Phase 2 PR, through the flow this plan
documents), new labels, and any change to the governed-path grammar or list.

Purpose: CICD-04 (Renovate tracks every version source with a cooldown), AIDEV-07 (GSD updates only deliberately, with
changelog and hook diff review) and AIDEV-06 (governed changes need the label) extended to the GSD pin.
Output: the manager and rule in `renovate.json5`, the never-exempt list in `scripts/governance/check-pr.sh` with tests,
the doctor fix, the `GSD updates` section and the ADR-0003 consequence.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md
@renovate.json5
@scripts/governance/check-pr.sh
@docs/tooling.md
@docs/adr/0003-governance-gate-trust-boundary.md

Read only the parts named:
- scripts/governance/test-check-pr.sh: the helpers `run_case`, `patch_file`, `entry`, `files_of` (lines 1-175) and the
  section `# --- Renovate version-only exemption ---` up to `# --- fixtures for mise sidecar lockfiles ---`
  (about lines 309-355). The fixture `mise_bump` and the variable `BOT` are defined there.
- scripts/doctor.sh: `check_gsd` (about lines 345-420), in particular `install_fix` and the version-mismatch `report fail`
  call near line 380.
- scripts/test-doctor.sh: the "version differs" case (about lines 329-334).
- governance/governed-paths.txt: header only; it is NOT modified by this plan.

Facts established while planning (2026-11-24, Renovate 44.252.8 via mise, HEAD d7e456e):
- `governance/**`, `renovate.json5` and `scripts/governance/**` are governed paths; `docs/tooling.md`, `docs/adr/**`,
  `scripts/doctor.sh` and `scripts/test-doctor.sh` are not. The executor runs in an agent worktree, where the governance
  edit hook does not ask (ADR-0015, quick task 261124-dvp); the merge back asks the author once. Edit governed files with
  the Edit/Write tools, never with shell writes.
- At HEAD the renovate[bot] exemption WOULD cover the pin: a patch that replaces one `x.y.z` line with another normalises
  to `<VER>` on both sides in `normalize`, the status is `modified`, so `is_version_only` succeeds. Hence the change in
  Task 2.
- A scratch run with this plan's manager proved extraction (`"depName":"@opengsd/gsd-core","currentValue":<pin>,
  "datasource":"npm"`) and lookup (`newVersion` 1.23.0, `pendingChecks: true` for the one-day-old release, branch
  `renovate/opengsd-gsd-core-1.x`, source repository detected, so the PR body carries release notes). The branch name
  matches the `renovate/*` rule of check-pr.sh.
- The validator prints a pre-existing `WARN: RE2 not usable, falling back to RegExp` with a stack trace, then
  `Config validated successfully`; exit status 0 is what counts. Keep regexes RE2-compatible (no lookarounds, no
  backreferences), because the hosted Renovate uses RE2.
- `npm diff --ignore-scripts --diff=@opengsd/gsd-core@<old> --diff=@opengsd/gsd-core@<new> hooks` lists and diffs only
  the package's `hooks/` files and runs no lifecycle scripts (verified between the current pin and 1.23.0: 20 hook files).
- Renovate's `prBodyNotes` supports Handlebars templating; `currentVersion` and `newVersion` are documented template
  fields (docs.renovatebot.com/templates).
- The top-level `minimumReleaseAge: "7 days"` does NOT reach npm-datasource dependencies. `config:best-practices`
  extends `security:minimumReleaseAgeNpm`, which adds a package rule `matchDatasources: ["npm"]`,
  `minimumReleaseAge: "3 days"`, and package rules override top-level values. Proven in a scratch repository with this
  repository's config: a regex-managed npm dependency whose newest release was five days old got no `pendingChecks`
  under the top-level seven days, and `pendingChecks: true` once its own package rule restated
  `minimumReleaseAge: "7 days"`. Hence the explicit value on the GSD rule in Task 1. The same gap affects the existing
  mise `npm:` tools; that is reported to the author as a follow-up and is NOT changed by this plan.
- docs/tooling.md rule: "Versions are never written in this document." Use `<old>`, `<new>` and `<pin>` in prose.
- Baselines: test-check-pr.sh 132 passed, 0 failed; test-doctor.sh 139 passed, 0 failed (about 35 s).
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Renovate reads the GSD pin as @opengsd/gsd-core, never automerges it, and lists the review steps</name>
  <precondition>`mise x -- renovate --version` prints a version (npm:renovate from mise.toml is installed); no network is needed for the extraction check.</precondition>
  <files>renovate.json5</files>
  <action>
Edit `renovate.json5` (governed; use the Edit tool) following its existing conventions.

1. Custom manager (CICD-04). In `customManagers`, directly after the `.java-version` entry (the other bare single-version
   file), add a regex entry with these keys, in the order the neighbouring entries use: `customType: "regex"`;
   `description` saying it is the GSD version pin in governance/gsd-version, read as the npm package @opengsd/gsd-core,
   with the GSD installation under .claude/ not committed; `managerFilePatterns: ["/^governance/gsd-version$/"]`
   (anchored on the root path, the way the qodana.yaml entry anchors its file); `matchStrings` with exactly one string
   `^(?<currentValue>\\d+\\.\\d+\\.\\d+)\\s*$` (same shape as the `.java-version` matcher, RE2-compatible);
   `depNameTemplate: "@opengsd/gsd-core"`; `datasourceTemplate: "npm"`. Do not add `versioningTemplate` or
   `extractVersionTemplate` (the npm datasource's default npm versioning fits an exact x.y.z pin), and put no cooldown
   key on the manager (the cooldown belongs to the package rule below).

2. Package rule. In `packageRules`, directly after the rule described "Agent tooling (MCP servers): never automerge" and
   before the `// JDK: LTS majors only` comment, add a rule preceded by a short `//` comment explaining why it exists:
   a GSD release replaces the commands, agents and hooks that run in the maintainer's Claude Code session, so its pull
   request is reviewed and merged by hand, it never takes the governance exemption (scripts/governance/check-pr.sh), and
   the flow is in docs/tooling.md (GSD updates); it stays after every automerge rule because later rules win. Keys:
   `description` along the lines of "GSD (agent commands, agents and hooks): never automerge, seven-day cooldown, with
   the review steps"; `matchDepNames: ["@opengsd/gsd-core"]`; `automerge: false`; `minimumReleaseAge: "7 days"`, with
   a comment saying it restates the repository's standard cooldown because the `security:minimumReleaseAgeNpm` rule of
   `config:best-practices` lowers npm packages to three days and package rules override the top-level value (the npm
   datasource reports release timestamps, so the default timestamp-required behaviour applies, unlike the JDK rule; do
   not add `minimumReleaseAgeBehaviour`); `prBodyNotes` with exactly one English string that says:
   this pull request changes only `governance/gsd-version` because the GSD installation is not committed; before merging,
   read the release notes, review the hook diff with
   `npm diff --ignore-scripts --diff=@opengsd/gsd-core@{{{currentVersion}}} --diff=@opengsd/gsd-core@{{{newVersion}}} hooks`,
   check out this branch, install the release with `npx -y @opengsd/gsd-core@{{{newVersion}}} --claude --local` and run
   `scripts/doctor.sh` until it is green; the pin never takes the governance exemption, so add the `governance-approved`
   label and merge by hand (docs/tooling.md, GSD updates). Do NOT add `groupName` (one pull request per release line),
   `addLabels` (top-level `deps` plus `deps:npm` from the existing `matchDatasources: ["npm"]` rule already apply, and a
   new label would need a change to the governed `.github/labels.yml`) or any automerge-related key other than
   `automerge: false`.

3. Do not change `governance/gsd-version` (the bump to the next release happens after the Phase 2 PR).

4. Prove it end to end from the checkout root (the worktree root): run the validator and the local extraction in the
   verify block. Then run the lookup once for the record, `LOG_LEVEL=debug LOG_FORMAT=json mise x -- renovate
   --platform=local --dry-run=lookup --onboarding=false`, and note in the SUMMARY the `newVersion`, `pendingChecks`
   and `branchName` it logs for `@opengsd/gsd-core` (needs network to the npm registry; date-dependent, so it is a
   record, not a gate).

5. Format with `mise x -- ./gradlew spotlessApply` (Prettier formats `*.json5`), then commit, for example
   `feat(quick-261124-eum): track the GSD version pin with a Renovate custom manager`.
  </action>
  <verify>
    <automated>mise x -- renovate-config-validator --strict renovate.json5</automated>
    <automated>LOG_LEVEL=debug LOG_FORMAT=json mise x -- renovate --platform=local --dry-run=extract --onboarding=false 2>&1 | grep -o '"depName":"@opengsd/gsd-core"[^}]*' | grep -c '"currentValue":"1.22.0","datasource":"npm"'  # [HEAD=0 EXPECT>=1 @d7e456e]</automated>
    <automated>LOG_LEVEL=debug LOG_FORMAT=json mise x -- renovate --platform=local --dry-run=extract --onboarding=false 2>&1 | grep -c '"packageFile":"governance/gsd-version"'  # [HEAD=0 EXPECT>=1 @d7e456e]</automated>
    <automated>grep -v '^ *//' renovate.json5 | grep -c '@opengsd/gsd-core'  # [HEAD=0 EXPECT>=3 @d7e456e]</automated>
    <automated>grep -c 'minimumReleaseAge: "7 days"' renovate.json5  # [HEAD=1 EXPECT==2 @d7e456e]</automated>
    <automated>grep -cx '1.22.0' governance/gsd-version  # [HEAD=1 EXPECT==1 @d7e456e GUARD MUTATION: a bump of governance/gsd-version]</automated>
  </verify>
  <done>The validator exits 0; the extraction from the real checkout logs `governance/gsd-version` with depName `@opengsd/gsd-core`, the current pin and datasource `npm`; the package rule is after every automerge rule with `matchDepNames`, `automerge: false`, `minimumReleaseAge: "7 days"` (with the comment on the three-day npm preset) and the templated review note, and no group or label; the pin is unchanged; the change is committed.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: The governance gate never exempts a renovate[bot] bump of the GSD pin</name>
  <files>scripts/governance/test-check-pr.sh, scripts/governance/check-pr.sh</files>
  <behavior>
    - A renovate[bot] pull request (head ref `renovate/opengsd-gsd-core-1.x`) whose only change is a version-only patch of `governance/gsd-version`, without the label: exit 1, output names `governance/gsd-version`.
    - Same input: the output contains the explanation line `never exempt: governance/gsd-version`.
    - Same input with `governance-approved`: exit 0, output `carries 'governance-approved'`.
    - A renovate[bot] pull request that bumps `mise.toml` (the existing `mise_bump` fixture, which alone is exempt) together with the pin, without the label: exit 1, output names `governance/gsd-version`.
    - Every pre-existing case keeps its verdict (132 cases), in particular the version-only exemption for `mise.toml`, `mise.lock`, workflows and sidecar lockfiles.
  </behavior>
  <action>
Both files are governed (`scripts/governance/**`); use the Edit tool. Implements the author decision of 2026-11-24 to keep
a human gate on the pin (AIDEV-06, AIDEV-07).

RED. In `scripts/governance/test-check-pr.sh`, after the last case of the `# --- Renovate version-only exemption ---`
section (the "renovate with the label passes even for a non-version change" case) and before
`# --- fixtures for mise sidecar lockfiles ---`, add a sub-section with a one-line comment (the GSD pin selects the hooks
that run in the maintainer's Claude Code session, so a Renovate bump of it always needs the label). Build the fixture
with the existing helpers: `patch_file gsd-bump` holding a single hunk header `@@ -1 +1 @@`, the removed line `1.4.0`
and the added line `1.5.0` (versions chosen so the fixture is clearly synthetic); `gsd_bump_files` from
`files_of gsd-bump` with one `entry governance/gsd-version modified` carrying that patch; `gsd_mixed_files` from
`files_of gsd-mixed` with the `mise_bump` entry for `mise.toml` and the pin entry. Add the four cases from `<behavior>`
with `run_case`, passing `PR_AUTHOR="$BOT" PR_HEAD_REF=renovate/opengsd-gsd-core-1.x` and `PR_LABELS_JSON="$LABEL_NONE"`
or `"$LABEL_APPROVED"`; escape dots in the expected-output regexes as the neighbouring cases do. Run
`bash scripts/governance/test-check-pr.sh` and confirm the new cases that expect exit 1 fail against the unchanged gate
(they pass the exemption today); record that in the SUMMARY.

GREEN. In `scripts/governance/check-pr.sh`:
- Next to `BOT_AUTHOR`, add `readonly NEVER_EXEMPT_PATHS=(governance/gsd-version)` (the `readonly NAME=(...)` form
  `scripts/doctor.sh` uses), with a comment: governed paths that never take the renovate[bot] exemption, compared byte
  for byte as whole paths; the GSD pin selects the release whose hooks and agents run in the maintainer's Claude Code
  session, so its bump is reviewed like code (docs/tooling.md, GSD updates; ADR-0003).
- Add a function `is_never_exempt` that succeeds when its argument equals an entry of `NEVER_EXEMPT_PATHS` exactly (a
  plain loop with `[[ $1 == "$entry" ]]`, no glob matching), and fails for an empty argument.
- In `check_governed_files`, declare a local array for never-exempt hits next to `touched`, `blocked` and `exempt`. In
  the loop, before the existing exemption condition, when `PR_AUTHOR` is `$BOT_AUTHOR` and `is_never_exempt` holds for
  `$filename` or for a non-empty `$previous`, append the path to that array and skip the exemption, so the file lands in
  `blocked`. Keep the existing exemption condition otherwise unchanged.
- In the failure block (the one that prints "this pull request touches governed files:"), after the list of blocked
  files and before the "ask a maintainer" line, print one line per never-exempt hit to standard error:
  `governance: never exempt: <path> (a GSD release replaces the hooks that run in the maintainer's Claude Code session)`.
  Guard the loop with a length check so an empty array never expands under `set -u`, as the function already does for
  `blocked`.
- Extend rule 1 of the header comment with one sentence: the paths in `NEVER_EXEMPT_PATHS` (the GSD version pin) never
  take the exemption, because a new GSD release changes hooks that execute in the maintainer's session.

Run the suite again until it reports at least 136 passed and 0 failed, run shellcheck, then commit, for example
`feat(quick-261124-eum): never exempt the GSD version pin from the governance label`.
  </action>
  <verify>
    <automated>bash scripts/governance/test-check-pr.sh | tail -n 1 | awk '{exit !($1 >= 136 && $3 == 0)}'  # [HEAD=132 EXPECT>=136 @d7e456e]</automated>
    <automated>grep -v '^ *#' scripts/governance/check-pr.sh | grep -c 'NEVER_EXEMPT_PATHS'  # [HEAD=0 EXPECT>=2 @d7e456e]</automated>
    <automated>grep -c 'governance/gsd-version' scripts/governance/test-check-pr.sh  # [HEAD=0 EXPECT>=4 @d7e456e]</automated>
    <automated>mise x -- shellcheck scripts/governance/check-pr.sh scripts/governance/test-check-pr.sh</automated>
  </verify>
  <done>The new cases failed before the gate change and pass after it; test-check-pr.sh reports at least 136 passed, 0 failed; a renovate[bot] version-only bump of the pin fails without the label with the explanation line and passes with it; the existing exemption cases are unchanged; shellcheck is clean; the change is committed.</done>
</task>

<task type="auto">
  <name>Task 3: Doctor asks stale checkouts to reinstall the pin, and the GSD update flow is documented</name>
  <files>scripts/doctor.sh, scripts/test-doctor.sh, docs/tooling.md, docs/adr/0003-governance-gate-trust-boundary.md</files>
  <action>
None of these four files is governed. Implements the flow the author described on 2026-11-24 (AIDEV-07).

1. Doctor fix (`scripts/doctor.sh`, `check_gsd`). In the version-mismatch `report fail gsd "installed GSD ... differs
   from governance/gsd-version ..."` call near line 380, replace the second argument (the fix text, which today tells the
   user to run the update command and then edit the pin) with `"$install_fix"`, the reinstall command for the pinned
   version that the function already builds. The pin now moves first, through the Renovate pull request, so every
   mismatch is repaired by installing the pin; keep the message text unchanged. Adjust the comment above `check_gsd` only
   if it contradicts this. In `scripts/test-doctor.sh`, in the "version differs" case, change the expected fix line to
   the install command for the version that case writes into the pin: `^      fix: npx -y @opengsd/gsd-core@` followed
   by that version with escaped dots and ` --claude --local`. Keep the number of assertions unchanged.

2. `docs/tooling.md`, GSD row of the Tools table: keep the Version source `governance/gsd-version` and rewrite the
   Installation cell to say the installation command `npx -y @opengsd/gsd-core@<version from governance/gsd-version>
   --claude --local` stays, and that Renovate proposes new releases, adopted only after the review in
   [GSD updates](#gsd-updates) (AIDEV-07). Prettier realigns the table.

3. `docs/tooling.md`, new section `## GSD updates`, placed directly after `## MCP servers` and before
   `## Claude Code skills`, in the page's bullet style. Write no version numbers (use `<old>`, `<new>`, `<pin>`). Cover:
   - What is committed: only the pin `governance/gsd-version`; the installation under `.claude/` (commands, agents,
     hooks, `gsd-core`) is ignored. Doctor's `gsd` step (local only, skipped by `--ci`) checks that the installed version
     equals the pin and that every file matches the hashes of the release manifest.
   - The signal: a Renovate regex custom manager reads the pin as the npm package `@opengsd/gsd-core`, with the
     repository's seven-day minimum release age, restated on the GSD rule because `config:best-practices` lowers npm
     packages to three days; the pull request comes from a `renovate/opengsd-gsd-core-*` branch, carries `deps` and
     `deps:npm`, is never automerged, changes only the pin, and its body repeats the steps below.
   - The governance decision, stated plainly: `governance/**` is governed, and the renovate[bot] version-only exemption
     of `scripts/governance/check-pr.sh` deliberately does not cover the pin (`NEVER_EXEMPT_PATHS`,
     [ADR-0003](adr/0003-governance-gate-trust-boundary.md)): a GSD release replaces hooks that execute in the
     maintainer's Claude Code session, so the pull request needs the `governance-approved` label from a human.
   - Before merging, as a numbered list: read the release notes (in the pull request body, or the changelog
     `/gsd-update` shows); review the hook diff with
     `npm diff --ignore-scripts --diff=@opengsd/gsd-core@<old> --diff=@opengsd/gsd-core@<new> hooks`, which runs no
     package scripts; check out the pull request branch (`gh pr checkout <number>`) and install the release with
     `npx -y @opengsd/gsd-core@<new> --claude --local`, or with `/gsd-update` only when the release it offers is exactly
     `<new>`; run `scripts/doctor.sh` until `gsd` reports ok; add `governance-approved` and merge by hand.
   - After merging: every other checkout's doctor fails with "installed GSD <old> differs from governance/gsd-version
     <new>" and prints the reinstall command as its fix; run it and re-run doctor. An installation ahead of the pin fails
     the same way, so a new release reaches a checkout only through the pin.
   - Security pull requests: Renovate skips the cooldown for a vulnerability fix, but the rule never automerges and the
     label is still required.

4. `docs/adr/0003-governance-gate-trust-boundary.md`: under `### Consequences`, add one bullet recording the author
   decision of 2026-11-24: the renovate[bot] version-only exemption of `scripts/governance/check-pr.sh` deliberately
   excludes `governance/gsd-version` (`NEVER_EXEMPT_PATHS`), because the pin selects the GSD release whose hooks and
   agents run in the maintainer's Claude Code session; a Renovate bump of it always needs `governance-approved` after
   the review in docs/tooling.md (GSD updates), and every other governed file keeps the exemption for version-only
   diffs. Keep the front matter (status, date, decision-makers) and every required section unchanged.

5. Run `mise x -- ./gradlew spotlessApply`, then the verify block, then commit, for example
   `docs(quick-261124-eum): describe the GSD update flow and reinstall the pin from doctor`.
  </action>
  <verify>
    <automated>grep -cE 'gsd-core@1\\+\.23\\+\.0 --claude --local' scripts/test-doctor.sh  # [HEAD=0 EXPECT==1 @d7e456e]</automated>
    <automated>bash scripts/test-doctor.sh | tail -n 1 | awk '{exit !($1 >= 139 && $3 == 0)}'  # [HEAD=139 EXPECT>=139 @d7e456e GUARD MUTATION: any other change to the doctor checks or a dropped assertion]</automated>
    <automated>test "$(grep -c 'run /gsd-update deliberately' scripts/doctor.sh)" -eq 0  # [HEAD=1 EXPECT==0 @d7e456e]</automated>
    <automated>grep -c '^## GSD updates$' docs/tooling.md  # [HEAD=0 EXPECT==1 @d7e456e]</automated>
    <automated>grep -c '(#gsd-updates)' docs/tooling.md  # [HEAD=0 EXPECT>=1 @d7e456e]</automated>
    <automated>grep -c 'npm diff --ignore-scripts' docs/tooling.md  # [HEAD=0 EXPECT>=1 @d7e456e]</automated>
    <automated>grep -c 'NEVER_EXEMPT_PATHS' docs/tooling.md  # [HEAD=0 EXPECT>=1 @d7e456e]</automated>
    <automated>test "$(grep -cE '1\.2[23]\.0' docs/tooling.md)" -eq 0  # [HEAD=0 EXPECT==0 @d7e456e GUARD MUTATION: a version number written into docs/tooling.md]</automated>
    <automated>grep -c 'gsd-version' docs/adr/0003-governance-gate-trust-boundary.md  # [HEAD=0 EXPECT>=1 @d7e456e]</automated>
    <automated>mise x -- ./gradlew checkToolingInventory checkAdrs spotlessCheck</automated>
    <automated>mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh</automated>
  </verify>
  <done>Doctor's version-mismatch fix is the reinstall command for the pin and test-doctor.sh passes; docs/tooling.md has the GSD updates section (flow before and after merge, the deliberate non-exemption, no version numbers) and the GSD row links to it; ADR-0003 records the exclusion; checkToolingInventory, checkAdrs and spotlessCheck are green; the change is committed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| npm registry -> Renovate pull request | A third-party release (`@opengsd/gsd-core`) proposes a new pin through renovate[bot] |
| Pull request -> `main` | The governance gate (check-pr.sh) and the maintainer's merge decide whether the pin moves |
| Pin -> maintainer's Claude Code session | The installed release's hooks and agents execute with the maintainer's permissions |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-eum-01 | Elevation of privilege | GSD release adopted without review | high | mitigate | Package rule `automerge: false` placed after every automerge rule (Task 1); seven-day cooldown restated on the rule so the three-day npm preset of `config:best-practices` cannot shorten it; `governance/gsd-version` in `NEVER_EXEMPT_PATHS` so the pull request needs `governance-approved` (Task 2); documented review of release notes and hook diff before merge (Task 3); doctor's manifest-hash check catches a tampered local installation |
| T-eum-02 | Tampering | renovate[bot] version-only exemption in check-pr.sh | high | mitigate | Exact whole-path comparison against `NEVER_EXEMPT_PATHS` before the exemption, checked for both `filename` and `previous_filename`; tests prove a bot bump of the pin fails without the label, also mixed with an otherwise exempt file (Task 2) |
| T-eum-03 | Spoofing | Regex manager reads the wrong file or package | low | mitigate | `managerFilePatterns` anchored on `^governance/gsd-version$`, fixed `depNameTemplate` and `datasourceTemplate: "npm"`; extraction from the real checkout asserted in Task 1 |
| T-eum-04 | Tampering | Vulnerability-alert pull request skips the cooldown | low | accept | Renovate waives the cooldown for security fixes by design; `vulnerabilityAlerts.automerge` is already false and the label gate still applies, so a human reviews it |
| T-eum-05 | Repudiation | A stale checkout keeps running hooks of an older release | low | mitigate | Doctor fails on any version mismatch and prints the reinstall command for the pin (Task 3) |
| T-eum-06 | Tampering | Gate code changed in the same pull request | medium | accept | Residual already accepted in ADR-0003 (one maintainer; the gate runs from the pull request's tree); the maintainer reads every governed diff, and this plan's edits to scripts/governance/** arrive through the Phase 2 pull request with the label |
| T-eum-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package-manager install happens in this plan (Renovate and npm are already pinned through mise; `npm diff --ignore-scripts` and the Renovate dry runs only read registry metadata); the pin is not bumped, and the later GSD upgrade goes through the reviewed flow documented here |
</threat_model>

<verification>
Run from the checkout root after all three tasks:
- `mise x -- renovate-config-validator --strict renovate.json5` exits 0.
- The local extraction (Task 1 verify) finds `@opengsd/gsd-core` in `governance/gsd-version` with datasource `npm`.
- `bash scripts/governance/test-check-pr.sh` reports at least 136 passed, 0 failed; `bash scripts/test-doctor.sh`
  reports at least 139 passed, 0 failed.
- `mise x -- ./gradlew checkToolingInventory checkAdrs spotlessCheck` succeeds.
- `mise x -- editorconfig-checker` succeeds.
- `governance/gsd-version` is unchanged (`git diff --quiet d7e456e -- governance/gsd-version`).
</verification>

<success_criteria>
- Renovate tracks the GSD pin as `@opengsd/gsd-core` (npm), with the standard cooldown, never automerged, and a pull
  request body that lists the review steps.
- The renovate[bot] version-only exemption deliberately does not cover `governance/gsd-version`; this is enforced in
  check-pr.sh, proven by tests and documented in docs/tooling.md and ADR-0003.
- Doctor tells a stale checkout to reinstall the pinned release.
- docs/tooling.md describes the update flow before and after merge without writing versions.
- The pin is not bumped; all gates named above are green.
</success_criteria>

<output>
Create `.planning/quick/261124-eum-add-a-renovate-custom-manager-that-track/261124-eum-SUMMARY.md` when done. Include the
RED evidence of Task 2 and the lookup record of Task 1 (`newVersion`, `pendingChecks`, `branchName`).
</output>
