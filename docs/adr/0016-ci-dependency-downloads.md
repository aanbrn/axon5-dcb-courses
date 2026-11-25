---
status: "accepted"
date: "2026-11-25"
decision-makers: ["repository author (gap G-02-1 decision, 2026-11-25)"]
---

# ADR-0016: CI dependency downloads go through a Maven Central mirror first, with a one-retry rate-limit wrapper

## Context and Problem Statement

The `compose-smoke` job failed at commit 729dcff (run 45532702352) because repo.maven.apache.org answered a download with
HTTP 429. The failing chain started at a Plugin Portal redirect and ended at Maven Central for the library dependencies
of the `kotlin-dsl` plugin. The second attempt of the same run succeeded, which confirms a flake and not a defect of the
build.

The cause is structural. Pull-request runs of the jobs that `main` does not run yet start with an empty Gradle cache
(`cache-read-only` keeps a pull request from ever writing the cache), so each downloads the whole plugin and dependency
classpath from a shared runner IP address, which is what Maven Central rate-limits. Gradle retries a failed download
internally, but it disables a repository after an HTTP 429 or 5xx and does not move on to the next repository, so the
declared repository order gives no protection against it; only a 404 moves on.

The five required Gradle jobs (`build`, `integration-test`, `functional-test`, `compose-smoke`, `doctor`) must fail for
repository reasons no more often than the shared infrastructure forces them to.

## Decision Drivers

- Required jobs fail only for reasons that belong to the change, not for a rate limit on a shared host.
- Local builds and Renovate read exactly the repositories the settings files declare.
- No weaker cache policy: the cache stays written from `main` only, and the provider stays `basic`.
- Reproducibility: every run installs the same file with the same content, so configuration-cache entries restored from
  `main` stay reusable.
- The trust position of every download source is stated plainly.

## Considered Options

- A CI-only init script that puts a Maven Central mirror first, plus a one-retry wrapper keyed to Gradle's HTTP 429
  message (chosen).
- The mirror as a trailing fallback after `mavenCentral()`.
- `allowInsecureContinueWhenDisabled` on the repositories.
- Gradle's internal `org.gradle.internal.network.retry.*` properties.
- Writing the cache from pull requests, or switching to the enhanced cache provider.
- The mirror in the settings files.

## Decision Outcome

Chosen option: "a CI-only init script that puts a Maven Central mirror first, plus a one-retry wrapper keyed to
Gradle's HTTP 429 message", because it is the only option that keeps rate-limited hosts out of the jobs without changing
what local builds and Renovate read or weakening the cache policy.

- `scripts/ci/maven-central-mirror.init.gradle.kts` adds Google's mirror,
  `https://maven-central.storage-download.googleapis.com/maven2/`, as the first plugin repository and the first
  dependency repository of every build, the root build and the included `build-logic` build alike. The repositories the
  settings files declare follow it in their original order. The Gradle Plugin Portal is added back behind the mirror in
  the plugin list, because it is Gradle's implicit default and disappears once that list is not empty. A file the mirror
  lacks (HTTP 404, for example a Plugin Portal marker) is found in the repositories behind it.
- The local composite action `.github/actions/maven-central-mirror` installs the script into
  `${GRADLE_USER_HOME:-$HOME/.gradle}/init.d/`, and each of the five Gradle jobs calls it after setup-gradle. A file
  there reaches every Gradle invocation of a job, including the one inside `scripts/doctor.sh --ci`, with no change to
  any command line. One definition replaces five copies, so the step cannot drift between jobs, and the init script's
  contribution to the configuration-cache fingerprint is the same everywhere, on `main` and on pull requests.
- `scripts/ci/retry-on-rate-limit.sh` (plan 02-24) stays as the second line: it reruns a Gradle step once, after a delay,
  when Gradle reports HTTP 429 from whichever host answered.
- `cache-provider: basic` and `cache-read-only` on pull requests stay as they are.

### Consequences

- Good, because a Maven Central rate limit no longer reaches a job as long as the mirror serves the file.
- Good, because local builds, Renovate and every settings file are unchanged: the mirror exists only in a file the
  workflow installs.
- Good, because a TestKit test pins the repository order of the fixture build and of the real `build-logic` build, and
  the script is a `functionalTest` input, so a change to it reruns that test.
- Bad, because the mirror is an additional, unverified download source until Phase 6 (CICD-05). `gradle/verification-metadata.xml`
  does not exist yet, so nothing checks the mirrored artifacts by checksum. Integrity rests on HTTPS and on the mirror's
  operator (Google), the same basis on which the build trusts Maven Central today; the `.sha1` files Gradle checks come from
  the same host, so they catch corruption, not tampering. The author accepted this on 2026-11-25. When CICD-05 lands,
  dependency verification checks every artifact by checksum whatever repository served it, so it covers the mirrored
  artifacts with no mirror-specific entry.
- Bad, because the jobs now depend on the mirror's availability. A 5xx from the mirror disables it and fails the build
  without moving on, and the wrapper retries only a 429. Accepted because the outage risk of the Google-hosted mirror is
  lower than Maven Central's per-IP rate limit for shared runners.
- Bad, because two residuals stay: TestKit fixture builds inside the `functional-test` job use TestKit's own Gradle user
  home, which has no `init.d` script, so they still resolve from the declared repositories (the wrapper covers a 429
  there only if Gradle's message reaches the job log); and the `qodana` job runs Gradle in its own container with its own
  Gradle home, so it is unchanged.

### Confirmation

`CiMavenMirrorFunctionalTest` (build-logic `functionalTest` suite) runs the real script, a fixture build and the real
`build-logic` build under `--configuration-cache --isolated-projects`, and asserts the order of both repository lists with
and without the script and the configuration-cache reuse of a second run. The first run of the CI workflow on GitHub
runners is plan 02-30. Revisit the decision when CICD-05 adds dependency verification (the mirror then needs no further
trust note), when Maven Central stops rate-limiting shared runners, or when the mirror stops serving a file the build
needs.

## Pros and Cons of the Options

### A CI-only init script that puts a mirror first, plus the 429 retry (chosen)

- Good, because the mirror answers before the rate-limited host is asked, and the retry covers a 429 from whichever host
  does answer.
- Good, because nothing local changes.
- Bad, because of the unverified source and the dependency on the mirror's availability described above.

### The mirror as a trailing fallback after `mavenCentral()`

- Good, because Maven Central stays the primary source.
- Bad, because it is useless for the failure that matters: Gradle does not move to the next repository after a 429 or a
  5xx, so the mirror would never be asked.

### `allowInsecureContinueWhenDisabled`

- Good, because Gradle would continue to the next repository after a disabled one.
- Bad, because the resolved artifacts then depend on which repository happened to answer, which breaks reproducibility.
  Rejected, and the property is not used anywhere.

### Gradle's internal `org.gradle.internal.network.retry.*` properties

- Good, because they tune the retry inside Gradle.
- Bad, because they are unsupported internals that Gradle has renamed before, and a retry inside one repository does not
  solve a rate limit that lasts longer than the retry.

### Writing the cache from pull requests, or the enhanced cache provider

- Good, because a warm cache would download less.
- Bad, because a cache written by a pull request can poison what `main` restores. The author rejected it and keeps the
  `basic` provider.

### The mirror in the settings files

- Good, because every build would use it with no workflow step.
- Bad, because it changes what local builds and Renovate read, and puts an unverified source into every developer's build.

## More Information

- Gap G-02-1 of phase 02, the failure analysis in `.planning/debug/compose-smoke-ci-failure.md`.
- `scripts/ci/maven-central-mirror.init.gradle.kts` and `scripts/ci/retry-on-rate-limit.sh` carry the same reasoning in
  their header comments. Both live under the governed path `scripts/ci/**`, so a pull request that changes one needs the
  `governance-approved` label (ADR-0003, ADR-0015).
