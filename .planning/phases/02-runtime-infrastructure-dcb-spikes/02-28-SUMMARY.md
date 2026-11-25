---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 28
subsystem: infra
tags: [upstream-records, axon-server, forum, gap-closure, ups-03, deferred]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: the approved UP-0006 draft, submitted to discuss.axoniq.io during UAT
provides:
  - a dated publication status in UP-0006 (submitted, held for moderation, account on hold)
  - the decision to move the record's `reported` step out of Phase 2 as a follow-up
affects: [upstream-records]

actuals:
  tokens: 400
  tasks: 0
  commits: 0

tech-stack:
  added: []
  patterns: []

key-files:
  created: []
  modified:
    - docs/upstream/0006-empty-platform-token.md

key-decisions:
  - "Author decision (2026-11-25): forum moderation is an external event of unknown duration, so it does not hold Phase 2; plans 02-28 and 02-29 are halted and tracked as a follow-up in STATE.md"

patterns-established: []

requirements-completed: []

duration: 0min
completed: 2026-11-25
status: halted
---

# Phase 02 Plan 28: UP-0006 publication halted on forum moderation

**UP-0006 stays `open`: the approved post is held by discuss.axoniq.io moderation (pending post 70) and the account is on hold; recording it as `reported` moved to a follow-up outside Phase 2.**

## What happened

- During UAT (2026-11-25) the author approved the UP-0006 text and it was submitted in category Axon Server.
- Discourse held it as pending post 70 and later sent the automated message "Account temporarily on hold"
  (https://discuss.axoniq.io/t/account-temporarily-on-hold/6850): the account can browse but cannot reply or create
  topics until staff review its posts. The author's assessment is that the text was entered too fast for a person,
  which matches Discourse's `min_first_post_typing_time` check for trust level 0 accounts.
- The read-only search of this plan's Task 1 (executor, 2026-11-25) found no public topic: newest topic 6849,
  `/t/6850` and `/t/6851` not public to anonymous readers.
- The author wrote to AxonIQ developer relations (devrel@axoniq.io) the same day, disclosing that the post was
  prepared and submitted by an AI coding agent under the author's account after the author's review and approval.

## Halt

Author decision (2026-11-25): two forum posts must not block the phase. Task 1 (checkpoint: public topic URL) cannot
complete before staff review, so the plan is halted by design, not failed.

Changed in the repository instead:

- `docs/upstream/0006-empty-platform-token.md`: dated publication status in the draft section; `status` stays
  `open`, `updated` 2026-11-25.
- ADR-0013 "Publication (UPS-03)" now carries the forum posting rule (quick task 261125-k0a).

## Follow-up (STATE.md Pending Todos)

When the post is public: verify the topic URL anonymously, set `status: "reported"`, `upstream: [<URL>]`,
`reported` and `updated`, compare the published text with the draft, run `checkUpstreamRecords` (the remaining steps of
Task 2 of this plan). If the moderators reject the post, record that and decide on AxonIQ support as the channel.

## Deviations

None in code. The plan did not run its Task 2; this SUMMARY records the halt.
