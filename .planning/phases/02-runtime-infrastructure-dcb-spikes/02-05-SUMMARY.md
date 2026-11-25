---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 05
subsystem: tooling
tags: [claude-code, skills, plugins, marketplace, axon, gradle, supply-chain]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "Plan 02-02: shared MCP servers, the MCP section and inventory layout of docs/tooling.md"
provides:
  - "A skills survey in docs/tooling.md: install rules, review criteria and a verdict per candidate for all nine AIDEV-01 areas, in AIDEV-01 order"
  - "Two author-approved skill plugins enabled from commit-pinned marketplaces in .claude/settings.json"
  - "A Tracked Claude Code files note (AIDEV-03)"
affects: [03-domain-model, phase-08-project-skills, 02-14-doctor]

actuals:
  tokens: 12000
  tasks: 3
  commits: 3
plan_head_before: 95238f6c611991d406d7a956d95a89a5a71eece3
plan_head_after: 2a7c91e6e99d2a793e7e31c7e3b6819fc777e985

tech-stack:
  added: ["axoniq-app-development plugin (marketplace axoniq)", "gradle-skills plugin (marketplace gradle-skills)"]
  patterns:
    - "Third-party skills are installed only as marketplace plugins, registered with extraKnownMarketplaces and a full 40-hex commit SHA in ref, enabled in enabledPlugins; nothing is copied into .claude/"
    - "Every plugin needs a recorded per-candidate verdict and explicit author approval of that commit before it is enabled"

key-files:
  created: []
  modified:
    - docs/tooling.md
    - .claude/settings.json

key-decisions:
  - "The author approved option both: axoniq-app-development@axoniq pinned at 59e7beb6bb487894aeb1294918463af1432bdece and gradle-skills@gradle-skills pinned at 00b7790139dff32063976f98334b2a2301b50a9d (the commit of tag 1.1.0), keeping the tag commit although the default branch is four commits ahead"
  - "No autoUpdate key is written: third-party marketplaces default to no auto-update, and the SHA ref makes updates a reviewed pull request anyway"
  - "docs/tooling.md carries no commit SHAs or version numbers; the SHAs live only in .claude/settings.json, which the two inventory rows name as their Version source"

patterns-established:
  - "AIDEV-01 gate: a blocking-human decision checkpoint precedes any skill or plugin install"

requirements-completed: [AIDEV-01, AIDEV-03]

coverage:
  - id: D1
    description: "docs/tooling.md records the skills survey for all nine AIDEV-01 areas in order, with a verdict per candidate, the review criteria and the install rules"
    requirement: AIDEV-01
    verification:
      - kind: other
        ref: "grep -E '^\\| (Axon|Kotlin|Gradle|Spring Boot|Playwright|React/shadcn|FSD|GitHub Actions|Paketo) ' docs/tooling.md lists every area, survey rows in AIDEV-01 order"
        status: pass
      - kind: other
        ref: "./gradlew checkToolingInventory (BUILD SUCCESSFUL)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Both approved plugins are registered from marketplaces pinned by a full 40-hex SHA and enabled; every enabled plugin names a declared marketplace; no enabledMcpjsonServers; all 55 ask rules kept"
    requirement: AIDEV-01
    verification:
      - kind: other
        ref: "jq: all extraKnownMarketplaces refs match ^[0-9a-f]{40}$ (true); enabledPlugins marketplaces subset of extraKnownMarketplaces (true); no enabledMcpjsonServers (true); .permissions.ask length 55"
        status: pass
      - kind: other
        ref: "./gradlew checkClaudeAskRules spotlessCheck (BUILD SUCCESSFUL)"
        status: pass
    human_judgment: false
  - id: D3
    description: "In Claude Code, /plugin lists both plugins as installed from the pinned marketplaces and a session start loads them without errors"
    requirement: AIDEV-01
    verification: []
    human_judgment: true
    rationale: "Interactive Claude Code behaviour after the workspace-trust prompt; not run by the executor to avoid registering marketplaces in the user's own Claude Code configuration"
  - id: D4
    description: "Only .claude/settings.json, .claude/CLAUDE.md and the D-39 skills/spike-findings-* files are tracked under .claude/"
    requirement: AIDEV-03
    verification:
      - kind: other
        ref: "git ls-files .claude filtered against the allow list (no other path)"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 05: Claude Code skills survey and pinned plugins Summary

**Survey of Claude Code skills for all nine AIDEV-01 areas with recorded verdicts, and the author-approved Axon (`axoniq-app-development`) and Gradle (`gradle-skills`) plugins enabled from marketplaces pinned by full 40-character commit SHA in `.claude/settings.json`.**

## Performance

- **Duration:** about 25 min of executor time, excluding the approval round trip
- **Started:** 2026-11-23T08:45Z (approximate)
- **Completed:** 2026-11-23
- **Tasks:** 3 (Task 2 was the approval checkpoint and has no commit)
- **Files modified:** 2

## Accomplishments

- `docs/tooling.md` gains a "Claude Code skills" section (install rules, review criteria, survey table) and a "Tracked Claude Code files" section. The survey covers Axon, Kotlin, Gradle, Spring Boot, Playwright, React/shadcn, FSD, GitHub Actions and Paketo in that order; each candidate row has evidence, a verdict and a phase.
- Verdicts: Axon and Gradle plugins installed; `axoniq-migration` and `axoniq-framework-contribution` rejected (not applicable); Kotlin skills rejected (JPA and Android only, unpinned remote MCP server); no Kotlin-idiom skill found; Spring Tools deferred (CDN marketplace cannot be pinned by commit, runs a language server, ships hooks); community Spring skills rejected; Playwright, shadcn and FSD deferred to their phases; GitHub Actions auditors rejected (no AI workflows, zizmor and actionlint already gate CI); Paketo none found.
- `.claude/settings.json` registers `axoniq` and `gradle-skills` under `extraKnownMarketplaces` with SHA `ref` values and enables `axoniq-app-development@axoniq` and `gradle-skills@gradle-skills`. All 55 `ask` rules are unchanged; there is no `enabledMcpjsonServers` and no `autoUpdate`.
- `docs/tooling.md` main table has one row per installed plugin (Version source `.claude/settings.json (ref)`, Installation `Claude Code marketplace plugin`, Used in `local (Claude Code)`).

## Author Approval Record (D-40, blocking-human checkpoint)

- **Decision:** option `both`, answered by the author on 2026-11-23 after reviewing both repositories at the pinned commits.
  - `AxonIQ/agent-skills` at `59e7beb6bb487894aeb1294918463af1432bdece`, plugin `axoniq-app-development`. Re-checked at execution: still the default-branch HEAD, Apache-2.0, plugin directory is Markdown plus `plugin.json` only. The hooks and settings at that repository's root are the maintainers' own development files and are not part of the plugin source.
  - `gradle/gradle-skills` at `00b7790139dff32063976f98334b2a2301b50a9d` (the commit of tag 1.1.0), plugin `gradle-skills`. At execution the default branch had moved four commits past the tag (a CI action bump and evaluation notes only; no skill content). The author chose to keep the tag commit.
- **Claude Code channel note:** pinning a marketplace by commit needs Claude Code 2.1.324 or later. The Homebrew cask `claude-code` (the stable channel) still shipped 2.1.323, so the author switched to the cask `claude-code@latest`; `claude --version` now prints 2.1.330 (checked by the orchestrator and again by the executor before Task 3). `docs/tooling.md` records the channel advice without a version number.
- Key shapes were confirmed against the current documentation before editing: `extraKnownMarketplaces.<name>.source` with `source: github`, `repo` and `ref` (a full lowercase commit SHA is accepted on 2.1.324 or later), `enabledPlugins` keyed `plugin@marketplace` with a Boolean, and `autoUpdate` optional and off by default for third-party marketplaces. Both marketplaces use relative-path plugin sources, so the marketplace commit pins the plugin content as well.

## Task Commits

1. **Task 1: Skills survey recorded** - `861a265` (docs)
2. **Task 2: Author decision and Claude Code upgrade** - no commit (blocking-human checkpoint, answered `both`)
3. **Task 3: Register approved plugins with SHA-pinned marketplaces, finalise verdicts** - `2a7c91e` (feat)

**Plan metadata:** the commit holding this SUMMARY (docs: complete plan).

## Files Created/Modified

- `docs/tooling.md` - skills survey, install rules, review criteria, tracked-files note, two inventory rows
- `.claude/settings.json` - `extraKnownMarketplaces` and `enabledPlugins` for the two approved plugins

## Decisions Made

- No `autoUpdate` key; the SHA pin plus reviewed pull requests make updates deliberate (see key-decisions in the frontmatter).
- The SHAs are written only in `.claude/settings.json`; the document names the file as the version source, which keeps a single place to update and respects the no-version-numbers rule of `docs/tooling.md`.

## Deviations from Plan

None - plan executed exactly as written. Two small observations, neither a deviation: the Gradle skills default branch moved after research (handled by the author's explicit choice to keep the tag commit), and the plan's Task 3 human-check (`/plugin` listing) was not run by the executor; it is recorded as D3 above for the verifier and author.

## Issues Encountered

- The Claude Code stable channel lagged behind the minimum version for commit-pinned marketplaces; resolved by the author switching to the `latest` channel (see the approval record).
- The sandbox refuses git commands in compound shell forms and treated the word "GitHub" in a loop as a git invocation; verification commands were split into plain separate calls. No effect on the result.

## Known Stubs

None.

## Threat Flags

None. The marketplaces, plugin content and settings file are covered by T-02-12 to T-02-14 and T-02-SC: refs are full SHAs (jq-checked), only the author-approved markdown-only plugins are enabled, no hooks, scripts or MCP servers are pulled in, no install commands ran, and the spike-findings skill and ADRs are documented as winning over third-party guidance.

## User Setup Required

Each contributor needs Claude Code at the minimum version for commit-pinned marketplaces (`scripts/doctor.sh`, plan 02-14, will check it) and must accept the workspace-trust prompt once so the marketplaces register; each user then installs the enabled plugins through `/plugin`.

## Next Phase Readiness

Agents in Phases 2 to 4 have vetted Axon 5 and Gradle guidance. Plan 02-14 (`doctor`) should check the Claude Code minimum version. Other areas wait for their phases per the survey. No blockers.

## Self-Check: PASSED

- `docs/tooling.md` and `.claude/settings.json` exist and are modified by commits `861a265` and `2a7c91e`, both ancestors of HEAD.
- Plan verification re-run: all refs match 40-hex, every enabled plugin names a declared marketplace, no `enabledMcpjsonServers`, ask rules 55, `checkClaudeAskRules`, `spotlessCheck` and `checkToolingInventory` pass, no version numbers in `docs/tooling.md`, tracked `.claude/` files are within the allow list.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
