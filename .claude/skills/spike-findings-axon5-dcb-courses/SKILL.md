---
name: spike-findings-axon5-dcb-courses
description: Implementation blueprint from spike experiments. Requirements, proven patterns, and verified knowledge for building axon5-dcb-courses. Auto-loaded during implementation work.
---

<context>
## Project: axon5-dcb-courses

**phase2-runtime-dcb-foundations** — Phase 2 resolves the high-risk unknowns before domain code (02-CONTEXT.md D-12):
decision-record capture around Axon's conditional append (D-13), Kotlin event tags with Jackson 3 and TypeID
value-class IDs including sealed command results (D-03..D-10), Axon Server in compose with a DCB context reachable from
compose and the host plus the licence and token behaviour (D-14, D-25, D-26), and Paketo images for the static
frontend and a Boot service (D-35).

Spike sessions wrapped: 2026-11-22
</context>

<requirements>
## Requirements

### phase2-runtime-dcb-foundations

- Spikes run in the order 003 → 001 → 002 → 004 → 005 (author's choice); numbering reflects risk.
- Decision-record capture uses the `EventStorageEngine` decorator (variant A), accepting the `@Internal` API with the
  D-13 safeguards; conflict retries run handling-side.
- Local Docker (colima) runs with explicit `network.dns` and the classic image store (`containerd-snapshotter: false`);
  author-approved during spike 005.
- Spike code lives in `.planning/spikes/`, built by a standalone Gradle build whose versions come from the main catalog.
</requirements>

<findings_index>
## Feature Areas

| Area | Reference | Key Finding |
|------|-----------|-------------|
| Axon Server runtime | references/axon-server-runtime.md | `AXONIQ_AXONSERVER_STANDALONE_DCB=true` + fixed `hostname` serve compose and host clients; every restart resets the 12 h window; token only via optional `env_file` (empty value zombies the server) |
| Decision capture and retries | references/decision-capture-and-retries.md | `@Internal` `EventStorageEngine` decorator captures everything incl. conflicting event; retry must wrap the local segment (`CommandBus` order `MIN+10`); creation without sourcing has no consistency check |
| Kotlin ids, tags, serialization | references/kotlin-ids-tags-serialization.md | Default tag resolver writes raw UUIDs for value classes (silent DCB failure) → `KotlinTagResolver`; entities registered with `UUID` id type + axon-kotlin; own TypeID codec; sealed results via Jackson mix-ins |
| Container images | references/container-images.md | Custom `nginx.conf` + `BootBuildImage` for the UI; `BPE_PREPEND_JAVA_TOOL_OPTIONS` not `DEFAULT`; `BPE_*` invisible in `Config.Env`; colima DNS and Docker 29 image store must be fixed locally |

## Source Files

Original spike source files are preserved in `sources/` for complete reference (`sources/settings.gradle.kts` and
`sources/build.gradle.kts` are the standalone spike build).
</findings_index>

<metadata>
## Processed Spikes

- 001-decision-record-capture
- 002-kotlin-tags-jackson-typeid
- 003-axon-server-compose-dcb-hostname
- 004-axon-server-licence-restart-token
- 005-paketo-web-and-boot-images
</metadata>
