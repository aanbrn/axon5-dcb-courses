---
spike: 001
idea: phase2-runtime-dcb-foundations
name: decision-record-capture
type: comparison
validates: "Given AF 5.4.2 conditional append, when a central decorator wraps the append layer, then it captures criteria, consistency marker, sourced and appended events, outcome and retries with no capture code in slices"
verdict: VALIDATED
related: [003, 002]
tags: [axon, dcb, decision-record, decorator, retry, internal-api]
---

# Spike 001: Decision-Record Capture via a Central Decorator

## What This Validates
Given AF 5.4.2 with DCB conditional appends, when one central decorator wraps the layer that performs the append,
then for every decision attempt it records (DEMO-01): the command, the sourcing criteria, the events the decision saw,
the append criteria and consistency marker actually sent, the appended events with tags, the outcome (committed with
the new position / nothing appended / conflict with the conflicting event / failure), and the attempt number across
retries. Slices contain no capture code. Compared two seams: **A** — the `@Internal` `EventStorageEngine`; **B** — the
public `EventStore` / `EventStoreTransaction`.

## Research
Read from the 5.4.2 sources (Maven Central `-sources.jar`):

- `DefaultEventStoreTransaction`: `source()` accumulates the `AppendCondition` criteria (OR of everything sourced) in
  the `ProcessingContext`; the consistency marker comes from the sourced stream; in phase `APPEND_EVENTS` (29000) it
  calls `EventStorageEngine.appendEvents(condition, context, events)`; `commit()` runs in `COMMIT`; `afterCommit()`
  yields the new `ConsistencyMarker`. With no sourcing and no entity the condition is `AppendCondition.none()`
  (`AnyEvent`, `INFINITY`) — **no consistency check at all**; tag-derived criteria are used only for
  `EntityMetamodel.CREATE_WITHOUT_LOAD`.
- `ConflictDiagnosingEventStorageEngine` (`@Internal`, not wired by default) is Axon's own decorator at exactly this
  seam: on rejection it sources the first event beyond the marker and attaches it to the exception.
- Decoration: `ComponentRegistry.registerDecorator(type, order, ComponentDecorator)` (public); in Spring Boot a
  `ConfigurationEnhancer` bean is discovered by `SpringComponentRegistry` (verified with the spike-003 probe:
  `ENHANCER-DECORATOR saw append: criteria=AnyEvent marker=INFINITY events=1`).
- Retries: `RetryingCommandBus` + `RetryScheduler`/`RetryPolicy` (public, `axon-messaging` `core.retry`). Opt-in: the
  default decorator applies it only if a `RetryScheduler` component exists. `AppendEventsTransactionRejectedException`
  extends `AxonNonTransientException`, so a plain policy never retries conflicts; a `FilteringRetryPolicy` is needed.
- Order semantics (`DistributedCommandBusConfigurationEnhancer`): lower order = applied to the delegate (inner).
  `DistributedCommandBus` = `MIN+50`, `InterceptingCommandBus` = `MIN+100`, `RetryingCommandBus` default = `MIN+1000`
  (outermost, i.e. dispatch side).

Stability annotations (`@Internal` = "may undergo breaking changes"):

| Type | Annotation | Needed by |
|------|-----------|-----------|
| `EventStorageEngine`, `AppendTransaction` | **@Internal** | A |
| `ConflictDiagnosingEventStorageEngine`, `InterceptingEventStore` | **@Internal** | neither (see below) |
| `EventStore`, `EventStoreTransaction` (`overrideAppendCondition`, `onAppend`, `appendPosition`) | public | B |
| `AppendCondition`, `ConsistencyMarker`, `SourcingCondition`, `TaggedEventMessage`, `AppendEventsTransactionRejectedException`, `EventCriteria`, `Tag`, `MessageStream`, `ProcessingContext`/`ProcessingLifecycle`, `Message.fromContext` | public | A, B |
| `ComponentRegistry.registerDecorator`, `ConfigurationEnhancer`, `RetryingCommandBus`, `RetryScheduler`, `AsyncRetryScheduler`, `RetryPolicy` family | public | both |

| Approach | Seam | Pros | Cons |
|----------|------|------|------|
| A engine decorator | `EventStorageEngine` (`@Internal`) | exact `AppendCondition` sent; exact store verdict; appended events *with tags*; marker after commit from `afterCommit`; conflicting event via own diagnosis | `@Internal` interface |
| B store decorator | `EventStore` + `EventStoreTransaction` (public) | public API only | `publish()` bypasses the decorator (must be overridden); resolved condition only via an identity `overrideAppendCondition` (sees it *before* later user overrides); no tags on appended events; marker after commit only via `appendPosition()` at `whenComplete`; no conflicting event (needs engine-level `source` beyond the marker) |

## How to Run
```bash
./gradlew -p .planning/spikes :001-capture:test     # from repo root; needs Docker (Testcontainers + Axon Server 2026.1.8)
```

## What to Expect
8 tests pass: {A, B} × {in-memory, Axon Server} × {single-decision record contents, 12 concurrent subscriptions on a
3-seat course with retries}. Output prints each `DecisionRecord` and the conflict statistics.

## Investigation Trail
1. Matrix first run: in-memory showed **0 conflicts** — commands sent from one thread run synchronously. Switched to a
   12-thread pool: in-memory 17 conflicts / 29 attempts; Axon Server 30 conflicts / 42 attempts, max attempt 4,
   exactly 3 `Subscribed` and 9 `Rejected(course-full)`.
2. Axon Server run first failed with `ConversionException ... byte[] to spike.SubscribeResult ... abstract types`:
   command results cross the wire even inside one JVM. `@JsonTypeInfo(use = NAME)` on the sealed interface fixed it
   (jackson-module-kotlin discovers sealed subtypes). Detailed in spike 002.
3. Default `RetryingCommandBus` (`MIN+1000`, dispatch side) did not retry on Axon Server: the conflict arrives as
   `CommandExecutionException(AxonServerNonTransientRemoteCommandHandlingException AXONIQ-4005)` with only the text of
   the original — the type is erased. **Fix: register `RetryingCommandBus` as a `CommandBus` decorator at order
   `MIN+10`**, below `DistributedCommandBus`, so it wraps the local segment on the handling side and sees the real
   `AppendEventsTransactionRejectedException`. Do not register a `RetryScheduler` component (that adds the outer one).
4. B missed append-only decisions (`CreateCourse`): `StorageEngineBackedEventStore.publish()` calls its own
   `transaction()`. Fixed by overriding `publish()` to route through the decorator first.
5. B reported `markerAfterCommit=ORIGIN`: its `runOnAfterCommit` ran before Axon's own after-commit update. Fixed by
   reading `appendPosition()` in `whenComplete`.
6. Conflicting event: wrapping with `ConflictDiagnosingEventStorageEngine` worked in memory but gave `null` on Axon
   Server. Logged commit failures: **43 × `CompletionException` caused by `AppendEventsTransactionRejectedException`** —
   the Axon Server engine's `commit()` is an `exceptionallyCompose` relay, so dependants see a wrapped exception, and
   `ConflictDiagnosingEventStorageEngine.diagnose` checks `instanceof` without unwrapping. **Upstream defect
   candidate.** A now does the same diagnosis itself with unwrapping, on both paths (in-memory rejects in
   `appendEvents()`, Axon Server in `commit()`). Every conflict record now names the conflicting event, e.g.
   `spike.StudentSubscribed#1:{"courseId":"course-…","studentId":"s1"}`.
7. Domain side-finding: a creation command that does not source anything appends with `AppendCondition.none()`, so two
   concurrent `CreateCourse` for the same id both succeed. Creation slices must source their own criteria (e.g. the
   course tag) to get a "does not exist yet" guarantee.

## Results
**VALIDATED — choose A (engine decorator), accept the `@Internal` `EventStorageEngine` per D-13.**
- Only A captures everything DEMO-01 asks for without workarounds: the exact condition sent, the tags, the new
  position, and the conflicting event. B is all-public but needs four workarounds and still lacks tags and the
  conflicting event. The only `@Internal` types A needs are `EventStorageEngine` and its nested `AppendTransaction`.
- Retries are captured as consecutive attempts of the same command identifier (the retry re-dispatches the same
  `CommandMessage`), each attempt a full record with its own marker and outcome.
- Wiring for the build: one `ConfigurationEnhancer` bean per command service registers (1) the capture decorator on
  `EventStorageEngine` and (2) a handling-side `RetryingCommandBus` at `CommandBus` order `MIN+10` with a
  `FilteringRetryPolicy` on `AppendEventsTransactionRejectedException`, `MaxAttemptsPolicy` and exponential backoff.
- Safeguards per D-13: the permanent capture test (A × Axon Server, conflict + retry + conflicting event) fails loudly
  on an Axon update; upstream records: (a) a public extension point for decision capture, (b)
  `ConflictDiagnosingEventStorageEngine` does not unwrap `CompletionException` on the Axon Server path, (c) the
  dispatch-side `RetryingCommandBus` cannot classify remote DCB conflicts (type erased to AXONIQ-4005).
