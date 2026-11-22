---
status: complete
phase: 01-repository-build-ci-foundation
source: [01-VERIFICATION.md, 01-01-SUMMARY.md, 01-02-SUMMARY.md, 01-03-SUMMARY.md, 01-04-SUMMARY.md, 01-05-SUMMARY.md, 01-06-SUMMARY.md, 01-07-SUMMARY.md, 01-08-SUMMARY.md, 01-09-SUMMARY.md, 01-10-SUMMARY.md, 01-11-SUMMARY.md, 01-12-SUMMARY.md, 01-13-SUMMARY.md, 01-14-SUMMARY.md, 01-15-SUMMARY.md, 01-16-SUMMARY.md, 01-17-SUMMARY.md, 01-18-SUMMARY.md, 01-19-SUMMARY.md, 01-20-SUMMARY.md]
started: 2026-11-21T16:46:16Z
updated: 2026-11-22T02:38:19.070Z
---

## Current Test

[testing complete]

## Tests

### 1. Commit-time hook behaviour in a clone
expected: With the lefthook pre-commit hook installed (`mise install`, `lefthook install`), `git commit` fires it. A badly formatted staged Kotlin file is re-staged formatted; a staged synthetic GitHub token blocks the commit via gitleaks; `reuse lint` checks the whole tree on every commit; actionlint, zizmor and shellcheck run only when matching files are staged
covers: [VERIFICATION human item 5, 01-08 D7, 01-14 D5 (lefthook install)]
result: pass
note: "Exercised by Claude on a detached HEAD in the phase worktree (hook already installed in the shared .git/hooks): format re-staged Bad.kt formatted; gitleaks 'leaks found: 1', exit 1, commit blocked; .sh commit ran shellcheck only; ci.yml commit ran actionlint and zizmor only. Expected text corrected at the author's request: reuse has no glob and lints the whole tree on every commit."

### 2. Spotless cache invalidation on a tool or config change
expected: After a green `./gradlew spotlessCheck`, switching the Node version, or changing the taplo or shfmt version, `.taplo.toml` or `.editorconfig`, makes the next run re-execute the affected Spotless steps instead of reporting them up to date; Node is verified before Prettier runs
covers: [01-04 D4, 01-05 D6]
result: issue
reported: "На поиск exec-ом бинаря по PATH полагаться нельзя. Нужно резолвить PATH, используя System.getenv(\"PATH\"), и самому находить полный путь до бинаря. Потом уже пользоваться exec. Это уже рабочий способ."
severity: major
note: "Exercised by Claude: .editorconfig re-ran spotlessShell+spotlessMisc; .taplo.toml re-ran spotlessToml; taplo/shfmt version change (PATH shim) re-ran spotlessToml+spotlessShell; .nvmrc=22 with Node 24 failed verifyNodeVersion before any Prettier task. With a warm Gradle daemon a Node switch on PATH was NOT seen: verifyNodeVersion's exec(\"node\") resolved the binary from the daemon's startup PATH and still reported Node 24; only --no-daemon made it see v22 and re-run spotlessPrettier. Prettier invalidation is by Node major only (by design)."

### 3. Governed paths and agent ask rules read as intended
expected: Reading `governance/governed-paths.txt` and `.claude/settings.json`, every governed name and every exclusion has accurate, adequate reasoning in the file header; the agent asks before each governed `Edit(/...)` and before `config-set`; nothing silently relaxes governance (no ignored underscore-prefixed keys, no widened patterns); `checkClaudeAskRules` keeps no state beyond its two inputs and one report
covers: [01-19 D3, 01-13 D6, 01-07 D6, 01-16 D4]
result: pass
note: "Claude compiled the summary and checked it mechanically: 54 entries and 54 Edit(/...) ask rules match one to one in the same order, Bash(*config-set*) present; checkClaudeAskRules and checkGsdConfigPolicy green; ConfigPolicyCheck compares key by key with no underscore exemption; checkClaudeAskRules declares two inputs and one report. Live check: a session started from the main checkout (no .claude/settings.json on its branch) was NOT asked, so the rules apply only to sessions launched where the file exists; a fresh session started inside the phase worktree asked before an Edit of governance/governed-paths.txt, and the author declined it. Path in the expected text corrected from scripts/governance/ to governance/."

### 4. Decision records read as intended
expected: ADR-0001 records the D-21 deviation from D-09 (`permissions: {}` and two required contexts); ADR-0003 states the WR-06 residual, why it is accepted, three rejected alternatives with reasons and the revisit triggers; `01-REVIEW-DISPOSITION.md` shows WR-05 fixed, WR-02 deferred and the open count as stated
covers: [01-12 D9, 01-20 D1, 01-18 D6]
result: pass
note: "Claude checked every claim against the repository: all six workflows have top-level permissions {}, 9/9 checkouts persist-credentials false, no unpinned uses; ci-success and governance-success use if: always(), permissions {} and jq all(.result == \"success\"); ruleset requires exactly ci-success and governance-success (integration_id 15368), 0 approvals, no code-owner review, squash only; governance.yml triggers on pull_request with the default checkout ref; no CODEOWNERS anywhere; T-01-20 at ASVS L1 in 01-09-PLAN.md line 194; WR-08 fix commit 5980b3d exists. Disposition: open 7 / total 16 match the rows; open was 8 at plan 01-18 (40f3bb4) and reached 7 via WR-06 deferred, WR-07 fixed, WR-08 added fixed, IN-07 added open."

### 5. Workflow and settings declarations read as intended
expected: `ci.yml` declares build (JDK/Node from version files, mise shfmt/taplo, main-only Gradle cache, optional GRADLE_ENCRYPTION_KEY, no Build Scans) and lint (actionlint, zizmor, shellcheck, editorconfig-checker, reuse lint, normalisation, strict Renovate validation); CodeQL (actions) and Scorecard workflows exist with java-kotlin deferred; `governance.yml` triggers on `pull_request` only under bash with pipefail; the drift workflow is read-only with token skip and narrowed-scope notice; `.github/rulesets/main.json` and the repository settings JSON match the plan's acceptance values
covers: [01-12 D1, 01-12 D2, 01-12 D6, 01-12 D8 (declared part), 01-15 D4, 01-13 D4, 01-13 D1]
result: pass
note: "Claude checked every claim against the files: ci.yml build (setup-java/.java-version, setup-node/.nvmrc, MISE_DISABLE_TOOLS java,node, mise-action installs shfmt and taplo, cache-read-only off main, cache-encryption-key from GRADLE_ENCRYPTION_KEY, no --scan or Develocity anywhere) and lint (actionlint, zizmor, shellcheck, editorconfig-checker, reuse lint, renormalise on an index copy, renovate-config-validator --strict, shell tests); codeql.yml matrix actions only with ADR-0002 for java-kotlin; scorecard.yml publishes and uploads SARIF; governance.yml on pull_request only with defaults.run.shell bash; settings-drift.yml read-only with token-skip notice and narrowed-scope notice; ruleset and repository-settings.json equal the 01-13 Task values field by field."

### 6. Package legitimacy gate was answered before installs
expected: You confirm that you answered the package legitimacy gate before any install in plan 01-03, including the trust-policy exception for `renovate`
covers: [01-03 D5]
result: pass
note: "Author confirmed. The gate (01-03 Task 1, blocking-human) put prettier, renovate, reuse and charset-normalizer (all SUS false positives of the too-new heuristic, no SLOP), brew install mise, and shfmt/taplo for information; the SUMMARY records 'approved by the author'. The libzip trust-policy exception was decided at the blocked `mise install` in Task 2, before renovate was installed, not at the gate itself. The research table said renovate is 'never installed in CI by default'; ci.yml lint now installs npm:renovate for renovate-config-validator --strict, which plan 01-12 planned and test 5 accepted."

### 7. Open the phase PR and let ci.yml and governance.yml run on GitHub
expected: ci-success (build, lint, secrets jobs) and governance-success are green; the PR carries `governance-approved`; the head branch name passes the naming rule; the `List changed files` step prints `governance: listed N changed files`; adding the label re-runs the check and supersedes the earlier run
covers: [VERIFICATION human item 1, 01-12 D1/D2/D6 (hosted run), 01-09 D5]
result: pass
evidence: "PR #2 head ff93f70: CI run 44975742539 Build, Lint, Secret scan and ci-success success (verifyNodeVersion and spotlessCheck ran, BUILD SUCCESSFUL); Governance run 44974801705 success after the label was added, superseding failed run 44974612506; log prints 'governance: listed 160 changed files', 'head branch name gsd/phase-01-execution is allowed' and 'governed files are touched and the pull request carries governance-approved'. The first Lint failure (pipx:reuse needs uv and a mise Python) was fixed in e1a038d."

### 8. Apply the deferred ruleset after both contexts are green
expected: The read-only dry-run lists the expected deltas; `scripts/apply-github-settings.sh --apply --only ruleset` makes ruleset `main` live (required contexts ci-success and governance-success bound to integration_id 15368, squash-only, linear history, no force-push or deletion, admin-only bypass); the printed bypass actor is the admin role; with the drift-check token set and after `gh variable delete SETTINGS_DRIFT_SECTIONS --repo aanbrn/axon5-dcb-courses`, `--check` reports no drift for all sections
covers: [VERIFICATION human item 2, 01-14 D4, 01-13 D3, 01-14 D5 (drift-check token)]
result: pass
evidence: "Dry-run listed 'ruleset main: create' (1 delta); --apply --only ruleset reported 'applied; the live settings match the declared ones' and bypass actor 'RepositoryRole 5 mode=pull_request' (Repository admin); live rules on main: deletion, non_fast_forward, required_linear_history, pull_request, required_status_checks; SETTINGS_READ_TOKEN present, SETTINGS_DRIFT_SECTIONS deleted, --check for ruleset repository security reports 'no drift'; PR #2 stays CLEAN."

### 9. Post-merge CodeQL, Scorecard and first label sync
expected: Code scanning shows the CodeQL (actions) and Scorecard analyses from main; the first label sync creates, updates and deletes labels so the repository equals `.github/labels.yml`, and a re-run on an unchanged file changes nothing
covers: [VERIFICATION human item 3, 01-10 D3, 01-12 D8 (hosted run)]
result: skipped
reason: "Deferred follow-up: post-merge check; CodeQL, Scorecard and the label sync run only from main after PR #2 is merged (author decision 2026-11-22)"

### 10. Close Renovate onboarding PR #1 and check the first grouped PRs
expected: No onboarding PR; the Mend app opens update PRs grouped per `renovate.json5` with the configured labels, none before the 7-day cooldown (except the documented vulnerability exception and the JDK timestamp-optional waiver), automerge only for the three allowed rules; `npm:renovate`/`pipx:reuse` bumps that rewrite a `.mise/locks/**` sidecar need `governance-approved`
covers: [VERIFICATION human item 4, 01-11 D7, 01-14 D5 (Renovate app)]
result: skipped
reason: "Deferred follow-up: post-merge check; Renovate reads renovate.json5 only from main, so grouped PRs appear after PR #2 is merged (author decision 2026-11-22)"

### 11. Register the repository at api.reuse.software
expected: The README REUSE badge resolves and shows `compliant`
covers: [VERIFICATION human item 6, 01-06 D5, 01-14 D5 (REUSE registration)]
result: skipped
reason: "Deferred follow-up: post-merge check; REUSE registration lints the default branch, so it needs PR #2 merged (author decision 2026-11-22)"

### 12. [01-01 D1] ./gradlew build compiles and tests the sample module through build-logic, :platform and the catalog; second run reuses the configuration cache with isolated projects on
expected: ./gradlew build compiles and tests the sample module through build-logic, :platform and the catalog; second run reuses the configuration cache with isolated projects on
result: pass
source: automated
coverage_id: D1

### 13. [01-01 D2] Build performance settings: configuration cache (problems fail), build cache, parallel execution, isolated projects, file-system watching, JVM args
expected: Build performance settings: configuration cache (problems fail), build cache, parallel execution, isolated projects, file-system watching, JVM args
result: pass
source: automated
coverage_id: D2

### 14. [01-01 D3] Single version source: catalog for libraries, plugins and tool versions; .java-version for the JDK; wrapper pinned by SHA-256
expected: Single version source: catalog for libraries, plugins and tool versions; .java-version for the JDK; wrapper pinned by SHA-256
result: pass
source: automated
coverage_id: D3

### 15. [01-01 D4] :platform imports eight BOMs through catalog aliases, and the catalog Kotlin overrides Spring Boot's managed Kotlin
expected: :platform imports eight BOMs through catalog aliases, and the catalog Kotlin overrides Spring Boot's managed Kotlin
result: pass
source: automated
coverage_id: D4

### 16. [01-01 D5] .gitignore covers build state, secrets files and tool output while keeping the existing .claude negations
expected: .gitignore covers build state, secrets files and tool output while keeping the existing .claude negations
result: pass
source: automated
coverage_id: D5

### 17. [01-02 D1] Spotless formats every .kt and .kts (including build-logic) with ktfmt kotlinlang style and requires the one-line SPDX header
expected: Spotless formats every .kt and .kts (including build-logic) with ktfmt kotlinlang style and requires the one-line SPDX header
result: pass
source: automated
coverage_id: D1

### 18. [01-02 D2] detekt with the comments rules and no baseline runs in check for the sample module and build-logic, and fails on an undocumented public class
expected: detekt with the comments rules and no baseline runs in check for the sample module and build-logic, and fails on an undocumented public class
result: pass
source: automated
coverage_id: D2

### 19. [01-02 D3] Dokka failOnWarning runs in check for modules and build-logic and fails on a KDoc link to a missing symbol
expected: Dokka failOnWarning runs in check for modules and build-logic and fails on a KDoc link to a missing symbol
result: pass
source: automated
coverage_id: D3

### 20. [01-02 D4] gradle-daemon-jvm.properties is generated by updateDaemonJvm from .java-version (toolchainVersion=25), with no CLI argument
expected: gradle-daemon-jvm.properties is generated by updateDaemonJvm from .java-version (toolchainVersion=25), with no CLI argument
result: pass
source: automated
coverage_id: D4

### 21. [01-02 D5] TestKit functionalTest suite proves cache reuse, .java-version invalidation, platform applied exactly once to implementation and testImplementation, all under --configuration-cache --isolated-projects; root check depends on it
expected: TestKit functionalTest suite proves cache reuse, .java-version invalidation, platform applied exactly once to implementation and testImplementation, all under --configuration-cache --isolated-projects; root check depends on it
result: pass
source: automated
coverage_id: D5

### 22. [01-03 D1] mise install provisions every pinned CLI tool from mise.toml and mise.lock; every tool answers its version command
expected: mise install provisions every pinned CLI tool from mise.toml and mise.lock; every tool answers its version command
result: pass
source: automated
coverage_id: D1

### 23. [01-03 D2] JDK comes from .java-version and Node.js from .nvmrc; neither is pinned in mise.toml; mise version lives only in min_version
expected: JDK comes from .java-version and Node.js from .nvmrc; neither is pinned in mise.toml; mise version lives only in min_version
result: pass
source: automated
coverage_id: D2

### 24. [01-03 D3] mise.lock holds per-platform checksums for four platforms and is enforced with locked = true
expected: mise.lock holds per-platform checksums for four platforms and is enforced with locked = true
result: pass
source: automated
coverage_id: D3

### 25. [01-03 D4] docs/tooling.md names every mise.toml tool key in backticks, has the Planned tools table with the same five columns, and contains no version numbers
expected: docs/tooling.md names every mise.toml tool key in backticks, has the Planned tools table with the same five columns, and contains no version numbers
result: pass
source: automated
coverage_id: D4

### 26. [01-04 D1] One ./gradlew spotlessApply formats Kotlin, KTS, JSON, JSON5, YAML and Markdown; a second run changes nothing
expected: One ./gradlew spotlessApply formats Kotlin, KTS, JSON, JSON5, YAML and Markdown; a second run changes nothing
result: pass
source: automated
coverage_id: D1

### 27. [01-04 D2] A Node major different from .nvmrc fails fast with a message naming .nvmrc and the fix; a matching major lets the Prettier tasks run
expected: A Node major different from .nvmrc fails fast with a message naming .nvmrc and the fix; a matching major lets the Prettier tasks run
result: pass
source: automated
coverage_id: D2

### 28. [01-04 D3] .editorconfig defines settings for every file type, declares [*] first, agrees with ktfmt and Prettier, and editorconfig-checker passes on the formatted tree
expected: .editorconfig defines settings for every file type, declares [*] first, agrees with ktfmt and Prettier, and editorconfig-checker passes on the formatted tree
result: pass
source: automated
coverage_id: D3

### 29. [01-05 D1] One ./gradlew spotlessApply formats TOML with taplo; unformatted TOML fails spotlessCheck, is fixed by spotlessApply, and a second run changes nothing
expected: One ./gradlew spotlessApply formats TOML with taplo; unformatted TOML fails spotlessCheck, is fixed by spotlessApply, and a second run changes nothing
result: pass
source: automated
coverage_id: D1

### 30. [01-05 D2] A missing taplo or shfmt fails only that type's Spotless tasks with a mise install hint; configuration (help) still succeeds
expected: A missing taplo or shfmt fails only that type's Spotless tasks with a mise install hint; configuration (help) still succeeds
result: pass
source: automated
coverage_id: D2

### 31. [01-05 D3] Shell scripts get the SPDX header on line 2 after the shebang, keep their descriptive comment, and are indented with 2 spaces by shfmt
expected: Shell scripts get the SPDX header on line 2 after the shebang, keep their descriptive comment, and are indented with 2 spaces by shfmt
result: pass
source: automated
coverage_id: D3

### 32. [01-05 D4] Properties, scenarios, txt and dotfile configs lose trailing whitespace and gain a final newline through the misc format
expected: Properties, scenarios, txt and dotfile configs lose trailing whitespace and gain a final newline through the misc format
result: pass
source: automated
coverage_id: D4

### 33. [01-05 D5] .editorconfig agrees with taplo and shfmt: editorconfig-checker exits 0 on the tree formatted by spotlessApply; the Renovate annotation above prettier in libs.versions.toml survives taplo
expected: .editorconfig agrees with taplo and shfmt: editorconfig-checker exits 0 on the tree formatted by spotlessApply; the Renovate annotation above prettier in libs.versions.toml survives taplo
result: pass
source: automated
coverage_id: D5

### 34. [01-06 D1] `reuse lint` reports the repository compliant with REUSE 3.3; every file (78 of 78) has copyright and licence information, the wrapper files resolve to Apache-2.0, and the generated mise lockfiles are covered
expected: `reuse lint` reports the repository compliant with REUSE 3.3; every file (78 of 78) has copyright and licence information, the wrapper files resolve to Apache-2.0, and the generated mise lockfiles are covered
result: pass
source: automated
coverage_id: D1

### 35. [01-06 D2] README.md shows the REUSE badge, a quick start (mise, mise install, lefthook install, ./gradlew build) and a pointer to docs/tooling.md
expected: README.md shows the REUSE badge, a quick start (mise, mise install, lefthook install, ./gradlew build) and a pointer to docs/tooling.md
result: pass
source: automated
coverage_id: D2

### 36. [01-06 D3] .gitattributes sets LF, CRLF for batch files, binary types, linguist-generated and linguist-documentation; git check-attr reports eol crlf for gradlew.bat, eol lf for gradlew and binary set for the wrapper jar
expected: .gitattributes sets LF, CRLF for batch files, binary types, linguist-generated and linguist-documentation; git check-attr reports eol crlf for gradlew.bat, eol lf for gradlew and binary set for the wrapper jar
result: pass
source: automated
coverage_id: D3

### 37. [01-06 D4] The committed tree is normalised: renormalising and diffing against the index exits 0, empty and binary files are untouched, and the working tree is clean afterwards
expected: The committed tree is normalised: renormalising and diffing against the index exits 0, empty and binary files are untouched, and the working tree is clean afterwards
result: pass
source: automated
coverage_id: D4

### 38. [01-07 D1] checkGsdConfigPolicy passes for a semantically equal config and fails on any added, removed or changed key (including inside lists), listing each key path and naming transient GSD keys and the same-PR policy rule
expected: checkGsdConfigPolicy passes for a semantically equal config and fails on any added, removed or changed key (including inside lists), listing each key path and naming transient GSD keys and the same-PR policy rule
result: pass
source: automated
coverage_id: D1

### 39. [01-07 D2] governance/gsd-config-policy.json is an exact copy of .planning/config.json
expected: governance/gsd-config-policy.json is an exact copy of .planning/config.json
result: pass
source: automated
coverage_id: D2

### 40. [01-07 D3] checkBuildScripts rejects version literals (GAV strings, plugin versions, version assignments, numeric jvmToolchain and JavaLanguageVersion.of) and eager or cross-project APIs, with sorted path:line: [rule] output, and passes on the repository
expected: checkBuildScripts rejects version literals (GAV strings, plugin versions, version assignments, numeric jvmToolchain and JavaLanguageVersion.of) and eager or cross-project APIs, with sorted path:line: [rule] output, and passes on the repository
result: pass
source: automated
coverage_id: D3

### 41. [01-07 D4] checkDaemonJvm fails when .java-version and toolchainVersion differ, when .java-version is missing, empty or non-numeric, or the properties file is missing or lacks the key, and names updateDaemonJvm
expected: checkDaemonJvm fails when .java-version and toolchainVersion differ, when .java-version is missing, empty or non-numeric, or the properties file is missing or lacks the key, and names updateDaemonJvm
result: pass
source: automated
coverage_id: D4

### 42. [01-07 D5] The three checks are cacheable tasks wired into root check through courses.governance; re-running with unchanged inputs is UP-TO-DATE or FROM-CACHE
expected: The three checks are cacheable tasks wired into root check through courses.governance; re-running with unchanged inputs is UP-TO-DATE or FROM-CACHE
result: pass
source: automated
coverage_id: D5

### 43. [01-08 D1] The pre-commit hook reformats staged Kotlin with the SPDX header and re-stages it, while an unstaged change in README.md stays untouched
expected: The pre-commit hook reformats staged Kotlin with the SPDX header and re-stages it, while an unstaged change in README.md stays untouched
result: pass
source: automated
coverage_id: D1

### 44. [01-08 D2] A staged synthetic GitHub token is blocked by the hook's gitleaks job
expected: A staged synthetic GitHub token is blocked by the hook's gitleaks job
result: pass
source: automated
coverage_id: D2

### 45. [01-08 D3] Licence, workflow and script checks run in the hook; the shellcheck, actionlint and zizmor jobs are skipped by their globs when no matching file is staged; the whole tree passes the hook
expected: Licence, workflow and script checks run in the hook; the shellcheck, actionlint and zizmor jobs are skipped by their globs when no matching file is staged; the whole tree passes the hook
result: pass
source: automated
coverage_id: D3

### 46. [01-08 D4] performance.scenarios parses and runs all four scenarios; docs/performance.md records a median per scenario
expected: performance.scenarios parses and runs all four scenarios; docs/performance.md records a median per scenario
result: pass
source: automated
coverage_id: D4

### 47. [01-08 D5] Build Scans are opt-in through --scan only and nothing in the build applies the Develocity plugin
expected: Build Scans are opt-in through --scan only and nothing in the build applies the Develocity plugin
result: pass
source: automated
coverage_id: D5

### 48. [01-08 D6] gradle-profiler output inside the project is never formatted
expected: gradle-profiler output inside the project is never formatted
result: pass
source: automated
coverage_id: D6

### 49. [01-09 D1] A PR that touches a governed file (or renames out of one) fails unless it carries the governance-approved label; empty, missing, malformed, spaced and non-ASCII inputs behave as specified
expected: A PR that touches a governed file (or renames out of one) fails unless it carries the governance-approved label; empty, missing, malformed, spaced and non-ASCII inputs behave as specified
result: pass
source: automated
coverage_id: D1

### 50. [01-09 D2] Only renovate[bot] with a version-only diff of every touched governed file is exempt; new lines, non-version tokens, human authors, added or patch-less files and hidden dash lines stay gated
expected: Only renovate[bot] with a version-only diff of every touched governed file is exempt; new lines, non-version tokens, human authors, added or patch-less files and hidden dash lines stay gated
result: pass
source: automated
coverage_id: D2

### 51. [01-09 D3] Same-repository PRs must come from gsd/phase-<N>[.<M>]-<slug>, renovate/* or release-please--* branches; fork PRs skip the rule; an empty head ref fails closed
expected: Same-repository PRs must come from gsd/phase-<N>[.<M>]-<slug>, renovate/* or release-please--* branches; fork PRs skip the rule; an empty head ref fails closed
result: pass
source: automated
coverage_id: D3

### 52. [01-09 D4] governance.yml triggers only on pull_request with a read-only token, passes every PR value through env, pins actions by SHA and ends in governance-success
expected: governance.yml triggers only on pull_request with a read-only token, passes every PR value through env, pins actions by SHA and ends in governance-success
result: pass
source: automated
coverage_id: D4

### 53. [01-10 D1] labels.yml defines all D-18 groups (9 type, 8 area, phase 1-13, deps plus 6 ecosystems, autorelease pending and tagged, upstream, governance-approved, security) with per-group colours, quoted names, and no community labels
expected: labels.yml defines all D-18 groups (9 type, 8 area, phase 1-13, deps plus 6 ecosystems, autorelease pending and tagged, upstream, governance-approved, security) with per-group colours, quoted names, and no community labels
result: pass
source: automated
coverage_id: D1

### 54. [01-10 D2] labels workflow: dry-run validate on pull requests, exact sync on main pushes and dispatch, issues write only in sync, serialised without cancelling, SHA-pinned actions, persist-credentials false
expected: labels workflow: dry-run validate on pull requests, exact sync on main pushes and dispatch, issues write only in sync, serialised without cancelling, SHA-pinned actions, persist-credentials false
result: pass
source: automated
coverage_id: D2

### 55. [01-10 D4] PULL_REQUEST_TEMPLATE.md has the five core headings, the four enabled configured headings and a checklist, in order
expected: PULL_REQUEST_TEMPLATE.md has the five core headings, the four enabled configured headings and a checklist, in order
result: pass
source: automated
coverage_id: D4

### 56. [01-10 D5] checkPrTemplate fails on a missing heading, wrong order, or a missing enabled configured section; disabled sections are not required; wired into check; cacheable
expected: checkPrTemplate fails on a missing heading, wrong order, or a missing enabled configured section; disabled sections are not required; wired into check; cacheable
result: pass
source: automated
coverage_id: D5

### 57. [01-10 D6] checkToolingInventory fails in both directions when mise.toml and docs/tooling.md disagree (including spelling drift pipx:reuse vs reuse), ignores order and non-mise rows, tolerates the inline-table npm:renovate entry and Prettier padding; the real repository passes
expected: checkToolingInventory fails in both directions when mise.toml and docs/tooling.md disagree (including spelling drift pipx:reuse vs reuse), ignores order and non-mise rows, tolerates the inline-table npm:renovate entry and Prettier padding; the real repository passes
result: pass
source: automated
coverage_id: D6

### 58. [01-10 D7] Both checks run in check; no TOML library was added to build-logic
expected: Both checks run in check; no TOML library was added to build-logic
result: pass
source: automated
coverage_id: D7

### 59. [01-11 D1] renovate.json5 passes renovate-config-validator --strict and sets minimumReleaseAge 7 days at the top level plus internalChecksFilter strict
expected: renovate.json5 passes renovate-config-validator --strict and sets minimumReleaseAge 7 days at the top level plus internalChecksFilter strict
result: pass
source: automated
coverage_id: D1

### 60. [01-11 D2] A local extract run finds every version source: github-actions, gradle (catalog, plugins), gradle-wrapper, mise, nvm and the three regex dependencies java, prettier and jdx/mise (plus ktfmt after Task 2)
expected: A local extract run finds every version source: github-actions, gradle (catalog, plugins), gradle-wrapper, mise, nvm and the three regex dependencies java, prettier and jdx/mise (plus ktfmt after Task 2)
result: pass
source: automated
coverage_id: D2

### 61. [01-11 D3] Mise tool versions, .nvmrc and GitHub Actions SHA pins are extracted too (TOOL-02, CICD-03)
expected: Mise tool versions, .nvmrc and GitHub Actions SHA pins are extracted too (TOOL-02, CICD-03)
result: pass
source: automated
coverage_id: D3

### 62. [01-11 D4] Updates are grouped (kotlin, junit, kotest, spring-boot with Jackson, axon, testcontainers, formatting and analysis, github-actions, dev tools) and labelled deps plus an ecosystem label that exists in .github/labels.yml
expected: Updates are grouped (kotlin, junit, kotest, spring-boot with Jackson, axon, testcontainers, formatting and analysis, github-actions, dev tools) and labelled deps plus an ecosystem label that exists in .github/labels.yml
result: pass
source: automated
coverage_id: D4

### 63. [01-11 D5] Automerge is enabled by exactly three rules (Action digests, mise patches, formatting tool patches), platform automerge with squash, and never for runtime libraries
expected: Automerge is enabled by exactly three rules (Action digests, mise patches, formatting tool patches), platform automerge with squash, and never for runtime libraries
result: pass
source: automated
coverage_id: D5

### 64. [01-11 D6] JDK updates propose only LTS majors as a bare major, carry the deps:jdk label and an updateDaemonJvm reminder
expected: JDK updates propose only LTS majors as a bare major, carry the deps:jdk label and an updateDaemonJvm reminder
result: pass
source: automated
coverage_id: D6

### 65. [01-12 D3] secrets job scans the full history with gitleaks git --redact
expected: secrets job scans the full history with gitleaks git --redact
result: pass
source: automated
coverage_id: D3

### 66. [01-12 D4] ci-success aggregates build, lint and secrets with if: always() and treats skipped, cancelled and failed results as failure; no path filters
expected: ci-success aggregates build, lint and secrets with if: always() and treats skipped, cancelled and failed results as failure; no path filters
result: pass
source: automated
coverage_id: D4

### 67. [01-12 D5] Hardening: permissions {} at workflow level, minimal job grants, persist-credentials false, every uses pinned by a 40-hex SHA with a tag comment
expected: Hardening: permissions {} at workflow level, minimal job grants, persist-credentials false, every uses pinned by a 40-hex SHA with a tag comment
result: pass
source: automated
coverage_id: D5

### 68. [01-12 D7] Normalisation check never touches the real index
expected: Normalisation check never touches the real index
result: pass
source: automated
coverage_id: D7

### 69. [01-13 D2] Apply script is idempotent and cannot write in dry-run or check mode
expected: Apply script is idempotent and cannot write in dry-run or check mode
result: pass
source: automated
coverage_id: D2

### 70. [01-13 D5] checkClaudeAskRules keeps ask rules and governed paths in sync (missing, duplicate, empty, order, extras)
expected: checkClaudeAskRules keeps ask rules and governed paths in sync (missing, duplicate, empty, order, extras)
result: pass
source: automated
coverage_id: D5

### 71. [01-14 D1] Merge settings (squash only, PR_TITLE and PR_BODY, delete head branches, auto-merge, update branch) and security features (Dependabot alerts, private vulnerability reporting, secret scanning, push protection) live and matching the declared files
expected: Merge settings (squash only, PR_TITLE and PR_BODY, delete head branches, auto-merge, update branch) and security features (Dependabot alerts, private vulnerability reporting, secret scanning, push protection) live and matching the declared files
result: pass
source: automated
coverage_id: D1

### 72. [01-14 D2] governance-approved label exists on the repository before the phase PR is opened
expected: governance-approved label exists on the repository before the phase PR is opened
result: pass
source: automated
coverage_id: D2

### 73. [01-14 D3] GRADLE_ENCRYPTION_KEY secret present (value never shown)
expected: GRADLE_ENCRYPTION_KEY secret present (value never shown)
result: pass
source: automated
coverage_id: D3

### 74. [01-15 D1] The governance workflow obtains the pull request file list only through list-pr-files.sh, which fails on a failing gh, empty output, non-array or invalid JSON (CR-01)
expected: The governance workflow obtains the pull request file list only through list-pr-files.sh, which fails on a failing gh, empty output, non-array or invalid JSON (CR-01)
result: pass
source: automated
coverage_id: D1

### 75. [01-15 D2] Strict input validation, no stale destination and no leaked temporary file in the listing
expected: Strict input validation, no stale destination and no leaked temporary file in the listing
result: pass
source: automated
coverage_id: D2

### 76. [01-15 D3] A list of 3000 or more entries needs the label and never takes the renovate exemption; 2999 entries are evaluated normally (WR-03)
expected: A list of 3000 or more entries needs the label and never takes the renovate exemption; 2999 entries are evaluated normally (WR-03)
result: pass
source: automated
coverage_id: D3

### 77. [01-15 D5] 01-REVIEW-DISPOSITION.md records CR-01 and WR-03 fixed, WR-02 deferred, 7 open
expected: 01-REVIEW-DISPOSITION.md records CR-01 and WR-03 fixed, WR-02 deferred, 7 open
result: pass
source: automated
coverage_id: D5

### 78. [01-16 D1] A padded, tab-indented or CRLF governed entry governs its path in the CI gate exactly as the Gradle check expects its ask rule
expected: A padded, tab-indented or CRLF governed entry governs its path in the CI gate exactly as the Gradle check expects its ask rule
result: pass
source: automated
coverage_id: D1

### 79. [01-16 D2] Comments, inner spaces, non-ASCII, BOM, NUL, inner CR, empty file and line ordering behave identically in both parsers
expected: Comments, inner spaces, non-ASCII, BOM, NUL, inner CR, empty file and line ordering behave identically in both parsers
result: pass
source: automated
coverage_id: D2

### 80. [01-16 D3] WR-04 recorded fixed in 01-REVIEW-DISPOSITION.md with WR-02 deferred and open 6
expected: WR-04 recorded fixed in 01-REVIEW-DISPOSITION.md with WR-02 deferred and open 6
result: pass
source: automated
coverage_id: D3

### 81. [01-17 D1] A pull request changing a governance check, its wiring, the settings writer, the root build configuration, the plugin build, the root and settings conventions or the launcher needs the governance-approved label
expected: A pull request changing a governance check, its wiring, the settings writer, the root build configuration, the plugin build, the root and settings conventions or the launcher needs the governance-approved label
result: pass
source: automated
coverage_id: D1

### 82. [01-17 D2] Claude Code asks before editing any governed path: one Edit rule per entry (29) plus config-set
expected: Claude Code asks before editing any governed path: one Edit rule per entry (29) plus config-set
result: pass
source: automated
coverage_id: D2

### 83. [01-17 D3] Deliberate exclusions (catalog, gradlew.bat, module conventions, functional tests, settings-script tests, convention sample) pass without the label
expected: Deliberate exclusions (catalog, gradlew.bat, module conventions, functional tests, settings-script tests, convention sample) pass without the label
result: pass
source: automated
coverage_id: D3

### 84. [01-17 D4] WR-01 recorded fixed; CR-01, WR-03, WR-04 fixed, WR-02 deferred, open 5
expected: WR-01 recorded fixed; CR-01, WR-03, WR-04 fixed, WR-02 deferred, open 5
result: pass
source: automated
coverage_id: D4

### 85. [01-18 D1] The reproduced WR-05 list (harmless first entry, governed second entry with a NUL in its patch, no label) fails the gate instead of printing 'no governed file is touched'
expected: The reproduced WR-05 list (harmless first entry, governed second entry with a NUL in its patch, no label) fails the gate instead of printing 'no governed file is touched'
result: pass
source: automated
coverage_id: D1

### 86. [01-18 D2] NUL in a filename, a previous_filename or the patch of an ungoverned only entry fails the gate; the label and the renovate exemption never turn an unreadable list into a pass
expected: NUL in a filename, a previous_filename or the patch of an ungoverned only entry fails the gate; the label and the renovate exemption never turn an unreadable list into a pass
result: pass
source: automated
coverage_id: D2

### 87. [01-18 D3] The status guard and the completeness guard each fail on their own: a fake jq that exits 3 after writing every record (R7), exits 0 one record short (R8) or writes a torn record (R9)
expected: The status guard and the completeness guard each fail on their own: a fake jq that exits 3 after writing every record (R7), exits 0 one record short (R8) or writes a torn record (R9)
result: pass
source: automated
coverage_id: D3

### 88. [01-18 D4] No records file is left in TMPDIR after a failing or a passing run, and it existed during the run
expected: No records file is left in TMPDIR after a failing or a passing run, and it existed during the run
result: pass
source: automated
coverage_id: D4

### 89. [01-18 D5] Existing behaviour is unchanged: 80 earlier cases (empty list, 2999-entry list, label rule, files API limit, branch rule, normaliser) and 25 listing cases still pass
expected: Existing behaviour is unchanged: 80 earlier cases (empty list, 2999-entry list, label rule, files API limit, branch rule, normaliser) and 25 listing cases still pass
result: pass
source: automated
coverage_id: D5

### 90. [01-19 D1] A pull request adding or changing renovate.json, renovate.jsonc or a sidecar lockfile fails governance-success without the label, and the agent's ask rules cover the same paths
expected: A pull request adding or changing renovate.json, renovate.jsonc or a sidecar lockfile fails governance-success without the label, and the agent's ask rules cover the same paths
result: pass
source: automated
coverage_id: D1

### 91. [01-19 D2] Every Renovate config name and every mise config, rc and lock location CI honours is governed, with 22 further boundary cases and four ungoverned exclusion cases
expected: Every Renovate config name and every mise config, rc and lock location CI honours is governed, with 22 further boundary cases and four ungoverned exclusion cases
result: pass
source: automated
coverage_id: D2

### 92. [01-20 D2] ADR-0001 links ADR-0003 from its Consequences section with its Context, Decision and earlier Consequences unchanged
expected: ADR-0001 links ADR-0003 from its Consequences section with its Context, Decision and earlier Consequences unchanged
result: pass
source: automated
coverage_id: D2

### 93. [01-20 D3] 01-REVIEW-DISPOSITION.md records WR-06 as deferred with the ADR path; WR-05 and WR-07 stay fixed, WR-02 stays deferred, open count is 6
expected: 01-REVIEW-DISPOSITION.md records WR-06 as deferred with the ADR path; WR-05 and WR-07 stay fixed, WR-02 stays deferred, open count is 6
result: pass
source: automated
coverage_id: D3

## Summary

total: 93
passed: 89
issues: 1
pending: 0
skipped: 3
blocked: 0

## Gaps

- gap_id: G-01-2
  truth: "A Node switch on PATH is seen by the build: verifyNodeVersion checks, and Prettier runs with, the node binary on the current build's PATH, so the Prettier cache is invalidated on a Node major change even with a warm Gradle daemon"
  status: resolved
  reason: "User reported: На поиск exec-ом бинаря по PATH полагаться нельзя. Нужно резолвить PATH, используя System.getenv(\"PATH\"), и самому находить полный путь до бинаря. Потом уже пользоваться exec. Это уже рабочий способ."
  severity: major
  test: 2
  resolved_by: "01-21-PLAN.md"
  resolved_at: "2026-11-22"
  root_cause: "Two independent causes. (1) VerifyNodeVersion execs `node` by bare name; the JDK resolves a bare command against parentPathv, a native copy of PATH cached once when the daemon JVM initialises ProcessImpl (JDK-6413313), so a warm daemon finds node on the PATH of the client that started it. Gradle 9.9.2 does update System.getenv() and the native environ per build, so System.getenv(\"PATH\") inside a task action is current; only the bare-name lookup is stale. (2) Spotless Prettier (spotless-lib 4.11.0) never runs the verified node: with no explicit executables it searches System.getenv(\"PATH\") npm-first and runs the node next to that npm, so the guard (node-first, stale) and the formatter (npm-first, current) can pick different binaries with or without a daemon."
  artifacts:
    - path: "build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/VerifyNodeVersion.kt"
      issue: "commandLine(\"node\", \"--version\") at lines 58-59 runs node by bare name"
    - path: "build-logic/src/main/kotlin/courses.root.gradle.kts"
      issue: "prettier(...) at line 113 sets no nodeExecutable/npmExecutable; the comment at lines 47-48 ('Spotless runs Prettier with the node on PATH') is inaccurate; pathOf (lines 59-67) is correct but shell-based"
    - path: "build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/FormattingFunctionalTest.kt"
      issue: "no test switches node on PATH within one daemon or checks that Prettier runs the verified node"
  missing:
    - "One resolver that walks System.getenv(\"PATH\") (split on File.pathSeparator, skip empty entries) and returns the first File(dir, name) that isFile && canExecute() as an absolute path, or null to keep the existing 'not available on PATH' message"
    - "VerifyNodeVersion execs the resolved absolute node path, never \"node\""
    - "Spotless Prettier is pointed at the same binary: prettier(...).nodeExecutable(nodePath).npmExecutable(<nodeDir>/npm), resolved at configuration time through a ValueSource (providers.of) rather than a direct System.getenv(\"PATH\") read, so the configuration cache is invalidated only when the resolved path changes; configuration must not fail without Node (verifyNodeVersion fails first)"
    - "Correct the 'Spotless runs Prettier with the node on PATH' comment"
    - "TestKit regression tests: two builds on one daemon with a @TempDir node shim prepended in the second, expecting verifyNodeVersion to fail naming .nvmrc; and a logging node shim plus npm link asserting Prettier's server ran the verified node"
    - "Optional: record the resolved node path in node-major.txt so a different binary also invalidates Prettier (author's call; today invalidation is by major only); optional: replace pathOf's sh -c command -v with the same ValueSource resolver"
  debug_session: ".planning/debug/node-binary-resolved-from-daemon-path.md"

## Deferred Follow-Ups

- test: 3
  idea: "GSD hooks fail in a Claude Code session started inside a git worktree: the hooks are registered in the main checkout's .claude/settings.local.json as \"$CLAUDE_PROJECT_DIR\"/.claude/hooks/..., the session in the worktree picks those settings up, but $CLAUDE_PROJECT_DIR points to the worktree, where the gitignored, locally installed .claude/hooks/ does not exist (SessionStart, PreToolUse, PostToolUse and Stop hooks all fail with 'No such file or directory' / MODULE_NOT_FOUND). Every GSD session run from a phase worktree therefore runs without the GSD guards. Options: install GSD locally in each worktree, or register the hooks with absolute paths to the main checkout. GSD tooling setup, not a Phase 1 deliverable."
  deferred_at: 2026-11-22
- test: 9
  idea: "Post-merge: confirm CodeQL (actions) and Scorecard analyses from main, and that the first label sync makes the repository equal .github/labels.yml with an idempotent re-run"
  deferred_at: 2026-11-22
- test: 10
  idea: "Post-merge: close Renovate onboarding PR #1 and check the first grouped PRs (labels, 7-day cooldown, three automerge rules, governance-approved for .mise/locks sidecar bumps)"
  deferred_at: 2026-11-22
- test: 11
  idea: "Post-merge: register the repository at api.reuse.software and confirm the README REUSE badge shows compliant"
  deferred_at: 2026-11-22
