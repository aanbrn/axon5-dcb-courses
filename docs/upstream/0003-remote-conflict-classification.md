---
id: "UP-0003"
product: "axon"
product-version: "5.4.2"
status: "open"
workaround: true
upstream: []
reported: null
updated: "2026-11-23"
---

# UP-0003: A dispatch-side retry cannot classify a DCB conflict that arrives from Axon Server

## Symptom

A command whose conditional append is rejected by the DCB check must run again on fresh events, so the conflict has to
be retried. With Axon's own retry placement it is never retried over Axon Server. The dispatcher receives a
`CommandExecutionException` whose cause is an `AxonServerNonTransientRemoteCommandHandlingException` with error code
AXONIQ-4005, carrying only the message text of the original failure. The type `AppendEventsTransactionRejectedException`
is gone, so a `FilteringRetryPolicy` that looks for it in the cause chain never matches, and the losing command fails
on its first attempt.

Observed with Axon Framework 5.4.2 and Axoniq Framework 5.4.2 against Axon Server 2026.1.8, even when the command
handler runs in the same JVM as the dispatcher, because command results and failures cross the wire in every case.

## Minimal reproduction

1. Build an application on the Axon Server connector with a DCB context and a command handler that sources a tag
   criterion and appends an event with that tag.
2. Register a `RetryScheduler` component whose policy retries a failure only when
   `generateSequence(failure) { it.cause }.any { it is AppendEventsTransactionRejectedException }` holds. Axon then
   decorates the command bus with `RetryingCommandBus` at its default order `Int.MIN_VALUE + 1000`, the dispatching
   side.
3. Send 12 commands concurrently from a thread pool against the same tag so that appends conflict.
4. A losing command fails without a retry. Its failure is `CommandExecutionException` with the cause
   `AxonServerNonTransientRemoteCommandHandlingException` (AXONIQ-4005), and no
   `AppendEventsTransactionRejectedException` in the cause chain.

## Root cause

The connector converts the error code of a failed remote command into an exception without the original type. A
non-transient command error becomes `CommandExecutionException(message, AxonServerNonTransientRemoteCommandHandlingException(code, message), details, ...)`
([lines 162-168][factory]), so only the message and the application-specific `details` survive. The retry decorator
sits at `DECORATION_ORDER = Integer.MIN_VALUE + 1000` ([line 57][retrying]), outside the distributed command bus, and
therefore sees only this converted failure. On the handling side, below the distributed bus, the same failure is still
the typed `AppendEventsTransactionRejectedException`, which extends `AxonNonTransientException` and is for that reason
never retried by a plain policy.

[factory]: https://github.com/AxonIQ/axoniq-framework/blob/axoniq-5.4.2/connector/axon-server-connector/src/main/java/io/axoniq/framework/axonserver/connector/shared/ExceptionFactory.java#L162-L168
[retrying]: https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/messaging/src/main/java/org/axonframework/messaging/commandhandling/retry/RetryingCommandBus.java#L57

## Workaround

A `RetryingCommandBus` registered as a `CommandBus` decorator at `Int.MIN_VALUE + 10`, below the distributed command
bus (`Int.MIN_VALUE + 50`), so that it wraps the local handling segment and sees the typed exception. Its policy is a
`FilteringRetryPolicy` over the cause chain, bounded by `MaxAttemptsPolicy` and spaced by exponential back-off. No
`RetryScheduler` component is registered, because that would add Axon's own dispatch-side retry on top.

- `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/retry/ConflictRetry.kt`: the decoration
  order and the scheduler.
- `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt`: the
  registration of the retrying command bus.

## Removal criteria

An Axon or Axoniq Framework release in which a dispatch-side retry policy can recognise a DCB conflict that comes from
Axon Server: for example the exception of a remote failure carries the original exception type or a classification, or
the framework retries `AppendEventsTransactionRejectedException` on the handling side by default. The release notes
or the changed `ExceptionFactory` mapping show it.

## How to verify removal

Delete the `Int.MIN_VALUE + 10` decorator and both markers, register a `RetryScheduler` component whose policy filters
on the new classification, and run
`mise x -- ./gradlew :command-service:integrationTest --tests '*DecisionCaptureIntegrationTest*'`. The concurrent test
still ends with exactly 3 `Subscribed` and 9 `Rejected(course-full)`, and every attempt above 1 follows a conflict of
the same command.

## Upstream issue draft

Target: `AxonIQ/axoniq-framework`, where the connector converts the remote error, as an enhancement request. Not
published; publication needs the approval of the author (plan 02-22).

### A dispatch-side retry cannot recognise a DCB conflict that arrives from Axon Server

#### Environment and versions

- Product and version: Axoniq Framework 5.4.2 (`axon-server-connector`), Axon Framework 5.4.2
- Related products and versions: Axon Server 2026.1.8 (DCB context)
- JDK, operating system and build tool: Temurin 25, Linux and macOS, Gradle 9.9.2

#### Steps to reproduce

1. Use the Axon Server connector with a DCB context and a command handler that sources a tag criterion and appends an
   event with that tag.
2. Register a `RetryScheduler` whose policy retries only failures that have an `AppendEventsTransactionRejectedException`
   in their cause chain.
3. Send concurrent commands against the same tag so that appends conflict.

#### Expected behavior

The conflicting command is retried: a DCB conflict is the textbook case for running a decision again on fresh events.

#### Actual behavior

The losing command fails on its first attempt. The dispatcher sees
`CommandExecutionException` caused by `AxonServerNonTransientRemoteCommandHandlingException` (AXONIQ-4005) with the
message text of the original failure, and no `AppendEventsTransactionRejectedException` in the cause chain, so the
policy never matches.

#### Minimal reproduction

```kotlin
val policy = FilteringRetryPolicy(ExponentialBackOffRetryPolicy(5)) { failure ->
    generateSequence(failure) { it.cause }.any { it is AppendEventsTransactionRejectedException }
}
// Register AsyncRetryScheduler(MaxAttemptsPolicy(policy, 10), executor) as the RetryScheduler component, send
// concurrent commands that conflict, and print the failure of a loser: CommandExecutionException caused by
// AxonServerNonTransientRemoteCommandHandlingException, never the typed rejection.
```

#### Analysis

`ExceptionFactory` converts the error code to `CommandExecutionException` with an
`AxonServerNonTransientRemoteCommandHandlingException`
([source](https://github.com/AxonIQ/axoniq-framework/blob/axoniq-5.4.2/connector/axon-server-connector/src/main/java/io/axoniq/framework/axonserver/connector/shared/ExceptionFactory.java#L162-L168)),
which keeps no type information, and `RetryingCommandBus` is decorated at `Integer.MIN_VALUE + 1000`
([source](https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/messaging/src/main/java/org/axonframework/messaging/commandhandling/retry/RetryingCommandBus.java#L57)),
the dispatching side.

#### Proposed fix

Let a remote command failure carry the type or a classification of the original exception, so that a `RetryPolicy`
can filter on it on the dispatching side, or document handling-side placement of `RetryingCommandBus` as the way to
retry DCB conflicts. Either way, a short section in the reference guide on retrying DCB conflicts would help.
