# Phase 1: Repository, Build & CI Foundation - Pattern Map

**Mapped:** 2026-11-21
**Files analyzed:** 52 (new or modified)
**In-repo analogs found:** 2 / 52 (only `.gitignore` extension and `LICENSE` as SPDX source). Greenfield: `git ls-files` outside `.planning/` returns only `.claude/CLAUDE.md`, `.gitignore`, `LICENSE`.

**Canonical pattern source:** where "No in-repo analog" appears, copy from `01-RESEARCH.md` (cited by Pattern/Finding number). Those excerpts were run end to end in a prototype (Gradle 9.9.2, JDK 25). Never use `.claude/gsd-core/` as a pattern for project code.

**Corrections to the inputs (planner must act on these):**

- `.claude/settings.json` does NOT exist yet (CONTEXT says it is tracked; only `.claude/CLAUDE.md` is). AIDEV-08 CREATES it. The existing `.gitignore` negation (`!/.claude/settings.json`) already allows tracking it.
- `.planning/config.json` is tracked and is the source for `governance/gsd-config-policy.json` (exact copy at the start). It currently contains `git.branching_strategy: "phase"`, `create_tag: false`, `phase_branch_template: "gsd/phase-{phase}-{slug}"`.
- Use `org.gradle.isolated-projects=true` (new name, not the D-19 `unsafe` name). Findings 1, 3 and 6 of RESEARCH amend D-19, D-07 and D-09/D-21.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `.gitignore` (modify) | config | n/a | existing `.gitignore` lines 1-6 | exact (extend, don't replace) |
| `REUSE.toml`, `LICENSES/MIT.txt`, `LICENSES/Apache-2.0.txt` | config | n/a | `LICENSE` (MIT, copyright source) + RESEARCH Pattern 8 | partial |
| `settings.gradle.kts` | config | build | RESEARCH Pattern 1 | no in-repo analog |
| `build.gradle.kts` (root; applies `courses.root`) | config | build | RESEARCH Pattern 2 | no in-repo analog |
| `gradle.properties` | config | build | RESEARCH Pattern 13 | no in-repo analog |
| `gradle/libs.versions.toml` | config | build | RESEARCH Standard Stack + Pattern 12 annotations | no in-repo analog |
| `gradle/wrapper/*`, `gradlew*`, `gradle/gradle-daemon-jvm.properties` | config | generated | RESEARCH "Wrapper bootstrap" + "Wrapper properties" | no in-repo analog |
| `.java-version`, `.nvmrc` | config | single version source | RESEARCH Pattern 5 | no in-repo analog |
| `build-logic/settings.gradle.kts`, `build-logic/build.gradle.kts` | config | build | RESEARCH Pattern 1, 6 | no in-repo analog |
| `build-logic/src/main/kotlin/courses.settings.settings.gradle.kts` | config (settings plugin) | build | RESEARCH Pattern 1 | no in-repo analog |
| `build-logic/src/main/kotlin/courses.root.gradle.kts` | config (root plugin) | build | RESEARCH Pattern 2 | no in-repo analog |
| `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` | config (module plugin) | build | RESEARCH Pattern 3 | no in-repo analog |
| `build-logic/src/main/kotlin/**` governance task classes (config policy, version literals, JDK consistency, tooling table) | service (cacheable Gradle task) | batch/transform | RESEARCH Pattern 9 | no in-repo analog |
| `build-logic/src/functionalTest/kotlin/**` | test (TestKit) | request-response | RESEARCH Pattern 6 | no in-repo analog |
| `platform/build.gradle.kts` | config (java-platform) | build | RESEARCH Pattern 3 | no in-repo analog |
| `tooling/convention-sample/build.gradle.kts` + `src/{main,test}/kotlin/io/github/aanbrn/courses/sample/*.kt` | component (sample module) | transform | RESEARCH Pattern 3 and 4 | no in-repo analog |
| `config/detekt/detekt.yml` | config | static analysis | RESEARCH Pattern 4 | no in-repo analog |
| `config/spotless/license-header.txt` | config | n/a | RESEARCH Pattern 2 (one line) | no in-repo analog |
| `.editorconfig`, `.editorconfig-checker.json`, `.prettierrc.yaml`, `.gitattributes` | config | n/a | RESEARCH Pattern 14 | no in-repo analog |
| `mise.toml`, `mise.lock` | config | tool pinning | RESEARCH Pattern 10 | no in-repo analog |
| `lefthook.yml`, `scripts/format-staged.sh` | config, utility | event-driven (git hook) | RESEARCH Pattern 7 | no in-repo analog |
| `.github/workflows/ci.yml` | config (workflow) | event-driven | RESEARCH Pattern 11 | no in-repo analog |
| `.github/workflows/governance.yml`, `scripts/governance/*.sh` | config, utility | event-driven | RESEARCH Pattern 17 | no in-repo analog |
| `.github/workflows/codeql.yml`, `scorecard.yml` | config (workflow) | event-driven | RESEARCH Pattern 11 (last two paragraphs) | no in-repo analog |
| `.github/workflows/labels.yml`, `.github/labels.yml` | config | event-driven | RESEARCH Pitfall 11 + D-18 | no in-repo analog |
| `renovate.json5` | config | batch | RESEARCH Pattern 12 | no in-repo analog |
| `.github/rulesets/main.json`, `scripts/apply-github-settings.sh` | config, utility | CRUD (idempotent gh api) | RESEARCH Pattern 15 | no in-repo analog |
| `.github/PULL_REQUEST_TEMPLATE.md` | config | n/a | RESEARCH Pattern 16 | no in-repo analog |
| `.claude/settings.json` (create) | config | n/a | RESEARCH Pattern 18 | no in-repo analog |
| `governance/gsd-config-policy.json` | config | n/a | `.planning/config.json` (exact copy) | exact |
| `docs/tooling.md`, `docs/adr/*.md` (D-21 ADR-lite) | doc | n/a | RESEARCH Pattern 9 (table) | no in-repo analog |
| `README.md` | doc | n/a | RESEARCH Pattern 8 end, D-23 | no in-repo analog |
| `performance.scenarios` | config | n/a | RESEARCH Pattern 13 | no in-repo analog |

## Pattern Assignments

All excerpts below live in `01-RESEARCH.md`. Line numbers refer to that file.

### `.gitignore` (modify)

**Analog:** the file itself, lines 1-6 (`/.idea/`, then `/.claude/*`, `!/.claude/settings.json`, `!/.claude/CLAUDE.md`). Keep those lines and append: `.gradle/`, `build/`, `.kotlin/`, Node/pnpm, `.env`, test and coverage artifacts, `profile-out/`, `gradle-user-home/`. Add `.mise/` only if sidecar locks are not committed (Pattern 14, line 623).

### Gradle layers: `settings.gradle.kts`, `build-logic/**`, root plugin, module plugin, `:platform`, sample module

**Source:** RESEARCH Patterns 1-4 (lines 293-420). Copy the code blocks verbatim. Key traps:

- No plugin versions in `settings.gradle.kts`. Use the settings plugin `courses.settings` (lines 298-316).
- `build-logic/build.gradle.kts` needs `implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))` for the typed `libs` accessor (lines 330-331).
- Root build must not `alias(libs.plugins.spotless)`; apply tool plugins by plain id inside convention plugins (line 334).
- Spotless targets use `layout.projectDirectory.asFileTree.matching { include(...); exclude(...) }`, never `"**/*.kt"` strings (lines 340-367). Exclude `.planning/**` and `.claude/**`.
- `.kts` files must start with an import, `@file:` annotation or statement, not a comment (Pitfall 8).
- Read `.java-version` via `isolated.rootProject.projectDirectory` (lines 386-387).
- detekt `comments` rules are off by default; commit `config/detekt/detekt.yml` from lines 411-419. No baseline.
- Dokka on `build-logic` needs `sourceRoots.setFrom(src/main/kotlin)` (line 449).
- Source files start with the one-line `// SPDX-License-Identifier: MIT` header, supplied via `licenseHeaderFile(...)` only. A string literal in a build script poisons `reuse lint` (Finding 5).

**Packages:** `io.github.aanbrn.courses.sample` for the sample module (D-01, D-22). Plugin IDs follow D-04 (`courses.root`, `courses.kotlin-library`, `courses.settings`).

### Governance Gradle tasks (`build-logic/src/main/kotlin/**`)

**Source:** RESEARCH Pattern 9 (lines 493-500). Shape: `@CacheableTask`, `@InputFile`/`@InputFiles` with `PathSensitivity.RELATIVE`, `@OutputFile` marker, no `Project` access at execution time. Wire into root `check` from `courses.root`. Each task gets a TestKit test.

Tasks: config-policy diff (parsed-JSON equality, report added, removed and changed key paths, mention transient keys such as `workflow._auto_chain_active`, Finding 8), version-literal scan, `.java-version` vs `gradle-daemon-jvm.properties` `toolchainVersion`, `docs/tooling.md` vs `mise.toml` `[tools]`. Keep backend-prefixed keys (`pipx:reuse`, `github:gradle/gradle-profiler`) verbatim. Prefer a zero-dependency line reader over a TOML library.

### TestKit tests (`build-logic/src/functionalTest/kotlin/**`)

**Source:** RESEARCH Pattern 6 (lines 430-449). Use a `functionalTest` JvmTestSuite wired into `check`. The fixture `settings.gradle.kts` uses `pluginManagement { includeBuild(<abs build-logic>) }`. Pass `catalog` and `buildLogic` as `systemProperty` inputs. Do NOT use `withPluginClasspath()`. Run the fixture twice with `--configuration-cache --isolated-projects` and assert `Reusing configuration cache`. Add a test that the platform is applied to `implementation` and `testImplementation` (line 406), and a test that an undocumented public declaration fails detekt (Pitfall 3).

### `mise.toml` / `mise.lock`

**Source:** RESEARCH Pattern 10 (lines 505-520). Tool list: gitleaks, actionlint (aqua backend, Pitfall 15), shellcheck, lefthook, zizmor, editorconfig-checker, `pipx:reuse` with `extras = "charset-normalizer"`, `github:gradle/gradle-profiler`. Keep `idiomatic_version_file_enable_tools = ["node", "java"]`.

### `lefthook.yml`, `scripts/format-staged.sh`

**Source:** RESEARCH Pattern 7 (lines 453-470). The format step calls `./gradlew --quiet spotlessApply -PspotlessIdeHook=<comma-separated absolute paths>` with `stage_fixed: true`. The hook does not run detekt, Dokka or tests (D-08).

### `.github/workflows/ci.yml` (and other workflows)

**Source:** RESEARCH Pattern 11 (lines 525-567). Rules:

- Workflow-level `permissions: {}`, job-level `contents: read` (D-21).
- Every action pinned by SHA with a `# vX.Y.Z` comment, using the SHA table at lines 169-182. Re-resolve at execution time and dereference the codeql tag object.
- `persist-credentials: false`.
- `ci-success` uses `if: always()` and passes `needs` via `env:` into a `jq -e 'all(...; .result == "success")'` step.
- Add a `lint` job (actionlint, zizmor, shellcheck, editorconfig-checker, `reuse lint`, `git add --renormalize . && git diff --cached --exit-code`).
- Run `jdx/mise-action` with `MISE_DISABLE_TOOLS=java,node`.
- Re-run actionlint and zizmor on the final files, because the flow-style YAML was not linted.

**`governance.yml`:** RESEARCH Pattern 17 (line 639-641). Trigger `pull_request` only, never `pull_request_target`. List files through the REST API with no checkout. Final job `governance-success`. The bot exemption requires the `renovate[bot]` author AND a version/digest-only diff.

**`codeql.yml`, `scorecard.yml`:** Pattern 11 lines 570-572. `actions` language unconditional; `java-kotlin` uses `build-mode: manual` as a time-boxed spike (D-24).

**`labels.yml`:** trigger on push to `main` touching `.github/labels.yml` plus `workflow_dispatch`, with `issues: write` only, using `crazy-max/ghaction-github-labeler` (Pitfall 11). The author creates `governance-approved` manually before the first PR.

### `renovate.json5`

**Source:** RESEARCH Pattern 12 (lines 579-596). It is validated except for the packageRules, which are unvalidated TODOs: grouping, deps labels, D-12 automerge limits, LTS-only `allowedVersions` for java, `semanticCommits`. Include custom regex managers for `.java-version` and the catalog annotations. Run `renovate-config-validator --strict` after editing.

### `.github/rulesets/main.json`, `scripts/apply-github-settings.sh`

**Source:** RESEARCH Pattern 15 (lines 627-633). The script is idempotent (read the current state, apply only differences), supports `--dry-run`, and uses `set -euo pipefail`. Required status contexts are `ci-success` and `governance-success`. Squash only. The current live deltas are in Finding 9. Apply only after author approval.

### `.claude/settings.json` (create)

**Source:** RESEARCH Pattern 18 (lines 646-662). Use the `permissions.ask` array verbatim. GSD's own `config-set` calls will prompt; this is intended (Finding 8).

### `.github/PULL_REQUEST_TEMPLATE.md`

**Source:** RESEARCH Pattern 16 (line 637). Headings in order: `## Summary`, `## Changes`, `## Requirements Addressed`, `## Verification`, `## Key Decisions`, then `User Stories & Acceptance Criteria`, `Risks & Dependencies`, `Success Metrics & Release Criteria`, `Stakeholder Review & Approval`. Add a short checklist. Prettier-normal Markdown.

### `REUSE.toml`, `.editorconfig`, `.editorconfig-checker.json`, `.gitattributes`, `gradle.properties`

**Source:** `REUSE.toml` Pattern 8 lines 477-490 plus `LICENSES/MIT.txt` and `LICENSES/Apache-2.0.txt`. Copyright text is `2026 Alexey Afanasyev`, from the in-repo `LICENSE`. `.editorconfig` and `.gitattributes` follow Pattern 14 lines 620-622. `gradle.properties` is Pattern 13 lines 608-615 and uses the new `org.gradle.isolated-projects` name.

## Shared Patterns

### Single version source (INFRA-09)
**Apply to:** every build script, workflow, and Renovate config.

- JVM libraries and plugins live in `gradle/libs.versions.toml` and nowhere else.
- The JDK lives in `.java-version` and Node in `.nvmrc`.
- CLI tools live in `mise.toml`.
- Actions live in SHA pins.
- `docs/tooling.md` contains no version numbers.

### SPDX header
**Apply to:** every `.kt`, `.kts` and `.sh` file the project owns. The header comes from `config/spotless/license-header.txt` (one line, `// SPDX-License-Identifier: MIT`). `.sh` files and YAML are covered by `REUSE.toml` aggregate annotations.

### Workflow hardening
**Apply to:** all workflows. `permissions: {}` at workflow level, minimal job grants, SHA pins, `persist-credentials: false`, and untrusted values passed through `env:` (RESEARCH Pattern 11).

### Governance file set (shared by AIDEV-06, AIDEV-08, the D-16 exemption)
`.planning/config.json`, `.claude/settings.json`, `.mcp.json`, `.github/**`, `mise.toml`, `mise.lock`, `renovate.json5`, `CODEOWNERS`, `governance/**` (Patterns 17 and 18). Keep the glob lists in the workflow and the `ask` rules consistent.

### Node for Prettier
Prettier needs `npm` on `PATH`. In CI use setup-node with `node-version-file: .nvmrc`; locally use mise. Make the node major an input of the Prettier Spotless tasks (Pattern 5, Pitfall 4).

## No Analog Found

Everything except the two partial cases above has no in-repo analog (greenfield). The planner should use the RESEARCH patterns cited in the classification table. No analog exists in the codebase for: Gradle build logic, Kotlin sources, GitHub workflows, shell scripts, Renovate or mise configs. This is expected for Phase 1.

## Metadata

**Analog search scope:** `git ls-files` for the whole repo, plus `.gitignore`, `.planning/config.json` and `.claude/` listing.
**Files scanned:** 5 in-repo; pattern source is 01-RESEARCH.md (997 lines, read through line 803).
**Pattern extraction date:** 2026-11-21
