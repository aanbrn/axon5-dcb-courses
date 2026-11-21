---
phase: 01-repository-build-ci-foundation
plan: 01
subsystem: infra
tags: [gradle, kotlin-dsl, build-logic, version-catalog, java-platform, configuration-cache, isolated-projects]

requires: []
provides:
  - Checksum-pinned Gradle 9.9.2 wrapper
  - build-logic included build with the courses.settings and courses.kotlin-library convention plugins
  - ":platform java-platform importing eight BOMs (Spring Boot, Axon Framework, Axoniq Framework, Testcontainers, Jackson, Kotlin, JUnit, Kotest) through catalog aliases"
  - Single version catalog (gradle/libs.versions.toml) and .java-version as the only JDK source
  - ":tooling:convention-sample convention fixture (compile, test through the conventions)"
  - Comprehensive .gitignore (Gradle, Kotlin, Node, IDE, env, test output, profiler, OS)
affects: [01-02, 01-03, 01-04, 01-05, 01-07, 01-08, 01-12, phase-02]

plan_head_before: 8ba88f34381bd608ec57816739ae78dc346da933
plan_head_after: 4c13be0d7453beea9d65ed76d78a9f679c4f386a

actuals:
  tokens: 5600
  tasks: 2
  commits: 2

tech-stack:
  added: [Gradle 9.9.2, Kotlin 2.4.21, JUnit 6.0.3, Kotest assertions 6.2.7, foojay-resolver 1.0.0]
  patterns:
    - "Settings plugin (courses.settings) in an included build keeps plugin versions out of settings.gradle.kts"
    - "Module convention plugin adds platform(project(':platform')) to every implementation and *Implementation configuration lazily"
    - "JDK major read from .java-version through providers.fileContents and isolated.rootProject (isolated-projects safe)"
    - "Typed libs accessor in precompiled script plugins via the LibrariesForLibs classpath entry"

key-files:
  created:
    - gradle/wrapper/gradle-wrapper.properties
    - gradle/wrapper/gradle-wrapper.jar
    - gradlew
    - gradlew.bat
    - gradle.properties
    - settings.gradle.kts
    - gradle/libs.versions.toml
    - .java-version
    - build-logic/settings.gradle.kts
    - build-logic/build.gradle.kts
    - build-logic/src/main/kotlin/courses.settings.settings.gradle.kts
    - build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
    - platform/build.gradle.kts
    - tooling/convention-sample/build.gradle.kts
    - tooling/convention-sample/src/main/kotlin/io/github/aanbrn/courses/sample/Greeting.kt
    - tooling/convention-sample/src/test/kotlin/io/github/aanbrn/courses/sample/GreetingTest.kt
  modified:
    - .gitignore

key-decisions:
  - "Kotlin override is the only api constraint in :platform in Phase 1: api(libs.kotlin.stdlib) with the catalog Kotlin 2.4.21; Gradle conflict resolution picks it over Spring Boot's managed 2.3.21"
  - "Every later non-BOM library or deliberate pin gets its own api constraint in :platform that references a catalog alias (INFRA-11 scope record)"
  - "Jackson BOM pinned to 3.1.8, re-read from spring-boot-dependencies 4.1.3 on Maven Central (jackson-bom.version)"
  - "Gradle dependency locking and verification-metadata.xml deferred to Phase 6 (CICD-05)"

patterns-established:
  - "Every .kt and .kts file starts with // SPDX-License-Identifier: MIT followed directly by a statement, laid out for ktfmt kotlinlang style"
  - "Only lazy Gradle APIs and no cross-project access in convention plugins (isolated projects)"

requirements-completed: [INFRA-01, INFRA-09, INFRA-10, INFRA-11, INFRA-14]

coverage:
  - id: D1
    description: "./gradlew build compiles and tests the sample module through build-logic, :platform and the catalog; second run reuses the configuration cache with isolated projects on"
    requirement: INFRA-01
    verification:
      - kind: integration
        ref: "./gradlew :tooling:convention-sample:build --console=plain (twice; second run prints 'Reusing configuration cache.')"
        status: pass
      - kind: unit
        ref: "tooling/convention-sample/src/test/kotlin/io/github/aanbrn/courses/sample/GreetingTest.kt#greets the given name with the configured salutation"
        status: pass
    human_judgment: false
  - id: D2
    description: "Build performance settings: configuration cache (problems fail), build cache, parallel execution, isolated projects, file-system watching, JVM args"
    requirement: INFRA-10
    verification:
      - kind: other
        ref: "grep -c '^org.gradle.isolated-projects=true$' gradle.properties (1); grep -c '^org.gradle.vfs.watch=true$' gradle.properties (1)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Single version source: catalog for libraries, plugins and tool versions; .java-version for the JDK; wrapper pinned by SHA-256"
    requirement: INFRA-09
    verification:
      - kind: other
        ref: "grep distributionSha256Sum in gradle-wrapper.properties (1); cat .java-version prints 25"
        status: pass
    human_judgment: false
  - id: D4
    description: ":platform imports eight BOMs through catalog aliases, and the catalog Kotlin overrides Spring Boot's managed Kotlin"
    requirement: INFRA-11
    verification:
      - kind: integration
        ref: "./gradlew :platform:dependencies --configuration api | grep -cE '<bom>:' (8)"
        status: pass
      - kind: integration
        ref: "./gradlew :tooling:convention-sample:dependencyInsight --configuration runtimeClasspath --dependency org.jetbrains.kotlin:kotlin-stdlib (resolves 2.4.21, by constraint, over 2.3.21)"
        status: pass
    human_judgment: false
  - id: D5
    description: ".gitignore covers build state, secrets files and tool output while keeping the existing .claude negations"
    requirement: INFRA-14
    verification:
      - kind: other
        ref: "grep -c '^\\.env$' .gitignore (1); grep -c '^!/.claude/settings.json$' .gitignore (1)"
        status: pass
    human_judgment: false

duration: 7min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 01: Gradle Build Skeleton Summary

**Checksum-pinned Gradle 9.9.2 build with build-logic convention plugins, a catalog-driven `:platform` aligning eight BOMs (catalog Kotlin 2.4.21 overriding Spring Boot's managed Kotlin), and a sample module that builds and tests under configuration cache and isolated projects**

## Performance

- **Duration:** 7 min
- **Started:** 2026-11-21T08:42:00Z
- **Completed:** 2026-11-21T08:49:00Z
- **Tasks:** 2
- **Files modified:** 17 (16 created, 1 modified)

## Accomplishments

- Tracer slice proven end to end: `./gradlew :tooling:convention-sample:build` compiles and tests the sample through `courses.settings`, `courses.kotlin-library`, `:platform` and the catalog; the second run prints `Reusing configuration cache.` with `org.gradle.isolated-projects=true`, parallel execution and the build cache on.
- `:platform` is the single alignment point (INFRA-11): eight BOM imports through catalog aliases, plus the `api(libs.kotlin.stdlib)` constraint. `dependencyInsight` shows `kotlin-stdlib:2.4.21` selected "by constraint" and "by conflict resolution: between versions 2.4.21 and 2.3.21".
- The JDK major version comes only from `.java-version` (`25`), read through an isolated-projects-safe provider that throws a `GradleException` naming `.java-version` when it is empty or not numeric.
- Compiler strictness from the stack decision: `allWarningsAsErrors`, `-Xjsr305=strict`, `-Xjspecify-annotations=strict`.

## Task Commits

1. **Task 1: End-to-end build (tracer)** - `05e50f0` (feat)
2. **Task 2: Stack alignment in `:platform`** - `4c13be0` (feat)

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

Tracer gate: after Task 1 the `<verify>` commands were re-run end to end and passed (`Reusing configuration cache.`, `BUILD SUCCESSFUL`), logged as "Tracer verified end-to-end - expanding".

## Files Created/Modified

- `gradle/wrapper/*`, `gradlew`, `gradlew.bat` - generated wrapper; `distributionSha256Sum=971fcd71...` and `validateDistributionUrl=true` (T-01-01)
- `gradle.properties` - configuration cache (problems fail), build cache, parallel, isolated projects, VFS watching, JVM args, `group=io.github.aanbrn.courses`
- `gradle/libs.versions.toml` - the version catalog (libraries, BOMs, plugins, tool versions, Renovate-annotated Prettier)
- `settings.gradle.kts` - includes `build-logic`, applies `courses.settings`, includes `:platform` and `:tooling:convention-sample`, documents the D-03 role-based layout
- `build-logic/*` - settings, build and the two convention plugins
- `platform/build.gradle.kts` - `java-platform` with eight BOM imports and the Kotlin override
- `tooling/convention-sample/*` - `Greeting` (full KDoc) and `GreetingTest` (JUnit 6 plus Kotest `shouldBe`)
- `.gitignore` - extended; existing `/.idea/`, `/.claude/*`, `!/.claude/settings.json`, `!/.claude/CLAUDE.md` and `/.planning/state.json` lines kept

## Decisions Made

- Kotlin override stays the only `api` constraint in `:platform` for Phase 1 (no non-BOM library is used); later pins such as `protobuf-java` get their own constraint referencing a catalog alias.
- Jackson BOM 3.1.8 verified against `spring-boot-dependencies:4.1.3` (`jackson-bom.version`); Spring Boot manages Kotlin 2.3.21 there, confirming the override is real, not cosmetic.
- Accepted scope deviation (file count): 17 `files_modified` entries (4 generated wrapper files, 13 hand-written) exceed the 10-file warning threshold. Every file is one layer of the single end-to-end path, so the slice could not be split without losing the tracer.
- Recorded deferral for the author: Gradle dependency locking and `gradle/verification-metadata.xml` (from the CLAUDE.md stack table) are left to the phase that first needs exact resolved versions for scanning (Phase 6, CICD-05). `.gitattributes` in plan 01-06 already marks the verification file as generated.

## Deviations from Plan

None - plan executed exactly as written.

## Measured timings (Nyquist)

Wall-clock of each `<automated>` command, warm Gradle user home (Gradle 9.9.2 distribution and the prototype's plugin and library artifacts were already cached, so no network downloads were observed; a truly cold machine will be slower):

| Command | Time |
|---------|------|
| First `./gradlew :tooling:convention-sample:build` (build-logic compile, config cache miss) | 14.8 s |
| Task 1 verify, both builds chained plus grep | about 1.0 s |
| `:tooling:convention-sample:test --rerun` plus `ls` | about 1.1 s |
| `:platform:dependencies --configuration api` | 6.5 s (config cache miss) |
| `dependencyInsight` on `runtimeClasspath` | about 1.0 s |
| `:tooling:convention-sample:build` after Task 2 | 2.4 s |
| Full `./gradlew build`, then second run | 0.7 s, then 0.5 s (`Reusing configuration cache.`) |

All are far inside the 180 s per-task feedback budget of `01-VALIDATION.md`.

## Issues Encountered

None. The ktfmt layout could not be machine-checked in this plan because Spotless arrives in plan 01-02; files were written by hand to the kotlinlang layout (4-space indent, 100 columns, KDoc as in the RESEARCH example), and plan 01-02's first `spotlessApply` should be a no-op. If it is not, that is a formatting nit, not a build break.

## Known Stubs

None.

## Threat Flags

None. Threat register items T-01-01 (wrapper checksum plus URL validation), T-01-02 (versions only from the catalog, `FAIL_ON_PROJECT_REPOS`), T-01-08 (`.env`, `.env.*` ignored except `.env.example`) and T-01-SC (no npm/pip/cargo installs; Maven artifacts only) are mitigated as planned.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready for plan 01-02: the `courses.root` plugin, Spotless, detekt, Dokka and the TestKit suite plug into `build-logic` and the catalog, which already carries their versions and aliases (`spotless-gradle-plugin`, `detekt-gradle-plugin`, `dokka-gradle-plugin`, `ktfmt`, `prettier`, plugin aliases `detekt` and `dokka`).
- Flagged assumption carried forward (INFRA-01): "one command builds the whole backend" is read as every included project at the end of Phase 1; the root `check` wiring of `build-logic` arrives in plan 01-02.

## Self-Check: PASSED

All 16 created files exist on disk; commits `05e50f0` and `4c13be0` are ancestors of HEAD; every task acceptance criterion and plan-level verification command passed.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
