// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.testsupport

import io.axoniq.framework.testcontainer.AxonServerContainer
import org.testcontainers.utility.DockerImageName

/**
 * The Axon Server every integration test of the JVM talks to: one per JVM, started on first use.
 *
 * When `-Pcourses.axonserver.external=localhost:8124` is set, the tests use that already running server and no
 * container starts (D-46, the host scenario against the compose stack). Otherwise one container is started from the
 * catalog image, with a DCB context and development mode, and stays up until the JVM ends, when Testcontainers' Ryuk
 * reaper removes it. The shared holder that `axoniq-testcontainer` ships is deliberately not used: it hard-codes the
 * floating `:latest` image, which would bypass the digest pin of the catalog (INFRA-09).
 *
 * Reuse across JVMs is opt-in and local only: with `testcontainers.reuse.enable=true` in `~/.testcontainers.properties`
 * the container survives the JVM and the next test run, even in another module, attaches to it. CI never enables it, so
 * every CI run starts a fresh server. Sharing a server is safe because no test purges the store: every test creates its
 * own entities from fresh IDs, so its events never overlap another test's criteria (D-16).
 */
object AxonServerEnvironment {
    private const val IMAGE_FAMILY = "docker.axoniq.io/axoniq/axonserver"

    private val container: AxonServerContainer by lazy {
        AxonServerContainer(DockerImageName.parse(ContainerImages.axonServer).asCompatibleSubstituteFor(IMAGE_FAMILY))
            .withDevMode(true)
            .withDcbContext(true)
            .withEnv("AXONIQ_UPDATE_CHECKER_DISABLED", "true")
            .withReuse(true)
            .also { it.start() }
    }

    /** The gRPC address of the Axon Server, as `host:port`: the external one when set, otherwise the container's. */
    val address: String by lazy { ContainerImages.axonServerExternal ?: container.axonServerAddress }

    /**
     * The address of the HTTP port (management API and `/actuator/health`) of the container, as `host:port`, or `null`
     * when an external server is used, because only its gRPC address is configured.
     */
    val httpAddress: String? by lazy {
        if (ContainerImages.axonServerExternal != null) null else "${container.host}:${container.httpPort}"
    }
}
