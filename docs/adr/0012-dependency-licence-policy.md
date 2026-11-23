---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0012: Dependency licences are checked per module against an allowlist, with the Axoniq terms as explicit exceptions

## Context and Problem Statement

The project is MIT-licensed and builds on open-source components and on Axoniq Framework modules that are not open source:
they are free for development, demonstrations and CI, and need a licence in production (CICD-09). Every dependency
licence must be reported and checked in CI, the Axoniq commercial terms must be listed and justified, and a `NOTICE` file
must carry the attributions.

Four questions needed an answer:

- Which source describes the licences of what actually ships?
- Which licences pass, and what happens to dual-licensed components?
- How are the commercial Axoniq modules admitted without weakening the rule for everything else?
- What is in scope, and which half of the requirement belongs to a later phase?

## Decision Drivers

- The check reads exact versions and licences from one machine-readable document, which Phase 6 needs anyway for image SBOMs.
- It works with the Gradle configuration cache and isolated projects, which this build enforces.
- A component the policy does not recognise must fail, never pass silently.
- The exceptions are few, visible and justified in writing, and they cannot be widened by a pattern.

## Considered Options

- A CycloneDX SBOM per module plus a small own check against the allowlist (chosen), versus the jk1 Gradle License Report
  plugin, versus the Trivy licence scanner.
- One SBOM aggregated at the root project, versus one SBOM per module.
- An allowlist of permissive licences and weak copyleft with per-artifact exceptions (chosen), versus permissive licences
  only with exceptions.
- Exceptions by pattern or group, versus exceptions per artifact.

## Decision Outcome

Chosen option: "a CycloneDX SBOM per module and a cacheable own check against `governance/dependency-licences.json`",
because the SBOM is the document the images need in Phase 6, the CycloneDX plugin was verified to work under the
configuration cache and isolated projects when applied per module, and the own check can be strict where the scanners are
lenient. The jk1 report plugin stays the documented fallback and was not needed.

- **Per module, not aggregated.** `courses.licences` applies `org.cyclonedx.bom` in each Kotlin module through
  `courses.kotlin-library`, never from the root project or `allprojects`. A root task cannot read the outputs of other
  projects under isolated projects, so aggregating at the root was rejected. Each module writes
  `build/licences/bom.json` from its runtime classpaths only (the compiler, analysis and documentation classpaths are not
  part of the product), and `checkDependencyLicences` runs from `check` in every module.
- **The policy (D-31).** `allowed` lists permissive licences and weak copyleft: Apache-2.0, MIT, MIT-0, BSD-2-Clause,
  BSD-3-Clause, ISC, EPL-1.0, EPL-2.0 and MPL-2.0. `forbidden` lists the families GPL and AGPL.
- **OR semantics.** A component passes when any of its licence alternatives is allowed or an exception covers it. That is how
  JNA (Apache-2.0 or LGPL), logback (EPL-2.0 or LGPL) and `jakarta.annotation-api` (EPL-2.0 or GPL with classpath
  exception) pass without an exception: each has an allowed alternative. Which alternative is used is stated in `NOTICE`.
- **Fail closed.** A component with no licence entry, a name the policy does not know, only forbidden alternatives or a
  malformed expression fails, and the failure names `group:name:version` and the licence text, sorted so two runs report the
  same text. Project dependencies of the same build are recognised by the `?project_path=` purl qualifier of the SBOM and are
  not judged; no group-based exemption exists.
- **Exceptions per artifact.** Five Axoniq artifacts are listed with the verbatim licence text `Axoniq Terms of Service`
  and a written reason: `io.axoniq:axonserver-connector-java`, `io.axoniq.framework:axon-server-connector`,
  `io.axoniq.framework:axoniq-distributed-messaging`, `io.axoniq.framework:axoniq-framework-bom` and
  `io.axoniq.framework:axoniq-testcontainer` (test scope). An exception matches one artifact, never a pattern. The policy
  refuses to be weakened: a wildcard exception, a blank text or reason, an alias that resolves to a forbidden licence,
  and an allowed identifier that matches a forbidden family make it invalid.
- **`NOTICE` mirrors the exceptions.** It carries the third-party attributions and the Axoniq terms, and `checkNotice` fails
  when an exception artifact or the terms URL is missing, so a new exception cannot land without its attribution.
- **Governed gate.** `courses.licences.gradle.kts` is a governed path with an ask rule, so changing it needs the
  `governance-approved` label (plan 02-20). CI names `checkDependencyLicences` on the `build` job's Gradle step, so a build
  that lost the task fails with "Task not found" instead of skipping the gate. The detekt rule set is governed in the same
  way.

### Scope

- **JVM half, now.** Phase 2 delivers the check for every Kotlin module.
- **npm half, Phase 5.** npm project dependencies join the same policy when the frontend's `package.json` lands. The owner of
  that half is recorded in the REQUIREMENTS.md traceability row `CICD-09 | Phase 2 (JVM), Phase 5 (npm)` and in the ROADMAP
  Cross-Cutting row `CICD-09 (npm half), UPS-02 (index on the site) | Phase 5`. A second source joins by producing a
  CycloneDX document and registering another instance of the same check against the same policy (D-30).
- **Developer tools are out of scope.** The mise-pinned npm developer tools, the Context7 and Playwright MCP servers
  (`npm:@upstash/context7-mcp` and `npm:@playwright/mcp`), are outside the CICD-09 licence check, because they are developer
  tooling, neither shipped with nor linked into the system. They are reviewed through the AIDEV-04 approval gate and listed
  in `docs/tooling.md` (ADR-0014).

### Consequences

- Good, because the SBOM that the check reads is the one the image SBOMs will reuse, so the licence statement and the
  supply-chain statement do not drift.
- Good, because an unknown licence fails the build at the introducing commit, and the exceptions are a short, reasoned,
  mirrored list.
- Bad, because the check runs once per module, so the SBOM task runs in every module even where the dependency set is the
  same.
- Bad, because the check trusts the licence metadata of POMs. A wrong POM entry would pass, which no scanner can fix.
- Bad, because the exceptions mean that the system cannot run in production without an Axoniq licence. That is the intended
  position and is stated in `NOTICE`, but the repository is for development and CI use only.

### Confirmation

- `checkDependencyLicences` runs from `check` in every Kotlin module, and CI names it. `checkNotice` and the policy's
  self-validation run in the same `check`. The TestKit suite in `build-logic` covers 17 licence and 5 `NOTICE` cases.
- `checkClaudeAskRules` and the governed-paths list keep `courses.licences.gradle.kts` behind the approval label.
- `checkToolingInventory` keeps the CycloneDX row and the scope note of `docs/tooling.md` current.

## Pros and Cons of the Options

### CycloneDX SBOM plus an own check

- Good, because the SBOM doubles as the image SBOM input and the check is strict and testable.
- Bad, because the project owns a small check.

### jk1 Gradle License Report

- Good, because it is a ready-made licence report.
- Bad, because it is a second source of dependency data next to the SBOM, and its fit with isolated projects was the open
  question. It is the fallback.

### Trivy licence scanner

- Good, because it scans images and file systems alike.
- Bad, because it is a binary the project would pin and verify, and it would not give the per-module allowlist with written
  exceptions that the requirement asks for.

### Aggregation at the root

- Good, because it is one report.
- Bad, because it does not work under isolated projects, which forbid reading other projects' task outputs.

### Permissive plus weak copyleft versus permissive only

- Good, because allowing EPL and MPL lets JUnit, logback and others through without per-artifact exceptions.
- Bad, because weak copyleft carries obligations for modified files of the component, which an allowlist entry accepts.
- Permissive only would force an exception for each EPL component, including JUnit, and make the exception list long enough to
  be skimmed.

### Exceptions by pattern or group

- Good, because one line would cover a family such as `io.axoniq.*`.
- Bad, because a new artifact of the family would pass without a decision, so the policy rejects a pattern as invalid.

## More Information

- Decisions: D-30 and D-31, and CICD-09. The npm tools scope follows plan 02-20.
- Revisit the policy when a component with a new licence family enters, when the Axoniq terms change, or when Phase 5 adds the
  npm source (the allowlist is then shared, and the scope section here is updated).
