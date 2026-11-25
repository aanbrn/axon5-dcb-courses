# Axon Server Runtime (Compose, DCB Context, Hostname, Licence)

## Requirements

- Spike code lives in `.planning/spikes/`, built by a standalone Gradle build whose versions come from the main catalog.
- Local Docker (colima) runs with explicit `network.dns` and the classic image store (`containerd-snapshotter: false`).

## How to Build It

1. **Service definition** (Axon Server 2026.1.8, `-nonroot` image, pin by digest in the real build):

   ```yaml
   axonserver:
     image: docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot
     hostname: axonserver            # fixed: the node name is persisted in the control DB
     environment:
       AXONIQ_AXONSERVER_STANDALONE_DCB: "true"   # creates the DCB `default` context at boot
       AXONIQ_AXONSERVER_DEVMODE_ENABLED: "true"
     env_file:
       - path: ./axonserver.env      # gitignored; holds AXONIQ_PLATFORM_AUTHENTICATION=<id>:<token>
         required: false
     healthcheck:
       test: ["CMD", "curl", "-fsS", "-o", "/dev/null", "http://localhost:8024/actuator/health"]
       interval: 5s
       timeout: 3s
       retries: 30
       start_period: 10s
     ports: ["8024:8024", "8124:8124"]
     volumes: [as-data:/axonserver/data, as-events:/axonserver/events, as-log:/axonserver/log]
   ```

2. **Clients** need no hostname tricks: `axon.axonserver.servers=axonserver:8124` inside compose,
   `localhost:8124` from the IDE. A single node answers `sameConnection`, so the client never dials the advertised
   `axonserver` name; after a server restart it reconnects through the configured routing server
   (`forceReconnectThroughServers=true`, the default).
3. Dependants use `depends_on: {axonserver: {condition: service_healthy}}`.
4. Disable the AF5 update checker in compose and CI: `AXONIQ_UPDATE_CHECKER_DISABLED=true`
   (`axoniq.update-checker.disabled`); it phones home by default.
5. Licence: without a token the server logs `Shutdown scheduled at <boot + 12 h>` and closes its Spring context at
   that instant. **Every restart resets the window** (in-place `restart`, recreate with volumes, fresh volumes). The
   quick start says: restart Axon Server for a new window, or put a Platform token in `axonserver.env`.
6. Tests use `AxonServerContainer("…:2026.1.8-nonroot").withDevMode(true).withDcbContext(true)` (it POSTs
   `/v2/cluster/init?dcb=true` with a cross-process lock).

## What to Avoid

- `AXONIQ_PLATFORM_AUTHENTICATION: "${AXONIQ_PLATFORM_AUTHENTICATION:-}"`: an **empty** value makes the server treat
  the Platform as configured and fail with `IndexOutOfBoundsException` (`AxoniqConsoleCoordinationService.kt:69`);
  the container stays `running` with dead HTTP/gRPC. The variable must be absent, not empty. A healthcheck turns that
  into `unhealthy`.
- An init container calling `/v2/cluster/init` — not needed with `STANDALONE_DCB`, and a double init corrupts the
  single-node Raft group.
- A shell `/dev/tcp` healthcheck: the image's `/bin/sh` is dash. `curl` exists.
- Trusting `GET /v1/public/context` `dcbContext`: it reports `false` even for the DCB context. Prove DCB functionally
  (tag-based sourcing).
- Injecting `EventStorageEngine` by type in Spring: two beans (engine and `SnapshotStore`) — qualify by name.
- A random container hostname: recreating the container orphans the stored contexts.
- Probing licence enforcement with fake tokens.

## Constraints

- Unregistered mode reports `maxApplications: 1, maxContexts: 1, maxUsers: 2`, but three differently named apps
  connected and appended concurrently — the application limit is not enforced in 2026.1.8.
- Token format is `<id>:<token>` (the server splits on `:`); env name `AXONIQ_PLATFORM_AUTHENTICATION`
  (`@ConfigurationProperties("axoniq.platform")`, default host `axonserver.console.axoniq.io`).
- Stored event type format: `namespace.Name#version` (e.g. `courses.CourseCreated#1`); sourcing returns a trailing
  `TerminalEventMessage` entry that consumers must filter.
- Not observed: whether the process exits at the 12-hour deadline (no soak, per D-14).
- Upstream-record candidate: empty Platform token should mean "not configured" or fail fast.

## Origin

Synthesized from spikes: 003, 004
Source files available in: sources/003-axon-server-compose-dcb-hostname/, sources/004-axon-server-licence-restart-token/
