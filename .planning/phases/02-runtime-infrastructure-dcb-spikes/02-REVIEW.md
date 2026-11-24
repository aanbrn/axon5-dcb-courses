---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-24T00:00:00Z
depth: standard
files_reviewed: 5
files_reviewed_list:
  - backend/serialization-jackson/src/test/kotlin/io/github/aanbrn/courses/serialization/JsonRoundTripTest.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - scripts/doctor.sh
  - scripts/test-doctor.sh
findings:
  critical: 0
  warning: 1
  info: 2
  total: 3
  necessity: 0
status: issues_found
---

# Phase 2: Code Review Report (incremental, quick task 261124-s7w)

**Reviewed:** 2026-11-24
**Depth:** standard
**Files Reviewed:** 5
**Status:** issues_found

## Summary

This is an incremental review of the diff since 97724dc. The diff fixes the previous WR-01, IN-01 and IN-02.

- **WR-01 (doctor dies under `set -e`):** fixed. Every guarded read I traced is correct. That covers `mise.toml`, the tool list, the colima file, the GSD pin, the manifest shape check, the `.mcp.json` shape check and `.env`. I checked pipefail status propagation, the `! a || ! b` precedence, and the `local` declarations that sit apart from the assignments. The manifest shape check does make the later `.files` reads (`to_entries`, `keys`, `length`) safe. `scripts/test-doctor.sh` runs green here (246 passed, 0 failed).
- **IN-01 (TypeID map-key test):** fixed. The assertion now pins the failing UUID key deserializer and the offending value.
- **IN-02 (Renovate reads outside the verified entries):** `MANAGER_READ` and `readOutsideEntries` are sound. I traced these cases: first match at index 0 on a verified `[versions]` line, quoted keys, a header line, an indented or tabbed entry, a trailing-comment match after an accepted entry, and `old-image-x`. None gives a wrong result. I did not run the Gradle functional tests.

One real defect remains in `doctor.sh`. It was already there, but the jq program that contains it is one the diff touched. There are also two minor items.

## Warnings

### WR-01: An `.mcp.json` server without `command` is reported as an unresolved binary named `path`

**File:** `scripts/doctor.sh:511-528`
**Issue:** The jq program emits `[.key, .value.command, "path"] | @tsv`. For a server with no `command`, such as an HTTP or SSE server that has only `url`, the command is `null`, and `@tsv` renders it as an empty field. The consuming loop is `IFS=$'\t' read -r name binary wrapped`. Tab is an IFS whitespace character, so the two adjacent tabs collapse into one separator. `read` then shifts the fields: `name=remote binary=path wrapped=''`. I reproduced this with `{"mcpServers":{"remote":{"type":"http","url":"https://x"}}}`. The result is a false `warn mcp: servers ... whose binary cannot be resolved: remote (path)`, with the fix `mise install`, which cannot help. The same collapse happens for `mise x --` with no third argument (`$args[2]` is null). The diff added a shape check that treats this configuration as valid, but the value then goes down the buggy path. The committed `.mcp.json` has only stdio servers, so it does not trigger today. The doctor is meant as a reference tool and the shape check was just widened, so the gap matters.
**Fix:** Make the field non-empty and non-whitespace in jq, and skip servers that have no command (or report them as remote):
```jq
.mcpServers // {} | to_entries[]
| select(.value.command != null)   # url-only servers have nothing to resolve on PATH
| (.value.args // []) as $args
| if .value.command == "mise" and ($args[0] == "x" or $args[0] == "exec") and $args[1] == "--" and ($args[2] | type) == "string"
  then [.key, $args[2], "mise"] else [.key, .value.command, "path"] end | @tsv
```
Alternatively, use a non-whitespace delimiter such as `\u001f` with `IFS=$'\037'`. Add a test case with a `url`-only server that expects `ok mcp` and no warning.

## Info

### IN-01: The "mise.toml cannot be read" fix suggests `git restore` for what may be a permissions problem

**File:** `scripts/doctor.sh:106-110`
**Issue:** The new branch fires when `sed` fails. That happens if the file is missing or if it exists but is unreadable. The fix text is always `git restore mise.toml`. For a mode-000 file that does nothing useful, because git restore would only help if the file was modified or deleted. The sibling branches (`gsd-version`, `.env`) give `chmod u+r` for the unreadable case. `test-doctor.sh` covers only the missing case.
**Fix:** Split the case, as `check_gsd` does for the pin. Give `git restore mise.toml` for a missing file and `chmod u+r mise.toml` when `[[ -e $ROOT/mise.toml ]]`. Add a `make_unreadable` case for it.

### IN-02: Catalog findings sort lexicographically, so line 10 comes before line 2

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt:72` (`problems.sorted()`; the new line-numbered messages are built at 121)
**Issue:** The new `path:line:` catalog findings are sorted as strings, so `libs.versions.toml:10:` comes before `libs.versions.toml:2:`. The output is deterministic, which is the documented goal, but it is not in file order. The compose findings already behaved this way. The new catalog findings are the first where a reader would go through a single file top to bottom.
**Fix:** Sort on `(path, line number)` instead of the rendered string, for example by holding findings as a small data class and rendering after `sortedWith(compareBy({ it.path }, { it.line }))`. Alternatively, leave it and state in the KDoc that the order is lexical.

---

_Reviewed: 2026-11-24_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
