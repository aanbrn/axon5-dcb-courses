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

Versions are never written in this document. Each row of the tables below points to the single
place where its version is declared, so there is exactly one thing to update when a tool moves.

## Tools

The `Version source` column holds `mise` for every tool pinned in the `[tools]` table of `mise.toml`.
For those rows the `Tool` column also carries the exact `mise.toml` key in backticks.

| Tool                                                          | Purpose                                                               | Version source                             | Installation                                                                                       | Used in         |
| ------------------------------------------------------------- | --------------------------------------------------------------------- | ------------------------------------------ | -------------------------------------------------------------------------------------------------- | --------------- |
| mise                                                          | Installs and runs the pinned CLI tools                                | `mise.toml (min_version)`                  | Homebrew or the mise installer (host prerequisite); CI installs the version named by `min_version` | local, CI       |
| JDK                                                           | Compiles and runs the Kotlin code                                     | `.java-version`                            | mise, through `idiomatic_version_file_enable_tools`; CI uses `setup-java` on the same file         | local, CI       |
| Node.js                                                       | Runs Prettier for Spotless; later the frontend toolchain              | `.nvmrc`                                   | mise, through `idiomatic_version_file_enable_tools`; CI uses `setup-node` on the same file         | local, CI       |
| Gradle                                                        | Build tool                                                            | `gradle/wrapper/gradle-wrapper.properties` | The Gradle wrapper (`./gradlew`)                                                                   | local, CI, hook |
| Spotless                                                      | Formatting and licence-header checks                                  | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                | local, CI, hook |
| ktfmt                                                         | Kotlin formatter run by Spotless                                      | `gradle/libs.versions.toml`                | Resolved by Spotless                                                                               | local, CI, hook |
| Prettier                                                      | Formatter for Markdown, YAML and JSON run by Spotless                 | `gradle/libs.versions.toml`                | Installed by Spotless through Node.js                                                              | local, CI, hook |
| detekt                                                        | Static analysis for Kotlin                                            | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                | local, CI       |
| Dokka                                                         | KDoc documentation generation and checks                              | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                | local, CI       |
| Kotlin                                                        | Language and compiler                                                 | `gradle/libs.versions.toml`                | Gradle plugin resolved by the build                                                                | local, CI       |
| JUnit                                                         | Test platform                                                         | `gradle/libs.versions.toml`                | Dependency resolved by Gradle                                                                      | local, CI       |
| Kotest                                                        | Assertion library                                                     | `gradle/libs.versions.toml`                | Dependency resolved by Gradle                                                                      | local, CI       |
| GitHub Actions                                                | CI workflows and the actions they call                                | `SHA pins in .github/workflows`            | Resolved by GitHub when a workflow runs                                                            | CI              |
| git                                                           | Version control                                                       | `host (not pinned)`                        | Host package manager (host prerequisite)                                                           | local, CI, hook |
| Docker-compatible engine (colima, OrbStack or Docker Desktop) | Runs containers for tests and the local stack; needed from Phase 2    | `host (not pinned)`                        | Host installation (host prerequisite)                                                              | local           |
| gitleaks (`gitleaks`)                                         | Secret scanning                                                       | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| actionlint (`aqua:rhysd/actionlint`)                          | Lints GitHub Actions workflows                                        | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| shellcheck (`shellcheck`)                                     | Lints shell scripts                                                   | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| lefthook (`lefthook`)                                         | Git hook runner                                                       | `mise`                                     | `mise install`, then `lefthook install` once per clone                                             | local, hook     |
| zizmor (`zizmor`)                                             | Security linter for GitHub Actions workflows                          | `mise`                                     | `mise install`                                                                                     | local, CI       |
| editorconfig-checker (`editorconfig-checker`)                 | Checks files against `.editorconfig`                                  | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| Python (`python`)                                             | Interpreter for the pipx tools, installed from their dependency locks | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| uv (`uv`)                                                     | Installs the pipx tools against their committed dependency locks      | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| reuse (`pipx:reuse`)                                          | REUSE licence-compliance linter                                       | `mise`                                     | `mise install` (installed with its `charset-normalizer` extra)                                     | local, CI, hook |
| gradle-profiler (`github:gradle/gradle-profiler`)             | Build performance profiling                                           | `mise`                                     | `mise install`                                                                                     | local, CI       |
| shfmt (`aqua:mvdan/sh`)                                       | Shell script formatter run by Spotless                                | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| taplo (`aqua:tamasfe/taplo`)                                  | TOML formatter run by Spotless                                        | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| renovate (`npm:renovate`)                                     | Validates the Renovate configuration with `renovate-config-validator` | `mise`                                     | `mise install`                                                                                     | local, CI, hook |
| jq (`jq`)                                                     | JSON processing in scripts                                            | `mise`                                     | `mise install`                                                                                     | local, CI       |
| gh (`gh`)                                                     | GitHub command-line client                                            | `mise`                                     | `mise install`                                                                                     | local, CI       |

## Planned tools

These tools are named by the tooling requirement but are introduced by later phases. Their
`Version source` is `not yet pinned (Phase N)`, and the `Installation` column names the intended
route. When a phase pins a tool, it moves the row into the main table and corrects the route if
it chose a different one.

| Tool           | Purpose                                     | Version source                          | Installation                                                   | Used in         |
| -------------- | ------------------------------------------- | --------------------------------------- | -------------------------------------------------------------- | --------------- |
| pnpm           | Frontend package manager                    | `not yet pinned (Phase 5)`              | Corepack, from `packageManager` in `package.json` (Phase 5)    | local, CI, hook |
| pack           | Cloud Native Buildpacks command-line tool   | `not yet pinned (Phase 6)`              | `mise.toml` (Phase 6)                                          | local, CI       |
| trivy          | Filesystem and image vulnerability scanning | `not yet pinned (Phase 6)`              | `mise.toml` (Phase 6)                                          | local, CI       |
| grype          | Second-opinion vulnerability scan on SBOMs  | `not yet pinned (Phase 6)`              | `mise.toml` (Phase 6)                                          | local, CI       |
| syft           | Image SBOM generation                       | `not yet pinned (Phase 12)`             | `mise.toml` (Phase 12)                                         | CI              |
| cosign         | Keyless image signing and attestation       | `not yet pinned (Phase 12)`             | `mise.toml` (Phase 12)                                         | CI              |
| Structurizr    | C4 model as code, exported to diagrams      | `not yet pinned (Phase 13)`             | Container image by tag and digest, from the catalog (Phase 13) | local, CI       |
| act            | Runs GitHub Actions workflows locally       | `not yet pinned (phase that adopts it)` | `mise.toml` when adopted                                       | local           |
| docker compose | Runs the whole system locally               | `not yet pinned (Phase 2)`              | Bundled with the Docker-compatible engine (host)               | local, CI       |

## Notes

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
