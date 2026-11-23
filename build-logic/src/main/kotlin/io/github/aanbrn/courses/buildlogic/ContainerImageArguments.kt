// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider

/**
 * Hands the container images of the version catalog, and the optional external Axon Server address, to a test task as
 * system properties.
 *
 * The values are `@Input`s of the provider, so Gradle tracks them as inputs of the test task: a new image tag or digest
 * in the catalog, or another external address, re-runs the tests instead of reusing a cached result. A plain
 * `systemProperty(name, provider)` would hide that dependency from the task's cache key.
 *
 * The property names are the contract with `:tooling:test-support`, which reads them back in `ContainerImages`.
 */
abstract class ContainerImageArguments : CommandLineArgumentProvider {

    /** Full image references (tag and digest) by short name: `axonserver`, `postgres` and `ryuk`. */
    @get:Input abstract val images: MapProperty<String, String>

    /** The address of an Axon Server that is already running, such as `localhost:8124`; absent to start a container. */
    @get:Input @get:Optional abstract val axonServerExternal: Property<String>

    /**
     * @return one `-Dcourses.image.<name>=<reference>` per image, and `-Dcourses.axonserver.external` when it is set
     */
    override fun asArguments(): Iterable<String> {
        val imageArguments = images.get().toSortedMap().map { (name, reference) -> "-D$IMAGE_PREFIX$name=$reference" }
        val externalArguments = axonServerExternal.map { listOf("-D$EXTERNAL_PROPERTY=$it") }.getOrElse(emptyList())
        return imageArguments + externalArguments
    }

    private companion object {
        const val IMAGE_PREFIX = "courses.image."
        const val EXTERNAL_PROPERTY = "courses.axonserver.external"
    }
}
