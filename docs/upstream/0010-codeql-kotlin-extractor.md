---
id: "UP-0010"
product: "action:github/codeql-action"
product-version: "4.39.4"
status: "open"
workaround: true
upstream: []
reported: null
updated: "2026-11-25"
---

# UP-0010: The CodeQL Kotlin extractor extracted no sources from the Kotlin 2.4.21 Gradle build

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

- the build uses Kotlin 2.4.21, and the Kotlin extractor range of the CodeQL bundle that matches
  `github/codeql-action` 4.39.4 (CodeQL bundle 2.27.5) was first read as 1.8.0 to 2.4.20; the check of 2026-11-25
  (Root cause) found that the bundle documents 1.8.0 to 2.4.2x, which includes 2.4.21, so this condition is probably
  not the cause;
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

Bundle check of 2026-11-25. The latest `github/codeql-action` release is v4.39.5 (published 2026-11-24), which moves the
default CodeQL bundle to 2.27.6 (the latest bundle release, published 2026-11-24); the repository pins v4.39.4, which
uses bundle 2.27.5. The Kotlin row of the CodeQL supported-languages page reads "Kotlin 1.8.0 to 2.4.2x", and the
same row in `docs/codeql/reusables/supported-versions-compilers.rst` of the `codeql-cli/v2.27.5` and `codeql-cli/v2.27.6`
tags reads `Kotlin 1.8.0 to 2.4.2\ *x*`, so both bundles document a range that includes Kotlin 2.4.21. The range was
widened from "1.8.0 to 2.4.20" by `github/codeql#23092` (merged 2026-11-13, "Kotlin: update test compiler and
documentation to 2.4.21"), whose description says that Kotlin 2.4.21 was already supported by the extractor and that
the full K2 suite of 205 tests passed with it. Bundle 2.27.5 contains that change, and the CodeQL CLI changelog for
2.27.5 and 2.27.6 lists no Kotlin extractor change. The conclusion for this record: the first removal criterion
(a bundle whose Kotlin range includes the build's Kotlin version) is met by the documentation already, and the unproven
cause is the Isolated Projects interaction, which no upstream issue or CodeQL documentation addresses. The second
removal criterion, a throwaway `workflow_dispatch` run of a `java-kotlin` leg on Linux x86-64 with bundle 2.27.6 (action
v4.39.5), is the next step and is an outward action that needs its own approval; it is not done here. The spike itself
ran on bundle 2.27.5 and was not repeated on 2.27.6.

Sources: `https://codeql.github.com/docs/codeql-overview/supported-languages-and-frameworks/`,
`https://github.com/github/codeql-action/releases/tag/v4.39.5`,
`https://github.com/github/codeql-action/releases/tag/codeql-bundle-v2.27.6`,
`https://github.com/github/codeql-cli-binaries/blob/HEAD/CHANGELOG.md`, `https://github.com/github/codeql/pull/23092`
and `https://github.com/github/codeql/pull/22404` (support for Kotlin 2.4.20).

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

The author decided on 2026-11-25 not to publish this draft for now. Its Analysis section reflects the bundle check of
the same date: the Kotlin range no longer explains the result, and the draft has to be narrowed to the Isolated Projects
interaction (and re-run on bundle 2.27.6 and Linux x86-64) before it could be published.

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

Two causes were possible and are not yet separated: the Kotlin version of the build, and the Isolated Projects
requirement for the configuration cache, which keeps the compile from being observed. The documented extractor range
(1.8.0 to 2.4.2x) includes Kotlin 2.4.21, so the Isolated Projects interaction is the likelier cause.

## Proposed fix

Document how to extract a Gradle 9 build that uses Isolated Projects, and extend the extractor range to Kotlin patch
releases of a supported minor version.
````
