# Feature Landscape

**Domain:** DCB (Dynamic Consistency Boundary) reference implementation of "students subscribe to courses" on Axon Framework 5, with React UI, CQRS service split, teaching-grade docs
**Researched:** 2026-11-20
**Overall confidence:** MEDIUM-HIGH. Canonical-example facts (HIGH) come from dcb.events and AxonIQ docs fetched this session. The extension rules (waiting list, transfer, schedule, unique name) are not covered by any published DCB sample I could find, so they are my design recommendations (MEDIUM). Reference-repo quality norms are mostly from general knowledge plus thin search results (LOW-MEDIUM). Anything needing verification against AF5 5.x/Axon Server is marked **VERIFY**.

---

## 0. What the canonical examples actually contain (ground truth)

### 0.1 dcb.events "Course subscriptions" (HIGH, fetched)

The page presents an imagined app derived from Sara Pellegrini's "Killing the Aggregate" article series. It is notation-level (language-agnostic), not an Axon implementation.

| Item | Content |
|------|---------|
| Tag types | `CourseId`, `StudentId` |
| Events | `CourseDefined{courseId, capacity}` [tag: courseId]; `CourseCapacityChanged{courseId, newCapacity}` [courseId]; `StudentSubscribedToCourse{studentId, courseId}` [studentId, courseId] |
| Commands | `DefineCourse`, `ChangeCourseCapacity`, `SubscribeStudentToCourse` (no unsubscribe, rename or enrol-student in the base page) |
| Invariants | Define: course must not exist. ChangeCapacity: course exists AND capacity != new capacity ("Capacity is unchanged"). Subscribe: course exists; subscriptions(course) < capacity ("Course is full"); student not already subscribed; subscriptions(student) < **5** ("too many courses") |
| Decision model for Subscribe | 3 event types, 2 tags: `CourseDefined`/`CourseCapacityChanged`/`StudentSubscribedToCourse` by `CourseId`, plus `StudentSubscribedToCourse` by `StudentId`. Append condition = `failIfEventsMatch(combined query) after(last read position)`. Appended event carries both tags |
| Key teaching points | Narrow criteria: `DefineCourse` reads only `CourseDefined` for that course. One event (`StudentSubscribedToCourse`) belongs to two boundaries via two tags. Each projection is composable and the decision model is their union |

The "Aggregates and DCB" article (sliceworkz, MEDIUM) frames the same case as: two invariants (max courses per student, course capacity) each break one aggregate. Alternatives rejected are merging into a God aggregate and a saga with compensation. It notes aggregates can still coexist, for example to block capacity reduction below current count.

### 0.2 AxonIQ AF5 getting-started / university-demo (HIGH, fetched)

The repo `AxonIQ/university-demo` is archived. It moved into the `university-demo` module of the AxonFramework repo, and the docs live on docs.axoniq.io (observed up to the 5.4 docs path). It is a Faculty bounded context with vertical slices (`write` / `read` / `automation` / shared `events` package), tested with `AxonTestFixture` (given events, when command, then events; given events, then read model; when event, then command).

| Item | Content |
|------|---------|
| Events | `StudentEnrolledInFaculty` [studentId], `CourseCreated` [courseId], `CourseCapacityChanged` [courseId], `CourseRenamed` (referenced), `StudentSubscribedToCourse` [studentId, courseId], `StudentUnsubscribedFromCourse` [studentId, courseId] |
| Capabilities | Create course with capacity; change capacity; change name; enrol student in faculty; subscribe within capacity and student limit; unsubscribe |
| Subscribe rules | Student enrolled (throws, so the error reaches the client); course exists; not already subscribed; student has fewer than **3** courses (hardcoded, differs from dcb.events' 5); course not full |
| Test scenarios | success; already subscribed; subscribed to another course (ok); other student holds a seat (ok); course full (reject); student at limit (reject) |
| Decision state | State holds only counts and flags: `noOfCoursesStudentSubscribed`, `alreadySubscribed`, `courseCapacity`, `noOfStudentsSubscribedToCourse`. A pure `decide` function; entity id is a composite `SubscriptionId(courseId, studentId)` via `@TargetEntityId` |
| Criteria | `@EventCriteriaBuilder` combines with `either`: course criteria (courseId tag; Created, CapacityChanged, Subscribed, Unsubscribed) OR student criteria (studentId tag; Enrolled, Subscribed, Unsubscribed). **`CourseRenamed` is deliberately excluded so renames never conflict with subscriptions** |
| Tag semantics | Tags inside one `havingTags` are AND; split into separate criteria and `either` to get OR. A very common mistake |
| Tutorial chapters | Course creation; Subscribe student; Axon Server connection; stateless notification automation; stateful automation (admin notified when ALL courses are fully booked); course-statistics read model |
| Not present | UI, waiting list, transfer, schedule, unique name, multi-service split, observability, E2E/load tests. **This project's gap versus the official sample is exactly its differentiation** |

### 0.3 Other DCB examples worth citing (HIGH, fetched from dcb.events)

- **Unique username**: events carry tag `Username:{name}`. `ChangeUsername` appends `UsernameReleased(old)` + `UsernameClaimed(new)` in ONE append, tagged with both names. Projection `UsernameTaken` flips false on release. Notes: normalise or hash tag values (case sensitivity); renaming to one's own name is not addressed (the model would reject it as "taken"); optional retention period before reuse. This is the direct template for the unique course name feature.
- **Bike rental**: guard a transition with another entity's state without merging. Narrow commands read only their own tag. Conditional emit: `BikeReturned` + `BikeSentToRepair` in one decision. This is the template for the conditional multi-event emit used by transfer and waiting-list promotion.
- Others (library loans, invoice number, opt-in token, idempotency id) are optional "see also" links in the docs, not features.

---

## 1. Domain features (write side)

Notation: **B** = boundary read (tag: event types). Every write decision uses `failIfEventsMatch(criteria) after(marker)`.

Global rules I recommend, applied to all commands:
- Commands carry a client-generated idempotency key or ids (course/student ids generated by the client or BFF). Duplicate create is rejected by the "already exists" invariant, which is a free idempotency story.
- Rejections are first-class, typed domain errors (stable codes like `COURSE_FULL`), surfaced as RFC 9457 problem+json by the BFF. The university-demo notes that "no events" vs "throw" is a style choice; pick **throw typed errors** consistently so UI and tests can assert on them.
- Constants (student course limit, min/max capacity) are configuration, not events. Pick one limit (suggest **3**, matching the official AF5 sample and keeping demos tractable) and document that dcb.events uses 5.

### 1.1 Table-stakes domain commands (core DCB story)

| Feature | Business rules | Boundary (B) | Edge cases to specify and test | Complexity |
|---------|---------------|--------------|-------------------------------|------------|
| **RegisterStudent** (a.k.a. enrol in faculty) | Student id must not exist; name/email required | `StudentRegistered`: tag studentId | Duplicate id rejected. Do NOT make student name or email unique (scope creep; keep unique-name demo on courses) | Low |
| **CreateCourse** | Course id not already used; capacity >= 1; name unique (see 1.2); optional schedule slots | tags courseId + `CourseName:{normalized}`: `CourseCreated`, `CourseRenamed` | Id collision; name collision; capacity 0 or negative; whitespace or case variants of the name | Low-Med |
| **ChangeCourseCapacity** | Course exists; new capacity != current ("unchanged", matching dcb.events); new capacity >= 1; **new capacity >= current subscription count** | courseId: `CourseCreated`, `CourseCapacityChanged`, `StudentSubscribed`, `StudentUnsubscribed`, `StudentTransferred` (in/out) | **Reduce below current subscriptions: REJECT** (`CAPACITY_BELOW_SUBSCRIPTIONS`). The alternative (allow over-subscription, no eviction) breaks the "capacity is never exceeded" invariant statement and complicates every read model. Reduce to exactly current count is allowed. A concurrent subscribe vs a concurrent reduce conflict on shared criteria; exactly one wins (a good race-demo variant). Increase triggers waitlist promotion for possibly several seats | Med |
| **SubscribeStudentToCourse** | Student registered; course exists; not already subscribed; student subscriptions < limit; course seats free; (extensions: no schedule overlap; no waitlist barging) | courseId OR studentId (university-demo `either` pattern). **Exclude `CourseRenamed`** | Already subscribed; student exactly at limit (reject) vs one below (ok); last seat race; two courses racing for the same student's last slot under the limit (conflict on student criteria) | Med |
| **UnsubscribeStudentFromCourse** | Student must currently be subscribed; frees a seat | studentId+courseId pair | Not subscribed; double click (idempotent from the user's perspective: second call rejected `NOT_SUBSCRIBED` and UI treats it as a no-op); triggers waitlist promotion | Low-Med |
| **RenameCourse** | See 1.2 | courseId + old name tag + new name tag | See 1.2 | Med |

Complexity for the "must nail" ones is in testing concurrency, not in the decision logic.

### 1.2 Extension: unique course name with release on rename (table stakes per PROJECT.md)

- **Events:** `CourseCreated{courseId, name, capacity, ...}` tagged `CourseId:{id}` and `CourseName:{key}`. `CourseRenamed{courseId, oldName, newName}` tagged `CourseId`, `CourseName:{oldKey}`, `CourseName:{newKey}`. The old-name tag on the rename event is what lets the name-owner projection release the old name, mirroring dcb.events' `UsernameReleased`.
- **Projection:** `CourseNameOwner(key)` is `courseId | none`. Set on Created and on Renamed(new). Cleared on Renamed(old).
- **Decision (Rename):** reads `CourseId:{id}` events (existence and current name) plus `CourseName:{newKey}` events (owner). Append condition covers both. Rejects if owner(newKey) is a different course.
- **Normalisation:** trim, collapse whitespace, Unicode NFKC, case-fold, and use the normalised key as the tag value (dcb.events recommends normalising/hashing). Display name keeps the original casing. **VERIFY** tag value length and charset limits in Axon Server.
- **Edge cases:**
  - Rename to its own exact name: reject as `NAME_UNCHANGED` (consistent with `capacity unchanged`) or accept as no-op; pick one and test it. The naive "is the name taken?" projection would wrongly reject it, so use **owner == me**, not a boolean.
  - Case-only rename of one's own course ("math" to "Math"): owner is self, so allowed as a display change, no new claim.
  - Rename racing with create of the same name by another course: both read `CourseName:{key}`, and exactly one appends.
  - Released name reused immediately by another course: allowed (no retention period in v1; retention is a dcb.events optional extension, defer).
  - Name swap between two courses (A to B, B to A): not atomically possible without a temp name. Document as a known limit, do not build.
  - Renaming does not conflict with subscriptions because subscription criteria exclude `CourseRenamed` (shows narrow boundaries).
- **Complexity:** Med.

### 1.3 Extension: atomic transfer between courses (table stakes per PROJECT.md; the strongest showcase)

- **Command:** `TransferStudent(studentId, fromCourseId, toCourseId)`.
- **Rules:**
  - Student is currently subscribed to `from`.
  - `to` exists and has a free seat. **Transfer to a full course is rejected** (`TARGET_COURSE_FULL`). Recommend NOT auto-waitlisting transfers: the student would be left in limbo and the unsubscribe-then-waitlist semantics are ambiguous. The student keeps the source seat.
  - `from != to` (`SAME_COURSE`).
  - Student not already subscribed to `to`.
  - Student limit is unchanged by a transfer, so a student at the limit CAN transfer (important test case; a naive "unsubscribe then subscribe" implementation passes this accidentally and a naive "subscribe" check would fail it).
  - Schedule: overlap against the student's other subscriptions **excluding `from`**. Moving into a slot that only clashes with the course being left is valid.
  - Waitlist fairness: reject if `to` has a non-empty waitlist and a free seat (no barging); see 1.4.
- **Boundary (three entities, one decision):** criteria = `either(course(to), course(from), student)`. This is the "one decision spanning three entities" claim from PROJECT.md, and it is the cleanest demonstration that aggregates cannot do this atomically.
- **Event shape, my recommendation:** one dedicated event `StudentTransferredBetweenCourses{studentId, fromCourseId, toCourseId}` tagged `StudentId`, `CourseId:{from}`, `CourseId:{to}` (one fact, three tags, visually perfect in the event explorer).
  - Alternative: two existing events (`StudentUnsubscribed` + `StudentSubscribed`) in one append. This reuses projections and automations unchanged but loses intent.
  - **Risk of the dedicated event:** every projection and decision model that counts subscriptions must handle it (source -1, target +1, student unchanged). A missed handler is a silent invariant leak. Mitigate with a shared "subscription facts" fold used by all decision states and a property-based test.
  - Record the choice in an ADR.
- **Edge cases:** source seat freed, so source waitlist promotion fires; concurrent transfers into the last seat of `to` (one wins); concurrent unsubscribe from `from` while transferring (one wins); transfer racing with capacity reduction on `to`.
- **Complexity:** Med-High (criteria union plus shared fold).

### 1.4 Extension: waiting list with auto-enrolment (table stakes per PROJECT.md; highest design risk)

The invariant (never exceed capacity) is hard and DCB-enforced. Promotion is a **policy**: asynchronous, eventually consistent, driven by the automation service. Separate those two clearly in docs.

**Events:** `StudentAddedToWaitlist{courseId, studentId}` and `StudentRemovedFromWaitlist{courseId, studentId, reason}` with reason in {PROMOTED, LEFT, INELIGIBLE}; both tagged courseId + studentId. Promotion appends `StudentSubscribedToCourse` + `StudentRemovedFromWaitlist(PROMOTED)` in ONE append (never a state where the student is both or neither).

**Commands:**
- `SubscribeStudentToCourse` becomes a request with outcome-dependent events: seat free and waitlist empty gives Subscribed; course full (or waitlist non-empty) gives AddedToWaitlist. One command, state-dependent events (the bike-rental conditional-emit pattern). Same invariants apply at join time (student registered, not already subscribed or waitlisted, student below limit, no overlap). Do not let a student wait for a course they could not take.
- `PromoteFromWaitlist(courseId)` is issued by the automation service. Decision: seat free AND queue non-empty AND the head candidate is still eligible. It reads course-tagged events for queue order, and the candidate's student events for limit and overlap, so the automation passes the candidate `studentId` (read from the waitlist read model) and the decision **re-validates that the candidate is truly the head** (the read model may be stale; the event store is truth). **VERIFY** the cleanest AF5 pattern for a criteria that depends on a first-pass result (two-step read, or candidate passed in the command).
- `WithdrawStudentFromCourse(studentId, courseId)`: **one** user-facing command whose decision reads both subscription and waitlist state. Outcome: Subscribed gives `StudentUnsubscribed`; Waitlisted gives `StudentRemovedFromWaitlist(LEFT)`; neither gives reject `NOT_SUBSCRIBED_OR_WAITLISTED`. Rationale: with two separate commands, a student clicking "leave waitlist" while being promoted gets a confusing rejection. One decision over both facts eliminates this race, a nice DCB talking point.

**Ordering and fairness rules:**
- FIFO by **event-store position of `StudentAddedToWaitlist`**, not wall-clock timestamps (clock skew, ties).
- **No barging:** a direct subscribe while seats are free but the waitlist is non-empty must enqueue instead of taking the seat. Otherwise a newcomer steals the freed seat in the window before promotion fires. Cost: join decisions must read the waitlist events, so concurrent joins conflict and retry (contention on hot courses). Accept it, since correctness beats throughput in a reference, and document the trade-off. The narrower alternative (joins read only seat counts) is faster but allows barging.
- **Ineligible head** (at student limit or overlap since joining): emit `StudentRemovedFromWaitlist(INELIGIBLE)` and continue to the next candidate. Never block the queue behind a student who cannot take the seat. Log or notify (the stateless-notification pattern from the AF5 tutorial).
- Waitlisting holds no seat and does not count towards the student limit. Eligibility is re-checked at promotion.
- No waitlist size cap in v1.

**Triggers for promotion (automation listens to):** `StudentUnsubscribed`, `StudentTransferred` (source freed), `CourseCapacityChanged` (a capacity increase may free N seats, so the automation loops until no seat or no candidate), `StudentRemovedFromWaitlist(INELIGIBLE or LEFT)` when a seat is free and the head changed. A startup or periodic sweep is a safety net: a missed trigger would otherwise strand a free seat forever.

**Edge cases to test:**
- Unsubscribe while waitlisted (covered by `Withdraw`).
- Student on waitlist subscribes elsewhere and hits the limit, so promotion is skipped as INELIGIBLE.
- Promotion racing a manual subscribe or transfer into the same seat (append condition makes exactly one win, the loser retries and re-evaluates).
- Two automation instances promoting the same head (the second conflicts and re-evaluates, so it is idempotent by construction; show this).
- Capacity increase by 3 with 2 waiters promotes 2 and leaves 1 free.
- Transfer into a course with a free seat but waiters is rejected (no barging).
- Automation is down while seats free up, then recovers (processor catches up from its token).
- Waitlisted student's course is renamed (no effect).

**Complexity:** High. This is the biggest risk to schedule and correctness. It needs deeper phase research (see Research Flags).

### 1.5 Extension: schedule slots with overlap prevention (table stakes per PROJECT.md; hardest boundary-shaping problem)

**The problem:** a student's overlap check needs the schedules of all their subscribed courses, but a `StudentSubscribed` event is tagged with studentId and courseId only. The set of courses is not known before reading, so criteria cannot be static.

**Recommended design (keeps the boundary static and small):**
- Use a **fixed slot grid**: a catalogue of weekly slots (e.g. `MON-09:00-10:30`). A course occupies a set of slot ids. Overlap becomes set intersection and avoids interval-arithmetic edge cases (back-to-back classes, midnight).
- Subscription events **copy the course's slot ids** (`StudentSubscribedToCourse{..., slotIds}`), a fact recorded "as scheduled at subscription time". The student's own events then carry everything needed, so subscribe criteria stay `either(course, student)`.
- **Invariant on schedule change:** a course's slots can only be changed while it has **zero subscribers** (reject `COURSE_HAS_SUBSCRIBERS`). Otherwise a schedule change would have to read every subscriber's whole timetable (unbounded boundary and a mass-conflict). Document this restriction as a deliberate modelling choice with its trade-off. Waitlisted students are unaffected because eligibility is re-checked at promotion.
- Events: `CourseScheduleSet{courseId, slotIds}` (or fold the slots into `CourseCreated` plus `CourseScheduleChanged`), tagged courseId.
- Rules: slots must exist in the grid; a course may have 0 slots (unscheduled, never conflicts) or a few; the student's new slots must not intersect any slot of their current subscriptions (excluding `from` on transfer).
- **Edge cases:** two courses with identical slots (reject second); partial overlap (one shared slot among several); back-to-back distinct slots (ok); unsubscribe frees slots; transfer between overlapping courses (ok when only the source overlaps); concurrent subscribe to two overlapping courses by the same student (student criteria conflict, one wins); schedule change racing a first subscribe (course criteria conflict).
- **Alternative to record in an ADR:** arbitrary time intervals with a two-pass criteria. It is more realistic but breaks the static-boundary elegance and adds interval-edge complexity. Put it in the "what next" docs, not the code.
- **Complexity:** High. It touches subscribe, transfer, promotion and the UI timetable. Build it after transfer and waitlist are stable, or you will rework both.

### 1.6 Differentiating domain extras (optional, cheap)

| Feature | Value | Complexity | Notes |
|---------|-------|------------|-------|
| Narrow-boundary "no-conflict" proof test | Executable evidence that rename / schedule change on course X does not abort subscribes that don't read those events (the `CourseRenamed` exclusion lesson) | Low | Pair with a test that DOES conflict on a mistaken broad criteria |
| Idempotent re-delivery test for automation | Shows effects of at-least-once delivery | Low-Med | Replays `StudentUnsubscribed` and asserts only one promotion |
| Stateful automation: "all courses fully booked, notify admin" | Mirrors official AF5 tutorial chapter; shows a stateful event handler | Med | Cheap to add once read models exist; surface as an admin alert in the UI |
| Course statistics read model | Mirrors official tutorial; useful admin dashboard | Low | See 2 |

### 1.7 Deliberately deferred domain features

Course archive/delete (must release the name, clear waitlist, handle subscribers; opens a large edge-case surface), configurable per-student limit as an event (global config boundary), waitlist cap, retention period for released names, prerequisites, grades or completion, student deregistration, multi-faculty or multi-context, payments. See Anti-Features.

---

## 2. Read side / CQRS views

All views are projections fed by the query service from the same events. Queries go through the QueryBus (BFF to query service), and live updates use subscription queries (PROJECT.md). Views are denormalised and per-screen. Never reuse a decision model as a read model.

### 2.1 Table stakes views

| View | Consumer | Content | Complexity | Notes |
|------|----------|---------|------------|-------|
| **Course catalogue** | student, admin | id, name, capacity, subscribed count, free seats, waitlist length, schedule slots, status flags (full / has waitlist) | Low | Primary list. Shows "seats left" as a non-authoritative hint (eventually consistent) |
| **Course detail** | both | above plus roster (admin) and waitlist in order (admin; students see only their own position) | Low-Med | Position = index in FIFO order |
| **Student directory** | admin, role switcher | id, name, subscription count, waitlist count | Low | Powers the role switcher |
| **My subscriptions and timetable** | student | subscribed courses with slots, rendered as a weekly grid | Med | Timetable is the best visual for overlap rules |
| **My waitlist** | student | waitlisted courses with position, updated live as the queue moves | Low-Med | Highlights auto-enrolment visibly |
| **Course name directory** | internal (BFF validation hint) | normalised-name to courseId | Low | Optional UX pre-check only. Never authoritative (the decision is). Explicitly demonstrates "read model for UX, event store for invariants" |
| **Command status or outcome feed** | both | the latest rejection or success per user action | Low | Often just the HTTP response and toast |

### 2.2 Admin views

| View | Content | Complexity |
|------|---------|------------|
| **Dashboard / statistics** | courses count, total subscriptions, fill rate per course, fully-booked courses, waitlist totals (mirrors the official statistics-read-model tutorial) | Low-Med |
| **Capacity pressure** | courses sorted by waitlist length or fill rate | Low |
| **Schedule grid** | all courses by slot, to spot collisions in the grid | Med |
| **Per-student detail** | their subscriptions, waitlist entries, limit usage | Low |

### 2.3 Differentiating views (teaching)

| View | Value | Complexity |
|------|-------|------------|
| **Event stream / explorer** (section 3.2) | The headline differentiator | High |
| **Projection health / lag** (token position vs head) | Makes eventual consistency visible; pairs with OTel | Med |
| **Decision log** (command to criteria to marker to appended events or conflict) | Shows the boundary per command | High (see 3.2) |
| **Automation log** (promotions, skips with reason) | Shows the policy and invariant split | Low-Med |

**Dependencies:** all views depend on events carrying enough payload; the timetable depends on `slotIds` in the subscription events; the waitlist view depends on the waitlist events and the transfer event being handled in every counting projection.

**Pitfall tie-in:** read models lag. UI must not infer "I can subscribe" from a view and must treat the command result as the source of truth. Read-your-own-writes needs subscription-query updates or polling with a wait-for-position mechanism. **VERIFY** whether the query side can expose its tracking position per event.

---

## 3. UI features

### 3.1 Table stakes (demo/admin app expectations)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Role switcher (admin / student X) | Required by PROJECT.md; replaces auth | Low | Persist choice in the URL or local storage. Make clear in the UI it is demo-only |
| Course list and detail with seats, waitlist, schedule | Core screen | Low-Med | |
| Admin: create / rename / change capacity / set schedule forms | All write-side operations must be reachable (PROJECT.md: "covering all business operations") | Med | |
| Student: subscribe, unsubscribe/withdraw, transfer, join waitlist | Core flow | Med | Transfer needs target selection with clear disabled-state reasons |
| Student registration | Needed to have actors | Low | |
| Human-readable domain errors | Rejections are the teaching moment ("Course is full", "Schedule overlaps with X") | Low-Med | Map stable error codes to messages and highlight the violated invariant |
| Live updates (SSE or WebSocket over subscription queries) | Required by PROJECT.md | Med-High | SSE preferred (one-way, simpler through the BFF). **VERIFY** AF5 subscription query API and backpressure |
| Loading, empty and error states, toasts, responsive layout, keyboard and ARIA basics | "Polished" bar | Low-Med | Playwright E2E needs stable selectors (`data-testid`) |
| Timetable (weekly grid) | Makes schedule overlap legible | Med | |
| Generated, typed API client from OpenAPI | Keeps UI and BFF in sync | Low | Fits "OpenAPI spec" requirement |

### 3.2 Differentiators (what makes this a teaching app, not a CRUD demo)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Event explorer with tags** | Browse the event stream as a table or timeline. Filter by type, by tag (`CourseId:x`), by time; click a tag to see every event sharing it. Shows that one event belongs to several boundaries | High | Needs a read path over the event store (query service reading from Axon Server, or its own append-only copy projection with global position). **VERIFY** the Axon Server DCB read API from a query service |
| **"Boundary of this command" view** | For a chosen command (or any decision in the log), highlight the events that were in its criteria (matching tags and types), the consistency marker position, and the events appended. Beside it, show a faded "events NOT in the boundary" | High | Axon does not expose the criteria of a past decision. Requires a **decision trace**: wrap or intercept command handling to record `{commandName, criteria (tags and types), consistencyMarker, appendedEvent ids, outcome, retryCount}` to a side store or OTel span attributes. Plan it in the command service from the start. Retrofitting is painful |
| **Rejection explainer** | On a rejected command, show the violated rule and the projected state values that caused it (capacity 2, count 2) | Med | Falls out of the decision trace |
| **Conflict and retry visibility** | Show when a command was aborted by a concurrent append and retried; show retry counts | Med-High | Needs knowledge of AF5 conflict exception and retry behaviour. **VERIFY** (my search found no AF5-specific retry documentation) |
| **Concurrency race demo** | Button "N students race for the last seat" fires N parallel subscribe commands. Live result: 1 success, N-1 rejected or waitlisted, capacity never exceeded; per-request latency and retry counts; final seat count | Med-High | Fire commands server-side (a BFF demo endpoint) to avoid browser connection limits and to get true parallelism. Pair with a repeatable k6 or Gatling load test that asserts the same property. Variant races: capacity reduction vs subscribe; two courses vs the student's last slot; two renames to the same name; two transfers into the last seat |
| **Live updates that make automation visible** | The waitlist auto-promotion moves a student from waitlist to subscribed in front of you; toast "promoted from waitlist" | Low once live updates exist | Best single "wow" moment after the race demo |
| **Scenario seeding and reset** | One click to load a named scenario ("last seat", "full course with waitlist", "overlapping schedules", "student at limit") and reset to clean | Med-High | **Seed only through the command API, never by writing events directly**, so invariants are not bypassed. Reset needs a dev-only mechanism, since the event store is append-only: recreate the Axon Server context (or wipe the volume) and clear projections. **VERIFY** the admin API. Gate the reset endpoint to a demo profile |
| **Guided scenarios ("Try this" cards)** | Each card names the invariant, the boundary, the steps to perform, and links to the docs page and the test | Med | Turns the UI into the front door of the documentation |
| **Diagram of the boundary** (tags as chips; criteria as a Venn-style highlight) | Pedagogical clarity | Med-High | Optional polish, only after the explorer works |
| **Links from UI to docs / code** | Portfolio value | Low | |
| **Tracing deep links** (trace id to Grafana/Tempo) | Ties UI to observability | Low-Med | Only if OTel lands first |
| Dark mode, i18n | Polish | Low | Not teaching value. Defer unless free |

### 3.3 UI anti-patterns for this app
See Anti-Features (no client-side invariant enforcement, no optimistic mutation of read data as truth, no fake authentication).

---

## 4. Reference-project quality features

(Confidence LOW-MEDIUM. Search results were generic. The list reflects widely accepted open-source reference practice and what the official AF5 sample actually ships: Event Modeling images in `docs/images`, a `base` branch to start fresh, a getting-started guide, CI workflows, a docker-compose with Axon Server, and AxonTestFixture-style tests.)

### 4.1 Table stakes for a "highest professional class" reference

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **One-command start** (`docker compose up`) with seeded demo data | Spring PetClinic's key success factor is "clone, run, see it" | Med | Healthchecks, pinned image digests, Axon Server DCB context auto-created |
| **README** with: pitch, "why DCB", a 60-second quick start, screenshots/GIF, architecture diagram, feature to scenario table, links to docs | First impression | Low-Med | Lead with the problem (multi-entity invariant) not the tech |
| **Tests that read as documentation** (given-when-then per rule, named by business rule) | Reference code must be learnable | Med | A rule-to-test traceability table |
| **ADRs** (MADR format) for the key decisions | Shows why not just what | Low per ADR | Topics: DCB vs aggregates; tag taxonomy; transfer event shape; waitlist fairness; slot grid; read-model strategy; service split; event-store reset strategy; no-auth |
| **C4 diagrams as code** (Structurizr DSL, PlantUML or Mermaid) checked in | Diagrams that rot are worse than none | Med | Context, Container, and a Component view of the command service |
| **Event Modeling artifacts** (board image plus per-slice description) | Official AF5 sample ships these; DCB community speaks this language | Med | Slices map 1:1 to code packages |
| **Documentation site** (MkDocs Material, Docusaurus or Antora) published via GitHub Pages | Searchable, navigable | Med | Include a glossary or ubiquitous language page |
| **"DCB concepts as applied here"** page | Core Value per PROJECT.md | Med | Tags, criteria, decision model, append condition, consistency marker, each shown in this repo's code |
| **Aggregate vs DCB comparison** | The "before/after" story | Med | Per scenario: aggregate sketch, why it fails (God aggregate / saga + compensation / eventual consistency), DCB solution. Be fair: say where aggregates remain fine |
| CONTRIBUTING, LICENSE, SECURITY, issue templates, CHANGELOG/releases | Community hygiene | Low | |
| CI badges, coverage badge | Signals quality | Low | |
| Pinned and documented tool versions (e.g. devcontainer or `mise`) | Reproducibility | Low-Med | |

### 4.2 Differentiators

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Scenario catalogue (living documentation)**: each business rule has an id (e.g. `INV-CAP-01`) linked to the decision code, the given-when-then test, the E2E test and the UI "try this" card | Traceability that very few samples have | Med | Optionally generated from test names or annotations |
| **"Why this boundary" per command**: a table of command, tags, event types read, tags written | Directly teaches DCB; doubles as spec for the decision-trace feature | Low-Med | |
| **"When NOT to use DCB / when aggregates are fine"** page | Credibility. Azure guidance warns event sourcing is often not justified | Low | |
| **Failure-mode and operations page** (what happens on conflict, retry, projection lag, Axon Server down) | Production-grade honesty | Med | |
| **"How this was built" section**: publish `.planning/` (roadmap, decisions, research), prompts and review practices, honest lessons learned (where AI was wrong, how it was verified against docs) | The second stated goal, and unusual for a reference | Low-Med | Keep it curated: a short narrative page plus links to raw artifacts. Don't leave stale planning noise at the repo root |
| **Benchmark/load-test results page** with methodology | Evidence the invariants hold under load | Med | Pair with the k6/Gatling test |
| **Observability tour** (Grafana dashboards committed as JSON; screenshot walkthrough) | Operational credibility | Med | |
| **Migration guide: Axon 4 aggregate developer to DCB** (concept mapping table) | Target audience per PROJECT.md | Med | `@Aggregate` / `@AggregateIdentifier` to `@EventSourcedEntity` / tags, etc. Verify against AF5 migration docs |
| Supply-chain hygiene visible in README (Renovate, vuln scanning, secret scanning, SBOM) | Portfolio signal | Low (config) | PROJECT.md already scopes it |
| Walkthrough article / blog draft or talk outline | Portfolio | Low-Med | Derived from the docs, not duplicated |

---

## 5. Anti-Features

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| A working aggregate-based implementation | Doubles effort; already out of scope (PROJECT.md) | Documentation-only comparison with diagrams and snippets |
| Splitting write side by entity (Course service / Student service) | Contradicts the DCB point | One command service; split by CQRS role only |
| Real authentication/authorization (OIDC, Keycloak) | Out of scope for v1; dilutes focus | Role switcher, clearly labelled demo-only |
| Using a read model to enforce an invariant (e.g. pre-checking name uniqueness or seat counts from a projection and trusting it) | Eventually consistent, so racy. This is exactly the anti-pattern DCB removes | Read models for UX hints only; the decision model over the event store is the only authority |
| Sagas / process managers with compensation for the invariants | The "before" approach DCB replaces | Single atomic append; automations only for policies (promotion, notifications) |
| One big "God" decision model reused by every command | Defeats narrow boundaries; causes needless conflicts | One decision model per command, each reading only what it needs (type + tag filters) |
| Broad criteria by accident (e.g. all events for a course tag without type filters) | Spurious conflicts, e.g. rename aborting subscribe | Filter by type as in the official sample; test that unrelated events do not conflict |
| Snapshots, upcasters, event-schema versioning machinery in v1 | Premature; boundary sizes here are tiny | Mention in "what next" docs. Keep payloads versionable |
| An "invariants off" toggle or deliberately broken mode in the code | Dead code, risk of confusion, test matrix explosion | Show the failure case in docs and, if needed, an isolated test that demonstrates the race with a naive check |
| Direct event insertion for seeding/reset or "edit event" admin tools | Bypasses invariants; teaches the wrong thing | Seed through commands; reset via context recreation (demo profile only) |
| Course delete/archive, configurable limits as events, retention windows, waitlist caps, prerequisites, grades | Large edge-case surface, little extra DCB insight | Deferred list; mention in docs as exercises |
| Arbitrary-interval schedule overlap in v1 | Forces two-pass dynamic criteria and interval edge cases | Slot grid now; ADR notes the alternative |
| Auto-waitlisting transfers | Ambiguous semantics (student in limbo) | Reject transfer to a full course |
| Cross-context / multiple bounded contexts, multi-tenant | DCB boundaries are within a context | Single Faculty context |
| Reflection-heavy generic framework layer over AF5 | Hides what is being taught | Plain, readable AF5 usage, commented |
| Kubernetes / cloud deployment | Out of scope | docker compose |
| Duplicated "docs about docs": planning noise at the repo root, auto-generated walls of API docs without narrative | Looks sloppy | Curated docs site; `.planning/` summarised on one page |
| UI-side enforcement of invariants | UI hints only; server is the authority | Disable buttons for guidance but always handle rejections |

---

## Feature Dependencies

```
RegisterStudent ---------------------------------------+
CreateCourse (+ unique name) --> RenameCourse           |
CreateCourse --> ChangeCourseCapacity                   |
CreateCourse + RegisterStudent --> Subscribe --> Unsubscribe/Withdraw
Subscribe + Unsubscribe --> Transfer (needs same shared "subscription facts" fold)
Subscribe + Capacity --> WaitlistJoin (outcome of the Subscribe request)
Unsubscribe / Transfer / CapacityIncrease --> Automation: PromoteFromWaitlist
Withdraw (decision over subscription + waitlist state) requires Waitlist events
CourseSchedule (slot grid) --> Subscribe overlap check (subscription events carry slotIds)
CourseSchedule --> Transfer overlap check (exclude source course)
CourseSchedule --> Promotion eligibility re-check
Subscribe / Transfer / Waitlist events --> Projections --> QueryBus --> Subscription queries --> BFF SSE --> UI live updates
Decision trace (command service instrumentation) --> Boundary-of-command view --> Rejection explainer --> Guided scenarios
Event store read path (query service) --> Event explorer
Commands-only seeding + dev-only context reset --> Scenario seeding/reset --> Race demo repeatability --> Playwright E2E
Race demo + load test both assert: seats never exceed capacity
Observability (OTel) --> trace deep links, decision trace as span attributes
Docs: Event Modeling --> ADRs --> "Why this boundary" tables --> comparison page --> scenario catalogue
```

Ordering consequences:
1. Build the base DCB slice (register, create, capacity, subscribe, unsubscribe) with shared fold and test fixtures first. Everything else reuses its tag taxonomy.
2. Unique name and rename can be built any time after create (independent boundary).
3. Schedule slots **before** transfer and waitlist if possible. Both reuse the subscription events and the overlap check, and adding `slotIds` to events later forces rework of already-written fixtures. If schedule is later in the plan, define the `slotIds` field in the event up front.
4. Transfer before waitlist (transfer is a trigger source for promotion), but the no-barging rule means waitlist-aware Subscribe must be designed before Transfer's final shape.
5. Decision-trace instrumentation must land in the command service **before** the event explorer's boundary view, and ideally before many commands exist.

---

## MVP Recommendation

Prioritize (in roadmap order):
1. Base DCB write slice: register student, create course, change capacity, subscribe, unsubscribe, with typed rejections and given-when-then tests, plus the narrow-boundary no-conflict tests. (Table stakes; proves the stack.)
2. Concurrent-subscription invariant proof (integration test with Testcontainers plus a first race demo). This is the Core Value, so show it as early as possible.
3. Read models (catalogue, detail, my subscriptions) via QueryBus, live updates, a plain but real UI with a role switcher.
4. Unique course name with release on rename.
5. Slot-grid schedule with overlap prevention.
6. Atomic transfer.
7. Waiting list with auto-enrolment (automation service) and `Withdraw`.
8. Decision trace, event explorer, boundary-of-command view, scenario seeding/reset, guided scenarios (the main differentiators). Start decision-trace capture earlier (step 1 or 2) even if the UI arrives here.
9. Docs: ADRs and Event Modeling should be written as the decisions happen. Aggregate-vs-DCB comparison, DCB concepts page, "how this was built" and the docs site are assembled continuously and finalised at the end.

Defer: dark mode and i18n, projection-lag dashboard, benchmark results page, boundary Venn diagram, course archive, everything on the deferred list in 1.7.

---

## Research Flags (needs deeper phase-specific research)

| Topic | Why | Confidence now |
|-------|-----|----------------|
| AF5 conflict detection and retry semantics for command handlers (exception types, built-in retry or not, retry policy) | Race demo, load test and waitlist contention all depend on it. My searches found nothing AF5-specific | LOW |
| Criteria that depend on a first-pass read (waitlist head, student's courses) in `@EventCriteriaBuilder` | Affects promotion and arbitrary-interval schedule | LOW |
| Multiple events in one append plus multi-entity injection in AF5 (`EventAppender`, `@InjectEntity` with composite ids, three-entity transfer) | Transfer and promotion are built on it | MEDIUM (university-demo uses a composite `SubscriptionId` and `either`) |
| How to capture decision criteria and consistency marker for the decision trace | Axon doesn't expose it declaratively | LOW |
| Reading the event store with tags from the query service (Axon Server DCB read/stream API) for the explorer | Explorer feasibility | LOW-MEDIUM |
| Resetting an Axon Server DCB context for scenario reset | Seeding/reset feasibility | LOW |
| Tag value constraints (length, charset) in Axon Server for normalised names | Unique-name design | LOW |
| AF5 subscription queries over SSE/WebSocket and the testing story (`AxonTestFixture` for read slices) | Live updates | MEDIUM |

## Sources

- dcb.events Course subscriptions example: https://dcb.events/examples/course-subscriptions/ (HIGH, fetched)
- dcb.events Examples index: https://dcb.events/examples/ (HIGH, fetched)
- dcb.events Unique username example: https://dcb.events/examples/unique-username/ (HIGH, fetched)
- dcb.events Bike rental example: https://dcb.events/examples/bike-rental/ (HIGH, fetched)
- dcb.events "Aggregates" topic: https://dcb.events/topics/aggregates/ (HIGH, fetched)
- AxonIQ university-demo (archived, moved into AxonFramework repo): https://github.com/AxonIQ/university-demo (HIGH, fetched)
- AF5 getting-started, University Domain: https://docs.axoniq.io/axon-framework-5-getting-started/5.4/sample-domain-event-modeling/ (HIGH, fetched)
- AF5 getting-started, Subscribe Student: https://docs.axoniq.io/axon-framework-5-getting-started/5.4/implement-command-subscribe-student/ (HIGH, fetched)
- AF5 getting-started chapter list: https://docs.axoniq.io/axon-framework-5-getting-started/5.4/ (HIGH, fetched)
- AF5 Event Store Internals (consistency marker, append condition, tags/criteria): https://docs.axoniq.io/axon-framework-reference/5.3/events/event-store-internals/ (HIGH, fetched; does not cover conflict exceptions or retry)
- Aggregates and DCB (sliceworkz): https://sliceworkz.github.io/posts/aggregates-and-dcb (MEDIUM, third-party)
- eventsourcing (Python) DCB course subscriptions: https://eventsourcing.readthedocs.io/en/stable/topics/examples/dcb-enrolment-introduction.html (MEDIUM)
- AxonIQ DCB concept page: https://www.axoniq.io/concepts/dynamic-consistency-boundary (MEDIUM, snippet only)
- Reference-project quality norms (README/ADR/C4/one-command run): general practice plus Microsoft CQRS Journey and the AF5 sample's own repo layout (LOW-MEDIUM; no authoritative survey found)
