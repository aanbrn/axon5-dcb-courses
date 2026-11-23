---
status: testing
phase: 02-runtime-infrastructure-dcb-spikes
source: [02-VERIFICATION.md]
started: 2026-11-23T06:58:20.881Z
updated: 2026-11-23T06:58:20.881Z
---

## Current Test

number: 1
name: Open a pull request from this branch and let GitHub Actions run the full workflow
expected: |
  Jobs integration-test, compose-smoke, doctor and upstream-records pass on a GitHub runner, and ci-success (which needs all of them) is green
awaiting: user response

## Tests

### 1. Open a pull request from this branch and let GitHub Actions run the full workflow
expected: Jobs integration-test, compose-smoke, doctor and upstream-records pass on a GitHub runner, and ci-success (which needs all of them) is green
result: [pending]

### 2. In Claude Code run /plugin and confirm axoniq-app-development@axoniq and gradle-skills@gradle-skills install from the SHA-pinned marketplaces in .claude/settings.json
expected: Both plugins install and list their skills; the 'global:' agent_skills entries in .planning/config.json resolve through gsd-tools query agent-skills
result: [pending]

### 3. Read the Axoniq wording in NOTICE, docs/quick-start.md and ADR-0005 against the current Axoniq Terms of Service page
expected: The statements 'free for development, demonstrations and CI; a licence is required in production' and the 12-hour unregistered window are accurate and acceptable to the author
result: [pending]

### 4. Read the re-written Considered Options of ADR-0001 and ADR-0002
expected: The structure now follows MADR with decisions, dates and statuses unchanged (02-19 prohibition)
result: [pending]

### 5. Post the forum or support drafts for UP-0006 (empty Platform token) and UP-0013 (dcbContext flag), and decide on UP-0010 and UP-0011, then record the URLs and set status to reported
expected: Each record moves to status reported with a non-empty upstream list and a reported date, and checkUpstreamRecords stays green. Five GitHub issues exist and were checked read-only: AxonFramework#5326, #5327, #5328, axoniq-framework#811, #812.
result: [pending]

### 6. Run docker compose up -d --wait and ./gradlew integrationTest on a Docker Desktop machine (not colima)
expected: Stack healthy and tests green
result: [pending]

## Summary

total: 6
passed: 0
issues: 0
pending: 6
skipped: 0
blocked: 0

## Gaps
