---
phase: 02-runtime-infrastructure-dcb-spikes
reviewed: 2026-11-24T00:00:00Z
depth: standard
files_reviewed: 13
files_reviewed_list:
  - backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt
  - build-logic/src/functionalTest/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheckFunctionalTest.kt
  - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt
  - docs/adr/0015-governance-hooks-for-worktrees-and-merges.md
  - docs/tooling.md
  - docs/upstream/0001-decision-capture-extension-point.md
  - docs/upstream/0004-tag-resolver-value-classes.md
  - scripts/doctor.sh
  - scripts/governance/ask-governed-edit.sh
  - scripts/governance/ask-governed-merge.sh
  - scripts/governance/test-ask-governed-edit.sh
  - scripts/governance/test-ask-governed-merge.sh
  - scripts/test-doctor.sh
findings:
  critical: 0
  warning: 3
  info: 1
  total: 4
  necessity: 0
status: issues_found
---

# Phase 2: Code Review Report (incremental re-review)

**Reviewed:** 2026-11-24
**Depth:** standard
**Files Reviewed:** 13
**Status:** issues_found

## Summary

This re-review covers the diff `d49aa53..HEAD` for the files changed by quick tasks 261124-kh6 and 261124-nlk, read in the context of the whole files. I also ran the merge hook against about 50 commands, replayed the new `check_git` against a failing `git`, and ran a throwaway probe of the Jackson module in a scratch copy of the repository (`git archive HEAD`). The repository itself was not modified.

The earlier findings are fixed correctly:
- The merge hook now reads shell option clusters, wrapper options, `GIT_*` repository variables, brace expansion and moved refs as the ADR describes, and I found no bypass among the cases I tried.
- `below_agent_root` in the edit hook exempts only byte-for-byte agent-root paths, so the `..`, symlink and case-spelling cases still ask.
- `ImageReferenceCheck` now trims lines, so indented entries are matched.
- The gsd pin is validated before it reaches a command.

No new Critical issues. Three Warnings remain, and one of them (WR-01) is a regression introduced by the new `check_git`. WR-02 is a factual error in the new KDoc, which I confirmed by running the code. WR-03 is a gap between the image check and the Renovate manager, which the amended doc comment overstates.

## Warnings

### WR-01: `check_git` aborts the whole doctor run, with no report, when `git --version` exits non-zero

**File:** `scripts/doctor.sh:586-594`
**Issue:** `doctor.sh` runs under `set -euo pipefail`. The new check reads the version with:

```bash
installed=$(git --version 2>/dev/null | awk 'NR == 1 && $1 == "git" && $2 == "version" { print $3 }')
```

If `git --version` exits non-zero, `pipefail` makes the pipeline fail, and `set -e` terminates the script at that assignment. The `[[ ! $installed =~ ... ]]` branch that is meant to report "cannot read the version" is never reached.

The common real case is a fresh macOS machine without the Xcode Command Line Tools. `/usr/bin/git` is a shim there: `has git` succeeds, and the shim exits 1 with `xcode-select: note: no developer tools were found`. I replayed `check_git` with a fake `git` that prints that note and exits 1. The script exited with status 1 and printed nothing. There was no `fail  git:` line, no `fix:` line and no summary, so the machine-health tool is silent exactly where it is meant to help. `test-doctor.sh` only covers a fake `git` that exits 0 with an unparsable line (the "git version unknown" case), so it did not catch this.

**Fix:**

```bash
installed=$(git --version 2>/dev/null | awk 'NR == 1 && $1 == "git" && $2 == "version" { print $3 }') || installed=''
```

Add a test in `scripts/test-doctor.sh` where the fake `git --version` prints to stderr and exits 1. It should assert `^fail  git: cannot read the version`, a non-empty summary and exit code 1.

### WR-02: The new KDoc on `CoursesJsonModule` states a wrong map-key behaviour and a remedy that would not round-trip

**File:** `backend/serialization-jackson/src/main/kotlin/io/github/aanbrn/courses/serialization/CoursesJsonModule.kt:27-30`
**Issue:** The added paragraph says a map keyed by an ID "would be written with the ID's `toString()` as the key". It is not. `CourseId` is a `@JvmInline value class` over `UUID`, and jackson-module-kotlin serialises value-class keys by unboxing them.

I ran `coursesJsonMapper().writeValueAsString(mapOf(courseId to 1))` in a scratch copy of the repository:
- It wrote `{"01a20d2a-468f-7579-adc8-b838667c40d5":1}`, the raw UUID, while `courseId.toString()` is `course_01m86jmhmfenwtvj5r71k7rg6n`.
- Reading that JSON back into `Map<CourseId, Int>` succeeds, because the Kotlin module deserialises the key as a UUID.
- Reading the TypeID form `{"course_01m8...":1}` fails with `Cannot deserialize Map key of type java.util.UUID ... Invalid UUID string`.

So ID-keyed maps already round-trip today, but their key wire form differs from the TypeID wire form used for every value. The KDoc claims "no key deserializer to read it back", which is false. The plan it proposes (add only `addKeyDeserializer` that parses through `TypedIdCompanion.parse`) would make the module read TypeID keys while the Kotlin module keeps writing UUID keys. That breaks the round trip. Whoever follows this note later will be misled.

**Fix:** Correct the paragraph to the observed behaviour. For example: "These types are registered as values only. As map keys, the Kotlin module writes a value-class ID as its unboxed UUID, not as the TypeID text, and reads it back as a UUID. The first contract with an ID-keyed map must decide the wire form: to use the TypeID form, add both `addKeySerializer` (`ToStringSerializer`) and `addKeyDeserializer`, with the latter parsing through `TypedIdCompanion.parse` and reporting rejection through `weirdKeyException`." Optionally pin the current behaviour with a test in `JsonRoundTripTest` so the decision is visible.

### WR-03: `ImageReferenceCheck` accepts catalog entries that the Renovate manager never matches, though its KDoc says the check exists to catch them

**File:** `build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ImageReferenceCheck.kt:27-30, 148, 158`
**Issue:** The class KDoc says the double-quoted-value rule exists "because the Renovate custom manager reads only the double-quoted form and such an entry would never be updated". The check and the manager disagree on more than the value quoting. The manager regex (`renovate.json5:83`) is:

`\bimage-[a-z0-9-]+ = "(?<depName>[^:"@\s]+):(?<currentValue>[^@"\s]+)@(?<currentDigest>sha256:[a-f0-9]{64})"`

The check passes, and Renovate silently never updates, each of these entries:
- A quoted key: `"image-x" = "name:1@sha256:..."`. `CATALOG_ENTRY` accepts it, but the regex needs `image-x = "` with no quote between key and ` =`.
- A key with an uppercase letter, `_` or `.`, such as `image-Foo` or `image-a_b`. `CATALOG_ENTRY` allows `[A-Za-z0-9_.-]+`; the manager allows only `[a-z0-9-]+`.
- No spaces around `=`, or more than one space (`image-x="..."`, `image-x  = "..."`). `CATALOG_ENTRY` uses `\s*=\s*`; the manager needs exactly one space on each side.
- A registry with a port, `localhost:5000/app:1.0@sha256:...`. `PINNED_REFERENCE` explicitly allows the port prefix, but the manager's `depName` group excludes `:`.

The indentation fix in this round widened the matched shapes further without narrowing this gap. This is a robustness defect in a guard whose purpose is "every image is updated by Renovate". It pre-exists, but the amended KDoc now presents the parity as complete.

**Fix:** Either tighten the check to the manager's shape (bare lower-case key `image-[a-z0-9-]+`, single-space `=`, no registry port) and report anything else, or loosen the manager regex to the shapes the check accepts. Add a functional test for a quoted key and for a key written without spaces. Whichever way, update the KDoc to say what the two share.

## Info

### IN-01: A shell whose operand comes from an expansion now asks with a misleading reason

**File:** `scripts/governance/ask-governed-merge.sh:898-910`
**Issue:** The new dynamic-word check in `analyze_shell` runs for the first word after the shell name. For `bash $SCRIPT; git merge docs-only` I verified that the hook now asks with "cannot tell where the options of bash end: they come from a shell expansion". There `$SCRIPT` is a script path, not an option. It is a legitimate fail-closed outcome (the hook cannot read the script). But the ADR lists "script files that a shell runs" as outside the hook's view, and the message points the maintainer at options that are not there. The ordinary command `bash "$HOME/run.sh" && git merge x` now prompts.

**Fix:** When the dynamic word is the first operand after the shell's options (nothing before it started with `-`), report "cannot read the script of bash: it comes from a shell expansion". Alternatively, treat a dynamic first operand as a script file and stay silent, as for a literal script file. If the prompt is kept on purpose, add a sentence to the ADR.

---

_Reviewed: 2026-11-24_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
