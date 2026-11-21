---
phase: 01-repository-build-ci-foundation
reviewed: 2026-11-21T00:00:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - .claude/settings.json
  - docs/adr/0001-workflow-permissions-and-required-checks.md
  - docs/adr/0003-governance-gate-trust-boundary.md
  - governance/governed-paths.txt
  - scripts/governance/check-pr.sh
  - scripts/governance/test-check-pr.sh
findings:
  critical: 0
  warning: 2
  info: 7
  total: 9
  necessity: 1
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-11-21
**Depth:** standard
**Files Reviewed:** 6 (incremental re-review after gap round 2, plans 01-18, 01-19 and 01-20; scope is the diff since commit 4b7b594, the rest is not re-read)
**Status:** issues_found

## Summary

This is the re-review after gap round 2. It rewrites the phase's single review record. WR-05 and WR-07 are claimed fixed and both checked out against the code: they are RESOLVED. WR-06 is recorded as an accepted residual in ADR-0003 by the author's decision. The ADR states the facts about the ruleset, the trigger, the missing CODEOWNERS and the rejected alternatives accurately, so WR-06 is marked ACCEPTED-RESIDUAL (deferred), not a defect. One of the ADR's three reasons overstates a control (see WR-08). CR-01, WR-01, WR-03 and WR-04 stay RESOLVED. WR-02 (deferred) and IN-01 to IN-06 are carried forward unchanged.

Verification run in this review: `scripts/governance/test-check-pr.sh` passes 122 of 122 under both the default `bash` and `/bin/bash`, which on this machine is bash 3.2.57 (jq 1.7.1). The 54 entries of `governance/governed-paths.txt` and the 54 `Edit(/...)` ask rules in `.claude/settings.json` were compared programmatically: same set, same order. No new code defect was found in `check-pr.sh`: every `jq` status is read explicitly, the records file is created by `mktemp`, removed by an EXIT trap, and the completeness guard (`seen` against `count`) is independent of `jq`'s status. One new Warning (WR-08, documentation accuracy in ADR-0003) and one new Info (IN-07, governed-list granularity) were found.

Decisions recorded in SUMMARYs (the mise trust-policy exclusion, the deferred ruleset, the Renovate automerge rules, the documented accepted residual for build code in `governed-paths.txt`) were not re-litigated. Structural findings (fallow) were not provided. The necessity rubric was read at the first pass.

## Resolution of findings from earlier reviews

### CR-01: Governance file-list step fails open when `gh api` produces no output - RESOLVED

Resolved by plan 01-15 and verified at the previous review (`list-pr-files.sh` reads the `gh` status directly, rejects empty and non-array output, renames a temp file into place). Not in this round's scope; unchanged.

### WR-01: Governed paths do not cover the code that enforces governance - RESOLVED

Resolved by plan 01-17 and verified at the previous review. The list grew in plan 01-19; the boundary rule in the file header still holds for every entry.

### WR-03: A PR with 3000 or more files hides governed files from the gate - RESOLVED

Resolved by plan 01-15 (`check_files_api_limit`, `FILES_API_LIMIT`). `check-pr.sh:228-231` is unchanged by the gap round and the boundary cases are still tested.

### WR-04: The governed-paths file is parsed differently by the shell gate and the Gradle check - RESOLVED

Resolved by plan 01-16. The grammar in `load_globs` (`check-pr.sh:87-122`) is unchanged in this round; `AskRulesCheck` still parses the file through `GovernedPaths.parse`, and the bash and Kotlin case tables are unchanged.

### WR-05: `check-pr.sh` ignores the exit status of the `jq` process substitution - RESOLVED (plan 01-18)

**Verification:** `check-pr.sh:233-247` writes the per-file records to a `mktemp` file and reads `jq`'s status with `|| jq_status=$?` before any entry is evaluated; a non-zero status returns 1 with a message. Errexit is suspended in this function (called from an `||` list in `main`), and the code does not rely on it: every status is read explicitly (`mktemp ... || return 1`, `count=$(jq ...) || return 1`). A second, independent guard (`:285-291`) compares the number of records read (`seen`) with the array length (`count`) and fails when they differ, which covers a producer that exits 0 but writes fewer records or a torn record.

Temp-file handling is sound. `RECORDS_FILE` is a script-level global initialised to `''` before the trap is installed, so `set -u` is satisfied, a failed `mktemp` leaves it empty (nothing to remove) and the EXIT trap, installed in `main` before the first `die`, removes the file on every exit path, including `exit 1` from `load_globs`. `mktemp` creates the file with mode 0600 and a random suffix. The `TMPDIR` fallback and the template work with GNU and BSD `mktemp`. Bash 3.2 behaviour is correct: `read -d ''`, the empty-array expansions (`"${exempt[@]}"` and `"${blocked[@]}"` are only reached non-empty, `"${offending[@]}"` and `"${GLOBS[@]}"` are guarded) and `${#touched[@]}` all work under `set -u`, and the suite passes on 3.2.57.

The tests prove each guard separately: R1 to R6 use a real NUL in a patch, a filename and a `previous_filename` (fails on `jq`'s status), R7 uses a fake `jq` that exits non-zero after writing every record, R8 and R9 use a fake `jq` that exits 0 with fewer or torn records (fail on the completeness guard alone), and R10 and R11 prove through a logged directory listing that the records file existed during the run and is gone afterwards on both a failing and a passing run. The label and the Renovate exemption never rescue an unreadable list (R5, R6). Complete.

### WR-07: The governed boundary omits files that carry the same authority as governed ones - RESOLVED (plan 01-19)

**Verification:** `governance/governed-paths.txt` now lists `renovate.json`, `renovate.jsonc`, `.gitlab/renovate.{json,jsonc,json5}`, `.renovaterc`, `.renovaterc.{json,jsonc,json5}` and `package.json` next to `renovate.json5` (`.github/renovate.*` stays covered by `.github/**`), the `.mise/locks/**` sidecar lockfiles, and every mise config and rc name mise reads from the repository root (`.mise.toml`, `mise.local.toml`, `.mise.local.toml`, `mise/**`, `.mise/config.toml`, `.mise/config.local.toml`, `.mise/conf.d/**`, `.config/mise.toml`, `.config/mise.local.toml`, `.config/mise/**`, `.tool-versions`, `.miserc.toml`, `.miserc.local.toml`, `.config/miserc.toml`). The new exclusions (environment files, lockfiles of alternate configs, nested configs, `.java-version` and `.nvmrc`, nested `package.json`) are argued in the header and are consistent with how CI runs mise: `ci.yml` sets `MISE_DISABLE_TOOLS: java,node`, runs `jdx/mise-action` from the repository root, and neither the workflows, `mise.toml` nor the scripts use mise tasks, so no task directory needs governing.

Lockstep and anchoring are correct. The 54 entries and the 54 `Edit(/...)` ask rules are identical in content and order, and `AskRulesCheck` enforces the pairing. `glob_match` anchors a plain name by exact comparison and a `dir/**` entry by prefix, so `.github/**` does not match a nested `x/.github/...`, and `mise/**` does not match `mise.toml` (which has its own entry). `test-check-pr.sh` pins 25 new governed and 4 deliberately ungoverned paths against the real file, an added `renovate.json` without the label (fails), and a Renovate-added sidecar file without the label (fails). The accepted cost of governing `.mise/locks/**` is stated in the file and tested: sidecar changes arrive as added, removed or renamed files, which the version-only exemption never covers (it requires status `modified` and no `previous_filename`). In practice the cost is smaller than the file suggests, because `renovate.json5` disables Renovate for `.mise/locks/**`, so those files change only through a human or a `mise lock` run. Complete; the only residue is IN-07.

### WR-06: The gate runs from the pull request's own merge commit - ACCEPTED-RESIDUAL (deferred, not a defect)

Recorded by the author in `docs/adr/0003-governance-gate-trust-boundary.md` and cross-linked from ADR-0001 (the link target `0003-governance-gate-trust-boundary.md` resolves in the same directory) and from the disposition file. The ADR was checked against the repository:

- **Trigger and checkout:** correct. `governance.yml` triggers on `pull_request` only (types opened, synchronize, reopened, labeled, unlabeled, edited), the checkout uses the default ref (the PR merge ref), and `list-pr-files.sh`, `check-pr.sh`, `governed-paths.txt` and the workflow come from the PR tree.
- **Bypass routes:** correct. A gate that exits 0, a list narrowed to an unused entry (an empty list fails closed, which `load_globs` implements and the ADR states correctly), and a workflow that passes the aggregator without running the check are all possible.
- **Ruleset facts:** correct against `.github/rulesets/main.json`: pull request required, squash only, `required_approving_review_count: 0`, `require_code_owner_review: false`, `dismiss_stale_reviews_on_push: false`, two required contexts. The ADR correctly calls the ruleset declared and applied later.
- **CODEOWNERS:** correct. None of `CODEOWNERS`, `docs/CODEOWNERS` or `.github/CODEOWNERS` exists.
- **Rejected alternatives:** accurate. `pull_request_target` runs in the base context with secrets and a token that can write, and zizmor's `dangerous-triggers` audit reports it. A sparse base checkout moves the hole because the workflow file still comes from the PR. Required code-owner review with one maintainer cannot work, because GitHub does not count an author's approval of their own pull request, so every governed change would need the admin bypass (the ruleset's one bypass actor is the Repository admin role with `bypass_mode: pull_request`).
- **Provenance:** T-01-20 does exist in plan 01-09 with the ASVS L1 acceptance the ADR cites.
- **Scope statement:** the ADR says plainly that the gate is not a security boundary against a crafted pull request. That is an accurate statement of the risk. One supporting reason is stated too strongly (WR-08).

## Warnings

### WR-02: The approval label is sticky and is not bound to a commit

_Carried from the previous review, unchanged; deferred by the author. The code is at `scripts/governance/check-pr.sh:297-300`._

**File:** `scripts/governance/check-pr.sh:297-300`
**Issue:** Once `governance-approved` is on the PR, every later push passes. The check never learns which commit the maintainer approved, or who applied the label. The ruleset also has `dismiss_stale_reviews_on_push: false` and zero required reviews (`.github/rulesets/main.json`), so nothing else invalidates the approval. A PR approved at commit A can gain further governed changes at commit B and still pass. The wider governed boundary from plans 01-17 and 01-19 makes more files subject to this.
**Fix:** Bind approval to the head SHA, for example by removing the label in a workflow step on `synchronize`. This needs `issues: write` or `pull-requests: write` and is only safe on same-repository PRs. Alternatively, make the label step record `github.event.pull_request.head.sha` and require the label event to be newer than the last push. At minimum, document the accepted residual risk in ADR-0001 (ADR-0003 now lists it as a separate deferred residual).

### WR-08: ADR-0003 relies on Claude Code ask rules as a control over the agent, which the project itself says is not a security boundary

**File:** `docs/adr/0003-governance-gate-trust-boundary.md:51-53`
**Issue:** Reason 2 says the ask rules "cover every governed path ... so the project's agent cannot change the gate without the author confirming the edit (AIDEV-08)". That overstates what the control does. `.claude/settings.json` holds only `Edit(/...)` ask rules (plus `Bash(*config-set*)`); there is no ask or deny rule on shell commands, so a `sed -i`, redirect, `git apply` or `cp` through the Bash tool changes a governed file without matching any rule. Edit rules also do not apply in a permission mode that skips prompts. The project's own code says the same: the KDoc of `AskRulesCheck` states "Ask rules are a convenience for the author, not a security boundary". The ADR's Decision paragraph hedges correctly ("an agent that does not tamper with the gate"), but Reason 2 and the Consequences section read as if the agent route were closed. The ADR is the record a later reader uses to judge the residual, and on the agent path it rates the residual lower than it is. The decision itself stands: the maintainer's diff review is the real control either way.
**Fix:** Reword Reason 2 to match the Decision paragraph, for example: "Claude Code's ask rules cover every governed path for the editing tools, so an agent that edits files with the Edit and Write tools has to ask the author first. The rules do not cover shell commands and are a convenience, not a security boundary (`AskRulesCheck`), so the diff review in Consequences stays the control." Optionally add the same sentence to the `governed-paths.txt` header.

## Info

### IN-01: Branch-name rule rejects the GSD milestone branches that the pinned policy declares

_Carried from the previous review, unchanged. The pattern is now at `scripts/governance/check-pr.sh:29`._

**File:** `scripts/governance/check-pr.sh:29`
**Issue:** `governance/gsd-config-policy.json` pins `milestone_branch_template: "gsd/{milestone}-{slug}"`. `PHASE_BRANCH_PATTERN` only allows `gsd/phase-...`, so a PR from a milestone branch fails rule 2. This is unreachable while `branching_strategy` stays `phase`, but the two files disagree.
**Fix:** Either allow the milestone shape in the pattern, or note in the script header that milestone branching is unsupported. A single shared source for the branch templates is better.

### IN-02: `ToolingTableCheck` cannot see `[tools.<name>]` sub-tables or multi-line inline tables

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt:79-90`
**Issue:** Any line starting with `[` sets `inTools` to false, and `TOOLS_HEADER` matches only the exact `[tools]`. A tool pinned as `[tools."npm:foo"]` or `[tools.foo]` is invisible to the inventory check. With TOML 1.1 multi-line inline tables, continuation lines such as `version = "..."` would be reported as phantom tools. Today every entry is a single-line key, so nothing is wrong yet.
**Fix:** Match `^\[tools(\.|])` headers and derive the key from the header, or parse with a TOML library. Alternatively add a test that pins the "single-line keys only" assumption and fails loudly on a `[tools.` header.

### IN-03: Duplicated logic

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `.github/workflows/ci.yml:44-49,98-103,161-166` and `build-logic/src/main/kotlin/courses.root.gradle.kts:198-206` vs `courses.kotlin-library.gradle.kts:26-37`
**Issue:** The "Read the mise version" step is copy-pasted into three jobs. The `.java-version` to `JavaLanguageVersion` parsing, including the error message, is duplicated in two convention plugins.
**Fix:** Move the mise-version read into a local composite action, or read it once in a job and pass it on. Put the JDK major parse into a shared build-logic helper.

### IN-04: Redundant or not-yet-instantiated attribute entries

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `.gitattributes:5-6,31,34-35` and `.editorconfig:46-48`
**Category:** necessity
**Issue:** `gradlew text eol=lf` and `*.sh text eol=lf` repeat what `* text=auto eol=lf` on line 2 already sets, and the `.editorconfig` `[gradlew] end_of_line = lf` repeats the `[*]` default. `frontend/**/generated/**`, `pnpm-lock.yaml`, `docs-site/**` and `gradle/verification-metadata.xml` match no tracked file in this repository. This is provable from the repository, but the entries are cheap and anticipatory, so it rests on judgment.
**Fix:** Keep them if they are deliberate forward declarations. Otherwise drop the redundant `gradlew` and `*.sh` rules, and add the future-path rules in the phase that creates those paths.

### IN-05: The governance gate and shell tests depend on unpinned runner tools

_Carried from the previous review, unchanged. It also applies to `list-pr-files.sh`, and to the `--raw-output0` records stream that WR-05's fix still depends on (it needs jq 1.7)._

**File:** `.github/workflows/governance.yml:25-46`, `docs/tooling.md:80`
**Issue:** The governance job runs `scripts/governance/check-pr.sh` with the runner image's `jq` and `gh`, not the versions pinned in `mise.toml` (jq 1.8.2, gh 2.104.0). The script needs `jq --raw-output0`, which exists only in jq 1.7 or later. A runner image change could break or alter the gate. It fails closed in the missing-feature case, but behaviour can differ between the pinned and the runner jq. The shell tests in the `lint` job run with the same unpinned jq.
**Fix:** Install `jq` and `gh` through the pinned `mise` setup in the governance job. Alternatively, have the script assert a minimum `jq` version at the start.

### IN-06: Invisible characters are written literally in the Kotlin test source

_Carried from the previous review, unchanged; not in the incremental scope._

**File:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPathsFormatFunctionalTest.kt:55,84`
**Issue:** K4 relies on a literal non-breaking space at the start of the second line of `"mise.toml\n .mcp.json\n"`, and K7 on a literal U+FEFF at the start of `"<U+FEFF>mise.toml\n"`. Neither is visible in an editor or a diff. If a tool or a contributor normalises the NBSP to an ordinary space, K4 changes meaning: the padded entry is stripped and valid, the build passes, and the test fails with a confusing message that does not mention whitespace. The bash twins already spell these as `\xc2\xa0` and `\xef\xbb\xbf` escapes.
**Fix:** Spell them as Kotlin escapes, `"mise.toml\n<U+00A0>.mcp.json\n"` and `"<U+FEFF>mise.toml\n"`, so the intent is readable and survives reformatting.

### IN-07: `package.json` is governed as a whole file for the sake of a deprecated `renovate` key

**File:** `governance/governed-paths.txt:92-104`
**Issue:** The header's boundary rule governs a path when a change to it alone can rewrite the policy, and the comment itself says `package.json` counts "only through a deprecated `renovate` section". The entry governs the whole root file. No root `package.json` exists today, but the project's stack decision takes the pnpm version from the root `packageManager` field, so one will exist once the frontend lands, and it will then carry scripts, dev dependencies and the `packageManager` pin. Every human edit to it will need the label. A Renovate bump of the `packageManager` pin (`pnpm@x.y.z+sha512.<128 hex>`) will also not count as version-only: `normalize` collapses 64-hex and 40-hex runs, not a 128-hex sha512, so the removed and added lines differ after normalisation. This is a judgment call about future friction, not a bug today, and the protection is real: the root file would otherwise be a way to switch Renovate config on.
**Fix:** When the root `package.json` is created, decide deliberately: keep it governed and extend `normalize` with a `[0-9a-f]{128}` digest rule, or drop the entry and add a check that rejects a `renovate` key in `package.json`. Until then, note the expected friction next to the entry.

---

_Reviewed: 2026-11-21_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
