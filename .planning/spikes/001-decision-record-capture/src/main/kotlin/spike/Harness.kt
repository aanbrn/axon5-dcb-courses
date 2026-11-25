package spike

import java.util.concurrent.Executors
import org.axonframework.common.configuration.AxonConfiguration
import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson.JacksonConverter
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.messaging.core.retry.AsyncRetryScheduler
import org.axonframework.messaging.core.retry.ExponentialBackOffRetryPolicy
import org.axonframework.messaging.core.retry.FilteringRetryPolicy
import org.axonframework.messaging.core.retry.MaxAttemptsPolicy
import org.axonframework.messaging.core.retry.RetryScheduler
import org.axonframework.messaging.commandhandling.CommandBus
import org.axonframework.messaging.commandhandling.retry.RetryingCommandBus
import io.axoniq.framework.axonserver.connector.api.AxonServerConfiguration
import io.axoniq.framework.axonserver.connector.configuration.AxonServerConfigurationEnhancer
import spike.capture.DecisionRecorder
import spike.capture.EngineCapture
import spike.capture.StoreCapture
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

enum class Variant { ENGINE, STORE }

sealed interface Backend {
    data object InMemory : Backend
    data class AxonServer(val address: String) : Backend
}

/** Retry only DCB conflicts: AppendEventsTransactionRejectedException is non-transient, so Axon never retries it by default. */
fun conflictRetryScheduler(maxRetries: Int = 10): RetryScheduler =
    AsyncRetryScheduler(
        MaxAttemptsPolicy(
            FilteringRetryPolicy(ExponentialBackOffRetryPolicy(5)) { t ->
                generateSequence(t) { it.cause }.any { it is AppendEventsTransactionRejectedException }
            },
            maxRetries,
        ),
        Executors.newScheduledThreadPool(4),
    )

fun startAxon(variant: Variant, backend: Backend, recorder: DecisionRecorder, retries: Boolean): AxonConfiguration =
    EventSourcingConfigurer.create()
        .registerEntity(EventSourcedEntityModule.autodetected(String::class.java, CourseState::class.java))
        .registerCommandHandlingModule(
            CommandHandlingModule.named("courses").commandHandlers().autodetectedCommandHandlingComponent { CourseCommandHandlers() }
        )
        .componentRegistry { cr ->
            cr.registerComponent(GeneralConverter::class.java) {
                DelegatingGeneralConverter(JacksonConverter(JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()))
            }
            when (backend) {
                Backend.InMemory -> {
                    cr.disableEnhancer(AxonServerConfigurationEnhancer::class.java)
                    cr.registerComponent(EventStorageEngine::class.java) { InMemoryEventStorageEngine() }
                }
                is Backend.AxonServer ->
                    cr.registerComponent(AxonServerConfiguration::class.java) {
                        AxonServerConfiguration().apply { setServers(backend.address) }
                    }
            }
            when (variant) {
                Variant.ENGINE -> cr.registerDecorator(EventStorageEngine::class.java, 0) { _, _, d -> EngineCapture(d, recorder) }
                Variant.STORE -> cr.registerDecorator(EventStore::class.java, 0) { _, _, d -> StoreCapture(d, recorder) }
            }
            // Handling-side retry: order below DistributedCommandBus (MIN+50) wraps the local segment, so the retry
            // sees the real AppendEventsTransactionRejectedException instead of a remote, type-erased error.
            if (retries) cr.registerDecorator(CommandBus::class.java, Int.MIN_VALUE + 10) { _, _, d -> RetryingCommandBus(d, conflictRetryScheduler()) }
        }
        .start()
