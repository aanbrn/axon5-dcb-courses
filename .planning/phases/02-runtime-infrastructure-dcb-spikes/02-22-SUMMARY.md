---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 22
subsystem: governance
tags: [upstream-records, github-issues, axon-framework, axoniq-framework, approval-gate]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: "Upstream records and issue drafts (02-16, 02-17) and checkUpstreamRecords (02-15)"
provides:
  - "Five public upstream issues, filed after the author's explicit approval (UPS-03)"
  - "Records UP-0001, UP-0002, UP-0003, UP-0004 and UP-0007 in status reported with issue links and dates"
affects: [upstream tracking, workaround removal criteria, phase 5 documentation site]

actuals:
  tokens: 3500
  tasks: 2
  commits: 1
plan_head_before: 984d9949533bc505ad7312463ecefe6cb981d04c
plan_head_after: 30ce4bea467473e3d0196c0647291485a9175201

tech-stack:
  added: []
  patterns:
    - "Publish exactly the approved draft text with gh issue create --body-file; the record keeps the draft and links the issue"

key-files:
  created: []
  modified:
    - docs/upstream/0001-decision-capture-extension-point.md
    - docs/upstream/0002-conflict-diagnosis-completion-exception.md
    - docs/upstream/0003-remote-conflict-classification.md
    - docs/upstream/0004-tag-resolver-value-classes.md
    - docs/upstream/0007-shared-testcontainer-latest-image.md

key-decisions:
  - "Published only UP-0001, UP-0002, UP-0003, UP-0004 and UP-0007 (the author's 'all five' approval); UP-0010 and UP-0011 held back because their reproductions were not re-verified on a clean setup"
  - "UP-0005 (already AxonFramework#3761) and the forum drafts UP-0006 and UP-0013 stay open or unchanged; the author posts forum drafts later"

requirements-completed: [UPS-03]

coverage:
  - id: D1
    description: "Approved upstream issue drafts are published as GitHub issues and the five records point to them"
    requirement: "UPS-03"
    verification:
      - kind: other
        ref: "mise x -- ./gradlew checkUpstreamRecords spotlessCheck --console=plain"
        status: pass
    human_judgment: false

duration: 15min
completed: 2026-11-23
status: complete
---

# Phase 2 Plan 22: Upstream Issue Approval Gate and Publication Summary

**Five approved upstream drafts are now public GitHub issues on AxonIQ/AxonFramework and AxonIQ/axoniq-framework, and their records are `reported` with the issue links.**

## Performance

- **Duration:** about 15 min (continuation after the Task 1 approval gate)
- **Tasks:** 2 of 2 (Task 1 approval-only, no commit; Task 2 published and recorded)
- **Files modified:** 5

## Published issues

| Record  | Repository                | Type            | Issue                                                |
| ------- | ------------------------- | --------------- | ---------------------------------------------------- |
| UP-0001 | AxonIQ/AxonFramework      | feature request | https://github.com/AxonIQ/AxonFramework/issues/5326  |
| UP-0002 | AxonIQ/AxonFramework      | bug             | https://github.com/AxonIQ/AxonFramework/issues/5327  |
| UP-0003 | AxonIQ/axoniq-framework   | enhancement     | https://github.com/AxonIQ/axoniq-framework/issues/811 |
| UP-0004 | AxonIQ/AxonFramework      | bug             | https://github.com/AxonIQ/AxonFramework/issues/5328  |
| UP-0007 | AxonIQ/axoniq-framework   | enhancement     | https://github.com/AxonIQ/axoniq-framework/issues/812 |

Each issue was created with `gh issue create --repo <target> --title <draft heading> --body-file <scratch file>` under the
author's GitHub account. The body is the record's "Upstream issue draft" section with the "Target:/Not published" intro
paragraph removed, as the author accepted at the gate. Before each creation a read-only search for an exact-title duplicate
returned no result. Nothing else was published: no comments, reactions, pull requests or issues on other repositories.

## Not published (records left byte-identical)

UP-0005 (already AxonIQ/AxonFramework#3761, no comment), UP-0006 and UP-0013 (forum drafts, the author posts them later; both
stay `open`), UP-0008, UP-0009, UP-0010, UP-0011 (reproduction not re-verified on a clean setup) and UP-0012.

## Task Commits

1. **Task 1: UPS-03 approval gate** - no commit (decision recorded by the author: "all five")
2. **Task 2: Publish approved drafts and update records** - `30ce4be` (docs)

The SUMMARY itself is committed after these (docs commit following `30ce4be`).

## Verification

- `mise x -- ./gradlew checkUpstreamRecords spotlessCheck --console=plain`: BUILD SUCCESSFUL
- Reported records without an `upstream` link: 0
- `ls docs/upstream/*.md | wc -l`: 13
- Files changed under `docs/upstream/` since the plan base (`984d994`): only the five approved records

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Stale "Not published" sentence in the five published records**
- **Found during:** Task 2
- **Issue:** The draft intro said "Not published; publication needs the approval of the author (plan 02-22)", which became false once the issue was filed.
- **Fix:** Replaced that single sentence with "Published as <issue URL> on 2026-11-23." The draft title and body, which are the published text, are unchanged.
- **Files modified:** the five published records
- **Commit:** `30ce4be`

## Authentication gates

None. `gh auth status` showed an active account with `repo` scope; every `gh issue create` succeeded without templates or permission problems.

## Known Stubs

None.

## Threat Flags

None. The published text was screened for private paths and e-mail addresses (no match) before each publication (T-02-49, T-02-50).

## Self-Check: PASSED

- Records modified: FOUND (five files under `docs/upstream/`)
- Commit `30ce4be`: FOUND
- Issue URLs in the table: returned by `gh issue create`
