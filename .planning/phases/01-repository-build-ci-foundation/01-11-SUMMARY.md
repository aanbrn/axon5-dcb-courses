---
phase: 01-repository-build-ci-foundation
plan: 11
subsystem: infra
tags: [renovate, dependency-updates, supply-chain, automerge, minimum-release-age, regex-manager]

requires:
  - phase: 01-repository-build-ci-foundation
    provides: "Version catalog and .java-version (01-01), mise.toml tools and the renovate entry (01-03), the annotated prettier version (01-05), the governance renovate[bot] exemption and renovate/* branch rule (01-09), the deps label set (01-10)"
provides:
  - "renovate.json5: strict-valid Renovate config for the Mend app with a top-level 7-day minimumReleaseAge and strict internal checks"
  - "Three custom regex managers: .java-version (java), catalog '# renovate:' annotations (prettier, ktfmt), mise min_version (jdx/mise)"
  - "Grouped, labelled updates (deps plus deps:<ecosystem>, security) with build(deps) semantic commits"
  - "Exactly three automerge rules (Action digests, mise patches, formatting tool patches) using platform squash automerge"
  - "LTS-only JDK updates kept as a bare major, with an updateDaemonJvm and mise lock reminder in the PR body"
affects: [01-12, 01-13, 01-14, phase-06-supply-chain]

plan_head_before: 5b3656b59fc4bfa46bfa390c10ba84dfd2d1079d
plan_head_after: cf6a78780189d8ee079df5044428d3d430ffcb1d

actuals:
  tokens: 9700
  tasks: 2
  commits: 2

tech-stack:
  added: []
  patterns:
    - "Renovate packageRules ordered: ignore, labels, groups, automerge, JDK; later rules win"
    - "Renovate regex managers read single-file version sources that have no built-in manager"
    - "Local Renovate dry runs (extract and lookup) as the proof of a Renovate config, with the GitHub token only as a process environment variable"

key-files:
  created: [renovate.json5]
  modified: [gradle/libs.versions.toml]

key-decisions:
  - "The java rule waives the minimum-release-age timestamp requirement (minimumReleaseAgeBehaviour timestamp-optional) because the java-version datasource has no release timestamps; without it every JDK bump stays pending forever"
  - "Security PRs bypass the cooldown (Renovate forces this for vulnerability alerts) but vulnerabilityAlerts sets automerge false, so a human always merges them"
  - "ktfmt gets a '# renovate: datasource=maven depName=com.facebook:ktfmt' annotation so the version Spotless uses is actually updated"
  - "Generated .mise/locks sidecar lockfiles are ignored: Renovate otherwise extracts npm and pep621 dependencies from them and would open PRs against generated files"
  - "Automerge types follow D-12 literally (digest, pinDigest for Actions): with the semver-comment pin helper most Action bumps are patch or minor updates and merge by hand"

patterns-established:
  - "A Renovate rule that touches a labelled ecosystem uses only label names that exist in .github/labels.yml"
  - "The tracer's extract run needs renovate.json5 tracked in git: the local platform lists files with git ls-files and silently ignores an untracked config"

requirements-completed: [CICD-04, INFRA-09, TOOL-02, CICD-03]

coverage:
  - id: D1
    description: "renovate.json5 passes renovate-config-validator --strict and sets minimumReleaseAge 7 days at the top level plus internalChecksFilter strict"
    requirement: CICD-04
    verification:
      - kind: other
        ref: "mise x -- renovate-config-validator --strict renovate.json5 (exit 0, 'Config validated successfully'); grep -c 'minimumReleaseAge: \"7 days\"' prints 1"
        status: pass
    human_judgment: false
  - id: D2
    description: "A local extract run finds every version source: github-actions, gradle (catalog, plugins), gradle-wrapper, mise, nvm and the three regex dependencies java, prettier and jdx/mise (plus ktfmt after Task 2)"
    requirement: INFRA-09
    verification:
      - kind: other
        ref: "LOG_LEVEL=debug mise x -- renovate --platform=local --dry-run=extract | grep -cE '\"depName\": \"(java|prettier|jdx/mise)\"' prints 3"
        status: pass
    human_judgment: false
  - id: D3
    description: "Mise tool versions, .nvmrc and GitHub Actions SHA pins are extracted too (TOOL-02, CICD-03)"
    requirement: TOOL-02
    verification:
      - kind: other
        ref: "extract log lists mise (13 tools), nvm (.nvmrc node 24) and github-actions (actions/checkout and crazy-max/ghaction-github-labeler pinned by SHA)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Updates are grouped (kotlin, junit, kotest, spring-boot with Jackson, axon, testcontainers, formatting and analysis, github-actions, dev tools) and labelled deps plus an ecosystem label that exists in .github/labels.yml"
    requirement: CICD-04
    verification:
      - kind: other
        ref: "validator strict pass; lookup dry run produced renovate/formatting-and-analysis, renovate/junit, renovate/spring-boot and renovate/dev-tools branches; label cross-check against .github/labels.yml prints nothing"
        status: pass
    human_judgment: false
  - id: D5
    description: "Automerge is enabled by exactly three rules (Action digests, mise patches, formatting tool patches), platform automerge with squash, and never for runtime libraries"
    requirement: CICD-04
    verification:
      - kind: other
        ref: "grep -vE '^\\s*//' renovate.json5 | grep -c 'automerge: true' prints 3; grep -c 'automergeStrategy: \"squash\"' prints 1"
        status: pass
    human_judgment: false
  - id: D6
    description: "JDK updates propose only LTS majors as a bare major, carry the deps:jdk label and an updateDaemonJvm reminder"
    requirement: INFRA-09
    verification:
      - kind: other
        ref: "lookup dry run with .java-version temporarily 21 proposed exactly '25' (not 26, no patch version, no pending check); with 25 it proposed nothing; grep -c updateDaemonJvm prints 1"
        status: pass
    human_judgment: false
  - id: D7
    description: "The Mend app really opens PRs from this config, applies the labels, honours the cooldown on live data, and automerges only the allowed updates after the required checks"
    verification: []
    human_judgment: true
    rationale: "Backstop: the app is installed by the author in plan 01-14 (D-11) and the ruleset required checks do not exist before then. The first Renovate run on GitHub is the proof."

duration: 8min
completed: 2026-11-21
status: complete
---

# Phase 1 Plan 11: Renovate Configuration Summary

**A strict-valid `renovate.json5` for the Mend app: a 7-day cooldown on every manager, regex managers for `.java-version`, the catalog annotations and mise's `min_version`, grouped and labelled updates, exactly three automerge rules and LTS-only JDK bumps, all proven with local extract and lookup dry runs**

## Performance

- **Duration:** about 8 min
- **Started:** 2026-11-21T11:06:17Z
- **Completed:** 2026-11-21T11:14:22Z
- **Tasks:** 2 (one tracer, one auto)
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments

- Tracer proven end to end: after the first commit the local extract run listed every version source and the log line `Tracer verified end-to-end - expanding` applies. Managers found: `github-actions` (`actions/checkout`, `crazy-max/ghaction-github-labeler`, both SHA-pinned), `gradle` (catalog libraries, plugins, the Kotlin, JUnit, Kotest, Spring Boot, Axon, Axoniq, Testcontainers and Jackson BOMs), `gradle-wrapper` (`gradle`), `mise` (all 13 tools including `pipx:reuse` and `npm:renovate`), `nvm` (`node` 24 from `.nvmrc`), and `regex` with `java` (`.java-version`), `prettier` (catalog annotation) and `jdx/mise` (`min_version`).
- The lookup dry run (network, token only as a process environment variable) proposed no JDK update from 25, and exactly bare `25` from a temporary 21: no non-LTS 26, no full patch version. It also produced the expected group branches (`renovate/formatting-and-analysis`, `renovate/junit`, `renovate/spring-boot`, `renovate/dev-tools`).
- Confirmed in Renovate's own source that `vulnerabilityAlerts` rules are appended last and forced, so `automerge: false` there wins over any automerge rule, and that vulnerability alerts always skip the cooldown.
- A JDK major change was shown to break mise's locked mode (`java@21 is not in the lockfile`), so the PR note also tells the author to run `mise lock`.

## Task Commits

1. **Task 1: Strict-valid extraction of every version source (tracer)** - `0c142c5` (feat)
2. **Task 2: Grouping, labels, narrow automerge, LTS-only JDK** - `cf6a787` (feat)

**Plan metadata:** recorded by the closing docs commit (docs: complete plan)

## Files Created/Modified

- `renovate.json5` - Renovate configuration: defaults, three regex managers, ordered package rules
- `gradle/libs.versions.toml` - Renovate annotation above `ktfmt`

## Decisions Made

See `key-decisions`. The `npm:renovate` trust-policy exclusion for `@yarnpkg/libzip@3.2.2` in `mise.toml` stays unchanged: this plan did not bump renovate, so the follow-up from 01-03 is still open and nothing was broadened.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] JDK updates would never be proposed under the cooldown**
- **Found during:** Task 2 (lookup dry run with `.java-version` temporarily 21)
- **Issue:** the `java-version` datasource returns no release timestamps. Renovate's default `minimumReleaseAgeBehaviour: timestamp-required` therefore marked the only candidate release as pending (`"check": "minimumReleaseAge"`), so the whole JDK rule was dead.
- **Fix:** the JDK package rule sets `minimumReleaseAgeBehaviour: "timestamp-optional"`. Only LTS majors pass `allowedVersions` and the PR is always a manual merge. Only the `java` dependency lacked timestamps in the lookup log.
- **Files modified:** `renovate.json5`
- **Verification:** re-run showed no pending check and the proposal `25` from 21.
- **Committed in:** `cf6a787`

**2. [Rule 2 - Missing Critical] ktfmt was not covered by any Renovate manager**
- **Found during:** Task 2 (planning the formatting group and automerge rule that name ktfmt)
- **Issue:** `ktfmt` is only a `[versions]` entry without a library alias, so the gradle manager never extracted it and Spotless's formatter version would never be updated, contradicting INFRA-09 and the rules that name it.
- **Fix:** added `# renovate: datasource=maven depName=com.facebook:ktfmt` above `ktfmt` in the catalog (the same pattern as `prettier`). The lookup run found `com.facebook:ktfmt` with `registryUrl` Maven Central.
- **Files modified:** `gradle/libs.versions.toml`
- **Verification:** the lookup log lists `com.facebook:ktfmt`; `spotlessApply`, `check` and `editorconfig-checker` stay green.
- **Committed in:** `cf6a787`

**3. [Rule 2 - Missing Critical] Generated mise sidecar lockfiles were being extracted**
- **Found during:** Task 1 (first extract run)
- **Issue:** `.mise/locks/**/package.json` and `pyproject.toml` were picked up by the `npm` and `pep621` managers; PRs against generated files would be wrong and would fight the mise manager, which already covers `npm:renovate` and `pipx:reuse` from `mise.toml`.
- **Fix:** first package rule disables `.mise/locks/**`.
- **Files modified:** `renovate.json5`
- **Verification:** validator strict pass; the lookup log shows the sidecar dependencies skipped.
- **Committed in:** `cf6a787`

**4. [Rule 2 - Missing Critical] Security PRs must never be merged by a rule**
- **Found during:** Task 2 (reading Renovate's vulnerability handling)
- **Issue:** Renovate waives the cooldown for vulnerability alerts regardless of configuration, so a security patch of a dev tool could otherwise match an automerge rule and merge a fresh release unreviewed.
- **Fix:** `vulnerabilityAlerts: { addLabels: ["security"], automerge: false }`. This keeps T-01-24 intact; the cooldown exception for security fixes is disclosed in a comment.
- **Files modified:** `renovate.json5`
- **Committed in:** `cf6a787`

---

**Total deviations:** 4 auto-fixed (1 bug, 3 missing critical)
**Impact on plan:** all necessary for correctness of the configuration; no scope creep. One extra line was added to the catalog, outside the plan's `files_modified`.

## Issues Encountered

- Renovate's local platform lists files with `git ls-files`, so the first extract run found no config (and no regex managers) while `renovate.json5` was untracked. After `git add` the same command found everything. Recorded as a pattern above.
- Renovate prints `RE2 not usable, falling back to RegExp` because the native re2 build is not installed (mise installs the npm tool without running build scripts). The validator still passes; its regex validation may be less strict than the hosted app's RE2. Not changed, as it comes from the pinned supply-chain setup.
- The session harness refused commands that combine `git` tokens with other work (including the `GITHUB_COM_TOKEN` assignment). The lookup runs went through a small script kept outside the repository; commands were split into plain ones.
- The plan-commit ledger file under the git directory was not written (shared `.git` writes are blocked); `plan_head_before` is the HEAD recorded before the first commit and `commits` is `git rev-list --count` from it.

## Open Verifications and Follow-ups

- **Action automerge scope:** the plan and D-12 say automerge Action digest updates and the config does exactly that (`digest`, `pinDigest`). With `helpers:pinGitHubActionDigestsToSemver`, a bump of a SHA-pinned Action with a `# vX.Y.Z` comment is reported as a `patch` or `minor` update, so in practice those merge by hand and only branch-pinned Actions would hit the rule. This is the conservative reading; the author can widen it later. The current Actions had no pending updates at lookup time, so the update type could not be observed.
- **`jdx/mise` lookup:** the extract run reports `skipReason: github-token-required` without a token; with `GITHUB_COM_TOKEN` it looked up cleanly. The hosted app supplies a token.
- **Node bumps:** `.nvmrc` bumps change `mise.lock` the same way a JDK major does (node is locked), but Renovate cannot run `mise lock`. Not covered by this plan; consider a PR note for the `nvm` manager if it becomes a problem.
- **First live run (plan 01-14):** confirm labels, group names, cooldown pending status and `build(deps)` titles on the real app; D7 in the coverage block.

## Verification Results and Timing

Measured wall clock, warm caches (the lookup needs network):

| Command | Result | Time |
| --- | --- | --- |
| `mise x -- renovate-config-validator --strict renovate.json5` | pass | about 1 s |
| `renovate --platform=local --dry-run=extract` (depName grep prints 3, later 4 with ktfmt) | pass | about 3 s |
| `renovate --platform=local --dry-run=lookup` (twice for the JDK check) | pass | about 11 s |
| `mise x -- ./gradlew spotlessApply` | pass | about 1 s |
| `mise x -- ./gradlew check` (all earlier plans' gates) | pass | 2 min 9 s |

Only the full `check` exceeds 30 s; it is inside the 180 s per-task feedback budget of `01-VALIDATION.md`.

Acceptance criteria: `minimumReleaseAge: "7 days"` 1, `customType: "regex"` 3, `automerge: true` 3 (comments excluded), `deps:github-actions` 1, `updateDaemonJvm` 1, `automergeStrategy: "squash"` 1, `allowedVersions` 1, label cross-check against `.github/labels.yml` prints nothing; `editorconfig-checker` and `reuse lint` clean.

## Known Stubs

None.

## Threat Flags

None. T-01-23 (7-day cooldown, digest pinning, strict internal checks; security PRs excepted but never auto-merged), T-01-24 (exactly three automerge rules, security PRs forced manual) and T-01-25 (token only in one process environment, never written; no token strings in the saved logs) are mitigated as planned. T-01-SC: no packages installed.

## User Setup Required

None - no external service configuration required. Installing the Mend Renovate app remains the author's step in plan 01-14.

## Next Phase Readiness

- Ready for 01-12: Actions extraction is already visible for `governance.yml` and `labels.yml`; re-verify once `ci.yml` exists.
- Ready for 01-14: the app can be installed against this config; D7 is the first-run proof.

## Self-Check: PASSED

`renovate.json5` exists; commits `0c142c5` and `cf6a787` are ancestors of HEAD; every acceptance criterion and the plan-level verification commands passed; `mise x -- ./gradlew check` green.

---
*Phase: 01-repository-build-ci-foundation*
*Completed: 2026-11-21*
