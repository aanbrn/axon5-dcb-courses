// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

dependencies {
    implementation(project(":contracts-kernel"))
    api(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    // The module depends on the kernel only (D-02, D-44); the contracts it serialises are test subjects. The kernel is
    // `implementation`: no kernel type appears in the public API, which exposes Jackson types only.
    testImplementation(project(":contracts-events"))
    testImplementation(project(":contracts-commands"))
    testImplementation(testFixtures(project(":contracts-kernel")))
}
