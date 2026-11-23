// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.doctor

import kotlin.system.exitProcess
import org.testcontainers.DockerClientFactory
import org.testcontainers.utility.TestcontainersConfiguration

/** The name of the check as `scripts/doctor.sh` prints it. */
private const val CHECK = "testcontainers"

/** The Testcontainers setting that disables the Ryuk reaper, read the way Testcontainers reads it. */
private const val RYUK_DISABLED_KEY = "ryuk.disabled"

/**
 * Checks that Testcontainers reaches a Docker engine, through the Testcontainers library itself, so that the result is
 * the one every integration test gets. Prints one `ok` or `fail` line in the format of `scripts/doctor.sh` and exits
 * non-zero on failure. It starts nothing besides the Ryuk reaper that Testcontainers starts for any client, and changes
 * nothing.
 */
fun main() {
    val exitCode =
        try {
            println(describeConnection())
            0
        } catch (@Suppress("TooGenericExceptionCaught") failure: RuntimeException) {
            // The probe exists to turn any failure of Testcontainers' discovery or of the Docker call into a diagnosis.
            println(describeFailure(failure))
            1
        }
    exitProcess(exitCode)
}

private fun describeConnection(): String {
    val factory = DockerClientFactory.instance()
    val client = factory.client()
    val dockerHost = factory.transportConfig.dockerHost
    val serverVersion = client.versionCmd().exec().version ?: "unknown"
    val ryukState = if (isRyukDisabled()) "disabled" else "enabled"
    return "ok  $CHECK: $dockerHost, server $serverVersion, ryuk $ryukState"
}

private fun isRyukDisabled(): Boolean =
    TestcontainersConfiguration.getInstance().getEnvVarOrProperty(RYUK_DISABLED_KEY, "false").toBoolean()

private fun describeFailure(failure: RuntimeException): String {
    val reason = failure.message?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim() ?: failure::class.simpleName
    return listOf(
            "fail  $CHECK: $reason",
            "      fix: colima: set tc.host=unix:///var/run/docker.sock (or docker.host) in ~/.testcontainers.properties, " +
                "or export DOCKER_HOST",
            "      fix: colima, Ryuk cannot mount the socket: export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock",
            "      fix: Docker Desktop: start Docker Desktop; Testcontainers detects it automatically",
        )
        .joinToString("\n")
}
