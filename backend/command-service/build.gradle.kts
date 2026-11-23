// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

dependencies {
    implementation(project(":contracts-events"))
    implementation(project(":contracts-commands"))
    implementation(project(":serialization-jackson"))
    implementation(libs.axon.eventsourcing)
    implementation(libs.axon.server.connector)
    implementation(libs.kotlin.reflect)

    // The test application of the fixtures builds on the same Axon artifacts as the service; its public API exposes
    // Axon types (the event store and the unit of work factory), so consumers see them.
    testFixturesApi(libs.axon.eventsourcing)
    testFixturesImplementation(libs.axon.server.connector)

    testImplementation(testFixtures(project(":contracts-kernel")))

    integrationTestImplementation(project(":contracts-events"))
    integrationTestImplementation(project(":tooling:test-support"))
    integrationTestRuntimeOnly(libs.logback.classic)
    testRuntimeOnly(libs.logback.classic)
}
