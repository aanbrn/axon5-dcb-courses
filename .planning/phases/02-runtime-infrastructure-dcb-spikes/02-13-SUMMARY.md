---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 13
subsystem: governance
tags: [cyclonedx, sbom, licences, notice, gradle, testkit, axoniq]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "build-logic conventions (courses.kotlin-library, courses.governance), the functionalTest TestKit suite and GradleFixture, the module set of plans 02-03 to 02-11"
provides:
  - "courses.licences convention: per-module CycloneDX SBOM (build/licences/bom.json) and checkDependencyLicences wired into check"
  - "DependencyLicenceCheck: fail-closed SBOM versus policy evaluation with OR/AND expression semantics, aliases and per-artifact justified exceptions"
  - "governance/dependency-licences.json: allowlist, forbidden families, aliases and the five Axoniq exceptions"
  - "NOTICE and checkNotice: every exception artifact and the Axoniq terms URL must appear in NOTICE"
affects: [02-20 governed path, ask rule and CI step for the licence gate, phase 5 npm half of CICD-09, phase 6 image SBOMs]

actuals:
  tokens: 12700
  tasks: 2
  commits: 3
plan_head_before: 325793d6b2bbdcd89eeb3824d2db2317f43301e0
plan_head_after: 5492153d4aca290e92ae7d6d5531fcfb33e3e9a2

tech-stack:
  added: [org.cyclonedx:cyclonedx-gradle-plugin 3.6.1]
  patterns:
    - "Gradle plugin applied per module through a convention plugin, never from the root project or allprojects"
    - "Cacheable build-logic check task that reads a governed JSON policy with groovy.json.JsonSlurper and writes a sorted report"
    - "Fail-closed policy evaluation: unknown, missing and forbidden-only licences fail naming coordinates and text"

key-files:
  created:
    - build-logic/src/main/kotlin/courses.licences.gradle.kts
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheck.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicencePolicy.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/LicenceExpression.kt
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/NoticeCheck.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheckFunctionalTest.kt
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/NoticeCheckFunctionalTest.kt
    - governance/dependency-licences.json
    - NOTICE
  modified:
    - gradle/libs.versions.toml
    - build-logic/build.gradle.kts
    - build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts
    - build-logic/src/main/kotlin/courses.governance.gradle.kts
    - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/GradleFixture.kt

key-decisions:
  - "Project dependencies of the same build are recognised by the `?project_path=` purl qualifier the SBOM plugin adds, and are not judged; no group-based or wildcard exemption exists in the policy"
  - "No JNA or logback exception: the POM-derived SBOM lists a permissive alternative for both (Apache-2.0 for JNA, EPL-2.0 for logback), so the OR rule passes them"
  - "Exceptions match one artifact (group:name) and the verbatim licence text; a pattern, a blank text or a blank reason makes the policy invalid"
  - "An alias that resolves to a forbidden licence makes the policy invalid"
  - "checkNotice matches an artifact as a whole name, so io.axoniq:connector-extra does not mention io.axoniq:connector"

patterns-established:
  - "Per-module SBOM plus per-module check: a root task cannot read other projects' outputs under isolated projects"
  - "Policy files carry their own validation: the check refuses a policy that weakens the rules"

requirements-completed: [CICD-09]

coverage:
  - id: D1
    description: "Every Kotlin module writes a CycloneDX SBOM of its runtime classpaths and runs checkDependencyLicences from check; the real dependency set passes"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "mise x -- ./gradlew checkDependencyLicences checkNotice build (BUILD SUCCESSFUL, every module's SBOM present)"
        status: pass
      - kind: integration
        ref: "DependencyLicenceCheckFunctionalTest#a module with the library convention writes its own SBOM and passes under the isolated projects flags"
        status: pass
    human_judgment: false
  - id: D2
    description: "Licence semantics: alternatives, AND/OR expressions, aliases, exceptions, missing and unknown licences fail closed, violations sorted by coordinates"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "DependencyLicenceCheckFunctionalTest (17 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D3
    description: "NOTICE lists third-party attributions and the Axoniq terms, and checkNotice fails when an exception artifact or the terms URL is missing"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "NoticeCheckFunctionalTest (5 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D4
    description: "The policy cannot be weakened with a wildcard exception, an alias to a forbidden licence or an exception without a reason"
    requirement: CICD-09
    verification:
      - kind: integration
        ref: "DependencyLicenceCheckFunctionalTest#an exception for a pattern is rejected, #an exception without a written reason is rejected, #an alias that resolves to a forbidden licence is rejected"
        status: pass
    human_judgment: false
  - id: D5
    description: "Each Axoniq exception reason and the NOTICE wording read as an accurate and fair statement of the Axoniq terms"
    verification: []
    human_judgment: true
    rationale: "Whether the stated terms (free for development and CI, licence required in production) are legally accurate is not something a test can assert; it is the author's call."

duration: 25min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 13: Dependency licence gate and NOTICE Summary

**Per-module CycloneDX SBOMs checked against a governed allowlist with fail-closed OR/AND licence semantics, five justified Axoniq exceptions, and a NOTICE that `checkNotice` proves mirrors every exception**

## Performance

- **Duration:** 25 min
- **Started:** 2026-11-23T04:19:00Z
- **Completed:** 2026-11-23T04:44:32Z
- **Tasks:** 2
- **Files modified:** 14

## Accomplishments

- `courses.licences` applies `org.cyclonedx.bom` per module (through `courses.kotlin-library`, never from the root) and writes `build/licences/bom.json` from the runtime classpaths only; `checkDependencyLicences` runs from `check` in all eight Kotlin modules. The CycloneDX plugin works under the configuration cache and isolated projects when applied per module, as the orchestrator note anticipated.
- `DependencyLicenceCheck` evaluates every component: any acceptable alternative passes, an exception covers exactly one artifact and one verbatim licence text, and everything else (no entry, unknown text, forbidden-only, malformed expression) fails naming the coordinates and the licence, sorted by `group:name:version`.
- The policy file refuses to be weakened: a wildcard or malformed exception, an exception without a reason, and an alias to a forbidden licence make the build fail.
- `NOTICE` and `checkNotice`: the check fails when an exception artifact or `https://www.axoniq.io/legal/terms-of-service` is missing.
- The real dependency set passes: `./gradlew checkNotice build` is green and the full `:build-logic:functionalTest` suite (including 17 licence and 5 NOTICE cases) is green.

## Exceptions and the licence text found in the SBOMs

All five exceptions carry the SBOM licence name `Axoniq Terms of Service` (no SPDX id; URL `https://www.axoniq.io/legal/terms-of-service`):

| Artifact | Licence text in the SBOM | Version seen |
|----------|--------------------------|--------------|
| `io.axoniq:axonserver-connector-java` | Axoniq Terms of Service | 2026.1.1 |
| `io.axoniq.framework:axon-server-connector` | Axoniq Terms of Service | 5.4.2 |
| `io.axoniq.framework:axoniq-distributed-messaging` | Axoniq Terms of Service | 5.4.2 |
| `io.axoniq.framework:axoniq-framework-bom` | Axoniq Terms of Service | 5.4.2 |
| `io.axoniq.framework:axoniq-testcontainer` | Axoniq Terms of Service | 5.4.2 |

Every other non-allowlist entry in the SBOMs is an alternative that has an allowed sibling, so no further exception exists:

| Component | Entries found | Passes through |
|-----------|---------------|----------------|
| `net.java.dev.jna:jna` 5.18.1 and 5.9.0, `jna-platform` 5.9.0 (test scope via Testcontainers) | `LGPL-2.1-or-later`, `Apache-2.0` | `Apache-2.0` (RESEARCH A4 confirmed from the POM-derived SBOM) |
| `ch.qos.logback:logback-classic`, `logback-core` 1.5.38 | `EPL-2.0`, `LGPL-2.1-only` | `EPL-2.0` |
| `jakarta.annotation:jakarta.annotation-api` | `EPL-2.0`, `GPL-2.0-with-classpath-exception` | `EPL-2.0` |

Aliases are empty: the cyclonedx plugin already normalises every licence it can to an SPDX id, so the SBOMs contain no free-text name other than the Axoniq one, which is excepted rather than aliased.

## Task Commits

1. **Task 1: End-to-end licence gate (tracer)** - `39994ed` (feat)
2. **Task 2 RED: licence semantics and NOTICE mirror TestKit cases** - `c60528a` (test)
3. **Task 2 GREEN: NOTICE and checkNotice** - `5492153` (feat)

**Plan metadata:** committed with this SUMMARY (docs).

## TDD Gate Compliance

- **RED (`c60528a`):** `NoticeCheckFunctionalTest` (5 tests) written before `NoticeCheck` existed. The run failed all five on the planned behavior: `checkNotice` does not exist, so the passing cases hit `UnexpectedBuildFailure` and the failing cases miss the asserted `missing from NOTICE: ...` message. `gsd-tools check tdd-red-evidence` returned `RED_EVIDENCE_OK` (`target_test_failed`) on the unchanged JUnit XML. Semantic assessment: the target test executed and failed on the planned assertion for the intended reason (the feature is absent), not on a compile, fixture or discovery fault.
- **Licence-semantics tests in the same commit:** `DependencyLicenceCheckFunctionalTest` (17 tests) passed on arrival, by design. They pin the behaviour of the Task 1 tracer, which implemented the check before Task 2 began, so no RED exists for them; this is the tracer shape the plan chose, recorded here rather than hidden.
- **GREEN (`5492153`):** `NoticeCheck`, the `courses.governance` wiring and `NOTICE`; all 22 tests pass.
- **REFACTOR:** none needed.

## Files Created/Modified

- `build-logic/src/main/kotlin/courses.licences.gradle.kts` - applies the SBOM plugin per module, registers `checkDependencyLicences`, wires it into `check`
- `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts` - applies `courses.licences`
- `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/DependencyLicenceCheck.kt` - cacheable SBOM-versus-policy task
- `.../governance/LicencePolicy.kt` - policy model, validation and the per-component rules
- `.../governance/LicenceExpression.kt` - SPDX expression parser (`AND`, `OR`, parentheses, `WITH`)
- `.../governance/NoticeCheck.kt` and `courses.governance.gradle.kts` - the NOTICE mirror check and its `check` wiring
- `governance/dependency-licences.json` - the policy
- `NOTICE` - third-party attributions and Axoniq terms
- `gradle/libs.versions.toml`, `build-logic/build.gradle.kts` - plugin version and dependency, functionalTest inputs for the policy and NOTICE
- `GradleFixture.kt` - the standard fixture copies the policy
- `DependencyLicenceCheckFunctionalTest.kt`, `NoticeCheckFunctionalTest.kt` - TestKit cases

## Decisions Made

- Project dependencies are skipped by the `?project_path=` purl qualifier, not by a group in the policy (see deviation 1).
- No exceptions beyond the five Axoniq artifacts: dual-licensed components pass through their permissive alternative.
- The policy validates itself (no pattern exceptions, no blank reasons, no alias to a forbidden licence), so a governed-path review cannot be bypassed by a malformed entry.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Project dependencies appear in SBOMs as unlicensed components**
- **Found during:** Task 1
- **Issue:** Each module's SBOM lists the other modules of the build (for example `io.github.aanbrn.courses:contracts-kernel:unspecified`) as components with no licence entry, which the fail-closed rule rejected. The first full `functionalTest` run failed 7 fixture tests on `fixture:platform:unspecified` for the same reason.
- **Fix:** `DependencyLicenceCheck` marks a component as a project dependency when its purl carries `?project_path=` (added by the plugin to project dependencies only) and does not judge it; its licence is the repository's `LICENSE`. A group-based `firstPartyGroups` entry was tried first and dropped: it would not hold in fixtures, and it is closer to the family-wide exemption the plan prohibits. A component with the same shape but no such qualifier still fails (`a project dependency without a licence is not judged, the same shape from outside is`).
- **Files modified:** `DependencyLicenceCheck.kt`, `LicencePolicy.kt`
- **Verification:** `functionalTest` Conventions and TestSuites classes (7 failures) green; full suite green
- **Committed in:** `39994ed`

**2. [Rule 3 - Blocking] Detekt limits required splitting the check**
- **Found during:** Task 1
- **Issue:** `ReturnCount` and function-size limits fail the build on a single-file implementation.
- **Fix:** the plan lists only `DependencyLicenceCheck.kt`; the policy rules and the expression parser live in `LicencePolicy.kt` and `LicenceExpression.kt`, and the NOTICE tests in `NoticeCheckFunctionalTest.kt` instead of the licence test class. All are in the same package or directory.
- **Files modified:** the four files above
- **Verification:** `:build-logic:detekt` green
- **Committed in:** `39994ed`, `c60528a`

**3. [Rule 1 - Bug] Flaky configuration-cache assertion in the new library-convention test**
- **Found during:** Task 2 GREEN run
- **Issue:** `Reusing configuration cache` was asserted on a second TestKit run, but Gradle reported "configuration cache cannot be reused because system property 'platform.random.idempotence.check.rate' has changed", a Gradle-internal property that varies between runs. The existing `ConventionsFunctionalTest` carries the real reuse assertion.
- **Fix:** removed the text assertion; the test still proves the module writes its SBOM and that the second run is `UP_TO_DATE` or `FROM_CACHE`, with configuration cache and isolated projects flags on every run (the fixture adds them).
- **Files modified:** `DependencyLicenceCheckFunctionalTest.kt`
- **Verification:** 17/17 green, full suite green
- **Committed in:** `5492153`

---

**Total deviations:** 3 auto-fixed (2 blocking, 1 bug)
**Impact on plan:** All needed for a green, honest build. No scope creep; the policy format gained no keys beyond the plan (`allowed`, `forbidden`, `aliases`, `exceptions`).

## Issues Encountered

- `ConventionsFunctionalTest > a second run reuses the configuration cache` could be exposed to the same `platform.random.idempotence.check.rate` toggle; it was green in the final run. Not changed here (out of scope), noted for the verifier in case it flakes.
- The `courses.licences` convention is outside the governed boundary until plan 02-20 (T-02-59, accepted in the plan).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-20 can govern `courses.licences.gradle.kts`, make CI name `checkDependencyLicences` and add the tooling-inventory note; both tasks are registered (`checkDependencyLicences` per module, `checkNotice` on the root).
- Phase 5 adds the npm half: a CycloneDX document for `package.json` and `pnpm-lock.yaml` can be fed to another `DependencyLicenceCheck` instance with the same policy and NOTICE.
- Phase 6 can reuse `build/licences/bom.json` for image SBOMs.
- The Axoniq wording in `NOTICE` and the exception reasons is a legal statement that a human should read once (coverage item D5).

## Self-Check: PASSED

- Created files exist: NOTICE, governance/dependency-licences.json, courses.licences.gradle.kts, DependencyLicenceCheck.kt, LicencePolicy.kt, LicenceExpression.kt, NoticeCheck.kt, both TestKit classes (verified with `[ -f ]`).
- Commits exist on this branch: 39994ed, c60528a, 5492153.
- Acceptance criteria: `io.axoniq` exception count 5 (>= 2); every exception has a reason longer than 20 characters; wildcard exceptions 0; `org.cyclonedx.bom` count 1; `NOTICE` carries the terms URL (1); `checkNotice` appears 3 times in `courses.governance.gradle.kts`; `DependencyLicenceCheckFunctionalTest` result XML reports 17 tests, 0 failures; `reuse lint` exits 0.
- Plan verification: `checkDependencyLicences`, `checkNotice build` and `:build-logic:functionalTest` all pass; `.github/workflows/ci.yml`, `docs/tooling.md`, `governance/governed-paths.txt` and `.claude/settings.json` are unchanged.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-23*
