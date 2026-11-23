// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

dependencies {
    api(project(":contracts-kernel"))
    // The annotations (@Event, @EventTag) are metadata only: the contracts carry no Axon runtime (D-04).
    compileOnly(libs.axon.messaging)
    compileOnly(libs.axon.eventsourcing)
}
