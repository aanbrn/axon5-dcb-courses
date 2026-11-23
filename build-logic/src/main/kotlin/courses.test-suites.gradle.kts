// SPDX-License-Identifier: MIT
plugins {
    id("courses.kotlin-library")
    `java-test-fixtures`
}

// The three JVM test levels of a module (D-15), as Gradle JVM Test Suites:
//   test            unit tests: no Spring context, no containers
//   componentTest   one service: Spring context, a PostgreSQL container, Axon in memory
//   integrationTest the real Axon Server (and whatever else the scenario needs)
// Gradle wires only `test` into `check`; this convention wires all three, so that `check` is the one command that
// runs every level (D-19). Every suite sees its module, the module's `testFixtures` and the same assertion libraries
// (D-21); the two infrastructure suites run on a single fork (D-22) and after the preceding suite. The order uses
// `mustRunAfter`: `shouldRunAfter` is only a hint, and Gradle ran `integrationTest` before `test` in a `check` build
// (TestSuitesFunctionalTest). `mustRunAfter` adds no dependency, so running one suite alone still runs only that suite.

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

// The JVM Test Suite API is still marked @Incubating. It is the documented way to declare test levels (TEST-11), and a
// Gradle upgrade that changes it fails this build in its Renovate pull request.
@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.property)
            }
        }
        register<JvmTestSuite>("componentTest") {
            targets.configureEach {
                testTask.configure {
                    group = "verification"
                    description = "Runs the component tests of one service: Spring context, PostgreSQL, Axon in memory."
                    mustRunAfter(tasks.named("test"))
                    maxParallelForks = 1
                }
            }
        }
        register<JvmTestSuite>("integrationTest") {
            targets.configureEach {
                testTask.configure {
                    group = "verification"
                    description = "Runs the integration tests against a real Axon Server."
                    mustRunAfter(tasks.named("componentTest"))
                    maxParallelForks = 1
                }
            }
        }
    }
}

// D-19: `check` runs every suite, not only the built-in `test`.
@Suppress("UnstableApiUsage")
tasks.named("check") {
    dependsOn(testing.suites.withType<JvmTestSuite>().map { suite -> suite.targets.map { target -> target.testTask } })
}

// detekt analyses `main` and `test` only; the suite and fixture sources are widened in explicitly, so that they get the
// same analysis as production code (RESEARCH Pitfall 2).
detekt { source.from("src/componentTest/kotlin", "src/integrationTest/kotlin", "src/testFixtures/kotlin") }
