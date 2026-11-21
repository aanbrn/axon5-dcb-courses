// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.VerifyFormatterBinary
import io.github.aanbrn.courses.buildlogic.VerifyNodeVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    base
    id("com.diffplug.spotless")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

// Directories that are generated, vendored or owned by another tool. `.planning` and `.claude` are
// owned by GSD, which would otherwise fail `spotlessCheck` on every generated document.
// `.gsd` holds GSD's self-ignored runtime state, such as dispatch sentinels.
// `gradle-user-home` and `profile-out*` are written into the project by gradle-profiler.
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
        "**/.gsd/**",
        "**/.mise/**",
        "**/gradle-user-home/**",
        "**/profile-out*/**",
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

/** The absolute path of [tool] on `PATH`, or an empty string when it is not installed. */
fun pathOf(tool: String): Provider<String> =
    providers
        .exec {
            commandLine("sh", "-c", "command -v $tool")
            isIgnoreExitValue = true
        }
        .standardOutput
        .asText
        .map { it.trim() }

// Spotless runs the formatter binaries pinned in `mise.toml`. A missing binary fails only the
// formatting tasks of that file type, with a `mise install` hint, never the configuration, so a
// Gradle run that does not format (such as the CodeQL compile) works without mise.
val taploConfig = layout.projectDirectory.file(".taplo.toml")
val taploPath = pathOf("taplo")
val shfmtPath = pathOf("shfmt")

/** Registers the task that checks that the formatter binary [tool], found at [path], runs. */
fun registerBinaryCheck(tool: String, path: Provider<String>) =
    tasks.register<VerifyFormatterBinary>("verify${tool.replaceFirstChar { it.uppercase() }}") {
        toolName.set(tool)
        executable.set(path)
        output.set(layout.buildDirectory.file("verify-formatter-binary/$tool.txt"))
        // The result depends on the environment, not on declared inputs, so it is checked on every
        // build.
        outputs.upToDateWhen { false }
    }

val verifyTaplo = registerBinaryCheck("taplo", taploPath)
val verifyShfmt = registerBinaryCheck("shfmt", shfmtPath)

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
    format("toml") {
        target(sources("**/*.toml"))
        // `mise.lock` is generated and its extension is `.lock`, so it is never matched. Spotless
        // hashes the executable when the configuration cache is stored, so a missing taplo is
        // replaced by an existing file that is never run: `verifyTaplo` fails first, with the hint.
        nativeCmd(
            "taplo",
            taploPath.get().ifEmpty { taploConfig.asFile.absolutePath },
            listOf("fmt", "--config", taploConfig.asFile.absolutePath, "-"),
        )
    }
    shell {
        target(sources("**/*.sh"))
        // The version is pinned once, in `mise.toml`; `*` accepts whatever that provides.
        shfmt("*").pathToExe(shfmtPath.get().ifEmpty { "shfmt" })
        // The delimiter matches the first line that is not an SPDX line, so only the SPDX line is
        // managed, and the shebang is skipped so that the header lands on line 2.
        // REUSE-IgnoreStart
        licenseHeaderFile(
                "config/spotless/license-header-shell.txt",
                "^(?!# SPDX-License-Identifier: )",
            )
            // REUSE-IgnoreEnd
            .skipLinesMatching("^#!.+?$")
    }
    format("misc") {
        target(
            sources(
                    "**/*.properties",
                    "**/*.scenarios",
                    "**/*.txt",
                    ".editorconfig",
                    ".gitattributes",
                    ".gitignore",
                    ".nvmrc",
                    ".java-version",
                )
                .matching { exclude("gradle/gradle-daemon-jvm.properties") },
        )
        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(2)
    }
}

// The binary and its version, and the configuration file the tool reads, are inputs of every TOML
// and shell task, so a cached result never survives a change to either.
val editorConfig = layout.projectDirectory.file(".editorconfig")

tasks
    .matching { it.name.startsWith("spotlessToml") }
    .configureEach {
        inputs.files(verifyTaplo.flatMap { it.output })
        inputs
            .file(taploConfig)
            .withPropertyName("taploConfig")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }

tasks
    .matching { it.name.startsWith("spotlessShell") }
    .configureEach {
        inputs.files(verifyShfmt.flatMap { it.output })
        inputs
            .file(editorConfig)
            .withPropertyName("editorConfig")
            .withPathSensitivity(PathSensitivity.RELATIVE)
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
