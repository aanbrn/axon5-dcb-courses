// SPDX-License-Identifier: MIT
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
}

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
