---
phase: 01-repository-build-ci-foundation
reviewed: 2026-11-21T00:00:00Z
depth: standard
files_reviewed: 10
files_reviewed_list:
  - .claude/settings.json
  - .github/workflows/governance.yml
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPathsFormatFunctionalTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPaths.kt
  - governance/governed-paths.txt
  - scripts/governance/check-pr.sh
  - scripts/governance/list-pr-files.sh
  - scripts/governance/test-check-pr.sh
  - scripts/governance/test-list-pr-files.sh
findings:
  critical: 0
  warning: 4
  info: 6
  total: 10
  necessity: 1
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-11-21
**Depth:** standard
**Files Reviewed:** 10 (incremental re-review after gap plans 01-15, 01-16 and 01-17; the 76-file scope of the first pass is not re-read)
**Status:** issues_found

## Summary

This is the re-review after gap closure. It rewrites the phase's single review record. The four findings the gap plans claimed to fix (CR-01, WR-01, WR-03, WR-04) were checked against the code, and each one is resolved; the evidence is in the next section. Both shell test suites were run in this review: `test-list-pr-files.sh` passes 25 of 25 and `test-check-pr.sh` passes 80 of 80. The governed-paths grammar was compared line by line between `load_globs` (bash) and `GovernedPaths.parse` (Kotlin): leading and trailing strip set, comment syntax, printable-ASCII rule, LF-only splitting, NUL handling, BOM, and line numbering all agree. The 29 `Edit(...)` ask rules in `.claude/settings.json` match the 29 entries of `governance/governed-paths.txt` one to one.

The gap changes introduced three new open defects. One is a fail-open path left in `check-pr.sh` (WR-05): the exit status of the `jq` process substitution is never read, so a `jq` failure part-way through the list silently drops the remaining files. I reproduced it. The second is a design gap that the widened boundary makes more visible (WR-06): the gate runs from the pull request's own merge commit, so a PR can edit the gate in the same change that it is meant to be held by. The third is coverage (WR-07): the boundary still omits files that carry the same authority as governed ones (alternate Renovate config names that take precedence over `renovate.json5`, and the tracked `.mise/locks/**` sidecar lockfiles). WR-02 (deferred by the author) and IN-01 to IN-05 are carried forward unchanged. One new Info item (IN-06) concerns invisible characters in a Kotlin test source file.

Decisions recorded in SUMMARYs (the mise trust-policy exclusion, the deferred ruleset, the Renovate automerge rules, the documented accepted residual for build code in `governed-paths.txt`) were not re-litigated. Structural findings (fallow) were not provided. The necessity rubric was read at the first pass.

## Resolution of findings from the previous review

### CR-01: Governance file-list step fails open when `gh api` produces no output - RESOLVED

**Verification:** `.github/workflows/governance.yml:36-42` now calls `scripts/governance/list-pr-files.sh`. The script runs `gh api --paginate` as a plain command whose exit status is read directly (`list-pr-files.sh:61-64`), rejects an empty output file (`:65-66`), combines pages with `jq -s 'add'` and checks its status (`:68-69`), requires a JSON array (`:70-71`), and only then renames a same-directory temp file into place (`:73`). A stale destination is deleted before the call (`:52`), and the EXIT trap removes the temp files (`:31-34`, `:54`). Inputs that reach the API path are validated before any `gh` call (`:38-47`). The workflow also sets `defaults.run.shell: bash`, which gives `governance-success` (`echo "$NEEDS" | jq -e ...`) pipefail as well. `test-list-pr-files.sh` covers the original reproduction (empty output with exit 0), error bodies, broken pagination, truncated JSON, an object page after an array page, a stale destination, temp-file cleanup, and the gate chain; the CI lint job picks it up through the `scripts/*/test-*.sh` glob. The fix is complete. A separate fail-open path inside `check-pr.sh` is reported as WR-05.

### WR-01: Governed paths do not cover the code that enforces governance - RESOLVED

**Verification:** `governance/governed-paths.txt:53-79` now governs `courses.governance.gradle.kts`, the whole `buildlogic/governance/**` package, `scripts/apply-github-settings.sh`, the root and settings convention plugins, the root and `build-logic` build scripts (Kotlin and Groovy spellings), `gradle.properties`, `buildSrc/**`, `gradlew`, `gradle/wrapper/**`, `gradle/gradle-daemon-jvm.properties` and `docs/CODEOWNERS`. `.claude/settings.json` carries the matching 29 ask rules, and `test-check-pr.sh:490-522` pins governed and deliberately ungoverned paths against the real file. The exclusions are argued in the file header (isolated projects, `checkBuildScripts`, Phase 6 for the catalog) and I accept them. The fix covers the scope the finding named. Files outside the boundary that carry the same authority as governed ones are reported as the new WR-07.

### WR-03: A PR with 3000 or more files hides governed files from the gate - RESOLVED

**Verification:** `check-pr.sh:215-222` reads the length with an explicit `|| return 1` and routes any list with `FILES_API_LIMIT` (3000) or more entries to `check_files_api_limit`. That function passes only with the approval label, never consults the Renovate exemption, and returns its status through `return` (`:199-212`). The boundary values 2999, 3000 and 3001, the label, and a Renovate author are all tested (`test-check-pr.sh:358-375`). Complete.

### WR-04: The governed-paths file is parsed differently by the shell gate and the Gradle check - RESOLVED

**Verification:** One grammar is now implemented twice and documented in both places (`check-pr.sh:78-87`, `GovernedPaths.kt:4-20`, `governed-paths.txt:8-11`). Strip set is space, tab and carriage return only; lines starting with `#` and empty lines are ignored; every other entry must be printable ASCII; a NUL byte is turned into `\001` before `read` so it cannot be dropped; every offending line is reported in ascending order; an empty entry list fails closed. `AskRulesCheck.check()` rejects a file with problems or no entries before comparing rules. The twin case tables G1 to G9 (bash) and K1 to K9 (Kotlin) cover padding, CRLF, comments, inner space, non-breaking space, line order, comment-only files, BOM, NUL and inner CR. I found no remaining divergence. The test twins are not machine-linked to each other (see IN-06 for a related fragility).

## Warnings

### WR-02: The approval label is sticky and is not bound to a commit

_Carried from the previous review, unchanged; deferred by the author. The code is now at `scripts/governance/check-pr.sh:201-205` and `:266-269`._

**File:** `scripts/governance/check-pr.sh:266-269`
**Issue:** Once `governance-approved` is on the PR, every later push passes. The check never learns which commit the maintainer approved, or who applied the label. The ruleset also has `dismiss_stale_reviews_on_push: false` and zero required reviews (`.github/rulesets/main.json`), so nothing else invalidates the approval. A PR approved at commit A can gain further governed changes at commit B and still pass. The wider governed boundary from plan 01-17 makes more files subject to this.
**Fix:** Bind approval to the head SHA, for example by removing the label in a workflow step on `synchronize`. This needs `issues: write` or `pull-requests: write` and is only safe on same-repository PRs. Alternatively, make the label step record `github.event.pull_request.head.sha` and require the label event to be newer than the last push. At minimum, document the accepted residual risk in ADR-0001.

### WR-05: `check-pr.sh` ignores the exit status of the `jq` process substitution, so a mid-stream `jq` failure drops files and the gate passes

**File:** `scripts/governance/check-pr.sh:226-260`
**Issue:** The per-file loop reads from `< <(jq --raw-output0 ...)`. The status of a process substitution is not observable by the shell, and `set -e` does not see it. If `jq` aborts after printing part of the stream, `read` hits EOF, the `while ... && ...` condition fails, the loop ends quietly, and every entry from the failing one onward is never evaluated. `touched` stays empty and the script prints "no governed file is touched" and returns 0. This is the same class of defect as CR-01: a failing producer read as an empty result.

`validate_inputs` checks the types of `.filename` and the array shape, but not the other fields. I reproduced the failure: a list with a harmless first entry and a second entry `.github/workflows/ci.yml` with `"patch": "...\u0000..."` makes `jq` print `Cannot dump a string containing NUL with --raw-output0 option` and the script exits 0 with no label (checked with `echo exit=$?`). A real GitHub response is unlikely to contain a NUL in a patch, so the likelihood is low, but the fail-open shape is real and the line 217 comment shows the author already guards against the neighbouring `errexit` pitfall.
**Fix:** Do not consume the stream through a process substitution whose status is lost. Write the records to a temp file and check `jq`'s status before looping, and also check that the loop saw every entry:
```bash
records=$(mktemp)
trap 'rm -f -- "$records"' RETURN
jq --raw-output0 '...' "$PR_FILES_JSON" >"$records" || return 1
...
done <"$records"
[[ $seen -eq $count ]] || return 1   # seen is incremented once per loop iteration
```
Alternatively reject a NUL in any string up front in `validate_inputs` (`jq -e '[.. | strings | contains("\u0000")] | any | not'`) and assert `seen == count`. Add a test case with a NUL in the patch of a governed entry that expects exit 1.

### WR-06: The gate runs from the pull request's own merge commit, so a PR can edit the gate that is meant to hold it

**File:** `.github/workflows/governance.yml:8-10,32-34,42,52`
**Issue:** The workflow triggers on `pull_request` and checks out the default ref, which for that event is the merge of the PR head into the base. `scripts/governance/list-pr-files.sh`, `scripts/governance/check-pr.sh` and `governance/governed-paths.txt` are therefore read from the PR's own content, and the workflow definition itself is also the PR's version. A PR that wants to change a governed file can, in the same PR, make `check-pr.sh` end with `exit 0`, empty the entries in `governed-paths.txt` (an empty list fails closed, but a list that governs only `nothing/**` does not), or rename the job. The `governance-success` context is then green without the label. The research doc (01-RESEARCH.md, "no checkout of PR code") assumed the gate would not execute PR code; the implementation checks the PR out and runs it.

The ruleset requires zero approving reviews and no code-owner review, and no `CODEOWNERS` file exists, so nothing else holds such a PR. This is not a new line of code, but plan 01-17 now declares the gate's own scripts, the Gradle checks and the launcher as protected while the protection is evaluated by the code under change. It is a design-level bypass of the maintainer-approval contract, so ADR-0001 should at least record it.
**Fix:** Evaluate the gate from trusted content. Options, in order of strength:
1. Switch to `pull_request_target` with a checkout of the base only (`ref: ${{ github.event.pull_request.base.sha }}`), run the scripts from that checkout, and never check out or execute PR content. The files API input already works without PR code. The trigger types stay the same.
2. Keep `pull_request` but check out the base ref for `scripts/governance/**` and `governance/governed-paths.txt` (sparse second checkout into a separate directory) so the script and the list cannot be changed by the PR. The workflow file is still the PR's.
3. Add a real `CODEOWNERS` file and enable `require_code_owner_review` for the governed paths, so a human gate backs up the label.
If the residual is accepted for a one-maintainer repository, say so in ADR-0001 together with the reason.

### WR-07: The governed boundary omits files that carry the same authority as governed ones

**File:** `governance/governed-paths.txt:41-44` (and the tracked `.mise/locks/**`)
**Issue:** The boundary rule in the file header covers "tool pins and their update policy", but two groups of tracked or precedence-winning files are not listed:
- **Alternate Renovate config names.** Renovate reads exactly one config file, the first found in its search order, which starts with `renovate.json` and only then `renovate.json5`. A new root `renovate.json` (or `.renovaterc`, `.renovaterc.json`, `.renovaterc.json5`, lower in the order) takes effect over, or instead of, the governed `renovate.json5` without matching any glob. A PR adding `renovate.json` with `automerge: true` rules would pass the gate unlabelled. `.github/renovate.json5` is already covered by `.github/**`.
- **mise sidecar lockfiles.** `.mise/locks/npm-renovate/.../aube-lock.yaml`, `package.json` and `.mise/locks/pipx-reuse/.../uv.lock`, `pyproject.toml` are tracked (`git ls-files .mise` lists four files). They pin the transitive dependency trees of `npm:renovate` and `pipx:reuse` that CI installs, so they are tool-pin integrity artifacts of the same kind as `mise.lock`, and `renovate.json5` explicitly disables Renovate for them, so only a human edits them. A change to a hash or version in `uv.lock` changes what CI installs without touching `mise.toml` or `mise.lock`.
- Likely also relevant: other file names mise merges over `mise.toml` (`mise.local.toml`, `.mise.toml`, `.tool-versions`, `.config/mise*`). Whether CI would trust and load them depends on the `jdx/mise-action` trust handling, which I did not verify here, so treat this item as something to confirm.
**Fix:** Add the entries and the matching `Edit(/...)` ask rules (`checkClaudeAskRules` enforces the pairing):
```
renovate.json
.renovaterc
.renovaterc.json
.renovaterc.json5
.mise/locks/**
```
Decide on the mise alternates after checking how CI trusts config files. Note the cost of governing `.mise/locks/**`: a Renovate patch bump of `npm:renovate` or `pipx:reuse` creates a new versioned directory (added files, no patch for a lockfile of that kind to normalise), so it would no longer take the version-only exemption and would need the label. That follows from the file's role, so accept it or extend the exemption deliberately.

## Info

### IN-01: Branch-name rule rejects the GSD milestone branches that the pinned policy declares

_Carried from the previous review, unchanged. The pattern is now at `scripts/governance/check-pr.sh:28`._

**File:** `scripts/governance/check-pr.sh:28`
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

_Carried from the previous review, unchanged. It now also applies to `list-pr-files.sh`, which uses the runner's `gh` and `jq` the same way._

**File:** `.github/workflows/governance.yml:25-46`, `docs/tooling.md:80`
**Issue:** The governance job runs `scripts/governance/check-pr.sh` with the runner image's `jq` and `gh`, not the versions pinned in `mise.toml` (jq 1.8.2, gh 2.104.0). The script needs `jq --raw-output0`, which exists only in jq 1.7 or later. A runner image change could break or alter the gate. It fails closed in the missing-feature case, but behaviour can differ between the pinned and the runner jq. The shell tests in the `lint` job run with the same unpinned jq.
**Fix:** Install `jq` and `gh` through the pinned `mise` setup in the governance job. Alternatively, have the script assert a minimum `jq` version at the start.

### IN-06: Invisible characters are written literally in the Kotlin test source

**File:** `build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/GovernedPathsFormatFunctionalTest.kt:55,84`
**Issue:** K4 relies on a literal non-breaking space at the start of the second line of `"mise.toml\n .mcp.json\n"`, and K7 on a literal U+FEFF at the start of `"\uFEFFmise.toml\n"`. Neither is visible in an editor or a diff. If a tool or a contributor normalises the NBSP to an ordinary space, K4 changes meaning: the padded entry is stripped and valid, the build passes, and the test fails with a confusing message that does not mention whitespace. The bash twins already spell these as `\xc2\xa0` and `\xef\xbb\xbf` escapes (`test-check-pr.sh:435,450`).
**Fix:** Spell them as Kotlin escapes, `"mise.toml\n\u00A0.mcp.json\n"` and `"\uFEFFmise.toml\n"`, so the intent is readable and survives reformatting.

---

_Reviewed: 2026-11-21_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
