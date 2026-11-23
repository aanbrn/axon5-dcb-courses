// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.ComposeVersions
import io.github.aanbrn.courses.buildlogic.governance.AdrCheck
import io.github.aanbrn.courses.buildlogic.governance.AskRulesCheck
import io.github.aanbrn.courses.buildlogic.governance.BuildScriptLintCheck
import io.github.aanbrn.courses.buildlogic.governance.ConfigPolicyCheck
import io.github.aanbrn.courses.buildlogic.governance.DaemonJvmConsistencyCheck
import io.github.aanbrn.courses.buildlogic.governance.GeneratedFileCheck
import io.github.aanbrn.courses.buildlogic.governance.ImageReferenceCheck
import io.github.aanbrn.courses.buildlogic.governance.NoticeCheck
import io.github.aanbrn.courses.buildlogic.governance.PrTemplateCheck
import io.github.aanbrn.courses.buildlogic.governance.ToolingTableCheck
import io.github.aanbrn.courses.buildlogic.governance.UpstreamRecordCheck
import org.gradle.api.artifacts.VersionCatalogsExtension

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

// Directories that are generated, vendored or owned by another tool are never scanned. The functional tests of the
// checks hold fixture text that would trip the checks themselves.
val scanExcludes =
    listOf(
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
                exclude(scanExcludes)
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

// The expected text is the rendering of every compose image of the catalog through `ComposeVersions.images`, the same
// function `generateComposeVersions` (courses.root) uses. The catalog is read at configuration time as a plain value:
// its
// contents are settings-level constants and a changed catalog already invalidates the configuration cache.
val composeImages = ComposeVersions.images(the<VersionCatalogsExtension>().named("libs"))
val checkComposeVersions =
    tasks.register<GeneratedFileCheck>("checkComposeVersions") {
        group = "verification"
        description = "Fails when compose/versions.env differs from the image references in the version catalog."
        expected.set(ComposeVersions.render(composeImages))
        actual.set(layout.projectDirectory.file("compose/versions.env"))
        fixCommand.set("./gradlew generateComposeVersions")
        report.set(reports.map { it.file("compose-versions.txt") })
    }

val checkImageReferences =
    tasks.register<ImageReferenceCheck>("checkImageReferences") {
        group = "verification"
        description = "Fails on a compose image that is not a variable and on a catalog image without tag and digest."
        composeFiles.from(
            layout.projectDirectory.asFileTree.matching {
                include("compose.yaml", "compose.yml", "compose/**/*.yaml", "compose/**/*.yml")
            },
        )
        catalog.set(layout.projectDirectory.file("gradle/libs.versions.toml"))
        report.set(reports.map { it.file("image-references.txt") })
    }

// Every exception of the licence policy and the Axoniq terms URL must appear in NOTICE (CICD-09, D-31). The per-module
// check `checkDependencyLicences` (courses.licences) reads the same policy.
val checkNotice =
    tasks.register<NoticeCheck>("checkNotice") {
        group = "verification"
        description = "Fails when NOTICE does not name every exception of governance/dependency-licences.json."
        policy.set(layout.projectDirectory.file("governance/dependency-licences.json"))
        notice.set(layout.projectDirectory.file("NOTICE"))
        report.set(reports.map { it.file("notice.txt") })
    }

// Every ADR has the MADR front matter, a number that continues the sequence without gaps, and the required sections
// (DOC-03, D-38). The template `docs/templates/adr.md` is outside the input on purpose.
val checkAdrs =
    tasks.register<AdrCheck>("checkAdrs") {
        group = "verification"
        description = "Fails when an ADR breaks the MADR front matter, numbering or structure rules."
        adrs.from(layout.projectDirectory.asFileTree.matching { include("docs/adr/*.md") })
        report.set(reports.map { it.file("adrs.txt") })
    }

// Every workaround carries an `UPSTREAM` marker that names a record in docs/upstream/, and a record whose workaround is
// gone leaves no marker behind (UPS-01, UPS-02). The records and the templates are the only places that may show the
// marker syntax as an example.
val checkUpstreamRecords =
    tasks.register<UpstreamRecordCheck>("checkUpstreamRecords") {
        group = "verification"
        description = "Fails when upstream records and UPSTREAM markers disagree."
        records.from(layout.projectDirectory.asFileTree.matching { include("docs/upstream/*.md") })
        sources.from(
            layout.projectDirectory.asFileTree.matching {
                include(
                    "**/*.kt",
                    "**/*.kts",
                    "**/*.sh",
                    "**/*.yaml",
                    "**/*.yml",
                    "**/*.toml",
                    "**/*.json",
                    "**/*.json5",
                    "**/*.md",
                    "**/*.properties",
                    "**/*.xml",
                    ".env.example",
                    "NOTICE",
                )
                exclude(scanExcludes)
                exclude("docs/upstream/**", "docs/templates/**")
            },
        )
        index.set(reports.map { it.file("upstream-index.md") })
        report.set(reports.map { it.file("upstream-records.txt") })
    }

tasks.named("check") {
    dependsOn(
        checkGsdConfigPolicy,
        checkClaudeAskRules,
        checkBuildScripts,
        checkDaemonJvm,
        checkPrTemplate,
        checkToolingInventory,
        checkComposeVersions,
        checkImageReferences,
        checkNotice,
        checkUpstreamRecords,
        checkAdrs,
    )
}
