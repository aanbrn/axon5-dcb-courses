---
phase: quick-261121-wjt
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - .planning/phases/01-repository-build-ci-foundation/COVERAGE.md
  - docs/adr/0004-gsd-configuration.md
autonomous: true
requirements: [AIDEV-05]

estimate:
  tokens: 45000
  raw_tokens: 45000
  tasks: 2
  confidence: low

must_haves:
  truths:
    - "The Phase 1 api-coverage seal check returns block false, passed true and coverage_present true, from a matrix rather than a no-integration declaration"
    - "Every row of the Phase 1 coverage matrix is INTEGRATE or OPT-OUT, every OPT-OUT carries a one-line reason, and every INTEGRATE row names the repository file that performs the call"
    - "ADR-0004 explains why governance/gsd-config-policy.json pins .planning/config.json as a whole file (D-14, AIDEV-05, plan 01-07) and how to change both deliberately in one governed, labelled pull request without committing transient runtime keys"
    - "ADR-0004 records why the GSD planner gets WebSearch, the risks, the mitigations and when to revisit, citing the WR-01 and WR-07 evidence from Phase 1"
    - "ADR-0004 states that later non-obvious keys (model_profile, branching_strategy, security_enforcement) are recorded in the same ADR, and says nothing about the per-agent skills mapping"
    - ".planning/config.json and governance/gsd-config-policy.json are unchanged; spotlessCheck, checkGsdConfigPolicy, editorconfig-checker and reuse lint pass"
  artifacts:
    - path: ".planning/phases/01-repository-build-ci-foundation/COVERAGE.md"
      provides: "API coverage matrix for the Phase 1 repository and CI tooling surfaces"
      contains: "| capability | decision | reason |"
    - path: "docs/adr/0004-gsd-configuration.md"
      provides: "ADR-0004, GSD configuration as a decision"
      contains: "# ADR-0004: GSD configuration as a decision"
  key_links:
    - from: ".planning/phases/01-repository-build-ci-foundation/COVERAGE.md"
      to: "scripts/governance/list-pr-files.sh"
      via: "INTEGRATE row for the pull request files endpoint names the script that calls it"
    - from: ".planning/phases/01-repository-build-ci-foundation/COVERAGE.md"
      to: "scripts/apply-github-settings.sh"
      via: "INTEGRATE rows for repository, security and ruleset endpoints name the script that calls them"
    - from: "docs/adr/0004-gsd-configuration.md"
      to: "governance/gsd-config-policy.json"
      via: "the ADR names the policy file and checkGsdConfigPolicy as the enforcement of D-14"
    - from: "docs/adr/0004-gsd-configuration.md"
      to: "docs/adr/0003-governance-gate-trust-boundary.md"
      via: "relative Markdown link for the governance-approved label and its trust boundary"
---

<objective>
Unblock the Phase 1 seal and record the GSD configuration decisions.

1. Write `.planning/phases/01-repository-build-ci-foundation/COVERAGE.md`, an API coverage matrix that the
   `api-coverage.verify-pre` gate accepts. Phase 1 integrates no product API. Its only API use is repository and CI
   tooling: the read-only GitHub REST call in `scripts/governance/list-pr-files.sh`, the approval-gated
   `scripts/apply-github-settings.sh`, the GitHub Actions in the workflows, OpenSSF Scorecard publishing, the
   REUSE API badge, and Renovate. Each capability is INTEGRATE or OPT-OUT with a reason, scoped honestly to what
   Phase 1 uses.
2. Write `docs/adr/0004-gsd-configuration.md` ("GSD configuration as a decision"). It covers the whole-file pin of
   `.planning/config.json` (D-14, AIDEV-05, plan 01-07) and how to change it deliberately. It records the decision
   to give the planner WebSearch in `agent_tools`, and it reserves the ADR for later non-obvious keys.

Purpose: as things stand, `check api-coverage.verify-pre` blocks the Phase 1 seal (live observation at planning
time: `block: true`, `coverage_present: false`, signal `(surface)` + `api`). The configuration decisions are
enforced by `checkGsdConfigPolicy` and the ask rules, but no record explains them.
Output: two new documentation files in two atomic commits. No configuration, policy, code or GitHub state
changes. Related requirement: AIDEV-05. The ADR documents how the policy changes. Enforcement stays the same.
</objective>

<execution_context>
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/workflows/execute-plan.md
@/Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.claude/CLAUDE.md

Facts observed at planning time (HEAD `8ab7174`):

- Matrix format accepted by `.claude/gsd-core/bin/lib/api-coverage.cjs` (`parseCoverageMatrix`,
  `validateCoverageMatrix`) and its gate `gate-api-coverage-verify-pre.cjs`:
  - A matrix section starts at a pipe-table header whose first cell is `capability`. The header is exactly
    `| capability | decision | reason |`. Repeated headers are allowed, one per section. Any line that does not
    start with a pipe ends a section.
  - Every data row has exactly 3 cells. The decision is `INTEGRATE` or `OPT-OUT`.
  - An OPT-OUT row needs a non-empty reason. An INTEGRATE reason is optional.
  - A capability is at most 80 characters and a reason at most 200. Neither may contain a pipe or a newline.
  - Capability names must be unique across the whole file, compared case-insensitively.
  - A line that starts with the no-integration declaration phrase turns the file into a declaration. That line
    next to rows is a validation error, so the file must not contain it.
  - The gate's exit status is 0 even when it blocks, so verification parses its JSON.
- Formatting scope: Spotless/Prettier and editorconfig-checker exclude `.planning/**`. REUSE covers `.planning/**`
  with an override annotation and everything else (including `docs/adr/**`) with the aggregate annotation, so
  neither file carries an SPDX header. The ADR must not contain SPDX tag text, because REUSE would parse it.
  Prettier (`printWidth: 100`, `proseWrap: preserve`) formats `docs/adr/0004-gsd-configuration.md`.
- Neither `docs/adr/**` nor `.planning/phases/**` is a governed path (`governance/governed-paths.txt`), so writing
  them triggers no ask rule and needs no `governance-approved` label.
- `.planning/config.json` equals `governance/gsd-config-policy.json` today. `agent_tools` is
  `{ "gsd-planner": ["WebSearch"] }`. All external search-provider keys (`brave_search`, `firecrawl`, `exa_search`,
  `tavily_search`, `ref_search`, `perplexity`, `jina`) are `false`. `plan_review.source_grounding` is `true` with
  authority `grep`. `workflow.plan_check` is `true`.
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Phase 1 API coverage matrix that clears the api-coverage seal gate</name>
  <files>.planning/phases/01-repository-build-ci-foundation/COVERAGE.md</files>
  <read_first>
    - .claude/gsd-core/bin/lib/api-coverage.cjs (parseCoverageMatrix and validateCoverageMatrix, lines 622-845; format rules summarised in the context block)
    - scripts/governance/list-pr-files.sh (the single `gh api --paginate` call)
    - scripts/apply-github-settings.sh (api_get, probe_status, probe_enabled, check_security, apply_changes, print_bypass_actors)
    - .github/repository-settings.json, .github/workflows/governance.yml, .github/workflows/labels.yml, .github/workflows/settings-drift.yml, .github/workflows/scorecard.yml, .github/workflows/codeql.yml, .github/workflows/ci.yml
    - renovate.json5 (extends, vulnerabilityAlerts, the three automerge rules), README.md line 3 (REUSE badge), docs/tooling.md (renovate row)
    - .planning/phases/01-repository-build-ci-foundation/01-14-SUMMARY.md (author-only steps: secrets, REUSE registration, deferred ruleset)
  </read_first>
  <action>
Create `.planning/phases/01-repository-build-ci-foundation/COVERAGE.md` as a multi-section matrix. This is the
tracer: write the file, run the gate, confirm `block: false`, then commit. Check every row against the file it
cites before you write it. If a row proves inaccurate, correct its decision or reason. Drop a row only when the
capability does not exist, and say so in the SUMMARY.

Preamble, in order:
- The heading `# API Coverage — Phase 1 repository and CI tooling`.
- The blockquote `> Full coverage by default. Opt-outs are explicit, reasoned decisions.`
- A short scope paragraph covering these points:
  - Phase 1 ships no product code and integrates no product API.
  - The seal-time detector found an external-API surface signal in the Phase 1 plans. The external APIs the phase
    touches are repository and CI tooling, enumerated below one section per surface.
  - Every INTEGRATE row names the file that performs the call.
  - Repository settings, rulesets and labels change only in two places: the approval-gated `--apply` mode of
    `scripts/apply-github-settings.sh` and the labels sync on `main`. Apart from that, CI uploads results
    (SARIF, artifacts, Scorecard) and Renovate opens pull requests.
  - Package registries that resolve build and tool dependencies are dependency sources, not integrations, and
    are out of scope. These are Maven Central, the Gradle Plugin Portal, npm and PyPI.
  - The file was written at seal time by quick task 261121-wjt.
- No paragraph line may begin with a pipe or with the no-integration declaration phrase.

Each section is a `##` heading, a blank line and a table with the header `| capability | decision | reason |`.
Keep reasons to one line under 200 characters, with no pipe characters. Name endpoints without a leading slash,
for example `repos/{owner}/{repo}/rulesets`.

Section "GitHub REST API: pull requests (governance gate)":
- `pulls.list-files`, INTEGRATE. Calls GET `repos/{owner}/{repo}/pulls/{n}/files` with `--paginate` in
  `scripts/governance/list-pr-files.sh`. Read-only (`pull-requests: read` in governance.yml) and fails closed.
- `pulls.read-labels`, OPT-OUT. The labels come from the `pull_request` event payload (`PR_LABELS_JSON` in
  governance.yml), so no API read is needed.
- `pulls.write`, OPT-OUT. The gate never comments, reviews or merges, so its token stays read-only.

Section "GitHub REST API: repository settings (scripts/apply-github-settings.sh)":
- `repos.get`, INTEGRATE. GET `repos/{owner}/{repo}` (also `gh repo view`) in every mode, including the scheduled
  `--check` in settings-drift.yml.
- `repos.update`, INTEGRATE. PATCH `repos/{owner}/{repo}` for the merge settings and for `security_and_analysis`
  (secret scanning and push protection). Runs only in `--apply`, by the author.
- `vulnerability-alerts`, INTEGRATE. A GET probe reads 2xx as on and 404 as off. `--apply` sends PUT or DELETE.
- `private-vulnerability-reporting`, INTEGRATE. GET reads `enabled`. `--apply` sends PUT or DELETE.
- `automated-security-fixes`, INTEGRATE. GET reads `enabled`. `--apply` sends PUT or DELETE. Declared `false` in
  `.github/repository-settings.json`.
- `rulesets.read`, INTEGRATE. GET `repos/{owner}/{repo}/rulesets` and `rulesets/{id}` for the drift comparison and
  the bypass-actor read-back.
- `rulesets.write`, INTEGRATE. POST `rulesets` or PUT `rulesets/{id}` from `.github/rulesets/main.json`, only in
  `--apply`. Applying `main` stays deferred until the required contexts are green (plan 01-14).
- `rulesets.delete`, OPT-OUT. The script never deletes a ruleset, so removing protection stays a manual,
  reviewed decision.
- `branch-protection.classic`, OPT-OUT. The repository is protected by the ruleset in
  `.github/rulesets/main.json`, not by classic branch protection.
- `actions.secrets`, OPT-OUT. The author set `GRADLE_ENCRYPTION_KEY` and `SETTINGS_READ_TOKEN` once (plan 01-14).
  No script reads or writes secrets.
- `actions.variables`, OPT-OUT. The author maintains `SETTINGS_DRIFT_SECTIONS`. settings-drift.yml reads it
  through the `vars` context, not the API.

Section "GitHub REST API: labels (labels.yml)":
- `labels.sync`, INTEGRATE. `crazy-max/ghaction-github-labeler` syncs `.github/labels.yml`. Pull requests get a
  dry run (`issues: read`). Pushes to `main` get the sync (`issues: write`).

Section "GitHub Actions platform (workflows)":
- `actions.checkout`, INTEGRATE. `actions/checkout` with `persist-credentials: false`, every action pinned by SHA
  (ADR-0001).
- `actions.toolchains`, INTEGRATE. `actions/setup-java`, `actions/setup-node`, and `jdx/mise-action` installing
  the tools locked in `mise.lock`.
- `actions.gradle-cache`, INTEGRATE. `gradle/actions/setup-gradle`. The cache is read-only off `main` and
  encrypted with `GRADLE_ENCRYPTION_KEY` (ci.yml).
- `actions.artifacts`, INTEGRATE. `actions/upload-artifact` in ci.yml.
- `code-scanning.upload`, INTEGRATE. `github/codeql-action` init and analyze in codeql.yml (actions language,
  ADR-0002), and upload-sarif in scorecard.yml, with `security-events: write`.
- `code-scanning.read-alerts`, OPT-OUT. Findings are read in the GitHub UI. No workflow or script queries alerts.
- `releases.publish`, OPT-OUT. Not needed yet. release-please owns tags and GitHub Releases from Phase 12
  (ROADMAP Phase 12, criterion 5).
- `packages.publish`, OPT-OUT. Not needed yet. Images go to GHCR from Phase 12 (ROADMAP Phase 12, criterion 5).
- `attestations.create`, OPT-OUT. Not needed yet. Provenance attestations arrive with signed releases in
  Phase 12.

Section "OpenSSF Scorecard API":
- `scorecard.publish`, INTEGRATE. `ossf/scorecard-action` with `publish_results: true` and `id-token: write`
  (OIDC) in scorecard.yml.
- `scorecard.badge`, OPT-OUT. Not needed yet. README badges, Scorecard included, are Phase 13 scope (ROADMAP
  Phase 13, criterion 1).

Section "REUSE API (api.reuse.software)":
- `reuse.badge`, INTEGRATE. The README.md status badge image, GET `badge/github.com/{owner}/{repo}`.
- `reuse.info`, INTEGRATE. The badge link target, GET `info/github.com/{owner}/{repo}`.
- `reuse.register`, OPT-OUT. Registration is a one-time author step in the browser and an open item of plan
  01-14. No tooling sends project data to the API.

Section "Renovate (Mend Renovate GitHub App, D-11)":
- `renovate.version-updates`, INTEGRATE. `renovate.json5` (`config:best-practices`) opens update pull requests
  for every version source.
- `renovate.action-digest-pinning`, INTEGRATE. `helpers:pinGitHubActionDigestsToSemver`.
- `renovate.automerge`, INTEGRATE. `automerge: true` in exactly the three D-12 rules, with squash platform
  automerge. Everything else is merged by hand.
- `renovate.vulnerability-alerts`, INTEGRATE. These pull requests get the `security` label and never automerge.
- `renovate.config-validation`, INTEGRATE. `renovate-config-validator` from `npm:renovate` (mise.toml), run
  locally, in CI and in the hook (docs/tooling.md).
- `renovate.self-hosted`, OPT-OUT. D-11 runs Renovate as the Mend GitHub App, so no workflow runs the renovate
  CLI against GitHub.

Name repositories with the `{owner}/{repo}` placeholder. Write secret names only, never values. Do not edit
`.planning/config.json`, `governance/gsd-config-policy.json` or any other file.

Commit only this file. Run `mise x -- git add .planning/phases/01-repository-build-ci-foundation/COVERAGE.md`.
Then run `mise x -- git commit -m "docs(01): add the Phase 1 API coverage matrix"`, with a second `-m` carrying
the attribution line from the executor's environment. The lefthook pre-commit hook runs gitleaks and REUSE.
Never use `git stash`, never push, and make no GitHub writes.
  </action>
  <verify>
    <automated>node /Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/bin/gsd-tools.cjs check api-coverage.verify-pre .planning/phases/01-repository-build-ci-foundation --raw | mise x -- jq '[.block == false, .passed == true, .coverage_present == true, (has("none_declared") | not), (.counts.surface // 0) >= 25, (.counts.optout // 0) >= 8] | map(select(.)) | length' # [HEAD=1 EXPECT==6 @8ab7174]</automated>
    The command prints how many of the six gate conditions hold. It must print 6: not blocked, passed, matrix
    present, no declaration, at least 25 capabilities, at least 8 opt-outs.
    Additional checks. Run each as its own command:
    - `grep -o -F -e scripts/governance/list-pr-files.sh -e scripts/apply-github-settings.sh -e labels.yml -e scorecard.yml -e renovate.json5 -e README.md .planning/phases/01-repository-build-ci-foundation/COVERAGE.md | sort -u | grep -c .` [HEAD=0 EXPECT==6 @8ab7174]
    - `mise x -- reuse lint` exits 0 (invariant, also run by the pre-commit hook).
  </verify>
  <done>
    The gate prints `api-coverage: matrix present (N capabilities, M opt-out)` with N of at least 25 and M of at
    least 8, and returns `block: false` without `none_declared`. Every INTEGRATE row names its performing file,
    every OPT-OUT row has a reason, and the file is committed on its own.
  </done>
</task>

<task type="auto">
  <name>Task 2: ADR-0004, GSD configuration as a decision</name>
  <files>docs/adr/0004-gsd-configuration.md</files>
  <read_first>
    - docs/adr/0003-governance-gate-trust-boundary.md and docs/adr/0001-workflow-permissions-and-required-checks.md (format: title line, Status/Date/Deciders bullets, minimal-record note, section style, line wrapping near 110 columns, relative ADR links)
    - .planning/config.json and governance/gsd-config-policy.json (read only)
    - build-logic/src/main/kotlin/io/github/aanbrn/courses/buildlogic/governance/ConfigPolicyCheck.kt (KDoc) and build-logic/src/main/kotlin/courses.governance.gradle.kts (task registration)
    - .claude/settings.json (the `Edit(/.planning/config.json)` and `Bash(*config-set*)` ask rules)
    - .planning/phases/01-repository-build-ci-foundation/01-CONTEXT.md (D-14), .planning/REQUIREMENTS.md (AIDEV-05, AIDEV-08), 01-07-SUMMARY.md line 139, 01-RESEARCH.md finding 8 (line 115)
    - .planning/phases/01-repository-build-ci-foundation/01-17-PLAN.md lines 80-90 (WR-01: Groovy script read before the Kotlin one, sourced from the Gradle forum, MEDIUM confidence) and 01-19-PLAN.md lines 88-110 (WR-07: Renovate config search order, mise config names), 01-REVIEW-DISPOSITION.md (WR-01 fixed by 01-17, WR-07 fixed by 01-19)
  </read_first>
  <action>
Create `docs/adr/0004-gsd-configuration.md` in the format of ADR-0001 to ADR-0003. Write it in English, in plain
reference prose.

Header:
- The title line `# ADR-0004: GSD configuration as a decision`.
- The bullets `- Status: accepted`, `- Date: 2026-11-21` and `- Deciders: repository author (decision D-14 and the planner web-search setting)`.
- Then, verbatim, the minimal-record note:
  `This is a minimal record. Phase 2 introduces the ADR templates and normalises the format of this note.`

Use exactly these eight `##` sections, in this order.

1. `## Context`. GSD reads `.planning/config.json` in every workflow, so a changed key changes how later phases
   are planned, executed and verified. GSD also writes keys at runtime through `config-set`, for example
   `workflow._auto_chain_active` during auto-chaining (01-RESEARCH finding 8), and a GSD update can add keys.
   AIDEV-05 asks for the settings to be enforced as policy.

2. `## Decision: the policy pins the whole file`. Implements D-14.
   - `governance/gsd-config-policy.json` is an exact copy of the accepted `.planning/config.json`.
   - `checkGsdConfigPolicy` (`ConfigPolicyCheck`, plan 01-07, part of `check`) compares the two parsed documents.
     It ignores key order and whitespace. It fails on any added, removed or changed key, and it reports key paths
     only, never values.
   - Reason for pinning the whole file rather than a key list: keys added by a GSD update and keys written at
     runtime become visible, reviewable diffs instead of silent drift.

3. `## Changing the configuration deliberately`. A numbered procedure:
   1. Change `.planning/config.json`, through GSD or by hand. The ask rules in `.claude/settings.json` (AIDEV-08)
      make the author confirm both routes.
   2. Make the same change to `governance/gsd-config-policy.json` in the same pull request.
      `checkGsdConfigPolicy` passes only when the two agree.
   3. Both files are governed paths, so the pull request needs the `governance-approved` label for
      `governance-success` to pass. Link ADR-0001 and ADR-0003 relatively, and keep ADR-0003's point that the gate
      runs from the pull request's own tree.
   4. Record the reason for a non-obvious key in this ADR.

   Then a rule: transient runtime keys such as `workflow._auto_chain_active` are never committed. Reset them
   before committing, and never copy them into the policy to make the check pass. The check's failure message
   names the offending key path.

4. `## Decision: the planner may search the web`.
   - The decision: `agent_tools` gives the `gsd-planner` agent the `WebSearch` tool and no other agent gets it.
     The external search-provider keys in the same file stay `false`.
   - Why: the toolchain moves fast and training data lags behind it, while the planner writes the facts that
     executors act on. Phase 1 evidence:
     - for WR-01 (fixed by plan 01-17), the planner verified that Gradle reads a Groovy build or settings script
       before the Kotlin one;
     - for WR-07 (fixed by plan 01-19), it verified Renovate's configuration search order and mise's config file
       names, which became governed paths.
     - Say plainly that the Gradle claim rested on a forum source at MEDIUM confidence, and that the plan chose
       the action that was safe either way: governing a file that does not exist costs nothing.
   - Risks, as a list:
     - web content, whether wrong, outdated or written to steer agents, can shape a plan;
     - query text leaves the machine, so queries carry technical questions only, never secrets, tokens,
       credentials, personal data or private repository content;
     - each planning run costs extra tokens and time.
   - Mitigations, as a list:
     - the plan checker (`workflow.plan_check`) reviews every plan before execution;
     - executors prove every result by running the build, linters and tests, so a wrong web fact fails a gate
       instead of shipping;
     - source grounding (`plan_review.source_grounding`, authority `grep`) checks plan claims against the
       repository;
     - only the planner has the tool;
     - the maintainer reviews the diff of every pull request (ADR-0003).

5. `## Rejected alternatives`. Numbered, two per decision:
   - pinning only selected keys, as in AIDEV-05's examples, would let unlisted keys and keys added by an update
     change silently;
   - ignoring `_`-prefixed keys automatically would make the check lenient by convention. It is not adopted, and
     adopting it would itself be a deliberate policy change;
   - no web access for the planner would leave plans resting on training data, and the WR-01 and WR-07 facts were
     not in the repository;
   - web access for every agent would widen the channel through which outside content can steer code changes,
     without a gain, because executors prove facts by running tools.

6. `## Consequences`.
   - Every configuration change is a reviewed two-file pull request with the label.
   - A GSD update that adds keys fails `check` until the policy is updated deliberately.
   - Planning runs cost more.
   - Plans may cite web sources, and the executors' runs remain the proof.

7. `## Recording later keys`. Further non-obvious keys get a short section in this ADR, in the same pull request
   that changes the key, with the decision, the reason and a revisit trigger. Name `model_profile`,
   `git.branching_strategy` and `workflow.security_enforcement` as examples, without recording decisions for them
   now.

8. `## When to revisit`.
   - Web search: revisit when a plan proves to have been steered by wrong web content that no gate caught, when
     the plan checker or source grounding is switched off, when planning cost becomes a problem, or when a second
     maintainer or external contributors arrive (as in ADR-0003).
   - The whole-file pin: revisit when transient keys keep failing the check, and decide then, as a policy change,
     whether to ignore them.

Constraints:
- Do not mention the per-agent skills mapping key in `.planning/config.json`, the key whose value is an empty
  object. The author postponed that topic.
- Do not write SPDX tag text.
- Do not edit `.planning/config.json`, `governance/gsd-config-policy.json` or any other existing file.

After writing:
1. Run `mise x -- ./gradlew spotlessCheck checkGsdConfigPolicy`. If Prettier flags the ADR, run
   `mise x -- ./gradlew spotlessApply` and re-run the check.
2. Run `mise x -- editorconfig-checker` and `mise x -- reuse lint`.
3. Commit only this file. Run `mise x -- git add docs/adr/0004-gsd-configuration.md`, then
   `mise x -- git commit -m "docs(adr): record the GSD configuration decisions in ADR-0004"`, with a second `-m`
   carrying the attribution line from the executor's environment.
4. Never use `git stash`, never push, and make no GitHub writes.
  </action>
  <verify>
    <automated>grep -c -E '^## (Context|Decision: the policy pins the whole file|Changing the configuration deliberately|Decision: the planner may search the web|Rejected alternatives|Consequences|Recording later keys|When to revisit)$' docs/adr/0004-gsd-configuration.md # [HEAD=error:2 EXPECT==8 @8ab7174]</automated>
    Additional checks. Run each as its own command:
    - `grep -c -x -F -e '# ADR-0004: GSD configuration as a decision' -e '- Status: accepted' -e 'This is a minimal record. Phase 2 introduces the ADR templates and normalises the format of this note.' docs/adr/0004-gsd-configuration.md` [HEAD=error:2 EXPECT==3 @8ab7174]
    - `grep -o -F -e governance/gsd-config-policy.json -e checkGsdConfigPolicy -e governance-approved -e _auto_chain_active -e gsd-planner -e WebSearch -e WR-01 -e WR-07 -e model_profile -e branching_strategy -e security_enforcement -e 0003-governance-gate-trust-boundary.md docs/adr/0004-gsd-configuration.md | sort -u | grep -c .` [HEAD=0 EXPECT==12 @8ab7174]
    - `grep -c -e agent_skills -e SPDX- docs/adr/0004-gsd-configuration.md` [HEAD=error:2 EXPECT==0 @8ab7174]
    - `git diff --exit-code 8ab7174 -- .planning/config.json governance/gsd-config-policy.json` [HEAD=0 EXPECT==0 @8ab7174 GUARD MUTATION: any edit to .planning/config.json or governance/gsd-config-policy.json makes it exit 1]. If 8ab7174 is no longer an ancestor after a rebase, compare against the commit the executor started from.
    - `mise x -- ./gradlew spotlessCheck checkGsdConfigPolicy` ends in BUILD SUCCESSFUL (invariant).
    - `mise x -- editorconfig-checker` exits 0 (invariant).
    - `mise x -- reuse lint` exits 0 (invariant).
  </verify>
  <done>
    ADR-0004 exists with the ADR-0001 to ADR-0003 header format and the eight sections. It explains the whole-file
    pin, the deliberate two-file, labelled change procedure, the transient-key rule, the planner WebSearch decision
    (why, risks, mitigations, rejected alternatives, revisit triggers) and the rule for recording later keys. It
    does not mention the skills mapping. Spotless, the config-policy check, editorconfig-checker and REUSE pass.
    Both configuration files are unchanged, and the ADR is committed on its own.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| repository → public readers | Both files are public documentation. Anything written there is disclosed. |
| COVERAGE.md → GSD seal gate → orchestrator | The gate parses COVERAGE.md and its verdict decides whether Phase 1 may seal. |
| planner → web search provider (subject of the ADR, not exercised by this task) | Query text leaves the machine. Results flow back into plans. |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-QWJT-01 | Information disclosure | COVERAGE.md, ADR-0004 | low | mitigate | Name secrets only (they already appear in workflow files) and never values. Use `{owner}/{repo}` placeholders. The gitleaks pre-commit hook scans the staged files. |
| T-QWJT-02 | Tampering | `.planning/config.json`, `governance/gsd-config-policy.json` | medium | mitigate | The task writes two new files only. Task 2 verify runs `git diff --exit-code 8ab7174` on both files and runs `checkGsdConfigPolicy`. |
| T-QWJT-03 | Spoofing | api-coverage seal gate | medium | mitigate | A matrix with fabricated INTEGRATE rows, or a no-integration declaration, would clear the gate falsely. Every INTEGRATE row cites the file that performs the call (Task 1 anchor grep), and the jq check rejects `none_declared`. Inaccurate rows are corrected, not padded. |
| T-QWJT-04 | Tampering | gate message read by the orchestrator | low | accept | The gate relays only counts and row-indexed errors, never cell text. Cell lengths are capped at 80 and 200 characters by the validator. |
| T-QWJT-05 | Information disclosure | planner WebSearch queries (documented by ADR-0004) | medium | mitigate | ADR-0004 states that queries carry technical questions only, never secrets, credentials, personal data or private repository content, and lists the revisit triggers. |
| T-QWJT-SC | Tampering | npm/pip/cargo installs | high | accept | This plan installs no packages. It uses only the tools already locked in `mise.lock`. |
</threat_model>

<verification>
- `node /Users/afanasyev/Development/axon5-dcb-courses/.claude/gsd-core/bin/gsd-tools.cjs check api-coverage.verify-pre .planning/phases/01-repository-build-ci-foundation --raw` prints `"block": false` and `"coverage_present": true`, with no `none_declared`.
- `mise x -- ./gradlew spotlessCheck checkGsdConfigPolicy`, `mise x -- editorconfig-checker` and `mise x -- reuse lint` all pass.
- `git log --oneline -2` shows the two task commits. `git status --porcelain` is empty apart from the orchestrator-owned SUMMARY.
- `git diff --exit-code 8ab7174 -- .planning/config.json governance/gsd-config-policy.json` exits 0.
</verification>

<success_criteria>
- The Phase 1 api-coverage seal gate no longer blocks, and passes through a real matrix of at least 25 capabilities with at least 8 reasoned opt-outs.
- ADR-0004 is committed, matches the existing ADR format, and covers the whole-file pin, the deliberate change procedure, transient keys, the planner WebSearch decision and the rule for recording later keys, without touching the postponed skills topic.
- No configuration, policy, code or GitHub state changed. Nothing is pushed.
</success_criteria>

<output>
Create `.planning/quick/261121-wjt-adr-0004-on-gsd-configuration-and-covera/261121-wjt-SUMMARY.md` with `status: complete` in its frontmatter when done (the orchestrator commits it).
</output>
