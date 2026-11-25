---
status: diagnosed
trigger: "compose-smoke-ci-failure: Jobs integration-test, compose-smoke, doctor and upstream-records pass on a GitHub runner, and ci-success (which needs all of them) is green"
created: 2026-11-25T03:00:00Z
updated: 2026-11-25T03:17:00Z
---

## Current Focus

hypothesis: CONFIRMED - infrastructure flake. Maven Central (repo.maven.apache.org, reached via the Gradle Plugin Portal redirect) returned HTTP 429 Too Many Requests to the compose-smoke runner's egress IP during cold-cache resolution of the build-logic plugin classpath; Gradle disabled the repository and the build failed before any test ran. Not a code or workflow regression.
test: compared red vs green compose-smoke logs, the other four Gradle jobs in the same red run, and the 07fb5d1..729dcff diff
expecting: n/a (diagnosis complete)
next_action: Return ROOT CAUSE FOUND to the caller (goal find_root_cause_only); no code change.
mode: find_root_cause_only
bug_class: Heisenbug/Mandelbug (environment-dependent, non-deterministic: same commit and inputs pass on other runners in the same minute)
reasoning_checkpoint:
  hypothesis: "compose-smoke failed because Maven Central rate-limited (HTTP 429) the runner's IP while Gradle resolved the build-logic plugin classpath from a cold Gradle User Home, and with no mirror or longer retry Gradle disabled the repository and failed configuration"
  confirming_evidence:
    - "Failed log: `Could not GET 'https://repo.maven.apache.org/maven2/.../kotlinx-serialization-bom-1.6.0.pom'. > Received status code 429 from server: Too Many Requests` plus 7 identical failures; BUILD FAILED in 27s at settings.gradle.kts line 4"
    - "Build, Integration tests, Functional tests and Doctor in the same run, at the same commit and minute, also cold-cache, resolved the same plugins from Maven Central and succeeded"
    - "Green compose-smoke at 07fb5d1 had the identical cold-cache start and succeeded; the only non-.planning change since is .gitignore"
  falsification_test: "A rerun of job 138931678592 at 729dcff that fails again with a non-429 error (or fails deterministically) would refute the flake classification"
  fix_rationale: "Diagnosis only. A rerun clears this instance; reducing cold Maven Central traffic (main-written caches for the new jobs after merge, a Maven Central mirror, or retry with backoff around resolution) addresses the exposure"
  blind_spots: "No same-SHA rerun exists yet; the runner's egress IP is not logged, so per-IP throttling is inferred from Sonatype's documented policy and the cross-job comparison, not observed directly; Gradle's internal retry behaviour for 429 was not inspected"
  candidate_causes:
    - "environment: Maven Central per-IP consumption limit returned 429 to the compose-smoke runner (confirmed trigger)"
    - "config: PR jobs restore only main-written caches (`cache-read-only` on non-main refs) and main has no compose-smoke job yet, so every PR run resolves cold (contributing, constant across green and red runs)"
    - "code: regression in 07fb5d1..729dcff (eliminated - only .gitignore changed)"
    - "code/config: quick tasks 261125-a09/bha workflow changes (eliminated - present in the green 07fb5d1 run)"
  and_gate: "yes - the failure needs (1) a 429 from Maven Central on that runner AND (2) a cold cache with no mirror forcing full resolution from Maven Central. (2) is present in every PR run including green ones, so (1) is the varying trigger; (2) is the standing exposure"

## Symptoms

expected: All CI jobs on PR #21 (aanbrn/axon5-dcb-courses, branch gsd/phase-2-runtime-infrastructure-dcb-spikes) pass, including "Compose smoke test", so ci-success is green.
actual: On PR #21 at head 729dcff76c87da47fe091ec5456cc81e740d137d, job "Compose smoke test" = FAILURE (workflow run 45532702352, job 138931678592), therefore ci-success = FAILURE (job 138935533158). All other jobs SUCCESS. An earlier run at 07fb5d1 had compose-smoke green; commits after it include a8e8c2e (.gitignore entry), quick tasks 261125-a09 and 261125-bha (TestKit suite moved into a parallel functional-test job), and .planning/ docs.
errors: see failing job log (`gh run view 45532702352 --job 138931678592 --log-failed`)
reproduction: UAT test 1 (.planning/phases/02-runtime-infrastructure-dcb-spikes/02-UAT.md)
started: Discovered during UAT on 2026-11-25; CI green at 07fb5d1, red at 729dcff.

## Eliminated

- hypothesis: Regression from quick tasks 261125-a09 / 261125-bha (TestKit suite moved to a parallel functional-test job, doctor fixes)
  evidence: Both quick tasks are ancestors of 07fb5d1, whose run 45528279704 had compose-smoke SUCCESS; they are not in the 07fb5d1..729dcff range.
  timestamp: 2026-11-25T03:05:00Z

- hypothesis: Regression from a8e8c2e (.gitignore `/.build/` entry) or any other repository change between 07fb5d1 and 729dcff
  evidence: The only non-.planning diff is three lines of .gitignore, which is not a Gradle, compose or workflow input; the failure is an HTTP 429 from repo.maven.apache.org, independent of repository content.
  timestamp: 2026-11-25T03:05:00Z

- hypothesis: Compose stack or Axon Server failed to start (the smoke test's own subject)
  evidence: "Start the stack" (docker compose up -d --wait) succeeded; Gradle failed at settings evaluation before any test ran; Axon Server logs show normal initialisation of _admin and default contexts.
  timestamp: 2026-11-25T03:03:00Z

- hypothesis: Cache state changed (a previously restored cache went missing in the red run)
  evidence: Green compose-smoke at 07fb5d1 also logged "Basic caching did not find an entry to restore" and downloaded the Gradle distribution; the starting state is identical in both runs.
  timestamp: 2026-11-25T03:08:00Z

## Evidence

- timestamp: 2026-11-25T03:02:00Z
  checked: Knowledge base (.planning/debug/knowledge-base.md)
  found: File does not exist; no prior resolved pattern to seed a hypothesis.
  implication: Open-ended investigation.

- timestamp: 2026-11-25T03:03:00Z
  checked: `gh run view 45532702352 --job 138931678592 --log-failed`
  found: Failing step is "Host scenario (test against localhost:8124)" (`./gradlew :command-service:integrationTest -Pcourses.axonserver.external=localhost:8124 --tests '*ConditionalAppendIntegrationTest*'`). "Start the stack" (docker compose up -d --wait) succeeded. Gradle failed at settings.gradle.kts line 4 resolving plugin `courses.settings` -> configuring included `:build-logic` -> `Could not resolve org.jetbrains.kotlin:kotlin-stdlib:2.4.21` (required by org.gradle.kotlin.kotlin-dsl 6.7.13) -> "Repository Gradle Central Plugin Repository is disabled due to earlier error" -> `Could not GET 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-bom/1.6.0/kotlinx-serialization-bom-1.6.0.pom'` -> "Received status code 429 from server: Too Many Requests"; "There are 7 more failures with identical causes". BUILD FAILED in 27s, before any project code was compiled or any test ran.
  implication: The failure is an HTTP 429 rate-limit response from Maven Central (reached through the Gradle Plugin Portal redirect) during plugin/dependency resolution, not a test, compose or build-logic code failure.

- timestamp: 2026-11-25T03:05:00Z
  checked: `git log 07fb5d1..729dcff` and `git diff --stat 07fb5d1..729dcff -- . ':!.planning'`
  found: Quick tasks 261125-a09 (a4af17b..a881aac) and 261125-bha (3d3cc34..eb15939) are ancestors of 07fb5d1, so the green run 45528279704 at 07fb5d1 already contained them. The only non-.planning change between green 07fb5d1 and red 729dcff is a8e8c2e: three lines in .gitignore (`/.build/` plus a comment). No change to .github/workflows/ci.yml, compose/*, settings/build files, scripts or the version catalog.
  implication: No code or workflow regression exists between the last green and the red run; a .gitignore entry cannot affect Gradle dependency resolution.

- timestamp: 2026-11-25T03:08:00Z
  checked: setup-gradle step output in red compose-smoke (138931678592) vs green compose-smoke (138916944376 in run 45528279704)
  found: Both log "Basic caching did not find an entry to restore. Will start with empty state." and both download gradle-9.9.2-bin.zip ("Fetching distribution."). Green then resolved build-logic plugins from Maven Central and finished "BUILD SUCCESSFUL in 1m 48s"; red got 429 on the same resolution.
  implication: Identical cold-cache starting state and identical inputs; the only difference is the response from repo.maven.apache.org at that moment. The outcome is decided by an external service, not by the repository.

- timestamp: 2026-11-25T03:10:00Z
  checked: Other Gradle jobs in the same red run 45532702352: Build 138931678742, Integration tests 138931678645, Functional tests 138931678591, Doctor 138931678325
  found: All four also log "Basic caching did not find an entry to restore" and "Fetching distribution." (cold Gradle User Home, full resolution from Maven Central at the same minute, 02:24-02:25Z) and all four SUCCEEDED. No 429 in their logs.
  implication: Same commit, same build files, same resolution, same minute, different runner VMs: only the compose-smoke runner was throttled. Consistent with Maven Central's per-client-IP rate limiting of one runner's egress IP, i.e. an infrastructure flake.

- timestamp: 2026-11-25T03:11:00Z
  checked: `git show origin/main:.github/workflows/ci.yml` (main at 78f9bde)
  found: main has jobs build, lint, secrets, qodana, ci-success only; compose-smoke, integration-test, functional-test, doctor and upstream-records do not exist on main yet. Every setup-gradle step uses `cache-read-only: ${{ github.ref != 'refs/heads/main' }}`, so PR runs only restore caches written on main.
  implication: Contributing condition (not the trigger): no main-written Gradle cache can exist for the new jobs, so every PR run of compose-smoke (and the other new jobs) resolves ~all dependencies from Maven Central cold, maximising exposure to rate limiting. This is a property of the branch since compose-smoke was added, not a change between 07fb5d1 and 729dcff.

- timestamp: 2026-11-25T03:13:00Z
  checked: `gh api repos/aanbrn/axon5-dcb-courses/actions/runs/45532702352` and `gh run list --commit 729dcff...`
  found: run_attempt 1, previous_attempt_url null; the only runs at 729dcff are CI 45532702352 (failure), CodeQL 45532702401 (success) and Governance 45532702334 (success).
  implication: No rerun of the failed job at the same SHA exists, so there is no direct same-SHA green confirmation yet; the flake classification rests on the log evidence and the same-run/same-minute comparison above.

- timestamp: 2026-11-25T03:14:00Z
  checked: Sonatype FAQ "429 Too Many Requests and Maven Central consumption limits" (central.sonatype.org/faq/429-error/) and related reports (CircleCI, Bitrise, Renovate discussions)
  found: Maven Central enforces consumption limits per client IP / network; a 429 reflects aggregate traffic from an organisation, network or CI provider egress, is a temporary recoverable signal, and Sonatype recommends caching or a repository manager instead of repeated downloads from clean ephemeral environments.
  implication: Matches the observed pattern exactly: an ephemeral GitHub-hosted runner with a cold cache, throttled at its very first Maven Central requests (build failed 27 s into the step, during settings-plugin resolution, before the job had produced meaningful traffic of its own).

- timestamp: 2026-11-25T03:15:00Z
  checked: Compose smoke test outcome in recent runs on the branch: 45528279704 (07fb5d1), 45522275399 (a881aac), 45507745588 (5d66ae4), 45435248166 (af7ecc4); earlier failed run 45419305916 (6e267b3)
  found: compose-smoke SUCCESS in all four; the 6e267b3 failure was Lint and Qodana, not compose-smoke.
  implication: First and only compose-smoke failure on the branch; no recurring pattern in the job itself.

- timestamp: 2026-11-25T03:16:00Z
  checked: build-logic/settings.gradle.kts repository declaration
  found: `repositories { gradlePluginPortal(); mavenCentral() }`; the Plugin Portal redirects non-plugin artifacts to repo.maven.apache.org, so both declared repositories ultimately hit Maven Central; no mirror is configured.
  implication: A Maven Central 429 has no fallback source; once Gradle marks the repository disabled after the error, all remaining resolutions fail ("7 more failures with identical causes").

## Resolution

root_cause: "Infrastructure flake, not a regression: Maven Central (repo.maven.apache.org, via the Gradle Plugin Portal redirect) answered HTTP 429 Too Many Requests to the compose-smoke runner during cold-cache resolution of the build-logic plugin classpath (kotlin-dsl 6.7.13 -> kotlin-stdlib 2.4.21 / kotlinx-serialization-bom 1.6.0); Gradle disabled the repository and failed at settings evaluation before any test ran; contributing standing condition: PR jobs restore only main-written Gradle caches and main has no compose-smoke job yet, so every PR run of the job resolves all dependencies from Maven Central with no mirror"
fix: (diagnosis only - not applied)
verification: (not applicable - find_root_cause_only)
files_changed: []
