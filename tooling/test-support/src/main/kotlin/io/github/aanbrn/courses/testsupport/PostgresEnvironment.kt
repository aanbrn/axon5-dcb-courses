// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.testsupport

import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * The PostgreSQL every component test of the JVM talks to: one per JVM, started on first use from the catalog image,
 * and stopped when the JVM ends, when Testcontainers' Ryuk reaper removes it.
 *
 * Reuse across JVMs is opt-in and local only, exactly as for [AxonServerEnvironment]: with
 * `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` the container survives the JVM, and CI never
 * enables it. The database is shared by every test of the JVM, so a test creates its own tables or schemas under unique
 * names instead of assuming an empty database.
 *
 * Nothing here knows Spring: the Spring wiring of a datasource (`@ServiceConnection`) belongs to the service that needs
 * it (Phase 4), and reads the same singleton.
 */
object PostgresEnvironment {
    private const val IMAGE_FAMILY = "postgres"

    private val container: PostgreSQLContainer by lazy {
        PostgreSQLContainer(DockerImageName.parse(ContainerImages.postgres).asCompatibleSubstituteFor(IMAGE_FAMILY))
            .withReuse(true)
            .also { it.start() }
    }

    /** The JDBC URL of the database inside the container, reachable from the test JVM. */
    val jdbcUrl: String by lazy { container.jdbcUrl }

    /** The name of the database user. */
    val username: String by lazy { container.username }

    /** The password of [username]. */
    val password: String by lazy { container.password }

    /** The image the container runs, as the full reference the catalog names, so that a test can check the pin. */
    val image: String by lazy { container.dockerImageName }
}
