# Phase 1: Repository, Build & CI Foundation - Research

**Researched:** 2026-11-21
**Domain:** Gradle multi-project build foundation (build-logic, version catalog, `:platform`), Kotlin formatting and static analysis, REUSE/SPDX, GitHub Actions supply-chain-hardened CI, Renovate, mise-pinned tooling, repository governance as code
**Confidence:** HIGH for the build, formatting, analysis, REUSE, mise and Renovate-extraction findings (each was run end to end in a scratch prototype on Gradle 9.9.2 / JDK 25 this session); MEDIUM for GitHub settings payloads, CodeQL-for-Kotlin and Scorecard details (documentation only, not applied)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Modules & Identity**
- **D-01:** The root Maven group and Kotlin base package is `io.github.aanbrn.courses`, so code lives in packages such as `io.github.aanbrn.courses.command...`. — **Reversibility:** costly — every package, Konsist rule and Spring scan path depends on it. It becomes effectively one-way once event class names are persisted in Axon Server (Phase 3), unless event type names are decoupled from class names.
- **D-02:** Phase 1 creates only infrastructure projects: `build-logic` (included build), `:platform` (`java-platform`), and one tiny **sample module**. The sample module exercises the conventions end to end in `./gradlew build`: Kotlin compile, ktfmt, detekt, Dokka KDoc checks, test suites, SPDX header. Real modules (contracts, services, frontend, docs-site) arrive in the phases that need them. The planner decides what happens to the sample module later, either removed when the first real module lands in Phase 2 or kept as a permanent convention fixture, and records the choice.
- **D-03:** Directories are grouped by role: `platform/`, `services/{command,query,automation,bff}`, `contracts/*`, `frontend/`, `docs-site/`. Phase 1 creates only `platform/` and the sample module, but `settings.gradle.kts` and conventions must anticipate this layout. — **Reversibility:** costly — paths are referenced from CI, Renovate, REUSE, `.gitattributes`, IDE run configurations and docs.
- **D-04:** Convention plugin IDs use a project prefix plus a role, for example `courses.kotlin-library`, `courses.spring-service`, `courses.test-suites`. Plugins are composed per module role. Phase 1 needs only the ones the sample module and `:platform` use, plus any root or quality plugins.

**Formatting, Static Analysis & Pre-commit**
- **D-05:** Prettier, run through Spotless, formats JSON, YAML and Markdown, so one `./gradlew spotlessApply` formats everything. Generated code is excluded. Biome joins for TS/TSX in Phase 5. Research needs to settle how Spotless provides the Node runtime for Prettier while honouring the INFRA-09 rule that the Node version lives only in `.nvmrc`.
- **D-06:** ktfmt runs with the **kotlinlang** style (4-space indent, official Kotlin coding conventions), including KDoc formatting, for `*.kt` and `*.kts`, `build-logic` included. `.editorconfig` must agree with it.
- **D-07:** detekt uses `dev.detekt` 2.0.0-alpha.x with a strict configuration and no baseline, `comments` rules enabled. If it breaks with Kotlin 2.4, the configuration cache or isolated projects, fall back to `allWarningsAsErrors` plus Konsist and Dokka. Record the fallback in an ADR and an upstream record. Formal ADR and upstream templates arrive in Phase 2, so a Phase 1 fallback uses a minimal MADR file under `docs/adr/` that Phase 2 normalises.
- **D-08:** The pre-commit hook is lefthook and is kept **fast**. It formats staged files and re-stages them, and runs gitleaks on staged changes, `reuse lint`, and actionlint, zizmor and shellcheck on changed files only. detekt, Dokka and tests run in CI, not in the hook. There is no pre-push hook.

**CI Structure & Renovate**
- **D-09:** Workflows are split by purpose: `ci.yml` runs build, tests, analysis and linters as parallel jobs, alongside `codeql.yml`, `scorecard.yml`, `governance.yml` and `labels.yml`. A final aggregating `ci-success` job is the **single required status check** in the ruleset, so adding jobs never requires a ruleset change. Defaults are `permissions: read-all` with per-job elevation and `persist-credentials: false`.
- **D-10:** CI runs on **ubuntu only**. There is no macOS or Windows matrix.
- **D-11:** Renovate runs as the **Mend Renovate GitHub App**, configured in-repo. Installing the app is an author approval gate at execution time.
- **D-12:** Renovate automerges only GitHub Actions digest updates and patch updates of non-runtime dev tools (mise tools, linter and formatter plugins), and only when CI is green. Runtime libraries (Axon, Spring, Kotlin and so on) and every minor or major update need a manual merge.

**Governance as Code**
- **D-13:** Repository governance checks are **Gradle tasks in `build-logic`**, written in Kotlin, TestKit-tested, cacheable and wired into `./gradlew check` locally and in CI. They cover the `.planning/config.json` policy, version-literal rejection in build scripts, `.java-version` and `gradle-daemon-jvm.properties` consistency, and the `docs/tooling.md` cross-check. The policy file lives in-repo, for example `governance/gsd-config-policy.json`. The PR label check (`governance-approved`) stays in `governance.yml` because it needs PR context.
- **D-14:** The `.planning/config.json` policy pins **the whole file**: an exact expected copy. Any change, including new keys from a GSD update, requires a deliberate policy update in the same PR.
- **D-15:** GitHub repository settings live **declaratively in the repo**: ruleset JSON such as `.github/rulesets/main.json`, merge settings and security features. They are applied by an idempotent script using `gh api`, which the author runs after explicit approval. CI runs a **read-only drift check** against the live settings. That check needs a read token stored as a secret, and creating that secret is an approval gate.
- **D-16:** PRs from `renovate[bot]` that only change versions or digests, verified from the diff, are **exempt** from the `governance-approved` label requirement even when they touch governance files (`mise.toml`, workflows, Renovate config). Without this exemption, D-12 automerge cannot work. Every other governance change requires the label.

**Tooling Inventory, Labels & Build Performance**
- **D-17:** `docs/tooling.md` is a hand-written table with these columns: tool, purpose, version source (mise / catalog / `.java-version` / `.nvmrc` / SHA), installation, and where it is used (local / CI / hook). It contains **no version numbers**, only pointers to the single source. A Gradle check fails if a `mise.toml` tool is missing from the table, or if a table entry backed by mise is missing from `mise.toml`.
- **D-18:** Labels in `labels.yml` use prefix groups, each with its own colour:
  - `type:` with values feat, fix, docs, chore, refactor, test, ci, build and perf
  - `area:` with values build, ci, command, query, automation, bff, ui and docs
  - `phase:N`
  - `deps` and `deps:<ecosystem>` (Renovate)
  - `autorelease: pending` and `autorelease: tagged` (release-please)
  - `upstream`
  - `governance-approved`
  - `security`

  CI syncs the labels. Community labels such as `good first issue` are left for Phase 13.
- **D-19:** Isolated projects: set `org.gradle.unsafe.isolated-projects=true` if the build is green with it. Otherwise turn the flag off, name the blocking plugin in an upstream record and an ADR with re-enable criteria, and keep `build-logic` isolated-projects-compatible either way. A TestKit test runs the conventions with isolated projects enabled.
- **D-20:** Build Scans are **opt-in only**, via `--scan` or `-Pscan`, and never run in CI. gradle-profiler scenarios (`performance.scenarios`: clean build, no-op, ABI change, configuration-cache hit) are committed and run manually. Baseline results are documented.

### Claude's Discretion
- What happens to the sample module after Phase 1 (D-02), and its name and location.
- Exact job breakdown inside `ci.yml`, and caching details within the INFRA-10 constraints.
- Exact detekt rule set strictness beyond "no baseline, `comments` enabled".
- Concrete file names for the policy, ruleset and apply script, and the colour palette for labels.

### Deferred Ideas (OUT OF SCOPE)
- Community labels (`good first issue`, `help wanted`, `question`): Phase 13, with the community health files.
- Scheduled gradle-profiler job in CI with trend reports: considered and not adopted (shared-runner noise). Can be revisited in Phase 12 hardening.
- macOS or Windows CI matrix: not adopted. Revisit only if platform-specific defects appear.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| INFRA-01 | One Gradle command builds the backend; `build-logic` + version catalog | Prototype proves settings plugin + root plugin + module convention + catalog on Gradle 9.9.2 (Patterns 1-4) |
| INFRA-09 | One version source per ecosystem; literals rejected by a check; Renovate covers all | Catalog/`.java-version`/`.nvmrc`/`mise.toml`/SHA layout; Renovate extraction proved for every source (Pattern 12); literal-check design (Pattern 9); Node-for-Prettier answer (Pattern 5) |
| INFRA-10 | Every applicable speed-up; isolated projects evaluated | `org.gradle.isolated-projects=true` is green in the prototype (D-19 resolved, Finding 1); daemon JVM, gradle-profiler scenarios, setup-gradle caching (Patterns 10, 11, 13) |
| INFRA-11 | `:platform` `java-platform` is the single alignment point | `platform(project(":platform"))` applied by the convention; `api(platform(...))` + `constraints` (Pattern 3) |
| INFRA-12 | `build-logic` is first-class: catalog, formatted, analysed, KDoc'd, TestKit incl. config cache | Typed `libs` accessor recipe, self-quality of build-logic, TestKit via `includeBuild` (Finding 2, Patterns 2, 6) |
| INFRA-13 | `.gitattributes` consistent with `.editorconfig`; CI verifies normalisation | `git add --renormalize . && git diff --cached --exit-code` proven (Pattern 14) |
| INFRA-14 | Comprehensive `.gitignore`; `.editorconfig` for every file type; formatters do not conflict | `.editorconfig` + `editorconfig-checker` proven; ktfmt string-length conflict handled (Pitfall 14) |
| INFRA-16 | REUSE-compliant; header via Spotless; `REUSE.toml` aggregate; `reuse lint` in hook and CI | Compliance proven; three traps found (Finding 5, Pattern 8) |
| TEST-09 | One command formats everything; hook formats staged files; CI fails on violations | Spotless (ktfmt + Prettier + SPDX) and lefthook + IDE hook proven (Patterns 4, 7) |
| TEST-10 | KDoc formatted, presence enforced by detekt `comments`, broken refs fail Dokka | ktfmt KDoc behaviour verified; detekt comments rules are OFF by default (Finding 4); Dokka `failOnWarning` proven (Pattern 6) |
| TEST-13 | Static analysis for Kotlin with build failing on violations | detekt `dev.detekt` 2.0.0-alpha.6 works with Kotlin 2.4.21, config cache and isolated projects; fallback not needed (Finding 1) |
| CICD-01 | GitHub Actions builds, tests, analyses on every push and PR | `ci.yml` skeleton with `ci-success` gate passes actionlint and zizmor (Pattern 11) |
| CICD-03 | All Actions pinned by SHA | SHAs resolved 2026-11-21 (Standard Stack); zizmor default requires hash pins for all actions |
| CICD-04 | Renovate with grouping and minimum release age | Config validated `--strict`; extraction run covers github-actions, gradle, gradle-wrapper, mise, nvm, regex (Pattern 12) |
| CICD-06 | Secret leaks detected in full history and PRs | `gitleaks git` flags verified; the repo's history is currently clean (Pattern 11) |
| CICD-08 | actionlint, zizmor, shellcheck, locally and in CI | Tools installed via mise and run; `permissions: read-all` fails zizmor (Finding 6) |
| TOOL-01 | Tool inventory in `docs/tooling.md` | Table design plus Gradle cross-check against `mise.toml` (Pattern 9) |
| TOOL-02 | Non-Gradle CLI tools pinned in `mise.toml`, updated by Renovate | `mise.toml` + `mise.lock` proven; Renovate mise manager extracts every tool (Pattern 10) |
| GH-06 | Security settings enabled (ruleset, secret scanning, Dependabot alerts, PVR, CodeQL, Scorecard) | Payload shapes cited; live state read (Pattern 15, Finding 9); approval gate |
| GH-07 | GSD workflow settings: squash only, PR title/body as commit, delete head branches | Repo PATCH fields cited; `allow_auto_merge` needed for D-12 (Pattern 15) |
| GH-08 | One PR template mirroring `/gsd-ship` body | Exact section headings read from `ship.md` and `config.json` (Pattern 16) |
| GH-11 | Labels as code, synced by CI | `labels.yml` + `crazy-max/ghaction-github-labeler` SHA-pinned; bootstrap trap (Pitfall 11) |
| AIDEV-05 | CI test validates `.planning/config.json` against a policy | Gradle task design; transient-key and whole-file risk (Finding 8) |
| AIDEV-06 | Governance file changes fail CI without `governance-approved` | `governance.yml` design, required-check tension, bot exemption (Finding 7, Pattern 17) |
| AIDEV-08 | Claude Code `ask` rules for governance files and `config-set` | Rule syntax cited; GSD itself calls `config-set` (Finding 8, Pattern 18) |
</phase_requirements>

## Summary

The whole Phase 1 stack is buildable as specified. A scratch prototype (Gradle 9.9.2, JDK 25 Temurin, Kotlin 2.4.21) reproduced the decided architecture with every guard on: configuration cache with problems failing, **isolated projects on**, build cache, `build-logic` included build with a settings plugin, a root plugin and a module convention, a `:platform` `java-platform`, Spotless (ktfmt kotlinlang style, Prettier for JSON/YAML/Markdown, SPDX header on `.kt` and `.kts`), detekt `dev.detekt` 2.0.0-alpha.6 with the `comments` rules switched on, Dokka 2.3.0 with `failOnWarning`, JUnit 6 + Kotest assertions, and TestKit functional tests that run the conventions with `--configuration-cache --isolated-projects`. A complete `./gradlew build` was green and a second run reused the configuration cache. D-19 therefore resolves to "enable isolated projects"; D-07 resolves to "keep detekt, no ADR fallback needed". The same prototype also exercised mise (all CLI tools, `mise.lock`), `reuse lint`, lefthook, actionlint, zizmor, editorconfig-checker, gitleaks and a Renovate local extraction run.

The prototype surfaced ten findings that change how the plans must be written, because following the obvious path fails. The largest are: (1) the typed `libs` accessor in precompiled plugins needs a classpath hack and fails inside TestKit unless the fixture includes the real `build-logic`; (2) Spotless string globs starting with `**` break under isolated projects; (3) detekt's `comments` rules are all `active: false` by default; (4) `reuse lint` rejects the SPDX string inside a Spotless config, needs `LICENSES/MIT.txt`, and sees the Apache-2.0 headers in the Gradle wrapper scripts; (5) `permissions: read-all` fails zizmor, which collides with D-09; (6) a `governance` check in a separate workflow cannot be part of the single `ci-success` required check, which collides with D-09/D-16; (7) Renovate has no built-in manager for `.java-version`, so a custom regex manager is required; (8) the GSD workflows themselves call `config-set`, so an `ask` rule and a whole-file policy (D-14) will fire during normal GSD operation. Each is stated with its fix below.

**Primary recommendation:** Build exactly the prototype's three-layer convention structure (`courses.settings` settings plugin, `courses.root` root plugin that formats the whole repository including `build-logic`, `courses.kotlin-library` module plugin), pin tools through `mise.toml` + committed `mise.lock`, make `ci-success` plus a `governance` check the two required contexts (flag the D-09 deviation to the author), and gate every GitHub-side change behind the apply script and explicit approval.

## Findings That Change the Plan

These are discoveries from running things, not from documentation. The planner must bake each into task actions.

1. **Isolated projects, detekt alpha and the whole quality chain are green** `[VERIFIED: prototype run, Gradle 9.9.2, 2026-11-21]`. `org.gradle.isolated-projects=true` is the current property name; the `org.gradle.unsafe.` name from D-19 is still accepted but the docs prefer the new one `[CITED: docs.gradle.org/current/userguide/isolated_projects.html]`. Isolated projects are incubating, were promoted from experimental in 9.7.0, fail on the first violation, and force the configuration cache on. Use the new property name. No ADR/upstream fallback for detekt is needed; keep the D-07 fallback text only as a contingency note.
2. **Typed catalog access in precompiled script plugins still needs a hack, and TestKit needs `includeBuild`** `[VERIFIED: prototype]`. Without `implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))` in `build-logic/build.gradle.kts`, `the<org.gradle.accessors.dm.LibrariesForLibs>()` is "Unresolved reference 'accessors'" on Gradle 9.9.2. With `GradleRunner.withPluginClasspath()` the typed accessor fails at apply time (`Extension of type 'LibrariesForLibs' does not exist` even when the fixture declares the same catalog, because the class identities differ). The working TestKit fixture writes `pluginManagement { includeBuild("<abs build-logic dir>") }` into the fixture `settings.gradle.kts` (no `withPluginClasspath()`) and passes the catalog and build-logic paths as `systemProperty` inputs. Warm run: about 6 s per test; cold: about 85 s.
3. **Spotless string globs starting with `**` fail under isolated projects** `[VERIFIED: prototype]`: `target("**/*.kt")` throws `Project ':' cannot access 'Project.layout' functionality on subprojects`. `target("sample/**/*.kt")`, and a `layout.projectDirectory.asFileTree.matching { include(...); exclude(...) }` FileTree both work. Use the FileTree form in the root plugin.
4. **detekt 2.0.0-alpha.6 `comments` rules are all `active: false` by default** `[VERIFIED: detektGenerateConfig output]`. The set is `AbsentOrWrongFileLicense`, `DeprecatedBlockTag`, `DocumentationOverPrivateFunction`, `DocumentationOverPrivateProperty`, `EndOfSentenceFormat`, `KDocReferencesNonPublicProperty`, `OutdatedDocumentation`, `UndocumentedPublicClass`, `UndocumentedPublicFunction`, `UndocumentedPublicProperty`. TEST-10 needs an explicit `config/detekt/detekt.yml` override that activates them (verified to fail the build on undocumented public API). `OutdatedDocumentation` wants `@param` (not `@property`) for a `private val` constructor parameter.
5. **REUSE has three traps** `[VERIFIED: prototype with reuse 6.3.0]`: (a) a Spotless `licenseHeader("// SPDX-License-Identifier: MIT")` string inside `build.gradle.kts` is parsed as a tag with invalid expression `MIT",` and fails `reuse lint`; use `licenseHeaderFile("config/spotless/license-header.txt")` instead; (b) a `LICENSES/MIT.txt` is required (`reuse download MIT`); the root `LICENSE` is not a REUSE license file; (c) the Gradle wrapper scripts `gradlew`/`gradlew.bat` carry an `Apache-2.0` header, so `LICENSES/Apache-2.0.txt` is also required and an `override` annotation in `REUSE.toml` must declare `gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` as Apache-2.0, otherwise the aggregate rule would add MIT on top of their Apache-2.0 header (inferred from the `aggregate` semantics, not run). Also, `pipx:reuse` under mise crashes at import without `extras = "charset-normalizer"`.
6. **`permissions: read-all` fails zizmor** (`excessive-permissions`, medium, exit code 13) while `permissions: {}` at workflow level plus `permissions: contents: read` per job is clean `[VERIFIED: zizmor 1.32.0 --offline]`. D-09 says `permissions: read-all`; the intent (least privilege, per-job elevation) is kept by using `{}` plus explicit job permissions. **Flag to the author as a small deviation from D-09's wording.**
7. **`ci-success` cannot absorb a separate `governance.yml` workflow.** A job can only `needs:` jobs of its own workflow. The PR-label check must run on `labeled`/`unlabeled` events; putting it in `ci.yml` would re-run everything on label events, and a skipped-jobs shortcut would let a later green `ci-success` check run mask a failing build. Recommendation: keep `governance.yml` separate, make its single final job a stable context (`governance-success`), and list **two** required contexts in the ruleset (`ci-success`, `governance-success`). This deviates from D-09's "single required status check" wording and must be confirmed by the author. See Pattern 17.
8. **GSD itself calls `config-set` and mutates config keys at runtime** `[VERIFIED: grep of .claude/gsd-core/workflows]`: `gsd_run query config-set workflow._auto_chain_active ...` (execute-phase, discuss-phase-assumptions), `workflow.tdd_mode`, `workflow.nyquist_validation`, `workflow.context_drift_action`. An `ask` rule on `config-set` (AIDEV-08) will prompt during legitimate GSD flows (expected and acceptable), and the whole-file policy (D-14) will fail if a transient key such as `workflow._auto_chain_active` is committed. The policy task must produce a diff-style message naming the offending keys, and the plan must document that transient keys are reset before commit.
9. **Live repository state (read-only `gh api`, 2026-11-21):** the repo is public, `main` has **no rulesets**, merge commits and rebase merges are allowed, `deleteBranchOnMerge` is false, the wiki is enabled, and secret scanning plus push protection are already enabled; Dependabot security updates are disabled `[VERIFIED: gh repo view / gh api repos/aanbrn/axon5-dcb-courses]`. The apply script must therefore be a diff-and-set (idempotent) script, and the approval summary should list exactly these deltas.
10. **`/gsd-ship` PR titles are not Conventional Commits** `[VERIFIED: ship.md lines 225-229]`: the title is `Phase {phase_number}: {phase_name}`. With squash merge and `squash_merge_commit_title: PR_TITLE` (GH-07) the commit subject on `main` will be that string, which release-please (Phase 12, GH-05) cannot classify. Not a Phase 1 blocker, but record it as an open question and do not add a Conventional-Commit PR-title check in Phase 1.

## Architectural Responsibility Map

The "tiers" of this phase are the places where a capability can live, not application tiers.

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Version pinning (JVM libs, plugins, tool versions) | Gradle catalog (`gradle/libs.versions.toml`) | `:platform` module | Single source per ecosystem (INFRA-09); `:platform` is the alignment point (INFRA-11) |
| JDK / Node version | `.java-version` / `.nvmrc` files | mise, setup-java, setup-node, Gradle convention | INFRA-09: only those files hold the value |
| CLI tool pinning | `mise.toml` + `mise.lock` | CI via `jdx/mise-action` | TOOL-02; lock file adds checksums and attestation provenance |
| Formatting | Gradle (Spotless in root plugin) | lefthook (staged files via Spotless IDE hook), CI `spotlessCheck` | One command locally, same engine in hook and CI |
| Kotlin static analysis, KDoc checks | Gradle (detekt, Dokka in module convention) | CI `check` | Cacheable tasks with declared inputs, wired into `check` |
| Build-logic quality and tests | `build-logic` own build (detekt, Dokka, TestKit) | Root `check` depends on `:build-logic:check` | Included build is configured separately; root plugin wires it in |
| Governance policy checks (config policy, version literals, daemon JVM, tooling table) | Gradle tasks in `build-logic` | CI `check` | D-13: cacheable, TestKit-tested |
| PR-context governance (label gate, bot-diff exemption, head-branch naming) | GitHub Actions workflow (`governance.yml`) | Ruleset required check | Needs the PR event payload |
| Secret scanning | lefthook (staged) + CI (full history) | GitHub secret scanning + push protection | Defence in depth |
| Workflow security linting | actionlint, zizmor, shellcheck (mise-pinned) | lefthook, CI | CICD-08 |
| Dependency and tool updates | Renovate (Mend app, in-repo config) | Dependabot alerts only | Minimum release age, grouping, bot exemption |
| Branch protection and repo settings | `.github/rulesets/main.json` + apply script (`gh api`) | CI read-only drift check | D-15; applied only after approval |
| Agent guardrails | `.claude/settings.json` `permissions.ask` | CI governance label | AIDEV-08 |

## Standard Stack

### Core (all versions checked against registries on 2026-11-21)

| Library / Tool | Version | Purpose | Verification |
|----------------|---------|---------|--------------|
| Gradle | 9.9.2 | Build (wrapper with `distributionSha256Sum`) | `[VERIFIED: services.gradle.org/versions/current]`; embeds Kotlin 2.4.21, Groovy 4.0.33 |
| Kotlin (Gradle plugin) | 2.4.21 | Language (2.5.0-RC is the registry "latest", not stable) | `[VERIFIED: Maven Central metadata]` |
| JDK | 25 (Temurin 25.0.5.1) | Toolchain, from `.java-version` = `25` | `[VERIFIED: java -version, mise ls]` |
| Spotless Gradle plugin | 8.11.0 (8.12.0 exists, released the research date) | Formatting orchestration | `[VERIFIED: Maven Central]`; prototype passed with 8.11.0 and 8.12.0 |
| ktfmt | 0.65 (`org.jetbrains.kotlinx:ktfmt`; coordinate moved from `com.facebook:ktfmt` at 0.65) | Kotlin + KDoc formatter, `kotlinlangStyle()` | `[CITED: Spotless plugin-gradle CHANGES 8.10.4]`; `[VERIFIED: Maven Central 0.65, 2026-10-07]` |
| Prettier (via Spotless) | 3.10.1 | JSON, YAML, Markdown | `[VERIFIED: npm view prettier version]` |
| detekt | `dev.detekt` 2.0.0-alpha.6 | Kotlin analysis incl. `comments` rules | `[VERIFIED: Maven Central; prototype]` |
| Dokka | `org.jetbrains.dokka` 2.3.0 | KDoc link checking (`failOnWarning`) | `[VERIFIED: Maven Central; prototype]` |
| foojay resolver | `org.gradle.toolchains:foojay-resolver` 1.0.0 (plugin id `org.gradle.toolchains.foojay-resolver-convention`) | Toolchain download for `updateDaemonJvm` | `[VERIFIED: plugin portal; prototype]` |
| JUnit | 6.0.3 (via `junit-bom` in `:platform`) | Test engine | `[VERIFIED: prototype resolved and ran]` |
| Kotest assertions | 6.2.7 (`kotest-assertions-core`) | Matchers | `[VERIFIED: Maven Central; prototype]` |
| mise | 2026.11.11 | CLI tool manager | `[VERIFIED: GitHub releases; binary SHA-256 checked]` |
| REUSE tool | 6.3.0 (`pipx:reuse` with `extras = "charset-normalizer"`) | `reuse lint` | `[VERIFIED: PyPI 6.3.0, 2026-10-30; prototype]` |
| lefthook | 2.2.2 | Git hook runner | `[VERIFIED: GitHub releases; prototype]` |
| gitleaks | 8.30.1 (CLI, not the action) | Secret scan (`gitleaks git`) | `[VERIFIED: releases; local run on this repo: no leaks]` |
| actionlint | 1.7.12 | Workflow lint | `[VERIFIED: releases]` (see Pitfall 15 about the mise "deprecated" flag) |
| zizmor | 1.32.0 | Workflow security audit | `[VERIFIED: releases, PyPI; prototype]` |
| shellcheck | 0.11.0 | Shell lint | `[VERIFIED: releases; prototype]` |
| editorconfig-checker | 4.0.2 | `.editorconfig` conformance for files no formatter owns | `[VERIFIED: releases; prototype]` |
| gradle-profiler | 0.26.0 (`github:gradle/gradle-profiler` in mise) | Performance scenarios | `[VERIFIED: releases; mise install ran]` |
| Renovate | Mend GitHub App (config validated with renovate 44.257.6) | Updates | `[VERIFIED: npm; config-validator --strict passed]` |

### GitHub Actions (resolved commit SHAs, 2026-11-21; re-resolve at execution time)

| Action | Tag | Commit SHA |
|--------|-----|-----------|
| actions/checkout | v7.0.1 | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| actions/setup-java | v6.3.0 | `d1374f8ac74474200d16ef8ecee22c7ae7a51880` |
| actions/setup-node | v7.1.0 | `949feb2413d6458794dcd2491c4babbbce0c15c1` |
| actions/upload-artifact | v7.0.2 | `cf430e030ddbb5b0abf93d22962f4752f3646cd9` |
| gradle/actions (setup-gradle) | v6.4.1 | `8da46b539cd8fe6ab27ca75128919a18d757604c` |
| jdx/mise-action | v5.2.10 | `90f833d396d0cc3674b631d2d225e306476c303a` |
| github/codeql-action | v4.39.4 | `e0462179768953fd6790193e15caaef674ff709e` (tag object SHA from `git/ref`; deref to the commit before use) |
| ossf/scorecard-action | v2.6.0 | `23f22cd43608bae7a1f1fa2603d7314dee9538a5` |
| zizmorcore/zizmor-action | v0.8.1 | `45d7276c9ca790aabd705e31ca99c6185c3e76dd` (optional; CI can run the mise-pinned CLI instead) |
| actions/dependency-review-action | v5.0.1 | `bbc44d14a204704ccc493733616cd1e93a0321ec` (Phase 6 scope, listed for completeness) |
| actions/github-script | v9.0.0 | `3a2844b7e9c422d3c10d287c895573f7108da1b3` |
| crazy-max/ghaction-github-labeler | v6.0.0 | `548a7c3603594ec17c819e1239f281a3b801ab4d` |

All `[VERIFIED: gh api repos/<repo>/git/ref/tags/<tag>]`; annotated tags were dereferenced for all rows except codeql-action, whose tag object must be dereferenced with `gh api repos/github/codeql-action/git/tags/<sha> --jq .object.sha` by the executor.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Prettier via Spotless (D-05) | Biome + extra tools | Locked by D-05 |
| `libs` typed accessor hack | `VersionCatalogsExtension.named("libs").findLibrary(...)` | Dynamic, no hack, works in any TestKit fixture, but not type-safe; INFRA-12 asks for type-safe access, so keep the hack and the `includeBuild` fixture |
| Spotless at root over the whole repo | Spotless per module | Per-module repeats Prettier setup and cannot see `build-logic`; root-only keeps one npm install |
| `crazy-max/ghaction-github-labeler` | `gh label create --force` script reading `labels.yml` | Script avoids a third-party action but needs a YAML parser (`yq`); the action is SHA-pinned and small |
| `gradle/actions` v6 default (proprietary `gradle-actions-caching`, free for public repos) | `cache-provider: basic` (MIT, wraps `actions/cache`) | Basic is fully open source; enhanced is faster. Decision for the author; see Assumption A3 |
| Renovate Mend app (D-11) | Self-hosted action | Locked by D-11 |

**Installation (contributor bootstrap):**
```bash
# one-time: install mise (brew install mise, or the pinned release binary), then
mise install          # JDK from .java-version, Node from .nvmrc, plus every CLI in mise.toml
./gradlew build       # wrapper downloads Gradle 9.9.2 with checksum validation
lefthook install      # or ./gradlew task that wraps it
```

**Wrapper bootstrap (executor, once):** the wrapper does not exist yet. Generate it with any Gradle 9.9.2 (for example `mise x gradle@9.9.2 -- gradle wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum 971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b`). `[VERIFIED: the sha256 equals the checksum published at services.gradle.org for gradle-9.9.2-bin.zip; the wrapper task accepted the flag in the prototype]`. The `mise x gradle@...` form itself is `[ASSUMED]`.

## Package Legitimacy Audit

Run through `gsd-tools query package-legitimacy check` on 2026-11-21. Gradle/Maven artifacts are outside the seam's ecosystems and were checked directly against Maven Central metadata and the Gradle plugin portal (all exist, all from the maintainers' known groups: `com.diffplug.spotless`, `dev.detekt`, `org.jetbrains.*`, `org.gradle.toolchains`, `io.kotest`, `org.junit`).

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| prettier | npm | many years (latest version published 2026-11-12) | ~196M/wk | github.com/prettier/prettier | SUS (`too-new`: refers to the latest *version*, not the package) | Approved; executed only by Spotless at build time. Planner adds one `checkpoint:human-verify` covering the tool list |
| renovate | npm | many years (latest version published 2026-11-21) | ~617k/wk | github.com/renovatebot/renovate | SUS (`too-new`, same reason) | Approved; used only as `renovate-config-validator` in an optional local check, never installed in CI by default |
| reuse | PyPI | 6+ years (6.3.0 published 2026-10-30) | unknown to the seam | reuse.software (FSFE) | SUS (`too-new`, `unknown-downloads`) | Approved; official FSFE tool named in the REUSE specification. Gate with the same human-verify checkpoint |
| charset-normalizer | PyPI | long-established | unknown to the seam | none listed | SUS (`unknown-downloads`, `no-repository`) | Approved; required extra of `reuse` (without it `reuse` crashes). Pinned transitively by `mise.lock`/uv |

**Packages removed due to SLOP verdict:** none.
**Packages flagged SUS:** prettier, renovate, reuse, charset-normalizer. All four are false positives of the `too-new` heuristic (it keys off the newest version's publish date). `npm view prettier scripts.postinstall` returned nothing. Because the protocol requires it, the planner inserts a single `checkpoint:human-verify` before the first `mise install`/Spotless run that confirms the tool list and versions; this coincides with the author-approval flow already planned for tooling.

## Architecture Patterns

### System Architecture Diagram

```
Developer / agent edit
        |
        v
  git commit --> lefthook pre-commit (fast)
        |          |- scripts/format-staged.sh -> ./gradlew spotlessApply -PspotlessIdeHook=<abs,paths>  (re-stage)
        |          |- gitleaks git --pre-commit --staged
        |          |- reuse lint
        |          '- actionlint / zizmor --offline / shellcheck  (changed files only)
        v
  git push --> GitHub PR (head: gsd/phase-*, renovate/*, release-please--*)
        |
        +--> ci.yml (read-only, SHA-pinned)
        |      |- build   : setup-java(.java-version) + setup-gradle + ./gradlew build  --> :spotlessCheck, :sample:check(detekt, dokka, test), :build-logic:check(TestKit)
        |      |- lint    : mise tools -> actionlint, zizmor, shellcheck, editorconfig-checker, reuse lint, git add --renormalize check
        |      |- secrets : fetch-depth 0 -> gitleaks git (full history)
        |      '- ci-success (needs all, if: always) ----------------------> REQUIRED CHECK #1
        +--> governance.yml (pull_request: opened, synchronize, reopened, labeled, unlabeled)
        |      |- files via REST API (no checkout of PR code)
        |      |- governance file touched? -> need label `governance-approved` (or bot+version-only-diff exemption)
        |      |- head-branch naming policy
        |      '- governance-success -----------------------------------> REQUIRED CHECK #2
        +--> codeql.yml (actions + java-kotlin manual build)  scorecard.yml (main + schedule)
        '--> labels.yml (push to main touching labels.yml) -> sync labels
                |
                v
   Ruleset on main: PR required, linear history, no force-push/deletion, squash only, required contexts above
                |
                v
   squash merge (title = PR title, body = PR body), head branch auto-deleted
        ^
        |  Renovate (Mend app) PRs: grouped, minimumReleaseAge 7 days, automerge only for action digests + dev-tool patches
   Apply script (gh api, author-approved) <-> CI drift check (read token secret, approval gate)
```

### Recommended Project Structure

```
.
|- .java-version                  # "25" (single JDK source)
|- .nvmrc                         # "24" (single Node source)
|- mise.toml  mise.lock           # CLI tools; lock = checksums + provenance
|- gradle/
|  |- libs.versions.toml          # all versions; renovate annotation comments for npm entries
|  |- gradle-daemon-jvm.properties  # generated by :updateDaemonJvm from .java-version
|  '- wrapper/ (gradle-wrapper.properties with distributionSha256Sum, jar)
|- gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts
|- build-logic/                   # included build (kotlin-dsl)
|  |- settings.gradle.kts  build.gradle.kts
|  '- src/{main,functionalTest}/kotlin/...
|     courses.settings.settings.gradle.kts   # foojay + repositories (settings plugin)
|     courses.root.gradle.kts                # Spotless for the whole repo + wires :build-logic:check
|     courses.kotlin-library.gradle.kts      # Kotlin, platform(), detekt, Dokka, JUnit/Kotest
|- platform/build.gradle.kts      # java-platform
|- tooling/convention-sample/     # sample module (name/location = discretion, see Open Questions)
|- config/{detekt/detekt.yml, spotless/license-header.txt}
|- governance/gsd-config-policy.json
|- docs/{tooling.md, adr/}
|- scripts/{format-staged.sh, apply-github-settings.sh, ...}
|- .github/{workflows/*.yml, rulesets/main.json, labels.yml, PULL_REQUEST_TEMPLATE.md}
|- renovate.json5  lefthook.yml  performance.scenarios
|- .editorconfig  .editorconfig-checker.json  .gitattributes  .gitignore
|- REUSE.toml  LICENSES/{MIT.txt,Apache-2.0.txt}  LICENSE
'- .claude/settings.json         # ask rules (tracked via existing .gitignore negation)
```

`settings.gradle.kts` must anticipate `services/*`, `contracts/*`, `frontend/`, `docs-site/` (D-03). Phase 1 includes only `:platform` and the sample. Keep the `include(...)` list explicit; add a comment, not empty directories.

### Pattern 1: Settings plugin plus included build (no version literal in settings)

Plugin versions cannot come from the catalog inside `settings.gradle.kts` `plugins {}`. Move them into a precompiled **settings plugin** in `build-logic`. `[VERIFIED: prototype; isolated projects + configuration cache green]`

```kotlin
// settings.gradle.kts
pluginManagement { includeBuild("build-logic") }
plugins { id("courses.settings") }
rootProject.name = "axon5-dcb-courses"
include(":platform", ":tooling:convention-sample")   // names are placeholders
```
```kotlin
// build-logic/src/main/kotlin/courses.settings.settings.gradle.kts
plugins { id("org.gradle.toolchains.foojay-resolver-convention") }
dependencyResolutionManagement { repositories { mavenCentral() } }
```
```kotlin
// build-logic/settings.gradle.kts
dependencyResolutionManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}
rootProject.name = "build-logic"
```
```kotlin
// build-logic/build.gradle.kts
plugins {
    `kotlin-dsl`
    alias(libs.plugins.detekt)   // build-logic formats and analyses itself
    alias(libs.plugins.dokka)
}
dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.foojay.resolver.plugin)
    // required for the typed `libs` accessor inside precompiled script plugins (still needed on 9.9.2)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
```
**Trap:** once `courses.settings` is on the classpath, the root build must not request `com.diffplug.spotless` (or any other build-logic dependency) with `alias(libs.plugins.spotless)`: Gradle fails with "plugin is already on the classpath with an unknown version". Apply tool plugins through convention plugins by plain id (Pattern 2). The `[plugins]` catalog section is then only used by `build-logic/build.gradle.kts` itself.

### Pattern 2: Root plugin formats the whole repository (including `build-logic`) and wires `build-logic` into `check`

`[VERIFIED: prototype]`. Use a FileTree target, never a `**`-prefixed string (Finding 3). `.planning/**` and `.claude/**` are excluded because GSD owns their formatting and would otherwise fail `spotlessCheck` on every generated document (see Open Questions for `.claude/settings.json`).

```kotlin
// build-logic/src/main/kotlin/courses.root.gradle.kts
plugins { id("com.diffplug.spotless") }
val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

val excluded = listOf("**/build/**", "**/.gradle/**", "**/.kotlin/**", "**/node_modules/**",
    "**/.git/**", "**/.idea/**", "**/.planning/**", "**/.claude/**", "**/.mise/**", "LICENSES/**")
fun sources(vararg globs: String) =
    layout.projectDirectory.asFileTree.matching { include(*globs); exclude(excluded) }

spotless {
    kotlin {
        target(sources("**/*.kt"))
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
        licenseHeaderFile("config/spotless/license-header.txt")
    }
    kotlinGradle {
        target(sources("**/*.kts"))
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
        // delimiter [^/\s] = first non-comment, non-blank line (see Pitfall 8)
        licenseHeaderFile("config/spotless/license-header.txt", "[^/\\s]")
    }
    format("prettier") {
        target(sources("**/*.json", "**/*.yml", "**/*.yaml", "**/*.md"))
        prettier(libs.versions.prettier.get()).configFile(".prettierrc.yaml")   // align with .editorconfig
    }
}
tasks.named("check") { dependsOn(gradle.includedBuild("build-logic").task(":check")) }  // proven under isolated projects
```
`config/spotless/license-header.txt` contains exactly `// SPDX-License-Identifier: MIT` (one line). `[VERIFIED]` that `.kts` files get the header before the first statement and that re-running is idempotent.

### Pattern 3: Module convention plugin (`courses.kotlin-library`) and `:platform`

`[VERIFIED: prototype]`. The JDK comes from `.java-version` read through the isolated-projects-safe API `isolated.rootProject.projectDirectory` (using `rootProject.layout` is a violation).

```kotlin
// build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
plugins { `java-library`; id("org.jetbrains.kotlin.jvm"); id("dev.detekt"); id("org.jetbrains.dokka") }
val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencies {
    implementation(platform(project(":platform")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotest.assertions.core)
}
val javaVersion = providers.fileContents(isolated.rootProject.projectDirectory.file(".java-version"))
    .asText.map { it.trim().substringBefore('.').toInt() }
kotlin {
    jvmToolchain { languageVersion.set(javaVersion.map(JavaLanguageVersion::of)) }
    compilerOptions { allWarningsAsErrors.set(true) }
}
tasks.withType<Test>().configureEach { useJUnitPlatform() }
dokka { dokkaPublications.html { failOnWarning.set(true) } }
detekt { buildUponDefaultConfig.set(true) }     // overrides live in config/detekt/detekt.yml
tasks.named("check") { dependsOn(tasks.named("dokkaGeneratePublicationHtml")) }
```
```kotlin
// platform/build.gradle.kts   (INFRA-11)
plugins { `java-platform` }
javaPlatform { allowDependencies() }
dependencies {
    api(platform(libs.junit.bom))             // BOMs through the catalog
    constraints { api(libs.kotest.assertions.core) }   // non-BOM libs and overrides
}
```
Apply `platform(project(":platform"))` to every relevant configuration (main now; test suites, `testFixtures` and load tests when Phase 2 adds them: `testImplementation`, `testFixturesImplementation`, etc.). A Phase 1 TestKit test should assert the platform is applied to `implementation` and `testImplementation`.

### Pattern 4: detekt strict configuration (comments on, no baseline)

`[VERIFIED: prototype failed the build on undocumented class/function/property and on a non-punctuated first sentence]`. `config/detekt/detekt.yml` (picked up automatically as the default location):
```yaml
comments:
  UndocumentedPublicClass: { active: true }
  UndocumentedPublicFunction: { active: true }
  UndocumentedPublicProperty: { active: true }
  OutdatedDocumentation: { active: true }
  KDocReferencesNonPublicProperty: { active: true }
  EndOfSentenceFormat: { active: true }
```
Never create a baseline. Also apply detekt to `build-logic` (its own build script) and share the same config file. detekt analyses `*.gradle.kts` under `build-logic/src/main/kotlin` and applies `MaxLineLength` (120); ktfmt cannot wrap long string literals, so keep such strings short or built from parts.

### Pattern 5: Node for Spotless/Prettier (the D-05 question, settled)

Spotless auto-discovers `npm` on `PATH`; with no Node it fails with `Can't automatically determine npm executable` `[VERIFIED: prototype with a stripped PATH]` `[CITED: Spotless README, "npm detection"]`. Provide Node from the **single source** `.nvmrc` by environment, not by Gradle:
- Locally: mise reads `.nvmrc` through `[settings] idiomatic_version_file_enable_tools = ["node", "java"]` (`mise ls` showed `node 24.22.3` sourced from `.nvmrc` and `java 25.0.5.1+2.0.LTS` from `.java-version`) `[VERIFIED]`.
- CI: `actions/setup-node` with `node-version-file: .nvmrc` and `actions/setup-java` with `java-version-file: .java-version`, `distribution: temurin` (INFRA-09 names these actions). Run `jdx/mise-action` with `MISE_DISABLE_TOOLS=java,node` so mise installs only CLI tools `[VERIFIED: mise ls hides java/node with that variable]`.
- Do **not** add the Gradle Node plugin in Phase 1; it belongs to Phase 5 (INFRA-08) and would duplicate version reading.
- Add a `verifyNodeVersion` safeguard: a small task that compares `node --version`'s major with `.nvmrc` and is declared an **input** of the Prettier Spotless tasks. This also closes Pitfall 4 (stale cached lint failures and node switches). Pin Prettier through `libs.versions.prettier` with a Renovate annotation comment (`# renovate: datasource=npm depName=prettier`), proven to extract (Pattern 12). Spotless 8.12 adds `dependencyOverrides(Map)` to pin transitive npm packages `[CITED: Spotless CHANGES 8.12.0]`; consider it once on 8.12+.

### Pattern 6: TestKit functional tests for conventions (config cache + isolated projects)

`[VERIFIED: prototype, passes warm in about 6 s]`. Dedicated `functionalTest` JVM test suite in `build-logic`, wired into `check`, `gradlePlugin { testSourceSets(...) }`, system properties declared as task inputs (cacheable).
```kotlin
// build-logic/build.gradle.kts (excerpt)
testing { suites { register<JvmTestSuite>("functionalTest") {
    useJUnitJupiter(libs.versions.junit)
    dependencies { implementation(gradleTestKit()); implementation(libs.kotest.assertions.core) }
    targets.configureEach { testTask.configure {
        shouldRunAfter("test")
        val catalog = layout.projectDirectory.file("../gradle/libs.versions.toml")
        inputs.file(catalog).withPropertyName("catalog")
        systemProperty("catalog", catalog.asFile.absolutePath)
        systemProperty("buildLogic", layout.projectDirectory.asFile.absolutePath)
    } }
} } }
gradlePlugin { testSourceSets(sourceSets["functionalTest"]) }
tasks.named("check") { dependsOn("functionalTest") }
```
Fixture `settings.gradle.kts` text: `pluginManagement { includeBuild("${System.getProperty("buildLogic")}") }` plus `dependencyResolutionManagement { versionCatalogs { create("libs") { from(files("${System.getProperty("catalog")}")) } } }`. Run the fixture twice with `--configuration-cache --isolated-projects` and assert `Reusing configuration cache` on the second run. Do not use `withPluginClasspath()` for plugins that use the typed `libs` accessor. Dokka on `build-logic` needs `dokkaSourceSets.configureEach { sourceRoots.setFrom(layout.projectDirectory.dir("src/main/kotlin")) }` because the `kotlin-dsl` generated plugin adapters carry unresolvable KDoc links that otherwise fail `failOnWarning` (`suppressedFiles` did not help) `[VERIFIED]`.

### Pattern 7: lefthook pre-commit (fast)

`[VERIFIED: prototype; the format job took about 2 s and re-staged files; the failing `reuse` job correctly aborted the commit]`.
```yaml
# lefthook.yml
min_version: 2.2.2
pre-commit:
  parallel: false
  jobs:
    - name: format
      glob: "*.{kt,kts,json,yml,yaml,md}"
      run: scripts/format-staged.sh {staged_files}
      stage_fixed: true
    - { name: gitleaks, run: gitleaks git --pre-commit --staged --no-banner --redact }
    - { name: reuse, run: reuse lint --quiet }
    - { name: actionlint, glob: ".github/workflows/*.{yml,yaml}", run: actionlint {staged_files} }
    - { name: zizmor, glob: ".github/workflows/*.{yml,yaml}", run: zizmor --offline --no-progress {staged_files} }
    - { name: shellcheck, glob: "*.sh", run: shellcheck {staged_files} }
```
`scripts/format-staged.sh` converts the staged relative paths to a comma-separated absolute list for `./gradlew --quiet spotlessApply -PspotlessIdeHook=<paths>`. The Gradle plugin does **not** read `-PspotlessFiles`; the IDE hook is the supported way `[CITED: Spotless CHANGES 8.12.0 fix #3183]`. Each distinct hook argument invalidates the configuration cache entry (about 1 to 3 s per commit). Install the hook with `lefthook install`, wrapped in a documented one-liner. `zizmor` runs `--offline` in the hook (its online audits need a token).

### Pattern 8: REUSE configuration

`[VERIFIED: prototype reached "compliant with version 3.3"]`
```toml
# REUSE.toml
version = 1

[[annotations]]
path = "**"
precedence = "aggregate"
SPDX-FileCopyrightText = "2026 Alexey Afanasyev"
SPDX-License-Identifier = "MIT"

[[annotations]]
path = ["gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar"]
precedence = "override"
SPDX-FileCopyrightText = "Gradle Inc. and the original authors"
SPDX-License-Identifier = "Apache-2.0"
```
Also commit `LICENSES/MIT.txt` and `LICENSES/Apache-2.0.txt` (`reuse download MIT Apache-2.0`) and keep the root `LICENSE` for GitHub's licence detection. `reuse` ignores git-ignored files, but only once the repository has a commit. Hook and CI: `reuse lint`. The README badge is Phase 13 (GH-02) except for the INFRA-16 clause "README shows the REUSE badge": add the badge line in the Phase 1 README stub or record it as satisfied when the README lands (Open Question 6).

### Pattern 9: Governance Gradle tasks (D-13) and the `docs/tooling.md` cross-check

Write them as `build-logic` classes (`@CacheableTask`, `@InputFile`/`@InputFiles` with `PathSensitivity.RELATIVE`, `@OutputFile` marker, no `Project` access at execution), wired into the root `check` by `courses.root`. Each gets a TestKit test.
- **`.planning/config.json` policy (AIDEV-05):** compare parsed JSON of `.planning/config.json` with `governance/gsd-config-policy.json` (semantic equality, key order and whitespace ignored). Report added, removed and changed key paths. Local detail: `.planning/config.json` today pins `git.branching_strategy: "phase"`, `git.create_tag: false`, `git.phase_branch_template: "gsd/phase-{phase}-{slug}"` `[VERIFIED: config.json lines 13-20]`; the policy file starts as an exact copy.
- **Version literals (INFRA-09):** scan `**/*.gradle.kts` (and `gradle.properties` for dependency versions) for GAV strings with a version (`"group:artifact:version"`), `version = "..."`, `version("...")`, and `id("...") version "..."`; allow no exceptions (the settings-plugin pattern removes the only legitimate one). Plugin versions of `build-logic`'s own plugins resolve from the catalog.
- **JDK consistency:** `gradle/gradle-daemon-jvm.properties` `toolchainVersion` must equal the major version in `.java-version`. The generated file holds `toolchainVersion=25` plus foojay `toolchainUrl.<OS>.<ARCH>` entries `[VERIFIED: updateDaemonJvm output]`. Configure `tasks.updateDaemonJvm { languageVersion = JavaLanguageVersion.of(<from .java-version>) }` in the root plugin so regeneration needs no CLI argument (`updateDaemonJvm --jvm-version` is the CLI form `[CITED: docs.gradle.org gradle_daemon]`).
- **Tooling table:** parse `mise.toml` `[tools]` keys (keep backend-prefixed keys such as `pipx:reuse` and `github:gradle/gradle-profiler` verbatim) and the `docs/tooling.md` table; fail on tools missing in either direction (D-17). A TOML parser dependency in `build-logic` (for example `org.tomlj:tomlj`) is `[ASSUMED]`; a line-based reader of the small `[tools]` table is the zero-dependency alternative.
- **Working-tree normalisation (INFRA-13):** a CI step, not a Gradle task: `git add --renormalize . && git diff --cached --exit-code` `[VERIFIED: returned exit code 1 on a CRLF file committed before `.gitattributes`]`.

### Pattern 10: `mise.toml` plus `mise.lock`

`[VERIFIED: mise install of all tools succeeded; mise lock produced mise.lock with per-platform checksums and github-attestations provenance]`
```toml
[settings]
idiomatic_version_file_enable_tools = ["node", "java"]   # .nvmrc and .java-version stay the only sources
lockfile = true                                           # [ASSUMED] exact setting name; `mise lock` creates mise.lock regardless

[tools]
gitleaks = "8.30.1"
actionlint = "1.7.12"
shellcheck = "0.11.0"
lefthook = "2.2.2"
zizmor = "1.32.0"
editorconfig-checker = "4.0.2"
"pipx:reuse" = { version = "6.3.0", extras = "charset-normalizer" }
"github:gradle/gradle-profiler" = "0.26.0"
```
`jdx/mise-action` pins: `version: 2026.11.11`, optionally `sha256:` of the mise binary; with a `mise.lock` present the action adds `--locked` automatically `[CITED: mise-action action.yml]`. Renovate supports `mise.lock` updates (`lockedVersion` extraction, `mise lock --bump` maintenance) `[CITED: Renovate mise readme]`. Treat `mise.lock` (and `.mise/locks/**` sidecars if generated) as governance files alongside `mise.toml`. `docs/tooling.md` also lists non-mise tools (git, gh, Docker engine, JDK, Node, mise itself) per TOOL-01; only the mise-backed rows are cross-checked.

### Pattern 11: `ci.yml` skeleton (passes actionlint and zizmor)

`[VERIFIED: actionlint 1.7.12 rc 0 and zizmor 1.32.0 "No findings" on a structurally equivalent block-style version with checkout, mise-action, setup-gradle and the `ci-success` job; the setup-java/setup-node steps and the flow-style YAML below were not linted, so the executor must re-run both linters]`
```yaml
name: CI
on:
  push: { branches: [main] }
  pull_request:
concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
permissions: {}
jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    permissions: { contents: read }
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
        with: { persist-credentials: false }
      - uses: actions/setup-java@d1374f8ac74474200d16ef8ecee22c7ae7a51880 # v6.3.0
        with: { distribution: temurin, java-version-file: .java-version }
      - uses: actions/setup-node@949feb2413d6458794dcd2491c4babbbce0c15c1 # v7.1.0
        with: { node-version-file: .nvmrc }
      - uses: gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1
        with: { cache-read-only: "${{ github.ref != 'refs/heads/main' }}" }
      - run: ./gradlew build
  secrets:
    runs-on: ubuntu-latest
    permissions: { contents: read }
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
        with: { fetch-depth: 0, persist-credentials: false }
      - uses: jdx/mise-action@90f833d396d0cc3674b631d2d225e306476c303a # v5.2.10
        env: { MISE_DISABLE_TOOLS: "java,node" }
      - run: gitleaks git --redact --no-banner
  ci-success:
    needs: [build, secrets]   # plus lint, codeql is separate
    if: always()
    runs-on: ubuntu-latest
    permissions: {}
    steps:
      - name: Verify all required jobs passed
        env: { NEEDS: "${{ toJSON(needs) }}" }
        run: echo "$NEEDS" | jq -e 'all(.[]; .result == "success")' >/dev/null
```
The `needs` JSON goes through `env:` to stay template-injection-safe (zizmor). A `lint` job runs the mise tools (actionlint, zizmor, shellcheck, editorconfig-checker, `reuse lint`, the normalisation check). `cache-read-only` is also the default behaviour (writes only from the default branch) `[CITED: setup-gradle docs]`; make it explicit. `cache-encryption-key` (a secret, base64 AES key) encrypts the configuration cache; creating that secret is an approval gate. Gradle's remote build cache is not used; the local build cache works through `setup-gradle`'s cache. Do not run Build Scans in CI (D-20). `pull_request` runs get a read-only cache by default.

`codeql.yml`: languages `actions` and `java-kotlin`. Kotlin analysis requires a build, `build-mode: none` does not cover Kotlin (a warning is emitted and Kotlin is skipped) `[CITED: docs.github.com compiled-languages]`, so use `build-mode: manual` with a Gradle compile step; CodeQL lists Kotlin "1.8.0 to 2.4.2x" and Java 7 to 26 `[CITED: codeql.github.com supported-languages]`. Gradle flags that make the extractor see a compile (no build cache, no daemon) are `[ASSUMED]`; treat the Kotlin leg as a spike and keep the `actions` leg regardless.

`scorecard.yml`: `ossf/scorecard-action` on push to `main`, `branch_protection_rule` and a schedule, `permissions: {}` at workflow level and `security-events: write`, `id-token: write`, `contents: read`, `actions: read` on the job, `publish_results: true` (public repo) `[ASSUMED: standard Scorecard setup]`.

### Pattern 12: Renovate configuration (validated, extraction proven)

`[VERIFIED: renovate-config-validator --strict passed; renovate --platform=local --dry-run=extract found github-actions, gradle (catalog libraries, plugins, settings plugin), gradle-wrapper, mise (all tools including pipx:reuse), nvm (.nvmrc) and the two custom regex dependencies (java in .java-version, prettier in the catalog)]`
```json5
// renovate.json5
{
  $schema: "https://docs.renovatebot.com/renovate-schema.json",
  extends: ["config:best-practices", "helpers:pinGitHubActionDigestsToSemver"],
  minimumReleaseAge: "7 days",
  internalChecksFilter: "strict",
  customManagers: [
    { customType: "regex", description: "JDK major version in .java-version",
      managerFilePatterns: ["/(^|/)\\.java-version$/"],
      matchStrings: ["^(?<currentValue>\\d+(?:\\.\\d+)*)\\s*$"],
      depNameTemplate: "java", datasourceTemplate: "java-version", versioningTemplate: "loose" },
    { customType: "regex", description: "Annotated versions in the Gradle version catalog",
      managerFilePatterns: ["/(^|/)gradle/libs\\.versions\\.toml$/"],
      matchStrings: ["# renovate: datasource=(?<datasource>\\S+) depName=(?<depName>\\S+)\\s+\\S+ = \"(?<currentValue>[^\"]+)\""] },
  ],
  // packageRules to add (not yet validated): group kotlin+ksp+plugins, group junit, labels deps/deps:<ecosystem>,
  // automerge only github-actions digests and mise/spotless/detekt/dokka patch bumps (D-12), allowedVersions to
  // keep java on LTS majors, semanticCommits so PR titles are `build(deps): ...`.
}
```
Facts that shape the plans:
- Renovate has **no built-in manager for `.java-version`** (the managers directory lists `nvm` for `.nvmrc` and `mise`, but no jenv/java-version manager) `[VERIFIED: GitHub contents API listing]`; hence the custom regex above. The `java-version` datasource returns Adoptium GA releases including non-LTS majors, so an `allowedVersions` rule limiting to LTS majors is needed `[ASSUMED]`.
- A JDK bump PR will change `.java-version` but not `gradle/gradle-daemon-jvm.properties` (the file is generated and holds foojay URLs); the D-13 consistency check will fail that PR until `./gradlew updateDaemonJvm` is run on the branch. Label such PRs and document the manual step; Mend's hosted app cannot run it.
- `minimumReleaseAge` ages digest updates against the **matched version's** timestamp, so it does apply to action digest pins; for `github-tags` it uses the commit date and cannot detect a force-pushed tag; `prCreation: "not-pending"` needs `internalChecksAsSuccess: true` when CI runs only on `pull_request` `[CITED: docs.renovatebot.com key-concepts/minimum-release-age, configuration-options]`. This resolves the earlier `[VERIFY]` in PITFALLS 28.
- The Mend app proposes updates from `renovate.json5` at the repository root; installing the app is an approval gate (D-11). For automerge (D-12) the repository also needs `allow_auto_merge: true` (Pattern 15).

### Pattern 13: Gradle performance settings and gradle-profiler scenarios (INFRA-10, D-20)

`gradle.properties` `[VERIFIED: used in the prototype]`:
```properties
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=fail
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.isolated-projects=true
org.gradle.jvmargs=-Xmx1g -XX:+UseParallelGC
kotlin.daemon.jvmargs=-Xmx1g
```
File-system watching is on by default; do not add `org.gradle.vfs.watch=false`. Heap sizes are placeholders to tune against the baseline run. Build Scan stays opt-in: do not apply the Develocity/scan plugin unconditionally. `performance.scenarios` accepted by gradle-profiler 0.26.0 (`--dry-run` parsed it) with `clean_build`, `noop_build`, `abi_change` (`apply-abi-change-to`), `config_cache_hit` scenarios `[VERIFIED: parse only; no benchmark was run]`. gradle-profiler writes `profile-out/` and a `gradle-user-home/` into the project; both go into `.gitignore`.

### Pattern 14: `.gitattributes`, `.editorconfig`, `.gitignore`

- `.gitattributes` per INFRA-13; verification step above. Mark `gradle/wrapper/gradle-wrapper.jar` `binary`, keep `*.bat`/`*.cmd` `eol=crlf`, `gradlew` and `*.sh` `eol=lf`.
- `.editorconfig`: `[*.{kt,kts}] indent_size = 4, max_line_length = 100`; `[*.{json,json5,yml,yaml,md,toml,properties}] indent_size = 2, max_line_length = off`; `[*.md] trim_trailing_whitespace = false`; `[*.bat] end_of_line = crlf` `[VERIFIED: editorconfig-checker passed on the prototype]`. Prettier in Spotless does not auto-discover config; commit `.prettierrc.yaml` and pass it with `.configFile(...)` so tab width agrees. ktfmt's kotlinlang style is 4-space indent and 100 columns `[VERIFIED: ktfmt output]`.
- `editorconfig-checker` runs in the lint job and the hook-free CI. `.editorconfig-checker.json`: `{"Exclude": ["^LICENSES/", "gradle/wrapper/gradle-wrapper\\.jar$"], "Disable": {"MaxLineLength": true}}` `[VERIFIED]` (ktfmt cannot break strings, so MaxLineLength cannot be enforced for Kotlin; ktfmt and detekt own line length).
- `.gitignore`: extend, do not replace. Existing content, quoted: `/.idea/`, then `/.claude/*`, `!/.claude/settings.json`, `!/.claude/CLAUDE.md` `[VERIFIED: .gitignore lines 1-6]`. Add Gradle (`.gradle/`, `build/`, `.kotlin/`), Node/pnpm, IDE, `.env`, test/Playwright/coverage artifacts, `profile-out/`, `gradle-user-home/`, `.mise/` only if sidecar locks are not committed.

### Pattern 15: GitHub settings as code (D-15; approval gate, never applied by CI)

`.github/rulesets/main.json` and `scripts/apply-github-settings.sh` (shellchecked, `set -euo pipefail`, read current state, apply only differences, print what it changed, support `--dry-run`).
- Ruleset (`POST /repos/{owner}/{repo}/rulesets`, upsert by name): `target: "branch"`, `enforcement: "active"`, `conditions.ref_name.include: ["~DEFAULT_BRANCH"]`, rules `deletion`, `non_fast_forward`, `required_linear_history`, `pull_request` with `allowed_merge_methods: ["squash"]`, `required_approving_review_count: 0`, `dismiss_stale_reviews_on_push`, `require_code_owner_review`, `require_last_push_approval`, `required_review_thread_resolution` (all required booleans), and `required_status_checks` with `strict_required_status_checks_policy` and the contexts from Finding 7 `[CITED: docs.github.com REST rulesets]`. `evaluate` enforcement is Enterprise-only, so there is no dry-run in the platform; use the script's own `--dry-run`.
- Bypass for admins only: `bypass_actors` with `actor_type: "RepositoryRole"`; the admin role id `5` is `[ASSUMED]`; the script must read the role back after creation. `bypass_mode: "pull_request"` (bypass only through a PR) is stricter than `"always"`; recommend `pull_request` and let the author choose at the approval gate.
- Repo PATCH `/repos/{owner}/{repo}` `[CITED: docs.github.com REST repos]`: `allow_squash_merge: true`, `allow_merge_commit: false`, `allow_rebase_merge: false`, `squash_merge_commit_title: "PR_TITLE"`, `squash_merge_commit_message: "PR_BODY"`, `delete_branch_on_merge: true`, `allow_auto_merge: true` (needed by D-12), and `has_wiki: false` (GH-01 is Phase 13, so list it as optional in the approval summary).
- Security features: `secret_scanning` and `secret_scanning_push_protection` through `security_and_analysis` (already `enabled`), Dependabot alerts via `PUT /repos/{owner}/{repo}/vulnerability-alerts`, private vulnerability reporting via `PUT /repos/{owner}/{repo}/private-vulnerability-reporting`, optional `automated-security-fixes` `[CITED]`.
- The read-only drift check needs a token with admin read; reading rulesets of a public repository may work unauthenticated `[ASSUMED]`. Creating the secret and enabling the drift job are approval gates; until then the job must be absent or skipped, not failing.
- **Head-branch naming cannot be enforced by a ruleset on `main`**; enforce `gsd/phase-*` (plus the `renovate/*` and `release-please--*` bot branches) in `governance.yml`.

### Pattern 16: PR template mirroring `/gsd-ship`

`/gsd-ship` core sections, quoted from `ship.md`: `## Summary`, `## Changes`, `## Requirements Addressed`, `## Verification`, `## Key Decisions`; then the append-only configured sections from `.planning/config.json` `ship.pr_body_sections`: `User Stories & Acceptance Criteria`, `Risks & Dependencies`, `Success Metrics & Release Criteria`, `Stakeholder Review & Approval` `[VERIFIED: ship.md lines 235-276; config.json lines 57-77]`. Optional trailing `## TDD Audit` and a `gate-status:` trailer appear only when TDD mode produced them. `.github/PULL_REQUEST_TEMPLATE.md` should contain these headings in this order, followed by a short checklist (conventions, governance label if governance files change). Prettier formats Markdown, so write the template in its normal form. A CI governance step may check that the PR body contains the five core headings for PRs from `gsd/phase-*` branches only (optional).

### Pattern 17: `governance.yml`

Triggers `pull_request` (types `opened`, `synchronize`, `reopened`, `labeled`, `unlabeled`, `edited`); never `pull_request_target`. Permissions `contents: read` and `pull-requests: read` on the job. Steps: list changed files via `gh api repos/.../pulls/<n>/files --paginate` (no checkout of PR code), match against the governance globs (`.planning/config.json`, `.claude/settings.json`, `.mcp.json`, `.github/**`, `mise.toml`, `mise.lock`, `renovate.json5`, `CODEOWNERS`, `governance/**`), require label `governance-approved` unless exempt. **Exemption (D-16):** `github.event.pull_request.user.login == 'renovate[bot]'` **and** every changed line in governance files matches a version/digest-only pattern (checked from the diff, for example `gh pr diff` or the files API `patch` field). Keep the exemption script in `scripts/governance/` with shellcheck and tests (a TestKit-style fixture or bats-free shell test). Final job `governance-success`, `if: always()`, `needs:` the others. Add the head-branch naming check here. **The label must exist before the first PR that needs it**, see Pitfall 11.

### Pattern 18: `.claude/settings.json` `ask` rules (AIDEV-08)

Rule syntax `[CITED: code.claude.com/docs/en/permissions]`: `permissions.ask` array; evaluation order deny, then ask, then allow (an ask rule prompts even when a more specific allow matches, so the local `Edit(.planning/*)` allow in `settings.local.json` does not suppress it); `Edit(path)` applies to all built-in file-editing tools; paths use gitignore syntax and `/path` anchors at the project root in project settings; `Bash` wildcards may appear anywhere, and Bash rules are explicitly not a security boundary (a different invocation form such as `sh -c` is not matched).
```json
{
  "permissions": {
    "ask": [
      "Edit(/.planning/config.json)",
      "Edit(/.claude/settings.json)",
      "Edit(/.mcp.json)",
      "Edit(/.github/**)",
      "Edit(/mise.toml)",
      "Edit(/mise.lock)",
      "Edit(/renovate.json5)",
      "Edit(/governance/**)",
      "Bash(*config-set*)"
    ]
  }
}
```
Rules for the `.claude/` directory are `[ASSUMED]` to behave like any project path (some `.claude` paths may be protected paths with their own prompts). Remember that GSD workflows call `config-set` (Finding 8): the prompt is intended. Residual risk to document: a Bash command that writes a governance file through a redirect is checked against Edit rules only for forms Claude Code can analyse `[CITED]`, so CI (`governance.yml` + policy task) remains the actual enforcement; optional hardening is a PreToolUse hook.

### Anti-Patterns to Avoid
- **`alias(libs.plugins.x)` in the root build for any plugin that build-logic also carries:** fails with "already on the classpath with an unknown version".
- **`target("**/*.kt")` strings in Spotless:** break isolated projects.
- **`withPluginClasspath()` for plugins using the typed `libs` accessor.**
- **Reading `.java-version` via `rootProject.layout`:** use `isolated.rootProject.projectDirectory`.
- **`licenseHeader("// SPDX-...")` string literals in build files:** poison `reuse lint`.
- **A detekt baseline, or relying on default-on `comments` rules:** they are off.
- **Gradle Node plugin in Phase 1:** duplicates version handling; Phase 5.
- **`gitleaks/gitleaks-action` and `aquasecurity/trivy-action` by tag:** already excluded by project decisions; keep using the CLI via mise.
- **A Renovate cooldown below the repository's supply-chain stance:** do not pin tools released the same day (Spotless 8.12.0 was published on the research date).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Kotlin and KDoc formatting | Custom KDoc reflow or ktlint | Spotless + ktfmt kotlinlang style | ktfmt reflows KDoc to 100 columns, inserts a blank line before block tags, normalises lists, leaves code fences alone `[VERIFIED: ktfmt 0.65 run]` |
| JSON/YAML/Markdown formatting | Custom scripts | Prettier via Spotless | D-05 |
| Licence headers | Header-adding scripts | Spotless `licenseHeaderFile` + `REUSE.toml` + `reuse lint` | Idempotent, checkable |
| Tool installation and pinning | Bash installers, `curl | sh` | mise + `mise.lock` | Checksums, attestations, Renovate support |
| Hook management | Hand-written `.git/hooks` | lefthook | `stage_fixed`, globs, parallelism |
| Workflow security review | Regex greps on YAML | actionlint + zizmor | Template injection, unpinned actions, excessive permissions |
| Secret scanning | Custom regexes | gitleaks CLI | Maintained rule set, full-history mode |
| Dependency updates | Scheduled scripts | Renovate | Digest pinning, release-age gating, mise lock updates |
| Required-check aggregation | Many required contexts | One `ci-success` job (+ `governance-success`) | Ruleset stays stable as jobs change |
| Repo settings drift | Manual clicking | Ruleset JSON + `gh api` apply script + read-only drift check | D-15 |
| Daemon JVM file | Hand-edited properties | `updateDaemonJvm` task | Generated format incl. foojay URLs |
| Governance diff parsing for bot exemption | Regex over raw `git diff` text of arbitrary files | Files API `patch` per file with an allow-listed line pattern, covered by tests | The exemption must stay narrow (D-16) |

**Key insight:** every guard in this phase already exists as a maintained tool; the only genuinely custom code is the governance checks (config policy, version literals, daemon-JVM and tooling-table consistency) and the bot-diff exemption, and those are small, deterministic and TestKit-testable.

## Common Pitfalls

### Pitfall 1: Typed `libs` accessor fails in TestKit or does not compile
**What goes wrong:** `Unresolved reference 'accessors'`, or `Extension of type 'LibrariesForLibs' does not exist` in fixtures. **Why:** the accessor jar is not on build-logic's classpath; with injected plugin classpath the class identity differs. **Avoid:** the `files(libs.javaClass.superclass...)` dependency and `includeBuild`-based fixtures (Pattern 6). **Warning sign:** a TestKit test that only fails when it applies a plugin using `libs`.

### Pitfall 2: Spotless `**` targets under isolated projects
**What goes wrong:** `Project ':' cannot access 'Project.layout' functionality on subprojects`. **Avoid:** FileTree targets (Pattern 2). **Warning sign:** the failure appears only with isolated projects on.

### Pitfall 3: detekt comments rules silently off
**What goes wrong:** TEST-10 appears satisfied while undocumented public API passes. **Avoid:** explicit overrides plus a TestKit or sample-module test that proves an undocumented public declaration fails (Validation Architecture).

### Pitfall 4: Spotless/Prettier results cached while the environment changed
**What goes wrong:** with `org.gradle.caching=true`, a `spotlessPrettier` result recorded without `npm` on `PATH` (a failing lint) was restored from the build cache after Node became available, and kept failing until `--no-build-cache --rerun-tasks` `[VERIFIED: prototype]`. Switching Node versions can similarly reuse results. **Avoid:** make the output of `verifyNodeVersion` (or the node major) an input of the Prettier tasks, or exclude Spotless tasks from the build cache. **Warning sign:** "lint error: Can't automatically determine npm executable" persisting after `mise install`.

### Pitfall 5: Typed accessor vs plugin-version conflict in the root build
Described under Pattern 1 (apply tool plugins by id through convention plugins).

### Pitfall 6: Dokka fails on `kotlin-dsl` generated sources
**What goes wrong:** every precompiled script plugin yields `Couldn't resolve link: [Courses_..._gradle]` and `failOnWarning` fails the build-logic Dokka task. **Avoid:** restrict `sourceRoots` to `src/main/kotlin` (Pattern 6) `[VERIFIED]`.

### Pitfall 7: `reuse lint` surprises
SPDX text inside configuration strings, missing `LICENSES/*.txt`, third-party Apache headers, missing `charset-normalizer`, and a repository without commits not honouring `.gitignore` (Finding 5, Pattern 8).

### Pitfall 8: Spotless header delimiter eats top-of-file comments in `.kts`
With delimiter `[^/\s]`, everything before the first line that does not start with `/` or whitespace counts as the old header and is **replaced**, so a descriptive `//` comment or a `/** */` block at the top of a `.kts` file would be deleted (`[ASSUMED]` consequence of the delimiter semantics; `[VERIFIED]` observations: an existing `// SPDX` first line was replaced idempotently, and a file starting with `@file:Suppress` received the header above it). **Avoid:** start `.kts` files with an `import`, `@file:` annotation or a statement; place explanatory comments after it.

### Pitfall 9: zizmor `read-all`
Use `permissions: {}` plus job-level `contents: read` (Finding 6).

### Pitfall 10: Single required check vs separate workflows
See Finding 7 and Pattern 17. Also: `ci-success` must use `if: always()` and treat anything other than `success` as failure, otherwise skipped dependencies pass the gate.

### Pitfall 11: Governance bootstrap deadlock
**What goes wrong:** the Phase 1 PR itself touches governance files (`.github/**`, `mise.toml`, `renovate.json5`, `.claude/settings.json`), so it needs the `governance-approved` label, but `labels.yml` only syncs labels after the workflow exists on `main` (post-merge). The required contexts also only appear after the workflows have run once. **Avoid:** the author creates the label manually before the first PR (`gh label create governance-approved`), applies the ruleset **after** the first green run (contexts known) and before or after the merge as decided at the approval gate; the plan states this order explicitly. The same applies to the `labels.yml` job: it should run on `push` to `main` when `labels.yml` changes plus `workflow_dispatch`, with `issues: write` only.

### Pitfall 12: Whole-file config policy meets runtime GSD mutations
Transient keys like `workflow._auto_chain_active` would fail the D-14 policy if committed (Finding 8). The policy task output must list offending key paths; document "reset before commit" and consider ignoring keys prefixed with `_` only if the author agrees (a policy change, not silent leniency).

### Pitfall 13: `jdx/mise-action` installs a second JDK and Node
Set `MISE_DISABLE_TOOLS=java,node` in CI (Pattern 5); pin the mise `version` and use `mise.lock`.

### Pitfall 14: ktfmt and `max_line_length`
ktfmt cannot split string literals, so lines can exceed 100 columns; editorconfig-checker's `MaxLineLength` would then flag formatter-clean code. Disable that single check (Pattern 14); detekt's `MaxLineLength` (120) remains.

### Pitfall 15: mise registry flags `actionlint` as deprecated
The mise registry entry for `actionlint` carries a `deprecated` note pointing to `jactionlint`, a fork created 2026-10-05 `[VERIFIED: mise registry file; gh search]`, while upstream `rhysd/actionlint` 1.7.12 is current and was pushed to in 2026-07. CICD-08 names actionlint, so keep `aqua:rhysd/actionlint` and treat the fork as an unvetted package (SUS-class). Expect a deprecation warning from mise; record the decision in `docs/tooling.md`.

### Pitfall 16: `.java-version` bumps vs daemon-JVM file
See Pattern 12: a Renovate JDK PR cannot regenerate `gradle-daemon-jvm.properties`.

### Pitfall 17: `/gsd-ship` title vs squash commit subject
See Finding 10.

### Pitfall 18: IDE hook invalidates the configuration cache per invocation
Acceptable (about 1 to 3 s per commit); do not "fix" it by caching the property.

### Pitfall 19: CodeQL for Kotlin sees nothing when Gradle serves cached outputs
Kotlin requires a traced compile; a build-cache hit or a warm daemon yields an empty database `[ASSUMED]`. Spike in the plan; keep the `actions` language independent.

## Code Examples

Verified end to end in the prototype unless noted.

### ktfmt kotlinlang KDoc behaviour (input to output)
```kotlin
/** Greets people.   Single line KDoc that is quite long and goes well beyond the hundred column limit of kotlinlang style. */
// becomes
/**
 * Greets people. Single line KDoc that is quite long and goes well beyond the hundred column limit
 * of kotlinlang style.
 */
```
Block tags get a blank line before the first `@param`; list items are normalised to `- ` with a hanging indent; fenced code is untouched; imports are sorted and unused ones removed, but a name referenced only from KDoc (`[UUID]`) counts as used `[VERIFIED: ktfmt 0.65 CLI run]`.

### Spotless Gradle properties used in CI
```bash
./gradlew spotlessCheck                      # CI
./gradlew spotlessApply                      # one command, all artifact types
./gradlew spotlessApply -PspotlessIdeHook=/abs/A.kt,/abs/b.json   # hook; spotlessCheck is disabled in this mode
```

### Normalisation and REUSE checks (CI lint job)
```bash
git add --renormalize . && git diff --cached --exit-code
reuse lint
editorconfig-checker
actionlint && zizmor --offline --no-progress .github/workflows
shellcheck $(git ls-files '*.sh')
```

### Wrapper properties (checksum pinned)
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.9.2-bin.zip
distributionSha256Sum=971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b
validateDistributionUrl=true
```
`setup-gradle` validates the wrapper jar by default `[CITED: setup-gradle docs]`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `org.gradle.unsafe.isolated-projects` | `org.gradle.isolated-projects` (old name still accepted) | promoted in Gradle 9.7.0 | Use the new name; failures stop on first violation |
| `com.facebook:ktfmt` | `org.jetbrains.kotlinx:ktfmt` | ktfmt 0.65 (Spotless 8.10.4) | Older versions still supported through Spotless |
| detekt `io.gitlab.arturbosch.detekt` 1.x | `dev.detekt` 2.0 alpha | alpha line on Kotlin 2.4 | Rule `comments` set renamed (`DocumentationOverPrivate*`); all off by default |
| Dokka v1 tasks (`dokkaHtml`) | Dokka v2 (`dokkaGeneratePublicationHtml`, `dokka { }` DSL) | Dokka 2.x | V1 tasks are disabled in the prototype |
| Spotless `-PspotlessFiles` | `-PspotlessIdeHook` / `ratchetFrom` | removed in Spotless 5.0.0 | Hook uses the IDE hook |
| zizmor ref-pin allowance for `actions/*` | hash-pin everywhere by default | zizmor 1.20.0 | CICD-03 matches the default |
| actions caching without a licence note | `gradle/actions` v6 enhanced caching is a proprietary component, free for public repos | v6 | Choose `basic` or accept (Assumption A3) |

**Deprecated/outdated:** `buildSrc` (not used, per INFRA-10); Gradle Enterprise naming; `gitleaks protect`/`detect` (replaced by `git`/`dir`; this session ran 8.30.1 `git`); detekt 1.23.x (Kotlin 2.0.21 era, already excluded by project research).

## Runtime State Inventory

Not applicable. This phase is greenfield (no rename or migration); the only pre-existing repository state is listed in Finding 9 and the CONTEXT code insights. Nothing found in any of the five categories needs migration: stored data none, live service config only the GitHub settings in Finding 9 (handled by the apply script), OS-registered state none, secrets none yet (the future drift-check token and optional `cache-encryption-key` are approval gates), build artifacts none.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The GitHub repository role id for "Admin" in `bypass_actors` is `5` | Pattern 15 | Ruleset creation fails or grants bypass to the wrong role; the script reads the role back |
| A2 | CodeQL's Kotlin extractor needs `build-mode: manual`, no build cache and no daemon for a clean trace | Pattern 11, Pitfall 19 | Kotlin leg returns an empty database; mitigate by spike, keep `actions` leg |
| A3 | `gradle/actions` v6 default caching (proprietary, free for public repos) is acceptable for this project; `cache-provider: basic` is the open alternative | Pattern 11 | Supply-chain stance conflict for a showcase repo; one-line switch |
| A4 | `org.tomlj:tomlj` (or a hand-written reader) is suitable for parsing `mise.toml` in a build-logic task | Pattern 9 | Extra dependency; use the line reader |
| A5 | Renovate `allowedVersions` can restrict the `java-version` datasource to LTS majors | Pattern 12 | Renovate proposes non-LTS JDKs; add a package rule or disable that dependency |
| A6 | `lockfile = true` is the exact mise setting that makes `mise install` honour `mise.lock` | Pattern 10 | Lock exists but is not enforced locally; CI `--locked` behaviour is documented separately |
| A7 | OpenSSF Scorecard workflow needs `security-events: write`, `id-token: write` and `publish_results: true` for a public repo | Pattern 11 | Badge/results not published |
| A8 | `.claude/settings.json` path rules behave like ordinary project paths (no protected-path exception) | Pattern 18 | The ask rule may be bypassed or prompt twice |
| A9 | Reading rulesets of a public repo for the drift check may work without admin scope | Pattern 15 | A token secret is mandatory sooner than planned |
| A10 | `crazy-max/ghaction-github-labeler` v6 input names (`yaml-file`, `skip-delete`) and `.github/labels.yml` entry format (`name`, `color`, `description`) | Pitfall 11, Standard Stack | Labels workflow fails on first run |
| A11 | `mise x gradle@9.9.2 -- gradle wrapper ...` can bootstrap the wrapper | Standard Stack | Use a manual Gradle download instead |
| A12 | Spotless's FileTree target stays fast when `node_modules` appears (Phase 5) because `**/node_modules/**` directory excludes prune the walk | Pattern 2 | Slow configuration; move to per-module targets |

## Open Questions (RESOLVED)

1. **D-09 wording vs two findings (decision needed from the author)**
   - Known: `permissions: read-all` fails zizmor (exit 13); a separate governance workflow cannot be part of `ci-success`.
   - Unclear: whether the author accepts `permissions: {}` + job-level `contents: read` and two required contexts (`ci-success`, `governance-success`).
   - Recommendation: accept both; record them as an ADR-lite note in `docs/adr/` (Phase 2 normalises).
   - **RESOLVED:** author decision D-21 (CONTEXT): workflow-level `permissions: {}` with job grants and two required contexts `ci-success` and `governance-success`; implemented in plans 01-09 (`governance.yml`), 01-12 (`ci.yml`, ADR-lite `docs/adr/0001-workflow-permissions-and-required-checks.md`) and 01-13 (ruleset).
2. **Sample module name, location and fate (discretion D-02)**
   - Recommendation: `:tooling:convention-sample` at `tooling/convention-sample` (package `io.github.aanbrn.courses.sample`), kept through Phase 1 as the live proof for detekt/Dokka/tests, removed when Phase 2's first real Kotlin module lands, with the removal recorded in the Phase 2 plan. TestKit tests stay permanent.
   - **RESOLVED:** author decision D-22 (CONTEXT) adopts the recommendation; the module is created in plan 01-01.
3. **Should `.claude/settings.json` be formatted by Prettier?**
   - It is the only tracked file under `.claude/`. Recommendation: include it (explicit target) and exclude the rest of `.claude/**` and all of `.planning/**`. Needs confirmation because GSD updates may rewrite it.
   - **RESOLVED:** planner discretion (formatting is TEST-09 scope, no author decision needed): plan 01-04 Task 1 formats `.claude/settings.json` through an explicit Prettier target and keeps the rest of `.claude/**` and all of `.planning/**` excluded.
4. **Spotless 8.11.0 or 8.12.0?**
   - Both passed the prototype; 8.12.0 was released on the research date. Recommendation: 8.11.0 (released 2026-11-02, inside the 7-day cooldown policy); Renovate will propose 8.12.0 after the cooldown.
   - **RESOLVED:** Spotless 8.11.0 is pinned in the catalog by plan 01-01; Renovate (plan 01-11) proposes 8.12.0 once the 7-day cooldown has passed.
5. **`gradle/actions` caching provider (A3)** and whether to supply `cache-encryption-key` (approval gate).
   - **RESOLVED:** plan 01-12 uses the MIT-licensed `cache-provider: basic` with cache writes from `main` only; `GRADLE_ENCRYPTION_KEY` is optional (the workflow works without it) and is an approval item the author applies in plan 01-14.
6. **README REUSE badge in Phase 1 (INFRA-16)**
   - There is no README yet and GH-02/DOC-01 are Phase 13. Recommendation: create a minimal README in Phase 1 containing the REUSE badge and a pointer to `docs/tooling.md`, or record the badge clause as completed in Phase 13 in the verification notes.
   - **RESOLVED:** author decision D-23 (CONTEXT): a minimal README with the REUSE badge, quick start and `docs/tooling.md` pointer is created in plan 01-06; the full README stays in Phase 13.
7. **Cross-phase: Conventional-Commit PR titles (Finding 10)** to be solved in Phase 12 (GH-05), possibly by a pre-merge title rewrite or a ship-title config. Phase 1 must not add a title lint.
   - **RESOLVED:** deferred to Phase 12 (GH-05) by scope; Phase 1 adds no title lint (plan 01-10's PR template explicitly omits a Conventional-Commit title rule).
8. **Whether the Kotlin leg of CodeQL ships in Phase 1** or only the `actions` language (A2). Recommendation: ship `actions` unconditionally and `java-kotlin` as a time-boxed spike.
   - **RESOLVED:** author decision D-24 (CONTEXT) adopts the recommendation; plan 01-12 Task 3 runs the spike and records the outcome in `docs/adr/0002-codeql-kotlin-analysis.md`.
9. **Detekt rule strictness beyond `comments`** (discretion): recommend `buildUponDefaultConfig = true` plus the six comments rules; add `MaxLineLength` at 120; revisit when real code arrives.
   - **RESOLVED:** Claude's discretion per CONTEXT; plan 01-02 Task 1 configures `buildUponDefaultConfig = true`, the six `comments` rules and `MaxLineLength` 120, with no baseline.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| mise | TOOL-02, hooks, CI parity | no (not installed on this machine) | - | `brew install mise` (Homebrew 7.0.15 present) or the verified release binary |
| JDK 25 | build | yes | Temurin 25.0.5.1 | provisioned by foojay/mise |
| Node.js | Spotless/Prettier | yes (nvm) | 24.20.0 (`.nvmrc` would pin major 24) | mise-provided 24.22.3 |
| Gradle (global) | wrapper bootstrap only | no | - | download distribution once, or `mise x gradle@9.9.2` |
| git | everything | yes | 2.54.0 | - |
| gh CLI (authenticated, admin on the repo) | apply script, labels | yes | 2.105.0 | - |
| Docker engine | not needed in Phase 1 | yes | 29.10.0 | - |
| gitleaks | hook, CI | yes | 8.30.1 | mise copy |
| actionlint | hook, CI | yes | 1.7.12 | mise copy |
| shellcheck | hook, CI | yes | present (version line not captured) | mise copy |
| zizmor, lefthook, reuse, editorconfig-checker | hook, CI | no | - | all install through mise (verified) |
| pnpm | not needed until Phase 5 | no | - | - |
| Network (Maven Central, plugin portal, npm registry, foojay, GitHub) | build, Spotless Prettier, `updateDaemonJvm` | yes | - | - |

**Missing dependencies with no fallback:** none. **With fallback:** mise (install first; it is the only manual bootstrap prerequisite and belongs in `docs/tooling.md` and the README quick start).

## Validation Architecture

`workflow.nyquist_validation` is `true` in `.planning/config.json`, so this section applies.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 6.0.3 (Jupiter) + Kotest assertions 6.2.7; Gradle TestKit for `build-logic` (`functionalTest` suite) |
| Config file | `build-logic/build.gradle.kts` (suite), sample module `build.gradle.kts` via the convention |
| Quick run command | `./gradlew :build-logic:functionalTest :tooling:convention-sample:check` |
| Full suite command | `./gradlew build` (root `check` includes `:build-logic:check`, Spotless, detekt, Dokka, tests) plus the CI lint job commands |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INFRA-01, INFRA-11 | Conventions apply; `platform(project(":platform"))` on main and test configurations | TestKit | `./gradlew :build-logic:functionalTest --tests '*PlatformConvention*'` | Wave 0 |
| INFRA-10 | Config cache reused, isolated projects on, no problems | TestKit | `./gradlew :build-logic:functionalTest --tests '*ConfigurationCache*'` | Wave 0 |
| INFRA-12 | build-logic formatted/analysed/KDoc'd | check | `./gradlew :build-logic:check` | Wave 0 |
| INFRA-09 | Version literal rejected; `.java-version` vs daemon JVM drift fails | TestKit | `./gradlew :build-logic:functionalTest --tests '*VersionLiteral*' --tests '*DaemonJvm*'` | Wave 0 |
| AIDEV-05 | Config policy: exact copy passes, any deviation fails with key paths | TestKit | `./gradlew :build-logic:functionalTest --tests '*ConfigPolicy*'` | Wave 0 |
| TOOL-01, TOOL-02 | mise table cross-check both directions | TestKit | `./gradlew :build-logic:functionalTest --tests '*ToolingTable*'` | Wave 0 |
| TEST-09 | Unformatted Kotlin/JSON/YAML/Markdown fails `spotlessCheck`; apply fixes | TestKit + CI | `./gradlew spotlessCheck` | Wave 0 |
| TEST-10 | Undocumented public declaration fails detekt; broken KDoc link fails Dokka | TestKit (negative fixtures) | `./gradlew :build-logic:functionalTest --tests '*KDoc*'` | Wave 0 |
| TEST-13 | detekt runs in `check` with no baseline | sample module | `./gradlew :tooling:convention-sample:detekt` | Wave 0 |
| INFRA-16 | `reuse lint` compliant | CLI | `reuse lint` | Wave 0 |
| INFRA-13 | Tree normalised | CLI | `git add --renormalize . && git diff --cached --exit-code` | Wave 0 |
| INFRA-14 | `.editorconfig` conformance | CLI | `editorconfig-checker` | Wave 0 |
| CICD-01, CICD-03, CICD-08 | Workflows lint clean, pinned | CLI | `actionlint && zizmor --offline --no-progress .github/workflows` | Wave 0 |
| CICD-06 | No secrets in full history | CLI | `gitleaks git --redact --no-banner` | exists (tool) |
| CICD-04 | Renovate config valid; every source extracted | CLI | `npx --yes -p renovate@<ver> renovate-config-validator --strict` (and local extract) | Wave 0 |
| GH-06, GH-07 | Ruleset JSON well-formed; script idempotent in `--dry-run` | shell test | `scripts/apply-github-settings.sh --dry-run` | Wave 0 |
| GH-08, GH-11 | Template headings match `ship.md`; `labels.yml` parses | shell/Gradle check | `./gradlew :build-logic:check` (template-headings and labels checks) | Wave 0 |
| AIDEV-06 | Label gate and bot exemption | script tests | `scripts/governance/test-*.sh` | Wave 0 |
| AIDEV-08 | `ask` rules present for each governance path and `config-set` | JSON check | Gradle check or `jq` assertion over `.claude/settings.json` | Wave 0 |

### Sampling Rate
- **Per task commit:** the lefthook hook plus the quick run command above.
- **Per wave merge:** `./gradlew build` and the CI lint commands.
- **Phase gate:** full CI green on the PR, then `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] Gradle wrapper, `settings.gradle.kts`, catalog, `build-logic` skeleton with `functionalTest` suite (no test infrastructure exists yet)
- [ ] TestKit fixtures per convention and governance task, including negative fixtures for detekt/Dokka/Spotless
- [ ] `config/detekt/detekt.yml`, `config/spotless/license-header.txt`, `.prettierrc.yaml`, `.editorconfig`, `.editorconfig-checker.json`
- [ ] `mise.toml`, `mise.lock`, `lefthook.yml`, `scripts/format-staged.sh`
- [ ] Framework install is via the wrapper and mise; nothing else to install

## Security Domain

`security_enforcement` is enabled (absent means enabled; `security_asvs_level: 1`, `security_block_on: "high"` in `.planning/config.json`). The phase ships no application code, so the relevant controls are supply-chain and CI/repository controls rather than request-level ASVS categories.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V1 Architecture / V14 Configuration | yes | Declarative, reviewable repo settings; least-privilege workflow permissions; pinned tools and actions |
| V2 Authentication | no (no runtime) | - |
| V3 Session Management | no | - |
| V4 Access Control | yes (repository) | Ruleset on `main`, `governance-approved` label gate, `ask` rules, admin-only bypass |
| V5 Input Validation | partially | Workflow inputs only through `env:`; bot-diff exemption parses diffs with an allow-list pattern |
| V6 Cryptography | no (none hand-rolled) | Checksums via `distributionSha256Sum`, `mise.lock`, SHA pins |
| V8/V13 Data protection, secrets | yes | gitleaks (hook + full history), GitHub push protection, secrets only as repository secrets, no tokens in repo |
| V14 Dependencies | yes | Renovate with 7-day cooldown, SHA-pinned actions, wrapper checksum, Dependabot alerts |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Compromised third-party action via moved tag (Trivy-style) | Tampering | Full-SHA pins for all actions (zizmor default), Renovate digest updates with cooldown, no tag-referenced scanners |
| Template/script injection through `${{ }}` in `run:` | Tampering / EoP | Pass values through `env:` (as in `ci-success`), zizmor `template-injection` audit |
| Over-privileged `GITHUB_TOKEN` | EoP | `permissions: {}` at workflow level, per-job minimum, `persist-credentials: false` |
| `pull_request_target` with PR code checkout | EoP | Not used; governance reads the PR files API only |
| Governance bypass by editing policy and config in the same PR | Tampering | Label gate by a maintainer, whole-file config policy, ruleset required contexts |
| Agent silently editing governance files or running `config-set` | Tampering | `permissions.ask` rules; CI remains the enforcement (rules are not a boundary) |
| Secret committed in history or staged | Information disclosure | gitleaks full history in CI and `--staged` in the hook, push protection, `.env` ignored |
| Malicious or typosquatted tool/package | Tampering | mise registry + `mise.lock` checksums and provenance, verified package names, human-verify checkpoint for SUS-flagged packages |
| Cache poisoning of the Gradle/CI cache | Tampering | Cache writes only from `main`, optional `cache-encryption-key` |
| Overbroad drift-check token | EoP | Fine-grained read-only token as a secret, created only after explicit approval |

## Project Constraints (from CLAUDE.md)

Extracted from `/Users/afanasyev/Development/axon5-dcb-courses/.claude/CLAUDE.md` and the global instructions; treat them as locked.
- Code, comments, commit messages, PRs, repository documents and subagent prompts in English.
- Idiomatic Kotlin; reference-quality artifacts; every artifact auto-formatted and style-checked locally (hook) and in CI.
- Gradle JVM Test Suites for test levels and Java Test Fixtures for shared test helpers (Phase 2 owns the levels; Phase 1 conventions must not block them).
- Decision Precedence overrides the generated stack section: container images via Cloud Native Buildpacks (not relevant to Phase 1), **ktfmt via Spotless (not ktlint)**, JDK only from `.java-version`, Node only from `.nvmrc`, pnpm only from `packageManager`, everything else Gradle consumes from the version catalog via `:platform`, **TypeID** IDs, no MapStruct/mapping generators, no kapt, tags and releases owned by release-please, GSD works on one branch per phase with squash merges.
- Work starts through a GSD command; no direct edits outside a GSD workflow unless the user explicitly asks to bypass it.
- Formatting stack, test toolchain and CI tooling choices in the generated STACK section are superseded where this research updated versions (Spotless 8.11/8.12, ktfmt instead of ktlint, Dokka, Renovate specifics).
- Git commits end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`; pull request descriptions end with the "Generated with Claude Code" line (attribution reminder).
- No emojis in communication.

## Suggested Plan Slicing

The planner owns the final decomposition; this slicing follows the dependency order discovered above (each slice leaves `./gradlew build` and the hook green).
1. **Repository hygiene and legal:** `.gitignore` (extend), `.gitattributes`, `.editorconfig`, `.editorconfig-checker.json`, `LICENSES/`, `REUSE.toml`, `config/spotless/license-header.txt`, README stub (Open Question 6).
2. **Build skeleton:** wrapper (+ checksum), `gradle.properties`, catalog, `build-logic` (settings plugin, root plugin, module convention, self-quality), `:platform`, sample module; `.java-version`, `.nvmrc`, `updateDaemonJvm`.
3. **TestKit suite and governance tasks:** `functionalTest` suite, convention tests, config policy, version-literal, daemon-JVM and tooling-table checks, `governance/gsd-config-policy.json`, `docs/tooling.md`.
4. **Tooling and hook:** `mise.toml`/`mise.lock`, `lefthook.yml`, `scripts/format-staged.sh`, `performance.scenarios` and baseline notes.
5. **CI:** `ci.yml`, `codeql.yml` (spike leg), `scorecard.yml`, `labels.yml` + `.github/labels.yml`, `governance.yml` and its scripts.
6. **Renovate:** `renovate.json5` with packageRules (D-12, D-16 interplay), validation task.
7. **GitHub-side artifacts (no applying):** `.github/rulesets/main.json`, apply script with `--dry-run`, PR template, `.claude/settings.json` ask rules.
8. **Approval-gated execution step (explicit author approval each):** apply GitHub settings (GH-06/GH-07), install the Mend Renovate app, create the drift-check token secret and `cache-encryption-key`, create the `governance-approved` label before the first PR.

## Sources

### Primary (HIGH confidence, executed or authoritative)
- Scratch prototype (Gradle 9.9.2, JDK 25, Kotlin 2.4.21; scratchpad `proto/`): every `[VERIFIED: prototype]` claim.
- Maven Central and plugin-portal `maven-metadata.xml` (Spotless 8.12.0/8.11.0, ktfmt 0.65, detekt 2.0.0-alpha.6, Dokka 2.3.0, Kotlin 2.4.21, Kover 0.9.11, foojay-resolver-convention 1.0.0); `services.gradle.org/versions/current` (9.9.2); npm registry (prettier 3.10.1); PyPI (reuse 6.3.0, zizmor 1.32.0); GitHub releases and tag refs for all tools and actions; `gh api` read-only calls on `aanbrn/axon5-dcb-courses`.
- Spotless plugin-gradle README and CHANGES (`raw.githubusercontent.com/diffplug/spotless/main/plugin-gradle/`).
- Gradle docs: Isolated Projects, Daemon JVM criteria (docs.gradle.org/current/userguide).
- Renovate docs and source: configuration-options (`minimumReleaseAge`, `internalChecksFilter`), key-concepts/minimum-release-age, managers (`github-actions`, `gradle`, `mise`, `nvm`), datasource `java-version` (github.com/renovatebot/renovate).
- zizmor audits documentation (`unpinned-uses`, `excessive-permissions`).
- Local repository files read this session: `01-CONTEXT.md`, `REQUIREMENTS.md`, `STATE.md`, `.gitignore`, `.planning/config.json`, `.claude/gsd-core/workflows/ship.md`.

### Secondary (MEDIUM confidence, official docs fetched through a summarising tool)
- Claude Code permissions documentation (`code.claude.com/docs/en/permissions`).
- GitHub REST docs for rulesets and repository settings; CodeQL docs (compiled languages, supported languages); `gradle/actions` setup-gradle documentation; REUSE specification 3.3; mise registry files and `mise-action` `action.yml`.

### Tertiary (LOW confidence, marked [ASSUMED] in the log)
- Admin role id, Scorecard permissions, labeler input names, CodeQL Gradle flags, mise `lockfile` setting name.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, versions checked against registries on the research date and exercised in a prototype.
- Architecture: HIGH for build-logic, formatting, analysis, REUSE, mise, Renovate extraction; MEDIUM for GitHub settings payloads and CodeQL-for-Kotlin.
- Pitfalls: HIGH, nearly all were reproduced; the governance bootstrap deadlock and the `/gsd-ship` title mismatch are derived from reading, not from a run.

**Research date:** 2026-11-21
**Valid until:** 2026-12-05 for pinned versions and action SHAs (fast-moving; re-resolve at execution); 2026-12-21 for the architecture findings.
