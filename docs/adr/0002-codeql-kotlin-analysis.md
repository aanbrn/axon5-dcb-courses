# ADR-0002: CodeQL analyses GitHub Actions workflows only, Kotlin is deferred

- Status: accepted
- Date: 2026-11-21
- Deciders: repository author (decision D-24)

This is a minimal record. Phase 2 introduces the ADR templates and normalises the format of this note.

## Context

D-24 asks for CodeQL on the repository, with the `java-kotlin` language included only if a time-boxed local
spike proves that the extractor sees the Kotlin sources of this Gradle build. CodeQL does not read Kotlin
without observing a compile: `build-mode: none` does not cover Kotlin, so the language needs
`build-mode: manual` and a Gradle command that really compiles the code.

The build uses Gradle 9.9.2 with Isolated Projects (which forces the configuration cache on), a Gradle
daemon JVM pinned by criteria, Kotlin 2.4.21 and JDK 25. The CodeQL bundle that matches the pinned
`github/codeql-action` release v4.39.4 is `codeql-bundle-v2.27.5`; its Kotlin extractors cover 1.8.0 to 2.4.20.

## Spike

Time box: two extraction attempts. Platform: macOS on Apple Silicon with the published `osx64` bundle
(SHA-256 `c671dd6956a604a189aa43827c54d2cff974b7dfcb10290a7ced4d3e3f52fade`, matching the release checksum),
unpacked into a scratch directory outside the repository and deleted afterwards. Base command:

```sh
codeql database create <scratch>/db --language=java-kotlin --source-root . \
  --command "./gradlew --no-daemon --no-build-cache --rerun-tasks :tooling:convention-sample:compileKotlin <flags>"
```

| Attempt | Extra flags                                                                      | Result                                                                                                                                                                      |
| ------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1       | `--no-configuration-cache -Pkotlin.compiler.execution.strategy=in-process`       | Gradle aborted: `Configuration Cache cannot be disabled when Isolated Projects is enabled.`                                                                                 |
| 2       | `-Pkotlin.compiler.execution.strategy=in-process` (configuration cache stays on) | Gradle ran the compile (`BUILD SUCCESSFUL`, 11 tasks executed, `:tooling:convention-sample:compileKotlin` among them), but the database was finalised with the error below. |

Attempt 2 extraction summary: `ERROR: CodeQL detected code written in Java/Kotlin but could not process any of
it.` The Kotlin extractor therefore saw no sources, and the database was empty for Java and Kotlin.

## Decision

Ship the `actions` language only. `codeql.yml` analyses the workflows on every push to `main`, on every pull
request and weekly. The `java-kotlin` language is not in the matrix, because the spike did not extract a
single Kotlin file and a CodeQL leg that always fails or analyses nothing is noise, not assurance.

The negative result is bounded: the spike ran on a macOS Apple Silicon machine, not on the Linux x86-64
runner CI uses, and the repository contains a single Kotlin module (the convention sample), so a stronger
conclusion about the Gradle setup in general is not justified.

## Re-enable criteria

Retry the spike, and add `java-kotlin` with `build-mode: manual` plus a `setup-java` step and the
spike's Gradle compile command to the matrix, when either of these holds:

- Phase 2 lands the first real Kotlin module (more sources, and a compile that does not depend on the
  convention sample), or
- CodeQL documents a recipe for Gradle 9 with Isolated Projects and Kotlin 2.4, or a newer bundle lists
  support for the Kotlin version in use.

The retry must run on Linux x86-64 (a throwaway `workflow_dispatch` run of the matrix leg is enough) and must
check that the extraction summary counts Kotlin files greater than zero before the language joins the matrix.
Gradle flags that defeat caching must keep the configuration cache on (`--rerun-tasks` instead of
`--no-configuration-cache`), because Isolated Projects rejects the latter.

## Consequences

- Kotlin sources have no CodeQL analysis until the criteria above hold. Static analysis of Kotlin
  stays with the compiler (all warnings are errors) and detekt.
- CodeQL results are not a required status check; only `ci-success` and `governance-success` are
  (ADR-0001).
