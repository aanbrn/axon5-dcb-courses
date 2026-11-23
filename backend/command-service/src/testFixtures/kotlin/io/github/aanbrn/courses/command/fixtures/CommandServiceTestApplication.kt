// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.fixtures

import io.axoniq.framework.axonserver.connector.api.AxonServerConfiguration
import io.axoniq.framework.axonserver.connector.configuration.AxonServerConfigurationEnhancer
import io.github.aanbrn.courses.command.CommandServiceConfigurationEnhancer
import io.github.aanbrn.courses.command.capture.DecisionRecorder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import org.axonframework.common.configuration.AxonConfiguration
import org.axonframework.common.configuration.ComponentRegistry
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.core.MessageTypeResolver
import org.axonframework.messaging.core.conversion.MessageConverter
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory

/**
 * The command service's Axon wiring, started the way a test needs it and shut down with [close].
 *
 * It is the production wiring ([CommandServiceConfigurationEnhancer]) on an event store of the test's choosing: a real
 * Axon Server through [onAxonServer], or an in-memory engine through [inMemory] for unit tests that need no container.
 * The tests use only the public [EventStore], [UnitOfWorkFactory] and [CommandGateway], never the `@Internal` storage
 * engine.
 *
 * The production wiring requires a [DecisionRecorder], and so does this application: every factory takes one, and the
 * default discards the records. A test that inspects them passes a [CollectingDecisionRecorder]. The [CapacityHarness],
 * the one command handler the command service has before Phase 3, is registered only when a test asks for it.
 */
class CommandServiceTestApplication
private constructor(
    private val configuration: AxonConfiguration,
    private val harness: AtomicReference<CapacityHarness>,
) : AutoCloseable {

    /** The event store of the application, over Axon Server or over memory. */
    val eventStore: EventStore = configuration.getComponent(EventStore::class.java)

    /** Creates the units of work in which tests source and append events. */
    val unitOfWorkFactory: UnitOfWorkFactory = configuration.getComponent(UnitOfWorkFactory::class.java)

    /** Converts message payloads the way the service does, so a test can read a stored event back into its type. */
    val messageConverter: MessageConverter = configuration.getComponent(MessageConverter::class.java)

    /** Dispatches commands to the handlers of the application, over Axon Server or in memory. */
    val commandGateway: CommandGateway = configuration.getComponent(CommandGateway::class.java)

    /**
     * The capacity harness of the application.
     *
     * @throws IllegalStateException when the application was started without the harness
     */
    val capacityHarness: CapacityHarness
        get() = checkNotNull(harness.get()) { "The application was started without the capacity harness" }

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
         * @param recorder receives the decision record of every command attempt
         * @param withCapacityHarness whether to register the [CapacityHarness] as the handler of `SubscribeStudent`
         * @param beforeAppend the `beforeAppend` hook of the harness; ignored without the harness
         * @return the started application
         */
        fun onAxonServer(
            address: String,
            recorder: DecisionRecorder = DecisionRecorder {},
            withCapacityHarness: Boolean = false,
            beforeAppend: () -> CompletableFuture<*> = READY,
        ): CommandServiceTestApplication =
            start(recorder, withCapacityHarness, beforeAppend) { registry ->
                registry.registerComponent(AxonServerConfiguration::class.java) {
                    AxonServerConfiguration().apply { servers = address }
                }
            }

        /**
         * Starts the application over an in-memory event store, with Axon Server's wiring switched off.
         *
         * @param recorder receives the decision record of every command attempt
         * @param withCapacityHarness whether to register the [CapacityHarness] as the handler of `SubscribeStudent`
         * @param beforeAppend the `beforeAppend` hook of the harness; ignored without the harness
         * @return the started application
         */
        fun inMemory(
            recorder: DecisionRecorder = DecisionRecorder {},
            withCapacityHarness: Boolean = false,
            beforeAppend: () -> CompletableFuture<*> = READY,
        ): CommandServiceTestApplication =
            start(recorder, withCapacityHarness, beforeAppend) { registry ->
                registry.disableEnhancer(AxonServerConfigurationEnhancer::class.java)
                registry.registerComponent(EventStorageEngine::class.java) { InMemoryEventStorageEngine() }
            }

        private val READY: () -> CompletableFuture<*> = { CompletableFuture.completedFuture(Unit) }

        private fun start(
            recorder: DecisionRecorder,
            withCapacityHarness: Boolean,
            beforeAppend: () -> CompletableFuture<*>,
            configureStore: (ComponentRegistry) -> Unit,
        ): CommandServiceTestApplication {
            val harness = AtomicReference<CapacityHarness>()
            val configurer =
                EventSourcingConfigurer.create().componentRegistry { registry ->
                    registry.registerEnhancer(CommandServiceConfigurationEnhancer())
                    registry.registerComponent(DecisionRecorder::class.java) { recorder }
                    configureStore(registry)
                }
            if (withCapacityHarness) {
                configurer.registerCommandHandlingModule(
                    CommandHandlingModule.named("capacity-harness").commandHandlers().commandHandler(
                        CapacityHarness.SUBSCRIBE_STUDENT_NAME,
                    ) { configuration ->
                        CapacityHarness(
                                configuration.getComponent(EventStore::class.java),
                                configuration.getComponent(UnitOfWorkFactory::class.java),
                                configuration.getComponent(MessageConverter::class.java),
                                configuration.getComponent(MessageTypeResolver::class.java),
                                beforeAppend,
                            )
                            .also(harness::set)
                    },
                )
            }
            return CommandServiceTestApplication(configurer.start(), harness)
        }
    }
}
