---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 25
subsystem: tooling
tags: [doctor, claude-code, plugins, shell, bash, jq, worktree, docs]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "scripts/doctor.sh and its shell tests (02-08), the approved plugins pinned in .claude/settings.json (02-05)"
provides:
  - "doctor check claude-plugins: every plugin enabled in .claude/settings.json has an install record usable from this checkout, with the install command as the fix"
  - "docs/tooling.md, docs/quick-start.md and ADR-0004 say that enabling a plugin is not installing it and give the per-clone install step"
affects: [tooling, agent-skills, onboarding, 02-30]

actuals:
  tokens: 9000
  tasks: 2
  commits: 3

plan_head_before: e45269c8271bb77e1b7163856789c17a602c26a8
plan_head_after: e2ea870034db1fbcd1dbc172fea8e1bc0fa672c2

tech-stack:
  added: []
  patterns:
    - "Read-only doctor check over Claude Code's own state file (installed_plugins.json), never a `claude plugin` command"
    - "Record paths compared physically (symlinks resolved) against the checkout and the main worktree of its clone"

key-files:
  created: []
  modified:
    - scripts/doctor.sh
    - scripts/test-doctor.sh
    - docs/tooling.md
    - docs/quick-start.md
    - docs/adr/0004-gsd-configuration.md

key-decisions:
  - "doctor reads ${CLAUDE_CONFIG_DIR:-$HOME/.claude}/plugins/installed_plugins.json instead of running `claude plugin list`, so it stays read-only"
  - "A user-scope record counts everywhere; project and local records count only when projectPath equals this checkout or the main worktree of its repository"
  - "An enabledPlugins key that is not <plugin>@<marketplace> fails the check and is never echoed (T-02-66)"

patterns-established:
  - "Every printed plugin id is validated against a strict pattern before it reaches output or a fix command"

requirements-completed: [AIDEV-01, TOOL-03]

coverage:
  - id: D1
    description: "doctor warns for every enabled plugin without an install usable from this checkout and prints the install command as the fix"
    requirement: TOOL-03
    verification:
      - kind: unit
        ref: "scripts/test-doctor.sh#no install records, foreign project record, user scope record, CLAUDE_CONFIG_DIR, linked worktree, symlink, malformed plugin key, unreadable install records, settings missing, jq missing, claude missing, --ci"
        status: pass
    human_judgment: false
  - id: D2
    description: "On the author's machine, doctor run inside a linked worktree of the clone reports both plugins installed"
    requirement: TOOL-03
    verification:
      - kind: other
        ref: "DOCTOR_SKIP_GRADLE=1 scripts/doctor.sh (ok  claude-plugins: 2 enabled plugins are installed for this clone)"
        status: pass
    human_judgment: false
  - id: D3
    description: "A Claude Code session started inside a linked worktree loads both plugin skills"
    requirement: AIDEV-01
    verification:
      - kind: other
        ref: "claude -p --output-format stream-json --verbose init event, jq filter for the two skills (true), Claude Code 2.1.331"
        status: pass
    human_judgment: false
  - id: D4
    description: "tooling, quick start and ADR-0004 say registering and enabling is not installing, give the per-clone commands and no longer call the plugins installed by the repository"
    requirement: AIDEV-01
    verification:
      - kind: other
        ref: "./gradlew spotlessCheck checkAdrs checkToolingInventory; editorconfig-checker; grep acceptance criteria of Task 2"
        status: pass
    human_judgment: false
  - id: D5
    description: "The author's /plugin screen lists both plugins under Installed with no 'isn't installed here' error (re-test of UAT test 2)"
    requirement: AIDEV-01
    verification: []
    human_judgment: true
    rationale: "The /plugin UI of the author's interactive session is not observable from an executor; the plan defers it to the end-of-phase human check"

duration: 30min
completed: 2026-11-25
status: complete
---

# Phase 2 Plan 25: Plugin install check and docs (G-02-2) Summary

**`scripts/doctor.sh` gains a read-only `claude-plugins` check that warns for every plugin enabled in `.claude/settings.json` without an install record usable from this checkout (worktree-aware, symlink-safe), and the docs now say that enabling a plugin is not installing it.**

## Performance

- **Duration:** about 30 min
- **Started:** 2026-11-25T04:03:00Z (approximate)
- **Completed:** 2026-11-25T04:33:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- `check_claude_plugins` reads the keys of `enabledPlugins` with value `true`, validates each against `<plugin>@<marketplace>`, reads `${CLAUDE_CONFIG_DIR:-$HOME/.claude}/plugins/installed_plugins.json` (format version 2) and reports one warning per id without an install, with the fixes `claude plugin install <id> --scope project (once per clone, in the main checkout)` and `then run /reload-plugins in Claude Code`.
- A user-scope record counts everywhere; a project or local record counts only when its `projectPath`, symlinks resolved, equals this checkout or the main worktree of its repository (taken from `git rev-parse --path-format=absolute --git-common-dir`). A record of another project never counts.
- The check never runs a `claude plugin` command (acceptance grep prints 0) and never prints a record field; it is skipped by `--ci`, skipped when `claude` is not on PATH, and warns (mise fix) without jq.
- `scripts/test-doctor.sh` grew from 318 to 365 passing assertions: healthy fixture with both plugins enabled and installed, and cases for no plugin enabled, only disabled plugins, no records, foreign project record (with a no-leak assertion), user scope, `CLAUDE_CONFIG_DIR`, linked worktree, record and root through a symlink, malformed keys (not echoed, nothing executed), three unreadable-record shapes plus mode 000, missing and non-JSON settings, no jq, no claude, `--ci`.
- Docs: `docs/tooling.md` (Getting started step, both plugin rows, "How skills are installed" with the commands, worktree note and symptom, doctor lists, survey verdicts reworded from "Installed" to "Approved ... install once per clone"), `docs/quick-start.md` (optional prerequisite) and the first caveat of ADR-0004 (status, date, decision and table untouched).
- Real run from this linked agent worktree on the author's machine: `ok  claude-plugins: 2 enabled plugins are installed for this clone (axoniq-app-development@axoniq, gradle-skills@gradle-skills)`.
- Worktree probe (Claude Code 2.1.331, `claude -p --output-format stream-json --verbose --max-turns 1` run from this linked worktree): the `init` event lists both `axoniq-app-development:axoniq-app-dev` and `gradle-skills:gradle-best-practices`; the jq filter returned `true`. The probe was not refused.
- `node .claude/gsd-core/bin/gsd-tools.cjs query agent-skills gsd-planner` names both plugin skills (count 2).

## TDD (Task 1, tracer)

- **RED:** `5ef402d` adds the cases. Run against the unchanged `doctor.sh`: `335 passed, 30 failed`; all 30 failures are assertions on the planned `claude-plugins` output (healthy line, warnings, fix lines, fail and exit code of the malformed key, skip lines, `--ci` skip), with no syntax or fixture fault. Semantic assessment: the target assertions executed and failed because the check does not exist yet. The shell test output is not in a format the `check tdd-red-evidence` classifier supports and the plan is `type: execute` with `workflow.tdd_mode` off, so no classifier record was produced.
- **GREEN:** `6496c83` adds `check_claude_plugins`; `365 passed, 0 failed`.
- **REFACTOR:** none needed.
- Tracer gate: interactive, `end-of-phase`, automated-only verify; re-ran the end-to-end verify (365 passed, real doctor run ok) and continued to Task 2.

## Task Commits

1. **Task 1 RED: failing claude-plugins doctor cases** - `5ef402d` (test)
2. **Task 1 GREEN: claude-plugins doctor check** - `6496c83` (feat)
3. **Task 2: docs say enabling is not installing** - `e2ea870` (docs)

**Plan metadata:** the commit that adds this SUMMARY (follows `e2ea870`, not counted in `actuals.commits`).

## Files Created/Modified

- `scripts/doctor.sh` - `resolve_dir`, `check_claude_plugins`, call in `main`, header and `--ci` usage text
- `scripts/test-doctor.sh` - install-record helpers, healthy fixture with plugins, 47 new assertions
- `docs/tooling.md` - per-clone install step, worktree note, symptom, reworded verdicts, doctor lists
- `docs/quick-start.md` - optional Claude Code prerequisite
- `docs/adr/0004-gsd-configuration.md` - first caveat reworded

## Decisions Made

- Doctor reads Claude Code's records file; it does not call `claude plugin list --json` (a CLI start can write state). The format-version guard turns a future format change into one warning that points to `claude plugin list --json` for a manual look.
- Ids are validated before they are printed or put into a fix command; a malformed key fails the check without being echoed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] shellcheck SC2016 on two intentional single-quoted `$` strings**
- **Found during:** Task 1 (RED commit hook, then GREEN verify)
- **Issue:** the lefthook shellcheck job flagged the `$(touch ...)` text of the malformed-key fixture and the `$id` jq variable
- **Fix:** targeted `# shellcheck disable=SC2016` comments with the reason
- **Files modified:** scripts/test-doctor.sh, scripts/doctor.sh
- **Verification:** `mise x -- shellcheck scripts/doctor.sh scripts/test-doctor.sh` clean, commit hooks pass
- **Committed in:** `5ef402d`, `6496c83`

**2. [Process] Plan commit ledger file not written**
- The sandbox refused the shared `.git/worktrees/<id>` path for the `gsd-plan-head-before` ledger, so the base was taken from the dispatch (`e45269c`, equal to the worktree's starting HEAD, verified at start) and recorded as `plan_head_before`. `commits: 3` was measured with `git rev-list --count e45269c..HEAD` before the SUMMARY commit.

---

**Total deviations:** 1 auto-fixed (Rule 3), 1 process note
**Impact on plan:** none on scope or behavior.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required. The author's installs were done before this plan.

## Threat Flags

None. T-02-66 (key validation, never echoed), T-02-67 (foreign path and user scope tested) and T-02-68 (no record field printed, asserted) are covered by tests.

## Known Stubs

None.

## Next Phase Readiness

- G-02-2 is closed in the repository; the only remaining item is the end-of-phase human check: `/plugin` in the author's main-checkout session lists both plugins under Installed with no "isn't installed here" error.
- `scripts/doctor.sh` and the three docs are governed paths; the orchestrator's merge hook asks the author once. `.claude/settings.json` was not touched (`git log e45269c..HEAD -- .claude/settings.json` prints nothing). Nothing was pushed.

## Self-Check: PASSED

- `scripts/doctor.sh`, `scripts/test-doctor.sh`, `docs/tooling.md`, `docs/quick-start.md`, `docs/adr/0004-gsd-configuration.md` exist and carry the changes.
- Commits `5ef402d`, `6496c83`, `e2ea870` are ancestors of HEAD.
- Verify gates: test-doctor 365 passed 0 failed; shellcheck, spotlessCheck, checkAdrs, checkToolingInventory and editorconfig-checker clean; acceptance greps pass (`check_claude_plugins` 2, `installed_plugins.json` 1, `claude plugin` invocations 0, no commit touches `.claude/settings.json`).

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-25*
