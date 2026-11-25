---
status: diagnosed
trigger: "plugins-inactive-not-installed: Both plugins install and list their skills; the 'global:' agent_skills entries in .planning/config.json resolve through gsd-tools query agent-skills"
created: 2026-11-25T03:10:00Z
updated: 2026-11-25T03:30:00Z
---

## Current Focus

bug_class: Bohrbug (deterministic: state on disk, same result on every session)
hypothesis: CONFIRMED - the two plugins have no install record in ~/.claude/plugins/installed_plugins.json because only the committed .claude/settings.json enables them; Claude Code by design never installs (records) a plugin enabled solely by repo-authored settings when its marketplace is a remote (github) source, so the loader reports "is on in project settings but isn't installed here" (inactive) and `plugin update` fails with `Plugin "<id>" is not installed`. The project's setup (docs/tooling.md, UAT test 2, doctor) treats enabling as installing and has no `claude plugin install <id> --scope project` step or check.
test: done (static evidence from installed_plugins.json, known_marketplaces.json, Claude Code 2.1.331 code paths, official docs)
expecting: n/a
next_action: return ROOT CAUSE FOUND (goal find_root_cause_only)

reasoning_checkpoint:
  hypothesis: "The plugins are inactive and 'not installed' because installed_plugins.json holds no record for axoniq-app-development@axoniq or gradle-skills@gradle-skills; enabledPlugins in the committed .claude/settings.json only enables them, and Claude Code deliberately does not create install records from repo-authored settings for remote (github) marketplaces."
  confirming_evidence:
    - "installed_plugins.json (mtime 2026-11-18) lists only spring-tools and warp; no record at any scope for the two plugins"
    - "Claude Code 2.1.331 sync code: projectSettings entries get fromOwnConfig=false -> 'Skipped auto-recording <id> ... enabled only by repo-authored settings'"
    - "Loader mFi(): relative-path plugin with no install record and only repo-authored enablement -> plugin-not-installed ('is on in project settings but isn't installed here')"
    - "updatePlugin(): no record -> 'Plugin \"<id>\" is not installed' (failureCode not_installed) - the reported update message"
    - "Official docs: 'Setting enabledPlugins in the repository doesn't install the plugin ... run claude plugin install <name>@<marketplace> --scope project'"
    - "history.jsonl: no plugin install ever ran in this project; docs/tooling.md and doctor have no install step or check"
  falsification_test: "If installed_plugins.json had a project/user/local record for either id, or if running `claude plugin install <id> --scope project` followed by /reload-plugins left the plugin still shown as not installed, the hypothesis would be wrong."
  fix_rationale: "Creating the install record (claude plugin install <id> --scope project, per contributor) is the documented way to turn a repo-enabled remote-marketplace plugin into an installed, loaded plugin; documenting the step and checking it in doctor prevents recurrence."
  blind_spots: "The /plugin UI itself was not observed (cannot drive the TUI); the exact label the author read as 'inactive' is inferred from the 2.1.331 strings. The session-only path that loaded the skills in earlier sessions (skillUsage 23/9) and why it stopped in session #39 was not fully traced. Whether a project-scope record keyed on the main checkout path also matches a Claude Code session started inside a git worktree (projectPath is compared exactly) was not tested. Whether `claude plugin install --scope project` rewrites the governed .claude/settings.json (formatting diff) was not tested."
  candidate_causes:
    - "config: enabledPlugins in committed project settings is enable-only; no install record (CONFIRMED)"
    - "environment: Claude Code version / workspace trust (ELIMINATED: 2.1.331, trust accepted, marketplaces registered)"
    - "data: marketplace/plugin name mismatch at the pinned commits (ELIMINATED: names match)"
    - "process/docs: project docs, UAT and doctor equate enable with install; no install step or check (CONTRIBUTING)"
  and_gate: "yes, weakly: the symptom needs (a) Claude Code's enable-only semantics for repo-authored settings with github marketplaces AND (b) the per-contributor install never being performed, which the project's docs/doctor/UAT never prompted. (a) is intended upstream behaviour, so the actionable causes are the missing install and the documentation/check gap."

## Symptoms

expected: In Claude Code, `/plugin` shows axoniq-app-development@axoniq and gradle-skills@gradle-skills installed from the SHA-pinned marketplaces declared in .claude/settings.json (extraKnownMarketplaces with 40-hex refs, enabledPlugins), both active, listing their skills; the 'global:' agent_skills entries in .planning/config.json resolve through `node .claude/gsd-core/bin/gsd-tools.cjs query agent-skills <agent>`.
actual: User reported: "I can see the skills, but they are inactive, and an update attempt says they are not installed." (Observed in the /plugin UI of the author's Claude Code session in this repository.)
errors: None reported beyond the /plugin UI message "not installed" on update.
reproduction: Test 2 in UAT (.planning/phases/02-runtime-infrastructure-dcb-spikes/02-UAT.md)
started: Discovered during UAT on 2026-11-25. Plugin config was added in plan 02-21.

## Eliminated

- hypothesis: marketplace name / plugin name pairs in enabledPlugins do not match the marketplace.json at the pinned commits
  evidence: ~/.claude/plugins/marketplaces/axoniq/.claude-plugin/marketplace.json has name "axoniq" and plugin "axoniq-app-development" (source ./plugins/axoniq-app-development); gradle-skills marketplace.json has name "gradle-skills" and plugin "gradle-skills" (source ./). Both IDs match enabledPlugins exactly.
  timestamp: 2026-11-25T03:20:00Z

- hypothesis: SHA-pinned `ref` on a `github` marketplace source is unsupported or ignored
  evidence: known_marketplaces.json records both marketplaces with the 40-hex ref; the clones' .git/HEAD files hold exactly 59e7beb6... and 00b77901... (detached at the pin). Claude Code 2.1.331 contains SHA-pin code (sha_pin_mismatch, "set as this marketplace's `ref`, as a commit id").
  timestamp: 2026-11-25T03:20:00Z

- hypothesis: folder not trusted, so extraKnownMarketplaces was ignored
  evidence: ~/.claude.json projects["/Users/afanasyev/Development/axon5-dcb-courses"].hasTrustDialogAccepted = true; known_marketplaces.json holds both marketplaces
  timestamp: 2026-11-25T03:21:00Z

- hypothesis: Claude Code too old for commit-pinned marketplaces
  evidence: claude --version prints 2.1.331 (cask claude-code@latest), above the 2.1.324 minimum doctor checks
  timestamp: 2026-11-25T03:21:00Z

- hypothesis: the plugins were installed earlier and the update attempt dropped the install record
  evidence: installed_plugins.json mtime is 2026-11-18, before plan 02-05 (2026-11-23) and before the marketplaces were cloned (2026-11-24); it was never rewritten, so no record ever existed
  timestamp: 2026-11-25T03:22:00Z

## Evidence

- timestamp: 2026-11-25T00:00:00Z
  checked: gsd-tools query agent-skills gsd-debugger --skills-only
  found: empty block (no skills configured for gsd-debugger)
  implication: no self-load needed

- timestamp: 2026-11-25T03:12:00Z
  checked: .claude/settings.json (repo, committed in 2a7c91e, plan 02-05)
  found: extraKnownMarketplaces axoniq (github AxonIQ/agent-skills ref 59e7beb6...) and gradle-skills (github gradle/gradle-skills ref 00b77901...); enabledPlugins axoniq-app-development@axoniq=true, gradle-skills@gradle-skills=true. Nothing else declares these plugins: ~/.claude/settings.json enabledPlugins has only spring-tools and warp; main checkout .claude/settings.local.json has no enabledPlugins.
  implication: the two plugins are enabled ONLY by repo-authored (projectSettings) settings

- timestamp: 2026-11-25T03:13:00Z
  checked: ~/.claude/plugins/known_marketplaces.json
  found: axoniq and gradle-skills recorded with the SHA refs, installLocation under ~/.claude/plugins/marketplaces/, lastUpdated 2026-11-25T02:41Z (during the UAT session)
  implication: marketplace registration via extraKnownMarketplaces worked

- timestamp: 2026-11-25T03:13:00Z
  checked: ~/.claude/plugins/installed_plugins.json (version 2), mtime 2026-11-18
  found: only spring-tools@spring-tools and warp@claude-code-warp (scope user). No record at any scope (user/project/local) for axoniq-app-development@axoniq or gradle-skills@gradle-skills.
  implication: the plugins were never installed (no install record) - the explicit install step never ran

- timestamp: 2026-11-25T03:14:00Z
  checked: ~/.claude/plugins/cache/{axoniq/axoniq-app-development/0.5.1,gradle-skills/gradle-skills/1.1.0}
  found: plugin content present (created 2026-11-24 08:06 +07, same time as the marketplace clones), with .in_use/33929 markers; PID 33929 is the current `claude -c` session started 2026-11-25 09:36 +07
  implication: the content was materialized into the cache and the /plugin UI can list skills from it, but cache presence is not an install record

- timestamp: 2026-11-25T03:16:00Z
  checked: ~/.claude.json pluginUsage/skillUsage
  found: gradle-skills:gradle-best-practices used 23 times (last 2026-11-25T02:11Z, startup #38), axoniq-app-development:axoniq-app-dev 9 times (last 00:19Z). numStartups is now 39; the current subagent's skill list contains amplicode-spring-skills:* (user-installed spring-tools) but no axoniq-app-development:* or gradle-skills:* skill.
  implication: earlier sessions loaded the skills through some session-only path; they are not loaded for the current session's agents

- timestamp: 2026-11-25T03:18:00Z
  checked: Claude Code 2.1.331 binary strings (/opt/homebrew/Caskroom/claude-code@latest/2.1.331/claude), installed_plugins sync function
  found: the settings->installed_plugins.json sync builds entries per settings source with fromOwnConfig = (source is userSettings, or localSettings not tracked by git); projectSettings gets fromOwnConfig=false and hits `Skipped auto-recording ${id} for ${scope} (${projectPath}) - enabled only by repo-authored settings`. Helper e$n(id) returns true only if a user/flag/policy/untracked-local source enables the plugin (U3e: projectSettings -> false).
  implication: by design Claude Code never creates an install record for a plugin that only the committed .claude/settings.json enables

- timestamp: 2026-11-25T03:18:00Z
  checked: Claude Code 2.1.331 plugin loader mFi() for relative-path (string) plugin sources
  found: loads from the install record's installPath (A); if no record and !e$n(id) it pushes {type:"plugin-not-installed", loadsOnFullLoad:true} and returns null. The error text: `Plugin "X" is enabled in project settings but isn't installed - run \`claude plugin install X --scope project\`` (or "install it at project scope (from /plugin or claude plugin install)"). A separate background "settingsInstall" full-load path can bring such plugins into a session only (XTn merges settingsInstall.loaded for loadsOnFullLoad errors).
  implication: without an install record the plugin is not loaded by the normal loader -> shown as enabled-but-not-installed ("inactive"); any session-only load is outside the install records

- timestamp: 2026-11-25T03:18:00Z
  checked: Claude Code 2.1.331 updatePlugin()
  found: reads installed_plugins.json records for the plugin id; if none -> {outcome:"failed", message:`Plugin "${id}" is not installed`, failureCode:"not_installed"}
  implication: exactly the reported "update attempt says they are not installed"

- timestamp: 2026-11-25T03:24:00Z
  checked: official docs https://code.claude.com/docs/en/plugins/org.md ("Require plugins per repository") and plugins/troubleshooting.md
  found: "Setting `enabledPlugins` in the repository doesn't install the plugin. Once the marketplace is registered, ask each contributor to run `claude plugin install <name>@<marketplace> --scope project`. Until they do, the /plugin Errors tab can show a line ending in `isn't installed here`." Install is unnecessary only for a local `directory`/`file` marketplace with relative-path plugin sources. Troubleshooting: `Plugin "<name>" is on in project settings but isn't installed here` -> run the install command, then /reload-plugins; the plugin then appears under Installed. The 2.1.331 binary carries the same UI string.
  implication: documented, intended behaviour; both marketplaces here are `github` sources, so the explicit install is required

- timestamp: 2026-11-25T03:25:00Z
  checked: ~/.claude/history.jsonl entries containing /plugin or plugin install
  found: in this project only `/plugins` at 2026-11-25T02:40Z (the UAT); no `/plugin install` or `claude plugin install` ever ran for these plugins
  implication: the install step was never performed

- timestamp: 2026-11-25T03:26:00Z
  checked: project docs and checks: docs/tooling.md "How skills are installed", 02-05-SUMMARY "User Setup Required", docs/adr/0004 caveat, scripts/doctor.sh check_claude_code, UAT test 2 wording
  found: docs/tooling.md says a skill "is installed only as a marketplace plugin ... registered ... under extraKnownMarketplaces ... and the plugin is enabled under enabledPlugins" and marks both verdicts "Installed", with no `claude plugin install ... --scope project` step anywhere in the repository. Only 02-05-SUMMARY mentions "each user then installs the enabled plugins through /plugin". doctor checks only the Claude Code version (>= 2.1.324), not whether the plugins are installed. UAT test 2 expected the settings alone to make them "install".
  implication: the project's setup treats enabling as installing; no documented step or check makes a contributor perform the install

- timestamp: 2026-11-25T03:27:00Z
  checked: node gsd-tools.cjs query agent-skills gsd-planner / gsd-code-reviewer --skills-only; .planning/config.json agent_skills
  found: global:axoniq-app-development:axoniq-app-dev and global:gradle-skills:gradle-best-practices resolve to "Load the `<plugin>:<skill>` skill via the Skill tool ... (plugin-provided)"; names match the skill folders in the cache (skills/axoniq-app-dev, skills/gradle-best-practices)
  implication: the GSD side resolves correctly; the directives only work once Claude Code loads the plugins, which needs the install record

## Resolution

root_cause: "axoniq-app-development@axoniq and gradle-skills@gradle-skills were enabled but never installed: ~/.claude/plugins/installed_plugins.json has no record for either at any scope. The committed .claude/settings.json (plan 02-05) registers the SHA-pinned marketplaces (which worked) and sets enabledPlugins, but Claude Code by design does not install or record a plugin that only repo-authored settings enable when its marketplace is a remote github source ('Skipped auto-recording ... enabled only by repo-authored settings'). Without the record the loader reports 'is on in project settings but isn't installed here' (inactive), and plugin update fails with 'Plugin \"<id>\" is not installed'. Contributing: docs/tooling.md, UAT test 2 and doctor treat enabling as installing; nothing tells a contributor to run `claude plugin install <id> --scope project` or checks that it ran."
fix: "(not applied - diagnose-only mode)"
verification: ""
files_changed: []
