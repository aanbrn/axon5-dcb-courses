---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0013: Upstream defects are records with two-way workaround markers, checked by `check`

## Context and Problem Statement

Building on Axon Framework 5, Axon Server and a recent toolchain exposes defects that the project works around: an Axon
tag resolver that ignores Kotlin value classes, a retry that cannot classify a remote conflict, an empty token that leaves
Axon Server running with dead endpoints, a container engine that cannot export an image. The project follows an
upstream-first policy: a workaround is temporary, so it must be findable, justified and removable when upstream fixes the
defect (UPS-01 to UPS-04).

Without a system a workaround is a comment that nobody removes, or a record that nobody can tie to code. The questions are
how records and code stay consistent, how a dependency update reminds the author of removable workarounds, and when a
defect is published to an upstream tracker.

## Decision Drivers

- Every workaround in the code points to a record, and every open record points to its workaround.
- The consistency is enforced by `check`, so it cannot rot.
- A dependency update surfaces the workarounds it might make removable, without noise and without write access.
- Nothing is published to a third party without the author's approval.
- The records feed the documentation site.

## Considered Options

- A record per defect under `docs/upstream/` with `UPSTREAM: UP-NNNN` markers in the workaround sites, checked by a Gradle
  task in `build-logic` (chosen).
- A targeted CI check on pull requests that prints the affected records in the job summary and as a warning annotation
  (chosen), versus a pull request comment, versus a static note in the Renovate pull request body.
- The check logic as a Gradle task with TestKit (chosen), shared with the ADR check, versus separate parsers.

## Decision Outcome

Chosen option: "records, two-way markers and a read-only advisory", because it turns the policy into checks that fail
the build at the introducing commit and into a reminder at the moment a dependency moves.

### Records and markers (UPS-01, UPS-02, D-33, D-45)

- **Records.** `docs/upstream/NNNN-<slug>.md`, from `docs/templates/upstream-record.md`, has front matter with the eight keys
  `id`, `product`, `product-version`, `status`, `workaround`, `upstream`, `reported` and `updated`, a `# UP-NNNN: <title>`
  heading and the sections Symptom, Minimal reproduction, Root cause, Workaround, Removal criteria and How to verify removal.
  The `product` names a version key of the catalog, a tool key of `mise.toml`, or `gradle`, `java`, `node` or
  `action:<owner>/<repo>`, which is what the advisory matches.
- **Lifecycle.** `open`, then `reported`, then `fixed-upstream`, then `workaround-removed`, or `wont-fix`. A `reported` record
  needs a non-empty `upstream` list with the issue or pull request URL and the `reported` date; a link to an existing
  upstream issue counts, with that issue's opening date.
- **Markers.** A comment `UPSTREAM: UP-NNNN` sits on its own line next to the workaround. Every marker must name an existing
  record. A record with `workaround: true` needs at least one marker unless its status is `workaround-removed`, and a record
  with `workaround: false` or the status `workaround-removed` has none. The malformed `UP-00010` or `UP-0001x` are errors, not
  markers of another record.
- **`wont-fix` keeps its markers.** If upstream declines, the workaround becomes permanent, so its markers stay (D-45); only a
  record whose workaround has been removed forbids markers. This clarifies UPS-02.
- **One task, one parser.** `checkUpstreamRecords` is a cacheable Gradle task in `build-logic` with TestKit tests, part of
  `check` (carried forward from the Phase 1 decision on governance checks). It shares its front-matter parser with
  `checkAdrs` (D-33 and D-38), so the two cannot drift. It writes a generated status index, a
  Markdown table sorted by `id`, that the documentation site publishes from its Phase 5 skeleton on. The owner of that half
  is recorded in the REQUIREMENTS.md traceability row
  `UPS-02 | Phase 2 (markers, check, index), Phase 5 (index on the documentation site)` and in the ROADMAP Cross-Cutting row
  `CICD-09 (npm half), UPS-02 (index on the site) | Phase 5`.

### The dependency-update advisory (UPS-04, D-32)

`scripts/upstream/changed-dependencies.sh`, run by the `Upstream records advisory` job of `ci.yml`, compares the pinned
versions between the base and the head revision: the keys of the catalog and of `mise.toml`, the Gradle wrapper, `.java-version`,
`.nvmrc` and the `uses:` lines of the workflows. For each record whose `status` is `open`, `reported` or `fixed-upstream` and
whose `product` equals a changed key, it prints the removal criteria in the job summary and as a warning annotation. It
has read-only permissions, never fails on a match and posts no comment. Records are read from the head revision, so a
record added in the same change is judged as merged, and record text is escaped so that it cannot inject a workflow
command. A base that is not in the checkout, as after a force push, skips the advisory with exit 0, because the job
sits under the required `ci-success`.

### Publication (UPS-03)

An issue is published, commented on or reacted to only after the author has approved the draft. The draft stays in the
record, in a verbatim publishable block, until then. Publication of the first batch followed this rule: the author approved
UP-0001, UP-0002, UP-0003, UP-0004 and UP-0007, which became AxonIQ/AxonFramework#5326, #5327 and #5328 and
AxonIQ/axoniq-framework#811 and #812, and the records moved to `reported` with those links. UP-0005 is `reported` through an
existing upstream issue (AxonIQ/AxonFramework#3761). UP-0006 and UP-0013 are drafts for the AxonIQ forum, which the author posts
later. UP-0010 and UP-0011 are withheld until their reproductions are re-verified on a clean setup, and UP-0008,
UP-0009 and UP-0012 link upstream issues that already exist.

A draft for a Discourse forum, such as the AxonIQ forum at `discuss.axoniq.io`, is never filled into the composer in one
automated action. Discourse sends a post from an account at trust level 0 to the moderation queue when its composer was
filled faster than the forum's minimum first-post typing time (`min_first_post_typing_time`, about 3 seconds by
default), and while that post is pending the account cannot open another topic. On 2026-11-25 the UP-0006 topic was held
as pending post 70 and opening the UP-0013 topic was then refused with "Access Denied"; the author's assessment is that
the text had been entered too fast for a person, which matches that setting. So the author enters the approved text in
the browser, in parts or with a pause before submitting, or it is typed character by character.

### Consequences

- Good, because a workaround without a record, or an open record without a workaround, fails the build.
- Good, because a Renovate or hand-made version bump shows which workarounds to re-test, with their removal criteria, in the
  pull request.
- Good, because the same data feeds the documentation site, and publication has one visible gate.
- Bad, because the advisory matches version keys, so a record about a product without a pinned key (colima, GSD) never matches,
  and the record says so itself.
- Good, because `scripts/doctor.sh` lists every fixed-upstream record whose markers remain, with its removal criteria,
  whatever its product, which covers the records the advisory cannot match.
- Bad, because markers are text conventions: a workaround that is not marked is invisible to the check. Reviewing new
  workarounds for a marker is a review duty.
- Bad, because publication needs the author, so a defect can wait unreported.

### Confirmation

- `checkUpstreamRecords` runs in `check`, with TestKit tests in `build-logic`; it fails on a missing record, a missing or
  surplus marker, a malformed marker and a `reported` record without link and date.
- `scripts/upstream/test-changed-dependencies.sh` runs with the other shell tests in CI, and the `Upstream records advisory`
  job runs the script on every pull request.
- Each published record carries the issue link and the date, which the check requires.

## Pros and Cons of the Options

### Records and two-way markers

- Good, because consistency in both directions is machine-checked.
- Bad, because every workaround costs a record and a marker.

### A targeted CI check with job summary and annotation

- Good, because it needs read-only permission, posts nothing and shows only the records that the change touches.
- Bad, because it is visible only to someone who opens the job or the pull request checks.

### A pull request comment

- Good, because it sits in the conversation of the pull request.
- Bad, because it needs write permission on pull requests, which the workflows avoid, and a comment from a job is one more
  thing to maintain.

### A static note in the Renovate pull request body

- Good, because it needs no job.
- Bad, because it is not tied to what the change touches, so it cannot name the records that the update affects.

## More Information

- Decisions: D-32, D-33 and D-45 (clarifying UPS-02), and UPS-01 to UPS-04.
- The templates are `docs/templates/upstream-record.md` and `docs/templates/upstream-issue.md`.
- Revisit the advisory when records matching a product without a pinned key become common (then a key for it is needed), and
  the publication gate when a second maintainer joins.
