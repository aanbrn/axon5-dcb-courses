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
    // Required for the typed `libs` accessor inside precompiled script plugins (still needed on
    // Gradle 9.9.2).
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

// build-logic is analysed under the same rules as every module (INFRA-12): the strict detekt
// configuration is shared and Dokka fails the build on warnings.
detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(layout.projectDirectory.file("../config/detekt/detekt.yml"))
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
