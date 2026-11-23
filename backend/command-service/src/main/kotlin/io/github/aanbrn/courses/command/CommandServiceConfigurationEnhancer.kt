// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.serialization.coursesJsonMapper
import org.axonframework.common.configuration.ComponentRegistry
import org.axonframework.common.configuration.ConfigurationEnhancer
import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson.JacksonConverter
import org.axonframework.eventsourcing.eventstore.TagResolver

/**
 * Wires Axon for the command service: the converter and the tag resolver that the contracts need.
 *
 * - [GeneralConverter] is the Jackson converter over [coursesJsonMapper], the one mapper shared with the BFF, so that
 *   entity IDs are stored and read as TypeID strings (INFRA-06).
 * - [TagResolver] is [KotlinTagResolver], so that events are tagged with the TypeID of their value-class properties and
 *   the criteria built from the same IDs match them.
 *
 * Applications and tests register the enhancer explicitly, and there is no `META-INF/services` entry: scanning would
 * apply it to every Axon configuration on the classpath, including those of other services. Phase 3 exposes it as a
 * Spring bean.
 */
class CommandServiceConfigurationEnhancer : ConfigurationEnhancer {

    /**
     * Registers the converter and the tag resolver.
     *
     * @param registry the registry of the Axon configuration being built
     */
    override fun enhance(registry: ComponentRegistry) {
        registry.registerComponent(GeneralConverter::class.java) {
            DelegatingGeneralConverter(JacksonConverter(coursesJsonMapper()))
        }
        registry.registerComponent(TagResolver::class.java) { KotlinTagResolver() }
    }
}
