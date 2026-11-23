// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

dependencies {
    api(project(":contracts-kernel"))
    // The annotations (@Command, @JsonTypeInfo) are metadata only: the contracts carry no Axon or Jackson runtime
    // (D-04, D-44). The commands do not depend on the events module (D-02).
    compileOnly(libs.axon.messaging)
    compileOnly(libs.jackson.annotations)
}
