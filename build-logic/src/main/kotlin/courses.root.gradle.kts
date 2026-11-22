// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.ExecutableOnPath
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
 * A FileTree is used because a string glob starting with `**` makes Spotless touch `Project.layout` on subprojects,
 * which isolated projects forbid.
 */
fun sources(vararg globs: String): FileTree =
    layout.projectDirectory.asFileTree.matching {
        include(*globs)
        exclude(excluded)
    }

/** The absolute path of [name] on the build's `PATH`, absent when the executable is not installed. */
fun executableOnPath(name: String): Provider<String> =
    providers.of(ExecutableOnPath::class.java) { parameters.executableName.set(name) }

// `node` is resolved once from the build's `PATH` through `ExecutableOnPath`. The same path goes
// to `verifyNodeVersion` and to Spotless's Prettier step, so the guard checks the binary that
// formats. The environment provides the version named in `.nvmrc` (mise locally, setup-node in CI).
val nodePath = executableOnPath("node")
val verifyNodeVersion =
    tasks.register<VerifyNodeVersion>("verifyNodeVersion") {
        nvmrc.set(layout.projectDirectory.file(".nvmrc"))
        executable.set(nodePath)
        output.set(layout.buildDirectory.file("verifyNodeVersion/node-major.txt"))
        // The result depends on the environment, not on declared inputs, so it is checked on every
        // build.
        outputs.upToDateWhen { false }
    }

// Spotless runs the formatter binaries pinned in `mise.toml`. They are resolved from the build's
// `PATH` through `ExecutableOnPath`, like `node`. A missing binary fails only the formatting tasks
// of that file type, with a `mise install` hint, never the configuration, so a Gradle run that does
// not format (such as the CodeQL compile) works without mise. An empty string means not found.
val taploConfig = layout.projectDirectory.file(".taplo.toml")
val taploPath = executableOnPath("taplo").orElse("")
val shfmtPath = executableOnPath("shfmt").orElse("")

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
val claudeSettings: FileTree = layout.projectDirectory.asFileTree.matching { include(".claude/settings.json") }

// The one line-width limit of every formatter. The same value is `max_line_length` in
// `.editorconfig`, `printWidth` in `.prettierrc.yaml` and `column_width` in `.taplo.toml`. detekt
// `MaxLineLength` (140) is only a backstop for lines that no formatter can break.
val maxLineWidth = 120

spotless {
    kotlin {
        target(sources("**/*.kt"))
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle().configure { it.setMaxWidth(maxLineWidth) }
        licenseHeaderFile("config/spotless/license-header.txt")
    }
    kotlinGradle {
        target(sources("**/*.kts"))
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle().configure { it.setMaxWidth(maxLineWidth) }
        // The delimiter matches the first line that is neither a comment nor blank, so the header
        // lands above the first statement.
        licenseHeaderFile("config/spotless/license-header.txt", "[^/\\s]")
    }
    format("prettier") {
        target(
            sources("**/*.json", "**/*.json5", "**/*.yml", "**/*.yaml", "**/*.md"),
            claudeSettings,
        )
        val prettier = prettier(mapOf("prettier" to libs.versions.prettier.get())).configFile(".prettierrc.yaml")
        // Only the node is set: Spotless takes npm from the same directory when it is there, and
        // starts it with that directory first on `PATH`, so npm and Prettier's server run this
        // node.
        nodePath.orNull?.let { prettier.nodeExecutable(it) }
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
        // The fallback name is never run: `verifyShfmt` fails first, and the value source is
        // re-checked on every build.
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
        inputs.file(taploConfig).withPropertyName("taploConfig").withPathSensitivity(PathSensitivity.RELATIVE)
    }

tasks
    .matching { it.name.startsWith("spotlessShell") }
    .configureEach {
        inputs.files(verifyShfmt.flatMap { it.output })
        inputs.file(editorConfig).withPropertyName("editorConfig").withPathSensitivity(PathSensitivity.RELATIVE)
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
                        ".java-version must hold a numeric JDK major version, " + "but it holds '${text.trim()}'",
                    )
            JavaLanguageVersion.of(major)
        },
    )
}
