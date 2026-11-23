---
id: "UP-0008"
product: "gradle"
product-version: "9.9.2"
status: "reported"
workaround: true
upstream: ["https://github.com/gradle/gradle/issues/15383"]
reported: "2020-12-01"
updated: "2026-11-23"
---

# UP-0008: The typed `libs` accessors are not visible in precompiled script plugins

## Symptom

A precompiled script plugin under `build-logic/src/main/kotlin` cannot use the type-safe version catalog accessors of
the main build. Without the workaround, `val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()` fails to compile
with `Unresolved reference 'accessors'`, and every later `libs.versions...` use with `Unresolved reference 'versions'`.
The build script of `build-logic` itself does see its `libs` accessor, because `build-logic/settings.gradle.kts`
imports the same catalog file.

Four convention plugins of this repository read versions and libraries from the catalog this way. Gradle 9.9.2 still
has the gap (verified on 2026-11-23 by removing the workaround line: `:build-logic:compileKotlin` failed).

## Minimal reproduction

A two-file build is enough:

```text
<repository>/
  gradle/libs.versions.toml          # [versions] junit = "6.0.3"
  build-logic/settings.gradle.kts    # versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
  build-logic/build.gradle.kts       # plugins { `kotlin-dsl` }
  build-logic/src/main/kotlin/demo.gradle.kts
```

with `demo.gradle.kts` containing `println(libs.versions.junit.get())` and `build-logic` included through
`pluginManagement { includeBuild("build-logic") }`. Without the workaround, `demo.gradle.kts` fails to compile with an
unresolved reference to `libs` (or, when it uses `the<org.gradle.accessors.dm.LibrariesForLibs>()`, to `accessors`).
This is the layout of the examples in the upstream issue; this repository itself reproduces the failure as described
under Symptom.

## Root cause

Gradle generates the accessors per build, and a precompiled script plugin may be applied to builds other than the one
that declares it (it can be published, or sit in an included build), so Gradle cannot know which catalog the script
should see. The long-standing issue `gradle/gradle#15383` describes the design question and has no committed
solution; the discussion collects the workaround below.

## Workaround

The generated accessor object is a Gradle-decorated subclass of the generated `LibrariesForLibs` class. Its superclass
lives in the jar that holds the generated accessors, and putting that jar on the classpath of the build logic makes
`the<LibrariesForLibs>()` resolvable in precompiled scripts.

- `build-logic/build.gradle.kts`: `implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))`,
  carrying `UPSTREAM: UP-0008`.

## Removal criteria

A Gradle release in which the version catalog accessors are available in precompiled script plugins without the
workaround, closed through `gradle/gradle#15383` or a linked change, and named in the Gradle release notes. A Renovate
pull request that bumps `gradle` shows this record in its advisory.

## How to verify removal

Delete the `implementation(files(...))` line in `build-logic/build.gradle.kts` and run
`mise x -- ./gradlew :build-logic:compileKotlin`. The removal is safe when the build succeeds and no
`Unresolved reference` for `libs`, `accessors` or `versions` appears in any convention plugin.

## Upstream issue draft

Not needed: `https://github.com/gradle/gradle/issues/15383` (opened 2020-12-01, open, labels `in:kotlin-dsl` and
`in:precompiled-script-plugin`) is this defect. Add a reaction or a comment only after the author approves it (plan
02-22).
