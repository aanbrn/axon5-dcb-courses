// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.testsupport

/**
 * The container images the test infrastructure starts, read from the system properties that the `courses.test-suites`
 * convention sets from the version catalog.
 *
 * The catalog is the only place an image is named (INFRA-09), so no test source holds an image reference: a test that
 * needs a container asks this object. The property names are the contract with `ContainerImageArguments` in
 * build-logic, and are spelled once on each side.
 */
object ContainerImages {
    /** The system property that carries the Axon Server image reference. */
    const val AXON_SERVER_PROPERTY: String = "courses.image.axonserver"

    /** The system property that carries the PostgreSQL image reference. */
    const val POSTGRES_PROPERTY: String = "courses.image.postgres"

    /** The system property that carries the address of an Axon Server that is already running. */
    const val AXON_SERVER_EXTERNAL_PROPERTY: String = "courses.axonserver.external"

    /** The Axon Server image, with tag and digest. */
    val axonServer: String
        get() = required(AXON_SERVER_PROPERTY, "axonserver")

    /** The PostgreSQL image, with tag and digest. */
    val postgres: String
        get() = required(POSTGRES_PROPERTY, "postgres")

    /** The address of an Axon Server that already runs, such as `localhost:8124`, or `null` to start a container. */
    val axonServerExternal: String?
        get() = System.getProperty(AXON_SERVER_EXTERNAL_PROPERTY)?.takeIf { it.isNotBlank() }

    private fun required(property: String, key: String): String =
        System.getProperty(property)?.takeIf { it.isNotBlank() }
            ?: error("Container image '$key' is not set; run the test through Gradle, which passes the catalog images")
}
