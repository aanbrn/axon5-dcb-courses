// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command

import io.github.aanbrn.courses.command.capture.DecisionCaptureEngine
import io.github.aanbrn.courses.command.capture.DecisionRecorder
import io.github.aanbrn.courses.command.retry.CONFLICT_RETRY_DECORATION_ORDER
import io.github.aanbrn.courses.command.retry.conflictRetryScheduler
import io.github.aanbrn.courses.serialization.coursesJsonMapper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiConsumer
import org.axonframework.common.configuration.ComponentDefinition
import org.axonframework.common.configuration.ComponentRegistry
import org.axonframework.common.configuration.Configuration
import org.axonframework.common.configuration.ConfigurationEnhancer
import org.axonframework.common.lifecycle.Phase
import org.axonframework.conversion.DelegatingGeneralConverter
import org.axonframework.conversion.GeneralConverter
import org.axonframework.conversion.jackson.JacksonConverter
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.TagResolver
import org.axonframework.messaging.commandhandling.CommandBus
import org.axonframework.messaging.commandhandling.retry.RetryingCommandBus

/**
 * Wires Axon for the command service: the converter and the tag resolver that the contracts need, the capture of every
 * decision and the retry of conflicting commands.
 *
 * - [GeneralConverter] is the Jackson converter over [coursesJsonMapper], the one mapper shared with the BFF, so that
 *   entity IDs are stored and read as TypeID strings (INFRA-06).
 * - [TagResolver] is [KotlinTagResolver], so that events are tagged with the TypeID of their value-class properties and
 *   the criteria built from the same IDs match them.
 * - [DecisionCaptureEngine] decorates the [EventStorageEngine], so that every command attempt produces a decision
 *   record without any code in a slice (D-13). It reports to the [DecisionRecorder] component, which the application
 *   must register: a missing recorder fails the configuration, so that capture is never silently off.
 * - [RetryingCommandBus] decorates the [CommandBus] at [CONFLICT_RETRY_DECORATION_ORDER] and retries a command whose
 *   append conflicted, up to a bound, on the handling side where the conflict is still a typed exception. No
 *   `RetryScheduler` component is registered, because that would add Axon's own dispatch-side retry.
 *
 * Applications and tests register the enhancer explicitly, and there is no `META-INF/services` entry: scanning would
 * apply it to every Axon configuration on the classpath, including those of other services. Phase 3 exposes it as a
 * Spring bean.
 */
class CommandServiceConfigurationEnhancer : ConfigurationEnhancer {

    /**
     * Registers the converter, the tag resolver, the capture decorator, the retry executor and the retry decorator.
     *
     * @param registry the registry of the Axon configuration being built
     */
    override fun enhance(registry: ComponentRegistry) {
        registry.registerComponent(GeneralConverter::class.java) {
            DelegatingGeneralConverter(JacksonConverter(coursesJsonMapper()))
        }
        // UPSTREAM: UP-0004 Axon's default tag resolver tags the raw UUID of a Kotlin value-class property, so the
        // resolver is replaced.
        registry.registerComponent(TagResolver::class.java) { KotlinTagResolver() }
        // UPSTREAM: UP-0001 Axon has no public extension point to observe the conditional append, so the capture
        // decorates the @Internal EventStorageEngine.
        registry.registerDecorator(EventStorageEngine::class.java, CAPTURE_DECORATION_ORDER) { config, _, engine ->
            DecisionCaptureEngine(engine, config.getComponent(DecisionRecorder::class.java))
        }
        // The decorator above resolves the recorder only when the storage engine is first built, which can be long
        // after
        // the application started. This guard resolves it at start, so that a missing recorder stops the application at
        // once instead of at the first command.
        registry.registerComponent(
            ComponentDefinition.ofType(RecorderGuard::class.java)
                .withBuilder { RecorderGuard() }
                .onStart(
                    Phase.EXTERNAL_CONNECTIONS,
                    BiConsumer<Configuration, RecorderGuard> { config, _ ->
                        config.getComponent(DecisionRecorder::class.java)
                    },
                ),
        )
        registry.registerComponent(
            ComponentDefinition.ofType(ConflictRetryExecutor::class.java)
                .withBuilder { ConflictRetryExecutor() }
                // The executor is the last thing to stop, so that a command handled during shutdown can still retry.
                .onShutdown(Phase.EXTERNAL_CONNECTIONS) { executor: ConflictRetryExecutor -> executor.close() },
        )
        // UPSTREAM: UP-0003 A dispatch-side retry cannot classify a DCB conflict that arrives from Axon Server, so the
        // retry decorates the handling side.
        registry.registerDecorator(CommandBus::class.java, CONFLICT_RETRY_DECORATION_ORDER) { config, _, bus ->
            RetryingCommandBus(
                bus,
                conflictRetryScheduler(config.getComponent(ConflictRetryExecutor::class.java).executor),
            )
        }
    }

    private companion object {
        /** The capture engine wraps the engine as Axon built it, with nothing in between. */
        const val CAPTURE_DECORATION_ORDER = 0
    }
}

/**
 * Exists only to carry the start handler that requires a [DecisionRecorder], see [CommandServiceConfigurationEnhancer].
 */
internal class RecorderGuard

/**
 * The threads that run delayed retries of conflicting commands, owned by the Axon configuration and closed with it.
 *
 * It has its own type instead of being registered as a `ScheduledExecutorService`, so that no component lookup of that
 * type, by Axon or by an application, can find it by accident.
 */
internal class ConflictRetryExecutor : AutoCloseable {
    private val threadNumber = AtomicInteger()

    /** Runs the retries on daemon threads, so that a forgotten shutdown never keeps a JVM alive. */
    val executor: ScheduledExecutorService =
        Executors.newScheduledThreadPool(THREADS) { task ->
            Thread(task, "conflict-retry-${threadNumber.incrementAndGet()}").apply { isDaemon = true }
        }

    /** Stops the threads and drops the retries that have not run yet. */
    override fun close() {
        executor.shutdownNow()
    }

    private companion object {
        const val THREADS = 4
    }
}
