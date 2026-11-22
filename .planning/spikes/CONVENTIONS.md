# Spike Conventions

Patterns and stack choices established across spike sessions. New spikes follow these unless the question requires otherwise.

## Stack
- Kotlin/JVM spikes live in one standalone Gradle build at `.planning/spikes/` (`settings.gradle.kts` includes each
  spike module by `projectDir`); versions come from the main catalog via `versionCatalogs { from(files(...)) }` and
  BOMs are imported in the root `build.gradle.kts`. Run with `./gradlew -p .planning/spikes <task>` from the repo root.
- Axon spikes test both backends: in-memory (`InMemoryEventStorageEngine`, `disableEnhancer(AxonServerConfigurationEnhancer)`)
  and Axon Server 2026.1.8 via `AxonServerContainer(...).withDevMode(true).withDcbContext(true)`.
- Plain `EventSourcingConfigurer` for fast checks; one `SpringApplicationBuilder` test when Boot wiring matters.

## Structure
- `NNN-name/` holds `README.md` plus either a Gradle module (`src/main`, `src/test`) or compose/config files.
- Compose project names `spikeNNN`; host ports per spike (003: 8024/8124, 005: 18081/18082).

## Patterns
- Evidence is printed from tests (`println` of records/results) and the test asserts the conclusion, so a rerun
  reproduces both the observation and the verdict.
- Facts about third-party jars are read from Maven Central `-sources.jar` or `javap`, and verified by running code.
- Container images are built with Gradle (`bootBuildImage` / `BootBuildImage` tasks), not `pack`.

## Tools & Libraries
- Jackson 3 (`tools.jackson`) with jackson-module-kotlin; `com.fasterxml.uuid:java-uuid-generator` 5.2.0 for UUIDv7.
- Testcontainers via `~/.testcontainers.properties` (`tc.host=unix:///var/run/docker.sock`) on colima.
- Avoid: `pack` CLI and Docker's containerd image store for Paketo exports on the full noble run image
  (moby#52193); colima's default DNS forwarder for Go-based tools (AAAA timeouts).
