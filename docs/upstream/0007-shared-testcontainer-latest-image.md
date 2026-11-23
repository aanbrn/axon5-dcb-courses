---
id: "UP-0007"
product: "axon"
product-version: "5.4.2"
status: "reported"
workaround: true
upstream: ["https://github.com/AxonIQ/axoniq-framework/issues/812"]
reported: "2026-11-23"
updated: "2026-11-23"
---

# UP-0007: The shared Axon Server test container of axoniq-testcontainer hard-codes the floating latest image

## Symptom

`axoniq-testcontainer` ships `SharedAxonServerContainer`, a holder for the one `AxonServerContainer` configuration that
every integration-test suite of a build is meant to share. Its image is a constant: `docker.axoniq.io/axoniq/axonserver:latest`.
An application cannot choose another image, so a build that pins Axon Server to a tag and a digest cannot use the
holder, and a test run that does use it silently tests against whatever `latest` is on the day.

That is not reproducible, and it is not harmless: the image behind `latest` can move to a release the project has not
verified, and a release line can contain patch releases that are marked "Do not use" (2026.1.5 and 2026.1.6 are).
Observed with `io.axoniq.framework:axoniq-testcontainer` 5.4.2.

## Minimal reproduction

1. Add `io.axoniq.framework:axoniq-testcontainer:5.4.2` to the test dependencies of a Gradle project.
2. In a test, call `SharedAxonServerContainer.ensureStarted()`.
3. Look at the image of the running container, for example with `docker ps --format '{{.Image}}'` or by reading
   `SharedAxonServerContainer.INSTANCE.getDockerImageName()`: it is `docker.axoniq.io/axoniq/axonserver:latest`.
   Neither a system property nor an environment variable nor a method changes it, because `INSTANCE` is a constant
   that is built in the static initializer.

## Root cause

The constructor call of `INSTANCE` names the image literally ([line 62][shared]). The class documentation says why the
construction is fixed: it must be byte-identical in every module and JVM fork so that the reuse hash of Testcontainers
matches. A parameter that is read in the same way everywhere, such as a system property, would keep the hash stable and
still allow a pinned image.

A related, open report asks for licence resolution of the same container from the file system
([AxonIQ/axoniq-framework#221][licence]). It does not touch the image, so this record is not a duplicate of it.

[shared]: https://github.com/AxonIQ/axoniq-framework/blob/axoniq-5.4.2/testing/axoniq-testcontainer/src/main/java/io/axoniq/framework/testcontainer/SharedAxonServerContainer.java#L62
[licence]: https://github.com/AxonIQ/axoniq-framework/issues/221

## Workaround

`AxonServerEnvironment` in `:tooling:test-support` is the repository's own per-JVM singleton. It builds an
`AxonServerContainer` from the image of the version catalog, which is a tag plus a digest and reaches the test tasks as
a tracked input, with a DCB context, development mode and `withReuse(true)`, so the digest pin of the catalog is
honoured. The holder of `axoniq-testcontainer` is deliberately not used.

- `tooling/test-support/src/main/kotlin/io/github/aanbrn/courses/testsupport/AxonServerEnvironment.kt`: the singleton,
  marked in its KDoc.

## Removal criteria

An Axoniq Framework release in which the shared holder takes its image from a parameter, for example a system property
or a method that is called before `INSTANCE` is used, and keeps the reuse hash stable across modules. The release notes
or the changed `SharedAxonServerContainer` show it.

## How to verify removal

Pass the catalog image to the holder through the new parameter, replace the body of `AxonServerEnvironment` with a
delegation to the holder, delete the marker and run `mise x -- ./gradlew :command-service:integrationTest`. In the same
run, `docker ps --format '{{.Image}}'` shows the tag and digest of the catalog image, not `latest`.

## Upstream issue draft

Target: `AxonIQ/axoniq-framework`, as an enhancement request. Published as https://github.com/AxonIQ/axoniq-framework/issues/812 on 2026-11-23.

### Let SharedAxonServerContainer use a pinned Axon Server image

#### Environment and versions

- Product and version: Axoniq Framework 5.4.2 (`axoniq-testcontainer`)
- Related products and versions: Axon Server 2026.1.8, Testcontainers 2.0.5
- JDK, operating system and build tool: Temurin 25, Linux and macOS, Gradle 9.9.2

#### Steps to reproduce

1. Add `io.axoniq.framework:axoniq-testcontainer:5.4.2` to the test dependencies.
2. Call `SharedAxonServerContainer.ensureStarted()` in a test.
3. Read `SharedAxonServerContainer.INSTANCE.getDockerImageName()`.

#### Expected behavior

The image can be chosen, so that a project pins Axon Server to a tag and a digest and every module of the build shares
one container of that image.

#### Actual behavior

The image is always `docker.axoniq.io/axoniq/axonserver:latest`. Builds that want a reproducible Axon Server have to
write their own holder and lose the cross-module and cross-JVM sharing the class exists for.

#### Minimal reproduction

```java
SharedAxonServerContainer.ensureStarted();
System.out.println(SharedAxonServerContainer.INSTANCE.getDockerImageName());
// docker.axoniq.io/axoniq/axonserver:latest
```

#### Analysis

`INSTANCE` is created in the static initializer with a literal image name
([source](https://github.com/AxonIQ/axoniq-framework/blob/axoniq-5.4.2/testing/axoniq-testcontainer/src/main/java/io/axoniq/framework/testcontainer/SharedAxonServerContainer.java#L62)).
The class documentation explains that the construction must be identical in every module for the Testcontainers reuse
hash to match, which a system property read once would also satisfy.

#### Proposed fix

Read the image from a documented system property, for example `axoniq.testcontainer.image`, and fall back to the
current default. Because the value is the same in every module and fork of a build, the reuse hash stays stable. We can
contribute a pull request.
