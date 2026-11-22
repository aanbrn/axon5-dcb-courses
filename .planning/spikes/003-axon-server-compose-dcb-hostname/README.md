---
spike: 003
idea: phase2-runtime-dcb-foundations
name: axon-server-compose-dcb-hostname
type: standard
validates: "Given Axon Server 2026.1.8 in compose, when the DCB context is created at boot, then a client inside compose and a client on the host both connect without reconfiguration"
verdict: VALIDATED
related: [004, 001, 002]
tags: [axon-server, compose, dcb, hostname, healthcheck]
---

# Spike 003: Axon Server in Compose — DCB Context and Hostname

## What This Validates
Given Axon Server 2026.1.8 (`-nonroot` image) in compose, when the DCB context is created at boot, then an AF 5.4.2
Spring Boot client inside compose (`axonserver:8124`) and the same client on the host (`localhost:8124`) both connect,
append a tagged event and source it back by tag, with no per-scenario configuration.

## Research
- Property keys were taken from the server jar itself (the shipped `spring-configuration-metadata.json` is incomplete):
  `axoniq.axonserver.standalone-dcb`, `axoniq.axonserver.autocluster.{first,contexts,dcb}`,
  `axoniq.axonserver.devmode.enabled`, `axoniq.axonserver.hostname`, `axoniq.axonserver.internal-hostname`.
- `axoniq-testcontainer` 5.4.2 `AxonServerContainer` instead POSTs `/v2/cluster/init?dcb=true` after start
  (`AxonServerContainerUtils.initCluster`), with a cross-process file lock because two concurrent inits corrupt the
  single-node Raft group.
- Client routing (`axonserver-connector-java` 2026.1.1, `AxonServerManagedChannel.connectChannel`): the client asks the
  routing server for `PlatformInfo`; if `sameConnection` is true, or the advertised host/port equals the address it
  dialled, it reuses the channel. Only otherwise does it dial the advertised `hostname`.

| Approach | Mechanism | Pros | Cons | Status |
|----------|-----------|------|------|--------|
| `AXONIQ_AXONSERVER_STANDALONE_DCB=true` | native server config | one env var, no extra container, idempotent across restarts | not documented in the shipped metadata | **chosen, works** |
| init container `curl -X POST /v2/cluster/init?dcb=true` | REST | same as Testcontainers | extra service, must be idempotent, race on double init | not needed |
| `axoniq.axonserver.hostname=localhost` / split hostnames | advertised name | — | not needed: single node answers `sameConnection` | not needed |

## How to Run
```bash
../../gradlew -p .planning/spikes :003-probe:bootJar          # from repo root: ./gradlew -p .planning/spikes ...
cd .planning/spikes/003-axon-server-compose-dcb-hostname
docker compose up -d --wait axonserver
java -jar probe/build/libs/003-probe.jar                       # host client -> localhost:8124
docker compose --profile probe run --rm probe                  # compose client -> axonserver:8124
docker compose down -v
```

## What to Expect
Both runs print `PROBE RESULT ... events=2 types=[spike.ProbePinged#1, ...TerminalEventMessage#0.0.1]`.

## Investigation Trail
1. Inspected the image: entrypoint `java -jar ./axonserver.jar`, user `axonserver`, volumes `/axonserver/{config,data,
   events,exts,log,plugins,transformation}`, ports 8024/8124/8224, JDK 21.0.13.
2. Booted with `AXONIQ_AXONSERVER_STANDALONE_DCB=true` + `hostname: axonserver`: log shows
   `Creating DCB context: default` and `DCB append pipeline mode is DISRUPTOR`. No init container needed.
   Surprise: `GET /v1/public/context` still reports `"dcbContext": false` for `default` — the REST field is not a
   reliable signal; functional proof is that tag-based sourcing works.
3. Host probe: server advertises `hostName: axonserver` (unresolvable on the host) yet the client logs only
   `Requesting connection details from localhost:8124` and never `Connecting to [axonserver]` — `sameConnection` reuse.
4. Compose probe (`eclipse-temurin:25-jre` + mounted boot jar, `AXON_SERVERS=axonserver:8124`): works.
5. `/v1/public/me` reports `maxApplications: 1, maxContexts: 1, maxUsers: 2` in unregistered mode. Ran three apps with
   different `spring.application.name` (compose `probe`, host `second`, host `third`) concurrently: all appended and
   sourced. The limit is not enforced on client applications in 2026.1.8 dev mode (or counts something else).
6. Restart resilience: host client holding a connection across `docker compose restart axonserver` logs
   `Connection refused` retries every 2 s, reconnects via `localhost:8124` (`forceReconnectThroughServers=true` default
   goes back through the configured routing server, never the advertised name) and publishes successfully.
7. Healthcheck: image `/bin/sh` is dash (no `/dev/tcp`), but `curl` exists; `curl -fsS http://localhost:8024/actuator/health`
   works, and `depends_on: condition: service_healthy` gates clients.
8. Recreating the container with kept volumes works because `hostname:` is fixed; the node name is persisted in the
   control DB, so a random container hostname would orphan the stored contexts.

## Results
**VALIDATED.**
- DCB context: `AXONIQ_AXONSERVER_STANDALONE_DCB=true` (plus `AXONIQ_AXONSERVER_DEVMODE_ENABLED=true` for dev). No init
  container.
- Hostname: fixed compose `hostname: axonserver`; no `hostname`/`internal-hostname` overrides; clients use
  `axon.axonserver.servers` = `axonserver:8124` in compose and `localhost:8124` on the host. Both work unchanged.
- Healthcheck: `curl -fsS http://localhost:8024/actuator/health`; dependants use `service_healthy`.
- Side findings for the build:
  - AF5 stores the qualified type as `namespace.Name#version` (`spike.ProbePinged#1`).
  - Sourcing returns a trailing `TerminalEventMessage` entry; consumers must filter it.
  - Two `EventStorageEngine` beans exist (the engine and `SnapshotStore`); injecting by type is ambiguous.
  - AF5 runs an update checker that phones home; disable with `AXONIQ_UPDATE_CHECKER_DISABLED=true`
    (`axoniq.update-checker.disabled`) in compose and CI.
  - Boot logs a harmless WARN about `springDataPageJacksonModule` when Spring Data is absent.
  - The Axon resolved classpath already pins `protobuf-java` 4.36.2 and pulls Jackson 2 (`com.fasterxml`) transitively
    besides Jackson 3 — see spike 002.
