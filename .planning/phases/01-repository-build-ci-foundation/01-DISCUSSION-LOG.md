# Phase 1: Repository, Build & CI Foundation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md. This log preserves the alternatives considered.

**Date:** 2026-11-21
**Phase:** 01-repository-build-ci-foundation
**Areas discussed:** Modules & identity, Formatters & pre-commit, CI structure & Renovate, Governance as code, Tooling inventory / labels / build performance

---

## Modules & Identity

| Option | Description | Selected |
|--------|-------------|----------|
| `io.github.aanbrn.courses` | Standard GitHub namespace, publishable without own domain | ✓ |
| `io.github.aanbrn.dcb.courses` | Explicit DCB in package, longer | |
| `dev.courses` | Short, not owner-bound, not publishable | |

| Option | Description | Selected |
|--------|-------------|----------|
| Infrastructure only + sample module | `build-logic`, `:platform`, tiny sample module to exercise conventions | ✓ |
| Full module skeleton | Empty contracts/services modules up front | |
| No sample module | Conventions verified only by TestKit | |

| Option | Description | Selected |
|--------|-------------|----------|
| Grouped by role | `services/*`, `contracts/*`, `frontend/`, `docs-site/`, `platform/` | ✓ |
| Flat | All modules at root | |
| You decide | Planner chooses | |

| Option | Description | Selected |
|--------|-------------|----------|
| Prefix + role | `courses.kotlin-library`, `courses.spring-service`, ... | ✓ |
| Layered | `courses.base` → `courses.jvm` → `courses.spring-boot` | |
| You decide | Planner chooses | |

---

## Formatters & Pre-commit

| Option | Description | Selected |
|--------|-------------|----------|
| Prettier via Spotless | Mature YAML/MD formatting, one `spotlessApply` | ✓ |
| Biome + separate YAML/MD tools | Biome for JSON, extra tools for YAML/MD | |
| dprint | Single Rust formatter, weaker Spotless integration | |

| Option | Description | Selected |
|--------|-------------|----------|
| kotlinlang | Official Kotlin conventions, 4-space indent | ✓ |
| Google | 2-space indent | |
| Meta | ktfmt default, 2-space indent | |

| Option | Description | Selected |
|--------|-------------|----------|
| detekt 2.0-alpha with ADR fallback | Strict config, no baseline; fallback recorded if broken | ✓ |
| No detekt | `allWarningsAsErrors` + Konsist + Dokka only | |

| Option | Description | Selected |
|--------|-------------|----------|
| Fast hook | Format staged + gitleaks, reuse, actionlint, zizmor, shellcheck on changed files | ✓ |
| Full check on pre-commit | Adds detekt and `./gradlew check` | |
| Fast + pre-push check | Fast hook plus pre-push `./gradlew check` | |

---

## CI Structure & Renovate

| Option | Description | Selected |
|--------|-------------|----------|
| By purpose + single gate job | Separate workflows, `ci-success` as only required check | ✓ |
| One big workflow | Everything in `ci.yml` | |
| Reusable workflows | Callable workflows with thin triggers | |

| Option | Description | Selected |
|--------|-------------|----------|
| ubuntu only | Fast, cheap; Docker for Testcontainers | ✓ |
| ubuntu + macOS light | Build and unit tests on macOS | |
| ubuntu + macOS + windows | Maximum compatibility, expensive | |

| Option | Description | Selected |
|--------|-------------|----------|
| Mend Renovate GitHub App | No maintenance, Dependency Dashboard; install is an approval gate | ✓ |
| Self-hosted `renovatebot/github-action` | SHA-pinned, needs token and upkeep | |

| Option | Description | Selected |
|--------|-------------|----------|
| Patch/digest non-runtime only | Actions digests and dev-tool patches automerged on green CI | ✓ |
| No automerge | Everything manual | |
| All non-major | Risky for Axon/Spring | |

---

## Governance as Code

| Option | Description | Selected |
|--------|-------------|----------|
| Gradle tasks in build-logic | Kotlin, TestKit-tested, part of `check`; label check in workflow | ✓ |
| bash/jq scripts | Simpler, weaker testability | |
| `*.main.kts` scripts | Same language, slow start | |

| Option | Description | Selected |
|--------|-------------|----------|
| Declarative + apply script + drift check | Ruleset JSON in repo, `gh api` apply after approval, read-only CI drift check | ✓ |
| Declarative without drift check | No admin-read token needed | |
| One-off `gh` + docs | No history in repo | |

| Option | Description | Selected |
|--------|-------------|----------|
| Exemption for Renovate version-only PRs | Bot PRs changing only versions/digests skip the label | ✓ |
| Always require label | Stricter, blocks Actions digest automerge | |

| Option | Description | Selected |
|--------|-------------|----------|
| Pin whole config.json | Exact expected copy | ✓ |
| Critical keys only | `git.*`, `commit_docs`, `create_tag`, model overrides, key gates | |

---

## Tooling Inventory, Labels & Build Performance

| Option | Description | Selected |
|--------|-------------|----------|
| Hand-written table + CI cross-check | No versions in doc; Gradle check against `mise.toml` | ✓ |
| Generated from metadata | YAML descriptor → Markdown, drift check | |
| Plain hand-written doc | No automatic check | |

| Option | Description | Selected |
|--------|-------------|----------|
| Prefix groups | `type:`, `area:`, `phase:N`, `deps:*`, `autorelease:*`, `upstream`, `governance-approved`, `security` | ✓ |
| Minimal set | Only tool-required labels | |
| Prefix groups + community labels | Adds `good first issue` etc. | |

| Option | Description | Selected |
|--------|-------------|----------|
| Enable isolated projects if green, else ADR | Flag on; fallback with upstream record and ADR | ✓ |
| IP-compatible code, flag off | Verified only in TestKit | |

| Option | Description | Selected |
|--------|-------------|----------|
| Scans opt-in, gradle-profiler scenarios in repo | No scans in CI; manual profiling | ✓ |
| Scans in CI | Public build data, needs approval | |
| Periodic perf job in CI | Weekly profiler run, noisy | |

**Notes:** The user asked for details on the extra areas (tooling doc, labels, Gradle performance) before deciding.

## Claude's Discretion

- What happens to the sample module after Phase 1, and its name.
- Job breakdown inside `ci.yml` and caching details.
- Exact detekt strictness beyond "no baseline, `comments` on".
- File names for the policy, ruleset and apply script, and the label colours.

## Deferred Ideas

- Community labels → Phase 13.
- Scheduled gradle-profiler CI job → possibly Phase 12.
- macOS/Windows CI matrix → not adopted.
