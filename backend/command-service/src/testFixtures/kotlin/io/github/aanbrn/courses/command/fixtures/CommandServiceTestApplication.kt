// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.fixtures

import io.axoniq.framework.axonserver.connector.api.AxonServerConfiguration
import io.axoniq.framework.axonserver.connector.configuration.AxonServerConfigurationEnhancer
import io.github.aanbrn.courses.command.CommandServiceConfigurationEnhancer
import org.axonframework.common.configuration.AxonConfiguration
import org.axonframework.common.configuration.ComponentRegistry
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory

/**
 * The command service's Axon wiring, started the way a test needs it and shut down with [close].
 *
 * It is the production wiring ([CommandServiceConfigurationEnhancer]) on an event store of the test's choosing: a real
 * Axon Server through [onAxonServer], or an in-memory engine through [inMemory] for unit tests that need no container.
 * The tests use only the public [EventStore] and [UnitOfWorkFactory], never the `@Internal` storage engine.
 */
class CommandServiceTestApplication private constructor(private val configuration: AxonConfiguration) : AutoCloseable {

    /** The event store of the application, over Axon Server or over memory. */
    val eventStore: EventStore = configuration.getComponent(EventStore::class.java)

    /** Creates the units of work in which tests source and append events. */
    val unitOfWorkFactory: UnitOfWorkFactory = configuration.getComponent(UnitOfWorkFactory::class.java)

    /** Shuts the Axon configuration down, which closes its Axon Server connection. */
    override fun close() {
        configuration.shutdown()
    }

    /** Factories of the application. */
    companion object {
        /**
         * Starts the application against an Axon Server.
         *
         * @param address the gRPC address of the server, as `host:port`
         * @return the started application
         */
        fun onAxonServer(address: String): CommandServiceTestApplication = start { registry ->
            registry.registerComponent(AxonServerConfiguration::class.java) {
                AxonServerConfiguration().apply { setServers(address) }
            }
        }

        /**
         * Starts the application over an in-memory event store, with Axon Server's wiring switched off.
         *
         * @return the started application
         */
        fun inMemory(): CommandServiceTestApplication = start { registry ->
            registry.disableEnhancer(AxonServerConfigurationEnhancer::class.java)
            registry.registerComponent(EventStorageEngine::class.java) { InMemoryEventStorageEngine() }
        }

        private fun start(configureStore: (ComponentRegistry) -> Unit): CommandServiceTestApplication {
            val configuration =
                EventSourcingConfigurer.create()
                    .componentRegistry { registry ->
                        registry.registerEnhancer(CommandServiceConfigurationEnhancer())
                        configureStore(registry)
                    }
                    .start()
            return CommandServiceTestApplication(configuration)
        }
    }
}
