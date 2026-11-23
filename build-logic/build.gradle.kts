// SPDX-License-Identifier: MIT
plugins {
    `kotlin-dsl`
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.foojay.resolver.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.cyclonedx.gradle.plugin)
    // UPSTREAM: UP-0008 Gradle exposes no typed `libs` accessor to precompiled script plugins (gradle/gradle#15383).
    // Required for the typed `libs` accessor inside precompiled script plugins (still needed on
    // Gradle 9.9.2).
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

// build-logic is analysed under the same rules as every module (INFRA-12): the strict detekt
// configuration is shared and Dokka fails the build on warnings.
detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(layout.projectDirectory.file("../config/detekt/detekt.yml"))
    // The default sources cover `main` and `test`; the TestKit suite is analysed as well.
    source.from("src/functionalTest/kotlin")
}

dokka {
    dokkaPublications.html { failOnWarning.set(true) }
    // The `kotlin-dsl` plugin generates adapters with unresolvable KDoc links; documenting only
    // the hand-written sources keeps `failOnWarning` meaningful.
    dokkaSourceSets.configureEach {
        sourceRoots.setFrom(layout.projectDirectory.dir("src/main/kotlin"))
    }
}

tasks.named("check") { dependsOn(tasks.named("dokkaGeneratePublicationHtml")) }

// TestKit suite proving the conventions in isolation (INFRA-12). Fixtures include this same
// build-logic directory, so they must not run concurrently with one another. Gradle still marks the
// JVM Test Suite API @Incubating; it is the documented way to declare test levels (TEST-11), and a
// Gradle upgrade that changes it fails this build in its Renovate pull request.
@Suppress("UnstableApiUsage")
testing {
    suites {
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(gradleTestKit())
                implementation(platform(libs.kotest.bom))
                implementation(libs.kotest.assertions.core)
            }
            targets.configureEach {
                testTask.configure {
                    shouldRunAfter("test")
                    maxParallelForks = 1
                    // Declared as inputs so that the task stays cacheable. Files that later plans
                    // create are simply absent until then.
                    inputs
                        .files(
                            "../gradle/libs.versions.toml",
                            "../platform/build.gradle.kts",
                            "../governance/dependency-licences.json",
                            "../NOTICE",
                            "../.prettierrc.yaml",
                            "../.taplo.toml",
                            "../.editorconfig",
                            "../.nvmrc",
                        )
                        .withPropertyName("repoFiles")
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                    inputs.dir("../config").withPropertyName("repoConfig").withPathSensitivity(PathSensitivity.RELATIVE)
                    inputs
                        .dir("src/main/kotlin")
                        .withPropertyName("conventionSources")
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                    systemProperty("buildLogic", layout.projectDirectory.asFile.absolutePath)
                    systemProperty(
                        "repoRoot",
                        layout.projectDirectory.dir("..").asFile.absolutePath,
                    )
                    systemProperty(
                        "catalog",
                        layout.projectDirectory.file("../gradle/libs.versions.toml").asFile.absolutePath,
                    )
                }
            }
        }
    }
}

gradlePlugin { testSourceSets(sourceSets["functionalTest"]) }

tasks.named("check") { dependsOn("functionalTest") }
