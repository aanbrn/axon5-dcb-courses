---
phase: quick-261125-pep
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - .gitignore
  - .claude/skills/dynamic-consistency-boundaries/SKILL.md
  - .claude/skills/dynamic-consistency-boundaries/references/axon-framework-api.md
  - .claude/skills/dynamic-consistency-boundaries/references/axon-server-api.md
  - .claude/skills/dynamic-consistency-boundaries/references/concepts.md
  - .claude/skills/dynamic-consistency-boundaries/references/examples.md
  - .claude/skills/dynamic-consistency-boundaries/README.md
  - REUSE.toml
  - NOTICE
  - scripts/doctor.sh
  - scripts/test-doctor.sh
  - .planning/config.json
  - governance/gsd-config-policy.json
  - docs/adr/0004-gsd-configuration.md
  - .claude/CLAUDE.md
  - docs/tooling.md
  - docs/adr/0014-claude-code-skills-and-mcp-servers.md
autonomous: true
requirements: [AIDEV-01, AIDEV-03, AIDEV-05, CICD-09, DOC-03]

estimate:
  tokens: 95000
  raw_tokens: 95000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "The five upstream files (SKILL.md and references/{axon-framework-api,axon-server-api,concepts,examples}.md) exist under `.claude/skills/dynamic-consistency-boundaries/` byte-identical to AxonIQ/AxonFramework commit f521585912e2915d086158f62e1e6e2c351ae39b: their SHA-256 digests equal the planning-time digests and their git blob ids equal the upstream blob ids; the only other file in the directory is the project-owned README.md, and every file there is Markdown"
    - "The directory is tracked: `.gitignore` carries a narrow exception for exactly this directory, and the `claude-tracking` check of `scripts/doctor.sh` accepts it (60 tracked files under `.claude`) while it still fails for any other tracked skill directory, including the lookalike `dynamic-consistency-boundaries-x`"
    - "REUSE resolves the five copied files to Apache-2.0 with the copyright `2026 AxonIQ B.V. and the Axon Framework contributors` and the README to the project's MIT only; `reuse lint` passes; NOTICE names the copied skill and its licence"
    - "The README beside SKILL.md is the single place of the current pin: it names the source repository and path, the full commit SHA, the commit date, the licence, the author's approval of 2026-11-25, the version caveat (framework main branch versus the pinned 5.4.2; the spike findings skill and the ADRs win) and the verify and update procedure; docs/tooling.md writes no commit SHA"
    - "GSD injects the skill into gsd-planner, gsd-executor, gsd-phase-researcher and gsd-verifier (second entry, after the spike findings skill) and not into gsd-pattern-mapper or gsd-code-reviewer; `.planning/config.json` and `governance/gsd-config-policy.json` agree and checkGsdConfigPolicy passes"
    - "ADR-0004 records the extended mapping and its caveat; ADR-0014 records the vendoring decision (amending AIDEV-01 and AIDEV-03) and why no Renovate manager tracks the pin; docs/tooling.md documents the vendored-skill mode, an inventory row, a survey verdict and the tracked directory without re-padding existing table rows; `.claude/CLAUDE.md` lists the skill"
    - "Every step of the CI lint job, spotlessCheck, checkGsdConfigPolicy, checkToolingInventory, checkAdrs, checkClaudeAskRules and checkNotice pass, and no Kotlin or Gradle script changes"
  artifacts:
    - path: ".claude/skills/dynamic-consistency-boundaries/SKILL.md"
      provides: "Upstream DCB skill entry point, byte-identical to f521585912"
      contains: "name: dynamic-consistency-boundaries"
    - path: ".claude/skills/dynamic-consistency-boundaries/README.md"
      provides: "Provenance record: source, full commit SHA, date, licence, approval, caveat, verify and update steps"
      contains: "f521585912e2915d086158f62e1e6e2c351ae39b"
    - path: "REUSE.toml"
      provides: "Override annotation: Apache-2.0, AxonIQ copyright for SKILL.md and references/**"
      contains: "dynamic-consistency-boundaries"
    - path: "scripts/doctor.sh"
      provides: "claude-tracking allow-list extended by exactly the vendored directory"
      contains: "skills/dynamic-consistency-boundaries/"
    - path: "governance/gsd-config-policy.json"
      provides: "Policy copy of the extended agent_skills mapping"
      contains: ".claude/skills/dynamic-consistency-boundaries"
    - path: "docs/tooling.md"
      provides: "Vendored skills section, inventory row, survey verdict, tracked-files bullet"
      contains: "### Vendored skills"
    - path: "docs/adr/0014-claude-code-skills-and-mcp-servers.md"
      provides: "Vendoring decision amending AIDEV-01 and AIDEV-03"
      contains: "amending AIDEV-01"
  key_links:
    - from: ".planning/config.json"
      to: ".claude/skills/dynamic-consistency-boundaries/SKILL.md"
      via: "agent_skills project path; GSD resolves <path>/SKILL.md inside the project root and skips a missing one"
      pattern: ".claude/skills/dynamic-consistency-boundaries"
    - from: "REUSE.toml"
      to: ".claude/skills/dynamic-consistency-boundaries/SKILL.md"
      via: "[[annotations]] path list with precedence override"
      pattern: "dynamic-consistency-boundaries"
    - from: ".gitignore"
      to: "scripts/doctor.sh"
      via: "the negated directory in .gitignore and the claude-tracking allow-list name the same directory"
      pattern: "dynamic-consistency-boundaries"
    - from: "docs/tooling.md"
      to: ".claude/skills/dynamic-consistency-boundaries/README.md"
      via: "inventory row Version source and the Vendored skills section"
      pattern: "dynamic-consistency-boundaries/README.md"
---

<objective>
Vendor the `dynamic-consistency-boundaries` skill of AxonIQ/AxonFramework as a pinned, byte-identical project skill
and wire it into the repository's tracking, licensing, GSD agent mapping and documentation. The author approved this
skill and this commit on 2026-11-25, which satisfies the AIDEV-01 approval gate for it; the vendoring itself is an
author decision that amends AIDEV-01 (plugins only, nothing copied into `.claude/`) and AIDEV-03 (tracked `.claude/`
files) for a skill that no marketplace publishes, recorded in ADR-0014 the way D-39 amended AIDEV-03.

Purpose: Phase 3 starts designing DCB decisions. The skill gives every planning, executing, researching and verifying
agent the DCB concepts and their mapping onto Axon Framework 5 and the Axon Server DCB API, without an install step.

Output: the five upstream files and a provenance README under `.claude/skills/dynamic-consistency-boundaries/`, the
`.gitignore`, REUSE, NOTICE and doctor changes that accept them, the extended `agent_skills` mapping in the config and
its policy copy, and the documentation in docs/tooling.md, ADR-0004, ADR-0014 and `.claude/CLAUDE.md`.

Planning-time facts (verified on 2026-11-25, do not re-derive unless a gate fails):
- Upstream: https://github.com/AxonIQ/AxonFramework, path `.claude/skills/dynamic-consistency-boundaries`, commit
  f521585912e2915d086158f62e1e6e2c351ae39b, committed 2026-06-16T13:01:25+02:00, subject "Added a DCB skill based on
  AF API, AS API, and theoretical resources available." It is reachable from upstream/main, and the skill tree at the
  pin (82152c8219069976bf2213fca478bbd3cc23f739) equals the one on upstream/main b454038200 of 2026-11-24; the GitHub
  commits API lists the pin as the only commit touching the path on main. The pin is therefore the current content.
- Local clone (read-only, outside this repository by design): /Users/afanasyev/Development/AxonFramework, remote
  `upstream` = https://github.com/AxonIQ/AxonFramework.git. Its working tree is on a feature branch: never copy from it.
- Upstream blob ids at the pin: SKILL.md 9478d3187222be61c2899248646aad7abe82e078, references/axon-framework-api.md
  c3d55f87ad39d52323ce00d346b9e19db7c8cca1, references/axon-server-api.md 613c618454155e6bdc6fdd5b68c3447e70375c88,
  references/concepts.md 6a655c2e10e076d58f22774a50b4180b03695c1a, references/examples.md
  84cf7f616c3575c8b93eb4d283c909b57d63f122. All five are LF-only, end with a newline and carry no licence tag or
  copyright line. Upstream has no NOTICE file; its LICENSE.txt is Apache-2.0, and `LICENSES/Apache-2.0.txt` already
  exists here, so no `reuse download` is needed.
- Formatters: `.claude` is excluded from Spotless (`excludedDirectoryNames` in courses.root.gradle.kts), from
  editorconfig-checker (`.editorconfig-checker.json`) and from the pre-commit format job (`scripts/format-staged.sh`
  skips `.claude/*` except settings.json). The verbatim files therefore pass every formatter unchanged, and no new
  exclusion is needed. Qodana analyses the committed tree, and the tracked spike findings skill, which uses the same
  unpadded Markdown tables, passes it; no Kotlin changes here, so no local Qodana run.
- Renovate decision (record it, do not implement a manager): no Renovate manager tracks the pin, for the reasons given
  in Task 3. The marketplace plugin pins in `.claude/settings.json` have no manager either.
- Governed paths touched (governance/governed-paths.txt): `.planning/config.json`, `governance/gsd-config-policy.json`,
  `REUSE.toml`, `scripts/doctor.sh`. Edit them with the Edit or Write tool, never with shell writes. In the agent
  worktree the edit hook does not ask; the merge hook asks the author once at merge, and the pull request needs the
  `governance-approved` label. Do not push.
</objective>

<execution_context>
@.claude/gsd-core/workflows/execute-plan.md
@.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md

Read only the parts named (line numbers at b06a77a):
- .gitignore 3-10 (the `.claude` tracking rules and the D-39 exception).
- REUSE.toml 1-40 (the TypeID override annotation at 31-40 is the precedent for byte-identical third-party files).
- NOTICE 26-36 (the "Test data" section is the precedent for vendored third-party text).
- scripts/doctor.sh 792-811 (`check_claude_tracking`); scripts/test-doctor.sh 189-235 (helpers), 977-994 (tracked
  `.claude` cases) and the final summary line.
- .planning/config.json and governance/gsd-config-policy.json, key `agent_skills` (policy lines 86-107).
- docs/adr/0004-gsd-configuration.md 1-5 and 96-128 (front matter; the `agent_skills` decision, table and caveats).
- docs/adr/0014-claude-code-skills-and-mcp-servers.md whole file (155 lines; amended in Task 3).
- docs/tooling.md 23-30 and 72-75 (tools table header and the plugin rows), 220-310 (Claude Code skills, review
  criteria, survey, tracked files).
- .claude/CLAUDE.md 276-281 (the GSD-managed Project Skills block).
</context>

<tasks>

<task type="tracer" tdd="true">
  <name>Task 1: Vendor the five files byte for byte and make the repository accept them (tracking, licence, doctor)</name>
  <files>.gitignore, .claude/skills/dynamic-consistency-boundaries/SKILL.md, .claude/skills/dynamic-consistency-boundaries/references/axon-framework-api.md, .claude/skills/dynamic-consistency-boundaries/references/axon-server-api.md, .claude/skills/dynamic-consistency-boundaries/references/concepts.md, .claude/skills/dynamic-consistency-boundaries/references/examples.md, .claude/skills/dynamic-consistency-boundaries/README.md, REUSE.toml, NOTICE, scripts/doctor.sh, scripts/test-doctor.sh</files>
  <read_first>.gitignore, REUSE.toml, NOTICE, scripts/doctor.sh (792-811), scripts/test-doctor.sh (189-235, 977-994), docs/tooling.md (260-273, review criteria)</read_first>
  <behavior>
    - test-doctor: a fixture repo with tracked `.claude/skills/dynamic-consistency-boundaries/SKILL.md`, `references/concepts.md` and `README.md` gives doctor exit 0 and an `ok  claude-tracking:` line (fails before the doctor.sh change).
    - test-doctor: a fixture repo with tracked `.claude/skills/dynamic-consistency-boundaries-x/SKILL.md` gives exit 1, `fail  claude-tracking: .claude/skills/dynamic-consistency-boundaries-x/SKILL.md is tracked` and the fix `git rm --cached .claude/skills/dynamic-consistency-boundaries-x/SKILL.md`.
    - The five vendored files hash to the planning-time SHA-256 digests and blob ids; REUSE reports them as Apache-2.0 with the AxonIQ copyright and the README as MIT with the author's copyright only.
  </behavior>
  <action>
The tracer: one path from the upstream commit to a tracked, correctly licensed skill that every repository gate
accepts. The five upstream files come from one mechanical extraction, so the file count of this task is expected.

1. .gitignore. Directly after the line `!/.claude/skills/spike-findings-*/`, add a comment line
   `# ADR-0014: the vendored dynamic-consistency-boundaries skill of AxonIQ/AxonFramework is a second narrow exception`
   and the negation `!/.claude/skills/dynamic-consistency-boundaries/`. Nothing else in the file changes. Do this
   before the extraction, so git and `reuse lint` see the files.

2. Doctor tests first (RED). In scripts/test-doctor.sh, after the "tracked spike skill" case (ends at line 994), add
   two `fresh` cases in the style of that case, each tracking its files with `git -C "$ENV/repo" add -f`:
   (a) "tracked vendored skill": SKILL.md, references/concepts.md and README.md under
   `.claude/skills/dynamic-consistency-boundaries/`; assert_code 0 and assert_out `^ok  claude-tracking: `.
   (b) "tracked lookalike skill": `.claude/skills/dynamic-consistency-boundaries-x/SKILL.md`; assert_code 1, assert_out
   `^fail  claude-tracking: .claude/skills/dynamic-consistency-boundaries-x/SKILL\.md is tracked` and assert_out
   `^      fix: git rm --cached .claude/skills/dynamic-consistency-boundaries-x/SKILL\.md` (escape regex dots as the
   existing cases do). That is five new assertions: the suite goes from 365 to 370 passed. Run
   `bash scripts/test-doctor.sh` and confirm case (a) fails before step 3.

3. scripts/doctor.sh (governed, Edit tool), `check_claude_tracking` (GREEN):
   - The grep -vE allow-list becomes `'^$|^\.claude/(settings\.json|CLAUDE\.md|skills/spike-findings-[^/]+/.*|skills/dynamic-consistency-boundaries/.*)$'`:
     the exact directory name followed by a slash, so a lookalike directory stays rejected.
   - The fail message becomes `"$path is tracked, but only settings.json, CLAUDE.md, skills/spike-findings-* and skills/dynamic-consistency-boundaries may be (AIDEV-03)"`.
   - The comment above the function names the vendored dynamic-consistency-boundaries skill (ADR-0014) next to the
     spike-findings skills (D-39).
   Re-run `bash scripts/test-doctor.sh`: 370 passed, 0 failed.

4. Extract the upstream files. Each git command runs on its own (the worktree sandbox refuses git mixed with loops,
   chains or redirects). Create a temporary directory outside the repository with `mktemp -d`, then run
   `git -C /Users/afanasyev/Development/AxonFramework archive --format=tar --output=<tmp>/dcb.tar f521585912e2915d086158f62e1e6e2c351ae39b .claude/skills/dynamic-consistency-boundaries`
   and, from the checkout root, `tar -xf <tmp>/dcb.tar`. Planning proved that this yields the five files byte for
   byte. Never copy from the clone's working tree, and never open and re-save the five files with an editor, the
   Write tool or a formatter. Fallback only if the clone cannot be read: download each file with
   `gh api -H 'Accept: application/vnd.github.raw' 'repos/AxonIQ/AxonFramework/contents/.claude/skills/dynamic-consistency-boundaries/<file>?ref=f521585912e2915d086158f62e1e6e2c351ae39b'`
   into its destination. The SHA-256 gate decides either way.

5. `.claude/skills/dynamic-consistency-boundaries/README.md` (Write tool; project-owned, MIT through the aggregate
   annotation). Write it in the house Markdown style even though `.claude` is not formatted: `-` bullets, lines of at
   most 120 columns, fenced blocks tagged `shell`, repository paths in backticks rather than relative links. Content:
   - Title "Vendored skill: dynamic-consistency-boundaries". Opening paragraph: SKILL.md and references/ are a byte
     for byte copy of the upstream skill and are never edited here; this README is the project's provenance record
     and not part of the upstream skill; Claude Code loads the skill as a project skill, and GSD maps it to agents
     through `agent_skills` (ADR-0004).
   - "Source": repository https://github.com/AxonIQ/AxonFramework; path `.claude/skills/dynamic-consistency-boundaries`;
     commit `f521585912e2915d086158f62e1e6e2c351ae39b` of 2026-06-16; licence Apache-2.0 (the repository's
     LICENSE.txt; the text is in `LICENSES/Apache-2.0.txt`, and `REUSE.toml` and `NOTICE` record it); approved by the
     repository author on 2026-11-25 against the review criteria in `docs/tooling.md` (section Claude Code skills).
   - "Why vendored": AxonIQ does not publish the skill in its marketplace `AxonIQ/agent-skills`. Once a marketplace
     publishes it, a plugin pinned by commit replaces this directory (`docs/tooling.md`, Vendored skills).
   - "Version caveat": the skill is written against the framework's main branch (5.5.0-SNAPSHOT at the pinned
     commit), while the project pins Axon Framework 5.4.2 in `gradle/libs.versions.toml`. Where it disagrees with the
     `spike-findings-axon5-dcb-courses` skill or an ADR, those win. Paths such as `examples/university-java/` refer to
     the Axon Framework repository.
   - "Verify": the blob ids that `git ls-files -s -- .claude/skills/dynamic-consistency-boundaries` prints for every
     file but this README equal those of
     `gh api 'repos/AxonIQ/AxonFramework/git/trees/<pinned commit>:.claude/skills/dynamic-consistency-boundaries?recursive=1' --jq '.tree[] | select(.type == "blob") | [.sha, .path] | @tsv'`
     (write the full commit in place of the placeholder).
   - "Update", numbered: (1) find the newest upstream commit of the skill with
     `gh api 'repos/AxonIQ/AxonFramework/commits?path=.claude/skills/dynamic-consistency-boundaries&sha=main&per_page=1' --jq '.[0].sha'`;
     the pin means nothing to do. (2) Read the change at `https://github.com/AxonIQ/AxonFramework/compare/<pin>...<new>`
     against the review criteria (Markdown only; no hooks, scripts, MCP or LSP configuration) and get the author's
     approval of the new commit in the pull request. (3) Replace SKILL.md and references/ with the files of the new
     commit byte for byte, for example `git archive` of that commit from a clone of AxonIQ/AxonFramework extracted at
     the repository root, and delete files upstream removed. (4) Update the commit and date in this README; if
     upstream added files outside references/, extend the annotation in `REUSE.toml` and the entry in `NOTICE`.
     (5) Run the verification above, `reuse lint` and `bash scripts/test-doctor.sh`. When to check: whenever Renovate
     proposes an Axon Framework update and before planning a phase that designs DCB decisions. Why there is no
     Renovate manager: one sentence pointing at `docs/tooling.md`, Vendored skills.
   - It must hold no licence tag line and no copyright notice line: REUSE would read either as this file's own
     licensing data (the REUSE gate below checks the README resolves to the aggregate MIT annotation only).

6. REUSE.toml (governed, Edit tool). After the TypeID annotation, add a comment in the same style (the
   dynamic-consistency-boundaries agent skill is copied byte for byte from AxonIQ/AxonFramework, its README.md names
   the pinned commit and the update procedure, it stays under the upstream licence and cannot carry a header without
   ceasing to be a byte-identical copy; the README itself is the project's own file) and an `[[annotations]]` table:
   path list `.claude/skills/dynamic-consistency-boundaries/SKILL.md` and
   `.claude/skills/dynamic-consistency-boundaries/references/**`, precedence `override`, SPDX-FileCopyrightText
   `2026 AxonIQ B.V. and the Axon Framework contributors`, SPDX-License-Identifier `Apache-2.0`. Run
   `mise x -- ./gradlew spotlessApply --console=plain` afterwards (taplo may break the path array over lines).

7. NOTICE (Edit tool). After the "Test data" section and before "Axoniq Framework terms", add a section titled
   "Agent skill" in the same layout (title line, blank line, paragraph indented by two spaces, at most 120 columns):
   the Claude Code skill in .claude/skills/dynamic-consistency-boundaries/ (SKILL.md and references/) is copied byte
   for byte from https://github.com/AxonIQ/AxonFramework (.claude/skills/dynamic-consistency-boundaries) and stays
   under the Apache-2.0 licence of that project; the README.md beside it names the pinned commit and REUSE.toml
   records the licence. Write no copyright notice line (REUSE would attach it to NOTICE). checkNotice reads only the
   licence-policy exceptions, so this section is attribution, not a policy entry.

8. Stage the new and changed files, then run the verify gates.
  </action>
  <verify>
    <automated>printf '%s  %s\n' 31f318c5a7aa93f7f124c1fc7fae97831c859d18148ec0f4775090fb1bbe4434 .claude/skills/dynamic-consistency-boundaries/SKILL.md b87a8a5e38771cde245a8498a42f9cc2531cc550994a3af0866cde731c0cddd8 .claude/skills/dynamic-consistency-boundaries/references/axon-framework-api.md f11044941cf35d505c5c7146b272f3fe56b70d71ae3d8e0fde32635aef6c895c .claude/skills/dynamic-consistency-boundaries/references/axon-server-api.md d0f11790d8363979dca94df4797f7fa985736d5e4538d5dc813ee01bf76ea237 .claude/skills/dynamic-consistency-boundaries/references/concepts.md f504fab28fbce4421abe8edeb950ac6fc8a9e0ceaa57adbb441cb5a67278311f .claude/skills/dynamic-consistency-boundaries/references/examples.md | shasum -a 256 -c - 2>/dev/null | grep -c ': OK$'  # [HEAD=0 EXPECT==5 @b06a77a]</automated>
    <automated>git check-ignore -q .claude/skills/dynamic-consistency-boundaries/SKILL.md; echo "$?"  # prints 0 while ignored, 1 once tracked [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>git ls-files -s -- .claude/skills/dynamic-consistency-boundaries | grep -cE ' (9478d3187222be61c2899248646aad7abe82e078|c3d55f87ad39d52323ce00d346b9e19db7c8cca1|613c618454155e6bdc6fdd5b68c3447e70375c88|6a655c2e10e076d58f22774a50b4180b03695c1a|84cf7f616c3575c8b93eb4d283c909b57d63f122) '  # [HEAD=0 EXPECT==5 @b06a77a]</automated>
    <automated>test "$(git ls-files -- .claude/skills/dynamic-consistency-boundaries | wc -l | tr -d ' ')" -eq 6  # [HEAD=0 EXPECT==6 @b06a77a]</automated>
    <automated>test "$(find .claude/skills/dynamic-consistency-boundaries -type f ! -name '*.md' | wc -l | tr -d ' ')" -eq 0  # [HEAD=0 EXPECT==0 @b06a77a GUARD MUTATION: a non-Markdown file (script, hook, configuration) in the vendored directory]</automated>
    <automated>bash scripts/test-doctor.sh | tail -n 1 | awk '{exit !($1 >= 370 && $3 == 0)}'  # [HEAD=365 EXPECT>=370 @b06a77a]</automated>
    <automated>bash scripts/doctor.sh 2>&1 | grep -c '^ok  claude-tracking: only project-owned files are tracked under .claude (60 files)$'  # 54 files at HEAD; counts staged or committed files [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>mise x -- reuse lint</automated>
    <automated>mise x -- reuse spdx | grep -A6 '^FileName: ./.claude/skills/dynamic-consistency-boundaries/SKILL.md$' | grep -cx -e 'LicenseInfoInFile: Apache-2.0' -e 'FileCopyrightText: <text>SPDX-FileCopyrightText: 2026 AxonIQ B.V. and the Axon Framework contributors</text>'  # [HEAD=0 EXPECT==2 @b06a77a]</automated>
    <automated>mise x -- reuse spdx | grep -A6 '^FileName: ./.claude/skills/dynamic-consistency-boundaries/references/examples.md$' | grep -cx 'LicenseInfoInFile: Apache-2.0'  # [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>mise x -- reuse spdx | grep -A6 '^FileName: ./.claude/skills/dynamic-consistency-boundaries/README.md$' | grep -cx -e 'LicenseInfoInFile: MIT' -e 'FileCopyrightText: <text>SPDX-FileCopyrightText: 2026 Alexey Afanasyev</text>'  # [HEAD=0 EXPECT==2 @b06a77a]</automated>
    <automated>grep -c '^Agent skill$' NOTICE  # [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh</automated>
    <automated>mise x -- ./gradlew spotlessCheck checkNotice --console=plain</automated>
  </verify>
  <done>The five upstream files are tracked byte-identical to f521585912 (digests and blob ids match), the README
  records the provenance and procedures, REUSE reports Apache-2.0 with the AxonIQ copyright for the copies and MIT for
  the README, NOTICE carries the Agent skill section, doctor accepts the directory (60 tracked files) and rejects a
  lookalike, test-doctor passes 370 assertions, and the task is committed.</done>
</task>

<task type="auto">
  <name>Task 2: Map the skill to the DCB-facing GSD agents and record the mapping (config, policy, ADR-0004, CLAUDE.md)</name>
  <files>.planning/config.json, governance/gsd-config-policy.json, docs/adr/0004-gsd-configuration.md, .claude/CLAUDE.md</files>
  <read_first>.planning/config.json (agent_skills), governance/gsd-config-policy.json (86-107), docs/adr/0004-gsd-configuration.md (1-5, 96-128), .claude/CLAUDE.md (276-281)</read_first>
  <action>
1. `.planning/config.json` and `governance/gsd-config-policy.json` (both governed; Edit tool; never `config-set`,
   which an ask rule guards). In `agent_skills`, insert `.claude/skills/dynamic-consistency-boundaries` as the second
   entry, directly after `.claude/skills/spike-findings-axon5-dcb-courses` and before any `global:` entry, in exactly
   these four lists: `gsd-planner`, `gsd-executor`, `gsd-phase-researcher`, `gsd-verifier`. These are the agents that
   design, implement, research and verify DCB decisions (the author's mapping for this task). `gsd-pattern-mapper` and
   `gsd-code-reviewer` stay unchanged. Change no other key and leave no transient key that starts with an underscore.
   Keep the expanded style of `.planning/config.json` (Spotless does not format `.planning`); for the policy run
   `mise x -- ./gradlew spotlessApply --console=plain`, which lets Prettier collapse a short array onto one line.

2. Prove that GSD resolves the path: through the GSD launcher described in `.claude/gsd-core/references/gsd-run-resolver.md`
   (in an agent worktree it finds gsd-tools in the main checkout), run `gsd_run query agent-skills <agent> --skills-only`
   for the four agents. Each output lists `@.claude/skills/dynamic-consistency-boundaries/SKILL.md` and prints no
   "Skill not found" warning; `gsd-code-reviewer` does not list it. Record the four outputs in the summary.

3. docs/adr/0004-gsd-configuration.md (follow its own rule: the change to `agent_skills` is recorded here in the same
   pull request):
   - Front matter `date` becomes "2026-11-25".
   - In "Decision: GSD agents load skills through `agent_skills`": extend the approval sentence by the author adding
     the DCB skill on 2026-11-25 for the planner, the executor, the phase researcher and the verifier. Update the
     table rows of those four agents to name the DCB skill after "spike findings", and adjust their reasons (the
     planner and the executor also follow the DCB concepts and the Axon 5 and Axon Server DCB API map; research starts
     from verified facts, DCB theory and Axon 5 guidance; the verifier also checks DCB semantics, for example that a
     consistency boundary is as narrow as the invariant allows). Prettier re-pads this small table; that is expected.
   - In the paragraph naming the skill sources, add: the DCB skill is `dynamic-consistency-boundaries`, which AxonIQ
     keeps in the Axon Framework repository but publishes in no marketplace, so it is vendored at commit `f521585`
     under `.claude/skills/` ([ADR-0014](0014-claude-code-skills-and-mcp-servers.md); the full SHA lives in the
     skill's README). Add why the pattern mapper and the code reviewer do not get it: they work from the patterns and
     constraints the project has proven, which the spike findings skill carries, while the DCB skill explains and
     designs consistency boundaries (its own scope statement).
   - Caveats: the project-skill bullet says the project skill and the vendored DCB skill are tracked and always
     resolve; add a bullet that `dynamic-consistency-boundaries` targets the framework's main branch (5.5.0-SNAPSHOT
     at the pinned commit) while the project pins 5.4.2, and that the spike findings skill and the ADRs win; the trust
     boundary bullet names the vendored DCB skill next to the project skill and the approved plugins, and says a
     change of the vendored text is a reviewed diff of a byte-identical copy of a pinned upstream commit (ADR-0014).
   - Keep every heading and section that checkAdrs requires.

4. `.claude/CLAUDE.md`: inside the GSD-managed block between `<!-- GSD:skills-start source:skills/ -->` and
   `<!-- GSD:skills-end -->`, add a bullet after the spike findings bullet, built exactly like it: the bold name
   "Dynamic Consistency Boundaries", then in parentheses "DCB concepts and their mapping onto Axon Framework 5 and the
   Axon Server DCB API; vendored from AxonIQ/AxonFramework, provenance in its README.md", then the arrow and the code
   span Skill("dynamic-consistency-boundaries") in backticks. A later GSD regeneration of the block scans
   `.claude/skills/` and keeps the skill listed.
  </action>
  <verify>
    <automated>test "$(jq '[.agent_skills["gsd-planner","gsd-executor","gsd-phase-researcher","gsd-verifier"][1] | select(. == ".claude/skills/dynamic-consistency-boundaries")] | length' .planning/config.json)" -eq 4  # [HEAD=0 EXPECT==4 @b06a77a]</automated>
    <automated>test "$(jq '[.agent_skills["gsd-planner","gsd-executor","gsd-phase-researcher","gsd-verifier"][1] | select(. == ".claude/skills/dynamic-consistency-boundaries")] | length' governance/gsd-config-policy.json)" -eq 4  # [HEAD=0 EXPECT==4 @b06a77a]</automated>
    <automated>test "$(jq '[.agent_skills["gsd-pattern-mapper","gsd-code-reviewer"][] | select(. == ".claude/skills/dynamic-consistency-boundaries")] | length' .planning/config.json)" -eq 0  # [HEAD=0 EXPECT==0 @b06a77a GUARD MUTATION: mapping the skill to the pattern mapper or the code reviewer]</automated>
    <automated>grep -c '^date: "2026-11-25"$' docs/adr/0004-gsd-configuration.md  # [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>grep -c 'dynamic-consistency-boundaries' docs/adr/0004-gsd-configuration.md  # [HEAD=0 EXPECT>=2 @b06a77a]</automated>
    <automated>grep -c 'Skill("dynamic-consistency-boundaries")' .claude/CLAUDE.md  # [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>mise x -- ./gradlew checkGsdConfigPolicy checkAdrs spotlessCheck --console=plain</automated>
  </verify>
  <done>Config and policy carry the skill as the second entry for the four agents and agree (checkGsdConfigPolicy
  green), GSD resolves it for those agents only, ADR-0004 records the extended mapping with its caveat and date, and
  `.claude/CLAUDE.md` lists the skill; the task is committed.</done>
</task>

<task type="auto">
  <name>Task 3: Document the vendored-skill mode (docs/tooling.md, ADR-0014) and replay every CI lint step</name>
  <files>docs/tooling.md, docs/adr/0014-claude-code-skills-and-mcp-servers.md</files>
  <read_first>docs/tooling.md (1-30, 72-75, 220-310), docs/adr/0014-claude-code-skills-and-mcp-servers.md, .github/workflows/ci.yml (295-336, the lint job)</read_first>
  <action>
docs/tooling.md never writes a version number or a commit SHA ("Versions are never written in this document"); the
README of the skill is the single place of the pin. Keep every new table cell within the current column widths, so
Prettier does not re-pad existing rows: tools table 61 / 103 / 42 / 186 / 19 characters, survey table 14 / 89 / 315 /
75 / 6. The cells below fit; measure any rewording before writing it.

1. Tools table: after the "Gradle skills" row add a row whose cells are the following texts (each cell text runs
   from after the colon to the end of its line; backticks inside are part of the cell):
   - Tool: DCB skill (`dynamic-consistency-boundaries`)
   - Purpose: DCB concepts and their mapping onto Axon Framework 5 and the Axon Server DCB API, for Claude Code
   - Version source: `dynamic-consistency-boundaries/README.md`
   - Installation: Vendored byte for byte in `.claude/skills/dynamic-consistency-boundaries/` and tracked in git; nothing to install
   - Used in: local (Claude Code)
   checkToolingInventory checks only rows whose Version source is `mise`, so this row is outside it.

2. "How skills are installed": the first bullet opens with "A third-party skill is installed as a marketplace
   plugin." (instead of "only as") and ends with the sentence "A vetted skill that no marketplace publishes is
   vendored instead (see [Vendored skills](#vendored-skills))." Rewrite the bullet at line 250, which says that a
   plugin lives in Claude Code's own plugin cache, to read "A plugin is never copied into `.claude/`; it lives in
   Claude Code's own plugin cache."

3. New subsection `### Vendored skills` between "How skills are installed" and "Review criteria", as bullets:
   - When: a skill that passed the review criteria and that the author approved, but that no marketplace publishes.
     Its files are copied byte for byte from a full 40-character commit SHA into `.claude/skills/<upstream name>/` and
     are never edited. Today one skill: `dynamic-consistency-boundaries` from `AxonIQ/AxonFramework`.
   - Provenance: a project-owned `README.md` beside the copied files records the source, the pinned commit, the
     licence, the approval and the verify and update steps; it is the only place the commit is written.
   - Tracking and licence: a narrow `.gitignore` exception tracks the directory and the `claude-tracking` check of
     `scripts/doctor.sh` mirrors it; `REUSE.toml` annotates the copied files with their upstream licence and `NOTICE`
     names them; `.claude/` is outside Spotless and editorconfig-checker, so no formatter rewrites the upstream text.
   - Loading: Claude Code loads it as a project skill in every session, and `agent_skills` maps it to GSD agents
     ([ADR-0004](adr/0004-gsd-configuration.md)); there is nothing to install.
   - Updates, starting with the exact sentence "No Renovate manager tracks the vendored skill." Then why: a custom
     manager could change only the commit in the README, because copying the new files needs post-upgrade commands,
     which Renovate runs only where its operator allows them, so its pull request would leave the pin and the content
     disagreeing; a tracker of the framework's main branch would also propose nearly every framework commit, although
     the skill has changed in one commit so far; the marketplace plugin pins are updated by hand for the same reason of
     review. The README's procedure applies instead: check whenever Renovate proposes an Axon Framework update and
     before planning a phase that designs DCB decisions, review the upstream diff against the criteria, re-copy, and
     record the author's approval in the pull request.
   - Precedence and exit: the skill targets the framework's main branch, ahead of the release the project pins, so
     where it disagrees with the spike findings skill or an ADR, those win; once a marketplace publishes it, a plugin
     pinned by commit replaces the copy and the exception is removed. Link [ADR-0014](adr/0014-claude-code-skills-and-mcp-servers.md).

4. Survey: in the paragraph above the table add that a skill whose verdict reads "approved (vendored ...)" is copied
   at a pinned commit as [Vendored skills](#vendored-skills) describes and needs no install. Add a row after the
   `axoniq-migration` row with these cell texts (same convention as step 1):
   - Area: Axon
   - Candidate: `AxonIQ/AxonFramework`, skill `dynamic-consistency-boundaries`
   - Evidence: Official AxonIQ repository, Apache-2.0, added by an AxonIQ engineer. `SKILL.md` and four reference files, Markdown only: no hooks, scripts, MCP or LSP configuration. Explains DCB and maps it onto Axon Framework 5 and the Axon Server DCB API. Written for the framework's main branch; no marketplace publishes it.
   - Verdict: Approved (vendored at a pinned commit; tracked, nothing to install)
   - Phase: 3 to 4

5. "Tracked Claude Code files": after the spike-findings bullet add a bullet for
   `.claude/skills/dynamic-consistency-boundaries/`, the vendored DCB skill of [Vendored skills](#vendored-skills), a
   second narrow exception.

6. docs/adr/0014-claude-code-skills-and-mcp-servers.md:
   - Front matter `date` becomes "2026-11-25".
   - Considered Options: add a bullet: vendor a skill that no marketplace publishes as a byte-identical copy in
     `.claude/skills/` at a pinned commit (chosen on 2026-11-25 for `dynamic-consistency-boundaries`), versus waiting
     until AxonIQ publishes it, versus moving it into the repository-local plugin of Phase 8 (AIDEV-02).
   - "Tracked files (D-39, amending AIDEV-03)": the ignore rule now also exempts
     `.claude/skills/dynamic-consistency-boundaries/` (see the new section); the spike-findings skill is no longer "the
     one" exception but one of two narrow exceptions.
   - New section after "Skills (D-40, AIDEV-01)" and before "MCP servers": `### Vendored skill (author decision of 2026-11-25, amending AIDEV-01 and AIDEV-03)`
     with bullets: What (the DCB skill of AxonIQ/AxonFramework, added upstream on 2026-06-16, copied byte for byte at
     commit `f521585`, full SHA in its README; approved by the author on 2026-11-25 against the review criteria:
     official AxonIQ repository, Apache-2.0, Markdown only); Why (no marketplace publishes it, so the plugin rule of
     D-40 cannot admit it; Phase 3 designs DCB decisions now; Claude Code loads project skills only from
     `.claude/skills/`; the repository-local plugin of AIDEV-02 is for the project's own skills and arrives in Phase 8);
     Amendment (AIDEV-01 says third-party skills are installed as marketplace plugins with nothing copied into
     `.claude/`, and AIDEV-03 limits the tracked `.claude/` files; both now allow this one vendored skill);
     Controls (never edited and outside the formatters; the README is the one place of the pin and holds the verify
     and update steps; the `.gitignore` exception and the doctor `claude-tracking` allow-list name exactly this
     directory; REUSE.toml resolves the copies to Apache-2.0 with the AxonIQ copyright and NOTICE names them; the
     `agent_skills` mapping is pinned by the whole-file policy of ADR-0004); Updates (by hand, opening with the exact
     sentence "No Renovate manager tracks the vendored skill." and the reasons of step 3); Caveat (main branch,
     5.5.0-SNAPSHOT at the pin, versus the pinned 5.4.2; the spike findings skill and the ADRs win); Exit (once a
     marketplace publishes it, a pinned plugin replaces the copy and both exceptions go).
   - Consequences: add a Good bullet (the DCB concepts and API map reach every agent and session with no install
     step) and a Bad bullet (the copy can fall behind upstream unnoticed between manual checks). In the Bad bullet
     about the tracked exception for the spike-findings skill (line 100), say the rule now has two holes, the
     spike-findings skill and the vendored skill.
   - Confirmation: add that `reuse lint` checks the licence of the vendored files and that doctor's tracked-file
     check names the vendored directory.
   - Pros and Cons: add `### Vendoring a skill that no marketplace publishes` (Good: the skill is available now,
     pinned and reviewable, with provenance beside it; Bad: a manual update, a second exception, and an upstream
     written for an unreleased framework version). Under "Marketplace plugins pinned by SHA", extend the sentence
     about copying skills into `.claude/`: the one vendored skill is the exception for a skill no marketplace
     publishes, and it keeps its provenance in a README beside it.
   - More Information: add the author decision of 2026-11-25 to the decisions line, and a revisit trigger: when
     AxonIQ publishes the skill in a marketplace, replace the copy with a pinned plugin.
   - Keep every section checkAdrs requires.

7. Run `mise x -- ./gradlew spotlessApply --console=plain`, then every gate below. The lint gates replay the CI
   lint job of `.github/workflows/ci.yml` step by step (zizmor offline, because no job token exists locally). If the
   worktree sandbox refuses a compound command, run its parts one by one. No Kotlin or Gradle script may change, so
   no local Qodana run is needed.
  </action>
  <verify>
    <automated>grep -c '^### Vendored skills$' docs/tooling.md  # [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>grep -c '(#vendored-skills)' docs/tooling.md  # [HEAD=0 EXPECT>=3 @b06a77a]</automated>
    <automated>grep -c 'dynamic-consistency-boundaries' docs/tooling.md  # [HEAD=0 EXPECT>=4 @b06a77a]</automated>
    <automated>grep -c 'A plugin is never copied into' docs/tooling.md  # [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>grep -c 'No Renovate manager tracks the vendored skill.' docs/tooling.md  # [HEAD=0 EXPECT>=1 @b06a77a]</automated>
    <automated>grep -c 'No Renovate manager tracks the vendored skill.' docs/adr/0014-claude-code-skills-and-mcp-servers.md  # [HEAD=0 EXPECT>=1 @b06a77a]</automated>
    <automated>test "$(grep -cE '[0-9a-f]{40}' docs/tooling.md)" -eq 0  # [HEAD=0 EXPECT==0 @b06a77a GUARD MUTATION: a commit SHA written into the inventory page]</automated>
    <automated>git cat-file -e 'b06a77a^{commit}' && test "$(git diff -U0 b06a77a -- docs/tooling.md | grep -c '^-| ')" -eq 0  # [HEAD=0 EXPECT==0 @b06a77a GUARD MUTATION: a new cell wider than its column re-pads existing table rows]</automated>
    <automated>grep -c '^date: "2026-11-25"$' docs/adr/0014-claude-code-skills-and-mcp-servers.md  # [HEAD=0 EXPECT==1 @b06a77a]</automated>
    <automated>grep -c 'amending AIDEV-01 and AIDEV-03' docs/adr/0014-claude-code-skills-and-mcp-servers.md  # [HEAD=0 EXPECT>=1 @b06a77a]</automated>
    <automated>test "$(grep -c 'one hole' docs/adr/0014-claude-code-skills-and-mcp-servers.md)" -eq 0  # [HEAD=1 EXPECT==0 @b06a77a]</automated>
    <automated>git cat-file -e 'b06a77a^{commit}' && test "$(git diff --name-only b06a77a -- '*.kt' '*.kts' | wc -l | tr -d ' ')" -eq 0  # [HEAD=0 EXPECT==0 @b06a77a GUARD MUTATION: any Kotlin or Gradle script change, which would require a local Qodana run]</automated>
    <automated>mise x -- actionlint</automated>
    <automated>mise x -- zizmor --offline --no-progress .github/workflows .github/actions</automated>
    <automated>git ls-files -z '*.sh' | xargs -0 -r mise x -- shellcheck</automated>
    <automated>mise x -- editorconfig-checker</automated>
    <automated>mise x -- reuse lint</automated>
    <automated>temp_index="$(mktemp)"; cp "$(git rev-parse --git-path index)" "$temp_index"; GIT_INDEX_FILE="$temp_index" git add --renormalize .; GIT_INDEX_FILE="$temp_index" git diff --cached --exit-code --stat; status=$?; rm -f "$temp_index"; test "$status" -eq 0</automated>
    <automated>mise x -- renovate-config-validator --strict renovate.json5</automated>
    <automated>docker compose config --quiet</automated>
    <automated>git ls-files -z -- 'scripts/test-*.sh' 'scripts/*/test-*.sh' | xargs -0 -r -n1 bash</automated>
    <automated>mise x -- ./gradlew spotlessCheck checkGsdConfigPolicy checkToolingInventory checkAdrs checkClaudeAskRules checkNotice --console=plain</automated>
    <automated>for f in SKILL.md references/axon-framework-api.md references/axon-server-api.md references/concepts.md references/examples.md; do git -C /Users/afanasyev/Development/AxonFramework show "f521585912e2915d086158f62e1e6e2c351ae39b:.claude/skills/dynamic-consistency-boundaries/$f" | cmp - ".claude/skills/dynamic-consistency-boundaries/$f" || exit 1; done  # byte identity against the pinned upstream blobs; if the sandbox refuses the loop, run the five show-and-cmp pairs one by one</automated>
  </verify>
  <done>docs/tooling.md documents the vendored-skill mode, its inventory row, survey verdict and tracked directory
  without re-padding existing rows or writing a SHA; ADR-0014 records the decision amending AIDEV-01 and AIDEV-03 with
  the Renovate reasoning, controls, caveat and exit; every CI lint step, the shell tests, spotlessCheck and the
  governance checks pass; the five files are byte-identical to the pinned upstream blobs; the task is committed and
  nothing is pushed.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| upstream repository to vendored copy | third-party agent instructions enter the repository at a pinned commit |
| vendored skill to agent context | the skill text enters four GSD agent spawns and every Claude Code session in this repository |
| agent worktree to main checkout | governed edits (config, policy, REUSE.toml, doctor.sh) reach the main checkout only through the merge the merge hook asks about (ADR-0015) |
| tracked files to CI gates | doctor's claude-tracking check and `reuse lint` decide which `.claude/` files and which licences the repository accepts |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-pep-01 | Tampering | the five vendored files | high | mitigate | Extracted with `git archive` from the pinned full SHA of the AxonIQ clone, never from its working tree; the SHA-256 gate compares against digests taken from `git show` at planning time, the blob-id gate against the upstream blob ids, and Task 3 re-runs a `git show` and `cmp` comparison; `.claude/` is outside every formatter, so no tool rewrites the text; the README's verify step lets anyone repeat the comparison through the GitHub API |
| T-pep-02 | Spoofing | upstream source | medium | mitigate | At planning the clone's `upstream` remote is AxonIQ/AxonFramework, the commit is reachable from upstream/main, and GitHub's trees API for AxonIQ/AxonFramework at the commit returned the same five blob ids |
| T-pep-03 | Elevation of privilege | skill text in agent context (prompt steering) | medium | mitigate | Markdown-only content (gate: no non-Markdown file), reviewed and approved by the author at this exact commit, mapped to four agents only, and both the README and ADR-0004/0014 state that the spike findings skill and the ADRs win; the maintainer reviews every pull request diff (ADR-0003). Residual risk of subtly wrong guidance is accepted, because executors prove results by running the gates |
| T-pep-04 | Tampering | doctor claude-tracking allow-list | medium | mitigate | The allow-list names the exact directory followed by a slash; a test-doctor case proves a lookalike directory still fails; `.gitignore` negates the same single directory |
| T-pep-05 | Repudiation | licence attribution of the copies | medium | mitigate | REUSE.toml override annotation (Apache-2.0, AxonIQ copyright) checked through `reuse spdx` gates, the README checked to resolve to MIT only, and a NOTICE section naming the copied skill and its licence |
| T-pep-06 | Tampering | governed files `.planning/config.json`, `governance/gsd-config-policy.json`, `REUSE.toml`, `scripts/doctor.sh` | medium | mitigate | Edited with file tools in the agent worktree; the merge hook asks the author before they reach the main checkout; the pull request needs `governance-approved`; checkGsdConfigPolicy and checkClaudeAskRules run |
| T-pep-07 | Information disclosure | GitHub API calls of the README procedure | low | accept | They read public metadata of a public repository and send no project data |
| T-pep-SC | Tampering | npm/pip/cargo installs | low | accept | This plan installs no package; the vendored content is plain Markdown verified by digest and blob id |
</threat_model>

<verification>
- Byte identity: the SHA-256 gate (Task 1), the blob-id gate (Task 1) and the `git show` plus `cmp` loop (Task 3)
  agree for all five files.
- Tracking: `.gitignore` no longer ignores the directory, doctor reports `ok  claude-tracking` with 60 files, and
  test-doctor passes 370 assertions including the lookalike rejection.
- Licensing: `reuse lint` passes; `reuse spdx` shows Apache-2.0 with the AxonIQ copyright for the copies and MIT with
  the author's copyright only for the README; NOTICE has the Agent skill section.
- Wiring: config and policy agree; GSD lists the skill for gsd-planner, gsd-executor, gsd-phase-researcher and
  gsd-verifier, and not for gsd-pattern-mapper or gsd-code-reviewer.
- Documentation: docs/tooling.md, ADR-0004, ADR-0014 and `.claude/CLAUDE.md` describe the skill, the mode, the
  amendment of AIDEV-01 and AIDEV-03 and the update procedure; checkAdrs and checkToolingInventory pass.
- CI parity: actionlint, zizmor, shellcheck, editorconfig-checker, reuse lint, the normalisation check, the Renovate
  validator, `docker compose config`, the shell tests and the Gradle checks all pass locally. No `.kt` or `.kts` file
  changed.
</verification>

<success_criteria>
- `.claude/skills/dynamic-consistency-boundaries/` holds exactly six tracked Markdown files: the five byte-identical
  upstream files of f521585912e2915d086158f62e1e6e2c351ae39b and the project's README with the full provenance.
- Every gate of the three tasks passes, every task is committed on the executor's branch, and nothing is pushed.
- The summary lists the governed files changed (for the merge prompt and the `governance-approved` label), the
  Renovate decision with its reasons, and the four `gsd_run query agent-skills` outputs.
</success_criteria>

<output>
Create `.planning/quick/261125-pep-vendor-the-axonframework-dynamic-consist/261125-pep-SUMMARY.md` when done
</output>
