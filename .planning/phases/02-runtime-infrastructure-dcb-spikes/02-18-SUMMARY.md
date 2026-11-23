---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 18
subsystem: infra
tags: [upstream-records, ci, github-actions, bash, awk, workflow-commands, zizmor]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "Upstream record template and front-matter shape (plan 02-15), CI jobs and hygiene rules (plans 02-08, 02-12, 02-14)"
provides:
  - "scripts/upstream/changed-dependencies.sh: changed-key extraction and record matching with a job summary and escaped warning annotations"
  - "scripts/upstream/test-changed-dependencies.sh: 62 shell assertions over temporary git repositories, auto-run by the lint job"
  - "upstream-records CI job (Upstream records advisory), part of ci-success"
affects: [02-16 and 02-17 upstream records (product keys must match the advisory), phase 5 documentation]

actuals:
  tokens: 7700
  tasks: 2
  commits: 2
plan_head_before: 34196d2c9701b8d97559ef056ffdbef9c7c605d7
plan_head_after: c21f43fa453650a0034218aadd0481075954248b

tech-stack:
  added: []
  patterns:
    - "Keys are compared as base-versus-head value maps (TOML sections, workflow action pins), not as diff lines, so moved lines and comment edits are not changes"
    - "Untrusted text reaches stdout only behind a line prefix or after %, CR and LF escaping; the property form also escapes colon and comma"
    - "Shell tests copy a baseline git repository per case and run the real script in it with an empty environment"

key-files:
  created:
    - scripts/upstream/changed-dependencies.sh
    - scripts/upstream/test-changed-dependencies.sh
  modified:
    - .github/workflows/ci.yml

key-decisions:
  - "Records are read from the head revision with git show, not from the working tree, so a record added or removed in the same change is judged as merged"
  - "A base that is not in the checkout (force push) skips the advisory with exit 0 instead of failing, because the job sits under the required ci-success; only usage errors, an unresolvable head revision and an unreadable record exit 2"
  - "Every record line in the Markdown summary carries a prefix (heading, bullet or quote), so no record text can begin a workflow command even when the report goes to standard output"
  - "The script avoids bash 4 features and quoted replacements inside parameter expansion; it runs on the stock macOS bash 3.2 as well as on bash 5"

patterns-established:
  - "Advisory CI job: contents: read, persist-credentials: false, untrusted event values only through env"

requirements-completed: [UPS-04]

coverage:
  - id: D1
    description: "A change that moves a dependency version prints every affected record (open, reported or fixed-upstream, matched by product) with id, status and removal criteria to the job summary and as a warning annotation"
    requirement: UPS-04
    verification:
      - kind: integration
        ref: "bash scripts/upstream/test-changed-dependencies.sh (62 passed, 0 failed)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Changed keys come from [versions], mise [tools], the wrapper, .java-version, .nvmrc and SHA-pinned workflow actions"
    requirement: UPS-04
    verification:
      - kind: integration
        ref: "test-changed-dependencies.sh cases: catalog, mise (quoted key), wrapper, java, node, action, comment-only and [libraries]-only changes"
        status: pass
    human_judgment: false
  - id: D3
    description: "Record text cannot inject workflow commands: %, CR and LF are escaped in annotations, and summary lines are always prefixed"
    requirement: UPS-04
    verification:
      - kind: integration
        ref: "test-changed-dependencies.sh injection cases (summary mode and standard-output mode); mutation check removing the % escape fails the percent case"
        status: pass
    human_judgment: false
  - id: D4
    description: "The upstream-records job is advisory and read-only: contents: read, no secret or comment, base SHA through env, part of ci-success"
    requirement: UPS-04
    verification:
      - kind: command
        ref: "mise x -- actionlint && mise x -- zizmor --offline --no-progress .github/workflows (no findings)"
        status: pass
    human_judgment: false
  - id: D5
    description: "The job behaves as intended on a real pull request and a real push on GitHub"
    verification: []
    human_judgment: true
    rationale: "The workflow only runs on GitHub; the base-SHA expression and the rendered job summary are confirmed on the first pull request that moves a pinned version."

duration: 30min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 18: Upstream records advisory Summary

**A read-only CI job whose script maps changed version keys (catalog, mise, wrapper, java, node, pinned actions) to open upstream records and prints their removal criteria to the job summary and as injection-safe warnings, proven by 62 shell assertions**

## Performance

- **Duration:** about 30 min
- **Completed:** 2026-11-23
- **Tasks:** 2
- **Files modified:** 3 (2 created, 1 modified)

## Accomplishments

- `changed-dependencies.sh <base-sha> [<head-ref>]` compares the `[versions]` table of `gradle/libs.versions.toml` and the `[tools]` table of `mise.toml` between base and head as key-to-value maps (quoted keys unquoted, trailing comments ignored), adds `gradle`, `java` and `node` when their files differ, and `action:<owner>/<repo>` when the set of `uses: <owner>/<repo>@<ref>` pins of a repository changes. Local actions, `docker://` references and `uses:` text inside `run:` strings are not actions.
- Records of `docs/upstream/` are read at the head revision. Status `open`, `reported` and `fixed-upstream` with a changed `product` produce a Markdown section (id, title, product, status, record path, removal criteria as a quote) and `::warning title=Upstream record <id>::<title> - check the removal criteria in docs/upstream/<file>`. `workaround-removed`, `wont-fix` and products without a pinned key never match.
- T-02-42: `%`, CR and LF are escaped in every annotation (`%25`, `%0D`, `%0A`), the title property also escapes `:` and `,`, and every summary line is prefixed, so even the standard-output mode (no `GITHUB_STEP_SUMMARY`) cannot emit a command from record text. The tests feed a title with `%0A::error::injected` and removal criteria lines starting with `::error::` and `::set-output`.
- T-02-43: the `upstream-records` job has `contents: read`, `fetch-depth: 0`, `persist-credentials: false`, no secret and no comment; `BASE_SHA` reaches the script through `env` and `run:` contains no `github.event`. The job is in `ci-success` `needs`; the script exits 0 after any match.
- 62 shell assertions run the real script in a temporary git repository per case, pass on bash 3.2 and bash 5, and are picked up by the existing "Run shell tests" step with no workflow edit.

## Task Commits

1. **Task 1: Changed version keys matched to upstream records, printed safely, proven by shell tests** - `6aa102d` (feat, tracer)
2. **Task 2: The upstream-records job in CI** - `c21f43f` (feat)

Tracer gate: the Task 1 verify (shell tests, shellcheck, shfmt) passed end to end before the CI task started; Tracer verified end-to-end, expanding.

## Files Created/Modified

- `scripts/upstream/changed-dependencies.sh` - key extraction, record parsing (awk subset parser over the front matter), matching, summary and escaped annotations
- `scripts/upstream/test-changed-dependencies.sh` - baseline repository with seven records of every status and product kind, per-case copies, 62 assertions
- `.github/workflows/ci.yml` - `upstream-records` job and its `ci-success` dependency

## Decisions Made

See `key-decisions` above. The plan said to read records from "every `docs/upstream/*.md`"; reading them from the head revision instead of the working tree is equivalent in CI and also correct for a named `<head-ref>`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Test helper mangled replacements on macOS bash 3.2**
- **Found during:** Task 1 (first test run)
- **Issue:** `${content//"$old"/"$new"}` keeps the quote characters of the replacement in bash 3.2, so edited fixtures contained stray quotes; two cases failed and others passed by accident.
- **Fix:** the replacement stays unquoted in `replace_in`, with a comment; the script itself builds escapes with variables for the same reason.
- **Files modified:** `scripts/upstream/test-changed-dependencies.sh`
- **Committed in:** `6aa102d`

**2. [Rule 2 - Missing critical] A base missing from the checkout skips instead of failing**
- **Found during:** Task 1 (design)
- **Issue:** after a force push `github.event.before` is not in the clone; with exit 2 the advisory would fail `ci-success` on a path where it can say nothing useful.
- **Fix:** skip with exit 0 and a message in the summary; usage errors, an unresolvable head and an unreadable record still exit 2. Covered by tests.
- **Files modified:** `scripts/upstream/changed-dependencies.sh`
- **Committed in:** `6aa102d`

---

**Total deviations:** 2 auto-fixed (1 bug in test tooling, 1 missing robustness)
**Impact on plan:** none on scope; both are internal to the planned files.

## Issues Encountered

- The base of this worktree has no `docs/upstream/*.md` yet (plans 02-16 and 02-17 add them in parallel), so the real-repository check ran with base `HEAD~1` and printed "No upstream records affected."; matching is proven by the fixture records, which follow the front-matter shape of the 02-15 template. Plans 02-16 and 02-17 must use product keys the advisory understands (a catalog `[versions]` key, a mise tool key, `gradle`, `java`, `node` or `action:<owner>/<repo>`).
- `ci.yml` and `scripts/` are governed paths, so the pull request needs the `governance-approved` label (not a code concern of this plan).

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None - the only new surface is the read-only job already covered by T-02-42 and T-02-43.

## Next Phase Readiness

- The advisory is complete; once 02-16 and 02-17 land, the first pull request that bumps `axon`, a mise tool or an action shows the matching records in its job summary.
- Open item for the first pull request: confirm the summary renders and `BASE_SHA` resolves on both `pull_request` and `push` runs.

## Self-Check: PASSED

- `scripts/upstream/changed-dependencies.sh`, `scripts/upstream/test-changed-dependencies.sh` and the `upstream-records` job in `.github/workflows/ci.yml` exist.
- Commits `6aa102d` and `c21f43f` are ancestors of HEAD.
- `bash scripts/upstream/test-changed-dependencies.sh`: 62 passed, 0 failed; shellcheck and shfmt clean; actionlint and zizmor report no findings.
- Acceptance criteria: `%0A` count 1, `injected` count 3, `needs` line count 1, `run: .*github.event` count 0, real-repository run with `HEAD~1` exits 0.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
