---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0005: Axon Server runs unregistered by default, and the Platform token is an optional pass-through variable

## Context and Problem Statement

The system needs a DCB-capable Axon Server on every contributor's machine and in CI (INFRA-05). Axon Server 2026.1 is one
artifact with a licence check: an instance that is not registered with an AxonIQ Platform workspace runs for a limited
time, and a Platform token lifts the limit. Three questions had to be answered before the compose file and the quick
start could be written:

- How long does an unregistered server run, and does a restart reset the limit?
- How does the token reach the container, and how does compose pass it when it is optional?
- What does a contributor without a token experience?

The first answer is the author's own observation (decision D-14): an unlicensed Axon Server in compose stops working after
about 12 hours, and with a Platform token it connects and works without the limit. The spike therefore verified only the
two open points, the effect of restarts and the delivery of the token. It ran no multi-hour soak test.

## Decision Drivers

- A fresh clone must start with `docker compose up -d --wait`, with no `.env` file and no secret (D-25).
- A token is a credential: it must stay out of the repository, out of logs and out of the shell history of the quick
  start.
- A wrong or empty configuration must show up as a failing check, not as a server that looks alive and is dead.
- CI runs are short, so they need no token.

## Considered Options

- Pass the variable through from the root `.env` with a bare compose entry, `- AXONIQ_PLATFORM_AUTHENTICATION` (chosen).
- An `env_file` per service with `required: false`, the arrangement spike 004 first proved to work.
- A committed default of an empty value, the common compose idiom `${AXONIQ_PLATFORM_AUTHENTICATION:-}`.
- A compose profile `platform` that adds the token, weighed during the discussion of D-25.

## Decision Outcome

Chosen option: "bare pass-through from the root `.env`", because it is the only option that keeps the variable absent
when it is unused, needs no second file next to the `.env` that compose reads anyway, and puts the credential in the one
untracked file contributors already know.

The facts behind the decision:

- **The window.** An Axon Server without a licence logs `No license detected. Running in unregistered mode. Trial access
is limited to 12 hours.` and `Shutdown scheduled at <boot + 12 h>`. The limit is 12 hours after the server
  starts. This agrees with the author's observation (D-14) and with the product documentation.
- **Every restart opens a new window.** Spike 004 booted the same image four ways and every boot scheduled its shutdown
  12 hours after that boot: a first start, `down` and `up` with the volumes kept, `down -v` and `up` with fresh volumes,
  and an in-place `docker compose restart axonserver`. The deadline lives only in the memory of the process.
  Decompiling `LicenseValidityEnforcer` shows a deadline computed in the constructor as now plus 12 hours, nothing read
  from or written to disk, and a call that closes the Spring context at the deadline. Whether the JVM then exits, so
  that a restart policy would open a new window by itself, was not observed.
- **The token.** It is `AXONIQ_PLATFORM_AUTHENTICATION=<environment-id>:<token>`, the environment form of the property
  `axoniq.platform.authentication`; the code splits on the colon. The value is read from the untracked `.env` in the
  repository root. `.env.example` carries the line commented out, with an explanation (D-47).
- **Absent, not empty.** Compose passes the variable as a bare list entry, so an undefined variable never reaches the
  container. An empty value does reach it, and Axon Server then treats the Platform as configured, fails with an
  `IndexOutOfBoundsException` while it starts the connection and stays in the state "running" with dead HTTP and gRPC
  endpoints. That is upstream record UP-0006, and the reason the example line is commented out instead of empty, and why
  `doctor` fails on a defined but empty value.
- **CI uses no token.** The jobs live a few minutes, far below the window.

### Consequences

- Good, because a fresh clone starts without any configuration, and a contributor who needs a long-running server adds
  one line to `.env`.
- Good, because no credential is ever tracked, interpolated into a committed file or passed through a second file.
- Bad, because a server without a token stops itself after about 12 hours. The quick start says so and names the
  remedies: restart the container for a new window, or set the token.
- Bad, because the safety of the optional variable rests on a compose behaviour (a bare entry is dropped when the variable
  is unset) and not on a contract of Axon Server. A shell that exports the variable as an empty string defeats it, and
  `doctor` only reads `.env`, not the shell.
- Bad, because a real token was not exercised by the spike (D-14 treats it as author-verified), and no soak test ran, so
  the behaviour at the deadline itself is read from the code and the log, not observed.

### Confirmation

- `scripts/doctor.sh` has a `platform-token` check: `ok` when the variable is set in `.env`, `warn` when it is absent and
  `fail` when it is defined but empty. It classifies the state and never prints the value.
- The compose healthcheck of the `axonserver` service turns a zombie into `unhealthy`, and the `Compose smoke test` CI
  job starts the stack without a token and without a `.env` file.
- `docs/quick-start.md` documents the window and the token, and links to this ADR.

## Pros and Cons of the Options

### Bare pass-through from the root `.env`

- Good, because an undefined variable is absent in the container, which is the state Axon Server handles.
- Good, because compose reads the root `.env` without any declaration, so no extra file and no `env_file` entry exist.
- Bad, because an empty definition still reaches the container, so the guard is a check (`doctor`) and a comment, not a
  mechanism.

### An `env_file` per service with `required: false`

- Good, because spike 004 showed that an absent file leaves the variable absent and the server healthy.
- Bad, because it needs a dedicated, git-ignored file and an `.example` sibling next to the `.env` that compose already
  reads, and the file would be one more place a token can leak from.
- Bad, because a listed `.env` would hand every variable in it to the container, so the file must be a different one.

### A compose profile `platform`

- Good, because the token would be opt-in by name.
- Bad, because every command that uses the token needs the profile flag, and the token would still have to arrive through
  the environment, so the profile adds a switch without solving the delivery.

### A committed default of an empty value

- Good, because it is the idiom everyone knows and it makes the variable visible in the compose file.
- Bad, because with the variable unset it puts an empty value into the container, which is exactly the state that leaves
  Axon Server running with dead endpoints (spike 004, UP-0006).

## More Information

- Spike: `.planning/spikes/004-axon-server-licence-restart-token/README.md`.
- Decisions: D-14 (author-verified licence behaviour), D-25 (optional token in `.env`), D-47 (commented example line, bare
  pass-through, `doctor` fails on an empty definition).
- Upstream record: [UP-0006](../upstream/0006-empty-platform-token.md), an empty token counts as configured. The report
  for the AxonIQ forum is left to the author.
- Revisit this decision when AxonIQ changes the unregistered window or the licence model of Axon Server, when UP-0006 is
  fixed upstream (the commented line and the `doctor` failure can then relax), or when a service of this repository needs
  a long-running server in CI.
