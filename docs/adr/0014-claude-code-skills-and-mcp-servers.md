---
status: "accepted"
date: "2026-11-25"
decision-makers: ["repository author"]
---

# ADR-0014: Claude Code skills and MCP servers are pinned, individually approved and never auto-updated

## Context and Problem Statement

The project is developed with Claude Code and GSD, and the agents benefit from vetted skills (Axon, Gradle) and from MCP
servers for library documentation (Context7) and browser checks (Playwright). A skill is an instruction that an agent
follows, and an MCP server is a program that an agent can call, so both are an input to code changes and a supply-chain
surface (AIDEV-01, AIDEV-03, AIDEV-04).

Four questions came up in Phase 2:

- Which files under `.claude/` are tracked, given that GSD generates a spike-findings skill at a fixed path?
- How are third-party skills admitted, pinned and updated?
- How are the MCP servers installed, started and kept free of secrets?
- Who may change which version, and how does an update arrive?

## Decision Drivers

- Third-party skills and servers are untrusted input until a human has reviewed that exact content.
- A version must have one source and an update must be a reviewable pull request, never a silent change.
- No secret in a tracked file, and no pre-approved server.
- The project's own skills stay in a repository-local plugin outside `.claude/` (AIDEV-02, Phase 8).

## Considered Options

- Track `.claude/skills/spike-findings-*/` as a narrow exception (chosen), versus moving the findings into the plugin, versus
  tracking all own skills in `.claude/skills`.
- Survey every stack area and install only what Phases 2 to 4 need (chosen), versus install every candidate now.
- Skills only as marketplace plugins pinned by full commit SHA (chosen), versus copying skills into `.claude/`.
- Vendoring a skill that no marketplace publishes as a byte-identical copy in `.claude/skills/` at a pinned commit (chosen
  on 2026-11-25 for `dynamic-consistency-boundaries`), versus waiting until AxonIQ publishes it, versus moving it into the
  repository-local plugin of Phase 8 (AIDEV-02).
- MCP servers as exact-version mise `npm:` tools (chosen), versus `npx` with an exact version and a Renovate regex manager.
- GSD `agent_skills` filled in Phase 2 after the survey (chosen), versus deferred.

## Decision Outcome

Chosen option: "pinned marketplace plugins and mise-installed MCP servers, each approved by the author", because it keeps
every version in one reviewed place and every admission a human decision.

### Tracked files (D-39, amending AIDEV-03)

`.gitignore` ignores everything under `.claude/` except `settings.json`, `CLAUDE.md` and `.claude/skills/spike-findings-*/` and `.claude/skills/dynamic-consistency-boundaries/` (see the vendored skill below).
The spike-findings skill is one of two narrow exceptions, because it is a GSD-generated artifact at the path the GSD wrap-up
expects and appends to. The GSD installation and `settings.local.json` stay ignored. Claude Code documents only
`.claude/skills/` for project skills, and the GSD wrap-up path is hard-coded, so neither moving the findings nor `.agents/skills`
was an option.

### Skills (D-40, AIDEV-01)

- **Survey, then install what is needed.** The survey covers Axon, Kotlin, Gradle, Spring Boot, Playwright, React and shadcn,
  Feature-Sliced Design, GitHub Actions and Paketo, and `docs/tooling.md` records a verdict for every candidate, with the review
  criteria: owner and licence, Markdown-only content, a full commit SHA, no auto-update, what the plugin can read or run, and
  the author's approval. Only the plugins needed in Phases 2 to 4 are installed: `axoniq-app-development` (Axon) and
  `gradle-skills` (Gradle). The survey rejected the Kotlin skills as not applicable (JPA and Android, and a bundled unpinned remote
  MCP server), deferred the Spring Boot plugin until it can be pinned by commit (it installs from a CDN URL, downloads and
  runs a language server and ships hooks), and deferred the Playwright, React and FSD skills to their phases.
- **Only as marketplace plugins, pinned by SHA.** A marketplace is registered under `extraKnownMarketplaces` in
  `.claude/settings.json` with `ref` set to a full 40-character commit SHA, and the plugin is enabled under `enabledPlugins`.
  A branch, a tag or `latest` is never a `ref`, no plugin is copied into `.claude/`, and no `autoUpdate` key is written: an
  update is a pull request that changes the SHA after the new content was reviewed. The author approved both plugins
  individually on 2026-11-23 at the commits `59e7beb` (`axoniq-app-development`) and `00b7790` (`gradle-skills`, the commit of
  its tag 1.1.0, kept although the default branch is four commits ahead). The SHAs live only in `.claude/settings.json`.
- **Claude Code itself.** Pinning a marketplace by commit needs Claude Code 2.1.324 or newer. The stable release channel lagged
  at 2.1.323, so the installation moved to the `claude-code@latest` Homebrew cask, and `scripts/doctor.sh` enforces the minimum.
- **`agent_skills` (D-41).** The GSD mapping of skills to agents was proposed after the survey and approved by the author as
  the option `project-and-plugins`. It is pinned in `.planning/config.json` and `governance/gsd-config-policy.json` together
  and is recorded in ADR-0004, which is not repeated here.

### Vendored skill (author decision of 2026-11-25, amending AIDEV-01 and AIDEV-03)

- **What.** The `dynamic-consistency-boundaries` skill of `AxonIQ/AxonFramework`, added upstream on 2026-06-16, is copied
  byte for byte at commit `f521585` (the full SHA is in the README beside the copy). The author approved it on 2026-11-25
  against the review criteria: official AxonIQ repository, Apache-2.0, Markdown only.
- **Why.** No marketplace publishes it, so the plugin rule of D-40 cannot admit it. Phase 3 designs DCB decisions now.
  Claude Code loads project skills only from `.claude/skills/`, and the repository-local plugin of AIDEV-02 is for the
  project's own skills and arrives in Phase 8.
- **Amendment.** AIDEV-01 says that third-party skills are installed as marketplace plugins with nothing copied into
  `.claude/`, and AIDEV-03 limits the tracked `.claude/` files. Both now allow this one vendored skill.
- **Controls.** The copy is never edited and sits outside the formatters. Its README is the one place of the pin and holds
  the verify and update steps. The `.gitignore` exception and the `claude-tracking` allow-list of `scripts/doctor.sh` name
  exactly this directory. `REUSE.toml` resolves the copies to Apache-2.0 with the AxonIQ copyright, and `NOTICE` names
  them. The `agent_skills` mapping is pinned by the whole-file policy of ADR-0004.
- **Updates.** No Renovate manager tracks the vendored skill. A custom manager could change only the commit in the
  README, because copying the new files needs post-upgrade commands, which Renovate runs only where its operator allows
  them, so its pull request would leave the pin and the content disagreeing. A tracker of the framework's main branch
  would also propose nearly every framework commit, although the skill has changed in one commit so far. The marketplace
  plugin pins are updated by hand for the same reason of review. The update is by hand: check whenever Renovate proposes
  an Axon Framework update and before planning a phase that designs DCB decisions, review the upstream diff against the
  criteria, re-copy the files and record the author's approval in the pull request.
- **Caveat.** The skill targets the framework's main branch (5.5.0-SNAPSHOT at the pinned commit), while the project
  pins 5.4.2. The spike findings skill and the ADRs win where they disagree.
- **Exit.** Once a marketplace publishes the skill, a pinned plugin replaces the copy and both exceptions go.

### MCP servers (D-42, AIDEV-04)

- **Installed through mise.** Context7 and Playwright MCP are `npm:` tools in `mise.toml` with an exact version, checksums in
  `mise.lock` and one version source (INFRA-09), updated by Renovate. The author approved Context7 4.7.0 and Playwright MCP 0.0.85
  on 2026-11-23, chose those pinned versions over the newest release that was at least seven days old, and accepted the exactly
  pinned alpha Playwright dependency `1.66.0-alpha-2026-11-09` of the server.
- **Started by `.mcp.json`, without secrets.** `.mcp.json` runs `mise x -- context7-mcp` and `mise x -- playwright-mcp --isolated
--output-dir .playwright-mcp`, carries no `env` block and references no secret. Context7 works without a key; a user who wants a
  higher rate limit exports `CONTEXT7_API_KEY` in their own environment. Both servers were started over stdio from a
  subdirectory to confirm that mise finds `mise.toml` from any working directory, so no `-C` form is needed. `.playwright-mcp/`
  is git-ignored.
- **Never pre-approved.** The repository sets no `enabledMcpjsonServers` and no `enableAllProjectMcpServers`: each user approves
  the servers the first time Claude Code asks. Edits to `.mcp.json` and to `.claude/settings.json` are `ask` rules and governed
  paths.
- **No automerge for agent tooling.** A Renovate rule keeps the two servers out of every automerge rule. It sits after all of
  them, because later rules win and every Playwright MCP release is a `0.0.N` patch bump that the patch automerge rule for mise
  tools would otherwise merge unreviewed. The author reads the changelog, runs `claude mcp list` and merges by hand.
- **Personal servers stay local.** The IntelliJ MCP server and, optionally, the GitHub MCP server are added in local scope and
  documented, never committed.

### Consequences

- Good, because a skill or server changes only through a pull request that shows the new SHA or version, and no admission
  happens without the author.
- Good, because there is one place per version: `.claude/settings.json` for the plugins, `mise.toml` for the servers.
- Good, because the servers need no secret in a tracked file.
- Bad, because each update costs a manual review, which is the intended price of the control.
- Good, because the DCB concepts and the API map reach every agent and session with no install step.
- Bad, because the tracked exceptions are a rule with two holes that a reader must remember, the spike-findings skill and the
  vendored skill, and the spike-findings skill is generated content in the repository.
- Bad, because the vendored copy can fall behind upstream unnoticed between manual checks.
- Bad, because the pinned MCP servers depend on an alpha Playwright build until a release replaces it.
- Bad, because the `global:` skills of the `agent_skills` mapping have no effect on a machine where the plugins are not yet
  installed (ADR-0004).

### Confirmation

- `checkToolingInventory` keeps `docs/tooling.md` in step with `mise.toml`, and `checkClaudeAskRules` keeps the ask rules for the
  governed files.
- `scripts/doctor.sh` checks the MCP servers, the tracked `.claude/` files and the Claude Code release against the minimum.
  Its tracked-file check names the vendored directory, and `reuse lint` checks the licence of the vendored files.
- Renovate's configuration check (`renovate-config-validator`) runs in CI, and the agent-tooling rule is the last of the
  automerge-related rules.

## Pros and Cons of the Options

### Narrow exception for the spike-findings skill

- Good, because the GSD wrap-up writes to the path it expects, and the findings stay a reviewable artifact.
- Bad, because it is an exception to a rule that otherwise tracks two files.
- Moving the findings into the plugin would have conflicted with AIDEV-02, which places the project's own skills in a
  repository-local plugin outside `.claude/`, and tracking all own skills in `.claude/skills` would have widened the
  exception.

### Survey everything and install what is needed

- Good, because every candidate has a recorded verdict and the later phases start from it.
- Bad, because the survey costs time for areas that are installed later. Installing every candidate now would admit
  unreviewed instructions for areas with no work yet.

### Marketplace plugins pinned by SHA

- Good, because the SHA pins the whole plugin and an update is a reviewed change.
- Bad, because it needs a recent Claude Code.
- Copying skills into `.claude/` would put third-party instructions into a tracked directory and lose the provenance.
  The one vendored skill is the exception for a skill that no marketplace publishes, and it keeps its provenance in a
  README beside it.

### Vendoring a skill that no marketplace publishes

- Good, because the skill is available now, pinned and reviewable, with its provenance beside it.
- Bad, because an update is manual, the tracked `.claude/` files get a second exception, and the upstream skill is
  written for an unreleased framework version.

### MCP servers through mise `npm:` tools

- Good, because one version source holds the version and the checksums, and Renovate manages it.
- Bad, because the mise npm backend adds sidecar lockfiles that Renovate must treat specially.
- `npx` with an exact version needs a Renovate regex manager and downloads at start, so the fallback was not needed.

### `agent_skills` in Phase 2

- Good, because the survey result reaches the agents right away.
- Bad, because the mapping is a governed change to two files in one pull request.

## More Information

- Decisions: D-39 to D-42, and AIDEV-01, AIDEV-03 and AIDEV-04, and the author decision of 2026-11-25 that vendors the
  DCB skill. `docs/tooling.md` holds the inventory, the review criteria and the survey verdicts.
- Related: ADR-0004 (the `agent_skills` mapping and the GSD configuration policy) and ADR-0012 (the npm developer tools are outside
  the licence check and pass through the AIDEV-04 gate instead).
- Revisit a deferred verdict when its phase starts or when its blocker is lifted, for example when the Spring Boot plugin can
  be pinned by commit. Revisit the exception for the spike-findings skill if GSD changes its wrap-up path. When AxonIQ
  publishes the DCB skill in a marketplace, replace the vendored copy with a pinned plugin.
