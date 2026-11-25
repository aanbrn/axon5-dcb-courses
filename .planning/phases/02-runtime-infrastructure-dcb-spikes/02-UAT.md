---
status: complete
phase: 02-runtime-infrastructure-dcb-spikes
source: [02-VERIFICATION.md]
started: 2026-11-23T06:58:20.881Z
updated: 2026-11-25T02:53:04.497Z
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
  truth: "Jobs integration-test, compose-smoke, doctor and upstream-records pass on a GitHub runner, and ci-success (which needs all of them) is green"
  status: failed
  reason: "User reported: Wait, actually. The Compose smoke test failed in CI."
  severity: blocker
  test: 1
  artifacts: []
  missing: []

- gap_id: G-02-2
  truth: "Both plugins install and list their skills; the 'global:' agent_skills entries in .planning/config.json resolve through gsd-tools query agent-skills"
  status: failed
  reason: "User reported: I can see the skills, but they are inactive, and an update attempt says they are not installed."
  severity: major
  test: 2
  artifacts: []
  missing: []

- gap_id: G-02-5
  truth: "Each upstream record moves to status reported with a non-empty upstream list and a reported date, and checkUpstreamRecords stays green"
  status: failed
  reason: "User reported: What do you want from me here? If it is not done, do it yourself."
  severity: major
  test: 5
  artifacts: []
  missing: []
