---
phase: 02-runtime-infrastructure-dcb-spikes
plan: 26
subsystem: infra
tags: [upstream-records, codeql, colima, dns, gap-closure]

requires:
  - phase: 02-runtime-infrastructure-dcb-spikes
    provides: upstream records UP-0010 and UP-0011 (plans 02-22, 02-23) and the G-02-5 gap from UAT
provides:
  - UP-0010 with a dated CodeQL bundle check showing that bundles 2.27.5 and 2.27.6 document a Kotlin range that includes 2.4.21
  - UP-0011 with a clean-profile measurement showing that the Go lookup stall does not reproduce on colima 0.10.3
  - the evidence plan 02-27 needs for the publication decision on UP-0011
affects: [02-27, upstream-records, codeql-workflow, doctor-colima-check]

actuals:
  tokens: 3000
  tasks: 2
  commits: 2

plan_head_before: e45269c8271bb77e1b7163856789c17a602c26a8
plan_head_after: a611ca2ed96a08143a809b96fc057b87a4bb4d93

tech-stack:
  added: []
  patterns:
    - "Upstream records carry dated, sourced checks and are corrected when a later check contradicts an earlier reading"

key-files:
  created: []
  modified:
    - docs/upstream/0010-codeql-kotlin-extractor.md
    - docs/upstream/0011-colima-aaaa-dns.md

key-decisions:
  - "UP-0010 stays open: the Kotlin range of the bundle no longer explains the empty extraction, Isolated Projects is the remaining suspect, and the throwaway workflow_dispatch run is a separately approved next step"
  - "UP-0011 stays open and its workaround stays: the stall is recorded as observed once in the spike and not reproduced on a clean profile, and the draft is marked not publishable until a reproducing condition is known"

patterns-established:
  - "Verification of a colima defect starts a throwaway profile with --template=false so a user template cannot mask the result"

requirements-completed: [UPS-01, UPS-03]

coverage:
  - id: D1
    description: "UP-0010 records, with date and sources, that the latest action v4.39.5 ships CodeQL bundle 2.27.6 and that bundles 2.27.5 and 2.27.6 document Kotlin 1.8.0 to 2.4.2x (which includes 2.4.21); status stays open and the workflow is untouched"
    requirement: "UPS-01"
    verification:
      - kind: other
        ref: "mise x -- ./gradlew checkUpstreamRecords spotlessCheck --console=plain"
        status: pass
    human_judgment: true
    rationale: "The factual claims come from external sources (CodeQL docs, tags, a pull request) that no repository test asserts; a reviewer should confirm the corrected reading"
  - id: D2
    description: "UP-0011 records the clean-profile reproduction (not reproduced in three variants, with the durations), the template-independent verification command, and that the stall stays unexplained; status stays open"
    requirement: "UPS-03"
    verification:
      - kind: other
        ref: "mise x -- ./gradlew checkUpstreamRecords spotlessCheck --console=plain"
        status: pass
      - kind: other
        ref: "grep -c -- '--template=false' docs/upstream/0011-colima-aaaa-dns.md (4)"
        status: pass
    human_judgment: true
    rationale: "The measurements were taken on the author's machine and are not repeatable by a test; plan 02-27 needs a human reading of the evidence"
  - id: D3
    description: "The throwaway colima profile dns-check is deleted and the author's default profile is unchanged (stopped, same settings)"
    verification:
      - kind: other
        ref: "colima list shows only default (Stopped, aarch64, 10 CPUs, 16GiB, 100GiB, docker+k3s)"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-11-25
status: complete
---

# Phase 02 Plan 26: UP-0010 bundle check and UP-0011 clean-profile reproduction Summary

**UP-0010 now records that CodeQL bundles 2.27.5 and 2.27.6 already document Kotlin 1.8.0 to 2.4.2x, and UP-0011 records that the colima Go-lookup stall did not reproduce on three throwaway clean profiles of colima 0.10.3; both stay open.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-11-25T04:19:37Z
- **Completed:** 2026-11-25T04:31:55Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- UP-0010: the latest `github/codeql-action` release is v4.39.5 (2026-11-24), whose default bundle is 2.27.6 (the latest bundle release, 2026-11-24). The repository pins v4.39.4 (bundle 2.27.5). The Kotlin row of the CodeQL supported-languages page reads "Kotlin 1.8.0 to 2.4.2x", and `docs/codeql/reusables/supported-versions-compilers.rst` reads `Kotlin 1.8.0 to 2.4.2\ *x*` at both tags `codeql-cli/v2.27.5` and `codeql-cli/v2.27.6`. The range was widened from "1.8.0 to 2.4.20" by `github/codeql#23092` (merged 2026-11-13; commit 3afb1b5734 is an ancestor of both tags), whose description says that 2.4.21 was already supported and that the full K2 suite of 205 tests passed. The CLI changelog for 2.27.5 and 2.27.6 lists no Kotlin extractor change.
- UP-0010 consequence: the record's earlier reading ("documents 1.8.0 to 2.4.20" for 2.27.5) does not match the 2.27.5 tag. The first removal criterion is met by the documentation; the unproven cause is the Isolated Projects interaction; the second criterion (a throwaway `workflow_dispatch` run on Linux x86-64 with bundle 2.27.6) is the next step and is an outward action that needs its own approval. Not done here.
- UP-0011: not reproduced in any of three variants of a throwaway profile created with `--template=false` and without `--dns` (colima 0.10.3, macOS 27.2.1, Apple Silicon, `vz`, Docker runtime, 1 CPU, 2 GiB, 10 GiB). Go lookup durations, run 1 (cold) / run 2 / run 3: clean defaults (`network.address: false`, `dnsHosts: {}`, `network.dns: null`) 103.46 ms / 1.91 ms / 2.51 ms; `--network-address` (`network.address: true`) 132.25 ms / 2.51 ms / 2.37 ms; `--dns-host host.docker.internal=host.lima.internal` 103.31 ms / 2.68 ms / 2.32 ms. No error in any run; the daemon's own image pulls (Go) resolved without delay. Outcome per variant: not reproduced, not reproduced, not reproduced.
- UP-0011 busybox control: 0.144 s (`network.address: true`) and 0.171 s (`dnsHosts` variant); the clean-defaults control took 7.28 s of wall time because it included the first pull of the `busybox` image, so it is not a lookup time and the record says so.
- UP-0011 record: Minimal reproduction carries the dated result and a table; Root cause says the stall is not a property of the 0.10.3 defaults by themselves and names untested candidates (host upstream resolvers at the time, rest of the user template such as Kubernetes, Rosetta and VirtioFS, VM history); Removal criteria say the verification already passes on a clean profile and that the explicit `network.dns` workaround and the doctor check stay until the author decides; the verification command starts the throwaway profile with `--template=false` (and uses the valid flag `--cpus`); the issue draft is marked not publishable until a reproducing condition is known.
- Cleanup: `colima delete --force dns-check` ran from a trap in both scripts; `colima list` afterwards shows only `default`, Stopped, aarch64, 10 CPUs, 16GiB, 100GiB, docker+k3s, as before. `colima` was only ever called with the profile `dns-check`.

URLs used for UP-0010: https://codeql.github.com/docs/codeql-overview/supported-languages-and-frameworks/ , https://github.com/github/codeql-action/releases/tag/v4.39.5 , https://github.com/github/codeql-action/releases/tag/codeql-bundle-v2.27.6 , https://github.com/github/codeql-cli-binaries/blob/HEAD/CHANGELOG.md , https://github.com/github/codeql/pull/23092 , https://github.com/github/codeql/pull/22404 , the rst file at `codeql-cli/v2.27.5` and `codeql-cli/v2.27.6` of `github/codeql`.

## Task Commits

1. **Task 1: UP-0010 bundle check, status stays open** - `4723d31` (docs)
2. **Task 2: UP-0011 clean-profile reproduction** - `a611ca2` (docs)

**Plan metadata:** committed with this SUMMARY (docs: complete plan)

## Files Created/Modified

- `docs/upstream/0010-codeql-kotlin-extractor.md` - dated bundle check, corrected Kotlin-range reading, author's no-publication decision, narrowed Analysis of the draft, new title
- `docs/upstream/0011-colima-aaaa-dns.md` - clean-profile result, narrowed root cause, removal criteria, template-independent verification, draft marked not publishable

## Decisions Made

- UP-0010 title and draft Analysis were changed along with the Root cause (see deviation 1): leaving "does not cover Kotlin 2.4.21" in the record while it documents the opposite would be a false statement.
- Three variants were measured for UP-0011 instead of the planned two (see deviation 2); the plan's step 4 only required the second variant if the first did not reproduce, and the third narrows the cause cheaply.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The record's Symptom bullet, title and draft Analysis contradicted the bundle check**
- **Found during:** Task 1
- **Issue:** The record said bundle 2.27.5 documents Kotlin only up to 2.4.20 and the title said the extractor "does not cover Kotlin 2.4.21"; the tag `codeql-cli/v2.27.5` documents 2.4.2x. The plan asked for a dated paragraph and one sentence in the draft section only.
- **Fix:** Corrected the Symptom bullet, the title and the draft's Analysis paragraph to match the check, kept status, workaround, `upstream`, `reported` and `product-version` unchanged.
- **Files modified:** docs/upstream/0010-codeql-kotlin-extractor.md
- **Verification:** `checkUpstreamRecords` and `spotlessCheck` pass; workflow file diff is empty.
- **Committed in:** 4723d31

**2. [Rule 2 - Missing critical] Added a `dnsHosts` variant and used 2 GiB of memory, `--cpus` flag name**
- **Found during:** Task 2
- **Issue:** The user template differs from clean defaults also by a `dnsHosts` entry, which can change which resolver colima configures; a record that blames or clears only `network.address` would be incomplete. Memory of 1 GiB risked an out-of-memory failure of `go run` unrelated to DNS, and the record's flag `--cpu` is not a colima flag (`--cpus` is).
- **Fix:** Ran a third variant (`--dns-host host.docker.internal=host.lima.internal`), used `--memory 2`, and corrected `--cpu` to `--cpus` in the record's commands.
- **Files modified:** docs/upstream/0011-colima-aaaa-dns.md
- **Verification:** third variant measured 103.31 ms / 2.68 ms / 2.32 ms, profile deleted, `colima list` shows only `default`.
- **Committed in:** a611ca2

**3. [Rule 1 - Bug] Workflow marker comment left stale (not fixed, by prohibition)**
- `.github/workflows/codeql.yml` line 32 still says "the CodeQL Kotlin extractor does not cover Kotlin 2.4.21 and found no sources here." The plan forbids touching that file, so it is unchanged; the comment now overstates the cause. A follow-up edit of the marker text (the marker `UPSTREAM: UP-0010` stays valid) belongs to a later plan.

---

**Total deviations:** 2 auto-fixed (1 Rule 1, 1 Rule 2) plus 1 documented non-fix.
**Impact on plan:** No scope creep beyond the two records; both corrections make the records match the evidence.

## Issues Encountered

- The sandbox allowed `colima` and its image and container downloads, so no permission checkpoint was needed. A stray scratch file written by mistake outside the scratchpad was removed before any commit.
- Colima switches the Docker CLI's current context when a profile starts and resets it when the profile is deleted; afterwards `docker context ls` shows `default` as current and `~/.docker/config.json` has no `currentContext` key. The author's earlier current context could not be read before the run; with the `default` profile stopped it was most likely `default` already.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or schema; the pre-commit scan found no home paths, no host or user names, and no `100.64.` addresses in either record (the only address is colima's documented default forwarder `192.168.5.1`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-27 can decide on UP-0011 publication with the evidence above: the stall did not reproduce, so an issue is not justified yet, and the draft says so.
- A follow-up is needed for the CodeQL workflow marker text and, if the author wants the Isolated Projects question answered, the throwaway `workflow_dispatch` run with bundle 2.27.6 (needs separate approval).

## Self-Check: PASSED

- `docs/upstream/0010-codeql-kotlin-extractor.md` and `docs/upstream/0011-colima-aaaa-dns.md` exist and carry `status: "open"` and `updated: "2026-11-25"`.
- Commits `4723d31` and `a611ca2` exist on the branch; measured `commits: 2` from the ledger base `e45269c`.
- `checkUpstreamRecords` and `spotlessCheck` green; `git diff HEAD -- .github/workflows/codeql.yml` empty; `grep -cE '100\.64\.|/Users/|/home/'` prints 0 for UP-0011; `colima list` shows only `default`.

---
*Phase: 02-runtime-infrastructure-dcb-spikes*
*Completed: 2026-11-25*
