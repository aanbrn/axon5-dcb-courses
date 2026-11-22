// SPDX-License-Identifier: MIT
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Gradle still marks the repositories API @Incubating (since 6.8), yet it is the documented way to declare
    // repositories centrally; a Gradle upgrade that changes it fails this build in its Renovate pull request.
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }
}

rootProject.name = "build-logic"
