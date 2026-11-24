---
phase: "02"
slug: "runtime-infrastructure-dcb-spikes"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
block_on: high
register_authored_at_plan_time: true
created: "2026-11-23"
---

# Phase 02 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

The register merges the `<threat_model>` blocks of plans 02-01 to 02-23. `T-02-SC` (npm/pip/cargo installs) appears in
every plan and is listed once. No `## Threat Flags` entry in any SUMMARY raised a new threat. Verification is at ASVS
level 1 (grep depth against the implementation, tests and configuration).

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| Maven Central / Gradle plugin portal → build | Libraries, the Kover agent, Konsist and the CycloneDX plugin resolve through `:platform` and the catalog | Build-time code (integrity) |
| npm registry → developer machine | MCP server packages are installed and executed locally | Executable code (integrity) |
| MCP server → Claude Code session | Servers expose documentation fetch and browser control to the agent | Tool calls, browser state |
| Third-party marketplace / skills → agent context | Plugin and project skills become agent instructions | Instructions (integrity) |
| Upstream spec repository → test data | TypeID vectors copied into the repository | Test vectors |
| Untrusted text / JSON → `TypeId`, `RejectionCode`, Jackson | IDs, rejection codes and polymorphic results are parsed and deserialised | Identifiers, payloads (ASVS V5) |
| Command handling → event store | Capture decorator and conflict retry sit before every conditional append | Events, decision records |
| Container registry → test JVM / compose | Axon Server, PostgreSQL and Ryuk images are pulled and run | Container images (integrity) |
| Host network → compose services | Published Axon Server and PostgreSQL ports | Unauthenticated dev services |
| Developer `.env` / `~/.gsd` → containers and doctor output | Optional Platform token, dev passwords, GSD defaults | Credentials (confidential) |
| PR code → CI runner | `integration-test`, `compose-smoke`, `doctor` and advisory jobs run PR code | GitHub token, runner |
| Repository text → workflow commands / governance verdicts | Records, ADR front matter and markers drive checks and runner output | Text (integrity) |
| Repository → public upstream trackers and docs | Drafts, records and ADRs leave the project | Text (confidentiality) |

---

## Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-02-SC | Tampering | npm/pip/cargo installs | high | mitigate | Only the two MCP npm packages (02-02), behind the blocking legitimacy checkpoint, exact versions and `mise.lock` digests; every other plan installs none | closed |
| T-02-01 | Tampering | Dependency resolution of new libraries | medium | mitigate | Catalog-only versions through `:platform`; `checkBuildScripts` rejects literals (`BuildScriptLintCheckFunctionalTest`) | closed |
| T-02-02 | Tampering | TypeID vector files | low | mitigate | `REUSE.toml` records upstream commit `26a84e8…`; vectors in `backend/contracts/kernel/src/test/resources/typeid/` | closed |
| T-02-03 | Tampering | `TypeId.decode` / `parse` input validation | medium | mitigate | Fail-closed codec in `TypeId.kt:83`, `TypedId.kt:54`; invalid vectors tested | closed |
| T-02-04 | Tampering | npm packages of the MCP servers | high | mitigate | Blocking checkpoint (02-02 SUMMARY); `mise.toml:44-45` exact versions; `mise.lock:307-316` digests | closed |
| T-02-05 | Information Disclosure | `.mcp.json` secrets | high | mitigate | `.mcp.json` has no `env` block and no `${...}`; gitleaks on every commit | closed |
| T-02-06 | Elevation of Privilege | Playwright MCP browser automation | medium | mitigate | `--isolated --output-dir .playwright-mcp` in `.mcp.json`; `.gitignore:21` | closed |
| T-02-07 | Tampering | Unattended MCP updates | medium | mitigate | `renovate.json5:252-256` `automerge: false` for both packages | closed |
| T-02-08 | Repudiation | Silently skipped property tests | medium | mitigate | `runBlocking<Unit>` block bodies (`TypeIdPropertyTest`, `TypedIdTest`, `JsonRoundTripTest`) | closed |
| T-02-09 | Tampering | Test-suite convention regressions | medium | mitigate | TestKit `GradleFixture` runs with configuration cache and isolated projects in `build-logic` `check` | closed |
| T-02-10 | Tampering | ID deserialisation | medium | mitigate | Deserialisers call `parse`; `JsonRoundTripTest:58-60` asserts "Wrong TypeID prefix" | closed |
| T-02-11 | Elevation of Privilege | Polymorphic deserialisation | high | mitigate | No default typing in `backend/`; `@JsonTypeInfo` only on sealed results; `JsonRoundTripTest:124-125` rejects unknown `@type` | closed |
| T-02-12 | Tampering | Marketplace content drift | high | mitigate | `.claude/settings.json` marketplace `ref`s are full commit SHAs; no auto-update keys | closed |
| T-02-13 | Elevation of Privilege | Plugins shipping hooks, scripts or MCP servers | high | mitigate | Markdown-only review criteria and per-plugin author approval (02-05 SUMMARY) | closed |
| T-02-14 | Tampering | Incorrect third-party guidance | medium | mitigate | Documented precedence: spike-findings skill and ADRs win; quality gates still apply | closed |
| T-02-15 | Tampering | Floating container images | high | mitigate | Tag plus `@sha256` in catalog; Ryuk pinned via `TESTCONTAINERS_RYUK_CONTAINER_IMAGE` (`courses.test-suites.gradle.kts`); no `SharedAxonServerContainer` in sources | closed |
| T-02-16 | Elevation of Privilege | Docker socket access by Testcontainers and Ryuk | medium | accept | See Accepted Risks AR-02-01 | closed |
| T-02-17 | Information Disclosure | Axon update checker phoning home | low | mitigate | `AXONIQ_UPDATE_CHECKER_DISABLED=true` in `courses.test-suites.gradle.kts` and `compose/infra.yaml:16` | closed |
| T-02-18 | Elevation of Privilege | `integration-test` CI job token | medium | mitigate | `contents: read`, `persist-credentials: false`, all `uses:` SHA-pinned; actionlint and zizmor | closed |
| T-02-19 | Tampering | Capture decorator altering append outcomes | high | mitigate | `DecisionCaptureEngine` swallows recorder failures and re-raises the original; `DecisionCaptureIntegrationTest` asserts capacity holds | closed |
| T-02-20 | Denial of Service | Retry storms and leaked threads | medium | mitigate | `ConflictRetry.kt:28,53` retries only `AppendEventsTransactionRejectedException`, max 10 attempts | closed |
| T-02-21 | Repudiation | Missing decision records | medium | mitigate | `CommandServiceConfigurationEnhancer` requires the `DecisionRecorder` component; one record per attempt tested | closed |
| T-02-22 | Tampering | Architecture rules passing vacuously | medium | mitigate | Konsist `assertTrue(strict = true)`, scope sanity test, declared `scannedSources` inputs | closed |
| T-02-23 | Tampering | Image literals bypassing the catalog | medium | mitigate | `ContainerImageArchitectureTest` | closed |
| T-02-24 | Tampering | Image references in compose | high | mitigate | `compose/infra.yaml` uses only `${*_IMAGE:?}`; generated `compose/versions.env` digest-pinned; `checkImageReferences`, `checkComposeVersions` | closed |
| T-02-25 | Information Disclosure | Unauthenticated dev Axon Server / PostgreSQL | medium | mitigate | Ports bound to `127.0.0.1` (`compose/infra.yaml:30-31,77`) | closed |
| T-02-26 | Information Disclosure | Secrets leaking into containers or repo | high | mitigate | No `env_file: .env` (only `compose/versions.env`); token is bare pass-through (`infra.yaml:22`); gitleaks | closed |
| T-02-27 | Spoofing | Development database passwords | low | accept | See Accepted Risks AR-02-02 | closed |
| T-02-28 | Information Disclosure | Platform token in tracked files or logs | high | mitigate | `.env.example:18` commented only; gitleaks | closed |
| T-02-29 | Denial of Service | Empty token zombies Axon Server | medium | mitigate | Commented example, healthcheck; `test-doctor.sh:428-435` asserts `doctor` fails on empty token | closed |
| T-02-30 | Elevation of Privilege | `compose-smoke` job token | medium | mitigate | `contents: read`, `persist-credentials: false`, SHA-pinned actions, no new secrets | closed |
| T-02-31 | Tampering | Image updates without digest / partial updates | medium | mitigate | One Renovate regex manager over catalog and `compose/versions.env` (`renovate.json5:80-84`); consistency checks | closed |
| T-02-32 | Tampering | Forbidden or unknown licences | high | mitigate | Fail-closed `checkDependencyLicences` per module, named in CI (`ci.yml:73`); `DependencyLicenceCheckFunctionalTest` | closed |
| T-02-33 | Repudiation | Licence exceptions without justification | medium | mitigate | Reasons in governed `governance/dependency-licences.json`; `checkNotice` mirrors `NOTICE` | closed |
| T-02-34 | Information Disclosure | Token / defaults values in doctor output | high | mitigate | Presence-only checks; `test-doctor.sh:135,396,412,426` sentinels never appears in output | closed |
| T-02-35 | Tampering | Modified or extra GSD hooks and files | high | mitigate | SHA-256 manifest verification, unexpected-file detection, pinned `governance/gsd-version` (1.22.0) | closed |
| T-02-36 | Tampering | Diagnostic tool mutating the environment | medium | mitigate | `doctor.sh` is read-only by contract; `test-doctor.sh:205,666-673` tree-hash idempotency comparison | closed |
| T-02-37 | Elevation of Privilege | `doctor` CI job token | medium | mitigate | `contents: read`, `persist-credentials: false`, SHA-pinned actions | closed |
| T-02-38 | Tampering | Front-matter parsing | medium | mitigate | Fail-closed parser; TestKit malformed-input cases | closed |
| T-02-39 | Repudiation | Stale or orphaned workarounds | medium | mitigate | Two-way consistency rules in `check`, governed path | closed |
| T-02-40 | Information Disclosure | Private data in Axon records or drafts | medium | mitigate | Static grep for user paths and e-mails returns nothing in `docs/upstream` | closed |
| T-02-41 | Tampering | Workarounds without record or vice versa | medium | mitigate | `checkUpstreamRecords` both directions (`UpstreamRecordCheckFunctionalTest`) | closed |
| T-02-42 | Tampering | Workflow-command injection via record text | high | mitigate | `%`, CR, LF escaped; `scripts/upstream/test-changed-dependencies.sh:336-346` injection payload test | closed |
| T-02-43 | Elevation of Privilege | Advisory job token | medium | mitigate | `contents: read`, `persist-credentials: false`, base SHA via `env`, no secrets; zizmor | closed |
| T-02-44 | Tampering | ADR normalisation altering decisions | medium | mitigate | Structure-only rewrite, status/date comparison and word-diff in 02-19 SUMMARY | closed |
| T-02-45 | Tampering | Malformed ADR metadata | low | mitigate | Fail-closed parser and `checkAdrs` in `check` (`AdrCheckFunctionalTest`) | closed |
| T-02-46 | Information Disclosure | Credentials or private data in ADRs | medium | mitigate | Variable names and formats only; gitleaks; grep for paths/e-mails/tokens empty (02-23 SUMMARY) | closed |
| T-02-47 | Repudiation | Undocumented or misattributed decisions | low | mitigate | ADRs cite decision IDs and sources; `checkAdrs` | closed |
| T-02-48 | Tampering | Unvetted instructions injected into all agents | high | mitigate | `agent_skills` limited to the project skill and 02-05-approved plugins; pinned in governed `governance/gsd-config-policy.json` | closed |
| T-02-49 | Repudiation | Publication without consent | high | mitigate | Blocking decision checkpoint per draft; URLs listed in 02-22 SUMMARY | closed |
| T-02-50 | Information Disclosure | Private data in published issues | medium | mitigate | Screened drafts (02-16/02-17), approved text only | closed |
| T-02-51 | Tampering | `RejectionCode` construction | low | mitigate | `Rejection.kt:4,29` kebab-case regex with `require` | closed |
| T-02-52 | Elevation of Privilege | Governance boundary cases naming deleted paths | medium | mitigate | Cases repointed, not deleted; `test-check-pr.sh` 132 passed | closed |
| T-02-53 | Repudiation | Coverage report silently omitting a suite | medium | mitigate | No `disabledForTestTasks` anywhere; TestKit parses the XML report per suite | closed |
| T-02-54 | Tampering | Kover plugin and JVM agent | medium | mitigate | Catalog key `kover`; `build-logic/build.gradle.kts` governed | closed |
| T-02-55 | Repudiation | Stale cached integration-test results | medium | mitigate | Images as `@Input`; `ContainerImageInputsFunctionalTest` | closed |
| T-02-56 | Information Disclosure | Private data in host/tooling records | medium | mitigate | Placeholders in reproductions; private-data grep empty | closed |
| T-02-57 | Tampering | Marker edits to governed configuration | low | mitigate | Comment-only diffs verified by actionlint, zizmor, `test-doctor.sh`, `docker compose config`, build; label gate | closed |
| T-02-58 | Elevation of Privilege | Switching off the per-module licence gate | medium | mitigate | `courses.licences.gradle.kts` governed (`governed-paths.txt:159`) with ask rule (`.claude/settings.json:43`); CI names `checkDependencyLicences` | closed |
| T-02-59 | Elevation of Privilege | `courses.licences.gradle.kts` ungoverned window | low | accept | See Accepted Risks AR-02-03; window now closed by T-02-58 | closed |
| T-02-60 | Tampering | Silently relaxing detekt | medium | mitigate | `config/detekt/**` governed (`governed-paths.txt:182`) with one ask rule (`.claude/settings.json:57`) | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-02-01 | T-02-16 | Testcontainers and Ryuk need the Docker socket; development and CI only, no privileged Ryuk configuration, nothing runs in production | Plan 02-07 threat model (author-approved plan) | 2026-11-23 |
| AR-02-02 | T-02-27 | Development database passwords are documented as non-secret, overridable from `.env`, and bound to loopback; production deployment is out of scope | Plan 02-10 threat model (author-approved plan) | 2026-11-23 |
| AR-02-03 | T-02-59 | Ungoverned window for `courses.licences.gradle.kts` existed only on the phase branch between plans 02-13 and 02-20; the file is now governed (T-02-58), so the residual is historical | Plan 02-13 threat model (author-approved plan) | 2026-11-23 |

*Accepted risks do not resurface in future audit runs.*

Residuals named in `governance/governed-paths.txt` (a single module's own build script switching off its licence task,
its detekt task or its compiler options) remain accepted there and are not repeated here. The convention-plugin route is
closed: `build-logic/src/main/kotlin/courses.kotlin-library.gradle.kts`, which applies detekt and the licence plugin and
sets `allWarningsAsErrors` for every module, is governed since quick task 261124-hbh (author decision of 2026-11-24).

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-23 | 61 | 61 | 0 | /gsd-secure-phase orchestrator (ASVS L1 grep verification; auditor skipped per short-circuit rule) |

## Security Audit 2026-11-23

| Metric | Count |
|--------|-------|
| Threats found | 61 |
| Closed | 61 |
| Open | 0 |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-11-23
