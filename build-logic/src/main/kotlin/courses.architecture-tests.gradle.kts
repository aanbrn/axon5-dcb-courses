// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

// The `architectureTest` suite (D-17, INFRA-02, TEST-11): Konsist rules that scan the Kotlin sources of the whole
// repository from one module. The suite is wired into `check` like every other suite.
//
// The rules read files outside the module, so those files are declared as inputs of the test task. Without the
// declaration Gradle would serve a cached result after an edit that breaks a rule (RESEARCH Pitfall 5, T-02-22). The
// patterns cover exactly what the rules scan: the sources and build scripts under `backend`, the sources under
// `tooling` and the TestKit tests of `build-logic`.

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

// The root directory through the isolated-projects view, the same way `courses.kotlin-library` reads root files. Gradle
// still marks `Project.isolated` @Incubating; a Gradle upgrade that changes it fails this build in its Renovate pull
// request.
@Suppress("UnstableApiUsage") val rootDirectory: Directory = isolated.rootProject.projectDirectory

// The JVM Test Suite API is still marked @Incubating, see `courses.test-suites`.
@Suppress("UnstableApiUsage")
testing {
    suites {
        register<JvmTestSuite>("architectureTest") {
            dependencies { implementation(libs.konsist) }
            targets.configureEach {
                testTask.configure {
                    group = "verification"
                    description = "Runs the Konsist architecture rules over the Kotlin sources of the repository."
                    mustRunAfter(tasks.named("integrationTest"))
                    inputs
                        .files(
                            fileTree(rootDirectory) {
                                include(
                                    "backend/**/src/**/*.kt",
                                    "backend/**/build.gradle.kts",
                                    "tooling/**/src/**/*.kt",
                                    "build-logic/src/functionalTest/**/*.kt",
                                )
                                exclude("**/build/**")
                            },
                        )
                        .withPropertyName("scannedSources")
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                }
            }
        }
    }
}

// The convention of `courses.test-suites` wires the suites that exist when it is applied; this one is registered after
// it, so `check` depends on it explicitly.
tasks.named("check") { dependsOn(tasks.named("architectureTest")) }

// detekt analyses the suite sources like production code; the exclusions for test names live in config/detekt.
detekt { source.from("src/architectureTest/kotlin") }
