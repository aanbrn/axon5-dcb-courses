// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.VerifyNodeVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
    id("com.diffplug.spotless")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

// Directories that are generated, vendored or owned by another tool. `.planning` and `.claude` are
// owned by GSD, which would otherwise fail `spotlessCheck` on every generated document.
val excluded =
    listOf(
        "**/build/**",
        "**/.gradle/**",
        "**/.kotlin/**",
        "**/node_modules/**",
        "**/.git/**",
        "**/.idea/**",
        "**/.planning/**",
        "**/.claude/**",
        "**/.mise/**",
        "gradle/wrapper/**",
        "LICENSES/**",
    )

/**
 * Selects the repository files matching [globs] outside the [excluded] directories.
 *
 * A FileTree is used because a string glob starting with `**` makes Spotless touch `Project.layout`
 * on subprojects, which isolated projects forbid.
 */
fun sources(vararg globs: String): FileTree =
    layout.projectDirectory.asFileTree.matching {
        include(*globs)
        exclude(excluded)
    }

// Spotless runs Prettier with the `node` on PATH. The environment provides the version named in
// `.nvmrc` (mise locally, setup-node in CI) and this task fails fast when the active major differs.
val verifyNodeVersion =
    tasks.register<VerifyNodeVersion>("verifyNodeVersion") {
        nvmrc.set(layout.projectDirectory.file(".nvmrc"))
        output.set(layout.buildDirectory.file("verifyNodeVersion/node-major.txt"))
        // The result depends on the environment, not on declared inputs, so it is checked on every
        // build.
        outputs.upToDateWhen { false }
    }

// The one tracked file under `.claude` that is project-owned; the rest of `.claude` and all of
// `.planning` belong to GSD and stay outside the formatting contract.
val claudeSettings: FileTree =
    layout.projectDirectory.asFileTree.matching { include(".claude/settings.json") }

spotless {
    kotlin {
        target(sources("**/*.kt"))
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
        licenseHeaderFile("config/spotless/license-header.txt")
    }
    kotlinGradle {
        target(sources("**/*.kts"))
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
        // The delimiter matches the first line that is neither a comment nor blank, so the header
        // lands above the first statement.
        licenseHeaderFile("config/spotless/license-header.txt", "[^/\\s]")
    }
    format("prettier") {
        target(
            sources("**/*.json", "**/*.json5", "**/*.yml", "**/*.yaml", "**/*.md"),
            claudeSettings,
        )
        prettier(mapOf("prettier" to libs.versions.prettier.get())).configFile(".prettierrc.yaml")
    }
}

// The verified Node major is an input of every Prettier task, so a cached result never survives a
// Node switch.
tasks
    .matching { it.name.startsWith("spotlessPrettier") }
    .configureEach { inputs.files(verifyNodeVersion.flatMap { it.output }) }

// build-logic is a separate included build: root `check` reaches its detekt, Dokka and TestKit
// checks through it.
tasks.named("check") { dependsOn(gradle.includedBuild("build-logic").task(":check")) }

// `.java-version` is the only place the JDK version lives (INFRA-09), so regenerating the daemon
// JVM criteria needs no command-line argument.
tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion.set(
        providers.fileContents(layout.projectDirectory.file(".java-version")).asText.map { text ->
            val major =
                text.trim().substringBefore('.').toIntOrNull()
                    ?: throw GradleException(
                        ".java-version must hold a numeric JDK major version, " +
                            "but it holds '${text.trim()}'",
                    )
            JavaLanguageVersion.of(major)
        },
    )
}
