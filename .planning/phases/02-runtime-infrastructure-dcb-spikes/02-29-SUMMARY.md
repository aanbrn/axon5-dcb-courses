---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 29
subsystem: infra
tags: [upstream-records, axon-server, forum, gap-closure, ups-03, discourse]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: plan 02-28 (UP-0006 reported, forum account released on 2026-11-25)
provides:
  - UP-0013 published on discuss.axoniq.io (topic 6853, category Axon Server) and recorded as reported
  - the draft in the record carries the published text, including the link to the earlier 2025.2.0-EAP1 mention
affects: [upstream-records]

actuals:
  tokens: 2500
  tasks: 2
  # Measured at SUMMARY write time from plan_head_before: the record commit. The SUMMARY commit follows it.
  commits: 1

plan_head_before: 95dceb1b4e5697be71ba0a18d82c48331c62bdf7
plan_head_after: 540a0f406fa6448cb2178a18a96d8808c1a92d85

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - docs/upstream/0013-axon-server-dcb-context-flag.md

key-decisions:
  - "UP-0013 is posted as a new topic that links the earlier 2025.2.0-EAP1 mention (topic 6177) instead of replying there, because that mention is an unanswered reply to a release announcement"
  - "The record's whole-line `# prettier-ignore` above `upstream:` is used again (precedent: UP-0006) so the one-line flow form passes checkUpstreamRecords"

requirements-completed: [UPS-01, UPS-03]

duration: 25min
completed: 2026-11-25
status: complete
---

# Phase 02 Plan 29: Publish UP-0013 on the AxonIQ forum and record it

**UP-0013 (`GET /v1/public/context` reports `dcbContext: false` for a DCB default context) is public on discuss.axoniq.io as topic 6853 with the author-approved text, and its record is `reported` with the topic URL.**

## History

The plan halted earlier on 2026-11-25 because the forum account was on hold after the UP-0006 post (the SUMMARY of
that run was `status: halted`). The same day plan 02-28 completed, the forum account was released, and this plan was
resumed from Task 2. This SUMMARY replaces the halted one.

## Accomplishments

- Task 1 (checkpoint, done by the author in the orchestrator's browser): the topic was posted.
  - Precondition met: UP-0006 is reported (plan 02-28) and the forum account was released on 2026-11-25.
  - Duplicate search, read-only (`search.json?q=dcbContext`): one earlier mention, a reply by another user to the
    2025.2.0-EAP1 release announcement, https://discuss.axoniq.io/t/axon-server-2025-2-0-eap1/6177/2 (2025-10-21,
    unanswered). The author approved a new topic that links it, with the sentence "The same symptom was mentioned on
    2025.2.0-EAP1 in a reply to the release announcement (link); it still occurs on 2026.1.8." under Actual behavior.
  - The private-data scan of the text was clean. Per the ADR-0013 forum rule the author entered the text in the browser
    and submitted it himself (he posted before the orchestrator's pre-submit look; the orchestrator verified the
    published post afterwards).
  - Published 2026-11-25T08:34:45Z with no moderation hold, category Axon Server, topic 6853:
    https://discuss.axoniq.io/t/axon-server-2026-1-8-get-v1-public-context-reports-dcbcontext-false-for-a-dcb-default-context/6853
- Task 2 (this run): verified the URL anonymously (`curl` returns 200) and compared `https://discuss.axoniq.io/raw/6853/1`
  with the approved text. They match word for word. The forum text differs only by editor normalizations: list markers
  `-` became `*`, `_italic_` became `*italic*`, and spaces were added inside the parentheses around the 6177 link; lines
  were joined because Discourse renders single newlines as line breaks. No word changes, so the draft in the record
  stays the published text.
- `docs/upstream/0013-axon-server-dcb-context-flag.md`: `status: "reported"`, `upstream` holds the topic URL (one line
  behind a whole-line `# prettier-ignore`), `reported: "2026-11-25"`, `updated: "2026-11-25"`, `workaround: false`
  unchanged. The "Publication status" paragraph now states the final state (posted by the author on 2026-11-25 as a new
  topic after the account was released, public at once, links the 6177 mention). The 6177 sentence was added to the
  draft under Actual behavior, and the duplicate-search result is noted in the "Target" paragraph.

## Verification

- `mise x -- ./gradlew checkUpstreamRecords spotlessCheck --console=plain`: BUILD SUCCESSFUL.
- `grep -c 'publication needs the approval of the author'` on the record prints 0 (was 1).
- `grep -c '^upstream: \["https://discuss.axoniq.io/t/'` on the record prints 1 (was 0).

## Task Commits

1. Task 1: no repository commit (publication done by the author in the browser).
2. Task 2: `540a0f4` docs(02-29): record UP-0013 as reported with the public forum topic

The SUMMARY commit follows as a separate `docs(02-29)` commit.

## Deviations from Plan

None - plan executed exactly as written. The plan expected the orchestrator or the author to enter the text; the
author posted before the orchestrator's pre-submit look, which the ADR-0013 rule allows (the author types and submits
himself) and which the orchestrator verified afterwards.

## Threat Model

- T-02-74 (posting without consent): the author wrote and submitted the post himself; the raw post matches the approved
  text.
- T-02-75 (credentials or private data): the author signed in himself in an isolated browser, nothing was written to the
  repository, and the private-data scan was clean.

## Known Stubs

None.

## Next Phase Readiness

G-02-5's forum items are complete: UP-0006 (plan 02-28) and UP-0013 (this plan) are both reported. When AxonIQ answers
either topic, update the record (status, `updated`) per its removal criteria. REQUIREMENTS.md is not marked complete
here; the orchestrator owns that and the STATE.md and ROADMAP.md writes.

## Self-Check: PASSED

- FOUND: docs/upstream/0013-axon-server-dcb-context-flag.md (status reported, upstream link, reported date)
- FOUND commit: 540a0f4
