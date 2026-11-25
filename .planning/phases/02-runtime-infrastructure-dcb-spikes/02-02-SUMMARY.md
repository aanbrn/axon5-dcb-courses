---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 02
subsystem: infra
tags: [mcp, context7, playwright, mise, renovate, claude-code]

requires:
  - phase: 01-foundation
    provides: mise.toml with locked = true, mise.lock and sidecar lockfiles, docs/tooling.md inventory check, Renovate config
provides:
  - Context7 and Playwright MCP servers pinned as exact npm tools in mise.toml and locked for four platforms
  - Project-scope .mcp.json starting both servers through mise, with no env block and no secret
  - Tooling inventory rows, an MCP servers section and the local-scope instructions for personal servers
  - A Renovate rule that never automerges the two servers
affects: [03-domain-model, 05-frontend, phase-ui-verification]

actuals:
  tokens: 15000
  tasks: 3
  commits: 2
plan_head_before: 9d5403637ab471533f7767dabf9b0ab5695be0a6
plan_head_after: 6c7e27e2b3030435bc10a7c2fa8c5c2fac3fd366

tech-stack:
  added: ["@upstash/context7-mcp 4.7.0", "@playwright/mcp 0.0.85"]
  patterns:
    - "Shared agent tooling is pinned once as an exact npm: tool in mise.toml and started from .mcp.json through `mise x --`"
    - "Project MCP servers are never pre-approved; each user approves them in Claude Code"

key-files:
  created:
    - .mcp.json
    - .mise/locks/npm-upstash-context7-mcp/4.7.0/package.json
    - .mise/locks/npm-upstash-context7-mcp/4.7.0/aube-lock.yaml
    - .mise/locks/npm-playwright-mcp/0.0.85/package.json
    - .mise/locks/npm-playwright-mcp/0.0.85/aube-lock.yaml
  modified:
    - mise.toml
    - mise.lock
    - docs/tooling.md
    - renovate.json5
    - .gitignore

key-decisions:
  - "The author chose the plan's pinned versions (context7 4.7.0, playwright 0.0.85) over the newest release at least seven days old, and accepted the exact-pinned alpha Playwright dependency 1.66.0-alpha-2026-11-09"
  - "mcp.json uses the plain `mise x -- <binary>` form, not `-C ${CLAUDE_PROJECT_DIR}`: mise finds mise.toml by walking up from any subdirectory, and the `${` form would add a variable reference"
  - "The Renovate no-automerge rule sits after all three automerge rules, because later rules win and every Playwright MCP release is a 0.0.N patch bump"

patterns-established:
  - "AIDEV-04 gate: a blocking-human package-legitimacy checkpoint precedes any install of agent-facing tooling"

requirements-completed: [AIDEV-04]

coverage:
  - id: D1
    description: "Both MCP servers are pinned as exact npm: tool versions in mise.toml and locked with checksums for linux-x64, linux-arm64, macos-arm64 and macos-x64 (mise.lock plus sidecar lockfiles)"
    requirement: AIDEV-04
    verification:
      - kind: other
        ref: "mise lock --check (exit 0; 19 tool versions, 76 platform entries)"
        status: pass
      - kind: other
        ref: "mise x -- context7-mcp --help && mise x -- playwright-mcp --help"
        status: pass
    human_judgment: false
  - id: D2
    description: "Project-scope .mcp.json starts exactly the context7 and playwright servers from the mise-installed binaries with no env block and no ${ reference"
    requirement: AIDEV-04
    verification:
      - kind: other
        ref: "jq keys == context7,playwright; grep -c '${' .mcp.json == 0; no server has an env key"
        status: pass
      - kind: other
        ref: "MCP initialize handshake over stdio from the docs/ subdirectory: Context7 v4.7.0 and Playwright 1.66.0-alpha-2026-11-09 both answered"
        status: pass
    human_judgment: false
  - id: D3
    description: "Both servers are approved in Claude Code by the author and connected in /mcp"
    requirement: AIDEV-04
    verification: []
    human_judgment: true
    rationale: "Per-user approval prompt in Claude Code; reported by the author in an interactive session, no output captured"
  - id: D4
    description: "docs/tooling.md lists both servers with their exact mise.toml keys, an MCP servers section and the local-scope instructions; Renovate never automerges them; .playwright-mcp/ is ignored"
    requirement: AIDEV-04
    verification:
      - kind: other
        ref: "./gradlew checkToolingInventory (BUILD SUCCESSFUL)"
        status: pass
      - kind: other
        ref: "renovate-config-validator --strict renovate.json5 (exit 0)"
        status: pass
      - kind: other
        ref: "no x.y.z version string outside comments in docs/tooling.md (count 0)"
        status: pass
    human_judgment: false

duration: 25min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 02: Shared MCP servers (AIDEV-04) Summary

**Context7 4.7.0 and Playwright MCP 0.0.85 pinned as exact mise npm tools with four-platform locks, started from a secret-free project-scope `.mcp.json`, inventoried in `docs/tooling.md`, and shielded from Renovate automerge.**

## Performance

- **Duration:** about 25 min in this continuation (the approval checkpoint round-trips excluded)
- **Started:** 2026-11-23 (continuation after the Task 1 approval)
- **Completed:** 2026-11-23
- **Tasks:** 3 (Task 1 approval-only, Tasks 2 and 3 committed)
- **Files modified:** 10 (5 created, 5 modified)

## Accomplishments

- Both servers install from `mise.lock` under `locked = true`, with no `trust_policy_excludes` exception.
- `.mcp.json` declares exactly `context7` and `playwright`. Neither server shadows the other: they resolve to the distinct binaries `context7-mcp` and `playwright-mcp`. There is no `env` key and no `${` anywhere, so an unset variable can never reach a server as literal text.
- Before the author approved, `claude mcp list` showed both project servers as pending, which is the intended no-pre-approval state:
  ```
  context7: mise x -- context7-mcp - ⏸ Pending approval (run `claude` to approve)
  playwright: mise x -- playwright-mcp --isolated --output-dir .playwright-mcp - ⏸ Pending approval (run `claude` to approve)
  ```
  The author then approved both servers interactively in a new Claude Code session and confirmed that `/mcp` showed both connected. No output was pasted; this is the author's report.
- Playwright MCP runs `--isolated`, with its output in the git-ignored `.playwright-mcp/`.
- `docs/tooling.md` holds the two rows in the same order as the `mise.toml` keys, plus an MCP servers section (project scope, per-user approval, optional `CONTEXT7_API_KEY`, browser install, `claude mcp add --scope local` for IntelliJ and GitHub MCP). It contains no version numbers.
- Renovate rule "Agent tooling (MCP servers): never automerge" sits after the automerge rules and carries a `prBodyNotes` reminder to re-run `claude mcp list`.

## AIDEV-04 Approval Record

Package-legitimacy checkpoint (Task 1, `gate="blocking-human"`) answered "APPROVED" by the author on 2026-11-23, before anything was installed. The author asked for both servers, using exactly the plan's pinned versions rather than the newest release at least seven days old. The author also accepted the exact-pinned alpha dependency of `@playwright/mcp`. Both servers go into the project-scope `.mcp.json`. IntelliJ MCP and GitHub MCP stay personal (local scope) and are only documented.

Evidence gathered from the npm registry by the orchestrator on 2026-11-23 before asking:

| Package | Repository and maintainers | Approved version | Published | Newer releases at approval time | Install scripts |
| ------- | -------------------------- | ---------------- | --------- | ------------------------------- | --------------- |
| `@upstash/context7-mcp` | git+https://github.com/upstash/context7.git; Upstash staff | 4.7.0 | 2026-11-12T09:04:46Z | 4.7.1 (2026-11-16), 4.7.2 and 4.7.3 (2026-11-17), all under seven days old | none |
| `@playwright/mcp` | git+https://github.com/microsoft/playwright-mcp.git; Microsoft (including microsoft-oss-releases) | 0.0.85 | 2026-11-09T18:31:27Z | 0.0.86 (2026-11-18), under seven days old | none; depends on `playwright` and `playwright-core` 1.66.0-alpha-2026-11-09 |

## Task Commits

1. **Task 1: Package legitimacy and AIDEV-04 approval** - no commit (approval-only checkpoint)
2. **Task 2: Pin both servers in mise, lock, write `.mcp.json`** - `36753d0` (feat)
3. **Task 3: Inventory, Renovate no-automerge rule, `.gitignore` entry** - `6c7e27e` (docs)

**Plan metadata:** the commit holding this SUMMARY (docs: complete plan).

## Files Created/Modified

- `mise.toml` - exact pins `"npm:@upstash/context7-mcp" = "4.7.0"` and `"npm:@playwright/mcp" = "0.0.85"` under an explanatory comment
- `mise.lock` - locked entries for both tools on four platforms
- `.mise/locks/npm-upstash-context7-mcp/4.7.0/` and `.mise/locks/npm-playwright-mcp/0.0.85/` - sidecar `package.json` and `aube-lock.yaml`
- `.mcp.json` - project-scope servers `context7` and `playwright`
- `docs/tooling.md` - inventory rows and the MCP servers section
- `renovate.json5` - no-automerge rule for both packages
- `.gitignore` - `.playwright-mcp/`

## Decisions Made

- Plain `["x", "--", "<binary>"]` args, no `-C ${CLAUDE_PROJECT_DIR}` (see Assumption A9 below).
- The `.gitignore` entry is `.playwright-mcp/` without a leading slash, so it matches the output directory at any depth. `--output-dir .playwright-mcp` is relative to the server's working directory, so a Claude Code session started from a subdirectory would write it there.
- `.claude/settings.json` was not touched: no `enabledMcpjsonServers` or `enableAllProjectMcpServers`.

## Assumption A9 (where Claude Code starts stdio servers)

I started both servers from the `docs/` subdirectory with a real MCP `initialize` handshake over stdio, using the exact `.mcp.json` command and args. Context7 answered as v4.7.0 and Playwright as 1.66.0-alpha-2026-11-09. mise finds `mise.toml` by walking up the directory tree, so the cwd form works from any subdirectory and the `-C` form is unnecessary. The only caveat is the relative `--output-dir`, handled by the unanchored `.gitignore` entry.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `mise lock` had to run before `mise install`**
- **Found during:** Task 2
- **Issue:** `mise.toml` sets `locked = true`, so `mise install` refused both new tools with "is not in the lockfile". The plan ordered `mise install` before `mise lock`.
- **Fix:** Ran `mise lock --platform linux-x64,linux-arm64,macos-arm64,macos-x64` first, then `mise install`, which succeeded. No trust-policy exception was needed.
- **Files modified:** `mise.lock`, `.mise/locks/npm-*-mcp/**`
- **Verification:** `mise install` installed both tools; `mise lock --check` exits 0.
- **Committed in:** `36753d0`

---

**Total deviations:** 1 auto-fixed (1 blocking, ordering only)
**Impact on plan:** None on the result; only the order of two commands changed.

## Issues Encountered

- `renovate-config-validator` prints a re2 stack trace before "Config validated successfully against 1 file(s)". It exits 0 under `--strict` and the trace was not caused by this plan's changes, so it is out of scope and was not investigated.
- A Context7 and Playwright sidecar `aube-lock.yaml` is large (about 900 lines for Context7). It is generated and committed on purpose, as the `npm:renovate` precedent does.

## Known Stubs

None.

## Threat Flags

None. `.mcp.json` and the two npm packages were already in the plan's threat model (T-02-04 to T-02-07, T-02-SC). Each is mitigated: the approval checkpoint ran first, versions are exact and locked, there is no secret in `.mcp.json`, and Renovate cannot automerge the servers.

## User Setup Required

None required. Optional: export `CONTEXT7_API_KEY` in your own environment for a higher Context7 rate limit; install the Playwright browser once through the server's `browser_install` tool.

## Next Phase Readiness

Agents on later plans can fetch version-accurate Axon 5, Gradle 9 and Spring Boot 4 documentation through Context7. UI phases have a pinned Playwright MCP server. No blockers.

## Self-Check: PASSED

- Files exist: `.mcp.json`, the four sidecar files under `.mise/locks/npm-*-mcp/`, `mise.toml`, `mise.lock`, `docs/tooling.md`, `renovate.json5`, `.gitignore`.
- Commits `36753d0` and `6c7e27e` are ancestors of HEAD.
- Plan verification re-run: `mise x -- context7-mcp --help`, `mise x -- playwright-mcp --help`, `mise lock --check`, the `.mcp.json` jq and grep checks, `checkToolingInventory`, `renovate-config-validator --strict`, and the no-version-numbers check all pass.
- Acceptance criteria: all pass, including `.claude/settings.json` unchanged.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
