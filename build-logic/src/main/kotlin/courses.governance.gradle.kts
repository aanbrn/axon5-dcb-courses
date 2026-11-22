// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.governance.AskRulesCheck
import io.github.aanbrn.courses.buildlogic.governance.BuildScriptLintCheck
import io.github.aanbrn.courses.buildlogic.governance.ConfigPolicyCheck
import io.github.aanbrn.courses.buildlogic.governance.DaemonJvmConsistencyCheck
import io.github.aanbrn.courses.buildlogic.governance.PrTemplateCheck
import io.github.aanbrn.courses.buildlogic.governance.ToolingTableCheck

// Repository governance checks (D-13): cacheable build-logic tasks wired into `check`, so a local
// build and CI enforce the same rules. The plugin is applied to the root project only.

plugins { base }

val reports = layout.buildDirectory.dir("reports/governance")

val checkGsdConfigPolicy =
    tasks.register<ConfigPolicyCheck>("checkGsdConfigPolicy") {
        group = "verification"
        description = "Fails when .planning/config.json deviates from governance/gsd-config-policy.json."
        config.set(layout.projectDirectory.file(".planning/config.json"))
        policy.set(layout.projectDirectory.file("governance/gsd-config-policy.json"))
        report.set(reports.map { it.file("gsd-config-policy.txt") })
    }

val checkClaudeAskRules =
    tasks.register<AskRulesCheck>("checkClaudeAskRules") {
        group = "verification"
        description = "Fails when the Claude Code ask rules do not cover every governed path and config-set."
        claudeSettings.set(layout.projectDirectory.file(".claude/settings.json"))
        governedPaths.set(layout.projectDirectory.file("governance/governed-paths.txt"))
        report.set(reports.map { it.file("claude-ask-rules.txt") })
    }

// Directories that are generated, vendored or owned by another tool are never scanned.
val checkBuildScripts =
    tasks.register<BuildScriptLintCheck>("checkBuildScripts") {
        group = "verification"
        description = "Fails on version literals and on eager or cross-project APIs in build scripts."
        sources.from(
            layout.projectDirectory.asFileTree.matching {
                include(
                    "**/*.gradle.kts",
                    "gradle.properties",
                    "build-logic/src/main/kotlin/**/*.kt",
                )
                exclude(
                    "**/build/**",
                    "**/.gradle/**",
                    "**/.kotlin/**",
                    "**/node_modules/**",
                    "**/.git/**",
                    "**/.idea/**",
                    "**/.mise/**",
                    "**/.planning/**",
                    "**/.claude/**",
                    "build-logic/src/functionalTest/**",
                )
            },
        )
        report.set(reports.map { it.file("build-scripts.txt") })
    }

val checkDaemonJvm =
    tasks.register<DaemonJvmConsistencyCheck>("checkDaemonJvm") {
        group = "verification"
        description = "Fails when .java-version and gradle-daemon-jvm.properties name different JDKs."
        javaVersionFile.from(layout.projectDirectory.file(".java-version").asFile)
        daemonJvmProperties.from(
            layout.projectDirectory.file("gradle/gradle-daemon-jvm.properties").asFile,
        )
        report.set(reports.map { it.file("daemon-jvm.txt") })
    }

val checkPrTemplate =
    tasks.register<PrTemplateCheck>("checkPrTemplate") {
        group = "verification"
        description = "Fails when the pull request template drifts from the /gsd-ship body sections."
        config.set(layout.projectDirectory.file(".planning/config.json"))
        template.set(layout.projectDirectory.file(".github/PULL_REQUEST_TEMPLATE.md"))
        report.set(reports.map { it.file("pr-template.txt") })
    }

val checkToolingInventory =
    tasks.register<ToolingTableCheck>("checkToolingInventory") {
        group = "verification"
        description = "Fails when mise.toml and docs/tooling.md list different mise tools."
        miseToml.set(layout.projectDirectory.file("mise.toml"))
        inventory.set(layout.projectDirectory.file("docs/tooling.md"))
        report.set(reports.map { it.file("tooling-inventory.txt") })
    }

tasks.named("check") {
    dependsOn(
        checkGsdConfigPolicy,
        checkClaudeAskRules,
        checkBuildScripts,
        checkDaemonJvm,
        checkPrTemplate,
        checkToolingInventory,
    )
}
