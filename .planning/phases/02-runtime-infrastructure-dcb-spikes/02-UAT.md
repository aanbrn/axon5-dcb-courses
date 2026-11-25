---
status: complete
phase: 02-runtime-infrastructure-dcb-spikes
source: [02-VERIFICATION.md]
started: 2026-11-23T06:58:20.881Z
updated: 2026-11-25T09:30:00.000Z
---

## Current Test

[testing complete]

## Tests

### 1. Open a pull request from this branch and let GitHub Actions run the full workflow
expected: Jobs integration-test, compose-smoke, doctor and upstream-records pass on a GitHub runner, and ci-success (which needs all of them) is green
result: issue
reported: "Wait, actually. The Compose smoke test failed in CI."
severity: blocker
evidence: "PR #21 at head 729dcff: Compose smoke test FAILURE (run 45532702352, job 138931678592), so ci-success FAILURE; all other jobs SUCCESS"

### 2. In Claude Code run /plugin and confirm axoniq-app-development@axoniq and gradle-skills@gradle-skills install from the SHA-pinned marketplaces in .claude/settings.json
expected: Both plugins install and list their skills; the 'global:' agent_skills entries in .planning/config.json resolve through gsd-tools query agent-skills
result: issue
reported: "I can see the skills, but they are inactive, and an update attempt says they are not installed."
severity: major

### 3. Read the Axoniq wording in NOTICE, docs/quick-start.md and ADR-0005 against the current Axoniq Terms of Service page
expected: The statements 'free for development, demonstrations and CI; a licence is required in production' and the 12-hour unregistered window are accurate and acceptable to the author
result: pass

### 4. Read the re-written Considered Options of ADR-0001 and ADR-0002
expected: The structure now follows MADR with decisions, dates and statuses unchanged (02-19 prohibition)
result: pass

### 5. Post the forum or support drafts for UP-0006 (empty Platform token) and UP-0013 (dcbContext flag), and decide on UP-0010 and UP-0011, then record the URLs and set status to reported
expected: Each record moves to status reported with a non-empty upstream list and a reported date, and checkUpstreamRecords stays green. Five GitHub issues exist and were checked read-only: AxonFramework#5326, #5327, #5328, axoniq-framework#811, #812.
result: issue
reported: "What do you want from me here? If it is not done, do it yourself."
severity: major
agreed_resolution: "UP-0006 and UP-0013: Claude publishes the drafts on discuss.axoniq.io through the Playwright browser after the author signs in, showing the final text before each post, then records URLs and sets status reported. UP-0010: not published now; check whether a newer CodeQL bundle covers Kotlin 2.4.21 and keep status open. UP-0011: re-run the reproduction on a throwaway clean colima profile and, if the stall reproduces, open an issue in abiosoft/colima with gh after the author approves the text."

### 6. Run docker compose up -d --wait and ./gradlew integrationTest on a Docker Desktop machine (not colima)
expected: Stack healthy and tests green
result: skipped
reason: "Docker Desktop is not installed on the author's machine and the author declined installing it (2026-11-23); colima is the verified engine"

## Summary

total: 6
passed: 2
issues: 3
pending: 0
skipped: 1
blocked: 0

## Gaps

- gap_id: G-02-1
  resolved_by: "plans 02-24, 02-30, 02-31 (429 retry wrapper, CI mirror; CI green at 07f309d and 6d6cd2a)"
  truth: "Jobs integration-test, compose-smoke, doctor and upstream-records pass on a GitHub runner, and ci-success (which needs all of them) is green"
  status: resolved
  reason: "User reported: Wait, actually. The Compose smoke test failed in CI."
  severity: blocker
  test: 1
  root_cause: "Infrastructure flake, not a regression. Maven Central answered the compose-smoke runner with 429 Too Many Requests while Gradle resolved the build-logic plugin classpath (kotlin-stdlib 2.4.21 for kotlin-dsl, kotlinx-serialization-bom 1.6.0), Gradle disabled the repository and failed in settings.gradle.kts before any test ran. Between the green run at 07fb5d1 and the red run at 729dcff only .gitignore changed; four other Gradle jobs in the same run resolved the same artifacts in the same minute. Standing exposure: on pull requests every setup-gradle step is cache-read-only and main has no compose-smoke, integration-test, functional-test or doctor job yet, so these jobs always start with an empty cache and download everything from Maven Central with no fallback."
  artifacts:
    - path: ".github/workflows/ci.yml"
      issue: "compose-smoke (and integration-test, functional-test, doctor) restore only caches written on main, which has no such jobs, so every PR run resolves from scratch"
    - path: "build-logic/settings.gradle.kts"
      issue: "gradlePluginPortal() and mavenCentral() both end at repo.maven.apache.org; no mirror or retry when it returns 429"
  missing:
    - "Re-run the failed compose-smoke job of run 45532702352 to confirm the flake and get ci-success green at the PR head"
    - "Optionally reduce exposure: a Maven Central mirror as fallback, or one retry with backoff around the Gradle step of the empty-cache jobs (relaxing cache-read-only on PRs is an author decision because of cache poisoning)"
  debug_session: ".planning/debug/compose-smoke-ci-failure.md"

- gap_id: G-02-2
  resolved_by: "plan 02-25 (doctor claude-plugins check, docs); plugins installed at project scope"
  truth: "Both plugins install and list their skills; the 'global:' agent_skills entries in .planning/config.json resolve through gsd-tools query agent-skills"
  status: resolved
  reason: "User reported: I can see the skills, but they are inactive, and an update attempt says they are not installed."
  severity: major
  test: 2
  root_cause: "The two plugins are enabled but were never installed. .claude/settings.json registers the SHA-pinned marketplaces (that worked: known_marketplaces.json has both refs) and sets enabledPlugins, but Claude Code deliberately does not install or record a plugin that only repository settings enable when its marketplace is a remote github source; each contributor must run 'claude plugin install <id> --scope project'. ~/.claude/plugins/installed_plugins.json has no record for either plugin, so Claude Code shows them as on in project settings but not installed (inactive) and the update fails with 'Plugin <id> is not installed'. Contributing cause: docs/tooling.md, UAT test 2 and scripts/doctor.sh treat enabled as installed and nothing tells a contributor to run the install or checks it."
  artifacts:
    - path: "docs/tooling.md"
      issue: "'How skills are installed' describes register plus enable as the whole install; no per-contributor install step; verdicts say Installed"
    - path: "scripts/doctor.sh"
      issue: "check_claude_code checks only the Claude Code version, not that each enabledPlugins id has a project-scope install record (governed file)"
    - path: "docs/adr/0004-gsd-configuration.md"
      issue: "caveat says the /plugin step is pending; stays true until the install runs"
    - path: ".claude/settings.json"
      issue: "correct as is; enabling cannot install by design (governed file)"
  missing:
    - "Author runs 'claude plugin install axoniq-app-development@axoniq --scope project' and 'claude plugin install gradle-skills@gradle-skills --scope project', then /reload-plugins; check git status for any rewrite of .claude/settings.json"
    - "Document the per-contributor project-scope install step in docs/tooling.md (and quick-start) and reword the verdicts"
    - "Add a doctor check that warns when an enabledPlugins id has no project-scope install record, with the install command as the hint"
    - "Verify that the plugins load in a Claude Code session started inside a git worktree (projectPath is matched exactly); if not, document --scope user as the alternative"
  debug_session: ".planning/debug/plugins-inactive-not-installed.md"

- gap_id: G-02-5
  resolved_by: "plans 02-26..02-29 (UP-0010 and UP-0011 decided open; UP-0006 topic 6851 and UP-0013 topic 6853 reported)"
  truth: "Each upstream record moves to status reported with a non-empty upstream list and a reported date, and checkUpstreamRecords stays green"
  status: resolved
  reason: "User reported: What do you want from me here? If it is not done, do it yourself."
  severity: major
  test: 5
  root_cause: "Process gap, not a code defect. Four upstream drafts were never published because publication needs the author's approval (UPS-03), and two of them (UP-0010, UP-0011) still lack a confirmed reproduction. During UAT on 2026-11-25 the author approved publication: UP-0006 was submitted to discuss.axoniq.io (category Axon Server) and is held in the moderation queue (pending post id 70, author account at trust level 0, so no public URL yet); UP-0013 was refused with 'Access Denied' because a trust level 0 account cannot open a second topic while the first is pending."
  artifacts:
    - path: "docs/upstream/0006-empty-platform-token.md"
      issue: "status open; becomes reported with the topic URL once the moderators approve the pending post"
    - path: "docs/upstream/0013-axon-server-dcb-context-flag.md"
      issue: "status open; post after UP-0006 is approved (account then allowed a second topic), then record the URL"
    - path: "docs/upstream/0010-codeql-kotlin-extractor.md"
      issue: "agreed not to publish now; check whether a newer CodeQL bundle covers Kotlin 2.4.21 and keep status open"
    - path: "docs/upstream/0011-colima-aaaa-dns.md"
      issue: "reproduction on a clean colima profile not yet confirmed"
  missing:
    - "After moderator approval: record the UP-0006 topic URL, set status reported and the reported date"
    - "Publish UP-0013 on discuss.axoniq.io after UP-0006 is approved, then record its URL, status reported and reported date"
    - "UP-0010: check the latest github/codeql-action bundle's documented Kotlin range; update the record's notes, keep status open"
    - "UP-0011: re-run the Go lookup on a throwaway colima profile without --dns; if the stall reproduces, open an issue in abiosoft/colima with gh after the author approves the text and record it as reported; if not, update the record with the finding"
    - "checkUpstreamRecords stays green after every record change"
  debug_session: ""

## Deferred Follow-Ups

- id: DF-02-1
  gap: G-02-5
  items: "UP-0006 recorded as reported (plan 02-28) and UP-0013 posted and recorded (plan 02-29)"
  deferred: 2026-11-25
  reason: "Author decision: forum moderation is an external event of unknown duration and does not hold Phase 2. UP-0006 is held as pending post 70 and the account is on hold (https://discuss.axoniq.io/t/account-temporarily-on-hold/6850); the author wrote to devrel@axoniq.io. Both plans are status halted; their records carry a dated publication status."
  done_in_phase_2: "UP-0010 (plan 02-26: bundle range documented, record stays open) and UP-0011 (plans 02-26 and 02-27: not reproduced on a clean profile, nothing published) are resolved."
  tracked_in: ".planning/STATE.md Pending Todos"
  resolved: "2026-11-25: the forum account was released the same day; plan 02-28 recorded UP-0006 as reported (topic 6851) and plan 02-29 posted UP-0013 (topic 6853); both plans complete"
