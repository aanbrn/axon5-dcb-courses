# Build performance

The build is tuned for fast feedback, and its speed is measured with
[gradle-profiler](https://github.com/gradle/gradle-profiler) scenarios committed in
`performance.scenarios`. The scenarios run manually, never in CI: timings from shared CI runners are
too noisy to compare, and a scheduled profiler job was considered and not adopted (decision D-20).

## Running the scenarios

```shell
rm -rf profile-out
mise x -- gradle-profiler --benchmark --project-dir . --scenario-file performance.scenarios \
  clean_build noop_build abi_change config_cache_hit
```

`mise x --` puts the pinned gradle-profiler, Node.js, shfmt and taplo on `PATH`. Name one scenario
instead of four to run only that one, and add `--dry-run` to check that the file parses.

The first run downloads a Gradle distribution and every dependency into `gradle-user-home/`, which
takes several minutes and about a gigabyte. Later runs reuse it. gradle-profiler writes its results
into `profile-out/`, or into `profile-out-2/` and so on when that directory already exists, so
delete it first. Both directories are ignored by git and excluded from every formatter, so they
never need cleaning up for the build to pass. Remove them when you are done.

| Scenario           | Tasks      | What it measures                                                            |
| ------------------ | ---------- | --------------------------------------------------------------------------- |
| `clean_build`      | `build`    | The root project cleaned, the build cache bypassed, then built and checked  |
| `noop_build`       | `build`    | A build with nothing changed, which may do up-to-date checks only           |
| `abi_change`       | `assemble` | An API change to `Greeting.kt`, so incremental compilation and its fan-out  |
| `config_cache_hit` | `help`     | Configuration only: the configuration cache entry reused for a trivial task |

## Speed-ups in place

Each of these is a deliberate choice, and the build fails rather than silently losing one.

- **Configuration cache**, with problems failing the build (`org.gradle.configuration-cache.problems=fail`
  in `gradle.properties`). Every build logic change has to stay cacheable.
- **Build cache** (`org.gradle.caching=true`), so an unchanged task output is restored instead of
  rebuilt.
- **Parallel execution** (`org.gradle.parallel=true`) and **isolated projects**
  (`org.gradle.isolated-projects=true`), which keeps projects independent of each other's
  configuration.
- **File-system watching** (`org.gradle.vfs.watch=true`), so a no-op build does not rescan the
  tree.
- **Daemon JVM criteria** in `gradle/gradle-daemon-jvm.properties`, generated from `.java-version`
  by `./gradlew updateDaemonJvm`, so every contributor's daemon runs the same JDK.
- **Tuned JVM arguments** for the daemon and the Kotlin daemon (`org.gradle.jvmargs` and
  `kotlin.daemon.jvmargs`: a 1 GiB heap and the parallel collector). Tune them against the baseline
  below rather than by feel.
- **No kapt**: annotation processing would force a slow Java stub generation step on every Kotlin
  compile, so mapping and boilerplate are written in plain Kotlin.
- **Parallel test forks**: every test task uses half of the available processors as forks.

## Build Scans

Build Scans are opt-in. Pass `--scan` to publish one for a single run:

```shell
./gradlew build --scan
```

Gradle applies the Develocity plugin only when `--scan` is given, so nothing is published by
default and no build file applies the plugin. No property form is provided, because `--scan` needs
no change to the build. A scan sends build data to an external service, so it is never used in CI.

## Baseline

Measured on 2026-11-21 on macOS, arm64, 10 cores, with a warm daemon and a warm `gradle-user-home`.
The value is the median of the measured builds in milliseconds; warm-ups are not counted.

| Scenario         | Median (ms) | Warm-ups | Measured builds | Notes                                                                                                                                                             |
| ---------------- | ----------- | -------- | --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| clean_build      | 4812        | 1        | 3               | The first warm-up took 115 s because it also ran every TestKit functional test of the included `build-logic` build. A clean root build then reuses those results. |
| noop_build       | 328         | 2        | 5               | Up-to-date checks only. The first warm-up took 6.8 s, which is configuration and the cold daemon.                                                                 |
| abi_change       | 278         | 2        | 5               | Recompiles `convention-sample`, rebuilds its jar and runs nothing else. The builds got faster as the daemon warmed up (313 ms down to 253 ms).                    |
| config_cache_hit | 70          | 2        | 8               | The configuration cache entry is reused. The first warm-up took 5.8 s, because it computed the task graph and stored the entry.                                   |

How to read it: `noop_build` is the number to protect, because it is what every contributor pays
when nothing changed. A rise there usually means a task lost its up-to-date check, so run
`./gradlew build --info` and look for tasks that executed. `clean_build` is deliberately not a cold
build of the whole repository; the cold figure is the first warm-up.

Replace the table when the build changes enough to matter (a new subproject type, a new analysis
tool, a Gradle upgrade), and say so in the commit message.
