---
id: "UP-0002"
product: "axon"
product-version: "5.4.2"
status: "reported"
workaround: true
upstream: ["https://github.com/AxonIQ/AxonFramework/issues/5327"]
reported: "2026-11-23"
updated: "2026-11-24"
---

# UP-0002: ConflictDiagnosingEventStorageEngine does not unwrap CompletionException, so it never diagnoses on Axon Server

## Symptom

`ConflictDiagnosingEventStorageEngine` is Axon's own decorator for the case this repository needs: after an
`AppendEventsTransactionRejectedException` it sources the first event beyond the consistency marker and attaches it to
the exception, so that a rejection names the event that caused it. It works on the in-memory engine. On Axon Server it
never enriches the exception: `AppendEventsTransactionRejectedException.conflictingEvent()` stays empty, with no error
and no log line.

In the spike that found this, all 43 commit failures of a concurrent run on Axon Server 2026.1.8 reached the decorator
as a `CompletionException` caused by the `AppendEventsTransactionRejectedException`. Axon Framework 5.4.2 and Axoniq
Framework 5.4.2 were used.

## Minimal reproduction

The mechanism needs no Axon at all. A dependent stage that follows a composed failure sees the failure wrapped:

```java
var rejection = new IllegalStateException("rejected");

CompletableFuture.<String>failedFuture(rejection)
    .exceptionallyCompose(f -> {
        System.out.println("direct failedFuture sees: " + f.getClass().getSimpleName());
        return CompletableFuture.failedFuture(f);
    });

CompletableFuture.<String>failedFuture(new RuntimeException("remote"))
    .exceptionallyCompose(f -> CompletableFuture.<String>failedFuture(rejection))
    .exceptionallyCompose(f -> {
        System.out.println("relayed stage sees: " + f.getClass().getSimpleName()
            + " caused by " + f.getCause().getClass().getSimpleName());
        return CompletableFuture.failedFuture(f);
    });
```

On JDK 25 this prints `direct failedFuture sees: IllegalStateException` and `relayed stage sees: CompletionException
caused by IllegalStateException`.

With Axon, on a DCB context of Axon Server 2026.1.8:

1. Register `ConflictDiagnosingEventStorageEngine(engine)` as a decorator of the `EventStorageEngine`.
2. Run two concurrent units of work that source the same tag criteria and then append an event with that tag, so that
   the second commit is rejected.
3. Read `conflictingEvent()` of the `AppendEventsTransactionRejectedException` at the end of the cause chain. It is
   empty. On the in-memory engine, where the rejection is thrown directly by `appendEvents`, it holds the event.

## Root cause

The commit of the Axon Server engine maps the transport failure and relays the rejection through
`exceptionallyCompose` ([lines 389-407][engine]). Every stage that follows receives that failure wrapped in a
`CompletionException`. `DiagnosingAppendTransaction.commit()` continues with `exceptionallyCompose(failure ->
diagnose(...))` ([line 166][diagnosing]), and `diagnose` begins with `failure instanceof
AppendEventsTransactionRejectedException` ([line 103][diagnosing]) without looking at the cause chain, so the check is
false and the failure is re-thrown unchanged. The in-memory engine rejects inside `appendEvents`, where the failure
arrives unwrapped, which is why the decorator appears to work there.

The class has been available since 5.3.2, is `@Internal` and is not wired by default.

A related, closed report concerns the same commit path ([AxonIQ/axoniq-framework#299][related]): it asks that only
genuine condition failures map to `AppendEventsTransactionRejectedException`, which the 5.4.2 code does. It does not
address the wrapping described here, so this record is not a duplicate of it.

[engine]: https://github.com/AxonIQ/axoniq-framework/blob/axoniq-5.4.2/connector/axon-server-connector/src/main/java/io/axoniq/framework/axonserver/connector/event/AxonServerEventStorageEngine.java#L389-L407
[diagnosing]: https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/ConflictDiagnosingEventStorageEngine.java#L103
[related]: https://github.com/AxonIQ/axoniq-framework/issues/299

## Workaround

`DecisionCaptureEngine` does its own diagnosis: it searches the whole cause chain for the rejection, on both paths (the
in-memory engine rejects in `appendEvents`, Axon Server in `commit()`), sources the first event beyond the marker and
re-raises the original failure.

- `backend/command-service/src/main/kotlin/io/github/aanbrn/courses/command/capture/DecisionCaptureEngine.kt`: the
  `diagnose` function.

## Removal criteria

An Axon Framework release in which `ConflictDiagnosingEventStorageEngine`, or its public successor, finds the
rejection in the cause chain, so that a rejected append on Axon Server carries its conflicting event. The release notes
or the changed `diagnose` method show it. Upstream plans the fix for Axon Framework 5.4.3 (milestone on 2026-11-24), so
the release notes of that version are the first place to check.

## How to verify removal

In a scratch test on the Axon release, wrap the engine of an Axon Server container with
`ConflictDiagnosingEventStorageEngine`, provoke a rejection as in the reproduction and assert that `conflictingEvent()`
is present. Then let the capture engine rely on Axon's class, delete the `diagnose` function and its marker, and run
`mise x -- ./gradlew :command-service:integrationTest --tests '*DecisionCaptureIntegrationTest*'`: every conflict
record still names the conflicting event.

## Upstream issue draft

Target: `AxonIQ/AxonFramework`, as a bug report. Published as https://github.com/AxonIQ/AxonFramework/issues/5327 on 2026-11-23.

Maintainer response on 2026-11-23 ([comment](https://github.com/AxonIQ/AxonFramework/issues/5327#issuecomment-7105876689)):
the maintainer agrees that "this should definitely be fixed". The priority is Could, because
`ConflictDiagnosingEventStorageEngine` is typically not used in production, and the fix is wanted for 5.4.3, the
upcoming patch release.

Status on 2026-11-24: open, milestone "Release 5.4.3", label "Priority 3: Could".

### ConflictDiagnosingEventStorageEngine never diagnoses a rejection that arrives as a CompletionException

#### Environment and versions

- Product and version: Axon Framework 5.4.2
- Related products and versions: Axoniq Framework 5.4.2 (`axon-server-connector`), Axon Server 2026.1.8 (DCB context)
- JDK, operating system and build tool: Temurin 25, Linux and macOS, Gradle 9.9.2

#### Steps to reproduce

1. Register `ConflictDiagnosingEventStorageEngine` as a decorator of the `EventStorageEngine` of an application that
   uses the Axon Server connector with a DCB context.
2. Run two concurrent units of work that source the same tag criteria and then append an event with that tag.
3. Inspect the `AppendEventsTransactionRejectedException` of the unit of work that loses.

#### Expected behavior

The exception carries the conflicting event: the class documentation says that upon an
`AppendEventsTransactionRejectedException` the decorator identifies an event that caused the rejection.

#### Actual behavior

`conflictingEvent()` is empty. The failure that reaches `diagnose` is a `CompletionException` whose cause is the
`AppendEventsTransactionRejectedException`, so `failure instanceof AppendEventsTransactionRejectedException` is
false and the failure is re-thrown unchanged. On the in-memory engine the same code enriches the exception.

#### Minimal reproduction

```java
var rejection = new IllegalStateException("rejected");
CompletableFuture.<String>failedFuture(new RuntimeException("remote"))
    .exceptionallyCompose(f -> CompletableFuture.<String>failedFuture(rejection))
    .exceptionallyCompose(f -> {
        System.out.println(f.getClass().getSimpleName() + " caused by " + f.getCause().getClass().getSimpleName());
        return CompletableFuture.failedFuture(f);
    });
```

Prints `CompletionException caused by IllegalStateException`: a stage that follows a composed failure sees it wrapped.

#### Analysis

`AxonServerEventStorageEngine` relays the mapped rejection through `exceptionallyCompose`
([source](https://github.com/AxonIQ/axoniq-framework/blob/axoniq-5.4.2/connector/axon-server-connector/src/main/java/io/axoniq/framework/axonserver/connector/event/AxonServerEventStorageEngine.java#L389-L407)),
and `ConflictDiagnosingEventStorageEngine.diagnose` checks the type of the failure without unwrapping
([source](https://github.com/AxonIQ/AxonFramework/blob/axon-5.4.2/eventsourcing/src/main/java/org/axonframework/eventsourcing/eventstore/ConflictDiagnosingEventStorageEngine.java#L103)).

#### Proposed fix

Look for the `AppendEventsTransactionRejectedException` in the cause chain of the failure before the type check, and
re-fail with the enriched copy. A test that wraps the rejection in a `CompletionException` guards it. We can
contribute a pull request.
