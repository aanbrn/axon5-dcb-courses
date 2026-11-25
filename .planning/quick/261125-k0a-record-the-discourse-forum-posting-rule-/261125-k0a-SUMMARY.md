---
phase: quick-261125-k0a
plan: 01
subsystem: docs
tags: [upstream, adr, discourse, ups-03]
requires: []
provides:
  - Discourse forum posting rule in ADR-0013 under Publication (UPS-03)
  - One-line pointer to that rule in the upstream issue template
affects: [02-28, 02-29]
key-files:
  modified:
    - docs/adr/0013-upstream-records-and-workaround-markers.md
    - docs/templates/upstream-issue.md
decisions:
  - "A forum draft is never filled into the Discourse composer in one automated action; the author enters it in the browser in parts, with a pause, or typed character by character"
requirements-completed: [UPS-03]
status: complete
commits: 2
plan_head_before: 2e3c5cab5f9a37b1d68f2f449801373d1a9f54a5
plan_head_after: 5f388d618700d7de3fd2871774d1ba543bab77d8
actuals:
  tokens: 1000
  tasks: 2
  commits: 2
---

# Phase quick-261125-k0a Plan 01: Record the Discourse forum posting rule Summary

ADR-0013 now records that a trust level 0 Discourse post filled faster than `min_first_post_typing_time` is held for moderation, and that forum drafts are therefore never filled in one automated action.

## What was done

- Task 1 (`1cdbd6a`): added one paragraph to `### Publication (UPS-03)` in ADR-0013 with the rule, the mechanism, the 2026-11-25 observation (UP-0006 held as pending post 70, UP-0013 refused with "Access Denied"), the cause phrased as the author's assessment, and the procedure. Front matter, headings and existing sentences unchanged.
- Task 2 (`5f388d6`): added one line, the last of the leading comment in `docs/templates/upstream-issue.md`, pointing forum drafts to ADR-0013 under "Publication (UPS-03)". Skeleton unchanged.

## Verification

checkAdrs, checkUpstreamRecords, spotlessCheck, editorconfig-checker and reuse lint pass; all text gates from the plan pass. Nothing pushed.

## Deviations from Plan

None - plan executed exactly as written. The plan's one-line verify commands were run as equivalent script files in the scratchpad because the sandbox refuses long compound shell commands.

## Known Stubs

None.

## Self-Check: PASSED

Both commits exist (`1cdbd6a`, `5f388d6`); both modified files carry the new text.
