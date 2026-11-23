// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

dependencies {
    implementation(libs.java.uuid.generator)
    testImplementation(libs.jackson.dataformat.yaml)
}
