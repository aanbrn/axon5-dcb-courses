---
id: "UP-0001"
product: "axon"
product-version: "5.4.2"
status: "reported"
workaround: true
upstream: ["https://github.com/AxonIQ/AxonFramework/issues/5326"]
reported: "2026-11-23"
updated: "2026-11-24"
---

# UP-0001: No public extension point to observe the conditional append of a decision

## Symptom

The command service records one decision record per command attempt: the criteria the decision sourced, the events it
saw, the append condition (criteria and consistency marker) that was sent to the store, the tagged events it appended,
the consistency marker after the commit and the verdict of the store, including the event that caused a conflict.
Axon Framework 5.4.2 has no public type that reports these facts. The public `EventStore` and `EventStoreTransaction`
show only some of them, and only through workarounds (see the reproduction). The one place where all of them meet is
the `@Internal` `EventStorageEngine`.

Impact on this repository: the command service decorates `EventStorageEngine`, which is `@Internal`. According to the
maintainer response, `@Internal` here signals that the type may change, it is not experimental, and the engine stays in
Axon Framework 5 without being removed. A minor release can still change its contract without a deprecation period. The
permanent `DecisionCaptureIntegrationTest` makes such a change fail in the pull request that brings the update.

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
that it may change in any release. The decoration mechanism itself is public, because every component registered with
Axon Framework can be decorated through the configurer
([documentation](https://docs.axoniq.io/axon-framework-reference/5.4/configuration/configurer/#decorators)). The gap is
that the one component that sees all the facts is an `@Internal` type, and no public type offers the same view.

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

The maintainer response applies to this criterion as follows. The statement that `EventStorageEngine` stays in Axon
Framework 5 is not a stability promise for the decorator contract, so it does not meet the criterion on its own. A
`debug` log statement in the `EventStorageEngine`, the angle the maintainer suggests for a pull request, does not meet
it either, because the capture needs the facts as data in the application, not as log text. An opt-in decorator
definition shipped by Axon Framework, which the maintainer also accepts, meets the criterion when it hands the listed
facts to application code. The issue is on the Axon Framework 5.5.0 milestone (on 2026-11-24), set on the expectation of
a pull request from this repository and to be moved otherwise.

## How to verify removal

Replace `DecisionCaptureEngine` with the public API, delete the decorator registration and both markers, then run
`mise x -- ./gradlew :command-service:integrationTest --tests '*DecisionCaptureIntegrationTest*'`. The test passes
unchanged, and `grep -rn "eventstore.EventStorageEngine" backend/command-service/src/main` prints nothing.

## Upstream issue draft

Target: `AxonIQ/AxonFramework`, as a feature request. Published as https://github.com/AxonIQ/AxonFramework/issues/5326 on 2026-11-23.

Maintainer response on 2026-11-23 ([comment](https://github.com/AxonIQ/AxonFramework/issues/5326#issuecomment-7102785005)):
the issue is re-classified as an enhancement, although it was filed like a bug. An observer API is redundant in the
maintainer's view, because every component registered with Axon Framework can be decorated
([documentation](https://docs.axoniq.io/axon-framework-reference/5.4/configuration/configurer/#decorators)). The
suggested angle for a pull request is a `debug` log statement with all the information in the `EventStorageEngine`; an
opt-in decorator definition that users can enable is acceptable as well. `@Internal` on `EventStorageEngine` is not
experimental: the concept stays in Axon Framework 5 and is not going to be removed, and the annotation only signals
that the type may change and calls for care. The issue is placed on 5.5.0, expecting a pull request from the reporter;
the milestone is adjusted otherwise.

Status on 2026-11-24: open, milestone "Release 5.5.0", labels "Priority 4: Would" and "Ideal for Contribution".

Open decision for the author: upstream would welcome a pull request that adds a `debug` log statement with all the
information to the `EventStorageEngine` or an opt-in decorator definition. This repository has not decided whether to
contribute it; until it does, the 5.5.0 milestone may move.

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
