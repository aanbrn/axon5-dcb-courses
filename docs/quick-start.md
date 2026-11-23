# Quick start

This page starts the local runtime of the course system: a DCB-capable Axon Server and a PostgreSQL server, both
in containers. The domain services arrive in later phases and join the same compose project.

## Prerequisites

- A Docker engine with Docker Compose: colima or Docker Desktop. See [Tooling](tooling.md) for the details.
- [mise](https://mise.jdx.dev/) with the repository tools installed: run `mise install` once in the repository root.
  Docker alone is enough to start the runtime; mise and the JDK it provides are needed for the Gradle commands below.

No `.env` file and no Gradle run are needed in a fresh clone.

## Start

From the repository root:

```shell
docker compose up -d --wait
```

The command returns when Axon Server and PostgreSQL are healthy. The image tags and digests come from
`compose/versions.env`, which is generated from the version catalog and committed with the repository.

## Endpoints

Every port is published on the loopback interface only, so nothing is reachable from the network.

| Endpoint              | Address                                 | Notes                                                                       |
| --------------------- | --------------------------------------- | --------------------------------------------------------------------------- |
| Axon Server dashboard | `http://localhost:8024`                 | The DCB context `default` exists from the first start                       |
| Axon Server gRPC      | `localhost:8124`                        | Where host applications and tests connect                                   |
| PostgreSQL            | `localhost:5432`                        | Databases `query_service` and `automation_service`, one login role for each |
| Axon Server (compose) | `axonserver:8124` and `axonserver:8024` | Only from containers on the compose network                                 |

The development passwords are defaults, not secrets, and can be changed in `.env`; see `.env.example`. When another
PostgreSQL already holds port 5432 on your machine, set `POSTGRES_PORT` in `.env` to another host port.

## Axon Server licence

Axon Server runs without a licence for development, with a time limit:

- An Axon Server that is not registered with an AxonIQ Platform workspace shuts down about 12 hours after it starts.
- Every container restart starts a new window of 12 hours: `docker compose restart axonserver` is the way to get one.
  Recreating the container or wiping the volumes starts a new window as well.
- An AxonIQ Platform token lifts the limit. Create an environment in an AxonIQ Platform workspace, copy `.env.example`
  to `.env` (it is git-ignored), uncomment `AXONIQ_PLATFORM_AUTHENTICATION` and replace the placeholder with
  `<environment-id>:<token>`. Then run `docker compose up -d --wait` again.
- Never set the variable to an empty value. Axon Server then treats the Platform connection as configured, fails to
  start it and stays in the state "running" with dead HTTP and gRPC endpoints. Leave the line commented out or remove
  it to run without a token.

CI uses no token: its jobs are short-lived.

## Check the connection from inside the network

The `probe` service runs on request. It reaches Axon Server by its service name, on the health endpoint and on the gRPC
port, the way a service in the compose project will:

```shell
docker compose --profile probe run --rm probe
```

The command exits with status 0 when both are reachable.

## Stop

```shell
docker compose down
```

This keeps the data in the named volumes, so the next start continues where this one stopped. Add `-v` to remove the
volumes as well and start from empty databases and an empty event store:

```shell
docker compose down -v
```

## Build and test

Run these with the repository tools on `PATH` (see [Tooling](tooling.md)):

- `./gradlew build` builds and checks everything, including every test suite.
- `./gradlew test` runs the unit tests only and is the fast loop while developing.

The integration tests start their own Axon Server with Testcontainers. To run them against the compose Axon Server
instead, pass its address:

```shell
./gradlew :command-service:integrationTest -Pcourses.axonserver.external=localhost:8124
```
