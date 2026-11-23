---
id: "UP-0001"
product: "axon"
product-version: "5.4.2"
status: "reported"
workaround: true
upstream: ["https://github.com/AxonIQ/AxonFramework/issues/5326"]
reported: "2026-11-23"
updated: "2026-11-23"
---

# UP-0001: No public extension point to observe the conditional append of a decision

## Symptom

The command service records one decision record per command attempt: the criteria the decision sourced, the events it
saw, the append condition (criteria and consistency marker) that was sent to the store, the tagged events it appended,
the consistency marker after the commit and the verdict of the store, including the event that caused a conflict.
Axon Framework 5.4.2 has no public type that reports these facts. The public `EventStore` and `EventStoreTransaction`
show only some of them, and only through workarounds (see the reproduction). The one place where all of them meet is
the `@Internal` `EventStorageEngine`.

Impact on this repository: the command service decorates an `@Internal` type, so any Axon release may break the
decorator without a deprecation period. The permanent `DecisionCaptureIntegrationTest` makes such a break fail in the
pull request that brings the update.

## Minimal reproduction

Build the record from the public API only, on Axon Framework 5.4.2 with the in-memory or the Axon Server engine:

1. Decorate the public `EventStore`. `StorageEngineBackedEventStore.publish()` opens its own transaction and bypasses the
   decorator, so `publish` has to be overridden as well, or append-only decisions are not seen at all.
2. Read the append condition through `EventStoreTransaction.overrideAppendCondition` with an identity function. The
   decorator sees the condition before any later override by user code, not the condition that is finally sent.
3. Read the appended events through the transaction. They carry no tags at that level.
4. Read the consistency marker after the commit. `runOnAfterCommit` runs before the framework's own update and reads
   `ConsistencyMarker.ORIGIN`; only `appendPosition()` read in `whenComplete` gives the new position.
5. Ask for the event that caused a rejection. The public API offers none; the lookup needs a read of the engine beyond
   the consistency marker.

The same facts are available at one seam, the `EventStorageEngine`, which is `@Internal`:

```kotlin
registry.registerDecorator(EventStorageEngine::class.java, 0) { config, _, engine ->
    CapturingEngine(engine, config.getComponent(Recorder::class.java))
}
```

## Root cause

A design gap, not a defect. `DefaultEventStoreTransaction` collects the criteria of everything a unit of work sourced
and, in the `APPEND_EVENTS` phase, calls `eventStorageEngine.appendEvents(appendCondition, processingContext,
eventQueue)` ([line 231][transaction]). That call is the only place where the exact condition, the tagged events and
the verdict of the store come together, and `EventStorageEngine` carries `@Internal` ([line 51][engine]), which means
that it may change in any release. No public hook offers the same view.

[transaction]: https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/DefaultEventStoreTransaction.java#L231
[engine]: https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/EventStorageEngine.java#L51

## Workaround

`DecisionCaptureEngine` decorates the `EventStorageEngine` and reports one record per unit of work to a
`DecisionRecorder`. The repository accepts the `@Internal` dependency on condition that the safeguards stay in place:
the integration test described above, and the per-slice stamping of records as the fallback if the contract breaks.

- `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngine.kt`: the
  decorator, marked on the class.
- `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/CommandServiceConfigurationEnhancer.kt`: the
  registration of the decorator.

## Removal criteria

An Axon Framework release that offers a public API, not annotated `@Internal`, through which an application registers
an observer that receives, for every unit of work: the sourcing criteria, the append condition with its consistency
marker, the tagged events, the marker after the commit and the verdict of the store, including the conflicting event
of a rejection. A release that removes `@Internal` from the engine decorator contract and promises its stability
meets the criterion as well. The release notes or the API documentation of the release name the API.

## How to verify removal

Replace `DecisionCaptureEngine` with the public API, delete the decorator registration and both markers, then run
`mise x -- ./gradlew :command-service:integrationTest --tests '*DecisionCaptureIntegrationTest*'`. The test passes
unchanged, and `grep -rn "eventstore.EventStorageEngine" backend/command-service/src/main` prints nothing.

## Upstream issue draft

Target: `AxonIQ/AxonFramework`, as a feature request. Published as https://github.com/AxonIQ/AxonFramework/issues/5326 on 2026-11-23.

### Public extension point to observe the conditional append of a decision

#### Environment and versions

- Product and version: Axon Framework 5.4.2
- Related products and versions: Axoniq Framework 5.4.2 with `axon-server-connector`, Axon Server 2026.1.8 (DCB context)
- JDK, operating system and build tool: Temurin 25, Linux and macOS, Gradle 9.9.2

#### Steps to reproduce

1. Build an application on Axon Framework 5.4.2 with an event-sourced entity and a DCB event store.
2. Try to log, for every command attempt, the append condition that was sent to the store, the tags of the appended
   events, the consistency marker after the commit and, on a rejection, the event that caused it, using only types
   that are not annotated `@Internal`.

#### Expected behavior

A public hook, for example a listener on `EventStoreTransaction` or a documented decorator contract, reports these
facts, so that tooling such as an audit trail, a decision log or a debugging aid does not depend on `@Internal` types.

#### Actual behavior

The facts are spread over the public `EventStore` and `EventStoreTransaction`, each with a gap:

- `StorageEngineBackedEventStore.publish()` bypasses a decorator of the event store.
- `overrideAppendCondition` shows the condition before later overrides.
- Appended events carry no tags at that level.
- `runOnAfterCommit` reads `ConsistencyMarker.ORIGIN`, because it runs before the framework updates the marker.
- The event that caused a rejection is not available.

Everything is available in `EventStorageEngine.appendEvents(...)`, which is `@Internal`.

#### Minimal reproduction

```kotlin
class CapturingEngine(private val delegate: EventStorageEngine) : EventStorageEngine by delegate {
    override fun appendEvents(
        condition: AppendCondition,
        context: ProcessingContext?,
        events: List<TaggedEventMessage<*>>,
    ): CompletableFuture<EventStorageEngine.AppendTransaction<*>> {
        println("criteria=${condition.criteria()} marker=${condition.consistencyMarker()} events=${events.size}")
        return delegate.appendEvents(condition, context, events)
    }
}

registry.registerDecorator(EventStorageEngine::class.java, 0) { _, _, engine -> CapturingEngine(engine) }
```

Dispatch one command that appends a tagged event: the line shows everything that the public API cannot.

#### Analysis

`DefaultEventStoreTransaction` calls `eventStorageEngine.appendEvents(appendCondition, processingContext,
eventQueue)` in the `APPEND_EVENTS` phase
([source](https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/DefaultEventStoreTransaction.java#L231)).
The engine contract is the only seam that sees the final condition, the tags and the verdict.

#### Proposed fix

Either publish a small observer API next to `EventStoreTransaction` that is called with the final `AppendCondition`,
the tagged events, the marker after the commit and the outcome of the append, or promote the engine decorator contract
to public API with a stability promise. We can contribute a pull request once the preferred shape is agreed.
