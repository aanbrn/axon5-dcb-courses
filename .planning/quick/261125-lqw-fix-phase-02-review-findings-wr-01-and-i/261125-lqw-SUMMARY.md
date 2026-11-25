---
phase: quick-261125-lqw
plan: 01
subsystem: ci
tags: [ci, shell, retry, locale, annotations]
requires: []
provides:
  - "retry-on-rate-limit.sh leaves the caller's locale untouched for the wrapped command"
  - "host-neutral 429 retry announcement and annotation title"
affects: [scripts/ci/retry-on-rate-limit.sh, scripts/ci/test-retry-on-rate-limit.sh]
key-files:
  modified:
    - scripts/ci/retry-on-rate-limit.sh
    - scripts/ci/test-retry-on-rate-limit.sh
decisions:
  - "LC_ALL=C is a prefix of the 429 grep alone, never exported"
  - "Announcement: 'a server answered HTTP 429 (Too Many Requests)'; annotation title: 'Dependency download rate limit'"
metrics:
  duration: "about 15 min"
  completed: 2026-11-25
status: complete
commits: 2
plan_head_before: 68d72b1681d130e1773c1285426d279f837cee92
plan_head_after: 39822f542a8a24d788a65a2f2cea89321bafbcb9
actuals:
  tokens: 6000
  tasks: 2
  commits: 2
---

# Quick 261125-lqw: Fix Phase 02 review findings WR-01 and IN-02 Summary

`retry-on-rate-limit.sh` no longer forces the C locale on the wrapped command (the C locale is scoped to the 429 grep), and its retry line and `::warning` title name no host.

Both files are governed paths (`scripts/ci/**`, ADR-0015) and were changed with the Edit tool only.

## Commits

| Finding | Commit | Message |
| ------- | ------ | ------- |
| WR-01 | `ec2b3f4` | fix(quick-261125-lqw): keep the caller's locale for commands run by retry-on-rate-limit.sh (WR-01) |
| IN-02 | `39822f5` | fix(quick-261125-lqw): name no host in the rate-limit retry announcement (IN-02) |

No gate-fix commits were needed.

## What changed

- **WR-01.** Removed `export LC_ALL=C`; the match is now `LC_ALL=C grep -aqF -- "$RATE_LIMIT_MESSAGE" "$log"`, so it stays byte-wise while both runs of the command see the caller's LC_ALL. Header "What it never does" now says the wrapper never changes the command's environment, its locale included. New case 13 (caller LC_ALL `<unset>` and `POSIX`) uses a fake command that records the LC_ALL of each run in `run.N.lc_all`.
- **IN-02.** Retry line: `retry-on-rate-limit: a server answered HTTP 429 (Too Many Requests); running the command once more in ${delay} s`. Annotation title: `Dependency download rate limit`. The header "Why." paragraph now notes that Gradle prints the same message whichever host answered (Maven Central, the CI mirror, the Plugin Portal). Test constants `RETRY_MARKER` and `WARNING_PREFIX` follow, and case 3 gains "the announcement names no host".
- Docs: `.github/workflows/ci.yml` and `docs/adr/0016-ci-dependency-downloads.md` were already host-neutral, so no file outside the two scripts changed.

## RED run (WR-01)

On the unchanged wrapper, with the new case 13 only:

```
FAIL - caller LC_ALL <unset>: the first run sees the caller's LC_ALL (exit 0, 2 run(s))
FAIL - caller LC_ALL <unset>: the second run sees the caller's LC_ALL (exit 0, 2 run(s))
FAIL - caller LC_ALL POSIX: the first run sees the caller's LC_ALL (exit 0, 2 run(s))
FAIL - caller LC_ALL POSIX: the second run sees the caller's LC_ALL (exit 0, 2 run(s))

52 passed, 4 failed
```

GREEN: `56 passed, 0 failed`. Live probes: `LC_ALL=POSIX ... bash -c 'echo "LC_ALL=${LC_ALL-<unset>}"'` printed `LC_ALL=POSIX`; under `env -u LC_ALL` it printed `LC_ALL=<unset>`.

## RED run (IN-02)

On the wrapper of Task 1, with the new constants and the no-host assertion:

```
FAIL - 429 then success: prints one warning annotation (exit 0, 2 run(s))
FAIL - 429 then success: the annotation names the delay (exit 0, 2 run(s))
FAIL - 429 then success: the announcement names no host (exit 0, 2 run(s))
FAIL - 429 twice: announces the retry once (exit 4, 2 run(s))
FAIL - outside Actions: prints a plain retry line (exit 0, 2 run(s))
FAIL - fixed text: only the wrapper's own warning carries its title (exit 0, 2 run(s))

51 passed, 6 failed
```

GREEN: `57 passed, 0 failed`.

## Gate results (final HEAD 39822f5)

| Gate | Result |
| ---- | ------ |
| `scripts/ci/test-retry-on-rate-limit.sh` | 57 passed, 0 failed |
| every tracked shell suite (8 suites) | all end with `0 failed` |
| shellcheck on all tracked `*.sh` | exit 0 |
| `spotlessShellCheck`, `spotlessCheck` (shfmt, SPDX) | BUILD SUCCESSFUL |
| actionlint | exit 0 |
| editorconfig-checker | exit 0 |
| `reuse lint` | compliant (525 / 525) |
| line-ending normalisation (ci.yml step, run from a scratch script) | exit 0 |
| `git grep 'Maven Central answered' / 'title=Maven Central'` outside `.planning` | 0 matches (HEAD before: 4) |
| scope: `git diff --stat afb081f HEAD` | only the two scripts plus the PLAN under `.planning` |

Skipped, because they read only files this plan does not change: zizmor, the Renovate config validator, `docker compose config`. `spotlessCheck` was run before the SUMMARY was written; the SUMMARY is left uncommitted for the orchestrator's docs commit.

## Deviations from Plan

None - plan executed exactly as written. One note: the message line in the wrapper is 122 characters (the plan states this); it is code, not a header comment, and shfmt does not wrap it.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

- `scripts/ci/retry-on-rate-limit.sh` and `scripts/ci/test-retry-on-rate-limit.sh` modified: FOUND
- Commits `ec2b3f4` and `39822f5` reachable from HEAD: FOUND
