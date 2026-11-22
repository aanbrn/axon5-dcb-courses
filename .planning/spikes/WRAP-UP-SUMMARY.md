# Spike Wrap-Up Summary

**Date:** 2026-11-22
**Spikes processed:** 5
**Feature areas:** Axon Server runtime, decision capture and retries, Kotlin ids/tags/serialization, container images
**Skill output:** `./.claude/skills/spike-findings-axon5-dcb-courses/`

## Processed Spikes
| # | Name | Type | Verdict | Feature Area |
|---|------|------|---------|--------------|
| 001 | decision-record-capture | comparison | VALIDATED (variant A) | Decision capture and retries |
| 002 | kotlin-tags-jackson-typeid | standard | PARTIAL | Kotlin ids, tags, serialization |
| 003 | axon-server-compose-dcb-hostname | standard | VALIDATED | Axon Server runtime |
| 004 | axon-server-licence-restart-token | standard | VALIDATED | Axon Server runtime |
| 005 | paketo-web-and-boot-images | comparison | VALIDATED | Container images |

## Key Findings
- DCB context in compose needs only `AXONIQ_AXONSERVER_STANDALONE_DCB=true` and a fixed `hostname`; compose and host
  clients connect unchanged and reconnect after restarts.
- The unregistered 12-hour limit restarts with every container restart; the Platform token is
  `AXONIQ_PLATFORM_AUTHENTICATION=<id>:<token>` via an optional `env_file` — an empty value leaves a zombie server.
- Decision records come from a decorator on the `@Internal` `EventStorageEngine`; conflict retries must run on the
  handling side (`RetryingCommandBus` at `CommandBus` order `MIN+10`) because Axon Server erases the conflict type for
  the dispatcher; the conflicting event has to be diagnosed by the decorator (`ConflictDiagnosingEventStorageEngine`
  misses `CompletionException`).
- Creation commands that source nothing append with `AppendCondition.none()` — no consistency check.
- Value-class ids need Kotlin-aware wiring: a `KotlinTagResolver` (the default writes raw UUIDs, silently breaking
  criteria), entities registered with the `UUID` id type, and axon-kotlin for nullable `@InjectEntity`.
- No mature JVM TypeID library; an own codec passes all 30 official vectors.
- Paketo: custom `nginx.conf` built through Gradle `BootBuildImage`; `BPE_PREPEND_JAVA_TOOL_OPTIONS` for JVM defaults;
  `BPE_*` invisible in `Config.Env` (labels document them); health checks need container env `THC_*`.
- Local blockers fixed with approval: colima DNS (AAAA never answered) and Docker 29's containerd image store
  (moby#52193). Both are `doctor` candidates; the image-store issue can hit CI runners too.
- Upstream-record candidates: decision-capture extension point; `ConflictDiagnosingEventStorageEngine` unwrap;
  dispatch-side retry classification; value-class tags; value-class `@TargetEntityId`; empty Platform token.
