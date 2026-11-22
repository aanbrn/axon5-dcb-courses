# Decision-Record Capture and Conflict Retries

## Requirements

- Decision-record capture uses the `EventStorageEngine` decorator (variant A), accepting the `@Internal` API with the
  D-13 safeguards; conflict retries run handling-side.

## How to Build It

1. **Capture decorator** on `EventStorageEngine` (Kotlin delegation keeps it small; full code in
   `sources/001-decision-record-capture/src/main/kotlin/spike/capture/`):
   - `source(condition, ctx)`: record `condition.criteria()` and each sourced event (skip entries that carry
     `ConsistencyMarker.RESOURCE_KEY`, they are marker-only).
   - `appendEvents(condition, ctx, events)`: record `condition.criteria()`, `condition.consistencyMarker()` and the
     `TaggedEventMessage`s (type, payload, `tags()`); wrap the returned `AppendTransaction` to record
     `afterCommit(...)`'s new `ConsistencyMarker`.
   - Conflict diagnosis on **both** paths (in-memory rejects in `appendEvents`, Axon Server in `commit()`): unwrap the
     cause chain to `AppendEventsTransactionRejectedException`, then
     `delegate.source(SourcingCondition.conditionFor(marker.position(), criteria), ctx)` → first non-terminal event =
     the conflicting event; re-fail with the original failure.
   - Per-`ProcessingContext` accumulator stored under a `ResourceKey`; command from `Message.fromContext(ctx)`;
     finish in `ctx.whenComplete` (committed / nothing appended) and `ctx.onError` (conflict / failure).
2. **Attempt numbering**: retries re-dispatch the same `CommandMessage`, so count records per
   `command.identifier()`.
3. **Retry**, registered as a `CommandBus` decorator at order `Int.MIN_VALUE + 10` (below `DistributedCommandBus`
   `MIN+50`, i.e. wrapping the local segment on the handling side):

   ```kotlin
   registry.registerDecorator(CommandBus::class.java, Int.MIN_VALUE + 10) { _, _, d ->
       RetryingCommandBus(d, AsyncRetryScheduler(
           MaxAttemptsPolicy(
               FilteringRetryPolicy(ExponentialBackOffRetryPolicy(5)) { t ->
                   generateSequence(t) { it.cause }.any { it is AppendEventsTransactionRejectedException }
               }, 10),
           Executors.newScheduledThreadPool(4)))
   }
   ```

   Do **not** also register a `RetryScheduler` component (that adds Axon's outer, dispatch-side retry).
4. **Wiring in Spring Boot**: one `ConfigurationEnhancer` `@Bean` per command service registers both decorators
   (verified: `SpringComponentRegistry` discovers enhancer beans; the decorator saw the append).
5. **Permanent test** (D-13 safeguard): A × Axon Server Testcontainer, 12 concurrent subscriptions on a 3-seat
   course → exactly 3 committed, conflicts recorded with the conflicting event, every attempt > 1 preceded by a
   conflict of the same command id. Observed: 42 attempts, 30 conflicts, max attempt 4.

## What to Avoid

- **Variant B (public `EventStore`/`EventStoreTransaction` decorator)** — needs four workarounds and still lacks
  tags and the conflicting event: `StorageEngineBackedEventStore.publish()` bypasses the decorator (override
  `publish`); the resolved condition only via an identity `overrideAppendCondition` (sees it before later user
  overrides); after-commit marker only via `appendPosition()` in `whenComplete` (`runOnAfterCommit` reads `ORIGIN`).
- **`ConflictDiagnosingEventStorageEngine`** (`@Internal`): never diagnoses on Axon Server — the rejection reaches it
  wrapped in `CompletionException` and it checks `instanceof` without unwrapping.
- **Axon's default retry placement** (`RetryingCommandBus.DECORATION_ORDER = MIN+1000`, dispatch side): over Axon
  Server the conflict arrives as `CommandExecutionException(AxonServerNonTransientRemoteCommandHandlingException
  AXONIQ-4005)` with only the message text — no type to filter on.
- **A plain retry policy**: `AppendEventsTransactionRejectedException extends AxonNonTransientException`, so conflicts
  are never retried without a `FilteringRetryPolicy`.
- **Creation commands that source nothing**: they append with `AppendCondition.none()` (`AnyEvent`, `INFINITY`) — no
  consistency check, two concurrent creates of the same id both succeed. Creation slices must source their own
  criteria (e.g. the course tag) to guarantee "does not exist yet".
- **Concurrency tests that dispatch from one thread**: in-memory runs synchronously → 0 conflicts. Use a pool.

## Constraints

- `@Internal` types needed: `EventStorageEngine` and its nested `AppendTransaction` only. Everything else used is
  public (`AppendCondition`, `ConsistencyMarker`, `SourcingCondition`, `TaggedEventMessage`,
  `AppendEventsTransactionRejectedException`, `ProcessingContext`, `ComponentRegistry.registerDecorator`,
  `ConfigurationEnhancer`, `RetryingCommandBus`, `RetryScheduler`/`RetryPolicy` family).
- Decorator order: lower value = applied to the delegate (inner). `DistributedCommandBus` `MIN+50`,
  `InterceptingCommandBus` `MIN+100`, default `RetryingCommandBus` `MIN+1000`.
- `APPEND_EVENTS` phase is 29000, between `PREPARE_COMMIT` (20000) and `COMMIT` (30000).
- Upstream-record candidates: public extension point for decision capture; `ConflictDiagnosingEventStorageEngine`
  missing `CompletionException` unwrap; dispatch-side retry cannot classify remote DCB conflicts.

## Origin

Synthesized from spikes: 001
Source files available in: sources/001-decision-record-capture/
