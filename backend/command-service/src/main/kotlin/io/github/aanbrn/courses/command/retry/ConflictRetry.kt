// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.retry

import java.util.concurrent.ScheduledExecutorService
import org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException
import org.axonframework.messaging.core.retry.AsyncRetryScheduler
import org.axonframework.messaging.core.retry.ExponentialBackOffRetryPolicy
import org.axonframework.messaging.core.retry.FilteringRetryPolicy
import org.axonframework.messaging.core.retry.MaxAttemptsPolicy
import org.axonframework.messaging.core.retry.RetryScheduler

/**
 * The order of the retrying command bus among the command bus decorators.
 *
 * A lower order is applied closer to the bus that handles the command. The distributed command bus has order
 * `Int.MIN_VALUE + 50`, so the retry sits below it and wraps the local handling segment. That is the only place where
 * the retry sees the real [AppendEventsTransactionRejectedException]: a retry on the dispatching side, where Axon
 * registers its own at `Int.MIN_VALUE + 1000`, receives a remote error that carries only the text of the original and
 * cannot tell a DCB conflict from any other failure.
 *
 * Register the retrying bus as a decorator at this order, and never register a [RetryScheduler] component, because that
 * adds Axon's own dispatch-side retry on top.
 */
const val CONFLICT_RETRY_DECORATION_ORDER = Int.MIN_VALUE + 10

/** The default bound on the retries of a conflicting command, see [conflictRetryScheduler]. */
const val DEFAULT_MAX_ATTEMPTS = 10

/** The delay before the first retry, which doubles with each further one. */
private const val INITIAL_BACK_OFF_MILLIS: Long = 5

/**
 * Creates the scheduler that retries a command whose append conflicted, and nothing else.
 *
 * A DCB conflict means that another decision appended an event this decision's criteria match, so the decision has to
 * run again on fresh events. Axon classifies the conflict as non-transient and never retries it by default, hence the
 * filter on the cause chain. Any other failure, including a business failure or a lost connection, is reported at once.
 * Retries are bounded and spaced by exponential back-off.
 *
 * @param executor runs the delayed retries; the caller owns it and shuts it down with the application
 * @param maxAttempts the bound handed to Axon's `MaxAttemptsPolicy`, which counts retries: a command is dispatched at
 *   most `maxAttempts + 1` times, and the conflict of the last attempt is reported to the caller
 * @return the scheduler for a retrying command bus
 */
fun conflictRetryScheduler(
    executor: ScheduledExecutorService,
    maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
): RetryScheduler =
    AsyncRetryScheduler(
        MaxAttemptsPolicy(
            FilteringRetryPolicy(ExponentialBackOffRetryPolicy(INITIAL_BACK_OFF_MILLIS)) { failure ->
                generateSequence(failure) { it.cause }.any { it is AppendEventsTransactionRejectedException }
            },
            maxAttempts,
        ),
        executor,
    )
