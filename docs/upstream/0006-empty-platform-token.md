---
id: "UP-0006"
product: "image-axonserver"
product-version: "2026.1.8"
status: "reported"
workaround: true
upstream:
  [
    "https://discuss.axoniq.io/t/axon-server-2026-1-8-empty-axoniq-platform-authentication-is-treated-as-configured-and-crashes-the-startup-runner/6851",
  ]
reported: "2026-11-25"
updated: "2026-11-25"
---

# UP-0006: An empty AxonIQ Platform token leaves Axon Server running with dead HTTP and gRPC endpoints

## Symptom

Axon Server 2026.1.8 treats `AXONIQ_PLATFORM_AUTHENTICATION` as "the AxonIQ Platform is configured" as soon as the
variable is defined, even when its value is empty. It then logs `[LICENSE] Axoniq Platform is configured. Awaiting
license retrieval...` and fails in a startup runner:

```text
ERROR ... o.s.boot.SpringApplication : Application run failed
java.lang.IndexOutOfBoundsException: Index: 1, Size: 1
    at java.base/java.util.Collections$SingletonList.get(Collections.java:5180)
    at io.axoniq.console.client.axonserver.AxoniqConsoleCoordinationService.run(AxoniqConsoleCoordinationService.kt:69)
```

The gateway and cluster server stop, and what the process does next depends on the configuration. In three runs of the
reproduction below:

- without `AXONIQ_AXONSERVER_STANDALONE_DCB` the process exits with code 1, which a restart policy or a healthcheck
  would notice;
- with `AXONIQ_AXONSERVER_STANDALONE_DCB=true`, the setting this repository uses to create its DCB context, the
  container stayed `running` with HTTP (8024) and gRPC (8124) dead in two of three runs, and exited in the third. A
  container in this state shows `running` in `docker ps` and only a healthcheck reveals it (spike 004).

The empty value is easy to produce by accident: the Compose idiom `${AXONIQ_PLATFORM_AUTHENTICATION:-}` and a
`.env` line `AXONIQ_PLATFORM_AUTHENTICATION=` both define the variable with an empty value.

## Minimal reproduction

On a machine with Docker, no repository needed:

```sh
docker run -d --name axonserver-empty-token \
  -e AXONIQ_PLATFORM_AUTHENTICATION= \
  -e AXONIQ_AXONSERVER_STANDALONE_DCB=true \
  axoniq/axonserver:2026.1.8-nonroot
sleep 60
docker inspect -f '{{.State.Status}} exit={{.State.ExitCode}}' axonserver-empty-token
docker logs axonserver-empty-token 2>&1 | grep -E 'IndexOutOfBoundsException|Application run failed'
docker rm -f axonserver-empty-token
```

Expected with the defect: the log lines appear, and the status is `exited exit=1` or, more insidiously, `running exit=0`
while `docker exec axonserver-empty-token curl -fsS http://localhost:8024/actuator/health` fails. Remove the first `-e`
line and the server starts in unregistered mode and answers the health check.

## Root cause

Axon Server is closed source, so the cause comes from the stack trace and from `javap` on the shipped classes (spike
004), not from a source link. `AxoniqConsoleProperties` in `axonserver-console-2026.1.8.jar` is a
`@ConfigurationProperties("axoniq.platform")` class. The environment variable binds to `authentication`, and Spring
Boot binds an empty string rather than leaving the property unset. The coordination service then splits the value on
`:` into `<environment-id>:<token>` and reads index 1, which does not exist in the one-element list that an empty string
produces.

Two defects combine: an empty value counts as configured, and the failure in the startup runner is neither fatal for
the process in every configuration nor reported through the health endpoint.

## Workaround

This repository never lets an empty value reach the container and fails loudly when one would.

- `compose/infra.yaml`: the bare `- AXONIQ_PLATFORM_AUTHENTICATION` entry passes the variable through only when `.env`
  or the shell defines it, and the comment above it carries `UPSTREAM: UP-0006`. The container healthcheck makes a
  zombie visible as `unhealthy`.
- `.env.example`: the commented example line and the instruction never to set an empty value, carrying
  `UPSTREAM: UP-0006`.
- `scripts/doctor.sh`: the `platform-token` check fails on a defined but empty value (four spellings) and prints the
  state without ever printing the value, carrying `UPSTREAM: UP-0006`.

## Removal criteria

A released Axon Server in which an empty `AXONIQ_PLATFORM_AUTHENTICATION` means "not configured" (or fails fast with
a clear message and a non-zero exit in every configuration), confirmed by the verification below and named in the
release notes or the answer to the upstream report. The `doctor` failure can stay as a cheap guard even then.

## How to verify removal

Run the minimal reproduction against the new image tag. The defect is gone when the log has no
`IndexOutOfBoundsException`, the log says `Running in unregistered mode`, and
`docker exec axonserver-empty-token curl -fsS http://localhost:8024/actuator/health` succeeds, in at least three
consecutive runs with `AXONIQ_AXONSERVER_STANDALONE_DCB=true`.

## Upstream issue draft

Axon Server is not open source and has no public issue tracker, so the target is the AxonIQ forum
(`https://discuss.axoniq.io`, category for Axon Server) or AxonIQ support. A search of the forum on 2026-11-23 found no
report of this behavior; the one topic about the Platform variable concerns a Raft pre-vote loop with a valid token on
a fresh volume, which is a different defect.

Publication status (2026-11-25): the author approved the text below and submitted it to the forum in category Axon
Server on 2026-11-25. Discourse held it for moderation because the account was new, and the topic became public on
2026-11-25 after forum staff reviewed the account and released it (see the forum posting rule in ADR-0013, "Publication
(UPS-03)"). The topic is
[Axon Server 2026.1.8: empty AXONIQ_PLATFORM_AUTHENTICATION is treated as configured and crashes the startup runner](https://discuss.axoniq.io/t/axon-server-2026-1-8-empty-axoniq-platform-authentication-is-treated-as-configured-and-crashes-the-startup-runner/6851).
The first post (`https://discuss.axoniq.io/raw/6851/1`) matches the draft below word for word, apart from line wrapping;
the forum title adds the `Axon Server 2026.1.8:` prefix to the draft heading. The text below therefore stays as the
published text. The same day the author edited the post to add the AI-assistance disclosure line at its end, as in
UP-0013.

````markdown
# Empty `AXONIQ_PLATFORM_AUTHENTICATION` is treated as configured and crashes the startup runner

## Environment and versions

- Product and version: Axon Server 2026.1.8 (image `axoniq/axonserver:2026.1.8-nonroot`)
- Related products and versions: none
- Operating system and runtime: Docker 29.x on a Linux VM, started with `docker run` or Docker Compose

## Steps to reproduce

1. Start the image with the variable defined and empty, and the standalone DCB setting:
   `docker run -d --name axonserver-empty-token -e AXONIQ_PLATFORM_AUTHENTICATION= -e AXONIQ_AXONSERVER_STANDALONE_DCB=true axoniq/axonserver:2026.1.8-nonroot`
2. Wait 60 seconds.
3. Run `docker inspect -f '{{.State.Status}} exit={{.State.ExitCode}}' axonserver-empty-token` and
   `docker logs axonserver-empty-token`.

## Expected behavior

An empty value is treated like an absent one: Axon Server starts in unregistered mode. If an empty value is
considered a configuration error, the process fails fast with a message that names the property, and always exits with
a non-zero code.

## Actual behavior

The log says `[LICENSE] Axoniq Platform is configured. Awaiting license retrieval...`, followed by:

```text
ERROR ... o.s.boot.SpringApplication : Application run failed
java.lang.IndexOutOfBoundsException: Index: 1, Size: 1
    at java.base/java.util.Collections$SingletonList.get(Collections.java:5180)
    at io.axoniq.console.client.axonserver.AxoniqConsoleCoordinationService.run(AxoniqConsoleCoordinationService.kt:69)
```

The gateway and cluster server then log that they stopped. In two of three runs with the standalone DCB setting the
container kept the state `running` while ports 8024 and 8124 refused connections; without that setting it exited with
code 1.

## Minimal reproduction

The `docker run` command in step 1; it needs no volumes, no network and no other configuration.

## Analysis

The value is split on `:` into `<environment-id>:<token>` and index 1 is read without a length check, so an empty
string, which yields a one-element list, fails. The empty environment variable is bound as an empty string, so the
Platform integration counts as configured. A process that survives the failed runner while its endpoints are dead
defeats restart policies and needs a healthcheck to be noticed.

## Proposed fix

Treat a blank `axoniq.platform.authentication` as unset, and validate the `<environment-id>:<token>` shape up front
with an error message that names the property. Make a failed startup runner always terminate the process.

_This report was prepared with an AI coding agent; I reviewed and approved the findings and the text before posting._
````
