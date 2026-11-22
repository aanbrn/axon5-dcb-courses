# Phase 2: Runtime Infrastructure & DCB Spikes - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents. Decisions are captured in
> CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-11-22
**Phase:** 02-runtime-infrastructure-dcb-spikes
**Areas discussed:** Contracts and kernel, TypeID and serialization, Spikes, Test suites, Compose and licence, doctor,
Upstream and licences, AI skills and MCP, Templates and ADRs, Paketo + pnpm spike

---

## Contracts and kernel

| Option | Description | Selected |
|--------|-------------|----------|
| By message kind | contracts kernel, commands, events, queries; CQRS boundaries visible in the dependency graph | ✓ |
| One module + kernel | packages inside one contracts module, boundaries only via Konsist | |
| By domain area | course, student, subscription; contradicts DCB decisions spanning entities | |

**User's choice:** By message kind.
**Notes:** The author asked whether to group backend modules under `backend/`. Claude first advised against it, then
preferred the author's refinement (contracts grouped, services flat as `*-service`), which removes the depth objection
and aligns directory, compose and image names. Final layout: `backend/contracts/{kernel,commands,events,queries}`,
`backend/{command-service,query-service,automation-service,bff}` (amends Phase 1 D-03).

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit namespace + version | standard AF5 `@Namespace`/`@Event`, short namespace, explicit version, Konsist check | ✓ |
| AF5 defaults | namespace = package, version 0.0.1 | |

**User's choice:** Explicit namespace + version.
**Notes:** The author asked whether the recommendation was invented or established. Claude explained that the
principle (decouple persisted type names from class names, version them) is established event-sourcing practice, that
AF5 supports it natively (`MessageType` = `QualifiedName` + version; namespace defaults to the package), and that only
the concrete value `courses` is Claude's suggestion.

| Option | Description | Selected |
|--------|-------------|----------|
| compileOnly annotations | Axon annotations as metadata, no runtime dependency, Konsist forbids other imports | ✓ |
| Pure Kotlin | names and tags registered in services | |
| Decide by spike | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Sealed per command + shared Rejection | exhaustive `when` in the BFF | ✓ |
| Generic Outcome<T> | fewer types, rejections not known per command | |
| Claude decides | | |

---

## TypeID and serialization

| Option | Description | Selected |
|--------|-------------|----------|
| UUID inside value class | native `uuid` columns, UUIDv7 ordering, canonical string on serialization | ✓ |
| Canonical string inside | trivial serialization, conversion for read models | |

| Option | Description | Selected |
|--------|-------------|----------|
| Own implementation + spec vectors | if no mature JVM library; ADR, IDEM-04 wording clarified | ✓ |
| Best available library | dependency on a low-activity project | |

| Option | Description | Selected |
|--------|-------------|----------|
| Jackson 3 + Kotlin module | AF5 converter, shared with BFF REST | ✓ |
| kotlinx.serialization | needs a custom Axon converter and compiler plugin | |

| Option | Description | Selected |
|--------|-------------|----------|
| Fixed IDs + Arb + generator with Clock | readable given-when-then, property tests, deterministic ordering | ✓ |
| Always random | | |

| Option | Description | Selected |
|--------|-------------|----------|
| parse + parseOrNull | stdlib-style | ✓ |
| Sealed parse result | | |

| Option | Description | Selected |
|--------|-------------|----------|
| const keys = TypeID prefix | usable in `@EventTag`, Konsist check | ✓ |
| Typed TagKey<T> | not usable in annotations | |
| Both levels | | |

**Notes:** The author asked for extra TypeID questions after the first round; test IDs, parse API and tag registry came
from that round.

---

## Spikes

| Option | Description | Selected |
|--------|-------------|----------|
| /gsd-spike before planning + track .claude/skills | outcomes in ADRs, proofs become tests | ✓ (later narrowed) |
| /gsd-spike without tracking the skill | | |
| Spikes as phase plans | | |

**User's choice:** /gsd-spike, with the findings skill tracked.
**Notes:** The author pointed out that GSD has its own spike mechanism, then asked whether Claude Code supports
`.agents/skills` and whether GSD could write there. Research: Claude Code documents only `.claude/skills/` (root,
nested, `--add-dir`) and plugins; GSD's wrap-up path is hard-coded and patching it breaks the AIDEV-07 manifest check.
In the AI tooling area the decision was narrowed to tracking only `.claude/skills/spike-findings-*/`, because AIDEV-02
keeps own skills in a plugin outside `.claude/`.

| Option | Description | Selected |
|--------|-------------|----------|
| A: central decorator with safeguards | full data, capture test, upstream record | ✓ |
| B: explicit in every slice | public API only, repetitive, possibly incomplete | |
| Decide after spike | | |

**Notes:** The author asked twice for background before choosing. Claude gave a primer on DCB command execution
(criteria, consistency marker, conditional append, retries), the decision record and the meaning of `@Internal`.

| Option | Description | Selected |
|--------|-------------|----------|
| Docs + short run + Platform | | |
| 12h+ soak | | |
| Docs only | | |

**User's choice:** Free text — the author has verified that unlicensed Axon Server in compose stops after about 12 hours
and works with a Platform token. Follow-up: restart behaviour unknown → verify by spike.

---

## Test suites

| Option | Description | Selected |
|--------|-------------|----------|
| Boundary at Axon Server | component = one service + real PostgreSQL, Axon in memory | ✓ |
| Boundary at containers | component without any containers | |

**Notes:** The author suggested H2 in PostgreSQL mode for component tests. Claude explained the gaps (`COLLATE "C"`,
`ON CONFLICT`, `jsonb`, migration dialect, false confidence) and the small cost of a singleton PostgreSQL container; the
author chose PostgreSQL in Testcontainers.

| Option | Description | Selected |
|--------|-------------|----------|
| Unique IDs per test | | ✓ |
| Purge between tests | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated Konsist module | | ✓ |
| Per module | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Owner testFixtures + container module | | ✓ |
| One test-support module | | |

| Option | Description | Selected |
|--------|-------------|----------|
| All suites in check | | ✓ |
| integrationTest explicit only | | |

**Notes:** The author asked for arguments for including integration tests in `check` and noted an apparent conflict
with a separate CI job. Claude gave the arguments (agents use `build` as the done criterion, the build cache, Gradle and
Maven conventions) and explained that `check` defines the full verification while CI splits it into parallel jobs. A
`quickCheck` aggregate was considered (the author asked whether `check` + `quickCheck` or `check` + `fullCheck` is more
conventional; Claude: a complete `check` is the expectation). Finally the author dropped `quickCheck`: code is written
and verified by agents.

| Option | Description | Selected |
|--------|-------------|----------|
| build + separate integration-test job | | ✓ |
| Single job | | |
| Job per suite | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Only libraries used in Phase 2 | | ✓ |
| All TEST-14 libraries now | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Parallelism by level | | ✓ |
| All sequential | | |
| Claude decides | | |

---

## Compose and licence

| Option | Description | Selected |
|--------|-------------|----------|
| Root compose.yaml + include | | ✓ |
| Single compose.yaml | | |

| Option | Description | Selected |
|--------|-------------|----------|
| One server, database + user per service | | ✓ |
| One database, schema per service | | |
| Container per service | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Optional env via .env | | ✓ |
| Compose profile platform | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Both compose and host clients work | | ✓ |
| Compose only | | |

---

## doctor

| Option | Description | Selected |
|--------|-------------|----------|
| Script + Gradle task | | ✓ |
| Shell only | | |
| Gradle only | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Diagnose + fix command | | ✓ |
| With --fix | | |

| Option | Description | Selected |
|--------|-------------|----------|
| In CI with --ci | | ✓ |
| Local only | | |

---

## Upstream and licences

| Option | Description | Selected |
|--------|-------------|----------|
| CycloneDX SBOM + own check | | ✓ |
| jk1 Gradle License Report | | |
| Trivy license scanner | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Permissive + weak copyleft + exceptions | | ✓ |
| Permissive only + exceptions | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Targeted CI check (summary + annotation) | | ✓ |
| PR comment | | |
| Static Renovate note | | |

---

## AI skills and MCP

| Option | Description | Selected |
|--------|-------------|----------|
| Narrow exception for spike-findings | | ✓ |
| Move findings into the plugin | | |
| All own skills in .claude/skills | | |

**Notes:** Claude flagged that the earlier spike decision conflicted with AIDEV-02 and corrected it.

| Option | Description | Selected |
|--------|-------------|----------|
| Survey all, install as needed | | ✓ |
| Install all now | | |

| Option | Description | Selected |
|--------|-------------|----------|
| agent_skills in Phase 2 after survey | | ✓ |
| Defer | | |

| Option | Description | Selected |
|--------|-------------|----------|
| MCP servers via mise | | ✓ |
| npx with exact version | | |

---

## Templates and ADRs

| Option | Description | Selected |
|--------|-------------|----------|
| One template: full MADR 4 | | ✓ |
| Full + minimal | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Normalise ADR-0001..0004 | | ✓ |
| Front matter only | | |

| Option | Description | Selected |
|--------|-------------|----------|
| All templates in docs/templates/ | | ✓ |
| Next to target directories | | |

| Option | Description | Selected |
|--------|-------------|----------|
| Gradle front-matter check | | ✓ |
| Template only | | |

---

## Paketo + pnpm spike

| Option | Description | Selected |
|--------|-------------|----------|
| Prebuilt dist/ from Gradle | buildpack only serves static files via nginx | ✓ |
| Buildpack builds from source | needs pnpm support in Paketo, double build | |

| Option | Description | Selected |
|--------|-------------|----------|
| Minimal build in spike | | ✓ |
| Documentation only | | |

---

## Claude's Discretion

- How the DCB context is created in compose, Gradle project path names, module directory names, the token variable
  name, host ports, the PostgreSQL init script, the `courses.test-suites` internals, and candidate upstream records.

## Deferred Ideas

- Static analysis in a fast-loop aggregate; JUnit in-fork parallelism; `doctor --fix`; frontend, Playwright and Paketo
  skills in their phases.
