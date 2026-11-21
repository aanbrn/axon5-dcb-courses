---
phase: 01-repository-build-ci-foundation
reviewed: 2026-11-21T00:00:00Z
depth: standard
files_reviewed: 76
files_reviewed_list:
  - .claude/settings.json
  - .editorconfig
  - .editorconfig-checker.json
  - .gitattributes
  - .github/PULL_REQUEST_TEMPLATE.md
  - .github/labels.yml
  - .github/repository-settings.json
  - .github/rulesets/main.json
  - .github/workflows/ci.yml
  - .github/workflows/codeql.yml
  - .github/workflows/governance.yml
  - .github/workflows/labels.yml
  - .github/workflows/scorecard.yml
  - .github/workflows/settings-drift.yml
  - .gitignore
  - .java-version
  - .nvmrc
  - .prettierrc.yaml
  - .taplo.toml
  - README.md
  - REUSE.toml
  - build-logic/build.gradle.kts
  - build-logic/settings.gradle.kts
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/ConventionsFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/QualityGatesFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheckInputShapeFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/BuildScriptLintCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DaemonJvmCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheckFunctionalTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/courses.governance.gradle.kts
  - build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
  - build-logic/src/main/kotlin/courses.root.gradle.kts
  - build-logic/src/main/kotlin/courses.settings.settings.gradle.kts
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyFormatterBinary.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/BuildScriptLintCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/DaemonJvmConsistencyCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/PrTemplateCheck.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt
  - build.gradle.kts
  - config/detekt/detekt.yml
  - config/spotless/license-header-shell.txt
  - config/spotless/license-header.txt
  - docs/adr/0001-workflow-permissions-and-required-checks.md
  - docs/adr/0002-codeql-kotlin-analysis.md
  - docs/performance.md
  - docs/tooling.md
  - governance/governed-paths.txt
  - governance/gsd-config-policy.json
  - gradle.properties
  - gradle/gradle-daemon-jvm.properties
  - gradle/libs.versions.toml
  - gradle/wrapper/gradle-wrapper.properties
  - gradlew
  - gradlew.bat
  - lefthook.yml
  - mise.toml
  - performance.scenarios
  - platform/build.gradle.kts
  - renovate.json5
  - scripts/apply-github-settings.sh
  - scripts/format-staged.sh
  - scripts/governance/check-pr.sh
  - scripts/governance/test-check-pr.sh
  - scripts/test-apply-github-settings.sh
  - settings.gradle.kts
  - tooling/convention-sample/build.gradle.kts
  - tooling/convention-sample/src/main/kotlin/io/github/aanbrn/courses/sample/Greeting.kt
  - tooling/convention-sample/src/test/kotlin/io/github/aanbrn/courses/sample/GreetingTest.kt
findings:
  critical: 1
  warning: 4
  info: 5
  total: 10
  necessity: 1
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-11-21
**Depth:** standard
**Files Reviewed:** 76
**Status:** issues_found

## Summary

The phase is of high quality overall. All actions are pinned by SHA, workflow-level `permissions: {}` is applied consistently, and untrusted PR data reaches shell steps through `env:` only. The governance script is carefully written and heavily tested, and the Gradle governance tasks are cacheable and well covered by TestKit tests. No script or workflow injection path was found.

The most important problem is in the governance gate itself. The step that fetches the pull request file list can silently degrade to an empty list. The required `governance-success` check then goes green without ever evaluating the changed files, which is a fail-open path in a gate whose design contract is to fail closed. Four warnings concern the gate's coverage: the governed-path list, the sticky approval label, the 3000-file API cap, and inconsistent parsing of the governed-path list between the shell gate and the Gradle check. The remaining items are Info-level.

Decisions recorded in SUMMARYs (the mise trust-policy exclusion, the deferred ruleset, the detekt test excludes, the Renovate automerge rules) were not re-litigated. Structural findings (fallow) were not provided. The necessity rubric was read.

## Critical Issues

### CR-01: Governance file-list step fails open when `gh api` produces no output

**File:** `.github/workflows/governance.yml:34-36`
**Issue:** The step runs `gh api --paginate ... | jq -s 'add // []' > pr-files.json` with no `shell:` key. GitHub then uses `bash -e {0}`, which does not set `pipefail`. The pipeline status is therefore jq's status, not gh's. Two things combine into a fail-open path:

1. If `gh api` produces no stdout (DNS, TLS or connection failure, timeout, runner network blip), `jq -s` slurps empty input into `[]`.
2. `add` of `[]` is `null`, and `// []` turns that into `[]`.

The step succeeds, `check-pr.sh` reads a valid empty array, prints "no governed file is touched" and exits 0. `governance-success` goes green for a PR that may touch `.github/**`, `mise.toml` and so on, with no label.

HTTP errors that print an error body to stdout are caught later, because `validate_inputs` rejects a non-array. The empty-output case is not caught. The fail-closed tests in `test-check-pr.sh` never cover this step, because they call the script with a hand-made file.

**Fix:** Make the step fail on any `gh` failure and drop the `// []` default. `gh api --paginate` of an array endpoint always emits at least `[]`.
```yaml
- name: List changed files
  shell: bash # adds -o pipefail
  env:
    GH_TOKEN: ${{ github.token }}
    PR_NUMBER: ${{ github.event.pull_request.number }}
    REPO: ${{ github.repository }}
  run: |
    set -euo pipefail
    gh api --paginate "repos/${REPO}/pulls/${PR_NUMBER}/files" | jq -s 'add' > "${RUNNER_TEMP}/pr-files.json"
    jq -e 'type == "array"' "${RUNNER_TEMP}/pr-files.json" > /dev/null
```
Alternatively write the `gh` output to a temp file first, so its exit status is checked directly.

## Warnings

### WR-01: Governed paths do not cover the code that enforces governance

**File:** `governance/governed-paths.txt:1-10`
**Issue:** The list protects `governance/**`, `scripts/governance/**` and `.github/**`. It leaves unprotected the Gradle code that implements the other half of the policy:
- `build-logic/src/main/kotlin/courses.governance.gradle.kts` wires the checks into `check`.
- `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**` holds `ConfigPolicyCheck`, `AskRulesCheck`, `PrTemplateCheck` and the others.
- `scripts/apply-github-settings.sh` is the only writer of the ruleset and repository security settings.

A PR can edit `courses.governance.gradle.kts` to drop `checkGsdConfigPolicy` from `check`, or edit `ConfigPolicyCheck` to always pass. CI stays green and `governance-success` passes with no `governance-approved` label. The Kotlin KDoc itself says CI is the enforcement ("CI (`checkGsdConfigPolicy` and the governance workflow) stays the enforcement"), yet the enforcement code is ungoverned.
**Fix:** Add these globs to `governance/governed-paths.txt`, and add matching `Edit(/...)` ask rules to `.claude/settings.json` (`checkClaudeAskRules` requires them):
```
build-logic/src/main/kotlin/courses.governance.gradle.kts
build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/**
scripts/apply-github-settings.sh
```

### WR-02: The approval label is sticky and is not bound to a commit

**File:** `scripts/governance/check-pr.sh:206-209`
**Issue:** Once `governance-approved` is on the PR, every later push passes. The check never learns which commit the maintainer approved, or who applied the label. The ruleset also has `dismiss_stale_reviews_on_push: false` and zero required reviews (`.github/rulesets/main.json`), so nothing else invalidates the approval. A PR approved at commit A can gain further governed changes at commit B and still pass.
**Fix:** Bind approval to the head SHA, for example by removing the label in a workflow step on `synchronize`. This needs `issues: write` or `pull-requests: write` and is only safe on same-repository PRs. Alternatively, make the label step record `github.event.pull_request.head.sha` and require the label event to be newer than the last push. At minimum, document the accepted residual risk in ADR-0001.

### WR-03: A PR with 3000 or more files hides governed files from the gate

**File:** `.github/workflows/governance.yml:35-36` and `scripts/governance/check-pr.sh:81-84`
**Issue:** The pull request files API returns at most 3000 files and truncates silently. Files come back in path order, and `mise.toml`, `renovate.json5`, `governance/**` and `scripts/governance/**` sort after `build-logic/` and `docs/`. A PR with 3000 or more earlier files can push a governed file out of the list, so the gate sees "no governed file is touched". `validate_inputs` accepts the truncated array.
**Fix:** Fail closed when the list is at the cap. In `validate_inputs`:
```bash
[[ $(jq 'length' "$PR_FILES_JSON") -lt 3000 ]] ||
  die "the files API list is at its 3000-file limit and may be truncated; a maintainer must review"
```
Add a test case for it in `test-check-pr.sh`.

### WR-04: The governed-paths file is parsed differently by the shell gate and the Gradle check

**File:** `scripts/governance/check-pr.sh:74-76` and `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/AskRulesCheck.kt:46`
**Issue:** `AskRulesCheck` trims each line, and `AskRulesCheckFunctionalTest` (the "blank lines and surrounding whitespace ... are ignored" test) deliberately blesses `"  .mcp.json  "`. `check-pr.sh::load_globs` only drops empty lines. It does not trim and it has no comment syntax. A governed entry with leading whitespace is matched literally and never matches a real path, so that file becomes silently ungoverned in CI while the Gradle check still passes. Spotless trims only trailing whitespace on `*.txt`, so leading whitespace is not caught.
**Fix:** Make both parsers identical. In `load_globs`:
```bash
line=${line#"${line%%[![:space:]]*}"}   # ltrim
line=${line%"${line##*[![:space:]]}"}   # rtrim
[[ -n $line ]] && GLOBS+=("$line")
```
Optionally reject lines that start with `#`, in both places. Add a shell test with a padded glob.

## Info

### IN-01: Branch-name rule rejects the GSD milestone branches that the pinned policy declares

**File:** `scripts/governance/check-pr.sh:24-26`
**Issue:** `governance/gsd-config-policy.json` pins `milestone_branch_template: "gsd/{milestone}-{slug}"`. `PHASE_BRANCH_PATTERN` only allows `gsd/phase-...`, so a PR from a milestone branch fails rule 2. This is unreachable while `branching_strategy` stays `phase`, but the two files disagree.
**Fix:** Either allow the milestone shape in the pattern, or note in the script header that milestone branching is unsupported. A single shared source for the branch templates is better.

### IN-02: `ToolingTableCheck` cannot see `[tools.<name>]` sub-tables or multi-line inline tables

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ToolingTableCheck.kt:79-90`
**Issue:** Any line starting with `[` sets `inTools` to false, and `TOOLS_HEADER` matches only the exact `[tools]`. A tool pinned as `[tools."npm:foo"]` or `[tools.foo]` is invisible to the inventory check. With TOML 1.1 multi-line inline tables, continuation lines such as `version = "..."` would be reported as phantom tools. Today every entry is a single-line key, so nothing is wrong yet.
**Fix:** Match `^\[tools(\.|])` headers and derive the key from the header, or parse with a TOML library. Alternatively add a test that pins the "single-line keys only" assumption and fails loudly on a `[tools.` header.

### IN-03: Duplicated logic

**File:** `.github/workflows/ci.yml:44-49,98-103,161-166` and `build-logic/src/main/kotlin/courses.root.gradle.kts:198-206` vs `courses.kotlin-library.gradle.kts:26-37`
**Issue:** The "Read the mise version" step is copy-pasted into three jobs. The `.java-version` to `JavaLanguageVersion` parsing, including the error message, is duplicated in two convention plugins.
**Fix:** Move the mise-version read into a local composite action, or read it once in a job and pass it on. Put the JDK major parse into a shared build-logic helper.

### IN-04: Redundant or not-yet-instantiated attribute entries

**File:** `.gitattributes:5-6,31,34-35` and `.editorconfig:46-48`
**Category:** necessity
**Issue:** `gradlew text eol=lf` and `*.sh text eol=lf` repeat what `* text=auto eol=lf` on line 2 already sets, and the `.editorconfig` `[gradlew] end_of_line = lf` repeats the `[*]` default. `frontend/**/generated/**`, `pnpm-lock.yaml`, `docs-site/**` and `gradle/verification-metadata.xml` match no tracked file in this repository. This is provable from the repository, but the entries are cheap and anticipatory, so it rests on judgment.
**Fix:** Keep them if they are deliberate forward declarations. Otherwise drop the redundant `gradlew` and `*.sh` rules, and add the future-path rules in the phase that creates those paths.

### IN-05: The governance gate and shell tests depend on unpinned runner tools

**File:** `.github/workflows/governance.yml:25-46`, `docs/tooling.md:80`
**Issue:** The governance job runs `scripts/governance/check-pr.sh` with the runner image's `jq` and `gh`, not the versions pinned in `mise.toml` (jq 1.8.2, gh 2.104.0). The script needs `jq --raw-output0`, which exists only in jq 1.7 or later. A runner image change could break or alter the gate. It fails closed in the missing-feature case, but behaviour can differ between the pinned and the runner jq. The shell tests in the `lint` job run with the same unpinned jq.
**Fix:** Install `jq` and `gh` through the pinned `mise` setup in the governance job. Alternatively, have the script assert a minimum `jq` version at the start.

---

_Reviewed: 2026-11-21_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
