// SPDX-License-Identifier: MIT
@Suppress("UnstableApiUsage")
// Spotless takes every comment above the first statement for the licence header and rewrites it, so this explanation
// sits below that statement's annotation.
//
// CI only. The workflow installs this script into the runner's Gradle user home, `init.d/`, after setup-gradle in
// every Gradle job; nothing else applies it, so local builds and Renovate read exactly the repositories the settings
// files declare. See docs/adr/0016-ci-dependency-downloads.md.
//
// It puts Google's Maven Central mirror first in the plugin repositories and the dependency repositories of every build
// (the root build and the included build-logic build), so a Maven Central rate limit no longer reaches a job as long as
// the mirror serves the file. Gradle disables a repository after an HTTP 429 or 5xx and does not move on to the next
// one, so a mirror placed after `mavenCentral()` would not help; a 404 does move on, so a file the mirror lacks (a
// Plugin Portal marker, for example) is still found in the repositories behind it.
//
// It runs in `beforeSettings`, so the mirror exists before the settings script's own blocks run and before any settings
// plugin is resolved. Repositories the settings scripts declare are appended after it, so their order is kept. The
// Gradle Plugin Portal is added back behind the mirror because it is Gradle's implicit default and disappears as soon
// as the plugin repository list is not empty.
//
// It never changes or drops a declared repository, reads no environment variable or system property, and touches no
// project, so it is compatible with isolated projects and the configuration cache.
//
// Trust: the mirror is an additional, unverified download source until dependency verification arrives with Phase 6
// (CICD-05). Integrity rests on HTTPS and on the mirror's operator, the same basis as Maven Central today; the .sha1
// files Gradle checks come from the same host, so they catch corruption, not tampering.
gradle.beforeSettings {
    // Gradle still marks the repositories API @Incubating (since 6.8), yet it is the documented way to declare
    // repositories centrally; a Gradle upgrade that changes it fails this build in its Renovate pull request.
    fun RepositoryHandler.addMirror() {
        maven {
            name = "MavenCentralMirror"
            setUrl("https://maven-central.storage-download.googleapis.com/maven2/")
            mavenContent { releasesOnly() }
        }
    }

    pluginManagement.repositories {
        addMirror()
        gradlePluginPortal()
    }
    dependencyResolutionManagement.repositories { addMirror() }
}
