---
spike: 004
idea: phase2-runtime-dcb-foundations
name: axon-server-licence-restart-token
type: standard
validates: "Given unregistered Axon Server, when the container restarts with and without its volumes, then we know whether the 12-hour limit resets, and how a Platform token is supplied"
verdict: VALIDATED
related: [003]
tags: [axon-server, licence, platform-token, compose]
---

# Spike 004: Axon Server Licence — Restart Behaviour and Token Delivery

## What This Validates
Given Axon Server 2026.1.8 without a licence, when the container is restarted in place, recreated with kept volumes, or
recreated with fresh volumes, then the scheduled shutdown time tells whether the 12-hour limit resets. And: which
setting carries the AxonIQ Platform token, and how compose passes it when it is optional.

## Research
- The server logs the deadline explicitly at boot and every few minutes:
  `[LICENSE] No license detected. Running in unregistered mode. Trial access is limited to 12 hours. Register at
  platform.axoniq.io to continue. Shutdown scheduled at <instant>.`
- `javap` on `io.axoniq.axonserver.licensing.LicenseValidityEnforcer` (strings obfuscated): the deadline is a
  `final Instant` computed in the constructor (`now + 12h`), nothing is read from or written to disk, and at the
  deadline it calls `ConfigurableApplicationContext.close()`. A Platform configuration (`AxoniqConsoleProperties`)
  switches it to `Axoniq Platform is configured. Awaiting license retrieval...`.
- `AxoniqConsoleProperties` (`axonserver-console-2026.1.8.jar`) is `@ConfigurationProperties("axoniq.platform")` with
  `host` (default `axonserver.console.axoniq.io`), `port`, `secure`, `authentication`, `contexts`, `initializeDelay`.
  So the token is `axoniq.platform.authentication`, i.e. env `AXONIQ_PLATFORM_AUTHENTICATION`, in the form
  `<id>:<token>` (the code splits on `:`).

## How to Run
```bash
cd .planning/spikes/003-axon-server-compose-dcb-hostname   # reuses spike 003's compose
docker compose up -d --wait axonserver
docker compose logs axonserver | grep 'Shutdown scheduled'
docker compose restart axonserver            # then: docker compose logs --since <t> ... | grep 'Shutdown scheduled'
docker compose down && docker compose up -d --wait axonserver
docker compose down -v && docker compose up -d --wait axonserver
```

## What to Expect
Each boot prints a new `Shutdown scheduled at` = boot time + 12 h.

## Investigation Trail
| Boot (UTC) | Variant | Shutdown scheduled at |
|------------|---------|-----------------------|
| 11:45:28 | first boot | 23:45:28 |
| 11:56:26 | `down` + `up`, volumes kept | 23:56:26 |
| 11:56:37 | `down -v` + `up`, fresh volumes | 23:56:37 |
| 11:56:56 | `restart` (same container) | 23:56:56 |

(A first reading of the in-place restart showed the old deadline; that was the previous boot's log line still in the
container log. Re-measured with `docker compose logs --since`.)

Token delivery:
1. `AXONIQ_PLATFORM_AUTHENTICATION: "${AXONIQ_PLATFORM_AUTHENTICATION:-}"` (the obvious compose idiom) with the variable
   unset → the container gets an **empty** value → server logs `Axoniq Platform is configured` and fails with
   `IndexOutOfBoundsException: Index: 1, Size: 1` at `AxoniqConsoleCoordinationService.kt:69`. The JVM does not exit:
   the container stays `running` while HTTP and gRPC are dead. Upstream-record candidate (empty value should mean
   "not configured", or at least fail fast).
2. Fix: the variable must be **absent** when unused. `env_file: [{path: ./platform.env, required: false}]` (gitignored
   by the existing `.env.*` rule) passes it only when the file exists. Without the file the server boots in
   unregistered mode, healthy.
3. Not tested on purpose: a real token (author-verified per D-14) and any fake or malformed token (that would be probing
   licence enforcement, which is out of scope).

## Results
**VALIDATED.**
- Every restart variant resets the 12-hour window, including an in-place `restart`. The deadline lives only in memory.
- At the deadline the server closes its Spring context. Whether the process then exits (so `restart: unless-stopped`
  would restart it and open a new window) is not observed here (no 12-hour soak, per D-14); the quick start should say
  "restart Axon Server (`docker compose restart axonserver`) to get a new 12-hour window, or set a Platform token".
- Token: env `AXONIQ_PLATFORM_AUTHENTICATION=<id>:<token>` delivered through an optional `env_file`, never through
  `${VAR:-}` interpolation. Add a healthcheck so a mis-set token shows up as `unhealthy` instead of a silent zombie.
- D-25 adjustment: the gitignored file holding the token should be a dedicated file passed with `env_file`
  (e.g. `axonserver.env` / `platform.env`), with an `.example` sibling, rather than compose's interpolation `.env`.
