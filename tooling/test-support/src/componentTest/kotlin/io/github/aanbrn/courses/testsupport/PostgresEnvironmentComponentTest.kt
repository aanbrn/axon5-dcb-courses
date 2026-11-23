// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.testsupport

import io.kotest.matchers.shouldBe
import java.sql.DriverManager
import org.junit.jupiter.api.Test

/** Smoke test of the PostgreSQL singleton: the container starts from the catalog image and accepts JDBC queries. */
class PostgresEnvironmentComponentTest {
    @Test
    fun `the PostgreSQL singleton answers a query over JDBC`() {
        val answer =
            DriverManager.getConnection(
                    PostgresEnvironment.jdbcUrl,
                    PostgresEnvironment.username,
                    PostgresEnvironment.password,
                )
                .use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT 1").use { rows ->
                            rows.next()
                            rows.getInt(1)
                        }
                    }
                }

        answer shouldBe 1
    }

    @Test
    fun `the PostgreSQL singleton runs the image of the version catalog`() {
        PostgresEnvironment.image shouldBe ContainerImages.postgres
    }
}
