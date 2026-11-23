// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

dependencies {
    // Consumers of the singletons start and talk to containers themselves, so both libraries are part of the API.
    api(libs.testcontainers)
    api(libs.axoniq.testcontainer)
}
