// SPDX-License-Identifier: MIT
plugins { id("courses.test-suites") }

// The fourth D-02 contracts module. It holds no source until the first query slice adds a query message.
dependencies {
    api(project(":contracts-kernel"))
    // The @Query annotation is metadata only (D-04). Neither the events nor the commands module is needed (D-02).
    // A Jackson dependency waits until a query result needs @JsonTypeInfo (D-44).
    compileOnly(libs.axon.messaging)
}
