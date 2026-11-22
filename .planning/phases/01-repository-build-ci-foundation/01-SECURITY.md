---
phase: "1"
slug: "repository-build-ci-foundation"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
block_on: high
register_authored_at_plan_time: true
created: "2026-11-22"
---

# Phase 1 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.
> Built from the `<threat_model>` blocks of plans 01-01 to 01-21. Mitigations were verified against the
> implementation by `gsd-security-auditor` (ASVS level 1, verify-only mode, no new-threat scan).

---

## Trust Boundaries

Collected from every plan's threat model, in plan order; a boundary named by several plans appears once.

| Boundary | Description |
|----------|-------------|
| Network → build | Gradle distribution, plugins, libraries and BOMs are downloaded from services.gradle.org, the Gradle Plugin Portal and Maven Central |
| Build-logic → every module | Convention plugins execute in every build of every contributor and in CI |
| Working tree → git history | Ignore rules decide what reaches the public repository |
| Registries → developer machine and CI | Tool binaries and packages come from GitHub releases (aqua/github backends), npm and PyPI |
| npm registry → build | Spotless downloads Prettier from npm at build time |
| Developer or CI `PATH` → build | The build executes the taplo and shfmt binaries it finds on `PATH` |
| GSD runtime / agent → `.planning/config.json` | GSD workflows and the agent can rewrite project settings |
| Contributor → build scripts | Any PR can add dependencies or build logic |
| Working tree → commit | Staged content leaves the contributor's control once pushed |
| Hook → local machine | The hook runs repository scripts and Gradle on every commit |
| PR author → CI | PR titles, branch names, labels and file lists are attacker-controlled input to the workflow |
| PR → `main` | Governance changes must be consciously approved by the maintainer |
| Workflow → repository issues API | The sync job holds `issues: write` |
| Upstream registries → repository | Renovate proposes new third-party versions; automerge can land them without a human |
| PR code → CI runner | Untrusted PR code runs in `pull_request` jobs |
| Third-party actions → CI | Action code runs with the job token |
| CI cache → later runs | Cached Gradle state is restored into future builds |
| Local admin `gh` session → GitHub settings | The apply script can change branch protection and security features |
| Agent → governance files | Claude Code can edit any file unless asked to confirm |
| Status reporters → required checks | Any GitHub App could post a status with the same context name |
| Author's admin session → live GitHub repository | Admin-level writes to branch protection, security features, labels and secrets |
| Agent → live GitHub repository | Read-only verification calls |
| Author → third-party services | Renovate app installation and REUSE registration involve the author's identity |
| GitHub API → workflow step | The files API answer can be missing, partial, an error body or truncated; the step must treat anything but a complete array as unusable |
| Governed-path list → two consumers | One reviewed file drives the CI label gate (shell) and the agent ask rules (Gradle check); a divergence turns a reviewed entry into an unenforced one |
| Agent → repository | Claude Code edits to governance paths need the author's confirmation |
| Files API data → gate loop | Pull request file names and patches are untrusted data; any value the record stream cannot carry must stop the gate, not shorten the list it evaluates |
| Repository config → CI tool installation | mise trusts and applies every config file it reads from the checkout root during `mise install` |
| PR content → governance gate | The gate's workflow, scripts and governed list are read from the pull request's own merge ref, so the pull request can change the check it is judged by |
| Maintainer → `main` | The maintainer's review of the squash diff is the control that covers that residual |
| Developer or CI environment → build | `PATH` decides which `node`, `taplo` and `shfmt` binaries the build runs; the build trusts the environment it is given, but must not substitute a different one |
| Build → formatter subprocesses | The verified binary must be the one that formats, or the integrity of the formatted output rests on an unverified runtime |

---

## Threat Register

The Mitigation column is the plan's mitigation text; the plans that register the threat follow in brackets.
When plans differ, the latest plan's row is shown. The auditor's per-threat evidence (file and line) is
summarised in the audit trail below.

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-01-01 | Tampering | Gradle wrapper distribution | high | mitigate | `distributionSha256Sum` pinned in `gradle-wrapper.properties` and `validateDistributionUrl=true` (Task 1); setup-gradle validates the wrapper jar in CI (plan 01-12) (01-01) | closed |
| T-01-02 | Tampering | Plugin, library and BOM resolution | medium | mitigate | Versions only from the catalog, BOMs imported through catalog aliases (Task 2); repositories centralised in `courses.settings` with `FAIL_ON_PROJECT_REPOS` (Task 1); `checkBuildScripts` rejects literals (plan 01-07); Renovate cooldown (plan 01-11) (01-01) | closed |
| T-01-03 | Elevation of Privilege | Quality gates weakened to pass | medium | mitigate | No detekt baseline (acceptance `find ... '*baseline*.xml'` = 0), TestKit negative tests prove each gate fails on a violation (Task 2) (01-02) | closed |
| T-01-04 | Tampering | Tool binaries downloaded by mise | high | mitigate | `mise.lock` commits per-platform checksums and attestation provenance; CI runs mise with the lock (`--locked`) (Task 2, plan 01-12) (01-03) | closed |
| T-01-05 | Spoofing | actionlint registry alias pointing to an unvetted fork | medium | mitigate | Pin `aqua:rhysd/actionlint` explicitly (Task 2 acceptance) (01-03) | closed |
| T-01-06 | Tampering | Typosquatted or malicious npm/PyPI package | high | mitigate | Exact package names only; blocking human legitimacy checkpoint (Task 1); 7-day release cooldown for unpinned-by-research versions (01-03) | closed |
| T-01-07 | Tampering | Prettier fetched by Spotless | medium | mitigate | Version pinned in the catalog (Renovate-annotated, 7-day cooldown), legitimacy confirmed in plan 01-03, Node major verified by `verifyNodeVersion` (Task 1); no Prettier plugins, so no unpinned transitive npm dependencies (01-04) | closed |
| T-01-08 | Information Disclosure | `.env` and local tool state committed by mistake | high | mitigate | `.gitignore` covers `.env`, `.env.*` (except `.env.example`), IDE and tool state (Task 1); gitleaks hook and CI (plans 01-08, 01-12) (01-01) | closed |
| T-01-09 | Repudiation | Unclear licensing of files | low | mitigate | `REUSE.toml` + `LICENSES/` + `reuse lint` (Task 1), enforced in the hook and CI (plans 01-08, 01-12) (01-06) | closed |
| T-01-10 | Tampering | `.planning/config.json` changed silently (foreign model overrides, branching or tagging changes) | high | mitigate | `checkGsdConfigPolicy` whole-file comparison in `check` (Task 1); policy file is a governed path needing the `governance-approved` label (plan 01-09) (01-07) | closed |
| T-01-11 | Tampering | Unreviewed dependency version introduced as a literal outside the catalog | medium | mitigate | `checkBuildScripts` `version-literal` rule (Task 2); Renovate only tracks catalog sources (plan 01-11) (01-07) | closed |
| T-01-12 | Denial of Service | False-positive lint blocks contributors | low | accept | Rules are line-based with comment skipping and TestKit coverage; a false positive is fixed in the rule with a test, never by a suppression list (01-07) | closed |
| T-01-13 | Information Disclosure | Secret staged for commit | high | mitigate | gitleaks `--staged --redact` in the hook (Task 1); the hook is bypassable, so CI scans the full history (plan 01-12) and GitHub push protection is enabled (plan 01-14) (01-08) | closed |
| T-01-14 | Information Disclosure | Synthetic test token leaking into history | medium | mitigate | Token generated at run time into a scratch file that is deleted; never written into plans, SUMMARY or committed files (Task 1 step 3b-c) (01-08) | closed |
| T-01-15 | Elevation of Privilege | Hook executes repository code (Gradle, scripts) on commit | low | accept | Only code already in the contributor's own clone runs; the hook is opt-in via `lefthook install` (01-08) | closed |
| T-01-16 | Information Disclosure | Build Scan publishing build data | medium | mitigate | Scans only with an explicit `--scan`; never in CI; documented in `docs/performance.md` (Task 2) (01-08) | closed |
| T-01-17 | Elevation of Privilege | Privileged PR trigger running PR code | high | mitigate | Only the `pull_request` event with a read-only token and no secrets; acceptance gate on the trigger (Task 1) (01-09) | closed |
| T-01-18 | Tampering | Script injection through PR title, branch or label values | high | mitigate | Every PR value passed via `env:`, no `${{ }}` in `run:`, JSON read only with `jq`, no `eval` (Tasks 1-2); zizmor template-injection audit (01-09) | closed |
| T-01-19 | Spoofing | Someone imitating the Renovate bot to use the exemption | medium | mitigate | Exact author `renovate[bot]` (the `[bot]` suffix is reserved for GitHub Apps) AND a version-only diff for every governed file (Task 2) (01-09) | closed |
| T-01-20 | Tampering | A PR edits the governance workflow or script so its own check passes | medium | mitigate | `.github/**` and `scripts/governance/**` are governed, so such a PR needs the label; the maintainer reviews the squash-merge diff; the agent is stopped by ask rules (plan 01-13). Residual risk (a malicious PR neutralising the check that runs from its own tree) is accepted at ASVS L1 because the only robust alternative runs PR-adjacent logic in a privileged context (01-09) | closed |
| T-01-21 | Elevation of Privilege | Label sync token | medium | mitigate | `issues: write` only in the `sync` job, which runs only on `main` pushes and manual dispatch; PR runs are dry-run with `issues: read`; action pinned by SHA (Task 1) (01-10) | closed |
| T-01-22 | Tampering | Removal of labels still in use | low | accept | Exact sync is intended for labels-as-code; the first sync's deletion of GitHub's default labels is disclosed in the plan 01-14 approval summary (01-10) | closed |
| T-01-23 | Tampering | Compromised or hijacked fresh release | high | mitigate | `minimumReleaseAge: "7 days"` for all managers, digest pinning for actions, `internalChecksFilter: "strict"` (Task 1) (01-11) | closed |
| T-01-24 | Elevation of Privilege | Automerge landing risky updates | high | mitigate | Automerge only for action digests and dev-tool patches (exactly 3 rules, gate in Task 2), only after required checks pass; governed files additionally need a version-only diff (plan 01-09) (01-11) | closed |
| T-01-25 | Information Disclosure | GitHub token used for a local lookup | medium | mitigate | Token only as an environment variable for one command, never written to disk or logs (Task 2 step 5) (01-11) | closed |
| T-01-26 | Elevation of Privilege | Over-privileged `GITHUB_TOKEN` | high | mitigate | `permissions: {}` per workflow, minimal job grants, `persist-credentials: false` (all tasks); zizmor excessive-permissions audit (01-12) | closed |
| T-01-27 | Tampering | Moved tag or impostor commit in a third-party action | high | mitigate | Full-SHA pins (gate in Task 3), zizmor online audits in CI with the job token, Renovate digest updates with a 7-day cooldown (plan 01-11) (01-12) | closed |
| T-01-28 | Tampering | Template injection through `${{ }}` in `run:` | high | mitigate | Values passed through `env:` (aggregator, mise version step), actionlint and zizmor template-injection checks (01-12) | closed |
| T-01-29 | Tampering | Gradle cache poisoning from a PR | medium | mitigate | `cache-read-only` except on `main`; optional `GRADLE_ENCRYPTION_KEY` for configuration-cache entries (01-12) | closed |
| T-01-30 | Information Disclosure | Secrets printed in CI logs | medium | mitigate | `gitleaks git --redact`; no secret values echoed; secret only referenced in `with:` (01-12) | closed |
| T-01-31 | Elevation of Privilege | Accidental settings change by the script | high | mitigate | Default mode `--dry-run`, writes only with `--apply`, tests prove no non-GET calls in dry-run and check modes (Task 1); application only after the blocking approval in plan 01-14 (01-13) | closed |
| T-01-32 | Spoofing | Another app posting a fake `ci-success` status | medium | mitigate | Required contexts bound to the GitHub Actions app via `integration_id` (Task 1) (01-13) | closed |
| T-01-33 | Information Disclosure | Drift-check token exposure | medium | mitigate | Read-only fine-grained token stored as a secret, referenced only in `env:`, never printed; workflow cannot run `--apply` (Task 2 gate) (01-13) | closed |
| T-01-34 | Tampering | Agent silently editing governance files or running `config-set` | high | mitigate | `permissions.ask` rules for every governed path and `config-set` (Task 3), kept in sync by `checkClaudeAskRules`; ask rules are not a security boundary, so CI (`checkGsdConfigPolicy`, `governance.yml`) remains the enforcement (01-13) | closed |
| T-01-35 | Elevation of Privilege | Settings applied without consent or beyond scope | high | mitigate | Blocking-human decision with the exact dry-run delta and per-item commands, including the `SETTINGS_DRIFT_SECTIONS` scope record for a partial approval (Task 1); the author runs exactly the approved commands (Task 2, D-15); the executor only verifies with `--check`, `gh variable get` and list calls (Task 3) (01-14) | closed |
| T-01-36 | Information Disclosure | Secret values in transcript or files | high | mitigate | The encryption key is piped from the generator into `gh secret set` by the author; the PAT is typed by the author at the gh prompt (Task 2); verification lists names only (Task 3) (01-14) | closed |
| T-01-37 | Denial of Service | Ruleset blocks all merges if a required context never reports | medium | mitigate | `apply-without-ruleset` option, admin bypass through PRs, `--check` verification; contexts bound to the GitHub Actions app id read from the API (01-14) | closed |
| T-01-38 | Information Disclosure | Author's personal data sent to third parties by the agent | medium | mitigate | REUSE registration and app installation are author-only steps (Task 2) (01-14) | closed |
| T-01-39 | Tampering | taplo and shfmt binaries executed by the build | medium | mitigate | Both pinned in `mise.toml` with per-platform checksums in `mise.lock` (plan 01-03) and installed by mise locally and in CI (plan 01-12); the resolved tool version is an input of the Spotless tasks (Tasks 1-2), so a different binary on `PATH` invalidates cached results instead of hiding behind them (01-05) | closed |
| T-01-40 | Tampering | `SETTINGS_DRIFT_SECTIONS` narrowing the drift check beyond what was deferred | low | mitigate | Writing Actions variables needs admin rights on the repository (the same trust level that could delete the token); the workflow names the checked sections in a notice on every run; unknown names exit 2; plan 01-14 Task 3 verifies the recorded scope equals the applied sections (01-13) | closed |
| T-01-41 | Tampering | `List changed files` step / `list-pr-files.sh` | high | mitigate | `gh` exit status checked directly (no pipeline), empty output rejected, pages must combine into one JSON array, destination written only by atomic rename after validation; fake-gh tests for empty, partial, error and non-array output (Tasks 1-2) (01-15) | closed |
| T-01-42 | Tampering | `check-pr.sh` on a list at the files API limit | medium | mitigate | 3000 or more entries require the label and bypass the renovate[bot] exemption; boundary cases 2999, 3000 and 3001 (Task 3) (01-15) | closed |
| T-01-43 | Tampering | `PR_BASE_REPO` and `PR_NUMBER` in the API path | low | mitigate | Values arrive through `env:` only and must match strict patterns before any `gh` call; tests prove `gh` is never invoked for malformed values (Task 2) (01-15) | closed |
| T-01-44 | Denial of Service | A transient API failure blocks a pull request | low | accept | Failing closed is the intended behaviour; a job re-run or any new push or label event re-evaluates (01-15) | closed |
| T-01-45 | Tampering | Governed-paths parsing in `check-pr.sh` versus `AskRulesCheck` | medium | mitigate | One grammar implemented in `load_globs` and `GovernedPaths.parse`, with the same case table tested on both sides; entries outside printable ASCII fail closed in both instead of matching nothing (Tasks 1-2) (01-16) | closed |
| T-01-46 | Denial of Service | A malformed governed-paths line fails every pull request's governance check | low | accept | Failing closed is intended; the message names the file and line, and the fix is a one-line governed edit (01-16) | closed |
| T-01-47 | Elevation of Privilege | Governance checks neutered through their code, wiring, root build configuration, plugin build or launcher | high | mitigate | 19 entries added to `governance/governed-paths.txt` with matching ask rules; `checkClaudeAskRules` keeps them in sync; boundary tests assert each route needs the label (Tasks 1-2) (01-17) | closed |
| T-01-48 | Tampering | Build code outside the list reaching governance tasks by other means (reflection, daemon-wide hooks) | medium | accept | Isolated projects (switch in the now-governed `gradle.properties`) and `checkBuildScripts`' cross-project rules block the ordinary APIs; the residual is written into the file header and covered by maintainer review, as for T-01-20 (01-17) | closed |
| T-01-49 | Denial of Service | Renovate Gradle wrapper pull requests and module-adding phase pull requests now need the label | low | accept | Wrapper updates are manual merges under D-12 already; phase pull requests are the maintainer's own; D-16 still exempts version-only diffs such as `gradle-wrapper.properties` (01-17) | closed |
| T-01-50 | Tampering | `check_governed_files` reading the per-file records | high | mitigate | Records go to a mktemp file; jq's exit status is checked before any entry is evaluated; the loop must see exactly `jq length` entries; both failures return before the label or the exemption is consulted; cases R1 to R9 prove each guard on its own (Tasks 1-2) (01-18) | closed |
| T-01-51 | Denial of Service | A pull request whose files API data contains a NUL can never pass the gate | low | accept | Failing closed is intended. GitHub sends no patch for files git classifies as binary, so a NUL in a patch is rare. The fix is to change the offending file or use the ruleset's admin bypass, which is declared with mode `pull_request` (01-18) | closed |
| T-01-52 | Elevation of Privilege | Renovate policy replaced by a config file under another name (`renovate.json` or `renovate.jsonc` win over `renovate.json5`; the others apply when it is absent), for example to automerge runtime updates | high | mitigate | Every name in Renovate's documented search order is governed with an ask rule; boundary cases for each name and for adding `renovate.json` (Tasks 1-2) (01-19) | closed |
| T-01-53 | Tampering | CI tool set changed through an alternate mise config, an rc file, `.tool-versions` or a sidecar lockfile (version override, unlocked tool, `[env]`/PATH injection that later steps inherit, redirected discovery) | high | mitigate | All 15 mise names CI honours from the repository root are governed with ask rules; reasoning in the file; boundary cases for each (Tasks 1-2) (01-19) | closed |
| T-01-54 | Denial of Service | Renovate bumps of `npm:renovate` or `pipx:reuse` that add a sidecar directory now need the label; a root `package.json` (if Phase 5 adds one) becomes governed | low | accept | Author-accepted trade-off; the exemption is not extended in this run and the follow-up is recorded in the SUMMARY; version-only bumps of a modified root `package.json` still take the exemption (01-19) | closed |
| T-01-55 | Tampering | mise environment files, subdirectory configs and alternate lockfiles stay outside the list | low | accept | Only a governed file can select an environment; a root-level mise run never reads subdirectory configs; a lockfile only matters through its governed config file. Reasoning recorded in the file header; ungoverned boundary cases pin each exclusion (01-19) | closed |
| T-01-56 | Elevation of Privilege | `governance.yml` and the gate it runs, evaluated from the pull request's tree (WR-06) | medium | accept | Recorded in ADR-0003. For a one-maintainer repository, accepted on these grounds: the maintainer merges every pull request through the ruleset, every gate file is governed, and ask rules guard the agent. The ADR names the revisit trigger (a second maintainer or external contributors). Same residual as T-01-20 (01-20) | closed |
| T-01-57 | Information Disclosure | ADR-0003 describes publicly how the gate can be rewritten by a pull request | low | accept | The repository and its workflows are public, and the property is inherent to every `pull_request` workflow; writing it down adds no capability and tells reviewers what to read (01-20) | closed |
| T-01-58 | Tampering | `ExecutableOnPath` `PATH` walk | medium | mitigate | Only absolute entries are searched. Empty and relative entries are skipped (P-3): inside the daemon, a relative entry's file checks follow the working directory each build was started from, while its absolute path is reported against the daemon's start directory. Only regular files the build can execute are accepted, and the first match wins as in a shell. Test (d) build B proves the skip rules, including a relative entry that holds an executable `node`, and the Task 2 mutation run proves that the relative case is not vacuous (01-21) | closed |
| T-01-59 | Tampering | `verifyNodeVersion` versus Spotless Prettier | medium | mitigate | One provider feeds both, and Spotless gets it as `nodeExecutable`. Test (b) proves that Prettier's server runs the binary the guard checked, also with no npm beside it (01-21) | closed |
| T-01-60 | Spoofing | `node` in a warm daemon | medium | mitigate | Programs are launched by an absolute path resolved from the current build's environment, never through the JDK's cached `PATH` lookup. Test (a) and the real-worktree reproduction prove it (01-21) | closed |
| T-01-61 | Information Disclosure | Resolved absolute paths in build output and the configuration-cache fingerprint | low | accept | The paths are local install locations (mise or setup-node), already printed by `verifyTaplo` and `verifyShfmt` outputs and Gradle logs; they carry no secrets (01-21) | closed |
| T-01-SC | Tampering | npm/pip/cargo installs | high | mitigate | No package installs. Spotless installs the catalog-pinned Prettier exactly as before; only the node that runs it changes (01-01, 01-02, 01-03, 01-04, 01-05, 01-06, 01-07, 01-08, 01-09, 01-10, 01-11, 01-12, 01-13, 01-14, 01-15, 01-16, 01-17, 01-18, 01-19, 01-20, 01-21) | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

### Controls declared in the repository but not yet live on GitHub

These threats are closed because the declared artifact exists and its deferral is documented. Their live effect
starts only when the author applies the ruleset and the workflows first run on GitHub.

| Threat | Pending part | Where the deferral is recorded |
|--------|--------------|--------------------------------|
| T-01-32, T-01-37 | Ruleset `main` with `integration_id` 15368 on both required contexts and the admin bypass | `01-14-SUMMARY.md` (apply-without-ruleset), `01-VERIFICATION.md` human item 2, ADR-0003 |
| T-01-24 | Automerge "only after required checks pass" relies on the ruleset | as above; Renovate cannot act before the merge |
| T-01-20, T-01-56 | Maintainer review of every merge to `main` through the ruleset; direct pushes are possible until it is applied | as above |
| T-01-51 | Admin bypass with `bypass_mode: pull_request` | `.github/rulesets/main.json` |
| T-01-01, T-01-27, T-01-29 | Hosted behaviour: setup-gradle wrapper validation, online zizmor, read-only cache off `main` | `01-VERIFICATION.md` human item 1 (first CI run) |

Live and confirmed by read-only `gh api` calls on 2026-11-22: secret scanning, push protection, vulnerability
alerts, private vulnerability reporting, squash-only merges with delete-branch-on-merge; `/rulesets` returns `[]`.

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01-01 | T-01-12 | False positives of the build-script lint are fixed in the rule, not suppressed (`01-07-PLAN.md`) | Author (plan approval) | 2026-11-21 |
| AR-01-02 | T-01-15 | The opt-in pre-commit hook runs repository scripts and Gradle locally (`01-08-PLAN.md`) | Author (plan approval) | 2026-11-21 |
| AR-01-03 | T-01-22 | The first label sync deletes GitHub's default labels; disclosed in the 01-14 approval text (`01-10-PLAN.md`) | Author (plan approval, 01-14 checkpoint) | 2026-11-21 |
| AR-01-04 | T-01-44 | A transient files-API failure fails the gate closed; re-run the job (`01-15-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-05 | T-01-46 | A malformed governed-paths line stops both consumers with a file and line error (`01-16-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-06 | T-01-48 | Build code may reach checks by routes outside the governed list; residual written in `governance/governed-paths.txt` header (`01-17-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-07 | T-01-49 | Wrapper and module changes need the label except version-only diffs (`01-17-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-08 | T-01-51 | A NUL in a patch blocks the gate; the admin bypass resolves it (`01-18-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-09 | T-01-54 | Renovate bumps that rewrite a `.mise/locks/**` sidecar need the label (`01-19-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-10 | T-01-55 | mise env, subdirectory and alternate-lockfile files stay ungoverned; reasoning in the governed-paths header (`01-19-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-11 | T-01-56 | The gate is evaluated from the pull request's own merge ref (review finding WR-06); ADR-0003 records the decision and its revisit triggers | Author (WR-06 disposition: document) | 2026-11-21 |
| AR-01-12 | T-01-57 | ADR-0003 describes the bypass publicly (`01-20-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-13 | T-01-61 | Absolute tool paths appear in local build output (`01-21-PLAN.md`) | Author (gap plan approval) | 2026-11-21 |
| AR-01-14 | — (review WR-02) | The `governance-approved` label is sticky and not bound to a commit; deferred because binding it needs a separate design decision (`01-REVIEW-DISPOSITION.md`, ADR-0003) | Author (WR-02 disposition: deferred) | 2026-11-21 |

*Accepted risks do not resurface in future audit runs.*

---

## Audit Notes

- Review findings closed by gap plans and verified in code: CR-01 (T-01-41), WR-01 (T-01-47), WR-03 (T-01-42),
  WR-04 (T-01-45), WR-05 (T-01-50), WR-07 (T-01-52, T-01-53), WR-08 (ADR-0003).
- Review finding WR-09 is still open: `docs/adr/0004-gsd-configuration.md` overstates what the agent ask rules
  guarantee. The T-01-34 mitigation itself is in place (54 governed entries equal 54 `Edit(/…)` ask rules plus
  `Bash(*config-set*)`, checked by `checkClaudeAskRules`); the ADR text needs correcting. Not a threat-register item.
- zizmor reports two suppressed `secrets-outside-env` findings (`GRADLE_ENCRYPTION_KEY` in the `ci.yml` build job,
  `SETTINGS_READ_TOKEN` in `settings-drift.yml`). Both are outside the registered threats; candidates for the next
  phase's threat model.
- `01-07-SUMMARY.md` has no `## Threat Flags` section. Plan 01-07 added only local build-time checks, so no flag is
  missing, but the section is absent.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-11-22 | 62 | 62 | 0 | `/gsd-secure-phase 01` (gsd-security-auditor, ASVS L1) |

Auditor evidence (read-only): actionlint, zizmor (offline and online), shellcheck, `reuse lint` (156/156),
`renovate-config-validator --strict`, gitleaks over 141 commits and the working tree (no leaks), GET-only `gh api`
calls for repository settings, and a governed-paths to ask-rules set comparison.

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-11-22
