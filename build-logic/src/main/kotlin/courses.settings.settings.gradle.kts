// SPDX-License-Identifier: MIT
plugins { id("org.gradle.toolchains.foojay-resolver-convention") }

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Gradle still marks the repositories API @Incubating (since 6.8), yet it is the documented way to declare
    // repositories centrally; a Gradle upgrade that changes it fails this build in its Renovate pull request.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}
