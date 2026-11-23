// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.testsupport

import io.kotest.matchers.shouldBe
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.junit.jupiter.api.Test

/** Smoke test of the Axon Server singleton: the configured external server, or a container that reports healthy. */
class AxonServerEnvironmentIntegrationTest {
    @Test
    fun `the Axon Server singleton is the configured server or a healthy container`() {
        val external = ContainerImages.axonServerExternal
        if (external != null) {
            AxonServerEnvironment.address shouldBe external
            return
        }

        val health = URI.create("http://${AxonServerEnvironment.httpAddress}/actuator/health")
        val response =
            HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(health).GET().build(), HttpResponse.BodyHandlers.ofString())

        response.statusCode() shouldBe HTTP_OK
    }

    private companion object {
        const val HTTP_OK = 200
    }
}
