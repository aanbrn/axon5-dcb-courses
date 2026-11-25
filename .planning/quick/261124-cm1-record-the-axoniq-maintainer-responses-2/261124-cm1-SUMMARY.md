---
phase: quick-261124-cm1
plan: 01
subsystem: upstream-records
tags: [upstream, docs, axon, maintainer-response]
requires: [UPS-01, UPS-03]
provides:
  - UP-0001, UP-0002, UP-0003, UP-0004 and UP-0007 record the AxonIQ maintainer responses of 2026-11-23
key-files:
  modified:
    - docs/upstream/0001-decision-capture-extension-point.md
    - docs/upstream/0002-conflict-diagnosis-completion-exception.md
    - docs/upstream/0003-remote-conflict-classification.md
    - docs/upstream/0004-tag-resolver-value-classes.md
    - docs/upstream/0007-shared-testcontainer-latest-image.md
decisions:
  - "Response blocks sit in the lead of the Upstream issue draft section, between the Target paragraph and the verbatim issue text"
  - "Contributing pull requests for AxonFramework#5326 and #5328 stays an open author decision"
status: complete
completed: 2026-11-24
commits: 3
plan_head_before: 135f2682fe1649cbbf72acc492fce4de7be705b1
plan_head_after: 3226cd7707c1f23f54b9336cbc009a2316bbee42
actuals:
  tokens: 9000
  tasks: 3
  commits: 3
---

# Quick Task 261124-cm1: Record the AxonIQ maintainer responses Summary

Five upstream records now state the maintainer responses of 2026-11-23 with links to the exact comments, the issue state,
milestone and labels as read on 2026-11-24, and the target milestones in their Removal criteria.

## What changed

- **UP-0002 and UP-0007** (commit 356b891): response block, status line (Release 5.4.3 and Release 5.5.0, both
  "Priority 3: Could") and one Removal criteria sentence each naming the milestone.
- **UP-0001 and UP-0004** (commit 5627c01):
  - UP-0001: the unqualified "any Axon release may break the decorator" claim is replaced by the maintainer's reading,
    that `@Internal` means "may change", is not experimental, and `EventStorageEngine` stays in Axon Framework 5. Root
    cause notes that the decoration mechanism is public. Removal criteria explain why the Axon Framework 5 statement and
    a `debug` log do not meet the criterion and when an opt-in decorator definition does.
  - UP-0004: the #3761 distinction (`AnnotationBasedEntityIdResolver` versus `AnnotationBasedTagResolver`) and the 5.7.0
    milestone.
  - Both carry an `Open decision for the author:` paragraph.
- **UP-0003** (commit 3226cd7): new title ("... sees a DCB conflict from Axon Server only as message text"), Symptom
  qualified, Root cause with the deliberate-design rationale (class may be absent on the dispatcher; the rejection is
  non-transient because the condition is bound to the old consistency marker; only re-dispatching is retryable),
  Workaround with the message-text `RetryPolicy` alternative (marker bullets and the `Int.MIN_VALUE + 10` description
  unchanged), and rewritten Removal criteria that a message-text match does not satisfy.

Status stays `reported`, `updated` is `2026-11-24` in all five, no front matter key was added, the verbatim published issue
text (from the first `### ` heading to the end) is byte-identical to ae9067c in all five, and no code, configuration or
marker changed.

## Live versus planning-time facts

None. `gh issue view` on 2026-11-24 returned the same state (open), milestone, labels and comment URLs for all five
issues as the plan's table, and each issue still has exactly one comment. Only `gh issue view` was used; nothing was
posted to GitHub.

## Verification

- `mise x -- ./gradlew checkUpstreamRecords spotlessCheck` passed after each task; `editorconfig-checker docs/upstream`
  passed after each task.
- All grep and byte-identity gates of the three tasks passed, run as separate commands because the sandbox refuses
  compound shell commands that mix git with `&&`, process substitution or loops. The base text from ae9067c was
  compared through files in the scratchpad.
- The last gate (commit paths) was measured as `git diff --name-only 135f268 HEAD`: exactly the five records.

## Deviations from Plan

None - plan executed exactly as written. The only variation is mechanical: the Task 3 final gate that uses
`git log -F --grep` was replaced by the equivalent `git diff --name-only <base> HEAD`, because the sandbox refused the
original form. No Spotless apply was needed.

## Known Stubs

None.

## Threat Flags

None.

## Suggested STATE.md updates (for the orchestrator)

- Blockers/Concerns, new line: `[Upstream]` open author decision whether to contribute pull requests for
  AxonIQ/AxonFramework#5326 (UP-0001: a `debug` log statement in the `EventStorageEngine` or an opt-in decorator
  definition; the 5.5.0 milestone assumes a pull request from this repository) and #5328 (UP-0004: the Kotlin-aware tag
  resolver; otherwise upstream targets 5.7.0).
- Blockers/Concerns, the `[Phase 3]` WR-01 line: append the upstream input from UP-0003 (axoniq-framework#811): a
  dispatch-side `RetryPolicy` can match `AppendEventsTransactionRejectedException` in the `CommandExecutionException`
  message; the rejection is non-transient on purpose and only re-dispatching the command is retryable; weigh this
  against the handling-side `Int.MIN_VALUE + 10` decorator when the retry becomes configurable.
- Blockers/Concerns, the `[Phase 2/3]` `@Internal` SPI line: append that the maintainer (AxonFramework#5326) states
  `@Internal` means "may change", not experimental, and that `EventStorageEngine` stays in Axon Framework 5.

## Self-Check: PASSED

- All five records exist and carry the response blocks.
- Commits 356b891, 5627c01 and 3226cd7 are ancestors of HEAD.
