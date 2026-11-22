# Spike Manifest

## Ideas

### phase2-runtime-dcb-foundations
Phase 2 resolves the high-risk unknowns before domain code (02-CONTEXT.md D-12): decision-record capture around Axon's
conditional append (D-13), Kotlin event tags with Jackson 3 and TypeID value-class IDs including sealed command results
(D-03..D-10), Axon Server in compose with a DCB context reachable from compose and the host plus the licence and token
behaviour (D-14, D-25, D-26), and Paketo images for the static frontend and a Boot service (D-35).

**Requirements:**

- Spikes run in the order 003 → 001 → 002 → 004 → 005 (author's choice); numbering reflects risk.
- Decision-record capture uses the `EventStorageEngine` decorator (variant A), accepting the `@Internal` API with the D-13 safeguards; conflict retries run handling-side.
- Spike code lives in `.planning/spikes/`, built by a standalone Gradle build whose versions come from the main catalog.

## Spikes

| # | Idea | Name | Type | Validates | Verdict | Tags |
|---|------|------|------|-----------|---------|------|
| 001 | phase2-runtime-dcb-foundations | decision-record-capture | comparison | Given AF 5.4.2 conditional append, when a central decorator wraps the append layer, then criteria, marker, events, outcome and retries are captured with no slice code | VALIDATED (A: @Internal engine decorator) | axon, dcb, decision-record, retry |
| 003 | phase2-runtime-dcb-foundations | axon-server-compose-dcb-hostname | standard | Given Axon Server 2026.1.8 in compose, when the DCB context is created at boot, then compose and host clients both connect unchanged | VALIDATED | axon-server, compose, dcb, hostname |
| 004 | phase2-runtime-dcb-foundations | axon-server-licence-restart-token | standard | Given unregistered Axon Server, when restarted with/without volumes, then the 12-hour limit behaviour and token delivery are known | VALIDATED | axon-server, licence, platform-token |
