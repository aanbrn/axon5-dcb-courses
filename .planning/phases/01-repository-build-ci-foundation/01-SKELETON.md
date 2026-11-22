# Walking Skeleton — Axon 5 DCB Courses

**Phase:** 1
**Generated:** 2026-11-21

## Capability Proven End-to-End

> A contributor clones the repository, runs `mise install` and `./gradlew build`, commits through the
> pre-commit hook, and opens a pull request that passes the `ci-success` and `governance-success`
> checks and is squash-merged into `main` under the repository ruleset.

Phase 1 is locked to "no domain code yet" (CONTEXT D-02, ROADMAP goal). The skeleton's end-to-end
capability is therefore the **delivery pipeline itself**, exercised through the sample module
`:tooling:convention-sample` (D-22), which passes every convention: Kotlin compile, ktfmt, SPDX header,
detekt with `comments` rules, Dokka `failOnWarning`, JUnit 6 + Kotest assertions, `:platform`
alignment, configuration cache and isolated projects.

| Leg of the path | Proven by | Plan |
|---|---|---|
| Fresh clone → pinned tools | `mise install` (tools from `mise.toml` + `mise.lock`, JDK from `.java-version`, Node from `.nvmrc`) | 01-03 |
| Pinned tools → one-command build | `./gradlew build` (build-logic conventions, `:platform`, catalog, sample module, TestKit, formatting, governance checks) | 01-01, 01-02, 01-04, 01-05, 01-07, 01-10 |
| Edit → commit | lefthook pre-commit (format staged files, gitleaks, `reuse lint`, workflow and shell linters) | 01-08 |
| Commit → PR checks | `ci.yml` (`ci-success`), `governance.yml` (`governance-success`), CodeQL, Scorecard | 01-09, 01-10, 01-12 |
| PR → `main` | ruleset (squash only, linear history, required contexts), applied by the author after approval (D-15) | 01-13, 01-14 |
| Updates keep flowing | Renovate (Mend app) with 7-day cooldown and narrow automerge | 01-11, 01-14 |

The hosted leg (a real GitHub Actions run on a PR and the squash merge) is closed by `/gsd-ship`:
the phase PR is the first PR on the repository, and the ruleset the author applies in plan 01-14 blocks its
merge until both required contexts are green.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Build tool | Gradle 9.9.2 wrapper (checksum-pinned), Kotlin DSL | Project constraint; configuration cache, build cache, parallel execution and isolated projects all green in the research prototype |
| Build logic | Included build `build-logic` (no `buildSrc`) with precompiled convention plugins `courses.settings`, `courses.root`, `courses.kotlin-library`, `courses.governance` (D-04) | Composable per module role; TestKit-tested; isolated-projects compatible |
| Version management | One source per ecosystem: `gradle/libs.versions.toml` (libraries, BOMs, plugins, ktfmt, Prettier), `.java-version` (JDK), `.nvmrc` (Node), `mise.toml` + `mise.lock` (CLI tools, mise `min_version`), SHA pins (Actions) | INFRA-09; version literals in build scripts fail `checkBuildScripts` |
| Dependency alignment | `:platform` (`java-platform`) imports the Spring Boot, Axon Framework, Axoniq Framework, Testcontainers, Jackson, Kotlin, JUnit and Kotest BOMs; the module convention adds `platform(project(":platform"))` to every `*Implementation` configuration | INFRA-11; future test suites and fixtures inherit it automatically; the catalog's Kotlin overrides Boot's managed Kotlin through an explicit `api` constraint on `kotlin-stdlib` (catalog alias) |
| Group and base package | `io.github.aanbrn.courses` (D-01) | Costly to change; fixed before any Kotlin code lands |
| Directory layout | By role: `platform/`, `services/{command,query,automation,bff}`, `contracts/*`, `frontend/`, `docs-site/`, `tooling/*` (D-03) | Paths are referenced by CI, Renovate, REUSE, `.gitattributes` and docs |
| Formatting | Spotless at the root over the whole repository: ktfmt kotlinlang style (Kotlin, KTS, KDoc), Prettier (JSON, JSON5, YAML, Markdown), taplo (TOML), shfmt (shell), generic whitespace steps (properties, scenarios, text and dotfile configs), SPDX header on Kotlin, KTS and shell; taplo and shfmt pinned in `mise.toml` | One `./gradlew spotlessApply` for every artifact type (D-05, D-06, TEST-09, CLAUDE.md formatting constraint) |
| Static analysis | detekt `dev.detekt` 2.0.0-alpha.6 with `comments` rules active and no baseline; Dokka 2.3.0 `failOnWarning`; Kotlin `allWarningsAsErrors` | TEST-10, TEST-13, D-07 |
| Test runner | JUnit 6 + Kotest assertions; Gradle TestKit `functionalTest` suite in `build-logic` | Project test toolchain; TestKit proves conventions under configuration cache and isolated projects (D-19) |
| Governance | Gradle tasks in `build-logic` (`checkGsdConfigPolicy`, `checkBuildScripts`, `checkDaemonJvm`, `checkToolingInventory`, `checkPrTemplate`, `checkClaudeAskRules`) wired into `check`; PR label gate in `governance.yml` | D-13, D-14, D-17, AIDEV-05/06/08 |
| CI | GitHub Actions on `ubuntu-latest` only (D-10): `ci.yml` (build, lint, secrets → `ci-success`), `governance.yml` (→ `governance-success`), `codeql.yml`, `scorecard.yml`, `labels.yml`, `settings-drift.yml`; workflow-level `permissions: {}`, SHA-pinned actions | D-09 as amended by D-21 |
| Repository settings | Declarative `.github/rulesets/main.json` + `.github/repository-settings.json`, applied by the author with `scripts/apply-github-settings.sh` after approval; read-only drift check | D-15 |
| Licensing | REUSE 3.3: SPDX header in source files, `REUSE.toml` (`precedence = "aggregate"`), `LICENSES/` | INFRA-16 |
| Agent guardrails | `.claude/settings.json` `permissions.ask` for every governed path and GSD `config-set` | AIDEV-08 |

## Stack Touched in Phase 1

- [x] Project scaffold: Gradle wrapper, `build-logic`, `:platform`, catalog, sample module, formatting, static analysis, test runner, licensing (plans 01-01, 01-02, 01-04, 01-05, 01-06)
- [x] Tooling: `mise.toml`, `mise.lock`, lefthook pre-commit hook (plans 01-03, 01-08)
- [x] CI: build, lint, secret scan, governance gate, CodeQL, Scorecard, labels sync (plans 01-09, 01-10, 01-12)
- [x] Repository governance: governance checks, ruleset, merge settings, security features, Renovate (plans 01-07, 01-11, 01-13, 01-14)
- [ ] Routing — **Out of Scope: deferred to Phase 5** (BFF REST API) per ROADMAP
- [ ] Database read and write — **Out of Scope: deferred to Phases 2–4** (Axon Server DCB event store, PostgreSQL read models) per ROADMAP
- [ ] UI interaction wired to an API — **Out of Scope: deferred to Phase 5** (first live UI) per ROADMAP
- [ ] Deployment / local full-stack run — **Out of Scope: deferred to Phase 2** (`docker compose` for Axon Server and PostgreSQL) **and Phase 6** (whole system from buildpack images) per ROADMAP

## Out of Scope (Deferred to Later Slices)

- Any Axon, Spring or domain code, contracts modules and services (Phases 2–5).
- `docker compose`, Testcontainers, `doctor` command, ADR and upstream templates (Phase 2).
- Gradle JVM test suites beyond the sample `test` and the build-logic `functionalTest` suite; test fixtures (Phase 2).
- Kover coverage, Konsist architecture rules (Phase 3).
- Frontend, Biome, pnpm, the Gradle Node plugin and the docs-site build (Phase 5).
- Container images, buildpacks, image scanning (Phase 6).
- Conventional-Commit PR-title lint, release-please, signed releases (Phase 12).
- Community health files, issue forms, Discussions, repository metadata, community labels (Phase 13).

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2: DCB on a pinned Axon Server works locally and in CI; contracts modules; test suites and fixtures; spike ADRs. The sample module `:tooling:convention-sample` is removed when the first real Kotlin module lands (D-22); the TestKit tests stay.
- Phase 3: command service enforces unique name, capacity and per-student limit through DCB decisions.
- Phase 4: query service with idempotent PostgreSQL projections and subscription queries.
- Phase 5: BFF REST + SSE and the first live UI; frontend joins `./gradlew build`.
- Phase 6: whole system from buildpack images with one `docker compose up`.
- Phases 7–13: UI foundation, feature slices (administration and schedules, transfer, waiting list), DCB teaching UI, hardening and signed releases, reference documentation.
