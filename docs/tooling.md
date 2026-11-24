# Tooling

This repository pins every tool it depends on, and this page is the inventory.

## Getting started

1. Install [mise](https://mise.jdx.dev/) and activate it in your shell, so that Node.js, shfmt and
   taplo are on `PATH` when Spotless runs. Without activation, prefix commands with `mise x --`
   (for example `mise x -- ./gradlew build`).
2. Run `mise install` in the repository root. It installs every tool pinned in `mise.toml` and
   verifies the downloads against the checksums committed in `mise.lock`. The JDK and Node.js come
   from `.java-version` and `.nvmrc`.
3. Run `lefthook install` once per clone to enable the git hooks.
4. Run `./gradlew build` to build and check the whole multi-project build.
5. Run `scripts/doctor.sh` whenever something fails for no obvious reason. It checks the environment and
   prints the command that fixes each problem (see [Environment check](#environment-check-doctor)).

Versions are never written in this document. Each row of the tables below points to the single
place where its version is declared, so there is exactly one thing to update when a tool moves.

## Tools

The `Version source` column holds `mise` for every tool pinned in the `[tools]` table of `mise.toml`.
For those rows the `Tool` column also carries the exact `mise.toml` key in backticks.

| Tool                                                          | Purpose                                                                                                 | Version source                             | Installation                                                                                                                                                                               | Used in             |
| ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------- |
| mise                                                          | Installs and runs the pinned CLI tools                                                                  | `mise.toml (min_version)`                  | Homebrew or the mise installer (host prerequisite); CI installs the version named by `min_version`                                                                                         | local, CI           |
| JDK                                                           | Compiles and runs the Kotlin code                                                                       | `.java-version`                            | mise, through `idiomatic_version_file_enable_tools`; CI uses `setup-java` on the same file                                                                                                 | local, CI           |
| Node.js                                                       | Runs Prettier for Spotless; later the frontend toolchain                                                | `.nvmrc`                                   | mise, through `idiomatic_version_file_enable_tools`; CI uses `setup-node` on the same file                                                                                                 | local, CI           |
| Gradle                                                        | Build tool                                                                                              | `gradle/wrapper/gradle-wrapper.properties` | The Gradle wrapper (`./gradlew`)                                                                                                                                                           | local, CI, hook     |
| Spotless                                                      | Formatting and licence-header checks                                                                    | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                                                                                                        | local, CI, hook     |
| ktfmt                                                         | Kotlin formatter run by Spotless                                                                        | `gradle/libs.versions.toml`                | Resolved by Spotless                                                                                                                                                                       | local, CI, hook     |
| Prettier                                                      | Formatter for Markdown, YAML and JSON run by Spotless                                                   | `gradle/libs.versions.toml`                | Installed by Spotless through Node.js                                                                                                                                                      | local, CI, hook     |
| detekt                                                        | Static analysis for Kotlin                                                                              | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                                                                                                        | local, CI           |
| Dokka                                                         | KDoc documentation generation and checks                                                                | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                                                                                                        | local, CI           |
| Kotlin                                                        | Language and compiler                                                                                   | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                                                                                                        | local, CI           |
| JUnit                                                         | Test platform                                                                                           | `gradle/libs.versions.toml`                | Dependency resolved by Gradle                                                                                                                                                              | local, CI           |
| Kotest                                                        | Assertion library                                                                                       | `gradle/libs.versions.toml`                | Dependency resolved by Gradle                                                                                                                                                              | local, CI           |
| Konsist                                                       | Architecture rules over the Kotlin sources of the repository                                            | `gradle/libs.versions.toml`                | Dependency resolved by Gradle                                                                                                                                                              | local, CI           |
| Testcontainers                                                | Starts the containers of component and integration tests                                                | `gradle/libs.versions.toml`                | Dependency resolved by Gradle                                                                                                                                                              | local, CI           |
| Test container images (`image-*`)                             | Axon Server, PostgreSQL and the Ryuk reaper that the test suites start                                  | `gradle/libs.versions.toml (image-*)`      | Pulled by Testcontainers by tag and digest                                                                                                                                                 | local, CI           |
| CycloneDX                                                     | Per-module SBOM of the runtime classpaths, read by the licence check; Phase 6 reuses it for image SBOMs | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                                                                                                        | local, CI           |
| Kover                                                         | Coverage of every JVM test suite of a module (aggregation and thresholds: Phase 3)                      | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                                                                                                        | local, CI           |
| GitHub Actions                                                | CI workflows and the actions they call                                                                  | `SHA pins in .github/workflows`            | Resolved by GitHub when a workflow runs                                                                                                                                                    | CI                  |
| git                                                           | Version control                                                                                         | `host (not pinned)`                        | Host package manager (host prerequisite)                                                                                                                                                   | local, CI, hook     |
| Docker-compatible engine (colima, OrbStack or Docker Desktop) | Runs containers for tests, the local Qodana run and the local stack (from Phase 2)                      | `host (not pinned)`                        | Host installation (host prerequisite)                                                                                                                                                      | local               |
| docker compose                                                | Runs the whole system locally from `compose.yaml`                                                       | `host (not pinned)`                        | Bundled with the Docker-compatible engine (host prerequisite)                                                                                                                              | local, CI           |
| Compose container images (`image-*`)                          | Axon Server and PostgreSQL of the local stack                                                           | `gradle/libs.versions.toml (image-*)`      | Pulled by Docker Compose by tag and digest, generated into `compose/versions.env`                                                                                                          | local, CI           |
| GSD                                                           | Agent workflow commands, agents and hooks for Claude Code                                               | `governance/gsd-version`                   | `npx -y @opengsd/gsd-core@<version from governance/gsd-version> --claude --local`; Renovate proposes new releases, adopted only after the review in [GSD updates](#gsd-updates) (AIDEV-07) | local (Claude Code) |
| Claude Code                                                   | Agent command-line client                                                                               | `host (not pinned)`                        | Homebrew cask `claude-code@latest`; the minimum release is enforced by `scripts/doctor.sh`                                                                                                 | local               |
| Qodana JVM Community                                          | Runs the IntelliJ IDEA inspections (profile `Default`) as the required `qodana` CI gate                 | `qodana.yaml (linter)`                     | Container image by tag and digest, run by `scripts/qodana.sh` on the Docker-compatible engine                                                                                              | local, CI           |
| IntelliJ IDEA                                                 | IDE; its year.minor must equal the Qodana image tag                                                     | `qodana.yaml (linter year.minor)`          | Host installation (host prerequisite)                                                                                                                                                      | local               |
| gitleaks (`gitleaks`)                                         | Secret scanning                                                                                         | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| actionlint (`aqua:rhysd/actionlint`)                          | Lints GitHub Actions workflows                                                                          | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| shellcheck (`shellcheck`)                                     | Lints shell scripts                                                                                     | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| lefthook (`lefthook`)                                         | Git hook runner                                                                                         | `mise`                                     | `mise install`, then `lefthook install` once per clone                                                                                                                                     | local, hook         |
| zizmor (`zizmor`)                                             | Security linter for GitHub Actions workflows                                                            | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI           |
| editorconfig-checker (`editorconfig-checker`)                 | Checks files against `.editorconfig`                                                                    | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| Python (`python`)                                             | Interpreter for the pipx tools, installed from their dependency locks                                   | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| uv (`uv`)                                                     | Installs the pipx tools against their committed dependency locks                                        | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| reuse (`pipx:reuse`)                                          | REUSE licence-compliance linter                                                                         | `mise`                                     | `mise install` (installed with its `charset-normalizer` extra)                                                                                                                             | local, CI, hook     |
| gradle-profiler (`github:gradle/gradle-profiler`)             | Build performance profiling                                                                             | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI           |
| shfmt (`aqua:mvdan/sh`)                                       | Shell script formatter run by Spotless                                                                  | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| taplo (`aqua:tamasfe/taplo`)                                  | TOML formatter run by Spotless                                                                          | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| renovate (`npm:renovate`)                                     | Validates the Renovate configuration with `renovate-config-validator`                                   | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI, hook     |
| jq (`jq`)                                                     | JSON processing in scripts                                                                              | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI           |
| gh (`gh`)                                                     | GitHub command-line client                                                                              | `mise`                                     | `mise install`                                                                                                                                                                             | local, CI           |
| Context7 MCP server (`npm:@upstash/context7-mcp`)             | Current library documentation for Claude Code (Axon 5, Gradle 9, Spring Boot 4)                         | `mise`                                     | `mise install`                                                                                                                                                                             | local (Claude Code) |
| Playwright MCP server (`npm:@playwright/mcp`)                 | Browser-driven UI verification for Claude Code                                                          | `mise`                                     | `mise install`                                                                                                                                                                             | local (Claude Code) |
| AxonIQ agent skills (`axoniq-app-development@axoniq`)         | Axon 5 entity, DCB, event store and testing guidance for Claude Code                                    | `.claude/settings.json (ref)`              | Claude Code marketplace plugin                                                                                                                                                             | local (Claude Code) |
| Gradle skills (`gradle-skills@gradle-skills`)                 | Gradle build best practices and wrapper-upgrade guidance for Claude Code                                | `.claude/settings.json (ref)`              | Claude Code marketplace plugin                                                                                                                                                             | local (Claude Code) |

For a fast loop while developing, run `./gradlew test` or `./gradlew componentTest`; `./gradlew check` and
`./gradlew build` run every suite, including the integration tests (CI runs those in their own job).

The `architectureTest` suite of `:tooling:architecture` runs from `check` and scans the Kotlin sources of the whole
repository with Konsist. Its rule classes are `ContractsArchitectureTest` (no framework logic in the contracts),
`MessageArchitectureTest` (explicit message names and versions, registry tag keys), `TestNamingArchitectureTest`
(test suffix per suite, block-bodied tests), `ContainerImageArchitectureTest` (no image literals) and
`ScopeSanityArchitectureTest` (every rule sees the modules it must). The scanned files are declared task inputs.

## Planned tools

These tools are named by the tooling requirement but are introduced by later phases. Their
`Version source` is `not yet pinned (Phase N)`, and the `Installation` column names the intended
route. When a phase pins a tool, it moves the row into the main table and corrects the route if
it chose a different one.

| Tool        | Purpose                                     | Version source                          | Installation                                                   | Used in         |
| ----------- | ------------------------------------------- | --------------------------------------- | -------------------------------------------------------------- | --------------- |
| pnpm        | Frontend package manager                    | `not yet pinned (Phase 5)`              | Corepack, from `packageManager` in `package.json` (Phase 5)    | local, CI, hook |
| pack        | Cloud Native Buildpacks command-line tool   | `not yet pinned (Phase 6)`              | `mise.toml` (Phase 6)                                          | local, CI       |
| trivy       | Filesystem and image vulnerability scanning | `not yet pinned (Phase 6)`              | `mise.toml` (Phase 6)                                          | local, CI       |
| grype       | Second-opinion vulnerability scan on SBOMs  | `not yet pinned (Phase 6)`              | `mise.toml` (Phase 6)                                          | local, CI       |
| syft        | Image SBOM generation                       | `not yet pinned (Phase 12)`             | `mise.toml` (Phase 12)                                         | CI              |
| cosign      | Keyless image signing and attestation       | `not yet pinned (Phase 12)`             | `mise.toml` (Phase 12)                                         | CI              |
| Structurizr | C4 model as code, exported to diagrams      | `not yet pinned (Phase 13)`             | Container image by tag and digest, from the catalog (Phase 13) | local, CI       |
| act         | Runs GitHub Actions workflows locally       | `not yet pinned (phase that adopts it)` | `mise.toml` when adopted                                       | local           |

## Qodana

- The `qodana` CI gate runs every IntelliJ IDEA inspection of profile `Default` over the whole tree
  and fails on any problem (`failThreshold: 0`, no baseline). It is required through `ci-success`.
- IDE parity rule: the year.minor of the image tag in `qodana.yaml` must equal the IntelliJ IDEA
  release used for development. An older image lacks inspections the IDE has, and only profile
  `Default` matches the IDE's defaults. When Renovate proposes a new year.minor, update the IDE in
  the same step; a digest-only update needs nothing.
- Two exclusions keep the gate quiet without hiding real findings. Build output and generated Kotlin
  DSL accessors are excluded, and a new module adds its build directory to `qodana.yaml`.
  `EditorConfigOptionRedundancy` is excluded for `.editorconfig`, because INFRA-14 requires it to
  define settings for every file type explicitly.
- The policy is to fix a finding. When the fix reads worse, add `@Suppress("<InspectionId>")` with a
  reason comment.
- Local run: `scripts/qodana.sh` analyses the committed HEAD with the same image and flags as CI, in
  `QODANA_WORK_DIR`. The first run takes about half an hour (a multi-GB image, Gradle and JDK
  downloads), later runs about ten minutes. On colima the work directory must be under `$HOME`.

## Docker engine and Testcontainers

Any Docker-compatible engine works, because the tests, the compose stack, the buildpack builds and the local Qodana run
need only a reachable Docker API. colima is the engine this repository was verified on. Docker Desktop is supported
through the automatic detection of Testcontainers; that half is documented here but not verified, because no Docker
Desktop is available on the author's machine.

- colima, the verified setup:
  - `~/.testcontainers.properties` holds `tc.host=unix:///var/run/docker.sock`. Testcontainers has no docker-context
    discovery, so it gets the colima daemon explicitly. The `/var/run` path is the daemon socket inside the VM, so Ryuk
    can mount it. `scripts/doctor.sh` reports the `tc.host` and `docker.host` keys by name and never their values.
  - `network.dns` in the colima configuration lists at least one resolver (`colima start --dns <address>`), because the
    default forwarder can stall image pulls on AAAA lookups.
  - `docker.features.containerd-snapshotter` is `false`, the classic image store. The containerd store behaves
    differently for image pulls and builds, and `doctor` warns when it is active.
- colima, the documented Testcontainers alternative: start colima with `--network-address`, export `DOCKER_HOST` with the
  colima socket (`unix://$HOME/.colima/default/docker.sock`), `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`
  and `TESTCONTAINERS_HOST_OVERRIDE` with the address from `colima ls -j`.
- Docker Desktop needs no setting: Testcontainers finds its socket by itself.
- Ryuk, the reaper that removes the containers of a crashed test run, stays enabled. Its image is pinned in the version
  catalog (`image-ryuk`) and reaches Testcontainers through `TESTCONTAINERS_RYUK_CONTAINER_IMAGE`, in the test suites and
  in the `doctor` probe alike. `doctor` warns when Ryuk is disabled.

## Environment check (doctor)

`scripts/doctor.sh` diagnoses the environment and prints every check as `ok`, `warn`, `fail` or `skip`, with the exact
command that fixes it. It exits with a non-zero status when a check fails, and it never changes anything: it installs,
updates, starts and deletes nothing, and it never prints a secret (the Platform token is only classified as set, empty or
absent). It runs as many checks as it can instead of stopping at the first failure.

- The script itself runs the checks that must work before Gradle can start, and the ones Gradle does not own: mise and
  its pinned tools, a Java runtime for the Gradle wrapper, the Docker engine and its image store, the Testcontainers
  settings, the pinned GSD installation against the hashes of its manifest (modified hooks and unexpected files fail),
  the global GSD defaults, the MCP servers, the Platform token, colima, the Claude Code release, the tracked
  `.claude/` files and the upstream-removal reminder: every `docs/upstream` record whose status is `fixed-upstream`
  while its workaround markers remain in a tracked file is listed with its removal criteria, whatever its product (a
  warning, never a failure).
- `./gradlew doctor` runs the rest, so that every check has one implementation: the Node.js major
  (`verifyNodeVersion`), the formatter binaries (`verifyShfmt`, `verifyTaplo`), the daemon JVM criteria
  (`checkDaemonJvm`) and Testcontainers connectivity through the Testcontainers library itself
  (`testcontainersProbe`). The script calls it last, with `--continue`, so every failure shows its task's own fix hint.
  Without a Java runtime the script reports that as the failure and skips this step.
- `scripts/doctor.sh --ci` is what the `Doctor` CI job runs. It skips the checks that only make sense on a developer
  machine: GSD, the global GSD defaults, the MCP servers, the Platform token, colima and the Claude Code release. The
  tracked-file checks (`.claude/` and upstream-removal) read only repository files, so they run in `--ci` too.

## MCP servers

- Project-scope MCP servers live in `.mcp.json` and start from the mise-installed binaries
  (`mise x -- context7-mcp`, `mise x -- playwright-mcp`), so each one is pinned once, in `mise.toml`.
  Each user approves them in Claude Code the first time it asks; the repository never pre-approves
  them (`claude mcp list` shows them as pending until then).
- Context7 works without a key. To lift its rate limit, export `CONTEXT7_API_KEY` in your own
  environment; `.mcp.json` carries no `env` block and no secret.
- Playwright MCP runs `--isolated` (in-memory browser profile) and writes its output to
  `.playwright-mcp/`, which is git-ignored. Install the browser once, through the server's
  `browser_install` tool or the Playwright install command for the bundled version.
- Personal servers are never committed. Add the IntelliJ MCP server and, if you want it, the GitHub
  MCP server in local scope: `claude mcp add --scope local <name> ...`.
- Updates arrive as Renovate pull requests. They are never automerged: read the changelog, run
  `claude mcp list`, and merge by hand.

## GSD updates

- Only the pin `governance/gsd-version` is committed. The installation under `.claude/` (commands, agents, hooks,
  `gsd-core`) is ignored. Doctor's `gsd` step (local only, skipped by `--ci`) checks that the installed version equals
  the pin and that every file matches the hashes of the release manifest.
- Renovate is the signal. A regex custom manager reads the pin as the npm package `@opengsd/gsd-core`, with the
  repository's seven-day minimum release age, restated on the GSD rule because `config:best-practices` lowers npm
  packages to three days. The pull request comes from a `renovate/opengsd-gsd-core-*` branch, carries `deps` and
  `deps:npm`, is never automerged, changes only the pin, and its body repeats the steps below.
- The governance decision, stated plainly: `governance/**` is governed, and the renovate[bot] version-only exemption of
  `scripts/governance/check-pr.sh` deliberately does not cover the pin (`NEVER_EXEMPT_PATHS`,
  [ADR-0003](adr/0003-governance-gate-trust-boundary.md)). A GSD release replaces hooks that execute in the maintainer's
  Claude Code session, so the pull request needs the `governance-approved` label from a human.
- Before merging:
  1. Read the release notes, in the pull request body or in the changelog that `/gsd-update` shows.
  2. Review the hook diff with
     `npm diff --ignore-scripts --diff=@opengsd/gsd-core@<old> --diff=@opengsd/gsd-core@<new> hooks`, which runs no
     package scripts.
  3. Check out the pull request branch (`gh pr checkout <number>`) and install the release with
     `npx -y @opengsd/gsd-core@<new> --claude --local`, or with `/gsd-update` only when the release it offers is exactly
     `<new>`.
  4. Run `scripts/doctor.sh` until `gsd` reports ok.
  5. Add `governance-approved` and merge by hand.
- After merging, every other checkout's doctor fails with "installed GSD <old> differs from governance/gsd-version
  <new>" and prints the reinstall command for `<pin>` as its fix. Run it and re-run doctor. An installation ahead of the
  pin fails the same way, so a new release reaches a checkout only through the pin.
- Security pull requests: Renovate skips the cooldown for a vulnerability fix, but the rule never automerges and the
  label is still required.

## Claude Code skills

Third-party skills are agent instructions, so each one is vetted before it is enabled. Only the
areas of Phases 2 to 4 (Axon, Kotlin, Gradle, Spring Boot, GitHub Actions) get skills now; every
other area records its decision here and waits for its phase.

### How skills are installed

- A skill is installed only as a marketplace plugin. The marketplace is registered in
  `.claude/settings.json` under `extraKnownMarketplaces`, with `ref` set to a full 40-character
  commit SHA, and the plugin is enabled under `enabledPlugins`. A branch, a tag or `latest` is never
  used as `ref`.
- Nothing is copied into `.claude/`. A plugin lives in Claude Code's own plugin cache.
- Updates are deliberate pull requests that change the SHA after the new content has been reviewed
  against the criteria below. Nothing updates itself.
- Pinning a marketplace by commit needs a minimum Claude Code version. `scripts/doctor.sh` checks
  it, so this page names no version. The stable release channel of Claude Code (for example the
  Homebrew cask `claude-code`) can lag behind that minimum; if so, use the `latest` channel (the cask
  `claude-code@latest`).
- A plugin is enabled only after the author approved that candidate and that commit. The approval is
  recorded in the pull request.

### Review criteria

Every candidate is judged on the same points:

1. Owner and licence.
2. Markdown-only content: no hooks, scripts, MCP or LSP configuration, and no downloads at run time.
3. Pinned by full commit SHA.
4. No auto-update.
5. What it can read or run, once enabled.
6. Explicit author approval, recorded in the pull request.

When third-party guidance conflicts with the project's `spike-findings-axon5-dcb-courses` skill or
with an ADR, the skill and the ADR win. Third-party output can be wrong and still has to pass every
quality gate.

### Survey

Areas follow the order of the tooling requirement. A verdict is one of install, defer to its phase,
reject, or none found. A plugin whose verdict reads "installed" is pinned in `.claude/settings.json`
under `extraKnownMarketplaces` (`ref`).

| Area           | Candidate                                                                                 | Evidence                                                                                                                                                                                                                                                                                                                    | Verdict                                              | Phase  |
| -------------- | ----------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- | ------ |
| Axon           | `AxonIQ/agent-skills`, plugin `axoniq-app-development`                                    | Official AxonIQ organisation, Apache-2.0. The plugin directory holds Markdown and `plugin.json` only; the hooks and settings at the repository root are the maintainers' own development files, outside the plugin. Covers entities, DCB decision models, the event store and testing. Its README warns about wrong output. | Installed (pinned commit in `.claude/settings.json`) | 2 to 4 |
| Axon           | `AxonIQ/agent-skills`, plugins `axoniq-migration` and `axoniq-framework-contribution`     | Same repository. Aimed at Axon 4 to 5 migration and at contributors to the framework; the migration plugin also ships scripts.                                                                                                                                                                                              | Reject (not applicable)                              | none   |
| Kotlin         | `Kotlin/kotlin-agent-skills`                                                              | Official organisation, Apache-2.0. Skills cover JPA entity mapping and Android Gradle plugin migration, neither of which is used here. The plugin bundles an `.mcp.json` that points at an unpinned remote server.                                                                                                          | Reject (not applicable, unpinned remote MCP)         | none   |
| Kotlin         | Kotlin idioms                                                                             | No official skill found; the search returned only aggregator listings of community skills.                                                                                                                                                                                                                                  | None found (Context7 supplies Kotlin docs)           | none   |
| Gradle         | `gradle/gradle-skills`, plugin `gradle-skills`                                            | Official Gradle organisation, Apache-2.0. Two Markdown skills (build best practices, wrapper upgrade) with reference files. The plugin source is the repository root, so CI-only scripts and evaluation notes come along but are never run. No hooks, MCP or LSP configuration.                                             | Installed (pinned commit in `.claude/settings.json`) | 2 to 4 |
| Spring Boot    | `spring-projects/spring-tools` Claude plugin                                              | Official, EPL-1.0. Installs from a CDN marketplace URL, so it cannot be pinned by commit; it downloads and runs a language server and ships hooks and MCP tools.                                                                                                                                                            | Defer until it can be pinned by commit               | later  |
| Spring Boot    | Community Spring skills                                                                   | Several listings, none vetted.                                                                                                                                                                                                                                                                                              | Reject (unvetted)                                    | none   |
| Playwright     | `microsoft/playwright-cli` skills                                                         | Official, Apache-2.0. The Playwright MCP server already covers browser checks.                                                                                                                                                                                                                                              | Defer to the UI phases                               | 6 to 7 |
| React/shadcn   | `shadcn-ui/ui` skill                                                                      | MIT. Reviewed by metadata only.                                                                                                                                                                                                                                                                                             | Defer to the frontend phase                          | 5      |
| FSD            | `feature-sliced/skills`                                                                   | The repository declares no licence, so reuse terms are unclear.                                                                                                                                                                                                                                                             | Defer to the frontend phase (licence unclear)        | 5      |
| GitHub Actions | `trailofbits/skills`, skill `agentic-actions-auditor`                                     | CC-BY-SA-4.0. Targets workflows that invoke AI agents, and this repository has none. zizmor and actionlint already gate CI.                                                                                                                                                                                                 | Reject (no benefit)                                  | none   |
| GitHub Actions | zizmor wrappers (`Seldaek/zizmorify`, the harden-github-actions skill of `alexop-skills`) | They duplicate zizmor, which CI already runs.                                                                                                                                                                                                                                                                               | Reject (duplicate)                                   | none   |
| Paketo         | none                                                                                      | No skill found for Cloud Native Buildpacks or Paketo.                                                                                                                                                                                                                                                                       | None found                                           | none   |

## Tracked Claude Code files

Only these files under `.claude/` are tracked:

- `.claude/settings.json`, the shared project settings: the governance ask rules and hooks
  ([ADR-0015](adr/0015-governance-hooks-for-worktrees-and-merges.md)), whose scripts live in
  `scripts/governance/`, and the marketplace pins above.
- `.claude/CLAUDE.md`, the project instructions.
- `.claude/skills/spike-findings-*/`, the spike wrap-up skill that GSD generates (a narrow exception
  from the rule that ignores everything else there).

The GSD installation and `settings.local.json` stay ignored. The project's own skills will live in a
repository-local plugin outside `.claude/`, introduced in Phase 8.

## Notes

- The dependency licence policy lives in `governance/dependency-licences.json`. `checkDependencyLicences`
  checks it in every Kotlin module from `check`, CI's `build` job names the task, and every exception is
  mirrored in `NOTICE` (checked by `checkNotice`). The mise-pinned npm developer tools (the Context7 and
  Playwright MCP servers) are outside the licence check, because they are neither shipped with nor linked
  into the system; the approval gate for agent tooling reviews them instead (AIDEV-04). npm project
  dependencies join the same policy in Phase 5, with the frontend's `package.json` and `pnpm-lock.yaml`,
  as the requirement traceability records.
- actionlint is pinned through the explicit `aqua:rhysd/actionlint` backend, because the short
  registry alias is marked deprecated in favour of an unvetted fork.
- CI installs only the CLI tools through mise. The JDK and Node.js come from `setup-java` and
  `setup-node`, which read the same `.java-version` and `.nvmrc` files.
- CI jobs that run without mise use the `gh` and `jq` preinstalled on the runner image.
- `mise.lock` (with the sidecar lockfiles under `.mise/locks`) is enforced through `locked = true`,
  so mise installs only what is pinned there, verified against the recorded checksums.
- `renovate` (`npm:renovate`) is installed under the npm trust policy of mise, with one narrow,
  exact-version exception for a transitive dependency that was published without provenance. The
  reason and the removal condition are documented next to the entry in `mise.toml`.
