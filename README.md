# Axon 5 DCB Courses

[![REUSE status](https://api.reuse.software/badge/github.com/aanbrn/axon5-dcb-courses)](https://api.reuse.software/info/github.com/aanbrn/axon5-dcb-courses)

A reference implementation of the classic "students subscribe to courses" domain built on Axon Framework 5 using Dynamic Consistency Boundaries (DCB) instead of aggregates, inspired by the article "Killing the Aggregate". It is a learning example engineered to production-grade standards: architecture, code, tests, documentation, CI/CD and observability. It is aimed at developers moving from Axon 4 aggregates to DCB.

## Quick start

1. Install [mise](https://mise.jdx.dev/) and activate it in your shell, so that Node.js, shfmt and taplo are on `PATH` for the formatters. Without activation, prefix commands with `mise x --` (for example `mise x -- ./gradlew build`).
2. Run `mise install` in the repository root to install every pinned tool.
3. Run `lefthook install` once per clone to enable the git hooks.
4. Run `./gradlew build` to build and check the whole multi-project build.

The full tool inventory, with the single place where each version is declared, is in [docs/tooling.md](docs/tooling.md).

## Licence

The project is licensed under the [MIT licence](LICENSE) and is [REUSE](https://reuse.software/) compliant: every file carries licence information, either in an SPDX header or in `REUSE.toml`. The Gradle wrapper files keep their Apache-2.0 licence. Run `mise x -- reuse lint` to check compliance.
