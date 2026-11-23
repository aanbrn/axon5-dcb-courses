---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0010: Test levels are JVM test suites, and `check` runs every one of them

## Context and Problem Statement

The project needs distinct test levels from the first module on (TEST-11), with shared helpers (TEST-12) and the agreed
libraries (TEST-14). The levels differ in what they need: DCB decisions can run in memory, a service needs its Spring
context and a database, and conditional appends need a real Axon Server. Gradle wires only the built-in `test` task into
`check`, so a suite that is added later is silently outside the done criterion unless something wires it.

The author and the agents that execute plans also need one command that means "done", and CI needs a layout that is both
fast and complete.

## Decision Drivers

- `check` and `build` must be the single done criterion for the author, GSD executors and CI, so that nobody reports green
  without the integration tests.
- The boundary between levels must be a real infrastructure boundary, not a convention.
- Tests that share infrastructure must be safe to run in parallel.
- The image of every container comes from the version catalog, pinned by digest (INFRA-09).
- Every level must count toward coverage (TEST-11).

## Considered Options

- Three suites, `test`, `componentTest` and `integrationTest`, from a `courses.test-suites` convention, with `check`
  running all of them (chosen).
- H2 in PostgreSQL mode for component tests.
- A `quickCheck` and a `fullCheck` aggregate next to `check`, or `integrationTest` outside `check` and run explicitly.
- One CI job for everything, or one job per suite.
- The shared Axon Server container holder of `axoniq-testcontainer` (`SharedAxonServerContainer`) as the singleton.
- Purging the shared Axon Server between tests.
- Adding Kover (coverage) in Phase 3, as RESEARCH first proposed.

## Decision Outcome

Chosen option: "three JVM test suites from one convention, all wired into `check`", because it makes the infrastructure
boundary the level boundary and the cheapest correct done criterion one command. The decisions D-15 to D-22 and TEST-11
settle the details:

- **The suites (D-15).** `test` is unit level: no Spring and no containers, and DCB decisions run on `AxonTestFixture` in
  memory. `componentTest` is one service with its Spring context, a real PostgreSQL in Testcontainers and Axon in
  memory. `integrationTest` runs against a real Axon Server container, with conditional appends and cross-service flows.
  The boundary between component and integration is Axon Server. A fourth suite, `architectureTest`, holds the Konsist
  rules of `:tooling:architecture` (D-17), and the build logic keeps its TestKit tests in `functionalTest`.
- **H2 rejected.** H2 in PostgreSQL mode lacks `COLLATE "C"`, `ON CONFLICT` upserts, `jsonb` and dialect-faithful Flyway
  migrations, and it gives false confidence.
- **`check` and `build` run every suite (D-19).** The convention adds every suite's test task to `check`, and orders them
  with `mustRunAfter`. There is no `quickCheck` and no `fullCheck`. A human who wants a fast loop runs `./gradlew test` or
  `componentTest`. The Gradle build cache keeps unchanged integration tests from re-running.
- **Two parallel CI jobs (D-20).** `build` runs `./gradlew build checkDependencyLicences -x integrationTest` and
  `integration-test` runs `./gradlew integrationTest`; `ci-success` aggregates both, so together they are `check`.
- **Shared infrastructure.** Testcontainers singletons live in a small `:tooling:test-support` module (D-18), because
  they belong to no module's types. Helpers for a module's own types live in that module's `testFixtures`: ID fixtures and
  `Arb` generators in the kernel, builders in events, the capture fixtures in the command service. The module owns its
  Axon Server singleton, started from the digest-pinned catalog image, instead of the `SharedAxonServerContainer` holder
  of `axoniq-testcontainer`, whose image is a hard-coded floating `latest` that bypasses the pin (UP-0007). Reuse across
  JVMs is opt-in and local only (`testcontainers.reuse.enable`), and CI never enables it.
- **Isolation by fresh data (D-16).** Tests on the shared Axon Server create their own entities from fresh TypeIDs and
  never purge the store. That is parallel-safe because DCB criteria select events by tag, and a global check such as name
  uniqueness uses unique data.
- **Fork settings per level (D-22).** Unit tests keep several forks. `componentTest` and `integrationTest` use one fork
  per module, so the container singletons and the Spring context cache are reused. In-fork JUnit parallelism waits for
  measurements.
- **Libraries (D-21).** Only what a suite uses is added: `kotest-property` and the assertion library of Kotest under the
  JUnit runner (the Kotest runner is not used, so JUnit and `AxonTestFixture` stay the one style), Testcontainers and
  Awaitility if needed. MockK and `AxonTestFixture` arrive in Phase 3 with the first decisions.
- **Naming and style.** A test class ends with the suffix of its suite: `*Test`, `*ComponentTest`, `*IntegrationTest`,
  `*ArchitectureTest`, `*FunctionalTest`. A test function has a block body, because an expression-bodied test that returns
  a value is not run by JUnit and is skipped without a signal.
- **Coverage (TEST-11).** Kover is applied inside `courses.test-suites` with no test task excluded and no class filter, so
  `test`, `componentTest` and `integrationTest` all feed the module's coverage report. Aggregation across modules,
  thresholds and the CI reporting are left to TEST-08 in Phase 3. Leaving Kover itself to Phase 3, as RESEARCH first
  proposed, was rejected because TEST-11 is a Phase 2 requirement.

### Consequences

- Good, because there is one command, `./gradlew check`, that runs every level, and an executor cannot report green
  without the integration tests.
- Good, because the CI jobs run in parallel and still cover everything, and a missing suite wiring would show as a
  missing task.
- Good, because tests do not interfere through shared data, so parallel runs on one Axon Server are safe.
- Bad, because `check` needs a Docker daemon and takes longer than a unit-only loop. The remedy is the documented fast
  loop, not a second aggregate.
- Bad, because the JVM Test Suite API is still `@Incubating`, so a Gradle upgrade can change it. Renovate's pull request
  for that upgrade fails the build first.
- Bad, because the global state of the shared store is never cleaned, so a test that needs a pristine store must start its
  own container.

### Confirmation

- `TestSuitesFunctionalTest` runs TestKit builds that prove `check` depends on every suite, that a failing integration
  test fails `check`, that the suites run in order and that the infrastructure suites use one fork, and that Kover reports
  the code each suite reaches. `ContainerImageInputsFunctionalTest` proves that a new catalog image re-runs the tests.
- `TestNamingArchitectureTest` enforces the suffix rule and the block bodies.
- `koverXmlReport` of any module that applies the convention lists the code reached by every suite; no module needs
  coverage configuration of its own.
- `ci.yml` has the two jobs and the `ci-success` aggregate, which makes both required through ADR-0001.

## Pros and Cons of the Options

### Three suites from one convention

- Good, because the level is a build concept with its own sources, dependencies, fork settings and task.
- Good, because a new module gets the levels by applying one plugin.
- Bad, because it relies on an incubating Gradle API.

### H2 in PostgreSQL mode

- Good, because it starts in-process, with no container.
- Bad, because it lacks the PostgreSQL features the read models use and gives false confidence, which is worse than a slower
  test.

### `quickCheck` and `fullCheck` aggregates, or `integrationTest` outside `check`

- Good, because a fast loop would exist as a named task.
- Bad, because there would be two meanings of "done" and executors would pick the cheap one. A human can run a single
  suite task.

### One CI job, or one job per suite

- A single job would serialize the unit and integration levels, and a job per suite would repeat the checkout and Gradle
  setup for every level. Two parallel jobs split the cost where the cost is, at the Axon Server boundary.

### The shared container holder of `axoniq-testcontainer`

- Good, because it is the library's own mechanism for sharing one container.
- Bad, because its image is the floating `latest`, which can move to a release the project has not verified, even one that
  is marked "do not use" (UP-0007).

### Purging the shared Axon Server between tests

- Good, because every test would start from an empty store.
- Bad, because a purge would delete events that a test running in parallel still reads. Fresh IDs give the isolation
  without that interference (D-16).

### Kover in Phase 3

- Good, because it defers one plugin.
- Bad, because TEST-11 asks for coverage per level in this phase, and adding the plugin later would leave the first suites
  without numbers.

## More Information

- Decisions: D-15 to D-22, D-11 (test ID sources) and TEST-11, TEST-12, TEST-14.
- Upstream record: [UP-0007](../upstream/0007-shared-testcontainer-latest-image.md), reported as AxonIQ/axoniq-framework#812.
- Revisit the fork settings when measurements justify in-fork parallelism, the level boundaries when the Axon Server
  boundary stops being the cost driver, and the convention when the JVM Test Suite API leaves incubation.
