// SPDX-License-Identifier: MIT
import io.github.aanbrn.courses.buildlogic.ContainerImageArguments

plugins {
    id("courses.kotlin-library")
    id("org.jetbrains.kotlinx.kover")
}

// The three JVM test levels of a module (D-15), as Gradle JVM Test Suites:
//   test            unit tests: no Spring context, no containers
//   componentTest   one service: Spring context, a PostgreSQL container, Axon in memory
//   integrationTest the real Axon Server (and whatever else the scenario needs)
// Gradle wires only `test` into `check`; this convention wires all three, so that `check` is the one command that
// runs every level (D-19). Every suite sees its module and the same assertion libraries (D-21); the two infrastructure
// suites run on a single fork (D-22) and after the preceding suite. The order uses
// `mustRunAfter`: `shouldRunAfter` is only a hint, and Gradle ran `integrationTest` before `test` in a `check` build
// (TestSuitesFunctionalTest). `mustRunAfter` adds no dependency, so running one suite alone still runs only that suite.

// Test fixtures are opt-in. A module that has fixtures applies `java-test-fixtures` in its own plugins block, and then
// every suite of the module, including `architectureTest`, sees them. A module without fixtures gets no empty
// `testFixtures` source set: a suite dependency on it would be a dependency nobody uses (D-21), and the IDE would
// import it as an unused module dependency.

// Coverage (TEST-11): Kover instruments every `Test` task of the module, so `test`, `componentTest` and
// `integrationTest` all feed the module's report (`koverXmlReport`, `koverHtmlReport`). No test task is excluded and no
// class filter is set here. Kover is a Gradle plugin, not a test library: it adds nothing to any suite's dependencies.
// The cross-module aggregation, the thresholds, the exclusions and the CI reporting are TEST-08 (Phase 3).

// Container images (INFRA-09, TOOL-04): the two infrastructure suites receive the images of the version catalog as
// system properties through `ContainerImageArguments`, whose values are task inputs, so a new image re-runs the tests.
// `-Pcourses.axonserver.external=localhost:8124` makes the tests use an Axon Server that already runs (D-46) and is an
// input as well. Testcontainers' own Ryuk reaper is pinned through its environment variable, the same way on colima and
// Docker Desktop, and the Axon update checker is switched off in every test JVM. No image literal exists in any test.

// Native access (JEP 472): since JDK 24 a call of a restricted method (`System::load`, `System::loadLibrary`, the
// foreign function and memory linker) from code without native access prints a warning, and a future JDK blocks the
// call. Two libraries of the test classpath make such calls: the gRPC transport of the Axon Server connector
// (grpc-netty-shaded, `NativeLibraryUtil` through `System::loadLibrary`) and JNA (`com.sun.jna.Native` through
// `System::load`, reached through Testcontainers). Both load from the classpath, which is the unnamed module, so
// `ALL-UNNAMED` is the narrowest scope that covers them; a module name would match nothing. The argument is set on
// every `Test` task of a module that applies this convention, `architectureTest` included. It is a task input, so a
// change re-runs the suites. It does not cover the Gradle daemon, which Gradle starts with the flag itself, the
// build-logic TestKit JVM, which loads neither library, or the runtime JVMs (`bootRun`, the service images).

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

/** Passes the catalog images and the environment settings of the container-based suites to [this] test task. */
fun Test.useContainerImages() {
    jvmArgumentProviders.add(
        objects.newInstance<ContainerImageArguments>().apply {
            images.put("axonserver", libs.versions.image.axonserver)
            images.put("postgres", libs.versions.image.postgres)
            images.put("ryuk", libs.versions.image.ryuk)
            axonServerExternal.set(providers.gradleProperty("courses.axonserver.external"))
        },
    )
    environment("AXONIQ_UPDATE_CHECKER_DISABLED", "true")
    environment("TESTCONTAINERS_RYUK_CONTAINER_IMAGE", libs.versions.image.ryuk.get())
}

// The JVM Test Suite API is still marked @Incubating. It is the documented way to declare test levels (TEST-11), and a
// Gradle upgrade that changes it fails this build in its Renovate pull request.
@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            val suite = this
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(project())
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.property)
            }
            pluginManager.withPlugin("java-test-fixtures") {
                suite.dependencies { implementation(testFixtures(project())) }
            }
        }
        register<JvmTestSuite>("componentTest") {
            targets.configureEach {
                testTask.configure {
                    group = "verification"
                    description = "Runs the component tests of one service: Spring context, PostgreSQL, Axon in memory."
                    mustRunAfter(tasks.named("test"))
                    maxParallelForks = 1
                    useContainerImages()
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
                    useContainerImages()
                }
            }
        }
    }
}

// Every test JVM of the module gets native access for the classpath (see "Native access" above). By type, so that
// `architectureTest` and any later suite is covered, and lazy, so that no task is created for it.
tasks.withType<Test>().configureEach { jvmArgs("--enable-native-access=ALL-UNNAMED") }

// D-19: `check` runs every suite, not only the built-in `test`.
@Suppress("UnstableApiUsage")
tasks.named("check") {
    dependsOn(testing.suites.withType<JvmTestSuite>().map { suite -> suite.targets.map { target -> target.testTask } })
}

// detekt analyses `main` and `test` only; the suite and fixture sources are widened in explicitly, so that they get the
// same analysis as production code (RESEARCH Pitfall 2).
detekt { source.from("src/componentTest/kotlin", "src/integrationTest/kotlin", "src/testFixtures/kotlin") }
