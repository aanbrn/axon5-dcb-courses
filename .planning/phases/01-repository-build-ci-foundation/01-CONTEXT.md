# Phase 1: Repository, Build & CI Foundation - Context

**Gathered:** 2026-11-21
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase gives the repository reference quality before any runtime or domain code exists. It delivers:

- A multi-project Gradle build: the `build-logic` included build with TestKit-tested convention plugins, a `:platform` module and the version catalog. Configuration cache is on, and version literals or `.java-version` drift fail the build.
- One-command formatting for every artifact type, with a fast pre-commit hook.
- Static analysis with detekt and Dokka KDoc checks.
- REUSE/SPDX compliance, `.gitattributes`, `.editorconfig` and `.gitignore`.
- GitHub Actions CI with SHA-pinned actions, workflow and script linting, and a full-history secret scan.
- Renovate covering every version source.
- Pinned CLI tools in `mise.toml`, inventoried in `docs/tooling.md`.
- Governance: a policy check for `.planning/config.json`, a `governance-approved` label gate, Claude Code `ask` permissions, labels as code, a PR template and GitHub repository settings.

Out of scope: no runtime services, Axon or Spring application code, docker compose, Testcontainers or ADR templates (Phase 2), and no frontend or docs-site build (Phase 5).

Requirements: INFRA-01, INFRA-09, INFRA-10, INFRA-11, INFRA-12, INFRA-13, INFRA-14, INFRA-16, TEST-09, TEST-10, TEST-13, CICD-01, CICD-03, CICD-04, CICD-06, CICD-08, TOOL-01, TOOL-02, GH-06, GH-07, GH-08, GH-11, AIDEV-05, AIDEV-06, AIDEV-08.

</domain>

<decisions>
## Implementation Decisions

### Modules & Identity
- **D-01:** The root Maven group and Kotlin base package is `io.github.aanbrn.courses`, so code lives in packages such as `io.github.aanbrn.courses.command...`. — **Reversibility:** costly — every package, Konsist rule and Spring scan path depends on it. It becomes effectively one-way once event class names are persisted in Axon Server (Phase 3), unless event type names are decoupled from class names.
- **D-02:** Phase 1 creates only infrastructure projects: `build-logic` (included build), `:platform` (`java-platform`), and one tiny **sample module**. The sample module exercises the conventions end to end in `./gradlew build`: Kotlin compile, ktfmt, detekt, Dokka KDoc checks, test suites, SPDX header. Real modules (contracts, services, frontend, docs-site) arrive in the phases that need them. The planner decides what happens to the sample module later, either removed when the first real module lands in Phase 2 or kept as a permanent convention fixture, and records the choice.
- **D-03:** Directories are grouped by role: `platform/`, `services/{command,query,automation,bff}`, `contracts/*`, `frontend/`, `docs-site/`. Phase 1 creates only `platform/` and the sample module, but `settings.gradle.kts` and conventions must anticipate this layout. — **Reversibility:** costly — paths are referenced from CI, Renovate, REUSE, `.gitattributes`, IDE run configurations and docs.
- **D-04:** Convention plugin IDs use a project prefix plus a role, for example `courses.kotlin-library`, `courses.spring-service`, `courses.test-suites`. Plugins are composed per module role. Phase 1 needs only the ones the sample module and `:platform` use, plus any root or quality plugins.

### Formatting, Static Analysis & Pre-commit
- **D-05:** Prettier, run through Spotless, formats JSON, YAML and Markdown, so one `./gradlew spotlessApply` formats everything. Generated code is excluded. Biome joins for TS/TSX in Phase 5. Research needs to settle how Spotless provides the Node runtime for Prettier while honouring the INFRA-09 rule that the Node version lives only in `.nvmrc`.
- **D-06:** ktfmt runs with the **kotlinlang** style (4-space indent, official Kotlin coding conventions), including KDoc formatting, for `*.kt` and `*.kts`, `build-logic` included. `.editorconfig` must agree with it.
- **D-07:** detekt uses `dev.detekt` 2.0.0-alpha.x with a strict configuration and no baseline, `comments` rules enabled. If it breaks with Kotlin 2.4, the configuration cache or isolated projects, fall back to `allWarningsAsErrors` plus Konsist and Dokka. Record the fallback in an ADR and an upstream record. Formal ADR and upstream templates arrive in Phase 2, so a Phase 1 fallback uses a minimal MADR file under `docs/adr/` that Phase 2 normalises.
- **D-08:** The pre-commit hook is lefthook and is kept **fast**. It formats staged files and re-stages them, and runs gitleaks on staged changes, `reuse lint`, and actionlint, zizmor and shellcheck on changed files only. detekt, Dokka and tests run in CI, not in the hook. There is no pre-push hook.

### CI Structure & Renovate
- **D-09:** Workflows are split by purpose: `ci.yml` runs build, tests, analysis and linters as parallel jobs, alongside `codeql.yml`, `scorecard.yml`, `governance.yml` and `labels.yml`. A final aggregating `ci-success` job is the **single required status check** in the ruleset, so adding jobs never requires a ruleset change. Defaults are `permissions: read-all` with per-job elevation and `persist-credentials: false`.
- **D-10:** CI runs on **ubuntu only**. There is no macOS or Windows matrix.
- **D-11:** Renovate runs as the **Mend Renovate GitHub App**, configured in-repo. Installing the app is an author approval gate at execution time.
- **D-12:** Renovate automerges only GitHub Actions digest updates and patch updates of non-runtime dev tools (mise tools, linter and formatter plugins), and only when CI is green. Runtime libraries (Axon, Spring, Kotlin and so on) and every minor or major update need a manual merge.

### Governance as Code
- **D-13:** Repository governance checks are **Gradle tasks in `build-logic`**, written in Kotlin, TestKit-tested, cacheable and wired into `./gradlew check` locally and in CI. They cover the `.planning/config.json` policy, version-literal rejection in build scripts, `.java-version` and `gradle-daemon-jvm.properties` consistency, and the `docs/tooling.md` cross-check. The policy file lives in-repo, for example `governance/gsd-config-policy.json`. The PR label check (`governance-approved`) stays in `governance.yml` because it needs PR context.
- **D-14:** The `.planning/config.json` policy pins **the whole file**: an exact expected copy. Any change, including new keys from a GSD update, requires a deliberate policy update in the same PR.
- **D-15:** GitHub repository settings live **declaratively in the repo**: ruleset JSON such as `.github/rulesets/main.json`, merge settings and security features. They are applied by an idempotent script using `gh api`, which the author runs after explicit approval. CI runs a **read-only drift check** against the live settings. That check needs a read token stored as a secret, and creating that secret is an approval gate.
- **D-16:** PRs from `renovate[bot]` that only change versions or digests, verified from the diff, are **exempt** from the `governance-approved` label requirement even when they touch governance files (`mise.toml`, workflows, Renovate config). Without this exemption, D-12 automerge cannot work. Every other governance change requires the label.

### Tooling Inventory, Labels & Build Performance
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

### Planning-time Resolutions (author-confirmed 2026-11-21, after research)
- **D-21 (amends D-09):** Workflows declare `permissions: {}` at workflow level with minimal job-level grants (`contents: read` etc.) instead of `permissions: read-all`, because `read-all` fails zizmor. `governance.yml` stays a separate workflow with its own aggregating `governance-success` job, so the ruleset requires two status checks: `ci-success` and `governance-success`. Record the deviation from D-09 as an ADR-lite note under `docs/adr/`.
- **D-22 (resolves D-02 discretion):** The sample module is `:tooling:convention-sample` at `tooling/convention-sample` (package `io.github.aanbrn.courses.sample`). It stays through Phase 1 as the live proof for detekt, Dokka, tests and SPDX headers and is removed when Phase 2's first real Kotlin module lands; the TestKit tests stay permanent.
- **D-23 (INFRA-16 scope):** Phase 1 creates a minimal `README.md` with the REUSE badge, a quick start (`mise install`, `./gradlew build`) and a pointer to `docs/tooling.md`. The full README is Phase 13.
- **D-24 (CodeQL scope):** `codeql.yml` analyses the `actions` language unconditionally; the `java-kotlin` language is a time-boxed spike. If it cannot run green within the time box, defer it with a recorded reason instead of blocking the phase.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project scope and requirements
- `.planning/ROADMAP.md` §"Phase 1: Repository, Build & CI Foundation": goal, success criteria, research topics, approval gates. Also §"Cross-Cutting Conventions" for the standing obligations this phase establishes.
- `.planning/REQUIREMENTS.md`: full text of INFRA-01/09/10/11/12/13/14/16, TEST-09/10/13, CICD-01/03/04/06/08, TOOL-01/02, GH-06/07/08/11 and AIDEV-05/06/08.
- `.planning/PROJECT.md` §"Key Decisions": single version source per ecosystem, ktfmt over ktlint, release-please owns tags, squash-merge workflow, mise, upstream-first.
- `.claude/CLAUDE.md` §"Decision Precedence": precedence of PROJECT.md and REQUIREMENTS.md over the generated stack section.

### Stack and pitfalls research
- `.planning/research/STACK.md`: versions for Gradle 9.9.2, Kotlin 2.4.21, Spotless 8.11, detekt 2.0.0-alpha.6, Kover, actions pins, Renovate, gitleaks CLI (not the action), Trivy supply-chain note.
- `.planning/research/PITFALLS.md`: SHA-pinning and supply-chain pitfalls.
- `.planning/research/SUMMARY.md`: overall risk summary.

### Governed files
- `.planning/config.json`: the exact file the D-14 policy pins.
- `.gitignore`: already tracks only `.claude/settings.json` and `.claude/CLAUDE.md` under `.claude/`. Extend it, don't replace it.
- `LICENSE`: MIT, copyright Alexey Afanasyev 2026. Source for the `REUSE.toml` and SPDX headers.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None. The repository contains only `.gitignore`, `LICENSE`, `.planning/` and the `.claude/` project files. This phase is greenfield.

### Established Patterns
- `.gitignore` already excludes `/.idea/` and all of `/.claude/` except `settings.json` and `CLAUDE.md`. Keep this when making `.gitignore` comprehensive (INFRA-14).
- GSD config: `git.branching_strategy: phase`, `phase_branch_template: gsd/phase-{phase}-{slug}`, `create_tag: false`. The ruleset and PR template must match this.

### Integration Points
- Remote: `github.com/aanbrn/axon5-dcb-courses`. Owner `aanbrn` is used in the package name and the future GHCR image names.
- `.claude/settings.json` (tracked) receives the AIDEV-08 `ask` permission rules for governance files and GSD `config-set`.

</code_context>

<specifics>
## Specific Ideas

- `ci-success` is the only required check, so the ruleset stays stable as CI grows.
- Renovate automerge (D-12) and the governance label exemption (D-16) are designed together. The exemption must be narrow: bot author **and** a diff that only changes versions or digests.
- Approval gates during execution: applying GitHub settings (GH-06, GH-07), installing the Mend Renovate app, creating the drift-check read token secret, and any Build Scan publication.

</specifics>

<deferred>
## Deferred Ideas

- Community labels (`good first issue`, `help wanted`, `question`): Phase 13, with the community health files.
- Scheduled gradle-profiler job in CI with trend reports: considered and not adopted (shared-runner noise). Can be revisited in Phase 12 hardening.
- macOS or Windows CI matrix: not adopted. Revisit only if platform-specific defects appear.

</deferred>

---

*Phase: 01-repository-build-ci-foundation*
*Context gathered: 2026-11-21*
