---
id: "UP-0010"
product: "action:github/codeql-action"
product-version: "4.39.4"
status: "open"
workaround: true
upstream: []
reported: null
updated: "2026-11-23"
---

# UP-0010: The CodeQL Kotlin extractor does not cover Kotlin 2.4.21 and extracted no sources from the Gradle build

## Symptom

CodeQL analyses the `actions` language of this repository only. A spike for the `java-kotlin` language (ADR-0002) built
the project with the extractor attached and ended with:

```text
ERROR: CodeQL detected code written in Java/Kotlin but could not process any of it.
```

Gradle itself compiled the Kotlin sources (`BUILD SUCCESSFUL`), so the extractor saw no sources while the compile ran.
Kotlin sources therefore have no CodeQL analysis; the compiler (all warnings are errors) and detekt are the static
analysis of Kotlin.

Two conditions of the spike could each explain the result, and the spike did not separate them:

- the CodeQL bundle that matches `github/codeql-action` 4.39.4 (CodeQL bundle 2.27.5) documents Kotlin extractor
  support from 1.8.0 to 2.4.20, while the build uses Kotlin 2.4.21;
- the build runs with Gradle's Isolated Projects, which forces the configuration cache on and rejects
  `--no-configuration-cache`, the flag the spike first used to force a fresh, observable compile.

## Minimal reproduction

The spike of ADR-0002, on macOS with Apple Silicon and the published `osx64` bundle of CodeQL 2.27.5, unpacked outside
the repository:

```sh
codeql database create "$TMPDIR/db" --language=java-kotlin --source-root . \
  --command "./gradlew --no-daemon --no-build-cache --rerun-tasks <compile task> -Pkotlin.compiler.execution.strategy=in-process"
```

with a Gradle build that sets `org.gradle.isolated-projects=true` and compiles one Kotlin class with Kotlin
2.4.21. Passing `--no-configuration-cache` aborts Gradle with `Configuration Cache cannot be disabled when Isolated
Projects is enabled.`; without it the compile runs and the database creation fails with the error above.

This has been run on the repository's own build only. A stand-alone project that separates the two conditions
(Kotlin 2.4.20 with Isolated Projects, and Kotlin 2.4.21 without it) and a run on Linux x86-64, the platform of the CI
runner, are still to do before an issue is published (plan 02-22).

## Root cause

Not established. Upstream history shows that each Kotlin release lags in CodeQL: support for Kotlin 2.4.20 landed in
CodeQL 2.27.1 about a month after the release (`github/codeql#22381`), and the maintainers call a structural fix a
substantial amount of work (`github/codeql#21938`). No upstream issue for Kotlin 2.4.21 exists at the time of writing
(searched on 2026-11-23), and none describes the Isolated Projects interaction.

## Workaround

CodeQL analyses `actions` only, until Kotlin extraction works for this build.

- `.github/workflows/codeql.yml`: the marker above the matrix entry that lists the `actions` language and the marker
  above the `languages` setting of the Initialize step, both carrying `UPSTREAM: UP-0010`.

## Removal criteria

The re-enable criteria of `docs/adr/0002-codeql-kotlin-analysis.md`, made observable:

- a CodeQL bundle whose Kotlin extractor range includes the Kotlin version of the build (the supported-languages page
  of CodeQL and the release notes of the bundle say so), or CodeQL documents a recipe for Gradle 9 with Isolated
  Projects and Kotlin 2.4; and
- a throwaway `workflow_dispatch` run of a `java-kotlin` matrix leg on Linux x86-64 whose extraction summary counts
  Kotlin files greater than zero.

## How to verify removal

Add a `java-kotlin` leg with `build-mode: manual` (a `setup-java` step and the Gradle compile command of the spike,
keeping the configuration cache on with `--rerun-tasks`) to a throwaway branch, run the workflow with
`workflow_dispatch`, and read the extraction summary of the `Analyze` step: the check passes when it lists Kotlin files
greater than zero and the step ends without `could not process any of it`. Then move the leg into the matrix, drop both
markers and set the record to `workaround-removed`.

## Upstream issue draft

````markdown
# Java/Kotlin extraction finds no sources for a Kotlin 2.4.21 build under Gradle Isolated Projects

## Environment and versions

- Product and version: CodeQL bundle 2.27.5 (the bundle of `github/codeql-action` 4.39.4)
- Related products and versions: Kotlin 2.4.21, Gradle 9.9.2 with Isolated Projects (`org.gradle.isolated-projects=true`)
- JDK, operating system and build tool: Temurin 25, macOS on Apple Silicon (not yet run on Linux x86-64), Gradle 9.9.2

## Steps to reproduce

1. Create a Gradle build with the Kotlin JVM plugin 2.4.21 and one Kotlin class, and `org.gradle.isolated-projects=true` in
   `gradle.properties`.
2. Run `codeql database create db --language=java-kotlin --source-root . --command "./gradlew --no-daemon --rerun-tasks compileKotlin -Pkotlin.compiler.execution.strategy=in-process"`.

## Expected behavior

The extractor observes the Kotlin compile and the database contains the Kotlin file.

## Actual behavior

Gradle reports `BUILD SUCCESSFUL` and the compile task runs, then database creation fails with:

```text
ERROR: CodeQL detected code written in Java/Kotlin but could not process any of it.
```

Adding `--no-configuration-cache` to force a fresh compile fails earlier with
`Configuration Cache cannot be disabled when Isolated Projects is enabled.`

## Minimal reproduction

The two-step project above. To be added before publication: a repository that varies the Kotlin version (2.4.20 and
2.4.21) and Isolated Projects (on and off) independently, so the report can say which one prevents the extraction.

## Analysis

Two causes are possible and not yet separated: Kotlin 2.4.21 is outside the documented extractor range (1.8.0 to
2.4.20), and the Isolated Projects requirement for the configuration cache keeps the compile from being observed.

## Proposed fix

Document how to extract a Gradle 9 build that uses Isolated Projects, and extend the extractor range to Kotlin patch
releases of a supported minor version.
````
