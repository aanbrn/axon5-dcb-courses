---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0009: Backend code lives under `backend/` with flat Gradle project names and contracts split by message kind

## Context and Problem Statement

Phase 1 laid the repository out by role and left the backend layout open. Phase 2 creates the first backend modules, so the
layout has to be fixed before the paths spread into CI filters, Renovate, REUSE, IDE run configurations and the
documentation. Decision D-01 amends the Phase 1 decision (D-03) and calls itself costly to reverse, and the cheapest point
to change it is now, because only `platform/` and `tooling/` existed.

Four questions followed:

- Where does backend code live, and how are the directories and Gradle projects named?
- How are the message types (commands, events, queries) packaged, and what do they depend on?
- How much framework may a contracts module carry?
- Where does the Axon wiring proven by the spikes live while no Spring Boot service exists?

## Decision Drivers

- The CQRS boundaries must be visible in the module dependency graph.
- Directory names equal the compose service names and image names, so one name identifies a service everywhere.
- Image names derive from `<rootProject.name>-<project.name>` (CICD-10), so a Gradle project name is also an image name.
- The contracts are shared by every service and by the BFF, so a framework in them would reach every consumer.

## Considered Options

- `backend/` directories with flat Gradle project names mapped by `projectDir` (chosen), versus nested Gradle project paths
  such as `:contracts:kernel`, versus a flat top-level layout without a `backend/` directory.
- Contracts split by message kind into four modules (chosen), versus one contracts module with a kernel and packages whose
  boundaries only Konsist enforces, versus modules by domain area (course, student, subscription).
- Framework annotations as `compileOnly` metadata in the contracts (chosen), versus pure Kotlin contracts with names and tags
  registered in the services, versus deciding by spike.
- The Axon wiring in `backend/command-service`, a plain library in Phase 2 (chosen), versus keeping it in the spike directory
  until the Spring Boot command service exists.

## Decision Outcome

Chosen option: "`backend/` directories with flat project names, mapped by `projectDir`", together with four contracts
modules, annotation-only framework use and the wiring in a plain library, because each choice keeps one concern in one
place and makes the dependency graph readable.

- **Layout (D-01).** `backend/contracts/{kernel,commands,events,queries}` and the service directories
  `backend/command-service`, `backend/query-service`, `backend/automation-service` and `backend/bff` carry the names of
  the compose services and images. `platform/`, `build-logic/`, `frontend/`, `docs-site/` and `tooling/` stay at the top
  level. The shared Jackson module is `backend/serialization-jackson`.
- **Flat names.** Gradle projects are `:contracts-kernel`, `:contracts-commands`, `:contracts-events`,
  `:contracts-queries`, `:serialization-jackson` and `:command-service`, each mapped to its directory with
  `project(":x").projectDir = file("backend/...")` in `settings.gradle.kts`. A prototype configured and ran with that
  mapping and no intermediate project. A nested path such as `:contracts:kernel` would create an intermediate
  `:contracts` project whose default directory does not exist, which Gradle deprecates; that behaviour on Gradle 9 was not
  exercised. The flat names equal the compose service names, so Phase 6 can derive image names from them. The existing
  `:tooling:*` projects keep their nested form, because the `tooling/` directory exists.
- **Contracts by message kind (D-02).** The kernel holds the TypeID IDs, the tag registry and the shared `Rejection` and
  result base types. Commands, events and queries are three more modules. The graph shows the CQRS boundaries: the
  query service depends on events and queries, and the BFF on commands and queries but not on events.
- **Metadata only (D-04, D-44).** The contracts may carry Axon and Jackson annotations, with the libraries as
  `compileOnly` dependencies, so a contracts module has no framework at runtime. `ContractsArchitectureTest` enforces it
  and the author accepted its scope when it was written: Spring is banned in every source set, while Axon and Jackson
  imports are banned in `src/main` only, restricted to the annotation packages. A contracts build script may declare an
  Axon or Jackson library as `compileOnly`, `testImplementation` or `testRuntimeOnly`, never as `implementation`, `api`
  or `runtimeOnly`.
- **The command service starts as a library (D-43).** The `KotlinTagResolver`, the decision-capture decorator, the
  handling-side retry and the permanent Axon Server tests live in `backend/command-service`, a plain Kotlin library in
  Phase 2. Phase 3 turns it into the Spring Boot command service.

### Consequences

- Good, because a service and its directory, compose service and image carry one name, and a reader finds a module
  by the name in the build output.
- Good, because a consumer receives annotations and nothing else from the contracts, so no service inherits Axon or Jackson
  through them.
- Good, because the dependency graph is the CQRS picture, and a wrong dependency is visible in one `build.gradle.kts`.
- Bad, because the rule is looser than the strictest reading of "no framework in the contracts". The invariant is that
  the contracts carry no framework at runtime, and the test classpath does not reach a consumer. Test scope is allowed
  for any Axon or Jackson library in a contracts build script, which is broader than the one package a test needs today:
  the kernel's vector test reads the TypeID YAML vectors with `tools.jackson.dataformat.yaml`.
- Bad, because the flat names mean that the directory tree and the project paths differ, and every new module needs its
  `projectDir` line.
- Bad, because the paths are referenced from CI filters, Renovate, REUSE and IDE run configurations, so a later move is
  expensive.

### Confirmation

- `ContractsArchitectureTest` in `:tooling:architecture` checks the import rules, the allowed configurations in the
  contracts build scripts and that exactly the four contracts modules have a build script. `check` runs it with every
  other suite.
- `MessageArchitectureTest` checks that a production message lives in the contracts module of its kind.
- The layout comment in `settings.gradle.kts` states the mapping, and a new module that breaks it fails review.

## Pros and Cons of the Options

### `backend/` directories with flat project names

- Good, because directory, project, compose service and image names agree.
- Good, because no intermediate project is needed.
- Bad, because directories and project paths differ, and each module needs a `projectDir` line.

### Nested Gradle project paths

- Good, because project paths mirror the directory tree.
- Bad, because an intermediate `:contracts` project would exist without a directory, and image names derived from a nested
  path are not the compose service names.

### A flat top-level layout

- Good, because it is the shallowest tree.
- Bad, because contracts, services and libraries would sit in one list next to `platform/`, `build-logic/` and `tooling/`,
  and the grouping of the contracts would not show. The author's refinement, contracts grouped and services flat, removed
  the objection against `backend/` that a deeper tree costs depth.

### Contracts split by message kind

- Good, because the CQRS boundaries are visible in the dependency graph, with no rule needed to keep them.
- Bad, because four modules need four build scripts.

### One contracts module with a kernel

- Good, because it is the least build configuration.
- Bad, because every consumer would depend on every message kind, so the boundaries would rest on Konsist alone.

### Contracts by domain area

- Bad, because DCB decisions span entities, so a course or student module would cut across the decisions it serves.

### `compileOnly` annotations

- Good, because the contracts carry names, versions and tags as metadata and no framework at runtime.
- Bad, because the contracts compile against the framework, so a rule is needed to keep any other import out.
- Pure Kotlin contracts would register names and tags in the services, away from the types they describe. Deciding by spike
  was not needed: the spikes confirmed that the annotations work as metadata (spike 002).

### The wiring in `backend/command-service`

- Good, because the permanent safeguards (the conflict test, the round trip and the decision-capture test) need a real module
  that `check` runs.
- Bad, because a module exists before the service it will become. Keeping the wiring in the spike directory would leave the
  safeguards outside the build.

## More Information

- Decisions: D-01 (amends Phase 1 D-03), D-02, D-04, D-43, D-44.
- The author accepted the looser scope of the contracts rules (imports banned in `src/main` only, test-scope
  declarations allowed) on 2026-11-23, when the Konsist suite of plan 02-11 was reviewed; the header comment of
  `ContractsArchitectureTest` carries the same wording.
- Revisit the flat names if a Gradle release makes nested paths without intermediate directories practical, and tighten
  the test-scope allowance if a contracts module ever needs a test library beyond the one the vector test uses.
