// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

dependencies {
    implementation(libs.java.uuid.generator)
    // The ID generators in `testFixtures` return kotest `Arb` values, so consumers need kotest-property.
    testFixturesApi(libs.kotest.property)
    testFixturesImplementation(libs.java.uuid.generator)
    testImplementation(libs.jackson.dataformat.yaml)
}
