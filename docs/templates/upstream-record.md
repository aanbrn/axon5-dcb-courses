---
id: "UP-NNNN"
product: "<catalog version key, mise tool key, gradle, java, node or action:<owner>/<repo>>"
product-version: "<version>"
status: "open"
workaround: true
upstream: []
reported: null
updated: "YYYY-MM-DD"
---

<!--
Template for a record in docs/upstream/. Copy it to docs/upstream/NNNN-<slug>.md (four digits, lowercase slug),
replace every placeholder, and delete this comment. `./gradlew checkUpstreamRecords` validates the result.

Front matter: only flat `key: value` lines and one-line lists are accepted, with double-quoted or bare strings, `true`,
`false` and `null`. All eight keys are required and `id` must be UP-NNNN with the file's number.

Product key: name the key the dependency-update advisory matches, that is a version alias of
gradle/libs.versions.toml, a tool key of mise.toml, or `gradle`, `java`, `node`, or `action:<owner>/<repo>` for a GitHub
action. A record whose product has no pinned key, such as colima, never matches the advisory; that is fine, say so in
the Symptom section.

Status lifecycle: open -> reported -> fixed-upstream -> workaround-removed, or wont-fix.
- open: found and recorded, not yet reported upstream.
- reported: moving here needs the issue or pull request URL in `upstream` and the date in `reported`; the check fails
  without both. A link to an existing upstream issue counts, with that issue's opening date.
- fixed-upstream: a released version contains the fix; the workaround is still in the code.
- workaround-removed: the workaround is gone from the code, no marker is left and the Removal criteria are met.
- wont-fix: upstream declined. A wont-fix record with `workaround: true` keeps its markers, because the workaround is
  now permanent.

Workaround and markers: a record with `workaround: true` and any status but workaround-removed needs at least one
`UPSTREAM: UP-NNNN` comment (with the record's own number) next to the workaround in the code or configuration. A
record with `workaround: false`, or with status workaround-removed, has none. List every marker location in the
Workaround section.

Publication: an upstream issue is published, commented on or reacted to only after the author has explicitly
approved the draft (UPS-03). Until then the draft stays in this record. Keep tokens, local paths with user names and
e-mail addresses out of the record and the draft.
-->

# UP-NNNN: <short title of the defect>

## Symptom

What goes wrong and where it was observed: the product and version, the exact error text and the impact on this
repository.

## Minimal reproduction

The smallest steps, code or repository link that show the defect on a clean checkout.

## Root cause

Why it happens, with a link to the upstream source line or documentation when there is one.

## Workaround

What this repository does about it.

- `path/to/File.kt`: the code carrying `UPSTREAM: UP-NNNN`, one bullet per marker location.

## Removal criteria

The observable upstream change that makes the workaround unnecessary, for example a released version that contains
the fix.

## How to verify removal

A concrete command or test that proves the defect is gone once the workaround is removed.

## Upstream issue draft

Optional. The issue text for the upstream maintainers, filled from `docs/templates/upstream-issue.md`.
