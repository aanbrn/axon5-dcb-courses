---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0006: Decision records are captured by one decorator on the event storage engine

## Context and Problem Statement

The event explorer and the concurrency demo (DEMO-01) need a decision record for every command attempt: the command, the
criteria the decision sourced, the events it saw, the append condition and consistency marker that were sent, the events
it appended with their tags, the outcome (committed with the new position, nothing appended, conflict with the
conflicting event, or failure) and the attempt number across retries. It must answer a question such as "the 31st
student was rejected because the decision saw these 30 subscriptions up to position 1042".

Slices must contain no capture code, and the record must be complete: completeness matters more than avoiding an
`@Internal` Axon API (decision D-13). Axon Framework 5.4.2 has no public extension point for this, so the question is
which seam to decorate. Spike 001 built and ran two candidates against the in-memory engine and a real Axon Server.

## Decision Drivers

- No capture code in any slice.
- The exact append condition that was sent and the verdict of the store, not a reconstruction.
- The tags of the appended events, the consistency marker after the commit and the conflicting event on a rejection.
- Retries visible as consecutive attempts of one command.
- A break caused by an Axon update must fail loudly in the pull request that brings the update.

## Considered Options

- Variant A: decorate the `@Internal` `EventStorageEngine` (chosen).
- Variant B: decorate the public `EventStore` and `EventStoreTransaction`.
- Fallback, not built: stamp each slice explicitly through a shared helper, with a Konsist rule that requires the call.

## Decision Outcome

Chosen option: "variant A, a decorator on the `EventStorageEngine`", because it is the only seam where everything the
record needs meets, and it needs no workaround to read it. `DecisionCaptureEngine` in the command service records one
`DecisionRecord` per attempt and reports it to a `DecisionRecorder` component, which the application must register. The
record holds text and numbers only, so Phase 3 can persist and render it without depending on Axon.

How the pieces fit:

- **Capture.** One `ConfigurationEnhancer` registers the decorator through `ComponentRegistry.registerDecorator`. In
  Spring Boot a `ConfigurationEnhancer` bean is discovered, so nothing changes for Phase 3. A recorder that fails is
  logged and swallowed, because capture must never change the outcome of a decision.
- **The conflicting event.** The engine diagnoses a conflict itself: it sources the first event beyond the marker and
  attaches it to the record. Axon's own `ConflictDiagnosingEventStorageEngine` is not used, because on the Axon Server
  path the engine's `commit()` relays the rejection wrapped in a `CompletionException` and the Axon class checks
  `instanceof` without unwrapping (UP-0002).
- **Retries on the handling side.** A `RetryingCommandBus` is registered as a `CommandBus` decorator at order
  `Int.MIN_VALUE + 10`, with a filtering policy on `AppendEventsTransactionRejectedException`, a maximum number of
  attempts and exponential backoff. That order puts it below the `DistributedCommandBus`
  (`Int.MIN_VALUE + 50`), so it wraps the local segment and sees the real, typed exception. Axon's default retry sits at
  the dispatch side and does not retry on Axon Server: the conflict reaches the dispatcher as
  `CommandExecutionException` carrying AXONIQ-4005 and only the text of the original, the type erased (UP-0003). No
  `RetryScheduler` component is registered, because that would add the outer, dispatch-side retry as well.
- **Creation commands.** A command that sources nothing appends with `AppendCondition.none()`, so there is no
  consistency check at all, and two concurrent creations of the same ID both succeed. A creation slice that needs a
  "does not exist yet" guarantee must source its own criteria, for example the course tag.

### Consequences

- Good, because slices never see capture, and the record has the exact condition, the tags, the position after the
  commit and the conflicting event.
- Good, because every retry is a full record with its own marker and outcome, tied together by the command identifier.
- Bad, because `EventStorageEngine` and its nested `AppendTransaction` are `@Internal` ("may undergo breaking changes"),
  so any Axon release can break the decorator without a deprecation period.
- Bad, because the retry placement depends on the order constant of the `DistributedCommandBus`, an implementation detail
  of Axon.
- Bad, because decisions that source nothing leave a gap that the decorator cannot close: it records them, but no
  invariant protects them.

### Confirmation

- `DecisionCaptureIntegrationTest` in the `integrationTest` suite, which the `integration-test` CI job runs, is the
  safeguard. It runs 12 concurrent subscriptions against a 3-seat course on a real Axon Server and asserts exactly 3
  `Subscribed` and 9 `Rejected(course-full)`, a conflict that names `courses.StudentSubscribed#1`, and retry attempts
  preceded by the conflict of the previous attempt. The Renovate pull request that moves Axon fails here when the
  decorator breaks.
- The in-memory `DecisionCaptureTest` checks the record contents, and the plan that built it paired each test with a
  mutation that proves it fails when the decorator is broken.
- Upstream record [UP-0001](../upstream/0001-decision-capture-extension-point.md) asks AxonIQ for a public extension
  point, and carries the markers at the workaround sites.

## Pros and Cons of the Options

### Variant A: the `EventStorageEngine` decorator

- Good, because it sees the exact `AppendCondition` sent, the exact verdict of the store, the appended events with tags,
  the marker after the commit and, with its own diagnosis, the conflicting event.
- Good, because it covers append-only commands and conditional appends alike.
- Bad, because the interface is `@Internal`.

### Variant B: the public `EventStore` decorator

- Good, because it uses public API only.
- Bad, because it needed four workarounds: `publish()` bypasses the decorator and must be overridden, the resolved
  condition is visible only through an identity `overrideAppendCondition` and before later user overrides, the marker
  after the commit is readable only through `appendPosition()` in `whenComplete` (`runOnAfterCommit` runs too early and
  reads `ORIGIN`), and the conflicting event needs an engine-level read.
- Bad, because even with them the appended events carry no tags and the conflicting event is missing.

### Per-slice stamping with a Konsist rule

- Good, because it depends on no Axon internals.
- Bad, because every slice would carry capture code, which D-13 rules out, and a forgotten call would silently lose a
  record. It stays documented as the fallback if the `@Internal` seam disappears.

## More Information

- Spike: `.planning/spikes/001-decision-record-capture/README.md`, verdict VALIDATED with variant A.
- Decisions: D-13 (central decorator, accept `@Internal` with two safeguards, per-slice stamping as the fallback), D-43
  (the wiring lives in `backend/command-service`).
- Upstream records: [UP-0001](../upstream/0001-decision-capture-extension-point.md) (public extension point),
  [UP-0002](../upstream/0002-conflict-diagnosis-completion-exception.md) (the diagnosing engine does not unwrap
  `CompletionException`) and [UP-0003](../upstream/0003-remote-conflict-classification.md) (remote conflicts are not
  classifiable on the dispatch side).
- Revisit this decision when Axon publishes a supported extension point (switch the decorator to it and close UP-0001),
  or when an Axon update changes `EventStorageEngine` so that the capture test cannot be repaired cheaply (then evaluate
  per-slice stamping).
